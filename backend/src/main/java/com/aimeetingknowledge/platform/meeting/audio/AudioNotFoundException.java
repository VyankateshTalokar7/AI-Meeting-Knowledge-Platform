package com.aimeetingknowledge.platform.meeting.audio;

public class AudioNotFoundException extends RuntimeException {
    public AudioNotFoundException() { super("Audio recording was not found."); }
}
