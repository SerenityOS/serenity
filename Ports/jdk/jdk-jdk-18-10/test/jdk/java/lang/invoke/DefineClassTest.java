/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @modules java.base/java.lang:open
 *          java.base/jdk.internal.org.objectweb.asm
 * @run testng/othervm test.DefineClassTest
 * @summary Basic test for java.lang.invoke.MethodHandles.Lookup.defineClass
 */

package test;

import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodHandles.Lookup.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class DefineClassTest {
    private static final String THIS_PACKAGE = DefineClassTest.class.getPackageName();

    /**
     * Test that a class has the same class loader, and is in the same package and
     * protection domain, as a lookup class.
     */
    void testSameAbode(Class<?> clazz, Class<?> lc) {
        assertTrue(clazz.getClassLoader() == lc.getClassLoader());
        assertEquals(clazz.getPackageName(), lc.getPackageName());
        assertTrue(clazz.getProtectionDomain() == lc.getProtectionDomain());
    }

    /**
     * Tests that a class is discoverable by name using Class.forName and
     * lookup.findClass
     */
    void testDiscoverable(Class<?> clazz, Lookup lookup) throws Exception {
        String cn = clazz.getName();
        ClassLoader loader = clazz.getClassLoader();
        assertTrue(Class.forName(cn, false, loader) == clazz);
        assertTrue(lookup.findClass(cn) == clazz);
    }

    /**
     * Basic test of defineClass to define a class in the same package as test.
     */
    @Test
    public void testDefineClass() throws Exception {
        final String CLASS_NAME = THIS_PACKAGE + ".Foo";
        Lookup lookup = lookup();
        Class<?> clazz = lookup.defineClass(generateClass(CLASS_NAME));

        // test name
        assertEquals(clazz.getName(), CLASS_NAME);

        // test loader/package/protection-domain
        testSameAbode(clazz, lookup.lookupClass());

        // test discoverable
        testDiscoverable(clazz, lookup);

        // attempt defineClass again
        try {
            lookup.defineClass(generateClass(CLASS_NAME));
            assertTrue(false);
        } catch (LinkageError expected) { }
    }

    /**
     * Test public/package/protected/private access from class defined with defineClass.
     */
    @Test
    public void testAccess() throws Exception {
        final String THIS_CLASS = this.getClass().getName();
        final String CLASS_NAME = THIS_PACKAGE + ".Runner";
        Lookup lookup = lookup();

        // public
        byte[] classBytes = generateRunner(CLASS_NAME + nextNumber(), THIS_CLASS, "method1");
        testInvoke(lookup.defineClass(classBytes));

        // package
        classBytes = generateRunner(CLASS_NAME + nextNumber(), THIS_CLASS, "method2");
        testInvoke(lookup.defineClass(classBytes));

        // protected (same package)
        classBytes = generateRunner(CLASS_NAME + nextNumber(), THIS_CLASS, "method3");
        testInvoke(lookup.defineClass(classBytes));

        // private
        classBytes = generateRunner(CLASS_NAME + nextNumber(), THIS_CLASS, "method4");
        Class<?> clazz = lookup.defineClass(classBytes);
        Runnable r = (Runnable) clazz.newInstance();
        try {
            r.run();
            assertTrue(false);
        } catch (IllegalAccessError expected) { }
    }

    public static void method1() { }
    static void method2() { }
    protected static void method3() { }
    private static void method4() { }

    void testInvoke(Class<?> clazz) throws Exception {
        Object obj = clazz.newInstance();
        ((Runnable) obj).run();
    }

    /**
     * Test that defineClass does not run the class initializer
     */
    @Test
    public void testInitializerNotRun() throws Exception {
        final String THIS_CLASS = this.getClass().getName();
        final String CLASS_NAME = THIS_PACKAGE + ".ClassWithClinit";

        byte[] classBytes = generateClassWithInitializer(CLASS_NAME, THIS_CLASS, "fail");
        Class<?> clazz = lookup().defineClass(classBytes);

        // trigger initializer to run
        try {
            clazz.newInstance();
            assertTrue(false);
        } catch (ExceptionInInitializerError e) {
            assertTrue(e.getCause() instanceof IllegalCallerException);
        }
    }

    static void fail() { throw new IllegalCallerException(); }


    /**
     * Test defineClass to define classes in a package containing classes with
     * different protection domains.
     */
    @Test
    public void testTwoProtectionDomains() throws Exception {
        Path here = Paths.get("");

        // p.C1 in one exploded directory
        Path dir1 = Files.createTempDirectory(here, "classes");
        Path p = Files.createDirectory(dir1.resolve("p"));
        Files.write(p.resolve("C1.class"), generateClass("p.C1"));
        URL url1 = dir1.toUri().toURL();

        // p.C2 in another exploded directory
        Path dir2 = Files.createTempDirectory(here, "classes");
        p = Files.createDirectory(dir2.resolve("p"));
        Files.write(p.resolve("C2.class"), generateClass("p.C2"));
        URL url2 = dir2.toUri().toURL();

        // load p.C1 and p.C2
        ClassLoader loader = new URLClassLoader(new URL[] { url1, url2 });
        Class<?> target1 = Class.forName("p.C1", false, loader);
        Class<?> target2 = Class.forName("p.C2", false, loader);
        assertTrue(target1.getClassLoader() == loader);
        assertTrue(target1.getClassLoader() == loader);
        assertNotEquals(target1.getProtectionDomain(), target2.getProtectionDomain());

        // protection domain 1
        Lookup lookup1 = privateLookupIn(target1, lookup());

        Class<?> clazz = lookup1.defineClass(generateClass("p.Foo"));
        testSameAbode(clazz, lookup1.lookupClass());
        testDiscoverable(clazz, lookup1);

        // protection domain 2
        Lookup lookup2 = privateLookupIn(target2, lookup());

        clazz = lookup2.defineClass(generateClass("p.Bar"));
        testSameAbode(clazz, lookup2.lookupClass());
        testDiscoverable(clazz, lookup2);
    }

    /**
     * Test defineClass defining a class to the boot loader
     */
    @Test
    public void testBootLoader() throws Exception {
        Lookup lookup = privateLookupIn(Thread.class, lookup());
        assertTrue(lookup.getClass().getClassLoader() == null);

        Class<?> clazz = lookup.defineClass(generateClass("java.lang.Foo"));
        assertEquals(clazz.getName(), "java.lang.Foo");
        testSameAbode(clazz, Thread.class);
        testDiscoverable(clazz, lookup);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void testWrongPackage() throws Exception {
        lookup().defineClass(generateClass("other.C"));
    }

    @Test(expectedExceptions = { IllegalAccessException.class })
    public void testNoPackageAccess() throws Exception {
        Lookup lookup = lookup().dropLookupMode(PACKAGE);
        lookup.defineClass(generateClass(THIS_PACKAGE + ".C"));
    }

    @Test(expectedExceptions = { ClassFormatError.class })
    public void testTruncatedClassFile() throws Exception {
        lookup().defineClass(new byte[0]);
    }

    @Test(expectedExceptions = { NullPointerException.class })
    public void testNull() throws Exception {
        lookup().defineClass(null);
    }

    @Test(expectedExceptions = { NoClassDefFoundError.class })
    public void testLinking() throws Exception {
        lookup().defineClass(generateNonLinkableClass(THIS_PACKAGE + ".NonLinkableClass"));
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void testModuleInfo() throws Exception {
        lookup().defineClass(generateModuleInfo());
    }

    /**
     * Generates a class file with the given class name
     */
    byte[] generateClass(String className) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
                                         + ClassWriter.COMPUTE_FRAMES);
        cw.visit(V9,
                ACC_PUBLIC + ACC_SUPER,
                className.replace(".", "/"),
                null,
                "java/lang/Object",
                null);

        // <init>
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * Generate a class file with the given class name. The class implements Runnable
     * with a run method to invokestatic the given targetClass/targetMethod.
     */
    byte[] generateRunner(String className,
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

    /**
     * Generate a class file with the given class name. The class will initializer
     * to invokestatic the given targetClass/targetMethod.
     */
    byte[] generateClassWithInitializer(String className,
                                        String targetClass,
                                        String targetMethod) throws Exception {

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
                                         + ClassWriter.COMPUTE_FRAMES);
        cw.visit(V9,
                ACC_PUBLIC + ACC_SUPER,
                className.replace(".", "/"),
                null,
                "java/lang/Object",
                null);

        // <init>
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // <clinit>
        String tc = targetClass.replace(".", "/");
        mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitMethodInsn(INVOKESTATIC, tc, targetMethod, "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * Generates a non-linkable class file with the given class name
     */
    byte[] generateNonLinkableClass(String className) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
                + ClassWriter.COMPUTE_FRAMES);
        cw.visit(V14,
                ACC_PUBLIC + ACC_SUPER,
                className.replace(".", "/"),
                null,
                "MissingSuperClass",
                null);

        // <init>
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "MissingSuperClass", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * Generates a class file with the given class name
     */
    byte[] generateModuleInfo() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
                + ClassWriter.COMPUTE_FRAMES);
        cw.visit(V14,
                ACC_MODULE,
                "module-info",
                null,
                null,
                null);

        cw.visitEnd();
        return cw.toByteArray();
    }

    private int nextNumber() {
        return ++nextNumber;
    }

    private int nextNumber;
}
