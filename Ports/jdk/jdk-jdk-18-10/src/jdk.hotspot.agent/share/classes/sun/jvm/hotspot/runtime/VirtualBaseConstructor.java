/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.CDebugger;
import sun.jvm.hotspot.debugger.cdbg.ClosestSymbol;
import sun.jvm.hotspot.debugger.cdbg.LoadObject;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.HotSpotTypeDataBase;

/** This provides a factory to create instances where the base virtual
 * type is know and the expected subclasses are within a particular
 * package. */

public class VirtualBaseConstructor<T> extends InstanceConstructor<T> {
  private TypeDataBase db;
  private Map<String, Class<? extends T>> map;
  private Type         baseType;
  private Class<T>     unknownTypeHandler;

  public VirtualBaseConstructor(TypeDataBase db, Type baseType, String packageName, Class<T> unknownTypeHandler) {
    this.db = (HotSpotTypeDataBase)db;
    map     = new HashMap<>();
    this.baseType = baseType;
    this.unknownTypeHandler = unknownTypeHandler;
    if (packageName != null) {
    // Try to find mirror types for each of the types.  If there isn't
    // a direct mirror then try to find an instantiable superclass and
    // treat it as that.
    for (Iterator iter = db.getTypes(); iter.hasNext(); ) {
      Type t = (Type) iter.next();
      Type superType = t;
      while (superType != null && superType != baseType) {
        superType = superType.getSuperclass();
      }
      if (superType == baseType) {
        superType = t;
        Class<? extends T> c = null;
        while (c == null && superType != null) {
          try {
            @SuppressWarnings("unchecked")
            Class<? extends T> lookedUpClass = (Class<? extends T>)Class.forName(packageName + "." + superType.getName());
            c = lookedUpClass;
          } catch (Exception e) {
          }
          if (c == null) superType = superType.getSuperclass();
        }
        if (c == null) {
          c = unknownTypeHandler;
        }
        map.put(t.getName(), c);
      }
    }
  }
  }

  /** Adds a mapping from the given C++ type name to the given Java
      class. The latter must be a subclass of
      sun.jvm.hotspot.runtime.VMObject. Returns false if there was
      already a class for this type name in the map. */
  public boolean addMapping(String cTypeName, Class<? extends T> clazz) {
    if (map.get(cTypeName) != null) {
      return false;
    }

    map.put(cTypeName, clazz);
    return true;
  }

  /** Instantiate the most-precisely typed wrapper object available
      for the type of the given Address. If no type in the mapping
      matched the type of the Address, throws a WrongTypeException.
      Returns null for a null address (similar behavior to
      VMObjectFactory). */
  public T instantiateWrapperFor(Address addr) throws WrongTypeException {
    if (addr == null) {
      return null;
    }

    Type type = db.findDynamicTypeForAddress(addr, baseType);
    if (type != null) {
      return VMObjectFactory.newObject(map.get(type.getName()), addr);
    } else if (unknownTypeHandler != null) {
      return VMObjectFactory.newObject(unknownTypeHandler, addr);
    }

    throw newWrongTypeException(addr);
  }
}
