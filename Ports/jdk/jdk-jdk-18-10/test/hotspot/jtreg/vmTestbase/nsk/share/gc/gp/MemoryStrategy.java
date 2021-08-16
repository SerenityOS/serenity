/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.gc.gp;

import nsk.share.TestBug;

/**
 * This class encapsulates notions like "many objects of small size",
 * "small number of objects of big size".
 */
public abstract class MemoryStrategy {
        private static int smallNumber = 100;
        public abstract long getSize(long memory);
        public abstract long getSize(long memory, long objectExtra);
        public abstract int getCount(long memory);
        public abstract int getCount(long memory, long objectExtra);

        protected MemoryStrategy() {
        }

        /**
         * Small object size, big number of objects.
         */
        public static final MemoryStrategy LOW = new MemoryStrategy() {
                public long getSize(long memory) {
                        return getSize(memory, 0);
                }

                public long getSize(long memory, long objectExtra) {
                        return smallNumber;
                }

                public int getCount(long memory) {
                        return getCount(memory, 0);
                }

                public int getCount(long memory, long objectExtra) {
                        return (int) Math.min(Integer.MAX_VALUE, memory / (getSize(memory) + objectExtra));
                }

                public String toString() {
                        return "low";
                }
        };

        /**
         * Medium object size, medium number of objects.
         */
        public static final MemoryStrategy MEDIUM = new MemoryStrategy() {
                public long getSize(long memory) {
                        return getSize(memory, 0);
                }

                public long getSize(long memory, long objectExtra) {
                        return Math.round(Math.floor(Math.sqrt(memory)));
                }

                public int getCount(long memory) {
                        return getCount(memory, 0);
                }

                public int getCount(long memory, long objectExtra) {
                        return (int) Math.min(Integer.MAX_VALUE, memory / (getSize(memory) + objectExtra));
                }

                public String toString() {
                        return "medium";
                }
        };

        /**
         * Big object size, small number of objects.
         */
        public static final MemoryStrategy HIGH = new MemoryStrategy() {
                public long getSize(long memory) {
                        return getSize(memory, 0);
                }

                public long getSize(long memory, long objectExtra) {
                        return memory / getCount(memory, objectExtra);
                }

                public int getCount(long memory) {
                        return getCount(memory, 0);
                }

                public int getCount(long memory, long objectExtra) {
                        return smallNumber;
                }

                public String toString() {
                        return "high";
                }
        };

        /**
         * Get memory strategy by identifier.
         *
         * @param id identifier
         * @return memory strategy for this identifier
         * @throws TestBug if id is invalid
         */
        public static MemoryStrategy fromString(String id) {
                if (id.equalsIgnoreCase("low"))
                        return LOW;
                else if (id.equalsIgnoreCase("medium"))
                        return MEDIUM;
                else if (id.equalsIgnoreCase("high"))
                        return HIGH;
                else
                        throw new TestBug("Unknown memory strategy identifier: " + id);
        }
}
