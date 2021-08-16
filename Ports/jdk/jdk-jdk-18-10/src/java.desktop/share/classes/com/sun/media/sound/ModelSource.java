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

/**
 * This class is used to identify sources in connection blocks,
 * see ModelConnectionBlock.
 *
 * @author Karl Helgason
 */
public final class ModelSource {

    public static final ModelIdentifier SOURCE_NONE = null;
    public static final ModelIdentifier SOURCE_NOTEON_KEYNUMBER =
            new ModelIdentifier("noteon", "keynumber");     // midi keynumber
    public static final ModelIdentifier SOURCE_NOTEON_VELOCITY =
            new ModelIdentifier("noteon", "velocity");      // midi velocity
    public static final ModelIdentifier SOURCE_EG1 =
            new ModelIdentifier("eg", null, 0);
    public static final ModelIdentifier SOURCE_EG2 =
            new ModelIdentifier("eg", null, 1);
    public static final ModelIdentifier SOURCE_LFO1 =
            new ModelIdentifier("lfo", null, 0);
    public static final ModelIdentifier SOURCE_LFO2 =
            new ModelIdentifier("lfo", null, 1);
    public static final ModelIdentifier SOURCE_MIDI_PITCH =
            new ModelIdentifier("midi", "pitch", 0);            // (0..16383)
    public static final ModelIdentifier SOURCE_MIDI_CHANNEL_PRESSURE =
            new ModelIdentifier("midi", "channel_pressure", 0); // (0..127)
//    public static final ModelIdentifier SOURCE_MIDI_MONO_PRESSURE =
//            new ModelIdentifier("midi","mono_pressure",0);    // (0..127)
    public static final ModelIdentifier SOURCE_MIDI_POLY_PRESSURE =
            new ModelIdentifier("midi", "poly_pressure", 0);    // (0..127)
    public static final ModelIdentifier SOURCE_MIDI_CC_0 =
            new ModelIdentifier("midi_cc", "0", 0);             // (0..127)
    public static final ModelIdentifier SOURCE_MIDI_RPN_0 =
            new ModelIdentifier("midi_rpn", "0", 0);            // (0..16383)
    private ModelIdentifier source = SOURCE_NONE;
    private ModelTransform transform;

    public ModelSource() {
        this.transform = new ModelStandardTransform();
    }

    public ModelSource(ModelIdentifier id) {
        source = id;
        this.transform = new ModelStandardTransform();
    }

    public ModelSource(ModelIdentifier id, boolean direction) {
        source = id;
        this.transform = new ModelStandardTransform(direction);
    }

    public ModelSource(ModelIdentifier id, boolean direction, boolean polarity) {
        source = id;
        this.transform = new ModelStandardTransform(direction, polarity);
    }

    public ModelSource(ModelIdentifier id, boolean direction, boolean polarity,
            int transform) {
        source = id;
        this.transform =
                new ModelStandardTransform(direction, polarity, transform);
    }

    public ModelSource(ModelIdentifier id, ModelTransform transform) {
        source = id;
        this.transform = transform;
    }

    public ModelIdentifier getIdentifier() {
        return source;
    }

    public void setIdentifier(ModelIdentifier source) {
        this.source = source;
    }

    public ModelTransform getTransform() {
        return transform;
    }

    public void setTransform(ModelTransform transform) {
        this.transform = transform;
    }
}
