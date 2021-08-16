/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/javaClasses.inline.hpp"
#include "code/codeCache.hpp"
#include "code/debugInfoRec.hpp"
#include "code/nmethod.hpp"
#include "code/pcDesc.hpp"
#include "code/scopeDesc.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/oopMapCache.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/oop.inline.hpp"
#include "prims/jvmtiDeferredUpdates.hpp"
#include "runtime/basicLock.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/monitorChunk.hpp"
#include "runtime/signature.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/vframeArray.hpp"
#include "runtime/vframe_hp.hpp"
#ifdef COMPILER2
#include "opto/matcher.hpp"
#endif


// ------------- compiledVFrame --------------

StackValueCollection* compiledVFrame::locals() const {
  // Natives has no scope
  if (scope() == NULL) return new StackValueCollection(0);
  GrowableArray<ScopeValue*>*  scv_list = scope()->locals();
  if (scv_list == NULL) return new StackValueCollection(0);

  // scv_list is the list of ScopeValues describing the JVM stack state.
  // There is one scv_list entry for every JVM stack state in use.
  int length = scv_list->length();
  StackValueCollection* result = new StackValueCollection(length);
  for (int i = 0; i < length; i++) {
    result->add(create_stack_value(scv_list->at(i)));
  }

  // Replace the original values with any stores that have been
  // performed through compiledVFrame::update_locals.
  GrowableArray<jvmtiDeferredLocalVariableSet*>* list = JvmtiDeferredUpdates::deferred_locals(thread());
  if (list != NULL ) {
    // In real life this never happens or is typically a single element search
    for (int i = 0; i < list->length(); i++) {
      if (list->at(i)->matches(this)) {
        list->at(i)->update_locals(result);
        break;
      }
    }
  }

  return result;
}


void compiledVFrame::set_locals(StackValueCollection* values) const {

  fatal("Should use update_local for each local update");
}

void compiledVFrame::update_local(BasicType type, int index, jvalue value) {
  assert(index >= 0 && index < method()->max_locals(), "out of bounds");
  update_deferred_value(type, index, value);
}

void compiledVFrame::update_stack(BasicType type, int index, jvalue value) {
  assert(index >= 0 && index < method()->max_stack(), "out of bounds");
  update_deferred_value(type, index + method()->max_locals(), value);
}

void compiledVFrame::update_monitor(int index, MonitorInfo* val) {
  assert(index >= 0, "out of bounds");
  jvalue value;
  value.l = cast_from_oop<jobject>(val->owner());
  update_deferred_value(T_OBJECT, index + method()->max_locals() + method()->max_stack(), value);
}

void compiledVFrame::update_deferred_value(BasicType type, int index, jvalue value) {
  assert(fr().is_deoptimized_frame(), "frame must be scheduled for deoptimization");
  GrowableArray<jvmtiDeferredLocalVariableSet*>* deferred = JvmtiDeferredUpdates::deferred_locals(thread());
  jvmtiDeferredLocalVariableSet* locals = NULL;
  if (deferred != NULL ) {
    // See if this vframe has already had locals with deferred writes
    for (int f = 0; f < deferred->length(); f++ ) {
      if (deferred->at(f)->matches(this)) {
        locals = deferred->at(f);
        break;
      }
    }
    // No matching vframe must push a new vframe
  } else {
    // No deferred updates pending for this thread.
    // allocate in C heap
    JvmtiDeferredUpdates::create_for(thread());
    deferred = JvmtiDeferredUpdates::deferred_locals(thread());
  }
  if (locals == NULL) {
    locals = new jvmtiDeferredLocalVariableSet(method(), bci(), fr().id(), vframe_id());
    deferred->push(locals);
    assert(locals->id() == fr().id(), "Huh? Must match");
  }
  locals->set_value_at(index, type, value);
}

// After object deoptimization, that is object reallocation and relocking, we
// create deferred updates for all objects in scope. No new update will be
// created if a deferred update already exists. It is not easy to see how this
// is achieved: the deoptimized objects are in the arrays returned by locals(),
// expressions(), and monitors(). For each object in these arrays we create a
// deferred updated. If an update already exists, then it will override the
// corresponding deoptimized object returned in one of the arrays. So the
// original update is kept.
void compiledVFrame::create_deferred_updates_after_object_deoptimization() {
  // locals
  GrowableArray<ScopeValue*>* scopeLocals = scope()->locals();
  StackValueCollection* lcls = locals();
  if (lcls != NULL) {
    for (int i2 = 0; i2 < lcls->size(); i2++) {
      StackValue* var = lcls->at(i2);
      if (var->type() == T_OBJECT && scopeLocals->at(i2)->is_object()) {
        jvalue val;
        val.l = cast_from_oop<jobject>(lcls->at(i2)->get_obj()());
        update_local(T_OBJECT, i2, val);
      }
    }
  }

  // expressions
  GrowableArray<ScopeValue*>* scopeExpressions = scope()->expressions();
  StackValueCollection* exprs = expressions();
  if (exprs != NULL) {
    for (int i2 = 0; i2 < exprs->size(); i2++) {
      StackValue* var = exprs->at(i2);
      if (var->type() == T_OBJECT && scopeExpressions->at(i2)->is_object()) {
        jvalue val;
        val.l = cast_from_oop<jobject>(exprs->at(i2)->get_obj()());
        update_stack(T_OBJECT, i2, val);
      }
    }
  }

  // monitors
  GrowableArray<MonitorInfo*>* mtrs = monitors();
  if (mtrs != NULL) {
    for (int i2 = 0; i2 < mtrs->length(); i2++) {
      if (mtrs->at(i2)->eliminated()) {
        assert(!mtrs->at(i2)->owner_is_scalar_replaced(),
               "reallocation failure, should not update");
        update_monitor(i2, mtrs->at(i2));
      }
    }
  }
}

StackValueCollection* compiledVFrame::expressions() const {
  // Natives has no scope
  if (scope() == NULL) return new StackValueCollection(0);
  GrowableArray<ScopeValue*>*  scv_list = scope()->expressions();
  if (scv_list == NULL) return new StackValueCollection(0);

  // scv_list is the list of ScopeValues describing the JVM stack state.
  // There is one scv_list entry for every JVM stack state in use.
  int length = scv_list->length();
  StackValueCollection* result = new StackValueCollection(length);
  for (int i = 0; i < length; i++) {
    result->add(create_stack_value(scv_list->at(i)));
  }

  // Replace the original values with any stores that have been
  // performed through compiledVFrame::update_stack.
  GrowableArray<jvmtiDeferredLocalVariableSet*>* list = JvmtiDeferredUpdates::deferred_locals(thread());
  if (list != NULL ) {
    // In real life this never happens or is typically a single element search
    for (int i = 0; i < list->length(); i++) {
      if (list->at(i)->matches(this)) {
        list->at(i)->update_stack(result);
        break;
      }
    }
  }

  return result;
}


// The implementation of the following two methods was factorized into the
// class StackValue because it is also used from within deoptimization.cpp for
// rematerialization and relocking of non-escaping objects.

StackValue *compiledVFrame::create_stack_value(ScopeValue *sv) const {
  return StackValue::create_stack_value(&_fr, register_map(), sv);
}

BasicLock* compiledVFrame::resolve_monitor_lock(Location location) const {
  return StackValue::resolve_monitor_lock(&_fr, location);
}


GrowableArray<MonitorInfo*>* compiledVFrame::monitors() const {
  // Natives has no scope
  if (scope() == NULL) {
    CompiledMethod* nm = code();
    Method* method = nm->method();
    assert(method->is_native(), "Expect a native method");
    if (!method->is_synchronized()) {
      return new GrowableArray<MonitorInfo*>(0);
    }
    // This monitor is not really needed but return it for now as it might be
    // useful for stack traces and tools
    GrowableArray<MonitorInfo*> *monitors = new GrowableArray<MonitorInfo*>(1);
    // Casting away const
    frame& fr = (frame&) _fr;
    MonitorInfo* info = new MonitorInfo(
        fr.get_native_receiver(), fr.get_native_monitor(), false, false);
    monitors->push(info);
    return monitors;
  }
  GrowableArray<MonitorValue*>* monitors = scope()->monitors();
  if (monitors == NULL) {
    return new GrowableArray<MonitorInfo*>(0);
  }
  GrowableArray<MonitorInfo*>* result = new GrowableArray<MonitorInfo*>(monitors->length());
  for (int index = 0; index < monitors->length(); index++) {
    MonitorValue* mv = monitors->at(index);
    ScopeValue*   ov = mv->owner();
    StackValue *owner_sv = create_stack_value(ov); // it is an oop
    if (ov->is_object() && owner_sv->obj_is_scalar_replaced()) { // The owner object was scalar replaced
      assert(mv->eliminated(), "monitor should be eliminated for scalar replaced object");
      // Put klass for scalar replaced object.
      ScopeValue* kv = ((ObjectValue *)ov)->klass();
      assert(kv->is_constant_oop(), "klass should be oop constant for scalar replaced object");
      Handle k(Thread::current(), ((ConstantOopReadValue*)kv)->value()());
      assert(java_lang_Class::is_instance(k()), "must be");
      result->push(new MonitorInfo(k(), resolve_monitor_lock(mv->basic_lock()),
                                   mv->eliminated(), true));
    } else {
      result->push(new MonitorInfo(owner_sv->get_obj()(), resolve_monitor_lock(mv->basic_lock()),
                                   mv->eliminated(), false));
    }
  }

  // Replace the original values with any stores that have been
  // performed through compiledVFrame::update_monitors.
  GrowableArray<jvmtiDeferredLocalVariableSet*>* list = JvmtiDeferredUpdates::deferred_locals(thread());
  if (list != NULL ) {
    // In real life this never happens or is typically a single element search
    for (int i = 0; i < list->length(); i++) {
      if (list->at(i)->matches(this)) {
        list->at(i)->update_monitors(result);
        break;
      }
    }
  }

  return result;
}


compiledVFrame::compiledVFrame(const frame* fr, const RegisterMap* reg_map, JavaThread* thread, CompiledMethod* nm)
: javaVFrame(fr, reg_map, thread) {
  _scope  = NULL;
  _vframe_id = 0;
  // Compiled method (native stub or Java code)
  // native wrappers have no scope data, it is implied
  if (!nm->is_compiled() || !nm->as_compiled_method()->is_native_method()) {
      _scope  = nm->scope_desc_at(_fr.pc());
  }
}

compiledVFrame::compiledVFrame(const frame* fr, const RegisterMap* reg_map, JavaThread* thread, ScopeDesc* scope, int vframe_id)
: javaVFrame(fr, reg_map, thread) {
  _scope  = scope;
  _vframe_id = vframe_id;
  guarantee(_scope != NULL, "scope must be present");
}

compiledVFrame* compiledVFrame::at_scope(int decode_offset, int vframe_id) {
  if (scope()->decode_offset() != decode_offset) {
    ScopeDesc* scope = this->scope()->at_offset(decode_offset);
    return new compiledVFrame(frame_pointer(), register_map(), thread(), scope, vframe_id);
  }
  assert(_vframe_id == vframe_id, "wrong frame id");
  return this;
}

bool compiledVFrame::is_top() const {
  // FIX IT: Remove this when new native stubs are in place
  if (scope() == NULL) return true;
  return scope()->is_top();
}


CompiledMethod* compiledVFrame::code() const {
  return CodeCache::find_compiled(_fr.pc());
}


Method* compiledVFrame::method() const {
  if (scope() == NULL) {
    // native nmethods have no scope the method is implied
    nmethod* nm = code()->as_nmethod();
    assert(nm->is_native_method(), "must be native");
    return nm->method();
  }
  return scope()->method();
}


int compiledVFrame::bci() const {
  int raw = raw_bci();
  return raw == SynchronizationEntryBCI ? 0 : raw;
}


int compiledVFrame::raw_bci() const {
  if (scope() == NULL) {
    // native nmethods have no scope the method/bci is implied
    nmethod* nm = code()->as_nmethod();
    assert(nm->is_native_method(), "must be native");
    return 0;
  }
  return scope()->bci();
}

bool compiledVFrame::should_reexecute() const {
  if (scope() == NULL) {
    // native nmethods have no scope the method/bci is implied
    nmethod* nm = code()->as_nmethod();
    assert(nm->is_native_method(), "must be native");
    return false;
  }
  return scope()->should_reexecute();
}

bool compiledVFrame::has_ea_local_in_scope() const {
  if (scope() == NULL) {
    // native nmethod, all objs escape
    assert(code()->as_nmethod()->is_native_method(), "must be native");
    return false;
  }
  return (scope()->objects() != NULL) || scope()->has_ea_local_in_scope();
}

bool compiledVFrame::arg_escape() const {
  if (scope() == NULL) {
    // native nmethod, all objs escape
    assert(code()->as_nmethod()->is_native_method(), "must be native");
    return false;
  }
  return scope()->arg_escape();
}

vframe* compiledVFrame::sender() const {
  const frame f = fr();
  if (scope() == NULL) {
    // native nmethods have no scope the method/bci is implied
    nmethod* nm = code()->as_nmethod();
    assert(nm->is_native_method(), "must be native");
    return vframe::sender();
  } else {
    return scope()->is_top()
      ? vframe::sender()
      : new compiledVFrame(&f, register_map(), thread(), scope()->sender(), vframe_id() + 1);
  }
}

jvmtiDeferredLocalVariableSet::jvmtiDeferredLocalVariableSet(Method* method, int bci, intptr_t* id, int vframe_id) {
  _method = method;
  _bci = bci;
  _id = id;
  _vframe_id = vframe_id;
  // Alway will need at least one, must be on C heap
  _locals = new(ResourceObj::C_HEAP, mtCompiler) GrowableArray<jvmtiDeferredLocalVariable*> (1, mtCompiler);
  _objects_are_deoptimized = false;
}

jvmtiDeferredLocalVariableSet::~jvmtiDeferredLocalVariableSet() {
  for (int i = 0; i < _locals->length(); i++ ) {
    delete _locals->at(i);
  }
  // Free growableArray and c heap for elements
  delete _locals;
}

bool jvmtiDeferredLocalVariableSet::matches(const vframe* vf) {
  if (!vf->is_compiled_frame()) return false;
  compiledVFrame* cvf = (compiledVFrame*)vf;
  if (cvf->fr().id() == id() && cvf->vframe_id() == vframe_id()) {
    assert(cvf->method() == method() && cvf->bci() == bci(), "must agree");
    return true;
  }
  return false;
}

void jvmtiDeferredLocalVariableSet::set_value_at(int idx, BasicType type, jvalue val) {
  for (int i = 0; i < _locals->length(); i++) {
    if (_locals->at(i)->index() == idx) {
      assert(_locals->at(i)->type() == type, "Wrong type");
      _locals->at(i)->set_value(val);
      return;
    }
  }
  _locals->push(new jvmtiDeferredLocalVariable(idx, type, val));
}

void jvmtiDeferredLocalVariableSet::update_value(StackValueCollection* locals, BasicType type, int index, jvalue value) {
  switch (type) {
    case T_BOOLEAN:
      locals->set_int_at(index, value.z);
      break;
    case T_CHAR:
      locals->set_int_at(index, value.c);
      break;
    case T_FLOAT:
      locals->set_float_at(index, value.f);
      break;
    case T_DOUBLE:
      locals->set_double_at(index, value.d);
      break;
    case T_BYTE:
      locals->set_int_at(index, value.b);
      break;
    case T_SHORT:
      locals->set_int_at(index, value.s);
      break;
    case T_INT:
      locals->set_int_at(index, value.i);
      break;
    case T_LONG:
      locals->set_long_at(index, value.j);
      break;
    case T_OBJECT:
      {
        Handle obj(Thread::current(), cast_to_oop(value.l));
        locals->set_obj_at(index, obj);
      }
      break;
    default:
      ShouldNotReachHere();
  }
}

void jvmtiDeferredLocalVariableSet::update_locals(StackValueCollection* locals) {
  for (int l = 0; l < _locals->length(); l ++) {
    jvmtiDeferredLocalVariable* val = _locals->at(l);
    if (val->index() >= 0 && val->index() < method()->max_locals()) {
      update_value(locals, val->type(), val->index(), val->value());
    }
  }
}


void jvmtiDeferredLocalVariableSet::update_stack(StackValueCollection* expressions) {
  for (int l = 0; l < _locals->length(); l ++) {
    jvmtiDeferredLocalVariable* val = _locals->at(l);
    if (val->index() >= method()->max_locals() && val->index() < method()->max_locals() + method()->max_stack()) {
      update_value(expressions, val->type(), val->index() - method()->max_locals(), val->value());
    }
  }
}


void jvmtiDeferredLocalVariableSet::update_monitors(GrowableArray<MonitorInfo*>* monitors) {
  for (int l = 0; l < _locals->length(); l ++) {
    jvmtiDeferredLocalVariable* val = _locals->at(l);
    if (val->index() >= method()->max_locals() + method()->max_stack()) {
      int lock_index = val->index() - (method()->max_locals() + method()->max_stack());
      MonitorInfo* info = monitors->at(lock_index);
      // Originally the owner may have been scalar replaced but as an update
      // exists it must have been deoptimized, i.e. reallocated to the heap, and
      // now it is considered not to be scalar replaced.
      MonitorInfo* new_info = new MonitorInfo((oopDesc*)val->value().l, info->lock(),
                                              info->eliminated(), false);
      monitors->at_put(lock_index, new_info);
    }
  }
}


void jvmtiDeferredLocalVariableSet::oops_do(OopClosure* f) {
  // The Method* is on the stack so a live activation keeps it alive
  // either by mirror in interpreter or code in compiled code.
  for (int i = 0; i < _locals->length(); i++) {
    if (_locals->at(i)->type() == T_OBJECT) {
      f->do_oop(_locals->at(i)->oop_addr());
    }
  }
}

jvmtiDeferredLocalVariable::jvmtiDeferredLocalVariable(int index, BasicType type, jvalue value) {
  _index = index;
  _type = type;
  _value = value;
}


#ifndef PRODUCT
void compiledVFrame::verify() const {
  Unimplemented();
}
#endif // PRODUCT
