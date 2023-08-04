package io.vanslog.spring.data.meilisearch.client;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.json.JsonHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * FactoryBean class that creates a Meilisearch {@link Client}.
 * The Meilisearch client is created by setting the host URL,
 * API key, JSON handler, and client agents.
 *
 * @author Junghoon Ban
 * @see Client
 */
public final class MeilisearchClientFactoryBean
        implements FactoryBean<Client>, InitializingBean, DisposableBean {

    private static final Log LOGGER =
            LogFactory.getLog(MeilisearchClientFactoryBean.class);

    @Nullable private String hostUrl;
    @Nullable private String apiKey;
    @Nullable private JsonHandler jsonHandler;
    private String[] clientAgents;
    @Nullable private Client client;

    private MeilisearchClientFactoryBean() {
        this.clientAgents = new String[0];
    }

    @Override
    public Client getObject() {
        return client;
    }

    @Override
    public Class<? extends Client> getObjectType() {
        return Client.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Config config = new Config(hostUrl, apiKey, jsonHandler, clientAgents);
        client = new Client(config);
    }

    /**
     * Set the host URL.
     * @param hostUrl the host URL
     */
    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }

    /**
     * Set the API key.
     * @param apiKey the API key
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Set the JSON handler.
     * @param jsonHandler the JSON handler
     */
    public void setJsonHandler(JsonHandler jsonHandler) {
        this.jsonHandler = jsonHandler;
    }

    /**
     * Set the client agents.
     * @param clientAgents the client agents
     */
    public void setClientAgents(String[] clientAgents) {
        this.clientAgents = clientAgents;
    }

    @Override
    public void destroy() {
        if (client != null) {
            LOGGER.info("Closing Meilisearch client");
            client = null;
        }
    }
}
