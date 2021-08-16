/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/**
   Provides a simple high-level Http server API, which can be used to build
   embedded HTTP servers. Both "http" and "https" are supported. The API provides
   a partial implementation of RFC <a href="https://www.ietf.org/rfc/rfc2616.txt">2616</a> (HTTP 1.1)
   and RFC <a href="https://www.ietf.org/rfc/rfc2818.txt">2818</a> (HTTP over TLS).
   Any HTTP functionality not provided by this API can be implemented by application code
   using the API.
   <p>
   Programmers must implement the {@link com.sun.net.httpserver.HttpHandler} interface. This interface
   provides a callback which is invoked to handle incoming requests from clients.
   A HTTP request and its response is known as an exchange. HTTP exchanges are
   represented by the {@link com.sun.net.httpserver.HttpExchange} class.
   The {@link com.sun.net.httpserver.HttpServer} class is used to listen for incoming TCP connections
   and it dispatches requests on these connections to handlers which have been
   registered with the server.
   <p>
   A minimal Http server example is shown below:
   <blockquote><pre>
   class MyHandler implements HttpHandler {
       public void handle(HttpExchange t) throws IOException {
           InputStream is = t.getRequestBody();
           read(is); // .. read the request body
           String response = "This is the response";
           t.sendResponseHeaders(200, response.length());
           OutputStream os = t.getResponseBody();
           os.write(response.getBytes());
           os.close();
       }
   }
   ...

   HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
   server.createContext("/applications/myapp", new MyHandler());
   server.setExecutor(null); // creates a default executor
   server.start();
   </pre></blockquote>
   <p>The example above creates a simple HttpServer which uses the calling
   application thread to invoke the handle() method for incoming http
   requests directed to port 8000, and to the path /applications/myapp/.
   <p>
   The {@link com.sun.net.httpserver.HttpExchange} class encapsulates everything an application needs to
   process incoming requests and to generate appropriate responses.
   <p>
   Registering a handler with a HttpServer creates a {@link com.sun.net.httpserver.HttpContext} object and
   {@link com.sun.net.httpserver.Filter}
   objects can be added to the returned context. Filters are used to perform automatic pre- and
   post-processing of exchanges before they are passed to the exchange handler.
   <p>
   For sensitive information, a {@link com.sun.net.httpserver.HttpsServer} can
   be used to process "https" requests secured by the SSL or TLS protocols.
   A HttpsServer must be provided with a
   {@link com.sun.net.httpserver.HttpsConfigurator} object, which contains an
   initialized {@link javax.net.ssl.SSLContext}.
   HttpsConfigurator can be used to configure the
   cipher suites and other SSL operating parameters.
   A simple example SSLContext could be created as follows:
   <blockquote><pre>
   char[] passphrase = "passphrase".toCharArray();
   KeyStore ks = KeyStore.getInstance("JKS");
   ks.load(new FileInputStream("testkeys"), passphrase);

   KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
   kmf.init(ks, passphrase);

   TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
   tmf.init(ks);

   SSLContext ssl = SSLContext.getInstance("TLS");
   ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
   </pre></blockquote>
   <p>
   In the example above, a keystore file called "testkeys", created with the keytool utility
   is used as a certificate store for client and server certificates.
   The following code shows how the SSLContext is then used in a HttpsConfigurator
   and how the SSLContext and HttpsConfigurator are linked to the HttpsServer.
   <blockquote><pre>
    server.setHttpsConfigurator (new HttpsConfigurator(sslContext) {
        public void configure (HttpsParameters params) {

        // get the remote address if needed
        InetSocketAddress remote = params.getClientAddress();

        SSLContext c = getSSLContext();

        // get the default parameters
        SSLParameters sslparams = c.getDefaultSSLParameters();
        if (remote.equals (...) ) {
            // modify the default set for client x
        }

        params.setSSLParameters(sslparams);
        // statement above could throw IAE if any params invalid.
        // eg. if app has a UI and parameters supplied by a user.

        }
    });
   </pre></blockquote>

   @since 1.6
 */
package com.sun.net.httpserver;
