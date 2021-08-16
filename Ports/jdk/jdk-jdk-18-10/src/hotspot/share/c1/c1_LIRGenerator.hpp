/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_C1_C1_LIRGENERATOR_HPP
#define SHARE_C1_C1_LIRGENERATOR_HPP

#include "c1/c1_Decorators.hpp"
#include "c1/c1_Instruction.hpp"
#include "c1/c1_LIR.hpp"
#include "ci/ciMethodData.hpp"
#include "gc/shared/barrierSet.hpp"
#include "jfr/support/jfrIntrinsics.hpp"
#include "utilities/macros.hpp"
#include "utilities/sizes.hpp"

class BarrierSetC1;

// The classes responsible for code emission and register allocation


class LIRGenerator;
class LIREmitter;
class Invoke;
class LIRItem;

typedef GrowableArray<LIRItem*> LIRItemList;

class C1SwitchRange: public CompilationResourceObj {
 private:
  int _low_key;
  int _high_key;
  BlockBegin* _sux;
 public:
  C1SwitchRange(int start_key, BlockBegin* sux): _low_key(start_key), _high_key(start_key), _sux(sux) {}
  void set_high_key(int key) { _high_key = key; }

  int high_key() const { return _high_key; }
  int low_key() const { return _low_key; }
  BlockBegin* sux() const { return _sux; }
};

typedef GrowableArray<C1SwitchRange*> SwitchRangeArray;
typedef GrowableArray<C1SwitchRange*> SwitchRangeList;

class ResolveNode;

typedef GrowableArray<ResolveNode*> NodeList;

// Node objects form a directed graph of LIR_Opr
// Edges between Nodes represent moves from one Node to its destinations
class ResolveNode: public CompilationResourceObj {
 private:
  LIR_Opr    _operand;       // the source or destinaton
  NodeList   _destinations;  // for the operand
  bool       _assigned;      // Value assigned to this Node?
  bool       _visited;       // Node already visited?
  bool       _start_node;    // Start node already visited?

 public:
  ResolveNode(LIR_Opr operand)
    : _operand(operand)
    , _assigned(false)
    , _visited(false)
    , _start_node(false) {};

  // accessors
  LIR_Opr operand() const           { return _operand; }
  int no_of_destinations() const    { return _destinations.length(); }
  ResolveNode* destination_at(int i)     { return _destinations.at(i); }
  bool assigned() const             { return _assigned; }
  bool visited() const              { return _visited; }
  bool start_node() const           { return _start_node; }

  // modifiers
  void append(ResolveNode* dest)         { _destinations.append(dest); }
  void set_assigned()               { _assigned = true; }
  void set_visited()                { _visited = true; }
  void set_start_node()             { _start_node = true; }
};


// This is shared state to be used by the PhiResolver so the operand
// arrays don't have to be reallocated for each resolution.
class PhiResolverState: public CompilationResourceObj {
  friend class PhiResolver;

 private:
  NodeList _virtual_operands; // Nodes where the operand is a virtual register
  NodeList _other_operands;   // Nodes where the operand is not a virtual register
  NodeList _vreg_table;       // Mapping from virtual register to Node

 public:
  PhiResolverState() {}

  void reset();
};


// class used to move value of phi operand to phi function
class PhiResolver: public CompilationResourceObj {
 private:
  LIRGenerator*     _gen;
  PhiResolverState& _state; // temporary state cached by LIRGenerator

  ResolveNode*   _loop;
  LIR_Opr _temp;

  // access to shared state arrays
  NodeList& virtual_operands() { return _state._virtual_operands; }
  NodeList& other_operands()   { return _state._other_operands;   }
  NodeList& vreg_table()       { return _state._vreg_table;       }

  ResolveNode* create_node(LIR_Opr opr, bool source);
  ResolveNode* source_node(LIR_Opr opr)      { return create_node(opr, true); }
  ResolveNode* destination_node(LIR_Opr opr) { return create_node(opr, false); }

  void emit_move(LIR_Opr src, LIR_Opr dest);
  void move_to_temp(LIR_Opr src);
  void move_temp_to(LIR_Opr dest);
  void move(ResolveNode* src, ResolveNode* dest);

  LIRGenerator* gen() {
    return _gen;
  }

 public:
  PhiResolver(LIRGenerator* _lir_gen);
  ~PhiResolver();

  void move(LIR_Opr src, LIR_Opr dest);
};


// only the classes below belong in the same file
class LIRGenerator: public InstructionVisitor, public BlockClosure {
 // LIRGenerator should never get instatiated on the heap.
 private:
  void* operator new(size_t size) throw();
  void* operator new[](size_t size) throw();
  void operator delete(void* p) { ShouldNotReachHere(); }
  void operator delete[](void* p) { ShouldNotReachHere(); }

  Compilation*  _compilation;
  ciMethod*     _method;    // method that we are compiling
  PhiResolverState  _resolver_state;
  BlockBegin*   _block;
  int           _virtual_register_number;
  Values        _instruction_for_operand;
  BitMap2D      _vreg_flags; // flags which can be set on a per-vreg basis
  LIR_List*     _lir;

  LIRGenerator* gen() {
    return this;
  }

  void print_if_not_loaded(const NewInstance* new_instance) PRODUCT_RETURN;

 public:
#ifdef ASSERT
  LIR_List* lir(const char * file, int line) const {
    _lir->set_file_and_line(file, line);
    return _lir;
  }
#endif
  LIR_List* lir() const {
    return _lir;
  }

 private:
  // a simple cache of constants used within a block
  GrowableArray<LIR_Const*>       _constants;
  LIR_OprList                     _reg_for_constants;
  Values                          _unpinned_constants;

  friend class PhiResolver;

 public:
  // unified bailout support
  void bailout(const char* msg) const            { compilation()->bailout(msg); }
  bool bailed_out() const                        { return compilation()->bailed_out(); }

  void block_do_prolog(BlockBegin* block);
  void block_do_epilog(BlockBegin* block);

  // register allocation
  LIR_Opr rlock(Value instr);                      // lock a free register
  LIR_Opr rlock_result(Value instr);
  LIR_Opr rlock_result(Value instr, BasicType type);
  LIR_Opr rlock_byte(BasicType type);
  LIR_Opr rlock_callee_saved(BasicType type);

  // get a constant into a register and get track of what register was used
  LIR_Opr load_constant(Constant* x);
  LIR_Opr load_constant(LIR_Const* constant);

  // Given an immediate value, return an operand usable in logical ops.
  LIR_Opr load_immediate(int x, BasicType type);

  void  set_result(Value x, LIR_Opr opr)           {
    assert(opr->is_valid(), "must set to valid value");
    assert(x->operand()->is_illegal(), "operand should never change");
    assert(!opr->is_register() || opr->is_virtual(), "should never set result to a physical register");
    x->set_operand(opr);
    assert(opr == x->operand(), "must be");
    if (opr->is_virtual()) {
      _instruction_for_operand.at_put_grow(opr->vreg_number(), x, NULL);
    }
  }
  void  set_no_result(Value x)                     { assert(!x->has_uses(), "can't have use"); x->clear_operand(); }

  friend class LIRItem;

  LIR_Opr round_item(LIR_Opr opr);
  LIR_Opr force_to_spill(LIR_Opr value, BasicType t);

  PhiResolverState& resolver_state() { return _resolver_state; }

  void  move_to_phi(PhiResolver* resolver, Value cur_val, Value sux_val);
  void  move_to_phi(ValueStack* cur_state);

  // platform dependent
  LIR_Opr getThreadPointer();

 private:
  // code emission
  void do_ArithmeticOp_Long(ArithmeticOp* x);
  void do_ArithmeticOp_Int (ArithmeticOp* x);
  void do_ArithmeticOp_FPU (ArithmeticOp* x);

  void do_RegisterFinalizer(Intrinsic* x);
  void do_isInstance(Intrinsic* x);
  void do_isPrimitive(Intrinsic* x);
  void do_getModifiers(Intrinsic* x);
  void do_getClass(Intrinsic* x);
  void do_currentThread(Intrinsic* x);
  void do_getObjectSize(Intrinsic* x);
  void do_FmaIntrinsic(Intrinsic* x);
  void do_MathIntrinsic(Intrinsic* x);
  void do_LibmIntrinsic(Intrinsic* x);
  void do_ArrayCopy(Intrinsic* x);
  void do_CompareAndSwap(Intrinsic* x, ValueType* type);
  void do_PreconditionsCheckIndex(Intrinsic* x, BasicType type);
  void do_FPIntrinsics(Intrinsic* x);
  void do_Reference_get(Intrinsic* x);
  void do_update_CRC32(Intrinsic* x);
  void do_update_CRC32C(Intrinsic* x);
  void do_vectorizedMismatch(Intrinsic* x);
  void do_blackhole(Intrinsic* x);

 public:
  LIR_Opr call_runtime(BasicTypeArray* signature, LIRItemList* args, address entry, ValueType* result_type, CodeEmitInfo* info);
  LIR_Opr call_runtime(BasicTypeArray* signature, LIR_OprList* args, address entry, ValueType* result_type, CodeEmitInfo* info);

  // convenience functions
  LIR_Opr call_runtime(Value arg1, address entry, ValueType* result_type, CodeEmitInfo* info);
  LIR_Opr call_runtime(Value arg1, Value arg2, address entry, ValueType* result_type, CodeEmitInfo* info);

  // Access API

 private:
  BarrierSetC1 *_barrier_set;

 public:
  void access_store_at(DecoratorSet decorators, BasicType type,
                       LIRItem& base, LIR_Opr offset, LIR_Opr value,
                       CodeEmitInfo* patch_info = NULL, CodeEmitInfo* store_emit_info = NULL);

  void access_load_at(DecoratorSet decorators, BasicType type,
                      LIRItem& base, LIR_Opr offset, LIR_Opr result,
                      CodeEmitInfo* patch_info = NULL, CodeEmitInfo* load_emit_info = NULL);

  void access_load(DecoratorSet decorators, BasicType type,
                   LIR_Opr addr, LIR_Opr result);

  LIR_Opr access_atomic_cmpxchg_at(DecoratorSet decorators, BasicType type,
                                   LIRItem& base, LIRItem& offset, LIRItem& cmp_value, LIRItem& new_value);

  LIR_Opr access_atomic_xchg_at(DecoratorSet decorators, BasicType type,
                                LIRItem& base, LIRItem& offset, LIRItem& value);

  LIR_Opr access_atomic_add_at(DecoratorSet decorators, BasicType type,
                               LIRItem& base, LIRItem& offset, LIRItem& value);

  // These need to guarantee JMM volatile semantics are preserved on each platform
  // and requires one implementation per architecture.
  LIR_Opr atomic_cmpxchg(BasicType type, LIR_Opr addr, LIRItem& cmp_value, LIRItem& new_value);
  LIR_Opr atomic_xchg(BasicType type, LIR_Opr addr, LIRItem& new_value);
  LIR_Opr atomic_add(BasicType type, LIR_Opr addr, LIRItem& new_value);

#ifdef CARDTABLEBARRIERSET_POST_BARRIER_HELPER
  virtual void CardTableBarrierSet_post_barrier_helper(LIR_OprDesc* addr, LIR_Const* card_table_base);
#endif

  // specific implementations
  void array_store_check(LIR_Opr value, LIR_Opr array, CodeEmitInfo* store_check_info, ciMethod* profiled_method, int profiled_bci);

  static LIR_Opr result_register_for(ValueType* type, bool callee = false);

  ciObject* get_jobject_constant(Value value);

  LIRItemList* invoke_visit_arguments(Invoke* x);
  void invoke_load_arguments(Invoke* x, LIRItemList* args, const LIR_OprList* arg_list);

  void trace_block_entry(BlockBegin* block);

  // volatile field operations are never patchable because a klass
  // must be loaded to know it's volatile which means that the offset
  // it always known as well.
  void volatile_field_store(LIR_Opr value, LIR_Address* address, CodeEmitInfo* info);
  void volatile_field_load(LIR_Address* address, LIR_Opr result, CodeEmitInfo* info);

  void put_Object_unsafe(LIR_Opr src, LIR_Opr offset, LIR_Opr data, BasicType type, bool is_volatile);
  void get_Object_unsafe(LIR_Opr dest, LIR_Opr src, LIR_Opr offset, BasicType type, bool is_volatile);

  void arithmetic_call_op (Bytecodes::Code code, LIR_Opr result, LIR_OprList* args);

  void increment_counter(address counter, BasicType type, int step = 1);
  void increment_counter(LIR_Address* addr, int step = 1);

  void arithmetic_op(Bytecodes::Code code, LIR_Opr result, LIR_Opr left, LIR_Opr right, LIR_Opr tmp, CodeEmitInfo* info = NULL);
  // machine dependent.  returns true if it emitted code for the multiply
  bool strength_reduce_multiply(LIR_Opr left, jint constant, LIR_Opr result, LIR_Opr tmp);

  void store_stack_parameter (LIR_Opr opr, ByteSize offset_from_sp_in_bytes);

  void klass2reg_with_patching(LIR_Opr r, ciMetadata* obj, CodeEmitInfo* info, bool need_resolve = false);

  // this loads the length and compares against the index
  void array_range_check          (LIR_Opr array, LIR_Opr index, CodeEmitInfo* null_check_info, CodeEmitInfo* range_check_info);

  void arithmetic_op_int  (Bytecodes::Code code, LIR_Opr result, LIR_Opr left, LIR_Opr right, LIR_Opr tmp);
  void arithmetic_op_long (Bytecodes::Code code, LIR_Opr result, LIR_Opr left, LIR_Opr right, CodeEmitInfo* info = NULL);
  void arithmetic_op_fpu  (Bytecodes::Code code, LIR_Opr result, LIR_Opr left, LIR_Opr right, LIR_Opr tmp = LIR_OprFact::illegalOpr);

  void shift_op   (Bytecodes::Code code, LIR_Opr dst_reg, LIR_Opr value, LIR_Opr count, LIR_Opr tmp);

  void logic_op   (Bytecodes::Code code, LIR_Opr dst_reg, LIR_Opr left, LIR_Opr right);

  void monitor_enter (LIR_Opr object, LIR_Opr lock, LIR_Opr hdr, LIR_Opr scratch, int monitor_no, CodeEmitInfo* info_for_exception, CodeEmitInfo* info);
  void monitor_exit  (LIR_Opr object, LIR_Opr lock, LIR_Opr hdr, LIR_Opr scratch, int monitor_no);

  void new_instance    (LIR_Opr  dst, ciInstanceKlass* klass, bool is_unresolved, LIR_Opr  scratch1, LIR_Opr  scratch2, LIR_Opr  scratch3,  LIR_Opr scratch4, LIR_Opr  klass_reg, CodeEmitInfo* info);

  // machine dependent
  void cmp_mem_int(LIR_Condition condition, LIR_Opr base, int disp, int c, CodeEmitInfo* info);
  void cmp_reg_mem(LIR_Condition condition, LIR_Opr reg, LIR_Opr base, int disp, BasicType type, CodeEmitInfo* info);

  void arraycopy_helper(Intrinsic* x, int* flags, ciArrayKlass** expected_type);

  // returns a LIR_Address to address an array location.  May also
  // emit some code as part of address calculation.  If
  // needs_card_mark is true then compute the full address for use by
  // both the store and the card mark.
  LIR_Address* generate_address(LIR_Opr base,
                                LIR_Opr index, int shift,
                                int disp,
                                BasicType type);
  LIR_Address* generate_address(LIR_Opr base, int disp, BasicType type) {
    return generate_address(base, LIR_OprFact::illegalOpr, 0, disp, type);
  }
  LIR_Address* emit_array_address(LIR_Opr array_opr, LIR_Opr index_opr, BasicType type);

  // the helper for generate_address
  void add_large_constant(LIR_Opr src, int c, LIR_Opr dest);

  // machine preferences and characteristics
  bool can_inline_as_constant(Value i S390_ONLY(COMMA int bits = 20)) const;
  bool can_inline_as_constant(LIR_Const* c) const;
  bool can_store_as_constant(Value i, BasicType type) const;

  LIR_Opr safepoint_poll_register();

  void profile_branch(If* if_instr, If::Condition cond);
  void increment_event_counter_impl(CodeEmitInfo* info,
                                    ciMethod *method, LIR_Opr step, int frequency,
                                    int bci, bool backedge, bool notify);
  void increment_event_counter(CodeEmitInfo* info, LIR_Opr step, int bci, bool backedge);
  void increment_invocation_counter(CodeEmitInfo *info) {
    if (compilation()->is_profiling()) {
      increment_event_counter(info, LIR_OprFact::intConst(InvocationCounter::count_increment), InvocationEntryBci, false);
    }
  }
  void increment_backedge_counter(CodeEmitInfo* info, int bci) {
    if (compilation()->is_profiling()) {
      increment_event_counter(info, LIR_OprFact::intConst(InvocationCounter::count_increment), bci, true);
    }
  }
  void increment_backedge_counter_conditionally(LIR_Condition cond, LIR_Opr left, LIR_Opr right, CodeEmitInfo* info, int left_bci, int right_bci, int bci);
  void increment_backedge_counter(CodeEmitInfo* info, LIR_Opr step, int bci) {
    if (compilation()->is_profiling()) {
      increment_event_counter(info, step, bci, true);
    }
  }
  void decrement_age(CodeEmitInfo* info);
  CodeEmitInfo* state_for(Instruction* x, ValueStack* state, bool ignore_xhandler = false);
  CodeEmitInfo* state_for(Instruction* x);

  // allocates a virtual register for this instruction if
  // one isn't already allocated.  Only for Phi and Local.
  LIR_Opr operand_for_instruction(Instruction *x);

  void set_block(BlockBegin* block)              { _block = block; }

  void block_prolog(BlockBegin* block);
  void block_epilog(BlockBegin* block);

  void do_root (Instruction* instr);
  void walk    (Instruction* instr);

  LIR_Opr new_register(BasicType type);
  LIR_Opr new_register(Value value)              { return new_register(as_BasicType(value->type())); }
  LIR_Opr new_register(ValueType* type)          { return new_register(as_BasicType(type)); }

  // returns a register suitable for doing pointer math
  LIR_Opr new_pointer_register() {
#ifdef _LP64
    return new_register(T_LONG);
#else
    return new_register(T_INT);
#endif
  }

  static LIR_Condition lir_cond(If::Condition cond) {
    LIR_Condition l = lir_cond_unknown;
    switch (cond) {
    case If::eql: l = lir_cond_equal;        break;
    case If::neq: l = lir_cond_notEqual;     break;
    case If::lss: l = lir_cond_less;         break;
    case If::leq: l = lir_cond_lessEqual;    break;
    case If::geq: l = lir_cond_greaterEqual; break;
    case If::gtr: l = lir_cond_greater;      break;
    case If::aeq: l = lir_cond_aboveEqual;   break;
    case If::beq: l = lir_cond_belowEqual;   break;
    default: fatal("You must pass valid If::Condition");
    };
    return l;
  }

#ifdef __SOFTFP__
  void do_soft_float_compare(If *x);
#endif // __SOFTFP__

  SwitchRangeArray* create_lookup_ranges(TableSwitch* x);
  SwitchRangeArray* create_lookup_ranges(LookupSwitch* x);
  void do_SwitchRanges(SwitchRangeArray* x, LIR_Opr value, BlockBegin* default_sux);

#ifdef JFR_HAVE_INTRINSICS
  void do_getEventWriter(Intrinsic* x);
#endif

  void do_RuntimeCall(address routine, Intrinsic* x);

  ciKlass* profile_type(ciMethodData* md, int md_first_offset, int md_offset, intptr_t profiled_k,
                        Value arg, LIR_Opr& mdp, bool not_null, ciKlass* signature_at_call_k,
                        ciKlass* callee_signature_k);
  void profile_arguments(ProfileCall* x);
  void profile_parameters(Base* x);
  void profile_parameters_at_call(ProfileCall* x);
  LIR_Opr mask_boolean(LIR_Opr array, LIR_Opr value, CodeEmitInfo*& null_check_info);

 public:
  Compilation*  compilation() const              { return _compilation; }
  FrameMap*     frame_map() const                { return _compilation->frame_map(); }
  ciMethod*     method() const                   { return _method; }
  BlockBegin*   block() const                    { return _block; }
  IRScope*      scope() const                    { return block()->scope(); }

  int max_virtual_register_number() const        { return _virtual_register_number; }

  void block_do(BlockBegin* block);

  // Flags that can be set on vregs
  enum VregFlag {
      must_start_in_memory = 0  // needs to be assigned a memory location at beginning, but may then be loaded in a register
    , callee_saved     = 1    // must be in a callee saved register
    , byte_reg         = 2    // must be in a byte register
    , num_vreg_flags

  };

  LIRGenerator(Compilation* compilation, ciMethod* method)
    : _compilation(compilation)
    , _method(method)
    , _virtual_register_number(LIR_OprDesc::vreg_base)
    , _vreg_flags(num_vreg_flags)
    , _barrier_set(BarrierSet::barrier_set()->barrier_set_c1()) {
  }

  // for virtual registers, maps them back to Phi's or Local's
  Instruction* instruction_for_opr(LIR_Opr opr);
  Instruction* instruction_for_vreg(int reg_num);

  void set_vreg_flag   (int vreg_num, VregFlag f);
  bool is_vreg_flag_set(int vreg_num, VregFlag f);
  void set_vreg_flag   (LIR_Opr opr,  VregFlag f) { set_vreg_flag(opr->vreg_number(), f); }
  bool is_vreg_flag_set(LIR_Opr opr,  VregFlag f) { return is_vreg_flag_set(opr->vreg_number(), f); }

  // statics
  static LIR_Opr exceptionOopOpr();
  static LIR_Opr exceptionPcOpr();
  static LIR_Opr divInOpr();
  static LIR_Opr divOutOpr();
  static LIR_Opr remOutOpr();
#ifdef S390
  // On S390 we can do ldiv, lrem without RT call.
  static LIR_Opr ldivInOpr();
  static LIR_Opr ldivOutOpr();
  static LIR_Opr lremOutOpr();
#endif
  static LIR_Opr shiftCountOpr();
  LIR_Opr syncLockOpr();
  LIR_Opr syncTempOpr();
  LIR_Opr atomicLockOpr();

  // returns a register suitable for saving the thread in a
  // call_runtime_leaf if one is needed.
  LIR_Opr getThreadTemp();

  // visitor functionality
  virtual void do_Phi            (Phi*             x);
  virtual void do_Local          (Local*           x);
  virtual void do_Constant       (Constant*        x);
  virtual void do_LoadField      (LoadField*       x);
  virtual void do_StoreField     (StoreField*      x);
  virtual void do_ArrayLength    (ArrayLength*     x);
  virtual void do_LoadIndexed    (LoadIndexed*     x);
  virtual void do_StoreIndexed   (StoreIndexed*    x);
  virtual void do_NegateOp       (NegateOp*        x);
  virtual void do_ArithmeticOp   (ArithmeticOp*    x);
  virtual void do_ShiftOp        (ShiftOp*         x);
  virtual void do_LogicOp        (LogicOp*         x);
  virtual void do_CompareOp      (CompareOp*       x);
  virtual void do_IfOp           (IfOp*            x);
  virtual void do_Convert        (Convert*         x);
  virtual void do_NullCheck      (NullCheck*       x);
  virtual void do_TypeCast       (TypeCast*        x);
  virtual void do_Invoke         (Invoke*          x);
  virtual void do_NewInstance    (NewInstance*     x);
  virtual void do_NewTypeArray   (NewTypeArray*    x);
  virtual void do_NewObjectArray (NewObjectArray*  x);
  virtual void do_NewMultiArray  (NewMultiArray*   x);
  virtual void do_CheckCast      (CheckCast*       x);
  virtual void do_InstanceOf     (InstanceOf*      x);
  virtual void do_MonitorEnter   (MonitorEnter*    x);
  virtual void do_MonitorExit    (MonitorExit*     x);
  virtual void do_Intrinsic      (Intrinsic*       x);
  virtual void do_BlockBegin     (BlockBegin*      x);
  virtual void do_Goto           (Goto*            x);
  virtual void do_If             (If*              x);
  virtual void do_TableSwitch    (TableSwitch*     x);
  virtual void do_LookupSwitch   (LookupSwitch*    x);
  virtual void do_Return         (Return*          x);
  virtual void do_Throw          (Throw*           x);
  virtual void do_Base           (Base*            x);
  virtual void do_OsrEntry       (OsrEntry*        x);
  virtual void do_ExceptionObject(ExceptionObject* x);
  virtual void do_RoundFP        (RoundFP*         x);
  virtual void do_UnsafeGet      (UnsafeGet*       x);
  virtual void do_UnsafePut      (UnsafePut*       x);
  virtual void do_UnsafeGetAndSet(UnsafeGetAndSet* x);
  virtual void do_ProfileCall    (ProfileCall*     x);
  virtual void do_ProfileReturnType (ProfileReturnType* x);
  virtual void do_ProfileInvoke  (ProfileInvoke*   x);
  virtual void do_RuntimeCall    (RuntimeCall*     x);
  virtual void do_MemBar         (MemBar*          x);
  virtual void do_RangeCheckPredicate(RangeCheckPredicate* x);
#ifdef ASSERT
  virtual void do_Assert         (Assert*          x);
#endif

#ifdef C1_LIRGENERATOR_MD_HPP
#include C1_LIRGENERATOR_MD_HPP
#endif
};


class LIRItem: public CompilationResourceObj {
 private:
  Value         _value;
  LIRGenerator* _gen;
  LIR_Opr       _result;
  bool          _destroys_register;
  LIR_Opr       _new_result;

  LIRGenerator* gen() const { return _gen; }

 public:
  LIRItem(Value value, LIRGenerator* gen) {
    _destroys_register = false;
    _gen = gen;
    set_instruction(value);
  }

  LIRItem(LIRGenerator* gen) {
    _destroys_register = false;
    _gen = gen;
    _result = LIR_OprFact::illegalOpr;
    set_instruction(NULL);
  }

  void set_instruction(Value value) {
    _value = value;
    _result = LIR_OprFact::illegalOpr;
    if (_value != NULL) {
      _gen->walk(_value);
      _result = _value->operand();
    }
    _new_result = LIR_OprFact::illegalOpr;
  }

  Value value() const          { return _value;          }
  ValueType* type() const      { return value()->type(); }
  LIR_Opr result()             {
    assert(!_destroys_register || (!_result->is_register() || _result->is_virtual()),
           "shouldn't use set_destroys_register with physical regsiters");
    if (_destroys_register && _result->is_register()) {
      if (_new_result->is_illegal()) {
        _new_result = _gen->new_register(type());
        gen()->lir()->move(_result, _new_result);
      }
      return _new_result;
    } else {
      return _result;
    }
    return _result;
  }

  void set_result(LIR_Opr opr);

  void load_item();
  void load_byte_item();
  void load_nonconstant(S390_ONLY(int bits = 20));
  // load any values which can't be expressed as part of a single store instruction
  void load_for_store(BasicType store_type);
  void load_item_force(LIR_Opr reg);

  void dont_load_item() {
    // do nothing
  }

  void set_destroys_register() {
    _destroys_register = true;
  }

  bool is_constant() const { return value()->as_Constant() != NULL; }
  bool is_stack()          { return result()->is_stack(); }
  bool is_register()       { return result()->is_register(); }

  ciObject* get_jobject_constant() const;
  jint      get_jint_constant() const;
  jlong     get_jlong_constant() const;
  jfloat    get_jfloat_constant() const;
  jdouble   get_jdouble_constant() const;
  jint      get_address_constant() const;
};

#endif // SHARE_C1_C1_LIRGENERATOR_HPP
