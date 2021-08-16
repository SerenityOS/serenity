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
 * @bug 8153042
 * @summary Tests JDK internal APIs that have been removed.
 * @library ../lib
 * @build CompilerUtils JdepsRunner JdepsUtil ModuleMetaData
 * @modules jdk.jdeps/com.sun.tools.jdeps
 * @run testng RemovedJDKInternals
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.tools.jdeps.DepsAnalyzer;
import com.sun.tools.jdeps.Graph;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RemovedJDKInternals {
    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path CLASSES_DIR = Paths.get("classes");
    private static final Path PATCHES_DIR = Paths.get("patches");

    private static final String JDK_UNSUPPORTED = "jdk.unsupported";
    /**
     * Compiles classes used by the test
     */
    @BeforeTest
    public void compileAll() throws Exception {
        CompilerUtils.cleanDir(PATCHES_DIR);
        CompilerUtils.cleanDir(CLASSES_DIR);

        // compile sun.misc types
        Path sunMiscSrc = Paths.get(TEST_SRC, "patches", JDK_UNSUPPORTED);
        Path patchDir = PATCHES_DIR.resolve(JDK_UNSUPPORTED);
        assertTrue(CompilerUtils.compile(sunMiscSrc, patchDir,
                                         "--patch-module", JDK_UNSUPPORTED + "=" + sunMiscSrc.toString()));

        // compile com.sun.image.codec.jpeg types
        Path codecSrc = Paths.get(TEST_SRC, "patches", "java.desktop");
        Path codecDest = PATCHES_DIR;
        assertTrue(CompilerUtils.compile(codecSrc, codecDest));

        // patch jdk.unsupported and set -cp to codec types
        assertTrue(CompilerUtils.compile(Paths.get(TEST_SRC, "src", "p"),
                                         CLASSES_DIR,
                                         "--patch-module", "jdk.unsupported=" + patchDir,
                                         "-cp", codecDest.toString()));
    }

    @DataProvider(name = "deps")
    public Object[][] deps() {
        return new Object[][] {
            { "classes", new ModuleMetaData("classes", false)
                .reference("p.Main", "java.lang.Class", "java.base")
                .reference("p.Main", "java.lang.Object", "java.base")
                .reference("p.Main", "java.util.Iterator", "java.base")
                .reference("p.S", "java.lang.Object", "java.base")
                .jdkInternal("p.Main", "sun.reflect.ReflectionFactory", "jdk.unsupported")
                .removedJdkInternal("p.Main", "sun.reflect.Reflection")
                .removedJdkInternal("p.Main", "com.sun.image.codec.jpeg.JPEGCodec")
                .removedJdkInternal("p.Main", "sun.misc.Service")
                .removedJdkInternal("p.Main", "sun.misc.SoftCache")
            },
        };
    }

    @Test(dataProvider = "deps")
    public void runTest(String name, ModuleMetaData data) throws Exception {
        String cmd = String.format("jdeps -verbose:class %s%n", CLASSES_DIR);
        try (JdepsUtil.Command jdeps = JdepsUtil.newCommand(cmd)) {
            jdeps.verbose("-verbose:class")
                .addRoot(CLASSES_DIR);

            DepsAnalyzer analyzer = jdeps.getDepsAnalyzer();
            assertTrue(analyzer.run());
            jdeps.dumpOutput(System.err);

            Graph<DepsAnalyzer.Node> g = analyzer.dependenceGraph();
            // there are two node with p.Main as origin
            // one for exported API and one for removed JDK internal
            g.nodes().stream()
                .filter(u -> u.source.equals(data.moduleName))
                .forEach(u -> g.adjacentNodes(u).stream()
                    .forEach(v -> data.checkDependence(u.name, v.name, v.source, v.info)));
        }
    }

    private static final List<String> REMOVED_APIS = List.of(
            "com.sun.image.codec.jpeg.JPEGCodec",
            "sun.misc.Service",
            "sun.misc.SoftCache",
            "sun.reflect.Reflection"
    );
    private static final String REMOVED_INTERNAL_API = "JDK removed internal API";


    @Test
    public void removedInternalJDKs() throws IOException  {
        // verify the JDK removed internal API
        JdepsRunner summary = JdepsRunner.run("-summary", CLASSES_DIR.toString());
        Arrays.stream(summary.output()).map(l -> l.split(" -> "))
              .map(a -> a[1]).filter(n -> n.equals(REMOVED_INTERNAL_API))
              .findFirst().orElseThrow();

        JdepsRunner jdeps = JdepsRunner.run("-verbose:class", CLASSES_DIR.toString());
        String output = jdeps.stdout.toString();
        Map<String, String> result = findDeps(output);
        for (String cn : result.keySet()) {
            String name = result.get(cn);
            if (REMOVED_APIS.contains(cn)) {
                assertEquals(name, REMOVED_INTERNAL_API);
            } else if (cn.startsWith("sun.reflect")){
                assertEquals(name, "JDK internal API (jdk.unsupported)");
            } else {
                assertEquals(name, "java.base");
            }
        }
        REMOVED_APIS.stream().map(result::containsKey).allMatch(b -> b);
    }

    // Pattern used to parse lines
    private static final Pattern linePattern = Pattern.compile(".*\r?\n");
    private static final  Pattern pattern = Pattern.compile("\\s+ -> (\\S+) +(.*)");

    // Use the linePattern to break the given String into lines, applying
    // the pattern to each line to see if we have a match
    private static Map<String, String> findDeps(String out) {
        Map<String, String> result = new LinkedHashMap<>();
        Matcher lm = linePattern.matcher(out);  // Line matcher
        Matcher pm = null;                      // Pattern matcher
        int lines = 0;
        while (lm.find()) {
            lines++;
            CharSequence cs = lm.group();       // The current line
            if (pm == null)
                pm = pattern.matcher(cs);
            else
                pm.reset(cs);
            if (pm.find())
                result.put(pm.group(1), pm.group(2).trim());
            if (lm.end() == out.length())
                break;
        }
        return result;
    }

    private static final Map<String, String> REPLACEMENTS = Map.of(
        "com.sun.image.codec.jpeg.JPEGCodec", "Use javax.imageio @since 1.4",
        "sun.misc.Service", "Use java.util.ServiceLoader @since 1.6",
        "sun.misc.SoftCache", "Removed. See http://openjdk.java.net/jeps/260",
        "sun.reflect.Reflection", "Use java.lang.StackWalker @since 9",
        "sun.reflect.ReflectionFactory", "See http://openjdk.java.net/jeps/260"
    );

    @Test
    public void checkReplacement() {
        JdepsRunner jdeps = JdepsRunner.run("-jdkinternals", CLASSES_DIR.toString());
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
                pos = line.indexOf("Removed. ");
            if (pos < 0)
                pos = line.indexOf("See ");

            assertTrue(pos > 0);
            String name = line.substring(0, pos).trim();
            String repl = line.substring(pos, line.length()).trim();
            assertEquals(REPLACEMENTS.get(name), repl);
        }
    }
}
