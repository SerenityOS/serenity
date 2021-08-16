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
   @summary Test AudioFloatInputStream read method
   @modules java.desktop/com.sun.media.sound
*/

import java.io.*;

import javax.sound.sampled.*;

import com.sun.media.sound.*;

public class Read {

    static float[] test_float_array;
    static byte[] test_byte_array;
    static AudioFormat format = new AudioFormat(44100, 16, 1, true, false);

    static AudioFloatInputStream getStream1()
    {
        return AudioFloatInputStream.getInputStream(format, test_byte_array, 0, test_byte_array.length);
    }

    static AudioFloatInputStream getStream2()
    {
        AudioInputStream strm = new AudioInputStream(new ByteArrayInputStream(test_byte_array), format, 1024);
        return AudioFloatInputStream.getInputStream(strm);
    }

    static void setUp() throws Exception {
        test_float_array = new float[1024];
        test_byte_array = new byte[1024*format.getFrameSize()];
        for (int i = 0; i < 1024; i++) {
            double ii = i / 1024.0;
            ii = ii * ii;
            test_float_array[i] = (float)Math.sin(10*ii*2*Math.PI);
            test_float_array[i] += (float)Math.sin(1.731 + 2*ii*2*Math.PI);
            test_float_array[i] += (float)Math.sin(0.231 + 6.3*ii*2*Math.PI);
            test_float_array[i] *= 0.3;
        }
        AudioFloatConverter.getConverter(format).toByteArray(test_float_array, test_byte_array);
    }

    public static void main(String[] args) throws Exception {
        setUp();

        for (int i = 0; i < 2; i++) {
            AudioFloatInputStream stream = null;
            if(i == 0) stream = getStream1();
            if(i == 1) stream = getStream2();
            float v = 0;
            stream.skip(512);
            v = stream.read();
            if(!(Math.abs(v - test_float_array[512]) < 0.0001))
            {
                throw new RuntimeException("Read returned unexpected value.");
            }
        }
    }

}
