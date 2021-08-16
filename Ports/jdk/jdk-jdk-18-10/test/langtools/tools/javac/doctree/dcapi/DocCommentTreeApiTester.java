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

/*
 * @test
 * @bug  8132096 8157611 8190552 8251357
 * @summary test the APIs  in the DocTree interface
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @compile ../DocCommentTester.java DocCommentTreeApiTester.java
 * @run main DocCommentTreeApiTester
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.DocPretty;

public class DocCommentTreeApiTester {

    private static final String MARKER_START = "EXPECT_START";
    private static final String MARKER_END   = "EXPECT_END";

    private static final String testSrc = System.getProperty("test.src", ".");

    private static final JavacTool javac = JavacTool.create();

    private static final DocCommentTester.ASTChecker.Printer printer =
            new DocCommentTester.ASTChecker.Printer();
    int pass;
    int fail;

    public DocCommentTreeApiTester() {
        pass = 0;
        fail = 0;
    }

    public static void main(String... args) throws Exception {
        DocCommentTreeApiTester test = new DocCommentTreeApiTester();
        try {
            // test getting a DocTree from an element
            test.runElementAndBreakIteratorTests("Anchor.java", "Anchor test.");

            // test relative paths in a class within a package
            test.runRelativePathTest("pkg/Anchor.java", "package.html", true);

            // tests files relative path in an unnamed package
            test.runRelativePathTest("Anchor.java", "overview0.html", true);

            // test doctree path and Doc
            test.runDocTreePath("Anchor.java", "package.html");

            // test for correct parsing using valid and some invalid html tags
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(Path.of(testSrc))) {
                for (Path entry: ds) {
                    String name = entry.getFileName().toString();
                    if (name.matches("overview[0-9]+\\.html")) {
                        test.runFileObjectTest(name);
                    }
                }
            }

        } finally {
            test.status();
        }
    }
    void status() throws Exception {
        System.err.println("pass:" + pass + "  fail: " + fail);
        if (fail > 0) {
            throw new Exception("Fails");
        }
    }

    /**
     * Tests getting a DocCommentTree from an element, as well
     * as test if break iterator setter/getter works correctly.
     *
     * @param javaFileName a test file to be processed
     * @param expected the expected output
     * @throws java.io.IOException
     */
    public void runElementAndBreakIteratorTests(String javaFileName, String expected) throws IOException {
        List<File> javaFiles = new ArrayList<>();
        javaFiles.add(new File(testSrc, javaFileName));

        List<File> dirs = new ArrayList<>();
        dirs.add(new File(testSrc));

        try (StandardJavaFileManager fm = javac.getStandardFileManager(null, null, null)) {
            fm.setLocation(javax.tools.StandardLocation.SOURCE_PATH, dirs);
            Iterable<? extends JavaFileObject> fos = fm.getJavaFileObjectsFromFiles(javaFiles);

            final JavacTask t = javac.getTask(null, fm, null, null, null, fos);
            final DocTrees trees = DocTrees.instance(t);

            Iterable<? extends Element> elements = t.analyze();

            Element klass = elements.iterator().next();
            DocCommentTree dcTree = trees.getDocCommentTree(klass);

            List<? extends DocTree> firstSentence = dcTree.getFirstSentence();
            StringWriter sw = new StringWriter();
            DocPretty pretty = new DocPretty(sw);
            pretty.print(firstSentence);
            check("getDocCommentTree(Element)", expected, sw.toString());

            BreakIterator bi = BreakIterator.getSentenceInstance(Locale.FRENCH);
            trees.setBreakIterator(bi);
            BreakIterator nbi = trees.getBreakIterator();
            if (bi.equals(nbi)) {
                pass++;
                check("getDocCommentTree(Element) with BreakIterator", expected, sw.toString());
            } else {
                fail++;
                System.err.println("BreakIterators don't match");
            }
        }
    }

    /**
     * Tests DocTrees.getDocCommentTree(Element e, String relpath) using relative path.
     *
     * @param javaFileName the reference java file
     * @param fileName the relative html file
     * @throws java.lang.Exception ouch
     */
    public void runRelativePathTest(String javaFileName, String fileName,
                                    boolean bodyOnly) throws Exception  {
        List<File> javaFiles = new ArrayList<>();
        javaFiles.add(new File(testSrc, javaFileName));

        List<File> dirs = new ArrayList<>();
        dirs.add(new File(testSrc));

        try (StandardJavaFileManager fm = javac.getStandardFileManager(null, null, null)) {
            fm.setLocation(javax.tools.StandardLocation.SOURCE_PATH, dirs);
            Iterable<? extends JavaFileObject> fos = fm.getJavaFileObjectsFromFiles(javaFiles);

            final JavacTask t = javac.getTask(null, fm, null, null, null, fos);
            final DocTrees trees = DocTrees.instance(t);

            Iterable<? extends Element> elements = t.analyze();

            Element klass = elements.iterator().next();

            DocCommentTree dcTree = trees.getDocCommentTree(klass, fileName);

            if (dcTree == null)
                throw new Error("invalid input: " + fileName);

            StringWriter sw = new StringWriter();
            printer.print(dcTree, sw);
            String found = sw.toString();

            FileObject htmlFo = fm.getFileForInput(javax.tools.StandardLocation.SOURCE_PATH,
                    t.getElements().getPackageOf(klass).getQualifiedName().toString(),
                    fileName + ".out");

            String expected = getExpected(htmlFo.openReader(true));
            astcheck(fileName, expected, found);
        }
    }

    /**
     * Tests DocTrees.getDocCommentTree(FileObject fo).
     *
     * @param htmlfileName the file to be parsed
     * @throws Exception when an error occurs.
     */
    public void runFileObjectTest(String htmlfileName) throws Exception {
        List<File> javaFiles =  Collections.emptyList();

        List<File> otherFiles = new ArrayList<>();
        otherFiles.add(new File(testSrc, htmlfileName));
        otherFiles.add(new File(testSrc, htmlfileName + ".out"));

        try (StandardJavaFileManager fm = javac.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> fos = fm.getJavaFileObjectsFromFiles(javaFiles);
            Iterable<? extends JavaFileObject> others = fm.getJavaFileObjectsFromFiles(otherFiles);

            final JavacTask t = javac.getTask(null, fm, null, null, null, fos);
            final DocTrees trees = DocTrees.instance(t);

            StringWriter sw = new StringWriter();
            DocCommentTree dct = null;
            String expected = null;
            for (JavaFileObject jfo : others) {
                switch (jfo.getKind()) {
                    case HTML:
                        dct = trees.getDocCommentTree(jfo);
                        if (dct == null)
                            throw new Exception("invalid input: " + jfo);
                        break;
                    default:
                        expected = getExpected(jfo.openReader(true));
                }
            }

            printer.print(dct, sw);
            String found = sw.toString();
            astcheck(otherFiles.toString(), expected, found);
        }
    }

    /**
     * Tests DocTrees.getDocTreePath(PackageElement p, FileObject fo).
     *
     * @param javaFileName the java anchor file
     * @param pkgFileName the package file name
     * @throws Exception e if something goes awry
     */
    public void runDocTreePath(String javaFileName, String pkgFileName) throws Exception  {
        List<File> javaFiles = new ArrayList<>();
        javaFiles.add(new File(testSrc, javaFileName));

        List<File> otherFiles = new ArrayList<>();
        otherFiles.add(new File(testSrc, pkgFileName + ".out"));

        List<File> dirs = new ArrayList<>();
        dirs.add(new File(testSrc));

        try (StandardJavaFileManager fm = javac.getStandardFileManager(null, null, null)) {
            fm.setLocation(javax.tools.StandardLocation.SOURCE_PATH, dirs);
            Iterable<? extends JavaFileObject> fos = fm.getJavaFileObjectsFromFiles(javaFiles);

            final JavacTask t = javac.getTask(null, fm, null, null, null, fos);
            final DocTrees trees = DocTrees.instance(t);
            final Elements elementUtils = t.getElements();

            Iterable<? extends Element> elements = t.analyze();

            Element klass = elements.iterator().next();
            PackageElement pkg = elementUtils.getPackageOf(klass);

            FileObject htmlFo = fm.getFileForInput(javax.tools.StandardLocation.SOURCE_PATH,
                    t.getElements().getPackageOf(klass).getQualifiedName().toString(),
                    "package.html");
            DocTreePath treePath = trees.getDocTreePath(htmlFo, pkg);

            if (treePath == null) {
                throw new Exception("invalid input: " + htmlFo);
            }

            DocCommentTree dcTree = treePath.getDocComment();
            if (dcTree == null)
                throw new Exception("invalid input" + htmlFo);

            StringWriter sw = new StringWriter();
            printer.print(dcTree, sw);
            String found = sw.toString();
            Iterable<? extends JavaFileObject> oos = fm.getJavaFileObjectsFromFiles(otherFiles);
            JavaFileObject otherFo = oos.iterator().next();
            String expected = getExpected(otherFo.openReader(true));

            astcheck(pkgFileName, expected, found);
        }
    }

    void astcheck(String testinfo, String expected, String found) {
        System.err.print("ASTChecker: " + testinfo);
        check0(expected, found);
    }
    void check(String testinfo, String expected, String found) {
        System.err.print(testinfo);
        check0(expected, found);
    }
    void check0(String expected, String found) {
        if (expected.equals(found)) {
            pass++;
            System.err.println(" PASS");
        } else {
            fail++;
            System.err.println(" FAILED");
            System.err.println("Expect:\n" + expected);
            System.err.println("Found:\n" + found);
        }
    }

    String getExpected(Reader inrdr) throws IOException {
        BufferedReader rdr = new BufferedReader(inrdr);
        List<String> lines = new ArrayList<>();
        String line = rdr.readLine();
        while (line != null) {
            lines.add(line);
            line = rdr.readLine();
        }
        return getExpected(lines);
    }

    String getExpected(List<String> lines) {
        boolean start = false;
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        for (String line : lines) {
            if (!start) {
                start = line.startsWith(MARKER_START);
                continue;
            }
            if (line.startsWith(MARKER_END)) {
                out.flush();
                return sw.toString();
            }
            out.println(line);
        }
        return out.toString() + "Warning: html comment end not found";
    }
}

