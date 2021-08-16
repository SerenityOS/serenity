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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

/**
 * @test
 * @bug 4883060
 * @summary AudioSystem.getAudioFileTypes returns duplicates
 */
public class AudioFileTypeUniqueness {

    public static void main(String[] args) throws Exception {
        boolean foundDuplicates = false;
        AudioFileFormat.Type[]  aTypes = AudioSystem.getAudioFileTypes();
        for (int i = 0; i < aTypes.length; i++)
        {
            for (int j = 0; j < aTypes.length; j++)
            {
                if (aTypes[i].equals(aTypes[j]) && i != j) {
                    foundDuplicates = true;
                }
            }
        }
        if (foundDuplicates) {
            throw new Exception("Test failed");
        } else {
            System.out.println("Test passed");
        }
    }
}
