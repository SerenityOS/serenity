/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_OOPSTORAGESETPARSTATE_HPP
#define SHARE_GC_SHARED_OOPSTORAGESETPARSTATE_HPP

#include "gc/shared/oopStorageParState.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "utilities/enumIterator.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/valueObjArray.hpp"

// Base class for OopStorageSet{Strong,Weak}ParState.
template<typename StorageId, bool concurrent, bool is_const>
class OopStorageSetParState {
public:
  using ParState = OopStorage::ParState<concurrent, is_const>;

  ParState* par_state(StorageId id) const {
    return _par_states.at(checked_cast<int>(EnumRange<StorageId>().index(id)));
  }

protected:
  OopStorageSetParState() : _par_states(OopStorageSet::Range<StorageId>().begin()) {}
  ~OopStorageSetParState() = default;

private:
  ValueObjArray<ParState, EnumRange<StorageId>().size()> _par_states;

  NONCOPYABLE(OopStorageSetParState);
};

// Set of strong parallel states.
template<bool concurrent, bool is_const>
class OopStorageSetStrongParState
  : public OopStorageSetParState<OopStorageSet::StrongId, concurrent, is_const>
{
public:
  template<typename Closure>
  void oops_do(Closure* cl);
};

// Set of weak parallel states.
template<bool concurrent, bool is_const>
class OopStorageSetWeakParState
  : public OopStorageSetParState<OopStorageSet::WeakId, concurrent, is_const>
{
public:
  template<typename Closure>
  void oops_do(Closure* cl);

  void report_num_dead();
};

#endif // SHARE_GC_SHARED_OOPSTORAGESETPARSTATE_HPP
