/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Represents a location where a value can be stored. This can be either a {@link Register} or a
 * stack slot.
 */
public final class Location {

    public final Register reg;
    public final int offset;

    private Location(Register reg, int offset) {
        this.reg = reg;
        this.offset = offset;
    }

    /**
     * Create a {@link Location} for a register.
     */
    public static Location register(Register reg) {
        return new Location(reg, 0);
    }

    /**
     * Create a {@link Location} for a vector subregister.
     *
     * @param reg the {@link Register vector register}
     * @param offset the offset in bytes into the vector register
     */
    public static Location subregister(Register reg, int offset) {
        return new Location(reg, offset);
    }

    /**
     * Create a {@link Location} for a stack slot.
     */
    public static Location stack(int offset) {
        return new Location(null, offset);
    }

    public boolean isRegister() {
        return reg != null;
    }

    public boolean isStack() {
        return reg == null;
    }

    @Override
    public String toString() {
        String regName;
        if (isRegister()) {
            regName = reg.name + ":";
        } else {
            regName = "stack:";
        }
        return regName + offset;
    }
}
