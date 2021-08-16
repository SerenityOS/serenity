/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 */

#ifndef SHARE_GC_Z_ZROOTSITERATOR_HPP
#define SHARE_GC_Z_ZROOTSITERATOR_HPP

#include "gc/shared/oopStorageSetParState.hpp"
#include "logging/log.hpp"
#include "memory/iterator.hpp"
#include "runtime/threadSMR.hpp"

template <typename Iterator>
class ZParallelApply {
private:
  Iterator      _iter;
  volatile bool _completed;

public:
  ZParallelApply() :
      _iter(),
      _completed(false) {}

  template <typename ClosureType>
  void apply(ClosureType* cl);

  Iterator& iter() {
    return _iter;
  }
};

class ZStrongOopStorageSetIterator {
  OopStorageSetStrongParState<true /* concurrent */, false /* is_const */> _iter;

public:
  ZStrongOopStorageSetIterator();

  void apply(OopClosure* cl);
};

class ZStrongCLDsIterator {
public:
  void apply(CLDClosure* cl);
};

class ZJavaThreadsIterator {
private:
  ThreadsListHandle _threads;
  volatile uint     _claimed;

  uint claim();

public:
  ZJavaThreadsIterator();

  void apply(ThreadClosure* cl);
};

class ZNMethodsIterator {
public:
  ZNMethodsIterator();
  ~ZNMethodsIterator();

  void apply(NMethodClosure* cl);
};

class ZRootsIterator {
private:
  ZParallelApply<ZStrongOopStorageSetIterator> _oop_storage_set;
  ZParallelApply<ZStrongCLDsIterator>          _class_loader_data_graph;
  ZParallelApply<ZJavaThreadsIterator>         _java_threads;
  ZParallelApply<ZNMethodsIterator>            _nmethods;

public:
  ZRootsIterator(int cld_claim);

  void apply(OopClosure* cl,
             CLDClosure* cld_cl,
             ThreadClosure* thread_cl,
             NMethodClosure* nm_cl);
};

class ZWeakOopStorageSetIterator {
private:
  OopStorageSetWeakParState<true /* concurrent */, false /* is_const */> _iter;

public:
  ZWeakOopStorageSetIterator();

  void apply(OopClosure* cl);

  void report_num_dead();
};

class ZWeakRootsIterator {
private:
  ZParallelApply<ZWeakOopStorageSetIterator> _oop_storage_set;

public:
  void apply(OopClosure* cl);

  void report_num_dead();
};

#endif // SHARE_GC_Z_ZROOTSITERATOR_HPP
