package com.aimeetingknowledge.platform.meeting.transcript;

import com.aimeetingknowledge.platform.aiservice.dto.SegmentResponse;
import com.aimeetingknowledge.platform.aiservice.dto.TranscriptionResponse;
import com.aimeetingknowledge.platform.meeting.Meeting;
import com.aimeetingknowledge.platform.meeting.audio.TranscriptionResultHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class JpaTranscriptionResultHandler implements TranscriptionResultHandler {

    private final MeetingTranscriptRepository meetingTranscriptRepository;

    public JpaTranscriptionResultHandler(MeetingTranscriptRepository meetingTranscriptRepository) {
        this.meetingTranscriptRepository = meetingTranscriptRepository;
    }

    @Override
    @Transactional
    public void handleResult(Meeting meeting, TranscriptionResponse response) {
        MeetingTranscript transcript = new MeetingTranscript(
                meeting,
                response.text(),
                response.language()
        );

        List<SegmentResponse> segmentResponses = response.segments();
        if (segmentResponses != null) {
            for (int i = 0; i < segmentResponses.size(); i++) {
                SegmentResponse seg = segmentResponses.get(i);
                int order = i;
                TranscriptSegment segment = new TranscriptSegment(
                        transcript,
                        order,
                        seg.start(),
                        seg.end(),
                        seg.text()
                );
                transcript.addSegment(segment);
            }
        }

        meetingTranscriptRepository.save(transcript);
    }
}
