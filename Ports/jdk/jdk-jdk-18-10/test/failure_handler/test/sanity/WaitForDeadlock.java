/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.file.Paths;

/*
 * @test
 * @build Deadlock
 * @run driver WaitForDeadlock
 */
public class WaitForDeadlock {
    public static void main(String[] args) throws Exception {
        System.out.println("START");
        ProcessBuilder pb = new ProcessBuilder(Paths.get(
                System.getProperty("test.jdk"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"),
                Deadlock.class.getName());
        pb.redirectError(ProcessBuilder.Redirect.to(Paths.get("out").toFile()));
        pb.redirectOutput(ProcessBuilder.Redirect.to(Paths.get("err").toFile()));
        int r = pb.start().waitFor();
        System.out.println("END. " + r);
    }
}
