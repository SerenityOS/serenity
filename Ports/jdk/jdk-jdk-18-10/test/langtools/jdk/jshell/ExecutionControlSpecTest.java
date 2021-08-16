/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8168615
 * @summary Test ExecutionControlProvider specs can load user ExecutionControlProviders
 * with direct maps.
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 *          jdk.jshell/jdk.jshell.execution
 *          jdk.jshell/jdk.jshell.spi
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask
 * @build KullaTesting Compiler
 * @run testng ExecutionControlSpecTest
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

public class ExecutionControlSpecTest extends KullaTesting {

    ClassLoader ccl;

    @BeforeMethod
    @Override
    public void setUp() {
        String mod = "my.ec";
        String pkg = "package my.ec;\n";
        Compiler compiler = new Compiler();
        Path modDir = Paths.get("mod");
        compiler.compile(modDir,
                pkg +
                "public class PrefixingExecutionControl extends jdk.jshell.execution.LocalExecutionControl {\n" +
                "    @Override\n" +
                "    public String invoke(String className, String methodName)\n" +
                "            throws RunException, InternalException, EngineTerminationException {\n" +
                "        return \"Blah:\" + super.invoke(className, methodName);\n" +
                "    }\n" +
                "}\n",
                pkg +
                "public class PrefixingExecutionControlProvider implements jdk.jshell.spi.ExecutionControlProvider {\n" +
                "    @Override\n" +
                "    public String name() {\n" +
                "        return \"prefixing\";\n" +
                "    }\n" +
                "    @Override\n" +
                "    public jdk.jshell.spi.ExecutionControl generate(jdk.jshell.spi.ExecutionEnv env,\n" +
                "            java.util.Map<String, String> parameters) {\n" +
                "        return new PrefixingExecutionControl();\n" +
                "    }\n" +
                "}\n",
                "module my.ec {\n" +
                "    requires transitive jdk.jshell;\n" +
                "    provides jdk.jshell.spi.ExecutionControlProvider\n" +
                "        with my.ec.PrefixingExecutionControlProvider;\n" +
                " }");
        Path modPath = compiler.getPath(modDir);
        ccl = createAndRunFromModule(mod, modPath);

        setUp(builder -> builder.executionEngine("prefixing"));
    }

    @AfterMethod
    @Override
    public void tearDown() {
        super.tearDown();
        Thread.currentThread().setContextClassLoader(ccl);
    }

    @Test
    public void testPrefix() {
        assertEval("43;", "Blah:43");
    }

}
