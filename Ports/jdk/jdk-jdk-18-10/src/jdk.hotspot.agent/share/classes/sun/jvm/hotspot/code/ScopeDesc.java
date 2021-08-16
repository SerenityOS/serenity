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

import java.io.*;
import java.util.*;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.utilities.*;

/** ScopeDescs contain the information that makes source-level
    debugging of nmethods possible; each scopeDesc describes a method
    activation */

public class ScopeDesc {
  /** NMethod information */
  private NMethod code;
  private Method  method;
  private int     bci;
  private boolean reexecute;
  /** Decoding offsets */
  private int     decodeOffset;
  private int     senderDecodeOffset;
  private int     localsDecodeOffset;
  private int     expressionsDecodeOffset;
  private int     monitorsDecodeOffset;
  /** Scalar replaced bjects pool */
  private List<ObjectValue> objects;

  private ScopeDesc(NMethod code, int decodeOffset, List<ObjectValue> objects, boolean reexecute) {
    this.code = code;
    this.decodeOffset = decodeOffset;
    this.objects      = objects;
    this.reexecute    = reexecute;

    // Decode header
    DebugInfoReadStream stream  = streamAt(decodeOffset);

    senderDecodeOffset = stream.readInt();
    method = stream.readMethod();
    bci    = stream.readBCI();
    // Decode offsets for body and sender
    localsDecodeOffset      = stream.readInt();
    expressionsDecodeOffset = stream.readInt();
    monitorsDecodeOffset    = stream.readInt();
  }

  public ScopeDesc(NMethod code, int decodeOffset, int objectDecodeOffset, boolean reexecute) {
    this.code = code;
    this.decodeOffset = decodeOffset;
    this.objects      = decodeObjectValues(objectDecodeOffset);
    this.reexecute    = reexecute;

    // Decode header
    DebugInfoReadStream stream  = streamAt(decodeOffset);

    senderDecodeOffset = stream.readInt();
    method = stream.readMethod();
    bci    = stream.readBCI();
    // Decode offsets for body and sender
    localsDecodeOffset      = stream.readInt();
    expressionsDecodeOffset = stream.readInt();
    monitorsDecodeOffset    = stream.readInt();
  }

  public NMethod getNMethod()   { return code; }
  public Method getMethod()     { return method; }
  public int    getBCI()        { return bci;    }
  public boolean getReexecute() { return reexecute;}

  /** Returns a List&lt;ScopeValue&gt; */
  public List<ScopeValue> getLocals() {
    return decodeScopeValues(localsDecodeOffset);
  }

  /** Returns a List&lt;ScopeValue&gt; */
  public List<ScopeValue> getExpressions() {
    return decodeScopeValues(expressionsDecodeOffset);
  }

  /** Returns a List&lt;MonitorValue&gt; */
  public List<MonitorValue> getMonitors() {
    return decodeMonitorValues(monitorsDecodeOffset);
  }

  /** Returns a List&lt;ObjectValue&gt; */
  public List<ObjectValue> getObjects() {
    return objects;
  }

  /** Stack walking. Returns null if this is the outermost scope. */
  public ScopeDesc sender() {
    if (isTop()) {
      return null;
    }

    return new ScopeDesc(code, senderDecodeOffset, objects, false);
  }

  /** Returns where the scope was decoded */
  public int getDecodeOffset() {
    return decodeOffset;
  }

  /** Tells whether sender() returns null */
  public boolean isTop() {
    return (senderDecodeOffset == DebugInformationRecorder.SERIALIZED_NULL);
  }

  public boolean equals(Object arg) {
    if (arg == null) {
      return false;
    }

    if (!(arg instanceof ScopeDesc)) {
      return false;
    }

    ScopeDesc sd = (ScopeDesc) arg;

    return (sd.method.equals(method) && (sd.bci == bci));
  }

  public void printValue() {
    printValueOn(System.out);
  }

  public void printValueOn(PrintStream tty) {
    tty.print("ScopeDesc for ");
    method.printValueOn(tty);
    tty.print(" @bci " + bci);
    tty.println(" reexecute=" + reexecute);
  }

  // FIXME: add more accessors

  //--------------------------------------------------------------------------------
  // Internals only below this point
  //
  private DebugInfoReadStream streamAt(int decodeOffset) {
    return new DebugInfoReadStream(code, decodeOffset, objects);
  }

  /** Returns a List&lt;ScopeValue&gt; or null if no values were present */
  private List<ScopeValue> decodeScopeValues(int decodeOffset) {
    if (decodeOffset == DebugInformationRecorder.SERIALIZED_NULL) {
      return null;
    }
    DebugInfoReadStream stream = streamAt(decodeOffset);
    int length = stream.readInt();
    List<ScopeValue> res = new ArrayList<>(length);
    for (int i = 0; i < length; i++) {
      res.add(ScopeValue.readFrom(stream));
    }
    return res;
  }

  /** Returns a List&lt;MonitorValue&gt; or null if no values were present */
  private List<MonitorValue> decodeMonitorValues(int decodeOffset) {
    if (decodeOffset == DebugInformationRecorder.SERIALIZED_NULL) {
      return null;
    }
    DebugInfoReadStream stream = streamAt(decodeOffset);
    int length = stream.readInt();
    List<MonitorValue> res = new ArrayList<>(length);
    for (int i = 0; i < length; i++) {
      res.add(new MonitorValue(stream));
    }
    return res;
  }

  /** Returns a List&lt;ObjectValue&gt; or null if no values were present */
  private List<ObjectValue> decodeObjectValues(int decodeOffset) {
    if (decodeOffset == DebugInformationRecorder.SERIALIZED_NULL) {
      return null;
    }
    List<ObjectValue> res = new ArrayList<>();
    DebugInfoReadStream stream = new DebugInfoReadStream(code, decodeOffset, res);
    int length = stream.readInt();
    for (int i = 0; i < length; i++) {
      // Objects values are pushed to 'res' array during read so that
      // object's fields could reference it (OBJECT_ID_CODE).
      ScopeValue.readFrom(stream);
      // res.add(ScopeValue.readFrom(stream));
    }
    Assert.that(res.size() == length, "inconsistent debug information");
    return res;
  }
}
