/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4098742
   @summary Test biginteger modpow method
   @author Michael McCloskey
   @run main/othervm ModPowPowersof2
*/

import java.math.BigInteger;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;

/**
 * This class tests to see if using modPow on a power
 * of two crashes the vm
 *
 */
public class ModPowPowersof2 {

      public static void main(String args[]) throws Exception {
          // Construct a command that runs the test in other vm
          String[] command = new String[4];
          int n = 0;

          command[n++] = System.getProperty("java.home") + File.separator +
              "bin" + File.separator + "java";
          if (System.getProperty("java.class.path") != null) {
              command[n++] = "-classpath";
              command[n++] = System.getProperty("java.class.path");
          }

          command[n++] = "ModPowPowersof2$ModTester";

          // Exec another vm to run test in
          Process p = null;
          p = Runtime.getRuntime().exec(command);

          // Read the result to determine if test failed
          BufferedReader in = new BufferedReader(new InputStreamReader(
                                             p.getInputStream()));
          String s;
          s = in.readLine();
          if (s == null)
              throw new RuntimeException("ModPow causes vm crash");

     }

    public static class ModTester {
        public static void main(String [] args) {
            BigInteger two = BigInteger.valueOf(2);
            BigInteger four = BigInteger.valueOf(4);

            two.modPow(two, BigInteger.valueOf(4));
            two.modPow(two, BigInteger.valueOf(8));
            two.modPow(four, BigInteger.valueOf(8));

            System.out.println("success");
        }
    }

}
