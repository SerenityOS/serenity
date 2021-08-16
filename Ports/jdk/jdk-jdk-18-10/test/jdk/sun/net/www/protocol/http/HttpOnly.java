/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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
/**
 * @test
 * @bug 7095980 8007315
 * @modules jdk.httpserver
 * @library /test/lib
 * @summary Ensure HttpURLConnection (and supporting APIs) don't expose
 *          HttpOnly cookies
 * @run main HttpOnly
 * @run main/othervm -Djava.net.preferIPv6Addresses=true HttpOnly
 */

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import jdk.test.lib.net.URIBuilder;

/*
 * 1) start the HTTP server
 * 2) populate cookie store with HttpOnly cookies
 * 3) make HTTP request that should contain HttpOnly cookies
 * 4) check HttpOnly cookies received by server
 * 5) server reply with Set-Cookie containing HttpOnly cookie
 * 6) check HttpOnly cookies are not accessible from Http client
 * 7) check that non-null (empty string) values are returned for
      scenario where all values are stripped from original key values
 */

public class HttpOnly {

    static final String URI_PATH = "/xxyyzz/";
    static final int SESSION_ID = 12345;

     void test(String[] args) throws Exception {
        HttpServer server = startHttpServer();
        CookieHandler previousHandler = CookieHandler.getDefault();
        try {
            InetSocketAddress address = server.getAddress();
            URI uri = URIBuilder.newBuilder()
                                .scheme("http")
                                .host(address.getAddress())
                                .port(address.getPort())
                                .path(URI_PATH)
                                .build();
            populateCookieStore(uri);
            doClient(uri);
        } finally {
            CookieHandler.setDefault(previousHandler);
            server.stop(0);
        }
    }

    void populateCookieStore(URI uri)
            throws IOException {

        CookieManager cm = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cm);
        Map<String,List<String>> header = new HashMap<>();
        List<String> values = new ArrayList<>();
        values.add("JSESSIONID=" + SESSION_ID + "; version=1; Path="
                   + URI_PATH +"; HttpOnly");
        values.add("CUSTOMER=WILE_E_COYOTE; version=1; Path=" + URI_PATH);
        header.put("Set-Cookie", values);
        cm.put(uri, header);
    }

    void doClient(URI uri) throws Exception {
        HttpURLConnection uc = (HttpURLConnection) uri.toURL().openConnection(Proxy.NO_PROXY);
        int resp = uc.getResponseCode();
        check(resp == 200,
              "Unexpected response code. Expected 200, got " + resp);

        // TEST 1: check getRequestProperty doesn't return the HttpOnly cookie
        // In fact, that it doesn't return any automatically set cookies.
        String cookie = uc.getRequestProperty("Cookie");
        check(cookie == null,
              "Cookie header returned from getRequestProperty, value " + cookie);

        // TEST 2: check getRequestProperties doesn't return the HttpOnly cookie.
        // In fact, that it doesn't return any automatically set cookies.
        Map<String,List<String>> reqHeaders = uc.getRequestProperties();
        Set<Map.Entry<String,List<String>>> entries = reqHeaders.entrySet();
        for (Map.Entry<String,List<String>> entry : entries) {
            String header = entry.getKey();
            check(!"Cookie".equalsIgnoreCase(header),
                  "Cookie header returned from getRequestProperties, value " +
                         entry.getValue());
        }

        // TEST 3: check getHeaderField doesn't return Set-Cookie with HttpOnly
        String setCookie = uc.getHeaderField("Set-Cookie");
        if (setCookie != null) {
            debug("Set-Cookie:" + setCookie);
            check(!setCookie.toLowerCase().contains("httponly"),
                  "getHeaderField returned Set-Cookie header with HttpOnly, " +
                  "value = " + setCookie);
        }

        // TEST 3.5: check getHeaderField doesn't return Set-Cookie2 with HttpOnly
        String setCookie2 = uc.getHeaderField("Set-Cookie2");
        if (setCookie2 != null) {
            debug("Set-Cookie2:" + setCookie2);
            check(!setCookie2.toLowerCase().contains("httponly"),
                  "getHeaderField returned Set-Cookie2 header with HttpOnly, " +
                  "value = " + setCookie2);
        }

        // TEST 4: check getHeaderFields doesn't return Set-Cookie
        //         or Set-Cookie2 headers with HttpOnly
        Map<String,List<String>> respHeaders = uc.getHeaderFields();
        Set<Map.Entry<String,List<String>>> respEntries = respHeaders.entrySet();
        for (Map.Entry<String,List<String>> entry : respEntries) {
            String header = entry.getKey();
            if ("Set-Cookie".equalsIgnoreCase(header)) {
                List<String> setCookieValues = entry.getValue();
                debug("Set-Cookie:" + setCookieValues);
                for (String value : setCookieValues)
                    check(!value.toLowerCase().contains("httponly"),
                          "getHeaderFields returned Set-Cookie header with HttpOnly, "
                          + "value = " + value);
            }
            if ("Set-Cookie2".equalsIgnoreCase(header)) {
                List<String> setCookieValues = entry.getValue();
                debug("Set-Cookie2:" + setCookieValues);
                for (String value : setCookieValues)
                    check(!value.toLowerCase().contains("httponly"),
                          "getHeaderFields returned Set-Cookie2 header with HttpOnly, "
                          + "value = " + value);
            }
        }

        // Now add some user set cookies into the mix.
        uc = (HttpURLConnection) uri.toURL().openConnection(Proxy.NO_PROXY);
        uc.addRequestProperty("Cookie", "CUSTOMER_ID=CHEGAR;");
        resp = uc.getResponseCode();
        check(resp == 200,
              "Unexpected response code. Expected 200, got " + resp);

        // TEST 5: check getRequestProperty doesn't return the HttpOnly cookie
        cookie = uc.getRequestProperty("Cookie");
        check(!cookie.toLowerCase().contains("httponly"),
              "HttpOnly cookie returned from getRequestProperty, value " + cookie);

        // TEST 6: check getRequestProperties doesn't return the HttpOnly cookie.
        reqHeaders = uc.getRequestProperties();
        entries = reqHeaders.entrySet();
        for (Map.Entry<String,List<String>> entry : entries) {
            String header = entry.getKey();
            if ("Cookie".equalsIgnoreCase(header)) {
                for (String val : entry.getValue())
                    check(!val.toLowerCase().contains("httponly"),
                          "HttpOnly cookie returned from getRequestProperties," +
                          " value " + val);
            }
        }

        // TEST 7 : check that header keys containing empty key values don't return null
        int i = 1;
        String key = "";
        String value = "";

        while (true) {
            key = uc.getHeaderFieldKey(i);
            value = uc.getHeaderField(i++);
            if (key == null && value == null)
                break;

            if (key != null)
                check(value != null,
                    "Encountered a null value for key value : " + key);
        }

        // TEST 7.5 similar test but use getHeaderFields
        respHeaders = uc.getHeaderFields();
        respEntries = respHeaders.entrySet();
        for (Map.Entry<String,List<String>> entry : respEntries) {
            String header = entry.getKey();
            if (header != null) {
                List<String> listValues = entry.getValue();
                for (String value1 : listValues)
                    check(value1 != null,
                        "getHeaderFields returned null values for header:, "
                        + header);
            }
        }
    }

    // HTTP Server
    HttpServer startHttpServer() throws IOException {
        InetAddress localhost = InetAddress.getLocalHost();
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(localhost, 0), 0);
        httpServer.createContext(URI_PATH, new SimpleHandler());
        httpServer.start();
        return httpServer;
    }

    class SimpleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Headers reqHeaders = t.getRequestHeaders();

            // some small sanity check
            List<String> cookies = reqHeaders.get("Cookie");
            for (String cookie : cookies) {
                if (!cookie.contains("JSESSIONID")
                    || !cookie.contains("WILE_E_COYOTE"))
                    t.sendResponseHeaders(400, -1);
            }

            // return some cookies so we can check getHeaderField(s)
            Headers respHeaders = t.getResponseHeaders();
            List<String> values = new ArrayList<>();
            values.add("ID=JOEBLOGGS; version=1; Path=" + URI_PATH);
            values.add("NEW_JSESSIONID=" + (SESSION_ID+1) + "; version=1; Path="
                       + URI_PATH +"; HttpOnly");
            values.add("NEW_CUSTOMER=WILE_E_COYOTE2; version=1; Path=" + URI_PATH);
            respHeaders.put("Set-Cookie", values);
            values = new ArrayList<>();
            values.add("COOKIE2_CUSTOMER=WILE_E_COYOTE2; version=1; Path="
                       + URI_PATH);
            respHeaders.put("Set-Cookie2", values);
            values.add("COOKIE2_JSESSIONID=" + (SESSION_ID+100)
                       + "; version=1; Path=" + URI_PATH +"; HttpOnly");
            respHeaders.put("Set-Cookie2", values);

            t.sendResponseHeaders(200, -1);
            t.close();
        }
    }

    volatile int passed = 0, failed = 0;
    boolean debug = false;
    void pass() {passed++;}
    void fail() {failed++;}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void debug(String message) { if (debug) System.out.println(message); }
    void check(boolean cond, String failMessage) {if (cond) pass(); else fail(failMessage);}
    public static void main(String[] args) throws Throwable {
        Class<?> k = new Object(){}.getClass().getEnclosingClass();
        try {k.getMethod("instanceMain",String[].class)
                .invoke( k.newInstance(), (Object) args);}
        catch (Throwable e) {throw e.getCause();}}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
