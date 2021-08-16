/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright (c) 2011 IBM Corporation
 */

/*
 * @test
 * @bug 7014637
 * @summary EnumSet's iterator.remove() can be resilient to set's modification.
 * @author Neil Richards <neil.richards@ngmr.net>, <neil_richards@uk.ibm.com>
 */

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

public class SmallEnumIteratorRemoveResilience {
    // enum with less than 64 values
    private static enum SmallEnum { e0, e1, e2 }

    public static void main(final String[] args) throws Exception {
        final Set<SmallEnum> set = EnumSet.noneOf(SmallEnum.class);

        set.add(SmallEnum.e0);
        set.add(SmallEnum.e1);

        final Iterator<SmallEnum> iterator = set.iterator();

        int size = set.size();
        SmallEnum element = iterator.next();

        iterator.remove();
        checkSetAfterRemoval(set, size, element);

        size = set.size();
        element = iterator.next();

        set.remove(element);
        checkSetAfterRemoval(set, size, element);

        // The Java API declares that the behaviour here - to call
        // iterator.remove() after the underlying collection has been
        // modified - is "unspecified".
        // However, in the case of iterators for EnumSet, it is easy to
        // implement their remove() operation such that the set is
        // unmodified if it is called for an element that has already been
        // removed from the set - this being the naturally "resilient"
        // behaviour.
        iterator.remove();
        checkSetAfterRemoval(set, size, element);
    }

    private static void checkSetAfterRemoval(final Set<SmallEnum> set,
            final int origSize, final SmallEnum removedElement)
            throws Exception {
        if (set.size() != (origSize - 1)) {
            throw new Exception("Test FAILED: Unexpected set size after removal; expected '" + (origSize - 1) + "' but found '" + set.size() + "'");
        }
        if (set.contains(removedElement)) {
            throw new Exception("Test FAILED: Element returned from iterator unexpectedly still in set after removal.");
        }
    }
}
