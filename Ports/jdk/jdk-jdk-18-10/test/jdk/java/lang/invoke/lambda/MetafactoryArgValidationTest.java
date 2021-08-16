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
 * @bug 8174222
 * @summary Validation of LambdaMetafactory arguments
 */
import java.lang.invoke.*;
import java.util.*;

public class MetafactoryArgValidationTest {

    public static void main(String... args) {
        testNPE();
        testIAE();
        testLCE();
    }

    public static void testNPE() {
        MethodType toI = mt(I.class);
        MethodType toVoid = mt(void.class);
        MethodType toC = mt(C.class);
        MethodHandle impl = C.invokeStaticMH();
        MethodHandle impl2 = C.newInvokeSpecialMH();
        int flagSer = LambdaMetafactory.FLAG_SERIALIZABLE;
        int flagMark = LambdaMetafactory.FLAG_MARKERS;
        int flagBridge = LambdaMetafactory.FLAG_BRIDGES;

        mfFail(NullPointerException.class, null, "m", toI, toVoid, impl, toVoid);
        mfFail(NullPointerException.class, C.lookup, null, toI, toVoid, impl, toVoid);
        mfFail(NullPointerException.class, C.lookup, "m", null, toVoid, impl, toVoid);
        mfFail(NullPointerException.class, C.lookup, "m", toI, null, impl, toVoid);
        mfFail(NullPointerException.class, C.lookup, "m", toI, toVoid, null, toVoid);
        mfFail(NullPointerException.class, C.lookup, "m", toI, toVoid, impl, null);

        amfFail(NullPointerException.class, null, "m", toI, arr(toVoid, impl, toVoid, flagSer));
        amfFail(NullPointerException.class, C.lookup, null, toI, arr(toVoid, impl, toVoid, flagSer));
        amfFail(NullPointerException.class, C.lookup, "m", null, arr(toVoid, impl, toVoid, flagSer));
        amfFail(NullPointerException.class, C.lookup, "m", toI, null);
        amfFail(NullPointerException.class, C.lookup, "m", toI, arr(null, impl, toVoid, flagSer));
        amfFail(NullPointerException.class, C.lookup, "m", toI, arr(toVoid, null, toVoid, flagSer));
        amfFail(NullPointerException.class, C.lookup, "m", toI, arr(toVoid, impl, null, flagSer));
        amfFail(NullPointerException.class, C.lookup, "m", toI, arr(toVoid, impl, toVoid, null));
        amfFail(NullPointerException.class, C.lookup, "m", toI, arr(toVoid, impl, toVoid, flagMark, null));
        amfFail(NullPointerException.class, C.lookup, "m", toI, arr(toVoid, impl, toVoid, flagMark, 1, null));
        amfFail(NullPointerException.class, C.lookup, "m", toI, arr(toVoid, impl, toVoid, flagMark, 2, Runnable.class, null));
        amfFail(NullPointerException.class, C.lookup, "m", toI, arr(toC, impl2, toC, flagBridge, null));
        amfFail(NullPointerException.class, C.lookup, "m", toI, arr(toC, impl2, toC, flagBridge, 1, null));
        amfFail(NullPointerException.class, C.lookup, "m", toI, arr(toC, impl2, toC, flagBridge, 1, null));
        amfFail(NullPointerException.class, C.lookup, "m", toI, arr(toC, impl2, toC, flagBridge, 2, mt(Object.class), null));
        amfFail(NullPointerException.class, C.lookup, "m", toI, arr(toC, impl2, toC, flagBridge|flagMark|flagSer, 1, Runnable.class, 2, mt(Object.class), null));
    }

    public static void testIAE() {
        MethodType toI = mt(I.class);
        MethodType toVoid = mt(void.class);
        MethodType toC = mt(C.class);
        MethodHandle impl = C.invokeStaticMH();
        MethodHandle impl2 = C.newInvokeSpecialMH();
        int flagSer = LambdaMetafactory.FLAG_SERIALIZABLE;
        int flagMark = LambdaMetafactory.FLAG_MARKERS;
        int flagBridge = LambdaMetafactory.FLAG_BRIDGES;

        // missing arguments
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr());
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toVoid, impl, toVoid));
        amfSucceed(C.lookup, "m", toI, arr(toVoid, impl, toVoid, 0));
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toVoid, impl, toVoid, flagMark));
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toVoid, impl, toVoid, flagMark, 1));
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toVoid, impl, toVoid, flagMark, 2, Runnable.class));
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toC, impl2, toC, flagBridge));
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toC, impl2, toC, flagBridge, 1));
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toC, impl2, toC, flagBridge, 2, mt(Object.class)));
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toC, impl2, toC, flagBridge|flagMark|flagSer, 1, Runnable.class, 2, mt(Object.class)));

        // too many arguments
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toVoid, impl, toVoid, 0, 1));
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toC, impl2, toC, flagMark|flagSer, 1, Runnable.class, 1, mt(Object.class)));

        // wrong argument types
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(impl, impl, toVoid, flagSer));
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toVoid, toVoid, toVoid, flagSer));
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toVoid, impl, impl, flagSer));
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toVoid, impl, toVoid, "hi"));
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toVoid, impl, toVoid, flagMark, Runnable.class));
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toVoid, impl, toVoid, flagBridge, 1, Runnable.class));
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toVoid, impl, toVoid, flagMark, 1, toVoid));

        // negative count
        amfSucceed(C.lookup, "m", toI, arr(toC, impl2, toC, flagBridge|flagMark|flagSer, 0, 0));
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toC, impl2, toC, flagBridge|flagMark|flagSer, -1, 0));
        amfFail(IllegalArgumentException.class, C.lookup, "m", toI, arr(toC, impl2, toC, flagBridge|flagMark|flagSer, 0, -1));
    }

    public static void testLCE() {
        MethodType toI = mt(I.class);
        MethodType toC = mt(C.class);
        MethodType toVoid = mt(void.class);
        MethodType cToVoid = mt(void.class, C.class);
        MethodType ccToVoid = mt(void.class, C.class, C.class);
        MethodType cToC = mt(C.class, C.class);
        MethodType cToString = mt(String.class, C.class);
        MethodHandle impl = C.invokeStaticMH();
        int flagSer = LambdaMetafactory.FLAG_SERIALIZABLE;
        int flagMark = LambdaMetafactory.FLAG_MARKERS;

        // non-interface
        mfFail(LambdaConversionException.class, C.lookup, "m", mt(Object.class), toVoid, impl, toVoid);
        amfFail(LambdaConversionException.class, C.lookup, "m", mt(Object.class), arr(toVoid, impl, toVoid, flagSer));
        mfFail(LambdaConversionException.class, C.lookup, "m", mt(int.class), toVoid, impl, toVoid);
        amfFail(LambdaConversionException.class, C.lookup, "m", mt(int.class), arr(toVoid, impl, toVoid, flagSer));
        amfFail(LambdaConversionException.class, C.lookup, "m", toI, arr(toVoid, impl, toVoid, flagMark, 1, Object.class));
        amfFail(LambdaConversionException.class, C.lookup, "m", toI, arr(toVoid, impl, toVoid, flagMark, 1, int.class));
        amfFail(LambdaConversionException.class, C.lookup, "m", toI, arr(toVoid, impl, toVoid, flagMark, 2, Runnable.class, Object.class));

        // lookup without private access
        mfFail(LambdaConversionException.class, MethodHandles.publicLookup(), "m", toI, toVoid, impl, toVoid);
        amfFail(LambdaConversionException.class, MethodHandles.publicLookup(), "m", toI, arr(toVoid, impl, toVoid, flagSer));
        mfFail(LambdaConversionException.class, C.lookup.dropLookupMode(MethodHandles.Lookup.PRIVATE), "m", toI, toVoid, impl, toVoid);
        amfFail(LambdaConversionException.class, C.lookup.dropLookupMode(MethodHandles.Lookup.PRIVATE), "m", toI, arr(toVoid, impl, toVoid, flagSer));

        // unsupported MethodHandle
        mfFail(LambdaConversionException.class, C.lookup, "m", toI, cToC, C.getFieldMH(), cToC);
        amfFail(LambdaConversionException.class, C.lookup, "m", toI, arr(cToC, C.getFieldMH(), cToC, flagSer));
        mfFail(LambdaConversionException.class, C.lookup, "m", toI, toC, C.getStaticMH(), toC);
        amfFail(LambdaConversionException.class, C.lookup, "m", toI, arr(toC, C.getStaticMH(), toC, flagSer));
        mfFail(LambdaConversionException.class, C.lookup, "m", toI, ccToVoid, C.putFieldMH(), ccToVoid);
        amfFail(LambdaConversionException.class, C.lookup, "m", toI, arr(ccToVoid, C.putFieldMH(), ccToVoid, flagSer));
        mfFail(LambdaConversionException.class, C.lookup, "m", toI, cToVoid, C.putStaticMH(), cToVoid);
        amfFail(LambdaConversionException.class, C.lookup, "m", toI, arr(cToVoid, C.putStaticMH(), cToVoid, flagSer));
        mfSucceed(C.lookup, "m", toI, cToVoid, C.invokeVirtualMH(), cToVoid);
        amfSucceed(C.lookup, "m", toI, arr(cToVoid, C.invokeVirtualMH(), cToVoid, flagSer));
        mfSucceed(C.lookup, "m", toI, toVoid, C.invokeStaticMH(), toVoid);
        amfSucceed(C.lookup, "m", toI, arr(toVoid, C.invokeStaticMH(), toVoid, flagSer));
        mfSucceed(C.lookup, "m", toI, cToString, C.invokeSpecialMH(), cToString);
        amfSucceed(C.lookup, "m", toI, arr(cToString, C.invokeSpecialMH(), cToString, flagSer));
        mfSucceed(C.lookup, "m", toI, toC, C.newInvokeSpecialMH(), toC);
        amfSucceed(C.lookup, "m", toI, arr(toC, C.newInvokeSpecialMH(), toC, flagSer));
        mfSucceed(C.lookup, "m", toI, cToVoid, C.invokeInterfaceMH(), cToVoid);
        amfSucceed(C.lookup, "m", toI, arr(cToVoid, C.invokeInterfaceMH(), cToVoid, flagSer));
        mfFail(LambdaConversionException.class, C.lookup, "m", toI, toVoid, MethodHandles.empty(toVoid), toVoid);
        amfFail(LambdaConversionException.class, C.lookup, "m", toI, arr(toVoid, MethodHandles.empty(toVoid), toVoid, flagSer));
    }

    static MethodType mt(Class<?> ret, Class<?>... params) {
        return MethodType.methodType(ret, params);
    }

    static Object[] arr(Object... args) {
        return args;
    }

    public static class C implements Runnable {

        public static MethodHandles.Lookup lookup = MethodHandles.lookup();

        public static MethodHandle getFieldMH() {
            try { return lookup.findGetter(C.class, "iv", C.class); }
            catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        }

        public static MethodHandle getStaticMH() {
            try { return lookup.findStaticGetter(C.class, "sv", C.class); }
            catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        }

        public static MethodHandle putFieldMH() {
            try { return lookup.findSetter(C.class, "iv", C.class); }
            catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        }

        public static MethodHandle putStaticMH() {
            try { return lookup.findStaticSetter(C.class, "sv", C.class); }
            catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        }

        public static MethodHandle invokeVirtualMH() {
            try { return lookup.findVirtual(C.class, "im", mt(void.class)); }
            catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        }

        public static MethodHandle invokeStaticMH() {
            try { return lookup.findStatic(C.class, "sm", mt(void.class)); }
            catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        }

        public static MethodHandle invokeSpecialMH() {
            try { return lookup.findSpecial(Object.class, "toString", mt(String.class), C.class); }
            catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        }

        public static MethodHandle newInvokeSpecialMH() {
            try { return lookup.findConstructor(C.class, mt(void.class)); }
            catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        }

        public static MethodHandle invokeInterfaceMH() {
            try { return lookup.findVirtual(Runnable.class, "run", mt(void.class)); }
            catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        }

        public static C sv;
        public C iv;
        public static void sm() {}
        public void im() {}
        public C() {}
        public void run() {}
    }

    public interface I {}

    static CallSite mfSucceed(MethodHandles.Lookup lookup,
                              String name,
                              MethodType capType,
                              MethodType desc,
                              MethodHandle impl,
                              MethodType checked) {
        try {
            return LambdaMetafactory.metafactory(lookup, name, capType, desc, impl, checked);
        } catch (Throwable t) {
            String msg = String.format("Unexpected exception during linkage for metafactory(%s, %s, %s, %s, %s, %s)",
                    lookup, name, capType, desc, impl, checked);
            throw new AssertionError(msg, t);
        }
    }

    static void mfFail(Class<?> exceptionType,
                       MethodHandles.Lookup lookup,
                       String name,
                       MethodType capType,
                       MethodType desc,
                       MethodHandle impl,
                       MethodType checked) {
        try {
            LambdaMetafactory.metafactory(lookup, name, capType, desc, impl, checked);
        } catch (Throwable t) {
            if (exceptionType.isInstance(t)) {
                return;
            } else {
                String msg = String.format("Unexpected exception: expected %s during linkage for metafactory(%s, %s, %s, %s, %s, %s)",
                                           exceptionType.getName(), lookup, name, capType, desc, impl, checked);
                throw new AssertionError(msg, t);
            }
        }
        String msg = String.format("Unexpected success: expected %s during linkage for metafactory(%s, %s, %s, %s, %s, %s)",
                                   exceptionType.getName(), lookup, name, capType, desc, impl, checked);
        throw new AssertionError(msg);
    }

    static CallSite amfSucceed(MethodHandles.Lookup lookup,
                               String name,
                               MethodType capType,
                               Object[] args) {
        try {
            return LambdaMetafactory.altMetafactory(lookup, name, capType, args);
        } catch (Throwable t) {
            String msg = String.format("Unexpected exception during linkage for altMetafactory(%s, %s, %s, %s)",
                                       lookup, name, capType, Arrays.asList(args));
            throw new AssertionError(msg, t);
        }
    }

    static void amfFail(Class<?> exceptionType,
                        MethodHandles.Lookup lookup,
                        String name,
                        MethodType capType,
                        Object[] args) {
        try {
            LambdaMetafactory.altMetafactory(lookup, name, capType, args);
        } catch (Throwable t) {
            if (exceptionType.isInstance(t)) {
                return;
            } else {
                String msg = String.format("Unexpected exception: expected %s during linkage for altMetafactory(%s, %s, %s, %s)",
                                           exceptionType.getName(), lookup, name, capType, Arrays.asList(args));
                throw new AssertionError(msg, t);
            }
        }
        String msg = String.format("Unexpected success: expected %s during linkage for altMetafactory(%s, %s, %s, %s)",
                                   exceptionType.getName(), lookup, name, capType, Arrays.asList(args));
        throw new AssertionError(msg);
    }

}
