/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

import sun.hotspot.WhiteBox;

// This class should be loaded from a shared archive.
public class MultiProcClass {
    private static String instanceLabel;

    public static void main(String args[]) throws Exception {
        instanceLabel = args[0];
        String checkPmap = args[1];

        long pid = ProcessHandle.current().pid();
        System.out.println(inst("========================== Starting MultiProcClass"));
        System.out.println(inst("My PID: " + pid ));
        System.out.println(inst("checkPmap = <" + checkPmap + ">" ));

        if ("true".equals(checkPmap)) {
            if (runPmap(pid, true) != 0)
                System.out.println("MultiProcClass: Pmap failed");
        }

        WhiteBox wb = WhiteBox.getWhiteBox();
        if (!wb.isSharedClass(MultiProcClass.class)) {
            throw new RuntimeException(inst("MultiProcClass should be shared but is not."));
        }

        System.out.println(inst("========================== Leaving MultiProcClass"));
    }

    // A convenience method to append process instance label
    private static String inst(String msg) {
        return "process-" + instanceLabel + " : " + msg;
    }

    // Use on Linux-only; requires jdk-9 for Process.pid()
    public static int runPmap(long pid, boolean inheritIO) throws Exception {
        ProcessBuilder pmapPb = new ProcessBuilder("pmap", "" + pid);
        if (inheritIO)
            pmapPb.inheritIO();

        return pmapPb.start().waitFor();
    }
}
