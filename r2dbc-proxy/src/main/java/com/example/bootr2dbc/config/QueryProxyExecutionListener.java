package com.example.bootr2dbc.config;

import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.proxy.support.QueryExecutionInfoFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class QueryProxyExecutionListener implements ProxyExecutionListener {

    @Override
    public void afterQuery(QueryExecutionInfo queryExecutionInfo) {
        QueryExecutionInfoFormatter formatter = QueryExecutionInfoFormatter.showAll();
        String str = formatter.format(queryExecutionInfo);
        log.info("Query: {}", str);
        log.info("Execution Time: {} ms", queryExecutionInfo.getExecuteDuration().toMillis());
    }
}
