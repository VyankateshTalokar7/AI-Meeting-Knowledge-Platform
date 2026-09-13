package com.aimeetingknowledge.platform.meeting.audio;

import com.aimeetingknowledge.platform.aiservice.AiServiceClient;
import com.aimeetingknowledge.platform.aiservice.AiServiceException;
import com.aimeetingknowledge.platform.aiservice.dto.SegmentResponse;
import com.aimeetingknowledge.platform.aiservice.dto.TranscriptionResponse;
import com.aimeetingknowledge.platform.meeting.Meeting;
import com.aimeetingknowledge.platform.meeting.MeetingRepository;
import com.aimeetingknowledge.platform.meeting.MeetingService;
import com.aimeetingknowledge.platform.meeting.MeetingStatus;
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
    @InjectMocks private MeetingTranscriptionProcessor processor;

    @Test
    void successfulTranscriptionUpdatesStatusToCompleted() {
        Meeting meeting = new Meeting(new User("User", "user@example.com", "hash"), "Sync", null, Instant.now());
        TranscriptionResponse response = sampleResponse();

        when(aiServiceClient.transcribeAudio("/path/audio.mp3")).thenReturn(response);
        when(meetingRepository.findById(1L)).thenReturn(Optional.of(meeting));

        processor.processTranscription(1L, "/path/audio.mp3");

        verify(transcriptionResultHandler).handleResult(meeting, response);
        verify(meetingService).updateMeetingStatus(1L, MeetingStatus.COMPLETED);
        verify(meetingService, never()).updateMeetingStatus(1L, MeetingStatus.FAILED);
    }

    @Test
    void failedTranscriptionUpdatesStatusToFailed() {
        when(aiServiceClient.transcribeAudio("/path/audio.mp3"))
                .thenThrow(new AiServiceException("Service unavailable"));

        processor.processTranscription(1L, "/path/audio.mp3");

        verify(transcriptionResultHandler, never()).handleResult(any(), any());
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
