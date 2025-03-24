package com.aknopov.wssimulator.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import com.aknopov.wssimulator.SocketFactory;
import com.aknopov.wssimulator.Utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.answersWithDelay;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TcpProxyTest {
    private static final Class<byte[]> BYTE_ARRAY_TYPE = byte[].class;
    private static final Duration TEST_DURATION = Duration.ofMillis(300);
    private static final Duration SHORT_PAUSE = TEST_DURATION.dividedBy(10);
    private static final Duration ACCEPT_PAUSE = TEST_DURATION.dividedBy(4);
    private static final InetSocketAddress DOWN_ADDRESS = new InetSocketAddress("localhost", 1234);
    private static final InetSocketAddress UP_ADDRESS = new InetSocketAddress("localhost", 4321);

    private static final ProxyConfig PROXY_CONFIG = new ProxyConfig(1234, 4321, 60_000, 1024, TEST_DURATION);

    private final SocketFactory mockFactory = mock(SocketFactory.class);
    private final ServerSocket mockServerSocket = mock(ServerSocket.class);
    private final Socket mockSocketUp = mock(Socket.class);
    private final Socket mockSocketDown = mock(Socket.class);
    private final InputStream mockInStreamUp = mock(InputStream.class);
    private final OutputStream mockOutStreamUp = mock(OutputStream.class);
    private final InputStream mockInStreamDown = mock(InputStream.class);
    private final OutputStream mockOutStreamDown = mock(OutputStream.class);

    @BeforeEach
    void setUp() throws IOException {
        when(mockFactory.createServerSocket(anyInt())).thenReturn(mockServerSocket);
        when(mockFactory.creatUpstreamSocket(anyInt())).thenReturn(mockSocketUp);
        when(mockServerSocket.accept()).thenAnswer(answersWithDelay(ACCEPT_PAUSE.toMillis(), i -> mockSocketDown));

        when(mockSocketUp.getInputStream()).thenReturn(mockInStreamUp);
        when(mockSocketUp.getOutputStream()).thenReturn(mockOutStreamUp);
        when(mockSocketDown.getInputStream()).thenReturn(mockInStreamDown);
        when(mockSocketDown.getOutputStream()).thenReturn(mockOutStreamDown);
    }

    @Test
    void testPublicFactoryMethods() {
        try (MockedConstruction<SocketFactory> clientMockClass = mockConstruction(SocketFactory.class)) {
            TcpProxy ignored1 = TcpProxy.createNonToxicProxy(PROXY_CONFIG);
            TcpProxy ignored2 = TcpProxy.createJitterProxy(PROXY_CONFIG, ACCEPT_PAUSE, SHORT_PAUSE, Duration.ZERO);
            TcpProxy ignored3 = TcpProxy.createInterruptingProxy(PROXY_CONFIG, ACCEPT_PAUSE);
            TcpProxy ignored4 = TcpProxy.createSlicerProxy(PROXY_CONFIG, 64, ACCEPT_PAUSE);

            assertThat(clientMockClass.constructed(), hasSize(4));
        }
    }

    @Test
    void testLifeSpan() {
        TcpProxy proxy = TcpProxy.createNonToxicProxy(PROXY_CONFIG, new SocketFactory());

        Instant startTime = Instant.now();
        proxy.start();
        proxy.awaitTermination(TEST_DURATION);
        Instant endTime = Instant.now();

        assertTrue(endTime.isAfter(startTime.plus(TEST_DURATION)));
        assertTrue(endTime.isBefore(startTime.plus(TEST_DURATION.multipliedBy(2))));
    }

    @Test
    void testIncomingConnections() throws Exception {
        when(mockInStreamUp.read(any(BYTE_ARRAY_TYPE))).thenReturn(0);
        when(mockInStreamDown.read(any(BYTE_ARRAY_TYPE))).thenReturn(0);

        TcpProxy proxy = TcpProxy.createNonToxicProxy(PROXY_CONFIG, mockFactory);

        Instant startTime = Instant.now();
        proxy.start();
        proxy.awaitTermination(TEST_DURATION);
        Instant endTime = Instant.now();

        assertTrue(endTime.isAfter(startTime.plus(TEST_DURATION)));
        assertTrue(endTime.isBefore(startTime.plus(TEST_DURATION.multipliedBy(2))));

        verify(mockFactory).createServerSocket(DOWN_ADDRESS.getPort());
        verify(mockServerSocket, atLeast(1)).accept();
        verify(mockFactory, atLeast(1)).creatUpstreamSocket(UP_ADDRESS.getPort());

        verify(mockSocketDown, atLeast(1)).getInputStream();
        verify(mockSocketDown, atLeast(1)).getOutputStream();
        verify(mockSocketUp, atLeast(1)).getInputStream();
        verify(mockSocketUp, atLeast(1)).getOutputStream();

        verify(mockInStreamUp, atLeast(1)).read(any(BYTE_ARRAY_TYPE));
        verify(mockInStreamDown, atLeast(1)).read(any(BYTE_ARRAY_TYPE));
        verify(mockOutStreamUp, times(0)).write(any(BYTE_ARRAY_TYPE), anyInt(), anyInt());
        verify(mockOutStreamDown, times(0)).write(any(BYTE_ARRAY_TYPE), anyInt(), anyInt());
    }

    @Test
    void testDataPumping() throws Exception {
        when(mockInStreamUp.read(any(BYTE_ARRAY_TYPE))).thenReturn(2, 2, 0);
        when(mockInStreamDown.read(any(BYTE_ARRAY_TYPE))).thenReturn(2, 2, 0);
        // Just one incoming connection
        when(mockServerSocket.accept())
                .thenAnswer(answersWithDelay(ACCEPT_PAUSE.toMillis(), i -> mockSocketDown))
                .thenAnswer(answersWithDelay(TEST_DURATION.toMillis(), i -> null));

        TcpProxy proxy = TcpProxy.createNonToxicProxy(PROXY_CONFIG, mockFactory);
        proxy.start();
        proxy.awaitTermination(TEST_DURATION);

        verify(mockInStreamDown, times(3)).read(any(BYTE_ARRAY_TYPE));
        verify(mockOutStreamDown, times(2)).write(any(BYTE_ARRAY_TYPE), eq(0), eq(2));
        verify(mockInStreamUp, times(3)).read(any(BYTE_ARRAY_TYPE));
        verify(mockOutStreamUp, times(2)).write(any(BYTE_ARRAY_TYPE), eq(0), eq(2));
    }

    @Test
    void testCommunicationFailures() throws Exception {
        when(mockInStreamUp.read(any(BYTE_ARRAY_TYPE))).thenReturn(2);
        when(mockInStreamDown.read(any(BYTE_ARRAY_TYPE))).thenReturn(2);
        doThrow(IOException.class).when(mockOutStreamDown).write(any(BYTE_ARRAY_TYPE), anyInt(), anyInt());
        doThrow(IOException.class).when(mockOutStreamUp).write(any(BYTE_ARRAY_TYPE), anyInt(), anyInt());
        TcpProxy proxy = TcpProxy.createNonToxicProxy(PROXY_CONFIG, mockFactory);
        assertDoesNotThrow(proxy::start);
        proxy.awaitTermination(TEST_DURATION);

        when(mockInStreamDown.read(any(BYTE_ARRAY_TYPE))).thenThrow(IOException.class);
        when(mockInStreamUp.read(any(BYTE_ARRAY_TYPE))).thenThrow(IOException.class);
        proxy = TcpProxy.createNonToxicProxy(PROXY_CONFIG, mockFactory);
        assertDoesNotThrow(proxy::start);
        proxy.awaitTermination(TEST_DURATION);

        when(mockSocketUp.getOutputStream()).thenThrow(IOException.class);
        when(mockSocketDown.getInputStream()).thenThrow(IOException.class);
        proxy = TcpProxy.createNonToxicProxy(PROXY_CONFIG, mockFactory);
        assertDoesNotThrow(proxy::start);
        proxy.awaitTermination(TEST_DURATION);
    }

    @Test
    void testDoubleStop() {
        try (MockedStatic<NamedThreadPool> mockeThreadPool = mockStatic(NamedThreadPool.class)) {
            ExecutorService mockExecutor = mock(ExecutorService.class);
            mockeThreadPool.when(() -> NamedThreadPool.createFixedPool(anyInt(), anyString())).thenReturn(mockExecutor);

            TcpProxy proxy = TcpProxy.createNonToxicProxy(PROXY_CONFIG, mockFactory);
            proxy.start();
            Utils.sleepUnchecked(SHORT_PAUSE);
            proxy.stop();

            verify(mockExecutor, times(2)).submit(any(Runnable.class));
            verify(mockExecutor).shutdownNow();

            // Repeated stop doesn't have effect
            proxy.stop();
            verifyNoMoreInteractions(mockExecutor);
        }
    }

    @Test
    void testNoInterruptionsOnStop() {
        TcpProxy proxy = TcpProxy.createNonToxicProxy(PROXY_CONFIG, mockFactory);
        proxy.start();
        Utils.sleepUnchecked(SHORT_PAUSE);
        proxy.stop();

    }

    @Test
    void testAwaitTerminationOnTimeout() {
        TcpProxy proxy = TcpProxy.createNonToxicProxy(PROXY_CONFIG, mockFactory);
        proxy.start();

        assertFalse(proxy.awaitTermination(TEST_DURATION.dividedBy(5)));
        assertTrue(proxy.awaitTermination(TEST_DURATION));
    }

    @Test
    void testAwaitTerminationOnInterrupt() {
        TcpProxy proxy = TcpProxy.createNonToxicProxy(PROXY_CONFIG, mockFactory);
        proxy.start();

        Thread testThread = Thread.currentThread();
        ScheduledThreadPoolExecutor shutdownExecutor = new ScheduledThreadPoolExecutor(1);
        shutdownExecutor.schedule(testThread::interrupt, SHORT_PAUSE.toMillis(), TimeUnit.MILLISECONDS);

        assertFalse(proxy.awaitTermination(SHORT_PAUSE.multipliedBy(2)));
    }

    @Test
    void testIOErrors() throws IOException {
        TcpProxy proxy = TcpProxy.createNonToxicProxy(PROXY_CONFIG, mockFactory);

        doThrow(SocketException.class).when(mockFactory).creatUpstreamSocket(anyInt());
        assertDoesNotThrow(() -> proxy.proxyCommunications(mockSocketDown));

        doThrow(IOException.class).when(mockFactory).creatUpstreamSocket(anyInt());
        assertDoesNotThrow(() -> proxy.proxyCommunications(mockSocketDown));

        doThrow(IOException.class).when(mockFactory).createServerSocket(anyInt());
        assertDoesNotThrow(proxy::waitForIncomingConnections);
    }

    @Test
    void testInterruptConnection() throws IOException {
        TcpProxy proxy = TcpProxy.createNonToxicProxy(PROXY_CONFIG, mockFactory);

        // Before establlishing connection
        proxy.interrupt();
        verifyNoInteractions(mockSocketUp);
        verifyNoInteractions(mockSocketDown);

        proxy.start();
        Utils.sleepUnchecked(ACCEPT_PAUSE.plus(SHORT_PAUSE));
        proxy.interrupt();

        verify(mockSocketUp).close();
        verify(mockSocketDown).close();

        proxy.stop();
    }
}