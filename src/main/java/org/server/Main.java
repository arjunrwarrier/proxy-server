package org.server;

import java.io.*;
import java.net.*;

public class Main {
    public static void main(String[] args) throws IOException {
        Socket socket = null;
        InputStreamReader inputStreamReader = null;
        OutputStreamWriter outputStreamWriter = null;
        BufferedReader in = null;
        BufferedWriter out = null;
        URL url;
        HttpURLConnection conn;

        ServerSocket serverSocket = null;

        serverSocket = new ServerSocket(9090);

        while (true) {
            try {
                socket = serverSocket.accept();
                inputStreamReader = new InputStreamReader(socket.getInputStream());
                outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());

                in = new BufferedReader(inputStreamReader);
                out = new BufferedWriter(outputStreamWriter);

                System.out.println("STARTED");
                while (true) {
                    System.out.println("CLIENT CONNECTED");
                    String requestLine = in.readLine();
                    System.out.println("msgFromClient: " + requestLine);
                    if (requestLine == null) {
                        System.out.println("Client disconnected.");
                        break;
                    }
                    String[] requestTokens = requestLine.split(" ");
                    if (requestTokens.length < 2 || !requestTokens[0].equals("GET")) {
                        System.out.println("Invalid request format: " + requestLine);
                        continue;
                    }

                    String method = requestTokens[0];
                    if (method.equalsIgnoreCase("CONNECT")) {
                        System.out.println("Received HTTPS CONNECT request. Not supported. Closing connection.");
                    }
                    System.out.println("request0 : " + requestTokens[0]);
                    System.out.println("request0 : " + requestTokens[1]);
                        url = new URL(requestTokens[1]);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod(requestTokens[0]);
                        conn.setRequestProperty(requestTokens[0], requestTokens[1]);


                    try (BufferedReader remoteIn = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        System.out.println("Forwarding response to client");

                        out.write("HTTP/1.1 " + conn.getResponseCode() + " " + conn.getResponseMessage() + "\r\n");
                        for (String headerKey : conn.getHeaderFields().keySet()) {
                            if (headerKey != null) {
                                for (String headerValue : conn.getHeaderFields().get(headerKey)) {
                                    out.write(headerKey + ": " + headerValue + "\r\n");
                                    System.out.println(headerKey + ": " + headerValue);
                                }
                            }
                        }
                        out.write("\r\n");


                        String line;
                        while ((line = remoteIn.readLine()) != null) {
                            out.write(line + "\r\n");
                        }
                        System.out.println("HIIII");
                        out.write("\n");
                        out.flush();
                    } catch (IOException e) {
                        System.out.println("Exception in innerloop: "+e.getMessage());
                    }

                }

            } catch (IOException | RuntimeException e) {
                throw new RuntimeException(e);
            }
        }
    }
}