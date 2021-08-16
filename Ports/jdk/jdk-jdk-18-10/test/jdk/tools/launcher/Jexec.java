/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8175000
 * @summary test jexec
 * @build TestHelper
 * @run main Jexec
 */

import java.io.File;
import java.io.IOException;

public class Jexec extends TestHelper {
    private final File testJar;
    private final File jexecCmd;
    private final String message = "Hello, do you read me ?";

    Jexec() throws IOException {
        jexecCmd = new File(JAVA_LIB, "jexec");
        if (!jexecCmd.exists() || !jexecCmd.canExecute()) {
            throw new Error("jexec: does not exist or not executable");
        }

        testJar = new File("test.jar");
        StringBuilder tsrc = new StringBuilder();
        tsrc.append("public static void main(String... args) {\n");
        tsrc.append("   for (String x : args) {\n");
        tsrc.append("        System.out.println(x);\n");
        tsrc.append("   }\n");
        tsrc.append("}\n");
        createJar(testJar, tsrc.toString());
    }

    public static void main(String... args) throws Exception {
        // linux is the only supported platform, give the others a pass
        if (!isLinux) {
            System.err.println("Warning: unsupported platform test passes vacuously");
            return;
        }
        // ok to run the test now
        Jexec t = new Jexec();
        t.run(null);
    }

    @Test
    void jexec() throws Exception {
        TestResult tr = doExec(jexecCmd.getAbsolutePath(),
                testJar.getAbsolutePath(), message);
        if (!tr.isOK()) {
            System.err.println(tr);
            throw new Exception("incorrect exit value");
        }
        if (!tr.contains(message)) {
            System.err.println(tr);
            throw new Exception("expected message \'" + message + "\' not found");
        }
    }
}
