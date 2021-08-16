/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @requires vm.jvmci
 * @requires vm.simpleArch == "x64" | vm.simpleArch == "aarch64"
 * @library /test/lib /
 * @modules jdk.internal.vm.ci/jdk.vm.ci.hotspot
 *          jdk.internal.vm.ci/jdk.vm.ci.code
 *          jdk.internal.vm.ci/jdk.vm.ci.code.site
 *          jdk.internal.vm.ci/jdk.vm.ci.meta
 *          jdk.internal.vm.ci/jdk.vm.ci.runtime
 *          jdk.internal.vm.ci/jdk.vm.ci.common
 *          jdk.internal.vm.ci/jdk.vm.ci.aarch64
 *          jdk.internal.vm.ci/jdk.vm.ci.amd64
 * @compile CodeInstallationTest.java TestHotSpotVMConfig.java NativeCallTest.java TestAssembler.java amd64/AMD64TestAssembler.java aarch64/AArch64TestAssembler.java
 * @run junit/othervm/native -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI  -Xbootclasspath/a:. jdk.vm.ci.code.test.NativeCallTest
 */
package jdk.vm.ci.code.test;

import static jdk.vm.ci.hotspot.HotSpotCallingConventionType.NativeCall;

import org.junit.BeforeClass;
import org.junit.Test;

import jdk.vm.ci.code.CallingConvention;
import jdk.vm.ci.code.RegisterValue;
import jdk.vm.ci.meta.JavaType;

public class NativeCallTest extends CodeInstallationTest {

    @BeforeClass
    public static void beforeClass() {
        System.loadLibrary("NativeCallTest");
    }

    @Test
    public void testFF() {
        float a = 1.2345678f;
        float b = 8.7654321f;
        test("FF", getFF(), float.class, new Class<?>[]{float.class, float.class}, new Object[]{a, b});
    }

    @Test
    public void testSDILDS() {
        float a = 1.2345678f;
        double b = 3.212434;
        int c = 43921652;
        long d = 412435326;
        double e = .31212333;
        float f = 8.7654321f;
        Class<?>[] argClazz = new Class[]{float.class, double.class, int.class, long.class, double.class,
                        float.class};
        test("SDILDS", getSDILDS(), float.class, argClazz, new Object[]{a, b, c, d, e, f});
    }

    @Test
    public void testF32SDILDS() {
        int sCount = 32;
        // Pairs of <Object>, <Class>
        Object[] remainingArgs = new Object[]{
                        1.2345678F, float.class,
                        3.212434D, double.class,
                        43921652, int.class,
                        0xCAFEBABEDEADBEEFL, long.class,
                        .31212333D, double.class,
                        8.7654321F, float.class
        };
        Class<?>[] argClazz = new Class[sCount + remainingArgs.length / 2];
        Object[] argValues = new Object[sCount + remainingArgs.length / 2];
        for (int i = 0; i < sCount; i++) {
            argValues[i] = (float) i;
            argClazz[i] = float.class;
        }
        for (int i = 0; i < remainingArgs.length; i += 2) {
            argValues[sCount + i / 2] = remainingArgs[i + 0];
            argClazz[sCount + i / 2] = (Class<?>) remainingArgs[i + 1];
        }
        test("F32SDILDS", getF32SDILDS(), float.class, argClazz, argValues);
    }

    @Test
    public void testI32SDILDS() {
        int sCount = 32;
        // Pairs of <Object>, <Class>
        Object[] remainingArgs = new Object[]{
                        1.2345678F, float.class,
                        3.212434D, double.class,
                        43921652, int.class,
                        0xCAFEBABEDEADBEEFL, long.class,
                        .31212333D, double.class,
                        8.7654321F, float.class
        };
        Class<?>[] argClazz = new Class[sCount + remainingArgs.length / 2];
        Object[] argValues = new Object[sCount + remainingArgs.length / 2];
        for (int i = 0; i < sCount; i++) {
            argValues[i] = i;
            argClazz[i] = int.class;
        }
        for (int i = 0; i < remainingArgs.length; i += 2) {
            argValues[sCount + i / 2] = remainingArgs[i + 0];
            argClazz[sCount + i / 2] = (Class<?>) remainingArgs[i + 1];
        }
        test("I32SDILDS", getI32SDILDS(), float.class, argClazz, argValues);
    }

    public void test(String name, long addr, Class<?> returnClazz, Class<?>[] types, Object[] values) {
        try {
            test(asm -> {
                JavaType[] argTypes = new JavaType[types.length];
                int i = 0;
                for (Class<?> clazz : types) {
                    argTypes[i++] = metaAccess.lookupJavaType(clazz);
                }
                JavaType returnType = metaAccess.lookupJavaType(returnClazz);
                CallingConvention cc = codeCache.getRegisterConfig().getCallingConvention(NativeCall, returnType, argTypes, asm.valueKindFactory);
                asm.emitCallPrologue(cc, values);
                asm.emitCall(addr);
                asm.emitCallEpilogue(cc);
                asm.emitFloatRet(((RegisterValue) cc.getReturn()).getRegister());
            }, getMethod(name, types), values);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    // Checkstyle: stop

    public static native long getFF();

    public static native float _FF(float a, float b);

    public static float FF(float a, float b) {
        return _FF(a, b);
    }

    public static native long getSDILDS();

    public static native float _SDILDS(float a, double b, int c, long d, double e, float f);

    public static float SDILDS(float a, double b, int c, long d, double e, float f) {
        return _SDILDS(a, b, c, d, e, f);
    }

    public static native long getF32SDILDS();

    public static native float _F32SDILDS(float f00, float f01, float f02, float f03, float f04, float f05, float f06, float f07,
                    float f08, float f09, float f0a, float f0b, float f0c, float f0d, float f0e, float f0f,
                    float f10, float f11, float f12, float f13, float f14, float f15, float f16, float f17,
                    float f18, float f19, float f1a, float f1b, float f1c, float f1d, float f1e, float f1f,
                    float a, double b, int c, long d, double e, float f);

    public static float F32SDILDS(float f00, float f01, float f02, float f03, float f04, float f05, float f06, float f07,
                    float f08, float f09, float f0a, float f0b, float f0c, float f0d, float f0e, float f0f,
                    float f10, float f11, float f12, float f13, float f14, float f15, float f16, float f17,
                    float f18, float f19, float f1a, float f1b, float f1c, float f1d, float f1e, float f1f,
                    float a, double b, int c, long d, double e, float f) {
        return _F32SDILDS(f00, f01, f02, f03, f04, f05, f06, f07,
                        f08, f09, f0a, f0b, f0c, f0d, f0e, f0f,
                        f10, f11, f12, f13, f14, f15, f16, f17,
                        f18, f19, f1a, f1b, f1c, f1d, f1e, f1f,
                        a, b, c, d, e, f);
    }

    public static native long getD32SDILDS();

    public static native float _D32SDILDS(double d00, double d01, double d02, double d03, double d04, double d05, double d06, double d07,
                    double d08, double d09, double d0a, double d0b, double d0c, double d0d, double d0e, double d0f,
                    double d10, double d11, double d12, double d13, double d14, double d15, double d16, double d17,
                    double d18, double d19, double d1a, double d1b, double d1c, double d1d, double d1e, double d1f,
                    float a, double b, int c, long d, double e, float f);

    @SuppressWarnings("unused")
    public static float D32SDILDS(double d00, double d01, double d02, double d03, double d04, double d05, double d06, double d07,
                    double d08, double d09, double d0a, double d0b, double d0c, double d0d, double d0e, double d0f,
                    double d10, double d11, double d12, double d13, double d14, double d15, double d16, double d17,
                    double d18, double d19, double d1a, double d1b, double d1c, double d1d, double d1e, double d1f,
                    float a, double b, int c, long d, double e, float f) {
        return _D32SDILDS(d00, d01, d02, d03, d04, d05, d06, d07,
                        d08, d09, d0a, d0b, d0c, d0d, d0e, d0d,
                        d10, d11, d12, d13, d14, d15, d16, d17,
                        d18, d19, d1a, d1b, d1c, d1d, d1e, d1f,
                        a, b, c, d, e, f);
    }

    public static native long getI32SDILDS();

    public static native float _I32SDILDS(int i00, int i01, int i02, int i03, int i04, int i05, int i06, int i07,
                    int i08, int i09, int i0a, int i0b, int i0c, int i0d, int i0e, int i0f,
                    int i10, int i11, int i12, int i13, int i14, int i15, int i16, int i17,
                    int i18, int i19, int i1a, int i1b, int i1c, int i1d, int i1e, int i1f,
                    float a, double b, int c, long d, double e, float f);

    public static float I32SDILDS(int i00, int i01, int i02, int i03, int i04, int i05, int i06, int i07,
                    int i08, int i09, int i0a, int i0b, int i0c, int i0d, int i0e, int i0f,
                    int i10, int i11, int i12, int i13, int i14, int i15, int i16, int i17,
                    int i18, int i19, int i1a, int i1b, int i1c, int i1d, int i1e, int i1f,
                    float a, double b, int c, long d, double e, float f) {
        return _I32SDILDS(i00, i01, i02, i03, i04, i05, i06, i07,
                        i08, i09, i0a, i0b, i0c, i0d, i0e, i0f,
                        i10, i11, i12, i13, i14, i15, i16, i17,
                        i18, i19, i1a, i1b, i1c, i1d, i1e, i1f,
                        a, b, c, d, e, f);
    }

    public static native long getL32SDILDS();

    public static native float _L32SDILDS(long l00, long l01, long l02, long l03, long l04, long l05, long l06, long l07,
                    long l08, long l09, long l0a, long l0b, long l0c, long l0d, long l0e, long l0f,
                    long l10, long l11, long l12, long l13, long l14, long l15, long l16, long l17,
                    long l18, long l19, long l1a, long l1b, long l1c, long l1d, long l1e, long l1f,
                    float a, double b, int c, long d, double e, float f);

    public static float L32SDILDS(long l00, long l01, long l02, long l03, long l04, long l05, long l06, long l07,
                    long l08, long l09, long l0a, long l0b, long l0c, long l0d, long l0e, long l0f,
                    long l10, long l11, long l12, long l13, long l14, long l15, long l16, long l17,
                    long l18, long l19, long l1a, long l1b, long l1c, long l1d, long l1e, long l1f,
                    float a, double b, int c, long d, double e, float f) {
        return _L32SDILDS(l00, l01, l02, l03, l04, l05, l06, l07,
                        l08, l09, l0a, l0b, l0c, l0d, l0e, l0f,
                        l10, l11, l12, l13, l14, l15, l16, l17,
                        l18, l19, l1a, l1b, l1c, l1d, l1e, l1f,
                        a, b, c, d, e, f);
    }
}
