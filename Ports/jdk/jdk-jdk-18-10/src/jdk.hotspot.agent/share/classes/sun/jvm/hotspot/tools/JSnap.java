/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.stream.*;
import sun.jvm.hotspot.debugger.JVMDebugger;
import sun.jvm.hotspot.runtime.*;

public class JSnap extends Tool {

    private boolean all;

    public JSnap() {
        super();
    }

    public JSnap(JVMDebugger d) {
        super(d);
    }

    public void run() {
        final PrintStream out = System.out;
        if (PerfMemory.initialized()) {
            PerfDataPrologue prologue = PerfMemory.prologue();
            if (prologue.accessible()) {
                PerfMemory.iterate(new PerfMemory.PerfDataEntryVisitor() {
                        public boolean visit(PerfDataEntry pde) {
                            if (all || pde.supported()) {
                                out.print(pde.name());
                                out.print('=');
                                out.println(pde.valueAsString());
                            }
                            // goto next entry
                            return true;
                        }
                    });
            } else {
                out.println("PerfMemory is not accessible");
            }
        } else {
            out.println("PerfMemory is not initialized");
        }
    }

    @Override
    protected void printFlagsUsage() {
        System.out.println("    -a\tto print all performance counters");
        super.printFlagsUsage();
    }

    public static void main(String[] args) {
        JSnap js = new JSnap();
        js.all = Arrays.stream(args)
                       .anyMatch(s -> s.equals("-a"));

        if (js.all) {
            args = Arrays.stream(args)
                         .filter(s -> !s.equals("-a"))
                         .collect(Collectors.toList())
                         .toArray(new String[0]);
        }

        js.execute(args);
    }
}
