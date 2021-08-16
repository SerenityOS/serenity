/*
 * Copyright (c) 1999, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.sound.midi.spi.MidiFileReader;

/**
 * MIDI file reader.
 *
 * @author Kara Kytle
 * @author Jan Borgersen
 * @author Florian Bomers
 */
public final class StandardMidiFileReader extends MidiFileReader {

    private static final int MThd_MAGIC = 0x4d546864;  // 'MThd'

    private static final int bisBufferSize = 1024; // buffer size in buffered input streams

    @Override
    public MidiFileFormat getMidiFileFormat(InputStream stream)
            throws InvalidMidiDataException, IOException {
        return getMidiFileFormatFromStream(stream, MidiFileFormat.UNKNOWN_LENGTH, null);
    }

    // $$fb 2002-04-17: part of fix for 4635286: MidiSystem.getMidiFileFormat()
    // returns format having invalid length
    private MidiFileFormat getMidiFileFormatFromStream(InputStream stream,
                                                       int fileLength,
                                                       SMFParser smfParser)
            throws InvalidMidiDataException, IOException{
        int maxReadLength = 16;
        int duration = MidiFileFormat.UNKNOWN_LENGTH;
        DataInputStream dis;

        if (stream instanceof DataInputStream) {
            dis = (DataInputStream) stream;
        } else {
            dis = new DataInputStream(stream);
        }
        if (smfParser == null) {
            dis.mark(maxReadLength);
        } else {
            smfParser.stream = dis;
        }

        int type;
        int numtracks;
        float divisionType;
        int resolution;

        try {
            int magic = dis.readInt();
            if( !(magic == MThd_MAGIC) ) {
                // not MIDI
                throw new InvalidMidiDataException("not a valid MIDI file");
            }

            // read header length
            int bytesRemaining = dis.readInt() - 6;
            type = dis.readShort();
            numtracks = dis.readShort();
            int timing = dis.readShort();

            // decipher the timing code
            if (timing > 0) {
                // tempo based timing.  value is ticks per beat.
                divisionType = Sequence.PPQ;
                resolution = timing;
            } else {
                // SMPTE based timing.  first decipher the frame code.
                int frameCode = -1 * (timing >> 8);
                switch(frameCode) {
                case 24:
                    divisionType = Sequence.SMPTE_24;
                    break;
                case 25:
                    divisionType = Sequence.SMPTE_25;
                    break;
                case 29:
                    divisionType = Sequence.SMPTE_30DROP;
                    break;
                case 30:
                    divisionType = Sequence.SMPTE_30;
                    break;
                default:
                    throw new InvalidMidiDataException("Unknown frame code: " + frameCode);
                }
                // now determine the timing resolution in ticks per frame.
                resolution = timing & 0xFF;
            }
            if (smfParser != null) {
                // remainder of this chunk
                dis.skip(bytesRemaining);
                smfParser.tracks = numtracks;
            }
        } finally {
            // if only reading the file format, reset the stream
            if (smfParser == null) {
                dis.reset();
            }
        }
        MidiFileFormat format = new MidiFileFormat(type, divisionType, resolution, fileLength, duration);
        return format;
    }

    @Override
    public MidiFileFormat getMidiFileFormat(URL url) throws InvalidMidiDataException, IOException {
        InputStream urlStream = url.openStream(); // throws IOException
        BufferedInputStream bis = new BufferedInputStream( urlStream, bisBufferSize );
        MidiFileFormat fileFormat = null;
        try {
            fileFormat = getMidiFileFormat( bis ); // throws InvalidMidiDataException
        } finally {
            bis.close();
        }
        return fileFormat;
    }

    @Override
    public MidiFileFormat getMidiFileFormat(File file) throws InvalidMidiDataException, IOException {
        FileInputStream fis = new FileInputStream(file); // throws IOException
        BufferedInputStream bis = new BufferedInputStream(fis, bisBufferSize);

        // $$fb 2002-04-17: part of fix for 4635286: MidiSystem.getMidiFileFormat() returns format having invalid length
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            length = MidiFileFormat.UNKNOWN_LENGTH;
        }
        MidiFileFormat fileFormat = null;
        try {
            fileFormat = getMidiFileFormatFromStream(bis, (int) length, null);
        } finally {
            bis.close();
        }
        return fileFormat;
    }

    @Override
    public Sequence getSequence(InputStream stream) throws InvalidMidiDataException, IOException {
        SMFParser smfParser = new SMFParser();
        MidiFileFormat format = getMidiFileFormatFromStream(stream,
                                                            MidiFileFormat.UNKNOWN_LENGTH,
                                                            smfParser);

        // must be MIDI Type 0 or Type 1
        if ((format.getType() != 0) && (format.getType() != 1)) {
            throw new InvalidMidiDataException("Invalid or unsupported file type: "  + format.getType());
        }

        // construct the sequence object
        Sequence sequence = new Sequence(format.getDivisionType(), format.getResolution());

        // for each track, go to the beginning and read the track events
        for (int i = 0; i < smfParser.tracks; i++) {
            if (smfParser.nextTrack()) {
                smfParser.readTrack(sequence.createTrack());
            } else {
                break;
            }
        }
        return sequence;
    }

    @Override
    public Sequence getSequence(URL url) throws InvalidMidiDataException, IOException {
        InputStream is = url.openStream();  // throws IOException
        is = new BufferedInputStream(is, bisBufferSize);
        Sequence seq = null;
        try {
            seq = getSequence(is);
        } finally {
            is.close();
        }
        return seq;
    }

    @Override
    public Sequence getSequence(File file) throws InvalidMidiDataException, IOException {
        InputStream is = new FileInputStream(file); // throws IOException
        is = new BufferedInputStream(is, bisBufferSize);
        Sequence seq = null;
        try {
            seq = getSequence(is);
        } finally {
            is.close();
        }
        return seq;
    }
}

//=============================================================================================================

/**
 * State variables during parsing of a MIDI file.
 */
final class SMFParser {
    private static final int MTrk_MAGIC = 0x4d54726b;  // 'MTrk'

    // set to true to not allow corrupt MIDI files tombe loaded
    private static final boolean STRICT_PARSER = false;

    private static final boolean DEBUG = false;

    int tracks;                       // number of tracks
    DataInputStream stream;   // the stream to read from

    private int trackLength = 0;  // remaining length in track
    private byte[] trackData = null;
    private int pos = 0;

    SMFParser() {
    }

    private int readUnsigned() throws IOException {
        return trackData[pos++] & 0xFF;
    }

    private void read(byte[] data) throws IOException {
        System.arraycopy(trackData, pos, data, 0, data.length);
        pos += data.length;
    }

    private long readVarInt() throws IOException {
        long value = 0; // the variable-lengh int value
        int currentByte = 0;
        do {
            currentByte = trackData[pos++] & 0xFF;
            value = (value << 7) + (currentByte & 0x7F);
        } while ((currentByte & 0x80) != 0);
        return value;
    }

    private int readIntFromStream() throws IOException {
        try {
            return stream.readInt();
        } catch (EOFException eof) {
            throw new EOFException("invalid MIDI file");
        }
    }

    boolean nextTrack() throws IOException, InvalidMidiDataException {
        int magic;
        trackLength = 0;
        do {
            // $$fb 2003-08-20: fix for 4910986: MIDI file parser breaks up on http connection
            if (stream.skipBytes(trackLength) != trackLength) {
                if (!STRICT_PARSER) {
                    return false;
                }
                throw new EOFException("invalid MIDI file");
            }
            magic = readIntFromStream();
            trackLength = readIntFromStream();
        } while (magic != MTrk_MAGIC);
        if (!STRICT_PARSER) {
            if (trackLength < 0) {
                return false;
            }
        }
        // now read track in a byte array
        try {
            trackData = new byte[trackLength];
        } catch (final OutOfMemoryError oom) {
            throw new IOException("Track length too big", oom);
        }
        try {
            // $$fb 2003-08-20: fix for 4910986: MIDI file parser breaks up on http connection
            stream.readFully(trackData);
        } catch (EOFException eof) {
            if (!STRICT_PARSER) {
                return false;
            }
            throw new EOFException("invalid MIDI file");
        }
        pos = 0;
        return true;
    }

    private boolean trackFinished() {
        return pos >= trackLength;
    }

    void readTrack(Track track) throws IOException, InvalidMidiDataException {
        try {
            // reset current tick to 0
            long tick = 0;

            // reset current status byte to 0 (invalid value).
            // this should cause us to throw an InvalidMidiDataException if we don't
            // get a valid status byte from the beginning of the track.
            int status = 0;
            boolean endOfTrackFound = false;

            while (!trackFinished() && !endOfTrackFound) {
                MidiMessage message;

                int data1 = -1;         // initialize to invalid value
                int data2 = 0;

                // each event has a tick delay and then the event data.

                // first read the delay (a variable-length int) and update our tick value
                tick += readVarInt();

                // check for new status
                int byteValue = readUnsigned();

                if (byteValue >= 0x80) {
                    status = byteValue;
                } else {
                    data1 = byteValue;
                }

                switch (status & 0xF0) {
                case 0x80:
                case 0x90:
                case 0xA0:
                case 0xB0:
                case 0xE0:
                    // two data bytes
                    if (data1 == -1) {
                        data1 = readUnsigned();
                    }
                    data2 = readUnsigned();
                    message = new FastShortMessage(status | (data1 << 8) | (data2 << 16));
                    break;
                case 0xC0:
                case 0xD0:
                    // one data byte
                    if (data1 == -1) {
                        data1 = readUnsigned();
                    }
                    message = new FastShortMessage(status | (data1 << 8));
                    break;
                case 0xF0:
                    // sys-ex or meta
                    switch(status) {
                    case 0xF0:
                    case 0xF7:
                        // sys ex
                        int sysexLength = (int) readVarInt();
                        byte[] sysexData = new byte[sysexLength];
                        read(sysexData);

                        SysexMessage sysexMessage = new SysexMessage();
                        sysexMessage.setMessage(status, sysexData, sysexLength);
                        message = sysexMessage;
                        break;

                    case 0xFF:
                        // meta
                        int metaType = readUnsigned();
                        int metaLength = (int) readVarInt();
                        final byte[] metaData;
                        try {
                            metaData = new byte[metaLength];
                        } catch (final OutOfMemoryError oom) {
                            throw new IOException("Meta length too big", oom);
                        }

                        read(metaData);

                        MetaMessage metaMessage = new MetaMessage();
                        metaMessage.setMessage(metaType, metaData, metaLength);
                        message = metaMessage;
                        if (metaType == 0x2F) {
                            // end of track means it!
                            endOfTrackFound = true;
                        }
                        break;
                    default:
                        throw new InvalidMidiDataException("Invalid status byte: " + status);
                    } // switch sys-ex or meta
                    break;
                default:
                    throw new InvalidMidiDataException("Invalid status byte: " + status);
                } // switch
                track.add(new MidiEvent(message, tick));
            } // while
        } catch (ArrayIndexOutOfBoundsException e) {
            if (DEBUG) e.printStackTrace();
            // fix for 4834374
            throw new EOFException("invalid MIDI file");
        }
    }
}
