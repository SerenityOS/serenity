/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.sound.midi.spi;

import java.util.Arrays;

import javax.sound.midi.MidiDevice;

/**
 * A {@code MidiDeviceProvider} is a factory or provider for a particular type
 * of MIDI device. This mechanism allows the implementation to determine how
 * resources are managed in the creation and management of a device.
 *
 * @author Kara Kytle
 */
public abstract class MidiDeviceProvider {

    /**
     * Constructor for subclasses to call.
     */
    protected MidiDeviceProvider() {}

    /**
     * Indicates whether the device provider supports the device represented by
     * the specified device info object.
     *
     * @param  info an info object that describes the device for which support
     *         is queried
     * @return {@code true} if the specified device is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code info} is {@code null}
     */
    public boolean isDeviceSupported(final MidiDevice.Info info) {
        return Arrays.stream(getDeviceInfo()).anyMatch(info::equals);
    }

    /**
     * Obtains the set of info objects representing the device or devices
     * provided by this {@code MidiDeviceProvider}.
     *
     * @return set of device info objects
     */
    public abstract MidiDevice.Info[] getDeviceInfo();

    /**
     * Obtains an instance of the device represented by the info object.
     *
     * @param  info an info object that describes the desired device
     * @return device instance
     * @throws IllegalArgumentException if the info object specified does not
     *         match the info object for a device supported by this
     *         {@code MidiDeviceProvider}
     * @throws NullPointerException if {@code info} is {@code null}
     */
    public abstract MidiDevice getDevice(MidiDevice.Info info);
}
