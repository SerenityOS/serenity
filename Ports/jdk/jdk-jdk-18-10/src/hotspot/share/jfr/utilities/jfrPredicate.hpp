/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_UTILITIES_JFRPREDICATE_HPP
#define SHARE_JFR_UTILITIES_JFRPREDICATE_HPP

#include "memory/allocation.hpp"
#include "utilities/growableArray.hpp"

/*
 * Premise is that the set is sorted.
 */
template <typename T, int cmp(const T&, const T&)>
class JfrPredicate : AllStatic {
 public:
  static bool test(GrowableArray<T>* set, T value) {
    assert(set != NULL, "invariant");
    bool found = false;
    set->template find_sorted<T, cmp>(value, found);
    return found;
  }
};

/*
 * Premise is that the set is sorted.
 */
template <typename T, int cmp(const T&, const T&)>
class JfrMutablePredicate : AllStatic {
 public:
  static bool test(GrowableArray<T>* set, T value) {
    assert(set != NULL, "invariant");
    bool found = false;
    const int location = set->template find_sorted<T, cmp>(value, found);
    if (!found) {
      set->insert_before(location, value);
    }
    return found;
  }
};

#endif // SHARE_JFR_UTILITIES_JFRPREDICATE_HPP
