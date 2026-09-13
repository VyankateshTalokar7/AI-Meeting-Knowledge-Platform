package com.aimeetingknowledge.platform.meeting.transcript;

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
@Table(name = "transcript_segments")
public class TranscriptSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transcript_id", nullable = false)
    private MeetingTranscript transcript;

    @Column(name = "segment_order", nullable = false)
    private Integer segmentOrder;

    @Column(name = "start_time", nullable = false)
    private Double startTime;

    @Column(name = "end_time", nullable = false)
    private Double endTime;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    protected TranscriptSegment() {
    }

    public TranscriptSegment(MeetingTranscript transcript, Integer segmentOrder, Double startTime, Double endTime, String text) {
        this.transcript = transcript;
        this.segmentOrder = segmentOrder;
        this.startTime = startTime;
        this.endTime = endTime;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public MeetingTranscript getTranscript() {
        return transcript;
    }

    public void setTranscript(MeetingTranscript transcript) {
        this.transcript = transcript;
    }

    public Integer getSegmentOrder() {
        return segmentOrder;
    }

    public Double getStartTime() {
        return startTime;
    }

    public Double getEndTime() {
        return endTime;
    }

    public String getText() {
        return text;
    }
}
