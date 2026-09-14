package com.aimeetingknowledge.platform.meeting.knowledge;

import com.aimeetingknowledge.platform.aiservice.dto.ActionItemDto;
import com.aimeetingknowledge.platform.aiservice.dto.MeetingAnalysisResponse;
import com.aimeetingknowledge.platform.meeting.Meeting;
import com.aimeetingknowledge.platform.meeting.MeetingNotFoundException;
import com.aimeetingknowledge.platform.meeting.MeetingService;
import com.aimeetingknowledge.platform.meeting.knowledge.dto.MeetingKnowledgeResponse;
import com.aimeetingknowledge.platform.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingKnowledgeServiceTest {

    @Mock
    private MeetingService meetingService;

    @Mock
    private MeetingKnowledgeRepository meetingKnowledgeRepository;

    private MeetingKnowledgeService service;
    private User user;
    private Meeting meeting;

    @BeforeEach
    void setUp() {
        service = new MeetingKnowledgeService(meetingService, meetingKnowledgeRepository);
        user = new User("Owner", "owner@example.com", "password");
        meeting = new Meeting(user, "Title", "Desc", Instant.now());
    }

    @Test
    void returnsKnowledgeForOwnedMeeting() {
        when(meetingService.getOwnedMeeting(1L, "owner@example.com")).thenReturn(meeting);
        MeetingKnowledge knowledge = new MeetingKnowledge(meeting, "Summary text.");
        knowledge.addTopic(new MeetingTopic(knowledge, "Topic 1", 0));
        knowledge.addDecision(new MeetingDecision(knowledge, "Decision 1", 0));
        knowledge.addActionItem(new ActionItem(knowledge, "Task 1", "John", LocalDate.parse("2026-09-20"), 0));

        when(meetingKnowledgeRepository.findByMeeting(meeting)).thenReturn(Optional.of(knowledge));

        MeetingKnowledgeResponse response = service.getKnowledge(1L, "owner@example.com");

        assertNotNull(response);
        assertEquals("Summary text.", response.summary());
        assertEquals(List.of("Topic 1"), response.keyTopics());
        assertEquals(List.of("Decision 1"), response.decisions());
        assertEquals(1, response.actionItems().size());
        assertEquals("Task 1", response.actionItems().get(0).task());
        assertEquals("John", response.actionItems().get(0).assignee());
        assertEquals(LocalDate.parse("2026-09-20"), response.actionItems().get(0).dueDate());
    }

    @Test
    void throwsMeetingNotFoundExceptionWhenUnownedMeeting() {
        when(meetingService.getOwnedMeeting(99L, "other@example.com")).thenThrow(new MeetingNotFoundException());

        assertThrows(MeetingNotFoundException.class, () -> service.getKnowledge(99L, "other@example.com"));
    }

    @Test
    void throwsKnowledgeNotFoundExceptionWhenKnowledgeMissing() {
        when(meetingService.getOwnedMeeting(1L, "owner@example.com")).thenReturn(meeting);
        when(meetingKnowledgeRepository.findByMeeting(meeting)).thenReturn(Optional.empty());

        assertThrows(MeetingKnowledgeNotFoundException.class, () -> service.getKnowledge(1L, "owner@example.com"));
    }

    @Test
    void savesKnowledgeFromAnalysis() {
        when(meetingKnowledgeRepository.existsByMeeting(meeting)).thenReturn(false);

        MeetingAnalysisResponse analysis = new MeetingAnalysisResponse(
                "Executive Summary",
                List.of("Architecture", "Security"),
                List.of("Approved V1 design"),
                List.of(
                        new ActionItemDto("Build API", "Alice", "2026-10-01"),
                        new ActionItemDto("Write docs", null, "invalid-date")
                )
        );

        service.saveKnowledgeFromAnalysis(meeting, analysis);

        ArgumentCaptor<MeetingKnowledge> captor = ArgumentCaptor.forClass(MeetingKnowledge.class);
        verify(meetingKnowledgeRepository).save(captor.capture());

        MeetingKnowledge saved = captor.getValue();
        assertEquals("Executive Summary", saved.getSummary());
        assertEquals(2, saved.getTopics().size());
        assertEquals("Architecture", saved.getTopics().get(0).getTopic());
        assertEquals("Security", saved.getTopics().get(1).getTopic());
        assertEquals(1, saved.getDecisions().size());
        assertEquals("Approved V1 design", saved.getDecisions().get(0).getDecision());
        assertEquals(2, saved.getActionItems().size());

        ActionItem item0 = saved.getActionItems().get(0);
        assertEquals("Build API", item0.getTask());
        assertEquals("Alice", item0.getAssignee());
        assertEquals(LocalDate.parse("2026-10-01"), item0.getDueDate());

        ActionItem item1 = saved.getActionItems().get(1);
        assertEquals("Write docs", item1.getTask());
        assertNull(item1.getAssignee());
        assertNull(item1.getDueDate());
    }

    @Test
    void preventsDuplicateKnowledgeSave() {
        when(meetingKnowledgeRepository.existsByMeeting(meeting)).thenReturn(true);

        MeetingAnalysisResponse analysis = new MeetingAnalysisResponse("Summary", List.of(), List.of(), List.of());
        service.saveKnowledgeFromAnalysis(meeting, analysis);

        verify(meetingKnowledgeRepository, never()).save(any());
    }
}
