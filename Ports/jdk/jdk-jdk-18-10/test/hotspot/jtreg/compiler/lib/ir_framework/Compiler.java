/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package compiler.lib.ir_framework;

/**
 * Compilers to select for {@link DontCompile}. HotSpot does not handle the exclusion of a C1 method at a specific level.
 * It can only exclude a method for the entire C1 compilation. Thus, this annotation is provided for {@link DontCompile}
 * instead of {@link CompLevel}.
 *
 * @see DontCompile
 */
public enum Compiler {
    /**
     * Selecting both the C1 and C2 compiler. This must be in sync with hotspot/share/compiler/compilerDefinitions.hpp.
     */
    ANY(-1),
    /**
     * The C1 compiler.
     */
    C1(1),
    /**
     * The C2 compiler.
     */
    C2(4),

    ;

    private final int value;

    Compiler(int level) {
        this.value = level;
    }

    /**
     * Get the compilation level as integer value. These will match the levels specified in HotSpot (if available).
     *
     * @return the compilation level as integer.
     */
    public int getValue() {
        return value;
    }
}
