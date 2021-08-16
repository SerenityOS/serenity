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

import javax.sound.midi.MidiSystem;

/**
 * @test
 * @bug 4303933
 * @summary MidiSystem fails to load MIDI file on systems with EBCDIC simulation
 */
public class SMFCp037 {

    public static void main(String args[]) throws Exception {
        // Test to read MIDI files with Cp037 character set - close enough
        // for EBCDIC simulation
        System.setProperty("file.encoding", "Cp037");
        // try to read this file with Cp037 encoding
        MidiSystem.getSequence(new ByteArrayInputStream(SHORT_SMF));
        System.out.println("  test passed.");
    }

public static byte[] SHORT_SMF = {
        77, 84, 104, 100, 0, 0, 0, 6, 0, 1, 0, 2, 0, 120, 77, 84, 114, 107, 0, 0,
        0, 27, 0, -1, 3, 19, 77, 73, 68, 73, 32, 116, 101, 115, 116, 32, 45, 32,
        116, 114, 97, 99, 107, 32, 48, 0, -1, 47, 0, 77, 84, 114, 107, 0, 0, 0, -44,
        0, -1, 3, 19, 77, 73, 68, 73, 32, 116, 101, 115, 116, 32, 45, 32, 116, 114,
        97, 99, 107, 32, 49, 0, -64, 30, 0, -112, 68, 126, 0, -32, 6, 67, 0, 14,
        71, 0, 20, 74, 0, 26, 77, 0, 32, 80, 0, 42, 85, 6, 50, 89, 6, 56, 92, 5,
        66, 97, 6, 74, 101, 6, 80, 104, 11, 84, 106, 20, 76, 102, 6, 70, 99, 5, 60,
        94, 6, 52, 90, 5, 44, 86, 4, 34, 81, 5, 26, 77, 5, 20, 74, 6, 10, 69, 5,
        2, 65, 7, 0, 64, 42, -112, 66, 123, 11, 68, 0, 72, 63, 126, 4, 66, 0, 43,
        -32, 0, 63, 6, 0, 60, 7, 0, 56, 6, 0, 53, 5, 0, 49, 5, 0, 43, 4, 0, 37, 3,
        0, 30, 3, 0, 25, 3, 0, 19, 3, 0, 13, 4, 0, 8, 4, 0, 2, 4, 0, 0, 70, 0, 3,
        5, 0, 9, 3, 0, 14, 7, 0, 16, 25, 0, 21, 5, 0, 25, 7, 0, 28, 5, 0, 32, 5,
        0, 36, 5, 0, 41, 6, 0, 46, 5, 0, 50, 5, 0, 53, 4, 0, 58, 7, 0, 61, 7, 0,
        64, 117, -112, 63, 0, 0, -1, 47, 0
    };
}
