package com.aimeetingknowledge.platform.meeting.audio;

import com.aimeetingknowledge.platform.aiservice.AiServiceClient;
import com.aimeetingknowledge.platform.aiservice.AiServiceException;
import com.aimeetingknowledge.platform.aiservice.dto.MeetingAnalysisResponse;
import com.aimeetingknowledge.platform.aiservice.dto.SegmentResponse;
import com.aimeetingknowledge.platform.aiservice.dto.TranscriptionResponse;
import com.aimeetingknowledge.platform.meeting.Meeting;
import com.aimeetingknowledge.platform.meeting.MeetingRepository;
import com.aimeetingknowledge.platform.meeting.MeetingService;
import com.aimeetingknowledge.platform.meeting.MeetingStatus;
import com.aimeetingknowledge.platform.meeting.knowledge.MeetingKnowledgeService;
import com.aimeetingknowledge.platform.meeting.transcript.MeetingTranscript;
import com.aimeetingknowledge.platform.meeting.transcript.MeetingTranscriptRepository;
import com.aimeetingknowledge.platform.meeting.transcript.TranscriptSegment;
import com.aimeetingknowledge.platform.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingTranscriptionProcessorTest {

    @Mock private MeetingRepository meetingRepository;
    @Mock private MeetingService meetingService;
    @Mock private AiServiceClient aiServiceClient;
    @Mock private TranscriptionResultHandler transcriptionResultHandler;
    @Mock private MeetingKnowledgeService meetingKnowledgeService;
    @Mock private MeetingTranscriptRepository meetingTranscriptRepository;
    @InjectMocks private MeetingTranscriptionProcessor processor;

    @Test
    void successfulTranscriptionAnalysisAndIndexingUpdatesStatusToCompleted() {
        Meeting meeting = new Meeting(new User("User", "user@example.com", "hash"), "Sync", null, Instant.now());
        TranscriptionResponse transcriptionResponse = sampleResponse();
        MeetingAnalysisResponse analysisResponse = new MeetingAnalysisResponse("Summary", List.of(), List.of(), List.of());
        MeetingTranscript transcript = new MeetingTranscript(meeting, "Hello world", "en");
        transcript.addSegment(new TranscriptSegment(transcript, 0, 0.0, 1.5, "Hello world"));

        when(aiServiceClient.transcribeAudio("/path/audio.mp3")).thenReturn(transcriptionResponse);
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(aiServiceClient.analyzeMeeting("Hello world")).thenReturn(analysisResponse);
        when(meetingTranscriptRepository.findByMeeting(meeting)).thenReturn(Optional.of(transcript));

        processor.processTranscription(1L, "/path/audio.mp3");

        verify(transcriptionResultHandler).handleResult(meeting, transcriptionResponse);
        verify(meetingKnowledgeService).saveKnowledgeFromAnalysis(meeting, analysisResponse);
        verify(aiServiceClient).indexTranscript(any());
        verify(meetingService).updateMeetingStatus(1L, MeetingStatus.COMPLETED);
        verify(meetingService, never()).updateMeetingStatus(1L, MeetingStatus.FAILED);
    }

    @Test
    void indexingFailureDoesNotChangeCompletedStatus() {
        Meeting meeting = new Meeting(new User("User", "user@example.com", "hash"), "Sync", null, Instant.now());
        TranscriptionResponse transcriptionResponse = sampleResponse();
        MeetingAnalysisResponse analysisResponse = new MeetingAnalysisResponse("Summary", List.of(), List.of(), List.of());
        MeetingTranscript transcript = new MeetingTranscript(meeting, "Hello world", "en");
        transcript.addSegment(new TranscriptSegment(transcript, 0, 0.0, 1.5, "Hello world"));

        when(aiServiceClient.transcribeAudio("/path/audio.mp3")).thenReturn(transcriptionResponse);
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(aiServiceClient.analyzeMeeting("Hello world")).thenReturn(analysisResponse);
        when(meetingTranscriptRepository.findByMeeting(meeting)).thenReturn(Optional.of(transcript));
        when(aiServiceClient.indexTranscript(any())).thenThrow(new AiServiceException("Vector store offline"));

        processor.processTranscription(1L, "/path/audio.mp3");

        verify(transcriptionResultHandler).handleResult(meeting, transcriptionResponse);
        verify(meetingKnowledgeService).saveKnowledgeFromAnalysis(meeting, analysisResponse);
        verify(aiServiceClient).indexTranscript(any());
        verify(meetingService).updateMeetingStatus(1L, MeetingStatus.COMPLETED);
        verify(meetingService, never()).updateMeetingStatus(1L, MeetingStatus.FAILED);
    }

    @Test
    void failedTranscriptionUpdatesStatusToFailed() {
        when(aiServiceClient.transcribeAudio("/path/audio.mp3"))
                .thenThrow(new AiServiceException("Service unavailable"));

        processor.processTranscription(1L, "/path/audio.mp3");

        verify(transcriptionResultHandler, never()).handleResult(any(), any());
        verify(meetingKnowledgeService, never()).saveKnowledgeFromAnalysis(any(), any());
        verify(meetingService).updateMeetingStatus(1L, MeetingStatus.FAILED);
        verify(meetingService, never()).updateMeetingStatus(1L, MeetingStatus.COMPLETED);
    }

    @Test
    void failedLlmAnalysisLeavesTranscriptStoredAndSetsStatusToFailed() {
        Meeting meeting = new Meeting(new User("User", "user@example.com", "hash"), "Sync", null, Instant.now());
        TranscriptionResponse transcriptionResponse = sampleResponse();

        when(aiServiceClient.transcribeAudio("/path/audio.mp3")).thenReturn(transcriptionResponse);
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        when(aiServiceClient.analyzeMeeting("Hello world")).thenThrow(new AiServiceException("LLM API failed"));

        processor.processTranscription(1L, "/path/audio.mp3");

        // Verify transcript persistence DID execute
        verify(transcriptionResultHandler).handleResult(meeting, transcriptionResponse);
        // Verify knowledge save was NOT called
        verify(meetingKnowledgeService, never()).saveKnowledgeFromAnalysis(any(), any());
        // Verify status becomes FAILED
        verify(meetingService).updateMeetingStatus(1L, MeetingStatus.FAILED);
        verify(meetingService, never()).updateMeetingStatus(1L, MeetingStatus.COMPLETED);
    }

    @Test
    void resultHandlerFailureUpdatesStatusToFailedAndNeverCompleted() {
        Meeting meeting = new Meeting(new User("User", "user@example.com", "hash"), "Sync", null, Instant.now());
        TranscriptionResponse response = sampleResponse();

        when(aiServiceClient.transcribeAudio("/path/audio.mp3")).thenReturn(response);
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));
        doThrow(new RuntimeException("Result handler exception"))
                .when(transcriptionResultHandler).handleResult(meeting, response);

        processor.processTranscription(1L, "/path/audio.mp3");

        verify(meetingService).updateMeetingStatus(1L, MeetingStatus.FAILED);
        verify(meetingService, never()).updateMeetingStatus(1L, MeetingStatus.COMPLETED);
    }

    @Test
    void missingMeetingNeverUpdatesStatusToCompleted() {
        TranscriptionResponse response = sampleResponse();

        when(aiServiceClient.transcribeAudio("/path/audio.mp3")).thenReturn(response);
        when(meetingRepository.findById(99L)).thenReturn(Optional.empty());

        processor.processTranscription(99L, "/path/audio.mp3");

        verify(meetingService).updateMeetingStatus(99L, MeetingStatus.FAILED);
        verify(meetingService, never()).updateMeetingStatus(99L, MeetingStatus.COMPLETED);
        verify(transcriptionResultHandler, never()).handleResult(any(), any());
    }

    private TranscriptionResponse sampleResponse() {
        return new TranscriptionResponse(
                "recording.mp3",
                "Hello world",
                "en",
                List.of(new SegmentResponse(0, 0.0, 1.5, "Hello world"))
        );
    }
}
