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
   @summary Test ModelStandardTransform transform method
   @modules java.desktop/com.sun.media.sound
*/

import com.sun.media.sound.ModelStandardTransform;

public class TransformConvex {

    private static boolean checkLinearity(ModelStandardTransform transform)
    {
        double lastx = 0;
        for (int p = 0; p < 2; p++)
        for (int d = 0; d < 2; d++)
        for (double i = 0; i < 1.0; i+=0.001) {
            if(p == 0)
                transform.setPolarity(ModelStandardTransform.POLARITY_UNIPOLAR);
            else
                transform.setPolarity(ModelStandardTransform.POLARITY_BIPOLAR);
            if(d == 0)
                transform.setDirection(ModelStandardTransform.DIRECTION_MIN2MAX);
            else
                transform.setDirection(ModelStandardTransform.DIRECTION_MAX2MIN);
            double x = transform.transform(i);
            if(i == 0)
                lastx = x;
            else
            {
                if(lastx - x > 0.2) return false;
                lastx = x;
            }
        }
        return true;
    }

    private static void assertTrue(boolean value) throws Exception
    {
        if(!value)
            throw new RuntimeException("assertTrue fails!");
    }

    public static void main(String[] args) throws Exception {
        ModelStandardTransform transform = new ModelStandardTransform();
        transform.setTransform(ModelStandardTransform.TRANSFORM_CONVEX);
        assertTrue(checkLinearity(transform));

        transform.setDirection(ModelStandardTransform.DIRECTION_MIN2MAX);
        transform.setPolarity(ModelStandardTransform.POLARITY_UNIPOLAR);
        assertTrue(Math.abs(transform.transform(0.0f) - 0.0f) < 0.0001f);
        assertTrue(transform.transform(0.5f) > 0.5f);
        assertTrue(Math.abs(transform.transform(1.0f) - 1.0f) < 0.0001f);

        transform.setDirection(ModelStandardTransform.DIRECTION_MAX2MIN);
        transform.setPolarity(ModelStandardTransform.POLARITY_UNIPOLAR);
        assertTrue(Math.abs(transform.transform(1.0f) - 0.0f) < 0.0001f);
        assertTrue(transform.transform(0.5f) > 0.5f);
        assertTrue(Math.abs(transform.transform(0.0f) - 1.0f) < 0.0001f);

        transform.setDirection(ModelStandardTransform.DIRECTION_MIN2MAX);
        transform.setPolarity(ModelStandardTransform.POLARITY_BIPOLAR);
        assertTrue(Math.abs(transform.transform(0.0f) + 1.0f) < 0.0001f);
        assertTrue(transform.transform(0.25f) < -0.5f);
        assertTrue(Math.abs(transform.transform(0.5f) - 0.0f) < 0.0001f);
        assertTrue(transform.transform(0.75f) > 0.5f);
        assertTrue(Math.abs(transform.transform(1.0f) - 1.0f) < 0.0001f);

        transform.setDirection(ModelStandardTransform.DIRECTION_MAX2MIN);
        transform.setPolarity(ModelStandardTransform.POLARITY_BIPOLAR);
        assertTrue(Math.abs(transform.transform(1.0f) + 1.0f) < 0.0001f);
        assertTrue(transform.transform(0.75f) < -0.5f);
        assertTrue(Math.abs(transform.transform(0.50f) - 0.0f) < 0.0001f);
        assertTrue(transform.transform(0.25f) > 0.5f);
        assertTrue(Math.abs(transform.transform(0.0f) - 1.0f) < 0.0001f);
    }
}
