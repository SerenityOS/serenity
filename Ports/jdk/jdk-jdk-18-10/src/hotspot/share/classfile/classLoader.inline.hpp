/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_CLASSLOADER_INLINE_HPP
#define SHARE_CLASSFILE_CLASSLOADER_INLINE_HPP

#include "classfile/classLoader.hpp"

#include "runtime/atomic.hpp"
#include "runtime/arguments.hpp"

// Next entry in class path
inline ClassPathEntry* ClassPathEntry::next() const { return Atomic::load_acquire(&_next); }

inline void ClassPathEntry::set_next(ClassPathEntry* next) {
  // may have unlocked readers, so ensure visibility.
  Atomic::release_store(&_next, next);
}

inline ClassPathEntry* ClassLoader::classpath_entry(int n) {
  assert(n >= 0, "sanity");
  if (n == 0) {
    assert(has_jrt_entry(), "No class path entry at 0 for exploded module builds");
    return ClassLoader::_jrt_entry;
  } else {
    // The java runtime image is always the first entry
    // in the FileMapInfo::_classpath_entry_table. Even though
    // the _jrt_entry is not included in the _first_append_entry
    // linked list, it must be accounted for when comparing the
    // class path vs. the shared archive class path.
    ClassPathEntry* e = first_append_entry();
    while (--n >= 1) {
      assert(e != NULL, "Not that many classpath entries.");
      e = e->next();
    }
    return e;
  }
}

inline void ClassLoader::load_zip_library_if_needed() {
  if (Atomic::load_acquire(&_libzip_loaded) == 0) {
    release_load_zip_library();
  }
}

#if INCLUDE_CDS

// Helper function used by CDS code to get the number of boot classpath
// entries during shared classpath setup time.

inline int ClassLoader::num_boot_classpath_entries() {
  Arguments::assert_is_dumping_archive();
  assert(has_jrt_entry(), "must have a java runtime image");
  int num_entries = 1; // count the runtime image
  ClassPathEntry* e = first_append_entry();
  while (e != NULL) {
    num_entries ++;
    e = e->next();
  }
  return num_entries;
}

inline ClassPathEntry* ClassLoader::get_next_boot_classpath_entry(ClassPathEntry* e) {
  if (e == ClassLoader::_jrt_entry) {
    return first_append_entry();
  } else {
    return e->next();
  }
}

// Helper function used by CDS code to get the number of app classpath
// entries during shared classpath setup time.
inline int ClassLoader::num_app_classpath_entries() {
  Arguments::assert_is_dumping_archive();
  int num_entries = 0;
  ClassPathEntry* e= ClassLoader::_app_classpath_entries;
  while (e != NULL) {
    num_entries ++;
    e = e->next();
  }
  return num_entries;
}

#endif // INCLUDE_CDS

#endif // SHARE_CLASSFILE_CLASSLOADER_INLINE_HPP
