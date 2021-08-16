/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_COMPILER_COMPILETASK_HPP
#define SHARE_COMPILER_COMPILETASK_HPP

#include "ci/ciMethod.hpp"
#include "code/nmethod.hpp"
#include "compiler/compileLog.hpp"
#include "memory/allocation.hpp"
#include "utilities/xmlstream.hpp"

JVMCI_ONLY(class JVMCICompileState;)

// CompileTask
//
// An entry in the compile queue.  It represents a pending or current
// compilation.

class CompileTask : public CHeapObj<mtCompiler> {
  friend class VMStructs;
  friend class JVMCIVMStructs;

 public:
  // Different reasons for a compilation
  // The order is important - mapped to reason_names[]
  enum CompileReason {
      Reason_None,
      Reason_InvocationCount,  // Simple/StackWalk-policy
      Reason_BackedgeCount,    // Simple/StackWalk-policy
      Reason_Tiered,           // Tiered-policy
      Reason_Replay,           // ciReplay
      Reason_Whitebox,         // Whitebox API
      Reason_MustBeCompiled,   // Used for -Xcomp or AlwaysCompileLoopMethods (see CompilationPolicy::must_be_compiled())
      Reason_Bootstrap,        // JVMCI bootstrap
      Reason_Count
  };

  static const char* reason_name(CompileTask::CompileReason compile_reason) {
    static const char* reason_names[] = {
      "no_reason",
      "count",
      "backedge_count",
      "tiered",
      "replay",
      "whitebox",
      "must_be_compiled",
      "bootstrap"
    };
    return reason_names[compile_reason];
  }

 private:
  static CompileTask* _task_free_list;
  Monitor*     _lock;
  uint         _compile_id;
  Method*      _method;
  jobject      _method_holder;
  int          _osr_bci;
  bool         _is_complete;
  bool         _is_success;
  bool         _is_blocking;
#if INCLUDE_JVMCI
  bool         _has_waiter;
  // Compilation state for a blocking JVMCI compilation
  JVMCICompileState* _blocking_jvmci_compile_state;
#endif
  int          _comp_level;
  int          _num_inlined_bytecodes;
  nmethodLocker* _code_handle;  // holder of eventual result
  CompileTask* _next, *_prev;
  bool         _is_free;
  // Fields used for logging why the compilation was initiated:
  jlong        _time_queued;  // time when task was enqueued
  jlong        _time_started; // time when compilation started
  Method*      _hot_method;   // which method actually triggered this task
  jobject      _hot_method_holder;
  int          _hot_count;    // information about its invocation counter
  CompileReason _compile_reason;      // more info about the task
  const char*  _failure_reason;
  // Specifies if _failure_reason is on the C heap.
  bool         _failure_reason_on_C_heap;

 public:
  CompileTask() : _failure_reason(NULL), _failure_reason_on_C_heap(false) {
    _lock = new Monitor(Mutex::nonleaf+2, "CompileTaskLock");
  }

  void initialize(int compile_id, const methodHandle& method, int osr_bci, int comp_level,
                  const methodHandle& hot_method, int hot_count,
                  CompileTask::CompileReason compile_reason, bool is_blocking);

  static CompileTask* allocate();
  static void         free(CompileTask* task);

  int          compile_id() const                { return _compile_id; }
  Method*      method() const                    { return _method; }
  Method*      hot_method() const                { return _hot_method; }
  int          osr_bci() const                   { return _osr_bci; }
  bool         is_complete() const               { return _is_complete; }
  bool         is_blocking() const               { return _is_blocking; }
  bool         is_success() const                { return _is_success; }
  bool         can_become_stale() const          {
    switch (_compile_reason) {
      case Reason_BackedgeCount:
      case Reason_InvocationCount:
      case Reason_Tiered:
        return !_is_blocking;
      default:
        return false;
    }
  }
#if INCLUDE_JVMCI
  bool         should_wait_for_compilation() const {
    // Wait for blocking compilation to finish.
    switch (_compile_reason) {
        case Reason_Replay:
        case Reason_Whitebox:
        case Reason_Bootstrap:
          return _is_blocking;
        default:
          return false;
    }
  }

  bool         has_waiter() const                { return _has_waiter; }
  void         clear_waiter()                    { _has_waiter = false; }
  JVMCICompileState* blocking_jvmci_compile_state() const { return _blocking_jvmci_compile_state; }
  void         set_blocking_jvmci_compile_state(JVMCICompileState* state) {
    _blocking_jvmci_compile_state = state;
  }
#endif

  nmethodLocker* code_handle() const             { return _code_handle; }
  void         set_code_handle(nmethodLocker* l) { _code_handle = l; }
  nmethod*     code() const;                     // _code_handle->code()
  void         set_code(nmethod* nm);            // _code_handle->set_code(nm)

  Monitor*     lock() const                      { return _lock; }

  void         mark_complete()                   { _is_complete = true; }
  void         mark_success()                    { _is_success = true; }
  void         mark_started(jlong time)          { _time_started = time; }

  int          comp_level()                      { return _comp_level;}
  void         set_comp_level(int comp_level)    { _comp_level = comp_level;}

  AbstractCompiler* compiler();
  CompileTask*      select_for_compilation();

  int          num_inlined_bytecodes() const     { return _num_inlined_bytecodes; }
  void         set_num_inlined_bytecodes(int n)  { _num_inlined_bytecodes = n; }

  CompileTask* next() const                      { return _next; }
  void         set_next(CompileTask* next)       { _next = next; }
  CompileTask* prev() const                      { return _prev; }
  void         set_prev(CompileTask* prev)       { _prev = prev; }
  bool         is_free() const                   { return _is_free; }
  void         set_is_free(bool val)             { _is_free = val; }
  bool         is_unloaded() const;

  // RedefineClasses support
  void         metadata_do(MetadataClosure* f);
  void         mark_on_stack();

private:
  static void  print_impl(outputStream* st, Method* method, int compile_id, int comp_level,
                                      bool is_osr_method = false, int osr_bci = -1, bool is_blocking = false,
                                      const char* msg = NULL, bool short_form = false, bool cr = true,
                                      jlong time_queued = 0, jlong time_started = 0);

public:
  void         print(outputStream* st = tty, const char* msg = NULL, bool short_form = false, bool cr = true);
  void         print_ul(const char* msg = NULL);
  static void  print(outputStream* st, const nmethod* nm, const char* msg = NULL, bool short_form = false, bool cr = true) {
    print_impl(st, nm->method(), nm->compile_id(), nm->comp_level(),
                           nm->is_osr_method(), nm->is_osr_method() ? nm->osr_entry_bci() : -1, /*is_blocking*/ false,
                           msg, short_form, cr);
  }
  static void  print_ul(const nmethod* nm, const char* msg = NULL);

  static void  print_inline_indent(int inline_level, outputStream* st = tty);

  void         print_tty();
  void         print_line_on_error(outputStream* st, char* buf, int buflen);

  void         log_task(xmlStream* log);
  void         log_task_queued();
  void         log_task_start(CompileLog* log);
  void         log_task_done(CompileLog* log);

  void         set_failure_reason(const char* reason, bool on_C_heap = false) {
    _failure_reason = reason;
    _failure_reason_on_C_heap = on_C_heap;
  }

  bool         check_break_at_flags();

  static void print_inlining_inner(outputStream* st, ciMethod* method, int inline_level, int bci, const char* msg = NULL);
  static void print_inlining_tty(ciMethod* method, int inline_level, int bci, const char* msg = NULL) {
    print_inlining_inner(tty, method, inline_level, bci, msg);
  }
  static void print_inlining_ul(ciMethod* method, int inline_level, int bci, const char* msg = NULL);
};

#endif // SHARE_COMPILER_COMPILETASK_HPP
