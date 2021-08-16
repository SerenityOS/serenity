/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8263043
 * @summary Add test to verify order of tag output
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.ToolBox javadoc.tester.*
 * @run main TestTagOrder
 */

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

/**
 * Tests the order of the output of block tags in the generated output.
 * There is a default order, embodied in the order of declaration of tags in
 * {@code TagletManager}, but this can be overridden on the command line by
 * specifying {@code -tag} options in the desired order.
 */
public class TestTagOrder extends JavadocTester {
    public static void main(String... args) throws Exception {
        TestTagOrder tester = new TestTagOrder();
        tester.runTests(m -> new Object[] { Path.of(m.getName()) });
    }

    ToolBox tb = new ToolBox();
    Path src = Path.of("src");
    Map<String, String> expectMethod = new LinkedHashMap<>();
    Map<String, String> expectClass = new LinkedHashMap<>();

    TestTagOrder() throws IOException {
        tb.writeJavaFiles(src,
                """
                    package p;
                    /** Class C1. */
                    public class C1 {
                        /**
                         * This is method m.
                         * @param p1 first parameter
                         * @param p2 second parameter
                         * @return zero
                         * @throws IllegalArgumentException well, never
                         * @since 1.0
                         * @see <a href="http://example.com">example</a>
                         */
                        public int m(int p1, int p2) throws IllegalArgumentException {
                            return 0;
                        }
                    }
                    """, """
                    package p;
                    /**
                     * Class C2.
                     * @since 1.0
                     * @author J. Duke.
                     * @version 2.0
                     * @see <a href="http://example.com">example</a>
                     */
                    public class C2 { }
                    """);

        // The following add map entries in the default order of appearance in the output.
        // Note that the list is not otherwise ordered, such as alphabetically.

        expectMethod.put("@param", """
                <dt>Parameters:</dt>
                <dd><code>p1</code> - first parameter</dd>
                <dd><code>p2</code> - second parameter</dd>
                """);

        expectMethod.put("@return", """
                <dt>Returns:</dt>
                <dd>zero</dd>
                """);

        expectMethod.put("@throws", """
                <dt>Throws:</dt>
                <dd><code>java.lang.IllegalArgumentException</code> - well, never</dd>
                """);

        expectMethod.put("@since", """
                <dt>Since:</dt>
                <dd>1.0</dd>
                """);

        expectMethod.put("@see", """
                <dt>See Also:</dt>
                <dd>
                <ul class="see-list">
                <li><a href="http://example.com">example</a></li>
                </ul>
                </dd>
                """);

        expectClass.put("@since", """
                <dt>Since:</dt>
                <dd>1.0</dd>
                """);

        expectClass.put("@version", """
                <dt>Version:</dt>
                <dd>2.0</dd>
                """);

        expectClass.put("@author", """
                <dt>Author:</dt>
                <dd>J. Duke.</dd>
                """);

        expectClass.put("@see", """
                <dt>See Also:</dt>
                <dd>
                <ul class="see-list">
                <li><a href="http://example.com">example</a></li>
                </ul>
                </dd>
                """);
    }

    @Test
    public void testDefault(Path base) {
        test(base, null);
    }

    @Test
    public void testAlpha(Path base) {
        test(base, Comparator.naturalOrder());
    }

    @Test
    public void testReverse(Path base) {
        test(base, Comparator.reverseOrder());
    }

    private void test(Path base, Comparator<String> c) {
        List<String> args = new ArrayList<>();
        args.addAll(List.of(
                "-d", base.resolve("out").toString(),
                "--source-path", src.toString(),
                "--no-platform-links",
                "-author",
                "-version"));
        args.addAll(getTagArgs(c, expectMethod, expectClass));
        args.add("p");

        javadoc(args.toArray(new String[0]));
        checkExit(Exit.OK);

        checkOutput("p/C1.html", true,
                "<dl class=\"notes\">\n"
                        + getExpectString(c, expectMethod)
                        + "</dl>");

        checkOutput("p/C2.html", true,
                "<dl class=\"notes\">\n"
                        + getExpectString(c, expectClass)
                        + "</dl>");
    }

    /**
     * Returns a series of {@code -tag} options derived from the keys of a series of maps,
     * sorted according to the given comparator, or an empty list if the comparator is {@code null}.
     *
     * @param c      the comparator, or {@code null}
     * @param expect the maps from which to infer the options
     *
     * @return the list of options
     */
    @SafeVarargs
    private List<String> getTagArgs(Comparator<String> c, Map<String, String>... expect) {
        if (c == null) {
            return List.of();
        }

        SortedSet<String> allTags = new TreeSet<>(c);
        for (Map<String, String> e : expect) {
            allTags.addAll(e.keySet());
        }
        List<String> args = new ArrayList<>();
        allTags.forEach(t -> { args.add("-tag"); args.add(t.substring(1)); });
        return args;
    }

    /**
     * Returns the "expected string" derived from the values of a map, sorted according
     * to the keys of the map with a given comparator if the comparator is not {@code null}.
     *
     * @param c      the comparator, or {@code null}
     * @param expect the map
     *
     * @return the "expected string"
     */
    private String getExpectString(Comparator<String> c, Map<String, String> expect) {
        Map<String, String> e;
        if (c == null) {
            e = expect;
        } else {
            e = new TreeMap<>(c);
            e.putAll(expect);
        }
        return String.join("", e.values());
    }
}
