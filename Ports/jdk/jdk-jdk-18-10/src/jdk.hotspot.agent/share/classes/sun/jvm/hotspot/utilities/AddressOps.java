/*
 * Copyright (c) 2000, 2002, Oracle and/or its affiliates. All rights reserved.
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

import sun.jvm.hotspot.debugger.*;

/** Helper class with operations on addresses. Solves the problem of
    one or both of the arguments being null and needing to call
    methods like greaterThan(), lessThan(), etc. on them. */

public class AddressOps {
  /** Returns true if a1 is less than a2. Either or both may be null. */
  public static boolean lessThan(Address a1, Address a2) {
    if (a2 == null) {
      return false;
    } else if (a1 == null) {
      return true;
    } else {
      return a1.lessThan(a2);
    }
  }

  /** Returns true if a1 is less than or equal to a2. Either or both may be null. */
  public static boolean lessThanOrEqual(Address a1, Address a2) {
    if (a2 == null) {
      return (a1 == null);
    } else if (a1 == null) {
      return true;
    } else {
      return a1.lessThanOrEqual(a2);
    }
  }

  /** Returns true if a1 is greater than a2. Either or both may be null. */
  public static boolean greaterThan(Address a1, Address a2) {
    if (a1 == null) {
      return false;
    } else if (a2 == null) {
      return true;
    } else {
      return a1.greaterThan(a2);
    }
  }

  /** Returns true if a1 is greater than or equal to a2. Either or both may be null. */
  public static boolean greaterThanOrEqual(Address a1, Address a2) {
    if (a1 == null) {
      return (a2 == null);
    } else if (a2 == null) {
      return true;
    } else {
      return a1.greaterThanOrEqual(a2);
    }
  }

  /** Returns true if a1 is equal to a2. Either or both may be null. */
  public static boolean equal(Address a1, Address a2) {
    if ((a1 == null) && (a2 == null)) {
      return true;
    }

    if ((a1 == null) || (a2 == null)) {
      return false;
    }

    return (a1.equals(a2));
  }

  /** Shorthand for {@link #lessThan} */
  public static boolean lt(Address a1, Address a2) {
    return lessThan(a1, a2);
  }

  /** Shorthand for {@link #lessThanOrEqual} */
  public static boolean lte(Address a1, Address a2) {
    return lessThanOrEqual(a1, a2);
  }

  /** Shorthand for {@link #greaterThan} */
  public static boolean gt(Address a1, Address a2) {
    return greaterThan(a1, a2);
  }

  /** Shorthand for {@link #greaterThanOrEqual} */
  public static boolean gte(Address a1, Address a2) {
    return greaterThanOrEqual(a1, a2);
  }

  /** Returns maximum of the two addresses */
  public static Address max(Address a1, Address a2) {
    return (gt(a1, a2) ? a1 : a2);
  }

  /** Returns minimum of the two addresses */
  public static Address min(Address a1, Address a2) {
    return (lt(a1, a2) ? a1 : a2);
  }
}
