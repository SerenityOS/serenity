/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.security.auth.login.LoginException;
import sun.security.krb5.Asn1Exception;
import sun.security.krb5.Config;

/*
 * @test
 * @bug 8164656 8181461 8194486
 * @summary krb5.kdc.bad.policy test
 * @library /test/lib
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts KdcPolicy udp
 * @run main/othervm -Djdk.net.hosts.file=TestHosts KdcPolicy tcp
 */
public class KdcPolicy {

    // Is this test on UDP?
    static boolean udp;

    public static void main(String[] args) throws Exception {

        udp = args[0].equals("udp");

        try {
            main0();
        } catch (LoginException le) {
            Throwable cause = le.getCause();
            if (cause instanceof Asn1Exception) {
                System.out.println("Another process sends a packet to " +
                        "this server. Ignored.");
                return;
            }
            throw le;
        }
    }

    static DebugMatcher cm = new DebugMatcher();

    static void main0() throws Exception {

        System.setProperty("sun.security.krb5.debug", "true");

        // One real KDC. Must be created before fake KDCs
        // to read the TestHosts file.
        OneKDC kdc = new OneKDC(null);

        // Two fake KDCs, d1 and d2 only listen but do not respond.

        if (udp) {
            try (DatagramSocket d1 = new DatagramSocket();
                 DatagramSocket d2 = new DatagramSocket()) {
                run(d1.getLocalPort(), d2.getLocalPort(), kdc.getPort());
            }
        } else {
            try (ServerSocket d1 = new ServerSocket(0);
                 ServerSocket d2 = new ServerSocket(0)) {
                run(d1.getLocalPort(), d2.getLocalPort(), kdc.getPort());
            }
        }
    }

    static void run(int p1, int p2, int p3) throws Exception {

        // cm.kdc() will return a and b for fake KDCs, and c for real KDC.
        cm.addPort(-1).addPort(p1).addPort(p2).addPort(p3);

        System.setProperty("java.security.krb5.conf", "alternative-krb5.conf");

        // Check default timeout is 30s. Use real KDC only, otherwise too
        // slow to wait for timeout. Each request (without preauth and with
        // preauth) might be retried 3 times, and could fail if one fails for
        // all 3 times.
        writeConf(-1, -1, p3);
        test("(c30000){2,6}|(c30000){3,6}-");

        // 1. Default policy is tryLast
        //Security.setProperty("krb5.kdc.bad.policy", "tryLast");

        // Need a real KDC, otherwise there is no last good.
        // This test waste 3 seconds waiting for d1 to timeout.
        // It is possible the real KDC cannot fulfil the request
        // in 3s, so it might fail (either 1st time or 2nd time).
        writeConf(1, 3000, p1, p3);
        test("a3000c3000c3000|a3000c3000-|a3000c3000c3000a3000-");

        // If a test case won't use a real KDC, it can be sped up.
        writeConf(3, 5, p1, p2);
        test("a5a5a5b5b5b5-");  // default max_retries == 3
        test("a5a5a5b5b5b5-");  // all bad means no bad

        // 2. No policy.
        Security.setProperty("krb5.kdc.bad.policy", "");
        Config.refresh();

        // This case needs a real KDC, otherwise, all bad means no
        // bad and we cannot tell the difference. This case waste 3
        // seconds on d1 to timeout twice. It is possible the real KDC
        // cannot fulfil the request within 3s, so it might fail
        // (either 1st time or 2nd time).
        writeConf(1, 3000, p1, p3);
        test("a3000c3000a3000c3000|a3000c3000-|a3000c3000a3000c3000-");

        // 3. tryLess with no argument means tryLess:1,5000
        Security.setProperty("krb5.kdc.bad.policy", "tryLess");

        // This case will waste 11s. We are checking that the default
        // value of 5000 in tryLess is only used if it's less than timeout
        // in krb5.conf
        writeConf(1, 6000, p1);
        test("a6000-"); // timeout in krb5.conf is 6s
        test("a5000-"); // tryLess to 5s. This line can be made faster if
                        // d1 is a read KDC, but we have no existing method
                        // to start KDC on an existing ServerSocket (port).

        writeConf(-1, 4, p1, p2);
        test("a4a4a4b4b4b4-");  // default max_retries == 3
        test("a4b4-");          // tryLess to 1. And since 4 < 5000, use 4.
        Config.refresh();
        test("a4a4a4b4b4b4-");

        writeConf(5, 4, p1, p2);
        test("a4a4a4a4a4b4b4b4b4b4-"); // user-provided max_retries == 5
        test("a4b4-");
        Config.refresh();
        test("a4a4a4a4a4b4b4b4b4b4-");

        // 3. tryLess with arguments
        Security.setProperty("krb5.kdc.bad.policy",
                "tryLess:2,5");

        writeConf(-1, 6, p1, p2);
        test("a6a6a6b6b6b6-");  // default max_retries == 3
        test("a5a5b5b5-");      // tryLess to 2
        Config.refresh();
        test("a6a6a6b6b6b6-");

        writeConf(5, 4, p1, p2);
        test("a4a4a4a4a4b4b4b4b4b4-");  // user-provided max_retries == 5
        test("a4a4b4b4-");              // tryLess to 2
        Config.refresh();
        test("a4a4a4a4a4b4b4b4b4b4-");
    }

    /**
     * Writes a krb5.conf file.
     * @param max max_retries, -1 if not set
     * @param to kdc_timeout, -1 if not set
     * @param ports where KDCs listen on
     */
    static void writeConf(int max, int to, int... ports) throws Exception {

        // content of krb5.conf
        String conf = "";

        // Extra settings in [libdefaults]
        String inDefaults = "";

        // Extra settings in [realms]
        String inRealm = "";

        // We will randomly put extra settings only in [libdefaults],
        // or in [realms] but with different values in [libdefaults],
        // to prove that settings in [realms] override those in [libdefaults].
        Random r = new Random();

        if (max > 0) {
            if (r.nextBoolean()) {
                inDefaults += "max_retries = " + max + "\n";
            } else {
                inRealm += "   max_retries = " + max + "\n";
                inDefaults += "max_retries = " + (max + 1) + "\n";
            }
        }

        if (to > 0) {
            if (r.nextBoolean()) {
                inDefaults += "kdc_timeout = " + to + "\n";
            } else {
                inRealm += "   kdc_timeout = " + to + "\n";
                inDefaults += "kdc_timeout = " + (to + 1) + "\n";
            }
        }

        if (udp) {
            if (r.nextBoolean()) {
                inDefaults += "udp_preference_limit = 10000\n";
            } else if (r.nextBoolean()) {
                inRealm += "   udp_preference_limit = 10000\n";
                inDefaults += "udp_preference_limit = 1\n";
            } // else no settings means UDP
        } else {
            if (r.nextBoolean()) {
                inDefaults += "udp_preference_limit = 1\n";
            } else {
                inRealm += "   udp_preference_limit = 1\n";
                inDefaults += "udp_preference_limit = 10000\n";
            }
        }

        conf = "[libdefaults]\n" +
                "default_realm = " + OneKDC.REALM + "\n" +
                inDefaults +
                "\n" +
                "[realms]\n" +
                OneKDC.REALM + " = {\n";

        for (int port : ports) {
            conf += "   kdc = " + OneKDC.KDCHOST + ":" + port + "\n" +
                    inRealm;
        }

        conf += "}\n";

        Files.write(Paths.get("alternative-krb5.conf"), conf.getBytes());
        Config.refresh();
    }

    /**
     * One call of krb5 login. As long as the result matches one of expected,
     * the test is considered as success. The grammar of expected is
     *
     *    kdc#, timeout, kdc#, timeout, ..., optional "-" for failure
     */
    static void test(String... expected) throws Exception {

        System.out.println("------------------TEST----------------------");
        PrintStream oldOut = System.out;
        boolean failed = false;
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bo));
        try {
            Context.fromUserPass(OneKDC.USER, OneKDC.PASS, false);
        } catch (Exception e) {
            failed = true;
        } finally {
            System.setOut(oldOut);
        }

        String[] lines = new String(bo.toByteArray()).split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line: lines) {
            if (cm.match(line)) {
                if (udp != cm.isUDP()) {
                    sb.append("x");
                }
                sb.append(cm.kdc()).append(cm.timeout());
            }
        }
        if (failed) sb.append('-');

        String output = sb.toString();

        boolean found = false;
        for (String ex : expected) {
            if (output.matches(ex)) {
                System.out.println("Expected: " + ex + ", actual " + output);
                found = true;
                break;
            }
        }

        if (!found) {
            System.out.println("--------------- ERROR START -------------");
            System.out.println(new String(bo.toByteArray()));
            System.out.println("--------------- ERROR END ---------------");
            throw new Exception("Does not match. Output is " + output);
        }
    }

    /**
     * A helper class to match the krb5 debug output:
     * >>> KDCCommunication: kdc=host UDP:11555, timeout=200,Attempt =1, #bytes=138
     *
     * Example:
     *  DebugMatcher cm = new DebugMatcher();
     *  cm.addPort(12345).addPort(11555);
     *  for (String line : debugOutput) {
     *      if (cm.match(line)) {
     *          System.out.printf("%c%d\n", cm.kdc(), cm.timeout());
     *          // shows b200 for the example above
     *      }
     *  }
     */
    static class DebugMatcher {

        static final Pattern re = Pattern.compile(
                ">>> KDCCommunication: kdc=\\S+ (TCP|UDP):(\\d+), " +
                        "timeout=(\\d+),Attempt\\s*=(\\d+)");

        List<Integer> kdcPorts = new ArrayList<>();
        Matcher matcher;

        /**
         * Add KDC ports one by one. See {@link #kdc()}.
         */
        DebugMatcher addPort(int port) {
            if (port > 0) {
                kdcPorts.add(port);
            } else {
                kdcPorts.clear();
            }
            return this;
        }

        /**
         * When a line matches the ">>> KDCCommunication:" pattern. After a
         * match, the getters below can be called on this match.
         */
        boolean match(String line) {
            matcher = re.matcher(line);
            return matcher.find();
        }

        /**
         * Protocol of this match, "UDP" or "TCP".
         */
        boolean isUDP() {
            return matcher.group(1).equals("UDP");
        }

        /**
         * KDC for this match, "a" for the one 1st added bt addPort(), "b"
         * for second, etc. Undefined for not added.
         */
        char kdc() {
            int port = Integer.parseInt(matcher.group(2));
            return (char) (kdcPorts.indexOf(port) + 'a');
        }

        /**
         * Timeout value for this match.
         */
        int timeout() {
            return Integer.parseInt(matcher.group(3));
        }
    }
}
