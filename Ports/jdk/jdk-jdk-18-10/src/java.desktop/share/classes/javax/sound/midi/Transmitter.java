/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.sound.midi;

/**
 * A {@code Transmitter} sends {@link MidiEvent} objects to one or more
 * {@link Receiver Receivers}. Common MIDI transmitters include sequencers and
 * MIDI input ports.
 *
 * @author Kara Kytle
 * @see Receiver
 */
public interface Transmitter extends AutoCloseable {

    /**
     * Sets the receiver to which this transmitter will deliver MIDI messages.
     * If a receiver is currently set, it is replaced with this one.
     *
     * @param  receiver the desired receiver
     */
    void setReceiver(Receiver receiver);

    /**
     * Obtains the current receiver to which this transmitter will deliver MIDI
     * messages.
     *
     * @return the current receiver. If no receiver is currently set, returns
     *         {@code null}.
     */
    Receiver getReceiver();

    /**
     * Indicates that the application has finished using the transmitter, and
     * that limited resources it requires may be released or made available.
     * <p>
     * If the creation of this {@code Transmitter} resulted in implicitly
     * opening the underlying device, the device is implicitly closed by this
     * method. This is true unless the device is kept open by other
     * {@code Receiver} or {@code Transmitter} instances that opened the device
     * implicitly, and unless the device has been opened explicitly. If the
     * device this {@code Transmitter} is retrieved from is closed explicitly by
     * calling {@link MidiDevice#close MidiDevice.close}, the
     * {@code Transmitter} is closed, too. For a detailed description of
     * open/close behaviour see the class description of
     * {@link MidiDevice MidiDevice}.
     *
     * @see MidiSystem#getTransmitter
     */
    @Override
    void close();
}
