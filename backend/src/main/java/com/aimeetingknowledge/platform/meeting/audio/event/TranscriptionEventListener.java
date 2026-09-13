package com.aimeetingknowledge.platform.meeting.audio.event;

import com.aimeetingknowledge.platform.meeting.audio.MeetingTranscriptionProcessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TranscriptionEventListener {

    private final MeetingTranscriptionProcessor transcriptionProcessor;

    public TranscriptionEventListener(MeetingTranscriptionProcessor transcriptionProcessor) {
        this.transcriptionProcessor = transcriptionProcessor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTranscriptionRequested(TranscriptionRequestedEvent event) {
        transcriptionProcessor.processTranscription(event.meetingId(), event.storagePath());
    }
}
