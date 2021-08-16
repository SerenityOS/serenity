/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8057967
 * @requires vm.opt.final.ClassUnloading
 * @modules java.base/jdk.internal.misc
 *          java.base/jdk.internal.org.objectweb.asm
 * @library patches /
 *
 * @build java.base/java.lang.invoke.MethodHandleHelper
 * @run main/bootclasspath/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -Xlog:class+unload
 *                                 -XX:+PrintCompilation -XX:+TraceDependencies -XX:+TraceReferenceGC
 *                                 -verbose:gc
 *                                 compiler.jsr292.CallSiteDepContextTest
 */

package compiler.jsr292;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.MethodVisitor;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleHelper;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_STATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_SUPER;
import static jdk.internal.org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.IRETURN;

public class CallSiteDepContextTest {
    static final MethodHandles.Lookup LOOKUP = MethodHandleHelper.IMPL_LOOKUP;
    static final String           CLASS_NAME = "compiler/jsr292/Test";
    static final String          METHOD_NAME = "m";
    static final MethodType TYPE = MethodType.methodType(int.class);

    static MutableCallSite mcs;
    static MethodHandle bsmMH;

    static {
        try {
            bsmMH = LOOKUP.findStatic(
                    CallSiteDepContextTest.class, "bootstrap",
                    MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class));
        } catch(Throwable e) {
            throw new InternalError(e);
        }
    }

    public static CallSite bootstrap(MethodHandles.Lookup caller,
                                     String invokedName,
                                     MethodType invokedType) {
        return mcs;
    }

    static class T {
        static int f1() { return 1; }
        static int f2() { return 2; }
    }

    static byte[] getClassFile(String suffix) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        MethodVisitor mv;
        cw.visit(52, ACC_PUBLIC | ACC_SUPER, CLASS_NAME + suffix, null, "java/lang/Object", null);
        {
            mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, METHOD_NAME, TYPE.toMethodDescriptorString(), null, null);
            mv.visitCode();
            Handle bsm = new Handle(H_INVOKESTATIC,
                    CallSiteDepContextTest.class.getName().replace(".", "/"),
                    "bootstrap",
                    bsmMH.type().toMethodDescriptorString());
            mv.visitInvokeDynamicInsn("methodName", TYPE.toMethodDescriptorString(), bsm);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static void execute(int expected, MethodHandle... mhs) throws Throwable {
        for (int i = 0; i < 20_000; i++) {
            for (MethodHandle mh : mhs) {
                int r = (int) mh.invokeExact();
                if (r != expected) {
                    throw new Error(r + " != " + expected);
                }
            }
        }
    }

    public static void testHiddenDepField() {
        try {
            Field f = MethodHandleHelper.MHN_CALL_SITE_CONTEXT_CLASS.getDeclaredField("vmdependencies");
            throw new AssertionError("Context.dependencies field should be hidden");
        } catch(NoSuchFieldException e) { /* expected */ }
    }

    public static void testSharedCallSite() throws Throwable {
        Lookup lookup = MethodHandles.lookup();
        Class<?> cls1 = lookup.defineHiddenClass(getClassFile("CS_1"), true).lookupClass();
        Class<?> cls2 = lookup.defineHiddenClass(getClassFile("CS_2"), true).lookupClass();

        MethodHandle[] mhs = new MethodHandle[] {
                LOOKUP.findStatic(cls1, METHOD_NAME, TYPE),
                LOOKUP.findStatic(cls2, METHOD_NAME, TYPE)
        };

        mcs = new MutableCallSite(LOOKUP.findStatic(T.class, "f1", TYPE));
        execute(1, mhs);
        mcs.setTarget(LOOKUP.findStatic(T.class, "f2", TYPE));
        execute(2, mhs);
    }

    public static void testNonBoundCallSite() throws Throwable {
        mcs = new MutableCallSite(LOOKUP.findStatic(T.class, "f1", TYPE));

        // mcs.context == null
        MethodHandle mh = mcs.dynamicInvoker();
        execute(1, mh);

        // mcs.context == cls1
        Lookup lookup = MethodHandles.lookup();
        Class<?> cls1 = lookup.defineHiddenClass(getClassFile("NonBound_1"), true).lookupClass();
        MethodHandle mh1 = LOOKUP.findStatic(cls1, METHOD_NAME, TYPE);

        execute(1, mh1);

        mcs.setTarget(LOOKUP.findStatic(T.class, "f2", TYPE));

        execute(2, mh, mh1);
    }

    static ReferenceQueue rq = new ReferenceQueue();
    static PhantomReference ref;

    public static void testGC(boolean clear, boolean precompile) throws Throwable {
        String id = "_" + clear + "_" + precompile;

        mcs = new MutableCallSite(LOOKUP.findStatic(T.class, "f1", TYPE));

        Lookup lookup = MethodHandles.lookup();
        Class<?>[] cls = new Class[] {
            lookup.defineHiddenClass(getClassFile("GC_1"), true).lookupClass(),
            lookup.defineHiddenClass(getClassFile("GC_2"), true).lookupClass(),
        };

        MethodHandle[] mhs = new MethodHandle[] {
                LOOKUP.findStatic(cls[0], METHOD_NAME, TYPE),
                LOOKUP.findStatic(cls[1], METHOD_NAME, TYPE),
        };

        // mcs.context == cls[0]
        int r = (int) mhs[0].invokeExact();

        execute(1, mhs);

        ref = new PhantomReference<>(cls[0], rq);
        cls[0] = lookup.defineHiddenClass(getClassFile("GC_3"), true).lookupClass();
        mhs[0] = LOOKUP.findStatic(cls[0], METHOD_NAME, TYPE);

        do {
            System.gc();
            try {
                Reference ref1 = rq.remove(100);
                if (ref1 == ref) {
                    break;
                }
            } catch(InterruptedException e) { /* ignore */ }
        } while (true);

        if (clear) {
            ref.clear();
            System.gc(); // Ensure that the stale context is unloaded
        }
        if (precompile) {
            execute(1, mhs);
        }
        mcs.setTarget(LOOKUP.findStatic(T.class, "f2", TYPE));
        execute(2, mhs);
    }

    public static void main(String[] args) throws Throwable {
        testHiddenDepField();
        testSharedCallSite();
        testNonBoundCallSite();
        testGC(false, false);
        testGC(false,  true);
        testGC( true, false);
        testGC( true,  true);
        System.out.println("TEST PASSED");
    }
}
