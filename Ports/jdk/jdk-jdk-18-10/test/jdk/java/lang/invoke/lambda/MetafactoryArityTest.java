/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8035776
 * @summary metafactory should fail if arities are mismatched
 */
import java.lang.invoke.*;
import java.util.Arrays;
import static java.lang.invoke.MethodType.methodType;

public class MetafactoryArityTest {

    public interface I {}
    public static class C { public static String m(int arg) { return ""; } }

    static final MethodHandles.Lookup lookup = MethodHandles.lookup();
    static final Class<?>[] capInt = { int.class };
    static final MethodHandle C_m;
    static {
        try { C_m = lookup.findStatic(C.class, "m", methodType(String.class, int.class)); }
        catch (NoSuchMethodException | IllegalAccessException e) { throw new RuntimeException(e); }
    }

    public static void main(String... args) {
        MethodType unary = methodType(String.class, int.class);
        MethodType nullary = methodType(String.class);
        MethodType binary = methodType(String.class, int.class, int.class);
        MethodType unaryCS = methodType(CharSequence.class, int.class);
        MethodType nullaryCS = methodType(CharSequence.class);
        MethodType binaryCS = methodType(CharSequence.class, int.class, int.class);
        MethodType unaryObj = methodType(Object.class, int.class);
        MethodType nullaryObj = methodType(Object.class);
        MethodType binaryObj = methodType(Object.class, int.class, int.class);

        test(true, C_m, unary, unary);
        test(false, C_m, unary, nullary);
        test(false, C_m, nullary, unary);
        test(false, C_m, unary, binary);
        test(false, C_m, binary, unary);

        testBridge(true, C_m, unary, unary, unaryCS);
        testBridge(false, C_m, unary, unary, nullaryCS);
        testBridge(false, C_m, unary, unary, binaryCS);

        testBridge(true, C_m, unary, unary, unaryCS, unaryObj);
        testBridge(false, C_m, unary, unary, unaryCS, nullaryObj);
        testBridge(false, C_m, unary, unary, unaryCS, binaryObj);

        testCapture(true, C_m, capInt, nullary, nullary);
        testCapture(false, C_m, capInt, binary, binary);
        testCapture(false, C_m, capInt, nullary, unary);
        testCapture(false, C_m, capInt, nullary, binary);
        testCapture(false, C_m, capInt, unary, nullary);
        testCapture(false, C_m, capInt, unary, binary);

        testCaptureBridge(true, C_m, capInt, nullary, nullary, nullaryCS);
        testCaptureBridge(false, C_m, capInt, unary, unary, unaryCS);
        testCaptureBridge(false, C_m, capInt, nullary, nullary, unaryCS);
        testCaptureBridge(false, C_m, capInt, nullary, nullary, binaryCS);

        testCaptureBridge(true, C_m, capInt, nullary, nullary, nullaryCS, nullaryObj);
        testCaptureBridge(false, C_m, capInt, unary, unary, unaryCS, unaryObj);
        testCaptureBridge(false, C_m, capInt, nullary, nullary, nullaryCS, unaryObj);
        testCaptureBridge(false, C_m, capInt, nullary, nullary, nullaryCS, binaryObj);
    }

    static void test(boolean correct, MethodHandle mh, MethodType instMT, MethodType samMT) {
        tryMetafactory(correct, mh, new Class<?>[]{}, instMT, samMT);
        tryAltMetafactory(correct, mh, new Class<?>[]{}, instMT, samMT);
    }

    static void testBridge(boolean correct, MethodHandle mh, MethodType instMT, MethodType samMT, MethodType... bridgeMTs) {
        tryAltMetafactory(correct, mh, new Class<?>[]{}, instMT, samMT, bridgeMTs);
    }

    static void testCapture(boolean correct, MethodHandle mh, Class<?>[] captured, MethodType instMT, MethodType samMT) {
        tryMetafactory(correct, mh, captured, instMT, samMT);
        tryAltMetafactory(correct, mh, captured, instMT, samMT);
    }

    static void testCaptureBridge(boolean correct, MethodHandle mh, Class<?>[] captured,
                                  MethodType instMT, MethodType samMT, MethodType... bridgeMTs) {
        tryAltMetafactory(correct, mh, captured, instMT, samMT, bridgeMTs);
    }

    static void tryMetafactory(boolean correct, MethodHandle mh, Class<?>[] captured,
                               MethodType instMT, MethodType samMT) {
        try {
            LambdaMetafactory.metafactory(lookup, "run", methodType(I.class, captured),
                                          samMT, mh, instMT);
            if (!correct) {
                throw new AssertionError("Uncaught linkage error:" +
                                         " impl=" + mh +
                                         ", captured=" + Arrays.toString(captured) +
                                         ", inst=" + instMT +
                                         ", sam=" + samMT);
            }
        }
        catch (LambdaConversionException e) {
            if (correct) {
                throw new AssertionError("Unexpected linkage error:" +
                                         " e=" + e +
                                         ", impl=" + mh +
                                         ", captured=" + Arrays.toString(captured) +
                                         ", inst=" + instMT +
                                         ", sam=" + samMT);
            }
        }
    }

    static void tryAltMetafactory(boolean correct, MethodHandle mh, Class<?>[] captured,
                                  MethodType instMT, MethodType samMT, MethodType... bridgeMTs) {
        boolean bridge = bridgeMTs.length > 0;
        Object[] args = new Object[bridge ? 5+bridgeMTs.length : 4];
        args[0] = samMT;
        args[1] = mh;
        args[2] = instMT;
        args[3] = bridge ? LambdaMetafactory.FLAG_BRIDGES : 0;
        if (bridge) {
            args[4] = bridgeMTs.length;
            for (int i = 0; i < bridgeMTs.length; i++) args[5+i] = bridgeMTs[i];
        }
        try {
            LambdaMetafactory.altMetafactory(lookup, "run", methodType(I.class, captured), args);
            if (!correct) {
                throw new AssertionError("Uncaught linkage error:" +
                                         " impl=" + mh +
                                         ", captured=" + Arrays.toString(captured) +
                                         ", inst=" + instMT +
                                         ", sam=" + samMT +
                                         ", bridges=" + Arrays.toString(bridgeMTs));
            }
        }
        catch (LambdaConversionException e) {
            if (correct) {
                throw new AssertionError("Unexpected linkage error:" +
                                         " e=" + e +
                                         ", impl=" + mh +
                                         ", captured=" + Arrays.toString(captured) +
                                         ", inst=" + instMT +
                                         ", sam=" + samMT +
                                         ", bridges=" + Arrays.toString(bridgeMTs));
            }
        }
    }

}
