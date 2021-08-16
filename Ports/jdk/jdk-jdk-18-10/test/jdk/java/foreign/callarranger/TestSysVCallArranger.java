/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

/*
 * @test
 * @modules jdk.incubator.foreign/jdk.internal.foreign
 *          jdk.incubator.foreign/jdk.internal.foreign.abi
 *          jdk.incubator.foreign/jdk.internal.foreign.abi.x64
 *          jdk.incubator.foreign/jdk.internal.foreign.abi.x64.sysv
 * @build CallArrangerTestBase
 * @run testng TestSysVCallArranger
 */

import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.internal.foreign.abi.Binding;
import jdk.internal.foreign.abi.CallingSequence;
import jdk.internal.foreign.abi.x64.sysv.CallArranger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodType;

import static jdk.internal.foreign.PlatformLayouts.SysV.*;
import static jdk.internal.foreign.abi.Binding.*;
import static jdk.internal.foreign.abi.x64.X86_64Architecture.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TestSysVCallArranger extends CallArrangerTestBase {

    @Test
    public void testEmpty() {
        MethodType mt = MethodType.methodType(void.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid();
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt.appendParameterTypes(long.class));
        assertEquals(callingSequence.functionDesc(), fd.withAppendedArgumentLayouts(C_LONG));

        checkArgumentBindings(callingSequence, new Binding[][]{
            { vmStore(rax, long.class) }
        });

        checkReturnBindings(callingSequence, new Binding[]{});

        assertEquals(bindings.nVectorArgs, 0);
    }

    @Test
    public void testNestedStructs() {
        MemoryLayout POINT = MemoryLayout.structLayout(
                C_INT,
                MemoryLayout.structLayout(
                        C_INT,
                        C_INT
                )
        );
        MethodType mt = MethodType.methodType(void.class, MemorySegment.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(POINT);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt.appendParameterTypes(long.class));
        assertEquals(callingSequence.functionDesc(), fd.withAppendedArgumentLayouts(C_LONG));

        checkArgumentBindings(callingSequence, new Binding[][]{
                { dup(), bufferLoad(0, long.class), vmStore(rdi, long.class),
                  bufferLoad(8, int.class), vmStore(rsi, int.class)},
                { vmStore(rax, long.class) },
        });

        checkReturnBindings(callingSequence, new Binding[]{});

        assertEquals(bindings.nVectorArgs, 0);
    }

    @Test
    public void testNestedUnion() {
        MemoryLayout POINT = MemoryLayout.structLayout(
                C_INT,
                MemoryLayout.paddingLayout(32),
                MemoryLayout.unionLayout(
                        MemoryLayout.structLayout(C_INT, C_INT),
                        C_LONG
                )
        );
        MethodType mt = MethodType.methodType(void.class, MemorySegment.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(POINT);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt.appendParameterTypes(long.class));
        assertEquals(callingSequence.functionDesc(), fd.withAppendedArgumentLayouts(C_LONG));

        checkArgumentBindings(callingSequence, new Binding[][]{
                { dup(), bufferLoad(0, long.class), vmStore(rdi, long.class),
                        bufferLoad(8, long.class), vmStore(rsi, long.class)},
                { vmStore(rax, long.class) },
        });

        checkReturnBindings(callingSequence, new Binding[]{});

        assertEquals(bindings.nVectorArgs, 0);
    }

    @Test
    public void testNestedStructsUnaligned() {
        MemoryLayout POINT = MemoryLayout.structLayout(
                C_INT,
                MemoryLayout.structLayout(
                        C_LONG,
                        C_INT
                )
        );
        MethodType mt = MethodType.methodType(void.class, MemorySegment.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(POINT);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt.appendParameterTypes(long.class));
        assertEquals(callingSequence.functionDesc(), fd.withAppendedArgumentLayouts(C_LONG));

        checkArgumentBindings(callingSequence, new Binding[][]{
                { dup(), bufferLoad(0, long.class), vmStore(stackStorage(0), long.class),
                        bufferLoad(8, long.class), vmStore(stackStorage(1), long.class)},
                { vmStore(rax, long.class) },
        });

        checkReturnBindings(callingSequence, new Binding[]{});

        assertEquals(bindings.nVectorArgs, 0);
    }

    @Test
    public void testNestedUnionUnaligned() {
        MemoryLayout POINT = MemoryLayout.structLayout(
                C_INT,
                MemoryLayout.unionLayout(
                        MemoryLayout.structLayout(C_INT, C_INT),
                        C_LONG
                )
        );
        MethodType mt = MethodType.methodType(void.class, MemorySegment.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(POINT);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt.appendParameterTypes(long.class));
        assertEquals(callingSequence.functionDesc(), fd.withAppendedArgumentLayouts(C_LONG));

        checkArgumentBindings(callingSequence, new Binding[][]{
                { dup(), bufferLoad(0, long.class), vmStore(stackStorage(0), long.class),
                        bufferLoad(8, int.class), vmStore(stackStorage(1), int.class)},
                { vmStore(rax, long.class) },
        });

        checkReturnBindings(callingSequence, new Binding[]{});

        assertEquals(bindings.nVectorArgs, 0);
    }

    @Test
    public void testIntegerRegs() {
        MethodType mt = MethodType.methodType(void.class,
                int.class, int.class, int.class, int.class, int.class, int.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(
                C_INT, C_INT, C_INT, C_INT, C_INT, C_INT);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt.appendParameterTypes(long.class));
        assertEquals(callingSequence.functionDesc(), fd.withAppendedArgumentLayouts(C_LONG));

        checkArgumentBindings(callingSequence, new Binding[][]{
            { vmStore(rdi, int.class) },
            { vmStore(rsi, int.class) },
            { vmStore(rdx, int.class) },
            { vmStore(rcx, int.class) },
            { vmStore(r8, int.class) },
            { vmStore(r9, int.class) },
            { vmStore(rax, long.class) },
        });

        checkReturnBindings(callingSequence, new Binding[]{});

        assertEquals(bindings.nVectorArgs, 0);
    }

    @Test
    public void testDoubleRegs() {
        MethodType mt = MethodType.methodType(void.class,
                double.class, double.class, double.class, double.class,
                double.class, double.class, double.class, double.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(
                C_DOUBLE, C_DOUBLE, C_DOUBLE, C_DOUBLE,
                C_DOUBLE, C_DOUBLE, C_DOUBLE, C_DOUBLE);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt.appendParameterTypes(long.class));
        assertEquals(callingSequence.functionDesc(), fd.withAppendedArgumentLayouts(C_LONG));

        checkArgumentBindings(callingSequence, new Binding[][]{
            { vmStore(xmm0, double.class) },
            { vmStore(xmm1, double.class) },
            { vmStore(xmm2, double.class) },
            { vmStore(xmm3, double.class) },
            { vmStore(xmm4, double.class) },
            { vmStore(xmm5, double.class) },
            { vmStore(xmm6, double.class) },
            { vmStore(xmm7, double.class) },
            { vmStore(rax, long.class) },
        });

        checkReturnBindings(callingSequence, new Binding[]{});

        assertEquals(bindings.nVectorArgs, 8);
    }

    @Test
    public void testMixed() {
        MethodType mt = MethodType.methodType(void.class,
                long.class, long.class, long.class, long.class, long.class, long.class, long.class, long.class,
                float.class, float.class, float.class, float.class,
                float.class, float.class, float.class, float.class, float.class, float.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(
                C_LONG, C_LONG, C_LONG, C_LONG, C_LONG, C_LONG, C_LONG, C_LONG,
                C_FLOAT, C_FLOAT, C_FLOAT, C_FLOAT,
                C_FLOAT, C_FLOAT, C_FLOAT, C_FLOAT, C_FLOAT, C_FLOAT);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt.appendParameterTypes(long.class));
        assertEquals(callingSequence.functionDesc(), fd.withAppendedArgumentLayouts(C_LONG));

        checkArgumentBindings(callingSequence, new Binding[][]{
            { vmStore(rdi, long.class) },
            { vmStore(rsi, long.class) },
            { vmStore(rdx, long.class) },
            { vmStore(rcx, long.class) },
            { vmStore(r8, long.class) },
            { vmStore(r9, long.class) },
            { vmStore(stackStorage(0), long.class) },
            { vmStore(stackStorage(1), long.class) },
            { vmStore(xmm0, float.class) },
            { vmStore(xmm1, float.class) },
            { vmStore(xmm2, float.class) },
            { vmStore(xmm3, float.class) },
            { vmStore(xmm4, float.class) },
            { vmStore(xmm5, float.class) },
            { vmStore(xmm6, float.class) },
            { vmStore(xmm7, float.class) },
            { vmStore(stackStorage(2), float.class) },
            { vmStore(stackStorage(3), float.class) },
            { vmStore(rax, long.class) },
        });

        checkReturnBindings(callingSequence, new Binding[]{});

        assertEquals(bindings.nVectorArgs, 8);
    }

    /**
     * This is the example from the System V ABI AMD64 document
     *
     * struct structparm {
     *   int32_t a, int32_t b, double d;
     * } s;
     * int32_t e, f, g, h, i, j, k;
     * double m, n;
     *
     * void m(e, f, s, g, h, m, n, i, j, k);
     *
     * m(s);
     */
    @Test
    public void testAbiExample() {
        MemoryLayout struct = MemoryLayout.structLayout(C_INT, C_INT, C_DOUBLE);

        MethodType mt = MethodType.methodType(void.class,
                int.class, int.class, MemorySegment.class, int.class, int.class,
                double.class, double.class, int.class, int.class, int.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(
                C_INT, C_INT, struct, C_INT, C_INT, C_DOUBLE, C_DOUBLE, C_INT, C_INT, C_INT);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt.appendParameterTypes(long.class));
        assertEquals(callingSequence.functionDesc(), fd.withAppendedArgumentLayouts(C_LONG));

        checkArgumentBindings(callingSequence, new Binding[][]{
            { vmStore(rdi, int.class) },
            { vmStore(rsi, int.class) },
            {
                dup(),
                bufferLoad(0, long.class), vmStore(rdx, long.class),
                bufferLoad(8, double.class), vmStore(xmm0, double.class)
            },
            { vmStore(rcx, int.class) },
            { vmStore(r8, int.class) },
            { vmStore(xmm1, double.class) },
            { vmStore(xmm2, double.class) },
            { vmStore(r9, int.class) },
            { vmStore(stackStorage(0), int.class) },
            { vmStore(stackStorage(1), int.class) },
            { vmStore(rax, long.class) },
        });

        checkReturnBindings(callingSequence, new Binding[]{});

        assertEquals(bindings.nVectorArgs, 3);
    }

    /**
     * typedef void (*f)(void);
     *
     * void m(f f);
     * void f_impl(void);
     *
     * m(f_impl);
     */
    @Test
    public void testMemoryAddress() {
        MethodType mt = MethodType.methodType(void.class, MemoryAddress.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid( C_POINTER);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt.appendParameterTypes(long.class));
        assertEquals(callingSequence.functionDesc(), fd.withAppendedArgumentLayouts(C_LONG));

        checkArgumentBindings(callingSequence, new Binding[][]{
            { unboxAddress(), vmStore(rdi, long.class) },
            { vmStore(rax, long.class) },
        });

        checkReturnBindings(callingSequence, new Binding[]{});

        assertEquals(bindings.nVectorArgs, 0);
    }

    @Test(dataProvider = "structs")
    public void testStruct(MemoryLayout struct, Binding[] expectedBindings) {
        MethodType mt = MethodType.methodType(void.class, MemorySegment.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(struct);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt.appendParameterTypes(long.class));
        assertEquals(callingSequence.functionDesc(), fd.withAppendedArgumentLayouts(C_LONG));

        checkArgumentBindings(callingSequence, new Binding[][]{
            expectedBindings,
            { vmStore(rax, long.class) },
        });

        checkReturnBindings(callingSequence, new Binding[]{});

        assertEquals(bindings.nVectorArgs, 0);
    }


    @DataProvider
    public static Object[][] structs() {
        return new Object[][]{
            { MemoryLayout.structLayout(C_LONG), new Binding[]{
                    bufferLoad(0, long.class), vmStore(rdi, long.class)
                }
            },
            { MemoryLayout.structLayout(C_LONG, C_LONG), new Binding[]{
                    dup(),
                    bufferLoad(0, long.class), vmStore(rdi, long.class),
                    bufferLoad(8, long.class), vmStore(rsi, long.class)
                }
            },
            { MemoryLayout.structLayout(C_LONG, C_LONG, C_LONG), new Binding[]{
                    dup(),
                    bufferLoad(0, long.class), vmStore(stackStorage(0), long.class),
                    dup(),
                    bufferLoad(8, long.class), vmStore(stackStorage(1), long.class),
                    bufferLoad(16, long.class), vmStore(stackStorage(2), long.class)
                }
            },
            { MemoryLayout.structLayout(C_LONG, C_LONG, C_LONG, C_LONG), new Binding[]{
                    dup(),
                    bufferLoad(0, long.class), vmStore(stackStorage(0), long.class),
                    dup(),
                    bufferLoad(8, long.class), vmStore(stackStorage(1), long.class),
                    dup(),
                    bufferLoad(16, long.class), vmStore(stackStorage(2), long.class),
                    bufferLoad(24, long.class), vmStore(stackStorage(3), long.class)
                }
            },
        };
    }

    @Test
    public void testReturnRegisterStruct() {
        MemoryLayout struct = MemoryLayout.structLayout(C_LONG, C_LONG);

        MethodType mt = MethodType.methodType(MemorySegment.class);
        FunctionDescriptor fd = FunctionDescriptor.of(struct);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt.appendParameterTypes(long.class));
        assertEquals(callingSequence.functionDesc(), fd.withAppendedArgumentLayouts(C_LONG));

        checkArgumentBindings(callingSequence, new Binding[][]{
            { vmStore(rax, long.class) }
        });

        checkReturnBindings(callingSequence, new Binding[] {
            allocate(struct),
            dup(),
            vmLoad(rax, long.class),
            bufferStore(0, long.class),
            dup(),
            vmLoad(rdx, long.class),
            bufferStore(8, long.class)
        });

        assertEquals(bindings.nVectorArgs, 0);
    }

    @Test
    public void testIMR() {
        MemoryLayout struct = MemoryLayout.structLayout(C_LONG, C_LONG, C_LONG);

        MethodType mt = MethodType.methodType(MemorySegment.class);
        FunctionDescriptor fd = FunctionDescriptor.of(struct);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertTrue(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), MethodType.methodType(void.class, MemoryAddress.class, long.class));
        assertEquals(callingSequence.functionDesc(), FunctionDescriptor.ofVoid(C_POINTER, C_LONG));

        checkArgumentBindings(callingSequence, new Binding[][]{
            { unboxAddress(), vmStore(rdi, long.class) },
            { vmStore(rax, long.class) }
        });

        checkReturnBindings(callingSequence, new Binding[] {});

        assertEquals(bindings.nVectorArgs, 0);
    }

    @Test
    public void testFloatStructsUpcall() {
        MemoryLayout struct = MemoryLayout.structLayout(C_FLOAT); // should be passed in float regs

        MethodType mt = MethodType.methodType(MemorySegment.class, MemorySegment.class);
        FunctionDescriptor fd = FunctionDescriptor.of(struct, struct);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, true);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt);
        assertEquals(callingSequence.functionDesc(), fd);

        checkArgumentBindings(callingSequence, new Binding[][]{
            { allocate(struct), dup(), vmLoad(xmm0, float.class), bufferStore(0, float.class) },
        });

        checkReturnBindings(callingSequence, new Binding[] {
            bufferLoad(0, float.class), vmStore(xmm0, float.class)
        });

        assertEquals(bindings.nVectorArgs, 1);
    }

}
