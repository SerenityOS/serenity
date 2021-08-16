/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;

/**
 * @test
 * @bug 4667064
 * @summary Java Sound provides bogus SourceDataLine and TargetDataLine
 */
public class BogusMixers {

    public static void main(String[] args) throws Exception {
        try {
            out("4667064: Java Sound provides bogus SourceDataLine and TargetDataLine");

            Mixer.Info[]    aInfos = AudioSystem.getMixerInfo();
            out("  available Mixers:");
            for (int i = 0; i < aInfos.length; i++) {
                if (aInfos[i].getName().startsWith("Java Sound Audio Engine")) {
                    Mixer mixer = AudioSystem.getMixer(aInfos[i]);
                    Line.Info[] tlInfos = mixer.getTargetLineInfo();
                    for (int ii = 0; ii<tlInfos.length; ii++) {
                        if (tlInfos[ii].getLineClass() == DataLine.class) {
                            throw new Exception("Bogus TargetDataLine with DataLine info present!");
                        }
                    }
                }
                if (aInfos[i].getName().startsWith("WinOS,waveOut,multi threaded")) {
                    throw new Exception("Bogus mixer 'WinOS,waveOut,multi threaded' present!");
                }
                out(aInfos[i].getName());
            }
            if (aInfos.length == 0)
            {
                out("[No mixers available] - not a failure of this test case.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        out("Test passed");
    }

    static void out(String s) {
        System.out.println(s); System.out.flush();
    }
}
