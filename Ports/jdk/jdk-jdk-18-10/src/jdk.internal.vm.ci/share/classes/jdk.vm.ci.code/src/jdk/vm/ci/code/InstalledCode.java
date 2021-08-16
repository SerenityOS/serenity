/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * Represents a compiled instance of a method. It may have been invalidated or removed in the
 * meantime.
 */
public class InstalledCode {

    /**
     * Raw address address of entity representing this installed code.
     */
    protected long address;

    /**
     * Raw address of entryPoint of this installed code.
     */
    protected long entryPoint;

    /**
     * Counts how often the address field was reassigned.
     */
    protected long version;

    protected final String name;

    public InstalledCode(String name) {
        this.name = name;
    }

    /**
     * @return the address of entity representing this installed code.
     */
    public long getAddress() {
        return address;
    }

    /**
     * @return the address of the normal entry point of the installed code.
     */
    public long getEntryPoint() {
        return entryPoint;
    }

    /**
     * @return the version number of this installed code
     */
    public final long getVersion() {
        return version;
    }

    /**
     * Returns the name of this installed code.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the start address of this installed code if it is {@linkplain #isValid() valid}, 0
     * otherwise.
     */
    public long getStart() {
        return 0;
    }

    /**
     * @return true if the code represented by this object is still valid for invocation, false
     *         otherwise (may happen due to deopt, etc.)
     */
    public boolean isValid() {
        return entryPoint != 0;
    }

    /**
     * @return true if the code represented by this object still exists and might have live
     *         activations, false otherwise (may happen due to deopt, etc.)
     */
    public boolean isAlive() {
        return address != 0;
    }

    /**
     * Returns a copy of this installed code if it is {@linkplain #isValid() valid}, null otherwise.
     */
    public byte[] getCode() {
        return null;
    }

    /**
     * Invalidates this installed code such that any subsequent
     * {@linkplain #executeVarargs(Object...) invocation} will throw an
     * {@link InvalidInstalledCodeException} and all existing invocations will be deoptimized.
     */
    public void invalidate() {
        throw new UnsupportedOperationException();
    }

    /**
     * Executes the installed code with a variable number of arguments.
     *
     * @param args the array of object arguments
     * @return the value returned by the executed code
     */
    @SuppressWarnings("unused")
    public Object executeVarargs(Object... args) throws InvalidInstalledCodeException {
        throw new UnsupportedOperationException();
    }
}
