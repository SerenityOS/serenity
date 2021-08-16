/*
 * Copyright 2016 Google, Inc.  All rights reserved.
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
 * @bug 8171132
 * @summary Improve class reading of invalid or out-of-range ConstantValue attributes
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.jvm
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build BadConstantValue
 * @run main BadConstantValue
 */

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ClassWriter;
import com.sun.tools.classfile.Field;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.ClassFinder.BadClassFile;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Names;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Objects;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class BadConstantValue {

    static final File classesdir = new File("badconstants");

    public static void main(String[] args) throws Exception {
        // report errors for ConstantValues of the wrong type
        testInvalidConstantType("int");
        testInvalidConstantType("short");
        testInvalidConstantType("byte");
        testInvalidConstantType("char");
        testInvalidConstantType("boolean");

        // report errors for ConstantValues outside the expected range
        testValidConstRange("int", Integer.MAX_VALUE);
        testValidConstRange("int", Integer.MIN_VALUE);

        testValidConstRange("short", Short.MAX_VALUE);
        testValidConstRange("short", Short.MIN_VALUE);
        testInvalidConstRange("short", Short.MAX_VALUE + 1);
        testInvalidConstRange("short", Short.MIN_VALUE - 1);

        testValidConstRange("byte", Byte.MAX_VALUE);
        testValidConstRange("byte", Byte.MIN_VALUE);
        testInvalidConstRange("byte", Byte.MAX_VALUE + 1);
        testInvalidConstRange("byte", Byte.MIN_VALUE - 1);

        testValidConstRange("char", Character.MAX_VALUE);
        testValidConstRange("char", Character.MIN_VALUE);
        testInvalidConstRange("char", Character.MAX_VALUE + 1);
        testInvalidConstRange("char", Character.MIN_VALUE - 1);

        testValidConstRange("boolean", 0);
        testValidConstRange("boolean", 1);
        testInvalidConstRange("boolean", 2);
        testInvalidConstRange("boolean", Integer.MIN_VALUE);
        testInvalidConstRange("boolean", Integer.MAX_VALUE);
    }

    /**
     * Tests that a constant value of the given {@code type} and initialized with an out-of-range
     * {@code value} is rejected.
     */
    private static void testInvalidConstRange(String type, int value) throws Exception {
        createConstantWithValue(type, value);
        BadClassFile badClassFile = loadBadClass("Lib");
        if (badClassFile == null) {
            throw new AssertionError("did not see expected error");
        }
        JCDiagnostic diagnostic = (JCDiagnostic) badClassFile.getDiagnostic().getArgs()[1];
        assertEquals("compiler.misc.bad.constant.range", diagnostic.getCode());
        assertEquals(3, diagnostic.getArgs().length);
        assertEquals(value, diagnostic.getArgs()[0]);
        assertEquals("B", diagnostic.getArgs()[1].toString());
        assertEquals(type, String.valueOf(diagnostic.getArgs()[2]));
    }

    /**
     * Tests that a constant value of the given {@code type} and initialized with {@code value} is
     * accepted.
     */
    private static void testValidConstRange(String type, int value) throws Exception {
        createConstantWithValue(type, value);
        BadClassFile badClassFile = loadBadClass("Lib");
        if (badClassFile != null) {
          throw new AssertionError("saw unexpected error", badClassFile);
        }
    }

    /**
     * Creates a class file containing a constant field with the given type and value, which may be
     * outside the expected range.
     */
    private static void createConstantWithValue(String type, int value) throws Exception {
        // Create a class with two constants, A and B. A is of type int and has value "actual";
        // B is of type "type" and is initialized to that type's default value.
        File lib = writeFile(classesdir, "Lib.java", String.format(
                "class Lib { static final int A = %s; static final %s B = %s; }",
                value, type, (type.equals("boolean") ? "false" : "0")));
        compile("-d", classesdir.getPath(), lib.getPath());
        // Lib.class may possibly not get a newer timestamp. Make sure .java file won't get used.
        lib.delete();
        File libClass = new File(classesdir, "Lib.class");
        // Rewrite the class to only have field B of type "type" and with "value" (potentially
        // out of range).
        swapConstantValues(libClass);
    }

    /** Tests that a field of the given integral type with a constant string value is rejected. */
    private static void testInvalidConstantType(String type) throws Exception {
        // create a class file with field that has an invalid CONSTANT_String ConstantValue
        File lib = writeFile(classesdir, "Lib.java", String.format(
                "class Lib { static final String A = \"hello\"; static final %s CONST = %s; }",
                type, type.equals("boolean") ? "false" : "0"));
        compile("-d", classesdir.getPath(), lib.getPath());
        // Lib.class may possibly not get a newer timestamp. Make sure .java file won't get used.
        lib.delete();
        File libClass = new File(classesdir, "Lib.class");
        swapConstantValues(libClass);

        BadClassFile badClassFile = loadBadClass("Lib");

        JCDiagnostic diagnostic = (JCDiagnostic) badClassFile.getDiagnostic().getArgs()[1];
        assertEquals("compiler.misc.bad.constant.value", diagnostic.getCode());
        assertEquals(3, diagnostic.getArgs().length);
        assertEquals("hello", diagnostic.getArgs()[0]);
        assertEquals("CONST", diagnostic.getArgs()[1].toString());
        assertEquals("Integer", diagnostic.getArgs()[2]);
    }

    private static BadClassFile loadBadClass(String className) {
        // load the class, and save the thrown BadClassFile exception
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        JavacTaskImpl task = (JavacTaskImpl) c.getTask(null, null, null,
                Arrays.asList("-classpath", classesdir.getPath()), null, null);
        Names names = Names.instance(task.getContext());
        Symtab syms = Symtab.instance(task.getContext());
        task.ensureEntered();
        try {
            syms.enterClass(syms.unnamedModule, names.fromString(className)).complete();
        } catch (BadClassFile e) {
            return e;
        }
        return null;
    }

    /**
     * Given a class file with two constant fields A and B, replaces both with a single field with
     * B's type and A's ConstantValue attribute.
     */
    private static void swapConstantValues(File file) throws Exception {
        ClassFile cf = ClassFile.read(file);
        Field a = cf.fields[0];
        Field b = cf.fields[1];
        Field[] fields = {
            new Field(b.access_flags, b.name_index, b.descriptor, a.attributes),
        };
        cf = new ClassFile(cf.magic, Target.JDK1_7.minorVersion, Target.JDK1_7.majorVersion,
                cf.constant_pool, cf.access_flags, cf.this_class, cf.super_class, cf.interfaces,
                fields, cf.methods, cf.attributes);
        new ClassWriter().write(cf, file);
    }

    static String compile(String... args) throws Exception {
        System.err.println("compile: " + Arrays.asList(args));
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.close();
        String out = sw.toString();
        if (out.length() > 0) {
            System.err.println(out);
        }
        if (rc != 0) {
            throw new AssertionError("compilation failed, rc=" + rc);
        }
        return out;
    }

    static File writeFile(File dir, String path, String body) throws IOException {
        File f = new File(dir, path);
        f.getParentFile().mkdirs();
        FileWriter out = new FileWriter(f);
        out.write(body);
        out.close();
        return f;
    }

    static void assertEquals(Object expected, Object actual) {
        Assert.check(Objects.equals(expected, actual),
                String.format("expected: %s, but was: %s", expected, actual));
    }
}
