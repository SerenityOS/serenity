/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.sound.midi;

import com.sun.media.sound.MidiUtils;

/**
 * A {@code SysexMessage} object represents a MIDI system exclusive message.
 * <p>
 * When a system exclusive message is read from a MIDI file, it always has a
 * defined length. Data from a system exclusive message from a MIDI file should
 * be stored in the data array of a {@code SysexMessage} as follows: the system
 * exclusive message status byte (0xF0 or 0xF7), all message data bytes, and
 * finally the end-of-exclusive flag (0xF7). The length reported by the
 * {@code SysexMessage} object is therefore the length of the system exclusive
 * data plus two: one byte for the status byte and one for the end-of-exclusive
 * flag.
 * <p>
 * As dictated by the Standard MIDI Files specification, two status byte values
 * are legal for a {@code SysexMessage} read from a MIDI file:
 * <ul>
 *   <li>0xF0: System Exclusive message (same as in MIDI wire protocol)
 *   <li>0xF7: Special System Exclusive message
 * </ul>
 * When Java Sound is used to handle system exclusive data that is being
 * received using MIDI wire protocol, it should place the data in one or more
 * {@code SysexMessages}. In this case, the length of the system exclusive data
 * is not known in advance; the end of the system exclusive data is marked by an
 * end-of-exclusive flag (0xF7) in the MIDI wire byte stream.
 * <ul>
 *   <li>0xF0: System Exclusive message (same as in MIDI wire protocol)
 *   <li>0xF7: End of Exclusive (EOX)
 * </ul>
 * The first {@code SysexMessage} object containing data for a particular system
 * exclusive message should have the status value 0xF0. If this message contains
 * all the system exclusive data for the message, it should end with the status
 * byte 0xF7 (EOX). Otherwise, additional system exclusive data should be sent
 * in one or more {@code SysexMessages} with a status value of 0xF7. The
 * {@code SysexMessage} containing the last of the data for the system exclusive
 * message should end with the value 0xF7 (EOX) to mark the end of the system
 * exclusive message.
 * <p>
 * If system exclusive data from {@code SysexMessages} objects is being
 * transmitted using MIDI wire protocol, only the initial 0xF0 status byte, the
 * system exclusive data itself, and the final 0xF7 (EOX) byte should be
 * propagated; any 0xF7 status bytes used to indicate that a
 * {@code SysexMessage} contains continuing system exclusive data should not be
 * propagated via MIDI wire protocol.
 *
 * @author David Rivas
 * @author Kara Kytle
 * @author Florian Bomers
 */
public class SysexMessage extends MidiMessage {

    // Status byte defines

    /**
     * Status byte for System Exclusive message (0xF0, or 240).
     *
     * @see MidiMessage#getStatus
     */
    public static final int SYSTEM_EXCLUSIVE = 0xF0; // 240

    /**
     * Status byte for Special System Exclusive message (0xF7, or 247), which is
     * used in MIDI files. It has the same value as END_OF_EXCLUSIVE, which is
     * used in the real-time "MIDI wire" protocol.
     *
     * @see MidiMessage#getStatus
     */
    public static final int SPECIAL_SYSTEM_EXCLUSIVE = 0xF7; // 247

    /**
     * The data bytes for this system exclusive message. These are initialized
     * to {@code null} and are set explicitly by
     * {@link #setMessage(int, byte[], int, long) setMessage}.
     */
    //protected byte[] data = null;

    /**
     * Constructs a new {@code SysexMessage}. The contents of the new message
     * are guaranteed to specify a valid MIDI message. Subsequently, you may set
     * the contents of the message using one of the {@code setMessage} methods.
     *
     * @see #setMessage
     */
    public SysexMessage() {
        this(new byte[2]);
        // Default sysex message data: SOX followed by EOX
        data[0] = (byte) (SYSTEM_EXCLUSIVE & 0xFF);
        data[1] = (byte) (ShortMessage.END_OF_EXCLUSIVE & 0xFF);
    }

    /**
     * Constructs a new {@code SysexMessage} and sets the data for the message.
     * The first byte of the data array must be a valid system exclusive status
     * byte (0xF0 or 0xF7). The contents of the message can be changed by using
     * one of the {@code setMessage} methods.
     *
     * @param  data the system exclusive message data including the status byte
     * @param  length the length of the valid message data in the array,
     *         including the status byte; it should be non-negative and less
     *         than or equal to {@code data.length}
     * @throws InvalidMidiDataException if the parameter values do not specify a
     *         valid MIDI meta message
     * @see #setMessage(byte[], int)
     * @see #setMessage(int, byte[], int)
     * @see #getData()
     * @since 1.7
     */
    public SysexMessage(byte[] data, int length)
            throws InvalidMidiDataException {
        super(null);
        setMessage(data, length);
    }

    /**
     * Constructs a new {@code SysexMessage} and sets the data for the message.
     * The contents of the message can be changed by using one of the
     * {@code setMessage} methods.
     *
     * @param  status the status byte for the message; it must be a valid system
     *         exclusive status byte (0xF0 or 0xF7)
     * @param  data the system exclusive message data (without the status byte)
     * @param  length the length of the valid message data in the array; it
     *         should be non-negative and less than or equal to
     *         {@code data.length}
     * @throws InvalidMidiDataException if the parameter values do not specify a
     *         valid MIDI system exclusive message
     * @see #setMessage(byte[], int)
     * @see #setMessage(int, byte[], int)
     * @see #getData()
     * @since 1.7
     */
    public SysexMessage(int status, byte[] data, int length)
            throws InvalidMidiDataException {
        super(null);
        setMessage(status, data, length);
    }

    /**
     * Constructs a new {@code SysexMessage}.
     *
     * @param  data an array of bytes containing the complete message. The
     *         message data may be changed using the {@code setMessage} method.
     * @see #setMessage
     */
    protected SysexMessage(byte[] data) {
        super(data);
    }

    /**
     * Sets the data for the system exclusive message. The first byte of the
     * data array must be a valid system exclusive status byte (0xF0 or 0xF7).
     *
     * @param  data the system exclusive message data
     * @param  length the length of the valid message data in the array,
     *         including the status byte
     * @throws InvalidMidiDataException if the parameter values do not specify a
     *         valid MIDI system exclusive message
     */
    @Override
    public void setMessage(byte[] data, int length) throws InvalidMidiDataException {
        MidiUtils.checkSysexStatus(data, length);
        super.setMessage(data, length);
    }

    /**
     * Sets the data for the system exclusive message.
     *
     * @param  status the status byte for the message (0xF0 or 0xF7)
     * @param  data the system exclusive message data
     * @param  length the length of the valid message data in the array
     * @throws InvalidMidiDataException if the status byte is invalid for a
     *         system exclusive message
     */
    public void setMessage(int status, byte[] data, int length) throws InvalidMidiDataException {
        MidiUtils.checkSysexStatus(status);
        if (length < 0 || length > data.length) {
            throw new IndexOutOfBoundsException("length out of bounds: "+length);
        }
        this.length = length + 1;

        if (this.data==null || this.data.length < this.length) {
            this.data = new byte[this.length];
        }

        this.data[0] = (byte) (status & 0xFF);
        if (length > 0) {
            System.arraycopy(data, 0, this.data, 1, length);
        }
    }

    /**
     * Obtains a copy of the data for the system exclusive message. The returned
     * array of bytes does not include the status byte.
     *
     * @return array containing the system exclusive message data
     */
    public byte[] getData() {
        byte[] returnedArray = new byte[length - 1];
        System.arraycopy(data, 1, returnedArray, 0, (length - 1));
        return returnedArray;
    }

    /**
     * Creates a new object of the same class and with the same contents as this
     * object.
     *
     * @return a clone of this instance
     */
    @Override
    public Object clone() {
        byte[] newData = new byte[length];
        System.arraycopy(data, 0, newData, 0, newData.length);
        return new SysexMessage(newData);
    }
}
