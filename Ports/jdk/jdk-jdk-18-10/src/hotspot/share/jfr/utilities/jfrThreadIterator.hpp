/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_VM_JFR_UTILITIES_JFRTHREADITERATOR_HPP
#define SHARE_VM_JFR_UTILITIES_JFRTHREADITERATOR_HPP

#include "memory/allocation.hpp"
#include "runtime/nonJavaThread.hpp"
#include "runtime/thread.hpp"
#include "runtime/threadSMR.hpp"

template <typename Adapter, typename AP = StackObj>
class JfrThreadIterator : public AP {
 private:
  Adapter _adapter;
 public:
  JfrThreadIterator(bool live_only = true) : _adapter(live_only) {}
  typename Adapter::Type* next() {
    assert(has_next(), "invariant");
    return _adapter.next();
  }
  bool has_next() const {
    return _adapter.has_next();
  }
};

class JfrJavaThreadIteratorAdapter {
 private:
  JavaThreadIteratorWithHandle _iter;
  JavaThread* _next;
  bool _live_only;
 public:
  typedef JavaThread Type;
  JfrJavaThreadIteratorAdapter(bool live_only = true);
  bool has_next() const {
    return _next != NULL;
  }
  Type* next();
};

class JfrNonJavaThreadIteratorAdapter {
 private:
  NonJavaThread::Iterator _iter;
  NonJavaThread* _next;
 public:
  typedef NonJavaThread Type;
  JfrNonJavaThreadIteratorAdapter(bool live_only = true);
  bool has_next() const;
  Type* next();
};

typedef JfrThreadIterator<JfrJavaThreadIteratorAdapter, StackObj> JfrJavaThreadIterator;
typedef JfrThreadIterator<JfrNonJavaThreadIteratorAdapter, StackObj> JfrNonJavaThreadIterator;

#endif // SHARE_VM_JFR_UTILITIES_JFRTHREADITERATOR_HPP
