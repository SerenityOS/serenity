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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * AU file reader.
 *
 * @author Kara Kytle
 * @author Jan Borgersen
 * @author Florian Bomers
 */
public final class AuFileReader extends SunFileReader {

    @Override
    StandardFileFormat getAudioFileFormatImpl(final InputStream stream)
            throws UnsupportedAudioFileException, IOException {
        final DataInputStream dis = new DataInputStream(stream);
        final int magic = dis.readInt();

        if (magic != AuFileFormat.AU_SUN_MAGIC) {
            // not AU, throw exception
            throw new UnsupportedAudioFileException("not an AU file");
        }

        final int headerSize = dis.readInt();
        if (headerSize < AuFileFormat.AU_HEADERSIZE) {
            throw new UnsupportedAudioFileException("Invalid header size");
        }
        final long /* unsigned int */ dataSize = dis.readInt() & 0xffffffffL;
        final int auType = dis.readInt();
        final int sampleRate = dis.readInt();
        if (sampleRate <= 0) {
            throw new UnsupportedAudioFileException("Invalid sample rate");
        }
        final int channels = dis.readInt();
        if (channels <= 0) {
            throw new UnsupportedAudioFileException("Invalid number of channels");
        }

        final int sampleSizeInBits;
        final AudioFormat.Encoding encoding;
        switch (auType) {
            case AuFileFormat.AU_ULAW_8:
                encoding = AudioFormat.Encoding.ULAW;
                sampleSizeInBits = 8;
                break;
            case AuFileFormat.AU_ALAW_8:
                encoding = AudioFormat.Encoding.ALAW;
                sampleSizeInBits = 8;
                break;
            case AuFileFormat.AU_LINEAR_8:
                // $$jb: 04.29.99: 8bit linear is *signed*, not *unsigned*
                encoding = AudioFormat.Encoding.PCM_SIGNED;
                sampleSizeInBits = 8;
                break;
            case AuFileFormat.AU_LINEAR_16:
                encoding = AudioFormat.Encoding.PCM_SIGNED;
                sampleSizeInBits = 16;
                break;
            case AuFileFormat.AU_LINEAR_24:
                encoding = AudioFormat.Encoding.PCM_SIGNED;
                sampleSizeInBits = 24;
                break;
            case AuFileFormat.AU_LINEAR_32:
                encoding = AudioFormat.Encoding.PCM_SIGNED;
                sampleSizeInBits = 32;
                break;
            case AuFileFormat.AU_FLOAT:
                encoding = AudioFormat.Encoding.PCM_FLOAT;
                sampleSizeInBits = 32;
                break;
            case AuFileFormat.AU_DOUBLE:
                encoding = AudioFormat.Encoding.PCM_FLOAT;
                sampleSizeInBits = 64;
                break;
            // we don't support these ...
            /*          case AuFileFormat.AU_ADPCM_G721:
                        encoding = new AudioFormat.G721_ADPCM;
                        sampleSizeInBits = 16;
                        break;
                        case AuFileFormat.AU_ADPCM_G723_3:
                        encoding = new AudioFormat.G723_3;
                        sampleSize = 24;
                        SamplePerUnit = 8;
                        break;
                        case AuFileFormat.AU_ADPCM_G723_5:
                        encoding = new AudioFormat.G723_5;
                        sampleSize = 40;
                        SamplePerUnit = 8;
                        break;
            */
            default:
                // unsupported filetype, throw exception
                throw new UnsupportedAudioFileException("not a valid AU file");
        }

        // Skip the variable-length annotation field. The content of this field
        // is currently undefined by AU specification and is unsupported by
        // JavaSound, so seek past the header
        dis.skipBytes(headerSize - AuFileFormat.AU_HEADERSIZE);

        // Even if the sampleSizeInBits and channels are supported we can get an
        // unsupported frameSize because of overflow
        final int frameSize = calculatePCMFrameSize(sampleSizeInBits, channels);
        if (frameSize <= 0) {
            throw new UnsupportedAudioFileException("Invalid frame size");
        }

        //$$fb 2002-11-02: fix for 4629669: AU file reader: problems with empty files
        //$$fb 2003-10-20: fix for 4940459: AudioInputStream.getFrameLength() returns 0 instead of NOT_SPECIFIED
        long frameLength = AudioSystem.NOT_SPECIFIED;
        long byteLength = AudioSystem.NOT_SPECIFIED;
        if (dataSize != AuFileFormat.UNKNOWN_SIZE) {
            frameLength = dataSize / frameSize;
            byteLength = dataSize + headerSize;
        }
        final AudioFormat format = new AudioFormat(encoding, sampleRate,
                                                   sampleSizeInBits, channels,
                                                   frameSize, sampleRate, true);
        return new AuFileFormat(Type.AU, byteLength, format, frameLength);
    }
}
