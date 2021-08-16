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
 * @bug 4635698
 * @summary Check that HttpURLConnection.getResponseCode returns -1 for
 *          malformed status-lines in the http response.
 */
import java.net.*;
import java.io.*;
import static java.net.Proxy.NO_PROXY;

public class Responses {

    /*
     * Test cases :-
     *       "Response from server"     expected getResponse() and
     *                                  getResponseMessage() results
     */
    static Object[][] getTests() {
        return new Object[][] {
            { "HTTP/1.1 200 OK",        "200",  "OK" },
            { "HTTP/1.1 404 ",          "404",  null },
            { "HTTP/1.1 200",           "200",  null },
            { "HTTP/1.1",               "-1",   null },
            { "Invalid",                "-1",   null },
            { null,                     "-1" ,  null },
        };
    }

    /*
     * Simple http server used by test
     *
     * GET /<n> HTTP/1.x results in http response with the status line
     * set to geTests()[<n>][0]  -- eg: GET /2 results in a response of
     * "HTTP/1.1 404 "
     */
    static class HttpServer implements Runnable {
        final ServerSocket ss;
        volatile boolean shutdown;

        public HttpServer() {
            try {
                InetAddress loopback = InetAddress.getLoopbackAddress();
                ss = new ServerSocket();
                ss.bind(new InetSocketAddress(loopback, 0));
            } catch (IOException ioe) {
                throw new Error("Unable to create ServerSocket: " + ioe);
            }
        }

        public int port() {
            return ss.getLocalPort();
        }

        public String authority() {
            InetAddress address = ss.getInetAddress();
            String hostaddr = address.isAnyLocalAddress()
                ? "localhost" : address.getHostAddress();
            if (hostaddr.indexOf(':') > -1) {
                hostaddr = "[" + hostaddr + "]";
            }
            return hostaddr + ":" + port();
        }

        public void shutdown() throws IOException {
            shutdown = true;
            ss.close();
        }

        public void run() {
            Object[][] tests = getTests();

            try {
                while(!shutdown) {
                    Socket s = ss.accept();

                    BufferedReader in = new BufferedReader(
                                              new InputStreamReader(
                                                s.getInputStream()));
                    String req = in.readLine();
                    int pos1 = req.indexOf(' ');
                    int pos2 = req.indexOf(' ', pos1+1);

                    int i = Integer.parseInt(req.substring(pos1+2, pos2));
                    System.out.println("Server replying to >" + tests[i][0] + "<");

                    PrintStream out = new PrintStream(
                                        new BufferedOutputStream(
                                          s.getOutputStream() ));

                    out.print( tests[i][0] );
                    out.print("\r\n");
                    out.print("Content-Length: 0\r\n");
                    out.print("Connection: close\r\n");
                    out.print("\r\n");
                    out.flush();

                    s.shutdownOutput();
                    s.close();
                }
            } catch (Exception e) {
                if (!shutdown) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static void main(String args[]) throws Exception {

        /* start the http server */
        HttpServer svr = new HttpServer();
        (new Thread(svr)).start();

        String authority = svr.authority();
        System.out.println("Server listening on: " + authority);

        /*
         * Iterate through each test case and check that getResponseCode
         * returns the expected result.
         */
        int failures = 0;
        Object tests[][] = getTests();
        for (int i=0; i<tests.length; i++) {

            System.out.println("******************");
            System.out.println("Test with response: >" + tests[i][0] + "<");

            URL url = new URL("http://" + authority + "/" + i);
            HttpURLConnection http = (HttpURLConnection)url.openConnection(NO_PROXY);

            try {

                // test getResponseCode
                //
                int expectedCode = Integer.parseInt((String)tests[i][1]);
                int actualCode = http.getResponseCode();
                if (actualCode != expectedCode) {
                    System.out.println("getResponseCode returned: " + actualCode +
                        ", expected: " + expectedCode);
                    failures++;
                    continue;
                }

                // test getResponseMessage
                //
                String expectedPhrase = (String)tests[i][2];
                String actualPhrase = http.getResponseMessage();
                if (actualPhrase == null && expectedPhrase == null) {
                    continue;
                }
                if (!actualPhrase.equals(expectedPhrase)) {
                    System.out.println("getResponseMessage returned: " +
                        actualPhrase + ", expected: " + expectedPhrase);
                }
            } catch (IOException e) {
                System.err.println("Test failed for >" + tests[i][0] + "<: " + e);
                e.printStackTrace();
                failures++;
            }
        }

        /* shutdown http server */
        svr.shutdown();

        if (failures > 0) {
            throw new Exception(failures + " sub-test(s) failed.");
        }
    }
}
