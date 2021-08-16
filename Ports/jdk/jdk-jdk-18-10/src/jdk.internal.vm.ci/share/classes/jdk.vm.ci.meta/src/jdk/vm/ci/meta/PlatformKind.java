/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.meta;

/**
 * Represents a platform-specific low-level type for values.
 */
public interface PlatformKind {

    String name();

    public interface Key {

    }

    class EnumKey<E extends Enum<E>> implements Key {
        private final Enum<E> e;

        public EnumKey(Enum<E> e) {
            this.e = e;
        }

        @Override
        public int hashCode() {
            return e.ordinal();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof EnumKey) {
                EnumKey<?> that = (EnumKey<?>) obj;
                return this.e == that.e;
            }
            return false;
        }
    }

    /**
     * Gets a value associated with this object that can be used as a stable key in a map. The
     * {@link Object#hashCode()} implementation of the returned value should be stable between VM
     * executions.
     */
    Key getKey();

    /**
     * Get the size in bytes of this {@link PlatformKind}.
     */
    int getSizeInBytes();

    /**
     * Returns how many primitive values fit in this {@link PlatformKind}. For scalar types this is
     * one, for SIMD types it may be higher.
     */
    int getVectorLength();

    /**
     * Gets a single type char that identifies this type for use in debug output.
     */
    char getTypeChar();
}
