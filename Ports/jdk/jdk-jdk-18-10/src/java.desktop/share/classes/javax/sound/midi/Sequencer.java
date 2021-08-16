/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;

/**
 * A hardware or software device that plays back a MIDI
 * {@link Sequence sequence} is known as a <em>sequencer</em>. A MIDI sequence
 * contains lists of time-stamped MIDI data, such as might be read from a
 * standard MIDI file. Most sequencers also provide functions for creating and
 * editing sequences.
 * <p>
 * The {@code Sequencer} interface includes methods for the following basic MIDI
 * sequencer operations:
 * <ul>
 *   <li>obtaining a sequence from MIDI file data
 *   <li>starting and stopping playback
 *   <li>moving to an arbitrary position in the sequence
 *   <li>changing the tempo (speed) of playback
 *   <li>synchronizing playback to an internal clock or to received MIDI
 *   messages
 *   <li>controlling the timing of another device
 * </ul>
 * In addition, the following operations are supported, either directly, or
 * indirectly through objects that the {@code Sequencer} has access to:
 * <ul>
 *   <li>editing the data by adding or deleting individual MIDI events or entire
 *   tracks
 *   <li>muting or soloing individual tracks in the sequence
 *   <li>notifying listener objects about any meta-events or control-change
 *   events encountered while playing back the sequence
 * </ul>
 *
 * @author Kara Kytle
 * @author Florian Bomers
 * @see SyncMode
 * @see #addMetaEventListener
 * @see ControllerEventListener
 * @see Receiver
 * @see Transmitter
 * @see MidiDevice
 */
public interface Sequencer extends MidiDevice {

    /**
     * A value indicating that looping should continue indefinitely rather than
     * complete after a specific number of loops.
     *
     * @see #setLoopCount
     * @since 1.5
     */
    int LOOP_CONTINUOUSLY = -1;

    /**
     * Sets the current sequence on which the sequencer operates.
     * <p>
     * This method can be called even if the {@code Sequencer} is closed.
     *
     * @param  sequence the sequence to be loaded
     * @throws InvalidMidiDataException if the sequence contains invalid MIDI
     *         data, or is not supported
     */
    void setSequence(Sequence sequence) throws InvalidMidiDataException;

    /**
     * Sets the current sequence on which the sequencer operates. The stream
     * must point to MIDI file data.
     * <p>
     * This method can be called even if the {@code Sequencer} is closed.
     *
     * @param  stream stream containing MIDI file data
     * @throws IOException if an I/O exception occurs during reading of the
     *         stream
     * @throws InvalidMidiDataException if invalid data is encountered in the
     *         stream, or the stream is not supported
     */
    void setSequence(InputStream stream)
            throws IOException, InvalidMidiDataException;

    /**
     * Obtains the sequence on which the Sequencer is currently operating.
     * <p>
     * This method can be called even if the {@code Sequencer} is closed.
     *
     * @return the current sequence, or {@code null} if no sequence is currently
     *         set
     */
    Sequence getSequence();

    /**
     * Starts playback of the MIDI data in the currently loaded sequence.
     * Playback will begin from the current position. If the playback position
     * reaches the loop end point, and the loop count is greater than 0,
     * playback will resume at the loop start point for the number of
     * repetitions set with {@code setLoopCount}. After that, or if the loop
     * count is 0, playback will continue to play to the end of the sequence.
     * <p>
     * The implementation ensures that the synthesizer is brought to a
     * consistent state when jumping to the loop start point by sending
     * appropriate controllers, pitch bend, and program change events.
     *
     * @throws IllegalStateException if the {@code Sequencer} is closed
     * @see #setLoopStartPoint
     * @see #setLoopEndPoint
     * @see #setLoopCount
     * @see #stop
     */
    void start();

    /**
     * Stops recording, if active, and playback of the currently loaded
     * sequence, if any.
     *
     * @throws IllegalStateException if the {@code Sequencer} is closed
     * @see #start
     * @see #isRunning
     */
    void stop();

    /**
     * Indicates whether the Sequencer is currently running. The default is
     * {@code false}. The Sequencer starts running when either{@link #start} or
     * {@link #startRecording} is called. {@code isRunning} then returns
     * {@code true} until playback of the sequence completes or {@link #stop} is
     * called.
     *
     * @return {@code true} if the Sequencer is running, otherwise {@code false}
     */
    boolean isRunning();

    /**
     * Starts recording and playback of MIDI data. Data is recorded to all
     * enabled tracks, on the channel(s) for which they were enabled. Recording
     * begins at the current position of the sequencer. Any events already in
     * the track are overwritten for the duration of the recording session.
     * Events from the currently loaded sequence, if any, are delivered to the
     * sequencer's transmitter(s) along with messages received during recording.
     * <p>
     * Note that tracks are not by default enabled for recording. In order to
     * record MIDI data, at least one track must be specifically enabled for
     * recording.
     *
     * @throws IllegalStateException if the {@code Sequencer} is closed
     * @see #recordEnable
     * @see #recordDisable
     */
    void startRecording();

    /**
     * Stops recording, if active. Playback of the current sequence continues.
     *
     * @throws IllegalStateException if the {@code Sequencer} is closed
     * @see #startRecording
     * @see #isRecording
     */
    void stopRecording();

    /**
     * Indicates whether the Sequencer is currently recording. The default is
     * {@code false}. The Sequencer begins recording when
     * {@link #startRecording} is called, and then returns {@code true} until
     * {@link #stop} or {@link #stopRecording} is called.
     *
     * @return {@code true} if the Sequencer is recording, otherwise
     *         {@code false}
     */
    boolean isRecording();

    /**
     * Prepares the specified track for recording events received on a
     * particular channel. Once enabled, a track will receive events when
     * recording is active.
     *
     * @param  track the track to which events will be recorded
     * @param  channel the channel on which events will be received. If -1 is
     *         specified for the channel value, the track will receive data from
     *         all channels.
     * @throws IllegalArgumentException thrown if the track is not part of the
     *         current sequence
     */
    void recordEnable(Track track, int channel);

    /**
     * Disables recording to the specified track. Events will no longer be
     * recorded into this track.
     *
     * @param  track the track to disable for recording, or {@code null} to
     *         disable recording for all tracks
     */
    void recordDisable(Track track);

    /**
     * Obtains the current tempo, expressed in beats per minute. The actual
     * tempo of playback is the product of the returned value and the tempo
     * factor.
     *
     * @return the current tempo in beats per minute
     * @see #getTempoFactor
     * @see #setTempoInBPM(float)
     * @see #getTempoInMPQ
     */
    float getTempoInBPM();

    /**
     * Sets the tempo in beats per minute. The actual tempo of playback is the
     * product of the specified value and the tempo factor.
     *
     * @param  bpm desired new tempo in beats per minute
     * @see #getTempoFactor
     * @see #setTempoInMPQ(float)
     * @see #getTempoInBPM
     */
    void setTempoInBPM(float bpm);

    /**
     * Obtains the current tempo, expressed in microseconds per quarter note.
     * The actual tempo of playback is the product of the returned value and the
     * tempo factor.
     *
     * @return the current tempo in microseconds per quarter note
     * @see #getTempoFactor
     * @see #setTempoInMPQ(float)
     * @see #getTempoInBPM
     */
    float getTempoInMPQ();

    /**
     * Sets the tempo in microseconds per quarter note. The actual tempo of
     * playback is the product of the specified value and the tempo factor.
     *
     * @param  mpq desired new tempo in microseconds per quarter note
     * @see #getTempoFactor
     * @see #setTempoInBPM(float)
     * @see #getTempoInMPQ
     */
    void setTempoInMPQ(float mpq);

    /**
     * Scales the sequencer's actual playback tempo by the factor provided. The
     * default is 1.0. A value of 1.0 represents the natural rate (the tempo
     * specified in the sequence), 2.0 means twice as fast, etc. The tempo
     * factor does not affect the values returned by {@link #getTempoInMPQ} and
     * {@link #getTempoInBPM}. Those values indicate the tempo prior to scaling.
     * <p>
     * Note that the tempo factor cannot be adjusted when external
     * synchronization is used. In that situation, {@code setTempoFactor} always
     * sets the tempo factor to 1.0.
     *
     * @param  factor the requested tempo scalar
     * @see #getTempoFactor
     */
    void setTempoFactor(float factor);

    /**
     * Returns the current tempo factor for the sequencer. The default is 1.0.
     *
     * @return tempo factor
     * @see #setTempoFactor(float)
     */
    float getTempoFactor();

    /**
     * Obtains the length of the current sequence, expressed in MIDI ticks, or 0
     * if no sequence is set.
     *
     * @return length of the sequence in ticks
     */
    long getTickLength();

    /**
     * Obtains the current position in the sequence, expressed in MIDI ticks.
     * (The duration of a tick in seconds is determined both by the tempo and by
     * the timing resolution stored in the {@link Sequence}.)
     *
     * @return current tick
     * @see #setTickPosition
     */
    long getTickPosition();

    /**
     * Sets the current sequencer position in MIDI ticks.
     *
     * @param  tick the desired tick position
     * @see #getTickPosition
     */
    void setTickPosition(long tick);

    /**
     * Obtains the length of the current sequence, expressed in microseconds, or
     * 0 if no sequence is set.
     *
     * @return length of the sequence in microseconds
     */
    long getMicrosecondLength();

    /**
     * Obtains the current position in the sequence, expressed in microseconds.
     *
     * @return the current position in microseconds
     * @see #setMicrosecondPosition
     */
    @Override
    long getMicrosecondPosition();

    /**
     * Sets the current position in the sequence, expressed in microseconds.
     *
     * @param  microseconds desired position in microseconds
     * @see #getMicrosecondPosition
     */
    void setMicrosecondPosition(long microseconds);

    /**
     * Sets the source of timing information used by this sequencer. The
     * sequencer synchronizes to the master, which is the internal clock, MIDI
     * clock, or MIDI time code, depending on the value of {@code sync}. The
     * {@code sync} argument must be one of the supported modes, as returned by
     * {@link #getMasterSyncModes}.
     *
     * @param  sync the desired master synchronization mode
     * @see SyncMode#INTERNAL_CLOCK
     * @see SyncMode#MIDI_SYNC
     * @see SyncMode#MIDI_TIME_CODE
     * @see #getMasterSyncMode
     */
    void setMasterSyncMode(SyncMode sync);

    /**
     * Obtains the current master synchronization mode for this sequencer.
     *
     * @return the current master synchronization mode
     * @see #setMasterSyncMode(SyncMode)
     * @see #getMasterSyncModes
     */
    SyncMode getMasterSyncMode();

    /**
     * Obtains the set of master synchronization modes supported by this
     * sequencer.
     *
     * @return the available master synchronization modes
     * @see SyncMode#INTERNAL_CLOCK
     * @see SyncMode#MIDI_SYNC
     * @see SyncMode#MIDI_TIME_CODE
     * @see #getMasterSyncMode
     * @see #setMasterSyncMode(SyncMode)
     */
    SyncMode[] getMasterSyncModes();

    /**
     * Sets the slave synchronization mode for the sequencer. This indicates the
     * type of timing information sent by the sequencer to its receiver. The
     * {@code sync} argument must be one of the supported modes, as returned by
     * {@link #getSlaveSyncModes}.
     *
     * @param  sync the desired slave synchronization mode
     * @see SyncMode#MIDI_SYNC
     * @see SyncMode#MIDI_TIME_CODE
     * @see SyncMode#NO_SYNC
     * @see #getSlaveSyncModes
     */
    void setSlaveSyncMode(SyncMode sync);

    /**
     * Obtains the current slave synchronization mode for this sequencer.
     *
     * @return the current slave synchronization mode
     * @see #setSlaveSyncMode(SyncMode)
     * @see #getSlaveSyncModes
     */
    SyncMode getSlaveSyncMode();

    /**
     * Obtains the set of slave synchronization modes supported by the
     * sequencer.
     *
     * @return the available slave synchronization modes
     * @see SyncMode#MIDI_SYNC
     * @see SyncMode#MIDI_TIME_CODE
     * @see SyncMode#NO_SYNC
     */
    SyncMode[] getSlaveSyncModes();

    /**
     * Sets the mute state for a track. This method may fail for a number of
     * reasons. For example, the track number specified may not be valid for the
     * current sequence, or the sequencer may not support this functionality. An
     * application which needs to verify whether this operation succeeded should
     * follow this call with a call to {@link #getTrackMute}.
     *
     * @param  track the track number. Tracks in the current sequence are
     *         numbered from 0 to the number of tracks in the sequence minus 1.
     * @param  mute the new mute state for the track. {@code true} implies the
     *         track should be muted, {@code false} implies the track should be
     *         unmuted.
     * @see #getSequence
     */
    void setTrackMute(int track, boolean mute);

    /**
     * Obtains the current mute state for a track. The default mute state for
     * all tracks which have not been muted is false. In any case where the
     * specified track has not been muted, this method should return false. This
     * applies if the sequencer does not support muting of tracks, and if the
     * specified track index is not valid.
     *
     * @param  track the track number. Tracks in the current sequence are
     *         numbered from 0 to the number of tracks in the sequence minus 1.
     * @return {@code true} if muted, {@code false} if not
     */
    boolean getTrackMute(int track);

    /**
     * Sets the solo state for a track. If {@code solo} is {@code true} only
     * this track and other solo'd tracks will sound. If {@code solo} is
     * {@code false} then only other solo'd tracks will sound, unless no tracks
     * are solo'd in which case all un-muted tracks will sound.
     * <p>
     * This method may fail for a number of reasons. For example, the track
     * number specified may not be valid for the current sequence, or the
     * sequencer may not support this functionality. An application which needs
     * to verify whether this operation succeeded should follow this call with a
     * call to {@link #getTrackSolo}.
     *
     * @param  track the track number. Tracks in the current sequence are
     *         numbered from 0 to the number of tracks in the sequence minus 1.
     * @param  solo the new solo state for the track. {@code true} implies the
     *         track should be solo'd, {@code false} implies the track should
     *         not be solo'd.
     * @see #getSequence
     */
    void setTrackSolo(int track, boolean solo);

    /**
     * Obtains the current solo state for a track. The default mute state for
     * all tracks which have not been solo'd is false. In any case where the
     * specified track has not been solo'd, this method should return false.
     * This applies if the sequencer does not support soloing of tracks, and if
     * the specified track index is not valid.
     *
     * @param  track the track number. Tracks in the current sequence are
     *         numbered from 0 to the number of tracks in the sequence minus 1.
     * @return {@code true} if solo'd, {@code false} if not
     */
    boolean getTrackSolo(int track);

    /**
     * Registers a meta-event listener to receive notification whenever a
     * meta-event is encountered in the sequence and processed by the sequencer.
     * This method can fail if, for instance,this class of sequencer does not
     * support meta-event notification.
     *
     * @param  listener listener to add
     * @return {@code true} if the listener was successfully added, otherwise
     *         {@code false}
     * @see #removeMetaEventListener
     * @see MetaEventListener
     * @see MetaMessage
     */
    boolean addMetaEventListener(MetaEventListener listener);

    /**
     * Removes the specified meta-event listener from this sequencer's list of
     * registered listeners, if in fact the listener is registered.
     *
     * @param  listener the meta-event listener to remove
     * @see #addMetaEventListener
     */
    void removeMetaEventListener(MetaEventListener listener);

    /**
     * Registers a controller event listener to receive notification whenever
     * the sequencer processes a control-change event of the requested type or
     * types. The types are specified by the {@code controllers} argument, which
     * should contain an array of MIDI controller numbers. (Each number should
     * be between 0 and 127, inclusive. See the MIDI 1.0 Specification for the
     * numbers that correspond to various types of controllers.)
     * <p>
     * The returned array contains the MIDI controller numbers for which the
     * listener will now receive events. Some sequencers might not support
     * controller event notification, in which case the array has a length of 0.
     * Other sequencers might support notification for some controllers but not
     * all. This method may be invoked repeatedly. Each time, the returned array
     * indicates all the controllers that the listener will be notified about,
     * not only the controllers requested in that particular invocation.
     *
     * @param  listener the controller event listener to add to the list of
     *         registered listeners
     * @param  controllers the MIDI controller numbers for which change
     *         notification is requested
     * @return the numbers of all the MIDI controllers whose changes will now be
     *         reported to the specified listener
     * @see #removeControllerEventListener
     * @see ControllerEventListener
     */
    int[] addControllerEventListener(ControllerEventListener listener,
                                     int[] controllers);

    /**
     * Removes a controller event listener's interest in one or more types of
     * controller event. The {@code controllers} argument is an array of MIDI
     * numbers corresponding to the controllers for which the listener should no
     * longer receive change notifications. To completely remove this listener
     * from the list of registered listeners, pass in {@code null} for
     * {@code controllers}. The returned array contains the MIDI controller
     * numbers for which the listener will now receive events. The array has a
     * length of 0 if the listener will not receive change notifications for any
     * controllers.
     *
     * @param  listener old listener
     * @param  controllers the MIDI controller numbers for which change
     *         notification should be cancelled, or {@code null} to cancel for
     *         all controllers
     * @return the numbers of all the MIDI controllers whose changes will now be
     *         reported to the specified listener
     * @see #addControllerEventListener
     */
    int[] removeControllerEventListener(ControllerEventListener listener,
                                        int[] controllers);

    /**
     * Sets the first MIDI tick that will be played in the loop. If the loop
     * count is greater than 0, playback will jump to this point when reaching
     * the loop end point.
     * <p>
     * A value of 0 for the starting point means the beginning of the loaded
     * sequence. The starting point must be lower than or equal to the ending
     * point, and it must fall within the size of the loaded sequence.
     * <p>
     * A sequencer's loop start point defaults to start of the sequence.
     *
     * @param  tick the loop's starting position, in MIDI ticks (zero-based)
     * @throws IllegalArgumentException if the requested loop start point cannot
     *         be set, usually because it falls outside the sequence's duration
     *         or because the start point is after the end point
     * @see #setLoopEndPoint
     * @see #setLoopCount
     * @see #getLoopStartPoint
     * @see #start
     * @since 1.5
     */
    void setLoopStartPoint(long tick);

    /**
     * Obtains the start position of the loop, in MIDI ticks.
     *
     * @return the start position of the loop, in MIDI ticks (zero-based)
     * @see #setLoopStartPoint
     * @since 1.5
     */
    long getLoopStartPoint();

    /**
     * Sets the last MIDI tick that will be played in the loop. If the loop
     * count is 0, the loop end point has no effect and playback continues to
     * play when reaching the loop end point.
     * <p>
     * A value of -1 for the ending point indicates the last tick of the
     * sequence. Otherwise, the ending point must be greater than or equal to
     * the starting point, and it must fall within the size of the loaded
     * sequence.
     * <p>
     * A sequencer's loop end point defaults to -1, meaning the end of the
     * sequence.
     *
     * @param  tick the loop's ending position, in MIDI ticks (zero-based), or
     *         -1 to indicate the final tick
     * @throws IllegalArgumentException if the requested loop point cannot be
     *         set, usually because it falls outside the sequence's duration or
     *         because the ending point is before the starting point
     * @see #setLoopStartPoint
     * @see #setLoopCount
     * @see #getLoopEndPoint
     * @see #start
     * @since 1.5
     */
    void setLoopEndPoint(long tick);

    /**
     * Obtains the end position of the loop, in MIDI ticks.
     *
     * @return the end position of the loop, in MIDI ticks (zero-based), or -1
     *         to indicate the end of the sequence
     * @see #setLoopEndPoint
     * @since 1.5
     */
    long getLoopEndPoint();

    /**
     * Sets the number of repetitions of the loop for playback. When the
     * playback position reaches the loop end point, it will loop back to the
     * loop start point {@code count} times, after which playback will continue
     * to play to the end of the sequence.
     * <p>
     * If the current position when this method is invoked is greater than the
     * loop end point, playback continues to the end of the sequence without
     * looping, unless the loop end point is changed subsequently.
     * <p>
     * A {@code count} value of 0 disables looping: playback will continue at
     * the loop end point, and it will not loop back to the loop start point.
     * This is a sequencer's default.
     * <p>
     * If playback is stopped during looping, the current loop status is
     * cleared; subsequent start requests are not affected by an interrupted
     * loop operation.
     *
     * @param  count the number of times playback should loop back from the
     *         loop's end position to the loop's start position, or
     *         {@link #LOOP_CONTINUOUSLY} to indicate that looping should
     *         continue until interrupted
     * @throws IllegalArgumentException if {@code count} is negative and not
     *         equal to {@link #LOOP_CONTINUOUSLY}
     * @see #setLoopStartPoint
     * @see #setLoopEndPoint
     * @see #getLoopCount
     * @see #start
     * @since 1.5
     */
    void setLoopCount(int count);

    /**
     * Obtains the number of repetitions for playback.
     *
     * @return the number of loops after which playback plays to the end of the
     *         sequence
     * @see #setLoopCount
     * @see #start
     * @since 1.5
     */
    int getLoopCount();

    /**
     * A {@code SyncMode} object represents one of the ways in which a MIDI
     * sequencer's notion of time can be synchronized with a master or slave
     * device. If the sequencer is being synchronized to a master, the sequencer
     * revises its current time in response to messages from the master. If the
     * sequencer has a slave, the sequencer similarly sends messages to control
     * the slave's timing.
     * <p>
     * There are three predefined modes that specify possible masters for a
     * sequencer: {@code INTERNAL_CLOCK}, {@code MIDI_SYNC}, and
     * {@code MIDI_TIME_CODE}. The latter two work if the sequencer receives
     * MIDI messages from another device. In these two modes, the sequencer's
     * time gets reset based on system real-time timing clock messages or MIDI
     * time code (MTC) messages, respectively. These two modes can also be used
     * as slave modes, in which case the sequencer sends the corresponding types
     * of MIDI messages to its receiver (whether or not the sequencer is also
     * receiving them from a master). A fourth mode, {@code NO_SYNC}, is used to
     * indicate that the sequencer should not control its receiver's timing.
     *
     * @see Sequencer#setMasterSyncMode(SyncMode)
     * @see Sequencer#setSlaveSyncMode(SyncMode)
     */
    class SyncMode {

        /**
         * Synchronization mode name.
         */
        private final String name;

        /**
         * Constructs a synchronization mode.
         *
         * @param  name name of the synchronization mode
         */
        protected SyncMode(String name) {

            this.name = name;
        }

        /**
         * Indicates whether the specified object is equal to this
         * synchronization mode, returning {@code true} if the objects are the
         * same.
         *
         * @param  obj the reference object with which to compare
         * @return {@code true} if the specified object is equal to this
         *         synchronization mode; {@code false} otherwise
         */
        @Override
        public final boolean equals(Object obj) {

            return super.equals(obj);
        }

        /**
         * Returns a hash code value for this synchronization mode.
         *
         * @return a hash code value for this synchronization mode
         */
        @Override
        public final int hashCode() {

            return super.hashCode();
        }

        /**
         * Returns mode's name as the string representation of the
         * synchronization mode.
         *
         * @return a string representation of the synchronization mode
         */
        @Override
        public final String toString() {
            return name;
        }

        /**
         * A master synchronization mode that makes the sequencer get its timing
         * information from its internal clock. This is not a legal slave sync
         * mode.
         */
        public static final SyncMode INTERNAL_CLOCK = new SyncMode("Internal Clock");

        /**
         * A master or slave synchronization mode that specifies the use of MIDI
         * clock messages. If this mode is used as the master sync mode, the
         * sequencer gets its timing information from system real-time MIDI
         * clock messages. This mode only applies as the master sync mode for
         * sequencers that are also MIDI receivers. If this is the slave sync
         * mode, the sequencer sends system real-time MIDI clock messages to its
         * receiver. MIDI clock messages are sent at a rate of 24 per quarter
         * note.
         */
        public static final SyncMode MIDI_SYNC = new SyncMode("MIDI Sync");

        /**
         * A master or slave synchronization mode that specifies the use of MIDI
         * Time Code. If this mode is used as the master sync mode, the
         * sequencer gets its timing information from MIDI Time Code messages.
         * This mode only applies as the master sync mode to sequencers that are
         * also MIDI receivers. If this mode is used as the slave sync mode, the
         * sequencer sends MIDI Time Code messages to its receiver. (See the
         * MIDI 1.0 Detailed Specification for a description of MIDI Time Code.)
         */
        public static final SyncMode MIDI_TIME_CODE = new SyncMode("MIDI Time Code");

        /**
         * A slave synchronization mode indicating that no timing information
         * should be sent to the receiver. This is not a legal master sync mode.
         */
        public static final SyncMode NO_SYNC = new SyncMode("No Timing");

    }
}
