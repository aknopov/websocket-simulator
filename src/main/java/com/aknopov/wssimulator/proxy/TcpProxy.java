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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.SocketFactory;
import com.aknopov.wssimulator.proxy.toxy.Toxic;
import com.aknopov.wssimulator.proxy.toxy.ToxicNoop;

/**
 * TCP proxy implementation for "localhost"
 */
public class TcpProxy {
    private static final Logger logger = LoggerFactory.getLogger(TcpProxy.class);

    private final ProxyConfig proxyConfig;
    private final SocketFactory socketFactory;
    private final ExecutorService executor;
    private final Toxic toxic;
    private final CountDownLatch stopped = new CountDownLatch(1);

    TcpProxy(ProxyConfig proxyConfig, SocketFactory socketFactory) {
        this(proxyConfig, socketFactory, new ToxicNoop());
    }

    /**
     * Creates proxy with configuration and socket factory
     *
     * @param proxyConfig proxy config
     * @param socketFactory socket factory
     * @param toxic toxy of choice
     */
    public TcpProxy(ProxyConfig proxyConfig, SocketFactory socketFactory, Toxic toxic) {
        this.proxyConfig = proxyConfig;
        this.socketFactory = socketFactory;
        this.toxic = toxic;
        this.executor = NamedThreadPool.createFixedPool(3, "TcpProxy");

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
        try (Socket upstreamSocket = socketFactory.creatUpstreamSocket(proxyConfig.upPort())) {
            logger.debug("Created proxy client on port {}", proxyConfig.upPort());

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
}
