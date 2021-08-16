/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @modules java.base/jdk.internal.org.objectweb.asm
 * @build  HiddenNestmateTest
 * @run testng/othervm HiddenNestmateTest
 */

import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import java.util.Arrays;

import jdk.internal.org.objectweb.asm.*;
import org.testng.annotations.Test;

import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*;
import static java.lang.invoke.MethodHandles.Lookup.*;

import static jdk.internal.org.objectweb.asm.Opcodes.*;
import static org.testng.Assert.*;

public class HiddenNestmateTest {
    private static final byte[] bytes = classBytes("HiddenInjected");

    private static void assertNestmate(Lookup lookup) {
        assertTrue((lookup.lookupModes() & PRIVATE) != 0);
        assertTrue((lookup.lookupModes() & MODULE) != 0);

        Class<?> hiddenClass = lookup.lookupClass();
        Class<?> nestHost = hiddenClass.getNestHost();
        assertTrue(hiddenClass.isHidden());
        assertTrue(nestHost == MethodHandles.lookup().lookupClass());

        // hidden nestmate is not listed in the return array of getNestMembers
        assertTrue(Stream.of(nestHost.getNestMembers()).noneMatch(k -> k == hiddenClass));
        assertTrue(hiddenClass.isNestmateOf(lookup.lookupClass()));
        assertTrue(Arrays.equals(hiddenClass.getNestMembers(), nestHost.getNestMembers()));
    }

    /*
     * Test a hidden class to have no access to private members of another class
     */
    @Test
    public void hiddenClass() throws Throwable {
        // define a hidden class
        Lookup lookup = MethodHandles.lookup().defineHiddenClass(bytes, false);
        Class<?> c = lookup.lookupClass();
        assertTrue(lookup.hasFullPrivilegeAccess());
        assertTrue((lookup.lookupModes() & ORIGINAL) == ORIGINAL);
        assertTrue(c.getNestHost() == c);  // host of its own nest
        assertTrue(c.isHidden());

        // invoke int test(HiddenNestmateTest o) via MethodHandle
        MethodHandle ctor = lookup.findConstructor(c, MethodType.methodType(void.class));
        MethodHandle mh = lookup.findVirtual(c, "test", MethodType.methodType(int.class, HiddenNestmateTest.class));
        try {
            int x = (int) mh.bindTo(ctor.invoke()).invokeExact(this);
            throw new RuntimeException("should fail when accessing HiddenNestmateTest.privMethod()");
        } catch (IllegalAccessError e) {}

        // invoke int test(HiddenNestmateTest o)
        try {
            int x1 = testInjectedClass(c);
            throw new RuntimeException("should fail when accessing HiddenNestmateTest.privMethod()");
        } catch (IllegalAccessError e) {}
    }

    /*
     * Test a hidden class to have access to private members of its nestmates
     */
    @Test
    public void hiddenNestmate() throws Throwable {
        // define a hidden nestmate class
        Lookup lookup = MethodHandles.lookup().defineHiddenClass(bytes, false, NESTMATE);
        Class<?> c = lookup.lookupClass();
        assertNestmate(lookup);

        // invoke int test(HiddenNestmateTest o) via MethodHandle
        MethodHandle ctor = lookup.findConstructor(c, MethodType.methodType(void.class));
        MethodHandle mh = lookup.findVirtual(c, "test", MethodType.methodType(int.class, HiddenNestmateTest.class));
        int x = (int)mh.bindTo(ctor.invoke()).invokeExact( this);
        assertTrue(x == privMethod());

        // invoke int test(HiddenNestmateTest o)
        int x1 = testInjectedClass(c);
        assertTrue(x1 == privMethod());
    }

    /*
     * Test a hidden class created with NESTMATE and STRONG option is a nestmate
     */
    @Test
    public void hiddenStrongClass() throws Throwable {
        // define a hidden class strongly referenced by the class loader
        Lookup lookup = MethodHandles.lookup().defineHiddenClass(bytes, false, NESTMATE, STRONG);
        assertNestmate(lookup);
    }

    /*
     * Fail to create a hidden class if dropping PRIVATE lookup mode
     */
    @Test(expectedExceptions = IllegalAccessException.class)
    public void noPrivateLookupAccess() throws Throwable {
        Lookup lookup = MethodHandles.lookup().dropLookupMode(Lookup.PRIVATE);
        lookup.defineHiddenClass(bytes, false, NESTMATE);
    }

    public void teleportToNestmate() throws Throwable {
        Lookup lookup = MethodHandles.lookup().defineHiddenClass(bytes, false, NESTMATE);
        assertNestmate(lookup);

        // Teleport to a hidden nestmate
        Lookup lc =  MethodHandles.lookup().in(lookup.lookupClass());
        assertTrue((lc.lookupModes() & PRIVATE) != 0);
        assertTrue((lc.lookupModes() & ORIGINAL) == 0);

        Lookup lc2 = lc.defineHiddenClass(bytes, false, NESTMATE);
        assertNestmate(lc2);
    }

    /*
     * Fail to create a hidden class in a different package from the lookup class' package
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void notSamePackage() throws Throwable {
        MethodHandles.lookup().defineHiddenClass(classBytes("p/HiddenInjected"), false, NESTMATE);
    }

    /*
     * invoke int test(HiddenNestmateTest o) method defined in the injected class
     */
    private int testInjectedClass(Class<?> c) throws Throwable {
        try {
            Method m = c.getMethod("test", HiddenNestmateTest.class);
            return (int) m.invoke(c.newInstance(), this);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static byte[] classBytes(String classname) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES);
        MethodVisitor mv;

        cw.visit(V12, ACC_FINAL, classname, null, "java/lang/Object", null);

        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        {
            // access a private member of the nest host class
            mv = cw.visitMethod(ACC_PUBLIC, "test", "(LHiddenNestmateTest;)I", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "HiddenNestmateTest", "privMethod", "()I");
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    private int privMethod() { return 1234; }
}
