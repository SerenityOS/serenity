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
   @summary Test SoftChannel overflow test 2
   @modules java.desktop/com.sun.media.sound
*/

import java.util.HashMap;
import java.util.Map;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.Patch;
import javax.sound.midi.VoiceStatus;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import com.sun.media.sound.AudioSynthesizer;
import com.sun.media.sound.SF2Instrument;
import com.sun.media.sound.SF2InstrumentRegion;
import com.sun.media.sound.SF2Layer;
import com.sun.media.sound.SF2LayerRegion;
import com.sun.media.sound.SF2Region;
import com.sun.media.sound.SF2Sample;
import com.sun.media.sound.SF2Soundbank;
import com.sun.media.sound.SoftSynthesizer;

public class NoteOverFlowTest2 {

    public static void main(String[] args) throws Exception
    {
        // Create instance of the synthesizer with very low polyphony
        AudioSynthesizer synth = new SoftSynthesizer();
        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
        Map<String, Object> p = new HashMap<String, Object>();
        p.put("max polyphony", new Integer(5));
        AudioInputStream stream = synth.openStream(format, p);

        // Create instrument with too many regions (more than max polyphony)
        SF2Soundbank sf2 = new SF2Soundbank();

        SF2Sample sample = new SF2Sample(sf2);
        sample.setName("test sample");
        sample.setData(new byte[100]);
        sample.setSampleRate(44100);
        sample.setOriginalPitch(20);
        sf2.addResource(sample);

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName("test layer");
        sf2.addResource(layer);

        for (int i = 0; i < 100; i++) {
            SF2LayerRegion region = new SF2LayerRegion();
            region.setSample(sample);
            layer.getRegions().add(region);
        }

        SF2Instrument ins = new SF2Instrument(sf2);
        ins.setPatch(new Patch(0,0));
        ins.setName("test instrument");
        sf2.addInstrument(ins);

        SF2InstrumentRegion insregion = new SF2InstrumentRegion();
        insregion.setLayer(layer);
        ins.getRegions().add(insregion);

        // Load the test soundbank into the synthesizer
        synth.unloadAllInstruments(synth.getDefaultSoundbank());
        synth.loadAllInstruments(sf2);

        // Send out one midi on message
        MidiChannel ch1 = synth.getChannels()[0];
        ch1.programChange(0);
        ch1.noteOn(64, 64);

        // Read 1 sec from stream
        stream.skip(format.getFrameSize() * ((int)(format.getFrameRate() * 2)));

        // Close the synthesizer after use
        synth.close();
    }
}
