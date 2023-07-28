Spring Data for Meilisearch
===========================

Spring Data Implementation for Meilisearch

## Client Configuration

This library support using Java based `@Configuration` or XML Namespace for configuring the Meilisearch Client.

### Annotation based configuration

```java

@Configuration
public class CustomConfiguration extends MeilisearchConfiguration {

  @Override
  public Config clientConfiguration() {
    return ClientConfiguration.builder()
        .connectedToLocalhost()
        .withApiKey("masterKey")
        .withGsonJsonHandler()
        .build();
  }
}
```

### Namespace based configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:meilisearch="http://www.vanslog.io/spring/data/meilisearch"
  xsi:schemaLocation="http://www.vanslog.io/spring/data/meilisearch http://www.vanslog.io/spring/data/meilisearch/spring-meilisearch-1.0.xsd
		http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

  <meilisearch:meilisearch-client id="meilisearchClient"
    api-key="masterKey" json-handler="GSON"/>
</beans>
