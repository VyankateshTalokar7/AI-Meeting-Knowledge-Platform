package com.aimeetingknowledge.platform.meeting.knowledge.dto;

import com.aimeetingknowledge.platform.meeting.knowledge.ActionItem;

import java.time.LocalDate;

public record ActionItemResponse(
        Long id,
        String task,
        String assignee,
        LocalDate dueDate,
        Integer itemOrder
) {
    public static ActionItemResponse from(ActionItem item) {
        return new ActionItemResponse(
                item.getId(),
                item.getTask(),
                item.getAssignee(),
                item.getDueDate(),
                item.getItemOrder()
        );
    }
}
