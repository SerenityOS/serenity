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
 *
 */

#ifndef SHARE_OOPS_INSTANCECLASSLOADERKLASS_INLINE_HPP
#define SHARE_OOPS_INSTANCECLASSLOADERKLASS_INLINE_HPP

#include "oops/instanceClassLoaderKlass.hpp"

#include "classfile/javaClasses.hpp"
#include "memory/iterator.hpp"
#include "oops/instanceKlass.inline.hpp"
#include "oops/oop.inline.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

template <typename T, class OopClosureType>
inline void InstanceClassLoaderKlass::oop_oop_iterate(oop obj, OopClosureType* closure) {
  InstanceKlass::oop_oop_iterate<T>(obj, closure);

  if (Devirtualizer::do_metadata(closure)) {
    ClassLoaderData* cld = java_lang_ClassLoader::loader_data(obj);
    // cld can be null if we have a non-registered class loader.
    if (cld != NULL) {
      Devirtualizer::do_cld(closure, cld);
    }
  }
}

template <typename T, class OopClosureType>
inline void InstanceClassLoaderKlass::oop_oop_iterate_reverse(oop obj, OopClosureType* closure) {
  InstanceKlass::oop_oop_iterate_reverse<T>(obj, closure);

  assert(!Devirtualizer::do_metadata(closure),
      "Code to handle metadata is not implemented");
}

template <typename T, class OopClosureType>
inline void InstanceClassLoaderKlass::oop_oop_iterate_bounded(oop obj, OopClosureType* closure, MemRegion mr) {
  InstanceKlass::oop_oop_iterate_bounded<T>(obj, closure, mr);

  if (Devirtualizer::do_metadata(closure)) {
    if (mr.contains(obj)) {
      ClassLoaderData* cld = java_lang_ClassLoader::loader_data(obj);
      // cld can be null if we have a non-registered class loader.
      if (cld != NULL) {
        Devirtualizer::do_cld(closure, cld);
      }
    }
  }
}

#endif // SHARE_OOPS_INSTANCECLASSLOADERKLASS_INLINE_HPP
