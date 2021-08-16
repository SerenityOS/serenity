/*
 * Copyright (c) 1999, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.util.EventListener;

/**
 * The {@code ControllerEventListener} interface should be implemented by
 * classes whose instances need to be notified when a {@link Sequencer} has
 * processed a requested type of MIDI control-change event. To register a
 * {@code ControllerEventListener} object to receive such notifications, invoke
 * the
 * {@link Sequencer#addControllerEventListener(ControllerEventListener, int[])
 * addControllerEventListener} method of {@code Sequencer}, specifying the types
 * of MIDI controllers about which you are interested in getting control-change
 * notifications.
 *
 * @author Kara Kytle
 * @see MidiChannel#controlChange(int, int)
 */
public interface ControllerEventListener extends EventListener {

    /**
     * Invoked when a {@link Sequencer} has encountered and processed a
     * control-change event of interest to this listener. The event passed in is
     * a {@code ShortMessage} whose first data byte indicates the controller
     * number and whose second data byte is the value to which the controller
     * was set.
     *
     * @param  event the control-change event that the sequencer encountered in
     *         the sequence it is processing
     * @see Sequencer#addControllerEventListener(ControllerEventListener, int[])
     * @see MidiChannel#controlChange(int, int)
     * @see ShortMessage#getData1
     * @see ShortMessage#getData2
     */
    void controlChange(ShortMessage event);
}
