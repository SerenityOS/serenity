/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classLoaderDataGraph.hpp"
#include "jfr/leakprofiler/utilities/saveRestore.hpp"
#include "oops/oop.inline.hpp"

MarkWordContext::MarkWordContext() : _obj(NULL), _mark_word(markWord::zero()) {}

MarkWordContext::MarkWordContext(const oop obj) : _obj(obj), _mark_word(obj->mark()) {
  assert(_obj->mark() == _mark_word, "invariant");
  // now we will "poison" the mark word of the object
  // to the intermediate monitor INFLATING state.
  // This is an "impossible" state during a safepoint,
  // hence we will use it to quickly identify objects
  // during the reachability search from gc roots.
  assert(markWord::zero() == markWord::INFLATING(), "invariant");
  _obj->set_mark(markWord::INFLATING());
  assert(markWord::zero() == obj->mark(), "invariant");
}

MarkWordContext::~MarkWordContext() {
  if (_obj != NULL) {
    _obj->set_mark(_mark_word);
    assert(_obj->mark() == _mark_word, "invariant");
  }
}

MarkWordContext::MarkWordContext(const MarkWordContext& rhs) : _obj(NULL), _mark_word(markWord::zero()) {
  swap(const_cast<MarkWordContext&>(rhs));
}

void MarkWordContext::operator=(MarkWordContext rhs) {
  swap(rhs);
}

void MarkWordContext::swap(MarkWordContext& rhs) {
  oop temp_obj = rhs._obj;
  markWord temp_mark_word = rhs._mark_word;
  rhs._obj = _obj;
  rhs._mark_word = _mark_word;
  _obj = temp_obj;
  _mark_word = temp_mark_word;
}

CLDClaimContext::CLDClaimContext() : _cld(NULL) {}

CLDClaimContext::CLDClaimContext(ClassLoaderData* cld) : _cld(cld) {
  assert(_cld->claimed(), "invariant");
  _cld->clear_claim();
}

CLDClaimContext::~CLDClaimContext() {
  if (_cld != NULL) {
    _cld->try_claim(ClassLoaderData::_claim_strong);
    assert(_cld->claimed(), "invariant");
  }
}

CLDClaimContext::CLDClaimContext(const CLDClaimContext& rhs) : _cld(NULL) {
  swap(const_cast<CLDClaimContext&>(rhs));
}

void CLDClaimContext::operator=(CLDClaimContext rhs) {
  swap(rhs);
}

void CLDClaimContext::swap(CLDClaimContext& rhs) {
  ClassLoaderData* temp_cld = rhs._cld;
  rhs._cld = _cld;
  _cld = temp_cld;
}

CLDClaimStateClosure::CLDClaimStateClosure() : CLDClosure(), _state() {}

void CLDClaimStateClosure::do_cld(ClassLoaderData* cld) {
  assert(cld != NULL, "invariant");
  if (cld->claimed()) {
    _state.save(cld);
  }
}

SaveRestoreCLDClaimBits::SaveRestoreCLDClaimBits() : _claim_state_closure() {
  // interferes with GC, so walk all oops that GC would.
  ClassLoaderDataGraph::cld_do(&_claim_state_closure);
}

SaveRestoreCLDClaimBits::~SaveRestoreCLDClaimBits() {
  ClassLoaderDataGraph::clear_claimed_marks();
}
