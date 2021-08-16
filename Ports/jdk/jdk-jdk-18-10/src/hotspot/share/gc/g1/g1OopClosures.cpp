/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1OopClosures.inline.hpp"
#include "gc/g1/g1ParScanThreadState.hpp"
#include "memory/iterator.inline.hpp"
#include "utilities/stack.inline.hpp"

G1ParCopyHelper::G1ParCopyHelper(G1CollectedHeap* g1h,  G1ParScanThreadState* par_scan_state) :
  _g1h(g1h),
  _par_scan_state(par_scan_state),
  _worker_id(par_scan_state->worker_id()),
  _scanned_cld(NULL),
  _cm(_g1h->concurrent_mark())
{ }

G1ScanClosureBase::G1ScanClosureBase(G1CollectedHeap* g1h, G1ParScanThreadState* par_scan_state) :
  _g1h(g1h), _par_scan_state(par_scan_state)
{ }

void G1CLDScanClosure::do_cld(ClassLoaderData* cld) {
  // If the class loader data has not been dirtied we know that there's
  // no references into the young gen and we can skip it.
  if (!_process_only_dirty || cld->has_modified_oops()) {

    // Tell the closure that this class loader data is the CLD to scavenge
    // and is the one to dirty if oops are left pointing into the young gen.
    _closure->set_scanned_cld(cld);
    // Clean modified oops since we're going to scavenge all the metadata.
    cld->oops_do(_closure, ClassLoaderData::_claim_none, true /*clear_modified_oops*/);

    _closure->set_scanned_cld(NULL);

    _closure->trim_queue_partially();
  }
  _count++;
}
