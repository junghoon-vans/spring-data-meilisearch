package io.vanslog.spring.data.meilisearch.client;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.json.GsonJsonHandler;
import com.meilisearch.sdk.json.JsonHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * FactoryBean class that creates a MeiliSearch {@link Client}.
 * The MeiliSearch client is created by setting the host URL, API key, JSON handler, and client agents.
 *
 * @since 1.0.0
 * @see Client
 * @author Junghoon Ban
 */
public class MeilisearchClientFactoryBean implements FactoryBean<Client>, InitializingBean, DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(MeilisearchClientFactoryBean.class);

	private String hostUrl;
	private String apiKey;
	private JsonHandler jsonHandler;
	private String[] clientAgents;
	private Client client;

	private MeilisearchClientFactoryBean() {
		this.hostUrl = "http://localhost:7700";
		this.apiKey = "";
		this.jsonHandler = new GsonJsonHandler();
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
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Config config = new Config(hostUrl, apiKey, jsonHandler, clientAgents);
		client = new Client(config);
	}

	public void setHostUrl(String hostUrl) {
		this.hostUrl = hostUrl;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public void setJsonHandler(JsonHandler jsonHandler) {
		this.jsonHandler = jsonHandler;
	}

	public void setClientAgents(String[] clientAgents) {
		this.clientAgents = clientAgents;
	}

	@Override
	public void destroy() {
		if (client != null) {
			client = null;
			logger.info("Closed MeiliSearch client");
		}
	}
}
