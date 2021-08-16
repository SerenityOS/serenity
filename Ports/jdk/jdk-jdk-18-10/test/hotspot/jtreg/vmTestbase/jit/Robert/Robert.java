/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase jit/Robert.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.Robert.Robert
 */

package jit.Robert;

import java.io.*;
import nsk.share.TestFailure;

public class Robert
   {
   Robert()
      throws Exception
      {
      try{
         testSoftwareException();
         }
      catch (Exception e)
         {
         throw e;
         }
      }

   static void
   testHardwareException()
      throws Exception
      {
      int i = 1; int j = 0; int k = i / j;
      System.out.println(k);
      }

   static void
   testSoftwareException()
      throws Exception
      {
      Float f = Float.valueOf("abc");
      System.out.println(f);
      }

   static void
   testUserException()
      throws Exception
      {
      throw new IOException();
      }

   static void
   testRethrownException()
      throws Exception
      {
      new Robert();
      }

   static void
   trouble(int choice)
      throws Exception
      {
      if (choice == 2) testSoftwareException();
      if (choice == 3) testUserException();
      if (choice == 4) testRethrownException();
      }

   public static void main(String args[])
      throws Exception
      {
      boolean failed = false;
      System.out.println("Robert");
      for (int i = 2; i <= 4; ++i)
         {
         System.out.println("test " + i);
         try{
            trouble(i);
            }
         catch (Exception e)
            {
            System.out.println("caught " + e);
            e.printStackTrace(System.out);
            continue;
            }
         failed = true;
         }
      if (failed)
         throw new TestFailure("Test failed.");
      else
         System.out.println("Test passed.");

      }
   }
