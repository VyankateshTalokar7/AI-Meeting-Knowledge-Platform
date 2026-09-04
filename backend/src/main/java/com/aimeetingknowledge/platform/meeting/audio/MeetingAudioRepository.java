package com.aimeetingknowledge.platform.meeting.audio;

import com.aimeetingknowledge.platform.meeting.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingAudioRepository extends JpaRepository<MeetingAudio, Long> {

    Optional<MeetingAudio> findByMeeting(Meeting meeting);
}
