/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

/* ****************************************************************
 ******************************************************************
 ******************************************************************
 *** COPYRIGHT (c) Eastman Kodak Company, 1997
 *** As  an unpublished  work pursuant to Title 17 of the United
 *** States Code.  All rights reserved.
 ******************************************************************
 ******************************************************************
 ******************************************************************/

package java.awt.image;

import static sun.java2d.StateTrackable.State.*;

/**
 * This class extends {@code DataBuffer} and stores data internally as bytes.
 * Values stored in the byte array(s) of this {@code DataBuffer} are treated as
 * unsigned values.
 * <p>
 * <a id="optimizations">
 * Note that some implementations may function more efficiently
 * if they can maintain control over how the data for an image is
 * stored.
 * For example, optimizations such as caching an image in video
 * memory require that the implementation track all modifications
 * to that data.
 * Other implementations may operate better if they can store the
 * data in locations other than a Java array.
 * To maintain optimum compatibility with various optimizations
 * it is best to avoid constructors and methods which expose the
 * underlying storage as a Java array, as noted below in the
 * documentation for those methods.
 * </a>
 */
public final class DataBufferByte extends DataBuffer
{
    /** The default data bank. */
    byte[] data;

    /** All data banks */
    byte[][] bankdata;

    /**
     * Constructs a byte-based {@code DataBuffer} with a single bank and the
     * specified size.
     *
     * @param size The size of the {@code DataBuffer}.
     */
    public DataBufferByte(int size) {
      super(STABLE, TYPE_BYTE, size);
      data = new byte[size];
      bankdata = new byte[1][];
      bankdata[0] = data;
    }

    /**
     * Constructs a byte based {@code DataBuffer} with the specified number of
     * banks all of which are the specified size.
     *
     * @param size The size of the banks in the {@code DataBuffer}.
     * @param numBanks The number of banks in the a {@code DataBuffer}.
     */
    public DataBufferByte(int size, int numBanks) {
        super(STABLE, TYPE_BYTE, size, numBanks);
        bankdata = new byte[numBanks][];
        for (int i= 0; i < numBanks; i++) {
            bankdata[i] = new byte[size];
        }
        data = bankdata[0];
    }

    /**
     * Constructs a byte-based {@code DataBuffer} with a single bank using the
     * specified array.
     * Only the first {@code size} elements should be used by accessors of
     * this {@code DataBuffer}.  {@code dataArray} must be large enough to
     * hold {@code size} elements.
     * <p>
     * Note that {@code DataBuffer} objects created by this constructor
     * may be incompatible with <a href="#optimizations">performance
     * optimizations</a> used by some implementations (such as caching
     * an associated image in video memory).
     *
     * @param dataArray The byte array for the {@code DataBuffer}.
     * @param size The size of the {@code DataBuffer} bank.
     */
    public DataBufferByte(byte[] dataArray, int size) {
        super(UNTRACKABLE, TYPE_BYTE, size);
        data = dataArray;
        bankdata = new byte[1][];
        bankdata[0] = data;
    }

    /**
     * Constructs a byte-based {@code DataBuffer} with a single bank using the
     * specified array, size, and offset.  {@code dataArray} must have at least
     * {@code offset} + {@code size} elements.  Only elements {@code offset}
     * through {@code offset} + {@code size} - 1
     * should be used by accessors of this {@code DataBuffer}.
     * <p>
     * Note that {@code DataBuffer} objects created by this constructor
     * may be incompatible with <a href="#optimizations">performance
     * optimizations</a> used by some implementations (such as caching
     * an associated image in video memory).
     *
     * @param dataArray The byte array for the {@code DataBuffer}.
     * @param size The size of the {@code DataBuffer} bank.
     * @param offset The offset into the {@code dataArray}. {@code dataArray}
     * must have at least {@code offset} + {@code size} elements.
     */
    public DataBufferByte(byte[] dataArray, int size, int offset){
        super(UNTRACKABLE, TYPE_BYTE, size, 1, offset);
        data = dataArray;
        bankdata = new byte[1][];
        bankdata[0] = data;
    }

    /**
     * Constructs a byte-based {@code DataBuffer} with the specified arrays.
     * The number of banks is equal to {@code dataArray.length}.
     * Only the first {@code size} elements of each array should be used by
     * accessors of this {@code DataBuffer}.
     * <p>
     * Note that {@code DataBuffer} objects created by this constructor
     * may be incompatible with <a href="#optimizations">performance
     * optimizations</a> used by some implementations (such as caching
     * an associated image in video memory).
     *
     * @param dataArray The byte arrays for the {@code DataBuffer}.
     * @param size The size of the banks in the {@code DataBuffer}.
     */
    public DataBufferByte(byte[][] dataArray, int size) {
        super(UNTRACKABLE, TYPE_BYTE, size, dataArray.length);
        bankdata = dataArray.clone();
        data = bankdata[0];
    }

    /**
     * Constructs a byte-based {@code DataBuffer} with the specified arrays, size,
     * and offsets.
     * The number of banks is equal to {@code dataArray.length}.  Each array must
     * be at least as large as {@code size} + the corresponding {@code offset}.
     * There must be an entry in the {@code offset} array for each {@code dataArray}
     * entry.  For each bank, only elements {@code offset} through
     * {@code offset} + {@code size} - 1 should be used by accessors of this
     * {@code DataBuffer}.
     * <p>
     * Note that {@code DataBuffer} objects created by this constructor
     * may be incompatible with <a href="#optimizations">performance
     * optimizations</a> used by some implementations (such as caching
     * an associated image in video memory).
     *
     * @param dataArray The byte arrays for the {@code DataBuffer}.
     * @param size The size of the banks in the {@code DataBuffer}.
     * @param offsets The offsets into each array.
     */
    public DataBufferByte(byte[][] dataArray, int size, int[] offsets) {
        super(UNTRACKABLE, TYPE_BYTE, size, dataArray.length, offsets);
        bankdata = dataArray.clone();
        data = bankdata[0];
    }

    /**
     * Returns the default (first) byte data array.
     * <p>
     * Note that calling this method may cause this {@code DataBuffer}
     * object to be incompatible with <a href="#optimizations">performance
     * optimizations</a> used by some implementations (such as caching
     * an associated image in video memory).
     *
     * @return The first byte data array.
     */
    public byte[] getData() {
        theTrackable.setUntrackable();
        return data;
    }

    /**
     * Returns the data array for the specified bank.
     * <p>
     * Note that calling this method may cause this {@code DataBuffer}
     * object to be incompatible with <a href="#optimizations">performance
     * optimizations</a> used by some implementations (such as caching
     * an associated image in video memory).
     *
     * @param bank The bank whose data array you want to get.
     * @return The data array for the specified bank.
     */
    public byte[] getData(int bank) {
        theTrackable.setUntrackable();
        return bankdata[bank];
    }

    /**
     * Returns the data arrays for all banks.
     * <p>
     * Note that calling this method may cause this {@code DataBuffer}
     * object to be incompatible with <a href="#optimizations">performance
     * optimizations</a> used by some implementations (such as caching
     * an associated image in video memory).
     *
     * @return All of the data arrays.
     */
    public byte[][] getBankData() {
        theTrackable.setUntrackable();
        return bankdata.clone();
    }

    /**
     * Returns the requested data array element from the first (default) bank.
     *
     * @param i The data array element you want to get.
     * @return The requested data array element as an integer.
     * @see #setElem(int, int)
     * @see #setElem(int, int, int)
     */
    public int getElem(int i) {
        return (int)(data[i+offset]) & 0xff;
    }

    /**
     * Returns the requested data array element from the specified bank.
     *
     * @param bank The bank from which you want to get a data array element.
     * @param i The data array element you want to get.
     * @return The requested data array element as an integer.
     * @see #setElem(int, int)
     * @see #setElem(int, int, int)
     */
    public int getElem(int bank, int i) {
        return (int)(bankdata[bank][i+offsets[bank]]) & 0xff;
    }

    /**
     * Sets the requested data array element in the first (default) bank
     * to the specified value.
     *
     * @param i The data array element you want to set.
     * @param val The integer value to which you want to set the data array element.
     * @see #getElem(int)
     * @see #getElem(int, int)
     */
    public void setElem(int i, int val) {
        data[i+offset] = (byte)val;
        theTrackable.markDirty();
    }

    /**
     * Sets the requested data array element in the specified bank
     * from the given integer.
     * @param bank The bank in which you want to set the data array element.
     * @param i The data array element you want to set.
     * @param val The integer value to which you want to set the specified data array element.
     * @see #getElem(int)
     * @see #getElem(int, int)
     */
    public void setElem(int bank, int i, int val) {
        bankdata[bank][i+offsets[bank]] = (byte)val;
        theTrackable.markDirty();
    }
}
