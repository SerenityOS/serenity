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

/*
 * @test
 * @bug 7162400
 * @summary Regression test for attach issue where stale pid files in /tmp lead to connection issues
 * @requires os.family != "windows"
 * @requires vm.flagless
 * @modules java.base/jdk.internal.misc:open
 * @modules java.base/java.lang:open
 * @modules jdk.attach/sun.tools.attach
 * @library /test/lib
 * @run driver AttachWithStalePidFile
 */

import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import com.sun.tools.attach.VirtualMachine;
import sun.tools.attach.HotSpotVirtualMachine;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.io.*;

public class AttachWithStalePidFile {
  public static void main(String... args) throws Exception {

    // Since there might be stale pid-files owned by different
    // users on the system we may need to retry the test in case we
    // are unable to remove the existing file.
    int retries = 5;
    while(!runTest() && --retries > 0);

    if(retries == 0) {
      throw new RuntimeException("Test failed after 5 retries. " +
        "Remove any /tmp/.java_pid* files and retry.");
    }
  }

  public static boolean runTest() throws Exception {
    ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
      "-XX:+UnlockDiagnosticVMOptions", "-XX:+PauseAtStartup", "AttachWithStalePidFileTarget");
    Process target = pb.start();
    Path pidFile = null;

    try {
      int pid = getUnixProcessId(target);

      // create the stale .java_pid file. use hard-coded /tmp path as in th VM
      pidFile = createJavaPidFile(pid);
      if(pidFile == null) {
        return false;
      }

      // wait for vm.paused file to be created and delete it once we find it.
      waitForAndResumeVM(pid);

      waitForTargetReady(target);

      HotSpotVirtualMachine vm = (HotSpotVirtualMachine)VirtualMachine.attach(((Integer)pid).toString());
      BufferedReader remoteDataReader = new BufferedReader(new InputStreamReader(vm.remoteDataDump()));
      String line = null;
      while((line = remoteDataReader.readLine()) != null);

      vm.detach();
      return true;
    }
    finally {
      target.destroy();
      target.waitFor();

      if(pidFile != null && Files.exists(pidFile)) {
        Files.delete(pidFile);
      }
    }
  }

  private static void waitForTargetReady(Process target) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(target.getInputStream()));
    String line = br.readLine();
    // wait for the ready message having been printed or EOF (line == null)
    while (line != null && !line.equals(AttachWithStalePidFileTarget.READY_MSG)) {
        line = br.readLine();
    }
    // target VM ready
  }

  private static Path createJavaPidFile(int pid) throws Exception {
    Path pidFile = Paths.get("/tmp/.java_pid" + pid);
    if(Files.exists(pidFile)) {
      try {
        Files.delete(pidFile);
      }
      catch(FileSystemException e) {
        if(e.getReason().matches("Operation not permitted|Not owner")) {
          System.out.println("Unable to remove exisiting stale PID file" + pidFile);
          System.out.println("===================================================");
          e.printStackTrace(System.out);
          return null;
        }
        throw e;
      }
    }
    return Files.createFile(pidFile,
      PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
  }

  private static void waitForAndResumeVM(int pid) throws Exception {
    Path pauseFile = Paths.get("vm.paused." + pid);
    int retries = 60;
    while(!Files.exists(pauseFile) && --retries > 0) {
      Thread.sleep(1000);
    }
    if(retries == 0) {
      throw new RuntimeException("Timeout waiting for VM to start. " +
        "vm.paused file not created within 60 seconds.");
    }
    Files.delete(pauseFile);
  }

  private static int getUnixProcessId(Process unixProcess) throws Exception {
    Field pidField = unixProcess.getClass().getDeclaredField("pid");
    pidField.setAccessible(true);
    return (Integer)pidField.get(unixProcess);
  }
}
