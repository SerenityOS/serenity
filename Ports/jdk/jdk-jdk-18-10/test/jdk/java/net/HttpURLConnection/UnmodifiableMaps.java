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
 * @bug 7128648
 * @library /test/lib
 * @modules jdk.httpserver
 * @summary HttpURLConnection.getHeaderFields should return an unmodifiable Map
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.Headers;
import static java.net.Proxy.NO_PROXY;
import jdk.test.lib.net.URIBuilder;

public class UnmodifiableMaps {

    void test(String[] args) throws Exception {
        HttpServer server = startHttpServer();
        try {
            InetSocketAddress address = server.getAddress();
            URI uri = URIBuilder.newBuilder()
                .scheme("http")
                .host(address.getAddress())
                .port(address.getPort())
                .path("/foo")
                .build();
            doClient(uri);
        } finally {
            server.stop(0);
        }
    }

    void doClient(URI uri) throws Exception {
        HttpURLConnection uc = (HttpURLConnection) uri.toURL().openConnection(NO_PROXY);

        // Test1: getRequestProperties is unmodifiable
        System.out.println("Check getRequestProperties");
        checkUnmodifiable(uc.getRequestProperties());
        uc.addRequestProperty("X", "V");
        uc.addRequestProperty("X1", "V1");
        checkUnmodifiable(uc.getRequestProperties());

        int resp = uc.getResponseCode();
        check(resp == 200,
              "Unexpected response code. Expected 200, got " + resp);

        // Test2: getHeaderFields is unmodifiable
        System.out.println("Check getHeaderFields");
        checkUnmodifiable(uc.getHeaderFields());
        // If the implementation does caching, check again.
        checkUnmodifiable(uc.getHeaderFields());
    }

    // HTTP Server
    HttpServer startHttpServer() throws IOException {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
        httpServer.createContext("/foo", new SimpleHandler());
        httpServer.start();
        return httpServer;
    }

    class SimpleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Headers respHeaders = t.getResponseHeaders();
            // ensure some response headers, over the usual ones
            respHeaders.add("RespHdr1", "Value1");
            respHeaders.add("RespHdr2", "Value2");
            respHeaders.add("RespHdr3", "Value3");
            t.sendResponseHeaders(200, -1);
            t.close();
        }
    }

    void checkUnmodifiable(Map<String,List<String>> map) {
        checkUnmodifiableMap(map);

        // Now check the individual values
        Collection<List<String>> values = map.values();
        for (List<String> value : values) {
            checkUnmodifiableList(value);
        }
    }

    void checkUnmodifiableMap(final Map<String,List<String>> map) {
        expectThrow( new Runnable() {
            public void run() { map.clear(); }});
        expectThrow( new Runnable() {
            public void run() { map.put("X", new ArrayList<String>()); }});
        expectThrow( new Runnable() {
            public void run() { map.remove("X"); }});
    }

    void checkUnmodifiableList(final List<String> list) {
        expectThrow( new Runnable() {
            public void run() { list.clear(); }});
        expectThrow( new Runnable() {
            public void run() { list.add("X"); }});
        expectThrow( new Runnable() {
            public void run() { list.remove("X"); }});
    }

    void expectThrow(Runnable r) {
        try { r.run(); fail("Excepted UOE to be thrown."); Thread.dumpStack(); }
        catch (UnsupportedOperationException e) { pass(); }
    }

    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++;}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
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
