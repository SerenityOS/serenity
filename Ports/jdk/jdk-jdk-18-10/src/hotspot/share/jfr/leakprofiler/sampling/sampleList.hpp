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

#ifndef SHARE_JFR_LEAKPROFILER_SAMPLING_SAMPLELIST_HPP
#define SHARE_JFR_LEAKPROFILER_SAMPLING_SAMPLELIST_HPP
#include "jfr/utilities/jfrAllocation.hpp"
#include "jfr/utilities/jfrDoublyLinkedList.hpp"

class ObjectSample;

class SampleList : public JfrCHeapObj {
  typedef JfrDoublyLinkedList<ObjectSample> List;
 private:
  List _free_list;
  List _in_use_list;
  const ObjectSample* _last_resolved;
  mutable size_t _allocated;
  const size_t _limit;
  const size_t _cache_size;

  void populate_cache();
  ObjectSample* newSample() const;
  void link(ObjectSample* sample);
  void unlink(ObjectSample* sample);
  void deallocate_samples(List& list);
  void reset(ObjectSample* sample);

 public:
  SampleList(size_t limit, size_t cache_size = 0);
  ~SampleList();

  ObjectSample* get();
  ObjectSample* first() const;
  ObjectSample* last() const;
  const ObjectSample* last_resolved() const;
  void set_last_resolved(const ObjectSample* sample);
  void release(ObjectSample* sample);
  ObjectSample* reuse(ObjectSample* sample);
  bool is_full() const;
  size_t count() const;
};

#endif // SHARE_JFR_LEAKPROFILER_SAMPLING_SAMPLELIST_HPP
