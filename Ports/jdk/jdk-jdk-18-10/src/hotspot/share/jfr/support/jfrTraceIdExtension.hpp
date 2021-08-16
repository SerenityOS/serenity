/*
* Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_SUPPORT_JFRTRACEIDEXTENSION_HPP
#define SHARE_JFR_SUPPORT_JFRTRACEIDEXTENSION_HPP

#include "jfr/recorder/checkpoint/types/traceid/jfrTraceId.hpp"
#include "utilities/macros.hpp"

#define DEFINE_TRACE_ID_FIELD mutable traceid _trace_id

#define DEFINE_TRACE_ID_METHODS \
  traceid trace_id() const { return _trace_id; } \
  traceid* const trace_id_addr() const { return &_trace_id; } \
  void set_trace_id(traceid id) const { _trace_id = id; }

#define DEFINE_TRACE_ID_SIZE \
  static size_t trace_id_size() { return sizeof(traceid); }

#define INIT_ID(data) JfrTraceId::assign(data)
#define ASSIGN_PRIMITIVE_CLASS_ID(data) JfrTraceId::assign_primitive_klass_id()
#define REMOVE_ID(k) JfrTraceId::remove(k);
#define REMOVE_METHOD_ID(method) JfrTraceId::remove(method);
#define RESTORE_ID(k) JfrTraceId::restore(k);

class JfrTraceFlag {
 private:
  mutable jshort _flags;
 public:
  JfrTraceFlag() : _flags(0) {}
  bool is_set(jshort flag) const {
    return (_flags & flag) != 0;
  }

  jshort flags() const {
    return _flags;
  }

  void set_flags(jshort flags) const {
    _flags = flags;
  }

  jbyte* flags_addr() const {
#ifdef VM_LITTLE_ENDIAN
    return (jbyte*)&_flags;
#else
    return ((jbyte*)&_flags) + 1;
#endif
  }

  jbyte* meta_addr() const {
#ifdef VM_LITTLE_ENDIAN
    return ((jbyte*)&_flags) + 1;
#else
    return (jbyte*)&_flags;
#endif
  }
};

#define DEFINE_TRACE_FLAG mutable JfrTraceFlag _trace_flags

#define DEFINE_TRACE_FLAG_ACCESSOR                 \
  bool is_trace_flag_set(jshort flag) const {      \
    return _trace_flags.is_set(flag);              \
  }                                                \
  jshort trace_flags() const {                     \
    return _trace_flags.flags();                   \
  }                                                \
  void set_trace_flags(jshort flags) const {       \
    _trace_flags.set_flags(flags);                 \
  }                                                \
  jbyte* trace_flags_addr() const {                \
    return _trace_flags.flags_addr();              \
  }                                                \
  jbyte* trace_meta_addr() const {                 \
    return _trace_flags.meta_addr();               \
  }

#endif // SHARE_JFR_SUPPORT_JFRTRACEIDEXTENSION_HPP
