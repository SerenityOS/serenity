/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test TestMaxRAMFlags
 * @bug 8222252
 * @summary Verify correct MaxHeapSize and UseCompressedOops when MaxRAM and MaxRAMPercentage
 * are specified.
 * @library /test/lib
 * @library /
 * @requires vm.bits == "64"
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run driver gc.arguments.TestMaxRAMFlags
 * @author bob.vandette@oracle.com
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.Arrays;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestMaxRAMFlags {

  private static void checkMaxRAMSize(long maxram, int maxrampercent, boolean forcecoop, long expectheap, boolean expectcoop) throws Exception {

    ArrayList<String> args = new ArrayList<String>();
    args.add("-XX:MaxRAM=" + maxram);
    args.add("-XX:MaxRAMPercentage=" + maxrampercent);
    if (forcecoop) {
      args.add("-XX:+UseCompressedOops");
    }

    args.add("-XX:+PrintFlagsFinal");
    args.add("-version");

    ProcessBuilder pb = GCArguments.createJavaProcessBuilder(args);
    OutputAnalyzer output = new OutputAnalyzer(pb.start());
    output.shouldHaveExitValue(0);
    String stdout = output.getStdout();

    long actualheap = new Long(getFlagValue("MaxHeapSize", stdout)).longValue();
    if (actualheap != expectheap) {
      throw new RuntimeException("MaxHeapSize value set to " + actualheap +
        ", expected " + expectheap + " when running with the following flags: " + Arrays.asList(args).toString());
    }

    boolean actualcoop = getFlagBoolValue("UseCompressedOops", stdout);
    if (actualcoop != expectcoop) {
      throw new RuntimeException("UseCompressedOops set to " + actualcoop +
        ", expected " + expectcoop + " when running with the following flags: " + Arrays.asList(args).toString());
    }
  }

  private static long getHeapBaseMinAddress() throws Exception {
    ArrayList<String> args = new ArrayList<String>();
    args.add("-XX:+PrintFlagsFinal");
    args.add("-version");

    ProcessBuilder pb = GCArguments.createJavaProcessBuilder(args);
    OutputAnalyzer output = new OutputAnalyzer(pb.start());
    output.shouldHaveExitValue(0);
    String stdout = output.getStdout();
    return (new Long(getFlagValue("HeapBaseMinAddress", stdout)).longValue());
  }

  private static String getFlagValue(String flag, String where) {
    Matcher m = Pattern.compile(flag + "\\s+:?=\\s+\\d+").matcher(where);
    if (!m.find()) {
      throw new RuntimeException("Could not find value for flag " + flag + " in output string");
    }
    String match = m.group();
    return match.substring(match.lastIndexOf(" ") + 1, match.length());
  }

  private static boolean getFlagBoolValue(String flag, String where) {
    Matcher m = Pattern.compile(flag + "\\s+:?= (true|false)").matcher(where);
    if (!m.find()) {
      throw new RuntimeException("Could not find value for flag " + flag + " in output string");
    }
    return m.group(1).equals("true");
  }

  public static void main(String args[]) throws Exception {
    // Tests
    // 1. Verify that MaxRAMPercentage overrides UseCompressedOops Ergo
    // 2. Verify that UseCompressedOops forces compressed oops limit even
    //    when other flags are specified.

    long oneG = 1L * 1024L * 1024L * 1024L;

    // Hotspot startup logic reduces MaxHeapForCompressedOops by HeapBaseMinAddress
    // in order to get zero based compressed oops offsets.
    long heapbaseminaddr = getHeapBaseMinAddress();
    long maxcoopheap = TestUseCompressedOopsErgoTools.getMaxHeapForCompressedOops(new String [0]) - heapbaseminaddr;

    // Args: MaxRAM , MaxRAMPercentage, forcecoop, expect heap, expect coop
    checkMaxRAMSize(maxcoopheap - oneG, 100, false, maxcoopheap - oneG, true);
    checkMaxRAMSize(maxcoopheap + oneG, 100, false, maxcoopheap + oneG, false);
    checkMaxRAMSize(maxcoopheap + oneG, 100, true, maxcoopheap, true);
  }
}
