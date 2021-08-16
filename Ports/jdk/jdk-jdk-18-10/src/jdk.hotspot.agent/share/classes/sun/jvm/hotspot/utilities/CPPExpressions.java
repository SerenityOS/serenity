/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

import java.util.regex.*;

/** Provides helper routines for parsing simple C++ expressions such
    as "(JavaThread *) 0xf00" and "Universe::_collectedHeap". */

public class CPPExpressions {
  private static Pattern castPattern;

  /** Represents a cast expression such as "(JavaThread *)
      0xf00". Returns both the type to which we are casting as well as
      the address. */
  public static class CastExpr {
    private String type;
    private String address;

    private CastExpr(String type, String address) {
      this.type = type;
      this.address = address;
    }

    public String getType() {
      return type;
    }

    public String getAddress() {
      return address;
    }
  }

  /** Represents a static field expression such as "Universe::_collectedHeap". */
  public static class StaticFieldExpr {
    private String containingType;
    private String fieldName;

    private StaticFieldExpr(String containingType, String fieldName) {
      this.containingType = containingType;
      this.fieldName = fieldName;
    }

    public String getContainingType() {
      return containingType;
    }

    public String getFieldName() {
      return fieldName;
    }
  }

  /** Attempts to parse the given string into a CastExpr. Returns null
      if the string did not match the supported pattern. */
  public static CastExpr parseCast(String expr) {
    if (castPattern == null) {
      castPattern = Pattern.compile("\\s*\\(\\s*([0-9A-Za-z:_]*)\\s*\\*\\s*\\)\\s*([0-9a-zA-Z]*)\\s*");
    }
    Matcher matcher = castPattern.matcher(expr);
    if (matcher.matches()) {
      String type = matcher.group(1);
      String addr = matcher.group(2);
      return new CastExpr(type, addr);
    }
    return null;
  }

  /** Attempts to parse the given string into a
      StaticFieldExpr. Returns null if the string did not match the
      supported pattern. */
  public static StaticFieldExpr parseStaticField(String expr) {
    String sep = "::";
    int idx = expr.lastIndexOf(sep);
    if (idx < 0) {
      return null;
    }
    String containingType = expr.substring(0, idx);
    String fieldName = expr.substring(idx + sep.length(), expr.length());
    return new StaticFieldExpr(containingType, fieldName);
  }
}
