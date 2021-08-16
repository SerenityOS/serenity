/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.code;

import java.nio.ByteOrder;
import java.util.Set;

import jdk.vm.ci.code.Register.RegisterCategory;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.PlatformKind;

/**
 * Represents a CPU architecture, including information such as its endianness, CPU registers, word
 * width, etc.
 */
public abstract class Architecture {

    /**
     * The architecture specific type of a native word.
     */
    private final PlatformKind wordKind;

    /**
     * The name of this architecture (e.g. "AMD64").
     */
    private final String name;

    /**
     * List of all available registers on this architecture. The index of each register in this list
     * is equal to its {@linkplain Register#number number}.
     */
    private final RegisterArray registers;

    /**
     * The byte ordering can be either little or big endian.
     */
    private final ByteOrder byteOrder;

    /**
     * Whether the architecture supports unaligned memory accesses.
     */
    private final boolean unalignedMemoryAccess;

    /**
     * Mask of the barrier constants denoting the barriers that are not required to be explicitly
     * inserted under this architecture.
     */
    private final int implicitMemoryBarriers;

    /**
     * Offset in bytes from the beginning of a call instruction to the displacement.
     */
    private final int machineCodeCallDisplacementOffset;

    /**
     * The size of the return address pushed to the stack by a call instruction. A value of 0
     * denotes that call linkage uses registers instead (e.g. SPARC).
     */
    private final int returnAddressSize;

    protected Architecture(String name, PlatformKind wordKind, ByteOrder byteOrder, boolean unalignedMemoryAccess, RegisterArray registers, int implicitMemoryBarriers,
                    int nativeCallDisplacementOffset,
                    int returnAddressSize) {
        this.name = name;
        this.registers = registers;
        this.wordKind = wordKind;
        this.byteOrder = byteOrder;
        this.unalignedMemoryAccess = unalignedMemoryAccess;
        this.implicitMemoryBarriers = implicitMemoryBarriers;
        this.machineCodeCallDisplacementOffset = nativeCallDisplacementOffset;
        this.returnAddressSize = returnAddressSize;
    }

    /**
     * Gets the set of CPU features supported by the current platform.
     */
    public abstract Set<? extends CPUFeatureName> getFeatures();

    /**
     * Converts this architecture to a string.
     *
     * @return the string representation of this architecture
     */
    @Override
    public final String toString() {
        return getName().toLowerCase();
    }

    /**
     * Gets the natural size of words (typically registers and pointers) of this architecture, in
     * bytes.
     */
    public int getWordSize() {
        return wordKind.getSizeInBytes();
    }

    public PlatformKind getWordKind() {
        return wordKind;
    }

    /**
     * Gets the name of this architecture.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the list of all registers that exist on this architecture. This contains all registers
     * that exist in the specification of this architecture. Not all of them may be available on
     * this particular architecture instance. The index of each register in this list is equal to
     * its {@linkplain Register#number number}.
     */
    public RegisterArray getRegisters() {
        return registers;
    }

    /**
     * Gets a list of all registers available for storing values on this architecture. This may be a
     * subset of {@link #getRegisters()}, depending on the capabilities of this particular CPU.
     */
    public RegisterArray getAvailableValueRegisters() {
        return getRegisters();
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    /**
     * @return true if the architecture supports unaligned memory accesses.
     */
    public boolean supportsUnalignedMemoryAccess() {
        return unalignedMemoryAccess;
    }

    /**
     * Gets the size of the return address pushed to the stack by a call instruction. A value of 0
     * denotes that call linkage uses registers instead.
     */
    public int getReturnAddressSize() {
        return returnAddressSize;
    }

    /**
     * Gets the offset in bytes from the beginning of a call instruction to the displacement.
     */
    public int getMachineCodeCallDisplacementOffset() {
        return machineCodeCallDisplacementOffset;
    }

    /**
     * Determines the barriers in a given barrier mask that are explicitly required on this
     * architecture.
     *
     * @param barriers a mask of the barrier constants
     * @return the value of {@code barriers} minus the barriers unnecessary on this architecture
     */
    public final int requiredBarriers(int barriers) {
        return barriers & ~implicitMemoryBarriers;
    }

    /**
     * Determine whether a kind can be stored in a register of a given category.
     *
     * @param category the category of the register
     * @param kind the kind that should be stored in the register
     */
    public abstract boolean canStoreValue(RegisterCategory category, PlatformKind kind);

    /**
     * Return the largest kind that can be stored in a register of a given category.
     *
     * @param category the category of the register
     * @return the largest kind that can be stored in a register {@code category}
     */
    public abstract PlatformKind getLargestStorableKind(RegisterCategory category);

    /**
     * Gets the {@link PlatformKind} that is used to store values of a given {@link JavaKind}.
     *
     * @return {@code null} if there no deterministic {@link PlatformKind} for {@code javaKind}
     */
    public abstract PlatformKind getPlatformKind(JavaKind javaKind);

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Architecture) {
            Architecture that = (Architecture) obj;
            if (this.name.equals(that.name)) {
                assert this.byteOrder.equals(that.byteOrder);
                assert this.implicitMemoryBarriers == that.implicitMemoryBarriers;
                assert this.machineCodeCallDisplacementOffset == that.machineCodeCallDisplacementOffset;
                assert this.registers.equals(that.registers);
                assert this.returnAddressSize == that.returnAddressSize;
                assert this.unalignedMemoryAccess == that.unalignedMemoryAccess;
                assert this.wordKind == that.wordKind;
                return true;
            }
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return name.hashCode();
    }
}
