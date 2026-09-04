package com.aimeetingknowledge.platform.meeting;

import com.aimeetingknowledge.platform.meeting.dto.CreateMeetingRequest;
import com.aimeetingknowledge.platform.meeting.dto.MeetingResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MeetingResponse createMeeting(@Valid @RequestBody CreateMeetingRequest request, Authentication authentication) {
        return meetingService.createMeeting(request, authentication.getName());
    }

    @GetMapping
    public List<MeetingResponse> getMyMeetings(Authentication authentication) {
        return meetingService.getMyMeetings(authentication.getName());
    }

    @GetMapping("/{id}")
    public MeetingResponse getMeetingById(@PathVariable Long id, Authentication authentication) {
        return meetingService.getMeetingById(id, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeeting(@PathVariable Long id, Authentication authentication) {
        meetingService.deleteMeeting(id, authentication.getName());
    }
}
