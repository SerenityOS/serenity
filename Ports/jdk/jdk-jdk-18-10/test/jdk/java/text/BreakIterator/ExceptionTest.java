/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6521742
 * @summary test exceptions
 */

import java.text.*;
import java.util.*;
import static java.text.BreakIterator.DONE;

public class ExceptionTest {
    private static final String text =
          "An ordered collection (also known as a sequence). "
        + "The user of this interface has precise control over "
        + "where in the list each element is inserted. "
        + "The user can access elements by their integer index (position in the list), "
        + "and search for elements in the list.";

    public static void main(String[] args) {
        BreakIterator bi = BreakIterator.getWordInstance();
        bi.setText(text);
        MirroredBreakIterator mirror = new MirroredBreakIterator(bi);
        final int first = bi.first();
        if (first != 0) {
            throw new RuntimeException("first != 0: " + first);
        }
        final int last = bi.last();
        bi = BreakIterator.getWordInstance();
        bi.setText(text);
        int length = text.length();

        /*
         * following(int)
         */
        for (int i = 0; i <= length; i++) {
            if (i == length) {
                check(bi.following(i), DONE);
            }
            check(bi.following(i), mirror.following(i));
            check(bi.current(), mirror.current());
        }
        for (int i = -length; i < 0; i++) {
            checkFollowingException(bi, i);
            checkFollowingException(mirror, i);
            check(bi.current(), mirror.current());
        }
        for (int i = 1; i < length; i++) {
            checkFollowingException(bi, length + i);
            checkFollowingException(mirror, length + i);
            check(bi.current(), mirror.current());
        }

        /*
         * preceding(int)
         */
        for (int i = length; i >= 0; i--) {
            if (i == 0) {
                check(bi.preceding(i), DONE);
            }
            check(bi.preceding(i), mirror.preceding(i));
            check(bi.current(), mirror.current());
        }
        for (int i = -length; i < 0; i++) {
            checkPrecedingException(bi, i);
            checkPrecedingException(mirror, i);
            check(bi.current(), mirror.current());
        }
        for (int i = 1; i < length; i++) {
            checkPrecedingException(bi, length + i);
            checkPrecedingException(mirror, length + i);
            check(bi.current(), mirror.current());
        }

        /*
         * isBoundary(int)
         */
        for (int i = 0; i <= length; i++) {
            check(bi.isBoundary(i), mirror.isBoundary(i));
            check(bi.current(), mirror.current());
        }
        for (int i = -length; i < 0; i++) {
            checkIsBoundaryException(bi, i);
            checkIsBoundaryException(mirror, i);
        }
        for (int i = 1; i < length; i++) {
            checkIsBoundaryException(bi, length + i);
            checkIsBoundaryException(mirror, length + i);
        }
    }

    private static void check(int i1, int i2) {
        if (i1 != i2) {
            throw new RuntimeException(i1 + " != " + i2);
        }
    }

    private static void check(boolean b1, boolean b2) {
        if (b1 != b2) {
            throw new RuntimeException(b1 + " != " + b2);
        }
    }

    private static void checkFollowingException(BreakIterator bi, int offset) {
        try {
            bi.following(offset);
        } catch (IllegalArgumentException e) {
            return; // OK
        }
        throw new RuntimeException(bi + ": following() doesn't throw an IAE with offset "
                                   + offset);
    }

    private static void checkPrecedingException(BreakIterator bi, int offset) {
        try {
            bi.preceding(offset);
        } catch (IllegalArgumentException e) {
            return; // OK
        }
        throw new RuntimeException(bi + ": preceding() doesn't throw an IAE with offset "
                                   + offset);
    }

    private static void checkIsBoundaryException(BreakIterator bi, int offset) {
        try {
            bi.isBoundary(offset);
        } catch (IllegalArgumentException e) {
            return; // OK
        }
        throw new RuntimeException(bi + ": isBoundary() doesn't throw an IAE with offset "
                                   + offset);
    }
}
