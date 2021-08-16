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

/**
 * @test
 * @bug 8159305 8167383
 * @summary Tests elements filtering options
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.api
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.TestRunner
 * @run main FilterOptions
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import toolbox.*;

public class FilterOptions extends ModuleTestBase {

    private static final String NL = System.getProperty("line.separator");
    private static final String INDENT = "    ";

    // the sources are shared, so create it once
    private final String src;

    public static void main(String... args) throws Exception {
        new FilterOptions().runTests();
    }

    FilterOptions() throws IOException {
        this.src = createSources(Paths.get(".").resolve("src"));
    }

    @Test
    public void testDefault(Path base) throws Exception {
        execTask("--module-source-path", src, "--module", "m1");

        checkModulesSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesIncluded("pub");
        checkTypesIncluded("pub.A", "pub.A.ProtectedNested", "pub.A.PublicNested");
    }

    @Test
    public void testModuleModeApi(Path base) throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1", "--show-module-contents", "api");

        checkModuleMode("API");
        checkModulesSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesIncluded("pub");
        checkPackagesNotIncluded("pro", "pqe");
    }

    @Test
    public void testModuleModeAll(Path base) throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1", "--show-module-contents", "all");

        checkModuleMode("ALL");
        checkModulesSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesIncluded("pub", "pqe");
        checkPackagesNotIncluded("pro");
    }

    @Test
    public void testShowPackagesExported(Path base)  throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1",
                "--show-packages", "exported"); // default

        checkModulesSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesIncluded("pub");
        checkPackagesNotIncluded("pqe", "pro");
        checkTypesIncluded("pub.A", "pub.A.ProtectedNested", "pub.A.PublicNested");
    }

    @Test
    public void testShowPackagesAll(Path base) throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1",
                "--show-packages", "all");
        checkModulesSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesIncluded("pub", "pqe", "pro");

        checkTypesIncluded("pub.A", "pub.A.ProtectedNested", "pub.A.PublicNested",
                           "pqe.A", "pqe.A.ProtectedNested", "pqe.A.PublicNested",
                           "pro.A", "pro.A.ProtectedNested", "pro.A.PublicNested");
    }

    @Test
    public void testShowTypesPrivate(Path base) throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1",
                "--show-types", "private");

        checkModulesSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesIncluded("pub");

        checkTypesIncluded("pub.A", "pub.A.PrivateNested", "pub.A.Nested", "pub.A.ProtectedNested",
                           "pub.A.PublicNested",
                           "pub.B", "pub.B.PrivateNested", "pub.B.Nested", "pub.B.ProtectedNested",
                           "pub.B.PublicNested");

    }

    @Test
    public void testShowTypesPackage(Path base) throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1",
                "--show-types", "package");

        checkModulesSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesIncluded("pub");

        checkTypesIncluded("pub.A", "pub.A.Nested", "pub.A.ProtectedNested", "pub.A.PublicNested",
                           "pub.B", "pub.B.Nested", "pub.B.ProtectedNested", "pub.B.PublicNested");

        checkTypesNotIncluded(".*private.*");
    }

    @Test
    public void testShowTypesProtected(Path base) throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1",
                "--show-types", "protected");

        checkModulesSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesIncluded("pub");

        checkTypesIncluded("pub.A", "pub.A.ProtectedNested", "pub.A.PublicNested");

        checkTypesNotIncluded("pub.A.Nested", "pub.A.PrivateNested", "pub.B.Nested",
                              "pub.B.PrivateNested", "pub.B.ProtectedNested",
                              "pub.B.PublicNested");
    }

    @Test
    public void testShowTypesPublic(Path base) throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1",
                "--show-types", "public");

        checkModulesSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesIncluded("pub");

        checkTypesIncluded("pub.A", "pub.A.PublicNested");
        checkTypesNotIncluded("pub.A.Nested",
                              "pub.A.ProtectedNested", "pub.A.PrivateNested",
                              "pub.B.Nested", "pub.B.ProtectedNested", "pub.B.PrivateNested",
                              "pub.B.PublicNested");
    }

    @Test
    public void testShowMembersPrivate(Path base) throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1",
                "--show-members", "private");

        checkMembers(Visibility.PRIVATE);
    }

    @Test
    public void testShowMembersPackage(Path base) throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1",
                "--show-members", "package");

        checkMembers(Visibility.PACKAGE);
    }

    @Test
    public void testShowMembersProtected(Path base) throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1",
                "--show-members", "protected");

        checkMembers(Visibility.PROTECTED);
    }

    @Test
    public void testShowMembersPublic(Path base) throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1",
                "--show-members", "public");

        checkMembers(Visibility.PUBLIC);
    }

    @Test
    public void testLegacyPublic(Path base) throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1",
                "-public");

        checkModuleMode("API");
        checkModulesSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesIncluded("pub");
        checkPackagesNotIncluded("pqe", "pro");
        checkTypesIncluded("pub.A", "pub.A.PublicNested");

        checkMembers(Visibility.PUBLIC);
    }

    @Test
    public void testLegacyDefault(Path base) throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1");

        checkModuleMode("API");
        checkModulesSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesIncluded("pub");
        checkPackagesNotIncluded("pqe", "pro");
        checkTypesIncluded("pub.A", "pub.A.ProtectedNested", "pub.A.PublicNested");

        checkMembers(Visibility.PROTECTED);
    }

    @Test
    public void testLegacyProtected(Path base) throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1",
                "-protected");

        checkModuleMode("API");
        checkModulesSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesIncluded("pub");
        checkPackagesNotIncluded("pqe", "pro");
        checkTypesIncluded("pub.A", "pub.A.ProtectedNested", "pub.A.PublicNested");

        checkMembers(Visibility.PROTECTED);
    }

    @Test
    public void testLegacyPackage(Path base) throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1",
                "-package");

        checkModuleMode("ALL");
        checkModulesSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesIncluded("pub", "pqe", "pro");
        checkTypesIncluded("pub.B", "pub.B.Nested", "pub.B.ProtectedNested", "pub.B.PublicNested",
                           "pub.A", "pub.A.Nested", "pub.A.ProtectedNested", "pub.A.PublicNested",
                           "pqe.A", "pqe.A.Nested", "pqe.A.ProtectedNested", "pqe.A.PublicNested",
                           "pro.B", "pro.B.Nested", "pro.B.ProtectedNested", "pro.B.PublicNested",
                           "pro.A", "pro.A.Nested", "pro.A.ProtectedNested", "pro.A.PublicNested");

        checkMembers(Visibility.PACKAGE);
    }

    @Test
    public void testLegacyPrivate(Path base) throws Exception {
        execTask("--module-source-path", src,
                "--module", "m1",
                "-private");

        checkModuleMode("ALL");
        checkModulesSpecified("m1");
        checkModulesIncluded("m1");
        checkPackagesIncluded("pub", "pqe", "pro");
        checkTypesIncluded("pub.B", "pub.B.PrivateNested", "pub.B.Nested", "pub.B.ProtectedNested",
                           "pub.B.PublicNested",
                           "pub.A", "pub.A.PrivateNested", "pub.A.Nested", "pub.A.ProtectedNested",
                           "pub.A.PublicNested",
                           "pqe.A", "pqe.A.PrivateNested", "pqe.A.Nested", "pqe.A.ProtectedNested",
                           "pqe.A.PublicNested",
                           "pro.B", "pro.B.PrivateNested", "pro.B.Nested", "pro.B.ProtectedNested",
                           "pro.B.PublicNested",
                           "pro.A", "pro.A.PrivateNested", "pro.A.Nested", "pro.A.ProtectedNested",
                           "pro.A.PublicNested");

        checkMembers(Visibility.PRIVATE);
    }

    private static enum Visibility {
        PRIVATE, PACKAGE, PROTECTED, PUBLIC;
    }

    void checkMembers(Visibility v) throws Exception {
        checkMembersPresence(v);
        checkMembersAbsence(v);
    }

    void checkMembersPresence(Visibility v) throws Exception {
        switch (v) {
            case PRIVATE:
                checkMembersSelected("pub.A.privateFieldA",
                                     "pub.A.PublicNested.privateFieldPublicNested",
                                     "pub.A.ProtectedNested.privateFieldProtectedNested");

            case PACKAGE:
                checkMembersSelected("pub.A.FieldA",
                                     "pub.A.PublicNested.FieldPublicNested",
                                     "pub.A.ProtectedNested.FieldProtectedNested");

            case PROTECTED:
                checkMembersSelected("pub.A.<init>",
                                     "pub.A.protectedFieldA",
                                     "pub.A.PublicNested.protectedFieldPublicNested",
                                     "pub.A.ProtectedNested.<init>",
                                     "pub.A.ProtectedNested.protectedFieldProtectedNested",
                                     "pub.A.ProtectedNested.publicFieldProtectedNested");

            case PUBLIC:
                checkMembersSelected("pub.A.publicFieldA",
                                     "pub.A.PublicNested.<init>",
                                     "pub.A.PublicNested.publicFieldPublicNested");

                break;
        }
    }

    void checkMembersAbsence(Visibility v) throws Exception {
        switch (v) {
            case PUBLIC:
                checkMembersNotSelected("pub.A.protectedFieldA",
                                        "pub.A.PublicNested.protectedFieldPublicNested",
                                        "pub.A.ProtectedNested.<init>",
                                        "pub.A.ProtectedNested.protectedFieldProtectedNested");

            case PROTECTED:
                checkMembersNotSelected("pub.A.FieldA",
                                        "pub.A.PublicNested.FieldPublicNested",
                                        "pub.A.ProtectedNested.FieldProtectedNested");

            case PACKAGE:
                checkMembersNotSelected("pub.A.privateFieldA",
                                        "pub.A.PublicNested.privateFieldPublicNested",
                                        "pub.A.ProtectedNested.privateFieldProtectedNested");

            case PRIVATE:
                break;
        }
    }

    String createSources(Path src) throws IOException {
        ModuleBuilder mb1 = new ModuleBuilder(tb, "m1");
        mb1.comment("The first module.")
                .classes(createClass("pub", "A", true))
                .classes(createClass("pub", "B", false))
                .classes(createClass("pro", "A", true))
                .classes(createClass("pro", "B", false))
                .classes(createClass("pqe", "A", true))
                .exports("pub")
                .exportsTo("pqe", "m2")
                .write(src);

        ModuleBuilder mb2 = new ModuleBuilder(tb, "m2");
        mb2.comment("The second module")
                .classes(createClass("m2pub", "A", true))
                .requires("m1")
                .write(src);

        return src.toString();
    }

    String createClass(String pkg, String name, boolean isPublic) {
        StringBuilder sb = new StringBuilder("package ")
                .append(pkg)
                .append("; ")
                .append(NL);
        sb.append(" /** Klass ")
                .append(name)
                .append(" */")
                .append(NL);
        sb.append(isPublic ? "public " : " ")
                .append("class ")
                .append(name)
                .append(" {")
                .append(NL);

        sb.append(createMembers(INDENT, name));
        sb.append(createNestedClass(INDENT, "PublicNested", name, "public"));
        sb.append(createNestedClass(INDENT, "ProtectedNested", name, "protected"));
        sb.append(createNestedClass(INDENT, "Nested", name, ""));
        sb.append(createNestedClass(INDENT, "PrivateNested", name, "private"));

        return sb.append("}").toString();
    }

    StringBuilder createNestedClass(String indent, String name, String enclosing, String visibility) {
        return new StringBuilder()
                .append(indent).append(" /** Klass ").append(name).append(" */").append(NL)
                .append(indent).append(visibility).append(" class ").append(name).append(" {").append(NL)
                .append(createMembers(indent + INDENT, name)).append(indent).append("}").append(NL);
    }

    StringBuilder createMembers(String indent, String enclosing) {
        return new StringBuilder()
                .append(indent).append(createMember(enclosing, "public"))
                .append(indent).append(createMember(enclosing, "protected"))
                .append(indent).append(createMember(enclosing, ""))
                .append(indent).append(createMember(enclosing, "private"))
                .append(NL);
    }

    StringBuilder createMember(String enclosingClass, String visibility) {
        return new StringBuilder()
                .append("/** a ")
                .append(visibility)
                .append("Field in ")
                .append(enclosingClass)
                .append(" */ ")
                .append(visibility)
                .append(" String ")
                .append(visibility)
                .append("Field")
                .append(enclosingClass)
                .append(";")
                .append(NL);
    }
}
