/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "oops/oop.inline.hpp"
#include "runtime/basicLock.hpp"
#include "runtime/synchronizer.hpp"

void BasicLock::print_on(outputStream* st, oop owner) const {
  st->print("monitor");
  markWord mark_word = displaced_header();
  if (mark_word.value() != 0) {
    // Print monitor info if there's an owning oop and it refers to this BasicLock.
    bool print_monitor_info = (owner != NULL) && (owner->mark() == markWord::from_pointer((void*)this));
    mark_word.print_on(st, print_monitor_info);
  }
}

void BasicLock::move_to(oop obj, BasicLock* dest) {
  // Check to see if we need to inflate the lock. This is only needed
  // if an object is locked using "this" lightweight monitor. In that
  // case, the displaced_header() is unlocked/is_neutral, because the
  // displaced_header() contains the header for the originally unlocked
  // object. However the lock could have already been inflated. But it
  // does not matter, this inflation will just a no-op. For other cases,
  // the displaced header will be either 0x0 or 0x3, which are location
  // independent, therefore the BasicLock is free to move.
  //
  // During OSR we may need to relocate a BasicLock (which contains a
  // displaced word) from a location in an interpreter frame to a
  // new location in a compiled frame.  "this" refers to the source
  // BasicLock in the interpreter frame.  "dest" refers to the destination
  // BasicLock in the new compiled frame.  We *always* inflate in move_to()
  // when the object is locked using "this" lightweight monitor.
  //
  // The always-Inflate policy works properly, but it depends on the
  // inflated fast-path operations in fast_lock and fast_unlock to avoid
  // performance problems. See x86/macroAssembler_x86.cpp: fast_lock()
  // and fast_unlock() for examples.
  //
  // Note that there is a way to safely swing the object's markword from
  // one stack location to another.  This avoids inflation.  Obviously,
  // we need to ensure that both locations refer to the current thread's stack.
  // There are some subtle concurrency issues, however, and since the benefit is
  // is small (given the support for inflated fast-path locking in the fast_lock, etc)
  // we'll leave that optimization for another time.

  if (displaced_header().is_neutral()) {
    // The object is locked and the resulting ObjectMonitor* will also be
    // locked so it can't be async deflated until ownership is dropped.
    ObjectSynchronizer::inflate_helper(obj);
    // WARNING: We cannot put a check here, because the inflation
    // will not update the displaced header. Once BasicLock is inflated,
    // no one should ever look at its content.
  } else {
    // Typically the displaced header will be 0 (recursive stack lock) or
    // unused_mark.  Naively we'd like to assert that the displaced mark
    // value is either 0, neutral, or 3.  But with the advent of the
    // store-before-CAS avoidance in fast_lock/compiler_lock_object
    // we can find any flavor mark in the displaced mark.
  }
  dest->set_displaced_header(displaced_header());
}
