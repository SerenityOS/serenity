/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_EXCEPTIONS_HPP
#define SHARE_UTILITIES_EXCEPTIONS_HPP

#include "memory/allocation.hpp"
#include "oops/oopsHierarchy.hpp"
#include "utilities/ostream.hpp"
#include "utilities/sizes.hpp"

// This file provides the basic support for exception handling in the VM.
// Note: We do not use C++ exceptions to avoid compiler dependencies and
// unpredictable performance.
//
// Scheme: Exceptions are stored with the thread. There is never more
// than one pending exception per thread. All functions that can throw
// an exception carry a THREAD argument (usually the last argument and
// declared with the TRAPS macro). Throwing an exception means setting
// a pending exception in the thread. Upon return from a function that
// can throw an exception, we must check if an exception is pending.
// The CHECK macros do this in a convenient way. Carrying around the
// thread provides also convenient access to it (e.g. for Handle
// creation, w/o the need for recomputation).



// Forward declarations to be independent of the include structure.

class JavaThread;
class Handle;
class Symbol;
class JavaCallArguments;
class methodHandle;

// The ThreadShadow class is a helper class to access the _pending_exception
// field of the Thread class w/o having access to the Thread's interface (for
// include hierachy reasons).

class ThreadShadow: public CHeapObj<mtThread> {
  friend class VMStructs;
  friend class JVMCIVMStructs;

 protected:
  oop  _pending_exception;                       // Thread has gc actions.
  const char* _exception_file;                   // file information for exception (debugging only)
  int         _exception_line;                   // line information for exception (debugging only)
  friend void check_ThreadShadow();              // checks _pending_exception offset

  // The following virtual exists only to force creation of a vtable.
  // We need ThreadShadow to have a vtable, even in product builds,
  // so that its layout will start at an offset of zero relative to Thread.
  // Some C++ compilers are so "clever" that they put the ThreadShadow
  // base class at offset 4 in Thread (after Thread's vtable), if they
  // notice that Thread has a vtable but ThreadShadow does not.
  virtual void unused_initial_virtual() { }

 public:
  oop  pending_exception() const                 { return _pending_exception; }
  bool has_pending_exception() const             { return _pending_exception != NULL; }
  const char* exception_file() const             { return _exception_file; }
  int  exception_line() const                    { return _exception_line; }

  // Code generation support
  static ByteSize pending_exception_offset()     { return byte_offset_of(ThreadShadow, _pending_exception); }

  // use THROW whenever possible!
  void set_pending_exception(oop exception, const char* file, int line);

  // use CLEAR_PENDING_EXCEPTION whenever possible!
  void clear_pending_exception();

  // use CLEAR_PENDING_NONASYNC_EXCEPTION to clear probable nonasync exception.
  void clear_pending_nonasync_exception();

  ThreadShadow() : _pending_exception(NULL),
                   _exception_file(NULL), _exception_line(0) {}
};


// Exceptions is a helper class that encapsulates all operations
// that require access to the thread interface and which are
// relatively rare. The Exceptions operations should only be
// used directly if the macros below are insufficient.

class Exceptions {
  static bool special_exception(JavaThread* thread, const char* file, int line, Handle exception);
  static bool special_exception(JavaThread* thread, const char* file, int line, Symbol* name, const char* message);

  // Count out of memory errors that are interesting in error diagnosis
  static volatile int _out_of_memory_error_java_heap_errors;
  static volatile int _out_of_memory_error_metaspace_errors;
  static volatile int _out_of_memory_error_class_metaspace_errors;

  // Count linkage errors
  static volatile int _linkage_errors;
 public:
  // this enum is defined to indicate whether it is safe to
  // ignore the encoding scheme of the original message string.
  typedef enum {
    safe_to_utf8 = 0,
    unsafe_to_utf8 = 1
  } ExceptionMsgToUtf8Mode;
  // Throw exceptions: w/o message, w/ message & with formatted message.
  static void _throw_oop(JavaThread* thread, const char* file, int line, oop exception);
  static void _throw(JavaThread* thread, const char* file, int line, Handle exception, const char* msg = NULL);

  static void _throw_msg(JavaThread* thread, const char* file, int line, Symbol* name, const char* message);
  static void _throw_msg(JavaThread* thread, const char* file, int line, Symbol* name, const char* message,
                         Handle loader, Handle protection_domain);

  static void _throw_msg_cause(JavaThread* thread, const char* file, int line, Symbol* name, const char* message, Handle h_cause);
  static void _throw_msg_cause(JavaThread* thread, const char* file, int line, Symbol* name, const char* message, Handle h_cause,
                               Handle h_loader, Handle h_protection_domain);

  static void _throw_cause(JavaThread* thread, const char* file, int line, Symbol* name, Handle h_cause);
  static void _throw_cause(JavaThread* thread, const char* file, int line, Symbol* name, Handle h_cause,
                           Handle h_loader, Handle h_protection_domain);

  static void _throw_args(JavaThread* thread, const char* file, int line,
                          Symbol* name, Symbol* signature,
                          JavaCallArguments* args);

  // There is no THROW... macro for this method. Caller should remember
  // to do a return after calling it.
  static void fthrow(JavaThread* thread, const char* file, int line, Symbol* name,
                     const char* format, ...) ATTRIBUTE_PRINTF(5, 6);

  // Create and initialize a new exception
  static Handle new_exception(JavaThread* thread, Symbol* name,
                              Symbol* signature, JavaCallArguments* args,
                              Handle loader, Handle protection_domain);

  static Handle new_exception(JavaThread* thread, Symbol* name,
                              Symbol* signature, JavaCallArguments* args,
                              Handle cause,
                              Handle loader, Handle protection_domain);

  static Handle new_exception(JavaThread* thread, Symbol* name,
                              Handle cause,
                              Handle loader, Handle protection_domain,
                              ExceptionMsgToUtf8Mode to_utf8_safe = safe_to_utf8);

  static Handle new_exception(JavaThread* thread, Symbol* name,
                              const char* message, Handle cause,
                              Handle loader, Handle protection_domain,
                              ExceptionMsgToUtf8Mode to_utf8_safe = safe_to_utf8);

  static Handle new_exception(JavaThread* thread, Symbol* name,
                              const char* message,
                              ExceptionMsgToUtf8Mode to_utf8_safe = safe_to_utf8);

  static void throw_stack_overflow_exception(JavaThread* thread, const char* file, int line, const methodHandle& method);

  static void throw_unsafe_access_internal_error(JavaThread* thread, const char* file, int line, const char* message);

  static void wrap_dynamic_exception(bool is_indy, JavaThread* thread);

  // Exception counting for error files of interesting exceptions that may have
  // caused a problem for the jvm
  static volatile int _stack_overflow_errors;

  static bool has_exception_counts();
  static void count_out_of_memory_exceptions(Handle exception);
  static void print_exception_counts_on_error(outputStream* st);

  // for AbortVMOnException flag
  static void debug_check_abort(Handle exception, const char* message = NULL);
  static void debug_check_abort_helper(Handle exception, const char* message = NULL);
  static void debug_check_abort(const char *value_string, const char* message = NULL);

  // for logging exceptions
  static void log_exception(Handle exception, const char* message);
};


// The THREAD & TRAPS macros facilitate the declaration of functions that throw exceptions.
// Convention: Use the TRAPS macro as the last argument of such a function; e.g.:
//
// int this_function_may_trap(int x, float y, TRAPS)

#define THREAD __the_thread__
#define TRAPS  JavaThread* THREAD


// The CHECK... macros should be used to pass along a THREAD reference and to check for pending
// exceptions. In special situations it is necessary to handle pending exceptions explicitly,
// in these cases the PENDING_EXCEPTION helper macros should be used.
//
// Macro naming conventions: Macros that end with _ require a result value to be returned. They
// are for functions with non-void result type. The result value is usually ignored because of
// the exception and is only needed for syntactic correctness. The _0 ending is a shortcut for
// _(0) since this is a frequent case. Example:
//
// int result = this_function_may_trap(x_arg, y_arg, CHECK_0);
//
// CAUTION: make sure that the function call using a CHECK macro is not the only statement of a
// conditional branch w/o enclosing {} braces, since the CHECK macros expand into several state-
// ments! Also make sure it is not used on a function call that is part of a return statement!

#define PENDING_EXCEPTION                        (((ThreadShadow*)THREAD)->pending_exception())
#define HAS_PENDING_EXCEPTION                    (((ThreadShadow*)THREAD)->has_pending_exception())
#define CLEAR_PENDING_EXCEPTION                  (((ThreadShadow*)THREAD)->clear_pending_exception())

#define CHECK                                    THREAD); if (HAS_PENDING_EXCEPTION) return       ; (void)(0
#define CHECK_(result)                           THREAD); if (HAS_PENDING_EXCEPTION) return result; (void)(0
#define CHECK_0                                  CHECK_(0)
#define CHECK_NH                                 CHECK_(Handle())
#define CHECK_NULL                               CHECK_(NULL)
#define CHECK_false                              CHECK_(false)
#define CHECK_JNI_ERR                            CHECK_(JNI_ERR)

// CAUTION: These macros clears all exceptions including async exceptions, use it with caution.
#define CHECK_AND_CLEAR                         THREAD); if (HAS_PENDING_EXCEPTION) { CLEAR_PENDING_EXCEPTION; return;        } (void)(0
#define CHECK_AND_CLEAR_(result)                THREAD); if (HAS_PENDING_EXCEPTION) { CLEAR_PENDING_EXCEPTION; return result; } (void)(0
#define CHECK_AND_CLEAR_0                       CHECK_AND_CLEAR_(0)
#define CHECK_AND_CLEAR_NH                      CHECK_AND_CLEAR_(Handle())
#define CHECK_AND_CLEAR_NULL                    CHECK_AND_CLEAR_(NULL)
#define CHECK_AND_CLEAR_false                   CHECK_AND_CLEAR_(false)

// CAUTION: These macros clears all exceptions except probable async exceptions j.l.InternalError and j.l.ThreadDeath.
// So use it with caution.
#define CLEAR_PENDING_NONASYNC_EXCEPTION        (((ThreadShadow*)THREAD)->clear_pending_nonasync_exception())
#define CHECK_AND_CLEAR_NONASYNC                THREAD); if (HAS_PENDING_EXCEPTION) { CLEAR_PENDING_NONASYNC_EXCEPTION; return; } (void)(0
#define CHECK_AND_CLEAR_NONASYNC_(result)       THREAD); if (HAS_PENDING_EXCEPTION) { CLEAR_PENDING_NONASYNC_EXCEPTION; return result; } (void)(0
#define CHECK_AND_CLEAR_NONASYNC_0              CHECK_AND_CLEAR_NONASYNC_(0)
#define CHECK_AND_CLEAR_NONASYNC_NH             CHECK_AND_CLEAR_NONASYNC_(Handle())
#define CHECK_AND_CLEAR_NONASYNC_NULL           CHECK_AND_CLEAR_NONASYNC_(NULL)
#define CHECK_AND_CLEAR_NONASYNC_false          CHECK_AND_CLEAR_NONASYNC_(false)

// The THROW... macros should be used to throw an exception. They require a THREAD variable to be
// visible within the scope containing the THROW. Usually this is achieved by declaring the function
// with a TRAPS argument.

#define THREAD_AND_LOCATION                      THREAD, __FILE__, __LINE__

#define THROW_OOP(e)                                \
  { Exceptions::_throw_oop(THREAD_AND_LOCATION, e);                             return;  }

#define THROW_HANDLE(e)                                \
  { Exceptions::_throw(THREAD_AND_LOCATION, e);                             return;  }

#define THROW(name)                                 \
  { Exceptions::_throw_msg(THREAD_AND_LOCATION, name, NULL); return;  }

#define THROW_MSG(name, message)                    \
  { Exceptions::_throw_msg(THREAD_AND_LOCATION, name, message); return;  }

#define THROW_CAUSE(name, cause)   \
  { Exceptions::_throw_cause(THREAD_AND_LOCATION, name, cause); return; }

#define THROW_MSG_LOADER(name, message, loader, protection_domain) \
  { Exceptions::_throw_msg(THREAD_AND_LOCATION, name, message, loader, protection_domain); return;  }

#define THROW_ARG(name, signature, args) \
  { Exceptions::_throw_args(THREAD_AND_LOCATION, name, signature, args);   return; }

#define THROW_OOP_(e, result)                       \
  { Exceptions::_throw_oop(THREAD_AND_LOCATION, e);                           return result; }

#define THROW_HANDLE_(e, result)                       \
  { Exceptions::_throw(THREAD_AND_LOCATION, e);                           return result; }

#define THROW_(name, result)                        \
  { Exceptions::_throw_msg(THREAD_AND_LOCATION, name, NULL); return result; }

#define THROW_MSG_(name, message, result)           \
  { Exceptions::_throw_msg(THREAD_AND_LOCATION, name, message); return result; }

#define THROW_MSG_LOADER_(name, message, loader, protection_domain, result) \
  { Exceptions::_throw_msg(THREAD_AND_LOCATION, name, message, loader, protection_domain); return result; }

#define THROW_ARG_(name, signature, args, result) \
  { Exceptions::_throw_args(THREAD_AND_LOCATION, name, signature, args); return result; }

#define THROW_MSG_CAUSE(name, message, cause)   \
  { Exceptions::_throw_msg_cause(THREAD_AND_LOCATION, name, message, cause); return; }

#define THROW_MSG_CAUSE_(name, message, cause, result)   \
  { Exceptions::_throw_msg_cause(THREAD_AND_LOCATION, name, message, cause); return result; }


#define THROW_OOP_0(e)                      THROW_OOP_(e, 0)
#define THROW_HANDLE_0(e)                   THROW_HANDLE_(e, 0)
#define THROW_0(name)                       THROW_(name, 0)
#define THROW_MSG_0(name, message)          THROW_MSG_(name, message, 0)
#define THROW_WRAPPED_0(name, oop_to_wrap)  THROW_WRAPPED_(name, oop_to_wrap, 0)
#define THROW_ARG_0(name, signature, arg)   THROW_ARG_(name, signature, arg, 0)
#define THROW_MSG_CAUSE_0(name, message, cause) THROW_MSG_CAUSE_(name, message, cause, 0)
#define THROW_MSG_CAUSE_NULL(name, message, cause) THROW_MSG_CAUSE_(name, message, cause, NULL)

#define THROW_NULL(name)                    THROW_(name, NULL)
#define THROW_MSG_NULL(name, message)       THROW_MSG_(name, message, NULL)

// The CATCH macro checks that no exception has been thrown by a function; it is used at
// call sites about which is statically known that the callee cannot throw an exception
// even though it is declared with TRAPS.

#define CATCH                              \
  THREAD); if (HAS_PENDING_EXCEPTION) {    \
    oop ex = PENDING_EXCEPTION;            \
    CLEAR_PENDING_EXCEPTION;               \
    DEBUG_ONLY(ex->print();)               \
    assert(false, "CATCH");                \
  } (void)(0

// ExceptionMark is a stack-allocated helper class for local exception handling.
// It is used with the EXCEPTION_MARK macro.

class ExceptionMark {
 private:
  JavaThread* _thread;
  inline void check_no_pending_exception();

 public:
  ExceptionMark();
  ExceptionMark(JavaThread* thread);
  ~ExceptionMark();

  JavaThread* thread() {
    return _thread;
  }
};

// Use an EXCEPTION_MARK for 'local' exceptions. EXCEPTION_MARK makes sure that no
// pending exception exists upon entering its scope and tests that no pending exception
// exists when leaving the scope.

// See also preserveException.hpp for PreserveExceptionMark
// which preserves pre-existing exceptions and does not allow new
// exceptions.

#define EXCEPTION_MARK                           ExceptionMark __em; JavaThread* THREAD = __em.thread();

#endif // SHARE_UTILITIES_EXCEPTIONS_HPP
