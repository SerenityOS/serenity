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
package org.openjdk.bench.jdk.incubator.foreign;

import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import jdk.incubator.foreign.SymbolLookup;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodHandles.insertArguments;
import static jdk.incubator.foreign.CLinker.C_DOUBLE;
import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_LONG_LONG;
import static jdk.incubator.foreign.CLinker.C_POINTER;

public class CallOverheadHelper {

    static final CLinker abi = CLinker.getInstance();

    static final MethodHandle func;
    static final MethodHandle func_v;
    static Addressable func_addr;
    static final MethodHandle identity;
    static final MethodHandle identity_v;
    static Addressable identity_addr;
    static final MethodHandle identity_struct;
    static final MethodHandle identity_struct_v;
    static Addressable identity_struct_addr;
    static final MethodHandle identity_memory_address;
    static final MethodHandle identity_memory_address_v;
    static Addressable identity_memory_address_addr;
    static final MethodHandle args1;
    static final MethodHandle args1_v;
    static Addressable args1_addr;
    static final MethodHandle args2;
    static final MethodHandle args2_v;
    static Addressable args2_addr;
    static final MethodHandle args3;
    static final MethodHandle args3_v;
    static Addressable args3_addr;
    static final MethodHandle args4;
    static final MethodHandle args4_v;
    static Addressable args4_addr;
    static final MethodHandle args5;
    static final MethodHandle args5_v;
    static Addressable args5_addr;
    static final MethodHandle args10;
    static final MethodHandle args10_v;
    static Addressable args10_addr;
    static final MethodHandle func_trivial;
    static final MethodHandle func_trivial_v;
    static final MethodHandle identity_trivial;
    static final MethodHandle identity_trivial_v;

    static final MemoryLayout POINT_LAYOUT = MemoryLayout.structLayout(
            C_LONG_LONG, C_LONG_LONG
    );

    static final MemorySegment point = MemorySegment.allocateNative(POINT_LAYOUT, ResourceScope.newImplicitScope());

    static final SegmentAllocator recycling_allocator = SegmentAllocator.ofSegment(MemorySegment.allocateNative(POINT_LAYOUT, ResourceScope.newImplicitScope()));

    static {
        System.loadLibrary("CallOverheadJNI");

        System.loadLibrary("CallOverhead");
        SymbolLookup lookup = SymbolLookup.loaderLookup();
        {
            func_addr = lookup.lookup("func").orElseThrow();
            MethodType mt = MethodType.methodType(void.class);
            FunctionDescriptor fd = FunctionDescriptor.ofVoid();
            func_v = abi.downcallHandle(mt, fd);
            func = insertArguments(func_v, 0, func_addr);
            func_trivial_v = abi.downcallHandle(mt, fd.withAttribute(FunctionDescriptor.TRIVIAL_ATTRIBUTE_NAME, true));
            func_trivial = insertArguments(func_trivial_v, 0, func_addr);
        }
        {
            identity_addr = lookup.lookup("identity").orElseThrow();
            MethodType mt = MethodType.methodType(int.class, int.class);
            FunctionDescriptor fd = FunctionDescriptor.of(C_INT, C_INT);
            identity_v = abi.downcallHandle(mt, fd);
            identity = insertArguments(identity_v, 0, identity_addr);
            identity_trivial_v = abi.downcallHandle(mt, fd.withAttribute(FunctionDescriptor.TRIVIAL_ATTRIBUTE_NAME, true));
            identity_trivial = insertArguments(identity_trivial_v, 0, identity_addr);
        }
        identity_struct_addr = lookup.lookup("identity_struct").orElseThrow();
        identity_struct_v = abi.downcallHandle(
                MethodType.methodType(MemorySegment.class, MemorySegment.class),
                FunctionDescriptor.of(POINT_LAYOUT, POINT_LAYOUT));
        identity_struct = insertArguments(identity_struct_v, 0, identity_struct_addr);

        identity_memory_address_addr = lookup.lookup("identity_memory_address").orElseThrow();
        identity_memory_address_v = abi.downcallHandle(
                MethodType.methodType(MemoryAddress.class, MemoryAddress.class),
                FunctionDescriptor.of(C_POINTER, C_POINTER));
        identity_memory_address = insertArguments(identity_memory_address_v, 0, identity_memory_address_addr);

        args1_addr = lookup.lookup("args1").orElseThrow();
        args1_v = abi.downcallHandle(
                MethodType.methodType(void.class, long.class),
                FunctionDescriptor.ofVoid(C_LONG_LONG));
        args1 = insertArguments(args1_v, 0, args1_addr);

        args2_addr = lookup.lookup("args2").orElseThrow();
        args2_v = abi.downcallHandle(
                MethodType.methodType(void.class, long.class, double.class),
                FunctionDescriptor.ofVoid(C_LONG_LONG, C_DOUBLE));
        args2 = insertArguments(args2_v, 0, args2_addr);

        args3_addr = lookup.lookup("args3").orElseThrow();
        args3_v = abi.downcallHandle(
                MethodType.methodType(void.class, long.class, double.class, long.class),
                FunctionDescriptor.ofVoid(C_LONG_LONG, C_DOUBLE, C_LONG_LONG));
        args3 = insertArguments(args3_v, 0, args3_addr);

        args4_addr = lookup.lookup("args4").orElseThrow();
        args4_v = abi.downcallHandle(
                MethodType.methodType(void.class, long.class, double.class, long.class, double.class),
                FunctionDescriptor.ofVoid(C_LONG_LONG, C_DOUBLE, C_LONG_LONG, C_DOUBLE));
        args4 = insertArguments(args4_v, 0, args4_addr);

        args5_addr = lookup.lookup("args5").orElseThrow();
        args5_v = abi.downcallHandle(
                MethodType.methodType(void.class, long.class, double.class, long.class, double.class, long.class),
                FunctionDescriptor.ofVoid(C_LONG_LONG, C_DOUBLE, C_LONG_LONG, C_DOUBLE, C_LONG_LONG));
        args5 = insertArguments(args5_v, 0, args5_addr);

        args10_addr = lookup.lookup("args10").orElseThrow();
        args10_v = abi.downcallHandle(
                MethodType.methodType(void.class, long.class, double.class, long.class, double.class, long.class,
                                                  double.class, long.class, double.class, long.class, double.class),
                FunctionDescriptor.ofVoid(C_LONG_LONG, C_DOUBLE, C_LONG_LONG, C_DOUBLE, C_LONG_LONG,
                                          C_DOUBLE, C_LONG_LONG, C_DOUBLE, C_LONG_LONG, C_DOUBLE));
        args10 = insertArguments(args10_v, 0, args10_addr);
    }

    static native void blank();
    static native int identity(int x);
}
