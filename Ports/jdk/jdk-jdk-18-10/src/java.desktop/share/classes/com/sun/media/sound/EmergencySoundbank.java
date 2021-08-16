/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Random;

import javax.sound.midi.Patch;
import javax.sound.sampled.AudioFormat;

/**
 * Emergency Soundbank generator.
 * Used when no other default soundbank can be found.
 *
 * @author Karl Helgason
 */
public final class EmergencySoundbank {

    private static final String[] general_midi_instruments = {
        "Acoustic Grand Piano",
        "Bright Acoustic Piano",
        "Electric Grand Piano",
        "Honky-tonk Piano",
        "Electric Piano 1",
        "Electric Piano 2",
        "Harpsichord",
        "Clavi",
        "Celesta",
        "Glockenspiel",
        "Music Box",
        "Vibraphone",
        "Marimba",
        "Xylophone",
        "Tubular Bells",
        "Dulcimer",
        "Drawbar Organ",
        "Percussive Organ",
        "Rock Organ",
        "Church Organ",
        "Reed Organ",
        "Accordion",
        "Harmonica",
        "Tango Accordion",
        "Acoustic Guitar (nylon)",
        "Acoustic Guitar (steel)",
        "Electric Guitar (jazz)",
        "Electric Guitar (clean)",
        "Electric Guitar (muted)",
        "Overdriven Guitar",
        "Distortion Guitar",
        "Guitar harmonics",
        "Acoustic Bass",
        "Electric Bass (finger)",
        "Electric Bass (pick)",
        "Fretless Bass",
        "Slap Bass 1",
        "Slap Bass 2",
        "Synth Bass 1",
        "Synth Bass 2",
        "Violin",
        "Viola",
        "Cello",
        "Contrabass",
        "Tremolo Strings",
        "Pizzicato Strings",
        "Orchestral Harp",
        "Timpani",
        "String Ensemble 1",
        "String Ensemble 2",
        "SynthStrings 1",
        "SynthStrings 2",
        "Choir Aahs",
        "Voice Oohs",
        "Synth Voice",
        "Orchestra Hit",
        "Trumpet",
        "Trombone",
        "Tuba",
        "Muted Trumpet",
        "French Horn",
        "Brass Section",
        "SynthBrass 1",
        "SynthBrass 2",
        "Soprano Sax",
        "Alto Sax",
        "Tenor Sax",
        "Baritone Sax",
        "Oboe",
        "English Horn",
        "Bassoon",
        "Clarinet",
        "Piccolo",
        "Flute",
        "Recorder",
        "Pan Flute",
        "Blown Bottle",
        "Shakuhachi",
        "Whistle",
        "Ocarina",
        "Lead 1 (square)",
        "Lead 2 (sawtooth)",
        "Lead 3 (calliope)",
        "Lead 4 (chiff)",
        "Lead 5 (charang)",
        "Lead 6 (voice)",
        "Lead 7 (fifths)",
        "Lead 8 (bass + lead)",
        "Pad 1 (new age)",
        "Pad 2 (warm)",
        "Pad 3 (polysynth)",
        "Pad 4 (choir)",
        "Pad 5 (bowed)",
        "Pad 6 (metallic)",
        "Pad 7 (halo)",
        "Pad 8 (sweep)",
        "FX 1 (rain)",
        "FX 2 (soundtrack)",
        "FX 3 (crystal)",
        "FX 4 (atmosphere)",
        "FX 5 (brightness)",
        "FX 6 (goblins)",
        "FX 7 (echoes)",
        "FX 8 (sci-fi)",
        "Sitar",
        "Banjo",
        "Shamisen",
        "Koto",
        "Kalimba",
        "Bag pipe",
        "Fiddle",
        "Shanai",
        "Tinkle Bell",
        "Agogo",
        "Steel Drums",
        "Woodblock",
        "Taiko Drum",
        "Melodic Tom",
        "Synth Drum",
        "Reverse Cymbal",
        "Guitar Fret Noise",
        "Breath Noise",
        "Seashore",
        "Bird Tweet",
        "Telephone Ring",
        "Helicopter",
        "Applause",
        "Gunshot"
    };

    public static SF2Soundbank createSoundbank() throws Exception {
        SF2Soundbank sf2 = new SF2Soundbank();
        sf2.setName("Emergency GM sound set");
        sf2.setVendor("Generated");
        sf2.setDescription("Emergency generated soundbank");

        /*
         *  percussion instruments
         */

        SF2Layer bass_drum = new_bass_drum(sf2);
        SF2Layer snare_drum = new_snare_drum(sf2);
        SF2Layer tom = new_tom(sf2);
        SF2Layer open_hihat = new_open_hihat(sf2);
        SF2Layer closed_hihat = new_closed_hihat(sf2);
        SF2Layer crash_cymbal = new_crash_cymbal(sf2);
        SF2Layer side_stick = new_side_stick(sf2);

        SF2Layer[] drums = new SF2Layer[128];
        drums[35] = bass_drum;
        drums[36] = bass_drum;
        drums[38] = snare_drum;
        drums[40] = snare_drum;
        drums[41] = tom;
        drums[43] = tom;
        drums[45] = tom;
        drums[47] = tom;
        drums[48] = tom;
        drums[50] = tom;
        drums[42] = closed_hihat;
        drums[44] = closed_hihat;
        drums[46] = open_hihat;
        drums[49] = crash_cymbal;
        drums[51] = crash_cymbal;
        drums[52] = crash_cymbal;
        drums[55] = crash_cymbal;
        drums[57] = crash_cymbal;
        drums[59] = crash_cymbal;

        // Use side_stick for missing drums:
        drums[37] = side_stick;
        drums[39] = side_stick;
        drums[53] = side_stick;
        drums[54] = side_stick;
        drums[56] = side_stick;
        drums[58] = side_stick;
        drums[69] = side_stick;
        drums[70] = side_stick;
        drums[75] = side_stick;
        drums[60] = side_stick;
        drums[61] = side_stick;
        drums[62] = side_stick;
        drums[63] = side_stick;
        drums[64] = side_stick;
        drums[65] = side_stick;
        drums[66] = side_stick;
        drums[67] = side_stick;
        drums[68] = side_stick;
        drums[71] = side_stick;
        drums[72] = side_stick;
        drums[73] = side_stick;
        drums[74] = side_stick;
        drums[76] = side_stick;
        drums[77] = side_stick;
        drums[78] = side_stick;
        drums[79] = side_stick;
        drums[80] = side_stick;
        drums[81] = side_stick;


        SF2Instrument drum_instrument = new SF2Instrument(sf2);
        drum_instrument.setName("Standard Kit");
        drum_instrument.setPatch(new ModelPatch(0, 0, true));
        sf2.addInstrument(drum_instrument);
        for (int i = 0; i < drums.length; i++) {
            if (drums[i] != null) {
                SF2InstrumentRegion region = new SF2InstrumentRegion();
                region.setLayer(drums[i]);
                region.putBytes(SF2InstrumentRegion.GENERATOR_KEYRANGE,
                        new byte[]{(byte) i, (byte) i});
                drum_instrument.getRegions().add(region);
            }
        }


        /*
         *  melodic instruments
         */

        SF2Layer gpiano = new_gpiano(sf2);
        SF2Layer gpiano2 = new_gpiano2(sf2);
        SF2Layer gpiano_hammer = new_piano_hammer(sf2);
        SF2Layer piano1 = new_piano1(sf2);
        SF2Layer epiano1 = new_epiano1(sf2);
        SF2Layer epiano2 = new_epiano2(sf2);

        SF2Layer guitar = new_guitar1(sf2);
        SF2Layer guitar_pick = new_guitar_pick(sf2);
        SF2Layer guitar_dist = new_guitar_dist(sf2);
        SF2Layer bass1 = new_bass1(sf2);
        SF2Layer bass2 = new_bass2(sf2);
        SF2Layer synthbass = new_synthbass(sf2);
        SF2Layer string2 = new_string2(sf2);
        SF2Layer orchhit = new_orchhit(sf2);
        SF2Layer choir = new_choir(sf2);
        SF2Layer solostring = new_solostring(sf2);
        SF2Layer organ = new_organ(sf2);
        SF2Layer ch_organ = new_ch_organ(sf2);
        SF2Layer bell = new_bell(sf2);
        SF2Layer flute = new_flute(sf2);

        SF2Layer timpani = new_timpani(sf2);
        SF2Layer melodic_toms = new_melodic_toms(sf2);
        SF2Layer trumpet = new_trumpet(sf2);
        SF2Layer trombone = new_trombone(sf2);
        SF2Layer brass_section = new_brass_section(sf2);
        SF2Layer horn = new_horn(sf2);
        SF2Layer sax = new_sax(sf2);
        SF2Layer oboe = new_oboe(sf2);
        SF2Layer bassoon = new_bassoon(sf2);
        SF2Layer clarinet = new_clarinet(sf2);
        SF2Layer reverse_cymbal = new_reverse_cymbal(sf2);

        SF2Layer defaultsound = piano1;

        newInstrument(sf2, "Piano", new Patch(0, 0), gpiano, gpiano_hammer);
        newInstrument(sf2, "Piano", new Patch(0, 1), gpiano2, gpiano_hammer);
        newInstrument(sf2, "Piano", new Patch(0, 2), piano1);
        {
            SF2Instrument ins = newInstrument(sf2, "Honky-tonk Piano",
                    new Patch(0, 3), piano1, piano1);
            SF2InstrumentRegion region = ins.getRegions().get(0);
            region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 80);
            region.putInteger(SF2Region.GENERATOR_FINETUNE, 30);
            region = ins.getRegions().get(1);
            region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 30);
        }
        newInstrument(sf2, "Rhodes", new Patch(0, 4), epiano2);
        newInstrument(sf2, "Rhodes", new Patch(0, 5), epiano2);
        newInstrument(sf2, "Clavinet", new Patch(0, 6), epiano1);
        newInstrument(sf2, "Clavinet", new Patch(0, 7), epiano1);
        newInstrument(sf2, "Rhodes", new Patch(0, 8), epiano2);
        newInstrument(sf2, "Bell", new Patch(0, 9), bell);
        newInstrument(sf2, "Bell", new Patch(0, 10), bell);
        newInstrument(sf2, "Vibraphone", new Patch(0, 11), bell);
        newInstrument(sf2, "Marimba", new Patch(0, 12), bell);
        newInstrument(sf2, "Marimba", new Patch(0, 13), bell);
        newInstrument(sf2, "Bell", new Patch(0, 14), bell);
        newInstrument(sf2, "Rock Organ", new Patch(0, 15), organ);
        newInstrument(sf2, "Rock Organ", new Patch(0, 16), organ);
        newInstrument(sf2, "Perc Organ", new Patch(0, 17), organ);
        newInstrument(sf2, "Rock Organ", new Patch(0, 18), organ);
        newInstrument(sf2, "Church Organ", new Patch(0, 19), ch_organ);
        newInstrument(sf2, "Accordion", new Patch(0, 20), organ);
        newInstrument(sf2, "Accordion", new Patch(0, 21), organ);
        newInstrument(sf2, "Accordion", new Patch(0, 22), organ);
        newInstrument(sf2, "Accordion", new Patch(0, 23), organ);
        newInstrument(sf2, "Guitar", new Patch(0, 24), guitar, guitar_pick);
        newInstrument(sf2, "Guitar", new Patch(0, 25), guitar, guitar_pick);
        newInstrument(sf2, "Guitar", new Patch(0, 26), guitar, guitar_pick);
        newInstrument(sf2, "Guitar", new Patch(0, 27), guitar, guitar_pick);
        newInstrument(sf2, "Guitar", new Patch(0, 28), guitar, guitar_pick);
        newInstrument(sf2, "Distorted Guitar", new Patch(0, 29), guitar_dist);
        newInstrument(sf2, "Distorted Guitar", new Patch(0, 30), guitar_dist);
        newInstrument(sf2, "Guitar", new Patch(0, 31), guitar, guitar_pick);
        newInstrument(sf2, "Finger Bass", new Patch(0, 32), bass1);
        newInstrument(sf2, "Finger Bass", new Patch(0, 33), bass1);
        newInstrument(sf2, "Finger Bass", new Patch(0, 34), bass1);
        newInstrument(sf2, "Frettless Bass", new Patch(0, 35), bass2);
        newInstrument(sf2, "Frettless Bass", new Patch(0, 36), bass2);
        newInstrument(sf2, "Frettless Bass", new Patch(0, 37), bass2);
        newInstrument(sf2, "Synth Bass1", new Patch(0, 38), synthbass);
        newInstrument(sf2, "Synth Bass2", new Patch(0, 39), synthbass);
        newInstrument(sf2, "Solo String", new Patch(0, 40), string2, solostring);
        newInstrument(sf2, "Solo String", new Patch(0, 41), string2, solostring);
        newInstrument(sf2, "Solo String", new Patch(0, 42), string2, solostring);
        newInstrument(sf2, "Solo String", new Patch(0, 43), string2, solostring);
        newInstrument(sf2, "Solo String", new Patch(0, 44), string2, solostring);
        newInstrument(sf2, "Def", new Patch(0, 45), defaultsound);
        newInstrument(sf2, "Harp", new Patch(0, 46), bell);
        newInstrument(sf2, "Timpani", new Patch(0, 47), timpani);
        newInstrument(sf2, "Strings", new Patch(0, 48), string2);
        SF2Instrument slow_strings =
                newInstrument(sf2, "Slow Strings", new Patch(0, 49), string2);
        SF2InstrumentRegion region = slow_strings.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, 2500);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 2000);
        newInstrument(sf2, "Synth Strings", new Patch(0, 50), string2);
        newInstrument(sf2, "Synth Strings", new Patch(0, 51), string2);


        newInstrument(sf2, "Choir", new Patch(0, 52), choir);
        newInstrument(sf2, "Choir", new Patch(0, 53), choir);
        newInstrument(sf2, "Choir", new Patch(0, 54), choir);
        {
            SF2Instrument ins = newInstrument(sf2, "Orch Hit",
                    new Patch(0, 55), orchhit, orchhit, timpani);
            region = ins.getRegions().get(0);
            region.putInteger(SF2Region.GENERATOR_COARSETUNE, -12);
            region.putInteger(SF2Region.GENERATOR_INITIALATTENUATION, -100);
        }
        newInstrument(sf2, "Trumpet", new Patch(0, 56), trumpet);
        newInstrument(sf2, "Trombone", new Patch(0, 57), trombone);
        newInstrument(sf2, "Trombone", new Patch(0, 58), trombone);
        newInstrument(sf2, "Trumpet", new Patch(0, 59), trumpet);
        newInstrument(sf2, "Horn", new Patch(0, 60), horn);
        newInstrument(sf2, "Brass Section", new Patch(0, 61), brass_section);
        newInstrument(sf2, "Brass Section", new Patch(0, 62), brass_section);
        newInstrument(sf2, "Brass Section", new Patch(0, 63), brass_section);
        newInstrument(sf2, "Sax", new Patch(0, 64), sax);
        newInstrument(sf2, "Sax", new Patch(0, 65), sax);
        newInstrument(sf2, "Sax", new Patch(0, 66), sax);
        newInstrument(sf2, "Sax", new Patch(0, 67), sax);
        newInstrument(sf2, "Oboe", new Patch(0, 68), oboe);
        newInstrument(sf2, "Horn", new Patch(0, 69), horn);
        newInstrument(sf2, "Bassoon", new Patch(0, 70), bassoon);
        newInstrument(sf2, "Clarinet", new Patch(0, 71), clarinet);
        newInstrument(sf2, "Flute", new Patch(0, 72), flute);
        newInstrument(sf2, "Flute", new Patch(0, 73), flute);
        newInstrument(sf2, "Flute", new Patch(0, 74), flute);
        newInstrument(sf2, "Flute", new Patch(0, 75), flute);
        newInstrument(sf2, "Flute", new Patch(0, 76), flute);
        newInstrument(sf2, "Flute", new Patch(0, 77), flute);
        newInstrument(sf2, "Flute", new Patch(0, 78), flute);
        newInstrument(sf2, "Flute", new Patch(0, 79), flute);
        newInstrument(sf2, "Organ", new Patch(0, 80), organ);
        newInstrument(sf2, "Organ", new Patch(0, 81), organ);
        newInstrument(sf2, "Flute", new Patch(0, 82), flute);
        newInstrument(sf2, "Organ", new Patch(0, 83), organ);
        newInstrument(sf2, "Organ", new Patch(0, 84), organ);
        newInstrument(sf2, "Choir", new Patch(0, 85), choir);
        newInstrument(sf2, "Organ", new Patch(0, 86), organ);
        newInstrument(sf2, "Organ", new Patch(0, 87), organ);
        newInstrument(sf2, "Synth Strings", new Patch(0, 88), string2);
        newInstrument(sf2, "Organ", new Patch(0, 89), organ);
        newInstrument(sf2, "Def", new Patch(0, 90), defaultsound);
        newInstrument(sf2, "Choir", new Patch(0, 91), choir);
        newInstrument(sf2, "Organ", new Patch(0, 92), organ);
        newInstrument(sf2, "Organ", new Patch(0, 93), organ);
        newInstrument(sf2, "Organ", new Patch(0, 94), organ);
        newInstrument(sf2, "Organ", new Patch(0, 95), organ);
        newInstrument(sf2, "Organ", new Patch(0, 96), organ);
        newInstrument(sf2, "Organ", new Patch(0, 97), organ);
        newInstrument(sf2, "Bell", new Patch(0, 98), bell);
        newInstrument(sf2, "Organ", new Patch(0, 99), organ);
        newInstrument(sf2, "Organ", new Patch(0, 100), organ);
        newInstrument(sf2, "Organ", new Patch(0, 101), organ);
        newInstrument(sf2, "Def", new Patch(0, 102), defaultsound);
        newInstrument(sf2, "Synth Strings", new Patch(0, 103), string2);
        newInstrument(sf2, "Def", new Patch(0, 104), defaultsound);
        newInstrument(sf2, "Def", new Patch(0, 105), defaultsound);
        newInstrument(sf2, "Def", new Patch(0, 106), defaultsound);
        newInstrument(sf2, "Def", new Patch(0, 107), defaultsound);
        newInstrument(sf2, "Marimba", new Patch(0, 108), bell);
        newInstrument(sf2, "Sax", new Patch(0, 109), sax);
        newInstrument(sf2, "Solo String", new Patch(0, 110), string2, solostring);
        newInstrument(sf2, "Oboe", new Patch(0, 111), oboe);
        newInstrument(sf2, "Bell", new Patch(0, 112), bell);
        newInstrument(sf2, "Melodic Toms", new Patch(0, 113), melodic_toms);
        newInstrument(sf2, "Marimba", new Patch(0, 114), bell);
        newInstrument(sf2, "Melodic Toms", new Patch(0, 115), melodic_toms);
        newInstrument(sf2, "Melodic Toms", new Patch(0, 116), melodic_toms);
        newInstrument(sf2, "Melodic Toms", new Patch(0, 117), melodic_toms);
        newInstrument(sf2, "Reverse Cymbal", new Patch(0, 118), reverse_cymbal);
        newInstrument(sf2, "Reverse Cymbal", new Patch(0, 119), reverse_cymbal);
        newInstrument(sf2, "Guitar", new Patch(0, 120), guitar);
        newInstrument(sf2, "Def", new Patch(0, 121), defaultsound);
        {
            SF2Instrument ins = newInstrument(sf2, "Seashore/Reverse Cymbal",
                    new Patch(0, 122), reverse_cymbal);
            region = ins.getRegions().get(0);
            region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);
            region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 18500);
            region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 4500);
            region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, -4500);
        }
        {
            SF2Instrument ins = newInstrument(sf2, "Bird/Flute",
                    new Patch(0, 123), flute);
            region = ins.getRegions().get(0);
            region.putInteger(SF2Region.GENERATOR_COARSETUNE, 24);
            region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, -3000);
            region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);
        }
        newInstrument(sf2, "Def", new Patch(0, 124), side_stick);
        {
            SF2Instrument ins = newInstrument(sf2, "Seashore/Reverse Cymbal",
                    new Patch(0, 125), reverse_cymbal);
            region = ins.getRegions().get(0);
            region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);
            region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 18500);
            region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 4500);
            region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, -4500);
        }
        newInstrument(sf2, "Applause/crash_cymbal",
                new Patch(0, 126), crash_cymbal);
        newInstrument(sf2, "Gunshot/side_stick", new Patch(0, 127), side_stick);

        for (SF2Instrument instrument : sf2.getInstruments()) {
            Patch patch = instrument.getPatch();
            if (patch instanceof ModelPatch) {
                if (((ModelPatch) patch).isPercussion())
                    continue;
            }
            instrument.setName(general_midi_instruments[patch.getProgram()]);
        }

        return sf2;

    }

    public static SF2Layer new_bell(SF2Soundbank sf2) {
        Random random = new Random(102030201);
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 25;
        double start_w = 0.01;
        double end_w = 0.05;
        double start_a = 0.2;
        double end_a = 0.00001;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 40.0);
        for (int i = 0; i < 40; i++) {
            double detune = 1 + (random.nextDouble() * 2 - 1) * 0.01;
            double w = start_w + (end_w - start_w) * (i / 40.0);
            complexGaussianDist(data, base * (i + 1) * detune, w, a);
            a *= a_step;
        }
        SF2Sample sample = newSimpleFFTSample(sf2, "EPiano", data, base);
        SF2Layer layer = newLayer(sf2, "EPiano", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -12000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 0);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);
        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, 1200);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, -9000);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 16000);
        return layer;
    }

    public static SF2Layer new_guitar1(SF2Soundbank sf2) {

        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 25;
        double start_w = 0.01;
        double end_w = 0.01;
        double start_a = 2;
        double end_a = 0.01;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 40.0);

        double[] aa = new double[40];
        for (int i = 0; i < 40; i++) {
            aa[i] = a;
            a *= a_step;
        }

        aa[0] = 2;
        aa[1] = 0.5;
        aa[2] = 0.45;
        aa[3] = 0.2;
        aa[4] = 1;
        aa[5] = 0.5;
        aa[6] = 2;
        aa[7] = 1;
        aa[8] = 0.5;
        aa[9] = 1;
        aa[9] = 0.5;
        aa[10] = 0.2;
        aa[11] = 1;
        aa[12] = 0.7;
        aa[13] = 0.5;
        aa[14] = 1;

        for (int i = 0; i < 40; i++) {
            double w = start_w + (end_w - start_w) * (i / 40.0);
            complexGaussianDist(data, base * (i + 1), w, aa[i]);
        }

        SF2Sample sample = newSimpleFFTSample(sf2, "Guitar", data, base);
        SF2Layer layer = newLayer(sf2, "Guitar", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -12000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 0);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 2400);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);

        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, -100);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, -6000);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 16000);
        region.putInteger(SF2Region.GENERATOR_INITIALATTENUATION, -20);
        return layer;
    }

    public static SF2Layer new_guitar_dist(SF2Soundbank sf2) {

        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 25;
        double start_w = 0.01;
        double end_w = 0.01;
        double start_a = 2;
        double end_a = 0.01;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 40.0);

        double[] aa = new double[40];
        for (int i = 0; i < 40; i++) {
            aa[i] = a;
            a *= a_step;
        }

        aa[0] = 5;
        aa[1] = 2;
        aa[2] = 0.45;
        aa[3] = 0.2;
        aa[4] = 1;
        aa[5] = 0.5;
        aa[6] = 2;
        aa[7] = 1;
        aa[8] = 0.5;
        aa[9] = 1;
        aa[9] = 0.5;
        aa[10] = 0.2;
        aa[11] = 1;
        aa[12] = 0.7;
        aa[13] = 0.5;
        aa[14] = 1;

        for (int i = 0; i < 40; i++) {
            double w = start_w + (end_w - start_w) * (i / 40.0);
            complexGaussianDist(data, base * (i + 1), w, aa[i]);
        }


        SF2Sample sample = newSimpleFFTSample_dist(sf2, "Distorted Guitar",
                data, base, 10000.0);


        SF2Layer layer = newLayer(sf2, "Distorted Guitar", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -12000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 0);
        //region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 2400);
        //region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 200);

        //region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, -100);
        //region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        //region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, -1000);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 8000);
        //region.putInteger(SF2Region.GENERATOR_INITIALATTENUATION, -20);
        return layer;
    }

    public static SF2Layer new_guitar_pick(SF2Soundbank sf2) {

        double[] datab;

        // Make treble part
        {
            int m = 2;
            int fftlen = 4096 * m;
            double[] data = new double[2 * fftlen];
            Random random = new Random(3049912);
            for (int i = 0; i < data.length; i += 2)
                data[i] = (2.0 * (random.nextDouble() - 0.5));
            fft(data);
            // Remove all negative frequency
            for (int i = fftlen / 2; i < data.length; i++)
                data[i] = 0;
            for (int i = 0; i < 2048 * m; i++) {
                data[i] *= Math.exp(-Math.abs((i - 23) / ((double) m)) * 1.2)
                        + Math.exp(-Math.abs((i - 40) / ((double) m)) * 0.9);
            }
            randomPhase(data, new Random(3049912));
            ifft(data);
            normalize(data, 0.8);
            data = realPart(data);
            double gain = 1.0;
            for (int i = 0; i < data.length; i++) {
                data[i] *= gain;
                gain *= 0.9994;
            }
            datab = data;

            fadeUp(data, 80);
        }

        SF2Sample sample = newSimpleDrumSample(sf2, "Guitar Noise", datab);

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName("Guitar Noise");

        SF2GlobalRegion global = new SF2GlobalRegion();
        layer.setGlobalZone(global);
        sf2.addResource(layer);

        SF2LayerRegion region = new SF2LayerRegion();
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 12000);
        //region.putInteger(SF2Region.GENERATOR_SCALETUNING, 0);
//        region.putInteger(SF2Region.GENERATOR_INITIALATTENUATION, -100);
/*
        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, 0);
        region.putInteger(SF2Region.GENERATOR_SUSTAINMODENV, 1000);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, -11000);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 12000);
         */

        region.setSample(sample);
        layer.getRegions().add(region);

        return layer;
    }

    public static SF2Layer new_gpiano(SF2Soundbank sf2) {
        //Random random = new Random(302030201);
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 25;
        double start_a = 0.2;
        double end_a = 0.001;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 15.0);

        double[] aa = new double[30];
        for (int i = 0; i < 30; i++) {
            aa[i] = a;
            a *= a_step;
        }

        aa[0] *= 2;
        //aa[2] *= 0.1;
        aa[4] *= 2;


        aa[12] *= 0.9;
        aa[13] *= 0.7;
        for (int i = 14; i < 30; i++) {
            aa[i] *= 0.5;
        }


        for (int i = 0; i < 30; i++) {
            //double detune = 1 + (random.nextDouble()*2 - 1)*0.0001;
            double w = 0.2;
            double ai = aa[i];
            if (i > 10) {
                w = 5;
                ai *= 10;
            }
            int adjust = 0;
            if (i > 5) {
                adjust = (i - 5) * 7;
            }
            complexGaussianDist(data, base * (i + 1) + adjust, w, ai);
        }

        SF2Sample sample = newSimpleFFTSample(sf2, "Grand Piano", data, base, 200);
        SF2Layer layer = newLayer(sf2, "Grand Piano", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -7000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 0);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);
        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, -6000);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, -5500);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 18000);
        return layer;
    }

    public static SF2Layer new_gpiano2(SF2Soundbank sf2) {
        //Random random = new Random(302030201);
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 25;
        double start_a = 0.2;
        double end_a = 0.001;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 20.0);

        double[] aa = new double[30];
        for (int i = 0; i < 30; i++) {
            aa[i] = a;
            a *= a_step;
        }

        aa[0] *= 1;
        //aa[2] *= 0.1;
        aa[4] *= 2;


        aa[12] *= 0.9;
        aa[13] *= 0.7;
        for (int i = 14; i < 30; i++) {
            aa[i] *= 0.5;
        }


        for (int i = 0; i < 30; i++) {
            //double detune = 1 + (random.nextDouble()*2 - 1)*0.0001;
            double w = 0.2;
            double ai = aa[i];
            if (i > 10) {
                w = 5;
                ai *= 10;
            }
            int adjust = 0;
            if (i > 5) {
                adjust = (i - 5) * 7;
            }
            complexGaussianDist(data, base * (i + 1) + adjust, w, ai);
        }

        SF2Sample sample = newSimpleFFTSample(sf2, "Grand Piano", data, base, 200);
        SF2Layer layer = newLayer(sf2, "Grand Piano", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -7000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 0);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);
        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, -6000);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, -5500);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 18000);
        return layer;
    }

    public static SF2Layer new_piano_hammer(SF2Soundbank sf2) {

        double[] datab;

        // Make treble part
        {
            int m = 2;
            int fftlen = 4096 * m;
            double[] data = new double[2 * fftlen];
            Random random = new Random(3049912);
            for (int i = 0; i < data.length; i += 2)
                data[i] = (2.0 * (random.nextDouble() - 0.5));
            fft(data);
            // Remove all negative frequency
            for (int i = fftlen / 2; i < data.length; i++)
                data[i] = 0;
            for (int i = 0; i < 2048 * m; i++)
                data[i] *= Math.exp(-Math.abs((i - 37) / ((double) m)) * 0.05);
            randomPhase(data, new Random(3049912));
            ifft(data);
            normalize(data, 0.6);
            data = realPart(data);
            double gain = 1.0;
            for (int i = 0; i < data.length; i++) {
                data[i] *= gain;
                gain *= 0.9997;
            }
            datab = data;

            fadeUp(data, 80);
        }

        SF2Sample sample = newSimpleDrumSample(sf2, "Piano Hammer", datab);

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName("Piano Hammer");

        SF2GlobalRegion global = new SF2GlobalRegion();
        layer.setGlobalZone(global);
        sf2.addResource(layer);

        SF2LayerRegion region = new SF2LayerRegion();
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 12000);
        //region.putInteger(SF2Region.GENERATOR_SCALETUNING, 0);
/*
        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, 0);
        region.putInteger(SF2Region.GENERATOR_SUSTAINMODENV, 1000);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, -11000);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 12000);
         */

        region.setSample(sample);
        layer.getRegions().add(region);

        return layer;
    }

    public static SF2Layer new_piano1(SF2Soundbank sf2) {
        //Random random = new Random(302030201);
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 25;
        double start_a = 0.2;
        double end_a = 0.0001;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 40.0);

        double[] aa = new double[30];
        for (int i = 0; i < 30; i++) {
            aa[i] = a;
            a *= a_step;
        }

        aa[0] *= 5;
        aa[2] *= 0.1;
        aa[7] *= 5;


        for (int i = 0; i < 30; i++) {
            //double detune = 1 + (random.nextDouble()*2 - 1)*0.0001;
            double w = 0.2;
            double ai = aa[i];
            if (i > 12) {
                w = 5;
                ai *= 10;
            }
            int adjust = 0;
            if (i > 5) {
                adjust = (i - 5) * 7;
            }
            complexGaussianDist(data, base * (i + 1) + adjust, w, ai);
        }

        complexGaussianDist(data, base * (15.5), 1, 0.1);
        complexGaussianDist(data, base * (17.5), 1, 0.01);

        SF2Sample sample = newSimpleFFTSample(sf2, "EPiano", data, base, 200);
        SF2Layer layer = newLayer(sf2, "EPiano", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -12000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 0);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);
        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, -1200);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, -5500);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 16000);
        return layer;
    }

    public static SF2Layer new_epiano1(SF2Soundbank sf2) {
        Random random = new Random(302030201);
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 25;
        double start_w = 0.05;
        double end_w = 0.05;
        double start_a = 0.2;
        double end_a = 0.0001;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 40.0);
        for (int i = 0; i < 40; i++) {
            double detune = 1 + (random.nextDouble() * 2 - 1) * 0.0001;
            double w = start_w + (end_w - start_w) * (i / 40.0);
            complexGaussianDist(data, base * (i + 1) * detune, w, a);
            a *= a_step;
        }



        SF2Sample sample = newSimpleFFTSample(sf2, "EPiano", data, base);
        SF2Layer layer = newLayer(sf2, "EPiano", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -12000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 0);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);
        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, 1200);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, -9000);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 16000);
        return layer;
    }

    public static SF2Layer new_epiano2(SF2Soundbank sf2) {
        Random random = new Random(302030201);
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 25;
        double start_w = 0.01;
        double end_w = 0.05;
        double start_a = 0.2;
        double end_a = 0.00001;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 40.0);
        for (int i = 0; i < 40; i++) {
            double detune = 1 + (random.nextDouble() * 2 - 1) * 0.0001;
            double w = start_w + (end_w - start_w) * (i / 40.0);
            complexGaussianDist(data, base * (i + 1) * detune, w, a);
            a *= a_step;
        }

        SF2Sample sample = newSimpleFFTSample(sf2, "EPiano", data, base);
        SF2Layer layer = newLayer(sf2, "EPiano", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -12000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 0);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 8000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);
        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, 2400);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, -9000);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 16000);
        region.putInteger(SF2Region.GENERATOR_INITIALATTENUATION, -100);
        return layer;
    }

    public static SF2Layer new_bass1(SF2Soundbank sf2) {
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 25;
        double start_w = 0.05;
        double end_w = 0.05;
        double start_a = 0.2;
        double end_a = 0.02;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 25.0);

        double[] aa = new double[25];
        for (int i = 0; i < 25; i++) {
            aa[i] = a;
            a *= a_step;
        }

        aa[0] *= 8;
        aa[1] *= 4;
        aa[3] *= 8;
        aa[5] *= 8;

        for (int i = 0; i < 25; i++) {
            double w = start_w + (end_w - start_w) * (i / 40.0);
            complexGaussianDist(data, base * (i + 1), w, aa[i]);
        }


        SF2Sample sample = newSimpleFFTSample(sf2, "Bass", data, base);
        SF2Layer layer = newLayer(sf2, "Bass", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -12000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 0);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);
        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, -3000);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, -5000);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 11000);
        region.putInteger(SF2Region.GENERATOR_INITIALATTENUATION, -100);
        return layer;
    }

    public static SF2Layer new_synthbass(SF2Soundbank sf2) {
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 25;
        double start_w = 0.05;
        double end_w = 0.05;
        double start_a = 0.2;
        double end_a = 0.02;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 25.0);

        double[] aa = new double[25];
        for (int i = 0; i < 25; i++) {
            aa[i] = a;
            a *= a_step;
        }

        aa[0] *= 16;
        aa[1] *= 4;
        aa[3] *= 16;
        aa[5] *= 8;

        for (int i = 0; i < 25; i++) {
            double w = start_w + (end_w - start_w) * (i / 40.0);
            complexGaussianDist(data, base * (i + 1), w, aa[i]);
        }


        SF2Sample sample = newSimpleFFTSample(sf2, "Bass", data, base);
        SF2Layer layer = newLayer(sf2, "Bass", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -12000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 0);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);
        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, -3000);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, -3000);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERQ, 100);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 8000);
        region.putInteger(SF2Region.GENERATOR_INITIALATTENUATION, -100);
        return layer;
    }

    public static SF2Layer new_bass2(SF2Soundbank sf2) {
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 25;
        double start_w = 0.05;
        double end_w = 0.05;
        double start_a = 0.2;
        double end_a = 0.002;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 25.0);

        double[] aa = new double[25];
        for (int i = 0; i < 25; i++) {
            aa[i] = a;
            a *= a_step;
        }

        aa[0] *= 8;
        aa[1] *= 4;
        aa[3] *= 8;
        aa[5] *= 8;

        for (int i = 0; i < 25; i++) {
            double w = start_w + (end_w - start_w) * (i / 40.0);
            complexGaussianDist(data, base * (i + 1), w, aa[i]);
        }


        SF2Sample sample = newSimpleFFTSample(sf2, "Bass2", data, base);
        SF2Layer layer = newLayer(sf2, "Bass2", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -8000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 0);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);
        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, -6000);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 5000);
        region.putInteger(SF2Region.GENERATOR_INITIALATTENUATION, -100);
        return layer;
    }

    public static SF2Layer new_solostring(SF2Soundbank sf2) {
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 25;
        double start_w = 2;
        double end_w = 2;
        double start_a = 0.2;
        double end_a = 0.01;

        double[] aa = new double[18];
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 40.0);
        for (int i = 0; i < aa.length; i++) {
            a *= a_step;
            aa[i] = a;
        }

        aa[0] *= 5;
        aa[1] *= 5;
        aa[2] *= 5;
        aa[3] *= 4;
        aa[4] *= 4;
        aa[5] *= 3;
        aa[6] *= 3;
        aa[7] *= 2;

        for (int i = 0; i < aa.length; i++) {
            double w = start_w + (end_w - start_w) * (i / 40.0);
            complexGaussianDist(data, base * (i + 1), w, a);
        }
        SF2Sample sample = newSimpleFFTSample(sf2, "Strings", data, base);
        SF2Layer layer = newLayer(sf2, "Strings", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -5000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 1000);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, -100);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 9500);
        region.putInteger(SF2Region.GENERATOR_FREQVIBLFO, -1000);
        region.putInteger(SF2Region.GENERATOR_VIBLFOTOPITCH, 15);
        return layer;

    }

    public static SF2Layer new_orchhit(SF2Soundbank sf2) {
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 25;
        double start_w = 2;
        double end_w = 80;
        double start_a = 0.2;
        double end_a = 0.001;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 40.0);
        for (int i = 0; i < 40; i++) {
            double w = start_w + (end_w - start_w) * (i / 40.0);
            complexGaussianDist(data, base * (i + 1), w, a);
            a *= a_step;
        }
        complexGaussianDist(data, base * 4, 300, 1);


        SF2Sample sample = newSimpleFFTSample(sf2, "Och Strings", data, base);
        SF2Layer layer = newLayer(sf2, "Och Strings", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -5000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 200);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 200);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 9500);
        return layer;

    }

    public static SF2Layer new_string2(SF2Soundbank sf2) {
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 25;
        double start_w = 2;
        double end_w = 80;
        double start_a = 0.2;
        double end_a = 0.001;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 40.0);
        for (int i = 0; i < 40; i++) {
            double w = start_w + (end_w - start_w) * (i / 40.0);
            complexGaussianDist(data, base * (i + 1), w, a);
            a *= a_step;
        }
        SF2Sample sample = newSimpleFFTSample(sf2, "Strings", data, base);
        SF2Layer layer = newLayer(sf2, "Strings", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -5000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 1000);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, -100);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 9500);
        return layer;

    }

    public static SF2Layer new_choir(SF2Soundbank sf2) {
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 25;
        double start_w = 2;
        double end_w = 80;
        double start_a = 0.2;
        double end_a = 0.001;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 40.0);
        double[] aa = new double[40];
        for (int i = 0; i < aa.length; i++) {
            a *= a_step;
            aa[i] = a;
        }

        aa[5] *= 0.1;
        aa[6] *= 0.01;
        aa[7] *= 0.1;
        aa[8] *= 0.1;

        for (int i = 0; i < aa.length; i++) {
            double w = start_w + (end_w - start_w) * (i / 40.0);
            complexGaussianDist(data, base * (i + 1), w, aa[i]);
        }
        SF2Sample sample = newSimpleFFTSample(sf2, "Strings", data, base);
        SF2Layer layer = newLayer(sf2, "Strings", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -5000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 1000);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, -100);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 9500);
        return layer;

    }

    public static SF2Layer new_organ(SF2Soundbank sf2) {
        Random random = new Random(102030201);
        int x = 1;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 15;
        double start_w = 0.01;
        double end_w = 0.01;
        double start_a = 0.2;
        double end_a = 0.001;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 40.0);

        for (int i = 0; i < 12; i++) {
            double w = start_w + (end_w - start_w) * (i / 40.0);
            complexGaussianDist(data, base * (i + 1), w,
                    a * (0.5 + 3 * (random.nextDouble())));
            a *= a_step;
        }
        SF2Sample sample = newSimpleFFTSample(sf2, "Organ", data, base);
        SF2Layer layer = newLayer(sf2, "Organ", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -6000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, -1000);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, -100);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 9500);
        return layer;

    }

    public static SF2Layer new_ch_organ(SF2Soundbank sf2) {
        int x = 1;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 15;
        double start_w = 0.01;
        double end_w = 0.01;
        double start_a = 0.2;
        double end_a = 0.001;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 60.0);

        double[] aa = new double[60];
        for (int i = 0; i < aa.length; i++) {
            a *= a_step;
            aa[i] = a;
        }

        aa[0] *= 5;
        aa[1] *= 2;
        aa[2] = 0;
        aa[4] = 0;
        aa[5] = 0;
        aa[7] *= 7;
        aa[9] = 0;
        aa[10] = 0;
        aa[12] = 0;
        aa[15] *= 7;
        aa[18] = 0;
        aa[20] = 0;
        aa[24] = 0;
        aa[27] *= 5;
        aa[29] = 0;
        aa[30] = 0;
        aa[33] = 0;
        aa[36] *= 4;
        aa[37] = 0;
        aa[39] = 0;
        aa[42] = 0;
        aa[43] = 0;
        aa[47] = 0;
        aa[50] *= 4;
        aa[52] = 0;
        aa[55] = 0;
        aa[57] = 0;


        aa[10] *= 0.1;
        aa[11] *= 0.1;
        aa[12] *= 0.1;
        aa[13] *= 0.1;

        aa[17] *= 0.1;
        aa[18] *= 0.1;
        aa[19] *= 0.1;
        aa[20] *= 0.1;

        for (int i = 0; i < 60; i++) {
            double w = start_w + (end_w - start_w) * (i / 40.0);
            complexGaussianDist(data, base * (i + 1), w, aa[i]);
            a *= a_step;
        }
        SF2Sample sample = newSimpleFFTSample(sf2, "Organ", data, base);
        SF2Layer layer = newLayer(sf2, "Organ", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -10000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, -1000);
        return layer;

    }

    public static SF2Layer new_flute(SF2Soundbank sf2) {
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 15;

        complexGaussianDist(data, base * 1, 0.001, 0.5);
        complexGaussianDist(data, base * 2, 0.001, 0.5);
        complexGaussianDist(data, base * 3, 0.001, 0.5);
        complexGaussianDist(data, base * 4, 0.01, 0.5);

        complexGaussianDist(data, base * 4, 100, 120);
        complexGaussianDist(data, base * 6, 100, 40);
        complexGaussianDist(data, base * 8, 100, 80);

        complexGaussianDist(data, base * 5, 0.001, 0.05);
        complexGaussianDist(data, base * 6, 0.001, 0.06);
        complexGaussianDist(data, base * 7, 0.001, 0.04);
        complexGaussianDist(data, base * 8, 0.005, 0.06);
        complexGaussianDist(data, base * 9, 0.005, 0.06);
        complexGaussianDist(data, base * 10, 0.01, 0.1);
        complexGaussianDist(data, base * 11, 0.08, 0.7);
        complexGaussianDist(data, base * 12, 0.08, 0.6);
        complexGaussianDist(data, base * 13, 0.08, 0.6);
        complexGaussianDist(data, base * 14, 0.08, 0.6);
        complexGaussianDist(data, base * 15, 0.08, 0.5);
        complexGaussianDist(data, base * 16, 0.08, 0.5);
        complexGaussianDist(data, base * 17, 0.08, 0.2);


        complexGaussianDist(data, base * 1, 10, 8);
        complexGaussianDist(data, base * 2, 10, 8);
        complexGaussianDist(data, base * 3, 10, 8);
        complexGaussianDist(data, base * 4, 10, 8);
        complexGaussianDist(data, base * 5, 10, 8);
        complexGaussianDist(data, base * 6, 20, 9);
        complexGaussianDist(data, base * 7, 20, 9);
        complexGaussianDist(data, base * 8, 20, 9);
        complexGaussianDist(data, base * 9, 20, 8);
        complexGaussianDist(data, base * 10, 30, 8);
        complexGaussianDist(data, base * 11, 30, 9);
        complexGaussianDist(data, base * 12, 30, 9);
        complexGaussianDist(data, base * 13, 30, 8);
        complexGaussianDist(data, base * 14, 30, 8);
        complexGaussianDist(data, base * 15, 30, 7);
        complexGaussianDist(data, base * 16, 30, 7);
        complexGaussianDist(data, base * 17, 30, 6);

        SF2Sample sample = newSimpleFFTSample(sf2, "Flute", data, base);
        SF2Layer layer = newLayer(sf2, "Flute", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -6000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, -1000);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, -100);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 9500);
        return layer;

    }

    public static SF2Layer new_horn(SF2Soundbank sf2) {
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 15;

        double start_a = 0.5;
        double end_a = 0.00000000001;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 40.0);
        for (int i = 0; i < 40; i++) {
            if (i == 0)
                complexGaussianDist(data, base * (i + 1), 0.1, a * 0.2);
            else
                complexGaussianDist(data, base * (i + 1), 0.1, a);
            a *= a_step;
        }

        complexGaussianDist(data, base * 2, 100, 1);


        SF2Sample sample = newSimpleFFTSample(sf2, "Horn", data, base);
        SF2Layer layer = newLayer(sf2, "Horn", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -6000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, -1000);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, -100);

        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, -500);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, 5000);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 4500);
        return layer;

    }

    public static SF2Layer new_trumpet(SF2Soundbank sf2) {
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 15;

        double start_a = 0.5;
        double end_a = 0.00001;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 80.0);
        double[] aa = new double[80];
        for (int i = 0; i < 80; i++) {
            aa[i] = a;
            a *= a_step;
        }

        aa[0] *= 0.05;
        aa[1] *= 0.2;
        aa[2] *= 0.5;
        aa[3] *= 0.85;

        for (int i = 0; i < 80; i++) {
            complexGaussianDist(data, base * (i + 1), 0.1, aa[i]);
        }

        complexGaussianDist(data, base * 5, 300, 3);


        SF2Sample sample = newSimpleFFTSample(sf2, "Trumpet", data, base);
        SF2Layer layer = newLayer(sf2, "Trumpet", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -10000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 0);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, -100);

        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, -4000);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, -2500);
        region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, 5000);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 4500);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERQ, 10);
        return layer;

    }

    public static SF2Layer new_brass_section(SF2Soundbank sf2) {
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 15;

        double start_a = 0.5;
        double end_a = 0.005;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 30.0);
        double[] aa = new double[30];
        for (int i = 0; i < 30; i++) {
            aa[i] = a;
            a *= a_step;
        }

        aa[0] *= 0.8;
        aa[1] *= 0.9;

        double w = 5;
        for (int i = 0; i < 30; i++) {
            complexGaussianDist(data, base * (i + 1), 0.1 * w, aa[i] * w);
            w += 6; //*= w_step;
        }

        complexGaussianDist(data, base * 6, 300, 2);


        SF2Sample sample = newSimpleFFTSample(sf2, "Brass Section", data, base);
        SF2Layer layer = newLayer(sf2, "Brass Section", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -9200);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, -1000);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, -100);

        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, -3000);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, 5000);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 4500);
        return layer;

    }

    public static SF2Layer new_trombone(SF2Soundbank sf2) {
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 15;

        double start_a = 0.5;
        double end_a = 0.001;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 80.0);
        double[] aa = new double[80];
        for (int i = 0; i < 80; i++) {
            aa[i] = a;
            a *= a_step;
        }

        aa[0] *= 0.3;
        aa[1] *= 0.7;

        for (int i = 0; i < 80; i++) {
            complexGaussianDist(data, base * (i + 1), 0.1, aa[i]);
        }

        complexGaussianDist(data, base * 6, 300, 2);


        SF2Sample sample = newSimpleFFTSample(sf2, "Trombone", data, base);
        SF2Layer layer = newLayer(sf2, "Trombone", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -8000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, -1000);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, -100);

        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, -2000);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, 5000);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 4500);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERQ, 10);
        return layer;

    }

    public static SF2Layer new_sax(SF2Soundbank sf2) {
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 15;

        double start_a = 0.5;
        double end_a = 0.01;
        double a = start_a;
        double a_step = Math.pow(end_a / start_a, 1.0 / 40.0);
        for (int i = 0; i < 40; i++) {
            if (i == 0 || i == 2)
                complexGaussianDist(data, base * (i + 1), 0.1, a * 4);
            else
                complexGaussianDist(data, base * (i + 1), 0.1, a);
            a *= a_step;
        }

        complexGaussianDist(data, base * 4, 200, 1);

        SF2Sample sample = newSimpleFFTSample(sf2, "Sax", data, base);
        SF2Layer layer = newLayer(sf2, "Sax", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -6000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, -1000);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, -100);

        region.putInteger(SF2Region.GENERATOR_ATTACKMODENV, -3000);
        region.putInteger(SF2Region.GENERATOR_RELEASEMODENV, 12000);
        region.putInteger(SF2Region.GENERATOR_MODENVTOFILTERFC, 5000);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 4500);
        return layer;

    }

    public static SF2Layer new_oboe(SF2Soundbank sf2) {
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 15;

        complexGaussianDist(data, base * 5, 100, 80);


        complexGaussianDist(data, base * 1, 0.01, 0.53);
        complexGaussianDist(data, base * 2, 0.01, 0.51);
        complexGaussianDist(data, base * 3, 0.01, 0.48);
        complexGaussianDist(data, base * 4, 0.01, 0.49);
        complexGaussianDist(data, base * 5, 0.01, 5);
        complexGaussianDist(data, base * 6, 0.01, 0.51);
        complexGaussianDist(data, base * 7, 0.01, 0.50);
        complexGaussianDist(data, base * 8, 0.01, 0.59);
        complexGaussianDist(data, base * 9, 0.01, 0.61);
        complexGaussianDist(data, base * 10, 0.01, 0.52);
        complexGaussianDist(data, base * 11, 0.01, 0.49);
        complexGaussianDist(data, base * 12, 0.01, 0.51);
        complexGaussianDist(data, base * 13, 0.01, 0.48);
        complexGaussianDist(data, base * 14, 0.01, 0.51);
        complexGaussianDist(data, base * 15, 0.01, 0.46);
        complexGaussianDist(data, base * 16, 0.01, 0.35);
        complexGaussianDist(data, base * 17, 0.01, 0.20);
        complexGaussianDist(data, base * 18, 0.01, 0.10);
        complexGaussianDist(data, base * 19, 0.01, 0.5);
        complexGaussianDist(data, base * 20, 0.01, 0.1);


        SF2Sample sample = newSimpleFFTSample(sf2, "Oboe", data, base);
        SF2Layer layer = newLayer(sf2, "Oboe", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -6000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, -1000);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, -100);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 9500);
        return layer;

    }

    public static SF2Layer new_bassoon(SF2Soundbank sf2) {
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 15;

        complexGaussianDist(data, base * 2, 100, 40);
        complexGaussianDist(data, base * 4, 100, 20);

        complexGaussianDist(data, base * 1, 0.01, 0.53);
        complexGaussianDist(data, base * 2, 0.01, 5);
        complexGaussianDist(data, base * 3, 0.01, 0.51);
        complexGaussianDist(data, base * 4, 0.01, 0.48);
        complexGaussianDist(data, base * 5, 0.01, 1.49);
        complexGaussianDist(data, base * 6, 0.01, 0.51);
        complexGaussianDist(data, base * 7, 0.01, 0.50);
        complexGaussianDist(data, base * 8, 0.01, 0.59);
        complexGaussianDist(data, base * 9, 0.01, 0.61);
        complexGaussianDist(data, base * 10, 0.01, 0.52);
        complexGaussianDist(data, base * 11, 0.01, 0.49);
        complexGaussianDist(data, base * 12, 0.01, 0.51);
        complexGaussianDist(data, base * 13, 0.01, 0.48);
        complexGaussianDist(data, base * 14, 0.01, 0.51);
        complexGaussianDist(data, base * 15, 0.01, 0.46);
        complexGaussianDist(data, base * 16, 0.01, 0.35);
        complexGaussianDist(data, base * 17, 0.01, 0.20);
        complexGaussianDist(data, base * 18, 0.01, 0.10);
        complexGaussianDist(data, base * 19, 0.01, 0.5);
        complexGaussianDist(data, base * 20, 0.01, 0.1);


        SF2Sample sample = newSimpleFFTSample(sf2, "Flute", data, base);
        SF2Layer layer = newLayer(sf2, "Flute", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -6000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, -1000);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, -100);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 9500);
        return layer;

    }

    public static SF2Layer new_clarinet(SF2Soundbank sf2) {
        int x = 8;
        int fftsize = 4096 * x;
        double[] data = new double[fftsize * 2];
        double base = x * 15;

        complexGaussianDist(data, base * 1, 0.001, 0.5);
        complexGaussianDist(data, base * 2, 0.001, 0.02);
        complexGaussianDist(data, base * 3, 0.001, 0.2);
        complexGaussianDist(data, base * 4, 0.01, 0.1);

        complexGaussianDist(data, base * 4, 100, 60);
        complexGaussianDist(data, base * 6, 100, 20);
        complexGaussianDist(data, base * 8, 100, 20);

        complexGaussianDist(data, base * 5, 0.001, 0.1);
        complexGaussianDist(data, base * 6, 0.001, 0.09);
        complexGaussianDist(data, base * 7, 0.001, 0.02);
        complexGaussianDist(data, base * 8, 0.005, 0.16);
        complexGaussianDist(data, base * 9, 0.005, 0.96);
        complexGaussianDist(data, base * 10, 0.01, 0.9);
        complexGaussianDist(data, base * 11, 0.08, 1.2);
        complexGaussianDist(data, base * 12, 0.08, 1.8);
        complexGaussianDist(data, base * 13, 0.08, 1.6);
        complexGaussianDist(data, base * 14, 0.08, 1.2);
        complexGaussianDist(data, base * 15, 0.08, 0.9);
        complexGaussianDist(data, base * 16, 0.08, 0.5);
        complexGaussianDist(data, base * 17, 0.08, 0.2);


        complexGaussianDist(data, base * 1, 10, 8);
        complexGaussianDist(data, base * 2, 10, 8);
        complexGaussianDist(data, base * 3, 10, 8);
        complexGaussianDist(data, base * 4, 10, 8);
        complexGaussianDist(data, base * 5, 10, 8);
        complexGaussianDist(data, base * 6, 20, 9);
        complexGaussianDist(data, base * 7, 20, 9);
        complexGaussianDist(data, base * 8, 20, 9);
        complexGaussianDist(data, base * 9, 20, 8);
        complexGaussianDist(data, base * 10, 30, 8);
        complexGaussianDist(data, base * 11, 30, 9);
        complexGaussianDist(data, base * 12, 30, 9);
        complexGaussianDist(data, base * 13, 30, 8);
        complexGaussianDist(data, base * 14, 30, 8);
        complexGaussianDist(data, base * 15, 30, 7);
        complexGaussianDist(data, base * 16, 30, 7);
        complexGaussianDist(data, base * 17, 30, 6);

        SF2Sample sample = newSimpleFFTSample(sf2, "Clarinet", data, base);
        SF2Layer layer = newLayer(sf2, "Clarinet", sample);
        SF2Region region = layer.getRegions().get(0);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -6000);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, -1000);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 4000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, -100);
        region.putInteger(SF2Region.GENERATOR_INITIALFILTERFC, 9500);
        return layer;

    }

    public static SF2Layer new_timpani(SF2Soundbank sf2) {

        double[] datab;
        double[] datah;

        // Make Bass Part
        {
            int fftlen = 4096 * 8;
            double[] data = new double[2 * fftlen];
            double base = 48;
            complexGaussianDist(data, base * 2, 0.2, 1);
            complexGaussianDist(data, base * 3, 0.2, 0.7);
            complexGaussianDist(data, base * 5, 10, 1);
            complexGaussianDist(data, base * 6, 9, 1);
            complexGaussianDist(data, base * 8, 15, 1);
            complexGaussianDist(data, base * 9, 18, 0.8);
            complexGaussianDist(data, base * 11, 21, 0.5);
            complexGaussianDist(data, base * 13, 28, 0.3);
            complexGaussianDist(data, base * 14, 22, 0.1);
            randomPhase(data, new Random(3049912));
            ifft(data);
            normalize(data, 0.5);
            data = realPart(data);

            double d_len = data.length;
            for (int i = 0; i < data.length; i++) {
                double g = (1.0 - (i / d_len));
                data[i] *= g * g;
            }
            fadeUp(data, 40);
            datab = data;
        }

        // Make treble part
        {
            int fftlen = 4096 * 4;
            double[] data = new double[2 * fftlen];
            Random random = new Random(3049912);
            for (int i = 0; i < data.length; i += 2) {
                data[i] = (2.0 * (random.nextDouble() - 0.5)) * 0.1;
            }
            fft(data);
            // Remove all negative frequency
            for (int i = fftlen / 2; i < data.length; i++)
                data[i] = 0;
            for (int i = 1024 * 4; i < 2048 * 4; i++)
                data[i] = 1.0 - (i - 4096) / 4096.0;
            for (int i = 0; i < 300; i++) {
                double g = (1.0 - (i / 300.0));
                data[i] *= 1.0 + 20 * g * g;
            }
            for (int i = 0; i < 24; i++)
                data[i] = 0;
            randomPhase(data, new Random(3049912));
            ifft(data);
            normalize(data, 0.9);
            data = realPart(data);
            double gain = 1.0;
            for (int i = 0; i < data.length; i++) {
                data[i] *= gain;
                gain *= 0.9998;
            }
            datah = data;
        }

        for (int i = 0; i < datah.length; i++)
            datab[i] += datah[i] * 0.02;

        normalize(datab, 0.9);

        SF2Sample sample = newSimpleDrumSample(sf2, "Timpani", datab);

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName("Timpani");

        SF2GlobalRegion global = new SF2GlobalRegion();
        layer.setGlobalZone(global);
        sf2.addResource(layer);

        SF2LayerRegion region = new SF2LayerRegion();
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 12000);
        region.putInteger(SF2Region.GENERATOR_INITIALATTENUATION, -100);
        region.setSample(sample);
        layer.getRegions().add(region);

        return layer;
    }

    public static SF2Layer new_melodic_toms(SF2Soundbank sf2) {

        double[] datab;
        double[] datah;

        // Make Bass Part
        {
            int fftlen = 4096 * 4;
            double[] data = new double[2 * fftlen];
            complexGaussianDist(data, 30, 0.5, 1);
            randomPhase(data, new Random(3049912));
            ifft(data);
            normalize(data, 0.8);
            data = realPart(data);

            double d_len = data.length;
            for (int i = 0; i < data.length; i++)
                data[i] *= (1.0 - (i / d_len));
            datab = data;
        }

        // Make treble part
        {
            int fftlen = 4096 * 4;
            double[] data = new double[2 * fftlen];
            Random random = new Random(3049912);
            for (int i = 0; i < data.length; i += 2)
                data[i] = (2.0 * (random.nextDouble() - 0.5)) * 0.1;
            fft(data);
            // Remove all negative frequency
            for (int i = fftlen / 2; i < data.length; i++)
                data[i] = 0;
            for (int i = 1024 * 4; i < 2048 * 4; i++)
                data[i] = 1.0 - (i - 4096) / 4096.0;
            for (int i = 0; i < 200; i++) {
                double g = (1.0 - (i / 200.0));
                data[i] *= 1.0 + 20 * g * g;
            }
            for (int i = 0; i < 30; i++)
                data[i] = 0;
            randomPhase(data, new Random(3049912));
            ifft(data);
            normalize(data, 0.9);
            data = realPart(data);
            double gain = 1.0;
            for (int i = 0; i < data.length; i++) {
                data[i] *= gain;
                gain *= 0.9996;
            }
            datah = data;
        }

        for (int i = 0; i < datah.length; i++)
            datab[i] += datah[i] * 0.5;
        for (int i = 0; i < 5; i++)
            datab[i] *= i / 5.0;

        normalize(datab, 0.99);

        SF2Sample sample = newSimpleDrumSample(sf2, "Melodic Toms", datab);
        sample.setOriginalPitch(63);

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName("Melodic Toms");

        SF2GlobalRegion global = new SF2GlobalRegion();
        layer.setGlobalZone(global);
        sf2.addResource(layer);

        SF2LayerRegion region = new SF2LayerRegion();
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 12000);
        //region.putInteger(SF2Region.GENERATOR_SCALETUNING, 0);
        region.putInteger(SF2Region.GENERATOR_INITIALATTENUATION, -100);
        region.setSample(sample);
        layer.getRegions().add(region);

        return layer;
    }

    public static SF2Layer new_reverse_cymbal(SF2Soundbank sf2) {
        double[] datah;
        {
            int fftlen = 4096 * 4;
            double[] data = new double[2 * fftlen];
            Random random = new Random(3049912);
            for (int i = 0; i < data.length; i += 2)
                data[i] = (2.0 * (random.nextDouble() - 0.5));
            for (int i = fftlen / 2; i < data.length; i++)
                data[i] = 0;
            for (int i = 0; i < 100; i++)
                data[i] = 0;

            for (int i = 0; i < 512 * 2; i++) {
                double gain = (i / (512.0 * 2.0));
                data[i] = 1 - gain;
            }
            datah = data;
        }

        SF2Sample sample = newSimpleFFTSample(sf2, "Reverse Cymbal",
                datah, 100, 20);

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName("Reverse Cymbal");

        SF2GlobalRegion global = new SF2GlobalRegion();
        layer.setGlobalZone(global);
        sf2.addResource(layer);

        SF2LayerRegion region = new SF2LayerRegion();
        region.putInteger(SF2Region.GENERATOR_ATTACKVOLENV, -200);
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, -12000);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, -1000);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);
        region.setSample(sample);
        layer.getRegions().add(region);

        return layer;
    }

    public static SF2Layer new_snare_drum(SF2Soundbank sf2) {

        double[] datab;
        double[] datah;

        // Make Bass Part
        {
            int fftlen = 4096 * 4;
            double[] data = new double[2 * fftlen];
            complexGaussianDist(data, 24, 0.5, 1);
            randomPhase(data, new Random(3049912));
            ifft(data);
            normalize(data, 0.5);
            data = realPart(data);

            double d_len = data.length;
            for (int i = 0; i < data.length; i++)
                data[i] *= (1.0 - (i / d_len));
            datab = data;
        }

        // Make treble part
        {
            int fftlen = 4096 * 4;
            double[] data = new double[2 * fftlen];
            Random random = new Random(3049912);
            for (int i = 0; i < data.length; i += 2)
                data[i] = (2.0 * (random.nextDouble() - 0.5)) * 0.1;
            fft(data);
            // Remove all negative frequency
            for (int i = fftlen / 2; i < data.length; i++)
                data[i] = 0;
            for (int i = 1024 * 4; i < 2048 * 4; i++)
                data[i] = 1.0 - (i - 4096) / 4096.0;
            for (int i = 0; i < 300; i++) {
                double g = (1.0 - (i / 300.0));
                data[i] *= 1.0 + 20 * g * g;
            }
            for (int i = 0; i < 24; i++)
                data[i] = 0;
            randomPhase(data, new Random(3049912));
            ifft(data);
            normalize(data, 0.9);
            data = realPart(data);
            double gain = 1.0;
            for (int i = 0; i < data.length; i++) {
                data[i] *= gain;
                gain *= 0.9998;
            }
            datah = data;
        }

        for (int i = 0; i < datah.length; i++)
            datab[i] += datah[i];
        for (int i = 0; i < 5; i++)
            datab[i] *= i / 5.0;

        SF2Sample sample = newSimpleDrumSample(sf2, "Snare Drum", datab);

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName("Snare Drum");

        SF2GlobalRegion global = new SF2GlobalRegion();
        layer.setGlobalZone(global);
        sf2.addResource(layer);

        SF2LayerRegion region = new SF2LayerRegion();
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 12000);
        region.putInteger(SF2Region.GENERATOR_SCALETUNING, 0);
        region.putInteger(SF2Region.GENERATOR_INITIALATTENUATION, -100);
        region.setSample(sample);
        layer.getRegions().add(region);

        return layer;
    }

    public static SF2Layer new_bass_drum(SF2Soundbank sf2) {

        double[] datab;
        double[] datah;

        // Make Bass Part
        {
            int fftlen = 4096 * 4;
            double[] data = new double[2 * fftlen];
            complexGaussianDist(data, 1.8 * 5 + 1, 2, 1);
            complexGaussianDist(data, 1.8 * 9 + 1, 2, 1);
            randomPhase(data, new Random(3049912));
            ifft(data);
            normalize(data, 0.9);
            data = realPart(data);
            double d_len = data.length;
            for (int i = 0; i < data.length; i++)
                data[i] *= (1.0 - (i / d_len));
            datab = data;
        }

        // Make treble part
        {
            int fftlen = 4096;
            double[] data = new double[2 * fftlen];
            Random random = new Random(3049912);
            for (int i = 0; i < data.length; i += 2)
                data[i] = (2.0 * (random.nextDouble() - 0.5)) * 0.1;
            fft(data);
            // Remove all negative frequency
            for (int i = fftlen / 2; i < data.length; i++)
                data[i] = 0;
            for (int i = 1024; i < 2048; i++)
                data[i] = 1.0 - (i - 1024) / 1024.0;
            for (int i = 0; i < 512; i++)
                data[i] = 10 * i / 512.0;
            for (int i = 0; i < 10; i++)
                data[i] = 0;
            randomPhase(data, new Random(3049912));
            ifft(data);
            normalize(data, 0.9);
            data = realPart(data);
            double gain = 1.0;
            for (int i = 0; i < data.length; i++) {
                data[i] *= gain;
                gain *= 0.999;
            }
            datah = data;
        }

        for (int i = 0; i < datah.length; i++)
            datab[i] += datah[i] * 0.5;
        for (int i = 0; i < 5; i++)
            datab[i] *= i / 5.0;

        SF2Sample sample = newSimpleDrumSample(sf2, "Bass Drum", datab);

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName("Bass Drum");

        SF2GlobalRegion global = new SF2GlobalRegion();
        layer.setGlobalZone(global);
        sf2.addResource(layer);

        SF2LayerRegion region = new SF2LayerRegion();
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 12000);
        region.putInteger(SF2Region.GENERATOR_SCALETUNING, 0);
        region.putInteger(SF2Region.GENERATOR_INITIALATTENUATION, -100);
        region.setSample(sample);
        layer.getRegions().add(region);

        return layer;
    }

    public static SF2Layer new_tom(SF2Soundbank sf2) {

        double[] datab;
        double[] datah;

        // Make Bass Part
        {
            int fftlen = 4096 * 4;
            double[] data = new double[2 * fftlen];
            complexGaussianDist(data, 30, 0.5, 1);
            randomPhase(data, new Random(3049912));
            ifft(data);
            normalize(data, 0.8);
            data = realPart(data);

            double d_len = data.length;
            for (int i = 0; i < data.length; i++)
                data[i] *= (1.0 - (i / d_len));
            datab = data;
        }

        // Make treble part
        {
            int fftlen = 4096 * 4;
            double[] data = new double[2 * fftlen];
            Random random = new Random(3049912);
            for (int i = 0; i < data.length; i += 2)
                data[i] = (2.0 * (random.nextDouble() - 0.5)) * 0.1;
            fft(data);
            // Remove all negative frequency
            for (int i = fftlen / 2; i < data.length; i++)
                data[i] = 0;
            for (int i = 1024 * 4; i < 2048 * 4; i++)
                data[i] = 1.0 - (i - 4096) / 4096.0;
            for (int i = 0; i < 200; i++) {
                double g = (1.0 - (i / 200.0));
                data[i] *= 1.0 + 20 * g * g;
            }
            for (int i = 0; i < 30; i++)
                data[i] = 0;
            randomPhase(data, new Random(3049912));
            ifft(data);
            normalize(data, 0.9);
            data = realPart(data);
            double gain = 1.0;
            for (int i = 0; i < data.length; i++) {
                data[i] *= gain;
                gain *= 0.9996;
            }
            datah = data;
        }

        for (int i = 0; i < datah.length; i++)
            datab[i] += datah[i] * 0.5;
        for (int i = 0; i < 5; i++)
            datab[i] *= i / 5.0;

        normalize(datab, 0.99);

        SF2Sample sample = newSimpleDrumSample(sf2, "Tom", datab);
        sample.setOriginalPitch(50);

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName("Tom");

        SF2GlobalRegion global = new SF2GlobalRegion();
        layer.setGlobalZone(global);
        sf2.addResource(layer);

        SF2LayerRegion region = new SF2LayerRegion();
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 12000);
        //region.putInteger(SF2Region.GENERATOR_SCALETUNING, 0);
        region.putInteger(SF2Region.GENERATOR_INITIALATTENUATION, -100);
        region.setSample(sample);
        layer.getRegions().add(region);

        return layer;
    }

    public static SF2Layer new_closed_hihat(SF2Soundbank sf2) {
        double[] datah;

        // Make treble part
        {
            int fftlen = 4096 * 4;
            double[] data = new double[2 * fftlen];
            Random random = new Random(3049912);
            for (int i = 0; i < data.length; i += 2)
                data[i] = (2.0 * (random.nextDouble() - 0.5)) * 0.1;
            fft(data);
            // Remove all negative frequency
            for (int i = fftlen / 2; i < data.length; i++)
                data[i] = 0;
            for (int i = 1024 * 4; i < 2048 * 4; i++)
                data[i] = 1.0 - (i - 4096) / 4096.0;
            for (int i = 0; i < 2048; i++)
                data[i] = 0.2 + 0.8 * (i / 2048.0);
            randomPhase(data, new Random(3049912));
            ifft(data);
            normalize(data, 0.9);
            data = realPart(data);
            double gain = 1.0;
            for (int i = 0; i < data.length; i++) {
                data[i] *= gain;
                gain *= 0.9996;
            }
            datah = data;
        }

        for (int i = 0; i < 5; i++)
            datah[i] *= i / 5.0;
        SF2Sample sample = newSimpleDrumSample(sf2, "Closed Hi-Hat", datah);

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName("Closed Hi-Hat");

        SF2GlobalRegion global = new SF2GlobalRegion();
        layer.setGlobalZone(global);
        sf2.addResource(layer);

        SF2LayerRegion region = new SF2LayerRegion();
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 12000);
        region.putInteger(SF2Region.GENERATOR_SCALETUNING, 0);
        region.putInteger(SF2Region.GENERATOR_EXCLUSIVECLASS, 1);
        region.setSample(sample);
        layer.getRegions().add(region);

        return layer;
    }

    public static SF2Layer new_open_hihat(SF2Soundbank sf2) {
        double[] datah;
        {
            int fftlen = 4096 * 4;
            double[] data = new double[2 * fftlen];
            Random random = new Random(3049912);
            for (int i = 0; i < data.length; i += 2)
                data[i] = (2.0 * (random.nextDouble() - 0.5));
            for (int i = fftlen / 2; i < data.length; i++)
                data[i] = 0;
            for (int i = 0; i < 200; i++)
                data[i] = 0;
            for (int i = 0; i < 2048 * 4; i++) {
                double gain = (i / (2048.0 * 4.0));
                data[i] = gain;
            }
            datah = data;
        }

        SF2Sample sample = newSimpleFFTSample(sf2, "Open Hi-Hat", datah, 1000, 5);

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName("Open Hi-Hat");

        SF2GlobalRegion global = new SF2GlobalRegion();
        layer.setGlobalZone(global);
        sf2.addResource(layer);

        SF2LayerRegion region = new SF2LayerRegion();
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 1500);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 1500);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);
        region.putInteger(SF2Region.GENERATOR_SCALETUNING, 0);
        region.putInteger(SF2Region.GENERATOR_EXCLUSIVECLASS, 1);
        region.setSample(sample);
        layer.getRegions().add(region);

        return layer;
    }

    public static SF2Layer new_crash_cymbal(SF2Soundbank sf2) {
        double[] datah;
        {
            int fftlen = 4096 * 4;
            double[] data = new double[2 * fftlen];
            Random random = new Random(3049912);
            for (int i = 0; i < data.length; i += 2)
                data[i] = (2.0 * (random.nextDouble() - 0.5));
            for (int i = fftlen / 2; i < data.length; i++)
                data[i] = 0;
            for (int i = 0; i < 100; i++)
                data[i] = 0;
            for (int i = 0; i < 512 * 2; i++) {
                double gain = (i / (512.0 * 2.0));
                data[i] = gain;
            }
            datah = data;
        }

        SF2Sample sample = newSimpleFFTSample(sf2, "Crash Cymbal", datah, 1000, 5);

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName("Crash Cymbal");

        SF2GlobalRegion global = new SF2GlobalRegion();
        layer.setGlobalZone(global);
        sf2.addResource(layer);

        SF2LayerRegion region = new SF2LayerRegion();
        region.putInteger(SF2Region.GENERATOR_DECAYVOLENV, 1800);
        region.putInteger(SF2Region.GENERATOR_SAMPLEMODES, 1);
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 1800);
        region.putInteger(SF2Region.GENERATOR_SUSTAINVOLENV, 1000);
        region.putInteger(SF2Region.GENERATOR_SCALETUNING, 0);
        region.setSample(sample);
        layer.getRegions().add(region);

        return layer;
    }

    public static SF2Layer new_side_stick(SF2Soundbank sf2) {
        double[] datab;

        // Make treble part
        {
            int fftlen = 4096 * 4;
            double[] data = new double[2 * fftlen];
            Random random = new Random(3049912);
            for (int i = 0; i < data.length; i += 2)
                data[i] = (2.0 * (random.nextDouble() - 0.5)) * 0.1;
            fft(data);
            // Remove all negative frequency
            for (int i = fftlen / 2; i < data.length; i++)
                data[i] = 0;
            for (int i = 1024 * 4; i < 2048 * 4; i++)
                data[i] = 1.0 - (i - 4096) / 4096.0;
            for (int i = 0; i < 200; i++) {
                double g = (1.0 - (i / 200.0));
                data[i] *= 1.0 + 20 * g * g;
            }
            for (int i = 0; i < 30; i++)
                data[i] = 0;
            randomPhase(data, new Random(3049912));
            ifft(data);
            normalize(data, 0.9);
            data = realPart(data);
            double gain = 1.0;
            for (int i = 0; i < data.length; i++) {
                data[i] *= gain;
                gain *= 0.9996;
            }
            datab = data;
        }

        for (int i = 0; i < 10; i++)
            datab[i] *= i / 10.0;

        SF2Sample sample = newSimpleDrumSample(sf2, "Side Stick", datab);

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName("Side Stick");

        SF2GlobalRegion global = new SF2GlobalRegion();
        layer.setGlobalZone(global);
        sf2.addResource(layer);

        SF2LayerRegion region = new SF2LayerRegion();
        region.putInteger(SF2Region.GENERATOR_RELEASEVOLENV, 12000);
        region.putInteger(SF2Region.GENERATOR_SCALETUNING, 0);
        region.putInteger(SF2Region.GENERATOR_INITIALATTENUATION, -50);
        region.setSample(sample);
        layer.getRegions().add(region);

        return layer;

    }

    public static SF2Sample newSimpleFFTSample(SF2Soundbank sf2, String name,
            double[] data, double base) {
        return newSimpleFFTSample(sf2, name, data, base, 10);
    }

    public static SF2Sample newSimpleFFTSample(SF2Soundbank sf2, String name,
            double[] data, double base, int fadeuptime) {

        int fftsize = data.length / 2;
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        double basefreq = (base / fftsize) * format.getSampleRate() * 0.5;

        randomPhase(data);
        ifft(data);
        data = realPart(data);
        normalize(data, 0.9);
        float[] fdata = toFloat(data);
        fdata = loopExtend(fdata, fdata.length + 512);
        fadeUp(fdata, fadeuptime);
        byte[] bdata = toBytes(fdata, format);

        /*
         * Create SoundFont2 sample.
         */
        SF2Sample sample = new SF2Sample(sf2);
        sample.setName(name);
        sample.setData(bdata);
        sample.setStartLoop(256);
        sample.setEndLoop(fftsize + 256);
        sample.setSampleRate((long) format.getSampleRate());
        double orgnote = (69 + 12)
                + (12 * Math.log(basefreq / 440.0) / Math.log(2));
        sample.setOriginalPitch((int) orgnote);
        sample.setPitchCorrection((byte) (-(orgnote - (int) orgnote) * 100.0));
        sf2.addResource(sample);

        return sample;
    }

    public static SF2Sample newSimpleFFTSample_dist(SF2Soundbank sf2,
            String name, double[] data, double base, double preamp) {

        int fftsize = data.length / 2;
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        double basefreq = (base / fftsize) * format.getSampleRate() * 0.5;

        randomPhase(data);
        ifft(data);
        data = realPart(data);

        for (int i = 0; i < data.length; i++) {
            data[i] = (1 - Math.exp(-Math.abs(data[i] * preamp)))
                    * Math.signum(data[i]);
        }

        normalize(data, 0.9);
        float[] fdata = toFloat(data);
        fdata = loopExtend(fdata, fdata.length + 512);
        fadeUp(fdata, 80);
        byte[] bdata = toBytes(fdata, format);

        /*
         * Create SoundFont2 sample.
         */
        SF2Sample sample = new SF2Sample(sf2);
        sample.setName(name);
        sample.setData(bdata);
        sample.setStartLoop(256);
        sample.setEndLoop(fftsize + 256);
        sample.setSampleRate((long) format.getSampleRate());
        double orgnote = (69 + 12)
                + (12 * Math.log(basefreq / 440.0) / Math.log(2));
        sample.setOriginalPitch((int) orgnote);
        sample.setPitchCorrection((byte) (-(orgnote - (int) orgnote) * 100.0));
        sf2.addResource(sample);

        return sample;
    }

    public static SF2Sample newSimpleDrumSample(SF2Soundbank sf2, String name,
            double[] data) {

        int fftsize = data.length;
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);

        byte[] bdata = toBytes(toFloat(realPart(data)), format);

        /*
         * Create SoundFont2 sample.
         */
        SF2Sample sample = new SF2Sample(sf2);
        sample.setName(name);
        sample.setData(bdata);
        sample.setStartLoop(256);
        sample.setEndLoop(fftsize + 256);
        sample.setSampleRate((long) format.getSampleRate());
        sample.setOriginalPitch(60);
        sf2.addResource(sample);

        return sample;
    }

    public static SF2Layer newLayer(SF2Soundbank sf2, String name, SF2Sample sample) {
        SF2LayerRegion region = new SF2LayerRegion();
        region.setSample(sample);

        SF2Layer layer = new SF2Layer(sf2);
        layer.setName(name);
        layer.getRegions().add(region);
        sf2.addResource(layer);

        return layer;
    }

    public static SF2Instrument newInstrument(SF2Soundbank sf2, String name,
            Patch patch, SF2Layer... layers) {

        /*
         * Create SoundFont2 instrument.
         */
        SF2Instrument ins = new SF2Instrument(sf2);
        ins.setPatch(patch);
        ins.setName(name);
        sf2.addInstrument(ins);

        /*
         * Create region for instrument.
         */
        for (int i = 0; i < layers.length; i++) {
            SF2InstrumentRegion insregion = new SF2InstrumentRegion();
            insregion.setLayer(layers[i]);
            ins.getRegions().add(insregion);
        }

        return ins;
    }

    public static void ifft(double[] data) {
        new FFT(data.length / 2, 1).transform(data);
    }

    public static void fft(double[] data) {
        new FFT(data.length / 2, -1).transform(data);
    }

    public static void complexGaussianDist(double[] cdata, double m,
            double s, double v) {
        for (int x = 0; x < cdata.length / 4; x++) {
            cdata[x * 2] += v * (1.0 / (s * Math.sqrt(2 * Math.PI))
                    * Math.exp((-1.0 / 2.0) * Math.pow((x - m) / s, 2.0)));
        }
    }

    public static void randomPhase(double[] data) {
        for (int i = 0; i < data.length; i += 2) {
            double phase = Math.random() * 2 * Math.PI;
            double d = data[i];
            data[i] = Math.sin(phase) * d;
            data[i + 1] = Math.cos(phase) * d;
        }
    }

    public static void randomPhase(double[] data, Random random) {
        for (int i = 0; i < data.length; i += 2) {
            double phase = random.nextDouble() * 2 * Math.PI;
            double d = data[i];
            data[i] = Math.sin(phase) * d;
            data[i + 1] = Math.cos(phase) * d;
        }
    }

    public static void normalize(double[] data, double target) {
        double maxvalue = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] > maxvalue)
                maxvalue = data[i];
            if (-data[i] > maxvalue)
                maxvalue = -data[i];
        }
        if (maxvalue == 0)
            return;
        double gain = target / maxvalue;
        for (int i = 0; i < data.length; i++)
            data[i] *= gain;
    }

    public static void normalize(float[] data, double target) {
        double maxvalue = 0.5;
        for (int i = 0; i < data.length; i++) {
            if (data[i * 2] > maxvalue)
                maxvalue = data[i * 2];
            if (-data[i * 2] > maxvalue)
                maxvalue = -data[i * 2];
        }
        double gain = target / maxvalue;
        for (int i = 0; i < data.length; i++)
            data[i * 2] *= gain;
    }

    public static double[] realPart(double[] in) {
        double[] out = new double[in.length / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = in[i * 2];
        }
        return out;
    }

    public static double[] imgPart(double[] in) {
        double[] out = new double[in.length / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = in[i * 2];
        }
        return out;
    }

    public static float[] toFloat(double[] in) {
        float[] out = new float[in.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = (float) in[i];
        }
        return out;
    }

    public static byte[] toBytes(float[] in, AudioFormat format) {
        byte[] out = new byte[in.length * format.getFrameSize()];
        return AudioFloatConverter.getConverter(format).toByteArray(in, out);
    }

    public static void fadeUp(double[] data, int samples) {
        double dsamples = samples;
        for (int i = 0; i < samples; i++)
            data[i] *= i / dsamples;
    }

    public static void fadeUp(float[] data, int samples) {
        double dsamples = samples;
        for (int i = 0; i < samples; i++)
            data[i] *= i / dsamples;
    }

    public static double[] loopExtend(double[] data, int newsize) {
        double[] outdata = new double[newsize];
        int p_len = data.length;
        int p_ps = 0;
        for (int i = 0; i < outdata.length; i++) {
            outdata[i] = data[p_ps];
            p_ps++;
            if (p_ps == p_len)
                p_ps = 0;
        }
        return outdata;
    }

    public static float[] loopExtend(float[] data, int newsize) {
        float[] outdata = new float[newsize];
        int p_len = data.length;
        int p_ps = 0;
        for (int i = 0; i < outdata.length; i++) {
            outdata[i] = data[p_ps];
            p_ps++;
            if (p_ps == p_len)
                p_ps = 0;
        }
        return outdata;
    }
}
