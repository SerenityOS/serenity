/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Load app classes from CDS archive in parallel threads. Similar to ParallelLoad.java, but each class in its own JAR
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/ParallelLoad.java
 * @compile test-classes/ParallelClasses.java
 * @run driver ParallelLoad2
 */

import java.io.File;

public class ParallelLoad2 {
  public static int MAX_CLASSES = 40;
  public static void main(String[] args) throws Exception {
    JarBuilder.build("parallel_load2", "ParallelLoad", "ParallelLoadThread", "ParallelLoadWatchdog");
    for (int i=0; i<MAX_CLASSES; i++) {
      JarBuilder.build("parallel_load2_" + i, "ParallelClass" + i);
    }

    String cp = TestCommon.getTestJar("parallel_load2.jar");
    for (int i=0; i<MAX_CLASSES; i++) {
      cp += File.pathSeparator + TestCommon.getTestJar("parallel_load2_" + i + ".jar");
    }

    String[] class_list = new String[MAX_CLASSES + 2];
    for (int i=0; i<MAX_CLASSES; i++) {
      class_list[i] = "ParallelClass" + i;
    }
    class_list[class_list.length - 1] = "ParallelLoad";
    class_list[class_list.length - 2] = "ParallelLoadThread";

    TestCommon.test(cp, class_list,
                          "ParallelLoad");
  }
}
