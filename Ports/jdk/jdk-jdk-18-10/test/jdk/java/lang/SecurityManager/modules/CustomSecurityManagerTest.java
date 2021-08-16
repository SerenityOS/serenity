/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.nio.file.Paths;
import java.util.List;

/*
 * @test
 * @summary Basic test of -Djava.security.manager to a class in named module.
 * @library /test/lib
 * @build jdk.test.lib.process.*
 *        m/*
 * @run testng/othervm CustomSecurityManagerTest
 */
public class CustomSecurityManagerTest {

    private static final String MODULE_PATH = Paths.get(Utils.TEST_CLASSES).resolve("modules").toString();
    private static final String POLICY_PATH = Paths.get(Utils.TEST_SRC).resolve("test.policy").toString();

    @DataProvider
    public Object[][] testCases() {
        return new Object[][]{
            new Object[] { List.of(
                    "--module-path", MODULE_PATH,
                    "--add-modules", "m",
                    "-Djava.security.manager",
                    String.format("-Djava.security.policy=%s", POLICY_PATH),
                    "RunTest"
            ) },
            new Object[] { List.of(
                    "--module-path", MODULE_PATH,
                    "--add-modules", "m",
                    "-Djava.security.manager=p.CustomSecurityManager",
                    String.format("-Djava.security.policy=%s", POLICY_PATH),
                    "RunTest"
            ) }
        };
    }

    @Test(dataProvider = "testCases")
    public void testProvider(List<String> args) throws Throwable {
        ProcessBuilder processBuilder = ProcessTools.createJavaProcessBuilder(args);
        OutputAnalyzer outputAnalyzer = ProcessTools.executeCommand(processBuilder);
        outputAnalyzer.shouldHaveExitValue(0);
    }

}

class RunTest {
    public static void main(String... args) {
        SecurityManager sm = System.getSecurityManager();
        Module module = sm.getClass().getModule();
        String s = System.getProperty("java.security.manager");
        String expected = s.isEmpty() ? "java.base" : "m";
        if (!module.isNamed() || !module.getName().equals(expected)) {
            throw new RuntimeException(module + " expected module m instead");
        }
    }
}
