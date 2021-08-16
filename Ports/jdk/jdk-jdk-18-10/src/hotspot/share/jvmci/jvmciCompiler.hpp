/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 */

#ifndef SHARE_JVMCI_JVMCICOMPILER_HPP
#define SHARE_JVMCI_JVMCICOMPILER_HPP

#include "compiler/abstractCompiler.hpp"
#include "compiler/compiler_globals.hpp"
#include "runtime/atomic.hpp"

class JVMCICompiler : public AbstractCompiler {
private:
  bool _bootstrapping;

  /**
   * True if we have seen a bootstrap compilation request.
   */
  volatile bool _bootstrap_compilation_request_handled;

  /**
   * Number of methods successfully compiled by a call to
   * JVMCICompiler::compile_method().
   */
  volatile int _methods_compiled;

  // Incremented periodically by JVMCI compiler threads
  // to indicate JVMCI compilation activity.
  volatile int _global_compilation_ticks;

  static JVMCICompiler* _instance;

  // Code installation timer for CompileBroker compilations
  static elapsedTimer _codeInstallTimer;

  // Code installation timer for non-CompileBroker compilations
  static elapsedTimer _hostedCodeInstallTimer;

  /**
   * Exits the VM due to an unexpected exception.
   */
  static void exit_on_pending_exception(oop exception, const char* message);

public:
  JVMCICompiler();

  static JVMCICompiler* instance(bool require_non_null, TRAPS);

  virtual const char* name() { return UseJVMCINativeLibrary ? "JVMCI-native" : "JVMCI"; }

  bool is_jvmci()                                { return true; }
  bool is_c1   ()                                { return false; }
  bool is_c2   ()                                { return false; }

  bool needs_stubs            () { return false; }

  // Initialization
  virtual void initialize();

  /**
   * Initialize the compile queue with the methods in java.lang.Object and
   * then wait until the queue is empty.
   */
  void bootstrap(TRAPS);

  // Should force compilation of method at CompLevel_simple?
  bool force_comp_at_level_simple(const methodHandle& method);

  bool is_bootstrapping() const { return _bootstrapping; }

  void set_bootstrap_compilation_request_handled() {
    _instance->_bootstrap_compilation_request_handled = true;
  }

  // Compilation entry point for methods
  virtual void compile_method(ciEnv* env, ciMethod* target, int entry_bci, bool install_code, DirectiveSet* directive);

  // Print compilation timers and statistics
  virtual void print_timers();

  // Gets the number of methods that have been successfully compiled by
  // a call to JVMCICompiler::compile_method().
  int methods_compiled() { return _methods_compiled; }
  void inc_methods_compiled();

  // Gets a value indicating JVMCI compilation activity on any thread.
  // If successive calls to this method return a different value, then
  // some degree of JVMCI compilation occurred between the calls.
  int global_compilation_ticks() const { return _global_compilation_ticks; }
  void inc_global_compilation_ticks();

  // Print timers related to non-CompileBroker compilations
  static void print_hosted_timers();

  static elapsedTimer* codeInstallTimer(bool hosted) {
    if (!hosted) {
      return &_codeInstallTimer;
    } else {
      return &_hostedCodeInstallTimer;
    }
  }
};

#endif // SHARE_JVMCI_JVMCICOMPILER_HPP
