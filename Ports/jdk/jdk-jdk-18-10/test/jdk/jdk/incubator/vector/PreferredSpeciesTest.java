/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.incubator.vector.*;
import jdk.internal.vm.vector.VectorSupport;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @test
 * @modules jdk.incubator.vector java.base/jdk.internal.vm.vector
 * @run testng PreferredSpeciesTest
 */

/**
 * @test
 * @bug 8262096
 * @requires vm.compiler2.enabled
 * @summary Test the initialization of vector shapes
 * @modules jdk.incubator.vector java.base/jdk.internal.vm.vector
 * @run testng/othervm -XX:MaxVectorSize=8 PreferredSpeciesTest
 * @run testng/othervm -XX:MaxVectorSize=4 PreferredSpeciesTest
 */

public class PreferredSpeciesTest {
    @DataProvider
    public static Object[][] classesProvider() {
        return new Object[][]{
                {byte.class},
                {short.class},
                {int.class},
                {float.class},
                {long.class},
                {double.class},
        };
    }

    @Test(dataProvider = "classesProvider")
    void testVectorLength(Class<?> c) {
        VectorSpecies<?> species = null;
        int elemSize = 0;
        if (c == byte.class) {
            species = ByteVector.SPECIES_PREFERRED;
            elemSize = Byte.SIZE;
        } else if (c == short.class) {
            species = ShortVector.SPECIES_PREFERRED;
            elemSize = Short.SIZE;
        } else if (c == int.class) {
            species = IntVector.SPECIES_PREFERRED;
            elemSize = Integer.SIZE;
        } else if (c == long.class) {
            species = LongVector.SPECIES_PREFERRED;
            elemSize = Long.SIZE;
        } else if (c == float.class) {
            species = FloatVector.SPECIES_PREFERRED;
            elemSize = Float.SIZE;
        } else if (c == double.class) {
            species = DoubleVector.SPECIES_PREFERRED;
            elemSize = Double.SIZE;
        } else {
            throw new IllegalArgumentException("Bad vector element type: " + c.getName());
        }
        VectorShape shape = VectorShape.preferredShape();

        int maxLaneCount = Math.max(VectorSupport.getMaxLaneCount(c), 64 / elemSize);

        System.out.println("class = "+c+"; preferred shape"+shape+"; preferred species = "+species+"; maxSize="+maxLaneCount);
        Assert.assertEquals(species.vectorShape(), shape);
        Assert.assertEquals(species.length(), Math.min(species.length(), maxLaneCount));
    }

    @Test(dataProvider = "classesProvider")
    void testVectorShape(Class<?> c) {
        VectorSpecies<?> species = null;
        int elemSize = 0;
        if (c == byte.class) {
            species = ByteVector.SPECIES_PREFERRED;
            elemSize = Byte.SIZE;
        } else if (c == short.class) {
            species = ShortVector.SPECIES_PREFERRED;
            elemSize = Short.SIZE;
        } else if (c == int.class) {
            species = IntVector.SPECIES_PREFERRED;
            elemSize = Integer.SIZE;
        } else if (c == long.class) {
            species = LongVector.SPECIES_PREFERRED;
            elemSize = Long.SIZE;
        } else if (c == float.class) {
            species = FloatVector.SPECIES_PREFERRED;
            elemSize = Float.SIZE;
        } else if (c == double.class) {
            species = DoubleVector.SPECIES_PREFERRED;
            elemSize = Double.SIZE;
        } else {
            throw new IllegalArgumentException("Bad vector element type: " + c.getName());
        }

        int maxLaneCount = Math.max(VectorSupport.getMaxLaneCount(c), 64 / elemSize);

        VectorSpecies largestSpecies = VectorSpecies.ofLargestShape(c);
        VectorShape largestShape = VectorShape.forBitSize(maxLaneCount * elemSize);

        System.out.println("class = "+c+"; largest species = "+largestSpecies+"; maxSize="+maxLaneCount);
        Assert.assertEquals(largestSpecies.vectorShape(), largestShape);
        Assert.assertEquals(largestSpecies.length(), maxLaneCount);
        Assert.assertEquals(largestSpecies.length(), Math.max(species.length(), maxLaneCount));
    }
}
