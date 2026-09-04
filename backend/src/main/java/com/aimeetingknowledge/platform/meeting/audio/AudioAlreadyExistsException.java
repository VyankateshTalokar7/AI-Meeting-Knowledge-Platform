package com.aimeetingknowledge.platform.meeting.audio;

public class AudioAlreadyExistsException extends RuntimeException {
    public AudioAlreadyExistsException() { super("This meeting already has an audio recording."); }
}
