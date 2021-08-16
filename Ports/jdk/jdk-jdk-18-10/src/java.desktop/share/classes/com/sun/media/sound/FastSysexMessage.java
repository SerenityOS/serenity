/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.SysexMessage;

/**
 * optimized FastSysexMessage that doesn't copy the array upon instantiation.
 *
 * @author Florian Bomers
 */
final class FastSysexMessage extends SysexMessage {

    FastSysexMessage(byte[] data) throws InvalidMidiDataException {
        super(data);
        MidiUtils.checkSysexStatus(data, data.length);
    }

    /**
     * The returned array may be larger than this message is.
     * Use getLength() to get the real length of the message.
     */
    byte[] getReadOnlyMessage() {
        return data;
    }

    // overwrite this method so that the original data array,
    // which is shared among all transmitters, cannot be modified
    @Override
    public void setMessage(byte[] data, int length) throws InvalidMidiDataException {
        MidiUtils.checkSysexStatus(data, length);
        this.length = length;
        this.data = new byte[this.length];
        System.arraycopy(data, 0, this.data, 0, length);
    }

} // class FastSysexMessage
