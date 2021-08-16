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
 * Lookup/reverse lookup class for regression test 4773521
 * @test
 * @bug 4773521
 * @summary Test that reverse lookups of IPv4 addresses work when IPv6
 *          is enabled
 * @library /test/lib
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 *        Lookup
 * @run main Lookup root
 *
 */
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.OutputAnalyzer;

public class Lookup {
    private static final String HOST = "icann.org";
    private static final String SKIP = "SKIP";
    private static final String CLASS_PATH = System.getProperty(
            "test.class.path");

    public static void main(String args[]) throws IOException {
        String addr = null;
        String ipv4Name = null;
        String ipv4Reversed = null;

        if (args.length == 0) {
            // called from lookupWithIPv4Prefer
            // obtain an IPv4 address from the hostname.
            try {
                InetAddress ia = InetAddress.getByName(HOST);
                addr = ia.getHostAddress();
                ia = InetAddress.getByName(addr);
                System.out.print(addr + ":" + ia.getHostName());
                return;
            } catch (UnknownHostException e) {
                System.out.print(SKIP);
                return;
            }
        } else if (args.length == 2 && args[0].equals("reverse")) {
            // called from reverseWithIPv4Prefer
            // Check that IPv4 address can be resolved to host
            // with -Djava.net.preferIPv4Stack=true
            try {
                InetAddress ia = InetAddress.getByName(args[1]);
                addr = ia.getHostAddress();
                ipv4Reversed = ia.getHostName();
                System.out.print(addr + ":" + ipv4Reversed);
                return;
            } catch (UnknownHostException e) {
                System.out.print(SKIP);
                return;
            }
        } else if (args.length != 1 || !args[0].equals("root")) {
            throw new IllegalArgumentException(Stream.of(args).collect(Collectors.joining(" ")));
        }

        // spawn a subprocess to obtain the IPv4 address
        String tmp = lookupWithIPv4Prefer();
        System.out.println("IPv4 lookup results: [" + tmp + "]");
        if (SKIP.equals(tmp)) {
            System.out.println(HOST + " can't be resolved - test skipped.");
            return;
        }

        String[] strs = tmp.split(":");
        addr = strs[0];
        ipv4Name = strs[1];

        // check that the a reverse lookup of the IPv4 address
        // will succeed with the IPv4 only stack
        tmp = reverseWithIPv4Prefer(addr);
        System.out.println("IPv4 reverse lookup results: [" + tmp + "]");
        if (SKIP.equals(tmp)) {
            System.out.println(addr + " can't be resolved with preferIPv4 - test skipped.");
            return;
        }

        strs = tmp.split(":");
        ipv4Reversed = strs[1];

        // Now check that a reverse lookup will succeed with the dual stack.
        InetAddress ia = InetAddress.getByName(addr);
        String name = ia.getHostName();

        System.out.println("(default) " + addr + "--> " + name
                               + " (reversed IPv4: " + ipv4Reversed + ")");
        if (!ipv4Name.equals(name)) {
            // adding some diagnosting
            System.err.println("name=" + name + " doesn't match expected=" + ipv4Name);
            System.err.println("Listing all adresses:");
            for (InetAddress any : InetAddress.getAllByName(HOST)) {
                System.err.println("\t[" + any + "] address=" + any.getHostAddress()
                                   + ", host=" + any.getHostName());
            }
            // make the test fail...
            throw new RuntimeException("Mismatch between default"
                    + " and java.net.preferIPv4Stack=true results");
        }
    }

    static String lookupWithIPv4Prefer() throws IOException {
        String java = JDKToolFinder.getTestJDKTool("java");
        String testClz = Lookup.class.getName();
        List<String> cmd = List.of(java, "-Djava.net.preferIPv4Stack=true",
                "-cp", CLASS_PATH, testClz);
        System.out.println("Executing: " + cmd);
        return new OutputAnalyzer(new ProcessBuilder(cmd).start()).getOutput();
    }

    static String reverseWithIPv4Prefer(String addr) throws IOException {
        String java = JDKToolFinder.getTestJDKTool("java");
        String testClz = Lookup.class.getName();
        List<String> cmd = List.of(java, "-Djava.net.preferIPv4Stack=true",
                                   "-cp", CLASS_PATH, testClz, "reverse", addr);
        System.out.println("Executing: " + cmd);
        return new OutputAnalyzer(new ProcessBuilder(cmd).start()).getOutput();
    }
}
