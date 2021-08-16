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

package sun.jvm.hotspot.code;

import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;
import sun.jvm.hotspot.utilities.Observable;
import sun.jvm.hotspot.utilities.Observer;

/** <P> A port of the VM's StubQueue. Note that the VM implicitly
    knows the type of the objects contained in each StubQueue because
    it passes in an instance of a StubInterface to the StubQueue's
    constructor; the goal in the VM was to save space in the generated
    code. In the SA APIs the pattern has been to use the
    VirtualConstructor mechanism to instantiate wrapper objects of the
    appropriate type for objects down in the VM; see, for example, the
    CodeCache, which identifies NMethods, RuntimeStubs, etc. </P>

    <P> In this port we eliminate the StubInterface in favor of
    passing in the class corresponding to the type of Stub which this
    StubQueue contains. </P> */

public class StubQueue extends VMObject {
  // FIXME: add the rest of the fields
  private static AddressField  stubBufferField;
  private static CIntegerField bufferLimitField;
  private static CIntegerField queueBeginField;
  private static CIntegerField queueEndField;
  private static CIntegerField numberOfStubsField;

  // The type of the contained stubs (i.e., InterpreterCodelet,
  // ICStub). Must be a subclass of type Stub.
  private Class<?> stubType;

  static {
    VM.registerVMInitializedObserver(new Observer() {
        public void update(Observable o, Object data) {
          initialize(VM.getVM().getTypeDataBase());
        }
      });
  }

  private static synchronized void initialize(TypeDataBase db) {
    Type type = db.lookupType("StubQueue");

    stubBufferField    = type.getAddressField("_stub_buffer");
    bufferLimitField   = type.getCIntegerField("_buffer_limit");
    queueBeginField    = type.getCIntegerField("_queue_begin");
    queueEndField      = type.getCIntegerField("_queue_end");
    numberOfStubsField = type.getCIntegerField("_number_of_stubs");
  }

  public StubQueue(Address addr, Class stubType) {
    super(addr);
    this.stubType = stubType;
  }

  public boolean contains(Address pc) {
    if (pc == null) return false;
    long offset = pc.minus(getStubBuffer());
    return ((0 <= offset) && (offset < getBufferLimit()));
  }

  public Stub getStubContaining(Address pc) {
    if (contains(pc)) {
      int i = 0;
      for (Stub s = getFirst(); s != null; s = getNext(s)) {
        if (stubContains(s, pc)) {
          return s;
        }
      }
    }
    return null;
  }

  public boolean stubContains(Stub s, Address pc) {
    return (s.codeBegin().lessThanOrEqual(pc) && s.codeEnd().greaterThan(pc));
  }

  public int getNumberOfStubs() {
    return (int) numberOfStubsField.getValue(addr);
  }

  public Stub getFirst() {
    return ((getNumberOfStubs() > 0) ? getStubAt(getQueueBegin()) : null);
  }

  public Stub getNext(Stub s) {
    long i = getIndexOf(s) + getStubSize(s);
    if (i == getBufferLimit()) {
      i = 0;
    }
    return ((i == getQueueEnd()) ? null : getStubAt(i));
  }

  public Stub getPrev(Stub s) {
    if (getIndexOf(s) == getQueueBegin()) {
       return null;
    }

    Stub temp = getFirst();
    Stub prev = null;
    while (temp != null && getIndexOf(temp) != getIndexOf(s)) {
       prev = temp;
       temp  = getNext(temp);
    }

    return prev;
  }

  //--------------------------------------------------------------------------------
  // Internals only below this point
  //

  private long getQueueBegin() {
    return queueBeginField.getValue(addr);
  }

  private long getQueueEnd() {
    return queueEndField.getValue(addr);
  }

  private long getBufferLimit() {
    return bufferLimitField.getValue(addr);
  }

  private Address getStubBuffer() {
    return stubBufferField.getValue(addr);
  }

  private Stub getStubAt(long offset) {
    checkIndex(offset);
    return (Stub) VMObjectFactory.newObject(stubType, getStubBuffer().addOffsetTo(offset));
  }

  private long getIndexOf(Stub s) {
    long i = s.getAddress().minus(getStubBuffer());
    checkIndex(i);
    return i;
  }

  private long getStubSize(Stub s) {
    return s.getSize();
  }

  private void checkIndex(long i) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(0 <= i && i < getBufferLimit() && (i % VM.getVM().getAddressSize() == 0), "illegal index");
    }
  }
}
