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

import compiler.lib.ir_framework.driver.IRViolationException;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is used to define a constraint/rule/check on the resulting IR of a test method (method with
 * {@link Test @Test} annotation). A test method can define multiple {@code @IR} rules.
 * <p>
 * There are two kinds of checks that can be specified:
 * <ul>
 *     <li><p>{@link #failOn()}: Specify a list of (node) regexes that should not be matched on the {@code PrintIdeal} or
 *            {@code PrintOptoAssembly} output.</li>
 *     <li><p>{@link #counts()}: Specify a list of ({@code regex,count}) pairs: The (node) {@code regex} should be matched
 *            for the specified amount in {@code count} on the {@code PrintIdeal} or {@code PrintOptoAssembly} output.</li>
 * </ul>
 * An IR rule must specify either or both of these two checks. If one or both of the checks fails, an
 * {@link IRViolationException} is thrown. A user can provide a custom regex string or specify any of the default node
 * regexes defined in {@link IRNode}.
 * <p>
 * Sometimes, the shape of the resulting IR is changed by commonly used VM flags in such a way that an IR rule no longer
 * applies. Generally, the framework does <b>not</b> apply any IR rules when any of the following flags are used:
 * {@code -Xint, -XX:-UseCompiler, -XX:TieredStopAtLevel={1,2,3}, -DExcludeRandom=true, -DFlipC1C2=true}.
 * Furthermore, a JTreg test could be run with additional VM and Javaoptions flags. The IR verification is <b>not</b>
 * performed in this case if any of these JTreg flags is used that is not part of the whitelist specified by
 * {@link TestFramework#JTREG_WHITELIST_FLAGS}.
 * <p>
 * For any other flag specified either by user code (e.g. {@link Scenario#Scenario(int, String...)},
 * {@link TestFramework#runWithFlags(String...) etc.} or as part of the JTreg whitelist, IR verification is applied.
 * To restrict the application of IR rules when certain flags are present that could change the IR, each {@code @IR}
 * annotation can specify additional preconditions on the allowed test VM flags that must hold when an IR rule is applied.
 * If the specified preconditions fail, then the framework does not apply the IR rule. These preconditions can be
 * set with {@link #applyIf()}, {@link #applyIfNot()}, {@link #applyIfAnd()}, or {@link #applyIfOr()}.
 * <p>
 * Examples on how to write tests with IR rules can be found in {@link jdk.test.lib.hotspot.ir_framework.examples.IRExample}
 * and also as part of the internal testing in {@link jdk.test.lib.hotspot.ir_framework.tests.TestIRMatching}.
 *
 * @see Test
 * @see IRNode
 */
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(IRs.class)
public @interface IR {
    /**
     * Define a list of (node) regexes. If any of these regexes are matched on the PrintIdeal or PrintOptoAssembly, the
     * IR rule fails and an {@link IRViolationException} is thrown.
     */
    String[] failOn() default {};

    /**
     * Define a list of ((node) regexes,count) string pairs: A regex to be matched on the PrintIdeal or PrintOptoAssembly
     * is immediately followed by a number specifying how often the regex should be matched. The number can be proceeded
     * by comparators ({@code =, !=, <, <=, =>, >}) where the equality operator is optional (default if no comparator is
     * specified).
     * <p>
     * If any constraint on the number of regexes cannot be met, the IR rule fails and an
     * {@link IRViolationException} is thrown.
     */
    String[] counts() default {};

    /**
     * Define a single VM flag precondition which <i>must hold</i> when applying the IR rule. If the VM flag precondition
     * fails, then the IR rule is not applied. This is useful if a commonly used flag alters the IR in such a way that an IR rule
     * would fail.
     * <p>
     * The precondition is a (flag, value) string pair where the flag must be a valid VM flag and the value must conform
     * with the type of the VM flag. A number based flag value can be proceeded with an additional comparator
     * ({@code =, !=, <, <=, =>, >}) where the equality operator is optional (default if no comparator is specified).
     * <p>
     * This is the inverse of {@link #applyIfNot()}. For multiple preconditions, use {@link #applyIfAnd()} or
     * {@link #applyIfOr()} depending on the use case.
     */
    String[] applyIf() default {};

    /**
     * Define a single VM flag precondition which <i>must <b>not</b> hold</i> when applying the IR rule. If, however,
     * the VM flag precondition holds, then the IR rule is not applied. This could also be defined as <i>negative</i>
     * precondition. This is useful if a commonly used flag alters the IR in such a way that an IR rule would fail.
     * <p>
     * The precondition is a (flag, value) string pair where the flag must be a valid VM flag and the value must conform
     * with the type of the VM flag. A number based flag value can be proceeded with an additional comparator
     * ({@code =, !=, <, <=, =>, >}) where the equality operator is optional (default if no comparator is specified).
     * <p>
     * This is the inverse of {@link #applyIf()}. For multiple preconditions, use {@link #applyIfAnd()} or
     * {@link #applyIfOr()} depending on the use case.
     */
    String[] applyIfNot() default {};

    /**
     * Define a list of at least two VM flag precondition which <i><b>all</b> must hold</i> when applying the IR rule.
     * If the one of the VM flag preconditions does not hold, then the IR rule is not applied. This is useful if
     * commonly used flags alter the IR in such a way that an IR rule would fail. This can also be defined as conjunction
     * of preconditions.
     * <p>
     * A precondition is a (flag, value) string pair where the flag must be a valid VM flag and the value must conform
     * with the type of the VM flag. A number based flag value can be proceeded with an additional comparator
     * ({@code =, !=, <, <=, =>, >}) where the equality operator is optional (default if no comparator is specified).
     * <p>
     * Use  {@link #applyIfOr()} for disjunction and for single precondition constraints use {@link #applyIf()} or
     * {@link #applyIfNot()} depending on the use case.
     */
    String[] applyIfAnd() default {};

    /**
     * Define a list of at least two VM flag precondition from which <i><b>at least one</b> must hold</i> when applying
     * the IR rule. If none of the VM flag preconditions holds, then the IR rule is not applied. This is useful if
     * commonly used flags alter the IR in such a way that an IR rule would fail. This can also be defined as disjunction
     * of preconditions.
     * <p>
     * A precondition is a (flag, value) string pair where the flag must be a valid VM flag and the value must conform
     * with the type of the VM flag. A number based flag value can be proceeded with an additional comparator
     * ({@code =, !=, <, <=, =>, >}) where the equality operator is optional (default if no comparator is specified).
     * <p>
     * Use  {@link #applyIfAnd()} for conjunction and for single precondition constraints use {@link #applyIf()} or
     * {@link #applyIfNot()} depending on the use case.
     */
    String[] applyIfOr() default {};
}
