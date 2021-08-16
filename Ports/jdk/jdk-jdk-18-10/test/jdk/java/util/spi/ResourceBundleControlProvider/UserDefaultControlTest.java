/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6959653 8172365
 * @summary Test ResourceBundle.Control provided using SPI.
 * @library test
 * @build test/*
 * @build com.foo.UserControlProvider
 * @run main/othervm UserDefaultControlTest false
 * @run main/othervm -Djava.security.manager=allow UserDefaultControlTest true
 */

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

import jdk.test.*;

public class UserDefaultControlTest {
    public static void main(String... args) throws Exception {
        boolean smExists = Boolean.valueOf(args[0]);
        initServices();
        if (smExists) {
            System.out.println("test with security manager present:");
            System.setSecurityManager(new SecurityManager());
        } else {
            System.out.println("test without security manager present:");
        }

        test(smExists);
    }

    private static void initServices() throws IOException {
        Path testClasses = Paths.get(System.getProperty("test.classes"));
        Path services = testClasses.resolve(Paths.get("META-INF", "services"));
        Files.createDirectories(services);
        Files.write(services.resolve("java.util.spi.ResourceBundleControlProvider"),
                List.of("com.foo.UserControlProvider"));
        Path comfoo = testClasses.resolve(Paths.get("com", "foo"));
        Path testSrcComFoo =
            Paths.get(System.getProperty("test.src")).resolve(Paths.get("com", "foo"));
        Files.copy(testSrcComFoo.resolve("XmlRB.xml"), comfoo.resolve("XmlRB.xml"),
            StandardCopyOption.REPLACE_EXISTING);
        Files.copy(testSrcComFoo.resolve("XmlRB_ja.xml"), comfoo.resolve("XmlRB_ja.xml"),
            StandardCopyOption.REPLACE_EXISTING);
    }

    private static void test(boolean smExists) {
        ResourceBundle rb;

        try {
            rb = ResourceBundle.getBundle("com.foo.XmlRB", Locale.ROOT);
            if (smExists) {
                throw new RuntimeException("getBundle did not throw " +
                    "MissingResourceException with a security manager");
            }
        } catch (MissingResourceException e) {
            if (smExists) {
                // failed successfully
                return;
            } else {
                throw e;
            }
        }

        String type = rb.getString("type");
        if (!type.equals("XML")) {
            throw new RuntimeException("Root Locale: type: got " + type
                                       + ", expected XML (ASCII)");
        }

        rb = ResourceBundle.getBundle("com.foo.XmlRB", Locale.JAPAN);
        type = rb.getString("type");
        // Expect fullwidth "XML"
        if (!type.equals("\uff38\uff2d\uff2c")) {
            throw new RuntimeException("Locale.JAPAN: type: got " + type
                                       + ", expected \uff38\uff2d\uff2c (fullwidth XML)");
        }

        try {
            rb = ResourceBundle.getBundle("com.bar.XmlRB", Locale.JAPAN);
            throw new RuntimeException("com.bar.XmlRB test failed.");
        } catch (MissingResourceException e) {
            // OK
        }

        // tests with named module. Only resource bundles on the classpath
        // should be available, unless an unnamed module is explicitly
        // specified.
        rb = ResourceBundleDelegate.getBundle("simple", Locale.ROOT);
        try {
            rb = ResourceBundleDelegate.getBundle("com.foo.XmlRB", Locale.ROOT);
            throw new RuntimeException("getBundle in a named module incorrectly loaded " +
                                    "a resouce bundle through RBControlProvider");
        } catch (MissingResourceException e) {
            // OK
        }

        Module unnamedModule = UserDefaultControlTest.class
                                .getClassLoader()
                                .getUnnamedModule();
        rb = ResourceBundleDelegate.getBundle("com.foo.XmlRB", Locale.JAPAN, unnamedModule);
        type = rb.getString("type");
        // Expect fullwidth "XML"
        if (!type.equals("\uff38\uff2d\uff2c")) {
            throw new RuntimeException("getBundle called from named module for unnamed module."
                                       + " Locale.JAPAN: type: got " + type
                                       + ", expected \uff38\uff2d\uff2c (fullwidth XML)");
        }
    }
}
