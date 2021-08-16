/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.interpreter;

import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.utilities.*;

public class BytecodeInvoke extends BytecodeWithCPIndex {
  BytecodeInvoke(Method method, int bci) {
    super(method, bci);
  }

  public static BytecodeInvoke at(Method method, int bci) {
    BytecodeInvoke b = new BytecodeInvoke(method, bci);
    if (Assert.ASSERTS_ENABLED) {
      b.verify();
    }
    return b;
  }

  /** Like at, but returns null if the BCI is not at an invoke */
  public static BytecodeInvoke atCheck(Method method, int bci) {
    BytecodeInvoke b = new BytecodeInvoke(method, bci);
    return (b.isValid() ? b : null);
  }

  public static BytecodeInvoke at(BytecodeStream bcs) {
    return new BytecodeInvoke(bcs.method(), bcs.bci());
  }

  // returns the name of the invoked method
  public Symbol name() {
    ConstantPool cp = method().getConstants();
    if (isInvokedynamic()) {
      return cp.uncachedGetNameRefAt(indexForFieldOrMethod());
    }
    return cp.getNameRefAt(index());
  }

  // returns the signature of the invoked method
  public Symbol signature() {
    ConstantPool cp = method().getConstants();
    if (isInvokedynamic()) {
      return cp.uncachedGetSignatureRefAt(indexForFieldOrMethod());
    }
    return cp.getSignatureRefAt(index());
  }

  public Method getInvokedMethod() {
    return method().getConstants().getMethodRefAt(index());
  }

  // returns the result type (see BasicType.java) of the invoke
  public int resultType() {
    ResultTypeFinder rts = new ResultTypeFinder(signature());
    rts.iterate();
    return rts.type();
  }

  public int adjustedInvokeCode() {
    return javaCode();
  }

  // "specified" method   (from constant pool)
  // FIXME: elided for now
  // public Method staticTarget();

  // Testers
  public boolean isInvokeinterface() { return adjustedInvokeCode() == Bytecodes._invokeinterface; }
  public boolean isInvokevirtual()   { return adjustedInvokeCode() == Bytecodes._invokevirtual;   }
  public boolean isInvokestatic()    { return adjustedInvokeCode() == Bytecodes._invokestatic;    }
  public boolean isInvokespecial()   { return adjustedInvokeCode() == Bytecodes._invokespecial;   }
  public boolean isInvokedynamic()   { return adjustedInvokeCode() == Bytecodes._invokedynamic; }

  public boolean isValid()           { return isInvokeinterface() ||
                                              isInvokevirtual()   ||
                                              isInvokestatic()    ||
                                              isInvokespecial(); }
  public void verify() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(isValid(), "check invoke");
    }
  }

  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(getJavaBytecodeName());
    buf.append(spaces);
    buf.append('#');
    buf.append(indexForFieldOrMethod());
    if (isInvokedynamic()) {
      ConstantPool cp = method.getConstants();
      buf.append('(');
      int poolIndex = cp.invokeDynamicNameAndTypeRefIndexAt(indexForFieldOrMethod());
      buf.append(poolIndex);
      buf.append(')');
      buf.append(" [Name and Type ");
      buf.append(name().asString());
      buf.append(":");
      buf.append(signature().asString().replace('/', '.'));
    } else {
      buf.append(" [Method ");
      StringBuffer sigBuf = new StringBuffer();
      new SignatureConverter(signature(), sigBuf).iterateReturntype();
      buf.append(sigBuf.toString().replace('/', '.'));
      buf.append(spaces);
      buf.append(name().asString());
      buf.append('(');
      sigBuf = new StringBuffer();
      new SignatureConverter(signature(), sigBuf).iterateParameters();
      buf.append(sigBuf.toString().replace('/', '.'));
      buf.append(')');
    }
    buf.append(']');
    if (code() != javaCode()) {
       buf.append(spaces);
       buf.append('[');
       buf.append(getBytecodeName());
       buf.append(']');
    }
    return buf.toString();
  }
}
