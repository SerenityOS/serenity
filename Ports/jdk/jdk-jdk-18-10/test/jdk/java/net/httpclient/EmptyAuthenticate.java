/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 8263899
 * @summary HttpClient throws NPE in AuthenticationFilter when parsing www-authenticate head
 *
 * @run main/othervm EmptyAuthenticate
 */
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class EmptyAuthenticate {

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        int port = 0;

        //start server:
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        port = server.getAddress().getPort();
        server.createContext("/", exchange -> {
            String response = "test body";
            //this empty header will make the HttpClient throw NPE
            exchange.getResponseHeaders().add("www-authenticate", "");
            exchange.sendResponseHeaders(401, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });
        server.start();

        HttpResponse<String> response = null;
        //run client:
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(new URI("http://localhost:" + port + "/")).GET().build();
            //this line will throw NPE (wrapped by IOException) when parsing empty www-authenticate response header in AuthenticationFilter:
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean ok = !response.headers().firstValue("WWW-Authenticate").isEmpty();
            if (!ok) {
                throw new RuntimeException("WWW-Authenicate missing");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Test failed");
        } finally {
            server.stop(0);
        }
    }
}
