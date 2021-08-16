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

#ifndef SHARE_CODE_CODEBLOB_HPP
#define SHARE_CODE_CODEBLOB_HPP

#include "asm/codeBuffer.hpp"
#include "compiler/compilerDefinitions.hpp"
#include "runtime/javaFrameAnchor.hpp"
#include "runtime/frame.hpp"
#include "runtime/handles.hpp"
#include "utilities/align.hpp"
#include "utilities/macros.hpp"

class ImmutableOopMap;
class ImmutableOopMapSet;
class JNIHandleBlock;
class OopMapSet;

// CodeBlob Types
// Used in the CodeCache to assign CodeBlobs to different CodeHeaps
struct CodeBlobType {
  enum {
    MethodNonProfiled   = 0,    // Execution level 1 and 4 (non-profiled) nmethods (including native nmethods)
    MethodProfiled      = 1,    // Execution level 2 and 3 (profiled) nmethods
    NonNMethod          = 2,    // Non-nmethods like Buffers, Adapters and Runtime Stubs
    All                 = 3,    // All types (No code cache segmentation)
    NumTypes            = 4     // Number of CodeBlobTypes
  };
};

// CodeBlob - superclass for all entries in the CodeCache.
//
// Subtypes are:
//  CompiledMethod       : Compiled Java methods (include method that calls to native code)
//   nmethod             : JIT Compiled Java methods
//  RuntimeBlob          : Non-compiled method code; generated glue code
//   BufferBlob          : Used for non-relocatable code such as interpreter, stubroutines, etc.
//    AdapterBlob        : Used to hold C2I/I2C adapters
//    VtableBlob         : Used for holding vtable chunks
//    MethodHandlesAdapterBlob : Used to hold MethodHandles adapters
//    OptimizedEntryBlob : Used for upcalls from native code
//   RuntimeStub         : Call to VM runtime methods
//   SingletonBlob       : Super-class for all blobs that exist in only one instance
//    DeoptimizationBlob : Used for deoptimization
//    ExceptionBlob      : Used for stack unrolling
//    SafepointBlob      : Used to handle illegal instruction exceptions
//    UncommonTrapBlob   : Used to handle uncommon traps
//
//
// Layout : continuous in the CodeCache
//   - header
//   - relocation
//   - content space
//     - instruction space
//   - data space


class CodeBlobLayout;
class OptimizedEntryBlob; // for as_optimized_entry_blob()
class JavaFrameAnchor; // for OptimizedEntryBlob::jfa_for_frame

class CodeBlob {
  friend class VMStructs;
  friend class JVMCIVMStructs;
  friend class CodeCacheDumper;

protected:

  const CompilerType _type;                      // CompilerType
  int        _size;                              // total size of CodeBlob in bytes
  int        _header_size;                       // size of header (depends on subclass)
  int        _frame_complete_offset;             // instruction offsets in [0.._frame_complete_offset) have
                                                 // not finished setting up their frame. Beware of pc's in
                                                 // that range. There is a similar range(s) on returns
                                                 // which we don't detect.
  int        _data_offset;                       // offset to where data region begins
  int        _frame_size;                        // size of stack frame

  address    _code_begin;
  address    _code_end;
  address    _content_begin;                     // address to where content region begins (this includes consts, insts, stubs)
                                                 // address    _content_end - not required, for all CodeBlobs _code_end == _content_end for now
  address    _data_end;
  address    _relocation_begin;
  address    _relocation_end;

  ImmutableOopMapSet* _oop_maps;                 // OopMap for this CodeBlob
  bool                _caller_must_gc_arguments;

  const char*         _name;
  S390_ONLY(int       _ctable_offset;)

  NOT_PRODUCT(CodeStrings _strings;)

  CodeBlob(const char* name, CompilerType type, const CodeBlobLayout& layout, int frame_complete_offset, int frame_size, ImmutableOopMapSet* oop_maps, bool caller_must_gc_arguments);
  CodeBlob(const char* name, CompilerType type, const CodeBlobLayout& layout, CodeBuffer* cb, int frame_complete_offset, int frame_size, OopMapSet* oop_maps, bool caller_must_gc_arguments);

public:
  // Only used by unit test.
  CodeBlob()
    : _type(compiler_none) {}

  // Returns the space needed for CodeBlob
  static unsigned int allocation_size(CodeBuffer* cb, int header_size);
  static unsigned int align_code_offset(int offset);

  // Deletion
  virtual void flush();

  // Typing
  virtual bool is_buffer_blob() const                 { return false; }
  virtual bool is_nmethod() const                     { return false; }
  virtual bool is_runtime_stub() const                { return false; }
  virtual bool is_deoptimization_stub() const         { return false; }
  virtual bool is_uncommon_trap_stub() const          { return false; }
  virtual bool is_exception_stub() const              { return false; }
  virtual bool is_safepoint_stub() const              { return false; }
  virtual bool is_adapter_blob() const                { return false; }
  virtual bool is_vtable_blob() const                 { return false; }
  virtual bool is_method_handles_adapter_blob() const { return false; }
  virtual bool is_compiled() const                    { return false; }
  virtual bool is_optimized_entry_blob() const                  { return false; }

  inline bool is_compiled_by_c1() const    { return _type == compiler_c1; };
  inline bool is_compiled_by_c2() const    { return _type == compiler_c2; };
  inline bool is_compiled_by_jvmci() const { return _type == compiler_jvmci; };
  const char* compiler_name() const;
  CompilerType compiler_type() const { return _type; }

  // Casting
  nmethod* as_nmethod_or_null()                { return is_nmethod() ? (nmethod*) this : NULL; }
  nmethod* as_nmethod()                        { assert(is_nmethod(), "must be nmethod"); return (nmethod*) this; }
  CompiledMethod* as_compiled_method_or_null() { return is_compiled() ? (CompiledMethod*) this : NULL; }
  CompiledMethod* as_compiled_method()         { assert(is_compiled(), "must be compiled"); return (CompiledMethod*) this; }
  CodeBlob* as_codeblob_or_null() const        { return (CodeBlob*) this; }
  OptimizedEntryBlob* as_optimized_entry_blob() const             { assert(is_optimized_entry_blob(), "must be entry blob"); return (OptimizedEntryBlob*) this; }

  // Boundaries
  address header_begin() const        { return (address) this; }
  relocInfo* relocation_begin() const { return (relocInfo*) _relocation_begin; };
  relocInfo* relocation_end() const   { return (relocInfo*) _relocation_end; }
  address content_begin() const       { return _content_begin; }
  address content_end() const         { return _code_end; } // _code_end == _content_end is true for all types of blobs for now, it is also checked in the constructor
  address code_begin() const          { return _code_begin;    }
  address code_end() const            { return _code_end; }
  address data_end() const            { return _data_end;      }

  // This field holds the beginning of the const section in the old code buffer.
  // It is needed to fix relocations of pc-relative loads when resizing the
  // the constant pool or moving it.
  S390_ONLY(address ctable_begin() const { return header_begin() + _ctable_offset; })
  void set_ctable_begin(address ctable) { S390_ONLY(_ctable_offset = ctable - header_begin();) }

  // Sizes
  int size() const                               { return _size; }
  int header_size() const                        { return _header_size; }
  int relocation_size() const                    { return (address) relocation_end() - (address) relocation_begin(); }
  int content_size() const                       { return           content_end()    -           content_begin();    }
  int code_size() const                          { return           code_end()       -           code_begin();       }
  // Only used from CodeCache::free_unused_tail() after the Interpreter blob was trimmed
  void adjust_size(size_t used) {
    _size = (int)used;
    _data_offset = (int)used;
    _code_end = (address)this + used;
    _data_end = (address)this + used;
  }

  // Containment
  bool blob_contains(address addr) const         { return header_begin()       <= addr && addr < data_end();       }
  bool code_contains(address addr) const         { return code_begin()         <= addr && addr < code_end();       }
  bool contains(address addr) const              { return content_begin()      <= addr && addr < content_end();    }
  bool is_frame_complete_at(address addr) const  { return _frame_complete_offset != CodeOffsets::frame_never_safe &&
                                                          code_contains(addr) && addr >= code_begin() + _frame_complete_offset; }
  int frame_complete_offset() const              { return _frame_complete_offset; }

  // CodeCache support: really only used by the nmethods, but in order to get
  // asserts and certain bookkeeping to work in the CodeCache they are defined
  // virtual here.
  virtual bool is_zombie() const                 { return false; }
  virtual bool is_locked_by_vm() const           { return false; }

  virtual bool is_unloaded() const               { return false; }
  virtual bool is_not_entrant() const            { return false; }

  // GC support
  virtual bool is_alive() const                  = 0;

  // OopMap for frame
  ImmutableOopMapSet* oop_maps() const           { return _oop_maps; }
  void set_oop_maps(OopMapSet* p);
  const ImmutableOopMap* oop_map_for_return_address(address return_address);
  virtual void preserve_callee_argument_oops(frame fr, const RegisterMap* reg_map, OopClosure* f) = 0;

  // Frame support. Sizes are in word units.
  int  frame_size() const                        { return _frame_size; }
  void set_frame_size(int size)                  { _frame_size = size; }

  // Returns true, if the next frame is responsible for GC'ing oops passed as arguments
  bool caller_must_gc_arguments(JavaThread* thread) const { return _caller_must_gc_arguments; }

  // Naming
  const char* name() const                       { return _name; }
  void set_name(const char* name)                { _name = name; }

  // Debugging
  virtual void verify() = 0;
  virtual void print() const;
  virtual void print_on(outputStream* st) const;
  virtual void print_value_on(outputStream* st) const;
  void dump_for_addr(address addr, outputStream* st, bool verbose) const;
  void print_code();

  // Print the comment associated with offset on stream, if there is one
  virtual void print_block_comment(outputStream* stream, address block_begin) const {
  #ifndef PRODUCT
    intptr_t offset = (intptr_t)(block_begin - code_begin());
    _strings.print_block_comment(stream, offset);
  #endif
  }

#ifndef PRODUCT
  void set_strings(CodeStrings& strings) {
    _strings.copy(strings);
  }
#endif
};

class CodeBlobLayout : public StackObj {
private:
  int _size;
  int _header_size;
  int _relocation_size;
  int _content_offset;
  int _code_offset;
  int _data_offset;
  address _code_begin;
  address _code_end;
  address _content_begin;
  address _content_end;
  address _data_end;
  address _relocation_begin;
  address _relocation_end;

public:
  CodeBlobLayout(address code_begin, address code_end, address content_begin, address content_end, address data_end, address relocation_begin, address relocation_end) :
    _size(0),
    _header_size(0),
    _relocation_size(0),
    _content_offset(0),
    _code_offset(0),
    _data_offset(0),
    _code_begin(code_begin),
    _code_end(code_end),
    _content_begin(content_begin),
    _content_end(content_end),
    _data_end(data_end),
    _relocation_begin(relocation_begin),
    _relocation_end(relocation_end)
  {
  }

  CodeBlobLayout(const address start, int size, int header_size, int relocation_size, int data_offset) :
    _size(size),
    _header_size(header_size),
    _relocation_size(relocation_size),
    _content_offset(CodeBlob::align_code_offset(_header_size + _relocation_size)),
    _code_offset(_content_offset),
    _data_offset(data_offset)
  {
    assert(is_aligned(_relocation_size, oopSize), "unaligned size");

    _code_begin = (address) start + _code_offset;
    _code_end = (address) start + _data_offset;

    _content_begin = (address) start + _content_offset;
    _content_end = (address) start + _data_offset;

    _data_end = (address) start + _size;
    _relocation_begin = (address) start + _header_size;
    _relocation_end = _relocation_begin + _relocation_size;
  }

  CodeBlobLayout(const address start, int size, int header_size, const CodeBuffer* cb) :
    _size(size),
    _header_size(header_size),
    _relocation_size(align_up(cb->total_relocation_size(), oopSize)),
    _content_offset(CodeBlob::align_code_offset(_header_size + _relocation_size)),
    _code_offset(_content_offset + cb->total_offset_of(cb->insts())),
    _data_offset(_content_offset + align_up(cb->total_content_size(), oopSize))
  {
    assert(is_aligned(_relocation_size, oopSize), "unaligned size");

    _code_begin = (address) start + _code_offset;
    _code_end = (address) start + _data_offset;

    _content_begin = (address) start + _content_offset;
    _content_end = (address) start + _data_offset;

    _data_end = (address) start + _size;
    _relocation_begin = (address) start + _header_size;
    _relocation_end = _relocation_begin + _relocation_size;
  }

  int size() const { return _size; }
  int header_size() const { return _header_size; }
  int relocation_size() const { return _relocation_size; }
  int content_offset() const { return _content_offset; }
  int code_offset() const { return _code_offset; }
  int data_offset() const { return _data_offset; }
  address code_begin() const { return _code_begin; }
  address code_end() const { return _code_end; }
  address data_end() const { return _data_end; }
  address relocation_begin() const { return _relocation_begin; }
  address relocation_end() const { return _relocation_end; }
  address content_begin() const { return _content_begin; }
  address content_end() const { return _content_end; }
};


class RuntimeBlob : public CodeBlob {
  friend class VMStructs;
 public:

  // Creation
  // a) simple CodeBlob
  // frame_complete is the offset from the beginning of the instructions
  // to where the frame setup (from stackwalk viewpoint) is complete.
  RuntimeBlob(const char* name, int header_size, int size, int frame_complete, int locs_size);

  // b) full CodeBlob
  RuntimeBlob(
    const char* name,
    CodeBuffer* cb,
    int         header_size,
    int         size,
    int         frame_complete,
    int         frame_size,
    OopMapSet*  oop_maps,
    bool        caller_must_gc_arguments = false
  );

  // GC support
  virtual bool is_alive() const                  = 0;

  void verify();

  // OopMap for frame
  virtual void preserve_callee_argument_oops(frame fr, const RegisterMap* reg_map, OopClosure* f)  { ShouldNotReachHere(); }

  // Debugging
  virtual void print_on(outputStream* st) const { CodeBlob::print_on(st); }
  virtual void print_value_on(outputStream* st) const { CodeBlob::print_value_on(st); }

  // Deal with Disassembler, VTune, Forte, JvmtiExport, MemoryService.
  static void trace_new_stub(RuntimeBlob* blob, const char* name1, const char* name2 = "");
};

class WhiteBox;
//----------------------------------------------------------------------------------------------------
// BufferBlob: used to hold non-relocatable machine code such as the interpreter, stubroutines, etc.

class BufferBlob: public RuntimeBlob {
  friend class VMStructs;
  friend class AdapterBlob;
  friend class VtableBlob;
  friend class MethodHandlesAdapterBlob;
  friend class OptimizedEntryBlob;
  friend class WhiteBox;

 private:
  // Creation support
  BufferBlob(const char* name, int size);
  BufferBlob(const char* name, int size, CodeBuffer* cb);

  // This ordinary operator delete is needed even though not used, so the
  // below two-argument operator delete will be treated as a placement
  // delete rather than an ordinary sized delete; see C++14 3.7.4.2/p2.
  void operator delete(void* p);
  void* operator new(size_t s, unsigned size) throw();

 public:
  // Creation
  static BufferBlob* create(const char* name, int buffer_size);
  static BufferBlob* create(const char* name, CodeBuffer* cb);

  static void free(BufferBlob* buf);

  // Typing
  virtual bool is_buffer_blob() const            { return true; }

  // GC/Verification support
  void preserve_callee_argument_oops(frame fr, const RegisterMap* reg_map, OopClosure* f)  { /* nothing to do */ }
  bool is_alive() const                          { return true; }

  void verify();
  void print_on(outputStream* st) const;
  void print_value_on(outputStream* st) const;
};


//----------------------------------------------------------------------------------------------------
// AdapterBlob: used to hold C2I/I2C adapters

class AdapterBlob: public BufferBlob {
private:
  AdapterBlob(int size, CodeBuffer* cb);

public:
  // Creation
  static AdapterBlob* create(CodeBuffer* cb);

  // Typing
  virtual bool is_adapter_blob() const { return true; }
};

//---------------------------------------------------------------------------------------------------
class VtableBlob: public BufferBlob {
private:
  VtableBlob(const char*, int);

  void* operator new(size_t s, unsigned size) throw();

public:
  // Creation
  static VtableBlob* create(const char* name, int buffer_size);

  // Typing
  virtual bool is_vtable_blob() const { return true; }
};

//----------------------------------------------------------------------------------------------------
// MethodHandlesAdapterBlob: used to hold MethodHandles adapters

class MethodHandlesAdapterBlob: public BufferBlob {
private:
  MethodHandlesAdapterBlob(int size)                 : BufferBlob("MethodHandles adapters", size) {}

public:
  // Creation
  static MethodHandlesAdapterBlob* create(int buffer_size);

  // Typing
  virtual bool is_method_handles_adapter_blob() const { return true; }
};


//----------------------------------------------------------------------------------------------------
// RuntimeStub: describes stubs used by compiled code to call a (static) C++ runtime routine

class RuntimeStub: public RuntimeBlob {
  friend class VMStructs;
 private:
  // Creation support
  RuntimeStub(
    const char* name,
    CodeBuffer* cb,
    int         size,
    int         frame_complete,
    int         frame_size,
    OopMapSet*  oop_maps,
    bool        caller_must_gc_arguments
  );

  // This ordinary operator delete is needed even though not used, so the
  // below two-argument operator delete will be treated as a placement
  // delete rather than an ordinary sized delete; see C++14 3.7.4.2/p2.
  void operator delete(void* p);
  void* operator new(size_t s, unsigned size) throw();

 public:
  // Creation
  static RuntimeStub* new_runtime_stub(
    const char* stub_name,
    CodeBuffer* cb,
    int         frame_complete,
    int         frame_size,
    OopMapSet*  oop_maps,
    bool        caller_must_gc_arguments
  );

  // Typing
  bool is_runtime_stub() const                   { return true; }

  address entry_point() const                    { return code_begin(); }

  // GC/Verification support
  void preserve_callee_argument_oops(frame fr, const RegisterMap *reg_map, OopClosure* f)  { /* nothing to do */ }
  bool is_alive() const                          { return true; }

  void verify();
  void print_on(outputStream* st) const;
  void print_value_on(outputStream* st) const;
};


//----------------------------------------------------------------------------------------------------
// Super-class for all blobs that exist in only one instance. Implements default behaviour.

class SingletonBlob: public RuntimeBlob {
  friend class VMStructs;

 protected:
  // This ordinary operator delete is needed even though not used, so the
  // below two-argument operator delete will be treated as a placement
  // delete rather than an ordinary sized delete; see C++14 3.7.4.2/p2.
  void operator delete(void* p);
  void* operator new(size_t s, unsigned size) throw();

 public:
   SingletonBlob(
     const char* name,
     CodeBuffer* cb,
     int         header_size,
     int         size,
     int         frame_size,
     OopMapSet*  oop_maps
   )
   : RuntimeBlob(name, cb, header_size, size, CodeOffsets::frame_never_safe, frame_size, oop_maps)
  {};

  address entry_point()                          { return code_begin(); }

  bool is_alive() const                          { return true; }

  // GC/Verification support
  void preserve_callee_argument_oops(frame fr, const RegisterMap *reg_map, OopClosure* f)  { /* nothing to do */ }
  void verify(); // does nothing
  void print_on(outputStream* st) const;
  void print_value_on(outputStream* st) const;
};


//----------------------------------------------------------------------------------------------------
// DeoptimizationBlob

class DeoptimizationBlob: public SingletonBlob {
  friend class VMStructs;
  friend class JVMCIVMStructs;
 private:
  int _unpack_offset;
  int _unpack_with_exception;
  int _unpack_with_reexecution;

  int _unpack_with_exception_in_tls;

#if INCLUDE_JVMCI
  // Offsets when JVMCI calls uncommon_trap.
  int _uncommon_trap_offset;
  int _implicit_exception_uncommon_trap_offset;
#endif

  // Creation support
  DeoptimizationBlob(
    CodeBuffer* cb,
    int         size,
    OopMapSet*  oop_maps,
    int         unpack_offset,
    int         unpack_with_exception_offset,
    int         unpack_with_reexecution_offset,
    int         frame_size
  );

 public:
  // Creation
  static DeoptimizationBlob* create(
    CodeBuffer* cb,
    OopMapSet*  oop_maps,
    int         unpack_offset,
    int         unpack_with_exception_offset,
    int         unpack_with_reexecution_offset,
    int         frame_size
  );

  // Typing
  bool is_deoptimization_stub() const { return true; }

  // GC for args
  void preserve_callee_argument_oops(frame fr, const RegisterMap *reg_map, OopClosure* f) { /* Nothing to do */ }

  // Printing
  void print_value_on(outputStream* st) const;

  address unpack() const                         { return code_begin() + _unpack_offset;           }
  address unpack_with_exception() const          { return code_begin() + _unpack_with_exception;   }
  address unpack_with_reexecution() const        { return code_begin() + _unpack_with_reexecution; }

  // Alternate entry point for C1 where the exception and issuing pc
  // are in JavaThread::_exception_oop and JavaThread::_exception_pc
  // instead of being in registers.  This is needed because C1 doesn't
  // model exception paths in a way that keeps these registers free so
  // there may be live values in those registers during deopt.
  void set_unpack_with_exception_in_tls_offset(int offset) {
    _unpack_with_exception_in_tls = offset;
    assert(code_contains(code_begin() + _unpack_with_exception_in_tls), "must be PC inside codeblob");
  }
  address unpack_with_exception_in_tls() const   { return code_begin() + _unpack_with_exception_in_tls; }

#if INCLUDE_JVMCI
  // Offsets when JVMCI calls uncommon_trap.
  void set_uncommon_trap_offset(int offset) {
    _uncommon_trap_offset = offset;
    assert(contains(code_begin() + _uncommon_trap_offset), "must be PC inside codeblob");
  }
  address uncommon_trap() const                  { return code_begin() + _uncommon_trap_offset; }

  void set_implicit_exception_uncommon_trap_offset(int offset) {
    _implicit_exception_uncommon_trap_offset = offset;
    assert(contains(code_begin() + _implicit_exception_uncommon_trap_offset), "must be PC inside codeblob");
  }
  address implicit_exception_uncommon_trap() const { return code_begin() + _implicit_exception_uncommon_trap_offset; }
#endif // INCLUDE_JVMCI
};


//----------------------------------------------------------------------------------------------------
// UncommonTrapBlob (currently only used by Compiler 2)

#ifdef COMPILER2

class UncommonTrapBlob: public SingletonBlob {
  friend class VMStructs;
 private:
  // Creation support
  UncommonTrapBlob(
    CodeBuffer* cb,
    int         size,
    OopMapSet*  oop_maps,
    int         frame_size
  );

 public:
  // Creation
  static UncommonTrapBlob* create(
    CodeBuffer* cb,
    OopMapSet*  oop_maps,
    int         frame_size
  );

  // GC for args
  void preserve_callee_argument_oops(frame fr, const RegisterMap *reg_map, OopClosure* f)  { /* nothing to do */ }

  // Typing
  bool is_uncommon_trap_stub() const             { return true; }
};


//----------------------------------------------------------------------------------------------------
// ExceptionBlob: used for exception unwinding in compiled code (currently only used by Compiler 2)

class ExceptionBlob: public SingletonBlob {
  friend class VMStructs;
 private:
  // Creation support
  ExceptionBlob(
    CodeBuffer* cb,
    int         size,
    OopMapSet*  oop_maps,
    int         frame_size
  );

 public:
  // Creation
  static ExceptionBlob* create(
    CodeBuffer* cb,
    OopMapSet*  oop_maps,
    int         frame_size
  );

  // GC for args
  void preserve_callee_argument_oops(frame fr, const RegisterMap* reg_map, OopClosure* f)  { /* nothing to do */ }

  // Typing
  bool is_exception_stub() const                 { return true; }
};
#endif // COMPILER2


//----------------------------------------------------------------------------------------------------
// SafepointBlob: handles illegal_instruction exceptions during a safepoint

class SafepointBlob: public SingletonBlob {
  friend class VMStructs;
 private:
  // Creation support
  SafepointBlob(
    CodeBuffer* cb,
    int         size,
    OopMapSet*  oop_maps,
    int         frame_size
  );

 public:
  // Creation
  static SafepointBlob* create(
    CodeBuffer* cb,
    OopMapSet*  oop_maps,
    int         frame_size
  );

  // GC for args
  void preserve_callee_argument_oops(frame fr, const RegisterMap* reg_map, OopClosure* f)  { /* nothing to do */ }

  // Typing
  bool is_safepoint_stub() const                 { return true; }
};

//----------------------------------------------------------------------------------------------------

class ProgrammableUpcallHandler;

class OptimizedEntryBlob: public BufferBlob {
  friend class ProgrammableUpcallHandler;
 private:
  intptr_t _exception_handler_offset;
  jobject _receiver;
  ByteSize _frame_data_offset;

  OptimizedEntryBlob(const char* name, int size, CodeBuffer* cb, intptr_t exception_handler_offset,
                     jobject receiver, ByteSize frame_data_offset);

  struct FrameData {
    JavaFrameAnchor jfa;
    JavaThread* thread;
    JNIHandleBlock* old_handles;
    JNIHandleBlock* new_handles;
    bool should_detach;
  };

  // defined in frame_ARCH.cpp
  FrameData* frame_data_for_frame(const frame& frame) const;
 public:
  // Creation
  static OptimizedEntryBlob* create(const char* name, CodeBuffer* cb,
                                    intptr_t exception_handler_offset, jobject receiver,
                                    ByteSize frame_data_offset);

  address exception_handler() { return code_begin() + _exception_handler_offset; }
  jobject receiver() { return _receiver; }

  JavaFrameAnchor* jfa_for_frame(const frame& frame) const;

  void oops_do(OopClosure* f, const frame& frame);

  // Typing
  virtual bool is_optimized_entry_blob() const override { return true; }
};

#endif // SHARE_CODE_CODEBLOB_HPP
