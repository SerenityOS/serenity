/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8218998 8219946 8219060 8241190 8242056 8254627
 * @summary Add metadata to generated API documentation files
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.javadoc/jdk.javadoc.internal.api
 *          jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.ToolBox toolbox.JavacTask javadoc.tester.*
 * @run main TestMetadata
 */

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import toolbox.ModuleBuilder;
import toolbox.ToolBox;

import javadoc.tester.JavadocTester;

public class TestMetadata extends JavadocTester {
    public static void main(String... args) throws Exception {
        TestMetadata tester = new TestMetadata();
        tester.runTests();
    }

    enum Index  { SINGLE, SPLIT };
    enum Source { PACKAGES, MODULES };

    final ToolBox tb = new ToolBox();
    final Set<String> allBodyClassesFound = new HashSet<>();
    final Set<String> allGeneratorsFound = new HashSet<>();

    public void runTests() throws Exception {
        for (Source s : Source.values()) {
            Path src = genSource(s);
                 for (Index i : Index.values()) {
                     List<String> args = new ArrayList<>();
                     args.add("-d");
                     args.add(String.format("out-%s-%s", s, i));
                     args.add("-use");
                     args.add("-linksource");
                     if (i == Index.SPLIT) {
                         args.add("-splitIndex");
                     }
                     if (s == Source.PACKAGES) {
                         args.add("-sourcepath");
                         args.add(src.toString());
                         args.add("pA");
                         args.add("pB");
                     } else {
                         args.add("--module-source-path");
                         args.add(src.toString());
                         args.add("--module");
                         args.add("mA,mB");
                     }
                     javadoc(args.toArray(new String[args.size()]));
                     checkExit(Exit.OK);
                     checkBodyClasses();
                     checkMetadata();

                     // spot check the descriptions for declarations
                     switch (s) {
                         case PACKAGES:
                             checkOutput("pA/package-summary.html", true,
                                     """
                                         <meta name="description" content="declaration: package: pA">""");
                             checkOutput("pA/CA.html", true,
                                     """
                                         <meta name="description" content="declaration: package: pA, class: CA">""");
                             break;

                         case MODULES:
                             checkOutput("mA/module-summary.html", true,
                                     """
                                         <meta name="description" content="declaration: module: mA">""");
                             checkOutput("mA/pA/package-summary.html", true,
                                     """
                                         <meta name="description" content="declaration: module: mA, package: pA">""");
                             checkOutput("mA/pA/CA.html", true,
                                     """
                                         <meta name="description" content="declaration: module: mA, package: pA, class: CA">""");
                             break;
                     }
                 }
        }

        checking ("all generators");
        if (allGeneratorsFound.equals(allGenerators)) {
            passed("all generators found");
        } else {
            Set<String> notFound = new TreeSet<>(allGenerators);
            notFound.removeAll(allGeneratorsFound);
            failed("not found: " + notFound);
        }

        checking ("all body classes");
        if (allBodyClassesFound.equals(allBodyClasses)) {
            passed("all body classes found");
        } else {
            Set<String> notFound = new TreeSet<>(allBodyClasses);
            notFound.removeAll(allBodyClassesFound);
            failed("not found: " + notFound);
        }

        printSummary();
    }

    final Pattern nl = Pattern.compile("[\\r\\n]+");
    final Pattern bodyPattern = Pattern.compile("<body [^>]*class=\"([^\"]+)\"");
    final Set<String> allBodyClasses = Set.of(
        "all-classes-index-page",
        "all-packages-index-page",
        "class-declaration-page",
        "class-use-page",
        "constants-summary-page",
        "deprecated-list-page",
        "doc-file-page",
        "help-page",
        "index-page",
        "index-redirect-page",
        "module-declaration-page",
        "module-index-page",
        "package-declaration-page",
        "package-index-page",
        "package-tree-page",
        "package-use-page",
        "serialized-form-page",
        "source-page",
        "system-properties-page",
        "tree-page"
    );

    void checkBodyClasses() throws IOException {
        Path outputDirPath = outputDir.toPath();
        for (Path p : tb.findFiles(".html", outputDirPath)) {
            checkBodyClass(outputDirPath.relativize(p));
        }
    }

    void checkBodyClass(Path p) {
        checking("Check body: " + p);

        List<String> bodyLines = nl.splitAsStream(readOutputFile(p.toString()))
                .filter(s -> s.contains("<body class="))
                .collect(Collectors.toList());

        String bodyLine;
        switch (bodyLines.size()) {
            case 0:
                 failed("Not found: <body class=");
                 return;
            case 1:
                 bodyLine = bodyLines.get(0);
                 break;
            default:
                 failed("Multiple found: <body class=");
                 return;
        }

        Matcher m = bodyPattern.matcher(bodyLine);
        if (m.find()) {
            String bodyClass = m.group(1);
            if (allBodyClasses.contains(bodyClass)) {
                passed("found: " + bodyClass);
                allBodyClassesFound.add(bodyClass);
            } else {
                failed("Unrecognized body class: " + bodyClass);
            }
        } else {
            failed("Unrecognized line:\n" + bodyLine);
        }
    }

    final Pattern contentPattern = Pattern.compile("content=\"([^\"]+)\">");
    final Pattern generatorPattern = Pattern.compile("content=\"javadoc/([^\"]+)\">");
    final Set<String> allGenerators = Set.of(
            "AllClassesIndexWriter",
            "AllPackagesIndexWriter",
            "ClassUseWriter",
            "ClassWriterImpl",
            "ConstantsSummaryWriterImpl",
            "DeprecatedListWriter",
            "DocFileWriter",
            "HelpWriter",
            "IndexRedirectWriter",
            "IndexWriter",
            "ModuleIndexWriter",
            "ModuleWriterImpl",
            "PackageIndexWriter",
            "PackageTreeWriter",
            "PackageUseWriter",
            "PackageWriterImpl",
            "SerializedFormWriterImpl",
            "SourceToHTMLConverter",
            "SystemPropertiesWriter",
            "TreeWriter"
            );

    void checkMetadata() throws IOException {
        Path outputDirPath = outputDir.toPath();
        for (Path p : tb.findFiles(".html", outputDirPath)) {
            checkMetadata(outputDirPath.relativize(p));
        }
    }

    void checkMetadata(Path p) {
        checking("Check generator: " + p);

        List<String> generators = nl.splitAsStream(readOutputFile(p.toString()))
                .filter(s -> s.contains("<meta name=\"generator\""))
                .collect(Collectors.toList());

        String generator;
        switch (generators.size()) {
            case 0:
                 failed("""
                     Not found: <meta name="generator\"""");
                 return;
            case 1:
                 generator = generators.get(0);
                 break;
            default:
                 failed("""
                     Multiple found: <meta name="generator\"""");
                 return;
        }

        Matcher m = generatorPattern.matcher(generator);
        if (m.find()) {
            String content = m.group(1);
            if (allGenerators.contains(content)) {
                passed("found: " + content);
                allGeneratorsFound.add(content);
                checkDescription(p, content);
            } else {
                failed("Unrecognized content: " + content);
            }
        } else {
            failed("Unrecognized line:\n" + generator);
        }

    }

    void checkDescription(Path p, String generator) {
        checking("Check description: " + p);

        List<String> descriptions = nl.splitAsStream(readOutputFile(p.toString()))
                .filter(s -> s.contains("<meta name=\"description\""))
                .collect(Collectors.toList());

        String description;
        switch (descriptions.size()) {
            case 0:
                if (generator.equals("DocFileWriter")) {
                    passed("Not found, as expected");
                } else {
                    failed("""
                        Not found: <meta name="description\"""");
                }
                return;
            case 1:
                description = descriptions.get(0);
                break;
            default:
                failed("""
                    Multiple found: <meta name="description\"""");
                return;
        }

        String content;
        Matcher m = contentPattern.matcher(description);
        if (m.find()) {
            content = m.group(1);
        } else {
            failed("Unrecognized line:\n" + description);
            return;
        }

        switch (generator) {
            case "AllClassesIndexWriter":
            case "AllPackagesIndexWriter":
            case "ModuleIndexWriter":
            case "PackageIndexWriter":
                check(generator, content, content.contains("index"));
                break;


            case "AnnotationTypeWriterImpl":
            case "ClassWriterImpl":
            case "ModuleWriterImpl":
            case "PackageWriterImpl":
                check(generator, content, content.startsWith("declaration: "));
                break;

            case "ClassUseWriter":
            case "PackageUseWriter":
                check(generator, content, content.startsWith("use: "));
                break;

            case "ConstantsSummaryWriterImpl":
                check(generator, content, content.contains("constants"));
                break;

            case "DeprecatedListWriter":
                check(generator, content, content.contains("deprecated"));
                break;

            case "DocFileWriter":
                passed("no constraint for user-provided doc-files");
                break;

            case "HelpWriter":
                check(generator, content, content.contains("help"));
                break;

            case "IndexRedirectWriter":
                check(generator, content, content.contains("redirect"));
                break;

            case "IndexWriter":
                check(generator, content, content.startsWith("index"));
                break;

            case "PackageTreeWriter":
            case "TreeWriter":
                check(generator, content, content.contains("tree"));
                break;

            case "SerializedFormWriterImpl":
                check(generator, content, content.contains("serialized"));
                break;

            case "SourceToHTMLConverter":
                check(generator, content, content.startsWith("source:"));
                break;

            case "SystemPropertiesWriter":
                check(generator, content, content.contains("system properties"));
                break;

            default:
                failed("unexpected generator: " + generator);
                break;
        }
    }

    void check(String generator, String content, boolean ok) {
        if (ok) {
            passed("OK: " + generator + " " + content);
        } else {
            failed("unexpected value for " + generator + ": " + content);
        }
    }

    Path genSource(Source s) throws IOException {
        Path src = Path.of("src-" + s);
        switch (s) {
            case PACKAGES:
                tb.writeJavaFiles(src,
                    "/** Package pA. {@systemProperty exampleProperty} */ package pA;",
                    "/** Class pA.CA. */ package pA; public class CA { @Deprecated public static final int ZERO = 0; }",
                    "/** Anno pA.Anno, */ package pA; public @interface Anno { }",
                    "/** Serializable pA.Ser, */ package pA; public class Ser implements java.io.Serializable { }",
                    "/** Package pB. */ package pB;",
                    "/** Class pB.CB. */ package pB; public class CB { }");
                tb.writeFile(src.resolve("pA").resolve("doc-files").resolve("extra.html"),
                        """
                            <!doctype html>
                            <html><head></head><body>Extra</body></html>""");
                break;

            case MODULES:
                new ModuleBuilder(tb, "mA")
                        .exports("pA")
                        .classes("/** Package mA/pA. */ package pA;")
                        .classes("/** Class mA/pA.CA. */ package pA; public class CA { @Deprecated public static int ZERO = 0; }")
                        .write(src);
                new ModuleBuilder(tb, "mB")
                        .exports("pB")
                        .classes("/** Package mB/pB. */ package pB;")
                        .classes("/** Class mB/pB.CB. */ package pB; public class CB { }")
                        .write(src);
                break;
        }

        return src;
    }
}
