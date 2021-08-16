/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
package gc.hashcode;

import java.util.ArrayList;
import java.util.Random;

/**
 * Helper class for the hash code tests.
 */
public final class HCHelper {

    /**
     * Evacuation list 0 constant.
     */
    public static final int EVAC_LIST_0 = 0;
    /**
     * Evacuation list 1 constant.
     */
    public static final int EVAC_LIST_1 = 1;
    /**
     * Evacuation list 2 constant.
     */
    public static final int EVAC_LIST_2 = 2;
    /**
     * Evacuation list 3 constant.
     */
    public static final int EVAC_LIST_3 = 3;
    /**
     * Evacuation list 4 constant.
     */
    public static final int EVAC_LIST_4 = 4;
    /**
     * Evacuation list 5 constant.
     */
    public static final int EVAC_LIST_5 = 5;
    /**
     * Evacuation list 0 percentage constant.
     */
    public static final double EVAC_SIZE_0 = 0.50;
    /**
     * Evacuation list 1 percentage constant.
     */
    public static final double EVAC_SIZE_1 = 0.14;
    /**
     * Evacuation list 2 percentage constant.
     */
    public static final double EVAC_SIZE_2 = 0.12;
    /**
     * Evacuation list 3 percentage constant.
     */
    public static final double EVAC_SIZE_3 = 0.10;
    /**
     * Evacuation list 4 percentage constant.
     */
    public static final double EVAC_SIZE_4 = 0.07;
    /**
     * Evacuation list 5 percentage constant.
     */
    public static final double EVAC_SIZE_5 = 0.05;

    /**
     * Helper class that allocates memory and also tracks the original
     * as well as current hash code.
     */
    final class AllocObject {
        private byte[] allocatedArray;
        private int hashValue;

        /**
         * Create a new allocator object that allocates size bytes.
         *
         * @param size Number of bytes to allocate.
         */
        AllocObject(int size) {
            allocatedArray = new byte[size];
            hashValue = allocatedArray.hashCode();
        }

        /**
         * Get the stored hash code value.
         *
         * @return Stored hash code.
         */
        int getStoredHashValue() {
            return hashValue;
        }

        /**
         * Get the current hash code value.
         *
         * @return Current hash code.
         */
        int getCurrentHashValue() {
            return allocatedArray.hashCode();
        }

        /**
         * Get the size of the allocated object.
         *
         * @return Size of allocated object.
         */
        int getAllocatedSize() {
            return allocatedArray.length;
        }
    }

    /**
     * Helper class that holds all the allocation lists.
     */
    final class AllocInfo {
        private long allocatedSize;
        private long numOfAllocedObjs;
        private ArrayList safeList;
        private ArrayList allocList;
        private ArrayList evacList0;
        private ArrayList evacList1;
        private ArrayList evacList2;
        private ArrayList evacList3;
        private ArrayList evacList4;
        private ArrayList evacList5;

        /**
         * Create the helper object.
         */
        AllocInfo() {
            allocatedSize = 0;
            numOfAllocedObjs = 0;
            safeList = new ArrayList();
            allocList = new ArrayList();
            evacList0 = new ArrayList();
            evacList1 = new ArrayList();
            evacList2 = new ArrayList();
            evacList3 = new ArrayList();
            evacList4 = new ArrayList();
            evacList5 = new ArrayList();
        }

        /**
         * Get the amount of memory allocated in total.
         *
         * @return Total allocated size.
         */
        public long getAllocatedSize() {
            return allocatedSize;
        }

        /**
         * Set the amount of memory allocated in total.
         *
         * @param allocatedSize Total allocated size.
         */
        public void setAllocatedSize(long allocatedSize) {
            this.allocatedSize = allocatedSize;
        }

        /**
         * Get total number of objects allocated.
         *
         * @return Number of objects allocated.
         */
        public long getNumOfAllocedObjs() {
            return numOfAllocedObjs;
        }

        /**
         * Set total number of objects allocated.
         *
         * @param numOfAllocedObjs Number of objects allocated.
         */
        public void setNumOfAllocedObjs(long numOfAllocedObjs) {
            this.numOfAllocedObjs = numOfAllocedObjs;
        }

        /**
         * Increase the number of objects allocated.
         */
        public void incNumOfAllocedObjs() {
            numOfAllocedObjs++;
        }

        /**
         * Decrease the number of objects allocated.
         */
        public void decNumOfAllocedObjs() {
            numOfAllocedObjs--;
        }

        /**
         * Get the safe list.
         *
         * @return ArrayList that contains the safe list.
         */
        public ArrayList getSafeList() {
            return safeList;
        }

        /**
         * Get the alloc list.
         *
         * @return ArrayList that contains the alloc list.
         */
        public ArrayList getAllocList() {
            return allocList;
        }

        /**
         * Get evacuation list 0.
         *
         * @return ArrayList that contains evacuation list 0.
         */
        public ArrayList getEvacList0() {
            return evacList0;
        }

        /**
         * Get evacuation list 1.
         *
         * @return ArrayList that contains evacuation list 1.
         */
        public ArrayList getEvacList1() {
            return evacList1;
        }

        /**
         * Get evacuation list 2.
         *
         * @return ArrayList that contains evacuation list 2.
         */
        public ArrayList getEvacList2() {
            return evacList2;
        }

        /**
         * Get evacuation list 3.
         *
         * @return ArrayList that contains evacuation list 3.
         */
        public ArrayList getEvacList3() {
            return evacList3;
        }

        /**
         * Get evacuation list 4.
         *
         * @return ArrayList that contains evacuation list 4.
         */
        public ArrayList getEvacList4() {
            return evacList4;
        }

        /**
         * Get evacuation list 5.
         *
         * @return ArrayList that contains evacuation list 5.
         */
        public ArrayList getEvacList5() {
            return evacList5;
        }
    }


    private int minSize;
    private int maxSize;
    private double percentToFill;
    private int allocTrigSize;
    private AllocInfo ai;
    private Random rnd;

    private long sizeLimit0;
    private long sizeLimit1;
    private long sizeLimit2;
    private long sizeLimit3;
    private long sizeLimit4;
    private long sizeLimit5;

    /**
     * Create the helper class.
     *
     * @param minSize Minimum size of objects to allocate.
     * @param maxSize Maximum size of objects to allocate.
     * @param seed Random seed to use.
     * @param percentToFill Percentage of the heap to fill.
     * @param allocTrigSize Object size to use when triggering a GC.
     */
    public HCHelper(int minSize, int maxSize, long seed,
                    double percentToFill, int allocTrigSize) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.percentToFill = percentToFill;
        this.allocTrigSize = allocTrigSize;
        ai = new AllocInfo();
        rnd = new Random(seed);

        sizeLimit0 = 0;
        sizeLimit1 = 0;
        sizeLimit2 = 0;
        sizeLimit3 = 0;
        sizeLimit4 = 0;
        sizeLimit5 = 0;
    }

    /**
     * Setup all the evacuation lists and fill them with objects.
     */
    public void setupLists() {
        Runtime r = Runtime.getRuntime();
        long maxMem = r.maxMemory();
        long safeMaxMem = (long) (maxMem * percentToFill);
        sizeLimit0 = (long) (safeMaxMem * EVAC_SIZE_0);
        sizeLimit1 = (long) (safeMaxMem * EVAC_SIZE_1);
        sizeLimit2 = (long) (safeMaxMem * EVAC_SIZE_2);
        sizeLimit3 = (long) (safeMaxMem * EVAC_SIZE_3);
        sizeLimit4 = (long) (safeMaxMem * EVAC_SIZE_4);
        sizeLimit5 = (long) (safeMaxMem * EVAC_SIZE_5);

        // Fill the memory with objects
        System.gc();
        allocObjects(ai.getEvacList0(), sizeLimit0);
        System.gc();
        allocObjects(ai.getEvacList1(), sizeLimit1);
        System.gc();
        allocObjects(ai.getEvacList2(), sizeLimit2);
        System.gc();
        allocObjects(ai.getEvacList3(), sizeLimit3);
        System.gc();
        allocObjects(ai.getEvacList4(), sizeLimit4);
        System.gc();
        allocObjects(ai.getEvacList5(), sizeLimit5);
        System.gc();
    }

    private void allocObjects(ArrayList al, long totalSizeLimit) {
        long allocedSize = 0;
        int multiplier = maxSize - minSize;

        while (allocedSize < totalSizeLimit) {
            int allocSize = minSize + (int) (rnd.nextDouble() * multiplier);
            if (allocSize >= totalSizeLimit - allocedSize) {
                allocSize = (int) (totalSizeLimit - allocedSize);
            }

            al.add(new AllocObject(allocSize));
            allocedSize += allocSize;
        }
    }

    /**
     * Free all objects in a specific evacuation list.
     *
     * @param listNr The evacuation list to clear. Must be between 0 and 5.
     */
    public void clearList(int listNr) {
        if (listNr < EVAC_LIST_0 || listNr > EVAC_LIST_5) {
            throw new IllegalArgumentException("List to removed bust be "
                    + "between EVAC_LIST_0 and EVAC_LIST_5");
        }

        switch (listNr) {
            case EVAC_LIST_0:
                ai.getEvacList0().clear();
                break;
            case EVAC_LIST_1:
                ai.getEvacList1().clear();
                break;
            case EVAC_LIST_2:
                ai.getEvacList2().clear();
                break;
            case EVAC_LIST_3:
                ai.getEvacList3().clear();
                break;
            case EVAC_LIST_4:
                ai.getEvacList4().clear();
                break;
            case EVAC_LIST_5:
                ai.getEvacList5().clear();
                break;
            default: // Should never occur, since we test the listNr param
                break;
        }
    }

    /**
     * Verify the hash codes for a list of AllocObject:s.
     *
     * @param objList ArrayList containing AllocObject:s
     * @return true if all hash codes are OK, otherwise false
     */
    boolean verifyHashCodes(ArrayList objList) {
        // Check the hash values
        for (int i = 0; i < objList.size(); i++) {
            AllocObject tmp = (AllocObject) objList.get(i);
            if (tmp.getStoredHashValue() != tmp.getCurrentHashValue()) {
                // At least one of the hash values mismatch, so the test failed
                return false;
            }
        }

        return true;
    }


    /**
     * Verify the hash codes for all objects in all the lists.
     *
     * @return Success if all hash codes matches the original hash codes.
     */
    public boolean verifyHashCodes() {
        return verifyHashCodes(ai.getAllocList())
                && verifyHashCodes(ai.getSafeList())
                && verifyHashCodes(ai.getEvacList0())
                && verifyHashCodes(ai.getEvacList1())
                && verifyHashCodes(ai.getEvacList2())
                && verifyHashCodes(ai.getEvacList3())
                && verifyHashCodes(ai.getEvacList4())
                && verifyHashCodes(ai.getEvacList5());
    }

    /**
     * Free all allocated objects from all the lists.
     */
    public void cleanupLists() {
        ai.getAllocList().clear();
        ai.getSafeList().clear();

        ai.getEvacList0().clear();
        ai.getEvacList1().clear();
        ai.getEvacList2().clear();
        ai.getEvacList3().clear();
        ai.getEvacList4().clear();
        ai.getEvacList5().clear();
    }

    /**
     * Get the size of evacuation list 0.
     *
     * @return Size of evacuation list 0.
     */
    public long getEvac0Size() {
        return sizeLimit0;
    }

    /**
     * Get the size of evacuation list 1.
     *
     * @return Size of evacuation list 1.
     */
    public long getEvac1Size() {
        return sizeLimit1;
    }

    /**
     * Get the size of evacuation list 2.
     *
     * @return Size of evacuation list 2.
     */
    public long getEvac2Size() {
        return sizeLimit2;
    }

    /**
     * Get the size of evacuation list 3.
     *
     * @return Size of evacuation list 3.
     */
    public long getEvac3Size() {
        return sizeLimit3;
    }

    /**
     * Get the size of evacuation list 4.
     *
     * @return Size of evacuation list 4.
     */
    public long getEvac4Size() {
        return sizeLimit4;
    }

    /**
     * Get the size of evacuation list 5.
     *
     * @return Size of evacuation list 5.
     */
    public long getEvac5Size() {
        return sizeLimit5;
    }
}
