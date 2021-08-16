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

#include "precompiled.hpp"
#include "ci/ciMethodData.hpp"
#include "ci/ciTypeFlow.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/symbolTable.hpp"
#include "compiler/compileLog.hpp"
#include "libadt/dict.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/instanceMirrorKlass.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/typeArrayKlass.hpp"
#include "opto/matcher.hpp"
#include "opto/node.hpp"
#include "opto/opcodes.hpp"
#include "opto/type.hpp"
#include "utilities/powerOfTwo.hpp"
#include "utilities/stringUtils.hpp"

// Portions of code courtesy of Clifford Click

// Optimization - Graph Style

// Dictionary of types shared among compilations.
Dict* Type::_shared_type_dict = NULL;

// Array which maps compiler types to Basic Types
const Type::TypeInfo Type::_type_info[Type::lastype] = {
  { Bad,             T_ILLEGAL,    "bad",           false, Node::NotAMachineReg, relocInfo::none          },  // Bad
  { Control,         T_ILLEGAL,    "control",       false, 0,                    relocInfo::none          },  // Control
  { Bottom,          T_VOID,       "top",           false, 0,                    relocInfo::none          },  // Top
  { Bad,             T_INT,        "int:",          false, Op_RegI,              relocInfo::none          },  // Int
  { Bad,             T_LONG,       "long:",         false, Op_RegL,              relocInfo::none          },  // Long
  { Half,            T_VOID,       "half",          false, 0,                    relocInfo::none          },  // Half
  { Bad,             T_NARROWOOP,  "narrowoop:",    false, Op_RegN,              relocInfo::none          },  // NarrowOop
  { Bad,             T_NARROWKLASS,"narrowklass:",  false, Op_RegN,              relocInfo::none          },  // NarrowKlass
  { Bad,             T_ILLEGAL,    "tuple:",        false, Node::NotAMachineReg, relocInfo::none          },  // Tuple
  { Bad,             T_ARRAY,      "array:",        false, Node::NotAMachineReg, relocInfo::none          },  // Array

#if defined(PPC64)
  { Bad,             T_ILLEGAL,    "vectormask:",   false, Op_RegVectMask,       relocInfo::none          },  // VectorMask.
  { Bad,             T_ILLEGAL,    "vectora:",      false, Op_VecA,              relocInfo::none          },  // VectorA.
  { Bad,             T_ILLEGAL,    "vectors:",      false, 0,                    relocInfo::none          },  // VectorS
  { Bad,             T_ILLEGAL,    "vectord:",      false, Op_RegL,              relocInfo::none          },  // VectorD
  { Bad,             T_ILLEGAL,    "vectorx:",      false, Op_VecX,              relocInfo::none          },  // VectorX
  { Bad,             T_ILLEGAL,    "vectory:",      false, 0,                    relocInfo::none          },  // VectorY
  { Bad,             T_ILLEGAL,    "vectorz:",      false, 0,                    relocInfo::none          },  // VectorZ
#elif defined(S390)
  { Bad,             T_ILLEGAL,    "vectormask:",   false, Op_RegVectMask,       relocInfo::none          },  // VectorMask.
  { Bad,             T_ILLEGAL,    "vectora:",      false, Op_VecA,              relocInfo::none          },  // VectorA.
  { Bad,             T_ILLEGAL,    "vectors:",      false, 0,                    relocInfo::none          },  // VectorS
  { Bad,             T_ILLEGAL,    "vectord:",      false, Op_RegL,              relocInfo::none          },  // VectorD
  { Bad,             T_ILLEGAL,    "vectorx:",      false, 0,                    relocInfo::none          },  // VectorX
  { Bad,             T_ILLEGAL,    "vectory:",      false, 0,                    relocInfo::none          },  // VectorY
  { Bad,             T_ILLEGAL,    "vectorz:",      false, 0,                    relocInfo::none          },  // VectorZ
#else // all other
  { Bad,             T_ILLEGAL,    "vectormask:",   false, Op_RegVectMask,       relocInfo::none          },  // VectorMask.
  { Bad,             T_ILLEGAL,    "vectora:",      false, Op_VecA,              relocInfo::none          },  // VectorA.
  { Bad,             T_ILLEGAL,    "vectors:",      false, Op_VecS,              relocInfo::none          },  // VectorS
  { Bad,             T_ILLEGAL,    "vectord:",      false, Op_VecD,              relocInfo::none          },  // VectorD
  { Bad,             T_ILLEGAL,    "vectorx:",      false, Op_VecX,              relocInfo::none          },  // VectorX
  { Bad,             T_ILLEGAL,    "vectory:",      false, Op_VecY,              relocInfo::none          },  // VectorY
  { Bad,             T_ILLEGAL,    "vectorz:",      false, Op_VecZ,              relocInfo::none          },  // VectorZ
#endif
  { Bad,             T_ADDRESS,    "anyptr:",       false, Op_RegP,              relocInfo::none          },  // AnyPtr
  { Bad,             T_ADDRESS,    "rawptr:",       false, Op_RegP,              relocInfo::none          },  // RawPtr
  { Bad,             T_OBJECT,     "oop:",          true,  Op_RegP,              relocInfo::oop_type      },  // OopPtr
  { Bad,             T_OBJECT,     "inst:",         true,  Op_RegP,              relocInfo::oop_type      },  // InstPtr
  { Bad,             T_OBJECT,     "ary:",          true,  Op_RegP,              relocInfo::oop_type      },  // AryPtr
  { Bad,             T_METADATA,   "metadata:",     false, Op_RegP,              relocInfo::metadata_type },  // MetadataPtr
  { Bad,             T_METADATA,   "klass:",        false, Op_RegP,              relocInfo::metadata_type },  // KlassPtr
  { Bad,             T_OBJECT,     "func",          false, 0,                    relocInfo::none          },  // Function
  { Abio,            T_ILLEGAL,    "abIO",          false, 0,                    relocInfo::none          },  // Abio
  { Return_Address,  T_ADDRESS,    "return_address",false, Op_RegP,              relocInfo::none          },  // Return_Address
  { Memory,          T_ILLEGAL,    "memory",        false, 0,                    relocInfo::none          },  // Memory
  { FloatBot,        T_FLOAT,      "float_top",     false, Op_RegF,              relocInfo::none          },  // FloatTop
  { FloatCon,        T_FLOAT,      "ftcon:",        false, Op_RegF,              relocInfo::none          },  // FloatCon
  { FloatTop,        T_FLOAT,      "float",         false, Op_RegF,              relocInfo::none          },  // FloatBot
  { DoubleBot,       T_DOUBLE,     "double_top",    false, Op_RegD,              relocInfo::none          },  // DoubleTop
  { DoubleCon,       T_DOUBLE,     "dblcon:",       false, Op_RegD,              relocInfo::none          },  // DoubleCon
  { DoubleTop,       T_DOUBLE,     "double",        false, Op_RegD,              relocInfo::none          },  // DoubleBot
  { Top,             T_ILLEGAL,    "bottom",        false, 0,                    relocInfo::none          }   // Bottom
};

// Map ideal registers (machine types) to ideal types
const Type *Type::mreg2type[_last_machine_leaf];

// Map basic types to canonical Type* pointers.
const Type* Type::     _const_basic_type[T_CONFLICT+1];

// Map basic types to constant-zero Types.
const Type* Type::            _zero_type[T_CONFLICT+1];

// Map basic types to array-body alias types.
const TypeAryPtr* TypeAryPtr::_array_body_type[T_CONFLICT+1];

//=============================================================================
// Convenience common pre-built types.
const Type *Type::ABIO;         // State-of-machine only
const Type *Type::BOTTOM;       // All values
const Type *Type::CONTROL;      // Control only
const Type *Type::DOUBLE;       // All doubles
const Type *Type::FLOAT;        // All floats
const Type *Type::HALF;         // Placeholder half of doublewide type
const Type *Type::MEMORY;       // Abstract store only
const Type *Type::RETURN_ADDRESS;
const Type *Type::TOP;          // No values in set

//------------------------------get_const_type---------------------------
const Type* Type::get_const_type(ciType* type) {
  if (type == NULL) {
    return NULL;
  } else if (type->is_primitive_type()) {
    return get_const_basic_type(type->basic_type());
  } else {
    return TypeOopPtr::make_from_klass(type->as_klass());
  }
}

//---------------------------array_element_basic_type---------------------------------
// Mapping to the array element's basic type.
BasicType Type::array_element_basic_type() const {
  BasicType bt = basic_type();
  if (bt == T_INT) {
    if (this == TypeInt::INT)   return T_INT;
    if (this == TypeInt::CHAR)  return T_CHAR;
    if (this == TypeInt::BYTE)  return T_BYTE;
    if (this == TypeInt::BOOL)  return T_BOOLEAN;
    if (this == TypeInt::SHORT) return T_SHORT;
    return T_VOID;
  }
  return bt;
}

// For two instance arrays of same dimension, return the base element types.
// Otherwise or if the arrays have different dimensions, return NULL.
void Type::get_arrays_base_elements(const Type *a1, const Type *a2,
                                    const TypeInstPtr **e1, const TypeInstPtr **e2) {

  if (e1) *e1 = NULL;
  if (e2) *e2 = NULL;
  const TypeAryPtr* a1tap = (a1 == NULL) ? NULL : a1->isa_aryptr();
  const TypeAryPtr* a2tap = (a2 == NULL) ? NULL : a2->isa_aryptr();

  if (a1tap != NULL && a2tap != NULL) {
    // Handle multidimensional arrays
    const TypePtr* a1tp = a1tap->elem()->make_ptr();
    const TypePtr* a2tp = a2tap->elem()->make_ptr();
    while (a1tp && a1tp->isa_aryptr() && a2tp && a2tp->isa_aryptr()) {
      a1tap = a1tp->is_aryptr();
      a2tap = a2tp->is_aryptr();
      a1tp = a1tap->elem()->make_ptr();
      a2tp = a2tap->elem()->make_ptr();
    }
    if (a1tp && a1tp->isa_instptr() && a2tp && a2tp->isa_instptr()) {
      if (e1) *e1 = a1tp->is_instptr();
      if (e2) *e2 = a2tp->is_instptr();
    }
  }
}

//---------------------------get_typeflow_type---------------------------------
// Import a type produced by ciTypeFlow.
const Type* Type::get_typeflow_type(ciType* type) {
  switch (type->basic_type()) {

  case ciTypeFlow::StateVector::T_BOTTOM:
    assert(type == ciTypeFlow::StateVector::bottom_type(), "");
    return Type::BOTTOM;

  case ciTypeFlow::StateVector::T_TOP:
    assert(type == ciTypeFlow::StateVector::top_type(), "");
    return Type::TOP;

  case ciTypeFlow::StateVector::T_NULL:
    assert(type == ciTypeFlow::StateVector::null_type(), "");
    return TypePtr::NULL_PTR;

  case ciTypeFlow::StateVector::T_LONG2:
    // The ciTypeFlow pass pushes a long, then the half.
    // We do the same.
    assert(type == ciTypeFlow::StateVector::long2_type(), "");
    return TypeInt::TOP;

  case ciTypeFlow::StateVector::T_DOUBLE2:
    // The ciTypeFlow pass pushes double, then the half.
    // Our convention is the same.
    assert(type == ciTypeFlow::StateVector::double2_type(), "");
    return Type::TOP;

  case T_ADDRESS:
    assert(type->is_return_address(), "");
    return TypeRawPtr::make((address)(intptr_t)type->as_return_address()->bci());

  default:
    // make sure we did not mix up the cases:
    assert(type != ciTypeFlow::StateVector::bottom_type(), "");
    assert(type != ciTypeFlow::StateVector::top_type(), "");
    assert(type != ciTypeFlow::StateVector::null_type(), "");
    assert(type != ciTypeFlow::StateVector::long2_type(), "");
    assert(type != ciTypeFlow::StateVector::double2_type(), "");
    assert(!type->is_return_address(), "");

    return Type::get_const_type(type);
  }
}


//-----------------------make_from_constant------------------------------------
const Type* Type::make_from_constant(ciConstant constant, bool require_constant,
                                     int stable_dimension, bool is_narrow_oop,
                                     bool is_autobox_cache) {
  switch (constant.basic_type()) {
    case T_BOOLEAN:  return TypeInt::make(constant.as_boolean());
    case T_CHAR:     return TypeInt::make(constant.as_char());
    case T_BYTE:     return TypeInt::make(constant.as_byte());
    case T_SHORT:    return TypeInt::make(constant.as_short());
    case T_INT:      return TypeInt::make(constant.as_int());
    case T_LONG:     return TypeLong::make(constant.as_long());
    case T_FLOAT:    return TypeF::make(constant.as_float());
    case T_DOUBLE:   return TypeD::make(constant.as_double());
    case T_ARRAY:
    case T_OBJECT: {
        const Type* con_type = NULL;
        ciObject* oop_constant = constant.as_object();
        if (oop_constant->is_null_object()) {
          con_type = Type::get_zero_type(T_OBJECT);
        } else {
          guarantee(require_constant || oop_constant->should_be_constant(), "con_type must get computed");
          con_type = TypeOopPtr::make_from_constant(oop_constant, require_constant);
          if (Compile::current()->eliminate_boxing() && is_autobox_cache) {
            con_type = con_type->is_aryptr()->cast_to_autobox_cache();
          }
          if (stable_dimension > 0) {
            assert(FoldStableValues, "sanity");
            assert(!con_type->is_zero_type(), "default value for stable field");
            con_type = con_type->is_aryptr()->cast_to_stable(true, stable_dimension);
          }
        }
        if (is_narrow_oop) {
          con_type = con_type->make_narrowoop();
        }
        return con_type;
      }
    case T_ILLEGAL:
      // Invalid ciConstant returned due to OutOfMemoryError in the CI
      assert(Compile::current()->env()->failing(), "otherwise should not see this");
      return NULL;
    default:
      // Fall through to failure
      return NULL;
  }
}

static ciConstant check_mismatched_access(ciConstant con, BasicType loadbt, bool is_unsigned) {
  BasicType conbt = con.basic_type();
  switch (conbt) {
    case T_BOOLEAN: conbt = T_BYTE;   break;
    case T_ARRAY:   conbt = T_OBJECT; break;
    default:                          break;
  }
  switch (loadbt) {
    case T_BOOLEAN:   loadbt = T_BYTE;   break;
    case T_NARROWOOP: loadbt = T_OBJECT; break;
    case T_ARRAY:     loadbt = T_OBJECT; break;
    case T_ADDRESS:   loadbt = T_OBJECT; break;
    default:                             break;
  }
  if (conbt == loadbt) {
    if (is_unsigned && conbt == T_BYTE) {
      // LoadB (T_BYTE) with a small mask (<=8-bit) is converted to LoadUB (T_BYTE).
      return ciConstant(T_INT, con.as_int() & 0xFF);
    } else {
      return con;
    }
  }
  if (conbt == T_SHORT && loadbt == T_CHAR) {
    // LoadS (T_SHORT) with a small mask (<=16-bit) is converted to LoadUS (T_CHAR).
    return ciConstant(T_INT, con.as_int() & 0xFFFF);
  }
  return ciConstant(); // T_ILLEGAL
}

// Try to constant-fold a stable array element.
const Type* Type::make_constant_from_array_element(ciArray* array, int off, int stable_dimension,
                                                   BasicType loadbt, bool is_unsigned_load) {
  // Decode the results of GraphKit::array_element_address.
  ciConstant element_value = array->element_value_by_offset(off);
  if (element_value.basic_type() == T_ILLEGAL) {
    return NULL; // wrong offset
  }
  ciConstant con = check_mismatched_access(element_value, loadbt, is_unsigned_load);

  assert(con.basic_type() != T_ILLEGAL, "elembt=%s; loadbt=%s; unsigned=%d",
         type2name(element_value.basic_type()), type2name(loadbt), is_unsigned_load);

  if (con.is_valid() &&          // not a mismatched access
      !con.is_null_or_zero()) {  // not a default value
    bool is_narrow_oop = (loadbt == T_NARROWOOP);
    return Type::make_from_constant(con, /*require_constant=*/true, stable_dimension, is_narrow_oop, /*is_autobox_cache=*/false);
  }
  return NULL;
}

const Type* Type::make_constant_from_field(ciInstance* holder, int off, bool is_unsigned_load, BasicType loadbt) {
  ciField* field;
  ciType* type = holder->java_mirror_type();
  if (type != NULL && type->is_instance_klass() && off >= InstanceMirrorKlass::offset_of_static_fields()) {
    // Static field
    field = type->as_instance_klass()->get_field_by_offset(off, /*is_static=*/true);
  } else {
    // Instance field
    field = holder->klass()->as_instance_klass()->get_field_by_offset(off, /*is_static=*/false);
  }
  if (field == NULL) {
    return NULL; // Wrong offset
  }
  return Type::make_constant_from_field(field, holder, loadbt, is_unsigned_load);
}

const Type* Type::make_constant_from_field(ciField* field, ciInstance* holder,
                                           BasicType loadbt, bool is_unsigned_load) {
  if (!field->is_constant()) {
    return NULL; // Non-constant field
  }
  ciConstant field_value;
  if (field->is_static()) {
    // final static field
    field_value = field->constant_value();
  } else if (holder != NULL) {
    // final or stable non-static field
    // Treat final non-static fields of trusted classes (classes in
    // java.lang.invoke and sun.invoke packages and subpackages) as
    // compile time constants.
    field_value = field->constant_value_of(holder);
  }
  if (!field_value.is_valid()) {
    return NULL; // Not a constant
  }

  ciConstant con = check_mismatched_access(field_value, loadbt, is_unsigned_load);

  assert(con.is_valid(), "elembt=%s; loadbt=%s; unsigned=%d",
         type2name(field_value.basic_type()), type2name(loadbt), is_unsigned_load);

  bool is_stable_array = FoldStableValues && field->is_stable() && field->type()->is_array_klass();
  int stable_dimension = (is_stable_array ? field->type()->as_array_klass()->dimension() : 0);
  bool is_narrow_oop = (loadbt == T_NARROWOOP);

  const Type* con_type = make_from_constant(con, /*require_constant=*/ true,
                                            stable_dimension, is_narrow_oop,
                                            field->is_autobox_cache());
  if (con_type != NULL && field->is_call_site_target()) {
    ciCallSite* call_site = holder->as_call_site();
    if (!call_site->is_fully_initialized_constant_call_site()) {
      ciMethodHandle* target = con.as_object()->as_method_handle();
      Compile::current()->dependencies()->assert_call_site_target_value(call_site, target);
    }
  }
  return con_type;
}

//------------------------------make-------------------------------------------
// Create a simple Type, with default empty symbol sets.  Then hashcons it
// and look for an existing copy in the type dictionary.
const Type *Type::make( enum TYPES t ) {
  return (new Type(t))->hashcons();
}

//------------------------------cmp--------------------------------------------
int Type::cmp( const Type *const t1, const Type *const t2 ) {
  if( t1->_base != t2->_base )
    return 1;                   // Missed badly
  assert(t1 != t2 || t1->eq(t2), "eq must be reflexive");
  return !t1->eq(t2);           // Return ZERO if equal
}

const Type* Type::maybe_remove_speculative(bool include_speculative) const {
  if (!include_speculative) {
    return remove_speculative();
  }
  return this;
}

//------------------------------hash-------------------------------------------
int Type::uhash( const Type *const t ) {
  return t->hash();
}

#define SMALLINT ((juint)3)  // a value too insignificant to consider widening
#define POSITIVE_INFINITE_F 0x7f800000 // hex representation for IEEE 754 single precision positive infinite
#define POSITIVE_INFINITE_D 0x7ff0000000000000 // hex representation for IEEE 754 double precision positive infinite

//--------------------------Initialize_shared----------------------------------
void Type::Initialize_shared(Compile* current) {
  // This method does not need to be locked because the first system
  // compilations (stub compilations) occur serially.  If they are
  // changed to proceed in parallel, then this section will need
  // locking.

  Arena* save = current->type_arena();
  Arena* shared_type_arena = new (mtCompiler)Arena(mtCompiler);

  current->set_type_arena(shared_type_arena);
  _shared_type_dict =
    new (shared_type_arena) Dict( (CmpKey)Type::cmp, (Hash)Type::uhash,
                                  shared_type_arena, 128 );
  current->set_type_dict(_shared_type_dict);

  // Make shared pre-built types.
  CONTROL = make(Control);      // Control only
  TOP     = make(Top);          // No values in set
  MEMORY  = make(Memory);       // Abstract store only
  ABIO    = make(Abio);         // State-of-machine only
  RETURN_ADDRESS=make(Return_Address);
  FLOAT   = make(FloatBot);     // All floats
  DOUBLE  = make(DoubleBot);    // All doubles
  BOTTOM  = make(Bottom);       // Everything
  HALF    = make(Half);         // Placeholder half of doublewide type

  TypeF::MAX = TypeF::make(max_jfloat); // Float MAX
  TypeF::MIN = TypeF::make(min_jfloat); // Float MIN
  TypeF::ZERO = TypeF::make(0.0); // Float 0 (positive zero)
  TypeF::ONE  = TypeF::make(1.0); // Float 1
  TypeF::POS_INF = TypeF::make(jfloat_cast(POSITIVE_INFINITE_F));
  TypeF::NEG_INF = TypeF::make(-jfloat_cast(POSITIVE_INFINITE_F));

  TypeD::MAX = TypeD::make(max_jdouble); // Double MAX
  TypeD::MIN = TypeD::make(min_jdouble); // Double MIN
  TypeD::ZERO = TypeD::make(0.0); // Double 0 (positive zero)
  TypeD::ONE  = TypeD::make(1.0); // Double 1
  TypeD::POS_INF = TypeD::make(jdouble_cast(POSITIVE_INFINITE_D));
  TypeD::NEG_INF = TypeD::make(-jdouble_cast(POSITIVE_INFINITE_D));

  TypeInt::MAX = TypeInt::make(max_jint); // Int MAX
  TypeInt::MIN = TypeInt::make(min_jint); // Int MIN
  TypeInt::MINUS_1 = TypeInt::make(-1);  // -1
  TypeInt::ZERO    = TypeInt::make( 0);  //  0
  TypeInt::ONE     = TypeInt::make( 1);  //  1
  TypeInt::BOOL    = TypeInt::make(0,1,   WidenMin);  // 0 or 1, FALSE or TRUE.
  TypeInt::CC      = TypeInt::make(-1, 1, WidenMin);  // -1, 0 or 1, condition codes
  TypeInt::CC_LT   = TypeInt::make(-1,-1, WidenMin);  // == TypeInt::MINUS_1
  TypeInt::CC_GT   = TypeInt::make( 1, 1, WidenMin);  // == TypeInt::ONE
  TypeInt::CC_EQ   = TypeInt::make( 0, 0, WidenMin);  // == TypeInt::ZERO
  TypeInt::CC_LE   = TypeInt::make(-1, 0, WidenMin);
  TypeInt::CC_GE   = TypeInt::make( 0, 1, WidenMin);  // == TypeInt::BOOL
  TypeInt::BYTE    = TypeInt::make(-128,127,     WidenMin); // Bytes
  TypeInt::UBYTE   = TypeInt::make(0, 255,       WidenMin); // Unsigned Bytes
  TypeInt::CHAR    = TypeInt::make(0,65535,      WidenMin); // Java chars
  TypeInt::SHORT   = TypeInt::make(-32768,32767, WidenMin); // Java shorts
  TypeInt::POS     = TypeInt::make(0,max_jint,   WidenMin); // Non-neg values
  TypeInt::POS1    = TypeInt::make(1,max_jint,   WidenMin); // Positive values
  TypeInt::INT     = TypeInt::make(min_jint,max_jint, WidenMax); // 32-bit integers
  TypeInt::SYMINT  = TypeInt::make(-max_jint,max_jint,WidenMin); // symmetric range
  TypeInt::TYPE_DOMAIN  = TypeInt::INT;
  // CmpL is overloaded both as the bytecode computation returning
  // a trinary (-1,0,+1) integer result AND as an efficient long
  // compare returning optimizer ideal-type flags.
  assert( TypeInt::CC_LT == TypeInt::MINUS_1, "types must match for CmpL to work" );
  assert( TypeInt::CC_GT == TypeInt::ONE,     "types must match for CmpL to work" );
  assert( TypeInt::CC_EQ == TypeInt::ZERO,    "types must match for CmpL to work" );
  assert( TypeInt::CC_GE == TypeInt::BOOL,    "types must match for CmpL to work" );
  assert( (juint)(TypeInt::CC->_hi - TypeInt::CC->_lo) <= SMALLINT, "CC is truly small");

  TypeLong::MAX = TypeLong::make(max_jlong);  // Long MAX
  TypeLong::MIN = TypeLong::make(min_jlong);  // Long MIN
  TypeLong::MINUS_1 = TypeLong::make(-1);        // -1
  TypeLong::ZERO    = TypeLong::make( 0);        //  0
  TypeLong::ONE     = TypeLong::make( 1);        //  1
  TypeLong::POS     = TypeLong::make(0,max_jlong, WidenMin); // Non-neg values
  TypeLong::LONG    = TypeLong::make(min_jlong,max_jlong,WidenMax); // 64-bit integers
  TypeLong::INT     = TypeLong::make((jlong)min_jint,(jlong)max_jint,WidenMin);
  TypeLong::UINT    = TypeLong::make(0,(jlong)max_juint,WidenMin);
  TypeLong::TYPE_DOMAIN  = TypeLong::LONG;

  const Type **fboth =(const Type**)shared_type_arena->AmallocWords(2*sizeof(Type*));
  fboth[0] = Type::CONTROL;
  fboth[1] = Type::CONTROL;
  TypeTuple::IFBOTH = TypeTuple::make( 2, fboth );

  const Type **ffalse =(const Type**)shared_type_arena->AmallocWords(2*sizeof(Type*));
  ffalse[0] = Type::CONTROL;
  ffalse[1] = Type::TOP;
  TypeTuple::IFFALSE = TypeTuple::make( 2, ffalse );

  const Type **fneither =(const Type**)shared_type_arena->AmallocWords(2*sizeof(Type*));
  fneither[0] = Type::TOP;
  fneither[1] = Type::TOP;
  TypeTuple::IFNEITHER = TypeTuple::make( 2, fneither );

  const Type **ftrue =(const Type**)shared_type_arena->AmallocWords(2*sizeof(Type*));
  ftrue[0] = Type::TOP;
  ftrue[1] = Type::CONTROL;
  TypeTuple::IFTRUE = TypeTuple::make( 2, ftrue );

  const Type **floop =(const Type**)shared_type_arena->AmallocWords(2*sizeof(Type*));
  floop[0] = Type::CONTROL;
  floop[1] = TypeInt::INT;
  TypeTuple::LOOPBODY = TypeTuple::make( 2, floop );

  TypePtr::NULL_PTR= TypePtr::make(AnyPtr, TypePtr::Null, 0);
  TypePtr::NOTNULL = TypePtr::make(AnyPtr, TypePtr::NotNull, OffsetBot);
  TypePtr::BOTTOM  = TypePtr::make(AnyPtr, TypePtr::BotPTR, OffsetBot);

  TypeRawPtr::BOTTOM = TypeRawPtr::make( TypePtr::BotPTR );
  TypeRawPtr::NOTNULL= TypeRawPtr::make( TypePtr::NotNull );

  const Type **fmembar = TypeTuple::fields(0);
  TypeTuple::MEMBAR = TypeTuple::make(TypeFunc::Parms+0, fmembar);

  const Type **fsc = (const Type**)shared_type_arena->AmallocWords(2*sizeof(Type*));
  fsc[0] = TypeInt::CC;
  fsc[1] = Type::MEMORY;
  TypeTuple::STORECONDITIONAL = TypeTuple::make(2, fsc);

  TypeInstPtr::NOTNULL = TypeInstPtr::make(TypePtr::NotNull, current->env()->Object_klass());
  TypeInstPtr::BOTTOM  = TypeInstPtr::make(TypePtr::BotPTR,  current->env()->Object_klass());
  TypeInstPtr::MIRROR  = TypeInstPtr::make(TypePtr::NotNull, current->env()->Class_klass());
  TypeInstPtr::MARK    = TypeInstPtr::make(TypePtr::BotPTR,  current->env()->Object_klass(),
                                           false, 0, oopDesc::mark_offset_in_bytes());
  TypeInstPtr::KLASS   = TypeInstPtr::make(TypePtr::BotPTR,  current->env()->Object_klass(),
                                           false, 0, oopDesc::klass_offset_in_bytes());
  TypeOopPtr::BOTTOM  = TypeOopPtr::make(TypePtr::BotPTR, OffsetBot, TypeOopPtr::InstanceBot);

  TypeMetadataPtr::BOTTOM = TypeMetadataPtr::make(TypePtr::BotPTR, NULL, OffsetBot);

  TypeNarrowOop::NULL_PTR = TypeNarrowOop::make( TypePtr::NULL_PTR );
  TypeNarrowOop::BOTTOM   = TypeNarrowOop::make( TypeInstPtr::BOTTOM );

  TypeNarrowKlass::NULL_PTR = TypeNarrowKlass::make( TypePtr::NULL_PTR );

  mreg2type[Op_Node] = Type::BOTTOM;
  mreg2type[Op_Set ] = 0;
  mreg2type[Op_RegN] = TypeNarrowOop::BOTTOM;
  mreg2type[Op_RegI] = TypeInt::INT;
  mreg2type[Op_RegP] = TypePtr::BOTTOM;
  mreg2type[Op_RegF] = Type::FLOAT;
  mreg2type[Op_RegD] = Type::DOUBLE;
  mreg2type[Op_RegL] = TypeLong::LONG;
  mreg2type[Op_RegFlags] = TypeInt::CC;

  TypeAryPtr::RANGE   = TypeAryPtr::make( TypePtr::BotPTR, TypeAry::make(Type::BOTTOM,TypeInt::POS), NULL /* current->env()->Object_klass() */, false, arrayOopDesc::length_offset_in_bytes());

  TypeAryPtr::NARROWOOPS = TypeAryPtr::make(TypePtr::BotPTR, TypeAry::make(TypeNarrowOop::BOTTOM, TypeInt::POS), NULL /*ciArrayKlass::make(o)*/,  false,  Type::OffsetBot);

#ifdef _LP64
  if (UseCompressedOops) {
    assert(TypeAryPtr::NARROWOOPS->is_ptr_to_narrowoop(), "array of narrow oops must be ptr to narrow oop");
    TypeAryPtr::OOPS  = TypeAryPtr::NARROWOOPS;
  } else
#endif
  {
    // There is no shared klass for Object[].  See note in TypeAryPtr::klass().
    TypeAryPtr::OOPS  = TypeAryPtr::make(TypePtr::BotPTR, TypeAry::make(TypeInstPtr::BOTTOM,TypeInt::POS), NULL /*ciArrayKlass::make(o)*/,  false,  Type::OffsetBot);
  }
  TypeAryPtr::BYTES   = TypeAryPtr::make(TypePtr::BotPTR, TypeAry::make(TypeInt::BYTE      ,TypeInt::POS), ciTypeArrayKlass::make(T_BYTE),   true,  Type::OffsetBot);
  TypeAryPtr::SHORTS  = TypeAryPtr::make(TypePtr::BotPTR, TypeAry::make(TypeInt::SHORT     ,TypeInt::POS), ciTypeArrayKlass::make(T_SHORT),  true,  Type::OffsetBot);
  TypeAryPtr::CHARS   = TypeAryPtr::make(TypePtr::BotPTR, TypeAry::make(TypeInt::CHAR      ,TypeInt::POS), ciTypeArrayKlass::make(T_CHAR),   true,  Type::OffsetBot);
  TypeAryPtr::INTS    = TypeAryPtr::make(TypePtr::BotPTR, TypeAry::make(TypeInt::INT       ,TypeInt::POS), ciTypeArrayKlass::make(T_INT),    true,  Type::OffsetBot);
  TypeAryPtr::LONGS   = TypeAryPtr::make(TypePtr::BotPTR, TypeAry::make(TypeLong::LONG     ,TypeInt::POS), ciTypeArrayKlass::make(T_LONG),   true,  Type::OffsetBot);
  TypeAryPtr::FLOATS  = TypeAryPtr::make(TypePtr::BotPTR, TypeAry::make(Type::FLOAT        ,TypeInt::POS), ciTypeArrayKlass::make(T_FLOAT),  true,  Type::OffsetBot);
  TypeAryPtr::DOUBLES = TypeAryPtr::make(TypePtr::BotPTR, TypeAry::make(Type::DOUBLE       ,TypeInt::POS), ciTypeArrayKlass::make(T_DOUBLE), true,  Type::OffsetBot);

  // Nobody should ask _array_body_type[T_NARROWOOP]. Use NULL as assert.
  TypeAryPtr::_array_body_type[T_NARROWOOP] = NULL;
  TypeAryPtr::_array_body_type[T_OBJECT]  = TypeAryPtr::OOPS;
  TypeAryPtr::_array_body_type[T_ARRAY]   = TypeAryPtr::OOPS; // arrays are stored in oop arrays
  TypeAryPtr::_array_body_type[T_BYTE]    = TypeAryPtr::BYTES;
  TypeAryPtr::_array_body_type[T_BOOLEAN] = TypeAryPtr::BYTES;  // boolean[] is a byte array
  TypeAryPtr::_array_body_type[T_SHORT]   = TypeAryPtr::SHORTS;
  TypeAryPtr::_array_body_type[T_CHAR]    = TypeAryPtr::CHARS;
  TypeAryPtr::_array_body_type[T_INT]     = TypeAryPtr::INTS;
  TypeAryPtr::_array_body_type[T_LONG]    = TypeAryPtr::LONGS;
  TypeAryPtr::_array_body_type[T_FLOAT]   = TypeAryPtr::FLOATS;
  TypeAryPtr::_array_body_type[T_DOUBLE]  = TypeAryPtr::DOUBLES;

  TypeKlassPtr::OBJECT = TypeKlassPtr::make( TypePtr::NotNull, current->env()->Object_klass(), 0 );
  TypeKlassPtr::OBJECT_OR_NULL = TypeKlassPtr::make( TypePtr::BotPTR, current->env()->Object_klass(), 0 );

  const Type **fi2c = TypeTuple::fields(2);
  fi2c[TypeFunc::Parms+0] = TypeInstPtr::BOTTOM; // Method*
  fi2c[TypeFunc::Parms+1] = TypeRawPtr::BOTTOM; // argument pointer
  TypeTuple::START_I2C = TypeTuple::make(TypeFunc::Parms+2, fi2c);

  const Type **intpair = TypeTuple::fields(2);
  intpair[0] = TypeInt::INT;
  intpair[1] = TypeInt::INT;
  TypeTuple::INT_PAIR = TypeTuple::make(2, intpair);

  const Type **longpair = TypeTuple::fields(2);
  longpair[0] = TypeLong::LONG;
  longpair[1] = TypeLong::LONG;
  TypeTuple::LONG_PAIR = TypeTuple::make(2, longpair);

  const Type **intccpair = TypeTuple::fields(2);
  intccpair[0] = TypeInt::INT;
  intccpair[1] = TypeInt::CC;
  TypeTuple::INT_CC_PAIR = TypeTuple::make(2, intccpair);

  const Type **longccpair = TypeTuple::fields(2);
  longccpair[0] = TypeLong::LONG;
  longccpair[1] = TypeInt::CC;
  TypeTuple::LONG_CC_PAIR = TypeTuple::make(2, longccpair);

  _const_basic_type[T_NARROWOOP]   = TypeNarrowOop::BOTTOM;
  _const_basic_type[T_NARROWKLASS] = Type::BOTTOM;
  _const_basic_type[T_BOOLEAN]     = TypeInt::BOOL;
  _const_basic_type[T_CHAR]        = TypeInt::CHAR;
  _const_basic_type[T_BYTE]        = TypeInt::BYTE;
  _const_basic_type[T_SHORT]       = TypeInt::SHORT;
  _const_basic_type[T_INT]         = TypeInt::INT;
  _const_basic_type[T_LONG]        = TypeLong::LONG;
  _const_basic_type[T_FLOAT]       = Type::FLOAT;
  _const_basic_type[T_DOUBLE]      = Type::DOUBLE;
  _const_basic_type[T_OBJECT]      = TypeInstPtr::BOTTOM;
  _const_basic_type[T_ARRAY]       = TypeInstPtr::BOTTOM; // there is no separate bottom for arrays
  _const_basic_type[T_VOID]        = TypePtr::NULL_PTR;   // reflection represents void this way
  _const_basic_type[T_ADDRESS]     = TypeRawPtr::BOTTOM;  // both interpreter return addresses & random raw ptrs
  _const_basic_type[T_CONFLICT]    = Type::BOTTOM;        // why not?

  _zero_type[T_NARROWOOP]   = TypeNarrowOop::NULL_PTR;
  _zero_type[T_NARROWKLASS] = TypeNarrowKlass::NULL_PTR;
  _zero_type[T_BOOLEAN]     = TypeInt::ZERO;     // false == 0
  _zero_type[T_CHAR]        = TypeInt::ZERO;     // '\0' == 0
  _zero_type[T_BYTE]        = TypeInt::ZERO;     // 0x00 == 0
  _zero_type[T_SHORT]       = TypeInt::ZERO;     // 0x0000 == 0
  _zero_type[T_INT]         = TypeInt::ZERO;
  _zero_type[T_LONG]        = TypeLong::ZERO;
  _zero_type[T_FLOAT]       = TypeF::ZERO;
  _zero_type[T_DOUBLE]      = TypeD::ZERO;
  _zero_type[T_OBJECT]      = TypePtr::NULL_PTR;
  _zero_type[T_ARRAY]       = TypePtr::NULL_PTR; // null array is null oop
  _zero_type[T_ADDRESS]     = TypePtr::NULL_PTR; // raw pointers use the same null
  _zero_type[T_VOID]        = Type::TOP;         // the only void value is no value at all

  // get_zero_type() should not happen for T_CONFLICT
  _zero_type[T_CONFLICT]= NULL;

  TypeVect::VECTMASK = (TypeVect*)(new TypeVectMask(TypeInt::BOOL, MaxVectorSize))->hashcons();
  mreg2type[Op_RegVectMask] = TypeVect::VECTMASK;

  if (Matcher::supports_scalable_vector()) {
    TypeVect::VECTA = TypeVect::make(T_BYTE, Matcher::scalable_vector_reg_size(T_BYTE));
  }

  // Vector predefined types, it needs initialized _const_basic_type[].
  if (Matcher::vector_size_supported(T_BYTE,4)) {
    TypeVect::VECTS = TypeVect::make(T_BYTE,4);
  }
  if (Matcher::vector_size_supported(T_FLOAT,2)) {
    TypeVect::VECTD = TypeVect::make(T_FLOAT,2);
  }
  if (Matcher::vector_size_supported(T_FLOAT,4)) {
    TypeVect::VECTX = TypeVect::make(T_FLOAT,4);
  }
  if (Matcher::vector_size_supported(T_FLOAT,8)) {
    TypeVect::VECTY = TypeVect::make(T_FLOAT,8);
  }
  if (Matcher::vector_size_supported(T_FLOAT,16)) {
    TypeVect::VECTZ = TypeVect::make(T_FLOAT,16);
  }

  mreg2type[Op_VecA] = TypeVect::VECTA;
  mreg2type[Op_VecS] = TypeVect::VECTS;
  mreg2type[Op_VecD] = TypeVect::VECTD;
  mreg2type[Op_VecX] = TypeVect::VECTX;
  mreg2type[Op_VecY] = TypeVect::VECTY;
  mreg2type[Op_VecZ] = TypeVect::VECTZ;

  // Restore working type arena.
  current->set_type_arena(save);
  current->set_type_dict(NULL);
}

//------------------------------Initialize-------------------------------------
void Type::Initialize(Compile* current) {
  assert(current->type_arena() != NULL, "must have created type arena");

  if (_shared_type_dict == NULL) {
    Initialize_shared(current);
  }

  Arena* type_arena = current->type_arena();

  // Create the hash-cons'ing dictionary with top-level storage allocation
  Dict *tdic = new (type_arena) Dict(*_shared_type_dict, type_arena);
  current->set_type_dict(tdic);
}

//------------------------------hashcons---------------------------------------
// Do the hash-cons trick.  If the Type already exists in the type table,
// delete the current Type and return the existing Type.  Otherwise stick the
// current Type in the Type table.
const Type *Type::hashcons(void) {
  debug_only(base());           // Check the assertion in Type::base().
  // Look up the Type in the Type dictionary
  Dict *tdic = type_dict();
  Type* old = (Type*)(tdic->Insert(this, this, false));
  if( old ) {                   // Pre-existing Type?
    if( old != this )           // Yes, this guy is not the pre-existing?
      delete this;              // Yes, Nuke this guy
    assert( old->_dual, "" );
    return old;                 // Return pre-existing
  }

  // Every type has a dual (to make my lattice symmetric).
  // Since we just discovered a new Type, compute its dual right now.
  assert( !_dual, "" );         // No dual yet
  _dual = xdual();              // Compute the dual
  if (cmp(this, _dual) == 0) {  // Handle self-symmetric
    if (_dual != this) {
      delete _dual;
      _dual = this;
    }
    return this;
  }
  assert( !_dual->_dual, "" );  // No reverse dual yet
  assert( !(*tdic)[_dual], "" ); // Dual not in type system either
  // New Type, insert into Type table
  tdic->Insert((void*)_dual,(void*)_dual);
  ((Type*)_dual)->_dual = this; // Finish up being symmetric
#ifdef ASSERT
  Type *dual_dual = (Type*)_dual->xdual();
  assert( eq(dual_dual), "xdual(xdual()) should be identity" );
  delete dual_dual;
#endif
  return this;                  // Return new Type
}

//------------------------------eq---------------------------------------------
// Structural equality check for Type representations
bool Type::eq( const Type * ) const {
  return true;                  // Nothing else can go wrong
}

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int Type::hash(void) const {
  return _base;
}

//------------------------------is_finite--------------------------------------
// Has a finite value
bool Type::is_finite() const {
  return false;
}

//------------------------------is_nan-----------------------------------------
// Is not a number (NaN)
bool Type::is_nan()    const {
  return false;
}

//----------------------interface_vs_oop---------------------------------------
#ifdef ASSERT
bool Type::interface_vs_oop_helper(const Type *t) const {
  bool result = false;

  const TypePtr* this_ptr = this->make_ptr(); // In case it is narrow_oop
  const TypePtr*    t_ptr =    t->make_ptr();
  if( this_ptr == NULL || t_ptr == NULL )
    return result;

  const TypeInstPtr* this_inst = this_ptr->isa_instptr();
  const TypeInstPtr*    t_inst =    t_ptr->isa_instptr();
  if( this_inst && this_inst->is_loaded() && t_inst && t_inst->is_loaded() ) {
    bool this_interface = this_inst->klass()->is_interface();
    bool    t_interface =    t_inst->klass()->is_interface();
    result = this_interface ^ t_interface;
  }

  return result;
}

bool Type::interface_vs_oop(const Type *t) const {
  if (interface_vs_oop_helper(t)) {
    return true;
  }
  // Now check the speculative parts as well
  const TypePtr* this_spec = isa_ptr() != NULL ? is_ptr()->speculative() : NULL;
  const TypePtr* t_spec = t->isa_ptr() != NULL ? t->is_ptr()->speculative() : NULL;
  if (this_spec != NULL && t_spec != NULL) {
    if (this_spec->interface_vs_oop_helper(t_spec)) {
      return true;
    }
    return false;
  }
  if (this_spec != NULL && this_spec->interface_vs_oop_helper(t)) {
    return true;
  }
  if (t_spec != NULL && interface_vs_oop_helper(t_spec)) {
    return true;
  }
  return false;
}

#endif

void Type::check_symmetrical(const Type* t, const Type* mt) const {
#ifdef ASSERT
  const Type* mt2 = t->xmeet(this);
  if (mt != mt2) {
    tty->print_cr("=== Meet Not Commutative ===");
    tty->print("t           = ");   t->dump(); tty->cr();
    tty->print("this        = ");      dump(); tty->cr();
    tty->print("t meet this = "); mt2->dump(); tty->cr();
    tty->print("this meet t = ");  mt->dump(); tty->cr();
    fatal("meet not commutative");
  }
  const Type* dual_join = mt->_dual;
  const Type* t2t    = dual_join->xmeet(t->_dual);
  const Type* t2this = dual_join->xmeet(this->_dual);

  // Interface meet Oop is Not Symmetric:
  // Interface:AnyNull meet Oop:AnyNull == Interface:AnyNull
  // Interface:NotNull meet Oop:NotNull == java/lang/Object:NotNull

  if (!interface_vs_oop(t) && (t2t != t->_dual || t2this != this->_dual)) {
    tty->print_cr("=== Meet Not Symmetric ===");
    tty->print("t   =                   ");              t->dump(); tty->cr();
    tty->print("this=                   ");                 dump(); tty->cr();
    tty->print("mt=(t meet this)=       ");             mt->dump(); tty->cr();

    tty->print("t_dual=                 ");       t->_dual->dump(); tty->cr();
    tty->print("this_dual=              ");          _dual->dump(); tty->cr();
    tty->print("mt_dual=                ");      mt->_dual->dump(); tty->cr();

    tty->print("mt_dual meet t_dual=    "); t2t           ->dump(); tty->cr();
    tty->print("mt_dual meet this_dual= "); t2this        ->dump(); tty->cr();

    fatal("meet not symmetric");
  }
#endif
}

//------------------------------meet-------------------------------------------
// Compute the MEET of two types.  NOT virtual.  It enforces that meet is
// commutative and the lattice is symmetric.
const Type *Type::meet_helper(const Type *t, bool include_speculative) const {
  if (isa_narrowoop() && t->isa_narrowoop()) {
    const Type* result = make_ptr()->meet_helper(t->make_ptr(), include_speculative);
    return result->make_narrowoop();
  }
  if (isa_narrowklass() && t->isa_narrowklass()) {
    const Type* result = make_ptr()->meet_helper(t->make_ptr(), include_speculative);
    return result->make_narrowklass();
  }

  const Type *this_t = maybe_remove_speculative(include_speculative);
  t = t->maybe_remove_speculative(include_speculative);

  const Type *mt = this_t->xmeet(t);
#ifdef ASSERT
  if (isa_narrowoop() || t->isa_narrowoop()) return mt;
  if (isa_narrowklass() || t->isa_narrowklass()) return mt;
  Compile* C = Compile::current();
  if (!C->_type_verify_symmetry) {
    return mt;
  }
  this_t->check_symmetrical(t, mt);
  // In the case of an array, computing the meet above, caused the
  // computation of the meet of the elements which at verification
  // time caused the computation of the meet of the dual of the
  // elements. Computing the meet of the dual of the arrays here
  // causes the meet of the dual of the elements to be computed which
  // would cause the meet of the dual of the dual of the elements,
  // that is the meet of the elements already computed above to be
  // computed. Avoid redundant computations by requesting no
  // verification.
  C->_type_verify_symmetry = false;
  const Type *mt_dual = this_t->_dual->xmeet(t->_dual);
  this_t->_dual->check_symmetrical(t->_dual, mt_dual);
  assert(!C->_type_verify_symmetry, "shouldn't have changed");
  C->_type_verify_symmetry = true;
#endif
  return mt;
}

//------------------------------xmeet------------------------------------------
// Compute the MEET of two types.  It returns a new Type object.
const Type *Type::xmeet( const Type *t ) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type-rep?

  // Meeting TOP with anything?
  if( _base == Top ) return t;

  // Meeting BOTTOM with anything?
  if( _base == Bottom ) return BOTTOM;

  // Current "this->_base" is one of: Bad, Multi, Control, Top,
  // Abio, Abstore, Floatxxx, Doublexxx, Bottom, lastype.
  switch (t->base()) {  // Switch on original type

  // Cut in half the number of cases I must handle.  Only need cases for when
  // the given enum "t->type" is less than or equal to the local enum "type".
  case FloatCon:
  case DoubleCon:
  case Int:
  case Long:
    return t->xmeet(this);

  case OopPtr:
    return t->xmeet(this);

  case InstPtr:
    return t->xmeet(this);

  case MetadataPtr:
  case KlassPtr:
    return t->xmeet(this);

  case AryPtr:
    return t->xmeet(this);

  case NarrowOop:
    return t->xmeet(this);

  case NarrowKlass:
    return t->xmeet(this);

  case Bad:                     // Type check
  default:                      // Bogus type not in lattice
    typerr(t);
    return Type::BOTTOM;

  case Bottom:                  // Ye Olde Default
    return t;

  case FloatTop:
    if( _base == FloatTop ) return this;
  case FloatBot:                // Float
    if( _base == FloatBot || _base == FloatTop ) return FLOAT;
    if( _base == DoubleTop || _base == DoubleBot ) return Type::BOTTOM;
    typerr(t);
    return Type::BOTTOM;

  case DoubleTop:
    if( _base == DoubleTop ) return this;
  case DoubleBot:               // Double
    if( _base == DoubleBot || _base == DoubleTop ) return DOUBLE;
    if( _base == FloatTop || _base == FloatBot ) return Type::BOTTOM;
    typerr(t);
    return Type::BOTTOM;

  // These next few cases must match exactly or it is a compile-time error.
  case Control:                 // Control of code
  case Abio:                    // State of world outside of program
  case Memory:
    if( _base == t->_base )  return this;
    typerr(t);
    return Type::BOTTOM;

  case Top:                     // Top of the lattice
    return this;
  }

  // The type is unchanged
  return this;
}

//-----------------------------filter------------------------------------------
const Type *Type::filter_helper(const Type *kills, bool include_speculative) const {
  const Type* ft = join_helper(kills, include_speculative);
  if (ft->empty())
    return Type::TOP;           // Canonical empty value
  return ft;
}

//------------------------------xdual------------------------------------------

const Type *Type::xdual() const {
  // Note: the base() accessor asserts the sanity of _base.
  assert(_type_info[base()].dual_type != Bad, "implement with v-call");
  return new Type(_type_info[_base].dual_type);
}

//------------------------------has_memory-------------------------------------
bool Type::has_memory() const {
  Type::TYPES tx = base();
  if (tx == Memory) return true;
  if (tx == Tuple) {
    const TypeTuple *t = is_tuple();
    for (uint i=0; i < t->cnt(); i++) {
      tx = t->field_at(i)->base();
      if (tx == Memory)  return true;
    }
  }
  return false;
}

#ifndef PRODUCT
//------------------------------dump2------------------------------------------
void Type::dump2( Dict &d, uint depth, outputStream *st ) const {
  st->print("%s", _type_info[_base].msg);
}

//------------------------------dump-------------------------------------------
void Type::dump_on(outputStream *st) const {
  ResourceMark rm;
  Dict d(cmpkey,hashkey);       // Stop recursive type dumping
  dump2(d,1, st);
  if (is_ptr_to_narrowoop()) {
    st->print(" [narrow]");
  } else if (is_ptr_to_narrowklass()) {
    st->print(" [narrowklass]");
  }
}

//-----------------------------------------------------------------------------
const char* Type::str(const Type* t) {
  stringStream ss;
  t->dump_on(&ss);
  return ss.as_string();
}
#endif

//------------------------------singleton--------------------------------------
// TRUE if Type is a singleton type, FALSE otherwise.   Singletons are simple
// constants (Ldi nodes).  Singletons are integer, float or double constants.
bool Type::singleton(void) const {
  return _base == Top || _base == Half;
}

//------------------------------empty------------------------------------------
// TRUE if Type is a type with no values, FALSE otherwise.
bool Type::empty(void) const {
  switch (_base) {
  case DoubleTop:
  case FloatTop:
  case Top:
    return true;

  case Half:
  case Abio:
  case Return_Address:
  case Memory:
  case Bottom:
  case FloatBot:
  case DoubleBot:
    return false;  // never a singleton, therefore never empty

  default:
    ShouldNotReachHere();
    return false;
  }
}

//------------------------------dump_stats-------------------------------------
// Dump collected statistics to stderr
#ifndef PRODUCT
void Type::dump_stats() {
  tty->print("Types made: %d\n", type_dict()->Size());
}
#endif

//------------------------------category---------------------------------------
#ifndef PRODUCT
Type::Category Type::category() const {
  const TypeTuple* tuple;
  switch (base()) {
    case Type::Int:
    case Type::Long:
    case Type::Half:
    case Type::NarrowOop:
    case Type::NarrowKlass:
    case Type::Array:
    case Type::VectorA:
    case Type::VectorS:
    case Type::VectorD:
    case Type::VectorX:
    case Type::VectorY:
    case Type::VectorZ:
    case Type::VectorMask:
    case Type::AnyPtr:
    case Type::RawPtr:
    case Type::OopPtr:
    case Type::InstPtr:
    case Type::AryPtr:
    case Type::MetadataPtr:
    case Type::KlassPtr:
    case Type::Function:
    case Type::Return_Address:
    case Type::FloatTop:
    case Type::FloatCon:
    case Type::FloatBot:
    case Type::DoubleTop:
    case Type::DoubleCon:
    case Type::DoubleBot:
      return Category::Data;
    case Type::Memory:
      return Category::Memory;
    case Type::Control:
      return Category::Control;
    case Type::Top:
    case Type::Abio:
    case Type::Bottom:
      return Category::Other;
    case Type::Bad:
    case Type::lastype:
      return Category::Undef;
    case Type::Tuple:
      // Recursive case. Return CatMixed if the tuple contains types of
      // different categories (e.g. CallStaticJavaNode's type), or the specific
      // category if all types are of the same category (e.g. IfNode's type).
      tuple = is_tuple();
      if (tuple->cnt() == 0) {
        return Category::Undef;
      } else {
        Category first = tuple->field_at(0)->category();
        for (uint i = 1; i < tuple->cnt(); i++) {
          if (tuple->field_at(i)->category() != first) {
            return Category::Mixed;
          }
        }
        return first;
      }
    default:
      assert(false, "unmatched base type: all base types must be categorized");
  }
  return Category::Undef;
}
#endif

//------------------------------typerr-----------------------------------------
void Type::typerr( const Type *t ) const {
#ifndef PRODUCT
  tty->print("\nError mixing types: ");
  dump();
  tty->print(" and ");
  t->dump();
  tty->print("\n");
#endif
  ShouldNotReachHere();
}


//=============================================================================
// Convenience common pre-built types.
const TypeF *TypeF::MAX;        // Floating point max
const TypeF *TypeF::MIN;        // Floating point min
const TypeF *TypeF::ZERO;       // Floating point zero
const TypeF *TypeF::ONE;        // Floating point one
const TypeF *TypeF::POS_INF;    // Floating point positive infinity
const TypeF *TypeF::NEG_INF;    // Floating point negative infinity

//------------------------------make-------------------------------------------
// Create a float constant
const TypeF *TypeF::make(float f) {
  return (TypeF*)(new TypeF(f))->hashcons();
}

//------------------------------meet-------------------------------------------
// Compute the MEET of two types.  It returns a new Type object.
const Type *TypeF::xmeet( const Type *t ) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type-rep?

  // Current "this->_base" is FloatCon
  switch (t->base()) {          // Switch on original type
  case AnyPtr:                  // Mixing with oops happens when javac
  case RawPtr:                  // reuses local variables
  case OopPtr:
  case InstPtr:
  case AryPtr:
  case MetadataPtr:
  case KlassPtr:
  case NarrowOop:
  case NarrowKlass:
  case Int:
  case Long:
  case DoubleTop:
  case DoubleCon:
  case DoubleBot:
  case Bottom:                  // Ye Olde Default
    return Type::BOTTOM;

  case FloatBot:
    return t;

  default:                      // All else is a mistake
    typerr(t);

  case FloatCon:                // Float-constant vs Float-constant?
    if( jint_cast(_f) != jint_cast(t->getf()) )         // unequal constants?
                                // must compare bitwise as positive zero, negative zero and NaN have
                                // all the same representation in C++
      return FLOAT;             // Return generic float
                                // Equal constants
  case Top:
  case FloatTop:
    break;                      // Return the float constant
  }
  return this;                  // Return the float constant
}

//------------------------------xdual------------------------------------------
// Dual: symmetric
const Type *TypeF::xdual() const {
  return this;
}

//------------------------------eq---------------------------------------------
// Structural equality check for Type representations
bool TypeF::eq(const Type *t) const {
  // Bitwise comparison to distinguish between +/-0. These values must be treated
  // as different to be consistent with C1 and the interpreter.
  return (jint_cast(_f) == jint_cast(t->getf()));
}

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int TypeF::hash(void) const {
  return *(int*)(&_f);
}

//------------------------------is_finite--------------------------------------
// Has a finite value
bool TypeF::is_finite() const {
  return g_isfinite(getf()) != 0;
}

//------------------------------is_nan-----------------------------------------
// Is not a number (NaN)
bool TypeF::is_nan()    const {
  return g_isnan(getf()) != 0;
}

//------------------------------dump2------------------------------------------
// Dump float constant Type
#ifndef PRODUCT
void TypeF::dump2( Dict &d, uint depth, outputStream *st ) const {
  Type::dump2(d,depth, st);
  st->print("%f", _f);
}
#endif

//------------------------------singleton--------------------------------------
// TRUE if Type is a singleton type, FALSE otherwise.   Singletons are simple
// constants (Ldi nodes).  Singletons are integer, float or double constants
// or a single symbol.
bool TypeF::singleton(void) const {
  return true;                  // Always a singleton
}

bool TypeF::empty(void) const {
  return false;                 // always exactly a singleton
}

//=============================================================================
// Convenience common pre-built types.
const TypeD *TypeD::MAX;        // Floating point max
const TypeD *TypeD::MIN;        // Floating point min
const TypeD *TypeD::ZERO;       // Floating point zero
const TypeD *TypeD::ONE;        // Floating point one
const TypeD *TypeD::POS_INF;    // Floating point positive infinity
const TypeD *TypeD::NEG_INF;    // Floating point negative infinity

//------------------------------make-------------------------------------------
const TypeD *TypeD::make(double d) {
  return (TypeD*)(new TypeD(d))->hashcons();
}

//------------------------------meet-------------------------------------------
// Compute the MEET of two types.  It returns a new Type object.
const Type *TypeD::xmeet( const Type *t ) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type-rep?

  // Current "this->_base" is DoubleCon
  switch (t->base()) {          // Switch on original type
  case AnyPtr:                  // Mixing with oops happens when javac
  case RawPtr:                  // reuses local variables
  case OopPtr:
  case InstPtr:
  case AryPtr:
  case MetadataPtr:
  case KlassPtr:
  case NarrowOop:
  case NarrowKlass:
  case Int:
  case Long:
  case FloatTop:
  case FloatCon:
  case FloatBot:
  case Bottom:                  // Ye Olde Default
    return Type::BOTTOM;

  case DoubleBot:
    return t;

  default:                      // All else is a mistake
    typerr(t);

  case DoubleCon:               // Double-constant vs Double-constant?
    if( jlong_cast(_d) != jlong_cast(t->getd()) )       // unequal constants? (see comment in TypeF::xmeet)
      return DOUBLE;            // Return generic double
  case Top:
  case DoubleTop:
    break;
  }
  return this;                  // Return the double constant
}

//------------------------------xdual------------------------------------------
// Dual: symmetric
const Type *TypeD::xdual() const {
  return this;
}

//------------------------------eq---------------------------------------------
// Structural equality check for Type representations
bool TypeD::eq(const Type *t) const {
  // Bitwise comparison to distinguish between +/-0. These values must be treated
  // as different to be consistent with C1 and the interpreter.
  return (jlong_cast(_d) == jlong_cast(t->getd()));
}

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int TypeD::hash(void) const {
  return *(int*)(&_d);
}

//------------------------------is_finite--------------------------------------
// Has a finite value
bool TypeD::is_finite() const {
  return g_isfinite(getd()) != 0;
}

//------------------------------is_nan-----------------------------------------
// Is not a number (NaN)
bool TypeD::is_nan()    const {
  return g_isnan(getd()) != 0;
}

//------------------------------dump2------------------------------------------
// Dump double constant Type
#ifndef PRODUCT
void TypeD::dump2( Dict &d, uint depth, outputStream *st ) const {
  Type::dump2(d,depth,st);
  st->print("%f", _d);
}
#endif

//------------------------------singleton--------------------------------------
// TRUE if Type is a singleton type, FALSE otherwise.   Singletons are simple
// constants (Ldi nodes).  Singletons are integer, float or double constants
// or a single symbol.
bool TypeD::singleton(void) const {
  return true;                  // Always a singleton
}

bool TypeD::empty(void) const {
  return false;                 // always exactly a singleton
}

const TypeInteger* TypeInteger::make(jlong lo, jlong hi, int w, BasicType bt) {
  if (bt == T_INT) {
    return TypeInt::make(checked_cast<jint>(lo), checked_cast<jint>(hi), w);
  }
  assert(bt == T_LONG, "basic type not an int or long");
  return TypeLong::make(lo, hi, w);
}

jlong TypeInteger::get_con_as_long(BasicType bt) const {
  if (bt == T_INT) {
    return is_int()->get_con();
  }
  assert(bt == T_LONG, "basic type not an int or long");
  return is_long()->get_con();
}

const TypeInteger* TypeInteger::bottom(BasicType bt) {
  if (bt == T_INT) {
    return TypeInt::INT;
  }
  assert(bt == T_LONG, "basic type not an int or long");
  return TypeLong::LONG;
}

//=============================================================================
// Convience common pre-built types.
const TypeInt *TypeInt::MAX;    // INT_MAX
const TypeInt *TypeInt::MIN;    // INT_MIN
const TypeInt *TypeInt::MINUS_1;// -1
const TypeInt *TypeInt::ZERO;   // 0
const TypeInt *TypeInt::ONE;    // 1
const TypeInt *TypeInt::BOOL;   // 0 or 1, FALSE or TRUE.
const TypeInt *TypeInt::CC;     // -1,0 or 1, condition codes
const TypeInt *TypeInt::CC_LT;  // [-1]  == MINUS_1
const TypeInt *TypeInt::CC_GT;  // [1]   == ONE
const TypeInt *TypeInt::CC_EQ;  // [0]   == ZERO
const TypeInt *TypeInt::CC_LE;  // [-1,0]
const TypeInt *TypeInt::CC_GE;  // [0,1] == BOOL (!)
const TypeInt *TypeInt::BYTE;   // Bytes, -128 to 127
const TypeInt *TypeInt::UBYTE;  // Unsigned Bytes, 0 to 255
const TypeInt *TypeInt::CHAR;   // Java chars, 0-65535
const TypeInt *TypeInt::SHORT;  // Java shorts, -32768-32767
const TypeInt *TypeInt::POS;    // Positive 32-bit integers or zero
const TypeInt *TypeInt::POS1;   // Positive 32-bit integers
const TypeInt *TypeInt::INT;    // 32-bit integers
const TypeInt *TypeInt::SYMINT; // symmetric range [-max_jint..max_jint]
const TypeInt *TypeInt::TYPE_DOMAIN; // alias for TypeInt::INT

//------------------------------TypeInt----------------------------------------
TypeInt::TypeInt( jint lo, jint hi, int w ) : TypeInteger(Int), _lo(lo), _hi(hi), _widen(w) {
}

//------------------------------make-------------------------------------------
const TypeInt *TypeInt::make( jint lo ) {
  return (TypeInt*)(new TypeInt(lo,lo,WidenMin))->hashcons();
}

static int normalize_int_widen( jint lo, jint hi, int w ) {
  // Certain normalizations keep us sane when comparing types.
  // The 'SMALLINT' covers constants and also CC and its relatives.
  if (lo <= hi) {
    if (((juint)hi - lo) <= SMALLINT)  w = Type::WidenMin;
    if (((juint)hi - lo) >= max_juint) w = Type::WidenMax; // TypeInt::INT
  } else {
    if (((juint)lo - hi) <= SMALLINT)  w = Type::WidenMin;
    if (((juint)lo - hi) >= max_juint) w = Type::WidenMin; // dual TypeInt::INT
  }
  return w;
}

const TypeInt *TypeInt::make( jint lo, jint hi, int w ) {
  w = normalize_int_widen(lo, hi, w);
  return (TypeInt*)(new TypeInt(lo,hi,w))->hashcons();
}

//------------------------------meet-------------------------------------------
// Compute the MEET of two types.  It returns a new Type representation object
// with reference count equal to the number of Types pointing at it.
// Caller should wrap a Types around it.
const Type *TypeInt::xmeet( const Type *t ) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type?

  // Currently "this->_base" is a TypeInt
  switch (t->base()) {          // Switch on original type
  case AnyPtr:                  // Mixing with oops happens when javac
  case RawPtr:                  // reuses local variables
  case OopPtr:
  case InstPtr:
  case AryPtr:
  case MetadataPtr:
  case KlassPtr:
  case NarrowOop:
  case NarrowKlass:
  case Long:
  case FloatTop:
  case FloatCon:
  case FloatBot:
  case DoubleTop:
  case DoubleCon:
  case DoubleBot:
  case Bottom:                  // Ye Olde Default
    return Type::BOTTOM;
  default:                      // All else is a mistake
    typerr(t);
  case Top:                     // No change
    return this;
  case Int:                     // Int vs Int?
    break;
  }

  // Expand covered set
  const TypeInt *r = t->is_int();
  return make( MIN2(_lo,r->_lo), MAX2(_hi,r->_hi), MAX2(_widen,r->_widen) );
}

//------------------------------xdual------------------------------------------
// Dual: reverse hi & lo; flip widen
const Type *TypeInt::xdual() const {
  int w = normalize_int_widen(_hi,_lo, WidenMax-_widen);
  return new TypeInt(_hi,_lo,w);
}

//------------------------------widen------------------------------------------
// Only happens for optimistic top-down optimizations.
const Type *TypeInt::widen( const Type *old, const Type* limit ) const {
  // Coming from TOP or such; no widening
  if( old->base() != Int ) return this;
  const TypeInt *ot = old->is_int();

  // If new guy is equal to old guy, no widening
  if( _lo == ot->_lo && _hi == ot->_hi )
    return old;

  // If new guy contains old, then we widened
  if( _lo <= ot->_lo && _hi >= ot->_hi ) {
    // New contains old
    // If new guy is already wider than old, no widening
    if( _widen > ot->_widen ) return this;
    // If old guy was a constant, do not bother
    if (ot->_lo == ot->_hi)  return this;
    // Now widen new guy.
    // Check for widening too far
    if (_widen == WidenMax) {
      int max = max_jint;
      int min = min_jint;
      if (limit->isa_int()) {
        max = limit->is_int()->_hi;
        min = limit->is_int()->_lo;
      }
      if (min < _lo && _hi < max) {
        // If neither endpoint is extremal yet, push out the endpoint
        // which is closer to its respective limit.
        if (_lo >= 0 ||                 // easy common case
            (juint)(_lo - min) >= (juint)(max - _hi)) {
          // Try to widen to an unsigned range type of 31 bits:
          return make(_lo, max, WidenMax);
        } else {
          return make(min, _hi, WidenMax);
        }
      }
      return TypeInt::INT;
    }
    // Returned widened new guy
    return make(_lo,_hi,_widen+1);
  }

  // If old guy contains new, then we probably widened too far & dropped to
  // bottom.  Return the wider fellow.
  if ( ot->_lo <= _lo && ot->_hi >= _hi )
    return old;

  //fatal("Integer value range is not subset");
  //return this;
  return TypeInt::INT;
}

//------------------------------narrow---------------------------------------
// Only happens for pessimistic optimizations.
const Type *TypeInt::narrow( const Type *old ) const {
  if (_lo >= _hi)  return this;   // already narrow enough
  if (old == NULL)  return this;
  const TypeInt* ot = old->isa_int();
  if (ot == NULL)  return this;
  jint olo = ot->_lo;
  jint ohi = ot->_hi;

  // If new guy is equal to old guy, no narrowing
  if (_lo == olo && _hi == ohi)  return old;

  // If old guy was maximum range, allow the narrowing
  if (olo == min_jint && ohi == max_jint)  return this;

  if (_lo < olo || _hi > ohi)
    return this;                // doesn't narrow; pretty wierd

  // The new type narrows the old type, so look for a "death march".
  // See comments on PhaseTransform::saturate.
  juint nrange = (juint)_hi - _lo;
  juint orange = (juint)ohi - olo;
  if (nrange < max_juint - 1 && nrange > (orange >> 1) + (SMALLINT*2)) {
    // Use the new type only if the range shrinks a lot.
    // We do not want the optimizer computing 2^31 point by point.
    return old;
  }

  return this;
}

//-----------------------------filter------------------------------------------
const Type *TypeInt::filter_helper(const Type *kills, bool include_speculative) const {
  const TypeInt* ft = join_helper(kills, include_speculative)->isa_int();
  if (ft == NULL || ft->empty())
    return Type::TOP;           // Canonical empty value
  if (ft->_widen < this->_widen) {
    // Do not allow the value of kill->_widen to affect the outcome.
    // The widen bits must be allowed to run freely through the graph.
    ft = TypeInt::make(ft->_lo, ft->_hi, this->_widen);
  }
  return ft;
}

//------------------------------eq---------------------------------------------
// Structural equality check for Type representations
bool TypeInt::eq( const Type *t ) const {
  const TypeInt *r = t->is_int(); // Handy access
  return r->_lo == _lo && r->_hi == _hi && r->_widen == _widen;
}

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int TypeInt::hash(void) const {
  return java_add(java_add(_lo, _hi), java_add((jint)_widen, (jint)Type::Int));
}

//------------------------------is_finite--------------------------------------
// Has a finite value
bool TypeInt::is_finite() const {
  return true;
}

//------------------------------dump2------------------------------------------
// Dump TypeInt
#ifndef PRODUCT
static const char* intname(char* buf, jint n) {
  if (n == min_jint)
    return "min";
  else if (n < min_jint + 10000)
    sprintf(buf, "min+" INT32_FORMAT, n - min_jint);
  else if (n == max_jint)
    return "max";
  else if (n > max_jint - 10000)
    sprintf(buf, "max-" INT32_FORMAT, max_jint - n);
  else
    sprintf(buf, INT32_FORMAT, n);
  return buf;
}

void TypeInt::dump2( Dict &d, uint depth, outputStream *st ) const {
  char buf[40], buf2[40];
  if (_lo == min_jint && _hi == max_jint)
    st->print("int");
  else if (is_con())
    st->print("int:%s", intname(buf, get_con()));
  else if (_lo == BOOL->_lo && _hi == BOOL->_hi)
    st->print("bool");
  else if (_lo == BYTE->_lo && _hi == BYTE->_hi)
    st->print("byte");
  else if (_lo == CHAR->_lo && _hi == CHAR->_hi)
    st->print("char");
  else if (_lo == SHORT->_lo && _hi == SHORT->_hi)
    st->print("short");
  else if (_hi == max_jint)
    st->print("int:>=%s", intname(buf, _lo));
  else if (_lo == min_jint)
    st->print("int:<=%s", intname(buf, _hi));
  else
    st->print("int:%s..%s", intname(buf, _lo), intname(buf2, _hi));

  if (_widen != 0 && this != TypeInt::INT)
    st->print(":%.*s", _widen, "wwww");
}
#endif

//------------------------------singleton--------------------------------------
// TRUE if Type is a singleton type, FALSE otherwise.   Singletons are simple
// constants.
bool TypeInt::singleton(void) const {
  return _lo >= _hi;
}

bool TypeInt::empty(void) const {
  return _lo > _hi;
}

//=============================================================================
// Convenience common pre-built types.
const TypeLong *TypeLong::MAX;
const TypeLong *TypeLong::MIN;
const TypeLong *TypeLong::MINUS_1;// -1
const TypeLong *TypeLong::ZERO; // 0
const TypeLong *TypeLong::ONE;  // 1
const TypeLong *TypeLong::POS;  // >=0
const TypeLong *TypeLong::LONG; // 64-bit integers
const TypeLong *TypeLong::INT;  // 32-bit subrange
const TypeLong *TypeLong::UINT; // 32-bit unsigned subrange
const TypeLong *TypeLong::TYPE_DOMAIN; // alias for TypeLong::LONG

//------------------------------TypeLong---------------------------------------
TypeLong::TypeLong(jlong lo, jlong hi, int w) : TypeInteger(Long), _lo(lo), _hi(hi), _widen(w) {
}

//------------------------------make-------------------------------------------
const TypeLong *TypeLong::make( jlong lo ) {
  return (TypeLong*)(new TypeLong(lo,lo,WidenMin))->hashcons();
}

static int normalize_long_widen( jlong lo, jlong hi, int w ) {
  // Certain normalizations keep us sane when comparing types.
  // The 'SMALLINT' covers constants.
  if (lo <= hi) {
    if (((julong)hi - lo) <= SMALLINT)   w = Type::WidenMin;
    if (((julong)hi - lo) >= max_julong) w = Type::WidenMax; // TypeLong::LONG
  } else {
    if (((julong)lo - hi) <= SMALLINT)   w = Type::WidenMin;
    if (((julong)lo - hi) >= max_julong) w = Type::WidenMin; // dual TypeLong::LONG
  }
  return w;
}

const TypeLong *TypeLong::make( jlong lo, jlong hi, int w ) {
  w = normalize_long_widen(lo, hi, w);
  return (TypeLong*)(new TypeLong(lo,hi,w))->hashcons();
}


//------------------------------meet-------------------------------------------
// Compute the MEET of two types.  It returns a new Type representation object
// with reference count equal to the number of Types pointing at it.
// Caller should wrap a Types around it.
const Type *TypeLong::xmeet( const Type *t ) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type?

  // Currently "this->_base" is a TypeLong
  switch (t->base()) {          // Switch on original type
  case AnyPtr:                  // Mixing with oops happens when javac
  case RawPtr:                  // reuses local variables
  case OopPtr:
  case InstPtr:
  case AryPtr:
  case MetadataPtr:
  case KlassPtr:
  case NarrowOop:
  case NarrowKlass:
  case Int:
  case FloatTop:
  case FloatCon:
  case FloatBot:
  case DoubleTop:
  case DoubleCon:
  case DoubleBot:
  case Bottom:                  // Ye Olde Default
    return Type::BOTTOM;
  default:                      // All else is a mistake
    typerr(t);
  case Top:                     // No change
    return this;
  case Long:                    // Long vs Long?
    break;
  }

  // Expand covered set
  const TypeLong *r = t->is_long(); // Turn into a TypeLong
  return make( MIN2(_lo,r->_lo), MAX2(_hi,r->_hi), MAX2(_widen,r->_widen) );
}

//------------------------------xdual------------------------------------------
// Dual: reverse hi & lo; flip widen
const Type *TypeLong::xdual() const {
  int w = normalize_long_widen(_hi,_lo, WidenMax-_widen);
  return new TypeLong(_hi,_lo,w);
}

//------------------------------widen------------------------------------------
// Only happens for optimistic top-down optimizations.
const Type *TypeLong::widen( const Type *old, const Type* limit ) const {
  // Coming from TOP or such; no widening
  if( old->base() != Long ) return this;
  const TypeLong *ot = old->is_long();

  // If new guy is equal to old guy, no widening
  if( _lo == ot->_lo && _hi == ot->_hi )
    return old;

  // If new guy contains old, then we widened
  if( _lo <= ot->_lo && _hi >= ot->_hi ) {
    // New contains old
    // If new guy is already wider than old, no widening
    if( _widen > ot->_widen ) return this;
    // If old guy was a constant, do not bother
    if (ot->_lo == ot->_hi)  return this;
    // Now widen new guy.
    // Check for widening too far
    if (_widen == WidenMax) {
      jlong max = max_jlong;
      jlong min = min_jlong;
      if (limit->isa_long()) {
        max = limit->is_long()->_hi;
        min = limit->is_long()->_lo;
      }
      if (min < _lo && _hi < max) {
        // If neither endpoint is extremal yet, push out the endpoint
        // which is closer to its respective limit.
        if (_lo >= 0 ||                 // easy common case
            ((julong)_lo - min) >= ((julong)max - _hi)) {
          // Try to widen to an unsigned range type of 32/63 bits:
          if (max >= max_juint && _hi < max_juint)
            return make(_lo, max_juint, WidenMax);
          else
            return make(_lo, max, WidenMax);
        } else {
          return make(min, _hi, WidenMax);
        }
      }
      return TypeLong::LONG;
    }
    // Returned widened new guy
    return make(_lo,_hi,_widen+1);
  }

  // If old guy contains new, then we probably widened too far & dropped to
  // bottom.  Return the wider fellow.
  if ( ot->_lo <= _lo && ot->_hi >= _hi )
    return old;

  //  fatal("Long value range is not subset");
  // return this;
  return TypeLong::LONG;
}

//------------------------------narrow----------------------------------------
// Only happens for pessimistic optimizations.
const Type *TypeLong::narrow( const Type *old ) const {
  if (_lo >= _hi)  return this;   // already narrow enough
  if (old == NULL)  return this;
  const TypeLong* ot = old->isa_long();
  if (ot == NULL)  return this;
  jlong olo = ot->_lo;
  jlong ohi = ot->_hi;

  // If new guy is equal to old guy, no narrowing
  if (_lo == olo && _hi == ohi)  return old;

  // If old guy was maximum range, allow the narrowing
  if (olo == min_jlong && ohi == max_jlong)  return this;

  if (_lo < olo || _hi > ohi)
    return this;                // doesn't narrow; pretty wierd

  // The new type narrows the old type, so look for a "death march".
  // See comments on PhaseTransform::saturate.
  julong nrange = _hi - _lo;
  julong orange = ohi - olo;
  if (nrange < max_julong - 1 && nrange > (orange >> 1) + (SMALLINT*2)) {
    // Use the new type only if the range shrinks a lot.
    // We do not want the optimizer computing 2^31 point by point.
    return old;
  }

  return this;
}

//-----------------------------filter------------------------------------------
const Type *TypeLong::filter_helper(const Type *kills, bool include_speculative) const {
  const TypeLong* ft = join_helper(kills, include_speculative)->isa_long();
  if (ft == NULL || ft->empty())
    return Type::TOP;           // Canonical empty value
  if (ft->_widen < this->_widen) {
    // Do not allow the value of kill->_widen to affect the outcome.
    // The widen bits must be allowed to run freely through the graph.
    ft = TypeLong::make(ft->_lo, ft->_hi, this->_widen);
  }
  return ft;
}

//------------------------------eq---------------------------------------------
// Structural equality check for Type representations
bool TypeLong::eq( const Type *t ) const {
  const TypeLong *r = t->is_long(); // Handy access
  return r->_lo == _lo &&  r->_hi == _hi  && r->_widen == _widen;
}

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int TypeLong::hash(void) const {
  return (int)(_lo+_hi+_widen+(int)Type::Long);
}

//------------------------------is_finite--------------------------------------
// Has a finite value
bool TypeLong::is_finite() const {
  return true;
}

//------------------------------dump2------------------------------------------
// Dump TypeLong
#ifndef PRODUCT
static const char* longnamenear(jlong x, const char* xname, char* buf, jlong n) {
  if (n > x) {
    if (n >= x + 10000)  return NULL;
    sprintf(buf, "%s+" JLONG_FORMAT, xname, n - x);
  } else if (n < x) {
    if (n <= x - 10000)  return NULL;
    sprintf(buf, "%s-" JLONG_FORMAT, xname, x - n);
  } else {
    return xname;
  }
  return buf;
}

static const char* longname(char* buf, jlong n) {
  const char* str;
  if (n == min_jlong)
    return "min";
  else if (n < min_jlong + 10000)
    sprintf(buf, "min+" JLONG_FORMAT, n - min_jlong);
  else if (n == max_jlong)
    return "max";
  else if (n > max_jlong - 10000)
    sprintf(buf, "max-" JLONG_FORMAT, max_jlong - n);
  else if ((str = longnamenear(max_juint, "maxuint", buf, n)) != NULL)
    return str;
  else if ((str = longnamenear(max_jint, "maxint", buf, n)) != NULL)
    return str;
  else if ((str = longnamenear(min_jint, "minint", buf, n)) != NULL)
    return str;
  else
    sprintf(buf, JLONG_FORMAT, n);
  return buf;
}

void TypeLong::dump2( Dict &d, uint depth, outputStream *st ) const {
  char buf[80], buf2[80];
  if (_lo == min_jlong && _hi == max_jlong)
    st->print("long");
  else if (is_con())
    st->print("long:%s", longname(buf, get_con()));
  else if (_hi == max_jlong)
    st->print("long:>=%s", longname(buf, _lo));
  else if (_lo == min_jlong)
    st->print("long:<=%s", longname(buf, _hi));
  else
    st->print("long:%s..%s", longname(buf, _lo), longname(buf2, _hi));

  if (_widen != 0 && this != TypeLong::LONG)
    st->print(":%.*s", _widen, "wwww");
}
#endif

//------------------------------singleton--------------------------------------
// TRUE if Type is a singleton type, FALSE otherwise.   Singletons are simple
// constants
bool TypeLong::singleton(void) const {
  return _lo >= _hi;
}

bool TypeLong::empty(void) const {
  return _lo > _hi;
}

//=============================================================================
// Convenience common pre-built types.
const TypeTuple *TypeTuple::IFBOTH;     // Return both arms of IF as reachable
const TypeTuple *TypeTuple::IFFALSE;
const TypeTuple *TypeTuple::IFTRUE;
const TypeTuple *TypeTuple::IFNEITHER;
const TypeTuple *TypeTuple::LOOPBODY;
const TypeTuple *TypeTuple::MEMBAR;
const TypeTuple *TypeTuple::STORECONDITIONAL;
const TypeTuple *TypeTuple::START_I2C;
const TypeTuple *TypeTuple::INT_PAIR;
const TypeTuple *TypeTuple::LONG_PAIR;
const TypeTuple *TypeTuple::INT_CC_PAIR;
const TypeTuple *TypeTuple::LONG_CC_PAIR;

//------------------------------make-------------------------------------------
// Make a TypeTuple from the range of a method signature
const TypeTuple *TypeTuple::make_range(ciSignature* sig) {
  ciType* return_type = sig->return_type();
  uint arg_cnt = return_type->size();
  const Type **field_array = fields(arg_cnt);
  switch (return_type->basic_type()) {
  case T_LONG:
    field_array[TypeFunc::Parms]   = TypeLong::LONG;
    field_array[TypeFunc::Parms+1] = Type::HALF;
    break;
  case T_DOUBLE:
    field_array[TypeFunc::Parms]   = Type::DOUBLE;
    field_array[TypeFunc::Parms+1] = Type::HALF;
    break;
  case T_OBJECT:
  case T_ARRAY:
  case T_BOOLEAN:
  case T_CHAR:
  case T_FLOAT:
  case T_BYTE:
  case T_SHORT:
  case T_INT:
    field_array[TypeFunc::Parms] = get_const_type(return_type);
    break;
  case T_VOID:
    break;
  default:
    ShouldNotReachHere();
  }
  return (TypeTuple*)(new TypeTuple(TypeFunc::Parms + arg_cnt, field_array))->hashcons();
}

// Make a TypeTuple from the domain of a method signature
const TypeTuple *TypeTuple::make_domain(ciInstanceKlass* recv, ciSignature* sig) {
  uint arg_cnt = sig->size();

  uint pos = TypeFunc::Parms;
  const Type **field_array;
  if (recv != NULL) {
    arg_cnt++;
    field_array = fields(arg_cnt);
    // Use get_const_type here because it respects UseUniqueSubclasses:
    field_array[pos++] = get_const_type(recv)->join_speculative(TypePtr::NOTNULL);
  } else {
    field_array = fields(arg_cnt);
  }

  int i = 0;
  while (pos < TypeFunc::Parms + arg_cnt) {
    ciType* type = sig->type_at(i);

    switch (type->basic_type()) {
    case T_LONG:
      field_array[pos++] = TypeLong::LONG;
      field_array[pos++] = Type::HALF;
      break;
    case T_DOUBLE:
      field_array[pos++] = Type::DOUBLE;
      field_array[pos++] = Type::HALF;
      break;
    case T_OBJECT:
    case T_ARRAY:
    case T_FLOAT:
    case T_INT:
      field_array[pos++] = get_const_type(type);
      break;
    case T_BOOLEAN:
    case T_CHAR:
    case T_BYTE:
    case T_SHORT:
      field_array[pos++] = TypeInt::INT;
      break;
    default:
      ShouldNotReachHere();
    }
    i++;
  }

  return (TypeTuple*)(new TypeTuple(TypeFunc::Parms + arg_cnt, field_array))->hashcons();
}

const TypeTuple *TypeTuple::make( uint cnt, const Type **fields ) {
  return (TypeTuple*)(new TypeTuple(cnt,fields))->hashcons();
}

//------------------------------fields-----------------------------------------
// Subroutine call type with space allocated for argument types
// Memory for Control, I_O, Memory, FramePtr, and ReturnAdr is allocated implicitly
const Type **TypeTuple::fields( uint arg_cnt ) {
  const Type **flds = (const Type **)(Compile::current()->type_arena()->AmallocWords((TypeFunc::Parms+arg_cnt)*sizeof(Type*) ));
  flds[TypeFunc::Control  ] = Type::CONTROL;
  flds[TypeFunc::I_O      ] = Type::ABIO;
  flds[TypeFunc::Memory   ] = Type::MEMORY;
  flds[TypeFunc::FramePtr ] = TypeRawPtr::BOTTOM;
  flds[TypeFunc::ReturnAdr] = Type::RETURN_ADDRESS;

  return flds;
}

//------------------------------meet-------------------------------------------
// Compute the MEET of two types.  It returns a new Type object.
const Type *TypeTuple::xmeet( const Type *t ) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type-rep?

  // Current "this->_base" is Tuple
  switch (t->base()) {          // switch on original type

  case Bottom:                  // Ye Olde Default
    return t;

  default:                      // All else is a mistake
    typerr(t);

  case Tuple: {                 // Meeting 2 signatures?
    const TypeTuple *x = t->is_tuple();
    assert( _cnt == x->_cnt, "" );
    const Type **fields = (const Type **)(Compile::current()->type_arena()->AmallocWords( _cnt*sizeof(Type*) ));
    for( uint i=0; i<_cnt; i++ )
      fields[i] = field_at(i)->xmeet( x->field_at(i) );
    return TypeTuple::make(_cnt,fields);
  }
  case Top:
    break;
  }
  return this;                  // Return the double constant
}

//------------------------------xdual------------------------------------------
// Dual: compute field-by-field dual
const Type *TypeTuple::xdual() const {
  const Type **fields = (const Type **)(Compile::current()->type_arena()->AmallocWords( _cnt*sizeof(Type*) ));
  for( uint i=0; i<_cnt; i++ )
    fields[i] = _fields[i]->dual();
  return new TypeTuple(_cnt,fields);
}

//------------------------------eq---------------------------------------------
// Structural equality check for Type representations
bool TypeTuple::eq( const Type *t ) const {
  const TypeTuple *s = (const TypeTuple *)t;
  if (_cnt != s->_cnt)  return false;  // Unequal field counts
  for (uint i = 0; i < _cnt; i++)
    if (field_at(i) != s->field_at(i)) // POINTER COMPARE!  NO RECURSION!
      return false;             // Missed
  return true;
}

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int TypeTuple::hash(void) const {
  intptr_t sum = _cnt;
  for( uint i=0; i<_cnt; i++ )
    sum += (intptr_t)_fields[i];     // Hash on pointers directly
  return sum;
}

//------------------------------dump2------------------------------------------
// Dump signature Type
#ifndef PRODUCT
void TypeTuple::dump2( Dict &d, uint depth, outputStream *st ) const {
  st->print("{");
  if( !depth || d[this] ) {     // Check for recursive print
    st->print("...}");
    return;
  }
  d.Insert((void*)this, (void*)this);   // Stop recursion
  if( _cnt ) {
    uint i;
    for( i=0; i<_cnt-1; i++ ) {
      st->print("%d:", i);
      _fields[i]->dump2(d, depth-1, st);
      st->print(", ");
    }
    st->print("%d:", i);
    _fields[i]->dump2(d, depth-1, st);
  }
  st->print("}");
}
#endif

//------------------------------singleton--------------------------------------
// TRUE if Type is a singleton type, FALSE otherwise.   Singletons are simple
// constants (Ldi nodes).  Singletons are integer, float or double constants
// or a single symbol.
bool TypeTuple::singleton(void) const {
  return false;                 // Never a singleton
}

bool TypeTuple::empty(void) const {
  for( uint i=0; i<_cnt; i++ ) {
    if (_fields[i]->empty())  return true;
  }
  return false;
}

//=============================================================================
// Convenience common pre-built types.

inline const TypeInt* normalize_array_size(const TypeInt* size) {
  // Certain normalizations keep us sane when comparing types.
  // We do not want arrayOop variables to differ only by the wideness
  // of their index types.  Pick minimum wideness, since that is the
  // forced wideness of small ranges anyway.
  if (size->_widen != Type::WidenMin)
    return TypeInt::make(size->_lo, size->_hi, Type::WidenMin);
  else
    return size;
}

//------------------------------make-------------------------------------------
const TypeAry* TypeAry::make(const Type* elem, const TypeInt* size, bool stable) {
  if (UseCompressedOops && elem->isa_oopptr()) {
    elem = elem->make_narrowoop();
  }
  size = normalize_array_size(size);
  return (TypeAry*)(new TypeAry(elem,size,stable))->hashcons();
}

//------------------------------meet-------------------------------------------
// Compute the MEET of two types.  It returns a new Type object.
const Type *TypeAry::xmeet( const Type *t ) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type-rep?

  // Current "this->_base" is Ary
  switch (t->base()) {          // switch on original type

  case Bottom:                  // Ye Olde Default
    return t;

  default:                      // All else is a mistake
    typerr(t);

  case Array: {                 // Meeting 2 arrays?
    const TypeAry *a = t->is_ary();
    return TypeAry::make(_elem->meet_speculative(a->_elem),
                         _size->xmeet(a->_size)->is_int(),
                         _stable && a->_stable);
  }
  case Top:
    break;
  }
  return this;                  // Return the double constant
}

//------------------------------xdual------------------------------------------
// Dual: compute field-by-field dual
const Type *TypeAry::xdual() const {
  const TypeInt* size_dual = _size->dual()->is_int();
  size_dual = normalize_array_size(size_dual);
  return new TypeAry(_elem->dual(), size_dual, !_stable);
}

//------------------------------eq---------------------------------------------
// Structural equality check for Type representations
bool TypeAry::eq( const Type *t ) const {
  const TypeAry *a = (const TypeAry*)t;
  return _elem == a->_elem &&
    _stable == a->_stable &&
    _size == a->_size;
}

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int TypeAry::hash(void) const {
  return (intptr_t)_elem + (intptr_t)_size + (_stable ? 43 : 0);
}

/**
 * Return same type without a speculative part in the element
 */
const Type* TypeAry::remove_speculative() const {
  return make(_elem->remove_speculative(), _size, _stable);
}

/**
 * Return same type with cleaned up speculative part of element
 */
const Type* TypeAry::cleanup_speculative() const {
  return make(_elem->cleanup_speculative(), _size, _stable);
}

/**
 * Return same type but with a different inline depth (used for speculation)
 *
 * @param depth  depth to meet with
 */
const TypePtr* TypePtr::with_inline_depth(int depth) const {
  if (!UseInlineDepthForSpeculativeTypes) {
    return this;
  }
  return make(AnyPtr, _ptr, _offset, _speculative, depth);
}

//----------------------interface_vs_oop---------------------------------------
#ifdef ASSERT
bool TypeAry::interface_vs_oop(const Type *t) const {
  const TypeAry* t_ary = t->is_ary();
  if (t_ary) {
    const TypePtr* this_ptr = _elem->make_ptr(); // In case we have narrow_oops
    const TypePtr*    t_ptr = t_ary->_elem->make_ptr();
    if(this_ptr != NULL && t_ptr != NULL) {
      return this_ptr->interface_vs_oop(t_ptr);
    }
  }
  return false;
}
#endif

//------------------------------dump2------------------------------------------
#ifndef PRODUCT
void TypeAry::dump2( Dict &d, uint depth, outputStream *st ) const {
  if (_stable)  st->print("stable:");
  _elem->dump2(d, depth, st);
  st->print("[");
  _size->dump2(d, depth, st);
  st->print("]");
}
#endif

//------------------------------singleton--------------------------------------
// TRUE if Type is a singleton type, FALSE otherwise.   Singletons are simple
// constants (Ldi nodes).  Singletons are integer, float or double constants
// or a single symbol.
bool TypeAry::singleton(void) const {
  return false;                 // Never a singleton
}

bool TypeAry::empty(void) const {
  return _elem->empty() || _size->empty();
}

//--------------------------ary_must_be_exact----------------------------------
bool TypeAry::ary_must_be_exact() const {
  // This logic looks at the element type of an array, and returns true
  // if the element type is either a primitive or a final instance class.
  // In such cases, an array built on this ary must have no subclasses.
  if (_elem == BOTTOM)      return false;  // general array not exact
  if (_elem == TOP   )      return false;  // inverted general array not exact
  const TypeOopPtr*  toop = NULL;
  if (UseCompressedOops && _elem->isa_narrowoop()) {
    toop = _elem->make_ptr()->isa_oopptr();
  } else {
    toop = _elem->isa_oopptr();
  }
  if (!toop)                return true;   // a primitive type, like int
  ciKlass* tklass = toop->klass();
  if (tklass == NULL)       return false;  // unloaded class
  if (!tklass->is_loaded()) return false;  // unloaded class
  const TypeInstPtr* tinst;
  if (_elem->isa_narrowoop())
    tinst = _elem->make_ptr()->isa_instptr();
  else
    tinst = _elem->isa_instptr();
  if (tinst)
    return tklass->as_instance_klass()->is_final();
  const TypeAryPtr*  tap;
  if (_elem->isa_narrowoop())
    tap = _elem->make_ptr()->isa_aryptr();
  else
    tap = _elem->isa_aryptr();
  if (tap)
    return tap->ary()->ary_must_be_exact();
  return false;
}

//==============================TypeVect=======================================
// Convenience common pre-built types.
const TypeVect *TypeVect::VECTA = NULL; // vector length agnostic
const TypeVect *TypeVect::VECTS = NULL; //  32-bit vectors
const TypeVect *TypeVect::VECTD = NULL; //  64-bit vectors
const TypeVect *TypeVect::VECTX = NULL; // 128-bit vectors
const TypeVect *TypeVect::VECTY = NULL; // 256-bit vectors
const TypeVect *TypeVect::VECTZ = NULL; // 512-bit vectors
const TypeVect *TypeVect::VECTMASK = NULL; // predicate/mask vector

//------------------------------make-------------------------------------------
const TypeVect* TypeVect::make(const Type *elem, uint length) {
  BasicType elem_bt = elem->array_element_basic_type();
  assert(is_java_primitive(elem_bt), "only primitive types in vector");
  assert(Matcher::vector_size_supported(elem_bt, length), "length in range");
  int size = length * type2aelembytes(elem_bt);
  switch (Matcher::vector_ideal_reg(size)) {
  case Op_VecA:
    return (TypeVect*)(new TypeVectA(elem, length))->hashcons();
  case Op_VecS:
    return (TypeVect*)(new TypeVectS(elem, length))->hashcons();
  case Op_RegL:
  case Op_VecD:
  case Op_RegD:
    return (TypeVect*)(new TypeVectD(elem, length))->hashcons();
  case Op_VecX:
    return (TypeVect*)(new TypeVectX(elem, length))->hashcons();
  case Op_VecY:
    return (TypeVect*)(new TypeVectY(elem, length))->hashcons();
  case Op_VecZ:
    return (TypeVect*)(new TypeVectZ(elem, length))->hashcons();
  }
 ShouldNotReachHere();
  return NULL;
}

const TypeVect *TypeVect::makemask(const Type* elem, uint length) {
  if (Matcher::has_predicated_vectors()) {
    const TypeVect* mtype = Matcher::predicate_reg_type(elem, length);
    return (TypeVect*)(const_cast<TypeVect*>(mtype))->hashcons();
  } else {
    return make(elem, length);
  }
}

//------------------------------meet-------------------------------------------
// Compute the MEET of two types.  It returns a new Type object.
const Type *TypeVect::xmeet( const Type *t ) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type-rep?

  // Current "this->_base" is Vector
  switch (t->base()) {          // switch on original type

  case Bottom:                  // Ye Olde Default
    return t;

  default:                      // All else is a mistake
    typerr(t);
  case VectorMask: {
    const TypeVectMask* v = t->is_vectmask();
    assert(  base() == v->base(), "");
    assert(length() == v->length(), "");
    assert(element_basic_type() == v->element_basic_type(), "");
    return TypeVect::makemask(_elem->xmeet(v->_elem), _length);
  }
  case VectorA:
  case VectorS:
  case VectorD:
  case VectorX:
  case VectorY:
  case VectorZ: {                // Meeting 2 vectors?
    const TypeVect* v = t->is_vect();
    assert(  base() == v->base(), "");
    assert(length() == v->length(), "");
    assert(element_basic_type() == v->element_basic_type(), "");
    return TypeVect::make(_elem->xmeet(v->_elem), _length);
  }
  case Top:
    break;
  }
  return this;
}

//------------------------------xdual------------------------------------------
// Dual: compute field-by-field dual
const Type *TypeVect::xdual() const {
  return new TypeVect(base(), _elem->dual(), _length);
}

//------------------------------eq---------------------------------------------
// Structural equality check for Type representations
bool TypeVect::eq(const Type *t) const {
  const TypeVect *v = t->is_vect();
  return (_elem == v->_elem) && (_length == v->_length);
}

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int TypeVect::hash(void) const {
  return (intptr_t)_elem + (intptr_t)_length;
}

//------------------------------singleton--------------------------------------
// TRUE if Type is a singleton type, FALSE otherwise.   Singletons are simple
// constants (Ldi nodes).  Vector is singleton if all elements are the same
// constant value (when vector is created with Replicate code).
bool TypeVect::singleton(void) const {
// There is no Con node for vectors yet.
//  return _elem->singleton();
  return false;
}

bool TypeVect::empty(void) const {
  return _elem->empty();
}

//------------------------------dump2------------------------------------------
#ifndef PRODUCT
void TypeVect::dump2(Dict &d, uint depth, outputStream *st) const {
  switch (base()) {
  case VectorA:
    st->print("vectora["); break;
  case VectorS:
    st->print("vectors["); break;
  case VectorD:
    st->print("vectord["); break;
  case VectorX:
    st->print("vectorx["); break;
  case VectorY:
    st->print("vectory["); break;
  case VectorZ:
    st->print("vectorz["); break;
  case VectorMask:
    st->print("vectormask["); break;
  default:
    ShouldNotReachHere();
  }
  st->print("%d]:{", _length);
  _elem->dump2(d, depth, st);
  st->print("}");
}
#endif

bool TypeVectMask::eq(const Type *t) const {
  const TypeVectMask *v = t->is_vectmask();
  return (element_type() == v->element_type()) && (length() == v->length());
}

const Type *TypeVectMask::xdual() const {
  return new TypeVectMask(element_type()->dual(), length());
}

//=============================================================================
// Convenience common pre-built types.
const TypePtr *TypePtr::NULL_PTR;
const TypePtr *TypePtr::NOTNULL;
const TypePtr *TypePtr::BOTTOM;

//------------------------------meet-------------------------------------------
// Meet over the PTR enum
const TypePtr::PTR TypePtr::ptr_meet[TypePtr::lastPTR][TypePtr::lastPTR] = {
  //              TopPTR,    AnyNull,   Constant, Null,   NotNull, BotPTR,
  { /* Top     */ TopPTR,    AnyNull,   Constant, Null,   NotNull, BotPTR,},
  { /* AnyNull */ AnyNull,   AnyNull,   Constant, BotPTR, NotNull, BotPTR,},
  { /* Constant*/ Constant,  Constant,  Constant, BotPTR, NotNull, BotPTR,},
  { /* Null    */ Null,      BotPTR,    BotPTR,   Null,   BotPTR,  BotPTR,},
  { /* NotNull */ NotNull,   NotNull,   NotNull,  BotPTR, NotNull, BotPTR,},
  { /* BotPTR  */ BotPTR,    BotPTR,    BotPTR,   BotPTR, BotPTR,  BotPTR,}
};

//------------------------------make-------------------------------------------
const TypePtr *TypePtr::make(TYPES t, enum PTR ptr, int offset, const TypePtr* speculative, int inline_depth) {
  return (TypePtr*)(new TypePtr(t,ptr,offset, speculative, inline_depth))->hashcons();
}

//------------------------------cast_to_ptr_type-------------------------------
const Type *TypePtr::cast_to_ptr_type(PTR ptr) const {
  assert(_base == AnyPtr, "subclass must override cast_to_ptr_type");
  if( ptr == _ptr ) return this;
  return make(_base, ptr, _offset, _speculative, _inline_depth);
}

//------------------------------get_con----------------------------------------
intptr_t TypePtr::get_con() const {
  assert( _ptr == Null, "" );
  return _offset;
}

//------------------------------meet-------------------------------------------
// Compute the MEET of two types.  It returns a new Type object.
const Type *TypePtr::xmeet(const Type *t) const {
  const Type* res = xmeet_helper(t);
  if (res->isa_ptr() == NULL) {
    return res;
  }

  const TypePtr* res_ptr = res->is_ptr();
  if (res_ptr->speculative() != NULL) {
    // type->speculative() == NULL means that speculation is no better
    // than type, i.e. type->speculative() == type. So there are 2
    // ways to represent the fact that we have no useful speculative
    // data and we should use a single one to be able to test for
    // equality between types. Check whether type->speculative() ==
    // type and set speculative to NULL if it is the case.
    if (res_ptr->remove_speculative() == res_ptr->speculative()) {
      return res_ptr->remove_speculative();
    }
  }

  return res;
}

const Type *TypePtr::xmeet_helper(const Type *t) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type-rep?

  // Current "this->_base" is AnyPtr
  switch (t->base()) {          // switch on original type
  case Int:                     // Mixing ints & oops happens when javac
  case Long:                    // reuses local variables
  case FloatTop:
  case FloatCon:
  case FloatBot:
  case DoubleTop:
  case DoubleCon:
  case DoubleBot:
  case NarrowOop:
  case NarrowKlass:
  case Bottom:                  // Ye Olde Default
    return Type::BOTTOM;
  case Top:
    return this;

  case AnyPtr: {                // Meeting to AnyPtrs
    const TypePtr *tp = t->is_ptr();
    const TypePtr* speculative = xmeet_speculative(tp);
    int depth = meet_inline_depth(tp->inline_depth());
    return make(AnyPtr, meet_ptr(tp->ptr()), meet_offset(tp->offset()), speculative, depth);
  }
  case RawPtr:                  // For these, flip the call around to cut down
  case OopPtr:
  case InstPtr:                 // on the cases I have to handle.
  case AryPtr:
  case MetadataPtr:
  case KlassPtr:
    return t->xmeet(this);      // Call in reverse direction
  default:                      // All else is a mistake
    typerr(t);

  }
  return this;
}

//------------------------------meet_offset------------------------------------
int TypePtr::meet_offset( int offset ) const {
  // Either is 'TOP' offset?  Return the other offset!
  if( _offset == OffsetTop ) return offset;
  if( offset == OffsetTop ) return _offset;
  // If either is different, return 'BOTTOM' offset
  if( _offset != offset ) return OffsetBot;
  return _offset;
}

//------------------------------dual_offset------------------------------------
int TypePtr::dual_offset( ) const {
  if( _offset == OffsetTop ) return OffsetBot;// Map 'TOP' into 'BOTTOM'
  if( _offset == OffsetBot ) return OffsetTop;// Map 'BOTTOM' into 'TOP'
  return _offset;               // Map everything else into self
}

//------------------------------xdual------------------------------------------
// Dual: compute field-by-field dual
const TypePtr::PTR TypePtr::ptr_dual[TypePtr::lastPTR] = {
  BotPTR, NotNull, Constant, Null, AnyNull, TopPTR
};
const Type *TypePtr::xdual() const {
  return new TypePtr(AnyPtr, dual_ptr(), dual_offset(), dual_speculative(), dual_inline_depth());
}

//------------------------------xadd_offset------------------------------------
int TypePtr::xadd_offset( intptr_t offset ) const {
  // Adding to 'TOP' offset?  Return 'TOP'!
  if( _offset == OffsetTop || offset == OffsetTop ) return OffsetTop;
  // Adding to 'BOTTOM' offset?  Return 'BOTTOM'!
  if( _offset == OffsetBot || offset == OffsetBot ) return OffsetBot;
  // Addition overflows or "accidentally" equals to OffsetTop? Return 'BOTTOM'!
  offset += (intptr_t)_offset;
  if (offset != (int)offset || offset == OffsetTop) return OffsetBot;

  // assert( _offset >= 0 && _offset+offset >= 0, "" );
  // It is possible to construct a negative offset during PhaseCCP

  return (int)offset;        // Sum valid offsets
}

//------------------------------add_offset-------------------------------------
const TypePtr *TypePtr::add_offset( intptr_t offset ) const {
  return make(AnyPtr, _ptr, xadd_offset(offset), _speculative, _inline_depth);
}

//------------------------------eq---------------------------------------------
// Structural equality check for Type representations
bool TypePtr::eq( const Type *t ) const {
  const TypePtr *a = (const TypePtr*)t;
  return _ptr == a->ptr() && _offset == a->offset() && eq_speculative(a) && _inline_depth == a->_inline_depth;
}

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int TypePtr::hash(void) const {
  return java_add(java_add((jint)_ptr, (jint)_offset), java_add((jint)hash_speculative(), (jint)_inline_depth));
;
}

/**
 * Return same type without a speculative part
 */
const Type* TypePtr::remove_speculative() const {
  if (_speculative == NULL) {
    return this;
  }
  assert(_inline_depth == InlineDepthTop || _inline_depth == InlineDepthBottom, "non speculative type shouldn't have inline depth");
  return make(AnyPtr, _ptr, _offset, NULL, _inline_depth);
}

/**
 * Return same type but drop speculative part if we know we won't use
 * it
 */
const Type* TypePtr::cleanup_speculative() const {
  if (speculative() == NULL) {
    return this;
  }
  const Type* no_spec = remove_speculative();
  // If this is NULL_PTR then we don't need the speculative type
  // (with_inline_depth in case the current type inline depth is
  // InlineDepthTop)
  if (no_spec == NULL_PTR->with_inline_depth(inline_depth())) {
    return no_spec;
  }
  if (above_centerline(speculative()->ptr())) {
    return no_spec;
  }
  const TypeOopPtr* spec_oopptr = speculative()->isa_oopptr();
  // If the speculative may be null and is an inexact klass then it
  // doesn't help
  if (speculative() != TypePtr::NULL_PTR && speculative()->maybe_null() &&
      (spec_oopptr == NULL || !spec_oopptr->klass_is_exact())) {
    return no_spec;
  }
  return this;
}

/**
 * dual of the speculative part of the type
 */
const TypePtr* TypePtr::dual_speculative() const {
  if (_speculative == NULL) {
    return NULL;
  }
  return _speculative->dual()->is_ptr();
}

/**
 * meet of the speculative parts of 2 types
 *
 * @param other  type to meet with
 */
const TypePtr* TypePtr::xmeet_speculative(const TypePtr* other) const {
  bool this_has_spec = (_speculative != NULL);
  bool other_has_spec = (other->speculative() != NULL);

  if (!this_has_spec && !other_has_spec) {
    return NULL;
  }

  // If we are at a point where control flow meets and one branch has
  // a speculative type and the other has not, we meet the speculative
  // type of one branch with the actual type of the other. If the
  // actual type is exact and the speculative is as well, then the
  // result is a speculative type which is exact and we can continue
  // speculation further.
  const TypePtr* this_spec = _speculative;
  const TypePtr* other_spec = other->speculative();

  if (!this_has_spec) {
    this_spec = this;
  }

  if (!other_has_spec) {
    other_spec = other;
  }

  return this_spec->meet(other_spec)->is_ptr();
}

/**
 * dual of the inline depth for this type (used for speculation)
 */
int TypePtr::dual_inline_depth() const {
  return -inline_depth();
}

/**
 * meet of 2 inline depths (used for speculation)
 *
 * @param depth  depth to meet with
 */
int TypePtr::meet_inline_depth(int depth) const {
  return MAX2(inline_depth(), depth);
}

/**
 * Are the speculative parts of 2 types equal?
 *
 * @param other  type to compare this one to
 */
bool TypePtr::eq_speculative(const TypePtr* other) const {
  if (_speculative == NULL || other->speculative() == NULL) {
    return _speculative == other->speculative();
  }

  if (_speculative->base() != other->speculative()->base()) {
    return false;
  }

  return _speculative->eq(other->speculative());
}

/**
 * Hash of the speculative part of the type
 */
int TypePtr::hash_speculative() const {
  if (_speculative == NULL) {
    return 0;
  }

  return _speculative->hash();
}

/**
 * add offset to the speculative part of the type
 *
 * @param offset  offset to add
 */
const TypePtr* TypePtr::add_offset_speculative(intptr_t offset) const {
  if (_speculative == NULL) {
    return NULL;
  }
  return _speculative->add_offset(offset)->is_ptr();
}

/**
 * return exact klass from the speculative type if there's one
 */
ciKlass* TypePtr::speculative_type() const {
  if (_speculative != NULL && _speculative->isa_oopptr()) {
    const TypeOopPtr* speculative = _speculative->join(this)->is_oopptr();
    if (speculative->klass_is_exact()) {
      return speculative->klass();
    }
  }
  return NULL;
}

/**
 * return true if speculative type may be null
 */
bool TypePtr::speculative_maybe_null() const {
  if (_speculative != NULL) {
    const TypePtr* speculative = _speculative->join(this)->is_ptr();
    return speculative->maybe_null();
  }
  return true;
}

bool TypePtr::speculative_always_null() const {
  if (_speculative != NULL) {
    const TypePtr* speculative = _speculative->join(this)->is_ptr();
    return speculative == TypePtr::NULL_PTR;
  }
  return false;
}

/**
 * Same as TypePtr::speculative_type() but return the klass only if
 * the speculative tells us is not null
 */
ciKlass* TypePtr::speculative_type_not_null() const {
  if (speculative_maybe_null()) {
    return NULL;
  }
  return speculative_type();
}

/**
 * Check whether new profiling would improve speculative type
 *
 * @param   exact_kls    class from profiling
 * @param   inline_depth inlining depth of profile point
 *
 * @return  true if type profile is valuable
 */
bool TypePtr::would_improve_type(ciKlass* exact_kls, int inline_depth) const {
  // no profiling?
  if (exact_kls == NULL) {
    return false;
  }
  if (speculative() == TypePtr::NULL_PTR) {
    return false;
  }
  // no speculative type or non exact speculative type?
  if (speculative_type() == NULL) {
    return true;
  }
  // If the node already has an exact speculative type keep it,
  // unless it was provided by profiling that is at a deeper
  // inlining level. Profiling at a higher inlining depth is
  // expected to be less accurate.
  if (_speculative->inline_depth() == InlineDepthBottom) {
    return false;
  }
  assert(_speculative->inline_depth() != InlineDepthTop, "can't do the comparison");
  return inline_depth < _speculative->inline_depth();
}

/**
 * Check whether new profiling would improve ptr (= tells us it is non
 * null)
 *
 * @param   ptr_kind always null or not null?
 *
 * @return  true if ptr profile is valuable
 */
bool TypePtr::would_improve_ptr(ProfilePtrKind ptr_kind) const {
  // profiling doesn't tell us anything useful
  if (ptr_kind != ProfileAlwaysNull && ptr_kind != ProfileNeverNull) {
    return false;
  }
  // We already know this is not null
  if (!this->maybe_null()) {
    return false;
  }
  // We already know the speculative type cannot be null
  if (!speculative_maybe_null()) {
    return false;
  }
  // We already know this is always null
  if (this == TypePtr::NULL_PTR) {
    return false;
  }
  // We already know the speculative type is always null
  if (speculative_always_null()) {
    return false;
  }
  if (ptr_kind == ProfileAlwaysNull && speculative() != NULL && speculative()->isa_oopptr()) {
    return false;
  }
  return true;
}

//------------------------------dump2------------------------------------------
const char *const TypePtr::ptr_msg[TypePtr::lastPTR] = {
  "TopPTR","AnyNull","Constant","NULL","NotNull","BotPTR"
};

#ifndef PRODUCT
void TypePtr::dump2( Dict &d, uint depth, outputStream *st ) const {
  if( _ptr == Null ) st->print("NULL");
  else st->print("%s *", ptr_msg[_ptr]);
  if( _offset == OffsetTop ) st->print("+top");
  else if( _offset == OffsetBot ) st->print("+bot");
  else if( _offset ) st->print("+%d", _offset);
  dump_inline_depth(st);
  dump_speculative(st);
}

/**
 *dump the speculative part of the type
 */
void TypePtr::dump_speculative(outputStream *st) const {
  if (_speculative != NULL) {
    st->print(" (speculative=");
    _speculative->dump_on(st);
    st->print(")");
  }
}

/**
 *dump the inline depth of the type
 */
void TypePtr::dump_inline_depth(outputStream *st) const {
  if (_inline_depth != InlineDepthBottom) {
    if (_inline_depth == InlineDepthTop) {
      st->print(" (inline_depth=InlineDepthTop)");
    } else {
      st->print(" (inline_depth=%d)", _inline_depth);
    }
  }
}
#endif

//------------------------------singleton--------------------------------------
// TRUE if Type is a singleton type, FALSE otherwise.   Singletons are simple
// constants
bool TypePtr::singleton(void) const {
  // TopPTR, Null, AnyNull, Constant are all singletons
  return (_offset != OffsetBot) && !below_centerline(_ptr);
}

bool TypePtr::empty(void) const {
  return (_offset == OffsetTop) || above_centerline(_ptr);
}

//=============================================================================
// Convenience common pre-built types.
const TypeRawPtr *TypeRawPtr::BOTTOM;
const TypeRawPtr *TypeRawPtr::NOTNULL;

//------------------------------make-------------------------------------------
const TypeRawPtr *TypeRawPtr::make( enum PTR ptr ) {
  assert( ptr != Constant, "what is the constant?" );
  assert( ptr != Null, "Use TypePtr for NULL" );
  return (TypeRawPtr*)(new TypeRawPtr(ptr,0))->hashcons();
}

const TypeRawPtr *TypeRawPtr::make( address bits ) {
  assert( bits, "Use TypePtr for NULL" );
  return (TypeRawPtr*)(new TypeRawPtr(Constant,bits))->hashcons();
}

//------------------------------cast_to_ptr_type-------------------------------
const Type *TypeRawPtr::cast_to_ptr_type(PTR ptr) const {
  assert( ptr != Constant, "what is the constant?" );
  assert( ptr != Null, "Use TypePtr for NULL" );
  assert( _bits==0, "Why cast a constant address?");
  if( ptr == _ptr ) return this;
  return make(ptr);
}

//------------------------------get_con----------------------------------------
intptr_t TypeRawPtr::get_con() const {
  assert( _ptr == Null || _ptr == Constant, "" );
  return (intptr_t)_bits;
}

//------------------------------meet-------------------------------------------
// Compute the MEET of two types.  It returns a new Type object.
const Type *TypeRawPtr::xmeet( const Type *t ) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type-rep?

  // Current "this->_base" is RawPtr
  switch( t->base() ) {         // switch on original type
  case Bottom:                  // Ye Olde Default
    return t;
  case Top:
    return this;
  case AnyPtr:                  // Meeting to AnyPtrs
    break;
  case RawPtr: {                // might be top, bot, any/not or constant
    enum PTR tptr = t->is_ptr()->ptr();
    enum PTR ptr = meet_ptr( tptr );
    if( ptr == Constant ) {     // Cannot be equal constants, so...
      if( tptr == Constant && _ptr != Constant)  return t;
      if( _ptr == Constant && tptr != Constant)  return this;
      ptr = NotNull;            // Fall down in lattice
    }
    return make( ptr );
  }

  case OopPtr:
  case InstPtr:
  case AryPtr:
  case MetadataPtr:
  case KlassPtr:
    return TypePtr::BOTTOM;     // Oop meet raw is not well defined
  default:                      // All else is a mistake
    typerr(t);
  }

  // Found an AnyPtr type vs self-RawPtr type
  const TypePtr *tp = t->is_ptr();
  switch (tp->ptr()) {
  case TypePtr::TopPTR:  return this;
  case TypePtr::BotPTR:  return t;
  case TypePtr::Null:
    if( _ptr == TypePtr::TopPTR ) return t;
    return TypeRawPtr::BOTTOM;
  case TypePtr::NotNull: return TypePtr::make(AnyPtr, meet_ptr(TypePtr::NotNull), tp->meet_offset(0), tp->speculative(), tp->inline_depth());
  case TypePtr::AnyNull:
    if( _ptr == TypePtr::Constant) return this;
    return make( meet_ptr(TypePtr::AnyNull) );
  default: ShouldNotReachHere();
  }
  return this;
}

//------------------------------xdual------------------------------------------
// Dual: compute field-by-field dual
const Type *TypeRawPtr::xdual() const {
  return new TypeRawPtr( dual_ptr(), _bits );
}

//------------------------------add_offset-------------------------------------
const TypePtr *TypeRawPtr::add_offset( intptr_t offset ) const {
  if( offset == OffsetTop ) return BOTTOM; // Undefined offset-> undefined pointer
  if( offset == OffsetBot ) return BOTTOM; // Unknown offset-> unknown pointer
  if( offset == 0 ) return this; // No change
  switch (_ptr) {
  case TypePtr::TopPTR:
  case TypePtr::BotPTR:
  case TypePtr::NotNull:
    return this;
  case TypePtr::Null:
  case TypePtr::Constant: {
    address bits = _bits+offset;
    if ( bits == 0 ) return TypePtr::NULL_PTR;
    return make( bits );
  }
  default:  ShouldNotReachHere();
  }
  return NULL;                  // Lint noise
}

//------------------------------eq---------------------------------------------
// Structural equality check for Type representations
bool TypeRawPtr::eq( const Type *t ) const {
  const TypeRawPtr *a = (const TypeRawPtr*)t;
  return _bits == a->_bits && TypePtr::eq(t);
}

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int TypeRawPtr::hash(void) const {
  return (intptr_t)_bits + TypePtr::hash();
}

//------------------------------dump2------------------------------------------
#ifndef PRODUCT
void TypeRawPtr::dump2( Dict &d, uint depth, outputStream *st ) const {
  if( _ptr == Constant )
    st->print(INTPTR_FORMAT, p2i(_bits));
  else
    st->print("rawptr:%s", ptr_msg[_ptr]);
}
#endif

//=============================================================================
// Convenience common pre-built type.
const TypeOopPtr *TypeOopPtr::BOTTOM;

//------------------------------TypeOopPtr-------------------------------------
TypeOopPtr::TypeOopPtr(TYPES t, PTR ptr, ciKlass* k, bool xk, ciObject* o, int offset,
                       int instance_id, const TypePtr* speculative, int inline_depth)
  : TypePtr(t, ptr, offset, speculative, inline_depth),
    _const_oop(o), _klass(k),
    _klass_is_exact(xk),
    _is_ptr_to_narrowoop(false),
    _is_ptr_to_narrowklass(false),
    _is_ptr_to_boxed_value(false),
    _instance_id(instance_id) {
  if (Compile::current()->eliminate_boxing() && (t == InstPtr) &&
      (offset > 0) && xk && (k != 0) && k->is_instance_klass()) {
    _is_ptr_to_boxed_value = k->as_instance_klass()->is_boxed_value_offset(offset);
  }
#ifdef _LP64
  if (_offset > 0 || _offset == Type::OffsetTop || _offset == Type::OffsetBot) {
    if (_offset == oopDesc::klass_offset_in_bytes()) {
      _is_ptr_to_narrowklass = UseCompressedClassPointers;
    } else if (klass() == NULL) {
      // Array with unknown body type
      assert(this->isa_aryptr(), "only arrays without klass");
      _is_ptr_to_narrowoop = UseCompressedOops;
    } else if (this->isa_aryptr()) {
      _is_ptr_to_narrowoop = (UseCompressedOops && klass()->is_obj_array_klass() &&
                             _offset != arrayOopDesc::length_offset_in_bytes());
    } else if (klass()->is_instance_klass()) {
      ciInstanceKlass* ik = klass()->as_instance_klass();
      ciField* field = NULL;
      if (this->isa_klassptr()) {
        // Perm objects don't use compressed references
      } else if (_offset == OffsetBot || _offset == OffsetTop) {
        // unsafe access
        _is_ptr_to_narrowoop = UseCompressedOops;
      } else {
        assert(this->isa_instptr(), "must be an instance ptr.");

        if (klass() == ciEnv::current()->Class_klass() &&
            (_offset == java_lang_Class::klass_offset() ||
             _offset == java_lang_Class::array_klass_offset())) {
          // Special hidden fields from the Class.
          assert(this->isa_instptr(), "must be an instance ptr.");
          _is_ptr_to_narrowoop = false;
        } else if (klass() == ciEnv::current()->Class_klass() &&
                   _offset >= InstanceMirrorKlass::offset_of_static_fields()) {
          // Static fields
          ciField* field = NULL;
          if (const_oop() != NULL) {
            ciInstanceKlass* k = const_oop()->as_instance()->java_lang_Class_klass()->as_instance_klass();
            field = k->get_field_by_offset(_offset, true);
          }
          if (field != NULL) {
            BasicType basic_elem_type = field->layout_type();
            _is_ptr_to_narrowoop = UseCompressedOops && is_reference_type(basic_elem_type);
          } else {
            // unsafe access
            _is_ptr_to_narrowoop = UseCompressedOops;
          }
        } else {
          // Instance fields which contains a compressed oop references.
          field = ik->get_field_by_offset(_offset, false);
          if (field != NULL) {
            BasicType basic_elem_type = field->layout_type();
            _is_ptr_to_narrowoop = UseCompressedOops && is_reference_type(basic_elem_type);
          } else if (klass()->equals(ciEnv::current()->Object_klass())) {
            // Compile::find_alias_type() cast exactness on all types to verify
            // that it does not affect alias type.
            _is_ptr_to_narrowoop = UseCompressedOops;
          } else {
            // Type for the copy start in LibraryCallKit::inline_native_clone().
            _is_ptr_to_narrowoop = UseCompressedOops;
          }
        }
      }
    }
  }
#endif
}

//------------------------------make-------------------------------------------
const TypeOopPtr *TypeOopPtr::make(PTR ptr, int offset, int instance_id,
                                     const TypePtr* speculative, int inline_depth) {
  assert(ptr != Constant, "no constant generic pointers");
  ciKlass*  k = Compile::current()->env()->Object_klass();
  bool      xk = false;
  ciObject* o = NULL;
  return (TypeOopPtr*)(new TypeOopPtr(OopPtr, ptr, k, xk, o, offset, instance_id, speculative, inline_depth))->hashcons();
}


//------------------------------cast_to_ptr_type-------------------------------
const Type *TypeOopPtr::cast_to_ptr_type(PTR ptr) const {
  assert(_base == OopPtr, "subclass must override cast_to_ptr_type");
  if( ptr == _ptr ) return this;
  return make(ptr, _offset, _instance_id, _speculative, _inline_depth);
}

//-----------------------------cast_to_instance_id----------------------------
const TypeOopPtr *TypeOopPtr::cast_to_instance_id(int instance_id) const {
  // There are no instances of a general oop.
  // Return self unchanged.
  return this;
}

//-----------------------------cast_to_exactness-------------------------------
const Type *TypeOopPtr::cast_to_exactness(bool klass_is_exact) const {
  // There is no such thing as an exact general oop.
  // Return self unchanged.
  return this;
}


//------------------------------as_klass_type----------------------------------
// Return the klass type corresponding to this instance or array type.
// It is the type that is loaded from an object of this type.
const TypeKlassPtr* TypeOopPtr::as_klass_type() const {
  ciKlass* k = klass();
  bool    xk = klass_is_exact();
  if (k == NULL)
    return TypeKlassPtr::OBJECT;
  else
    return TypeKlassPtr::make(xk? Constant: NotNull, k, 0);
}

//------------------------------meet-------------------------------------------
// Compute the MEET of two types.  It returns a new Type object.
const Type *TypeOopPtr::xmeet_helper(const Type *t) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type-rep?

  // Current "this->_base" is OopPtr
  switch (t->base()) {          // switch on original type

  case Int:                     // Mixing ints & oops happens when javac
  case Long:                    // reuses local variables
  case FloatTop:
  case FloatCon:
  case FloatBot:
  case DoubleTop:
  case DoubleCon:
  case DoubleBot:
  case NarrowOop:
  case NarrowKlass:
  case Bottom:                  // Ye Olde Default
    return Type::BOTTOM;
  case Top:
    return this;

  default:                      // All else is a mistake
    typerr(t);

  case RawPtr:
  case MetadataPtr:
  case KlassPtr:
    return TypePtr::BOTTOM;     // Oop meet raw is not well defined

  case AnyPtr: {
    // Found an AnyPtr type vs self-OopPtr type
    const TypePtr *tp = t->is_ptr();
    int offset = meet_offset(tp->offset());
    PTR ptr = meet_ptr(tp->ptr());
    const TypePtr* speculative = xmeet_speculative(tp);
    int depth = meet_inline_depth(tp->inline_depth());
    switch (tp->ptr()) {
    case Null:
      if (ptr == Null)  return TypePtr::make(AnyPtr, ptr, offset, speculative, depth);
      // else fall through:
    case TopPTR:
    case AnyNull: {
      int instance_id = meet_instance_id(InstanceTop);
      return make(ptr, offset, instance_id, speculative, depth);
    }
    case BotPTR:
    case NotNull:
      return TypePtr::make(AnyPtr, ptr, offset, speculative, depth);
    default: typerr(t);
    }
  }

  case OopPtr: {                 // Meeting to other OopPtrs
    const TypeOopPtr *tp = t->is_oopptr();
    int instance_id = meet_instance_id(tp->instance_id());
    const TypePtr* speculative = xmeet_speculative(tp);
    int depth = meet_inline_depth(tp->inline_depth());
    return make(meet_ptr(tp->ptr()), meet_offset(tp->offset()), instance_id, speculative, depth);
  }

  case InstPtr:                  // For these, flip the call around to cut down
  case AryPtr:
    return t->xmeet(this);      // Call in reverse direction

  } // End of switch
  return this;                  // Return the double constant
}


//------------------------------xdual------------------------------------------
// Dual of a pure heap pointer.  No relevant klass or oop information.
const Type *TypeOopPtr::xdual() const {
  assert(klass() == Compile::current()->env()->Object_klass(), "no klasses here");
  assert(const_oop() == NULL,             "no constants here");
  return new TypeOopPtr(_base, dual_ptr(), klass(), klass_is_exact(), const_oop(), dual_offset(), dual_instance_id(), dual_speculative(), dual_inline_depth());
}

//--------------------------make_from_klass_common-----------------------------
// Computes the element-type given a klass.
const TypeOopPtr* TypeOopPtr::make_from_klass_common(ciKlass *klass, bool klass_change, bool try_for_exact) {
  if (klass->is_instance_klass()) {
    Compile* C = Compile::current();
    Dependencies* deps = C->dependencies();
    assert((deps != NULL) == (C->method() != NULL && C->method()->code_size() > 0), "sanity");
    // Element is an instance
    bool klass_is_exact = false;
    if (klass->is_loaded()) {
      // Try to set klass_is_exact.
      ciInstanceKlass* ik = klass->as_instance_klass();
      klass_is_exact = ik->is_final();
      if (!klass_is_exact && klass_change
          && deps != NULL && UseUniqueSubclasses) {
        ciInstanceKlass* sub = ik->unique_concrete_subklass();
        if (sub != NULL) {
          deps->assert_abstract_with_unique_concrete_subtype(ik, sub);
          klass = ik = sub;
          klass_is_exact = sub->is_final();
        }
      }
      if (!klass_is_exact && try_for_exact && deps != NULL &&
          !ik->is_interface() && !ik->has_subklass()) {
        // Add a dependence; if concrete subclass added we need to recompile
        deps->assert_leaf_type(ik);
        klass_is_exact = true;
      }
    }
    return TypeInstPtr::make(TypePtr::BotPTR, klass, klass_is_exact, NULL, 0);
  } else if (klass->is_obj_array_klass()) {
    // Element is an object array. Recursively call ourself.
    const TypeOopPtr *etype = TypeOopPtr::make_from_klass_common(klass->as_obj_array_klass()->element_klass(), false, try_for_exact);
    bool xk = etype->klass_is_exact();
    const TypeAry* arr0 = TypeAry::make(etype, TypeInt::POS);
    // We used to pass NotNull in here, asserting that the sub-arrays
    // are all not-null.  This is not true in generally, as code can
    // slam NULLs down in the subarrays.
    const TypeAryPtr* arr = TypeAryPtr::make(TypePtr::BotPTR, arr0, klass, xk, 0);
    return arr;
  } else if (klass->is_type_array_klass()) {
    // Element is an typeArray
    const Type* etype = get_const_basic_type(klass->as_type_array_klass()->element_type());
    const TypeAry* arr0 = TypeAry::make(etype, TypeInt::POS);
    // We used to pass NotNull in here, asserting that the array pointer
    // is not-null. That was not true in general.
    const TypeAryPtr* arr = TypeAryPtr::make(TypePtr::BotPTR, arr0, klass, true, 0);
    return arr;
  } else {
    ShouldNotReachHere();
    return NULL;
  }
}

//------------------------------make_from_constant-----------------------------
// Make a java pointer from an oop constant
const TypeOopPtr* TypeOopPtr::make_from_constant(ciObject* o, bool require_constant) {
  assert(!o->is_null_object(), "null object not yet handled here.");

  const bool make_constant = require_constant || o->should_be_constant();

  ciKlass* klass = o->klass();
  if (klass->is_instance_klass()) {
    // Element is an instance
    if (make_constant) {
      return TypeInstPtr::make(o);
    } else {
      return TypeInstPtr::make(TypePtr::NotNull, klass, true, NULL, 0);
    }
  } else if (klass->is_obj_array_klass()) {
    // Element is an object array. Recursively call ourself.
    const TypeOopPtr *etype =
      TypeOopPtr::make_from_klass_raw(klass->as_obj_array_klass()->element_klass());
    const TypeAry* arr0 = TypeAry::make(etype, TypeInt::make(o->as_array()->length()));
    // We used to pass NotNull in here, asserting that the sub-arrays
    // are all not-null.  This is not true in generally, as code can
    // slam NULLs down in the subarrays.
    if (make_constant) {
      return TypeAryPtr::make(TypePtr::Constant, o, arr0, klass, true, 0);
    } else {
      return TypeAryPtr::make(TypePtr::NotNull, arr0, klass, true, 0);
    }
  } else if (klass->is_type_array_klass()) {
    // Element is an typeArray
    const Type* etype =
      (Type*)get_const_basic_type(klass->as_type_array_klass()->element_type());
    const TypeAry* arr0 = TypeAry::make(etype, TypeInt::make(o->as_array()->length()));
    // We used to pass NotNull in here, asserting that the array pointer
    // is not-null. That was not true in general.
    if (make_constant) {
      return TypeAryPtr::make(TypePtr::Constant, o, arr0, klass, true, 0);
    } else {
      return TypeAryPtr::make(TypePtr::NotNull, arr0, klass, true, 0);
    }
  }

  fatal("unhandled object type");
  return NULL;
}

//------------------------------get_con----------------------------------------
intptr_t TypeOopPtr::get_con() const {
  assert( _ptr == Null || _ptr == Constant, "" );
  assert( _offset >= 0, "" );

  if (_offset != 0) {
    // After being ported to the compiler interface, the compiler no longer
    // directly manipulates the addresses of oops.  Rather, it only has a pointer
    // to a handle at compile time.  This handle is embedded in the generated
    // code and dereferenced at the time the nmethod is made.  Until that time,
    // it is not reasonable to do arithmetic with the addresses of oops (we don't
    // have access to the addresses!).  This does not seem to currently happen,
    // but this assertion here is to help prevent its occurence.
    tty->print_cr("Found oop constant with non-zero offset");
    ShouldNotReachHere();
  }

  return (intptr_t)const_oop()->constant_encoding();
}


//-----------------------------filter------------------------------------------
// Do not allow interface-vs.-noninterface joins to collapse to top.
const Type *TypeOopPtr::filter_helper(const Type *kills, bool include_speculative) const {

  const Type* ft = join_helper(kills, include_speculative);
  const TypeInstPtr* ftip = ft->isa_instptr();
  const TypeInstPtr* ktip = kills->isa_instptr();

  if (ft->empty()) {
    // Check for evil case of 'this' being a class and 'kills' expecting an
    // interface.  This can happen because the bytecodes do not contain
    // enough type info to distinguish a Java-level interface variable
    // from a Java-level object variable.  If we meet 2 classes which
    // both implement interface I, but their meet is at 'j/l/O' which
    // doesn't implement I, we have no way to tell if the result should
    // be 'I' or 'j/l/O'.  Thus we'll pick 'j/l/O'.  If this then flows
    // into a Phi which "knows" it's an Interface type we'll have to
    // uplift the type.
    if (!empty()) {
      if (ktip != NULL && ktip->is_loaded() && ktip->klass()->is_interface()) {
        return kills;           // Uplift to interface
      }
      // Also check for evil cases of 'this' being a class array
      // and 'kills' expecting an array of interfaces.
      Type::get_arrays_base_elements(ft, kills, NULL, &ktip);
      if (ktip != NULL && ktip->is_loaded() && ktip->klass()->is_interface()) {
        return kills;           // Uplift to array of interface
      }
    }

    return Type::TOP;           // Canonical empty value
  }

  // If we have an interface-typed Phi or cast and we narrow to a class type,
  // the join should report back the class.  However, if we have a J/L/Object
  // class-typed Phi and an interface flows in, it's possible that the meet &
  // join report an interface back out.  This isn't possible but happens
  // because the type system doesn't interact well with interfaces.
  if (ftip != NULL && ktip != NULL &&
      ftip->is_loaded() &&  ftip->klass()->is_interface() &&
      ktip->is_loaded() && !ktip->klass()->is_interface()) {
    assert(!ftip->klass_is_exact(), "interface could not be exact");
    return ktip->cast_to_ptr_type(ftip->ptr());
  }

  return ft;
}

//------------------------------eq---------------------------------------------
// Structural equality check for Type representations
bool TypeOopPtr::eq( const Type *t ) const {
  const TypeOopPtr *a = (const TypeOopPtr*)t;
  if (_klass_is_exact != a->_klass_is_exact ||
      _instance_id != a->_instance_id)  return false;
  ciObject* one = const_oop();
  ciObject* two = a->const_oop();
  if (one == NULL || two == NULL) {
    return (one == two) && TypePtr::eq(t);
  } else {
    return one->equals(two) && TypePtr::eq(t);
  }
}

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int TypeOopPtr::hash(void) const {
  return
    java_add(java_add((jint)(const_oop() ? const_oop()->hash() : 0), (jint)_klass_is_exact),
             java_add((jint)_instance_id, (jint)TypePtr::hash()));
}

//------------------------------dump2------------------------------------------
#ifndef PRODUCT
void TypeOopPtr::dump2( Dict &d, uint depth, outputStream *st ) const {
  st->print("oopptr:%s", ptr_msg[_ptr]);
  if( _klass_is_exact ) st->print(":exact");
  if( const_oop() ) st->print(INTPTR_FORMAT, p2i(const_oop()));
  switch( _offset ) {
  case OffsetTop: st->print("+top"); break;
  case OffsetBot: st->print("+any"); break;
  case         0: break;
  default:        st->print("+%d",_offset); break;
  }
  if (_instance_id == InstanceTop)
    st->print(",iid=top");
  else if (_instance_id != InstanceBot)
    st->print(",iid=%d",_instance_id);

  dump_inline_depth(st);
  dump_speculative(st);
}
#endif

//------------------------------singleton--------------------------------------
// TRUE if Type is a singleton type, FALSE otherwise.   Singletons are simple
// constants
bool TypeOopPtr::singleton(void) const {
  // detune optimizer to not generate constant oop + constant offset as a constant!
  // TopPTR, Null, AnyNull, Constant are all singletons
  return (_offset == 0) && !below_centerline(_ptr);
}

//------------------------------add_offset-------------------------------------
const TypePtr *TypeOopPtr::add_offset(intptr_t offset) const {
  return make(_ptr, xadd_offset(offset), _instance_id, add_offset_speculative(offset), _inline_depth);
}

/**
 * Return same type without a speculative part
 */
const Type* TypeOopPtr::remove_speculative() const {
  if (_speculative == NULL) {
    return this;
  }
  assert(_inline_depth == InlineDepthTop || _inline_depth == InlineDepthBottom, "non speculative type shouldn't have inline depth");
  return make(_ptr, _offset, _instance_id, NULL, _inline_depth);
}

/**
 * Return same type but drop speculative part if we know we won't use
 * it
 */
const Type* TypeOopPtr::cleanup_speculative() const {
  // If the klass is exact and the ptr is not null then there's
  // nothing that the speculative type can help us with
  if (klass_is_exact() && !maybe_null()) {
    return remove_speculative();
  }
  return TypePtr::cleanup_speculative();
}

/**
 * Return same type but with a different inline depth (used for speculation)
 *
 * @param depth  depth to meet with
 */
const TypePtr* TypeOopPtr::with_inline_depth(int depth) const {
  if (!UseInlineDepthForSpeculativeTypes) {
    return this;
  }
  return make(_ptr, _offset, _instance_id, _speculative, depth);
}

//------------------------------with_instance_id--------------------------------
const TypePtr* TypeOopPtr::with_instance_id(int instance_id) const {
  assert(_instance_id != -1, "should be known");
  return make(_ptr, _offset, instance_id, _speculative, _inline_depth);
}

//------------------------------meet_instance_id--------------------------------
int TypeOopPtr::meet_instance_id( int instance_id ) const {
  // Either is 'TOP' instance?  Return the other instance!
  if( _instance_id == InstanceTop ) return  instance_id;
  if(  instance_id == InstanceTop ) return _instance_id;
  // If either is different, return 'BOTTOM' instance
  if( _instance_id != instance_id ) return InstanceBot;
  return _instance_id;
}

//------------------------------dual_instance_id--------------------------------
int TypeOopPtr::dual_instance_id( ) const {
  if( _instance_id == InstanceTop ) return InstanceBot; // Map TOP into BOTTOM
  if( _instance_id == InstanceBot ) return InstanceTop; // Map BOTTOM into TOP
  return _instance_id;              // Map everything else into self
}

/**
 * Check whether new profiling would improve speculative type
 *
 * @param   exact_kls    class from profiling
 * @param   inline_depth inlining depth of profile point
 *
 * @return  true if type profile is valuable
 */
bool TypeOopPtr::would_improve_type(ciKlass* exact_kls, int inline_depth) const {
  // no way to improve an already exact type
  if (klass_is_exact()) {
    return false;
  }
  return TypePtr::would_improve_type(exact_kls, inline_depth);
}

//=============================================================================
// Convenience common pre-built types.
const TypeInstPtr *TypeInstPtr::NOTNULL;
const TypeInstPtr *TypeInstPtr::BOTTOM;
const TypeInstPtr *TypeInstPtr::MIRROR;
const TypeInstPtr *TypeInstPtr::MARK;
const TypeInstPtr *TypeInstPtr::KLASS;

//------------------------------TypeInstPtr-------------------------------------
TypeInstPtr::TypeInstPtr(PTR ptr, ciKlass* k, bool xk, ciObject* o, int off,
                         int instance_id, const TypePtr* speculative, int inline_depth)
  : TypeOopPtr(InstPtr, ptr, k, xk, o, off, instance_id, speculative, inline_depth),
    _name(k->name()) {
   assert(k != NULL &&
          (k->is_loaded() || o == NULL),
          "cannot have constants with non-loaded klass");
};

//------------------------------make-------------------------------------------
const TypeInstPtr *TypeInstPtr::make(PTR ptr,
                                     ciKlass* k,
                                     bool xk,
                                     ciObject* o,
                                     int offset,
                                     int instance_id,
                                     const TypePtr* speculative,
                                     int inline_depth) {
  assert( !k->is_loaded() || k->is_instance_klass(), "Must be for instance");
  // Either const_oop() is NULL or else ptr is Constant
  assert( (!o && ptr != Constant) || (o && ptr == Constant),
          "constant pointers must have a value supplied" );
  // Ptr is never Null
  assert( ptr != Null, "NULL pointers are not typed" );

  assert(instance_id <= 0 || xk, "instances are always exactly typed");
  if (ptr == Constant) {
    // Note:  This case includes meta-object constants, such as methods.
    xk = true;
  } else if (k->is_loaded()) {
    ciInstanceKlass* ik = k->as_instance_klass();
    if (!xk && ik->is_final())     xk = true;   // no inexact final klass
    if (xk && ik->is_interface())  xk = false;  // no exact interface
  }

  // Now hash this baby
  TypeInstPtr *result =
    (TypeInstPtr*)(new TypeInstPtr(ptr, k, xk, o ,offset, instance_id, speculative, inline_depth))->hashcons();

  return result;
}

/**
 *  Create constant type for a constant boxed value
 */
const Type* TypeInstPtr::get_const_boxed_value() const {
  assert(is_ptr_to_boxed_value(), "should be called only for boxed value");
  assert((const_oop() != NULL), "should be called only for constant object");
  ciConstant constant = const_oop()->as_instance()->field_value_by_offset(offset());
  BasicType bt = constant.basic_type();
  switch (bt) {
    case T_BOOLEAN:  return TypeInt::make(constant.as_boolean());
    case T_INT:      return TypeInt::make(constant.as_int());
    case T_CHAR:     return TypeInt::make(constant.as_char());
    case T_BYTE:     return TypeInt::make(constant.as_byte());
    case T_SHORT:    return TypeInt::make(constant.as_short());
    case T_FLOAT:    return TypeF::make(constant.as_float());
    case T_DOUBLE:   return TypeD::make(constant.as_double());
    case T_LONG:     return TypeLong::make(constant.as_long());
    default:         break;
  }
  fatal("Invalid boxed value type '%s'", type2name(bt));
  return NULL;
}

//------------------------------cast_to_ptr_type-------------------------------
const Type *TypeInstPtr::cast_to_ptr_type(PTR ptr) const {
  if( ptr == _ptr ) return this;
  // Reconstruct _sig info here since not a problem with later lazy
  // construction, _sig will show up on demand.
  return make(ptr, klass(), klass_is_exact(), const_oop(), _offset, _instance_id, _speculative, _inline_depth);
}


//-----------------------------cast_to_exactness-------------------------------
const Type *TypeInstPtr::cast_to_exactness(bool klass_is_exact) const {
  if( klass_is_exact == _klass_is_exact ) return this;
  if (!_klass->is_loaded())  return this;
  ciInstanceKlass* ik = _klass->as_instance_klass();
  if( (ik->is_final() || _const_oop) )  return this;  // cannot clear xk
  if( ik->is_interface() )              return this;  // cannot set xk
  return make(ptr(), klass(), klass_is_exact, const_oop(), _offset, _instance_id, _speculative, _inline_depth);
}

//-----------------------------cast_to_instance_id----------------------------
const TypeOopPtr *TypeInstPtr::cast_to_instance_id(int instance_id) const {
  if( instance_id == _instance_id ) return this;
  return make(_ptr, klass(), _klass_is_exact, const_oop(), _offset, instance_id, _speculative, _inline_depth);
}

//------------------------------xmeet_unloaded---------------------------------
// Compute the MEET of two InstPtrs when at least one is unloaded.
// Assume classes are different since called after check for same name/class-loader
const TypeInstPtr *TypeInstPtr::xmeet_unloaded(const TypeInstPtr *tinst) const {
    int off = meet_offset(tinst->offset());
    PTR ptr = meet_ptr(tinst->ptr());
    int instance_id = meet_instance_id(tinst->instance_id());
    const TypePtr* speculative = xmeet_speculative(tinst);
    int depth = meet_inline_depth(tinst->inline_depth());

    const TypeInstPtr *loaded    = is_loaded() ? this  : tinst;
    const TypeInstPtr *unloaded  = is_loaded() ? tinst : this;
    if( loaded->klass()->equals(ciEnv::current()->Object_klass()) ) {
      //
      // Meet unloaded class with java/lang/Object
      //
      // Meet
      //          |                     Unloaded Class
      //  Object  |   TOP    |   AnyNull | Constant |   NotNull |  BOTTOM   |
      //  ===================================================================
      //   TOP    | ..........................Unloaded......................|
      //  AnyNull |  U-AN    |................Unloaded......................|
      // Constant | ... O-NN .................................. |   O-BOT   |
      //  NotNull | ... O-NN .................................. |   O-BOT   |
      //  BOTTOM  | ........................Object-BOTTOM ..................|
      //
      assert(loaded->ptr() != TypePtr::Null, "insanity check");
      //
      if(      loaded->ptr() == TypePtr::TopPTR ) { return unloaded; }
      else if (loaded->ptr() == TypePtr::AnyNull) { return TypeInstPtr::make(ptr, unloaded->klass(), false, NULL, off, instance_id, speculative, depth); }
      else if (loaded->ptr() == TypePtr::BotPTR ) { return TypeInstPtr::BOTTOM; }
      else if (loaded->ptr() == TypePtr::Constant || loaded->ptr() == TypePtr::NotNull) {
        if (unloaded->ptr() == TypePtr::BotPTR  ) { return TypeInstPtr::BOTTOM;  }
        else                                      { return TypeInstPtr::NOTNULL; }
      }
      else if( unloaded->ptr() == TypePtr::TopPTR )  { return unloaded; }

      return unloaded->cast_to_ptr_type(TypePtr::AnyNull)->is_instptr();
    }

    // Both are unloaded, not the same class, not Object
    // Or meet unloaded with a different loaded class, not java/lang/Object
    if( ptr != TypePtr::BotPTR ) {
      return TypeInstPtr::NOTNULL;
    }
    return TypeInstPtr::BOTTOM;
}


//------------------------------meet-------------------------------------------
// Compute the MEET of two types.  It returns a new Type object.
const Type *TypeInstPtr::xmeet_helper(const Type *t) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type-rep?

  // Current "this->_base" is Pointer
  switch (t->base()) {          // switch on original type

  case Int:                     // Mixing ints & oops happens when javac
  case Long:                    // reuses local variables
  case FloatTop:
  case FloatCon:
  case FloatBot:
  case DoubleTop:
  case DoubleCon:
  case DoubleBot:
  case NarrowOop:
  case NarrowKlass:
  case Bottom:                  // Ye Olde Default
    return Type::BOTTOM;
  case Top:
    return this;

  default:                      // All else is a mistake
    typerr(t);

  case MetadataPtr:
  case KlassPtr:
  case RawPtr: return TypePtr::BOTTOM;

  case AryPtr: {                // All arrays inherit from Object class
    // Call in reverse direction to avoid duplication
    return t->is_aryptr()->xmeet_helper(this);
  }

  case OopPtr: {                // Meeting to OopPtrs
    // Found a OopPtr type vs self-InstPtr type
    const TypeOopPtr *tp = t->is_oopptr();
    int offset = meet_offset(tp->offset());
    PTR ptr = meet_ptr(tp->ptr());
    switch (tp->ptr()) {
    case TopPTR:
    case AnyNull: {
      int instance_id = meet_instance_id(InstanceTop);
      const TypePtr* speculative = xmeet_speculative(tp);
      int depth = meet_inline_depth(tp->inline_depth());
      return make(ptr, klass(), klass_is_exact(),
                  (ptr == Constant ? const_oop() : NULL), offset, instance_id, speculative, depth);
    }
    case NotNull:
    case BotPTR: {
      int instance_id = meet_instance_id(tp->instance_id());
      const TypePtr* speculative = xmeet_speculative(tp);
      int depth = meet_inline_depth(tp->inline_depth());
      return TypeOopPtr::make(ptr, offset, instance_id, speculative, depth);
    }
    default: typerr(t);
    }
  }

  case AnyPtr: {                // Meeting to AnyPtrs
    // Found an AnyPtr type vs self-InstPtr type
    const TypePtr *tp = t->is_ptr();
    int offset = meet_offset(tp->offset());
    PTR ptr = meet_ptr(tp->ptr());
    int instance_id = meet_instance_id(InstanceTop);
    const TypePtr* speculative = xmeet_speculative(tp);
    int depth = meet_inline_depth(tp->inline_depth());
    switch (tp->ptr()) {
    case Null:
      if( ptr == Null ) return TypePtr::make(AnyPtr, ptr, offset, speculative, depth);
      // else fall through to AnyNull
    case TopPTR:
    case AnyNull: {
      return make(ptr, klass(), klass_is_exact(),
                  (ptr == Constant ? const_oop() : NULL), offset, instance_id, speculative, depth);
    }
    case NotNull:
    case BotPTR:
      return TypePtr::make(AnyPtr, ptr, offset, speculative,depth);
    default: typerr(t);
    }
  }

  /*
                 A-top         }
               /   |   \       }  Tops
           B-top A-any C-top   }
              | /  |  \ |      }  Any-nulls
           B-any   |   C-any   }
              |    |    |
           B-con A-con C-con   } constants; not comparable across classes
              |    |    |
           B-not   |   C-not   }
              | \  |  / |      }  not-nulls
           B-bot A-not C-bot   }
               \   |   /       }  Bottoms
                 A-bot         }
  */

  case InstPtr: {                // Meeting 2 Oops?
    // Found an InstPtr sub-type vs self-InstPtr type
    const TypeInstPtr *tinst = t->is_instptr();
    int off = meet_offset( tinst->offset() );
    PTR ptr = meet_ptr( tinst->ptr() );
    int instance_id = meet_instance_id(tinst->instance_id());
    const TypePtr* speculative = xmeet_speculative(tinst);
    int depth = meet_inline_depth(tinst->inline_depth());

    // Check for easy case; klasses are equal (and perhaps not loaded!)
    // If we have constants, then we created oops so classes are loaded
    // and we can handle the constants further down.  This case handles
    // both-not-loaded or both-loaded classes
    if (ptr != Constant && klass()->equals(tinst->klass()) && klass_is_exact() == tinst->klass_is_exact()) {
      return make(ptr, klass(), klass_is_exact(), NULL, off, instance_id, speculative, depth);
    }

    // Classes require inspection in the Java klass hierarchy.  Must be loaded.
    ciKlass* tinst_klass = tinst->klass();
    ciKlass* this_klass  = this->klass();
    bool tinst_xk = tinst->klass_is_exact();
    bool this_xk  = this->klass_is_exact();
    if (!tinst_klass->is_loaded() || !this_klass->is_loaded() ) {
      // One of these classes has not been loaded
      const TypeInstPtr *unloaded_meet = xmeet_unloaded(tinst);
#ifndef PRODUCT
      if( PrintOpto && Verbose ) {
        tty->print("meet of unloaded classes resulted in: "); unloaded_meet->dump(); tty->cr();
        tty->print("  this == "); this->dump(); tty->cr();
        tty->print(" tinst == "); tinst->dump(); tty->cr();
      }
#endif
      return unloaded_meet;
    }

    // Handle mixing oops and interfaces first.
    if( this_klass->is_interface() && !(tinst_klass->is_interface() ||
                                        tinst_klass == ciEnv::current()->Object_klass())) {
      ciKlass *tmp = tinst_klass; // Swap interface around
      tinst_klass = this_klass;
      this_klass = tmp;
      bool tmp2 = tinst_xk;
      tinst_xk = this_xk;
      this_xk = tmp2;
    }
    if (tinst_klass->is_interface() &&
        !(this_klass->is_interface() ||
          // Treat java/lang/Object as an honorary interface,
          // because we need a bottom for the interface hierarchy.
          this_klass == ciEnv::current()->Object_klass())) {
      // Oop meets interface!

      // See if the oop subtypes (implements) interface.
      ciKlass *k;
      bool xk;
      if( this_klass->is_subtype_of( tinst_klass ) ) {
        // Oop indeed subtypes.  Now keep oop or interface depending
        // on whether we are both above the centerline or either is
        // below the centerline.  If we are on the centerline
        // (e.g., Constant vs. AnyNull interface), use the constant.
        k  = below_centerline(ptr) ? tinst_klass : this_klass;
        // If we are keeping this_klass, keep its exactness too.
        xk = below_centerline(ptr) ? tinst_xk    : this_xk;
      } else {                  // Does not implement, fall to Object
        // Oop does not implement interface, so mixing falls to Object
        // just like the verifier does (if both are above the
        // centerline fall to interface)
        k = above_centerline(ptr) ? tinst_klass : ciEnv::current()->Object_klass();
        xk = above_centerline(ptr) ? tinst_xk : false;
        // Watch out for Constant vs. AnyNull interface.
        if (ptr == Constant) {
          ptr = NotNull;  // forget it was a constant
        }
        if (instance_id > 0) {
          instance_id = InstanceBot;
        }
      }
      ciObject* o = NULL;  // the Constant value, if any
      if (ptr == Constant) {
        // Find out which constant.
        o = (this_klass == klass()) ? const_oop() : tinst->const_oop();
      }
      return make(ptr, k, xk, o, off, instance_id, speculative, depth);
    }

    // Either oop vs oop or interface vs interface or interface vs Object

    // !!! Here's how the symmetry requirement breaks down into invariants:
    // If we split one up & one down AND they subtype, take the down man.
    // If we split one up & one down AND they do NOT subtype, "fall hard".
    // If both are up and they subtype, take the subtype class.
    // If both are up and they do NOT subtype, "fall hard".
    // If both are down and they subtype, take the supertype class.
    // If both are down and they do NOT subtype, "fall hard".
    // Constants treated as down.

    // Now, reorder the above list; observe that both-down+subtype is also
    // "fall hard"; "fall hard" becomes the default case:
    // If we split one up & one down AND they subtype, take the down man.
    // If both are up and they subtype, take the subtype class.

    // If both are down and they subtype, "fall hard".
    // If both are down and they do NOT subtype, "fall hard".
    // If both are up and they do NOT subtype, "fall hard".
    // If we split one up & one down AND they do NOT subtype, "fall hard".

    // If a proper subtype is exact, and we return it, we return it exactly.
    // If a proper supertype is exact, there can be no subtyping relationship!
    // If both types are equal to the subtype, exactness is and-ed below the
    // centerline and or-ed above it.  (N.B. Constants are always exact.)

    // Check for subtyping:
    ciKlass *subtype = NULL;
    bool subtype_exact = false;
    if( tinst_klass->equals(this_klass) ) {
      subtype = this_klass;
      subtype_exact = below_centerline(ptr) ? (this_xk && tinst_xk) : (this_xk || tinst_xk);
    } else if( !tinst_xk && this_klass->is_subtype_of( tinst_klass ) ) {
      subtype = this_klass;     // Pick subtyping class
      subtype_exact = this_xk;
    } else if( !this_xk && tinst_klass->is_subtype_of( this_klass ) ) {
      subtype = tinst_klass;    // Pick subtyping class
      subtype_exact = tinst_xk;
    }

    if( subtype ) {
      if( above_centerline(ptr) ) { // both are up?
        this_klass = tinst_klass = subtype;
        this_xk = tinst_xk = subtype_exact;
      } else if( above_centerline(this ->_ptr) && !above_centerline(tinst->_ptr) ) {
        this_klass = tinst_klass; // tinst is down; keep down man
        this_xk = tinst_xk;
      } else if( above_centerline(tinst->_ptr) && !above_centerline(this ->_ptr) ) {
        tinst_klass = this_klass; // this is down; keep down man
        tinst_xk = this_xk;
      } else {
        this_xk = subtype_exact;  // either they are equal, or we'll do an LCA
      }
    }

    // Check for classes now being equal
    if (tinst_klass->equals(this_klass)) {
      // If the klasses are equal, the constants may still differ.  Fall to
      // NotNull if they do (neither constant is NULL; that is a special case
      // handled elsewhere).
      ciObject* o = NULL;             // Assume not constant when done
      ciObject* this_oop  = const_oop();
      ciObject* tinst_oop = tinst->const_oop();
      if( ptr == Constant ) {
        if (this_oop != NULL && tinst_oop != NULL &&
            this_oop->equals(tinst_oop) )
          o = this_oop;
        else if (above_centerline(this ->_ptr))
          o = tinst_oop;
        else if (above_centerline(tinst ->_ptr))
          o = this_oop;
        else
          ptr = NotNull;
      }
      return make(ptr, this_klass, this_xk, o, off, instance_id, speculative, depth);
    } // Else classes are not equal

    // Since klasses are different, we require a LCA in the Java
    // class hierarchy - which means we have to fall to at least NotNull.
    if (ptr == TopPTR || ptr == AnyNull || ptr == Constant) {
      ptr = NotNull;
    }
    instance_id = InstanceBot;

    // Now we find the LCA of Java classes
    ciKlass* k = this_klass->least_common_ancestor(tinst_klass);
    return make(ptr, k, false, NULL, off, instance_id, speculative, depth);
  } // End of case InstPtr

  } // End of switch
  return this;                  // Return the double constant
}


//------------------------java_mirror_type--------------------------------------
ciType* TypeInstPtr::java_mirror_type() const {
  // must be a singleton type
  if( const_oop() == NULL )  return NULL;

  // must be of type java.lang.Class
  if( klass() != ciEnv::current()->Class_klass() )  return NULL;

  return const_oop()->as_instance()->java_mirror_type();
}


//------------------------------xdual------------------------------------------
// Dual: do NOT dual on klasses.  This means I do NOT understand the Java
// inheritance mechanism.
const Type *TypeInstPtr::xdual() const {
  return new TypeInstPtr(dual_ptr(), klass(), klass_is_exact(), const_oop(), dual_offset(), dual_instance_id(), dual_speculative(), dual_inline_depth());
}

//------------------------------eq---------------------------------------------
// Structural equality check for Type representations
bool TypeInstPtr::eq( const Type *t ) const {
  const TypeInstPtr *p = t->is_instptr();
  return
    klass()->equals(p->klass()) &&
    TypeOopPtr::eq(p);          // Check sub-type stuff
}

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int TypeInstPtr::hash(void) const {
  int hash = java_add((jint)klass()->hash(), (jint)TypeOopPtr::hash());
  return hash;
}

//------------------------------dump2------------------------------------------
// Dump oop Type
#ifndef PRODUCT
void TypeInstPtr::dump2(Dict &d, uint depth, outputStream* st) const {
  // Print the name of the klass.
  klass()->print_name_on(st);

  switch( _ptr ) {
  case Constant:
    if (WizardMode || Verbose) {
      ResourceMark rm;
      stringStream ss;

      st->print(" ");
      const_oop()->print_oop(&ss);
      // 'const_oop->print_oop()' may emit newlines('\n') into ss.
      // suppress newlines from it so -XX:+Verbose -XX:+PrintIdeal dumps one-liner for each node.
      char* buf = ss.as_string(/* c_heap= */false);
      StringUtils::replace_no_expand(buf, "\n", "");
      st->print_raw(buf);
    }
  case BotPTR:
    if (!WizardMode && !Verbose) {
      if( _klass_is_exact ) st->print(":exact");
      break;
    }
  case TopPTR:
  case AnyNull:
  case NotNull:
    st->print(":%s", ptr_msg[_ptr]);
    if( _klass_is_exact ) st->print(":exact");
    break;
  default:
    break;
  }

  if( _offset ) {               // Dump offset, if any
    if( _offset == OffsetBot )      st->print("+any");
    else if( _offset == OffsetTop ) st->print("+unknown");
    else st->print("+%d", _offset);
  }

  st->print(" *");
  if (_instance_id == InstanceTop)
    st->print(",iid=top");
  else if (_instance_id != InstanceBot)
    st->print(",iid=%d",_instance_id);

  dump_inline_depth(st);
  dump_speculative(st);
}
#endif

//------------------------------add_offset-------------------------------------
const TypePtr *TypeInstPtr::add_offset(intptr_t offset) const {
  return make(_ptr, klass(), klass_is_exact(), const_oop(), xadd_offset(offset),
              _instance_id, add_offset_speculative(offset), _inline_depth);
}

const Type *TypeInstPtr::remove_speculative() const {
  if (_speculative == NULL) {
    return this;
  }
  assert(_inline_depth == InlineDepthTop || _inline_depth == InlineDepthBottom, "non speculative type shouldn't have inline depth");
  return make(_ptr, klass(), klass_is_exact(), const_oop(), _offset,
              _instance_id, NULL, _inline_depth);
}

const TypePtr *TypeInstPtr::with_inline_depth(int depth) const {
  if (!UseInlineDepthForSpeculativeTypes) {
    return this;
  }
  return make(_ptr, klass(), klass_is_exact(), const_oop(), _offset, _instance_id, _speculative, depth);
}

const TypePtr *TypeInstPtr::with_instance_id(int instance_id) const {
  assert(is_known_instance(), "should be known");
  return make(_ptr, klass(), klass_is_exact(), const_oop(), _offset, instance_id, _speculative, _inline_depth);
}

//=============================================================================
// Convenience common pre-built types.
const TypeAryPtr *TypeAryPtr::RANGE;
const TypeAryPtr *TypeAryPtr::OOPS;
const TypeAryPtr *TypeAryPtr::NARROWOOPS;
const TypeAryPtr *TypeAryPtr::BYTES;
const TypeAryPtr *TypeAryPtr::SHORTS;
const TypeAryPtr *TypeAryPtr::CHARS;
const TypeAryPtr *TypeAryPtr::INTS;
const TypeAryPtr *TypeAryPtr::LONGS;
const TypeAryPtr *TypeAryPtr::FLOATS;
const TypeAryPtr *TypeAryPtr::DOUBLES;

//------------------------------make-------------------------------------------
const TypeAryPtr *TypeAryPtr::make(PTR ptr, const TypeAry *ary, ciKlass* k, bool xk, int offset,
                                   int instance_id, const TypePtr* speculative, int inline_depth) {
  assert(!(k == NULL && ary->_elem->isa_int()),
         "integral arrays must be pre-equipped with a class");
  if (!xk)  xk = ary->ary_must_be_exact();
  assert(instance_id <= 0 || xk, "instances are always exactly typed");
  return (TypeAryPtr*)(new TypeAryPtr(ptr, NULL, ary, k, xk, offset, instance_id, false, speculative, inline_depth))->hashcons();
}

//------------------------------make-------------------------------------------
const TypeAryPtr *TypeAryPtr::make(PTR ptr, ciObject* o, const TypeAry *ary, ciKlass* k, bool xk, int offset,
                                   int instance_id, const TypePtr* speculative, int inline_depth,
                                   bool is_autobox_cache) {
  assert(!(k == NULL && ary->_elem->isa_int()),
         "integral arrays must be pre-equipped with a class");
  assert( (ptr==Constant && o) || (ptr!=Constant && !o), "" );
  if (!xk)  xk = (o != NULL) || ary->ary_must_be_exact();
  assert(instance_id <= 0 || xk, "instances are always exactly typed");
  return (TypeAryPtr*)(new TypeAryPtr(ptr, o, ary, k, xk, offset, instance_id, is_autobox_cache, speculative, inline_depth))->hashcons();
}

//------------------------------cast_to_ptr_type-------------------------------
const Type *TypeAryPtr::cast_to_ptr_type(PTR ptr) const {
  if( ptr == _ptr ) return this;
  return make(ptr, const_oop(), _ary, klass(), klass_is_exact(), _offset, _instance_id, _speculative, _inline_depth);
}


//-----------------------------cast_to_exactness-------------------------------
const Type *TypeAryPtr::cast_to_exactness(bool klass_is_exact) const {
  if( klass_is_exact == _klass_is_exact ) return this;
  if (_ary->ary_must_be_exact())  return this;  // cannot clear xk
  return make(ptr(), const_oop(), _ary, klass(), klass_is_exact, _offset, _instance_id, _speculative, _inline_depth);
}

//-----------------------------cast_to_instance_id----------------------------
const TypeOopPtr *TypeAryPtr::cast_to_instance_id(int instance_id) const {
  if( instance_id == _instance_id ) return this;
  return make(_ptr, const_oop(), _ary, klass(), _klass_is_exact, _offset, instance_id, _speculative, _inline_depth);
}


//-----------------------------max_array_length-------------------------------
// A wrapper around arrayOopDesc::max_array_length(etype) with some input normalization.
jint TypeAryPtr::max_array_length(BasicType etype) {
  if (!is_java_primitive(etype) && !is_reference_type(etype)) {
    if (etype == T_NARROWOOP) {
      etype = T_OBJECT;
    } else if (etype == T_ILLEGAL) { // bottom[]
      etype = T_BYTE; // will produce conservatively high value
    } else {
      fatal("not an element type: %s", type2name(etype));
    }
  }
  return arrayOopDesc::max_array_length(etype);
}

//-----------------------------narrow_size_type-------------------------------
// Narrow the given size type to the index range for the given array base type.
// Return NULL if the resulting int type becomes empty.
const TypeInt* TypeAryPtr::narrow_size_type(const TypeInt* size) const {
  jint hi = size->_hi;
  jint lo = size->_lo;
  jint min_lo = 0;
  jint max_hi = max_array_length(elem()->basic_type());
  //if (index_not_size)  --max_hi;     // type of a valid array index, FTR
  bool chg = false;
  if (lo < min_lo) {
    lo = min_lo;
    if (size->is_con()) {
      hi = lo;
    }
    chg = true;
  }
  if (hi > max_hi) {
    hi = max_hi;
    if (size->is_con()) {
      lo = hi;
    }
    chg = true;
  }
  // Negative length arrays will produce weird intermediate dead fast-path code
  if (lo > hi)
    return TypeInt::ZERO;
  if (!chg)
    return size;
  return TypeInt::make(lo, hi, Type::WidenMin);
}

//-------------------------------cast_to_size----------------------------------
const TypeAryPtr* TypeAryPtr::cast_to_size(const TypeInt* new_size) const {
  assert(new_size != NULL, "");
  new_size = narrow_size_type(new_size);
  if (new_size == size())  return this;
  const TypeAry* new_ary = TypeAry::make(elem(), new_size, is_stable());
  return make(ptr(), const_oop(), new_ary, klass(), klass_is_exact(), _offset, _instance_id, _speculative, _inline_depth);
}

//------------------------------cast_to_stable---------------------------------
const TypeAryPtr* TypeAryPtr::cast_to_stable(bool stable, int stable_dimension) const {
  if (stable_dimension <= 0 || (stable_dimension == 1 && stable == this->is_stable()))
    return this;

  const Type* elem = this->elem();
  const TypePtr* elem_ptr = elem->make_ptr();

  if (stable_dimension > 1 && elem_ptr != NULL && elem_ptr->isa_aryptr()) {
    // If this is widened from a narrow oop, TypeAry::make will re-narrow it.
    elem = elem_ptr = elem_ptr->is_aryptr()->cast_to_stable(stable, stable_dimension - 1);
  }

  const TypeAry* new_ary = TypeAry::make(elem, size(), stable);

  return make(ptr(), const_oop(), new_ary, klass(), klass_is_exact(), _offset, _instance_id, _speculative, _inline_depth);
}

//-----------------------------stable_dimension--------------------------------
int TypeAryPtr::stable_dimension() const {
  if (!is_stable())  return 0;
  int dim = 1;
  const TypePtr* elem_ptr = elem()->make_ptr();
  if (elem_ptr != NULL && elem_ptr->isa_aryptr())
    dim += elem_ptr->is_aryptr()->stable_dimension();
  return dim;
}

//----------------------cast_to_autobox_cache-----------------------------------
const TypeAryPtr* TypeAryPtr::cast_to_autobox_cache() const {
  if (is_autobox_cache())  return this;
  const TypeOopPtr* etype = elem()->make_oopptr();
  if (etype == NULL)  return this;
  // The pointers in the autobox arrays are always non-null.
  etype = etype->cast_to_ptr_type(TypePtr::NotNull)->is_oopptr();
  const TypeAry* new_ary = TypeAry::make(etype, size(), is_stable());
  return make(ptr(), const_oop(), new_ary, klass(), klass_is_exact(), _offset, _instance_id, _speculative, _inline_depth, /*is_autobox_cache=*/true);
}

//------------------------------eq---------------------------------------------
// Structural equality check for Type representations
bool TypeAryPtr::eq( const Type *t ) const {
  const TypeAryPtr *p = t->is_aryptr();
  return
    _ary == p->_ary &&  // Check array
    TypeOopPtr::eq(p);  // Check sub-parts
}

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int TypeAryPtr::hash(void) const {
  return (intptr_t)_ary + TypeOopPtr::hash();
}

//------------------------------meet-------------------------------------------
// Compute the MEET of two types.  It returns a new Type object.
const Type *TypeAryPtr::xmeet_helper(const Type *t) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type-rep?
  // Current "this->_base" is Pointer
  switch (t->base()) {          // switch on original type

  // Mixing ints & oops happens when javac reuses local variables
  case Int:
  case Long:
  case FloatTop:
  case FloatCon:
  case FloatBot:
  case DoubleTop:
  case DoubleCon:
  case DoubleBot:
  case NarrowOop:
  case NarrowKlass:
  case Bottom:                  // Ye Olde Default
    return Type::BOTTOM;
  case Top:
    return this;

  default:                      // All else is a mistake
    typerr(t);

  case OopPtr: {                // Meeting to OopPtrs
    // Found a OopPtr type vs self-AryPtr type
    const TypeOopPtr *tp = t->is_oopptr();
    int offset = meet_offset(tp->offset());
    PTR ptr = meet_ptr(tp->ptr());
    int depth = meet_inline_depth(tp->inline_depth());
    const TypePtr* speculative = xmeet_speculative(tp);
    switch (tp->ptr()) {
    case TopPTR:
    case AnyNull: {
      int instance_id = meet_instance_id(InstanceTop);
      return make(ptr, (ptr == Constant ? const_oop() : NULL),
                  _ary, _klass, _klass_is_exact, offset, instance_id, speculative, depth);
    }
    case BotPTR:
    case NotNull: {
      int instance_id = meet_instance_id(tp->instance_id());
      return TypeOopPtr::make(ptr, offset, instance_id, speculative, depth);
    }
    default: ShouldNotReachHere();
    }
  }

  case AnyPtr: {                // Meeting two AnyPtrs
    // Found an AnyPtr type vs self-AryPtr type
    const TypePtr *tp = t->is_ptr();
    int offset = meet_offset(tp->offset());
    PTR ptr = meet_ptr(tp->ptr());
    const TypePtr* speculative = xmeet_speculative(tp);
    int depth = meet_inline_depth(tp->inline_depth());
    switch (tp->ptr()) {
    case TopPTR:
      return this;
    case BotPTR:
    case NotNull:
      return TypePtr::make(AnyPtr, ptr, offset, speculative, depth);
    case Null:
      if( ptr == Null ) return TypePtr::make(AnyPtr, ptr, offset, speculative, depth);
      // else fall through to AnyNull
    case AnyNull: {
      int instance_id = meet_instance_id(InstanceTop);
      return make(ptr, (ptr == Constant ? const_oop() : NULL),
                  _ary, _klass, _klass_is_exact, offset, instance_id, speculative, depth);
    }
    default: ShouldNotReachHere();
    }
  }

  case MetadataPtr:
  case KlassPtr:
  case RawPtr: return TypePtr::BOTTOM;

  case AryPtr: {                // Meeting 2 references?
    const TypeAryPtr *tap = t->is_aryptr();
    int off = meet_offset(tap->offset());
    const TypeAry *tary = _ary->meet_speculative(tap->_ary)->is_ary();
    PTR ptr = meet_ptr(tap->ptr());
    int instance_id = meet_instance_id(tap->instance_id());
    const TypePtr* speculative = xmeet_speculative(tap);
    int depth = meet_inline_depth(tap->inline_depth());
    ciKlass* lazy_klass = NULL;
    if (tary->_elem->isa_int()) {
      // Integral array element types have irrelevant lattice relations.
      // It is the klass that determines array layout, not the element type.
      if (_klass == NULL)
        lazy_klass = tap->_klass;
      else if (tap->_klass == NULL || tap->_klass == _klass) {
        lazy_klass = _klass;
      } else {
        // Something like byte[int+] meets char[int+].
        // This must fall to bottom, not (int[-128..65535])[int+].
        instance_id = InstanceBot;
        tary = TypeAry::make(Type::BOTTOM, tary->_size, tary->_stable);
      }
    } else // Non integral arrays.
      // Must fall to bottom if exact klasses in upper lattice
      // are not equal or super klass is exact.
      if ((above_centerline(ptr) || ptr == Constant) && klass() != tap->klass() &&
          // meet with top[] and bottom[] are processed further down:
          tap->_klass != NULL  && this->_klass != NULL   &&
          // both are exact and not equal:
          ((tap->_klass_is_exact && this->_klass_is_exact) ||
           // 'tap'  is exact and super or unrelated:
           (tap->_klass_is_exact && !tap->klass()->is_subtype_of(klass())) ||
           // 'this' is exact and super or unrelated:
           (this->_klass_is_exact && !klass()->is_subtype_of(tap->klass())))) {
      if (above_centerline(ptr) || (tary->_elem->make_ptr() && above_centerline(tary->_elem->make_ptr()->_ptr))) {
        tary = TypeAry::make(Type::BOTTOM, tary->_size, tary->_stable);
      }
      return make(NotNull, NULL, tary, lazy_klass, false, off, InstanceBot, speculative, depth);
    }

    bool xk = false;
    switch (tap->ptr()) {
    case AnyNull:
    case TopPTR:
      // Compute new klass on demand, do not use tap->_klass
      if (below_centerline(this->_ptr)) {
        xk = this->_klass_is_exact;
      } else {
        xk = (tap->_klass_is_exact || this->_klass_is_exact);
      }
      return make(ptr, const_oop(), tary, lazy_klass, xk, off, instance_id, speculative, depth);
    case Constant: {
      ciObject* o = const_oop();
      if( _ptr == Constant ) {
        if( tap->const_oop() != NULL && !o->equals(tap->const_oop()) ) {
          xk = (klass() == tap->klass());
          ptr = NotNull;
          o = NULL;
          instance_id = InstanceBot;
        } else {
          xk = true;
        }
      } else if(above_centerline(_ptr)) {
        o = tap->const_oop();
        xk = true;
      } else {
        // Only precise for identical arrays
        xk = this->_klass_is_exact && (klass() == tap->klass());
      }
      return make(ptr, o, tary, lazy_klass, xk, off, instance_id, speculative, depth);
    }
    case NotNull:
    case BotPTR:
      // Compute new klass on demand, do not use tap->_klass
      if (above_centerline(this->_ptr)) {
        xk = tap->_klass_is_exact;
      } else {
        xk = (tap->_klass_is_exact & this->_klass_is_exact) &&
             (klass() == tap->klass()); // Only precise for identical arrays
      }
      return make(ptr, NULL, tary, lazy_klass, xk, off, instance_id, speculative, depth);
    default: ShouldNotReachHere();
    }
  }

  // All arrays inherit from Object class
  case InstPtr: {
    const TypeInstPtr *tp = t->is_instptr();
    int offset = meet_offset(tp->offset());
    PTR ptr = meet_ptr(tp->ptr());
    int instance_id = meet_instance_id(tp->instance_id());
    const TypePtr* speculative = xmeet_speculative(tp);
    int depth = meet_inline_depth(tp->inline_depth());
    switch (ptr) {
    case TopPTR:
    case AnyNull:                // Fall 'down' to dual of object klass
      // For instances when a subclass meets a superclass we fall
      // below the centerline when the superclass is exact. We need to
      // do the same here.
      if (tp->klass()->equals(ciEnv::current()->Object_klass()) && !tp->klass_is_exact()) {
        return make(ptr, _ary, _klass, _klass_is_exact, offset, instance_id, speculative, depth);
      } else {
        // cannot subclass, so the meet has to fall badly below the centerline
        ptr = NotNull;
        instance_id = InstanceBot;
        return TypeInstPtr::make(ptr, ciEnv::current()->Object_klass(), false, NULL,offset, instance_id, speculative, depth);
      }
    case Constant:
    case NotNull:
    case BotPTR:                // Fall down to object klass
      // LCA is object_klass, but if we subclass from the top we can do better
      if (above_centerline(tp->ptr())) {
        // If 'tp'  is above the centerline and it is Object class
        // then we can subclass in the Java class hierarchy.
        // For instances when a subclass meets a superclass we fall
        // below the centerline when the superclass is exact. We need
        // to do the same here.
        if (tp->klass()->equals(ciEnv::current()->Object_klass()) && !tp->klass_is_exact()) {
          // that is, my array type is a subtype of 'tp' klass
          return make(ptr, (ptr == Constant ? const_oop() : NULL),
                      _ary, _klass, _klass_is_exact, offset, instance_id, speculative, depth);
        }
      }
      // The other case cannot happen, since t cannot be a subtype of an array.
      // The meet falls down to Object class below centerline.
      if (ptr == Constant) {
         ptr = NotNull;
      }
      if (instance_id > 0) {
        instance_id = InstanceBot;
      }
      return TypeInstPtr::make(ptr, ciEnv::current()->Object_klass(), false, NULL, offset, instance_id, speculative, depth);
    default: typerr(t);
    }
  }
  }
  return this;                  // Lint noise
}

//------------------------------xdual------------------------------------------
// Dual: compute field-by-field dual
const Type *TypeAryPtr::xdual() const {
  return new TypeAryPtr(dual_ptr(), _const_oop, _ary->dual()->is_ary(),_klass, _klass_is_exact, dual_offset(), dual_instance_id(), is_autobox_cache(), dual_speculative(), dual_inline_depth());
}

//----------------------interface_vs_oop---------------------------------------
#ifdef ASSERT
bool TypeAryPtr::interface_vs_oop(const Type *t) const {
  const TypeAryPtr* t_aryptr = t->isa_aryptr();
  if (t_aryptr) {
    return _ary->interface_vs_oop(t_aryptr->_ary);
  }
  return false;
}
#endif

//------------------------------dump2------------------------------------------
#ifndef PRODUCT
void TypeAryPtr::dump2( Dict &d, uint depth, outputStream *st ) const {
  _ary->dump2(d,depth,st);
  switch( _ptr ) {
  case Constant:
    const_oop()->print(st);
    break;
  case BotPTR:
    if (!WizardMode && !Verbose) {
      if( _klass_is_exact ) st->print(":exact");
      break;
    }
  case TopPTR:
  case AnyNull:
  case NotNull:
    st->print(":%s", ptr_msg[_ptr]);
    if( _klass_is_exact ) st->print(":exact");
    break;
  default:
    break;
  }

  if( _offset != 0 ) {
    int header_size = objArrayOopDesc::header_size() * wordSize;
    if( _offset == OffsetTop )       st->print("+undefined");
    else if( _offset == OffsetBot )  st->print("+any");
    else if( _offset < header_size ) st->print("+%d", _offset);
    else {
      BasicType basic_elem_type = elem()->basic_type();
      int array_base = arrayOopDesc::base_offset_in_bytes(basic_elem_type);
      int elem_size = type2aelembytes(basic_elem_type);
      st->print("[%d]", (_offset - array_base)/elem_size);
    }
  }
  st->print(" *");
  if (_instance_id == InstanceTop)
    st->print(",iid=top");
  else if (_instance_id != InstanceBot)
    st->print(",iid=%d",_instance_id);

  dump_inline_depth(st);
  dump_speculative(st);
}
#endif

bool TypeAryPtr::empty(void) const {
  if (_ary->empty())       return true;
  return TypeOopPtr::empty();
}

//------------------------------add_offset-------------------------------------
const TypePtr *TypeAryPtr::add_offset(intptr_t offset) const {
  return make(_ptr, _const_oop, _ary, _klass, _klass_is_exact, xadd_offset(offset), _instance_id, add_offset_speculative(offset), _inline_depth);
}

const Type *TypeAryPtr::remove_speculative() const {
  if (_speculative == NULL) {
    return this;
  }
  assert(_inline_depth == InlineDepthTop || _inline_depth == InlineDepthBottom, "non speculative type shouldn't have inline depth");
  return make(_ptr, _const_oop, _ary->remove_speculative()->is_ary(), _klass, _klass_is_exact, _offset, _instance_id, NULL, _inline_depth);
}

const TypePtr *TypeAryPtr::with_inline_depth(int depth) const {
  if (!UseInlineDepthForSpeculativeTypes) {
    return this;
  }
  return make(_ptr, _const_oop, _ary->remove_speculative()->is_ary(), _klass, _klass_is_exact, _offset, _instance_id, _speculative, depth);
}

const TypePtr *TypeAryPtr::with_instance_id(int instance_id) const {
  assert(is_known_instance(), "should be known");
  return make(_ptr, _const_oop, _ary->remove_speculative()->is_ary(), _klass, _klass_is_exact, _offset, instance_id, _speculative, _inline_depth);
}

//=============================================================================

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int TypeNarrowPtr::hash(void) const {
  return _ptrtype->hash() + 7;
}

bool TypeNarrowPtr::singleton(void) const {    // TRUE if type is a singleton
  return _ptrtype->singleton();
}

bool TypeNarrowPtr::empty(void) const {
  return _ptrtype->empty();
}

intptr_t TypeNarrowPtr::get_con() const {
  return _ptrtype->get_con();
}

bool TypeNarrowPtr::eq( const Type *t ) const {
  const TypeNarrowPtr* tc = isa_same_narrowptr(t);
  if (tc != NULL) {
    if (_ptrtype->base() != tc->_ptrtype->base()) {
      return false;
    }
    return tc->_ptrtype->eq(_ptrtype);
  }
  return false;
}

const Type *TypeNarrowPtr::xdual() const {    // Compute dual right now.
  const TypePtr* odual = _ptrtype->dual()->is_ptr();
  return make_same_narrowptr(odual);
}


const Type *TypeNarrowPtr::filter_helper(const Type *kills, bool include_speculative) const {
  if (isa_same_narrowptr(kills)) {
    const Type* ft =_ptrtype->filter_helper(is_same_narrowptr(kills)->_ptrtype, include_speculative);
    if (ft->empty())
      return Type::TOP;           // Canonical empty value
    if (ft->isa_ptr()) {
      return make_hash_same_narrowptr(ft->isa_ptr());
    }
    return ft;
  } else if (kills->isa_ptr()) {
    const Type* ft = _ptrtype->join_helper(kills, include_speculative);
    if (ft->empty())
      return Type::TOP;           // Canonical empty value
    return ft;
  } else {
    return Type::TOP;
  }
}

//------------------------------xmeet------------------------------------------
// Compute the MEET of two types.  It returns a new Type object.
const Type *TypeNarrowPtr::xmeet( const Type *t ) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type-rep?

  if (t->base() == base()) {
    const Type* result = _ptrtype->xmeet(t->make_ptr());
    if (result->isa_ptr()) {
      return make_hash_same_narrowptr(result->is_ptr());
    }
    return result;
  }

  // Current "this->_base" is NarrowKlass or NarrowOop
  switch (t->base()) {          // switch on original type

  case Int:                     // Mixing ints & oops happens when javac
  case Long:                    // reuses local variables
  case FloatTop:
  case FloatCon:
  case FloatBot:
  case DoubleTop:
  case DoubleCon:
  case DoubleBot:
  case AnyPtr:
  case RawPtr:
  case OopPtr:
  case InstPtr:
  case AryPtr:
  case MetadataPtr:
  case KlassPtr:
  case NarrowOop:
  case NarrowKlass:

  case Bottom:                  // Ye Olde Default
    return Type::BOTTOM;
  case Top:
    return this;

  default:                      // All else is a mistake
    typerr(t);

  } // End of switch

  return this;
}

#ifndef PRODUCT
void TypeNarrowPtr::dump2( Dict & d, uint depth, outputStream *st ) const {
  _ptrtype->dump2(d, depth, st);
}
#endif

const TypeNarrowOop *TypeNarrowOop::BOTTOM;
const TypeNarrowOop *TypeNarrowOop::NULL_PTR;


const TypeNarrowOop* TypeNarrowOop::make(const TypePtr* type) {
  return (const TypeNarrowOop*)(new TypeNarrowOop(type))->hashcons();
}

const Type* TypeNarrowOop::remove_speculative() const {
  return make(_ptrtype->remove_speculative()->is_ptr());
}

const Type* TypeNarrowOop::cleanup_speculative() const {
  return make(_ptrtype->cleanup_speculative()->is_ptr());
}

#ifndef PRODUCT
void TypeNarrowOop::dump2( Dict & d, uint depth, outputStream *st ) const {
  st->print("narrowoop: ");
  TypeNarrowPtr::dump2(d, depth, st);
}
#endif

const TypeNarrowKlass *TypeNarrowKlass::NULL_PTR;

const TypeNarrowKlass* TypeNarrowKlass::make(const TypePtr* type) {
  return (const TypeNarrowKlass*)(new TypeNarrowKlass(type))->hashcons();
}

#ifndef PRODUCT
void TypeNarrowKlass::dump2( Dict & d, uint depth, outputStream *st ) const {
  st->print("narrowklass: ");
  TypeNarrowPtr::dump2(d, depth, st);
}
#endif


//------------------------------eq---------------------------------------------
// Structural equality check for Type representations
bool TypeMetadataPtr::eq( const Type *t ) const {
  const TypeMetadataPtr *a = (const TypeMetadataPtr*)t;
  ciMetadata* one = metadata();
  ciMetadata* two = a->metadata();
  if (one == NULL || two == NULL) {
    return (one == two) && TypePtr::eq(t);
  } else {
    return one->equals(two) && TypePtr::eq(t);
  }
}

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int TypeMetadataPtr::hash(void) const {
  return
    (metadata() ? metadata()->hash() : 0) +
    TypePtr::hash();
}

//------------------------------singleton--------------------------------------
// TRUE if Type is a singleton type, FALSE otherwise.   Singletons are simple
// constants
bool TypeMetadataPtr::singleton(void) const {
  // detune optimizer to not generate constant metadata + constant offset as a constant!
  // TopPTR, Null, AnyNull, Constant are all singletons
  return (_offset == 0) && !below_centerline(_ptr);
}

//------------------------------add_offset-------------------------------------
const TypePtr *TypeMetadataPtr::add_offset( intptr_t offset ) const {
  return make( _ptr, _metadata, xadd_offset(offset));
}

//-----------------------------filter------------------------------------------
// Do not allow interface-vs.-noninterface joins to collapse to top.
const Type *TypeMetadataPtr::filter_helper(const Type *kills, bool include_speculative) const {
  const TypeMetadataPtr* ft = join_helper(kills, include_speculative)->isa_metadataptr();
  if (ft == NULL || ft->empty())
    return Type::TOP;           // Canonical empty value
  return ft;
}

 //------------------------------get_con----------------------------------------
intptr_t TypeMetadataPtr::get_con() const {
  assert( _ptr == Null || _ptr == Constant, "" );
  assert( _offset >= 0, "" );

  if (_offset != 0) {
    // After being ported to the compiler interface, the compiler no longer
    // directly manipulates the addresses of oops.  Rather, it only has a pointer
    // to a handle at compile time.  This handle is embedded in the generated
    // code and dereferenced at the time the nmethod is made.  Until that time,
    // it is not reasonable to do arithmetic with the addresses of oops (we don't
    // have access to the addresses!).  This does not seem to currently happen,
    // but this assertion here is to help prevent its occurence.
    tty->print_cr("Found oop constant with non-zero offset");
    ShouldNotReachHere();
  }

  return (intptr_t)metadata()->constant_encoding();
}

//------------------------------cast_to_ptr_type-------------------------------
const Type *TypeMetadataPtr::cast_to_ptr_type(PTR ptr) const {
  if( ptr == _ptr ) return this;
  return make(ptr, metadata(), _offset);
}

//------------------------------meet-------------------------------------------
// Compute the MEET of two types.  It returns a new Type object.
const Type *TypeMetadataPtr::xmeet( const Type *t ) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type-rep?

  // Current "this->_base" is OopPtr
  switch (t->base()) {          // switch on original type

  case Int:                     // Mixing ints & oops happens when javac
  case Long:                    // reuses local variables
  case FloatTop:
  case FloatCon:
  case FloatBot:
  case DoubleTop:
  case DoubleCon:
  case DoubleBot:
  case NarrowOop:
  case NarrowKlass:
  case Bottom:                  // Ye Olde Default
    return Type::BOTTOM;
  case Top:
    return this;

  default:                      // All else is a mistake
    typerr(t);

  case AnyPtr: {
    // Found an AnyPtr type vs self-OopPtr type
    const TypePtr *tp = t->is_ptr();
    int offset = meet_offset(tp->offset());
    PTR ptr = meet_ptr(tp->ptr());
    switch (tp->ptr()) {
    case Null:
      if (ptr == Null)  return TypePtr::make(AnyPtr, ptr, offset, tp->speculative(), tp->inline_depth());
      // else fall through:
    case TopPTR:
    case AnyNull: {
      return make(ptr, _metadata, offset);
    }
    case BotPTR:
    case NotNull:
      return TypePtr::make(AnyPtr, ptr, offset, tp->speculative(), tp->inline_depth());
    default: typerr(t);
    }
  }

  case RawPtr:
  case KlassPtr:
  case OopPtr:
  case InstPtr:
  case AryPtr:
    return TypePtr::BOTTOM;     // Oop meet raw is not well defined

  case MetadataPtr: {
    const TypeMetadataPtr *tp = t->is_metadataptr();
    int offset = meet_offset(tp->offset());
    PTR tptr = tp->ptr();
    PTR ptr = meet_ptr(tptr);
    ciMetadata* md = (tptr == TopPTR) ? metadata() : tp->metadata();
    if (tptr == TopPTR || _ptr == TopPTR ||
        metadata()->equals(tp->metadata())) {
      return make(ptr, md, offset);
    }
    // metadata is different
    if( ptr == Constant ) {  // Cannot be equal constants, so...
      if( tptr == Constant && _ptr != Constant)  return t;
      if( _ptr == Constant && tptr != Constant)  return this;
      ptr = NotNull;            // Fall down in lattice
    }
    return make(ptr, NULL, offset);
    break;
  }
  } // End of switch
  return this;                  // Return the double constant
}


//------------------------------xdual------------------------------------------
// Dual of a pure metadata pointer.
const Type *TypeMetadataPtr::xdual() const {
  return new TypeMetadataPtr(dual_ptr(), metadata(), dual_offset());
}

//------------------------------dump2------------------------------------------
#ifndef PRODUCT
void TypeMetadataPtr::dump2( Dict &d, uint depth, outputStream *st ) const {
  st->print("metadataptr:%s", ptr_msg[_ptr]);
  if( metadata() ) st->print(INTPTR_FORMAT, p2i(metadata()));
  switch( _offset ) {
  case OffsetTop: st->print("+top"); break;
  case OffsetBot: st->print("+any"); break;
  case         0: break;
  default:        st->print("+%d",_offset); break;
  }
}
#endif


//=============================================================================
// Convenience common pre-built type.
const TypeMetadataPtr *TypeMetadataPtr::BOTTOM;

TypeMetadataPtr::TypeMetadataPtr(PTR ptr, ciMetadata* metadata, int offset):
  TypePtr(MetadataPtr, ptr, offset), _metadata(metadata) {
}

const TypeMetadataPtr* TypeMetadataPtr::make(ciMethod* m) {
  return make(Constant, m, 0);
}
const TypeMetadataPtr* TypeMetadataPtr::make(ciMethodData* m) {
  return make(Constant, m, 0);
}

//------------------------------make-------------------------------------------
// Create a meta data constant
const TypeMetadataPtr *TypeMetadataPtr::make(PTR ptr, ciMetadata* m, int offset) {
  assert(m == NULL || !m->is_klass(), "wrong type");
  return (TypeMetadataPtr*)(new TypeMetadataPtr(ptr, m, offset))->hashcons();
}


//=============================================================================
// Convenience common pre-built types.

// Not-null object klass or below
const TypeKlassPtr *TypeKlassPtr::OBJECT;
const TypeKlassPtr *TypeKlassPtr::OBJECT_OR_NULL;

//------------------------------TypeKlassPtr-----------------------------------
TypeKlassPtr::TypeKlassPtr( PTR ptr, ciKlass* klass, int offset )
  : TypePtr(KlassPtr, ptr, offset), _klass(klass), _klass_is_exact(ptr == Constant) {
}

//------------------------------make-------------------------------------------
// ptr to klass 'k', if Constant, or possibly to a sub-klass if not a Constant
const TypeKlassPtr *TypeKlassPtr::make( PTR ptr, ciKlass* k, int offset ) {
  assert( k != NULL, "Expect a non-NULL klass");
  assert(k->is_instance_klass() || k->is_array_klass(), "Incorrect type of klass oop");
  TypeKlassPtr *r =
    (TypeKlassPtr*)(new TypeKlassPtr(ptr, k, offset))->hashcons();

  return r;
}

//------------------------------eq---------------------------------------------
// Structural equality check for Type representations
bool TypeKlassPtr::eq( const Type *t ) const {
  const TypeKlassPtr *p = t->is_klassptr();
  return
    klass()->equals(p->klass()) &&
    TypePtr::eq(p);
}

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int TypeKlassPtr::hash(void) const {
  return java_add((jint)klass()->hash(), (jint)TypePtr::hash());
}

//------------------------------singleton--------------------------------------
// TRUE if Type is a singleton type, FALSE otherwise.   Singletons are simple
// constants
bool TypeKlassPtr::singleton(void) const {
  // detune optimizer to not generate constant klass + constant offset as a constant!
  // TopPTR, Null, AnyNull, Constant are all singletons
  return (_offset == 0) && !below_centerline(_ptr);
}

// Do not allow interface-vs.-noninterface joins to collapse to top.
const Type *TypeKlassPtr::filter_helper(const Type *kills, bool include_speculative) const {
  // logic here mirrors the one from TypeOopPtr::filter. See comments
  // there.
  const Type* ft = join_helper(kills, include_speculative);
  const TypeKlassPtr* ftkp = ft->isa_klassptr();
  const TypeKlassPtr* ktkp = kills->isa_klassptr();

  if (ft->empty()) {
    if (!empty() && ktkp != NULL && ktkp->klass()->is_loaded() && ktkp->klass()->is_interface())
      return kills;             // Uplift to interface

    return Type::TOP;           // Canonical empty value
  }

  // Interface klass type could be exact in opposite to interface type,
  // return it here instead of incorrect Constant ptr J/L/Object (6894807).
  if (ftkp != NULL && ktkp != NULL &&
      ftkp->is_loaded() &&  ftkp->klass()->is_interface() &&
      !ftkp->klass_is_exact() && // Keep exact interface klass
      ktkp->is_loaded() && !ktkp->klass()->is_interface()) {
    return ktkp->cast_to_ptr_type(ftkp->ptr());
  }

  return ft;
}

//----------------------compute_klass------------------------------------------
// Compute the defining klass for this class
ciKlass* TypeAryPtr::compute_klass(DEBUG_ONLY(bool verify)) const {
  // Compute _klass based on element type.
  ciKlass* k_ary = NULL;
  const TypeInstPtr *tinst;
  const TypeAryPtr *tary;
  const Type* el = elem();
  if (el->isa_narrowoop()) {
    el = el->make_ptr();
  }

  // Get element klass
  if ((tinst = el->isa_instptr()) != NULL) {
    // Compute array klass from element klass
    k_ary = ciObjArrayKlass::make(tinst->klass());
  } else if ((tary = el->isa_aryptr()) != NULL) {
    // Compute array klass from element klass
    ciKlass* k_elem = tary->klass();
    // If element type is something like bottom[], k_elem will be null.
    if (k_elem != NULL)
      k_ary = ciObjArrayKlass::make(k_elem);
  } else if ((el->base() == Type::Top) ||
             (el->base() == Type::Bottom)) {
    // element type of Bottom occurs from meet of basic type
    // and object; Top occurs when doing join on Bottom.
    // Leave k_ary at NULL.
  } else {
    // Cannot compute array klass directly from basic type,
    // since subtypes of TypeInt all have basic type T_INT.
#ifdef ASSERT
    if (verify && el->isa_int()) {
      // Check simple cases when verifying klass.
      BasicType bt = T_ILLEGAL;
      if (el == TypeInt::BYTE) {
        bt = T_BYTE;
      } else if (el == TypeInt::SHORT) {
        bt = T_SHORT;
      } else if (el == TypeInt::CHAR) {
        bt = T_CHAR;
      } else if (el == TypeInt::INT) {
        bt = T_INT;
      } else {
        return _klass; // just return specified klass
      }
      return ciTypeArrayKlass::make(bt);
    }
#endif
    assert(!el->isa_int(),
           "integral arrays must be pre-equipped with a class");
    // Compute array klass directly from basic type
    k_ary = ciTypeArrayKlass::make(el->basic_type());
  }
  return k_ary;
}

//------------------------------klass------------------------------------------
// Return the defining klass for this class
ciKlass* TypeAryPtr::klass() const {
  if( _klass ) return _klass;   // Return cached value, if possible

  // Oops, need to compute _klass and cache it
  ciKlass* k_ary = compute_klass();

  if( this != TypeAryPtr::OOPS && this->dual() != TypeAryPtr::OOPS ) {
    // The _klass field acts as a cache of the underlying
    // ciKlass for this array type.  In order to set the field,
    // we need to cast away const-ness.
    //
    // IMPORTANT NOTE: we *never* set the _klass field for the
    // type TypeAryPtr::OOPS.  This Type is shared between all
    // active compilations.  However, the ciKlass which represents
    // this Type is *not* shared between compilations, so caching
    // this value would result in fetching a dangling pointer.
    //
    // Recomputing the underlying ciKlass for each request is
    // a bit less efficient than caching, but calls to
    // TypeAryPtr::OOPS->klass() are not common enough to matter.
    ((TypeAryPtr*)this)->_klass = k_ary;
    if (UseCompressedOops && k_ary != NULL && k_ary->is_obj_array_klass() &&
        _offset != 0 && _offset != arrayOopDesc::length_offset_in_bytes()) {
      ((TypeAryPtr*)this)->_is_ptr_to_narrowoop = true;
    }
  }
  return k_ary;
}


//------------------------------add_offset-------------------------------------
// Access internals of klass object
const TypePtr *TypeKlassPtr::add_offset( intptr_t offset ) const {
  return make( _ptr, klass(), xadd_offset(offset) );
}

//------------------------------cast_to_ptr_type-------------------------------
const Type *TypeKlassPtr::cast_to_ptr_type(PTR ptr) const {
  assert(_base == KlassPtr, "subclass must override cast_to_ptr_type");
  if( ptr == _ptr ) return this;
  return make(ptr, _klass, _offset);
}


//-----------------------------cast_to_exactness-------------------------------
const Type *TypeKlassPtr::cast_to_exactness(bool klass_is_exact) const {
  if( klass_is_exact == _klass_is_exact ) return this;
  return make(klass_is_exact ? Constant : NotNull, _klass, _offset);
}


//-----------------------------as_instance_type--------------------------------
// Corresponding type for an instance of the given class.
// It will be NotNull, and exact if and only if the klass type is exact.
const TypeOopPtr* TypeKlassPtr::as_instance_type() const {
  ciKlass* k = klass();
  bool    xk = klass_is_exact();
  //return TypeInstPtr::make(TypePtr::NotNull, k, xk, NULL, 0);
  const TypeOopPtr* toop = TypeOopPtr::make_from_klass_raw(k);
  guarantee(toop != NULL, "need type for given klass");
  toop = toop->cast_to_ptr_type(TypePtr::NotNull)->is_oopptr();
  return toop->cast_to_exactness(xk)->is_oopptr();
}


//------------------------------xmeet------------------------------------------
// Compute the MEET of two types, return a new Type object.
const Type    *TypeKlassPtr::xmeet( const Type *t ) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type-rep?

  // Current "this->_base" is Pointer
  switch (t->base()) {          // switch on original type

  case Int:                     // Mixing ints & oops happens when javac
  case Long:                    // reuses local variables
  case FloatTop:
  case FloatCon:
  case FloatBot:
  case DoubleTop:
  case DoubleCon:
  case DoubleBot:
  case NarrowOop:
  case NarrowKlass:
  case Bottom:                  // Ye Olde Default
    return Type::BOTTOM;
  case Top:
    return this;

  default:                      // All else is a mistake
    typerr(t);

  case AnyPtr: {                // Meeting to AnyPtrs
    // Found an AnyPtr type vs self-KlassPtr type
    const TypePtr *tp = t->is_ptr();
    int offset = meet_offset(tp->offset());
    PTR ptr = meet_ptr(tp->ptr());
    switch (tp->ptr()) {
    case TopPTR:
      return this;
    case Null:
      if( ptr == Null ) return TypePtr::make(AnyPtr, ptr, offset, tp->speculative(), tp->inline_depth());
    case AnyNull:
      return make( ptr, klass(), offset );
    case BotPTR:
    case NotNull:
      return TypePtr::make(AnyPtr, ptr, offset, tp->speculative(), tp->inline_depth());
    default: typerr(t);
    }
  }

  case RawPtr:
  case MetadataPtr:
  case OopPtr:
  case AryPtr:                  // Meet with AryPtr
  case InstPtr:                 // Meet with InstPtr
    return TypePtr::BOTTOM;

  //
  //             A-top         }
  //           /   |   \       }  Tops
  //       B-top A-any C-top   }
  //          | /  |  \ |      }  Any-nulls
  //       B-any   |   C-any   }
  //          |    |    |
  //       B-con A-con C-con   } constants; not comparable across classes
  //          |    |    |
  //       B-not   |   C-not   }
  //          | \  |  / |      }  not-nulls
  //       B-bot A-not C-bot   }
  //           \   |   /       }  Bottoms
  //             A-bot         }
  //

  case KlassPtr: {  // Meet two KlassPtr types
    const TypeKlassPtr *tkls = t->is_klassptr();
    int  off     = meet_offset(tkls->offset());
    PTR  ptr     = meet_ptr(tkls->ptr());

    // Check for easy case; klasses are equal (and perhaps not loaded!)
    // If we have constants, then we created oops so classes are loaded
    // and we can handle the constants further down.  This case handles
    // not-loaded classes
    if( ptr != Constant && tkls->klass()->equals(klass()) ) {
      return make( ptr, klass(), off );
    }

    // Classes require inspection in the Java klass hierarchy.  Must be loaded.
    ciKlass* tkls_klass = tkls->klass();
    ciKlass* this_klass = this->klass();
    assert( tkls_klass->is_loaded(), "This class should have been loaded.");
    assert( this_klass->is_loaded(), "This class should have been loaded.");

    // If 'this' type is above the centerline and is a superclass of the
    // other, we can treat 'this' as having the same type as the other.
    if ((above_centerline(this->ptr())) &&
        tkls_klass->is_subtype_of(this_klass)) {
      this_klass = tkls_klass;
    }
    // If 'tinst' type is above the centerline and is a superclass of the
    // other, we can treat 'tinst' as having the same type as the other.
    if ((above_centerline(tkls->ptr())) &&
        this_klass->is_subtype_of(tkls_klass)) {
      tkls_klass = this_klass;
    }

    // Check for classes now being equal
    if (tkls_klass->equals(this_klass)) {
      // If the klasses are equal, the constants may still differ.  Fall to
      // NotNull if they do (neither constant is NULL; that is a special case
      // handled elsewhere).
      if( ptr == Constant ) {
        if (this->_ptr == Constant && tkls->_ptr == Constant &&
            this->klass()->equals(tkls->klass()));
        else if (above_centerline(this->ptr()));
        else if (above_centerline(tkls->ptr()));
        else
          ptr = NotNull;
      }
      return make( ptr, this_klass, off );
    } // Else classes are not equal

    // Since klasses are different, we require the LCA in the Java
    // class hierarchy - which means we have to fall to at least NotNull.
    if( ptr == TopPTR || ptr == AnyNull || ptr == Constant )
      ptr = NotNull;
    // Now we find the LCA of Java classes
    ciKlass* k = this_klass->least_common_ancestor(tkls_klass);
    return   make( ptr, k, off );
  } // End of case KlassPtr

  } // End of switch
  return this;                  // Return the double constant
}

//------------------------------xdual------------------------------------------
// Dual: compute field-by-field dual
const Type    *TypeKlassPtr::xdual() const {
  return new TypeKlassPtr( dual_ptr(), klass(), dual_offset() );
}

//------------------------------get_con----------------------------------------
intptr_t TypeKlassPtr::get_con() const {
  assert( _ptr == Null || _ptr == Constant, "" );
  assert( _offset >= 0, "" );

  if (_offset != 0) {
    // After being ported to the compiler interface, the compiler no longer
    // directly manipulates the addresses of oops.  Rather, it only has a pointer
    // to a handle at compile time.  This handle is embedded in the generated
    // code and dereferenced at the time the nmethod is made.  Until that time,
    // it is not reasonable to do arithmetic with the addresses of oops (we don't
    // have access to the addresses!).  This does not seem to currently happen,
    // but this assertion here is to help prevent its occurence.
    tty->print_cr("Found oop constant with non-zero offset");
    ShouldNotReachHere();
  }

  return (intptr_t)klass()->constant_encoding();
}
//------------------------------dump2------------------------------------------
// Dump Klass Type
#ifndef PRODUCT
void TypeKlassPtr::dump2( Dict & d, uint depth, outputStream *st ) const {
  switch( _ptr ) {
  case Constant:
    st->print("precise ");
  case NotNull:
    {
      const char *name = klass()->name()->as_utf8();
      if( name ) {
        st->print("klass %s: " INTPTR_FORMAT, name, p2i(klass()));
      } else {
        ShouldNotReachHere();
      }
    }
  case BotPTR:
    if( !WizardMode && !Verbose && !_klass_is_exact ) break;
  case TopPTR:
  case AnyNull:
    st->print(":%s", ptr_msg[_ptr]);
    if( _klass_is_exact ) st->print(":exact");
    break;
  default:
    break;
  }

  if( _offset ) {               // Dump offset, if any
    if( _offset == OffsetBot )      { st->print("+any"); }
    else if( _offset == OffsetTop ) { st->print("+unknown"); }
    else                            { st->print("+%d", _offset); }
  }

  st->print(" *");
}
#endif



//=============================================================================
// Convenience common pre-built types.

//------------------------------make-------------------------------------------
const TypeFunc *TypeFunc::make( const TypeTuple *domain, const TypeTuple *range ) {
  return (TypeFunc*)(new TypeFunc(domain,range))->hashcons();
}

//------------------------------make-------------------------------------------
const TypeFunc *TypeFunc::make(ciMethod* method) {
  Compile* C = Compile::current();
  const TypeFunc* tf = C->last_tf(method); // check cache
  if (tf != NULL)  return tf;  // The hit rate here is almost 50%.
  const TypeTuple *domain;
  if (method->is_static()) {
    domain = TypeTuple::make_domain(NULL, method->signature());
  } else {
    domain = TypeTuple::make_domain(method->holder(), method->signature());
  }
  const TypeTuple *range  = TypeTuple::make_range(method->signature());
  tf = TypeFunc::make(domain, range);
  C->set_last_tf(method, tf);  // fill cache
  return tf;
}

//------------------------------meet-------------------------------------------
// Compute the MEET of two types.  It returns a new Type object.
const Type *TypeFunc::xmeet( const Type *t ) const {
  // Perform a fast test for common case; meeting the same types together.
  if( this == t ) return this;  // Meeting same type-rep?

  // Current "this->_base" is Func
  switch (t->base()) {          // switch on original type

  case Bottom:                  // Ye Olde Default
    return t;

  default:                      // All else is a mistake
    typerr(t);

  case Top:
    break;
  }
  return this;                  // Return the double constant
}

//------------------------------xdual------------------------------------------
// Dual: compute field-by-field dual
const Type *TypeFunc::xdual() const {
  return this;
}

//------------------------------eq---------------------------------------------
// Structural equality check for Type representations
bool TypeFunc::eq( const Type *t ) const {
  const TypeFunc *a = (const TypeFunc*)t;
  return _domain == a->_domain &&
    _range == a->_range;
}

//------------------------------hash-------------------------------------------
// Type-specific hashing function.
int TypeFunc::hash(void) const {
  return (intptr_t)_domain + (intptr_t)_range;
}

//------------------------------dump2------------------------------------------
// Dump Function Type
#ifndef PRODUCT
void TypeFunc::dump2( Dict &d, uint depth, outputStream *st ) const {
  if( _range->cnt() <= Parms )
    st->print("void");
  else {
    uint i;
    for (i = Parms; i < _range->cnt()-1; i++) {
      _range->field_at(i)->dump2(d,depth,st);
      st->print("/");
    }
    _range->field_at(i)->dump2(d,depth,st);
  }
  st->print(" ");
  st->print("( ");
  if( !depth || d[this] ) {     // Check for recursive dump
    st->print("...)");
    return;
  }
  d.Insert((void*)this,(void*)this);    // Stop recursion
  if (Parms < _domain->cnt())
    _domain->field_at(Parms)->dump2(d,depth-1,st);
  for (uint i = Parms+1; i < _domain->cnt(); i++) {
    st->print(", ");
    _domain->field_at(i)->dump2(d,depth-1,st);
  }
  st->print(" )");
}
#endif

//------------------------------singleton--------------------------------------
// TRUE if Type is a singleton type, FALSE otherwise.   Singletons are simple
// constants (Ldi nodes).  Singletons are integer, float or double constants
// or a single symbol.
bool TypeFunc::singleton(void) const {
  return false;                 // Never a singleton
}

bool TypeFunc::empty(void) const {
  return false;                 // Never empty
}


BasicType TypeFunc::return_type() const{
  if (range()->cnt() == TypeFunc::Parms) {
    return T_VOID;
  }
  return range()->field_at(TypeFunc::Parms)->basic_type();
}
