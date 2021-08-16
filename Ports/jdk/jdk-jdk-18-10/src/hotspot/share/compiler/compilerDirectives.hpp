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

#ifndef SHARE_COMPILER_COMPILERDIRECTIVES_HPP
#define SHARE_COMPILER_COMPILERDIRECTIVES_HPP

#include "classfile/vmIntrinsics.hpp"
#include "ci/ciMetadata.hpp"
#include "ci/ciMethod.hpp"
#include "compiler/compiler_globals.hpp"
#include "compiler/methodMatcher.hpp"
#include "compiler/compilerOracle.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/tribool.hpp"

  //      Directives flag name,    type, default value, compile command name
  #define compilerdirectives_common_flags(cflags) \
    cflags(Enable,                  bool, false, Unknown) \
    cflags(Exclude,                 bool, false, Unknown) \
    cflags(BreakAtExecute,          bool, false, BreakAtExecute) \
    cflags(BreakAtCompile,          bool, false, BreakAtCompile) \
    cflags(Log,                     bool, LogCompilation, Unknown) \
    cflags(PrintAssembly,           bool, PrintAssembly, PrintAssembly) \
    cflags(PrintInlining,           bool, PrintInlining, PrintInlining) \
    cflags(PrintNMethods,           bool, PrintNMethods, PrintNMethods) \
    cflags(BackgroundCompilation,   bool, BackgroundCompilation, BackgroundCompilation) \
    cflags(ReplayInline,            bool, false, ReplayInline) \
    cflags(DumpReplay,              bool, false, DumpReplay) \
    cflags(DumpInline,              bool, false, DumpInline) \
    cflags(CompilerDirectivesIgnoreCompileCommands, bool, CompilerDirectivesIgnoreCompileCommands, Unknown) \
    cflags(DisableIntrinsic,        ccstrlist, DisableIntrinsic, DisableIntrinsic) \
    cflags(ControlIntrinsic,        ccstrlist, ControlIntrinsic, ControlIntrinsic) \
    cflags(RepeatCompilation,       intx, RepeatCompilation, RepeatCompilation)

#ifdef COMPILER1
  #define compilerdirectives_c1_flags(cflags)
#else
  #define compilerdirectives_c1_flags(cflags)
#endif

#ifdef COMPILER2
  #define compilerdirectives_c2_flags(cflags) \
    cflags(BlockLayoutByFrequency,  bool, BlockLayoutByFrequency,  BlockLayoutByFrequency) \
    cflags(PrintOptoAssembly,       bool, PrintOptoAssembly, PrintOptoAssembly) \
    cflags(PrintIntrinsics,         bool, PrintIntrinsics, PrintIntrinsics) \
NOT_PRODUCT(cflags(TraceOptoPipelining, bool, TraceOptoPipelining, TraceOptoPipelining)) \
NOT_PRODUCT(cflags(TraceOptoOutput,     bool, TraceOptoOutput, TraceOptoOutput)) \
NOT_PRODUCT(cflags(PrintIdeal,          bool, PrintIdeal, PrintIdeal)) \
    cflags(TraceSpilling,           bool, TraceSpilling, TraceSpilling) \
    cflags(Vectorize,               bool, false, Vectorize) \
    cflags(CloneMapDebug,           bool, false, CloneMapDebug) \
NOT_PRODUCT(cflags(IGVPrintLevel,       intx, PrintIdealGraphLevel, IGVPrintLevel)) \
    cflags(VectorizeDebug,          uintx, 0, VectorizeDebug) \
    cflags(IncrementalInlineForceCleanup, bool, IncrementalInlineForceCleanup, IncrementalInlineForceCleanup) \
    cflags(MaxNodeLimit,            intx, MaxNodeLimit, MaxNodeLimit)
#else
  #define compilerdirectives_c2_flags(cflags)
#endif

class AbstractCompiler;
class CompilerDirectives;
class DirectiveSet;

class DirectivesStack : AllStatic {
private:
  static CompilerDirectives* _top;
  static CompilerDirectives* _bottom;
  static int _depth;

  static void pop_inner(); // no lock version of pop
public:
  static void init();
  static DirectiveSet* getMatchingDirective(const methodHandle& mh, AbstractCompiler* comp);
  static DirectiveSet* getDefaultDirective(AbstractCompiler* comp);
  static void push(CompilerDirectives* directive);
  static void pop(int count);
  static bool check_capacity(int request_size, outputStream* st);
  static void clear();
  static void print(outputStream* st);
  static void release(DirectiveSet* set);
  static void release(CompilerDirectives* dir);
};

class DirectiveSet : public CHeapObj<mtCompiler> {
private:
  InlineMatcher* _inlinematchers;
  CompilerDirectives* _directive;
  TriBoolArray<(size_t)vmIntrinsics::number_of_intrinsics(), int> _intrinsic_control_words;

public:
  DirectiveSet(CompilerDirectives* directive);
  ~DirectiveSet();
  void init_control_intrinsic();
  CompilerDirectives* directive();
  bool parse_and_add_inline(char* str, const char*& error_msg);
  void append_inline(InlineMatcher* m);
  bool should_inline(ciMethod* inlinee);
  bool should_not_inline(ciMethod* inlinee);
  void print_inline(outputStream* st);
  DirectiveSet* compilecommand_compatibility_init(const methodHandle& method);
  bool is_exclusive_copy() { return _directive == NULL; }
  bool matches_inline(const methodHandle& method, int inline_action);
  static DirectiveSet* clone(DirectiveSet const* src);
  bool is_intrinsic_disabled(const methodHandle& method);
  static ccstrlist canonicalize_control_intrinsic(ccstrlist option_value);
  void finalize(outputStream* st);

  typedef enum {
#define enum_of_flags(name, type, dvalue, cc_flag) name##Index,
    compilerdirectives_common_flags(enum_of_flags)
    compilerdirectives_c2_flags(enum_of_flags)
    compilerdirectives_c1_flags(enum_of_flags)
    number_of_flags
  } flags;

 private:
  bool _modified[number_of_flags]; // Records what options where set by a directive

 public:
#define flag_store_definition(name, type, dvalue, cc_flag) type name##Option;
  compilerdirectives_common_flags(flag_store_definition)
  compilerdirectives_c2_flags(flag_store_definition)
  compilerdirectives_c1_flags(flag_store_definition)

// Casting to get the same function signature for all setters. Used from parser.
#define set_function_definition(name, type, dvalue, cc_flag) void set_##name(void* value) { type val = *(type*)value; name##Option = val; _modified[name##Index] = true; }
  compilerdirectives_common_flags(set_function_definition)
  compilerdirectives_c2_flags(set_function_definition)
  compilerdirectives_c1_flags(set_function_definition)

  void print_intx(outputStream* st, ccstr n, intx v, bool mod) { if (mod) { st->print("%s:" INTX_FORMAT " ", n, v); } }
  void print_uintx(outputStream* st, ccstr n, intx v, bool mod) { if (mod) { st->print("%s:" UINTX_FORMAT " ", n, v); } }
  void print_bool(outputStream* st, ccstr n, bool v, bool mod) { if (mod) { st->print("%s:%s ", n, v ? "true" : "false"); } }
  void print_double(outputStream* st, ccstr n, double v, bool mod) { if (mod) { st->print("%s:%f ", n, v); } }
  void print_ccstr(outputStream* st, ccstr n, ccstr v, bool mod) { if (mod) { st->print("%s:%s ", n, v); } }
  void print_ccstrlist(outputStream* st, ccstr n, ccstr v, bool mod) { print_ccstr(st, n, v, mod); }

void print(outputStream* st) {
    print_inline(st);
    st->print("  ");
#define print_function_definition(name, type, dvalue, cc_flag) print_##type(st, #name, this->name##Option, true);
    compilerdirectives_common_flags(print_function_definition)
    compilerdirectives_c2_flags(print_function_definition)
    compilerdirectives_c1_flags(print_function_definition)
    st->cr();
  }
};

// Iterator of ControlIntrinsic=+_id1,-_id2,+_id3,...
//
// If disable_all is set, it accepts DisableIntrinsic and all intrinsic Ids
// appear in the list are disabled. Arguments don't have +/- prefix. eg.
// DisableIntrinsic=_id1,_id2,_id3,...
class ControlIntrinsicIter {
 private:
  bool _enabled;
  char* _token;
  char* _saved_ptr;
  char* _list;
  const bool _disableIntrinsic;
  void next_token();

 public:
  ControlIntrinsicIter(ccstrlist option, bool disable_all = false);
  ~ControlIntrinsicIter();

  bool is_enabled() const { return _enabled; }
  const char* operator*() const { return _token; }

  ControlIntrinsicIter& operator++();
};

class ControlIntrinsicValidator {
 private:
  bool _valid;
  char* _bad;

 public:
  ControlIntrinsicValidator(ccstrlist option, bool disable_all) : _valid(true), _bad(nullptr) {
    for (ControlIntrinsicIter iter(option, disable_all); *iter != NULL && _valid; ++iter) {
      if (vmIntrinsics::_none == vmIntrinsics::find_id(*iter)) {
        const size_t len = MIN2<size_t>(strlen(*iter), 63) + 1;  // cap len to a value we know is enough for all intrinsic names
        _bad = NEW_C_HEAP_ARRAY(char, len, mtCompiler);
        // strncpy always writes len characters. If the source string is shorter, the function fills the remaining bytes with NULLs.
        strncpy(_bad, *iter, len);
        _valid = false;
      }
    }
  }

  ~ControlIntrinsicValidator() {
    if (_bad != NULL) {
      FREE_C_HEAP_ARRAY(char, _bad);
    }
  }

  bool is_valid() const {
    return _valid;
  }

  const char* what() const {
    return _bad;
  }
};

class CompilerDirectives : public CHeapObj<mtCompiler> {
private:
  CompilerDirectives* _next;
  BasicMatcher* _match;
  int _ref_count;

public:

  CompilerDirectives();
  ~CompilerDirectives();

  CompilerDirectives* next();
  void set_next(CompilerDirectives* next) {_next = next; }

  bool match(const methodHandle& method);
  BasicMatcher* match() { return _match; }
  bool add_match(char* str, const char*& error_msg);
  DirectiveSet* get_for(AbstractCompiler *comp);
  void print(outputStream* st);
  bool is_default_directive() { return _next == NULL; }
  void finalize(outputStream* st);

  void inc_refcount();
  void dec_refcount();
  int refcount();

  DirectiveSet* _c1_store;
  DirectiveSet* _c2_store;
};

#endif // SHARE_COMPILER_COMPILERDIRECTIVES_HPP
