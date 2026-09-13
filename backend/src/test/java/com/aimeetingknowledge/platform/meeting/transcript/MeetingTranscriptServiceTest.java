package com.aimeetingknowledge.platform.meeting.transcript;

import com.aimeetingknowledge.platform.meeting.Meeting;
import com.aimeetingknowledge.platform.meeting.MeetingNotFoundException;
import com.aimeetingknowledge.platform.meeting.MeetingService;
import com.aimeetingknowledge.platform.meeting.transcript.dto.MeetingTranscriptResponse;
import com.aimeetingknowledge.platform.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingTranscriptServiceTest {

    @Mock
    private MeetingService meetingService;

    @Mock
    private MeetingTranscriptRepository meetingTranscriptRepository;

    private MeetingTranscriptService meetingTranscriptService;

    private User user;
    private Meeting meeting;

    @BeforeEach
    void setUp() {
        meetingTranscriptService = new MeetingTranscriptService(meetingService, meetingTranscriptRepository);
        user = new User("Owner", "owner@example.com", "password");
        meeting = new Meeting(user, "Title", "Desc", Instant.now());
    }

    @Test
    void returnsTranscriptForOwnedMeeting() {
        when(meetingService.getOwnedMeeting(1L, "owner@example.com")).thenReturn(meeting);
        MeetingTranscript transcript = new MeetingTranscript(meeting, "Full text transcription", "en");
        transcript.addSegment(new TranscriptSegment(transcript, 0, 0.0, 3.0, "Full text"));
        transcript.addSegment(new TranscriptSegment(transcript, 1, 3.0, 6.0, "transcription"));

        when(meetingTranscriptRepository.findByMeeting(meeting)).thenReturn(Optional.of(transcript));

        MeetingTranscriptResponse response = meetingTranscriptService.getTranscript(1L, "owner@example.com");

        assertNotNull(response);
        assertEquals("Full text transcription", response.fullText());
        assertEquals("en", response.language());
        assertEquals(2, response.segments().size());
        assertEquals("Full text", response.segments().get(0).text());
        assertEquals("transcription", response.segments().get(1).text());
    }

    @Test
    void throwsMeetingNotFoundExceptionWhenMeetingNotOwnedOrNotFound() {
        when(meetingService.getOwnedMeeting(99L, "other@example.com"))
                .thenThrow(new MeetingNotFoundException());

        assertThrows(MeetingNotFoundException.class,
                () -> meetingTranscriptService.getTranscript(99L, "other@example.com"));
    }

    @Test
    void throwsTranscriptNotFoundExceptionWhenTranscriptDoesNotExist() {
        when(meetingService.getOwnedMeeting(1L, "owner@example.com")).thenReturn(meeting);
        when(meetingTranscriptRepository.findByMeeting(meeting)).thenReturn(Optional.empty());

        assertThrows(TranscriptNotFoundException.class,
                () -> meetingTranscriptService.getTranscript(1L, "owner@example.com"));
    }
}
