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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Soundbank;
import javax.sound.midi.spi.SoundbankReader;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Soundbank reader that uses audio files as soundbanks.
 *
 * @author Karl Helgason
 */
public final class AudioFileSoundbankReader extends SoundbankReader {

    @Override
    public Soundbank getSoundbank(URL url)
            throws InvalidMidiDataException, IOException {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            Soundbank sbk = getSoundbank(ais);
            ais.close();
            return sbk;
        } catch (UnsupportedAudioFileException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Soundbank getSoundbank(InputStream stream)
            throws InvalidMidiDataException, IOException {
        stream.mark(512);
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(stream);
            Soundbank sbk = getSoundbank(ais);
            if (sbk != null)
                return sbk;
        } catch (UnsupportedAudioFileException e) {
        } catch (IOException e) {
        }
        stream.reset();
        return null;
    }

    public Soundbank getSoundbank(AudioInputStream ais)
            throws InvalidMidiDataException, IOException {
        try {
            byte[] buffer;
            if (ais.getFrameLength() == -1) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buff = new byte[1024
                        - (1024 % ais.getFormat().getFrameSize())];
                int ret;
                while ((ret = ais.read(buff)) != -1) {
                    baos.write(buff, 0, ret);
                }
                ais.close();
                buffer = baos.toByteArray();
            } else {
                buffer = new byte[(int) (ais.getFrameLength()
                                    * ais.getFormat().getFrameSize())];
                new DataInputStream(ais).readFully(buffer);
            }
            ModelByteBufferWavetable osc = new ModelByteBufferWavetable(
                    new ModelByteBuffer(buffer), ais.getFormat(), -4800);
            ModelPerformer performer = new ModelPerformer();
            performer.getOscillators().add(osc);

            SimpleSoundbank sbk = new SimpleSoundbank();
            SimpleInstrument ins = new SimpleInstrument();
            ins.add(performer);
            sbk.addInstrument(ins);
            return sbk;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Soundbank getSoundbank(File file)
            throws InvalidMidiDataException, IOException {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            ais.close();
            ModelByteBufferWavetable osc = new ModelByteBufferWavetable(
                    new ModelByteBuffer(file, 0, file.length()), -4800);
            ModelPerformer performer = new ModelPerformer();
            performer.getOscillators().add(osc);
            SimpleSoundbank sbk = new SimpleSoundbank();
            SimpleInstrument ins = new SimpleInstrument();
            ins.add(performer);
            sbk.addInstrument(ins);
            return sbk;
        } catch (UnsupportedAudioFileException e1) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}
