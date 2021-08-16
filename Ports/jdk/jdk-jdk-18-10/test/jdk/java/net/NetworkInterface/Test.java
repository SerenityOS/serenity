/*
 * Copyright (c) 2001, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4405354 6594296 8058216
 * @library /test/lib
 * @run main/othervm -Xcheck:jni Test
 * @run main/othervm -Xcheck:jni -Djava.net.preferIPv4Stack=true Test
 * @summary Basic tests for NetworkInterface
 */
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.util.Enumeration;
import jdk.test.lib.net.IPSupport;

public class Test {
    static final boolean isWindows = System.getProperty("os.name").startsWith("Windows");

    public static void main(String args[]) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        Enumeration nifs = NetworkInterface.getNetworkInterfaces();

        while (nifs.hasMoreElements()) {
            NetworkInterface ni = (NetworkInterface)nifs.nextElement();

            //JDK-8038276: Should not test on Windows with Teredo Tunneling Pseudo-Interface
            String dName = ni.getDisplayName();
            if (isWindows && dName != null && dName.contains("Teredo"))
                continue;

            String name = ni.getName();
            System.out.println("\n" + name);

            /*
             * Enumeration the IP addresses on this interface
             */
            Enumeration addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress addr = (InetAddress)addrs.nextElement();
                System.out.println(addr);
                if (NetworkInterface.getByInetAddress(addr) == null) {
                    throw new Exception("getByInetAddress returned null");
                }
            }
            System.out.println("getInetAddresses() test passed.");

            /*
             * Check equals and hashCode contract
             */
            NetworkInterface ni2 = NetworkInterface.getByName(name);
            if (!ni2.equals(ni)) {
                throw new Exception("getByName returned: " + ni2);
            }
            if (!ni.equals(ni2)) {
                throw new Exception("equals specification broken");
            }
            System.out.println("equals() tests passed.");
            if (ni2.hashCode() != ni.hashCode()) {
                throw new Exception("hashCode contract broken");
            }
            System.out.println("hashCode() test passed.");

            byte[] ba = ni.getHardwareAddress();
            if (ba != null && ba.length == 0) {
                throw new Exception("getHardwareAddress returned 0 length byte array");
            }
            System.out.println("getHardwareAddress() test passed.");
        }

        // misc tests :-
        //      getByXXX(null) should throw NPE
        //      getByXXX("garbage") should return null

        System.out.println("\nMiscellenous tests: ");

        try {
            NetworkInterface.getByName(null);
        } catch (NullPointerException npe) {
        }
        System.out.println("getByName(null) test passed.");

        try {
            NetworkInterface.getByInetAddress(null);
        } catch (NullPointerException npe) {
        }
        System.out.println("getByInetAddress(null) test passed.");

        if (NetworkInterface.getByName("not-a-valid-name") != null) {
            throw new Exception
                ("getByName returned unexpected interface: null expected");
        }
        System.out.println("getByName(<unknown>) test passed.");

        InetAddress ia = InetAddress.getByName("255.255.255.255");
        if (NetworkInterface.getByInetAddress(ia) != null) {
            throw new Exception
                ("getByInetAddress returned unexpected interface: null expected");
        }
        System.out.println("getByName(getByInetAddress(<unknown>) test passed.");

    }
}
