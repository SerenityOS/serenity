/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.sound.sampled;

import java.io.IOException;

/**
 * The {@code Clip} interface represents a special kind of data line whose audio
 * data can be loaded prior to playback, instead of being streamed in real time.
 * <p>
 * Because the data is pre-loaded and has a known length, you can set a clip to
 * start playing at any position in its audio data. You can also create a loop,
 * so that when the clip is played it will cycle repeatedly. Loops are specified
 * with a starting and ending sample frame, along with the number of times that
 * the loop should be played.
 * <p>
 * Clips may be obtained from a {@link Mixer} that supports lines of this type.
 * Data is loaded into a clip when it is opened.
 * <p>
 * Playback of an audio clip may be started and stopped using the
 * {@link #start start} and {@link #stop stop} methods. These methods do not
 * reset the media position; {@code start} causes playback to continue from the
 * position where playback was last stopped. To restart playback from the
 * beginning of the clip's audio data, simply follow the invocation of
 * {@code stop} with {@code setFramePosition(0)}, which rewinds the media to the
 * beginning of the clip.
 *
 * @author Kara Kytle
 * @since 1.3
 */
public interface Clip extends DataLine {

    /**
     * A value indicating that looping should continue indefinitely rather than
     * complete after a specific number of loops.
     *
     * @see #loop
     */
    int LOOP_CONTINUOUSLY = -1;

    /**
     * Opens the clip, meaning that it should acquire any required system
     * resources and become operational. The clip is opened with the format and
     * audio data indicated. If this operation succeeds, the line is marked as
     * open and an {@link LineEvent.Type#OPEN OPEN} event is dispatched to the
     * line's listeners.
     * <p>
     * Invoking this method on a line which is already open is illegal and may
     * result in an {@code IllegalStateException}.
     * <p>
     * Note that some lines, once closed, cannot be reopened. Attempts to reopen
     * such a line will always result in a {@code LineUnavailableException}.
     *
     * @param  format the format of the supplied audio data
     * @param  data a byte array containing audio data to load into the clip
     * @param  offset the point at which to start copying, expressed in
     *         <em>bytes</em> from the beginning of the array
     * @param  bufferSize the number of <em>bytes</em> of data to load into the
     *         clip from the array
     * @throws LineUnavailableException if the line cannot be opened due to
     *         resource restrictions
     * @throws IllegalArgumentException if the buffer size does not represent an
     *         integral number of sample frames, or if {@code format} is not
     *         fully specified or invalid
     * @throws IllegalStateException if the line is already open
     * @throws SecurityException if the line cannot be opened due to security
     *         restrictions
     * @see #close
     * @see #isOpen
     * @see LineListener
     */
    void open(AudioFormat format, byte[] data, int offset, int bufferSize)
            throws LineUnavailableException;

    /**
     * Opens the clip with the format and audio data present in the provided
     * audio input stream. Opening a clip means that it should acquire any
     * required system resources and become operational. If this operation input
     * stream. If this operation succeeds, the line is marked open and an
     * {@link LineEvent.Type#OPEN OPEN} event is dispatched to the line's
     * listeners.
     * <p>
     * Invoking this method on a line which is already open is illegal and may
     * result in an {@code IllegalStateException}.
     * <p>
     * Note that some lines, once closed, cannot be reopened. Attempts to reopen
     * such a line will always result in a {@code LineUnavailableException}.
     *
     * @param  stream an audio input stream from which audio data will be read
     *         into the clip
     * @throws LineUnavailableException if the line cannot be opened due to
     *         resource restrictions
     * @throws IOException if an I/O exception occurs during reading of the
     *         stream
     * @throws IllegalArgumentException if the stream's audio format is not
     *         fully specified or invalid
     * @throws IllegalStateException if the line is already open
     * @throws SecurityException if the line cannot be opened due to security
     *         restrictions
     * @see #close
     * @see #isOpen
     * @see LineListener
     */
    void open(AudioInputStream stream)
            throws LineUnavailableException, IOException;

    /**
     * Obtains the media length in sample frames.
     *
     * @return the media length, expressed in sample frames, or
     *         {@code AudioSystem.NOT_SPECIFIED} if the line is not open
     * @see AudioSystem#NOT_SPECIFIED
     */
    int getFrameLength();

    /**
     * Obtains the media duration in microseconds.
     *
     * @return the media duration, expressed in microseconds, or
     *         {@code AudioSystem.NOT_SPECIFIED} if the line is not open
     * @see AudioSystem#NOT_SPECIFIED
     */
    long getMicrosecondLength();

    /**
     * Sets the media position in sample frames. The position is zero-based; the
     * first frame is frame number zero. When the clip begins playing the next
     * time, it will start by playing the frame at this position.
     * <p>
     * To obtain the current position in sample frames, use the
     * {@link DataLine#getFramePosition getFramePosition} method of
     * {@code DataLine}.
     *
     * @param  frames the desired new media position, expressed in sample frames
     */
    void setFramePosition(int frames);

    /**
     * Sets the media position in microseconds. When the clip begins playing the
     * next time, it will start at this position. The level of precision is not
     * guaranteed. For example, an implementation might calculate the
     * microsecond position from the current frame position and the audio sample
     * frame rate. The precision in microseconds would then be limited to the
     * number of microseconds per sample frame.
     * <p>
     * To obtain the current position in microseconds, use the
     * {@link DataLine#getMicrosecondPosition getMicrosecondPosition} method of
     * {@code DataLine}.
     *
     * @param  microseconds the desired new media position, expressed in
     *         microseconds
     */
    void setMicrosecondPosition(long microseconds);

    /**
     * Sets the first and last sample frames that will be played in the loop.
     * The ending point must be greater than or equal to the starting point, and
     * both must fall within the size of the loaded media. A value of 0 for the
     * starting point means the beginning of the loaded media. Similarly, a
     * value of -1 for the ending point indicates the last frame of the media.
     *
     * @param  start the loop's starting position, in sample frames (zero-based)
     * @param  end the loop's ending position, in sample frames (zero-based), or
     *         -1 to indicate the final frame
     * @throws IllegalArgumentException if the requested loop points cannot be
     *         set, usually because one or both falls outside the media's
     *         duration or because the ending point is before the starting point
     */
    void setLoopPoints(int start, int end);

    /**
     * Starts looping playback from the current position. Playback will continue
     * to the loop's end point, then loop back to the loop start point
     * {@code count} times, and finally continue playback to the end of the
     * clip.
     * <p>
     * If the current position when this method is invoked is greater than the
     * loop end point, playback simply continues to the end of the clip without
     * looping.
     * <p>
     * A {@code count} value of 0 indicates that any current looping should
     * cease and playback should continue to the end of the clip. The behavior
     * is undefined when this method is invoked with any other value during a
     * loop operation.
     * <p>
     * If playback is stopped during looping, the current loop status is
     * cleared; the behavior of subsequent loop and start requests is not
     * affected by an interrupted loop operation.
     *
     * @param  count the number of times playback should loop back from the
     *         loop's end position to the loop's start position, or
     *         {@link #LOOP_CONTINUOUSLY} to indicate that looping should
     *         continue until interrupted
     */
    void loop(int count);
}
