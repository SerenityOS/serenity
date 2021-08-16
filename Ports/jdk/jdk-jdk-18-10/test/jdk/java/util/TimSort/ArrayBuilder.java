/*
 * Copyright 2009 Google Inc.  All Rights Reserved.
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

import java.util.Random;
import java.math.BigInteger;

public enum ArrayBuilder {

    // These seven are from  Tim's paper (listsort.txt)

    RANDOM_INT {
        public Object[] build(int len) {
            Integer[] result = new Integer[len];
            for (int i = 0; i < len; i++)
                result[i] = rnd.nextInt();
            return result;
        }
    },

    DESCENDING_INT {
        public Object[] build(int len) {
            Integer[] result = new Integer[len];
            for (int i = 0; i < len; i++)
                result[i] = len - i;
            return result;
        }
    },

    ASCENDING_INT {
        public Object[] build(int len) {
            Integer[] result = new Integer[len];
            for (int i = 0; i < len; i++)
                result[i] = i;
            return result;
        }
    },

    ASCENDING_3_RND_EXCH_INT {
        public Object[] build(int len) {
            Integer[] result = new Integer[len];
            for (int i = 0; i < len; i++)
                result[i] = i;
            for (int i = 0; i < 3; i++)
                swap(result, rnd.nextInt(result.length),
                             rnd.nextInt(result.length));
            return result;
        }
    },

    ASCENDING_10_RND_AT_END_INT {
        public Object[] build(int len) {
            Integer[] result = new Integer[len];
            int endStart = len - 10;
            for (int i = 0; i < endStart; i++)
                result[i] = i;
            for (int i = endStart; i < len; i++)
                result[i] = rnd.nextInt(endStart + 10);
            return result;
        }
    },

    ALL_EQUAL_INT {
        public Object[] build(int len) {
            Integer[] result = new Integer[len];
            for (int i = 0; i < len; i++)
                result[i] = 666;
            return result;
        }
    },

    DUPS_GALORE_INT {
        public Object[] build(int len) {
            Integer[] result = new Integer[len];
            for (int i = 0; i < len; i++)
                result[i] = rnd.nextInt(4);
            return result;
        }
    },

    RANDOM_WITH_DUPS_INT {
        public Object[] build(int len) {
            Integer[] result = new Integer[len];
            for (int i = 0; i < len; i++)
                result[i] = rnd.nextInt(len);
            return result;
        }
    },

    PSEUDO_ASCENDING_STRING {
        public String[] build(int len) {
            String[] result = new String[len];
            for (int i = 0; i < len; i++)
                result[i] = Integer.toString(i);
            return result;
        }
    },

    RANDOM_BIGINT {
        public BigInteger[] build(int len) {
            BigInteger[] result = new BigInteger[len];
            for (int i = 0; i < len; i++)
                result[i] = HUGE.add(BigInteger.valueOf(rnd.nextInt(len)));
            return result;
        }
    };

    public abstract Object[] build(int len);

    public void resetRandom() {
        rnd = new Random(666);
    }

    private static Random rnd = new Random(666);

    private static void swap(Object[] a, int i, int j) {
        Object t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    private static BigInteger HUGE = BigInteger.ONE.shiftLeft(100);
}
