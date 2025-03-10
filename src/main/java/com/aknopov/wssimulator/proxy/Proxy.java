package com.aknopov.wssimulator.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aknopov.wssimulator.SocketFactory;

/**
 * Proxy implementation for "localhost"
 */
public class Proxy {
    private static final Logger logger = LoggerFactory.getLogger(Proxy.class);

    private static final String LOCALHOST = "localhost";

    private final ProxyConfig proxyConfig;
    private final SocketFactory socketFactory;
    private final ExecutorService executor;
    private final AtomicBoolean keepGoing = new AtomicBoolean(true);

    /**
     * Creates proxy with configuration and socket factory
     *
     * @param proxyConfig proxy config
     * @param socketFactory socket factory
     */
    public Proxy(ProxyConfig proxyConfig, SocketFactory socketFactory) {
        this.proxyConfig = proxyConfig;
        this.socketFactory = socketFactory;
        this.executor = NamedThreadPool.createFixedPool(4, "Proxy");
    }

    /**
     * Starts proxying and returns immediately. You can wait for completion with  {@link Proxy#awaitTermination(Duration)}.
     */
    public void start() {
        try (ServerSocket serverSocket = socketFactory.createServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(socketAddress(proxyConfig.downPort()));

            Future<?> unused1 = executor.submit(this::sleepToShutdown);
            Future<?> unused2 = executor.submit(() -> waitForIncomingConnection(serverSocket));
        }
        catch (IOException ex) {
            logger.error("Failed to create server socket", ex);
        }
    }

    /**
     * Stops proxying immediately.
     */
    public void stop() {
        logger.debug("Stopping proxy");
        if (keepGoing.getAndSet(false)) {
            executor.shutdownNow();
        }
    }

    /**
     * Awaits termination of proxy either after call of {@link Proxy#stop()}
     * or {@link ProxyConfig#shutdownTime()} timeout
     *
     * @param waitTime waiting time
     */
    public boolean awaitTermination(Duration waitTime) {
        try {
            return executor.awaitTermination(waitTime.toMillis(), TimeUnit.MILLISECONDS);
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

    private void waitForIncomingConnection(ServerSocket serverSocket) {
        while (keepGoing.get()) {
            logger.debug("Waiting for incoming connection");
            try (Socket downstreamSocket = serverSocket.accept();
                 Socket upstreamSocket = socketFactory.creatUpsteamSocket()
            ) {
                logger.debug("Connection accepted");
                upstreamSocket.connect(socketAddress(proxyConfig.upPort()));

                try (InputStream downInStream = downstreamSocket.getInputStream();
                     OutputStream downOutStream = downstreamSocket.getOutputStream();
                     InputStream upInStream = upstreamSocket.getInputStream();
                     OutputStream upOutStream = upstreamSocket.getOutputStream()
                ) {
                    Future<?> upTask = executor.submit(() -> exchange(upInStream, downOutStream, "1"));
                    exchange(downInStream, upOutStream, "2");
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
        }
    }

    private static InetSocketAddress socketAddress(int port) {
        return new InetSocketAddress(LOCALHOST, port);
    }

    private void exchange(InputStream inputStream, OutputStream outputStream, String logHint) {
        logger.debug("Starting exchange {}", logHint);
        byte[] buffer = new byte[proxyConfig.bufSize()];
        int len;
        try {
            while (keepGoing.get() && (len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
        }
        catch (IOException ex) {
            logger.error("Error in transmission", ex);
        }
        logger.debug("Done with exchange {}", logHint);
    }
}
