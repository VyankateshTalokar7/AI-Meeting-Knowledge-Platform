package com.aimeetingknowledge.platform.meeting;

import com.aimeetingknowledge.platform.meeting.audio.AudioStorageService;
import com.aimeetingknowledge.platform.meeting.audio.MeetingAudioRepository;
import com.aimeetingknowledge.platform.meeting.dto.CreateMeetingRequest;
import com.aimeetingknowledge.platform.meeting.dto.MeetingResponse;
import com.aimeetingknowledge.platform.meeting.knowledge.MeetingKnowledgeRepository;
import com.aimeetingknowledge.platform.meeting.transcript.MeetingTranscriptRepository;
import com.aimeetingknowledge.platform.user.User;
import com.aimeetingknowledge.platform.user.UserNotFoundException;
import com.aimeetingknowledge.platform.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final MeetingAudioRepository meetingAudioRepository;
    private final AudioStorageService audioStorageService;
    private final MeetingTranscriptRepository meetingTranscriptRepository;
    private final MeetingKnowledgeRepository meetingKnowledgeRepository;

    public MeetingService(
            MeetingRepository meetingRepository,
            UserRepository userRepository,
            MeetingAudioRepository meetingAudioRepository,
            AudioStorageService audioStorageService,
            MeetingTranscriptRepository meetingTranscriptRepository,
            MeetingKnowledgeRepository meetingKnowledgeRepository
    ) {
        this.meetingRepository = meetingRepository;
        this.userRepository = userRepository;
        this.meetingAudioRepository = meetingAudioRepository;
        this.audioStorageService = audioStorageService;
        this.meetingTranscriptRepository = meetingTranscriptRepository;
        this.meetingKnowledgeRepository = meetingKnowledgeRepository;
    }

    public MeetingResponse createMeeting(CreateMeetingRequest request, String email) {
        User user = currentUser(email);
        String description = request.description() == null ? null : request.description().trim();
        Meeting meeting = new Meeting(user, request.title().trim(), description, request.meetingDate());
        return MeetingResponse.from(meetingRepository.save(meeting));
    }

    public List<MeetingResponse> getMyMeetings(String email) {
        User user = currentUser(email);
        return meetingRepository.findAllByUserOrderByMeetingDateDesc(user).stream()
                .map(MeetingResponse::from)
                .toList();
    }

    public MeetingResponse getMeetingById(Long id, String email) {
        return MeetingResponse.from(getOwnedMeeting(id, email));
    }

    @Transactional
    public void deleteMeeting(Long id, String email) {
        Meeting meeting = getOwnedMeeting(id, email);

        meetingAudioRepository.findByMeeting(meeting).ifPresent(audio -> {
            audioStorageService.delete(audio.getStoredFilename());
            meetingAudioRepository.delete(audio);
            meetingAudioRepository.flush();
        });

        meetingTranscriptRepository.findByMeeting(meeting).ifPresent(transcript -> {
            meetingTranscriptRepository.delete(transcript);
            meetingTranscriptRepository.flush();
        });

        meetingKnowledgeRepository.findByMeeting(meeting).ifPresent(knowledge -> {
            meetingKnowledgeRepository.delete(knowledge);
            meetingKnowledgeRepository.flush();
        });

        meetingRepository.delete(meeting);
    }

    public Meeting getOwnedMeeting(Long id, String email) {
        return meetingRepository.findByIdAndUser(id, currentUser(email))
                .orElseThrow(MeetingNotFoundException::new);
    }

    @Transactional
    public void updateMeetingStatus(Long meetingId, MeetingStatus status) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(MeetingNotFoundException::new);
        meeting.setStatus(status);
        meetingRepository.save(meeting);
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);
    }
}
