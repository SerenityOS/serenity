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
 * This class is used to identify destinations in connection blocks,
 * see ModelConnectionBlock.
 *
 * @author Karl Helgason
 */
public final class ModelDestination {

    public static final ModelIdentifier DESTINATION_NONE = null;
    public static final ModelIdentifier DESTINATION_KEYNUMBER
            = new ModelIdentifier("noteon", "keynumber");
    public static final ModelIdentifier DESTINATION_VELOCITY
            = new ModelIdentifier("noteon", "velocity");
    public static final ModelIdentifier DESTINATION_PITCH
            = new ModelIdentifier("osc", "pitch");   // cent
    public static final ModelIdentifier DESTINATION_GAIN
            = new ModelIdentifier("mixer", "gain");   // cB
    public static final ModelIdentifier DESTINATION_PAN
            = new ModelIdentifier("mixer", "pan");   // 0.1 %
    public static final ModelIdentifier DESTINATION_REVERB
            = new ModelIdentifier("mixer", "reverb");   // 0.1 %
    public static final ModelIdentifier DESTINATION_CHORUS
            = new ModelIdentifier("mixer", "chorus");   // 0.1 %
    public static final ModelIdentifier DESTINATION_LFO1_DELAY
            = new ModelIdentifier("lfo", "delay", 0); // timecent
    public static final ModelIdentifier DESTINATION_LFO1_FREQ
            = new ModelIdentifier("lfo", "freq", 0); // cent
    public static final ModelIdentifier DESTINATION_LFO2_DELAY
            = new ModelIdentifier("lfo", "delay", 1); // timecent
    public static final ModelIdentifier DESTINATION_LFO2_FREQ
            = new ModelIdentifier("lfo", "freq", 1); // cent
    public static final ModelIdentifier DESTINATION_EG1_DELAY
            = new ModelIdentifier("eg", "delay", 0); // timecent
    public static final ModelIdentifier DESTINATION_EG1_ATTACK
            = new ModelIdentifier("eg", "attack", 0); // timecent
    public static final ModelIdentifier DESTINATION_EG1_HOLD
            = new ModelIdentifier("eg", "hold", 0); // timecent
    public static final ModelIdentifier DESTINATION_EG1_DECAY
            = new ModelIdentifier("eg", "decay", 0); // timecent
    public static final ModelIdentifier DESTINATION_EG1_SUSTAIN
            = new ModelIdentifier("eg", "sustain", 0);
                                        // 0.1 % (I want this to be value not %)
    public static final ModelIdentifier DESTINATION_EG1_RELEASE
            = new ModelIdentifier("eg", "release", 0); // timecent
    public static final ModelIdentifier DESTINATION_EG1_SHUTDOWN
            = new ModelIdentifier("eg", "shutdown", 0); // timecent
    public static final ModelIdentifier DESTINATION_EG2_DELAY
            = new ModelIdentifier("eg", "delay", 1); // timecent
    public static final ModelIdentifier DESTINATION_EG2_ATTACK
            = new ModelIdentifier("eg", "attack", 1); // timecent
    public static final ModelIdentifier DESTINATION_EG2_HOLD
            = new ModelIdentifier("eg", "hold", 1); // 0.1 %
    public static final ModelIdentifier DESTINATION_EG2_DECAY
            = new ModelIdentifier("eg", "decay", 1); // timecent
    public static final ModelIdentifier DESTINATION_EG2_SUSTAIN
            = new ModelIdentifier("eg", "sustain", 1);
                                        // 0.1 % ( I want this to be value not %)
    public static final ModelIdentifier DESTINATION_EG2_RELEASE
            = new ModelIdentifier("eg", "release", 1); // timecent
    public static final ModelIdentifier DESTINATION_EG2_SHUTDOWN
            = new ModelIdentifier("eg", "shutdown", 1); // timecent
    public static final ModelIdentifier DESTINATION_FILTER_FREQ
            = new ModelIdentifier("filter", "freq", 0); // cent
    public static final ModelIdentifier DESTINATION_FILTER_Q
            = new ModelIdentifier("filter", "q", 0); // cB
    private ModelIdentifier destination = DESTINATION_NONE;
    private ModelTransform transform = new ModelStandardTransform();

    public ModelDestination() {
    }

    public ModelDestination(ModelIdentifier id) {
        destination = id;
    }

    public ModelIdentifier getIdentifier() {
        return destination;
    }

    public void setIdentifier(ModelIdentifier destination) {
        this.destination = destination;
    }

    public ModelTransform getTransform() {
        return transform;
    }

    public void setTransform(ModelTransform transform) {
        this.transform = transform;
    }
}
