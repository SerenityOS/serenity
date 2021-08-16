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
 * A resampler that uses third-order (cubic) interpolation.
 *
 * @author Karl Helgason
 */
public final class SoftCubicResampler extends SoftAbstractResampler {

    @Override
    public int getPadding() {
        return 3;
    }

    @Override
    public void interpolate(float[] in, float[] in_offset, float in_end,
                            float[] startpitch, float pitchstep, float[] out, int[] out_offset,
                            int out_end) {
        float pitch = startpitch[0];
        float ix = in_offset[0];
        int ox = out_offset[0];
        float ix_end = in_end;
        int ox_end = out_end;
        if (pitchstep == 0) {
            while (ix < ix_end && ox < ox_end) {
                int iix = (int) ix;
                float fix = ix - iix;
                float y0 = in[iix - 1];
                float y1 = in[iix];
                float y2 = in[iix + 1];
                float y3 = in[iix + 2];
                float a0 = y3 - y2 + y1 - y0;
                float a1 = y0 - y1 - a0;
                float a2 = y2 - y0;
                float a3 = y1;
                //float fix2 = fix * fix;
                //out[ox++] = (a0 * fix + a1) * fix2 + (a2 * fix + a3);
                out[ox++] = ((a0 * fix + a1) * fix + a2) * fix + a3;
                ix += pitch;
            }
        } else {
            while (ix < ix_end && ox < ox_end) {
                int iix = (int) ix;
                float fix = ix - iix;
                float y0 = in[iix - 1];
                float y1 = in[iix];
                float y2 = in[iix + 1];
                float y3 = in[iix + 2];
                float a0 = y3 - y2 + y1 - y0;
                float a1 = y0 - y1 - a0;
                float a2 = y2 - y0;
                float a3 = y1;
                //float fix2 = fix * fix;
                //out[ox++] = (a0 * fix + a1) * fix2 + (a2 * fix + a3);
                out[ox++] = ((a0 * fix + a1) * fix + a2) * fix + a3;
                ix += pitch;
                pitch += pitchstep;
            }
        }
        in_offset[0] = ix;
        out_offset[0] = ox;
        startpitch[0] = pitch;

    }
}
