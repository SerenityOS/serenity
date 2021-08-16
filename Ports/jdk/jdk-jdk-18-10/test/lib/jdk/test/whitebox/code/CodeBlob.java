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

import jdk.test.whitebox.WhiteBox;

public class CodeBlob {
  private static final WhiteBox WB = WhiteBox.getWhiteBox();
  public static CodeBlob[] getCodeBlobs(BlobType type) {
    Object[] obj = WB.getCodeHeapEntries(type.id);
    if (obj == null) {
      return null;
    }
    CodeBlob[] result = new CodeBlob[obj.length];
    for (int i = 0, n = result.length; i < n; ++i) {
      result[i] = new CodeBlob((Object[]) obj[i]);
    }
    return result;
  }
  public static CodeBlob getCodeBlob(long addr) {
    Object[] obj = WB.getCodeBlob(addr);
    if (obj == null) {
      return null;
    }
    return new CodeBlob(obj);
  }
  protected CodeBlob(Object[] obj) {
    assert obj.length == 4;
    name = (String) obj[0];
    size = (Integer) obj[1];
    int blob_type_index = (Integer) obj[2];
    code_blob_type = BlobType.values()[blob_type_index];
    assert code_blob_type.id == (Integer) obj[2];
    address = (Long) obj[3];
  }
  public final String name;
  public final int size;
  public final BlobType code_blob_type;
  public final long address;
  @Override
  public String toString() {
    return "CodeBlob{"
        + "name=" + name
        + ", size=" + size
        + ", code_blob_type=" + code_blob_type
        + ", address=" + address
        + '}';
  }
}
