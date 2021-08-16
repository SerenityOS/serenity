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
 * A {@code Patch} object represents a location, on a MIDI synthesizer, into
 * which a single instrument is stored (loaded). Every {@code Instrument} object
 * has its own {@code Patch} object that specifies the memory location into
 * which that instrument should be loaded. The location is specified abstractly
 * by a bank index and a program number (not by any scheme that directly refers
 * to a specific address or offset in RAM). This is a hierarchical indexing
 * scheme: MIDI provides for up to 16384 banks, each of which contains up to 128
 * program locations. For example, a minimal sort of synthesizer might have only
 * one bank of instruments, and only 32 instruments (programs) in that bank.
 * <p>
 * To select what instrument should play the notes on a particular MIDI channel,
 * two kinds of MIDI message are used that specify a patch location: a
 * bank-select command, and a program-change channel command. The Java Sound
 * equivalent is the
 * {@link MidiChannel#programChange(int, int) programChange(int, int)} method of
 * {@code MidiChannel}.
 *
 * @author Kara Kytle
 * @see Instrument
 * @see Instrument#getPatch()
 * @see MidiChannel#programChange(int, int)
 * @see Synthesizer#loadInstruments(Soundbank, Patch[])
 * @see Soundbank
 * @see Sequence#getPatchList()
 */
public class Patch {

    /**
     * Bank index.
     */
    private final int bank;

    /**
     * Program change number.
     */
    private final int program;

    /**
     * Constructs a new patch object from the specified bank and program
     * numbers.
     *
     * @param  bank the bank index (in the range from 0 to 16383)
     * @param  program the program index (in the range from 0 to 127)
     */
    public Patch(int bank, int program) {
        this.bank = bank;
        this.program = program;
    }

    /**
     * Returns the number of the bank that contains the instrument whose
     * location this {@code Patch} specifies.
     *
     * @return the bank number, whose range is from 0 to 16383
     * @see MidiChannel#programChange(int, int)
     */
    public int getBank() {
        return bank;
    }

    /**
     * Returns the index, within a bank, of the instrument whose location this
     * {@code Patch} specifies.
     *
     * @return the instrument's program number, whose range is from 0 to 127
     * @see MidiChannel#getProgram
     * @see MidiChannel#programChange(int)
     * @see MidiChannel#programChange(int, int)
     */
    public int getProgram() {
        return program;
    }
}
