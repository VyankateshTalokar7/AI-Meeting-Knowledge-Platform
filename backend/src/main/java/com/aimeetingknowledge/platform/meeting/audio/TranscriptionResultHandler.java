package com.aimeetingknowledge.platform.meeting.audio;

import com.aimeetingknowledge.platform.aiservice.dto.TranscriptionResponse;
import com.aimeetingknowledge.platform.meeting.Meeting;

public interface TranscriptionResultHandler {
    void handleResult(Meeting meeting, TranscriptionResponse response);
}
