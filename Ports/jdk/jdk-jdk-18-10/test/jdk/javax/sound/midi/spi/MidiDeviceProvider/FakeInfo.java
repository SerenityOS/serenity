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

import java.util.Collection;
import java.util.HashSet;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.spi.MidiDeviceProvider;

import static java.util.ServiceLoader.load;

/**
 * @test
 * @bug 8059743
 * @summary MidiDeviceProvider shouldn't returns incorrect results in case of
 *          some unknown MidiDevice.Info
 * @author Sergey Bylokhov
 */
public final class FakeInfo {

    private static final class Fake extends Info {

        Fake() {
            super("a", "b", "c", "d");
        }
    }

    public static void main(final String[] args) {
        final Info fake = new Fake();
        // MidiSystem API
        try {
            MidiSystem.getMidiDevice(fake);
            throw new RuntimeException("IllegalArgumentException expected");
        } catch (final MidiUnavailableException e) {
            throw new RuntimeException("IllegalArgumentException expected", e);
        } catch (final IllegalArgumentException ignored) {
            // expected
        }
        // MidiDeviceProvider API
        final Collection<String> errors = new HashSet<>();
        for (final MidiDeviceProvider mdp : load(MidiDeviceProvider.class)) {
            try {
                if (mdp.isDeviceSupported(fake)) {
                    throw new RuntimeException("fake is supported");
                }
                final MidiDevice device = mdp.getDevice(fake);
                System.err.println("MidiDevice: " + device);
                throw new RuntimeException("IllegalArgumentException expected");
            } catch (final IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }
        if (errors.size() != 1) {
            throw new RuntimeException("Wrong number of messages:" + errors);
        }
    }
}
