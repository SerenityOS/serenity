/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test Testexecstack.java
 * @summary Searches for all libraries in test VM and checks that they
 *          have the noexecstack bit set.
 * @requires (os.family == "linux")
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   TestCheckJDK
 */

import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestCheckJDK {
    static boolean testPassed = true;
    private static final WhiteBox WB = WhiteBox.getWhiteBox();

    static void checkExecStack(Path file) {
        String filename = file.toString();
        Path parent = file.getParent();
        if (parent.endsWith("bin") || filename.endsWith(".so")) {
            if (!WB.checkLibSpecifiesNoexecstack(filename)) {
                System.out.println("Library does not have the noexecstack bit set: " + filename);
                testPassed = false;
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        String vmInstallDir = System.getProperty("java.home");

        Files.walk(Paths.get(vmInstallDir)).filter(Files::isRegularFile).forEach(TestCheckJDK::checkExecStack);

        Asserts.assertTrue(testPassed,
            "The tested VM contains libs that don't have the noexecstack " +
            "bit set. They must be linked with -z,noexecstack.");
    }
}
