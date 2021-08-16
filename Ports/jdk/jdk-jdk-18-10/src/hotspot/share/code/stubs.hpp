/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CODE_STUBS_HPP
#define SHARE_CODE_STUBS_HPP

#include "asm/codeBuffer.hpp"
#include "memory/allocation.hpp"

// The classes in this file provide a simple framework for the
// management of little pieces of machine code - or stubs -
// created on the fly and frequently discarded. In this frame-
// work stubs are stored in a queue.


// Stub serves as abstract base class. A concrete stub
// implementation is a subclass of Stub, implementing
// all (non-virtual!) functions required sketched out
// in the Stub class.
//
// A concrete stub layout may look like this (both data
// and code sections could be empty as well):
//
//                ________
// stub       -->|        | <--+
//               |  data  |    |
//               |________|    |
// code_begin -->|        |    |
//               |        |    |
//               |  code  |    | size
//               |        |    |
//               |________|    |
// code_end   -->|        |    |
//               |  data  |    |
//               |________|    |
//                          <--+


class Stub {
 public:
  // Initialization/finalization
  void    initialize(int size,
                     CodeStrings& strings)       { ShouldNotCallThis(); }                // called to initialize/specify the stub's size
  void    finalize()                             { ShouldNotCallThis(); }                // called before the stub is deallocated

  // General info/converters
  int     size() const                           { ShouldNotCallThis(); return 0; }      // must return the size provided by initialize
  static  int code_size_to_size(int code_size)   { ShouldNotCallThis(); return 0; }      // computes the size given the code size

  // Code info
  address code_begin() const                     { ShouldNotCallThis(); return NULL; }   // points to the first byte of    the code
  address code_end() const                       { ShouldNotCallThis(); return NULL; }   // points to the first byte after the code

  // Debugging
  void    verify()                               { ShouldNotCallThis(); }                // verifies the Stub
  void    print()                                { ShouldNotCallThis(); }                // prints some information about the stub
};


// A stub interface defines the interface between a stub queue
// and the stubs it queues. In order to avoid a vtable and
// (and thus the extra word) in each stub, a concrete stub
// interface object is created and associated with a stub
// buffer which in turn uses the stub interface to interact
// with its stubs.
//
// StubInterface serves as an abstract base class. A concrete
// stub interface implementation is a subclass of StubInterface,
// forwarding its virtual function calls to non-virtual calls
// of the concrete stub (see also macro below). There's exactly
// one stub interface instance required per stub queue.

class StubInterface: public CHeapObj<mtCode> {
 public:
  // Initialization/finalization
  virtual void    initialize(Stub* self, int size,
                             CodeStrings& strings)         = 0; // called after creation (called twice if allocated via (request, commit))
  virtual void    finalize(Stub* self)                     = 0; // called before deallocation

  // General info/converters
  virtual int     size(Stub* self) const                   = 0; // the total size of the stub in bytes (must be a multiple of CodeEntryAlignment)
  virtual int     code_size_to_size(int code_size) const   = 0; // computes the total stub size in bytes given the code size in bytes

  // Code info
  virtual address code_begin(Stub* self) const             = 0; // points to the first code byte
  virtual address code_end(Stub* self) const               = 0; // points to the first byte after the code

  // Debugging
  virtual void    verify(Stub* self)                       = 0; // verifies the stub
  virtual void    print(Stub* self)                        = 0; // prints information about the stub
};


// DEF_STUB_INTERFACE is used to create a concrete stub interface
// class, forwarding stub interface calls to the corresponding
// stub calls.

#define DEF_STUB_INTERFACE(stub)                           \
  class stub##Interface: public StubInterface {            \
   private:                                                \
    static stub*    cast(Stub* self)                       { return (stub*)self; }                 \
                                                           \
   public:                                                 \
    /* Initialization/finalization */                      \
    virtual void    initialize(Stub* self, int size,       \
                               CodeStrings& strings)       { cast(self)->initialize(size, strings); } \
    virtual void    finalize(Stub* self)                   { cast(self)->finalize(); }             \
                                                           \
    /* General info */                                     \
    virtual int     size(Stub* self) const                 { return cast(self)->size(); }          \
    virtual int     code_size_to_size(int code_size) const { return stub::code_size_to_size(code_size); } \
                                                           \
    /* Code info */                                        \
    virtual address code_begin(Stub* self) const           { return cast(self)->code_begin(); }    \
    virtual address code_end(Stub* self) const             { return cast(self)->code_end(); }      \
                                                           \
    /* Debugging */                                        \
    virtual void    verify(Stub* self)                     { cast(self)->verify(); }               \
    virtual void    print(Stub* self)                      { cast(self)->print(); }                \
  };


// A StubQueue maintains a queue of stubs.
// Note: All sizes (spaces) are given in bytes.

class StubQueue: public CHeapObj<mtCode> {
  friend class VMStructs;
 private:
  StubInterface* _stub_interface;                // the interface prototype
  address        _stub_buffer;                   // where all stubs are stored
  int            _buffer_size;                   // the buffer size in bytes
  int            _buffer_limit;                  // the (byte) index of the actual buffer limit (_buffer_limit <= _buffer_size)
  int            _queue_begin;                   // the (byte) index of the first queue entry (word-aligned)
  int            _queue_end;                     // the (byte) index of the first entry after the queue (word-aligned)
  int            _number_of_stubs;               // the number of buffered stubs
  Mutex* const   _mutex;                         // the lock used for a (request, commit) transaction

  void  check_index(int i) const                 { assert(0 <= i && i < _buffer_limit && i % CodeEntryAlignment == 0, "illegal index"); }
  bool  is_contiguous() const                    { return _queue_begin <= _queue_end; }
  int   index_of(Stub* s) const                  { int i = (address)s - _stub_buffer; check_index(i); return i; }
  Stub* stub_at(int i) const                     { check_index(i); return (Stub*)(_stub_buffer + i); }
  Stub* current_stub() const                     { return stub_at(_queue_end); }

  // Stub functionality accessed via interface
  void  stub_initialize(Stub* s, int size,
                        CodeStrings& strings)    { assert(size % CodeEntryAlignment == 0, "size not aligned"); _stub_interface->initialize(s, size, strings); }
  void  stub_finalize(Stub* s)                   { _stub_interface->finalize(s); }
  int   stub_size(Stub* s) const                 { return _stub_interface->size(s); }
  bool  stub_contains(Stub* s, address pc) const { return _stub_interface->code_begin(s) <= pc && pc < _stub_interface->code_end(s); }
  int   stub_code_size_to_size(int code_size) const { return _stub_interface->code_size_to_size(code_size); }
  void  stub_verify(Stub* s)                     { _stub_interface->verify(s); }
  void  stub_print(Stub* s)                      { _stub_interface->print(s); }

 public:
  StubQueue(StubInterface* stub_interface, int buffer_size, Mutex* lock,
            const char* name);
  ~StubQueue();

  // General queue info
  bool  is_empty() const                         { return _queue_begin == _queue_end; }
  int   total_space() const                      { return _buffer_size - 1; }
  int   available_space() const                  { int d = _queue_begin - _queue_end - 1; return d < 0 ? d + _buffer_size : d; }
  int   used_space() const                       { return total_space() - available_space(); }
  int   number_of_stubs() const                  { return _number_of_stubs; }
  bool  contains(address pc) const               { return _stub_buffer <= pc && pc < _stub_buffer + _buffer_limit; }
  Stub* stub_containing(address pc) const;
  address code_start() const                     { return _stub_buffer; }
  address code_end() const                       { return _stub_buffer + _buffer_limit; }

  // Stub allocation (atomic transactions)
  Stub* request_committed(int code_size);        // request a stub that provides exactly code_size space for code
  Stub* request(int requested_code_size);        // request a stub with a (maximum) code space - locks the queue
  void  commit (int committed_code_size,
                CodeStrings& strings);           // commit the previously requested stub - unlocks the queue

  // Stub deallocation
  void  remove_first();                          // remove the first stub in the queue
  void  remove_first(int n);                     // remove the first n stubs in the queue
  void  remove_all();                            // remove all stubs in the queue

  void deallocate_unused_tail();                 // deallocate the unused tail of the underlying CodeBlob
                                                 // only used from TemplateInterpreter::initialize()
  // Iteration
  Stub* first() const                            { return number_of_stubs() > 0 ? stub_at(_queue_begin) : NULL; }
  Stub* next(Stub* s) const                      { int i = index_of(s) + stub_size(s);
                                                   // Only wrap around in the non-contiguous case (see stubss.cpp)
                                                   if (i == _buffer_limit && _queue_end < _buffer_limit) i = 0;
                                                   return (i == _queue_end) ? NULL : stub_at(i);
                                                 }

  // Debugging/printing
  void  verify();                                // verifies the stub queue
  void  print();                                 // prints information about the stub queue

};

#endif // SHARE_CODE_STUBS_HPP
