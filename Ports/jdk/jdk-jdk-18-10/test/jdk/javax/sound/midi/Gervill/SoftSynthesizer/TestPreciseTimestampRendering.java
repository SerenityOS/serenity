/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
 @summary Test rendering when using precise timestamps
 @modules java.desktop/com.sun.media.sound
*/

import java.util.Arrays;
import java.util.Random;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import com.sun.media.sound.AudioFloatConverter;
import com.sun.media.sound.AudioSynthesizer;
import com.sun.media.sound.ModelAbstractChannelMixer;
import com.sun.media.sound.ModelChannelMixer;
import com.sun.media.sound.SF2Instrument;
import com.sun.media.sound.SF2InstrumentRegion;
import com.sun.media.sound.SF2Layer;
import com.sun.media.sound.SF2LayerRegion;
import com.sun.media.sound.SF2Sample;
import com.sun.media.sound.SF2Soundbank;
import com.sun.media.sound.SimpleInstrument;
import com.sun.media.sound.SimpleSoundbank;
import com.sun.media.sound.SoftSynthesizer;

public class TestPreciseTimestampRendering {

    public static AudioFormat format = new AudioFormat(44100, 16, 1, true,
            false);

    public static SF2Soundbank createTestSoundbank() {
        // Create impulse instrument
        // used to measure timing of note-on playback
        SF2Soundbank soundbank = new SF2Soundbank();
        float[] data = new float[100];
        Arrays.fill(data, 0);
        data[0] = 1.0f;
        byte[] bdata = new byte[data.length * format.getFrameSize()];
        AudioFloatConverter.getConverter(format).toByteArray(data, bdata);

        SF2Sample sample = new SF2Sample(soundbank);
        sample.setName("Test Sample");
        sample.setData(bdata);
        sample.setSampleRate((long) format.getSampleRate());
        sample.setOriginalPitch(69);
        soundbank.addResource(sample);

        SF2Layer layer = new SF2Layer(soundbank);
        layer.setName("Test Layer");
        soundbank.addResource(layer);
        SF2LayerRegion region = new SF2LayerRegion();
        region.setSample(sample);
        layer.getRegions().add(region);

        SF2Instrument ins = new SF2Instrument(soundbank);
        ins.setName("Test Instrument");
        soundbank.addInstrument(ins);
        SF2InstrumentRegion insregion = new SF2InstrumentRegion();
        insregion.setLayer(layer);
        ins.getRegions().add(insregion);

        return soundbank;
    }

    public static Soundbank createTestSoundbankWithChannelMixer() {
        SF2Soundbank soundbank = createTestSoundbank();

        SimpleSoundbank simplesoundbank = new SimpleSoundbank();
        SimpleInstrument simpleinstrument = new SimpleInstrument() {

            public ModelChannelMixer getChannelMixer(MidiChannel channel,
                    AudioFormat format) {
                return new ModelAbstractChannelMixer() {
                    boolean active = true;

                    public boolean process(float[][] buffer, int offset, int len) {
                        for (int i = 0; i < buffer.length; i++) {
                            float[] cbuffer = buffer[i];
                            for (int j = 0; j < cbuffer.length; j++) {
                                cbuffer[j] = -cbuffer[j];
                            }
                        }
                        return active;
                    }

                    public void stop() {
                        active = false;
                    }
                };
            }

        };
        simpleinstrument.add(soundbank.getInstruments()[0]);
        simplesoundbank.addInstrument(simpleinstrument);

        return simplesoundbank;
    }

    public static void main(String[] args) throws Exception {
        test(createTestSoundbank());
        test(createTestSoundbankWithChannelMixer());
    }

    public static void test(Soundbank soundbank) throws Exception {

        // Create instance of synthesizer using the testing soundbank above
        AudioSynthesizer synth = new SoftSynthesizer();
        AudioInputStream stream = synth.openStream(format, null);
        synth.unloadAllInstruments(synth.getDefaultSoundbank());
        synth.loadAllInstruments(soundbank);
        Receiver recv = synth.getReceiver();

        // Set volume to max and turn reverb off
        ShortMessage reverb_off = new ShortMessage();
        reverb_off.setMessage(ShortMessage.CONTROL_CHANGE, 91, 0);
        recv.send(reverb_off, -1);
        ShortMessage full_volume = new ShortMessage();
        full_volume.setMessage(ShortMessage.CONTROL_CHANGE, 7, 127);
        recv.send(full_volume, -1);

        Random random = new Random(3485934583945l);

        // Create random timestamps
        long[] test_timestamps = new long[30];
        for (int i = 1; i < test_timestamps.length; i++) {
            test_timestamps[i] = i * 44100
                    + (int) (random.nextDouble() * 22050.0);
        }

        // Send midi note on message to synthesizer
        for (int i = 0; i < test_timestamps.length; i++) {
            ShortMessage midi_on = new ShortMessage();
            midi_on.setMessage(ShortMessage.NOTE_ON, 69, 127);
            recv.send(midi_on,
                    (long) ((test_timestamps[i] / 44100.0) * 1000000.0));
        }

        // Measure timing from rendered audio
        float[] fbuffer = new float[100];
        byte[] buffer = new byte[fbuffer.length * format.getFrameSize()];
        long firsts = -1;
        int counter = 0;
        long s = 0;
        long max_jitter = 0;
        outerloop: for (int k = 0; k < 10000000; k++) {
            stream.read(buffer);
            AudioFloatConverter.getConverter(format).toFloatArray(buffer,
                    fbuffer);
            for (int i = 0; i < fbuffer.length; i++) {
                if (fbuffer[i] != 0) {
                    if (firsts == -1)
                        firsts = s;

                    long measure_time = (s - firsts);
                    long predicted_time = test_timestamps[counter];

                    long jitter = Math.abs(measure_time - predicted_time);

                    if (jitter > 10)
                        max_jitter = jitter;

                    counter++;
                    if (counter == test_timestamps.length)
                        break outerloop;
                }
                s++;
            }
        }
        synth.close();

        if (counter == 0)
            throw new Exception("Nothing was measured!");

        if (max_jitter != 0) {
            throw new Exception("Jitter has occurred! "
                    + "(max jitter = " + max_jitter + ")");
        }

    }

}
