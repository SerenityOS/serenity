/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Objects;

import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Represents a code position, that is, a chain of inlined methods with bytecode locations, that is
 * communicated from the compiler to the runtime system. A code position can be used by the runtime
 * system to reconstruct a source-level stack trace for exceptions and to create
 * {@linkplain BytecodeFrame frames} for deoptimization.
 */
public class BytecodePosition {

    private final BytecodePosition caller;
    private final ResolvedJavaMethod method;
    private final int bci;

    /**
     * Constructs a new object representing a given parent/caller, a given method, and a given BCI.
     *
     * @param caller the parent position
     * @param method the method
     * @param bci a BCI such that {@code method.codeSize() == 0 || bci < method.getCodeSize()}. That
     *            is, if code size is 0 then allow any value, otherwise the bci must be less than
     *            the code size.
     */
    public BytecodePosition(BytecodePosition caller, ResolvedJavaMethod method, int bci) {
        assert method != null;
        this.caller = caller;
        this.method = method;
        this.bci = bci;
        int codeSize = method.getCodeSize();
        if (codeSize != 0 && bci >= codeSize) {
            throw new IllegalArgumentException(String.format("bci %d is out of range for %s %d bytes", bci, method.format("%H.%n(%p)"), codeSize));
        }
    }

    /**
     * Converts this code position to a string representation.
     *
     * @return a string representation of this code position
     */
    @Override
    public String toString() {
        return CodeUtil.append(new StringBuilder(100), this).toString();
    }

    /**
     * Deep equality test.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            BytecodePosition that = (BytecodePosition) obj;
            if (this.bci == that.bci && Objects.equals(this.getMethod(), that.getMethod()) && Objects.equals(this.caller, that.caller)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hc = method.hashCode() * 31 + bci;
        if (caller != null) {
            hc = (hc * 31) + caller.hashCode();
        }
        return hc;
    }

    /**
     * @return The location within the method, as a bytecode index. The constant {@code -1} may be
     *         used to indicate the location is unknown, for example within code synthesized by the
     *         compiler.
     */
    public int getBCI() {
        return bci;
    }

    /**
     * @return The runtime interface method for this position.
     */
    public ResolvedJavaMethod getMethod() {
        return method;
    }

    /**
     * The position where this position has been called, {@code null} if none.
     */
    public BytecodePosition getCaller() {
        return caller;
    }

    /**
     * Adds a caller to the current position returning the new position.
     */
    public BytecodePosition addCaller(BytecodePosition link) {
        if (getCaller() == null) {
            return new BytecodePosition(link, getMethod(), getBCI());
        } else {
            return new BytecodePosition(getCaller().addCaller(link), getMethod(), getBCI());
        }
    }
}
