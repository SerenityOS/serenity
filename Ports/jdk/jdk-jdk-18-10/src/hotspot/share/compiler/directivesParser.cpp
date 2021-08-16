/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "compiler/compileBroker.hpp"
#include "compiler/directivesParser.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/os.hpp"
#include <string.h>

void DirectivesParser::push_tmp(CompilerDirectives* dir) {
  _tmp_depth++;
  dir->set_next(_tmp_top);
  _tmp_top = dir;
}

CompilerDirectives* DirectivesParser::pop_tmp() {
  if (_tmp_top == NULL) {
    return NULL;
  }
  CompilerDirectives* tmp = _tmp_top;
  _tmp_top = _tmp_top->next();
  tmp->set_next(NULL);
  _tmp_depth--;
  return tmp;
}

void DirectivesParser::clean_tmp() {
  CompilerDirectives* tmp = pop_tmp();
  while (tmp != NULL) {
    delete tmp;
    tmp = pop_tmp();
  }
  assert(_tmp_depth == 0, "Consistency");
}

int DirectivesParser::parse_string(const char* text, outputStream* st) {
  DirectivesParser cd(text, st, false);
  if (cd.valid()) {
    return cd.install_directives();
  } else {
    cd.clean_tmp();
    st->flush();
    st->print_cr("Parsing of compiler directives failed");
    return -1;
  }
}

bool DirectivesParser::has_file() {
  return CompilerDirectivesFile != NULL;
}

bool DirectivesParser::parse_from_flag() {
  return parse_from_file(CompilerDirectivesFile, tty);
}

bool DirectivesParser::parse_from_file(const char* filename, outputStream* st) {
  assert(filename != NULL, "Test before calling this");
  if (!parse_from_file_inner(filename, st)) {
    st->print_cr("Could not load file: %s", filename);
    return false;
  }
  return true;
}

bool DirectivesParser::parse_from_file_inner(const char* filename, outputStream* stream) {
  struct stat st;
  ResourceMark rm;
  if (os::stat(filename, &st) == 0) {
    // found file, open it
    int file_handle = os::open(filename, 0, 0);
    if (file_handle != -1) {
      // read contents into resource array
      char* buffer = NEW_RESOURCE_ARRAY(char, st.st_size+1);
      ssize_t num_read = os::read(file_handle, (char*) buffer, st.st_size);
      if (num_read >= 0) {
        buffer[num_read] = '\0';
        // close file
        os::close(file_handle);
        return parse_string(buffer, stream) > 0;
      }
    }
  }
  return false;
}

int DirectivesParser::install_directives() {
  // Check limit
  if (!DirectivesStack::check_capacity(_tmp_depth, _st)) {
    clean_tmp();
    return 0;
  }

  // Pop from internal temporary stack and push to compileBroker.
  CompilerDirectives* tmp = pop_tmp();
  int i = 0;
  while (tmp != NULL) {
    i++;
    DirectivesStack::push(tmp);
    tmp = pop_tmp();
  }
  if (i == 0) {
    _st->print_cr("No directives in file");
    return 0;
  } else {
    _st->print_cr("%i compiler directives added", i);
    if (CompilerDirectivesPrint) {
      // Print entire directives stack after new has been pushed.
      DirectivesStack::print(_st);
    }
    return i;
  }
}

DirectivesParser::DirectivesParser(const char* text, outputStream* st, bool silent)
: JSON(text, silent, st), depth(0), current_directive(NULL), current_directiveset(NULL), _tmp_top(NULL), _tmp_depth(0) {
#ifndef PRODUCT
  memset(stack, 0, MAX_DEPTH * sizeof(stack[0]));
#endif
  parse();
}

DirectivesParser::~DirectivesParser() {
  assert(_tmp_top == NULL, "Consistency");
  assert(_tmp_depth == 0, "Consistency");
}

const DirectivesParser::key DirectivesParser::keys[] = {
    // name, keytype, allow_array, allowed_mask, set_function
    { "c1",     type_c1,     0, mask(type_directives), NULL, UnknownFlagType },
    { "c2",     type_c2,     0, mask(type_directives), NULL, UnknownFlagType },
    { "match",  type_match,  1, mask(type_directives), NULL, UnknownFlagType },
    { "inline", type_inline, 1, mask(type_directives) | mask(type_c1) | mask(type_c2), NULL, UnknownFlagType },

    // Global flags
    #define common_flag_key(name, type, dvalue, compiler) \
    { #name, type_flag, 0, mask(type_directives) | mask(type_c1) | mask(type_c2), &DirectiveSet::set_##name, type##Flag},
    compilerdirectives_common_flags(common_flag_key)
    compilerdirectives_c2_flags(common_flag_key)
    compilerdirectives_c1_flags(common_flag_key)
    #undef common_flag_key
};

const DirectivesParser::key DirectivesParser::dir_array_key = {
     "top level directives array", type_dir_array, 0, 1 // Lowest bit means allow at top level
};
const DirectivesParser::key DirectivesParser::dir_key = {
   "top level directive", type_directives, 0, mask(type_dir_array) | 1 // Lowest bit means allow at top level
};
const DirectivesParser::key DirectivesParser::value_array_key = {
   "value array", type_value_array, 0, UINT_MAX // Allow all, checked by allow_array on other keys, not by allowed_mask from this key
};

const DirectivesParser::key* DirectivesParser::lookup_key(const char* str, size_t len) {
  for (size_t i = 0; i < (sizeof(keys) / sizeof(keys[0])); i++) {
    if (strncasecmp(keys[i].name, str, len) == 0) {
      return &keys[i];
    }
  }
  return NULL;
}

uint DirectivesParser::mask(keytype kt) {
  return 1 << (kt + 1);
}

bool DirectivesParser::push_key(const char* str, size_t len) {
  bool result = true;
  const key* k = lookup_key(str, len);

  if (k == NULL) {
    // os::strdup
    char* s = NEW_C_HEAP_ARRAY(char, len + 1, mtCompiler);
    strncpy(s, str, len);
    s[len] = '\0';
    error(KEY_ERROR, "No such key: '%s'.", s);
    FREE_C_HEAP_ARRAY(char, s);
    return false;
  }

  return push_key(k);
}

bool DirectivesParser::push_key(const key* k) {
  assert(k->allowedmask != 0, "not allowed anywhere?");

  // Exceeding the stack should not be possible with a valid compiler directive,
  // and an invalid should abort before this happens
  assert(depth < MAX_DEPTH, "exceeded stack depth");
  if (depth >= MAX_DEPTH) {
    error(INTERNAL_ERROR, "Stack depth exceeded.");
    return false;
  }

  assert(stack[depth] == NULL, "element not nulled, something is wrong");

  if (depth == 0 && !(k->allowedmask & 1)) {
    error(KEY_ERROR, "Key '%s' not allowed at top level.", k->name);
    return false;
  }

  if (depth > 0) {
    const key* prev = stack[depth - 1];
    if (!(k->allowedmask & mask(prev->type))) {
      error(KEY_ERROR, "Key '%s' not allowed after '%s' key.", k->name, prev->name);
      return false;
    }
  }

  stack[depth] = k;
  depth++;
  return true;
}

const DirectivesParser::key* DirectivesParser::current_key() {
  assert(depth > 0, "getting key from empty stack");
  if (depth == 0) {
    return NULL;
  }
  return stack[depth - 1];
}

const DirectivesParser::key* DirectivesParser::pop_key() {
  assert(depth > 0, "popping empty stack");
  if (depth == 0) {
    error(INTERNAL_ERROR, "Popping empty stack.");
    return NULL;
  }
  depth--;

  const key* k = stack[depth];
#ifndef PRODUCT
  stack[depth] = NULL;
#endif

  return k;
}

bool DirectivesParser::set_option_flag(JSON_TYPE t, JSON_VAL* v, const key* option_key, DirectiveSet* set) {

  void (DirectiveSet::*test)(void *args);
  test = option_key->set;

  switch (t) {
    case JSON_TRUE:
      if (option_key->flag_type != boolFlag) {
        error(VALUE_ERROR, "Cannot use bool value for an %s flag", flag_type_names[option_key->flag_type]);
        return false;
      } else {
        bool val = true;
        (set->*test)((void *)&val);
      }
      break;

    case JSON_FALSE:
      if (option_key->flag_type != boolFlag) {
        error(VALUE_ERROR, "Cannot use bool value for an %s flag", flag_type_names[option_key->flag_type]);
        return false;
      } else {
        bool val = false;
        (set->*test)((void *)&val);
      }
      break;

    case JSON_NUMBER_INT:
      if (option_key->flag_type == intxFlag) {
        intx ival = v->int_value;
        (set->*test)((void *)&ival);
      } else if (option_key->flag_type == uintxFlag) {
        uintx ival = v->uint_value;
        (set->*test)((void *)&ival);
      } else if (option_key->flag_type == doubleFlag) {
        double dval = (double)v->int_value;
        (set->*test)((void *)&dval);
      } else {
        error(VALUE_ERROR, "Cannot use int value for an %s flag", flag_type_names[option_key->flag_type]);
        return false;
      }
      break;

    case JSON_NUMBER_FLOAT:
      if (option_key->flag_type != doubleFlag) {
        error(VALUE_ERROR, "Cannot use double value for an %s flag", flag_type_names[option_key->flag_type]);
        return false;
      } else {
        double dval = v->double_value;
        (set->*test)((void *)&dval);
      }
      break;

    case JSON_STRING:
      if (option_key->flag_type != ccstrFlag && option_key->flag_type != ccstrlistFlag) {
        error(VALUE_ERROR, "Cannot use string value for a %s flag", flag_type_names[option_key->flag_type]);
        return false;
      } else {
        char* s = NEW_C_HEAP_ARRAY(char, v->str.length+1,  mtCompiler);
        strncpy(s, v->str.start, v->str.length + 1);
        s[v->str.length] = '\0';
        (set->*test)((void *)&s);

        if (strncmp(option_key->name, "ControlIntrinsic", 16) == 0) {
          ControlIntrinsicValidator validator(s, false/*disabled_all*/);

          if (!validator.is_valid()) {
            error(VALUE_ERROR, "Unrecognized intrinsic detected in ControlIntrinsic: %s", validator.what());
            return false;
          }
        } else if (strncmp(option_key->name, "DisableIntrinsic", 16) == 0) {
          ControlIntrinsicValidator validator(s, true/*disabled_all*/);

          if (!validator.is_valid()) {
            error(VALUE_ERROR, "Unrecognized intrinsic detected in DisableIntrinsic: %s", validator.what());
            return false;
          }
        }
      }
      break;

    default:
      assert(0, "Should not reach here.");
    }
  return true;
}

bool DirectivesParser::set_option(JSON_TYPE t, JSON_VAL* v) {

  const key* option_key = pop_key();
  const key* enclosing_key = current_key();

  if (option_key->type == value_array_key.type) {
    // Multi value array, we are really setting the value
    // for the key one step further up.
    option_key = pop_key();
    enclosing_key = current_key();

    // Repush option_key and multi value marker, since
    // we need to keep them until all multi values are set.
    push_key(option_key);
    push_key(&value_array_key);
  }

  switch (option_key->type) {
  case type_flag:
  {
    if (current_directiveset == NULL) {
      assert(depth == 2, "Must not have active directive set");

      if (!set_option_flag(t, v, option_key, current_directive->_c1_store)) {
        return false;
      }
      if (!set_option_flag(t, v, option_key, current_directive->_c2_store)) {
        return false;
      }
    } else {
      assert(depth > 2, "Must have active current directive set");
      if (!set_option_flag(t, v, option_key, current_directiveset)) {
        return false;
      }
    }
    break;
  }

  case type_match:
    if (t != JSON_STRING) {
      error(VALUE_ERROR, "Key of type %s needs a value of type string", option_key->name);
      return false;
    }
    if (enclosing_key->type != type_directives) {
      error(SYNTAX_ERROR, "Match keyword can only exist inside a directive");
      return false;
    }
    {
      char* s = NEW_C_HEAP_ARRAY(char, v->str.length + 1, mtCompiler);
      strncpy(s, v->str.start, v->str.length);
      s[v->str.length] = '\0';

      const char* error_msg = NULL;
      if (!current_directive->add_match(s, error_msg)) {
        assert (error_msg != NULL, "Must have valid error message");
        error(VALUE_ERROR, "Method pattern error: %s", error_msg);
      }
      FREE_C_HEAP_ARRAY(char, s);
    }
    break;

  case type_inline:
    if (t != JSON_STRING) {
      error(VALUE_ERROR, "Key of type %s needs a value of type string", option_key->name);
      return false;
    }
    {
      //char* s = strndup(v->str.start, v->str.length);
      char* s = NEW_C_HEAP_ARRAY(char, v->str.length + 1, mtCompiler);
      strncpy(s, v->str.start, v->str.length);
      s[v->str.length] = '\0';

      const char* error_msg = NULL;
      if (current_directiveset == NULL) {
        if (current_directive->_c1_store->parse_and_add_inline(s, error_msg)) {
          if (!current_directive->_c2_store->parse_and_add_inline(s, error_msg)) {
            assert (error_msg != NULL, "Must have valid error message");
            error(VALUE_ERROR, "Method pattern error: %s", error_msg);
          }
        } else {
          assert (error_msg != NULL, "Must have valid error message");
          error(VALUE_ERROR, "Method pattern error: %s", error_msg);
        }
      } else {
        if (!current_directiveset->parse_and_add_inline(s, error_msg)) {
          assert (error_msg != NULL, "Must have valid error message");
          error(VALUE_ERROR, "Method pattern error: %s", error_msg);
        }
      }
      FREE_C_HEAP_ARRAY(char, s);
    }
    break;

  case type_c1:
    current_directiveset = current_directive->_c1_store;
    if (t != JSON_TRUE && t != JSON_FALSE) {
      error(VALUE_ERROR, "Key of type %s needs a true or false value", option_key->name);
      return false;
    }
    break;

  case type_c2:
    current_directiveset = current_directive->_c2_store;
    if (t != JSON_TRUE && t != JSON_FALSE) {
      error(VALUE_ERROR, "Key of type %s needs a true or false value", option_key->name);
      return false;
    }
    break;

  default:
    break;
  }

  return true;
}

bool DirectivesParser::callback(JSON_TYPE t, JSON_VAL* v, uint rlimit) {
  const key* k;

  if (depth == 0) {
    switch (t) {
      case JSON_ARRAY_BEGIN:
        return push_key(&dir_array_key);

      case JSON_OBJECT_BEGIN:
        // push synthetic dir_array
        push_key(&dir_array_key);
        assert(depth == 1, "Make sure the stack are aligned with the directives");
        break;

      default:
        error(SYNTAX_ERROR, "DirectivesParser can only start with an array containing directive objects, or one single directive.");
        return false;
      }
  }
  if (depth == 1) {
    switch (t) {
      case JSON_OBJECT_BEGIN:
        // Parsing a new directive.
        current_directive = new CompilerDirectives();
        return push_key(&dir_key);

      case JSON_ARRAY_END:
        k = pop_key();

        if (k->type != type_dir_array) {
          error(SYNTAX_ERROR, "Expected end of directives array");
          return false;
        }
        return true;

    default:
      error(SYNTAX_ERROR, "DirectivesParser can only start with an array containing directive objects, or one single directive.");
      return false;
    }
  } else {
    switch (t) {
    case JSON_OBJECT_BEGIN:
      k = current_key();
      switch (k->type) {
      case type_c1:
        current_directiveset = current_directive->_c1_store;
        return true;
      case type_c2:
        current_directiveset = current_directive->_c2_store;
        return true;

      case type_dir_array:
        return push_key(&dir_key);

      default:
        error(SYNTAX_ERROR, "The key '%s' does not allow an object to follow.", k->name);
        return false;
      }
      return false;

    case JSON_OBJECT_END:
      k = pop_key();
      switch (k->type) {
      case type_c1:
      case type_c2:
        // This is how we now if options apply to a single or both directive sets
        current_directiveset = NULL;
        break;

      case type_directives:
        // Check, finish and push to stack!
        if (current_directive->match() == NULL) {
          error(INTERNAL_ERROR, "Directive missing required match.");
          return false;
        }
        current_directive->finalize(_st);
        push_tmp(current_directive);
        current_directive = NULL;
        break;

      default:
        error(INTERNAL_ERROR, "Object end with wrong key type on stack: %s.", k->name);
        ShouldNotReachHere();
        return false;
      }
      return true;

    case JSON_ARRAY_BEGIN:
      k = current_key();
      if (!(k->allow_array_value)) {
        if (k->type == type_dir_array) {
          error(SYNTAX_ERROR, "Array not allowed inside top level array, expected directive object.");
        } else {
          error(VALUE_ERROR, "The key '%s' does not allow an array of values.", k->name);
        }
        return false;
      }
      return push_key(&value_array_key);

    case JSON_ARRAY_END:
      k = pop_key(); // Pop multi value marker
      assert(k->type == value_array_key.type, "array end for level != 0 should terminate multi value");
      k = pop_key(); // Pop key for option that was set
      return true;

    case JSON_KEY:
      return push_key(v->str.start, v->str.length);

    case JSON_STRING:
    case JSON_NUMBER_INT:
    case JSON_NUMBER_FLOAT:
    case JSON_TRUE:
    case JSON_FALSE:
    case JSON_NULL:
      return set_option(t, v);

    default:
      error(INTERNAL_ERROR, "Unknown JSON type: %d.", t);
      ShouldNotReachHere();
      return false;
    }
  }
}

