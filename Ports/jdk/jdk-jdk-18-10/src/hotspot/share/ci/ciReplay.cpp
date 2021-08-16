/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciMethodData.hpp"
#include "ci/ciReplay.hpp"
#include "ci/ciSymbol.hpp"
#include "ci/ciKlass.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "compiler/compilationPolicy.hpp"
#include "compiler/compileBroker.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "oops/constantPool.hpp"
#include "oops/klass.inline.hpp"
#include "oops/method.inline.hpp"
#include "oops/oop.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/java.hpp"
#include "utilities/copy.hpp"
#include "utilities/macros.hpp"
#include "utilities/utf8.hpp"

#ifndef PRODUCT

// ciReplay

typedef struct _ciMethodDataRecord {
  const char* _klass_name;
  const char* _method_name;
  const char* _signature;

  int _state;
  int _current_mileage;

  intptr_t* _data;
  char*     _orig_data;
  Klass**   _classes;
  Method**  _methods;
  int*      _classes_offsets;
  int*      _methods_offsets;
  int       _data_length;
  int       _orig_data_length;
  int       _classes_length;
  int       _methods_length;
} ciMethodDataRecord;

typedef struct _ciMethodRecord {
  const char* _klass_name;
  const char* _method_name;
  const char* _signature;

  int _instructions_size;
  int _interpreter_invocation_count;
  int _interpreter_throwout_count;
  int _invocation_counter;
  int _backedge_counter;
} ciMethodRecord;

typedef struct _ciInlineRecord {
  const char* _klass_name;
  const char* _method_name;
  const char* _signature;

  int _inline_depth;
  int _inline_bci;
} ciInlineRecord;

class  CompileReplay;
static CompileReplay* replay_state;

class CompileReplay : public StackObj {
 private:
  FILE*   _stream;
  Thread* _thread;
  Handle  _protection_domain;
  Handle  _loader;

  GrowableArray<ciMethodRecord*>     _ci_method_records;
  GrowableArray<ciMethodDataRecord*> _ci_method_data_records;

  // Use pointer because we may need to return inline records
  // without destroying them.
  GrowableArray<ciInlineRecord*>*    _ci_inline_records;

  const char* _error_message;

  char* _bufptr;
  char* _buffer;
  int   _buffer_length;
  int   _buffer_pos;

  // "compile" data
  ciKlass* _iklass;
  Method*  _imethod;
  int      _entry_bci;
  int      _comp_level;

 public:
  CompileReplay(const char* filename, TRAPS) {
    _thread = THREAD;
    _loader = Handle(_thread, SystemDictionary::java_system_loader());
    _protection_domain = Handle();

    _stream = fopen(filename, "rt");
    if (_stream == NULL) {
      fprintf(stderr, "ERROR: Can't open replay file %s\n", filename);
    }

    _ci_inline_records = NULL;
    _error_message = NULL;

    _buffer_length = 32;
    _buffer = NEW_RESOURCE_ARRAY(char, _buffer_length);
    _bufptr = _buffer;
    _buffer_pos = 0;

    _imethod = NULL;
    _iklass  = NULL;
    _entry_bci  = 0;
    _comp_level = 0;

    test();
  }

  ~CompileReplay() {
    if (_stream != NULL) fclose(_stream);
  }

  void test() {
    strcpy(_buffer, "1 2 foo 4 bar 0x9 \"this is it\"");
    _bufptr = _buffer;
    assert(parse_int("test") == 1, "what");
    assert(parse_int("test") == 2, "what");
    assert(strcmp(parse_string(), "foo") == 0, "what");
    assert(parse_int("test") == 4, "what");
    assert(strcmp(parse_string(), "bar") == 0, "what");
    assert(parse_intptr_t("test") == 9, "what");
    assert(strcmp(parse_quoted_string(), "this is it") == 0, "what");
  }

  bool had_error() {
    return _error_message != NULL || _thread->has_pending_exception();
  }

  bool can_replay() {
    return !(_stream == NULL || had_error());
  }

  void report_error(const char* msg) {
    _error_message = msg;
    // Restore the _buffer contents for error reporting
    for (int i = 0; i < _buffer_pos; i++) {
      if (_buffer[i] == '\0') _buffer[i] = ' ';
    }
  }

  int parse_int(const char* label) {
    if (had_error()) {
      return 0;
    }

    int v = 0;
    int read;
    if (sscanf(_bufptr, "%i%n", &v, &read) != 1) {
      report_error(label);
    } else {
      _bufptr += read;
    }
    return v;
  }

  intptr_t parse_intptr_t(const char* label) {
    if (had_error()) {
      return 0;
    }

    intptr_t v = 0;
    int read;
    if (sscanf(_bufptr, INTPTR_FORMAT "%n", &v, &read) != 1) {
      report_error(label);
    } else {
      _bufptr += read;
    }
    return v;
  }

  void skip_ws() {
    // Skip any leading whitespace
    while (*_bufptr == ' ' || *_bufptr == '\t') {
      _bufptr++;
    }
  }


  char* scan_and_terminate(char delim) {
    char* str = _bufptr;
    while (*_bufptr != delim && *_bufptr != '\0') {
      _bufptr++;
    }
    if (*_bufptr != '\0') {
      *_bufptr++ = '\0';
    }
    if (_bufptr == str) {
      // nothing here
      return NULL;
    }
    return str;
  }

  char* parse_string() {
    if (had_error()) return NULL;

    skip_ws();
    return scan_and_terminate(' ');
  }

  char* parse_quoted_string() {
    if (had_error()) return NULL;

    skip_ws();

    if (*_bufptr == '"') {
      _bufptr++;
      return scan_and_terminate('"');
    } else {
      return scan_and_terminate(' ');
    }
  }

  const char* parse_escaped_string() {
    char* result = parse_quoted_string();
    if (result != NULL) {
      unescape_string(result);
    }
    return result;
  }

  // Look for the tag 'tag' followed by an
  bool parse_tag_and_count(const char* tag, int& length) {
    const char* t = parse_string();
    if (t == NULL) {
      return false;
    }

    if (strcmp(tag, t) != 0) {
      report_error(tag);
      return false;
    }
    length = parse_int("parse_tag_and_count");
    return !had_error();
  }

  // Parse a sequence of raw data encoded as bytes and return the
  // resulting data.
  char* parse_data(const char* tag, int& length) {
    int read_size = 0;
    if (!parse_tag_and_count(tag, read_size)) {
      return NULL;
    }

    int actual_size = sizeof(MethodData::CompilerCounters);
    char *result = NEW_RESOURCE_ARRAY(char, actual_size);
    int i = 0;
    if (read_size != actual_size) {
      tty->print_cr("Warning: ciMethodData parsing sees MethodData size %i in file, current is %i", read_size,
                    actual_size);
      // Replay serializes the entire MethodData, but the data is at the end.
      // If the MethodData instance size has changed, we can pad or truncate in the beginning
      int padding = actual_size - read_size;
      if (padding > 0) {
        // pad missing data with zeros
        tty->print_cr("- Padding MethodData");
        for (; i < padding; i++) {
          result[i] = 0;
        }
      } else if (padding < 0) {
        // drop some data
        tty->print_cr("- Truncating MethodData");
        for (int j = 0; j < -padding; j++) {
          int val = parse_int("data");
          // discard val
        }
      }
    }

    assert(i < actual_size, "At least some data must remain to be copied");
    for (; i < actual_size; i++) {
      int val = parse_int("data");
      result[i] = val;
    }
    length = actual_size;
    return result;
  }

  // Parse a standard chunk of data emitted as:
  //   'tag' <length> # # ...
  // Where each # is an intptr_t item
  intptr_t* parse_intptr_data(const char* tag, int& length) {
    if (!parse_tag_and_count(tag, length)) {
      return NULL;
    }

    intptr_t* result = NEW_RESOURCE_ARRAY(intptr_t, length);
    for (int i = 0; i < length; i++) {
      skip_ws();
      intptr_t val = parse_intptr_t("data");
      result[i] = val;
    }
    return result;
  }

  // Parse a possibly quoted version of a symbol into a symbolOop
  Symbol* parse_symbol(TRAPS) {
    const char* str = parse_escaped_string();
    if (str != NULL) {
      Symbol* sym = SymbolTable::new_symbol(str);
      return sym;
    }
    return NULL;
  }

  // Parse a valid klass name and look it up
  Klass* parse_klass(TRAPS) {
    const char* str = parse_escaped_string();
    Symbol* klass_name = SymbolTable::new_symbol(str);
    if (klass_name != NULL) {
      Klass* k = NULL;
      if (_iklass != NULL) {
        k = (Klass*)_iklass->find_klass(ciSymbol::make(klass_name->as_C_string()))->constant_encoding();
      } else {
        k = SystemDictionary::resolve_or_fail(klass_name, _loader, _protection_domain, true, THREAD);
      }
      if (HAS_PENDING_EXCEPTION) {
        oop throwable = PENDING_EXCEPTION;
        java_lang_Throwable::print(throwable, tty);
        tty->cr();
        report_error(str);
        if (ReplayIgnoreInitErrors) {
          CLEAR_PENDING_EXCEPTION;
          _error_message = NULL;
        }
        return NULL;
      }
      return k;
    }
    return NULL;
  }

  // Lookup a klass
  Klass* resolve_klass(const char* klass, TRAPS) {
    Symbol* klass_name = SymbolTable::new_symbol(klass);
    return SystemDictionary::resolve_or_fail(klass_name, _loader, _protection_domain, true, THREAD);
  }

  // Parse the standard tuple of <klass> <name> <signature>
  Method* parse_method(TRAPS) {
    InstanceKlass* k = (InstanceKlass*)parse_klass(CHECK_NULL);
    if (k == NULL) {
      report_error("Can't find holder klass");
      return NULL;
    }
    Symbol* method_name = parse_symbol(CHECK_NULL);
    Symbol* method_signature = parse_symbol(CHECK_NULL);
    Method* m = k->find_method(method_name, method_signature);
    if (m == NULL) {
      report_error("Can't find method");
    }
    return m;
  }

  int get_line(int c) {
    while(c != EOF) {
      if (_buffer_pos + 1 >= _buffer_length) {
        int new_length = _buffer_length * 2;
        // Next call will throw error in case of OOM.
        _buffer = REALLOC_RESOURCE_ARRAY(char, _buffer, _buffer_length, new_length);
        _buffer_length = new_length;
      }
      if (c == '\n') {
        c = getc(_stream); // get next char
        break;
      } else if (c == '\r') {
        // skip LF
      } else {
        _buffer[_buffer_pos++] = c;
      }
      c = getc(_stream);
    }
    // null terminate it, reset the pointer
    _buffer[_buffer_pos] = '\0'; // NL or EOF
    _buffer_pos = 0;
    _bufptr = _buffer;
    return c;
  }

  // Process each line of the replay file executing each command until
  // the file ends.
  void process(TRAPS) {
    int line_no = 1;
    int c = getc(_stream);
    while(c != EOF) {
      c = get_line(c);
      process_command(THREAD);
      if (had_error()) {
        tty->print_cr("Error while parsing line %d: %s\n", line_no, _error_message);
        if (ReplayIgnoreInitErrors) {
          CLEAR_PENDING_EXCEPTION;
          _error_message = NULL;
        } else {
          return;
        }
      }
      line_no++;
    }
  }

  void process_command(TRAPS) {
    char* cmd = parse_string();
    if (cmd == NULL) {
      return;
    }
    if (strcmp("#", cmd) == 0) {
      // ignore
    } else if (strcmp("compile", cmd) == 0) {
      process_compile(CHECK);
    } else if (strcmp("ciMethod", cmd) == 0) {
      process_ciMethod(CHECK);
    } else if (strcmp("ciMethodData", cmd) == 0) {
      process_ciMethodData(CHECK);
    } else if (strcmp("staticfield", cmd) == 0) {
      process_staticfield(CHECK);
    } else if (strcmp("ciInstanceKlass", cmd) == 0) {
      process_ciInstanceKlass(CHECK);
    } else if (strcmp("instanceKlass", cmd) == 0) {
      process_instanceKlass(CHECK);
#if INCLUDE_JVMTI
    } else if (strcmp("JvmtiExport", cmd) == 0) {
      process_JvmtiExport(CHECK);
#endif // INCLUDE_JVMTI
    } else {
      report_error("unknown command");
    }
  }

  // validation of comp_level
  bool is_valid_comp_level(int comp_level) {
    const int msg_len = 256;
    char* msg = NULL;
    if (!is_compile(comp_level)) {
      msg = NEW_RESOURCE_ARRAY(char, msg_len);
      jio_snprintf(msg, msg_len, "%d isn't compilation level", comp_level);
    } else if (is_c1_compile(comp_level) && !CompilerConfig::is_c1_enabled()) {
      msg = NEW_RESOURCE_ARRAY(char, msg_len);
      jio_snprintf(msg, msg_len, "compilation level %d requires C1", comp_level);
    } else if (is_c2_compile(comp_level) && !CompilerConfig::is_c2_enabled()) {
      msg = NEW_RESOURCE_ARRAY(char, msg_len);
      jio_snprintf(msg, msg_len, "compilation level %d requires C2", comp_level);
    }
    if (msg != NULL) {
      report_error(msg);
      return false;
    }
    return true;
  }

  // compile <klass> <name> <signature> <entry_bci> <comp_level> inline <count> (<depth> <bci> <klass> <name> <signature>)*
  void* process_inline(ciMethod* imethod, Method* m, int entry_bci, int comp_level, TRAPS) {
    _imethod    = m;
    _iklass     = imethod->holder();
    _entry_bci  = entry_bci;
    _comp_level = comp_level;
    int line_no = 1;
    int c = getc(_stream);
    while(c != EOF) {
      c = get_line(c);
      // Expecting only lines with "compile" command in inline replay file.
      char* cmd = parse_string();
      if (cmd == NULL || strcmp("compile", cmd) != 0) {
        return NULL;
      }
      process_compile(CHECK_NULL);
      if (had_error()) {
        tty->print_cr("Error while parsing line %d: %s\n", line_no, _error_message);
        tty->print_cr("%s", _buffer);
        return NULL;
      }
      if (_ci_inline_records != NULL && _ci_inline_records->length() > 0) {
        // Found inlining record for the requested method.
        return _ci_inline_records;
      }
      line_no++;
    }
    return NULL;
  }

  // compile <klass> <name> <signature> <entry_bci> <comp_level> inline <count> (<depth> <bci> <klass> <name> <signature>)*
  void process_compile(TRAPS) {
    Method* method = parse_method(CHECK);
    if (had_error()) return;
    int entry_bci = parse_int("entry_bci");
    const char* comp_level_label = "comp_level";
    int comp_level = parse_int(comp_level_label);
    // old version w/o comp_level
    if (had_error() && (error_message() == comp_level_label)) {
      // use highest available tier
      comp_level = CompilationPolicy::highest_compile_level();
    }
    if (!is_valid_comp_level(comp_level)) {
      return;
    }
    if (_imethod != NULL) {
      // Replay Inlining
      if (entry_bci != _entry_bci || comp_level != _comp_level) {
        return;
      }
      const char* iklass_name  = _imethod->method_holder()->name()->as_utf8();
      const char* imethod_name = _imethod->name()->as_utf8();
      const char* isignature   = _imethod->signature()->as_utf8();
      const char* klass_name   = method->method_holder()->name()->as_utf8();
      const char* method_name  = method->name()->as_utf8();
      const char* signature    = method->signature()->as_utf8();
      if (strcmp(iklass_name,  klass_name)  != 0 ||
          strcmp(imethod_name, method_name) != 0 ||
          strcmp(isignature,   signature)   != 0) {
        return;
      }
    }
    int inline_count = 0;
    if (parse_tag_and_count("inline", inline_count)) {
      // Record inlining data
      _ci_inline_records = new GrowableArray<ciInlineRecord*>();
      for (int i = 0; i < inline_count; i++) {
        int depth = parse_int("inline_depth");
        int bci = parse_int("inline_bci");
        if (had_error()) {
          break;
        }
        Method* inl_method = parse_method(CHECK);
        if (had_error()) {
          break;
        }
        new_ciInlineRecord(inl_method, bci, depth);
      }
    }
    if (_imethod != NULL) {
      return; // Replay Inlining
    }
    InstanceKlass* ik = method->method_holder();
    ik->initialize(THREAD);
    if (HAS_PENDING_EXCEPTION) {
      oop throwable = PENDING_EXCEPTION;
      java_lang_Throwable::print(throwable, tty);
      tty->cr();
      if (ReplayIgnoreInitErrors) {
        CLEAR_PENDING_EXCEPTION;
        ik->set_init_state(InstanceKlass::fully_initialized);
      } else {
        return;
      }
    }
    // Make sure the existence of a prior compile doesn't stop this one
    CompiledMethod* nm = (entry_bci != InvocationEntryBci) ? method->lookup_osr_nmethod_for(entry_bci, comp_level, true) : method->code();
    if (nm != NULL) {
      nm->make_not_entrant();
    }
    replay_state = this;
    CompileBroker::compile_method(methodHandle(THREAD, method), entry_bci, comp_level,
                                  methodHandle(), 0, CompileTask::Reason_Replay, THREAD);
    replay_state = NULL;
    reset();
  }

  // ciMethod <klass> <name> <signature> <invocation_counter> <backedge_counter> <interpreter_invocation_count> <interpreter_throwout_count> <instructions_size>
  void process_ciMethod(TRAPS) {
    Method* method = parse_method(CHECK);
    if (had_error()) return;
    ciMethodRecord* rec = new_ciMethod(method);
    rec->_invocation_counter = parse_int("invocation_counter");
    rec->_backedge_counter = parse_int("backedge_counter");
    rec->_interpreter_invocation_count = parse_int("interpreter_invocation_count");
    rec->_interpreter_throwout_count = parse_int("interpreter_throwout_count");
    rec->_instructions_size = parse_int("instructions_size");
  }

  // ciMethodData <klass> <name> <signature> <state> <current_mileage> orig <length> <byte>* data <length> <ptr>* oops <length> (<offset> <klass>)* methods <length> (<offset> <klass> <name> <signature>)*
  void process_ciMethodData(TRAPS) {
    Method* method = parse_method(CHECK);
    if (had_error()) return;
    /* just copied from Method, to build interpret data*/

    // To be properly initialized, some profiling in the MDO needs the
    // method to be rewritten (number of arguments at a call for
    // instance)
    method->method_holder()->link_class(CHECK);
    // Method::build_interpreter_method_data(method, CHECK);
    {
      // Grab a lock here to prevent multiple
      // MethodData*s from being created.
      MutexLocker ml(THREAD, MethodData_lock);
      if (method->method_data() == NULL) {
        ClassLoaderData* loader_data = method->method_holder()->class_loader_data();
        MethodData* method_data = MethodData::allocate(loader_data, methodHandle(THREAD, method), CHECK);
        method->set_method_data(method_data);
      }
    }

    // collect and record all the needed information for later
    ciMethodDataRecord* rec = new_ciMethodData(method);
    rec->_state = parse_int("state");
    rec->_current_mileage = parse_int("current_mileage");

    rec->_orig_data = parse_data("orig", rec->_orig_data_length);
    if (rec->_orig_data == NULL) {
      return;
    }
    rec->_data = parse_intptr_data("data", rec->_data_length);
    if (rec->_data == NULL) {
      return;
    }
    if (!parse_tag_and_count("oops", rec->_classes_length)) {
      return;
    }
    rec->_classes = NEW_RESOURCE_ARRAY(Klass*, rec->_classes_length);
    rec->_classes_offsets = NEW_RESOURCE_ARRAY(int, rec->_classes_length);
    for (int i = 0; i < rec->_classes_length; i++) {
      int offset = parse_int("offset");
      if (had_error()) {
        return;
      }
      Klass* k = parse_klass(CHECK);
      rec->_classes_offsets[i] = offset;
      rec->_classes[i] = k;
    }

    if (!parse_tag_and_count("methods", rec->_methods_length)) {
      return;
    }
    rec->_methods = NEW_RESOURCE_ARRAY(Method*, rec->_methods_length);
    rec->_methods_offsets = NEW_RESOURCE_ARRAY(int, rec->_methods_length);
    for (int i = 0; i < rec->_methods_length; i++) {
      int offset = parse_int("offset");
      if (had_error()) {
        return;
      }
      Method* m = parse_method(CHECK);
      rec->_methods_offsets[i] = offset;
      rec->_methods[i] = m;
    }
  }

  // instanceKlass <name>
  //
  // Loads and initializes the klass 'name'.  This can be used to
  // create particular class loading environments
  void process_instanceKlass(TRAPS) {
    // just load the referenced class
    Klass* k = parse_klass(CHECK);
  }

  // ciInstanceKlass <name> <is_linked> <is_initialized> <length> tag*
  //
  // Load the klass 'name' and link or initialize it.  Verify that the
  // constant pool is the same length as 'length' and make sure the
  // constant pool tags are in the same state.
  void process_ciInstanceKlass(TRAPS) {
    InstanceKlass* k = (InstanceKlass *)parse_klass(CHECK);
    if (k == NULL) {
      return;
    }
    int is_linked = parse_int("is_linked");
    int is_initialized = parse_int("is_initialized");
    int length = parse_int("length");
    if (is_initialized) {
      k->initialize(THREAD);
      if (HAS_PENDING_EXCEPTION) {
        oop throwable = PENDING_EXCEPTION;
        java_lang_Throwable::print(throwable, tty);
        tty->cr();
        if (ReplayIgnoreInitErrors) {
          CLEAR_PENDING_EXCEPTION;
          k->set_init_state(InstanceKlass::fully_initialized);
        } else {
          return;
        }
      }
    } else if (is_linked) {
      k->link_class(CHECK);
    }
    ConstantPool* cp = k->constants();
    if (length != cp->length()) {
      report_error("constant pool length mismatch: wrong class files?");
      return;
    }

    int parsed_two_word = 0;
    for (int i = 1; i < length; i++) {
      int tag = parse_int("tag");
      if (had_error()) {
        return;
      }
      switch (cp->tag_at(i).value()) {
        case JVM_CONSTANT_UnresolvedClass: {
          if (tag == JVM_CONSTANT_Class) {
            tty->print_cr("Resolving klass %s at %d", cp->klass_name_at(i)->as_utf8(), i);
            Klass* k = cp->klass_at(i, CHECK);
          }
          break;
        }
        case JVM_CONSTANT_Long:
        case JVM_CONSTANT_Double:
          parsed_two_word = i + 1;

        case JVM_CONSTANT_ClassIndex:
        case JVM_CONSTANT_StringIndex:
        case JVM_CONSTANT_String:
        case JVM_CONSTANT_UnresolvedClassInError:
        case JVM_CONSTANT_Fieldref:
        case JVM_CONSTANT_Methodref:
        case JVM_CONSTANT_InterfaceMethodref:
        case JVM_CONSTANT_NameAndType:
        case JVM_CONSTANT_Utf8:
        case JVM_CONSTANT_Integer:
        case JVM_CONSTANT_Float:
        case JVM_CONSTANT_MethodHandle:
        case JVM_CONSTANT_MethodType:
        case JVM_CONSTANT_Dynamic:
        case JVM_CONSTANT_InvokeDynamic:
          if (tag != cp->tag_at(i).value()) {
            report_error("tag mismatch: wrong class files?");
            return;
          }
          break;

        case JVM_CONSTANT_Class:
          if (tag == JVM_CONSTANT_Class) {
          } else if (tag == JVM_CONSTANT_UnresolvedClass) {
            tty->print_cr("Warning: entry was unresolved in the replay data");
          } else {
            report_error("Unexpected tag");
            return;
          }
          break;

        case 0:
          if (parsed_two_word == i) continue;

        default:
          fatal("Unexpected tag: %d", cp->tag_at(i).value());
          break;
      }

    }
  }

  // staticfield <klass> <name> <signature> <value>
  //
  // Initialize a class and fill in the value for a static field.
  // This is useful when the compile was dependent on the value of
  // static fields but it's impossible to properly rerun the static
  // initializer.
  void process_staticfield(TRAPS) {
    InstanceKlass* k = (InstanceKlass *)parse_klass(CHECK);

    if (k == NULL || ReplaySuppressInitializers == 0 ||
        (ReplaySuppressInitializers == 2 && k->class_loader() == NULL)) {
      return;
    }

    assert(k->is_initialized(), "must be");

    const char* field_name = parse_escaped_string();
    const char* field_signature = parse_string();
    fieldDescriptor fd;
    Symbol* name = SymbolTable::new_symbol(field_name);
    Symbol* sig = SymbolTable::new_symbol(field_signature);
    if (!k->find_local_field(name, sig, &fd) ||
        !fd.is_static() ||
        fd.has_initial_value()) {
      report_error(field_name);
      return;
    }

    oop java_mirror = k->java_mirror();
    if (field_signature[0] == JVM_SIGNATURE_ARRAY) {
      int length = parse_int("array length");
      oop value = NULL;

      if (field_signature[1] == JVM_SIGNATURE_ARRAY) {
        // multi dimensional array
        ArrayKlass* kelem = (ArrayKlass *)parse_klass(CHECK);
        if (kelem == NULL) {
          return;
        }
        int rank = 0;
        while (field_signature[rank] == JVM_SIGNATURE_ARRAY) {
          rank++;
        }
        jint* dims = NEW_RESOURCE_ARRAY(jint, rank);
        dims[0] = length;
        for (int i = 1; i < rank; i++) {
          dims[i] = 1; // These aren't relevant to the compiler
        }
        value = kelem->multi_allocate(rank, dims, CHECK);
      } else {
        if (strcmp(field_signature, "[B") == 0) {
          value = oopFactory::new_byteArray(length, CHECK);
        } else if (strcmp(field_signature, "[Z") == 0) {
          value = oopFactory::new_boolArray(length, CHECK);
        } else if (strcmp(field_signature, "[C") == 0) {
          value = oopFactory::new_charArray(length, CHECK);
        } else if (strcmp(field_signature, "[S") == 0) {
          value = oopFactory::new_shortArray(length, CHECK);
        } else if (strcmp(field_signature, "[F") == 0) {
          value = oopFactory::new_floatArray(length, CHECK);
        } else if (strcmp(field_signature, "[D") == 0) {
          value = oopFactory::new_doubleArray(length, CHECK);
        } else if (strcmp(field_signature, "[I") == 0) {
          value = oopFactory::new_intArray(length, CHECK);
        } else if (strcmp(field_signature, "[J") == 0) {
          value = oopFactory::new_longArray(length, CHECK);
        } else if (field_signature[0] == JVM_SIGNATURE_ARRAY &&
                   field_signature[1] == JVM_SIGNATURE_CLASS) {
          Klass* kelem = resolve_klass(field_signature + 1, CHECK);
          value = oopFactory::new_objArray(kelem, length, CHECK);
        } else {
          report_error("unhandled array staticfield");
        }
      }
      java_mirror->obj_field_put(fd.offset(), value);
    } else {
      const char* string_value = parse_escaped_string();
      if (strcmp(field_signature, "I") == 0) {
        int value = atoi(string_value);
        java_mirror->int_field_put(fd.offset(), value);
      } else if (strcmp(field_signature, "B") == 0) {
        int value = atoi(string_value);
        java_mirror->byte_field_put(fd.offset(), value);
      } else if (strcmp(field_signature, "C") == 0) {
        int value = atoi(string_value);
        java_mirror->char_field_put(fd.offset(), value);
      } else if (strcmp(field_signature, "S") == 0) {
        int value = atoi(string_value);
        java_mirror->short_field_put(fd.offset(), value);
      } else if (strcmp(field_signature, "Z") == 0) {
        int value = atoi(string_value);
        java_mirror->bool_field_put(fd.offset(), value);
      } else if (strcmp(field_signature, "J") == 0) {
        jlong value;
        if (sscanf(string_value, JLONG_FORMAT, &value) != 1) {
          fprintf(stderr, "Error parsing long: %s\n", string_value);
          return;
        }
        java_mirror->long_field_put(fd.offset(), value);
      } else if (strcmp(field_signature, "F") == 0) {
        float value = atof(string_value);
        java_mirror->float_field_put(fd.offset(), value);
      } else if (strcmp(field_signature, "D") == 0) {
        double value = atof(string_value);
        java_mirror->double_field_put(fd.offset(), value);
      } else if (strcmp(field_signature, "Ljava/lang/String;") == 0) {
        Handle value = java_lang_String::create_from_str(string_value, CHECK);
        java_mirror->obj_field_put(fd.offset(), value());
      } else if (field_signature[0] == JVM_SIGNATURE_CLASS) {
        Klass* k = resolve_klass(string_value, CHECK);
        oop value = InstanceKlass::cast(k)->allocate_instance(CHECK);
        java_mirror->obj_field_put(fd.offset(), value);
      } else {
        report_error("unhandled staticfield");
      }
    }
  }

#if INCLUDE_JVMTI
  // JvmtiExport <field> <value>
  void process_JvmtiExport(TRAPS) {
    const char* field = parse_string();
    bool value = parse_int("JvmtiExport flag") != 0;
    if (strcmp(field, "can_access_local_variables") == 0) {
      JvmtiExport::set_can_access_local_variables(value);
    } else if (strcmp(field, "can_hotswap_or_post_breakpoint") == 0) {
      JvmtiExport::set_can_hotswap_or_post_breakpoint(value);
    } else if (strcmp(field, "can_post_on_exceptions") == 0) {
      JvmtiExport::set_can_post_on_exceptions(value);
    } else {
      report_error("Unrecognized JvmtiExport directive");
    }
  }
#endif // INCLUDE_JVMTI

  // Create and initialize a record for a ciMethod
  ciMethodRecord* new_ciMethod(Method* method) {
    ciMethodRecord* rec = NEW_RESOURCE_OBJ(ciMethodRecord);
    rec->_klass_name =  method->method_holder()->name()->as_utf8();
    rec->_method_name = method->name()->as_utf8();
    rec->_signature = method->signature()->as_utf8();
    _ci_method_records.append(rec);
    return rec;
  }

  // Lookup data for a ciMethod
  ciMethodRecord* find_ciMethodRecord(Method* method) {
    const char* klass_name =  method->method_holder()->name()->as_utf8();
    const char* method_name = method->name()->as_utf8();
    const char* signature = method->signature()->as_utf8();
    for (int i = 0; i < _ci_method_records.length(); i++) {
      ciMethodRecord* rec = _ci_method_records.at(i);
      if (strcmp(rec->_klass_name, klass_name) == 0 &&
          strcmp(rec->_method_name, method_name) == 0 &&
          strcmp(rec->_signature, signature) == 0) {
        return rec;
      }
    }
    return NULL;
  }

  // Create and initialize a record for a ciMethodData
  ciMethodDataRecord* new_ciMethodData(Method* method) {
    ciMethodDataRecord* rec = NEW_RESOURCE_OBJ(ciMethodDataRecord);
    rec->_klass_name =  method->method_holder()->name()->as_utf8();
    rec->_method_name = method->name()->as_utf8();
    rec->_signature = method->signature()->as_utf8();
    _ci_method_data_records.append(rec);
    return rec;
  }

  // Lookup data for a ciMethodData
  ciMethodDataRecord* find_ciMethodDataRecord(Method* method) {
    const char* klass_name =  method->method_holder()->name()->as_utf8();
    const char* method_name = method->name()->as_utf8();
    const char* signature = method->signature()->as_utf8();
    for (int i = 0; i < _ci_method_data_records.length(); i++) {
      ciMethodDataRecord* rec = _ci_method_data_records.at(i);
      if (strcmp(rec->_klass_name, klass_name) == 0 &&
          strcmp(rec->_method_name, method_name) == 0 &&
          strcmp(rec->_signature, signature) == 0) {
        return rec;
      }
    }
    return NULL;
  }

  // Create and initialize a record for a ciInlineRecord
  ciInlineRecord* new_ciInlineRecord(Method* method, int bci, int depth) {
    ciInlineRecord* rec = NEW_RESOURCE_OBJ(ciInlineRecord);
    rec->_klass_name =  method->method_holder()->name()->as_utf8();
    rec->_method_name = method->name()->as_utf8();
    rec->_signature = method->signature()->as_utf8();
    rec->_inline_bci = bci;
    rec->_inline_depth = depth;
    _ci_inline_records->append(rec);
    return rec;
  }

  // Lookup inlining data for a ciMethod
  ciInlineRecord* find_ciInlineRecord(Method* method, int bci, int depth) {
    if (_ci_inline_records != NULL) {
      return find_ciInlineRecord(_ci_inline_records, method, bci, depth);
    }
    return NULL;
  }

  static ciInlineRecord* find_ciInlineRecord(GrowableArray<ciInlineRecord*>*  records,
                                      Method* method, int bci, int depth) {
    if (records != NULL) {
      const char* klass_name  = method->method_holder()->name()->as_utf8();
      const char* method_name = method->name()->as_utf8();
      const char* signature   = method->signature()->as_utf8();
      for (int i = 0; i < records->length(); i++) {
        ciInlineRecord* rec = records->at(i);
        if ((rec->_inline_bci == bci) &&
            (rec->_inline_depth == depth) &&
            (strcmp(rec->_klass_name, klass_name) == 0) &&
            (strcmp(rec->_method_name, method_name) == 0) &&
            (strcmp(rec->_signature, signature) == 0)) {
          return rec;
        }
      }
    }
    return NULL;
  }

  const char* error_message() {
    return _error_message;
  }

  void reset() {
    _error_message = NULL;
    _ci_method_records.clear();
    _ci_method_data_records.clear();
  }

  // Take an ascii string contain \u#### escapes and convert it to utf8
  // in place.
  static void unescape_string(char* value) {
    char* from = value;
    char* to = value;
    while (*from != '\0') {
      if (*from != '\\') {
        *from++ = *to++;
      } else {
        switch (from[1]) {
          case 'u': {
            from += 2;
            jchar value=0;
            for (int i=0; i<4; i++) {
              char c = *from++;
              switch (c) {
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                  value = (value << 4) + c - '0';
                  break;
                case 'a': case 'b': case 'c':
                case 'd': case 'e': case 'f':
                  value = (value << 4) + 10 + c - 'a';
                  break;
                case 'A': case 'B': case 'C':
                case 'D': case 'E': case 'F':
                  value = (value << 4) + 10 + c - 'A';
                  break;
                default:
                  ShouldNotReachHere();
              }
            }
            UNICODE::convert_to_utf8(&value, 1, to);
            to++;
            break;
          }
          case 't': *to++ = '\t'; from += 2; break;
          case 'n': *to++ = '\n'; from += 2; break;
          case 'r': *to++ = '\r'; from += 2; break;
          case 'f': *to++ = '\f'; from += 2; break;
          default:
            ShouldNotReachHere();
        }
      }
    }
    *from = *to;
  }
};

void ciReplay::replay(TRAPS) {
  int exit_code = replay_impl(THREAD);

  Threads::destroy_vm();

  vm_exit(exit_code);
}

void* ciReplay::load_inline_data(ciMethod* method, int entry_bci, int comp_level) {
  if (FLAG_IS_DEFAULT(InlineDataFile)) {
    tty->print_cr("ERROR: no inline replay data file specified (use -XX:InlineDataFile=inline_pid12345.txt).");
    return NULL;
  }

  VM_ENTRY_MARK;
  // Load and parse the replay data
  CompileReplay rp(InlineDataFile, THREAD);
  if (!rp.can_replay()) {
    tty->print_cr("ciReplay: !rp.can_replay()");
    return NULL;
  }
  void* data = rp.process_inline(method, method->get_Method(), entry_bci, comp_level, THREAD);
  if (HAS_PENDING_EXCEPTION) {
    Handle throwable(THREAD, PENDING_EXCEPTION);
    CLEAR_PENDING_EXCEPTION;
    java_lang_Throwable::print_stack_trace(throwable, tty);
    tty->cr();
    return NULL;
  }

  if (rp.had_error()) {
    tty->print_cr("ciReplay: Failed on %s", rp.error_message());
    return NULL;
  }
  return data;
}

int ciReplay::replay_impl(TRAPS) {
  HandleMark hm(THREAD);
  ResourceMark rm(THREAD);

  if (ReplaySuppressInitializers > 2) {
    // ReplaySuppressInitializers > 2 means that we want to allow
    // normal VM bootstrap but once we get into the replay itself
    // don't allow any intializers to be run.
    ReplaySuppressInitializers = 1;
  }

  if (FLAG_IS_DEFAULT(ReplayDataFile)) {
    tty->print_cr("ERROR: no compiler replay data file specified (use -XX:ReplayDataFile=replay_pid12345.txt).");
    return 1;
  }

  // Load and parse the replay data
  CompileReplay rp(ReplayDataFile, THREAD);
  int exit_code = 0;
  if (rp.can_replay()) {
    rp.process(THREAD);
  } else {
    exit_code = 1;
    return exit_code;
  }

  if (HAS_PENDING_EXCEPTION) {
    Handle throwable(THREAD, PENDING_EXCEPTION);
    CLEAR_PENDING_EXCEPTION;
    java_lang_Throwable::print_stack_trace(throwable, tty);
    tty->cr();
    exit_code = 2;
  }

  if (rp.had_error()) {
    tty->print_cr("Failed on %s", rp.error_message());
    exit_code = 1;
  }
  return exit_code;
}

void ciReplay::initialize(ciMethodData* m) {
  if (replay_state == NULL) {
    return;
  }

  ASSERT_IN_VM;
  ResourceMark rm;

  Method* method = m->get_MethodData()->method();
  ciMethodDataRecord* rec = replay_state->find_ciMethodDataRecord(method);
  if (rec == NULL) {
    // This indicates some mismatch with the original environment and
    // the replay environment though it's not always enough to
    // interfere with reproducing a bug
    tty->print_cr("Warning: requesting ciMethodData record for method with no data: ");
    method->print_name(tty);
    tty->cr();
  } else {
    m->_state = rec->_state;
    m->_current_mileage = rec->_current_mileage;
    if (rec->_data_length != 0) {
      assert(m->_data_size + m->_extra_data_size == rec->_data_length * (int)sizeof(rec->_data[0]) ||
             m->_data_size == rec->_data_length * (int)sizeof(rec->_data[0]), "must agree");

      // Write the correct ciObjects back into the profile data
      ciEnv* env = ciEnv::current();
      for (int i = 0; i < rec->_classes_length; i++) {
        Klass *k = rec->_classes[i];
        // In case this class pointer is is tagged, preserve the tag bits
        intptr_t status = 0;
        if (k != NULL) {
          status = ciTypeEntries::with_status(env->get_metadata(k)->as_klass(), rec->_data[rec->_classes_offsets[i]]);
        }
        rec->_data[rec->_classes_offsets[i]] = status;
      }
      for (int i = 0; i < rec->_methods_length; i++) {
        Method *m = rec->_methods[i];
        *(ciMetadata**)(rec->_data + rec->_methods_offsets[i]) =
          env->get_metadata(m);
      }
      // Copy the updated profile data into place as intptr_ts
#ifdef _LP64
      Copy::conjoint_jlongs_atomic((jlong *)rec->_data, (jlong *)m->_data, rec->_data_length);
#else
      Copy::conjoint_jints_atomic((jint *)rec->_data, (jint *)m->_data, rec->_data_length);
#endif
    }

    // copy in the original header
    Copy::conjoint_jbytes(rec->_orig_data, (char*)&m->_orig, rec->_orig_data_length);
  }
}


bool ciReplay::should_not_inline(ciMethod* method) {
  if (replay_state == NULL) {
    return false;
  }
  VM_ENTRY_MARK;
  // ciMethod without a record shouldn't be inlined.
  return replay_state->find_ciMethodRecord(method->get_Method()) == NULL;
}

bool ciReplay::should_inline(void* data, ciMethod* method, int bci, int inline_depth) {
  if (data != NULL) {
    GrowableArray<ciInlineRecord*>*  records = (GrowableArray<ciInlineRecord*>*)data;
    VM_ENTRY_MARK;
    // Inline record are ordered by bci and depth.
    return CompileReplay::find_ciInlineRecord(records, method->get_Method(), bci, inline_depth) != NULL;
  } else if (replay_state != NULL) {
    VM_ENTRY_MARK;
    // Inline record are ordered by bci and depth.
    return replay_state->find_ciInlineRecord(method->get_Method(), bci, inline_depth) != NULL;
  }
  return false;
}

bool ciReplay::should_not_inline(void* data, ciMethod* method, int bci, int inline_depth) {
  if (data != NULL) {
    GrowableArray<ciInlineRecord*>*  records = (GrowableArray<ciInlineRecord*>*)data;
    VM_ENTRY_MARK;
    // Inline record are ordered by bci and depth.
    return CompileReplay::find_ciInlineRecord(records, method->get_Method(), bci, inline_depth) == NULL;
  } else if (replay_state != NULL) {
    VM_ENTRY_MARK;
    // Inline record are ordered by bci and depth.
    return replay_state->find_ciInlineRecord(method->get_Method(), bci, inline_depth) == NULL;
  }
  return false;
}

void ciReplay::initialize(ciMethod* m) {
  if (replay_state == NULL) {
    return;
  }

  ASSERT_IN_VM;
  ResourceMark rm;

  Method* method = m->get_Method();
  ciMethodRecord* rec = replay_state->find_ciMethodRecord(method);
  if (rec == NULL) {
    // This indicates some mismatch with the original environment and
    // the replay environment though it's not always enough to
    // interfere with reproducing a bug
    tty->print_cr("Warning: requesting ciMethod record for method with no data: ");
    method->print_name(tty);
    tty->cr();
  } else {
    EXCEPTION_CONTEXT;
    // m->_instructions_size = rec->_instructions_size;
    m->_instructions_size = -1;
    m->_interpreter_invocation_count = rec->_interpreter_invocation_count;
    m->_interpreter_throwout_count = rec->_interpreter_throwout_count;
    MethodCounters* mcs = method->get_method_counters(CHECK_AND_CLEAR);
    guarantee(mcs != NULL, "method counters allocation failed");
    mcs->invocation_counter()->_counter = rec->_invocation_counter;
    mcs->backedge_counter()->_counter = rec->_backedge_counter;
  }
}

bool ciReplay::is_loaded(Method* method) {
  if (replay_state == NULL) {
    return true;
  }

  ASSERT_IN_VM;
  ResourceMark rm;

  ciMethodRecord* rec = replay_state->find_ciMethodRecord(method);
  return rec != NULL;
}
#endif // PRODUCT
