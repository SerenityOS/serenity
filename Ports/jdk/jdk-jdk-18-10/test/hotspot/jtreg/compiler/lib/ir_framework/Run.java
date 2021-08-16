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
 * Annotation for a run method of a <b>custom run test</b>.
 *
 * <p>
 * Let {@code t} be a test method specifying the {@link Test @Test} annotation and {@code r} be a run method specifying
 * the {@code @Run(test = "t")} annotation. These two methods represent a so-called <i>custom run test</i>. The only
 * difference to a <i>base test</i> (see {@link Test}) is that the framework will not invoke the test method {@code t}
 * but instead the run method {@code r} which is then responsible to invoke {@code t} in any way and optionally do any
 * additional verification (e.g. of the return value). If {@code r} does not specify {@link RunMode#STANDALONE} as
 * {@link #mode()} property, the framework does the following, similar as for <i>base tests</i>:
 * <ol>
 *     <li><p>The framework warms {@code r} up by invoking it for a predefined number of iterations (default: 2000)
 *            or any number specified by an additional {@link Warmup} annotation at the run method {@code r} or by using
 *            {@link TestFramework#setDefaultWarmup(int)} (could also be 0 which skips the warm-up completely which is
 *            similar to simulating {@code -Xcomp}). More information about the warm-up in general can be found in
 *            {@link Warmup @Warmup}.</li>
 *     <li><p>After the warm-up, the framework compiles the test method {@code t} at the specified compilation level set by
 *            {@link Test#compLevel()} (default {@link CompLevel#ANY} will pick the highest available level which is usually
 *            {@link CompLevel#C2}).</li>
 *     <li><p>The framework invokes the run method {@code r} one more time to check the compilation.</li>
 *     <li><p>The framework checks any specified {@link IR @IR} constraints at the test method {@code t}.
 *            More information about IR matching can be found at {@link IR}.</li>
 * </ol>
 *
 * <p>
 *  If {@code r} specifies {@link RunMode#STANDALONE} as {@link #mode()} property, the framework gives complete
 *  control to the run method {@code r}:
 * <ol>
 *     <li><p>The framework invokes the run method {@code r} only one time without any warm-up or compilation of
 *            {@code t} ({@link Warmup @Warmup} is not allowed at {@code r} in this case).</li>
 *     <li><p>After this single invocation, the framework directly checks any specified {@link IR} constraints at the test
 *            method {@code t}. The run method {@code r} needs to make sure to reliably trigger a C2 compilation. Otherwise,
 *            IR matching will fail. More information about IR matching can be found at {@link IR}.</li>
 * </ol>
 *
 * <p>
 * The test method {@code t} and run method {@code r} have the following properties:
 * <ul>
 *     <li><p>{@code t} can specify any parameter or return type except {@link AbstractInfo} or any of its subclasses.</li>
 *     <li><p>{@code t} is not inlined.
 *     <li><p>{@code r} is not compiled nor inlined.
 *     <li><p>{@code r} is responsible to invoke {@code t} in any way (once, multiple times or even skipping on some
 *                      invocations of {@code r}).
 *     <li><p>{@code r} can specify the following method parameter combinations:
 *     <ul>
 *         <li><p>void</li>
 *         <li><p>One parameter: {@link RunInfo} which provides some information about {@code t} and utility methods.</li>
 *         <li><p>Any other combination will result in a {@link TestFormatException}.
 *     </ul>
 *     <li><p>{@code t} and {@code r} must be part of the test class. Using {@code @Run} and {@code @Test} in nested or
 *             other helper classes is not allowed.</li>
 *     <li><p>{@code t} and {@code r} cannot specify any helper-method-specific compile command annotations
 *            ({@link ForceCompile @ForceCompile}, {@link DontCompile @DontCompile}, {@link ForceInline @ForceInline},
 *            {@link DontInline @DontInline}).</li>
 * </ul>
 *
 * <p>
 * Examples on how to write custom run tests can be found in {@link jdk.test.lib.hotspot.ir_framework.examples.CustomRunTestExample}
 * and also as part of the internal testing in the package {@link jdk.test.lib.hotspot.ir_framework.tests}.
 *
 * @see Test
 * @see RunInfo
 * @see RunMode
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Run {
    /**
     * The associated {@link Test @Test} methods (one or more) for this {@code @Run} annotated run method.
     * The framework directly invokes the run method instead of the associated {@code @Test} methods.
     */
    String[] test();

    /**
     * The mode of this custom run test.
     *
     * @see RunMode
     */
    RunMode mode() default RunMode.NORMAL;
}
