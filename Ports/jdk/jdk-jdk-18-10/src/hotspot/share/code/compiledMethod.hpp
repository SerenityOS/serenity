/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CODE_COMPILEDMETHOD_HPP
#define SHARE_CODE_COMPILEDMETHOD_HPP

#include "code/codeBlob.hpp"
#include "code/pcDesc.hpp"
#include "oops/metadata.hpp"
#include "oops/method.hpp"

class Dependencies;
class ExceptionHandlerTable;
class ImplicitExceptionTable;
class AbstractCompiler;
class xmlStream;
class CompiledStaticCall;
class NativeCallWrapper;
class ScopeDesc;
class CompiledIC;
class MetadataClosure;

// This class is used internally by nmethods, to cache
// exception/pc/handler information.

class ExceptionCache : public CHeapObj<mtCode> {
  friend class VMStructs;
 private:
  enum { cache_size = 16 };
  Klass*   _exception_type;
  address  _pc[cache_size];
  address  _handler[cache_size];
  volatile int _count;
  ExceptionCache* volatile _next;
  ExceptionCache* _purge_list_next;

  inline address pc_at(int index);
  void set_pc_at(int index, address a)      { assert(index >= 0 && index < cache_size,""); _pc[index] = a; }

  inline address handler_at(int index);
  void set_handler_at(int index, address a) { assert(index >= 0 && index < cache_size,""); _handler[index] = a; }

  inline int count();
  // increment_count is only called under lock, but there may be concurrent readers.
  void increment_count();

 public:

  ExceptionCache(Handle exception, address pc, address handler);

  Klass*    exception_type()                { return _exception_type; }
  ExceptionCache* next();
  void      set_next(ExceptionCache *ec);
  ExceptionCache* purge_list_next()                 { return _purge_list_next; }
  void      set_purge_list_next(ExceptionCache *ec) { _purge_list_next = ec; }

  address match(Handle exception, address pc);
  bool    match_exception_with_space(Handle exception) ;
  address test_address(address addr);
  bool    add_address_and_handler(address addr, address handler) ;
};

class nmethod;

// cache pc descs found in earlier inquiries
class PcDescCache {
  friend class VMStructs;
 private:
  enum { cache_size = 4 };
  // The array elements MUST be volatile! Several threads may modify
  // and read from the cache concurrently. find_pc_desc_internal has
  // returned wrong results. C++ compiler (namely xlC12) may duplicate
  // C++ field accesses if the elements are not volatile.
  typedef PcDesc* PcDescPtr;
  volatile PcDescPtr _pc_descs[cache_size]; // last cache_size pc_descs found
 public:
  PcDescCache() { debug_only(_pc_descs[0] = NULL); }
  void    reset_to(PcDesc* initial_pc_desc);
  PcDesc* find_pc_desc(int pc_offset, bool approximate);
  void    add_pc_desc(PcDesc* pc_desc);
  PcDesc* last_pc_desc() { return _pc_descs[0]; }
};

class PcDescSearch {
private:
  address _code_begin;
  PcDesc* _lower;
  PcDesc* _upper;
public:
  PcDescSearch(address code, PcDesc* lower, PcDesc* upper) :
    _code_begin(code), _lower(lower), _upper(upper)
  {
  }

  address code_begin() const { return _code_begin; }
  PcDesc* scopes_pcs_begin() const { return _lower; }
  PcDesc* scopes_pcs_end() const { return _upper; }
};

class PcDescContainer {
private:
  PcDescCache _pc_desc_cache;
public:
  PcDescContainer() {}

  PcDesc* find_pc_desc_internal(address pc, bool approximate, const PcDescSearch& search);
  void    reset_to(PcDesc* initial_pc_desc) { _pc_desc_cache.reset_to(initial_pc_desc); }

  PcDesc* find_pc_desc(address pc, bool approximate, const PcDescSearch& search) {
    address base_address = search.code_begin();
    PcDesc* desc = _pc_desc_cache.last_pc_desc();
    if (desc != NULL && desc->pc_offset() == pc - base_address) {
      return desc;
    }
    return find_pc_desc_internal(pc, approximate, search);
  }
};


class CompiledMethod : public CodeBlob {
  friend class VMStructs;
  friend class NMethodSweeper;

  void init_defaults();
protected:
  enum MarkForDeoptimizationStatus {
    not_marked,
    deoptimize,
    deoptimize_noupdate
  };

  MarkForDeoptimizationStatus _mark_for_deoptimization_status; // Used for stack deoptimization

  // set during construction
  unsigned int _has_unsafe_access:1;         // May fault due to unsafe access.
  unsigned int _has_method_handle_invokes:1; // Has this method MethodHandle invokes?
  unsigned int _has_wide_vectors:1;          // Preserve wide vectors at safepoints

  Method*   _method;
  address _scopes_data_begin;
  // All deoptee's will resume execution at this location described by
  // this address.
  address _deopt_handler_begin;
  // All deoptee's at a MethodHandle call site will resume execution
  // at this location described by this offset.
  address _deopt_mh_handler_begin;

  PcDescContainer _pc_desc_container;
  ExceptionCache * volatile _exception_cache;

  void* _gc_data;

  virtual void flush() = 0;
protected:
  CompiledMethod(Method* method, const char* name, CompilerType type, const CodeBlobLayout& layout, int frame_complete_offset, int frame_size, ImmutableOopMapSet* oop_maps, bool caller_must_gc_arguments);
  CompiledMethod(Method* method, const char* name, CompilerType type, int size, int header_size, CodeBuffer* cb, int frame_complete_offset, int frame_size, OopMapSet* oop_maps, bool caller_must_gc_arguments);

public:
  // Only used by unit test.
  CompiledMethod() {}

  virtual bool is_compiled() const                { return true; }

  template<typename T>
  T* gc_data() const                              { return reinterpret_cast<T*>(_gc_data); }
  template<typename T>
  void set_gc_data(T* gc_data)                    { _gc_data = reinterpret_cast<void*>(gc_data); }

  bool  has_unsafe_access() const                 { return _has_unsafe_access; }
  void  set_has_unsafe_access(bool z)             { _has_unsafe_access = z; }

  bool  has_method_handle_invokes() const         { return _has_method_handle_invokes; }
  void  set_has_method_handle_invokes(bool z)     { _has_method_handle_invokes = z; }

  bool  has_wide_vectors() const                  { return _has_wide_vectors; }
  void  set_has_wide_vectors(bool z)              { _has_wide_vectors = z; }

  enum { not_installed = -1, // in construction, only the owner doing the construction is
                             // allowed to advance state
         in_use        = 0,  // executable nmethod
         not_used      = 1,  // not entrant, but revivable
         not_entrant   = 2,  // marked for deoptimization but activations may still exist,
                             // will be transformed to zombie when all activations are gone
         unloaded      = 3,  // there should be no activations, should not be called, will be
                             // transformed to zombie by the sweeper, when not "locked in vm".
         zombie        = 4   // no activations exist, nmethod is ready for purge
  };

  virtual bool  is_in_use() const = 0;
  virtual int   comp_level() const = 0;
  virtual int   compile_id() const = 0;

  virtual address verified_entry_point() const = 0;
  virtual void log_identity(xmlStream* log) const = 0;
  virtual void log_state_change() const = 0;
  virtual bool make_not_used() = 0;
  virtual bool make_not_entrant() = 0;
  virtual bool make_entrant() = 0;
  virtual address entry_point() const = 0;
  virtual bool make_zombie() = 0;
  virtual bool is_osr_method() const = 0;
  virtual int osr_entry_bci() const = 0;
  Method* method() const                          { return _method; }
  virtual void print_pcs() = 0;
  bool is_native_method() const { return _method != NULL && _method->is_native(); }
  bool is_java_method() const { return _method != NULL && !_method->is_native(); }

  // ScopeDesc retrieval operation
  PcDesc* pc_desc_at(address pc)   { return find_pc_desc(pc, false); }
  // pc_desc_near returns the first PcDesc at or after the given pc.
  PcDesc* pc_desc_near(address pc) { return find_pc_desc(pc, true); }

  // ScopeDesc for an instruction
  ScopeDesc* scope_desc_at(address pc);
  ScopeDesc* scope_desc_near(address pc);

  bool is_at_poll_return(address pc);
  bool is_at_poll_or_poll_return(address pc);

  bool  is_marked_for_deoptimization() const { return _mark_for_deoptimization_status != not_marked; }
  void  mark_for_deoptimization(bool inc_recompile_counts = true);

  bool update_recompile_counts() const {
    // Update recompile counts when either the update is explicitly requested (deoptimize)
    // or the nmethod is not marked for deoptimization at all (not_marked).
    // The latter happens during uncommon traps when deoptimized nmethod is made not entrant.
    return _mark_for_deoptimization_status != deoptimize_noupdate;
  }

  // tells whether frames described by this nmethod can be deoptimized
  // note: native wrappers cannot be deoptimized.
  bool can_be_deoptimized() const { return is_java_method(); }

  virtual oop oop_at(int index) const = 0;
  virtual Metadata* metadata_at(int index) const = 0;

  address scopes_data_begin() const { return _scopes_data_begin; }
  virtual address scopes_data_end() const = 0;
  int scopes_data_size() const { return scopes_data_end() - scopes_data_begin(); }

  virtual PcDesc* scopes_pcs_begin() const = 0;
  virtual PcDesc* scopes_pcs_end() const = 0;
  int scopes_pcs_size() const { return (intptr_t) scopes_pcs_end() - (intptr_t) scopes_pcs_begin(); }

  address insts_begin() const { return code_begin(); }
  address insts_end() const { return stub_begin(); }
  // Returns true if a given address is in the 'insts' section. The method
  // insts_contains_inclusive() is end-inclusive.
  bool insts_contains(address addr) const { return insts_begin() <= addr && addr < insts_end(); }
  bool insts_contains_inclusive(address addr) const { return insts_begin() <= addr && addr <= insts_end(); }

  int insts_size() const { return insts_end() - insts_begin(); }

  virtual address consts_begin() const = 0;
  virtual address consts_end() const = 0;
  bool consts_contains(address addr) const { return consts_begin() <= addr && addr < consts_end(); }
  int consts_size() const { return consts_end() - consts_begin(); }

  virtual address stub_begin() const = 0;
  virtual address stub_end() const = 0;
  bool stub_contains(address addr) const { return stub_begin() <= addr && addr < stub_end(); }
  int stub_size() const { return stub_end() - stub_begin(); }

  virtual address handler_table_begin() const = 0;
  virtual address handler_table_end() const = 0;
  bool handler_table_contains(address addr) const { return handler_table_begin() <= addr && addr < handler_table_end(); }
  int handler_table_size() const { return handler_table_end() - handler_table_begin(); }

  virtual address exception_begin() const = 0;

  virtual address nul_chk_table_begin() const = 0;
  virtual address nul_chk_table_end() const = 0;
  bool nul_chk_table_contains(address addr) const { return nul_chk_table_begin() <= addr && addr < nul_chk_table_end(); }
  int nul_chk_table_size() const { return nul_chk_table_end() - nul_chk_table_begin(); }

  virtual oop* oop_addr_at(int index) const = 0;
  virtual Metadata** metadata_addr_at(int index) const = 0;
  virtual void    set_original_pc(const frame* fr, address pc) = 0;

protected:
  // Exception cache support
  // Note: _exception_cache may be read and cleaned concurrently.
  ExceptionCache* exception_cache() const         { return _exception_cache; }
  ExceptionCache* exception_cache_acquire() const;
  void set_exception_cache(ExceptionCache *ec)    { _exception_cache = ec; }

public:
  address handler_for_exception_and_pc(Handle exception, address pc);
  void add_handler_for_exception_and_pc(Handle exception, address pc, address handler);
  void clean_exception_cache();

  void add_exception_cache_entry(ExceptionCache* new_entry);
  ExceptionCache* exception_cache_entry_for_exception(Handle exception);

  // MethodHandle
  bool is_method_handle_return(address return_pc);
  address deopt_mh_handler_begin() const  { return _deopt_mh_handler_begin; }

  address deopt_handler_begin() const { return _deopt_handler_begin; }
  virtual address get_original_pc(const frame* fr) = 0;
  // Deopt
  // Return true is the PC is one would expect if the frame is being deopted.
  inline bool is_deopt_pc(address pc);
  inline bool is_deopt_mh_entry(address pc);
  inline bool is_deopt_entry(address pc);

  virtual bool can_convert_to_zombie() = 0;
  virtual const char* compile_kind() const = 0;
  virtual int get_state() const = 0;

  const char* state() const;

  bool inlinecache_check_contains(address addr) const {
    return (addr >= code_begin() && addr < verified_entry_point());
  }

  void preserve_callee_argument_oops(frame fr, const RegisterMap *reg_map, OopClosure* f);

  // implicit exceptions support
  address continuation_for_implicit_div0_exception(address pc) { return continuation_for_implicit_exception(pc, true); }
  address continuation_for_implicit_null_exception(address pc) { return continuation_for_implicit_exception(pc, false); }

  static address get_deopt_original_pc(const frame* fr);

  // Inline cache support for class unloading and nmethod unloading
 private:
  bool cleanup_inline_caches_impl(bool unloading_occurred, bool clean_all);

  address continuation_for_implicit_exception(address pc, bool for_div0_check);

 public:
  // Serial version used by sweeper and whitebox test
  void cleanup_inline_caches(bool clean_all);

  virtual void clear_inline_caches();
  void clear_ic_callsites();

  // Execute nmethod barrier code, as if entering through nmethod call.
  void run_nmethod_entry_barrier();

  // Verify and count cached icholder relocations.
  int  verify_icholder_relocations();
  void verify_oop_relocations();

  bool has_evol_metadata();

  // Fast breakpoint support. Tells if this compiled method is
  // dependent on the given method. Returns true if this nmethod
  // corresponds to the given method as well.
  virtual bool is_dependent_on_method(Method* dependee) = 0;

  virtual NativeCallWrapper* call_wrapper_at(address call) const = 0;
  virtual NativeCallWrapper* call_wrapper_before(address return_pc) const = 0;
  virtual address call_instruction_address(address pc) const = 0;

  virtual CompiledStaticCall* compiledStaticCall_at(Relocation* call_site) const = 0;
  virtual CompiledStaticCall* compiledStaticCall_at(address addr) const = 0;
  virtual CompiledStaticCall* compiledStaticCall_before(address addr) const = 0;

  Method* attached_method(address call_pc);
  Method* attached_method_before_pc(address pc);

  virtual void metadata_do(MetadataClosure* f) = 0;

  // GC support
 protected:
  address oops_reloc_begin() const;

 private:
  bool static clean_ic_if_metadata_is_dead(CompiledIC *ic);

 public:
  // GC unloading support
  // Cleans unloaded klasses and unloaded nmethods in inline caches

  virtual bool is_unloading() = 0;

  bool unload_nmethod_caches(bool class_unloading_occurred);
  virtual void do_unloading(bool unloading_occurred) = 0;

private:
  PcDesc* find_pc_desc(address pc, bool approximate) {
    return _pc_desc_container.find_pc_desc(pc, approximate, PcDescSearch(code_begin(), scopes_pcs_begin(), scopes_pcs_end()));
  }
};

#endif // SHARE_CODE_COMPILEDMETHOD_HPP
