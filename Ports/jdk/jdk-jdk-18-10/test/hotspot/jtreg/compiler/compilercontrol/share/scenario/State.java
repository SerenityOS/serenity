/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.compilercontrol.share.scenario;

import jdk.test.lib.Asserts;

import java.util.Arrays;
import java.util.Optional;

/**
 * Represents method compilation state
 */
public class State {
    // Each of the two-elements array contains a state for each compiler
    private Optional<Boolean>[] compile =
            (Optional<Boolean>[]) new Optional[Scenario.Compiler.values().length];
    private Optional<Boolean>[] forceInline =
            (Optional<Boolean>[]) new Optional[Scenario.Compiler.values().length];
    private Optional<Boolean>[] dontInline =
            (Optional<Boolean>[]) new Optional[Scenario.Compiler.values().length];
    private Optional<Boolean> printAssembly = Optional.empty();
    private Optional<Boolean> printInline = Optional.empty();
    private Optional<Boolean> log = Optional.empty();
    private Optional<String> controlIntrinsic = Optional.empty();

    public State() {
        Arrays.fill(compile, Optional.empty());
        Arrays.fill(forceInline, Optional.empty());
        Arrays.fill(dontInline, Optional.empty());
    }

    /**
     * Creates state from the string
     *
     * @param strings array of strings that represent the state
     * @return State instance
     * @see #toString()
     */
    public static State fromString(String[] strings) {
        Asserts.assertNotNull(strings, "Non null array is required");
        Asserts.assertNE(strings.length, 0, "Non empty array is required");
        State st = new State();
        for (String string : strings) {
            int i = string.indexOf(' ');
            String command = string.substring(0, i);
            String values = string.substring(i + 1); // skip space symbol
            switch (command) {
                case "compile" :
                    parseArray(st.compile, values);
                    break;
                case "force_inline" :
                    parseArray(st.forceInline, values);
                    break;
                case "dont_inline" :
                    parseArray(st.dontInline, values);
                    break;
                case "log" :
                    st.log = parseElement(values);
                    break;
                case "print_assembly" :
                    st.printAssembly = parseElement(values);
                    break;
                case "print_inline" :
                    st.printInline = parseElement(values);
                    break;
                default:
                    throw new Error("TESTBUG: ");
            }
        }
        return  st;
    }

    private static void parseArray(Optional<Boolean>[] array, String str) {
        Asserts.assertNotNull(str);
        int beginBrace = 0;
        int endBrace = str.length() - 1;
        if (str.charAt(beginBrace) != '[' || str.charAt(endBrace) != ']') {
            throw new Error("TESTBUG: not an array type: " + str);
        }
        // Get all elements divided with comma as an array
        String[] strValues = str.substring(beginBrace + 1, endBrace)
                .split(", ");
        Asserts.assertEQ(strValues.length, array.length, "Different amount of "
                + "elements in the string");
        for (int i = 0; i < strValues.length; i++) {
            array[i] = parseElement(strValues[i]);
        }
    }

    private static Optional<Boolean> parseElement(String str) {
        Asserts.assertNotNull(str);
        Asserts.assertTrue(str.startsWith(Optional.class.getSimpleName()),
                "String is not of type Optional: " + str);
        if ("Optional.empty".equals(str)) {
            return Optional.empty();
        }
        int begin = str.indexOf('[');
        Asserts.assertNE(begin, -1, "TEST BUG: Wrong Optional string");
        int end = str.indexOf(']');
        Asserts.assertEQ(end, str.length() - 1);
        boolean b = Boolean.parseBoolean(str.substring(begin + 1, end));
        return Optional.of(b);
    }

    /**
     * Gets string representation of this state
     */
    @Override
    public String toString() {
        return "compile " + Arrays.toString(compile)
                + "\nforce_inline " + Arrays.toString(forceInline)
                + "\ndont_inline " + Arrays.toString(dontInline)
                + "\nlog " + log
                + "\nprint_assembly " + printAssembly
                + "\nprint_inline " + printInline;
    }

    public Optional<Boolean> getCompilableOptional(Scenario.Compiler compiler) {
        return compile[compiler.ordinal()];
    }

    public boolean isC1Compilable() {
        return compile[Scenario.Compiler.C1.ordinal()].orElse(true);
    }

    public boolean isC2Compilable() {
        return compile[Scenario.Compiler.C2.ordinal()].orElse(true);
    }

    public boolean isCompilable() {
        return isC1Compilable() && isC2Compilable();
    }

    public void setC1Compilable(boolean value) {
        setCompilable(Scenario.Compiler.C1.ordinal(), value);
    }

    public void setC2Compilable(boolean value) {
        setCompilable(Scenario.Compiler.C2.ordinal(), value);
    }

    public void setCompilable(Scenario.Compiler compiler, boolean value) {
        if (compiler == null) {
            setC1Compilable(value);
            setC2Compilable(value);
            return;
        }
        switch (compiler) {
            case C1:
                setC1Compilable(value);
                break;
            case C2:
                setC2Compilable(value);
                break;
            default:
                throw new Error("Unknown compiler");
        }
    }

    private void setCompilable(int level, boolean value) {
        check(level);
        compile[level] = Optional.of(value);
        if (!value) {
            setDontInline(level);
        }
    }

    public boolean isC1Inlinable() {
        return ! dontInline[Scenario.Compiler.C1.ordinal()].orElse(false)
                && isC1Compilable();
    }

    public boolean isC2Inlinable() {
        return ! dontInline[Scenario.Compiler.C2.ordinal()].orElse(false)
                && isC2Compilable();
    }

    public boolean isInlinable() {
        return isC1Inlinable() && isC2Inlinable();
    }

    private void setDontInline(int level) {
        check(level);
        dontInline[level] = Optional.of(true);
        forceInline[level] = Optional.of(false);
    }

    private void setForceInline(int level) {
        check(level);
        dontInline[level] = Optional.of(false);
        forceInline[level] = Optional.of(true);
    }

    public boolean isC1ForceInline() {
        return forceInline[Scenario.Compiler.C1.ordinal()].orElse(false)
                && isC1Compilable();
    }

    public boolean isC2ForceInline() {
        return forceInline[Scenario.Compiler.C2.ordinal()].orElse(false)
                && isC2Compilable();
    }

    public boolean isForceInline() {
        return isC1ForceInline() && isC2ForceInline();
    }

    public void setC1Inline(boolean value) {
        if (value && isC1Compilable()) {
            setForceInline(Scenario.Compiler.C1.ordinal());
        } else {
            setDontInline(Scenario.Compiler.C1.ordinal());
        }
    }

    public void setC2Inline(boolean value) {
        if (value && isC2Compilable()) {
            setForceInline(Scenario.Compiler.C2.ordinal());
        } else {
            setDontInline(Scenario.Compiler.C2.ordinal());
        }
    }

    public void setInline(Scenario.Compiler compiler, boolean value) {
        if (compiler == null) {
            setC1Inline(value);
            setC2Inline(value);
            return;
        }
        switch (compiler) {
            case C1:
                setC1Inline(value);
                break;
            case C2:
                setC2Inline(value);
                break;
            default:
                throw new Error("Unknown compiler");
        }
    }

    public boolean isPrintAssembly() {
        return printAssembly.orElse(false);
    }

    public void setPrintAssembly(boolean value) {
        printAssembly = Optional.of(value);
    }

    public boolean isPrintInline() {
        return printInline.orElse(false);
    }

    public void setPrintInline(boolean value) {
        printInline = Optional.of(value);
    }

    public void setControlIntrinsic(String argument) {
        if (argument != null) {
            controlIntrinsic = Optional.of(argument);
        }
    }

    public boolean isLog() {
        return log.orElse(false);
    }

    public void setLog(boolean log) {
        this.log = Optional.of(log);
    }

    private void check(int level) {
        if (level < 0 || level > compile.length) {
            throw new IllegalArgumentException("TESTBUG: Wrong level " + level);
        }
    }

    /**
     * Applies given command to the state.
     *
     * @param compileCommand command to be applied
     */
    public void apply(CompileCommand compileCommand) {
        switch (compileCommand.command) {
            case COMPILEONLY:
                setCompilable(compileCommand.compiler, true);
                break;
            case EXCLUDE:
                setCompilable(compileCommand.compiler, false);
                break;
            case INLINE:
                setInline(compileCommand.compiler, true);
                break;
            case DONTINLINE:
                setInline(compileCommand.compiler, false);
                break;
            case LOG:
                setLog(true);
                break;
            case PRINT:
                setPrintAssembly(true);
                break;
            case INTRINSIC:
                setControlIntrinsic(compileCommand.argument);
                break;
            case QUIET:
            case NONEXISTENT:
                // doesn't apply the state
                break;
            default:
                throw new Error("Wrong command: " + compileCommand.command);
        }
    }

    /**
     * Merges two given states with different priority
     *
     * @param low  state with lower merge priority
     * @param high state with higher merge priority
     */
    public static State merge(State low, State high) {
        if (high == null) {
            if (low == null) {
                return new State();
            }
            return low;
        }
        if (low == null) {
            return high;
        }
        State result = new State();
        // Compilable
        result.compile[Scenario.Compiler.C1.ordinal()] = mergeOptional(
                high.compile[Scenario.Compiler.C1.ordinal()],
                low.compile[Scenario.Compiler.C1.ordinal()]);
        result.compile[Scenario.Compiler.C2.ordinal()] = mergeOptional(
                high.compile[Scenario.Compiler.C2.ordinal()],
                low.compile[Scenario.Compiler.C2.ordinal()]);
        // Force inline
        result.forceInline[Scenario.Compiler.C1.ordinal()] = mergeOptional(
                high.forceInline[Scenario.Compiler.C1.ordinal()],
                low.forceInline[Scenario.Compiler.C1.ordinal()]);
        result.forceInline[Scenario.Compiler.C2.ordinal()] = mergeOptional(
                high.forceInline[Scenario.Compiler.C2.ordinal()],
                low.forceInline[Scenario.Compiler.C2.ordinal()]);
        // Don't inline
        result.dontInline[Scenario.Compiler.C1.ordinal()] = mergeOptional(
                high.dontInline[Scenario.Compiler.C1.ordinal()],
                low.dontInline[Scenario.Compiler.C1.ordinal()]);
        result.dontInline[Scenario.Compiler.C2.ordinal()] = mergeOptional(
                high.dontInline[Scenario.Compiler.C2.ordinal()],
                low.dontInline[Scenario.Compiler.C2.ordinal()]);
        // set PrintAssembly
        result.printAssembly = mergeOptional(high.printAssembly,
                low.printAssembly);
        // set PrintInline
        result.printInline = mergeOptional(high.printInline, low.printInline);
        // set LogCompilation
        result.log = mergeOptional(high.log, low.log);
        // set controlIntrinsic
        result.controlIntrinsic = mergeOptional(high.controlIntrinsic, low.controlIntrinsic);

        return result;
    }

    private static <T> Optional<T> mergeOptional(Optional<T> high,
                                                 Optional<T> low) {
        T val = high.orElse(low.orElse(null));
        return Optional.ofNullable(val);
    }
}
