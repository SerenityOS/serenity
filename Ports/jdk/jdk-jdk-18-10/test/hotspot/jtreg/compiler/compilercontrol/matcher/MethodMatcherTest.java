/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @key randomness
 * @bug 8135068
 * @summary Tests CompilerCommand's method matcher
 * @modules java.base/jdk.internal.misc
 * @library /test/lib /
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *           -XX:+WhiteBoxAPI compiler.compilercontrol.matcher.MethodMatcherTest
 */

package compiler.compilercontrol.matcher;

import compiler.compilercontrol.share.method.MethodDescriptor;
import compiler.compilercontrol.share.method.MethodGenerator;
import compiler.compilercontrol.share.pool.PoolHelper;
import jdk.test.lib.util.Pair;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodMatcherTest {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    private static final PoolHelper POOL = new PoolHelper();
    private static final List<Pair<Executable, Callable<?>>> METHODS =
            POOL.getAllMethods();
    private static final int AMOUNT = Integer.parseInt(System
            .getProperty("test.amount", "25"));

    public static void main(String[] args) {
        MethodGenerator gen = new MethodGenerator();
        List<Pair<Executable, Callable<?>>> testMethods =
                POOL.getAllMethods(PoolHelper.METHOD_FILTER);
        for (Pair<Executable, Callable<?>> pair : testMethods) {
            for (int i = 0; i < AMOUNT; i++) {
                MethodDescriptor md = gen.generateRandomDescriptor(pair.first);
                check(md);
            }
        }
    }

    /**
     * Check method matcher with given test case
     *
     * @param methodDescriptor method descriptor to check matcher's pattern
     */
    private static void check(MethodDescriptor methodDescriptor) {
        System.out.println("Test case: " + methodDescriptor.getString());
        System.out.println("Regex: " + methodDescriptor.getRegexp());
        Pattern pattern = Pattern.compile(methodDescriptor.getRegexp());
        boolean isValidDesc = methodDescriptor.isValid();
        List<MethodDescriptor> failList = new ArrayList<>();
        // walk through all methods in pool to check match with test pattern
        for (Pair<Executable, Callable<?>> pair : METHODS) {
            MethodDescriptor m = MethodGenerator.commandDescriptor(pair.first);
            Matcher matcher = pattern.matcher(m.getCanonicalString());
            // get expected result
            MatcherResult expected;
            if (isValidDesc) {
                expected = matcher.matches() ?
                        MatcherResult.MATCH : MatcherResult.NO_MATCH;
            } else {
                expected = MatcherResult.PARSING_FAILURE;
            }
            // get MethodMatcher's result
            MatcherResult matchResult = MatcherResult.fromCode(WB.matchesMethod(
                    pair.first, methodDescriptor.getString()));
            // compare
            if (matchResult != expected) {
                System.out.printf("- Method: %s%n-- FAILED: result: %s, " +
                                "but expected: %s%n", m.getCanonicalString(),
                        matchResult, expected);
                failList.add(m);
            }
        }
        int size = failList.size();
        if (size != 0) {
            System.err.println("FAILED test case: " + methodDescriptor
                    .getString());
            if (size == METHODS.size()) {
                System.err.println("-- All methods failed to match");
            } else {
                for (MethodDescriptor md : failList) {
                    System.err.println("-- FAILED match: " + md.getString());
                }
            }
            throw new AssertionError("FAIL: " + methodDescriptor.getString());
        }
        System.out.println("--PASSED");
    }

    /**
     * Represents MethodMatcher's matching result
     */
    public enum MatcherResult {
        PARSING_FAILURE(-1, "Parsing failed"),
        NO_MATCH(0, "No match"),
        MATCH(1, "Match");

        public final int code;
        private final String message;

        private MatcherResult(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public static MatcherResult fromCode(int code) {
            switch (code) {
                case -1: return PARSING_FAILURE;
                case  0: return NO_MATCH;
                case  1: return MATCH;
                default:
                    throw new IllegalArgumentException("MATCHER FAILURE:"
                            + "Wrong code: " + code);
            }
        }

        @Override
        public String toString() {
            return message;
        }
    }
}
