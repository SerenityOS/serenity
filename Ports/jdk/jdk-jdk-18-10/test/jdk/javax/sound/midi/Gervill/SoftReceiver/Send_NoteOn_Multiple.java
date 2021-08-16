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
   @summary Test SoftReceiver send method
   @modules java.desktop/com.sun.media.sound
*/

import javax.sound.midi.*;
import javax.sound.sampled.*;

import com.sun.media.sound.*;

public class Send_NoteOn_Multiple {

    private static void assertEquals(Object a, Object b) throws Exception
    {
        if(!a.equals(b))
            throw new RuntimeException("assertEquals fails!");
    }

    private static void assertTrue(boolean value) throws Exception
    {
        if(!value)
            throw new RuntimeException("assertTrue fails!");
    }

    public static void main(String[] args) throws Exception {
        SoftTestUtils soft = new SoftTestUtils();
        MidiChannel channel = soft.synth.getChannels()[0];
        Receiver receiver = soft.synth.getReceiver();

        ShortMessage smsg = new ShortMessage();
        smsg.setMessage(ShortMessage.NOTE_ON,0, 60, 64);
        receiver.send(smsg, -1);
        smsg.setMessage(ShortMessage.NOTE_ON,0, 61, 64);
        receiver.send(smsg, -1);
        smsg.setMessage(ShortMessage.NOTE_ON,0, 62, 64);
        receiver.send(smsg, -1);
        soft.read(1);
        assertTrue(soft.findVoice(0,60) != null);
        assertTrue(soft.findVoice(0,61) != null);
        assertTrue(soft.findVoice(0,62) != null);

        smsg.setMessage(ShortMessage.NOTE_ON,0, 60, 0);
        receiver.send(smsg, -1);
        smsg.setMessage(ShortMessage.NOTE_ON,0, 61, 0);
        receiver.send(smsg, -1);
        soft.read(1);
        assertTrue(soft.findVoice(0,60) == null);
        assertTrue(soft.findVoice(0,61) == null);
        assertTrue(soft.findVoice(0,62) != null);

        soft.close();
    }
}
