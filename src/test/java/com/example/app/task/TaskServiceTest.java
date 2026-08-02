package com.example.app.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.app.task.TaskDtos.CreateTaskRequest;
import com.example.app.user.AppUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock TaskRepository repository;
    @InjectMocks TaskService service;

    @Test
    void createsTrimmedTaskForCurrentUser() {
        AppUser user = new AppUser("Demo", "demo@example.com", "hash");
        when(repository.save(org.mockito.ArgumentMatchers.any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(user, new CreateTaskRequest("  Task pertama  "));

        assertThat(result.title()).isEqualTo("Task pertama");
        verify(repository).save(org.mockito.ArgumentMatchers.any(Task.class));
    }

    @Test
    void listsOnlyTasksOwnedByUser() {
        AppUser user = org.mockito.Mockito.mock(AppUser.class);
        when(user.getId()).thenReturn(7L);
        when(repository.findAllByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());

        assertThat(service.list(user)).isEmpty();
    }
}
