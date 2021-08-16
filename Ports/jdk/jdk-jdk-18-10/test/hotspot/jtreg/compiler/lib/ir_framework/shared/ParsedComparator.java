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

package compiler.lib.ir_framework.shared;

import java.util.function.BiPredicate;

/**
 * Utility class to parse a comparator either in the applyIf* or in the counts properties of an @IR rules.
 */
public class ParsedComparator<T extends Comparable<T>> {
    private final String strippedString;
    private final BiPredicate<T, T> predicate;
    private final String comparator;

    public ParsedComparator(String strippedString, BiPredicate<T, T> predicate, String comparator) {
        this.strippedString = strippedString;
        this.predicate = predicate;
        this.comparator = comparator;
    }

    public String getStrippedString() {
        return strippedString;
    }

    public BiPredicate<T, T> getPredicate() {
        return predicate;
    }

    public String getComparator() {
        return comparator;
    }

    /**
     * Return parsed comparator object which provides the predicate to perform the test.
     * Allowed comparators: <, <=, >, =>, =, !=
     */
    public static <T extends Comparable<T>> ParsedComparator<T> parseComparator(String value) throws CheckedTestFrameworkException {
        BiPredicate<T, T> comparison;
        value = value.trim();
        String comparator = "";
        switch (value.charAt(0)) {
            case '<':
                if (value.charAt(1) == '=') {
                    comparator = "<=";
                    comparison = (x, y) -> x.compareTo(y) <= 0;
                    value = value.substring(2).trim();
                } else {
                    comparator = "<";
                    comparison = (x, y) -> x.compareTo(y) < 0;
                    value = value.substring(1).trim();
                }
                break;
            case '>':
                if (value.charAt(1) == '=') {
                    comparator = ">=";
                    comparison = (x, y) -> x.compareTo(y) >= 0;
                    value = value.substring(2).trim();
                } else {
                    comparator = ">";
                    comparison = (x, y) -> x.compareTo(y) > 0;
                    value = value.substring(1).trim();
                }
                break;
            case '!':
                if (value.charAt(1) != '=') {
                    throw new CheckedTestFrameworkException(value.substring(0, 1));
                }
                comparator = "!=";
                comparison = (x, y) -> x.compareTo(y) != 0;
                value = value.substring(2).trim();
                break;
            case '=': // Allowed syntax, equivalent to not using any symbol.
                comparator = "=";
                value = value.substring(1).trim();
                // Fall through
            default:
                comparison = (x, y) -> x.compareTo(y) == 0;
                value = value.trim();
                break;
        }
        return new ParsedComparator<>(value, comparison, comparator);
    }
}
