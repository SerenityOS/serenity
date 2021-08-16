/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_C1_C1_LIR_HPP
#define SHARE_C1_C1_LIR_HPP

#include "c1/c1_Defs.hpp"
#include "c1/c1_ValueType.hpp"
#include "oops/method.hpp"
#include "utilities/globalDefinitions.hpp"

class BlockBegin;
class BlockList;
class LIR_Assembler;
class CodeEmitInfo;
class CodeStub;
class CodeStubList;
class C1SafepointPollStub;
class ArrayCopyStub;
class LIR_Op;
class ciType;
class ValueType;
class LIR_OpVisitState;
class FpuStackSim;

//---------------------------------------------------------------------
//                 LIR Operands
//  LIR_OprDesc
//    LIR_OprPtr
//      LIR_Const
//      LIR_Address
//---------------------------------------------------------------------
class LIR_OprDesc;
class LIR_OprPtr;
class LIR_Const;
class LIR_Address;
class LIR_OprVisitor;


typedef LIR_OprDesc* LIR_Opr;
typedef int          RegNr;

typedef GrowableArray<LIR_Opr> LIR_OprList;
typedef GrowableArray<LIR_Op*> LIR_OpArray;
typedef GrowableArray<LIR_Op*> LIR_OpList;

// define LIR_OprPtr early so LIR_OprDesc can refer to it
class LIR_OprPtr: public CompilationResourceObj {
 public:
  bool is_oop_pointer() const                    { return (type() == T_OBJECT); }
  bool is_float_kind() const                     { BasicType t = type(); return (t == T_FLOAT) || (t == T_DOUBLE); }

  virtual LIR_Const*  as_constant()              { return NULL; }
  virtual LIR_Address* as_address()              { return NULL; }
  virtual BasicType type() const                 = 0;
  virtual void print_value_on(outputStream* out) const = 0;
};



// LIR constants
class LIR_Const: public LIR_OprPtr {
 private:
  JavaValue _value;

  void type_check(BasicType t) const   { assert(type() == t, "type check"); }
  void type_check(BasicType t1, BasicType t2) const   { assert(type() == t1 || type() == t2, "type check"); }
  void type_check(BasicType t1, BasicType t2, BasicType t3) const   { assert(type() == t1 || type() == t2 || type() == t3, "type check"); }

 public:
  LIR_Const(jint i, bool is_address=false)       { _value.set_type(is_address?T_ADDRESS:T_INT); _value.set_jint(i); }
  LIR_Const(jlong l)                             { _value.set_type(T_LONG);    _value.set_jlong(l); }
  LIR_Const(jfloat f)                            { _value.set_type(T_FLOAT);   _value.set_jfloat(f); }
  LIR_Const(jdouble d)                           { _value.set_type(T_DOUBLE);  _value.set_jdouble(d); }
  LIR_Const(jobject o)                           { _value.set_type(T_OBJECT);  _value.set_jobject(o); }
  LIR_Const(void* p) {
#ifdef _LP64
    assert(sizeof(jlong) >= sizeof(p), "too small");;
    _value.set_type(T_LONG);    _value.set_jlong((jlong)p);
#else
    assert(sizeof(jint) >= sizeof(p), "too small");;
    _value.set_type(T_INT);     _value.set_jint((jint)p);
#endif
  }
  LIR_Const(Metadata* m) {
    _value.set_type(T_METADATA);
#ifdef _LP64
    _value.set_jlong((jlong)m);
#else
    _value.set_jint((jint)m);
#endif // _LP64
  }

  virtual BasicType type()       const { return _value.get_type(); }
  virtual LIR_Const* as_constant()     { return this; }

  jint      as_jint()    const         { type_check(T_INT, T_ADDRESS); return _value.get_jint(); }
  jlong     as_jlong()   const         { type_check(T_LONG  ); return _value.get_jlong(); }
  jfloat    as_jfloat()  const         { type_check(T_FLOAT ); return _value.get_jfloat(); }
  jdouble   as_jdouble() const         { type_check(T_DOUBLE); return _value.get_jdouble(); }
  jobject   as_jobject() const         { type_check(T_OBJECT); return _value.get_jobject(); }
  jint      as_jint_lo() const         { type_check(T_LONG  ); return low(_value.get_jlong()); }
  jint      as_jint_hi() const         { type_check(T_LONG  ); return high(_value.get_jlong()); }

#ifdef _LP64
  address   as_pointer() const         { type_check(T_LONG  ); return (address)_value.get_jlong(); }
  Metadata* as_metadata() const        { type_check(T_METADATA); return (Metadata*)_value.get_jlong(); }
#else
  address   as_pointer() const         { type_check(T_INT   ); return (address)_value.get_jint(); }
  Metadata* as_metadata() const        { type_check(T_METADATA); return (Metadata*)_value.get_jint(); }
#endif


  jint      as_jint_bits() const       { type_check(T_FLOAT, T_INT, T_ADDRESS); return _value.get_jint(); }
  jint      as_jint_lo_bits() const    {
    if (type() == T_DOUBLE) {
      return low(jlong_cast(_value.get_jdouble()));
    } else {
      return as_jint_lo();
    }
  }
  jint      as_jint_hi_bits() const    {
    if (type() == T_DOUBLE) {
      return high(jlong_cast(_value.get_jdouble()));
    } else {
      return as_jint_hi();
    }
  }
  jlong      as_jlong_bits() const    {
    if (type() == T_DOUBLE) {
      return jlong_cast(_value.get_jdouble());
    } else {
      return as_jlong();
    }
  }

  virtual void print_value_on(outputStream* out) const PRODUCT_RETURN;


  bool is_zero_float() {
    jfloat f = as_jfloat();
    jfloat ok = 0.0f;
    return jint_cast(f) == jint_cast(ok);
  }

  bool is_one_float() {
    jfloat f = as_jfloat();
    return !g_isnan(f) && g_isfinite(f) && f == 1.0;
  }

  bool is_zero_double() {
    jdouble d = as_jdouble();
    jdouble ok = 0.0;
    return jlong_cast(d) == jlong_cast(ok);
  }

  bool is_one_double() {
    jdouble d = as_jdouble();
    return !g_isnan(d) && g_isfinite(d) && d == 1.0;
  }
};


//---------------------LIR Operand descriptor------------------------------------
//
// The class LIR_OprDesc represents a LIR instruction operand;
// it can be a register (ALU/FPU), stack location or a constant;
// Constants and addresses are represented as resource area allocated
// structures (see above).
// Registers and stack locations are inlined into the this pointer
// (see value function).

class LIR_OprDesc: public CompilationResourceObj {
 public:
  // value structure:
  //     data       opr-type opr-kind
  // +--------------+-------+-------+
  // [max...........|7 6 5 4|3 2 1 0]
  //                               ^
  //                         is_pointer bit
  //
  // lowest bit cleared, means it is a structure pointer
  // we need  4 bits to represent types

 private:
  friend class LIR_OprFact;

  // Conversion
  intptr_t value() const                         { return (intptr_t) this; }

  bool check_value_mask(intptr_t mask, intptr_t masked_value) const {
    return (value() & mask) == masked_value;
  }

  enum OprKind {
      pointer_value      = 0
    , stack_value        = 1
    , cpu_register       = 3
    , fpu_register       = 5
    , illegal_value      = 7
  };

  enum OprBits {
      pointer_bits   = 1
    , kind_bits      = 3
    , type_bits      = 4
    , size_bits      = 2
    , destroys_bits  = 1
    , virtual_bits   = 1
    , is_xmm_bits    = 1
    , last_use_bits  = 1
    , is_fpu_stack_offset_bits = 1        // used in assertion checking on x86 for FPU stack slot allocation
    , non_data_bits  = pointer_bits + kind_bits + type_bits + size_bits + destroys_bits + virtual_bits
                       + is_xmm_bits + last_use_bits + is_fpu_stack_offset_bits
    , data_bits      = BitsPerInt - non_data_bits
    , reg_bits       = data_bits / 2      // for two registers in one value encoding
  };

  enum OprShift {
      kind_shift     = 0
    , type_shift     = kind_shift     + kind_bits
    , size_shift     = type_shift     + type_bits
    , destroys_shift = size_shift     + size_bits
    , last_use_shift = destroys_shift + destroys_bits
    , is_fpu_stack_offset_shift = last_use_shift + last_use_bits
    , virtual_shift  = is_fpu_stack_offset_shift + is_fpu_stack_offset_bits
    , is_xmm_shift   = virtual_shift + virtual_bits
    , data_shift     = is_xmm_shift + is_xmm_bits
    , reg1_shift = data_shift
    , reg2_shift = data_shift + reg_bits

  };

  enum OprSize {
      single_size = 0 << size_shift
    , double_size = 1 << size_shift
  };

  enum OprMask {
      kind_mask      = right_n_bits(kind_bits)
    , type_mask      = right_n_bits(type_bits) << type_shift
    , size_mask      = right_n_bits(size_bits) << size_shift
    , last_use_mask  = right_n_bits(last_use_bits) << last_use_shift
    , is_fpu_stack_offset_mask = right_n_bits(is_fpu_stack_offset_bits) << is_fpu_stack_offset_shift
    , virtual_mask   = right_n_bits(virtual_bits) << virtual_shift
    , is_xmm_mask    = right_n_bits(is_xmm_bits) << is_xmm_shift
    , pointer_mask   = right_n_bits(pointer_bits)
    , lower_reg_mask = right_n_bits(reg_bits)
    , no_type_mask   = (int)(~(type_mask | last_use_mask | is_fpu_stack_offset_mask))
  };

  uintptr_t data() const                         { return value() >> data_shift; }
  int lo_reg_half() const                        { return data() & lower_reg_mask; }
  int hi_reg_half() const                        { return (data() >> reg_bits) & lower_reg_mask; }
  OprKind kind_field() const                     { return (OprKind)(value() & kind_mask); }
  OprSize size_field() const                     { return (OprSize)(value() & size_mask); }

  static char type_char(BasicType t);

 public:
  enum {
    vreg_base = ConcreteRegisterImpl::number_of_registers,
    vreg_max = (1 << data_bits) - 1
  };

  static inline LIR_Opr illegalOpr();

  enum OprType {
      unknown_type  = 0 << type_shift    // means: not set (catch uninitialized types)
    , int_type      = 1 << type_shift
    , long_type     = 2 << type_shift
    , object_type   = 3 << type_shift
    , address_type  = 4 << type_shift
    , float_type    = 5 << type_shift
    , double_type   = 6 << type_shift
    , metadata_type = 7 << type_shift
  };
  friend OprType as_OprType(BasicType t);
  friend BasicType as_BasicType(OprType t);

  OprType type_field_valid() const               { assert(is_register() || is_stack(), "should not be called otherwise"); return (OprType)(value() & type_mask); }
  OprType type_field() const                     { return is_illegal() ? unknown_type : (OprType)(value() & type_mask); }

  static OprSize size_for(BasicType t) {
    switch (t) {
      case T_LONG:
      case T_DOUBLE:
        return double_size;
        break;

      case T_FLOAT:
      case T_BOOLEAN:
      case T_CHAR:
      case T_BYTE:
      case T_SHORT:
      case T_INT:
      case T_ADDRESS:
      case T_OBJECT:
      case T_ARRAY:
      case T_METADATA:
        return single_size;
        break;

      default:
        ShouldNotReachHere();
        return single_size;
      }
  }


  void validate_type() const PRODUCT_RETURN;

  BasicType type() const {
    if (is_pointer()) {
      return pointer()->type();
    }
    return as_BasicType(type_field());
  }


  ValueType* value_type() const                  { return as_ValueType(type()); }

  char type_char() const                         { return type_char((is_pointer()) ? pointer()->type() : type()); }

  bool is_equal(LIR_Opr opr) const         { return this == opr; }
  // checks whether types are same
  bool is_same_type(LIR_Opr opr) const     {
    assert(type_field() != unknown_type &&
           opr->type_field() != unknown_type, "shouldn't see unknown_type");
    return type_field() == opr->type_field();
  }
  bool is_same_register(LIR_Opr opr) {
    return (is_register() && opr->is_register() &&
            kind_field() == opr->kind_field() &&
            (value() & no_type_mask) == (opr->value() & no_type_mask));
  }

  bool is_pointer() const      { return check_value_mask(pointer_mask, pointer_value); }
  bool is_illegal() const      { return kind_field() == illegal_value; }
  bool is_valid() const        { return kind_field() != illegal_value; }

  bool is_register() const     { return is_cpu_register() || is_fpu_register(); }
  bool is_virtual() const      { return is_virtual_cpu()  || is_virtual_fpu();  }

  bool is_constant() const     { return is_pointer() && pointer()->as_constant() != NULL; }
  bool is_address() const      { return is_pointer() && pointer()->as_address() != NULL; }

  bool is_float_kind() const   { return is_pointer() ? pointer()->is_float_kind() : (kind_field() == fpu_register); }
  bool is_oop() const;

  // semantic for fpu- and xmm-registers:
  // * is_float and is_double return true for xmm_registers
  //   (so is_single_fpu and is_single_xmm are true)
  // * So you must always check for is_???_xmm prior to is_???_fpu to
  //   distinguish between fpu- and xmm-registers

  bool is_stack() const        { validate_type(); return check_value_mask(kind_mask,                stack_value);                 }
  bool is_single_stack() const { validate_type(); return check_value_mask(kind_mask | size_mask,    stack_value  | single_size);  }
  bool is_double_stack() const { validate_type(); return check_value_mask(kind_mask | size_mask,    stack_value  | double_size);  }

  bool is_cpu_register() const { validate_type(); return check_value_mask(kind_mask,                cpu_register);                }
  bool is_virtual_cpu() const  { validate_type(); return check_value_mask(kind_mask | virtual_mask, cpu_register | virtual_mask); }
  bool is_fixed_cpu() const    { validate_type(); return check_value_mask(kind_mask | virtual_mask, cpu_register);                }
  bool is_single_cpu() const   { validate_type(); return check_value_mask(kind_mask | size_mask,    cpu_register | single_size);  }
  bool is_double_cpu() const   { validate_type(); return check_value_mask(kind_mask | size_mask,    cpu_register | double_size);  }

  bool is_fpu_register() const { validate_type(); return check_value_mask(kind_mask,                fpu_register);                }
  bool is_virtual_fpu() const  { validate_type(); return check_value_mask(kind_mask | virtual_mask, fpu_register | virtual_mask); }
  bool is_fixed_fpu() const    { validate_type(); return check_value_mask(kind_mask | virtual_mask, fpu_register);                }
  bool is_single_fpu() const   { validate_type(); return check_value_mask(kind_mask | size_mask,    fpu_register | single_size);  }
  bool is_double_fpu() const   { validate_type(); return check_value_mask(kind_mask | size_mask,    fpu_register | double_size);  }

  bool is_xmm_register() const { validate_type(); return check_value_mask(kind_mask | is_xmm_mask,             fpu_register | is_xmm_mask); }
  bool is_single_xmm() const   { validate_type(); return check_value_mask(kind_mask | size_mask | is_xmm_mask, fpu_register | single_size | is_xmm_mask); }
  bool is_double_xmm() const   { validate_type(); return check_value_mask(kind_mask | size_mask | is_xmm_mask, fpu_register | double_size | is_xmm_mask); }

  // fast accessor functions for special bits that do not work for pointers
  // (in this functions, the check for is_pointer() is omitted)
  bool is_single_word() const      { assert(is_register() || is_stack(), "type check"); return check_value_mask(size_mask, single_size); }
  bool is_double_word() const      { assert(is_register() || is_stack(), "type check"); return check_value_mask(size_mask, double_size); }
  bool is_virtual_register() const { assert(is_register(),               "type check"); return check_value_mask(virtual_mask, virtual_mask); }
  bool is_oop_register() const     { assert(is_register() || is_stack(), "type check"); return type_field_valid() == object_type; }
  BasicType type_register() const  { assert(is_register() || is_stack(), "type check"); return as_BasicType(type_field_valid());  }

  bool is_last_use() const         { assert(is_register(), "only works for registers"); return (value() & last_use_mask) != 0; }
  bool is_fpu_stack_offset() const { assert(is_register(), "only works for registers"); return (value() & is_fpu_stack_offset_mask) != 0; }
  LIR_Opr make_last_use()          { assert(is_register(), "only works for registers"); return (LIR_Opr)(value() | last_use_mask); }
  LIR_Opr make_fpu_stack_offset()  { assert(is_register(), "only works for registers"); return (LIR_Opr)(value() | is_fpu_stack_offset_mask); }


  int single_stack_ix() const  { assert(is_single_stack() && !is_virtual(), "type check"); return (int)data(); }
  int double_stack_ix() const  { assert(is_double_stack() && !is_virtual(), "type check"); return (int)data(); }
  RegNr cpu_regnr() const      { assert(is_single_cpu()   && !is_virtual(), "type check"); return (RegNr)data(); }
  RegNr cpu_regnrLo() const    { assert(is_double_cpu()   && !is_virtual(), "type check"); return (RegNr)lo_reg_half(); }
  RegNr cpu_regnrHi() const    { assert(is_double_cpu()   && !is_virtual(), "type check"); return (RegNr)hi_reg_half(); }
  RegNr fpu_regnr() const      { assert(is_single_fpu()   && !is_virtual(), "type check"); return (RegNr)data(); }
  RegNr fpu_regnrLo() const    { assert(is_double_fpu()   && !is_virtual(), "type check"); return (RegNr)lo_reg_half(); }
  RegNr fpu_regnrHi() const    { assert(is_double_fpu()   && !is_virtual(), "type check"); return (RegNr)hi_reg_half(); }
  RegNr xmm_regnr() const      { assert(is_single_xmm()   && !is_virtual(), "type check"); return (RegNr)data(); }
  RegNr xmm_regnrLo() const    { assert(is_double_xmm()   && !is_virtual(), "type check"); return (RegNr)lo_reg_half(); }
  RegNr xmm_regnrHi() const    { assert(is_double_xmm()   && !is_virtual(), "type check"); return (RegNr)hi_reg_half(); }
  int   vreg_number() const    { assert(is_virtual(),                       "type check"); return (RegNr)data(); }

  LIR_OprPtr* pointer()  const                   { assert(is_pointer(), "type check");      return (LIR_OprPtr*)this; }
  LIR_Const* as_constant_ptr() const             { return pointer()->as_constant(); }
  LIR_Address* as_address_ptr() const            { return pointer()->as_address(); }

  Register as_register()    const;
  Register as_register_lo() const;
  Register as_register_hi() const;

  Register as_pointer_register() {
#ifdef _LP64
    if (is_double_cpu()) {
      assert(as_register_lo() == as_register_hi(), "should be a single register");
      return as_register_lo();
    }
#endif
    return as_register();
  }

  FloatRegister as_float_reg   () const;
  FloatRegister as_double_reg  () const;
#ifdef X86
  XMMRegister as_xmm_float_reg () const;
  XMMRegister as_xmm_double_reg() const;
  // for compatibility with RInfo
  int fpu() const { return lo_reg_half(); }
#endif

  jint      as_jint()    const { return as_constant_ptr()->as_jint(); }
  jlong     as_jlong()   const { return as_constant_ptr()->as_jlong(); }
  jfloat    as_jfloat()  const { return as_constant_ptr()->as_jfloat(); }
  jdouble   as_jdouble() const { return as_constant_ptr()->as_jdouble(); }
  jobject   as_jobject() const { return as_constant_ptr()->as_jobject(); }

  void print() const PRODUCT_RETURN;
  void print(outputStream* out) const PRODUCT_RETURN;
};


inline LIR_OprDesc::OprType as_OprType(BasicType type) {
  switch (type) {
  case T_INT:      return LIR_OprDesc::int_type;
  case T_LONG:     return LIR_OprDesc::long_type;
  case T_FLOAT:    return LIR_OprDesc::float_type;
  case T_DOUBLE:   return LIR_OprDesc::double_type;
  case T_OBJECT:
  case T_ARRAY:    return LIR_OprDesc::object_type;
  case T_ADDRESS:  return LIR_OprDesc::address_type;
  case T_METADATA: return LIR_OprDesc::metadata_type;
  case T_ILLEGAL:  // fall through
  default: ShouldNotReachHere(); return LIR_OprDesc::unknown_type;
  }
}

inline BasicType as_BasicType(LIR_OprDesc::OprType t) {
  switch (t) {
  case LIR_OprDesc::int_type:     return T_INT;
  case LIR_OprDesc::long_type:    return T_LONG;
  case LIR_OprDesc::float_type:   return T_FLOAT;
  case LIR_OprDesc::double_type:  return T_DOUBLE;
  case LIR_OprDesc::object_type:  return T_OBJECT;
  case LIR_OprDesc::address_type: return T_ADDRESS;
  case LIR_OprDesc::metadata_type:return T_METADATA;
  case LIR_OprDesc::unknown_type: // fall through
  default: ShouldNotReachHere();  return T_ILLEGAL;
  }
}


// LIR_Address
class LIR_Address: public LIR_OprPtr {
 friend class LIR_OpVisitState;

 public:
  // NOTE: currently these must be the log2 of the scale factor (and
  // must also be equivalent to the ScaleFactor enum in
  // assembler_i486.hpp)
  enum Scale {
    times_1  =  0,
    times_2  =  1,
    times_4  =  2,
    times_8  =  3
  };

 private:
  LIR_Opr   _base;
  LIR_Opr   _index;
  Scale     _scale;
  intx      _disp;
  BasicType _type;

 public:
  LIR_Address(LIR_Opr base, LIR_Opr index, BasicType type):
       _base(base)
     , _index(index)
     , _scale(times_1)
     , _disp(0)
     , _type(type) { verify(); }

  LIR_Address(LIR_Opr base, intx disp, BasicType type):
       _base(base)
     , _index(LIR_OprDesc::illegalOpr())
     , _scale(times_1)
     , _disp(disp)
     , _type(type) { verify(); }

  LIR_Address(LIR_Opr base, BasicType type):
       _base(base)
     , _index(LIR_OprDesc::illegalOpr())
     , _scale(times_1)
     , _disp(0)
     , _type(type) { verify(); }

  LIR_Address(LIR_Opr base, LIR_Opr index, intx disp, BasicType type):
       _base(base)
     , _index(index)
     , _scale(times_1)
     , _disp(disp)
     , _type(type) { verify(); }

  LIR_Address(LIR_Opr base, LIR_Opr index, Scale scale, intx disp, BasicType type):
       _base(base)
     , _index(index)
     , _scale(scale)
     , _disp(disp)
     , _type(type) { verify(); }

  LIR_Opr base()  const                          { return _base;  }
  LIR_Opr index() const                          { return _index; }
  Scale   scale() const                          { return _scale; }
  intx    disp()  const                          { return _disp;  }

  bool equals(LIR_Address* other) const          { return base() == other->base() && index() == other->index() && disp() == other->disp() && scale() == other->scale(); }

  virtual LIR_Address* as_address()              { return this;   }
  virtual BasicType type() const                 { return _type; }
  virtual void print_value_on(outputStream* out) const PRODUCT_RETURN;

  void verify() const PRODUCT_RETURN;

  static Scale scale(BasicType type);
};


// operand factory
class LIR_OprFact: public AllStatic {
 public:

  static LIR_Opr illegalOpr;

  static LIR_Opr single_cpu(int reg) {
    return (LIR_Opr)(intptr_t)((reg  << LIR_OprDesc::reg1_shift) |
                               LIR_OprDesc::int_type             |
                               LIR_OprDesc::cpu_register         |
                               LIR_OprDesc::single_size);
  }
  static LIR_Opr single_cpu_oop(int reg) {
    return (LIR_Opr)(intptr_t)((reg  << LIR_OprDesc::reg1_shift) |
                               LIR_OprDesc::object_type          |
                               LIR_OprDesc::cpu_register         |
                               LIR_OprDesc::single_size);
  }
  static LIR_Opr single_cpu_address(int reg) {
    return (LIR_Opr)(intptr_t)((reg  << LIR_OprDesc::reg1_shift) |
                               LIR_OprDesc::address_type         |
                               LIR_OprDesc::cpu_register         |
                               LIR_OprDesc::single_size);
  }
  static LIR_Opr single_cpu_metadata(int reg) {
    return (LIR_Opr)(intptr_t)((reg  << LIR_OprDesc::reg1_shift) |
                               LIR_OprDesc::metadata_type        |
                               LIR_OprDesc::cpu_register         |
                               LIR_OprDesc::single_size);
  }
  static LIR_Opr double_cpu(int reg1, int reg2) {
    LP64_ONLY(assert(reg1 == reg2, "must be identical"));
    return (LIR_Opr)(intptr_t)((reg1 << LIR_OprDesc::reg1_shift) |
                               (reg2 << LIR_OprDesc::reg2_shift) |
                               LIR_OprDesc::long_type            |
                               LIR_OprDesc::cpu_register         |
                               LIR_OprDesc::double_size);
  }

  static LIR_Opr single_fpu(int reg) {
    return (LIR_Opr)(intptr_t)((reg  << LIR_OprDesc::reg1_shift) |
                               LIR_OprDesc::float_type           |
                               LIR_OprDesc::fpu_register         |
                               LIR_OprDesc::single_size);
  }

  // Platform dependant.
  static LIR_Opr double_fpu(int reg1, int reg2 = -1 /*fnoreg*/);

#ifdef ARM32
  static LIR_Opr single_softfp(int reg) {
    return (LIR_Opr)(intptr_t)((reg  << LIR_OprDesc::reg1_shift) |
                               LIR_OprDesc::float_type           |
                               LIR_OprDesc::cpu_register         |
                               LIR_OprDesc::single_size);
  }
  static LIR_Opr double_softfp(int reg1, int reg2) {
    return (LIR_Opr)(intptr_t)((reg1 << LIR_OprDesc::reg1_shift) |
                               (reg2 << LIR_OprDesc::reg2_shift) |
                               LIR_OprDesc::double_type          |
                               LIR_OprDesc::cpu_register         |
                               LIR_OprDesc::double_size);
  }
#endif // ARM32

#if defined(X86)
  static LIR_Opr single_xmm(int reg) {
    return (LIR_Opr)(intptr_t)((reg << LIR_OprDesc::reg1_shift) |
                               LIR_OprDesc::float_type          |
                               LIR_OprDesc::fpu_register        |
                               LIR_OprDesc::single_size         |
                               LIR_OprDesc::is_xmm_mask);
  }
  static LIR_Opr double_xmm(int reg) {
    return (LIR_Opr)(intptr_t)((reg << LIR_OprDesc::reg1_shift) |
                               (reg << LIR_OprDesc::reg2_shift) |
                               LIR_OprDesc::double_type         |
                               LIR_OprDesc::fpu_register        |
                               LIR_OprDesc::double_size         |
                               LIR_OprDesc::is_xmm_mask);
  }
#endif // X86

  static LIR_Opr virtual_register(int index, BasicType type) {
    if (index > LIR_OprDesc::vreg_max) {
      // Running out of virtual registers. Caller should bailout.
      return illegalOpr;
    }

    LIR_Opr res;
    switch (type) {
      case T_OBJECT: // fall through
      case T_ARRAY:
        res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift)  |
                                            LIR_OprDesc::object_type  |
                                            LIR_OprDesc::cpu_register |
                                            LIR_OprDesc::single_size  |
                                            LIR_OprDesc::virtual_mask);
        break;

      case T_METADATA:
        res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift)  |
                                            LIR_OprDesc::metadata_type|
                                            LIR_OprDesc::cpu_register |
                                            LIR_OprDesc::single_size  |
                                            LIR_OprDesc::virtual_mask);
        break;

      case T_INT:
        res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) |
                                  LIR_OprDesc::int_type              |
                                  LIR_OprDesc::cpu_register          |
                                  LIR_OprDesc::single_size           |
                                  LIR_OprDesc::virtual_mask);
        break;

      case T_ADDRESS:
        res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) |
                                  LIR_OprDesc::address_type          |
                                  LIR_OprDesc::cpu_register          |
                                  LIR_OprDesc::single_size           |
                                  LIR_OprDesc::virtual_mask);
        break;

      case T_LONG:
        res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) |
                                  LIR_OprDesc::long_type             |
                                  LIR_OprDesc::cpu_register          |
                                  LIR_OprDesc::double_size           |
                                  LIR_OprDesc::virtual_mask);
        break;

#ifdef __SOFTFP__
      case T_FLOAT:
        res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) |
                                  LIR_OprDesc::float_type  |
                                  LIR_OprDesc::cpu_register |
                                  LIR_OprDesc::single_size |
                                  LIR_OprDesc::virtual_mask);
        break;
      case T_DOUBLE:
        res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) |
                                  LIR_OprDesc::double_type |
                                  LIR_OprDesc::cpu_register |
                                  LIR_OprDesc::double_size |
                                  LIR_OprDesc::virtual_mask);
        break;
#else // __SOFTFP__
      case T_FLOAT:
        res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) |
                                  LIR_OprDesc::float_type           |
                                  LIR_OprDesc::fpu_register         |
                                  LIR_OprDesc::single_size          |
                                  LIR_OprDesc::virtual_mask);
        break;

      case
        T_DOUBLE: res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) |
                                            LIR_OprDesc::double_type           |
                                            LIR_OprDesc::fpu_register          |
                                            LIR_OprDesc::double_size           |
                                            LIR_OprDesc::virtual_mask);
        break;
#endif // __SOFTFP__
      default:       ShouldNotReachHere(); res = illegalOpr;
    }

#ifdef ASSERT
    res->validate_type();
    assert(res->vreg_number() == index, "conversion check");
    assert(index >= LIR_OprDesc::vreg_base, "must start at vreg_base");
    assert(index <= (max_jint >> LIR_OprDesc::data_shift), "index is too big");

    // old-style calculation; check if old and new method are equal
    LIR_OprDesc::OprType t = as_OprType(type);
#ifdef __SOFTFP__
    LIR_Opr old_res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) |
                               t |
                               LIR_OprDesc::cpu_register |
                               LIR_OprDesc::size_for(type) | LIR_OprDesc::virtual_mask);
#else // __SOFTFP__
    LIR_Opr old_res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) | t |
                                          ((type == T_FLOAT || type == T_DOUBLE) ?  LIR_OprDesc::fpu_register : LIR_OprDesc::cpu_register) |
                               LIR_OprDesc::size_for(type) | LIR_OprDesc::virtual_mask);
    assert(res == old_res, "old and new method not equal");
#endif // __SOFTFP__
#endif // ASSERT

    return res;
  }

  // 'index' is computed by FrameMap::local_stack_pos(index); do not use other parameters as
  // the index is platform independent; a double stack useing indeces 2 and 3 has always
  // index 2.
  static LIR_Opr stack(int index, BasicType type) {
    LIR_Opr res;
    switch (type) {
      case T_OBJECT: // fall through
      case T_ARRAY:
        res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) |
                                  LIR_OprDesc::object_type           |
                                  LIR_OprDesc::stack_value           |
                                  LIR_OprDesc::single_size);
        break;

      case T_METADATA:
        res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) |
                                  LIR_OprDesc::metadata_type         |
                                  LIR_OprDesc::stack_value           |
                                  LIR_OprDesc::single_size);
        break;
      case T_INT:
        res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) |
                                  LIR_OprDesc::int_type              |
                                  LIR_OprDesc::stack_value           |
                                  LIR_OprDesc::single_size);
        break;

      case T_ADDRESS:
        res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) |
                                  LIR_OprDesc::address_type          |
                                  LIR_OprDesc::stack_value           |
                                  LIR_OprDesc::single_size);
        break;

      case T_LONG:
        res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) |
                                  LIR_OprDesc::long_type             |
                                  LIR_OprDesc::stack_value           |
                                  LIR_OprDesc::double_size);
        break;

      case T_FLOAT:
        res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) |
                                  LIR_OprDesc::float_type            |
                                  LIR_OprDesc::stack_value           |
                                  LIR_OprDesc::single_size);
        break;
      case T_DOUBLE:
        res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) |
                                  LIR_OprDesc::double_type           |
                                  LIR_OprDesc::stack_value           |
                                  LIR_OprDesc::double_size);
        break;

      default:       ShouldNotReachHere(); res = illegalOpr;
    }

#ifdef ASSERT
    assert(index >= 0, "index must be positive");
    assert(index <= (max_jint >> LIR_OprDesc::data_shift), "index is too big");

    LIR_Opr old_res = (LIR_Opr)(intptr_t)((index << LIR_OprDesc::data_shift) |
                                          LIR_OprDesc::stack_value           |
                                          as_OprType(type)                   |
                                          LIR_OprDesc::size_for(type));
    assert(res == old_res, "old and new method not equal");
#endif

    return res;
  }

  static LIR_Opr intConst(jint i)                { return (LIR_Opr)(new LIR_Const(i)); }
  static LIR_Opr longConst(jlong l)              { return (LIR_Opr)(new LIR_Const(l)); }
  static LIR_Opr floatConst(jfloat f)            { return (LIR_Opr)(new LIR_Const(f)); }
  static LIR_Opr doubleConst(jdouble d)          { return (LIR_Opr)(new LIR_Const(d)); }
  static LIR_Opr oopConst(jobject o)             { return (LIR_Opr)(new LIR_Const(o)); }
  static LIR_Opr address(LIR_Address* a)         { return (LIR_Opr)a; }
  static LIR_Opr intptrConst(void* p)            { return (LIR_Opr)(new LIR_Const(p)); }
  static LIR_Opr intptrConst(intptr_t v)         { return (LIR_Opr)(new LIR_Const((void*)v)); }
  static LIR_Opr illegal()                       { return (LIR_Opr)-1; }
  static LIR_Opr addressConst(jint i)            { return (LIR_Opr)(new LIR_Const(i, true)); }
  static LIR_Opr metadataConst(Metadata* m)      { return (LIR_Opr)(new LIR_Const(m)); }

  static LIR_Opr value_type(ValueType* type);
};


//-------------------------------------------------------------------------------
//                   LIR Instructions
//-------------------------------------------------------------------------------
//
// Note:
//  - every instruction has a result operand
//  - every instruction has an CodeEmitInfo operand (can be revisited later)
//  - every instruction has a LIR_OpCode operand
//  - LIR_OpN, means an instruction that has N input operands
//
// class hierarchy:
//
class  LIR_Op;
class    LIR_Op0;
class      LIR_OpLabel;
class    LIR_Op1;
class      LIR_OpBranch;
class      LIR_OpConvert;
class      LIR_OpAllocObj;
class      LIR_OpReturn;
class      LIR_OpRoundFP;
class    LIR_Op2;
class    LIR_OpDelay;
class    LIR_Op3;
class      LIR_OpAllocArray;
class    LIR_OpCall;
class      LIR_OpJavaCall;
class      LIR_OpRTCall;
class    LIR_OpArrayCopy;
class    LIR_OpUpdateCRC32;
class    LIR_OpLock;
class    LIR_OpTypeCheck;
class    LIR_OpCompareAndSwap;
class    LIR_OpProfileCall;
class    LIR_OpProfileType;
#ifdef ASSERT
class    LIR_OpAssert;
#endif

// LIR operation codes
enum LIR_Code {
    lir_none
  , begin_op0
      , lir_label
      , lir_nop
      , lir_std_entry
      , lir_osr_entry
      , lir_fpop_raw
      , lir_breakpoint
      , lir_rtcall
      , lir_membar
      , lir_membar_acquire
      , lir_membar_release
      , lir_membar_loadload
      , lir_membar_storestore
      , lir_membar_loadstore
      , lir_membar_storeload
      , lir_get_thread
      , lir_on_spin_wait
  , end_op0
  , begin_op1
      , lir_fxch
      , lir_fld
      , lir_push
      , lir_pop
      , lir_null_check
      , lir_return
      , lir_leal
      , lir_branch
      , lir_cond_float_branch
      , lir_move
      , lir_convert
      , lir_alloc_object
      , lir_monaddr
      , lir_roundfp
      , lir_safepoint
      , lir_unwind
  , end_op1
  , begin_op2
      , lir_cmp
      , lir_cmp_l2i
      , lir_ucmp_fd2i
      , lir_cmp_fd2i
      , lir_cmove
      , lir_add
      , lir_sub
      , lir_mul
      , lir_div
      , lir_rem
      , lir_sqrt
      , lir_abs
      , lir_neg
      , lir_tan
      , lir_log10
      , lir_logic_and
      , lir_logic_or
      , lir_logic_xor
      , lir_shl
      , lir_shr
      , lir_ushr
      , lir_alloc_array
      , lir_throw
      , lir_xadd
      , lir_xchg
  , end_op2
  , begin_op3
      , lir_idiv
      , lir_irem
      , lir_fmad
      , lir_fmaf
  , end_op3
  , begin_opJavaCall
      , lir_static_call
      , lir_optvirtual_call
      , lir_icvirtual_call
      , lir_dynamic_call
  , end_opJavaCall
  , begin_opArrayCopy
      , lir_arraycopy
  , end_opArrayCopy
  , begin_opUpdateCRC32
      , lir_updatecrc32
  , end_opUpdateCRC32
  , begin_opLock
    , lir_lock
    , lir_unlock
  , end_opLock
  , begin_delay_slot
    , lir_delay_slot
  , end_delay_slot
  , begin_opTypeCheck
    , lir_instanceof
    , lir_checkcast
    , lir_store_check
  , end_opTypeCheck
  , begin_opCompareAndSwap
    , lir_cas_long
    , lir_cas_obj
    , lir_cas_int
  , end_opCompareAndSwap
  , begin_opMDOProfile
    , lir_profile_call
    , lir_profile_type
  , end_opMDOProfile
  , begin_opAssert
    , lir_assert
  , end_opAssert
};


enum LIR_Condition {
    lir_cond_equal
  , lir_cond_notEqual
  , lir_cond_less
  , lir_cond_lessEqual
  , lir_cond_greaterEqual
  , lir_cond_greater
  , lir_cond_belowEqual
  , lir_cond_aboveEqual
  , lir_cond_always
  , lir_cond_unknown = -1
};


enum LIR_PatchCode {
  lir_patch_none,
  lir_patch_low,
  lir_patch_high,
  lir_patch_normal
};


enum LIR_MoveKind {
  lir_move_normal,
  lir_move_volatile,
  lir_move_wide,
  lir_move_max_flag
};


// --------------------------------------------------
// LIR_Op
// --------------------------------------------------
class LIR_Op: public CompilationResourceObj {
 friend class LIR_OpVisitState;

#ifdef ASSERT
 private:
  const char *  _file;
  int           _line;
#endif

 protected:
  LIR_Opr       _result;
  unsigned short _code;
  unsigned short _flags;
  CodeEmitInfo* _info;
  int           _id;     // value id for register allocation
  int           _fpu_pop_count;
  Instruction*  _source; // for debugging

  static void print_condition(outputStream* out, LIR_Condition cond) PRODUCT_RETURN;

 protected:
  static bool is_in_range(LIR_Code test, LIR_Code start, LIR_Code end)  { return start < test && test < end; }

 public:
  LIR_Op()
    :
#ifdef ASSERT
      _file(NULL)
    , _line(0),
#endif
      _result(LIR_OprFact::illegalOpr)
    , _code(lir_none)
    , _flags(0)
    , _info(NULL)
    , _id(-1)
    , _fpu_pop_count(0)
    , _source(NULL) {}

  LIR_Op(LIR_Code code, LIR_Opr result, CodeEmitInfo* info)
    :
#ifdef ASSERT
      _file(NULL)
    , _line(0),
#endif
      _result(result)
    , _code(code)
    , _flags(0)
    , _info(info)
    , _id(-1)
    , _fpu_pop_count(0)
    , _source(NULL) {}

  CodeEmitInfo* info() const                  { return _info;   }
  LIR_Code code()      const                  { return (LIR_Code)_code;   }
  LIR_Opr result_opr() const                  { return _result; }
  void    set_result_opr(LIR_Opr opr)         { _result = opr;  }

#ifdef ASSERT
  void set_file_and_line(const char * file, int line) {
    _file = file;
    _line = line;
  }
#endif

  virtual const char * name() const PRODUCT_RETURN0;
  virtual void visit(LIR_OpVisitState* state);

  int id()             const                  { return _id;     }
  void set_id(int id)                         { _id = id; }

  // FPU stack simulation helpers -- only used on Intel
  void set_fpu_pop_count(int count)           { assert(count >= 0 && count <= 1, "currently only 0 and 1 are valid"); _fpu_pop_count = count; }
  int  fpu_pop_count() const                  { return _fpu_pop_count; }
  bool pop_fpu_stack()                        { return _fpu_pop_count > 0; }

  Instruction* source() const                 { return _source; }
  void set_source(Instruction* ins)           { _source = ins; }

  virtual void emit_code(LIR_Assembler* masm) = 0;
  virtual void print_instr(outputStream* out) const   = 0;
  virtual void print_on(outputStream* st) const PRODUCT_RETURN;

  virtual bool is_patching() { return false; }
  virtual LIR_OpCall* as_OpCall() { return NULL; }
  virtual LIR_OpJavaCall* as_OpJavaCall() { return NULL; }
  virtual LIR_OpLabel* as_OpLabel() { return NULL; }
  virtual LIR_OpDelay* as_OpDelay() { return NULL; }
  virtual LIR_OpLock* as_OpLock() { return NULL; }
  virtual LIR_OpAllocArray* as_OpAllocArray() { return NULL; }
  virtual LIR_OpAllocObj* as_OpAllocObj() { return NULL; }
  virtual LIR_OpRoundFP* as_OpRoundFP() { return NULL; }
  virtual LIR_OpBranch* as_OpBranch() { return NULL; }
  virtual LIR_OpReturn* as_OpReturn() { return NULL; }
  virtual LIR_OpRTCall* as_OpRTCall() { return NULL; }
  virtual LIR_OpConvert* as_OpConvert() { return NULL; }
  virtual LIR_Op0* as_Op0() { return NULL; }
  virtual LIR_Op1* as_Op1() { return NULL; }
  virtual LIR_Op2* as_Op2() { return NULL; }
  virtual LIR_Op3* as_Op3() { return NULL; }
  virtual LIR_OpArrayCopy* as_OpArrayCopy() { return NULL; }
  virtual LIR_OpUpdateCRC32* as_OpUpdateCRC32() { return NULL; }
  virtual LIR_OpTypeCheck* as_OpTypeCheck() { return NULL; }
  virtual LIR_OpCompareAndSwap* as_OpCompareAndSwap() { return NULL; }
  virtual LIR_OpProfileCall* as_OpProfileCall() { return NULL; }
  virtual LIR_OpProfileType* as_OpProfileType() { return NULL; }
#ifdef ASSERT
  virtual LIR_OpAssert* as_OpAssert() { return NULL; }
#endif

  virtual void verify() const {}
};

// for calls
class LIR_OpCall: public LIR_Op {
 friend class LIR_OpVisitState;

 protected:
  address      _addr;
  LIR_OprList* _arguments;
 protected:
  LIR_OpCall(LIR_Code code, address addr, LIR_Opr result,
             LIR_OprList* arguments, CodeEmitInfo* info = NULL)
    : LIR_Op(code, result, info)
    , _addr(addr)
    , _arguments(arguments) {}

 public:
  address addr() const                           { return _addr; }
  const LIR_OprList* arguments() const           { return _arguments; }
  virtual LIR_OpCall* as_OpCall()                { return this; }
};


// --------------------------------------------------
// LIR_OpJavaCall
// --------------------------------------------------
class LIR_OpJavaCall: public LIR_OpCall {
 friend class LIR_OpVisitState;

 private:
  ciMethod* _method;
  LIR_Opr   _receiver;
  LIR_Opr   _method_handle_invoke_SP_save_opr;  // Used in LIR_OpVisitState::visit to store the reference to FrameMap::method_handle_invoke_SP_save_opr.

 public:
  LIR_OpJavaCall(LIR_Code code, ciMethod* method,
                 LIR_Opr receiver, LIR_Opr result,
                 address addr, LIR_OprList* arguments,
                 CodeEmitInfo* info)
  : LIR_OpCall(code, addr, result, arguments, info)
  , _method(method)
  , _receiver(receiver)
  , _method_handle_invoke_SP_save_opr(LIR_OprFact::illegalOpr)
  { assert(is_in_range(code, begin_opJavaCall, end_opJavaCall), "code check"); }

  LIR_OpJavaCall(LIR_Code code, ciMethod* method,
                 LIR_Opr receiver, LIR_Opr result, intptr_t vtable_offset,
                 LIR_OprList* arguments, CodeEmitInfo* info)
  : LIR_OpCall(code, (address)vtable_offset, result, arguments, info)
  , _method(method)
  , _receiver(receiver)
  , _method_handle_invoke_SP_save_opr(LIR_OprFact::illegalOpr)
  { assert(is_in_range(code, begin_opJavaCall, end_opJavaCall), "code check"); }

  LIR_Opr receiver() const                       { return _receiver; }
  ciMethod* method() const                       { return _method;   }

  // JSR 292 support.
  bool is_invokedynamic() const                  { return code() == lir_dynamic_call; }
  bool is_method_handle_invoke() const {
    return method()->is_compiled_lambda_form() ||   // Java-generated lambda form
           method()->is_method_handle_intrinsic();  // JVM-generated MH intrinsic
  }

  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_OpJavaCall* as_OpJavaCall() { return this; }
  virtual void print_instr(outputStream* out) const PRODUCT_RETURN;
};

// --------------------------------------------------
// LIR_OpLabel
// --------------------------------------------------
// Location where a branch can continue
class LIR_OpLabel: public LIR_Op {
 friend class LIR_OpVisitState;

 private:
  Label* _label;
 public:
  LIR_OpLabel(Label* lbl)
   : LIR_Op(lir_label, LIR_OprFact::illegalOpr, NULL)
   , _label(lbl)                                 {}
  Label* label() const                           { return _label; }

  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_OpLabel* as_OpLabel() { return this; }
  virtual void print_instr(outputStream* out) const PRODUCT_RETURN;
};

// LIR_OpArrayCopy
class LIR_OpArrayCopy: public LIR_Op {
 friend class LIR_OpVisitState;

 private:
  ArrayCopyStub*  _stub;
  LIR_Opr   _src;
  LIR_Opr   _src_pos;
  LIR_Opr   _dst;
  LIR_Opr   _dst_pos;
  LIR_Opr   _length;
  LIR_Opr   _tmp;
  ciArrayKlass* _expected_type;
  int       _flags;

public:
  enum Flags {
    src_null_check         = 1 << 0,
    dst_null_check         = 1 << 1,
    src_pos_positive_check = 1 << 2,
    dst_pos_positive_check = 1 << 3,
    length_positive_check  = 1 << 4,
    src_range_check        = 1 << 5,
    dst_range_check        = 1 << 6,
    type_check             = 1 << 7,
    overlapping            = 1 << 8,
    unaligned              = 1 << 9,
    src_objarray           = 1 << 10,
    dst_objarray           = 1 << 11,
    all_flags              = (1 << 12) - 1
  };

  LIR_OpArrayCopy(LIR_Opr src, LIR_Opr src_pos, LIR_Opr dst, LIR_Opr dst_pos, LIR_Opr length, LIR_Opr tmp,
                  ciArrayKlass* expected_type, int flags, CodeEmitInfo* info);

  LIR_Opr src() const                            { return _src; }
  LIR_Opr src_pos() const                        { return _src_pos; }
  LIR_Opr dst() const                            { return _dst; }
  LIR_Opr dst_pos() const                        { return _dst_pos; }
  LIR_Opr length() const                         { return _length; }
  LIR_Opr tmp() const                            { return _tmp; }
  int flags() const                              { return _flags; }
  ciArrayKlass* expected_type() const            { return _expected_type; }
  ArrayCopyStub* stub() const                    { return _stub; }

  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_OpArrayCopy* as_OpArrayCopy() { return this; }
  void print_instr(outputStream* out) const PRODUCT_RETURN;
};

// LIR_OpUpdateCRC32
class LIR_OpUpdateCRC32: public LIR_Op {
  friend class LIR_OpVisitState;

private:
  LIR_Opr   _crc;
  LIR_Opr   _val;

public:

  LIR_OpUpdateCRC32(LIR_Opr crc, LIR_Opr val, LIR_Opr res);

  LIR_Opr crc() const                            { return _crc; }
  LIR_Opr val() const                            { return _val; }

  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_OpUpdateCRC32* as_OpUpdateCRC32()  { return this; }
  void print_instr(outputStream* out) const PRODUCT_RETURN;
};

// --------------------------------------------------
// LIR_Op0
// --------------------------------------------------
class LIR_Op0: public LIR_Op {
 friend class LIR_OpVisitState;

 public:
  LIR_Op0(LIR_Code code)
   : LIR_Op(code, LIR_OprFact::illegalOpr, NULL)  { assert(is_in_range(code, begin_op0, end_op0), "code check"); }
  LIR_Op0(LIR_Code code, LIR_Opr result, CodeEmitInfo* info = NULL)
   : LIR_Op(code, result, info)  { assert(is_in_range(code, begin_op0, end_op0), "code check"); }

  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_Op0* as_Op0() { return this; }
  virtual void print_instr(outputStream* out) const PRODUCT_RETURN;
};


// --------------------------------------------------
// LIR_Op1
// --------------------------------------------------

class LIR_Op1: public LIR_Op {
 friend class LIR_OpVisitState;

 protected:
  LIR_Opr         _opr;   // input operand
  BasicType       _type;  // Operand types
  LIR_PatchCode   _patch; // only required with patchin (NEEDS_CLEANUP: do we want a special instruction for patching?)

  static void print_patch_code(outputStream* out, LIR_PatchCode code);

  void set_kind(LIR_MoveKind kind) {
    assert(code() == lir_move, "must be");
    _flags = kind;
  }

 public:
  LIR_Op1(LIR_Code code, LIR_Opr opr, LIR_Opr result = LIR_OprFact::illegalOpr, BasicType type = T_ILLEGAL, LIR_PatchCode patch = lir_patch_none, CodeEmitInfo* info = NULL)
    : LIR_Op(code, result, info)
    , _opr(opr)
    , _type(type)
    , _patch(patch)                    { assert(is_in_range(code, begin_op1, end_op1), "code check"); }

  LIR_Op1(LIR_Code code, LIR_Opr opr, LIR_Opr result, BasicType type, LIR_PatchCode patch, CodeEmitInfo* info, LIR_MoveKind kind)
    : LIR_Op(code, result, info)
    , _opr(opr)
    , _type(type)
    , _patch(patch)                    {
    assert(code == lir_move, "must be");
    set_kind(kind);
  }

  LIR_Op1(LIR_Code code, LIR_Opr opr, CodeEmitInfo* info)
    : LIR_Op(code, LIR_OprFact::illegalOpr, info)
    , _opr(opr)
    , _type(T_ILLEGAL)
    , _patch(lir_patch_none)           { assert(is_in_range(code, begin_op1, end_op1), "code check"); }

  LIR_Opr in_opr()           const               { return _opr;   }
  LIR_PatchCode patch_code() const               { return _patch; }
  BasicType type()           const               { return _type;  }

  LIR_MoveKind move_kind() const {
    assert(code() == lir_move, "must be");
    return (LIR_MoveKind)_flags;
  }

  virtual bool is_patching() { return _patch != lir_patch_none; }
  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_Op1* as_Op1() { return this; }
  virtual const char * name() const PRODUCT_RETURN0;

  void set_in_opr(LIR_Opr opr) { _opr = opr; }

  virtual void print_instr(outputStream* out) const PRODUCT_RETURN;
  virtual void verify() const;
};


// for runtime calls
class LIR_OpRTCall: public LIR_OpCall {
 friend class LIR_OpVisitState;

 private:
  LIR_Opr _tmp;
 public:
  LIR_OpRTCall(address addr, LIR_Opr tmp,
               LIR_Opr result, LIR_OprList* arguments, CodeEmitInfo* info = NULL)
    : LIR_OpCall(lir_rtcall, addr, result, arguments, info)
    , _tmp(tmp) {}

  virtual void print_instr(outputStream* out) const PRODUCT_RETURN;
  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_OpRTCall* as_OpRTCall() { return this; }

  LIR_Opr tmp() const                            { return _tmp; }

  virtual void verify() const;
};


class LIR_OpBranch: public LIR_Op {
 friend class LIR_OpVisitState;

 private:
  LIR_Condition _cond;
  Label*        _label;
  BlockBegin*   _block;  // if this is a branch to a block, this is the block
  BlockBegin*   _ublock; // if this is a float-branch, this is the unorderd block
  CodeStub*     _stub;   // if this is a branch to a stub, this is the stub

 public:
  LIR_OpBranch(LIR_Condition cond, Label* lbl)
    : LIR_Op(lir_branch, LIR_OprFact::illegalOpr, (CodeEmitInfo*) NULL)
    , _cond(cond)
    , _label(lbl)
    , _block(NULL)
    , _ublock(NULL)
    , _stub(NULL) { }

  LIR_OpBranch(LIR_Condition cond, BlockBegin* block);
  LIR_OpBranch(LIR_Condition cond, CodeStub* stub);

  // for unordered comparisons
  LIR_OpBranch(LIR_Condition cond, BlockBegin* block, BlockBegin* ublock);

  LIR_Condition cond()        const              { return _cond;        }
  Label*        label()       const              { return _label;       }
  BlockBegin*   block()       const              { return _block;       }
  BlockBegin*   ublock()      const              { return _ublock;      }
  CodeStub*     stub()        const              { return _stub;       }

  void          change_block(BlockBegin* b);
  void          change_ublock(BlockBegin* b);
  void          negate_cond();

  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_OpBranch* as_OpBranch() { return this; }
  virtual void print_instr(outputStream* out) const PRODUCT_RETURN;
};

class LIR_OpReturn: public LIR_Op1 {
 friend class LIR_OpVisitState;

 private:
  C1SafepointPollStub* _stub;

 public:
  LIR_OpReturn(LIR_Opr opr);

  C1SafepointPollStub* stub() const { return _stub; }
  virtual LIR_OpReturn* as_OpReturn() { return this; }
};

class ConversionStub;

class LIR_OpConvert: public LIR_Op1 {
 friend class LIR_OpVisitState;

 private:
   Bytecodes::Code _bytecode;
   ConversionStub* _stub;

 public:
   LIR_OpConvert(Bytecodes::Code code, LIR_Opr opr, LIR_Opr result, ConversionStub* stub)
     : LIR_Op1(lir_convert, opr, result)
     , _bytecode(code)
     , _stub(stub)                               {}

  Bytecodes::Code bytecode() const               { return _bytecode; }
  ConversionStub* stub() const                   { return _stub; }

  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_OpConvert* as_OpConvert() { return this; }
  virtual void print_instr(outputStream* out) const PRODUCT_RETURN;

  static void print_bytecode(outputStream* out, Bytecodes::Code code) PRODUCT_RETURN;
};


// LIR_OpAllocObj
class LIR_OpAllocObj : public LIR_Op1 {
 friend class LIR_OpVisitState;

 private:
  LIR_Opr _tmp1;
  LIR_Opr _tmp2;
  LIR_Opr _tmp3;
  LIR_Opr _tmp4;
  int     _hdr_size;
  int     _obj_size;
  CodeStub* _stub;
  bool    _init_check;

 public:
  LIR_OpAllocObj(LIR_Opr klass, LIR_Opr result,
                 LIR_Opr t1, LIR_Opr t2, LIR_Opr t3, LIR_Opr t4,
                 int hdr_size, int obj_size, bool init_check, CodeStub* stub)
    : LIR_Op1(lir_alloc_object, klass, result)
    , _tmp1(t1)
    , _tmp2(t2)
    , _tmp3(t3)
    , _tmp4(t4)
    , _hdr_size(hdr_size)
    , _obj_size(obj_size)
    , _stub(stub)
    , _init_check(init_check)                    { }

  LIR_Opr klass()        const                   { return in_opr();     }
  LIR_Opr obj()          const                   { return result_opr(); }
  LIR_Opr tmp1()         const                   { return _tmp1;        }
  LIR_Opr tmp2()         const                   { return _tmp2;        }
  LIR_Opr tmp3()         const                   { return _tmp3;        }
  LIR_Opr tmp4()         const                   { return _tmp4;        }
  int     header_size()  const                   { return _hdr_size;    }
  int     object_size()  const                   { return _obj_size;    }
  bool    init_check()   const                   { return _init_check;  }
  CodeStub* stub()       const                   { return _stub;        }

  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_OpAllocObj * as_OpAllocObj () { return this; }
  virtual void print_instr(outputStream* out) const PRODUCT_RETURN;
};


// LIR_OpRoundFP
class LIR_OpRoundFP : public LIR_Op1 {
 friend class LIR_OpVisitState;

 private:
  LIR_Opr _tmp;

 public:
  LIR_OpRoundFP(LIR_Opr reg, LIR_Opr stack_loc_temp, LIR_Opr result)
    : LIR_Op1(lir_roundfp, reg, result)
    , _tmp(stack_loc_temp) {}

  LIR_Opr tmp() const                            { return _tmp; }
  virtual LIR_OpRoundFP* as_OpRoundFP()          { return this; }
  void print_instr(outputStream* out) const PRODUCT_RETURN;
};

// LIR_OpTypeCheck
class LIR_OpTypeCheck: public LIR_Op {
 friend class LIR_OpVisitState;

 private:
  LIR_Opr       _object;
  LIR_Opr       _array;
  ciKlass*      _klass;
  LIR_Opr       _tmp1;
  LIR_Opr       _tmp2;
  LIR_Opr       _tmp3;
  bool          _fast_check;
  CodeEmitInfo* _info_for_patch;
  CodeEmitInfo* _info_for_exception;
  CodeStub*     _stub;
  ciMethod*     _profiled_method;
  int           _profiled_bci;
  bool          _should_profile;

public:
  LIR_OpTypeCheck(LIR_Code code, LIR_Opr result, LIR_Opr object, ciKlass* klass,
                  LIR_Opr tmp1, LIR_Opr tmp2, LIR_Opr tmp3, bool fast_check,
                  CodeEmitInfo* info_for_exception, CodeEmitInfo* info_for_patch, CodeStub* stub);
  LIR_OpTypeCheck(LIR_Code code, LIR_Opr object, LIR_Opr array,
                  LIR_Opr tmp1, LIR_Opr tmp2, LIR_Opr tmp3, CodeEmitInfo* info_for_exception);

  LIR_Opr object() const                         { return _object;         }
  LIR_Opr array() const                          { assert(code() == lir_store_check, "not valid"); return _array;         }
  LIR_Opr tmp1() const                           { return _tmp1;           }
  LIR_Opr tmp2() const                           { return _tmp2;           }
  LIR_Opr tmp3() const                           { return _tmp3;           }
  ciKlass* klass() const                         { assert(code() == lir_instanceof || code() == lir_checkcast, "not valid"); return _klass;          }
  bool fast_check() const                        { assert(code() == lir_instanceof || code() == lir_checkcast, "not valid"); return _fast_check;     }
  CodeEmitInfo* info_for_patch() const           { return _info_for_patch;  }
  CodeEmitInfo* info_for_exception() const       { return _info_for_exception; }
  CodeStub* stub() const                         { return _stub;           }

  // MethodData* profiling
  void set_profiled_method(ciMethod *method)     { _profiled_method = method; }
  void set_profiled_bci(int bci)                 { _profiled_bci = bci;       }
  void set_should_profile(bool b)                { _should_profile = b;       }
  ciMethod* profiled_method() const              { return _profiled_method;   }
  int       profiled_bci() const                 { return _profiled_bci;      }
  bool      should_profile() const               { return _should_profile;    }

  virtual bool is_patching() { return _info_for_patch != NULL; }
  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_OpTypeCheck* as_OpTypeCheck() { return this; }
  void print_instr(outputStream* out) const PRODUCT_RETURN;
};

// LIR_Op2
class LIR_Op2: public LIR_Op {
 friend class LIR_OpVisitState;

  int  _fpu_stack_size; // for sin/cos implementation on Intel

 protected:
  LIR_Opr   _opr1;
  LIR_Opr   _opr2;
  BasicType _type;
  LIR_Opr   _tmp1;
  LIR_Opr   _tmp2;
  LIR_Opr   _tmp3;
  LIR_Opr   _tmp4;
  LIR_Opr   _tmp5;
  LIR_Condition _condition;

  void verify() const;

 public:
  LIR_Op2(LIR_Code code, LIR_Condition condition, LIR_Opr opr1, LIR_Opr opr2, CodeEmitInfo* info = NULL)
    : LIR_Op(code, LIR_OprFact::illegalOpr, info)
    , _fpu_stack_size(0)
    , _opr1(opr1)
    , _opr2(opr2)
    , _type(T_ILLEGAL)
    , _tmp1(LIR_OprFact::illegalOpr)
    , _tmp2(LIR_OprFact::illegalOpr)
    , _tmp3(LIR_OprFact::illegalOpr)
    , _tmp4(LIR_OprFact::illegalOpr)
    , _tmp5(LIR_OprFact::illegalOpr)
    , _condition(condition) {
    assert(code == lir_cmp || code == lir_assert, "code check");
  }

  LIR_Op2(LIR_Code code, LIR_Condition condition, LIR_Opr opr1, LIR_Opr opr2, LIR_Opr result, BasicType type)
    : LIR_Op(code, result, NULL)
    , _fpu_stack_size(0)
    , _opr1(opr1)
    , _opr2(opr2)
    , _type(type)
    , _tmp1(LIR_OprFact::illegalOpr)
    , _tmp2(LIR_OprFact::illegalOpr)
    , _tmp3(LIR_OprFact::illegalOpr)
    , _tmp4(LIR_OprFact::illegalOpr)
    , _tmp5(LIR_OprFact::illegalOpr)
    , _condition(condition) {
    assert(code == lir_cmove, "code check");
    assert(type != T_ILLEGAL, "cmove should have type");
  }

  LIR_Op2(LIR_Code code, LIR_Opr opr1, LIR_Opr opr2, LIR_Opr result = LIR_OprFact::illegalOpr,
          CodeEmitInfo* info = NULL, BasicType type = T_ILLEGAL)
    : LIR_Op(code, result, info)
    , _fpu_stack_size(0)
    , _opr1(opr1)
    , _opr2(opr2)
    , _type(type)
    , _tmp1(LIR_OprFact::illegalOpr)
    , _tmp2(LIR_OprFact::illegalOpr)
    , _tmp3(LIR_OprFact::illegalOpr)
    , _tmp4(LIR_OprFact::illegalOpr)
    , _tmp5(LIR_OprFact::illegalOpr)
    , _condition(lir_cond_unknown) {
    assert(code != lir_cmp && is_in_range(code, begin_op2, end_op2), "code check");
  }

  LIR_Op2(LIR_Code code, LIR_Opr opr1, LIR_Opr opr2, LIR_Opr result, LIR_Opr tmp1, LIR_Opr tmp2 = LIR_OprFact::illegalOpr,
          LIR_Opr tmp3 = LIR_OprFact::illegalOpr, LIR_Opr tmp4 = LIR_OprFact::illegalOpr, LIR_Opr tmp5 = LIR_OprFact::illegalOpr)
    : LIR_Op(code, result, NULL)
    , _fpu_stack_size(0)
    , _opr1(opr1)
    , _opr2(opr2)
    , _type(T_ILLEGAL)
    , _tmp1(tmp1)
    , _tmp2(tmp2)
    , _tmp3(tmp3)
    , _tmp4(tmp4)
    , _tmp5(tmp5)
    , _condition(lir_cond_unknown) {
    assert(code != lir_cmp && is_in_range(code, begin_op2, end_op2), "code check");
  }

  LIR_Opr in_opr1() const                        { return _opr1; }
  LIR_Opr in_opr2() const                        { return _opr2; }
  BasicType type()  const                        { return _type; }
  LIR_Opr tmp1_opr() const                       { return _tmp1; }
  LIR_Opr tmp2_opr() const                       { return _tmp2; }
  LIR_Opr tmp3_opr() const                       { return _tmp3; }
  LIR_Opr tmp4_opr() const                       { return _tmp4; }
  LIR_Opr tmp5_opr() const                       { return _tmp5; }
  LIR_Condition condition() const  {
    assert(code() == lir_cmp || code() == lir_cmove || code() == lir_assert, "only valid for cmp and cmove and assert"); return _condition;
  }
  void set_condition(LIR_Condition condition) {
    assert(code() == lir_cmp || code() == lir_cmove, "only valid for cmp and cmove");  _condition = condition;
  }

  void set_fpu_stack_size(int size)              { _fpu_stack_size = size; }
  int  fpu_stack_size() const                    { return _fpu_stack_size; }

  void set_in_opr1(LIR_Opr opr)                  { _opr1 = opr; }
  void set_in_opr2(LIR_Opr opr)                  { _opr2 = opr; }

  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_Op2* as_Op2() { return this; }
  virtual void print_instr(outputStream* out) const PRODUCT_RETURN;
};

class LIR_OpAllocArray : public LIR_Op {
 friend class LIR_OpVisitState;

 private:
  LIR_Opr   _klass;
  LIR_Opr   _len;
  LIR_Opr   _tmp1;
  LIR_Opr   _tmp2;
  LIR_Opr   _tmp3;
  LIR_Opr   _tmp4;
  BasicType _type;
  CodeStub* _stub;

 public:
  LIR_OpAllocArray(LIR_Opr klass, LIR_Opr len, LIR_Opr result, LIR_Opr t1, LIR_Opr t2, LIR_Opr t3, LIR_Opr t4, BasicType type, CodeStub* stub)
    : LIR_Op(lir_alloc_array, result, NULL)
    , _klass(klass)
    , _len(len)
    , _tmp1(t1)
    , _tmp2(t2)
    , _tmp3(t3)
    , _tmp4(t4)
    , _type(type)
    , _stub(stub) {}

  LIR_Opr   klass()   const                      { return _klass;       }
  LIR_Opr   len()     const                      { return _len;         }
  LIR_Opr   obj()     const                      { return result_opr(); }
  LIR_Opr   tmp1()    const                      { return _tmp1;        }
  LIR_Opr   tmp2()    const                      { return _tmp2;        }
  LIR_Opr   tmp3()    const                      { return _tmp3;        }
  LIR_Opr   tmp4()    const                      { return _tmp4;        }
  BasicType type()    const                      { return _type;        }
  CodeStub* stub()    const                      { return _stub;        }

  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_OpAllocArray * as_OpAllocArray () { return this; }
  virtual void print_instr(outputStream* out) const PRODUCT_RETURN;
};


class LIR_Op3: public LIR_Op {
 friend class LIR_OpVisitState;

 private:
  LIR_Opr _opr1;
  LIR_Opr _opr2;
  LIR_Opr _opr3;
 public:
  LIR_Op3(LIR_Code code, LIR_Opr opr1, LIR_Opr opr2, LIR_Opr opr3, LIR_Opr result, CodeEmitInfo* info = NULL)
    : LIR_Op(code, result, info)
    , _opr1(opr1)
    , _opr2(opr2)
    , _opr3(opr3)                                { assert(is_in_range(code, begin_op3, end_op3), "code check"); }
  LIR_Opr in_opr1() const                        { return _opr1; }
  LIR_Opr in_opr2() const                        { return _opr2; }
  LIR_Opr in_opr3() const                        { return _opr3; }

  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_Op3* as_Op3() { return this; }
  virtual void print_instr(outputStream* out) const PRODUCT_RETURN;
};


//--------------------------------
class LabelObj: public CompilationResourceObj {
 private:
  Label _label;
 public:
  LabelObj()                                     {}
  Label* label()                                 { return &_label; }
};


class LIR_OpLock: public LIR_Op {
 friend class LIR_OpVisitState;

 private:
  LIR_Opr _hdr;
  LIR_Opr _obj;
  LIR_Opr _lock;
  LIR_Opr _scratch;
  CodeStub* _stub;
 public:
  LIR_OpLock(LIR_Code code, LIR_Opr hdr, LIR_Opr obj, LIR_Opr lock, LIR_Opr scratch, CodeStub* stub, CodeEmitInfo* info)
    : LIR_Op(code, LIR_OprFact::illegalOpr, info)
    , _hdr(hdr)
    , _obj(obj)
    , _lock(lock)
    , _scratch(scratch)
    , _stub(stub)                      {}

  LIR_Opr hdr_opr() const                        { return _hdr; }
  LIR_Opr obj_opr() const                        { return _obj; }
  LIR_Opr lock_opr() const                       { return _lock; }
  LIR_Opr scratch_opr() const                    { return _scratch; }
  CodeStub* stub() const                         { return _stub; }

  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_OpLock* as_OpLock() { return this; }
  void print_instr(outputStream* out) const PRODUCT_RETURN;
};


class LIR_OpDelay: public LIR_Op {
 friend class LIR_OpVisitState;

 private:
  LIR_Op* _op;

 public:
  LIR_OpDelay(LIR_Op* op, CodeEmitInfo* info):
    LIR_Op(lir_delay_slot, LIR_OprFact::illegalOpr, info),
    _op(op) {
    assert(op->code() == lir_nop, "should be filling with nops");
  }
  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_OpDelay* as_OpDelay() { return this; }
  void print_instr(outputStream* out) const PRODUCT_RETURN;
  LIR_Op* delay_op() const { return _op; }
  CodeEmitInfo* call_info() const { return info(); }
};

#ifdef ASSERT
// LIR_OpAssert
class LIR_OpAssert : public LIR_Op2 {
 friend class LIR_OpVisitState;

 private:
  const char* _msg;
  bool        _halt;

 public:
  LIR_OpAssert(LIR_Condition condition, LIR_Opr opr1, LIR_Opr opr2, const char* msg, bool halt)
    : LIR_Op2(lir_assert, condition, opr1, opr2)
    , _msg(msg)
    , _halt(halt) {
  }

  const char* msg() const                        { return _msg; }
  bool        halt() const                       { return _halt; }

  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_OpAssert* as_OpAssert()            { return this; }
  virtual void print_instr(outputStream* out) const PRODUCT_RETURN;
};
#endif

// LIR_OpCompareAndSwap
class LIR_OpCompareAndSwap : public LIR_Op {
 friend class LIR_OpVisitState;

 private:
  LIR_Opr _addr;
  LIR_Opr _cmp_value;
  LIR_Opr _new_value;
  LIR_Opr _tmp1;
  LIR_Opr _tmp2;

 public:
  LIR_OpCompareAndSwap(LIR_Code code, LIR_Opr addr, LIR_Opr cmp_value, LIR_Opr new_value,
                       LIR_Opr t1, LIR_Opr t2, LIR_Opr result)
    : LIR_Op(code, result, NULL)  // no result, no info
    , _addr(addr)
    , _cmp_value(cmp_value)
    , _new_value(new_value)
    , _tmp1(t1)
    , _tmp2(t2)                                  { }

  LIR_Opr addr()        const                    { return _addr;  }
  LIR_Opr cmp_value()   const                    { return _cmp_value; }
  LIR_Opr new_value()   const                    { return _new_value; }
  LIR_Opr tmp1()        const                    { return _tmp1;      }
  LIR_Opr tmp2()        const                    { return _tmp2;      }

  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_OpCompareAndSwap * as_OpCompareAndSwap () { return this; }
  virtual void print_instr(outputStream* out) const PRODUCT_RETURN;
};

// LIR_OpProfileCall
class LIR_OpProfileCall : public LIR_Op {
 friend class LIR_OpVisitState;

 private:
  ciMethod* _profiled_method;
  int       _profiled_bci;
  ciMethod* _profiled_callee;
  LIR_Opr   _mdo;
  LIR_Opr   _recv;
  LIR_Opr   _tmp1;
  ciKlass*  _known_holder;

 public:
  // Destroys recv
  LIR_OpProfileCall(ciMethod* profiled_method, int profiled_bci, ciMethod* profiled_callee, LIR_Opr mdo, LIR_Opr recv, LIR_Opr t1, ciKlass* known_holder)
    : LIR_Op(lir_profile_call, LIR_OprFact::illegalOpr, NULL)  // no result, no info
    , _profiled_method(profiled_method)
    , _profiled_bci(profiled_bci)
    , _profiled_callee(profiled_callee)
    , _mdo(mdo)
    , _recv(recv)
    , _tmp1(t1)
    , _known_holder(known_holder)                { }

  ciMethod* profiled_method() const              { return _profiled_method;  }
  int       profiled_bci()    const              { return _profiled_bci;     }
  ciMethod* profiled_callee() const              { return _profiled_callee;  }
  LIR_Opr   mdo()             const              { return _mdo;              }
  LIR_Opr   recv()            const              { return _recv;             }
  LIR_Opr   tmp1()            const              { return _tmp1;             }
  ciKlass*  known_holder()    const              { return _known_holder;     }

  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_OpProfileCall* as_OpProfileCall() { return this; }
  virtual void print_instr(outputStream* out) const PRODUCT_RETURN;
  bool should_profile_receiver_type() const {
    bool callee_is_static = _profiled_callee->is_loaded() && _profiled_callee->is_static();
    Bytecodes::Code bc = _profiled_method->java_code_at_bci(_profiled_bci);
    bool call_is_virtual = (bc == Bytecodes::_invokevirtual && !_profiled_callee->can_be_statically_bound()) || bc == Bytecodes::_invokeinterface;
    return C1ProfileVirtualCalls && call_is_virtual && !callee_is_static;
  }
};

// LIR_OpProfileType
class LIR_OpProfileType : public LIR_Op {
 friend class LIR_OpVisitState;

 private:
  LIR_Opr      _mdp;
  LIR_Opr      _obj;
  LIR_Opr      _tmp;
  ciKlass*     _exact_klass;   // non NULL if we know the klass statically (no need to load it from _obj)
  intptr_t     _current_klass; // what the profiling currently reports
  bool         _not_null;      // true if we know statically that _obj cannot be null
  bool         _no_conflict;   // true if we're profling parameters, _exact_klass is not NULL and we know
                               // _exact_klass it the only possible type for this parameter in any context.

 public:
  // Destroys recv
  LIR_OpProfileType(LIR_Opr mdp, LIR_Opr obj, ciKlass* exact_klass, intptr_t current_klass, LIR_Opr tmp, bool not_null, bool no_conflict)
    : LIR_Op(lir_profile_type, LIR_OprFact::illegalOpr, NULL)  // no result, no info
    , _mdp(mdp)
    , _obj(obj)
    , _tmp(tmp)
    , _exact_klass(exact_klass)
    , _current_klass(current_klass)
    , _not_null(not_null)
    , _no_conflict(no_conflict) { }

  LIR_Opr      mdp()              const             { return _mdp;              }
  LIR_Opr      obj()              const             { return _obj;              }
  LIR_Opr      tmp()              const             { return _tmp;              }
  ciKlass*     exact_klass()      const             { return _exact_klass;      }
  intptr_t     current_klass()    const             { return _current_klass;    }
  bool         not_null()         const             { return _not_null;         }
  bool         no_conflict()      const             { return _no_conflict;      }

  virtual void emit_code(LIR_Assembler* masm);
  virtual LIR_OpProfileType* as_OpProfileType() { return this; }
  virtual void print_instr(outputStream* out) const PRODUCT_RETURN;
};

class LIR_InsertionBuffer;

//--------------------------------LIR_List---------------------------------------------------
// Maintains a list of LIR instructions (one instance of LIR_List per basic block)
// The LIR instructions are appended by the LIR_List class itself;
//
// Notes:
// - all offsets are(should be) in bytes
// - local positions are specified with an offset, with offset 0 being local 0

class LIR_List: public CompilationResourceObj {
 private:
  LIR_OpList  _operations;

  Compilation*  _compilation;
#ifndef PRODUCT
  BlockBegin*   _block;
#endif
#ifdef ASSERT
  const char *  _file;
  int           _line;
#endif

 public:
  void append(LIR_Op* op) {
    if (op->source() == NULL)
      op->set_source(_compilation->current_instruction());
#ifndef PRODUCT
    if (PrintIRWithLIR) {
      _compilation->maybe_print_current_instruction();
      op->print(); tty->cr();
    }
#endif // PRODUCT

    _operations.append(op);

#ifdef ASSERT
    op->verify();
    op->set_file_and_line(_file, _line);
    _file = NULL;
    _line = 0;
#endif
  }

  LIR_List(Compilation* compilation, BlockBegin* block = NULL);

#ifdef ASSERT
  void set_file_and_line(const char * file, int line);
#endif

  //---------- accessors ---------------
  LIR_OpList* instructions_list()                { return &_operations; }
  int         length() const                     { return _operations.length(); }
  LIR_Op*     at(int i) const                    { return _operations.at(i); }

  NOT_PRODUCT(BlockBegin* block() const          { return _block; });

  // insert LIR_Ops in buffer to right places in LIR_List
  void append(LIR_InsertionBuffer* buffer);

  //---------- mutators ---------------
  void insert_before(int i, LIR_List* op_list)   { _operations.insert_before(i, op_list->instructions_list()); }
  void insert_before(int i, LIR_Op* op)          { _operations.insert_before(i, op); }
  void remove_at(int i)                          { _operations.remove_at(i); }

  //---------- printing -------------
  void print_instructions() PRODUCT_RETURN;


  //---------- instructions -------------
  void call_opt_virtual(ciMethod* method, LIR_Opr receiver, LIR_Opr result,
                        address dest, LIR_OprList* arguments,
                        CodeEmitInfo* info) {
    append(new LIR_OpJavaCall(lir_optvirtual_call, method, receiver, result, dest, arguments, info));
  }
  void call_static(ciMethod* method, LIR_Opr result,
                   address dest, LIR_OprList* arguments, CodeEmitInfo* info) {
    append(new LIR_OpJavaCall(lir_static_call, method, LIR_OprFact::illegalOpr, result, dest, arguments, info));
  }
  void call_icvirtual(ciMethod* method, LIR_Opr receiver, LIR_Opr result,
                      address dest, LIR_OprList* arguments, CodeEmitInfo* info) {
    append(new LIR_OpJavaCall(lir_icvirtual_call, method, receiver, result, dest, arguments, info));
  }
  void call_dynamic(ciMethod* method, LIR_Opr receiver, LIR_Opr result,
                    address dest, LIR_OprList* arguments, CodeEmitInfo* info) {
    append(new LIR_OpJavaCall(lir_dynamic_call, method, receiver, result, dest, arguments, info));
  }

  void get_thread(LIR_Opr result)                { append(new LIR_Op0(lir_get_thread, result)); }
  void membar()                                  { append(new LIR_Op0(lir_membar)); }
  void membar_acquire()                          { append(new LIR_Op0(lir_membar_acquire)); }
  void membar_release()                          { append(new LIR_Op0(lir_membar_release)); }
  void membar_loadload()                         { append(new LIR_Op0(lir_membar_loadload)); }
  void membar_storestore()                       { append(new LIR_Op0(lir_membar_storestore)); }
  void membar_loadstore()                        { append(new LIR_Op0(lir_membar_loadstore)); }
  void membar_storeload()                        { append(new LIR_Op0(lir_membar_storeload)); }

  void nop()                                     { append(new LIR_Op0(lir_nop)); }

  void std_entry(LIR_Opr receiver)               { append(new LIR_Op0(lir_std_entry, receiver)); }
  void osr_entry(LIR_Opr osrPointer)             { append(new LIR_Op0(lir_osr_entry, osrPointer)); }

  void on_spin_wait()                            { append(new LIR_Op0(lir_on_spin_wait)); }

  void branch_destination(Label* lbl)            { append(new LIR_OpLabel(lbl)); }

  void leal(LIR_Opr from, LIR_Opr result_reg, LIR_PatchCode patch_code = lir_patch_none, CodeEmitInfo* info = NULL) { append(new LIR_Op1(lir_leal, from, result_reg, T_ILLEGAL, patch_code, info)); }

  // result is a stack location for old backend and vreg for UseLinearScan
  // stack_loc_temp is an illegal register for old backend
  void roundfp(LIR_Opr reg, LIR_Opr stack_loc_temp, LIR_Opr result) { append(new LIR_OpRoundFP(reg, stack_loc_temp, result)); }
  void move(LIR_Opr src, LIR_Opr dst, CodeEmitInfo* info = NULL) { append(new LIR_Op1(lir_move, src, dst, dst->type(), lir_patch_none, info)); }
  void move(LIR_Address* src, LIR_Opr dst, CodeEmitInfo* info = NULL) { append(new LIR_Op1(lir_move, LIR_OprFact::address(src), dst, src->type(), lir_patch_none, info)); }
  void move(LIR_Opr src, LIR_Address* dst, CodeEmitInfo* info = NULL) { append(new LIR_Op1(lir_move, src, LIR_OprFact::address(dst), dst->type(), lir_patch_none, info)); }
  void move_wide(LIR_Address* src, LIR_Opr dst, CodeEmitInfo* info = NULL) {
    if (UseCompressedOops) {
      append(new LIR_Op1(lir_move, LIR_OprFact::address(src), dst, src->type(), lir_patch_none, info, lir_move_wide));
    } else {
      move(src, dst, info);
    }
  }
  void move_wide(LIR_Opr src, LIR_Address* dst, CodeEmitInfo* info = NULL) {
    if (UseCompressedOops) {
      append(new LIR_Op1(lir_move, src, LIR_OprFact::address(dst), dst->type(), lir_patch_none, info, lir_move_wide));
    } else {
      move(src, dst, info);
    }
  }
  void volatile_move(LIR_Opr src, LIR_Opr dst, BasicType type, CodeEmitInfo* info = NULL, LIR_PatchCode patch_code = lir_patch_none) { append(new LIR_Op1(lir_move, src, dst, type, patch_code, info, lir_move_volatile)); }

  void oop2reg  (jobject o, LIR_Opr reg)         { assert(reg->type() == T_OBJECT, "bad reg"); append(new LIR_Op1(lir_move, LIR_OprFact::oopConst(o),    reg));   }
  void oop2reg_patch(jobject o, LIR_Opr reg, CodeEmitInfo* info);

  void metadata2reg  (Metadata* o, LIR_Opr reg)  { assert(reg->type() == T_METADATA, "bad reg"); append(new LIR_Op1(lir_move, LIR_OprFact::metadataConst(o), reg));   }
  void klass2reg_patch(Metadata* o, LIR_Opr reg, CodeEmitInfo* info);

  void safepoint(LIR_Opr tmp, CodeEmitInfo* info)  { append(new LIR_Op1(lir_safepoint, tmp, info)); }
  void return_op(LIR_Opr result)                   { append(new LIR_OpReturn(result)); }

  void convert(Bytecodes::Code code, LIR_Opr left, LIR_Opr dst, ConversionStub* stub = NULL/*, bool is_32bit = false*/) { append(new LIR_OpConvert(code, left, dst, stub)); }

  void logical_and (LIR_Opr left, LIR_Opr right, LIR_Opr dst) { append(new LIR_Op2(lir_logic_and,  left, right, dst)); }
  void logical_or  (LIR_Opr left, LIR_Opr right, LIR_Opr dst) { append(new LIR_Op2(lir_logic_or,   left, right, dst)); }
  void logical_xor (LIR_Opr left, LIR_Opr right, LIR_Opr dst) { append(new LIR_Op2(lir_logic_xor,  left, right, dst)); }

  void null_check(LIR_Opr opr, CodeEmitInfo* info, bool deoptimize_on_null = false);
  void throw_exception(LIR_Opr exceptionPC, LIR_Opr exceptionOop, CodeEmitInfo* info) {
    append(new LIR_Op2(lir_throw, exceptionPC, exceptionOop, LIR_OprFact::illegalOpr, info));
  }
  void unwind_exception(LIR_Opr exceptionOop) {
    append(new LIR_Op1(lir_unwind, exceptionOop));
  }

  void push(LIR_Opr opr)                                   { append(new LIR_Op1(lir_push, opr)); }
  void pop(LIR_Opr reg)                                    { append(new LIR_Op1(lir_pop,  reg)); }

  void cmp(LIR_Condition condition, LIR_Opr left, LIR_Opr right, CodeEmitInfo* info = NULL) {
    append(new LIR_Op2(lir_cmp, condition, left, right, info));
  }
  void cmp(LIR_Condition condition, LIR_Opr left, int right, CodeEmitInfo* info = NULL) {
    cmp(condition, left, LIR_OprFact::intConst(right), info);
  }

  void cmp_mem_int(LIR_Condition condition, LIR_Opr base, int disp, int c, CodeEmitInfo* info);
  void cmp_reg_mem(LIR_Condition condition, LIR_Opr reg, LIR_Address* addr, CodeEmitInfo* info);

  void cmove(LIR_Condition condition, LIR_Opr src1, LIR_Opr src2, LIR_Opr dst, BasicType type) {
    append(new LIR_Op2(lir_cmove, condition, src1, src2, dst, type));
  }

  void cas_long(LIR_Opr addr, LIR_Opr cmp_value, LIR_Opr new_value,
                LIR_Opr t1, LIR_Opr t2, LIR_Opr result = LIR_OprFact::illegalOpr);
  void cas_obj(LIR_Opr addr, LIR_Opr cmp_value, LIR_Opr new_value,
               LIR_Opr t1, LIR_Opr t2, LIR_Opr result = LIR_OprFact::illegalOpr);
  void cas_int(LIR_Opr addr, LIR_Opr cmp_value, LIR_Opr new_value,
               LIR_Opr t1, LIR_Opr t2, LIR_Opr result = LIR_OprFact::illegalOpr);

  void abs (LIR_Opr from, LIR_Opr to, LIR_Opr tmp)                { append(new LIR_Op2(lir_abs , from, tmp, to)); }
  void negate(LIR_Opr from, LIR_Opr to, LIR_Opr tmp = LIR_OprFact::illegalOpr)              { append(new LIR_Op2(lir_neg, from, tmp, to)); }
  void sqrt(LIR_Opr from, LIR_Opr to, LIR_Opr tmp)                { append(new LIR_Op2(lir_sqrt, from, tmp, to)); }
  void fmad(LIR_Opr from, LIR_Opr from1, LIR_Opr from2, LIR_Opr to) { append(new LIR_Op3(lir_fmad, from, from1, from2, to)); }
  void fmaf(LIR_Opr from, LIR_Opr from1, LIR_Opr from2, LIR_Opr to) { append(new LIR_Op3(lir_fmaf, from, from1, from2, to)); }
  void log10 (LIR_Opr from, LIR_Opr to, LIR_Opr tmp)              { append(new LIR_Op2(lir_log10, from, LIR_OprFact::illegalOpr, to, tmp)); }
  void tan (LIR_Opr from, LIR_Opr to, LIR_Opr tmp1, LIR_Opr tmp2) { append(new LIR_Op2(lir_tan , from, tmp1, to, tmp2)); }

  void add (LIR_Opr left, LIR_Opr right, LIR_Opr res)      { append(new LIR_Op2(lir_add, left, right, res)); }
  void sub (LIR_Opr left, LIR_Opr right, LIR_Opr res, CodeEmitInfo* info = NULL) { append(new LIR_Op2(lir_sub, left, right, res, info)); }
  void mul (LIR_Opr left, LIR_Opr right, LIR_Opr res) { append(new LIR_Op2(lir_mul, left, right, res)); }
  void mul (LIR_Opr left, LIR_Opr right, LIR_Opr res, LIR_Opr tmp) { append(new LIR_Op2(lir_mul, left, right, res, tmp)); }
  void div (LIR_Opr left, LIR_Opr right, LIR_Opr res, CodeEmitInfo* info = NULL)      { append(new LIR_Op2(lir_div, left, right, res, info)); }
  void div (LIR_Opr left, LIR_Opr right, LIR_Opr res, LIR_Opr tmp) { append(new LIR_Op2(lir_div, left, right, res, tmp)); }
  void rem (LIR_Opr left, LIR_Opr right, LIR_Opr res, CodeEmitInfo* info = NULL)      { append(new LIR_Op2(lir_rem, left, right, res, info)); }

  void volatile_load_mem_reg(LIR_Address* address, LIR_Opr dst, CodeEmitInfo* info, LIR_PatchCode patch_code = lir_patch_none);
  void volatile_load_unsafe_reg(LIR_Opr base, LIR_Opr offset, LIR_Opr dst, BasicType type, CodeEmitInfo* info, LIR_PatchCode patch_code);

  void load(LIR_Address* addr, LIR_Opr src, CodeEmitInfo* info = NULL, LIR_PatchCode patch_code = lir_patch_none);

  void store_mem_int(jint v,    LIR_Opr base, int offset_in_bytes, BasicType type, CodeEmitInfo* info, LIR_PatchCode patch_code = lir_patch_none);
  void store_mem_oop(jobject o, LIR_Opr base, int offset_in_bytes, BasicType type, CodeEmitInfo* info, LIR_PatchCode patch_code = lir_patch_none);
  void store(LIR_Opr src, LIR_Address* addr, CodeEmitInfo* info = NULL, LIR_PatchCode patch_code = lir_patch_none);
  void volatile_store_mem_reg(LIR_Opr src, LIR_Address* address, CodeEmitInfo* info, LIR_PatchCode patch_code = lir_patch_none);
  void volatile_store_unsafe_reg(LIR_Opr src, LIR_Opr base, LIR_Opr offset, BasicType type, CodeEmitInfo* info, LIR_PatchCode patch_code);

  void idiv(LIR_Opr left, LIR_Opr right, LIR_Opr res, LIR_Opr tmp, CodeEmitInfo* info);
  void idiv(LIR_Opr left, int   right, LIR_Opr res, LIR_Opr tmp, CodeEmitInfo* info);
  void irem(LIR_Opr left, LIR_Opr right, LIR_Opr res, LIR_Opr tmp, CodeEmitInfo* info);
  void irem(LIR_Opr left, int   right, LIR_Opr res, LIR_Opr tmp, CodeEmitInfo* info);

  void allocate_object(LIR_Opr dst, LIR_Opr t1, LIR_Opr t2, LIR_Opr t3, LIR_Opr t4, int header_size, int object_size, LIR_Opr klass, bool init_check, CodeStub* stub);
  void allocate_array(LIR_Opr dst, LIR_Opr len, LIR_Opr t1,LIR_Opr t2, LIR_Opr t3,LIR_Opr t4, BasicType type, LIR_Opr klass, CodeStub* stub);

  // jump is an unconditional branch
  void jump(BlockBegin* block) {
    append(new LIR_OpBranch(lir_cond_always, block));
  }
  void jump(CodeStub* stub) {
    append(new LIR_OpBranch(lir_cond_always, stub));
  }
  void branch(LIR_Condition cond, Label* lbl) {
    append(new LIR_OpBranch(cond, lbl));
  }
  // Should not be used for fp comparisons
  void branch(LIR_Condition cond, BlockBegin* block) {
    append(new LIR_OpBranch(cond, block));
  }
  // Should not be used for fp comparisons
  void branch(LIR_Condition cond, CodeStub* stub) {
    append(new LIR_OpBranch(cond, stub));
  }
  // Should only be used for fp comparisons
  void branch(LIR_Condition cond, BlockBegin* block, BlockBegin* unordered) {
    append(new LIR_OpBranch(cond, block, unordered));
  }

  void shift_left(LIR_Opr value, LIR_Opr count, LIR_Opr dst, LIR_Opr tmp);
  void shift_right(LIR_Opr value, LIR_Opr count, LIR_Opr dst, LIR_Opr tmp);
  void unsigned_shift_right(LIR_Opr value, LIR_Opr count, LIR_Opr dst, LIR_Opr tmp);

  void shift_left(LIR_Opr value, int count, LIR_Opr dst)       { shift_left(value, LIR_OprFact::intConst(count), dst, LIR_OprFact::illegalOpr); }
  void shift_right(LIR_Opr value, int count, LIR_Opr dst)      { shift_right(value, LIR_OprFact::intConst(count), dst, LIR_OprFact::illegalOpr); }
  void unsigned_shift_right(LIR_Opr value, int count, LIR_Opr dst) { unsigned_shift_right(value, LIR_OprFact::intConst(count), dst, LIR_OprFact::illegalOpr); }

  void lcmp2int(LIR_Opr left, LIR_Opr right, LIR_Opr dst)        { append(new LIR_Op2(lir_cmp_l2i,  left, right, dst)); }
  void fcmp2int(LIR_Opr left, LIR_Opr right, LIR_Opr dst, bool is_unordered_less);

  void call_runtime_leaf(address routine, LIR_Opr tmp, LIR_Opr result, LIR_OprList* arguments) {
    append(new LIR_OpRTCall(routine, tmp, result, arguments));
  }

  void call_runtime(address routine, LIR_Opr tmp, LIR_Opr result,
                    LIR_OprList* arguments, CodeEmitInfo* info) {
    append(new LIR_OpRTCall(routine, tmp, result, arguments, info));
  }

  void load_stack_address_monitor(int monitor_ix, LIR_Opr dst)  { append(new LIR_Op1(lir_monaddr, LIR_OprFact::intConst(monitor_ix), dst)); }
  void unlock_object(LIR_Opr hdr, LIR_Opr obj, LIR_Opr lock, LIR_Opr scratch, CodeStub* stub);
  void lock_object(LIR_Opr hdr, LIR_Opr obj, LIR_Opr lock, LIR_Opr scratch, CodeStub* stub, CodeEmitInfo* info);

  void breakpoint()                                                  { append(new LIR_Op0(lir_breakpoint)); }

  void arraycopy(LIR_Opr src, LIR_Opr src_pos, LIR_Opr dst, LIR_Opr dst_pos, LIR_Opr length, LIR_Opr tmp, ciArrayKlass* expected_type, int flags, CodeEmitInfo* info) { append(new LIR_OpArrayCopy(src, src_pos, dst, dst_pos, length, tmp, expected_type, flags, info)); }

  void update_crc32(LIR_Opr crc, LIR_Opr val, LIR_Opr res)  { append(new LIR_OpUpdateCRC32(crc, val, res)); }

  void instanceof(LIR_Opr result, LIR_Opr object, ciKlass* klass, LIR_Opr tmp1, LIR_Opr tmp2, LIR_Opr tmp3, bool fast_check, CodeEmitInfo* info_for_patch, ciMethod* profiled_method, int profiled_bci);
  void store_check(LIR_Opr object, LIR_Opr array, LIR_Opr tmp1, LIR_Opr tmp2, LIR_Opr tmp3, CodeEmitInfo* info_for_exception, ciMethod* profiled_method, int profiled_bci);

  void checkcast (LIR_Opr result, LIR_Opr object, ciKlass* klass,
                  LIR_Opr tmp1, LIR_Opr tmp2, LIR_Opr tmp3, bool fast_check,
                  CodeEmitInfo* info_for_exception, CodeEmitInfo* info_for_patch, CodeStub* stub,
                  ciMethod* profiled_method, int profiled_bci);
  // MethodData* profiling
  void profile_call(ciMethod* method, int bci, ciMethod* callee, LIR_Opr mdo, LIR_Opr recv, LIR_Opr t1, ciKlass* cha_klass) {
    append(new LIR_OpProfileCall(method, bci, callee, mdo, recv, t1, cha_klass));
  }
  void profile_type(LIR_Address* mdp, LIR_Opr obj, ciKlass* exact_klass, intptr_t current_klass, LIR_Opr tmp, bool not_null, bool no_conflict) {
    append(new LIR_OpProfileType(LIR_OprFact::address(mdp), obj, exact_klass, current_klass, tmp, not_null, no_conflict));
  }

  void xadd(LIR_Opr src, LIR_Opr add, LIR_Opr res, LIR_Opr tmp) { append(new LIR_Op2(lir_xadd, src, add, res, tmp)); }
  void xchg(LIR_Opr src, LIR_Opr set, LIR_Opr res, LIR_Opr tmp) { append(new LIR_Op2(lir_xchg, src, set, res, tmp)); }
#ifdef ASSERT
  void lir_assert(LIR_Condition condition, LIR_Opr opr1, LIR_Opr opr2, const char* msg, bool halt) { append(new LIR_OpAssert(condition, opr1, opr2, msg, halt)); }
#endif
};

void print_LIR(BlockList* blocks);

class LIR_InsertionBuffer : public CompilationResourceObj {
 private:
  LIR_List*   _lir;   // the lir list where ops of this buffer should be inserted later (NULL when uninitialized)

  // list of insertion points. index and count are stored alternately:
  // _index_and_count[i * 2]:     the index into lir list where "count" ops should be inserted
  // _index_and_count[i * 2 + 1]: the number of ops to be inserted at index
  intStack    _index_and_count;

  // the LIR_Ops to be inserted
  LIR_OpList  _ops;

  void append_new(int index, int count)  { _index_and_count.append(index); _index_and_count.append(count); }
  void set_index_at(int i, int value)    { _index_and_count.at_put((i << 1),     value); }
  void set_count_at(int i, int value)    { _index_and_count.at_put((i << 1) + 1, value); }

#ifdef ASSERT
  void verify();
#endif
 public:
  LIR_InsertionBuffer() : _lir(NULL), _index_and_count(8), _ops(8) { }

  // must be called before using the insertion buffer
  void init(LIR_List* lir)  { assert(!initialized(), "already initialized"); _lir = lir; _index_and_count.clear(); _ops.clear(); }
  bool initialized() const  { return _lir != NULL; }
  // called automatically when the buffer is appended to the LIR_List
  void finish()             { _lir = NULL; }

  // accessors
  LIR_List*  lir_list() const             { return _lir; }
  int number_of_insertion_points() const  { return _index_and_count.length() >> 1; }
  int index_at(int i) const               { return _index_and_count.at((i << 1));     }
  int count_at(int i) const               { return _index_and_count.at((i << 1) + 1); }

  int number_of_ops() const               { return _ops.length(); }
  LIR_Op* op_at(int i) const              { return _ops.at(i); }

  // append an instruction to the buffer
  void append(int index, LIR_Op* op);

  // instruction
  void move(int index, LIR_Opr src, LIR_Opr dst, CodeEmitInfo* info = NULL) { append(index, new LIR_Op1(lir_move, src, dst, dst->type(), lir_patch_none, info)); }
};


//
// LIR_OpVisitState is used for manipulating LIR_Ops in an abstract way.
// Calling a LIR_Op's visit function with a LIR_OpVisitState causes
// information about the input, output and temporaries used by the
// op to be recorded.  It also records whether the op has call semantics
// and also records all the CodeEmitInfos used by this op.
//


class LIR_OpVisitState: public StackObj {
 public:
  typedef enum { inputMode, firstMode = inputMode, tempMode, outputMode, numModes, invalidMode = -1 } OprMode;

  enum {
    maxNumberOfOperands = 20,
    maxNumberOfInfos = 4
  };

 private:
  LIR_Op*          _op;

  // optimization: the operands and infos are not stored in a variable-length
  //               list, but in a fixed-size array to save time of size checks and resizing
  int              _oprs_len[numModes];
  LIR_Opr*         _oprs_new[numModes][maxNumberOfOperands];
  int _info_len;
  CodeEmitInfo*    _info_new[maxNumberOfInfos];

  bool             _has_call;
  bool             _has_slow_case;


  // only include register operands
  // addresses are decomposed to the base and index registers
  // constants and stack operands are ignored
  void append(LIR_Opr& opr, OprMode mode) {
    assert(opr->is_valid(), "should not call this otherwise");
    assert(mode >= 0 && mode < numModes, "bad mode");

    if (opr->is_register()) {
       assert(_oprs_len[mode] < maxNumberOfOperands, "array overflow");
      _oprs_new[mode][_oprs_len[mode]++] = &opr;

    } else if (opr->is_pointer()) {
      LIR_Address* address = opr->as_address_ptr();
      if (address != NULL) {
        // special handling for addresses: add base and index register of the address
        // both are always input operands or temp if we want to extend
        // their liveness!
        if (mode == outputMode) {
          mode = inputMode;
        }
        assert (mode == inputMode || mode == tempMode, "input or temp only for addresses");
        if (address->_base->is_valid()) {
          assert(address->_base->is_register(), "must be");
          assert(_oprs_len[mode] < maxNumberOfOperands, "array overflow");
          _oprs_new[mode][_oprs_len[mode]++] = &address->_base;
        }
        if (address->_index->is_valid()) {
          assert(address->_index->is_register(), "must be");
          assert(_oprs_len[mode] < maxNumberOfOperands, "array overflow");
          _oprs_new[mode][_oprs_len[mode]++] = &address->_index;
        }

      } else {
        assert(opr->is_constant(), "constant operands are not processed");
      }
    } else {
      assert(opr->is_stack(), "stack operands are not processed");
    }
  }

  void append(CodeEmitInfo* info) {
    assert(info != NULL, "should not call this otherwise");
    assert(_info_len < maxNumberOfInfos, "array overflow");
    _info_new[_info_len++] = info;
  }

 public:
  LIR_OpVisitState()         { reset(); }

  LIR_Op* op() const         { return _op; }
  void set_op(LIR_Op* op)    { reset(); _op = op; }

  bool has_call() const      { return _has_call; }
  bool has_slow_case() const { return _has_slow_case; }

  void reset() {
    _op = NULL;
    _has_call = false;
    _has_slow_case = false;

    _oprs_len[inputMode] = 0;
    _oprs_len[tempMode] = 0;
    _oprs_len[outputMode] = 0;
    _info_len = 0;
  }


  int opr_count(OprMode mode) const {
    assert(mode >= 0 && mode < numModes, "bad mode");
    return _oprs_len[mode];
  }

  LIR_Opr opr_at(OprMode mode, int index) const {
    assert(mode >= 0 && mode < numModes, "bad mode");
    assert(index >= 0 && index < _oprs_len[mode], "index out of bound");
    return *_oprs_new[mode][index];
  }

  void set_opr_at(OprMode mode, int index, LIR_Opr opr) const {
    assert(mode >= 0 && mode < numModes, "bad mode");
    assert(index >= 0 && index < _oprs_len[mode], "index out of bound");
    *_oprs_new[mode][index] = opr;
  }

  int info_count() const {
    return _info_len;
  }

  CodeEmitInfo* info_at(int index) const {
    assert(index < _info_len, "index out of bounds");
    return _info_new[index];
  }

  XHandlers* all_xhandler();

  // collects all register operands of the instruction
  void visit(LIR_Op* op);

#ifdef ASSERT
  // check that an operation has no operands
  bool no_operands(LIR_Op* op);
#endif

  // LIR_Op visitor functions use these to fill in the state
  void do_input(LIR_Opr& opr)             { append(opr, LIR_OpVisitState::inputMode); }
  void do_output(LIR_Opr& opr)            { append(opr, LIR_OpVisitState::outputMode); }
  void do_temp(LIR_Opr& opr)              { append(opr, LIR_OpVisitState::tempMode); }
  void do_info(CodeEmitInfo* info)        { append(info); }

  void do_stub(CodeStub* stub);
  void do_call()                          { _has_call = true; }
  void do_slow_case()                     { _has_slow_case = true; }
  void do_slow_case(CodeEmitInfo* info) {
    _has_slow_case = true;
    append(info);
  }
};


inline LIR_Opr LIR_OprDesc::illegalOpr()   { return LIR_OprFact::illegalOpr; };

#endif // SHARE_C1_C1_LIR_HPP
