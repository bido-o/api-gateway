package com.bido.api_gateway.filter.global;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    @Mock
    private GatewayFilterChain chain;

    private RequestLoggingFilter filter;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.TRACE);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
    }

    private MockServerWebExchange exchangeWithStatus(HttpStatus status) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/x").build());
        if (status != null) {
            exchange.getResponse().setStatusCode(status);
        }
        return exchange;
    }

    private ILoggingEvent lastEvent() {
        List<ILoggingEvent> events = appender.list;
        assertThat(events).as("expected at least one log event").isNotEmpty();
        return events.getLast();
    }

    @Test
    void status2xx_logsAtInfo() {
        MockServerWebExchange exchange = exchangeWithStatus(HttpStatus.OK);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ILoggingEvent event = lastEvent();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).contains("200");
    }

    @Test
    void status4xx_logsAtWarn() {
        MockServerWebExchange exchange = exchangeWithStatus(HttpStatus.BAD_REQUEST);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ILoggingEvent event = lastEvent();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("400");
    }

    @Test
    void status5xx_logsAtError() {
        MockServerWebExchange exchange = exchangeWithStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ILoggingEvent event = lastEvent();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage()).contains("500");
    }

    @Test
    void cancelSignal_logsWarnWithCancelledMarker() {
        MockServerWebExchange exchange = exchangeWithStatus(HttpStatus.OK);
        when(chain.filter(exchange)).thenReturn(Mono.never());

        StepVerifier.create(filter.filter(exchange, chain))
                .thenCancel()
                .verify();

        ILoggingEvent event = lastEvent();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("CANCELLED");
    }

    @Test
    void noStatusCode_logsWarnWithNoStatusMarker() {
        MockServerWebExchange exchange = exchangeWithStatus(null);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        ILoggingEvent event = lastEvent();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("NO_STATUS");
    }
}