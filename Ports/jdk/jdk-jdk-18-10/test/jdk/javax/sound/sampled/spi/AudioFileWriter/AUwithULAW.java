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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * @test
 * @bug 4391108
 * @summary Writing au files with ulaw encoding is broken
 */
public class AUwithULAW {
    public static void main(String args[]) throws Exception {
        System.out.println();
        System.out.println();
        System.out.println("4391108: Writing au files with ulaw encoding is broken");
        byte[] fakedata=new byte[1234];
        InputStream is = new ByteArrayInputStream(fakedata);
        AudioFormat inFormat = new AudioFormat(AudioFormat.Encoding.ULAW, 8000, 8, 1, 1, 8000, false);

        AudioInputStream ais = new AudioInputStream(is, inFormat, fakedata.length);

        ByteArrayOutputStream out = new ByteArrayOutputStream(1500);
        System.out.println("  ulaw data will be written as AU to stream...");
        int t = AudioSystem.write(ais, AudioFileFormat.Type.AU, out);
        byte[] writtenData = out.toByteArray();
        is = new ByteArrayInputStream(writtenData);
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
        System.out.println("  test passed.");
    }
}
