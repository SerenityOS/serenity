/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * @test
 * @bug 4636355
 * @summary Check that RIFF headers are written with extra data length field.
 */
public class RIFFHeader {

    public static void main(String args[]) throws Exception {
        System.out.println();
        System.out.println();
        System.out.println("4636355: Check that RIFF headers are written with extra data length field.");
        byte[] fakedata=new byte[1234];
        MyByteArrayInputStream is = new MyByteArrayInputStream(fakedata);
        AudioFormat inFormat = new AudioFormat(AudioFormat.Encoding.ULAW, 8000, 8, 1, 1, 8000, true);

        AudioInputStream ais = new AudioInputStream((InputStream) is, inFormat, fakedata.length);
        ByteArrayOutputStream out = new ByteArrayOutputStream(1500);
        System.out.println("  ulaw data will be written as WAVE to stream...");
        int t = AudioSystem.write(ais, AudioFileFormat.Type.WAVE, out);
        byte[] writtenData = out.toByteArray();
        // now header must have at least 46 bytes
        System.out.println("  Length should be "+(fakedata.length+46)+" bytes: "+writtenData.length);
        // re-read this file
        is = new MyByteArrayInputStream(writtenData);
        System.out.println("  Get AudioFileFormat of written file");
        AudioFileFormat fileformat = AudioSystem.getAudioFileFormat(is);
        AudioFileFormat.Type type = fileformat.getType();
        System.out.println("  The file format type: "+type);
        if (fileformat.getFrameLength()!=fakedata.length
            && fileformat.getFrameLength()!=AudioSystem.NOT_SPECIFIED) {
            throw new Exception("The written file's frame length is "+fileformat.getFrameLength()+" but should be "+fakedata.length+" !");
        }
        ais = AudioSystem.getAudioInputStream(is);
        System.out.println("  Got Stream with format: "+ais.getFormat());
        if (is.getPos()<46) {
            throw new Exception("After reading the header, stream position must be at least 46, but is "+is.getPos()+" !");
        }
        System.out.println("  test passed.");
    }

    static class MyByteArrayInputStream extends ByteArrayInputStream {

        MyByteArrayInputStream(byte[] data) {
            super(data);
        }

        int getPos() {
            return pos;
        }
    }
}
