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

import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Represents a request to compile a method.
 */
public class CompilationRequest {

    private final ResolvedJavaMethod method;

    private final int entryBCI;

    /**
     * Creates a request to compile a method starting at its entry point.
     *
     * @param method the method to be compiled
     */
    public CompilationRequest(ResolvedJavaMethod method) {
        this(method, -1);
    }

    /**
     * Creates a request to compile a method starting at a given BCI.
     *
     * @param method the method to be compiled
     * @param entryBCI the bytecode index (BCI) at which to start compiling where -1 denotes the
     *            method's entry point
     */
    public CompilationRequest(ResolvedJavaMethod method, int entryBCI) {
        assert method != null;
        this.method = method;
        this.entryBCI = entryBCI;
    }

    /**
     * Gets the method to be compiled.
     */
    public ResolvedJavaMethod getMethod() {
        return method;
    }

    /**
     * Gets the bytecode index (BCI) at which to start compiling where -1 denotes a non-OSR
     * compilation request and all other values denote an on stack replacement (OSR) compilation
     * request.
     */
    public int getEntryBCI() {
        return entryBCI;
    }

    @Override
    public String toString() {
        return method.format("%H.%n(%p)@" + entryBCI);
    }
}
