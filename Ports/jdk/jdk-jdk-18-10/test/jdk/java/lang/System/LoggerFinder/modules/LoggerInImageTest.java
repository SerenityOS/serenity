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
 *            1. logger providers are in the customized image too,
 *            2. clients are in named/unnamed module, image,
 *               patched system module, or Xbootclasspath
 *          This test does not require existence of java.logging module,
 *          but require jdk.compiler module
 * @library /test/lib
 * @build Base jdk.test.lib.compiler.CompilerUtils
 * @run main/othervm LoggerInImageTest
 */

public class LoggerInImageTest extends Base {

    public static void main(String args[]) throws Throwable {
        LoggerInImageTest t = new LoggerInImageTest();
        t.setup();
        t.test();
    }

    private void setup() throws Throwable {
        setupAllClient();

        setupNamedLogger();

        setupLoggerImage();
        setupFullImage();
    }

    private void test() throws Throwable {
        if (!checkJMODS()) {
            return;
        }

        // logger client is in named module m.t.a which is also in customized image
        runTest(IMAGE_CLIENT_LOGGER,
                "-m", CLIENT_A, "named", LOGGER_A);
        // logger client in named module m.t.a
        runTest(IMAGE_LOGGER,
                "--module-path", DEST_NAMED_CLIENT.toString(),
                "-m", CLIENT_A, "named", LOGGER_A);
        // logger client is in unnamed module
        runTest(IMAGE_LOGGER,
                "--class-path", DEST_UNNAMED_CLIENT.toString(),
                CLIENT_B, "named", LOGGER_A);
        // logger client gets logger through boot class BootUsage
        runTest(IMAGE_LOGGER,
                "-Xbootclasspath/a:" + DEST_BOOT_USAGE.toString(),
                "--class-path", DEST_BOOT_CLIENT.toString(),
                BOOT_CLIENT, "system", LAZY_LOGGER, LOGGER_A);
        // logger client gets logger through patched class
        // java.base/java.lang.PatchedUsage
        runTest(IMAGE_LOGGER,
                "--patch-module", "java.base=" + DEST_PATCHED_USAGE.toString(),
                "--class-path", DEST_PATCHED_CLIENT.toString(),
                PATCHED_CLIENT, "system", LAZY_LOGGER, LOGGER_A);
    }
}
