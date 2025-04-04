package org.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

class ProxyServer {
    private static final int SERVER_PORT = 9090;
    private static final Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    public static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            logger.info("Proxy Server started on port " + SERVER_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Exception in startServer {}", e.getMessage());
        }
    }


    //HANDLE BOTH HTTPS/HTTP
    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8))) {

            String requestLine = in.readLine();
            if (requestLine == null) return;

            logger.info("Received request: {}",requestLine);
            String[] tokens = requestLine.split(" ");
            if (tokens.length < 2) return;

            String method = tokens[0];
            String target = tokens[1];

            if (method.equals("CONNECT")) {
                logger.info("In HTTPS");
                handleConnect(target, out, clientSocket);
            } else {
                logger.info("In HTTP");
                forwardHttpRequest(requestLine, in, out);
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("Exception in handleClient {}", e.getMessage());
        }
    }


    //HTTP METHOD
    private static void forwardHttpRequest(String requestLine, BufferedReader in, BufferedWriter out) {
        try {
            String[] requestTokens = requestLine.split(" ");
            if (requestTokens.length < 2) return;

            URL url = new URL(requestTokens[1]);
            logger.info("URL Recieved: {}", url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(requestTokens[0]);

            // Forward request headers
            String headerLine;
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                String[] headerTokens = headerLine.split(": ", 2);
                if (headerTokens.length == 2) {
                    conn.setRequestProperty(headerTokens[0], headerTokens[1]);
                }
            }

            // Forward response to client
            try (BufferedReader remoteIn = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                logger.info("Forwarding response to client");

                out.write("HTTP/1.1 " + conn.getResponseCode() + " " + conn.getResponseMessage() + "\r\n");
                for (String headerKey : conn.getHeaderFields().keySet()) {
                    if (headerKey != null) {
                        for (String headerValue : conn.getHeaderFields().get(headerKey)) {
                            out.write(headerKey + ": " + headerValue + "\r\n");
                        }
                    }
                }
                out.write("\r\n"); // End of headers

                // Send response body to client
                String line;
                while ((line = remoteIn.readLine()) != null) {
                    out.write(line + "\r\n");
                }
            } catch (IOException ignored) {
            }
            out.flush();
        } catch (IOException ignored) {
        }
    }

    //TODO : IMPLEMENT HTTPS CONNECTION
    private static void handleConnect(String target, BufferedWriter out, Socket clientSocket) {
        try {
            String[] parts = target.split(":");
            String host = parts[0];
            int port = (parts.length > 1) ? Integer.parseInt(parts[1]) : 443;

            Socket remoteSocket = new Socket(host, port);

            // Send 200 OK back to client
            out.write("HTTP/1.1 200 Connection Established\r\n\r\n");
            logger.info("HTTP/1.1 200 Connection Established");
            out.flush();

            // Relay raw byte stream (TLS)
            Thread t1 = new Thread(() -> forwardStream(clientSocket, remoteSocket));
            Thread t2 = new Thread(() -> forwardStream(remoteSocket, clientSocket));
            t1.start();
            t2.start();

            // Join to keep the thread alive until both directions are done
            t1.join();
            t2.join();

        } catch (IOException | InterruptedException e) {
            logger.error("Error handling CONNECT: " + e.getMessage());
            try {
                out.write("HTTP/1.1 502 Bad Gateway\r\n\r\n");
                out.flush();
            } catch (IOException ignored) {
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    //TODO : HTTPS METHOD
    private static void forwardStream(Socket inputSocket, Socket outputSocket) {
        try (InputStream input = inputSocket.getInputStream();
             OutputStream output = outputSocket.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                output.flush();
            }
        } catch (SocketException e) {
            System.out.println("Socket closed: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Stream forwarding error: " + e.getMessage());
        } finally {
            try {
                inputSocket.close();
                outputSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

}