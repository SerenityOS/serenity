/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "code/codeCache.hpp"
#include "code/nmethod.hpp"
#include "gc/shared/scavengableNMethods.hpp"
#include "gc/shared/scavengableNMethodsData.hpp"
#include "runtime/mutexLocker.hpp"
#include "utilities/debug.hpp"

static ScavengableNMethodsData gc_data(nmethod* nm) {
  return ScavengableNMethodsData(nm);
}

nmethod* ScavengableNMethods::_head = NULL;
BoolObjectClosure* ScavengableNMethods::_is_scavengable = NULL;

void ScavengableNMethods::initialize(BoolObjectClosure* is_scavengable) {
  _is_scavengable = is_scavengable;
}

// Conditionally adds the nmethod to the list if it is
// not already on the list and has a scavengeable root.
void ScavengableNMethods::register_nmethod(nmethod* nm) {
  assert_locked_or_safepoint(CodeCache_lock);

  ScavengableNMethodsData data = gc_data(nm);

  if (data.on_list() || !has_scavengable_oops(nm)) {
    return;
  }

  data.set_on_list();
  data.set_next(_head);

  _head = nm;
}

void ScavengableNMethods::unregister_nmethod(nmethod* nm) {
  assert_locked_or_safepoint(CodeCache_lock);

  if (gc_data(nm).on_list()) {
    nmethod* prev = NULL;
    for (nmethod* cur = _head; cur != NULL; cur = gc_data(cur).next()) {
      if (cur == nm) {
        unlist_nmethod(cur, prev);
        return;
      }
      prev = cur;
    }
  }
}

#ifndef PRODUCT

class DebugScavengableOops: public OopClosure {
  BoolObjectClosure* _is_scavengable;
  nmethod*           _nm;
  bool               _ok;
public:
  DebugScavengableOops(BoolObjectClosure* is_scavengable, nmethod* nm) :
      _is_scavengable(is_scavengable),
      _nm(nm),
      _ok(true) { }

  bool ok() { return _ok; }
  virtual void do_oop(oop* p) {
    if (*p == NULL || !_is_scavengable->do_object_b(*p)) {
      return;
    }

    if (_ok) {
      _nm->print_nmethod(true);
      _ok = false;
    }
    tty->print_cr("*** scavengable oop " PTR_FORMAT " found at " PTR_FORMAT " (offset %d)",
                  p2i(*p), p2i(p), (int)((intptr_t)p - (intptr_t)_nm));
    (*p)->print();
  }
  virtual void do_oop(narrowOop* p) { ShouldNotReachHere(); }
};

#endif // PRODUCT

void ScavengableNMethods::verify_nmethod(nmethod* nm) {
#ifndef PRODUCT
  if (!gc_data(nm).on_list()) {
    // Actually look inside, to verify the claim that it's clean.
    DebugScavengableOops cl(_is_scavengable, nm);
    nm->oops_do(&cl);
    if (!cl.ok())
      fatal("found an unadvertised bad scavengable oop in the code cache");
  }
  assert(gc_data(nm).not_marked(), "");
#endif // PRODUCT
}

class HasScavengableOops: public OopClosure {
  BoolObjectClosure* _is_scavengable;
  bool               _found;
public:
  HasScavengableOops(BoolObjectClosure* is_scavengable, nmethod* nm) :
      _is_scavengable(is_scavengable),
      _found(false) {}

  bool found() { return _found; }
  virtual void do_oop(oop* p) {
    if (!_found && *p != NULL && _is_scavengable->do_object_b(*p)) {
      _found = true;
    }
  }
  virtual void do_oop(narrowOop* p) { ShouldNotReachHere(); }
};

bool ScavengableNMethods::has_scavengable_oops(nmethod* nm) {
  HasScavengableOops cl(_is_scavengable, nm);
  nm->oops_do(&cl);
  return cl.found();
}

// Walk the list of methods which might contain oops to the java heap.
void ScavengableNMethods::nmethods_do_and_prune(CodeBlobToOopClosure* cl) {
  assert_locked_or_safepoint(CodeCache_lock);

  debug_only(mark_on_list_nmethods());

  nmethod* prev = NULL;
  nmethod* cur = _head;
  while (cur != NULL) {
    assert(cur->is_alive(), "Must be");

    ScavengableNMethodsData data = gc_data(cur);
    debug_only(data.clear_marked());
    assert(data.on_list(), "else shouldn't be on this list");

    if (cl != NULL) {
      cl->do_code_blob(cur);
    }

    nmethod* const next = data.next();

    if (!has_scavengable_oops(cur)) {
      unlist_nmethod(cur, prev);
    } else {
      prev = cur;
    }

    cur = next;
  }

  // Check for stray marks.
  debug_only(verify_unlisted_nmethods(NULL));
}

void ScavengableNMethods::prune_nmethods() {
  nmethods_do_and_prune(NULL /* No closure */);
}

// Walk the list of methods which might contain oops to the java heap.
void ScavengableNMethods::nmethods_do(CodeBlobToOopClosure* cl) {
  nmethods_do_and_prune(cl);
}

#ifndef PRODUCT
void ScavengableNMethods::asserted_non_scavengable_nmethods_do(CodeBlobClosure* cl) {
  // While we are here, verify the integrity of the list.
  mark_on_list_nmethods();
  for (nmethod* cur = _head; cur != NULL; cur = gc_data(cur).next()) {
    assert(gc_data(cur).on_list(), "else shouldn't be on this list");
    gc_data(cur).clear_marked();
  }
  verify_unlisted_nmethods(cl);
}
#endif // PRODUCT

void ScavengableNMethods::unlist_nmethod(nmethod* nm, nmethod* prev) {
  assert_locked_or_safepoint(CodeCache_lock);

  assert((prev == NULL && _head == nm) ||
         (prev != NULL && gc_data(prev).next() == nm), "precondition");

  ScavengableNMethodsData data = gc_data(nm);

  if (prev == NULL) {
    _head = data.next();
  } else {
    gc_data(prev).set_next(data.next());
  }
  data.set_next(NULL);
  data.clear_on_list();
}

#ifndef PRODUCT
// Temporarily mark nmethods that are claimed to be on the scavenge list.
void ScavengableNMethods::mark_on_list_nmethods() {
  NMethodIterator iter(NMethodIterator::only_alive);
  while(iter.next()) {
    nmethod* nm = iter.method();
    ScavengableNMethodsData data = gc_data(nm);
    assert(data.not_marked(), "clean state");
    if (data.on_list())
      data.set_marked();
  }
}

// If the closure is given, run it on the unlisted nmethods.
// Also make sure that the effects of mark_on_list_nmethods is gone.
void ScavengableNMethods::verify_unlisted_nmethods(CodeBlobClosure* cl) {
  NMethodIterator iter(NMethodIterator::only_alive);
  while(iter.next()) {
    nmethod* nm = iter.method();

    verify_nmethod(nm);

    if (cl != NULL && !gc_data(nm).on_list()) {
      cl->do_code_blob(nm);
    }
  }
}

#endif //PRODUCT
