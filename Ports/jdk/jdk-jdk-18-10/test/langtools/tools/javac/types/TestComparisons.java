/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8013357
 * @summary javac should correctly enforce binary comparison rules.
 * @modules jdk.compiler
 */

import java.io.*;

public class TestComparisons {

    private int errors = 0;
    private int testnum = 0;

    static final File testdir = new File("8013357");

    private enum CompareType {
        BYTE_PRIM("byte"),
        SHORT_PRIM("short"),
        CHAR_PRIM("char"),
        INTEGER_PRIM("int"),
        LONG_PRIM("long"),
        FLOAT_PRIM("float"),
        DOUBLE_PRIM("double"),
        BOOLEAN_PRIM("boolean"),

        BYTE("Byte"),
        SHORT("Short"),
        CHAR("Character"),
        INTEGER("Integer"),
        LONG("Long"),
        FLOAT("Float"),
        DOUBLE("Double"),
        BOOLEAN("Boolean"),

        BYTE_SUPER("List<? super Byte>", true),
        SHORT_SUPER("List<? super Short>", true),
        CHAR_SUPER("List<? super Character>", true),
        INTEGER_SUPER("List<? super Integer>", true),
        LONG_SUPER("List<? super Long>", true),
        FLOAT_SUPER("List<? super Float>", true),
        DOUBLE_SUPER("List<? super Double>", true),
        BOOLEAN_SUPER("List<? super Boolean>", true),

        OBJECT("Object"),
        NUMBER("Number"),
        STRING("String");

        public final boolean isList;
        public final String name;

        private CompareType(final String name, final boolean isList) {
            this.isList = isList;
            this.name = name;
        }

        private CompareType(final String name) {
            this(name, false);
        }
    }

    // The integers here refer to which subsection of JLS 15.21 is in
    // effect.  0 means no comparison is allowed.
    private static final int truthtab[][] = {
        // byte, comparable to itself, any numeric type, or any boxed
        // numeric type.
        { 1, 1, 1, 1, 1, 1, 1, 0, // Primitives
          1, 1, 1, 1, 1, 1, 1, 0, // Boxed primitives
          0, 0, 0, 0, 0, 0, 0, 0, // Captures
          0, 0, 0                 // Reference types
        },
        // short, comparable to itself, any numeric type, or any boxed
        // numeric type.
        { 1, 1, 1, 1, 1, 1, 1, 0, // Primitives
          1, 1, 1, 1, 1, 1, 1, 0, // Boxed primitives
          0, 0, 0, 0, 0, 0, 0, 0, // Captures
          0, 0, 0                 // Reference types
        },
        // char, comparable to itself, any numeric type, or any boxed
        // numeric type.
        { 1, 1, 1, 1, 1, 1, 1, 0, // Primitives
          1, 1, 1, 1, 1, 1, 1, 0, // Boxed primitives
          0, 0, 0, 0, 0, 0, 0, 0, // Captures
          0, 0, 0                 // Reference types
        },
        // int, comparable to itself, any numeric type, or any boxed
        // numeric type.
        { 1, 1, 1, 1, 1, 1, 1, 0, // Primitives
          1, 1, 1, 1, 1, 1, 1, 0, // Boxed primitives
          0, 0, 0, 0, 0, 0, 0, 0, // Captures
          0, 0, 0                 // Reference types
        },
        // long, comparable to itself, any numeric type, or any boxed
        // numeric type.
        { 1, 1, 1, 1, 1, 1, 1, 0, // Primitives
          1, 1, 1, 1, 1, 1, 1, 0, // Boxed primitives
          0, 0, 0, 0, 0, 0, 0, 0, // Captures
          0, 0, 0                 // Reference types
        },
        // float, comparable to itself, any numeric type, or any boxed
        // numeric type.
        { 1, 1, 1, 1, 1, 1, 1, 0, // Primitives
          1, 1, 1, 1, 1, 1, 1, 0, // Boxed primitives
          0, 0, 0, 0, 0, 0, 0, 0, // Captures
          0, 0, 0                 // Reference types
        },
        // double, comparable to itself, any numeric type, or any boxed
        // numeric type.
        { 1, 1, 1, 1, 1, 1, 1, 0, // Primitives
          1, 1, 1, 1, 1, 1, 1, 0, // Boxed primitives
          0, 0, 0, 0, 0, 0, 0, 0, // Captures
          0, 0, 0                 // Reference types
        },
        // boolean, comparable only to itself and Boolean.
        { 0, 0, 0, 0, 0, 0, 0, 2, // Primitives
          0, 0, 0, 0, 0, 0, 0, 2, // Boxed primitives
          0, 0, 0, 0, 0, 0, 0, 0, // Captures
          0, 0, 0                 // Reference types
        },
        // Byte, comparable to itself, Number, Object, any numeric primitive,
        // and any captures.
        { 1, 1, 1, 1, 1, 1, 1, 0, // Primitives
          3, 0, 0, 0, 0, 0, 0, 0, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 3, 0                 // Reference types
        },
        // Short, comparable to itself, Number, Object, any numeric primitive,
        // and any captures.
        { 1, 1, 1, 1, 1, 1, 1, 0, // Primitives
          0, 3, 0, 0, 0, 0, 0, 0, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 3, 0                 // Reference types
        },
        // Character, comparable to itself, Object, any numeric primitive,
        // and any captures.
        { 1, 1, 1, 1, 1, 1, 1, 0, // Primitives
          0, 0, 3, 0, 0, 0, 0, 0, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 0, 0                 // Reference types
        },
        // Int, comparable to itself, Number, Object, any numeric primitive,
        // and any captures.
        { 1, 1, 1, 1, 1, 1, 1, 0, // Primitives
          0, 0, 0, 3, 0, 0, 0, 0, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 3, 0                 // Reference types
        },
        // Long, comparable to itself, Number, Object, any numeric primitive,
        // and any captures.
        { 1, 1, 1, 1, 1, 1, 1, 0, // Primitives
          0, 0, 0, 0, 3, 0, 0, 0, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 3, 0                 // Reference types
        },
        // Float, comparable to itself, Number, Object, any numeric primitive,
        // and any captures.
        { 1, 1, 1, 1, 1, 1, 1, 0, // Primitives
          0, 0, 0, 0, 0, 3, 0, 0, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 3, 0                 // Reference types
        },
        // Double, comparable to itself, Number, Object, any numeric primitive,
        // and any captures.
        { 1, 1, 1, 1, 1, 1, 1, 0, // Primitives
          0, 0, 0, 0, 0, 0, 3, 0, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 3, 0                 // Reference types
        },
        // Boolean, to itself, any capture, Object, and boolean.
        { 0, 0, 0, 0, 0, 0, 0, 2, // Primitives
          0, 0, 0, 0, 0, 0, 0, 2, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 0, 0                 // Reference types
        },
        // Byte supertype wildcard, comparable to any reference type.
        // and any captures.
        { 0, 0, 0, 0, 0, 0, 0, 0, // Primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 3, 3                 // Reference types
        },
        // Short supertype wildcard, comparable to any reference type.
        // and any captures.
        { 0, 0, 0, 0, 0, 0, 0, 0, // Primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 3, 3                 // Reference types
        },
        // Character supertype wildcard, comparable to any reference type.
        // and any captures.
        { 0, 0, 0, 0, 0, 0, 0, 0, // Primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 3, 3                 // Reference types
        },
        // Integer supertype wildcard, comparable to any reference type.
        // and any captures.
        { 0, 0, 0, 0, 0, 0, 0, 0, // Primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 3, 3                 // Reference types
        },
        // Long supertype wildcard, comparable to any reference type.
        // and any captures.
        { 0, 0, 0, 0, 0, 0, 0, 0, // Primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 3, 3                 // Reference types
        },
        // Float supertype wildcard, comparable to any reference type.
        // and any captures.
        { 0, 0, 0, 0, 0, 0, 0, 0, // Primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 3, 3                 // Reference types
        },
        // Double supertype wildcard, comparable to any reference type.
        // and any captures.
        { 0, 0, 0, 0, 0, 0, 0, 0, // Primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 3, 3                 // Reference types
        },
        // Boolean supertype wildcard, comparable to any reference type.
        // and any captures.
        { 0, 0, 0, 0, 0, 0, 0, 0, // Primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 3, 3                 // Reference types
        },
        // Object, comparable to any reference type.
        // and any captures.
        { 0, 0, 0, 0, 0, 0, 0, 0, // Primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 3, 3                 // Reference types
        },
        // Number, comparable to Object, any of its subclasses.
        // and any captures.
        { 0, 0, 0, 0, 0, 0, 0, 0, // Primitives
          3, 3, 0, 3, 3, 3, 3, 0, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 3, 0                 // Reference types
        },
        // String supertype wildcard, comparable to any reference type.
        // and any captures.
        { 0, 0, 0, 0, 0, 0, 0, 0, // Primitives
          0, 0, 0, 0, 0, 0, 0, 0, // Boxed primitives
          3, 3, 3, 3, 3, 3, 3, 3, // Captures
          3, 0, 3                 // Reference types
        }
    };

    private void assert_compile_fail(final File file, final String body) {
        final String filename = file.getPath();
        final String[] args = { filename };
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        final int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.close();
        if (rc == 0) {
            System.err.println("Compilation of " + file.getName() +
                               " didn't fail as expected.\nFile:\n" +
                               body + "\nOutput:\n" + sw.toString());
            errors++;
        }
    }

    private void assert_compile_succeed(final File file, final String body) {
        final String filename = file.getPath();
        final String[] args = { filename };
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        final int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.close();
        if (rc != 0) {
            System.err.println("Compilation of " + file.getName() +
                               " didn't succeed as expected.\nFile:\n" +
                               body + "\nOutput:\n" +
                               sw.toString());
            errors++;
        }
    }

    private String makeBody(final int num,
                            final CompareType left,
                            final CompareType right) {
        return "import java.util.List;\n" +
            "public class Test" + num + " {\n" +
            "    public boolean test(" + left.name +
            " left, " + right.name + " right) {\n" +
            "        return left" + (left.isList ? ".get(0)" : "") +
            " == right" + (right.isList ? ".get(0)" : "") + ";\n" +
            "    }\n" +
            "}\n";
    }

    private File writeFile(final String filename,
                           final String body)
        throws IOException {
        final File f = new File(testdir, filename);
        f.getParentFile().mkdirs();
        final FileWriter out = new FileWriter(f);
        out.write(body);
        out.close();
        return f;
    }

    private void test(final CompareType left, final CompareType right)
        throws IOException {
        final int num = testnum++;
        final String filename = "Test" + num + ".java";
        final String body = makeBody(num, left, right);
        final File file = writeFile(filename, body);
        if (truthtab[left.ordinal()][right.ordinal()] != 0)
            assert_compile_succeed(file, body);
        else
            assert_compile_fail(file, body);
    }

    void run() throws Exception {
        testdir.mkdir();

        for(CompareType left : CompareType.values())
            for(CompareType right : CompareType.values())
                test(left, right);

        if (errors != 0)
            throw new Exception("ObjectZeroCompare test failed with " +
                                errors + " errors.");
    }

    public static void main(String... args) throws Exception {
        new TestComparisons().run();
    }
}
