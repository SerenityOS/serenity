/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciArrayKlass.hpp"
#include "ci/ciEnv.hpp"
#include "ci/ciKlass.hpp"
#include "ci/ciMethod.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/vmClasses.hpp"
#include "code/dependencies.hpp"
#include "compiler/compileLog.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/compileTask.hpp"
#include "memory/resourceArea.hpp"
#include "oops/klass.hpp"
#include "oops/oop.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "runtime/flags/flagSetting.hpp"
#include "runtime/handles.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/perfData.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/vmThread.hpp"
#include "utilities/copy.hpp"


#ifdef ASSERT
static bool must_be_in_vm() {
  Thread* thread = Thread::current();
  if (thread->is_Java_thread()) {
    return JavaThread::cast(thread)->thread_state() == _thread_in_vm;
  } else {
    return true;  // Could be VMThread or GC thread
  }
}
#endif //ASSERT

void Dependencies::initialize(ciEnv* env) {
  Arena* arena = env->arena();
  _oop_recorder = env->oop_recorder();
  _log = env->log();
  _dep_seen = new(arena) GrowableArray<int>(arena, 500, 0, 0);
#if INCLUDE_JVMCI
  _using_dep_values = false;
#endif
  DEBUG_ONLY(_deps[end_marker] = NULL);
  for (int i = (int)FIRST_TYPE; i < (int)TYPE_LIMIT; i++) {
    _deps[i] = new(arena) GrowableArray<ciBaseObject*>(arena, 10, 0, 0);
  }
  _content_bytes = NULL;
  _size_in_bytes = (size_t)-1;

  assert(TYPE_LIMIT <= (1<<LG2_TYPE_LIMIT), "sanity");
}

void Dependencies::assert_evol_method(ciMethod* m) {
  assert_common_1(evol_method, m);
}

void Dependencies::assert_leaf_type(ciKlass* ctxk) {
  if (ctxk->is_array_klass()) {
    // As a special case, support this assertion on an array type,
    // which reduces to an assertion on its element type.
    // Note that this cannot be done with assertions that
    // relate to concreteness or abstractness.
    ciType* elemt = ctxk->as_array_klass()->base_element_type();
    if (!elemt->is_instance_klass())  return;   // Ex:  int[][]
    ctxk = elemt->as_instance_klass();
    //if (ctxk->is_final())  return;            // Ex:  String[][]
  }
  check_ctxk(ctxk);
  assert_common_1(leaf_type, ctxk);
}

void Dependencies::assert_abstract_with_unique_concrete_subtype(ciKlass* ctxk, ciKlass* conck) {
  check_ctxk_abstract(ctxk);
  assert_common_2(abstract_with_unique_concrete_subtype, ctxk, conck);
}

void Dependencies::assert_unique_concrete_method(ciKlass* ctxk, ciMethod* uniqm) {
  check_ctxk(ctxk);
  check_unique_method(ctxk, uniqm);
  assert_common_2(unique_concrete_method_2, ctxk, uniqm);
}

void Dependencies::assert_unique_concrete_method(ciKlass* ctxk, ciMethod* uniqm, ciKlass* resolved_klass, ciMethod* resolved_method) {
  check_ctxk(ctxk);
  check_unique_method(ctxk, uniqm);
  if (UseVtableBasedCHA) {
    assert_common_4(unique_concrete_method_4, ctxk, uniqm, resolved_klass, resolved_method);
  } else {
    assert_common_2(unique_concrete_method_2, ctxk, uniqm);
  }
}

void Dependencies::assert_has_no_finalizable_subclasses(ciKlass* ctxk) {
  check_ctxk(ctxk);
  assert_common_1(no_finalizable_subclasses, ctxk);
}

void Dependencies::assert_call_site_target_value(ciCallSite* call_site, ciMethodHandle* method_handle) {
  assert_common_2(call_site_target_value, call_site, method_handle);
}

#if INCLUDE_JVMCI

Dependencies::Dependencies(Arena* arena, OopRecorder* oop_recorder, CompileLog* log) {
  _oop_recorder = oop_recorder;
  _log = log;
  _dep_seen = new(arena) GrowableArray<int>(arena, 500, 0, 0);
  _using_dep_values = true;
  DEBUG_ONLY(_dep_values[end_marker] = NULL);
  for (int i = (int)FIRST_TYPE; i < (int)TYPE_LIMIT; i++) {
    _dep_values[i] = new(arena) GrowableArray<DepValue>(arena, 10, 0, DepValue());
  }
  _content_bytes = NULL;
  _size_in_bytes = (size_t)-1;

  assert(TYPE_LIMIT <= (1<<LG2_TYPE_LIMIT), "sanity");
}

void Dependencies::assert_evol_method(Method* m) {
  assert_common_1(evol_method, DepValue(_oop_recorder, m));
}

void Dependencies::assert_has_no_finalizable_subclasses(Klass* ctxk) {
  check_ctxk(ctxk);
  assert_common_1(no_finalizable_subclasses, DepValue(_oop_recorder, ctxk));
}

void Dependencies::assert_leaf_type(Klass* ctxk) {
  if (ctxk->is_array_klass()) {
    // As a special case, support this assertion on an array type,
    // which reduces to an assertion on its element type.
    // Note that this cannot be done with assertions that
    // relate to concreteness or abstractness.
    BasicType elemt = ArrayKlass::cast(ctxk)->element_type();
    if (is_java_primitive(elemt))  return;   // Ex:  int[][]
    ctxk = ObjArrayKlass::cast(ctxk)->bottom_klass();
    //if (ctxk->is_final())  return;            // Ex:  String[][]
  }
  check_ctxk(ctxk);
  assert_common_1(leaf_type, DepValue(_oop_recorder, ctxk));
}

void Dependencies::assert_abstract_with_unique_concrete_subtype(Klass* ctxk, Klass* conck) {
  check_ctxk_abstract(ctxk);
  DepValue ctxk_dv(_oop_recorder, ctxk);
  DepValue conck_dv(_oop_recorder, conck, &ctxk_dv);
  assert_common_2(abstract_with_unique_concrete_subtype, ctxk_dv, conck_dv);
}

void Dependencies::assert_unique_concrete_method(Klass* ctxk, Method* uniqm) {
  check_ctxk(ctxk);
  check_unique_method(ctxk, uniqm);
  assert_common_2(unique_concrete_method_2, DepValue(_oop_recorder, ctxk), DepValue(_oop_recorder, uniqm));
}

void Dependencies::assert_call_site_target_value(oop call_site, oop method_handle) {
  assert_common_2(call_site_target_value, DepValue(_oop_recorder, JNIHandles::make_local(call_site)), DepValue(_oop_recorder, JNIHandles::make_local(method_handle)));
}

#endif // INCLUDE_JVMCI


// Helper function.  If we are adding a new dep. under ctxk2,
// try to find an old dep. under a broader* ctxk1.  If there is
//
bool Dependencies::maybe_merge_ctxk(GrowableArray<ciBaseObject*>* deps,
                                    int ctxk_i, ciKlass* ctxk2) {
  ciKlass* ctxk1 = deps->at(ctxk_i)->as_metadata()->as_klass();
  if (ctxk2->is_subtype_of(ctxk1)) {
    return true;  // success, and no need to change
  } else if (ctxk1->is_subtype_of(ctxk2)) {
    // new context class fully subsumes previous one
    deps->at_put(ctxk_i, ctxk2);
    return true;
  } else {
    return false;
  }
}

void Dependencies::assert_common_1(DepType dept, ciBaseObject* x) {
  assert(dep_args(dept) == 1, "sanity");
  log_dependency(dept, x);
  GrowableArray<ciBaseObject*>* deps = _deps[dept];

  // see if the same (or a similar) dep is already recorded
  if (note_dep_seen(dept, x)) {
    assert(deps->find(x) >= 0, "sanity");
  } else {
    deps->append(x);
  }
}

void Dependencies::assert_common_2(DepType dept,
                                   ciBaseObject* x0, ciBaseObject* x1) {
  assert(dep_args(dept) == 2, "sanity");
  log_dependency(dept, x0, x1);
  GrowableArray<ciBaseObject*>* deps = _deps[dept];

  // see if the same (or a similar) dep is already recorded
  bool has_ctxk = has_explicit_context_arg(dept);
  if (has_ctxk) {
    assert(dep_context_arg(dept) == 0, "sanity");
    if (note_dep_seen(dept, x1)) {
      // look in this bucket for redundant assertions
      const int stride = 2;
      for (int i = deps->length(); (i -= stride) >= 0; ) {
        ciBaseObject* y1 = deps->at(i+1);
        if (x1 == y1) {  // same subject; check the context
          if (maybe_merge_ctxk(deps, i+0, x0->as_metadata()->as_klass())) {
            return;
          }
        }
      }
    }
  } else {
    if (note_dep_seen(dept, x0) && note_dep_seen(dept, x1)) {
      // look in this bucket for redundant assertions
      const int stride = 2;
      for (int i = deps->length(); (i -= stride) >= 0; ) {
        ciBaseObject* y0 = deps->at(i+0);
        ciBaseObject* y1 = deps->at(i+1);
        if (x0 == y0 && x1 == y1) {
          return;
        }
      }
    }
  }

  // append the assertion in the correct bucket:
  deps->append(x0);
  deps->append(x1);
}

void Dependencies::assert_common_4(DepType dept,
                                   ciKlass* ctxk, ciBaseObject* x1, ciBaseObject* x2, ciBaseObject* x3) {
  assert(has_explicit_context_arg(dept), "sanity");
  assert(dep_context_arg(dept) == 0, "sanity");
  assert(dep_args(dept) == 4, "sanity");
  log_dependency(dept, ctxk, x1, x2, x3);
  GrowableArray<ciBaseObject*>* deps = _deps[dept];

  // see if the same (or a similar) dep is already recorded
  if (note_dep_seen(dept, x1) && note_dep_seen(dept, x2) && note_dep_seen(dept, x3)) {
    // look in this bucket for redundant assertions
    const int stride = 4;
    for (int i = deps->length(); (i -= stride) >= 0; ) {
      ciBaseObject* y1 = deps->at(i+1);
      ciBaseObject* y2 = deps->at(i+2);
      ciBaseObject* y3 = deps->at(i+3);
      if (x1 == y1 && x2 == y2 && x3 == y3) {  // same subjects; check the context
        if (maybe_merge_ctxk(deps, i+0, ctxk)) {
          return;
        }
      }
    }
  }
  // append the assertion in the correct bucket:
  deps->append(ctxk);
  deps->append(x1);
  deps->append(x2);
  deps->append(x3);
}

#if INCLUDE_JVMCI
bool Dependencies::maybe_merge_ctxk(GrowableArray<DepValue>* deps,
                                    int ctxk_i, DepValue ctxk2_dv) {
  Klass* ctxk1 = deps->at(ctxk_i).as_klass(_oop_recorder);
  Klass* ctxk2 = ctxk2_dv.as_klass(_oop_recorder);
  if (ctxk2->is_subtype_of(ctxk1)) {
    return true;  // success, and no need to change
  } else if (ctxk1->is_subtype_of(ctxk2)) {
    // new context class fully subsumes previous one
    deps->at_put(ctxk_i, ctxk2_dv);
    return true;
  } else {
    return false;
  }
}

void Dependencies::assert_common_1(DepType dept, DepValue x) {
  assert(dep_args(dept) == 1, "sanity");
  //log_dependency(dept, x);
  GrowableArray<DepValue>* deps = _dep_values[dept];

  // see if the same (or a similar) dep is already recorded
  if (note_dep_seen(dept, x)) {
    assert(deps->find(x) >= 0, "sanity");
  } else {
    deps->append(x);
  }
}

void Dependencies::assert_common_2(DepType dept,
                                   DepValue x0, DepValue x1) {
  assert(dep_args(dept) == 2, "sanity");
  //log_dependency(dept, x0, x1);
  GrowableArray<DepValue>* deps = _dep_values[dept];

  // see if the same (or a similar) dep is already recorded
  bool has_ctxk = has_explicit_context_arg(dept);
  if (has_ctxk) {
    assert(dep_context_arg(dept) == 0, "sanity");
    if (note_dep_seen(dept, x1)) {
      // look in this bucket for redundant assertions
      const int stride = 2;
      for (int i = deps->length(); (i -= stride) >= 0; ) {
        DepValue y1 = deps->at(i+1);
        if (x1 == y1) {  // same subject; check the context
          if (maybe_merge_ctxk(deps, i+0, x0)) {
            return;
          }
        }
      }
    }
  } else {
    if (note_dep_seen(dept, x0) && note_dep_seen(dept, x1)) {
      // look in this bucket for redundant assertions
      const int stride = 2;
      for (int i = deps->length(); (i -= stride) >= 0; ) {
        DepValue y0 = deps->at(i+0);
        DepValue y1 = deps->at(i+1);
        if (x0 == y0 && x1 == y1) {
          return;
        }
      }
    }
  }

  // append the assertion in the correct bucket:
  deps->append(x0);
  deps->append(x1);
}
#endif // INCLUDE_JVMCI

/// Support for encoding dependencies into an nmethod:

void Dependencies::copy_to(nmethod* nm) {
  address beg = nm->dependencies_begin();
  address end = nm->dependencies_end();
  guarantee(end - beg >= (ptrdiff_t) size_in_bytes(), "bad sizing");
  Copy::disjoint_words((HeapWord*) content_bytes(),
                       (HeapWord*) beg,
                       size_in_bytes() / sizeof(HeapWord));
  assert(size_in_bytes() % sizeof(HeapWord) == 0, "copy by words");
}

static int sort_dep(ciBaseObject** p1, ciBaseObject** p2, int narg) {
  for (int i = 0; i < narg; i++) {
    int diff = p1[i]->ident() - p2[i]->ident();
    if (diff != 0)  return diff;
  }
  return 0;
}
static int sort_dep_arg_1(ciBaseObject** p1, ciBaseObject** p2)
{ return sort_dep(p1, p2, 1); }
static int sort_dep_arg_2(ciBaseObject** p1, ciBaseObject** p2)
{ return sort_dep(p1, p2, 2); }
static int sort_dep_arg_3(ciBaseObject** p1, ciBaseObject** p2)
{ return sort_dep(p1, p2, 3); }
static int sort_dep_arg_4(ciBaseObject** p1, ciBaseObject** p2)
{ return sort_dep(p1, p2, 4); }

#if INCLUDE_JVMCI
// metadata deps are sorted before object deps
static int sort_dep_value(Dependencies::DepValue* p1, Dependencies::DepValue* p2, int narg) {
  for (int i = 0; i < narg; i++) {
    int diff = p1[i].sort_key() - p2[i].sort_key();
    if (diff != 0)  return diff;
  }
  return 0;
}
static int sort_dep_value_arg_1(Dependencies::DepValue* p1, Dependencies::DepValue* p2)
{ return sort_dep_value(p1, p2, 1); }
static int sort_dep_value_arg_2(Dependencies::DepValue* p1, Dependencies::DepValue* p2)
{ return sort_dep_value(p1, p2, 2); }
static int sort_dep_value_arg_3(Dependencies::DepValue* p1, Dependencies::DepValue* p2)
{ return sort_dep_value(p1, p2, 3); }
#endif // INCLUDE_JVMCI

void Dependencies::sort_all_deps() {
#if INCLUDE_JVMCI
  if (_using_dep_values) {
    for (int deptv = (int)FIRST_TYPE; deptv < (int)TYPE_LIMIT; deptv++) {
      DepType dept = (DepType)deptv;
      GrowableArray<DepValue>* deps = _dep_values[dept];
      if (deps->length() <= 1)  continue;
      switch (dep_args(dept)) {
      case 1: deps->sort(sort_dep_value_arg_1, 1); break;
      case 2: deps->sort(sort_dep_value_arg_2, 2); break;
      case 3: deps->sort(sort_dep_value_arg_3, 3); break;
      default: ShouldNotReachHere(); break;
      }
    }
    return;
  }
#endif // INCLUDE_JVMCI
  for (int deptv = (int)FIRST_TYPE; deptv < (int)TYPE_LIMIT; deptv++) {
    DepType dept = (DepType)deptv;
    GrowableArray<ciBaseObject*>* deps = _deps[dept];
    if (deps->length() <= 1)  continue;
    switch (dep_args(dept)) {
    case 1: deps->sort(sort_dep_arg_1, 1); break;
    case 2: deps->sort(sort_dep_arg_2, 2); break;
    case 3: deps->sort(sort_dep_arg_3, 3); break;
    case 4: deps->sort(sort_dep_arg_4, 4); break;
    default: ShouldNotReachHere(); break;
    }
  }
}

size_t Dependencies::estimate_size_in_bytes() {
  size_t est_size = 100;
#if INCLUDE_JVMCI
  if (_using_dep_values) {
    for (int deptv = (int)FIRST_TYPE; deptv < (int)TYPE_LIMIT; deptv++) {
      DepType dept = (DepType)deptv;
      GrowableArray<DepValue>* deps = _dep_values[dept];
      est_size += deps->length() * 2;  // tags and argument(s)
    }
    return est_size;
  }
#endif // INCLUDE_JVMCI
  for (int deptv = (int)FIRST_TYPE; deptv < (int)TYPE_LIMIT; deptv++) {
    DepType dept = (DepType)deptv;
    GrowableArray<ciBaseObject*>* deps = _deps[dept];
    est_size += deps->length()*2;  // tags and argument(s)
  }
  return est_size;
}

ciKlass* Dependencies::ctxk_encoded_as_null(DepType dept, ciBaseObject* x) {
  switch (dept) {
  case unique_concrete_method_2:
  case unique_concrete_method_4:
    return x->as_metadata()->as_method()->holder();
  default:
    return NULL;  // let NULL be NULL
  }
}

Klass* Dependencies::ctxk_encoded_as_null(DepType dept, Metadata* x) {
  assert(must_be_in_vm(), "raw oops here");
  switch (dept) {
  case unique_concrete_method_2:
  case unique_concrete_method_4:
    assert(x->is_method(), "sanity");
    return ((Method*)x)->method_holder();
  default:
    return NULL;  // let NULL be NULL
  }
}

void Dependencies::encode_content_bytes() {
  sort_all_deps();

  // cast is safe, no deps can overflow INT_MAX
  CompressedWriteStream bytes((int)estimate_size_in_bytes());

#if INCLUDE_JVMCI
  if (_using_dep_values) {
    for (int deptv = (int)FIRST_TYPE; deptv < (int)TYPE_LIMIT; deptv++) {
      DepType dept = (DepType)deptv;
      GrowableArray<DepValue>* deps = _dep_values[dept];
      if (deps->length() == 0)  continue;
      int stride = dep_args(dept);
      int ctxkj  = dep_context_arg(dept);  // -1 if no context arg
      assert(stride > 0, "sanity");
      for (int i = 0; i < deps->length(); i += stride) {
        jbyte code_byte = (jbyte)dept;
        int skipj = -1;
        if (ctxkj >= 0 && ctxkj+1 < stride) {
          Klass*  ctxk = deps->at(i+ctxkj+0).as_klass(_oop_recorder);
          DepValue x = deps->at(i+ctxkj+1);  // following argument
          if (ctxk == ctxk_encoded_as_null(dept, x.as_metadata(_oop_recorder))) {
            skipj = ctxkj;  // we win:  maybe one less oop to keep track of
            code_byte |= default_context_type_bit;
          }
        }
        bytes.write_byte(code_byte);
        for (int j = 0; j < stride; j++) {
          if (j == skipj)  continue;
          DepValue v = deps->at(i+j);
          int idx = v.index();
          bytes.write_int(idx);
        }
      }
    }
  } else {
#endif // INCLUDE_JVMCI
  for (int deptv = (int)FIRST_TYPE; deptv < (int)TYPE_LIMIT; deptv++) {
    DepType dept = (DepType)deptv;
    GrowableArray<ciBaseObject*>* deps = _deps[dept];
    if (deps->length() == 0)  continue;
    int stride = dep_args(dept);
    int ctxkj  = dep_context_arg(dept);  // -1 if no context arg
    assert(stride > 0, "sanity");
    for (int i = 0; i < deps->length(); i += stride) {
      jbyte code_byte = (jbyte)dept;
      int skipj = -1;
      if (ctxkj >= 0 && ctxkj+1 < stride) {
        ciKlass*  ctxk = deps->at(i+ctxkj+0)->as_metadata()->as_klass();
        ciBaseObject* x     = deps->at(i+ctxkj+1);  // following argument
        if (ctxk == ctxk_encoded_as_null(dept, x)) {
          skipj = ctxkj;  // we win:  maybe one less oop to keep track of
          code_byte |= default_context_type_bit;
        }
      }
      bytes.write_byte(code_byte);
      for (int j = 0; j < stride; j++) {
        if (j == skipj)  continue;
        ciBaseObject* v = deps->at(i+j);
        int idx;
        if (v->is_object()) {
          idx = _oop_recorder->find_index(v->as_object()->constant_encoding());
        } else {
          ciMetadata* meta = v->as_metadata();
          idx = _oop_recorder->find_index(meta->constant_encoding());
        }
        bytes.write_int(idx);
      }
    }
  }
#if INCLUDE_JVMCI
  }
#endif

  // write a sentinel byte to mark the end
  bytes.write_byte(end_marker);

  // round it out to a word boundary
  while (bytes.position() % sizeof(HeapWord) != 0) {
    bytes.write_byte(end_marker);
  }

  // check whether the dept byte encoding really works
  assert((jbyte)default_context_type_bit != 0, "byte overflow");

  _content_bytes = bytes.buffer();
  _size_in_bytes = bytes.position();
}


const char* Dependencies::_dep_name[TYPE_LIMIT] = {
  "end_marker",
  "evol_method",
  "leaf_type",
  "abstract_with_unique_concrete_subtype",
  "unique_concrete_method_2",
  "unique_concrete_method_4",
  "no_finalizable_subclasses",
  "call_site_target_value"
};

int Dependencies::_dep_args[TYPE_LIMIT] = {
  -1,// end_marker
  1, // evol_method m
  1, // leaf_type ctxk
  2, // abstract_with_unique_concrete_subtype ctxk, k
  2, // unique_concrete_method_2 ctxk, m
  4, // unique_concrete_method_4 ctxk, m, resolved_klass, resolved_method
  1, // no_finalizable_subclasses ctxk
  2  // call_site_target_value call_site, method_handle
};

const char* Dependencies::dep_name(Dependencies::DepType dept) {
  if (!dept_in_mask(dept, all_types))  return "?bad-dep?";
  return _dep_name[dept];
}

int Dependencies::dep_args(Dependencies::DepType dept) {
  if (!dept_in_mask(dept, all_types))  return -1;
  return _dep_args[dept];
}

void Dependencies::check_valid_dependency_type(DepType dept) {
  guarantee(FIRST_TYPE <= dept && dept < TYPE_LIMIT, "invalid dependency type: %d", (int) dept);
}

Dependencies::DepType Dependencies::validate_dependencies(CompileTask* task, char** failure_detail) {
  int klass_violations = 0;
  DepType result = end_marker;
  for (Dependencies::DepStream deps(this); deps.next(); ) {
    Klass* witness = deps.check_dependency();
    if (witness != NULL) {
      if (klass_violations == 0) {
        result = deps.type();
        if (failure_detail != NULL && klass_violations == 0) {
          // Use a fixed size buffer to prevent the string stream from
          // resizing in the context of an inner resource mark.
          char* buffer = NEW_RESOURCE_ARRAY(char, O_BUFLEN);
          stringStream st(buffer, O_BUFLEN);
          deps.print_dependency(witness, true, &st);
          *failure_detail = st.as_string();
        }
      }
      klass_violations++;
      if (xtty == NULL) {
        // If we're not logging then a single violation is sufficient,
        // otherwise we want to log all the dependences which were
        // violated.
        break;
      }
    }
  }

  return result;
}

// for the sake of the compiler log, print out current dependencies:
void Dependencies::log_all_dependencies() {
  if (log() == NULL)  return;
  ResourceMark rm;
  for (int deptv = (int)FIRST_TYPE; deptv < (int)TYPE_LIMIT; deptv++) {
    DepType dept = (DepType)deptv;
    GrowableArray<ciBaseObject*>* deps = _deps[dept];
    int deplen = deps->length();
    if (deplen == 0) {
      continue;
    }
    int stride = dep_args(dept);
    GrowableArray<ciBaseObject*>* ciargs = new GrowableArray<ciBaseObject*>(stride);
    for (int i = 0; i < deps->length(); i += stride) {
      for (int j = 0; j < stride; j++) {
        // flush out the identities before printing
        ciargs->push(deps->at(i+j));
      }
      write_dependency_to(log(), dept, ciargs);
      ciargs->clear();
    }
    guarantee(deplen == deps->length(), "deps array cannot grow inside nested ResoureMark scope");
  }
}

void Dependencies::write_dependency_to(CompileLog* log,
                                       DepType dept,
                                       GrowableArray<DepArgument>* args,
                                       Klass* witness) {
  if (log == NULL) {
    return;
  }
  ResourceMark rm;
  ciEnv* env = ciEnv::current();
  GrowableArray<ciBaseObject*>* ciargs = new GrowableArray<ciBaseObject*>(args->length());
  for (GrowableArrayIterator<DepArgument> it = args->begin(); it != args->end(); ++it) {
    DepArgument arg = *it;
    if (arg.is_oop()) {
      ciargs->push(env->get_object(arg.oop_value()));
    } else {
      ciargs->push(env->get_metadata(arg.metadata_value()));
    }
  }
  int argslen = ciargs->length();
  Dependencies::write_dependency_to(log, dept, ciargs, witness);
  guarantee(argslen == ciargs->length(), "ciargs array cannot grow inside nested ResoureMark scope");
}

void Dependencies::write_dependency_to(CompileLog* log,
                                       DepType dept,
                                       GrowableArray<ciBaseObject*>* args,
                                       Klass* witness) {
  if (log == NULL) {
    return;
  }
  ResourceMark rm;
  GrowableArray<int>* argids = new GrowableArray<int>(args->length());
  for (GrowableArrayIterator<ciBaseObject*> it = args->begin(); it != args->end(); ++it) {
    ciBaseObject* obj = *it;
    if (obj->is_object()) {
      argids->push(log->identify(obj->as_object()));
    } else {
      argids->push(log->identify(obj->as_metadata()));
    }
  }
  if (witness != NULL) {
    log->begin_elem("dependency_failed");
  } else {
    log->begin_elem("dependency");
  }
  log->print(" type='%s'", dep_name(dept));
  const int ctxkj = dep_context_arg(dept);  // -1 if no context arg
  if (ctxkj >= 0 && ctxkj < argids->length()) {
    log->print(" ctxk='%d'", argids->at(ctxkj));
  }
  // write remaining arguments, if any.
  for (int j = 0; j < argids->length(); j++) {
    if (j == ctxkj)  continue;  // already logged
    if (j == 1) {
      log->print(  " x='%d'",    argids->at(j));
    } else {
      log->print(" x%d='%d'", j, argids->at(j));
    }
  }
  if (witness != NULL) {
    log->object("witness", witness);
    log->stamp();
  }
  log->end_elem();
}

void Dependencies::write_dependency_to(xmlStream* xtty,
                                       DepType dept,
                                       GrowableArray<DepArgument>* args,
                                       Klass* witness) {
  if (xtty == NULL) {
    return;
  }
  Thread* thread = Thread::current();
  HandleMark rm(thread);
  ttyLocker ttyl;
  int ctxkj = dep_context_arg(dept);  // -1 if no context arg
  if (witness != NULL) {
    xtty->begin_elem("dependency_failed");
  } else {
    xtty->begin_elem("dependency");
  }
  xtty->print(" type='%s'", dep_name(dept));
  if (ctxkj >= 0) {
    xtty->object("ctxk", args->at(ctxkj).metadata_value());
  }
  // write remaining arguments, if any.
  for (int j = 0; j < args->length(); j++) {
    if (j == ctxkj)  continue;  // already logged
    DepArgument arg = args->at(j);
    if (j == 1) {
      if (arg.is_oop()) {
        xtty->object("x", Handle(thread, arg.oop_value()));
      } else {
        xtty->object("x", arg.metadata_value());
      }
    } else {
      char xn[12]; sprintf(xn, "x%d", j);
      if (arg.is_oop()) {
        xtty->object(xn, Handle(thread, arg.oop_value()));
      } else {
        xtty->object(xn, arg.metadata_value());
      }
    }
  }
  if (witness != NULL) {
    xtty->object("witness", witness);
    xtty->stamp();
  }
  xtty->end_elem();
}

void Dependencies::print_dependency(DepType dept, GrowableArray<DepArgument>* args,
                                    Klass* witness, outputStream* st) {
  ResourceMark rm;
  ttyLocker ttyl;   // keep the following output all in one block
  st->print_cr("%s of type %s",
                (witness == NULL)? "Dependency": "Failed dependency",
                dep_name(dept));
  // print arguments
  int ctxkj = dep_context_arg(dept);  // -1 if no context arg
  for (int j = 0; j < args->length(); j++) {
    DepArgument arg = args->at(j);
    bool put_star = false;
    if (arg.is_null())  continue;
    const char* what;
    if (j == ctxkj) {
      assert(arg.is_metadata(), "must be");
      what = "context";
      put_star = !Dependencies::is_concrete_klass((Klass*)arg.metadata_value());
    } else if (arg.is_method()) {
      what = "method ";
      put_star = !Dependencies::is_concrete_method((Method*)arg.metadata_value(), NULL);
    } else if (arg.is_klass()) {
      what = "class  ";
    } else {
      what = "object ";
    }
    st->print("  %s = %s", what, (put_star? "*": ""));
    if (arg.is_klass()) {
      st->print("%s", ((Klass*)arg.metadata_value())->external_name());
    } else if (arg.is_method()) {
      ((Method*)arg.metadata_value())->print_value_on(st);
    } else if (arg.is_oop()) {
      arg.oop_value()->print_value_on(st);
    } else {
      ShouldNotReachHere(); // Provide impl for this type.
    }

    st->cr();
  }
  if (witness != NULL) {
    bool put_star = !Dependencies::is_concrete_klass(witness);
    st->print_cr("  witness = %s%s",
                  (put_star? "*": ""),
                  witness->external_name());
  }
}

void Dependencies::DepStream::log_dependency(Klass* witness) {
  if (_deps == NULL && xtty == NULL)  return;  // fast cutout for runtime
  ResourceMark rm;
  const int nargs = argument_count();
  GrowableArray<DepArgument>* args = new GrowableArray<DepArgument>(nargs);
  for (int j = 0; j < nargs; j++) {
    if (is_oop_argument(j)) {
      args->push(argument_oop(j));
    } else {
      args->push(argument(j));
    }
  }
  int argslen = args->length();
  if (_deps != NULL && _deps->log() != NULL) {
    if (ciEnv::current() != NULL) {
      Dependencies::write_dependency_to(_deps->log(), type(), args, witness);
    } else {
      // Treat the CompileLog as an xmlstream instead
      Dependencies::write_dependency_to((xmlStream*)_deps->log(), type(), args, witness);
    }
  } else {
    Dependencies::write_dependency_to(xtty, type(), args, witness);
  }
  guarantee(argslen == args->length(), "args array cannot grow inside nested ResoureMark scope");
}

void Dependencies::DepStream::print_dependency(Klass* witness, bool verbose, outputStream* st) {
  ResourceMark rm;
  int nargs = argument_count();
  GrowableArray<DepArgument>* args = new GrowableArray<DepArgument>(nargs);
  for (int j = 0; j < nargs; j++) {
    if (is_oop_argument(j)) {
      args->push(argument_oop(j));
    } else {
      args->push(argument(j));
    }
  }
  int argslen = args->length();
  Dependencies::print_dependency(type(), args, witness, st);
  if (verbose) {
    if (_code != NULL) {
      st->print("  code: ");
      _code->print_value_on(st);
      st->cr();
    }
  }
  guarantee(argslen == args->length(), "args array cannot grow inside nested ResoureMark scope");
}


/// Dependency stream support (decodes dependencies from an nmethod):

#ifdef ASSERT
void Dependencies::DepStream::initial_asserts(size_t byte_limit) {
  assert(must_be_in_vm(), "raw oops here");
  _byte_limit = byte_limit;
  _type       = (DepType)(end_marker-1);  // defeat "already at end" assert
  assert((_code!=NULL) + (_deps!=NULL) == 1, "one or t'other");
}
#endif //ASSERT

bool Dependencies::DepStream::next() {
  assert(_type != end_marker, "already at end");
  if (_bytes.position() == 0 && _code != NULL
      && _code->dependencies_size() == 0) {
    // Method has no dependencies at all.
    return false;
  }
  int code_byte = (_bytes.read_byte() & 0xFF);
  if (code_byte == end_marker) {
    DEBUG_ONLY(_type = end_marker);
    return false;
  } else {
    int ctxk_bit = (code_byte & Dependencies::default_context_type_bit);
    code_byte -= ctxk_bit;
    DepType dept = (DepType)code_byte;
    _type = dept;
    Dependencies::check_valid_dependency_type(dept);
    int stride = _dep_args[dept];
    assert(stride == dep_args(dept), "sanity");
    int skipj = -1;
    if (ctxk_bit != 0) {
      skipj = 0;  // currently the only context argument is at zero
      assert(skipj == dep_context_arg(dept), "zero arg always ctxk");
    }
    for (int j = 0; j < stride; j++) {
      _xi[j] = (j == skipj)? 0: _bytes.read_int();
    }
    DEBUG_ONLY(_xi[stride] = -1);   // help detect overruns
    return true;
  }
}

inline Metadata* Dependencies::DepStream::recorded_metadata_at(int i) {
  Metadata* o = NULL;
  if (_code != NULL) {
    o = _code->metadata_at(i);
  } else {
    o = _deps->oop_recorder()->metadata_at(i);
  }
  return o;
}

inline oop Dependencies::DepStream::recorded_oop_at(int i) {
  return (_code != NULL)
         ? _code->oop_at(i)
    : JNIHandles::resolve(_deps->oop_recorder()->oop_at(i));
}

Metadata* Dependencies::DepStream::argument(int i) {
  Metadata* result = recorded_metadata_at(argument_index(i));

  if (result == NULL) { // Explicit context argument can be compressed
    int ctxkj = dep_context_arg(type());  // -1 if no explicit context arg
    if (ctxkj >= 0 && i == ctxkj && ctxkj+1 < argument_count()) {
      result = ctxk_encoded_as_null(type(), argument(ctxkj+1));
    }
  }

  assert(result == NULL || result->is_klass() || result->is_method(), "must be");
  return result;
}

/**
 * Returns a unique identifier for each dependency argument.
 */
uintptr_t Dependencies::DepStream::get_identifier(int i) {
  if (is_oop_argument(i)) {
    return (uintptr_t)(oopDesc*)argument_oop(i);
  } else {
    return (uintptr_t)argument(i);
  }
}

oop Dependencies::DepStream::argument_oop(int i) {
  oop result = recorded_oop_at(argument_index(i));
  assert(oopDesc::is_oop_or_null(result), "must be");
  return result;
}

InstanceKlass* Dependencies::DepStream::context_type() {
  assert(must_be_in_vm(), "raw oops here");

  // Most dependencies have an explicit context type argument.
  {
    int ctxkj = dep_context_arg(type());  // -1 if no explicit context arg
    if (ctxkj >= 0) {
      Metadata* k = argument(ctxkj);
      assert(k != NULL && k->is_klass(), "type check");
      return InstanceKlass::cast((Klass*)k);
    }
  }

  // Some dependencies are using the klass of the first object
  // argument as implicit context type.
  {
    int ctxkj = dep_implicit_context_arg(type());
    if (ctxkj >= 0) {
      Klass* k = argument_oop(ctxkj)->klass();
      assert(k != NULL, "type check");
      return InstanceKlass::cast(k);
    }
  }

  // And some dependencies don't have a context type at all,
  // e.g. evol_method.
  return NULL;
}

// ----------------- DependencySignature --------------------------------------
bool DependencySignature::equals(DependencySignature const& s1, DependencySignature const& s2) {
  if ((s1.type() != s2.type()) || (s1.args_count() != s2.args_count())) {
    return false;
  }

  for (int i = 0; i < s1.args_count(); i++) {
    if (s1.arg(i) != s2.arg(i)) {
      return false;
    }
  }
  return true;
}

/// Checking dependencies

// This hierarchy walker inspects subtypes of a given type, trying to find a "bad" class which breaks a dependency.
// Such a class is called a "witness" to the broken dependency.
// While searching around, we ignore "participants", which are already known to the dependency.
class AbstractClassHierarchyWalker {
 public:
  enum { PARTICIPANT_LIMIT = 3 };

 private:
  // if non-zero, tells how many witnesses to convert to participants
  uint _record_witnesses;

  // special classes which are not allowed to be witnesses:
  Klass* _participants[PARTICIPANT_LIMIT+1];
  uint   _num_participants;

#ifdef ASSERT
  uint _nof_requests; // one-shot walker
#endif // ASSERT

  static PerfCounter* _perf_find_witness_anywhere_calls_count;
  static PerfCounter* _perf_find_witness_anywhere_steps_count;
  static PerfCounter* _perf_find_witness_in_calls_count;

 protected:
  virtual Klass* find_witness_in(KlassDepChange& changes) = 0;
  virtual Klass* find_witness_anywhere(InstanceKlass* context_type) = 0;

  AbstractClassHierarchyWalker(Klass* participant) : _record_witnesses(0), _num_participants(0)
#ifdef ASSERT
  , _nof_requests(0)
#endif // ASSERT
  {
    for (uint i = 0; i < PARTICIPANT_LIMIT+1; i++) {
      _participants[i] = NULL;
    }
    if (participant != NULL) {
      add_participant(participant);
    }
  }

  bool is_participant(Klass* k) {
    for (uint i = 0; i < _num_participants; i++) {
      if (_participants[i] == k) {
        return true;
      }
    }
    return false;
  }

  bool record_witness(Klass* witness) {
    if (_record_witnesses > 0) {
      --_record_witnesses;
      add_participant(witness);
      return false; // not a witness
    } else {
      return true; // is a witness
    }
  }

  class CountingClassHierarchyIterator : public ClassHierarchyIterator {
   private:
    jlong _nof_steps;
   public:
    CountingClassHierarchyIterator(InstanceKlass* root) : ClassHierarchyIterator(root), _nof_steps(0) {}

    void next() {
      _nof_steps++;
      ClassHierarchyIterator::next();
    }

    ~CountingClassHierarchyIterator() {
      if (UsePerfData) {
        _perf_find_witness_anywhere_steps_count->inc(_nof_steps);
      }
    }
  };

 public:
  uint num_participants() { return _num_participants; }
  Klass* participant(uint n) {
    assert(n <= _num_participants, "oob");
    if (n < _num_participants) {
      return _participants[n];
    } else {
      return NULL;
    }
  }

  void add_participant(Klass* participant) {
    assert(!is_participant(participant), "sanity");
    assert(_num_participants + _record_witnesses < PARTICIPANT_LIMIT, "oob");
    uint np = _num_participants++;
    _participants[np] = participant;
  }

  void record_witnesses(uint add) {
    if (add > PARTICIPANT_LIMIT)  add = PARTICIPANT_LIMIT;
    assert(_num_participants + add < PARTICIPANT_LIMIT, "oob");
    _record_witnesses = add;
  }

  Klass* find_witness(InstanceKlass* context_type, KlassDepChange* changes = NULL);

  static void init();
  static void print_statistics();
};

PerfCounter* AbstractClassHierarchyWalker::_perf_find_witness_anywhere_calls_count = NULL;
PerfCounter* AbstractClassHierarchyWalker::_perf_find_witness_anywhere_steps_count = NULL;
PerfCounter* AbstractClassHierarchyWalker::_perf_find_witness_in_calls_count       = NULL;

void AbstractClassHierarchyWalker::init() {
  if (UsePerfData) {
    EXCEPTION_MARK;
    _perf_find_witness_anywhere_calls_count =
        PerfDataManager::create_counter(SUN_CI, "findWitnessAnywhere", PerfData::U_Events, CHECK);
    _perf_find_witness_anywhere_steps_count =
        PerfDataManager::create_counter(SUN_CI, "findWitnessAnywhereSteps", PerfData::U_Events, CHECK);
    _perf_find_witness_in_calls_count =
        PerfDataManager::create_counter(SUN_CI, "findWitnessIn", PerfData::U_Events, CHECK);
  }
}

Klass* AbstractClassHierarchyWalker::find_witness(InstanceKlass* context_type, KlassDepChange* changes) {
  // Current thread must be in VM (not native mode, as in CI):
  assert(must_be_in_vm(), "raw oops here");
  // Must not move the class hierarchy during this check:
  assert_locked_or_safepoint(Compile_lock);
  assert(_nof_requests++ == 0, "repeated requests are not supported");

  assert(changes == NULL || changes->involves_context(context_type), "irrelevant dependency");

  // (Note: Interfaces do not have subclasses.)
  // If it is an interface, search its direct implementors.
  // (Their subclasses are additional indirect implementors. See InstanceKlass::add_implementor().)
  if (context_type->is_interface()) {
    int nof_impls = context_type->nof_implementors();
    if (nof_impls == 0) {
      return NULL; // no implementors
    } else if (nof_impls == 1) { // unique implementor
      assert(context_type != context_type->implementor(), "not unique");
      context_type = InstanceKlass::cast(context_type->implementor());
    } else { // nof_impls >= 2
      // Avoid this case: *I.m > { A.m, C }; B.m > C
      // Here, I.m has 2 concrete implementations, but m appears unique
      // as A.m, because the search misses B.m when checking C.
      // The inherited method B.m was getting missed by the walker
      // when interface 'I' was the starting point.
      // %%% Until this is fixed more systematically, bail out.
      return context_type;
    }
  }
  assert(!context_type->is_interface(), "no interfaces allowed");

  if (changes != NULL) {
    if (UsePerfData) {
      _perf_find_witness_in_calls_count->inc();
    }
    return find_witness_in(*changes);
  } else {
    if (UsePerfData) {
      _perf_find_witness_anywhere_calls_count->inc();
    }
    return find_witness_anywhere(context_type);
  }
}

class ConcreteSubtypeFinder : public AbstractClassHierarchyWalker {
 private:
  bool is_witness(Klass* k);

 protected:
  virtual Klass* find_witness_in(KlassDepChange& changes);
  virtual Klass* find_witness_anywhere(InstanceKlass* context_type);

 public:
  ConcreteSubtypeFinder(Klass* participant = NULL) : AbstractClassHierarchyWalker(participant) {}
};

bool ConcreteSubtypeFinder::is_witness(Klass* k) {
  if (Dependencies::is_concrete_klass(k)) {
    return record_witness(k); // concrete subtype
  } else {
    return false; // not a concrete class
  }
}

Klass* ConcreteSubtypeFinder::find_witness_in(KlassDepChange& changes) {
  // When looking for unexpected concrete types, do not look beneath expected ones:
  //  * CX > CC > C' is OK, even if C' is new.
  //  * CX > { CC,  C' } is not OK if C' is new, and C' is the witness.
  Klass* new_type = changes.as_new_klass_change()->new_type();
  assert(!is_participant(new_type), "only old classes are participants");
  // If the new type is a subtype of a participant, we are done.
  for (uint i = 0; i < num_participants(); i++) {
    if (changes.involves_context(participant(i))) {
      // new guy is protected from this check by previous participant
      return NULL;
    }
  }
  if (is_witness(new_type)) {
    return new_type;
  }
  // No witness found.  The dependency remains unbroken.
  return NULL;
}

Klass* ConcreteSubtypeFinder::find_witness_anywhere(InstanceKlass* context_type) {
  for (CountingClassHierarchyIterator iter(context_type); !iter.done(); iter.next()) {
    Klass* sub = iter.klass();
    // Do not report participant types.
    if (is_participant(sub)) {
      // Don't walk beneath a participant since it hides witnesses.
      iter.skip_subclasses();
    } else if (is_witness(sub)) {
      return sub; // found a witness
    }
  }
  // No witness found.  The dependency remains unbroken.
  return NULL;
}

class ConcreteMethodFinder : public AbstractClassHierarchyWalker {
 private:
  Symbol* _name;
  Symbol* _signature;

  // cache of method lookups
  Method* _found_methods[PARTICIPANT_LIMIT+1];

  bool is_witness(Klass* k);

 protected:
  virtual Klass* find_witness_in(KlassDepChange& changes);
  virtual Klass* find_witness_anywhere(InstanceKlass* context_type);

 public:
  bool witnessed_reabstraction_in_supers(Klass* k);

  ConcreteMethodFinder(Method* m, Klass* participant = NULL) : AbstractClassHierarchyWalker(participant) {
    assert(m != NULL && m->is_method(), "sanity");
    _name      = m->name();
    _signature = m->signature();

    for (int i = 0; i < PARTICIPANT_LIMIT+1; i++) {
      _found_methods[i] = NULL;
    }
  }

  // Note:  If n==num_participants, returns NULL.
  Method* found_method(uint n) {
    assert(n <= num_participants(), "oob");
    Method* fm = _found_methods[n];
    assert(n == num_participants() || fm != NULL, "proper usage");
    if (fm != NULL && fm->method_holder() != participant(n)) {
      // Default methods from interfaces can be added to classes. In
      // that case the holder of the method is not the class but the
      // interface where it's defined.
      assert(fm->is_default_method(), "sanity");
      return NULL;
    }
    return fm;
  }

  void add_participant(Klass* participant) {
    AbstractClassHierarchyWalker::add_participant(participant);
    _found_methods[num_participants()] = NULL;
  }

  bool record_witness(Klass* witness, Method* m) {
    _found_methods[num_participants()] = m;
    return AbstractClassHierarchyWalker::record_witness(witness);
  }

 private:
  static PerfCounter* _perf_find_witness_anywhere_calls_count;
  static PerfCounter* _perf_find_witness_anywhere_steps_count;
  static PerfCounter* _perf_find_witness_in_calls_count;

 public:
  static void init();
  static void print_statistics();
};

bool ConcreteMethodFinder::is_witness(Klass* k) {
  if (is_participant(k)) {
    return false; // do not report participant types
  }
  if (k->is_instance_klass()) {
    InstanceKlass* ik = InstanceKlass::cast(k);
    // Search class hierarchy first, skipping private implementations
    // as they never override any inherited methods
    Method* m = ik->find_instance_method(_name, _signature, Klass::PrivateLookupMode::skip);
    if (Dependencies::is_concrete_method(m, ik)) {
      return record_witness(k, m); // concrete method found
    } else {
      // Check for re-abstraction of method
      if (!ik->is_interface() && m != NULL && m->is_abstract()) {
        // Found a matching abstract method 'm' in the class hierarchy.
        // This is fine iff 'k' is an abstract class and all concrete subtypes
        // of 'k' override 'm' and are participates of the current search.
        ConcreteSubtypeFinder wf;
        for (uint i = 0; i < num_participants(); i++) {
          Klass* p = participant(i);
          wf.add_participant(p);
        }
        Klass* w = wf.find_witness(ik);
        if (w != NULL) {
          Method* wm = InstanceKlass::cast(w)->find_instance_method(_name, _signature, Klass::PrivateLookupMode::skip);
          if (!Dependencies::is_concrete_method(wm, w)) {
            // Found a concrete subtype 'w' which does not override abstract method 'm'.
            // Bail out because 'm' could be called with 'w' as receiver (leading to an
            // AbstractMethodError) and thus the method we are looking for is not unique.
            return record_witness(k, m);
          }
        }
      }
      // Check interface defaults also, if any exist.
      Array<Method*>* default_methods = ik->default_methods();
      if (default_methods != NULL) {
        Method* dm = ik->find_method(default_methods, _name, _signature);
        if (Dependencies::is_concrete_method(dm, NULL)) {
          return record_witness(k, dm); // default method found
        }
      }
      return false; // no concrete method found
    }
  } else {
    return false; // no methods to find in an array type
  }
}

Klass* ConcreteMethodFinder::find_witness_in(KlassDepChange& changes) {
  // When looking for unexpected concrete methods, look beneath expected ones, to see if there are overrides.
  //  * CX.m > CC.m > C'.m is not OK, if C'.m is new, and C' is the witness.
  Klass* new_type = changes.as_new_klass_change()->new_type();
  assert(!is_participant(new_type), "only old classes are participants");
  if (is_witness(new_type)) {
    return new_type;
  } else {
    // No witness found, but is_witness() doesn't detect method re-abstraction in case of spot-checking.
    if (witnessed_reabstraction_in_supers(new_type)) {
      return new_type;
    }
  }
  // No witness found.  The dependency remains unbroken.
  return NULL;
}

bool ConcreteMethodFinder::witnessed_reabstraction_in_supers(Klass* k) {
  if (!k->is_instance_klass()) {
    return false; // no methods to find in an array type
  } else {
    // Looking for a case when an abstract method is inherited into a concrete class.
    if (Dependencies::is_concrete_klass(k) && !k->is_interface()) {
      Method* m = InstanceKlass::cast(k)->find_instance_method(_name, _signature, Klass::PrivateLookupMode::skip);
      if (m != NULL) {
        return false; // no reabstraction possible: local method found
      }
      for (InstanceKlass* super = k->java_super(); super != NULL; super = super->java_super()) {
        m = super->find_instance_method(_name, _signature, Klass::PrivateLookupMode::skip);
        if (m != NULL) { // inherited method found
          if (m->is_abstract() || m->is_overpass()) {
            return record_witness(super, m); // abstract method found
          }
          return false;
        }
      }
      // Miranda.
      return true;
    }
    return false;
  }
}


Klass* ConcreteMethodFinder::find_witness_anywhere(InstanceKlass* context_type) {
  // Walk hierarchy under a context type, looking for unexpected types.
  for (CountingClassHierarchyIterator iter(context_type); !iter.done(); iter.next()) {
    Klass* sub = iter.klass();
    if (is_witness(sub)) {
      return sub; // found a witness
    }
  }
  // No witness found.  The dependency remains unbroken.
  return NULL;
}

// For some method m and some class ctxk (subclass of method holder),
// enumerate all distinct overrides of m in concrete subclasses of ctxk.
// It relies on vtable/itable information to perform method selection on each linked subclass
// and ignores all non yet linked ones (speculatively treat them as "effectively abstract").
class LinkedConcreteMethodFinder : public AbstractClassHierarchyWalker {
 private:
  InstanceKlass* _resolved_klass;   // resolved class (JVMS-5.4.3.1)
  InstanceKlass* _declaring_klass;  // the holder of resolved method (JVMS-5.4.3.3)
  int            _vtable_index;     // vtable/itable index of the resolved method
  bool           _do_itable_lookup; // choose between itable and vtable lookup logic

  // cache of method lookups
  Method* _found_methods[PARTICIPANT_LIMIT+1];

  bool is_witness(Klass* k);
  Method* select_method(InstanceKlass* recv_klass);
  static int compute_vtable_index(InstanceKlass* resolved_klass, Method* resolved_method, bool& is_itable_index);
  static bool is_concrete_klass(InstanceKlass* ik);

  void add_participant(Method* m, Klass* participant) {
    uint np = num_participants();
    AbstractClassHierarchyWalker::add_participant(participant);
    assert(np + 1 == num_participants(), "sanity");
    _found_methods[np] = m; // record the method for the participant
  }

  bool record_witness(Klass* witness, Method* m) {
    for (uint i = 0; i < num_participants(); i++) {
      if (found_method(i) == m) {
        return false; // already recorded
      }
    }
    // Record not yet seen method.
    _found_methods[num_participants()] = m;
    return AbstractClassHierarchyWalker::record_witness(witness);
  }

  void initialize(Method* participant) {
    for (uint i = 0; i < PARTICIPANT_LIMIT+1; i++) {
      _found_methods[i] = NULL;
    }
    if (participant != NULL) {
      add_participant(participant, participant->method_holder());
    }
  }

 protected:
  virtual Klass* find_witness_in(KlassDepChange& changes);
  virtual Klass* find_witness_anywhere(InstanceKlass* context_type);

 public:
  // In order to perform method selection, the following info is needed:
  //  (1) interface or virtual call;
  //  (2) vtable/itable index;
  //  (3) declaring class (in case of interface call).
  //
  // It is prepared based on the results of method resolution: resolved class and resolved method (as specified in JVMS-5.4.3.3).
  // Optionally, a method which was previously determined as a unique target (uniqm) is added as a participant
  // to enable dependency spot-checking and speed up the search.
  LinkedConcreteMethodFinder(InstanceKlass* resolved_klass, Method* resolved_method, Method* uniqm = NULL) : AbstractClassHierarchyWalker(NULL) {
    assert(UseVtableBasedCHA, "required");
    assert(resolved_klass->is_linked(), "required");
    assert(resolved_method->method_holder()->is_linked(), "required");
    assert(!resolved_method->can_be_statically_bound(), "no vtable index available");

    _resolved_klass  = resolved_klass;
    _declaring_klass = resolved_method->method_holder();
    _vtable_index    = compute_vtable_index(resolved_klass, resolved_method,
                                            _do_itable_lookup); // out parameter
    assert(_vtable_index >= 0, "invalid vtable index");

    initialize(uniqm);
  }

  // Note:  If n==num_participants, returns NULL.
  Method* found_method(uint n) {
    assert(n <= num_participants(), "oob");
    assert(participant(n) != NULL || n == num_participants(), "proper usage");
    return _found_methods[n];
  }
};

Klass* LinkedConcreteMethodFinder::find_witness_in(KlassDepChange& changes) {
  Klass* type = changes.type();

  assert(!is_participant(type), "only old classes are participants");

  if (is_witness(type)) {
    return type;
  }
  return NULL; // No witness found.  The dependency remains unbroken.
}

Klass* LinkedConcreteMethodFinder::find_witness_anywhere(InstanceKlass* context_type) {
  for (CountingClassHierarchyIterator iter(context_type); !iter.done(); iter.next()) {
    Klass* sub = iter.klass();
    if (is_witness(sub)) {
      return sub;
    }
    if (sub->is_instance_klass() && !InstanceKlass::cast(sub)->is_linked()) {
      iter.skip_subclasses(); // ignore not yet linked classes
    }
  }
  return NULL; // No witness found. The dependency remains unbroken.
}

bool LinkedConcreteMethodFinder::is_witness(Klass* k) {
  if (is_participant(k)) {
    return false; // do not report participant types
  } else if (k->is_instance_klass()) {
    InstanceKlass* ik = InstanceKlass::cast(k);
    if (is_concrete_klass(ik)) {
      Method* m = select_method(ik);
      return record_witness(ik, m);
    } else {
      return false; // ignore non-concrete holder class
    }
  } else {
    return false; // no methods to find in an array type
  }
}

Method* LinkedConcreteMethodFinder::select_method(InstanceKlass* recv_klass) {
  Method* selected_method = NULL;
  if (_do_itable_lookup) {
    assert(_declaring_klass->is_interface(), "sanity");
    bool implements_interface; // initialized by method_at_itable_or_null()
    selected_method = recv_klass->method_at_itable_or_null(_declaring_klass, _vtable_index,
                                                           implements_interface); // out parameter
    assert(implements_interface, "not implemented");
  } else {
    selected_method = recv_klass->method_at_vtable(_vtable_index);
  }
  return selected_method; // NULL when corresponding slot is empty (AbstractMethodError case)
}

int LinkedConcreteMethodFinder::compute_vtable_index(InstanceKlass* resolved_klass, Method* resolved_method,
                                                     // out parameter
                                                     bool& is_itable_index) {
  if (resolved_klass->is_interface() && resolved_method->has_itable_index()) {
    is_itable_index = true;
    return resolved_method->itable_index();
  }
  // Check for default or miranda method first.
  InstanceKlass* declaring_klass = resolved_method->method_holder();
  if (!resolved_klass->is_interface() && declaring_klass->is_interface()) {
    is_itable_index = false;
    return resolved_klass->vtable_index_of_interface_method(resolved_method);
  }
  // At this point we are sure that resolved_method is virtual and not
  // a default or miranda method; therefore, it must have a valid vtable index.
  assert(resolved_method->has_vtable_index(), "");
  is_itable_index = false;
  return resolved_method->vtable_index();
}

bool LinkedConcreteMethodFinder::is_concrete_klass(InstanceKlass* ik) {
  if (!Dependencies::is_concrete_klass(ik)) {
    return false; // not concrete
  }
  if (ik->is_interface()) {
    return false; // interfaces aren't concrete
  }
  if (!ik->is_linked()) {
    return false; // not yet linked classes don't have instances
  }
  return true;
}

#ifdef ASSERT
// Assert that m is inherited into ctxk, without intervening overrides.
// (May return true even if this is not true, in corner cases where we punt.)
bool Dependencies::verify_method_context(InstanceKlass* ctxk, Method* m) {
  if (m->is_private()) {
    return false; // Quick lose.  Should not happen.
  }
  if (m->method_holder() == ctxk) {
    return true;  // Quick win.
  }
  if (!(m->is_public() || m->is_protected())) {
    // The override story is complex when packages get involved.
    return true;  // Must punt the assertion to true.
  }
  Method* lm = ctxk->lookup_method(m->name(), m->signature());
  if (lm == NULL && ctxk->is_instance_klass()) {
    // It might be an interface method
    lm = InstanceKlass::cast(ctxk)->lookup_method_in_ordered_interfaces(m->name(),
                                                                        m->signature());
  }
  if (lm == m) {
    // Method m is inherited into ctxk.
    return true;
  }
  if (lm != NULL) {
    if (!(lm->is_public() || lm->is_protected())) {
      // Method is [package-]private, so the override story is complex.
      return true;  // Must punt the assertion to true.
    }
    if (lm->is_static()) {
      // Static methods don't override non-static so punt
      return true;
    }
    if (!Dependencies::is_concrete_method(lm, ctxk) &&
        !Dependencies::is_concrete_method(m, ctxk)) {
      // They are both non-concrete
      if (lm->method_holder()->is_subtype_of(m->method_holder())) {
        // Method m is overridden by lm, but both are non-concrete.
        return true;
      }
      if (lm->method_holder()->is_interface() && m->method_holder()->is_interface() &&
          ctxk->is_subtype_of(m->method_holder()) && ctxk->is_subtype_of(lm->method_holder())) {
        // Interface method defined in multiple super interfaces
        return true;
      }
    }
  }
  ResourceMark rm;
  tty->print_cr("Dependency method not found in the associated context:");
  tty->print_cr("  context = %s", ctxk->external_name());
  tty->print(   "  method = "); m->print_short_name(tty); tty->cr();
  if (lm != NULL) {
    tty->print( "  found = "); lm->print_short_name(tty); tty->cr();
  }
  return false;
}
#endif // ASSERT

bool Dependencies::is_concrete_klass(Klass* k) {
  if (k->is_abstract())  return false;
  // %%% We could treat classes which are concrete but
  // have not yet been instantiated as virtually abstract.
  // This would require a deoptimization barrier on first instantiation.
  //if (k->is_not_instantiated())  return false;
  return true;
}

bool Dependencies::is_concrete_method(Method* m, Klass* k) {
  // NULL is not a concrete method.
  if (m == NULL) {
    return false;
  }
  // Statics are irrelevant to virtual call sites.
  if (m->is_static()) {
    return false;
  }
  // Abstract methods are not concrete.
  if (m->is_abstract()) {
    return false;
  }
  // Overpass (error) methods are not concrete if k is abstract.
  if (m->is_overpass() && k != NULL) {
     return !k->is_abstract();
  }
  // Note "true" is conservative answer: overpass clause is false if k == NULL,
  // implies return true if answer depends on overpass clause.
  return true;
 }

Klass* Dependencies::find_finalizable_subclass(InstanceKlass* ik) {
  for (ClassHierarchyIterator iter(ik); !iter.done(); iter.next()) {
    Klass* sub = iter.klass();
    if (sub->has_finalizer() && !sub->is_interface()) {
      return sub;
    }
  }
  return NULL; // not found
}

bool Dependencies::is_concrete_klass(ciInstanceKlass* k) {
  if (k->is_abstract())  return false;
  // We could also return false if k does not yet appear to be
  // instantiated, if the VM version supports this distinction also.
  //if (k->is_not_instantiated())  return false;
  return true;
}

bool Dependencies::has_finalizable_subclass(ciInstanceKlass* k) {
  return k->has_finalizable_subclass();
}

// Any use of the contents (bytecodes) of a method must be
// marked by an "evol_method" dependency, if those contents
// can change.  (Note: A method is always dependent on itself.)
Klass* Dependencies::check_evol_method(Method* m) {
  assert(must_be_in_vm(), "raw oops here");
  // Did somebody do a JVMTI RedefineClasses while our backs were turned?
  // Or is there a now a breakpoint?
  // (Assumes compiled code cannot handle bkpts; change if UseFastBreakpoints.)
  if (m->is_old()
      || m->number_of_breakpoints() > 0) {
    return m->method_holder();
  } else {
    return NULL;
  }
}

// This is a strong assertion:  It is that the given type
// has no subtypes whatever.  It is most useful for
// optimizing checks on reflected types or on array types.
// (Checks on types which are derived from real instances
// can be optimized more strongly than this, because we
// know that the checked type comes from a concrete type,
// and therefore we can disregard abstract types.)
Klass* Dependencies::check_leaf_type(InstanceKlass* ctxk) {
  assert(must_be_in_vm(), "raw oops here");
  assert_locked_or_safepoint(Compile_lock);
  Klass* sub = ctxk->subklass();
  if (sub != NULL) {
    return sub;
  } else if (ctxk->nof_implementors() != 0) {
    // if it is an interface, it must be unimplemented
    // (if it is not an interface, nof_implementors is always zero)
    InstanceKlass* impl = ctxk->implementor();
    assert(impl != NULL, "must be set");
    return impl;
  } else {
    return NULL;
  }
}

// Test the assertion that conck is the only concrete subtype* of ctxk.
// The type conck itself is allowed to have have further concrete subtypes.
// This allows the compiler to narrow occurrences of ctxk by conck,
// when dealing with the types of actual instances.
Klass* Dependencies::check_abstract_with_unique_concrete_subtype(InstanceKlass* ctxk,
                                                                 Klass* conck,
                                                                 NewKlassDepChange* changes) {
  ConcreteSubtypeFinder wf(conck);
  Klass* k = wf.find_witness(ctxk, changes);
  return k;
}


// Find the unique concrete proper subtype of ctxk, or NULL if there
// is more than one concrete proper subtype.  If there are no concrete
// proper subtypes, return ctxk itself, whether it is concrete or not.
// The returned subtype is allowed to have have further concrete subtypes.
// That is, return CC1 for CX > CC1 > CC2, but NULL for CX > { CC1, CC2 }.
Klass* Dependencies::find_unique_concrete_subtype(InstanceKlass* ctxk) {
  ConcreteSubtypeFinder wf(ctxk);  // Ignore ctxk when walking.
  wf.record_witnesses(1);          // Record one other witness when walking.
  Klass* wit = wf.find_witness(ctxk);
  if (wit != NULL)  return NULL;   // Too many witnesses.
  Klass* conck = wf.participant(0);
  if (conck == NULL) {
    return ctxk;                   // Return ctxk as a flag for "no subtypes".
  } else {
#ifndef PRODUCT
    // Make sure the dependency mechanism will pass this discovery:
    if (VerifyDependencies) {
      // Turn off dependency tracing while actually testing deps.
      FlagSetting fs(TraceDependencies, false);
      if (!Dependencies::is_concrete_klass(ctxk)) {
        guarantee(NULL == (void *)
                  check_abstract_with_unique_concrete_subtype(ctxk, conck),
                  "verify dep.");
      }
    }
#endif //PRODUCT
    return conck;
  }
}

// Try to determine whether root method in some context is concrete or not based on the information about the unique method
// in that context. It exploits the fact that concrete root method is always inherited into the context when there's a unique method.
// Hence, unique method holder is always a supertype of the context class when root method is concrete.
// Examples for concrete_root_method
//      C (C.m uniqm)
//      |
//      CX (ctxk) uniqm is inherited into context.
//
//      CX (ctxk) (CX.m uniqm) here uniqm is defined in ctxk.
// Examples for !concrete_root_method
//      CX (ctxk)
//      |
//      C (C.m uniqm) uniqm is in subtype of ctxk.
bool Dependencies::is_concrete_root_method(Method* uniqm, InstanceKlass* ctxk) {
  if (uniqm == NULL) {
    return false; // match Dependencies::is_concrete_method() behavior
  }
  // Theoretically, the "direction" of subtype check matters here.
  // On one hand, in case of interface context with a single implementor, uniqm can be in a superclass of the implementor which
  // is not related to context class.
  // On another hand, uniqm could come from an interface unrelated to the context class, but right now it is not possible:
  // it is required that uniqm->method_holder() is the participant (uniqm->method_holder() <: ctxk), hence a default method
  // can't be used as unique.
  if (ctxk->is_interface()) {
    InstanceKlass* implementor = ctxk->implementor();
    assert(implementor != ctxk, "single implementor only"); // should have been invalidated earlier
    ctxk = implementor;
  }
  InstanceKlass* holder = uniqm->method_holder();
  assert(!holder->is_interface(), "no default methods allowed");
  assert(ctxk->is_subclass_of(holder) || holder->is_subclass_of(ctxk), "not related");
  return ctxk->is_subclass_of(holder);
}

// If a class (or interface) has a unique concrete method uniqm, return NULL.
// Otherwise, return a class that contains an interfering method.
Klass* Dependencies::check_unique_concrete_method(InstanceKlass* ctxk,
                                                  Method* uniqm,
                                                  NewKlassDepChange* changes) {
  ConcreteMethodFinder wf(uniqm, uniqm->method_holder());
  Klass* k = wf.find_witness(ctxk, changes);
  if (k != NULL) {
    return k;
  }
  if (!Dependencies::is_concrete_root_method(uniqm, ctxk) || changes != NULL) {
    Klass* conck = find_witness_AME(ctxk, uniqm, changes);
    if (conck != NULL) {
      // Found a concrete subtype 'conck' which does not override abstract root method.
      return conck;
    }
  }
  return NULL;
}

// Search for AME.
// There are two version of checks.
//   1) Spot checking version(Classload time). Newly added class is checked for AME.
//      Checks whether abstract/overpass method is inherited into/declared in newly added concrete class.
//   2) Compile time analysis for abstract/overpass(abstract klass) root_m. The non uniqm subtrees are checked for concrete classes.
Klass* Dependencies::find_witness_AME(InstanceKlass* ctxk, Method* m, KlassDepChange* changes) {
  if (m != NULL) {
    if (changes != NULL) {
      // Spot checking version.
      ConcreteMethodFinder wf(m);
      Klass* new_type = changes->as_new_klass_change()->new_type();
      if (wf.witnessed_reabstraction_in_supers(new_type)) {
        return new_type;
      }
    } else {
      // Note: It is required that uniqm->method_holder() is the participant (see ClassHierarchyWalker::found_method()).
      ConcreteSubtypeFinder wf(m->method_holder());
      Klass* conck = wf.find_witness(ctxk);
      if (conck != NULL) {
        Method* cm = InstanceKlass::cast(conck)->find_instance_method(m->name(), m->signature(), Klass::PrivateLookupMode::skip);
        if (!Dependencies::is_concrete_method(cm, conck)) {
          return conck;
        }
      }
    }
  }
  return NULL;
}


// Find the set of all non-abstract methods under ctxk that match m.
// (The method m must be defined or inherited in ctxk.)
// Include m itself in the set, unless it is abstract.
// If this set has exactly one element, return that element.
Method* Dependencies::find_unique_concrete_method(InstanceKlass* ctxk, Method* m, Klass** participant) {
  // Return NULL if m is marked old; must have been a redefined method.
  if (m->is_old()) {
    return NULL;
  }
  if (m->is_default_method()) {
    return NULL; // not supported
  }
  assert(verify_method_context(ctxk, m), "proper context");
  ConcreteMethodFinder wf(m);
  wf.record_witnesses(1);
  Klass* wit = wf.find_witness(ctxk);
  if (wit != NULL)  return NULL;  // Too many witnesses.
  Method* fm = wf.found_method(0);  // Will be NULL if num_parts == 0.
  if (participant != NULL) {
    (*participant) = wf.participant(0);
  }
  if (!Dependencies::is_concrete_method(fm, NULL)) {
    fm = NULL; // ignore abstract methods
  }
  if (Dependencies::is_concrete_method(m, ctxk)) {
    if (fm == NULL) {
      // It turns out that m was always the only implementation.
      fm = m;
    } else if (fm != m) {
      // Two conflicting implementations after all.
      // (This can happen if m is inherited into ctxk and fm overrides it.)
      return NULL;
    }
  } else if (Dependencies::find_witness_AME(ctxk, fm) != NULL) {
    // Found a concrete subtype which does not override abstract root method.
    return NULL;
  }
  assert(Dependencies::is_concrete_root_method(fm, ctxk) == Dependencies::is_concrete_method(m, ctxk), "mismatch");
#ifndef PRODUCT
  // Make sure the dependency mechanism will pass this discovery:
  if (VerifyDependencies && fm != NULL) {
    guarantee(NULL == (void *)check_unique_concrete_method(ctxk, fm),
              "verify dep.");
  }
#endif //PRODUCT
  return fm;
}

// If a class (or interface) has a unique concrete method uniqm, return NULL.
// Otherwise, return a class that contains an interfering method.
Klass* Dependencies::check_unique_concrete_method(InstanceKlass* ctxk,
                                                  Method* uniqm,
                                                  Klass* resolved_klass,
                                                  Method* resolved_method,
                                                  KlassDepChange* changes) {
  assert(UseVtableBasedCHA, "required");
  assert(!ctxk->is_interface() || ctxk == resolved_klass, "sanity");
  assert(!resolved_method->can_be_statically_bound() || resolved_method == uniqm, "sanity");
  assert(resolved_klass->is_subtype_of(resolved_method->method_holder()), "sanity");

  if (!InstanceKlass::cast(resolved_klass)->is_linked() ||
      !resolved_method->method_holder()->is_linked() ||
      resolved_method->can_be_statically_bound()) {
    // Dependency is redundant, but benign. Just keep it to avoid unnecessary recompilation.
    return NULL; // no vtable index available
  }

  LinkedConcreteMethodFinder mf(InstanceKlass::cast(resolved_klass), resolved_method, uniqm);
  return mf.find_witness(ctxk, changes);
}

// Find the set of all non-abstract methods under ctxk that match m.
// (The method m must be defined or inherited in ctxk.)
// Include m itself in the set, unless it is abstract.
// If this set has exactly one element, return that element.
// Not yet linked subclasses of ctxk are ignored since they don't have any instances yet.
// Additionally, resolved_klass and resolved_method complete the description of the call site being analyzed.
Method* Dependencies::find_unique_concrete_method(InstanceKlass* ctxk, Method* m, Klass* resolved_klass, Method* resolved_method) {
  // Return NULL if m is marked old; must have been a redefined method.
  if (m->is_old()) {
    return NULL;
  }
  if (!InstanceKlass::cast(resolved_klass)->is_linked() ||
      !resolved_method->method_holder()->is_linked() ||
      resolved_method->can_be_statically_bound()) {
    return m; // nothing to do: no witness under ctxk
  }
  LinkedConcreteMethodFinder wf(InstanceKlass::cast(resolved_klass), resolved_method);
  assert(Dependencies::verify_method_context(ctxk, m), "proper context");
  wf.record_witnesses(1);
  Klass* wit = wf.find_witness(ctxk);
  if (wit != NULL) {
    return NULL;  // Too many witnesses.
  }
  // p == NULL when no participants are found (wf.num_participants() == 0).
  // fm == NULL case has 2 meanings:
  //  * when p == NULL: no method found;
  //  * when p != NULL: AbstractMethodError-throwing method found.
  // Also, found method should always be accompanied by a participant class.
  Klass*   p = wf.participant(0);
  Method* fm = wf.found_method(0);
  assert(fm == NULL || p != NULL, "no participant");
  // Normalize all error-throwing cases to NULL.
  if (fm == Universe::throw_illegal_access_error() ||
      fm == Universe::throw_no_such_method_error() ||
      !Dependencies::is_concrete_method(fm, p)) {
    fm = NULL; // error-throwing method
  }
  if (Dependencies::is_concrete_method(m, ctxk)) {
    if (p == NULL) {
      // It turns out that m was always the only implementation.
      assert(fm == NULL, "sanity");
      fm = m;
    }
  }
#ifndef PRODUCT
  // Make sure the dependency mechanism will pass this discovery:
  if (VerifyDependencies && fm != NULL) {
    guarantee(NULL == check_unique_concrete_method(ctxk, fm, resolved_klass, resolved_method),
              "verify dep.");
  }
#endif // PRODUCT
  assert(fm == NULL || !fm->is_abstract(), "sanity");
  // Old CHA conservatively reports concrete methods in abstract classes
  // irrespective of whether they have concrete subclasses or not.
  // Also, abstract root method case is not fully supported.
#ifdef ASSERT
  Klass*  uniqp = NULL;
  Method* uniqm = Dependencies::find_unique_concrete_method(ctxk, m, &uniqp);
  assert(uniqm == NULL || uniqm == fm ||
         m->is_abstract() ||
         uniqm->method_holder()->is_abstract() ||
         (fm == NULL && uniqm != NULL && uniqp != NULL && !InstanceKlass::cast(uniqp)->is_linked()),
         "sanity");
#endif // ASSERT
  return fm;
}

Klass* Dependencies::check_has_no_finalizable_subclasses(InstanceKlass* ctxk, NewKlassDepChange* changes) {
  InstanceKlass* search_at = ctxk;
  if (changes != NULL) {
    search_at = changes->new_type(); // just look at the new bit
  }
  return find_finalizable_subclass(search_at);
}

Klass* Dependencies::check_call_site_target_value(oop call_site, oop method_handle, CallSiteDepChange* changes) {
  assert(call_site != NULL, "sanity");
  assert(method_handle != NULL, "sanity");
  assert(call_site->is_a(vmClasses::CallSite_klass()),     "sanity");

  if (changes == NULL) {
    // Validate all CallSites
    if (java_lang_invoke_CallSite::target(call_site) != method_handle)
      return call_site->klass();  // assertion failed
  } else {
    // Validate the given CallSite
    if (call_site == changes->call_site() && java_lang_invoke_CallSite::target(call_site) != changes->method_handle()) {
      assert(method_handle != changes->method_handle(), "must be");
      return call_site->klass();  // assertion failed
    }
  }
  return NULL;  // assertion still valid
}

void Dependencies::DepStream::trace_and_log_witness(Klass* witness) {
  if (witness != NULL) {
    if (TraceDependencies) {
      print_dependency(witness, /*verbose=*/ true);
    }
    // The following is a no-op unless logging is enabled:
    log_dependency(witness);
  }
}

Klass* Dependencies::DepStream::check_new_klass_dependency(NewKlassDepChange* changes) {
  assert_locked_or_safepoint(Compile_lock);
  Dependencies::check_valid_dependency_type(type());

  Klass* witness = NULL;
  switch (type()) {
  case evol_method:
    witness = check_evol_method(method_argument(0));
    break;
  case leaf_type:
    witness = check_leaf_type(context_type());
    break;
  case abstract_with_unique_concrete_subtype:
    witness = check_abstract_with_unique_concrete_subtype(context_type(), type_argument(1), changes);
    break;
  case unique_concrete_method_2:
    witness = check_unique_concrete_method(context_type(), method_argument(1), changes);
    break;
  case unique_concrete_method_4:
    witness = check_unique_concrete_method(context_type(), method_argument(1), type_argument(2), method_argument(3), changes);
    break;
  case no_finalizable_subclasses:
    witness = check_has_no_finalizable_subclasses(context_type(), changes);
    break;
  default:
    witness = NULL;
    break;
  }
  trace_and_log_witness(witness);
  return witness;
}

Klass* Dependencies::DepStream::check_klass_init_dependency(KlassInitDepChange* changes) {
  assert_locked_or_safepoint(Compile_lock);
  Dependencies::check_valid_dependency_type(type());

  // No new types added. Only unique_concrete_method_4 is sensitive to class initialization changes.
  Klass* witness = NULL;
  switch (type()) {
  case unique_concrete_method_4:
    witness = check_unique_concrete_method(context_type(), method_argument(1), type_argument(2), method_argument(3), changes);
    break;
  default:
    witness = NULL;
    break;
  }
  trace_and_log_witness(witness);
  return witness;
}

Klass* Dependencies::DepStream::check_klass_dependency(KlassDepChange* changes) {
  assert_locked_or_safepoint(Compile_lock);
  Dependencies::check_valid_dependency_type(type());

  if (changes != NULL) {
    if (UseVtableBasedCHA && changes->is_klass_init_change()) {
      return check_klass_init_dependency(changes->as_klass_init_change());
    } else {
      return check_new_klass_dependency(changes->as_new_klass_change());
    }
  } else {
    Klass* witness = check_new_klass_dependency(NULL);
    // check_klass_init_dependency duplicates check_new_klass_dependency checks when class hierarchy change info is absent.
    assert(witness != NULL || check_klass_init_dependency(NULL) == NULL, "missed dependency");
    return witness;
  }
}

Klass* Dependencies::DepStream::check_call_site_dependency(CallSiteDepChange* changes) {
  assert_locked_or_safepoint(Compile_lock);
  Dependencies::check_valid_dependency_type(type());

  Klass* witness = NULL;
  switch (type()) {
  case call_site_target_value:
    witness = check_call_site_target_value(argument_oop(0), argument_oop(1), changes);
    break;
  default:
    witness = NULL;
    break;
  }
  trace_and_log_witness(witness);
  return witness;
}


Klass* Dependencies::DepStream::spot_check_dependency_at(DepChange& changes) {
  // Handle klass dependency
  if (changes.is_klass_change() && changes.as_klass_change()->involves_context(context_type()))
    return check_klass_dependency(changes.as_klass_change());

  // Handle CallSite dependency
  if (changes.is_call_site_change())
    return check_call_site_dependency(changes.as_call_site_change());

  // irrelevant dependency; skip it
  return NULL;
}


void DepChange::print() {
  int nsup = 0, nint = 0;
  for (ContextStream str(*this); str.next(); ) {
    Klass* k = str.klass();
    switch (str.change_type()) {
    case Change_new_type:
      tty->print_cr("  dependee = %s", k->external_name());
      break;
    case Change_new_sub:
      if (!WizardMode) {
        ++nsup;
      } else {
        tty->print_cr("  context super = %s", k->external_name());
      }
      break;
    case Change_new_impl:
      if (!WizardMode) {
        ++nint;
      } else {
        tty->print_cr("  context interface = %s", k->external_name());
      }
      break;
    default:
      break;
    }
  }
  if (nsup + nint != 0) {
    tty->print_cr("  context supers = %d, interfaces = %d", nsup, nint);
  }
}

void DepChange::ContextStream::start() {
  Klass* type = (_changes.is_klass_change() ? _changes.as_klass_change()->type() : (Klass*) NULL);
  _change_type = (type == NULL ? NO_CHANGE : Start_Klass);
  _klass = type;
  _ti_base = NULL;
  _ti_index = 0;
  _ti_limit = 0;
}

bool DepChange::ContextStream::next() {
  switch (_change_type) {
  case Start_Klass:             // initial state; _klass is the new type
    _ti_base = InstanceKlass::cast(_klass)->transitive_interfaces();
    _ti_index = 0;
    _change_type = Change_new_type;
    return true;
  case Change_new_type:
    // fall through:
    _change_type = Change_new_sub;
  case Change_new_sub:
    // 6598190: brackets workaround Sun Studio C++ compiler bug 6629277
    {
      _klass = _klass->super();
      if (_klass != NULL) {
        return true;
      }
    }
    // else set up _ti_limit and fall through:
    _ti_limit = (_ti_base == NULL) ? 0 : _ti_base->length();
    _change_type = Change_new_impl;
  case Change_new_impl:
    if (_ti_index < _ti_limit) {
      _klass = _ti_base->at(_ti_index++);
      return true;
    }
    // fall through:
    _change_type = NO_CHANGE;  // iterator is exhausted
  case NO_CHANGE:
    break;
  default:
    ShouldNotReachHere();
  }
  return false;
}

void KlassDepChange::initialize() {
  // entire transaction must be under this lock:
  assert_lock_strong(Compile_lock);

  // Mark all dependee and all its superclasses
  // Mark transitive interfaces
  for (ContextStream str(*this); str.next(); ) {
    Klass* d = str.klass();
    assert(!InstanceKlass::cast(d)->is_marked_dependent(), "checking");
    InstanceKlass::cast(d)->set_is_marked_dependent(true);
  }
}

KlassDepChange::~KlassDepChange() {
  // Unmark all dependee and all its superclasses
  // Unmark transitive interfaces
  for (ContextStream str(*this); str.next(); ) {
    Klass* d = str.klass();
    InstanceKlass::cast(d)->set_is_marked_dependent(false);
  }
}

bool KlassDepChange::involves_context(Klass* k) {
  if (k == NULL || !k->is_instance_klass()) {
    return false;
  }
  InstanceKlass* ik = InstanceKlass::cast(k);
  bool is_contained = ik->is_marked_dependent();
  assert(is_contained == type()->is_subtype_of(k),
         "correct marking of potential context types");
  return is_contained;
}

void Dependencies::print_statistics() {
  AbstractClassHierarchyWalker::print_statistics();
}

void AbstractClassHierarchyWalker::print_statistics() {
  if (UsePerfData) {
    jlong deps_find_witness_calls   = _perf_find_witness_anywhere_calls_count->get_value();
    jlong deps_find_witness_steps   = _perf_find_witness_anywhere_steps_count->get_value();
    jlong deps_find_witness_singles = _perf_find_witness_in_calls_count->get_value();

    ttyLocker ttyl;
    tty->print_cr("Dependency check (find_witness) "
                  "calls=" JLONG_FORMAT ", steps=" JLONG_FORMAT " (avg=%.1f), singles=" JLONG_FORMAT,
                  deps_find_witness_calls,
                  deps_find_witness_steps,
                  (double)deps_find_witness_steps / deps_find_witness_calls,
                  deps_find_witness_singles);
    if (xtty != NULL) {
      xtty->elem("deps_find_witness calls='" JLONG_FORMAT "' steps='" JLONG_FORMAT "' singles='" JLONG_FORMAT "'",
                 deps_find_witness_calls,
                 deps_find_witness_steps,
                 deps_find_witness_singles);
    }
  }
}

CallSiteDepChange::CallSiteDepChange(Handle call_site, Handle method_handle) :
  _call_site(call_site),
  _method_handle(method_handle) {
  assert(_call_site()->is_a(vmClasses::CallSite_klass()), "must be");
  assert(_method_handle.is_null() || _method_handle()->is_a(vmClasses::MethodHandle_klass()), "must be");
}

void dependencies_init() {
  AbstractClassHierarchyWalker::init();
}
