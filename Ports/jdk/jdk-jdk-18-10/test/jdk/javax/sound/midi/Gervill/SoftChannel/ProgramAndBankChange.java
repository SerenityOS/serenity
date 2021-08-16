/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
   @summary Test SoftChannel program and bank change
   @modules java.desktop/com.sun.media.sound
*/

import java.io.IOException;

import javax.sound.midi.*;
import javax.sound.sampled.*;

import com.sun.media.sound.*;

public class ProgramAndBankChange {

    private static SimpleInstrument generateTestInstrument(Patch patch) {
        ModelOscillator osc = new ModelOscillator() {
            public float getAttenuation() {
                return 0;
            }

            public int getChannels() {
                return 1;
            }

            public ModelOscillatorStream open(float samplerate) {
                return new ModelOscillatorStream() {
                    public void close() throws IOException {
                    }

                    public void noteOff(int velocity) {
                    }

                    public void noteOn(MidiChannel channel, VoiceStatus voice,
                            int noteNumber, int velocity) {
                    }

                    public int read(float[][] buffer, int offset, int len)
                            throws IOException {
                        return len;
                    }

                    public void setPitch(float ipitch) {
                    }
                };
            }
        };
        ModelPerformer performer = new ModelPerformer();
        performer.getOscillators().add(osc);
        SimpleInstrument testinstrument = new SimpleInstrument();
        testinstrument.setPatch(patch);
        testinstrument.add(performer);
        return testinstrument;
    }

    private static void assertTrue(boolean value) throws Exception {
        if (!value)
            throw new RuntimeException("assertTrue fails!");
    }

    private static void testProgramAndBank(SoftSynthesizer soft,
            AudioInputStream stream, Patch patch) throws Exception {

        int program = patch.getProgram();
        int bank = patch.getBank();

        MidiChannel channel = soft.getChannels()[0];
        byte[] buff = new byte[2048];

        channel.programChange(bank, program);
        channel.noteOn(64, 64);
        stream.read(buff, 0, buff.length);

        int foundprogram = -1;
        int foundbank = -1;
        VoiceStatus[] vstatus = soft.getVoiceStatus();
        for (int i = 0; i < vstatus.length; i++) {
            if (vstatus[i].active) {
                foundprogram = vstatus[i].program;
                foundbank = vstatus[i].bank;
                break;
            }
        }

        assertTrue(foundprogram == program);
        assertTrue(foundbank == bank);

        channel.noteOn(64, 0);
        stream.read(buff, 0, buff.length);

        channel = soft.getChannels()[1];
        // Send MSB Bank
        channel.controlChange(0x00, bank / 128);
        // Send LSB Bank
        channel.controlChange(0x20, bank % 128);
        // Send Program Change
        channel.programChange(program);
        channel.noteOn(64, 64);
        stream.read(buff, 0, buff.length);

        foundprogram = -1;
        foundbank = -1;
        vstatus = soft.getVoiceStatus();
        for (int i = 0; i < vstatus.length; i++) {
            if (vstatus[i].active) {
                foundprogram = vstatus[i].program;
                foundbank = vstatus[i].bank;
                break;
            }
        }
        assertTrue(foundprogram == program);
        assertTrue(foundbank == bank);
        channel.noteOn(64, 0);
        stream.read(buff, 0, buff.length);
    }

    public static void main(String[] args) throws Exception {
        SoftSynthesizer soft = new SoftSynthesizer();
        AudioInputStream stream = soft.openStream(null, null);
        soft.unloadAllInstruments(soft.getDefaultSoundbank());

        soft.loadInstrument(generateTestInstrument(new Patch(0, 0)));
        soft.loadInstrument(generateTestInstrument(new Patch(7, 0)));
        soft.loadInstrument(generateTestInstrument(new Patch(20, 10)));
        soft.loadInstrument(generateTestInstrument(new Patch(3678, 15)));
        soft.loadInstrument(generateTestInstrument(new Patch(4678, 15)));

        testProgramAndBank(soft, stream, new Patch(0, 0));
        testProgramAndBank(soft, stream, new Patch(7, 0));
        testProgramAndBank(soft, stream, new Patch(20, 10));
        testProgramAndBank(soft, stream, new Patch(3678, 15));
        testProgramAndBank(soft, stream, new Patch(4678, 15));

        soft.close();
    }
}
