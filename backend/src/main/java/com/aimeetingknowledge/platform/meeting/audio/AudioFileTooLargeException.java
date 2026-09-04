package com.aimeetingknowledge.platform.meeting.audio;

public class AudioFileTooLargeException extends RuntimeException {
    public AudioFileTooLargeException() { super("The uploaded audio file exceeds the allowed size."); }
}
