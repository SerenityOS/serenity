/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5026745 6631048
 * @modules jdk.httpserver
 * @library /test/lib
 * @run main/othervm/timeout=500 Test
 * @summary Cannot flush output stream when writing to an HttpUrlConnection
 */

import java.io.*;
import java.net.*;
import com.sun.net.httpserver.*;

import jdk.test.lib.net.URIBuilder;

public class Test implements HttpHandler {

    static volatile int count = 0;

    static final String str1 = "Helloworld1234567890abcdefghijklmnopqrstuvwxyz"+
                                "1234567890abcdefkjsdlkjflkjsldkfjlsdkjflkj"+
                                "1434567890abcdefkjsdlkjflkjsldkfjlsdkjflkj";

    static final String str2 = "Helloworld1234567890abcdefghijklmnopqrstuvwxyz"+
                                "1234567890";

    public void handle(HttpExchange exchange) {
        String reqbody;
        try {
            switch (exchange.getRequestURI().toString()) {
            case "/test/test1": /* test1 -- keeps conn alive */
            case "/test/test2": /* test2 -- closes conn */
                printRequestURI(exchange);
                reqbody = read(exchange.getRequestBody());
                if (!reqbody.equals(str1)) {
                    exchange.sendResponseHeaders(500, 0);
                    break;
                }

                Headers headers = exchange.getRequestHeaders();
                String chunk =  headers.getFirst("Transfer-encoding");

                if (!"chunked".equals (chunk)) {
                    exchange.sendResponseHeaders(501, 0);
                    break;
                }

                exchange.sendResponseHeaders(200, reqbody.length());
                write(exchange.getResponseBody(), reqbody);

                if (count == 1) {
                    Headers resHeaders = exchange.getResponseHeaders() ;
                    resHeaders.set("Connection", "close");
                }
                break;
            case "/test/test3": /* test 3 */
                printRequestURI(exchange);
                reqbody = read(exchange.getRequestBody());

                if (!reqbody.equals(str2)) {
                    exchange.sendResponseHeaders(500, 0);
                    break;
                }
                headers = exchange.getRequestHeaders();
                int clen = Integer.parseInt( headers.getFirst("Content-length"));

                if (clen != str2.length()) {
                    exchange.sendResponseHeaders(501, 0);
                    break;
                }
                Headers resHeaders = exchange.getResponseHeaders() ;
                resHeaders.set("Connection", "close");

                exchange.sendResponseHeaders(200, reqbody.length());
                write(exchange.getResponseBody(), reqbody);
                break;
            case "/test/test4": /* test 4 */
            case "/test/test5": /* test 5 */
                printRequestURI(exchange);
                break;
            case "/test/test6": /* test 6 */
                printRequestURI(exchange);
                resHeaders = exchange.getResponseHeaders() ;
                resHeaders.set("Location", "http://foo.bar/");
                resHeaders.set("Connection", "close");
                exchange.sendResponseHeaders(307, 0);
                break;
            case "/test/test7": /* test 7 */
            case "/test/test8": /* test 8 */
                printRequestURI(exchange);
                reqbody = read(exchange.getRequestBody());
                if (reqbody != null && !"".equals(reqbody)) {
                    exchange.sendResponseHeaders(501, 0);
                    break;
                }
                resHeaders = exchange.getResponseHeaders() ;
                resHeaders.set("Connection", "close");
                exchange.sendResponseHeaders(200, 0);
                break;
            case "/test/test9": /* test 9 */
                printRequestURI(exchange);
                reqbody = read(exchange.getRequestBody());
                if (!reqbody.equals(str1)) {
                    exchange.sendResponseHeaders(500, 0);
                    break;
                }

                headers = exchange.getRequestHeaders();
                chunk =  headers.getFirst("Transfer-encoding");
                if (!"chunked".equals(chunk)) {
                    exchange.sendResponseHeaders(501, 0);
                    break;
                }

                exchange.sendResponseHeaders(200, reqbody.length());
                write(exchange.getResponseBody(), reqbody);
                break;
            case "/test/test10": /* test10 */
                printRequestURI(exchange);
                InputStream is = exchange.getRequestBody();
                String s = read (is, str1.length());

                boolean error = false;
                for (int i=10; i< 200 * 1024; i++) {
                    byte c = (byte)is.read();

                    if (c != (byte)i) {
                        error = true;
                        System.out.println ("error at position " + i);
                    }
                }
                if (!s.equals(str1) ) {
                    System.out.println ("received string : " + s);
                    exchange.sendResponseHeaders(500, 0);
                } else if (error) {
                    System.out.println ("error");
                    exchange.sendResponseHeaders(500, 0);
                } else {
                    exchange.sendResponseHeaders(200, 0);
                }
                break;
            case "/test/test11": /* test11 */
                printRequestURI(exchange);
                is = exchange.getRequestBody();
                s = read (is, str1.length());

                error = false;
                for (int i=10; i< 30 * 1024; i++) {
                    byte c = (byte)is.read();

                    if (c != (byte)i) {
                        error = true;
                        System.out.println ("error at position " + i);
                    }
                }
                if (!s.equals(str1) ) {
                    System.out.println ("received string : " + s);
                    exchange.sendResponseHeaders(500, 0);
                } else if (error) {
                    System.out.println ("error");
                    exchange.sendResponseHeaders(500, 0);
                } else {
                    exchange.sendResponseHeaders(200, 0);
                }
                break;
            case "/test/test12": /* test12 */
                printRequestURI(exchange);
                is = exchange.getRequestBody();

                error = false;
                for (int i=10; i< 30 * 1024; i++) {
                    byte c = (byte)is.read();

                    if (c != (byte)i) {
                        error = true;
                        System.out.println ("error at position " + i);
                    }
                }
                if (error) {
                    System.out.println ("error");
                    exchange.sendResponseHeaders(500, 0);
                } else {
                    exchange.sendResponseHeaders(200, 0);
                }
                break;
            }
            count ++;
            exchange.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void printRequestURI(HttpExchange exchange) {
        URI uri = exchange.getRequestURI();
        System.out.println("HttpServer: handle " + uri);
    }


    static String read (InputStream is, int len) {
        try {
            byte[] ba = new byte [len];
            int c;
            int l = 0;
            while ((c= is.read(ba, l, ba.length-l)) != -1 && l<len)  {
                l += c;
            }
            return new String (ba, 0, l, "ISO8859-1");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static String read(InputStream is) {
        try {
            byte[] ba = new byte [8096];
            int off = 0, c;
            while ((c= is.read(ba, off, ba.length)) != -1)  {
                off += c;
            }
            return new String(ba, 0, off, "ISO8859-1");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static void write(OutputStream os, String str) {
        try {
            byte[] ba = str.getBytes("ISO8859-1");
            os.write(ba);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void readAndCompare(InputStream is, String cmp) throws IOException {
        int c;
        byte buf[] = new byte [1024];
        int off = 0;
        int len = 1024;
        while ((c=is.read(buf, off, len)) != -1) {
            off += c;
            len -= c;
        }
        String s1 = new String(buf, 0, off, "ISO8859_1");
        if (!cmp.equals(s1)) {
            throw new IOException("strings not same");
        }
    }

    /* basic chunked test (runs twice) */

    static void test1(URL url) throws Exception {
        System.out.println("client opening connection to: " + url);
        HttpURLConnection urlc = (HttpURLConnection)url.openConnection ();
        urlc.setChunkedStreamingMode (20);
        urlc.setDoOutput(true);
        urlc.setRequestMethod ("POST");
        OutputStream os = urlc.getOutputStream ();
        os.write (str1.getBytes());
        os.close();
        InputStream is = urlc.getInputStream();
        readAndCompare (is, str1);
        is.close();
    }

    /* basic fixed length test */

    static void test3(URL url) throws Exception {
        System.out.println("client opening connection to: " + url);
        HttpURLConnection urlc = (HttpURLConnection)url.openConnection ();
        urlc.setFixedLengthStreamingMode (str2.length());
        urlc.setDoOutput(true);
        urlc.setRequestMethod ("POST");
        OutputStream os = urlc.getOutputStream ();
        os.write (str2.getBytes());
        os.close();
        InputStream is = urlc.getInputStream();
        readAndCompare (is, str2);
        is.close();
    }

    /* write too few bytes */

    static void test4(URL url) throws Exception {
        System.out.println("client opening connection to: " + url);
        HttpURLConnection urlc = (HttpURLConnection)url.openConnection ();
        urlc.setFixedLengthStreamingMode (str2.length()+1);
        urlc.setDoOutput(true);
        urlc.setRequestMethod ("POST");
        OutputStream os = urlc.getOutputStream ();
        os.write (str2.getBytes());
        try {
            os.close();
            throw new Exception ("should have thrown IOException");
        } catch (IOException e) {}
    }

    /* write too many bytes */

    static void test5(URL url) throws Exception {
        System.out.println("client opening connection to: " + url);
        HttpURLConnection urlc = (HttpURLConnection)url.openConnection ();
        urlc.setFixedLengthStreamingMode (str2.length()-1);
        urlc.setDoOutput(true);
        urlc.setRequestMethod ("POST");
        OutputStream os = urlc.getOutputStream ();
        try {
            os.write (str2.getBytes());
            throw new Exception ("should have thrown IOException");
        } catch (IOException e) {}
    }

    /* check for HttpRetryException on redirection */

    static void test6(URL url) throws Exception {
        System.out.println("client opening connection to: " + url);
        HttpURLConnection urlc = (HttpURLConnection)url.openConnection ();
        urlc.setChunkedStreamingMode (20);
        urlc.setDoOutput(true);
        urlc.setRequestMethod ("POST");
        OutputStream os = urlc.getOutputStream ();
        os.write (str1.getBytes());
        os.close();
        try {
            InputStream is = urlc.getInputStream();
            throw new Exception ("should have gotten HttpRetryException");
        } catch (HttpRetryException e) {
            if (e.responseCode() != 307) {
                throw new Exception ("Wrong response code " + e.responseCode());
            }
            if (!e.getLocation().equals ("http://foo.bar/")) {
                throw new Exception ("Wrong location " + e.getLocation());
            }
        }
    }

    /* next two tests send zero length posts */

    static void test7(URL url) throws Exception {
        System.out.println("client opening connection to: " + url);
        HttpURLConnection urlc = (HttpURLConnection)url.openConnection ();
        urlc.setChunkedStreamingMode (20);
        urlc.setDoOutput(true);
        urlc.setRequestMethod ("POST");
        OutputStream os = urlc.getOutputStream ();
        os.close();
        int ret = urlc.getResponseCode();
        if (ret != 200) {
            throw new Exception ("Expected 200: got " + ret);
        }
    }

    static void test8(URL url) throws Exception {
        System.out.println("client opening connection to: " + url);
        HttpURLConnection urlc = (HttpURLConnection)url.openConnection ();
        urlc.setFixedLengthStreamingMode (0);
        urlc.setDoOutput(true);
        urlc.setRequestMethod ("POST");
        OutputStream os = urlc.getOutputStream ();
        os.close();
        int ret = urlc.getResponseCode();
        if (ret != 200) {
            throw new Exception ("Expected 200: got " + ret);
        }
    }

    /* calling setChunkedStreamingMode with -1 should entail using
       the default chunk size */
    static void test9(URL url) throws Exception {
        System.out.println("client opening connection to: " + url);
        HttpURLConnection urlc = (HttpURLConnection)url.openConnection ();
        urlc.setChunkedStreamingMode (-1);
        urlc.setDoOutput(true);
        urlc.setRequestMethod ("POST");
        OutputStream os = urlc.getOutputStream ();
        os.write (str1.getBytes());
        os.close();
        InputStream is = urlc.getInputStream();
        readAndCompare (is, str1);
        is.close();
    }

    static void test10(URL url) throws Exception {
        System.out.println("client opening connection to: " + url);
        HttpURLConnection urlc = (HttpURLConnection)url.openConnection ();
        urlc.setChunkedStreamingMode (4 * 1024);
        urlc.setDoOutput(true);
        urlc.setRequestMethod ("POST");
        OutputStream os = urlc.getOutputStream ();
        byte[] buf = new byte [200 * 1024];
        for (int i=0; i< 200 * 1024; i++) {
            buf[i] = (byte) i;
        }
        /* write a small bit first, and then the large buffer */
        os.write (str1.getBytes());
        os.write (buf, 10, buf.length - 10); /* skip 10 bytes to test offset */
        os.close();
        InputStream is = urlc.getInputStream();
        is.close();
        int ret = urlc.getResponseCode();
        if (ret != 200) {
            throw new Exception ("Expected 200: got " + ret);
        }
    }

    static void test11(URL url) throws Exception {
        System.out.println("client opening connection to: " + url);
        HttpURLConnection urlc = (HttpURLConnection)url.openConnection ();
        urlc.setChunkedStreamingMode (36 * 1024);
        urlc.setDoOutput(true);
        urlc.setRequestMethod ("POST");
        OutputStream os = urlc.getOutputStream ();
        byte[] buf = new byte [30 * 1024];
        for (int i=0; i< 30 * 1024; i++) {
            buf[i] = (byte) i;
        }
        /* write a small bit first, and then the large buffer */
        os.write (str1.getBytes());
        //os.write (buf, 10, buf.length - 10); /* skip 10 bytes to test offset */
        os.write (buf, 10, (10 * 1024) - 10);
        os.write (buf, (10 * 1024), (10 * 1024));
        os.write (buf, (20 * 1024), (10 * 1024));
        os.close();
        InputStream is = urlc.getInputStream();
        is.close();
        int ret = urlc.getResponseCode();
        if (ret != 200) {
            throw new Exception ("Expected 200: got " + ret);
        }
    }

    static void test12(URL url) throws Exception {
        System.out.println("client opening connection to: " + url);
        HttpURLConnection urlc = (HttpURLConnection)url.openConnection ();
        urlc.setChunkedStreamingMode (36 * 1024);
        urlc.setDoOutput(true);
        urlc.setRequestMethod ("POST");
        OutputStream os = urlc.getOutputStream ();
        byte[] buf = new byte [30 * 1024];
        for (int i=0; i< 30 * 1024; i++) {
            buf[i] = (byte) i;
        }
        os.write (buf, 10, buf.length - 10); /* skip 10 bytes to test offset */
        os.close();
        InputStream is = urlc.getInputStream();
        is.close();
        int ret = urlc.getResponseCode();
        if (ret != 200) {
            throw new Exception ("Expected 200: got " + ret);
        }
    }


    static HttpServer httpserver;

    private static URL buildTestURL(int port, String path)
            throws MalformedURLException, URISyntaxException {
        return URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(port)
                .path(path)
                .toURL();
    }

    public static void main (String[] args) throws Exception {
        try {
            httpserver = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            httpserver.createContext("/test/", new Test());
            httpserver.start();

            int port = httpserver.getAddress().getPort();

            System.out.println ("Server started: listening on port: " + port);
            test1(buildTestURL(port, "/test/test1"));
            test1(buildTestURL(port, "/test/test2"));
            test3(buildTestURL(port, "/test/test3"));
            test4(buildTestURL(port, "/test/test4"));
            test5(buildTestURL(port, "/test/test5"));
            test6(buildTestURL(port, "/test/test6"));
            test7(buildTestURL(port, "/test/test7"));
            test8(buildTestURL(port, "/test/test8"));
            test9(buildTestURL(port, "/test/test9"));
            test10(buildTestURL(port, "/test/test10"));
            test11(buildTestURL(port, "/test/test11"));
            test12(buildTestURL(port, "/test/test12"));
        } finally {
            if (httpserver != null)
                httpserver.stop(0);
        }
    }

}
