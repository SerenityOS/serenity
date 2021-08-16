/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8138725 8226765
 * @summary test --allow-script-in-comments
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Combo-style test, exercising combinations of different HTML fragments that may contain
 * JavaScript, different places to place those fragments, and whether or not to allow the use
 * of JavaScript.
 */
public class TestScriptInComment {
    public static void main(String... args) throws Exception {
        new TestScriptInComment().run();
    }

    /**
     * Representative samples of different fragments of HTML that may contain JavaScript.
     * To facilitate checking the output manually in a browser, the text "#ALERT" will be
     * replaced by a JavaScript call of "alert(msg)", using a message string that is specific
     * to the test case.
     */
    enum Comment {
        LC("<script>#ALERT</script>", true), // script tag in Lower Case
        UC("<SCRIPT>#ALERT</script>", true), // script tag in Upper Case
        WS("< script >#ALERT</script>", false, "-Xdoclint:none"), // script tag with invalid white space
        SP("""
            <script src="file"> #ALERT </script>""", true), // script tag with an attribute
        ON("<a onclick='#ALERT'>x</a>", true), // event handler attribute
        OME("<img alt='1' onmouseenter='#ALERT'>", true), // onmouseenter event handler attribute
        OML("<img alt='1' onmouseleave='#ALERT'>", true), // onmouseleave event handler attribute
        OFI("<a href='#' onfocusin='#ALERT'>x</a>", true), // onfocusin event handler attribute
        OBE("<a onbogusevent='#ALERT'>x</a>", true), // bogus/future event handler attribute
        URI("<a href='javascript:#ALERT'>x</a>", true); // javascript URI

        /**
         * Creates an HTML fragment to be injected into a template.
         * @param text the HTML fragment to put into a doc comment or option.
         * @param hasScript whether or not this fragment does contain legal JavaScript
         * @param opts any additional options to be specified when javadoc is run
         */
        Comment(String text, boolean hasScript, String... opts) {
            this.text = text;
            this.hasScript = hasScript;
            this.opts = Arrays.asList(opts);
        }

        final String text;
        final boolean hasScript;
        final List<String> opts;
    };

    /**
     * Representative samples of positions in which javadoc may find JavaScript.
     * Each template contains a series of strings, which are written to files or inferred as options.
     * The first source file implies a corresponding output file which should not be written
     * if the comment contains JavaScript and JavaScript is not allowed.
     */
    enum Template {
        OVR("<html><body> overview #COMMENT </body></html>", "package p; public class C { }"),
        PKGINFO("#COMMENT package p;", "package p; public class C { }"),
        PKGHTML("<html><body>#COMMENT package p;</body></html>", "package p; public class C { }"),
        CLS("package p; #COMMENT public class C { }"),
        CON("package p; public class C { #COMMENT public C() { } }"),
        FLD("package p; public class C { #COMMENT public int f; }"),
        MTH("package p; public class C { #COMMENT public void m() { } }"),
        TOP("-top", "lorem #COMMENT ipsum", "package p; public class C { }"),
        HDR("-header", "lorem #COMMENT ipsum", "package p; public class C { }"),
        BTM("-bottom", "lorem #COMMENT ipsum", "package p; public class C { }"),
        DTTL("-doctitle", "lorem #COMMENT ipsum", "package p; public class C { }"),
        PHDR("-packagesheader", "lorem #COMMENT ipsum", "package p; public class C { }");

        Template(String... args) {
            opts = new ArrayList<String>();
            sources = new ArrayList<String>();
            int i = 0;
            while (args[i].startsWith("-")) {
                // all options being tested have a single argument that follow the option
                opts.add(args[i++]);
                opts.add(args[i++]);
            }
            while(i < args.length) {
                sources.add(args[i++]);
            }
        }

        // groups: 1 <html> or not;  2: package name;  3: class name
        private final Pattern pat =
                Pattern.compile("(?i)(<html>)?.*?(?:package ([a-z]+);.*?(?:class ([a-z]+).*)?)?");

        /**
         * Infer the file in which to write the given source.
         * @param dir the base source directory
         * @param src the source text
         * @return the file in which the source should be written
         */
        File getSrcFile(File srcDir, String src) {
            String f;
            Matcher m = pat.matcher(src);
            if (!m.matches())
                throw new Error("match failed");
            if (m.group(3) != null) {
                f = m.group(2) + "/" + m.group(3) + ".java";
            } else if (m.group(2) != null) {
                f = m.group(2) + "/" + (m.group(1) == null ? "package-info.java" : "package.html");
            } else {
                f = "overview.html";
            }
            return new File(srcDir, f);
        }

        /**
         * Get the options to give to javadoc.
         * @param srcDir the srcDir to use -overview is needed
         * @return
         */
        List<String> getOpts(File srcDir) {
            if (!opts.isEmpty()) {
                return opts;
            } else if (sources.get(0).contains("overview")) {
                return Arrays.asList("-overview", getSrcFile(srcDir, sources.get(0)).getPath());
            } else {
                return Collections.emptyList();
            }
        }

        /**
         * Gets the output file corresponding to the first source file.
         * This file should not be written if the comment contains JavaScript and JavaScripot is
         * not allowed.
         * @param dir the base output directory
         * @return the output file
         */
        File getOutFile(File outDir) {
            String f;
            Matcher m = pat.matcher(sources.get(0));
            if (!m.matches())
                throw new Error("match failed");
            if (m.group(3) != null) {
                f = m.group(2) + "/" + m.group(3) + ".html";
            } else if (m.group(2) != null) {
                f = m.group(2) + "/package-summary.html";
            } else {
                f = "overview-summary.html";
            }
            return new File(outDir, f);
        }

        final List<String> opts;
        final List<String> sources;
    };

    enum Option {
        OFF(null),
        ON("--allow-script-in-comments");

        Option(String text) {
            this.text = text;
        }

        final String text;
    };

    private PrintStream out = System.err;

    public void run() throws Exception {
        int count = 0;
        for (Template template: Template.values()) {
            for (Comment comment: Comment.values()) {
                for (Option option: Option.values()) {
                    if (test(template, comment, option)) {
                        count++;
                    }
                }
            }
        }

        out.println(count + " test cases run");
        if (errors > 0) {
            throw new Exception(errors + " errors occurred");
        }
    }

    boolean test(Template template, Comment comment, Option option) throws IOException {
        if (option == Option.ON && !comment.hasScript) {
            // skip --allowScriptInComments if comment does not contain JavaScript
            return false;
        }

        String test = template + "-" + comment + "-" + option;
        out.println("Test: " + test);

        File dir = new File(test);
        dir.mkdirs();
        File srcDir = new File(dir, "src");
        File outDir = new File(dir, "out");

        String alert = "alert(\"" + test + "\");";
        for (String src: template.sources) {
            writeFile(template.getSrcFile(srcDir, src),
                src.replace("#COMMENT",
                        "/** " + comment.text.replace("#ALERT", alert) + " **/"));
        }

        List<String> opts = new ArrayList<String>();
        opts.add("-sourcepath");
        opts.add(srcDir.getPath());
        opts.add("-d");
        opts.add(outDir.getPath());
        if (option.text != null)
            opts.add(option.text);
        for (String opt: template.getOpts(srcDir)) {
            opts.add(opt.replace("#COMMENT", comment.text.replace("#ALERT", alert)));
        }
        opts.addAll(comment.opts);
        opts.add("-noindex");   // index not required; save time/space writing files
        opts.add("p");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = javadoc(opts, pw);
        pw.close();
        String log = sw.toString();
        writeFile(new File(dir, "log.txt"), log);

        out.println("opts: " + opts);
        out.println("  rc: " + rc);
        out.println(" log:");
        out.println(log);

        String ERROR = "Use --allow-script-in-comment";
        File outFile = template.getOutFile(outDir);

        boolean expectErrors = comment.hasScript && (option == Option.OFF);

        if (expectErrors) {
            check(rc != 0, "unexpected exit code: " + rc);
            check(log.contains(ERROR), "expected error message not found");
            check(!outFile.exists(), "output file found unexpectedly");
        } else {
            check(rc == 0, "unexpected exit code: " + rc);
            check(!log.contains(ERROR), "error message found");
            check(outFile.exists(), "output file not found");
        }

        out.println();
        return true;
    }

    int javadoc(List<String> opts, PrintWriter pw) {
        return jdk.javadoc.internal.tool.Main.execute(opts.toArray(new String[opts.size()]), pw);
    }

    File writeFile(File f, String text) throws IOException {
        f.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(f);
        try {
            fw.write(text);
        } finally {
            fw.close();
        }
        return f;
    }

    void check(boolean cond, String errMessage) {
        if (!cond) {
            error(errMessage);
        }
    }

    void error(String message) {
        out.println("Error: " + message);
        errors++;
    }

    int errors = 0;
}

