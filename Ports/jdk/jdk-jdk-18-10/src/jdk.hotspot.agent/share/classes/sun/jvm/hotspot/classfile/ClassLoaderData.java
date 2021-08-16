/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.classfile;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class ClassLoaderData extends VMObject {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    Type type      = db.lookupType("ClassLoaderData");
    classLoaderFieldOffset = type.getAddressField("_class_loader").getOffset();
    nextField = type.getAddressField("_next");
    klassesField = new MetadataField(type.getAddressField("_klasses"), 0);
    hasClassMirrorHolderField = new CIntField(type.getCIntegerField("_has_class_mirror_holder"), 0);
  }

  private static long classLoaderFieldOffset;
  private static AddressField nextField;
  private static MetadataField  klassesField;
  private static CIntField hasClassMirrorHolderField;

  public ClassLoaderData(Address addr) {
    super(addr);
  }

  public static ClassLoaderData instantiateWrapperFor(Address addr) {
    if (addr == null) {
      return null;
    }
    return new ClassLoaderData(addr);
  }

  public Oop getClassLoader() {
    Address addr = getAddress().addOffsetTo(classLoaderFieldOffset);
    VMOopHandle vmOopHandle = VMObjectFactory.newObject(VMOopHandle.class, addr);
    return vmOopHandle.resolve();
  }

  public boolean gethasClassMirrorHolder() {
    return hasClassMirrorHolderField.getValue(this) != 0;
  }

  public ClassLoaderData next() {
    return instantiateWrapperFor(nextField.getValue(getAddress()));
  }

  public Klass getKlasses()    { return (Klass)klassesField.getValue(this);  }

  /** Lookup an already loaded class. If not found null is returned. */
  public Klass find(String className) {
    for (Klass l = getKlasses(); l != null; l = l.getNextLinkKlass()) {
        if (l.getName().equals(className)) {
            if (l instanceof InstanceKlass && !((InstanceKlass)l).isLoaded()) {
                return null; // don't return partially loaded classes
            } else {
                return l;
            }
        }
    }
    return null;
  }

  /** Iterate over all klasses - including object, primitive
      array klasses */
  public void classesDo(ClassLoaderDataGraph.ClassVisitor v) {
      for (Klass l = getKlasses(); l != null; l = l.getNextLinkKlass()) {
          // Only visit InstanceKlasses that are at least in the "loaded" init_state. Otherwise
          // the InstanceKlass won't have some required fields initialized, which can cause problems.
          if (l instanceof InstanceKlass && !((InstanceKlass)l).isLoaded()) {
              continue;
          }
          v.visit(l);
      }
  }
}
