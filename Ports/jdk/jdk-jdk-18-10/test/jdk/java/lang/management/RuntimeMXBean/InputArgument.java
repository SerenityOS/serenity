/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4530538
 * @summary Basic unit test of RuntimeMXBean.getInputArguments().
 *
 * @author  Mandy Chung
 *
 */

import java.lang.management.*;
import java.util.List;
import java.util.ListIterator;

public class InputArgument {
    private static RuntimeMXBean rm = ManagementFactory.getRuntimeMXBean();
    private static String vmOption = null;

    public static void main(String args[]) throws Exception {
        // set the expected input argument
        if (args.length > 0) {
            vmOption = args[0];
        }

        List<String> options = rm.getInputArguments();
        if (vmOption == null) {
            return;
        }

        boolean testFailed = true;
        System.out.println("Number of arguments = " + options.size());
        int i = 0;
        for (String arg : options) {
            System.out.println("Input arg " + i + " = " + arg);
            i++;
            if (arg.equals(vmOption)) {
                testFailed = false;
                break;
            }
        }
        if (testFailed) {
            throw new RuntimeException("TEST FAILED: " +
                "VM option " + vmOption + " not found");
        }

        System.out.println("Test passed.");
    }
}
