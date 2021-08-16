/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6227454
 * @summary package.html and overview.html may not be read fully
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.util.ElementFilter;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.DocTrees;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.DocletEnvironment;

public class Test implements Doclet {
    public static void main(String... args) throws Exception {
        new Test().run();
    }

    File referenceFile = new File("Foo.java");

    void run() throws Exception {
        test("<body>ABC      XYZ</body>");
        test("<body>ABC      XYZ</BODY>");
        test("<BODY>ABC      XYZ</body>");
        test("<BODY>ABC      XYZ</BODY>");
        test("<BoDy>ABC      XYZ</bOdY>");
        test("<body>ABC" + bigText(8192, 40) + "XYZ</body>");

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    void test(String body) throws IOException {
        test(body, null);
    }

    void test(String body, String expectError) throws IOException {
        if (!referenceFile.exists()) {
            writeFile(referenceFile.getName(), "public class Foo {}");
        }
        String docType = """
            <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">""";
        String headTag = "<head><title>Title </title></head>";
        String text = docType + "<html>" + headTag + body + "</html>";
        testNum++;
        System.err.println("test " + testNum);
        File file = writeFile("overview" + testNum + ".html", text);
        String thisClassName = Test.class.getName();
        File testSrc = new File(System.getProperty("test.src"));
        String[] args = {
            "-classpath", ".",
            "-package",
            "-overview", file.getPath(),
            new File(testSrc, thisClassName + ".java").getPath()
        };

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = jdk.javadoc.internal.tool.Main.execute(args, pw);
        pw.close();
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        System.err.println("javadoc exit: rc=" + rc);

        if (expectError == null) {
            if (rc != 0)
                error("unexpected exit from javadoc; rc:" + rc);
        } else {
            if (!out.contains(expectError))
                error("expected error text not found: " + expectError);
        }
    }

    String bigText(int lines, int lineLength) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lineLength; i++)
            sb.append(String.valueOf(i % 10));
        sb.append("\n");
        String line = sb.toString();
        sb.setLength(0);
        for (int i = 0; i < lines; i++)
            sb.append(line);
        return sb.toString();
    }

    File writeFile(String path, String body) throws IOException {
        File f = new File(path);
        FileWriter out = new FileWriter(f);
        try {
            out.write(body);
        } finally {
            out.close();
        }
        return f;
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int testNum;
    int errors;

    public boolean run(DocletEnvironment root) {
        DocTrees docTrees = root.getDocTrees();
        System.out.println("classes:" + ElementFilter.typesIn(root.getIncludedElements()));

        Element klass = ElementFilter.typesIn(root.getIncludedElements()).iterator().next();
        String text = "";
        try {
            DocCommentTree dcTree = docTrees.getDocCommentTree(klass, overviewpath);
            text = dcTree.getFullBody().toString();
        } catch (IOException ioe) {
            throw new Error(ioe);
        }

        if (text.length() < 64)
            System.err.println("text: '" + text + "'");
        else
            System.err.println("text: '"
                    + text.substring(0, 20)
                    + "..."
                    + text.substring(text.length() - 20)
                    + "'");
        return text.startsWith("ABC") && text.endsWith("XYZ");
    }

    @Override
    public String getName() {
        return "Test";
    }

    private String overviewpath;

    @Override
    public Set<Option> getSupportedOptions() {
        Option[] options = {
            new Option() {

                @Override
                public int getArgumentCount() {
                    return 1;
                }

                @Override
                public String getDescription() {
                    return "overview";
                }

                @Override
                public Option.Kind getKind() {
                    return Option.Kind.STANDARD;
                }

                @Override
                public List<String> getNames() {
                    List<String> out = new ArrayList<>();
                    out.add("overview");
                    return out;
                }

                @Override
                public String getParameters() {
                    return "url";
                }

                @Override
                public boolean process(String option, List<String> arguments) {
                    overviewpath = arguments.get(0);
                    return true;
                }
            }
        };
        return new HashSet<>(Arrays.asList(options));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    public void init(Locale locale, Reporter reporter) {
        return;
    }
}
