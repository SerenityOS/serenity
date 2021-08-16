/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.oops.*;

public class DebugServer {
  private void usage() {
    System.out.println("usage: java " + getClass().getName() + " <pid> [server id]");
    System.out.println("   or: java " + getClass().getName() + " <executable> <core> [server id]");
    System.out.println("\"pid\" must be the process ID of a HotSpot process.");
    System.out.println("If reading a core file, \"executable\" must (currently) be the");
    System.out.println("full path name to the precise java executable which generated");
    System.out.println("the core file (not, on Solaris, the \"java\" wrapper script in");
    System.out.println("the \"bin\" subdirectory of the JDK.)");
    System.out.println("The \"server id\" is a unique name for a specific remote debuggee.");
    System.exit(1);
  }

  public static void main(String[] args) {
    new DebugServer().run(args);
  }

  private void run(String[] args) {
    if ((args.length < 1) || (args.length > 3)) {
      usage();
    }

    // Attempt to handle "-h" or "-help"
    if (args[0].startsWith("-")) {
      usage();
    }

    int pid = 0;
    boolean usePid = false;
    String coreFileName = null;
    // FIXME: would be nice to pick this up from the core file
    // somehow, but that doesn't look possible. Should at least figure
    // it out from a path to the JDK.
    String javaExecutableName = null;
    String serverID = null;

    switch (args.length) {
       case 1:
         try {
           pid = Integer.parseInt(args[0]);
           usePid = true;
         } catch (NumberFormatException e) {
           usage();
         }
         break;

       case 2:
         // either we have pid and server id or exec file and core file
         try {
           pid = Integer.parseInt(args[0]);
           usePid = true;
           serverID = args[1];
         } catch (NumberFormatException e) {
           pid = -1;
           usePid = false;
           javaExecutableName = args[0];
           coreFileName = args[1];
         }
         break;

       case 3:
         javaExecutableName = args[0];
         coreFileName = args[1];
         serverID = args[2];
         break;

       default:
         // should not happend, taken care already.
         break;
    }

    final HotSpotAgent agent = new HotSpotAgent();
    try {
      if (usePid) {
        System.err.println("Attaching to process ID " + pid + " and starting RMI services, please wait...");
        agent.startServer(pid, serverID, null);
      } else {
        System.err.println("Attaching to core " + coreFileName +
                           " from executable " + javaExecutableName + " and starting RMI services, please wait...");
        agent.startServer(javaExecutableName, coreFileName, serverID, null);
      }
    }
    catch (DebuggerException e) {
      if (usePid) {
        System.err.print("Error attaching to process or starting server: ");
      } else {
        System.err.print("Error attaching to core file or starting server: ");
      }
      e.printStackTrace();
      System.exit(1);
    }

    // shutdown hook to clean-up the server in case of forced exit.
    Runtime.getRuntime().addShutdownHook(new java.lang.Thread(
                          new Runnable() {
                             public void run() {
                                agent.shutdownServer();
                             }
                          }));
    System.err.println("Debugger attached and RMI services started.");
  }
}
