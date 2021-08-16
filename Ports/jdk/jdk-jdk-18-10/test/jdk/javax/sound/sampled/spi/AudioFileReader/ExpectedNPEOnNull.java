/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.AudioFileReader;

import static java.util.ServiceLoader.load;

/**
 * @test
 * @bug 8135100
 * @author Sergey Bylokhov
 */
public final class ExpectedNPEOnNull {

    public static void main(final String[] args) throws Exception {
        testAS();
        testAFR();
    }

    /**
     * Tests the part of AudioSystem API, which implemented via AudioFileReader.
     */
    private static void testAS() throws Exception {

        try {
            AudioSystem.getAudioFileFormat((InputStream) null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }
        try {
            AudioSystem.getAudioFileFormat((URL) null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }
        try {
            AudioSystem.getAudioFileFormat((File) null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }
        try {
            AudioSystem.getAudioInputStream((InputStream) null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }
        try {
            AudioSystem.getAudioInputStream((URL) null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }
        try {
            AudioSystem.getAudioInputStream((File) null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }
    }

    /**
     * Tests the AudioFileReader API directly.
     */
    private static void testAFR() throws Exception {

        for (final AudioFileReader afr : load(AudioFileReader.class)) {
            try {
                afr.getAudioFileFormat((InputStream) null);
                throw new RuntimeException("NPE is expected: " + afr);
            } catch (final NullPointerException ignored) {
            }
            try {
                afr.getAudioFileFormat((URL) null);
                throw new RuntimeException("NPE is expected: " + afr);
            } catch (final NullPointerException ignored) {
            }
            try {
                afr.getAudioFileFormat((File) null);
                throw new RuntimeException("NPE is expected: " + afr);
            } catch (final NullPointerException ignored) {
            }
            try {
                afr.getAudioInputStream((InputStream) null);
                throw new RuntimeException("NPE is expected: " + afr);
            } catch (final NullPointerException ignored) {
            }
            try {
                afr.getAudioInputStream((URL) null);
                throw new RuntimeException("NPE is expected: " + afr);
            } catch (final NullPointerException ignored) {
            }
            try {
                afr.getAudioInputStream((File) null);
                throw new RuntimeException("NPE is expected: " + afr);
            } catch (final NullPointerException ignored) {
            }
        }
    }
}
