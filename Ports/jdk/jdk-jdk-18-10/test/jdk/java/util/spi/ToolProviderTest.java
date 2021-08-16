/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8159855
 * @summary test ToolProvider SPI
 * @run main/othervm -Djava.security.manager=allow ToolProviderTest
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.spi.ToolProvider;

public class ToolProviderTest {
    public static void main(String... args) throws Exception {
        ToolProviderTest t = new ToolProviderTest();
        t.run();
    }

    void run() throws Exception {
        initServices();

        System.out.println("Validate an NPE is thrown with null arguments");

        testNullArgs();

        System.out.println("test without security manager present:");
        test();

        System.setSecurityManager(new SecurityManager());

        System.out.println("test with security manager present:");
        test();
    }

    private void test() throws Exception {
        ToolProvider testProvider = ToolProvider.findFirst("test").get();
        int rc = testProvider.run(System.out, System.err, "hello test");
        if (rc != 0) {
            throw new Exception("unexpected exit code: " + rc);
        }
    }

    private void testNullArgs() {
        ToolProvider testProvider = ToolProvider.findFirst("test").get();

        // out null check
        expectNullPointerException(() -> testProvider.run(null, System.err));

        // err null check
        expectNullPointerException(() -> testProvider.run(System.out, null));

        // args array null check
        expectNullPointerException(() ->
                testProvider.run(System.out, System.err, (String[]) null));

        // args array elements null check
        expectNullPointerException(() ->
                testProvider.run(System.out, System.err, (String) null));
    }

    private static void expectNullPointerException(Runnable test) {
        try {
            test.run();
            throw new Error("NullPointerException not thrown");
        } catch (NullPointerException e) {
            // expected
        }
    }

    private void initServices() throws IOException {
        Path testClasses = Paths.get(System.getProperty("test.classes"));
        Path services = testClasses.resolve(Paths.get("META-INF", "services"));
        Files.createDirectories(services);
        Files.write(services.resolve(ToolProvider.class.getName()),
                Arrays.asList(TestProvider.class.getName()));
    }

    public static class TestProvider implements ToolProvider {
        public TestProvider() {
            checkPrivileges();
        }

        public String name() {
            return "test";
        }

        public int run(PrintWriter out, PrintWriter err, String... args) {
            out.println("Test: " + Arrays.toString(args));
            return 0;
        }

        private void checkPrivileges() {
            boolean haveSecurityManager = (System.getSecurityManager() != null);
            try {
                // validate appropriate privileges by checking access to a
                // system property
                System.getProperty("java.home");
                if (haveSecurityManager) {
                    throw new Error("exception exception not thrown");
                }
            } catch (SecurityException e) {
                if (!haveSecurityManager) {
                    throw new Error("unexpected exception: " + e);
                }
            }
        }
    }
}

