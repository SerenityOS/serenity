/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.media.sound;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDeviceTransmitter;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

/**
 * Helper class which allows to convert {@code Transmitter}
 * to {@code MidiDeviceTransmitter}.
 *
 * @author Alex Menkov
 */
public final class MidiDeviceTransmitterEnvelope implements MidiDeviceTransmitter {

    private final MidiDevice device;
    private final Transmitter transmitter;

    /**
     * Creates a new {@code MidiDeviceTransmitterEnvelope} object which
     * envelops the specified {@code Transmitter}
     * and is owned by the specified {@code MidiDevice}.
     *
     * @param device the owner {@code MidiDevice}
     * @param transmitter the {@code Transmitter} to be enveloped
     */
    public MidiDeviceTransmitterEnvelope(MidiDevice device, Transmitter transmitter) {
        if (device == null || transmitter == null) {
            throw new NullPointerException();
        }
        this.device = device;
        this.transmitter = transmitter;
    }

    // Transmitter implementation
    @Override
    public void setReceiver(Receiver receiver) {
        transmitter.setReceiver(receiver);
    }

    @Override
    public Receiver getReceiver() {
        return transmitter.getReceiver();
    }

    @Override
    public void close() {
        transmitter.close();
    }

    // MidiDeviceReceiver implementation
    @Override
    public MidiDevice getMidiDevice() {
        return device;
    }

    /**
     * Obtains the transmitter enveloped
     * by this {@code MidiDeviceTransmitterEnvelope} object.
     *
     * @return the enveloped transmitter
     */
    public Transmitter getTransmitter() {
        return transmitter;
    }
}
