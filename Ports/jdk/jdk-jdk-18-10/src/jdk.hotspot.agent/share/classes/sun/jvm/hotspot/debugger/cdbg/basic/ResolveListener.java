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

import sun.jvm.hotspot.debugger.cdbg.*;

/** Provides notification about failed resolutions in the debug info
    database without causing the entire resolve operation to fail */
public interface ResolveListener {
  /** Indicates failure to resolve a type within another type */
  public void resolveFailed(Type containingType, LazyType failedResolve, String detail);

  /** Indicates failure to resolve the address of a static field in a
      type */
  public void resolveFailed(Type containingType, String staticFieldName);

  /** Indicates failure to resolve reference to a type from a symbol */
  public void resolveFailed(Sym containingSymbol, LazyType failedResolve, String detail);

  /** Indicates failure to resolve reference from one symbol to
      another (currently occurs only from BlockSyms to other BlockSyms) */
  public void resolveFailed(Sym containingSymbol, LazyBlockSym failedResolve, String detail);
}
