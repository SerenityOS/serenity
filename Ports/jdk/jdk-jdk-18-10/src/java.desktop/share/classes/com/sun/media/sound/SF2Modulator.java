/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.sound;

/**
 * Soundfont modulator container.
 *
 * @author Karl Helgason
 */
public final class SF2Modulator {

    public static final int SOURCE_NONE = 0;
    public static final int SOURCE_NOTE_ON_VELOCITY = 2;
    public static final int SOURCE_NOTE_ON_KEYNUMBER = 3;
    public static final int SOURCE_POLY_PRESSURE = 10;
    public static final int SOURCE_CHANNEL_PRESSURE = 13;
    public static final int SOURCE_PITCH_WHEEL = 14;
    public static final int SOURCE_PITCH_SENSITIVITY = 16;
    public static final int SOURCE_MIDI_CONTROL = 128 * 1;
    public static final int SOURCE_DIRECTION_MIN_MAX = 256 * 0;
    public static final int SOURCE_DIRECTION_MAX_MIN = 256 * 1;
    public static final int SOURCE_POLARITY_UNIPOLAR = 512 * 0;
    public static final int SOURCE_POLARITY_BIPOLAR = 512 * 1;
    public static final int SOURCE_TYPE_LINEAR = 1024 * 0;
    public static final int SOURCE_TYPE_CONCAVE = 1024 * 1;
    public static final int SOURCE_TYPE_CONVEX = 1024 * 2;
    public static final int SOURCE_TYPE_SWITCH = 1024 * 3;
    public static final int TRANSFORM_LINEAR = 0;
    public static final int TRANSFORM_ABSOLUTE = 2;
    int sourceOperator;
    int destinationOperator;
    short amount;
    int amountSourceOperator;
    int transportOperator;

    public short getAmount() {
        return amount;
    }

    public void setAmount(short amount) {
        this.amount = amount;
    }

    public int getAmountSourceOperator() {
        return amountSourceOperator;
    }

    public void setAmountSourceOperator(int amountSourceOperator) {
        this.amountSourceOperator = amountSourceOperator;
    }

    public int getTransportOperator() {
        return transportOperator;
    }

    public void setTransportOperator(int transportOperator) {
        this.transportOperator = transportOperator;
    }

    public int getDestinationOperator() {
        return destinationOperator;
    }

    public void setDestinationOperator(int destinationOperator) {
        this.destinationOperator = destinationOperator;
    }

    public int getSourceOperator() {
        return sourceOperator;
    }

    public void setSourceOperator(int sourceOperator) {
        this.sourceOperator = sourceOperator;
    }
}
