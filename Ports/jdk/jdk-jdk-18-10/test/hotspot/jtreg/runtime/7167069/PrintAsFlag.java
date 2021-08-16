/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * Note that in the run command below the only important flag is PrintCommandLineFlags.
 * The others are just flags of all types; bool, intx, uintx, uint64_t, double and ccstr.
 *
 * @test PrintAsFlag
 * @summary verify that Flag::print_as_flag() works correctly. This is used by "jinfo -flag" and -XX:+PrintCommandLineFlags.
 * @run main/othervm -XX:+PrintCommandLineFlags -XX:-ShowMessageBoxOnError -XX:ParallelGCThreads=4 -XX:MaxRAM=1G -XX:ErrorFile="file" PrintAsFlag
 */

public class PrintAsFlag {
  public static void main(String... args) {
    System.out.printf("Done");
  }
}
