/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * A map from registers to frame slots. This can be used to describe where callee saved registers
 * are saved in a callee's frame.
 */
public final class RegisterSaveLayout {

    /**
     * Keys.
     */
    private final Register[] registers;

    /**
     * Slot indexes relative to stack pointer.
     */
    private final int[] slots;

    /**
     * Creates a map from registers to frame slots.
     *
     * @param registers the keys in the map
     * @param slots frame slot index for each register in {@code registers}
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "caller transfers ownership of `registers` and `slots`")
    public RegisterSaveLayout(Register[] registers, int[] slots) {
        assert registers.length == slots.length;
        this.registers = registers;
        this.slots = slots;
        assert registersToSlots(false).size() == registers.length : "non-unique registers";
        assert new HashSet<>(registersToSlots(false).values()).size() == slots.length : "non-unqiue slots";
    }

    /**
     * Gets the frame slot index for a given register.
     *
     * @param register register to get the frame slot index for
     * @return frame slot index
     */
    public int registerToSlot(Register register) {
        for (int i = 0; i < registers.length; i++) {
            if (register.equals(registers[i])) {
                return slots[i];
            }
        }
        throw new IllegalArgumentException(register + " not saved by this layout: " + this);
    }

    /**
     * Gets this layout information as a {@link Map} from registers to slots.
     */
    public Map<Register, Integer> registersToSlots(boolean sorted) {
        Map<Register, Integer> result;
        if (sorted) {
            result = new TreeMap<>();
        } else {
            result = new HashMap<>();
        }
        for (int i = 0; i < registers.length; i++) {
            result.put(registers[i], slots[i]);
        }
        return result;
    }

    /**
     * Gets this layout information as a {@link Map} from slots to registers.
     */
    public Map<Integer, Register> slotsToRegisters(boolean sorted) {
        Map<Integer, Register> result;
        if (sorted) {
            result = new TreeMap<>();
        } else {
            result = new HashMap<>();
        }
        for (int i = 0; i < registers.length; i++) {
            result.put(slots[i], registers[i]);
        }
        return result;
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RegisterSaveLayout) {
            RegisterSaveLayout that = (RegisterSaveLayout) obj;
            if (Arrays.equals(registers, that.registers) && Arrays.equals(slots, that.slots)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return registersToSlots(true).toString();
    }
}
