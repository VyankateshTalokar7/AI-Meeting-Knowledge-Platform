package com.aimeetingknowledge.platform.meeting.transcript;

import com.aimeetingknowledge.platform.meeting.transcript.dto.MeetingTranscriptResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/meetings")
public class MeetingTranscriptController {

    private final MeetingTranscriptService meetingTranscriptService;

    public MeetingTranscriptController(MeetingTranscriptService meetingTranscriptService) {
        this.meetingTranscriptService = meetingTranscriptService;
    }

    @GetMapping("/{meetingId}/transcript")
    public MeetingTranscriptResponse getTranscript(@PathVariable Long meetingId, Principal principal) {
        return meetingTranscriptService.getTranscript(meetingId, principal.getName());
    }
}
