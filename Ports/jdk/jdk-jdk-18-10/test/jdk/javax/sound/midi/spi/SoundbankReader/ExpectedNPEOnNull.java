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

import javax.sound.midi.MidiSystem;
import javax.sound.midi.spi.SoundbankReader;

import static java.util.ServiceLoader.load;

/**
 * @test
 * @bug 8143909
 * @author Sergey Bylokhov
 */
public final class ExpectedNPEOnNull {

    public static void main(final String[] args) throws Exception {
        testMS();
        for (final SoundbankReader sbr : load(SoundbankReader.class)) {
            testSBR(sbr);
        }
    }

    /**
     * Tests the part of MidiSystem API, which implemented via SoundbankReader.
     */
    private static void testMS() throws Exception {
        // MidiSystem#getSoundbank(InputStream)
        try {
            MidiSystem.getSoundbank((InputStream) null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }
        // MidiSystem#getSoundbank(URL)
        try {
            MidiSystem.getSoundbank((URL) null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }
        // MidiSystem#getSoundbank(File)
        try {
            MidiSystem.getSoundbank((File) null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }
    }

    /**
     * Tests the SoundbankReader API directly.
     */
    private static void testSBR(final SoundbankReader sbr) throws Exception {
        // SoundbankReader#getSoundbank(InputStream)
        try {
            sbr.getSoundbank((InputStream) null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }
        // SoundbankReader#getSoundbank(URL)
        try {
            sbr.getSoundbank((URL) null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }
        // SoundbankReader#getSoundbank(File)
        try {
            sbr.getSoundbank((File) null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }
    }
}
