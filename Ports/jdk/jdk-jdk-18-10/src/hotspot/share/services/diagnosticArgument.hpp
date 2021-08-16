/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_SERVICES_DIAGNOSTICARGUMENT_HPP
#define SHARE_SERVICES_DIAGNOSTICARGUMENT_HPP

#include "classfile/vmSymbols.hpp"
#include "memory/allocation.hpp"
#include "runtime/os.hpp"
#include "runtime/thread.hpp"
#include "utilities/exceptions.hpp"

class StringArrayArgument : public CHeapObj<mtInternal> {
private:
  GrowableArray<char*>* _array;
public:
  StringArrayArgument();
  ~StringArrayArgument();

  void add(const char* str, size_t len);

  GrowableArray<char*>* array() {
    return _array;
  }
};

class NanoTimeArgument {
public:
  jlong _nanotime;
  jlong _time;
  char _unit[3];
};

class MemorySizeArgument {
public:
  u8 _size;
  u8 _val;
  char _multiplier;
};

class GenDCmdArgument : public ResourceObj {
protected:
  GenDCmdArgument* _next;
  const char* const _name;
  const char* const _description;
  const char* const _type;
  const char* const _default_string;
  bool              _is_set;
  const bool        _is_mandatory;
  bool             _allow_multiple;
  GenDCmdArgument(const char* name, const char* description, const char* type,
                  const char* default_string, bool mandatory)
    : _next(NULL), _name(name), _description(description), _type(type),
      _default_string(default_string), _is_set(false), _is_mandatory(mandatory),
      _allow_multiple(false) {}
public:
  const char* name() const        { return _name; }
  const char* description() const { return _description; }
  const char* type() const        { return _type; }
  const char* default_string() const { return _default_string; }
  bool is_set() const             { return _is_set; }
  void set_is_set(bool b)         { _is_set = b; }
  bool allow_multiple() const     { return _allow_multiple; }
  bool is_mandatory() const       { return _is_mandatory; }
  bool has_value() const          { return _is_set || _default_string != NULL; }
  bool has_default() const        { return _default_string != NULL; }
  void read_value(const char* str, size_t len, TRAPS);
  virtual void parse_value(const char* str, size_t len, TRAPS) = 0;
  virtual void init_value(TRAPS) = 0;
  virtual void reset(TRAPS) = 0;
  virtual void cleanup() = 0;
  virtual void value_as_str(char* buf, size_t len) const = 0;
  void set_next(GenDCmdArgument* arg) {
    _next = arg;
  }
  GenDCmdArgument* next() {
    return _next;
  }

  void to_string(jlong l, char* buf, size_t len) const;
  void to_string(bool b, char* buf, size_t len) const;
  void to_string(char* c, char* buf, size_t len) const;
  void to_string(NanoTimeArgument n, char* buf, size_t len) const;
  void to_string(MemorySizeArgument f, char* buf, size_t len) const;
  void to_string(StringArrayArgument* s, char* buf, size_t len) const;
};

template <class ArgType> class DCmdArgument: public GenDCmdArgument {
private:
  ArgType _value;
public:
  DCmdArgument(const char* name, const char* description, const char* type,
               bool mandatory) :
               GenDCmdArgument(name, description, type, NULL, mandatory) { }
  DCmdArgument(const char* name, const char* description, const char* type,
               bool mandatory, const char* defaultvalue) :
               GenDCmdArgument(name, description, type, defaultvalue, mandatory)
               { }
  ~DCmdArgument() { destroy_value(); }
  ArgType value() const { return _value;}
  void set_value(ArgType v) { _value = v; }
  void reset(TRAPS) {
    destroy_value();
    init_value(CHECK);
    _is_set = false;
  }
  void cleanup() {
    destroy_value();
  }
  void parse_value(const char* str, size_t len, TRAPS);
  void init_value(TRAPS);
  void destroy_value();
  void value_as_str(char *buf, size_t len) const { to_string(_value, buf, len);}
};

#endif // SHARE_SERVICES_DIAGNOSTICARGUMENT_HPP
