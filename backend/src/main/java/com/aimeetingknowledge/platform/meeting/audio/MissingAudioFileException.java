package com.aimeetingknowledge.platform.meeting.audio;

public class MissingAudioFileException extends RuntimeException {
    public MissingAudioFileException() { super("An audio file is required."); }
}
