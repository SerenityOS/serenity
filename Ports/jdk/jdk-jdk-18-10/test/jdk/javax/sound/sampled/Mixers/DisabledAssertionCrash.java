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
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

/**
 * @test
 * @bug 4991672
 * @summary disabled assertion at maximum thread priority causes audio crash
 * @run main/timeout=600 DisabledAssertionCrash
 */
public class DisabledAssertionCrash {
    private static final int bufferSize = 1024;

    public static void main(String[] args) {

        System.out.println("This program hangs if priority is set,");
        System.out.println("and assertion is in the code.");
        System.out.println("The program crashes the entire Windows system");
        System.out.println("if assertions are disabled.");
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            AudioFormat audioFormat = new AudioFormat(44100,16,1,true,true);
            Line.Info sourceDataLineInfo = new DataLine.Info(SourceDataLine.class,audioFormat);
            SourceDataLine sourceDataLine =
            (SourceDataLine) AudioSystem.getLine(sourceDataLineInfo);
            System.out.println("SourceDataLine: "+sourceDataLine);
            sourceDataLine.open(audioFormat, bufferSize);
            sourceDataLine.start();
            Line.Info targetDataLineInfo =
            new DataLine.Info(TargetDataLine.class,audioFormat);
            TargetDataLine targetDataLine =
            (TargetDataLine) AudioSystem.getLine(targetDataLineInfo);
            System.out.println("TargetDataLine: "+targetDataLine);
            targetDataLine.open(audioFormat, bufferSize);
            targetDataLine.start();
            byte[] data = new byte[bufferSize];

            // execute for 20 seconds
            float bufferTime = (((float) data.length) / audioFormat.getFrameSize()) / audioFormat.getFrameRate();
            int count = (int) (20.0f / bufferTime);
            System.out.println("Buffer time: "+(bufferTime * 1000)+" millis. "+count+" iterations.");
            for (int i = 0; i < count; i++) {
                int cnt = targetDataLine.read(data,0,data.length);
                sourceDataLine.write(data,0,cnt);
                assert cnt == data.length;
            }
            System.out.println("Successfully recorded/played "+count+" buffers. Passed");
        } catch(LineUnavailableException lue) {
            System.out.println("Audio hardware is not available!");
            lue.printStackTrace();
            System.out.println("Cannot execute test. NOT failed.");
        } catch(IllegalArgumentException iae) {
            System.out.println("No audio hardware is installed!");
            iae.printStackTrace();
            System.out.println("Test system not correctly setup.");
            System.out.println("Cannot execute test. NOT failed.");
        } catch(Exception e) {
            System.out.println("Unexpected Exception: "+e);
            e.printStackTrace();
            System.out.println("Cannot execute test. NOT failed.");
        }
    }
}
