/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress randomness
 *
 * @summary converted from VM Testbase gc/hashcode/ExternalHashingTest.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent, jrockit]
 * VM Testbase readme:
 * DESCRIPTION
 * Test the possible interaction of external hashing and locking on object
 * headers.
 * The approach is to nearly simultaneously lock/hash a relatively small group
 * of objects. We do this repeatedly (munging), recording all hash values
 * collected therein.
 * After doing this for a large number of groups, we force a garbage collection,
 * which would change the hashCode of an object if it hasn't previously been
 * hashed. In our case, we _know_ what the previous hashcode was, so we can
 * recalculate all of their hashes and compare with the original value.
 * If any of the hashCodes hash changed, we know we have a problem.
 *
 * COMMENTS
 * This test was ported from JRockit test suite.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit gc.hashcode.ExternalHashingTest.ExternalHashingTest
 */

package gc.hashcode.ExternalHashingTest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

import jdk.test.lib.Utils;

/**
 * Test the possible interaction of external hashing and locking on object
 * headers.
 *
 * The approach is to nearly simultaneously lock/hash a relatively small group
 * of objects. We do this repeatedly (munging), recording all hash values
 * collected therein.
 *
 * After doing this for a large number of groups, we force a garbage collection,
 * which would change the hashCode of an object if it hasn't previously been
 * hashed. In our case, we _know_ what the previous hashcode was, so we can
 * recalculate all of their hashes and compare with the original value.
 *
 * If any of the hashCodes hash changed, we know we have a problem.
 */

public final class ExternalHashingTest {

    /** Random number generator. */
    static Random rand = Utils.getRandomInstance();

    /** Goes to true when the threads should start working. */
    public static volatile boolean startingGun;

    /** Goes to true when the hashing thread is done. */
    public static volatile boolean finishHashing;

    /** Goes to true when the locking thread is done. */
    public static volatile boolean finishLocking;

    /** The number of objects in each batch. */
    private static final int BATCH_SIZE = 20;

    /** This is the global list of objects that have been hashed. */
    static Vector allObjects = new Vector();

    /** This is the corresponding list of hashCodes collected. */
    static Vector allHashes = new Vector();

    /** The default milliseconds to run the program. */
    private static final long DEFAULT_DURATION = 10000;

    /** All static */
    private ExternalHashingTest() {}

    /**
     * This object holds garbage. It is a (probably unnecessary){ embellishment
     * to increase the amount of garbage created by this benchmark.
     * <p>
     * It is global to discourage optimizer from removing it.
     */
    public static Object[] garbageMonger;

    /**
     * We need a fairly short pause, since we're not using a semaphore to
     * synchronize threads.
     */
    public static void pause() {
        try {
            // Thread.sleep(100);
            Thread.yield();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Returns System.currentTimeMillis() in the Date format.
     * @return String
     */
    private static String getDateString()   {
        SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss z");
        Date date = new Date();
        date.setTime(System.currentTimeMillis());
        return df.format(date);
    }

    /**
     * Main driver.
     * @param args command line arguments aren't used.
     */
    public static void main(String[] args) {

        long timeToRun = DEFAULT_DURATION;;

        try {
            for (int i = 0; i < args.length; i++) {
                if ("-stressTime".equals(args[i])) {
                    if (i + 1 == args.length) {
                        throw new RuntimeException("Test bug: value of -stressTime option absents");
                    }
                    timeToRun = Long.parseLong(args[i + 1]);
                    if (timeToRun <= 0) {
                        throw new RuntimeException("Test bug: value of -stressTime option is not a positive number");
                    }
                    break;
                }
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Test bug: Exception occured while parsing -stressTime option's value", e);
        }

        long startTime = System.currentTimeMillis();

        System.out.println("[" + getDateString() + "] Test duration is: " + timeToRun + " ms");
        System.out.println("[" + getDateString() + "] Do munge objects...");
        while ((System.currentTimeMillis() - startTime) < timeToRun) {
            for (int i = 0; i < 100; i++) {
                mungeObjects();
            }
            System.out.println("[" + getDateString() + "] The next 100 objects are munged...");
        }

        // Force a GC (so that objects move their position)
        System.out.println("[" + getDateString() + "] Force a GC...");
        garbageMonger = null;
        System.gc();

        // Now, to check to see if hashes are correct
        System.out.println("[" + getDateString() + "] Check hash codes...");
        for (int i = 0; i < allObjects.size(); i++) {
            Object o = allObjects.elementAt(i);
            int hash = ((Integer) allHashes.elementAt(i)).intValue();

            if (o.hashCode() != hash) {
                System.out.println("Inconsistent hash code found (Object "
                         + i + " out of " + allObjects.size());
                System.out.println("Object = " + o.toString() + "; hashCode = 0x"
                         + Integer.toHexString(o.hashCode())
                         + "; expected = 0x" + Integer.toHexString(hash));
                System.exit(1);
            }
        }

        System.exit(95 /* PASSED */);
    }

    /**
     * Add a single batch of objects to the mix.
     * <p>
     * It prepares a list of candidate objects, and presents them to a
     * LockerThread and a HasherThread in randomized orders.
     * <p>
     * The two threads are launched, and control is returned to the caller after
     * they have finished their processing.
     */
    private static void mungeObjects() {

        startingGun = false;
        finishHashing = false;
        finishLocking = false;

        /* Create the list of victims. */
        Object[] candidates = new Object[BATCH_SIZE];
        for (int i = 0; i < candidates.length; i++) {
            candidates[i] = new Object();
        }

        Object[] lockedList = randomize(candidates);
        Object[] hashedList = randomize(candidates);
        int[] foundHashes = new int[BATCH_SIZE];

        // Launch the child threads
        LockerThread locker = new LockerThread(lockedList);
        Thread lockerThread = new Thread(locker);
        Thread hasherThread = new Thread(new HasherThread(hashedList,
                foundHashes));
        lockerThread.start();
        hasherThread.start();
        startingGun = true;

        while (!finishLocking || !finishHashing) {
            pause();
        }

        garbageMonger = new Object[BATCH_SIZE];
        for (int i = 0; i < BATCH_SIZE; i++) {
            /* Add all of the results of this pass to the global list. */
            allObjects.add(hashedList[i]);
            allHashes.add(Integer.valueOf(foundHashes[i]));

            /* Create even more garbage for the GC to find */
            garbageMonger[i] = new Object();
        }

        // just some noise to make sure that do-nothing code is not optimized
        // away.
        if (locker.getCount() != BATCH_SIZE) {
            throw new InternalError("should not get here");
        }
    }

    /**
     * Return the list of objects in random order
     */
    private static Object[] randomize(Object[] list) {

        Vector v = new Vector();
        for (int i = 0; i < list.length; i++) {
            v.add(list[i]);
        }

        Object[] result = new Object[list.length];
        for (int i = 0; i < list.length; i++) {
            int pos = rand.nextInt(list.length - i);
            result[i] = v.remove(pos);
        }
        return result;
    }
}

/**
 * This helper thread locks all objects in a list in a given order before
 * returning.
 */

class LockerThread implements Runnable {

    /** The list of objects to be locked. */
    Object[] theList;

    /**
     * This junk counter is an attempt to cause the contents of the synchronized
     * block not to be completely optimized away.
     */
    int count;

    /**
     * Construct a LockerThread and provide a list of objects to work with.
     * @param list the objects to lock.
     */
    LockerThread(Object[] list) {
        theList = list;
        count = 0;
    }

    /**
     * Proceed to lock the objects...
     */
    public void run() {
        // Synchronize. Wait for caller to say all is go.
        while (!ExternalHashingTest.startingGun) {
            ExternalHashingTest.pause();
        }

        // Perform the locking
        for (int i = 0; i < theList.length; i++) {
            synchronized (theList[i]) {
                count++;
            }
        }

        // Tell the caller we are done.
        ExternalHashingTest.finishLocking = true;
    }

    /**
     * Discourage compiler from removing do-nothing count field.
     * @return the number of objects locked.
     */
    public int getCount() {
        return count;
    }
}

/**
 * This helper thread hashes all objects in a list in a given order before
 * returning.
 */

class HasherThread implements Runnable {

    /** The list of objects to be hashed. */
    Object[] theList;

    /** The list of hash codes. */
    int[] theHashes;

    /**
     * Construct a HasherThread and provide a list of objects to work with.
     * @param list the objects to hash.
     * @param hashes for storing the hash values.
     */
    HasherThread(Object[] list, int[] hashes) {
        theList = list;
        theHashes = hashes;
    }

    /**
     * Proceed to hash the objects.
     */
    public void run() {
        // Synchronize. Wait for caller to say all is go.
        while (!ExternalHashingTest.startingGun) {
            ExternalHashingTest.pause();
        }

        // Perform the hashing (collect for the caller)
        for (int i = 0; i < theList.length; i++) {
            theHashes[i] = theList[i].hashCode();
        }
        // Tell the caller we are done.
        ExternalHashingTest.finishHashing = true;
    }
}
