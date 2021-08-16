/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;

/**
 * This class is used to convert between 8,16,24,32 bit signed/unsigned
 * big/litle endian fixed/floating stereo/mono/multi-channel audio streams and
 * perform sample-rate conversion if needed.
 *
 * @author Karl Helgason
 */
public final class AudioFloatFormatConverter extends FormatConversionProvider {

    private static class AudioFloatFormatConverterInputStream extends
            InputStream {
        private final AudioFloatConverter converter;

        private final AudioFloatInputStream stream;

        private float[] readfloatbuffer;

        private final int fsize;

        AudioFloatFormatConverterInputStream(AudioFormat targetFormat,
                AudioFloatInputStream stream) {
            this.stream = stream;
            converter = AudioFloatConverter.getConverter(targetFormat);
            fsize = ((targetFormat.getSampleSizeInBits() + 7) / 8);
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            int ret = read(b);
            if (ret < 0)
                return ret;
            return b[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {

            int flen = len / fsize;
            if (readfloatbuffer == null || readfloatbuffer.length < flen)
                readfloatbuffer = new float[flen];
            int ret = stream.read(readfloatbuffer, 0, flen);
            if (ret < 0)
                return ret;
            converter.toByteArray(readfloatbuffer, 0, ret, b, off);
            return ret * fsize;
        }

        @Override
        public int available() throws IOException {
            int ret = stream.available();
            if (ret < 0)
                return ret;
            return ret * fsize;
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }

        @Override
        public synchronized void mark(int readlimit) {
            stream.mark(readlimit * fsize);
        }

        @Override
        public boolean markSupported() {
            return stream.markSupported();
        }

        @Override
        public synchronized void reset() throws IOException {
            stream.reset();
        }

        @Override
        public long skip(long n) throws IOException {
            long ret = stream.skip(n / fsize);
            if (ret < 0)
                return ret;
            return ret * fsize;
        }

    }

    private static class AudioFloatInputStreamChannelMixer extends
            AudioFloatInputStream {

        private final int targetChannels;

        private final int sourceChannels;

        private final AudioFloatInputStream ais;

        private final AudioFormat targetFormat;

        private float[] conversion_buffer;

        AudioFloatInputStreamChannelMixer(AudioFloatInputStream ais,
                int targetChannels) {
            this.sourceChannels = ais.getFormat().getChannels();
            this.targetChannels = targetChannels;
            this.ais = ais;
            AudioFormat format = ais.getFormat();
            targetFormat = new AudioFormat(format.getEncoding(), format
                    .getSampleRate(), format.getSampleSizeInBits(),
                    targetChannels, (format.getFrameSize() / sourceChannels)
                            * targetChannels, format.getFrameRate(), format
                            .isBigEndian());
        }

        @Override
        public int available() throws IOException {
            return (ais.available() / sourceChannels) * targetChannels;
        }

        @Override
        public void close() throws IOException {
            ais.close();
        }

        @Override
        public AudioFormat getFormat() {
            return targetFormat;
        }

        @Override
        public long getFrameLength() {
            return ais.getFrameLength();
        }

        @Override
        public void mark(int readlimit) {
            ais.mark((readlimit / targetChannels) * sourceChannels);
        }

        @Override
        public boolean markSupported() {
            return ais.markSupported();
        }

        @Override
        public int read(float[] b, int off, int len) throws IOException {
            int len2 = (len / targetChannels) * sourceChannels;
            if (conversion_buffer == null || conversion_buffer.length < len2)
                conversion_buffer = new float[len2];
            int ret = ais.read(conversion_buffer, 0, len2);
            if (ret < 0)
                return ret;
            if (sourceChannels == 1) {
                int cs = targetChannels;
                for (int c = 0; c < targetChannels; c++) {
                    for (int i = 0, ix = off + c; i < len2; i++, ix += cs) {
                        b[ix] = conversion_buffer[i];
                    }
                }
            } else if (targetChannels == 1) {
                int cs = sourceChannels;
                for (int i = 0, ix = off; i < len2; i += cs, ix++) {
                    b[ix] = conversion_buffer[i];
                }
                for (int c = 1; c < sourceChannels; c++) {
                    for (int i = c, ix = off; i < len2; i += cs, ix++) {
                        b[ix] += conversion_buffer[i];
                    }
                }
                float vol = 1f / ((float) sourceChannels);
                for (int i = 0, ix = off; i < len2; i += cs, ix++) {
                    b[ix] *= vol;
                }
            } else {
                int minChannels = Math.min(sourceChannels, targetChannels);
                int off_len = off + len;
                int ct = targetChannels;
                int cs = sourceChannels;
                for (int c = 0; c < minChannels; c++) {
                    for (int i = off + c, ix = c; i < off_len; i += ct, ix += cs) {
                        b[i] = conversion_buffer[ix];
                    }
                }
                for (int c = minChannels; c < targetChannels; c++) {
                    for (int i = off + c; i < off_len; i += ct) {
                        b[i] = 0;
                    }
                }
            }
            return (ret / sourceChannels) * targetChannels;
        }

        @Override
        public void reset() throws IOException {
            ais.reset();
        }

        @Override
        public long skip(long len) throws IOException {
            long ret = ais.skip((len / targetChannels) * sourceChannels);
            if (ret < 0)
                return ret;
            return (ret / sourceChannels) * targetChannels;
        }

    }

    private static class AudioFloatInputStreamResampler extends
            AudioFloatInputStream {

        private final AudioFloatInputStream ais;

        private final AudioFormat targetFormat;

        private float[] skipbuffer;

        private SoftAbstractResampler resampler;

        private final float[] pitch = new float[1];

        private final float[] ibuffer2;

        private final float[][] ibuffer;

        private float ibuffer_index = 0;

        private int ibuffer_len = 0;

        private final int nrofchannels;

        private float[][] cbuffer;

        private final int buffer_len = 512;

        private final int pad;

        private final int pad2;

        private final float[] ix = new float[1];

        private final int[] ox = new int[1];

        private float[][] mark_ibuffer = null;

        private float mark_ibuffer_index = 0;

        private int mark_ibuffer_len = 0;

        AudioFloatInputStreamResampler(AudioFloatInputStream ais,
                AudioFormat format) {
            this.ais = ais;
            AudioFormat sourceFormat = ais.getFormat();
            targetFormat = new AudioFormat(sourceFormat.getEncoding(), format
                    .getSampleRate(), sourceFormat.getSampleSizeInBits(),
                    sourceFormat.getChannels(), sourceFormat.getFrameSize(),
                    format.getSampleRate(), sourceFormat.isBigEndian());
            nrofchannels = targetFormat.getChannels();
            Object interpolation = format.getProperty("interpolation");
            if (interpolation != null && (interpolation instanceof String)) {
                String resamplerType = (String) interpolation;
                if (resamplerType.equalsIgnoreCase("point"))
                    this.resampler = new SoftPointResampler();
                if (resamplerType.equalsIgnoreCase("linear"))
                    this.resampler = new SoftLinearResampler2();
                if (resamplerType.equalsIgnoreCase("linear1"))
                    this.resampler = new SoftLinearResampler();
                if (resamplerType.equalsIgnoreCase("linear2"))
                    this.resampler = new SoftLinearResampler2();
                if (resamplerType.equalsIgnoreCase("cubic"))
                    this.resampler = new SoftCubicResampler();
                if (resamplerType.equalsIgnoreCase("lanczos"))
                    this.resampler = new SoftLanczosResampler();
                if (resamplerType.equalsIgnoreCase("sinc"))
                    this.resampler = new SoftSincResampler();
            }
            if (resampler == null)
                resampler = new SoftLinearResampler2(); // new
                                                        // SoftLinearResampler2();
            pitch[0] = sourceFormat.getSampleRate() / format.getSampleRate();
            pad = resampler.getPadding();
            pad2 = pad * 2;
            ibuffer = new float[nrofchannels][buffer_len + pad2];
            ibuffer2 = new float[nrofchannels * buffer_len];
            ibuffer_index = buffer_len + pad;
            ibuffer_len = buffer_len;
        }

        @Override
        public int available() throws IOException {
            return 0;
        }

        @Override
        public void close() throws IOException {
            ais.close();
        }

        @Override
        public AudioFormat getFormat() {
            return targetFormat;
        }

        @Override
        public long getFrameLength() {
            return AudioSystem.NOT_SPECIFIED; // ais.getFrameLength();
        }

        @Override
        public void mark(int readlimit) {
            ais.mark((int) (readlimit * pitch[0]));
            mark_ibuffer_index = ibuffer_index;
            mark_ibuffer_len = ibuffer_len;
            if (mark_ibuffer == null) {
                mark_ibuffer = new float[ibuffer.length][ibuffer[0].length];
            }
            for (int c = 0; c < ibuffer.length; c++) {
                float[] from = ibuffer[c];
                float[] to = mark_ibuffer[c];
                for (int i = 0; i < to.length; i++) {
                    to[i] = from[i];
                }
            }
        }

        @Override
        public boolean markSupported() {
            return ais.markSupported();
        }

        private void readNextBuffer() throws IOException {

            if (ibuffer_len == -1)
                return;

            for (int c = 0; c < nrofchannels; c++) {
                float[] buff = ibuffer[c];
                int buffer_len_pad = ibuffer_len + pad2;
                for (int i = ibuffer_len, ix = 0; i < buffer_len_pad; i++, ix++) {
                    buff[ix] = buff[i];
                }
            }

            ibuffer_index -= (ibuffer_len);

            ibuffer_len = ais.read(ibuffer2);
            if (ibuffer_len >= 0) {
                while (ibuffer_len < ibuffer2.length) {
                    int ret = ais.read(ibuffer2, ibuffer_len, ibuffer2.length
                            - ibuffer_len);
                    if (ret == -1)
                        break;
                    ibuffer_len += ret;
                }
                Arrays.fill(ibuffer2, ibuffer_len, ibuffer2.length, 0);
                ibuffer_len /= nrofchannels;
            } else {
                Arrays.fill(ibuffer2, 0, ibuffer2.length, 0);
            }

            int ibuffer2_len = ibuffer2.length;
            for (int c = 0; c < nrofchannels; c++) {
                float[] buff = ibuffer[c];
                for (int i = c, ix = pad2; i < ibuffer2_len; i += nrofchannels, ix++) {
                    buff[ix] = ibuffer2[i];
                }
            }

        }

        @Override
        public int read(float[] b, int off, int len) throws IOException {

            if (cbuffer == null || cbuffer[0].length < len / nrofchannels) {
                cbuffer = new float[nrofchannels][len / nrofchannels];
            }
            if (ibuffer_len == -1)
                return -1;
            if (len < 0)
                return 0;
            int offlen = off + len;
            int remain = len / nrofchannels;
            int destPos = 0;
            int in_end = ibuffer_len;
            while (remain > 0) {
                if (ibuffer_len >= 0) {
                    if (ibuffer_index >= (ibuffer_len + pad))
                        readNextBuffer();
                    in_end = ibuffer_len + pad;
                }

                if (ibuffer_len < 0) {
                    in_end = pad2;
                    if (ibuffer_index >= in_end)
                        break;
                }

                if (ibuffer_index < 0)
                    break;
                int preDestPos = destPos;
                for (int c = 0; c < nrofchannels; c++) {
                    ix[0] = ibuffer_index;
                    ox[0] = destPos;
                    float[] buff = ibuffer[c];
                    resampler.interpolate(buff, ix, in_end, pitch, 0,
                            cbuffer[c], ox, len / nrofchannels);
                }
                ibuffer_index = ix[0];
                destPos = ox[0];
                remain -= destPos - preDestPos;
            }
            for (int c = 0; c < nrofchannels; c++) {
                int ix = 0;
                float[] buff = cbuffer[c];
                for (int i = c + off; i < offlen; i += nrofchannels) {
                    b[i] = buff[ix++];
                }
            }
            return len - remain * nrofchannels;
        }

        @Override
        public void reset() throws IOException {
            ais.reset();
            if (mark_ibuffer == null)
                return;
            ibuffer_index = mark_ibuffer_index;
            ibuffer_len = mark_ibuffer_len;
            for (int c = 0; c < ibuffer.length; c++) {
                float[] from = mark_ibuffer[c];
                float[] to = ibuffer[c];
                for (int i = 0; i < to.length; i++) {
                    to[i] = from[i];
                }
            }

        }

        @Override
        public long skip(long len) throws IOException {
            if (len < 0)
                return 0;
            if (skipbuffer == null)
                skipbuffer = new float[1024 * targetFormat.getFrameSize()];
            float[] l_skipbuffer = skipbuffer;
            long remain = len;
            while (remain > 0) {
                int ret = read(l_skipbuffer, 0, (int) Math.min(remain,
                        skipbuffer.length));
                if (ret < 0) {
                    if (remain == len)
                        return ret;
                    break;
                }
                remain -= ret;
            }
            return len - remain;

        }

    }

    private final Encoding[] formats = {Encoding.PCM_SIGNED,
                                        Encoding.PCM_UNSIGNED,
                                        Encoding.PCM_FLOAT};

    @Override
    public AudioInputStream getAudioInputStream(Encoding targetEncoding,
                                                AudioInputStream sourceStream) {
        if (!isConversionSupported(targetEncoding, sourceStream.getFormat())) {
            throw new IllegalArgumentException(
                    "Unsupported conversion: " + sourceStream.getFormat()
                            .toString() + " to " + targetEncoding.toString());
        }
        if (sourceStream.getFormat().getEncoding().equals(targetEncoding))
            return sourceStream;
        AudioFormat format = sourceStream.getFormat();
        int channels = format.getChannels();
        Encoding encoding = targetEncoding;
        float samplerate = format.getSampleRate();
        int bits = format.getSampleSizeInBits();
        boolean bigendian = format.isBigEndian();
        if (targetEncoding.equals(Encoding.PCM_FLOAT))
            bits = 32;
        AudioFormat targetFormat = new AudioFormat(encoding, samplerate, bits,
                channels, channels * bits / 8, samplerate, bigendian);
        return getAudioInputStream(targetFormat, sourceStream);
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat,
                                                AudioInputStream sourceStream) {
        if (!isConversionSupported(targetFormat, sourceStream.getFormat()))
            throw new IllegalArgumentException("Unsupported conversion: "
                    + sourceStream.getFormat().toString() + " to "
                    + targetFormat.toString());
        return getAudioInputStream(targetFormat, AudioFloatInputStream
                .getInputStream(sourceStream));
    }

    public AudioInputStream getAudioInputStream(AudioFormat targetFormat,
            AudioFloatInputStream sourceStream) {

        if (!isConversionSupported(targetFormat, sourceStream.getFormat()))
            throw new IllegalArgumentException("Unsupported conversion: "
                    + sourceStream.getFormat().toString() + " to "
                    + targetFormat.toString());
        if (targetFormat.getChannels() != sourceStream.getFormat()
                .getChannels())
            sourceStream = new AudioFloatInputStreamChannelMixer(sourceStream,
                    targetFormat.getChannels());
        if (Math.abs(targetFormat.getSampleRate()
                - sourceStream.getFormat().getSampleRate()) > 0.000001)
            sourceStream = new AudioFloatInputStreamResampler(sourceStream,
                    targetFormat);
        return new AudioInputStream(new AudioFloatFormatConverterInputStream(
                targetFormat, sourceStream), targetFormat, sourceStream
                .getFrameLength());
    }

    @Override
    public Encoding[] getSourceEncodings() {
        return new Encoding[] { Encoding.PCM_SIGNED, Encoding.PCM_UNSIGNED,
                Encoding.PCM_FLOAT };
    }

    @Override
    public Encoding[] getTargetEncodings() {
        return getSourceEncodings();
    }

    @Override
    public Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (AudioFloatConverter.getConverter(sourceFormat) == null)
            return new Encoding[0];
        return new Encoding[] { Encoding.PCM_SIGNED, Encoding.PCM_UNSIGNED,
                Encoding.PCM_FLOAT };
    }

    @Override
    public AudioFormat[] getTargetFormats(Encoding targetEncoding,
                                          AudioFormat sourceFormat) {
        Objects.requireNonNull(targetEncoding);
        if (AudioFloatConverter.getConverter(sourceFormat) == null)
            return new AudioFormat[0];
        int channels = sourceFormat.getChannels();

        ArrayList<AudioFormat> formats = new ArrayList<>();

        if (targetEncoding.equals(Encoding.PCM_SIGNED))
            formats.add(new AudioFormat(Encoding.PCM_SIGNED,
                    AudioSystem.NOT_SPECIFIED, 8, channels, channels,
                    AudioSystem.NOT_SPECIFIED, false));
        if (targetEncoding.equals(Encoding.PCM_UNSIGNED))
            formats.add(new AudioFormat(Encoding.PCM_UNSIGNED,
                    AudioSystem.NOT_SPECIFIED, 8, channels, channels,
                    AudioSystem.NOT_SPECIFIED, false));

        for (int bits = 16; bits < 32; bits += 8) {
            if (targetEncoding.equals(Encoding.PCM_SIGNED)) {
                formats.add(new AudioFormat(Encoding.PCM_SIGNED,
                        AudioSystem.NOT_SPECIFIED, bits, channels, channels
                                * bits / 8, AudioSystem.NOT_SPECIFIED, false));
                formats.add(new AudioFormat(Encoding.PCM_SIGNED,
                        AudioSystem.NOT_SPECIFIED, bits, channels, channels
                                * bits / 8, AudioSystem.NOT_SPECIFIED, true));
            }
            if (targetEncoding.equals(Encoding.PCM_UNSIGNED)) {
                formats.add(new AudioFormat(Encoding.PCM_UNSIGNED,
                        AudioSystem.NOT_SPECIFIED, bits, channels, channels
                                * bits / 8, AudioSystem.NOT_SPECIFIED, true));
                formats.add(new AudioFormat(Encoding.PCM_UNSIGNED,
                        AudioSystem.NOT_SPECIFIED, bits, channels, channels
                                * bits / 8, AudioSystem.NOT_SPECIFIED, false));
            }
        }

        if (targetEncoding.equals(Encoding.PCM_FLOAT)) {
            formats.add(new AudioFormat(Encoding.PCM_FLOAT,
                    AudioSystem.NOT_SPECIFIED, 32, channels, channels * 4,
                    AudioSystem.NOT_SPECIFIED, false));
            formats.add(new AudioFormat(Encoding.PCM_FLOAT,
                    AudioSystem.NOT_SPECIFIED, 32, channels, channels * 4,
                    AudioSystem.NOT_SPECIFIED, true));
            formats.add(new AudioFormat(Encoding.PCM_FLOAT,
                    AudioSystem.NOT_SPECIFIED, 64, channels, channels * 8,
                    AudioSystem.NOT_SPECIFIED, false));
            formats.add(new AudioFormat(Encoding.PCM_FLOAT,
                    AudioSystem.NOT_SPECIFIED, 64, channels, channels * 8,
                    AudioSystem.NOT_SPECIFIED, true));
        }

        return formats.toArray(new AudioFormat[formats.size()]);
    }

    @Override
    public boolean isConversionSupported(AudioFormat targetFormat,
                                         AudioFormat sourceFormat) {
        Objects.requireNonNull(targetFormat);
        if (AudioFloatConverter.getConverter(sourceFormat) == null)
            return false;
        if (AudioFloatConverter.getConverter(targetFormat) == null)
            return false;
        if (sourceFormat.getChannels() <= 0)
            return false;
        if (targetFormat.getChannels() <= 0)
            return false;
        return true;
    }

    @Override
    public boolean isConversionSupported(Encoding targetEncoding,
                                         AudioFormat sourceFormat) {
        Objects.requireNonNull(targetEncoding);
        if (AudioFloatConverter.getConverter(sourceFormat) == null)
            return false;
        for (int i = 0; i < formats.length; i++) {
            if (targetEncoding.equals(formats[i]))
                return true;
        }
        return false;
    }

}
