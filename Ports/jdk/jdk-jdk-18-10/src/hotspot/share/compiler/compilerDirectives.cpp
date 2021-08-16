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

#include "precompiled.hpp"
#include "ci/ciMethod.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "compiler/abstractCompiler.hpp"
#include "compiler/compilerDirectives.hpp"
#include "compiler/compilerOracle.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/globals_extension.hpp"

CompilerDirectives::CompilerDirectives() : _next(NULL), _match(NULL), _ref_count(0) {
  _c1_store = new DirectiveSet(this);
  _c1_store->init_control_intrinsic();
  _c2_store = new DirectiveSet(this);
  _c2_store->init_control_intrinsic();
};

CompilerDirectives::~CompilerDirectives() {
  if (_c1_store != NULL) {
    delete _c1_store;
  }
  if (_c2_store != NULL) {
    delete _c2_store;
  }

  // remove all linked method matchers
  BasicMatcher* tmp = _match;
  while (tmp != NULL) {
    BasicMatcher* next = tmp->next();
    delete tmp;
    tmp = next;
  }
}

void CompilerDirectives::print(outputStream* st) {
  assert(DirectivesStack_lock->owned_by_self(), "");
  if (_match != NULL) {
    st->cr();
    st->print("Directive:");
    if (is_default_directive()) {
      st->print_cr(" (default)");
    } else {
      st->cr();
    }
    st->print(" matching: ");
    _match->print(st);
    BasicMatcher* tmp = _match->next();
    while (tmp != NULL) {
      st->print(", ");
      tmp->print(st);
      tmp = tmp->next();
    }
    st->cr();
  } else {
    assert(0, "There should always be a match");
  }

  if (_c1_store != NULL) {
    st->print_cr(" c1 directives:");
    _c1_store->print(st);
  }
  if (_c2_store != NULL) {
    st->cr();
    st->print_cr(" c2 directives:");
    _c2_store->print(st);
  }
  //---
}

void CompilerDirectives::finalize(outputStream* st) {
  if (_c1_store != NULL) {
    _c1_store->finalize(st);
  }
  if (_c2_store != NULL) {
    _c2_store->finalize(st);
  }
}

void DirectiveSet::finalize(outputStream* st) {
  // Check LogOption and warn
  if (LogOption && !LogCompilation) {
    st->print_cr("Warning:  +LogCompilation must be set to enable compilation logging from directives");
  }
  if (PrintAssemblyOption && FLAG_IS_DEFAULT(DebugNonSafepoints)) {
    warning("printing of assembly code is enabled; turning on DebugNonSafepoints to gain additional output");
    DebugNonSafepoints = true;
  }

  // if any flag has been modified - set directive as enabled
  // unless it already has been explicitly set.
  if (!_modified[EnableIndex]) {
    if (_inlinematchers != NULL) {
      EnableOption = true;
      return;
    }
    int i;
    for (i = 0; i < number_of_flags; i++) {
      if (_modified[i]) {
        EnableOption = true;
        return;
      }
    }
  }
}

CompilerDirectives* CompilerDirectives::next() {
  return _next;
}

bool CompilerDirectives::match(const methodHandle& method) {
  if (is_default_directive()) {
    return true;
  }
  if (method == NULL) {
    return false;
  }
  if (_match->match(method)) {
    return true;
  }
  return false;
}

bool CompilerDirectives::add_match(char* str, const char*& error_msg) {
  BasicMatcher* bm = BasicMatcher::parse_method_pattern(str, error_msg, false);
  if (bm == NULL) {
    assert(error_msg != NULL, "Must have error message");
    return false;
  } else {
    bm->set_next(_match);
    _match = bm;
    return true;
  }
}

void CompilerDirectives::inc_refcount() {
  assert(DirectivesStack_lock->owned_by_self(), "");
  _ref_count++;
}

void CompilerDirectives::dec_refcount() {
  assert(DirectivesStack_lock->owned_by_self(), "");
  _ref_count--;
}

int CompilerDirectives::refcount() {
  assert(DirectivesStack_lock->owned_by_self(), "");
  return _ref_count;
}

DirectiveSet* CompilerDirectives::get_for(AbstractCompiler *comp) {
  assert(DirectivesStack_lock->owned_by_self(), "");
  if (comp == NULL) { // Xint
    return _c1_store;
  } else  if (comp->is_c2()) {
    return _c2_store;
  } else {
    // use c1_store as default
    assert(comp->is_c1() || comp->is_jvmci(), "");
    return _c1_store;
  }
}

// In the list of Control/disabled intrinsics, the ID of the control intrinsics can separated:
// - by ',' (if -XX:Control/DisableIntrinsic is used once when invoking the VM) or
// - by '\n' (if -XX:Control/DisableIntrinsic is used multiple times when invoking the VM) or
// - by ' ' (if Control/DisableIntrinsic is used on a per-method level, e.g., with CompileCommand).
//
// To simplify the processing of the list, the canonicalize_control_intrinsic() method
// returns a new copy of the list in which '\n' and ' ' is replaced with ','.
ccstrlist DirectiveSet::canonicalize_control_intrinsic(ccstrlist option_value) {
  char* canonicalized_list = NEW_C_HEAP_ARRAY(char, strlen(option_value) + 1, mtCompiler);
  int i = 0;
  char current;
  while ((current = option_value[i]) != '\0') {
    if (current == '\n' || current == ' ') {
      canonicalized_list[i] = ',';
    } else {
      canonicalized_list[i] = current;
    }
    i++;
  }
  canonicalized_list[i] = '\0';
  return canonicalized_list;
}

ControlIntrinsicIter::ControlIntrinsicIter(ccstrlist option_value, bool disable_all)
  : _disableIntrinsic(disable_all) {
  _list = (char*)DirectiveSet::canonicalize_control_intrinsic(option_value);
  _saved_ptr = _list;
  _enabled = false;

  _token = strtok_r(_saved_ptr, ",", &_saved_ptr);
  next_token();
}

ControlIntrinsicIter::~ControlIntrinsicIter() {
  FREE_C_HEAP_ARRAY(char, _list);
}

// pre-increment
ControlIntrinsicIter& ControlIntrinsicIter::operator++() {
  _token = strtok_r(NULL, ",", &_saved_ptr);
  next_token();
  return *this;
}

void ControlIntrinsicIter::next_token() {
  if (_token && !_disableIntrinsic) {
    char ch = _token[0];

    if (ch != '+' && ch != '-') {
      warning("failed to parse %s. must start with +/-!", _token);
    } else {
      _enabled = ch == '+';
      _token++;
    }
  }
}

void DirectiveSet::init_control_intrinsic() {
  for (ControlIntrinsicIter iter(ControlIntrinsic); *iter != NULL; ++iter) {
    vmIntrinsics::ID id = vmIntrinsics::find_id(*iter);

    if (id != vmIntrinsics::_none) {
      _intrinsic_control_words[vmIntrinsics::as_int(id)] = iter.is_enabled();
    }
  }

  // Order matters, DisableIntrinsic can overwrite ControlIntrinsic
  for (ControlIntrinsicIter iter(DisableIntrinsic, true/*disable_all*/); *iter != NULL; ++iter) {
    vmIntrinsics::ID id = vmIntrinsics::find_id(*iter);

    if (id != vmIntrinsics::_none) {
      _intrinsic_control_words[vmIntrinsics::as_int(id)] = false;
    }
  }
}

DirectiveSet::DirectiveSet(CompilerDirectives* d) :_inlinematchers(NULL), _directive(d) {
#define init_defaults_definition(name, type, dvalue, compiler) this->name##Option = dvalue;
  compilerdirectives_common_flags(init_defaults_definition)
  compilerdirectives_c2_flags(init_defaults_definition)
  compilerdirectives_c1_flags(init_defaults_definition)
  memset(_modified, 0, sizeof(_modified));
  _intrinsic_control_words.fill_in(/*default value*/TriBool());
}

DirectiveSet::~DirectiveSet() {
  // remove all linked methodmatchers
  InlineMatcher* tmp = _inlinematchers;
  while (tmp != NULL) {
    InlineMatcher* next = tmp->next();
    delete tmp;
    tmp = next;
  }
}

// A smart pointer of DirectiveSet. It uses Copy-on-Write strategy to avoid cloning.
// It provides 2 accesses of the underlying raw pointer.
// 1) operator->() returns a pointer to a constant DirectiveSet. It's read-only.
// 2) cloned() returns a pointer that points to the cloned DirectiveSet.
// Users should only use cloned() when they need to update DirectiveSet.
//
// In the end, users need to invoke commit() to finalize the pending changes.
// If cloning happens, the smart pointer will return the new pointer after releasing the original
// one on DirectivesStack. If cloning doesn't happen, it returns the original intact pointer.
class DirectiveSetPtr {
 private:
  DirectiveSet* _origin;
  DirectiveSet* _clone;
  NONCOPYABLE(DirectiveSetPtr);

 public:
  DirectiveSetPtr(DirectiveSet* origin): _origin(origin), _clone(nullptr) {
    assert(origin != nullptr, "DirectiveSetPtr cannot be initialized with a NULL pointer.");
  }

  DirectiveSet const* operator->() {
    return (_clone == nullptr) ? _origin : _clone;
  }

  DirectiveSet* cloned() {
    if (_clone == nullptr) {
      _clone = DirectiveSet::clone(_origin);
    }
    return _clone;
  }

  DirectiveSet* commit() {
    if (_clone != nullptr) {
      // We are returning a (parentless) copy. The originals parent don't need to account for this.
      DirectivesStack::release(_origin);
      _origin = _clone;
      _clone = nullptr;
    }

    return _origin;
  }
};

// Backward compatibility for CompileCommands
// Breaks the abstraction and causes lots of extra complexity
// - if some option is changed we need to copy directiveset since it no longer can be shared
// - Need to free copy after use
// - Requires a modified bit so we don't overwrite options that is set by directives
DirectiveSet* DirectiveSet::compilecommand_compatibility_init(const methodHandle& method) {
  // Early bail out - checking all options is expensive - we rely on them not being used
  // Only set a flag if it has not been modified and value changes.
  // Only copy set if a flag needs to be set
  if (!CompilerDirectivesIgnoreCompileCommandsOption && CompilerOracle::has_any_command_set()) {
    DirectiveSetPtr set(this);

    // All CompileCommands are not equal so this gets a bit verbose
    // When CompileCommands have been refactored less clutter will remain.
    if (CompilerOracle::should_break_at(method)) {
      if (!_modified[BreakAtCompileIndex]) {
        set.cloned()->BreakAtCompileOption = true;
      }
      if (!_modified[BreakAtExecuteIndex]) {
        set.cloned()->BreakAtExecuteOption = true;
      }
    }
    if (!_modified[LogIndex]) {
      bool log = CompilerOracle::should_log(method);
      if (log != set->LogOption) {
        set.cloned()->LogOption = log;
      }
    }

    if (CompilerOracle::should_print(method)) {
      if (!_modified[PrintAssemblyIndex]) {
        set.cloned()->PrintAssemblyOption = true;
      }
    }
    // Exclude as in should not compile == Enabled
    if (CompilerOracle::should_exclude(method)) {
      if (!_modified[ExcludeIndex]) {
        set.cloned()->ExcludeOption = true;
      }
    }

    // inline and dontinline (including exclude) are implemented in the directiveset accessors
#define init_default_cc(name, type, dvalue, cc_flag) { type v; if (!_modified[name##Index] && CompileCommand::cc_flag != CompileCommand::Unknown && CompilerOracle::has_option_value(method, CompileCommand::cc_flag, v) && v != this->name##Option) { set.cloned()->name##Option = v; } }
    compilerdirectives_common_flags(init_default_cc)
    compilerdirectives_c2_flags(init_default_cc)
    compilerdirectives_c1_flags(init_default_cc)

    // Canonicalize DisableIntrinsic to contain only ',' as a separator.
    ccstrlist option_value;
    bool need_reset = true; // if Control/DisableIntrinsic redefined, only need to reset control_words once

    if (!_modified[ControlIntrinsicIndex] &&
        CompilerOracle::has_option_value(method, CompileCommand::ControlIntrinsic, option_value)) {
      ControlIntrinsicIter iter(option_value);

      if (need_reset) {
        set.cloned()->_intrinsic_control_words.fill_in(TriBool());
        need_reset = false;
      }

      while (*iter != NULL) {
        vmIntrinsics::ID id = vmIntrinsics::find_id(*iter);
        if (id != vmIntrinsics::_none) {
          set.cloned()->_intrinsic_control_words[vmIntrinsics::as_int(id)] = iter.is_enabled();
        }

        ++iter;
      }
    }


    if (!_modified[DisableIntrinsicIndex] &&
        CompilerOracle::has_option_value(method, CompileCommand::DisableIntrinsic, option_value)) {
      ControlIntrinsicIter iter(option_value, true/*disable_all*/);

      if (need_reset) {
        set.cloned()->_intrinsic_control_words.fill_in(TriBool());
        need_reset = false;
      }

      while (*iter != NULL) {
        vmIntrinsics::ID id = vmIntrinsics::find_id(*iter);
        if (id != vmIntrinsics::_none) {
          set.cloned()->_intrinsic_control_words[vmIntrinsics::as_int(id)] = false;
        }

        ++iter;
      }
    }

    return set.commit();
  }
  // Nothing changed
  return this;
}

CompilerDirectives* DirectiveSet::directive() {
  assert(_directive != NULL, "Must have been initialized");
  return _directive;
}

bool DirectiveSet::matches_inline(const methodHandle& method, int inline_action) {
  if (_inlinematchers != NULL) {
    if (_inlinematchers->match(method, inline_action)) {
      return true;
    }
  }
  return false;
}

bool DirectiveSet::should_inline(ciMethod* inlinee) {
  inlinee->check_is_loaded();
  VM_ENTRY_MARK;
  methodHandle mh(THREAD, inlinee->get_Method());

  if (_inlinematchers != NULL) {
    return matches_inline(mh, InlineMatcher::force_inline);
  }
  if (!CompilerDirectivesIgnoreCompileCommandsOption) {
    return CompilerOracle::should_inline(mh);
  }
  return false;
}

bool DirectiveSet::should_not_inline(ciMethod* inlinee) {
  inlinee->check_is_loaded();
  VM_ENTRY_MARK;
  methodHandle mh(THREAD, inlinee->get_Method());

  if (_inlinematchers != NULL) {
    return matches_inline(mh, InlineMatcher::dont_inline);
  }
  if (!CompilerDirectivesIgnoreCompileCommandsOption) {
    return CompilerOracle::should_not_inline(mh);
  }
  return false;
}

bool DirectiveSet::parse_and_add_inline(char* str, const char*& error_msg) {
  InlineMatcher* m = InlineMatcher::parse_inline_pattern(str, error_msg);
  if (m != NULL) {
    // add matcher last in chain - the order is significant
    append_inline(m);
    return true;
  } else {
    assert(error_msg != NULL, "Error message must be set");
    return false;
  }
}

void DirectiveSet::append_inline(InlineMatcher* m) {
  if (_inlinematchers == NULL) {
    _inlinematchers = m;
    return;
  }
  InlineMatcher* tmp = _inlinematchers;
  while (tmp->next() != NULL) {
    tmp = tmp->next();
  }
  tmp->set_next(m);
}

void DirectiveSet::print_inline(outputStream* st) {
  if (_inlinematchers == NULL) {
    st->print_cr("  inline: -");
  } else {
    st->print("  inline: ");
    _inlinematchers->print(st);
    InlineMatcher* tmp = _inlinematchers->next();
    while (tmp != NULL) {
      st->print(", ");
      tmp->print(st);
      tmp = tmp->next();
    }
    st->cr();
  }
}

bool DirectiveSet::is_intrinsic_disabled(const methodHandle& method) {
  vmIntrinsics::ID id = method->intrinsic_id();
  assert(id > vmIntrinsics::_none && id < vmIntrinsics::ID_LIMIT, "invalid intrinsic_id!");

  TriBool b = _intrinsic_control_words[vmIntrinsics::as_int(id)];
  if (b.is_default()) {
    return false; // if unset, every intrinsic is enabled.
  } else {
    return !b;
  }
}

DirectiveSet* DirectiveSet::clone(DirectiveSet const* src) {
  DirectiveSet* set = new DirectiveSet(NULL);
  // Ordinary allocations of DirectiveSet would call init_control_intrinsic()
  // immediately to create a new copy for set->Control/DisableIntrinsicOption.
  // However, here it does not need to because the code below creates
  // a copy of src->Control/DisableIntrinsicOption that initializes
  // set->Control/DisableIntrinsicOption.

  memcpy(set->_modified, src->_modified, sizeof(src->_modified));

  InlineMatcher* tmp = src->_inlinematchers;
  while (tmp != NULL) {
    set->append_inline(tmp->clone());
    tmp = tmp->next();
  }

  #define copy_members_definition(name, type, dvalue, cc_flag) set->name##Option = src->name##Option;
    compilerdirectives_common_flags(copy_members_definition)
    compilerdirectives_c2_flags(copy_members_definition)
    compilerdirectives_c1_flags(copy_members_definition)

  set->_intrinsic_control_words = src->_intrinsic_control_words;
  return set;
}

// Create a new dirstack and push a default directive
void DirectivesStack::init() {
  CompilerDirectives* _default_directives = new CompilerDirectives();
  char str[] = "*.*";
  const char* error_msg = NULL;
  _default_directives->add_match(str, error_msg);
#if defined(COMPILER1) || INCLUDE_JVMCI
  _default_directives->_c1_store->EnableOption = true;
#endif
#ifdef COMPILER2
  if (CompilerConfig::is_c2_enabled()) {
    _default_directives->_c2_store->EnableOption = true;
  }
#endif
  assert(error_msg == NULL, "Must succeed.");
  push(_default_directives);
}

DirectiveSet* DirectivesStack::getDefaultDirective(AbstractCompiler* comp) {
  MutexLocker locker(DirectivesStack_lock, Mutex::_no_safepoint_check_flag);

  assert(_bottom != NULL, "Must never be empty");
  _bottom->inc_refcount();
  return _bottom->get_for(comp);
}

void DirectivesStack::push(CompilerDirectives* directive) {
  MutexLocker locker(DirectivesStack_lock, Mutex::_no_safepoint_check_flag);

  directive->inc_refcount();
  if (_top == NULL) {
    assert(_bottom == NULL, "There can only be one default directive");
    _bottom = directive; // default directive, can never be removed.
  }

  directive->set_next(_top);
  _top = directive;
  _depth++;
}

void DirectivesStack::pop(int count) {
  MutexLocker locker(DirectivesStack_lock, Mutex::_no_safepoint_check_flag);
  assert(count > -1, "No negative values");
  for (int i = 0; i < count; i++) {
    pop_inner();
  }
}

void DirectivesStack::pop_inner() {
  assert(DirectivesStack_lock->owned_by_self(), "");

  if (_top->next() == NULL) {
    return; // Do nothing - don't allow an empty stack
  }
  CompilerDirectives* tmp = _top;
  _top = _top->next();
  _depth--;

  DirectivesStack::release(tmp);
}

bool DirectivesStack::check_capacity(int request_size, outputStream* st) {
  if ((request_size + _depth) > CompilerDirectivesLimit) {
    st->print_cr("Could not add %i more directives. Currently %i/%i directives.", request_size, _depth, CompilerDirectivesLimit);
    return false;
  }
  return true;
}

void DirectivesStack::clear() {
  // holding the lock during the whole operation ensuring consistent result
  MutexLocker locker(DirectivesStack_lock, Mutex::_no_safepoint_check_flag);
  while (_top->next() != NULL) {
    pop_inner();
  }
}

void DirectivesStack::print(outputStream* st) {
  MutexLocker locker(DirectivesStack_lock, Mutex::_no_safepoint_check_flag);
  CompilerDirectives* tmp = _top;
  while (tmp != NULL) {
    tmp->print(st);
    tmp = tmp->next();
    st->cr();
  }
}

void DirectivesStack::release(DirectiveSet* set) {
  assert(set != NULL, "Never NULL");
  MutexLocker locker(DirectivesStack_lock, Mutex::_no_safepoint_check_flag);
  if (set->is_exclusive_copy()) {
    // Old CompilecCmmands forced us to create an exclusive copy
    delete set;
  } else {
    assert(set->directive() != NULL, "Never NULL");
    release(set->directive());
  }
}


void DirectivesStack::release(CompilerDirectives* dir) {
  assert(DirectivesStack_lock->owned_by_self(), "");
  dir->dec_refcount();
  if (dir->refcount() == 0) {
    delete dir;
  }
}

DirectiveSet* DirectivesStack::getMatchingDirective(const methodHandle& method, AbstractCompiler *comp) {
  assert(_depth > 0, "Must never be empty");

  DirectiveSet* match = NULL;
  {
    MutexLocker locker(DirectivesStack_lock, Mutex::_no_safepoint_check_flag);

    CompilerDirectives* dir = _top;
    assert(dir != NULL, "Must be initialized");

    while (dir != NULL) {
      if (dir->is_default_directive() || dir->match(method)) {
        match = dir->get_for(comp);
        assert(match != NULL, "Consistency");
        if (match->EnableOption) {
          // The directiveSet for this compile is also enabled -> success
          dir->inc_refcount();
          break;
        }
      }
      dir = dir->next();
    }
  }
  guarantee(match != NULL, "There should always be a default directive that matches");

  // Check for legacy compile commands update, without DirectivesStack_lock
  return match->compilecommand_compatibility_init(method);
}
