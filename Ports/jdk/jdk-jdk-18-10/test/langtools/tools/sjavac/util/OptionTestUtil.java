/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package util;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.sun.tools.sjavac.options.SourceLocation;


public class OptionTestUtil {

    public static void checkFilesFound(Collection<String> found, Path... expected) {

        Collection<String> expectedStrs = new HashSet<String>();
        for (Path p : expected)
            expectedStrs.add(p.toString());

        if (!expectedStrs.containsAll(found))
            throw new AssertionError("Expected (" + expectedStrs + ") does not " +
                                     "contain all actual (" + found + ")");

        if (!found.containsAll(expectedStrs))
            throw new AssertionError("Actual (" + found + ") does not " +
                                     "contain all expected (" + expectedStrs + ")");
    }

    public static void assertEquals(List<SourceLocation> expected, List<SourceLocation> actual) {
        if (expected.size() != actual.size())
            throw new AssertionError("Expected locs of length " + expected.size() + " but got something of size " + actual.size());

        Iterator<SourceLocation> iter1 = expected.iterator();
        Iterator<SourceLocation> iter2 = actual.iterator();

        while (iter1.hasNext()) {
            SourceLocation sl1 = iter1.next();
            SourceLocation sl2 = iter2.next();

            if (!sl1.getPath().equals(sl2.getPath()) ||
                    !sl1.getIncludes().equals(sl2.getIncludes()) ||
                    !sl1.getExcludes().equals(sl2.getExcludes()))
                throw new AssertionError("Expected " + sl1 + " but got " + sl2);
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual))
            throw new AssertionError("Expected " + expected + " but got " + actual);
    }

}
