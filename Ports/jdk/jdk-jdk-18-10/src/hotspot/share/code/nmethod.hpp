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

#ifndef SHARE_CODE_NMETHOD_HPP
#define SHARE_CODE_NMETHOD_HPP

#include "code/compiledMethod.hpp"

class DepChange;
class DirectiveSet;
class DebugInformationRecorder;
class JvmtiThreadState;

// nmethods (native methods) are the compiled code versions of Java methods.
//
// An nmethod contains:
//  - header                 (the nmethod structure)
//  [Relocation]
//  - relocation information
//  - constant part          (doubles, longs and floats used in nmethod)
//  - oop table
//  [Code]
//  - code body
//  - exception handler
//  - stub code
//  [Debugging information]
//  - oop array
//  - data array
//  - pcs
//  [Exception handler table]
//  - handler entry point array
//  [Implicit Null Pointer exception table]
//  - implicit null table array
//  [Speculations]
//  - encoded speculations array
//  [JVMCINMethodData]
//  - meta data for JVMCI compiled nmethod

#if INCLUDE_JVMCI
class FailedSpeculation;
class JVMCINMethodData;
#endif

class nmethod : public CompiledMethod {
  friend class VMStructs;
  friend class JVMCIVMStructs;
  friend class NMethodSweeper;
  friend class CodeCache;  // scavengable oops
  friend class JVMCINMethodData;

 private:
  // Shared fields for all nmethod's
  int       _entry_bci;        // != InvocationEntryBci if this nmethod is an on-stack replacement method

  // To support simple linked-list chaining of nmethods:
  nmethod*  _osr_link;         // from InstanceKlass::osr_nmethods_head

  // STW two-phase nmethod root processing helpers.
  //
  // When determining liveness of a given nmethod to do code cache unloading,
  // some collectors need to do different things depending on whether the nmethods
  // need to absolutely be kept alive during root processing; "strong"ly reachable
  // nmethods are known to be kept alive at root processing, but the liveness of
  // "weak"ly reachable ones is to be determined later.
  //
  // We want to allow strong and weak processing of nmethods by different threads
  // at the same time without heavy synchronization. Additional constraints are
  // to make sure that every nmethod is processed a minimal amount of time, and
  // nmethods themselves are always iterated at most once at a particular time.
  //
  // Note that strong processing work must be a superset of weak processing work
  // for this code to work.
  //
  // We store state and claim information in the _oops_do_mark_link member, using
  // the two LSBs for the state and the remaining upper bits for linking together
  // nmethods that were already visited.
  // The last element is self-looped, i.e. points to itself to avoid some special
  // "end-of-list" sentinel value.
  //
  // _oops_do_mark_link special values:
  //
  //   _oops_do_mark_link == NULL: the nmethod has not been visited at all yet, i.e.
  //      is Unclaimed.
  //
  // For other values, its lowest two bits indicate the following states of the nmethod:
  //
  //   weak_request (WR): the nmethod has been claimed by a thread for weak processing
  //   weak_done (WD): weak processing has been completed for this nmethod.
  //   strong_request (SR): the nmethod has been found to need strong processing while
  //       being weak processed.
  //   strong_done (SD): strong processing has been completed for this nmethod .
  //
  // The following shows the _only_ possible progressions of the _oops_do_mark_link
  // pointer.
  //
  // Given
  //   N as the nmethod
  //   X the current next value of _oops_do_mark_link
  //
  // Unclaimed (C)-> N|WR (C)-> X|WD: the nmethod has been processed weakly by
  //   a single thread.
  // Unclaimed (C)-> N|WR (C)-> X|WD (O)-> X|SD: after weak processing has been
  //   completed (as above) another thread found that the nmethod needs strong
  //   processing after all.
  // Unclaimed (C)-> N|WR (O)-> N|SR (C)-> X|SD: during weak processing another
  //   thread finds that the nmethod needs strong processing, marks it as such and
  //   terminates. The original thread completes strong processing.
  // Unclaimed (C)-> N|SD (C)-> X|SD: the nmethod has been processed strongly from
  //   the beginning by a single thread.
  //
  // "|" describes the concatentation of bits in _oops_do_mark_link.
  //
  // The diagram also describes the threads responsible for changing the nmethod to
  // the next state by marking the _transition_ with (C) and (O), which mean "current"
  // and "other" thread respectively.
  //
  struct oops_do_mark_link; // Opaque data type.

  // States used for claiming nmethods during root processing.
  static const uint claim_weak_request_tag = 0;
  static const uint claim_weak_done_tag = 1;
  static const uint claim_strong_request_tag = 2;
  static const uint claim_strong_done_tag = 3;

  static oops_do_mark_link* mark_link(nmethod* nm, uint tag) {
    assert(tag <= claim_strong_done_tag, "invalid tag %u", tag);
    assert(is_aligned(nm, 4), "nmethod pointer must have zero lower two LSB");
    return (oops_do_mark_link*)(((uintptr_t)nm & ~0x3) | tag);
  }

  static uint extract_state(oops_do_mark_link* link) {
    return (uint)((uintptr_t)link & 0x3);
  }

  static nmethod* extract_nmethod(oops_do_mark_link* link) {
    return (nmethod*)((uintptr_t)link & ~0x3);
  }

  void oops_do_log_change(const char* state);

  static bool oops_do_has_weak_request(oops_do_mark_link* next) {
    return extract_state(next) == claim_weak_request_tag;
  }

  static bool oops_do_has_any_strong_state(oops_do_mark_link* next) {
    return extract_state(next) >= claim_strong_request_tag;
  }

  // Attempt Unclaimed -> N|WR transition. Returns true if successful.
  bool oops_do_try_claim_weak_request();

  // Attempt Unclaimed -> N|SD transition. Returns the current link.
  oops_do_mark_link* oops_do_try_claim_strong_done();
  // Attempt N|WR -> X|WD transition. Returns NULL if successful, X otherwise.
  nmethod* oops_do_try_add_to_list_as_weak_done();

  // Attempt X|WD -> N|SR transition. Returns the current link.
  oops_do_mark_link* oops_do_try_add_strong_request(oops_do_mark_link* next);
  // Attempt X|WD -> X|SD transition. Returns true if successful.
  bool oops_do_try_claim_weak_done_as_strong_done(oops_do_mark_link* next);

  // Do the N|SD -> X|SD transition.
  void oops_do_add_to_list_as_strong_done();

  // Sets this nmethod as strongly claimed (as part of N|SD -> X|SD and N|SR -> X|SD
  // transitions).
  void oops_do_set_strong_done(nmethod* old_head);

  static nmethod* volatile _oops_do_mark_nmethods;
  oops_do_mark_link* volatile _oops_do_mark_link;

  // offsets for entry points
  address _entry_point;                      // entry point with class check
  address _verified_entry_point;             // entry point without class check
  address _osr_entry_point;                  // entry point for on stack replacement

  // Offsets for different nmethod parts
  int  _exception_offset;
  // Offset of the unwind handler if it exists
  int _unwind_handler_offset;

  int _consts_offset;
  int _stub_offset;
  int _oops_offset;                       // offset to where embedded oop table begins (inside data)
  int _metadata_offset;                   // embedded meta data table
  int _scopes_data_offset;
  int _scopes_pcs_offset;
  int _dependencies_offset;
  int _native_invokers_offset;
  int _handler_table_offset;
  int _nul_chk_table_offset;
#if INCLUDE_JVMCI
  int _speculations_offset;
  int _jvmci_data_offset;
#endif
  int _nmethod_end_offset;

  int code_offset() const { return (address) code_begin() - header_begin(); }

  // location in frame (offset for sp) that deopt can store the original
  // pc during a deopt.
  int _orig_pc_offset;

  int _compile_id;                           // which compilation made this nmethod
  int _comp_level;                           // compilation level

  // protected by CodeCache_lock
  bool _has_flushed_dependencies;            // Used for maintenance of dependencies (CodeCache_lock)

  // used by jvmti to track if an event has been posted for this nmethod.
  bool _unload_reported;
  bool _load_reported;

  // Protected by CompiledMethod_lock
  volatile signed char _state;               // {not_installed, in_use, not_entrant, zombie, unloaded}

#ifdef ASSERT
  bool _oops_are_stale;  // indicates that it's no longer safe to access oops section
#endif

#if INCLUDE_RTM_OPT
  // RTM state at compile time. Used during deoptimization to decide
  // whether to restart collecting RTM locking abort statistic again.
  RTMState _rtm_state;
#endif

  // Nmethod Flushing lock. If non-zero, then the nmethod is not removed
  // and is not made into a zombie. However, once the nmethod is made into
  // a zombie, it will be locked one final time if CompiledMethodUnload
  // event processing needs to be done.
  volatile jint _lock_count;

  // not_entrant method removal. Each mark_sweep pass will update
  // this mark to current sweep invocation count if it is seen on the
  // stack.  An not_entrant method can be removed when there are no
  // more activations, i.e., when the _stack_traversal_mark is less than
  // current sweep traversal index.
  volatile long _stack_traversal_mark;

  // The _hotness_counter indicates the hotness of a method. The higher
  // the value the hotter the method. The hotness counter of a nmethod is
  // set to [(ReservedCodeCacheSize / (1024 * 1024)) * 2] each time the method
  // is active while stack scanning (do_stack_scanning()). The hotness
  // counter is decreased (by 1) while sweeping.
  int _hotness_counter;

  // Local state used to keep track of whether unloading is happening or not
  volatile uint8_t _is_unloading_state;

  // These are used for compiled synchronized native methods to
  // locate the owner and stack slot for the BasicLock. They are
  // needed because there is no debug information for compiled native
  // wrappers and the oop maps are insufficient to allow
  // frame::retrieve_receiver() to work. Currently they are expected
  // to be byte offsets from the Java stack pointer for maximum code
  // sharing between platforms. JVMTI's GetLocalInstance() uses these
  // offsets to find the receiver for non-static native wrapper frames.
  ByteSize _native_receiver_sp_offset;
  ByteSize _native_basic_lock_sp_offset;

  friend class nmethodLocker;

  // For native wrappers
  nmethod(Method* method,
          CompilerType type,
          int nmethod_size,
          int compile_id,
          CodeOffsets* offsets,
          CodeBuffer *code_buffer,
          int frame_size,
          ByteSize basic_lock_owner_sp_offset, /* synchronized natives only */
          ByteSize basic_lock_sp_offset,       /* synchronized natives only */
          OopMapSet* oop_maps);

  // Creation support
  nmethod(Method* method,
          CompilerType type,
          int nmethod_size,
          int compile_id,
          int entry_bci,
          CodeOffsets* offsets,
          int orig_pc_offset,
          DebugInformationRecorder *recorder,
          Dependencies* dependencies,
          CodeBuffer *code_buffer,
          int frame_size,
          OopMapSet* oop_maps,
          ExceptionHandlerTable* handler_table,
          ImplicitExceptionTable* nul_chk_table,
          AbstractCompiler* compiler,
          int comp_level,
          const GrowableArrayView<RuntimeStub*>& native_invokers
#if INCLUDE_JVMCI
          , char* speculations,
          int speculations_len,
          int jvmci_data_size
#endif
          );

  // helper methods
  void* operator new(size_t size, int nmethod_size, int comp_level) throw();

  const char* reloc_string_for(u_char* begin, u_char* end);

  bool try_transition(int new_state);

  // Returns true if this thread changed the state of the nmethod or
  // false if another thread performed the transition.
  bool make_not_entrant_or_zombie(int state);
  bool make_entrant() { Unimplemented(); return false; }
  void inc_decompile_count();

  // Inform external interfaces that a compiled method has been unloaded
  void post_compiled_method_unload();

  // Initailize fields to their default values
  void init_defaults();

  // Offsets
  int content_offset() const                  { return content_begin() - header_begin(); }
  int data_offset() const                     { return _data_offset; }

  address header_end() const                  { return (address)    header_begin() + header_size(); }

 public:
  // create nmethod with entry_bci
  static nmethod* new_nmethod(const methodHandle& method,
                              int compile_id,
                              int entry_bci,
                              CodeOffsets* offsets,
                              int orig_pc_offset,
                              DebugInformationRecorder* recorder,
                              Dependencies* dependencies,
                              CodeBuffer *code_buffer,
                              int frame_size,
                              OopMapSet* oop_maps,
                              ExceptionHandlerTable* handler_table,
                              ImplicitExceptionTable* nul_chk_table,
                              AbstractCompiler* compiler,
                              int comp_level,
                              const GrowableArrayView<RuntimeStub*>& native_invokers = GrowableArrayView<RuntimeStub*>::EMPTY
#if INCLUDE_JVMCI
                              , char* speculations = NULL,
                              int speculations_len = 0,
                              int nmethod_mirror_index = -1,
                              const char* nmethod_mirror_name = NULL,
                              FailedSpeculation** failed_speculations = NULL
#endif
  );

  // Only used for unit tests.
  nmethod()
    : CompiledMethod(),
      _is_unloading_state(0),
      _native_receiver_sp_offset(in_ByteSize(-1)),
      _native_basic_lock_sp_offset(in_ByteSize(-1)) {}


  static nmethod* new_native_nmethod(const methodHandle& method,
                                     int compile_id,
                                     CodeBuffer *code_buffer,
                                     int vep_offset,
                                     int frame_complete,
                                     int frame_size,
                                     ByteSize receiver_sp_offset,
                                     ByteSize basic_lock_sp_offset,
                                     OopMapSet* oop_maps);

  // type info
  bool is_nmethod() const                         { return true; }
  bool is_osr_method() const                      { return _entry_bci != InvocationEntryBci; }

  // boundaries for different parts
  address consts_begin          () const          { return           header_begin() + _consts_offset        ; }
  address consts_end            () const          { return           code_begin()                           ; }
  address stub_begin            () const          { return           header_begin() + _stub_offset          ; }
  address stub_end              () const          { return           header_begin() + _oops_offset          ; }
  address exception_begin       () const          { return           header_begin() + _exception_offset     ; }
  address unwind_handler_begin  () const          { return _unwind_handler_offset != -1 ? (header_begin() + _unwind_handler_offset) : NULL; }
  oop*    oops_begin            () const          { return (oop*)   (header_begin() + _oops_offset)         ; }
  oop*    oops_end              () const          { return (oop*)   (header_begin() + _metadata_offset)     ; }

  Metadata** metadata_begin   () const            { return (Metadata**)  (header_begin() + _metadata_offset)     ; }
  Metadata** metadata_end     () const            { return (Metadata**)  _scopes_data_begin; }

  address scopes_data_end       () const          { return           header_begin() + _scopes_pcs_offset    ; }
  PcDesc* scopes_pcs_begin      () const          { return (PcDesc*)(header_begin() + _scopes_pcs_offset   ); }
  PcDesc* scopes_pcs_end        () const          { return (PcDesc*)(header_begin() + _dependencies_offset) ; }
  address dependencies_begin    () const          { return           header_begin() + _dependencies_offset  ; }
  address dependencies_end      () const          { return           header_begin() + _native_invokers_offset ; }
  RuntimeStub** native_invokers_begin() const     { return (RuntimeStub**)(header_begin() + _native_invokers_offset) ; }
  RuntimeStub** native_invokers_end  () const     { return (RuntimeStub**)(header_begin() + _handler_table_offset); }
  address handler_table_begin   () const          { return           header_begin() + _handler_table_offset ; }
  address handler_table_end     () const          { return           header_begin() + _nul_chk_table_offset ; }
  address nul_chk_table_begin   () const          { return           header_begin() + _nul_chk_table_offset ; }
#if INCLUDE_JVMCI
  address nul_chk_table_end     () const          { return           header_begin() + _speculations_offset  ; }
  address speculations_begin    () const          { return           header_begin() + _speculations_offset  ; }
  address speculations_end      () const          { return           header_begin() + _jvmci_data_offset   ; }
  address jvmci_data_begin      () const          { return           header_begin() + _jvmci_data_offset    ; }
  address jvmci_data_end        () const          { return           header_begin() + _nmethod_end_offset   ; }
#else
  address nul_chk_table_end     () const          { return           header_begin() + _nmethod_end_offset   ; }
#endif

  // Sizes
  int oops_size         () const                  { return (address)  oops_end         () - (address)  oops_begin         (); }
  int metadata_size     () const                  { return (address)  metadata_end     () - (address)  metadata_begin     (); }
  int dependencies_size () const                  { return            dependencies_end () -            dependencies_begin (); }
#if INCLUDE_JVMCI
  int speculations_size () const                  { return            speculations_end () -            speculations_begin (); }
  int jvmci_data_size   () const                  { return            jvmci_data_end   () -            jvmci_data_begin   (); }
#endif

  int     oops_count() const { assert(oops_size() % oopSize == 0, "");  return (oops_size() / oopSize) + 1; }
  int metadata_count() const { assert(metadata_size() % wordSize == 0, ""); return (metadata_size() / wordSize) + 1; }

  int total_size        () const;

  void dec_hotness_counter()        { _hotness_counter--; }
  void set_hotness_counter(int val) { _hotness_counter = val; }
  int  hotness_counter() const      { return _hotness_counter; }

  // Containment
  bool oops_contains         (oop*    addr) const { return oops_begin         () <= addr && addr < oops_end         (); }
  bool metadata_contains     (Metadata** addr) const   { return metadata_begin     () <= addr && addr < metadata_end     (); }
  bool scopes_data_contains  (address addr) const { return scopes_data_begin  () <= addr && addr < scopes_data_end  (); }
  bool scopes_pcs_contains   (PcDesc* addr) const { return scopes_pcs_begin   () <= addr && addr < scopes_pcs_end   (); }

  // entry points
  address entry_point() const                     { return _entry_point;             } // normal entry point
  address verified_entry_point() const            { return _verified_entry_point;    } // if klass is correct

  // flag accessing and manipulation
  bool  is_not_installed() const                  { return _state == not_installed; }
  bool  is_in_use() const                         { return _state <= in_use; }
  bool  is_alive() const                          { return _state < unloaded; }
  bool  is_not_entrant() const                    { return _state == not_entrant; }
  bool  is_zombie() const                         { return _state == zombie; }
  bool  is_unloaded() const                       { return _state == unloaded; }

  void clear_unloading_state();
  virtual bool is_unloading();
  virtual void do_unloading(bool unloading_occurred);

#if INCLUDE_RTM_OPT
  // rtm state accessing and manipulating
  RTMState  rtm_state() const                     { return _rtm_state; }
  void set_rtm_state(RTMState state)              { _rtm_state = state; }
#endif

  bool make_in_use() {
    return try_transition(in_use);
  }
  // Make the nmethod non entrant. The nmethod will continue to be
  // alive.  It is used when an uncommon trap happens.  Returns true
  // if this thread changed the state of the nmethod or false if
  // another thread performed the transition.
  bool  make_not_entrant() {
    assert(!method()->is_method_handle_intrinsic(), "Cannot make MH intrinsic not entrant");
    return make_not_entrant_or_zombie(not_entrant);
  }
  bool  make_not_used()    { return make_not_entrant(); }
  bool  make_zombie()      { return make_not_entrant_or_zombie(zombie); }

  int get_state() const {
    return _state;
  }

  void  make_unloaded();

  bool has_dependencies()                         { return dependencies_size() != 0; }
  void print_dependencies()                       PRODUCT_RETURN;
  void flush_dependencies(bool delete_immediately);
  bool has_flushed_dependencies()                 { return _has_flushed_dependencies; }
  void set_has_flushed_dependencies()             {
    assert(!has_flushed_dependencies(), "should only happen once");
    _has_flushed_dependencies = 1;
  }

  int   comp_level() const                        { return _comp_level; }

  void unlink_from_method();

  // Support for oops in scopes and relocs:
  // Note: index 0 is reserved for null.
  oop   oop_at(int index) const;
  oop   oop_at_phantom(int index) const; // phantom reference
  oop*  oop_addr_at(int index) const {  // for GC
    // relocation indexes are biased by 1 (because 0 is reserved)
    assert(index > 0 && index <= oops_count(), "must be a valid non-zero index");
    assert(!_oops_are_stale, "oops are stale");
    return &oops_begin()[index - 1];
  }

  // Support for meta data in scopes and relocs:
  // Note: index 0 is reserved for null.
  Metadata*     metadata_at(int index) const      { return index == 0 ? NULL: *metadata_addr_at(index); }
  Metadata**  metadata_addr_at(int index) const {  // for GC
    // relocation indexes are biased by 1 (because 0 is reserved)
    assert(index > 0 && index <= metadata_count(), "must be a valid non-zero index");
    return &metadata_begin()[index - 1];
  }

  void copy_values(GrowableArray<jobject>* oops);
  void copy_values(GrowableArray<Metadata*>* metadata);

  void free_native_invokers();

  // Relocation support
private:
  void fix_oop_relocations(address begin, address end, bool initialize_immediates);
  inline void initialize_immediate_oop(oop* dest, jobject handle);

public:
  void fix_oop_relocations(address begin, address end) { fix_oop_relocations(begin, end, false); }
  void fix_oop_relocations()                           { fix_oop_relocations(NULL, NULL, false); }

  // Sweeper support
  long  stack_traversal_mark()                    { return _stack_traversal_mark; }
  void  set_stack_traversal_mark(long l)          { _stack_traversal_mark = l; }

  // On-stack replacement support
  int   osr_entry_bci() const                     { assert(is_osr_method(), "wrong kind of nmethod"); return _entry_bci; }
  address  osr_entry() const                      { assert(is_osr_method(), "wrong kind of nmethod"); return _osr_entry_point; }
  void  invalidate_osr_method();
  nmethod* osr_link() const                       { return _osr_link; }
  void     set_osr_link(nmethod *n)               { _osr_link = n; }

  // Verify calls to dead methods have been cleaned.
  void verify_clean_inline_caches();

  // unlink and deallocate this nmethod
  // Only NMethodSweeper class is expected to use this. NMethodSweeper is not
  // expected to use any other private methods/data in this class.

 protected:
  void flush();

 public:
  // When true is returned, it is unsafe to remove this nmethod even if
  // it is a zombie, since the VM or the ServiceThread might still be
  // using it.
  bool is_locked_by_vm() const                    { return _lock_count >0; }

  // See comment at definition of _last_seen_on_stack
  void mark_as_seen_on_stack();
  bool can_convert_to_zombie();

  // Evolution support. We make old (discarded) compiled methods point to new Method*s.
  void set_method(Method* method) { _method = method; }

#if INCLUDE_JVMCI
  // Gets the JVMCI name of this nmethod.
  const char* jvmci_name();

  // Records the pending failed speculation in the
  // JVMCI speculation log associated with this nmethod.
  void update_speculation(JavaThread* thread);

  // Gets the data specific to a JVMCI compiled method.
  // This returns a non-NULL value iff this nmethod was
  // compiled by the JVMCI compiler.
  JVMCINMethodData* jvmci_nmethod_data() const {
    return jvmci_data_size() == 0 ? NULL : (JVMCINMethodData*) jvmci_data_begin();
  }
#endif

 public:
  void oops_do(OopClosure* f) { oops_do(f, false); }
  void oops_do(OopClosure* f, bool allow_dead);

  // All-in-one claiming of nmethods: returns true if the caller successfully claimed that
  // nmethod.
  bool oops_do_try_claim();

  // Class containing callbacks for the oops_do_process_weak/strong() methods
  // below.
  class OopsDoProcessor {
  public:
    // Process the oops of the given nmethod based on whether it has been called
    // in a weak or strong processing context, i.e. apply either weak or strong
    // work on it.
    virtual void do_regular_processing(nmethod* nm) = 0;
    // Assuming that the oops of the given nmethod has already been its weak
    // processing applied, apply the remaining strong processing part.
    virtual void do_remaining_strong_processing(nmethod* nm) = 0;
  };

  // The following two methods do the work corresponding to weak/strong nmethod
  // processing.
  void oops_do_process_weak(OopsDoProcessor* p);
  void oops_do_process_strong(OopsDoProcessor* p);

  static void oops_do_marking_prologue();
  static void oops_do_marking_epilogue();

 private:
  ScopeDesc* scope_desc_in(address begin, address end);

  address* orig_pc_addr(const frame* fr);

  // used by jvmti to track if the load and unload events has been reported
  bool  unload_reported() const                   { return _unload_reported; }
  void  set_unload_reported()                     { _unload_reported = true; }
  bool  load_reported() const                     { return _load_reported; }
  void  set_load_reported()                       { _load_reported = true; }

 public:
  // copying of debugging information
  void copy_scopes_pcs(PcDesc* pcs, int count);
  void copy_scopes_data(address buffer, int size);

  // Accessor/mutator for the original pc of a frame before a frame was deopted.
  address get_original_pc(const frame* fr) { return *orig_pc_addr(fr); }
  void    set_original_pc(const frame* fr, address pc) { *orig_pc_addr(fr) = pc; }

  // jvmti support:
  void post_compiled_method_load_event(JvmtiThreadState* state = NULL);

  // verify operations
  void verify();
  void verify_scopes();
  void verify_interrupt_point(address interrupt_point);

  // Disassemble this nmethod with additional debug information, e.g. information about blocks.
  void decode2(outputStream* st) const;
  void print_constant_pool(outputStream* st);

  // Avoid hiding of parent's 'decode(outputStream*)' method.
  void decode(outputStream* st) const { decode2(st); } // just delegate here.

  // printing support
  void print()                          const;
  void print(outputStream* st)          const;
  void print_code();

#if defined(SUPPORT_DATA_STRUCTS)
  // print output in opt build for disassembler library
  void print_relocations()                        PRODUCT_RETURN;
  void print_pcs() { print_pcs_on(tty); }
  void print_pcs_on(outputStream* st);
  void print_scopes() { print_scopes_on(tty); }
  void print_scopes_on(outputStream* st)          PRODUCT_RETURN;
  void print_value_on(outputStream* st) const;
  void print_native_invokers();
  void print_handler_table();
  void print_nul_chk_table();
  void print_recorded_oop(int log_n, int index);
  void print_recorded_oops();
  void print_recorded_metadata();

  void print_oops(outputStream* st);     // oops from the underlying CodeBlob.
  void print_metadata(outputStream* st); // metadata in metadata pool.
#else
  // void print_pcs()                             PRODUCT_RETURN;
  void print_pcs()                                { return; }
#endif

  void print_calls(outputStream* st)              PRODUCT_RETURN;
  static void print_statistics()                  PRODUCT_RETURN;

  void maybe_print_nmethod(DirectiveSet* directive);
  void print_nmethod(bool print_code);

  // need to re-define this from CodeBlob else the overload hides it
  virtual void print_on(outputStream* st) const { CodeBlob::print_on(st); }
  void print_on(outputStream* st, const char* msg) const;

  // Logging
  void log_identity(xmlStream* log) const;
  void log_new_nmethod() const;
  void log_state_change() const;

  // Prints block-level comments, including nmethod specific block labels:
  virtual void print_block_comment(outputStream* stream, address block_begin) const {
#if defined(SUPPORT_ASSEMBLY) || defined(SUPPORT_ABSTRACT_ASSEMBLY)
    print_nmethod_labels(stream, block_begin);
    CodeBlob::print_block_comment(stream, block_begin);
#endif
  }

  void print_nmethod_labels(outputStream* stream, address block_begin, bool print_section_labels=true) const;
  const char* nmethod_section_label(address pos) const;

  // returns whether this nmethod has code comments.
  bool has_code_comment(address begin, address end);
  // Prints a comment for one native instruction (reloc info, pc desc)
  void print_code_comment_on(outputStream* st, int column, address begin, address end);

  // Compiler task identification.  Note that all OSR methods
  // are numbered in an independent sequence if CICountOSR is true,
  // and native method wrappers are also numbered independently if
  // CICountNative is true.
  virtual int compile_id() const { return _compile_id; }
  const char* compile_kind() const;

  // tells if any of this method's dependencies have been invalidated
  // (this is expensive!)
  static void check_all_dependencies(DepChange& changes);

  // tells if this compiled method is dependent on the given changes,
  // and the changes have invalidated it
  bool check_dependency_on(DepChange& changes);

  // Fast breakpoint support. Tells if this compiled method is
  // dependent on the given method. Returns true if this nmethod
  // corresponds to the given method as well.
  virtual bool is_dependent_on_method(Method* dependee);

  // is it ok to patch at address?
  bool is_patchable_at(address instr_address);

  // JVMTI's GetLocalInstance() support
  ByteSize native_receiver_sp_offset() {
    return _native_receiver_sp_offset;
  }
  ByteSize native_basic_lock_sp_offset() {
    return _native_basic_lock_sp_offset;
  }

  // support for code generation
  static int verified_entry_point_offset()        { return offset_of(nmethod, _verified_entry_point); }
  static int osr_entry_point_offset()             { return offset_of(nmethod, _osr_entry_point); }
  static int state_offset()                       { return offset_of(nmethod, _state); }

  virtual void metadata_do(MetadataClosure* f);

  NativeCallWrapper* call_wrapper_at(address call) const;
  NativeCallWrapper* call_wrapper_before(address return_pc) const;
  address call_instruction_address(address pc) const;

  virtual CompiledStaticCall* compiledStaticCall_at(Relocation* call_site) const;
  virtual CompiledStaticCall* compiledStaticCall_at(address addr) const;
  virtual CompiledStaticCall* compiledStaticCall_before(address addr) const;
};

// Locks an nmethod so its code will not get removed and it will not
// be made into a zombie, even if it is a not_entrant method. After the
// nmethod becomes a zombie, if CompiledMethodUnload event processing
// needs to be done, then lock_nmethod() is used directly to keep the
// generated code from being reused too early.
class nmethodLocker : public StackObj {
  CompiledMethod* _nm;

 public:

  // note: nm can be NULL
  // Only JvmtiDeferredEvent::compiled_method_unload_event()
  // should pass zombie_ok == true.
  static void lock_nmethod(CompiledMethod* nm, bool zombie_ok = false);
  static void unlock_nmethod(CompiledMethod* nm); // (ditto)

  nmethodLocker(address pc); // derive nm from pc
  nmethodLocker(nmethod *nm) { _nm = nm; lock_nmethod(_nm); }
  nmethodLocker(CompiledMethod *nm) {
    _nm = nm;
    lock(_nm);
  }

  static void lock(CompiledMethod* method, bool zombie_ok = false) {
    if (method == NULL) return;
    lock_nmethod(method, zombie_ok);
  }

  static void unlock(CompiledMethod* method) {
    if (method == NULL) return;
    unlock_nmethod(method);
  }

  nmethodLocker() { _nm = NULL; }
  ~nmethodLocker() {
    unlock(_nm);
  }

  CompiledMethod* code() { return _nm; }
  void set_code(CompiledMethod* new_nm, bool zombie_ok = false) {
    unlock(_nm);   // note:  This works even if _nm==new_nm.
    _nm = new_nm;
    lock(_nm, zombie_ok);
  }
};

#endif // SHARE_CODE_NMETHOD_HPP
