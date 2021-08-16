/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.internal.invoke;

import java.lang.invoke.MethodType;
import java.util.Objects;

/**
 * This class describes a native call, including arguments/return shuffle moves, PC entry point and
 * various other info which are relevant when the call will be intrinsified by C2.
 */
public class NativeEntryPoint {
    static {
        registerNatives();
    }

    private final int shadowSpace;

    // encoded as VMRegImpl*
    private final long[] argMoves;
    private final long[] returnMoves;

    private final boolean needTransition;
    private final MethodType methodType; // C2 sees erased version (byte -> int), so need this explicitly
    private final String name;

    private NativeEntryPoint(int shadowSpace, long[] argMoves, long[] returnMoves,
                     boolean needTransition, MethodType methodType, String name) {
        this.shadowSpace = shadowSpace;
        this.argMoves = Objects.requireNonNull(argMoves);
        this.returnMoves = Objects.requireNonNull(returnMoves);
        this.needTransition = needTransition;
        this.methodType = methodType;
        this.name = name;
    }

    public static NativeEntryPoint make(String name, ABIDescriptorProxy abi,
                                        VMStorageProxy[] argMoves, VMStorageProxy[] returnMoves,
                                        boolean needTransition, MethodType methodType) {
        if (returnMoves.length > 1) {
            throw new IllegalArgumentException("Multiple register return not supported");
        }

        return new NativeEntryPoint(abi.shadowSpaceBytes(), encodeVMStorages(argMoves), encodeVMStorages(returnMoves),
                needTransition, methodType, name);
    }

    private static long[] encodeVMStorages(VMStorageProxy[] moves) {
        long[] out = new long[moves.length];
        for (int i = 0; i < moves.length; i++) {
            out[i] = vmStorageToVMReg(moves[i].type(), moves[i].index());
        }
        return out;
    }

    private static native long vmStorageToVMReg(int type, int index);

    public MethodType type() {
        return methodType;
    }

    private static native void registerNatives();
}
