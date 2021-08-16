/*
 * Copyright (c) 2001, 2011, Oracle and/or its affiliates. All rights reserved.
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

public class BytecodeStream {
  private Method _method;

  // reading position
  private int     _bci;       // bci if current bytecode
  private int     _next_bci;  // bci of next bytecode
  private int     _end_bci;   // bci after the current iteration interval

  // last bytecode read
  private int     _code;
  private boolean _is_wide;

  // Construction
  public BytecodeStream(Method method) {
    _method = method;
    setInterval(0, (int) method.getCodeSize());
  }

  // Iteration control
  public void setInterval(int beg_bci, int end_bci) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(0 <= beg_bci && beg_bci <= _method.getCodeSize(), "illegal beg_bci");
      Assert.that(0 <= end_bci && end_bci <= _method.getCodeSize(), "illegal end_bci");
    }
    // setup of iteration pointers
    _bci      = beg_bci;
    _next_bci = beg_bci;
    _end_bci  = end_bci;
  }

  public void setStart(int beg_bci) {
    setInterval(beg_bci, (int) _method.getCodeSize());
  }

  // Iteration
  public int next() {
    int code;
    // set reading position
    _bci = _next_bci;
    if (isLastBytecode()) {
      // indicate end of bytecode stream
      code = Bytecodes._illegal;
    } else {
      // get bytecode
      int rawCode = Bytecodes.codeAt(_method, _bci);
      code = 0; // Make javac happy
      try {
        code = Bytecodes.javaCode(rawCode);
      } catch (AssertionFailure e) {
        e.printStackTrace();
        Assert.that(false, "Failure occurred at bci " + _bci + " in method " + _method.externalNameAndSignature());
      }

      // set next bytecode position
      //
      int l = Bytecodes.lengthFor(code);
      if (l == 0) l = Bytecodes.lengthAt(_method, _bci);
      _next_bci  += l;
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(_bci < _next_bci, "length must be > 0");
      }
      // set attributes
      _is_wide      = false;
      // check for special (uncommon) cases
      if (code == Bytecodes._wide) {
        code = _method.getBytecodeOrBPAt(_bci + 1);
        _is_wide = true;
      }
      if (Assert.ASSERTS_ENABLED) {
        Assert.that(Bytecodes.isJavaCode(code), "sanity check");
      }
    }
    _code = code;
    return _code;
  }

  // Stream attributes
  public Method  method()             { return _method; }
  public int     bci()                { return _bci; }
  public int     nextBCI()            { return _next_bci; }
  public int     endBCI()             { return _end_bci; }
  public int     code()               { return _code; }
  public boolean isWide()             { return _is_wide; }
  public boolean isActiveBreakpoint() { return Bytecodes.isActiveBreakpointAt(_method, _bci); }
  public boolean isLastBytecode()     { return _next_bci >= _end_bci; }

  // State changes
  public void    setNextBCI(int bci)  {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(0 <= bci && bci <= _method.getCodeSize(), "illegal bci");
    }
    _next_bci = bci;
  }

  // Bytecode-specific attributes
  public int     dest()               { return bci() + _method.getBytecodeShortArg(bci() + 1); }
  public int     dest_w()             { return bci() + _method.getBytecodeIntArg(bci()   + 1); }

  // Unsigned indices, widening
  public int     getIndex()           { return (isWide())
                                          ? (_method.getBytecodeShortArg(bci() + 2) & 0xFFFF)
                                          : (_method.getBytecodeOrBPAt(bci() + 1) & 0xFF); }
  public int     getIndexU1()         { return _method.getBytecodeOrBPAt(bci() + 1) & 0xFF; }
  public int     getIndexU2()         { return _method.getBytecodeShortArg(bci() + 1) & 0xFFFF; }
  public int     getIndexU4()         { return _method.getNativeIntArg(bci() + 1); }
  public boolean hasIndexU4()         { return code() == Bytecodes._invokedynamic; }

  public int     getIndexU1Cpcache()         { return _method.getBytecodeOrBPAt(bci() + 1) & 0xFF; }
  public int     getIndexU2Cpcache()         { return _method.getNativeShortArg(bci() + 1) & 0xFFFF; }

  // Fetch at absolute BCI (for manual parsing of certain bytecodes)
  public int     codeAt(int bci) {
    return _method.getBytecodeOrBPAt(bci);
  }
}
