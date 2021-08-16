/*
 * Copyright (c) 2019, 2021, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHSTRINGDEDUP_INLINE_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHSTRINGDEDUP_INLINE_HPP

#include "gc/shenandoah/shenandoahStringDedup.hpp"

#include "classfile/javaClasses.inline.hpp"

bool ShenandoahStringDedup::is_string_candidate(oop obj) {
  assert(Thread::current()->is_Worker_thread(),
        "Only from a GC worker thread");
  return java_lang_String::is_instance_inlined(obj) &&
         java_lang_String::value(obj) != nullptr;
}

bool ShenandoahStringDedup::is_candidate(oop obj) {
  if (!is_string_candidate(obj)) {
    return false;
  }

  if (StringDedup::is_below_threshold_age(obj->age())) {
    const markWord mark = obj->mark();
    // Having/had displaced header, too risk to deal with them, skip
    if (mark == markWord::INFLATING() || mark.has_displaced_mark_helper()) {
      return false;
    }

    // Increase string age and enqueue it when it rearches age threshold
    markWord new_mark = mark.incr_age();
    if (mark == obj->cas_set_mark(new_mark, mark)) {
      return StringDedup::is_threshold_age(new_mark.age());
    }
  }
  return false;
}

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHSTRINGDEDUP_INLINE_HPP
