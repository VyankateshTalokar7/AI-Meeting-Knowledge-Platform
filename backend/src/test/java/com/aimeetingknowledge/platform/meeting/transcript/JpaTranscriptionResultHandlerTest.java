package com.aimeetingknowledge.platform.meeting.transcript;

import com.aimeetingknowledge.platform.aiservice.dto.SegmentResponse;
import com.aimeetingknowledge.platform.aiservice.dto.TranscriptionResponse;
import com.aimeetingknowledge.platform.meeting.Meeting;
import com.aimeetingknowledge.platform.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JpaTranscriptionResultHandlerTest {

    @Mock
    private MeetingTranscriptRepository meetingTranscriptRepository;

    private JpaTranscriptionResultHandler handler;

    @BeforeEach
    void setUp() {
        handler = new JpaTranscriptionResultHandler(meetingTranscriptRepository);
    }

    @Test
    void handleResultPersistsTranscriptAndSegments() {
        User user = new User("Test User", "test@example.com", "password");
        Meeting meeting = new Meeting(user, "Test Meeting", "Desc", Instant.now());

        List<SegmentResponse> segments = List.of(
                new SegmentResponse(0, 0.0, 2.5, "Hello world"),
                new SegmentResponse(1, 2.5, 5.0, "Second segment")
        );
        TranscriptionResponse response = new TranscriptionResponse("audio.mp3", "Hello world Second segment", "en", segments);

        handler.handleResult(meeting, response);

        ArgumentCaptor<MeetingTranscript> captor = ArgumentCaptor.forClass(MeetingTranscript.class);
        verify(meetingTranscriptRepository).save(captor.capture());

        MeetingTranscript saved = captor.getValue();
        assertEquals(meeting, saved.getMeeting());
        assertEquals("Hello world Second segment", saved.getFullText());
        assertEquals("en", saved.getLanguage());
        assertEquals(2, saved.getSegments().size());

        TranscriptSegment seg0 = saved.getSegments().get(0);
        assertEquals(0, seg0.getSegmentOrder());
        assertEquals(0.0, seg0.getStartTime());
        assertEquals(2.5, seg0.getEndTime());
        assertEquals("Hello world", seg0.getText());
        assertEquals(saved, seg0.getTranscript());

        TranscriptSegment seg1 = saved.getSegments().get(1);
        assertEquals(1, seg1.getSegmentOrder());
        assertEquals(2.5, seg1.getStartTime());
        assertEquals(5.0, seg1.getEndTime());
        assertEquals("Second segment", seg1.getText());
        assertEquals(saved, seg1.getTranscript());
    }
}
