/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "gc/g1/g1CodeBlobClosure.hpp"
#include "gc/g1/g1OopClosures.hpp"
#include "memory/iterator.hpp"

class G1CollectedHeap;
class G1ParScanThreadState;

// Simple holder object for a complete set of closures used by the G1 evacuation code.
template <bool should_mark>
class G1SharedClosures {
public:
  G1ParCopyClosure<G1BarrierNone, should_mark> _oops;
  G1ParCopyClosure<G1BarrierCLD,  should_mark> _oops_in_cld;
  // We do not need (and actually should not) collect oops from nmethods into the
  // optional collection set as we already automatically collect the corresponding
  // nmethods in the region's strong code roots set. So set G1BarrierNoOptRoots in
  // this closure.
  // If these were present there would be opportunity for multiple threads to try
  // to change this oop* at the same time. Since embedded oops are not necessarily
  // word-aligned, this could lead to word tearing during update and crashes.
  G1ParCopyClosure<G1BarrierNoOptRoots, should_mark> _oops_in_nmethod;

  G1CLDScanClosure                _clds;
  G1CodeBlobClosure               _codeblobs;

  G1SharedClosures(G1CollectedHeap* g1h, G1ParScanThreadState* pss, bool process_only_dirty) :
    _oops(g1h, pss),
    _oops_in_cld(g1h, pss),
    _oops_in_nmethod(g1h, pss),
    _clds(&_oops_in_cld, process_only_dirty),
    _codeblobs(pss->worker_id(), &_oops_in_nmethod, should_mark) {}
};
