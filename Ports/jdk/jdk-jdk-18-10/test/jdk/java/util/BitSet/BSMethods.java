/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4098239 4107540 4080736 4261102 4274710 4305272
 *      4979017 4979028 4979031 5030267 6222207 8040806
 * @summary Test the operation of the methods of BitSet class
 * @author Mike McCloskey, Martin Buchholz
 * @run main/othervm BSMethods
 * @key randomness
 */

import java.util.*;

/**
 * This is a simple test class created to run tests on the BitSet class.
 *
 */
public class BSMethods {

    private static Random generator = new Random();
    private static boolean failure = false;

    private static void fail(String diagnostic) {
        new Error(diagnostic).printStackTrace();
        failure = true;
    }

    private static void check(boolean condition) {
        check(condition, "something's fishy");
    }

    private static void check(boolean condition, String diagnostic) {
        if (! condition)
            fail(diagnostic);
    }

    private static void checkEmpty(BitSet s) {
        check(s.isEmpty(), "isEmpty");
        check(s.length() == 0, "length");
        check(s.cardinality() == 0, "cardinality");
        check(s.equals(new BitSet())   , "equals");
        check(s.equals(new BitSet(0))  , "equals");
        check(s.equals(new BitSet(127)), "equals");
        check(s.equals(new BitSet(128)), "equals");
        check(s.nextSetBit(0)   == -1, "nextSetBit");
        check(s.nextSetBit(127) == -1, "nextSetBit");
        check(s.nextSetBit(128) == -1, "nextSetBit");
        check(s.nextClearBit(0)   == 0,   "nextClearBit");
        check(s.nextClearBit(127) == 127, "nextClearBit");
        check(s.nextClearBit(128) == 128, "nextClearBit");
        check(s.toString().equals("{}"), "toString");
        check(! s.get(0), "get");
    }

    private static BitSet makeSet(int... elts) {
        BitSet s = new BitSet();
        for (int elt : elts)
            s.set(elt);
        return s;
    }

    private static void checkEquality(BitSet s, BitSet t) {
        checkSanity(s, t);
        check(s.equals(t), "equals");
        check(s.toString().equals(t.toString()), "equal strings");
        check(s.length() == t.length(), "equal lengths");
        check(s.cardinality() == t.cardinality(), "equal cardinalities");
    }

    private static void checkSanity(BitSet... sets) {
        for (BitSet s : sets) {
            int len = s.length();
            int cardinality1 = s.cardinality();
            int cardinality2 = 0;
            for (int i = s.nextSetBit(0); i >= 0; i = s.nextSetBit(i+1)) {
                check(s.get(i));
                cardinality2++;
            }
            check(s.nextSetBit(len) == -1, "last set bit");
            check(s.nextClearBit(len) == len, "last set bit");
            check(s.isEmpty() == (len == 0), "emptiness");
            check(cardinality1 == cardinality2, "cardinalities");
            check(len <= s.size(), "length <= size");
            check(len >= 0, "length >= 0");
            check(cardinality1 >= 0, "cardinality >= 0");
        }
    }

    public static void main(String[] args) {

        //testFlipTime();

        // These are the single bit versions
        testSetGetClearFlip();

        // Test the ranged versions
        testClear();

        testFlip();
        testSet();
        testGet();

        // BitSet interaction calls
        testAndNot();
        testAnd();
        testOr();
        testXor();

        // Miscellaneous calls
        testLength();
        testEquals();
        testNextSetBit();
        testNextClearBit();
        testIntersects();
        testCardinality();
        testEmpty();
        testEmpty2();
        testToString();
        testLogicalIdentities();

        if (failure)
            throw new RuntimeException("One or more BitSet failures.");
    }

    private static void report(String testName, int failCount) {
        System.err.println(testName+": " +
                           (failCount==0 ? "Passed":"Failed("+failCount+")"));
        if (failCount > 0)
            failure = true;
    }

    private static void testFlipTime() {
        // Make a fairly random bitset
        BitSet b1 = new BitSet();
        b1.set(1000);
        long startTime = System.currentTimeMillis();
        for(int x=0; x<100000; x++) {
            b1.flip(100, 900);
        }
        long endTime = System.currentTimeMillis();
        long total = endTime - startTime;
        System.out.println("Multiple word flip Time "+total);

        startTime = System.currentTimeMillis();
        for(int x=0; x<100000; x++) {
            b1.flip(2, 44);
        }
        endTime = System.currentTimeMillis();
        total = endTime - startTime;
        System.out.println("Single word flip Time "+total);
    }

    private static void testNextSetBit() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            int numberOfSetBits = generator.nextInt(100) + 1;
            BitSet testSet = new BitSet();
            int[] history = new int[numberOfSetBits];

            // Set some random bits and remember them
            int nextBitToSet = 0;
            for (int x=0; x<numberOfSetBits; x++) {
                nextBitToSet += generator.nextInt(30)+1;
                history[x] = nextBitToSet;
                testSet.set(nextBitToSet);
            }

            // Verify their retrieval using nextSetBit()
            int historyIndex = 0;
            for(int x=testSet.nextSetBit(0); x>=0; x=testSet.nextSetBit(x+1)) {
                if (x != history[historyIndex++])
                    failCount++;
            }

            checkSanity(testSet);
        }

        report("NextSetBit                  ", failCount);
    }

    private static void testNextClearBit() {
        int failCount = 0;

        for (int i=0; i<1000; i++) {
            BitSet b = new BitSet(256);
            int[] history = new int[10];

            // Set all the bits
            for (int x=0; x<256; x++)
                b.set(x);

            // Clear some random bits and remember them
            int nextBitToClear = 0;
            for (int x=0; x<10; x++) {
                nextBitToClear += generator.nextInt(24)+1;
                history[x] = nextBitToClear;
                b.clear(nextBitToClear);
            }

            // Verify their retrieval using nextClearBit()
            int historyIndex = 0;
            for(int x=b.nextClearBit(0); x<256; x=b.nextClearBit(x+1)) {
                if (x != history[historyIndex++])
                    failCount++;
            }

            checkSanity(b);
        }

        // regression test for 4350178
        BitSet bs  = new BitSet();
        if (bs.nextClearBit(0) != 0)
                failCount++;
        for (int i = 0; i < 64; i++) {
            bs.set(i);
            if (bs.nextClearBit(0) != i+1)
                failCount++;
        }

        checkSanity(bs);

        report("NextClearBit                ", failCount);
    }

    private static void testSetGetClearFlip() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            BitSet testSet = new BitSet();
            HashSet<Integer> history = new HashSet<Integer>();

            // Set a random number of bits in random places
            // up to a random maximum
            int nextBitToSet = 0;
            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;
            for (int x=0; x<numberOfSetBits; x++) {
                nextBitToSet = generator.nextInt(highestPossibleSetBit);
                history.add(new Integer(nextBitToSet));
                testSet.set(nextBitToSet);
            }

            // Make sure each bit is set appropriately
            for (int x=0; x<highestPossibleSetBit; x++) {
                if (testSet.get(x) != history.contains(new Integer(x)))
                    failCount++;
            }

            // Clear the bits
            Iterator<Integer> setBitIterator = history.iterator();
            while (setBitIterator.hasNext()) {
                Integer setBit = setBitIterator.next();
                testSet.clear(setBit.intValue());
            }

            // Verify they were cleared
            for (int x=0; x<highestPossibleSetBit; x++)
                if (testSet.get(x))
                    failCount++;
            if(testSet.length() != 0)
                failCount++;

            // Set them with set(int, boolean)
            setBitIterator = history.iterator();
            while (setBitIterator.hasNext()) {
                Integer setBit = setBitIterator.next();
                testSet.set(setBit.intValue(), true);
            }

            // Make sure each bit is set appropriately
            for (int x=0; x<highestPossibleSetBit; x++) {
                if (testSet.get(x) != history.contains(new Integer(x)))
                    failCount++;
            }

            // Clear them with set(int, boolean)
            setBitIterator = history.iterator();
            while (setBitIterator.hasNext()) {
                Integer setBit = (Integer)setBitIterator.next();
                testSet.set(setBit.intValue(), false);
            }

            // Verify they were cleared
            for (int x=0; x<highestPossibleSetBit; x++)
                if (testSet.get(x))
                    failCount++;
            if(testSet.length() != 0)
                failCount++;

            // Flip them on
            setBitIterator = history.iterator();
            while (setBitIterator.hasNext()) {
                Integer setBit = (Integer)setBitIterator.next();
                testSet.flip(setBit.intValue());
            }

            // Verify they were flipped
            for (int x=0; x<highestPossibleSetBit; x++) {
                if (testSet.get(x) != history.contains(new Integer(x)))
                    failCount++;
            }

            // Flip them off
            setBitIterator = history.iterator();
            while (setBitIterator.hasNext()) {
                Integer setBit = (Integer)setBitIterator.next();
                testSet.flip(setBit.intValue());
            }

            // Verify they were flipped
            for (int x=0; x<highestPossibleSetBit; x++)
                if (testSet.get(x))
                    failCount++;
            if(testSet.length() != 0)
                failCount++;

            checkSanity(testSet);
        }

        report("SetGetClearFlip             ", failCount);
    }

    private static void testAndNot() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);
            BitSet b2 = new BitSet(256);

            // Set some random bits in first set and remember them
            int nextBitToSet = 0;
            for (int x=0; x<10; x++)
                b1.set(generator.nextInt(255));

            // Set some random bits in second set and remember them
            for (int x=10; x<20; x++)
                b2.set(generator.nextInt(255));

            // andNot the sets together
            BitSet b3 = (BitSet)b1.clone();
            b3.andNot(b2);

            // Examine each bit of b3 for errors
            for(int x=0; x<256; x++) {
                boolean bit1 = b1.get(x);
                boolean bit2 = b2.get(x);
                boolean bit3 = b3.get(x);
                if (!(bit3 == (bit1 & (!bit2))))
                    failCount++;
            }
            checkSanity(b1, b2, b3);
        }

        report("AndNot                      ", failCount);
    }

    private static void testAnd() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);
            BitSet b2 = new BitSet(256);

            // Set some random bits in first set and remember them
            int nextBitToSet = 0;
            for (int x=0; x<10; x++)
                b1.set(generator.nextInt(255));

            // Set more random bits in second set and remember them
            for (int x=10; x<20; x++)
                b2.set(generator.nextInt(255));

            // And the sets together
            BitSet b3 = (BitSet)b1.clone();
            b3.and(b2);

            // Examine each bit of b3 for errors
            for(int x=0; x<256; x++) {
                boolean bit1 = b1.get(x);
                boolean bit2 = b2.get(x);
                boolean bit3 = b3.get(x);
                if (!(bit3 == (bit1 & bit2)))
                    failCount++;
            }
            checkSanity(b1, b2, b3);
        }

        // `and' that happens to clear the last word
        BitSet b4 = makeSet(2, 127);
        b4.and(makeSet(2, 64));
        checkSanity(b4);
        if (!(b4.equals(makeSet(2))))
            failCount++;

        report("And                         ", failCount);
    }

    private static void testOr() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);
            BitSet b2 = new BitSet(256);
            int[] history = new int[20];

            // Set some random bits in first set and remember them
            int nextBitToSet = 0;
            for (int x=0; x<10; x++) {
                nextBitToSet = generator.nextInt(255);
                history[x] = nextBitToSet;
                b1.set(nextBitToSet);
            }

            // Set more random bits in second set and remember them
            for (int x=10; x<20; x++) {
                nextBitToSet = generator.nextInt(255);
                history[x] = nextBitToSet;
                b2.set(nextBitToSet);
            }

            // Or the sets together
            BitSet b3 = (BitSet)b1.clone();
            b3.or(b2);

            // Verify the set bits of b3 from the history
            int historyIndex = 0;
            for(int x=0; x<20; x++) {
                if (!b3.get(history[x]))
                    failCount++;
            }

            // Examine each bit of b3 for errors
            for(int x=0; x<256; x++) {
                boolean bit1 = b1.get(x);
                boolean bit2 = b2.get(x);
                boolean bit3 = b3.get(x);
                if (!(bit3 == (bit1 | bit2)))
                    failCount++;
            }
            checkSanity(b1, b2, b3);
        }

        report("Or                          ", failCount);
    }

    private static void testXor() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);
            BitSet b2 = new BitSet(256);

            // Set some random bits in first set and remember them
            int nextBitToSet = 0;
            for (int x=0; x<10; x++)
                b1.set(generator.nextInt(255));

            // Set more random bits in second set and remember them
            for (int x=10; x<20; x++)
                b2.set(generator.nextInt(255));

            // Xor the sets together
            BitSet b3 = (BitSet)b1.clone();
            b3.xor(b2);

            // Examine each bit of b3 for errors
            for(int x=0; x<256; x++) {
                boolean bit1 = b1.get(x);
                boolean bit2 = b2.get(x);
                boolean bit3 = b3.get(x);
                if (!(bit3 == (bit1 ^ bit2)))
                    failCount++;
            }
            checkSanity(b1, b2, b3);
            b3.xor(b3); checkEmpty(b3);
        }

        // xor that happens to clear the last word
        BitSet b4 = makeSet(2, 64, 127);
        b4.xor(makeSet(64, 127));
        checkSanity(b4);
        if (!(b4.equals(makeSet(2))))
            failCount++;

        report("Xor                         ", failCount);
    }

    private static void testEquals() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            // Create BitSets of different sizes
            BitSet b1 = new BitSet(generator.nextInt(1000)+1);
            BitSet b2 = new BitSet(generator.nextInt(1000)+1);

            // Set some random bits
            int nextBitToSet = 0;
            for (int x=0; x<10; x++) {
                nextBitToSet += generator.nextInt(50)+1;
                b1.set(nextBitToSet);
                b2.set(nextBitToSet);
            }

            // Verify their equality despite different storage sizes
            if (!b1.equals(b2))
                failCount++;
            checkEquality(b1,b2);
        }

        report("Equals                      ", failCount);
    }

    private static void testLength() {
        int failCount = 0;

        // Test length after set
        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);
            int highestSetBit = 0;

            for(int x=0; x<100; x++) {
                int nextBitToSet = generator.nextInt(255);
                if (nextBitToSet > highestSetBit)
                    highestSetBit = nextBitToSet;
                b1.set(nextBitToSet);
                if (b1.length() != highestSetBit + 1)
                    failCount++;
            }
            checkSanity(b1);
        }

        // Test length after flip
        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);
            for(int x=0; x<100; x++) {
                // Flip a random range twice
                int rangeStart = generator.nextInt(100);
                int rangeEnd = rangeStart + generator.nextInt(100);
                b1.flip(rangeStart);
                b1.flip(rangeStart);
                if (b1.length() != 0)
                    failCount++;
                b1.flip(rangeStart, rangeEnd);
                b1.flip(rangeStart, rangeEnd);
                if (b1.length() != 0)
                    failCount++;
            }
            checkSanity(b1);
        }

        // Test length after or
        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);
            BitSet b2 = new BitSet(256);
            int bit1 = generator.nextInt(100);
            int bit2 = generator.nextInt(100);
            int highestSetBit = (bit1 > bit2) ? bit1 : bit2;
            b1.set(bit1);
            b2.set(bit2);
            b1.or(b2);
            if (b1.length() != highestSetBit + 1)
                failCount++;
            checkSanity(b1, b2);
        }

        report("Length                      ", failCount);
    }

    private static void testClear() {
        int failCount = 0;

        for (int i=0; i<1000; i++) {
            BitSet b1 = new BitSet();

            // Make a fairly random bitset
            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;

            for (int x=0; x<numberOfSetBits; x++)
                b1.set(generator.nextInt(highestPossibleSetBit));

            BitSet b2 = (BitSet)b1.clone();

            // Clear out a random range
            int rangeStart = generator.nextInt(100);
            int rangeEnd = rangeStart + generator.nextInt(100);

            // Use the clear(int, int) call on b1
            b1.clear(rangeStart, rangeEnd);

            // Use a loop on b2
            for (int x=rangeStart; x<rangeEnd; x++)
                b2.clear(x);

            // Verify their equality
            if (!b1.equals(b2)) {
                System.out.println("rangeStart = " + rangeStart);
                System.out.println("rangeEnd = " + rangeEnd);
                System.out.println("b1 = " + b1);
                System.out.println("b2 = " + b2);
                failCount++;
            }
            checkEquality(b1,b2);
        }

        report("Clear                       ", failCount);
    }

    private static void testSet() {
        int failCount = 0;

        // Test set(int, int)
        for (int i=0; i<1000; i++) {
            BitSet b1 = new BitSet();

            // Make a fairly random bitset
            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;

            for (int x=0; x<numberOfSetBits; x++)
                b1.set(generator.nextInt(highestPossibleSetBit));

            BitSet b2 = (BitSet)b1.clone();

            // Set a random range
            int rangeStart = generator.nextInt(100);
            int rangeEnd = rangeStart + generator.nextInt(100);

            // Use the set(int, int) call on b1
            b1.set(rangeStart, rangeEnd);

            // Use a loop on b2
            for (int x=rangeStart; x<rangeEnd; x++)
                b2.set(x);

            // Verify their equality
            if (!b1.equals(b2)) {
                System.out.println("Set 1");
                System.out.println("rangeStart = " + rangeStart);
                System.out.println("rangeEnd = " + rangeEnd);
                System.out.println("b1 = " + b1);
                System.out.println("b2 = " + b2);
                failCount++;
            }
            checkEquality(b1,b2);
        }

        // Test set(int, int, boolean)
        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet();

            // Make a fairly random bitset
            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;

            for (int x=0; x<numberOfSetBits; x++)
                b1.set(generator.nextInt(highestPossibleSetBit));

            BitSet b2 = (BitSet)b1.clone();
            boolean setOrClear = generator.nextBoolean();

            // Set a random range
            int rangeStart = generator.nextInt(100);
            int rangeEnd = rangeStart + generator.nextInt(100);

            // Use the set(int, int, boolean) call on b1
            b1.set(rangeStart, rangeEnd, setOrClear);

            // Use a loop on b2
            for (int x=rangeStart; x<rangeEnd; x++)
                b2.set(x, setOrClear);

            // Verify their equality
            if (!b1.equals(b2)) {
                System.out.println("Set 2");
                System.out.println("b1 = " + b1);
                System.out.println("b2 = " + b2);
                failCount++;
            }
            checkEquality(b1,b2);
        }

        report("Set                         ", failCount);
    }

    private static void testFlip() {
        int failCount = 0;

        for (int i=0; i<1000; i++) {
            BitSet b1 = new BitSet();

            // Make a fairly random bitset
            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;

            for (int x=0; x<numberOfSetBits; x++)
                b1.set(generator.nextInt(highestPossibleSetBit));

            BitSet b2 = (BitSet)b1.clone();

            // Flip a random range
            int rangeStart = generator.nextInt(100);
            int rangeEnd = rangeStart + generator.nextInt(100);

            // Use the flip(int, int) call on b1
            b1.flip(rangeStart, rangeEnd);

            // Use a loop on b2
            for (int x=rangeStart; x<rangeEnd; x++)
                b2.flip(x);

            // Verify their equality
            if (!b1.equals(b2))
                failCount++;
            checkEquality(b1,b2);
        }

        report("Flip                        ", failCount);
    }

    private static void testGet() {
        int failCount = 0;

        for (int i=0; i<1000; i++) {
            BitSet b1 = new BitSet();

            // Make a fairly random bitset
            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;

            for (int x=0; x<numberOfSetBits; x++)
                b1.set(generator.nextInt(highestPossibleSetBit));

            // Get a new set from a random range
            int rangeStart = generator.nextInt(100);
            int rangeEnd = rangeStart + generator.nextInt(100);

            BitSet b2 = b1.get(rangeStart, rangeEnd);

            BitSet b3 = new BitSet();
            for(int x=rangeStart; x<rangeEnd; x++)
                b3.set(x-rangeStart, b1.get(x));

            // Verify their equality
            if (!b2.equals(b3)) {
                System.out.println("start="+rangeStart);
                System.out.println("end="+rangeEnd);
                System.out.println(b1);
                System.out.println(b2);
                System.out.println(b3);
                failCount++;
            }
            checkEquality(b2,b3);
        }

        report("Get                         ", failCount);
    }


    private static void testIntersects() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);
            BitSet b2 = new BitSet(256);

            // Set some random bits in first set
            int nextBitToSet = 0;
            for (int x=0; x<30; x++) {
                nextBitToSet = generator.nextInt(255);
                b1.set(nextBitToSet);
            }

            // Set more random bits in second set
            for (int x=0; x<30; x++) {
                nextBitToSet = generator.nextInt(255);
                b2.set(nextBitToSet);
            }

            // Make sure they intersect
            nextBitToSet = generator.nextInt(255);
            b1.set(nextBitToSet);
            b2.set(nextBitToSet);

            if (!b1.intersects(b2))
                failCount++;

            // Remove the common set bits
            b1.andNot(b2);

            // Make sure they don't intersect
            if (b1.intersects(b2))
                failCount++;

            checkSanity(b1, b2);
        }

        report("Intersects                  ", failCount);
    }

    private static void testCardinality() {
        int failCount = 0;

        for (int i=0; i<100; i++) {
            BitSet b1 = new BitSet(256);

            // Set a random number of increasing bits
            int nextBitToSet = 0;
            int iterations = generator.nextInt(20)+1;
            for (int x=0; x<iterations; x++) {
                nextBitToSet += generator.nextInt(20)+1;
                b1.set(nextBitToSet);
            }

            if (b1.cardinality() != iterations) {
                System.out.println("Iterations is "+iterations);
                System.out.println("Cardinality is "+b1.cardinality());
                failCount++;
            }

            checkSanity(b1);
        }

        report("Cardinality                 ", failCount);
    }

    private static void testEmpty() {
        int failCount = 0;

        BitSet b1 = new BitSet();
        if (!b1.isEmpty())
            failCount++;

        int nextBitToSet = 0;
        int numberOfSetBits = generator.nextInt(100) + 1;
        int highestPossibleSetBit = generator.nextInt(1000) + 1;
        for (int x=0; x<numberOfSetBits; x++) {
            nextBitToSet = generator.nextInt(highestPossibleSetBit);
            b1.set(nextBitToSet);
            if (b1.isEmpty())
                failCount++;
            b1.clear(nextBitToSet);
            if (!b1.isEmpty())
                failCount++;
        }

        report("Empty                       ", failCount);
    }

    private static void testEmpty2() {
        {BitSet t = new BitSet(); t.set(100); t.clear(3,600); checkEmpty(t);}
        checkEmpty(new BitSet(0));
        checkEmpty(new BitSet(342));
        BitSet s = new BitSet(0);
        checkEmpty(s);
        s.clear(92);      checkEmpty(s);
        s.clear(127,127); checkEmpty(s);
        s.set(127,127);   checkEmpty(s);
        s.set(128,128);   checkEmpty(s);
        BitSet empty = new BitSet();
        {BitSet t = new BitSet(); t.and   (empty);     checkEmpty(t);}
        {BitSet t = new BitSet(); t.or    (empty);     checkEmpty(t);}
        {BitSet t = new BitSet(); t.xor   (empty);     checkEmpty(t);}
        {BitSet t = new BitSet(); t.andNot(empty);     checkEmpty(t);}
        {BitSet t = new BitSet(); t.and   (t);         checkEmpty(t);}
        {BitSet t = new BitSet(); t.or    (t);         checkEmpty(t);}
        {BitSet t = new BitSet(); t.xor   (t);         checkEmpty(t);}
        {BitSet t = new BitSet(); t.andNot(t);         checkEmpty(t);}
        {BitSet t = new BitSet(); t.and(makeSet(1));   checkEmpty(t);}
        {BitSet t = new BitSet(); t.and(makeSet(127)); checkEmpty(t);}
        {BitSet t = new BitSet(); t.and(makeSet(128)); checkEmpty(t);}
        {BitSet t = new BitSet(); t.flip(7);t.flip(7); checkEmpty(t);}
        {BitSet t = new BitSet(); checkEmpty(t.get(200,300));}
        {BitSet t = makeSet(2,5); check(t.get(2,6).equals(makeSet(0,3)),"");}
    }

    private static void testToString() {
        check(new BitSet().toString().equals("{}"));
        check(makeSet(2,3,42,43,234).toString().equals("{2, 3, 42, 43, 234}"));

        final long MB = 1024*1024;
        if (Runtime.getRuntime().maxMemory() >= 512*MB) {
            // only run it if we have enough memory
            try {
                check(makeSet(Integer.MAX_VALUE-1).toString().equals(
                        "{" + (Integer.MAX_VALUE-1) + "}"));
                check(makeSet(Integer.MAX_VALUE).toString().equals(
                        "{" + Integer.MAX_VALUE + "}"));
                check(makeSet(0, 1, Integer.MAX_VALUE-1, Integer.MAX_VALUE).toString().equals(
                        "{0, 1, " + (Integer.MAX_VALUE-1) + ", " + Integer.MAX_VALUE + "}"));
            } catch (IndexOutOfBoundsException exc) {
                fail("toString() with indices near MAX_VALUE");
            }
        }
    }

    private static void testLogicalIdentities() {
        int failCount = 0;

        // Verify that (!b1)|(!b2) == !(b1&b2)
        for (int i=0; i<50; i++) {
            // Construct two fairly random bitsets
            BitSet b1 = new BitSet();
            BitSet b2 = new BitSet();

            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;

            for (int x=0; x<numberOfSetBits; x++) {
                b1.set(generator.nextInt(highestPossibleSetBit));
                b2.set(generator.nextInt(highestPossibleSetBit));
            }

            BitSet b3 = (BitSet) b1.clone();
            BitSet b4 = (BitSet) b2.clone();

            for (int x=0; x<highestPossibleSetBit; x++) {
                b1.flip(x);
                b2.flip(x);
            }
            b1.or(b2);
            b3.and(b4);
            for (int x=0; x<highestPossibleSetBit; x++)
                b3.flip(x);
            if (!b1.equals(b3))
                failCount++;
            checkSanity(b1, b2, b3, b4);
        }

        // Verify that (b1&(!b2)|(b2&(!b1) == b1^b2
        for (int i=0; i<50; i++) {
            // Construct two fairly random bitsets
            BitSet b1 = new BitSet();
            BitSet b2 = new BitSet();

            int numberOfSetBits = generator.nextInt(100) + 1;
            int highestPossibleSetBit = generator.nextInt(1000) + 1;

            for (int x=0; x<numberOfSetBits; x++) {
                b1.set(generator.nextInt(highestPossibleSetBit));
                b2.set(generator.nextInt(highestPossibleSetBit));
            }

            BitSet b3 = (BitSet) b1.clone();
            BitSet b4 = (BitSet) b2.clone();
            BitSet b5 = (BitSet) b1.clone();
            BitSet b6 = (BitSet) b2.clone();

            for (int x=0; x<highestPossibleSetBit; x++)
                b2.flip(x);
            b1.and(b2);
            for (int x=0; x<highestPossibleSetBit; x++)
                b3.flip(x);
            b3.and(b4);
            b1.or(b3);
            b5.xor(b6);
            if (!b1.equals(b5))
                failCount++;
            checkSanity(b1, b2, b3, b4, b5, b6);
        }
        report("Logical Identities          ", failCount);
    }

}
