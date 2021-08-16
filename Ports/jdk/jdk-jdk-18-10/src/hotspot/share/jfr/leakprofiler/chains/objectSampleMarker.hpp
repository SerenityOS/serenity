/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_LEAKPROFILER_CHAINS_OBJECTSAMPLEMARKER_HPP
#define SHARE_JFR_LEAKPROFILER_CHAINS_OBJECTSAMPLEMARKER_HPP

#include "memory/allocation.hpp"
#include "oops/markWord.hpp"
#include "utilities/growableArray.hpp"
//
// This class will save the original mark oop of a object sample object.
// It will then install an "identifier" mark oop to be used for
// identification purposes in the search for reference chains.
// The destructor will restore each modified oop with its original mark oop.
//
class ObjectSampleMarker : public StackObj {
 private:
  class ObjectSampleMarkWord : public ResourceObj {
    friend class ObjectSampleMarker;
   private:
    oop _obj;
    markWord _mark_word;
    ObjectSampleMarkWord(const oop obj,
                         const markWord mark_word) : _obj(obj),
                                                     _mark_word(mark_word) {}
   public:
    ObjectSampleMarkWord() : _obj(NULL), _mark_word(markWord::zero()) {}
  };

  GrowableArray<ObjectSampleMarkWord>* _store;

 public:
  ObjectSampleMarker() :
       _store(new GrowableArray<ObjectSampleMarkWord>(16)) {}
  ~ObjectSampleMarker() {
    assert(_store != NULL, "invariant");
    // restore the saved, original, markWord for sample objects
    while (_store->is_nonempty()) {
      ObjectSampleMarkWord sample_oop = _store->pop();
      sample_oop._obj->set_mark(sample_oop._mark_word);
      assert(sample_oop._obj->mark() == sample_oop._mark_word, "invariant");
    }
  }

  void mark(oop obj) {
    assert(obj != NULL, "invariant");
    // save the original markWord
    _store->push(ObjectSampleMarkWord(obj, obj->mark()));
    // now we will set the mark word to "marked" in order to quickly
    // identify sample objects during the reachability search from gc roots.
    assert(!obj->mark().is_marked(), "should only mark an object once");
    obj->set_mark(markWord::prototype().set_marked());
    assert(obj->mark().is_marked(), "invariant");
  }
};

#endif // SHARE_JFR_LEAKPROFILER_CHAINS_OBJECTSAMPLEMARKER_HPP
