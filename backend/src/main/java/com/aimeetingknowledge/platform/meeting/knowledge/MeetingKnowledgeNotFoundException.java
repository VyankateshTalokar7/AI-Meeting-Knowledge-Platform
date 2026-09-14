package com.aimeetingknowledge.platform.meeting.knowledge;

public class MeetingKnowledgeNotFoundException extends RuntimeException {
    public MeetingKnowledgeNotFoundException() {
        super("Meeting knowledge was not found for this meeting.");
    }
}
