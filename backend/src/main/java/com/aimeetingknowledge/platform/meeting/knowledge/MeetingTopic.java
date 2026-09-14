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
@Table(name = "meeting_topics")
public class MeetingTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "knowledge_id", nullable = false)
    private MeetingKnowledge knowledge;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String topic;

    @Column(name = "topic_order", nullable = false)
    private Integer topicOrder;

    protected MeetingTopic() {
    }

    public MeetingTopic(MeetingKnowledge knowledge, String topic, Integer topicOrder) {
        this.knowledge = knowledge;
        this.topic = topic;
        this.topicOrder = topicOrder;
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

    public String getTopic() {
        return topic;
    }

    public Integer getTopicOrder() {
        return topicOrder;
    }
}
