/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8171355
 * @summary Test behavior of javax.lang.model.util.Elements.getOrigin.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.TestRunner
 * @build TestOrigin
 * @run main TestOrigin
 */

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.ModuleElement.OpensDirective;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.util.Elements;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.Attributes;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ClassWriter;
import com.sun.tools.classfile.Module_attribute;
import com.sun.tools.classfile.Module_attribute.ExportsEntry;
import com.sun.tools.classfile.Module_attribute.OpensEntry;
import com.sun.tools.classfile.Module_attribute.RequiresEntry;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class TestOrigin extends TestRunner {

    private final ToolBox tb;

    TestOrigin() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new TestOrigin().runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void testGeneratedConstr(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "package test; public class Test { private void test() { } }",
                          "class Dummy {}");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log;
        List<String> expected;

        //from source:
        log = new JavacTask(tb)
            .options("-processor", ListMembersAP.class.getName())
            .outdir(classes)
            .files(tb.findJavaFiles(src))
            .run()
            .writeAll()
            .getOutputLines(Task.OutputKind.STDOUT);

        expected = Arrays.asList(
                "<init>:MANDATED",
                "test:EXPLICIT");

        if (!expected.equals(log))
            throw new AssertionError("expected output not found: " + log);

        //from class:
        log = new JavacTask(tb)
            .options("-classpath", classes.toString(),
                     "-processorpath", System.getProperty("test.classes"),
                     "-processor", ListMembersAP.class.getName())
            .outdir(classes)
            .files(src.resolve("Dummy.java"))
            .run()
            .writeAll()
            .getOutputLines(Task.OutputKind.STDOUT);

        expected = Arrays.asList(
                "<init>:EXPLICIT",
                "test:EXPLICIT");

        if (!expected.equals(log))
            throw new AssertionError("expected output not found: " + log);
    }

    @SupportedAnnotationTypes("*")
    public static final class ListMembersAP extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (!roundEnv.processingOver())
                return false;

            Elements elements = processingEnv.getElementUtils();
            TypeElement test = elements.getTypeElement("test.Test");
            List<? extends Element> members = new ArrayList<>(test.getEnclosedElements());

            Collections.sort(members,
                             (e1, e2) -> e1.getSimpleName().toString().compareTo(e2.getSimpleName().toString()));

            for (Element el : members) {
                System.out.println(el.getSimpleName() + ":" + elements.getOrigin(el));
            }

            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();
        }

    }

    @Test
    public void testRepeatableAnnotations(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "package test; @A @A public class Test { }",
                          "package test;" +
                          "import java.lang.annotation.*;" +
                          "@Repeatable(Container.class)" +
                          "@Retention(RetentionPolicy.CLASS)" +
                          "@interface A {}",
                          "package test; @interface Container { A[] value(); }",
                          "class Dummy {}");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log;
        List<String> expected;

        //from source:
        log = new JavacTask(tb)
            .options("-processor", ListAnnotationsAP.class.getName())
            .outdir(classes)
            .files(tb.findJavaFiles(src))
            .run()
            .writeAll()
            .getOutputLines(Task.OutputKind.STDOUT);

        expected = Arrays.asList("test.Container:MANDATED");

        if (!expected.equals(log))
            throw new AssertionError("expected output not found: " + log);

        //from class:
        log = new JavacTask(tb)
            .options("-classpath", classes.toString(),
                     "-processorpath", System.getProperty("test.classes"),
                     "-processor", ListAnnotationsAP.class.getName())
            .outdir(classes)
            .files(src.resolve("Dummy.java"))
            .run()
            .writeAll()
            .getOutputLines(Task.OutputKind.STDOUT);

        expected = Arrays.asList("test.Container:EXPLICIT");

        if (!expected.equals(log))
            throw new AssertionError("expected output not found: " + log);
    }

    @SupportedAnnotationTypes("*")
    public static final class ListAnnotationsAP extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (!roundEnv.processingOver())
                return false;

            Elements elements = processingEnv.getElementUtils();
            TypeElement test = elements.getTypeElement("test.Test");

            for (AnnotationMirror am : test.getAnnotationMirrors()) {
                System.out.println(am.getAnnotationType() + ":" + elements.getOrigin(test, am));
            }

            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();
        }

    }

    @Test
    public void testModuleDirectives(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module m {}",
                          "package p1; class Test {}",
                          "package p2; class Test {}",
                          "package p3; class Test {}",
                          "package test; class Dummy {}");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log;
        List<String> expected;

        //from source:
        log = new JavacTask(tb)
            .options("-processor", ListModuleAP.class.getName())
            .outdir(classes)
            .files(tb.findJavaFiles(src))
            .run()
            .writeAll()
            .getOutputLines(Task.OutputKind.STDOUT);

        expected = Arrays.asList("REQUIRES:java.base:MANDATED");

        if (!expected.equals(log))
            throw new AssertionError("expected output not found: " + log);

        tb.writeJavaFiles(src,
                          "module m {" +
                          "    requires java.base;" +
                          "    requires java.compiler;" +
                          "    requires jdk.compiler;" +
                          "    exports p1;" +
                          "    exports p2;" +
                          "    exports p3;" +
                          "    opens p1;" +
                          "    opens p2;" +
                          "    opens p3;" +
                          "}");

        new JavacTask(tb)
            .outdir(classes)
            .files(src.resolve("module-info.java"))
            .run()
            .writeAll();

        Path moduleInfo = classes.resolve("module-info.class");
        ClassFile cf = ClassFile.read(moduleInfo);
        Module_attribute module = (Module_attribute) cf.getAttribute(Attribute.Module);

        RequiresEntry[] newRequires = new RequiresEntry[3];
        newRequires[0] = new RequiresEntry(module.requires[0].requires_index,
                                           Module_attribute.ACC_MANDATED,
                                           module.requires[0].requires_version_index);
        newRequires[1] = new RequiresEntry(module.requires[1].requires_index,
                                           Module_attribute.ACC_SYNTHETIC,
                                           module.requires[1].requires_version_index);
        newRequires[2] = module.requires[2];

        ExportsEntry[] newExports = new ExportsEntry[3];
        newExports[0] = new ExportsEntry(module.exports[0].exports_index,
                                         Module_attribute.ACC_MANDATED,
                                         module.exports[0].exports_to_index);
        newExports[1] = new ExportsEntry(module.exports[1].exports_index,
                                         Module_attribute.ACC_SYNTHETIC,
                                         module.exports[1].exports_to_index);
        newExports[2] = module.exports[2];

        OpensEntry[] newOpens = new OpensEntry[3];
        newOpens[0] = new OpensEntry(module.opens[0].opens_index,
                                     Module_attribute.ACC_MANDATED,
                                     module.opens[0].opens_to_index);
        newOpens[1] = new OpensEntry(module.opens[1].opens_index,
                                     Module_attribute.ACC_SYNTHETIC,
                                     module.opens[1].opens_to_index);
        newOpens[2] = module.opens[2];

        Module_attribute newModule = new Module_attribute(module.attribute_name_index,
                                                          module.module_name,
                                                          module.module_flags,
                                                          module.module_version_index,
                                                          newRequires,
                                                          newExports,
                                                          newOpens,
                                                          module.uses_index,
                                                          module.provides);
        Map<String, Attribute> newAttributesMap = new HashMap<>(cf.attributes.map);

        newAttributesMap.put(Attribute.Module, newModule);

        Attributes newAttributes = new Attributes(newAttributesMap);
        ClassFile newClassFile = new ClassFile(cf.magic,
                                               cf.minor_version,
                                               cf.major_version,
                                               cf.constant_pool,
                                               cf.access_flags,
                                               cf.this_class,
                                               cf.super_class,
                                               cf.interfaces,
                                               cf.fields,
                                               cf.methods,
                                               newAttributes);

        try (OutputStream out = Files.newOutputStream(moduleInfo)) {
            new ClassWriter().write(newClassFile, out);
        }

        //from class:
        log = new JavacTask(tb)
            .options("-processor", ListModuleAP.class.getName())
            .outdir(classes)
            .files(src.resolve("test").resolve("Dummy.java"))
            .run()
            .writeAll()
            .getOutputLines(Task.OutputKind.STDOUT);

        expected = Arrays.asList(
                "REQUIRES:java.base:MANDATED",
                "REQUIRES:java.compiler:SYNTHETIC",
                "REQUIRES:jdk.compiler:EXPLICIT",
                "EXPORTS:p1:MANDATED",
                "EXPORTS:p2:SYNTHETIC",
                "EXPORTS:p3:EXPLICIT",
                "OPENS:p1:MANDATED",
                "OPENS:p2:SYNTHETIC",
                "OPENS:p3:EXPLICIT");

        if (!expected.equals(log))
            throw new AssertionError("expected output not found: " + log);
    }

    @SupportedAnnotationTypes("*")
    public static final class ListModuleAP extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (!roundEnv.processingOver())
                return false;

            Elements elements = processingEnv.getElementUtils();
            ModuleElement m = elements.getModuleElement("m");

            for (Directive d : m.getDirectives()) {
                switch (d.getKind()) {
                    case REQUIRES:
                        RequiresDirective rd = (RequiresDirective) d;
                        System.out.println(rd.getKind() + ":" +
                                           rd.getDependency().getQualifiedName() + ":" +
                                           elements.getOrigin(m, rd));
                        break;
                    case EXPORTS:
                        ExportsDirective ed = (ExportsDirective) d;
                        System.out.println(ed.getKind() + ":" +
                                           ed.getPackage() + ":" +
                                           elements.getOrigin(m, ed));
                        break;
                    case OPENS:
                        OpensDirective od = (OpensDirective) d;
                        System.out.println(od.getKind() + ":" +
                                           od.getPackage() + ":" +
                                           elements.getOrigin(m, od));
                        break;
                }
            }

            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();
        }

    }

}
