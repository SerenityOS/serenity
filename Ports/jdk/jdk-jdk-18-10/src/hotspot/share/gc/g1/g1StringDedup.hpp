/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1STRINGDEDUP_HPP
#define SHARE_GC_G1_G1STRINGDEDUP_HPP

//
// G1 string deduplication candidate selection
//
// An object is considered a deduplication candidate if all of the following
// statements are true:
//
// - The object is an instance of java.lang.String
//
// - The object is being evacuated from a young heap region
//
// - The object is being evacuated to a young/survivor heap region and the
//   object's age is equal to the deduplication age threshold
//
//   or
//
//   The object is being evacuated to an old heap region and the object's age is
//   less than the deduplication age threshold
//
// Once an string object has been promoted to an old region, or its age is higher
// than the deduplication age threshold, is will never become a candidate again.
// This approach avoids making the same object a candidate more than once.
//

#include "gc/g1/g1HeapRegionAttr.hpp"
#include "gc/shared/stringdedup/stringDedup.hpp"
#include "memory/allStatic.hpp"
#include "oops/oopsHierarchy.hpp"

class G1StringDedup : AllStatic {
public:
  // Candidate selection policy for full GC, returning true if the given
  // String is a candidate for string deduplication.
  // precondition: StringDedup::is_enabled()
  // precondition: java_string is a Java String
  static bool is_candidate_from_mark(oop java_string);

  // Candidate selection policy for young/mixed GC.
  // If to is young then age should be the new (survivor's) age.
  // if to is old then age should be the age of the copied from object.
  static bool is_candidate_from_evacuation(const Klass* klass,
                                           G1HeapRegionAttr from,
                                           G1HeapRegionAttr to,
                                           uint age) {
    return StringDedup::is_enabled_string(klass) &&
           from.is_young() &&
           (to.is_young() ?
            StringDedup::is_threshold_age(age) :
            StringDedup::is_below_threshold_age(age));
  }
};

#endif // SHARE_GC_G1_G1STRINGDEDUP_HPP
