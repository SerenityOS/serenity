/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 SAP SE. All rights reserved.
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
 *
 */

/**
 * @test
 * @summary Test messages of IllegalAccessError.
 * @modules java.base/java.lang:open
 *          java.base/jdk.internal.org.objectweb.asm
 * @compile IAE_Loader1.java IAE_Loader2.java IAE78_A.java IAE78_B.java
 *          IllegalAccessErrorTest.java
 * @run main/othervm -Xbootclasspath/a:. test.IllegalAccessErrorTest
 */

// Put this test into a package so we see qualified class names in
// the error messages. Verify that classes are printed with '.' instead
// of '/'.
package test;

import java.lang.reflect.*;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodHandles.Lookup.*;
import java.security.*;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

import test.*;

abstract public class IllegalAccessErrorTest {

    // interface
    private static String expectedErrorMessage1a_1 =
        "class test.IAE1_B cannot access its superinterface test.IAE1_A " +
        "(test.IAE1_B is in unnamed module of loader test.IAE_Loader1 @";
    private static String expectedErrorMessage1a_2 =
        "; test.IAE1_A is in unnamed module of loader 'app')";
    private static String expectedErrorMessage1b_1 =
        "class test.IAE1_B cannot access its superinterface test.IAE1_A " +
        "(test.IAE1_B is in unnamed module of loader 'someCLName1' @";
    private static String expectedErrorMessage1b_2 =
        "; test.IAE1_A is in unnamed module of loader 'app')";

    // abstract class
    private static String expectedErrorMessage2_1 =
        "class test.IAE2_B cannot access its abstract superclass test.IAE2_A " +
        "(test.IAE2_B is in unnamed module of loader 'someCLName2' @";
    private static String expectedErrorMessage2_2 =
        "; test.IAE2_A is in unnamed module of loader 'app')";

    // class
    private static String expectedErrorMessage3_1 =
        "class test.IAE3_B cannot access its superclass test.IAE3_A " +
        "(test.IAE3_B is in unnamed module of loader 'someCLName3' @";
    private static String expectedErrorMessage3_2 =
        "; test.IAE3_A is in unnamed module of loader 'app')";

    public static void test123(String loaderName,
                               String expectedErrorMessage_1,
                               String expectedErrorMessage_2,
                               String testClass) throws Exception {
        String[] classNames = { testClass };
        // Some classes under a new Loader.
        ClassLoader l = new IAE_Loader1(loaderName, classNames);

        try {
            l.loadClass(testClass);
            throw new RuntimeException("Expected IllegalAccessError was not thrown.");
        } catch (IllegalAccessError iae) {
            String errorMsg = iae.getMessage();
            if (!(errorMsg.contains(expectedErrorMessage_1) &&
                  errorMsg.contains(expectedErrorMessage_2))) {
                System.out.println("Expected: " + expectedErrorMessage_1 + "@id " + expectedErrorMessage_2 +"\n" +
                                   "but got:  " + errorMsg);
                throw new RuntimeException("Wrong error message of IllegalAccessError.");
            } else {
                System.out.println("Passed with message: " + errorMsg);
            }
        }
    }

    // Generate a class file with the given class name. The class implements Runnable
    // with a run method to invokestatic the given targetClass/targetMethod.
    static byte[] iae4_generateRunner(String className,
                                      String targetClass,
                                      String targetMethod) throws Exception {

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
                                         + ClassWriter.COMPUTE_FRAMES);
        cw.visit(V9,
                ACC_PUBLIC + ACC_SUPER,
                className.replace(".", "/"),
                null,
                "java/lang/Object",
                new String[] { "java/lang/Runnable" });

        // <init>
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // run()
        String tc = targetClass.replace(".", "/");
        mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
        mv.visitMethodInsn(INVOKESTATIC, tc, targetMethod, "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    // Private method that should raise IllegalAccessError when called.
    private static void iae4_m() { }

    private static String expectedErrorMessage4 =
        "class test.Runner4 tried to access private method 'void test.IllegalAccessErrorTest.iae4_m()' " +
        "(test.Runner4 and test.IllegalAccessErrorTest are in unnamed module of loader 'app')";

    // Test according to java/lang/invoke/DefineClassTest.java
    public static void test4_privateMethod() throws Exception {
        final String THIS_PACKAGE = IllegalAccessErrorTest.class.getPackageName();
        final String THIS_CLASS   = IllegalAccessErrorTest.class.getName();
        final String CLASS_NAME   = THIS_PACKAGE + ".Runner4";
        Lookup lookup = lookup();

        // private
        byte[] classBytes = iae4_generateRunner(CLASS_NAME, THIS_CLASS, "iae4_m");
        Class<?> clazz = lookup.defineClass(classBytes);
        Runnable r = (Runnable) clazz.getDeclaredConstructor().newInstance();
        try {
            r.run();
            throw new RuntimeException("Expected IllegalAccessError was not thrown.");
        } catch (IllegalAccessError exc) {
            String errorMsg = exc.getMessage();
            if (!errorMsg.equals(expectedErrorMessage4)) {
                System.out.println("Expected: " + expectedErrorMessage4 + "\n" +
                                   "but got:  " + errorMsg);
                throw new RuntimeException("Wrong error message of IllegalAccessError.");
            }
            System.out.println("Passed with message: " + errorMsg);
        }
    }

    // Generate a class file with the given class name. The class implements Runnable
    // with a run method to invokestatic the given targetClass/targetField.
    static byte[] iae5_generateRunner(String className,
                                      String targetClass,
                                      String targetField) throws Exception {

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
                                         + ClassWriter.COMPUTE_FRAMES);
        cw.visit(V9,
                 ACC_PUBLIC + ACC_SUPER,
                 className.replace(".", "/"),
                 null,
                 "java/lang/Object",
                 new String[] { "java/lang/Runnable" });

        // <init>
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // run()
        String tc = targetClass.replace(".", "/");
        mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
        mv.visitFieldInsn(GETSTATIC, tc, targetField, "I");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    // Private field that should raise IllegalAccessError when accessed.
    private static int iae5_f = 77;

    private static String expectedErrorMessage5 =
        "class test.Runner5 tried to access private field test.IllegalAccessErrorTest.iae5_f " +
        "(test.Runner5 and test.IllegalAccessErrorTest are in unnamed module of loader 'app')";

    // Test according to java/lang/invoke/DefineClassTest.java
    public static void test5_privateField() throws Exception {
        final String THIS_PACKAGE = IllegalAccessErrorTest.class.getPackageName();
        final String THIS_CLASS   = IllegalAccessErrorTest.class.getName();
        final String CLASS_NAME   = THIS_PACKAGE + ".Runner5";
        Lookup lookup = lookup();

        // private
        byte[] classBytes = iae5_generateRunner(CLASS_NAME, THIS_CLASS, "iae5_f");
        Class<?> clazz = lookup.defineClass(classBytes);
        Runnable r = (Runnable) clazz.getDeclaredConstructor().newInstance();
        try {
            r.run();
            throw new RuntimeException("Expected IllegalAccessError was not thrown.");
        } catch (IllegalAccessError exc) {
            String errorMsg = exc.getMessage();
            if (!errorMsg.equals(expectedErrorMessage5)) {
                System.out.println("Expected: " + expectedErrorMessage5 + "\n" +
                                   "but got:  " + errorMsg);
                throw new RuntimeException("Wrong error message of IllegalAccessError.");
            }
            System.out.println("Passed with message: " + errorMsg);
        }
    }

    private static String expectedErrorMessage6 =
        "failed to access class test.IAE6_A from class test.IAE6_B " +
        "(test.IAE6_A is in unnamed module of loader 'app'; test.IAE6_B is in unnamed module of loader 'test6_class_CL' @";

    public static void test6_class() throws Exception {
        ClassLoader base = IllegalAccessErrorTest.class.getClassLoader();
        IAE_Loader2 loader = new IAE_Loader2("test6_class_CL", base.getParent(), base, new String[0],
                new String[] { IAE6_A.class.getName() });
        Class<?> cl = loader.loadClass(IAE6_B.class.getName());
        Method m = cl.getDeclaredMethod("create", new Class[0]);
        m.setAccessible(true);

        try {
            m.invoke(null, new Object[0]);
            throw new RuntimeException("Expected IllegalAccessError was not thrown.");
        } catch (InvocationTargetException e) {
            IllegalAccessError iae = (IllegalAccessError) e.getCause();
            String errorMsg = iae.getMessage();
            if (!errorMsg.contains(expectedErrorMessage6)) {
                System.out.println("Expected: " + expectedErrorMessage6 + "id)\n" +
                                   "but got:  " + errorMsg);
                throw new RuntimeException("Wrong error message of IllegalAccessError.");
            }
            System.out.println("Passed with message: " + errorMsg);
        }
    }

    private static String expectedErrorMessage7_1 =
        "class test.IAE78_B tried to access method 'void test.IAE78_A.<init>()' " +
        "(test.IAE78_B is in unnamed module of loader 'test7_method_CL' @";
    private static String expectedErrorMessage7_2 =
        "; test.IAE78_A is in unnamed module of loader 'app')";

    // Similar to test4.
    public static void test7_method() throws Exception {
        ClassLoader base = IllegalAccessErrorTest.class.getClassLoader();
        IAE_Loader2 loader = new IAE_Loader2("test7_method_CL", base.getParent(), base, new String[0],
                new String[] {IAE78_A.class.getName()});
        Class<?> cl = loader.loadClass(IAE78_B.class.getName());
        Method m = cl.getDeclaredMethod("create", new Class[0]);

        try {
            m.invoke(null, new Object[0]);
        } catch (InvocationTargetException e) {
            IllegalAccessError iae = (IllegalAccessError) e.getCause();
            String errorMsg = iae.getMessage();
            if (!(errorMsg.contains(expectedErrorMessage7_1) &&
                  errorMsg.contains(expectedErrorMessage7_2))) {
                System.out.println("Expected: " + expectedErrorMessage7_1 + "id" + expectedErrorMessage7_2 + "\n" +
                                   "but got:  " + errorMsg);
                throw new RuntimeException("Wrong error message of IllegalAccessError.");
            }
            System.out.println("Passed with message: " + errorMsg);
        }
    }

    private static String expectedErrorMessage8_1 =
        "class test.IAE78_B tried to access field test.IAE78_A.f " +
        "(test.IAE78_B is in unnamed module of loader 'test8_field_CL' @";
    private static String expectedErrorMessage8_2 =
        "; test.IAE78_A is in unnamed module of loader 'app')";

    // Similar to test5.
    public static void test8_field() throws Exception {
        ClassLoader base = IllegalAccessErrorTest.class.getClassLoader();
        IAE_Loader2 loader = new IAE_Loader2("test8_field_CL", base.getParent(), base, new String[0],
                                             new String[] { IAE78_A.class.getName() });
        Class<?> cl = loader.loadClass(IAE78_B.class.getName());
        Method m = cl.getDeclaredMethod("access", new Class[0]);

        try {
            m.invoke(null, new Object[0]);
        }
        catch (InvocationTargetException e) {
            IllegalAccessError iae = (IllegalAccessError) e.getCause();
            String errorMsg = iae.getMessage();
            if (!(errorMsg.contains(expectedErrorMessage8_1) &&
                  errorMsg.contains(expectedErrorMessage8_2))) {
                System.out.println("Expected: " + expectedErrorMessage8_1 + "id" + expectedErrorMessage8_2 + "\n" +
                                   "but got:  " + errorMsg);
                throw new RuntimeException("Wrong error message of IllegalAccessError.");
            }
            System.out.println("Passed with message: " + errorMsg);
        }
    }

    public static void main(String[] args) throws Exception {
        test123(null,          expectedErrorMessage1a_1, expectedErrorMessage1a_2, "test.IAE1_B"); // interface
        test123("someCLName1", expectedErrorMessage1b_1, expectedErrorMessage1b_2, "test.IAE1_B"); // interface
        test123("someCLName2", expectedErrorMessage2_1,  expectedErrorMessage2_2,  "test.IAE2_B"); // abstract class
        test123("someCLName3", expectedErrorMessage3_1,  expectedErrorMessage3_2,  "test.IAE3_B"); // class
        test4_privateMethod();
        test5_privateField();
        test6_class();
        test7_method();
        test8_field();
    }
}

// Class hierarchies for test1.
interface IAE1_A {
    public IAE1_D gen();
}

class IAE1_B implements IAE1_A {
    public IAE1_D gen() {
        return null;
    }
}

abstract class IAE1_C {
}

class IAE1_D extends IAE1_C {
}


// Class hierarchies for test2.
abstract class IAE2_A {
    abstract public IAE2_D gen();
}

class IAE2_B extends IAE2_A {
    public IAE2_D gen() {
        return null;
    }
}

abstract class IAE2_C {
}

class IAE2_D extends IAE2_C {
}


// Class hierarchies for test3.
class IAE3_A {
    public IAE3_D gen() {
        return null;
    };
}

class IAE3_B extends IAE3_A {
    public IAE3_D gen() {
        return null;
    }
}

abstract class IAE3_C {
}

class IAE3_D extends IAE3_C {
}


// Class hierarchies for test6.
class IAE6_A {
    IAE6_A() {
        // Nothing to do.
    }
}

class IAE6_B {
    public static void create() {
        new IAE6_A();
    }
}
