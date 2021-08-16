/*
 * Copyright (c) 2002, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4558835 4915146 8181841
 * @summary Verify timezone offset and fractional seconds are correctly parsed
 * @modules java.base/sun.security.util
 */

import java.io.*;
import java.util.Date;

import sun.security.util.DerInputStream;

public class TimeParsing {

  // 10 Aug 2001 17:43:51 GMT
  private final static long TIME = 997465431000l;
  private final static long TIME_FRACT1 = 997465431700l;
  private final static long TIME_FRACT2 = 997465431760l;
  private final static long TIME_FRACT3 = 997465431765l;

  // 010810174351Z
  private final static byte[] UTC_ZULU =
    {0x17, 0x0d, 0x30, 0x31, 0x30, 0x38, 0x31, 0x30, 0x31, 0x37, 0x34, 0x33, 0x35, 0x31, 0x5a};

  // 010810184351+0100
  private final static byte[] UTC_PLUS1 =
    {0x17, 0x11, 0x30, 0x31, 0x30, 0x38, 0x31, 0x30, 0x31, 0x38, 0x34, 0x33, 0x35, 0x31, 0x2b, 0x30, 0x31, 0x30, 0x30};

  // 010810164351-0100
  private final static byte[] UTC_MINUS1 =
    {0x17, 0x11, 0x30, 0x31, 0x30, 0x38, 0x31, 0x30, 0x31, 0x36, 0x34, 0x33, 0x35, 0x31, 0x2d, 0x30, 0x31, 0x30, 0x30};

  // 20010810174351Z
  private final static byte[] GEN_ZULU =
    {0x18, 0x0f, 0x32, 0x30, 0x30, 0x31, 0x30, 0x38, 0x31, 0x30, 0x31, 0x37, 0x34, 0x33, 0x35, 0x31, 0x5a};

  // 20010810184351+0100
  private final static byte[] GEN_PLUS1 =
    {0x18, 0x13, 0x32, 0x30, 0x30, 0x31, 0x30, 0x38, 0x31, 0x30, 0x31, 0x38, 0x34, 0x33, 0x35, 0x31, 0x2b, 0x30, 0x31, 0x30, 0x30};

  // 20010810164351-0100
  private final static byte[] GEN_MINUS1 =
    {0x18, 0x13, 0x32, 0x30, 0x30, 0x31, 0x30, 0x38, 0x31, 0x30, 0x31, 0x36, 0x34, 0x33, 0x35, 0x31, 0x2d, 0x30, 0x31, 0x30, 0x30};

  // 20010810174351.7Z
  private final static byte[] GEN_FRACT1_ZULU =
    {0x18, 0x11, 0x32, 0x30, 0x30, 0x31, 0x30, 0x38, 0x31, 0x30, 0x31, 0x37, 0x34, 0x33, 0x35, 0x31, 0x2e, 0x37, 0x5a};

  // 20010810174351.76Z
  private final static byte[] GEN_FRACT2_ZULU =
    {0x18, 0x12, 0x32, 0x30, 0x30, 0x31, 0x30, 0x38, 0x31, 0x30, 0x31, 0x37, 0x34, 0x33, 0x35, 0x31, 0x2e, 0x37, 0x36, 0x5a};

  // 20010810174351.765Z
  private final static byte[] GEN_FRACT3_ZULU =
    {0x18, 0x13, 0x32, 0x30, 0x30, 0x31, 0x30, 0x38, 0x31, 0x30, 0x31, 0x37, 0x34, 0x33, 0x35, 0x31, 0x2e, 0x37, 0x36, 0x35, 0x5a};

  // 20010810174351.7654Z
  private final static byte[] GEN_FRACT4_ZULU =
    {0x18, 0x14, 0x32, 0x30, 0x30, 0x31, 0x30, 0x38, 0x31, 0x30, 0x31, 0x37, 0x34, 0x33, 0x35, 0x31, 0x2e, 0x37, 0x36, 0x35, 0x34, 0x5a};

  // 20010810184351.7+0100
  private final static byte[] GEN_FRACT1_PLUS1 =
    {0x18, 0x15, 0x32, 0x30, 0x30, 0x31, 0x30, 0x38, 0x31, 0x30, 0x31, 0x38, 0x34, 0x33, 0x35, 0x31, 0x2e, 0x37, 0x2b, 0x30, 0x31, 0x30, 0x30};

  // 20010810184351.76+0100
  private final static byte[] GEN_FRACT2_PLUS1 =
    {0x18, 0x16, 0x32, 0x30, 0x30, 0x31, 0x30, 0x38, 0x31, 0x30, 0x31, 0x38, 0x34, 0x33, 0x35, 0x31, 0x2e, 0x37, 0x36, 0x2b, 0x30, 0x31, 0x30, 0x30};

  // 20010810184351.765+0100
  private final static byte[] GEN_FRACT3_PLUS1 =
    {0x18, 0x17, 0x32, 0x30, 0x30, 0x31, 0x30, 0x38, 0x31, 0x30, 0x31, 0x38, 0x34, 0x33, 0x35, 0x31, 0x2e, 0x37, 0x36, 0x35, 0x2b, 0x30, 0x31, 0x30, 0x30};

  // 20010810184351.7654+0100
  private final static byte[] GEN_FRACT4_PLUS1 =
    {0x18, 0x18, 0x32, 0x30, 0x30, 0x31, 0x30, 0x38, 0x31, 0x30, 0x31, 0x38, 0x34, 0x33, 0x35, 0x31, 0x2e, 0x37, 0x36, 0x35, 0x34, 0x2b, 0x30, 0x31, 0x30, 0x30};

  // 20010810184351,765+0100
  private final static byte[] GEN_FRACT3_COMMA_PLUS1 =
    {0x18, 0x17, 0x32, 0x30, 0x30, 0x31, 0x30, 0x38, 0x31, 0x30, 0x31, 0x38, 0x34, 0x33, 0x35, 0x31, 0x2c, 0x37, 0x36, 0x35, 0x2b, 0x30, 0x31, 0x30, 0x30};

  // 20010810184351,7654+0100
  private final static byte[] GEN_FRACT4_COMMA_PLUS1 =
    {0x18, 0x18, 0x32, 0x30, 0x30, 0x31, 0x30, 0x38, 0x31, 0x30, 0x31, 0x38, 0x34, 0x33, 0x35, 0x31, 0x2c, 0x37, 0x36, 0x35, 0x34, 0x2b, 0x30, 0x31, 0x30, 0x30};

  private static Date decodeUTC(byte[] b) throws IOException {
    DerInputStream derin = new DerInputStream(b);
    return derin.getUTCTime();
  }

  private static Date decodeGeneralized(byte[] b) throws IOException {
    DerInputStream derin = new DerInputStream(b);
    return derin.getGeneralizedTime();
  }

  private static  void checkUTC(Date d0, byte[] b, String text) throws Exception {
    Date d1 = decodeUTC(b);
    if( !d0.equals(d1) ) {
      throw new Exception("UTCTime " + text + " failed: " + d1.toGMTString());
    } else {
      System.out.println("UTCTime " + text + " ok");
    }
  }

  private static  void checkGeneralized(Date d0, byte[] b, String text) throws Exception {
    Date d1 = decodeGeneralized(b);
    if( !d0.equals(d1) ) {
      throw new Exception("GeneralizedTime " + text + " failed: " + d1.toGMTString());
    } else {
      System.out.println("GeneralizedTime " + text + " ok");
    }
  }

  public static void main(String args[]) throws Exception {
    Date d0 = new Date(TIME);
    System.out.println(d0.toGMTString());

    checkUTC(d0, UTC_ZULU, "Zulu");
    checkUTC(d0, UTC_PLUS1, "+0100");
    checkUTC(d0, UTC_MINUS1, "-0100");

    checkGeneralized(d0, GEN_ZULU, "Zulu");
    checkGeneralized(d0, GEN_PLUS1, "+0100");
    checkGeneralized(d0, GEN_MINUS1, "-0100");

    Date d1 = new Date(TIME_FRACT1);
    checkGeneralized(d1, GEN_FRACT1_ZULU, "fractional seconds (Zulu)");
    checkGeneralized(d1, GEN_FRACT1_PLUS1, "fractional seconds (+0100)");

    Date d2 = new Date(TIME_FRACT2);
    checkGeneralized(d2, GEN_FRACT2_ZULU, "fractional seconds (Zulu)");
    checkGeneralized(d2, GEN_FRACT2_PLUS1, "fractional seconds (+0100)");

    Date d3 = new Date(TIME_FRACT3);
    checkGeneralized(d3, GEN_FRACT3_ZULU, "fractional seconds (Zulu)");
    checkGeneralized(d3, GEN_FRACT3_PLUS1, "fractional seconds (+0100)");
    checkGeneralized(d3, GEN_FRACT3_COMMA_PLUS1, "fractional seconds (+0100)");
    checkGeneralized(d3, GEN_FRACT4_ZULU, "fractional seconds (Zulu)");
    checkGeneralized(d3, GEN_FRACT4_PLUS1, "fractional seconds (+0100)");
    checkGeneralized(d3, GEN_FRACT4_COMMA_PLUS1, "fractional seconds (+0100)");
  }
}
