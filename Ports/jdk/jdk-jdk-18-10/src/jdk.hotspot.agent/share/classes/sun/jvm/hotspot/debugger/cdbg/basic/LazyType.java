/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.cdbg.basic;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;
import sun.jvm.hotspot.utilities.Assert;

public class LazyType extends BasicType {
  private Object key;
  private int    cvAttributes;

  public LazyType(Object key) {
    this(key, 0);
  }

  private LazyType(Object key, int cvAttributes) {
    super(null, 0, cvAttributes);
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(key != null, "key must not be null");
    }
    this.key = key;
    this.cvAttributes = cvAttributes;
  }

  public boolean isLazy() { return true; }
  public Object getKey()  { return key; }

  Type resolveTypes(BasicCDebugInfoDataBase db, ResolveListener listener) {
    BasicType t = (BasicType) db.resolveType(this, this, listener, "resolving lazy type");
    // Returned type might be lazy if there was an error
    if (t.isLazy()) {
      return this;
    }
    if (cvAttributes != 0) {
      return t.getCVVariant(cvAttributes);
    }
    return t;
  }

  public void iterateObject(Address a, ObjectVisitor v, FieldIdentifier f) {}
  protected Type createCVVariant(int cvAttributes) {
    return new LazyType(key, cvAttributes);
  }

  public void visit(TypeVisitor v) {}
}
