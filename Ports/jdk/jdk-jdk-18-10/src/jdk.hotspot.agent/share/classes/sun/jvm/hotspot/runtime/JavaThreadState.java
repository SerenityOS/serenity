/*
 * Copyright (c) 2000, 2003, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.runtime;

/** This is a type-safe enum mirroring the JavaThreadState enum in
    globalDefinitions.hpp. The conversion between the underlying ints
    and these values is done in JavaThread. */

public class JavaThreadState {
  private String stringVal;

  /** Should never happen (missing initialization) */
  public static final JavaThreadState UNINITIALIZED     = new JavaThreadState("UNINITIALIZED");
  /** Just starting up, i.e., in process of being initialized */
  public static final JavaThreadState NEW               = new JavaThreadState("NEW");
  /** Corresponding transition state (not used, included for completness) */
  public static final JavaThreadState NEW_TRANS         = new JavaThreadState("NEW_TRANS");
  /** Running in native code */
  public static final JavaThreadState IN_NATIVE         = new JavaThreadState("IN_NATIVE");
  /** Corresponding transition state */
  public static final JavaThreadState IN_NATIVE_TRANS   = new JavaThreadState("IN_NATIVE_TRANS");
  /** Running in VM */
  public static final JavaThreadState IN_VM             = new JavaThreadState("IN_VM");
  /** Corresponding transition state */
  public static final JavaThreadState IN_VM_TRANS       = new JavaThreadState("IN_VM_TRANS");
  /** Running in Java or in stub code */
  public static final JavaThreadState IN_JAVA           = new JavaThreadState("IN_JAVA");
  /** Corresponding transition state (not used, included for completness) */
  public static final JavaThreadState IN_JAVA_TRANS     = new JavaThreadState("IN_JAVA_TRANS");
  /** Blocked in vm */
  public static final JavaThreadState BLOCKED           = new JavaThreadState("BLOCKED");
  /** Corresponding transition state   */
  public static final JavaThreadState BLOCKED_TRANS     = new JavaThreadState("BLOCKED_TRANS");
  /** Special state needed, since we cannot suspend a thread when it is in native_trans */

  private JavaThreadState(String stringVal) {
    this.stringVal = stringVal;
  }

  public String toString() {
    return stringVal;
  }
}
