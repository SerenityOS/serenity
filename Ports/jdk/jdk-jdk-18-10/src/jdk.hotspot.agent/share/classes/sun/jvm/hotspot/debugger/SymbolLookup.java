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

package sun.jvm.hotspot.debugger;

public interface SymbolLookup {
  /** Looks up the given symbol in the context of the given object.

      <P>

      FIXME: we may want to hide the objectName so the user does not
      have to specify it, but it isn't clear whether this will work
      transparently with dbx.

      <P>

      FIXME: what happens if the address is not found? Throw
      exception? Currently returns null. */
  public Address lookup(String objectName, String symbol);

  /** Looks up the given symbol in the context of the given object,
      assuming that symbol refers to a Java object.

      FIXME: still not sure whether this will be necessary. Seems that
      lookup of static fields with type "oop" already works, since the
      lookup routine returns the address of the oop (i.e., an
      oopDesc**).

      <P>

      FIXME: we may want to hide the objectName so the user does not
      have to specify it, but it isn't clear whether this will work
      transparently with dbx.

      <P>

      FIXME: what happens if the address is not found? Throw
      exception? Currently returns null. */
  public OopHandle lookupOop(String objectName, String symbol);
}
