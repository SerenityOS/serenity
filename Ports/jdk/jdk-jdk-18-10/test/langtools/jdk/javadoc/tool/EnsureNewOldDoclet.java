/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8035473 8154482 8154399 8159096 8176131 8176331 8177511
 * @summary make sure the javadoc tool responds correctly,
 *          to old and new doclet implementations.
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.TestRunner
 * @run main EnsureNewOldDoclet
 */

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;

import com.sun.source.doctree.DocTree;

import toolbox.*;


/**
 * This test ensures the doclet responds correctly when given
 * various conditions that force a fall back to the old javadoc
 * tool.
 */
public class EnsureNewOldDoclet extends TestRunner {

    final ToolBox tb;
    final File testSrc;
    final Path javadocPath;
    final ExecTask task;
    final String testClasses;
    final PrintStream ostream;

    final static String CLASS_NAME = "EnsureNewOldDoclet";
    final static String OLD_DOCLET_CLASS_NAME = CLASS_NAME + "$OldDoclet";
    final static String NEW_DOCLET_CLASS_NAME = CLASS_NAME + "$NewDoclet"; //unused
    final static String NEW_TAGLET_CLASS_NAME = CLASS_NAME + "$NewTaglet";

    final static Pattern OLD_HEADER = Pattern.compile(".*This is not the Standard Doclet.*");
    final static Pattern NEW_HEADER = Pattern.compile("^Standard Doclet version.*");


    final static String OLD_DOCLET_MARKER = "OLD_DOCLET_MARKER";

    final static String NEW_DOCLET_MARKER = "NEW_DOCLET_MARKER";
    final static String NEW_TAGLET_MARKER = "Registered Taglet " + CLASS_NAME + "\\$NewTaglet";

    final static String NEW_STDDOCLET = "jdk.javadoc.doclet.StandardDoclet";


    public EnsureNewOldDoclet() throws Exception {
        super(System.err);
        ostream = System.err;
        testClasses = System.getProperty("test.classes");
        tb = new ToolBox();
        javadocPath = tb.getJDKTool("javadoc");
        task = new ExecTask(tb, javadocPath);
        testSrc = new File("Foo.java");
        generateSample(testSrc);
    }

    void generateSample(File testSrc) throws Exception {
        String nl = System.getProperty("line.separator");
        String src = Arrays.asList(
            "/**",
            " * A test class to test javadoc. Nothing more nothing less.",
            " */",
            " public class Foo{}").stream().collect(Collectors.joining(nl));
        tb.writeFile(testSrc.getPath(), src);
    }

    public static void main(String... args) throws Exception {
        new EnsureNewOldDoclet().runTests();
    }

    // input: nothing, default mode
    // outcome: new tool and new doclet
    @Test
    public void testDefault() throws Exception {
        setArgs("-classpath", ".", // insulates us from ambient classpath
                  testSrc.toString());
        Task.Result tr = task.run(Task.Expect.SUCCESS);
        List<String> err = tr.getOutputLines(Task.OutputKind.STDERR);
        checkOutput(testName, err, NEW_HEADER);
    }

    // input: new doclet and new taglet
    // outcome: new doclet and new taglet should register
    @Test
    public void testNewDocletNewTaglet() throws Exception {
        setArgs("-classpath", ".", // ambient classpath insulation
                "-doclet",
                NEW_STDDOCLET,
                "-taglet",
                NEW_TAGLET_CLASS_NAME,
                "-tagletpath",
                testClasses,
                testSrc.toString());
        Task.Result tr = task.run(Task.Expect.SUCCESS);
        List<String> out = tr.getOutputLines(Task.OutputKind.STDOUT);
        List<String> err = tr.getOutputLines(Task.OutputKind.STDERR);
        checkOutput(testName, err, NEW_HEADER);
        checkOutput(testName, err, NEW_TAGLET_MARKER);
    }

    void setArgs(String... args) {
        ostream.println("cmds: " + Arrays.asList(args));
        task.args(args);
    }

    void checkOutput(String testCase, List<String> content, String toFind) throws Exception {
        checkOutput(testCase, content, Pattern.compile(".*" + toFind + ".*"));
    }

    void checkOutput(String testCase, List<String> content, Pattern toFind) throws Exception {
        ostream.println("---" + testCase + "---");
        content.stream().forEach(x -> System.out.println(x));
        for (String x : content) {
            ostream.println(x);
            if (toFind.matcher(x).matches()) {
                return;
            }
        }
        throw new Exception(testCase + ": Expected string not found: " +  toFind);
    }

    public static class NewTaglet implements jdk.javadoc.doclet.Taglet {

        @Override
        public Set<Location> getAllowedLocations() {
            return Collections.emptySet();
        }

        @Override
        public boolean isInlineTag() {
            return true;
        }

        @Override
        public String getName() {
            return "NewTaglet";
        }

        @Override
        public String toString(List<? extends DocTree> tags, Element element) {
            return tags.toString();
        }

    }
}
