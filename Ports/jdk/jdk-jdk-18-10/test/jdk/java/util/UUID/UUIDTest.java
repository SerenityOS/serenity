/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4173528 5068772 8148936 8196334
 * @summary Unit tests for java.util.UUID
 * @key randomness
 * @run main/othervm -XX:+CompactStrings UUIDTest
 * @run main/othervm -XX:-CompactStrings UUIDTest
 */

import java.util.*;

public class UUIDTest {

    static Random generator = new Random();

    public static void main(String[] args) throws Exception {
        containsTest();
        randomUUIDTest();
        nameUUIDFromBytesTest();
        stringTest();
        versionTest();
        variantTest();
        timestampTest();
        clockSequenceTest();
        nodeTest();
        hashCodeEqualsTest();
        compareTo();
    }

    // Verify that list.contains detects UUID collisons
    private static void containsTest() throws Exception {
        List list = new LinkedList();
        list.add(new UUID(4,4));
        if (!list.contains(new UUID(4,4)))
            throw new Exception("contains test did not work as expected");
    }

    private static void randomUUIDTest() throws Exception {
        List list = new LinkedList();
        for (int i=0; i<100; i++) {
            UUID u1 = UUID.randomUUID();
            if (4 != u1.version()) {
                throw new Exception("bad version");
            }
            if (2 != u1.variant()) {
                throw new Exception("bad variant");
            }
            if (list.contains(u1))
                throw new Exception("random UUID collision very unlikely");
            list.add(u1);
        }
    }

    private static void nameUUIDFromBytesTest() throws Exception {
        Random byteSource = new Random();
        byte[] someBytes = new byte[12];
        List list = new LinkedList();
        for (int i=0; i<100; i++) {
            byteSource.nextBytes(someBytes);
            UUID u1 = UUID.nameUUIDFromBytes(someBytes);
            if (3 != u1.version()) {
                throw new Exception("bad version");
            }
            if (2 != u1.variant()) {
                throw new Exception("bad variant");
            }
            if (list.contains(u1))
                throw new Exception("byte UUID collision very unlikely");
            list.add(u1);
        }
    }

    private static void stringTest() throws Exception {
        for (int i=0; i<100; i++) {
            UUID u1 = UUID.randomUUID();
            UUID u2 = UUID.fromString(u1.toString().toLowerCase());
            UUID u3 = UUID.fromString(u1.toString().toUpperCase());
            if (!u1.equals(u2) || !u1.equals(u3))
                throw new Exception("UUID -> string -> UUID failed");
        }

        testFromStringError("-0");
        testFromStringError("x");
        testFromStringError("----");
        testFromStringError("-0-0-0-0");
        testFromStringError("0-0-0-0-");
        testFromStringError("0-0-0-0-0-");
        testFromStringError("0-0-0-0-x");
    }

    private static void testFromStringError(String str) {
        try {
            UUID test = UUID.fromString(str);
            throw new RuntimeException("Should have thrown IAE");
        } catch (IllegalArgumentException iae) {
            // pass
        }
    }

    private static void versionTest() throws Exception {
        UUID test = UUID.randomUUID();
        if (test.version() != 4)
            throw new Exception("randomUUID not type 4");
        Random byteSource = new Random();
        byte[] someBytes = new byte[12];
        byteSource.nextBytes(someBytes);
        test = UUID.nameUUIDFromBytes(someBytes);
        if (test.version() != 3)
            throw new Exception("nameUUIDFromBytes not type 3");
        test = UUID.fromString("9835451d-e2e0-1e41-8a5a-be785f17dcda");
        if (test.version() != 1)
            throw new Exception("wrong version fromString 1");
        test = UUID.fromString("9835451d-e2e0-2e41-8a5a-be785f17dcda");
        if (test.version() != 2)
            throw new Exception("wrong version fromString 2");
        test = UUID.fromString("9835451d-e2e0-3e41-8a5a-be785f17dcda");
        if (test.version() != 3)
            throw new Exception("wrong version fromString 3");
        test = UUID.fromString("9835451d-e2e0-4e41-8a5a-be785f17dcda");
        if (test.version() != 4)
            throw new Exception("wrong version fromString 4");
        test = new UUID(0x0000000000001000L, 55L);
        if (test.version() != 1)
            throw new Exception("wrong version from bit set to 1");
        test = new UUID(0x0000000000002000L, 55L);
        if (test.version() != 2)
            throw new Exception("wrong version from bit set to 2");
        test = new UUID(0x0000000000003000L, 55L);
        if (test.version() != 3)
            throw new Exception("wrong version from bit set to 3");
        test = new UUID(0x0000000000004000L, 55L);
        if (test.version() != 4)
            throw new Exception("wrong version from bit set to 4");
    }

    private static void variantTest() throws Exception {
        UUID test = UUID.randomUUID();
        if (test.variant() != 2)
            throw new Exception("randomUUID not variant 2");
        Random byteSource = new Random();
        byte[] someBytes = new byte[12];
        byteSource.nextBytes(someBytes);
        test = UUID.nameUUIDFromBytes(someBytes);
        if (test.variant() != 2)
            throw new Exception("nameUUIDFromBytes not variant 2");
        test = new UUID(55L, 0x0000000000001000L);
        if (test.variant() != 0)
            throw new Exception("wrong variant from bit set to 0");
        test = new UUID(55L, 0x8000000000001000L);
        if (test.variant() != 2)
            throw new Exception("wrong variant from bit set to 2");
       test = new UUID(55L, 0xc000000000001000L);
        if (test.variant() != 6)
            throw new Exception("wrong variant from bit set to 6");
       test = new UUID(55L, 0xe000000000001000L);
        if (test.variant() != 7)
            throw new Exception("wrong variant from bit set to 7");
    }

    private static void timestampTest() throws Exception {
        UUID test = UUID.randomUUID();
        try {
            test.timestamp();
            throw new Exception("Expected exception not thrown");
        } catch (UnsupportedOperationException uoe) {
            // Correct result
        }
        test = UUID.fromString("00000001-0000-1000-8a5a-be785f17dcda");
        if (test.timestamp() != 1)
            throw new Exception("Incorrect timestamp");
        test = UUID.fromString("00000400-0000-1000-8a5a-be785f17dcda");
        if (test.timestamp() != 1024)
            throw new Exception("Incorrect timestamp");
        test = UUID.fromString("FFFFFFFF-FFFF-1FFF-8a5a-be785f17dcda");
        if (test.timestamp() != Long.MAX_VALUE>>3)
            throw new Exception("Incorrect timestamp");
    }

    private static void clockSequenceTest() throws Exception {
        UUID test = UUID.randomUUID();
        try {
            test.clockSequence();
            throw new Exception("Expected exception not thrown");
        } catch (UnsupportedOperationException uoe) {
            // Correct result
        }
        test = UUID.fromString("00000001-0000-1000-8001-be785f17dcda");
        if (test.clockSequence() != 1)
            throw new Exception("Incorrect sequence");
        test = UUID.fromString("00000001-0000-1000-8002-be785f17dcda");
        if (test.clockSequence() != 2)
            throw new Exception("Incorrect sequence");
        test = UUID.fromString("00000001-0000-1000-8010-be785f17dcda");
        if (test.clockSequence() != 16)
            throw new Exception("Incorrect sequence");
        test = UUID.fromString("00000001-0000-1000-bFFF-be785f17dcda");
        if (test.clockSequence() != ((2L<<13)-1)) // 2^14 - 1
            throw new Exception("Incorrect sequence");
    }

    private static void nodeTest() throws Exception {
        UUID test = UUID.randomUUID();
        try {
            test.node();
            throw new Exception("Expected exception not thrown");
        } catch (UnsupportedOperationException uoe) {
            // Correct result
        }
        test = UUID.fromString("00000001-0000-1000-8001-000000000001");
        if (test.node() != 1)
            throw new Exception("Incorrect node");
        test = UUID.fromString("00000001-0000-1000-8002-FFFFFFFFFFFF");
        if (test.node() != ((2L<<47)-1)) // 2^48 - 1
            throw new Exception("Incorrect node");
    }

    private static void hashCodeEqualsTest() throws Exception {
        // If two UUIDs are equal they must have the same hashCode
        for (int i=0; i<100; i++) {
            UUID u1 = UUID.randomUUID();
            UUID u2 = UUID.fromString(u1.toString());
            if (u1.hashCode() != u2.hashCode())
                throw new Exception("Equal UUIDs with different hashcodes");
        }
        // Test equality of UUIDs with tampered bits
        for (int i=0; i<1000; i++) {
            long l = generator.nextLong();
            long l2 = generator.nextLong();
            int position = generator.nextInt(64);
            UUID u1 = new UUID(l, l2);
            l = l ^ (1L << position);
            UUID u2 = new UUID(l, l2);
            if (u1.equals(u2))
                throw new Exception("UUIDs with different bits equal");
        }
    }

    private static void compareTo() throws Exception {
        UUID id = new UUID(33L, 63L);
        UUID id2 = new UUID(34L, 62L);
        UUID id3 = new UUID(34L, 63L);
        UUID id4 = new UUID(34L, 64L);
        UUID id5 = new UUID(35L, 63L);

        if ((id.compareTo(id2) >= 0) ||
            (id2.compareTo(id3) >= 0) ||
            (id3.compareTo(id4) >= 0) ||
            (id4.compareTo(id5) >= 0))
            throw new RuntimeException("compareTo failure");

        if ((id5.compareTo(id4) <= 0) ||
            (id4.compareTo(id3) <= 0) ||
            (id3.compareTo(id2) <= 0) ||
            (id2.compareTo(id) <= 0))
            throw new RuntimeException("compareTo failure");

        if (id.compareTo(id) != 0)
            throw new RuntimeException("compareTo failure");

    }

}
