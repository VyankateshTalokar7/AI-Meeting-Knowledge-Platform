package com.aimeetingknowledge.platform.aiservice;

import com.aimeetingknowledge.platform.aiservice.dto.MeetingAnalysisResponse;
import com.aimeetingknowledge.platform.aiservice.dto.TranscriptionResponse;

public interface AiServiceClient {
    TranscriptionResponse transcribeAudio(String filePath);
    MeetingAnalysisResponse analyzeMeeting(String text);
}
