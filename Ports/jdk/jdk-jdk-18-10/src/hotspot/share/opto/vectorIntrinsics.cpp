/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciSymbols.hpp"
#include "classfile/vmSymbols.hpp"
#include "opto/library_call.hpp"
#include "opto/runtime.hpp"
#include "opto/vectornode.hpp"
#include "prims/vectorSupport.hpp"
#include "runtime/stubRoutines.hpp"

#ifdef ASSERT
static bool is_vector(ciKlass* klass) {
  return klass->is_subclass_of(ciEnv::current()->vector_VectorPayload_klass());
}

static bool check_vbox(const TypeInstPtr* vbox_type) {
  assert(vbox_type->klass_is_exact(), "");

  ciInstanceKlass* ik = vbox_type->klass()->as_instance_klass();
  assert(is_vector(ik), "not a vector");

  ciField* fd1 = ik->get_field_by_name(ciSymbols::ETYPE_name(), ciSymbols::class_signature(), /* is_static */ true);
  assert(fd1 != NULL, "element type info is missing");

  ciConstant val1 = fd1->constant_value();
  BasicType elem_bt = val1.as_object()->as_instance()->java_mirror_type()->basic_type();
  assert(is_java_primitive(elem_bt), "element type info is missing");

  ciField* fd2 = ik->get_field_by_name(ciSymbols::VLENGTH_name(), ciSymbols::int_signature(), /* is_static */ true);
  assert(fd2 != NULL, "vector length info is missing");

  ciConstant val2 = fd2->constant_value();
  assert(val2.as_int() > 0, "vector length info is missing");

  return true;
}
#endif

Node* GraphKit::box_vector(Node* vector, const TypeInstPtr* vbox_type, BasicType elem_bt, int num_elem, bool deoptimize_on_exception) {
  assert(EnableVectorSupport, "");

  PreserveReexecuteState preexecs(this);
  jvms()->set_should_reexecute(true);

  VectorBoxAllocateNode* alloc = new VectorBoxAllocateNode(C, vbox_type);
  set_edges_for_java_call(alloc, /*must_throw=*/false, /*separate_io_proj=*/true);
  make_slow_call_ex(alloc, env()->Throwable_klass(), /*separate_io_proj=*/true, deoptimize_on_exception);
  set_i_o(gvn().transform( new ProjNode(alloc, TypeFunc::I_O) ));
  set_all_memory(gvn().transform( new ProjNode(alloc, TypeFunc::Memory) ));
  Node* ret = gvn().transform(new ProjNode(alloc, TypeFunc::Parms));

  assert(check_vbox(vbox_type), "");
  const TypeVect* vt = TypeVect::make(elem_bt, num_elem);
  VectorBoxNode* vbox = new VectorBoxNode(C, ret, vector, vbox_type, vt);
  return gvn().transform(vbox);
}

Node* GraphKit::unbox_vector(Node* v, const TypeInstPtr* vbox_type, BasicType elem_bt, int num_elem, bool shuffle_to_vector) {
  assert(EnableVectorSupport, "");
  const TypeInstPtr* vbox_type_v = gvn().type(v)->is_instptr();
  if (vbox_type->klass() != vbox_type_v->klass()) {
    return NULL; // arguments don't agree on vector shapes
  }
  if (vbox_type_v->maybe_null()) {
    return NULL; // no nulls are allowed
  }
  assert(check_vbox(vbox_type), "");
  const TypeVect* vt = TypeVect::make(elem_bt, num_elem);
  Node* unbox = gvn().transform(new VectorUnboxNode(C, vt, v, merged_memory(), shuffle_to_vector));
  return unbox;
}

Node* GraphKit::vector_shift_count(Node* cnt, int shift_op, BasicType bt, int num_elem) {
  assert(bt == T_INT || bt == T_LONG || bt == T_SHORT || bt == T_BYTE, "byte, short, long and int are supported");
  juint mask = (type2aelembytes(bt) * BitsPerByte - 1);
  Node* nmask = gvn().transform(ConNode::make(TypeInt::make(mask)));
  Node* mcnt = gvn().transform(new AndINode(cnt, nmask));
  return gvn().transform(VectorNode::shift_count(shift_op, mcnt, num_elem, bt));
}

bool LibraryCallKit::arch_supports_vector(int sopc, int num_elem, BasicType type, VectorMaskUseType mask_use_type, bool has_scalar_args) {
  // Check that the operation is valid.
  if (sopc <= 0) {
#ifndef PRODUCT
    if (C->print_intrinsics()) {
      tty->print_cr("  ** Rejected intrinsification because no valid vector op could be extracted");
    }
#endif
    return false;
  }

  // Check that architecture supports this op-size-type combination.
  if (!Matcher::match_rule_supported_vector(sopc, num_elem, type)) {
#ifndef PRODUCT
    if (C->print_intrinsics()) {
      tty->print_cr("  ** Rejected vector op (%s,%s,%d) because architecture does not support it",
                    NodeClassNames[sopc], type2name(type), num_elem);
    }
#endif
    return false;
  } else {
    assert(Matcher::match_rule_supported(sopc), "must be supported");
  }

  if (num_elem == 1) {
    if (mask_use_type != VecMaskNotUsed) {
#ifndef PRODUCT
      if (C->print_intrinsics()) {
        tty->print_cr("  ** Rejected vector mask op (%s,%s,%d) because architecture does not support it",
                      NodeClassNames[sopc], type2name(type), num_elem);
      }
#endif
      return false;
    }

    if (sopc != 0) {
      if (sopc != Op_LoadVector && sopc != Op_StoreVector) {
#ifndef PRODUCT
        if (C->print_intrinsics()) {
          tty->print_cr("  ** Not a svml call or load/store vector op (%s,%s,%d)",
                        NodeClassNames[sopc], type2name(type), num_elem);
        }
#endif
        return false;
      }
    }
  }

  if (!has_scalar_args && VectorNode::is_vector_shift(sopc) &&
      Matcher::supports_vector_variable_shifts() == false) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** Rejected vector op (%s,%s,%d) because architecture does not support variable vector shifts",
                    NodeClassNames[sopc], type2name(type), num_elem);
    }
    return false;
  }

  // Check whether mask unboxing is supported.
  if (mask_use_type == VecMaskUseAll || mask_use_type == VecMaskUseLoad) {
    if (!Matcher::match_rule_supported_vector(Op_VectorLoadMask, num_elem, type)) {
    #ifndef PRODUCT
      if (C->print_intrinsics()) {
        tty->print_cr("  ** Rejected vector mask loading (%s,%s,%d) because architecture does not support it",
                      NodeClassNames[Op_VectorLoadMask], type2name(type), num_elem);
      }
    #endif
      return false;
    }
  }

  // Check whether mask boxing is supported.
  if (mask_use_type == VecMaskUseAll || mask_use_type == VecMaskUseStore) {
    if (!Matcher::match_rule_supported_vector(Op_VectorStoreMask, num_elem, type)) {
    #ifndef PRODUCT
      if (C->print_intrinsics()) {
        tty->print_cr("Rejected vector mask storing (%s,%s,%d) because architecture does not support it",
                      NodeClassNames[Op_VectorStoreMask], type2name(type), num_elem);
      }
    #endif
      return false;
    }
  }

  return true;
}

static bool is_vector_mask(ciKlass* klass) {
  return klass->is_subclass_of(ciEnv::current()->vector_VectorMask_klass());
}

static bool is_vector_shuffle(ciKlass* klass) {
  return klass->is_subclass_of(ciEnv::current()->vector_VectorShuffle_klass());
}

static bool is_klass_initialized(const TypeInstPtr* vec_klass) {
  if (vec_klass->const_oop() == NULL) {
    return false; // uninitialized or some kind of unsafe access
  }
  assert(vec_klass->const_oop()->as_instance()->java_lang_Class_klass() != NULL, "klass instance expected");
  ciInstanceKlass* klass =  vec_klass->const_oop()->as_instance()->java_lang_Class_klass()->as_instance_klass();
  return klass->is_initialized();
}

// public static
// <VM>
// VM unaryOp(int oprId, Class<? extends VM> vmClass, Class<?> elementType, int length,
//            VM vm,
//            Function<VM, VM> defaultImpl) {
//
// public static
// <VM>
// VM binaryOp(int oprId, Class<? extends VM> vmClass, Class<?> elementType, int length,
//             VM vm1, VM vm2,
//             BiFunction<VM, VM, VM> defaultImpl) {
//
// public static
// <VM>
// VM ternaryOp(int oprId, Class<? extends VM> vmClass, Class<?> elementType, int length,
//              VM vm1, VM vm2, VM vm3,
//              TernaryOperation<VM> defaultImpl) {
//
bool LibraryCallKit::inline_vector_nary_operation(int n) {
  const TypeInt*     opr          = gvn().type(argument(0))->isa_int();
  const TypeInstPtr* vector_klass = gvn().type(argument(1))->isa_instptr();
  const TypeInstPtr* elem_klass   = gvn().type(argument(2))->isa_instptr();
  const TypeInt*     vlen         = gvn().type(argument(3))->isa_int();

  if (opr == NULL || vector_klass == NULL || elem_klass == NULL || vlen == NULL ||
      !opr->is_con() || vector_klass->const_oop() == NULL || elem_klass->const_oop() == NULL || !vlen->is_con()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** missing constant: opr=%s vclass=%s etype=%s vlen=%s",
                    NodeClassNames[argument(0)->Opcode()],
                    NodeClassNames[argument(1)->Opcode()],
                    NodeClassNames[argument(2)->Opcode()],
                    NodeClassNames[argument(3)->Opcode()]);
    }
    return false; // not enough info for intrinsification
  }
  ciType* elem_type = elem_klass->const_oop()->as_instance()->java_mirror_type();
  if (!elem_type->is_primitive_type()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not a primitive bt=%d", elem_type->basic_type());
    }
    return false; // should be primitive type
  }
  if (!is_klass_initialized(vector_klass)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** klass argument not initialized");
    }
    return false;
  }
  BasicType elem_bt = elem_type->basic_type();
  int num_elem = vlen->get_con();
  int opc = VectorSupport::vop2ideal(opr->get_con(), elem_bt);
  int sopc = VectorNode::opcode(opc, elem_bt);
  if ((opc != Op_CallLeafVector) && (sopc == 0)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** operation not supported: opc=%s bt=%s", NodeClassNames[opc], type2name(elem_bt));
    }
    return false; // operation not supported
  }
  if (num_elem == 1) {
    if (opc != Op_CallLeafVector || elem_bt != T_DOUBLE) {
      if (C->print_intrinsics()) {
        tty->print_cr("  ** not a svml call: arity=%d opc=%d vlen=%d etype=%s",
                      n, opc, num_elem, type2name(elem_bt));
      }
      return false;
    }
  }
  ciKlass* vbox_klass = vector_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* vbox_type = TypeInstPtr::make_exact(TypePtr::NotNull, vbox_klass);

  if (opc == Op_CallLeafVector) {
    if (!UseVectorStubs) {
      if (C->print_intrinsics()) {
        tty->print_cr("  ** vector stubs support is disabled");
      }
      return false;
    }
    if (!Matcher::supports_vector_calling_convention()) {
      if (C->print_intrinsics()) {
        tty->print_cr("  ** no vector calling conventions supported");
      }
      return false;
    }
    if (!Matcher::vector_size_supported(elem_bt, num_elem)) {
      if (C->print_intrinsics()) {
        tty->print_cr("  ** vector size (vlen=%d, etype=%s) is not supported",
                      num_elem, type2name(elem_bt));
      }
      return false;
    }
  }

  // TODO When mask usage is supported, VecMaskNotUsed needs to be VecMaskUseLoad.
  if ((sopc != 0) &&
      !arch_supports_vector(sopc, num_elem, elem_bt, is_vector_mask(vbox_klass) ? VecMaskUseAll : VecMaskNotUsed)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=%d opc=%d vlen=%d etype=%s ismask=%d",
                    n, sopc, num_elem, type2name(elem_bt),
                    is_vector_mask(vbox_klass) ? 1 : 0);
    }
    return false; // not supported
  }

  Node* opd1 = NULL; Node* opd2 = NULL; Node* opd3 = NULL;
  switch (n) {
    case 3: {
      opd3 = unbox_vector(argument(6), vbox_type, elem_bt, num_elem);
      if (opd3 == NULL) {
        if (C->print_intrinsics()) {
          tty->print_cr("  ** unbox failed v3=%s",
                        NodeClassNames[argument(6)->Opcode()]);
        }
        return false;
      }
      // fall-through
    }
    case 2: {
      opd2 = unbox_vector(argument(5), vbox_type, elem_bt, num_elem);
      if (opd2 == NULL) {
        if (C->print_intrinsics()) {
          tty->print_cr("  ** unbox failed v2=%s",
                        NodeClassNames[argument(5)->Opcode()]);
        }
        return false;
      }
      // fall-through
    }
    case 1: {
      opd1 = unbox_vector(argument(4), vbox_type, elem_bt, num_elem);
      if (opd1 == NULL) {
        if (C->print_intrinsics()) {
          tty->print_cr("  ** unbox failed v1=%s",
                        NodeClassNames[argument(4)->Opcode()]);
        }
        return false;
      }
      break;
    }
    default: fatal("unsupported arity: %d", n);
  }

  Node* operation = NULL;
  if (opc == Op_CallLeafVector) {
    assert(UseVectorStubs, "sanity");
    operation = gen_call_to_svml(opr->get_con(), elem_bt, num_elem, opd1, opd2);
    if (operation == NULL) {
      if (C->print_intrinsics()) {
        tty->print_cr("  ** svml call failed for %s_%s_%d",
                         (elem_bt == T_FLOAT)?"float":"double",
                         VectorSupport::svmlname[opr->get_con() - VectorSupport::VECTOR_OP_SVML_START],
                         num_elem * type2aelembytes(elem_bt));
      }
      return false;
     }
  } else {
    const TypeVect* vt = TypeVect::make(elem_bt, num_elem);
    switch (n) {
      case 1:
      case 2: {
        operation = gvn().transform(VectorNode::make(sopc, opd1, opd2, vt));
        break;
      }
      case 3: {
        operation = gvn().transform(VectorNode::make(sopc, opd1, opd2, opd3, vt));
        break;
      }
      default: fatal("unsupported arity: %d", n);
    }
  }
  // Wrap it up in VectorBox to keep object type information.
  Node* vbox = box_vector(operation, vbox_type, elem_bt, num_elem);
  set_result(vbox);
  C->set_max_vector_size(MAX2(C->max_vector_size(), (uint)(num_elem * type2aelembytes(elem_bt))));
  return true;
}

// <Sh extends VectorShuffle<E>,  E>
//  Sh ShuffleIota(Class<?> E, Class<?> ShuffleClass, Vector.Species<E> s, int length,
//                  int start, int step, int wrap, ShuffleIotaOperation<Sh, E> defaultImpl)
bool LibraryCallKit::inline_vector_shuffle_iota() {
  const TypeInstPtr* shuffle_klass = gvn().type(argument(1))->isa_instptr();
  const TypeInt*     vlen          = gvn().type(argument(3))->isa_int();
  const TypeInt*     start_val     = gvn().type(argument(4))->isa_int();
  const TypeInt*     step_val      = gvn().type(argument(5))->isa_int();
  const TypeInt*     wrap          = gvn().type(argument(6))->isa_int();

  Node* start = argument(4);
  Node* step  = argument(5);

  if (shuffle_klass == NULL || vlen == NULL || start_val == NULL || step_val == NULL || wrap == NULL) {
    return false; // dead code
  }
  if (!vlen->is_con() || !is_power_of_2(vlen->get_con()) ||
      shuffle_klass->const_oop() == NULL || !wrap->is_con()) {
    return false; // not enough info for intrinsification
  }
  if (!is_klass_initialized(shuffle_klass)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** klass argument not initialized");
    }
    return false;
  }

  int do_wrap = wrap->get_con();
  int num_elem = vlen->get_con();
  BasicType elem_bt = T_BYTE;

  if (!arch_supports_vector(VectorNode::replicate_opcode(elem_bt), num_elem, elem_bt, VecMaskNotUsed)) {
    return false;
  }
  if (!arch_supports_vector(Op_AddVB, num_elem, elem_bt, VecMaskNotUsed)) {
    return false;
  }
  if (!arch_supports_vector(Op_AndV, num_elem, elem_bt, VecMaskNotUsed)) {
    return false;
  }
  if (!arch_supports_vector(Op_VectorLoadConst, num_elem, elem_bt, VecMaskNotUsed)) {
    return false;
  }
  if (!arch_supports_vector(Op_VectorBlend, num_elem, elem_bt, VecMaskUseLoad)) {
    return false;
  }
  if (!arch_supports_vector(Op_VectorMaskCmp, num_elem, elem_bt, VecMaskUseStore)) {
    return false;
  }

  const Type * type_bt = Type::get_const_basic_type(elem_bt);
  const TypeVect * vt  = TypeVect::make(type_bt, num_elem);

  Node* res =  gvn().transform(new VectorLoadConstNode(gvn().makecon(TypeInt::ZERO), vt));

  if(!step_val->is_con() || !is_power_of_2(step_val->get_con())) {
    Node* bcast_step     = gvn().transform(VectorNode::scalar2vector(step, num_elem, type_bt));
    res = gvn().transform(VectorNode::make(Op_MulI, res, bcast_step, num_elem, elem_bt));
  } else if (step_val->get_con() > 1) {
    Node* cnt = gvn().makecon(TypeInt::make(log2i_exact(step_val->get_con())));
    Node* shift_cnt = vector_shift_count(cnt, Op_LShiftI, elem_bt, num_elem);
    res = gvn().transform(VectorNode::make(Op_LShiftVB, res, shift_cnt, vt));
  }

  if (!start_val->is_con() || start_val->get_con() != 0) {
    Node* bcast_start    = gvn().transform(VectorNode::scalar2vector(start, num_elem, type_bt));
    res = gvn().transform(VectorNode::make(Op_AddI, res, bcast_start, num_elem, elem_bt));
  }

  Node * mod_val = gvn().makecon(TypeInt::make(num_elem-1));
  Node * bcast_mod  = gvn().transform(VectorNode::scalar2vector(mod_val, num_elem, type_bt));
  if(do_wrap)  {
    // Wrap the indices greater than lane count.
    res = gvn().transform(VectorNode::make(Op_AndI, res, bcast_mod, num_elem, elem_bt));
  } else {
    ConINode* pred_node = (ConINode*)gvn().makecon(TypeInt::make(1));
    Node * lane_cnt  = gvn().makecon(TypeInt::make(num_elem));
    Node * bcast_lane_cnt = gvn().transform(VectorNode::scalar2vector(lane_cnt, num_elem, type_bt));
    Node* mask = gvn().transform(new VectorMaskCmpNode(BoolTest::ge, bcast_lane_cnt, res, pred_node, vt));

    // Make the indices greater than lane count as -ve values. This matches the java side implementation.
    res = gvn().transform(VectorNode::make(Op_AndI, res, bcast_mod, num_elem, elem_bt));
    Node * biased_val = gvn().transform(VectorNode::make(Op_SubI, res, bcast_lane_cnt, num_elem, elem_bt));
    res = gvn().transform(new VectorBlendNode(biased_val, res, mask));
  }

  ciKlass* sbox_klass = shuffle_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* shuffle_box_type = TypeInstPtr::make_exact(TypePtr::NotNull, sbox_klass);

  // Wrap it up in VectorBox to keep object type information.
  res = box_vector(res, shuffle_box_type, elem_bt, num_elem);
  set_result(res);
  C->set_max_vector_size(MAX2(C->max_vector_size(), (uint)(num_elem * type2aelembytes(elem_bt))));
  return true;
}

// <E, M>
// int maskReductionCoerced(int oper, Class<? extends M> maskClass, Class<?> elemClass,
//                          int length, M m, VectorMaskOp<M> defaultImpl)
bool LibraryCallKit::inline_vector_mask_operation() {
  const TypeInt*     oper       = gvn().type(argument(0))->isa_int();
  const TypeInstPtr* mask_klass = gvn().type(argument(1))->isa_instptr();
  const TypeInstPtr* elem_klass = gvn().type(argument(2))->isa_instptr();
  const TypeInt*     vlen       = gvn().type(argument(3))->isa_int();
  Node*              mask       = argument(4);

  if (mask_klass == NULL || elem_klass == NULL || mask->is_top() || vlen == NULL) {
    return false; // dead code
  }

  if (!is_klass_initialized(mask_klass)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** klass argument not initialized");
    }
    return false;
  }

  int num_elem = vlen->get_con();
  ciType* elem_type = elem_klass->const_oop()->as_instance()->java_mirror_type();
  BasicType elem_bt = elem_type->basic_type();

  if (!arch_supports_vector(Op_LoadVector, num_elem, T_BOOLEAN, VecMaskNotUsed)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=1 op=cast#%d/3 vlen2=%d etype2=%s",
                    Op_LoadVector, num_elem, type2name(T_BOOLEAN));
    }
    return false; // not supported
  }

  int mopc = VectorSupport::vop2ideal(oper->get_con(), elem_bt);
  if (!arch_supports_vector(mopc, num_elem, elem_bt, VecMaskNotUsed)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=1 op=cast#%d/3 vlen2=%d etype2=%s",
                    mopc, num_elem, type2name(elem_bt));
    }
    return false; // not supported
  }

  const Type* elem_ty = Type::get_const_basic_type(elem_bt);
  ciKlass* mbox_klass = mask_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* mask_box_type = TypeInstPtr::make_exact(TypePtr::NotNull, mbox_klass);
  Node* mask_vec = unbox_vector(mask, mask_box_type, elem_bt, num_elem, true);
  Node* store_mask = gvn().transform(VectorStoreMaskNode::make(gvn(), mask_vec, elem_bt, num_elem));
  Node* maskoper = gvn().transform(VectorMaskOpNode::make(store_mask, TypeInt::INT, mopc));
  set_result(maskoper);

  C->set_max_vector_size(MAX2(C->max_vector_size(), (uint)(num_elem * type2aelembytes(elem_bt))));
  return true;
}

// <VM ,Sh extends VectorShuffle<E>, E>
// VM shuffleToVector(Class<VM> VecClass, Class<?>E , Class<?> ShuffleClass, Sh s, int length,
//                    ShuffleToVectorOperation<VM,Sh,E> defaultImpl)
bool LibraryCallKit::inline_vector_shuffle_to_vector() {
  const TypeInstPtr* vector_klass  = gvn().type(argument(0))->isa_instptr();
  const TypeInstPtr* elem_klass    = gvn().type(argument(1))->isa_instptr();
  const TypeInstPtr* shuffle_klass = gvn().type(argument(2))->isa_instptr();
  Node*              shuffle       = argument(3);
  const TypeInt*     vlen          = gvn().type(argument(4))->isa_int();

  if (vector_klass == NULL || elem_klass == NULL || shuffle_klass == NULL || shuffle->is_top() || vlen == NULL) {
    return false; // dead code
  }
  if (!vlen->is_con() || vector_klass->const_oop() == NULL || shuffle_klass->const_oop() == NULL) {
    return false; // not enough info for intrinsification
  }
  if (!is_klass_initialized(shuffle_klass) || !is_klass_initialized(vector_klass) ) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** klass argument not initialized");
    }
    return false;
  }

  int num_elem = vlen->get_con();
  ciType* elem_type = elem_klass->const_oop()->as_instance()->java_mirror_type();
  BasicType elem_bt = elem_type->basic_type();

  if (num_elem < 4) {
    return false;
  }

  int cast_vopc = VectorCastNode::opcode(T_BYTE); // from shuffle of type T_BYTE
  // Make sure that cast is implemented to particular type/size combination.
  if (!arch_supports_vector(cast_vopc, num_elem, elem_bt, VecMaskNotUsed)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=1 op=cast#%d/3 vlen2=%d etype2=%s",
        cast_vopc, num_elem, type2name(elem_bt));
    }
    return false;
  }

  ciKlass* sbox_klass = shuffle_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* shuffle_box_type = TypeInstPtr::make_exact(TypePtr::NotNull, sbox_klass);

  // Unbox shuffle with true flag to indicate its load shuffle to vector
  // shuffle is a byte array
  Node* shuffle_vec = unbox_vector(shuffle, shuffle_box_type, T_BYTE, num_elem, true);

  // cast byte to target element type
  shuffle_vec = gvn().transform(VectorCastNode::make(cast_vopc, shuffle_vec, elem_bt, num_elem));

  ciKlass* vbox_klass = vector_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* vec_box_type = TypeInstPtr::make_exact(TypePtr::NotNull, vbox_klass);

  // Box vector
  Node* res = box_vector(shuffle_vec, vec_box_type, elem_bt, num_elem);
  set_result(res);
  C->set_max_vector_size(MAX2(C->max_vector_size(), (uint)(num_elem * type2aelembytes(elem_bt))));
  return true;
}

// <V extends Vector<?,?>>
// V broadcastCoerced(Class<?> vectorClass, Class<?> elementType, int vlen,
//                    long bits,
//                    LongFunction<V> defaultImpl)
bool LibraryCallKit::inline_vector_broadcast_coerced() {
  const TypeInstPtr* vector_klass = gvn().type(argument(0))->isa_instptr();
  const TypeInstPtr* elem_klass   = gvn().type(argument(1))->isa_instptr();
  const TypeInt*     vlen         = gvn().type(argument(2))->isa_int();

  if (vector_klass == NULL || elem_klass == NULL || vlen == NULL ||
      vector_klass->const_oop() == NULL || elem_klass->const_oop() == NULL || !vlen->is_con()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** missing constant: vclass=%s etype=%s vlen=%s",
                    NodeClassNames[argument(0)->Opcode()],
                    NodeClassNames[argument(1)->Opcode()],
                    NodeClassNames[argument(2)->Opcode()]);
    }
    return false; // not enough info for intrinsification
  }

  if (!is_klass_initialized(vector_klass)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** klass argument not initialized");
    }
    return false;
  }
  ciType* elem_type = elem_klass->const_oop()->as_instance()->java_mirror_type();
  if (!elem_type->is_primitive_type()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not a primitive bt=%d", elem_type->basic_type());
    }
    return false; // should be primitive type
  }
  BasicType elem_bt = elem_type->basic_type();
  int num_elem = vlen->get_con();
  ciKlass* vbox_klass = vector_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* vbox_type = TypeInstPtr::make_exact(TypePtr::NotNull, vbox_klass);

  // TODO When mask usage is supported, VecMaskNotUsed needs to be VecMaskUseLoad.
  if (!arch_supports_vector(VectorNode::replicate_opcode(elem_bt), num_elem, elem_bt,
                            (is_vector_mask(vbox_klass) ? VecMaskUseStore : VecMaskNotUsed), true /*has_scalar_args*/)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=0 op=broadcast vlen=%d etype=%s ismask=%d",
                    num_elem, type2name(elem_bt),
                    is_vector_mask(vbox_klass) ? 1 : 0);
    }
    return false; // not supported
  }

  Node* bits = argument(3); // long

  Node* elem = NULL;
  switch (elem_bt) {
    case T_BOOLEAN: // fall-through
    case T_BYTE:    // fall-through
    case T_SHORT:   // fall-through
    case T_CHAR:    // fall-through
    case T_INT: {
      elem = gvn().transform(new ConvL2INode(bits));
      break;
    }
    case T_DOUBLE: {
      elem = gvn().transform(new MoveL2DNode(bits));
      break;
    }
    case T_FLOAT: {
      bits = gvn().transform(new ConvL2INode(bits));
      elem = gvn().transform(new MoveI2FNode(bits));
      break;
    }
    case T_LONG: {
      elem = bits; // no conversion needed
      break;
    }
    default: fatal("%s", type2name(elem_bt));
  }

  Node* broadcast = VectorNode::scalar2vector(elem, num_elem, Type::get_const_basic_type(elem_bt));
  broadcast = gvn().transform(broadcast);

  Node* box = box_vector(broadcast, vbox_type, elem_bt, num_elem);
  set_result(box);
  C->set_max_vector_size(MAX2(C->max_vector_size(), (uint)(num_elem * type2aelembytes(elem_bt))));
  return true;
}

static bool elem_consistent_with_arr(BasicType elem_bt, const TypeAryPtr* arr_type) {
  assert(arr_type != NULL, "unexpected");
  BasicType arr_elem_bt = arr_type->elem()->array_element_basic_type();
  if (elem_bt == arr_elem_bt) {
    return true;
  } else if (elem_bt == T_SHORT && arr_elem_bt == T_CHAR) {
    // Load/store of short vector from/to char[] is supported
    return true;
  } else if (elem_bt == T_BYTE && arr_elem_bt == T_BOOLEAN) {
    // Load/store of byte vector from/to boolean[] is supported
    return true;
  } else {
    return false;
  }
}

//    <C, V extends Vector<?,?>>
//    V load(Class<?> vectorClass, Class<?> elementType, int vlen,
//           Object base, long offset,
//           /* Vector.Mask<E,S> m*/
//           Object container, int index,
//           LoadOperation<C, VM> defaultImpl) {
//
//    <C, V extends Vector<?,?>>
//    void store(Class<?> vectorClass, Class<?> elementType, int vlen,
//               Object base, long offset,
//               V v, /*Vector.Mask<E,S> m*/
//               Object container, int index,
//               StoreVectorOperation<C, V> defaultImpl) {

bool LibraryCallKit::inline_vector_mem_operation(bool is_store) {
  const TypeInstPtr* vector_klass = gvn().type(argument(0))->isa_instptr();
  const TypeInstPtr* elem_klass   = gvn().type(argument(1))->isa_instptr();
  const TypeInt*     vlen         = gvn().type(argument(2))->isa_int();

  if (vector_klass == NULL || elem_klass == NULL || vlen == NULL ||
      vector_klass->const_oop() == NULL || elem_klass->const_oop() == NULL || !vlen->is_con()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** missing constant: vclass=%s etype=%s vlen=%s",
                    NodeClassNames[argument(0)->Opcode()],
                    NodeClassNames[argument(1)->Opcode()],
                    NodeClassNames[argument(2)->Opcode()]);
    }
    return false; // not enough info for intrinsification
  }
  if (!is_klass_initialized(vector_klass)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** klass argument not initialized");
    }
    return false;
  }

  ciType* elem_type = elem_klass->const_oop()->as_instance()->java_mirror_type();
  if (!elem_type->is_primitive_type()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not a primitive bt=%d", elem_type->basic_type());
    }
    return false; // should be primitive type
  }
  BasicType elem_bt = elem_type->basic_type();
  int num_elem = vlen->get_con();

  // TODO When mask usage is supported, VecMaskNotUsed needs to be VecMaskUseLoad.
  if (!arch_supports_vector(is_store ? Op_StoreVector : Op_LoadVector, num_elem, elem_bt, VecMaskNotUsed)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=%d op=%s vlen=%d etype=%s ismask=no",
                    is_store, is_store ? "store" : "load",
                    num_elem, type2name(elem_bt));
    }
    return false; // not supported
  }

  ciKlass* vbox_klass = vector_klass->const_oop()->as_instance()->java_lang_Class_klass();
  bool is_mask = is_vector_mask(vbox_klass);

  Node* base = argument(3);
  Node* offset = ConvL2X(argument(4));

  // Save state and restore on bailout
  uint old_sp = sp();
  SafePointNode* old_map = clone_map();

  Node* addr = make_unsafe_address(base, offset, (is_mask ? T_BOOLEAN : elem_bt), true);
  // Can base be NULL? Otherwise, always on-heap access.
  bool can_access_non_heap = TypePtr::NULL_PTR->higher_equal(gvn().type(base));

  const TypePtr *addr_type = gvn().type(addr)->isa_ptr();
  const TypeAryPtr* arr_type = addr_type->isa_aryptr();

  // Now handle special case where load/store happens from/to byte array but element type is not byte.
  bool using_byte_array = arr_type != NULL && arr_type->elem()->array_element_basic_type() == T_BYTE && elem_bt != T_BYTE;
  // Handle loading masks.
  // If there is no consistency between array and vector element types, it must be special byte array case or loading masks
  if (arr_type != NULL && !using_byte_array && !is_mask && !elem_consistent_with_arr(elem_bt, arr_type)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=%d op=%s vlen=%d etype=%s atype=%s ismask=no",
                    is_store, is_store ? "store" : "load",
                    num_elem, type2name(elem_bt), type2name(arr_type->elem()->array_element_basic_type()));
    }
    set_map(old_map);
    set_sp(old_sp);
    return false;
  }
  // Since we are using byte array, we need to double check that the byte operations are supported by backend.
  if (using_byte_array) {
    int byte_num_elem = num_elem * type2aelembytes(elem_bt);
    if (!arch_supports_vector(is_store ? Op_StoreVector : Op_LoadVector, byte_num_elem, T_BYTE, VecMaskNotUsed)
        || !arch_supports_vector(Op_VectorReinterpret, byte_num_elem, T_BYTE, VecMaskNotUsed)) {
      if (C->print_intrinsics()) {
        tty->print_cr("  ** not supported: arity=%d op=%s vlen=%d*8 etype=%s/8 ismask=no",
                      is_store, is_store ? "store" : "load",
                      byte_num_elem, type2name(elem_bt));
      }
      set_map(old_map);
      set_sp(old_sp);
      return false; // not supported
    }
  }
  if (is_mask) {
    if (!arch_supports_vector(Op_LoadVector, num_elem, T_BOOLEAN, VecMaskNotUsed)) {
      if (C->print_intrinsics()) {
        tty->print_cr("  ** not supported: arity=%d op=%s/mask vlen=%d etype=bit ismask=no",
                      is_store, is_store ? "store" : "load",
                      num_elem);
      }
      set_map(old_map);
      set_sp(old_sp);
      return false; // not supported
    }
    if (!is_store) {
      if (!arch_supports_vector(Op_LoadVector, num_elem, elem_bt, VecMaskUseLoad)) {
        set_map(old_map);
        set_sp(old_sp);
        return false; // not supported
      }
    } else {
      if (!arch_supports_vector(Op_StoreVector, num_elem, elem_bt, VecMaskUseStore)) {
        set_map(old_map);
        set_sp(old_sp);
        return false; // not supported
      }
    }
  }

  const TypeInstPtr* vbox_type = TypeInstPtr::make_exact(TypePtr::NotNull, vbox_klass);

  if (can_access_non_heap) {
    insert_mem_bar(Op_MemBarCPUOrder);
  }

  if (is_store) {
    Node* val = unbox_vector(argument(6), vbox_type, elem_bt, num_elem);
    if (val == NULL) {
      set_map(old_map);
      set_sp(old_sp);
      return false; // operand unboxing failed
    }
    set_all_memory(reset_memory());

    // In case the store needs to happen to byte array, reinterpret the incoming vector to byte vector.
    int store_num_elem = num_elem;
    if (using_byte_array) {
      store_num_elem = num_elem * type2aelembytes(elem_bt);
      const TypeVect* to_vect_type = TypeVect::make(T_BYTE, store_num_elem);
      val = gvn().transform(new VectorReinterpretNode(val, val->bottom_type()->is_vect(), to_vect_type));
    }

    Node* vstore = gvn().transform(StoreVectorNode::make(0, control(), memory(addr), addr, addr_type, val, store_num_elem));
    set_memory(vstore, addr_type);
  } else {
    // When using byte array, we need to load as byte then reinterpret the value. Otherwise, do a simple vector load.
    Node* vload = NULL;
    if (using_byte_array) {
      int load_num_elem = num_elem * type2aelembytes(elem_bt);
      vload = gvn().transform(LoadVectorNode::make(0, control(), memory(addr), addr, addr_type, load_num_elem, T_BYTE));
      const TypeVect* to_vect_type = TypeVect::make(elem_bt, num_elem);
      vload = gvn().transform(new VectorReinterpretNode(vload, vload->bottom_type()->is_vect(), to_vect_type));
    } else {
      // Special handle for masks
      if (is_mask) {
        vload = gvn().transform(LoadVectorNode::make(0, control(), memory(addr), addr, addr_type, num_elem, T_BOOLEAN));
        const TypeVect* to_vect_type = TypeVect::make(elem_bt, num_elem);
        vload = gvn().transform(new VectorLoadMaskNode(vload, to_vect_type));
      } else {
        vload = gvn().transform(LoadVectorNode::make(0, control(), memory(addr), addr, addr_type, num_elem, elem_bt));
      }
    }
    Node* box = box_vector(vload, vbox_type, elem_bt, num_elem);
    set_result(box);
  }

  old_map->destruct(&_gvn);

  if (can_access_non_heap) {
    insert_mem_bar(Op_MemBarCPUOrder);
  }

  C->set_max_vector_size(MAX2(C->max_vector_size(), (uint)(num_elem * type2aelembytes(elem_bt))));
  return true;
}

//   <C, V extends Vector<?>, W extends IntVector, E, S extends VectorSpecies<E>>
//   void loadWithMap(Class<?> vectorClass, Class<E> E, int length, Class<?> vectorIndexClass,
//                    Object base, long offset, // Unsafe addressing
//                    W index_vector,
//                    C container, int index, int[] indexMap, int indexM, S s, // Arguments for default implementation
//                    LoadVectorOperationWithMap<C, V, E, S> defaultImpl)
//
//    <C, V extends Vector<?>, W extends IntVector>
//    void storeWithMap(Class<?> vectorClass, Class<?> elementType, int length, Class<?> vectorIndexClass,
//                      Object base, long offset,    // Unsafe addressing
//                      W index_vector, V v,
//                      C container, int index, int[] indexMap, int indexM, // Arguments for default implementation
//                      StoreVectorOperationWithMap<C, V> defaultImpl) {
//
bool LibraryCallKit::inline_vector_gather_scatter(bool is_scatter) {
  const TypeInstPtr* vector_klass     = gvn().type(argument(0))->isa_instptr();
  const TypeInstPtr* elem_klass       = gvn().type(argument(1))->isa_instptr();
  const TypeInt*     vlen             = gvn().type(argument(2))->isa_int();
  const TypeInstPtr* vector_idx_klass = gvn().type(argument(3))->isa_instptr();

  if (vector_klass == NULL || elem_klass == NULL || vector_idx_klass == NULL || vlen == NULL ||
      vector_klass->const_oop() == NULL || elem_klass->const_oop() == NULL || vector_idx_klass->const_oop() == NULL || !vlen->is_con()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** missing constant: vclass=%s etype=%s vlen=%s viclass=%s",
                    NodeClassNames[argument(0)->Opcode()],
                    NodeClassNames[argument(1)->Opcode()],
                    NodeClassNames[argument(2)->Opcode()],
                    NodeClassNames[argument(3)->Opcode()]);
    }
    return false; // not enough info for intrinsification
  }

  if (!is_klass_initialized(vector_klass) || !is_klass_initialized(vector_idx_klass)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** klass argument not initialized");
    }
    return false;
  }
  ciType* elem_type = elem_klass->const_oop()->as_instance()->java_mirror_type();
  if (!elem_type->is_primitive_type()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not a primitive bt=%d", elem_type->basic_type());
    }
    return false; // should be primitive type
  }
  BasicType elem_bt = elem_type->basic_type();
  int num_elem = vlen->get_con();

  if (!arch_supports_vector(is_scatter ? Op_StoreVectorScatter : Op_LoadVectorGather, num_elem, elem_bt, VecMaskNotUsed)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=%d op=%s vlen=%d etype=%s ismask=no",
                    is_scatter, is_scatter ? "scatter" : "gather",
                    num_elem, type2name(elem_bt));
    }
    return false; // not supported
  }

  // Check that the vector holding indices is supported by architecture
  if (!arch_supports_vector(Op_LoadVector, num_elem, T_INT, VecMaskNotUsed)) {
      if (C->print_intrinsics()) {
        tty->print_cr("  ** not supported: arity=%d op=%s/loadindex vlen=%d etype=int ismask=no",
                      is_scatter, is_scatter ? "scatter" : "gather",
                      num_elem);
      }
      return false; // not supported
    }

  Node* base = argument(4);
  Node* offset = ConvL2X(argument(5));

  // Save state and restore on bailout
  uint old_sp = sp();
  SafePointNode* old_map = clone_map();

  Node* addr = make_unsafe_address(base, offset, elem_bt, true);

  const TypePtr *addr_type = gvn().type(addr)->isa_ptr();
  const TypeAryPtr* arr_type = addr_type->isa_aryptr();

  // The array must be consistent with vector type
  if (arr_type == NULL || (arr_type != NULL && !elem_consistent_with_arr(elem_bt, arr_type))) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=%d op=%s vlen=%d etype=%s atype=%s ismask=no",
                    is_scatter, is_scatter ? "scatter" : "gather",
                    num_elem, type2name(elem_bt), type2name(arr_type->elem()->array_element_basic_type()));
    }
    set_map(old_map);
    set_sp(old_sp);
    return false;
  }
  ciKlass* vbox_klass = vector_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* vbox_type = TypeInstPtr::make_exact(TypePtr::NotNull, vbox_klass);

  ciKlass* vbox_idx_klass = vector_idx_klass->const_oop()->as_instance()->java_lang_Class_klass();

  if (vbox_idx_klass == NULL) {
    set_map(old_map);
    set_sp(old_sp);
    return false;
  }

  const TypeInstPtr* vbox_idx_type = TypeInstPtr::make_exact(TypePtr::NotNull, vbox_idx_klass);

  Node* index_vect = unbox_vector(argument(7), vbox_idx_type, T_INT, num_elem);
  if (index_vect == NULL) {
    set_map(old_map);
    set_sp(old_sp);
    return false;
  }
  const TypeVect* vector_type = TypeVect::make(elem_bt, num_elem);
  if (is_scatter) {
    Node* val = unbox_vector(argument(8), vbox_type, elem_bt, num_elem);
    if (val == NULL) {
      set_map(old_map);
      set_sp(old_sp);
      return false; // operand unboxing failed
    }
    set_all_memory(reset_memory());

    Node* vstore = gvn().transform(new StoreVectorScatterNode(control(), memory(addr), addr, addr_type, val, index_vect));
    set_memory(vstore, addr_type);
  } else {
    Node* vload = gvn().transform(new LoadVectorGatherNode(control(), memory(addr), addr, addr_type, vector_type, index_vect));

    Node* box = box_vector(vload, vbox_type, elem_bt, num_elem);
    set_result(box);
  }

  old_map->destruct(&_gvn);

  C->set_max_vector_size(MAX2(C->max_vector_size(), (uint)(num_elem * type2aelembytes(elem_bt))));
  return true;
}

// <V extends Vector<?,?>>
// long reductionCoerced(int oprId, Class<?> vectorClass, Class<?> elementType, int vlen,
//                       V v,
//                       Function<V,Long> defaultImpl)

bool LibraryCallKit::inline_vector_reduction() {
  const TypeInt*     opr          = gvn().type(argument(0))->isa_int();
  const TypeInstPtr* vector_klass = gvn().type(argument(1))->isa_instptr();
  const TypeInstPtr* elem_klass   = gvn().type(argument(2))->isa_instptr();
  const TypeInt*     vlen         = gvn().type(argument(3))->isa_int();

  if (opr == NULL || vector_klass == NULL || elem_klass == NULL || vlen == NULL ||
      !opr->is_con() || vector_klass->const_oop() == NULL || elem_klass->const_oop() == NULL || !vlen->is_con()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** missing constant: opr=%s vclass=%s etype=%s vlen=%s",
                    NodeClassNames[argument(0)->Opcode()],
                    NodeClassNames[argument(1)->Opcode()],
                    NodeClassNames[argument(2)->Opcode()],
                    NodeClassNames[argument(3)->Opcode()]);
    }
    return false; // not enough info for intrinsification
  }
  if (!is_klass_initialized(vector_klass)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** klass argument not initialized");
    }
    return false;
  }
  ciType* elem_type = elem_klass->const_oop()->as_instance()->java_mirror_type();
  if (!elem_type->is_primitive_type()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not a primitive bt=%d", elem_type->basic_type());
    }
    return false; // should be primitive type
  }
  BasicType elem_bt = elem_type->basic_type();
  int num_elem = vlen->get_con();

  int opc  = VectorSupport::vop2ideal(opr->get_con(), elem_bt);
  int sopc = ReductionNode::opcode(opc, elem_bt);

  // TODO When mask usage is supported, VecMaskNotUsed needs to be VecMaskUseLoad.
  if (!arch_supports_vector(sopc, num_elem, elem_bt, VecMaskNotUsed)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=1 op=%d/reduce vlen=%d etype=%s ismask=no",
                    sopc, num_elem, type2name(elem_bt));
    }
    return false;
  }

  ciKlass* vbox_klass = vector_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* vbox_type = TypeInstPtr::make_exact(TypePtr::NotNull, vbox_klass);

  Node* opd = unbox_vector(argument(4), vbox_type, elem_bt, num_elem);
  if (opd == NULL) {
    return false; // operand unboxing failed
  }

  Node* init = ReductionNode::make_reduction_input(gvn(), opc, elem_bt);
  Node* rn = gvn().transform(ReductionNode::make(opc, NULL, init, opd, elem_bt));

  Node* bits = NULL;
  switch (elem_bt) {
    case T_BYTE:
    case T_SHORT:
    case T_INT: {
      bits = gvn().transform(new ConvI2LNode(rn));
      break;
    }
    case T_FLOAT: {
      rn   = gvn().transform(new MoveF2INode(rn));
      bits = gvn().transform(new ConvI2LNode(rn));
      break;
    }
    case T_DOUBLE: {
      bits = gvn().transform(new MoveD2LNode(rn));
      break;
    }
    case T_LONG: {
      bits = rn; // no conversion needed
      break;
    }
    default: fatal("%s", type2name(elem_bt));
  }
  set_result(bits);
  C->set_max_vector_size(MAX2(C->max_vector_size(), (uint)(num_elem * type2aelembytes(elem_bt))));
  return true;
}

// public static <V> boolean test(int cond, Class<?> vectorClass, Class<?> elementType, int vlen,
//                                V v1, V v2,
//                                BiFunction<V, V, Boolean> defaultImpl) {
//
bool LibraryCallKit::inline_vector_test() {
  const TypeInt*     cond         = gvn().type(argument(0))->isa_int();
  const TypeInstPtr* vector_klass = gvn().type(argument(1))->isa_instptr();
  const TypeInstPtr* elem_klass   = gvn().type(argument(2))->isa_instptr();
  const TypeInt*     vlen         = gvn().type(argument(3))->isa_int();

  if (cond == NULL || vector_klass == NULL || elem_klass == NULL || vlen == NULL ||
      !cond->is_con() || vector_klass->const_oop() == NULL || elem_klass->const_oop() == NULL || !vlen->is_con()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** missing constant: cond=%s vclass=%s etype=%s vlen=%s",
                    NodeClassNames[argument(0)->Opcode()],
                    NodeClassNames[argument(1)->Opcode()],
                    NodeClassNames[argument(2)->Opcode()],
                    NodeClassNames[argument(3)->Opcode()]);
    }
    return false; // not enough info for intrinsification
  }
  if (!is_klass_initialized(vector_klass)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** klass argument not initialized");
    }
    return false;
  }
  ciType* elem_type = elem_klass->const_oop()->as_instance()->java_mirror_type();
  if (!elem_type->is_primitive_type()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not a primitive bt=%d", elem_type->basic_type());
    }
    return false; // should be primitive type
  }
  BasicType elem_bt = elem_type->basic_type();
  int num_elem = vlen->get_con();
  BoolTest::mask booltest = (BoolTest::mask)cond->get_con();
  ciKlass* vbox_klass = vector_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* vbox_type = TypeInstPtr::make_exact(TypePtr::NotNull, vbox_klass);

  if (!arch_supports_vector(Op_VectorTest, num_elem, elem_bt, is_vector_mask(vbox_klass) ? VecMaskUseLoad : VecMaskNotUsed)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=2 op=test/%d vlen=%d etype=%s ismask=%d",
                    cond->get_con(), num_elem, type2name(elem_bt),
                    is_vector_mask(vbox_klass));
    }
    return false;
  }

  Node* opd1 = unbox_vector(argument(4), vbox_type, elem_bt, num_elem);
  Node* opd2 = unbox_vector(argument(5), vbox_type, elem_bt, num_elem);
  if (opd1 == NULL || opd2 == NULL) {
    return false; // operand unboxing failed
  }
  Node* test = new VectorTestNode(opd1, opd2, booltest);
  test = gvn().transform(test);

  set_result(test);
  C->set_max_vector_size(MAX2(C->max_vector_size(), (uint)(num_elem * type2aelembytes(elem_bt))));
  return true;
}

// public static
// <V extends Vector, M extends Mask>
// V blend(Class<V> vectorClass, Class<M> maskClass, Class<?> elementType, int vlen,
//         V v1, V v2, M m,
//         VectorBlendOp<V,M> defaultImpl) { ...
//
bool LibraryCallKit::inline_vector_blend() {
  const TypeInstPtr* vector_klass = gvn().type(argument(0))->isa_instptr();
  const TypeInstPtr* mask_klass   = gvn().type(argument(1))->isa_instptr();
  const TypeInstPtr* elem_klass   = gvn().type(argument(2))->isa_instptr();
  const TypeInt*     vlen         = gvn().type(argument(3))->isa_int();

  if (mask_klass == NULL || vector_klass == NULL || elem_klass == NULL || vlen == NULL) {
    return false; // dead code
  }
  if (mask_klass->const_oop() == NULL || vector_klass->const_oop() == NULL ||
      elem_klass->const_oop() == NULL || !vlen->is_con()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** missing constant: vclass=%s mclass=%s etype=%s vlen=%s",
                    NodeClassNames[argument(0)->Opcode()],
                    NodeClassNames[argument(1)->Opcode()],
                    NodeClassNames[argument(2)->Opcode()],
                    NodeClassNames[argument(3)->Opcode()]);
    }
    return false; // not enough info for intrinsification
  }
  if (!is_klass_initialized(vector_klass) || !is_klass_initialized(mask_klass)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** klass argument not initialized");
    }
    return false;
  }
  ciType* elem_type = elem_klass->const_oop()->as_instance()->java_mirror_type();
  if (!elem_type->is_primitive_type()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not a primitive bt=%d", elem_type->basic_type());
    }
    return false; // should be primitive type
  }
  BasicType elem_bt = elem_type->basic_type();
  BasicType mask_bt = elem_bt;
  int num_elem = vlen->get_con();

  if (!arch_supports_vector(Op_VectorBlend, num_elem, elem_bt, VecMaskUseLoad)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=2 op=blend vlen=%d etype=%s ismask=useload",
                    num_elem, type2name(elem_bt));
    }
    return false; // not supported
  }
  ciKlass* vbox_klass = vector_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* vbox_type = TypeInstPtr::make_exact(TypePtr::NotNull, vbox_klass);

  ciKlass* mbox_klass = mask_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* mbox_type = TypeInstPtr::make_exact(TypePtr::NotNull, mbox_klass);

  Node* v1   = unbox_vector(argument(4), vbox_type, elem_bt, num_elem);
  Node* v2   = unbox_vector(argument(5), vbox_type, elem_bt, num_elem);
  Node* mask = unbox_vector(argument(6), mbox_type, mask_bt, num_elem);

  if (v1 == NULL || v2 == NULL || mask == NULL) {
    return false; // operand unboxing failed
  }

  Node* blend = gvn().transform(new VectorBlendNode(v1, v2, mask));

  Node* box = box_vector(blend, vbox_type, elem_bt, num_elem);
  set_result(box);
  C->set_max_vector_size(MAX2(C->max_vector_size(), (uint)(num_elem * type2aelembytes(elem_bt))));
  return true;
}

//  public static <V extends Vector<E,S>,
//          M extends Vector.Mask<E,S>,
//          S extends Vector.Shape, E>
//  M compare(int cond, Class<V> vectorClass, Class<M> maskClass, Class<?> elementType, int vlen,
//            V v1, V v2,
//            VectorCompareOp<V,M> defaultImpl) { ...
//
bool LibraryCallKit::inline_vector_compare() {
  const TypeInt*     cond         = gvn().type(argument(0))->isa_int();
  const TypeInstPtr* vector_klass = gvn().type(argument(1))->isa_instptr();
  const TypeInstPtr* mask_klass   = gvn().type(argument(2))->isa_instptr();
  const TypeInstPtr* elem_klass   = gvn().type(argument(3))->isa_instptr();
  const TypeInt*     vlen         = gvn().type(argument(4))->isa_int();

  if (cond == NULL || vector_klass == NULL || mask_klass == NULL || elem_klass == NULL || vlen == NULL) {
    return false; // dead code
  }
  if (!cond->is_con() || vector_klass->const_oop() == NULL || mask_klass->const_oop() == NULL ||
      elem_klass->const_oop() == NULL || !vlen->is_con()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** missing constant: cond=%s vclass=%s mclass=%s etype=%s vlen=%s",
                    NodeClassNames[argument(0)->Opcode()],
                    NodeClassNames[argument(1)->Opcode()],
                    NodeClassNames[argument(2)->Opcode()],
                    NodeClassNames[argument(3)->Opcode()],
                    NodeClassNames[argument(4)->Opcode()]);
    }
    return false; // not enough info for intrinsification
  }
  if (!is_klass_initialized(vector_klass) || !is_klass_initialized(mask_klass)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** klass argument not initialized");
    }
    return false;
  }
  ciType* elem_type = elem_klass->const_oop()->as_instance()->java_mirror_type();
  if (!elem_type->is_primitive_type()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not a primitive bt=%d", elem_type->basic_type());
    }
    return false; // should be primitive type
  }

  int num_elem = vlen->get_con();
  BasicType elem_bt = elem_type->basic_type();
  BasicType mask_bt = elem_bt;

  if ((cond->get_con() & BoolTest::unsigned_compare) != 0) {
    if (!Matcher::supports_vector_comparison_unsigned(num_elem, elem_bt)) {
      if (C->print_intrinsics()) {
        tty->print_cr("  ** not supported: unsigned comparison op=comp/%d vlen=%d etype=%s ismask=usestore",
                      cond->get_con() & (BoolTest::unsigned_compare - 1), num_elem, type2name(elem_bt));
      }
      return false;
    }
  }

  if (!arch_supports_vector(Op_VectorMaskCmp, num_elem, elem_bt, VecMaskUseStore)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=2 op=comp/%d vlen=%d etype=%s ismask=usestore",
                    cond->get_con(), num_elem, type2name(elem_bt));
    }
    return false;
  }

  ciKlass* vbox_klass = vector_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* vbox_type = TypeInstPtr::make_exact(TypePtr::NotNull, vbox_klass);

  ciKlass* mbox_klass = mask_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* mbox_type = TypeInstPtr::make_exact(TypePtr::NotNull, mbox_klass);

  Node* v1 = unbox_vector(argument(5), vbox_type, elem_bt, num_elem);
  Node* v2 = unbox_vector(argument(6), vbox_type, elem_bt, num_elem);

  if (v1 == NULL || v2 == NULL) {
    return false; // operand unboxing failed
  }
  BoolTest::mask pred = (BoolTest::mask)cond->get_con();
  ConINode* pred_node = (ConINode*)gvn().makecon(cond);

  const TypeVect* vt = TypeVect::make(mask_bt, num_elem);
  Node* operation = gvn().transform(new VectorMaskCmpNode(pred, v1, v2, pred_node, vt));

  Node* box = box_vector(operation, mbox_type, mask_bt, num_elem);
  set_result(box);
  C->set_max_vector_size(MAX2(C->max_vector_size(), (uint)(num_elem * type2aelembytes(elem_bt))));
  return true;
}

// public static
// <V extends Vector, Sh extends Shuffle>
//  V rearrangeOp(Class<V> vectorClass, Class<Sh> shuffleClass, Class< ? > elementType, int vlen,
//    V v1, Sh sh,
//    VectorSwizzleOp<V, Sh, S, E> defaultImpl) { ...

bool LibraryCallKit::inline_vector_rearrange() {
  const TypeInstPtr* vector_klass  = gvn().type(argument(0))->isa_instptr();
  const TypeInstPtr* shuffle_klass = gvn().type(argument(1))->isa_instptr();
  const TypeInstPtr* elem_klass    = gvn().type(argument(2))->isa_instptr();
  const TypeInt*     vlen          = gvn().type(argument(3))->isa_int();

  if (vector_klass == NULL || shuffle_klass == NULL || elem_klass == NULL || vlen == NULL) {
    return false; // dead code
  }
  if (shuffle_klass->const_oop() == NULL || vector_klass->const_oop() == NULL ||
    elem_klass->const_oop() == NULL || !vlen->is_con()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** missing constant: vclass=%s sclass=%s etype=%s vlen=%s",
                    NodeClassNames[argument(0)->Opcode()],
                    NodeClassNames[argument(1)->Opcode()],
                    NodeClassNames[argument(2)->Opcode()],
                    NodeClassNames[argument(3)->Opcode()]);
    }
    return false; // not enough info for intrinsification
  }
  if (!is_klass_initialized(vector_klass) || !is_klass_initialized(shuffle_klass)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** klass argument not initialized");
    }
    return false;
  }
  ciType* elem_type = elem_klass->const_oop()->as_instance()->java_mirror_type();
  if (!elem_type->is_primitive_type()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not a primitive bt=%d", elem_type->basic_type());
    }
    return false; // should be primitive type
  }
  BasicType elem_bt = elem_type->basic_type();
  BasicType shuffle_bt = elem_bt;
  int num_elem = vlen->get_con();

  if (!arch_supports_vector(Op_VectorLoadShuffle, num_elem, elem_bt, VecMaskNotUsed)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=0 op=load/shuffle vlen=%d etype=%s ismask=no",
                    num_elem, type2name(elem_bt));
    }
    return false; // not supported
  }
  if (!arch_supports_vector(Op_VectorRearrange, num_elem, elem_bt, VecMaskNotUsed)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=2 op=shuffle/rearrange vlen=%d etype=%s ismask=no",
                    num_elem, type2name(elem_bt));
    }
    return false; // not supported
  }
  ciKlass* vbox_klass = vector_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* vbox_type = TypeInstPtr::make_exact(TypePtr::NotNull, vbox_klass);

  ciKlass* shbox_klass = shuffle_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* shbox_type = TypeInstPtr::make_exact(TypePtr::NotNull, shbox_klass);

  Node* v1 = unbox_vector(argument(4), vbox_type, elem_bt, num_elem);
  Node* shuffle = unbox_vector(argument(5), shbox_type, shuffle_bt, num_elem);

  if (v1 == NULL || shuffle == NULL) {
    return false; // operand unboxing failed
  }

  Node* rearrange = gvn().transform(new VectorRearrangeNode(v1, shuffle));

  Node* box = box_vector(rearrange, vbox_type, elem_bt, num_elem);
  set_result(box);
  C->set_max_vector_size(MAX2(C->max_vector_size(), (uint)(num_elem * type2aelembytes(elem_bt))));
  return true;
}

static address get_svml_address(int vop, int bits, BasicType bt, char* name_ptr, int name_len) {
  address addr = NULL;
  assert(UseVectorStubs, "sanity");
  assert(name_ptr != NULL, "unexpected");
  assert((vop >= VectorSupport::VECTOR_OP_SVML_START) && (vop <= VectorSupport::VECTOR_OP_SVML_END), "unexpected");
  int op = vop - VectorSupport::VECTOR_OP_SVML_START;

  switch(bits) {
    case 64:  //fallthough
    case 128: //fallthough
    case 256: //fallthough
    case 512:
      if (bt == T_FLOAT) {
        snprintf(name_ptr, name_len, "vector_%s_float%d", VectorSupport::svmlname[op], bits);
        addr = StubRoutines::_vector_f_math[exact_log2(bits/64)][op];
      } else {
        assert(bt == T_DOUBLE, "must be FP type only");
        snprintf(name_ptr, name_len, "vector_%s_double%d", VectorSupport::svmlname[op], bits);
        addr = StubRoutines::_vector_d_math[exact_log2(bits/64)][op];
      }
      break;
    default:
      snprintf(name_ptr, name_len, "invalid");
      addr = NULL;
      Unimplemented();
      break;
  }

  return addr;
}

Node* LibraryCallKit::gen_call_to_svml(int vector_api_op_id, BasicType bt, int num_elem, Node* opd1, Node* opd2) {
  assert(UseVectorStubs, "sanity");
  assert(vector_api_op_id >= VectorSupport::VECTOR_OP_SVML_START && vector_api_op_id <= VectorSupport::VECTOR_OP_SVML_END, "need valid op id");
  assert(opd1 != NULL, "must not be null");
  const TypeVect* vt = TypeVect::make(bt, num_elem);
  const TypeFunc* call_type = OptoRuntime::Math_Vector_Vector_Type(opd2 != NULL ? 2 : 1, vt, vt);
  char name[100] = "";

  // Get address for svml method.
  address addr = get_svml_address(vector_api_op_id, vt->length_in_bytes() * BitsPerByte, bt, name, 100);

  if (addr == NULL) {
    return NULL;
  }

  assert(name != NULL, "name must not be null");
  Node* operation = make_runtime_call(RC_VECTOR,
                                      call_type,
                                      addr,
                                      name,
                                      TypePtr::BOTTOM,
                                      opd1,
                                      opd2);
  return gvn().transform(new ProjNode(gvn().transform(operation), TypeFunc::Parms));
}

//  public static
//  <V extends Vector<?,?>>
//  V broadcastInt(int opr, Class<V> vectorClass, Class<?> elementType, int vlen,
//                 V v, int i,
//                 VectorBroadcastIntOp<V> defaultImpl) {
//
bool LibraryCallKit::inline_vector_broadcast_int() {
  const TypeInt*     opr          = gvn().type(argument(0))->isa_int();
  const TypeInstPtr* vector_klass = gvn().type(argument(1))->isa_instptr();
  const TypeInstPtr* elem_klass   = gvn().type(argument(2))->isa_instptr();
  const TypeInt*     vlen         = gvn().type(argument(3))->isa_int();

  if (opr == NULL || vector_klass == NULL || elem_klass == NULL || vlen == NULL) {
    return false; // dead code
  }
  if (!opr->is_con() || vector_klass->const_oop() == NULL || elem_klass->const_oop() == NULL || !vlen->is_con()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** missing constant: opr=%s vclass=%s etype=%s vlen=%s",
                    NodeClassNames[argument(0)->Opcode()],
                    NodeClassNames[argument(1)->Opcode()],
                    NodeClassNames[argument(2)->Opcode()],
                    NodeClassNames[argument(3)->Opcode()]);
    }
    return false; // not enough info for intrinsification
  }
  if (!is_klass_initialized(vector_klass)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** klass argument not initialized");
    }
    return false;
  }
  ciType* elem_type = elem_klass->const_oop()->as_instance()->java_mirror_type();
  if (!elem_type->is_primitive_type()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not a primitive bt=%d", elem_type->basic_type());
    }
    return false; // should be primitive type
  }
  BasicType elem_bt = elem_type->basic_type();
  int num_elem = vlen->get_con();
  int opc = VectorSupport::vop2ideal(opr->get_con(), elem_bt);
  if (opc == 0 || !VectorNode::is_shift_opcode(opc)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** operation not supported: op=%d bt=%s", opr->get_con(), type2name(elem_bt));
    }
    return false; // operation not supported
  }
  int sopc = VectorNode::opcode(opc, elem_bt);
  if (sopc == 0) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** operation not supported: opc=%s bt=%s", NodeClassNames[opc], type2name(elem_bt));
    }
    return false; // operation not supported
  }
  ciKlass* vbox_klass = vector_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* vbox_type = TypeInstPtr::make_exact(TypePtr::NotNull, vbox_klass);

  if (!arch_supports_vector(sopc, num_elem, elem_bt, VecMaskNotUsed, true /*has_scalar_args*/)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=0 op=int/%d vlen=%d etype=%s ismask=no",
                    sopc, num_elem, type2name(elem_bt));
    }
    return false; // not supported
  }
  Node* opd1 = unbox_vector(argument(4), vbox_type, elem_bt, num_elem);
  Node* opd2 = vector_shift_count(argument(5), opc, elem_bt, num_elem);
  if (opd1 == NULL || opd2 == NULL) {
    return false;
  }
  Node* operation = gvn().transform(VectorNode::make(opc, opd1, opd2, num_elem, elem_bt));

  Node* vbox = box_vector(operation, vbox_type, elem_bt, num_elem);
  set_result(vbox);
  C->set_max_vector_size(MAX2(C->max_vector_size(), (uint)(num_elem * type2aelembytes(elem_bt))));
  return true;
}

// public static <VOUT extends VectorPayload,
//                 VIN extends VectorPayload,
//                   S extends VectorSpecies>
// VOUT convert(int oprId,
//           Class<?> fromVectorClass, Class<?> fromElementType, int fromVLen,
//           Class<?>   toVectorClass, Class<?>   toElementType, int   toVLen,
//           VIN v, S s,
//           VectorConvertOp<VOUT, VIN, S> defaultImpl) {
//
bool LibraryCallKit::inline_vector_convert() {
  const TypeInt*     opr               = gvn().type(argument(0))->isa_int();

  const TypeInstPtr* vector_klass_from = gvn().type(argument(1))->isa_instptr();
  const TypeInstPtr* elem_klass_from   = gvn().type(argument(2))->isa_instptr();
  const TypeInt*     vlen_from         = gvn().type(argument(3))->isa_int();

  const TypeInstPtr* vector_klass_to   = gvn().type(argument(4))->isa_instptr();
  const TypeInstPtr* elem_klass_to     = gvn().type(argument(5))->isa_instptr();
  const TypeInt*     vlen_to           = gvn().type(argument(6))->isa_int();

  if (opr == NULL ||
      vector_klass_from == NULL || elem_klass_from == NULL || vlen_from == NULL ||
      vector_klass_to   == NULL || elem_klass_to   == NULL || vlen_to   == NULL) {
    return false; // dead code
  }
  if (!opr->is_con() ||
      vector_klass_from->const_oop() == NULL || elem_klass_from->const_oop() == NULL || !vlen_from->is_con() ||
      vector_klass_to->const_oop() == NULL || elem_klass_to->const_oop() == NULL || !vlen_to->is_con()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** missing constant: opr=%s vclass_from=%s etype_from=%s vlen_from=%s vclass_to=%s etype_to=%s vlen_to=%s",
                    NodeClassNames[argument(0)->Opcode()],
                    NodeClassNames[argument(1)->Opcode()],
                    NodeClassNames[argument(2)->Opcode()],
                    NodeClassNames[argument(3)->Opcode()],
                    NodeClassNames[argument(4)->Opcode()],
                    NodeClassNames[argument(5)->Opcode()],
                    NodeClassNames[argument(6)->Opcode()]);
    }
    return false; // not enough info for intrinsification
  }
  if (!is_klass_initialized(vector_klass_from) || !is_klass_initialized(vector_klass_to)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** klass argument not initialized");
    }
    return false;
  }

  assert(opr->get_con() == VectorSupport::VECTOR_OP_CAST ||
         opr->get_con() == VectorSupport::VECTOR_OP_REINTERPRET, "wrong opcode");
  bool is_cast = (opr->get_con() == VectorSupport::VECTOR_OP_CAST);

  ciKlass* vbox_klass_from = vector_klass_from->const_oop()->as_instance()->java_lang_Class_klass();
  ciKlass* vbox_klass_to = vector_klass_to->const_oop()->as_instance()->java_lang_Class_klass();
  if (is_vector_shuffle(vbox_klass_from)) {
    return false; // vector shuffles aren't supported
  }
  bool is_mask = is_vector_mask(vbox_klass_from);

  ciType* elem_type_from = elem_klass_from->const_oop()->as_instance()->java_mirror_type();
  if (!elem_type_from->is_primitive_type()) {
    return false; // should be primitive type
  }
  BasicType elem_bt_from = elem_type_from->basic_type();
  ciType* elem_type_to = elem_klass_to->const_oop()->as_instance()->java_mirror_type();
  if (!elem_type_to->is_primitive_type()) {
    return false; // should be primitive type
  }
  BasicType elem_bt_to = elem_type_to->basic_type();
  if (is_mask && (type2aelembytes(elem_bt_from) != type2aelembytes(elem_bt_to))) {
    return false; // elem size mismatch
  }

  int num_elem_from = vlen_from->get_con();
  int num_elem_to = vlen_to->get_con();

  // Check whether we can unbox to appropriate size. Even with casting, checking for reinterpret is needed
  // since we may need to change size.
  if (!arch_supports_vector(Op_VectorReinterpret,
                            num_elem_from,
                            elem_bt_from,
                            is_mask ? VecMaskUseAll : VecMaskNotUsed)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=1 op=%s/1 vlen1=%d etype1=%s ismask=%d",
                    is_cast ? "cast" : "reinterpret",
                    num_elem_from, type2name(elem_bt_from), is_mask);
    }
    return false;
  }

  // Check whether we can support resizing/reinterpreting to the new size.
  if (!arch_supports_vector(Op_VectorReinterpret,
                            num_elem_to,
                            elem_bt_to,
                            is_mask ? VecMaskUseAll : VecMaskNotUsed)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=1 op=%s/2 vlen2=%d etype2=%s ismask=%d",
                    is_cast ? "cast" : "reinterpret",
                    num_elem_to, type2name(elem_bt_to), is_mask);
    }
    return false;
  }

  // At this point, we know that both input and output vector registers are supported
  // by the architecture. Next check if the casted type is simply to same type - which means
  // that it is actually a resize and not a cast.
  if (is_cast && elem_bt_from == elem_bt_to) {
    is_cast = false;
  }

  const TypeInstPtr* vbox_type_from = TypeInstPtr::make_exact(TypePtr::NotNull, vbox_klass_from);

  Node* opd1 = unbox_vector(argument(7), vbox_type_from, elem_bt_from, num_elem_from);
  if (opd1 == NULL) {
    return false;
  }

  const TypeVect* src_type = TypeVect::make(elem_bt_from, num_elem_from);
  const TypeVect* dst_type = TypeVect::make(elem_bt_to,   num_elem_to);

  Node* op = opd1;
  if (is_cast) {
    assert(!is_mask, "masks cannot be casted");
    int cast_vopc = VectorCastNode::opcode(elem_bt_from);
    // Make sure that cast is implemented to particular type/size combination.
    if (!arch_supports_vector(cast_vopc, num_elem_to, elem_bt_to, VecMaskNotUsed)) {
      if (C->print_intrinsics()) {
        tty->print_cr("  ** not supported: arity=1 op=cast#%d/3 vlen2=%d etype2=%s ismask=%d",
                      cast_vopc,
                      num_elem_to, type2name(elem_bt_to), is_mask);
      }
      return false;
    }

    if (num_elem_from < num_elem_to) {
      // Since input and output number of elements are not consistent, we need to make sure we
      // properly size. Thus, first make a cast that retains the number of elements from source.
      // In case the size exceeds the arch size, we do the minimum.
      int num_elem_for_cast = MIN2(num_elem_from, Matcher::max_vector_size(elem_bt_to));

      // It is possible that arch does not support this intermediate vector size
      // TODO More complex logic required here to handle this corner case for the sizes.
      if (!arch_supports_vector(cast_vopc, num_elem_for_cast, elem_bt_to, VecMaskNotUsed)) {
        if (C->print_intrinsics()) {
          tty->print_cr("  ** not supported: arity=1 op=cast#%d/4 vlen1=%d etype2=%s ismask=%d",
                        cast_vopc,
                        num_elem_for_cast, type2name(elem_bt_to), is_mask);
        }
        return false;
      }

      op = gvn().transform(VectorCastNode::make(cast_vopc, op, elem_bt_to, num_elem_for_cast));
      // Now ensure that the destination gets properly resized to needed size.
      op = gvn().transform(new VectorReinterpretNode(op, op->bottom_type()->is_vect(), dst_type));
    } else if (num_elem_from > num_elem_to) {
      // Since number elements from input is larger than output, simply reduce size of input (we are supposed to
      // drop top elements anyway).
      int num_elem_for_resize = MAX2(num_elem_to, Matcher::min_vector_size(elem_bt_from));

      // It is possible that arch does not support this intermediate vector size
      // TODO More complex logic required here to handle this corner case for the sizes.
      if (!arch_supports_vector(Op_VectorReinterpret,
                                num_elem_for_resize,
                                elem_bt_from,
                                VecMaskNotUsed)) {
        if (C->print_intrinsics()) {
          tty->print_cr("  ** not supported: arity=1 op=cast/5 vlen2=%d etype1=%s ismask=%d",
                        num_elem_for_resize, type2name(elem_bt_from), is_mask);
        }
        return false;
      }

      op = gvn().transform(new VectorReinterpretNode(op,
                                                     src_type,
                                                     TypeVect::make(elem_bt_from,
                                                                    num_elem_for_resize)));
      op = gvn().transform(VectorCastNode::make(cast_vopc, op, elem_bt_to, num_elem_to));
    } else {
      // Since input and output number of elements match, and since we know this vector size is
      // supported, simply do a cast with no resize needed.
      op = gvn().transform(VectorCastNode::make(cast_vopc, op, elem_bt_to, num_elem_to));
    }
  } else if (Type::cmp(src_type, dst_type) != 0) {
    assert(!is_cast, "must be reinterpret");
    op = gvn().transform(new VectorReinterpretNode(op, src_type, dst_type));
  }

  const TypeInstPtr* vbox_type_to = TypeInstPtr::make_exact(TypePtr::NotNull, vbox_klass_to);
  Node* vbox = box_vector(op, vbox_type_to, elem_bt_to, num_elem_to);
  set_result(vbox);
  C->set_max_vector_size(MAX2(C->max_vector_size(), (uint)(num_elem_to * type2aelembytes(elem_bt_to))));
  return true;
}

//  public static
//  <V extends Vector<?>>
//  V insert(Class<? extends V> vectorClass, Class<?> elementType, int vlen,
//           V vec, int ix, long val,
//           VecInsertOp<V> defaultImpl) {
//
bool LibraryCallKit::inline_vector_insert() {
  const TypeInstPtr* vector_klass = gvn().type(argument(0))->isa_instptr();
  const TypeInstPtr* elem_klass   = gvn().type(argument(1))->isa_instptr();
  const TypeInt*     vlen         = gvn().type(argument(2))->isa_int();
  const TypeInt*     idx          = gvn().type(argument(4))->isa_int();

  if (vector_klass == NULL || elem_klass == NULL || vlen == NULL || idx == NULL) {
    return false; // dead code
  }
  if (vector_klass->const_oop() == NULL || elem_klass->const_oop() == NULL || !vlen->is_con() || !idx->is_con()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** missing constant: vclass=%s etype=%s vlen=%s idx=%s",
                    NodeClassNames[argument(0)->Opcode()],
                    NodeClassNames[argument(1)->Opcode()],
                    NodeClassNames[argument(2)->Opcode()],
                    NodeClassNames[argument(4)->Opcode()]);
    }
    return false; // not enough info for intrinsification
  }
  if (!is_klass_initialized(vector_klass)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** klass argument not initialized");
    }
    return false;
  }
  ciType* elem_type = elem_klass->const_oop()->as_instance()->java_mirror_type();
  if (!elem_type->is_primitive_type()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not a primitive bt=%d", elem_type->basic_type());
    }
    return false; // should be primitive type
  }
  BasicType elem_bt = elem_type->basic_type();
  int num_elem = vlen->get_con();
  if (!arch_supports_vector(Op_VectorInsert, num_elem, elem_bt, VecMaskNotUsed)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=1 op=insert vlen=%d etype=%s ismask=no",
                    num_elem, type2name(elem_bt));
    }
    return false; // not supported
  }

  ciKlass* vbox_klass = vector_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* vbox_type = TypeInstPtr::make_exact(TypePtr::NotNull, vbox_klass);

  Node* opd = unbox_vector(argument(3), vbox_type, elem_bt, num_elem);
  if (opd == NULL) {
    return false;
  }

  Node* insert_val = argument(5);
  assert(gvn().type(insert_val)->isa_long() != NULL, "expected to be long");

  // Convert insert value back to its appropriate type.
  switch (elem_bt) {
    case T_BYTE:
      insert_val = gvn().transform(new ConvL2INode(insert_val));
      insert_val = gvn().transform(new CastIINode(insert_val, TypeInt::BYTE));
      break;
    case T_SHORT:
      insert_val = gvn().transform(new ConvL2INode(insert_val));
      insert_val = gvn().transform(new CastIINode(insert_val, TypeInt::SHORT));
      break;
    case T_INT:
      insert_val = gvn().transform(new ConvL2INode(insert_val));
      break;
    case T_FLOAT:
      insert_val = gvn().transform(new ConvL2INode(insert_val));
      insert_val = gvn().transform(new MoveI2FNode(insert_val));
      break;
    case T_DOUBLE:
      insert_val = gvn().transform(new MoveL2DNode(insert_val));
      break;
    case T_LONG:
      // no conversion needed
      break;
    default: fatal("%s", type2name(elem_bt)); break;
  }

  Node* operation = gvn().transform(VectorInsertNode::make(opd, insert_val, idx->get_con()));

  Node* vbox = box_vector(operation, vbox_type, elem_bt, num_elem);
  set_result(vbox);
  C->set_max_vector_size(MAX2(C->max_vector_size(), (uint)(num_elem * type2aelembytes(elem_bt))));
  return true;
}

//  public static
//  <V extends Vector<?>>
//  long extract(Class<?> vectorClass, Class<?> elementType, int vlen,
//               V vec, int ix,
//               VecExtractOp<V> defaultImpl) {
//
bool LibraryCallKit::inline_vector_extract() {
  const TypeInstPtr* vector_klass = gvn().type(argument(0))->isa_instptr();
  const TypeInstPtr* elem_klass   = gvn().type(argument(1))->isa_instptr();
  const TypeInt*     vlen         = gvn().type(argument(2))->isa_int();
  const TypeInt*     idx          = gvn().type(argument(4))->isa_int();

  if (vector_klass == NULL || elem_klass == NULL || vlen == NULL || idx == NULL) {
    return false; // dead code
  }
  if (vector_klass->const_oop() == NULL || elem_klass->const_oop() == NULL || !vlen->is_con() || !idx->is_con()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** missing constant: vclass=%s etype=%s vlen=%s idx=%s",
                    NodeClassNames[argument(0)->Opcode()],
                    NodeClassNames[argument(1)->Opcode()],
                    NodeClassNames[argument(2)->Opcode()],
                    NodeClassNames[argument(4)->Opcode()]);
    }
    return false; // not enough info for intrinsification
  }
  if (!is_klass_initialized(vector_klass)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** klass argument not initialized");
    }
    return false;
  }
  ciType* elem_type = elem_klass->const_oop()->as_instance()->java_mirror_type();
  if (!elem_type->is_primitive_type()) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not a primitive bt=%d", elem_type->basic_type());
    }
    return false; // should be primitive type
  }
  BasicType elem_bt = elem_type->basic_type();
  int num_elem = vlen->get_con();
  int vopc = ExtractNode::opcode(elem_bt);
  if (!arch_supports_vector(vopc, num_elem, elem_bt, VecMaskNotUsed)) {
    if (C->print_intrinsics()) {
      tty->print_cr("  ** not supported: arity=1 op=extract vlen=%d etype=%s ismask=no",
                    num_elem, type2name(elem_bt));
    }
    return false; // not supported
  }

  ciKlass* vbox_klass = vector_klass->const_oop()->as_instance()->java_lang_Class_klass();
  const TypeInstPtr* vbox_type = TypeInstPtr::make_exact(TypePtr::NotNull, vbox_klass);

  Node* opd = unbox_vector(argument(3), vbox_type, elem_bt, num_elem);
  if (opd == NULL) {
    return false;
  }

  Node* operation = gvn().transform(ExtractNode::make(opd, idx->get_con(), elem_bt));

  Node* bits = NULL;
  switch (elem_bt) {
    case T_BYTE:
    case T_SHORT:
    case T_INT: {
      bits = gvn().transform(new ConvI2LNode(operation));
      break;
    }
    case T_FLOAT: {
      bits = gvn().transform(new MoveF2INode(operation));
      bits = gvn().transform(new ConvI2LNode(bits));
      break;
    }
    case T_DOUBLE: {
      bits = gvn().transform(new MoveD2LNode(operation));
      break;
    }
    case T_LONG: {
      bits = operation; // no conversion needed
      break;
    }
    default: fatal("%s", type2name(elem_bt));
  }

  set_result(bits);
  return true;
}

