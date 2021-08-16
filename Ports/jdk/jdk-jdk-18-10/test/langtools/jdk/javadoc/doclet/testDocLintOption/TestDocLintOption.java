/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8236949 8238259
 * @summary javadoc -Xdoclint does not accumulate options correctly
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 *          jdk.javadoc/jdk.javadoc.internal.tool
 * @build   toolbox.ToolBox javadoc.tester.*
 * @run main TestDocLintOption
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.javadoc.internal.doclint.Messages.Group;
import static jdk.javadoc.internal.doclint.Messages.Group.*;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

/**
 * Runs javadoc with different sets of doclint options, and checks that
 * only the appropriate set of doclint messages are reported.
 */
public class TestDocLintOption extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestDocLintOption tester = new TestDocLintOption();
        tester.generateSrc();
        tester.runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    private final ToolBox tb = new ToolBox();

    @Test
    public void testNone(Path base) throws Exception {
        test(base, List.of("-Xdoclint:none"), Exit.OK, EnumSet.noneOf(Group.class));
    }

    @Test
    public void testAll(Path base) throws Exception {
        test(base, List.of("-Xdoclint:all"), Exit.ERROR, EnumSet.allOf(Group.class));
    }

    @Test
    public void testAccessibility(Path base) throws Exception {
        test(base, List.of("-Xdoclint:accessibility"), Exit.ERROR, EnumSet.of(ACCESSIBILITY));
    }

    @Test
    public void testHtml(Path base) throws Exception {
        test(base, List.of("-Xdoclint:html"), Exit.ERROR, EnumSet.of(HTML));
    }

    @Test
    public void testMissing(Path base) throws Exception {
        test(base, List.of("-Xdoclint:missing"), Exit.OK, EnumSet.of(MISSING));
    }

    @Test
    public void testReference(Path base) throws Exception {
        test(base, List.of("-Xdoclint:reference"), Exit.ERROR, EnumSet.of(REFERENCE));
    }

    @Test
    public void testSyntax(Path base) throws Exception {
        test(base, List.of("-Xdoclint:syntax"), Exit.ERROR, EnumSet.of(SYNTAX));
    }

    @Test
    public void testHtmlSyntax_1(Path base) throws Exception {
        test(base, List.of("-Xdoclint:html,syntax"), Exit.ERROR, EnumSet.of(HTML, SYNTAX));
    }

    @Test
    public void testHtmlSyntax_2(Path base) throws Exception {
        test(base, List.of("-Xdoclint:html", "-Xdoclint:syntax"), Exit.ERROR, EnumSet.of(HTML, SYNTAX));
    }

    @Test
    public void testNoSyntax_1(Path base) throws Exception {
        Set<Group> set = EnumSet.allOf(Group.class);
        set.remove(SYNTAX);
        test(base, List.of("-Xdoclint:all,-syntax"), Exit.ERROR, set);
    }

    @Test
    public void testNoSyntax_2(Path base) throws Exception {
        Set<Group> set = EnumSet.allOf(Group.class);
        set.remove(SYNTAX);
        test(base, List.of("-Xdoclint:all", "-Xdoclint:-syntax"), Exit.ERROR, set);
    }

    @Test
    public void testNoSyntax_3(Path base) throws Exception {
        // no positive entries; equivalent to "none"
        test(base, List.of("-Xdoclint:-syntax"), Exit.OK, EnumSet.noneOf(Group.class));
    }

    /**
     * Runs javadoc with a given set of doclint options, and checks that
     * only the appropriate set of doclint messages are reported.
     * Note: this does not bother to check the "legacy" messages generated
     * when doclint is disabled (for example, with {@code -Xdoclint:none}).
     */
    void test(Path base, List<String> options, Exit expectExit, Set<Group> expectGroups) {
        List<String> allOpts = new ArrayList<>();
        allOpts.addAll(List.of(
                "-d", base.resolve("out").toString(),
                "-sourcepath", "src"));
        allOpts.addAll(options);
        allOpts.add("p");

        javadoc(allOpts.toArray(new String[0]));
        checkExit(expectExit);

        checkOutput(Output.OUT, expectGroups.contains(ACCESSIBILITY),
                """
                    C.java:4: error: no "alt" attribute for image""");

        checkOutput(Output.OUT, expectGroups.contains(HTML),
                "C.java:8: error: text not allowed in <ul> element");

        checkOutput(Output.OUT, expectGroups.contains(MISSING),
                "C.java:13: warning: no @return");

        checkOutput(Output.OUT, expectGroups.contains(REFERENCE),
                "C.java:15: error: invalid use of @return");

        checkOutput(Output.OUT, expectGroups.contains(SYNTAX),
                "C.java:19: error: bad HTML entity");
    }

    /**
     * Generates a source file containing one issue in each group of
     * issues detected by doclint. The intent is not to detect all issues,
     * but instead, to detect whether the different groups of issues are
     * correctly enabled or disabled by the {@code -Xdoclint} options.
     */
    private void generateSrc() throws IOException {
        Path src = Path.of("src");
        tb.writeJavaFiles(src,
                  """
                      package p;
                      public class C {
                        /** Comment.
                         *  <img src="foo.png">
                      """       // "accessibility"" no alt attribute
                + """
                    \s  */
                      public void mAccessibility() { }
                      /** Comment.
                       *  <ul>123</ul>
                    """                // "HTML": text not allowed
                + """
                    \s  */
                      public void mHtml() { }
                      /** Comment.
                       */
                    """                             // "missing": no @return
                + """
                    \s public int mMissing() { }
                      /** Comment.
                       *  @return error
                    """               // "reference": invalid @return
                + """
                    \s  */
                      public void mReference() { }
                      /** Comment.
                       *  a & b
                    """                       // "syntax": bad use of &
                + """
                       */
                      public void mSyntax() { }
                    }""");
    }
}


