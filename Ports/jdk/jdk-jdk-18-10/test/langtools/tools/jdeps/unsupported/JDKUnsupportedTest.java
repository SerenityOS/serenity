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
 * @summary jdeps should flag jdk.unsupported exported API as internal
 * @modules java.base/jdk.internal.misc
 *          jdk.jdeps/com.sun.tools.jdeps
 *          jdk.unsupported
 * @build Foo Bar
 * @run testng JDKUnsupportedTest
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertTrue;

public class JDKUnsupportedTest {
    private static final String TEST_CLASSES = System.getProperty("test.classes");
    @DataProvider(name = "data")
    public Object[][] expected() {
        return new Object[][]{
            { "Foo.class", new String[][] {
                               new String[] { "java.lang", "java.base" },
                               new String[] { "jdk.internal.misc", "JDK internal API (java.base)" }
                           } },
            { "Bar.class", new String[][] {
                               new String[] { "java.lang", "java.base" },
                               new String[] { "sun.misc", "JDK internal API (jdk.unsupported)" }
                           } }
        };
    }

    @Test(dataProvider = "data")
    public void test(String filename, String[][] expected) {
        Path path = Paths.get(TEST_CLASSES, filename);

        Map<String, String> result = jdeps(path.toString());
        for (String[] e : expected) {
            String pn = e[0];
            String module = e[1];
            assertTrue(module.equals(result.get(pn)));
        }
    }

    private static Map<String,String> jdeps(String... args) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        System.err.println("jdeps " + Arrays.toString(args));
        int rc = com.sun.tools.jdeps.Main.run(args, pw);
        pw.close();
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        if (rc != 0)
            throw new Error("jdeps failed: rc=" + rc);
        return findDeps(out);
    }

    // Pattern used to parse lines
    private static Pattern linePattern = Pattern.compile(".*\r?\n");
    private static Pattern pattern = Pattern.compile("\\s+ -> (\\S+) +(.*)");

    // Use the linePattern to break the given String into lines, applying
    // the pattern to each line to see if we have a match
    private static Map<String,String> findDeps(String out) {
        Map<String,String> result = new LinkedHashMap<>();
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
}
