package com.aimeetingknowledge.platform.meeting.knowledge;

import com.aimeetingknowledge.platform.aiservice.dto.ActionItemDto;
import com.aimeetingknowledge.platform.aiservice.dto.MeetingAnalysisResponse;
import com.aimeetingknowledge.platform.meeting.Meeting;
import com.aimeetingknowledge.platform.meeting.MeetingService;
import com.aimeetingknowledge.platform.meeting.knowledge.dto.MeetingKnowledgeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class MeetingKnowledgeService {

    private final MeetingService meetingService;
    private final MeetingKnowledgeRepository meetingKnowledgeRepository;

    public MeetingKnowledgeService(
            MeetingService meetingService,
            MeetingKnowledgeRepository meetingKnowledgeRepository
    ) {
        this.meetingService = meetingService;
        this.meetingKnowledgeRepository = meetingKnowledgeRepository;
    }

    @Transactional(readOnly = true)
    public MeetingKnowledgeResponse getKnowledge(Long meetingId, String userEmail) {
        Meeting meeting = meetingService.getOwnedMeeting(meetingId, userEmail);
        MeetingKnowledge knowledge = meetingKnowledgeRepository.findByMeeting(meeting)
                .orElseThrow(MeetingKnowledgeNotFoundException::new);
        return MeetingKnowledgeResponse.from(knowledge);
    }

    @Transactional
    public void saveKnowledgeFromAnalysis(Meeting meeting, MeetingAnalysisResponse analysis) {
        if (meetingKnowledgeRepository.existsByMeeting(meeting)) {
            return;
        }

        MeetingKnowledge knowledge = new MeetingKnowledge(meeting, analysis.summary());

        List<String> keyTopics = analysis.keyTopics();
        if (keyTopics != null) {
            for (int i = 0; i < keyTopics.size(); i++) {
                knowledge.addTopic(new MeetingTopic(knowledge, keyTopics.get(i), i));
            }
        }

        List<String> decisions = analysis.decisions();
        if (decisions != null) {
            for (int i = 0; i < decisions.size(); i++) {
                knowledge.addDecision(new MeetingDecision(knowledge, decisions.get(i), i));
            }
        }

        List<ActionItemDto> actionItems = analysis.actionItems();
        if (actionItems != null) {
            for (int i = 0; i < actionItems.size(); i++) {
                ActionItemDto dto = actionItems.get(i);
                LocalDate dueDate = parseDateSafely(dto.dueDate());
                knowledge.addActionItem(new ActionItem(knowledge, dto.task(), dto.assignee(), dueDate, i));
            }
        }

        meetingKnowledgeRepository.save(knowledge);
    }

    private LocalDate parseDateSafely(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException exc) {
            return null;
        }
    }
}
