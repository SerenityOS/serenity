/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

abstract public class Metadata extends VMObject {
  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  public Metadata(Address addr) {
    super(addr);
  }

  public static long alignSize(long size) {
    // natural word size.
    return VM.getVM().alignUp(size, VM.getVM().getBytesPerWord());
  }

  private static VirtualBaseConstructor<Metadata> metadataConstructor;

  private static synchronized void initialize(TypeDataBase db) throws WrongTypeException {
    metadataConstructor = new VirtualBaseConstructor<>(db, db.lookupType("Metadata"), null, null);
    // Define an explicit mapping since the C++ and Java type names don't match.
    metadataConstructor.addMapping("Metadata", Metadata.class);
    metadataConstructor.addMapping("Klass", Klass.class);
    metadataConstructor.addMapping("InstanceKlass", InstanceKlass.class);
    metadataConstructor.addMapping("InstanceMirrorKlass", InstanceMirrorKlass.class);
    metadataConstructor.addMapping("InstanceRefKlass", InstanceRefKlass.class);
    metadataConstructor.addMapping("InstanceClassLoaderKlass", InstanceClassLoaderKlass.class);
    metadataConstructor.addMapping("TypeArrayKlass", TypeArrayKlass.class);
    metadataConstructor.addMapping("ObjArrayKlass", ObjArrayKlass.class);
    metadataConstructor.addMapping("Method", Method.class);
    metadataConstructor.addMapping("MethodData", MethodData.class);
    metadataConstructor.addMapping("ConstMethod", ConstMethod.class);
    metadataConstructor.addMapping("ConstantPool", ConstantPool.class);
    metadataConstructor.addMapping("ConstantPoolCache", ConstantPoolCache.class);
  }

  public static Metadata instantiateWrapperFor(Address addr) {
    return metadataConstructor.instantiateWrapperFor(addr);
  }

  public void iterate(MetadataVisitor visitor) {
    visitor.setObj(this);
    visitor.prologue();
    iterateFields(visitor);
    visitor.epilogue();
  }

  void iterateFields(MetadataVisitor visitor) {
  }

  abstract public void printValueOn(PrintStream tty);
  public void dumpReplayData(PrintStream out) {
      out.println("# Unknown Metadata");
  }

  public boolean isShared() {
    VM vm = VM.getVM();
    if (vm.isSharingEnabled()) {
      return MetaspaceObj.isShared(getAddress());
    }
    return false;
  }
}
