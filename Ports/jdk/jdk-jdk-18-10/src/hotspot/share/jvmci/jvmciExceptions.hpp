/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JVMCI_JVMCIEXCEPTIONS_HPP
#define SHARE_JVMCI_JVMCIEXCEPTIONS_HPP

class JVMCIEnv;

// JVMCIEnv exception utility macros.  Analagous to the regular CHECK, TRAP and THREAD macros.

#define JVMCIENV __jvmci_env__
#define JVMCI_TRAPS  JVMCIEnv* JVMCIENV

#define JNI_JVMCIENV(thread, env)                                     \
  JVMCIEnv __stack_jvmci_env__(thread, env, __FILE__, __LINE__);      \
  JVMCIEnv* JVMCIENV = &__stack_jvmci_env__

#define THREAD_JVMCIENV(thread)                               \
  JVMCIEnv __stack_jvmci_env__(thread, __FILE__, __LINE__);   \
  JVMCIEnv* JVMCIENV = &__stack_jvmci_env__

#define JVMCI_PENDING_EXCEPTION                        (JVMCIENV->pending_exception())
#define JVMCI_HAS_PENDING_EXCEPTION                    (JVMCIENV->has_pending_exception())
#define JVMCI_CLEAR_PENDING_EXCEPTION                  (JVMCIENV->clear_pending_exception())

#define JVMCI_CHECK                                    JVMCIENV); if (JVMCI_HAS_PENDING_EXCEPTION) return       ; (void)(0
#define JVMCI_CHECK_(result)                           JVMCIENV); if (JVMCI_HAS_PENDING_EXCEPTION) return result; (void)(0
#define JVMCI_CHECK_0                                  JVMCI_CHECK_(0)
#define JVMCI_CHECK_NULL                               JVMCI_CHECK_(NULL)
#define JVMCI_CHECK_false                              JVMCI_CHECK_(false)
#define JVMCI_CHECK_OK                                 JVMCI_CHECK_(JVMCI::ok)

#define JVMCI_ERROR(...)       \
  { JVMCIENV->fthrow_error(__FILE__, __LINE__, __VA_ARGS__); return; }

#define JVMCI_ERROR_(ret, ...) \
  { JVMCIENV->fthrow_error( __FILE__, __LINE__, __VA_ARGS__); return ret; }

#define JVMCI_ERROR_0(...)    JVMCI_ERROR_(0, __VA_ARGS__)
#define JVMCI_ERROR_NULL(...) JVMCI_ERROR_(NULL, __VA_ARGS__)
#define JVMCI_ERROR_OK(...)   JVMCI_ERROR_(JVMCI::ok, __VA_ARGS__)

#define JVMCI_THROW(name) { JVMCIENV->throw_##name(); return; }
#define JVMCI_THROW_NULL(name) { JVMCIENV->throw_##name(); return NULL; }
#define JVMCI_THROW_0(name) { JVMCIENV->throw_##name(); return 0; }
#define JVMCI_THROW_MSG_NULL(name, msg) { JVMCIENV->throw_##name(msg); return NULL; }
#define JVMCI_THROW_MSG_(name, msg, value) { JVMCIENV->throw_##name(msg); return (value); }
#define JVMCI_THROW_MSG_0(name, msg) { JVMCIENV->throw_##name(msg); return 0; }
#define JVMCI_THROW_MSG(name, msg) { JVMCIENV->throw_##name(msg); return; }
#define JVMCI_THROW_(name, value) { JVMCIENV->throw_##name(); return (value); }

#define JVMCI_CATCH                              \
  JVMCIENV); if (JVMCI_HAS_PENDING_EXCEPTION) {  \
    JVMCIENV->describe_pending_exception(true);  \
    ShouldNotReachHere();                        \
  } (void)(0

#endif // SHARE_JVMCI_JVMCIEXCEPTIONS_HPP
