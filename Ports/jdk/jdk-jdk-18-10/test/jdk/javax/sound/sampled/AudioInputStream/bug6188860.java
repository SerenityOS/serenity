/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * @test
 * @bug 6188860
 * @summary Tests that method AudioInputStream.read() returns right value
 */
public class bug6188860 {

    public static void main(String[] args) throws Exception {
        byte[] testData = new byte[256];

        // fill data
        for (int i = 0; i < testData.length; i++)
            testData[i] = (byte) (i % 128);

        InputStream streamSrc = new TestInputStream(testData);
        AudioFormat format = new AudioFormat(44100.0f, 8, 1, false, false); // frameSize = 1
        AudioInputStream streamAudio = new AudioInputStream(streamSrc, format, AudioSystem.NOT_SPECIFIED);

        int nErrCount = 0;
        int nTotal = 0;

        int dataSrc, dataRead;
        while (nTotal < (testData.length - 1)) {
            dataRead = streamAudio.read();
            if (dataRead < 0) {
                System.out.println("end of stream");
                break;
            }

            dataSrc = testData[nTotal];

            if (dataRead != dataSrc) {
                System.out.println("" + nTotal + " - mismatch :" + dataRead + " <> " + dataSrc);
                nErrCount++;
            }
            nTotal++;
        }

        System.out.println("Total: " + nTotal + "; Mismatches: " + nErrCount);

        if (nErrCount > 0) {
            throw new RuntimeException("test failed: " + nErrCount + " mismatches of total " + nTotal + " bytes.");
        }
        System.out.println("Test sucessfully passed.");
    }


    static class TestInputStream extends InputStream {
        byte[] data;
        int pos = 0;

        TestInputStream(byte[] data) {
            this.data = data;
        }

        public int read() throws IOException {
            if (pos >= data.length) {
                return -1;
            }
            return data[pos++] & 0xFF;
        }
    }

}
