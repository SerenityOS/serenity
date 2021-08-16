/*
 * Copyright (c) 1999, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.sound.midi;

/**
 * An instrument is a sound-synthesis algorithm with certain parameter settings,
 * usually designed to emulate a specific real-world musical instrument or to
 * achieve a specific sort of sound effect. Instruments are typically stored in
 * collections called soundbanks. Before the instrument can be used to play
 * notes, it must first be loaded onto a synthesizer, and then it must be
 * selected for use on one or more channels, via a program-change command. MIDI
 * notes that are subsequently received on those channels will be played using
 * the sound of the selected instrument.
 *
 * @author Kara Kytle
 * @see Soundbank
 * @see Soundbank#getInstruments
 * @see Patch
 * @see Synthesizer#loadInstrument(Instrument)
 * @see MidiChannel#programChange(int, int)
 */
public abstract class Instrument extends SoundbankResource {

    /**
     * Instrument patch.
     */
    private final Patch patch;

    /**
     * Constructs a new MIDI instrument from the specified {@code Patch}. When a
     * subsequent request is made to load the instrument, the sound bank will
     * search its contents for this instrument's {@code Patch}, and the
     * instrument will be loaded into the synthesizer at the bank and program
     * location indicated by the {@code Patch} object.
     *
     * @param  soundbank sound bank containing the instrument
     * @param  patch the patch of this instrument
     * @param  name the name of this instrument
     * @param  dataClass the class used to represent the sample's data
     * @see Synthesizer#loadInstrument(Instrument)
     */
    protected Instrument(Soundbank soundbank, Patch patch, String name, Class<?> dataClass) {

        super(soundbank, name, dataClass);
        this.patch = patch;
    }

    /**
     * Obtains the {@code Patch} object that indicates the bank and program
     * numbers where this instrument is to be stored in the synthesizer.
     *
     * @return this instrument's patch
     */
    public Patch getPatch() {
        return patch;
    }
}
