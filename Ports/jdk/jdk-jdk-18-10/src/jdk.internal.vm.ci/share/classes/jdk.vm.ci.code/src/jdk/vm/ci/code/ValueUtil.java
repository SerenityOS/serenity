/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaValue;
import jdk.vm.ci.meta.PlatformKind;
import jdk.vm.ci.meta.Value;

/**
 * Utility class for working with the {@link Value} class and its subclasses.
 */
public final class ValueUtil {

    public static boolean isIllegal(Value value) {
        assert value != null;
        return Value.ILLEGAL.equals(value);
    }

    public static boolean isIllegalJavaValue(JavaValue value) {
        assert value != null;
        return Value.ILLEGAL.equals(value);
    }

    public static boolean isLegal(Value value) {
        return !isIllegal(value);
    }

    public static boolean isVirtualObject(JavaValue value) {
        assert value != null;
        return value instanceof VirtualObject;
    }

    public static VirtualObject asVirtualObject(JavaValue value) {
        assert value != null;
        return (VirtualObject) value;
    }

    public static boolean isConstantJavaValue(JavaValue value) {
        assert value != null;
        return value instanceof JavaConstant;
    }

    public static JavaConstant asConstantJavaValue(JavaValue value) {
        assert value != null;
        return (JavaConstant) value;
    }

    public static boolean isAllocatableValue(Value value) {
        assert value != null;
        return value instanceof AllocatableValue;
    }

    public static AllocatableValue asAllocatableValue(Value value) {
        assert value != null;
        return (AllocatableValue) value;
    }

    public static boolean isStackSlot(Value value) {
        assert value != null;
        return value instanceof StackSlot;
    }

    public static StackSlot asStackSlot(Value value) {
        assert value != null;
        return (StackSlot) value;
    }

    public static boolean isRegister(Value value) {
        assert value != null;
        return value instanceof RegisterValue;
    }

    public static Register asRegister(Value value) {
        return asRegisterValue(value).getRegister();
    }

    public static RegisterValue asRegisterValue(Value value) {
        assert value != null;
        return (RegisterValue) value;
    }

    public static Register asRegister(Value value, PlatformKind kind) {
        if (value.getPlatformKind() != kind) {
            throw new InternalError("needed: " + kind + " got: " + value.getPlatformKind());
        } else {
            return asRegister(value);
        }
    }
}
