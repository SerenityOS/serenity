/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8182450
 * @summary Test model behavior when a completing a broken module-info.
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.processing
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.ModuleBuilder ModuleTestBase
 * @run main BrokenModulesTest
 */

import java.nio.file.Path;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import com.sun.tools.javac.api.JavacTaskImpl;

public class BrokenModulesTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new BrokenModulesTest().runTests();
    }

    List<String> jlObjectList = List.of("java.lang.Object");
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    @Test
    public void testBrokenModuleInfoModuleSourcePath(Path base) throws Exception {
        Path msp = base.resolve("msp");
        Path ma = msp.resolve("ma");
        tb.writeJavaFiles(ma,
                          "module ma { requires not.available; exports api1; }",
                          "package api1; public interface Api { }");
        Path out = base.resolve("out");
        tb.createDirectories(out);

        List<String> opts = List.of("--module-source-path", msp.toString(),
                                    "--add-modules", "ma");
        JavacTaskImpl task = (JavacTaskImpl) compiler.getTask(null, null, null, opts, jlObjectList, null);

        task.enter();

        task.getElements().getModuleElement("java.base");
    }

    @Test
    public void testSystem(Path base) throws Exception {
        Path jdkCompiler = base.resolve("jdk.compiler");
        tb.writeJavaFiles(jdkCompiler,
                          "module jdk.compiler { requires not.available; }");
        Path out = base.resolve("out");
        tb.createDirectories(out);

        List<String> opts = List.of("--patch-module", "jdk.compiler=" + jdkCompiler.toString(),
                                    "--add-modules", "jdk.compiler");
        JavacTaskImpl task = (JavacTaskImpl) compiler.getTask(null, null, null, opts, jlObjectList, null);

        task.enter();

        task.getElements().getModuleElement("java.base");
    }

    @Test
    public void testParseError(Path base) throws Exception {
        Path msp = base.resolve("msp");
        Path ma = msp.resolve("ma");
        tb.writeJavaFiles(ma,
                          "broken module ma { }",
                          "package api1; public interface Api { }");
        Path out = base.resolve("out");
        tb.createDirectories(out);

        List<String> opts = List.of("--module-source-path", msp.toString(),
                                    "--add-modules", "ma");
        JavacTaskImpl task = (JavacTaskImpl) compiler.getTask(null, null, null, opts, jlObjectList, null);

        task.analyze();

        task.getElements().getModuleElement("java.base");
    }

}
