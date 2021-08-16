/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8080535 8191410 8215194 8221431 8239383
 * @summary Expected size of Character.UnicodeBlock.map is not optimal
 * @library /test/lib
 * @modules java.base/java.lang:open
 *          java.base/java.util:open
 * @build jdk.test.lib.util.OptimalCapacity
 * @run main OptimalMapSize
 */

import java.lang.reflect.Field;
import jdk.test.lib.util.OptimalCapacity;

// What will be the number of the Unicode blocks in the future.
//
// According to http://www.unicode.org/versions/Unicode7.0.0/ ,
// in Unicode 7 there will be added 32 new blocks (96 with aliases).
// According to http://www.unicode.org/versions/beta-8.0.0.html ,
// in Unicode 8 there will be added 10 more blocks (30 with aliases).
//
// After implementing support of Unicode 9 and 10 in Java, there will
// be 638 entries in Character.UnicodeBlock.map.
//
// As of Unicode 11, 667 entries are expected.
// As of Unicode 12.1, 676 entries are expected.
// As of Unicode 13.0, 684 entries are expected.
//
// Initialization of the map and this test will have to be adjusted
// accordingly then.
//
// Note that HashMap's implementation aligns the initial capacity to
// a power of two size, so it will end up 1024 (and thus succeed) in
// cases, such as 638, 667, 676, and 684.

public class OptimalMapSize {
    public static void main(String[] args) throws Throwable {
        // The initial size of Character.UnicodeBlock.map.
        // See src/java.base/share/classes/java/lang/Character.java
        Field f = Character.UnicodeBlock.class.getDeclaredField("NUM_ENTITIES");
        f.setAccessible(true);
        int num_entities = f.getInt(null);
        assert num_entities == 684;
        int initialCapacity = (int)(num_entities / 0.75f + 1.0f);

        OptimalCapacity.ofHashMap(Character.UnicodeBlock.class,
                "map", initialCapacity);
    }
}
