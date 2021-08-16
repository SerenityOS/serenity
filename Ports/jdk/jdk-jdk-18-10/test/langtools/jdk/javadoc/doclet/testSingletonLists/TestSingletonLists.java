/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8219998 8221991
 * @summary Eliminate inherently singleton lists
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.javadoc/jdk.javadoc.internal.api
 *          jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.ToolBox toolbox.JavacTask javadoc.tester.*
 * @run main TestSingletonLists
 */

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.function.Function;

import javadoc.tester.HtmlChecker;
import javadoc.tester.JavadocTester;
import toolbox.ModuleBuilder;
import toolbox.ToolBox;


public class TestSingletonLists extends JavadocTester {
    public static void main(String... args) throws Exception {
        TestSingletonLists tester = new TestSingletonLists();
        tester.runTests();
    }

    enum Index  { SINGLE, SPLIT };
    enum Source { PACKAGES, MODULES };

    final ToolBox tb = new ToolBox();

    public void runTests() throws Exception {
        for (Source s : Source.values()) {
            Path src = genSource(s);
                for (Index i : Index.values()) {
                    List<String> args = new ArrayList<>();
                    args.add("-d");
                    args.add(String.format("out-%s-%s", s, i));
                    args.add("-use");
                    if (s != Source.MODULES) {
                        args.add("-linksource"); // broken, with modules: JDK-8219060
                    }
                    if (i == Index.SPLIT) {
                        args.add("-splitIndex");
                    }
                    if (s == Source.PACKAGES) {
                        args.add("-sourcepath");
                        args.add(src.toString());
                        args.add("p1");
                        args.add("p2");
                        args.add("p3");
                    } else {
                        args.add("--module-source-path");
                        args.add(src.toString());
                        args.add("--module");
                        args.add("mA,mB,mC");
                    }
                    javadoc(args.toArray(new String[args.size()]));
                    checkExit(Exit.OK);
                    checkLists();
                }
        }

        printSummary();
    }

    Path genSource(Source s) throws IOException {
        Path src = Path.of("src-" + s);
        switch (s) {
            case PACKAGES:
                for (String p : new String[] { "1", "2", "3" }) {
                    tb.writeJavaFiles(src, genClasses("p" + p));
                }
                break;

            case MODULES:
                for (String m : new String[] { "A", "B", "C"}) {
                    ModuleBuilder mb = new ModuleBuilder(tb, "m" + m);
                    for (String p : new String[] { "1", "2", "3" } ) {
                        mb.exports("p" + m + p);
                        mb.classes(genClasses("p" + m + p));
                    }
                    mb.write(src);
                }
                break;
        }

        return src;
    }


    String[] genClasses(String pkg) {
        List<String> list = new ArrayList<>();
        list.add("package " + pkg + ";");
        for (int i = 0; i < 3; i++) {
            list.add(genClass(pkg, i));
            list.add(genAnno(pkg, i));
            list.add(genEnum(pkg, i));
        }
        return list.toArray(new String[list.size()]);
    }

    String genClass(String pkg, int index) {
        String cn = (pkg + "c" + index).toUpperCase();
        StringBuilder sb = new StringBuilder();
        int pkgIndex = Character.getNumericValue(pkg.charAt(pkg.length()-1));
        String pkgdependency = pkg.substring(0, pkg.length()-1) + (pkgIndex == 3 ? 1 : pkgIndex + 1);
        String enumClassName = pkgdependency.toUpperCase() + "E" + index;
        sb.append("package ").append(pkg).append(";\n")
                .append("import " + pkgdependency + ".*;\n")
                .append("public class ").append(cn).append(" {\n");
        // fields
        for (int f = 0; f < 3; f++) {
            sb.append("public int f").append(f).append(";\n");
        }
        // constructors
        for (int c = 0; c < 3; c++) {
            sb.append("public ").append(cn).append("(");
            for (int i = 0; i < c; i++) {
                sb.append(i == 0 ? "" : ", ").append("int i").append(i);
            }
            sb.append(") { }\n");
        }
        // methods
        for (int m = 0; m < 3; m++) {
            sb.append("public void m").append(m).append("() { }\n");
        }
        sb.append("public void n(").append(enumClassName).append(" e){}");
        sb.append("}\n");
        return sb.toString();
    }

    String genAnno(String pkg, int index) {
        String an = (pkg + "a" + index).toUpperCase();
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n")
                .append("public @interface ").append(an).append(" {\n");
        // fields
        for (int f = 0; f < 3; f++) {
            sb.append("public static final int f").append(f).append(" = 0;\n");
        }
            // values
        for (int v = 0; v < 6; v++) {
            sb.append("public int v").append(v).append("()").append(v< 3 ? "" :  " default " + v).append(";\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    String genEnum(String pkg, int index) {
        String en = (pkg + "e" + index).toUpperCase();
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n")
                .append("public enum ").append(en).append(" {\n");
             // enum members
        for (int e = 0; e < 3; e++) {
            sb.append(e == 0 ? "" : ", ").append("E").append(e);
        }
        sb.append(";\n");
        // fields
        for (int f = 0; f < 3; f++) {
            sb.append("public int f").append(f).append(";\n");
        }
        // methods
        for (int m = 0; m < 3; m++) {
            sb.append("public void m").append(m).append("() { }\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    void checkLists() {
        checking("Check lists");
        ListChecker c = new ListChecker(out, this::readFile);
        try {
            c.checkDirectory(outputDir.toPath());
            c.report();
            int errors = c.getErrorCount();
            if (errors == 0) {
                passed("No list errors found");
            } else {
                failed(errors + " errors found when checking lists");
            }
        } catch (IOException e) {
            failed("exception thrown when reading files: " + e);
        }
    }

    /**
     * A class to check the presence of singleton lists.
     */
    public class ListChecker extends HtmlChecker {
        private int listErrors;
        // Ignore "Constant Field Values" @see items for final fields created by javadoc
        private boolean inSeeList;
        private Stack<Map<String,Integer>> counts = new Stack<>();
        private String fileName;
        private List<String> excludeFiles = List.of(
                "overview-tree.html",
                "package-summary.html",
                "package-tree.html",
                "module-summary.html",
                "help-doc.html");

        ListChecker(PrintStream out, Function<Path,String> fileReader) {
            super(out, fileReader);
        }

        protected int getErrorCount() {
            return errors;
        }

        @Override
        public void report() {
            if (listErrors == 0) {
                out.println("All lists OK");
            } else {
                out.println(listErrors + " list errors");
            }
        }

        @Override
        public void startFile(Path path) {
            fileName = path.getFileName().toString();
        }

        @Override
        public void endFile() {
        }

        @Override
        public void docType(String doctype) {
        }

        @Override
        public void startElement(String name, Map<String,String> attrs, boolean selfClosing) {
            switch (name) {
                case "ul": case "ol": case "dl":
                    counts.push(new TreeMap<>());
                    if ("see-list".equals(attrs.get("class"))) {
                        inSeeList = true;
                    }
                    break;

                case "li": case "dd": case "dt": {
                    Map<String, Integer> c = counts.peek();
                    c.put(name, 1 + c.computeIfAbsent(name, n -> 0));
                    break;
                }
            }
        }

        @Override
        public void endElement(String name) {
            switch (name) {
                case "ul": case "ol": {
                    Map<String,Integer> c = counts.pop();
                    if (inSeeList) {
                        // Ignore "Constant Field Values" @see items for final fields created by javadoc
                        inSeeList = false;
                    } else if (c.get("li") == 0) {
                        error(currFile, getLineNumber(), "empty list");
                        listErrors++;
                    } else if (c.get("li") == 1 && fileName != null && !excludeFiles.contains(fileName)) {
                        error(currFile, getLineNumber(), "singleton list");
                        listErrors++;
                    }
                    break;
                }

                case "dl": {
                    Map<String, Integer> c = counts.pop();
                    if (c.get("dd") == 0 || c.get("dt") == 0) {
                        error(currFile, getLineNumber(), "empty list");
                        listErrors++;
                    }
                    /*if (c.get("dd") == 1 || c.get("dt") == 1) {
                        error(currFile, getLineNumber(), "singleton list");
                        listErrors++;
                    }*/
                    break;
                }
            }
        }
    }
}
