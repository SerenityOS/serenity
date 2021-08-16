/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Wavetable oscillator for pre-loaded data.
 *
 * @author Karl Helgason
 */
public final class ModelByteBufferWavetable implements ModelWavetable {

    private class Buffer8PlusInputStream extends InputStream {

        private final boolean bigendian;
        private final int framesize_pc;
        int pos = 0;
        int pos2 = 0;
        int markpos = 0;
        int markpos2 = 0;

        Buffer8PlusInputStream() {
            framesize_pc = format.getFrameSize() / format.getChannels();
            bigendian = format.isBigEndian();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int avail = available();
            if (avail <= 0)
                return -1;
            if (len > avail)
                len = avail;
            byte[] buff1 = buffer.array();
            byte[] buff2 = buffer8.array();
            pos += buffer.arrayOffset();
            pos2 += buffer8.arrayOffset();
            if (bigendian) {
                for (int i = 0; i < len; i += (framesize_pc + 1)) {
                    System.arraycopy(buff1, pos, b, i, framesize_pc);
                    System.arraycopy(buff2, pos2, b, i + framesize_pc, 1);
                    pos += framesize_pc;
                    pos2 += 1;
                }
            } else {
                for (int i = 0; i < len; i += (framesize_pc + 1)) {
                    System.arraycopy(buff2, pos2, b, i, 1);
                    System.arraycopy(buff1, pos, b, i + 1, framesize_pc);
                    pos += framesize_pc;
                    pos2 += 1;
                }
            }
            pos -= buffer.arrayOffset();
            pos2 -= buffer8.arrayOffset();
            return len;
        }

        @Override
        public long skip(long n) throws IOException {
            int avail = available();
            if (avail <= 0)
                return -1;
            if (n > avail)
                n = avail;
            pos += (n / (framesize_pc + 1)) * (framesize_pc);
            pos2 += n / (framesize_pc + 1);
            return super.skip(n);
        }

        @Override
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            int ret = read(b, 0, 1);
            if (ret == -1)
                return -1;
            return 0 & 0xFF;
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public int available() throws IOException {
            return (int)buffer.capacity() + (int)buffer8.capacity() - pos - pos2;
        }

        @Override
        public synchronized void mark(int readlimit) {
            markpos = pos;
            markpos2 = pos2;
        }

        @Override
        public synchronized void reset() throws IOException {
            pos = markpos;
            pos2 = markpos2;

        }
    }

    private float loopStart = -1;
    private float loopLength = -1;
    private final ModelByteBuffer buffer;
    private ModelByteBuffer buffer8 = null;
    private AudioFormat format = null;
    private float pitchcorrection = 0;
    private float attenuation = 0;
    private int loopType = LOOP_TYPE_OFF;

    public ModelByteBufferWavetable(ModelByteBuffer buffer) {
        this.buffer = buffer;
    }

    public ModelByteBufferWavetable(ModelByteBuffer buffer,
            float pitchcorrection) {
        this.buffer = buffer;
        this.pitchcorrection = pitchcorrection;
    }

    public ModelByteBufferWavetable(ModelByteBuffer buffer, AudioFormat format) {
        this.format = format;
        this.buffer = buffer;
    }

    public ModelByteBufferWavetable(ModelByteBuffer buffer, AudioFormat format,
            float pitchcorrection) {
        this.format = format;
        this.buffer = buffer;
        this.pitchcorrection = pitchcorrection;
    }

    public void set8BitExtensionBuffer(ModelByteBuffer buffer) {
        buffer8 = buffer;
    }

    public ModelByteBuffer get8BitExtensionBuffer() {
        return buffer8;
    }

    public ModelByteBuffer getBuffer() {
        return buffer;
    }

    public AudioFormat getFormat() {
        if (format == null) {
            if (buffer == null)
                return null;
            InputStream is = buffer.getInputStream();
            AudioFormat format = null;
            try {
                format = AudioSystem.getAudioFileFormat(is).getFormat();
            } catch (Exception e) {
                //e.printStackTrace();
            }
            try {
                is.close();
            } catch (IOException e) {
                //e.printStackTrace();
            }
            return format;
        }
        return format;
    }

    @Override
    public AudioFloatInputStream openStream() {
        if (buffer == null)
            return null;
        if (format == null) {
            InputStream is = buffer.getInputStream();
            AudioInputStream ais = null;
            try {
                ais = AudioSystem.getAudioInputStream(is);
            } catch (Exception e) {
                //e.printStackTrace();
                return null;
            }
            return AudioFloatInputStream.getInputStream(ais);
        }
        if (buffer.array() == null) {
            return AudioFloatInputStream.getInputStream(new AudioInputStream(
                    buffer.getInputStream(), format,
                    buffer.capacity() / format.getFrameSize()));
        }
        if (buffer8 != null) {
            if (format.getEncoding().equals(Encoding.PCM_SIGNED)
                    || format.getEncoding().equals(Encoding.PCM_UNSIGNED)) {
                InputStream is = new Buffer8PlusInputStream();
                AudioFormat format2 = new AudioFormat(
                        format.getEncoding(),
                        format.getSampleRate(),
                        format.getSampleSizeInBits() + 8,
                        format.getChannels(),
                        format.getFrameSize() + (1 * format.getChannels()),
                        format.getFrameRate(),
                        format.isBigEndian());

                AudioInputStream ais = new AudioInputStream(is, format2,
                        buffer.capacity() / format.getFrameSize());
                return AudioFloatInputStream.getInputStream(ais);
            }
        }
        return AudioFloatInputStream.getInputStream(format, buffer.array(),
                (int)buffer.arrayOffset(), (int)buffer.capacity());
    }

    @Override
    public int getChannels() {
        return getFormat().getChannels();
    }

    @Override
    public ModelOscillatorStream open(float samplerate) {
        // ModelWavetableOscillator doesn't support ModelOscillatorStream
        return null;
    }

    // attenuation is in cB
    @Override
    public float getAttenuation() {
        return attenuation;
    }
    // attenuation is in cB
    public void setAttenuation(float attenuation) {
        this.attenuation = attenuation;
    }

    @Override
    public float getLoopLength() {
        return loopLength;
    }

    public void setLoopLength(float loopLength) {
        this.loopLength = loopLength;
    }

    @Override
    public float getLoopStart() {
        return loopStart;
    }

    public void setLoopStart(float loopStart) {
        this.loopStart = loopStart;
    }

    public void setLoopType(int loopType) {
        this.loopType = loopType;
    }

    @Override
    public int getLoopType() {
        return loopType;
    }

    @Override
    public float getPitchcorrection() {
        return pitchcorrection;
    }

    public void setPitchcorrection(float pitchcorrection) {
        this.pitchcorrection = pitchcorrection;
    }
}
