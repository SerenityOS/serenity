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

package compiler.lib.ir_framework.test;

import compiler.lib.ir_framework.*;
import compiler.lib.ir_framework.shared.*;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Prints an encoding to the dedicated test framework socket whether @IR rules of @Test methods should be applied or not.
 * This is done during the execution of the test VM by checking the active VM flags. This encoding is eventually parsed
 * and checked by the IRMatcher class in the driver VM after the termination of the test VM.
 */
public class IREncodingPrinter {
    public static final String START = "##### IRMatchRulesEncoding - used by TestFramework #####";
    public static final String END = "----- END -----";
    public static final int NO_RULE_APPLIED = -1;

    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final List<Function<String, Object>> LONG_GETTERS = Arrays.asList(
            WHITE_BOX::getIntVMFlag, WHITE_BOX::getUintVMFlag, WHITE_BOX::getIntxVMFlag,
            WHITE_BOX::getUintxVMFlag, WHITE_BOX::getUint64VMFlag, WHITE_BOX::getSizeTVMFlag);

    private final StringBuilder output = new StringBuilder();
    private Method method;
    private int ruleIndex;

    public IREncodingPrinter() {
        output.append(START).append(System.lineSeparator());
        output.append("<method>,{comma separated applied @IR rule ids}").append(System.lineSeparator());
    }

    /**
     * Emits "<method>,{ids}" where {ids} is either:
     * - indices of all @IR rules that should be applied, separated by a comma
     * - "-1" if no @IR rule should not be applied
     */
    public void emitRuleEncoding(Method m, boolean skipped) {
        method = m;
        int i = 0;
        ArrayList<Integer> validRules = new ArrayList<>();
        IR[] irAnnos = m.getAnnotationsByType(IR.class);
        if (!skipped) {
            for (IR irAnno : irAnnos) {
                ruleIndex = i + 1;
                try {
                    if (shouldApplyIrRule(irAnno)) {
                        validRules.add(i);
                    }
                } catch (TestFormatException e) {
                    // Catch logged failure and continue to check other IR annotations.
                }
                i++;
            }
        }
        if (irAnnos.length != 0) {
            output.append(m.getName());
            if (validRules.isEmpty()) {
                output.append("," + NO_RULE_APPLIED);
            } else {
                for (i = 0; i < validRules.size(); i++) {
                    output.append(",").append(validRules.get(i));
                }
            }
            output.append(System.lineSeparator());
        }
    }

    private boolean shouldApplyIrRule(IR irAnno) {
        checkIRAnnotations(irAnno);
        if (isDefaultRegexUnsupported(irAnno)) {
            return false;
        }
        if (irAnno.applyIf().length != 0) {
            return hasAllRequiredFlags(irAnno.applyIf(), "applyIf");
        }

        if (irAnno.applyIfNot().length != 0) {
            return hasNoRequiredFlags(irAnno.applyIfNot(), "applyIfNot");
        }

        if (irAnno.applyIfAnd().length != 0) {
            return hasAllRequiredFlags(irAnno.applyIfAnd(), "applyIfAnd");
        }

        if (irAnno.applyIfOr().length != 0) {
            return !hasNoRequiredFlags(irAnno.applyIfOr(), "applyIfOr");
        }
        // No conditions, always apply.
        return true;
    }

    private void checkIRAnnotations(IR irAnno) {
        TestFormat.checkNoThrow(irAnno.counts().length != 0 || irAnno.failOn().length != 0,
                                "Must specify either counts or failOn constraint" + failAt());
        int applyRules = 0;
        if (irAnno.applyIfAnd().length != 0) {
            applyRules++;
            TestFormat.checkNoThrow(irAnno.applyIfAnd().length > 2,
                                    "Use applyIf or applyIfNot or at least 2 conditions for applyIfAnd" + failAt());
        }
        if (irAnno.applyIfOr().length != 0) {
            applyRules++;
            TestFormat.checkNoThrow(irAnno.applyIfOr().length > 2,
                                    "Use applyIf or applyIfNot or at least 2 conditions for applyIfOr" + failAt());
        }
        if (irAnno.applyIf().length != 0) {
            applyRules++;
            TestFormat.checkNoThrow(irAnno.applyIf().length <= 2,
                                    "Use applyIfAnd or applyIfOr or only 1 condition for applyIf" + failAt());
        }
        if (irAnno.applyIfNot().length != 0) {
            applyRules++;
            TestFormat.checkNoThrow(irAnno.applyIfNot().length <= 2,
                                    "Use applyIfAnd or applyIfOr or only 1 condition for applyIfNot" + failAt());
        }
        TestFormat.checkNoThrow(applyRules <= 1,
                                "Can only specify one apply constraint " + failAt());
    }

    private boolean isDefaultRegexUnsupported(IR irAnno) {
        try {
            for (String s : irAnno.failOn()) {
                IRNode.checkDefaultRegexSupported(s);
            }
            for (String s : irAnno.counts()) {
                IRNode.checkDefaultRegexSupported(s);
            }
        } catch (CheckedTestFrameworkException e) {
            TestFrameworkSocket.write("Skip Rule " + ruleIndex + ": " + e.getMessage(), TestFrameworkSocket.DEFAULT_REGEX_TAG, true);
            return true;
        }
        return false;
    }

    private boolean hasAllRequiredFlags(String[] andRules, String ruleType) {
        boolean returnValue = true;
        for (int i = 0; i < andRules.length; i++) {
            String flag = andRules[i].trim();
            i++;
            TestFormat.check(i < andRules.length, "Missing value for flag " + flag + " in " + ruleType + failAt());
            String value = andRules[i].trim();
            if (!check(flag, value) && returnValue) {
                // Rule will not be applied but keep processing the other flags to verify that they are sane.
                returnValue = false;
            }
        }
        return returnValue;
    }

    private boolean hasNoRequiredFlags(String[] orRules, String ruleType) {
        boolean returnValue = true;
        for (int i = 0; i < orRules.length; i++) {
            String flag = orRules[i];
            i++;
            TestFormat.check(i < orRules.length, "Missing value for flag " + flag + " in " + ruleType + failAt());
            String value = orRules[i];
            if (check(flag, value) && returnValue) {
                // Rule will not be applied but keep processing the other flags to verify that they are sane.
                returnValue = false;
            }
        }
        return returnValue;
    }

    private boolean check(String flag, String value) {
        if (flag.isEmpty()) {
            TestFormat.failNoThrow("Provided empty flag" + failAt());
            return false;
        }
        if (value.isEmpty()) {
            TestFormat.failNoThrow("Provided empty value for flag " + flag + failAt());
            return false;
        }
        Object actualFlagValue = WHITE_BOX.getBooleanVMFlag(flag);
        if (actualFlagValue != null) {
            return checkBooleanFlag(flag, value, (Boolean) actualFlagValue);
        }
        actualFlagValue = LONG_GETTERS.stream().map(f -> f.apply(flag)).filter(Objects::nonNull).findAny().orElse(null);
        if (actualFlagValue != null) {
            return checkLongFlag(flag, value, (Long) actualFlagValue);
        }
        actualFlagValue = WHITE_BOX.getDoubleVMFlag(flag);
        if (actualFlagValue != null) {
            return checkDoubleFlag(flag, value, (Double) actualFlagValue);
        }
        actualFlagValue = WHITE_BOX.getStringVMFlag(flag);
        if (actualFlagValue != null) {
            return value.equals(actualFlagValue);
        }

        // This could be improved if the Whitebox offers a "isVMFlag" function. For now, just check if we can actually set
        // a value for a string flag. If we find this value, it's a string flag. If null is returned, the flag is unknown.
        WHITE_BOX.setStringVMFlag(flag, "test");
        String stringFlagValue = WHITE_BOX.getStringVMFlag(flag);
        if (stringFlagValue == null) {
            TestFormat.failNoThrow("Could not find VM flag \"" + flag + "\"" + failAt());
            return false;
        }
        TestFramework.check(stringFlagValue.equals("test"),
                         "Must find newly set flag value \"test\" but found " + failAt());
        WHITE_BOX.setStringVMFlag(flag, null); // reset flag to NULL
        return false;
    }

    private boolean checkBooleanFlag(String flag, String value, boolean actualFlagValue) {
        boolean booleanValue = false;
        if ("true".equalsIgnoreCase(value)) {
            booleanValue = true;
        } else if (!"false".equalsIgnoreCase(value)) {
            TestFormat.failNoThrow("Invalid value \"" + value + "\" for boolean flag " + flag + failAt());
            return false;
        }
        return booleanValue == actualFlagValue;
    }

    private boolean checkLongFlag(String flag, String value, long actualFlagValue) {
        long longValue;
        ParsedComparator<Long> parsedComparator;
        try {
            parsedComparator = ParsedComparator.parseComparator(value);
        } catch (CheckedTestFrameworkException e) {
            TestFormat.failNoThrow("Invalid comparator in \"" + value + "\" for integer based flag " + flag + failAt());
            return false;
        }  catch (IndexOutOfBoundsException e) {
            TestFormat.failNoThrow("Provided empty value for integer based flag " + flag + failAt());
            return false;
        }
        try {
            longValue = Long.parseLong(parsedComparator.getStrippedString());
        } catch (NumberFormatException e) {
            String comparator = parsedComparator.getComparator();
            if (!comparator.isEmpty()) {
                comparator = "after comparator \"" + parsedComparator.getComparator() + "\"";
            }
            TestFormat.failNoThrow("Invalid value \"" + parsedComparator.getStrippedString() + "\" "
                            + comparator + " for integer based flag " + flag + failAt());
            return false;
        }
        return parsedComparator.getPredicate().test(actualFlagValue, longValue);
    }

    private boolean checkDoubleFlag(String flag, String value, double actualFlagValue) {
        double doubleValue;
        ParsedComparator<Double> parsedComparator;
        try {
            parsedComparator = ParsedComparator.parseComparator(value);
        } catch (CheckedTestFrameworkException e) {
            TestFormat.failNoThrow("Invalid comparator in \"" + value + "\" for floating point based flag " + flag + failAt());
            return false;
        } catch (IndexOutOfBoundsException e) {
            TestFormat.failNoThrow("Provided empty value for floating point based flag " + flag + failAt());
            return false;
        }
        try {
            doubleValue = Double.parseDouble(parsedComparator.getStrippedString());
        } catch (NumberFormatException e) {
            String comparator = parsedComparator.getComparator();
            if (!comparator.isEmpty()) {
                comparator = "after comparator \"" + parsedComparator.getComparator() + "\"";
            }
            TestFormat.failNoThrow("Invalid value \"" + parsedComparator.getStrippedString() + "\" "
                    + comparator + " for floating point based flag " + flag + failAt());
            return false;
        }
        return parsedComparator.getPredicate().test(actualFlagValue, doubleValue);
    }

    private String failAt() {
        return " for @IR rule " + ruleIndex + " at " + method;
    }

    public void emit() {
        output.append(END);
        TestFrameworkSocket.write(output.toString(), "IR rule application encoding");
    }
}


