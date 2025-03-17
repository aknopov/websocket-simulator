package com.aknopov.wssimulator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Just for mocking in unit tests
 */
//VisibleForTesting
public class SocketFactory {
    private static final Logger logger = LoggerFactory.getLogger(SocketFactory.class);
    private static final int BACKLOG_LENGTH = 50;

    public static int getAvailablePort() {
        try (ServerSocket tempSocket = new ServerSocket(0)) {
            return tempSocket.getLocalPort();
        }
        catch (IOException e) {
            logger.error("Can't get available port", e);
            return 0;
        }
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        return new ServerSocket(port, BACKLOG_LENGTH, null);
    }


    public Socket creatUpstreamSocket(int port) throws IOException {
        return new Socket("localhost", port);
    }
}
