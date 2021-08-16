/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

/**
 * an optimized ShortMessage that does not need an array.
 *
 * @author Florian Bomers
 */
final class FastShortMessage extends ShortMessage {
    private int packedMsg;

    FastShortMessage(int packedMsg) throws InvalidMidiDataException {
        this.packedMsg = packedMsg;
        getDataLength(packedMsg & 0xFF); // to check for validity
    }

    /** Creates a FastShortMessage from this ShortMessage */
    FastShortMessage(ShortMessage msg) {
        this.packedMsg = msg.getStatus()
            | (msg.getData1() << 8)
            | (msg.getData2() << 16);
    }

    int getPackedMsg() {
        return packedMsg;
    }

    @Override
    public byte[] getMessage() {
        int length = 0;
        try {
            // fix for bug 4851018: MidiMessage.getLength and .getData return wrong values
            // fix for bug 4890405: Reading MidiMessage byte array fails in 1.4.2
            length = getDataLength(packedMsg & 0xFF) + 1;
        } catch (InvalidMidiDataException imde) {
            // should never happen
        }
        byte[] returnedArray = new byte[length];
        if (length>0) {
            returnedArray[0] = (byte) (packedMsg & 0xFF);
            if (length>1) {
                returnedArray[1] = (byte) ((packedMsg & 0xFF00) >> 8);
                if (length>2) {
                    returnedArray[2] = (byte) ((packedMsg & 0xFF0000) >> 16);
                }
            }
        }
        return returnedArray;
    }

    @Override
    public int getLength() {
        try {
            return getDataLength(packedMsg & 0xFF) + 1;
        } catch (InvalidMidiDataException imde) {
            // should never happen
        }
        return 0;
    }

    @Override
    public void setMessage(int status) throws InvalidMidiDataException {
        // check for valid values
        int dataLength = getDataLength(status); // can throw InvalidMidiDataException
        if (dataLength != 0) {
            super.setMessage(status); // throws Exception
        }
        packedMsg = (packedMsg & 0xFFFF00) | (status & 0xFF);
    }

    @Override
    public void setMessage(int status, int data1, int data2) throws InvalidMidiDataException {
        getDataLength(status); // can throw InvalidMidiDataException
        packedMsg = (status & 0xFF) | ((data1 & 0xFF) << 8) | ((data2 & 0xFF) << 16);
    }

    @Override
    public void setMessage(int command, int channel, int data1, int data2) throws InvalidMidiDataException {
        getDataLength(command); // can throw InvalidMidiDataException
        packedMsg = (command & 0xF0) | (channel & 0x0F) | ((data1 & 0xFF) << 8) | ((data2 & 0xFF) << 16);
    }

    @Override
    public int getChannel() {
        return packedMsg & 0x0F;
    }

    @Override
    public int getCommand() {
        return packedMsg & 0xF0;
    }

    @Override
    public int getData1() {
        return (packedMsg & 0xFF00) >> 8;
    }

    @Override
    public int getData2() {
        return (packedMsg & 0xFF0000) >> 16;
    }

    @Override
    public int getStatus() {
        return packedMsg & 0xFF;
    }

    /**
     * Creates a new object of the same class and with the same contents
     * as this object.
     * @return a clone of this instance.
     */
    @Override
    public Object clone() {
        try {
            return new FastShortMessage(packedMsg);
        } catch (InvalidMidiDataException imde) {
            // should never happen
        }
        return null;
    }

} // class FastShortMsg
