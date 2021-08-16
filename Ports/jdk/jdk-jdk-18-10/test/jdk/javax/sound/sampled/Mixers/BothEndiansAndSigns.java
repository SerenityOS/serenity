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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;

/**
 * @test
 * @bug 4936397
 * @summary Verify that there'll for a given endianness, there's also the little
 *          endian version
 */
public class BothEndiansAndSigns {
    static boolean failed = false;
    static int testedFormats = 0;

    public static void main(String[] args) throws Exception {
        out("4936397: Verify that there'll for a given endianness, there's also the little endian version");
        out("         and the same for signed'ness for 8-bit formats");

        Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
        for (int i = 0; i < aInfos.length; i++) {
            try {
                Mixer mixer = AudioSystem.getMixer(aInfos[i]);
                out("Mixer "+aInfos[i]);
                checkLines(mixer, mixer.getSourceLineInfo());
                checkLines(mixer, mixer.getTargetLineInfo());
            } catch (Exception e) {
                out("Unexpected exception when getting a mixer: "+e);
            }
        }
        if (testedFormats == 0) {
            out("[No appropriate lines available] - cannot exercise this test.");
        } else {
            if (failed) {
                throw new Exception("Test FAILED!");
            }
            out("Test passed");
        }
    }

    public static void checkLines(Mixer mixer, Line.Info[] infos) {
        for (int i = 0; i<infos.length; i++) {
            try {
                if (infos[i] instanceof DataLine.Info) {
                    DataLine.Info info = (DataLine.Info) infos[i];
                    System.out.println(" Line "+info+" (max. "+mixer.getMaxLines(info)+" simultaneously): ");
                    AudioFormat[] formats = info.getFormats();
                    for (int f = 0; f < formats.length; f++) {
                        try {
                            AudioFormat otherEndianOrSign = getOtherEndianOrSign(formats[f]);
                            if (otherEndianOrSign != null) {
                                checkFormat(formats, otherEndianOrSign);
                            }
                        } catch (Exception e1) {
                            out("  Unexpected exception when getting a format: "+e1);
                        }
                    }
                }
            } catch (Exception e) {
                out(" Unexpected exception when getting a line: "+e);
            }
        }
    }

    public static void checkFormat(AudioFormat[] formats, AudioFormat format) {
        for (int i = 0; i < formats.length; i++) {
            testedFormats++;
            if (formats[i].matches(format)) {
                return;
            }
        }
        out("  ## expected this format: "+format
            +" ("+format.getChannels()+" channels, "
            +"frameSize="+format.getFrameSize()+", "
            +(format.isBigEndian()?"big endian":"little endian")
            +")");
        failed = true;
    }

    public static AudioFormat getOtherEndianOrSign(AudioFormat format) {
        AudioFormat.Encoding newEnc = null;
        boolean newEndian = format.isBigEndian();
        boolean isSigned = format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
        boolean isUnsigned = format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED);
        if ((isSigned || isUnsigned) && format.getSampleSizeInBits() > 0) {
            if (format.getSampleSizeInBits() == 8) {
                // return the other signed'ness
                if (isSigned) {
                    newEnc = AudioFormat.Encoding.PCM_UNSIGNED;
                } else {
                    newEnc = AudioFormat.Encoding.PCM_SIGNED;
                }
            } else {
                newEnc = format.getEncoding();
                newEndian = !newEndian;
            }
            if (newEnc != null) {
                return new AudioFormat(newEnc, format.getSampleRate(),
                                       format.getSampleSizeInBits(),
                                       format.getChannels(),
                                       format.getFrameSize(),
                                       format.getFrameRate(),
                                       newEndian);
            }
        }
        return null;
    }

    static void out(String s) {
        System.out.println(s); System.out.flush();
    }
}
