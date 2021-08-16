/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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


import java.io.PrintStream;
import java.util.*;
import java.lang.management.*;
import bootreporter.*;

public class NativeMethodPrefixApp implements StringIdCallback {

    // This test is fragile like a golden file test.
    // It assumes that a specific non-native library method will call a specific
    // native method.  The below may need to be updated based on library changes.
    static String goldenNativeMethodName = "getStartupTime";

    static boolean gotIt[] = {false, false, false};

    public static void main(String args[]) throws Exception {
        (new NativeMethodPrefixApp()).run(args, System.err);
    }

    public void run(String args[], PrintStream out) throws Exception {
        StringIdCallbackReporter.registerCallback(this);
        System.err.println("start");

        java.lang.reflect.Array.getLength(new short[5]);
        RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();
        System.err.println(mxbean.getVmVendor());

        for (int i = 0; i < gotIt.length; ++i) {
            if (!gotIt[i]) {
                throw new Exception("ERROR: Missing callback for transform " + i);
            }
        }
    }

    public void tracker(String name, int id) {
        if (name.endsWith(goldenNativeMethodName)) {
            System.err.println("Tracked #" + id + ": MATCHED -- " + name);
            gotIt[id] = true;
        } else {
            System.err.println("Tracked #" + id + ": " + name);
        }
    }
}
