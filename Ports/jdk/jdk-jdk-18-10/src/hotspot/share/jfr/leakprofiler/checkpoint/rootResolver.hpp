/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_LEAKPROFILER_CHECKPOINT_ROOTRESOLVER_HPP
#define SHARE_JFR_LEAKPROFILER_CHECKPOINT_ROOTRESOLVER_HPP

#include "jfr/leakprofiler/utilities/rootType.hpp"
#include "jfr/leakprofiler/utilities/unifiedOopRef.hpp"
#include "memory/allocation.hpp"
#include "oops/oopsHierarchy.hpp"

struct RootCallbackInfo {
  address _high;
  address _low;
  const void* _context;
  OldObjectRoot::System _system;
  OldObjectRoot::Type _type;
};

class RootCallback {
 public:
  virtual bool process(const RootCallbackInfo& info) = 0;
  virtual int entries() const = 0;
  virtual UnifiedOopRef at(int idx) const = 0;
};

class RootResolver : public AllStatic {
 public:
  static void resolve(RootCallback& callback);
};

#endif // SHARE_JFR_LEAKPROFILER_CHECKPOINT_ROOTRESOLVER_HPP
