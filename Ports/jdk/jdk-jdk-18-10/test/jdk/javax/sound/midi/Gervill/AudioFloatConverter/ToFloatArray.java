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
   @summary Test AudioFloatConverter toFloatArray method
   @modules java.desktop/com.sun.media.sound
*/

import javax.sound.sampled.*;

import com.sun.media.sound.*;

public class ToFloatArray {

    public static void main(String[] args) throws Exception {
        float[] testarray = new float[1024];
        for (int i = 0; i < 1024; i++) {
            double ii = i / 1024.0;
            ii = ii * ii;
            testarray[i] = (float)Math.sin(10*ii*2*Math.PI);
            testarray[i] += (float)Math.sin(1.731 + 2*ii*2*Math.PI);
            testarray[i] += (float)Math.sin(0.231 + 6.3*ii*2*Math.PI);
            testarray[i] *= 0.3;
        }

        // Check conversion using PCM_FLOAT
        for (int big = 0; big < 2; big+=1)
        for (int bits = 32; bits <= 64; bits+=32) {
            AudioFormat frm = new AudioFormat(
                    AudioFormat.Encoding.PCM_FLOAT,
                    44100, bits, 1, bits/8,
                    44100, big==1);
            byte[] buff = new byte[testarray.length * frm.getFrameSize()];
            float[] testarray2 = new float[testarray.length];
            AudioFloatConverter conv = AudioFloatConverter.getConverter(frm);
            conv.toByteArray(testarray, buff);
            conv.toFloatArray(buff, testarray2);
            for (int i = 0; i < testarray2.length; i++) {
                if(Math.abs(testarray[i] - testarray2[i]) > 0.05)
                    throw new RuntimeException("Conversion failed for " + frm  +" , arrays not equal enough!\n");
            }
        }

        // Check conversion from float2byte and byte2float.
        for (int big = 0; big < 2; big+=1)
        for (int signed = 0; signed < 2; signed+=1)
        for (int bits = 6; bits <= 40; bits+=2) {
            AudioFormat frm = new AudioFormat(44100, bits, 1, signed==1, big==1);
            byte[] buff = new byte[testarray.length * frm.getFrameSize()];
            float[] testarray2 = new float[testarray.length];
            AudioFloatConverter conv = AudioFloatConverter.getConverter(frm);
            conv.toByteArray(testarray, buff);
            conv.toFloatArray(buff, testarray2);
            for (int i = 0; i < testarray2.length; i++) {
                if(Math.abs(testarray[i] - testarray2[i]) > 0.05)
                    throw new RuntimeException("Conversion failed for " + frm  +" , arrays not equal enough!\n");
            }
        }

        // Check big/little
        for (int big = 0; big < 2; big+=1)
        for (int signed = 0; signed < 2; signed+=1)
        for (int bits = 6; bits <= 40; bits+=2) {
            AudioFormat frm = new AudioFormat(44100, bits, 1, signed==1, big==1);
            byte[] buff = new byte[testarray.length * frm.getFrameSize()];
            AudioFloatConverter conv = AudioFloatConverter.getConverter(frm);
            conv.toByteArray(testarray, buff);
            byte[] buff2 = new byte[testarray.length * frm.getFrameSize()];
            int fs = frm.getFrameSize();
            for (int i = 0; i < buff2.length; i+=fs) {
                for (int j = 0; j < fs; j++) {
                    buff2[i+(fs-j-1)] = buff[i+j];
                }
            }
            float[] testarray2 = new float[testarray.length];
            AudioFormat frm2 = new AudioFormat(44100, bits, 1, signed==1, big==0);
            AudioFloatConverter.getConverter(frm2).toFloatArray(buff2, testarray2);
            for (int i = 0; i < testarray2.length; i++) {
                if(Math.abs(testarray[i] - testarray2[i]) > 0.05)
                {
                    throw new RuntimeException("Conversion failed for " + frm  +" to " + frm2 + " , arrays not equal enough!\n");
                }
            }
        }

        // Check signed/unsigned
        for (int big = 0; big < 2; big+=1)
        for (int signed = 0; signed < 2; signed+=1)
        for (int bits = 6; bits <= 40; bits+=2) {
            AudioFormat frm = new AudioFormat(44100, bits, 1, signed==1, big==1);
            byte[] b = new byte[testarray.length * frm.getFrameSize()];
            AudioFloatConverter conv = AudioFloatConverter.getConverter(frm);
            conv.toByteArray(testarray, b);
            int fs = frm.getFrameSize();
            if(big==1)
            {
                for(int i=0; i < b.length; i+= fs )
                    b[i] = (b[i] >= 0) ? (byte)(0x80 | b[i]) : (byte)(0x7F & b[i]);
            }
            else
            {
                for(int i=(0+fs-1); i < b.length; i+= fs )
                    b[i] = (b[i] >= 0) ? (byte)(0x80 | b[i]) : (byte)(0x7F & b[i]);
            }
            float[] testarray2 = new float[testarray.length];
            AudioFormat frm2 = new AudioFormat(44100, bits, 1, signed==0, big==1);
            AudioFloatConverter.getConverter(frm2).toFloatArray(b, testarray2);
            for (int i = 0; i < testarray2.length; i++) {
                if(Math.abs(testarray[i] - testarray2[i]) > 0.05)
                {
                    throw new RuntimeException("Conversion failed for " + frm  +" to " + frm2 + " , arrays not equal enough!\n");
                }
            }
        }

        // Check if conversion 32->24, 24->16, 16->8 result in same float data
        AudioFormat frm = new AudioFormat(44100, 40, 1, true, true);
        byte[] b = new byte[testarray.length * frm.getFrameSize()];
        AudioFloatConverter.getConverter(frm).toByteArray(testarray, b);
        for (int bits = 6; bits <= 40; bits+=2) {
            AudioFormat frm2 = new AudioFormat(44100, bits, 1, true, true);
            byte[] b2 = new byte[testarray.length * frm2.getFrameSize()];
            int fs1 = frm.getFrameSize();
            int fs2 = frm2.getFrameSize();
            int ii = 0;
            for (int i = 0; i < b.length; i+=fs1)
                for (int j = 0; j < fs2; j++)
                    b2[ii++] = b[i+j];
            float[] testarray2 = new float[testarray.length];
            AudioFloatConverter.getConverter(frm2).toFloatArray(b2, testarray2);
            for (int i = 0; i < testarray2.length; i++) {
                if(Math.abs(testarray[i] - testarray2[i]) > 0.05)
                {
                    throw new RuntimeException("Conversion failed for " + frm  +" to " + frm2 + " , arrays not equal enough!\n");
                }
            }
        }
    }

}
