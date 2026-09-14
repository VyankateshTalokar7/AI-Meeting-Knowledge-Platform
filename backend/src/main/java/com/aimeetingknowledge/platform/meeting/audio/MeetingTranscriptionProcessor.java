package com.aimeetingknowledge.platform.meeting.audio;

import com.aimeetingknowledge.platform.aiservice.AiServiceClient;
import com.aimeetingknowledge.platform.aiservice.dto.TranscriptionResponse;
import com.aimeetingknowledge.platform.meeting.Meeting;
import com.aimeetingknowledge.platform.meeting.MeetingRepository;
import com.aimeetingknowledge.platform.meeting.MeetingService;
import com.aimeetingknowledge.platform.meeting.MeetingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.aimeetingknowledge.platform.aiservice.dto.MeetingAnalysisResponse;
import com.aimeetingknowledge.platform.meeting.knowledge.MeetingKnowledgeService;

@Service
public class MeetingTranscriptionProcessor {

    private static final Logger log = LoggerFactory.getLogger(MeetingTranscriptionProcessor.class);

    private final MeetingRepository meetingRepository;
    private final MeetingService meetingService;
    private final AiServiceClient aiServiceClient;
    private final TranscriptionResultHandler transcriptionResultHandler;
    private final MeetingKnowledgeService meetingKnowledgeService;

    public MeetingTranscriptionProcessor(
            MeetingRepository meetingRepository,
            MeetingService meetingService,
            AiServiceClient aiServiceClient,
            TranscriptionResultHandler transcriptionResultHandler,
            MeetingKnowledgeService meetingKnowledgeService
    ) {
        this.meetingRepository = meetingRepository;
        this.meetingService = meetingService;
        this.aiServiceClient = aiServiceClient;
        this.transcriptionResultHandler = transcriptionResultHandler;
        this.meetingKnowledgeService = meetingKnowledgeService;
    }

    @Async("taskExecutor")
    public void processTranscription(Long meetingId, String filePath) {
        try {
            TranscriptionResponse response = aiServiceClient.transcribeAudio(filePath);
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new IllegalStateException("Meeting not found for ID " + meetingId));
            transcriptionResultHandler.handleResult(meeting, response);

            MeetingAnalysisResponse analysisResponse = aiServiceClient.analyzeMeeting(response.text());
            meetingKnowledgeService.saveKnowledgeFromAnalysis(meeting, analysisResponse);

            meetingService.updateMeetingStatus(meetingId, MeetingStatus.COMPLETED);
        } catch (Exception exc) {
            log.error("Transcription processing failed for meeting ID {}: {}", meetingId, exc.getMessage());
            try {
                meetingService.updateMeetingStatus(meetingId, MeetingStatus.FAILED);
            } catch (Exception updateExc) {
                log.error("Failed to set FAILED status for meeting ID {}: {}", meetingId, updateExc.getMessage());
            }
        }
    }
}
