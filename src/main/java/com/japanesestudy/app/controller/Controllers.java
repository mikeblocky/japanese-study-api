package com.japanesestudy.app.controller;

import com.japanesestudy.app.dto.auth.AuthDtos.*;
import com.japanesestudy.app.dto.common.ErrorResponse;
import com.japanesestudy.app.dto.progress.ProgressDtos.*;
import com.japanesestudy.app.entity.*;
import com.japanesestudy.app.repository.Repositories.*;
import com.japanesestudy.app.security.JwtUtils;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import com.japanesestudy.app.service.*;
import com.japanesestudy.app.util.ControllerUtils;
import com.japanesestudy.app.util.EntityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static com.japanesestudy.app.util.ControllerUtils.*;
import static com.japanesestudy.app.util.EntityUtils.*;

public class Controllers {

    @RestController
    @RequestMapping("/api/auth")
    @RequiredArgsConstructor
    @Slf4j
    public static class Auth {
        private final AuthenticationManager authenticationManager;
        private final UserRepository userRepository;
        private final org.springframework.security.crypto.password.PasswordEncoder encoder;
        private final JwtUtils jwtUtils;

        @PostMapping("/login")
        public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
            try {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                String jwt = jwtUtils.generateJwtToken(authentication);
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                List<String> roles = userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList();
                return ok(new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), roles));
            } catch (org.springframework.security.authentication.BadCredentialsException e) {
                return unauthorized("Invalid username or password");
            } catch (RuntimeException e) {
                log.error("Login failed", e);
                return error("Login failed: " + e.getMessage());
            }
        }

        @PostMapping("/register")
        public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
            try {
                if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                    return error("Username is already taken!");
                }
                User user = new User(signUpRequest.getUsername(), encoder.encode(signUpRequest.getPassword()), Role.USER);
                userRepository.save(user);
                return message("User registered successfully!");
            } catch (RuntimeException e) {
                log.error("Signup failed", e);
                return error("Signup failed: " + e.getMessage());
            }
        }
    }

    @RestController
    @RequestMapping("/api/courses")
    @RequiredArgsConstructor
    public static class Course {
        private final CatalogService catalogService;

        @GetMapping
        public ResponseEntity<List<com.japanesestudy.app.entity.Course>> getAllCourses() {
            return ok(catalogService.getAllCourses());
        }

        @GetMapping("/{courseId}")
        public ResponseEntity<com.japanesestudy.app.entity.Course> getCourseById(@PathVariable Long courseId) {
            return catalogService.getCourseById(courseId)
                    .map(ControllerUtils::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        @PostMapping
        public ResponseEntity<com.japanesestudy.app.entity.Course> createCourse(
                @RequestBody com.japanesestudy.app.entity.Course course,
                @AuthenticationPrincipal UserDetailsImpl userDetails) {
            User owner = new User();
            owner.setId(userDetails.getId());
            course.setOwner(owner);
            return created(catalogService.createCourse(course));
        }

        @PutMapping("/{courseId}")
        public ResponseEntity<com.japanesestudy.app.entity.Course> updateCourse(
                @PathVariable Long courseId,
                @RequestBody com.japanesestudy.app.entity.Course updates,
                @AuthenticationPrincipal UserDetailsImpl userDetails) {
            com.japanesestudy.app.entity.Course course = getOrThrow(() -> catalogService.getCourseById(courseId), "Course not found");
            validateOwnership(course.getOwner() != null ? course.getOwner().getId() : null, userDetails.getId());
            copyNonNullFields(updates, course);
            return ok(catalogService.updateCourse(course));
        }

        @DeleteMapping("/{courseId}")
        public ResponseEntity<Void> deleteCourse(
                @PathVariable Long courseId,
                @AuthenticationPrincipal UserDetailsImpl userDetails) {
            com.japanesestudy.app.entity.Course course = getOrThrow(() -> catalogService.getCourseById(courseId), "Course not found");
            validateOwnership(course.getOwner() != null ? course.getOwner().getId() : null, userDetails.getId());
            catalogService.deleteCourse(courseId);
            return noContent();
        }
    }

    @RestController
    @RequestMapping("/api/import")
    @RequiredArgsConstructor
    @Slf4j
    public static class Import {
        private final AnkiService ankiService;
        private final UserRepository userRepository;

        @PostMapping("/anki")
        public ResponseEntity<?> importAnki(
                @RequestParam("file") MultipartFile file,
                @RequestParam(value = "filename", required = false) String filename,
                @AuthenticationPrincipal UserDetailsImpl userDetails) {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse(400, "File is required"));
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.endsWith(".apkg")) {
                return ResponseEntity.badRequest().body(new ErrorResponse(400, "Only .apkg files are supported"));
            }
            try {
                User owner = userRepository.findById(userDetails.getId())
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));
                String deckName = filename != null ? filename : originalFilename;
                Map<String, Object> result = ankiService.importAnkiFile(file, deckName, owner);
                return ResponseEntity.ok(result);
            } catch (IllegalArgumentException e) {
                log.error("Validation error during import", e);
                return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
            } catch (Exception e) {
                log.error("Failed to import Anki deck", e);
                return ResponseEntity.internalServerError()
                        .body(new ErrorResponse(500, "Import failed: " + e.getMessage(), e.toString()));
            }
        }

        @GetMapping("/status")
        public ResponseEntity<Map<String, String>> getImportStatus() {
            return ok(Map.of("status", "ready", "supportedFormats", ".apkg"));
        }
    }

    @RestController
    @RequestMapping("/api/progress")
    @RequiredArgsConstructor
    public static class Progress {
        private final ProgressService progressService;

        @GetMapping
        public ResponseEntity<List<ProgressResponse>> getAllProgress(@AuthenticationPrincipal UserDetailsImpl userDetails) {
            return ResponseEntity.ok(progressService.getAllProgress(userDetails.getId()));
        }

        @GetMapping("/topic/{topicId}")
        public ResponseEntity<List<ProgressResponse>> getTopicProgress(
                @PathVariable Long topicId,
                @AuthenticationPrincipal UserDetailsImpl userDetails) {
            return ResponseEntity.ok(progressService.getTopicProgress(userDetails.getId(), topicId));
        }

        @PostMapping("/record")
        public ResponseEntity<ProgressResponse> recordProgress(
                @RequestBody RecordProgressRequest request,
                @AuthenticationPrincipal UserDetailsImpl userDetails) {
            return ResponseEntity.ok(progressService.recordProgress(userDetails.getId(), request.getStudyItemId(), request.isCorrect(), request.isHarshMode()));
        }

        @GetMapping("/challenge")
        public ResponseEntity<List<ProgressResponse>> getChallengeItems(
                @RequestParam(defaultValue = "20") int limit,
                @AuthenticationPrincipal UserDetailsImpl userDetails) {
            return ResponseEntity.ok(progressService.getChallengeItems(userDetails.getId(), limit));
        }

        @GetMapping("/stats")
        public ResponseEntity<ProgressStatsResponse> getStats(@AuthenticationPrincipal UserDetailsImpl userDetails) {
            return ResponseEntity.ok(progressService.getStats(userDetails.getId()));
        }
    }

    @RestController
    @RequestMapping("/api/items")
    @RequiredArgsConstructor
    public static class StudyItem {
        private final CatalogService catalogService;

        @GetMapping("/{itemId}")
        public ResponseEntity<com.japanesestudy.app.entity.StudyItem> getItemById(@PathVariable Long itemId) {
            return catalogService.getStudyItemById(itemId)
                    .map(ControllerUtils::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        @PutMapping("/{itemId}")
        public ResponseEntity<com.japanesestudy.app.entity.StudyItem> updateItem(
                @PathVariable Long itemId,
                @RequestBody com.japanesestudy.app.entity.StudyItem updates) {
            com.japanesestudy.app.entity.StudyItem item = getOrThrow(() -> catalogService.getStudyItemById(itemId), "Study item not found");
            copyNonNullFields(updates, item);
            return ok(catalogService.updateStudyItem(item));
        }

        @DeleteMapping("/{itemId}")
        public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) {
            catalogService.deleteStudyItem(itemId);
            return noContent();
        }
    }

    @RestController
    @RequestMapping("/api/topics")
    @RequiredArgsConstructor
    public static class Topic {
        private final CatalogService catalogService;

        @GetMapping("/course/{courseId}")
        public ResponseEntity<List<com.japanesestudy.app.entity.Topic>> getTopicsByCourse(@PathVariable Long courseId) {
            return ok(catalogService.getTopicsByCourse(courseId));
        }

        @GetMapping("/{topicId}/items")
        public ResponseEntity<List<com.japanesestudy.app.entity.StudyItem>> getItemsByTopic(@PathVariable Long topicId) {
            return ok(catalogService.getItemsByTopic(topicId));
        }

        @PostMapping
        public ResponseEntity<com.japanesestudy.app.entity.Topic> createTopic(@RequestBody com.japanesestudy.app.entity.Topic topic) {
            return created(catalogService.createTopic(topic));
        }

        @PutMapping("/{topicId}")
        public ResponseEntity<com.japanesestudy.app.entity.Topic> updateTopic(
                @PathVariable Long topicId,
                @RequestBody com.japanesestudy.app.entity.Topic updates) {
            com.japanesestudy.app.entity.Topic topic = getOrThrow(() -> catalogService.getTopicById(topicId), "Topic not found");
            copyNonNullFields(updates, topic);
            return ok(catalogService.updateTopic(topic));
        }

        @DeleteMapping("/{topicId}")
        public ResponseEntity<Void> deleteTopic(@PathVariable Long topicId) {
            catalogService.deleteTopic(topicId);
            return noContent();
        }
    }
}
