/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * This is a wavetable oscillator interface.
 *
 * @author Karl Helgason
 */
public interface ModelWavetable extends ModelOscillator {

    int LOOP_TYPE_OFF = 0;
    int LOOP_TYPE_FORWARD = 1;
    int LOOP_TYPE_RELEASE = 2;
    int LOOP_TYPE_PINGPONG = 4;
    int LOOP_TYPE_REVERSE = 8;

    AudioFloatInputStream openStream();

    float getLoopLength();

    float getLoopStart();

    int getLoopType();

    float getPitchcorrection();
}
