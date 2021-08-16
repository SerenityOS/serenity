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
package jdk.vm.ci.hotspot;

import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.runtime.JVMCICompilerFactory;

/**
 * HotSpot extensions to {@link JVMCICompilerFactory}.
 */
public abstract class HotSpotJVMCICompilerFactory implements JVMCICompilerFactory {

    public enum CompilationLevelAdjustment {
        /**
         * No adjustment.
         */
        None,

        /**
         * Adjust based on declaring class of method.
         */
        ByHolder,

        /**
         * Adjust based on declaring class, name and signature of method.
         */
        ByFullSignature
    }

    /**
     * Determines if this object may want to adjust the compilation level for a method that is being
     * scheduled by the VM for compilation.
     */
    public CompilationLevelAdjustment getCompilationLevelAdjustment() {
        return CompilationLevelAdjustment.None;
    }

    public enum CompilationLevel {
        None,
        Simple,
        LimitedProfile,
        FullProfile,
        FullOptimization
    }

    /**
     * Potentially modifies the compilation level currently selected by the VM compilation policy
     * for a method.
     *
     * @param declaringClass the class in which the method is declared. This value is either a
     *            {@code Class} instance or a {@code String} representing the
     *            {@link JavaType#toJavaName() name} of the class.
     * @param name the name of the method or {@code null} depending on the value that was returned
     *            by {@link #getCompilationLevelAdjustment()}
     * @param signature the signature of the method or {@code null} depending on the value that was
     *            returned by {@link #getCompilationLevelAdjustment()}
     * @param isOsr specifies if the compilation being scheduled in an OSR compilation
     * @param level the compilation level currently selected by the VM compilation policy
     * @return the compilation level to use for the compilation being scheduled (must be a valid
     *         {@code CompLevel} enum value)
     */
    public CompilationLevel adjustCompilationLevel(Object declaringClass, String name, String signature, boolean isOsr, CompilationLevel level) {
        throw new InternalError(getClass().getName() + " must override adjustCompilationLevel(...) since it returned a value other than " + CompilationLevel.class.getName() + "." +
                        CompilationLevel.None + " from getCompilationLevelAdjustment()");
    }
}
