package com.aknopov.wssimulator.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.aknopov.wssimulator.SocketFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.answersWithDelay;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProxyTest {
    private static final Class<byte[]> BYTE_ARRAY_TYPE = byte[].class;
    private static final Duration TEST_DURATION = Duration.ofMillis(100);
    private static final Duration ACCEPT_PAUSE = TEST_DURATION.dividedBy(3);
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
        when(mockFactory.createServerSocket()).thenReturn(mockServerSocket);
        when(mockFactory.creatUpsteamSocket()).thenReturn(mockSocketUp);
        when(mockServerSocket.accept()).thenAnswer(answersWithDelay(ACCEPT_PAUSE.toMillis(), i -> mockSocketDown));

        when(mockSocketUp.getInputStream()).thenReturn(mockInStreamUp);
        when(mockSocketUp.getOutputStream()).thenReturn(mockOutStreamUp);
        when(mockSocketDown.getInputStream()).thenReturn(mockInStreamDown);
        when(mockSocketDown.getOutputStream()).thenReturn(mockOutStreamDown);
    }

    @Test
    void testLifeSpan() {
        Proxy proxy = new Proxy(PROXY_CONFIG, new SocketFactory());

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

        Proxy proxy = new Proxy(PROXY_CONFIG, mockFactory);

        Instant startTime = Instant.now();
        proxy.start();
        proxy.awaitTermination(TEST_DURATION);
        Instant endTime = Instant.now();

        assertTrue(endTime.isAfter(startTime.plus(TEST_DURATION)));
        assertTrue(endTime.isBefore(startTime.plus(TEST_DURATION.multipliedBy(2))));

        ArgumentCaptor<InetSocketAddress> inetAddCaptor = ArgumentCaptor.forClass(InetSocketAddress.class);
        verify(mockFactory).createServerSocket();
        verify(mockServerSocket, atLeast(2)).accept();
        verify(mockServerSocket).bind(inetAddCaptor.capture());
        verify(mockFactory, atLeast(2)).creatUpsteamSocket();
        verify(mockSocketUp, atLeast(2)).connect(inetAddCaptor.capture());

        List<InetSocketAddress> addresses = inetAddCaptor.getAllValues();
        assertTrue(addresses.size() >= 3);
        assertEquals(DOWN_ADDRESS, addresses.get(0));
        assertEquals(UP_ADDRESS, addresses.get(1));
        assertEquals(UP_ADDRESS, addresses.get(2));

        verify(mockSocketDown, atLeast(2)).getInputStream();
        verify(mockSocketDown, atLeast(2)).getOutputStream();
        verify(mockSocketUp, atLeast(2)).getInputStream();
        verify(mockSocketUp, atLeast(2)).getOutputStream();

        verify(mockInStreamUp, atLeast(2)).read(any(BYTE_ARRAY_TYPE));
        verify(mockInStreamDown, atLeast(2)).read(any(BYTE_ARRAY_TYPE));
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

        Proxy proxy = new Proxy(PROXY_CONFIG, mockFactory);
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
        Proxy proxy = new Proxy(PROXY_CONFIG, mockFactory);
        assertDoesNotThrow(proxy::start);
        proxy.awaitTermination(TEST_DURATION);

        doThrow(IOException.class).when(mockInStreamDown).read(any(BYTE_ARRAY_TYPE));
        doThrow(IOException.class).when(mockInStreamUp).read(any(BYTE_ARRAY_TYPE));
        proxy = new Proxy(PROXY_CONFIG, mockFactory);
        assertDoesNotThrow(proxy::start);
        proxy.awaitTermination(TEST_DURATION);

        doThrow(IOException.class).when(mockSocketUp).getOutputStream();
        doThrow(IOException.class).when(mockSocketDown).getInputStream();
        proxy = new Proxy(PROXY_CONFIG, mockFactory);
        assertDoesNotThrow(proxy::start);
        proxy.awaitTermination(TEST_DURATION);
    }
}