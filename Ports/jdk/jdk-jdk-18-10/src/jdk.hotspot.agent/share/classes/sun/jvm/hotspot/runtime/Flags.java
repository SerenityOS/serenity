/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

//These definitions should be kept in sync with the definitions in the HotSpot code.

public enum Flags {
  // value origin
  DEFAULT ("Default"),
  COMMAND_LINE ("Command line"),
  ENVIRON_VAR ("Environment variable"),
  CONFIG_FILE ("Config file"),
  MANAGEMENT ("Management"),
  ERGONOMIC ("Ergonomic"),
  ATTACH_ON_DEMAND ("Attach on demand"),
  INTERNAL ("Internal"),
  JIMAGE_RESOURCE ("JImage");

  private final String value;

  Flags(String val) {
    this.value = val;
  }
  public String value() {
    return value;
  }
}
