/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.Objects;
import java.util.Vector;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;

/**
 * A-law encodes linear data, and decodes a-law data to linear data.
 *
 * @author Kara Kytle
 */
public final class AlawCodec extends FormatConversionProvider {

    /* Tables used for A-law decoding */

    private static final byte[] ALAW_TABH = new byte[256];
    private static final byte[] ALAW_TABL = new byte[256];

    private static final short[] seg_end = {
            0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF, 0x1FFF, 0x3FFF, 0x7FFF
    };

    /**
     * Initializes the decode tables.
     */
    static {
        for (int i=0;i<256;i++) {
            int input    = i ^ 0x55;
            int mantissa = (input & 0xf ) << 4;
            int segment  = (input & 0x70) >> 4;
            int value    = mantissa+8;

            if(segment>=1)
                value+=0x100;
            if(segment>1)
                value <<= (segment -1);

            if( (input & 0x80)==0 )
                value = -value;

            ALAW_TABL[i] = (byte)value;
            ALAW_TABH[i] = (byte)(value>>8);
        }
    }

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new Encoding[]{Encoding.ALAW, Encoding.PCM_SIGNED};
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return getSourceEncodings();
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat){

        if( sourceFormat.getEncoding().equals( AudioFormat.Encoding.PCM_SIGNED )) {

            if( sourceFormat.getSampleSizeInBits() == 16 ) {

                AudioFormat.Encoding[] enc = new AudioFormat.Encoding[1];
                enc[0] = AudioFormat.Encoding.ALAW;
                return enc;

            } else {
                return new AudioFormat.Encoding[0];
            }
        } else if( sourceFormat.getEncoding().equals( AudioFormat.Encoding.ALAW ) ) {

            if( sourceFormat.getSampleSizeInBits() == 8 ) {

                AudioFormat.Encoding[] enc = new AudioFormat.Encoding[1];
                enc[0] = AudioFormat.Encoding.PCM_SIGNED;
                return enc;

            } else {
                return new AudioFormat.Encoding[0];
            }

        } else {
            return new AudioFormat.Encoding[0];
        }
    }

    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat){
        Objects.requireNonNull(sourceFormat);
        if( (targetEncoding.equals( AudioFormat.Encoding.PCM_SIGNED ) && sourceFormat.getEncoding().equals( AudioFormat.Encoding.ALAW)) ||
            (targetEncoding.equals( AudioFormat.Encoding.ALAW) && sourceFormat.getEncoding().equals( AudioFormat.Encoding.PCM_SIGNED)) ) {
                return getOutputFormats( sourceFormat );
            } else {
                return new AudioFormat[0];
            }
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream){
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioFormat.Encoding sourceEncoding = sourceFormat.getEncoding();

        if( !isConversionSupported(targetEncoding,sourceStream.getFormat()) ) {
            throw new IllegalArgumentException("Unsupported conversion: " + sourceStream.getFormat().toString() + " to " + targetEncoding.toString());
        }
        if( sourceEncoding.equals( targetEncoding ) ) {
            return sourceStream;
        }
        AudioFormat targetFormat = null;
        if( sourceEncoding.equals( AudioFormat.Encoding.ALAW ) &&
            targetEncoding.equals( AudioFormat.Encoding.PCM_SIGNED ) ) {

            targetFormat = new AudioFormat( targetEncoding,
                                            sourceFormat.getSampleRate(),
                                            16,
                                            sourceFormat.getChannels(),
                                            2*sourceFormat.getChannels(),
                                            sourceFormat.getSampleRate(),
                                            sourceFormat.isBigEndian());

        } else if( sourceEncoding.equals( AudioFormat.Encoding.PCM_SIGNED ) &&
                   targetEncoding.equals( AudioFormat.Encoding.ALAW ) ) {

            targetFormat = new AudioFormat( targetEncoding,
                                            sourceFormat.getSampleRate(),
                                            8,
                                            sourceFormat.getChannels(),
                                            sourceFormat.getChannels(),
                                            sourceFormat.getSampleRate(),
                                            false);
        } else {
            throw new IllegalArgumentException("Unsupported conversion: " + sourceStream.getFormat().toString() + " to " + targetEncoding.toString());
        }
        return getConvertedStream(targetFormat, sourceStream);
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream){
        if (!isConversionSupported(targetFormat, sourceStream.getFormat()))
            throw new IllegalArgumentException("Unsupported conversion: "
                                               + sourceStream.getFormat().toString() + " to "
                                               + targetFormat.toString());
        return getConvertedStream( targetFormat, sourceStream );
    }

    /**
     * Opens the codec with the specified parameters.
     * @param stream stream from which data to be processed should be read
     * @param outputFormat desired data format of the stream after processing
     * @return stream from which processed data may be read
     * @throws IllegalArgumentException if the format combination supplied is
     * not supported.
     */
    private AudioInputStream getConvertedStream(AudioFormat outputFormat, AudioInputStream stream) {

        AudioInputStream cs = null;
        AudioFormat inputFormat = stream.getFormat();

        if( inputFormat.matches(outputFormat) ) {
            cs = stream;
        } else {
            cs = new AlawCodecStream(stream, outputFormat);
        }

        return cs;
    }

    /**
     * Obtains the set of output formats supported by the codec
     * given a particular input format.
     * If no output formats are supported for this input format,
     * returns an array of length 0.
     * @return array of supported output formats.
     */
    private AudioFormat[] getOutputFormats(AudioFormat inputFormat) {


        Vector<AudioFormat> formats = new Vector<>();
        AudioFormat format;

        if (inputFormat.getSampleSizeInBits() == 16
                && AudioFormat.Encoding.PCM_SIGNED.equals(inputFormat.getEncoding())) {
            format = new AudioFormat(AudioFormat.Encoding.ALAW,
                                     inputFormat.getSampleRate(), 8,
                                     inputFormat.getChannels(),
                                     inputFormat.getChannels(),
                                     inputFormat.getSampleRate(), false);
            formats.addElement(format);
        }
        if (inputFormat.getSampleSizeInBits() == 8
                && AudioFormat.Encoding.ALAW.equals(inputFormat.getEncoding())) {
            format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                     inputFormat.getSampleRate(), 16,
                                     inputFormat.getChannels(),
                                     inputFormat.getChannels() * 2,
                                     inputFormat.getSampleRate(), false);
            formats.addElement(format);
            format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                     inputFormat.getSampleRate(), 16,
                                     inputFormat.getChannels(),
                                     inputFormat.getChannels() * 2,
                                     inputFormat.getSampleRate(), true);
            formats.addElement(format);
        }

        AudioFormat[] formatArray = new AudioFormat[formats.size()];
        for (int i = 0; i < formatArray.length; i++) {
            formatArray[i] = formats.elementAt(i);
        }
        return formatArray;
    }


    private final class AlawCodecStream extends AudioInputStream {

        // tempBuffer required only for encoding (when encode is true)
        private static final int tempBufferSize = 64;
        private byte[] tempBuffer  = null;

        /**
         * True to encode to a-law, false to decode to linear
         */
        boolean encode = false;

        AudioFormat encodeFormat;
        AudioFormat decodeFormat;

        byte[] tabByte1 = null;
        byte[] tabByte2 = null;
        int highByte = 0;
        int lowByte  = 1;

        AlawCodecStream(AudioInputStream stream, AudioFormat outputFormat) {

            super(stream, outputFormat, -1);

            AudioFormat inputFormat = stream.getFormat();

            // throw an IllegalArgumentException if not ok
            if ( ! (isConversionSupported(outputFormat, inputFormat)) ) {

                throw new IllegalArgumentException("Unsupported conversion: " + inputFormat.toString() + " to " + outputFormat.toString());
            }

            //$$fb 2002-07-18: fix for 4714846: JavaSound ULAW (8-bit) encoder erroneously depends on endian-ness
            boolean PCMIsBigEndian;

            // determine whether we are encoding or decoding
            if (AudioFormat.Encoding.ALAW.equals(inputFormat.getEncoding())) {
                encode = false;
                encodeFormat = inputFormat;
                decodeFormat = outputFormat;
                PCMIsBigEndian = outputFormat.isBigEndian();
            } else {
                encode = true;
                encodeFormat = outputFormat;
                decodeFormat = inputFormat;
                PCMIsBigEndian = inputFormat.isBigEndian();
                tempBuffer = new byte[tempBufferSize];
            }

            if (PCMIsBigEndian) {
                tabByte1 = ALAW_TABH;
                tabByte2 = ALAW_TABL;
                highByte = 0;
                lowByte  = 1;
            } else {
                tabByte1 = ALAW_TABL;
                tabByte2 = ALAW_TABH;
                highByte = 1;
                lowByte  = 0;
            }

            // set the AudioInputStream length in frames if we know it
            if (stream instanceof AudioInputStream) {
                frameLength = stream.getFrameLength();
            }

            // set framePos to zero
            framePos = 0;
            frameSize = inputFormat.getFrameSize();
            if( frameSize==AudioSystem.NOT_SPECIFIED ) {
                frameSize=1;
            }
        }


        /*
         * $$jb 2/23/99
         * Used to determine segment number in aLaw encoding
         */
        private short search(short val, short[] table, short size) {
            for(short i = 0; i < size; i++) {
                if (val <= table[i]) { return i; }
            }
            return size;
        }

        /**
         * Note that this won't actually read anything; must read in
         * two-byte units.
         */
        @Override
        public int read() throws IOException {

            byte[] b = new byte[1];
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b) throws IOException {

            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {

            // don't read fractional frames
            if( len%frameSize != 0 ) {
                len -= (len%frameSize);
            }

            if (encode) {

                short QUANT_MASK = 0xF;
                short SEG_SHIFT = 4;
                short mask;
                short seg;
                int adj;
                int i;

                short sample;
                byte enc;

                int readCount = 0;
                int currentPos = off;
                int readLeft = len*2;
                int readLen = ( (readLeft>tempBufferSize) ? tempBufferSize : readLeft );

                while ((readCount = super.read(tempBuffer,0,readLen))>0) {

                    for (i = 0; i < readCount; i+=2) {

                        /* Get the sample from the tempBuffer */
                        sample = (short)(( (tempBuffer[i + highByte]) << 8) & 0xFF00);
                        sample |= (short)( (tempBuffer[i + lowByte]) & 0xFF);

                        if(sample >= 0) {
                            mask = 0xD5;
                        } else {
                            mask = 0x55;
                            sample = (short)(-sample - 8);
                        }
                        /* Convert the scaled magnitude to segment number. */
                        seg = search(sample, seg_end, (short) 8);
                        /*
                         * Combine the sign, segment, quantization bits
                         */
                        if (seg >= 8) {  /* out of range, return maximum value. */
                            enc = (byte) (0x7F ^ mask);
                        } else {
                            enc = (byte) (seg << SEG_SHIFT);
                            if(seg < 2) {
                                enc |= (byte) ( (sample >> 4) & QUANT_MASK);
                            } else {
                                enc |= (byte) ( (sample >> (seg + 3)) & QUANT_MASK );
                            }
                            enc ^= mask;
                        }
                        /* Now put the encoded sample where it belongs */
                        b[currentPos] = enc;
                        currentPos++;
                    }
                    /* And update pointers and counters for next iteration */
                    readLeft -= readCount;
                    readLen = ( (readLeft>tempBufferSize) ? tempBufferSize : readLeft );
                }

                if( currentPos==off && readCount<0 ) {  // EOF or error
                    return readCount;
                }

                return (currentPos - off);  /* Number of bytes written to new buffer */

            } else {

                int i;
                int readLen = len/2;
                int readOffset = off + len/2;
                int readCount = super.read(b, readOffset, readLen);

                for (i = off; i < (off + (readCount*2)); i+=2) {
                    b[i]        = tabByte1[b[readOffset] & 0xFF];
                    b[i+1]      = tabByte2[b[readOffset] & 0xFF];
                    readOffset++;
                }

                if( readCount<0 ) {             // EOF or error
                    return readCount;
                }

                return (i - off);
            }
        }

        @Override
        public long skip(final long n) throws IOException {
            // Implementation of this method assumes that we support
            // encoding/decoding from/to 8/16 bits only
            return encode ? super.skip(n * 2) / 2 : super.skip(n / 2) * 2;
        }
    } // end class AlawCodecStream
} // end class ALAW
