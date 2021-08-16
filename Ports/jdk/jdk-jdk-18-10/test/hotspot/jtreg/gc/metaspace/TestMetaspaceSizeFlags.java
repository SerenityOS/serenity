/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc.metaspace;

import jdk.test.lib.Asserts;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

/*
 * @test TestMetaspaceSizeFlags
 * @bug 8024650
 * @summary Test that metaspace size flags can be set correctly
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver gc.metaspace.TestMetaspaceSizeFlags
 */
public class TestMetaspaceSizeFlags {
  public static final long K = 1024L;
  public static final long M = 1024L * K;

  // HotSpot uses a number of different values to align memory size flags.
  // This is currently the largest alignment (unless huge large pages are used).
  public static final long MAX_ALIGNMENT = 32 * M;

  public static void main(String [] args) throws Exception {
    testMaxMetaspaceSizeEQMetaspaceSize(MAX_ALIGNMENT, MAX_ALIGNMENT);
    // 8024650: MaxMetaspaceSize was adjusted instead of MetaspaceSize.
    testMaxMetaspaceSizeLTMetaspaceSize(MAX_ALIGNMENT, MAX_ALIGNMENT * 2);
    testMaxMetaspaceSizeGTMetaspaceSize(MAX_ALIGNMENT * 2, MAX_ALIGNMENT);
  }

  private static void testMaxMetaspaceSizeEQMetaspaceSize(long maxMetaspaceSize, long metaspaceSize) throws Exception {
    MetaspaceFlags mf = runAndGetValue(maxMetaspaceSize, metaspaceSize);
    Asserts.assertEQ(maxMetaspaceSize, metaspaceSize);
    Asserts.assertEQ(mf.maxMetaspaceSize, maxMetaspaceSize);
    Asserts.assertEQ(mf.metaspaceSize, metaspaceSize);
  }

  private static void testMaxMetaspaceSizeLTMetaspaceSize(long maxMetaspaceSize, long metaspaceSize) throws Exception {
    MetaspaceFlags mf = runAndGetValue(maxMetaspaceSize, metaspaceSize);
    Asserts.assertEQ(mf.maxMetaspaceSize, maxMetaspaceSize);
    Asserts.assertEQ(mf.metaspaceSize, maxMetaspaceSize);
  }

  private static void testMaxMetaspaceSizeGTMetaspaceSize(long maxMetaspaceSize, long metaspaceSize) throws Exception {
    MetaspaceFlags mf = runAndGetValue(maxMetaspaceSize, metaspaceSize);
    Asserts.assertGT(maxMetaspaceSize, metaspaceSize);
    Asserts.assertGT(mf.maxMetaspaceSize, mf.metaspaceSize);
    Asserts.assertEQ(mf.maxMetaspaceSize, maxMetaspaceSize);
    Asserts.assertEQ(mf.metaspaceSize, metaspaceSize);
  }

  private static MetaspaceFlags runAndGetValue(long maxMetaspaceSize, long metaspaceSize) throws Exception {
    OutputAnalyzer output = run(maxMetaspaceSize, metaspaceSize);
    output.shouldNotMatch("Error occurred during initialization of VM\n.*");

    String stringMaxMetaspaceSize = output.firstMatch(".* MaxMetaspaceSize .* = (\\d+).*", 1);
    String stringMetaspaceSize = output.firstMatch(".* MetaspaceSize .* = (\\d+).*", 1);

    return new MetaspaceFlags(Long.parseLong(stringMaxMetaspaceSize),
                              Long.parseLong(stringMetaspaceSize));
  }

  private static OutputAnalyzer run(long maxMetaspaceSize, long metaspaceSize) throws Exception {
    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
        "-XX:MaxMetaspaceSize=" + maxMetaspaceSize,
        "-XX:MetaspaceSize=" + metaspaceSize,
        "-XX:-UseLargePages", // Prevent us from using 2GB large pages on solaris + sparc.
        "-XX:+PrintFlagsFinal",
        "-version");
    return new OutputAnalyzer(pb.start());
  }

  private static class MetaspaceFlags {
    public long maxMetaspaceSize;
    public long metaspaceSize;
    public MetaspaceFlags(long maxMetaspaceSize, long metaspaceSize) {
      this.maxMetaspaceSize = maxMetaspaceSize;
      this.metaspaceSize = metaspaceSize;
    }
  }
}
