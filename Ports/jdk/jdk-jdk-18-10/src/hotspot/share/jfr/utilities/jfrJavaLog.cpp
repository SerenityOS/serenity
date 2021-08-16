/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/jni/jfrJavaSupport.hpp"
#include "jfr/utilities/jfrJavaLog.hpp"
#include "jfr/utilities/jfrLogTagSets.hpp"
#include "logging/log.hpp"
#include "logging/logConfiguration.hpp"
#include "logging/logMessage.hpp"
#include "memory/resourceArea.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "runtime/thread.inline.hpp"

#define JFR_LOG_TAGS_CONCATED(T0, T1, T2, T3, T4, T5, ...)  \
  T0 ## _ ## T1 ## _ ## T2 ## _ ## T3 ## _ ## T4 ## _ ## T5

enum JfrLogTagSetType {
#define JFR_LOG_TAG(...) \
    EXPAND_VARARGS(JFR_LOG_TAGS_CONCATED(__VA_ARGS__, _NO_TAG, _NO_TAG, _NO_TAG, _NO_TAG, _NO_TAG, _NO_TAG)),

    JFR_LOG_TAG_SET_LIST

#undef JFR_LOG_TAG
    JFR_LOG_TAG_SET_COUNT
};

struct jfrLogSubscriber
{
  jobject log_tag_enum_ref;
  LogTagSet* log_tag_set;
};

static jfrLogSubscriber log_tag_sets[JFR_LOG_TAG_SET_COUNT];

static void log_cfg_update(LogLevelType llt, JfrLogTagSetType jflt, TRAPS) {
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));
  if (log_tag_sets[jflt].log_tag_enum_ref == NULL) {
    return;
  }
  jobject lt = log_tag_sets[jflt].log_tag_enum_ref;
  // set field tagSetLevel to llt value
  JavaValue result(T_VOID);
  JfrJavaArguments args(&result);
  args.set_klass(JfrJavaSupport::klass(lt));
  args.set_name("tagSetLevel");
  args.set_signature("I");
  args.set_receiver(JfrJavaSupport::resolve_non_null(lt));
  args.push_int(llt);
  JfrJavaSupport::set_field(&args, THREAD);
}

static LogLevelType highest_level(const LogTagSet& lts) {
  for (size_t i = 0; i < LogLevel::Count; i++) {
    if (lts.is_level((LogLevelType)i)) {
      return (LogLevelType)i;
    }
  }
  return LogLevel::Off;
}

static void log_config_change_internal(bool init, TRAPS) {
  LogLevelType llt;
  LogTagSet* lts;

#define JFR_LOG_TAG(...) \
  lts = &LogTagSetMapping<LOG_TAGS(__VA_ARGS__)>::tagset(); \
  if (init) { \
    JfrLogTagSetType tagSetType = \
      EXPAND_VARARGS(JFR_LOG_TAGS_CONCATED(__VA_ARGS__, _NO_TAG, _NO_TAG, _NO_TAG, _NO_TAG, _NO_TAG, _NO_TAG)); \
    assert(NULL == log_tag_sets[tagSetType].log_tag_set, "Init JFR LogTagSets twice"); \
    log_tag_sets[tagSetType].log_tag_set = lts; \
  } \
  llt = highest_level(*lts); \
  log_cfg_update(llt, \
  EXPAND_VARARGS(JFR_LOG_TAGS_CONCATED(__VA_ARGS__, _NO_TAG, _NO_TAG, _NO_TAG, _NO_TAG, _NO_TAG, _NO_TAG)), THREAD);
  JFR_LOG_TAG_SET_LIST
#undef JFR_LOG_TAG
}

static void log_config_change() {
  JavaThread* t = JavaThread::current();
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(t));
  log_config_change_internal(false, t);
}

void JfrJavaLog::subscribe_log_level(jobject log_tag, jint id, TRAPS) {
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));
  static bool subscribed_updates = true;
  assert(id < JFR_LOG_TAG_SET_COUNT,
    "LogTag id, java and native not in synch, %d < %d", id, JFR_LOG_TAG_SET_COUNT);
  assert(NULL == log_tag_sets[id].log_tag_enum_ref, "Subscribing twice");
  log_tag_sets[id].log_tag_enum_ref = JfrJavaSupport::global_jni_handle(log_tag, THREAD);
  if (subscribed_updates) {
    LogConfiguration::register_update_listener(&log_config_change);
    log_config_change_internal(true, THREAD);
    subscribed_updates = false;
  } else {
    log_config_change_internal(false, THREAD);
  }
}

void JfrJavaLog::log_event(JNIEnv* env, jint level, jobjectArray lines, bool system, TRAPS) {
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));
  if (lines == NULL) {
    return;
  }
  if (level < (jint)LogLevel::First || level > (jint)LogLevel::Last) {
    JfrJavaSupport::throw_illegal_argument_exception("LogLevel passed is outside valid range", THREAD);
    return;
  }

  objArrayOop the_lines = objArrayOop(JfrJavaSupport::resolve_non_null(lines));
  assert(the_lines != NULL, "invariant");
  assert(the_lines->is_array(), "must be array");
  const int length = the_lines->length();

  ResourceMark rm(THREAD);
  LogMessage(jfr, event) jfr_event;
  LogMessage(jfr, system, event) jfr_event_system;
  for (int i = 0; i < length; ++i) {
    const char* text = JfrJavaSupport::c_str(the_lines->obj_at(i), THREAD);
    if (text == NULL) {
      // An oome has been thrown and is pending.
      return;
    }
    if (system) {
      jfr_event_system.write((LogLevelType)level, "%s", text);
    } else {
      jfr_event.write((LogLevelType)level, "%s", text);
    }
  }
}

void JfrJavaLog::log(jint tag_set, jint level, jstring message, TRAPS) {
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));
  if (message == NULL) {
    return;
  }
  if (level < (jint)LogLevel::First || level > (jint)LogLevel::Last) {
    JfrJavaSupport::throw_illegal_argument_exception("LogLevel passed is outside valid range", THREAD);
    return;
  }
  if (tag_set < 0 || tag_set >= (jint)JFR_LOG_TAG_SET_COUNT) {
    JfrJavaSupport::throw_illegal_argument_exception("LogTagSet id is outside valid range", THREAD);
    return;
  }
  ResourceMark rm(THREAD);
  const char* const s = JfrJavaSupport::c_str(message, CHECK);
  assert(s != NULL, "invariant");
  assert(log_tag_sets[tag_set].log_tag_set != NULL, "LogTagSet is not init");
  log_tag_sets[tag_set].log_tag_set->log((LogLevelType)level, s);
}
