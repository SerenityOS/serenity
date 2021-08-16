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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;

/**
 * @test
 * @bug 5013897
 * @summary Verify that plughw: provides mono and 8-bit lines
 */
public class PlugHwMonoAnd8bitAvailable {
    static int failed = 0;
    static int testedFormats = 0;

    public static void main(String[] args) throws Exception {
        out("5013897: Verify that plughw: provides mono and 8-bit lines");

        Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
        for (int i = 0; i < aInfos.length; i++) {
            try {
                Mixer mixer = AudioSystem.getMixer(aInfos[i]);
                out("Mixer "+aInfos[i]);
                if (aInfos[i].getName().contains("plughw")) {
                    checkLines(mixer, mixer.getSourceLineInfo());
                    checkLines(mixer, mixer.getTargetLineInfo());
                } else {
                    out("  -> not plughw, ignored.");
                }
            } catch (Exception e) {
                out("Unexpected exception when getting a mixer: "+e);
            }
        }
        if (testedFormats == 0) {
            out("[No appropriate lines available] - cannot exercise this test.");
        } else {
            if (failed>0) {
                throw new Exception("Test FAILED!");
            }
            out("Successfully verified "+testedFormats+" formats.");
            out("Test passed");
        }
    }

    public static void checkLines(Mixer mixer, Line.Info[] infos) {
        for (int i = 0; i<infos.length; i++) {
                try {
                        System.out.println(" Line "+infos[i]+" (max. "+mixer.getMaxLines(infos[i])+" simultaneously): ");
                        if (infos[i] instanceof DataLine.Info) {
                                DataLine.Info info = (DataLine.Info) infos[i];
                                int thisTestedFormats = testedFormats;
                                int thisFailed = failed;
                                AudioFormat[] formats = info.getFormats();
                                for (int f = 0; f < formats.length; f++) {
                                        if (formats[f].getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)
                                        || formats[f].getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
                                                try {
                                                        if (formats[f].getSampleSizeInBits() > 16) {
                                                                // if a bit size larger than 16 is available, also 16-bit must be there
                                                                checkFormat(formats, getOtherBits(formats[f], 16));
                                                        } else
                                                        if (formats[f].getSampleSizeInBits() > 8) {
                                                                // if a bit size larger than 8 is available, also 8-bit must be there
                                                                checkFormat(formats, getOtherBits(formats[f], 8));
                                                        }
                                                        if (formats[f].getChannels() > 2) {
                                                                // if more than 2 channels, also 2 channels must be there
                                                                checkFormat(formats, getOtherChannels(formats[f], 2));
                                                        } else
                                                        if (formats[f].getChannels() > 1) {
                                                                // if more than 1 channel, also 1 channel must be there
                                                                checkFormat(formats, getOtherChannels(formats[f], 1));
                                                        }
                                                } catch (Exception e1) {
                                                        out("  Unexpected exception when getting a format: "+e1);
                                                }
                                        }
                                }
                                if (testedFormats - thisTestedFormats == 0) {
                                        out(" -->could not test any formats");
                                } else if (failed - thisFailed == 0) {
                                        out(" -->"+(testedFormats - thisTestedFormats)+" formats tested OK");
                                }

                        } else {
                                out("  --> not a DataLine");
                        }
                } catch (Exception e) {
                        out(" Unexpected exception when getting a line: "+e);
                }
        }
    }

    public static void checkFormat(AudioFormat[] formats, AudioFormat format) {
        testedFormats++;
        for (int i = 0; i < formats.length; i++) {
            if (formats[i].matches(format)) {
                return;
            }
        }
        out("  ## expected this format: "+format
            +" ("+format.getChannels()+" channels, "
            +"frameSize="+format.getFrameSize()+", "
            +(format.isBigEndian()?"big endian":"little endian")
            +")");
        failed++;
    }

    // only works for PCM encodings
    public static AudioFormat getOtherBits(AudioFormat format, int newBits) {
        boolean isSigned = format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
        return new AudioFormat(format.getSampleRate(),
                               newBits,
                               format.getChannels(),
                               isSigned,
                               (newBits>8)?format.isBigEndian():false);
    }

    // only works for PCM encodings
    public static AudioFormat getOtherChannels(AudioFormat format, int newChannels) {
        int newFrameSize;
        if (newChannels <= 0 || format.getChannels() <= 0 || format.getFrameSize() <= 0) {
            newFrameSize = -1;
        } else {
            newFrameSize = format.getFrameSize() / format.getChannels() * newChannels;
        }
        return new AudioFormat(format.getEncoding(),
                               format.getSampleRate(),
                               format.getSampleSizeInBits(),
                               newChannels,
                               newFrameSize,
                               format.getFrameRate(),
                               format.isBigEndian());
    }


    static void out(String s) {
        System.out.println(s); System.out.flush();
    }
}
