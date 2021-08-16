/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "gc/shared/preservedMarks.inline.hpp"
#include "oops/oop.inline.hpp"
#include "unittest.hpp"

// Class to create a "fake" oop with a mark that will
// return true for calls to must_be_preserved().
class FakeOop {
  oopDesc _oop;

public:
  FakeOop() : _oop() { _oop.set_mark(originalMark()); }

  oop get_oop() { return &_oop; }
  markWord mark() { return _oop.mark(); }
  void set_mark(markWord m) { _oop.set_mark(m); }
  void forward_to(oop obj) {
    markWord m = markWord::encode_pointer_as_mark(obj);
    _oop.set_mark(m);
  }

  static markWord originalMark() { return markWord(markWord::lock_mask_in_place); }
  static markWord changedMark()  { return markWord(0x4711); }
};

#define ASSERT_MARK_WORD_EQ(a, b) ASSERT_EQ((a).value(), (b).value())

TEST_VM(PreservedMarks, iterate_and_restore) {
  PreservedMarks pm;
  FakeOop o1;
  FakeOop o2;
  FakeOop o3;
  FakeOop o4;

  // Make sure initial marks are correct.
  ASSERT_MARK_WORD_EQ(o1.mark(), FakeOop::originalMark());
  ASSERT_MARK_WORD_EQ(o2.mark(), FakeOop::originalMark());
  ASSERT_MARK_WORD_EQ(o3.mark(), FakeOop::originalMark());
  ASSERT_MARK_WORD_EQ(o4.mark(), FakeOop::originalMark());

  // Change the marks and verify change.
  o1.set_mark(FakeOop::changedMark());
  o2.set_mark(FakeOop::changedMark());
  ASSERT_MARK_WORD_EQ(o1.mark(), FakeOop::changedMark());
  ASSERT_MARK_WORD_EQ(o2.mark(), FakeOop::changedMark());

  // Push o1 and o2 to have their marks preserved.
  pm.push(o1.get_oop(), o1.mark());
  pm.push(o2.get_oop(), o2.mark());

  // Fake a move from o1->o3 and o2->o4.
  o1.forward_to(o3.get_oop());
  o2.forward_to(o4.get_oop());
  ASSERT_EQ(o1.get_oop()->forwardee(), o3.get_oop());
  ASSERT_EQ(o2.get_oop()->forwardee(), o4.get_oop());
  // Adjust will update the PreservedMarks stack to
  // make sure the mark is updated at the new location.
  pm.adjust_during_full_gc();

  // Restore all preserved and verify that the changed
  // mark is now present at o3 and o4.
  pm.restore();
  ASSERT_MARK_WORD_EQ(o3.mark(), FakeOop::changedMark());
  ASSERT_MARK_WORD_EQ(o4.mark(), FakeOop::changedMark());
}
