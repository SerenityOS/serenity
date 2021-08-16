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
import javax.sound.midi.MidiDeviceReceiver;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

/**
 * Helper class which allows to convert {@code Receiver}
 * to {@code MidiDeviceReceiver}.
 *
 * @author Alex Menkov
 */
public final class MidiDeviceReceiverEnvelope implements MidiDeviceReceiver {

    private final MidiDevice device;
    private final Receiver receiver;

    /**
     * Creates a new {@code MidiDeviceReceiverEnvelope} object which
     * envelops the specified {@code Receiver}
     * and is owned by the specified {@code MidiDevice}.
     *
     * @param device the owner {@code MidiDevice}
     * @param receiver the {@code Receiver} to be enveloped
     */
    public MidiDeviceReceiverEnvelope(MidiDevice device, Receiver receiver) {
        if (device == null || receiver == null) {
            throw new NullPointerException();
        }
        this.device = device;
        this.receiver = receiver;
    }

    // Receiver implementation
    @Override
    public void close() {
        receiver.close();
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        receiver.send(message, timeStamp);
    }

    // MidiDeviceReceiver implementation
    @Override
    public MidiDevice getMidiDevice() {
        return device;
    }

    /**
     * Obtains the receiver enveloped
     * by this {@code MidiDeviceReceiverEnvelope} object.
     *
     * @return the enveloped receiver
     */
    public Receiver getReceiver() {
        return receiver;
    }
}
