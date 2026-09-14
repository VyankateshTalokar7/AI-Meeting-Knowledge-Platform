package com.aimeetingknowledge.platform.meeting.knowledge;

import com.aimeetingknowledge.platform.meeting.knowledge.dto.MeetingKnowledgeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/meetings")
public class MeetingKnowledgeController {

    private final MeetingKnowledgeService meetingKnowledgeService;

    public MeetingKnowledgeController(MeetingKnowledgeService meetingKnowledgeService) {
        this.meetingKnowledgeService = meetingKnowledgeService;
    }

    @GetMapping("/{meetingId}/knowledge")
    public MeetingKnowledgeResponse getKnowledge(@PathVariable Long meetingId, Principal principal) {
        return meetingKnowledgeService.getKnowledge(meetingId, principal.getName());
    }
}
