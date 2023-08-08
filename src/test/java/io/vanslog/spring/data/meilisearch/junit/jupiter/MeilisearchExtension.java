package io.vanslog.spring.data.meilisearch.junit.jupiter;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Extension for JUnit Jupiter to provide a Meilisearch connection.
 *
 * @author Junghoon Ban
 */
public class MeilisearchExtension implements BeforeAllCallback {

    private final Lock initLock = new ReentrantLock();

    @Override
    public void beforeAll(ExtensionContext context) {
        initLock.lock();
        try {
            new MeilisearchConnection();
        } finally {
            initLock.unlock();
        }
    }
}
