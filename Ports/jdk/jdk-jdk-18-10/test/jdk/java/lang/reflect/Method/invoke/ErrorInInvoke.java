/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4428861
   @summary Method.invoke() should wrap all Throwables in InvocationTargetException
   @author Kenneth Russell
*/

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class ErrorInInvoke {
  public static void run() {
    throw new AbstractMethodError("Not really, just testing");
  }

  public static void main(String[] args) {
    Method m = null;

    try {
      m = ErrorInInvoke.class.getMethod("run", new Class[] {});
    } catch (Throwable t) {
      throw new RuntimeException("Test failed (getMethod() failed");
    }

    try {
      m.invoke(null, null);
    } catch (AbstractMethodError e) {
      throw new RuntimeException("Test failed (AbstractMethodError passed through)");
    } catch (InvocationTargetException e) {
      Throwable t = e.getTargetException();
      if (!(t instanceof AbstractMethodError)) {
        throw new RuntimeException("Test failed (InvocationTargetException didn't wrap AbstractMethodError)");
      }
    } catch (Throwable t) {
      throw new RuntimeException("Test failed (Unexpected exception)");
    }
  }
}
