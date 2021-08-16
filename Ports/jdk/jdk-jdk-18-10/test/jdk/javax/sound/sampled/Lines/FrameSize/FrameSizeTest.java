/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;

/**
 * @test
 * @bug 4469409
 * @summary Check that the frame size in the formats returned by lines is
 *          correct
 */
public class FrameSizeTest {
    public static void main(String[] args) throws Exception {
        boolean res=true;
        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        for (int i = 0; i < mixerInfo.length; i++) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo[i]);
            System.out.println(mixer);
            Line.Info[] lineinfo = mixer.getSourceLineInfo();
            for (int j = 0; j < lineinfo.length; j++) {
                System.out.println("  " + lineinfo[j]);
                try {
                    AudioFormat[] formats = ((DataLine.Info)lineinfo[j]).getFormats();
                    for (int k = 0; k < formats.length; k++) {
                        if ( (formats[k].getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)
                              || formats[k].getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED))
                             && (formats[k].getFrameSize() != AudioSystem.NOT_SPECIFIED)
                             && ((formats[k].getSampleSizeInBits() == 16) || (formats[k].getSampleSizeInBits() == 8))
                             && ((((formats[k].getSampleSizeInBits() + 7) / 8) * formats[k].getChannels()) != formats[k].getFrameSize())) {
                            System.out.println(" # " + formats[k] + ", getFrameSize() wrongly returns"+ formats[k].getFrameSize());
                            res=false;
                        }
                    }
                } catch (ClassCastException e) {
                }
            }
        }

        if (res) {
            System.out.println("Test passed");
        } else {
            System.out.println("Test failed");
            throw new Exception("Test failed");
        }
    }
}
