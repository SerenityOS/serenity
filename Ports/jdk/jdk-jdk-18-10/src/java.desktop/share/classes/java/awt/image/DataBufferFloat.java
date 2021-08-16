/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.image;

import static sun.java2d.StateTrackable.State.*;

/**
 * This class extends {@code DataBuffer} and stores data internally
 * in {@code float} form.
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
 * underlying storage as a Java array as noted below in the
 * documentation for those methods.
 * </a>
 *
 * @since 1.4
 */

public final class DataBufferFloat extends DataBuffer {

    /** The array of data banks. */
    float[][] bankdata;

    /** A reference to the default data bank. */
    float[] data;

    /**
     * Constructs a {@code float}-based {@code DataBuffer}
     * with a specified size.
     *
     * @param size The number of elements in the DataBuffer.
     */
    public DataBufferFloat(int size) {
        super(STABLE, TYPE_FLOAT, size);
        data = new float[size];
        bankdata = new float[1][];
        bankdata[0] = data;
    }

    /**
     * Constructs a {@code float}-based {@code DataBuffer}
     * with a specified number of banks, all of which are of a
     * specified size.
     *
     * @param size The number of elements in each bank of the
     * {@code DataBuffer}.
     * @param numBanks The number of banks in the
     *        {@code DataBuffer}.
     */
    public DataBufferFloat(int size, int numBanks) {
        super(STABLE, TYPE_FLOAT, size, numBanks);
        bankdata = new float[numBanks][];
        for (int i= 0; i < numBanks; i++) {
            bankdata[i] = new float[size];
        }
        data = bankdata[0];
    }

    /**
     * Constructs a {@code float}-based {@code DataBuffer}
     * with the specified data array.  Only the first
     * {@code size} elements are available for use by this
     * {@code DataBuffer}.  The array must be large enough to
     * hold {@code size} elements.
     * <p>
     * Note that {@code DataBuffer} objects created by this constructor
     * may be incompatible with <a href="#optimizations">performance
     * optimizations</a> used by some implementations (such as caching
     * an associated image in video memory).
     *
     * @param dataArray An array of {@code float}s to be used as the
     *                  first and only bank of this {@code DataBuffer}.
     * @param size The number of elements of the array to be used.
     */
    public DataBufferFloat(float[] dataArray, int size) {
        super(UNTRACKABLE, TYPE_FLOAT, size);
        data = dataArray;
        bankdata = new float[1][];
        bankdata[0] = data;
    }

    /**
     * Constructs a {@code float}-based {@code DataBuffer}
     * with the specified data array.  Only the elements between
     * {@code offset} and {@code offset + size - 1} are
     * available for use by this {@code DataBuffer}.  The array
     * must be large enough to hold {@code offset + size}
     * elements.
     * <p>
     * Note that {@code DataBuffer} objects created by this constructor
     * may be incompatible with <a href="#optimizations">performance
     * optimizations</a> used by some implementations (such as caching
     * an associated image in video memory).
     *
     * @param dataArray An array of {@code float}s to be used as the
     *                  first and only bank of this {@code DataBuffer}.
     * @param size The number of elements of the array to be used.
     * @param offset The offset of the first element of the array
     *               that will be used.
     */
    public DataBufferFloat(float[] dataArray, int size, int offset) {
        super(UNTRACKABLE, TYPE_FLOAT, size, 1, offset);
        data = dataArray;
        bankdata = new float[1][];
        bankdata[0] = data;
    }

    /**
     * Constructs a {@code float}-based {@code DataBuffer}
     * with the specified data arrays.  Only the first
     * {@code size} elements of each array are available for use
     * by this {@code DataBuffer}.  The number of banks will be
     * equal to {@code dataArray.length}.
     * <p>
     * Note that {@code DataBuffer} objects created by this constructor
     * may be incompatible with <a href="#optimizations">performance
     * optimizations</a> used by some implementations (such as caching
     * an associated image in video memory).
     *
     * @param dataArray An array of arrays of {@code float}s to be
     *                  used as the banks of this {@code DataBuffer}.
     * @param size The number of elements of each array to be used.
     */
    public DataBufferFloat(float[][] dataArray, int size) {
        super(UNTRACKABLE, TYPE_FLOAT, size, dataArray.length);
        bankdata = dataArray.clone();
        data = bankdata[0];
    }

    /**
     * Constructs a {@code float}-based {@code DataBuffer}
     * with the specified data arrays, size, and per-bank offsets.
     * The number of banks is equal to {@code dataArray.length}.
     * Each array must be at least as large as {@code size} plus the
     * corresponding offset.  There must be an entry in the offsets
     * array for each data array.
     * <p>
     * Note that {@code DataBuffer} objects created by this constructor
     * may be incompatible with <a href="#optimizations">performance
     * optimizations</a> used by some implementations (such as caching
     * an associated image in video memory).
     *
     * @param dataArray An array of arrays of {@code float}s to be
     *                  used as the banks of this {@code DataBuffer}.
     * @param size The number of elements of each array to be used.
     * @param offsets An array of integer offsets, one for each bank.
     */
    public DataBufferFloat(float[][] dataArray, int size, int[] offsets) {
        super(UNTRACKABLE, TYPE_FLOAT, size,dataArray.length, offsets);
        bankdata = dataArray.clone();
        data = bankdata[0];
    }

    /**
     * Returns the default (first) {@code float} data array.
     * <p>
     * Note that calling this method may cause this {@code DataBuffer}
     * object to be incompatible with <a href="#optimizations">performance
     * optimizations</a> used by some implementations (such as caching
     * an associated image in video memory).
     *
     * @return the first float data array.
     */
    public float[] getData() {
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
     * @param bank the data array
     * @return the data array specified by {@code bank}.
     */
    public float[] getData(int bank) {
        theTrackable.setUntrackable();
        return bankdata[bank];
    }

    /**
     * Returns the data array for all banks.
     * <p>
     * Note that calling this method may cause this {@code DataBuffer}
     * object to be incompatible with <a href="#optimizations">performance
     * optimizations</a> used by some implementations (such as caching
     * an associated image in video memory).
     *
     * @return all data arrays for this data buffer.
     */
    public float[][] getBankData() {
        theTrackable.setUntrackable();
        return bankdata.clone();
    }

    /**
     * Returns the requested data array element from the first
     * (default) bank as an {@code int}.
     *
     * @param i The desired data array element.
     *
     * @return The data entry as an {@code int}.
     * @see #setElem(int, int)
     * @see #setElem(int, int, int)
     */
    public int getElem(int i) {
        return (int)(data[i+offset]);
    }

    /**
     * Returns the requested data array element from the specified
     * bank as an {@code int}.
     *
     * @param bank The bank number.
     * @param i The desired data array element.
     *
     * @return The data entry as an {@code int}.
     * @see #setElem(int, int)
     * @see #setElem(int, int, int)
     */
    public int getElem(int bank, int i) {
        return (int)(bankdata[bank][i+offsets[bank]]);
    }

    /**
     * Sets the requested data array element in the first (default)
     * bank to the given {@code int}.
     *
     * @param i The desired data array element.
     * @param val The value to be set.
     * @see #getElem(int)
     * @see #getElem(int, int)
     */
    public void setElem(int i, int val) {
        data[i+offset] = (float)val;
        theTrackable.markDirty();
    }

    /**
     * Sets the requested data array element in the specified bank to
     * the given {@code int}.
     *
     * @param bank The bank number.
     * @param i The desired data array element.
     * @param val The value to be set.
     * @see #getElem(int)
     * @see #getElem(int, int)
     */
    public void setElem(int bank, int i, int val) {
        bankdata[bank][i+offsets[bank]] = (float)val;
        theTrackable.markDirty();
    }

    /**
     * Returns the requested data array element from the first
     * (default) bank as a {@code float}.
     *
     * @param i The desired data array element.
     *
     * @return The data entry as a {@code float}.
     * @see #setElemFloat(int, float)
     * @see #setElemFloat(int, int, float)
     */
    public float getElemFloat(int i) {
        return data[i+offset];
    }

    /**
     * Returns the requested data array element from the specified
     * bank as a {@code float}.
     *
     * @param bank The bank number.
     * @param i The desired data array element.
     *
     * @return The data entry as a {@code float}.
     * @see #setElemFloat(int, float)
     * @see #setElemFloat(int, int, float)
     */
    public float getElemFloat(int bank, int i) {
        return bankdata[bank][i+offsets[bank]];
    }

    /**
     * Sets the requested data array element in the first (default)
     * bank to the given {@code float}.
     *
     * @param i The desired data array element.
     * @param val The value to be set.
     * @see #getElemFloat(int)
     * @see #getElemFloat(int, int)
     */
    public void setElemFloat(int i, float val) {
        data[i+offset] = val;
        theTrackable.markDirty();
    }

    /**
     * Sets the requested data array element in the specified bank to
     * the given {@code float}.
     *
     * @param bank The bank number.
     * @param i The desired data array element.
     * @param val The value to be set.
     * @see #getElemFloat(int)
     * @see #getElemFloat(int, int)
     */
    public void setElemFloat(int bank, int i, float val) {
        bankdata[bank][i+offsets[bank]] = val;
        theTrackable.markDirty();
    }

    /**
     * Returns the requested data array element from the first
     * (default) bank as a {@code double}.
     *
     * @param i The desired data array element.
     *
     * @return The data entry as a {@code double}.
     * @see #setElemDouble(int, double)
     * @see #setElemDouble(int, int, double)
     */
    public double getElemDouble(int i) {
        return (double)data[i+offset];
    }

    /**
     * Returns the requested data array element from the specified
     * bank as a {@code double}.
     *
     * @param bank The bank number.
     * @param i The desired data array element.
     *
     * @return The data entry as a {@code double}.
     * @see #setElemDouble(int, double)
     * @see #setElemDouble(int, int, double)
     */
    public double getElemDouble(int bank, int i) {
        return (double)bankdata[bank][i+offsets[bank]];
    }

    /**
     * Sets the requested data array element in the first (default)
     * bank to the given {@code double}.
     *
     * @param i The desired data array element.
     * @param val The value to be set.
     * @see #getElemDouble(int)
     * @see #getElemDouble(int, int)
     */
    public void setElemDouble(int i, double val) {
        data[i+offset] = (float)val;
        theTrackable.markDirty();
    }

    /**
     * Sets the requested data array element in the specified bank to
     * the given {@code double}.
     *
     * @param bank The bank number.
     * @param i The desired data array element.
     * @param val The value to be set.
     * @see #getElemDouble(int)
     * @see #getElemDouble(int, int)
     */
    public void setElemDouble(int bank, int i, double val) {
        bankdata[bank][i+offsets[bank]] = (float)val;
        theTrackable.markDirty();
    }
}
