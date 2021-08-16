/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.pipe;

/**
 * This interface defines a general method for iterating through the
 * rectangular "spans" that represent the interior of a filled path.
 * <p>
 * There can be many kinds of span iterators used in the rendering
 * pipeline, the most basic being an iterator that scan converts a
 * path defined by any PathIterator, or an nested iterator which
 * intersects another iterator's spans with a clip region.
 * Other iterators can be created for scan converting some of the
 * primitive shapes more explicitly for speed or quality.
 *
 * @author Jim Graham
 */
public interface SpanIterator {
    /**
     * This method returns the bounding box of the spans that the
     * iterator will be returning.
     * The array must be of length at least 4 and upon return, it
     * will be filled with the values:
     * <pre>
     *     {PathMinX, PathMinY, PathMaxX, PathMaxY}.
     * </pre>
     */
    public void getPathBox(int[] pathbox);

    /**
     * This method constrains the spans returned by nextSpan() to the
     * rectangle whose bounds are given.
     */
    public void intersectClipBox(int lox, int loy, int hix, int hiy);

    /**
     * This method returns the next span in the shape being iterated.
     * The array must be of length at least 4 and upon return, it
     * will be filled with the values:
     * <pre>
     *     {SpanMinX, SpanMinY, SpanMaxX, SpanMaxY}.
     * </pre>
     */
    public boolean nextSpan(int[] spanbox);

    /**
     * This method tells the iterator that it may skip all spans
     * whose Y range is completely above the indicated Y coordinate.
     * This method is used to provide feedback from the caller when
     * clipping prevents the display of any data in a given Y range.
     * Typically it will only be called when this iterator has returned
     * a span whose MaxY coordinate is less than the indicated Y and
     * the calling mechanism wants to avoid unnecessary iteration work.
     * While this request could technically be ignored (i.e. a NOP),
     * doing so could potentially cause the caller to make this callback
     * for each span that is being skipped.
     */
    public void skipDownTo(int y);

    /**
     * This method returns a native pointer to a function block that
     * can be used by a native method to perform the same iteration
     * cycle that the above methods provide while avoiding upcalls to
     * the Java object.
     * The definition of the structure whose pointer is returned by
     * this method is defined in:
     * <pre>
     *     src/share/native/sun/java2d/pipe/SpanIterator.h
     * </pre>
     */
    public long getNativeIterator();
}
