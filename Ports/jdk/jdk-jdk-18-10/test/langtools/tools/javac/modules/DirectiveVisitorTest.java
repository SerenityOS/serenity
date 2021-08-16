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

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.Directive;
import javax.lang.model.element.ModuleElement.DirectiveKind;
import javax.lang.model.element.ModuleElement.DirectiveVisitor;
import javax.lang.model.element.ModuleElement.ExportsDirective;
import javax.lang.model.element.ModuleElement.OpensDirective;
import javax.lang.model.element.ModuleElement.ProvidesDirective;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.ModuleElement.UsesDirective;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;

/*
 * @test
 * @bug 8175118
 * @summary Add ModuleElement.DirectiveVisitor
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox ModuleTestBase
 * @run main DirectiveVisitorTest
 */

public class DirectiveVisitorTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        new DirectiveVisitorTest().runTests();
    }

    @Test
    public void testVisitor(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { "
                          + " requires m2x;"
                          + " exports p1;"
                          + " opens p2;"
                          + " uses p1.Service;"
                          + " provides p1.Service with p2.Impl;"
                          + "}",
                          "package p1; public interface Service { }",
                          "package p2; public class Impl implements p1.Service { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { }");

        Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = javac.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(findJavaFiles(src));
            List<String> options = List.of(
                    "--module-source-path", src.toString(),
                    "-d", modules.toString()
            );
            JavacTask t = (JavacTask) javac.getTask(null, fm, null, options, null, files);
            t.analyze();
            ModuleElement e = t.getElements().getModuleElement("m1x");
            Set<DirectiveKind> kinds = EnumSet.<DirectiveKind>allOf(DirectiveKind.class);
            Visitor v = new Visitor();
            v.visit(e, kinds);
            if (!kinds.equals(EnumSet.<DirectiveKind>noneOf(DirectiveKind.class))) {
                error("Some kinds not found: " + kinds);
            }
        }
    }

    static class Visitor implements DirectiveVisitor<Void,Set<DirectiveKind>> {
        private final PrintStream out = System.err;

        public void visit(ModuleElement e, Set<DirectiveKind> kinds) {
            e.getDirectives().stream().forEach(d -> visit(d, kinds));
        }

        @Override
        public Void visitRequires(RequiresDirective d, Set<DirectiveKind> kinds) {
            visitAny(d, kinds);
            return null;
        }

        @Override
        public Void visitExports(ExportsDirective d, Set<DirectiveKind> kinds) {
            visitAny(d, kinds);
            return null;
        }

        @Override
        public Void visitOpens(OpensDirective d, Set<DirectiveKind> kinds) {
            visitAny(d, kinds);
            return null;
        }

        @Override
        public Void visitUses(UsesDirective d, Set<DirectiveKind> kinds) {
            visitAny(d, kinds);
            return null;
        }

        @Override
        public Void visitProvides(ProvidesDirective d, Set<DirectiveKind> kinds) {
            visitAny(d, kinds);
            return null;
        }

        private void visitAny(Directive d, Set<DirectiveKind> kinds) {
            out.println("visit: " + d);
            kinds.remove(d.getKind());
        }
    }
}

