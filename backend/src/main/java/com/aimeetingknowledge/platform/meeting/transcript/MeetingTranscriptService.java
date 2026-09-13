package com.aimeetingknowledge.platform.meeting.transcript;

import com.aimeetingknowledge.platform.meeting.Meeting;
import com.aimeetingknowledge.platform.meeting.MeetingService;
import com.aimeetingknowledge.platform.meeting.transcript.dto.MeetingTranscriptResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeetingTranscriptService {

    private final MeetingService meetingService;
    private final MeetingTranscriptRepository meetingTranscriptRepository;

    public MeetingTranscriptService(
            MeetingService meetingService,
            MeetingTranscriptRepository meetingTranscriptRepository
    ) {
        this.meetingService = meetingService;
        this.meetingTranscriptRepository = meetingTranscriptRepository;
    }

    @Transactional(readOnly = true)
    public MeetingTranscriptResponse getTranscript(Long meetingId, String userEmail) {
        Meeting meeting = meetingService.getOwnedMeeting(meetingId, userEmail);
        MeetingTranscript transcript = meetingTranscriptRepository.findByMeeting(meeting)
                .orElseThrow(TranscriptNotFoundException::new);
        return MeetingTranscriptResponse.from(transcript);
    }
}
