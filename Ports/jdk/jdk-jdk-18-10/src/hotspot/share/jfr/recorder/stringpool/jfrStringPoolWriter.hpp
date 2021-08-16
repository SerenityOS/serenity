/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_STRINGPOOL_JFRSTRINGPOOLWRITER_HPP
#define SHARE_JFR_RECORDER_STRINGPOOL_JFRSTRINGPOOLWRITER_HPP

#include "memory/allocation.hpp"
#include "jfr/recorder/stringpool/jfrStringPoolBuffer.hpp"
#include "jfr/writers/jfrEventWriterHost.hpp"
#include "jfr/writers/jfrMemoryWriterHost.hpp"
#include "jfr/writers/jfrStorageAdapter.hpp"

class Thread;

class JfrStringPoolFlush : public StackObj {
 public:
  typedef JfrStringPoolBuffer Type;
  JfrStringPoolFlush(Type* old, size_t used, size_t requested, Thread* t);
  Type* result() { return _result; }
 private:
  Type* _result;
};

typedef Adapter<JfrStringPoolFlush> JfrStringPoolAdapter;
typedef AcquireReleaseMemoryWriterHost<JfrStringPoolAdapter, StackObj> JfrTransactionalStringPoolWriter;
typedef EventWriterHost<BigEndianEncoder, CompressedIntegerEncoder, JfrTransactionalStringPoolWriter> JfrStringPoolWriterBase;

class JfrStringPoolWriter : public JfrStringPoolWriterBase {
 private:
  size_t _nof_strings;
 public:
  JfrStringPoolWriter(Thread* thread);
  ~JfrStringPoolWriter();
  void inc_nof_strings();
};

#endif // SHARE_JFR_RECORDER_STRINGPOOL_JFRSTRINGPOOLWRITER_HPP
