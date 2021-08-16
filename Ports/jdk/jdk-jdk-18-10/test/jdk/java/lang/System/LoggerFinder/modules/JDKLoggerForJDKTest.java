/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.logging
 * @modules jdk.compiler
 * @summary Test cases which run against the JDK image, check the situation where
 *            1. logger provider is the default one supplied by the JDK,
 *            2. clients are in named/unnamed module,
 *               patched system module, or Xbootclasspath
 *          This test DOES require existence of java.logging module
 * @library /test/lib
 * @build Base jdk.test.lib.compiler.CompilerUtils
 * @run main/othervm JDKLoggerForJDKTest
 */

public class JDKLoggerForJDKTest extends Base {

    public static void main(String args[]) throws Throwable {
        JDKLoggerForJDKTest t = new JDKLoggerForJDKTest();
        t.setup();
        t.test();
    }

    private void setup() throws Throwable {
        setupAllClient();
    }

    private void test() throws Throwable {
        // logger client is in named module m.t.a
        runTest(JDK_IMAGE,
                "--module-path", DEST_NAMED_CLIENT.toString(),
                "-m", CLIENT_A, "system", JUL_LOGGER);
        // logger client is in unnamed module
        runTest(JDK_IMAGE,
                "--class-path", DEST_UNNAMED_CLIENT.toString(),
                CLIENT_B, "system", JUL_LOGGER);
        // logger client gets logger through boot class BootUsage
        runTest(JDK_IMAGE,
                "-Xbootclasspath/a:" + DEST_BOOT_USAGE.toString(),
                "--class-path", DEST_BOOT_CLIENT.toString(),
                BOOT_CLIENT, "system", LAZY_LOGGER, JUL_LOGGER);
        // logger client gets logger through patched class
        // java.base/java.lang.PatchedUsage
        runTest(JDK_IMAGE,
                "--patch-module", "java.base=" + DEST_PATCHED_USAGE.toString(),
                "--class-path", DEST_PATCHED_CLIENT.toString(),
                PATCHED_CLIENT, "system", LAZY_LOGGER, JUL_LOGGER);
    }
}
