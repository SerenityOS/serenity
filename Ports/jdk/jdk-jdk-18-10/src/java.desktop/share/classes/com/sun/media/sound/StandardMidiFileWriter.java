/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.SequenceInputStream;
import java.util.Objects;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.sound.midi.spi.MidiFileWriter;

/**
 * MIDI file writer.
 *
 * @author Kara Kytle
 * @author Jan Borgersen
 */
public final class StandardMidiFileWriter extends MidiFileWriter {

    private static final int MThd_MAGIC = 0x4d546864;  // 'MThd'
    private static final int MTrk_MAGIC = 0x4d54726b;  // 'MTrk'

    private static final int ONE_BYTE   = 1;
    private static final int TWO_BYTE   = 2;
    private static final int SYSEX      = 3;
    private static final int META       = 4;
    private static final int ERROR      = 5;
    private static final int IGNORE     = 6;

    private static final int MIDI_TYPE_0 = 0;
    private static final int MIDI_TYPE_1 = 1;

    private DataOutputStream tddos;               // data output stream for track writing

    /**
     * MIDI parser types.
     */
    private static final int[] types = {
        MIDI_TYPE_0,
        MIDI_TYPE_1
    };

    @Override
    public int[] getMidiFileTypes() {
        int[] localArray = new int[types.length];
        System.arraycopy(types, 0, localArray, 0, types.length);
        return localArray;
    }

    /**
     * Obtains the file types that this provider can write from the
     * sequence specified.
     * @param sequence the sequence for which midi file type support
     * is queried
     * @return array of file types.  If no file types are supported,
     * returns an array of length 0.
     */
    @Override
    public int[] getMidiFileTypes(Sequence sequence){
        int[] typesArray;
        Track[] tracks = sequence.getTracks();

        if( tracks.length==1 ) {
            typesArray = new int[2];
            typesArray[0] = MIDI_TYPE_0;
            typesArray[1] = MIDI_TYPE_1;
        } else {
            typesArray = new int[1];
            typesArray[0] = MIDI_TYPE_1;
        }

        return typesArray;
    }

    @Override
    public int write(Sequence in, int type, OutputStream out) throws IOException {
        Objects.requireNonNull(out);
        if (!isFileTypeSupported(type, in)) {
            throw new IllegalArgumentException("Could not write MIDI file");
        }
        // First get the fileStream from this sequence
        InputStream fileStream = getFileStream(type,in);
        if (fileStream == null) {
            throw new IllegalArgumentException("Could not write MIDI file");
        }
        long bytesWritten = fileStream.transferTo(out);
        // Done....return bytesWritten
        return (int) bytesWritten;
    }

    @Override
    public int write(Sequence in, int type, File out) throws IOException {
        Objects.requireNonNull(in);
        FileOutputStream fos = new FileOutputStream(out); // throws IOException
        int bytesWritten = write( in, type, fos );
        fos.close();
        return bytesWritten;
    }

    //=================================================================================

    private InputStream getFileStream(int type, Sequence sequence) throws IOException {
        Track[] tracks = sequence.getTracks();
        int bytesBuilt = 0;
        int headerLength = 14;
        int length = 0;
        int timeFormat;
        float divtype;

        PipedOutputStream   hpos = null;
        DataOutputStream    hdos = null;
        PipedInputStream    headerStream = null;

        InputStream[]         trackStreams  = null;
        InputStream         trackStream = null;
        InputStream fStream = null;

        // Determine the filetype to write
        if( type==MIDI_TYPE_0 ) {
            if (tracks.length != 1) {
                return null;
            }
        } else if( type==MIDI_TYPE_1 ) {
            if (tracks.length < 1) { // $$jb: 05.31.99: we _can_ write TYPE_1 if tracks.length==1
                return null;
            }
        } else {
            if(tracks.length==1) {
                type = MIDI_TYPE_0;
            } else if(tracks.length>1) {
                type = MIDI_TYPE_1;
            } else {
                return null;
            }
        }

        // Now build the file one track at a time
        // Note that above we made sure that MIDI_TYPE_0 only happens
        // if tracks.length==1

        trackStreams = new InputStream[tracks.length];
        int trackCount = 0;
        for(int i=0; i<tracks.length; i++) {
            try {
                trackStreams[trackCount] = writeTrack( tracks[i], type );
                trackCount++;
            } catch (InvalidMidiDataException e) {
                if(Printer.err) Printer.err("Exception in write: " + e.getMessage());
            }
            //bytesBuilt += trackStreams[i].getLength();
        }

        // Now seqence the track streams
        if( trackCount == 1 ) {
            trackStream = trackStreams[0];
        } else if( trackCount > 1 ){
            trackStream = trackStreams[0];
            for(int i=1; i<tracks.length; i++) {
                // fix for 5048381: NullPointerException when saving a MIDI sequence
                // don't include failed track streams
                if (trackStreams[i] != null) {
                    trackStream = new SequenceInputStream( trackStream, trackStreams[i]);
                }
            }
        } else {
            throw new IllegalArgumentException("invalid MIDI data in sequence");
        }

        // Now build the header...
        hpos = new PipedOutputStream();
        hdos = new DataOutputStream(hpos);
        headerStream = new PipedInputStream(hpos);

        // Write the magic number
        hdos.writeInt( MThd_MAGIC );

        // Write the header length
        hdos.writeInt( headerLength - 8 );

        // Write the filetype
        if(type==MIDI_TYPE_0) {
            hdos.writeShort( 0 );
        } else {
            // MIDI_TYPE_1
            hdos.writeShort( 1 );
        }

        // Write the number of tracks
        hdos.writeShort( (short) trackCount );

        // Determine and write the timing format
        divtype = sequence.getDivisionType();
        if( divtype == Sequence.PPQ ) {
            timeFormat = sequence.getResolution();
        } else if( divtype == Sequence.SMPTE_24) {
            timeFormat = (24<<8) * -1;
            timeFormat += (sequence.getResolution() & 0xFF);
        } else if( divtype == Sequence.SMPTE_25) {
            timeFormat = (25<<8) * -1;
            timeFormat += (sequence.getResolution() & 0xFF);
        } else if( divtype == Sequence.SMPTE_30DROP) {
            timeFormat = (29<<8) * -1;
            timeFormat += (sequence.getResolution() & 0xFF);
        } else if( divtype == Sequence.SMPTE_30) {
            timeFormat = (30<<8) * -1;
            timeFormat += (sequence.getResolution() & 0xFF);
        } else {
            // $$jb: 04.08.99: What to really do here?
            return null;
        }
        hdos.writeShort( timeFormat );

        // now construct an InputStream to become the FileStream
        fStream = new SequenceInputStream(headerStream, trackStream);
        hdos.close();

        length = bytesBuilt + headerLength;
        return fStream;
    }

    /**
     * Returns ONE_BYTE, TWO_BYTE, SYSEX, META,
     * ERROR, or IGNORE (i.e. invalid for a MIDI file)
     */
    private int getType(int byteValue) {
        if ((byteValue & 0xF0) == 0xF0) {
            switch(byteValue) {
            case 0xF0:
            case 0xF7:
                return SYSEX;
            case 0xFF:
                return META;
            }
            return IGNORE;
        }

        switch(byteValue & 0xF0) {
        case 0x80:
        case 0x90:
        case 0xA0:
        case 0xB0:
        case 0xE0:
            return TWO_BYTE;
        case 0xC0:
        case 0xD0:
            return ONE_BYTE;
        }
        return ERROR;
    }

    private static final long mask = 0x7F;

    private int writeVarInt(long value) throws IOException {
        int len = 1;
        int shift=63; // number of bitwise left-shifts of mask
        // first screen out leading zeros
        while ((shift > 0) && ((value & (mask << shift)) == 0)) shift-=7;
        // then write actual values
        while (shift > 0) {
            tddos.writeByte((int) (((value & (mask << shift)) >> shift) | 0x80));
            shift-=7;
            len++;
        }
        tddos.writeByte((int) (value & mask));
        return len;
    }

    private InputStream writeTrack( Track track, int type ) throws IOException, InvalidMidiDataException {
        int bytesWritten = 0;
        int lastBytesWritten = 0;
        int size = track.size();
        PipedOutputStream thpos = new PipedOutputStream();
        DataOutputStream  thdos = new DataOutputStream(thpos);
        PipedInputStream  thpis = new PipedInputStream(thpos);

        ByteArrayOutputStream tdbos = new ByteArrayOutputStream();
        tddos = new DataOutputStream(tdbos);
        ByteArrayInputStream tdbis = null;

        SequenceInputStream  fStream = null;

        long currentTick = 0;
        long deltaTick = 0;
        long eventTick = 0;
        int runningStatus = -1;

        // -----------------------------
        // Write each event in the track
        // -----------------------------
        for(int i=0; i<size; i++) {
            MidiEvent event = track.get(i);

            int status;
            int eventtype;
            int metatype;
            int data1, data2;
            int length;
            byte[] data = null;
            ShortMessage shortMessage = null;
            MetaMessage  metaMessage  = null;
            SysexMessage sysexMessage = null;

            // get the tick
            // $$jb: this gets easier if we change all system-wide time to delta ticks
            eventTick = event.getTick();
            deltaTick = event.getTick() - currentTick;
            currentTick = event.getTick();

            // get the status byte
            status = event.getMessage().getStatus();
            eventtype = getType( status );

            switch( eventtype ) {
            case ONE_BYTE:
                shortMessage = (ShortMessage) event.getMessage();
                data1 = shortMessage.getData1();
                bytesWritten += writeVarInt( deltaTick );

                if(status!=runningStatus) {
                    runningStatus=status;
                    tddos.writeByte(status);  bytesWritten += 1;
                }
                tddos.writeByte(data1);   bytesWritten += 1;
                break;

            case TWO_BYTE:
                shortMessage = (ShortMessage) event.getMessage();
                data1 = shortMessage.getData1();
                data2 = shortMessage.getData2();

                bytesWritten += writeVarInt( deltaTick );
                if(status!=runningStatus) {
                    runningStatus=status;
                    tddos.writeByte(status);  bytesWritten += 1;
                }
                tddos.writeByte(data1);   bytesWritten += 1;
                tddos.writeByte(data2);   bytesWritten += 1;
                break;

            case SYSEX:
                sysexMessage = (SysexMessage) event.getMessage();
                length     = sysexMessage.getLength();
                data       = sysexMessage.getMessage();
                bytesWritten += writeVarInt( deltaTick );

                // $$jb: 04.08.99: always write status for sysex
                runningStatus=status;
                tddos.writeByte( data[0] ); bytesWritten += 1;

                // $$jb: 10.18.99: we don't maintain length in
                // the message data for SysEx (it is not transmitted
                // over the line), so write the calculated length
                // minus the status byte
                bytesWritten += writeVarInt( (data.length-1) );

                // $$jb: 10.18.99: now write the rest of the
                // message
                tddos.write(data, 1, (data.length-1));
                bytesWritten += (data.length-1);
                break;

            case META:
                metaMessage = (MetaMessage) event.getMessage();
                length    = metaMessage.getLength();
                data      = metaMessage.getMessage();
                bytesWritten += writeVarInt( deltaTick );

                // $$jb: 10.18.99: getMessage() returns the
                // entire valid midi message for a file,
                // including the status byte and the var-length-int
                // length value, so we can just write the data
                // here.  note that we must _always_ write the
                // status byte, regardless of runningStatus.
                runningStatus=status;
                tddos.write( data, 0, data.length );
                bytesWritten += data.length;
                break;

            case IGNORE:
                // ignore this event
                break;

            case ERROR:
                // ignore this event
                break;

            default:
                throw new InvalidMidiDataException("internal file writer error");
            }
        }
        // ---------------------------------
        // End write each event in the track
        // ---------------------------------

        // Build Track header now that we know length
        thdos.writeInt(MTrk_MAGIC);
        thdos.writeInt(bytesWritten);
        bytesWritten += 8;

        // Now sequence them
        tdbis = new ByteArrayInputStream( tdbos.toByteArray() );
        fStream = new SequenceInputStream(thpis,tdbis);
        thdos.close();
        tddos.close();

        return fStream;
    }
}
