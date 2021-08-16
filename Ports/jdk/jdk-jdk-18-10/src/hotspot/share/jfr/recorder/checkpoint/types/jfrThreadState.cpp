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
#include "classfile/javaClasses.inline.hpp"
#include "jfr/recorder/checkpoint/types/jfrThreadState.hpp"
#include "jfr/recorder/checkpoint/jfrCheckpointWriter.hpp"
#include "jfr/support/jfrThreadLocal.hpp"
#include "jvmtifiles/jvmti.h"
#include "runtime/osThread.hpp"
#include "runtime/thread.hpp"

struct jvmti_thread_state {
  u8 id;
  const char* description;
};

static jvmti_thread_state states[] = {
  {
    JVMTI_JAVA_LANG_THREAD_STATE_NEW,
    "STATE_NEW"
  },
  {
    JVMTI_THREAD_STATE_TERMINATED,
    "STATE_TERMINATED"
  },
  {
    JVMTI_JAVA_LANG_THREAD_STATE_RUNNABLE,
    "STATE_RUNNABLE"
  },
  {
    (JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT | JVMTI_THREAD_STATE_SLEEPING),
    "STATE_SLEEPING"
  },
  {
    (JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_WAITING_INDEFINITELY | JVMTI_THREAD_STATE_IN_OBJECT_WAIT),
    "STATE_IN_OBJECT_WAIT"
  },
  {
    (JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT | JVMTI_THREAD_STATE_IN_OBJECT_WAIT),
    "STATE_IN_OBJECT_WAIT_TIMED"
  },
  {
    (JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_WAITING_INDEFINITELY | JVMTI_THREAD_STATE_PARKED),
    "STATE_PARKED"
  },
  {
    (JVMTI_THREAD_STATE_ALIVE | JVMTI_THREAD_STATE_WAITING | JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT | JVMTI_THREAD_STATE_PARKED),
    "STATE_PARKED_TIMED"
  },
  {
    JVMTI_JAVA_LANG_THREAD_STATE_BLOCKED,
    "STATE_BLOCKED_ON_MONITOR_ENTER"
  }
};

void JfrThreadState::serialize(JfrCheckpointWriter& writer) {
  const u4 number_of_states = sizeof(states) / sizeof(jvmti_thread_state);
  writer.write_count(number_of_states);
  for (u4 i = 0; i < number_of_states; ++i) {
    writer.write_key(states[i].id);
    writer.write(states[i].description);
  }
}

traceid JfrThreadId::id(const Thread* t) {
  assert(t != NULL, "invariant");
  if (!t->is_Java_thread()) {
    return os_id(t);
  }
  const oop thread_obj = JavaThread::cast(t)->threadObj();
  return thread_obj != NULL ? java_lang_Thread::thread_id(thread_obj) : 0;
}

traceid JfrThreadId::os_id(const Thread* t) {
  assert(t != NULL, "invariant");
  const OSThread* const os_thread = t->osthread();
  return os_thread != NULL ? os_thread->thread_id() : 0;
}

traceid JfrThreadId::jfr_id(const Thread* t) {
  assert(t != NULL, "invariant");
  return t->jfr_thread_local()->thread_id();
}

// caller needs ResourceMark
const char* get_java_thread_name(const JavaThread* jt) {
  assert(jt != NULL, "invariant");
  const char* name_str = "<no-name - thread name unresolved>";
  const oop thread_obj = jt->threadObj();
  if (thread_obj != NULL) {
    const oop name = java_lang_Thread::name(thread_obj);
    if (name != NULL) {
      name_str = java_lang_String::as_utf8_string(name);
    }
  } else if (jt->is_attaching_via_jni()) {
    name_str = "<no-name - thread is attaching>";
  }
  assert(name_str != NULL, "unexpected NULL thread name");
  return name_str;
}

const char* JfrThreadName::name(const Thread* t) {
  assert(t != NULL, "invariant");
  return t->is_Java_thread() ? get_java_thread_name(JavaThread::cast(t)) : t->name();
}
