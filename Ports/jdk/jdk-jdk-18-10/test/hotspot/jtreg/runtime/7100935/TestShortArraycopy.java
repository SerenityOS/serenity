/*
 * Copyright (c) 2011 SAP SE. All rights reserved.
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
 * @test TestShortArraycopy
 * @bug 7100935
 * @summary  verify that shorts are copied element-wise atomic.
 * @run main/othervm -Xint TestShortArraycopy
 * @run main/othervm -Xcomp -Xbatch TestShortArraycopy
 * @author volker.simonis@gmail.com
 */

public class TestShortArraycopy {

  static short[] a1 = new short[8];
  static short[] a2 = new short[8];
  static short[] a3 = new short[8];

  static volatile boolean keepRunning = true;

  public static void main(String[] args) throws InterruptedException {

    for (int i = 0; i < a1.length ; i++) {
      a1[i] = (short)0xffff;
      a2[i] = (short)0xffff;
      a3[i] = (short)0x0000;
    }
    Thread reader = new Thread() {
      public void run() {
        while (keepRunning) {
          for (int j = 0; j < a1.length; j++) {
            short s = a1[j];
            if (s != (short)0xffff && s != (short)0x0000) {
              System.out.println("Error: s = " + s);
              throw new RuntimeException("wrong result");

            }
          }
        }
      }
    };
    Thread writer = new Thread() {
      public void run() {
        for (int i = 0; i < 1000000; i++) {
          System.arraycopy(a2, 5, a1, 3, 3);
          System.arraycopy(a3, 5, a1, 3, 3);
        }
      }
    };
    keepRunning = true;
    reader.start();
    writer.start();
    writer.join();
    keepRunning = false;
    reader.join();
  }
}
