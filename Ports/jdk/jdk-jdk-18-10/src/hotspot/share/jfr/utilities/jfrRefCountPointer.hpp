/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_UTILITIES_JFRREFCOUNTPOINTER_HPP
#define SHARE_JFR_UTILITIES_JFRREFCOUNTPOINTER_HPP

#include "jfr/utilities/jfrAllocation.hpp"
#include "runtime/atomic.hpp"

template <typename T>
class RefCountHandle {
 private:
  const T* _ptr;

  RefCountHandle(const T* ptr) : _ptr(ptr) {
    assert(_ptr != NULL, "invariant");
    _ptr->add_ref();
  }

 public:
  RefCountHandle() : _ptr(NULL) {}

  RefCountHandle(const RefCountHandle<T>& rhs) : _ptr(rhs._ptr) {
    if (_ptr != NULL) {
      _ptr->add_ref();
    }
  }

  ~RefCountHandle() {
    if (_ptr != NULL) {
      _ptr->remove_ref();
      _ptr = NULL;
    }
  }

  // The copy-and-swap idiom upholds reference counting semantics
  void operator=(RefCountHandle<T> rhs) {
    const T* temp = rhs._ptr;
    rhs._ptr = _ptr;
    _ptr = temp;
  }

  bool operator==(const RefCountHandle<T>& rhs) const {
    return _ptr == rhs._ptr;
  }

  bool operator!=(const RefCountHandle<T>& rhs) const {
    return !operator==(rhs);
  }

  bool valid() const {
    return _ptr != NULL;
  }

  const T& operator->() const {
    return *_ptr;
  }

  T& operator->() {
    return *const_cast<T*>(_ptr);
  }

  static RefCountHandle<T> make(const T* ptr) {
    return ptr;
  }
};

class SingleThreadedRefCounter {
 private:
  mutable intptr_t _refs;
 public:
  SingleThreadedRefCounter() : _refs(0) {}

  void inc() const {
    ++_refs;
  }

  bool dec() const {
    return --_refs == 0;
  }

  intptr_t current() const {
    return _refs;
  }
};

class MultiThreadedRefCounter {
 private:
  mutable volatile intptr_t _refs;
 public:
  MultiThreadedRefCounter() : _refs(0) {}

  void inc() const {
    Atomic::add(&_refs, 1);
  }

  bool dec() const {
    return 0 == Atomic::add(&_refs, (-1));
  }

  intptr_t current() const {
   return _refs;
  }
};

template <typename T, typename RefCountImpl = MultiThreadedRefCounter>
class RefCountPointer : public JfrCHeapObj {
  template <typename>
  friend class RefCountHandle;
  typedef RefCountHandle<RefCountPointer<T, RefCountImpl> > RefHandle;
 private:
  const T* _ptr;
  mutable RefCountImpl _refs;

  // disallow multiple copies
  RefCountPointer(const RefCountPointer<T, RefCountImpl>& rhs);
  void operator=(const RefCountPointer<T, RefCountImpl>& rhs);

  ~RefCountPointer() {
    assert(_refs.current() == 0, "invariant");
    delete const_cast<T*>(_ptr);
  }

  void add_ref() const {
    _refs.inc();
  }

  void remove_ref() const {
    if (_refs.dec()) {
      delete this;
    }
  }

  RefCountPointer(const T* ptr) : _ptr(ptr), _refs() {
    assert(_ptr != NULL, "invariant");
  }

 public:
  const T* operator->() const {
    return _ptr;
  }

  T* operator->() {
    return const_cast<T*>(_ptr);
  }

  static RefHandle make(const T* ptr) {
    return RefHandle::make(new RefCountPointer<T, RefCountImpl>(ptr));
  }
};

#endif // SHARE_JFR_UTILITIES_JFRREFCOUNTPOINTER_HPP
