package com.aknopov.wssimulator.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.SocketFactory;
import com.aknopov.wssimulator.proxy.toxy.Interruptible;
import com.aknopov.wssimulator.proxy.toxy.Toxic;
import com.aknopov.wssimulator.proxy.toxy.ToxicInterrupter;
import com.aknopov.wssimulator.proxy.toxy.ToxicLatency;
import com.aknopov.wssimulator.proxy.toxy.ToxicNoop;

/**
 * TCP proxy implementation for "localhost"
 */
public class TcpProxy implements Interruptible {
    private static final Logger logger = LoggerFactory.getLogger(TcpProxy.class);

    private final ProxyConfig proxyConfig;
    private final SocketFactory socketFactory;
    private final ExecutorService executor;
    private final Toxic toxic;
    private final CountDownLatch stopped = new CountDownLatch(1);
    @Nullable
    private Socket downstreamSocket;
    @Nullable
    private Socket upstreamSocket;
    private final Consumer<Socket> socketModifier;

    /**
     * Creates not intoxicated proxy with configuration and socket factory
     *
     * @param proxyConfig proxy config
     * @param socketFactory socket factory
     * @return proxy without toxic
     */
    public static TcpProxy createNonToxicProxy(ProxyConfig proxyConfig, SocketFactory socketFactory) {
        return new TcpProxy(proxyConfig, socketFactory, new ToxicNoop());
    }

    /**
     * Creates proxy that delays data exchange with specified parameters
     *
     * @param proxyConfig proxy config
     * @param socketFactory socket factory
     * @param startTime interval since proxy start to delay data transmission
     * @param latency average latency
     * @param jitter latency variance
     * @return proxy with toxic latency
     */
    public static TcpProxy createJitterProxy(ProxyConfig proxyConfig, SocketFactory socketFactory,
            Duration startTime, Duration latency, Duration jitter) {
        return new TcpProxy(proxyConfig, socketFactory, new ToxicLatency(startTime, latency, jitter));
    }

    /**
     * Creates proxy that interrupts connection at some point.
     *
     * @param proxyConfig proxy config
     * @param socketFactory socket factory
     * @param interruptTime interval sine start to interrupt connection
     * @return proxy that interrupts connection
     */
    public static TcpProxy createInterruptingProxy(ProxyConfig proxyConfig, SocketFactory socketFactory, Duration interruptTime) {
        return new TcpProxy(proxyConfig, socketFactory, interruptTime);
    }

    private TcpProxy(ProxyConfig proxyConfig, SocketFactory socketFactory, Toxic toxic) {
        this.proxyConfig = proxyConfig;
        this.socketFactory = socketFactory;
        this.executor = NamedThreadPool.createFixedPool(3, "TcpProxy");
        this.toxic = toxic;
        this.socketModifier = s -> {};
    }

    private TcpProxy(ProxyConfig proxyConfig, SocketFactory socketFactory, Duration interruptTime) {
        this.proxyConfig = proxyConfig;
        this.socketFactory = socketFactory;
        this.executor = NamedThreadPool.createFixedPool(3, "TcpProxy");
        this.toxic = new ToxicInterrupter(interruptTime, this);
        this.socketModifier = TcpProxy::resetOnClose;
    }

    /**
     * Starts proxying and returns immediately. You can wait for completion with  {@link TcpProxy#awaitTermination(Duration)}.
     */
    public void start() {
        logger.debug("Starting proxy");
        Future<?> unused1 = executor.submit(this::sleepToShutdown);
        Future<?> unused2 = executor.submit(this::waitForIncomingConnections);
    }

    /**
     * Stops proxying immediately.
     */
    public void stop() {
        logger.debug("Stopping proxy");
        if (stopped.getCount() > 0) {
            stopped.countDown();
            executor.shutdownNow();
        }
    }

    @Override
    public void interrupt() {
        logger.debug("Interrupting connection");
        closeSocket(downstreamSocket);
        closeSocket(upstreamSocket);
        downstreamSocket = null;
        upstreamSocket = null;
    }

    private static void closeSocket(@Nullable Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            }
            catch (IOException e) {
                //ignore
            }
        }
    }

    /**
     * Awaits termination of proxy either after call of {@link TcpProxy#stop()}
     * or {@link ProxyConfig#shutdownTime()} timeout
     *
     * @param waitTime waiting time
     */
    public boolean awaitTermination(Duration waitTime) {
        try {
            return stopped.await(waitTime.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            // ignore
        }
        return false;
    }

    private void sleepToShutdown() {
        try {
            Thread.sleep(proxyConfig.shutdownTime().toMillis());
            stop();
        }
        catch (InterruptedException e) {
            //ignore
        }
    }

    //VisibleForTesting
    void waitForIncomingConnections() {
        try (ServerSocket serverSocket = socketFactory.createServerSocket(proxyConfig.downPort())) {
            logger.debug("Started proxy server on port {}", proxyConfig.downPort());
            while (stopped.getCount() > 0) {
                logger.debug("Waiting for incoming connection");
                try (Socket downstreamSocket = serverSocket.accept()) {
                    proxyCommunications(downstreamSocket);
                }
            }
        }
        catch (IOException ex) {
            logger.error("Failed to create server socket", ex);
        }
    }

    //VisibleForTesting
    void proxyCommunications(Socket downstreamSocket) {
        logger.debug("Connection accepted");
        this.downstreamSocket = downstreamSocket;
        try (Socket upstreamSocket = socketFactory.creatUpstreamSocket(proxyConfig.upPort())) {
            logger.debug("Created proxy client on port {}", proxyConfig.upPort());
            this.upstreamSocket = upstreamSocket;
            // Modify sockets
            socketModifier.accept(downstreamSocket);
            socketModifier.accept(upstreamSocket);

            try (InputStream downInStream = downstreamSocket.getInputStream();
                 OutputStream downOutStream = downstreamSocket.getOutputStream();
                 OutputStream upOutStream = upstreamSocket.getOutputStream();
                 InputStream upInStream = upstreamSocket.getInputStream()
            ) {
                toxic.start();
                Future<?> upTask = executor.submit(() -> hookupStreams(upInStream, downOutStream, "upstream"));
                hookupStreams(downInStream, upOutStream, "downstream");
                var unused = upTask.get();
            }
            catch (InterruptedException | ExecutionException e) {
                logger.warn("Exchange interrupted");
            }
        }
        catch (SocketException ex) {
            // just ignore - server socket closed forcefully
        }
        catch (IOException ex) {
            logger.error("Failed to accept connection", ex);
        }
        finally {
            toxic.stop();
            this.downstreamSocket = null;
            this.upstreamSocket = null;
        }
    }

    private void hookupStreams(InputStream inputStream, OutputStream outputStream, String logHint) {
        logger.debug("Starting exchange {}", logHint);
        byte[] buffer = new byte[proxyConfig.bufSize()];
        int len;
        try {
            while (stopped.getCount() > 0 && (len = inputStream.read(buffer)) > 0) {
                ByteBuffer toxicBuffer = toxic.transform(ByteBuffer.wrap(buffer, 0, len));
                outputStream.write(toxicBuffer.array(), 0, toxicBuffer.remaining());
            }
        }
        catch (IOException ex) {
            logger.error("Error in transmission", ex);
        }
        logger.debug("Done with exchange {}", logHint);
    }

    // Force sending RST instead of FIN on a socket when closed
    private static void resetOnClose(Socket socket) {
        try {
            socket.setSoLinger(true, 0);
        }
        catch (SocketException e) {
            logger.error("Can't modify socket behavior", e);
        }
    }
}
