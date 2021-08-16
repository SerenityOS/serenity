/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * Specifies the action that should be taken by the runtime in case a certain deoptimization is
 * triggered.
 */
public enum DeoptimizationAction {
    /**
     * Do not invalidate the machine code. This is typically used when deoptimizing at a point where
     * it's highly likely nothing will change the likelihood of the deoptimization happening again.
     * For example, a compiled array allocation where the size is negative.
     */
    None(false),

    /**
     * Do not invalidate the machine code, but schedule a recompilation if this deoptimization is
     * triggered too often.
     */
    RecompileIfTooManyDeopts(true),

    /**
     * Invalidate the machine code and reset the profiling information.
     */
    InvalidateReprofile(true),

    /**
     * Invalidate the machine code and immediately schedule a recompilation. This is typically used
     * when deoptimizing to resolve an unresolved symbol in which case extra profiling is not
     * required to determine that the deoptimization will not re-occur.
     */
    InvalidateRecompile(true),

    /**
     * Invalidate the machine code and stop compiling the outermost method of this compilation.
     */
    InvalidateStopCompiling(true);

    private final boolean invalidatesCompilation;

    DeoptimizationAction(boolean invalidatesCompilation) {
        this.invalidatesCompilation = invalidatesCompilation;
    }

    public boolean doesInvalidateCompilation() {
        return invalidatesCompilation;
    }

}
