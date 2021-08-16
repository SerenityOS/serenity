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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * @test
 * @bug 4714846
 * @summary JavaSound ULAW (8-bit) encoder erroneously depends on endian-ness
 */
public class AlawUlaw {
    static ByteArrayInputStream in;
    static int byteLength = 1000;

    static boolean failed = false;

    public static void main(String[] args) throws Exception {
        // generate some random data
        byte[] soundData = new byte[byteLength];
        for (int i=0; i<soundData.length; i++) {
            soundData[i] = (byte) ((i % 256) - 128);
        }

        // create an AudioInputStream from it
        in = new ByteArrayInputStream(soundData);
        in.mark(1);

        test1(PCM_FORMAT_1, ULAW_FORMAT_1, ULAW_FORMAT_2);
        test1(PCM_FORMAT_2, ULAW_FORMAT_1, ULAW_FORMAT_2);
        test2(ULAW_FORMAT_1, ULAW_FORMAT_2, PCM_FORMAT_1);
        test2(ULAW_FORMAT_1, ULAW_FORMAT_2, PCM_FORMAT_2);

        test1(PCM_FORMAT_1, ALAW_FORMAT_1, ALAW_FORMAT_2);
        test1(PCM_FORMAT_2, ALAW_FORMAT_1, ALAW_FORMAT_2);
        test2(ALAW_FORMAT_1, ALAW_FORMAT_2, PCM_FORMAT_1);
        test2(ALAW_FORMAT_1, ALAW_FORMAT_2, PCM_FORMAT_2);

        if (failed) {
                throw new Exception("Test failed!");
        }
    }

    public static String printFormat(AudioFormat format) {
        return format.toString()+"  "+(format.isBigEndian()?"big":"little")+" endian";
    }


    public static void test1(AudioFormat inFormat, AudioFormat outFormat1, AudioFormat outFormat2) throws Exception {
        AudioInputStream inStream = new AudioInputStream(in, inFormat, -1);
        System.out.println("Input Format: " + printFormat(inStream.getFormat()));

        // get a converted stream
        AudioInputStream stream1 = AudioSystem.getAudioInputStream(outFormat1, inStream);
        System.out.println("Output Format 1: " + printFormat(stream1.getFormat()));

        // get a converted stream in big endian ulaw
        AudioInputStream stream2 = AudioSystem.getAudioInputStream(outFormat2, inStream);
        System.out.println("Output Format 2: " + printFormat(stream2.getFormat()));

        compareStreams(stream1, stream2);
    }

    public static void test2(AudioFormat inFormat1, AudioFormat inFormat2, AudioFormat outFormat) throws Exception {
        AudioInputStream inStream1 = new AudioInputStream(in, inFormat1, -1);
        System.out.println("Input Format1: " + printFormat(inStream1.getFormat()));

        // get a converted stream
        AudioInputStream stream1 = AudioSystem.getAudioInputStream(outFormat, inStream1);
        System.out.println("Output Format 1: " + printFormat(stream1.getFormat()));

        AudioInputStream inStream2 = new AudioInputStream(in, inFormat2, -1);
        System.out.println("Input Format1: " + printFormat(inStream2.getFormat()));

        // get a converted stream in big endian ulaw
        AudioInputStream stream2 = AudioSystem.getAudioInputStream(outFormat, inStream2);
        System.out.println("Output Format 2: " + printFormat(stream2.getFormat()));

        compareStreams(stream1, stream2);
    }

    public static void compareStreams(InputStream stream1, InputStream stream2) throws Exception {
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();

        in.reset();
        writeDirectly(stream1, baos1);
        in.reset();
        writeDirectly(stream2, baos2);

        if (baos1.size() != baos2.size()) {
            System.out.println("   stream1 has length = "+baos1.size()+", stream2 has length = "+baos2.size());
        }
        int len = baos1.size();
        if (len > baos2.size()) {
                len = baos2.size();
        }
        byte[] data1=baos1.toByteArray();
        byte[] data2=baos2.toByteArray();
        for (int i=0; i<len; i++) {
                if (data1[i] != data2[i]) {
                        System.out.println("  FAILED! Difference encountered at position "+i);
                        failed = true;
                        return;
                }
        }
        if (baos1.size() != baos2.size()) {
                System.out.println("  No difference, but different length!");
                failed = true;
                return;
        }
        System.out.println("   PASSED");
    }

    public static void writeDirectly(InputStream in, OutputStream out) throws Exception {
            // read data from the stream until we reach the end of the stream
            byte tmp[] = new byte[16384];
            while (true) {
                int bytesRead = in.read(tmp, 0, tmp.length);
                if (bytesRead == -1) {
                        break;
                }
                out.write(tmp, 0, bytesRead);
            } // while
    }

    public static final AudioFormat PCM_FORMAT_1 =
        new AudioFormat( AudioFormat.Encoding.PCM_SIGNED,
                         8000f, //sample rate
                         16, //bits per sample
                         1, //channels
                         2, //frame size
                         8000f, // frame rate
                         false); //isBigEndian
    public static final AudioFormat PCM_FORMAT_2 =
        new AudioFormat( AudioFormat.Encoding.PCM_SIGNED,
                         8000f, //sample rate
                         16, //bits per sample
                         1, //channels
                         2, //frame size
                         8000f, // frame rate
                         true); //isBigEndian

    public static final AudioFormat ULAW_FORMAT_1 =
        new AudioFormat( AudioFormat.Encoding.ULAW,
                         8000f, //sample rate
                         8, //bits per sample
                         1, //channels
                         1, //frame size
                         8000f, // frame rate
                         false); //isBigEndian

    public static final AudioFormat ULAW_FORMAT_2 =
        new AudioFormat( AudioFormat.Encoding.ULAW,
                         8000f, //sample rate
                         8, //bits per sample
                         1, //channels
                         1, //frame size
                         8000f, // frame rate
                         true); //isBigEndian

    public static final AudioFormat ALAW_FORMAT_1 =
        new AudioFormat( AudioFormat.Encoding.ALAW,
                         8000f, //sample rate
                         8, //bits per sample
                         1, //channels
                         1, //frame size
                         8000f, // frame rate
                         false); //isBigEndian

    public static final AudioFormat ALAW_FORMAT_2 =
        new AudioFormat( AudioFormat.Encoding.ALAW,
                         8000f, //sample rate
                         8, //bits per sample
                         1, //channels
                         1, //frame size
                         8000f, // frame rate
                         true); //isBigEndian
}
