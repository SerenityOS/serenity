/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

/** Given a pointer to some memory return an appropriate wrapper.
 Various subclasses provide different mechanisms for identifying the
 appropriate wrapper. */

abstract public class InstanceConstructor<T> {
  /** Instantiate the most-precisely typed wrapper object available
      for the type of the given Address. If no type in the mapping
      matched the type of the Address, throws a WrongTypeException.
      Returns null for a null address (similar behavior to
      VMObjectFactory). */
  abstract public T instantiateWrapperFor(Address addr) throws WrongTypeException;

  protected WrongTypeException newWrongTypeException(Address addr) {
    String message = "No suitable match for type of address " + addr;
    CDebugger cdbg = VM.getVM().getDebugger().getCDebugger();
    if (cdbg != null) {
      // Most common case: V-table pointer is the first field
      Address vtblPtr = addr.getAddressAt(0);
      LoadObject lo = cdbg.loadObjectContainingPC(vtblPtr);
      if (lo != null) {
        ClosestSymbol symbol = lo.closestSymbolToPC(vtblPtr);
        if (symbol != null) {
          message += " (nearest symbol is " + symbol.getName() + ")";
        }
      }
    }

    return new WrongTypeException(message);
  }
}
