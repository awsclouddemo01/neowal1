package org.apache.camel.metrics.timer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import org.apache.camel.Exchange;
import org.apache.camel.metrics.timer.TimerEndpoint.TimerAction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

@RunWith(MockitoJUnitRunner.class)
public class TimerProducerTest {

    private static final String METRICS_NAME = "metrics.name";
    private static final String PROPERTY_NAME = TimerEndpoint.ENDPOINT_URI + ":" + METRICS_NAME;

    @Mock
    private TimerEndpoint endpoint;

    @Mock
    private Exchange exchange;

    @Mock
    private MetricRegistry registry;

    @Mock
    private Timer timer;

    @Mock
    private Timer.Context context;

    private TimerProducer producer;

    @Mock
    private InOrder inOrder;

    @Before
    public void setUp() throws Exception {
        producer = new TimerProducer(endpoint);
        inOrder = Mockito.inOrder(endpoint, exchange, registry, timer, context);
        when(endpoint.getRegistry()).thenReturn(registry);
        when(endpoint.getMetricsName(exchange)).thenReturn(METRICS_NAME);
        when(registry.timer(METRICS_NAME)).thenReturn(timer);
        when(timer.time()).thenReturn(context);
    }

    @Test
    public void testTimerProducer() throws Exception {
        assertThat(producer, is(notNullValue()));
        assertThat(producer.getEndpoint().equals(endpoint), is(true));
    }

    @Test
    public void testProcessStart() throws Exception {
        when(endpoint.getAction()).thenReturn(TimerAction.start);
        when(exchange.getProperty(PROPERTY_NAME, Timer.Context.class)).thenReturn(null);
        producer.process(exchange);
        inOrder.verify(endpoint, times(1)).getRegistry();
        inOrder.verify(endpoint, times(1)).getMetricsName(exchange);
        inOrder.verify(endpoint, times(1)).getAction();
        inOrder.verify(exchange, times(1)).getProperty(PROPERTY_NAME, Timer.Context.class);
        inOrder.verify(registry, times(1)).timer(METRICS_NAME);
        inOrder.verify(timer, times(1)).time();
        inOrder.verify(exchange, times(1)).setProperty(PROPERTY_NAME, context);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testProcessStop() throws Exception {
        when(endpoint.getAction()).thenReturn(TimerAction.stop);
        when(exchange.getProperty(PROPERTY_NAME, Timer.Context.class)).thenReturn(context);
        producer.process(exchange);
        inOrder.verify(endpoint, times(1)).getRegistry();
        inOrder.verify(endpoint, times(1)).getMetricsName(exchange);
        inOrder.verify(endpoint, times(1)).getAction();
        inOrder.verify(exchange, times(1)).getProperty(PROPERTY_NAME, Timer.Context.class);
        inOrder.verify(context, times(1)).stop();
        inOrder.verify(exchange, times(1)).removeProperty(PROPERTY_NAME);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testProcessNoAction() throws Exception {
        when(endpoint.getAction()).thenReturn(null);
        producer.process(exchange);
        inOrder.verify(endpoint, times(1)).getRegistry();
        inOrder.verify(endpoint, times(1)).getMetricsName(exchange);
        inOrder.verify(endpoint, times(1)).getAction();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testHandleStart() throws Exception {
        when(exchange.getProperty(PROPERTY_NAME, Timer.Context.class)).thenReturn(null);
        producer.handleStart(exchange, registry, METRICS_NAME);
        inOrder.verify(exchange, times(1)).getProperty(PROPERTY_NAME, Timer.Context.class);
        inOrder.verify(registry, times(1)).timer(METRICS_NAME);
        inOrder.verify(timer, times(1)).time();
        inOrder.verify(exchange, times(1)).setProperty(PROPERTY_NAME, context);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testHandleStartAlreadyRunning() throws Exception {
        when(exchange.getProperty(PROPERTY_NAME, Timer.Context.class)).thenReturn(context);
        producer.handleStart(exchange, registry, METRICS_NAME);
        inOrder.verify(exchange, times(1)).getProperty(PROPERTY_NAME, Timer.Context.class);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testHandleStop() throws Exception {
        when(exchange.getProperty(PROPERTY_NAME, Timer.Context.class)).thenReturn(context);
        producer.handleStop(exchange, registry, METRICS_NAME);
        inOrder.verify(exchange, times(1)).getProperty(PROPERTY_NAME, Timer.Context.class);
        inOrder.verify(context, times(1)).stop();
        inOrder.verify(exchange, times(1)).removeProperty(PROPERTY_NAME);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testHandleStopContextNotFound() throws Exception {
        when(exchange.getProperty(PROPERTY_NAME, Timer.Context.class)).thenReturn(null);
        producer.handleStop(exchange, registry, METRICS_NAME);
        inOrder.verify(exchange, times(1)).getProperty(PROPERTY_NAME, Timer.Context.class);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testGetPropertyName() throws Exception {
        assertThat(producer.getPropertyName(METRICS_NAME), is(TimerEndpoint.ENDPOINT_URI + ":" + METRICS_NAME));
    }

    @Test
    public void testGetTimerContextFromExchange() throws Exception {
        when(exchange.getProperty(PROPERTY_NAME, Timer.Context.class)).thenReturn(context);
        assertThat(producer.getTimerContextFromExchange(exchange, PROPERTY_NAME), is(context));
        inOrder.verify(exchange, times(1)).getProperty(PROPERTY_NAME, Timer.Context.class);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testGetTimerContextFromExchangeNotFound() throws Exception {
        when(exchange.getProperty(PROPERTY_NAME, Timer.Context.class)).thenReturn(null);
        assertThat(producer.getTimerContextFromExchange(exchange, PROPERTY_NAME), is(nullValue()));
        inOrder.verify(exchange, times(1)).getProperty(PROPERTY_NAME, Timer.Context.class);
        inOrder.verifyNoMoreInteractions();
    }
}
