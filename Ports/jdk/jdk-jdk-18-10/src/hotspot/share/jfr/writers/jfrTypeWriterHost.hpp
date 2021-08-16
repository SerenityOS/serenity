/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_WRITERS_JFRTYPEWRITERHOST_HPP
#define SHARE_JFR_WRITERS_JFRTYPEWRITERHOST_HPP

#include "jfr/recorder/checkpoint/jfrCheckpointWriter.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "memory/allocation.hpp"

template <typename WriterImpl, u4 ID>
class JfrTypeWriterHost : public StackObj {
 private:
  WriterImpl _impl;
  JfrCheckpointWriter* _writer;
  JfrCheckpointContext _ctx;
  int64_t _count_offset;
  int _count;
  bool _skip_header;
 public:
  JfrTypeWriterHost(JfrCheckpointWriter* writer,
                    bool class_unload = false,
                    bool skip_header = false) : _impl(writer, class_unload),
                                                _writer(writer),
                                                _ctx(writer->context()),
                                                _count(0),
                                                _skip_header(skip_header) {
    assert(_writer != NULL, "invariant");
    if (!_skip_header) {
      _writer->write_type((JfrTypeId)ID);
      _count_offset = _writer->reserve(sizeof(u4)); // Don't know how many yet
    }
  }

  ~JfrTypeWriterHost() {
    if (_count == 0) {
      // nothing written, restore context for rewind
      _writer->set_context(_ctx);
      return;
    }
    assert(_count > 0, "invariant");
    if (!_skip_header) {
      _writer->write_count(_count, _count_offset);
    }
  }

  bool operator()(typename WriterImpl::Type const & value) {
    this->_count += _impl(value);
    return true;
  }

  int count() const   { return _count; }
  void add(int count) { _count += count; }
};

typedef int(*type_write_operation)(JfrCheckpointWriter*, const void*);

template <typename T, type_write_operation op>
class JfrTypeWriterImplHost {
 private:
  JfrCheckpointWriter* _writer;
 public:
  typedef T Type;
  JfrTypeWriterImplHost(JfrCheckpointWriter* writer, bool class_unload = false) : _writer(writer) {}
  int operator()(T const& value) {
    return op(this->_writer, value);
  }
};

template <typename T, typename Predicate, type_write_operation op>
class JfrPredicatedTypeWriterImplHost : public JfrTypeWriterImplHost<T, op> {
 private:
  Predicate _predicate;
  typedef JfrTypeWriterImplHost<T, op> Parent;
 public:
  JfrPredicatedTypeWriterImplHost(JfrCheckpointWriter* writer, bool class_unload = false) :
    Parent(writer), _predicate(class_unload) {}
  int operator()(T const& value) {
    return _predicate(value) ? Parent::operator()(value) : 0;
  }
};

#endif // SHARE_JFR_WRITERS_JFRTYPEWRITERHOST_HPP
