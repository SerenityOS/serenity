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

/*
 * @test
 * @bug 8266925
 * @summary hidden class members can't be statically invocable
 * @modules java.base/jdk.internal.misc java.base/jdk.internal.org.objectweb.asm
 * @build java.base/*
 * @run testng StaticInvocableTest
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.LookupHelper;
import jdk.internal.org.objectweb.asm.*;
import org.testng.annotations.Test;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

public class StaticInvocableTest {
    public static void main(String[] args) throws Throwable {
        StaticInvocableTest test = new StaticInvocableTest();
        test.testJavaLang();
        test.testJavaUtil();
        test.testJdkInternalMisc();
        test.testJavaLangInvoke();
        test.testProhibitedJavaPkg();
        System.out.println("TEST PASSED");
    }

    // Test hidden classes from different packages
    // (see j.l.i.InvokerBytecodeGenerator::isStaticallyInvocable).
    @Test public void testJavaLang()        throws Throwable { test("java/lang");         }
    @Test public void testJavaUtil()        throws Throwable { test("java/util");         }
    @Test public void testJdkInternalMisc() throws Throwable { test("jdk/internal/misc"); }
    @Test public void testJavaLangInvoke()  throws Throwable { test("java/lang/invoke");  }
    @Test public void testProhibitedJavaPkg() throws Throwable {
       try {
           test("java/prohibited");
       } catch (IllegalArgumentException e) {
           return;
       }
       throw new RuntimeException("Expected SecurityException");
     }

    private static void test(String pkg) throws Throwable {
        byte[] bytes = dumpClass(pkg);
        Lookup lookup;
        if (pkg.equals("java/prohibited")) {
            StaticInvocableTest sampleclass = new StaticInvocableTest();
            lookup = LookupHelper.newLookup(sampleclass.getClass());
        } else if (pkg.equals("java/lang")) {
            lookup = LookupHelper.newLookup(Object.class);
        } else if (pkg.equals("java/util")) {
            lookup = LookupHelper.newLookup(java.util.ArrayList.class);
        } else if (pkg.equals("jdk/internal/misc")) {
            lookup = LookupHelper.newLookup(jdk.internal.misc.Signal.class);
        } else if (pkg.equals("java/lang/invoke")) {
            lookup = LookupHelper.newLookup(java.lang.invoke.CallSite.class);
        } else {
            throw new RuntimeException("Unexpected pkg: " + pkg);
        }

        // Define hidden class
        Lookup l = lookup.defineHiddenClass(bytes, true);

        MethodType t = MethodType.methodType(Object.class, int.class);
        MethodHandle target = l.findStatic(l.lookupClass(), "get", t);

        // Wrap target into LF (convert) to get "target" referenced from LF
        MethodHandle wrappedMH = target.asType(MethodType.methodType(Object.class, Integer.class));

        // Invoke enough times to provoke LF compilation to bytecode.
        for (int i = 0; i<100; i++) {
            Object r = wrappedMH.invokeExact((Integer)1);
        }
    }

    /*
     * Constructs bytecode for the following class:
     * public class pkg.MyClass {
     *     MyClass() {}
     *     public Object get(int i) { return null; }
     * }
     */
    public static byte[] dumpClass(String pkg) {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(52, ACC_SUPER | ACC_PUBLIC, pkg+"/MyClass", null, "java/lang/Object", null);
        {
            mv = cw.visitMethod(0, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "get", "(I)Ljava/lang/Object;", null, null);
            mv.visitCode();
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }
}
