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

/**
 * A source data line is a data line to which data may be written. It acts as a
 * source to its mixer. An application writes audio bytes to a source data line,
 * which handles the buffering of the bytes and delivers them to the mixer. The
 * mixer may mix the samples with those from other sources and then deliver the
 * mix to a target such as an output port (which may represent an audio output
 * device on a sound card).
 * <p>
 * Note that the naming convention for this interface reflects the relationship
 * between the line and its mixer. From the perspective of an application, a
 * source data line may act as a target for audio data.
 * <p>
 * A source data line can be obtained from a mixer by invoking the
 * {@link Mixer#getLine getLine} method of {@code Mixer} with an appropriate
 * {@link DataLine.Info} object.
 * <p>
 * The {@code SourceDataLine} interface provides a method for writing audio data
 * to the data line's buffer. Applications that play or mix audio should write
 * data to the source data line quickly enough to keep the buffer from
 * underflowing (emptying), which could cause discontinuities in the audio that
 * are perceived as clicks. Applications can use the
 * {@link DataLine#available available} method defined in the {@code DataLine}
 * interface to determine the amount of data currently queued in the data line's
 * buffer. The amount of data which can be written to the buffer without
 * blocking is the difference between the buffer size and the amount of queued
 * data. If the delivery of audio output stops due to underflow, a
 * {@link LineEvent.Type#STOP STOP} event is generated. A
 * {@link LineEvent.Type#START START} event is generated when the audio output
 * resumes.
 *
 * @author Kara Kytle
 * @see Mixer
 * @see DataLine
 * @see TargetDataLine
 * @since 1.3
 */
public interface SourceDataLine extends DataLine {

    /**
     * Opens the line with the specified format and suggested buffer size,
     * causing the line to acquire any required system resources and become
     * operational.
     * <p>
     * The buffer size is specified in bytes, but must represent an integral
     * number of sample frames. Invoking this method with a requested buffer
     * size that does not meet this requirement may result in an
     * {@code IllegalArgumentException}. The actual buffer size for the open
     * line may differ from the requested buffer size. The value actually set
     * may be queried by subsequently calling {@link DataLine#getBufferSize}.
     * <p>
     * If this operation succeeds, the line is marked as open, and an
     * {@link LineEvent.Type#OPEN OPEN} event is dispatched to the line's
     * listeners.
     * <p>
     * Invoking this method on a line which is already open is illegal and may
     * result in an {@code IllegalStateException}.
     * <p>
     * Note that some lines, once closed, cannot be reopened. Attempts to reopen
     * such a line will always result in a {@code LineUnavailableException}.
     *
     * @param  format the desired audio format
     * @param  bufferSize the desired buffer size
     * @throws LineUnavailableException if the line cannot be opened due to
     *         resource restrictions
     * @throws IllegalArgumentException if the buffer size does not represent an
     *         integral number of sample frames, or if {@code format} is not
     *         fully specified or invalid
     * @throws IllegalStateException if the line is already open
     * @throws SecurityException if the line cannot be opened due to security
     *         restrictions
     * @see #open(AudioFormat)
     * @see Line#open
     * @see Line#close
     * @see Line#isOpen
     * @see LineEvent
     */
    void open(AudioFormat format, int bufferSize)
            throws LineUnavailableException;

    /**
     * Opens the line with the specified format, causing the line to acquire any
     * required system resources and become operational.
     * <p>
     * The implementation chooses a buffer size, which is measured in bytes but
     * which encompasses an integral number of sample frames. The buffer size
     * that the system has chosen may be queried by subsequently calling
     * {@link DataLine#getBufferSize}.
     * <p>
     * If this operation succeeds, the line is marked as open, and an
     * {@link LineEvent.Type#OPEN OPEN} event is dispatched to the line's
     * listeners.
     * <p>
     * Invoking this method on a line which is already open is illegal and may
     * result in an {@code IllegalStateException}.
     * <p>
     * Note that some lines, once closed, cannot be reopened. Attempts to reopen
     * such a line will always result in a {@code LineUnavailableException}.
     *
     * @param  format the desired audio format
     * @throws LineUnavailableException if the line cannot be opened due to
     *         resource restrictions
     * @throws IllegalArgumentException if {@code format} is not fully specified
     *         or invalid
     * @throws IllegalStateException if the line is already open
     * @throws SecurityException if the line cannot be opened due to security
     *         restrictions
     * @see #open(AudioFormat, int)
     * @see Line#open
     * @see Line#close
     * @see Line#isOpen
     * @see LineEvent
     */
    void open(AudioFormat format) throws LineUnavailableException;

    /**
     * Writes audio data to the mixer via this source data line. The requested
     * number of bytes of data are read from the specified array, starting at
     * the given offset into the array, and written to the data line's buffer.
     * If the caller attempts to write more data than can currently be written
     * (see {@link DataLine#available available}), this method blocks until the
     * requested amount of data has been written. This applies even if the
     * requested amount of data to write is greater than the data line's buffer
     * size. However, if the data line is closed, stopped, or flushed before the
     * requested amount has been written, the method no longer blocks, but
     * returns the number of bytes written thus far.
     * <p>
     * The number of bytes that can be written without blocking can be
     * ascertained using the {@link DataLine#available available} method of the
     * {@code DataLine} interface. (While it is guaranteed that this number of
     * bytes can be written without blocking, there is no guarantee that
     * attempts to write additional data will block.)
     * <p>
     * The number of bytes to write must represent an integral number of sample
     * frames, such that:
     * <p style="text-align:center">
     * {@code [ bytes written ] % [frame size in bytes ] == 0}
     * <p>
     * The return value will always meet this requirement. A request to write a
     * number of bytes representing a non-integral number of sample frames
     * cannot be fulfilled and may result in an
     * {@code IllegalArgumentException}.
     *
     * @param  b a byte array containing data to be written to the data line
     * @param  off the offset from the beginning of the array, in bytes
     * @param  len the length, in bytes, of the valid data in the array (in
     *         other words, the requested amount of data to write, in bytes)
     * @return the number of bytes actually written
     * @throws IllegalArgumentException if the requested number of bytes does
     *         not represent an integral number of sample frames, or if
     *         {@code len} is negative
     * @throws ArrayIndexOutOfBoundsException if {@code off} is negative, or
     *         {@code off+len} is greater than the length of the array {@code b}
     * @see TargetDataLine#read
     * @see DataLine#available
     */
    int write(byte[] b, int off, int len);

    /**
     * Obtains the number of sample frames of audio data that can be written to
     * the mixer, via this data line, without blocking. Note that the return
     * value measures sample frames, not bytes.
     *
     * @return the number of sample frames currently available for writing
     * @see TargetDataLine#availableRead
     */
    //public int availableWrite();
}
