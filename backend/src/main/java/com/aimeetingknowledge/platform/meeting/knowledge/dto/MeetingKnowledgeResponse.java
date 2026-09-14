package com.aimeetingknowledge.platform.meeting.knowledge.dto;

import com.aimeetingknowledge.platform.meeting.knowledge.MeetingDecision;
import com.aimeetingknowledge.platform.meeting.knowledge.MeetingKnowledge;
import com.aimeetingknowledge.platform.meeting.knowledge.MeetingTopic;

import java.time.Instant;
import java.util.List;

public record MeetingKnowledgeResponse(
        Long id,
        Long meetingId,
        String summary,
        List<String> keyTopics,
        List<String> decisions,
        List<ActionItemResponse> actionItems,
        Instant createdAt,
        Instant updatedAt
) {
    public static MeetingKnowledgeResponse from(MeetingKnowledge knowledge) {
        List<String> topics = knowledge.getTopics().stream()
                .map(MeetingTopic::getTopic)
                .toList();

        List<String> decisions = knowledge.getDecisions().stream()
                .map(MeetingDecision::getDecision)
                .toList();

        List<ActionItemResponse> actionItems = knowledge.getActionItems().stream()
                .map(ActionItemResponse::from)
                .toList();

        return new MeetingKnowledgeResponse(
                knowledge.getId(),
                knowledge.getMeeting().getId(),
                knowledge.getSummary(),
                topics,
                decisions,
                actionItems,
                knowledge.getCreatedAt(),
                knowledge.getUpdatedAt()
        );
    }
}
