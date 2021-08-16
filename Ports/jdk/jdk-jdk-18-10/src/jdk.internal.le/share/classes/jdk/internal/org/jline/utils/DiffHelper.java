/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.utils;

import java.util.LinkedList;
import java.util.List;

/**
 * Class containing the diff method.
 * This diff is ANSI aware and will correctly handle text attributes
 * so that any text in a Diff object is a valid ansi string.
 */
public class DiffHelper {

    /**
     * The data structure representing a diff is a Linked list of Diff objects:
     * {Diff(Operation.DELETE, "Hello"), Diff(Operation.INSERT, "Goodbye"),
     *  Diff(Operation.EQUAL, " world.")}
     * which means: delete "Hello", add "Goodbye" and keep " world."
     */
    public enum Operation {
        DELETE, INSERT, EQUAL
    }

    /**
     * Class representing one diff operation.
     */
    public static class Diff {
        /**
         * One of: INSERT, DELETE or EQUAL.
         */
        public final Operation operation;
        /**
         * The text associated with this diff operation.
         */
        public final AttributedString text;

        /**
         * Constructor.  Initializes the diff with the provided values.
         * @param operation One of INSERT, DELETE or EQUAL.
         * @param text The text being applied.
         */
        public Diff(Operation operation, AttributedString text) {
            // Construct a diff with the specified operation and text.
            this.operation = operation;
            this.text = text;
        }

        /**
         * Display a human-readable version of this Diff.
         * @return text version.
         */
        public String toString() {
            return "Diff(" + this.operation + ",\"" + this.text + "\")";
        }
    }

    /**
     * Compute a list of difference between two lines.
     * The result will contain at most 4 Diff objects, as the method
     * aims to return the common prefix, inserted text, deleted text and
     * common suffix.
     * The computation is done on characters and their attributes expressed
     * as ansi sequences.
     *
     * @param text1 the old line
     * @param text2 the new line
     * @return a list of Diff
     */
    public static List<Diff> diff(AttributedString text1, AttributedString text2) {
        int l1 = text1.length();
        int l2 = text2.length();
        int n = Math.min(l1, l2);
        int commonStart = 0;
        // Given a run of contiguous "hidden" characters (which are
        // sequences of uninterrupted escape sequences) we always want to
        // print either the entire run or none of it - never a part of it.
        int startHiddenRange = -1;
        while (commonStart < n
                && text1.charAt(commonStart) == text2.charAt(commonStart)
                && text1.styleAt(commonStart).equals(text2.styleAt(commonStart))) {
            if (text1.isHidden(commonStart)) {
                if (startHiddenRange < 0)
                    startHiddenRange = commonStart;
            } else
                startHiddenRange = -1;
            commonStart++;
        }
        if (startHiddenRange >= 0
            && ((l1 > commonStart && text1.isHidden(commonStart))
                || (l2 > commonStart && text2.isHidden(commonStart))))
            commonStart = startHiddenRange;

        startHiddenRange = -1;
        int commonEnd = 0;
        while (commonEnd < n - commonStart
                && text1.charAt(l1 - commonEnd - 1) == text2.charAt(l2 - commonEnd - 1)
                && text1.styleAt(l1 - commonEnd - 1).equals(text2.styleAt(l2 - commonEnd - 1))) {
            if (text1.isHidden(l1 - commonEnd - 1)) {
                if (startHiddenRange < 0)
                    startHiddenRange = commonEnd;
            } else
                startHiddenRange = -1;
            commonEnd++;
        }
        if (startHiddenRange >= 0)
            commonEnd = startHiddenRange;
        LinkedList<Diff> diffs = new LinkedList<>();
        if (commonStart > 0) {
            diffs.add(new Diff(DiffHelper.Operation.EQUAL,
                    text1.subSequence(0, commonStart)));
        }
        if (l2 > commonStart + commonEnd) {
            diffs.add(new Diff(DiffHelper.Operation.INSERT,
                    text2.subSequence(commonStart, l2 - commonEnd)));
        }
        if (l1 > commonStart + commonEnd) {
            diffs.add(new Diff(DiffHelper.Operation.DELETE,
                    text1.subSequence(commonStart, l1 - commonEnd)));
        }
        if (commonEnd > 0) {
            diffs.add(new Diff(DiffHelper.Operation.EQUAL,
                    text1.subSequence(l1 - commonEnd, l1)));
        }
        return diffs;
    }

}
