package com.aimeetingknowledge.platform.meeting.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "action_items")
public class ActionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "knowledge_id", nullable = false)
    private MeetingKnowledge knowledge;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String task;

    @Column(length = 255)
    private String assignee;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "item_order", nullable = false)
    private Integer itemOrder;

    protected ActionItem() {
    }

    public ActionItem(MeetingKnowledge knowledge, String task, String assignee, LocalDate dueDate, Integer itemOrder) {
        this.knowledge = knowledge;
        this.task = task;
        this.assignee = assignee;
        this.dueDate = dueDate;
        this.itemOrder = itemOrder;
    }

    public Long getId() {
        return id;
    }

    public MeetingKnowledge getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(MeetingKnowledge knowledge) {
        this.knowledge = knowledge;
    }

    public String getTask() {
        return task;
    }

    public String getAssignee() {
        return assignee;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Integer getItemOrder() {
        return itemOrder;
    }
}
