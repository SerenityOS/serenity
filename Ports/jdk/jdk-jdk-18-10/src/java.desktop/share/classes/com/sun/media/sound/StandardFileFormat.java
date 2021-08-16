/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

/**
 * An instance of the {@code StandardFileFormat} describes the file's length in
 * bytes and the length in sample frames as longs. This will provide an
 * additional precision unlike the {@code AudioFileFormat}.
 */
class StandardFileFormat extends AudioFileFormat {

    /**
     * File length in bytes stored as long.
     */
    private final long byteLength;

    /**
     * Audio data length in sample frames stored as long.
     */
    private final long frameLength;

    /**
     * Constructs {@code StandardFileFormat} object.
     *
     * @param  type the type of the audio file
     * @param  format the format of the audio data contained in the file
     * @param  frameLength the audio data length in sample frames, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     */
    StandardFileFormat(final Type type, final AudioFormat format,
                       final long frameLength) {
        this(type, AudioSystem.NOT_SPECIFIED, format, frameLength);
    }

    /**
     * Constructs {@code StandardFileFormat} object.
     *
     * @param  type the type of the audio file
     * @param  byteLength the length of the file in bytes, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     * @param  format the format of the audio data contained in the file
     * @param  frameLength the audio data length in sample frames, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     */
    StandardFileFormat(final Type type, final long byteLength,
                       final AudioFormat format, final long frameLength) {
        super(type, clip(byteLength), format, clip(frameLength));
        this.byteLength = byteLength;
        this.frameLength = frameLength;
    }

    /**
     * Replaces the passed value to {@code AudioSystem.NOT_SPECIFIED} if the
     * value is greater than {@code Integer.MAX_VALUE}.
     *
     * @param  value which should be clipped
     * @return the clipped value
     */
    private static int clip(final long value) {
        if (value > Integer.MAX_VALUE) {
            return AudioSystem.NOT_SPECIFIED;
        }
        return (int) value;
    }

    /**
     * Obtains the length of the audio data contained in the file, expressed in
     * sample frames. The long precision is used.
     *
     * @return the number of sample frames of audio data in the file
     * @see AudioSystem#NOT_SPECIFIED
     */
    public final long getLongFrameLength() {
        return frameLength;
    }

    /**
     * Obtains the size in bytes of the entire audio file (not just its audio
     * data). The long precision is used.
     *
     * @return the audio file length in bytes
     * @see AudioSystem#NOT_SPECIFIED
     */
    public final long getLongByteLength() {
        return byteLength;
    }
}
