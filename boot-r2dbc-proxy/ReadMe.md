
Proxy can be enabled directly by setting in URL as 

```
# with pooling
spring.r2dbc.url=r2dbc:proxy:pool:postgresql://localhost:5432/myDB?proxyListener=com.example.bootr2dbc.config.QueryProxyExecutionListener&maxIdleTime=PT60S
```

Issue with configuring using URL is that it is not available while Junits as we are using TestContainers, to make sure that it is working for both main and tests we should configure programatically so we are taking advantage of `ConnectionFactoryOptionsBuilderCustomizer` to set as below

```java
@Bean
ConnectionFactoryOptionsBuilderCustomizer postgresCustomizer(
        QueryProxyExecutionListener queryProxyExecutionListener) {
    return builder -> {
        builder.option(ConnectionFactoryOptions.LOCK_WAIT_TIMEOUT, Duration.ofSeconds(30));
        builder.option(ConnectionFactoryOptions.STATEMENT_TIMEOUT, Duration.ofMinutes(1));
        builder.option(ConnectionFactoryOptions.DRIVER, "proxy");
        builder.option(ConnectionFactoryOptions.PROTOCOL, "postgresql");
        builder.option(
                ProxyConnectionFactoryProvider.PROXY_LISTENERS, queryProxyExecutionListener);
    };
}
```