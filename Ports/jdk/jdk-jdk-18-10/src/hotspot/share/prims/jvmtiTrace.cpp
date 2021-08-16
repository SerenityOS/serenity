/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvmtifiles/jvmtiEnv.hpp"
#include "logging/log.hpp"
#include "logging/logConfiguration.hpp"
#include "memory/resourceArea.hpp"
#include "prims/jvmtiTrace.hpp"
#include "runtime/thread.inline.hpp"

//
// class JvmtiTrace
//
// Support for JVMTI tracing code
//
// ------------
// Usage:
//    -XX:TraceJVMTI=DESC,DESC,DESC
//
//    DESC is   DOMAIN ACTION KIND
//
//    DOMAIN is function name
//              event name
//              "all" (all functions and events)
//              "func" (all functions except boring)
//              "allfunc" (all functions)
//              "event" (all events)
//              "ec" (event controller)
//
//    ACTION is "+" (add)
//              "-" (remove)
//
//    KIND is
//     for func
//              "i" (input params)
//              "e" (error returns)
//              "o" (output)
//     for event
//              "t" (event triggered aka posted)
//              "s" (event sent)
//
// Example:
//            -XX:TraceJVMTI=ec+,GetCallerFrame+ie,Breakpoint+s

#ifdef JVMTI_TRACE

bool JvmtiTrace::_initialized = false;
bool JvmtiTrace::_on = false;
bool JvmtiTrace::_trace_event_controller = false;

void JvmtiTrace::initialize() {
  if (_initialized) {
    return;
  }
  SafeResourceMark rm;

  const char *very_end;
  const char *curr;
  if (TraceJVMTI != NULL) {
    curr = TraceJVMTI;
  } else {
    curr = "";  // hack in fixed tracing here
  }

  // Enable UL for JVMTI tracing
  if (strlen(curr) > 0) {
    if (!log_is_enabled(Trace, jvmti)) {
      log_warning(arguments)("-XX:+TraceJVMTI specified, "
         "but no log output configured for the 'jvmti' tag on Trace level. "
         "Defaulting to -Xlog:jvmti=trace");
      LogConfiguration::configure_stdout(LogLevel::Trace, true, LOG_TAGS(jvmti));
    }
  }

  very_end = curr + strlen(curr);
  while (curr < very_end) {
    const char *curr_end = strchr(curr, ',');
    if (curr_end == NULL) {
      curr_end = very_end;
    }
    const char *op_pos = strchr(curr, '+');
    const char *minus_pos = strchr(curr, '-');
    if (minus_pos != NULL && (minus_pos < op_pos || op_pos == NULL)) {
      op_pos = minus_pos;
    }
    char op;
    const char *flags = op_pos + 1;
    const char *flags_end = curr_end;
    if (op_pos == NULL || op_pos > curr_end) {
      flags = "ies";
      flags_end = flags + strlen(flags);
      op_pos = curr_end;
      op = '+';
    } else {
      op = *op_pos;
    }
    jbyte bits = 0;
    for (; flags < flags_end; ++flags) {
      switch (*flags) {
      case 'i':
        bits |= SHOW_IN;
        break;
      case 'I':
        bits |= SHOW_IN_DETAIL;
        break;
      case 'e':
        bits |= SHOW_ERROR;
        break;
      case 'o':
        bits |= SHOW_OUT;
        break;
      case 'O':
        bits |= SHOW_OUT_DETAIL;
        break;
      case 't':
        bits |= SHOW_EVENT_TRIGGER;
        break;
      case 's':
        bits |= SHOW_EVENT_SENT;
        break;
      default:
        log_warning(jvmti)("Invalid trace flag '%c'", *flags);
        break;
      }
    }
    const int FUNC = 1;
    const int EXCLUDE  = 2;
    const int ALL_FUNC = 4;
    const int EVENT = 8;
    const int ALL_EVENT = 16;
    int domain = 0;
    size_t len = op_pos - curr;
    if (op_pos == curr) {
      domain = ALL_FUNC | FUNC | ALL_EVENT | EVENT | EXCLUDE;
    } else if (len==3 && strncmp(curr, "all", 3)==0) {
      domain = ALL_FUNC | FUNC | ALL_EVENT | EVENT;
    } else if (len==7 && strncmp(curr, "allfunc", 7)==0) {
      domain = ALL_FUNC | FUNC;
    } else if (len==4 && strncmp(curr, "func", 4)==0) {
      domain = ALL_FUNC | FUNC | EXCLUDE;
    } else if (len==8 && strncmp(curr, "allevent", 8)==0) {
      domain = ALL_EVENT | EVENT;
    } else if (len==5 && strncmp(curr, "event", 5)==0) {
      domain = ALL_EVENT | EVENT;
    } else if (len==2 && strncmp(curr, "ec", 2)==0) {
      _trace_event_controller = true;
      log_trace(jvmti)("Tracing the event controller");
    } else {
      domain = FUNC | EVENT;  // go searching
    }

    int exclude_index = 0;
    if (domain & FUNC) {
      if (domain & ALL_FUNC) {
        if (domain & EXCLUDE) {
          log_trace(jvmti)("Tracing all significant functions");
        } else {
          log_trace(jvmti)("Tracing all functions");
        }
      }
      for (int i = 0; i <= _max_function_index; ++i) {
        if (domain & EXCLUDE && i == _exclude_functions[exclude_index]) {
          ++exclude_index;
        } else {
          bool do_op = false;
          if (domain & ALL_FUNC) {
            do_op = true;
          } else {
            const char *fname = function_name(i);
            if (fname != NULL) {
              size_t fnlen = strlen(fname);
              if (len==fnlen && strncmp(curr, fname, fnlen)==0) {
                log_trace(jvmti)("Tracing the function: %s", fname);
                do_op = true;
              }
            }
          }
          if (do_op) {
            if (op == '+') {
              _trace_flags[i] |= bits;
            } else {
              _trace_flags[i] &= ~bits;
            }
            _on = true;
          }
        }
      }
    }
    if (domain & EVENT) {
      if (domain & ALL_EVENT) {
        log_trace(jvmti)("Tracing all events");
      }
      for (int i = 0; i <= _max_event_index; ++i) {
        bool do_op = false;
        if (domain & ALL_EVENT) {
          do_op = true;
        } else {
          const char *ename = event_name(i);
          if (ename != NULL) {
            size_t evtlen = strlen(ename);
            if (len==evtlen && strncmp(curr, ename, evtlen)==0) {
              log_trace(jvmti)("Tracing the event: %s", ename);
              do_op = true;
            }
          }
        }
        if (do_op) {
          if (op == '+') {
            _event_trace_flags[i] |= bits;
          } else {
            _event_trace_flags[i] &= ~bits;
          }
          _on = true;
        }
      }
    }
    if (!_on && (domain & (FUNC|EVENT))) {
      log_warning(jvmti)("Trace domain not found");
    }
    curr = curr_end + 1;
  }
  _initialized = true;
}


void JvmtiTrace::shutdown() {
  int i;
  _on = false;
  _trace_event_controller = false;
  for (i = 0; i <= _max_function_index; ++i) {
    _trace_flags[i] = 0;
  }
  for (i = 0; i <= _max_event_index; ++i) {
    _event_trace_flags[i] = 0;
  }
}


const char* JvmtiTrace::enum_name(const char** names, const jint* values, jint value) {
  for (int index = 0; names[index] != 0; ++index) {
    if (values[index] == value) {
      return names[index];
    }
  }
  return "*INVALID-ENUM-VALUE*";
}


// return a valid string no matter what state the thread is in
const char *JvmtiTrace::safe_get_thread_name(Thread *thread) {
  if (thread == NULL) {
    return "NULL";
  }
  if (!thread->is_Java_thread()) {
    return thread->name();
  }
  oop threadObj = JavaThread::cast(thread)->threadObj();
  if (threadObj == NULL) {
    return "NULL";
  }
  oop name = java_lang_Thread::name(threadObj);
  if (name == NULL) {
    return "<NOT FILLED IN>";
  }
  return java_lang_String::as_utf8_string(name);
}


// return the name of the current thread
const char *JvmtiTrace::safe_get_current_thread_name() {
  if (JvmtiEnv::is_vm_live()) {
    return JvmtiTrace::safe_get_thread_name(Thread::current_or_null());
  } else {
    return "VM not live";
  }
}

// return a valid string no matter what the state of k_mirror
const char * JvmtiTrace::get_class_name(oop k_mirror) {
  if (java_lang_Class::is_primitive(k_mirror)) {
    return "primitive";
  }
  Klass* k_oop = java_lang_Class::as_Klass(k_mirror);
  if (k_oop == NULL) {
    return "INVALID";
  }
  return k_oop->external_name();
}

#endif /*JVMTI_TRACE */
