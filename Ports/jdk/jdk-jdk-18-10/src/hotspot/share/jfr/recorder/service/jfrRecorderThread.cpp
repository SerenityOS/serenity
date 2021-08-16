/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/javaClasses.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "jfr/jfr.hpp"
#include "jfr/jni/jfrJavaSupport.hpp"
#include "jfr/recorder/jfrRecorder.hpp"
#include "jfr/recorder/checkpoint/jfrCheckpointManager.hpp"
#include "jfr/recorder/service/jfrRecorderThread.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/thread.inline.hpp"
#include "utilities/preserveException.hpp"
#include "utilities/macros.hpp"

static Thread* start_thread(instanceHandle thread_oop, ThreadFunction proc, TRAPS) {
  assert(thread_oop.not_null(), "invariant");
  assert(proc != NULL, "invariant");

  JavaThread* new_thread = new JavaThread(proc);

  // At this point it may be possible that no
  // osthread was created for the JavaThread due to lack of resources.
  if (new_thread->osthread() == NULL) {
    delete new_thread;
    JfrJavaSupport::throw_out_of_memory_error("Unable to create native recording thread for JFR", THREAD);
    return NULL;
  } else {
    JavaThread::start_internal_daemon(THREAD, new_thread, thread_oop, NormPriority);
    return new_thread;
  }
}

JfrPostBox* JfrRecorderThread::_post_box = NULL;

JfrPostBox& JfrRecorderThread::post_box() {
  return *_post_box;
}

// defined in JfrRecorderThreadLoop.cpp
void recorderthread_entry(JavaThread*, JavaThread*);

bool JfrRecorderThread::start(JfrCheckpointManager* cp_manager, JfrPostBox* post_box, TRAPS) {
  assert(cp_manager != NULL, "invariant");
  assert(post_box != NULL, "invariant");
  _post_box = post_box;

  static const char klass[] = "jdk/jfr/internal/JVMUpcalls";
  static const char method[] = "createRecorderThread";
  static const char signature[] = "(Ljava/lang/ThreadGroup;Ljava/lang/ClassLoader;)Ljava/lang/Thread;";

  JavaValue result(T_OBJECT);
  JfrJavaArguments create_thread_args(&result, klass, method, signature, CHECK_false);

  // arguments
  create_thread_args.push_oop(Universe::system_thread_group());
  create_thread_args.push_oop(SystemDictionary::java_system_loader());

  JfrJavaSupport::call_static(&create_thread_args, CHECK_false);
  instanceHandle h_thread_oop(THREAD, (instanceOop)result.get_oop());
  assert(h_thread_oop.not_null(), "invariant");
  // attempt thread start
  Thread* const t = start_thread(h_thread_oop, recorderthread_entry,THREAD);
  if (!HAS_PENDING_EXCEPTION) {
    Jfr::exclude_thread(t);
    return true;
  }
  assert(HAS_PENDING_EXCEPTION, "invariant");
  // Start failed, remove the thread from the system thread group
  JavaValue void_result(T_VOID);
  JfrJavaArguments remove_thread_args(&void_result);
  remove_thread_args.set_klass(vmClasses::ThreadGroup_klass());
  remove_thread_args.set_name(vmSymbols::remove_method_name());
  remove_thread_args.set_signature(vmSymbols::thread_void_signature());
  remove_thread_args.set_receiver(Universe::system_thread_group());
  remove_thread_args.push_oop(h_thread_oop());
  PreserveExceptionMark cpe(THREAD);
  JfrJavaSupport::call_special(&remove_thread_args, THREAD);
  return false;
}
