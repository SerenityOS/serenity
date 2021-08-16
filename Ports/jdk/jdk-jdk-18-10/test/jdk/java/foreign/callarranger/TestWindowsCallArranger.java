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
 *          jdk.incubator.foreign/jdk.internal.foreign.abi.x64.windows
 * @build CallArrangerTestBase
 * @run testng TestWindowsCallArranger
 */

import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.internal.foreign.abi.Binding;
import jdk.internal.foreign.abi.CallingSequence;
import jdk.internal.foreign.abi.x64.windows.CallArranger;
import org.testng.annotations.Test;

import java.lang.invoke.MethodType;

import static jdk.internal.foreign.PlatformLayouts.Win64.*;
import static jdk.internal.foreign.abi.Binding.*;
import static jdk.internal.foreign.abi.Binding.copy;
import static jdk.internal.foreign.abi.x64.X86_64Architecture.*;
import static org.testng.Assert.*;

public class TestWindowsCallArranger extends CallArrangerTestBase {

    @Test
    public void testEmpty() {
        MethodType mt = MethodType.methodType(void.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid();
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt);
        assertEquals(callingSequence.functionDesc(), fd);

        checkArgumentBindings(callingSequence, new Binding[][]{});
        checkReturnBindings(callingSequence, new Binding[]{});
    }

    @Test
    public void testIntegerRegs() {
        MethodType mt = MethodType.methodType(void.class, int.class, int.class, int.class, int.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(C_INT, C_INT, C_INT, C_INT);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt);
        assertEquals(callingSequence.functionDesc(), fd);

        checkArgumentBindings(callingSequence, new Binding[][]{
            { vmStore(rcx, int.class) },
            { vmStore(rdx, int.class) },
            { vmStore(r8, int.class) },
            { vmStore(r9, int.class) }
        });

        checkReturnBindings(callingSequence, new Binding[]{});
    }

    @Test
    public void testDoubleRegs() {
        MethodType mt = MethodType.methodType(void.class, double.class, double.class, double.class, double.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(C_DOUBLE, C_DOUBLE, C_DOUBLE, C_DOUBLE);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt);
        assertEquals(callingSequence.functionDesc(), fd);

        checkArgumentBindings(callingSequence, new Binding[][]{
            { vmStore(xmm0, double.class) },
            { vmStore(xmm1, double.class) },
            { vmStore(xmm2, double.class) },
            { vmStore(xmm3, double.class) }
        });

        checkReturnBindings(callingSequence, new Binding[]{});
    }

    @Test
    public void testMixed() {
        MethodType mt = MethodType.methodType(void.class,
                long.class, long.class, float.class, float.class, long.class, long.class, float.class, float.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(
                C_LONG_LONG, C_LONG_LONG, C_FLOAT, C_FLOAT, C_LONG_LONG, C_LONG_LONG, C_FLOAT, C_FLOAT);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt);
        assertEquals(callingSequence.functionDesc(), fd);

        checkArgumentBindings(callingSequence, new Binding[][]{
            { vmStore(rcx, long.class) },
            { vmStore(rdx, long.class) },
            { vmStore(xmm2, float.class) },
            { vmStore(xmm3, float.class) },
            { vmStore(stackStorage(0), long.class) },
            { vmStore(stackStorage(1), long.class) },
            { vmStore(stackStorage(2), float.class) },
            { vmStore(stackStorage(3), float.class) }
        });

        checkReturnBindings(callingSequence, new Binding[]{});
    }

    @Test
    public void testAbiExample() {
        MemoryLayout structLayout = MemoryLayout.structLayout(C_INT, C_INT, C_DOUBLE);
        MethodType mt = MethodType.methodType(void.class,
                int.class, int.class, MemorySegment.class, int.class, int.class,
                double.class, double.class, double.class, int.class, int.class, int.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(
                C_INT, C_INT, structLayout, C_INT, C_INT,
                C_DOUBLE, C_DOUBLE, C_DOUBLE, C_INT, C_INT, C_INT);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt);
        assertEquals(callingSequence.functionDesc(), fd);

        checkArgumentBindings(callingSequence, new Binding[][]{
            { vmStore(rcx, int.class) },
            { vmStore(rdx, int.class) },
            {
                copy(structLayout),
                baseAddress(),
                unboxAddress(),
                vmStore(r8, long.class)
            },
            { vmStore(r9, int.class) },
            { vmStore(stackStorage(0), int.class) },
            { vmStore(stackStorage(1), double.class) },
            { vmStore(stackStorage(2), double.class) },
            { vmStore(stackStorage(3), double.class) },
            { vmStore(stackStorage(4), int.class) },
            { vmStore(stackStorage(5), int.class) },
            { vmStore(stackStorage(6), int.class) }
        });

        checkReturnBindings(callingSequence, new Binding[]{});
    }

    @Test
    public void testAbiExampleVarargs() {
        MethodType mt = MethodType.methodType(void.class,
                int.class, double.class, int.class, double.class, double.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(
                C_INT, C_DOUBLE, asVarArg(C_INT), asVarArg(C_DOUBLE), asVarArg(C_DOUBLE));
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt);
        assertEquals(callingSequence.functionDesc(), fd);

        checkArgumentBindings(callingSequence, new Binding[][]{
            { vmStore(rcx, int.class) },
            { vmStore(xmm1, double.class) },
            { vmStore(r8, int.class) },
            { dup(), vmStore(r9, double.class), vmStore(xmm3, double.class) },
            { vmStore(stackStorage(0), double.class) },
        });

        checkReturnBindings(callingSequence, new Binding[]{});
    }

    /**
     * struct s {
     *   uint64_t u0;
     * } s;
     *
     * void m(struct s s);
     *
     * m(s);
     */
    @Test
    public void testStructRegister() {
        MemoryLayout struct = MemoryLayout.structLayout(C_LONG_LONG);

        MethodType mt = MethodType.methodType(void.class, MemorySegment.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(struct);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt);
        assertEquals(callingSequence.functionDesc(), fd);

        checkArgumentBindings(callingSequence, new Binding[][]{
            { bufferLoad(0, long.class), vmStore(rcx, long.class) }
        });

        checkReturnBindings(callingSequence, new Binding[]{});
    }

    /**
     * struct s {
     *   uint64_t u0, u1;
     * } s;
     *
     * void m(struct s s);
     *
     * m(s);
     */
    @Test
    public void testStructReference() {
        MemoryLayout struct = MemoryLayout.structLayout(C_LONG_LONG, C_LONG_LONG);

        MethodType mt = MethodType.methodType(void.class, MemorySegment.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(struct);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt);
        assertEquals(callingSequence.functionDesc(), fd);

        checkArgumentBindings(callingSequence, new Binding[][]{
            {
                copy(struct),
                baseAddress(),
                unboxAddress(),
                vmStore(rcx, long.class)
            }
        });

        checkReturnBindings(callingSequence, new Binding[]{});
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
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(C_POINTER);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt);
        assertEquals(callingSequence.functionDesc(), fd);

        checkArgumentBindings(callingSequence, new Binding[][]{
            { unboxAddress(), vmStore(rcx, long.class) }
        });

        checkReturnBindings(callingSequence, new Binding[]{});
    }

    @Test
    public void testReturnRegisterStruct() {
        MemoryLayout struct = MemoryLayout.structLayout(C_LONG_LONG);

        MethodType mt = MethodType.methodType(MemorySegment.class);
        FunctionDescriptor fd = FunctionDescriptor.of(struct);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt);
        assertEquals(callingSequence.functionDesc(), fd);

        checkArgumentBindings(callingSequence, new Binding[][]{});

        checkReturnBindings(callingSequence,
            new Binding[]{ allocate(struct),
                dup(),
                vmLoad(rax, long.class),
                bufferStore(0, long.class) });
    }

    @Test
    public void testIMR() {
        MemoryLayout struct = MemoryLayout.structLayout(C_LONG_LONG, C_LONG_LONG);

        MethodType mt = MethodType.methodType(MemorySegment.class);
        FunctionDescriptor fd = FunctionDescriptor.of(struct);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertTrue(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), MethodType.methodType(void.class, MemoryAddress.class));
        assertEquals(callingSequence.functionDesc(), FunctionDescriptor.ofVoid(C_POINTER));

        checkArgumentBindings(callingSequence, new Binding[][]{
            { unboxAddress(), vmStore(rcx, long.class) }
        });

        checkReturnBindings(callingSequence, new Binding[]{});
    }

    @Test
    public void testStackStruct() {
        MemoryLayout struct = MemoryLayout.structLayout(C_POINTER, C_DOUBLE, C_INT);

        MethodType mt = MethodType.methodType(void.class,
            MemorySegment.class, int.class, double.class, MemoryAddress.class,
            MemorySegment.class, int.class, double.class, MemoryAddress.class,
            MemorySegment.class, int.class, double.class, MemoryAddress.class,
            MemorySegment.class, int.class, double.class, MemoryAddress.class);
        FunctionDescriptor fd = FunctionDescriptor.ofVoid(
            struct, C_INT, C_DOUBLE, C_POINTER,
            struct, C_INT, C_DOUBLE, C_POINTER,
            struct, C_INT, C_DOUBLE, C_POINTER,
            struct, C_INT, C_DOUBLE, C_POINTER);
        CallArranger.Bindings bindings = CallArranger.getBindings(mt, fd, false);

        assertFalse(bindings.isInMemoryReturn);
        CallingSequence callingSequence = bindings.callingSequence;
        assertEquals(callingSequence.methodType(), mt);
        assertEquals(callingSequence.functionDesc(), fd);

        checkArgumentBindings(callingSequence, new Binding[][]{
            { copy(struct), baseAddress(), unboxAddress(), vmStore(rcx, long.class) },
            { vmStore(rdx, int.class) },
            { vmStore(xmm2, double.class) },
            { unboxAddress(), vmStore(r9, long.class) },
            { copy(struct), baseAddress(), unboxAddress(), vmStore(stackStorage(0), long.class) },
            { vmStore(stackStorage(1), int.class) },
            { vmStore(stackStorage(2), double.class) },
            { unboxAddress(), vmStore(stackStorage(3), long.class) },
            { copy(struct), baseAddress(), unboxAddress(), vmStore(stackStorage(4), long.class) },
            { vmStore(stackStorage(5), int.class) },
            { vmStore(stackStorage(6), double.class) },
            { unboxAddress(), vmStore(stackStorage(7), long.class) },
            { copy(struct), baseAddress(), unboxAddress(), vmStore(stackStorage(8), long.class) },
            { vmStore(stackStorage(9), int.class) },
            { vmStore(stackStorage(10), double.class) },
            { unboxAddress(), vmStore(stackStorage(11), long.class) },
        });

        checkReturnBindings(callingSequence, new Binding[]{});
    }
}
