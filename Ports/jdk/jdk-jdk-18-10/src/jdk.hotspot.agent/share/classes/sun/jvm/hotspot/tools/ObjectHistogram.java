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

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;

import java.io.PrintStream;

/** A sample tool which uses the Serviceability Agent's APIs to obtain
    an object histogram from a remote or crashed VM. */
public class ObjectHistogram extends Tool {

    public ObjectHistogram() {
        super();
    }

    public ObjectHistogram(JVMDebugger d) {
        super(d);
    }

    @Override
    public String getName() {
        return "objectHistogram";
    }

    public void run() {
        run(System.out, System.err);
    }

    public void run(PrintStream out, PrintStream err) {
        // Ready to go with the database...
        ObjectHeap heap = VM.getVM().getObjectHeap();
        sun.jvm.hotspot.oops.ObjectHistogram histogram =
        new sun.jvm.hotspot.oops.ObjectHistogram();
        err.println("Iterating over heap. This may take a while...");
        long startTime = System.currentTimeMillis();
        heap.iterate(histogram);
        long endTime = System.currentTimeMillis();
        histogram.printOn(out);
        float secs = (float) (endTime - startTime) / 1000.0f;
        err.println("Heap traversal took " + secs + " seconds.");
    }

    public static void main(String[] args) {
        ObjectHistogram oh = new ObjectHistogram();
        oh.execute(args);
    }
}
