/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * @test
 * @bug 4851018
 * @summary MidiMessage.getLength and .getData return wrong values.
 *          also: 4890405: Reading MidiMessage byte array fails in 1.4.2
 */
public class FastShortMessage {
    public static void main(String args[]) throws Exception {
        int[] dataMes =  {ShortMessage.NOTE_ON | 9, 0x24, 0x50};
        int res = 240;
        Sequence midiData = new Sequence(Sequence.PPQ, res);

        Track track = midiData.createTrack();
        ShortMessage msg = new ShortMessage();
        msg.setMessage(dataMes[0], dataMes[1], dataMes[2]);
        track.add(new MidiEvent(msg, 0));

        // save sequence to outputstream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MidiSystem.write(midiData, 0, baos);

        // reload that sequence
        InputStream is = new ByteArrayInputStream(baos.toByteArray());
        Sequence seq = MidiSystem.getSequence(is);

        track = seq.getTracks()[0];
        msg = (ShortMessage) (track.get(0).getMessage());
        byte[] msgData = msg.getMessage();

        if (msgData.length != dataMes.length
         || (msgData[0] & 0xFF) != dataMes[0]
         || (msgData[1] & 0xFF) != dataMes[1]
         || (msgData[2] & 0xFF) != dataMes[2]) {
            throw new Exception("test failed. read length="+msgData.length);
        }
        System.out.println("Test Passed.");
    }
}
