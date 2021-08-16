/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8178067 8192007 8182765 8184205
 * @summary tests the module's services, such as provides and uses
 * @modules jdk.javadoc/jdk.javadoc.internal.api
 *          jdk.javadoc/jdk.javadoc.internal.tool
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @library ../../lib /tools/lib
 * @build toolbox.ToolBox toolbox.ModuleBuilder javadoc.tester.*
 * @run main TestModuleServices
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javadoc.tester.JavadocTester;
import toolbox.ModuleBuilder;
import toolbox.ToolBox;

public class TestModuleServices extends JavadocTester {

    public final ToolBox tb;
    public static void main(String... args) throws Exception {
        TestModuleServices tester = new TestModuleServices();
        tester.runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    public TestModuleServices() {
        tb = new ToolBox();
    }

    @Test
    public void checkModuleServicesDescription(Path base) throws Exception {
        Path src = Files.createDirectories(base.resolve("src"));
        ModuleBuilder mb = new ModuleBuilder(tb, "moduleService")
                .comment("This module exports a package containing the declaration of a service type.")
                .exports("pkgService")
                .classes("/**A Package that has a service.*/ package pkgService;")
                .classes("""
                    package pkgService; /**A service Interface for service providers.*/ public interface Service {
                        /**
                         * A test method for the service.
                         */
                        void testMethod1();
                        /**
                         * Another test method for the service.
                         */
                        void testMethod2();
                    }""");
        mb.write(src);
        mb = new ModuleBuilder(tb, "moduleServiceProvider")
                .comment("""
                    This module provides an implementation of a service.
                    @provides pkgService.Service Provides a service whose name is ServiceProvider.""")
                .requires("moduleService")
                .provides("pkgService.Service", "pkgServiceProvider.ServiceProvider")
                .classes("/**A Package that has a service provider.*/ package pkgServiceProvider;")
                .classes("""
                    package pkgServiceProvider;
                    public class ServiceProvider implements pkgService.Service {
                        /**
                         * {@inheritDoc}
                         */
                        public void testMethod1() {}
                        /**
                         * This is an internal implementation so the documentation will not be seen.
                         */
                        public void testMethod2() {}
                    }""");
        mb.write(src);
        mb = new ModuleBuilder(tb, "moduleServiceUser")
                .comment("""
                    This module uses a service defined in another module.
                    @uses pkgService.Service If no other provider is found, a default internal implementation will be used.""")
                .requires("moduleService")
                .uses("pkgService.Service")
                .classes("/**A Package that has a service user.*/ package pkgServiceUser;")
                .classes("""
                    package pkgServiceUser;
                    /**
                     * A service user class.
                     */
                    public class ServiceUser {
                    }""");
        mb.write(src);
        mb = new ModuleBuilder(tb, "moduleServiceUserNoDescription")
                .comment("""
                    This is another module that uses a service defined in another module.
                    @uses pkgService.Service""")
                .requires("moduleService")
                .uses("pkgService.Service")
                .classes("/**A Package that has a service user with no description.*/ package pkgServiceUserNoDescription;")
                .classes("""
                    package pkgServiceUserNoDescription;
                    /**
                     * A service user class.
                     */
                    public class ServiceUserNoDescription {
                    }""");
        mb.write(src);

        javadoc("-d", base.resolve("out").toString(),
                "-quiet",
                "-noindex",
                "--module-source-path", src.toString(),
                "--module", "moduleService,moduleServiceProvider,moduleServiceUser,moduleServiceUserNoDescription",
                "pkgService", "moduleServiceProvider/pkgServiceProvider", "moduleServiceUser/pkgServiceUser",
                "moduleServiceUserNoDescription/pkgServiceUserNoDescription");
        checkExit(Exit.OK);

        checkOutput("moduleServiceProvider/module-summary.html", true,
                """
                    <div class="col-first even-row-color"><a href="../moduleService/pkgService/Service.ht\
                    ml" title="interface in pkgService">Service</a></div>
                    <div class="col-last even-row-color">
                    <div class="block">Provides a service whose name is ServiceProvider.</div>""");
        checkOutput("moduleServiceUser/module-summary.html", true,
                """
                    <div class="col-first even-row-color"><a href="../moduleService/pkgService/Service.ht\
                    ml" title="interface in pkgService">Service</a></div>
                    <div class="col-last even-row-color">
                    <div class="block">If no other provider is found, a default internal implementat\
                    ion will be used.</div>""");
        checkOutput("moduleServiceUserNoDescription/module-summary.html", true,
                """
                    <div class="col-first even-row-color"><a href="../moduleService/pkgService/Service.ht\
                    ml" title="interface in pkgService">Service</a></div>
                    <div class="col-last even-row-color">
                    <div class="block">A service Interface for service providers.</div>""");
        checkOutput("moduleServiceProvider/module-summary.html", false,
                "A service Interface for service providers.");
        checkOutput("moduleServiceUser/module-summary.html", false,
                "A service Interface for service providers.");
    }

    @Test
    public void checkUsesNoApiTagModuleModeDefault(Path base) throws Exception {
        ModuleBuilder mb = new ModuleBuilder(tb, "m")
                .comment("module m.\n@provides p1.A abc") // bogus tag
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

        checkOutput("m/module-summary.html", false,
                "<h2>Services</h2>");
    }

    @Test
    public void checkUsesNoApiTagModuleModeAll(Path base) throws Exception {
        ModuleBuilder mb = new ModuleBuilder(tb, "m")
                .uses("p1.A")
                .uses("p1.B")
                .exports("p1")
                .classes("package p1; public class A {}")
                .classes("package p1; public class B {}");
        mb.write(base);

        javadoc("-d", base.toString() + "/out",
                "-quiet",
                "--show-module-contents", "all",
                "--module-source-path", base.toString(),
                "--module", "m");
        checkExit(Exit.OK);

        checkOutput("m/module-summary.html", true,
                "<h2>Services</h2>");

        checkOutput("m/module-summary.html", true,
                """
                    <div class="caption"><span>Uses</span></div>
                    <div class="details-table two-column-summary">
                    <div class="table-header col-first">Type</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-first even-row-color"><a href="p1/A.html" title="class in p1">A</a></div>
                    <div class="col-last even-row-color">&nbsp;</div>
                    <div class="col-first odd-row-color"><a href="p1/B.html" title="class in p1">B</a></div>
                    <div class="col-last odd-row-color">&nbsp;</div>
                    </div>
                    """);

    }

    @Test
    public void checkUsesWithApiTagModuleModeDefault(Path base) throws Exception {
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
                "<h2>Services</h2>");

        checkOutput("m/module-summary.html", true,
                """
                    <div class="caption"><span>Uses</span></div>
                    <div class="details-table two-column-summary">
                    <div class="table-header col-first">Type</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-first even-row-color"><a href="p1/A.html" title="class in p1">A</a></div>
                    <div class="col-last even-row-color">&nbsp;</div>
                    </div>
                    """);

    }

    @Test
    public void checkProvidesNoApiTagModuleModeDefault(Path base) throws Exception {
        ModuleBuilder mb = new ModuleBuilder(tb, "m")
                .comment("module m.\n@uses p1.A")
                .provides("p1.A", "p1.B")
                .exports("p1")
                .classes("package p1; public interface A {}")
                .classes("package p1; public class B implements A {}")
                .provides("p2.A", "p2.B")
                .exports("p2")
                .classes("package p2; public interface A {}")
                .classes("package p2; public class B implements A {}");
        mb.write(base);

        javadoc("-d", base.toString() + "/out",
                "-quiet",
                "--module-source-path", base.toString(),
                "--module", "m");

        checkExit(Exit.OK);

        checkOutput("m/module-summary.html", false,
                "<h2>Services</h2>");
    }

    @Test
    public void checkProvidesNoApiTagModuleModeAll(Path base) throws Exception {
        ModuleBuilder mb = new ModuleBuilder(tb, "m")
                .comment("module m.\n@uses p1.A") // bogus uses tag
                .provides("p1.A", "p1.B")
                .exports("p1")
                .classes("package p1; public interface A {}")
                .classes("package p1; public class B implements A {}")
                .provides("p2.A", "p2.B")
                .exports("p2")
                .classes("package p2; public interface A {}")
                .classes("package p2; public class B implements A {}");
        mb.write(base);

        javadoc("-d", base.toString() + "/out",
                "-quiet",
                "--show-module-contents", "all",
                "--module-source-path", base.toString(),
                "--module", "m");

        checkExit(Exit.OK);

        checkOutput("m/module-summary.html", true,
                "<h2>Services</h2>");

        checkOutput("m/module-summary.html", true,
                """
                    <div class="caption"><span>Provides</span></div>
                    <div class="details-table two-column-summary">
                    <div class="table-header col-first">Type</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-first even-row-color"><a href="p1/A.html" title="interface in p1">A</a></div>
                    <div class="col-last even-row-color">&nbsp;<br>(<span class="implementation-label">Implementation(s):</span>&nbsp;<a href="p1/B.html" title="class in p1">B</a>)</div>
                    <div class="col-first odd-row-color"><a href="p2/A.html" title="interface in p2">A</a></div>
                    <div class="col-last odd-row-color">&nbsp;<br>(<span class="implementation-label">Implementation(s):</span>&nbsp;<a href="p2/B.html" title="class in p2">B</a>)</div>
                    </div>
                    """);

    }

    @Test
    public void checkProvidesWithApiTagModuleModeDefault(Path base) throws Exception {
        ModuleBuilder mb = new ModuleBuilder(tb, "m")
                .comment("module m.\n@provides p1.A abc")
                .provides("p1.A", "p1.B")
                .exports("p1")
                .classes("package p1; public interface A {}")
                .classes("package p1; public class B implements A {}")
                .provides("p2.A", "p2.B")
                .exports("p2")
                .classes("package p2; public interface A {}")
                .classes("package p2; public class B implements A {}");
        mb.write(base);

        javadoc("-d", base.toString() + "/out",
                "-quiet",
                "--module-source-path", base.toString(),
                "--module", "m");

        checkExit(Exit.OK);

        checkOutput("m/module-summary.html", true,
                "<h2>Services</h2>");

        checkOutput("m/module-summary.html", true,
                """
                    <div class="caption"><span>Provides</span></div>
                    <div class="details-table two-column-summary">
                    <div class="table-header col-first">Type</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-first even-row-color"><a href="p1/A.html" title="interface in p1">A</a></div>
                    <div class="col-last even-row-color">
                    <div class="block">abc</div>
                    </div>
                    </div>
                    """);

    }

    @Test
    public void checkUsesProvidesWithApiTagsModeDefault(Path base) throws Exception {
        ModuleBuilder mb = new ModuleBuilder(tb, "m")
                .comment("""
                    module m.
                    @provides p1.A abc
                    @uses p2.B def""")
                .provides("p1.A", "p1.B")
                .exports("p1")
                .classes("package p1; public interface A {}")
                .classes("package p1; public class B implements A {}")
                .provides("p2.A", "p2.B")
                .uses("p2.B")
                .exports("p2")
                .classes("package p2; public interface A {}")
                .classes("package p2; public class B implements A {}");
        mb.write(base);

        javadoc("-d", base.toString() + "/out",
                "-quiet",
                "--module-source-path", base.toString(),
                "--module", "m");

        checkExit(Exit.OK);

        checkOutput("m/module-summary.html", true,
                "<h2>Services</h2>");

        checkOutput("m/module-summary.html", true,
                """
                    <div class="caption"><span>Provides</span></div>
                    <div class="details-table two-column-summary">
                    <div class="table-header col-first">Type</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-first even-row-color"><a href="p1/A.html" title="interface in p1">A</a></div>
                    <div class="col-last even-row-color">
                    <div class="block">abc</div>
                    </div>
                    </div>
                    """,
                """
                    <div class="caption"><span>Uses</span></div>
                    <div class="details-table two-column-summary">
                    <div class="table-header col-first">Type</div>
                    <div class="table-header col-last">Description</div>
                    <div class="col-first even-row-color"><a href="p2/B.html" title="class in p2">B</a></div>
                    <div class="col-last even-row-color">
                    <div class="block">def</div>
                    </div>
                    </div>
                    """);

    }

}
