/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.whitebox.code;

import java.lang.reflect.Executable;
import jdk.test.whitebox.WhiteBox;

public class NMethod extends CodeBlob {
  private static final WhiteBox wb = WhiteBox.getWhiteBox();
  public static NMethod get(Executable method, boolean isOsr) {
    Object[] obj = wb.getNMethod(method, isOsr);
    return obj == null ? null : new NMethod(obj);
  }
  private NMethod(Object[] obj) {
    super((Object[])obj[0]);
    assert obj.length == 5;
    comp_level = (Integer) obj[1];
    insts = (byte[]) obj[2];
    compile_id = (Integer) obj[3];
    entry_point = (Long) obj[4];
  }
  public final byte[] insts;
  public final int comp_level;
  public final int compile_id;
  public final long entry_point;

  @Override
  public String toString() {
    return "NMethod{"
        + super.toString()
        + ", insts=" + insts
        + ", comp_level=" + comp_level
        + ", compile_id=" + compile_id
        + ", entry_point=" + entry_point
        + '}';
  }
}
