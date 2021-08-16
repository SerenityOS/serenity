/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * Represents the type of {@link Value values}. This class can be extended by compilers to track
 * additional information about values.
 */
public abstract class ValueKind<K extends ValueKind<K>> {

    private enum IllegalKind implements PlatformKind {
        ILLEGAL;

        private final EnumKey<IllegalKind> key = new EnumKey<>(this);

        @Override
        public Key getKey() {
            return key;
        }

        @Override
        public int getSizeInBytes() {
            return 0;
        }

        @Override
        public int getVectorLength() {
            return 0;
        }

        @Override
        public char getTypeChar() {
            return '-';
        }
    }

    private static class IllegalValueKind extends ValueKind<IllegalValueKind> {

        IllegalValueKind() {
            super(IllegalKind.ILLEGAL);
        }

        @Override
        public IllegalValueKind changeType(PlatformKind newPlatformKind) {
            return this;
        }

        @Override
        public String toString() {
            return "ILLEGAL";
        }
    }

    /**
     * The non-type.
     */
    public static final ValueKind<?> Illegal = new IllegalValueKind();

    private final PlatformKind platformKind;

    public ValueKind(PlatformKind platformKind) {
        this.platformKind = platformKind;
    }

    public final PlatformKind getPlatformKind() {
        return platformKind;
    }

    /**
     * Create a new {@link ValueKind} with a different {@link PlatformKind}. Subclasses must
     * override this to preserve the additional information added by the compiler.
     */
    public abstract K changeType(PlatformKind newPlatformKind);

    /**
     * Returns a String representation of the kind, which will be included at the end of
     * {@link Value#toString()} implementation. Defaults to {@link #toString()} but can be
     * overridden to provide something more specific.
     */
    public String getKindSuffix() {
        return toString();
    }
}
