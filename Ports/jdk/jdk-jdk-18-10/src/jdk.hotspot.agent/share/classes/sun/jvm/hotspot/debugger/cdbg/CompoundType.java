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

package sun.jvm.hotspot.debugger.cdbg;

/** Models all compound types, i.e., those containing fields: classes,
    structs, and unions. The boolean type accessors indicate how the
    type is really defined in the debug information. */

public interface CompoundType {
  public int       getNumBaseClasses();
  public BaseClass getBaseClass(int i);

  public int   getNumFields();
  public Field getField(int i);

  /** Defined as a class in the debug information? */
  public boolean isClass();

  /** Defined as a struct in the debug information? */
  public boolean isStruct();

  /** Defined as a union in the debug information? */
  public boolean isUnion();
}
