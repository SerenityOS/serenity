/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_Z_ZVALUE_HPP
#define SHARE_GC_Z_ZVALUE_HPP

#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

//
// Storage
//

template <typename S>
class ZValueStorage : public AllStatic {
private:
  static uintptr_t _top;
  static uintptr_t _end;

public:
  static const size_t offset = 4 * K;

  static uintptr_t alloc(size_t size);
};

class ZContendedStorage : public ZValueStorage<ZContendedStorage> {
public:
  static size_t alignment();
  static uint32_t count();
  static uint32_t id();
};

class ZPerCPUStorage : public ZValueStorage<ZPerCPUStorage> {
public:
  static size_t alignment();
  static uint32_t count();
  static uint32_t id();
};

class ZPerNUMAStorage : public ZValueStorage<ZPerNUMAStorage> {
public:
  static size_t alignment();
  static uint32_t count();
  static uint32_t id();
};

class ZPerWorkerStorage : public ZValueStorage<ZPerWorkerStorage> {
public:
  static size_t alignment();
  static uint32_t count();
  static uint32_t id();
};

//
// Value
//

template <typename S, typename T>
class ZValue : public CHeapObj<mtGC> {
private:
  const uintptr_t _addr;

  uintptr_t value_addr(uint32_t value_id) const;

public:
  ZValue();
  ZValue(const T& value);

  const T* addr(uint32_t value_id = S::id()) const;
  T* addr(uint32_t value_id = S::id());

  const T& get(uint32_t value_id = S::id()) const;
  T& get(uint32_t value_id = S::id());

  void set(const T& value, uint32_t value_id = S::id());
  void set_all(const T& value);
};

template <typename T> using ZContended = ZValue<ZContendedStorage, T>;
template <typename T> using ZPerCPU = ZValue<ZPerCPUStorage, T>;
template <typename T> using ZPerNUMA = ZValue<ZPerNUMAStorage, T>;
template <typename T> using ZPerWorker = ZValue<ZPerWorkerStorage, T>;

//
// Iterator
//

template <typename S, typename T>
class ZValueIterator {
private:
  ZValue<S, T>* const _value;
  uint32_t            _value_id;

public:
  ZValueIterator(ZValue<S, T>* value);

  bool next(T** value);
};

template <typename T> using ZPerCPUIterator = ZValueIterator<ZPerCPUStorage, T>;
template <typename T> using ZPerNUMAIterator = ZValueIterator<ZPerNUMAStorage, T>;
template <typename T> using ZPerWorkerIterator = ZValueIterator<ZPerWorkerStorage, T>;

template <typename S, typename T>
class ZValueConstIterator {
private:
  const ZValue<S, T>* const _value;
  uint32_t                  _value_id;

public:
  ZValueConstIterator(const ZValue<S, T>* value);

  bool next(const T** value);
};

template <typename T> using ZPerCPUConstIterator = ZValueConstIterator<ZPerCPUStorage, T>;
template <typename T> using ZPerNUMAConstIterator = ZValueConstIterator<ZPerNUMAStorage, T>;
template <typename T> using ZPerWorkerConstIterator = ZValueConstIterator<ZPerWorkerStorage, T>;

#endif // SHARE_GC_Z_ZVALUE_HPP
