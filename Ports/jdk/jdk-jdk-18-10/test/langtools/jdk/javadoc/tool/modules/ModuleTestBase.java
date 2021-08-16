/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.SimpleElementVisitor14;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import toolbox.JavadocTask;
import toolbox.Task;
import toolbox.Task.Expect;
import toolbox.TestRunner;
import toolbox.ToolBox;

import static toolbox.Task.OutputKind.*;

/**
 * Base class for module tests.
 */
public class ModuleTestBase extends TestRunner {

    // Field Separator
    private static final String FS = " ";

    protected ToolBox tb;
    private final Class<?> docletClass;

    private Task.Result currentTask = null;

    ModuleTestBase() {
        super(System.err);
        tb = new ToolBox();
        ClassLoader cl = ModuleTestBase.class.getClassLoader();
        try {
            docletClass = cl.loadClass("ModuleTestBase$ModulesTesterDoclet");
        } catch (ClassNotFoundException cfe) {
            throw new Error(cfe);
        }
    }

    /**
     * Execute methods annotated with @Test, and throw an exception if any
     * errors are reported..
     *
     * @throws Exception if any errors occurred
     */
    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    Task.Result execTask(String... args) {
        return execTask0(false, args);
    }

    Task.Result execNegativeTask(String... args) {
        return execTask0(true, args);
    }

    private Task.Result execTask0(boolean isNegative, String... args) {
        JavadocTask et = new JavadocTask(tb, Task.Mode.API);
        et.docletClass(docletClass);
        //Arrays.asList(args).forEach((a -> System.err.println("arg: " + a)));
        System.err.println(Arrays.asList(args));
        currentTask = isNegative
                ? et.options(args).run(Expect.FAIL)
                : et.options(args).run();
        return currentTask;
    }

    Path[] findHtmlFiles(Path... paths) throws IOException {
        return tb.findFiles(".html", paths);
    }

    boolean grep(String regex, Path file) throws Exception {
        List<String> lines = tb.readAllLines(file);
        List<String> foundList = tb.grep(regex, lines);
        return !foundList.isEmpty();
    }

    String normalize(String in) {
        return in.replace('\\', '/');
    }

    void checkModulesSpecified(String... args) throws Exception {
        for (String arg : args) {
            checkDocletOutputPresent("Specified", ElementKind.MODULE, arg);
        }
    }

    void checkPackagesSpecified(String... args) throws Exception {
        for (String arg : args) {
            checkDocletOutputPresent("Specified", ElementKind.PACKAGE, arg);
        }
    }

    void checkTypesSpecified(String... args) throws Exception {
        for (String arg : args) {
            checkDocletOutputPresent("Specified", ElementKind.CLASS, arg);
        }
    }

    void checkModulesIncluded(String... args) throws Exception {
        for (String arg : args) {
            checkDocletOutputPresent("Included", ElementKind.MODULE, arg);
        }
    }

    void checkPackagesIncluded(String... args) throws Exception {
        for (String arg : args) {
            checkDocletOutputPresent("Included", ElementKind.PACKAGE, arg);
        }
    }

    void checkTypesIncluded(String... args) throws Exception {
        for (String arg : args) {
            checkDocletOutputPresent("Included", ElementKind.CLASS, arg);
        }
    }

    void checkTypesSelected(String... args) throws Exception {
        for (String arg : args) {
            checkDocletOutputPresent("Selected", ElementKind.CLASS, arg);
        }
    }

    void checkMembersSelected(String... args) throws Exception {
        for (String arg : args) {
            checkDocletOutputPresent("Selected", ElementKind.METHOD, arg);
        }
    }

    void checkModuleMode(String mode) throws Exception {
        assertPresent("^ModuleMode" + FS + mode);
    }

    void checkStringPresent(String regex) throws Exception {
        assertPresent(regex);
    }

    void checkDocletOutputPresent(String category, ElementKind kind, String regex) throws Exception {
        assertPresent("^" + category + " " + kind.toString() + " " + regex);
    }

    void assertPresent(String regex) throws Exception {
        assertPresent(regex, STDOUT);
    }

    void assertMessagePresent(String regex) throws Exception {
        assertPresent(regex, Task.OutputKind.DIRECT);
    }

    void assertMessageNotPresent(String regex) throws Exception {
        assertNotPresent(regex, Task.OutputKind.DIRECT);
    }

    void assertPresent(String regex, Task.OutputKind kind) throws Exception {
        List<String> foundList = tb.grep(regex, currentTask.getOutputLines(kind));
        if (foundList.isEmpty()) {
            dumpDocletDiagnostics();
            throw new Exception(regex + " not found in: " + kind);
        }
    }

    void assertNotPresent(String regex, Task.OutputKind kind) throws Exception {
        List<String> foundList = tb.grep(regex, currentTask.getOutputLines(kind));
        if (!foundList.isEmpty()) {
            dumpDocletDiagnostics();
            throw new Exception(regex + " found in: " + kind);
        }
    }

    void dumpDocletDiagnostics() {
        for (Task.OutputKind kind : Task.OutputKind.values()) {
            String output = currentTask.getOutput(kind);
            if (output != null && !output.isEmpty()) {
                System.err.println("<" + kind + ">");
                System.err.println(output);
            }
        }
    }

    void checkModulesNotSpecified(String... args) throws Exception {
        for (String arg : args) {
            checkDocletOutputAbsent("Specified", ElementKind.MODULE, arg);
        }
    }

    void checkPackagesNotSpecified(String... args) throws Exception {
        for (String arg : args) {
            checkDocletOutputAbsent("Specified", ElementKind.PACKAGE, arg);
        }
    }

    void checkTypesNotSpecified(String... args) throws Exception {
        for (String arg : args) {
            checkDocletOutputAbsent("Specified", ElementKind.CLASS, arg);
        }
    }

    void checkModulesNotIncluded(String... args) throws Exception {
        for (String arg : args) {
            checkDocletOutputAbsent("Included", ElementKind.MODULE, arg);
        }
    }

    void checkPackagesNotIncluded(String... args) throws Exception {
        for (String arg : args) {
            checkDocletOutputAbsent("Included", ElementKind.PACKAGE, arg);
        }
    }

    void checkTypesNotIncluded(String... args) throws Exception {
        for (String arg : args) {
            checkDocletOutputAbsent("Included", ElementKind.CLASS, arg);
        }
    }

    void checkMembersNotSelected(String... args) throws Exception {
        for (String arg : args) {
            checkDocletOutputAbsent("Selected", ElementKind.METHOD, arg);
        }
    }

    void checkStringAbsent(String regex) throws Exception {
        assertAbsent(regex);
    }

    void checkDocletOutputAbsent(String category, ElementKind kind, String regex) throws Exception {
        assertAbsent("^" + category + FS + kind.toString() + FS + regex);
    }

    void assertAbsent(String regex) throws Exception {
        assertAbsent(regex, STDOUT);
    }

    void assertAbsent(String regex, Task.OutputKind kind) throws Exception {
        List<String> foundList = tb.grep(regex, currentTask.getOutputLines(kind));
        if (!foundList.isEmpty()) {
            dumpDocletDiagnostics();
            throw new Exception(regex + " found in: " + kind);
        }
    }

    public static class ModulesTesterDoclet implements Doclet {
        StringWriter sw = new StringWriter();
        PrintWriter ps = new PrintWriter(sw);

        DocletEnvironment docEnv = null;

        boolean hasDocComments = false;

        String hasDocComments(Element e) {
            String comment = docEnv.getElementUtils().getDocComment(e);
            return comment != null && !comment.isEmpty()
                    ? "hasDocComments"
                    : "noDocComments";
        }

        // csv style output, for simple regex verification
        void printDataSet(String header, Set<? extends Element> set) {
            for (Element e : set) {
                ps.print(header);
                new SimpleElementVisitor14<Void, Void>() {
                    @Override
                    public Void visitModule(ModuleElement e, Void p) {
                        ps.print(FS);
                        ps.print(e.getKind());
                        ps.print(FS);
                        ps.print(e.getQualifiedName());
                        if (hasDocComments) {
                            ps.print(FS);
                            ps.print(hasDocComments(e));
                        }
                        ps.println();
                        return null;
                    }

                    @Override
                    public Void visitPackage(PackageElement e, Void p) {
                        ps.print(FS);
                        ps.print(e.getKind());
                        ps.print(FS);
                        ps.print(e.getQualifiedName());
                        if (hasDocComments) {
                            ps.print(FS);
                            ps.print(hasDocComments(e));
                        }
                        ps.println();
                        return null;
                    }

                    @Override
                    public Void visitType(TypeElement e, Void p) {
                        ps.print(FS);
                        ps.print(ElementKind.CLASS);
                        ps.print(FS);
                        ps.print(e.getQualifiedName());
                        if (hasDocComments) {
                            ps.print(FS);
                            ps.print(hasDocComments(e));
                        }
                        ps.println();
                        return null;
                    }

                    @Override
                    protected Void defaultAction(Element e, Void p) {
                        Element encl = e.getEnclosingElement();
                        CharSequence fqn = new SimpleElementVisitor14<CharSequence, Void>() {
                            @Override
                            public CharSequence visitModule(ModuleElement e, Void p) {
                                return e.getQualifiedName();
                            }

                            @Override
                            public CharSequence visitType(TypeElement e, Void p) {
                                return e.getQualifiedName();
                            }

                            @Override
                            public CharSequence visitPackage(PackageElement e, Void p) {
                                return e.getQualifiedName();
                            }

                        }.visit(encl);

                        ps.print(FS);
                        ps.print(ElementKind.METHOD); // always METHOD
                        ps.print(FS);
                        ps.print(fqn);
                        ps.print(".");
                        ps.print(e.getSimpleName());
                        if (hasDocComments) {
                            ps.print(FS);
                            ps.print(hasDocComments(e));
                        }
                        ps.println();
                        return null;
                    }
                }.visit(e);
            }
        }

        @Override
        public boolean run(DocletEnvironment docenv) {
            this.docEnv = docenv;
            ps.println("ModuleMode" + FS + docenv.getModuleMode());
            printDataSet("Specified", docenv.getSpecifiedElements());
            printDataSet("Included", docenv.getIncludedElements());
            printDataSet("Selected", getAllSelectedElements(docenv));
            System.out.println(sw);
            return true;
        }

        Set<Element> getAllSelectedElements(DocletEnvironment docenv) {
            Set<Element> result = new TreeSet<Element>((Element e1, Element e2) -> {
                // some grouping by kind preferred
                int rc = e1.getKind().compareTo(e2.getKind());
                if (rc != 0) return rc;
                rc = e1.toString().compareTo(e2.toString());
                if (rc != 0) return rc;
                return Integer.compare(e1.hashCode(), e2.hashCode());
            });
            Set<? extends Element> elements = docenv.getIncludedElements();
            for (ModuleElement me : ElementFilter.modulesIn(elements)) {
                addEnclosedElements(docenv, result, me);
            }
            for (PackageElement pe : ElementFilter.packagesIn(elements)) {
                ModuleElement mdle = docenv.getElementUtils().getModuleOf(pe);
                if (mdle != null)
                    addEnclosedElements(docenv, result, mdle);
                addEnclosedElements(docenv, result, pe);
            }
            for (TypeElement te : ElementFilter.typesIn(elements)) {
                addEnclosedElements(docenv, result, te);
            }
            return result;
        }

        void addEnclosedElements(DocletEnvironment docenv, Set<Element> result, Element e) {
            List<Element> elems = e.getEnclosedElements().stream()
                    .filter(el -> docenv.isIncluded(el))
                    .collect(Collectors.toList());
            result.addAll(elems);
            for (TypeElement t : ElementFilter.typesIn(elems)) {
                addEnclosedElements(docenv, result, t);
            }
        }

        @Override
        public Set<Doclet.Option> getSupportedOptions() {
            Option[] options = {
                new Option() {
                    private final List<String> someOption = Arrays.asList(
                            "-hasDocComments"
                    );

                    @Override
                    public int getArgumentCount() {
                        return 0;
                    }

                    @Override
                    public String getDescription() {
                        return "print disposition of doc comments on an element";
                    }

                    @Override
                    public Option.Kind getKind() {
                        return Option.Kind.STANDARD;
                    }

                    @Override
                    public List<String> getNames() {
                        return someOption;
                    }

                    @Override
                    public String getParameters() {
                        return "flag";
                    }

                    @Override
                    public boolean process(String opt, List<String> arguments) {
                        hasDocComments = true;
                        return true;
                    }
                }
            };
            return new HashSet<>(Arrays.asList(options));
        }

        @Override
        public void init(Locale locale, Reporter reporter) {}

        @Override
        public String getName() {
            return "ModulesTesterDoclet";
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }
    }
}
