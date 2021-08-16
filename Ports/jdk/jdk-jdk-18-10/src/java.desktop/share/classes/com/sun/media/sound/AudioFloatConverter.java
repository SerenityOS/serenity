/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

/**
 * This class is used to convert between 8,16,24,32,32+ bit signed/unsigned
 * big/litle endian fixed/floating point byte buffers and float buffers.
 *
 * @author Karl Helgason
 */
public abstract class AudioFloatConverter {

    /***************************************************************************
     *
     * LSB Filter, used filter least significant byte in samples arrays.
     *
     * Is used filter out data in lsb byte when SampleSizeInBits is not
     * dividable by 8.
     *
     **************************************************************************/

    private static class AudioFloatLSBFilter extends AudioFloatConverter {

        private final AudioFloatConverter converter;

        private final int offset;

        private final int stepsize;

        private final byte mask;

        private byte[] mask_buffer;

        AudioFloatLSBFilter(AudioFloatConverter converter, AudioFormat format) {
            int bits = format.getSampleSizeInBits();
            boolean bigEndian = format.isBigEndian();
            this.converter = converter;
            stepsize = (bits + 7) / 8;
            offset = bigEndian ? (stepsize - 1) : 0;
            int lsb_bits = bits % 8;
            if (lsb_bits == 0)
                mask = (byte) 0x00;
            else if (lsb_bits == 1)
                mask = (byte) 0x80;
            else if (lsb_bits == 2)
                mask = (byte) 0xC0;
            else if (lsb_bits == 3)
                mask = (byte) 0xE0;
            else if (lsb_bits == 4)
                mask = (byte) 0xF0;
            else if (lsb_bits == 5)
                mask = (byte) 0xF8;
            else if (lsb_bits == 6)
                mask = (byte) 0xFC;
            else if (lsb_bits == 7)
                mask = (byte) 0xFE;
            else
                mask = (byte) 0xFF;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            byte[] ret = converter.toByteArray(in_buff, in_offset, in_len,
                    out_buff, out_offset);

            int out_offset_end = in_len * stepsize;
            for (int i = out_offset + offset; i < out_offset_end; i += stepsize) {
                out_buff[i] = (byte) (out_buff[i] & mask);
            }

            return ret;
        }

        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            if (mask_buffer == null || mask_buffer.length < in_buff.length)
                mask_buffer = new byte[in_buff.length];
            System.arraycopy(in_buff, 0, mask_buffer, 0, in_buff.length);
            int in_offset_end = out_len * stepsize;
            for (int i = in_offset + offset; i < in_offset_end; i += stepsize) {
                mask_buffer[i] = (byte) (mask_buffer[i] & mask);
            }
            float[] ret = converter.toFloatArray(mask_buffer, in_offset,
                    out_buff, out_offset, out_len);
            return ret;
        }

    }

    /***************************************************************************
     *
     * 64 bit float, little/big-endian
     *
     **************************************************************************/

    // PCM 64 bit float, little-endian
    private static class AudioFloatConversion64L extends AudioFloatConverter {
        ByteBuffer bytebuffer = null;

        DoubleBuffer floatbuffer = null;

        double[] double_buff = null;

        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int in_len = out_len * 8;
            if (bytebuffer == null || bytebuffer.capacity() < in_len) {
                bytebuffer = ByteBuffer.allocate(in_len).order(
                        ByteOrder.LITTLE_ENDIAN);
                floatbuffer = bytebuffer.asDoubleBuffer();
            }
            bytebuffer.position(0);
            floatbuffer.position(0);
            bytebuffer.put(in_buff, in_offset, in_len);
            if (double_buff == null
                    || double_buff.length < out_len + out_offset)
                double_buff = new double[out_len + out_offset];
            floatbuffer.get(double_buff, out_offset, out_len);
            int out_offset_end = out_offset + out_len;
            for (int i = out_offset; i < out_offset_end; i++) {
                out_buff[i] = (float) double_buff[i];
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int out_len = in_len * 8;
            if (bytebuffer == null || bytebuffer.capacity() < out_len) {
                bytebuffer = ByteBuffer.allocate(out_len).order(
                        ByteOrder.LITTLE_ENDIAN);
                floatbuffer = bytebuffer.asDoubleBuffer();
            }
            floatbuffer.position(0);
            bytebuffer.position(0);
            if (double_buff == null || double_buff.length < in_offset + in_len)
                double_buff = new double[in_offset + in_len];
            int in_offset_end = in_offset + in_len;
            for (int i = in_offset; i < in_offset_end; i++) {
                double_buff[i] = in_buff[i];
            }
            floatbuffer.put(double_buff, in_offset, in_len);
            bytebuffer.get(out_buff, out_offset, out_len);
            return out_buff;
        }
    }

    // PCM 64 bit float, big-endian
    private static class AudioFloatConversion64B extends AudioFloatConverter {
        ByteBuffer bytebuffer = null;

        DoubleBuffer floatbuffer = null;

        double[] double_buff = null;

        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int in_len = out_len * 8;
            if (bytebuffer == null || bytebuffer.capacity() < in_len) {
                bytebuffer = ByteBuffer.allocate(in_len).order(
                        ByteOrder.BIG_ENDIAN);
                floatbuffer = bytebuffer.asDoubleBuffer();
            }
            bytebuffer.position(0);
            floatbuffer.position(0);
            bytebuffer.put(in_buff, in_offset, in_len);
            if (double_buff == null
                    || double_buff.length < out_len + out_offset)
                double_buff = new double[out_len + out_offset];
            floatbuffer.get(double_buff, out_offset, out_len);
            int out_offset_end = out_offset + out_len;
            for (int i = out_offset; i < out_offset_end; i++) {
                out_buff[i] = (float) double_buff[i];
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int out_len = in_len * 8;
            if (bytebuffer == null || bytebuffer.capacity() < out_len) {
                bytebuffer = ByteBuffer.allocate(out_len).order(
                        ByteOrder.BIG_ENDIAN);
                floatbuffer = bytebuffer.asDoubleBuffer();
            }
            floatbuffer.position(0);
            bytebuffer.position(0);
            if (double_buff == null || double_buff.length < in_offset + in_len)
                double_buff = new double[in_offset + in_len];
            int in_offset_end = in_offset + in_len;
            for (int i = in_offset; i < in_offset_end; i++) {
                double_buff[i] = in_buff[i];
            }
            floatbuffer.put(double_buff, in_offset, in_len);
            bytebuffer.get(out_buff, out_offset, out_len);
            return out_buff;
        }
    }

    /***************************************************************************
     *
     * 32 bit float, little/big-endian
     *
     **************************************************************************/

    // PCM 32 bit float, little-endian
    private static class AudioFloatConversion32L extends AudioFloatConverter {
        ByteBuffer bytebuffer = null;

        FloatBuffer floatbuffer = null;

        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int in_len = out_len * 4;
            if (bytebuffer == null || bytebuffer.capacity() < in_len) {
                bytebuffer = ByteBuffer.allocate(in_len).order(
                        ByteOrder.LITTLE_ENDIAN);
                floatbuffer = bytebuffer.asFloatBuffer();
            }
            bytebuffer.position(0);
            floatbuffer.position(0);
            bytebuffer.put(in_buff, in_offset, in_len);
            floatbuffer.get(out_buff, out_offset, out_len);
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int out_len = in_len * 4;
            if (bytebuffer == null || bytebuffer.capacity() < out_len) {
                bytebuffer = ByteBuffer.allocate(out_len).order(
                        ByteOrder.LITTLE_ENDIAN);
                floatbuffer = bytebuffer.asFloatBuffer();
            }
            floatbuffer.position(0);
            bytebuffer.position(0);
            floatbuffer.put(in_buff, in_offset, in_len);
            bytebuffer.get(out_buff, out_offset, out_len);
            return out_buff;
        }
    }

    // PCM 32 bit float, big-endian
    private static class AudioFloatConversion32B extends AudioFloatConverter {
        ByteBuffer bytebuffer = null;

        FloatBuffer floatbuffer = null;

        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int in_len = out_len * 4;
            if (bytebuffer == null || bytebuffer.capacity() < in_len) {
                bytebuffer = ByteBuffer.allocate(in_len).order(
                        ByteOrder.BIG_ENDIAN);
                floatbuffer = bytebuffer.asFloatBuffer();
            }
            bytebuffer.position(0);
            floatbuffer.position(0);
            bytebuffer.put(in_buff, in_offset, in_len);
            floatbuffer.get(out_buff, out_offset, out_len);
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int out_len = in_len * 4;
            if (bytebuffer == null || bytebuffer.capacity() < out_len) {
                bytebuffer = ByteBuffer.allocate(out_len).order(
                        ByteOrder.BIG_ENDIAN);
                floatbuffer = bytebuffer.asFloatBuffer();
            }
            floatbuffer.position(0);
            bytebuffer.position(0);
            floatbuffer.put(in_buff, in_offset, in_len);
            bytebuffer.get(out_buff, out_offset, out_len);
            return out_buff;
        }
    }

    /***************************************************************************
     *
     * 8 bit signed/unsigned
     *
     **************************************************************************/

    // PCM 8 bit, signed
    private static class AudioFloatConversion8S extends AudioFloatConverter {
        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                byte x = in_buff[ix++];
                out_buff[ox++] = x > 0 ? x / 127.0f : x / 128.0f;
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                final float x = in_buff[ix++];
                out_buff[ox++] = (byte) (x > 0 ? x * 127 : x * 128);
            }
            return out_buff;
        }
    }

    // PCM 8 bit, unsigned
    private static class AudioFloatConversion8U extends AudioFloatConverter {
        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                byte x = (byte) (in_buff[ix++] - 128);
                out_buff[ox++] = x > 0 ? x / 127.0f : x / 128.0f;
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                float x = in_buff[ix++];
                out_buff[ox++] = (byte) (128 + (x > 0 ? x * 127 : x * 128));
            }
            return out_buff;
        }
    }

    /***************************************************************************
     *
     * 16 bit signed/unsigned, little/big-endian
     *
     **************************************************************************/

    // PCM 16 bit, signed, little-endian
    private static class AudioFloatConversion16SL extends AudioFloatConverter {
        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int len = out_offset + out_len;
            for (int ox = out_offset; ox < len; ox++) {
                short x = (short) (in_buff[ix++] & 0xFF | (in_buff[ix++] << 8));
                out_buff[ox] = x > 0 ? x / 32767.0f : x / 32768.0f;
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ox = out_offset;
            int len = in_offset + in_len;
            for (int ix = in_offset; ix < len; ix++) {
                float f = in_buff[ix];
                short x = (short) (f > 0 ? f * 32767 : f * 32768);
                out_buff[ox++] = (byte) x;
                out_buff[ox++] = (byte) (x >>> 8);
            }
            return out_buff;
        }
    }

    // PCM 16 bit, signed, big-endian
    private static class AudioFloatConversion16SB extends AudioFloatConverter {
        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                short x = (short) ((in_buff[ix++] << 8) | (in_buff[ix++] & 0xFF));
                out_buff[ox++] = x > 0 ? x / 32767.0f : x / 32768.0f;
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                float f = in_buff[ix++];
                short x = (short) (f > 0 ? f * 32767.0f : f * 32768.0f);
                out_buff[ox++] = (byte) (x >>> 8);
                out_buff[ox++] = (byte) x;
            }
            return out_buff;
        }
    }

    // PCM 16 bit, unsigned, little-endian
    private static class AudioFloatConversion16UL extends AudioFloatConverter {
        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                int x = (in_buff[ix++] & 0xFF) | ((in_buff[ix++] & 0xFF) << 8);
                x -= 32768;
                out_buff[ox++] = x > 0 ? x / 32767.0f : x / 32768.0f;
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                float f = in_buff[ix++];
                int x = 32768 + (int) (f > 0 ? f * 32767 : f * 32768);
                out_buff[ox++] = (byte) x;
                out_buff[ox++] = (byte) (x >>> 8);
            }
            return out_buff;
        }
    }

    // PCM 16 bit, unsigned, big-endian
    private static class AudioFloatConversion16UB extends AudioFloatConverter {
        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                int x = ((in_buff[ix++] & 0xFF) << 8) | (in_buff[ix++] & 0xFF);
                x -= 32768;
                out_buff[ox++] = x > 0 ? x / 32767.0f : x / 32768.0f;
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                float f = in_buff[ix++];
                int x = 32768 + (int) (f > 0 ? f * 32767 : f * 32768);
                out_buff[ox++] = (byte) (x >>> 8);
                out_buff[ox++] = (byte) x;
            }
            return out_buff;
        }
    }

    /***************************************************************************
     *
     * 24 bit signed/unsigned, little/big-endian
     *
     **************************************************************************/

    // PCM 24 bit, signed, little-endian
    private static class AudioFloatConversion24SL extends AudioFloatConverter {
        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                int x = (in_buff[ix++] & 0xFF) | ((in_buff[ix++] & 0xFF) << 8)
                        | ((in_buff[ix++] & 0xFF) << 16);
                if (x > 0x7FFFFF)
                    x -= 0x1000000;
                out_buff[ox++] = x > 0 ? x / 8388607.0f : x / 8388608.0f;
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                float f = in_buff[ix++];
                int x = (int) (f > 0 ? f * 8388607.0f : f * 8388608.0f);
                if (x < 0)
                    x += 0x1000000;
                out_buff[ox++] = (byte) x;
                out_buff[ox++] = (byte) (x >>> 8);
                out_buff[ox++] = (byte) (x >>> 16);
            }
            return out_buff;
        }
    }

    // PCM 24 bit, signed, big-endian
    private static class AudioFloatConversion24SB extends AudioFloatConverter {
        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                int x = ((in_buff[ix++] & 0xFF) << 16)
                        | ((in_buff[ix++] & 0xFF) << 8) | (in_buff[ix++] & 0xFF);
                if (x > 0x7FFFFF)
                    x -= 0x1000000;
                out_buff[ox++] = x > 0 ? x / 8388607.0f : x / 8388608.0f;
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                float f = in_buff[ix++];
                int x = (int) (f > 0 ? f * 8388607.0f : f * 8388608.0f);
                if (x < 0)
                    x += 0x1000000;
                out_buff[ox++] = (byte) (x >>> 16);
                out_buff[ox++] = (byte) (x >>> 8);
                out_buff[ox++] = (byte) x;
            }
            return out_buff;
        }
    }

    // PCM 24 bit, unsigned, little-endian
    private static class AudioFloatConversion24UL extends AudioFloatConverter {
        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                int x = (in_buff[ix++] & 0xFF) | ((in_buff[ix++] & 0xFF) << 8)
                        | ((in_buff[ix++] & 0xFF) << 16);
                x -= 0x800000;
                out_buff[ox++] = x > 0 ? x / 8388607.0f : x / 8388608.0f;
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                float f = in_buff[ix++];
                int x = (int) (f > 0 ? f * 8388607.0f : f * 8388608.0f);
                x += 0x800000;
                out_buff[ox++] = (byte) x;
                out_buff[ox++] = (byte) (x >>> 8);
                out_buff[ox++] = (byte) (x >>> 16);
            }
            return out_buff;
        }
    }

    // PCM 24 bit, unsigned, big-endian
    private static class AudioFloatConversion24UB extends AudioFloatConverter {
        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                int x = ((in_buff[ix++] & 0xFF) << 16)
                        | ((in_buff[ix++] & 0xFF) << 8) | (in_buff[ix++] & 0xFF);
                x -= 0x800000;
                out_buff[ox++] = x > 0 ? x / 8388607.0f : x / 8388608.0f;
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                float f = in_buff[ix++];
                int x = (int) (f > 0 ? f * 8388607.0f : f * 8388608.0f);
                x += 8388608;
                out_buff[ox++] = (byte) (x >>> 16);
                out_buff[ox++] = (byte) (x >>> 8);
                out_buff[ox++] = (byte) x;
            }
            return out_buff;
        }
    }

    /***************************************************************************
     *
     * 32 bit signed/unsigned, little/big-endian
     *
     **************************************************************************/

    // PCM 32 bit, signed, little-endian
    private static class AudioFloatConversion32SL extends AudioFloatConverter {
        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                int x = (in_buff[ix++] & 0xFF) | ((in_buff[ix++] & 0xFF) << 8) |
                        ((in_buff[ix++] & 0xFF) << 16) |
                        ((in_buff[ix++] & 0xFF) << 24);
                out_buff[ox++] = x * (1.0f / (float)0x7FFFFFFF);
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                int x = (int) (in_buff[ix++] * (float)0x7FFFFFFF);
                out_buff[ox++] = (byte) x;
                out_buff[ox++] = (byte) (x >>> 8);
                out_buff[ox++] = (byte) (x >>> 16);
                out_buff[ox++] = (byte) (x >>> 24);
            }
            return out_buff;
        }
    }

    // PCM 32 bit, signed, big-endian
    private static class AudioFloatConversion32SB extends AudioFloatConverter {
        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                int x = ((in_buff[ix++] & 0xFF) << 24) |
                        ((in_buff[ix++] & 0xFF) << 16) |
                        ((in_buff[ix++] & 0xFF) << 8) | (in_buff[ix++] & 0xFF);
                out_buff[ox++] = x * (1.0f / (float)0x7FFFFFFF);
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                int x = (int) (in_buff[ix++] * (float)0x7FFFFFFF);
                out_buff[ox++] = (byte) (x >>> 24);
                out_buff[ox++] = (byte) (x >>> 16);
                out_buff[ox++] = (byte) (x >>> 8);
                out_buff[ox++] = (byte) x;
            }
            return out_buff;
        }
    }

    // PCM 32 bit, unsigned, little-endian
    private static class AudioFloatConversion32UL extends AudioFloatConverter {
        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                int x = (in_buff[ix++] & 0xFF) | ((in_buff[ix++] & 0xFF) << 8) |
                        ((in_buff[ix++] & 0xFF) << 16) |
                        ((in_buff[ix++] & 0xFF) << 24);
                x -= 0x80000000;
                out_buff[ox++] = x * (1.0f / (float)0x7FFFFFFF);
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                int x = (int) (in_buff[ix++] * (float)0x7FFFFFFF);
                x += 0x80000000;
                out_buff[ox++] = (byte) x;
                out_buff[ox++] = (byte) (x >>> 8);
                out_buff[ox++] = (byte) (x >>> 16);
                out_buff[ox++] = (byte) (x >>> 24);
            }
            return out_buff;
        }
    }

    // PCM 32 bit, unsigned, big-endian
    private static class AudioFloatConversion32UB extends AudioFloatConverter {

        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                int x = ((in_buff[ix++] & 0xFF) << 24) |
                        ((in_buff[ix++] & 0xFF) << 16) |
                        ((in_buff[ix++] & 0xFF) << 8) | (in_buff[ix++] & 0xFF);
                x -= 0x80000000;
                out_buff[ox++] = x * (1.0f / (float)0x7FFFFFFF);
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                int x = (int) (in_buff[ix++] * (float)0x7FFFFFFF);
                x += 0x80000000;
                out_buff[ox++] = (byte) (x >>> 24);
                out_buff[ox++] = (byte) (x >>> 16);
                out_buff[ox++] = (byte) (x >>> 8);
                out_buff[ox++] = (byte) x;
            }
            return out_buff;
        }
    }

    /***************************************************************************
     *
     * 32+ bit signed/unsigned, little/big-endian
     *
     **************************************************************************/

    // PCM 32+ bit, signed, little-endian
    private static class AudioFloatConversion32xSL extends AudioFloatConverter {

        private final int xbytes;

        AudioFloatConversion32xSL(int xbytes) {
            this.xbytes = xbytes;
        }

        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                ix += xbytes;
                int x = (in_buff[ix++] & 0xFF) | ((in_buff[ix++] & 0xFF) << 8)
                        | ((in_buff[ix++] & 0xFF) << 16)
                        | ((in_buff[ix++] & 0xFF) << 24);
                out_buff[ox++] = x * (1.0f / (float)0x7FFFFFFF);
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                int x = (int) (in_buff[ix++] * (float)0x7FFFFFFF);
                for (int j = 0; j < xbytes; j++) {
                    out_buff[ox++] = 0;
                }
                out_buff[ox++] = (byte) x;
                out_buff[ox++] = (byte) (x >>> 8);
                out_buff[ox++] = (byte) (x >>> 16);
                out_buff[ox++] = (byte) (x >>> 24);
            }
            return out_buff;
        }
    }

    // PCM 32+ bit, signed, big-endian
    private static class AudioFloatConversion32xSB extends AudioFloatConverter {

        private final int xbytes;

        AudioFloatConversion32xSB(int xbytes) {
            this.xbytes = xbytes;
        }

        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                int x = ((in_buff[ix++] & 0xFF) << 24)
                        | ((in_buff[ix++] & 0xFF) << 16)
                        | ((in_buff[ix++] & 0xFF) << 8)
                        | (in_buff[ix++] & 0xFF);
                ix += xbytes;
                out_buff[ox++] = x * (1.0f / (float)0x7FFFFFFF);
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                int x = (int) (in_buff[ix++] * (float)0x7FFFFFFF);
                out_buff[ox++] = (byte) (x >>> 24);
                out_buff[ox++] = (byte) (x >>> 16);
                out_buff[ox++] = (byte) (x >>> 8);
                out_buff[ox++] = (byte) x;
                for (int j = 0; j < xbytes; j++) {
                    out_buff[ox++] = 0;
                }
            }
            return out_buff;
        }
    }

    // PCM 32+ bit, unsigned, little-endian
    private static class AudioFloatConversion32xUL extends AudioFloatConverter {

        private final int xbytes;

        AudioFloatConversion32xUL(int xbytes) {
            this.xbytes = xbytes;
        }

        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                ix += xbytes;
                int x = (in_buff[ix++] & 0xFF) | ((in_buff[ix++] & 0xFF) << 8)
                        | ((in_buff[ix++] & 0xFF) << 16)
                        | ((in_buff[ix++] & 0xFF) << 24);
                x -= 0x80000000;
                out_buff[ox++] = x * (1.0f / (float)0x7FFFFFFF);
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                int x = (int) (in_buff[ix++] * (float)0x7FFFFFFF);
                x += 0x80000000;
                for (int j = 0; j < xbytes; j++) {
                    out_buff[ox++] = 0;
                }
                out_buff[ox++] = (byte) x;
                out_buff[ox++] = (byte) (x >>> 8);
                out_buff[ox++] = (byte) (x >>> 16);
                out_buff[ox++] = (byte) (x >>> 24);
            }
            return out_buff;
        }
    }

    // PCM 32+ bit, unsigned, big-endian
    private static class AudioFloatConversion32xUB extends AudioFloatConverter {

        private final int xbytes;

        AudioFloatConversion32xUB(int xbytes) {
            this.xbytes = xbytes;
        }

        @Override
        public float[] toFloatArray(byte[] in_buff, int in_offset,
                                    float[] out_buff, int out_offset, int out_len) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < out_len; i++) {
                int x = ((in_buff[ix++] & 0xFF) << 24) |
                        ((in_buff[ix++] & 0xFF) << 16) |
                        ((in_buff[ix++] & 0xFF) << 8) | (in_buff[ix++] & 0xFF);
                ix += xbytes;
                x -= 0x80000000;
                out_buff[ox++] = x * (1.0f / 2147483647.0f);
            }
            return out_buff;
        }

        @Override
        public byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                  byte[] out_buff, int out_offset) {
            int ix = in_offset;
            int ox = out_offset;
            for (int i = 0; i < in_len; i++) {
                int x = (int) (in_buff[ix++] * 2147483647.0f);
                x += 0x80000000;
                out_buff[ox++] = (byte) (x >>> 24);
                out_buff[ox++] = (byte) (x >>> 16);
                out_buff[ox++] = (byte) (x >>> 8);
                out_buff[ox++] = (byte) x;
                for (int j = 0; j < xbytes; j++) {
                    out_buff[ox++] = 0;
                }
            }
            return out_buff;
        }
    }

    public static AudioFloatConverter getConverter(AudioFormat format) {
        AudioFloatConverter conv = null;
        if (format.getFrameSize() == 0)
            return null;
        if (format.getFrameSize() !=
                ((format.getSampleSizeInBits() + 7) / 8) * format.getChannels()) {
            return null;
        }
        if (format.getEncoding().equals(Encoding.PCM_SIGNED)) {
            if (format.isBigEndian()) {
                if (format.getSampleSizeInBits() <= 8) {
                    conv = new AudioFloatConversion8S();
                } else if (format.getSampleSizeInBits() > 8 &&
                      format.getSampleSizeInBits() <= 16) {
                    conv = new AudioFloatConversion16SB();
                } else if (format.getSampleSizeInBits() > 16 &&
                      format.getSampleSizeInBits() <= 24) {
                    conv = new AudioFloatConversion24SB();
                } else if (format.getSampleSizeInBits() > 24 &&
                      format.getSampleSizeInBits() <= 32) {
                    conv = new AudioFloatConversion32SB();
                } else if (format.getSampleSizeInBits() > 32) {
                    conv = new AudioFloatConversion32xSB(((format
                            .getSampleSizeInBits() + 7) / 8) - 4);
                }
            } else {
                if (format.getSampleSizeInBits() <= 8) {
                    conv = new AudioFloatConversion8S();
                } else if (format.getSampleSizeInBits() > 8 &&
                         format.getSampleSizeInBits() <= 16) {
                    conv = new AudioFloatConversion16SL();
                } else if (format.getSampleSizeInBits() > 16 &&
                         format.getSampleSizeInBits() <= 24) {
                    conv = new AudioFloatConversion24SL();
                } else if (format.getSampleSizeInBits() > 24 &&
                         format.getSampleSizeInBits() <= 32) {
                    conv = new AudioFloatConversion32SL();
                } else if (format.getSampleSizeInBits() > 32) {
                    conv = new AudioFloatConversion32xSL(((format
                            .getSampleSizeInBits() + 7) / 8) - 4);
                }
            }
        } else if (format.getEncoding().equals(Encoding.PCM_UNSIGNED)) {
            if (format.isBigEndian()) {
                if (format.getSampleSizeInBits() <= 8) {
                    conv = new AudioFloatConversion8U();
                } else if (format.getSampleSizeInBits() > 8 &&
                        format.getSampleSizeInBits() <= 16) {
                    conv = new AudioFloatConversion16UB();
                } else if (format.getSampleSizeInBits() > 16 &&
                        format.getSampleSizeInBits() <= 24) {
                    conv = new AudioFloatConversion24UB();
                } else if (format.getSampleSizeInBits() > 24 &&
                        format.getSampleSizeInBits() <= 32) {
                    conv = new AudioFloatConversion32UB();
                } else if (format.getSampleSizeInBits() > 32) {
                    conv = new AudioFloatConversion32xUB(((
                            format.getSampleSizeInBits() + 7) / 8) - 4);
                }
            } else {
                if (format.getSampleSizeInBits() <= 8) {
                    conv = new AudioFloatConversion8U();
                } else if (format.getSampleSizeInBits() > 8 &&
                        format.getSampleSizeInBits() <= 16) {
                    conv = new AudioFloatConversion16UL();
                } else if (format.getSampleSizeInBits() > 16 &&
                        format.getSampleSizeInBits() <= 24) {
                    conv = new AudioFloatConversion24UL();
                } else if (format.getSampleSizeInBits() > 24 &&
                        format.getSampleSizeInBits() <= 32) {
                    conv = new AudioFloatConversion32UL();
                } else if (format.getSampleSizeInBits() > 32) {
                    conv = new AudioFloatConversion32xUL(((
                            format.getSampleSizeInBits() + 7) / 8) - 4);
                }
            }
        } else if (format.getEncoding().equals(Encoding.PCM_FLOAT)) {
            if (format.getSampleSizeInBits() == 32) {
                if (format.isBigEndian())
                    conv = new AudioFloatConversion32B();
                else
                    conv = new AudioFloatConversion32L();
            } else if (format.getSampleSizeInBits() == 64) {
                if (format.isBigEndian())
                    conv = new AudioFloatConversion64B();
                else
                    conv = new AudioFloatConversion64L();
            }

        }

        if ((format.getEncoding().equals(Encoding.PCM_SIGNED) ||
                format.getEncoding().equals(Encoding.PCM_UNSIGNED)) &&
                (format.getSampleSizeInBits() % 8 != 0)) {
            conv = new AudioFloatLSBFilter(conv, format);
        }

        if (conv != null)
            conv.format = format;
        return conv;
    }

    private AudioFormat format;

    public final AudioFormat getFormat() {
        return format;
    }

    public abstract float[] toFloatArray(byte[] in_buff, int in_offset,
            float[] out_buff, int out_offset, int out_len);

    public final float[] toFloatArray(byte[] in_buff, float[] out_buff,
            int out_offset, int out_len) {
        return toFloatArray(in_buff, 0, out_buff, out_offset, out_len);
    }

    public final float[] toFloatArray(byte[] in_buff, int in_offset,
            float[] out_buff, int out_len) {
        return toFloatArray(in_buff, in_offset, out_buff, 0, out_len);
    }

    public final float[] toFloatArray(byte[] in_buff, float[] out_buff,
                                      int out_len) {
        return toFloatArray(in_buff, 0, out_buff, 0, out_len);
    }

    public final float[] toFloatArray(byte[] in_buff, float[] out_buff) {
        return toFloatArray(in_buff, 0, out_buff, 0, out_buff.length);
    }

    public abstract byte[] toByteArray(float[] in_buff, int in_offset,
            int in_len, byte[] out_buff, int out_offset);

    public final byte[] toByteArray(float[] in_buff, int in_len,
                                    byte[] out_buff, int out_offset) {
        return toByteArray(in_buff, 0, in_len, out_buff, out_offset);
    }

    public final byte[] toByteArray(float[] in_buff, int in_offset, int in_len,
                                    byte[] out_buff) {
        return toByteArray(in_buff, in_offset, in_len, out_buff, 0);
    }

    public final byte[] toByteArray(float[] in_buff, int in_len,
                                    byte[] out_buff) {
        return toByteArray(in_buff, 0, in_len, out_buff, 0);
    }

    public final byte[] toByteArray(float[] in_buff, byte[] out_buff) {
        return toByteArray(in_buff, 0, in_buff.length, out_buff, 0);
    }
}
