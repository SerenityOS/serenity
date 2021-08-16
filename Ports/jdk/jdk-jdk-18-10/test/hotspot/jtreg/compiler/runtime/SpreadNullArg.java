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
 * @test SpreadNullArg
 * @bug 7141637
 * @summary verifies that the MethodHandle spread adapter can gracefully handle null arguments.
 *
 * @run main compiler.runtime.SpreadNullArg
 * @author volker.simonis@gmail.com
 */

package compiler.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class SpreadNullArg {

  public static void main(String args[]) {

    MethodType mt_ref_arg = MethodType.methodType(int.class, Integer.class);
    MethodHandle mh_spreadInvoker = MethodHandles.spreadInvoker(mt_ref_arg, 0);
    MethodHandle mh_spread_target;
    int result = 42;

    try {
      mh_spread_target =
          MethodHandles.lookup().findStatic(SpreadNullArg.class, "target_spread_arg", mt_ref_arg);
      result = (int) mh_spreadInvoker.invokeExact(mh_spread_target, (Object[]) null);
      throw new Error("Expected NullPointerException was not thrown");
    } catch (NullPointerException e) {
      System.out.println("Expected exception : " + e);
    } catch (Throwable e) {
      throw new Error(e);
    }

    if (result != 42) {
      throw new Error("result [" + result
          + "] != 42 : Expected NullPointerException was not thrown?");
    }
  }

  public static int target_spread_arg(Integer i1) {
    return i1.intValue();
  }

}
