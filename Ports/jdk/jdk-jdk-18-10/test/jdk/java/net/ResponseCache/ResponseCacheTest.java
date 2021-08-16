/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary Unit test for java.net.ResponseCache
 * @bug 4837267
 * @library /test/lib
 * @author Yingxian Wang
 */

import java.net.*;
import java.util.*;
import java.io.*;
import javax.net.ssl.*;
import jdk.test.lib.net.URIBuilder;
import static java.net.Proxy.NO_PROXY;

/**
 * Request should get serviced by the cache handler. Response get
 * saved through the cache handler.
 */
public class ResponseCacheTest implements Runnable {
    ServerSocket ss;
    static URL url1;
    static URL url2;
    static String FNPrefix, OutFNPrefix;
    static List<Closeable> streams = new ArrayList<>();
    static List<File> files = new ArrayList<>();

    /*
     * Our "http" server to return a 404 */
    public void run() {
        Socket s = null;
        FileInputStream fis = null;
        try {
            s = ss.accept();

            InputStream is = s.getInputStream ();
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            String x;
            while ((x=r.readLine()) != null) {
                if (x.length() ==0) {
                    break;
                }
            }
            PrintStream out = new PrintStream(
                                 new BufferedOutputStream(
                                    s.getOutputStream() ));

            /* send file2.1 */
            File file2 = new File(FNPrefix+"file2.1");
            out.print("HTTP/1.1 200 OK\r\n");
            out.print("Content-Type: text/html; charset=iso-8859-1\r\n");
            out.print("Content-Length: "+file2.length()+"\r\n");
            out.print("Connection: close\r\n");
            out.print("\r\n");
            fis = new FileInputStream(file2);
            byte[] buf = new byte[(int)file2.length()];
            int len;
            while ((len = fis.read(buf)) != -1) {
                out.print(new String(buf));
            }

            out.flush();

            s.close();
            ss.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { ss.close(); } catch (IOException unused) {}
            try { s.close(); } catch (IOException unused) {}
            try { fis.close(); } catch (IOException unused) {}
        }
    }
    static class NameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
    ResponseCacheTest() throws Exception {
        /* start the server */
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ss = new ServerSocket();
        ss.bind(new InetSocketAddress(loopback, 0));

        (new Thread(this)).start();
        /* establish http connection to server */
        url1 = new URL("http://localhost/file1.cache");
        HttpURLConnection http = (HttpURLConnection)url1.openConnection();
        InputStream is = null;
        System.out.println("request headers: "+http.getRequestProperties());
        System.out.println("responsecode is :"+http.getResponseCode());
        Map<String,List<String>> headers1 = http.getHeaderFields();
        try {
            is = http.getInputStream();
        } catch (IOException ioex) {
            throw new RuntimeException(ioex.getMessage());
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        String x;
        File fileout = new File(OutFNPrefix+"file1");
        PrintStream out = new PrintStream(
                                 new BufferedOutputStream(
                                    new FileOutputStream(fileout)));
        while ((x=r.readLine()) != null) {
            out.print(x+"\n");
        }
        out.flush();
        out.close();

        http.disconnect();

        // testing ResponseCacheHandler.put()
        url2 = URIBuilder.newBuilder()
                   .scheme("http")
                   .host(ss.getInetAddress())
                   .port(ss.getLocalPort())
                   .path("/file2.1")
                   .toURL();
        http = (HttpURLConnection)url2.openConnection(NO_PROXY);
        System.out.println("responsecode2 is :"+http.getResponseCode());
        Map<String,List<String>> headers2 = http.getHeaderFields();

        try {
            is = http.getInputStream();
        } catch (IOException ioex) {
            throw new RuntimeException(ioex.getMessage());
        }
        r = new BufferedReader(new InputStreamReader(is));
        fileout = new File(OutFNPrefix+"file2.2");
        out = new PrintStream(
                                 new BufferedOutputStream(
                                    new FileOutputStream(fileout)));
        while ((x=r.readLine()) != null) {
            out.print(x+"\n");
        }
        out.flush();
        out.close();

        // assert (headers1 == headers2 && file1 == file2.2)
        File file1 = new File(OutFNPrefix+"file1");
        File file2 = new File(OutFNPrefix+"file2.2");
        files.add(file1);
        files.add(file2);
        System.out.println("headers1"+headers1+"\nheaders2="+headers2);
        if (!headers1.equals(headers2) || file1.length() != file2.length()) {
            throw new RuntimeException("test failed");
        }
    }

    public static void main(String args[]) throws Exception {
        try {
            ResponseCache.setDefault(new MyResponseCache());
            FNPrefix = System.getProperty("test.src", ".")+"/";
            OutFNPrefix = System.getProperty("test.scratch", ".")+"/";
            new ResponseCacheTest();
        } finally{
            ResponseCache.setDefault(null);
            for (Closeable c: streams) {
                try { c.close(); } catch (IOException unused) {}
            }
            for (File f: files) {
                f.delete();
            }
        }
    }

    static class MyResponseCache extends ResponseCache {
        public CacheResponse get(URI uri, String rqstMethod,
                                 Map<String,List<String>> rqstHeaders)
            throws IOException
        {
            try {
                if (uri.equals(url1.toURI())) {
                    return new MyCacheResponse(FNPrefix+"file1.cache");
                }
            } catch (URISyntaxException ex) {
                throw new RuntimeException (ex);
            }
            return null;
        }

        public CacheRequest put(URI uri, URLConnection conn)  throws IOException {
            // save cache to file2.cache
            // 1. serialize headers into file2.cache
            // 2. write data to file2.cache
            return new MyCacheRequest(OutFNPrefix+"file2.cache", conn.getHeaderFields());
        }
    }

    static class MyCacheResponse extends CacheResponse {
        FileInputStream fis;
        Map<String,List<String>> headers;
        public MyCacheResponse(String filename) {
            try {
                fis = new FileInputStream(new File(filename));
                streams.add(fis);
                ObjectInputStream ois = new ObjectInputStream(fis);
                headers = (Map<String,List<String>>)ois.readObject();
            } catch (Exception ex) {
                // throw new RuntimeException(ex.getMessage());
            }
        }

        public InputStream getBody() throws IOException {
            return fis;
        }

        public Map<String,List<String>> getHeaders() throws IOException {
            return headers;
        }
    }

    static class MyCacheRequest extends CacheRequest {
        FileOutputStream fos;
        public MyCacheRequest(String filename, Map<String,List<String>> rspHeaders) {
            try {
                File file = new File(filename);
                fos = new FileOutputStream(file);
                streams.add(fos);
                files.add(file);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(rspHeaders);
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage());
            }
        }
        public OutputStream getBody() throws IOException {
            return fos;
        }

        public void abort() {
            // no op
        }
    }


}
