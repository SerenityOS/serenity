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
 * @test TestConjointAtomicArraycopy
 * @bug 7100935
 * @summary verify that oops are copied element-wise atomic
 * @run main/othervm -Xint TestConjointAtomicArraycopy
 * @run main/othervm -Xcomp -Xbatch TestConjointAtomicArraycopy
 * @author axel.siebenborn@sap.com
 */

public class TestConjointAtomicArraycopy {

  static volatile Object [] testArray = new Object [4];

  static short[] a1 = new short[8];
  static short[] a2 = new short[8];
  static short[] a3 = new short[8];

  static volatile boolean keepRunning = true;

  static void testOopsCopy() throws InterruptedException{

  }

  public static void main(String[] args ) throws InterruptedException{
    for (int i = 0; i < testArray.length; i++){
      testArray[i] = new String("A");
    }

    Thread writer = new Thread (new Runnable(){
      public void run(){
        for (int i = 0 ; i < 1000000; i++) {
          System.arraycopy(testArray, 1, testArray, 0, 3);
          testArray[2] = new String("a");
        }
      }
    });

    Thread reader = new Thread( new Runnable(){
      public void run(){
        while (keepRunning){
          String name = testArray[2].getClass().getName();
          if(!(name.endsWith("String"))){
            throw new RuntimeException("got wrong class name");
          }
        }
      }
    });
    keepRunning = true;
    reader.start();
    writer.start();
    writer.join();
    keepRunning = false;
    reader.join();
  }
}
