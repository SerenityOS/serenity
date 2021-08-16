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
 */

/*
 * @test
 * @bug 4492053
 * @summary Verify invalid zero length Integer value is rejected
 * @modules java.base/sun.security.util
 */

import java.io.*;
import java.math.BigInteger;

import sun.security.util.DerInputStream;

public class BigInteger0 {

  // invalid zero length value
  private final static byte[] INT_LEN0 = { 2, 0 };

  // correct zero integer
  private final static byte[] INT0 = { 2, 1, 0 };

  public static void main(String args[]) throws Exception {
    try {
      DerInputStream derin = new DerInputStream(INT_LEN0);
      BigInteger bi = derin.getBigInteger();
      throw new Exception("Succeeded parsing invalid zero length value");
    } catch( IOException e ) {
      System.out.println("OK, zero length value rejected.");
    }

    DerInputStream derin = new DerInputStream(INT0);
    BigInteger bi = derin.getBigInteger();
    if( bi.equals(BigInteger.ZERO) == false ) {
      throw new Exception("Failed to parse Integer 0");
    }

  }

}
