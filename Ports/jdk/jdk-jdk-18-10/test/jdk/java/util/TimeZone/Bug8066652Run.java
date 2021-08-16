/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8066652
 * @requires os.family == "mac"
 * @summary tests thread safe native function localtime_r is accessed by multiple
 *          threads at same time and zone id should not be  "GMT+00:00"
 *          if default timezone is "GMT" and user specifies a fake timezone.
 * @library /test/lib
 * @build Bug8066652
 * @run main Bug8066652Run
 */

import java.util.Map;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;

public class Bug8066652Run {
    private static String cp = Utils.TEST_CLASSES;
    private static ProcessBuilder pb = new ProcessBuilder();

    public static void main(String[] args) throws Throwable {
        //set system TimeZone to GMT using environment variable TZ
        Map<String, String> env = pb.environment();
        env.put("TZ", "GMT");
        System.out.println("Current TimeZone:" + pb.environment().get("TZ"));
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("java");
        //Setting invalid TimeZone using VM option
        launcher.addToolArg("-Duser.timezone=Foo/Bar")
                .addToolArg("-ea")
                .addToolArg("-esa")
                .addToolArg("-cp")
                .addToolArg(cp)
                .addToolArg("Bug8066652");

        pb.command(launcher.getCommand());
        int exitCode = ProcessTools.executeCommand(pb)
                .getExitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Unexpected exit code: " + exitCode);
        }
    }
}

