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

package javax.sound.midi;

import java.util.List;

/**
 * {@code MidiDevice} is the base interface for all MIDI devices. Common devices
 * include synthesizers, sequencers, MIDI input ports, and MIDI output ports.
 * <p>
 * A {@code MidiDevice} can be a transmitter or a receiver of MIDI events, or
 * both. Therefore, it can provide {@link Transmitter} or {@link Receiver}
 * instances (or both). Typically, MIDI IN ports provide transmitters, MIDI OUT
 * ports and synthesizers provide receivers. A Sequencer typically provides
 * transmitters for playback and receivers for recording.
 * <p>
 * A {@code MidiDevice} can be opened and closed explicitly as well as
 * implicitly. Explicit opening is accomplished by calling {@link #open},
 * explicit closing is done by calling {@link #close} on the {@code MidiDevice}
 * instance. If an application opens a {@code MidiDevice} explicitly, it has to
 * close it explicitly to free system resources and enable the application to
 * exit cleanly. Implicit opening is done by calling
 * {@link MidiSystem#getReceiver} and {@link MidiSystem#getTransmitter}. The
 * {@code MidiDevice} used by {@code MidiSystem.getReceiver} and
 * {@code MidiSystem.getTransmitter} is implementation-dependent unless the
 * properties {@code javax.sound.midi.Receiver} and
 * {@code javax.sound.midi.Transmitter} are used (see the description of
 * properties to select default providers in {@link MidiSystem}). A
 * {@code MidiDevice} that was opened implicitly, is closed implicitly by
 * closing the {@code Receiver} or {@code Transmitter} that resulted in opening
 * it. If more than one implicitly opening {@code Receiver} or
 * {@code Transmitter} were obtained by the application, the device is closed
 * after the last {@code Receiver} or {@code Transmitter} has been closed. On
 * the other hand, calling {@code getReceiver} or {@code getTransmitter} on the
 * device instance directly does not open the device implicitly. Closing these
 * {@code Transmitter}s and {@code Receiver}s does not close the device
 * implicitly. To use a device with {@code Receiver}s or {@code Transmitter}s
 * obtained this way, the device has to be opened and closed explicitly.
 * <p>
 * If implicit and explicit opening and closing are mixed on the same
 * {@code MidiDevice} instance, the following rules apply:
 *
 * <ul>
 *   <li>After an explicit open (either before or after implicit opens), the
 *   device will not be closed by implicit closing. The only way to close an
 *   explicitly opened device is an explicit close.
 *   <li>An explicit close always closes the device, even if it also has been
 *   opened implicitly. A subsequent implicit close has no further effect.
 * </ul>
 *
 * To detect if a MidiDevice represents a hardware MIDI port, the following
 * programming technique can be used:
 *
 * <pre>{@code
 * MidiDevice device = ...;
 * if (!(device instanceof Sequencer) && !(device instanceof Synthesizer)) {
 *   // we're now sure that device represents a MIDI port
 *   // ...
 * }
 * }</pre>
 *
 * <p>
 * A {@code MidiDevice} includes a {@link Info} object to provide manufacturer
 * information and so on.
 *
 * @author Kara Kytle
 * @author Florian Bomers
 * @see Synthesizer
 * @see Sequencer
 * @see Receiver
 * @see Transmitter
 */
public interface MidiDevice extends AutoCloseable {

    /**
     * Obtains information about the device, including its Java class and
     * {@code Strings} containing its name, vendor, and description.
     *
     * @return device info
     */
    Info getDeviceInfo();

    /**
     * Opens the device, indicating that it should now acquire any system
     * resources it requires and become operational.
     * <p>
     * An application opening a device explicitly with this call has to close
     * the device by calling {@link #close}. This is necessary to release system
     * resources and allow applications to exit cleanly.
     * <p>
     * Note that some devices, once closed, cannot be reopened. Attempts to
     * reopen such a device will always result in a
     * {@code MidiUnavailableException}.
     *
     * @throws MidiUnavailableException thrown if the device cannot be opened
     *         due to resource restrictions
     * @throws SecurityException thrown if the device cannot be opened due to
     *         security restrictions
     * @see #close
     * @see #isOpen
     */
    void open() throws MidiUnavailableException;

    /**
     * Closes the device, indicating that the device should now release any
     * system resources it is using.
     * <p>
     * All {@code Receiver} and {@code Transmitter} instances open from this
     * device are closed. This includes instances retrieved via
     * {@code MidiSystem}.
     *
     * @see #open
     * @see #isOpen
     */
    @Override
    void close();

    /**
     * Reports whether the device is open.
     *
     * @return {@code true} if the device is open, otherwise {@code false}
     * @see #open
     * @see #close
     */
    boolean isOpen();

    /**
     * Obtains the current time-stamp of the device, in microseconds. If a
     * device supports time-stamps, it should start counting at 0 when the
     * device is opened and continue incrementing its time-stamp in microseconds
     * until the device is closed. If it does not support time-stamps, it should
     * always return -1.
     *
     * @return the current time-stamp of the device in microseconds, or -1 if
     *         time-stamping is not supported by the device
     */
    long getMicrosecondPosition();

    /**
     * Obtains the maximum number of MIDI IN connections available on this MIDI
     * device for receiving MIDI data.
     *
     * @return maximum number of MIDI IN connections, or -1 if an unlimited
     *         number of connections is available
     */
    int getMaxReceivers();

    /**
     * Obtains the maximum number of MIDI OUT connections available on this MIDI
     * device for transmitting MIDI data.
     *
     * @return maximum number of MIDI OUT connections, or -1 if an unlimited
     *         number of connections is available
     */
    int getMaxTransmitters();

    /**
     * Obtains a MIDI IN receiver through which the MIDI device may receive MIDI
     * data. The returned receiver must be closed when the application has
     * finished using it.
     * <p>
     * Usually the returned receiver implements the {@code MidiDeviceReceiver}
     * interface.
     * <p>
     * Obtaining a {@code Receiver} with this method does not open the device.
     * To be able to use the device, it has to be opened explicitly by calling
     * {@link #open}. Also, closing the {@code Receiver} does not close the
     * device. It has to be closed explicitly by calling {@link #close}.
     *
     * @return a receiver for the device
     * @throws MidiUnavailableException thrown if a receiver is not available
     *         due to resource restrictions
     * @see Receiver#close()
     */
    Receiver getReceiver() throws MidiUnavailableException;

    /**
     * Returns all currently active, non-closed receivers connected with this
     * {@code MidiDevice}. A receiver can be removed from the device by closing
     * it.
     * <p>
     * Usually the returned receivers implement the {@code MidiDeviceReceiver}
     * interface.
     *
     * @return an unmodifiable list of the open receivers
     * @since 1.5
     */
    List<Receiver> getReceivers();

    /**
     * Obtains a MIDI OUT connection from which the MIDI device will transmit
     * MIDI data. The returned transmitter must be closed when the application
     * has finished using it.
     * <p>
     * Usually the returned transmitter implements the
     * {@code MidiDeviceTransmitter} interface.
     * <p>
     * Obtaining a {@code Transmitter} with this method does not open the
     * device. To be able to use the device, it has to be opened explicitly by
     * calling {@link #open}. Also, closing the {@code Transmitter} does not
     * close the device. It has to be closed explicitly by calling
     * {@link #close}.
     *
     * @return a MIDI OUT transmitter for the device
     * @throws MidiUnavailableException thrown if a transmitter is not available
     *         due to resource restrictions
     * @see Transmitter#close()
     */
    Transmitter getTransmitter() throws MidiUnavailableException;

    /**
     * Returns all currently active, non-closed transmitters connected with this
     * {@code MidiDevice}. A transmitter can be removed from the device by
     * closing it.
     * <p>
     * Usually the returned transmitters implement the
     * {@code MidiDeviceTransmitter} interface.
     *
     * @return an unmodifiable list of the open transmitters
     * @since 1.5
     */
    List<Transmitter> getTransmitters();

    /**
     * A {@code MidiDevice.Info} object contains assorted data about a
     * {@link MidiDevice}, including its name, the company who created it, and
     * descriptive text.
     *
     * @see MidiDevice#getDeviceInfo
     */
    class Info {

        /**
         * The device's name.
         */
        private final String name;

        /**
         * The name of the company who provides the device.
         */
        private final String vendor;

        /**
         * A description of the device.
         */
        private final String description;

        /**
         * Device version.
         */
        private final String version;

        /**
         * Constructs a device info object.
         *
         * @param  name the name of the device
         * @param  vendor the name of the company who provides the device
         * @param  description a description of the device
         * @param  version version information for the device
         */
        protected Info(String name, String vendor, String description,
                       String version) {

            this.name = name;
            this.vendor = vendor;
            this.description = description;
            this.version = version;
        }

        /**
         * Indicates whether the specified object is equal to this info object,
         * returning {@code true} if the objects are the same.
         *
         * @param  obj the reference object with which to compare
         * @return {@code true} if the specified object is equal to this info
         *         object; {@code false} otherwise
         */
        @Override
        public final boolean equals(Object obj) {
            return super.equals(obj);
        }

        /**
         * Returns a hash code value for this info object.
         *
         * @return a hash code value for this info object
         */
        @Override
        public final int hashCode() {
            return super.hashCode();
        }

        /**
         * Obtains the name of the device.
         *
         * @return a string containing the device's name
         */
        public final String getName() {
            return name;
        }

        /**
         * Obtains the name of the company who supplies the device.
         *
         * @return device the vendor's name
         */
        public final String getVendor() {
            return vendor;
        }

        /**
         * Obtains the description of the device.
         *
         * @return a description of the device
         */
        public final String getDescription() {
            return description;
        }

        /**
         * Obtains the version of the device.
         *
         * @return textual version information for the device
         */
        public final String getVersion() {
            return version;
        }

        /**
         * Returns a string representation of the info object.
         *
         * @return a string representation of the info object
         */
        @Override
        public final String toString() {
            return name;
        }
    }
}
