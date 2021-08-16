/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;

/**
 * @test
 * @bug 4964288
 * @summary Unexpected IAE raised while getting TargetDataLine
 */
public class UnexpectedIAE {

    public static void main(String argv[]) throws Exception {
        boolean success = true;

        Mixer.Info [] infos = AudioSystem.getMixerInfo();

        for (int i=0; i<infos.length; i++) {
            Mixer mixer = AudioSystem.getMixer(infos[i]);
            System.out.println("Mixer is: " + mixer);
            Line.Info [] target_line_infos = mixer.getTargetLineInfo();
            for (int j = 0; j < target_line_infos.length; j++) {
                try {
                    System.out.println("Trying to get:" + target_line_infos[j]);
                    mixer.getLine(target_line_infos[j]);
                } catch (IllegalArgumentException iae) {
                    System.out.println("Unexpected IllegalArgumentException raised:");
                    iae.printStackTrace();
                    success = false;
                } catch (LineUnavailableException lue) {
                    System.out.println("Unexpected LineUnavailableException raised:");
                    lue.printStackTrace();
                    success = false;
                }
            }
        }
        if (success) {
            System.out.println("Test passed");
        } else {
            throw new Exception("Test FAILED");
        }
    }
}
