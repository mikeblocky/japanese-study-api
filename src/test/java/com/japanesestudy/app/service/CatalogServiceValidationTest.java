package com.japanesestudy.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.japanesestudy.app.entity.Course;
import com.japanesestudy.app.entity.StudyItem;
import com.japanesestudy.app.entity.Topic;
import com.japanesestudy.app.repository.CourseAccessRepository;
import com.japanesestudy.app.repository.CourseRepository;
import com.japanesestudy.app.repository.StudyItemRepository;
import com.japanesestudy.app.repository.TopicRepository;
import com.japanesestudy.app.repository.UserProgressRepository;
import com.japanesestudy.app.repository.UserRepository;
import com.japanesestudy.app.service.CatalogService.BulkResult;
import com.japanesestudy.app.service.CatalogService.StudyItemUpsert;
import com.japanesestudy.app.service.CatalogService.TopicUpsert;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class CatalogServiceValidationTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    @SuppressWarnings("unused")
    private CourseAccessRepository courseAccessRepository;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private StudyItemRepository studyItemRepository;
    @Mock
    @SuppressWarnings("unused")
    private UserProgressRepository userProgressRepository;
    @Mock
    @SuppressWarnings("unused")
    private UserRepository userRepository;

    @InjectMocks
    private CatalogService catalogService;

    @Test
    void bulkUpsertStudyItems_missingPrimaryText_throwsBadRequest() {
        long topicId = 1L;
        Topic topic = new Topic();
        topic.setId(topicId);
        when(topicRepository.findByIdWithCourse(topicId)).thenReturn(Optional.of(topic));

        List<StudyItemUpsert> payload = List.of(new StudyItemUpsert(null, " ", null, null, null));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> catalogService.bulkUpsertStudyItems(topicId, payload, false, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void bulkUpsertTopics_negativeOrderIndex_throwsBadRequest() {
        long courseId = 10L;
        Course course = new Course();
        course.setId(courseId);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        List<TopicUpsert> payload = List.of(new TopicUpsert(null, "Lesson 1", "desc", -1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> catalogService.bulkUpsertTopics(courseId, payload, false, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void bulkUpsertTopics_blankTitle_throwsBadRequest() {
        long courseId = 11L;
        Course course = new Course();
        course.setId(courseId);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        List<TopicUpsert> payload = List.of(new TopicUpsert(null, " ", "desc", 0));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> catalogService.bulkUpsertTopics(courseId, payload, false, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createStudyItem_missingTopic_throwsBadRequest() {
        StudyItem item = new StudyItem();
        item.setPrimaryText("Kanji");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> catalogService.createStudyItem(item));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void bulkUpsertTopics_dryRun_skipsPersistence() {
        long courseId = 12L;
        Course course = new Course();
        course.setId(courseId);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(topicRepository.existsByCourseIdAndTitleIgnoreCaseAndDeletedFalse(courseId, "Lesson"))
                .thenReturn(false);

        List<TopicUpsert> payload = List.of(new TopicUpsert(null, "Lesson", "desc", 0));

        BulkResult result = catalogService.bulkUpsertTopics(courseId, payload, true, null);

        assertEquals(1, result.created());
        assertEquals(0, result.updated());
        verify(topicRepository, never()).saveAll(any());
    }

    @Test
    void bulkUpsertStudyItems_dryRun_skipsPersistence() {
        long topicId = 20L;
        Topic topic = new Topic();
        topic.setId(topicId);
        when(topicRepository.findByIdWithCourse(topicId)).thenReturn(Optional.of(topic));

        List<StudyItemUpsert> payload = List.of(new StudyItemUpsert(null, "Word", "", "Meaning", null));

        BulkResult result = catalogService.bulkUpsertStudyItems(topicId, payload, true, null);

        assertEquals(1, result.created());
        assertEquals(0, result.updated());
        verify(studyItemRepository, never()).saveAll(any());
    }
}
