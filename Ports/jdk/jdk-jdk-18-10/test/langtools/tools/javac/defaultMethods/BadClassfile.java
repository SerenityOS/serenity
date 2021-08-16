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
 * @bug 8025087
 * @summary Verify that pre-JDK8 classfiles with default and/or static methods
 *          are refused correctly.
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.jvm
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build BadClassfile
 * @run main BadClassfile
 */

import com.sun.tools.classfile.*;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.ClassFinder.BadClassFile;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.JCDiagnostic;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public class BadClassfile {
    public static void main(String... args) throws Exception {
        test("BadClassfile$DefaultMethodTest", "compiler.misc.invalid.default.interface");
        test("BadClassfile$StaticMethodTest", "compiler.misc.invalid.static.interface");
    }

    private static void test(String classname, String expected) throws Exception {
        File classfile = new File(System.getProperty("test.classes", "."), classname + ".class");
        ClassFile cf = ClassFile.read(classfile);

        cf = new ClassFile(cf.magic, Target.JDK1_7.minorVersion,
                 Target.JDK1_7.majorVersion, cf.constant_pool, cf.access_flags,
                cf.this_class, cf.super_class, cf.interfaces, cf.fields,
                cf.methods, cf.attributes);

        new ClassWriter().write(cf, classfile);

        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        JavacTaskImpl task = (JavacTaskImpl) c.getTask(null, null, null, Arrays.asList("-classpath", System.getProperty("test.classes", ".")), null, null);
        Symtab syms = Symtab.instance(task.getContext());

        task.ensureEntered();

        try {
            Symbol clazz = com.sun.tools.javac.main.JavaCompiler.instance(task.getContext()).resolveIdent(syms.unnamedModule, classname);

            clazz.complete();
        } catch (BadClassFile f) {
            JCDiagnostic embeddedDiag = (JCDiagnostic) f.getDiagnostic().getArgs()[1];
            assertEquals(expected, embeddedDiag.getCode());
            assertEquals(Integer.toString(Target.JDK1_7.majorVersion), embeddedDiag.getArgs()[0]);
            assertEquals(Integer.toString(Target.JDK1_7.minorVersion), embeddedDiag.getArgs()[1]);
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        Assert.check(Objects.equals(expected, actual),
                     "expected: " + expected + ", but was: " + actual);
    }

    interface DefaultMethodTest {
        default void test() { }
    }
    interface StaticMethodTest {
        static void test() { }
    }
}
