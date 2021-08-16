/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.Control.Type;

import com.sun.media.sound.AudioFloatConverter;

/**
 * This is a SourceDataLine simulator used for testing SoftSynthesizer
 * without using real SourceDataLine / Audio Device.
 *
 * @author Karl Helgason
 */

public class DummySourceDataLine implements SourceDataLine {

    private int bufferSize = -1;

    private AudioFormat format = new AudioFormat(44100.0f, 16, 2, true, false);

    private DataLine.Info sourceLineInfo;

    private boolean active = false;

    private long framepos = 0;

    private boolean opened = false;

    private int framesize = 0;

    public DummySourceDataLine()
    {
        ArrayList<AudioFormat> formats = new ArrayList<AudioFormat>();
        for (int channels = 1; channels <= 2; channels++) {
            formats.add(new AudioFormat(Encoding.PCM_SIGNED,
                    AudioSystem.NOT_SPECIFIED, 8, channels, channels,
                    AudioSystem.NOT_SPECIFIED, false));
            formats.add(new AudioFormat(Encoding.PCM_UNSIGNED,
                    AudioSystem.NOT_SPECIFIED, 8, channels, channels,
                    AudioSystem.NOT_SPECIFIED, false));
            for (int bits = 16; bits < 32; bits += 8) {
                formats.add(new AudioFormat(Encoding.PCM_SIGNED,
                        AudioSystem.NOT_SPECIFIED, bits, channels, channels
                                * bits / 8, AudioSystem.NOT_SPECIFIED, false));
                formats.add(new AudioFormat(Encoding.PCM_UNSIGNED,
                        AudioSystem.NOT_SPECIFIED, bits, channels, channels
                                * bits / 8, AudioSystem.NOT_SPECIFIED, false));
                formats.add(new AudioFormat(Encoding.PCM_SIGNED,
                        AudioSystem.NOT_SPECIFIED, bits, channels, channels
                                * bits / 8, AudioSystem.NOT_SPECIFIED, true));
                formats.add(new AudioFormat(Encoding.PCM_UNSIGNED,
                        AudioSystem.NOT_SPECIFIED, bits, channels, channels
                                * bits / 8, AudioSystem.NOT_SPECIFIED, true));
            }
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
        AudioFormat[] formats_array = formats.toArray(new AudioFormat[formats
                .size()]);
        sourceLineInfo = new DataLine.Info(SourceDataLine.class,
                formats_array, AudioSystem.NOT_SPECIFIED,
                AudioSystem.NOT_SPECIFIED);

    }

    public void open() throws LineUnavailableException {
        open(format);
    }

    public void open(AudioFormat format) throws LineUnavailableException {
        if (bufferSize == -1)
            bufferSize = ((int) (format.getFrameRate() / 2))
                    * format.getFrameSize();
        open(format, bufferSize);
    }

    public void open(AudioFormat format, int bufferSize)
            throws LineUnavailableException {
        this.format = format;
        this.bufferSize = bufferSize;
        this.framesize = format.getFrameSize();
        opened = true;
    }

    public boolean isOpen() {
        return opened;
    }

    public int write(byte[] b, int off, int len) {
        if (!isOpen())
            return 0;
        if (len % framesize != 0)
            throw new IllegalArgumentException(
                    "Number of bytes does not represent an integral number of sample frames.");


        int flen = len / framesize;
        framepos += flen;

        long time =  (long) (flen * (1000.0 / (double) getFormat()
                .getSampleRate()));
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return 0;
        }

        return len;
    }

    public int available() {
        return 0;
    }

    public void drain() {
    }

    public void flush() {
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public AudioFormat getFormat() {
        return format;
    }

    public int getFramePosition() {
        return (int) getLongFramePosition();
    }

    public float getLevel() {
        return AudioSystem.NOT_SPECIFIED;
    }

    public long getLongFramePosition() {
        return framepos;
    }

    public long getMicrosecondPosition() {
        return (long) (getLongFramePosition() * (1000000.0 / (double) getFormat()
                .getSampleRate()));
    }

    public boolean isActive() {
        return active;
    }

    public boolean isRunning() {
        return active;
    }

    public void start() {
        active = true;
    }

    public void stop() {
        active = false;
    }

    public void close() {
        stop();
    }

    public Control getControl(Type control) {
        throw new IllegalArgumentException("Unsupported control type : "
                + control);
    }

    public Control[] getControls() {
        return new Control[0];
    }

    public javax.sound.sampled.Line.Info getLineInfo() {
        return sourceLineInfo;
    }

    public boolean isControlSupported(Type control) {
        return false;
    }

    public void addLineListener(LineListener listener) {
    }

    public void removeLineListener(LineListener listener) {
    }

}
