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

import java.util.Objects;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.spi.MidiDeviceProvider;

import static java.util.ServiceLoader.load;

/**
 * @test
 * @bug 8143909
 * @author Sergey Bylokhov
 */
public final class ExpectedNPEOnNull {

    public static void main(final String[] args) throws Exception {
        testMS();
        for (final MidiDeviceProvider mdp : load(MidiDeviceProvider.class)) {
            testMDP(mdp);
        }
        testMDP(customMDP);
    }

    /**
     * Tests the part of MidiSystem API, which implemented via
     * MidiDeviceProvider.
     */
    private static void testMS() throws Exception {
        // MidiSystem#getMidiDevice(MidiDevice.Info)
        try {
            MidiSystem.getMidiDevice(null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }
    }

    /**
     * Tests the MidiDeviceProvider API directly.
     */
    private static void testMDP(final MidiDeviceProvider mdp) throws Exception {
        // MidiDeviceProvider#isDeviceSupported(Info)
        try {
            mdp.isDeviceSupported(null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }
        // MidiDeviceProvider#getDevice(Info)
        try {
            mdp.getDevice(null);
            throw new RuntimeException("NPE is expected");
        } catch (final NullPointerException ignored) {
        }
    }

    /**
     * Tests some default implementation of MidiDeviceProvider API, using the
     * custom {@code MidiDeviceProvider}, which support nothing.
     */
    static MidiDeviceProvider customMDP = new MidiDeviceProvider() {
        @Override
        public MidiDevice.Info[] getDeviceInfo() {
            return new MidiDevice.Info[0];
        }

        @Override
        public MidiDevice getDevice(MidiDevice.Info info) {
            Objects.requireNonNull(info);
            return null;
        }
    };
}
