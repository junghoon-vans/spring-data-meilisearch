package io.vanslog.spring.data.meilisearch.junit.jupiter;

import io.vanslog.testcontainers.meilisearch.MeilisearchContainer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.utility.DockerImageName;

/**
 * This class manages the connection to Meilisearch Instance.
 *
 * @author Junghoon Ban
 */
public class MeilisearchConnection
        implements ExtensionContext.Store.CloseableResource {

    private static final Log LOGGER =
            LogFactory.getLog(MeilisearchConnection.class);
    private static final String TESTCONTAINERS_IMAGE_NAME =
            "getmeili/meilisearch";
    private static final String TESTCONTAINERS_IMAGE_VERSION = "v1.2.0";
    private static final int MEILISEARCH_DEFAULT_PORT = 7700;
    private static final String MEILISEARCH_DEFAULT_MASTER_KEY = "masterKey";
    private static final ThreadLocal<MeilisearchConnectionInfo>
            meilisearchConnectionInfoThreadLocal = new ThreadLocal<>();
    private final MeilisearchConnectionInfo meilisearchConnectionInfo;

    public MeilisearchConnection() {
        meilisearchConnectionInfo = createConnectionInfo();
        if (meilisearchConnectionInfo != null) {
            meilisearchConnectionInfoThreadLocal.set(meilisearchConnectionInfo);
        }
    }

    public static MeilisearchConnectionInfo meilisearchConnectionInfo() {
        return meilisearchConnectionInfoThreadLocal.get();
    }

    public MeilisearchConnectionInfo getMeilisearchConnectionInfo() {
        return meilisearchConnectionInfo;
    }

    public MeilisearchConnectionInfo createConnectionInfo() {
        DockerImageName dockerImageName =
                DockerImageName.parse(TESTCONTAINERS_IMAGE_NAME)
                        .withTag(TESTCONTAINERS_IMAGE_VERSION);
        MeilisearchContainer meilisearchContainer =
                new MeilisearchContainer(dockerImageName)
                        .withMasterKey(MEILISEARCH_DEFAULT_MASTER_KEY);
        meilisearchContainer.start();

        return MeilisearchConnectionInfo.builder()
                .host(meilisearchContainer.getHost())
                .port(meilisearchContainer.getMappedPort(
                        MEILISEARCH_DEFAULT_PORT))
                .masterKey(MEILISEARCH_DEFAULT_MASTER_KEY)
                .meilisearchContainer(meilisearchContainer)
                .build();
    }

    @Override
    public void close() {
        if (meilisearchConnectionInfo != null &&
                meilisearchConnectionInfo.getMeilisearchContainer() != null) {
            LOGGER.debug("stopping MeilisearchConnection");
            meilisearchConnectionInfo.getMeilisearchContainer().stop();
        }
        LOGGER.debug("closed MeilisearchConnection");
    }
}
