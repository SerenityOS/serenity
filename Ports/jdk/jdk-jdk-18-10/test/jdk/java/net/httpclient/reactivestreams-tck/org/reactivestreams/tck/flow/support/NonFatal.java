/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package org.reactivestreams.tck.flow.support;


/**
 * Copy of scala.control.util.NonFatal in order to not depend on scala-library
 */
public class NonFatal {
  private NonFatal() {
    // no instances, please.
  }

  /**
   * Returns true if the provided `Throwable` is to be considered non-fatal, or false if it is to be considered fatal
   *
   * @param t throwable to be matched for fatal-ness
   * @return true if is a non-fatal throwable, false otherwise
   */
  public static boolean isNonFatal(Throwable t) {
    if (t instanceof StackOverflowError) {
      // StackOverflowError ok even though it is a VirtualMachineError
      return true;
    } else if (t instanceof VirtualMachineError ||
        t instanceof ThreadDeath ||
        t instanceof InterruptedException ||
        t instanceof LinkageError) {
      // VirtualMachineError includes OutOfMemoryError and other fatal errors
      return false;
    } else {
      return true;
    }
  }
}
