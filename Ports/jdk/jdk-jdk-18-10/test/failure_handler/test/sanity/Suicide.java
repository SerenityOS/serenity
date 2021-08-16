/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @summary Suicide test
 * @run main/othervm Suicide
 */
public class Suicide {
    public static void main(String[] args) {
        String cmd = null;
        try {
            long pid = ProcessHandle.current().pid();
            String osName = System.getProperty("os.name");
            if (osName.contains("Windows")) {
                cmd = "taskkill.exe /F /PID " + pid;
            } else {
                cmd = "kill -9 " + pid;
            }

            System.out.printf("executing `%s'%n", cmd);
            Runtime.getRuntime().exec(cmd);
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.printf("TEST/ENV BUG: %s didn't kill JVM%n", cmd);
        System.exit(1);
    }
}
