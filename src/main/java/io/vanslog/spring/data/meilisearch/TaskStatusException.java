package io.vanslog.spring.data.meilisearch;

import com.meilisearch.sdk.model.TaskStatus;
import org.springframework.dao.DataAccessException;

/**
 * Exception indicating that a task status was not successful.
 *
 * @author Junghoon Ban
 */
public class TaskStatusException extends DataAccessException {

    private final TaskStatus taskStatus;

    public TaskStatusException(TaskStatus taskStatus, String msg) {
        super(msg);
        this.taskStatus = taskStatus;
    }

    public TaskStatusException(TaskStatus taskStatus, String msg,
                               Throwable cause) {
        super(msg, cause);
        this.taskStatus = taskStatus;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    @Override
    public String toString() {
        return "TaskStatusException{" + "taskStatus=" + taskStatus + "} " +
                super.toString();
    }
}
