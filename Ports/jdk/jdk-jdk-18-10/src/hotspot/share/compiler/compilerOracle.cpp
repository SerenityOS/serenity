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
#include "jvm.h"
#include "classfile/symbolTable.hpp"
#include "compiler/compilerDirectives.hpp"
#include "compiler/compilerOracle.hpp"
#include "compiler/methodMatcher.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "oops/klass.hpp"
#include "oops/method.inline.hpp"
#include "oops/symbol.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/jniHandles.hpp"
#include "runtime/os.hpp"

static const char* optiontype_names[] = {
#define enum_of_types(type, name) name,
        OPTION_TYPES(enum_of_types)
#undef enum_of_types
};

const char* optiontype2name(enum OptionType type) {
  return optiontype_names[static_cast<int>(type)];
}

static enum OptionType option_types[] = {
#define enum_of_options(option, name, ctype) OptionType::ctype,
        COMPILECOMMAND_OPTIONS(enum_of_options)
#undef enum_of_options
};

enum OptionType option2type(enum CompileCommand option) {
  return option_types[static_cast<int>(option)];
}

static const char* option_names[] = {
#define enum_of_options(option, name, ctype) name,
        COMPILECOMMAND_OPTIONS(enum_of_options)
#undef enum_of_options
};

const char* option2name(enum CompileCommand option) {
  return option_names[static_cast<int>(option)];
}

/* Methods to map real type names to OptionType */
template<typename T>
static OptionType get_type_for() {
  return OptionType::Unknown;
};

template<> OptionType get_type_for<intx>() {
  return OptionType::Intx;
}

template<> OptionType get_type_for<uintx>() {
  return OptionType::Uintx;
}

template<> OptionType get_type_for<bool>() {
  return OptionType::Bool;
}

template<> OptionType get_type_for<ccstr>() {
  return OptionType::Ccstr;
}

template<> OptionType get_type_for<double>() {
  return OptionType::Double;
}

class MethodMatcher;
class TypedMethodOptionMatcher;

static TypedMethodOptionMatcher* option_list = NULL;
static bool any_set = false;

// A filter for quick lookup if an option is set
static bool option_filter[static_cast<int>(CompileCommand::Unknown) + 1] = { 0 };

void command_set_in_filter(enum CompileCommand option) {
  assert(option != CompileCommand::Unknown, "sanity");
  assert(option2type(option) != OptionType::Unknown, "sanity");

  if ((option != CompileCommand::DontInline) &&
      (option != CompileCommand::Inline) &&
      (option != CompileCommand::Log)) {
    any_set = true;
  }
  option_filter[static_cast<int>(option)] = true;
}

bool has_command(enum CompileCommand option) {
  return option_filter[static_cast<int>(option)];
}

class TypedMethodOptionMatcher : public MethodMatcher {
 private:
  TypedMethodOptionMatcher* _next;
  enum CompileCommand _option;
 public:

  union {
    bool bool_value;
    intx intx_value;
    uintx uintx_value;
    double double_value;
    ccstr ccstr_value;
  } _u;

  TypedMethodOptionMatcher() : MethodMatcher(),
    _next(NULL),
    _option(CompileCommand::Unknown) {
      memset(&_u, 0, sizeof(_u));
  }

  ~TypedMethodOptionMatcher();
  static TypedMethodOptionMatcher* parse_method_pattern(char*& line, char* errorbuf, const int buf_size);
  TypedMethodOptionMatcher* match(const methodHandle &method, enum CompileCommand option);

  void init(enum CompileCommand option, TypedMethodOptionMatcher* next) {
    _next = next;
    _option = option;
  }

  void init_matcher(Symbol* class_name, Mode class_mode,
                    Symbol* method_name, Mode method_mode,
                    Symbol* signature) {
    MethodMatcher::init(class_name, class_mode, method_name, method_mode, signature);
  }

  void set_next(TypedMethodOptionMatcher* next) {_next = next; }
  TypedMethodOptionMatcher* next() { return _next; }
  enum CompileCommand option() { return _option; }
  template<typename T> T value();
  template<typename T> void set_value(T value);
  void print();
  void print_all();
  TypedMethodOptionMatcher* clone();
};

// A few templated accessors instead of a full template class.
template<> intx TypedMethodOptionMatcher::value<intx>() {
  return _u.intx_value;
}

template<> uintx TypedMethodOptionMatcher::value<uintx>() {
  return _u.uintx_value;
}

template<> bool TypedMethodOptionMatcher::value<bool>() {
  return _u.bool_value;
}

template<> double TypedMethodOptionMatcher::value<double>() {
  return _u.double_value;
}

template<> ccstr TypedMethodOptionMatcher::value<ccstr>() {
  return _u.ccstr_value;
}

template<> void TypedMethodOptionMatcher::set_value(intx value) {
  _u.intx_value = value;
}

template<> void TypedMethodOptionMatcher::set_value(uintx value) {
  _u.uintx_value = value;
}

template<> void TypedMethodOptionMatcher::set_value(double value) {
  _u.double_value = value;
}

template<> void TypedMethodOptionMatcher::set_value(bool value) {
  _u.bool_value = value;
}

template<> void TypedMethodOptionMatcher::set_value(ccstr value) {
  _u.ccstr_value = (const ccstr)os::strdup_check_oom(value);
}

void TypedMethodOptionMatcher::print() {
  ttyLocker ttyl;
  print_base(tty);
  const char* name = option2name(_option);
  enum OptionType type = option2type(_option);
  switch (type) {
    case OptionType::Intx:
    tty->print_cr(" intx %s = " INTX_FORMAT, name, value<intx>());
    break;
    case OptionType::Uintx:
    tty->print_cr(" uintx %s = " UINTX_FORMAT, name, value<uintx>());
    break;
    case OptionType::Bool:
    tty->print_cr(" bool %s = %s", name, value<bool>() ? "true" : "false");
    break;
    case OptionType::Double:
    tty->print_cr(" double %s = %f", name, value<double>());
    break;
    case OptionType::Ccstr:
    case OptionType::Ccstrlist:
    tty->print_cr(" const char* %s = '%s'", name, value<ccstr>());
    break;
  default:
    ShouldNotReachHere();
  }
}

void TypedMethodOptionMatcher::print_all() {
   print();
   if (_next != NULL) {
     tty->print(" ");
     _next->print_all();
   }
 }

TypedMethodOptionMatcher* TypedMethodOptionMatcher::clone() {
  TypedMethodOptionMatcher* m = new TypedMethodOptionMatcher();
  m->_class_mode = _class_mode;
  m->_class_name = _class_name;
  m->_method_mode = _method_mode;
  m->_method_name = _method_name;
  m->_signature = _signature;
  // Need to ref count the symbols
  if (_class_name != NULL) {
    _class_name->increment_refcount();
  }
  if (_method_name != NULL) {
    _method_name->increment_refcount();
  }
  if (_signature != NULL) {
    _signature->increment_refcount();
  }
  return m;
}

TypedMethodOptionMatcher::~TypedMethodOptionMatcher() {
  enum OptionType type = option2type(_option);
  if (type == OptionType::Ccstr || type == OptionType::Ccstrlist) {
    ccstr v = value<ccstr>();
    os::free((void*)v);
  }
}

TypedMethodOptionMatcher* TypedMethodOptionMatcher::parse_method_pattern(char*& line, char* errorbuf, const int buf_size) {
  assert(*errorbuf == '\0', "Dont call here with error_msg already set");
  const char* error_msg = NULL;
  TypedMethodOptionMatcher* tom = new TypedMethodOptionMatcher();
  MethodMatcher::parse_method_pattern(line, error_msg, tom);
  if (error_msg != NULL) {
    jio_snprintf(errorbuf, buf_size, error_msg);
    delete tom;
    return NULL;
  }
  return tom;
}

TypedMethodOptionMatcher* TypedMethodOptionMatcher::match(const methodHandle& method, enum CompileCommand option) {
  TypedMethodOptionMatcher* current = this;
  while (current != NULL) {
    if (current->_option == option) {
      if (current->matches(method)) {
        return current;
      }
    }
    current = current->next();
  }
  return NULL;
}

template<typename T>
static void register_command(TypedMethodOptionMatcher* matcher,
                             enum CompileCommand option,
                             T value) {
  assert(matcher != option_list, "No circular lists please");
  if (option == CompileCommand::Log && !LogCompilation) {
    tty->print_cr("Warning:  +LogCompilation must be enabled in order for individual methods to be logged with ");
    tty->print_cr("          CompileCommand=log,<method pattern>");
  }
  assert(CompilerOracle::option_matches_type(option, value), "Value must match option type");

  if (option == CompileCommand::Blackhole && !UnlockExperimentalVMOptions) {
    warning("Blackhole compile option is experimental and must be enabled via -XX:+UnlockExperimentalVMOptions");
    return;
  }

  matcher->init(option, option_list);
  matcher->set_value<T>(value);
  option_list = matcher;
  command_set_in_filter(option);

  if (!CompilerOracle::be_quiet()) {
    // Print out the successful registration of a compile command
    ttyLocker ttyl;
    tty->print("CompileCommand: %s ", option2name(option));
    matcher->print();
  }
  return;
}

template<typename T>
bool CompilerOracle::has_option_value(const methodHandle& method, enum CompileCommand option, T& value) {
  assert(option_matches_type(option, value), "Value must match option type");
  if (!has_command(option)) {
    return false;
  }
  if (option_list != NULL) {
    TypedMethodOptionMatcher* m = option_list->match(method, option);
    if (m != NULL) {
      value = m->value<T>();
      return true;
    }
  }
  return false;
}

static bool resolve_inlining_predicate(enum CompileCommand option, const methodHandle& method) {
  assert(option == CompileCommand::Inline || option == CompileCommand::DontInline, "Sanity");
  bool v1 = false;
  bool v2 = false;
  bool has_inline = CompilerOracle::has_option_value(method, CompileCommand::Inline, v1);
  bool has_dnotinline = CompilerOracle::has_option_value(method, CompileCommand::DontInline, v2);
  if (has_inline && has_dnotinline) {
    if (v1 && v2) {
      // Conflict options detected
      // Find the last one for that method and return the predicate accordingly
      // option_list lists options in reverse order. So the first option we find is the last which was specified.
      enum CompileCommand last_one = CompileCommand::Unknown;
      TypedMethodOptionMatcher* current = option_list;
      while (current != NULL) {
        last_one = current->option();
        if (last_one == CompileCommand::Inline || last_one == CompileCommand::DontInline) {
          if (current->matches(method)) {
            return last_one == option;
          }
        }
        current = current->next();
      }
      ShouldNotReachHere();
      return false;
    } else {
      // No conflicts
      return option == CompileCommand::Inline ? v1 : v2;
    }
  } else {
    if (option == CompileCommand::Inline) {
      return has_inline ? v1 : false;
    } else {
      return has_dnotinline ? v2 : false;
    }
  }
}

static bool check_predicate(enum CompileCommand option, const methodHandle& method) {
  // Special handling for Inline and DontInline since conflict options may be specified
  if (option == CompileCommand::Inline || option == CompileCommand::DontInline) {
    return resolve_inlining_predicate(option, method);
  }

  bool value = false;
  if (CompilerOracle::has_option_value(method, option, value)) {
    return value;
  }
  return false;
}

bool CompilerOracle::has_any_command_set() {
  return any_set;
}

// Explicit instantiation for all OptionTypes supported.
template bool CompilerOracle::has_option_value<intx>(const methodHandle& method, enum CompileCommand option, intx& value);
template bool CompilerOracle::has_option_value<uintx>(const methodHandle& method, enum CompileCommand option, uintx& value);
template bool CompilerOracle::has_option_value<bool>(const methodHandle& method, enum CompileCommand option, bool& value);
template bool CompilerOracle::has_option_value<ccstr>(const methodHandle& method, enum CompileCommand option, ccstr& value);
template bool CompilerOracle::has_option_value<double>(const methodHandle& method, enum CompileCommand option, double& value);

template<typename T>
bool CompilerOracle::option_matches_type(enum CompileCommand option, T& value) {
  enum OptionType option_type = option2type(option);
  if (option_type == OptionType::Unknown) {
    return false; // Can't query options with type Unknown.
  }
  if (option_type == OptionType::Ccstrlist) {
    option_type = OptionType::Ccstr; // CCstrList type options are stored as Ccstr
  }
  return (get_type_for<T>() == option_type);
}

template bool CompilerOracle::option_matches_type<intx>(enum CompileCommand option, intx& value);
template bool CompilerOracle::option_matches_type<uintx>(enum CompileCommand option, uintx& value);
template bool CompilerOracle::option_matches_type<bool>(enum CompileCommand option, bool& value);
template bool CompilerOracle::option_matches_type<ccstr>(enum CompileCommand option, ccstr& value);
template bool CompilerOracle::option_matches_type<double>(enum CompileCommand option, double& value);

bool CompilerOracle::has_option(const methodHandle& method, enum CompileCommand option) {
  bool value = false;
  has_option_value(method, option, value);
  return value;
}

bool CompilerOracle::should_exclude(const methodHandle& method) {
  if (check_predicate(CompileCommand::Exclude, method)) {
    return true;
  }
  if (has_command(CompileCommand::CompileOnly)) {
    return !check_predicate(CompileCommand::CompileOnly, method);
  }
  return false;
}

bool CompilerOracle::should_inline(const methodHandle& method) {
  return (check_predicate(CompileCommand::Inline, method));
}

bool CompilerOracle::should_not_inline(const methodHandle& method) {
  return check_predicate(CompileCommand::DontInline, method) || check_predicate(CompileCommand::Exclude, method);
}

bool CompilerOracle::should_print(const methodHandle& method) {
  return check_predicate(CompileCommand::Print, method);
}

bool CompilerOracle::should_print_methods() {
  return has_command(CompileCommand::Print);
}

bool CompilerOracle::should_log(const methodHandle& method) {
  if (!LogCompilation) return false;
  if (!has_command(CompileCommand::Log)) {
    return true;  // by default, log all
  }
  return (check_predicate(CompileCommand::Log, method));
}

bool CompilerOracle::should_break_at(const methodHandle& method) {
  return check_predicate(CompileCommand::Break, method);
}

void CompilerOracle::tag_blackhole_if_possible(const methodHandle& method) {
  if (!check_predicate(CompileCommand::Blackhole, method)) {
    return;
  }
  guarantee(UnlockExperimentalVMOptions, "Checked during initial parsing");
  if (method->result_type() != T_VOID) {
    warning("Blackhole compile option only works for methods with void type: %s",
            method->name_and_sig_as_C_string());
    return;
  }
  if (!method->is_empty_method()) {
    warning("Blackhole compile option only works for empty methods: %s",
            method->name_and_sig_as_C_string());
    return;
  }
  if (!method->is_static()) {
    warning("Blackhole compile option only works for static methods: %s",
            method->name_and_sig_as_C_string());
    return;
  }
  if (method->intrinsic_id() == vmIntrinsics::_blackhole) {
    return;
  }
  if (method->intrinsic_id() != vmIntrinsics::_none) {
    warning("Blackhole compile option only works for methods that do not have intrinsic set: %s, %s",
            method->name_and_sig_as_C_string(), vmIntrinsics::name_at(method->intrinsic_id()));
    return;
  }
  method->set_intrinsic_id(vmIntrinsics::_blackhole);
}

static enum CompileCommand match_option_name(const char* line, int* bytes_read, char* errorbuf, int bufsize) {
  assert(ARRAY_SIZE(option_names) == static_cast<int>(CompileCommand::Count), "option_names size mismatch");

  *bytes_read = 0;
  char option_buf[256];
  int matches = sscanf(line, "%255[a-zA-Z0-9]%n", option_buf, bytes_read);
  if (matches > 0 && strcasecmp(option_buf, "unknown") != 0) {
    for (uint i = 0; i < ARRAY_SIZE(option_names); i++) {
      if (strcasecmp(option_buf, option_names[i]) == 0) {
        return static_cast<enum CompileCommand>(i);
      }
    }
  }
  jio_snprintf(errorbuf, bufsize, "Unrecognized option '%s'", option_buf);
  return CompileCommand::Unknown;
}

// match exactly and don't mess with errorbuf
enum CompileCommand CompilerOracle::parse_option_name(const char* line) {
  for (uint i = 0; i < ARRAY_SIZE(option_names); i++) {
    if (strcasecmp(line, option_names[i]) == 0) {
      return static_cast<enum CompileCommand>(i);
    }
  }
  return CompileCommand::Unknown;
}

enum OptionType CompilerOracle::parse_option_type(const char* type_str) {
  for (uint i = 0; i < ARRAY_SIZE(optiontype_names); i++) {
    if (strcasecmp(type_str, optiontype_names[i]) == 0) {
      return static_cast<enum OptionType>(i);
    }
  }
  return OptionType::Unknown;
}

void print_tip() { // CMH Update info
  tty->cr();
  tty->print_cr("Usage: '-XX:CompileCommand=<option>,<method pattern>' - to set boolean option to true");
  tty->print_cr("Usage: '-XX:CompileCommand=<option>,<method pattern>,<value>'");
  tty->print_cr("Use:   '-XX:CompileCommand=help' for more information and to list all option.");
  tty->cr();
}

void print_option(enum CompileCommand option, const char* name, enum OptionType type) {
  if (type != OptionType::Unknown) {
    tty->print_cr("    %s (%s)", name, optiontype2name(type));
  }
}

void print_commands() {
  tty->cr();
  tty->print_cr("All available options:");
#define enum_of_options(option, name, ctype) print_option(CompileCommand::option, name, OptionType::ctype);
  COMPILECOMMAND_OPTIONS(enum_of_options)
#undef enum_of_options
  tty->cr();
}

static void usage() {
  tty->cr();
  tty->print_cr("The CompileCommand option enables the user of the JVM to control specific");
  tty->print_cr("behavior of the dynamic compilers.");
  tty->cr();
  tty->print_cr("Compile commands has this general form:");
  tty->print_cr("-XX:CompileCommand=<option><method pattern><value>");
  tty->print_cr("    Sets <option> to the specified value for methods matching <method pattern>");
  tty->print_cr("    All options are typed");
  tty->cr();
  tty->print_cr("-XX:CompileCommand=<option><method pattern>");
  tty->print_cr("    Sets <option> to true for methods matching <method pattern>");
  tty->print_cr("    Only applies to boolean options.");
  tty->cr();
  tty->print_cr("-XX:CompileCommand=quiet");
  tty->print_cr("    Silence the compile command output");
  tty->cr();
  tty->print_cr("-XX:CompileCommand=help");
  tty->print_cr("    Prints this help text");
  tty->cr();
  print_commands();
  tty->cr();
    tty->print_cr("Method patterns has the format:");
  tty->print_cr("  package/Class.method()");
  tty->cr();
  tty->print_cr("For backward compatibility this form is also allowed:");
  tty->print_cr("  package.Class::method()");
  tty->cr();
  tty->print_cr("The signature can be separated by an optional whitespace or comma:");
  tty->print_cr("  package/Class.method ()");
  tty->cr();
  tty->print_cr("The class and method identifier can be used together with leading or");
  tty->print_cr("trailing *'s for wildcard matching:");
  tty->print_cr("  *ackage/Clas*.*etho*()");
  tty->cr();
  tty->print_cr("It is possible to use more than one CompileCommand on the command line:");
  tty->print_cr("  -XX:CompileCommand=exclude,java/*.* -XX:CompileCommand=log,java*.*");
  tty->cr();
  tty->print_cr("The CompileCommands can be loaded from a file with the flag");
  tty->print_cr("-XX:CompileCommandFile=<file> or be added to the file '.hotspot_compiler'");
  tty->print_cr("Use the same format in the file as the argument to the CompileCommand flag.");
  tty->print_cr("Add one command on each line.");
  tty->print_cr("  exclude java/*.*");
  tty->print_cr("  option java/*.* ReplayInline");
  tty->cr();
  tty->print_cr("The following commands have conflicting behavior: 'exclude', 'inline', 'dontinline',");
  tty->print_cr("and 'compileonly'. There is no priority of commands. Applying (a subset of) these");
  tty->print_cr("commands to the same method results in undefined behavior.");
  tty->cr();
};

int skip_whitespace(char* &line) {
  // Skip any leading spaces
  int whitespace_read = 0;
  sscanf(line, "%*[ \t]%n", &whitespace_read);
  line += whitespace_read;
  return whitespace_read;
}

void skip_comma(char* &line) {
  // Skip any leading spaces
  if (*line == ',') {
    line++;
  }
}

static void scan_value(enum OptionType type, char* line, int& total_bytes_read,
        TypedMethodOptionMatcher* matcher, enum CompileCommand option, char* errorbuf, const int buf_size) {
  int bytes_read = 0;
  const char* ccname = option2name(option);
  const char* type_str = optiontype2name(type);
  int skipped = skip_whitespace(line);
  total_bytes_read += skipped;
  if (type == OptionType::Intx) {
    intx value;
    if (sscanf(line, "" INTX_FORMAT "%n", &value, &bytes_read) == 1) {
      total_bytes_read += bytes_read;
      line += bytes_read;
      register_command(matcher, option, value);
      return;
    } else {
      jio_snprintf(errorbuf, buf_size, "Value cannot be read for option '%s' of type '%s'", ccname, type_str);
    }
  } else if (type == OptionType::Uintx) {
    uintx value;
    if (sscanf(line, "" UINTX_FORMAT "%n", &value, &bytes_read) == 1) {
      total_bytes_read += bytes_read;
      line += bytes_read;
      register_command(matcher, option, value);
      return;
    } else {
      jio_snprintf(errorbuf, buf_size, "Value cannot be read for option '%s' of type '%s'", ccname, type_str);
    }
  } else if (type == OptionType::Ccstr) {
    ResourceMark rm;
    char* value = NEW_RESOURCE_ARRAY(char, strlen(line) + 1);
    if (sscanf(line, "%255[_a-zA-Z0-9]%n", value, &bytes_read) == 1) {
      total_bytes_read += bytes_read;
      line += bytes_read;
      register_command(matcher, option, (ccstr) value);
      return;
    } else {
      jio_snprintf(errorbuf, buf_size, "Value cannot be read for option '%s' of type '%s'", ccname, type_str);
    }
  } else if (type == OptionType::Ccstrlist) {
    // Accumulates several strings into one. The internal type is ccstr.
    ResourceMark rm;
    char* value = NEW_RESOURCE_ARRAY(char, strlen(line) + 1);
    char* next_value = value;
    if (sscanf(line, "%255[_a-zA-Z0-9+\\-]%n", next_value, &bytes_read) == 1) {
      total_bytes_read += bytes_read;
      line += bytes_read;
      next_value += bytes_read + 1;
      char* end_value = next_value - 1;
      while (sscanf(line, "%*[ \t]%255[_a-zA-Z0-9+\\-]%n", next_value, &bytes_read) == 1) {
        total_bytes_read += bytes_read;
        line += bytes_read;
        *end_value = ' '; // override '\0'
        next_value += bytes_read;
        end_value = next_value-1;
      }

      if (option == CompileCommand::ControlIntrinsic || option == CompileCommand::DisableIntrinsic) {
        ControlIntrinsicValidator validator(value, (option == CompileCommand::DisableIntrinsic));

        if (!validator.is_valid()) {
          jio_snprintf(errorbuf, buf_size, "Unrecognized intrinsic detected in %s: %s", option2name(option), validator.what());
        }
      }

      register_command(matcher, option, (ccstr) value);
      return;
    } else {
      jio_snprintf(errorbuf, buf_size, "Value cannot be read for option '%s' of type '%s'", ccname, type_str);
    }
  } else if (type == OptionType::Bool) {
    char value[256];
    if (*line == '\0') {
      // Short version of a CompileCommand sets a boolean Option to true
      // -XXCompileCommand=<Option>,<method pattern>
      register_command(matcher, option, true);
      return;
    }
    if (sscanf(line, "%255[a-zA-Z]%n", value, &bytes_read) == 1) {
      if (strcasecmp(value, "true") == 0) {
        total_bytes_read += bytes_read;
        line += bytes_read;
        register_command(matcher, option, true);
        return;
      } else if (strcasecmp(value, "false") == 0) {
        total_bytes_read += bytes_read;
        line += bytes_read;
        register_command(matcher, option, false);
        return;
      } else {
        jio_snprintf(errorbuf, buf_size, "Value cannot be read for option '%s' of type '%s'", ccname, type_str);
      }
    } else {
      jio_snprintf(errorbuf, buf_size, "Value cannot be read for option '%s' of type '%s'", ccname, type_str);
    }
  } else if (type == OptionType::Double) {
    char buffer[2][256];
    // Decimal separator '.' has been replaced with ' ' or '/' earlier,
    // so read integer and fraction part of double value separately.
    if (sscanf(line, "%255[0-9]%*[ /\t]%255[0-9]%n", buffer[0], buffer[1], &bytes_read) == 2) {
      char value[512] = "";
      jio_snprintf(value, sizeof(value), "%s.%s", buffer[0], buffer[1]);
      total_bytes_read += bytes_read;
      line += bytes_read;
      register_command(matcher, option, atof(value));
      return;
    } else {
      jio_snprintf(errorbuf, buf_size, "Value cannot be read for option '%s' of type '%s'", ccname, type_str);
    }
  } else {
    jio_snprintf(errorbuf, buf_size, "Type '%s' not supported ", type_str);
  }
}

// Scan next option and value in line, return MethodMatcher object on success, NULL on failure.
// On failure, error_msg contains description for the first error.
// For future extensions: set error_msg on first error.
static void scan_option_and_value(enum OptionType type, char* line, int& total_bytes_read,
                                TypedMethodOptionMatcher* matcher,
                                char* errorbuf, const int buf_size) {
  total_bytes_read = 0;
  int bytes_read = 0;
  char option_buf[256];

  // Read option name.
  if (sscanf(line, "%*[ \t]%255[a-zA-Z0-9]%n", option_buf, &bytes_read) == 1) {
    line += bytes_read;
    total_bytes_read += bytes_read;
    int bytes_read2 = 0;
    total_bytes_read += skip_whitespace(line);
    enum CompileCommand option = match_option_name(option_buf, &bytes_read2, errorbuf, buf_size);
    if (option == CompileCommand::Unknown) {
      assert(*errorbuf != '\0', "error must have been set");
      return;
    }
    enum OptionType optiontype = option2type(option);
    if (option2type(option) != type) {
      const char* optiontype_name = optiontype2name(optiontype);
      const char* type_name = optiontype2name(type);
      jio_snprintf(errorbuf, buf_size, "Option '%s' with type '%s' doesn't match supplied type '%s'", option_buf, optiontype_name, type_name);
      return;
    }
    scan_value(type, line, total_bytes_read, matcher, option, errorbuf, buf_size);
  } else {
    const char* type_str = optiontype2name(type);
    jio_snprintf(errorbuf, buf_size, "Option name for type '%s' should be alphanumeric ", type_str);
  }
  return;
}

void CompilerOracle::print_parse_error(char* error_msg, char* original_line) {
  assert(*error_msg != '\0', "Must have error_message");
  ttyLocker ttyl;
  tty->print_cr("CompileCommand: An error occurred during parsing");
  tty->print_cr("Error: %s", error_msg);
  tty->print_cr("Line: '%s'", original_line);
  print_tip();
}

class LineCopy : StackObj {
  const char* _copy;
public:
    LineCopy(char* line) {
      _copy = os::strdup(line, mtInternal);
    }
    ~LineCopy() {
      os::free((void*)_copy);
    }
    char* get() {
      return (char*)_copy;
    }
};

void CompilerOracle::parse_from_line(char* line) {
  if (line[0] == '\0') return;
  if (line[0] == '#')  return;

  LineCopy original(line);
  int bytes_read;
  char error_buf[1024] = {0};

  enum CompileCommand option = match_option_name(line, &bytes_read, error_buf, sizeof(error_buf));
  line += bytes_read;
  ResourceMark rm;

  if (option == CompileCommand::Unknown) {
    print_parse_error(error_buf, original.get());
    return;
  }

  if (option == CompileCommand::Quiet) {
    _quiet = true;
    return;
  }

  if (option == CompileCommand::Help) {
    usage();
    return;
  }

  if (option == CompileCommand::Option) {
    // Look for trailing options.
    //
    // Two types of trailing options are
    // supported:
    //
    // (1) CompileCommand=option,Klass::method,option
    // (2) CompileCommand=option,Klass::method,type,option,value
    //
    // Type (1) is used to enable a boolean option for a method.
    //
    // Type (2) is used to support options with a value. Values can have the
    // the following types: intx, uintx, bool, ccstr, ccstrlist, and double.

    char option_type[256]; // stores option for Type (1) and type of Type (2)
    skip_comma(line);
    TypedMethodOptionMatcher* archetype = TypedMethodOptionMatcher::parse_method_pattern(line, error_buf, sizeof(error_buf));
    if (archetype == NULL) {
      print_parse_error(error_buf, original.get());
      return;
    }

    skip_whitespace(line);

    // This is unnecessarily complex. Should retire multi-option lines and skip while loop
    while (sscanf(line, "%255[a-zA-Z0-9]%n", option_type, &bytes_read) == 1) {
      line += bytes_read;

      // typed_matcher is used as a blueprint for each option, deleted at the end
      TypedMethodOptionMatcher* typed_matcher = archetype->clone();
      enum OptionType type = parse_option_type(option_type);
      if (type != OptionType::Unknown) {
        // Type (2) option: parse option name and value.
        scan_option_and_value(type, line, bytes_read, typed_matcher, error_buf, sizeof(error_buf));
        if (*error_buf != '\0') {
          print_parse_error(error_buf, original.get());
          return;
        }
        line += bytes_read;
      } else {
        // Type (1) option - option_type contains the option name -> bool value = true is implied
        int bytes_read;
        enum CompileCommand option = match_option_name(option_type, &bytes_read, error_buf, sizeof(error_buf));
        if (option == CompileCommand::Unknown) {
          print_parse_error(error_buf, original.get());
          return;
        }
        if (option2type(option) == OptionType::Bool) {
          register_command(typed_matcher, option, true);
        } else {
          jio_snprintf(error_buf, sizeof(error_buf), "  Missing type '%s' before option '%s'",
                       optiontype2name(option2type(option)), option2name(option));
          print_parse_error(error_buf, original.get());
          return;
        }
      }
      assert(typed_matcher != NULL, "sanity");
      assert(*error_buf == '\0', "No error here");
      skip_whitespace(line);
    } // while(
    delete archetype;
  } else {  // not an OptionCommand
    // Command has the following form:
    // CompileCommand=<option>,<method pattern><value>
    // CompileCommand=<option>,<method pattern>     (implies option is bool and value is true)
    assert(*error_buf == '\0', "Don't call here with error_buf already set");
    enum OptionType type = option2type(option);
    int bytes_read = 0;
    skip_comma(line);
    TypedMethodOptionMatcher* matcher = TypedMethodOptionMatcher::parse_method_pattern(line, error_buf, sizeof(error_buf));
    if (matcher == NULL) {
      print_parse_error(error_buf, original.get());
      return;
    }
    skip_whitespace(line);
    if (*line == '\0') {
      // if this is a bool option this implies true
      if (option2type(option) == OptionType::Bool) {
        register_command(matcher, option, true);
        return;
      } else {
        jio_snprintf(error_buf, sizeof(error_buf), "  Option '%s' is not followed by a value", option2name(option));
        print_parse_error(error_buf, original.get());
        return;
      }
    }
    scan_value(type, line, bytes_read, matcher, option, error_buf, sizeof(error_buf));
    if (*error_buf != '\0') {
      print_parse_error(error_buf, original.get());
      return;
    }
    assert(matcher != NULL, "consistency");
  }
}

static const char* default_cc_file = ".hotspot_compiler";

static const char* cc_file() {
#ifdef ASSERT
  if (CompileCommandFile == NULL)
    return default_cc_file;
#endif
  return CompileCommandFile;
}

bool CompilerOracle::has_command_file() {
  return cc_file() != NULL;
}

bool CompilerOracle::_quiet = false;

void CompilerOracle::parse_from_file() {
  assert(has_command_file(), "command file must be specified");
  FILE* stream = fopen(cc_file(), "rt");
  if (stream == NULL) return;

  char token[1024];
  int  pos = 0;
  int  c = getc(stream);
  while(c != EOF && pos < (int)(sizeof(token)-1)) {
    if (c == '\n') {
      token[pos++] = '\0';
      parse_from_line(token);
      pos = 0;
    } else {
      token[pos++] = c;
    }
    c = getc(stream);
  }
  token[pos++] = '\0';
  parse_from_line(token);

  fclose(stream);
}

void CompilerOracle::parse_from_string(const char* str, void (*parse_line)(char*)) {
  char token[1024];
  int  pos = 0;
  const char* sp = str;
  int  c = *sp++;
  while (c != '\0' && pos < (int)(sizeof(token)-1)) {
    if (c == '\n') {
      token[pos++] = '\0';
      parse_line(token);
      pos = 0;
    } else {
      token[pos++] = c;
    }
    c = *sp++;
  }
  token[pos++] = '\0';
  parse_line(token);
}

void compilerOracle_init() {
  CompilerOracle::parse_from_string(CompileCommand, CompilerOracle::parse_from_line);
  CompilerOracle::parse_from_string(CompileOnly, CompilerOracle::parse_compile_only);
  if (CompilerOracle::has_command_file()) {
    CompilerOracle::parse_from_file();
  } else {
    struct stat buf;
    if (os::stat(default_cc_file, &buf) == 0) {
      warning("%s file is present but has been ignored.  "
              "Run with -XX:CompileCommandFile=%s to load the file.",
              default_cc_file, default_cc_file);
    }
  }
  if (has_command(CompileCommand::Print)) {
    if (PrintAssembly) {
      warning("CompileCommand and/or %s file contains 'print' commands, but PrintAssembly is also enabled", default_cc_file);
    }
  }
}

void CompilerOracle::parse_compile_only(char* line) {
  int i;
  char name[1024];
  const char* className = NULL;
  const char* methodName = NULL;

  bool have_colon = (strstr(line, "::") != NULL);
  char method_sep = have_colon ? ':' : '.';

  if (Verbose) {
    tty->print_cr("%s", line);
  }

  ResourceMark rm;
  while (*line != '\0') {
    MethodMatcher::Mode c_match = MethodMatcher::Exact;
    MethodMatcher::Mode m_match = MethodMatcher::Exact;

    for (i = 0;
         i < 1024 && *line != '\0' && *line != method_sep && *line != ',' && !isspace(*line);
         line++, i++) {
      name[i] = *line;
      if (name[i] == '.')  name[i] = '/';  // package prefix uses '/'
    }

    if (i > 0) {
      char* newName = NEW_RESOURCE_ARRAY( char, i + 1);
      if (newName == NULL)
        return;
      strncpy(newName, name, i);
      newName[i] = '\0';

      if (className == NULL) {
        className = newName;
      } else {
        methodName = newName;
      }
    }

    if (*line == method_sep) {
      if (className == NULL) {
        className = "";
        c_match = MethodMatcher::Any;
      }
    } else {
      // got foo or foo/bar
      if (className == NULL) {
        ShouldNotReachHere();
      } else {
        // missing class name handled as "Any" class match
        if (className[0] == '\0') {
          c_match = MethodMatcher::Any;
        }
      }
    }

    // each directive is terminated by , or NUL or . followed by NUL
    if (*line == ',' || *line == '\0' || (line[0] == '.' && line[1] == '\0')) {
      if (methodName == NULL) {
        methodName = "";
        if (*line != method_sep) {
          m_match = MethodMatcher::Any;
        }
      }

      EXCEPTION_MARK;
      Symbol* c_name = SymbolTable::new_symbol(className);
      Symbol* m_name = SymbolTable::new_symbol(methodName);
      Symbol* signature = NULL;

      TypedMethodOptionMatcher* tom = new TypedMethodOptionMatcher();
      tom->init_matcher(c_name, c_match, m_name, m_match, signature);
      register_command(tom, CompileCommand::CompileOnly, true);
      if (PrintVMOptions) {
        tty->print("CompileOnly: compileonly ");
        tom->print();
      }

      className = NULL;
      methodName = NULL;
    }

    line = *line == '\0' ? line : line + 1;
  }
}

enum CompileCommand CompilerOracle::string_to_option(const char* name) {
  int bytes_read = 0;
  char errorbuf[1024] = {0};
  return match_option_name(name, &bytes_read, errorbuf, sizeof(errorbuf));
}
