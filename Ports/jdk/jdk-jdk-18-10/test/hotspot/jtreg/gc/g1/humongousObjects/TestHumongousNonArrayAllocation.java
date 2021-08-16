/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @test gc.g1.humongousObjects.TestHumongousNonArrayAllocation
 * @summary Checks that huge class' instances (ie with huge amount of fields) are allocated successfully
 * @requires vm.gc.G1
 * @requires vm.opt.G1HeapRegionSize == "null" | vm.opt.G1HeapRegionSize == "1M"
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xms128M -Xmx128M -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:G1HeapRegionSize=1M
 *                   gc.g1.humongousObjects.TestHumongousNonArrayAllocation LARGEST_NON_HUMONGOUS
 *
 * @run main/othervm -Xms128M -Xmx128M -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:G1HeapRegionSize=1M
 *                   gc.g1.humongousObjects.TestHumongousNonArrayAllocation SMALLEST_HUMONGOUS
 *
 * @run main/othervm -Xms128M -Xmx128M -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:G1HeapRegionSize=1M
 *                   gc.g1.humongousObjects.TestHumongousNonArrayAllocation ONE_REGION_HUMONGOUS
 *
 * @run main/othervm -Xms128M -Xmx128M -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:G1HeapRegionSize=1M
 *                   gc.g1.humongousObjects.TestHumongousNonArrayAllocation TWO_REGION_HUMONGOUS
 *
 * @run main/othervm -Xms128M -Xmx128M -XX:+UseG1GC -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:.
 *                   -XX:G1HeapRegionSize=1M
 *                   gc.g1.humongousObjects.TestHumongousNonArrayAllocation MORE_THAN_TWO_REGION_HUMONGOUS
 *
 */

/**
 * The test for objects which are instances of classes with a huge amount of fields. It's an alternative way to create
 * a humongous object rather to allocate a long array.
 * The size of a class object depends on the field declared in the class. So, the tests generates such classes to cover
 * the following cases:
 * largest non-humongous object (exactly half a region)
 * smallest humongous object (half a region + sizeof(long))
 * humongous object that takes exactly one region
 * humongous object that takes more than one region (region + sizeof(long))
 * humongous object that takes more than two regions (region * 2 + sizeof(long))
 *
 */
public class TestHumongousNonArrayAllocation {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    private static final String CLASS_NAME_PREFIX = TestHumongousNonArrayAllocation.class.getSimpleName() + "_";

    public static void main(String[] args) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException, IOException {

        if (args.length != 1) {
            throw new Error("Test Bug: Expected class name wasn't provided as command line argument");
        }
        G1SampleClass sampleClass = G1SampleClass.valueOf(args[0]);

        Path wrkDir = Files.createTempDirectory(Paths.get(""), CLASS_NAME_PREFIX);
        URL[] url = {wrkDir.toUri().toURL()};
        URLClassLoader urlLoader = new URLClassLoader(url);

        Object sampleObject;
        try {
            sampleObject = sampleClass.getCls(urlLoader, wrkDir, CLASS_NAME_PREFIX).newInstance();
        } catch (Throwable throwable) {
            throw new AssertionError("Test Bug: Cannot create object of provided class", throwable);
        }

        boolean isHumongous = WB.g1IsHumongous(sampleObject);
        boolean shouldBeHumongous = (sampleClass.expectedInstanceSize() > (WB.g1RegionSize() / 2));

        // Sanity check
        Asserts.assertEquals(WB.getObjectSize(sampleObject), sampleClass.expectedInstanceSize(),
                String.format("Test Bug: Object of class %s is expected to take %d bytes but it takes %d.",
                        sampleClass.name(), sampleClass.expectedInstanceSize(), WB.getObjectSize(sampleObject)));

        // Test check
        Asserts.assertEquals(isHumongous, shouldBeHumongous,
                String.format("Object of class %s is expected to be %shumongous but it is not",
                        sampleClass.name(), (shouldBeHumongous ? "" : "non-")));
    }

}
