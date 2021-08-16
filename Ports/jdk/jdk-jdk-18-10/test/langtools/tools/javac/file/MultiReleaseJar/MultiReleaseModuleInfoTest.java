/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8156568
 * @summary Update javac to support compiling against a modular multi-release JAR
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask
 * @run main MultiReleaseModuleInfoTest
 */

import java.nio.file.Paths;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;


public class MultiReleaseModuleInfoTest {

    private final String service_mi =
            "module service {\n" +
            "}\n";

    private final String service =
            "package service;\n" +
            "public class Service {\n" +
            "}\n";

    private final String service_mi9 =
            "module service {\n" +
            "    requires java.desktop;\n" +
            "}\n";

    private final String service9 =
            "package service;\n" +
            "public class Service {\n" +
            "}\n";

    private final String client_mi =
            "module client {\n" +
            "    requires service;\n" +
            "}\n";

    private final String manifest =
        "Manifest-Version: 1.0\n" +
        "Multi-Release: true\n";

    private final ToolBox tb = new ToolBox();

    public static void main(String [] args) throws Exception {
        new MultiReleaseModuleInfoTest().runTest();
    }

    private void runTest() throws Exception {
        tb.createDirectories("classes", "classes/META-INF/versions/9");
        new JavacTask(tb)
                .outdir("classes")
                .sources(service_mi, service)
                .run();
        new JavacTask(tb)
                .outdir("classes/META-INF/versions/9")
                .sources(service_mi9, service9)
                .run();
        new JarTask(tb, "multi-release.jar")
                .manifest(manifest)
                .baseDir("classes")
                .files("module-info.class", "service/Service.class",
                       "META-INF/versions/9/module-info.class",
                       "META-INF/versions/9/service/Service.class")
                .run();
        tb.cleanDirectory(Paths.get("classes"));

        tb.writeFile("module-info.java", client_mi);
        Task.Result result = new JavacTask(tb)
                .outdir("classes")
                .options("-processor", VerifyRequires.class.getName(),
                               "--module-path", "multi-release.jar")
                .files("module-info.java")
                .run();
        result.writeAll();
        tb.deleteFiles("module-info.java");

        tb.deleteFiles(
                "multi-release.jar",
                "classes/module-info.class",
                "classes"
        );
    }

    @SupportedAnnotationTypes("*")
    public static final class VerifyRequires extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> u, RoundEnvironment r) {
            ModuleElement sm = processingEnv.getElementUtils().getModuleElement("service");
            if (sm == null) {
                throw new AssertionError("Cannot find the service module!");
            }
            boolean foundjd = false;
            for (RequiresDirective rd : ElementFilter.requiresIn(sm.getDirectives())) {
                foundjd |= rd.getDependency().getQualifiedName().contentEquals("java.desktop");
            }
            if (!foundjd) {
                throw new AssertionError("Missing dependency on java desktop module!");
            }
            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }
    }
}
