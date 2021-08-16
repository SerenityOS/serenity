/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;
import sun.jvm.hotspot.debugger.remote.*;
import sun.jvm.hotspot.utilities.PlatformInfo;

public class PMap extends Tool {

   public PMap() {
       super();
   }

   public PMap(JVMDebugger d) {
       super(d);
   }

   public PMap(HotSpotAgent agent) {
       super(agent);
   }

   @Override
   public String getName() {
       return "pmap";
   }

   public void run() {
      run(System.out);
   }

   public void run(PrintStream out) {
      run(out, getAgent().getDebugger());
   }

   public void run(PrintStream out, Debugger dbg) {
      CDebugger cdbg = dbg.getCDebugger();
      if (cdbg != null) {
         List<LoadObject> l = cdbg.getLoadObjectList();
         Iterator<LoadObject> itr = l.iterator();
         if (!itr.hasNext() && PlatformInfo.getOS().equals("darwin")) {
             // If the list is empty, we assume we attached to a process, and on OSX we can only
             // get LoadObjects for a core file.
             out.println("Not available for Mac OS X processes");
             return;
         }
         while (itr.hasNext()) {
            LoadObject lo = itr.next();
            out.print(lo.getBase() + "\t");
            out.print(lo.getSize()/1024 + "K\t");
            out.println(lo.getName());
         }
      } else {
          if (getDebugeeType() == DEBUGEE_REMOTE) {
              out.print(((RemoteDebuggerClient)dbg).execCommandOnServer("pmap", null));
          } else {
              out.println("not yet implemented (debugger does not support CDebugger)!");
          }
      }
   }

   public static void main(String[] args) throws Exception {
      PMap t = new PMap();
      t.execute(args);
   }
}
