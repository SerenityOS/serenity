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
 * A resampler that uses first-order (linear) interpolation.
 *
 * This one doesn't perform float to int casting inside the processing loop.
 *
 * @author Karl Helgason
 */
public final class SoftLinearResampler2 extends SoftAbstractResampler {

    @Override
    public int getPadding() {
        return 2;
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

        // Check if we have do anything
        if (!(ix < ix_end && ox < ox_end))
            return;

        // 15 bit shift was choosed because
        // it resulted in no drift between p_ix and ix.
        int p_ix = (int) (ix * (1 << 15));
        int p_ix_end = (int) (ix_end * (1 << 15));
        int p_pitch = (int) (pitch * (1 << 15));
        // Pitch needs to recalculated
        // to ensure no drift between p_ix and ix.
        pitch = p_pitch * (1f / (1 << 15));

        if (pitchstep == 0f) {

            // To reduce
            //    while (p_ix < p_ix_end && ox < ox_end)
            // into
            //    while  (ox < ox_end)
            // We need to calculate new ox_end value.
            int p_ix_len = p_ix_end - p_ix;
            int p_mod = p_ix_len % p_pitch;
            if (p_mod != 0)
                p_ix_len += p_pitch - p_mod;
            int ox_end2 = ox + p_ix_len / p_pitch;
            if (ox_end2 < ox_end)
                ox_end = ox_end2;

            while (ox < ox_end) {
                int iix = p_ix >> 15;
                float fix = ix - iix;
                float i = in[iix];
                out[ox++] = i + (in[iix + 1] - i) * fix;
                p_ix += p_pitch;
                ix += pitch;
            }

        } else {

            int p_pitchstep = (int) (pitchstep * (1 << 15));
            pitchstep = p_pitchstep * (1f / (1 << 15));

            while (p_ix < p_ix_end && ox < ox_end) {
                int iix = p_ix >> 15;
                float fix = ix - iix;
                float i = in[iix];
                out[ox++] = i + (in[iix + 1] - i) * fix;
                ix += pitch;
                p_ix += p_pitch;
                pitch += pitchstep;
                p_pitch += p_pitchstep;
            }
        }
        in_offset[0] = ix;
        out_offset[0] = ox;
        startpitch[0] = pitch;

    }
}
