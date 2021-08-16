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
 * @summary Ensure that invocation parameters are always cast to the instantiatedMethodType
 */
import java.lang.invoke.*;
import java.util.Arrays;
import static java.lang.invoke.MethodType.methodType;

public class MetafactoryParameterCastTest {

    static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    public static class A {
    }

    public static class B extends A {
        void instance0() {}
        void instance1(B arg) {}
        static void static1(B arg) {}
        static void static2(B arg1, B arg2) {}
    }

    public static class C extends B {}
    public static class NotC extends B {}

    public interface ASink { void take(A arg); }
    public interface BSink { void take(B arg); }

    public static void main(String... args) throws Throwable {
        new MetafactoryParameterCastTest().test();
    }

    void test() throws Throwable {
        MethodType takeA = methodType(void.class, A.class);
        MethodType takeB = methodType(void.class, B.class);
        MethodType takeC = methodType(void.class, C.class);

        Class<?>[] noCapture = {};
        Class<?>[] captureB = { B.class };

        MethodHandle[] oneBParam = { lookup.findVirtual(B.class, "instance0", methodType(void.class)),
                                     lookup.findStatic(B.class, "static1", methodType(void.class, B.class)) };
        MethodHandle[] twoBParams = { lookup.findVirtual(B.class, "instance1", methodType(void.class, B.class)),
                                      lookup.findStatic(B.class, "static2", methodType(void.class, B.class, B.class)) };

        for (MethodHandle mh : oneBParam) {
            // sam
            tryASink(invokeMetafactory(mh, ASink.class, "take", noCapture, takeC, takeA));
            tryBSink(invokeMetafactory(mh, BSink.class, "take", noCapture, takeC, takeB));
            tryASink(invokeAltMetafactory(mh, ASink.class, "take", noCapture, takeC, takeA));
            tryBSink(invokeAltMetafactory(mh, BSink.class, "take", noCapture, takeC, takeB));

            // bridge
            tryASink(invokeAltMetafactory(mh, ASink.class, "take", noCapture, takeC, takeC, takeA));
            tryBSink(invokeAltMetafactory(mh, BSink.class, "take", noCapture, takeC, takeC, takeB));
        }

        for (MethodHandle mh : twoBParams) {
            // sam
            tryCapASink(invokeMetafactory(mh, ASink.class, "take", captureB, takeC, takeA));
            tryCapBSink(invokeMetafactory(mh, BSink.class, "take", captureB, takeC, takeB));
            tryCapASink(invokeAltMetafactory(mh, ASink.class, "take", captureB, takeC, takeA));
            tryCapBSink(invokeAltMetafactory(mh, BSink.class, "take", captureB, takeC, takeB));

            // bridge
            tryCapASink(invokeAltMetafactory(mh, ASink.class, "take", captureB, takeC, takeC, takeA));
            tryCapBSink(invokeAltMetafactory(mh, BSink.class, "take", captureB, takeC, takeC, takeB));
        }
    }

    void tryASink(CallSite cs) throws Throwable {
        ASink sink = (ASink) cs.dynamicInvoker().invoke();
        tryASink(sink);
    }

    void tryCapASink(CallSite cs) throws Throwable {
        ASink sink = (ASink) cs.dynamicInvoker().invoke(new B());
        tryASink(sink);
    }

    void tryBSink(CallSite cs) throws Throwable {
        BSink sink = (BSink) cs.dynamicInvoker().invoke();
        tryBSink(sink);
    }

    void tryCapBSink(CallSite cs) throws Throwable {
        BSink sink = (BSink) cs.dynamicInvoker().invoke(new B());
        tryBSink(sink);
    }

    void tryASink(ASink sink) {
        try { sink.take(new C()); }
        catch (ClassCastException e) {
            throw new AssertionError("Unexpected cast failure: " + e + " " + lastMFParams());
        }

        try {
            sink.take(new B());
            throw new AssertionError("Missing cast from A to C: " + lastMFParams());
        }
        catch (ClassCastException e) { /* expected */ }

        try {
            sink.take(new NotC());
            throw new AssertionError("Missing cast from A to C: " + lastMFParams());
        }
        catch (ClassCastException e) { /* expected */ }
    }

    void tryBSink(BSink sink) {
        try { sink.take(new C()); }
        catch (ClassCastException e) {
            throw new AssertionError("Unexpected cast failure: " + e + " " + lastMFParams());
        }

        try {
            sink.take(new B());
            throw new AssertionError("Missing cast from B to C: " + lastMFParams());
        }
        catch (ClassCastException e) { /* expected */ }

        try {
            sink.take(new NotC());
            throw new AssertionError("Missing cast from B to C: " + lastMFParams());
        }
        catch (ClassCastException e) { /* expected */ }
    }

    MethodHandle lastMH;
    Class<?>[] lastCaptured;
    MethodType lastInstMT;
    MethodType lastSamMT;
    MethodType[] lastBridgeMTs;

    String lastMFParams() {
        return "mh=" + lastMH +
               ", captured=" + Arrays.toString(lastCaptured) +
               ", instMT=" + lastInstMT +
               ", samMT=" + lastSamMT +
               ", bridgeMTs=" + Arrays.toString(lastBridgeMTs);
    }

    CallSite invokeMetafactory(MethodHandle mh, Class<?> sam, String methodName,
                               Class<?>[] captured, MethodType instMT, MethodType samMT) {
        lastMH = mh;
        lastCaptured = captured;
        lastInstMT = instMT;
        lastSamMT = samMT;
        lastBridgeMTs = new MethodType[]{};
        try {
            return LambdaMetafactory.metafactory(lookup, methodName, methodType(sam, captured),
                                                 samMT, mh, instMT);
        }
        catch (LambdaConversionException e) {
            // unexpected linkage error
            throw new RuntimeException(e);
        }
    }

    CallSite invokeAltMetafactory(MethodHandle mh, Class<?> sam, String methodName,
                                  Class<?>[] captured, MethodType instMT,
                                  MethodType samMT, MethodType... bridgeMTs) {
        lastMH = mh;
        lastCaptured = captured;
        lastInstMT = instMT;
        lastSamMT = samMT;
        lastBridgeMTs = bridgeMTs;
        try {
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
            return LambdaMetafactory.altMetafactory(lookup, methodName, methodType(sam, captured), args);
        }
        catch (LambdaConversionException e) {
            // unexpected linkage error
            throw new RuntimeException(e);
        }
    }

}
