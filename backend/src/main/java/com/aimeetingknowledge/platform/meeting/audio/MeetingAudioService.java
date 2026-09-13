package com.aimeetingknowledge.platform.meeting.audio;

import com.aimeetingknowledge.platform.meeting.Meeting;
import com.aimeetingknowledge.platform.meeting.MeetingService;
import com.aimeetingknowledge.platform.meeting.MeetingStatus;
import com.aimeetingknowledge.platform.meeting.audio.dto.MeetingAudioResponse;
import com.aimeetingknowledge.platform.meeting.audio.event.TranscriptionRequestedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MeetingAudioService {

    private final MeetingService meetingService;
    private final MeetingAudioRepository meetingAudioRepository;
    private final AudioStorageService audioStorageService;
    private final ApplicationEventPublisher eventPublisher;

    public MeetingAudioService(
            MeetingService meetingService,
            MeetingAudioRepository meetingAudioRepository,
            AudioStorageService audioStorageService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.meetingService = meetingService;
        this.meetingAudioRepository = meetingAudioRepository;
        this.audioStorageService = audioStorageService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public MeetingAudioResponse uploadAudio(Long meetingId, MultipartFile file, String email) {
        Meeting meeting = meetingService.getOwnedMeeting(meetingId, email);
        if (meetingAudioRepository.findByMeeting(meeting).isPresent()) {
            throw new AudioAlreadyExistsException();
        }

        AudioStorageService.StoredAudioFile storedFile = audioStorageService.store(file);
        try {
            MeetingAudio audio = new MeetingAudio(
                    meeting,
                    storedFile.originalFilename(),
                    storedFile.storedFilename(),
                    storedFile.contentType(),
                    storedFile.fileSize(),
                    storedFile.storagePath()
            );

            MeetingAudio savedAudio = meetingAudioRepository.save(audio);
            meetingService.updateMeetingStatus(meeting.getId(), MeetingStatus.PROCESSING);
            eventPublisher.publishEvent(new TranscriptionRequestedEvent(meeting.getId(), storedFile.storagePath()));

            return MeetingAudioResponse.from(savedAudio);
        } catch (RuntimeException exception) {
            audioStorageService.delete(storedFile.storedFilename());
            throw exception;
        }
    }

    public MeetingAudioResponse getAudioMetadata(Long meetingId, String email) {
        return MeetingAudioResponse.from(findOwnedAudio(meetingId, email));
    }

    public void deleteAudio(Long meetingId, String email) {
        MeetingAudio audio = findOwnedAudio(meetingId, email);
        audioStorageService.delete(audio.getStoredFilename());
        meetingAudioRepository.delete(audio);
    }

    private MeetingAudio findOwnedAudio(Long meetingId, String email) {
        Meeting meeting = meetingService.getOwnedMeeting(meetingId, email);
        return meetingAudioRepository.findByMeeting(meeting)
                .orElseThrow(AudioNotFoundException::new);
    }
}
