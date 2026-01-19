package com.japanesestudy.app.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.japanesestudy.app.entity.AccessLevel;
import com.japanesestudy.app.entity.AuditLog;
import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.CourseAccess;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.entity.User;
import com.japanesestudy.app.repository.AuditLogRepository;
import com.japanesestudy.app.repository.CourseAccessRepository;
import com.japanesestudy.app.repository.CourseRepository;
import com.japanesestudy.app.repository.StudyItemRepository;
import com.japanesestudy.app.repository.TopicRepository;
import com.japanesestudy.app.repository.UserProgressRepository;
import com.japanesestudy.app.repository.UserRepository;
import com.japanesestudy.app.util.Utils.EvictAllCaches;
import com.japanesestudy.app.util.Utils.EvictItemCaches;
import com.japanesestudy.app.util.Utils.EvictTopicCaches;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CourseRepository courseRepository;
    private final CourseAccessRepository courseAccessRepository;
    private final TopicRepository topicRepository;
    private final StudyItemRepository studyItemRepository;
    private final UserProgressRepository userProgressRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @Cacheable(cacheNames = "courses")
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public List<Course> searchCourses(Long ownerId, String level, String tag, String query) {
        return courseRepository.search(ownerId, level, tag, query);
    }

    @Cacheable(cacheNames = "courses", condition = "#userId != null")
    public List<Course> getVisibleCourses(Long userId) {
        return courseRepository.findByOwnerId(userId);
    }

    @Cacheable(cacheNames = "courseById", key = "#courseId")
    public Optional<Course> getCourseById(long courseId) {
        return courseRepository.findById(courseId);
    }

    @Transactional(readOnly = true)
    public CourseSummary getCourseSummary(long courseId, Long userId) {
        Course course = findCourseOrThrow(courseId);
        long topics = topicRepository.countByCourseIdAndDeletedFalse(courseId);
        long items = studyItemRepository.countActiveByCourseId(courseId);
        long studied = (userId == null) ? 0 : userProgressRepository.countStudiedByUserAndCourse(userId, courseId);
        double progressPercent = items == 0 ? 0 : (double) studied * 100.0 / items;
        return new CourseSummary(topics, items, studied, progressPercent, course.getCreatedAt(), course.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "topicsByCourse", key = "#courseId")
    public List<Topic> getTopicsByCourse(long courseId) {
        return topicRepository.findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(courseId);
    }

    @Transactional(readOnly = true)
    public Page<Topic> getTopicsByCourse(long courseId, int page, int size) {
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by("orderIndex").ascending());
        return topicRepository.findByCourseIdAndDeletedFalse(courseId, pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "itemsByTopic", key = "#topicId")
    public List<StudyItem> getItemsByTopic(long topicId) {
        return studyItemRepository.findByTopicIdAndDeletedFalse(topicId);
    }

    @Transactional(readOnly = true)
    public Page<StudyItem> getItemsByTopic(long topicId, int page, int size) {
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by("id").ascending());
        return studyItemRepository.findByTopicIdAndDeletedFalse(topicId, pageable);
    }

    public List<StudyItem> getItemsByTopicForUser(long topicId, Long userId) {
        List<StudyItem> items = studyItemRepository.findByTopicIdAndDeletedFalse(topicId);
        if (userId == null) {
            return items;
        }

        var progressList = userProgressRepository.findByUserIdAndTopicId(userId, topicId);
        var intervalMap = progressList.stream()
                .filter(p -> p.getStudyItem() != null)
                .collect(java.util.stream.Collectors.toMap(
                        p -> p.getStudyItem().getId(),
                        com.japanesestudy.app.entity.UserProgress::getInterval,
                        (v1, v2) -> v1
                ));

        items.forEach(item -> item.setUserSrsInterval(intervalMap.getOrDefault(item.getId(), 0)));
        return items;
    }

    @Cacheable(cacheNames = "itemsByTopic", key = "'topic:' + #topicId + ':limit:' + #limit")
    public List<StudyItem> getItemsByTopic(long topicId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return studyItemRepository.findByTopicIdAndDeletedFalse(topicId, PageRequest.of(0, limit)).getContent();
    }

    @Transactional
    @EvictAllCaches
    public Course createCourse(Course course) {
        return createCourse(course, null);
    }

    @Transactional
    @EvictAllCaches
    public Course createCourse(Course course, Long actorUserId) {
        Course saved = courseRepository.save(course);
        logAudit("Course", saved.getId(), "CREATE", actorUserId,
                "title=\"" + (saved.getTitle() == null ? "" : saved.getTitle()) + "\"");
        return saved;
    }

    @Transactional
    @EvictAllCaches
    public Course updateCourse(Course course) {
        return updateCourse(course, null);
    }

    @Transactional
    @EvictAllCaches
    public Course updateCourse(Course course, Long actorUserId) {
        Course saved = courseRepository.save(course);
        logAudit("Course", saved.getId(), "UPDATE", actorUserId,
                "title=\"" + (saved.getTitle() == null ? "" : saved.getTitle()) + "\" updatedAt=" + saved.getUpdatedAt());
        return saved;
    }

    @Transactional(readOnly = true)
    public boolean canViewCourse(Long courseId, Long userId, boolean isAdmin) {
        if (isAdmin) {
            return true;
        }
        if (userId == null) {
            return false;
        }
        return courseRepository.findById(courseId)
                .map(course -> isOwner(course, userId) || hasAccess(courseId, userId, AccessLevel.VIEW))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canEditCourse(Long courseId, Long userId, boolean isAdmin) {
        if (isAdmin) {
            return true;
        }
        if (userId == null) {
            return false;
        }
        return courseRepository.findById(courseId)
                .map(course -> isOwner(course, userId) || hasAccess(courseId, userId, AccessLevel.EDIT))
                .orElse(false);
    }

    @Transactional
    public CourseAccess grantCourseAccess(long courseId, long targetUserId, AccessLevel level, long actorUserId, boolean isAdmin) {
        Course course = findCourseOrThrow(courseId);
        if (!canEditCourse(courseId, actorUserId, isAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to share this course");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        CourseAccess access = courseAccessRepository.findByCourseIdAndUserId(courseId, targetUserId)
                .orElseGet(CourseAccess::new);
        access.setCourse(course);
        access.setUser(target);
        access.setAccessLevel(level);
        return courseAccessRepository.save(access);
    }

    @Transactional
    public void revokeCourseAccess(long courseId, long targetUserId, long actorUserId, boolean isAdmin) {
        if (!canEditCourse(courseId, actorUserId, isAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to revoke access");
        }
        courseAccessRepository.findByCourseIdAndUserId(courseId, targetUserId)
                .ifPresent(courseAccessRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<CourseAccess> listCourseAccess(long courseId, long actorUserId, boolean isAdmin) {
        if (!canEditCourse(courseId, actorUserId, isAdmin)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to list access");
        }
        return courseAccessRepository.findByCourseId(courseId);
    }

    @Transactional
    @EvictAllCaches
    public void deleteCourse(long courseId) {
        deleteCourse(courseId, null);
    }

    @Transactional
    @EvictAllCaches
    public void deleteCourse(long courseId, Long actorUserId) {
        courseRepository.findById(courseId).ifPresent(course -> {
            long topicCount = topicRepository.countByCourseIdAndDeletedFalse(courseId);
            // Remove dependent data to avoid FK conflicts
            userProgressRepository.hardDeleteByCourseId(courseId);
            studyItemRepository.hardDeleteByCourseId(courseId);
            topicRepository.hardDeleteByCourseId(courseId);
            logAudit("Course", courseId, "DELETE", actorUserId,
                    "title=\"" + (course.getTitle() == null ? "" : course.getTitle()) + "\"; topics="
                    + topicCount);
        });
        courseRepository.deleteById(courseId);
    }

    @Transactional
    @EvictTopicCaches
    public Topic createTopic(Topic topic) {
        return createTopic(topic, null);
    }

    @Transactional
    @EvictTopicCaches
    public Topic createTopic(Topic topic, Long actorUserId) {
        validateTopic(topic);
        Topic saved = topicRepository.save(topic);
        logAudit("Topic", saved.getId(), "CREATE", actorUserId,
                "courseId=" + (saved.getCourse() != null ? saved.getCourse().getId() : null)
                + "; title=\"" + (saved.getTitle() == null ? "" : saved.getTitle()) + "\"");
        return saved;
    }

    @Transactional
    @EvictTopicCaches
    public BulkResult bulkUpsertTopics(long courseId, List<TopicUpsert> payloads, boolean dryRun, Long actorUserId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        if (payloads == null || payloads.isEmpty()) {
            return new BulkResult(0, 0);
        }
        int created = 0;
        int updated = 0;
        List<Topic> toSave = new ArrayList<>();

        for (TopicUpsert dto : payloads) {
            if (dto == null) {
                continue;
            }
            if (dto.id() != null) {
                validateTopicUpsert(dto, true);
                Topic topic = findTopicOrThrow(dto.id());
                assertTopicBelongsToCourse(topic, courseId);
                applyTopicUpdates(topic, dto);
                validateTopic(topic);
                updated++;
                toSave.add(topic);
            } else {
                validateTopicUpsert(dto, false);
                Topic topic = new Topic();
                topic.setCourse(course);
                topic.setTitle(dto.title());
                topic.setDescription(dto.description());
                topic.setOrderIndex(dto.orderIndex());
                validateTopic(topic);
                created++;
                toSave.add(topic);
            }
        }

        if (!dryRun && !toSave.isEmpty()) {
            topicRepository.saveAll(toSave);
            logAudit("Topic", null, "BULK_UPSERT", actorUserId,
                    "courseId=" + courseId + "; created=" + created + "; updated=" + updated);
        }
        return new BulkResult(created, updated);
    }

    @Transactional
    @EvictTopicCaches
    public Topic updateTopic(Topic topic) {
        return updateTopic(topic, null);
    }

    @Transactional
    @EvictTopicCaches
    public Topic updateTopic(Topic topic, Long actorUserId) {
        validateTopic(topic);
        Topic saved = topicRepository.save(topic);
        logAudit("Topic", saved.getId(), "UPDATE", actorUserId,
                "courseId=" + (saved.getCourse() != null ? saved.getCourse().getId() : null)
                + "; title=\"" + (saved.getTitle() == null ? "" : saved.getTitle()) + "\"");
        return saved;
    }

    @Transactional
    @EvictTopicCaches
    public void deleteTopic(long topicId, boolean force) {
        deleteTopic(topicId, force, null);
    }

    @Transactional
    @EvictTopicCaches
    public void deleteTopic(long topicId, boolean force, Long actorUserId) {
        Topic topic = findTopicOrThrow(topicId);
        long itemCount = studyItemRepository.countByTopicIdAndDeletedFalse(topicId);
        boolean itemsRemoved = false;
        if (itemCount > 0) {
            userProgressRepository.deleteByTopicId(topicId);
            studyItemRepository.softDeleteByTopicId(topicId);
            itemsRemoved = true;
        }
        topicRepository.delete(topic);
        logAudit("Topic", topicId, "DELETE", actorUserId,
                "courseId=" + (topic.getCourse() != null ? topic.getCourse().getId() : null)
                + "; title=\"" + (topic.getTitle() == null ? "" : topic.getTitle()) + "\"; itemsRemoved=" + (itemsRemoved ? itemCount : 0)
                + "; forceRequested=" + force);
    }

    public Optional<Topic> getTopicById(long topicId) {
        return topicRepository.findByIdWithCourse(topicId);
    }

    @Transactional(readOnly = true)
    public TopicSummary getTopicSummary(long topicId, Long userId) {
        Topic topic = findTopicOrThrow(topicId);
        long items = studyItemRepository.countByTopicIdAndDeletedFalse(topicId);
        long studied = (userId == null) ? 0 : userProgressRepository.countStudiedByUserAndTopic(userId, topicId);
        double progressPercent = items == 0 ? 0 : (double) studied * 100.0 / items;
        return new TopicSummary(items, studied, progressPercent, topic.getCreatedAt(), topic.getUpdatedAt());
    }

    @Transactional
    @EvictItemCaches
    public StudyItem createStudyItem(StudyItem item) {
        return createStudyItem(item, null);
    }

    @Transactional
    @EvictItemCaches
    public StudyItem createStudyItem(StudyItem item, Long actorUserId) {
        validateStudyItemRequiredFields(item);
        StudyItem saved = studyItemRepository.save(item);
        logAudit("StudyItem", saved.getId(), "CREATE", actorUserId,
                "topicId=" + (saved.getTopic() != null ? saved.getTopic().getId() : null)
                + "; primary=\"" + (saved.getPrimaryText() == null ? "" : saved.getPrimaryText()) + "\"");
        return saved;
    }

    @Transactional
    @EvictItemCaches
    public BulkResult bulkUpsertStudyItems(long topicId, List<StudyItemUpsert> payloads, boolean dryRun, Long actorUserId) {
        Topic topic = topicRepository.findByIdWithCourse(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (payloads == null || payloads.isEmpty()) {
            return new BulkResult(0, 0);
        }

        int created = 0;
        int updated = 0;
        List<StudyItem> toSave = new ArrayList<>();

        for (StudyItemUpsert dto : payloads) {
            if (dto == null) {
                continue;
            }
            if (dto.id() != null) {
                validateStudyItemUpsert(dto, true);
                StudyItem item = findStudyItemOrThrow(dto.id());
                assertItemBelongsToTopic(item, topicId);
                applyStudyItemUpdates(item, dto);
                updated++;
                toSave.add(item);
            } else {
                validateStudyItemUpsert(dto, false);
                StudyItem item = new StudyItem();
                item.setTopic(topic);
                applyStudyItemUpdates(item, dto);
                created++;
                toSave.add(item);
            }
        }

        if (!dryRun && !toSave.isEmpty()) {
            studyItemRepository.saveAll(toSave);
            logAudit("StudyItem", null, "BULK_UPSERT", actorUserId,
                    "topicId=" + topicId + "; created=" + created + "; updated=" + updated);
        }
        return new BulkResult(created, updated);
    }

    @Transactional
    @EvictItemCaches
    public StudyItem updateStudyItem(StudyItem item) {
        return updateStudyItem(item, null);
    }

    @Transactional
    @EvictItemCaches
    public StudyItem updateStudyItem(StudyItem item, Long actorUserId) {
        validateStudyItemRequiredFields(item);
        StudyItem saved = studyItemRepository.save(item);
        logAudit("StudyItem", saved.getId(), "UPDATE", actorUserId,
                "topicId=" + (saved.getTopic() != null ? saved.getTopic().getId() : null)
                + "; primary=\"" + (saved.getPrimaryText() == null ? "" : saved.getPrimaryText()) + "\"");
        return saved;
    }

    @Transactional
    @EvictItemCaches
    public void deleteStudyItem(long itemId) {
        deleteStudyItem(itemId, null);
    }

    @Transactional
    @EvictItemCaches
    public void deleteStudyItem(long itemId, Long actorUserId) {
        StudyItem item = findStudyItemOrThrow(itemId);
        userProgressRepository.deleteByStudyItemId(itemId);
        studyItemRepository.softDeleteById(itemId);
        logAudit("StudyItem", itemId, "DELETE", actorUserId,
                "topicId=" + (item.getTopic() != null ? item.getTopic().getId() : null)
                + "; primary=\"" + (item.getPrimaryText() == null ? "" : item.getPrimaryText()) + "\"");
    }

    public Optional<StudyItem> getStudyItemById(long itemId) {
        return studyItemRepository.findById(itemId);
    }

    private Course findCourseOrThrow(long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    private Topic findTopicOrThrow(long topicId) {
        return topicRepository.findByIdWithCourse(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
    }

    private StudyItem findStudyItemOrThrow(long itemId) {
        return studyItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Study item not found"));
    }

    public record CourseSummary(long topics, long items, long studied, double progressPercent,
            LocalDateTime createdAt, LocalDateTime updatedAt) {

    }

    public record TopicSummary(long items, long studied, double progressPercent,
            LocalDateTime createdAt, LocalDateTime updatedAt) {

    }

    public record TopicUpsert(Long id, String title, String description, Integer orderIndex) {

    }

    public record StudyItemUpsert(Long id, String primaryText, String secondaryText,
            String meaning, Map<String, String> additionalData) {

    }

    public record BulkResult(int created, int updated) {

    }

    @Transactional
    @CacheEvict(cacheNames = {"topicsByCourse"}, allEntries = true)
    public int reorderTopicsByTitle(long courseId) {
        List<Topic> topics = topicRepository.findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(courseId);
        topics.sort((a, b) -> {
            java.util.regex.Matcher mA = java.util.regex.Pattern.compile("\\d+").matcher(a.getTitle() == null ? "" : a.getTitle());
            java.util.regex.Matcher mB = java.util.regex.Pattern.compile("\\d+").matcher(b.getTitle() == null ? "" : b.getTitle());
            int numA = mA.find() ? Integer.parseInt(mA.group()) : Integer.MAX_VALUE;
            int numB = mB.find() ? Integer.parseInt(mB.group()) : Integer.MAX_VALUE;
            if (numA != numB) {
                return numA - numB;
            }
            return a.getTitle().compareToIgnoreCase(b.getTitle());
        });
        for (int i = 0; i < topics.size(); i++) {
            topics.get(i).setOrderIndex(i);
        }
        topicRepository.saveAll(topics);
        return topics.size();
    }

    private void validateTopic(Topic topic) {
        if (topic.getCourse() == null || topic.getCourse().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course is required for topic");
        }
        if (topic.getTitle() == null || topic.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic title is required");
        }
        ensureOrderIndex(topic);
        validateTopicTitleUnique(topic.getCourse().getId(), topic.getTitle(), topic.getId());
    }

    private void ensureOrderIndex(Topic topic) {
        if (topic.getOrderIndex() == null) {
            int nextIndex = topicRepository.findMaxOrderIndexByCourseId(topic.getCourse().getId());
            topic.setOrderIndex(nextIndex + 1);
        } else if (topic.getOrderIndex() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderIndex must be non-negative");
        }
    }

    private void validateTopicTitleUnique(Long courseId, String title, Long topicId) {
        boolean exists = (topicId == null)
                ? topicRepository.existsByCourseIdAndTitleIgnoreCaseAndDeletedFalse(courseId, title)
                : topicRepository.existsByCourseIdAndTitleIgnoreCaseAndIdNotAndDeletedFalse(courseId, title, topicId);
        if (exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic title already exists in this course");
        }
    }

    private boolean isOwner(Course course, Long userId) {
        return course.getOwner() != null && userId != null && course.getOwner().getId().equals(userId);
    }

    private void assertTopicBelongsToCourse(Topic topic, long courseId) {
        if (topic.getCourse() == null || !topic.getCourse().getId().equals(courseId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic does not belong to this course");
        }
    }

    private void assertItemBelongsToTopic(StudyItem item, long topicId) {
        if (item.getTopic() == null || !item.getTopic().getId().equals(topicId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Study item does not belong to this topic");
        }
    }

    public List<TopicUpsert> parseTopicsCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        String[] lines = csv.split("\\r?\\n");
        int start = (lines.length > 0 && lines[0].toLowerCase().contains("title")) ? 1 : 0;
        List<TopicUpsert> payload = new ArrayList<>();
        for (int i = start; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(",", -1);
            if (parts.length < 1) {
                continue;
            }
            String title = parts[0].trim();
            String description = parts.length > 1 ? parts[1].trim() : null;
            Integer orderIndex = (parts.length > 2) ? parseOptionalInt(parts[2], i + 1) : null;
            payload.add(new TopicUpsert(null, title, description, orderIndex));
        }
        return payload;
    }

    public List<StudyItemUpsert> parseStudyItemsCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        String[] lines = csv.split("\\r?\\n");
        int start = (lines.length > 0 && lines[0].toLowerCase().contains("primary")) ? 1 : 0;
        List<StudyItemUpsert> payload = new ArrayList<>();
        for (int i = start; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(",", -1);
            if (parts.length < 1) {
                continue;
            }
            String primary = parts[0].trim();
            String secondary = parts.length > 1 ? parts[1].trim() : null;
            String meaning = parts.length > 2 ? parts[2].trim() : null;
            payload.add(new StudyItemUpsert(null, primary, secondary, meaning, null));
        }
        return payload;
    }

    private void validateTopicUpsert(TopicUpsert dto, boolean isUpdate) {
        if (!isUpdate || dto.title() != null) {
            if (dto.title() == null || dto.title().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic title is required");
            }
        }
        if (dto.orderIndex() != null && dto.orderIndex() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderIndex must be non-negative");
        }
    }

    private void validateStudyItemUpsert(StudyItemUpsert dto, boolean isUpdate) {
        if (!isUpdate || dto.primaryText() != null) {
            if (dto.primaryText() == null || dto.primaryText().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "primaryText is required");
            }
        }
    }

    private void validateStudyItemRequiredFields(StudyItem item) {
        if (item.getTopic() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Topic is required for study item");
        }
        if (item.getPrimaryText() == null || item.getPrimaryText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "primaryText is required");
        }
    }

    private void logAudit(String entityType, Long entityId, String action, Long actorUserId, String details) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setActorUserId(actorUserId);

        StringBuilder sb = new StringBuilder();
        sb.append("action=").append(action);
        if (entityId != null) {
            sb.append("; entityId=").append(entityId);
        }
        if (actorUserId != null) {
            sb.append("; actorUserId=").append(actorUserId);
        }
        if (details != null && !details.isBlank()) {
            sb.append("; ").append(details);
        }
        log.setDetails(sb.toString());

        auditLogRepository.save(log);
    }

    private boolean hasAccess(Long courseId, Long userId, AccessLevel needed) {
        return courseAccessRepository.findByCourseIdAndUserId(courseId, userId)
                .map(access -> needed == AccessLevel.VIEW ? access.getAccessLevel().allowsView() : access.getAccessLevel().allowsEdit())
                .orElse(false);
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        int defaultSize = 20;
        int maxSize = 100;
        if (size <= 0) {
            return defaultSize;
        }
        return Math.min(size, maxSize);
    }

    private void applyTopicUpdates(Topic topic, TopicUpsert dto) {
        if (dto.title() != null) {
            topic.setTitle(dto.title());
        }
        if (dto.description() != null) {
            topic.setDescription(dto.description());
        }
        if (dto.orderIndex() != null) {
            topic.setOrderIndex(dto.orderIndex());
        }
    }

    private void applyStudyItemUpdates(StudyItem item, StudyItemUpsert dto) {
        if (dto.primaryText() != null) {
            item.setPrimaryText(dto.primaryText());
        }
        if (dto.secondaryText() != null) {
            item.setSecondaryText(dto.secondaryText());
        }
        if (dto.meaning() != null) {
            item.setMeaning(dto.meaning());
        }
        if (dto.additionalData() != null) {
            item.setAdditionalData(dto.additionalData());
        }
    }

    private Integer parseOptionalInt(String raw, int rowNumber) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(trimmed);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid orderIndex at row " + rowNumber);
        }
    }

}
