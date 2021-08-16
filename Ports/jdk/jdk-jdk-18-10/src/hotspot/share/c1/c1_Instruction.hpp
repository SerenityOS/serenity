/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_C1_C1_INSTRUCTION_HPP
#define SHARE_C1_C1_INSTRUCTION_HPP

#include "c1/c1_Compilation.hpp"
#include "c1/c1_LIR.hpp"
#include "c1/c1_ValueType.hpp"
#include "ci/ciField.hpp"

// Predefined classes
class ciField;
class ValueStack;
class InstructionPrinter;
class IRScope;
class LIR_OprDesc;
typedef LIR_OprDesc* LIR_Opr;


// Instruction class hierarchy
//
// All leaf classes in the class hierarchy are concrete classes
// (i.e., are instantiated). All other classes are abstract and
// serve factoring.

class Instruction;
class   Phi;
class   Local;
class   Constant;
class   AccessField;
class     LoadField;
class     StoreField;
class   AccessArray;
class     ArrayLength;
class     AccessIndexed;
class       LoadIndexed;
class       StoreIndexed;
class   NegateOp;
class   Op2;
class     ArithmeticOp;
class     ShiftOp;
class     LogicOp;
class     CompareOp;
class     IfOp;
class   Convert;
class   NullCheck;
class   TypeCast;
class   OsrEntry;
class   ExceptionObject;
class   StateSplit;
class     Invoke;
class     NewInstance;
class     NewArray;
class       NewTypeArray;
class       NewObjectArray;
class       NewMultiArray;
class     TypeCheck;
class       CheckCast;
class       InstanceOf;
class     AccessMonitor;
class       MonitorEnter;
class       MonitorExit;
class     Intrinsic;
class     BlockBegin;
class     BlockEnd;
class       Goto;
class       If;
class       Switch;
class         TableSwitch;
class         LookupSwitch;
class       Return;
class       Throw;
class       Base;
class   RoundFP;
class   UnsafeOp;
class     UnsafeGet;
class     UnsafePut;
class     UnsafeGetAndSet;
class   ProfileCall;
class   ProfileReturnType;
class   ProfileInvoke;
class   RuntimeCall;
class   MemBar;
class   RangeCheckPredicate;
#ifdef ASSERT
class   Assert;
#endif

// A Value is a reference to the instruction creating the value
typedef Instruction* Value;
typedef GrowableArray<Value> Values;
typedef GrowableArray<ValueStack*> ValueStackStack;

// BlockClosure is the base class for block traversal/iteration.

class BlockClosure: public CompilationResourceObj {
 public:
  virtual void block_do(BlockBegin* block)       = 0;
};


// A simple closure class for visiting the values of an Instruction
class ValueVisitor: public StackObj {
 public:
  virtual void visit(Value* v) = 0;
};


// Some array and list classes
typedef GrowableArray<BlockBegin*> BlockBeginArray;

class BlockList: public GrowableArray<BlockBegin*> {
 public:
  BlockList(): GrowableArray<BlockBegin*>() {}
  BlockList(const int size): GrowableArray<BlockBegin*>(size) {}
  BlockList(const int size, BlockBegin* init): GrowableArray<BlockBegin*>(size, size, init) {}

  void iterate_forward(BlockClosure* closure);
  void iterate_backward(BlockClosure* closure);
  void blocks_do(void f(BlockBegin*));
  void values_do(ValueVisitor* f);
  void print(bool cfg_only = false, bool live_only = false) PRODUCT_RETURN;
};


// InstructionVisitors provide type-based dispatch for instructions.
// For each concrete Instruction class X, a virtual function do_X is
// provided. Functionality that needs to be implemented for all classes
// (e.g., printing, code generation) is factored out into a specialised
// visitor instead of added to the Instruction classes itself.

class InstructionVisitor: public StackObj {
 public:
  virtual void do_Phi            (Phi*             x) = 0;
  virtual void do_Local          (Local*           x) = 0;
  virtual void do_Constant       (Constant*        x) = 0;
  virtual void do_LoadField      (LoadField*       x) = 0;
  virtual void do_StoreField     (StoreField*      x) = 0;
  virtual void do_ArrayLength    (ArrayLength*     x) = 0;
  virtual void do_LoadIndexed    (LoadIndexed*     x) = 0;
  virtual void do_StoreIndexed   (StoreIndexed*    x) = 0;
  virtual void do_NegateOp       (NegateOp*        x) = 0;
  virtual void do_ArithmeticOp   (ArithmeticOp*    x) = 0;
  virtual void do_ShiftOp        (ShiftOp*         x) = 0;
  virtual void do_LogicOp        (LogicOp*         x) = 0;
  virtual void do_CompareOp      (CompareOp*       x) = 0;
  virtual void do_IfOp           (IfOp*            x) = 0;
  virtual void do_Convert        (Convert*         x) = 0;
  virtual void do_NullCheck      (NullCheck*       x) = 0;
  virtual void do_TypeCast       (TypeCast*        x) = 0;
  virtual void do_Invoke         (Invoke*          x) = 0;
  virtual void do_NewInstance    (NewInstance*     x) = 0;
  virtual void do_NewTypeArray   (NewTypeArray*    x) = 0;
  virtual void do_NewObjectArray (NewObjectArray*  x) = 0;
  virtual void do_NewMultiArray  (NewMultiArray*   x) = 0;
  virtual void do_CheckCast      (CheckCast*       x) = 0;
  virtual void do_InstanceOf     (InstanceOf*      x) = 0;
  virtual void do_MonitorEnter   (MonitorEnter*    x) = 0;
  virtual void do_MonitorExit    (MonitorExit*     x) = 0;
  virtual void do_Intrinsic      (Intrinsic*       x) = 0;
  virtual void do_BlockBegin     (BlockBegin*      x) = 0;
  virtual void do_Goto           (Goto*            x) = 0;
  virtual void do_If             (If*              x) = 0;
  virtual void do_TableSwitch    (TableSwitch*     x) = 0;
  virtual void do_LookupSwitch   (LookupSwitch*    x) = 0;
  virtual void do_Return         (Return*          x) = 0;
  virtual void do_Throw          (Throw*           x) = 0;
  virtual void do_Base           (Base*            x) = 0;
  virtual void do_OsrEntry       (OsrEntry*        x) = 0;
  virtual void do_ExceptionObject(ExceptionObject* x) = 0;
  virtual void do_RoundFP        (RoundFP*         x) = 0;
  virtual void do_UnsafeGet      (UnsafeGet*       x) = 0;
  virtual void do_UnsafePut      (UnsafePut*       x) = 0;
  virtual void do_UnsafeGetAndSet(UnsafeGetAndSet* x) = 0;
  virtual void do_ProfileCall    (ProfileCall*     x) = 0;
  virtual void do_ProfileReturnType (ProfileReturnType*  x) = 0;
  virtual void do_ProfileInvoke  (ProfileInvoke*   x) = 0;
  virtual void do_RuntimeCall    (RuntimeCall*     x) = 0;
  virtual void do_MemBar         (MemBar*          x) = 0;
  virtual void do_RangeCheckPredicate(RangeCheckPredicate* x) = 0;
#ifdef ASSERT
  virtual void do_Assert         (Assert*          x) = 0;
#endif
};


// Hashing support
//
// Note: This hash functions affect the performance
//       of ValueMap - make changes carefully!

#define HASH1(x1            )                    ((intx)(x1))
#define HASH2(x1, x2        )                    ((HASH1(x1        ) << 7) ^ HASH1(x2))
#define HASH3(x1, x2, x3    )                    ((HASH2(x1, x2    ) << 7) ^ HASH1(x3))
#define HASH4(x1, x2, x3, x4)                    ((HASH3(x1, x2, x3) << 7) ^ HASH1(x4))


// The following macros are used to implement instruction-specific hashing.
// By default, each instruction implements hash() and is_equal(Value), used
// for value numbering/common subexpression elimination. The default imple-
// mentation disables value numbering. Each instruction which can be value-
// numbered, should define corresponding hash() and is_equal(Value) functions
// via the macros below. The f arguments specify all the values/op codes, etc.
// that need to be identical for two instructions to be identical.
//
// Note: The default implementation of hash() returns 0 in order to indicate
//       that the instruction should not be considered for value numbering.
//       The currently used hash functions do not guarantee that never a 0
//       is produced. While this is still correct, it may be a performance
//       bug (no value numbering for that node). However, this situation is
//       so unlikely, that we are not going to handle it specially.

#define HASHING1(class_name, enabled, f1)             \
  virtual intx hash() const {                         \
    return (enabled) ? HASH2(name(), f1) : 0;         \
  }                                                   \
  virtual bool is_equal(Value v) const {              \
    if (!(enabled)  ) return false;                   \
    class_name* _v = v->as_##class_name();            \
    if (_v == NULL  ) return false;                   \
    if (f1 != _v->f1) return false;                   \
    return true;                                      \
  }                                                   \


#define HASHING2(class_name, enabled, f1, f2)         \
  virtual intx hash() const {                         \
    return (enabled) ? HASH3(name(), f1, f2) : 0;     \
  }                                                   \
  virtual bool is_equal(Value v) const {              \
    if (!(enabled)  ) return false;                   \
    class_name* _v = v->as_##class_name();            \
    if (_v == NULL  ) return false;                   \
    if (f1 != _v->f1) return false;                   \
    if (f2 != _v->f2) return false;                   \
    return true;                                      \
  }                                                   \


#define HASHING3(class_name, enabled, f1, f2, f3)     \
  virtual intx hash() const {                         \
    return (enabled) ? HASH4(name(), f1, f2, f3) : 0; \
  }                                                   \
  virtual bool is_equal(Value v) const {              \
    if (!(enabled)  ) return false;                   \
    class_name* _v = v->as_##class_name();            \
    if (_v == NULL  ) return false;                   \
    if (f1 != _v->f1) return false;                   \
    if (f2 != _v->f2) return false;                   \
    if (f3 != _v->f3) return false;                   \
    return true;                                      \
  }                                                   \


// The mother of all instructions...

class Instruction: public CompilationResourceObj {
 private:
  int          _id;                              // the unique instruction id
#ifndef PRODUCT
  int          _printable_bci;                   // the bci of the instruction for printing
#endif
  int          _use_count;                       // the number of instructions refering to this value (w/o prev/next); only roots can have use count = 0 or > 1
  int          _pin_state;                       // set of PinReason describing the reason for pinning
  ValueType*   _type;                            // the instruction value type
  Instruction* _next;                            // the next instruction if any (NULL for BlockEnd instructions)
  Instruction* _subst;                           // the substitution instruction if any
  LIR_Opr      _operand;                         // LIR specific information
  unsigned int _flags;                           // Flag bits

  ValueStack*  _state_before;                    // Copy of state with input operands still on stack (or NULL)
  ValueStack*  _exception_state;                 // Copy of state for exception handling
  XHandlers*   _exception_handlers;              // Flat list of exception handlers covering this instruction

  friend class UseCountComputer;

  void update_exception_state(ValueStack* state);

 protected:
  BlockBegin*  _block;                           // Block that contains this instruction

  void set_type(ValueType* type) {
    assert(type != NULL, "type must exist");
    _type = type;
  }

  // Helper class to keep track of which arguments need a null check
  class ArgsNonNullState {
  private:
    int _nonnull_state; // mask identifying which args are nonnull
  public:
    ArgsNonNullState()
      : _nonnull_state(AllBits) {}

    // Does argument number i needs a null check?
    bool arg_needs_null_check(int i) const {
      // No data is kept for arguments starting at position 33 so
      // conservatively assume that they need a null check.
      if (i >= 0 && i < (int)sizeof(_nonnull_state) * BitsPerByte) {
        return is_set_nth_bit(_nonnull_state, i);
      }
      return true;
    }

    // Set whether argument number i needs a null check or not
    void set_arg_needs_null_check(int i, bool check) {
      if (i >= 0 && i < (int)sizeof(_nonnull_state) * BitsPerByte) {
        if (check) {
          _nonnull_state |= nth_bit(i);
        } else {
          _nonnull_state &= ~(nth_bit(i));
        }
      }
    }
  };

 public:
  void* operator new(size_t size) throw() {
    Compilation* c = Compilation::current();
    void* res = c->arena()->Amalloc(size);
    return res;
  }

  static const int no_bci = -99;

  enum InstructionFlag {
    NeedsNullCheckFlag = 0,
    CanTrapFlag,
    DirectCompareFlag,
    IsEliminatedFlag,
    IsSafepointFlag,
    IsStaticFlag,
    NeedsStoreCheckFlag,
    NeedsWriteBarrierFlag,
    PreservesStateFlag,
    TargetIsFinalFlag,
    TargetIsLoadedFlag,
    UnorderedIsTrueFlag,
    NeedsPatchingFlag,
    ThrowIncompatibleClassChangeErrorFlag,
    InvokeSpecialReceiverCheckFlag,
    ProfileMDOFlag,
    IsLinkedInBlockFlag,
    NeedsRangeCheckFlag,
    InWorkListFlag,
    DeoptimizeOnException,
    InstructionLastFlag
  };

 public:
  bool check_flag(InstructionFlag id) const      { return (_flags & (1 << id)) != 0;    }
  void set_flag(InstructionFlag id, bool f)      { _flags = f ? (_flags | (1 << id)) : (_flags & ~(1 << id)); };

  // 'globally' used condition values
  enum Condition {
    eql, neq, lss, leq, gtr, geq, aeq, beq
  };

  // Instructions may be pinned for many reasons and under certain conditions
  // with enough knowledge it's possible to safely unpin them.
  enum PinReason {
      PinUnknown           = 1 << 0
    , PinExplicitNullCheck = 1 << 3
    , PinStackForStateSplit= 1 << 12
    , PinStateSplitConstructor= 1 << 13
    , PinGlobalValueNumbering= 1 << 14
  };

  static Condition mirror(Condition cond);
  static Condition negate(Condition cond);

  // initialization
  static int number_of_instructions() {
    return Compilation::current()->number_of_instructions();
  }

  // creation
  Instruction(ValueType* type, ValueStack* state_before = NULL, bool type_is_constant = false)
  : _id(Compilation::current()->get_next_id()),
#ifndef PRODUCT
  _printable_bci(-99),
#endif
    _use_count(0)
  , _pin_state(0)
  , _type(type)
  , _next(NULL)
  , _subst(NULL)
  , _operand(LIR_OprFact::illegalOpr)
  , _flags(0)
  , _state_before(state_before)
  , _exception_handlers(NULL)
  , _block(NULL)
  {
    check_state(state_before);
    assert(type != NULL && (!type->is_constant() || type_is_constant), "type must exist");
    update_exception_state(_state_before);
  }

  // accessors
  int id() const                                 { return _id; }
#ifndef PRODUCT
  bool has_printable_bci() const                 { return _printable_bci != -99; }
  int printable_bci() const                      { assert(has_printable_bci(), "_printable_bci should have been set"); return _printable_bci; }
  void set_printable_bci(int bci)                { _printable_bci = bci; }
#endif
  int dominator_depth();
  int use_count() const                          { return _use_count; }
  int pin_state() const                          { return _pin_state; }
  bool is_pinned() const                         { return _pin_state != 0 || PinAllInstructions; }
  ValueType* type() const                        { return _type; }
  BlockBegin *block() const                      { return _block; }
  Instruction* prev();                           // use carefully, expensive operation
  Instruction* next() const                      { return _next; }
  bool has_subst() const                         { return _subst != NULL; }
  Instruction* subst()                           { return _subst == NULL ? this : _subst->subst(); }
  LIR_Opr operand() const                        { return _operand; }

  void set_needs_null_check(bool f)              { set_flag(NeedsNullCheckFlag, f); }
  bool needs_null_check() const                  { return check_flag(NeedsNullCheckFlag); }
  bool is_linked() const                         { return check_flag(IsLinkedInBlockFlag); }
  bool can_be_linked()                           { return as_Local() == NULL && as_Phi() == NULL; }

  bool is_null_obj()                             { return as_Constant() != NULL && type()->as_ObjectType()->constant_value()->is_null_object(); }

  bool has_uses() const                          { return use_count() > 0; }
  ValueStack* state_before() const               { return _state_before; }
  ValueStack* exception_state() const            { return _exception_state; }
  virtual bool needs_exception_state() const     { return true; }
  XHandlers* exception_handlers() const          { return _exception_handlers; }

  // manipulation
  void pin(PinReason reason)                     { _pin_state |= reason; }
  void pin()                                     { _pin_state |= PinUnknown; }
  // DANGEROUS: only used by EliminateStores
  void unpin(PinReason reason)                   { assert((reason & PinUnknown) == 0, "can't unpin unknown state"); _pin_state &= ~reason; }

  Instruction* set_next(Instruction* next) {
    assert(next->has_printable_bci(), "_printable_bci should have been set");
    assert(next != NULL, "must not be NULL");
    assert(as_BlockEnd() == NULL, "BlockEnd instructions must have no next");
    assert(next->can_be_linked(), "shouldn't link these instructions into list");

    BlockBegin *block = this->block();
    next->_block = block;

    next->set_flag(Instruction::IsLinkedInBlockFlag, true);
    _next = next;
    return next;
  }

  Instruction* set_next(Instruction* next, int bci) {
#ifndef PRODUCT
    next->set_printable_bci(bci);
#endif
    return set_next(next);
  }

  // when blocks are merged
  void fixup_block_pointers() {
    Instruction *cur = next()->next(); // next()'s block is set in set_next
    while (cur && cur->_block != block()) {
      cur->_block = block();
      cur = cur->next();
    }
  }

  Instruction *insert_after(Instruction *i) {
    Instruction* n = _next;
    set_next(i);
    i->set_next(n);
    return _next;
  }

  Instruction *insert_after_same_bci(Instruction *i) {
#ifndef PRODUCT
    i->set_printable_bci(printable_bci());
#endif
    return insert_after(i);
  }

  void set_subst(Instruction* subst)             {
    assert(subst == NULL ||
           type()->base() == subst->type()->base() ||
           subst->type()->base() == illegalType, "type can't change");
    _subst = subst;
  }
  void set_exception_handlers(XHandlers *xhandlers) { _exception_handlers = xhandlers; }
  void set_exception_state(ValueStack* s)        { check_state(s); _exception_state = s; }
  void set_state_before(ValueStack* s)           { check_state(s); _state_before = s; }

  // machine-specifics
  void set_operand(LIR_Opr operand)              { assert(operand != LIR_OprFact::illegalOpr, "operand must exist"); _operand = operand; }
  void clear_operand()                           { _operand = LIR_OprFact::illegalOpr; }

  // generic
  virtual Instruction*      as_Instruction()     { return this; } // to satisfy HASHING1 macro
  virtual Phi*              as_Phi()             { return NULL; }
  virtual Local*            as_Local()           { return NULL; }
  virtual Constant*         as_Constant()        { return NULL; }
  virtual AccessField*      as_AccessField()     { return NULL; }
  virtual LoadField*        as_LoadField()       { return NULL; }
  virtual StoreField*       as_StoreField()      { return NULL; }
  virtual AccessArray*      as_AccessArray()     { return NULL; }
  virtual ArrayLength*      as_ArrayLength()     { return NULL; }
  virtual AccessIndexed*    as_AccessIndexed()   { return NULL; }
  virtual LoadIndexed*      as_LoadIndexed()     { return NULL; }
  virtual StoreIndexed*     as_StoreIndexed()    { return NULL; }
  virtual NegateOp*         as_NegateOp()        { return NULL; }
  virtual Op2*              as_Op2()             { return NULL; }
  virtual ArithmeticOp*     as_ArithmeticOp()    { return NULL; }
  virtual ShiftOp*          as_ShiftOp()         { return NULL; }
  virtual LogicOp*          as_LogicOp()         { return NULL; }
  virtual CompareOp*        as_CompareOp()       { return NULL; }
  virtual IfOp*             as_IfOp()            { return NULL; }
  virtual Convert*          as_Convert()         { return NULL; }
  virtual NullCheck*        as_NullCheck()       { return NULL; }
  virtual OsrEntry*         as_OsrEntry()        { return NULL; }
  virtual StateSplit*       as_StateSplit()      { return NULL; }
  virtual Invoke*           as_Invoke()          { return NULL; }
  virtual NewInstance*      as_NewInstance()     { return NULL; }
  virtual NewArray*         as_NewArray()        { return NULL; }
  virtual NewTypeArray*     as_NewTypeArray()    { return NULL; }
  virtual NewObjectArray*   as_NewObjectArray()  { return NULL; }
  virtual NewMultiArray*    as_NewMultiArray()   { return NULL; }
  virtual TypeCheck*        as_TypeCheck()       { return NULL; }
  virtual CheckCast*        as_CheckCast()       { return NULL; }
  virtual InstanceOf*       as_InstanceOf()      { return NULL; }
  virtual TypeCast*         as_TypeCast()        { return NULL; }
  virtual AccessMonitor*    as_AccessMonitor()   { return NULL; }
  virtual MonitorEnter*     as_MonitorEnter()    { return NULL; }
  virtual MonitorExit*      as_MonitorExit()     { return NULL; }
  virtual Intrinsic*        as_Intrinsic()       { return NULL; }
  virtual BlockBegin*       as_BlockBegin()      { return NULL; }
  virtual BlockEnd*         as_BlockEnd()        { return NULL; }
  virtual Goto*             as_Goto()            { return NULL; }
  virtual If*               as_If()              { return NULL; }
  virtual TableSwitch*      as_TableSwitch()     { return NULL; }
  virtual LookupSwitch*     as_LookupSwitch()    { return NULL; }
  virtual Return*           as_Return()          { return NULL; }
  virtual Throw*            as_Throw()           { return NULL; }
  virtual Base*             as_Base()            { return NULL; }
  virtual RoundFP*          as_RoundFP()         { return NULL; }
  virtual ExceptionObject*  as_ExceptionObject() { return NULL; }
  virtual UnsafeOp*         as_UnsafeOp()        { return NULL; }
  virtual ProfileInvoke*    as_ProfileInvoke()   { return NULL; }
  virtual RangeCheckPredicate* as_RangeCheckPredicate() { return NULL; }

#ifdef ASSERT
  virtual Assert*           as_Assert()          { return NULL; }
#endif

  virtual void visit(InstructionVisitor* v)      = 0;

  virtual bool can_trap() const                  { return false; }

  virtual void input_values_do(ValueVisitor* f)   = 0;
  virtual void state_values_do(ValueVisitor* f);
  virtual void other_values_do(ValueVisitor* f)   { /* usually no other - override on demand */ }
          void       values_do(ValueVisitor* f)   { input_values_do(f); state_values_do(f); other_values_do(f); }

  virtual ciType* exact_type() const;
  virtual ciType* declared_type() const          { return NULL; }

  // hashing
  virtual const char* name() const               = 0;
  HASHING1(Instruction, false, id())             // hashing disabled by default

  // debugging
  static void check_state(ValueStack* state)     PRODUCT_RETURN;
  void print()                                   PRODUCT_RETURN;
  void print_line()                              PRODUCT_RETURN;
  void print(InstructionPrinter& ip)             PRODUCT_RETURN;
};


// The following macros are used to define base (i.e., non-leaf)
// and leaf instruction classes. They define class-name related
// generic functionality in one place.

#define BASE(class_name, super_class_name)       \
  class class_name: public super_class_name {    \
   public:                                       \
    virtual class_name* as_##class_name()        { return this; }              \


#define LEAF(class_name, super_class_name)       \
  BASE(class_name, super_class_name)             \
   public:                                       \
    virtual const char* name() const             { return #class_name; }       \
    virtual void visit(InstructionVisitor* v)    { v->do_##class_name(this); } \


// Debugging support


#ifdef ASSERT
class AssertValues: public ValueVisitor {
  void visit(Value* x)             { assert((*x) != NULL, "value must exist"); }
};
  #define ASSERT_VALUES                          { AssertValues assert_value; values_do(&assert_value); }
#else
  #define ASSERT_VALUES
#endif // ASSERT


// A Phi is a phi function in the sense of SSA form. It stands for
// the value of a local variable at the beginning of a join block.
// A Phi consists of n operands, one for every incoming branch.

LEAF(Phi, Instruction)
 private:
  int         _pf_flags; // the flags of the phi function
  int         _index;    // to value on operand stack (index < 0) or to local
 public:
  // creation
  Phi(ValueType* type, BlockBegin* b, int index)
  : Instruction(type->base())
  , _pf_flags(0)
  , _index(index)
  {
    _block = b;
    NOT_PRODUCT(set_printable_bci(Value(b)->printable_bci()));
    if (type->is_illegal()) {
      make_illegal();
    }
  }

  // flags
  enum Flag {
    no_flag         = 0,
    visited         = 1 << 0,
    cannot_simplify = 1 << 1
  };

  // accessors
  bool  is_local() const          { return _index >= 0; }
  bool  is_on_stack() const       { return !is_local(); }
  int   local_index() const       { assert(is_local(), ""); return _index; }
  int   stack_index() const       { assert(is_on_stack(), ""); return -(_index+1); }

  Value operand_at(int i) const;
  int   operand_count() const;

  void   set(Flag f)              { _pf_flags |=  f; }
  void   clear(Flag f)            { _pf_flags &= ~f; }
  bool   is_set(Flag f) const     { return (_pf_flags & f) != 0; }

  // Invalidates phis corresponding to merges of locals of two different types
  // (these should never be referenced, otherwise the bytecodes are illegal)
  void   make_illegal() {
    set(cannot_simplify);
    set_type(illegalType);
  }

  bool is_illegal() const {
    return type()->is_illegal();
  }

  // generic
  virtual void input_values_do(ValueVisitor* f) {
  }
};


// A local is a placeholder for an incoming argument to a function call.
LEAF(Local, Instruction)
 private:
  int      _java_index;                          // the local index within the method to which the local belongs
  bool     _is_receiver;                         // if local variable holds the receiver: "this" for non-static methods
  ciType*  _declared_type;
 public:
  // creation
  Local(ciType* declared, ValueType* type, int index, bool receiver)
    : Instruction(type)
    , _java_index(index)
    , _is_receiver(receiver)
    , _declared_type(declared)
  {
    NOT_PRODUCT(set_printable_bci(-1));
  }

  // accessors
  int java_index() const                         { return _java_index; }
  bool is_receiver() const                       { return _is_receiver; }

  virtual ciType* declared_type() const          { return _declared_type; }

  // generic
  virtual void input_values_do(ValueVisitor* f)   { /* no values */ }
};


LEAF(Constant, Instruction)
 public:
  // creation
  Constant(ValueType* type):
      Instruction(type, NULL, /*type_is_constant*/ true)
  {
    assert(type->is_constant(), "must be a constant");
  }

  Constant(ValueType* type, ValueStack* state_before):
    Instruction(type, state_before, /*type_is_constant*/ true)
  {
    assert(state_before != NULL, "only used for constants which need patching");
    assert(type->is_constant(), "must be a constant");
    // since it's patching it needs to be pinned
    pin();
  }

  // generic
  virtual bool can_trap() const                  { return state_before() != NULL; }
  virtual void input_values_do(ValueVisitor* f)   { /* no values */ }

  virtual intx hash() const;
  virtual bool is_equal(Value v) const;

  virtual ciType* exact_type() const;

  enum CompareResult { not_comparable = -1, cond_false, cond_true };

  virtual CompareResult compare(Instruction::Condition condition, Value right) const;
  BlockBegin* compare(Instruction::Condition cond, Value right,
                      BlockBegin* true_sux, BlockBegin* false_sux) const {
    switch (compare(cond, right)) {
    case not_comparable:
      return NULL;
    case cond_false:
      return false_sux;
    case cond_true:
      return true_sux;
    default:
      ShouldNotReachHere();
      return NULL;
    }
  }
};


BASE(AccessField, Instruction)
 private:
  Value       _obj;
  int         _offset;
  ciField*    _field;
  NullCheck*  _explicit_null_check;              // For explicit null check elimination

 public:
  // creation
  AccessField(Value obj, int offset, ciField* field, bool is_static,
              ValueStack* state_before, bool needs_patching)
  : Instruction(as_ValueType(field->type()->basic_type()), state_before)
  , _obj(obj)
  , _offset(offset)
  , _field(field)
  , _explicit_null_check(NULL)
  {
    set_needs_null_check(!is_static);
    set_flag(IsStaticFlag, is_static);
    set_flag(NeedsPatchingFlag, needs_patching);
    ASSERT_VALUES
    // pin of all instructions with memory access
    pin();
  }

  // accessors
  Value obj() const                              { return _obj; }
  int offset() const                             { return _offset; }
  ciField* field() const                         { return _field; }
  BasicType field_type() const                   { return _field->type()->basic_type(); }
  bool is_static() const                         { return check_flag(IsStaticFlag); }
  NullCheck* explicit_null_check() const         { return _explicit_null_check; }
  bool needs_patching() const                    { return check_flag(NeedsPatchingFlag); }

  // Unresolved getstatic and putstatic can cause initialization.
  // Technically it occurs at the Constant that materializes the base
  // of the static fields but it's simpler to model it here.
  bool is_init_point() const                     { return is_static() && (needs_patching() || !_field->holder()->is_initialized()); }

  // manipulation

  // Under certain circumstances, if a previous NullCheck instruction
  // proved the target object non-null, we can eliminate the explicit
  // null check and do an implicit one, simply specifying the debug
  // information from the NullCheck. This field should only be consulted
  // if needs_null_check() is true.
  void set_explicit_null_check(NullCheck* check) { _explicit_null_check = check; }

  // generic
  virtual bool can_trap() const                  { return needs_null_check() || needs_patching(); }
  virtual void input_values_do(ValueVisitor* f)   { f->visit(&_obj); }
};


LEAF(LoadField, AccessField)
 public:
  // creation
  LoadField(Value obj, int offset, ciField* field, bool is_static,
            ValueStack* state_before, bool needs_patching)
  : AccessField(obj, offset, field, is_static, state_before, needs_patching)
  {}

  ciType* declared_type() const;

  // generic; cannot be eliminated if needs patching or if volatile.
  HASHING3(LoadField, !needs_patching() && !field()->is_volatile(), obj()->subst(), offset(), declared_type())
};


LEAF(StoreField, AccessField)
 private:
  Value _value;

 public:
  // creation
  StoreField(Value obj, int offset, ciField* field, Value value, bool is_static,
             ValueStack* state_before, bool needs_patching)
  : AccessField(obj, offset, field, is_static, state_before, needs_patching)
  , _value(value)
  {
    set_flag(NeedsWriteBarrierFlag, as_ValueType(field_type())->is_object());
    ASSERT_VALUES
    pin();
  }

  // accessors
  Value value() const                            { return _value; }
  bool needs_write_barrier() const               { return check_flag(NeedsWriteBarrierFlag); }

  // generic
  virtual void input_values_do(ValueVisitor* f)   { AccessField::input_values_do(f); f->visit(&_value); }
};


BASE(AccessArray, Instruction)
 private:
  Value       _array;

 public:
  // creation
  AccessArray(ValueType* type, Value array, ValueStack* state_before)
  : Instruction(type, state_before)
  , _array(array)
  {
    set_needs_null_check(true);
    ASSERT_VALUES
    pin(); // instruction with side effect (null exception or range check throwing)
  }

  Value array() const                            { return _array; }

  // generic
  virtual bool can_trap() const                  { return needs_null_check(); }
  virtual void input_values_do(ValueVisitor* f)   { f->visit(&_array); }
};


LEAF(ArrayLength, AccessArray)
 private:
  NullCheck*  _explicit_null_check;              // For explicit null check elimination

 public:
  // creation
  ArrayLength(Value array, ValueStack* state_before)
  : AccessArray(intType, array, state_before)
  , _explicit_null_check(NULL) {}

  // accessors
  NullCheck* explicit_null_check() const         { return _explicit_null_check; }

  // setters
  // See LoadField::set_explicit_null_check for documentation
  void set_explicit_null_check(NullCheck* check) { _explicit_null_check = check; }

  // generic
  HASHING1(ArrayLength, true, array()->subst())
};


BASE(AccessIndexed, AccessArray)
 private:
  Value     _index;
  Value     _length;
  BasicType _elt_type;
  bool      _mismatched;

 public:
  // creation
  AccessIndexed(Value array, Value index, Value length, BasicType elt_type, ValueStack* state_before, bool mismatched)
  : AccessArray(as_ValueType(elt_type), array, state_before)
  , _index(index)
  , _length(length)
  , _elt_type(elt_type)
  , _mismatched(mismatched)
  {
    set_flag(Instruction::NeedsRangeCheckFlag, true);
    ASSERT_VALUES
  }

  // accessors
  Value index() const                            { return _index; }
  Value length() const                           { return _length; }
  BasicType elt_type() const                     { return _elt_type; }
  bool mismatched() const                        { return _mismatched; }

  void clear_length()                            { _length = NULL; }
  // perform elimination of range checks involving constants
  bool compute_needs_range_check();

  // generic
  virtual void input_values_do(ValueVisitor* f)   { AccessArray::input_values_do(f); f->visit(&_index); if (_length != NULL) f->visit(&_length); }
};


LEAF(LoadIndexed, AccessIndexed)
 private:
  NullCheck*  _explicit_null_check;              // For explicit null check elimination

 public:
  // creation
  LoadIndexed(Value array, Value index, Value length, BasicType elt_type, ValueStack* state_before, bool mismatched = false)
  : AccessIndexed(array, index, length, elt_type, state_before, mismatched)
  , _explicit_null_check(NULL) {}

  // accessors
  NullCheck* explicit_null_check() const         { return _explicit_null_check; }

  // setters
  // See LoadField::set_explicit_null_check for documentation
  void set_explicit_null_check(NullCheck* check) { _explicit_null_check = check; }

  ciType* exact_type() const;
  ciType* declared_type() const;

  // generic;
  HASHING3(LoadIndexed, true, type()->tag(), array()->subst(), index()->subst())
};


LEAF(StoreIndexed, AccessIndexed)
 private:
  Value       _value;

  ciMethod* _profiled_method;
  int       _profiled_bci;
  bool      _check_boolean;

 public:
  // creation
  StoreIndexed(Value array, Value index, Value length, BasicType elt_type, Value value, ValueStack* state_before,
               bool check_boolean, bool mismatched = false)
  : AccessIndexed(array, index, length, elt_type, state_before, mismatched)
  , _value(value), _profiled_method(NULL), _profiled_bci(0), _check_boolean(check_boolean)
  {
    set_flag(NeedsWriteBarrierFlag, (as_ValueType(elt_type)->is_object()));
    set_flag(NeedsStoreCheckFlag, (as_ValueType(elt_type)->is_object()));
    ASSERT_VALUES
    pin();
  }

  // accessors
  Value value() const                            { return _value; }
  bool needs_write_barrier() const               { return check_flag(NeedsWriteBarrierFlag); }
  bool needs_store_check() const                 { return check_flag(NeedsStoreCheckFlag); }
  bool check_boolean() const                     { return _check_boolean; }
  // Helpers for MethodData* profiling
  void set_should_profile(bool value)                { set_flag(ProfileMDOFlag, value); }
  void set_profiled_method(ciMethod* method)         { _profiled_method = method;   }
  void set_profiled_bci(int bci)                     { _profiled_bci = bci;         }
  bool      should_profile() const                   { return check_flag(ProfileMDOFlag); }
  ciMethod* profiled_method() const                  { return _profiled_method;     }
  int       profiled_bci() const                     { return _profiled_bci;        }
  // generic
  virtual void input_values_do(ValueVisitor* f)   { AccessIndexed::input_values_do(f); f->visit(&_value); }
};


LEAF(NegateOp, Instruction)
 private:
  Value _x;

 public:
  // creation
  NegateOp(Value x) : Instruction(x->type()->base()), _x(x) {
    ASSERT_VALUES
  }

  // accessors
  Value x() const                                { return _x; }

  // generic
  virtual void input_values_do(ValueVisitor* f)   { f->visit(&_x); }
};


BASE(Op2, Instruction)
 private:
  Bytecodes::Code _op;
  Value           _x;
  Value           _y;

 public:
  // creation
  Op2(ValueType* type, Bytecodes::Code op, Value x, Value y, ValueStack* state_before = NULL)
  : Instruction(type, state_before)
  , _op(op)
  , _x(x)
  , _y(y)
  {
    ASSERT_VALUES
  }

  // accessors
  Bytecodes::Code op() const                     { return _op; }
  Value x() const                                { return _x; }
  Value y() const                                { return _y; }

  // manipulators
  void swap_operands() {
    assert(is_commutative(), "operation must be commutative");
    Value t = _x; _x = _y; _y = t;
  }

  // generic
  virtual bool is_commutative() const            { return false; }
  virtual void input_values_do(ValueVisitor* f)   { f->visit(&_x); f->visit(&_y); }
};


LEAF(ArithmeticOp, Op2)
 public:
  // creation
  ArithmeticOp(Bytecodes::Code op, Value x, Value y, ValueStack* state_before)
  : Op2(x->type()->meet(y->type()), op, x, y, state_before)
  {
    if (can_trap()) pin();
  }

  // generic
  virtual bool is_commutative() const;
  virtual bool can_trap() const;
  HASHING3(Op2, true, op(), x()->subst(), y()->subst())
};


LEAF(ShiftOp, Op2)
 public:
  // creation
  ShiftOp(Bytecodes::Code op, Value x, Value s) : Op2(x->type()->base(), op, x, s) {}

  // generic
  HASHING3(Op2, true, op(), x()->subst(), y()->subst())
};


LEAF(LogicOp, Op2)
 public:
  // creation
  LogicOp(Bytecodes::Code op, Value x, Value y) : Op2(x->type()->meet(y->type()), op, x, y) {}

  // generic
  virtual bool is_commutative() const;
  HASHING3(Op2, true, op(), x()->subst(), y()->subst())
};


LEAF(CompareOp, Op2)
 public:
  // creation
  CompareOp(Bytecodes::Code op, Value x, Value y, ValueStack* state_before)
  : Op2(intType, op, x, y, state_before)
  {}

  // generic
  HASHING3(Op2, true, op(), x()->subst(), y()->subst())
};


LEAF(IfOp, Op2)
 private:
  Value _tval;
  Value _fval;

 public:
  // creation
  IfOp(Value x, Condition cond, Value y, Value tval, Value fval)
  : Op2(tval->type()->meet(fval->type()), (Bytecodes::Code)cond, x, y)
  , _tval(tval)
  , _fval(fval)
  {
    ASSERT_VALUES
    assert(tval->type()->tag() == fval->type()->tag(), "types must match");
  }

  // accessors
  virtual bool is_commutative() const;
  Bytecodes::Code op() const                     { ShouldNotCallThis(); return Bytecodes::_illegal; }
  Condition cond() const                         { return (Condition)Op2::op(); }
  Value tval() const                             { return _tval; }
  Value fval() const                             { return _fval; }

  // generic
  virtual void input_values_do(ValueVisitor* f)   { Op2::input_values_do(f); f->visit(&_tval); f->visit(&_fval); }
};


LEAF(Convert, Instruction)
 private:
  Bytecodes::Code _op;
  Value           _value;

 public:
  // creation
  Convert(Bytecodes::Code op, Value value, ValueType* to_type) : Instruction(to_type), _op(op), _value(value) {
    ASSERT_VALUES
  }

  // accessors
  Bytecodes::Code op() const                     { return _op; }
  Value value() const                            { return _value; }

  // generic
  virtual void input_values_do(ValueVisitor* f)   { f->visit(&_value); }
  HASHING2(Convert, true, op(), value()->subst())
};


LEAF(NullCheck, Instruction)
 private:
  Value       _obj;

 public:
  // creation
  NullCheck(Value obj, ValueStack* state_before)
  : Instruction(obj->type()->base(), state_before)
  , _obj(obj)
  {
    ASSERT_VALUES
    set_can_trap(true);
    assert(_obj->type()->is_object(), "null check must be applied to objects only");
    pin(Instruction::PinExplicitNullCheck);
  }

  // accessors
  Value obj() const                              { return _obj; }

  // setters
  void set_can_trap(bool can_trap)               { set_flag(CanTrapFlag, can_trap); }

  // generic
  virtual bool can_trap() const                  { return check_flag(CanTrapFlag); /* null-check elimination sets to false */ }
  virtual void input_values_do(ValueVisitor* f)   { f->visit(&_obj); }
  HASHING1(NullCheck, true, obj()->subst())
};


// This node is supposed to cast the type of another node to a more precise
// declared type.
LEAF(TypeCast, Instruction)
 private:
  ciType* _declared_type;
  Value   _obj;

 public:
  // The type of this node is the same type as the object type (and it might be constant).
  TypeCast(ciType* type, Value obj, ValueStack* state_before)
  : Instruction(obj->type(), state_before, obj->type()->is_constant()),
    _declared_type(type),
    _obj(obj) {}

  // accessors
  ciType* declared_type() const                  { return _declared_type; }
  Value   obj() const                            { return _obj; }

  // generic
  virtual void input_values_do(ValueVisitor* f)  { f->visit(&_obj); }
};


BASE(StateSplit, Instruction)
 private:
  ValueStack* _state;

 protected:
  static void substitute(BlockList& list, BlockBegin* old_block, BlockBegin* new_block);

 public:
  // creation
  StateSplit(ValueType* type, ValueStack* state_before = NULL)
  : Instruction(type, state_before)
  , _state(NULL)
  {
    pin(PinStateSplitConstructor);
  }

  // accessors
  ValueStack* state() const                      { return _state; }
  IRScope* scope() const;                        // the state's scope

  // manipulation
  void set_state(ValueStack* state)              { assert(_state == NULL, "overwriting existing state"); check_state(state); _state = state; }

  // generic
  virtual void input_values_do(ValueVisitor* f)   { /* no values */ }
  virtual void state_values_do(ValueVisitor* f);
};


LEAF(Invoke, StateSplit)
 private:
  Bytecodes::Code _code;
  Value           _recv;
  Values*         _args;
  BasicTypeList*  _signature;
  ciMethod*       _target;

 public:
  // creation
  Invoke(Bytecodes::Code code, ValueType* result_type, Value recv, Values* args,
         ciMethod* target, ValueStack* state_before);

  // accessors
  Bytecodes::Code code() const                   { return _code; }
  Value receiver() const                         { return _recv; }
  bool has_receiver() const                      { return receiver() != NULL; }
  int number_of_arguments() const                { return _args->length(); }
  Value argument_at(int i) const                 { return _args->at(i); }
  BasicTypeList* signature() const               { return _signature; }
  ciMethod* target() const                       { return _target; }

  ciType* declared_type() const;

  // Returns false if target is not loaded
  bool target_is_final() const                   { return check_flag(TargetIsFinalFlag); }
  bool target_is_loaded() const                  { return check_flag(TargetIsLoadedFlag); }

  // JSR 292 support
  bool is_invokedynamic() const                  { return code() == Bytecodes::_invokedynamic; }
  bool is_method_handle_intrinsic() const        { return target()->is_method_handle_intrinsic(); }

  virtual bool needs_exception_state() const     { return false; }

  // generic
  virtual bool can_trap() const                  { return true; }
  virtual void input_values_do(ValueVisitor* f) {
    StateSplit::input_values_do(f);
    if (has_receiver()) f->visit(&_recv);
    for (int i = 0; i < _args->length(); i++) f->visit(_args->adr_at(i));
  }
  virtual void state_values_do(ValueVisitor *f);
};


LEAF(NewInstance, StateSplit)
 private:
  ciInstanceKlass* _klass;
  bool _is_unresolved;

 public:
  // creation
  NewInstance(ciInstanceKlass* klass, ValueStack* state_before, bool is_unresolved)
  : StateSplit(instanceType, state_before)
  , _klass(klass), _is_unresolved(is_unresolved)
  {}

  // accessors
  ciInstanceKlass* klass() const                 { return _klass; }
  bool is_unresolved() const                     { return _is_unresolved; }

  virtual bool needs_exception_state() const     { return false; }

  // generic
  virtual bool can_trap() const                  { return true; }
  ciType* exact_type() const;
  ciType* declared_type() const;
};


BASE(NewArray, StateSplit)
 private:
  Value       _length;

 public:
  // creation
  NewArray(Value length, ValueStack* state_before)
  : StateSplit(objectType, state_before)
  , _length(length)
  {
    // Do not ASSERT_VALUES since length is NULL for NewMultiArray
  }

  // accessors
  Value length() const                           { return _length; }

  virtual bool needs_exception_state() const     { return false; }

  ciType* exact_type() const                     { return NULL; }
  ciType* declared_type() const;

  // generic
  virtual bool can_trap() const                  { return true; }
  virtual void input_values_do(ValueVisitor* f)   { StateSplit::input_values_do(f); f->visit(&_length); }
};


LEAF(NewTypeArray, NewArray)
 private:
  BasicType _elt_type;

 public:
  // creation
  NewTypeArray(Value length, BasicType elt_type, ValueStack* state_before)
  : NewArray(length, state_before)
  , _elt_type(elt_type)
  {}

  // accessors
  BasicType elt_type() const                     { return _elt_type; }
  ciType* exact_type() const;
};


LEAF(NewObjectArray, NewArray)
 private:
  ciKlass* _klass;

 public:
  // creation
  NewObjectArray(ciKlass* klass, Value length, ValueStack* state_before) : NewArray(length, state_before), _klass(klass) {}

  // accessors
  ciKlass* klass() const                         { return _klass; }
  ciType* exact_type() const;
};


LEAF(NewMultiArray, NewArray)
 private:
  ciKlass* _klass;
  Values*  _dims;

 public:
  // creation
  NewMultiArray(ciKlass* klass, Values* dims, ValueStack* state_before) : NewArray(NULL, state_before), _klass(klass), _dims(dims) {
    ASSERT_VALUES
  }

  // accessors
  ciKlass* klass() const                         { return _klass; }
  Values* dims() const                           { return _dims; }
  int rank() const                               { return dims()->length(); }

  // generic
  virtual void input_values_do(ValueVisitor* f) {
    // NOTE: we do not call NewArray::input_values_do since "length"
    // is meaningless for a multi-dimensional array; passing the
    // zeroth element down to NewArray as its length is a bad idea
    // since there will be a copy in the "dims" array which doesn't
    // get updated, and the value must not be traversed twice. Was bug
    // - kbr 4/10/2001
    StateSplit::input_values_do(f);
    for (int i = 0; i < _dims->length(); i++) f->visit(_dims->adr_at(i));
  }
};


BASE(TypeCheck, StateSplit)
 private:
  ciKlass*    _klass;
  Value       _obj;

  ciMethod* _profiled_method;
  int       _profiled_bci;

 public:
  // creation
  TypeCheck(ciKlass* klass, Value obj, ValueType* type, ValueStack* state_before)
  : StateSplit(type, state_before), _klass(klass), _obj(obj),
    _profiled_method(NULL), _profiled_bci(0) {
    ASSERT_VALUES
    set_direct_compare(false);
  }

  // accessors
  ciKlass* klass() const                         { return _klass; }
  Value obj() const                              { return _obj; }
  bool is_loaded() const                         { return klass() != NULL; }
  bool direct_compare() const                    { return check_flag(DirectCompareFlag); }

  // manipulation
  void set_direct_compare(bool flag)             { set_flag(DirectCompareFlag, flag); }

  // generic
  virtual bool can_trap() const                  { return true; }
  virtual void input_values_do(ValueVisitor* f)   { StateSplit::input_values_do(f); f->visit(&_obj); }

  // Helpers for MethodData* profiling
  void set_should_profile(bool value)                { set_flag(ProfileMDOFlag, value); }
  void set_profiled_method(ciMethod* method)         { _profiled_method = method;   }
  void set_profiled_bci(int bci)                     { _profiled_bci = bci;         }
  bool      should_profile() const                   { return check_flag(ProfileMDOFlag); }
  ciMethod* profiled_method() const                  { return _profiled_method;     }
  int       profiled_bci() const                     { return _profiled_bci;        }
};


LEAF(CheckCast, TypeCheck)
 public:
  // creation
  CheckCast(ciKlass* klass, Value obj, ValueStack* state_before)
  : TypeCheck(klass, obj, objectType, state_before) {}

  void set_incompatible_class_change_check() {
    set_flag(ThrowIncompatibleClassChangeErrorFlag, true);
  }
  bool is_incompatible_class_change_check() const {
    return check_flag(ThrowIncompatibleClassChangeErrorFlag);
  }
  void set_invokespecial_receiver_check() {
    set_flag(InvokeSpecialReceiverCheckFlag, true);
  }
  bool is_invokespecial_receiver_check() const {
    return check_flag(InvokeSpecialReceiverCheckFlag);
  }

  virtual bool needs_exception_state() const {
    return !is_invokespecial_receiver_check();
  }

  ciType* declared_type() const;
};


LEAF(InstanceOf, TypeCheck)
 public:
  // creation
  InstanceOf(ciKlass* klass, Value obj, ValueStack* state_before) : TypeCheck(klass, obj, intType, state_before) {}

  virtual bool needs_exception_state() const     { return false; }
};


BASE(AccessMonitor, StateSplit)
 private:
  Value       _obj;
  int         _monitor_no;

 public:
  // creation
  AccessMonitor(Value obj, int monitor_no, ValueStack* state_before = NULL)
  : StateSplit(illegalType, state_before)
  , _obj(obj)
  , _monitor_no(monitor_no)
  {
    set_needs_null_check(true);
    ASSERT_VALUES
  }

  // accessors
  Value obj() const                              { return _obj; }
  int monitor_no() const                         { return _monitor_no; }

  // generic
  virtual void input_values_do(ValueVisitor* f)   { StateSplit::input_values_do(f); f->visit(&_obj); }
};


LEAF(MonitorEnter, AccessMonitor)
 public:
  // creation
  MonitorEnter(Value obj, int monitor_no, ValueStack* state_before)
  : AccessMonitor(obj, monitor_no, state_before)
  {
    ASSERT_VALUES
  }

  // generic
  virtual bool can_trap() const                  { return true; }
};


LEAF(MonitorExit, AccessMonitor)
 public:
  // creation
  MonitorExit(Value obj, int monitor_no)
  : AccessMonitor(obj, monitor_no, NULL)
  {
    ASSERT_VALUES
  }
};


LEAF(Intrinsic, StateSplit)
 private:
  vmIntrinsics::ID _id;
  Values*          _args;
  Value            _recv;
  ArgsNonNullState _nonnull_state;

 public:
  // preserves_state can be set to true for Intrinsics
  // which are guaranteed to preserve register state across any slow
  // cases; setting it to true does not mean that the Intrinsic can
  // not trap, only that if we continue execution in the same basic
  // block after the Intrinsic, all of the registers are intact. This
  // allows load elimination and common expression elimination to be
  // performed across the Intrinsic.  The default value is false.
  Intrinsic(ValueType* type,
            vmIntrinsics::ID id,
            Values* args,
            bool has_receiver,
            ValueStack* state_before,
            bool preserves_state,
            bool cantrap = true)
  : StateSplit(type, state_before)
  , _id(id)
  , _args(args)
  , _recv(NULL)
  {
    assert(args != NULL, "args must exist");
    ASSERT_VALUES
    set_flag(PreservesStateFlag, preserves_state);
    set_flag(CanTrapFlag,        cantrap);
    if (has_receiver) {
      _recv = argument_at(0);
    }
    set_needs_null_check(has_receiver);

    // some intrinsics can't trap, so don't force them to be pinned
    if (!can_trap() && !vmIntrinsics::should_be_pinned(_id)) {
      unpin(PinStateSplitConstructor);
    }
  }

  // accessors
  vmIntrinsics::ID id() const                    { return _id; }
  int number_of_arguments() const                { return _args->length(); }
  Value argument_at(int i) const                 { return _args->at(i); }

  bool has_receiver() const                      { return (_recv != NULL); }
  Value receiver() const                         { assert(has_receiver(), "must have receiver"); return _recv; }
  bool preserves_state() const                   { return check_flag(PreservesStateFlag); }

  bool arg_needs_null_check(int i) const {
    return _nonnull_state.arg_needs_null_check(i);
  }

  void set_arg_needs_null_check(int i, bool check) {
    _nonnull_state.set_arg_needs_null_check(i, check);
  }

  // generic
  virtual bool can_trap() const                  { return check_flag(CanTrapFlag); }
  virtual void input_values_do(ValueVisitor* f) {
    StateSplit::input_values_do(f);
    for (int i = 0; i < _args->length(); i++) f->visit(_args->adr_at(i));
  }
};


class LIR_List;

LEAF(BlockBegin, StateSplit)
 private:
  int        _block_id;                          // the unique block id
  int        _bci;                               // start-bci of block
  int        _depth_first_number;                // number of this block in a depth-first ordering
  int        _linear_scan_number;                // number of this block in linear-scan ordering
  int        _dominator_depth;
  int        _loop_depth;                        // the loop nesting level of this block
  int        _loop_index;                        // number of the innermost loop of this block
  int        _flags;                             // the flags associated with this block

  // fields used by BlockListBuilder
  int            _total_preds;                   // number of predecessors found by BlockListBuilder
  ResourceBitMap _stores_to_locals;              // bit is set when a local variable is stored in the block

  // SSA specific fields: (factor out later)
  BlockList   _successors;                       // the successors of this block
  BlockList   _predecessors;                     // the predecessors of this block
  BlockList   _dominates;                        // list of blocks that are dominated by this block
  BlockBegin* _dominator;                        // the dominator of this block
  // SSA specific ends
  BlockEnd*  _end;                               // the last instruction of this block
  BlockList  _exception_handlers;                // the exception handlers potentially invoked by this block
  ValueStackStack* _exception_states;            // only for xhandler entries: states of all instructions that have an edge to this xhandler
  int        _exception_handler_pco;             // if this block is the start of an exception handler,
                                                 // this records the PC offset in the assembly code of the
                                                 // first instruction in this block
  Label      _label;                             // the label associated with this block
  LIR_List*  _lir;                               // the low level intermediate representation for this block

  ResourceBitMap _live_in;                       // set of live LIR_Opr registers at entry to this block
  ResourceBitMap _live_out;                      // set of live LIR_Opr registers at exit from this block
  ResourceBitMap _live_gen;                      // set of registers used before any redefinition in this block
  ResourceBitMap _live_kill;                     // set of registers defined in this block

  ResourceBitMap _fpu_register_usage;
  intArray*      _fpu_stack_state;               // For x86 FPU code generation with UseLinearScan
  int            _first_lir_instruction_id;      // ID of first LIR instruction in this block
  int            _last_lir_instruction_id;       // ID of last LIR instruction in this block

  void iterate_preorder (boolArray& mark, BlockClosure* closure);
  void iterate_postorder(boolArray& mark, BlockClosure* closure);

  friend class SuxAndWeightAdjuster;

 public:
   void* operator new(size_t size) throw() {
    Compilation* c = Compilation::current();
    void* res = c->arena()->Amalloc(size);
    return res;
  }

  // initialization/counting
  static int  number_of_blocks() {
    return Compilation::current()->number_of_blocks();
  }

  // creation
  BlockBegin(int bci)
  : StateSplit(illegalType)
  , _block_id(Compilation::current()->get_next_block_id())
  , _bci(bci)
  , _depth_first_number(-1)
  , _linear_scan_number(-1)
  , _dominator_depth(-1)
  , _loop_depth(0)
  , _loop_index(-1)
  , _flags(0)
  , _total_preds(0)
  , _stores_to_locals()
  , _successors(2)
  , _predecessors(2)
  , _dominates(2)
  , _dominator(NULL)
  , _end(NULL)
  , _exception_handlers(1)
  , _exception_states(NULL)
  , _exception_handler_pco(-1)
  , _lir(NULL)
  , _live_in()
  , _live_out()
  , _live_gen()
  , _live_kill()
  , _fpu_register_usage()
  , _fpu_stack_state(NULL)
  , _first_lir_instruction_id(-1)
  , _last_lir_instruction_id(-1)
  {
    _block = this;
#ifndef PRODUCT
    set_printable_bci(bci);
#endif
  }

  // accessors
  int block_id() const                           { return _block_id; }
  int bci() const                                { return _bci; }
  BlockList* successors()                        { return &_successors; }
  BlockList* dominates()                         { return &_dominates; }
  BlockBegin* dominator() const                  { return _dominator; }
  int loop_depth() const                         { return _loop_depth; }
  int dominator_depth() const                    { return _dominator_depth; }
  int depth_first_number() const                 { return _depth_first_number; }
  int linear_scan_number() const                 { return _linear_scan_number; }
  BlockEnd* end() const                          { return _end; }
  Label* label()                                 { return &_label; }
  LIR_List* lir() const                          { return _lir; }
  int exception_handler_pco() const              { return _exception_handler_pco; }
  ResourceBitMap& live_in()                      { return _live_in;        }
  ResourceBitMap& live_out()                     { return _live_out;       }
  ResourceBitMap& live_gen()                     { return _live_gen;       }
  ResourceBitMap& live_kill()                    { return _live_kill;      }
  ResourceBitMap& fpu_register_usage()           { return _fpu_register_usage; }
  intArray* fpu_stack_state() const              { return _fpu_stack_state;    }
  int first_lir_instruction_id() const           { return _first_lir_instruction_id; }
  int last_lir_instruction_id() const            { return _last_lir_instruction_id; }
  int total_preds() const                        { return _total_preds; }
  BitMap& stores_to_locals()                     { return _stores_to_locals; }

  // manipulation
  void set_dominator(BlockBegin* dom)            { _dominator = dom; }
  void set_loop_depth(int d)                     { _loop_depth = d; }
  void set_dominator_depth(int d)                { _dominator_depth = d; }
  void set_depth_first_number(int dfn)           { _depth_first_number = dfn; }
  void set_linear_scan_number(int lsn)           { _linear_scan_number = lsn; }
  void set_end(BlockEnd* end);
  void clear_end();
  void disconnect_from_graph();
  static void disconnect_edge(BlockBegin* from, BlockBegin* to);
  BlockBegin* insert_block_between(BlockBegin* sux);
  void substitute_sux(BlockBegin* old_sux, BlockBegin* new_sux);
  void set_lir(LIR_List* lir)                    { _lir = lir; }
  void set_exception_handler_pco(int pco)        { _exception_handler_pco = pco; }
  void set_live_in  (const ResourceBitMap& map)  { _live_in = map;   }
  void set_live_out (const ResourceBitMap& map)  { _live_out = map;  }
  void set_live_gen (const ResourceBitMap& map)  { _live_gen = map;  }
  void set_live_kill(const ResourceBitMap& map)  { _live_kill = map; }
  void set_fpu_register_usage(const ResourceBitMap& map) { _fpu_register_usage = map; }
  void set_fpu_stack_state(intArray* state)      { _fpu_stack_state = state;  }
  void set_first_lir_instruction_id(int id)      { _first_lir_instruction_id = id;  }
  void set_last_lir_instruction_id(int id)       { _last_lir_instruction_id = id;  }
  void increment_total_preds(int n = 1)          { _total_preds += n; }
  void init_stores_to_locals(int locals_count)   { _stores_to_locals.initialize(locals_count); }

  // generic
  virtual void state_values_do(ValueVisitor* f);

  // successors and predecessors
  int number_of_sux() const;
  BlockBegin* sux_at(int i) const;
  void add_successor(BlockBegin* sux);
  void remove_successor(BlockBegin* pred);
  bool is_successor(BlockBegin* sux) const       { return _successors.contains(sux); }

  void add_predecessor(BlockBegin* pred);
  void remove_predecessor(BlockBegin* pred);
  bool is_predecessor(BlockBegin* pred) const    { return _predecessors.contains(pred); }
  int number_of_preds() const                    { return _predecessors.length(); }
  BlockBegin* pred_at(int i) const               { return _predecessors.at(i); }

  // exception handlers potentially invoked by this block
  void add_exception_handler(BlockBegin* b);
  bool is_exception_handler(BlockBegin* b) const { return _exception_handlers.contains(b); }
  int  number_of_exception_handlers() const      { return _exception_handlers.length(); }
  BlockBegin* exception_handler_at(int i) const  { return _exception_handlers.at(i); }

  // states of the instructions that have an edge to this exception handler
  int number_of_exception_states()               { assert(is_set(exception_entry_flag), "only for xhandlers"); return _exception_states == NULL ? 0 : _exception_states->length(); }
  ValueStack* exception_state_at(int idx) const  { assert(is_set(exception_entry_flag), "only for xhandlers"); return _exception_states->at(idx); }
  int add_exception_state(ValueStack* state);

  // flags
  enum Flag {
    no_flag                       = 0,
    std_entry_flag                = 1 << 0,
    osr_entry_flag                = 1 << 1,
    exception_entry_flag          = 1 << 2,
    subroutine_entry_flag         = 1 << 3,
    backward_branch_target_flag   = 1 << 4,
    is_on_work_list_flag          = 1 << 5,
    was_visited_flag              = 1 << 6,
    parser_loop_header_flag       = 1 << 7,  // set by parser to identify blocks where phi functions can not be created on demand
    critical_edge_split_flag      = 1 << 8, // set for all blocks that are introduced when critical edges are split
    linear_scan_loop_header_flag  = 1 << 9, // set during loop-detection for LinearScan
    linear_scan_loop_end_flag     = 1 << 10, // set during loop-detection for LinearScan
    donot_eliminate_range_checks  = 1 << 11  // Should be try to eliminate range checks in this block
  };

  void set(Flag f)                               { _flags |= f; }
  void clear(Flag f)                             { _flags &= ~f; }
  bool is_set(Flag f) const                      { return (_flags & f) != 0; }
  bool is_entry_block() const {
    const int entry_mask = std_entry_flag | osr_entry_flag | exception_entry_flag;
    return (_flags & entry_mask) != 0;
  }

  // iteration
  void iterate_preorder   (BlockClosure* closure);
  void iterate_postorder  (BlockClosure* closure);

  void block_values_do(ValueVisitor* f);

  // loops
  void set_loop_index(int ix)                    { _loop_index = ix;        }
  int  loop_index() const                        { return _loop_index;      }

  // merging
  bool try_merge(ValueStack* state);             // try to merge states at block begin
  void merge(ValueStack* state)                  { bool b = try_merge(state); assert(b, "merge failed"); }

  // debugging
  void print_block()                             PRODUCT_RETURN;
  void print_block(InstructionPrinter& ip, bool live_only = false) PRODUCT_RETURN;
};


BASE(BlockEnd, StateSplit)
 private:
  BlockList*  _sux;

 protected:
  BlockList* sux() const                         { return _sux; }

  void set_sux(BlockList* sux) {
#ifdef ASSERT
    assert(sux != NULL, "sux must exist");
    for (int i = sux->length() - 1; i >= 0; i--) assert(sux->at(i) != NULL, "sux must exist");
#endif
    _sux = sux;
  }

 public:
  // creation
  BlockEnd(ValueType* type, ValueStack* state_before, bool is_safepoint)
  : StateSplit(type, state_before)
  , _sux(NULL)
  {
    set_flag(IsSafepointFlag, is_safepoint);
  }

  // accessors
  bool is_safepoint() const                      { return check_flag(IsSafepointFlag); }
  // For compatibility with old code, for new code use block()
  BlockBegin* begin() const                      { return _block; }

  // manipulation
  void set_begin(BlockBegin* begin);

  // successors
  int number_of_sux() const                      { return _sux != NULL ? _sux->length() : 0; }
  BlockBegin* sux_at(int i) const                { return _sux->at(i); }
  BlockBegin* default_sux() const                { return sux_at(number_of_sux() - 1); }
  BlockBegin** addr_sux_at(int i) const          { return _sux->adr_at(i); }
  int sux_index(BlockBegin* sux) const           { return _sux->find(sux); }
  void substitute_sux(BlockBegin* old_sux, BlockBegin* new_sux);
};


LEAF(Goto, BlockEnd)
 public:
  enum Direction {
    none,            // Just a regular goto
    taken, not_taken // Goto produced from If
  };
 private:
  ciMethod*   _profiled_method;
  int         _profiled_bci;
  Direction   _direction;
 public:
  // creation
  Goto(BlockBegin* sux, ValueStack* state_before, bool is_safepoint = false)
    : BlockEnd(illegalType, state_before, is_safepoint)
    , _profiled_method(NULL)
    , _profiled_bci(0)
    , _direction(none) {
    BlockList* s = new BlockList(1);
    s->append(sux);
    set_sux(s);
  }

  Goto(BlockBegin* sux, bool is_safepoint) : BlockEnd(illegalType, NULL, is_safepoint)
                                           , _profiled_method(NULL)
                                           , _profiled_bci(0)
                                           , _direction(none) {
    BlockList* s = new BlockList(1);
    s->append(sux);
    set_sux(s);
  }

  bool should_profile() const                    { return check_flag(ProfileMDOFlag); }
  ciMethod* profiled_method() const              { return _profiled_method; } // set only for profiled branches
  int profiled_bci() const                       { return _profiled_bci; }
  Direction direction() const                    { return _direction; }

  void set_should_profile(bool value)            { set_flag(ProfileMDOFlag, value); }
  void set_profiled_method(ciMethod* method)     { _profiled_method = method; }
  void set_profiled_bci(int bci)                 { _profiled_bci = bci; }
  void set_direction(Direction d)                { _direction = d; }
};

#ifdef ASSERT
LEAF(Assert, Instruction)
  private:
  Value       _x;
  Condition   _cond;
  Value       _y;
  char        *_message;

 public:
  // creation
  // unordered_is_true is valid for float/double compares only
   Assert(Value x, Condition cond, bool unordered_is_true, Value y);

  // accessors
  Value x() const                                { return _x; }
  Condition cond() const                         { return _cond; }
  bool unordered_is_true() const                 { return check_flag(UnorderedIsTrueFlag); }
  Value y() const                                { return _y; }
  const char *message() const                    { return _message; }

  // generic
  virtual void input_values_do(ValueVisitor* f)  { f->visit(&_x); f->visit(&_y); }
};
#endif

LEAF(RangeCheckPredicate, StateSplit)
 private:
  Value       _x;
  Condition   _cond;
  Value       _y;

  void check_state();

 public:
  // creation
  // unordered_is_true is valid for float/double compares only
   RangeCheckPredicate(Value x, Condition cond, bool unordered_is_true, Value y, ValueStack* state) : StateSplit(illegalType)
  , _x(x)
  , _cond(cond)
  , _y(y)
  {
    ASSERT_VALUES
    set_flag(UnorderedIsTrueFlag, unordered_is_true);
    assert(x->type()->tag() == y->type()->tag(), "types must match");
    this->set_state(state);
    check_state();
  }

  // Always deoptimize
  RangeCheckPredicate(ValueStack* state) : StateSplit(illegalType)
  {
    this->set_state(state);
    _x = _y = NULL;
    check_state();
  }

  // accessors
  Value x() const                                { return _x; }
  Condition cond() const                         { return _cond; }
  bool unordered_is_true() const                 { return check_flag(UnorderedIsTrueFlag); }
  Value y() const                                { return _y; }

  void always_fail()                             { _x = _y = NULL; }

  // generic
  virtual void input_values_do(ValueVisitor* f)  { StateSplit::input_values_do(f); f->visit(&_x); f->visit(&_y); }
  HASHING3(RangeCheckPredicate, true, x()->subst(), y()->subst(), cond())
};

LEAF(If, BlockEnd)
 private:
  Value       _x;
  Condition   _cond;
  Value       _y;
  ciMethod*   _profiled_method;
  int         _profiled_bci; // Canonicalizer may alter bci of If node
  bool        _swapped;      // Is the order reversed with respect to the original If in the
                             // bytecode stream?
 public:
  // creation
  // unordered_is_true is valid for float/double compares only
  If(Value x, Condition cond, bool unordered_is_true, Value y, BlockBegin* tsux, BlockBegin* fsux, ValueStack* state_before, bool is_safepoint)
    : BlockEnd(illegalType, state_before, is_safepoint)
  , _x(x)
  , _cond(cond)
  , _y(y)
  , _profiled_method(NULL)
  , _profiled_bci(0)
  , _swapped(false)
  {
    ASSERT_VALUES
    set_flag(UnorderedIsTrueFlag, unordered_is_true);
    assert(x->type()->tag() == y->type()->tag(), "types must match");
    BlockList* s = new BlockList(2);
    s->append(tsux);
    s->append(fsux);
    set_sux(s);
  }

  // accessors
  Value x() const                                { return _x; }
  Condition cond() const                         { return _cond; }
  bool unordered_is_true() const                 { return check_flag(UnorderedIsTrueFlag); }
  Value y() const                                { return _y; }
  BlockBegin* sux_for(bool is_true) const        { return sux_at(is_true ? 0 : 1); }
  BlockBegin* tsux() const                       { return sux_for(true); }
  BlockBegin* fsux() const                       { return sux_for(false); }
  BlockBegin* usux() const                       { return sux_for(unordered_is_true()); }
  bool should_profile() const                    { return check_flag(ProfileMDOFlag); }
  ciMethod* profiled_method() const              { return _profiled_method; } // set only for profiled branches
  int profiled_bci() const                       { return _profiled_bci; }    // set for profiled branches and tiered
  bool is_swapped() const                        { return _swapped; }

  // manipulation
  void swap_operands() {
    Value t = _x; _x = _y; _y = t;
    _cond = mirror(_cond);
  }

  void swap_sux() {
    assert(number_of_sux() == 2, "wrong number of successors");
    BlockList* s = sux();
    BlockBegin* t = s->at(0); s->at_put(0, s->at(1)); s->at_put(1, t);
    _cond = negate(_cond);
    set_flag(UnorderedIsTrueFlag, !check_flag(UnorderedIsTrueFlag));
  }

  void set_should_profile(bool value)             { set_flag(ProfileMDOFlag, value); }
  void set_profiled_method(ciMethod* method)      { _profiled_method = method; }
  void set_profiled_bci(int bci)                  { _profiled_bci = bci;       }
  void set_swapped(bool value)                    { _swapped = value;         }
  // generic
  virtual void input_values_do(ValueVisitor* f)   { BlockEnd::input_values_do(f); f->visit(&_x); f->visit(&_y); }
};


BASE(Switch, BlockEnd)
 private:
  Value       _tag;

 public:
  // creation
  Switch(Value tag, BlockList* sux, ValueStack* state_before, bool is_safepoint)
  : BlockEnd(illegalType, state_before, is_safepoint)
  , _tag(tag) {
    ASSERT_VALUES
    set_sux(sux);
  }

  // accessors
  Value tag() const                              { return _tag; }
  int length() const                             { return number_of_sux() - 1; }

  virtual bool needs_exception_state() const     { return false; }

  // generic
  virtual void input_values_do(ValueVisitor* f)   { BlockEnd::input_values_do(f); f->visit(&_tag); }
};


LEAF(TableSwitch, Switch)
 private:
  int _lo_key;

 public:
  // creation
  TableSwitch(Value tag, BlockList* sux, int lo_key, ValueStack* state_before, bool is_safepoint)
    : Switch(tag, sux, state_before, is_safepoint)
  , _lo_key(lo_key) { assert(_lo_key <= hi_key(), "integer overflow"); }

  // accessors
  int lo_key() const                             { return _lo_key; }
  int hi_key() const                             { return _lo_key + (length() - 1); }
};


LEAF(LookupSwitch, Switch)
 private:
  intArray* _keys;

 public:
  // creation
  LookupSwitch(Value tag, BlockList* sux, intArray* keys, ValueStack* state_before, bool is_safepoint)
  : Switch(tag, sux, state_before, is_safepoint)
  , _keys(keys) {
    assert(keys != NULL, "keys must exist");
    assert(keys->length() == length(), "sux & keys have incompatible lengths");
  }

  // accessors
  int key_at(int i) const                        { return _keys->at(i); }
};


LEAF(Return, BlockEnd)
 private:
  Value _result;

 public:
  // creation
  Return(Value result) :
    BlockEnd(result == NULL ? voidType : result->type()->base(), NULL, true),
    _result(result) {}

  // accessors
  Value result() const                           { return _result; }
  bool has_result() const                        { return result() != NULL; }

  // generic
  virtual void input_values_do(ValueVisitor* f) {
    BlockEnd::input_values_do(f);
    if (has_result()) f->visit(&_result);
  }
};


LEAF(Throw, BlockEnd)
 private:
  Value _exception;

 public:
  // creation
  Throw(Value exception, ValueStack* state_before) : BlockEnd(illegalType, state_before, true), _exception(exception) {
    ASSERT_VALUES
  }

  // accessors
  Value exception() const                        { return _exception; }

  // generic
  virtual bool can_trap() const                  { return true; }
  virtual void input_values_do(ValueVisitor* f)   { BlockEnd::input_values_do(f); f->visit(&_exception); }
};


LEAF(Base, BlockEnd)
 public:
  // creation
  Base(BlockBegin* std_entry, BlockBegin* osr_entry) : BlockEnd(illegalType, NULL, false) {
    assert(std_entry->is_set(BlockBegin::std_entry_flag), "std entry must be flagged");
    assert(osr_entry == NULL || osr_entry->is_set(BlockBegin::osr_entry_flag), "osr entry must be flagged");
    BlockList* s = new BlockList(2);
    if (osr_entry != NULL) s->append(osr_entry);
    s->append(std_entry); // must be default sux!
    set_sux(s);
  }

  // accessors
  BlockBegin* std_entry() const                  { return default_sux(); }
  BlockBegin* osr_entry() const                  { return number_of_sux() < 2 ? NULL : sux_at(0); }
};


LEAF(OsrEntry, Instruction)
 public:
  // creation
#ifdef _LP64
  OsrEntry() : Instruction(longType) { pin(); }
#else
  OsrEntry() : Instruction(intType)  { pin(); }
#endif

  // generic
  virtual void input_values_do(ValueVisitor* f)   { }
};


// Models the incoming exception at a catch site
LEAF(ExceptionObject, Instruction)
 public:
  // creation
  ExceptionObject() : Instruction(objectType) {
    pin();
  }

  // generic
  virtual void input_values_do(ValueVisitor* f)   { }
};


// Models needed rounding for floating-point values on Intel.
// Currently only used to represent rounding of double-precision
// values stored into local variables, but could be used to model
// intermediate rounding of single-precision values as well.
LEAF(RoundFP, Instruction)
 private:
  Value _input;             // floating-point value to be rounded

 public:
  RoundFP(Value input)
  : Instruction(input->type()) // Note: should not be used for constants
  , _input(input)
  {
    ASSERT_VALUES
  }

  // accessors
  Value input() const                            { return _input; }

  // generic
  virtual void input_values_do(ValueVisitor* f)   { f->visit(&_input); }
};


BASE(UnsafeOp, Instruction)
 private:
  Value _object;                                 // Object to be fetched from or mutated
  Value _offset;                                 // Offset within object
  bool  _is_volatile;                            // true if volatile - dl/JSR166
  BasicType _basic_type;                         // ValueType can not express byte-sized integers

 protected:
  // creation
  UnsafeOp(BasicType basic_type, Value object, Value offset, bool is_put, bool is_volatile)
    : Instruction(is_put ? voidType : as_ValueType(basic_type)),
    _object(object), _offset(offset), _is_volatile(is_volatile), _basic_type(basic_type)
  {
    //Note:  Unsafe ops are not not guaranteed to throw NPE.
    // Convservatively, Unsafe operations must be pinned though we could be
    // looser about this if we wanted to..
    pin();
  }

 public:
  // accessors
  BasicType basic_type()                         { return _basic_type; }
  Value object()                                 { return _object; }
  Value offset()                                 { return _offset; }
  bool  is_volatile()                            { return _is_volatile; }

  // generic
  virtual void input_values_do(ValueVisitor* f)   { f->visit(&_object);
                                                    f->visit(&_offset); }
};

LEAF(UnsafeGet, UnsafeOp)
 private:
  bool _is_raw;
 public:
  UnsafeGet(BasicType basic_type, Value object, Value offset, bool is_volatile)
  : UnsafeOp(basic_type, object, offset, false, is_volatile)
  {
    ASSERT_VALUES
    _is_raw = false;
  }
  UnsafeGet(BasicType basic_type, Value object, Value offset, bool is_volatile, bool is_raw)
  : UnsafeOp(basic_type, object, offset, false, is_volatile), _is_raw(is_raw)
  {
    ASSERT_VALUES
  }

  // accessors
  bool is_raw()                             { return _is_raw; }
};


LEAF(UnsafePut, UnsafeOp)
 private:
  Value _value;                                  // Value to be stored
 public:
  UnsafePut(BasicType basic_type, Value object, Value offset, Value value, bool is_volatile)
  : UnsafeOp(basic_type, object, offset, true, is_volatile)
    , _value(value)
  {
    ASSERT_VALUES
  }

  // accessors
  Value value()                                  { return _value; }

  // generic
  virtual void input_values_do(ValueVisitor* f)   { UnsafeOp::input_values_do(f);
                                                   f->visit(&_value); }
};

LEAF(UnsafeGetAndSet, UnsafeOp)
 private:
  Value _value;                                  // Value to be stored
  bool  _is_add;
 public:
  UnsafeGetAndSet(BasicType basic_type, Value object, Value offset, Value value, bool is_add)
  : UnsafeOp(basic_type, object, offset, false, false)
    , _value(value)
    , _is_add(is_add)
  {
    ASSERT_VALUES
  }

  // accessors
  bool is_add() const                            { return _is_add; }
  Value value()                                  { return _value; }

  // generic
  virtual void input_values_do(ValueVisitor* f)   { UnsafeOp::input_values_do(f);
                                                   f->visit(&_value); }
};

LEAF(ProfileCall, Instruction)
 private:
  ciMethod*        _method;
  int              _bci_of_invoke;
  ciMethod*        _callee;         // the method that is called at the given bci
  Value            _recv;
  ciKlass*         _known_holder;
  Values*          _obj_args;       // arguments for type profiling
  ArgsNonNullState _nonnull_state;  // Do we know whether some arguments are never null?
  bool             _inlined;        // Are we profiling a call that is inlined

 public:
  ProfileCall(ciMethod* method, int bci, ciMethod* callee, Value recv, ciKlass* known_holder, Values* obj_args, bool inlined)
    : Instruction(voidType)
    , _method(method)
    , _bci_of_invoke(bci)
    , _callee(callee)
    , _recv(recv)
    , _known_holder(known_holder)
    , _obj_args(obj_args)
    , _inlined(inlined)
  {
    // The ProfileCall has side-effects and must occur precisely where located
    pin();
  }

  ciMethod* method()             const { return _method; }
  int bci_of_invoke()            const { return _bci_of_invoke; }
  ciMethod* callee()             const { return _callee; }
  Value recv()                   const { return _recv; }
  ciKlass* known_holder()        const { return _known_holder; }
  int nb_profiled_args()         const { return _obj_args == NULL ? 0 : _obj_args->length(); }
  Value profiled_arg_at(int i)   const { return _obj_args->at(i); }
  bool arg_needs_null_check(int i) const {
    return _nonnull_state.arg_needs_null_check(i);
  }
  bool inlined()                 const { return _inlined; }

  void set_arg_needs_null_check(int i, bool check) {
    _nonnull_state.set_arg_needs_null_check(i, check);
  }

  virtual void input_values_do(ValueVisitor* f)   {
    if (_recv != NULL) {
      f->visit(&_recv);
    }
    for (int i = 0; i < nb_profiled_args(); i++) {
      f->visit(_obj_args->adr_at(i));
    }
  }
};

LEAF(ProfileReturnType, Instruction)
 private:
  ciMethod*        _method;
  ciMethod*        _callee;
  int              _bci_of_invoke;
  Value            _ret;

 public:
  ProfileReturnType(ciMethod* method, int bci, ciMethod* callee, Value ret)
    : Instruction(voidType)
    , _method(method)
    , _callee(callee)
    , _bci_of_invoke(bci)
    , _ret(ret)
  {
    set_needs_null_check(true);
    // The ProfileType has side-effects and must occur precisely where located
    pin();
  }

  ciMethod* method()             const { return _method; }
  ciMethod* callee()             const { return _callee; }
  int bci_of_invoke()            const { return _bci_of_invoke; }
  Value ret()                    const { return _ret; }

  virtual void input_values_do(ValueVisitor* f)   {
    if (_ret != NULL) {
      f->visit(&_ret);
    }
  }
};

// Call some C runtime function that doesn't safepoint,
// optionally passing the current thread as the first argument.
LEAF(RuntimeCall, Instruction)
 private:
  const char* _entry_name;
  address     _entry;
  Values*     _args;
  bool        _pass_thread;  // Pass the JavaThread* as an implicit first argument

 public:
  RuntimeCall(ValueType* type, const char* entry_name, address entry, Values* args, bool pass_thread = true)
    : Instruction(type)
    , _entry_name(entry_name)
    , _entry(entry)
    , _args(args)
    , _pass_thread(pass_thread) {
    ASSERT_VALUES
    pin();
  }

  const char* entry_name() const  { return _entry_name; }
  address entry() const           { return _entry; }
  int number_of_arguments() const { return _args->length(); }
  Value argument_at(int i) const  { return _args->at(i); }
  bool pass_thread() const        { return _pass_thread; }

  virtual void input_values_do(ValueVisitor* f)   {
    for (int i = 0; i < _args->length(); i++) f->visit(_args->adr_at(i));
  }
};

// Use to trip invocation counter of an inlined method

LEAF(ProfileInvoke, Instruction)
 private:
  ciMethod*   _inlinee;
  ValueStack* _state;

 public:
  ProfileInvoke(ciMethod* inlinee,  ValueStack* state)
    : Instruction(voidType)
    , _inlinee(inlinee)
    , _state(state)
  {
    // The ProfileInvoke has side-effects and must occur precisely where located QQQ???
    pin();
  }

  ciMethod* inlinee()      { return _inlinee; }
  ValueStack* state()      { return _state; }
  virtual void input_values_do(ValueVisitor*)   {}
  virtual void state_values_do(ValueVisitor*);
};

LEAF(MemBar, Instruction)
 private:
  LIR_Code _code;

 public:
  MemBar(LIR_Code code)
    : Instruction(voidType)
    , _code(code)
  {
    pin();
  }

  LIR_Code code()           { return _code; }

  virtual void input_values_do(ValueVisitor*)   {}
};

class BlockPair: public CompilationResourceObj {
 private:
  BlockBegin* _from;
  BlockBegin* _to;
 public:
  BlockPair(BlockBegin* from, BlockBegin* to): _from(from), _to(to) {}
  BlockBegin* from() const { return _from; }
  BlockBegin* to() const   { return _to;   }
  bool is_same(BlockBegin* from, BlockBegin* to) const { return  _from == from && _to == to; }
  bool is_same(BlockPair* p) const { return  _from == p->from() && _to == p->to(); }
  void set_to(BlockBegin* b)   { _to = b; }
  void set_from(BlockBegin* b) { _from = b; }
};

typedef GrowableArray<BlockPair*> BlockPairList;

inline int         BlockBegin::number_of_sux() const            { assert(_end == NULL || _end->number_of_sux() == _successors.length(), "mismatch"); return _successors.length(); }
inline BlockBegin* BlockBegin::sux_at(int i) const              { assert(_end == NULL || _end->sux_at(i) == _successors.at(i), "mismatch");          return _successors.at(i); }
inline void        BlockBegin::add_successor(BlockBegin* sux)   { assert(_end == NULL, "Would create mismatch with successors of BlockEnd");         _successors.append(sux); }

#undef ASSERT_VALUES

#endif // SHARE_C1_C1_INSTRUCTION_HPP
