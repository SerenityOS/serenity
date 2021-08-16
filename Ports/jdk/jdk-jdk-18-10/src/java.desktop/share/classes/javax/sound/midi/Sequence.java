/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Vector;

/**
 * A {@code Sequence} is a data structure containing musical information (often
 * an entire song or composition) that can be played back by a {@link Sequencer}
 * object. Specifically, the {@code Sequence} contains timing information and
 * one or more tracks. Each {@link Track track} consists of a series of MIDI
 * events (such as note-ons, note-offs, program changes, and meta-events). The
 * sequence's timing information specifies the type of unit that is used to
 * time-stamp the events in the sequence.
 * <p>
 * A {@code Sequence} can be created from a MIDI file by reading the file into
 * an input stream and invoking one of the {@code getSequence} methods of
 * {@link MidiSystem}. A sequence can also be built from scratch by adding new
 * {@code Tracks} to an empty {@code Sequence}, and adding {@link MidiEvent}
 * objects to these {@code Tracks}.
 *
 * @author Kara Kytle
 * @see Sequencer#setSequence(java.io.InputStream stream)
 * @see Sequencer#setSequence(Sequence sequence)
 * @see Track#add(MidiEvent)
 * @see MidiFileFormat
 */
public class Sequence {

    // Timing types

    /**
     * The tempo-based timing type, for which the resolution is expressed in
     * pulses (ticks) per quarter note.
     *
     * @see #Sequence(float, int)
     */
    public static final float PPQ = 0.0f;

    /**
     * The SMPTE-based timing type with 24 frames per second (resolution is
     * expressed in ticks per frame).
     *
     * @see #Sequence(float, int)
     */
    public static final float SMPTE_24 = 24.0f;

    /**
     * The SMPTE-based timing type with 25 frames per second (resolution is
     * expressed in ticks per frame).
     *
     * @see #Sequence(float, int)
     */
    public static final float SMPTE_25 = 25.0f;

    /**
     * The SMPTE-based timing type with 29.97 frames per second (resolution is
     * expressed in ticks per frame).
     *
     * @see #Sequence(float, int)
     */
    public static final float SMPTE_30DROP = 29.97f;

    /**
     * The SMPTE-based timing type with 30 frames per second (resolution is
     * expressed in ticks per frame).
     *
     * @see #Sequence(float, int)
     */
    public static final float SMPTE_30 = 30.0f;

    // Variables

    /**
     * The timing division type of the sequence.
     *
     * @see #PPQ
     * @see #SMPTE_24
     * @see #SMPTE_25
     * @see #SMPTE_30DROP
     * @see #SMPTE_30
     * @see #getDivisionType
     */
    protected float divisionType;

    /**
     * The timing resolution of the sequence.
     *
     * @see #getResolution
     */
    protected int resolution;

    /**
     * The MIDI tracks in this sequence.
     *
     * @see #getTracks
     */
    protected Vector<Track> tracks = new Vector<>();

    /**
     * Constructs a new MIDI sequence with the specified timing division type
     * and timing resolution. The division type must be one of the recognized
     * MIDI timing types. For tempo-based timing, {@code divisionType} is PPQ
     * (pulses per quarter note) and the resolution is specified in ticks per
     * beat. For SMTPE timing, {@code divisionType} specifies the number of
     * frames per second and the resolution is specified in ticks per frame. The
     * sequence will contain no initial tracks. Tracks may be added to or
     * removed from the sequence using {@link #createTrack} and
     * {@link #deleteTrack}.
     *
     * @param  divisionType the timing division type (PPQ or one of the SMPTE
     *         types)
     * @param  resolution the timing resolution
     * @throws InvalidMidiDataException if {@code divisionType} is not valid
     * @see #PPQ
     * @see #SMPTE_24
     * @see #SMPTE_25
     * @see #SMPTE_30DROP
     * @see #SMPTE_30
     * @see #getDivisionType
     * @see #getResolution
     * @see #getTracks
     */
    public Sequence(float divisionType, int resolution) throws InvalidMidiDataException {

        if (divisionType == PPQ)
            this.divisionType = PPQ;
        else if (divisionType == SMPTE_24)
            this.divisionType = SMPTE_24;
        else if (divisionType == SMPTE_25)
            this.divisionType = SMPTE_25;
        else if (divisionType == SMPTE_30DROP)
            this.divisionType = SMPTE_30DROP;
        else if (divisionType == SMPTE_30)
            this.divisionType = SMPTE_30;
        else throw new InvalidMidiDataException("Unsupported division type: " + divisionType);

        this.resolution = resolution;
    }

    /**
     * Constructs a new MIDI sequence with the specified timing division type,
     * timing resolution, and number of tracks. The division type must be one of
     * the recognized MIDI timing types. For tempo-based timing,
     * {@code divisionType} is PPQ (pulses per quarter note) and the resolution
     * is specified in ticks per beat. For SMTPE timing, {@code divisionType}
     * specifies the number of frames per second and the resolution is specified
     * in ticks per frame. The sequence will be initialized with the number of
     * tracks specified by {@code numTracks}. These tracks are initially empty
     * (i.e. they contain only the meta-event End of Track). The tracks may be
     * retrieved for editing using the {@link #getTracks} method. Additional
     * tracks may be added, or existing tracks removed, using
     * {@link #createTrack} and {@link #deleteTrack}.
     *
     * @param  divisionType the timing division type (PPQ or one of the SMPTE
     *         types)
     * @param  resolution the timing resolution
     * @param  numTracks the initial number of tracks in the sequence
     * @throws InvalidMidiDataException if {@code divisionType} is not valid
     * @see #PPQ
     * @see #SMPTE_24
     * @see #SMPTE_25
     * @see #SMPTE_30DROP
     * @see #SMPTE_30
     * @see #getDivisionType
     * @see #getResolution
     */
    public Sequence(float divisionType, int resolution, int numTracks) throws InvalidMidiDataException {

        if (divisionType == PPQ)
            this.divisionType = PPQ;
        else if (divisionType == SMPTE_24)
            this.divisionType = SMPTE_24;
        else if (divisionType == SMPTE_25)
            this.divisionType = SMPTE_25;
        else if (divisionType == SMPTE_30DROP)
            this.divisionType = SMPTE_30DROP;
        else if (divisionType == SMPTE_30)
            this.divisionType = SMPTE_30;
        else throw new InvalidMidiDataException("Unsupported division type: " + divisionType);

        this.resolution = resolution;

        for (int i = 0; i < numTracks; i++) {
            tracks.addElement(new Track());
        }
    }

    /**
     * Obtains the timing division type for this sequence.
     *
     * @return the division type (PPQ or one of the SMPTE types)
     * @see #PPQ
     * @see #SMPTE_24
     * @see #SMPTE_25
     * @see #SMPTE_30DROP
     * @see #SMPTE_30
     * @see #Sequence(float, int)
     * @see MidiFileFormat#getDivisionType()
     */
    public float getDivisionType() {
        return divisionType;
    }

    /**
     * Obtains the timing resolution for this sequence. If the sequence's
     * division type is PPQ, the resolution is specified in ticks per beat. For
     * SMTPE timing, the resolution is specified in ticks per frame.
     *
     * @return the number of ticks per beat (PPQ) or per frame (SMPTE)
     * @see #getDivisionType
     * @see #Sequence(float, int)
     * @see MidiFileFormat#getResolution()
     */
    public int getResolution() {
        return resolution;
    }

    /**
     * Creates a new, initially empty track as part of this sequence. The track
     * initially contains the meta-event End of Track. The newly created track
     * is returned. All tracks in the sequence may be retrieved using
     * {@link #getTracks}. Tracks may be removed from the sequence using
     * {@link #deleteTrack}.
     *
     * @return the newly created track
     */
    public Track createTrack() {

        Track track = new Track();
        tracks.addElement(track);

        return track;
    }

    /**
     * Removes the specified track from the sequence.
     *
     * @param  track the track to remove
     * @return {@code true} if the track existed in the track and was removed,
     *         otherwise {@code false}
     * @see #createTrack
     * @see #getTracks
     */
    public boolean deleteTrack(Track track) {
        return tracks.removeElement(track);
    }

    /**
     * Obtains an array containing all the tracks in this sequence. If the
     * sequence contains no tracks, an array of length 0 is returned.
     *
     * @return the array of tracks
     * @see #createTrack
     * @see #deleteTrack
     */
    public Track[] getTracks() {
        // Creation of the non-empty array will be synchronized inside toArray()
        return tracks.toArray(new Track[0]);
    }

    /**
     * Obtains the duration of this sequence, expressed in microseconds.
     *
     * @return this sequence's duration in microseconds
     */
    public long getMicrosecondLength() {

        return com.sun.media.sound.MidiUtils.tick2microsecond(this, getTickLength(), null);
    }

    /**
     * Obtains the duration of this sequence, expressed in MIDI ticks.
     *
     * @return this sequence's length in ticks
     * @see #getMicrosecondLength
     */
    public long getTickLength() {

        long length = 0;

        synchronized(tracks) {

            for(int i=0; i<tracks.size(); i++ ) {
                long temp = tracks.elementAt(i).ticks();
                if( temp>length ) {
                    length = temp;
                }
            }
            return length;
        }
    }

    /**
     * Obtains a list of patches referenced in this sequence. This patch list
     * may be used to load the required {@link Instrument} objects into a
     * {@link Synthesizer}.
     *
     * @return an array of {@link Patch} objects used in this sequence
     * @see Synthesizer#loadInstruments(Soundbank, Patch[])
     */
    public Patch[] getPatchList() {

        // $$kk: 04.09.99: need to implement!!
        return new Patch[0];
    }
}
