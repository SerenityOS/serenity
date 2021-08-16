/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.win32.coff;

/** Models the "sstGlobalTypes" subsection in Visual C++ 5.0 debug
    information. This class provides access to the types via
    iterators; it does not instantiate objects to represent types
    because of the expected high volume of types. The caller is
    expected to traverse this table and convert the platform-dependent
    types into a platform-independent format at run time. */

public interface DebugVC50SSGlobalTypes extends DebugVC50Subsection {
  /** Number of types in the table. */
  public int getNumTypes();

  /** Absolute file offset of the <i>i</i>th (0..getNumTypes() - 1)
      type in the table. */
  public int getTypeOffset(int i);

  /** Create a new type iterator pointing to the first type in the
      subsection. */
  public DebugVC50TypeIterator getTypeIterator();
}
