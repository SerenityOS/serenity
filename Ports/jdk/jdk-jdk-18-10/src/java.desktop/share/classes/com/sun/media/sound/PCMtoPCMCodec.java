/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * Converts among signed/unsigned and little/big endianness of sampled.
 *
 * @author Jan Borgersen
 */
public final class PCMtoPCMCodec extends FormatConversionProvider {

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new Encoding[]{Encoding.PCM_SIGNED, Encoding.PCM_UNSIGNED};
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return getSourceEncodings();
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {

        final int sampleSize = sourceFormat.getSampleSizeInBits();
        AudioFormat.Encoding encoding = sourceFormat.getEncoding();
        if (sampleSize == 8) {
            if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
                return new AudioFormat.Encoding[]{
                        AudioFormat.Encoding.PCM_UNSIGNED
                };
            }
            if (encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
                return new AudioFormat.Encoding[]{
                        AudioFormat.Encoding.PCM_SIGNED
                };
            }
        } else if (sampleSize == 16) {
            if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED)
                    || encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
                return new AudioFormat.Encoding[]{
                        AudioFormat.Encoding.PCM_UNSIGNED,
                        AudioFormat.Encoding.PCM_SIGNED
                };
            }
        }
        return new AudioFormat.Encoding[0];
    }

    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat){
        Objects.requireNonNull(targetEncoding);

        // filter out targetEncoding from the old getOutputFormats( sourceFormat ) method

        AudioFormat[] formats = getOutputFormats( sourceFormat );
        Vector<AudioFormat> newFormats = new Vector<>();
        for(int i=0; i<formats.length; i++ ) {
            if( formats[i].getEncoding().equals( targetEncoding ) ) {
                newFormats.addElement( formats[i] );
            }
        }

        AudioFormat[] formatArray = new AudioFormat[newFormats.size()];

        for (int i = 0; i < formatArray.length; i++) {
            formatArray[i] = newFormats.elementAt(i);
        }

        return formatArray;
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream) {

        if( isConversionSupported(targetEncoding, sourceStream.getFormat()) ) {

            AudioFormat sourceFormat = sourceStream.getFormat();
            AudioFormat targetFormat = new AudioFormat( targetEncoding,
                                                        sourceFormat.getSampleRate(),
                                                        sourceFormat.getSampleSizeInBits(),
                                                        sourceFormat.getChannels(),
                                                        sourceFormat.getFrameSize(),
                                                        sourceFormat.getFrameRate(),
                                                        sourceFormat.isBigEndian() );

            return getConvertedStream(targetFormat, sourceStream);

        } else {
            throw new IllegalArgumentException("Unsupported conversion: " + sourceStream.getFormat().toString() + " to " + targetEncoding.toString() );
        }

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

            cs = new PCMtoPCMCodecStream(stream, outputFormat);
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

        int sampleSize = inputFormat.getSampleSizeInBits();
        boolean isBigEndian = inputFormat.isBigEndian();


        if ( sampleSize==8 ) {
            if ( AudioFormat.Encoding.PCM_SIGNED.equals(inputFormat.getEncoding()) ) {

                format = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED,
                                         inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits(),
                                         inputFormat.getChannels(),
                                         inputFormat.getFrameSize(),
                                         inputFormat.getFrameRate(),
                                         false );
                formats.addElement(format);
            }

            if ( AudioFormat.Encoding.PCM_UNSIGNED.equals(inputFormat.getEncoding()) ) {

                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                         inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits(),
                                         inputFormat.getChannels(),
                                         inputFormat.getFrameSize(),
                                         inputFormat.getFrameRate(),
                                         false );
                formats.addElement(format);
            }

        } else if ( sampleSize==16 ) {

            if ( AudioFormat.Encoding.PCM_SIGNED.equals(inputFormat.getEncoding()) && isBigEndian ) {

                format = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED,
                                         inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits(),
                                         inputFormat.getChannels(),
                                         inputFormat.getFrameSize(),
                                         inputFormat.getFrameRate(),
                                         true );
                formats.addElement(format);
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                         inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits(),
                                         inputFormat.getChannels(),
                                         inputFormat.getFrameSize(),
                                         inputFormat.getFrameRate(),
                                         false );
                formats.addElement(format);
                format = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED,
                                         inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits(),
                                         inputFormat.getChannels(),
                                         inputFormat.getFrameSize(),
                                         inputFormat.getFrameRate(),
                                         false );
                formats.addElement(format);
            }

            if ( AudioFormat.Encoding.PCM_UNSIGNED.equals(inputFormat.getEncoding()) && isBigEndian ) {

                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                         inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits(),
                                         inputFormat.getChannels(),
                                         inputFormat.getFrameSize(),
                                         inputFormat.getFrameRate(),
                                         true );
                formats.addElement(format);
                format = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED,
                                         inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits(),
                                         inputFormat.getChannels(),
                                         inputFormat.getFrameSize(),
                                         inputFormat.getFrameRate(),
                                         false );
                formats.addElement(format);
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                         inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits(),
                                         inputFormat.getChannels(),
                                         inputFormat.getFrameSize(),
                                         inputFormat.getFrameRate(),
                                         false );
                formats.addElement(format);
            }

            if ( AudioFormat.Encoding.PCM_SIGNED.equals(inputFormat.getEncoding()) && !isBigEndian ) {

                format = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED,
                                         inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits(),
                                         inputFormat.getChannels(),
                                         inputFormat.getFrameSize(),
                                         inputFormat.getFrameRate(),
                                         false );
                formats.addElement(format);
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                         inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits(),
                                         inputFormat.getChannels(),
                                         inputFormat.getFrameSize(),
                                         inputFormat.getFrameRate(),
                                         true );
                formats.addElement(format);
                format = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED,
                                         inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits(),
                                         inputFormat.getChannels(),
                                         inputFormat.getFrameSize(),
                                         inputFormat.getFrameRate(),
                                         true );
                formats.addElement(format);
            }

            if ( AudioFormat.Encoding.PCM_UNSIGNED.equals(inputFormat.getEncoding()) && !isBigEndian ) {

                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                         inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits(),
                                         inputFormat.getChannels(),
                                         inputFormat.getFrameSize(),
                                         inputFormat.getFrameRate(),
                                         false );
                formats.addElement(format);
                format = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED,
                                         inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits(),
                                         inputFormat.getChannels(),
                                         inputFormat.getFrameSize(),
                                         inputFormat.getFrameRate(),
                                         true );
                formats.addElement(format);
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                         inputFormat.getSampleRate(),
                                         inputFormat.getSampleSizeInBits(),
                                         inputFormat.getChannels(),
                                         inputFormat.getFrameSize(),
                                         inputFormat.getFrameRate(),
                                         true );
                formats.addElement(format);
            }
        }
        AudioFormat[] formatArray;

        synchronized(formats) {

            formatArray = new AudioFormat[formats.size()];

            for (int i = 0; i < formatArray.length; i++) {

                formatArray[i] = formats.elementAt(i);
            }
        }

        return formatArray;
    }

    class PCMtoPCMCodecStream extends AudioInputStream {

        private final int PCM_SWITCH_SIGNED_8BIT                = 1;
        private final int PCM_SWITCH_ENDIAN                             = 2;
        private final int PCM_SWITCH_SIGNED_LE                  = 3;
        private final int PCM_SWITCH_SIGNED_BE                  = 4;
        private final int PCM_UNSIGNED_LE2SIGNED_BE             = 5;
        private final int PCM_SIGNED_LE2UNSIGNED_BE             = 6;
        private final int PCM_UNSIGNED_BE2SIGNED_LE             = 7;
        private final int PCM_SIGNED_BE2UNSIGNED_LE             = 8;

        private final int sampleSizeInBytes;
        private int conversionType = 0;


        PCMtoPCMCodecStream(AudioInputStream stream, AudioFormat outputFormat) {

            super(stream, outputFormat, -1);

            int sampleSizeInBits = 0;
            AudioFormat.Encoding inputEncoding = null;
            AudioFormat.Encoding outputEncoding = null;
            boolean inputIsBigEndian;
            boolean outputIsBigEndian;

            AudioFormat inputFormat = stream.getFormat();

            // throw an IllegalArgumentException if not ok
            if ( ! (isConversionSupported(inputFormat, outputFormat)) ) {

                throw new IllegalArgumentException("Unsupported conversion: " + inputFormat.toString() + " to " + outputFormat.toString());
            }

            inputEncoding = inputFormat.getEncoding();
            outputEncoding = outputFormat.getEncoding();
            inputIsBigEndian = inputFormat.isBigEndian();
            outputIsBigEndian = outputFormat.isBigEndian();
            sampleSizeInBits = inputFormat.getSampleSizeInBits();
            sampleSizeInBytes = sampleSizeInBits/8;

            // determine conversion to perform

            if( sampleSizeInBits==8 ) {
                if( AudioFormat.Encoding.PCM_UNSIGNED.equals(inputEncoding) &&
                    AudioFormat.Encoding.PCM_SIGNED.equals(outputEncoding) ) {
                    conversionType = PCM_SWITCH_SIGNED_8BIT;
                } else if( AudioFormat.Encoding.PCM_SIGNED.equals(inputEncoding) &&
                           AudioFormat.Encoding.PCM_UNSIGNED.equals(outputEncoding) ) {
                    conversionType = PCM_SWITCH_SIGNED_8BIT;
                }
            } else {

                if( inputEncoding.equals(outputEncoding) && (inputIsBigEndian != outputIsBigEndian) ) {

                    conversionType = PCM_SWITCH_ENDIAN;
                } else if (AudioFormat.Encoding.PCM_UNSIGNED.equals(inputEncoding) && !inputIsBigEndian &&
                            AudioFormat.Encoding.PCM_SIGNED.equals(outputEncoding) && outputIsBigEndian) {

                    conversionType = PCM_UNSIGNED_LE2SIGNED_BE;
                } else if (AudioFormat.Encoding.PCM_SIGNED.equals(inputEncoding) && !inputIsBigEndian &&
                           AudioFormat.Encoding.PCM_UNSIGNED.equals(outputEncoding) && outputIsBigEndian) {

                    conversionType = PCM_SIGNED_LE2UNSIGNED_BE;
                } else if (AudioFormat.Encoding.PCM_UNSIGNED.equals(inputEncoding) && inputIsBigEndian &&
                           AudioFormat.Encoding.PCM_SIGNED.equals(outputEncoding) && !outputIsBigEndian) {

                    conversionType = PCM_UNSIGNED_BE2SIGNED_LE;
                } else if (AudioFormat.Encoding.PCM_SIGNED.equals(inputEncoding) && inputIsBigEndian &&
                           AudioFormat.Encoding.PCM_UNSIGNED.equals(outputEncoding) && !outputIsBigEndian) {

                    conversionType = PCM_SIGNED_BE2UNSIGNED_LE;
                }
            }

            // set the audio stream length in frames if we know it

            frameSize = inputFormat.getFrameSize();
            if( frameSize == AudioSystem.NOT_SPECIFIED ) {
                frameSize=1;
            }
            if( stream instanceof AudioInputStream ) {
                frameLength = stream.getFrameLength();
            } else {
                frameLength = AudioSystem.NOT_SPECIFIED;
            }

            // set framePos to zero
            framePos = 0;

        }

        /**
         * Note that this only works for sign conversions.
         * Other conversions require a read of at least 2 bytes.
         */
        @Override
        public int read() throws IOException {

            // $$jb: do we want to implement this function?

            int temp;
            byte tempbyte;

            if( frameSize==1 ) {
                if( conversionType == PCM_SWITCH_SIGNED_8BIT ) {
                    temp = super.read();

                    if( temp < 0 ) return temp;         // EOF or error

                    tempbyte = (byte) (temp & 0xf);
                    tempbyte = (tempbyte >= 0) ? (byte)(0x80 | tempbyte) : (byte)(0x7F & tempbyte);
                    temp = (int) tempbyte & 0xf;

                    return temp;

                } else {
                    // $$jb: what to return here?
                    throw new IOException("cannot read a single byte if frame size > 1");
                }
            } else {
                throw new IOException("cannot read a single byte if frame size > 1");
            }
        }

        @Override
        public int read(byte[] b) throws IOException {

            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {


            int i;

            // don't read fractional frames
            if ( len%frameSize != 0 ) {
                len -= (len%frameSize);
            }
            // don't read past our own set length
            if( (frameLength!=AudioSystem.NOT_SPECIFIED) && ( (len/frameSize) >(frameLength-framePos)) ) {
                len = (int)(frameLength-framePos) * frameSize;
            }

            int readCount = super.read(b, off, len);
            byte tempByte;

            if(readCount<0) {   // EOF or error
                return readCount;
            }

            // now do the conversions

            switch( conversionType ) {

            case PCM_SWITCH_SIGNED_8BIT:
                switchSigned8bit(b,off,len,readCount);
                break;

            case PCM_SWITCH_ENDIAN:
                switchEndian(b,off,len,readCount);
                break;

            case PCM_SWITCH_SIGNED_LE:
                switchSignedLE(b,off,len,readCount);
                break;

            case PCM_SWITCH_SIGNED_BE:
                switchSignedBE(b,off,len,readCount);
                break;

            case PCM_UNSIGNED_LE2SIGNED_BE:
            case PCM_SIGNED_LE2UNSIGNED_BE:
                switchSignedLE(b,off,len,readCount);
                switchEndian(b,off,len,readCount);
                break;

            case PCM_UNSIGNED_BE2SIGNED_LE:
            case PCM_SIGNED_BE2UNSIGNED_LE:
                switchSignedBE(b,off,len,readCount);
                switchEndian(b,off,len,readCount);
                break;

            default:
                                // do nothing
            }

            // we've done the conversion, just return the readCount
            return readCount;

        }

        private void switchSigned8bit(byte[] b, int off, int len, int readCount) {

            for(int i=off; i < (off+readCount); i++) {
                b[i] = (b[i] >= 0) ? (byte)(0x80 | b[i]) : (byte)(0x7F & b[i]);
            }
        }

        private void switchSignedBE(byte[] b, int off, int len, int readCount) {

            for(int i=off; i < (off+readCount); i+= sampleSizeInBytes ) {
                b[i] = (b[i] >= 0) ? (byte)(0x80 | b[i]) : (byte)(0x7F & b[i]);
            }
        }

        private void switchSignedLE(byte[] b, int off, int len, int readCount) {

            for(int i=(off+sampleSizeInBytes-1); i < (off+readCount); i+= sampleSizeInBytes ) {
                b[i] = (b[i] >= 0) ? (byte)(0x80 | b[i]) : (byte)(0x7F & b[i]);
            }
        }

        private void switchEndian(byte[] b, int off, int len, int readCount) {

            if(sampleSizeInBytes == 2) {
                for(int i=off; i < (off+readCount); i += sampleSizeInBytes ) {
                    byte temp;
                    temp = b[i];
                    b[i] = b[i+1];
                    b[i+1] = temp;
                }
            }
        }
    } // end class PCMtoPCMCodecStream
} // end class PCMtoPCMCodec
