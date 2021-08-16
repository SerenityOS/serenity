/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import utils.Utils;
import java.util.concurrent.CountDownLatch;

/*
 * @test JstackThreadTest
 * @bug 8151442
 * @summary jstack doesn't close quotation marks properly with threads' name greater than 1996 characters
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run main JstackThreadTest
 */
public class JstackThreadTest {
  static class NamedThread extends Thread {
   CountDownLatch latch;
   NamedThread(String name, CountDownLatch latch) {
      this.latch = latch;
      setName(name);

    }
    @Override
    public void run() {
     latch.countDown();
     Utils.sleep();
    }
   }

  public static void main(String[] args) throws Exception {
    StringBuilder sb = new StringBuilder();
     /*create a string more than 1996 character */
    for(int i = 0; i < 1998; i++){
      sb.append("a");
    }
    testWithName(sb.toString());
  }

  private static void testWithName(String name) throws Exception {
    //parent thread countDown latch
    CountDownLatch latch = new CountDownLatch(1);
    // Start a thread with a long thread name
    NamedThread thread = new NamedThread(name, latch);
    thread.setDaemon(true);
    thread.start();
    ProcessBuilder processBuilder = new ProcessBuilder();
    JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jstack");
    launcher.addVMArgs(jdk.test.lib.Utils.getTestJavaOpts());
    launcher.addToolArg("-l");
    launcher.addToolArg(Long.toString(ProcessTools.getProcessId()));
    processBuilder.command(launcher.getCommand());
    System.out.println(Arrays.toString(processBuilder.command().toArray()).replace(",", ""));
    // Ensuring that Jstack will always run after NamedThread
    latch.await();
    OutputAnalyzer output = ProcessTools.executeProcess(processBuilder);
    System.out.println(output.getOutput());
    output.shouldContain("\""+ name + "\"");
  }
}
