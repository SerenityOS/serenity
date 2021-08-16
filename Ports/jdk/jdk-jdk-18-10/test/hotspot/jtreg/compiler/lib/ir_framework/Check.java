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
 * Annotation for a check method of a <b>checked test</b>.
 *
 * <p>
 * Let {@code t} be a test method specifying the {@link Test @Test} annotation and {@code c} be a check method specifying
 * the {@code @Check(test = "t")} annotation. These two methods represent a so-called <i>checked test</i>. The only
 * difference to a <i>base test</i> (see {@link Test}) is that the framework will invoke the check method {@code c}
 * directly after the invocation of the test method {@code t} which allows to do some additional verification,
 * including the return value of {@code t}. The framework does the following, similar as for <i>base tests</i>:
 * <ol>
 *     <li><p>The framework warms {@code t} up by invoking it for a predefined number of iterations (default: 2000)
 *            or any number specified by an additional {@link Warmup @Warmup} annotation at {@code t} or by using
 *            {@link TestFramework#setDefaultWarmup(int)} (could also be 0 which skips the warm-up completely which is
 *            similar to simulating {@code -Xcomp}). After each invocation of {@code t}, the framework also invokes
 *            {@code c} if the {@code @Check} annotation specifies {@link CheckAt#EACH_INVOCATION} at {@link #when()}.
 *            More information about the warm-up in general can be found at {@link Warmup}</li>
 *     <li><p>After the warm-up, the framework compiles {@code t} at the specified compilation level set by
 *            {@link Test#compLevel()} (default {@link CompLevel#ANY} will pick the highest available level which is
 *            usually {@link CompLevel#C2}).</li>
 *     <li><p>The framework invokes {@code t} one more time to run the compilation. Afterwards, the framework will
 *            always invoke {@code c} again to be able perform additional checks after the compilation of {@code t}.</li>
 *     <li><p>The framework checks any specified {@link IR @IR} constraints at the test method {@code t}.
 *            More information about IR matching can be found at {@link IR}.</li>
 * </ol>
 *
 * <p>
 * The test method {@code t} has the same properties and follows the same constraints as stated in {@link Test}.
 * <p>
 * The following additional constraints must be met for the test method {@code t} and check method {@code c}:
 * <ul>
 *     <li><p>{@code c} must specify the method name {@code t} as property in {@code @Check(test = "t")}
 *     (see {@link #test()}. Specifying a non-{@code @Test} annotated method or a {@code @Test} method that
 *     has already been used by another {@code @Check} or {@link Run @Run} method results in a {@link TestFormatException}.
 *     <li><p>{@code c} can specify the following method parameter combinations:
 *     <ul>
 *         <li><p>void</li>
 *         <li><p>One parameter: {@link TestInfo} which provides some information about {@code t} and utility methods.</li>
 *         <li><p>One parameter: the <i>exact</i> same type as the return value of {@code t}. When {@code c} is
 *                invoked by the framework, this parameter contains the return value of {@code t}.</li>
 *         <li><p>1st parameter: {@link TestInfo}; 2nd parameter: the <i>exact</i> same type as the return value of
 *                {@code t} (see above)</li>
 *         <li><p> Any other combination will result in a {@link TestFormatException}.
 *     </ul>
 *     <li><p>{@code c} is not compiled nor inlined.
 *     <li><p>{@code c} must be part of the test class. Using {@code @Check} in nested or other classes is not allowed.</li>
 *     <li><p>{@code c} cannot specify any helper-method-specific compile command annotations
 *            ({@link ForceCompile @ForceCompile}, {@link DontCompile @DontCompile}, {@link ForceInline @ForceInline},
 *            {@link DontInline @DontInline}).</li>
 * </ul>
 *
 * <p>
 * If no verification is required, use a <i>base test</i> (see {@link Test}). If {@code t} must be invoked with more
 * complex or varying arguments and/or the {@code t} must be invoked differently in subsequent invocations, use a
 * <i>custom run test</i> (see {@link Run}).
 *
 * <p>
 * Examples on how to write checked tests can be found in {@link jdk.test.lib.hotspot.ir_framework.examples.CheckedTestExample}
 * and also as part of the internal testing in the package {@link jdk.test.lib.hotspot.ir_framework.tests}.
 *
 * @see Test
 * @see TestInfo
 * @see CheckAt
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Check {
    /**
     * The unique associated {@link Test} method for this {@code @Check} annotated check method. The framework will directly
     * invoke the {@code @Check} method after each invocation or only after the compilation of the associated {@code @Test}
     * method (depending on the value set with {@link #when()}).
     * <p>
     * If a non-{@code @Test} annotated method or a {@code @Test} method that has already been used by another
     * {@code @Check} or {@link Run} method is specified, then a {@link TestFormatException} is thrown.
     *
     * @see Test
     */
    String test();
    /**
     * When should the {@code @Check} method be invoked? By default, the check is done after each invocation which is
     * encouraged if performance is not critical.
     *
     * @see CheckAt
     */
    CheckAt when() default CheckAt.EACH_INVOCATION;
}
