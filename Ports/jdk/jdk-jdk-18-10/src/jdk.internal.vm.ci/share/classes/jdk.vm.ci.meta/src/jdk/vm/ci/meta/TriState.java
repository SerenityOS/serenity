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
package jdk.vm.ci.meta;

/**
 * Represents a logic value that can be either {@link #TRUE}, {@link #FALSE}, or {@link #UNKNOWN}.
 */
public enum TriState {
    TRUE,
    FALSE,
    UNKNOWN;

    public static TriState get(boolean value) {
        return value ? TRUE : FALSE;
    }

    /**
     * This is optimistic about {@link #UNKNOWN} (it prefers known values over {@link #UNKNOWN}) and
     * pesimistic about known (it perfers {@link #TRUE} over {@link #FALSE}).
     */
    public static TriState merge(TriState a, TriState b) {
        if (a == TRUE || b == TRUE) {
            return TRUE;
        }
        if (a == FALSE || b == FALSE) {
            return FALSE;
        }
        assert a == UNKNOWN && b == UNKNOWN;
        return UNKNOWN;
    }

    public boolean isTrue() {
        return this == TRUE;
    }

    public boolean isFalse() {
        return this == FALSE;
    }

    public boolean isUnknown() {
        return this == UNKNOWN;
    }

    public boolean isKnown() {
        return this != UNKNOWN;
    }

    public boolean toBoolean() {
        if (isTrue()) {
            return true;
        } else if (isFalse()) {
            return false;
        } else {
            throw new IllegalStateException("Cannot convert to boolean, TriState is in an unknown state");
        }
    }
}
