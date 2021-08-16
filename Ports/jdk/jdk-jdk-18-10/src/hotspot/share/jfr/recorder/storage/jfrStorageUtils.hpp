/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_STORAGE_JFRSTORAGEUTILS_HPP
#define SHARE_JFR_RECORDER_STORAGE_JFRSTORAGEUTILS_HPP

#include "jfr/recorder/storage/jfrBuffer.hpp"
#include "jfr/recorder/repository/jfrChunkWriter.hpp"
#include "jfr/utilities/jfrAllocation.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "runtime/thread.hpp"

class CompositeOperationOr {
 public:
  static bool evaluate(bool value) {
    return !value;
  }
};

class CompositeOperationAnd {
 public:
  static bool evaluate(bool value) {
    return value;
  }
};

template <typename Operation, typename NextOperation, typename TruthFunction = CompositeOperationAnd>
class CompositeOperation {
 private:
  Operation* _op;
  NextOperation* _next;
 public:
  CompositeOperation(Operation* op, NextOperation* next) : _op(op), _next(next) {
    assert(_op != NULL, "invariant");
  }
  typedef typename Operation::Type Type;
  bool process(Type* t) {
    const bool op_result = _op->process(t);
    return _next == NULL ? op_result : TruthFunction::evaluate(op_result) ? _next->process(t) : op_result;
  }
  size_t elements() const {
    return _next == NULL ? _op->elements() : _op->elements() + _next->elements();
  }
  size_t size() const {
    return _next == NULL ? _op->size() : _op->size() + _next->size();
  }
};

template <typename T>
class UnBufferedWriteToChunk {
 private:
  JfrChunkWriter& _writer;
  size_t _elements;
  size_t _size;
 public:
  typedef T Type;
  UnBufferedWriteToChunk(JfrChunkWriter& writer) : _writer(writer), _elements(0), _size(0) {}
  bool write(Type* t, const u1* data, size_t size);
  size_t elements() const { return _elements; }
  size_t size() const { return _size; }
};

template <typename T>
class DefaultDiscarder {
 private:
  size_t _elements;
  size_t _size;
 public:
  typedef T Type;
  DefaultDiscarder() : _elements(0), _size(0) {}
  bool discard(Type* t, const u1* data, size_t size);
  size_t elements() const { return _elements; }
  size_t size() const { return _size; }
};

template <typename T, bool negation>
class Retired {
 public:
  typedef T Type;
  bool process(Type* t) {
    assert(t != NULL, "invariant");
    return negation ? !t->retired() : t->retired();
  }
};

template <typename T, bool negation>
class Excluded {
 public:
  typedef T Type;
  bool process(Type* t) {
    assert(t != NULL, "invariant");
    return negation ? !t->excluded() : t->excluded();
  }
};

template <typename Operation>
class MutexedWriteOp {
 private:
  Operation& _operation;
 public:
  typedef typename Operation::Type Type;
  MutexedWriteOp(Operation& operation) : _operation(operation) {}
  bool process(Type* t);
  size_t elements() const { return _operation.elements(); }
  size_t size() const { return _operation.size(); }
};

template <typename Operation, typename Predicate>
class PredicatedMutexedWriteOp : public MutexedWriteOp<Operation> {
 private:
  Predicate& _predicate;
 public:
  PredicatedMutexedWriteOp(Operation& operation, Predicate& predicate) :
    MutexedWriteOp<Operation>(operation), _predicate(predicate) {}
  bool process(typename Operation::Type* t) {
    return _predicate.process(t) ? MutexedWriteOp<Operation>::process(t) : true;
  }
};

template <typename Operation>
class ConcurrentWriteOp {
 private:
  Operation& _operation;
 public:
  typedef typename Operation::Type Type;
  ConcurrentWriteOp(Operation& operation) : _operation(operation) {}
  bool process(Type* t);
  size_t elements() const { return _operation.elements(); }
  size_t size() const { return _operation.size(); }
};

template <typename Operation, typename Predicate>
class PredicatedConcurrentWriteOp : public ConcurrentWriteOp<Operation> {
 private:
  Predicate& _predicate;
 public:
  PredicatedConcurrentWriteOp(Operation& operation, Predicate& predicate) :
    ConcurrentWriteOp<Operation>(operation), _predicate(predicate) {}
  bool process(typename Operation::Type* t) {
    return _predicate.process(t) ? ConcurrentWriteOp<Operation>::process(t) : true;
  }
};

template <typename Operation>
class ExclusiveOp : private MutexedWriteOp<Operation> {
 public:
  typedef typename Operation::Type Type;
  ExclusiveOp(Operation& operation) : MutexedWriteOp<Operation>(operation) {}
  bool process(Type* t);
  size_t processed() const { return MutexedWriteOp<Operation>::processed(); }
};

enum jfr_operation_mode {
  mutexed = 1,
  concurrent
};

template <typename Operation>
class DiscardOp {
 private:
  Operation _operation;
  jfr_operation_mode _mode;
 public:
  typedef typename Operation::Type Type;
  DiscardOp(jfr_operation_mode mode = concurrent) : _operation(), _mode(mode) {}
  bool process(Type* t);
  size_t elements() const { return _operation.elements(); }
  size_t size() const { return _operation.size(); }
};

template <typename Operation>
class ExclusiveDiscardOp : private DiscardOp<Operation> {
 public:
  typedef typename Operation::Type Type;
  ExclusiveDiscardOp(jfr_operation_mode mode = concurrent) : DiscardOp<Operation>(mode) {}
  bool process(Type* t);
  size_t processed() const { return DiscardOp<Operation>::processed(); }
  size_t elements() const { return DiscardOp<Operation>::elements(); }
  size_t size() const { return DiscardOp<Operation>::size(); }
};

template <typename Operation>
class EpochDispatchOp {
  Operation& _operation;
  size_t _elements;
  bool _previous_epoch;
  size_t dispatch(bool previous_epoch, const u1* data, size_t size);
 public:
  typedef typename Operation::Type Type;
  EpochDispatchOp(Operation& operation, bool previous_epoch) :
    _operation(operation), _elements(0), _previous_epoch(previous_epoch) {}
  bool process(Type* t);
  size_t elements() const { return _elements; }
};

#endif // SHARE_JFR_RECORDER_STORAGE_JFRSTORAGEUTILS_HPP
