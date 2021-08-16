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

import compiler.lib.ir_framework.shared.TestFormatException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Prevent a compilation of the annotated <b>helper method</b> (not specifying {@link Test @Test},
 * {@link Check @Check} or {@link Run @Run}) with the specified compiler.
 *
 * <ul>
 *     <li><p>{@link Compiler#ANY} (default): No C1 or C2 compilation.</li>
 *     <li><p>{@link Compiler#C1}: No C1 compilation, C2 compilation still possible.</li>
 *     <li><p>{@link Compiler#C2}: No C2 compilation, C1 compilation still possible.</li>
 * </ul>
 * <p>
 * Using this annotation on <i>non-helper methods</i> results in a {@link TestFormatException TestFormatException}.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DontCompile {
    /**
     * The compiler with which a compilation of a helper method is excluded.
     */
    Compiler value() default Compiler.ANY;
}
