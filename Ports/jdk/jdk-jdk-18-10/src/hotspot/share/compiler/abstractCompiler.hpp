/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_COMPILER_ABSTRACTCOMPILER_HPP
#define SHARE_COMPILER_ABSTRACTCOMPILER_HPP

#include "ci/compilerInterface.hpp"
#include "compiler/compilerDefinitions.hpp"
#include "compiler/compilerDirectives.hpp"

typedef void (*initializer)(void);

// Per-compiler statistics
class CompilerStatistics {
  friend class VMStructs;

  class Data {
    friend class VMStructs;
  public:
    elapsedTimer _time;  // time spent compiling
    int _bytes;          // number of bytecodes compiled, including inlined bytecodes
    int _count;          // number of compilations
    Data() : _bytes(0), _count(0) {}
    void update(elapsedTimer time, int bytes) {
      _time.add(time);
      _bytes += bytes;
      _count++;
    }
    void reset() {
      _time.reset();
    }
  };

 public:
  Data _standard;  // stats for non-OSR compilations
  Data _osr;       // stats for OSR compilations
  int _nmethods_size; //
  int _nmethods_code_size;

  double total_time() { return _standard._time.seconds() + _osr._time.seconds(); }

  double bytes_per_second() {
    int bytes = _standard._bytes + _osr._bytes;
    if (bytes == 0) {
      return 0.0;
    }
    double seconds = total_time();
    return seconds == 0.0 ? 0.0 : (bytes / seconds);
  }

  CompilerStatistics() : _nmethods_size(0), _nmethods_code_size(0) {}
};

class AbstractCompiler : public CHeapObj<mtCompiler> {
 private:
  volatile int _num_compiler_threads;

 protected:
  volatile int _compiler_state;
  // Used for tracking global state of compiler runtime initialization
  enum { uninitialized, initializing, initialized, failed, shut_down };

  // This method returns true for the first compiler thread that reaches that methods.
  // This thread will initialize the compiler runtime.
  bool should_perform_init();

 private:
  const CompilerType _type;

  CompilerStatistics _stats;

 public:
  AbstractCompiler(CompilerType type) : _num_compiler_threads(0), _compiler_state(uninitialized), _type(type) {}

  // This function determines the compiler thread that will perform the
  // shutdown of the corresponding compiler runtime.
  bool should_perform_shutdown();

  // Name of this compiler
  virtual const char* name() = 0;

  // Determine if the current compiler provides an intrinsic
  // for method 'method'. An intrinsic is available if:
  //  - the intrinsic is enabled (by using the appropriate command-line flag,
  //    the command-line compile ommand, or a compiler directive)
  //  - the platform on which the VM is running supports the intrinsic
  //    (i.e., the platform provides the instructions necessary for the compiler
  //    to generate the intrinsic code).
  //
  // The directive provides the compilation context and includes pre-evaluated values
  // dependent on VM flags, compile commands, and compiler directives.
  //
  // Usually, the compilation context is the caller of the method 'method'.
  // The only case when for a non-recursive method 'method' the compilation context
  // is not the caller of the 'method' (but it is the method itself) is
  // java.lang.ref.Referene::get.
  // For java.lang.ref.Reference::get, the intrinsic version is used
  // instead of the compiled version so that the value in the referent
  // field can be registered by the G1 pre-barrier code. The intrinsified
  // version of Reference::get also adds a memory barrier to prevent
  // commoning reads from the referent field across safepoint since GC
  // can change the referent field's value. See Compile::Compile()
  // in src/share/vm/opto/compile.cpp or
  // GraphBuilder::GraphBuilder() in src/share/vm/c1/c1_GraphBuilder.cpp
  // for more details.

  virtual bool is_intrinsic_available(const methodHandle& method, DirectiveSet* directive) {
    return is_intrinsic_supported(method) &&
           !directive->is_intrinsic_disabled(method) &&
           !vmIntrinsics::is_disabled_by_flags(method);
  }

  // Determines if an intrinsic is supported by the compiler, that is,
  // the compiler provides the instructions necessary to generate
  // the intrinsic code for method 'method'.
  //
  // The 'is_intrinsic_supported' method is an allow-list, that is,
  // by default no intrinsics are supported by a compiler except
  // the ones listed in the method. Overriding methods should conform
  // to this behavior.
  virtual bool is_intrinsic_supported(const methodHandle& method) {
    return false;
  }

  // Compiler type queries.
  bool is_c1() const                     { return _type == compiler_c1; }
  bool is_c2() const                     { return _type == compiler_c2; }
  bool is_jvmci() const                  { return _type == compiler_jvmci; }
  CompilerType type() const              { return _type; }

  // Customization
  virtual void initialize () = 0;

  void set_num_compiler_threads(int num) { _num_compiler_threads = num;  }
  int num_compiler_threads()             { return _num_compiler_threads; }

  // Get/set state of compiler objects
  bool is_initialized()           { return _compiler_state == initialized; }
  bool is_failed     ()           { return _compiler_state == failed;}
  void set_state     (int state);
  void set_shut_down ()           { set_state(shut_down); }
  // Compilation entry point for methods
  virtual void compile_method(ciEnv* env, ciMethod* target, int entry_bci, bool install_code, DirectiveSet* directive) {
    ShouldNotReachHere();
  }

  // Print compilation timers and statistics
  virtual void print_timers() {
    ShouldNotReachHere();
  }

  CompilerStatistics* stats() { return &_stats; }
};

#endif // SHARE_COMPILER_ABSTRACTCOMPILER_HPP
