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
 * @modules jdk.compiler
 * @summary Test cases which run against customized image, check the situation where
 *            1. logger provider is the default one supplied by java.base,
 *            2. clients are in named/unnamed module,
 *               patched system module, or Xbootclasspath
 *          This test does not require existence of java.logging module,
 *          but require jdk.compiler module
 * @library /test/lib
 * @build Base jdk.test.lib.compiler.CompilerUtils
 * @run main/othervm JDKLoggerForImageTest
 */

public class JDKLoggerForImageTest extends Base {

    public static void main(String args[]) throws Throwable {
        JDKLoggerForImageTest t = new JDKLoggerForImageTest();
        t.setup();
        t.test();
    }

    private void setup() throws Throwable {
        setupAllClient();

        setupJavaBaseImage();
    }

    private void test() throws Throwable {
        if (!checkJMODS()) {
            return;
        }

        // logger client is in named module m.t.a
        runTest(IMAGE,
                "--module-path", DEST_NAMED_CLIENT.toString(),
                "-m", CLIENT_A, "system", SIMPLE_LOGGER);
        // logger client is in unnamed module
        runTest(IMAGE,
                "--class-path", DEST_UNNAMED_CLIENT.toString(),
                CLIENT_B, "system", SIMPLE_LOGGER);
        // logger client gets logger through boot class BootUsage
        runTest(IMAGE,
                "-Xbootclasspath/a:" + DEST_BOOT_USAGE.toString(),
                "--class-path", DEST_BOOT_CLIENT.toString(),
                BOOT_CLIENT, "system", SIMPLE_LOGGER);
        // logger client gets logger through patched class
        // java.base/java.lang.PatchedUsage
        runTest(IMAGE,
                "--patch-module", "java.base=" + DEST_PATCHED_USAGE.toString(),
                "--class-path", DEST_PATCHED_CLIENT.toString(),
                PATCHED_CLIENT, "system", SIMPLE_LOGGER);
    }
}
