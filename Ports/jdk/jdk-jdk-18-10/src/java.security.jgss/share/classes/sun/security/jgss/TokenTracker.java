/*
 * Copyright (c) 2000, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.security.jgss;

import org.ietf.jgss.MessageProp;
import java.util.LinkedList;

/**
 * A utility class that implements a number list that keeps track of which
 * tokens have arrived by storing their token numbers in the list. It helps
 * detect old tokens, out of sequence tokens, and duplicate tokens.
 *
 * Each element of the list is an interval [a, b]. Its existence in the
 * list implies that all token numbers in the range a, a+1, ..., b-1, b
 * have arrived. Gaps in arrived token numbers are represented by the
 * numbers that fall in between two elements of the list. eg. {[a,b],
 * [c,d]} indicates that the token numbers b+1, ..., c-1 have not arrived
 * yet.
 *
 * The maximum number of intervals that we keep track of is
 * MAX_INTERVALS. Thus if there are too many gaps, then some of the older
 * sequence numbers are deleted from the list. The earliest sequence number
 * that exists in the list is the windowStart. The next expected sequence
 * number, or expectedNumber, is one greater than the latest sequence
 * number in the list.
 *
 * The list keeps track the first token number that should have arrived
 * (initNumber) so that it is able to detect if certain numbers occur after
 * the first valid token number but before windowStart. That would happen
 * if the number of elements (intervals) exceeds MAX_INTERVALS and some
 * initial elements had  to be deleted.
 *
 * The working of the list is optimized for the normal case where the
 * tokens arrive in sequence.
 *
 * @author Mayank Upadhyay
 * @since 1.4
 */
public class TokenTracker {

    static final int MAX_INTERVALS = 5;

    private int initNumber;
    private int windowStart;
    private int expectedNumber;

    private int windowStartIndex = 0;

    private LinkedList<Entry> list = new LinkedList<Entry>();

    public TokenTracker(int initNumber) {

        this.initNumber = initNumber;
        this.windowStart = initNumber;
        this.expectedNumber = initNumber;

        // Make an entry with one less than the expected first token
        Entry entry = new Entry(initNumber-1);

        list.add(entry);
    }

    /**
     * Returns the index for the entry into which this number will fit. If
     * there is none, it returns the index of the last interval
     * which precedes this number. It returns -1 if the number needs to be
     * a in a new interval ahead of the whole list.
     */
    private int getIntervalIndex(int number) {
        Entry entry = null;
        int i;
        // Start from the rear to optimize for the normal case
        for (i = list.size() - 1; i >= 0; i--) {
            entry = list.get(i);
            if (entry.compareTo(number) <= 0)
                break;
        }
        return i;
    }

    /**
     * Sets the sequencing and replay information for the given token
     * number.
     *
     * The following represents the number line with positions of
     * initNumber, windowStart, expectedNumber marked on it. Regions in
     * between them show the different sequencing and replay state
     * possibilites for tokens that fall in there.
     *
     *  (1)      windowStart
     *           initNumber               expectedNumber
     *              |                           |
     *           ---|---------------------------|---
     *          GAP |    DUP/UNSEQ              | GAP
     *
     *
     *  (2)       initNumber   windowStart   expectedNumber
     *              |               |              |
     *           ---|---------------|--------------|---
     *          GAP |      OLD      |  DUP/UNSEQ   | GAP
     *
     *
     *  (3)                                windowStart
     *           expectedNumber            initNumber
     *              |                           |
     *           ---|---------------------------|---
     *    DUP/UNSEQ |           GAP             | DUP/UNSEQ
     *
     *
     *  (4)      expectedNumber    initNumber   windowStart
     *              |               |              |
     *           ---|---------------|--------------|---
     *    DUP/UNSEQ |        GAP    |    OLD       | DUP/UNSEQ
     *
     *
     *
     *  (5)      windowStart   expectedNumber    initNumber
     *              |               |              |
     *           ---|---------------|--------------|---
     *          OLD |    DUP/UNSEQ  |     GAP      | OLD
     *
     *
     *
     * (This analysis leaves out the possibility that expectedNumber passes
     * initNumber after wrapping around. That may be added later.)
     */
    synchronized public final void getProps(int number, MessageProp prop) {

        boolean gap = false;
        boolean old = false;
        boolean unsequenced = false;
        boolean duplicate = false;

        // System.out.println("\n\n==========");
        // System.out.println("TokenTracker.getProps(): number=" + number);
        // System.out.println(toString());

        int pos = getIntervalIndex(number);
        Entry entry = null;
        if (pos != -1)
            entry = list.get(pos);

        // Optimize for the expected case:

        if (number == expectedNumber) {
            expectedNumber++;
        } else {

            // Next trivial case is to check for duplicate
            if (entry != null && entry.contains(number))
                duplicate = true;
            else {

                if (expectedNumber >= initNumber) {

                    // Cases (1) and (2)

                    if (number > expectedNumber) {
                        gap = true;
                    } else if (number >= windowStart) {
                        unsequenced = true;
                    } else if (number >= initNumber) {
                        old = true;
                    } else {
                        gap = true;
                    }
                } else {

                    // Cases (3), (4) and (5)

                    if (number > expectedNumber) {
                        if (number < initNumber) {
                            gap = true;
                        } else if (windowStart >= initNumber) {
                            if (number >= windowStart) {
                               unsequenced = true;
                            } else
                                old = true;
                        } else {
                            old = true;
                        }
                    } else if (windowStart > expectedNumber) {
                        unsequenced = true;
                    } else if (number < windowStart) {
                        old = true;
                    } else
                        unsequenced = true;
                }
            }
        }

        if (!duplicate && !old)
            add(number, pos);

        if (gap)
            expectedNumber = number+1;

        prop.setSupplementaryStates(duplicate, old, unsequenced, gap,
                                    0, null);

        // System.out.println("Leaving with state:");
        // System.out.println(toString());
        // System.out.println("==========\n");
    }

    /**
     * Adds the number to the list just after the entry that is currently
     * at position prevEntryPos. If prevEntryPos is -1, then the number
     * will begin a new interval at the front of the list.
     */
    private void add(int number, int prevEntryPos) {

        Entry entry;
        Entry entryBefore = null;
        Entry entryAfter = null;

        boolean appended = false;
        boolean prepended = false;

        if (prevEntryPos != -1) {
            entryBefore = list.get(prevEntryPos);

            // Can this number simply be added to the previous interval?
            if (number == (entryBefore.getEnd() + 1)) {
                entryBefore.setEnd(number);
                appended = true;
            }
        }

        // Now check the interval that follows this number

        int nextEntryPos = prevEntryPos + 1;
        if ((nextEntryPos) < list.size()) {
            entryAfter = list.get(nextEntryPos);

            // Can this number simply be added to the next interval?
            if (number == (entryAfter.getStart() - 1)) {
                if (!appended) {
                    entryAfter.setStart(number);
                } else {
                    // Merge the two entries
                    entryAfter.setStart(entryBefore.getStart());
                    list.remove(prevEntryPos);
                    // Index of any entry following this gets decremented
                    if (windowStartIndex > prevEntryPos)
                        windowStartIndex--;
                }
                prepended = true;
            }
        }

        if (prepended || appended)
            return;

        /*
         * At this point we know that the number will start a new interval
         * which needs to be added to the list. We might have to recyle an
         * older entry in the list.
         */

        if (list.size() < MAX_INTERVALS) {
            entry = new Entry(number);
            if (prevEntryPos  < windowStartIndex)
                windowStartIndex++; // due to the insertion which will happen
        } else {
            /*
             * Delete the entry that marks the start of the current window.
             * The marker will automatically point to the next entry in the
             * list when this happens. If the current entry is at the end
             * of the list then set the marker to the start of the list.
             */
            int oldWindowStartIndex = windowStartIndex;
            if (windowStartIndex == (list.size() - 1))
                windowStartIndex = 0;

            entry = list.remove(oldWindowStartIndex);
            windowStart = list.get(windowStartIndex).getStart();
            entry.setStart(number);
            entry.setEnd(number);

            if (prevEntryPos >= oldWindowStartIndex) {
                prevEntryPos--; // due to the deletion that just happened
            } else {
                /*
                 * If the start of the current window just moved from the
                 * end of the list to the front of the list, and if the new
                 * entry will be added to the front of the list, then
                 * the new entry is the actual window start.
                 * eg, Consider { [-10, -8], ..., [-6, -3], [3, 9]}. In
                 * this list, suppose the element [3, 9] is the start of
                 * the window and has to be deleted to make place to add
                 * [-12, -12]. The resultant list will be
                 * {[-12, -12], [-10, -8], ..., [-6, -3]} and the new start
                 * of the window should be the element [-12, -12], not
                 * [-10, -8] which succeeded [3, 9] in the old list.
                 */
                if (oldWindowStartIndex != windowStartIndex) {
                    // windowStartIndex is 0 at this point
                    if (prevEntryPos == -1)
                        // The new entry is going to the front
                        windowStart = number;
                } else {
                    // due to the insertion which will happen:
                    windowStartIndex++;
                }
            }
        }

        // Finally we are ready to actually add to the list at index
        // 'prevEntryPos+1'

        list.add(prevEntryPos+1, entry);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("TokenTracker: ");
        sb.append(" initNumber=").append(initNumber);
        sb.append(" windowStart=").append(windowStart);
        sb.append(" expectedNumber=").append(expectedNumber);
        sb.append(" windowStartIndex=").append(windowStartIndex);
        sb.append("\n\tIntervals are: {");
        for (int i = 0; i < list.size(); i++) {
            if (i != 0)
                sb.append(", ");
            sb.append(list.get(i).toString());
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * An entry in the list that represents the sequence of received
     * tokens. Each entry is actaully an interval of numbers, all of which
     * have been received.
     */
    class Entry {

        private int start;
        private int end;

        Entry(int number) {
            start = number;
            end = number;
        }

        /**
         * Returns -1 if this interval represented by this entry precedes
         * the number, 0 if the number is contained in the interval,
         * and -1 if the interval occurs after the number.
         */
        final int compareTo(int number) {
            if (start > number)
                return 1;
            else if (end < number)
                return -1;
            else
                return 0;
        }

        final boolean contains(int number) {
            return (number >= start &&
                    number <= end);
        }

        final void append(int number) {
            if (number == (end + 1))
                end = number;
        }

        final void setInterval(int start, int end) {
            this.start = start;
            this.end = end;
        }

        final void setEnd(int end) {
            this.end = end;
        }

        final void setStart(int start) {
            this.start = start;
        }

        final int getStart() {
            return start;
        }

        final int getEnd() {
            return end;
        }

        public String toString() {
            return ("[" + start + ", " + end + "]");
        }

    }
}
