/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciCallSite.hpp"
#include "ci/ciConstant.hpp"
#include "ci/ciField.hpp"
#include "ci/ciStreams.hpp"
#include "ci/ciSymbols.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "runtime/handles.inline.hpp"

// ciExceptionHandlerStream
//
// Walk over some selected set of a methods exception handlers.

// ------------------------------------------------------------------
// ciExceptionHandlerStream::count
//
// How many exception handlers are there in this stream?
//
// Implementation note: Compiler2 needs this functionality, so I had
int ciExceptionHandlerStream::count() {
  int save_pos = _pos;
  int save_end = _end;

  int count = 0;

  _pos = -1;
  _end = _method->_handler_count;


  next();
  while (!is_done()) {
    count++;
    next();
  }

  _pos = save_pos;
  _end = save_end;

  return count;
}

int ciExceptionHandlerStream::count_remaining() {
  int save_pos = _pos;
  int save_end = _end;

  int count = 0;

  while (!is_done()) {
    count++;
    next();
  }

  _pos = save_pos;
  _end = save_end;

  return count;
}

// ciBytecodeStream
//
// The class is used to iterate over the bytecodes of a method.
// It hides the details of constant pool structure/access by
// providing accessors for constant pool items.

// ------------------------------------------------------------------
// ciBytecodeStream::next_wide_or_table
//
// Special handling for switch ops
Bytecodes::Code ciBytecodeStream::next_wide_or_table(Bytecodes::Code bc) {
  switch (bc) {                // Check for special bytecode handling
  case Bytecodes::_wide:
    // Special handling for the wide bytcode
    // Get following bytecode; do not return wide
    assert(Bytecodes::Code(_pc[0]) == Bytecodes::_wide, "");
    bc = Bytecodes::java_code(_raw_bc = (Bytecodes::Code)_pc[1]);
    assert(Bytecodes::wide_length_for(bc) > 2, "must make progress");
    _pc += Bytecodes::wide_length_for(bc);
    _was_wide = _pc;              // Flag last wide bytecode found
    assert(is_wide(), "accessor works right");
    break;

  case Bytecodes::_lookupswitch:
    _pc++;                      // Skip wide bytecode
    _pc += (_start-_pc)&3;      // Word align
    _table_base = (jint*)_pc;   // Capture for later usage
                                // table_base[0] is default far_dest
    // Table has 2 lead elements (default, length), then pairs of u4 values.
    // So load table length, and compute address at end of table
    _pc = (address)&_table_base[2+ 2*Bytes::get_Java_u4((address)&_table_base[1])];
    break;

  case Bytecodes::_tableswitch: {
    _pc++;                      // Skip wide bytecode
    _pc += (_start-_pc)&3;      // Word align
    _table_base = (jint*)_pc;   // Capture for later usage
                                // table_base[0] is default far_dest
    int lo = Bytes::get_Java_u4((address)&_table_base[1]);// Low bound
    int hi = Bytes::get_Java_u4((address)&_table_base[2]);// High bound
    int len = hi - lo + 1;      // Dense table size
    _pc = (address)&_table_base[3+len]; // Skip past table
    break;
  }

  default:
    fatal("unhandled bytecode");
  }
  return bc;
}

// ------------------------------------------------------------------
// ciBytecodeStream::reset_to_bci
void ciBytecodeStream::reset_to_bci( int bci ) {
  _bc_start=_was_wide=0;
  _pc = _start+bci;
}

// ------------------------------------------------------------------
// ciBytecodeStream::force_bci
void ciBytecodeStream::force_bci(int bci) {
  if (bci < 0) {
    reset_to_bci(0);
    _bc_start = _start + bci;
    _bc = EOBC();
  } else {
    reset_to_bci(bci);
    next();
  }
}


// ------------------------------------------------------------------
// Constant pool access
// ------------------------------------------------------------------

// ------------------------------------------------------------------
// ciBytecodeStream::get_klass_index
//
// If this bytecodes references a klass, return the index of the
// referenced klass.
int ciBytecodeStream::get_klass_index() const {
  switch(cur_bc()) {
  case Bytecodes::_ldc:
    return get_index_u1();
  case Bytecodes::_ldc_w:
  case Bytecodes::_ldc2_w:
  case Bytecodes::_checkcast:
  case Bytecodes::_instanceof:
  case Bytecodes::_anewarray:
  case Bytecodes::_multianewarray:
  case Bytecodes::_new:
  case Bytecodes::_newarray:
    return get_index_u2();
  default:
    ShouldNotReachHere();
    return 0;
  }
}

// ------------------------------------------------------------------
// ciBytecodeStream::get_klass
//
// If this bytecode is a new, newarray, multianewarray, instanceof,
// or checkcast, get the referenced klass.
ciKlass* ciBytecodeStream::get_klass(bool& will_link) {
  VM_ENTRY_MARK;
  constantPoolHandle cpool(THREAD, _method->get_Method()->constants());
  return CURRENT_ENV->get_klass_by_index(cpool, get_klass_index(), will_link, _holder);
}

// ------------------------------------------------------------------
// ciBytecodeStream::get_constant_raw_index
//
// If this bytecode is one of the ldc variants, get the index of the
// referenced constant.
int ciBytecodeStream::get_constant_raw_index() const {
  // work-alike for Bytecode_loadconstant::raw_index()
  switch (cur_bc()) {
  case Bytecodes::_ldc:
    return get_index_u1();
  case Bytecodes::_ldc_w:
  case Bytecodes::_ldc2_w:
    return get_index_u2();
  default:
    ShouldNotReachHere();
    return 0;
  }
}

// ------------------------------------------------------------------
// ciBytecodeStream::get_constant_pool_index
// Decode any reference index into a regular pool index.
int ciBytecodeStream::get_constant_pool_index() const {
  // work-alike for Bytecode_loadconstant::pool_index()
  int index = get_constant_raw_index();
  if (has_cache_index()) {
    VM_ENTRY_MARK;
    constantPoolHandle cpool(THREAD, _method->get_Method()->constants());
    return cpool->object_to_cp_index(index);
  }
  return index;
}

// ------------------------------------------------------------------
// ciBytecodeStream::get_constant
//
// If this bytecode is one of the ldc variants, get the referenced
// constant.
ciConstant ciBytecodeStream::get_constant() {
  int pool_index = get_constant_raw_index();
  int cache_index = -1;
  if (has_cache_index()) {
    cache_index = pool_index;
    pool_index = -1;
  }
  VM_ENTRY_MARK;
  constantPoolHandle cpool(THREAD, _method->get_Method()->constants());
  return CURRENT_ENV->get_constant_by_index(cpool, pool_index, cache_index, _holder);
}

// ------------------------------------------------------------------
// ciBytecodeStream::get_constant_pool_tag
//
// If this bytecode is one of the ldc variants, get the referenced
// constant.
constantTag ciBytecodeStream::get_constant_pool_tag(int index) const {
  VM_ENTRY_MARK;
  return _method->get_Method()->constants()->constant_tag_at(index);
}

// ------------------------------------------------------------------
// ciBytecodeStream::get_field_index
//
// If this is a field access bytecode, get the constant pool
// index of the referenced field.
int ciBytecodeStream::get_field_index() {
  assert(cur_bc() == Bytecodes::_getfield ||
         cur_bc() == Bytecodes::_putfield ||
         cur_bc() == Bytecodes::_getstatic ||
         cur_bc() == Bytecodes::_putstatic, "wrong bc");
  return get_index_u2_cpcache();
}


// ------------------------------------------------------------------
// ciBytecodeStream::get_field
//
// If this bytecode is one of get_field, get_static, put_field,
// or put_static, get the referenced field.
ciField* ciBytecodeStream::get_field(bool& will_link) {
  ciField* f = CURRENT_ENV->get_field_by_index(_holder, get_field_index());
  will_link = f->will_link(_method, _bc);
  return f;
}


// ------------------------------------------------------------------
// ciBytecodeStream::get_declared_field_holder
//
// Get the declared holder of the currently referenced field.
//
// Usage note: the holder() of a ciField class returns the canonical
// holder of the field, rather than the holder declared in the
// bytecodes.
//
// There is no "will_link" result passed back.  The user is responsible
// for checking linkability when retrieving the associated field.
ciInstanceKlass* ciBytecodeStream::get_declared_field_holder() {
  VM_ENTRY_MARK;
  constantPoolHandle cpool(THREAD, _method->get_Method()->constants());
  int holder_index = get_field_holder_index();
  bool ignore;
  return CURRENT_ENV->get_klass_by_index(cpool, holder_index, ignore, _holder)
      ->as_instance_klass();
}

// ------------------------------------------------------------------
// ciBytecodeStream::get_field_holder_index
//
// Get the constant pool index of the declared holder of the field
// referenced by the current bytecode.  Used for generating
// deoptimization information.
int ciBytecodeStream::get_field_holder_index() {
  GUARDED_VM_ENTRY(
    ConstantPool* cpool = _holder->get_instanceKlass()->constants();
    return cpool->klass_ref_index_at(get_field_index());
  )
}

// ------------------------------------------------------------------
// ciBytecodeStream::get_method_index
//
// If this is a method invocation bytecode, get the constant pool
// index of the invoked method.
int ciBytecodeStream::get_method_index() {
  assert(Bytecodes::is_invoke(cur_bc()), "invalid bytecode: %s", Bytecodes::name(cur_bc()));
  if (has_index_u4())
    return get_index_u4();  // invokedynamic
  return get_index_u2_cpcache();
}

// ------------------------------------------------------------------
// ciBytecodeStream::get_method
//
// If this is a method invocation bytecode, get the invoked method.
// Additionally return the declared signature to get more concrete
// type information if required (Cf. invokedynamic and invokehandle).
ciMethod* ciBytecodeStream::get_method(bool& will_link, ciSignature* *declared_signature_result) {
  VM_ENTRY_MARK;
  ciEnv* env = CURRENT_ENV;
  constantPoolHandle cpool(THREAD, _method->get_Method()->constants());
  ciMethod* m = env->get_method_by_index(cpool, get_method_index(), cur_bc(), _holder);
  will_link = m->is_loaded();

  // Use the signature stored in the CP cache to create a signature
  // with correct types (in respect to class loaders).
  //
  // In classic Java (before Java 7) there is never the slightest
  // difference between the signature at the call site and that of the
  // method.  Such a difference would have been a type error in the
  // JVM.
  //
  // Now there are a few circumstances where the signature of a call
  // site (which controls the outgoing stacked arguments) can differ
  // from the signature of the method (which controls the receipt of
  // those arguments at the method entry point).
  //
  // A. The signatures can differ if the callee is a static method and
  // the caller thinks it is calling a non-static method (VH.get).
  // This requires the method signature to have an explicit leading
  // argument for the implicit 'this', not present at the call site.
  //
  // B. The call site can have less specific parameter types than the
  // method, allowing loosely-typed code to handle strongly-typed
  // methods.  This happens with linkToStatic and related linker
  // commands.  Obviously the loosely-typed code has to ensure that
  // the strongly typed method's invariants are respected, and this is
  // done by issuing dynamic casts.
  //
  // C. The call site can have more specific parameter types than the
  // method, allowing loosely-typed methods to handle strongly-typed
  // requests.
  //
  // D. There are corresponding effects with return values, such as
  // boolean method returning an int to an int-receiving call site,
  // even though the method thought it returned just a boolean.
  //
  // E. The calling sequence at a particular call site may add an
  // "appendix" argument not mentioned in the call site signature.  It
  // is expected by the method signature, though, and this adds to the
  // method's arity, even after 'this' parameter effects (A) are
  // discounted.  Appendixes are used by invokehandle and
  // invokedynamic instructions.
  //
  // F. A linker method (linkToStatic, etc.) can also take an extra
  // argument, a MemberName which routes the call to a concrete
  // strongly-typed method.  In this case the linker method may also
  // differ in any of the ways A-D.  The eventual method will ignore
  // the presence of the extra argument.
  //
  // None of these changes to calling sequences requires an argument
  // to be moved or reformatted in any way.  This works because all
  // references look alike to the JVM, as do all primitives (except
  // float/long/double).  Another required property of the JVM is
  // that, if a trailing argument is added or dropped, the placement
  // of other arguments does not change.  This allows cases E and F to
  // work smoothly, against without any moving or reformatting,
  // despite the arity change.
  //
  if (has_local_signature()) {
    Symbol* local_signature = cpool->symbol_at(get_method_signature_index(cpool));
    ciSymbol* sig_sym  = env->get_symbol(local_signature);
    ciKlass* pool_holder = env->get_klass(cpool->pool_holder());
    ciSignature* call_site_sig = new (env->arena()) ciSignature(pool_holder, cpool, sig_sym);
    // Examples of how the call site signature can differ from the method's own signature:
    //
    //  meth = static jboolean java.lang.invoke.VarHandleGuards.guard_LII_Z(jobject, jobject, jint, jint, jobject)
    //  msig = (Ljava/lang/invoke/VarHandle;Ljava/lang/Object;IILjava/lang/invoke/VarHandle$AccessDescriptor;)Z
    //  call = (Ljava/util/concurrent/locks/AbstractQueuedSynchronizer;II)Z
    //
    //  meth = static jobject java.lang.invoke.LambdaForm$MH/0x0000000800066840.linkToTargetMethod(jobject, jobject)
    //  msig = (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //  call = (Ljava/lang/String;)Ljava/util/function/Predicate;
    //
    (*declared_signature_result) = call_site_sig;

  } else {
    // We can just use the method's own signature.  It may differ from the call site, but not by much.
    //
    // Examples of how the call site signature can differ from the method's signature:
    //
    // meth = static final native jint java.lang.invoke.MethodHandle.linkToStatic(jobject, jobject, jint, jint, jobject)
    // msig = (Ljava/lang/Object;Ljava/lang/Object;IILjava/lang/invoke/MemberName;)I
    // call = (Ljava/lang/invoke/VarHandle;Ljava/lang/Object;IILjava/lang/invoke/MemberName;)Z
    //
    // meth = final native jint java.lang.invoke.MethodHandle.invokeBasic(jobject, jobject, jint, jint)
    // msig = (Ljava/lang/Object;Ljava/lang/Object;II)I
    // call = (Ljava/lang/invoke/VarHandle;Ljava/lang/Object;II)Z
    //
    (*declared_signature_result) = m->signature();
  }
  return m;
}

// ------------------------------------------------------------------
// ciBytecodeStream::has_appendix
//
// Returns true if there is an appendix argument stored in the
// constant pool cache at the current bci.
bool ciBytecodeStream::has_appendix() {
  VM_ENTRY_MARK;
  constantPoolHandle cpool(THREAD, _method->get_Method()->constants());
  return ConstantPool::has_appendix_at_if_loaded(cpool, get_method_index());
}

// ------------------------------------------------------------------
// ciBytecodeStream::get_appendix
//
// Return the appendix argument stored in the constant pool cache at
// the current bci.
ciObject* ciBytecodeStream::get_appendix() {
  VM_ENTRY_MARK;
  constantPoolHandle cpool(THREAD, _method->get_Method()->constants());
  oop appendix_oop = ConstantPool::appendix_at_if_loaded(cpool, get_method_index());
  return CURRENT_ENV->get_object(appendix_oop);
}

// ------------------------------------------------------------------
// ciBytecodeStream::has_local_signature
//
// Returns true if the method stored in the constant
// pool cache at the current bci has a local signature.
bool ciBytecodeStream::has_local_signature() {
  GUARDED_VM_ENTRY(
    constantPoolHandle cpool(Thread::current(), _method->get_Method()->constants());
    return ConstantPool::has_local_signature_at_if_loaded(cpool, get_method_index());
  )
}

// ------------------------------------------------------------------
// ciBytecodeStream::get_declared_method_holder
//
// Get the declared holder of the currently referenced method.
//
// Usage note: the holder() of a ciMethod class returns the canonical
// holder of the method, rather than the holder declared in the
// bytecodes.
//
// There is no "will_link" result passed back.  The user is responsible
// for checking linkability when retrieving the associated method.
ciKlass* ciBytecodeStream::get_declared_method_holder() {
  VM_ENTRY_MARK;
  constantPoolHandle cpool(THREAD, _method->get_Method()->constants());
  bool ignore;
  // report as MethodHandle for invokedynamic, which is syntactically classless
  if (cur_bc() == Bytecodes::_invokedynamic)
    return CURRENT_ENV->get_klass_by_name(_holder, ciSymbols::java_lang_invoke_MethodHandle(), false);
  return CURRENT_ENV->get_klass_by_index(cpool, get_method_holder_index(), ignore, _holder);
}

// ------------------------------------------------------------------
// ciBytecodeStream::get_method_holder_index
//
// Get the constant pool index of the declared holder of the method
// referenced by the current bytecode.  Used for generating
// deoptimization information.
int ciBytecodeStream::get_method_holder_index() {
  ConstantPool* cpool = _method->get_Method()->constants();
  return cpool->klass_ref_index_at(get_method_index());
}

// ------------------------------------------------------------------
// ciBytecodeStream::get_method_signature_index
//
// Get the constant pool index of the signature of the method
// referenced by the current bytecode.  Used for generating
// deoptimization information.
int ciBytecodeStream::get_method_signature_index(const constantPoolHandle& cpool) {
  GUARDED_VM_ENTRY(
    const int method_index = get_method_index();
    const int name_and_type_index = cpool->name_and_type_ref_index_at(method_index);
    return cpool->signature_ref_index_at(name_and_type_index);
  )
}

