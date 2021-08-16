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
 * A target data line is a type of {@link DataLine} from which audio data can be
 * read. The most common example is a data line that gets its data from an audio
 * capture device. (The device is implemented as a mixer that writes to the
 * target data line.)
 * <p>
 * Note that the naming convention for this interface reflects the relationship
 * between the line and its mixer. From the perspective of an application, a
 * target data line may act as a source for audio data.
 * <p>
 * The target data line can be obtained from a mixer by invoking the
 * {@link Mixer#getLine getLine} method of {@code Mixer} with an appropriate
 * {@link DataLine.Info} object.
 * <p>
 * The {@code TargetDataLine} interface provides a method for reading the
 * captured data from the target data line's buffer. Applications that record
 * audio should read data from the target data line quickly enough to keep the
 * buffer from overflowing, which could cause discontinuities in the captured
 * data that are perceived as clicks. Applications can use the
 * {@link DataLine#available available} method defined in the {@code DataLine}
 * interface to determine the amount of data currently queued in the data line's
 * buffer. If the buffer does overflow, the oldest queued data is discarded and
 * replaced by new data.
 *
 * @author Kara Kytle
 * @see Mixer
 * @see DataLine
 * @see SourceDataLine
 * @since 1.3
 */
public interface TargetDataLine extends DataLine {

    /**
     * Opens the line with the specified format and requested buffer size,
     * causing the line to acquire any required system resources and become
     * operational.
     * <p>
     * The buffer size is specified in bytes, but must represent an integral
     * number of sample frames. Invoking this method with a requested buffer
     * size that does not meet this requirement may result in an
     * {@code IllegalArgumentException}. The actual buffer size for the open
     * line may differ from the requested buffer size. The value actually set
     * may be queried by subsequently calling {@link DataLine#getBufferSize}
     * <p>
     * If this operation succeeds, the line is marked as open, and an
     * {@link LineEvent.Type#OPEN OPEN} event is dispatched to the line's
     * listeners.
     * <p>
     * Invoking this method on a line that is already open is illegal and may
     * result in an {@code IllegalStateException}.
     * <p>
     * Some lines, once closed, cannot be reopened. Attempts to reopen such a
     * line will always result in a {@code LineUnavailableException}.
     *
     * @param  format the desired audio format
     * @param  bufferSize the desired buffer size, in bytes
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
    void open(AudioFormat format, int bufferSize) throws LineUnavailableException;

    /**
     * Opens the line with the specified format, causing the line to acquire any
     * required system resources and become operational.
     * <p>
     * The implementation chooses a buffer size, which is measured in bytes but
     * which encompasses an integral number of sample frames. The buffer size
     * that the system has chosen may be queried by subsequently calling
     * {@link DataLine#getBufferSize}
     * <p>
     * If this operation succeeds, the line is marked as open, and an
     * {@link LineEvent.Type#OPEN OPEN} event is dispatched to the line's
     * listeners.
     * <p>
     * Invoking this method on a line that is already open is illegal and may
     * result in an {@code IllegalStateException}.
     * <p>
     * Some lines, once closed, cannot be reopened. Attempts to reopen such a
     * line will always result in a {@code LineUnavailableException}.
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
     * Reads audio data from the data line's input buffer. The requested number
     * of bytes is read into the specified array, starting at the specified
     * offset into the array in bytes. This method blocks until the requested
     * amount of data has been read. However, if the data line is closed,
     * stopped, drained, or flushed before the requested amount has been read,
     * the method no longer blocks, but returns the number of bytes read thus
     * far.
     * <p>
     * The number of bytes that can be read without blocking can be ascertained
     * using the {@link DataLine#available available} method of the
     * {@code DataLine} interface. (While it is guaranteed that this number of
     * bytes can be read without blocking, there is no guarantee that attempts
     * to read additional data will block.)
     * <p>
     * The number of bytes to be read must represent an integral number of
     * sample frames, such that:
     * <p style="text-align:center">
     * {@code [ bytes read ] % [frame size in bytes ] == 0}
     * <p>
     * The return value will always meet this requirement. A request to read a
     * number of bytes representing a non-integral number of sample frames
     * cannot be fulfilled and may result in an IllegalArgumentException.
     *
     * @param  b a byte array that will contain the requested input data when
     *         this method returns
     * @param  off the offset from the beginning of the array, in bytes
     * @param  len the requested number of bytes to read
     * @return the number of bytes actually read
     * @throws IllegalArgumentException if the requested number of bytes does
     *         not represent an integral number of sample frames, or if
     *         {@code len} is negative
     * @throws ArrayIndexOutOfBoundsException if {@code off} is negative, or
     *         {@code off+len} is greater than the length of the array {@code b}
     *
     * @see SourceDataLine#write
     * @see DataLine#available
     */
    int read(byte[] b, int off, int len);

    /**
     * Obtains the number of sample frames of audio data that can be read from
     * the target data line without blocking. Note that the return value
     * measures sample frames, not bytes.
     *
     * @return the number of sample frames currently available for reading
     * @see SourceDataLine#availableWrite
     */
    //public int availableRead();
}
