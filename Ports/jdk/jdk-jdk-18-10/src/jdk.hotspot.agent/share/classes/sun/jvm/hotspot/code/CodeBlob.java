/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */
package sun.jvm.hotspot.code;

import sun.jvm.hotspot.compiler.ImmutableOopMap;
import sun.jvm.hotspot.compiler.ImmutableOopMapSet;
import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.runtime.VMObject;
import sun.jvm.hotspot.types.AddressField;
import sun.jvm.hotspot.types.CIntegerField;
import sun.jvm.hotspot.types.Type;
import sun.jvm.hotspot.types.TypeDataBase;
import sun.jvm.hotspot.utilities.Assert;
import sun.jvm.hotspot.utilities.CStringUtilities;

import java.io.PrintStream;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

public class CodeBlob extends VMObject {
  private static AddressField nameField;
  private static CIntegerField sizeField;
  private static CIntegerField headerSizeField;
  private static AddressField  contentBeginField;
  private static AddressField  codeBeginField;
  private static AddressField  codeEndField;
  private static AddressField  dataEndField;
  private static CIntegerField frameCompleteOffsetField;
  private static CIntegerField dataOffsetField;
  private static CIntegerField frameSizeField;
  private static AddressField  oopMapsField;

  public CodeBlob(Address addr) {
    super(addr);
  }

  protected static       int     matcherInterpreterFramePointerReg;

  private static void initialize(TypeDataBase db) {
    Type type = db.lookupType("CodeBlob");

    nameField                = type.getAddressField("_name");
    sizeField                = type.getCIntegerField("_size");
    headerSizeField          = type.getCIntegerField("_header_size");
    frameCompleteOffsetField = type.getCIntegerField("_frame_complete_offset");
    contentBeginField        = type.getAddressField("_content_begin");
    codeBeginField           = type.getAddressField("_code_begin");
    codeEndField             = type.getAddressField("_code_end");
    dataEndField             = type.getAddressField("_data_end");
    dataOffsetField          = type.getCIntegerField("_data_offset");
    frameSizeField           = type.getCIntegerField("_frame_size");
    oopMapsField             = type.getAddressField("_oop_maps");

    if (VM.getVM().isServerCompiler()) {
      matcherInterpreterFramePointerReg =
          db.lookupIntConstant("Matcher::interpreter_frame_pointer_reg").intValue();
    }
  }

  static {
    VM.registerVMInitializedObserver(new Observer() {
      public void update(Observable o, Object data) {
        initialize(VM.getVM().getTypeDataBase());
      }
    });
  }

  public Address headerBegin() { return getAddress(); }

  public Address headerEnd() { return getAddress().addOffsetTo(getHeaderSize()); }

  public Address contentBegin() { return contentBeginField.getValue(addr); }

  public Address contentEnd() { return headerBegin().addOffsetTo(getDataOffset()); }

  public Address codeBegin() { return codeBeginField.getValue(addr); }

  public Address codeEnd() { return codeEndField.getValue(addr); }

  public Address dataBegin() { return headerBegin().addOffsetTo(getDataOffset()); }

  public Address dataEnd() { return dataEndField.getValue(addr); }

  public long getFrameCompleteOffset() { return frameCompleteOffsetField.getValue(addr); }

  public int getDataOffset()       { return (int) dataOffsetField.getValue(addr); }

  // Sizes
  public int getSize()             { return (int) sizeField.getValue(addr); }

  public int getHeaderSize()       { return (int) headerSizeField.getValue(addr); }

  public long getFrameSizeWords() {
    return (int) frameSizeField.getValue(addr);
  }

  public String getName() {
    return CStringUtilities.getString(nameField.getValue(addr));
  }

  /** OopMap for frame; can return null if none available */

  public ImmutableOopMapSet getOopMaps() {
    Address value = oopMapsField.getValue(addr);
    if (value == null) {
      return null;
    }
    return new ImmutableOopMapSet(value);
  }


  // Typing
  public boolean isBufferBlob()         { return false; }

  public boolean isCompiled()           { return false; }

  public boolean isNMethod()            { return false; }

  public boolean isRuntimeStub()        { return false; }

  public boolean isDeoptimizationStub() { return false; }

  public boolean isUncommonTrapStub()   { return false; }

  public boolean isExceptionStub()      { return false; }

  public boolean isSafepointStub()      { return false; }

  public boolean isAdapterBlob()        { return false; }

  // Fine grain nmethod support: isNmethod() == isJavaMethod() || isNativeMethod() || isOSRMethod()
  public boolean isJavaMethod()         { return false; }

  public boolean isNativeMethod()       { return false; }

  /** On-Stack Replacement method */
  public boolean isOSRMethod()          { return false; }

  public NMethod asNMethodOrNull() {
    if (isNMethod()) return (NMethod)this;
    return null;
  }

  // FIXME: add getRelocationSize()
  public int getContentSize()      { return (int) contentEnd().minus(contentBegin()); }

  public int getCodeSize()         { return (int) codeEnd()   .minus(codeBegin());    }

  public int getDataSize()         { return (int) dataEnd()   .minus(dataBegin());    }

  // Containment
  public boolean blobContains(Address addr)    { return headerBegin() .lessThanOrEqual(addr) && dataEnd()   .greaterThan(addr); }

  // FIXME: add relocationContains
  public boolean contentContains(Address addr) { return contentBegin().lessThanOrEqual(addr) && contentEnd().greaterThan(addr); }

  public boolean codeContains(Address addr)    { return codeBegin()   .lessThanOrEqual(addr) && codeEnd()   .greaterThan(addr); }

  public boolean dataContains(Address addr)    { return dataBegin()   .lessThanOrEqual(addr) && dataEnd()   .greaterThan(addr); }

  public boolean contains(Address addr)        { return contentContains(addr);                                                  }

  public boolean isFrameCompleteAt(Address a)  { return codeContains(a) && a.minus(codeBegin()) >= getFrameCompleteOffset(); }

  // Reclamation support (really only used by the nmethods, but in order to get asserts to work
  // in the CodeCache they are defined virtual here)
  public boolean isZombie()             { return false; }

  public boolean isLockedByVM()         { return false; }

  public ImmutableOopMap getOopMapForReturnAddress(Address returnAddress, boolean debugging) {
    Address pc = returnAddress;
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(getOopMaps() != null, "nope");
    }
    return getOopMaps().findMapAtOffset(pc.minus(codeBegin()), debugging);
  }

  /** NOTE: this returns a size in BYTES in this system! */
  public long getFrameSize() {
    return VM.getVM().getAddressSize() * getFrameSizeWords();
  }

  // Returns true, if the next frame is responsible for GC'ing oops passed as arguments
  public boolean callerMustGCArguments() { return false; }

  public void print() {
    printOn(System.out);
  }

  public void printOn(PrintStream tty) {
    tty.print(getName());
    printComponentsOn(tty);
  }

  protected void printComponentsOn(PrintStream tty) {
    tty.println(" content: [" + contentBegin() + ", " + contentEnd() + "), " +
                " code: [" + codeBegin() + ", " + codeEnd() + "), " +
                " data: [" + dataBegin() + ", " + dataEnd() + "), " +
                " frame size: " + getFrameSize());
  }
}
