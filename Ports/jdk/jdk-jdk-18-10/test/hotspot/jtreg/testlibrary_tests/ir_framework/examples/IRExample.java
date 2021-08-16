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

package ir_framework.examples;

import compiler.lib.ir_framework.*;
import compiler.lib.ir_framework.driver.IRViolationException;

/*
 * @test
 * @summary Example test to use the new test framework.
 * @library /test/lib /
 * @run driver ir_framework.examples.IRExample
 */

/**
 * Multiple @IR rules can be specified at @Test methods. The framework performs a regex based match on the PrintIdeal
 * and PrintOptoAssembly of the run test VM. Some default string regexes for IR nodes are defined in the framework
 * IRNode class. There are two kinds of checks:
 * <ul>
 *     <li><p>{@link IR#failOn}: One or more (IR node) regexes that are not allowed to occur in the IR (neither in
 *                               PrintIdeal nor in PrintOptoAssembly)</li>
 *     <li><p>{@link IR#counts}: One or more regexes-count pairs specifies how often an (IR node) regex must be found in
 *                               PrintIdeal and PrintOptoAssembly.</li>
 * </ul>
 * <p>
 *
 * One might also want to restrict the application of certain @IR rules depending on the used flags in the test VM.
 * These could be flags defined by the user or by JTreg. In the latter case, the flags must be whitelisted (see
 * {@link TestFramework}) most of them should not have an impact on the IR except for different GC flags which should
 * be considered) to enable a verification by the framework (see below). The @IR rules thus have an option to restrict
 * their application:
 * <ul>
 *     <li><p>{@link IR#applyIf}: Only apply a rule if a flag has a certain value</li>
 *     <li><p>{@link IR#applyIfNot}: Only apply a rule if a flag has NOT a certain value (inverse of applyIf)</li>
 *     <li><p>{@link IR#applyIfAnd}: Only apply a rule if all flags have the specified value</li>
 *     <li><p>{@link IR#applyIfOr}: Only apply a rule if at least one flag has the specified value</li>
 * </ul>
 * <p>
 *
 * The framework, however, does not perform the verification if:
 * <ul>
 *     <li><p>-DVerifyIR=false is used</li>
 *     <li><p>The test is run with a non-debug build</li>
 *     <li><p>-Xcomp, -Xint, -XX:-UseCompile, -XX:CompileThreshold, -DFlipC1C2=true, or -DExcludeRandom=true are used.</li>
 *     <li><p>JTreg specifies non-whitelisted flags as VM and/or Javaoptions (could change the IR in an unexpected way)</li>
 * </ul>
 *
 * @see IR
 * @see Test
 * @see TestFramework
 */
// This test is expected to fail when run with JTreg.
public class IRExample {
    int iFld, iFld2, iFld3;
    public static void main(String[] args) {
        TestFramework.run(); // First run tests from IRExample
        try {
            TestFramework.run(FailingExamples.class); // Secondly, run tests from FailingExamples
        } catch (IRViolationException e) {
            // Expected. Check stderr/stdout to see how IR failures are reported (always printed, regardless if
            // exception is thrown or not). Uncomment the "throw" statement below to get a completely failing test.
            //throw e;
        }
    }

    // Rules with failOn constraint which all pass
    @Test
    @IR(failOn = IRNode.LOAD) // 1 default regex
    @IR(failOn = {IRNode.LOAD, IRNode.LOOP}) // 2 default regexes
    @IR(failOn = {IRNode.LOAD, "some regex that does not occur"}) // 1 default regex and a user-defined regex
    // Rule with special configurable default regexes. All regexes with a "_OF" postfix in IR node expect a
    // second string specifying an additional required information.
    @IR(failOn = {IRNode.STORE_OF_FIELD, "iFld2", IRNode.LOAD, IRNode.STORE_OF_CLASS, "Foo"})
    // Only apply this rule if the VM flag UseZGC is true
    @IR(applyIf = {"UseZGC", "true"}, failOn = IRNode.LOAD)
    // We can also use comparators (<, <=, >, >=, !=, =) to restrict the rules.
    // This rule is only applied if the loop unroll limit is 10 or greater.
    @IR(applyIf = {"LoopUnrollLimit", ">= 10"}, failOn = IRNode.LOAD)
    public void goodFailOn() {
        iFld = 42; // No load, no loop, no store to iFld2, no store to class Foo
    }

    // Rules with counts constraint which all pass
    @Test
    @IR(counts = {IRNode.STORE, "2"}) // 1 default regex
    @IR(counts = {IRNode.LOAD, "0"}) // equivalent to failOn = IRNode.LOAD
    @IR(counts = {IRNode.STORE, "2",
                  IRNode.LOAD, "0"}) // 2 default regexes
    @IR(counts = {IRNode.STORE, "2",
                  "some regex that does not occur", "0"}) // 1 default regex and a user-defined regex
    // Rule with special configurable default regexes. All regexes with a "_OF" postfix in IR node expect a
    // second string specifying an additional required information.
    @IR(counts = {IRNode.STORE_OF_FIELD, "iFld", "1",
                  IRNode.STORE, "2",
                  IRNode.STORE_OF_CLASS, "IRExample", "2"})
    public void goodCounts() {
        iFld = 42; // No load, store to iFld in class IRExample
        iFld2 = 42; // No load, store to iFld2 in class IRExample
    }

    // @IR rules can also specify both type of checks in the same rule
    @Test
    @IR(failOn = {IRNode.ALLOC,
                  IRNode.LOOP},
        counts = {IRNode.LOAD, "2",
                  IRNode.LOAD_OF_FIELD, "iFld2", "1",
                  IRNode.LOAD_OF_CLASS, "IRExample", "2"})
    public void mixFailOnAndCounts() {
        iFld = iFld2;
        iFld2 = iFld3;
    }
}

class FailingExamples {
    int iFld2, iFld3;
    IRExample irExample = new IRExample();

    // Rules with failOn constraint which all fail.
    @Test
    @IR(failOn = IRNode.STORE)
    @IR(failOn = {IRNode.STORE, IRNode.LOOP}) // LOOP regex not found but STORE regex, letting the rule fail
    @IR(failOn = {IRNode.LOOP, IRNode.STORE}) // Order does not matter
    @IR(failOn = {IRNode.STORE, IRNode.LOAD}) // STORE and LOAD regex found, letting the rule fail
    @IR(failOn = {"LoadI"}) // LoadI can be found in PrintIdeal letting the rule fail
    // Store to iFld, store, and store to class IRExample, all 3 regexes found letting the rule fail
    @IR(failOn = {IRNode.STORE_OF_FIELD, "iFld", IRNode.STORE, IRNode.STORE_OF_CLASS, "IRExample"})
    public void badFailOn() {
        irExample.iFld = iFld2; // Store to iFld in class IRExample, load from iFld2
    }


    // Rules with counts constraint which all fail
    @Test
    @IR(counts = {IRNode.STORE, "1"}) // There are 2 stores
    @IR(counts = {IRNode.LOAD, "0"}) // equivalent to failOn = IRNode.LOAD, there is 1 load
    @IR(counts = {IRNode.STORE, "1",
                  IRNode.LOAD, "1"}) // first constraint holds (there is 1 load) but 2 stores, letting this rule fail
    @IR(counts = {IRNode.LOAD, "1",
                  IRNode.STORE, "1"}) // order does not matter
    @IR(counts = {"some regex that does not occur", "1"}) // user-defined regex does not occur once
    // Rule with special configurable default regexes. All regexes with a "_OF" postfix in IR node expect a
    // second string specifying an additional required information.
    @IR(counts = {IRNode.STORE_OF_FIELD, "iFld", "2", // Only one store to iFld
                  IRNode.LOAD, "2", // Only 1 load
                  IRNode.STORE_OF_CLASS, "Foo", "1"}) // No store to class Foo
    public void badCounts() {
        irExample.iFld = iFld3; // No load, store to iFld in class IRExample
        iFld2 = 42; // No load, store to iFld2 in class IRExample
    }
}
