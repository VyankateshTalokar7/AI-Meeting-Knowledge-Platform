package com.aimeetingknowledge.platform.meeting;

import com.aimeetingknowledge.platform.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findAllByUserOrderByMeetingDateDesc(User user);

    Optional<Meeting> findByIdAndUser(Long id, User user);
}
