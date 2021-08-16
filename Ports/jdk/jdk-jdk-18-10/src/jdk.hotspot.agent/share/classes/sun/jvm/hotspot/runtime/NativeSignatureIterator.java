/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

/** Specialized SignatureIterator: Used for native call purposes */

public abstract class NativeSignatureIterator extends SignatureIterator {
  private Method method;
// [RGV] We need seperate JNI and Java offset values because in 64 bit mode, the argument offsets
//       are not in sync with the Java stack.  For example a long takes up 1 "C" stack entry
//       but 2 Java stack entries.
  private int offset;     // The java stack offset
  private int prepended;  // number of prepended JNI parameters (1 JNIEnv, plus 1 mirror if static)
  private int jni_offset; // the current parameter offset, starting with 0

  public void doBool  ()                     { passInt();    jni_offset++; offset++;       }
  public void doChar  ()                     { passInt();    jni_offset++; offset++;       }
  public void doFloat () {
    if (VM.getVM().isLP64()) {
      passFloat();
    } else {
      passInt();
    }
    jni_offset++; offset++;
  }

  public void doDouble() {
    if (VM.getVM().isLP64()) {
      passDouble(); jni_offset++; offset += 2;
    } else {
      passDouble(); jni_offset += 2; offset += 2;
    }
  }

  public void doByte  ()                     { passInt();    jni_offset++; offset++;       }
  public void doShort ()                     { passInt();    jni_offset++; offset++;       }
  public void doInt   ()                     { passInt();    jni_offset++; offset++;       }

  public void doLong  () {
    if (VM.getVM().isLP64()) {
      passLong(); jni_offset++; offset += 2;
    } else {
      passLong(); jni_offset += 2; offset += 2;
    }
  }

  public void doVoid  ()                     { throw new RuntimeException("should not reach here"); }
  public void doObject(int begin, int end)   { passObject(); jni_offset++; offset++;        }
  public void doArray (int begin, int end)   { passObject(); jni_offset++; offset++;        }

  public Method       method()               { return method; }
  public int          offset()               { return offset; }
  public int       jniOffset()               { return jni_offset + prepended; }
  public boolean    isStatic()               { return method.isStatic(); }

  public abstract void passInt();
  public abstract void passLong();
  public abstract void passObject();
  public abstract void passFloat();
  public abstract void passDouble();

  public NativeSignatureIterator(Method method) {
    super(method.getSignature());
    this.method = method;
    offset = 0;
    jni_offset = 0;

    int JNIEnv_words = 1;
    int mirror_words = 1;
    prepended = !isStatic() ? JNIEnv_words : JNIEnv_words + mirror_words;
  }

  // iterate() calles the 2 virtual methods according to the following invocation syntax:
  //
  // {pass_int | pass_long | pass_object}
  //
  // Arguments are handled from left to right (receiver first, if any).
  // The offset() values refer to the Java stack offsets but are 0 based and increasing.
  // The java_offset() values count down to 0, and refer to the Java TOS.
  // The jni_offset() values increase from 1 or 2, and refer to C arguments.
  public void iterate() {
    if (!isStatic()) {
      // handle receiver (not handled by iterate because not in signature)
      passObject(); jni_offset++; offset++;
    }
    iterateParameters();
  }
}
