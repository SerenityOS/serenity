/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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

import sun.jvm.hotspot.oops.*;

public class SignatureConverter extends SignatureIterator {
    private StringBuffer buf;
    private boolean first = true;

    public SignatureConverter(Symbol sig, StringBuffer buf) {
      super(sig);
      this.buf = buf;
    }

    public void doBool  () { appendComma(); buf.append("boolean"); }
    public void doChar  () { appendComma(); buf.append("char");    }
    public void doFloat () { appendComma(); buf.append("float");   }
    public void doDouble() { appendComma(); buf.append("double");  }
    public void doByte  () { appendComma(); buf.append("byte");    }
    public void doShort () { appendComma(); buf.append("short");   }
    public void doInt   () { appendComma(); buf.append("int");     }
    public void doLong  () { appendComma(); buf.append("long");    }
    public void doVoid  () {
       if(isReturnType()) {
          appendComma(); buf.append("void");
       } else {
          throw new RuntimeException("Should not reach here");
       }
    }

    public void doObject(int begin, int end) { doObject(begin, end, true); }
    public void doArray (int begin, int end) {
      appendComma();
      int inner = arrayInnerBegin(begin);
      switch (_signature.getByteAt(inner)) {
      case 'B': buf.append("byte"); break;
      case 'C': buf.append("char"); break;
      case 'D': buf.append("double"); break;
      case 'F': buf.append("float"); break;
      case 'I': buf.append("int"); break;
      case 'J': buf.append("long"); break;
      case 'S': buf.append("short"); break;
      case 'Z': buf.append("boolean"); break;
      case 'L': doObject(inner + 1, end, false); break;
      default: break;
      }
      for (int i = 0; i < inner - begin + 1; i++) {
        buf.append("[]");
      }
    }

    public void appendComma() {
      if (!first) {
        buf.append(", ");
      }
      first = false;
    }

    private void doObject(int begin, int end, boolean comma) {
      if (comma) {
        appendComma();
      }
      appendSubstring(begin, end - 1);
    }

    private void appendSubstring(int begin, int end) {
      for (int i = begin; i < end; i++) {
        buf.append((char) (_signature.getByteAt(i) & 0xFF));
      }
    }

    private int arrayInnerBegin(int begin) {
      while (_signature.getByteAt(begin) == '[') {
        ++begin;
      }
      return begin;
    }
}
