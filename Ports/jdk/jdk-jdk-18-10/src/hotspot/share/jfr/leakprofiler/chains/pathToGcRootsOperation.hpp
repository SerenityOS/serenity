/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_LEAKPROFILER_CHAINS_PATHTOGCROOTSOPERATION_HPP
#define SHARE_JFR_LEAKPROFILER_CHAINS_PATHTOGCROOTSOPERATION_HPP

#include "jfr/leakprofiler/utilities/vmOperation.hpp"

class EdgeStore;
class ObjectSampler;

// Safepoint operation for finding paths to gc roots
class PathToGcRootsOperation : public OldObjectVMOperation {
 private:
  ObjectSampler* _sampler;
  EdgeStore* const _edge_store;
  const int64_t _cutoff_ticks;
  const bool _emit_all;
  const bool _skip_bfs;

 public:
  PathToGcRootsOperation(ObjectSampler* sampler, EdgeStore* edge_store, int64_t cutoff, bool emit_all, bool skip_bfs);
  virtual void doit();
};

#endif // SHARE_JFR_LEAKPROFILER_CHAINS_PATHTOGCROOTSOPERATION_HPP
