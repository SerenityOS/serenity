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

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.sampled.AudioFormat;

/**
 * The model instrument class.
 *
 * <p>The main methods to override are:<br>
 * getPerformer, getDirector, getChannelMixer.
 *
 * <p>Performers are used to define what voices which will
 * playback when using the instrument.<br>
 *
 * ChannelMixer is used to add channel-wide processing
 * on voices output or to define non-voice oriented instruments.<br>
 *
 * Director is used to change how the synthesizer
 * chooses what performers to play on midi events.
 *
 * @author Karl Helgason
 */
public abstract class ModelInstrument extends Instrument {

    protected ModelInstrument(Soundbank soundbank, Patch patch, String name,
            Class<?> dataClass) {
        super(soundbank, patch, name, dataClass);
    }

    public ModelDirector getDirector(ModelPerformer[] performers,
            MidiChannel channel, ModelDirectedPlayer player) {
        return new ModelStandardIndexedDirector(performers, player);
    }

    public ModelPerformer[] getPerformers() {
        return new ModelPerformer[0];
    }

    public ModelChannelMixer getChannelMixer(MidiChannel channel,
            AudioFormat format) {
        return null;
    }

    // Get General MIDI 2 Alias patch for this instrument.
    public final Patch getPatchAlias() {
        Patch patch = getPatch();
        int program = patch.getProgram();
        int bank = patch.getBank();
        if (bank != 0)
            return patch;
        boolean percussion = false;
        if (getPatch() instanceof ModelPatch)
            percussion = ((ModelPatch)getPatch()).isPercussion();
        if (percussion)
            return new Patch(0x78 << 7, program);
        else
            return new Patch(0x79 << 7, program);
    }

    // Return name of all the keys.
    // This information is generated from ModelPerformer.getName()
    // returned from getPerformers().
    public final String[] getKeys() {
        String[] keys = new String[128];
        for (ModelPerformer performer : getPerformers()) {
            for (int k = performer.getKeyFrom(); k <= performer.getKeyTo(); k++) {
                if (k >= 0 && k < 128 && keys[k] == null) {
                    String name = performer.getName();
                    if (name == null)
                        name = "untitled";
                    keys[k] = name;
                }
            }
        }
        return keys;
    }

    // Return what channels this instrument will probably response
    // on General MIDI synthesizer.
    public final boolean[] getChannels() {
        boolean percussion = false;
        if (getPatch() instanceof ModelPatch)
            percussion = ((ModelPatch)getPatch()).isPercussion();

        // Check if instrument is percussion.
        if (percussion) {
            boolean[] ch = new boolean[16];
            for (int i = 0; i < ch.length; i++)
                ch[i] = false;
            ch[9] = true;
            return ch;
        }

        // Check if instrument uses General MIDI 2 default banks.
        int bank = getPatch().getBank();
        if (bank >> 7 == 0x78 || bank >> 7 == 0x79) {
            boolean[] ch = new boolean[16];
            for (int i = 0; i < ch.length; i++)
                ch[i] = true;
            return ch;
        }

        boolean[] ch = new boolean[16];
        for (int i = 0; i < ch.length; i++)
            ch[i] = true;
        ch[9] = false;
        return ch;
    }
}
