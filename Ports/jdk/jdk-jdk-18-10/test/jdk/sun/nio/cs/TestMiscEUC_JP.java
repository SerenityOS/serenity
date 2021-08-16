/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4121376
   @summary Verify that EUC_JP 0x8FA2B7 maps to \uFF5E
 */

import java.nio.*;
import java.nio.charset.*;

public class TestMiscEUC_JP {

  public static void main(String[] args) throws Exception
  {
    Charset cs = Charset.forName("EUC_JP");
    CharsetDecoder dec  = cs.newDecoder();
    CharsetEncoder enc  = cs.newEncoder();
    byte[] euc           = {(byte)0x8F, (byte)0xA2, (byte)0xB7};

    CharBuffer cb = dec.decode(ByteBuffer.wrap(euc));
    if (cb.charAt(0) != 0xFF5E) {
      throw new Exception("Converted EUC_JP 0x8FA2B7 to: 0x"
                          + Integer.toHexString((int)cb.charAt(0)));
    }
    ByteBuffer bb = enc.encode(cb);

    if (!((bb.limit() == 3)
          && (bb.get() == euc[0])
          && (bb.get() == euc[1])
          && (bb.get() == euc[2]))) {
      cb.flip();
      bb.flip();
      throw new Exception("Roundrip failed for char 0x"
                          + Integer.toHexString((int)cb.charAt(0)) + ": "
                          + Integer.toHexString(bb.limit()) + " 0x"
                          + Integer.toHexString((int)bb.get() & 0xff) + " "
                          + Integer.toHexString((int)bb.get() & 0xff) + " "
                          + Integer.toHexString((int)bb.get() & 0xff));
    }
  }
}
