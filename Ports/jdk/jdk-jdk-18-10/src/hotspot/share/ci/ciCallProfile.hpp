/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CICALLPROFILE_HPP
#define SHARE_CI_CICALLPROFILE_HPP

#include "ci/ciClassList.hpp"
#include "memory/allocation.hpp"

// ciCallProfile
//
// This class is used to determine the frequently called method
// at some call site
class ciCallProfile : StackObj {
private:
  // Fields are initialized directly by ciMethod::call_profile_at_bci.
  friend class ciMethod;
  friend class ciMethodHandle;

  enum { MorphismLimit = 2 }; // Max call site's morphism we care about
  int  _limit;                // number of receivers have been determined
  int  _morphism;             // determined call site's morphism
  int  _count;                // # times has this call been executed
  int  _receiver_count[MorphismLimit + 1]; // # times receivers have been seen
  ciKlass*  _receiver[MorphismLimit + 1];  // receivers (exact)

  ciCallProfile() {
    _limit = 0;
    _morphism    = 0;
    _count = -1;
    _receiver_count[0] = -1;
    _receiver[0] = NULL;
  }

  void add_receiver(ciKlass* receiver, int receiver_count);

public:
  // Note:  The following predicates return false for invalid profiles:
  bool      has_receiver(int i) const { return _limit > i; }
  int       morphism() const          { return _morphism; }

  int       count() const             { return _count; }
  int       receiver_count(int i)  {
    assert(i < _limit, "out of Call Profile MorphismLimit");
    return _receiver_count[i];
  }
  float     receiver_prob(int i)  {
    assert(i < _limit, "out of Call Profile MorphismLimit");
    return (float)_receiver_count[i]/(float)_count;
  }
  ciKlass*  receiver(int i)        {
    assert(i < _limit, "out of Call Profile MorphismLimit");
    return _receiver[i];
  }
};

#endif // SHARE_CI_CICALLPROFILE_HPP
