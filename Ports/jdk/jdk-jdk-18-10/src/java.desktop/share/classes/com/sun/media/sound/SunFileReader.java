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

package com.sun.media.sound;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

/**
 * Abstract File Reader class.
 *
 * @author Jan Borgersen
 */
abstract class SunFileReader extends AudioFileReader {

    @Override
    public final StandardFileFormat getAudioFileFormat(final InputStream stream)
            throws UnsupportedAudioFileException, IOException {
        stream.mark(200); // The biggest value which was historically used
        try {
            return getAudioFileFormatImpl(stream);
        } catch (final EOFException ignored) {
            // the header is less than was expected
            throw new UnsupportedAudioFileException();
        } finally {
            stream.reset();
        }
    }

    @Override
    public final AudioFileFormat getAudioFileFormat(final URL url)
            throws UnsupportedAudioFileException, IOException {
        try (InputStream is = url.openStream()) {
            return getAudioFileFormatImpl(new BufferedInputStream(is));
        } catch (final EOFException ignored) {
            // the header is less than was expected
            throw new UnsupportedAudioFileException();
        }
    }

    @Override
    public final AudioFileFormat getAudioFileFormat(final File file)
            throws UnsupportedAudioFileException, IOException {
        try (InputStream is = new FileInputStream(file)) {
            return getAudioFileFormatImpl(new BufferedInputStream(is));
        } catch (final EOFException ignored) {
            // the header is less than was expected
            throw new UnsupportedAudioFileException();
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(final InputStream stream)
            throws UnsupportedAudioFileException, IOException {
        stream.mark(200); // The biggest value which was historically used
        try {
            final StandardFileFormat format = getAudioFileFormatImpl(stream);
            // we've got everything, the stream is supported and it is at the
            // beginning of the audio data, so return an AudioInputStream
            return new AudioInputStream(stream, format.getFormat(),
                                        format.getLongFrameLength());
        } catch (UnsupportedAudioFileException | EOFException ignored) {
            // stream is unsupported or the header is less than was expected
            stream.reset();
            throw new UnsupportedAudioFileException();
        }
    }

    @Override
    public final AudioInputStream getAudioInputStream(final URL url)
            throws UnsupportedAudioFileException, IOException {
        final InputStream urlStream = url.openStream();
        try {
            return getAudioInputStream(new BufferedInputStream(urlStream));
        } catch (final Throwable e) {
            closeSilently(urlStream);
            throw e;
        }
    }

    @Override
    public final AudioInputStream getAudioInputStream(final File file)
            throws UnsupportedAudioFileException, IOException {
        final InputStream fileStream = new FileInputStream(file);
        try {
            return getAudioInputStream(new BufferedInputStream(fileStream));
        } catch (final Throwable e) {
            closeSilently(fileStream);
            throw e;
        }
    }

    /**
     * Obtains the audio file format of the input stream provided. The stream
     * must point to valid audio file data. Note that default implementation of
     * {@link #getAudioInputStream(InputStream)} assume that this method leaves
     * the input stream at the beginning of the audio data.
     *
     * @param  stream the input stream from which file format information should
     *         be extracted
     * @return an {@code AudioFileFormat} object describing the audio file
     *         format
     * @throws UnsupportedAudioFileException if the stream does not point to
     *         valid audio file data recognized by the system
     * @throws IOException if an I/O exception occurs
     * @throws EOFException is used incorrectly by our readers instead of
     *         UnsupportedAudioFileException if the header is less than was
     *         expected
     */
    abstract StandardFileFormat getAudioFileFormatImpl(InputStream stream)
            throws UnsupportedAudioFileException, IOException;

    // HELPER METHODS

    /**
     * Closes the InputStream when we have read all necessary data from it, and
     * ignores an IOException.
     *
     * @param is the InputStream which should be closed
     */
    private static void closeSilently(final InputStream is) {
        try {
            is.close();
        } catch (final IOException ignored) {
            // IOException is ignored
        }
    }

    /**
     * rllong
     * Protected helper method to read 64 bits and changing the order of
     * each bytes.
     * @return 32 bits swapped value.
     * @exception IOException
     */
    final int rllong(DataInputStream dis) throws IOException {

        int b1, b2, b3, b4 ;
        int i = 0;

        i = dis.readInt();

        b1 = ( i & 0xFF ) << 24 ;
        b2 = ( i & 0xFF00 ) << 8;
        b3 = ( i & 0xFF0000 ) >> 8;
        b4 = ( i & 0xFF000000 ) >>> 24;

        i = ( b1 | b2 | b3 | b4 );

        return i;
    }

    /**
     * big2little
     * Protected helper method to swap the order of bytes in a 32 bit int
     * @return 32 bits swapped value
     */
    final int big2little(int i) {

        int b1, b2, b3, b4 ;

        b1 = ( i & 0xFF ) << 24 ;
        b2 = ( i & 0xFF00 ) << 8;
        b3 = ( i & 0xFF0000 ) >> 8;
        b4 = ( i & 0xFF000000 ) >>> 24;

        i = ( b1 | b2 | b3 | b4 );

        return i;
    }

    /**
     * rlshort
     * Protected helper method to read 16 bits value. Swap high with low byte.
     * @return the swapped value.
     * @exception IOException
     */
    final short rlshort(DataInputStream dis)  throws IOException {

        short s=0;
        short high, low;

        s = dis.readShort();

        high = (short)(( s & 0xFF ) << 8) ;
        low = (short)(( s & 0xFF00 ) >>> 8);

        s = (short)( high | low );

        return s;
    }

    /**
     * big2little
     * Protected helper method to swap the order of bytes in a 16 bit short
     * @return 16 bits swapped value
     */
    final short big2littleShort(short i) {

        short high, low;

        high = (short)(( i & 0xFF ) << 8) ;
        low = (short)(( i & 0xFF00 ) >>> 8);

        i = (short)( high | low );

        return i;
    }

    /** Calculates the frame size for PCM frames.
     * Note that this method is appropriate for non-packed samples.
     * For instance, 12 bit, 2 channels will return 4 bytes, not 3.
     * @param sampleSizeInBits the size of a single sample in bits
     * @param channels the number of channels
     * @return the size of a PCM frame in bytes.
     */
    static final int calculatePCMFrameSize(int sampleSizeInBits, int channels) {
        try {
            return Math.multiplyExact((sampleSizeInBits + 7) / 8, channels);
        } catch (final ArithmeticException ignored) {
            return 0;
        }
    }
}
