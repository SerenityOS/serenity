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
 *
 */

/*
 * @test
 * @summary Run the CustomFJPoolTest in dynamic CDSarchive mode.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 *          /test/hotspot/jtreg/runtime/cds/appcds/dynamicArchive/test-classes
 * @compile ../../../../../../jdk/java/util/stream/CustomFJPoolTest.java
 *          test-classes/TestStreamApp.java
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run testng/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. CDSStreamTestDriver
 */

import org.testng.annotations.Test;
import java.io.File;
import jtreg.SkippedException;
import sun.hotspot.gc.GC;

@Test
public class CDSStreamTestDriver extends DynamicArchiveTestBase {
    @Test
    public void testMain() throws Exception {
        runTest(CDSStreamTestDriver::doTest);
    }

    private static final String classDir = System.getProperty("test.classes");
    private static final String mainClass = "TestStreamApp";
    private static final String javaClassPath = System.getProperty("java.class.path");
    private static final String ps = System.getProperty("path.separator");
    private static final String skippedException = "jtreg.SkippedException: Unable to map shared archive: test did not complete";

    static void doTest() throws Exception {
        String topArchiveName = getNewArchiveName();
        String appJar = JarBuilder.build("streamapp", new File(classDir), null);

        String[] classPaths = javaClassPath.split(File.pathSeparator);
        String testngJar = null;
        for (String path : classPaths) {
            if (path.endsWith("testng.jar")) {
                testngJar = path;
                break;
            }
        }

        String[] testClassNames = { "CustomFJPoolTest" };

        for (String className : testClassNames) {
            try {
            dumpAndRun(topArchiveName, "-Xlog:cds,cds+dynamic=debug,class+load=trace",
                "-cp", appJar + ps + testngJar,
                mainClass, className);
           } catch (SkippedException s) {
               if (GC.Z.isSelected() && s.toString().equals(skippedException)) {
                   System.out.println("Got " + s.toString() + " as expected.");
                   System.out.println("Because the test was run with ZGC with UseCompressedOops and UseCompressedClassPointers disabled,");
                   System.out.println("but the base archive was created with the options enabled");
              } else {
                   throw new RuntimeException("Archive mapping should always succeed after JDK-8231610 (did the machine run out of memory?)");
              }
           }
        }
    }
}
