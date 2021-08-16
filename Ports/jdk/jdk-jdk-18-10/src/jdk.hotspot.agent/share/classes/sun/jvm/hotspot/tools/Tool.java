/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.tools;

import java.io.PrintStream;

import sun.jvm.hotspot.HotSpotAgent;
import sun.jvm.hotspot.debugger.DebuggerException;
import sun.jvm.hotspot.debugger.JVMDebugger;
import sun.jvm.hotspot.runtime.VM;

// generic command line or GUI tool.
// override run & code main as shown below.

public abstract class Tool implements Runnable {
   private HotSpotAgent agent;
   private JVMDebugger jvmDebugger;
   private int debugeeType;

   // debugeeType is one of constants below
   protected static final int DEBUGEE_PID    = 0;
   protected static final int DEBUGEE_CORE   = 1;
   protected static final int DEBUGEE_REMOTE = 2;

   public Tool() {
   }

   public Tool(JVMDebugger d) {
      jvmDebugger = d;
   }

   public Tool(HotSpotAgent agent) {
      this.agent = agent;
      if (agent == null) {
          jvmDebugger = null;
          debugeeType = -1;
      } else {
          jvmDebugger = agent.getDebugger();
          debugeeType = switch (agent.getStartupMode()) {
              case HotSpotAgent.PROCESS_MODE   -> DEBUGEE_PID;
              case HotSpotAgent.CORE_FILE_MODE -> DEBUGEE_CORE;
              case HotSpotAgent.REMOTE_MODE    -> DEBUGEE_REMOTE;
              default -> throw new IllegalStateException("Invalid attach mode");
          };
      }
   }

   public String getName() {
      return getClass().getName();
   }

   protected boolean needsJavaPrefix() {
      return true;
   }

   protected void setAgent(HotSpotAgent a) {
      agent = a;
   }

   protected void setDebugeeType(int dt) {
      debugeeType = dt;
   }

   protected HotSpotAgent getAgent() {
      return agent;
   }

   protected int getDebugeeType() {
      return debugeeType;
   }

   protected void printUsage() {
      String name = null;
      if (needsJavaPrefix()) {
         name = "java " + getName();
      } else {
         name = getName();
      }
      System.out.println("Usage: " + name + " [option] <pid>");
      System.out.println("\t\t(to connect to a live java process)");
      System.out.println("   or " + name + " [option] <executable> <core>");
      System.out.println("\t\t(to connect to a core file)");
      System.out.println("   or " + name + " [option] [server_id@]<remote server IP or hostname>");
      System.out.println("\t\t(to connect to a remote debug server)");
      System.out.println();
      System.out.println("where option must be one of:");
      printFlagsUsage();
   }

   protected void printFlagsUsage() {
       System.out.println("    -h | -help\tto print this help message");
   }

   protected void usage() {
      printUsage();
   }

   /*
      Derived class main should be of the following form:

      public static void main(String[] args) {
         <derived class> obj = new <derived class>;
         obj.execute(args);
      }

   */

   protected void execute(String[] args) {
       int returnStatus = 1;

       try {
           returnStatus = start(args);
       } catch (Throwable t) {
           t.printStackTrace(System.err);
       } finally {
           stop();
       }

       // Exit with 0 or 1
       System.exit(returnStatus);
   }

   public void stop() {
      if (agent != null) {
         agent.detach();
      }
   }

   private int start(String[] args) {

      if ((args.length < 1) || (args.length > 2)) {
         usage();
         return 1;
      }

      // Attempt to handle -h or -help or some invalid flag
      if (args[0].startsWith("-h")) {
          usage();
          return 0;
      } else if (args[0].startsWith("-")) {
          usage();
          return 1;
      }

      PrintStream err = System.err;
      PrintStream out = System.out;

      int pid = 0;
      String coreFileName   = null;
      String executableName = null;
      String remoteServer   = null;

      switch (args.length) {
        case 1:
           try {
              pid = Integer.parseInt(args[0]);
              debugeeType = DEBUGEE_PID;
           } catch (NumberFormatException e) {
              // try remote server
              remoteServer = args[0];
              debugeeType  = DEBUGEE_REMOTE;
           }
           break;

        case 2:
           executableName = args[0];
           coreFileName   = args[1];
           debugeeType    = DEBUGEE_CORE;
           break;

        default:
           usage();
           return 1;
      }

      agent = new HotSpotAgent();
      try {
        switch (debugeeType) {
          case DEBUGEE_PID:
             out.println("Attaching to process ID " + pid + ", please wait...");
             agent.attach(pid);
             break;

          case DEBUGEE_CORE:
             out.println("Attaching to core " + coreFileName +
                         " from executable " + executableName + ", please wait...");
             agent.attach(executableName, coreFileName);
             break;

          case DEBUGEE_REMOTE:
             out.println("Attaching to remote server " + remoteServer + ", please wait...");
             agent.attach(remoteServer);
             break;
        }
      }
      catch (DebuggerException e) {
        switch (debugeeType) {
          case DEBUGEE_PID:
             err.print("Error attaching to process: ");
             break;

          case DEBUGEE_CORE:
             err.print("Error attaching to core file: ");
             break;

          case DEBUGEE_REMOTE:
             err.print("Error attaching to remote server: ");
             break;
        }
        if (e.getMessage() != null) {
          err.println(e.getMessage());
          e.printStackTrace();
        }
        err.println();
        return 1;
      }

      out.println("Debugger attached successfully.");
      startInternal();
      return 0;
   }

   // When using an existing JVMDebugger.
   public void start() {

      if (jvmDebugger == null) {
         throw new RuntimeException("Tool.start() called with no JVMDebugger set.");
      }
      agent = new HotSpotAgent();
      agent.attach(jvmDebugger);
      startInternal();
   }

   // Remains of the start mechanism, common to both start methods.
   private void startInternal() {

      PrintStream out = System.out;
      VM vm = VM.getVM();
      if (vm.isCore()) {
        out.println("Core build detected.");
      } else if (vm.isClientCompiler()) {
        out.println("Client compiler detected.");
      } else if (vm.isServerCompiler()) {
        out.println("Server compiler detected.");
      } else {
        throw new RuntimeException("Fatal error: "
            + "should have been able to detect core/C1/C2 build");
      }

      String version = vm.getVMRelease();
      if (version != null) {
        out.print("JVM version is ");
        out.println(version);
      }

      run();
   }
}
