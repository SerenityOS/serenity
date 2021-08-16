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
 * @bug 4216191 4721369 4807283
   @summary Test to validate case insensitivity of encoding alias names
 */

// Fixed since 1.4.0 by virtue of NIO charset lookup mechanism
// which is by design case insensitive

import java.lang.*;
import java.io.*;

public class CheckCaseInsensitiveEncAliases
{
  public static void main(String args[]) throws Exception
  {
    // Try various encoding names in mixed cases
    // Tests subset of encoding names provided within bugID 4216191

    // Various forms of US-ASCII
    tryToEncode( "ANSI_X3.4-1968" );
    tryToEncode( "iso-ir-6" );
    tryToEncode( "ANSI_X3.4-1986" );
    tryToEncode( "ISO_646.irv:1991" );
    tryToEncode( "ASCII" );
    tryToEncode( "ascii" );
    tryToEncode( "Ascii" );
    tryToEncode( "Ascii7" );
    tryToEncode( "ascii7" );
    tryToEncode( "ISO646-US" );
    tryToEncode( "US-ASCII" );
    tryToEncode( "us-ascii" );
    tryToEncode( "US-Ascii" );
    tryToEncode( "us" );
    tryToEncode( "IBM367" );
    tryToEncode( "cp367" );
    tryToEncode( "csASCII" );

    // Variants on Unicode
    tryToEncode( "Unicode" );
    tryToEncode( "UNICODE" );
    tryToEncode( "unicode" );

    // Variants on Big5
    tryToEncode( "Big5" );
    tryToEncode( "big5" );
    tryToEncode( "bIg5" );
    tryToEncode( "biG5" );
    tryToEncode( "bIG5" );

    // Variants of Cp1252
    tryToEncode( "Cp1252" );
    tryToEncode( "cp1252" );
    tryToEncode( "CP1252" );

    // Variants of PCK
    tryToEncode( "pck" );
    tryToEncode( "Pck" );

  }


  public static final String ENCODE_STRING = "Encode me";

  public static void tryToEncode( String encoding) throws Exception
  {
    try
    {
      byte[] bytes = ENCODE_STRING.getBytes( encoding );
      System.out.println( "Encoding \"" + encoding + "\" recognized" );
    }
    catch( UnsupportedEncodingException e )
    {
      throw new Exception("Encoding \"" + encoding + "\" NOT recognized");
    }
  }
}
