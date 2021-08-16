/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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

import jdk.vm.ci.meta.AllocatableValue;
import jdk.vm.ci.meta.ValueKind;

/**
 * Denotes a register that stores a value of a fixed kind.
 */
public final class RegisterValue extends AllocatableValue {

    private final Register reg;

    protected RegisterValue(ValueKind<?> kind, Register register) {
        super(kind);
        this.reg = register;
    }

    @Override
    public String toString() {
        return getRegister().name + getKindSuffix();
    }

    /**
     * @return the register that contains the value
     */
    public Register getRegister() {
        return reg;
    }

    @Override
    public int hashCode() {
        return 29 * super.hashCode() + reg.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RegisterValue) {
            RegisterValue other = (RegisterValue) obj;
            return super.equals(obj) && reg.equals(other.reg);
        }
        return false;
    }
}
