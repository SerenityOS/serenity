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

public class JimageClassPackage {
    public static void main(String args[]) throws Throwable {
        // Test Package for boot/app/ext module classes from the "modules" jimage.
        // The following classes are archived. See runtime/AppCDS/Package.java.
        //     java.util.Dictionary (testcase 0),
        //     sun.tools.javac.Main (testcase 1),
        //     jdk.nio.zipfs.ZipInfo (testcase 2),
        //     java.net.URL (testcase 3),
        //     com.sun.jndi.dns.DnsName (testcase 4)
        String testcases[][] =
            {{"Loading shared boot module class first", "java.util",
              "java.util.Dictionary", "java.util.ServiceConfigurationError"},

             {"Loading shared app module class first", "sun.tools.javac",
              "sun.tools.javac.Main", "sun.tools.javac.BatchParser"},

             {"Loading shared ext module class first", "jdk.nio.zipfs",
              "jdk.nio.zipfs.ZipInfo", "jdk.nio.zipfs.ZipPath"},

             {"Loading non-shared boot module class first", "java.net",
              "java.net.HttpCookie", "java.net.URL"},

             {"Loading non-shared ext module class first", "com.sun.jndi.dns",
              "com.sun.jndi.dns.Resolver", "com.sun.jndi.dns.DnsName"}};

        JimageClassPackage test = new JimageClassPackage();
        for (int i = 0; i < testcases.length; i++) {
            System.out.println("Testcase " + i + ": " + testcases[i][0]);
            test.testPackage(testcases[i][1], testcases[i][2], testcases[i][3]);
        }
    }

    private void testPackage (String pkg,
                              String shared,
                              String nonShared) throws Throwable {
        Class c1 = Class.forName(shared);
        ClassLoader cl = c1.getClassLoader();
        Package pkg_from_loader;
        if (cl != null) {
            pkg_from_loader = cl.getDefinedPackage(pkg);
        } else {
            pkg_from_loader = Package.getPackage(pkg);
        }

        Package pkg_from_shared_class = c1.getPackage();

        Class c2 = Class.forName(nonShared);
        Package pkg_from_nonshared_class = c2.getPackage();

        if (pkg_from_loader != null &&
            pkg_from_shared_class != null &&
            pkg_from_loader == pkg_from_shared_class &&
            pkg_from_shared_class == pkg_from_nonshared_class &&
            pkg_from_shared_class.getName().equals(pkg)) {
            System.out.println("Expected package: " + pkg_from_shared_class.toString());
        } else {
            System.out.println("Unexpected package" + pkg_from_shared_class);
            System.exit(1);
        }
        if (pkg_from_shared_class.isSealed()) {
            System.out.println("Package is sealed");
        } else {
            System.out.println("Package is not sealed");
            System.exit(1);
        }
    }
}
