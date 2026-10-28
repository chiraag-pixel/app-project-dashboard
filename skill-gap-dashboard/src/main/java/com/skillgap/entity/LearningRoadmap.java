package com.skillgap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/** A user's learning roadmap: a titled list of planned tasks. */
@Entity
@Table(name = "learning_roadmaps", indexes = @Index(name = "idx_roadmaps_user", columnList = "user_id"))
public class LearningRoadmap extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    /** Target label the roadmap was generated for, e.g. "Java Developer". */
    @Column(length = 200)
    private String targetLabel;

    @OneToMany(mappedBy = "roadmap", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RoadmapTask> tasks = new ArrayList<>();

    protected LearningRoadmap() {
    }

    public LearningRoadmap(User user, String title) {
        this.user = user;
        this.title = title;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTargetLabel() {
        return targetLabel;
    }

    public void setTargetLabel(String targetLabel) {
        this.targetLabel = targetLabel;
    }

    public List<RoadmapTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<RoadmapTask> tasks) {
        this.tasks = tasks;
    }

    public void addTask(RoadmapTask task) {
        task.setRoadmap(this);
        tasks.add(task);
    }
}
