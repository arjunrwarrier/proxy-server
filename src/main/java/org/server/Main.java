package org.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);


    public static void main(String[] args) {
        Socket socket = null;
        InputStreamReader inputStreamReader = null;
        OutputStreamWriter outputStreamWriter = null;
        BufferedReader in = null;
        BufferedWriter out = null;
        URL url;
        HttpURLConnection conn;
        int connectionsMade = 0;

        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(9090);
            logger.info("Server STARTED");
            while (true) {
                try {
                    socket = serverSocket.accept();
                    inputStreamReader = new InputStreamReader(socket.getInputStream());
                    outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());

                    in = new BufferedReader(inputStreamReader);
                    out = new BufferedWriter(outputStreamWriter);
                    connectionsMade+=1;
                    logger.info("Connections made to client: {}", connectionsMade);
                    while (true) {
                        String requestLine = in.readLine();
                        if (requestLine == null) {
                            logger.error("Client disconnected.");
                            break;
                        }
                        String[] requestTokens = requestLine.split(" ");
                        if (requestTokens.length < 2 || !requestTokens[0].equals("GET")) {
                            logger.error("Invalid request format: " + requestLine);
                            continue;
                        }

                        String method = requestTokens[0];
                        if (method.equalsIgnoreCase("CONNECT")) {
                            logger.error("Received HTTPS CONNECT request. Not supported. Closing connection.");
                            continue;
                        }
                        try {

                            url = new URL(requestTokens[1]);
                            conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod(requestTokens[0]);
                            conn.setRequestProperty(requestTokens[0], requestTokens[1]);


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
                                out.write("\r\n");


                                String line;
                                while ((line = remoteIn.readLine()) != null) {
                                    out.write(line + "\r\n");
                                }
                                out.write("END_OF_RESPONSE\r\n");
                                out.flush();
                                logger.info("Sended END_OF_RESPONSE to client ");
                            } catch (IOException e) {
                                logger.info("Exception in innerloop: " + e.getMessage());
                                out.write("END_OF_RESPONSE\r\n");
                            }
                        } catch (MalformedURLException e) {
                            logger.error("Incorrect url {}", e.getMessage());
                        }
                    }

                } catch (IOException | RuntimeException e) {
                    logger.error("CONNECTION LOST. {}", e.getMessage());
                } finally {
                    try {
                        if (in != null) in.close();
                        if (out != null) out.close();
                        if (socket != null && !socket.isClosed()) socket.close();
                        logger.info("Connection closed for {}", (socket != null ? socket.getInetAddress().getHostAddress() : "unknown"));
                    } catch (IOException e) {
                        logger.error("Error closing socket or streams: {}", e.getMessage());
                    }
                }
                logger.warn("Waiting for new connection");
            }
        } catch (IOException e) {
            logger.error("Could not start server or server socket error: {}", e.getMessage());
        }
    }
}