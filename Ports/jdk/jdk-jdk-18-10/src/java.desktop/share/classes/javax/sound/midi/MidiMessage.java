/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * {@code MidiMessage} is the base class for MIDI messages. They include not
 * only the standard MIDI messages that a synthesizer can respond to, but also
 * "meta-events" that can be used by sequencer programs. There are meta-events
 * for such information as lyrics, copyrights, tempo indications, time and key
 * signatures, markers, etc. For more information, see the Standard MIDI Files
 * 1.0 specification, which is part of the Complete MIDI 1.0 Detailed
 * Specification published by the MIDI Manufacturer's Association
 * (<a href = http://www.midi.org>http://www.midi.org</a>).
 * <p>
 * The base {@code MidiMessage} class provides access to three types of
 * information about a MIDI message:
 * <ul>
 *   <li>The messages's status byte
 *   <li>The total length of the message in bytes (the status byte plus any data
 *   bytes)
 *   <li>A byte array containing the complete message
 * </ul>
 *
 * {@code MidiMessage} includes methods to get, but not set, these values.
 * Setting them is a subclass responsibility.
 * <p>
 * <a id="integersVsBytes"></a>The MIDI standard expresses MIDI data in bytes.
 * However, because Java uses signed bytes, the Java Sound API uses
 * integers instead of bytes when expressing MIDI data. For example, the
 * {@link #getStatus()} method of {@code MidiMessage} returns MIDI status bytes
 * as integers. If you are processing MIDI data that originated outside Java
 * Sound and now is encoded as signed bytes, the bytes can be converted to
 * integers using this conversion:
 * <p style="text-align:center">
 * {@code int i = (int)(byte & 0xFF)}
 * <p>
 * If you simply need to pass a known MIDI byte value as a method parameter, it
 * can be expressed directly as an integer, using (for example) decimal or
 * hexadecimal notation. For instance, to pass the "active sensing" status byte
 * as the first argument to {@code ShortMessage}'s
 * {@link ShortMessage#setMessage(int) setMessage(int)} method, you can express
 * it as 254 or 0xFE.
 *
 * @author David Rivas
 * @author Kara Kytle
 * @see Track
 * @see Sequence
 * @see Receiver
 */
public abstract class MidiMessage implements Cloneable {

    /**
     * The MIDI message data. The first byte is the status byte for the message;
     * subsequent bytes up to the length of the message are data bytes for this
     * message.
     *
     * @see #getLength
     */
    protected byte[] data;

    /**
     * The number of bytes in the MIDI message, including the status byte and
     * any data bytes.
     *
     * @see #getLength
     */
    protected int length = 0;

    /**
     * Constructs a new {@code MidiMessage}. This protected constructor is
     * called by concrete subclasses, which should ensure that the data array
     * specifies a complete, valid MIDI message.
     *
     * @param  data an array of bytes containing the complete message. The
     *         message data may be changed using the {@code setMessage} method.
     * @see #setMessage
     */
    protected MidiMessage(byte[] data) {
        this.data = data;
        if (data != null) {
            this.length = data.length;
        }
    }

    /**
     * Sets the data for the MIDI message. This protected method is called by
     * concrete subclasses, which should ensure that the data array specifies a
     * complete, valid MIDI message.
     *
     * @param  data the data bytes in the MIDI message
     * @param  length the number of bytes in the data byte array
     * @throws InvalidMidiDataException if the parameter values do not specify a
     *         valid MIDI meta message
     */
    protected void setMessage(byte[] data, int length)
            throws InvalidMidiDataException {
        if (length < 0 || (length > 0 && length > data.length)) {
            throw new IndexOutOfBoundsException(
                    "length out of bounds: " + length);
        }
        this.length = length;

        if (this.data == null || this.data.length < this.length) {
            this.data = new byte[this.length];
        }
        System.arraycopy(data, 0, this.data, 0, length);
    }

    /**
     * Obtains the MIDI message data. The first byte of the returned byte array
     * is the status byte of the message. Any subsequent bytes up to the length
     * of the message are data bytes. The byte array may have a length which is
     * greater than that of the actual message; the total length of the message
     * in bytes is reported by the {@link #getLength} method.
     *
     * @return the byte array containing the complete {@code MidiMessage} data
     */
    public byte[] getMessage() {
        byte[] returnedArray = new byte[length];
        System.arraycopy(data, 0, returnedArray, 0, length);
        return returnedArray;
    }

    /**
     * Obtains the status byte for the MIDI message. The status "byte" is
     * represented as an integer; see the
     * <a href="#integersVsBytes">discussion</a> in the {@code MidiMessage}
     * class description.
     *
     * @return the integer representation of this event's status byte
     */
    public int getStatus() {
        if (length > 0) {
            return (data[0] & 0xFF);
        }
        return 0;
    }

    /**
     * Obtains the total length of the MIDI message in bytes. A MIDI message
     * consists of one status byte and zero or more data bytes. The return value
     * ranges from 1 for system real-time messages, to 2 or 3 for channel
     * messages, to any value for meta and system exclusive messages.
     *
     * @return the length of the message in bytes
     */
    public int getLength() {
        return length;
    }

    /**
     * Creates a new object of the same class and with the same contents as this
     * object.
     *
     * @return a clone of this instance
     */
    @Override
    public abstract Object clone();
}
