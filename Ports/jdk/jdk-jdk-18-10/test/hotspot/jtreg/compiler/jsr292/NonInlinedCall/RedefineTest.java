/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8072008
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.misc
 *          java.base/jdk.internal.vm.annotation
 * @library /test/lib / ../patches
 * @requires vm.jvmti
 *
 * @build sun.hotspot.WhiteBox
 *        java.base/java.lang.invoke.MethodHandleHelper
 *        compiler.jsr292.NonInlinedCall.RedefineTest
 * @run driver compiler.jsr292.NonInlinedCall.Agent
 *             agent.jar
 *             compiler.jsr292.NonInlinedCall.RedefineTest
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *                                compiler.jsr292.NonInlinedCall.RedefineTest
 * @run main/bootclasspath/othervm -javaagent:agent.jar
 *                                 -XX:+IgnoreUnrecognizedVMOptions
 *                                 -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                                 -Xbatch -XX:-TieredCompilation -XX:CICompilerCount=1
 *                                 compiler.jsr292.NonInlinedCall.RedefineTest
 */

package compiler.jsr292.NonInlinedCall;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.vm.annotation.DontInline;
import sun.hotspot.WhiteBox;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleHelper;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_STATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_SUPER;
import static jdk.internal.org.objectweb.asm.Opcodes.IRETURN;

public class RedefineTest {
    static final MethodHandles.Lookup LOOKUP = MethodHandleHelper.IMPL_LOOKUP;
    static final String NAME = "compiler/jsr292/NonInlinedCall/RedefineTest$T";

    static Class<?> getClass(int r) {
        byte[] classFile = getClassFile(r);
        try {
            return MethodHandles.lookup().defineClass(classFile);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }

    /**
     * Generates a class of the following shape:
     *     static class T {
     *         @DontInline public static int f() { return $r; }
     *     }
     */
    static byte[] getClassFile(int r) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        MethodVisitor mv;
        cw.visit(52, ACC_PUBLIC | ACC_SUPER, NAME, null, "java/lang/Object", null);
        {
            mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "f", "()I", null, null);
            mv.visitAnnotation("Ljdk/internal/vm/annotation/DontInline;", true);
            mv.visitCode();
            mv.visitLdcInsn(r);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    static final MethodHandle mh;
    static final Class<?> CLS = getClass(0);
    static {
        try {
            mh = LOOKUP.findStatic(CLS, "f", MethodType.methodType(int.class));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    static final WhiteBox WB = WhiteBox.getWhiteBox();

    @DontInline
    static int invokeExact() {
        try {
            return (int)mh.invokeExact();
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    static Instrumentation instr;
    public static void premain(String args, Instrumentation instr) {
        RedefineTest.instr = instr;
    }


    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 20_000; i++) {
            int r = invokeExact();
            if (r != 0) {
                throw new Error(r + " != 0");
            }
        }
        // WB.ensureCompiled();

        redefine();

        int exp = (instr != null) ? 1 : 0;

        for (int i = 0; i < 20_000; i++) {
            if (invokeExact() != exp) {
                throw new Error();
            }
        }

        WB.clearInlineCaches();

        for (int i = 0; i < 20_000; i++) {
            if (invokeExact() != exp) {
                throw new Error();
            }
        }

        // WB.ensureCompiled();
    }

    static void redefine() {
        if (instr == null) {
            System.out.println("NOT REDEFINED");
            return;
        }
        ClassDefinition cd = new ClassDefinition(CLS, getClassFile(1));
        try {
            instr.redefineClasses(cd);
        } catch (Exception e) {
            throw new Error(e);
        }
        System.out.println("REDEFINED");
    }
}
