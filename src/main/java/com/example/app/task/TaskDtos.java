package com.example.app.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class TaskDtos {
    private TaskDtos() {}

    public record CreateTaskRequest(@NotBlank @Size(max = 160) String title) {}
    public record UpdateTaskRequest(
            @Size(min = 1, max = 160) @Pattern(regexp = ".*\\S.*", message = "must not be blank") String title,
            Boolean completed) {}
    public record TaskResponse(Long id, String title, boolean completed, Instant createdAt, Instant updatedAt) {
        static TaskResponse from(Task task) {
            return new TaskResponse(task.getId(), task.getTitle(), task.isCompleted(),
                    task.getCreatedAt(), task.getUpdatedAt());
        }
    }
}
