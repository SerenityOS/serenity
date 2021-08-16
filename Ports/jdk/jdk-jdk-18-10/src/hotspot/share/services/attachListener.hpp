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

#ifndef SHARE_SERVICES_ATTACHLISTENER_HPP
#define SHARE_SERVICES_ATTACHLISTENER_HPP

#include "memory/allocation.hpp"
#include "runtime/atomic.hpp"
#include "runtime/globals.hpp"
#include "utilities/debug.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"
#include "utilities/ostream.hpp"

// The AttachListener thread services a queue of operations that are enqueued
// by client tools. Each operation is identified by a name and has up to 3
// arguments. The operation name is mapped to a function which performs the
// operation. The function is called with an outputStream which is can use to
// write any result data (for examples the properties command serializes
// properties names and values to the output stream). When the function
// complets the result value and any result data is returned to the client
// tool.

class AttachOperation;

typedef jint (*AttachOperationFunction)(AttachOperation* op, outputStream* out);

struct AttachOperationFunctionInfo {
  const char* name;
  AttachOperationFunction func;
};

enum AttachListenerState {
  AL_NOT_INITIALIZED,
  AL_INITIALIZING,
  AL_INITIALIZED
};

class AttachListener: AllStatic {
 public:
  static void vm_start() NOT_SERVICES_RETURN;
  static void init()  NOT_SERVICES_RETURN;
  static void abort() NOT_SERVICES_RETURN;

  // invoke to perform clean-up tasks when all clients detach
  static void detachall() NOT_SERVICES_RETURN;

  // check unix domain socket file on filesystem
  static bool check_socket_file() NOT_SERVICES_RETURN_(false);

  // indicates if the Attach Listener needs to be created at startup
  static bool init_at_startup() NOT_SERVICES_RETURN_(false);

  // indicates if we have a trigger to start the Attach Listener
  static bool is_init_trigger() NOT_SERVICES_RETURN_(false);

#if !INCLUDE_SERVICES
  static bool is_attach_supported()             { return false; }
#else

 private:
  static volatile AttachListenerState _state;

 public:
  static void set_state(AttachListenerState new_state) {
    Atomic::store(&_state, new_state);
  }

  static AttachListenerState get_state() {
    return Atomic::load(&_state);
  }

  static AttachListenerState transit_state(AttachListenerState new_state,
                                           AttachListenerState cmp_state) {
    return Atomic::cmpxchg(&_state, cmp_state, new_state);
  }

  static bool is_initialized() {
    return Atomic::load(&_state) == AL_INITIALIZED;
  }

  static void set_initialized() {
    Atomic::store(&_state, AL_INITIALIZED);
  }

  // indicates if this VM supports attach-on-demand
  static bool is_attach_supported()             { return !DisableAttachMechanism; }

  // platform specific initialization
  static int pd_init();

  // platform specific operation
  static AttachOperationFunctionInfo* pd_find_operation(const char* name);

  // platform specific flag change
  static jint pd_set_flag(AttachOperation* op, outputStream* out);

  // platform specific detachall
  static void pd_detachall();

  // platform specific data dump
  static void pd_data_dump();

  // dequeue the next operation
  static AttachOperation* dequeue();
#endif // !INCLUDE_SERVICES

 private:
  static bool has_init_error(TRAPS);
};

#if INCLUDE_SERVICES
class AttachOperation: public CHeapObj<mtInternal> {
 public:
  enum {
    name_length_max = 16,       // maximum length of  name
    arg_length_max = 1024,      // maximum length of argument
    arg_count_max = 3           // maximum number of arguments
  };

  // name of special operation that can be enqueued when all
  // clients detach
  static char* detachall_operation_name() { return (char*)"detachall"; }

 private:
  char _name[name_length_max+1];
  char _arg[arg_count_max][arg_length_max+1];

 public:
  const char* name() const                      { return _name; }

  // set the operation name
  void set_name(const char* name) {
    assert(strlen(name) <= name_length_max, "exceeds maximum name length");
    size_t len = MIN2(strlen(name), (size_t)name_length_max);
    memcpy(_name, name, len);
    _name[len] = '\0';
  }

  // get an argument value
  const char* arg(int i) const {
    assert(i>=0 && i<arg_count_max, "invalid argument index");
    return _arg[i];
  }

  // set an argument value
  void set_arg(int i, char* arg) {
    assert(i>=0 && i<arg_count_max, "invalid argument index");
    if (arg == NULL) {
      _arg[i][0] = '\0';
    } else {
      assert(strlen(arg) <= arg_length_max, "exceeds maximum argument length");
      size_t len = MIN2(strlen(arg), (size_t)arg_length_max);
      memcpy(_arg[i], arg, len);
      _arg[i][len] = '\0';
    }
  }

  // create an operation of a given name
  AttachOperation(const char* name) {
    set_name(name);
    for (int i=0; i<arg_count_max; i++) {
      set_arg(i, NULL);
    }
  }

  // complete operation by sending result code and any result data to the client
  virtual void complete(jint result, bufferedStream* result_stream) = 0;
};
#endif // INCLUDE_SERVICES

#endif // SHARE_SERVICES_ATTACHLISTENER_HPP
