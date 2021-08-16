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

#ifndef SHARE_JFR_LEAKPROFILER_UTILITIES_SAVERESTORE_HPP
#define SHARE_JFR_LEAKPROFILER_UTILITIES_SAVERESTORE_HPP

#include "memory/allocation.hpp"
#include "memory/iterator.hpp"
#include "oops/markWord.hpp"
#include "utilities/growableArray.hpp"

template <typename T, typename Impl>
class SaveRestore {
 private:
  Impl _impl;
 public:
  SaveRestore() : _impl() {
    _impl.setup();
  }

  void save(T const& value) {
    _impl.save(value);
  }

  ~SaveRestore() {
    _impl.restore();
  }
};

template <typename T, typename Context>
class ContextStore {
private:
  GrowableArray<Context>* _storage;
public:
  ContextStore() : _storage(NULL) {}

  void setup() {
    assert(_storage == NULL, "invariant");
    _storage = new GrowableArray<Context>(16);
  }

  void save(T const& value) {
    _storage->push(Context(value));
  }

  void restore() {
    for (int i = 0; i < _storage->length(); ++i) {
      _storage->at(i).~Context();
    }
  }
};

/*
* This class will save the original mark oop of an object sample object.
* It will then install an "identifier" mark oop to be used for
* identification purposes in the search for reference chains.
* The destructor will restore the original mark oop.
*/

class MarkWordContext {
 private:
  oop _obj;
  markWord _mark_word;
  void swap(MarkWordContext& rhs);
 public:
  MarkWordContext();
  MarkWordContext(const oop obj);
  MarkWordContext(const MarkWordContext& rhs);
  void operator=(MarkWordContext rhs);
  ~MarkWordContext();
};

typedef SaveRestore<oop, ContextStore<oop, MarkWordContext> > SaveRestoreMarkWords;

class ClassLoaderData;

class CLDClaimContext {
 private:
  ClassLoaderData* _cld;
  void swap(CLDClaimContext& rhs);
 public:
  CLDClaimContext();
  CLDClaimContext(ClassLoaderData* cld);
  CLDClaimContext(const CLDClaimContext& rhs);
  void operator=(CLDClaimContext rhs);
  ~CLDClaimContext();
};

typedef SaveRestore<ClassLoaderData*, ContextStore<ClassLoaderData*, CLDClaimContext> > SaveRestoreCLDClaimState;

class CLDClaimStateClosure : public CLDClosure {
 private:
  SaveRestoreCLDClaimState _state;
 public:
  CLDClaimStateClosure();
  void do_cld(ClassLoaderData* cld);
};

class SaveRestoreCLDClaimBits : public StackObj {
 private:
  CLDClaimStateClosure _claim_state_closure;
 public:
  SaveRestoreCLDClaimBits();
  ~SaveRestoreCLDClaimBits();
};

#endif // SHARE_JFR_LEAKPROFILER_UTILITIES_SAVERESTORE_HPP
