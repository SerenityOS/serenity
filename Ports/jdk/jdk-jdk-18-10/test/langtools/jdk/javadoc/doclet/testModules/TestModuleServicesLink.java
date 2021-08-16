/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8185151 8196200 8261976
 * @summary test that navigation summary links are not linked when there are no dependencies
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.javadoc/jdk.javadoc.internal.api
 *          jdk.javadoc/jdk.javadoc.internal.tool
 * @library ../../lib /tools/lib
 * @build toolbox.ToolBox toolbox.ModuleBuilder javadoc.tester.*
 * @run main TestModuleServicesLink
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import javadoc.tester.JavadocTester;
import toolbox.ModuleBuilder;
import toolbox.ToolBox;

public class TestModuleServicesLink extends JavadocTester {

    public final ToolBox tb;
    public static void main(String... args) throws Exception {
        TestModuleServicesLink  tester = new TestModuleServicesLink ();
        tester.runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    public TestModuleServicesLink () {
        tb = new ToolBox();
    }

    @Test
    public void checkNavbarWithServices1(Path base) throws Exception {
        ModuleBuilder mb = new ModuleBuilder(tb, "m")
                .comment("module m.\n@uses p1.A")
                .uses("p1.A")
                .uses("p1.B")
                .exports("p1")
                .classes("package p1; public class A {}")
                .classes("package p1; public class B {}");
        mb.write(base);

        javadoc("-d", base.toString() + "/out",
                "-quiet",
                "--module-source-path", base.toString(),
                "--module", "m");
        checkExit(Exit.OK);

        checkOutput("m/module-summary.html", true,
                """
                    <li><a href="#module-description">Description</a>&nbsp;|&nbsp;</li>
                    <li>Modules&nbsp;|&nbsp;</li>
                    <li><a href="#packages-summary">Packages</a>&nbsp;|&nbsp;</li>
                    <li><a href="#services-summary">Services</a></li>""");

    }

    @Test
    public void checkNavbarWithServices2(Path base) throws Exception {
        ModuleBuilder mb = new ModuleBuilder(tb, "m")
                        .comment("module m.\n@provides p1.A")
                        .provides("p1.A", "p1.B")
                        .exports("p1")
                        .classes("package p1; public interface A {}")
                        .classes("package p1; public class B implements A {}");
        mb.write(base);

        javadoc("-d", base.toString() + "/out",
                "-quiet",
                "--module-source-path", base.toString(),
                "--module", "m");
        checkExit(Exit.OK);

        checkOutput("m/module-summary.html", true,
                """
                    <li><a href="#module-description">Description</a>&nbsp;|&nbsp;</li>
                    <li>Modules&nbsp;|&nbsp;</li>
                    <li><a href="#packages-summary">Packages</a>&nbsp;|&nbsp;</li>
                    <li><a href="#services-summary">Services</a></li>""");

    }

    @Test
    public void checkNavbarWithoutServices(Path base) throws Exception {
        ModuleBuilder mb = new ModuleBuilder(tb, "m")
                .exports("p1")
                .classes("package p1; public class A {}")
                .classes("package p1; public class B {}");
        mb.write(base);

        javadoc("-d", base.toString() + "/out",
                "-quiet",
                "--module-source-path", base.toString(),
                "--module", "m");
        checkExit(Exit.OK);

        checkOutput("m/module-summary.html", true,
                """
                    <li>Description&nbsp;|&nbsp;</li>
                    <li>Modules&nbsp;|&nbsp;</li>
                    <li><a href="#packages-summary">Packages</a>&nbsp;|&nbsp;</li>
                    <li>Services</li>""");
    }

}
