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

    /**
     * Constructor for TaskStatusException.
     *
     * @param taskStatus the task status
     * @param msg        the detail message
     */
    public TaskStatusException(TaskStatus taskStatus, String msg) {
        super(msg);
        this.taskStatus = taskStatus;
    }

    /**
     * Constructor for TaskStatusException.
     *
     * @param taskStatus the task status
     * @param msg        the detail message
     * @param cause      the root cause from the data access API in use
     */
    public TaskStatusException(TaskStatus taskStatus, String msg,
                               Throwable cause) {
        super(msg, cause);
        this.taskStatus = taskStatus;
    }

    /**
     * Return the task status.
     * @return the task status
     */
    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    @Override
    public String toString() {
        return "TaskStatusException{" + "taskStatus=" + taskStatus + "} "
                + super.toString();
    }
}
