/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jni.h"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmSymbols.hpp"
#include "jfr/jni/jfrJavaSupport.hpp"
#include "jfr/recorder/storage/jfrStorage.hpp"
#include "jfr/support/jfrThreadId.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "jfr/writers/jfrJavaEventWriter.hpp"
#include "memory/iterator.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/thread.inline.hpp"

static int start_pos_offset = invalid_offset;
static int start_pos_address_offset = invalid_offset;
static int current_pos_offset = invalid_offset;
static int max_pos_offset = invalid_offset;
static int notified_offset = invalid_offset;
static int thread_id_offset = invalid_offset;
static int valid_offset = invalid_offset;

static bool find_field(InstanceKlass* ik,
                       Symbol* name_symbol,
                       Symbol* signature_symbol,
                       fieldDescriptor* fd,
                       bool is_static = false,
                       bool allow_super = false) {
  if (allow_super || is_static) {
    return ik->find_field(name_symbol, signature_symbol, is_static, fd) != NULL;
  } else {
    return ik->find_local_field(name_symbol, signature_symbol, fd);
  }
}

static void compute_offset(int &dest_offset,
                           Klass* klass,
                           Symbol* name_symbol,
                           Symbol* signature_symbol,
                           bool is_static = false, bool allow_super = false) {
  fieldDescriptor fd;
  InstanceKlass* ik = InstanceKlass::cast(klass);
  if (!find_field(ik, name_symbol, signature_symbol, &fd, is_static, allow_super)) {
    assert(false, "invariant");
  }
  dest_offset = fd.offset();
}

static bool setup_event_writer_offsets(TRAPS) {
  const char class_name[] = "jdk/jfr/internal/EventWriter";
  Symbol* const k_sym = SymbolTable::new_symbol(class_name);
  assert(k_sym != NULL, "invariant");
  Klass* klass = SystemDictionary::resolve_or_fail(k_sym, true, CHECK_false);
  assert(klass != NULL, "invariant");

  const char start_pos_name[] = "startPosition";
  Symbol* const start_pos_sym = SymbolTable::new_symbol(start_pos_name);
  assert(start_pos_sym != NULL, "invariant");
  assert(invalid_offset == start_pos_offset, "invariant");
  compute_offset(start_pos_offset, klass, start_pos_sym, vmSymbols::long_signature());
  assert(start_pos_offset != invalid_offset, "invariant");

  const char start_pos_address_name[] = "startPositionAddress";
  Symbol* const start_pos_address_sym = SymbolTable::new_symbol(start_pos_address_name);
  assert(start_pos_address_sym != NULL, "invariant");
  assert(invalid_offset == start_pos_address_offset, "invariant");
  compute_offset(start_pos_address_offset, klass, start_pos_address_sym, vmSymbols::long_signature());
  assert(start_pos_address_offset != invalid_offset, "invariant");

  const char event_pos_name[] = "currentPosition";
  Symbol* const event_pos_sym = SymbolTable::new_symbol(event_pos_name);
  assert(event_pos_sym != NULL, "invariant");
  assert(invalid_offset == current_pos_offset, "invariant");
  compute_offset(current_pos_offset, klass, event_pos_sym,vmSymbols::long_signature());
  assert(current_pos_offset != invalid_offset, "invariant");

  const char max_pos_name[] = "maxPosition";
  Symbol* const max_pos_sym = SymbolTable::new_symbol(max_pos_name);
  assert(max_pos_sym != NULL, "invariant");
  assert(invalid_offset == max_pos_offset, "invariant");
  compute_offset(max_pos_offset, klass, max_pos_sym, vmSymbols::long_signature());
  assert(max_pos_offset != invalid_offset, "invariant");

  const char notified_name[] = "notified";
  Symbol* const notified_sym = SymbolTable::new_symbol(notified_name);
  assert (notified_sym != NULL, "invariant");
  assert(invalid_offset == notified_offset, "invariant");
  compute_offset(notified_offset, klass, notified_sym, vmSymbols::bool_signature());
  assert(notified_offset != invalid_offset, "invariant");

  const char valid_name[] = "valid";
  Symbol* const valid_sym = SymbolTable::new_symbol(valid_name);
  assert (valid_sym != NULL, "invariant");
  assert(invalid_offset == valid_offset, "invariant");
  compute_offset(valid_offset, klass, valid_sym, vmSymbols::bool_signature());
  assert(valid_offset != invalid_offset, "invariant");
  return true;
}

bool JfrJavaEventWriter::initialize() {
  static bool initialized = false;
  if (!initialized) {
    initialized = setup_event_writer_offsets(JavaThread::current());
  }
  return initialized;
}

jboolean JfrJavaEventWriter::flush(jobject writer, jint used, jint requested, JavaThread* jt) {
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(jt));
  assert(writer != NULL, "invariant");
  oop const w = JNIHandles::resolve_non_null(writer);
  assert(w != NULL, "invariant");
  JfrBuffer* const current = jt->jfr_thread_local()->java_buffer();
  assert(current != NULL, "invariant");
  JfrBuffer* const buffer = JfrStorage::flush(current, used, requested, false, jt);
  assert(buffer != NULL, "invariant");
  // "validity" is contextually defined here to mean
  // that some memory location was provided that is
  // large enough to accommodate the "requested size".
  const bool is_valid = buffer->free_size() >= (size_t)(used + requested);
  u1* const new_current_position = is_valid ? buffer->pos() + used : buffer->pos();
  assert(start_pos_offset != invalid_offset, "invariant");
  w->long_field_put(start_pos_offset, (jlong)buffer->pos());
  w->long_field_put(current_pos_offset, (jlong)new_current_position);
  // only update java writer if underlying memory changed
  if (buffer != current) {
    w->long_field_put(start_pos_address_offset, (jlong)buffer->pos_address());
    w->long_field_put(max_pos_offset, (jlong)buffer->end());
  }
  if (!is_valid) {
    // mark writer as invalid for this write attempt
    w->release_bool_field_put(valid_offset, JNI_FALSE);
    return JNI_FALSE;
  }
  // An exclusive use of a leased buffer is treated equivalent to
  // holding a system resource. As such, it should be released as soon as possible.
  // Returning true here signals that the thread will need to call flush again
  // on EventWriter.endEvent() and that flush will return the lease.
  return buffer->lease() ? JNI_TRUE : JNI_FALSE;
}

class JfrJavaEventWriterNotificationClosure : public ThreadClosure {
 public:
   void do_thread(Thread* t) {
     if (t->is_Java_thread()) {
       JfrJavaEventWriter::notify(JavaThread::cast(t));
     }
   }
};

void JfrJavaEventWriter::notify() {
  assert(SafepointSynchronize::is_at_safepoint(), "invariant");
  JfrJavaEventWriterNotificationClosure closure;
  Threads::threads_do(&closure);
}

void JfrJavaEventWriter::notify(JavaThread* jt) {
  assert(jt != NULL, "invariant");
  assert(SafepointSynchronize::is_at_safepoint(), "invariant");
  if (jt->jfr_thread_local()->has_java_event_writer()) {
    oop buffer_writer = JNIHandles::resolve_non_null(jt->jfr_thread_local()->java_event_writer());
    assert(buffer_writer != NULL, "invariant");
    buffer_writer->release_bool_field_put(notified_offset, JNI_TRUE);
  }
}

static jobject create_new_event_writer(JfrBuffer* buffer, TRAPS) {
  assert(buffer != NULL, "invariant");
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));
  HandleMark hm(THREAD);
  static const char klass[] = "jdk/jfr/internal/EventWriter";
  static const char method[] = "<init>";
  static const char signature[] = "(JJJJZ)V";
  JavaValue result(T_OBJECT);
  JfrJavaArguments args(&result, klass, method, signature, CHECK_NULL);
  // parameters
  args.push_long((jlong)buffer->pos());
  args.push_long((jlong)buffer->end());
  args.push_long((jlong)buffer->pos_address());
  args.push_long((jlong)JFR_THREAD_ID(THREAD));
  args.push_int((int)JNI_TRUE);
  JfrJavaSupport::new_object_global_ref(&args, CHECK_NULL);
  return result.get_jobject();
}

jobject JfrJavaEventWriter::event_writer(JavaThread* t) {
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(t));
  JfrThreadLocal* const tl = t->jfr_thread_local();
  assert(tl->shelved_buffer() == NULL, "invariant");
  return tl->java_event_writer();
}

jobject JfrJavaEventWriter::new_event_writer(TRAPS) {
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));
  assert(event_writer(THREAD) == NULL, "invariant");
  JfrThreadLocal* const tl = THREAD->jfr_thread_local();
  assert(!tl->has_java_buffer(), "invariant");
  JfrBuffer* const buffer = tl->java_buffer();
  if (buffer == NULL) {
    JfrJavaSupport::throw_out_of_memory_error("OOME for thread local buffer", THREAD);
    return NULL;
  }
  jobject java_event_writer = create_new_event_writer(buffer, CHECK_NULL);
  tl->set_java_event_writer(java_event_writer);
  assert(tl->has_java_event_writer(), "invariant");
  return java_event_writer;
}
