/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.types.basic;

import java.util.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.types.*;

public abstract class BasicVtblAccess implements VtblAccess {
  protected SymbolLookup symbolLookup;
  protected String[] dllNames;

  private Map<Type, Object> typeToVtblMap = new HashMap<>();

  public BasicVtblAccess(SymbolLookup symbolLookup,
                         String[] dllNames) {
    this.symbolLookup = symbolLookup;
    this.dllNames = dllNames;
  }

  static Object nullAddress = new Object();

  public Address getVtblForType(Type type) {
    if (type == null) {
      return null;
    }
    Object result = typeToVtblMap.get(type);
    if (result == nullAddress) {
        return null;
    }
    if (result != null) {
      return (Address)result;
    }
    String vtblSymbol = vtblSymbolForType(type);
    if (vtblSymbol == null) {
      typeToVtblMap.put(type, nullAddress);
      return null;
    }
    for (int i = 0; i < dllNames.length; i++) {
      Address addr = symbolLookup.lookup(dllNames[i], vtblSymbol);
      if (addr != null) {
        typeToVtblMap.put(type, addr);
        return addr;
      }
    }
    typeToVtblMap.put(type, nullAddress);
    return null;
  }

  public void clearCaches() {
    typeToVtblMap.clear();
  }

  protected abstract String vtblSymbolForType(Type type);
}
