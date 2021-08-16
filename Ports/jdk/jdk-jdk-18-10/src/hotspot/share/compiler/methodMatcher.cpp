/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/symbolTable.hpp"
#include "classfile/vmSymbols.hpp"
#include "compiler/compilerOracle.hpp"
#include "compiler/methodMatcher.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "oops/method.hpp"
#include "oops/oop.inline.hpp"

// The JVM specification defines the allowed characters.
// Tokens that are disallowed by the JVM specification can have
// a meaning to the parser so we need to include them here.
// The parser does not enforce all rules of the JVMS - a successful parse
// does not mean that it is an allowed name. Illegal names will
// be ignored since they never can match a class or method.
//
// '\0' and 0xf0-0xff are disallowed in constant string values
// 0x20 ' ', 0x09 '\t' and, 0x2c ',' are used in the matching
// 0x5b '[' and 0x5d ']' can not be used because of the matcher
// 0x28 '(' and 0x29 ')' are used for the signature
// 0x2e '.' is always replaced before the matching
// 0x2f '/' is only used in the class name as package separator

#define RANGEBASE "\x1\x2\x3\x4\x5\x6\x7\x8\xa\xb\xc\xd\xe\xf" \
    "\x10\x11\x12\x13\x14\x15\x16\x17\x18\x19\x1a\x1b\x1c\x1d\x1e\x1f" \
    "\x21\x22\x23\x24\x25\x26\x27\x2a\x2b\x2c\x2d" \
    "\x30\x31\x32\x33\x34\x35\x36\x37\x38\x39\x3a\x3b\x3c\x3d\x3e\x3f" \
    "\x40\x41\x42\x43\x44\x45\x46\x47\x48\x49\x4a\x4b\x4c\x4d\x4e\x4f" \
    "\x50\x51\x52\x53\x54\x55\x56\x57\x58\x59\x5a\x5c\x5e\x5f" \
    "\x60\x61\x62\x63\x64\x65\x66\x67\x68\x69\x6a\x6b\x6c\x6d\x6e\x6f" \
    "\x70\x71\x72\x73\x74\x75\x76\x77\x78\x79\x7a\x7b\x7c\x7d\x7e\x7f" \
    "\x80\x81\x82\x83\x84\x85\x86\x87\x88\x89\x8a\x8b\x8c\x8d\x8e\x8f" \
    "\x90\x91\x92\x93\x94\x95\x96\x97\x98\x99\x9a\x9b\x9c\x9d\x9e\x9f" \
    "\xa0\xa1\xa2\xa3\xa4\xa5\xa6\xa7\xa8\xa9\xaa\xab\xac\xad\xae\xaf" \
    "\xb0\xb1\xb2\xb3\xb4\xb5\xb6\xb7\xb8\xb9\xba\xbb\xbc\xbd\xbe\xbf" \
    "\xc0\xc1\xc2\xc3\xc4\xc5\xc6\xc7\xc8\xc9\xca\xcb\xcc\xcd\xce\xcf" \
    "\xd0\xd1\xd2\xd3\xd4\xd5\xd6\xd7\xd8\xd9\xda\xdb\xdc\xdd\xde\xdf" \
    "\xe0\xe1\xe2\xe3\xe4\xe5\xe6\xe7\xe8\xe9\xea\xeb\xec\xed\xee\xef"

#define RANGE0 "[*" RANGEBASE "]"
#define RANGESLASH "[*" RANGEBASE "/]"

MethodMatcher::MethodMatcher():
    _class_name(NULL)
  , _method_name(NULL)
  , _signature(NULL)
  , _class_mode(Exact)
  , _method_mode(Exact) {
}

MethodMatcher::~MethodMatcher() {
  if (_class_name != NULL) {
    _class_name->decrement_refcount();
  }
  if (_method_name != NULL) {
    _method_name->decrement_refcount();
  }
  if (_signature != NULL) {
    _signature->decrement_refcount();
  }
}

void MethodMatcher::init(Symbol* class_name, Mode class_mode,
                             Symbol* method_name, Mode method_mode,
                             Symbol* signature) {
 _class_mode = class_mode;
 _method_mode = method_mode;
 _class_name = class_name;
 _method_name = method_name;
 _signature = signature;
}

bool MethodMatcher::canonicalize(char * line, const char *& error_msg) {
  char* colon = strstr(line, "::");
  bool have_colon = (colon != NULL);
  if (have_colon) {
    // Don't allow multiple '::'
    if (colon[2] != '\0') {
      if (strstr(colon+2, "::")) {
        error_msg = "Method pattern only allows one '::' allowed";
        return false;
      }
    }

    char* pos = line;
    if (pos != NULL) {
      for (char* lp = pos + 1; *lp != '\0'; lp++) {
        if (*lp == '(') {
          break;
        }

        if (*lp == '/') {
          error_msg = "Method pattern uses '/' together with '::' (tips: replace '/' with '+' for hidden classes)";
          return false;
        }
      }
    }
  } else {
    // Don't allow mixed package separators
    char* pos = strchr(line, '.');
    bool in_signature = false;
    if (pos != NULL) {
      for (char* lp = pos + 1; *lp != '\0'; lp++) {
        if (*lp == '(') {
          in_signature = true;
        }

        // After any comma the method pattern has ended
        if (*lp == ',') {
          break;
        }

        if (!in_signature && (*lp == '/')) {
          error_msg = "Method pattern uses mixed '/' and '.' package separators";
          return false;
        }

        if (*lp == '.') {
          error_msg = "Method pattern uses multiple '.' in pattern";
          return false;
        }
      }
    }
  }

  for (char* lp = line; *lp != '\0'; lp++) {
    // Allow '.' to separate the class name from the method name.
    // This is the preferred spelling of methods:
    //      exclude java/lang/String.indexOf(I)I
    // Allow ',' for spaces (eases command line quoting).
    //      exclude,java/lang/String.indexOf
    // For backward compatibility, allow space as separator also.
    //      exclude java/lang/String indexOf
    //      exclude,java/lang/String,indexOf
    // For easy cut-and-paste of method names, allow VM output format
    // as produced by Method::print_short_name:
    //      exclude java.lang.String::indexOf
    // For simple implementation convenience here, convert them all to space.

    if (have_colon) {
      if (*lp == '.')  *lp = '/';   // dots build the package prefix
      if (*lp == ':')  *lp = ' ';
    }
    if (*lp == ',' || *lp == '.')  *lp = ' ';
  }
  return true;
}

bool MethodMatcher::match(Symbol* candidate, Symbol* match, Mode match_mode) const {
  if (match_mode == Any) {
    return true;
  }

  if (match_mode == Exact) {
    return candidate == match;
  }

  ResourceMark rm;
  const char * candidate_string = candidate->as_C_string();
  const char * match_string = match->as_C_string();

  switch (match_mode) {
  case Prefix:
    return strstr(candidate_string, match_string) == candidate_string;

  case Suffix: {
    size_t clen = strlen(candidate_string);
    size_t mlen = strlen(match_string);
    return clen >= mlen && strcmp(candidate_string + clen - mlen, match_string) == 0;
  }

  case Substring:
    return strstr(candidate_string, match_string) != NULL;

  default:
    return false;
  }
}

static MethodMatcher::Mode check_mode(char name[], const char*& error_msg) {
  int match = MethodMatcher::Exact;
  if (name[0] == '*') {
    if (strlen(name) == 1) {
      return MethodMatcher::Any;
    }
    match |= MethodMatcher::Suffix;
    memmove(name, name + 1, strlen(name + 1) + 1);
  }

  size_t len = strlen(name);
  if (len > 0 && name[len - 1] == '*') {
    match |= MethodMatcher::Prefix;
    name[--len] = '\0';
  }

  if (strlen(name) == 0) {
    error_msg = "** Not a valid pattern";
    return MethodMatcher::Any;
  }

  if (strstr(name, "*") != NULL) {
    error_msg = " Embedded * not allowed";
    return MethodMatcher::Unknown;
  }
  return (MethodMatcher::Mode)match;
}

// Skip any leading spaces
void skip_leading_spaces(char*& line, int* total_bytes_read ) {
  int bytes_read = 0;
  sscanf(line, "%*[ \t]%n", &bytes_read);
  if (bytes_read > 0) {
    line += bytes_read;
    *total_bytes_read += bytes_read;
  }
}

PRAGMA_DIAG_PUSH
// warning C4189: The file contains a character that cannot be represented
//                in the current code page
PRAGMA_DISABLE_MSVC_WARNING(4819)
void MethodMatcher::parse_method_pattern(char*& line, const char*& error_msg, MethodMatcher* matcher) {
  MethodMatcher::Mode c_match;
  MethodMatcher::Mode m_match;
  char class_name[256] = {0};
  char method_name[256] = {0};
  char sig[1024] = {0};
  int bytes_read = 0;
  int total_bytes_read = 0;

  assert(error_msg == NULL, "Dont call here with error_msg already set");

  if (!MethodMatcher::canonicalize(line, error_msg)) {
    assert(error_msg != NULL, "Message must be set if parsing failed");
    return;
  }

  skip_leading_spaces(line, &total_bytes_read);
  if (*line == '\0') {
    error_msg = "Method pattern missing from command";
    return;
  }

  if (2 == sscanf(line, "%255" RANGESLASH "%*[ ]" "%255"  RANGE0 "%n", class_name, method_name, &bytes_read)) {
    c_match = check_mode(class_name, error_msg);
    m_match = check_mode(method_name, error_msg);

    // Over-consumption
    // method_name points to an option type or option name because the method name is not specified by users.
    // In very rare case, the method name happens to be same as option type/name, so look ahead to make sure
    // it doesn't show up again.
    if ((OptionType::Unknown != CompilerOracle::parse_option_type(method_name) ||
        CompileCommand::Unknown != CompilerOracle::parse_option_name(method_name)) &&
        *(line + bytes_read) != '\0' &&
        strstr(line + bytes_read, method_name) == NULL) {
      error_msg = "Did not specify any method name";
      method_name[0] = '\0';
      return;
    }

    if ((strchr(class_name, JVM_SIGNATURE_SPECIAL) != NULL) ||
        (strchr(class_name, JVM_SIGNATURE_ENDSPECIAL) != NULL)) {
      error_msg = "Chars '<' and '>' not allowed in class name";
      return;
    }

    if ((strchr(method_name, JVM_SIGNATURE_SPECIAL) != NULL) ||
        (strchr(method_name, JVM_SIGNATURE_ENDSPECIAL) != NULL)) {
      if (!vmSymbols::object_initializer_name()->equals(method_name) &&
          !vmSymbols::class_initializer_name()->equals(method_name)) {
        error_msg = "Chars '<' and '>' only allowed in <init> and <clinit>";
        return;
      }
    }

    if (c_match == MethodMatcher::Unknown || m_match == MethodMatcher::Unknown) {
      assert(error_msg != NULL, "Must have been set by check_mode()");
      return;
    }

    EXCEPTION_MARK;
    Symbol* signature = NULL;
    line += bytes_read;
    bytes_read = 0;

    skip_leading_spaces(line, &total_bytes_read);

    // there might be a signature following the method.
    // signatures always begin with ( so match that by hand
    if (line[0] == '(') {
      line++;
      sig[0] = '(';
      // scan the rest
      if (1 == sscanf(line, "%1022[[);/" RANGEBASE "]%n", sig+1, &bytes_read)) {
        if (strchr(sig, '*') != NULL) {
          error_msg = " Wildcard * not allowed in signature";
          return;
        }
        line += bytes_read;
      }
      signature = SymbolTable::new_symbol(sig);
    }
    Symbol* c_name = SymbolTable::new_symbol(class_name);
    Symbol* m_name = SymbolTable::new_symbol(method_name);

    matcher->init(c_name, c_match, m_name, m_match, signature);
    return;
  } else {
    error_msg = "Could not parse method pattern";
  }
}
PRAGMA_DIAG_POP

bool MethodMatcher::matches(const methodHandle& method) const {
  Symbol* class_name  = method->method_holder()->name();
  Symbol* method_name = method->name();
  Symbol* signature = method->signature();

  if (match(class_name, this->class_name(), _class_mode) &&
      match(method_name, this->method_name(), _method_mode) &&
      ((this->signature() == NULL) || match(signature, this->signature(), Prefix))) {
    return true;
  }
  return false;
}

void MethodMatcher::print_symbol(outputStream* st, Symbol* h, Mode mode) {
  if (mode == Suffix || mode == Substring || mode == Any) {
    st->print("*");
  }
  if (mode != Any) {
    h->print_utf8_on(st);
  }
  if (mode == Prefix || mode == Substring) {
    st->print("*");
  }
}

void MethodMatcher::print_base(outputStream* st) {
  ResourceMark rm;

  print_symbol(st, class_name(), _class_mode);
  st->print(".");
  print_symbol(st, method_name(), _method_mode);
  if (signature() != NULL) {
    signature()->print_utf8_on(st);
  }
}

BasicMatcher* BasicMatcher::parse_method_pattern(char* line, const char*& error_msg, bool expect_trailing_chars) {
  assert(error_msg == NULL, "Don't call here with error_msg already set");
  BasicMatcher* bm = new BasicMatcher();
  MethodMatcher::parse_method_pattern(line, error_msg, bm);
  if (error_msg != NULL) {
    delete bm;
    return NULL;
  }
  if (!expect_trailing_chars) {
    // check for bad trailing characters
    int bytes_read = 0;
    sscanf(line, "%*[ \t]%n", &bytes_read);
    if (line[bytes_read] != '\0') {
      error_msg = "Unrecognized trailing text after method pattern";
      delete bm;
      return NULL;
    }
  }
  return bm;
}

bool BasicMatcher::match(const methodHandle& method) {
  for (BasicMatcher* current = this; current != NULL; current = current->next()) {
    if (current->matches(method)) {
      return true;
    }
  }
  return false;
}

void InlineMatcher::print(outputStream* st) {
  if (_inline_action == InlineMatcher::force_inline) {
    st->print("+");
  } else {
    st->print("-");
  }
  print_base(st);
}

InlineMatcher* InlineMatcher::parse_method_pattern(char* line, const char*& error_msg) {
  assert(error_msg == NULL, "Dont call here with error_msg already set");
  InlineMatcher* im = new InlineMatcher();
  MethodMatcher::parse_method_pattern(line, error_msg, im);
  if (error_msg != NULL) {
    delete im;
    return NULL;
  }
  return im;
}

bool InlineMatcher::match(const methodHandle& method, int inline_action) {
  for (InlineMatcher* current = this; current != NULL; current = current->next()) {
    if (current->matches(method)) {
      return (current->_inline_action == inline_action);
    }
  }
  return false;
}

InlineMatcher* InlineMatcher::parse_inline_pattern(char* str, const char*& error_msg) {
  // check first token is +/-
  InlineType _inline_action;
   switch (str[0]) {
   case '-':
     _inline_action = InlineMatcher::dont_inline;
     break;
   case '+':
     _inline_action = InlineMatcher::force_inline;
     break;
   default:
     error_msg = "Missing leading inline type (+/-)";
     return NULL;
   }
   str++;

   assert(error_msg == NULL, "error_msg must not be set yet");
   InlineMatcher* im = InlineMatcher::parse_method_pattern(str, error_msg);
   if (im == NULL) {
     assert(error_msg != NULL, "Must have error message");
     return NULL;
   }
   im->set_action(_inline_action);
   return im;
}

InlineMatcher* InlineMatcher::clone() {
   InlineMatcher* m = new InlineMatcher();
   m->_class_mode =  _class_mode;
   m->_method_mode = _method_mode;
   m->_inline_action = _inline_action;
   m->_class_name = _class_name;
   if(_class_name != NULL) {
     _class_name->increment_refcount();
   }
   m->_method_name = _method_name;
   if (_method_name != NULL) {
     _method_name->increment_refcount();
   }
   m->_signature = _signature;
   if (_signature != NULL) {
     _signature->increment_refcount();
   }
   return m;
}
