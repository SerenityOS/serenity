/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @modules jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.parser
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @compile --enable-preview -source ${jdk.version} DisambiguatePatterns.java
 * @run main/othervm --enable-preview DisambiguatePatterns
 */

import com.sun.source.tree.CaseLabelTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.PatternTree;
import com.sun.source.tree.SwitchTree;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.util.Options;
import java.nio.charset.Charset;

public class DisambiguatePatterns {

    public static void main(String... args) throws Throwable {
        DisambiguatePatterns test = new DisambiguatePatterns();
        test.disambiguationTest("String s",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("String s && s.isEmpty()",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("(String s)",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("(@Ann String s)",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("((String s))",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("(String) s",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("((String) s)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("((0x1))",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a > b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a >> b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a >>> b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a < b | a > b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a << b | a >> b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a << b || a < b | a >>> b)",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("(a < c.d > b)",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<? extends c.d> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("@Ann a<? extends c.d> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<? extends @Ann c.d> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<? super c.d> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<? super @Ann c.d> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<b<c.d>> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<b<@Ann c.d>> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a<b<c<d>>> b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a[] b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a[][] b",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("int i",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("int[] i",
                                 ExpressionType.PATTERN);
        test.disambiguationTest("a[a]",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a[b][c]",
                                 ExpressionType.EXPRESSION);
        test.disambiguationTest("a & b",
                                 ExpressionType.EXPRESSION);
    }

    private final ParserFactory factory;

    public DisambiguatePatterns() {
        Context context = new Context();
        JavacFileManager jfm = new JavacFileManager(context, true, Charset.defaultCharset());
        Options.instance(context).put(Option.PREVIEW, "");
        factory = ParserFactory.instance(context);
    }

    void disambiguationTest(String snippet, ExpressionType expectedType) {
        String code = """
                      public class Test {
                          private void test() {
                              switch (null) {
                                  case SNIPPET -> {}
                              }
                          }
                      }
                      """.replace("SNIPPET", snippet);
        JavacParser parser = factory.newParser(code, false, false, false);
        CompilationUnitTree result = parser.parseCompilationUnit();
        ClassTree clazz = (ClassTree) result.getTypeDecls().get(0);
        MethodTree method = (MethodTree) clazz.getMembers().get(0);
        SwitchTree st = (SwitchTree) method.getBody().getStatements().get(0);
        CaseLabelTree label = st.getCases().get(0).getLabels().get(0);
        ExpressionType actualType = switch (label) {
            case ExpressionTree et -> ExpressionType.EXPRESSION;
            case PatternTree pt -> ExpressionType.PATTERN;
            default -> throw new AssertionError("Unexpected result: " + result);
        };
        if (expectedType != actualType) {
            throw new AssertionError("Expected: " + expectedType + ", actual: " + actualType +
                                      ", for: " + code + ", parsed: " + result);
        }
    }

    enum ExpressionType {
        PATTERN,
        EXPRESSION;
    }

}
