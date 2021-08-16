/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8186046
 * @summary Test bootstrap arguments for condy
 * @library /lib/testlibrary/bytecode /java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @run testng CondyStaticArgumentsTest
 * @run testng/othervm -XX:+UnlockDiagnosticVMOptions -XX:UseBootstrapCallInfo=3 CondyStaticArgumentsTest
 */

import jdk.experimental.bytecode.PoolHelper;
import org.testng.Assert;
import org.testng.annotations.Test;
import test.java.lang.invoke.lib.InstructionHelper;

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.StringJoiner;
import java.util.stream.Stream;

import static java.lang.invoke.MethodType.methodType;

public class CondyStaticArgumentsTest {
    static final MethodHandles.Lookup L = MethodHandles.lookup();

    static class BSMInfo {
        final String methodName;
        final MethodHandle handle;
        final String descriptor;

        BSMInfo(String name) {
            methodName = name;

            Method m = Stream.of(CondyStaticArgumentsTest.class.getDeclaredMethods())
                    .filter(x -> x.getName().equals(methodName)).findFirst()
                    .get();
            try {
                handle = MethodHandles.lookup().unreflect(m);
            }
            catch (Exception e) {
                throw new Error(e);
            }
            descriptor = handle.type().toMethodDescriptorString();
        }

        static BSMInfo of(String name) {
            return new BSMInfo(name);
        }
    }

    static String basicArgs(MethodHandles.Lookup l, String name, Class<?> type,
                            int i, long j, float f, double d,
                            Class<?> c, String s,
                            MethodType mt, MethodHandle mh) {
        return new StringJoiner("-")
                .add(name)
                .add(type.getSimpleName())
                .add(Integer.toString(i))
                .add(Long.toString(j))
                .add(Float.toString(f))
                .add(Double.toString(d))
                .add(c.getSimpleName())
                .add(s)
                .add(mt.toString())
                .add(Integer.toString(mh.type().parameterCount()))
                .toString();
    }

    @Test
    public void testBasicArgs() throws Throwable {
        BSMInfo bi = BSMInfo.of("basicArgs");
        MethodHandleInfo mhi = MethodHandles.lookup().revealDirect(bi.handle);

        MethodHandle mh = InstructionHelper.ldcDynamicConstant(
                L, "constant-name", String.class,
                bi.methodName, bi.handle.type(),
                S -> S.add(1).add(2L).add(3.0f).add(4.0d)
                        .add("java/lang/Number", PoolHelper::putClass)
                        .add("something", PoolHelper::putString)
                        .add("(IJFD)V", PoolHelper::putMethodType)
                        .add(mhi, (P, Z) -> {
                            return P.putHandle(mhi.getReferenceKind(), "CondyStaticArgumentsTest", mhi.getName(), bi.descriptor);
                        }));

        Assert.assertEquals(mh.invoke(), "constant-name-String-1-2-3.0-4.0-Number-something-(int,long,float,double)void-11");
    }


    static MathContext mathContext(MethodHandles.Lookup l, String value, Class<?> type) {
        switch (value) {
            case "UNLIMITED":
                return MathContext.UNLIMITED;
            case "DECIMAL32":
                return MathContext.DECIMAL32;
            case "DECIMAL64":
                return MathContext.DECIMAL64;
            case "DECIMAL128":
                return MathContext.DECIMAL128;
            default:
                throw new UnsupportedOperationException();
        }
    }

    static BigDecimal bigDecimal(MethodHandles.Lookup l, String name, Class<?> type,
                                 String value, MathContext mc) {
        return new BigDecimal(value, mc);
    }

    static String condyWithCondy(MethodHandles.Lookup l, String name, Class<?> type,
                                 BigDecimal d) {
        return new StringJoiner("-")
                .add(name)
                .add(type.getSimpleName())
                .add(d.toString())
                .add(Integer.toString(d.precision()))
                .toString();
    }

    static <E> int bigDecimalPoolHelper(String value, String mc, PoolHelper<String, String, E> P) {
        BSMInfo bi = BSMInfo.of("bigDecimal");
        return P.putDynamicConstant("big-decimal", "Ljava/math/BigDecimal;", InstructionHelper.csym(L.lookupClass()), bi.methodName, bi.descriptor,
                                    S -> S.add(value, PoolHelper::putString)
                                            .add(mc, (P2, s) -> {
                                                return mathContextPoolHelper(s, P2);
                                            }));
    }

    static <E> int mathContextPoolHelper(String mc, PoolHelper<String, String, E> P) {
        BSMInfo bi = BSMInfo.of("mathContext");
        return P.putDynamicConstant(mc, "Ljava/math/MathContext;", InstructionHelper.csym(L.lookupClass()), bi.methodName, bi.descriptor,
                                    S -> {
                                    });
    }

    @Test
    public void testCondyWithCondy() throws Throwable {
        BSMInfo bi = BSMInfo.of("condyWithCondy");

        MethodHandle mh = InstructionHelper.ldcDynamicConstant(
                L, "big-decimal-math-context", String.class,
                bi.methodName, bi.handle.type(),
                S -> S.add(null, (P, v) -> {
                    return bigDecimalPoolHelper("3.14159265358979323846", "DECIMAL32", P);
                }));
        Assert.assertEquals(mh.invoke(), "big-decimal-math-context-String-3.141593-7");
    }


    static ConstantCallSite indyWithCondy(MethodHandles.Lookup l, String name, MethodType type,
                                          BigDecimal d) {
        String s = new StringJoiner("-")
                .add(name)
                .add(type.toMethodDescriptorString())
                .add(d.toString())
                .add(Integer.toString(d.precision()))
                .toString();
        return new ConstantCallSite(MethodHandles.constant(String.class, s));
    }

    @Test
    public void testIndyWithCondy() throws Throwable {
        BSMInfo bi = BSMInfo.of("indyWithCondy");

        MethodHandle mh = InstructionHelper.invokedynamic(
                L, "big-decimal-math-context", methodType(String.class),
                bi.methodName, bi.handle.type(),
                S -> S.add(null, (P, v) -> {
                    return bigDecimalPoolHelper("3.14159265358979323846", "DECIMAL32", P);
                }));
        Assert.assertEquals(mh.invoke(), "big-decimal-math-context-()Ljava/lang/String;-3.141593-7");
    }
}
