/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_COMPILER_COMPILERTHREAD_HPP
#define SHARE_COMPILER_COMPILERTHREAD_HPP

#include "runtime/thread.hpp"

class BufferBlob;
class AbstractCompiler;
class ciEnv;
class CompileThread;
class CompileLog;
class CompileTask;
class CompileQueue;
class CompilerCounters;
class IdealGraphPrinter;
class JVMCIEnv;
class JVMCIPrimitiveArray;

// A thread used for Compilation.
class CompilerThread : public JavaThread {
  friend class VMStructs;
 private:
  CompilerCounters* _counters;

  ciEnv*                _env;
  CompileLog*           _log;
  CompileTask* volatile _task;  // print_threads_compiling can read this concurrently.
  CompileQueue*         _queue;
  BufferBlob*           _buffer_blob;

  AbstractCompiler*     _compiler;
  TimeStamp             _idle_time;

 public:

  static CompilerThread* current() {
    return CompilerThread::cast(JavaThread::current());
  }

  static CompilerThread* cast(Thread* t) {
    assert(t->is_Compiler_thread(), "incorrect cast to CompilerThread");
    return static_cast<CompilerThread*>(t);
  }

  CompilerThread(CompileQueue* queue, CompilerCounters* counters);
  ~CompilerThread();

  bool is_Compiler_thread() const                { return true; }

  virtual bool can_call_java() const;

  // Hide native compiler threads from external view.
  bool is_hidden_from_external_view() const      { return !can_call_java(); }

  void set_compiler(AbstractCompiler* c)         { _compiler = c; }
  AbstractCompiler* compiler() const             { return _compiler; }

  CompileQueue* queue()        const             { return _queue; }
  CompilerCounters* counters() const             { return _counters; }

  // Get/set the thread's compilation environment.
  ciEnv*        env()                            { return _env; }
  void          set_env(ciEnv* env)              { _env = env; }

  BufferBlob*   get_buffer_blob() const          { return _buffer_blob; }
  void          set_buffer_blob(BufferBlob* b)   { _buffer_blob = b; }

  // Get/set the thread's logging information
  CompileLog*   log()                            { return _log; }
  void          init_log(CompileLog* log) {
    // Set once, for good.
    assert(_log == NULL, "set only once");
    _log = log;
  }

  void start_idle_timer()                        { _idle_time.update(); }
  jlong idle_time_millis() {
    return TimeHelper::counter_to_millis(_idle_time.ticks_since_update());
  }

#ifndef PRODUCT
 private:
  IdealGraphPrinter *_ideal_graph_printer;
 public:
  IdealGraphPrinter *ideal_graph_printer()           { return _ideal_graph_printer; }
  void set_ideal_graph_printer(IdealGraphPrinter *n) { _ideal_graph_printer = n; }
#endif

  // Get/set the thread's current task
  CompileTask* task()                      { return _task; }
  void         set_task(CompileTask* task) { _task = task; }

  static void thread_entry(JavaThread* thread, TRAPS);
};

// Dedicated thread to sweep the code cache
class CodeCacheSweeperThread : public JavaThread {
  CompiledMethod*       _scanned_compiled_method; // nmethod being scanned by the sweeper

  static void thread_entry(JavaThread* thread, TRAPS);

 public:
  CodeCacheSweeperThread();
  // Track the nmethod currently being scanned by the sweeper
  void set_scanned_compiled_method(CompiledMethod* cm) {
    assert(_scanned_compiled_method == NULL || cm == NULL, "should reset to NULL before writing a new value");
    _scanned_compiled_method = cm;
  }

  // Hide sweeper thread from external view.
  bool is_hidden_from_external_view() const { return true; }

  bool is_Code_cache_sweeper_thread() const { return true; }

  // Prevent GC from unloading _scanned_compiled_method
  void oops_do_no_frames(OopClosure* f, CodeBlobClosure* cf);
  void nmethods_do(CodeBlobClosure* cf);
};


#endif  // SHARE_COMPILER_COMPILERTHREAD_HPP
