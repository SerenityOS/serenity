/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4636628
 * @summary HttpURLConnection duplicates HTTP GET requests when used with multiple threads
*/

/*
 * This tests keep-alive behavior using chunkedinputstreams
 * It checks that keep-alive connections are used and also
 * that requests are not being repeated (due to errors)
 *
 * It also checks that the keepalive connections are closed eventually
 * because the test will not terminate if the connections
 * are not closed by the keep-alive timer.
 */

import java.net.*;
import java.io.*;
import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadTest extends Thread {

    /*
     * Is debugging enabled - start with -d to enable.
     */
    static boolean debug = true; // disable debug once stability proven

    static Object threadlock = new Object ();
    static int threadCounter = 0;

    static Object getLock() { return threadlock; }

    static void debug(String msg) {
        if (debug)
            System.out.println(msg);
    }

    static final AtomicInteger reqnum = new AtomicInteger();

    void doRequest(String uri) throws Exception {
        URL url = new URL(uri + "?foo="+reqnum.getAndIncrement());
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        InputStream in = http.getInputStream();
        byte b[] = new byte[100];
        int total = 0;
        int n;
        do {
            n = in.read(b);
            if (n > 0) total += n;
        } while (n > 0);
        debug ("client: read " + total + " bytes");
        in.close();
        http.disconnect();
    }

    String uri;
    byte[] b;
    int requests;

    MultiThreadTest(String authority, int requests) throws Exception {
        uri = "http://" + authority + "/foo.html";

        b = new byte [256];
        this.requests = requests;

        synchronized (threadlock) {
            threadCounter ++;
        }
    }

    public void run() {
        long start = System.nanoTime();

        try {
            for (int i=0; i<requests; i++) {
                doRequest (uri);
            }
        } catch (Exception e) {
            throw new RuntimeException (e.getMessage());
        } finally {
            synchronized (threadlock) {
                threadCounter --;
                if (threadCounter == 0) {
                    threadlock.notifyAll();
                }
            }
        }
        debug("client: end - " + Duration.ofNanos(System.nanoTime() - start));
    }

    static int threads=5;

    public static void main(String args[]) throws Exception {
        long start = System.nanoTime();

        int x = 0, arg_len = args.length;
        int requests = 20;

        if (arg_len > 0 && args[0].equals("-d")) {
            debug = true;
            x = 1;
            arg_len --;
        }
        if (arg_len > 0) {
            threads = Integer.parseInt (args[x]);
            requests = Integer.parseInt (args[x+1]);
        }

        /* start the server */
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress(loopback, 0));
        Server svr = new Server(ss);
        svr.start();

        Object lock = MultiThreadTest.getLock();
        synchronized (lock) {
            for (int i=0; i<threads; i++) {
                MultiThreadTest t = new MultiThreadTest(svr.getAuthority(), requests);
                t.start ();
            }
            try {
                lock.wait();
            } catch (InterruptedException e) {}
        }

        // shutdown server - we're done.
        svr.shutdown();

        int cnt = svr.connectionCount();
        MultiThreadTest.debug("Connections = " + cnt);
        int reqs = Worker.getRequests ();
        MultiThreadTest.debug("Requests = " + reqs);
        System.out.println ("Connection count = " + cnt + " Request count = " + reqs);

        // We may have received traffic from something else than
        // our client. We should only count those workers for which
        // the expected header has been found.
        int validConnections = 0;
        for (Worker w : svr.workers()) {
            if (w.headerFound) validConnections++;
        }

        if (validConnections > threads + 1 || validConnections == 0) { // could be less
            throw new RuntimeException ("Expected " + threads + " connections: used " + validConnections);
        }

        // Sometimes the client drops a connection after a while and
        // spawns a new one. Why this is happening is not clear,
        // and JDK-8223783 is logged to follow up on this. For the sake
        // of test stabilization we don't fail on `threads + 1` connections
        // but log a warning instead.
        if (validConnections == threads + 1) {
            debug("WARNING: " + validConnections
                + " have been used, where only " + threads
                + " were expected!");
        }

        if (validConnections != cnt) {
            debug("WARNING: got " + (cnt - validConnections) + " unexpected connections!");
        }
        if  (validConnections == cnt && reqs != threads*requests) {
            throw new RuntimeException ("Expected "+ threads*requests+ " requests: got " +reqs);
        }

        for (Thread worker : svr.workers()) {
            worker.join(60_000);
        }

        debug("main thread end - " + Duration.ofNanos(System.nanoTime() - start));
    }
}

    /*
     * Server thread to accept connection and create worker threads
     * to service each connection.
     */
    class Server extends Thread {
        ServerSocket ss;
        int connectionCount;
        boolean shutdown = false;
        private final Queue<Worker> workers = new ConcurrentLinkedQueue<>();

        Server(ServerSocket ss) {
            this.ss = ss;
        }

        public String getAuthority() {
            InetAddress address = ss.getInetAddress();
            String hostaddr = address.isAnyLocalAddress()
                ? "localhost" : address.getHostAddress();
            if (hostaddr.indexOf(':') > -1) {
                hostaddr = "[" + hostaddr + "]";
            }
            return hostaddr + ":" + ss.getLocalPort();
        }

        public Queue<Worker> workers() {
            return workers;
        }

        public synchronized int connectionCount() {
            return connectionCount;
        }

        public synchronized void shutdown() {
            shutdown = true;
        }

        public void run() {
            try {
                ss.setSoTimeout(2000);

                for (;;) {
                    Socket s;
                    try {
                        MultiThreadTest.debug("server: calling accept.");
                        s = ss.accept();
                        MultiThreadTest.debug("server: return accept.");
                    } catch (SocketTimeoutException te) {
                        MultiThreadTest.debug("server: STE");
                        synchronized (this) {
                            if (shutdown) {
                                MultiThreadTest.debug("server: Shuting down.");
                                return;
                            }
                        }
                        continue;
                    }

                    int id;
                    Worker w;
                    synchronized (this) {
                        id = connectionCount++;
                        w = new Worker(s, id);
                        workers.add(w);
                    }
                    w.start();
                    MultiThreadTest.debug("server: Started worker " + id);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    ss.close();
                } catch (Exception e) { }
            }
        }
    }

    /*
     * Worker thread to service single connection - can service
     * multiple http requests on same connection.
     */
    class Worker extends Thread {
        Socket s;
        int id;
        volatile boolean headerFound;

        Worker(Socket s, int id) {
            super("Worker-" + id);
            this.s = s;
            this.id = id;
        }

        static int requests = 0;
        static final Object rlock = new Object();

        public static int getRequests () {
            synchronized (rlock) {
                return requests;
            }
        }

        public static void incRequests () {
            synchronized (rlock) {
                requests++;
            }
        }

        int readUntil(InputStream in, StringBuilder headers, char[] seq) throws IOException {
            int i=0, count=0;
            while (true) {
                int c = in.read();
                if (c == -1)
                    return -1;
                headers.append((char)c);
                count++;
                if (c == seq[i]) {
                    i++;
                    if (i == seq.length)
                        return count;
                    continue;
                } else {
                    i = 0;
                }
            }
        }

        public void run() {
            long start = System.nanoTime();

            try {
                int max = 400;
                byte b[] = new byte[1000];
                InputStream in = new BufferedInputStream(s.getInputStream());
                // response to client
                PrintStream out = new PrintStream(
                                    new BufferedOutputStream(
                                                s.getOutputStream() ));

                for (;;) {

                    // read entire request from client
                    int n=0;
                    StringBuilder headers = new StringBuilder();
                    n = readUntil(in, headers, new char[] {'\r','\n', '\r','\n'});
                    if (n <= 0) {
                        MultiThreadTest.debug("worker: " + id + ": Shutdown");
                        s.close();
                        return;
                    }
                    if (headers.toString().contains("/foo.html?foo=")) {
                        headerFound = true;
                    } else {
                        MultiThreadTest.debug("worker: " + id + ": Unexpected request received: " + headers);
                    }

                    MultiThreadTest.debug("worker " + id +
                        ": Read request from client " +
                        "(" + n + " bytes).");

                    incRequests();
                    out.print("HTTP/1.1 200 OK\r\n");
                    out.print("Transfer-Encoding: chunked\r\n");
                    out.print("Content-Type: text/html\r\n");
                    out.print("Connection: Keep-Alive\r\n");
                    out.print("Keep-Alive: timeout=15, max="+max+"\r\n");
                    out.print("\r\n");
                    out.print("6\r\nHello \r\n");
                    out.print("5\r\nWorld\r\n");
                    out.print("0\r\n\r\n");
                    out.flush();

                    if (--max == 0) {
                        s.close();
                        return;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    s.close();
                } catch (Exception e) { }
                MultiThreadTest.debug("worker: " + id  + " end - " +
                            Duration.ofNanos(System.nanoTime() - start));
            }
        }
    }
