/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary simple tests of module uses
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.ModuleBuilder ModuleTestBase
 * @run main ModuleVersion
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Module_attribute;
import toolbox.JavacTask;
import toolbox.Task.Expect;
import toolbox.Task.OutputKind;

public class ModuleVersion extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        ModuleVersion t = new ModuleVersion();
        t.runTests();
    }

    @Test
    public void testSetSingleModuleVersion(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "module m { }");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String version = "1.2.3.4";

        new JavacTask(tb)
            .options("--module-version", version)
            .outdir(classes)
            .files(findJavaFiles(src))
            .run()
            .writeAll();

        checkModuleVersion(classes.resolve("module-info.class"), version);
    }

    @Test
    public void testMultipleModuleVersions(Path base) throws Exception {
        Path src = base.resolve("src");
        Path m1 = src.resolve("m1");
        tb.writeJavaFiles(m1,
                "module m1 { }");
        Path m2 = src.resolve("m2");
        tb.writeJavaFiles(m2,
                "module m2 { }");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String version = "1.2.3.4";

        new JavacTask(tb)
            .options("--module-source-path", src.toString(),
                     "--module-version", version)
            .outdir(classes)
            .files(findJavaFiles(src))
            .run()
            .writeAll();

        checkModuleVersion(classes.resolve("m1").resolve("module-info.class"), version);
        checkModuleVersion(classes.resolve("m2").resolve("module-info.class"), version);

        String log = new JavacTask(tb, JavacTask.Mode.CMDLINE)
            .options("--module-source-path", src.toString(),
                     "--module-version", "b",
                     "-XDrawDiagnostics")
            .outdir(classes)
            .files(findJavaFiles(src))
            .run(Expect.FAIL)
            .writeAll()
            .getOutput(OutputKind.DIRECT);

        String expectedLog = "bad value for --module-version option: 'b'";

        if (!log.contains(expectedLog)) {
            throw new AssertionError("Incorrect log: " + log);
        }
    }

    private void checkModuleVersion(Path classfile, String version) throws IOException, ConstantPoolException {
        ClassFile cf = ClassFile.read(classfile);

        Module_attribute moduleAttribute = (Module_attribute) cf.attributes.get(Attribute.Module);

        if (moduleAttribute == null) {
            throw new AssertionError("Version attribute missing!");
        }

        String actualVersion = cf.constant_pool.getUTF8Value(moduleAttribute.module_version_index);

        if (!version.equals(actualVersion)) {
            throw new AssertionError("Incorrect version in the classfile: " + actualVersion);
        }
    }

}
