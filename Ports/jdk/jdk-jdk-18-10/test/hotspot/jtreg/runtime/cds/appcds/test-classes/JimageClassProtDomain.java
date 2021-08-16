/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

public class JimageClassProtDomain {
    public static void main(String args[]) throws Throwable {
        // Test ProtectionDomain for boot/app/ext module classes from the "modules" jimage.
        // The following classes are archived. See runtime/AppCDS/ProtectionDomain.java.
        //     java.util.Dictionary (testcase 0),
        //     sun.tools.javac.Main (testcase 1),
        //     jdk.nio.zipfs.ZipInfo (testcase 2),
        //     java.net.URL (testcase 3),
        //     com.sun.jndi.dns.DnsName (testcase 5)
        String testcases[][] =
            {{"Loading shared boot module class first",
              "java.util.Dictionary", "java.util.ServiceConfigurationError"},

             {"Loading shared app module class first",
              "com.sun.tools.javac.Main", "com.sun.tools.javac.code.Symbol"},

             {"Loading shared ext module class first",
              "jdk.nio.zipfs.ZipInfo", "jdk.nio.zipfs.ZipPath"},

             {"Loading non-shared boot module class first",
              "java.net.HttpCookie", "java.net.URL"},

             {"Loading non-shared app module class first",
              "com.sun.tools.sjavac.Util", "com.sun.tools.sjavac.Main"},

             {"Loading non-shared ext module class first",
              "com.sun.jndi.dns.Resolver", "com.sun.jndi.dns.DnsName"}};
        for (int i = 0; i < testcases.length; i++) {
            System.out.println("Testcase " + i + ": " + testcases[i][0]);
            JimageClassProtDomain.testProtectionDomain(testcases[i][1], testcases[i][2]);
        }
    }

    private static void testProtectionDomain(String shared, String nonShared)
              throws Throwable {
        Class c1 = Class.forName(shared);
        Class c2 = Class.forName(nonShared);
        if (c1.getProtectionDomain() != c2.getProtectionDomain()) {
            System.out.println(c1.getProtectionDomain());
            System.out.println(c1.getProtectionDomain().getCodeSource());
            System.out.println(c2.getProtectionDomain());
            System.out.println(c2.getProtectionDomain().getCodeSource());
            throw new RuntimeException("Failed: Protection Domains do not match!");
        } else {
            System.out.println(c1.getProtectionDomain());
            System.out.println(c1.getProtectionDomain().getCodeSource());
            System.out.println(c2.getProtectionDomain());
            System.out.println(c2.getProtectionDomain().getCodeSource());
            System.out.println("Passed: Protection Domains match.");
        }
    }
}
