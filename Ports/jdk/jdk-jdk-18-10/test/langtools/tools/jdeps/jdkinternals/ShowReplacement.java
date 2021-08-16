/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8159524
 * @summary Tests JDK internal APIs with and without replacements
 * @library ../lib
 * @modules jdk.jdeps/com.sun.tools.jdeps
 * @build CompilerUtils JdepsUtil
 * @run testng ShowReplacement
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ShowReplacement {
    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path CLASSES_DIR = Paths.get("classes");

    private static final Map<String, String> REPLACEMENTS = Map.of(
        "sun.security.util.HostnameChecker",
        "Use javax.net.ssl.SSLParameters.setEndpointIdentificationAlgorithm(\"HTTPS\") @since 1.7",
        "",
        "or javax.net.ssl.HttpsURLConnection.setHostnameVerifier() @since 1.4");

    /**
     * Compiles classes used by the test
     */
    @BeforeTest
    public void compileAll() throws Exception {
        CompilerUtils.cleanDir(CLASSES_DIR);

        Path tmp = Paths.get("tmp");
        assertTrue(CompilerUtils.compile(Paths.get(TEST_SRC, "src", "apple"), tmp));
        assertTrue(CompilerUtils.compile(Paths.get(TEST_SRC, "src", "q"),
                                         CLASSES_DIR,
                                         "-cp", tmp.toString(),
                                         "--add-exports=java.base/sun.security.util=ALL-UNNAMED"));
    }

    @Test
    public void withReplacement() {
        Path file = Paths.get("q", "WithRepl.class");
        JdepsRunner jdeps = JdepsRunner.run("-jdkinternals", CLASSES_DIR.resolve(file).toString());
        String[] output = jdeps.output();
        int i = 0;
        while (!output[i].contains("Suggested Replacement")) {
            i++;
        }

        // must match the number of JDK internal APIs
        int count = output.length-i-2;
        assertEquals(count, REPLACEMENTS.size());

        for (int j=i+2; j < output.length; j++) {
            String line = output[j];
            int pos = line.indexOf("Use ");
            if (pos < 0)
                pos = line.indexOf("or");

            assertTrue(pos > 0);
            String name = line.substring(0, pos).trim();
            String repl = line.substring(pos, line.length()).trim();
            assertEquals(REPLACEMENTS.get(name), repl);
        }
    }

    /*
     * A JDK internal class has been removed while its package still exists.
     */
    @Test
    public void noReplacement() {
        Path file = Paths.get("q", "NoRepl.class");
        JdepsRunner jdeps = JdepsRunner.run("-jdkinternals", CLASSES_DIR.resolve(file).toString());
        String[] output = jdeps.output();
        int i = 0;
        // expect no replacement
        while (i < output.length && !output[i].contains("Suggested Replacement")) {
            i++;
        }

        // no replacement
        assertEquals(output.length-i, 0);
    }

    /*
     * A JDK internal package has been removed.
     */
    @Test
    public void removedPackage() {
        Path file = Paths.get("q", "RemovedPackage.class");
        JdepsRunner jdeps = JdepsRunner.run("--jdk-internals", CLASSES_DIR.resolve(file).toString());
        String[] output = jdeps.output();
        int i = 0;
        // expect no replacement
        while (i < output.length && !output[i].contains("Suggested Replacement")) {
            i++;
        }

        // no replacement
        assertEquals(output.length-i, 0);
    }
}
