package com.example.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TraceIdFilter extends AbstractGatewayFilterFactory<TraceIdFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(TraceIdFilter.class);
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    public TraceIdFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);

            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString();
            }

            logger.info("Request to {} with Trace-ID: {}",
                    exchange.getRequest().getPath(), traceId);

            final String finalTraceId = traceId;

            return chain.filter(exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header(TRACE_ID_HEADER, finalTraceId)
                            .build())
                    .build());
        };
    }

    public static class Config {
        // Configuration properties if needed
    }
}

