package com.aimeetingknowledge.platform.meeting.transcript;

public class TranscriptNotFoundException extends RuntimeException {
    public TranscriptNotFoundException() {
        super("Transcript was not found for this meeting.");
    }
}
