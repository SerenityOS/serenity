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
 * Annotate all methods in your test class which the framework should test with {@code @Test}.
 * <p>
 * Let {@code m} be a test method specifying the {@code @Test} annotation. If {@code m} is neither part of a
 * <b>checked test</b> (an additional method specifying {@link Check @Check} with {@code @Check(test = "m")}) nor part
 * of a <b>custom run test</b> (an additional method specifying {@link Run @Run} with {@code @Run(test = "m")}),
 * then {@code m} is a so-called <b>base test</b> and the the framework invokes {@code m} in the following way:
 * <ol>
 *     <li><p>The framework warms {@code m} up by invoking it for a predefined number of iterations (default: 2000)
 *     or any number specified by an additional {@link Warmup @Warmup} annotation at {@code m} or by using
 *     {@link TestFramework#setDefaultWarmup(int)} (could also be 0 which skips the warm-up completely which is similar
 *     to simulating {@code -Xcomp}). More information about the warm-up in general can be found at {@link Warmup}</li>
 *     <li><p>After the warm-up, the framework compiles {@code m} at the specified compilation level set by
 *     {@link #compLevel()} (default {@link CompLevel#ANY} will pick the highest available level which is usually
 *     {@link CompLevel#C2}).</li>
 *     <li><p>The framework invokes {@code m} one more time to run the compilation.</li>
 *     <li><p>The framework checks any specified {@link IR @IR} constraints at {@code m}. More information about IR matching
 *     can be found at {@link IR}.</li>
 * </ol>
 *
 * <p>
 * {@code m} has the following properties:
 * <ul>
 *     <li><p>If {@code m} specifies no parameters, the framework can directly invoke {@code m}.</li>
 *     <li><p>If {@code m} specifies parameters, the framework needs to know how to invoke {@code m}. Use {@link Arguments}
 *     with {@link Argument} properties for each parameter to use well-defined parameters by the framework. If the method
 *     requires a more specific argument value, use a custom run test (see {@link Run}).</li>
 *     <li><p>{@code m} cannot specify {@link AbstractInfo} or any of its subclasses as parameter or return type.</li>
 *     <li><p>{@code m} is not inlined by the framework.</li>
 *     <li><p>Verification of the return value of {@code m} can only be done in a checked test (see {@link Check}) or
 *     custom run test (see {@link Run}).</li>
 * </ul>
 *
 * <p>
 * The following constraints must be met for the test method {@code m} specifying {@code @Test}:
 * <ul>
 *     <li><p>{@code m} must be part of the test class. Using {@code @Test} in nested or helper classes is not allowed.</li>
 *     <li><p>{@code m} cannot have the same name as another {@code @Test} method in the same test class. Method
 *     overloading is only allowed (but not encouraged) with other non-{@code @Test} methods.</li>
 *     <li><p>{@code m} cannot specify any helper-method-specific compile command annotations
 *            ({@link ForceCompile @ForceCompile}, {@link DontCompile @DontCompile}, {@link ForceInline @ForceInline},
 *            {@link DontInline @DontInline}). </li>
 * </ul>
 *
 * <p>
 * Examples on how to write base tests can be found in {@link ir_framework.examples.BaseTestExample}
 * and also as part of the internal testing in the package {@link ir_framework.tests}.
 *
 * @see Arguments
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Test {
    /**
     * Specify at which compilation level the framework should eventually compile the test method after an optional
     * warm-up period. The default {@link CompLevel#ANY} will let the framework compile the method at the highest
     * available level which is usually {@link CompLevel#C2}.
     */
    CompLevel compLevel() default CompLevel.ANY;
}
