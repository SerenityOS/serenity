/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
   @summary Test SoftFilter processAudio method
   @modules java.desktop/com.sun.media.sound
*/

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import javax.sound.sampled.*;

import com.sun.media.sound.*;

public class TestProcessAudio {

    public static void main(String[] args) throws Exception {
        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
        SoftAudioBuffer sbuffer = new SoftAudioBuffer(3600, format);
        SoftFilter filter = new SoftFilter(format.getSampleRate());
        Random random = new Random(42);


        for (int t = 0; t <= 6; t++)
        {
            if(t == 0) filter.setFilterType(SoftFilter.FILTERTYPE_BP12);
            if(t == 1) filter.setFilterType(SoftFilter.FILTERTYPE_HP12);
            if(t == 2) filter.setFilterType(SoftFilter.FILTERTYPE_HP24);
            if(t == 3) filter.setFilterType(SoftFilter.FILTERTYPE_LP12);
            if(t == 4) filter.setFilterType(SoftFilter.FILTERTYPE_LP24);
            if(t == 5) filter.setFilterType(SoftFilter.FILTERTYPE_LP6);
            if(t == 6) filter.setFilterType(SoftFilter.FILTERTYPE_NP12);


            // Try first by reseting always
            for (int f = 1200; f < 3600; f+=100)
                for (int r = 0; r <= 30; r+=5) {
                    filter.reset();
                    filter.setResonance(r);
                    filter.setFrequency(f);
                    float[] data = sbuffer.array();
                    int len = sbuffer.getSize();
                    for (int i = 0; i < len; i++)
                        data[i] = random.nextFloat() - 0.5f;
                    filter.processAudio(sbuffer);
                }

            // Now we skip reseting
            // to test how changing frequency and resonance
            // affect active filter
            for (int f = 100; f < 12800; f+=1200)
            for (int r = 0; r <= 30; r+=5) {
                filter.setResonance(r);
                filter.setFrequency(f);
                float[] data = sbuffer.array();
                int len = sbuffer.getSize();
                for (int i = 0; i < len; i++)
                    data[i] = random.nextFloat() - 0.5f;
                filter.processAudio(sbuffer);
            }
            for (int f = 12800; f >= 100; f-=1200)
                for (int r = 30; r >= 0; r-=5) {
                    filter.setResonance(r);
                    filter.setFrequency(f);
                    float[] data = sbuffer.array();
                    int len = sbuffer.getSize();
                    for (int i = 0; i < len; i++)
                        data[i] = random.nextFloat() - 0.5f;
                    filter.processAudio(sbuffer);
                }
            filter.reset();
        }

    }

}
