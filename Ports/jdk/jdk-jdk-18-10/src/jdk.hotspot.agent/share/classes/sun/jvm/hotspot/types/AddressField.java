/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.types;

import sun.jvm.hotspot.debugger.*;

/** A specialization of Field which adds Address-typed accessor
    methods. Since we currently do not understand pointer types (and
    since coercion from integer to pointer types and back is often
    done in C programs anyway) these accessors are not typechecked as,
    for example, the Java primitive type accessors are. */

public interface AddressField extends Field {
  /** This accessor requires that the field be nonstatic, or a WrongTypeException will be thrown. */
  public Address getValue(Address addr)     throws UnmappedAddressException, UnalignedAddressException, WrongTypeException;

  /** This accessor requires that the field be static, or a WrongTypeException will be thrown. */
  public Address getValue()                 throws UnmappedAddressException, UnalignedAddressException, WrongTypeException;
}
