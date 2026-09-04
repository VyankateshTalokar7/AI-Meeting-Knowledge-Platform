package com.aimeetingknowledge.platform.meeting.audio;

import com.aimeetingknowledge.platform.meeting.Meeting;
import com.aimeetingknowledge.platform.meeting.MeetingNotFoundException;
import com.aimeetingknowledge.platform.meeting.MeetingService;
import com.aimeetingknowledge.platform.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingAudioServiceTest {

    private static final String OWNER_EMAIL = "owner@example.com";

    @Mock private MeetingService meetingService;
    @Mock private MeetingAudioRepository meetingAudioRepository;
    @Mock private AudioStorageService audioStorageService;
    @InjectMocks private MeetingAudioService meetingAudioService;

    @Test
    void authenticatedOwnerCanUploadAudio() {
        Meeting meeting = meeting();
        MockMultipartFile file = audioFile();
        AudioStorageService.StoredAudioFile stored = storedAudioFile();
        when(meetingService.getOwnedMeeting(1L, OWNER_EMAIL)).thenReturn(meeting);
        when(meetingAudioRepository.findByMeeting(meeting)).thenReturn(Optional.empty());
        when(audioStorageService.store(file)).thenReturn(stored);
        when(meetingAudioRepository.save(any(MeetingAudio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = meetingAudioService.uploadAudio(1L, file, OWNER_EMAIL);

        ArgumentCaptor<MeetingAudio> audioCaptor = ArgumentCaptor.forClass(MeetingAudio.class);
        verify(meetingAudioRepository).save(audioCaptor.capture());
        assertThat(audioCaptor.getValue().getMeeting()).isSameAs(meeting);
        assertThat(audioCaptor.getValue().getStoredFilename()).isEqualTo("safe-id.mp3");
        assertThat(response.originalFilename()).isEqualTo("recording.mp3");
    }

    @Test
    void userCannotUploadToAnotherUsersMeeting() {
        MockMultipartFile file = audioFile();
        when(meetingService.getOwnedMeeting(99L, OWNER_EMAIL)).thenThrow(new MeetingNotFoundException());

        assertThatThrownBy(() -> meetingAudioService.uploadAudio(99L, file, OWNER_EMAIL))
                .isInstanceOf(MeetingNotFoundException.class);
        verify(audioStorageService, never()).store(any());
    }

    @Test
    void rejectsDuplicateUpload() {
        Meeting meeting = meeting();
        when(meetingService.getOwnedMeeting(1L, OWNER_EMAIL)).thenReturn(meeting);
        when(meetingAudioRepository.findByMeeting(meeting)).thenReturn(Optional.of(existingAudio(meeting)));

        assertThatThrownBy(() -> meetingAudioService.uploadAudio(1L, audioFile(), OWNER_EMAIL))
                .isInstanceOf(AudioAlreadyExistsException.class);
        verify(audioStorageService, never()).store(any());
    }

    @Test
    void ownerCanRetrieveAudioMetadata() {
        Meeting meeting = meeting();
        when(meetingService.getOwnedMeeting(1L, OWNER_EMAIL)).thenReturn(meeting);
        when(meetingAudioRepository.findByMeeting(meeting)).thenReturn(Optional.of(existingAudio(meeting)));

        var response = meetingAudioService.getAudioMetadata(1L, OWNER_EMAIL);

        assertThat(response.originalFilename()).isEqualTo("recording.mp3");
        assertThat(response.fileSize()).isEqualTo(5L);
    }

    @Test
    void userCannotRetrieveAnotherUsersAudioMetadata() {
        when(meetingService.getOwnedMeeting(99L, OWNER_EMAIL)).thenThrow(new MeetingNotFoundException());

        assertThatThrownBy(() -> meetingAudioService.getAudioMetadata(99L, OWNER_EMAIL))
                .isInstanceOf(MeetingNotFoundException.class);
        verify(meetingAudioRepository, never()).findByMeeting(any());
    }

    @Test
    void ownerCanDeleteAudio() {
        Meeting meeting = meeting();
        MeetingAudio audio = existingAudio(meeting);
        when(meetingService.getOwnedMeeting(1L, OWNER_EMAIL)).thenReturn(meeting);
        when(meetingAudioRepository.findByMeeting(meeting)).thenReturn(Optional.of(audio));

        meetingAudioService.deleteAudio(1L, OWNER_EMAIL);

        verify(audioStorageService).delete("safe-id.mp3");
        verify(meetingAudioRepository).delete(audio);
    }

    @Test
    void userCannotDeleteAnotherUsersAudio() {
        when(meetingService.getOwnedMeeting(99L, OWNER_EMAIL)).thenThrow(new MeetingNotFoundException());

        assertThatThrownBy(() -> meetingAudioService.deleteAudio(99L, OWNER_EMAIL))
                .isInstanceOf(MeetingNotFoundException.class);
        verify(meetingAudioRepository, never()).delete(any());
    }

    private Meeting meeting() {
        return new Meeting(new User("Owner", OWNER_EMAIL, "hash"), "Team sync", null, Instant.parse("2030-01-15T10:00:00Z"));
    }

    private MockMultipartFile audioFile() {
        return new MockMultipartFile("file", "recording.mp3", "audio/mpeg", new byte[]{1, 2, 3, 4, 5});
    }

    private AudioStorageService.StoredAudioFile storedAudioFile() {
        return new AudioStorageService.StoredAudioFile("recording.mp3", "safe-id.mp3", "audio/mpeg", 5L, "/safe/audio/safe-id.mp3");
    }

    private MeetingAudio existingAudio(Meeting meeting) {
        return new MeetingAudio(meeting, "recording.mp3", "safe-id.mp3", "audio/mpeg", 5L, "/safe/audio/safe-id.mp3");
    }
}
