/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.oops;

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;

// Entries in a ProfileData object to record types: it can either be
// none (no profile), unknown (conflicting profile data) or a klass if
// a single one is seen. Whether a null reference was seen is also
// recorded. No counter is associated with the type and a single type
// is tracked (unlike VirtualCallData).
public abstract class TypeEntries<K,M> {
  static final int nullSeen = 1;
  static final int typeMask = ~nullSeen;
  static final int typeUnknown = 2;
  static final int statusBits = nullSeen | typeUnknown;
  static final int typeKlassMask = ~statusBits;

  final ProfileData pd;
  final int baseOff;
  final MethodDataInterface<K,M> methodData;

  boolean wasNullSeen(int index) {
    long v = pd.intptrAt(index);
    return (v & nullSeen) != 0;
  }

  boolean isTypeUnknown(int index) {
    long v = pd.intptrAt(index);
    return (v & typeUnknown) != 0;
  }

  boolean isTypeNone(int index) {
    long v = pd.intptrAt(index);
    return (v & typeMask) == 0;
  }

  K validKlass(int index) {
    if (!isTypeNone(index) &&
        !isTypeUnknown(index)) {
      return methodData.getKlassAtAddress(pd.addressAt(index).andWithMask(typeKlassMask));
    } else {
      return null;
    }
  }

  void printKlass(PrintStream st, int index) {
    if (isTypeNone(index)) {
      st.print("none");
    } else if (isTypeUnknown(index)) {
      st.print("unknown");
    } else {
      methodData.printKlassValueOn(validKlass(index), st);
    }
    if (wasNullSeen(index)) {
      st.print(" (null seen)");
    }
  }

  TypeEntries(MethodDataInterface<K,M> methodData, ProfileData pd, int baseOff) {
    this.pd = pd;
    this.baseOff = baseOff;
    this.methodData = methodData;
  }

  long intptrAt(int index) {
    return pd.intptrAt(index);
  }

}
