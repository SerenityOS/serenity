/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8207954
 * @summary Verify that CreateSymbols can handle classfiles from the current release.
 * @library /tools/lib /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api:+open
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.jvm:+open
 *          jdk.compiler/com.sun.tools.javac.util:+open
 *          jdk.jdeps/com.sun.tools.classfile:+open
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.Task
 * @run main CanHandleClassFilesTest
 */

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import javax.lang.model.SourceVersion;

import javax.tools.StandardLocation;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class CanHandleClassFilesTest {

    public static void main(String... args) throws Exception {
        new CanHandleClassFilesTest().run();
    }

    void run() throws Exception {
        var testSrc = Paths.get(System.getProperty("test.src", "."));
        var targetDir = Paths.get(".");
        Path createSymbols = null;
        Path includeList = null;
        for (Path d = testSrc; d != null; d = d.getParent()) {
            if (Files.exists(d.resolve("TEST.ROOT"))) {
                d = d.getParent().getParent();
                Path test = d.resolve("make/langtools/src/classes/build/tools/symbolgenerator/CreateSymbols.java");
                if (Files.exists(test)) {
                    createSymbols = test;
                    includeList = d.resolve("make/data/symbols/include.list");
                    break;
                }
            }
        }
        if (createSymbols == null || includeList == null || !Files.isReadable(includeList)) {
            System.err.println("Warning: sources not found, test skipped.");
            return ;
        }
        try (ToolBox.MemoryFileManager mfm = new ToolBox.MemoryFileManager()) {
            ToolBox tb = new ToolBox();
            new JavacTask(tb)
              .options("--add-exports", "jdk.jdeps/com.sun.tools.classfile=ALL-UNNAMED",
                       "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                       "--add-exports", "jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED",
                       "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                       "--add-modules", "jdk.jdeps")
              .files(createSymbols)
              .fileManager(mfm)
              .run()
              .writeAll();

            ClassLoader cl = new ClassLoader() {
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    byte[] data = mfm.getFileBytes(StandardLocation.CLASS_OUTPUT, name);
                    if (data != null) {
                        return defineClass(name, data, 0, data.length);
                    } else {
                        throw new ClassNotFoundException(name);
                    }
                }
            };

            // open the non-exported packages needed by CreateSymbols to its module
            Module targetModule = cl.getUnnamedModule();
            Stream.of("jdk.compiler/com.sun.tools.javac.api",
                      "jdk.compiler/com.sun.tools.javac.jvm",
                      "jdk.compiler/com.sun.tools.javac.util",
                      "jdk.jdeps/com.sun.tools.classfile")
                    .forEach(p -> open(p, targetModule));

            var createSymbolsClass = Class.forName("build.tools.symbolgenerator.CreateSymbols", false, cl);
            var main = createSymbolsClass.getMethod("main", String[].class);
            var symbols = targetDir.resolve("symbols");
            var systemModules = targetDir.resolve("system-modules");

            try (Writer w = Files.newBufferedWriter(symbols)) {}
            try (Writer w = Files.newBufferedWriter(systemModules)) {}

            main.invoke(null,
                        (Object) new String[] {"build-description-incremental",
                                               symbols.toAbsolutePath().toString(),
                                               includeList.toAbsolutePath().toString()});

            main.invoke(null,
                        (Object) new String[] {"build-ctsym",
                                               "does-not-exist",
                                               symbols.toAbsolutePath().toString(),
                                               targetDir.resolve("ct.sym").toAbsolutePath().toString(),
                                               Long.toString(System.currentTimeMillis() / 1000),
                                               "" + SourceVersion.latest().ordinal(),
                                               systemModules.toAbsolutePath().toString()});
        }
    }

    void open(String moduleAndPackage, Module target) {
        String[] s = moduleAndPackage.split("/");
        var moduleName = s[0];
        var packageName = s[1];
        ModuleLayer.boot().findModule(moduleName).orElseThrow().addOpens(packageName, target);
    }

}
