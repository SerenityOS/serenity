/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package gc.arguments;

/**
 * @test TestMinInitialErgonomics
 * @bug 8006088
 * @requires vm.gc.Parallel
 * @summary Test Parallel GC ergonomics decisions related to minimum and initial heap size.
 * @library /test/lib
 * @library /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver gc.arguments.TestMinInitialErgonomics
 * @author thomas.schatzl@oracle.com
 */

public class TestMinInitialErgonomics {

  public static void main(String args[]) throws Exception {
    final String gcName = "-XX:+UseParallelGC";
    // check ergonomic decisions about minimum and initial heap size in
    // a single gc only as ergonomics are the same everywhere.
    TestMaxHeapSizeTools.checkMinInitialErgonomics(gcName);
  }
}

