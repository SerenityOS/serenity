/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * A {@code ShortMessage} contains a MIDI message that has at most two data
 * bytes following its status byte. The types of MIDI message that satisfy this
 * criterion are channel voice, channel mode, system common, and system
 * real-time--in other words, everything except system exclusive and
 * meta-events. The {@code ShortMessage} class provides methods for getting and
 * setting the contents of the MIDI message.
 * <p>
 * A number of {@code ShortMessage} methods have integer parameters by which you
 * specify a MIDI status or data byte. If you know the numeric value, you can
 * express it directly. For system common and system real-time messages, you can
 * often use the corresponding fields of {@code ShortMessage}, such as
 * {@link #SYSTEM_RESET SYSTEM_RESET}. For channel messages, the upper four bits
 * of the status byte are specified by a command value and the lower four bits
 * are specified by a MIDI channel number. To convert incoming MIDI data bytes
 * that are in the form of Java's signed bytes, you can use the
 * <a href="MidiMessage.html#integersVsBytes">conversion code</a> given in the
 * {@link MidiMessage} class description.
 *
 * @author David Rivas
 * @author Kara Kytle
 * @author Florian Bomers
 * @see SysexMessage
 * @see MetaMessage
 */
public class ShortMessage extends MidiMessage {

    // Status byte defines

    // System common messages

    /**
     * Status byte for MIDI Time Code Quarter Frame message (0xF1, or 241).
     *
     * @see MidiMessage#getStatus
     */
    public static final int MIDI_TIME_CODE = 0xF1; // 241

    /**
     * Status byte for Song Position Pointer message (0xF2, or 242).
     *
     * @see MidiMessage#getStatus
     */
    public static final int SONG_POSITION_POINTER = 0xF2; // 242

    /**
     * Status byte for MIDI Song Select message (0xF3, or 243).
     *
     * @see MidiMessage#getStatus
     */
    public static final int SONG_SELECT = 0xF3; // 243

    /**
     * Status byte for Tune Request message (0xF6, or 246).
     *
     * @see MidiMessage#getStatus
     */
    public static final int TUNE_REQUEST = 0xF6; // 246

    /**
     * Status byte for End of System Exclusive message (0xF7, or 247).
     *
     * @see MidiMessage#getStatus
     */
    public static final int END_OF_EXCLUSIVE = 0xF7; // 247

    // System real-time messages

    /**
     * Status byte for Timing Clock message (0xF8, or 248).
     *
     * @see MidiMessage#getStatus
     */
    public static final int TIMING_CLOCK = 0xF8; // 248

    /**
     * Status byte for Start message (0xFA, or 250).
     *
     * @see MidiMessage#getStatus
     */
    public static final int START = 0xFA; // 250

    /**
     * Status byte for Continue message (0xFB, or 251).
     *
     * @see MidiMessage#getStatus
     */
    public static final int CONTINUE = 0xFB; // 251

    /**
     * Status byte for Stop message (0xFC, or 252).
     *
     * @see MidiMessage#getStatus
     */
    public static final int STOP = 0xFC; //252

    /**
     * Status byte for Active Sensing message (0xFE, or 254).
     *
     * @see MidiMessage#getStatus
     */
    public static final int ACTIVE_SENSING = 0xFE; // 254

    /**
     * Status byte for System Reset message (0xFF, or 255).
     *
     * @see MidiMessage#getStatus
     */
    public static final int SYSTEM_RESET = 0xFF; // 255

    // Channel voice message upper nibble defines

    /**
     * Command value for Note Off message (0x80, or 128).
     */
    public static final int NOTE_OFF = 0x80;  // 128

    /**
     * Command value for Note On message (0x90, or 144).
     */
    public static final int NOTE_ON = 0x90;  // 144

    /**
     * Command value for Polyphonic Key Pressure (Aftertouch) message (0xA0, or
     * 160).
     */
    public static final int POLY_PRESSURE = 0xA0;  // 160

    /**
     * Command value for Control Change message (0xB0, or 176).
     */
    public static final int CONTROL_CHANGE = 0xB0;  // 176

    /**
     * Command value for Program Change message (0xC0, or 192).
     */
    public static final int PROGRAM_CHANGE = 0xC0;  // 192

    /**
     * Command value for Channel Pressure (Aftertouch) message (0xD0, or 208).
     */
    public static final int CHANNEL_PRESSURE = 0xD0;  // 208

    /**
     * Command value for Pitch Bend message (0xE0, or 224).
     */
    public static final int PITCH_BEND = 0xE0;  // 224

    /**
     * Constructs a new {@code ShortMessage}. The contents of the new message
     * are guaranteed to specify a valid MIDI message. Subsequently, you may set
     * the contents of the message using one of the {@code setMessage} methods.
     *
     * @see #setMessage
     */
    public ShortMessage() {
        this(new byte[3]);
        // Default message data: NOTE_ON on Channel 0 with max volume
        data[0] = (byte) (NOTE_ON & 0xFF);
        data[1] = (byte) 64;
        data[2] = (byte) 127;
        length = 3;
    }

    /**
     * Constructs a new {@code ShortMessage} which represents a MIDI message
     * that takes no data bytes. The contents of the message can be changed by
     * using one of the {@code setMessage} methods.
     *
     * @param  status the MIDI status byte
     * @throws InvalidMidiDataException if {@code status} does not specify a
     *         valid MIDI status byte for a message that requires no data bytes
     * @see #setMessage(int)
     * @see #setMessage(int, int, int)
     * @see #setMessage(int, int, int, int)
     * @see #getStatus()
     * @since 1.7
     */
    public ShortMessage(int status) throws InvalidMidiDataException {
        super(null);
        setMessage(status); // can throw InvalidMidiDataException
    }

    /**
     * Constructs a new {@code ShortMessage} which represents a MIDI message
     * that takes up to two data bytes. If the message only takes one data byte,
     * the second data byte is ignored. If the message does not take any data
     * bytes, both data bytes are ignored. The contents of the message can be
     * changed by using one of the {@code setMessage} methods.
     *
     * @param  status the MIDI status byte
     * @param  data1 the first data byte
     * @param  data2 the second data byte
     * @throws InvalidMidiDataException if the status byte or all data bytes
     *         belonging to the message do not specify a valid MIDI message
     * @see #setMessage(int)
     * @see #setMessage(int, int, int)
     * @see #setMessage(int, int, int, int)
     * @see #getStatus()
     * @see #getData1()
     * @see #getData2()
     * @since 1.7
     */
    public ShortMessage(int status, int data1, int data2)
            throws InvalidMidiDataException {
        super(null);
        setMessage(status, data1, data2); // can throw InvalidMidiDataException
    }

    /**
     * Constructs a new {@code ShortMessage} which represents a channel MIDI
     * message that takes up to two data bytes. If the message only takes one
     * data byte, the second data byte is ignored. If the message does not take
     * any data bytes, both data bytes are ignored. The contents of the message
     * can be changed by using one of the {@code setMessage} methods.
     *
     * @param  command the MIDI command represented by this message
     * @param  channel the channel associated with the message
     * @param  data1 the first data byte
     * @param  data2 the second data byte
     * @throws InvalidMidiDataException if the command value, channel value or
     *         all data bytes belonging to the message do not specify a valid
     *         MIDI message
     * @see #setMessage(int)
     * @see #setMessage(int, int, int)
     * @see #setMessage(int, int, int, int)
     * @see #getCommand()
     * @see #getChannel()
     * @see #getData1()
     * @see #getData2()
     * @since 1.7
     */
    public ShortMessage(int command, int channel, int data1, int data2)
            throws InvalidMidiDataException {
        super(null);
        setMessage(command, channel, data1, data2);
    }

    /**
     * Constructs a new {@code ShortMessage}.
     *
     * @param  data an array of bytes containing the complete message. The
     *         message data may be changed using the {@code setMessage} method.
     * @see #setMessage
     */
    // $$fb this should throw an Exception in case of an illegal message!
    protected ShortMessage(byte[] data) {
        // $$fb this may set an invalid message.
        // Can't correct without compromising compatibility
        super(data);
    }

    /**
     * Sets the parameters for a MIDI message that takes no data bytes.
     *
     * @param  status the MIDI status byte
     * @throws InvalidMidiDataException if {@code status} does not specify a
     *         valid MIDI status byte for a message that requires no data bytes
     * @see #setMessage(int, int, int)
     * @see #setMessage(int, int, int, int)
     */
    public void setMessage(int status) throws InvalidMidiDataException {
        // check for valid values
        int dataLength = getDataLength(status); // can throw InvalidMidiDataException
        if (dataLength != 0) {
            throw new InvalidMidiDataException("Status byte; " + status + " requires " + dataLength + " data bytes");
        }
        setMessage(status, 0, 0);
    }

    /**
     * Sets the parameters for a MIDI message that takes one or two data bytes.
     * If the message takes only one data byte, the second data byte is ignored;
     * if the message does not take any data bytes, both data bytes are ignored.
     *
     * @param  status the MIDI status byte
     * @param  data1 the first data byte
     * @param  data2 the second data byte
     * @throws InvalidMidiDataException if the status byte, or all data bytes
     *         belonging to the message, do not specify a valid MIDI message
     * @see #setMessage(int, int, int, int)
     * @see #setMessage(int)
     */
    public void setMessage(int status, int data1, int data2) throws InvalidMidiDataException {
        // check for valid values
        int dataLength = getDataLength(status); // can throw InvalidMidiDataException
        if (dataLength > 0) {
            if (data1 < 0 || data1 > 127) {
                throw new InvalidMidiDataException("data1 out of range: " + data1);
            }
            if (dataLength > 1) {
                if (data2 < 0 || data2 > 127) {
                    throw new InvalidMidiDataException("data2 out of range: " + data2);
                }
            }
        }


        // set the length
        length = dataLength + 1;
        // re-allocate array if ShortMessage(byte[]) constructor gave array with fewer elements
        if (data == null || data.length < length) {
            data = new byte[3];
        }

        // set the data
        data[0] = (byte) (status & 0xFF);
        if (length > 1) {
            data[1] = (byte) (data1 & 0xFF);
            if (length > 2) {
                data[2] = (byte) (data2 & 0xFF);
            }
        }
    }

    /**
     * Sets the short message parameters for a channel message which takes up to
     * two data bytes. If the message only takes one data byte, the second data
     * byte is ignored; if the message does not take any data bytes, both data
     * bytes are ignored.
     *
     * @param  command the MIDI command represented by this message
     * @param  channel the channel associated with the message
     * @param  data1 the first data byte
     * @param  data2 the second data byte
     * @throws InvalidMidiDataException if the status byte or all data bytes
     *         belonging to the message, do not specify a valid MIDI message
     * @see #setMessage(int, int, int)
     * @see #setMessage(int)
     * @see #getCommand
     * @see #getChannel
     * @see #getData1
     * @see #getData2
     */
    public void setMessage(int command, int channel, int data1, int data2) throws InvalidMidiDataException {
        // check for valid values
        if (command >= 0xF0 || command < 0x80) {
            throw new InvalidMidiDataException("command out of range: 0x" + Integer.toHexString(command));
        }
        if ((channel & 0xFFFFFFF0) != 0) { // <=> (channel<0 || channel>15)
            throw new InvalidMidiDataException("channel out of range: " + channel);
        }
        setMessage((command & 0xF0) | (channel & 0x0F), data1, data2);
    }

    /**
     * Obtains the MIDI channel associated with this event. This method assumes
     * that the event is a MIDI channel message; if not, the return value will
     * not be meaningful.
     *
     * @return MIDI channel associated with the message
     * @see #setMessage(int, int, int, int)
     */
    public int getChannel() {
        // this returns 0 if an invalid message is set
        return (getStatus() & 0x0F);
    }

    /**
     * Obtains the MIDI command associated with this event. This method assumes
     * that the event is a MIDI channel message; if not, the return value will
     * not be meaningful.
     *
     * @return the MIDI command associated with this event
     * @see #setMessage(int, int, int, int)
     */
    public int getCommand() {
        // this returns 0 if an invalid message is set
        return (getStatus() & 0xF0);
    }

    /**
     * Obtains the first data byte in the message.
     *
     * @return the value of the {@code data1} field
     * @see #setMessage(int, int, int)
     */
    public int getData1() {
        if (length > 1) {
            return (data[1] & 0xFF);
        }
        return 0;
    }

    /**
     * Obtains the second data byte in the message.
     *
     * @return the value of the {@code data2} field
     * @see #setMessage(int, int, int)
     */
    public int getData2() {
        if (length > 2) {
            return (data[2] & 0xFF);
        }
        return 0;
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
        return new ShortMessage(newData);
    }

    /**
     * Retrieves the number of data bytes associated with a particular status
     * byte value.
     *
     * @param  status status byte value, which must represent a short MIDI
     *         message
     * @return data length in bytes (0, 1, or 2)
     * @throws InvalidMidiDataException if the {@code status} argument does not
     *         represent the status byte for any short message
     */
    protected final int getDataLength(int status) throws InvalidMidiDataException {
        // system common and system real-time messages
        switch(status) {
        case 0xF6:                      // Tune Request
        case 0xF7:                      // EOX
            // System real-time messages
        case 0xF8:                      // Timing Clock
        case 0xF9:                      // Undefined
        case 0xFA:                      // Start
        case 0xFB:                      // Continue
        case 0xFC:                      // Stop
        case 0xFD:                      // Undefined
        case 0xFE:                      // Active Sensing
        case 0xFF:                      // System Reset
            return 0;
        case 0xF1:                      // MTC Quarter Frame
        case 0xF3:                      // Song Select
            return 1;
        case 0xF2:                      // Song Position Pointer
            return 2;
        default:
        }

        // channel voice and mode messages
        switch(status & 0xF0) {
        case 0x80:
        case 0x90:
        case 0xA0:
        case 0xB0:
        case 0xE0:
            return 2;
        case 0xC0:
        case 0xD0:
            return 1;
        default:
            throw new InvalidMidiDataException("Invalid status byte: " + status);
        }
    }
}
