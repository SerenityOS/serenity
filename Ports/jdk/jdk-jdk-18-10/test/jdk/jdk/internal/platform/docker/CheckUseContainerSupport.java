/*
 * Copyright (c) 2020, Red Hat, Inc.
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

import jdk.internal.platform.Metrics;

public class CheckUseContainerSupport {

    // Usage: boolean value of -XX:+/-UseContainerSupport
    //        passed as the only argument
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new RuntimeException("Expected only one boolean argument");
        }
        boolean expectedContainerSupport = Boolean.parseBoolean(args[0]);
        boolean actualContainerSupport = (Metrics.systemMetrics() != null);
        if (expectedContainerSupport != actualContainerSupport) {
            String msg = "-XX:" + ( expectedContainerSupport ? "+" : "-") + "UseContainerSupport, but got " +
                         "Metrics.systemMetrics() == " + (Metrics.systemMetrics() == null ? "null" : "non-null");
            System.out.println(msg);
            System.out.println("TEST FAILED!!!");
            return;
        }
        System.out.println("TEST PASSED!!!");
    }

}
