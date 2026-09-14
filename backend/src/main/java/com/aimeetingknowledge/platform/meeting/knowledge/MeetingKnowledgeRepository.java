package com.aimeetingknowledge.platform.meeting.knowledge;

import com.aimeetingknowledge.platform.meeting.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingKnowledgeRepository extends JpaRepository<MeetingKnowledge, Long> {
    Optional<MeetingKnowledge> findByMeeting(Meeting meeting);
    boolean existsByMeeting(Meeting meeting);
}
