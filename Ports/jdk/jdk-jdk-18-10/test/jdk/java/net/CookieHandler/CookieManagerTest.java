/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Unit test for java.net.CookieManager
 * @bug 6244040 7150552 7051862
 * @modules jdk.httpserver
 *          java.logging
 * @run main/othervm -ea -esa CookieManagerTest
 * @author Edward Wang
 */

import com.sun.net.httpserver.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.io.IOException;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.net.Proxy.NO_PROXY;

public class CookieManagerTest {

    static CookieTransactionHandler httpTrans;
    static HttpServer server;

    static final String hostAddress = getAddr();

    /** Returns an IP literal suitable for use by the test. */
    static String getAddr() {
        try {
            InetAddress lh = InetAddress.getLocalHost();
            System.out.println("Trying: " + lh);
            if (lh.isReachable(5_000)) {
                System.out.println("Using: " + lh);
                return lh.getHostAddress();
            }
        } catch (IOException x) {
            System.out.println("Debug: caught:" + x);
        }
        InetAddress loopback = InetAddress.getLoopbackAddress();
        System.out.println("Using: \"" + loopback.getHostAddress() + "\"");
        return loopback.getHostAddress();
    }

    public static void main(String[] args) throws Exception {
        // logs everything...
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.getHandlers()[0].setLevel(Level.ALL);

        startHttpServer();
        makeHttpCall();

        if (httpTrans.badRequest) {
            throw new RuntimeException("Test failed : bad cookie header");
        }
        checkCookiePolicy();
    }

   public static void startHttpServer() throws IOException {
        httpTrans = new CookieTransactionHandler();
        server = HttpServer.create(new InetSocketAddress(hostAddress, 0), 0);
        server.createContext("/", httpTrans);
        server.start();
    }

    /*
     * Checks if CookiePolicy.ACCEPT_ORIGINAL_SERVER#shouldAccept()
     * returns false for null arguments
     */
    private static void checkCookiePolicy() throws Exception {
        CookiePolicy cp = CookiePolicy.ACCEPT_ORIGINAL_SERVER;
        boolean retVal;
        retVal = cp.shouldAccept(null, null);
        checkValue(retVal);
        retVal = cp.shouldAccept(null, new HttpCookie("CookieName", "CookieVal"));
        checkValue(retVal);
        retVal = cp.shouldAccept((new URL("http", "localhost", 2345, "/")).toURI(),
                                  null);
        checkValue(retVal);
    }

    private static void checkValue(boolean val) {
        if (val)
            throw new RuntimeException("Return value is not false!");
    }

    public static void makeHttpCall() throws IOException {
        try {
            int port = server.getAddress().getPort();
            System.out.println("http server listenining on: " + port);

            // install CookieManager to use
            CookieHandler.setDefault(new CookieManager());

            for (int i = 0; i < CookieTransactionHandler.testCount; i++) {
                System.out.println("====== CookieManager test " + (i+1)
                                    + " ======");
                ((CookieManager)CookieHandler.getDefault())
                    .setCookiePolicy(CookieTransactionHandler.testPolicies[i]);
                ((CookieManager)CookieHandler.getDefault())
                    .getCookieStore().removeAll();
                URL url = new URL("http" ,
                                  hostAddress,
                                  server.getAddress().getPort(),
                                  CookieTransactionHandler.testCases[i][0]
                                                          .serverPath);
                System.out.println("Requesting " + url);
                HttpURLConnection uc = (HttpURLConnection)url.openConnection(NO_PROXY);
                uc.getResponseCode();
                uc.disconnect();
            }
        } finally {
            server.stop(0);
        }
    }
}

class CookieTransactionHandler implements HttpHandler {

    private int testcaseDone = 0;
    private int testDone = 0;

    public static boolean badRequest = false;
    // the main test control logic will also loop exactly this number
    // to send http request
    public static final int testCount = 6;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (testDone < testCases[testcaseDone].length) {
            // still have other tests to run,
            // check the Cookie header and then redirect it
            if (testDone > 0) checkRequest(exchange.getRequestHeaders());
            exchange.getResponseHeaders().add("Location",
                    testCases[testcaseDone][testDone].serverPath);
            exchange.getResponseHeaders()
                    .add(testCases[testcaseDone][testDone].headerToken,
                         testCases[testcaseDone][testDone].cookieToSend);
            exchange.sendResponseHeaders(302, -1);
            testDone++;
        } else {
            // the last test of this test case
            if (testDone > 0) checkRequest(exchange.getRequestHeaders());
            testcaseDone++;
            testDone = 0;
            exchange.sendResponseHeaders(200, -1);
        }
        exchange.close();
    }

    private static String trim(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isWhitespace(c))
                sb.append(c);
        }
        return sb.toString();
    }

    private static boolean cookieEquals(String s1, String s2) {
        s1 = trim(s1);
        s2 = trim(s2);
        String[] s1a = s1.split(";");
        String[] s2a = s2.split(";");
        List<String> l1 = new LinkedList(List.of(s1a));
        List<String> l2 = new LinkedList(List.of(s2a));
        Collections.sort(l1);
        Collections.sort(l2);
        int i = 0;
        for (String s : l1) {
            if (!s.equals(l2.get(i++))) {
                return false;
            }
        }
        return true;
    }

    private void checkRequest(Headers hdrs) {

        assert testDone > 0;
        String cookieHeader = hdrs.getFirst("Cookie");
        if (cookieHeader != null && cookieEquals(
                cookieHeader, testCases[testcaseDone][testDone-1].cookieToRecv))
        {
            System.out.printf("%15s %s\n", "PASSED:", cookieHeader);
        } else {
            System.out.printf("%15s %s\n", "FAILED:", cookieHeader);
            System.out.printf("%15s %s\n\n", "should be:",
                    testCases[testcaseDone][testDone-1].cookieToRecv);
            badRequest = true;
        }
    }

    // test cases
    public static class CookieTestCase {
        public String headerToken;
        public String cookieToSend;
        public String cookieToRecv;
        public String serverPath;

        public CookieTestCase(String h, String cts, String ctr, String sp) {
            headerToken = h;
            cookieToSend = cts;
            cookieToRecv = ctr;
            serverPath = sp;
        }
    };

    /*
     * these two must match each other,
     * i.e. testCases.length == testPolicies.length
     */

    // the test cases to run; each test case may contain multiple roundtrips
    public static CookieTestCase[][] testCases = null;
    // indicates what CookiePolicy to use with each test cases
    public static CookiePolicy[] testPolicies = null;

    CookieTransactionHandler() {
        testCases = new CookieTestCase[testCount][];
        testPolicies = new CookiePolicy[testCount];

        String localHostAddr = CookieManagerTest.hostAddress;

        int count = 0;

        // an http session with Netscape cookies exchanged
        testPolicies[count] = CookiePolicy.ACCEPT_ORIGINAL_SERVER;
        testCases[count++] = new CookieTestCase[]{
                new CookieTestCase("Set-Cookie",
                "CUSTOMER=WILE:BOB; " +
                "path=/; expires=Sat, 09-Nov-2030 23:12:40 GMT;" + "domain=." +
                localHostAddr,
                "CUSTOMER=WILE:BOB",
                "/"
                ),
                new CookieTestCase("Set-Cookie",
                "PART_NUMBER=ROCKET_LAUNCHER_0001; path=/;" + "domain=." + localHostAddr,
                "CUSTOMER=WILE:BOB; PART_NUMBER=ROCKET_LAUNCHER_0001",
                "/"
                ),
                new CookieTestCase("Set-Cookie",
                "SHIPPING=FEDEX; path=/foo;" + "domain=." + localHostAddr,
                "CUSTOMER=WILE:BOB; PART_NUMBER=ROCKET_LAUNCHER_0001",
                "/"
                ),
                new CookieTestCase("Set-Cookie",
                "SHIPPING=FEDEX; path=/foo;" + "domain=." + localHostAddr,
                "CUSTOMER=WILE:BOB; PART_NUMBER=ROCKET_LAUNCHER_0001; SHIPPING=FEDEX",
                "/foo"
                )
                };

        // check whether or not path rule is applied
        testPolicies[count] = CookiePolicy.ACCEPT_ORIGINAL_SERVER;
        testCases[count++] = new CookieTestCase[]{
                new CookieTestCase("Set-Cookie",
                "PART_NUMBER=ROCKET_LAUNCHER_0001; path=/;" + "domain=." + localHostAddr,
                "PART_NUMBER=ROCKET_LAUNCHER_0001",
                "/"
                ),
                new CookieTestCase("Set-Cookie",
                "PART_NUMBER=RIDING_ROCKET_0023; path=/ammo;" + "domain=." + localHostAddr,
                "PART_NUMBER=RIDING_ROCKET_0023; PART_NUMBER=ROCKET_LAUNCHER_0001",
                "/ammo"
                )
                };

        // an http session with rfc2965 cookies exchanged
        testPolicies[count] = CookiePolicy.ACCEPT_ORIGINAL_SERVER;
        testCases[count++] = new CookieTestCase[]{
                new CookieTestCase("Set-Cookie2",
                "Customer=\"WILE_E_COYOTE\"; Version=\"1\"; Path=\"/acme\";" + "domain=." + localHostAddr,
                "$Version=\"1\"; Customer=\"WILE_E_COYOTE\";$Path=\"/acme\";$Domain=\"." + localHostAddr + "\"",
                "/acme/login"
                ),
                new CookieTestCase("Set-Cookie2",
                "Part_Number=\"Rocket_Launcher_0001\"; Version=\"1\";Path=\"/acme\";" + "domain=." + localHostAddr,
                "$Version=\"1\"; Customer=\"WILE_E_COYOTE\";$Path=\"/acme\";" + "$Domain=\"." +
                    localHostAddr  + "\"" + "; Part_Number=\"Rocket_Launcher_0001\";$Path=\"/acme\";"
                    + "$Domain=\"." + localHostAddr +  "\"",
                "/acme/pickitem"
                ),
                new CookieTestCase("Set-Cookie2",
                "Shipping=\"FedEx\"; Version=\"1\"; Path=\"/acme\";" + "domain=." + localHostAddr,
                "$Version=\"1\"; Customer=\"WILE_E_COYOTE\";$Path=\"/acme\";" + "$Domain=\"." + localHostAddr  +
                    "\"" + "; Part_Number=\"Rocket_Launcher_0001\";$Path=\"/acme\";" + "$Domain=\"."
                    + localHostAddr  + "\"" + "; Shipping=\"FedEx\";$Path=\"/acme\";" +
                    "$Domain=\"." + localHostAddr + "\"",
                "/acme/shipping"
                )
                };

        // check whether or not the path rule is applied
        testPolicies[count] = CookiePolicy.ACCEPT_ORIGINAL_SERVER;
        testCases[count++] = new CookieTestCase[]{
                new CookieTestCase("Set-Cookie2",
                "Part_Number=\"Rocket_Launcher_0001\"; Version=\"1\"; Path=\"/acme\";" + "domain=." + localHostAddr,
                "$Version=\"1\"; Part_Number=\"Rocket_Launcher_0001\";$Path=\"/acme\";$Domain=\"." + localHostAddr + "\"",
                "/acme/ammo"
                ),
                new CookieTestCase("Set-Cookie2",
                "Part_Number=\"Riding_Rocket_0023\"; Version=\"1\"; Path=\"/acme/ammo\";" + "domain=."
                    + localHostAddr,
                "$Version=\"1\"; Part_Number=\"Riding_Rocket_0023\";$Path=\"/acme/ammo\";$Domain=\"."
                    + localHostAddr  + "\"" + "; Part_Number=\"Rocket_Launcher_0001\";$Path=\"/acme\";"
                    + "$Domain=\"." + localHostAddr + "\"",
                "/acme/ammo"
                ),
                new CookieTestCase("",
                "",
                "$Version=\"1\"; Part_Number=\"Rocket_Launcher_0001\";$Path=\"/acme\";" + "$Domain=\"." + localHostAddr + "\"",
                "/acme/parts"
                )
                };

        // new cookie should overwrite old cookie
        testPolicies[count] = CookiePolicy.ACCEPT_ORIGINAL_SERVER;
        testCases[count++] = new CookieTestCase[]{
                new CookieTestCase("Set-Cookie2",
                "Part_Number=\"Rocket_Launcher_0001\"; Version=\"1\"; Path=\"/acme\";" + "domain=." + localHostAddr,
                "$Version=\"1\"; Part_Number=\"Rocket_Launcher_0001\";$Path=\"/acme\";$Domain=\"." + localHostAddr + "\"",
                "/acme"
                ),
                new CookieTestCase("Set-Cookie2",
                "Part_Number=\"Rocket_Launcher_2000\"; Version=\"1\"; Path=\"/acme\";" + "domain=." + localHostAddr,
                "$Version=\"1\"; Part_Number=\"Rocket_Launcher_2000\";$Path=\"/acme\";$Domain=\"." + localHostAddr + "\"",
                "/acme"
                )
                };

        // cookies without domain attributes
        // RFC 2965 states that domain should default to host
        testPolicies[count] = CookiePolicy.ACCEPT_ALL;
        testCases[count++] = new CookieTestCase[]{
                new CookieTestCase("Set-Cookie2",
                "Customer=\"WILE_E_COYOTE\"; Version=\"1\"; Path=\"/acme\"",
                "$Version=\"1\"; Customer=\"WILE_E_COYOTE\";$Path=\"/acme\";$Domain=\""+localHostAddr+"\"",
                "/acme/login"
                ),
                new CookieTestCase("Set-Cookie2",
                "Part_Number=\"Rocket_Launcher_0001\"; Version=\"1\";Path=\"/acme\"",
                "$Version=\"1\"; Customer=\"WILE_E_COYOTE\";$Path=\"/acme\";$Domain=\""+localHostAddr+"\"" +
                    "; Part_Number=\"Rocket_Launcher_0001\";$Path=\"/acme\";$Domain=\""+localHostAddr+"\"",
                "/acme/pickitem"
                ),
                new CookieTestCase("Set-Cookie2",
                "Shipping=\"FedEx\"; Version=\"1\"; Path=\"/acme\"",
                "$Version=\"1\"; Customer=\"WILE_E_COYOTE\";$Path=\"/acme\";$Domain=\""+localHostAddr+"\"" +
                    "; Part_Number=\"Rocket_Launcher_0001\";$Path=\"/acme\";$Domain=\""+localHostAddr+"\"" +
                    "; Shipping=\"FedEx\";$Path=\"/acme\";$Domain=\""+localHostAddr+"\"",
                "/acme/shipping"
                )
                };

        assert count == testCount;
    }
}
