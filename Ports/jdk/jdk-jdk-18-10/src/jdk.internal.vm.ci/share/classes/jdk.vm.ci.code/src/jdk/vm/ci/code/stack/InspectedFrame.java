/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.code.stack;

import jdk.vm.ci.meta.ResolvedJavaMethod;

/**
 * Access to the object variables in a stack frame.
 */
public interface InspectedFrame {

    /**
     * Returns the value of the object local at {@code index}. This value is a copy iff
     * {@link #isVirtual(int)} is true.
     */
    Object getLocal(int index);

    /**
     * Returns whether the local at {@code index} is a virtual object, and therefore the object
     * returned by {@link #getLocal(int)} is a copy.
     */
    boolean isVirtual(int index);

    /**
     * Returns true if the stack frame is a compiled stack frame and there are virtual objects
     * anywhere in the current state of the compiled method. This can return true even if
     * {@link #isVirtual(int)} return false for all locals.
     */
    boolean hasVirtualObjects();

    /**
     * This method will materialize all virtual objects, deoptimize the stack frame and make sure
     * that subsequent execution of the deoptimized frame uses the materialized values.
     */
    void materializeVirtualObjects(boolean invalidateCode);

    /**
     * @return the current bytecode index
     */
    int getBytecodeIndex();

    /**
     * @return the current method
     */
    ResolvedJavaMethod getMethod();

    /**
     * Checks if the current method is equal to the given method. This is semantically equivalent to
     * {@code method.equals(getMethod())}, but can be implemented more efficiently.
     */
    boolean isMethod(ResolvedJavaMethod method);
}
