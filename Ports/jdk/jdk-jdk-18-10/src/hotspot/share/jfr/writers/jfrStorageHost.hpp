/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_WRITERS_JFRSTORAGEHOST_HPP
#define SHARE_JFR_WRITERS_JFRSTORAGEHOST_HPP

#include "jfr/writers/jfrPosition.inline.hpp"

template <typename Adapter, typename AP> // Adapter and AllocationPolicy
class StorageHost : public Position<AP> {
 public:
  typedef typename Adapter::StorageType StorageType;
 private:
  Adapter _adapter;

 protected:
  void bind();
  void soft_reset();
  void hard_reset();
  void cancel();
  bool is_backed();
  bool accommodate(size_t used, size_t requested);
  void commit();
  void release();
  StorageHost(StorageType* storage, Thread* thread);
  StorageHost(StorageType* storage, size_t size);
  StorageHost(Thread* thread);

 public:
  StorageType* storage();
  bool is_valid() const;
  void set_storage(StorageType* storage);
  void flush();
  void seek(intptr_t offset);
};

#endif // SHARE_JFR_WRITERS_JFRSTORAGEHOST_HPP
