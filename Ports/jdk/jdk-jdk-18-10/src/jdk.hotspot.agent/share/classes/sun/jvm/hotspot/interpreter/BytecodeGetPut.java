/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

// getfield, getstatic, putfield or putstatic

public abstract class BytecodeGetPut extends BytecodeWithCPIndex {
  BytecodeGetPut(Method method, int bci) {
    super(method, bci);
  }

  // returns the name of the accessed field
  public Symbol name() {
    ConstantPool cp = method().getConstants();
    return cp.getNameRefAt(index());
  }

  // returns the signature of the accessed field
  public Symbol signature() {
    ConstantPool cp = method().getConstants();
    return cp.getSignatureRefAt(index());
  }

  public Field getField() {
    return method().getConstants().getFieldRefAt(index());
  }

  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(getJavaBytecodeName());
    buf.append(spaces);
    buf.append('#');
    buf.append(indexForFieldOrMethod());
    buf.append(" [Field ");
    StringBuffer sigBuf = new StringBuffer();
    new SignatureConverter(signature(), sigBuf).dispatchField();
    buf.append(sigBuf.toString().replace('/', '.'));
    buf.append(spaces);
    buf.append(name().asString());
    buf.append("]");
    if (code() != javaCode()) {
       buf.append(spaces);
       buf.append('[');
       buf.append(getBytecodeName());
       buf.append(']');
    }
    return buf.toString();
  }

  public abstract boolean isStatic();
}
