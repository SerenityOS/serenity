/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
package jdk.internal.foreign.abi;

import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;
import jdk.internal.foreign.MemoryAddressImpl;

import java.io.PrintStream;
import java.lang.invoke.VarHandle;
import java.util.HashMap;
import java.util.Map;

class BufferLayout {
    static final VarHandle VH_LONG = MemoryLayouts.JAVA_LONG.varHandle(long.class);

    final long size;
    final long arguments_next_pc;
    final long stack_args_bytes;
    final long stack_args;

    // read by JNI
    final long[] input_type_offsets;
    final long[] output_type_offsets;

    private final Map<jdk.internal.foreign.abi.VMStorage, Long> argOffsets;
    private final Map<jdk.internal.foreign.abi.VMStorage, Long> retOffsets;

    private BufferLayout(long size, long arguments_next_pc, long stack_args_bytes, long stack_args,
                         long[] input_type_offsets, long[] output_type_offsets,
                         Map<jdk.internal.foreign.abi.VMStorage, Long> argOffsets, Map<jdk.internal.foreign.abi.VMStorage, Long> retOffsets) {
        this.size = size;
        this.arguments_next_pc = arguments_next_pc;
        this.stack_args_bytes = stack_args_bytes;
        this.stack_args = stack_args;
        this.input_type_offsets = input_type_offsets;
        this.output_type_offsets = output_type_offsets;
        this.argOffsets = argOffsets;
        this.retOffsets = retOffsets;
    }

    static BufferLayout of(ABIDescriptor abi) {
        long offset = 0;

        offset = SharedUtils.alignUp(offset, 8);
        long arguments_next_pc = offset;
        offset += 8;

        offset = SharedUtils.alignUp(offset, 8);
        long stack_args_bytes = offset;
        offset += 8;

        offset = SharedUtils.alignUp(offset, 8);
        long stack_args = offset;
        offset += 8;

        Map<jdk.internal.foreign.abi.VMStorage, Long> argOffsets = new HashMap<>();
        long[] input_type_offsets = new long[abi.inputStorage.length];
        for (int i = 0; i < abi.inputStorage.length; i++) {
            long size = abi.arch.typeSize(i);
            offset = SharedUtils.alignUp(offset, size);
            input_type_offsets[i] = offset;
            for (jdk.internal.foreign.abi.VMStorage store : abi.inputStorage[i]) {
                argOffsets.put(store, offset);
                offset += size;
            }
        }

        Map<jdk.internal.foreign.abi.VMStorage, Long> retOffsets = new HashMap<>();
        long[] output_type_offsets = new long[abi.outputStorage.length];
        for (int i = 0; i < abi.outputStorage.length; i++) {
            long size = abi.arch.typeSize(i);
            offset = SharedUtils.alignUp(offset, size);
            output_type_offsets[i] = offset;
            for (jdk.internal.foreign.abi.VMStorage store : abi.outputStorage[i]) {
                retOffsets.put(store, offset);
                offset += size;
            }
        }

        return new BufferLayout(offset, arguments_next_pc, stack_args_bytes, stack_args,
                input_type_offsets, output_type_offsets, argOffsets, retOffsets);
    }

    long argOffset(jdk.internal.foreign.abi.VMStorage storage) {
        return argOffsets.get(storage);
    }

    long retOffset(jdk.internal.foreign.abi.VMStorage storage) {
        return retOffsets.get(storage);
    }

    private static String getLongString(MemorySegment buffer, long offset) {
        return Long.toHexString((long) VH_LONG.get(buffer.asSlice(offset)));
    }

    private void dumpValues(jdk.internal.foreign.abi.Architecture arch, MemorySegment buff, PrintStream stream,
                                   Map<jdk.internal.foreign.abi.VMStorage, Long> offsets) {
        for (var entry : offsets.entrySet()) {
            VMStorage storage = entry.getKey();
            stream.print(storage.name());
            stream.print("={ ");
            MemorySegment start = buff.asSlice(entry.getValue());
            for (int i = 0; i < arch.typeSize(storage.type()) / 8; i += 8) {
                stream.print(getLongString(start, i));
                stream.print(" ");
            }
            stream.println("}");
        }
        long stack_ptr = (long) VH_LONG.get(buff.asSlice(stack_args));
        long stack_bytes = (long) VH_LONG.get(buff.asSlice(stack_args_bytes));
        MemorySegment stackArgs = MemoryAddressImpl.ofLongUnchecked(stack_ptr, stack_bytes);
        stream.println("Stack {");
        for (int i = 0; i < stack_bytes / 8; i += 8) {
            stream.printf("    @%d: %s%n", i, getLongString(stackArgs, i));
        }
        stream.println("}");
    }

    void dump(Architecture arch, MemorySegment buff, PrintStream stream) {
        stream.println("Next PC: " + getLongString(buff, arguments_next_pc));
        stream.println("Stack args bytes: " + getLongString(buff, stack_args_bytes));
        stream.println("Stack args ptr: " + getLongString(buff, stack_args));

        stream.println("Arguments:");
        dumpValues(arch, buff, stream, argOffsets);
        stream.println("Returns:");
        dumpValues(arch, buff, stream, retOffsets);
    }
}
