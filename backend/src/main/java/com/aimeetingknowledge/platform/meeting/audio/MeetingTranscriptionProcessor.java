package com.aimeetingknowledge.platform.meeting.audio;

import com.aimeetingknowledge.platform.aiservice.AiServiceClient;

import com.aimeetingknowledge.platform.aiservice.dto.IndexTranscriptRequest;
import com.aimeetingknowledge.platform.aiservice.dto.MeetingAnalysisResponse;
import com.aimeetingknowledge.platform.aiservice.dto.SegmentDto;
import com.aimeetingknowledge.platform.aiservice.dto.TranscriptionResponse;
import com.aimeetingknowledge.platform.meeting.Meeting;
import com.aimeetingknowledge.platform.meeting.MeetingRepository;
import com.aimeetingknowledge.platform.meeting.MeetingService;
import com.aimeetingknowledge.platform.meeting.MeetingStatus;
import com.aimeetingknowledge.platform.meeting.knowledge.MeetingKnowledgeService;
import com.aimeetingknowledge.platform.meeting.transcript.MeetingTranscript;
import com.aimeetingknowledge.platform.meeting.transcript.MeetingTranscriptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MeetingTranscriptionProcessor {

    private static final Logger log = LoggerFactory.getLogger(MeetingTranscriptionProcessor.class);

    private final MeetingRepository meetingRepository;
    private final MeetingService meetingService;
    private final AiServiceClient aiServiceClient;
    private final TranscriptionResultHandler transcriptionResultHandler;
    private final MeetingKnowledgeService meetingKnowledgeService;
    private final MeetingTranscriptRepository meetingTranscriptRepository;

    public MeetingTranscriptionProcessor(
            MeetingRepository meetingRepository,
            MeetingService meetingService,
            AiServiceClient aiServiceClient,
            TranscriptionResultHandler transcriptionResultHandler,
            MeetingKnowledgeService meetingKnowledgeService,
            MeetingTranscriptRepository meetingTranscriptRepository
    ) {
        this.meetingRepository = meetingRepository;
        this.meetingService = meetingService;
        this.aiServiceClient = aiServiceClient;
        this.transcriptionResultHandler = transcriptionResultHandler;
        this.meetingKnowledgeService = meetingKnowledgeService;
        this.meetingTranscriptRepository = meetingTranscriptRepository;
    }

    @Async("taskExecutor")
    public void processTranscription(Long meetingId, String filePath) {
        log.info("Starting transcription processing for meeting ID: {}, audio path: {}", meetingId, filePath);
        try {
            TranscriptionResponse response = aiServiceClient.transcribeAudio(filePath);
            log.info("Transcription completed for meeting ID {}", meetingId);

            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElseThrow(() -> new IllegalStateException("Meeting not found for ID " + meetingId));
            transcriptionResultHandler.handleResult(meeting, response);
            log.info("Transcription result stored for meeting ID {}", meetingId);

            MeetingAnalysisResponse analysisResponse = aiServiceClient.analyzeMeeting(response.text());
            log.info("LLM analysis completed for meeting ID {}", meetingId);
            meetingKnowledgeService.saveKnowledgeFromAnalysis(meeting, analysisResponse);
            log.info("Meeting knowledge stored for meeting ID {}", meetingId);

            try {
                MeetingTranscript transcript = meetingTranscriptRepository.findByMeeting(meeting).orElse(null);
                if (transcript != null) {
                    List<SegmentDto> segmentDtos = transcript.getSegments().stream()
                            .map(seg -> new SegmentDto(
                                    seg.getSegmentOrder(),
                                    seg.getStartTime(),
                                    seg.getEndTime(),
                                    seg.getText()
                            ))
                            .toList();

                    IndexTranscriptRequest indexRequest = new IndexTranscriptRequest(
                            meeting.getId(),
                            transcript.getId(),
                            transcript.getLanguage(),
                            segmentDtos
                    );

                    aiServiceClient.indexTranscript(indexRequest);
                    log.info("Vector indexing completed for meeting ID {}", meetingId);
                } else {
                    log.warn("MeetingTranscript not found for meeting ID {}, skipping vector indexing", meetingId);
                }
            } catch (Exception indexingExc) {
                log.warn("Vector indexing failed for meeting ID {}: {}", meetingId, indexingExc.getMessage(), indexingExc);
            }

            meetingService.updateMeetingStatus(meetingId, MeetingStatus.COMPLETED);
            log.info("Meeting status updated to COMPLETED for meeting ID {}", meetingId);
        } catch (Exception exc) {
            log.error("Transcription processing failed for meeting ID {}: {}", meetingId, exc.getMessage(), exc);
            try {
                meetingService.updateMeetingStatus(meetingId, MeetingStatus.FAILED);
            } catch (Exception updateExc) {
                log.error("Failed to set FAILED status for meeting ID {}: {}", meetingId, updateExc.getMessage());
            }
        }
    }
}
