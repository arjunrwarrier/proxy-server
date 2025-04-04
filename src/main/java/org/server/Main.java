package org.server;

public class Main {
    public static void main(String[] args) {
        new Thread(ProxyServer::startServer).start();
    }
}