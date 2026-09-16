package com.aimeetingknowledge.platform.aiservice;

import com.aimeetingknowledge.platform.aiservice.dto.IndexTranscriptRequest;
import com.aimeetingknowledge.platform.aiservice.dto.IndexTranscriptResponse;
import com.aimeetingknowledge.platform.aiservice.dto.MeetingAnalysisResponse;
import com.aimeetingknowledge.platform.aiservice.dto.TranscriptionResponse;

import com.aimeetingknowledge.platform.aiservice.dto.SearchAiServiceRequest;
import com.aimeetingknowledge.platform.aiservice.dto.SearchAiServiceResponse;

public interface AiServiceClient {
    TranscriptionResponse transcribeAudio(String filePath);
    MeetingAnalysisResponse analyzeMeeting(String text);
    IndexTranscriptResponse indexTranscript(IndexTranscriptRequest request);
    SearchAiServiceResponse search(SearchAiServiceRequest request);
}
