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
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

/**
 * @test
 * @bug 4479441
 * @summary Verify that the lines report 16KHz and 32KHz capability
 */
public class Has16and32KHz {

    public static boolean ok32=false;
    public static boolean ok16=false;

    public static void showMixerLines(Line.Info[] lineinfo) {
        for (int j = 0; j < lineinfo.length; j++) {
            boolean isSDL=false; // SourceDataLine
            Line.Info thisInfo=lineinfo[j];
            System.out.println("  " + thisInfo);
            String impl="";
            if (thisInfo.getLineClass().equals(SourceDataLine.class)) {
                isSDL=true;
                impl+="SourceDataLine";
            }
            if (thisInfo.getLineClass().equals(Clip.class)) {
                impl+="Clip";
            }
            if (thisInfo.getLineClass().equals(DataLine.class)) {
                impl+="DataLine";
            }
            if (thisInfo.getLineClass().equals(TargetDataLine.class)) {
                impl+="TargetDataLine";
            }
            if (thisInfo.getLineClass().equals(Mixer.class)) {
                impl+="Mixer";
            }
            System.out.println("  implements "+impl);
            try {
                AudioFormat[] formats = ((DataLine.Info)lineinfo[j]).getFormats();
                for (int k = 0; k < formats.length; k++) {
                    System.out.println("    " + formats[k] + ", "+ formats[k].getFrameSize()+" bytes/frame");
                    if (isSDL) {
                        if ((formats[k].getSampleRate()==AudioSystem.NOT_SPECIFIED)
                            || (formats[k].getSampleRate()==32000.0f)) {
                            ok32=true;
                        }
                        if ((formats[k].getSampleRate()==AudioSystem.NOT_SPECIFIED)
                            || (formats[k].getSampleRate()==16000.0f)) {
                            ok16=true;
                        }
                    }
                }
            } catch (ClassCastException e) {
            }
        }
    }

    public static void main(String[] args) throws Exception {
        boolean res=true;

        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        System.out.println(mixerInfo.length+" mixers on system.");
        if (mixerInfo.length == 0) {
            System.out.println("Cannot execute test. Not Failed!");
        } else {
            for (int i = 0; i < mixerInfo.length; i++) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo[i]);
                System.out.println();
                System.out.println(mixer+":");
                showMixerLines(mixer.getSourceLineInfo());
                showMixerLines(mixer.getTargetLineInfo());


            }
            res=ok16 && ok32;
        }
        if (res) {
            System.out.println("Test passed");
        } else {
            System.out.println("Test failed");
            throw new Exception("Test failed");
        }
        //ystem.exit(res?0:1);
    }
}
