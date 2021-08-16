/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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
import sun.jvm.hotspot.debugger.JVMDebugger;
import sun.jvm.hotspot.runtime.*;

public class FlagDumper extends Tool {

    public FlagDumper() {
        super();
    }

    public FlagDumper(JVMDebugger d) {
        super(d);
    }

   public void run() {
      VM.Flag[] flags = VM.getVM().getCommandLineFlags();
      PrintStream out = System.out;
      if (flags == null) {
         out.println("Command Flags info not available! (use 1.4.1_03 or later)");
      } else {
         for (int f = 0; f < flags.length; f++) {
            out.print(flags[f].getName());
            out.print(" = ");
            out.println(flags[f].getValue());
         }
      }
   }

   public static void main(String[] args) {
      FlagDumper fd = new FlagDumper();
      fd.execute(args);
   }
}
