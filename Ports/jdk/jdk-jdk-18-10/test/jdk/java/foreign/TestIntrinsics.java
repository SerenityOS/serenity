/*
 *  Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

/*
 * @test
 * @requires os.arch=="amd64" | os.arch=="x86_64" | os.arch=="aarch64"
 * @run testng/othervm
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_SPEC=true
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   --enable-native-access=ALL-UNNAMED
 *   -Xbatch
 *   TestIntrinsics
 */

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.SymbolLookup;
import org.testng.annotations.*;

import static java.lang.invoke.MethodType.methodType;
import static jdk.incubator.foreign.CLinker.*;
import static jdk.incubator.foreign.FunctionDescriptor.TRIVIAL_ATTRIBUTE_NAME;
import static org.testng.Assert.assertEquals;

public class TestIntrinsics {

    static final CLinker abi = CLinker.getInstance();
    static {
        System.loadLibrary("Intrinsics");
    }

    static final SymbolLookup LOOKUP = SymbolLookup.loaderLookup();

    private interface RunnableX {
        void run() throws Throwable;
    }

    @Test(dataProvider = "tests")
    public void testIntrinsics(RunnableX test) throws Throwable {
        for (int i = 0; i < 20_000; i++) {
            test.run();
        }
    }

    @DataProvider
    public Object[][] tests() {
        List<RunnableX> testsList = new ArrayList<>();

        interface AddTest {
            void add(MethodHandle target, Object expectedResult, Object... args);
        }

        AddTest tests = (mh, expectedResult, args) -> testsList.add(() -> {
            Object actual = mh.invokeWithArguments(args);
            assertEquals(actual, expectedResult);
        });

        interface AddIdentity {
            void add(String name, Class<?> carrier, MemoryLayout layout, Object arg);
        }

        AddIdentity addIdentity = (name, carrier, layout, arg) -> {
            MemoryAddress ma = LOOKUP.lookup(name).get();
            MethodType mt = methodType(carrier, carrier);
            FunctionDescriptor fd = FunctionDescriptor.of(layout, layout);

            tests.add(abi.downcallHandle(ma, mt, fd), arg, arg);
            tests.add(abi.downcallHandle(ma, mt, fd.withAttribute(TRIVIAL_ATTRIBUTE_NAME, true)), arg, arg);
            tests.add(abi.downcallHandle(mt, fd), arg, ma, arg);
        };

        { // empty
            MemoryAddress ma = LOOKUP.lookup("empty").get();
            MethodType mt = methodType(void.class);
            FunctionDescriptor fd = FunctionDescriptor.ofVoid();
            tests.add(abi.downcallHandle(ma, mt, fd), null);
            tests.add(abi.downcallHandle(ma, mt, fd.withAttribute(TRIVIAL_ATTRIBUTE_NAME, true)), null);
        }

        addIdentity.add("identity_char",   byte.class,   C_CHAR,   (byte) 10);
        addIdentity.add("identity_short",  short.class,  C_SHORT, (short) 10);
        addIdentity.add("identity_int",    int.class,    C_INT,           10);
        addIdentity.add("identity_long",   long.class,   C_LONG_LONG,     10L);
        addIdentity.add("identity_float",  float.class,  C_FLOAT,         10F);
        addIdentity.add("identity_double", double.class, C_DOUBLE,        10D);

        { // identity_va
            MemoryAddress ma = LOOKUP.lookup("identity_va").get();
            MethodType mt = methodType(int.class, int.class, double.class, int.class, float.class, long.class);
            FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT, asVarArg(C_DOUBLE),
                    asVarArg(C_INT), asVarArg(C_FLOAT), asVarArg(C_LONG_LONG));
            tests.add(abi.downcallHandle(ma, mt, fd), 1, 1, 10D, 2, 3F, 4L);
            tests.add(abi.downcallHandle(ma, mt, fd.withAttribute(TRIVIAL_ATTRIBUTE_NAME, true)), 1, 1, 10D, 2, 3F, 4L);
        }

        { // high_arity
            MethodType baseMT = methodType(void.class, int.class, double.class, long.class, float.class, byte.class,
                    short.class, char.class);
            FunctionDescriptor baseFD = FunctionDescriptor.ofVoid(C_INT, C_DOUBLE, C_LONG_LONG, C_FLOAT, C_CHAR,
                    C_SHORT, C_SHORT);
            Object[] args = {1, 10D, 2L, 3F, (byte) 0, (short) 13, 'a'};
            for (int i = 0; i < args.length; i++) {
                MemoryAddress ma = LOOKUP.lookup("invoke_high_arity" + i).get();
                MethodType mt = baseMT.changeReturnType(baseMT.parameterType(i));
                FunctionDescriptor fd = baseFD.withReturnLayout(baseFD.argumentLayouts().get(i));
                Object expected = args[i];
                tests.add(abi.downcallHandle(ma, mt, fd), expected, args);
                tests.add(abi.downcallHandle(ma, mt, fd.withAttribute(TRIVIAL_ATTRIBUTE_NAME, true)), expected, args);
            }
        }

        return testsList.stream().map(rx -> new Object[]{ rx }).toArray(Object[][]::new);
    }
}
