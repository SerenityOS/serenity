/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1.humongousObjects;

import gc.testlibrary.Helpers;
import sun.hotspot.WhiteBox;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Provides generated and compiled sample classes and theirs expected sizes.
 */
public enum G1SampleClass {

    LARGEST_NON_HUMONGOUS {
        @Override
        public long expectedInstanceSize() {
            return HALF_G1_REGION_SIZE;
        }
    },
    SMALLEST_HUMONGOUS {
        @Override
        public long expectedInstanceSize() {
            return HALF_G1_REGION_SIZE + Helpers.SIZE_OF_LONG;
        }
    },
    ONE_REGION_HUMONGOUS {
        @Override
        public long expectedInstanceSize() {
            return G1_REGION_SIZE;
        }
    },
    TWO_REGION_HUMONGOUS {
        @Override
        public long expectedInstanceSize() {
            return G1_REGION_SIZE + Helpers.SIZE_OF_LONG;
        }
    },
    MORE_THAN_TWO_REGION_HUMONGOUS {
        @Override
        public long expectedInstanceSize() {
            return G1_REGION_SIZE * 2 + Helpers.SIZE_OF_LONG;
        }
    };

    private static final long G1_REGION_SIZE = WhiteBox.getWhiteBox().g1RegionSize();
    private static final long HALF_G1_REGION_SIZE = G1_REGION_SIZE / 2;

    /**
     * Generates and compiles class with instance of specified size and loads it in specified class loader
     *
     * @param classLoader class loader which will be used to load class
     * @param wrkDir working dir where generated classes are put and compiled
     * @param classNamePrefix prefix for service classes (ones we use to create chain of inheritance)
     * @return a class with instances of the specified size loaded in specified class loader
     * @throws IOException
     * @throws ClassNotFoundException
     */

    public Class<?> getCls(ClassLoader classLoader, Path wrkDir, String classNamePrefix)
            throws IOException, ClassNotFoundException {
        return Helpers.generateCompileAndLoad(classLoader, Helpers.enumNameToClassName(name()) + "Class",
                expectedInstanceSize(), wrkDir, classNamePrefix);
    }

    /**
     * @return G1SampleClass instance expected size
     */
    public abstract long expectedInstanceSize();
}
