/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_ACCESSBARRIERSUPPORT_HPP
#define SHARE_GC_SHARED_ACCESSBARRIERSUPPORT_HPP

#include "memory/allocation.hpp"
#include "oops/access.hpp"

class AccessBarrierSupport: AllStatic {
private:
  static DecoratorSet resolve_unknown_oop_ref_strength(DecoratorSet decorators, oop base, ptrdiff_t offset);

public:
  // Some collectors, like G1, needs to keep referents alive when loading them.
  // Therefore, for APIs that accept unknown oop ref strength (e.g. unsafe),
  // we need to dynamically find out if a given field is on a java.lang.ref.Reference object.
  // and in that case what strength it has.
  template<DecoratorSet decorators>
  static DecoratorSet resolve_possibly_unknown_oop_ref_strength(oop base, ptrdiff_t offset);
};

#endif // SHARE_GC_SHARED_ACCESSBARRIERSUPPORT_HPP
