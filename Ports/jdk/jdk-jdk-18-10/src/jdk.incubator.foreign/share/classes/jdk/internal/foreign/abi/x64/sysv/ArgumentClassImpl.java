/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.foreign.abi.x64.sysv;

public enum ArgumentClassImpl {
    POINTER, INTEGER, SSE, SSEUP, X87, X87UP, COMPLEX_X87, NO_CLASS, MEMORY;

    public ArgumentClassImpl merge(ArgumentClassImpl other) {
        if (this == other) {
            return this;
        }

        if (other == NO_CLASS) {
            return this;
        }
        if (this == NO_CLASS) {
            return other;
        }

        if (this == MEMORY || other == MEMORY) {
            return MEMORY;
        }

        if (this == POINTER || other == POINTER) {
            return POINTER;
        }

        if (this == INTEGER || other == INTEGER) {
            return INTEGER;
        }

        if (this == X87 || this == X87UP || this == COMPLEX_X87) {
            return MEMORY;
        }
        if (other == X87 || other == X87UP || other == COMPLEX_X87) {
            return MEMORY;
        }

        return SSE;
    }

    public boolean isIntegral() {
        return this == INTEGER || this == POINTER;
    }

    public boolean isPointer() {
        return this == POINTER;
    }

    public boolean isIndirect() {
        return this == MEMORY;
    }
}
