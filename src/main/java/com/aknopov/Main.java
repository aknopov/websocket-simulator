package com.aknopov;

import java.util.Map;
import java.util.Scanner;

import org.glassfish.tyrus.server.Server;

public class Main {

    public static void main(String[] args) {
        Server server = new Server("localhost", 0, "/", Map.of(), WebSocketEndpoint2.getConfigClass());

        try {
            server.start();
            System.err.printf("Running on port %d%n", server.getPort());

            Scanner scanner = new Scanner(System.in);
            String ignored = scanner.nextLine();
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
        }
        finally {
            server.stop();
        }
    }
}
