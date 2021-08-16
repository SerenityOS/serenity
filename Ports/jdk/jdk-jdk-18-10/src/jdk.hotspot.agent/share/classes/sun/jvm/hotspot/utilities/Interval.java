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

/** An interval is an immutable data structure defined by its two
    endpoints. */

import java.util.Comparator;

public class Interval {
  private Object lowEndpoint;
  private Object highEndpoint;

  /** It is required that the low endpoint be less than or equal to
      the high endpoint according to the Comparator which will be
      passed into the overlaps() routines. */
  public Interval(Object lowEndpoint, Object highEndpoint) {
    this.lowEndpoint = lowEndpoint;
    this.highEndpoint = highEndpoint;
  }

  public Object getLowEndpoint() {
    return lowEndpoint;
  }

  public Object getHighEndpoint() {
    return highEndpoint;
  }

  /** This takes the Interval to compare against as well as a
      Comparator which will be applied to the low and high endpoints
      of the given intervals. */
  public boolean overlaps(Interval arg, Comparator<Object> endpointComparator) {
    return overlaps(arg.getLowEndpoint(), arg.getHighEndpoint(), endpointComparator);
  }

  /** Routine which can be used instead of the one taking an interval,
      for the situation where the endpoints are being retrieved from
      different data structures */
  public boolean overlaps(Object otherLowEndpoint,
                          Object otherHighEndpoint,
                          Comparator<Object> endpointComparator) {
    if (endpointComparator.compare(highEndpoint, otherLowEndpoint) <= 0) {
      return false;
    }
    if (endpointComparator.compare(lowEndpoint, otherHighEndpoint) >= 0) {
      return false;
    }
    return true;
  }

  public String toString() {
    return "[ " + getLowEndpoint().toString() + ", " + getHighEndpoint().toString() + ")";
  }
}
