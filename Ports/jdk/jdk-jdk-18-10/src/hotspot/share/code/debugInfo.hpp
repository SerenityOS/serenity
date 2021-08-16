/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CODE_DEBUGINFO_HPP
#define SHARE_CODE_DEBUGINFO_HPP

#include "code/compressedStream.hpp"
#include "code/location.hpp"
#include "code/nmethod.hpp"
#include "code/oopRecorder.hpp"
#include "runtime/stackValue.hpp"
#include "runtime/thread.hpp"
#include "utilities/growableArray.hpp"

// Classes used for serializing debugging information.
// These abstractions are introducted to provide symmetric
// read and write operations.

// ScopeValue        describes the value of a variable/expression in a scope
// - LocationValue   describes a value in a given location (in frame or register)
// - ConstantValue   describes a constant

class ConstantOopReadValue;
class LocationValue;
class ObjectValue;

class ScopeValue: public ResourceObj {
 public:
  // Testers
  virtual bool is_location() const { return false; }
  virtual bool is_object() const { return false; }
  virtual bool is_auto_box() const { return false; }
  virtual bool is_marker() const { return false; }
  virtual bool is_constant_int() const { return false; }
  virtual bool is_constant_double() const { return false; }
  virtual bool is_constant_long() const { return false; }
  virtual bool is_constant_oop() const { return false; }
  virtual bool equals(ScopeValue* other) const { return false; }

  ConstantOopReadValue* as_ConstantOopReadValue() {
    assert(is_constant_oop(), "must be");
    return (ConstantOopReadValue*) this;
  }

  ObjectValue* as_ObjectValue() {
    assert(is_object(), "must be");
    return (ObjectValue*)this;
  }

  LocationValue* as_LocationValue() {
    assert(is_location(), "must be");
    return (LocationValue*)this;
  }

  // Serialization of debugging information
  virtual void write_on(DebugInfoWriteStream* stream) = 0;
  static ScopeValue* read_from(DebugInfoReadStream* stream);
};


// A Location value describes a value in a given location; i.e. the corresponding
// logical entity (e.g., a method temporary) lives in this location.

class LocationValue: public ScopeValue {
 private:
  Location  _location;
 public:
  LocationValue(Location location)           { _location = location; }
  bool      is_location() const              { return true; }
  Location  location() const                 { return _location; }

  // Serialization of debugging information
  LocationValue(DebugInfoReadStream* stream);
  void write_on(DebugInfoWriteStream* stream);

  // Printing
  void print_on(outputStream* st) const;
};

// A placeholder value that has no concrete meaning other than helping constructing
// other values.

class MarkerValue: public ScopeValue {
public:
  bool      is_marker() const                { return true; }

  // Serialization of debugging information
  void write_on(DebugInfoWriteStream* stream);

  // Printing
  void print_on(outputStream* st) const;
};

// An ObjectValue describes an object eliminated by escape analysis.

class ObjectValue: public ScopeValue {
 protected:
  int                        _id;
  ScopeValue*                _klass;
  GrowableArray<ScopeValue*> _field_values;
  Handle                     _value;
  bool                       _visited;
 public:
  ObjectValue(int id, ScopeValue* klass)
     : _id(id)
     , _klass(klass)
     , _field_values()
     , _value()
     , _visited(false) {
    assert(klass->is_constant_oop(), "should be constant java mirror oop");
  }

  ObjectValue(int id)
     : _id(id)
     , _klass(NULL)
     , _field_values()
     , _value()
     , _visited(false) {}

  // Accessors
  bool                        is_object() const         { return true; }
  int                         id() const                { return _id; }
  ScopeValue*                 klass() const             { return _klass; }
  GrowableArray<ScopeValue*>* field_values()            { return &_field_values; }
  ScopeValue*                 field_at(int i) const     { return _field_values.at(i); }
  int                         field_size()              { return _field_values.length(); }
  Handle                      value() const             { return _value; }
  bool                        is_visited() const        { return _visited; }

  void                        set_value(oop value);
  void                        set_visited(bool visited) { _visited = visited; }

  // Serialization of debugging information
  void read_object(DebugInfoReadStream* stream);
  void write_on(DebugInfoWriteStream* stream);

  // Printing
  void print_on(outputStream* st) const;
  void print_fields_on(outputStream* st) const;
};

class AutoBoxObjectValue : public ObjectValue {
  bool                       _cached;
public:
  bool                       is_auto_box() const        { return true; }
  bool                       is_cached() const          { return _cached; }
  void                       set_cached(bool cached)    { _cached = cached; }
  AutoBoxObjectValue(int id, ScopeValue* klass) : ObjectValue(id, klass), _cached(false) { }
  AutoBoxObjectValue(int id) : ObjectValue(id), _cached(false) { }
};


// A ConstantIntValue describes a constant int; i.e., the corresponding logical entity
// is either a source constant or its computation has been constant-folded.

class ConstantIntValue: public ScopeValue {
 private:
  jint _value;
 public:
  ConstantIntValue(jint value)         { _value = value; }
  jint value() const                   { return _value;  }
  bool is_constant_int() const         { return true;    }
  bool equals(ScopeValue* other) const { return false;   }

  // Serialization of debugging information
  ConstantIntValue(DebugInfoReadStream* stream);
  void write_on(DebugInfoWriteStream* stream);

  // Printing
  void print_on(outputStream* st) const;
};

class ConstantLongValue: public ScopeValue {
 private:
  jlong _value;
 public:
  ConstantLongValue(jlong value)       { _value = value; }
  jlong value() const                  { return _value;  }
  bool is_constant_long() const        { return true;    }
  bool equals(ScopeValue* other) const { return false;   }

  // Serialization of debugging information
  ConstantLongValue(DebugInfoReadStream* stream);
  void write_on(DebugInfoWriteStream* stream);

  // Printing
  void print_on(outputStream* st) const;
};

class ConstantDoubleValue: public ScopeValue {
 private:
  jdouble _value;
 public:
  ConstantDoubleValue(jdouble value)   { _value = value; }
  jdouble value() const                { return _value;  }
  bool is_constant_double() const      { return true;    }
  bool equals(ScopeValue* other) const { return false;   }

  // Serialization of debugging information
  ConstantDoubleValue(DebugInfoReadStream* stream);
  void write_on(DebugInfoWriteStream* stream);

  // Printing
  void print_on(outputStream* st) const;
};

// A ConstantOopWriteValue is created by the compiler to
// be written as debugging information.

class ConstantOopWriteValue: public ScopeValue {
 private:
  jobject _value;
 public:
  ConstantOopWriteValue(jobject value) { _value = value; }
  jobject value() const                { return _value;  }
  bool is_constant_oop() const         { return true;    }
  bool equals(ScopeValue* other) const { return false;   }

  // Serialization of debugging information
  void write_on(DebugInfoWriteStream* stream);

  // Printing
  void print_on(outputStream* st) const;
};

// A ConstantOopReadValue is created by the VM when reading
// debug information

class ConstantOopReadValue: public ScopeValue {
 private:
  Handle _value;
 public:
  Handle value() const                 { return _value;  }
  bool is_constant_oop() const         { return true;    }
  bool equals(ScopeValue* other) const { return false;   }

  // Serialization of debugging information
  ConstantOopReadValue(DebugInfoReadStream* stream);
  void write_on(DebugInfoWriteStream* stream);

  // Printing
  void print_on(outputStream* st) const;
};

// MonitorValue describes the pair used for monitor_enter and monitor_exit.

class MonitorValue: public ResourceObj {
 private:
  ScopeValue* _owner;
  Location    _basic_lock;
  bool        _eliminated;
 public:
  // Constructor
  MonitorValue(ScopeValue* owner, Location basic_lock, bool eliminated = false);

  // Accessors
  ScopeValue*  owner()      const { return _owner; }
  Location     basic_lock() const { return _basic_lock;  }
  bool         eliminated() const { return _eliminated; }

  // Serialization of debugging information
  MonitorValue(DebugInfoReadStream* stream);
  void write_on(DebugInfoWriteStream* stream);

  // Printing
  void print_on(outputStream* st) const;
};

// DebugInfoReadStream specializes CompressedReadStream for reading
// debugging information. Used by ScopeDesc.

class DebugInfoReadStream : public CompressedReadStream {
 private:
  const CompiledMethod* _code;
  const CompiledMethod* code() const { return _code; }
  GrowableArray<ScopeValue*>* _obj_pool;
 public:
  DebugInfoReadStream(const CompiledMethod* code, int offset, GrowableArray<ScopeValue*>* obj_pool = NULL) :
    CompressedReadStream(code->scopes_data_begin(), offset) {
    _code = code;
    _obj_pool = obj_pool;

  } ;

  oop read_oop();
  Method* read_method() {
    Method* o = (Method*)(code()->metadata_at(read_int()));
    // is_metadata() is a faster check than is_metaspace_object()
    assert(o == NULL || o->is_metadata(), "meta data only");
    return o;
  }
  ScopeValue* read_object_value(bool is_auto_box);
  ScopeValue* get_cached_object();
  // BCI encoding is mostly unsigned, but -1 is a distinguished value
  int read_bci() { return read_int() + InvocationEntryBci; }
};

// DebugInfoWriteStream specializes CompressedWriteStream for
// writing debugging information. Used by ScopeDescRecorder.

class DebugInfoWriteStream : public CompressedWriteStream {
 private:
  DebugInformationRecorder* _recorder;
  DebugInformationRecorder* recorder() const { return _recorder; }
 public:
  DebugInfoWriteStream(DebugInformationRecorder* recorder, int initial_size);
  void write_handle(jobject h);
  void write_bci(int bci) { write_int(bci - InvocationEntryBci); }

  void write_metadata(Metadata* m);
};

#endif // SHARE_CODE_DEBUGINFO_HPP
