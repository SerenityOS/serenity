/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6304912
 * @summary unit test for Log
 * @modules jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.parser
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util:+open
 *          jdk.compiler/com.sun.tools.javac.resources
 */
import java.lang.reflect.Field;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Set;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticFlag;
import com.sun.tools.javac.util.JCDiagnostic.Factory;
import com.sun.tools.javac.util.Options;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.Warnings;

public class TestLog
{
    public static void main(String... args) throws Exception {
        test(false);
        test(true);
    }

    static void test(boolean genEndPos) throws Exception {
        Context context = new Context();

        Options options = Options.instance(context);
        options.put("diags", "%b:%s/%o/%e:%_%t%m|%p%m");

        Log log = Log.instance(context);
        Factory diagnosticFactory = JCDiagnostic.Factory.instance(context);
        Field defaultErrorFlagsField =
                JCDiagnostic.Factory.class.getDeclaredField("defaultErrorFlags");

        defaultErrorFlagsField.setAccessible(true);

        Set<DiagnosticFlag> defaultErrorFlags =
                (Set<DiagnosticFlag>) defaultErrorFlagsField.get(diagnosticFactory);

        defaultErrorFlags.add(DiagnosticFlag.API);

        JavacFileManager.preRegister(context);
        ParserFactory pfac = ParserFactory.instance(context);

        final String text =
              "public class Foo {\n"
            + "  public static void main(String[] args) {\n"
            + "    if (args.length == 0)\n"
            + "      System.out.println(\"no args\");\n"
            + "    else\n"
            + "      System.out.println(args.length + \" args\");\n"
            + "  }\n"
            + "}\n";
        JavaFileObject fo = new StringJavaFileObject("Foo", text);
        log.useSource(fo);

        CharSequence cs = fo.getCharContent(true);
        Parser parser = pfac.newParser(cs, false, genEndPos, false);
        JCTree.JCCompilationUnit tree = parser.parseCompilationUnit();
        log.setEndPosTable(fo, tree.endPositions);

        TreeScanner ts = new LogTester(log, tree.endPositions);
        ts.scan(tree);

        check(log.nerrors, 4, "errors");
        check(log.nwarnings, 4, "warnings");
    }

    private static void check(int found, int expected, String name) {
        if (found == expected)
            System.err.println(found + " " + name + " found, as expected.");
        else {
            System.err.println("incorrect number of " + name + " found.");
            System.err.println("expected: " + expected);
            System.err.println("   found: " + found);
            throw new IllegalStateException("test failed");
        }
    }

    private static class LogTester extends TreeScanner {
        LogTester(Log log, EndPosTable endPosTable) {
            this.log = log;
            this.endPosTable = endPosTable;
        }

        public void visitIf(JCTree.JCIf tree) {
            JCDiagnostic.DiagnosticPosition nil = null;
            // generate dummy messages to exercise the log API
            log.error(Errors.NotStmt);
            log.error(tree.pos, Errors.NotStmt);
            log.error(tree.pos(), Errors.NotStmt);
            log.error(nil, Errors.NotStmt);

            log.warning(Warnings.DivZero);
            log.warning(tree.pos, Warnings.DivZero);
            log.warning(tree.pos(), Warnings.DivZero);
            log.warning(nil, Warnings.DivZero);
        }

        private Log log;
        private EndPosTable endPosTable;
    }

    private static class StringJavaFileObject extends SimpleJavaFileObject {
        StringJavaFileObject(String name, String text) {
            super(URI.create(name), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        public CharSequence getCharContent(boolean b) {
            return text;
        }
        public InputStream openInputStream() {
            throw new UnsupportedOperationException();
        }
        public OutputStream openOutputStream() {
            throw new UnsupportedOperationException();
        }
        private String text;
    }
}
