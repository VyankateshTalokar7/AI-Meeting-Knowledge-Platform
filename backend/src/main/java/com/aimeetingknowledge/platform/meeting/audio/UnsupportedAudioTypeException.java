package com.aimeetingknowledge.platform.meeting.audio;

public class UnsupportedAudioTypeException extends RuntimeException {
    public UnsupportedAudioTypeException() { super("The uploaded file type is not supported."); }
}
