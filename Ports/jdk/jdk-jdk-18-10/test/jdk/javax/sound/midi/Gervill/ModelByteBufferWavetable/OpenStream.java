/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 @summary Test AudioFloatInputStream.getFrameLength() returned from
 ModelByteBufferWavetable openStream method
 @modules java.desktop/com.sun.media.sound
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.sound.sampled.*;

import com.sun.media.sound.*;

public class OpenStream {

    static float[] testarray;

    static byte[] test_byte_array;

    static byte[] test_byte_array_8ext;

    static AudioFormat format = new AudioFormat(44100, 16, 1, true, false);

    static AudioFormat format24 = new AudioFormat(44100, 24, 1, true, false);

    static ModelByteBuffer buffer;

    static ModelByteBuffer buffer_wave;

    static ModelByteBuffer buffer8;

    static ModelByteBuffer buffer16_8;

    static ModelByteBuffer buffer24;

    static File test_file;

    static ModelByteBuffer buffer_wave_ondisk;

    static void setUp() throws Exception {
        testarray = new float[1024];
        for (int i = 0; i < 1024; i++) {
            double ii = i / 1024.0;
            ii = ii * ii;
            testarray[i] = (float) Math.sin(10 * ii * 2 * Math.PI);
            testarray[i] += (float) Math.sin(1.731 + 2 * ii * 2 * Math.PI);
            testarray[i] += (float) Math.sin(0.231 + 6.3 * ii * 2 * Math.PI);
            testarray[i] *= 0.3;
        }
        test_byte_array = new byte[testarray.length * 2];
        AudioFloatConverter.getConverter(format).toByteArray(testarray,
                test_byte_array);
        buffer = new ModelByteBuffer(test_byte_array);

        byte[] test_byte_array2 = new byte[testarray.length * 3];
        buffer24 = new ModelByteBuffer(test_byte_array2);
        test_byte_array_8ext = new byte[testarray.length];
        byte[] test_byte_array_8_16 = new byte[testarray.length * 2];
        AudioFloatConverter.getConverter(format24).toByteArray(testarray,
                test_byte_array2);
        int ix = 0;
        int x = 0;
        for (int i = 0; i < test_byte_array_8ext.length; i++) {
            test_byte_array_8ext[i] = test_byte_array2[ix++];
            test_byte_array_8_16[x++] = test_byte_array2[ix++];
            test_byte_array_8_16[x++] = test_byte_array2[ix++];
        }
        buffer16_8 = new ModelByteBuffer(test_byte_array_8_16);
        buffer8 = new ModelByteBuffer(test_byte_array_8ext);

        AudioInputStream ais = new AudioInputStream(buffer.getInputStream(),
                format, testarray.length);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, baos);
        buffer_wave = new ModelByteBuffer(baos.toByteArray());

        test_file = File.createTempFile("test", ".raw");
        try (FileOutputStream fos = new FileOutputStream(test_file)) {
            fos.write(baos.toByteArray());
        }
        buffer_wave_ondisk = new ModelByteBuffer(test_file);

    }

    static void tearDown() throws Exception {
        Files.delete(Paths.get(test_file.getAbsolutePath()));
    }

    public static void testOpenStream(ModelByteBufferWavetable wavetable)
            throws Exception {
        AudioFloatInputStream ais = wavetable.openStream();
        long frames = wavetable.getBuffer().capacity()
                / wavetable.getFormat().getFrameSize();
        long framelength = ais.getFrameLength();
        ais.close();
        if (frames != framelength) {
            throw new Exception("Incorrect framelength returned (" + frames
                    + " != " + framelength + ")");
        }
    }

    public static void main(String[] args) throws Exception {

        setUp();

        try {
            testOpenStream(new ModelByteBufferWavetable(buffer, format));
            testOpenStream(new ModelByteBufferWavetable(buffer_wave, format));
            testOpenStream(new ModelByteBufferWavetable(buffer_wave_ondisk,
                    format));
        } finally {
            tearDown();
        }

    }

}
