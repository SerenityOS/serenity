/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8025260 8016839 8046171
 * @summary Ensure that correct exceptions are thrown, not NullPointerException
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @library / .
 *
 * @build p.*
 * @run main/othervm compiler.jsr292.methodHandleExceptions.TestAMEnotNPE
 * @run main/othervm -Xint compiler.jsr292.methodHandleExceptions.TestAMEnotNPE
 * @run main/othervm -Xcomp compiler.jsr292.methodHandleExceptions.TestAMEnotNPE
 */

// Since this test was written the specification for interface method selection has been
// revised (JEP 181 - Nestmates) so that private methods are never selected, as they never
// override any inherited method. So where a private method was previously selected
// and then resulted in IllegalAccessError, the private method is skipped and the invocation
// will either succeed or fail based on what other implementations are found in the inheritance
// hierarchy. This is explained for each test below.

package compiler.jsr292.methodHandleExceptions;

import p.Dok;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_STATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_SUPER;
import static jdk.internal.org.objectweb.asm.Opcodes.ALOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.ILOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static jdk.internal.org.objectweb.asm.Opcodes.IRETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.LLOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.RETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.V1_8;

public class TestAMEnotNPE {

    static boolean writeJarFiles = false;
    static boolean readJarFiles = false;

    /**
     * Optional command line parameter (any case-insensitive prefix of)
     * "writejarfiles" or "readjarfiles".
     *
     * "Writejarfiles" creates a jar file for each different set of tested classes.
     * "Readjarfiles" causes the classloader to use the copies of the classes
     * found in the corresponding jar files.
     *
     * Jarfilenames look something like pD_ext_pF (p.D extends p.F)
     * and qD_m_pp_imp_pI (q.D with package-private m implements p.I)
     *
     */
    public static void main(String args[]) throws Throwable {
        ArrayList<Throwable> lt = new ArrayList<Throwable>();

        if (args.length > 0) {
            String a0 = args[0].toLowerCase();
            if (a0.length() > 0) {
                writeJarFiles = ("writejarfiles").startsWith(a0);
                readJarFiles = ("readjarfiles").startsWith(a0);
            }
            if (!(writeJarFiles || readJarFiles)) {
                throw new Error("Command line parameter (if any) should be prefix of writeJarFiles or readJarFiles");
            }
        }

        System.out.println("TRYING p.D.m PRIVATE interface-invoked as p.I.m, p.D extends p.F, p.F.m FINAL");
        System.out.println(" - should invoke p.F.m as private p.D.m is skipped for selection");
        tryAndCheckThrown(lt, bytesForDprivateSubWhat("p/F"),
                          "p.D extends p.F (p.F implements p.I, FINAL public m), private m",
                          null /* should succeed */, "pD_ext_pF");
        System.out.println();

        System.out.println("TRYING p.D.m PRIVATE interface-invoked as p.I.m, p.D extends p.E");
        System.out.println(" - should invoke p.E.m as private p.D.m is skipped for selection");
        tryAndCheckThrown(lt, bytesForDprivateSubWhat("p/E"),
                          "p.D extends p.E (p.E implements p.I, public m), private m",
                          null /* should succeed */, "pD_ext_pE");

        System.out.println("TRYING p.D.m ABSTRACT interface-invoked as p.I.m");
        tryAndCheckThrown(lt, bytesForD(),
                          "D extends abstract C, no m",
                          AbstractMethodError.class, "pD_ext_pC");

        System.out.println("TRYING q.D.m PACKAGE interface-invoked as p.I.m");
        tryAndCheckThrown(lt, "q.D", bytesForDsomeAccess("q/D", 0),
                          "q.D implements p.I, protected m",
                          IllegalAccessError.class, "qD_m_pp_imp_pI");

        // Note jar file name is used in the plural-arg case.
        System.out.println("TRYING p.D.m PRIVATE interface-invoked as p.I.m");
        System.out.println(" - should invoke p.I.m as private p.D.m is skipped for selection");
        tryAndCheckThrown(lt, bytesForDsomeAccess("p/D", ACC_PRIVATE),
                          "p.D implements p.I, private m",
                          null /* should succeed */, "pD_m_pri_imp_pI");

        // Plural-arg test.
        System.out.println("TRYING p.D.m PRIVATE MANY ARG interface-invoked as p.I.m");
        System.out.println(" - should invoke p.I.m as private p.D.m is skipped for selection");
        tryAndCheckThrownMany(lt, bytesForDsomeAccess("p/D", ACC_PRIVATE),
                              "p.D implements p.I, private m", null /* should succeed */);

        if (lt.size() > 0) {
            System.out.flush();
            Thread.sleep(250); // This de-interleaves output and error in Netbeans, sigh.
            for (Throwable th : lt)
                System.err.println(th);
            throw new Error("Test failed, there were " + lt.size() + " failures listed above");
        } else {
            System.out.println("ALL PASS, HOORAY!");
        }
    }

    /**
     * The bytes for D, a NOT abstract class extending abstract class C without
     * supplying an implementation for abstract method m. There is a default
     * method in the interface I, but it should lose to the abstract class.
     *
     * @return
     * @throws Exception
     */
    public static byte[] bytesForD() throws Exception {

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES
                | ClassWriter.COMPUTE_MAXS);
        MethodVisitor mv;

        cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, "p/D", null, "p/C", null);

        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "p/C", "<init>", "()V");
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    /**
     * The bytes for D, implements I, does not extend C, declares m()I with
     * access method_acc.
     *
     * @param d_name Name of class defined
     * @param method_acc Accessibility of that class's method m.
     * @return
     * @throws Exception
     */
    public static byte[] bytesForDsomeAccess(String d_name, int method_acc) throws Exception {
        return bytesForSomeDsubSomethingSomeAccess(d_name, "java/lang/Object", method_acc);
    }

    /**
     * The bytes for D implements I, extends some class, declares m()I as
     * private.
     *
     * Invokeinterface of I.m applied to this D should throw IllegalAccessError
     *
     * @param sub_what The name of the class that D will extend.
     * @return
     * @throws Exception
     */
    public static byte[] bytesForDprivateSubWhat(String sub_what) throws Exception {
        return bytesForSomeDsubSomethingSomeAccess("p/D", sub_what, ACC_PRIVATE);
    }

    /**
     * Returns the bytes for a class with name d_name (presumably "D" in some
     * package), extending some class with name sub_what, implementing p.I,
     * and defining two methods m() and m(11args) with access method_acc.
     *
     * @param d_name      Name of class that is defined
     * @param sub_what    Name of class that it extends
     * @param method_acc  Accessibility of method(s) m in defined class.
     * @return
     * @throws Exception
     */
    public static byte[] bytesForSomeDsubSomethingSomeAccess
    (String d_name, String sub_what, int method_acc)
            throws Exception {

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES
                | ClassWriter.COMPUTE_MAXS);
        MethodVisitor mv;
        String[] interfaces = {"p/I"};

        cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, d_name, null, sub_what, interfaces);
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, sub_what, "<init>", "()V");
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        // int m() {return 3;}
        {
            mv = cw.visitMethod(method_acc, "m", "()I", null, null);
            mv.visitCode();
            mv.visitLdcInsn(new Integer(3));
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        // int m(11args) {return 3;}
        {
            mv = cw.visitMethod(method_acc, "m", "(BCSIJ"
                    + "Ljava/lang/Object;"
                    + "Ljava/lang/Object;"
                    + "Ljava/lang/Object;"
                    + "Ljava/lang/Object;"
                    + "Ljava/lang/Object;"
                    + "Ljava/lang/Object;"
                    + ")I", null, null);
            mv.visitCode();
            mv.visitLdcInsn(new Integer(3));
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * The bytecodes for a class p/T defining a methods test() and test(11args)
     * that contain an invokeExact of a particular methodHandle, I.m.
     *
     * Test will be passed values that may imperfectly implement I,
     * and thus may in turn throw exceptions.
     *
     * @return
     * @throws Exception
     */
    public static byte[] bytesForT() throws Exception {

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES
                | ClassWriter.COMPUTE_MAXS);
        MethodVisitor mv;

        cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, "p/T", null, "java/lang/Object", null);
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        // static int test(I)
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "test", "(Lp/I;)I", null, null);
            mv.visitCode();
            mv.visitLdcInsn(new Handle(Opcodes.H_INVOKEINTERFACE, "p/I", "m", "()I"));
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle",
                    "invokeExact", "(Lp/I;)I");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        // static int test(I,11args)
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "test", "(Lp/I;BCSIJLjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)I", null, null);
            mv.visitCode();
            mv.visitLdcInsn(new Handle(Opcodes.H_INVOKEINTERFACE, "p/I", "m", "(BCSIJLjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)I"));
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitVarInsn(ILOAD, 3);
            mv.visitVarInsn(ILOAD, 4);
            mv.visitVarInsn(LLOAD, 5);
            mv.visitVarInsn(ALOAD, 7);
            mv.visitVarInsn(ALOAD, 8);
            mv.visitVarInsn(ALOAD, 9);
            mv.visitVarInsn(ALOAD, 10);
            mv.visitVarInsn(ALOAD, 11);
            mv.visitVarInsn(ALOAD, 12);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle",
                    "invokeExact", "(Lp/I;BCSIJLjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)I");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static void tryAndCheckThrown(
            List<Throwable> lt, byte[] dBytes, String what, Class<?> expected, String jar_name)
            throws Throwable {
        tryAndCheckThrown(lt, "p.D", dBytes, what, expected, jar_name);
    }

    private static void tryAndCheckThrown(List<Throwable> lt, String d_name, byte[] dBytes, String what, Class<?> expected, String jar_name)
            throws Throwable {

        System.out.println("Methodhandle invokeExact I.m() for instance of " + what);
        ByteClassLoader bcl1 = new ByteClassLoader(jar_name, readJarFiles, writeJarFiles);
        try {
            Class<?> d1 = bcl1.loadBytes(d_name, dBytes);
            Class<?> t1 = bcl1.loadBytes("p.T", bytesForT());
            invokeTest(t1, d1, expected, lt);
        } finally {
            // Not necessary for others -- all class files are written in this call.
            // (unless the VM crashes first).
            bcl1.close();
        }

        System.out.println("Reflection invoke I.m() for instance of " + what);
        ByteClassLoader bcl3 = new ByteClassLoader(jar_name, readJarFiles, false);
        Class<?> d3 = bcl3.loadBytes(d_name, dBytes);
        Class<?> t3 = bcl3.loadClass("p.Treflect");
        invokeTest(t3, d3, expected, lt);

        System.out.println("Bytecode invokeInterface I.m() for instance of " + what);
        ByteClassLoader bcl2 = new ByteClassLoader(jar_name, readJarFiles, false);
        Class<?> d2 = bcl2.loadBytes(d_name, dBytes);
        Class<?> t2 = bcl2.loadClass("p.Tdirect");
        badGoodBadGood(t2, d2, expected, lt);
    }

    private static void invokeTest(Class<?> t, Class<?> d, Class<?> expected, List<Throwable> lt)
            throws Throwable {
        try {
            Method m = t.getMethod("test", p.I.class);
            Object o = d.newInstance();
            Object result = m.invoke(null, o);
            if (expected != null) {
                System.out.println("FAIL, Expected " + expected.getName()
                        + " wrapped in InvocationTargetException, but nothing was thrown");
                lt.add(new Error("Exception " + expected.getName() + " was not thrown"));
            } else {
                System.out.println("PASS, saw expected return.");
            }
        } catch (InvocationTargetException e) {
            Throwable th = e.getCause();
            th.printStackTrace(System.out);
            if (expected != null) {
                if (expected.isInstance(th)) {
                    System.out.println("PASS, saw expected exception (" + expected.getName() + ").");
                } else {
                    System.out.println("FAIL, Expected " + expected.getName()
                            + " wrapped in InvocationTargetException, saw " + th);
                    lt.add(th);
                }
            } else {
                System.out.println("FAIL, expected no exception, saw " + th);
                lt.add(th);
            }
        }
        System.out.println();
    }

    /* Many-arg versions of above */
    private static void tryAndCheckThrownMany(List<Throwable> lt, byte[] dBytes, String what, Class<?> expected)
            throws Throwable {

        System.out.println("Methodhandle invokeExact I.m(11params) for instance of " + what);
        ByteClassLoader bcl1 = new ByteClassLoader("p.D", readJarFiles, false);
        try {
            Class<?> d1 = bcl1.loadBytes("p.D", dBytes);
            Class<?> t1 = bcl1.loadBytes("p.T", bytesForT());
            invokeTestMany(t1, d1, expected, lt);
        } finally {
            bcl1.close(); // Not necessary for others -- all class files are written in this call.
        }

        {
            System.out.println("Bytecode invokeInterface I.m(11params) for instance of " + what);
            ByteClassLoader bcl2 = new ByteClassLoader("pD_m_pri_imp_pI", readJarFiles, false);
            Class<?> d2 = bcl2.loadBytes("p.D", dBytes);
            Class<?> t2 = bcl2.loadClass("p.Tdirect");
            badGoodBadGoodMany(t2, d2, expected, lt);

        }
        {
            System.out.println("Reflection invokeInterface I.m(11params) for instance of " + what);
            ByteClassLoader bcl2 = new ByteClassLoader("pD_m_pri_imp_pI", readJarFiles, false);
            Class<?> d2 = bcl2.loadBytes("p.D", dBytes);
            Class<?> t2 = bcl2.loadClass("p.Treflect");
            invokeTestMany(t2, d2, expected, lt);
        }
    }

    private static void invokeTestMany(Class<?> t, Class<?> d, Class<?> expected, List<Throwable> lt)
            throws Throwable {
        try {
            Method m = t.getMethod("test", p.I.class,
                    Byte.TYPE, Character.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE,
                    Object.class, Object.class, Object.class,
                    Object.class, Object.class, Object.class);
            Object o = d.newInstance();
            Byte b = 1;
            Character c = 2;
            Short s = 3;
            Integer i = 4;
            Long j = 5L;
            Object o1 = b;
            Object o2 = c;
            Object o3 = s;
            Object o4 = i;
            Object o5 = j;
            Object o6 = "6";

            Object result = m.invoke(null, o, b, c, s, i, j,
                    o1, o2, o3, o4, o5, o6);
            if (expected != null) {
                System.out.println("FAIL, Expected " + expected.getName()
                        + " wrapped in InvocationTargetException, but nothing was thrown");
                lt.add(new Error("Exception " + expected.getName()
                        + " was not thrown"));
            } else {
                System.out.println("PASS, saw expected return.");
            }
        } catch (InvocationTargetException e) {
            Throwable th = e.getCause();
            th.printStackTrace(System.out);
            if (expected != null) {
                if (expected.isInstance(th)) {
                    System.out.println("PASS, saw expected exception ("
                            + expected.getName() + ").");
                } else {
                    System.out.println("FAIL, Expected " + expected.getName()
                            + " wrapped in InvocationTargetException, saw " + th);
                    lt.add(th);
                }
            } else {
                System.out.println("FAIL, expected no exception, saw " + th);
                lt.add(th);
            }
        }
        System.out.println();
    }

    /**
     * This tests a peculiar idiom for tickling the bug on older VMs that lack
     * methodhandles.  The bug (if not fixed) acts in the following way:
     *
     *  When a broken receiver is passed to the first execution of an invokeinterface
     * bytecode, the illegal access is detected before the effects of resolution are
     * cached for later use, and so repeated calls with a broken receiver will always
     * throw the correct error.
     *
     * If, however, a good receiver is passed to the invokeinterface, the effects of
     * resolution will be successfully cached.  A subsequent execution with a broken
     * receiver will reuse the cached information, skip the detailed resolution work,
     * and instead encounter a null pointer.  By convention, that is the encoding for a
     * missing abstract method, and an AbstractMethodError is thrown -- not the expected
     * IllegalAccessError.
     *
     * @param t2 Test invocation class
     * @param d2 Test receiver class
     * @param expected expected exception type
     * @param lt list of unexpected throwables seen
     */
    private static void badGoodBadGood(Class<?> t2, Class<?> d2, Class<?> expected, List<Throwable> lt)
            throws Throwable {
        System.out.println("  Error input 1st time");
        invokeTest(t2, d2, expected, lt);
        System.out.println("  Good input (instance of Dok)");
        invokeTest(t2, Dok.class, null, lt);
        System.out.println("  Error input 2nd time");
        invokeTest(t2, d2, expected, lt);
        System.out.println("  Good input (instance of Dok)");
        invokeTest(t2, Dok.class, null, lt);
    }

    private static void badGoodBadGoodMany(Class<?> t2, Class<?> d2, Class<?> expected, List<Throwable> lt)
            throws Throwable {
        System.out.println("  Error input 1st time");
        invokeTestMany(t2, d2, expected, lt);
        System.out.println("  Good input (instance of Dok)");
        invokeTestMany(t2, Dok.class, null, lt);
        System.out.println("  Error input 2nd time");
        invokeTestMany(t2, d2, expected, lt);
        System.out.println("  Good input (instance of Dok)");
        invokeTestMany(t2, Dok.class, null, lt);
    }
}
