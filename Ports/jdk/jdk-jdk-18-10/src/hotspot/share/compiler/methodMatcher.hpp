/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_COMPILER_METHODMATCHER_HPP
#define SHARE_COMPILER_METHODMATCHER_HPP

#include "memory/allocation.hpp"
#include "runtime/handles.hpp"
#include "memory/resourceArea.hpp"

class MethodMatcher : public CHeapObj<mtCompiler> {
 public:
  enum Mode {
    Exact,
    Prefix = 1,
    Suffix = 2,
    Substring = Prefix | Suffix,
    Any,
    Unknown = -1
  };

 protected:
  Symbol*        _class_name;
  Symbol*        _method_name;
  Symbol*        _signature;
  Mode           _class_mode;
  Mode           _method_mode;

 public:
  Symbol* class_name() const { return _class_name; }
  Mode class_mode() const { return _class_mode; }
  Symbol* method_name() const { return _method_name; }
  Mode method_mode() const { return _method_mode; }
  Symbol* signature() const { return _signature; }

  MethodMatcher();
  ~MethodMatcher();

  void init(Symbol* class_name, Mode class_mode, Symbol* method_name, Mode method_mode, Symbol* signature);
  static void parse_method_pattern(char*& line, const char*& error_msg, MethodMatcher* m);
  static void print_symbol(outputStream* st, Symbol* h, Mode mode);
  bool matches(const methodHandle& method) const;
  void print_base(outputStream* st);

 private:
  static bool canonicalize(char * line, const char *& error_msg);
  bool match(Symbol* candidate, Symbol* match, Mode match_mode) const;
};

class BasicMatcher : public MethodMatcher {
private:
  BasicMatcher* _next;
public:

  BasicMatcher() : MethodMatcher(),
    _next(NULL) {
  }

  BasicMatcher(BasicMatcher* next) :
    _next(next) {
  }

  static BasicMatcher* parse_method_pattern(char* line, const char*& error_msg, bool expect_trailing_chars);
  bool match(const methodHandle& method);
  void set_next(BasicMatcher* next) { _next = next; }
  BasicMatcher* next() { return _next; }

  void print(outputStream* st) { print_base(st); }
  void print_all(outputStream* st) {
    print_base(st);
    if (_next != NULL) {
      _next->print_all(st);
    }
  }
};

class InlineMatcher : public MethodMatcher {
public:
  enum InlineType {
      unknown_inline,
      dont_inline,
      force_inline
    };

private:
  InlineType _inline_action;
  InlineMatcher * _next;

  InlineMatcher() : MethodMatcher(),
    _inline_action(unknown_inline), _next(NULL) {
  }

public:
  static InlineMatcher* parse_method_pattern(char* line, const char*& error_msg);
  bool match(const methodHandle& method, int inline_action);
  void print(outputStream* st);
  void set_next(InlineMatcher* next) { _next = next; }
  InlineMatcher* next() { return _next; }
  void set_action(InlineType inline_action) { _inline_action = inline_action; }
  int inline_action() { return _inline_action; }
  static InlineMatcher* parse_inline_pattern(char* line, const char*& error_msg);
  InlineMatcher* clone();
};

#endif // SHARE_COMPILER_METHODMATCHER_HPP
