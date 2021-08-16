/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_C1_C1_GRAPHBUILDER_HPP
#define SHARE_C1_C1_GRAPHBUILDER_HPP

#include "c1/c1_IR.hpp"
#include "c1/c1_Instruction.hpp"
#include "c1/c1_ValueMap.hpp"
#include "c1/c1_ValueStack.hpp"
#include "ci/ciMethodData.hpp"
#include "ci/ciStreams.hpp"
#include "compiler/compileLog.hpp"

class MemoryBuffer;

class GraphBuilder {
 private:
  // Per-scope data. These are pushed and popped as we descend into
  // inlined methods. Currently in order to generate good code in the
  // inliner we have to attempt to inline methods directly into the
  // basic block we are parsing; this adds complexity.
  class ScopeData: public CompilationResourceObj {
   private:
    ScopeData*  _parent;
    // bci-to-block mapping
    BlockList*   _bci2block;
    // Scope
    IRScope*     _scope;
    // Whether this scope or any parent scope has exception handlers
    bool         _has_handler;
    // The bytecodes
    ciBytecodeStream* _stream;

    // Work list
    BlockList*   _work_list;

    // Maximum inline size for this scope
    intx         _max_inline_size;
    // Expression stack depth at point where inline occurred
    int          _caller_stack_size;

    // The continuation point for the inline. Currently only used in
    // multi-block inlines, but eventually would like to use this for
    // all inlines for uniformity and simplicity; in this case would
    // get the continuation point from the BlockList instead of
    // fabricating it anew because Invokes would be considered to be
    // BlockEnds.
    BlockBegin*  _continuation;

    // Was this ScopeData created only for the parsing and inlining of
    // a jsr?
    bool         _parsing_jsr;
    // We track the destination bci of the jsr only to determine
    // bailout conditions, since we only handle a subset of all of the
    // possible jsr-ret control structures. Recursive invocations of a
    // jsr are disallowed by the verifier.
    int          _jsr_entry_bci;
    // We need to track the local variable in which the return address
    // was stored to ensure we can handle inlining the jsr, because we
    // don't handle arbitrary jsr/ret constructs.
    int          _jsr_ret_addr_local;
    // If we are parsing a jsr, the continuation point for rets
    BlockBegin*  _jsr_continuation;
    // Cloned XHandlers for jsr-related ScopeDatas
    XHandlers*   _jsr_xhandlers;

    // Number of returns seen in this scope
    int          _num_returns;

    // In order to generate profitable code for inlining, we currently
    // have to perform an optimization for single-block inlined
    // methods where we continue parsing into the same block. This
    // allows us to perform CSE across inlined scopes and to avoid
    // storing parameters to the stack. Having a global register
    // allocator and being able to perform global CSE would allow this
    // code to be removed and thereby simplify the inliner.
    BlockBegin*  _cleanup_block;       // The block to which the return was added
    Instruction* _cleanup_return_prev; // Instruction before return instruction
    ValueStack*  _cleanup_state;       // State of that block (not yet pinned)

    // When inlining do not push the result on the stack
    bool         _ignore_return;

   public:
    ScopeData(ScopeData* parent);

    ScopeData* parent() const                      { return _parent;            }

    BlockList* bci2block() const                   { return _bci2block;         }
    void       set_bci2block(BlockList* bci2block) { _bci2block = bci2block;    }

    // NOTE: this has a different effect when parsing jsrs
    BlockBegin* block_at(int bci);

    IRScope* scope() const                         { return _scope;             }
    // Has side-effect of setting has_handler flag
    void set_scope(IRScope* scope);

    // Whether this or any parent scope has exception handlers
    bool has_handler() const                       { return _has_handler;       }
    void set_has_handler()                         { _has_handler = true;       }

    // Exception handlers list to be used for this scope
    XHandlers* xhandlers() const;

    // How to get a block to be parsed
    void add_to_work_list(BlockBegin* block);
    // How to remove the next block to be parsed; returns NULL if none left
    BlockBegin* remove_from_work_list();
    // Indicates parse is over
    bool is_work_list_empty() const;

    ciBytecodeStream* stream()                     { return _stream;            }
    void set_stream(ciBytecodeStream* stream)      { _stream = stream;          }

    intx max_inline_size() const                   { return _max_inline_size;   }

    BlockBegin* continuation() const               { return _continuation;      }
    void set_continuation(BlockBegin* cont)        { _continuation = cont;      }

    // Indicates whether this ScopeData was pushed only for the
    // parsing and inlining of a jsr
    bool parsing_jsr() const                       { return _parsing_jsr;       }
    void set_parsing_jsr()                         { _parsing_jsr = true;       }
    int  jsr_entry_bci() const                     { return _jsr_entry_bci;     }
    void set_jsr_entry_bci(int bci)                { _jsr_entry_bci = bci;      }
    void set_jsr_return_address_local(int local_no){ _jsr_ret_addr_local = local_no; }
    int  jsr_return_address_local() const          { return _jsr_ret_addr_local; }
    // Must be called after scope is set up for jsr ScopeData
    void setup_jsr_xhandlers();

    // The jsr continuation is only used when parsing_jsr is true, and
    // is different from the "normal" continuation since we can end up
    // doing a return (rather than a ret) from within a subroutine
    BlockBegin* jsr_continuation() const           { return _jsr_continuation;  }
    void set_jsr_continuation(BlockBegin* cont)    { _jsr_continuation = cont;  }

    int num_returns();
    void incr_num_returns();

    void set_inline_cleanup_info(BlockBegin* block,
                                 Instruction* return_prev,
                                 ValueStack* return_state);
    BlockBegin*  inline_cleanup_block() const      { return _cleanup_block; }
    Instruction* inline_cleanup_return_prev() const{ return _cleanup_return_prev; }
    ValueStack*  inline_cleanup_state() const      { return _cleanup_state; }

    bool ignore_return() const                     { return _ignore_return;          }
    void set_ignore_return(bool ignore_return)     { _ignore_return = ignore_return; }
  };

  // for all GraphBuilders
  static bool       _can_trap[Bytecodes::number_of_java_codes];

  // for each instance of GraphBuilder
  ScopeData*        _scope_data;                 // Per-scope data; used for inlining
  Compilation*      _compilation;                // the current compilation
  ValueMap*         _vmap;                       // the map of values encountered (for CSE)
  MemoryBuffer*     _memory;
  const char*       _inline_bailout_msg;         // non-null if most recent inline attempt failed
  int               _instruction_count;          // for bailing out in pathological jsr/ret cases
  BlockBegin*       _start;                      // the start block
  BlockBegin*       _osr_entry;                  // the osr entry block block
  ValueStack*       _initial_state;              // The state for the start block

  // for each call to connect_to_end; can also be set by inliner
  BlockBegin*       _block;                      // the current block
  ValueStack*       _state;                      // the current execution state
  Instruction*      _last;                       // the last instruction added
  bool              _skip_block;                 // skip processing of the rest of this block

  // accessors
  ScopeData*        scope_data() const           { return _scope_data; }
  Compilation*      compilation() const          { return _compilation; }
  BlockList*        bci2block() const            { return scope_data()->bci2block(); }
  ValueMap*         vmap() const                 { assert(UseLocalValueNumbering, "should not access otherwise"); return _vmap; }
  bool              has_handler() const          { return scope_data()->has_handler(); }

  BlockBegin*       block() const                { return _block; }
  ValueStack*       state() const                { return _state; }
  void              set_state(ValueStack* state) { _state = state; }
  IRScope*          scope() const                { return scope_data()->scope(); }
  ciMethod*         method() const               { return scope()->method(); }
  ciBytecodeStream* stream() const               { return scope_data()->stream(); }
  Instruction*      last() const                 { return _last; }
  Bytecodes::Code   code() const                 { return stream()->cur_bc(); }
  int               bci() const                  { return stream()->cur_bci(); }
  int               next_bci() const             { return stream()->next_bci(); }

  // unified bailout support
  void bailout(const char* msg) const            { compilation()->bailout(msg); }
  bool bailed_out() const                        { return compilation()->bailed_out(); }

  // stack manipulation helpers
  void ipush(Value t) const                      { state()->ipush(t); }
  void lpush(Value t) const                      { state()->lpush(t); }
  void fpush(Value t) const                      { state()->fpush(t); }
  void dpush(Value t) const                      { state()->dpush(t); }
  void apush(Value t) const                      { state()->apush(t); }
  void  push(ValueType* type, Value t) const     { state()-> push(type, t); }

  Value ipop()                                   { return state()->ipop(); }
  Value lpop()                                   { return state()->lpop(); }
  Value fpop()                                   { return state()->fpop(); }
  Value dpop()                                   { return state()->dpop(); }
  Value apop()                                   { return state()->apop(); }
  Value  pop(ValueType* type)                    { return state()-> pop(type); }

  // instruction helpers
  void load_constant();
  void load_local(ValueType* type, int index);
  void store_local(ValueType* type, int index);
  void store_local(ValueStack* state, Value value, int index);
  void load_indexed (BasicType type);
  void store_indexed(BasicType type);
  void stack_op(Bytecodes::Code code);
  void arithmetic_op(ValueType* type, Bytecodes::Code code, ValueStack* state_before = NULL);
  void negate_op(ValueType* type);
  void shift_op(ValueType* type, Bytecodes::Code code);
  void logic_op(ValueType* type, Bytecodes::Code code);
  void compare_op(ValueType* type, Bytecodes::Code code);
  void convert(Bytecodes::Code op, BasicType from, BasicType to);
  void increment();
  void _goto(int from_bci, int to_bci);
  void if_node(Value x, If::Condition cond, Value y, ValueStack* stack_before);
  void if_zero(ValueType* type, If::Condition cond);
  void if_null(ValueType* type, If::Condition cond);
  void if_same(ValueType* type, If::Condition cond);
  void jsr(int dest);
  void ret(int local_index);
  void table_switch();
  void lookup_switch();
  void method_return(Value x, bool ignore_return = false);
  void call_register_finalizer();
  void access_field(Bytecodes::Code code);
  void invoke(Bytecodes::Code code);
  void new_instance(int klass_index);
  void new_type_array();
  void new_object_array();
  void check_cast(int klass_index);
  void instance_of(int klass_index);
  void monitorenter(Value x, int bci);
  void monitorexit(Value x, int bci);
  void new_multi_array(int dimensions);
  void throw_op(int bci);
  Value round_fp(Value fp_value);

  // stack/code manipulation helpers
  Instruction* append_with_bci(Instruction* instr, int bci);
  Instruction* append(Instruction* instr);
  Instruction* append_split(StateSplit* instr);

  // other helpers
  BlockBegin* block_at(int bci)                  { return scope_data()->block_at(bci); }
  XHandlers* handle_exception(Instruction* instruction);
  void connect_to_end(BlockBegin* beg);
  void null_check(Value value);
  void eliminate_redundant_phis(BlockBegin* start);
  BlockEnd* iterate_bytecodes_for_block(int bci);
  void iterate_all_blocks(bool start_in_current_block_for_inlining = false);
  Dependencies* dependency_recorder() const; // = compilation()->dependencies()
  bool direct_compare(ciKlass* k);
  Value make_constant(ciConstant value, ciField* field);

  void kill_all();

  // use of state copy routines (try to minimize unnecessary state
  // object allocations):

  // - if the instruction unconditionally needs a full copy of the
  // state (for patching for example), then use copy_state_before*

  // - if the instruction needs a full copy of the state only for
  // handler generation (Instruction::needs_exception_state() returns
  // false) then use copy_state_exhandling*

  // - if the instruction needs either a full copy of the state for
  // handler generation and a least a minimal copy of the state (as
  // returned by Instruction::exception_state()) for debug info
  // generation (that is when Instruction::needs_exception_state()
  // returns true) then use copy_state_for_exception*

  ValueStack* copy_state_before_with_bci(int bci);
  ValueStack* copy_state_before();
  ValueStack* copy_state_exhandling_with_bci(int bci);
  ValueStack* copy_state_exhandling();
  ValueStack* copy_state_for_exception_with_bci(int bci);
  ValueStack* copy_state_for_exception();
  ValueStack* copy_state_if_bb(bool is_bb) { return (is_bb || compilation()->is_optimistic()) ? copy_state_before() : NULL; }
  ValueStack* copy_state_indexed_access() { return compilation()->is_optimistic() ? copy_state_before() : copy_state_for_exception(); }

  //
  // Inlining support
  //

  // accessors
  bool parsing_jsr() const                               { return scope_data()->parsing_jsr();           }
  BlockBegin* continuation() const                       { return scope_data()->continuation();          }
  BlockBegin* jsr_continuation() const                   { return scope_data()->jsr_continuation();      }
  void set_continuation(BlockBegin* continuation)        { scope_data()->set_continuation(continuation); }
  void set_inline_cleanup_info(BlockBegin* block,
                               Instruction* return_prev,
                               ValueStack* return_state) { scope_data()->set_inline_cleanup_info(block,
                                                                                                  return_prev,
                                                                                                  return_state); }
  void set_inline_cleanup_info() {
    set_inline_cleanup_info(_block, _last, _state);
  }
  BlockBegin*  inline_cleanup_block() const              { return scope_data()->inline_cleanup_block();  }
  Instruction* inline_cleanup_return_prev() const        { return scope_data()->inline_cleanup_return_prev(); }
  ValueStack*  inline_cleanup_state() const              { return scope_data()->inline_cleanup_state();  }
  void restore_inline_cleanup_info() {
    _block = inline_cleanup_block();
    _last  = inline_cleanup_return_prev();
    _state = inline_cleanup_state();
  }
  void incr_num_returns()                                { scope_data()->incr_num_returns();             }
  int  num_returns() const                               { return scope_data()->num_returns();           }
  intx max_inline_size() const                           { return scope_data()->max_inline_size();       }
  int  inline_level() const                              { return scope()->level();                      }
  int  recursive_inline_level(ciMethod* callee) const;

  // inlining of synchronized methods
  void inline_sync_entry(Value lock, BlockBegin* sync_handler);
  void fill_sync_handler(Value lock, BlockBegin* sync_handler, bool default_handler = false);

  void build_graph_for_intrinsic(ciMethod* callee, bool ignore_return);

  // inliners
  bool try_inline(           ciMethod* callee, bool holder_known, bool ignore_return, Bytecodes::Code bc = Bytecodes::_illegal, Value receiver = NULL);
  bool try_inline_intrinsics(ciMethod* callee, bool ignore_return = false);
  bool try_inline_full(      ciMethod* callee, bool holder_known, bool ignore_return, Bytecodes::Code bc = Bytecodes::_illegal, Value receiver = NULL);
  bool try_inline_jsr(int jsr_dest_bci);

  const char* check_can_parse(ciMethod* callee) const;
  const char* should_not_inline(ciMethod* callee) const;

  // JSR 292 support
  bool try_method_handle_inline(ciMethod* callee, bool ignore_return);

  // helpers
  void inline_bailout(const char* msg);
  BlockBegin* header_block(BlockBegin* entry, BlockBegin::Flag f, ValueStack* state);
  BlockBegin* setup_start_block(int osr_bci, BlockBegin* std_entry, BlockBegin* osr_entry, ValueStack* init_state);
  void setup_osr_entry_block();
  void clear_inline_bailout();
  ValueStack* state_at_entry();
  void push_root_scope(IRScope* scope, BlockList* bci2block, BlockBegin* start);
  void push_scope(ciMethod* callee, BlockBegin* continuation);
  void push_scope_for_jsr(BlockBegin* jsr_continuation, int jsr_dest_bci);
  void pop_scope();
  void pop_scope_for_jsr();

  void append_unsafe_get(ciMethod* callee, BasicType t, bool is_volatile);
  void append_unsafe_put(ciMethod* callee, BasicType t, bool is_volatile);
  void append_unsafe_CAS(ciMethod* callee);
  void append_unsafe_get_and_set(ciMethod* callee, bool is_add);
  void append_char_access(ciMethod* callee, bool is_store);

  void print_inlining(ciMethod* callee, const char* msg, bool success = true);

  void profile_call(ciMethod* callee, Value recv, ciKlass* predicted_holder, Values* obj_args, bool inlined);
  void profile_return_type(Value ret, ciMethod* callee, ciMethod* m = NULL, int bci = -1);
  void profile_invocation(ciMethod* inlinee, ValueStack* state);

  // Shortcuts to profiling control.
  bool is_profiling()          { return _compilation->is_profiling();          }
  bool profile_branches()      { return _compilation->profile_branches();      }
  bool profile_calls()         { return _compilation->profile_calls();         }
  bool profile_inlined_calls() { return _compilation->profile_inlined_calls(); }
  bool profile_checkcasts()    { return _compilation->profile_checkcasts();    }
  bool profile_parameters()    { return _compilation->profile_parameters();    }
  bool profile_arguments()     { return _compilation->profile_arguments();     }
  bool profile_return()        { return _compilation->profile_return();        }

  Values* args_list_for_profiling(ciMethod* target, int& start, bool may_have_receiver);
  Values* collect_args_for_profiling(Values* args, ciMethod* target, bool may_have_receiver);
  void check_args_for_profiling(Values* obj_args, int expected);

 public:
  NOT_PRODUCT(void print_stats();)

  // initialization
  static void initialize();

  // public
  static bool can_trap(ciMethod* method, Bytecodes::Code code) {
    assert(0 <= code && code < Bytecodes::number_of_java_codes, "illegal bytecode");
    if (_can_trap[code]) return true;
    // special handling for finalizer registration
    return code == Bytecodes::_return && method->intrinsic_id() == vmIntrinsics::_Object_init;
  }

  // creation
  GraphBuilder(Compilation* compilation, IRScope* scope);
  static void sort_top_into_worklist(BlockList* worklist, BlockBegin* top);

  BlockBegin* start() const                      { return _start; }
};

#endif // SHARE_C1_C1_GRAPHBUILDER_HPP
