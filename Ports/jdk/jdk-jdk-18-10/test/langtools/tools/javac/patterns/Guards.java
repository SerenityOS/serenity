/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8262891 8268663
 * @summary Check guards implementation.
 * @compile --enable-preview -source ${jdk.version} Guards.java
 * @run main/othervm --enable-preview Guards
 */

import java.util.Objects;
import java.util.function.Function;

public class Guards {
    public static void main(String... args) {
        new Guards().run();
    }

    void run() {
        run(this::typeTestPatternSwitchTest);
        run(this::typeTestPatternSwitchExpressionTest);
        run(this::testBooleanSwitchExpression);
        assertEquals("a", testPatternInGuard("a"));
        assertEquals(null, testPatternInGuard(1));
        runIfTrue(this::typeGuardIfTrueIfStatement);
        runIfTrue(this::typeGuardIfTrueSwitchExpression);
        runIfTrue(this::typeGuardIfTrueSwitchStatement);
        runIfTrue(this::typeGuardAfterParenthesizedTrueSwitchStatement);
        runIfTrue(this::typeGuardAfterParenthesizedTrueSwitchExpression);
        runIfTrue(this::typeGuardAfterParenthesizedTrueIfStatement);
    }

    void run(Function<Object, String> convert) {
        assertEquals("zero", convert.apply(0));
        assertEquals("one", convert.apply(1));
        assertEquals("other", convert.apply(-1));
        assertEquals("any", convert.apply(""));
    }

    void runIfTrue(Function<Object, String> convert) {
        assertEquals("true", convert.apply(0));
        assertEquals("second", convert.apply(2));
        assertEquals("any", convert.apply(""));
    }

    String typeTestPatternSwitchTest(Object o) {
        switch (o) {
            case Integer i && i == 0: return "zero";
            case Integer i && i == 1: return "one";
            case Integer i: return "other";
            case Object x: return "any";
        }
    }

    String typeTestPatternSwitchExpressionTest(Object o) {
        return switch (o) {
            case Integer i && i == 0 -> "zero";
            case Integer i && i == 1 -> { yield "one"; }
            case Integer i -> "other";
            case Object x -> "any";
        };
    }

    String testBooleanSwitchExpression(Object o) {
        String x;
        if (switch (o) {
            case Integer i && i == 0 -> (x = "zero") != null;
            case Integer i && i == 1 -> { x = "one"; yield true; }
            case Integer i -> { x = "other"; yield true; }
            case Object other -> (x = "any") != null;
        }) {
            return x;
        } else {
            throw new IllegalStateException("TODO - needed?");
        }
    }

    String typeGuardIfTrueSwitchStatement(Object o) {
        Object o2 = "";
        switch (o) {
            case Integer i && i == 0 && i < 1 && o2 instanceof String s: o = s + String.valueOf(i); return "true";
            case Integer i && i == 0 || i > 1: o = String.valueOf(i); return "second";
            case Object x: return "any";
        }
    }

    String typeGuardIfTrueSwitchExpression(Object o) {
        Object o2 = "";
        return switch (o) {
            case Integer i && i == 0 && i < 1 && o2 instanceof String s: o = s + String.valueOf(i); yield "true";
            case Integer i && i == 0 || i > 1: o = String.valueOf(i); yield "second";
            case Object x: yield "any";
        };
    }

    String typeGuardIfTrueIfStatement(Object o) {
        Object o2 = "";
        if (o != null && o instanceof (Integer i && i == 0 && i < 1) && (o = i) != null && o2 instanceof String s) {
            return s != null ? "true" : null;
        } else if (o != null && o instanceof (Integer i && i == 0 || i > 1) && (o = i) != null) {
            return "second";
        } else {
            return "any";
        }
    }

    String typeGuardAfterParenthesizedTrueSwitchStatement(Object o) {
        switch (o) {
            case (Integer i) && i == 0: o = String.valueOf(i); return "true";
            case ((Integer i) && i == 2): o = String.valueOf(i); return "second";
            case Object x: return "any";
        }
    }

    String typeGuardAfterParenthesizedTrueSwitchExpression(Object o) {
        return switch (o) {
            case (Integer i) && i == 0: o = String.valueOf(i); yield "true";
            case ((Integer i) && i == 2): o = String.valueOf(i); yield "second";
            case Object x: yield "any";
        };
    }

    String typeGuardAfterParenthesizedTrueIfStatement(Object o) {
        if (o != null && o instanceof ((Integer i) && i == 0)) {
            return "true";
        } else if (o != null && o instanceof (((Integer i) && i == 2)) && (o = i) != null) {
            return "second";
        } else {
            return "any";
        }
    }

    String testPatternInGuard(Object o) {
        if (o instanceof (CharSequence cs && cs instanceof String s)) {
            return s;
        }
        return null;
    }

    void assertEquals(String expected, String actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected: " + expected + ", but got: " + actual);
        }
    }
}
