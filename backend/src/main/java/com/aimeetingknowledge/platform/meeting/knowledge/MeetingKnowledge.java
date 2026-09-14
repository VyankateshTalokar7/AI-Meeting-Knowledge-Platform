package com.aimeetingknowledge.platform.meeting.knowledge;

import com.aimeetingknowledge.platform.meeting.Meeting;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meeting_knowledge")
public class MeetingKnowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false, unique = true)
    private Meeting meeting;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "knowledge", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("topicOrder ASC")
    private List<MeetingTopic> topics = new ArrayList<>();

    @OneToMany(mappedBy = "knowledge", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("decisionOrder ASC")
    private List<MeetingDecision> decisions = new ArrayList<>();

    @OneToMany(mappedBy = "knowledge", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemOrder ASC")
    private List<ActionItem> actionItems = new ArrayList<>();

    protected MeetingKnowledge() {
    }

    public MeetingKnowledge(Meeting meeting, String summary) {
        this.meeting = meeting;
        this.summary = summary;
    }

    @PrePersist
    void onPrePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onPreUpdate() {
        updatedAt = Instant.now();
    }

    public void addTopic(MeetingTopic topic) {
        topics.add(topic);
        topic.setKnowledge(this);
    }

    public void addDecision(MeetingDecision decision) {
        decisions.add(decision);
        decision.setKnowledge(this);
    }

    public void addActionItem(ActionItem item) {
        actionItems.add(item);
        item.setKnowledge(this);
    }

    public Long getId() {
        return id;
    }

    public Meeting getMeeting() {
        return meeting;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<MeetingTopic> getTopics() {
        return topics;
    }

    public List<MeetingDecision> getDecisions() {
        return decisions;
    }

    public List<ActionItem> getActionItems() {
        return actionItems;
    }
}
