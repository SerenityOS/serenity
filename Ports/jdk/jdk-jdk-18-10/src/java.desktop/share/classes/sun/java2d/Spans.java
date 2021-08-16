/*
 * Copyright (c) 1998, 2000, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d;

import java.util.Comparator;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Maintains a list of half-open intervals, called Spans.
 * A Span can be tested against the list of Spans
 * for intersection.
 */
public class Spans {

    /**
     * This class will sort and collapse its span
     * entries after this many span additions via
     * the {@code add} method.
     */
    private static final int kMaxAddsSinceSort = 256;

    /**
     * Holds a list of individual
     * Span instances.
     */
    private List<Span> mSpans = new Vector<>(kMaxAddsSinceSort);

    /**
     * The number of {@code Span}
     * instances that have been added
     * to this object without a sort
     * and collapse taking place.
     */
    private int mAddsSinceSort = 0;

    public Spans() {

    }

    /**
     * Add a span covering the half open interval
     * including {@code start} up to
     * but not including {@code end}.
     */
    public void add(float start, float end) {

        if (mSpans != null) {
            mSpans.add(new Span(start, end));

            if (++mAddsSinceSort >= kMaxAddsSinceSort) {
                sortAndCollapse();
            }
        }
    }

    /**
     * Add a span which covers the entire range.
     * This call is logically equivalent to
     * {@code add(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)}
     * The result of making this call is that
     * all future {@code add} calls are ignored
     * and the {@code intersects} method always
     * returns true.
     */
    public void addInfinite() {
        mSpans = null;
    }

    /**
     * Returns true if the span defined by the half-open
     * interval from {@code start} up to,
     * but not including, {@code end} intersects
     * any of the spans defined by this instance.
     */
    public boolean intersects(float start, float end) {
        boolean doesIntersect;

        if (mSpans != null) {

            /* If we have added any spans since we last
             * sorted and collapsed our list of spans
             * then we need to resort and collapse.
             */
            if (mAddsSinceSort > 0) {
                sortAndCollapse();
            }

            /* The SpanIntersection comparator considers
             * two spans equal if they intersect. If
             * the search finds a match then we have an
             * intersection.
             */
            int found = Collections.binarySearch(mSpans,
                                                 new Span(start, end),
                                                 SpanIntersection.instance);

            doesIntersect = found >= 0;

        /* The addInfinite() method has been invoked so
         * everything intersect this instance.
         */
        } else {
           doesIntersect = true;
        }

        return doesIntersect;
    }

    /**
     * Sort the spans in ascending order by their
     * start position. After the spans are sorted
     * collapse any spans that intersect into a
     * single span. The result is a sorted,
     * non-overlapping list of spans.
     */
    private void sortAndCollapse() {

        Collections.sort(mSpans);
        mAddsSinceSort = 0;

        Iterator<Span> iter = mSpans.iterator();

        /* Have 'span' start at the first span in
         * the collection. The collection may be empty
         * so we're careful.
         */
        Span span = null;
        if (iter.hasNext()) {
            span = iter.next();
        }

        /* Loop over the spans collapsing those that intersect
         * into a single span.
         */
        while (iter.hasNext()) {

            Span nextSpan = iter.next();

            /* The spans are in ascending start position
             * order and so the next span's starting point
             * is either in the span we are trying to grow
             * or it is beyond the first span and thus the
             * two spans do not intersect.
             *
             * span:    <----------<
             * nextSpan:        <------         (intersects)
             * nextSpan:                <------ (doesn't intersect)
             *
             * If the spans intersect then we'll remove
             * nextSpan from the list. If nextSpan's
             * ending was beyond the first's then
             * we extend the first.
             *
             * span:    <----------<
             * nextSpan:   <-----<              (don't change span)
             * nextSpan:        <-----------<   (grow span)
             */

            if (span.subsume(nextSpan)) {
                iter.remove();

            /* The next span did not intersect the current
             * span and so it can not be collapsed. Instead
             * it becomes the start of the next set of spans
             * to be collapsed.
             */
            } else {
                span = nextSpan;
            }
        }
    }

    /*
    // For debugging.

    private void printSpans() {
        System.out.println("----------");
        if (mSpans != null) {
            Iterator<Span> iter = mSpans.iterator();
            while (iter.hasNext()) {
                Span span = iter.next();
                System.out.println(span);
            }
        }
        System.out.println("----------");

    }
    */

    /**
     * Holds a single half-open interval.
     */
    static class Span implements Comparable<Span> {

        /**
         * The span includes the starting point.
         */
        private float mStart;

        /**
         * The span goes up to but does not include
         * the ending point.
         */
        private float mEnd;

        /**
         * Create a half-open interval including
         * {@code start} but not including
         * {@code end}.
         */
        Span(float start, float end) {
            mStart = start;
            mEnd = end;
        }

        /**
         * Return the start of the {@code Span}.
         * The start is considered part of the
         * half-open interval.
         */
        final float getStart() {
            return mStart;
        }

        /**
         * Return the end of the {@code Span}.
         * The end is not considered part of the
         * half-open interval.
         */
        final float getEnd() {
            return mEnd;
        }

        /**
         * Change the initial position of the
         * {@code Span}.
         */
        final void setStart(float start) {
            mStart = start;
        }

        /**
         * Change the terminal position of the
         * {@code Span}.
         */
        final void setEnd(float end) {
            mEnd = end;
        }

        /**
         * Attempt to alter this {@code Span}
         * to include {@code otherSpan} without
         * altering this span's starting position.
         * If {@code otherSpan} can be so consumed
         * by this {@code Span} then {@code true}
         * is returned.
         */
        boolean subsume(Span otherSpan) {

            /* We can only subsume 'otherSpan' if
             * its starting position lies in our
             * interval.
             */
            boolean isSubsumed = contains(otherSpan.mStart);

            /* If the other span's starting position
             * was in our interval and the other span
             * was longer than this span, then we need
             * to grow this span to cover the difference.
             */
            if (isSubsumed && otherSpan.mEnd > mEnd) {
                mEnd = otherSpan.mEnd;
            }

            return isSubsumed;
        }

        /**
         * Return true if the passed in position
         * lies in the half-open interval defined
         * by this {@code Span}.
         */
        boolean contains(float pos) {
            return mStart <= pos && pos < mEnd;
        }

        /**
         * Rank spans according to their starting
         * position. The end position is ignored
         * in this ranking.
         */
        public int compareTo(Span otherSpan) {
            float otherStart = otherSpan.getStart();
            int result;

            if (mStart < otherStart) {
                result = -1;
            } else if (mStart > otherStart) {
                result = 1;
            } else {
                result = 0;
            }

            return result;
        }

        public String toString() {
            return "Span: " + mStart + " to " + mEnd;
        }

    }

    /**
     * This class ranks a pair of {@code Span}
     * instances. If the instances intersect they
     * are deemed equal otherwise they are ranked
     * by their relative position. Use
     * {@code SpanIntersection.instance} to
     * get the single instance of this class.
     */
    static class SpanIntersection implements Comparator<Span> {

        /**
         * This class is a Singleton and the following
         * is the single instance.
         */
        static final SpanIntersection instance =
                                      new SpanIntersection();

        /**
         * Only this class can create instances of itself.
         */
        private SpanIntersection() {

        }

        public int compare(Span span1, Span span2) {
            int result;

            /* Span 1 is entirely to the left of span2.
             * span1:   <-----<
             * span2:            <-----<
             */
            if (span1.getEnd() <= span2.getStart()) {
                result = -1;

            /* Span 2 is entirely to the right of span2.
             * span1:                     <-----<
             * span2:            <-----<
             */
            } else if (span1.getStart() >= span2.getEnd()) {
                result = 1;

            /* Otherwise they intersect and we declare them equal.
            */
            } else {
                result = 0;
            }

            return result;
        }

    }
}
