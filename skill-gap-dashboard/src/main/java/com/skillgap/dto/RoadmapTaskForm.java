package com.skillgap.dto;

import com.skillgap.entity.enums.Priority;
import com.skillgap.entity.enums.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Add/edit a learning roadmap task. */
public class RoadmapTaskForm {

    @NotBlank(message = "Title is required")
    @Size(max = 300)
    private String title;

    @Size(max = 500)
    private String url;

    @Min(value = 1, message = "Difficulty must be 1-5")
    @Max(value = 5, message = "Difficulty must be 1-5")
    private Integer difficulty = 3;

    @Min(value = 0)
    @Max(value = 1000)
    private Double estimatedHours;

    private Priority priority = Priority.MEDIUM;

    private LocalDate plannedDate;

    private TaskStatus status = TaskStatus.NOT_STARTED;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
    }

    public Double getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Double estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(LocalDate plannedDate) {
        this.plannedDate = plannedDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }
}
