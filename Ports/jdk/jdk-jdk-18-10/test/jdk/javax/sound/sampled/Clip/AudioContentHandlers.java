/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.applet.AudioClip;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import static javax.sound.sampled.AudioFileFormat.Type.AIFC;
import static javax.sound.sampled.AudioFileFormat.Type.AIFF;
import static javax.sound.sampled.AudioFileFormat.Type.AU;
import static javax.sound.sampled.AudioFileFormat.Type.SND;
import static javax.sound.sampled.AudioFileFormat.Type.WAVE;

/**
 * @test
 * @bug 8204454
 * @summary URL.getContent() should return AudioClip for supported formats
 * @run main/othervm -mx128m AudioContentHandlers
 */
public final class AudioContentHandlers {

    private static final List<AudioFormat> formats = new ArrayList<>();

    private static final AudioFormat.Encoding[] encodings =
            {AudioFormat.Encoding.ALAW, AudioFormat.Encoding.ULAW,
                    AudioFormat.Encoding.PCM_SIGNED,
                    AudioFormat.Encoding.PCM_UNSIGNED,
                    AudioFormat.Encoding.PCM_FLOAT};

    private static final AudioFileFormat.Type[] types =
            {WAVE, AU, AIFF, AIFC, SND};

    static {
        for (final AudioFormat.Encoding enc : encodings) {
            formats.add(new AudioFormat(enc, 44100, 8, 1, 1, 44100, true));
            formats.add(new AudioFormat(enc, 44100, 8, 1, 1, 44100, false));
        }
    }

    public static void main(final String[] args) throws Exception {
        for (final AudioFileFormat.Type type : types) {
            for (final AudioFormat format : formats) {
                File file = new File("audio." + type.getExtension());
                try {
                    AudioSystem.write(getStream(format), type, file);
                } catch (IOException | IllegalArgumentException ignored) {
                    continue;
                }
                AudioClip content;
                try {
                    content = (AudioClip) file.toURL().getContent();
                    // We need to generate OOM because the stream in AudioClip
                    // will be closed in finalize().
                    generateOOME();
                } finally {
                    Files.delete(file.toPath());
                }
                if (content == null) {
                    throw new RuntimeException("Content is null");
                }
            }
        }
    }

    private static AudioInputStream getStream(final AudioFormat format) {
        final InputStream in = new ByteArrayInputStream(new byte[100]);
        return new AudioInputStream(in, format, 10);
    }

    private static void generateOOME() throws Exception {
        List<Object> leak = new LinkedList<>();
        try {
            while (true) {
                leak.add(new byte[1024 * 1024]);
            }
        } catch (OutOfMemoryError ignored) {
        }
        // Give the GC a chance at that weakref in case of slow systems
        Thread.sleep(2000);
    }
}
