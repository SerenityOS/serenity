/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.util.stream;

import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamOpFlag;
import java.util.stream.Streams;

import static java.util.stream.StreamOpFlag.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * StreamFlagsTest
 *
 * @author Brian Goetz
 */
@Test
public class StreamFlagsTest {
    Stream<String> arrayList = new ArrayList<String>().stream();
    Stream<String> linkedList = new LinkedList<String>().stream();
    Stream<String> hashSet = new HashSet<String>().stream();
    Stream<String> treeSet = new TreeSet<String>().stream();
    Stream<String> linkedHashSet = new LinkedHashSet<String>().stream();
    Stream<String> repeat = Stream.generate(() -> "");

    Stream<?>[] streams = { arrayList, linkedList, hashSet, treeSet, linkedHashSet, repeat };

    private void assertFlags(int value, EnumSet<StreamOpFlag> setFlags, EnumSet<StreamOpFlag> clearFlags) {
        for (StreamOpFlag flag : setFlags)
            assertTrue(flag.isKnown(value));
        for (StreamOpFlag flag : clearFlags)
            assertTrue(!flag.isKnown(value));
    }

    public void testBaseStreams() {
        Stream<String> arrayList = new ArrayList<String>().stream();
        Stream<String> linkedList = new LinkedList<String>().stream();
        Stream<String> hashSet = new HashSet<String>().stream();
        Stream<String> treeSet = new TreeSet<String>().stream();
        Stream<String> linkedHashSet = new LinkedHashSet<String>().stream();
        Stream<String> repeat = Stream.generate(() -> "");

        assertFlags(OpTestCase.getStreamFlags(arrayList),
                    EnumSet.of(ORDERED, SIZED),
                    EnumSet.of(DISTINCT, SORTED, SHORT_CIRCUIT));
        assertFlags(OpTestCase.getStreamFlags(linkedList),
                    EnumSet.of(ORDERED, SIZED),
                    EnumSet.of(DISTINCT, SORTED, SHORT_CIRCUIT));
        assertFlags(OpTestCase.getStreamFlags(hashSet),
                    EnumSet.of(SIZED, DISTINCT),
                    EnumSet.of(ORDERED, SORTED, SHORT_CIRCUIT));
        assertFlags(OpTestCase.getStreamFlags(treeSet),
                    EnumSet.of(ORDERED, SIZED, DISTINCT, SORTED),
                    EnumSet.of(SHORT_CIRCUIT));
        assertFlags(OpTestCase.getStreamFlags(linkedHashSet),
                    EnumSet.of(ORDERED, DISTINCT, SIZED),
                    EnumSet.of(SORTED, SHORT_CIRCUIT));
        assertFlags(OpTestCase.getStreamFlags(repeat),
                    EnumSet.noneOf(StreamOpFlag.class),
                    EnumSet.of(DISTINCT, SORTED, SHORT_CIRCUIT));
    }

    public void testFilter() {
        for (Stream<?> s : streams) {
            int baseFlags = OpTestCase.getStreamFlags(s);
            int filteredFlags = OpTestCase.getStreamFlags(s.filter((Object e) -> true));
            int expectedFlags = baseFlags & ~SIZED.set();

            assertEquals(filteredFlags, expectedFlags);
        }
    }
}
