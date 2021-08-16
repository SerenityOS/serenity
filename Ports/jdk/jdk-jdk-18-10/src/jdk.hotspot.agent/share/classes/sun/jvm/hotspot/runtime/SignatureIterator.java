/*
 * Copyright (c) 2001, 2010, Oracle and/or its affiliates. All rights reserved.
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

/** <P> SignatureIterators iterate over a Java signature (or parts of it).
    (Syntax according to: "The Java Virtual Machine Specification" by
    Tim Lindholm & Frank Yellin; section 4.3 Descriptors; p. 89ff.) </P>

    <P> Example: Iterating over
<PRE>
([Lfoo;D)I
0123456789
</PRE>

    using </P>

<PRE>
iterateParameters() calls: do_array(2, 7); do_double();
iterateReturntype() calls:                              do_int();
iterate()           calls: do_array(2, 7); do_double(); do_int();

is_returnType()        is: false         ; false      ; true
</PRE>
*/

public abstract class SignatureIterator {
  protected Symbol _signature;       // the signature to iterate over
  protected int    _index;           // the current character index (only valid during iteration)
  protected int    _parameter_index; // the current parameter index (0 outside iteration phase)

  protected void expect(char c) {
    if (_signature.getByteAt(_index) != (byte) c) {
      throw new RuntimeException("expecting '" + c + "'");
    }
    _index++;
  }
  protected void skipOptionalSize() {
    byte c = _signature.getByteAt(_index);
    while ('0' <= c && c <= '9') {
      c = _signature.getByteAt(++_index);
    }
  }
  // returns the parameter size in words (0 for void)
  protected int parseType() {
    switch(_signature.getByteAt(_index)) {
    case 'B': doByte  (); _index++; return BasicTypeSize.getTByteSize();
    case 'C': doChar  (); _index++; return BasicTypeSize.getTCharSize();
    case 'D': doDouble(); _index++; return BasicTypeSize.getTDoubleSize();
    case 'F': doFloat (); _index++; return BasicTypeSize.getTFloatSize();
    case 'I': doInt   (); _index++; return BasicTypeSize.getTIntSize();
    case 'J': doLong  (); _index++; return BasicTypeSize.getTLongSize();
    case 'S': doShort (); _index++; return BasicTypeSize.getTShortSize();
    case 'Z': doBool  (); _index++; return BasicTypeSize.getTBooleanSize();
    case 'V':
      {
        if (!isReturnType()) {
          throw new RuntimeException("illegal parameter type V (void)");
        }

        doVoid(); _index++;
        return BasicTypeSize.getTVoidSize();
      }
    case 'L':
      {
        int begin = ++_index;
        while (_signature.getByteAt(_index++) != ';') ;
        doObject(begin, _index);
        return BasicTypeSize.getTObjectSize();
      }
    case '[':
      {
        int begin = ++_index;
        skipOptionalSize();
        while (_signature.getByteAt(_index) == '[') {
          _index++;
          skipOptionalSize();
        }
        if (_signature.getByteAt(_index) == 'L') {
          while (_signature.getByteAt(_index++) != ';') ;
        } else {
          _index++;
        }
        doArray(begin, _index);
        return BasicTypeSize.getTArraySize();
      }
    }
    throw new RuntimeException("Should not reach here: char " + (char)_signature.getByteAt(_index) + " @ " + _index + " in " + _signature.asString());
  }
  protected void checkSignatureEnd() {
    if (_index < _signature.getLength()) {
      System.err.println("too many chars in signature");
      _signature.printValueOn(System.err);
      System.err.println(" @ " + _index);
    }
  }

  public SignatureIterator(Symbol signature) {
    _signature       = signature;
    _parameter_index = 0;
  }

  //
  // Iteration
  //

  // dispatches once for field signatures
  public void dispatchField() {
    // no '(', just one (field) type
    _index = 0;
    _parameter_index = 0;
    parseType();
    checkSignatureEnd();
  }

  // iterates over parameters only
  public void iterateParameters() {
    // Parse parameters
    _index = 0;
    _parameter_index = 0;
    expect('(');
    while (_signature.getByteAt(_index) != ')') {
      _parameter_index += parseType();
    }
    expect(')');
    _parameter_index = 0; // so isReturnType() is false outside iteration
  }

  // iterates over returntype only
  public void iterateReturntype() {
    // Ignore parameters
    _index = 0;
    expect('(');
    while (_signature.getByteAt(_index) != ')') {
      _index++;
    }
    expect(')');
    // Parse return type
    _parameter_index = -1;
    parseType();
    checkSignatureEnd();
    _parameter_index = 0; // so isReturnType() is false outside iteration
  }

  // iterates over whole signature
  public void iterate() {
    // Parse parameters
    _index = 0;
    _parameter_index = 0;
    expect('(');
    while (_signature.getByteAt(_index) != ')') {
      _parameter_index += parseType();
    }
    expect(')');
    // Parse return type
    _parameter_index = -1;
    parseType();
    checkSignatureEnd();
    _parameter_index = 0; // so isReturnType() is false outside iteration
  }

  // Returns the word index of the current parameter; returns a negative value at the return type
  public int  parameterIndex()               { return _parameter_index; }
  public boolean isReturnType()              { return (parameterIndex() < 0); }

  // Basic types
  public abstract void doBool  ();
  public abstract void doChar  ();
  public abstract void doFloat ();
  public abstract void doDouble();
  public abstract void doByte  ();
  public abstract void doShort ();
  public abstract void doInt   ();
  public abstract void doLong  ();
  public abstract void doVoid  ();

  // Object types (begin indexes the first character of the entry, end
  // indexes the first character after the entry)
  public abstract void doObject(int begin, int end);
  public abstract void doArray (int begin, int end);
}
