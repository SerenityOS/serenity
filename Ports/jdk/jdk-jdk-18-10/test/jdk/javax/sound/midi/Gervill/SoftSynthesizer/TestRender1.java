/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 @summary Test SoftSynthesizer simple note rendering in many settings
 @modules java.desktop/com.sun.media.sound
*/

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.*;
import javax.sound.midi.*;

import com.sun.media.sound.*;

public class TestRender1 {

    public static double send(Sequence seq, Receiver recv) {
        float divtype = seq.getDivisionType();
        assert (seq.getDivisionType() == Sequence.PPQ);
        Track[] tracks = seq.getTracks();
        int[] trackspos = new int[tracks.length];
        int mpq = 60000000 / 100;
        int seqres = seq.getResolution();
        long lasttick = 0;
        long curtime = 0;
        while (true) {
            MidiEvent selevent = null;
            int seltrack = -1;
            for (int i = 0; i < tracks.length; i++) {
                int trackpos = trackspos[i];
                Track track = tracks[i];
                if (trackpos < track.size()) {
                    MidiEvent event = track.get(trackpos);
                    if (selevent == null
                            || event.getTick() < selevent.getTick()) {
                        selevent = event;
                        seltrack = i;
                    }
                }
            }
            if (seltrack == -1)
                break;
            trackspos[seltrack]++;
            long tick = selevent.getTick();
            if (divtype == Sequence.PPQ)
                curtime += ((tick - lasttick) * mpq) / seqres;
            else
                curtime = (long) ((tick * 1000000.0 * divtype) / seqres);
            lasttick = tick;
            MidiMessage msg = selevent.getMessage();
            if (msg instanceof MetaMessage) {
                if (divtype == Sequence.PPQ)
                    if (((MetaMessage) msg).getType() == 0x51) {
                        byte[] data = ((MetaMessage) msg).getData();
                        mpq = ((data[0] & 0xff) << 16)
                                | ((data[1] & 0xff) << 8) | (data[2] & 0xff);
                    }
            } else {
                if (recv != null)
                    recv.send(msg, curtime);
            }
        }

        return curtime / 1000000.0;
    }

    public static void test(AudioFormat format, Map<String, Object> info)
            throws Exception {
        OutputStream nullout = new OutputStream() {
            public void write(int b) throws IOException {
            }

            public void write(byte[] b, int off, int len) throws IOException {
            }

            public void write(byte[] b) throws IOException {
            }
        };
        render(nullout, format, info);
    }

    public static void render(OutputStream os, AudioFormat format,
            Map<String, Object> info) throws Exception {
        AudioSynthesizer synth = (AudioSynthesizer) new SoftSynthesizer();
        AudioInputStream stream = synth.openStream(format, info);
        Receiver recv = synth.getReceiver();
        Soundbank defsbk = synth.getDefaultSoundbank();
        if (defsbk != null)
            synth.unloadAllInstruments(defsbk);
        synth.loadAllInstruments(soundbank);

        double totalTime = 5;
        send(sequence, recv);

        long len = (long) (stream.getFormat().getFrameRate() * (totalTime + 4));
        stream = new AudioInputStream(stream, stream.getFormat(), len);

        long t = System.currentTimeMillis();
        AudioSystem.write(stream, AudioFileFormat.Type.WAVE, os);
        t = System.currentTimeMillis() - t;
        stream.close();
    }


    static Soundbank soundbank;

    static Sequence sequence;

    public static InputStream getInputStream(String filename) throws IOException
    {
        File file = new File(System.getProperty("test.src", "."), filename);
        FileInputStream fis = new FileInputStream(file);
        return new BufferedInputStream(fis);
    }

    public static void main(String[] args) throws Exception {

        InputStream sb = getInputStream("ding.sf2");
        soundbank = MidiSystem.getSoundbank(sb);
        sb.close();

        InputStream si = getInputStream("expresso.mid");
        sequence = MidiSystem.getSequence(si);
        si.close();

        AudioFormat format;
        Map<String, Object> info = new HashMap<String, Object>();
        {
            format = new AudioFormat(22050, 16, 2, true, false);
            test(format, info);
            format = new AudioFormat(44100, 16, 2, true, false);
            test(format, info);
        }
        {
            format = new AudioFormat(44100, 8, 2, true, false);
            test(format, info);
            format = new AudioFormat(44100, 16, 2, true, false);
            test(format, info);
            format = new AudioFormat(44100, 24, 2, true, false);
            test(format, info);
        }
        {
            format = new AudioFormat(44100, 16, 1, true, false);
            test(format, info);
            format = new AudioFormat(44100, 16, 2, true, false);
            test(format, info);
        }
        {
            format = new AudioFormat(44100, 16, 2, true, false);

            info.clear();
            info.put("control rate", 100f);
            test(format, info);
            info.clear();
            info.put("control rate", 147f);
            test(format, info);

        }
        {
            format = new AudioFormat(44100, 16, 2, true, false);

            info.clear();
            info.put("interpolation", "point");
            test(format, info);
            info.clear();
            info.put("interpolation", "linear");
            test(format, info);
            info.clear();
            info.put("interpolation", "cubic");
            test(format, info);
        }
        {
            format = new AudioFormat(44100, 16, 2, true, false);
            info.clear();
            info.put("max polyphony", 4);
            test(format, info);
            info.clear();
            info.put("max polyphony", 16);
            test(format, info);
            info.clear();

        }

    }
}
