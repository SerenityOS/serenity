/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8054823
 * @summary Tests the setFlag and printFlag attach command
 * @requires vm.flagless
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.management
 *          jdk.attach/sun.tools.attach
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @run driver AttachSetGetFlag
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import sun.tools.attach.HotSpotVirtualMachine;

import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import com.sun.tools.attach.VirtualMachine;

public class AttachSetGetFlag {

  public static void main(String... args) throws Exception {
    // Test a manageable uintx flag.
    testGetFlag("MaxHeapFreeRatio", "60");
    testSetFlag("MaxHeapFreeRatio", "50", "60");

    // Test a non-manageable size_t flag.
    // Since it is not manageable, we can't test the setFlag functionality.
    testGetFlag("ArrayAllocatorMallocLimit", "128");
    // testSetFlag("ArrayAllocatorMallocLimit", "64", "128");

    // Test a uint flag.
    testGetFlag("ParallelGCThreads", "10");
  }

  public static ProcessBuilder runTarget(String flagName, String flagValue) throws Exception {
    return ProcessTools.createJavaProcessBuilder(
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:" + flagName + "=" + flagValue,
        "AttachSetGetFlag$Target");
  }

  public static void testGetFlag(String flagName, String flagValue) throws Exception {
    ProcessBuilder pb = runTarget(flagName, flagValue);

    Process target = pb.start();

    try {
      waitForReady(target);

      int pid = (int)target.pid();

      HotSpotVirtualMachine vm = (HotSpotVirtualMachine)VirtualMachine.attach(((Integer)pid).toString());

      // Test Get
      BufferedReader remoteDataReader = new BufferedReader(new InputStreamReader(
          vm.printFlag(flagName)));

      boolean foundExpectedLine = false;

      String line = null;
      while((line = remoteDataReader.readLine()) != null) {
        System.out.println("printFlag: " + line);
        if (line.equals("-XX:" + flagName + "=" + flagValue)) {
          foundExpectedLine = true;
        }
      }

      Asserts.assertTrue(foundExpectedLine, "Didn't get the expected output: '-XX:" + flagName + "=" + flagValue + "'");

      vm.detach();
    }
    finally {
      target.destroy();
      target.waitFor();
    }
  }

  public static void testSetFlag(String flagName, String initialFlagValue, String flagValue) throws Exception {
    ProcessBuilder pb = runTarget(flagName, initialFlagValue);

    Process target = pb.start();

    try {
      waitForReady(target);

      int pid = (int)target.pid();

      HotSpotVirtualMachine vm = (HotSpotVirtualMachine)VirtualMachine.attach(((Integer)pid).toString());

      // First set the value.
      BufferedReader remoteDataReader = new BufferedReader(new InputStreamReader(
          vm.setFlag(flagName, flagValue)));

      String line;
      while((line = remoteDataReader.readLine()) != null) {
        System.out.println("setFlag: " + line);
        // Just empty the stream.
      }
      remoteDataReader.close();

      // Then read and make sure we get back the set value.
      remoteDataReader = new BufferedReader(new InputStreamReader(vm.printFlag(flagName)));

      boolean foundExpectedLine = false;
      line = null;
      while((line = remoteDataReader.readLine()) != null) {
        System.out.println("getFlag: " + line);
        if (line.equals("-XX:" + flagName + "=" + flagValue)) {
          foundExpectedLine = true;
        }
      }

      Asserts.assertTrue(foundExpectedLine, "Didn't get the expected output: '-XX:" + flagName + "=" + flagValue + "'");

      vm.detach();

    } finally {
      target.destroy();
      target.waitFor();
    }
  }

  private static void waitForReady(Process target) throws Exception {
    InputStream os = target.getInputStream();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(os))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if ("Ready".equals(line)) {
          return;
        }
      }
    }
  }


  public static class Target {
    public static void main(String [] args) throws Exception {
      System.out.println("Ready");
      System.out.flush();
      while (true) {
        Thread.sleep(1000);
      }
    }
  }
}
