/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6471577 6517779
 * @summary Test Elements.getConstantExpression
 * @author  Joseph D. Darcy
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build   JavacTestingAbstractProcessor TestGetConstantExpression
 * @compile -processor TestGetConstantExpression Foo.java
 */

import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import static javax.lang.model.SourceVersion.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import static javax.lang.model.util.ElementFilter.*;
import static javax.tools.Diagnostic.Kind.*;
import static javax.tools.StandardLocation.*;
import java.io.*;

/**
 * Test basic workings of Elements.getConstantExpression.
 */
public class TestGetConstantExpression extends JavacTestingAbstractProcessor {
    private int round = 1;

    /**
     * Check expected behavior on classes and packages.
     */
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        int errors = 0;
        boolean processingOver = roundEnv.processingOver();

        if (!processingOver && round == 1) {
            errors += expectIllegalArgumentException(null);
            errors += expectIllegalArgumentException(this);

            // Generate source code with various constant values and
            // make sure it compiles.

            try {
                PrintWriter pw = new PrintWriter(filer.createSourceFile("ConstantTest").openWriter());
                try {
                    Boolean[]   booleans = {true, false};
                    Byte[]      bytes    = {Byte.MIN_VALUE,    -1,  0, 1,  Byte.MAX_VALUE};
                    Short[]     shorts   = {Short.MIN_VALUE,   -1,  0, 1,  Short.MAX_VALUE};
                    Integer[]   ints     = {Integer.MIN_VALUE, -1,  0, 1,  Integer.MAX_VALUE};
                    Long[]      longs    = {Long.MIN_VALUE,    -1L, 0L,1L, Long.MAX_VALUE};
                    Character[] chars    = {Character.MIN_VALUE, ' ', '\t', 'a', 'b', 'c', '~', Character.MAX_VALUE};
                    Float[]     floats   = {Float.NaN,  Float.NEGATIVE_INFINITY,  -1.0f, -0.0f, 0.0f, 1.0f, Float.POSITIVE_INFINITY};
                    Double[]    doubles  = {Double.NaN, Double.NEGATIVE_INFINITY, -1.0,  -0.0,  0.0,  1.0,  Double.POSITIVE_INFINITY};

                    pw.println("class ConstantTest {");
                    pw.println(String.format("  private static boolean[] booleans = {%s};",
                                             printConstants(booleans)));
                    pw.println(String.format("  private static byte[] bytes = {%s};",
                                             printConstants(bytes)));
                    pw.println(String.format("  private static short[] shorts = {%s};",
                                             printConstants(shorts)));
                    pw.println(String.format("  private static int[] ints = {%s};",
                                             printConstants(ints)));
                    pw.println(String.format("  private static long[] longs = {%s};",
                                             printConstants(longs)));
                    pw.println(String.format("  private static char[] chars = {%s};",
                                             printConstants(chars)));
                    pw.println(String.format("  private static float[] floats = {%s};",
                                             printConstants(floats)));
                    pw.println(String.format("  private static double[] doubles = {%s};",
                                             printConstants(doubles)));
                    pw.println("}");
                } finally {
                    pw.close();
                }
            } catch(IOException io) {
                throw new RuntimeException(io);
            }
            round++;
        } else if (processingOver) {
            if (errors > 0) {
                throw new RuntimeException();
            }
        }
        return true;
    }

    String printConstants(Object[] constants) {
        StringBuilder sb = new StringBuilder();

        for(Object o : constants) {
            sb.append(eltUtils.getConstantExpression(o));
            sb.append(", ");
        }
        return sb.toString();
    }

    int expectIllegalArgumentException(Object o) {
        String s = "";
        try {
            s = eltUtils.getConstantExpression(o);
            System.err.println("Unexpected string returned: " + s);
            return 1;
        } catch (IllegalArgumentException iae) {
            return 0;
        }
    }
}
