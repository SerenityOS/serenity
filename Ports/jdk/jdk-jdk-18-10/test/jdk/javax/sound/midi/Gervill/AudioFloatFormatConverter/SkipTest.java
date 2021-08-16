/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 @summary Test skip method returned from AudioFloatFormatConverter.getAudioInputStream
 @modules java.desktop/com.sun.media.sound
*/

import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import com.sun.media.sound.AudioFloatFormatConverter;

public class SkipTest {

    public static void main(String[] args) throws Exception {
        AudioFloatFormatConverter converter = new AudioFloatFormatConverter();
        byte[] data = { 10, 20, 30, 40, 30, 20, 10 };
        AudioFormat format = new AudioFormat(8000, 8, 1, true, false);
        AudioFormat format2 = new AudioFormat(16000, 8, 1, true, false);
        AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(
                data), format, data.length);
        AudioInputStream ais2 = converter.getAudioInputStream(format2, ais);
        byte[] data2 = new byte[30];
        int ret = ais2.read(data2, 0, data2.length);
        ais.reset();
        AudioInputStream ais3 = converter.getAudioInputStream(format2, ais);
        byte[] data3 = new byte[100];
        ais3.skip(7);
        int ret2 = ais3.read(data3, 7, data3.length);
        if (ret2 != ret - 7)
            throw new Exception("Skip doesn't work correctly (" + ret2 + " != "
                    + (ret - 7) + ")");
        for (int i = 7; i < ret2 + 7; i++) {
            if (data3[i] != data2[i])
                throw new Exception("Skip doesn't work correctly (" + data3[i]
                        + " != " + data2[i] + ")");
        }
    }

}
