package com.aimeetingknowledge.platform.meeting.audio;

import com.aimeetingknowledge.platform.meeting.audio.dto.MeetingAudioResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/meetings/{meetingId}/audio")
public class MeetingAudioController {

    private final MeetingAudioService meetingAudioService;

    public MeetingAudioController(MeetingAudioService meetingAudioService) {
        this.meetingAudioService = meetingAudioService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public MeetingAudioResponse uploadAudio(
            @PathVariable Long meetingId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication authentication
    ) {
        return meetingAudioService.uploadAudio(meetingId, file, authentication.getName());
    }

    @GetMapping
    public MeetingAudioResponse getAudioMetadata(@PathVariable Long meetingId, Authentication authentication) {
        return meetingAudioService.getAudioMetadata(meetingId, authentication.getName());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAudio(@PathVariable Long meetingId, Authentication authentication) {
        meetingAudioService.deleteAudio(meetingId, authentication.getName());
    }
}
