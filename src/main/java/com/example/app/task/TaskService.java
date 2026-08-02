package com.example.app.task;

import com.example.app.common.NotFoundException;
import com.example.app.task.TaskDtos.CreateTaskRequest;
import com.example.app.task.TaskDtos.TaskResponse;
import com.example.app.task.TaskDtos.UpdateTaskRequest;
import com.example.app.user.AppUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
    private final TaskRepository tasks;

    public TaskService(TaskRepository tasks) {
        this.tasks = tasks;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(AppUser user) {
        return tasks.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(TaskResponse::from).toList();
    }

    @Transactional
    public TaskResponse create(AppUser user, CreateTaskRequest request) {
        return TaskResponse.from(tasks.save(new Task(request.title().trim(), user)));
    }

    @Transactional
    public TaskResponse update(AppUser user, Long id, UpdateTaskRequest request) {
        Task task = ownedTask(user, id);
        if (request.title() != null) {
            task.setTitle(request.title().trim());
        }
        if (request.completed() != null) {
            task.setCompleted(request.completed());
        }
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(AppUser user, Long id) {
        tasks.delete(ownedTask(user, id));
    }

    private Task ownedTask(AppUser user, Long id) {
        return tasks.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Task tidak ditemukan"));
    }
}

