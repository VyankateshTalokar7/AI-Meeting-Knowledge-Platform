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

@Entity
@Table(name = "meeting_decisions")
public class MeetingDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "knowledge_id", nullable = false)
    private MeetingKnowledge knowledge;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String decision;

    @Column(name = "decision_order", nullable = false)
    private Integer decisionOrder;

    protected MeetingDecision() {
    }

    public MeetingDecision(MeetingKnowledge knowledge, String decision, Integer decisionOrder) {
        this.knowledge = knowledge;
        this.decision = decision;
        this.decisionOrder = decisionOrder;
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

    public String getDecision() {
        return decision;
    }

    public Integer getDecisionOrder() {
        return decisionOrder;
    }
}
