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
 @summary Test EmergencySoundbank createSoundbank() method
 @modules java.desktop/com.sun.media.sound
*/

import java.io.File;

import javax.sound.midi.Instrument;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;

import com.sun.media.sound.EmergencySoundbank;
import com.sun.media.sound.ModelInstrument;
import com.sun.media.sound.ModelPatch;

public class TestCreateSoundbank {

    public static void main(String[] args) throws Exception {

        Soundbank soundbank = EmergencySoundbank.createSoundbank();
        for (int i = 0; i < 128; i++) {
            Patch patch = new ModelPatch(0, i, false);
            ModelInstrument ins = (ModelInstrument)soundbank.getInstrument(patch);
            if(ins == null)
                throw new Exception("Instrument " + i + " is missing!");
            if(ins.getPerformers().length == 0)
                throw new Exception("Instrument " + i + " doesn't have any performers!");
        }
        Patch patch = new ModelPatch(0, 0, true);
        ModelInstrument ins = (ModelInstrument)soundbank.getInstrument(patch);
        if(ins == null)
            throw new Exception("Drumkit instrument is missing!");
        if(ins.getPerformers().length == 0)
            throw new Exception("Drumkit instrument doesn't have any performers!");
    }
}
