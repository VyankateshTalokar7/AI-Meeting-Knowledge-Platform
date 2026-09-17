package com.aimeetingknowledge.platform.meeting;

import com.aimeetingknowledge.platform.meeting.audio.AudioStorageService;
import com.aimeetingknowledge.platform.meeting.audio.MeetingAudio;
import com.aimeetingknowledge.platform.meeting.audio.MeetingAudioRepository;
import com.aimeetingknowledge.platform.meeting.dto.CreateMeetingRequest;
import com.aimeetingknowledge.platform.meeting.knowledge.MeetingKnowledge;
import com.aimeetingknowledge.platform.meeting.knowledge.MeetingKnowledgeRepository;
import com.aimeetingknowledge.platform.meeting.transcript.MeetingTranscript;
import com.aimeetingknowledge.platform.meeting.transcript.MeetingTranscriptRepository;
import com.aimeetingknowledge.platform.user.User;
import com.aimeetingknowledge.platform.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    private static final String OWNER_EMAIL = "owner@example.com";

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MeetingAudioRepository meetingAudioRepository;

    @Mock
    private AudioStorageService audioStorageService;

    @Mock
    private MeetingTranscriptRepository meetingTranscriptRepository;

    @Mock
    private MeetingKnowledgeRepository meetingKnowledgeRepository;

    @InjectMocks
    private MeetingService meetingService;

    @Test
    void authenticatedUserCanCreateMeeting() {
        User owner = new User("Owner", OWNER_EMAIL, "hash");
        CreateMeetingRequest request = new CreateMeetingRequest(
                " Team sync ", " Weekly planning ", Instant.parse("2030-01-15T10:00:00Z")
        );
        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = meetingService.createMeeting(request, OWNER_EMAIL);

        ArgumentCaptor<Meeting> meetingCaptor = ArgumentCaptor.forClass(Meeting.class);
        verify(meetingRepository).save(meetingCaptor.capture());
        assertThat(meetingCaptor.getValue().getUser()).isSameAs(owner);
        assertThat(meetingCaptor.getValue().getTitle()).isEqualTo("Team sync");
        assertThat(meetingCaptor.getValue().getDescription()).isEqualTo("Weekly planning");
        assertThat(meetingCaptor.getValue().getStatus()).isEqualTo(MeetingStatus.CREATED);
        assertThat(response.title()).isEqualTo("Team sync");
    }

    @Test
    void listsOnlyMeetingsOwnedByCurrentUser() {
        User owner = new User("Owner", OWNER_EMAIL, "hash");
        Meeting ownedMeeting = new Meeting(owner, "Owner meeting", null, Instant.parse("2030-01-15T10:00:00Z"));
        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(meetingRepository.findAllByUserOrderByMeetingDateDesc(owner)).thenReturn(List.of(ownedMeeting));

        var meetings = meetingService.getMyMeetings(OWNER_EMAIL);

        verify(meetingRepository).findAllByUserOrderByMeetingDateDesc(owner);
        assertThat(meetings).hasSize(1);
        assertThat(meetings.getFirst().title()).isEqualTo("Owner meeting");
    }

    @Test
    void userCannotAccessAnotherUsersMeeting() {
        User owner = new User("Owner", OWNER_EMAIL, "hash");
        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(meetingRepository.findByIdAndUser(99L, owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingService.getMeetingById(99L, OWNER_EMAIL))
                .isInstanceOf(MeetingNotFoundException.class);
    }

    @Test
    void userCannotDeleteAnotherUsersMeeting() {
        User owner = new User("Owner", OWNER_EMAIL, "hash");
        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(meetingRepository.findByIdAndUser(99L, owner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingService.deleteMeeting(99L, OWNER_EMAIL))
                .isInstanceOf(MeetingNotFoundException.class);
    }

    @Test
    void userCanDeleteOwnedMeetingAndChildEntities() {
        User owner = new User("Owner", OWNER_EMAIL, "hash");
        Meeting ownedMeeting = new Meeting(owner, "Meeting to delete", null, Instant.parse("2030-01-15T10:00:00Z"));
        MeetingAudio audio = new MeetingAudio(ownedMeeting, "orig.wav", "stored.wav", "audio/wav", 100, "/path");
        MeetingTranscript transcript = new MeetingTranscript(ownedMeeting, "Text", "en");
        MeetingKnowledge knowledge = new MeetingKnowledge(ownedMeeting, "Summary");

        when(userRepository.findByEmail(OWNER_EMAIL)).thenReturn(Optional.of(owner));
        when(meetingRepository.findByIdAndUser(1L, owner)).thenReturn(Optional.of(ownedMeeting));
        when(meetingAudioRepository.findByMeeting(ownedMeeting)).thenReturn(Optional.of(audio));
        when(meetingTranscriptRepository.findByMeeting(ownedMeeting)).thenReturn(Optional.of(transcript));
        when(meetingKnowledgeRepository.findByMeeting(ownedMeeting)).thenReturn(Optional.of(knowledge));

        meetingService.deleteMeeting(1L, OWNER_EMAIL);

        verify(audioStorageService).delete("stored.wav");
        verify(meetingAudioRepository).delete(audio);
        verify(meetingTranscriptRepository).delete(transcript);
        verify(meetingKnowledgeRepository).delete(knowledge);
        verify(meetingRepository).delete(ownedMeeting);
        verify(userRepository, never()).delete(any());
    }
}
