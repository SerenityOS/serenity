/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.tools.javac.util;

import java.util.Arrays;

/** A class for extensible, mutable bit sets.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Bits {

    //       ____________      reset    _________
    //      /  UNKNOWN   \   <-------- / UNINIT  \
    //      \____________/       |     \_________/
    //            |              |          |
    //            |assign        |          | any
    //            |        ___________      |
    //            ------> /  NORMAL   \ <----
    //                    \___________/     |
    //                            |         |
    //                            |         |
    //                            -----------
    //                               any
    protected enum BitsState {
        /*  A Bits instance is in UNKNOWN state if it has been explicitly reset.
         *  It is possible to get to this state from any other by calling the
         *  reset method. An instance in the UNKNOWN state can pass to the
         *  NORMAL state after being assigned another Bits instance.
         *
         *  Bits instances are final fields in Flow so the UNKNOWN state models
         *  the null assignment.
         */
        UNKNOWN,
        /*  A Bits instance is in UNINIT when it is created with the default
         *  constructor but it isn't explicitly reset. The main objective of this
         *  internal state is to save some memory.
         */
        UNINIT,
        /*  The normal state is reached after creating a Bits instance from an
         *  existing one or after applying any operation to an instance on UNINIT
         *  or NORMAL state. From this state a bits instance can pass to the
         *  UNKNOWN state by calling the reset method.
         */
        NORMAL;

        static BitsState getState(int[] someBits, boolean reset) {
            if (reset) {
                return UNKNOWN;
            } else {
                if (someBits != unassignedBits) {
                    return NORMAL;
                } else {
                    return UNINIT;
                }
            }
        }

    }

    private static final int wordlen = 32;
    private static final int wordshift = 5;
    private static final int wordmask = wordlen - 1;

    public int[] bits = null;
    // This field will store last version of bits after every change.
    private static final int[] unassignedBits = new int[0];

    protected BitsState currentState;

    /** Construct an initially empty set.
     */
    public Bits() {
        this(false);
    }

    public Bits(Bits someBits) {
        this(someBits.dup().bits, BitsState.getState(someBits.bits, false));
    }

    public Bits(boolean reset) {
        this(unassignedBits, BitsState.getState(unassignedBits, reset));
    }

    /** Construct a set consisting initially of given bit vector.
     */
    protected Bits(int[] bits, BitsState initState) {
        this.bits = bits;
        this.currentState = initState;
        switch (initState) {
            case UNKNOWN:
                this.bits = null;
                break;
            case NORMAL:
                Assert.check(bits != unassignedBits);
                break;
        }
    }

    protected void sizeTo(int len) {
        if (bits.length < len) {
            bits = Arrays.copyOf(bits, len);
        }
    }

    /** This set = {}.
     */
    public void clear() {
        Assert.check(currentState != BitsState.UNKNOWN);
        for (int i = 0; i < bits.length; i++) {
            bits[i] = 0;
        }
        currentState = BitsState.NORMAL;
    }

    public void reset() {
        internalReset();
    }

    protected void internalReset() {
        bits = null;
        currentState = BitsState.UNKNOWN;
    }

    public boolean isReset() {
        return currentState == BitsState.UNKNOWN;
    }

    public Bits assign(Bits someBits) {
        bits = someBits.dup().bits;
        currentState = BitsState.NORMAL;
        return this;
    }

    /** Return a copy of this set.
     */
    public Bits dup() {
        Assert.check(currentState != BitsState.UNKNOWN);
        Bits tmp = new Bits();
        tmp.bits = dupBits();
        currentState = BitsState.NORMAL;
        return tmp;
    }

    protected int[] dupBits() {
        int [] result;
        if (currentState != BitsState.NORMAL) {
            result = bits;
        } else {
            result = new int[bits.length];
            System.arraycopy(bits, 0, result, 0, bits.length);
        }
        return result;
    }

    /** Include x in this set.
     */
    public void incl(int x) {
        Assert.check(currentState != BitsState.UNKNOWN);
        Assert.check(x >= 0);
        sizeTo((x >>> wordshift) + 1);
        bits[x >>> wordshift] = bits[x >>> wordshift] |
            (1 << (x & wordmask));
        currentState = BitsState.NORMAL;
    }


    /** Include [start..limit) in this set.
     */
    public void inclRange(int start, int limit) {
        Assert.check(currentState != BitsState.UNKNOWN);
        sizeTo((limit >>> wordshift) + 1);
        for (int x = start; x < limit; x++) {
            bits[x >>> wordshift] = bits[x >>> wordshift] |
                (1 << (x & wordmask));
        }
        currentState = BitsState.NORMAL;
    }

    /** Exclude [start...end] from this set.
     */
    public void excludeFrom(int start) {
        Assert.check(currentState != BitsState.UNKNOWN);
        Bits temp = new Bits();
        temp.sizeTo(bits.length);
        temp.inclRange(0, start);
        internalAndSet(temp);
        currentState = BitsState.NORMAL;
    }

    /** Exclude x from this set.
     */
    public void excl(int x) {
        Assert.check(currentState != BitsState.UNKNOWN);
        Assert.check(x >= 0);
        sizeTo((x >>> wordshift) + 1);
        bits[x >>> wordshift] = bits[x >>> wordshift] &
            ~(1 << (x & wordmask));
        currentState = BitsState.NORMAL;
    }

    /** Is x an element of this set?
     */
    public boolean isMember(int x) {
        Assert.check(currentState != BitsState.UNKNOWN);
        return
            0 <= x && x < (bits.length << wordshift) &&
            (bits[x >>> wordshift] & (1 << (x & wordmask))) != 0;
    }

    /** {@literal this set = this set & xs}.
     */
    public Bits andSet(Bits xs) {
        Assert.check(currentState != BitsState.UNKNOWN);
        internalAndSet(xs);
        currentState = BitsState.NORMAL;
        return this;
    }

    protected void internalAndSet(Bits xs) {
        Assert.check(currentState != BitsState.UNKNOWN);
        sizeTo(xs.bits.length);
        for (int i = 0; i < xs.bits.length; i++) {
            bits[i] = bits[i] & xs.bits[i];
        }
    }

    /** this set = this set | xs.
     */
    public Bits orSet(Bits xs) {
        Assert.check(currentState != BitsState.UNKNOWN);
        sizeTo(xs.bits.length);
        for (int i = 0; i < xs.bits.length; i++) {
            bits[i] = bits[i] | xs.bits[i];
        }
        currentState = BitsState.NORMAL;
        return this;
    }

    /** this set = this set \ xs.
     */
    public Bits diffSet(Bits xs) {
        Assert.check(currentState != BitsState.UNKNOWN);
        for (int i = 0; i < bits.length; i++) {
            if (i < xs.bits.length) {
                bits[i] = bits[i] & ~xs.bits[i];
            }
        }
        currentState = BitsState.NORMAL;
        return this;
    }

    /** this set = this set ^ xs.
     */
    public Bits xorSet(Bits xs) {
        Assert.check(currentState != BitsState.UNKNOWN);
        sizeTo(xs.bits.length);
        for (int i = 0; i < xs.bits.length; i++) {
            bits[i] = bits[i] ^ xs.bits[i];
        }
        currentState = BitsState.NORMAL;
        return this;
    }

    /** Count trailing zero bits in an int. Algorithm from "Hacker's
     *  Delight" by Henry S. Warren Jr. (figure 5-13)
     */
    private static int trailingZeroBits(int x) {
        Assert.check(wordlen == 32);
        if (x == 0) {
            return 32;
        }
        int n = 1;
        if ((x & 0xffff) == 0) { n += 16; x >>>= 16; }
        if ((x & 0x00ff) == 0) { n +=  8; x >>>=  8; }
        if ((x & 0x000f) == 0) { n +=  4; x >>>=  4; }
        if ((x & 0x0003) == 0) { n +=  2; x >>>=  2; }
        return n - (x&1);
    }

    /** Return the index of the least bit position &ge; x that is set.
     *  If none are set, returns -1.  This provides a nice way to iterate
     *  over the members of a bit set:
     *  <pre>{@code
     *  for (int i = bits.nextBit(0); i>=0; i = bits.nextBit(i+1)) ...
     *  }</pre>
     */
    public int nextBit(int x) {
        Assert.check(currentState != BitsState.UNKNOWN);
        int windex = x >>> wordshift;
        if (windex >= bits.length) {
            return -1;
        }
        int word = bits[windex] & ~((1 << (x & wordmask))-1);
        while (true) {
            if (word != 0) {
                return (windex << wordshift) + trailingZeroBits(word);
            }
            windex++;
            if (windex >= bits.length) {
                return -1;
            }
            word = bits[windex];
        }
    }

    /** a string representation of this set.
     */
    @Override
    public String toString() {
        if (bits != null && bits.length > 0) {
            char[] digits = new char[bits.length * wordlen];
            for (int i = 0; i < bits.length * wordlen; i++) {
                digits[i] = isMember(i) ? '1' : '0';
            }
            return new String(digits);
        } else {
            return "[]";
        }
    }

}
