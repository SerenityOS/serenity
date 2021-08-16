/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8159305
 * @summary Test modules with packages and subpackages filtering
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.api
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.TestRunner
 * @run main PackageOptions
 */

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import toolbox.*;

public class PackageOptions extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new PackageOptions().runTests();
    }

    @Test
    public void testExportedNonQualifiedPackagesLegacyMode(Path base)  throws Exception {
        execTask("--module-source-path", createSources(base.resolve("src")),
                "--add-modules", "m1",
                "m1pub");

        checkModulesNotSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesSpecified("m1pub");
        checkPackagesIncluded("m1pub");
    }

    @Test
    public void testExportedQualifiedPackagesLegacyMode(Path base)  throws Exception {
        execTask("--module-source-path", createSources(base.resolve("src")),
                "--add-modules", "m1",
                "m1/m1pub");

        checkModulesNotSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesSpecified("m1pub");
        checkPackagesIncluded("m1pub");
    }

    @Test
    public void testNonExportedQualifedPackagesLegacyMode(Path base)  throws Exception {
        execTask("--module-source-path", createSources(base.resolve("src")),
                "--add-modules", "m1",
                "m1/m1pro.pro1" /* not exported, therefore qualify with module */);

        checkModulesNotSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesSpecified("m1pro.pro1");
        checkPackagesIncluded("m1pro.pro1");
        checkPackagesNotIncluded("m1pro.pro2");
        checkPackagesNotIncluded("m1pub");
    }

    @Test
    public void testTypesLegacyMode(Path base) throws Exception {
        Path srcPath = base.resolve("src");
        Path typPath = srcPath.resolve("m1/m1pub/A.java");
        execTask("--module-source-path", createSources(srcPath),
                "--add-modules", "m1",
                typPath.toString());
        checkModulesNotSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesIncluded("m1pub");
        checkPackagesNotIncluded("m1pro");
        checkTypesSpecified("m1pub.A");
        checkTypesIncluded("m1pub.A");
        checkTypesNotIncluded("m1pub.B");
        checkTypesNotIncluded("m1pub.C");
    }

    @Test
    public void testSubclassedTypesLegacyMode(Path base) throws Exception {
        Path srcPath = base.resolve("src");
        Path typPath = srcPath.resolve("m1/m1pub/B.java");
        execTask("--module-source-path", createSources(srcPath),
                "--add-modules", "m1",
                typPath.toString());
        checkModulesNotSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesIncluded("m1pub");
        checkPackagesNotIncluded("m1pro");
        checkTypesSpecified("m1pub.B");
        checkTypesIncluded("m1pub.B");
        checkTypesNotIncluded("m1pub.A");
        checkTypesNotIncluded("m1pub.C");
    }

    @Test
    public void testDefaultPackages(Path base) throws Exception {
        execTask("--module-source-path", createSources(base.resolve("src")),
                "--module", "m1");

        checkModulesSpecified("m1");

        checkPackagesIncluded("m1pub", "m1pub.pub1", "m1pub.pub1.pub11",
                              "m1pub.pub1.pub12", "m1pub.pub2.pub21");
    }

    @Test
    public void testEmptyPackageDirectory(Path base) throws Exception {
        Path src = base.resolve("src");
        createSources(src);

        // need an empty package directory, to check whether
        // the behavior of subpackage and package
        Path pkgdir = src.resolve("m1/m1pro/");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pkgdir, "*.java")) {
            for (Path entry : stream) {
                Files.deleteIfExists(entry);
            }
        } catch (DirectoryIteratorException ex) {
            // I/O error encounted during the iteration
            throw ex.getCause();
        }
        execTask("--module-source-path", src.toString(),
                "-subpackages", "m1/m1pro");

        checkPackagesSpecified("m1pro", "m1pro.pro1", "m1pro.pro2");

        // empty package directory should cause an error
        execNegativeTask("--module-source-path", src.toString(),
                         "m1/m1pro");

    }

    @Test
    public void testExportedQualifiedSubpackageWithMultipleModules(Path base) throws Exception {
        execTask("--module-source-path", createSources(base.resolve("src"), 2),
                "--module", "m1",
                "-subpackages", "m1/m1pro.pro1:m1/m1pro.pro2:m2/m2pub.pub1");

        checkModulesSpecified("m1");
        checkPackagesSpecified("m1pro", "m1pro.pro2");
        checkPackagesSpecified("m2pub.pub1");

        checkPackagesIncluded("m1pub", "m1pub.pub1", "m1pub.pub1.pub11",
                              "m1pub.pub1.pub12", "m1pub.pub2.pub21");
        checkPackagesIncluded("m2pub.pub1");
    }

    @Test
    public void testUnexportedUnqualifiedSubpackages(Path base) throws Exception {
        execNegativeTask("--module-source-path", createSources(base.resolve("src")),
                         "--module", "m1",
                         "-subpackages", "m1pub.pub1:pro");

        assertMessagePresent("error: No source files for package pro");
    }

    @Test
    public void testUnexportedQualifiedPackage(Path base) throws Exception {
        execTask("--module-source-path", createSources(base.resolve("src")),
                "--module", "m1",
                "m1/m1pro");

        checkModulesSpecified("m1");
        checkPackagesSpecified("m1pro");

        checkPackagesIncluded("m1pub", "m1pub.pub1", "m1pub.pub1.pub11",
                              "m1pub.pub1.pub12", "m1pub.pub2.pub21");

        checkTypesIncluded("m1pro.L");
    }

    @Test
    public void testUnexportedQualifiedSubpackage(Path base) throws Exception {
        execTask("--module-source-path", createSources(base.resolve("src")),
                "--module", "m1",
                "-subpackages", "m1/m1pro");

        checkModulesSpecified("m1");
        checkPackagesSpecified("m1pro", "m1pro.pro1", "m1pro.pro2");

        checkPackagesIncluded("m1pub", "m1pub.pub1", "m1pub.pub1.pub11",
                              "m1pub.pub1.pub12", "m1pub.pub2.pub21");

        checkTypesIncluded("m1pro.L", "m1pro.pro1.M", "m1pro.pro2.O");
    }

    @Test
    public void testUnexportedQualifiedSubpackageExcludeQualified(Path base) throws Exception {
        execTask("--module-source-path", createSources(base.resolve("src")),
                "--module", "m1",
                "-subpackages", "m1/m1pro",
                "-exclude", "m1/m1pro.pro1.pro11:m1/m1pro.pro2.pro21");

        checkModulesSpecified("m1");
        checkPackagesSpecified("m1pro", "m1pro.pro1", "m1pro.pro2");

        checkPackagesIncluded("m1pub", "m1pub.pub1", "m1pub.pub1.pub11",
                              "m1pub.pub1.pub12", "m1pub.pub2.pub21");

        checkTypesIncluded("m1pro.L", "m1pro.pro1.M", "m1pro.pro2.O");
        checkPackagesNotSpecified(".*pro11.*", ".*pro21.*");
    }

    @Test
    public void testUnexportedQualifiedSubpackageExcludeUnqualified(Path base) throws Exception {
        execTask("--module-source-path", createSources(base.resolve("src")),
                "--module", "m1",
                "-subpackages", "m1/m1pro",
                "-exclude", "m1pro.pro1.pro11:m1pro.pro2.pro21");

        checkModulesSpecified("m1");
        checkPackagesSpecified("m1pro", "m1pro.pro1", "m1pro.pro2");

        checkPackagesIncluded("m1pub", "m1pub.pub1", "m1pub.pub1.pub11",
                              "m1pub.pub1.pub12", "m1pub.pub2.pub21");

        checkTypesIncluded("m1pro.L", "m1pro.pro1.M", "m1pro.pro2.O");
        checkPackagesNotSpecified(".*pro11.*", ".*pro21.*");
    }

    @Test
    public void testUnexportedQualifiedSubpackages(Path base) throws Exception {
        execTask("--module-source-path", createSources(base.resolve("src")),
                "--module", "m1",
                "-subpackages", "m1/m1pro.pro1:m1/m1pro.pro2");

        checkModulesSpecified("m1");
        checkPackagesSpecified("m1pro.pro1.pro11");

        checkPackagesIncluded("m1pub", "m1pub.pub1", "m1pub.pub1.pub11",
                               "m1pub.pub1.pub12", "m1pub.pub2.pub21");
        checkTypesIncluded("m1pro.pro1.pro11.N", "m1pro.pro2.pro21.P");
        checkTypesNotIncluded("m1pro.L");
    }

    String createSources(Path src) throws IOException {
        return createSources0(src, 1);
    }

    String createSources(Path src, int n) throws IOException {
        for (int i = 1 ; i <= n ; i++) {
            createSources0(src, i);
        }
        return src.toString();
    }

    String createSources0(Path src, int n) throws IOException {
        String mn = "m" + n;
        String pn = "package " + mn;

        ModuleBuilder mb1 = new ModuleBuilder(tb, mn);
        mb1.comment("The module #" + n)
                .classes(pn + "pub; /** Klass A */ public class A {}")
                .classes(pn + "pub; /** Klass B */ public class B extends A{}")
                .classes(pn + "pub; /** Klass C */ public class C {}")
                .classes(pn + "pub;")
                .classes(pn + "pub.pub1; /** Klass B */ public class B {}")
                .classes(pn + "pub.pub1.pub11; /** Klass C */ public class C {}")
                .classes(pn + "pub.pub1.pub12; /** Klass C */ public class C {}")
                .classes(pn + "pub.pub2.pub21; /** Klass C */ public class C {}")
                .classes(pn + "pro; /** Klass L */ public class L {}")
                .classes(pn + "pro.pro1; /** Klass M */ public class M {}")
                .classes(pn + "pro.pro1.pro11; /** Klass N */ public class N {}")
                .classes(pn + "pro.pro2; /** Klass O */ public class O {}")
                .classes(pn + "pro.pro2.pro21; /** Klass P */ public class P {}")
                .exports(mn + "pub")
                .exports(mn + "pub.pub1")
                .exports(mn + "pub.pub1.pub11")
                .exports(mn + "pub.pub1.pub12")
                .exports(mn + "pub.pub2.pub21")
                .write(src);
        return src.toString();
    }
}
