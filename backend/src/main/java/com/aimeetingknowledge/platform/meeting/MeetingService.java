package com.aimeetingknowledge.platform.meeting;

import com.aimeetingknowledge.platform.meeting.dto.CreateMeetingRequest;
import com.aimeetingknowledge.platform.meeting.dto.MeetingResponse;
import com.aimeetingknowledge.platform.user.User;
import com.aimeetingknowledge.platform.user.UserNotFoundException;
import com.aimeetingknowledge.platform.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;

    public MeetingService(MeetingRepository meetingRepository, UserRepository userRepository) {
        this.meetingRepository = meetingRepository;
        this.userRepository = userRepository;
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

    public void deleteMeeting(Long id, String email) {
        meetingRepository.delete(getOwnedMeeting(id, email));
    }

    public Meeting getOwnedMeeting(Long id, String email) {
        return meetingRepository.findByIdAndUser(id, currentUser(email))
                .orElseThrow(MeetingNotFoundException::new);
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);
    }
}
