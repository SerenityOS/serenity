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

public class TransformSwitch {

    private static void assertTrue(boolean value) throws Exception
    {
        if(!value)
            throw new RuntimeException("assertTrue fails!");
    }

    public static void main(String[] args) throws Exception {
        ModelStandardTransform transform = new ModelStandardTransform();
        transform.setTransform(ModelStandardTransform.TRANSFORM_SWITCH);

        transform.setDirection(ModelStandardTransform.DIRECTION_MIN2MAX);
        transform.setPolarity(ModelStandardTransform.POLARITY_UNIPOLAR);
        assertTrue(Math.abs(transform.transform(0.2f) - 0.0f) < 0.0001f);
        assertTrue(Math.abs(transform.transform(0.8f) - 1.0f) < 0.0001f);

        transform.setDirection(ModelStandardTransform.DIRECTION_MAX2MIN);
        transform.setPolarity(ModelStandardTransform.POLARITY_UNIPOLAR);
        assertTrue(Math.abs(transform.transform(0.2f) - 1.0f) < 0.0001f);
        assertTrue(Math.abs(transform.transform(0.8f) - 0.0f) < 0.0001f);

        transform.setDirection(ModelStandardTransform.DIRECTION_MIN2MAX);
        transform.setPolarity(ModelStandardTransform.POLARITY_BIPOLAR);
        assertTrue(Math.abs(transform.transform(0.2f) + 1.0f) < 0.0001f);
        assertTrue(Math.abs(transform.transform(0.8f) - 1.0f) < 0.0001f);

        transform.setDirection(ModelStandardTransform.DIRECTION_MAX2MIN);
        transform.setPolarity(ModelStandardTransform.POLARITY_BIPOLAR);
        assertTrue(Math.abs(transform.transform(0.2f) - 1.0f) < 0.0001f);
        assertTrue(Math.abs(transform.transform(0.8f) + 1.0f) < 0.0001f);
    }
}
