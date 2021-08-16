/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/* @test
   @summary Test SoftLinearResampler interpolate method
   @modules java.desktop/com.sun.media.sound
*/

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.sound.sampled.*;

import com.sun.media.sound.*;

public class Interpolate {

    private static float getResamplerTestValue(double i)
    {
        return (float)Math.sin(i / 10.0);
    }

    private static void perfectInterpolation(float[] in_offset, float in_end,
            float[] startpitch, float pitchstep, float[] out, int[] out_offset,
            int out_end) {

        float pitch = startpitch[0];
        float ix = in_offset[0];
        int ox = out_offset[0];
        float ix_end = in_end;
        int ox_end = out_end;
        if (pitchstep == 0f) {
            while (ix < ix_end && ox < ox_end) {
                out[ox++] = getResamplerTestValue(ix);
                ix += pitch;
            }
        } else {
            while (ix < ix_end && ox < ox_end) {
                out[ox++] = getResamplerTestValue(ix);
                ix += pitch;
                pitch += pitchstep;
            }
        }
        in_offset[0] = ix;
        out_offset[0] = ox;
        startpitch[0] = pitch;

    }

    private static float testResampler(SoftAbstractResampler resampler, float p_pitch, float p_pitch2)
    {
        float[] testbuffer = new float[4096];
        float[] testbuffer2 = new float[1024];
        float[] testbuffer3 = new float[1024];
        for (int i = 0; i < testbuffer.length; i++)
            testbuffer[i] = getResamplerTestValue(i);
        int pads = resampler.getPadding();
        float pitchstep = (p_pitch2 - p_pitch)/1024f;
        int[] out_offset2 = {0};
        int[] out_offset3 = {0};
        resampler.interpolate(testbuffer, new float[] {pads}, testbuffer.length - pads, new float[] {p_pitch}, pitchstep, testbuffer2, out_offset2, testbuffer2.length);
        perfectInterpolation(new float[] {pads}, testbuffer.length - pads, new float[] {p_pitch}, pitchstep, testbuffer3, out_offset3, testbuffer3.length);
        int out_off = out_offset2[0];
        if(out_offset3[0] < out_off)
            out_off = out_offset3[0];
        float ac_error = 0;
        int counter = 0;
        for (int i = pads; i < out_off; i++) {
            ac_error += Math.abs(testbuffer2[i] - testbuffer3[i]);
            counter++;
        }
        return ac_error / ((float)counter);
    }

    private static void fail(String error) throws Exception
    {
        throw new RuntimeException(error);
    }

    public static void main(String[] args) throws Exception {
        SoftLinearResampler resampler = new SoftLinearResampler();
        float max = testResampler(resampler, 0.3f, 0.3f);
        if(max > 0.001)
            fail("Interpolation failed, error="+max);
        max = testResampler(resampler, 0.3f, 0.01f);
        if(max > 0.001)
            fail("Interpolation failed, error="+max);
        max = testResampler(resampler, 1.0f, 0.00f);
        if(max > 0.001)
            fail("Interpolation failed, error="+max);
    }
}

