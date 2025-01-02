/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.codec.exampleDocker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.apache.commons.codec.CodecPolicy;
import org.apache.commons.codec.binary.Base64;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ExampleDocker {
    private static final int PORT = 8080;
    private static final String CHAR_ENCODING = "UTF-8";

    public static void main(String[] args) throws IOException {
        // Crea il server HTTP
        final HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        System.out.println("Server running on http://localhost:" + PORT);

        // Registra il percorso per la GET sulla radice (/) che restituisce il file HTML
        server.createContext("/", new HtmlHandler());

        // Registra il percorso /encode per la codifica
        server.createContext("/encode", new EncodeHandler());

        // Avvia il server
        server.setExecutor(null); // Usa il default executor
        server.start();
    }

    // Handler per la GET sulla radice (/) che serve la pagina HTML
    static class HtmlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Leggi il file HTML da disco
            final String htmlContent = readHtmlFile("/Users/leonardofalanga/Downloads/" +
                    "commons-codec/src/main/java/org/apache/commons/codec/exampleDocker/index.html");

            // Imposta le intestazioni della risposta
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, htmlContent.getBytes(CHAR_ENCODING).length);

            // Scrivi il contenuto HTML nella risposta
            final OutputStream os = exchange.getResponseBody();
            os.write(htmlContent.getBytes(CHAR_ENCODING));
            os.close();
        }

        // Funzione per leggere il contenuto del file HTML
        private String readHtmlFile(String fileName) throws IOException {
            final BufferedReader reader = new BufferedReader(new FileReader(fileName));
                final StringBuilder htmlContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    htmlContent.append(line).append("\n");
                }
                reader.close();
            return htmlContent.toString();

        }
    }


    // Handler per la POST su /encode per la codifica
    static class EncodeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                final InputStream input = exchange.getRequestBody();
                final BufferedReader reader = new BufferedReader(new InputStreamReader(input, CHAR_ENCODING));
                final StringBuilder requestBody = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    requestBody.append(line);
                }

                try {
                    final Base64 codec = new Base64(0, null, false, CodecPolicy.STRICT);
                    final byte[] response = codec.encode(requestBody.toString().getBytes(CHAR_ENCODING));
                    exchange.getResponseHeaders().add("Content-Type", "text/plain");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                } catch (Exception e) {
                    final String error = "Error: " + e.getMessage();
                    exchange.sendResponseHeaders(500, error.length());
                    exchange.getResponseBody().write(error.getBytes());
                } finally {
                    exchange.getResponseBody().close();
                }
            } else {
                final String error = "Method not allowed";
                exchange.sendResponseHeaders(405, error.length());
                exchange.getResponseBody().write(error.getBytes());
                exchange.getResponseBody().close();
            }
        }
    }
}

