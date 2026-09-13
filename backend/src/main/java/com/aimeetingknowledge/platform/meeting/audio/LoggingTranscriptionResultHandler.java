package com.aimeetingknowledge.platform.meeting.audio;

import com.aimeetingknowledge.platform.aiservice.dto.TranscriptionResponse;
import com.aimeetingknowledge.platform.meeting.Meeting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingTranscriptionResultHandler implements TranscriptionResultHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingTranscriptionResultHandler.class);

    @Override
    public void handleResult(Meeting meeting, TranscriptionResponse response) {
        log.info("Received transcription result for meeting ID {} [Language: {}]", meeting.getId(), response.language());
    }
}
