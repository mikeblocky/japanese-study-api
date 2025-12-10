package com.japanesestudy.app.service;

import com.japanesestudy.app.dto.TopicProgressDTO;
import com.japanesestudy.app.model.Mastery;
import com.japanesestudy.app.model.StudyItem;
import com.japanesestudy.app.model.User;
import com.japanesestudy.app.repository.MasteryRepository;
import com.japanesestudy.app.repository.StudyItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for calculating study progress metrics.
 */
@Service
public class ProgressService {

    private final StudyItemRepository itemRepository;
    private final MasteryRepository masteryRepository;

    public ProgressService(StudyItemRepository itemRepository, MasteryRepository masteryRepository) {
        this.itemRepository = itemRepository;
        this.masteryRepository = masteryRepository;
    }

    /**
     * Get progress for a specific topic.
     */
    public TopicProgressDTO getTopicProgress(Long topicId, User user) {
        List<StudyItem> items = itemRepository.findAll().stream()
                .filter(i -> i.getTopic() != null && i.getTopic().getId().equals(topicId))
                .toList();

        if (items.isEmpty()) {
            return TopicProgressDTO.empty(topicId);
        }

        List<Long> itemIds = items.stream().map(StudyItem::getId).toList();
        long masteredCount = masteryRepository.findAll().stream()
                .filter(m -> m.getUser().getId().equals(user.getId())
                        && itemIds.contains(m.getItem().getId())
                        && m.getSrsLevel() > 0)
                .count();

        int percentage = (int) ((double) masteredCount / items.size() * 100);
        return new TopicProgressDTO(topicId, items.size(), (int) masteredCount, percentage);
    }

    /**
     * Get progress summary for all topics.
     */
    public List<TopicProgressDTO> getAllTopicProgress(User user) {
        Map<Long, List<StudyItem>> itemsByTopic = itemRepository.findAll().stream()
                .filter(i -> i.getTopic() != null)
                .collect(Collectors.groupingBy(i -> i.getTopic().getId()));

        List<Mastery> userMastery = masteryRepository.findAll().stream()
                .filter(m -> m.getUser().getId().equals(user.getId()))
                .toList();

        return itemsByTopic.entrySet().stream().map(entry -> {
            Long topicId = entry.getKey();
            List<StudyItem> items = entry.getValue();

            long masteredCount = userMastery.stream()
                    .filter(m -> m.getSrsLevel() > 0
                            && items.stream().anyMatch(i -> i.getId().equals(m.getItem().getId())))
                    .count();

            int percentage = items.isEmpty() ? 0 : (int) ((double) masteredCount / items.size() * 100);
            return new TopicProgressDTO(topicId, items.size(), (int) masteredCount, percentage);
        }).toList();
    }
}
