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
import sun.jvm.hotspot.types.*;

/** This class provides generalized "virtual constructor"
    functionality for VMObjects. In simple terms, it creates
    correctly-typed Java wrapper objects for underlying Addresses,
    using the "RTTI-like" functionality of TypeDataBase. For example,
    if the given Address really is a DefNewGeneration*, the Java object
    created for it will be of type
    sun.jvm.hotspot.memory.DefNewGeneration, assuming the mapping from
    type "DefNewGeneration" to class
    sun.jvm.hotspot.memory.DefNewGeneration has been set up. */

public class VirtualConstructor extends InstanceConstructor<VMObject> {
  private TypeDataBase db;
  private Map<String, Class<? extends VMObject>> map;

  public VirtualConstructor(TypeDataBase db) {
    this.db = db;
    map     = new HashMap<>();
  }

  /** Adds a mapping from the given C++ type name to the given Java
      class. The latter must be a subclass of
      sun.jvm.hotspot.runtime.VMObject. Returns false if there was
      already a class for this type name in the map. */
  public boolean addMapping(String cTypeName, Class<? extends VMObject> clazz) {
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
  public VMObject instantiateWrapperFor(Address addr) throws WrongTypeException {
    if (addr == null) {
      return null;
    }

    for (Iterator<String> iter = map.keySet().iterator(); iter.hasNext(); ) {
      String typeName = iter.next();
      if (db.addressTypeIsEqualToType(addr, db.lookupType(typeName))) {
        return VMObjectFactory.newObject(map.get(typeName), addr);
      }
    }

    throw newWrongTypeException(addr);
  }
}
