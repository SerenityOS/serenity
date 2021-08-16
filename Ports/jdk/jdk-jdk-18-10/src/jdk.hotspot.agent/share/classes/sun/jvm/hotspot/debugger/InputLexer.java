/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger;

import java.io.*;

/** InputLexer is the lexer through which the current set of debuggers
    see the debug server. It provides the ability to read all of the
    types the debuggers are interested in. All read operations are
    blocking. */

public class InputLexer {
  public InputLexer(BufferedInputStream in) throws IOException {
    this.in = in;
    pushedBack = false;
  }

  public void close() throws IOException {
    in.close();
  }

  /** Parses a boolean (really either a 0 or 1 integer in US-ASCII
      encoding) on the input stream */
  public boolean parseBoolean() throws IOException {
    int val = parseInt();
    return (val != 0);
  }

  /** Parses an int in US-ASCII encoding on the input stream */
  public int parseInt() throws IOException {
    long l = parseLong();
    long mask = 0xFFFFFFFF00000000L;
    if ((l & mask) != 0) {
      throw new IOException("Overflow error reading int from debug server (read " + l + ")");
    }
    return (int) l;
  }

  /** Parses a long in US-ASCII encoding on the input stream */
  public long parseLong() throws IOException {
    skipWhitespace();
    byte b = readByte();
    if (!Character.isDigit((char) b)) {
      error();
    }
    long l = 0;
    while (Character.isDigit((char) b)) {
      l *= 10;
      l += (b - '0');
      b = readByte();
    }
    pushBack(b);
    return l;
  }

  /** Parses an address in the form 0x12345678 in US-ASCII encoding on
      the input stream */
  public long parseAddress() throws IOException {
    skipWhitespace();
    byte b;
    if ((b = readByte()) != '0') {
      error();
    }
    b = readByte();
    if (b != 'x') {
      error();
    }
    long val = 0;
    while (isHexDigit((char) (b = readByte()))) {
      val *= 16;
      val += Character.digit((char) b, 16);
    }
    pushBack(b);
    return val;
  }

  public void skipByte() throws IOException {
    readByte();
  }

  /** Reads binary data; one byte */
  public byte readByte() throws IOException {
    if (pushedBack) {
      pushedBack = false;
      return backBuf;
    }
    return readByteInternal();
  }

  /** Reads a block of binary data in BLOCKING fashion */
  public void readBytes(byte[] buf, int off, int len) throws IOException {
    int startIdx = off;
    int numRead = 0;
    if (pushedBack) {
      buf[startIdx] = backBuf;
      pushedBack = false;
      ++startIdx;
      ++numRead;
    }
    while (numRead < len) {
      numRead += in.read(buf, startIdx + numRead, len - numRead);
    }
    //    if (numRead != len) {
    //      throw new IOException("Only read " + numRead + " out of " +
    //                            len + " bytes requested");
    //    }
  }

  /** Reads binary data; one 16-bit character in big-endian format */
  public char readChar() throws IOException {
    int hi = ((int) readByte()) & 0xFF;
    int lo = ((int) readByte()) & 0xFF;
    return (char) ((hi << 8) | lo);
  }

  /** Reads binary data; one 32-bit unsigned int in big-endian format.
      Returned as a long. */
  public long readUnsignedInt() throws IOException {
    long b1 = ((long) readByte()) & 0xFF;
    long b2 = ((long) readByte()) & 0xFF;
    long b3 = ((long) readByte()) & 0xFF;
    long b4 = ((long) readByte()) & 0xFF;

    return ((b1 << 24) | (b2 << 16) | (b3 << 8) | b4);
  }

  /** Reads binary data; a US-ASCII string of the specified length */
  public String readByteString(int len) throws IOException {
    byte[] b = new byte[len];
    for (int i = 0; i < len; i++) {
      b[i] = readByte();
    }
    try {
      return new String(b, "US-ASCII");
    }
    catch (UnsupportedEncodingException e) {
      throw new IOException(e.toString());
    }
  }

  /** Reads binary data; a Unicode string of the specified length */
  public String readCharString(int len) throws IOException {
    char[] c = new char[len];
    for (int i = 0; i < len; i++) {
      c[i] = readChar();
    }
    return new String(c);
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void skipWhitespace() throws IOException {
    byte b;
    while (Character.isWhitespace((char) (b = readByte()))) {
    }
    pushBack(b);
  }

  private boolean isHexDigit(char c) {
    return (('0' <= c && c <= '9') ||
            ('a' <= c && c <= 'f') ||
            ('A' <= c && c <= 'F'));
  }

  private void pushBack(byte b) {
    if (pushedBack) {
      throw new InternalError("Only one character pushback supported");
    }
    backBuf = b;
    pushedBack = true;
  }

  private byte readByteInternal() throws IOException {
    int i = in.read();
    if (i == -1) {
      throw new IOException("End-of-file reached while reading from server");
    }
    return (byte) i;
  }

  private void error() throws IOException {
    throw new IOException("Error parsing output of debug server");
  }

  private BufferedInputStream in;
  private boolean pushedBack;
  private byte backBuf;
}
