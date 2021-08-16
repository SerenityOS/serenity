/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.aarch64;

import java.nio.ByteOrder;
import java.util.EnumSet;

import jdk.vm.ci.code.Architecture;
import jdk.vm.ci.code.CPUFeatureName;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.Register.RegisterCategory;
import jdk.vm.ci.code.RegisterArray;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.PlatformKind;

/**
 * Represents the AArch64 architecture.
 */
public class AArch64 extends Architecture {

    public static final RegisterCategory CPU = new RegisterCategory("CPU");

    // General purpose CPU registers
    public static final Register r0 = new Register(0, 0, "r0", CPU);
    public static final Register r1 = new Register(1, 1, "r1", CPU);
    public static final Register r2 = new Register(2, 2, "r2", CPU);
    public static final Register r3 = new Register(3, 3, "r3", CPU);
    public static final Register r4 = new Register(4, 4, "r4", CPU);
    public static final Register r5 = new Register(5, 5, "r5", CPU);
    public static final Register r6 = new Register(6, 6, "r6", CPU);
    public static final Register r7 = new Register(7, 7, "r7", CPU);
    public static final Register r8 = new Register(8, 8, "r8", CPU);
    public static final Register r9 = new Register(9, 9, "r9", CPU);
    public static final Register r10 = new Register(10, 10, "r10", CPU);
    public static final Register r11 = new Register(11, 11, "r11", CPU);
    public static final Register r12 = new Register(12, 12, "r12", CPU);
    public static final Register r13 = new Register(13, 13, "r13", CPU);
    public static final Register r14 = new Register(14, 14, "r14", CPU);
    public static final Register r15 = new Register(15, 15, "r15", CPU);
    public static final Register r16 = new Register(16, 16, "r16", CPU);
    public static final Register r17 = new Register(17, 17, "r17", CPU);
    public static final Register r18 = new Register(18, 18, "r18", CPU);
    public static final Register r19 = new Register(19, 19, "r19", CPU);
    public static final Register r20 = new Register(20, 20, "r20", CPU);
    public static final Register r21 = new Register(21, 21, "r21", CPU);
    public static final Register r22 = new Register(22, 22, "r22", CPU);
    public static final Register r23 = new Register(23, 23, "r23", CPU);
    public static final Register r24 = new Register(24, 24, "r24", CPU);
    public static final Register r25 = new Register(25, 25, "r25", CPU);
    public static final Register r26 = new Register(26, 26, "r26", CPU);
    public static final Register r27 = new Register(27, 27, "r27", CPU);
    public static final Register r28 = new Register(28, 28, "r28", CPU);
    public static final Register r29 = new Register(29, 29, "r29", CPU);
    public static final Register r30 = new Register(30, 30, "r30", CPU);

    /*
     * r31 is not a general purpose register, but represents either the stackpointer or the
     * zero/discard register depending on the instruction. So we represent those two uses as two
     * different registers. The register numbers are kept in sync with register_aarch64.hpp and have
     * to be sequential, hence we also need a general r31 register here, which is never used.
     */
    public static final Register r31 = new Register(31, 31, "r31", CPU);
    public static final Register zr = new Register(32, 31, "zr", CPU);
    public static final Register sp = new Register(33, 31, "sp", CPU);

    public static final Register lr = r30;

    // Used by runtime code: cannot be compiler-allocated.
    public static final Register rscratch1 = r8;
    public static final Register rscratch2 = r9;

    // @formatter:off
    public static final RegisterArray cpuRegisters = new RegisterArray(
        r0,  r1,  r2,  r3,  r4,  r5,  r6,  r7,
        r8,  r9,  r10, r11, r12, r13, r14, r15,
        r16, r17, r18, r19, r20, r21, r22, r23,
        r24, r25, r26, r27, r28, r29, r30, r31,
        zr,  sp
    );
    // @formatter:on

    public static final RegisterCategory SIMD = new RegisterCategory("SIMD");

    // Simd registers
    public static final Register v0 = new Register(34, 0, "v0", SIMD);
    public static final Register v1 = new Register(35, 1, "v1", SIMD);
    public static final Register v2 = new Register(36, 2, "v2", SIMD);
    public static final Register v3 = new Register(37, 3, "v3", SIMD);
    public static final Register v4 = new Register(38, 4, "v4", SIMD);
    public static final Register v5 = new Register(39, 5, "v5", SIMD);
    public static final Register v6 = new Register(40, 6, "v6", SIMD);
    public static final Register v7 = new Register(41, 7, "v7", SIMD);
    public static final Register v8 = new Register(42, 8, "v8", SIMD);
    public static final Register v9 = new Register(43, 9, "v9", SIMD);
    public static final Register v10 = new Register(44, 10, "v10", SIMD);
    public static final Register v11 = new Register(45, 11, "v11", SIMD);
    public static final Register v12 = new Register(46, 12, "v12", SIMD);
    public static final Register v13 = new Register(47, 13, "v13", SIMD);
    public static final Register v14 = new Register(48, 14, "v14", SIMD);
    public static final Register v15 = new Register(49, 15, "v15", SIMD);
    public static final Register v16 = new Register(50, 16, "v16", SIMD);
    public static final Register v17 = new Register(51, 17, "v17", SIMD);
    public static final Register v18 = new Register(52, 18, "v18", SIMD);
    public static final Register v19 = new Register(53, 19, "v19", SIMD);
    public static final Register v20 = new Register(54, 20, "v20", SIMD);
    public static final Register v21 = new Register(55, 21, "v21", SIMD);
    public static final Register v22 = new Register(56, 22, "v22", SIMD);
    public static final Register v23 = new Register(57, 23, "v23", SIMD);
    public static final Register v24 = new Register(58, 24, "v24", SIMD);
    public static final Register v25 = new Register(59, 25, "v25", SIMD);
    public static final Register v26 = new Register(60, 26, "v26", SIMD);
    public static final Register v27 = new Register(61, 27, "v27", SIMD);
    public static final Register v28 = new Register(62, 28, "v28", SIMD);
    public static final Register v29 = new Register(63, 29, "v29", SIMD);
    public static final Register v30 = new Register(64, 30, "v30", SIMD);
    public static final Register v31 = new Register(65, 31, "v31", SIMD);

    // @formatter:off
    public static final RegisterArray simdRegisters = new RegisterArray(
        v0,  v1,  v2,  v3,  v4,  v5,  v6,  v7,
        v8,  v9,  v10, v11, v12, v13, v14, v15,
        v16, v17, v18, v19, v20, v21, v22, v23,
        v24, v25, v26, v27, v28, v29, v30, v31
    );
    // @formatter:on

    // @formatter:off
    public static final RegisterArray allRegisters = new RegisterArray(
        r0,  r1,  r2,  r3,  r4,  r5,  r6,  r7,
        r8,  r9,  r10, r11, r12, r13, r14, r15,
        r16, r17, r18, r19, r20, r21, r22, r23,
        r24, r25, r26, r27, r28, r29, r30, r31,
        zr,  sp,

        v0,  v1,  v2,  v3,  v4,  v5,  v6,  v7,
        v8,  v9,  v10, v11, v12, v13, v14, v15,
        v16, v17, v18, v19, v20, v21, v22, v23,
        v24, v25, v26, v27, v28, v29, v30, v31
    );
    // @formatter:on

    /**
     * Basic set of CPU features mirroring what is returned from the cpuid instruction. See:
     * {@code VM_Version::cpuFeatureFlags}.
     */
    public enum CPUFeature implements CPUFeatureName {
        FP,
        ASIMD,
        EVTSTRM,
        AES,
        PMULL,
        SHA1,
        SHA2,
        CRC32,
        LSE,
        DCPOP,
        SHA3,
        SHA512,
        SVE,
        SVE2,
        STXR_PREFETCH,
        A53MAC,
    }

    private final EnumSet<CPUFeature> features;

    /**
     * Set of flags to control code emission.
     */
    public enum Flag {
        UseCRC32,
        UseNeon,
        UseSIMDForMemoryOps,
        AvoidUnalignedAccesses,
        UseLSE,
        UseBlockZeroing
    }

    private final EnumSet<Flag> flags;

    public AArch64(EnumSet<CPUFeature> features, EnumSet<Flag> flags) {
        super("aarch64", AArch64Kind.QWORD, ByteOrder.LITTLE_ENDIAN, true, allRegisters, 0, 0, 0);
        this.features = features;
        this.flags = flags;
    }

    @Override
    public EnumSet<CPUFeature> getFeatures() {
        return features;
    }

    public EnumSet<Flag> getFlags() {
        return flags;
    }

    @Override
    public PlatformKind getPlatformKind(JavaKind javaKind) {
        switch (javaKind) {
            case Boolean:
            case Byte:
                return AArch64Kind.BYTE;
            case Short:
            case Char:
                return AArch64Kind.WORD;
            case Int:
                return AArch64Kind.DWORD;
            case Long:
            case Object:
                return AArch64Kind.QWORD;
            case Float:
                return AArch64Kind.SINGLE;
            case Double:
                return AArch64Kind.DOUBLE;
            default:
                return null;
        }
    }

    @Override
    public boolean canStoreValue(RegisterCategory category, PlatformKind platformKind) {
        AArch64Kind kind = (AArch64Kind) platformKind;
        if (kind.isInteger()) {
            return category.equals(CPU);
        } else if (kind.isSIMD()) {
            return category.equals(SIMD);
        }
        return false;
    }

    @Override
    public AArch64Kind getLargestStorableKind(RegisterCategory category) {
        if (category.equals(CPU)) {
            return AArch64Kind.QWORD;
        } else if (category.equals(SIMD)) {
            return AArch64Kind.V128_QWORD;
        } else {
            return null;
        }
    }
}
