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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation overrides the default number (2000) of times the framework should warm up a test.
 * <ul>
 *     <li><p>Any positive value or zero is permitted. A warm-up of zero allows a simulation of {@code -Xcomp}.</li>
 *     <li><p>Custom run tests (see {@link Run}) must specify a {@code @Warmup} annotation at the run method.</li>
 *     <li><p>Base and checked tests (see {@link Test}, {@link Check}) must specify a {@code @Warmup} annotation at
 *            the test method.</li>
 * </ul>
 *
 * @see Test
 * @see Check
 * @see Run
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Warmup {
    /**
     * The warm-up iterations for the test.
     */
    int value();
}
