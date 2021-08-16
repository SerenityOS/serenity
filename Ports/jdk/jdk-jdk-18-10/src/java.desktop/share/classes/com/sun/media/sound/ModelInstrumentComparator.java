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

import java.util.Comparator;

import javax.sound.midi.Instrument;
import javax.sound.midi.Patch;

/**
 * Instrument comparator class.
 * Used to order instrument by program, bank, percussion.
 *
 * @author Karl Helgason
 */
public final class ModelInstrumentComparator implements Comparator<Instrument> {

    @Override
    public int compare(Instrument arg0, Instrument arg1) {
        Patch p0 = arg0.getPatch();
        Patch p1 = arg1.getPatch();
        int a = p0.getBank() * 128 + p0.getProgram();
        int b = p1.getBank() * 128 + p1.getProgram();
        if (p0 instanceof ModelPatch) {
            a += ((ModelPatch)p0).isPercussion() ? 2097152 : 0;
        }
        if (p1 instanceof ModelPatch) {
            b += ((ModelPatch)p1).isPercussion() ? 2097152 : 0;
        }
        return a - b;
    }
}
