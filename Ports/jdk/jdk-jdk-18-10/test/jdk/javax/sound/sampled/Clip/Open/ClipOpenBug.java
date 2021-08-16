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

import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;

/**
 * @test
 * @bug 4479444
 * @summary Verify that the error string of Clip.open() is meaningful
 */
public class ClipOpenBug {

    public static void main(String args[]) throws Exception {
        boolean res = true;
        try {
            AudioInputStream ais = new AudioInputStream(
                    new ByteArrayInputStream(new byte[2000]),
                    new AudioFormat(8000.0f, 8, 1, false, false), 2000); //
            AudioFormat format = ais.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format,
                                                   ((int) ais.getFrameLength()
                                                            * format
                                                           .getFrameSize()));
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open();
            FloatControl rateControl = (FloatControl) clip.getControl(
                    FloatControl.Type.SAMPLE_RATE);
            int c = 0;
            while (c++ < 10) {
                clip.stop();
                clip.setFramePosition(0);
                clip.start();
                for (float frq = 22000; frq < 44100; frq = frq + 100) {
                    try {
                        Thread.currentThread().sleep(20);
                    } catch (Exception e) {
                        break;
                    }
                    rateControl.setValue(frq);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            res = ex.getMessage().indexOf(
                    "This method should not have been invoked!") < 0;
        }
        if (res) {
            System.out.println("Test passed");
        } else {
            System.out.println("Test failed");
            throw new Exception("Test failed");
        }
    }
}
