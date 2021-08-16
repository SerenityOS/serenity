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

import java.io.ByteArrayInputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * @test
 * @bug 4940459
 * @summary AudioInputStream.getFrameLength() returns 0 instead of NOT_SPECIFIED
 */
public class AuNotSpecified {
    public static boolean failed = false;

    public static void main(String[] params) throws Exception {

        AudioInputStream is =
            AudioSystem.getAudioInputStream(new
                                            ByteArrayInputStream(new byte[] {
                                                (byte)0x2E, (byte)0x73, (byte)0x6E, (byte)0x64, (byte)0x00,
                                                (byte)0x00, (byte)0x00, (byte)0x18,
                                                (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00,
                                                (byte)0x00, (byte)0x00, (byte)0x03,
                                                (byte)0x00, (byte)0x00, (byte)0x1F, (byte)0x40, (byte)0x00,
                                                (byte)0x00, (byte)0x00, (byte)0x01,
                                                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                                                (byte)0x00, (byte)0x00, (byte)0x00,
                                            }));
        if (is.getFrameLength() != AudioSystem.NOT_SPECIFIED) {
            System.out.println("frame length should be NOT_SPECIFIED, but is: "+is.getFrameLength());
            failed=true;
        }
        //assertTrue(is.getFrameLength() == AudioSystem.NOT_SPECIFIED);
        //assertTrue(is.read(new byte[8]) == 8);
        //assertTrue(is.read(new byte[2]) == -1);
        if (failed) throw new Exception("Test FAILED!");
        System.out.println("Test Passed.");
    }
}
