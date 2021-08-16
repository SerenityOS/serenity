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
#include "jfr/leakprofiler/sampling/objectSample.hpp"
#include "jfr/leakprofiler/sampling/sampleList.hpp"
#include "oops/oop.inline.hpp"

SampleList::SampleList(size_t limit, size_t cache_size) :
  _free_list(),
  _in_use_list(),
  _last_resolved(NULL),
  _allocated(0),
  _limit(limit),
  _cache_size(cache_size) {
}

SampleList::~SampleList() {
  deallocate_samples(_free_list);
  deallocate_samples(_in_use_list);
}

ObjectSample* SampleList::last() const {
  return _in_use_list.head();
}

ObjectSample* SampleList::first() const {
  return _in_use_list.tail();
}

const ObjectSample* SampleList::last_resolved() const {
  return _last_resolved;
}

void SampleList::set_last_resolved(const ObjectSample* sample) {
  assert(last() == sample, "invariant");
  _last_resolved = sample;
}

void SampleList::link(ObjectSample* sample) {
  assert(sample != NULL, "invariant");
  _in_use_list.prepend(sample);
}

void SampleList::unlink(ObjectSample* sample) {
  assert(sample != NULL, "invariant");
  if (_last_resolved == sample) {
    _last_resolved = sample->next();
  }
  reset(_in_use_list.remove(sample));
}

ObjectSample* SampleList::reuse(ObjectSample* sample) {
  assert(sample != NULL, "invariant");
  unlink(sample);
  link(sample);
  return sample;
}

void SampleList::populate_cache() {
  if (_free_list.count() < _cache_size) {
    const size_t cache_delta = _cache_size - _free_list.count();
    for (size_t i = 0; i < cache_delta; ++i) {
      ObjectSample* sample = newSample();
      if (sample != NULL) {
        _free_list.append(sample);
      }
    }
  }
}

ObjectSample* SampleList::newSample() const {
  if (_limit == _allocated) {
    return NULL;
  }
  ++_allocated;
  return new ObjectSample();
}

ObjectSample* SampleList::get() {
  ObjectSample* sample = _free_list.head();
  if (sample != NULL) {
    link(_free_list.remove(sample));
  } else {
    sample = newSample();
    if (sample != NULL) {
      _in_use_list.prepend(sample);
    }
  }
  if (_cache_size > 0 && sample != NULL) {
    populate_cache();
  }
  return sample;
}

void SampleList::release(ObjectSample* sample) {
  assert(sample != NULL, "invariant");
  unlink(sample);
  _free_list.append(sample);
}

void SampleList::deallocate_samples(List& list) {
  if (list.count() > 0) {
    ObjectSample* sample = list.head();
    while (sample != NULL) {
      list.remove(sample);
      delete sample;
      sample = list.head();
    }
  }
  assert(list.count() == 0, "invariant");
}

void SampleList::reset(ObjectSample* sample) {
  assert(sample != NULL, "invariant");
  sample->reset();
}

bool SampleList::is_full() const {
  return _in_use_list.count() == _limit;
}

size_t SampleList::count() const {
  return _in_use_list.count();
}
