/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.code;

import java.io.*;
import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.utilities.*;

/** A port of the VM's Stub mechanism. Note that the separation of
    Stub and StubInterface (done in the VM to save space) is not
    currently necessary in these APIs and has been flattened so that
    class Stub has virtual functions overridden by concrete
    subclasses. */

public class Stub extends VMObject {

  public Stub(Address addr) {
    super(addr);
  }

  // NOTE (FIXME): initialize(int) / finalize() elided for now

  //
  // General info/converters
  //

  /** Must return the size provided by initialize */
  public long getSize()            { Assert.that(false, "should not call this"); return 0; }
  // NOTE (FIXME): code_size_to_size elided for now (would be a good reason for inserting the StubInterface abstraction)
  /** Needed to add this for iteration */
  public Address getAddress()      { return addr; }

  //
  // Code info
  //

  /** Points to the first byte of the code */
  public Address codeBegin()       { Assert.that(false, "should not call this"); return null; }
  /** Points to the first byte after the code */
  public Address codeEnd()         { Assert.that(false, "should not call this"); return null; }

  //
  // Debugging
  //

  /** Verifies the Stub */
  public void verify()             { Assert.that(false, "should not call this"); }
  /** Prints some information about the stub */
  public void printOn(PrintStream tty) { Assert.that(false, "should not call this"); }
}
