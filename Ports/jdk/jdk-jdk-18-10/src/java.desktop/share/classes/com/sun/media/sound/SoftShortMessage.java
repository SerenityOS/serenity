/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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
import javax.sound.midi.ShortMessage;

/**
 * A short message class that support for than 16 midi channels.
 *
 * @author Karl Helgason
 */
public final class SoftShortMessage extends ShortMessage {

    int channel = 0;

    @Override
    public int getChannel() {
        return channel;
    }

    @Override
    public void setMessage(int command, int channel, int data1, int data2)
            throws InvalidMidiDataException {
        this.channel = channel;
        super.setMessage(command, channel & 0xF, data1, data2);
    }

    @Override
    public Object clone() {
        SoftShortMessage clone = new SoftShortMessage();
        try {
            clone.setMessage(getCommand(), getChannel(), getData1(), getData2());
        } catch (InvalidMidiDataException e) {
            throw new IllegalArgumentException(e);
        }
        return clone;
    }
}
