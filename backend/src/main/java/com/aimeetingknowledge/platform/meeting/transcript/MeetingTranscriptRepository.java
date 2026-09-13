package com.aimeetingknowledge.platform.meeting.transcript;

import com.aimeetingknowledge.platform.meeting.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingTranscriptRepository extends JpaRepository<MeetingTranscript, Long> {
    Optional<MeetingTranscript> findByMeeting(Meeting meeting);
}
