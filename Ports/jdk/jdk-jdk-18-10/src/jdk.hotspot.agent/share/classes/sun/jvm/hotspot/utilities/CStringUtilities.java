/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.utilities;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import sun.jvm.hotspot.debugger.*;

/** A utility class encapsulating useful operations on C strings
    represented as Addresses */

public class CStringUtilities {
  /** Return the length of a null-terminated ASCII string in the
      remote process */
  public static int getStringLength(Address addr) {
    int i = 0;
    while (addr.getCIntegerAt(i, 1, false) != 0) {
      i++;
    }
    return i;
  }

  private static String encoding = System.getProperty("file.encoding", "US-ASCII");

  public static String getString(Address addr) {
    return getString(addr, Charset.forName(encoding));
  }

  /** Fetch a null-terminated ASCII string from the remote process.
      Returns null if the argument is null, otherwise returns a
      non-null string (for example, returns an empty string if the
      first character fetched is the null terminator). */
  public static String getString(Address addr, Charset charset) {
    if (addr == null) {
      return null;
    }

    List<Byte> data = new ArrayList<>();
    byte val = 0;
    long i = 0;
    do {
      val = (byte) addr.getCIntegerAt(i, 1, false);
      if (val != 0) {
        data.add(val);
      }
      ++i;
    } while (val != 0);

    // Convert to byte[] and from there to String
    byte[] bytes = new byte[data.size()];
    for (i = 0; i < data.size(); ++i) {
      bytes[(int) i] = data.get((int) i).byteValue();
    }
    // FIXME: When we switch to use JDK 6 to build SA,
    // we can change the following to just return:
    // return new String(bytes, Charset.defaultCharset());
    return new String(bytes, charset);
  }
}
