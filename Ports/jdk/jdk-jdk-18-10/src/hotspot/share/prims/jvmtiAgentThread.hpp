/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_PRIMS_JVMTIAGENTTHREAD_HPP
#define SHARE_PRIMS_JVMTIAGENTTHREAD_HPP

#include "jvmtifiles/jvmtiEnv.hpp"

//
// class JvmtiAgentThread
//
// JavaThread used to wrap a thread started by an agent
// using the JVMTI method RunAgentThread.
//
class JvmtiAgentThread : public JavaThread {

  jvmtiStartFunction _start_fn;
  JvmtiEnv* _env;
  const void *_start_arg;

public:
  JvmtiAgentThread(JvmtiEnv* env, jvmtiStartFunction start_fn, const void *start_arg);

  bool is_jvmti_agent_thread() const    { return true; }

  static void start_function_wrapper(JavaThread *thread, TRAPS);
  void call_start_function();
};

#endif // SHARE_PRIMS_JVMTIAGENTTHREAD_HPP
