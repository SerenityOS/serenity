/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "memory/allocation.inline.hpp"
#include "opto/connode.hpp"
#include "opto/mulnode.hpp"
#include "opto/subnode.hpp"
#include "opto/vectornode.hpp"
#include "opto/convertnode.hpp"
#include "utilities/powerOfTwo.hpp"
#include "utilities/globalDefinitions.hpp"

//------------------------------VectorNode--------------------------------------

// Return the vector operator for the specified scalar operation
// and vector length.
int VectorNode::opcode(int sopc, BasicType bt) {
  switch (sopc) {
  case Op_AddI:
    switch (bt) {
    case T_BOOLEAN:
    case T_BYTE:      return Op_AddVB;
    case T_CHAR:
    case T_SHORT:     return Op_AddVS;
    case T_INT:       return Op_AddVI;
    default:          return 0;
    }
  case Op_AddL: return (bt == T_LONG   ? Op_AddVL : 0);
  case Op_AddF: return (bt == T_FLOAT  ? Op_AddVF : 0);
  case Op_AddD: return (bt == T_DOUBLE ? Op_AddVD : 0);

  case Op_SubI:
    switch (bt) {
    case T_BOOLEAN:
    case T_BYTE:   return Op_SubVB;
    case T_CHAR:
    case T_SHORT:  return Op_SubVS;
    case T_INT:    return Op_SubVI;
    default:       return 0;
    }
  case Op_SubL: return (bt == T_LONG   ? Op_SubVL : 0);
  case Op_SubF: return (bt == T_FLOAT  ? Op_SubVF : 0);
  case Op_SubD: return (bt == T_DOUBLE ? Op_SubVD : 0);

  case Op_MulI:
    switch (bt) {
    case T_BOOLEAN:return 0;
    case T_BYTE:   return Op_MulVB;
    case T_CHAR:
    case T_SHORT:  return Op_MulVS;
    case T_INT:    return Op_MulVI;
    default:       return 0;
    }
  case Op_MulL: return (bt == T_LONG ? Op_MulVL : 0);
  case Op_MulF:
    return (bt == T_FLOAT ? Op_MulVF : 0);
  case Op_MulD:
    return (bt == T_DOUBLE ? Op_MulVD : 0);
  case Op_FmaD:
    return (bt == T_DOUBLE ? Op_FmaVD : 0);
  case Op_FmaF:
    return (bt == T_FLOAT ? Op_FmaVF : 0);
  case Op_CMoveF:
    return (bt == T_FLOAT ? Op_CMoveVF : 0);
  case Op_CMoveD:
    return (bt == T_DOUBLE ? Op_CMoveVD : 0);
  case Op_DivF:
    return (bt == T_FLOAT ? Op_DivVF : 0);
  case Op_DivD:
    return (bt == T_DOUBLE ? Op_DivVD : 0);
  case Op_AbsI:
    switch (bt) {
    case T_BOOLEAN:
    case T_CHAR:  return 0; // abs does not make sense for unsigned
    case T_BYTE:  return Op_AbsVB;
    case T_SHORT: return Op_AbsVS;
    case T_INT:   return Op_AbsVI;
    default:      return 0;
    }
  case Op_AbsL:
    return (bt == T_LONG ? Op_AbsVL : 0);
  case Op_MinI:
    switch (bt) {
    case T_BOOLEAN:
    case T_CHAR:   return 0;
    case T_BYTE:
    case T_SHORT:
    case T_INT:    return Op_MinV;
    default:       return 0;
    }
  case Op_MinL:
    return (bt == T_LONG ? Op_MinV : 0);
  case Op_MinF:
    return (bt == T_FLOAT ? Op_MinV : 0);
  case Op_MinD:
    return (bt == T_DOUBLE ? Op_MinV : 0);
  case Op_MaxI:
    switch (bt) {
    case T_BOOLEAN:
    case T_CHAR:   return 0;
    case T_BYTE:
    case T_SHORT:
    case T_INT:    return Op_MaxV;
    default:       return 0;
    }
  case Op_MaxL:
    return (bt == T_LONG ? Op_MaxV : 0);
  case Op_MaxF:
    return (bt == T_FLOAT ? Op_MaxV : 0);
  case Op_MaxD:
    return (bt == T_DOUBLE ? Op_MaxV : 0);
  case Op_AbsF:
    return (bt == T_FLOAT ? Op_AbsVF : 0);
  case Op_AbsD:
    return (bt == T_DOUBLE ? Op_AbsVD : 0);
  case Op_NegI:
    return (bt == T_INT ? Op_NegVI : 0);
  case Op_NegF:
    return (bt == T_FLOAT ? Op_NegVF : 0);
  case Op_NegD:
    return (bt == T_DOUBLE ? Op_NegVD : 0);
  case Op_RoundDoubleMode:
    return (bt == T_DOUBLE ? Op_RoundDoubleModeV : 0);
  case Op_RotateLeft:
    return (bt == T_LONG || bt == T_INT ? Op_RotateLeftV : 0);
  case Op_RotateRight:
    return (bt == T_LONG || bt == T_INT ? Op_RotateRightV : 0);
  case Op_SqrtF:
    return (bt == T_FLOAT ? Op_SqrtVF : 0);
  case Op_SqrtD:
    return (bt == T_DOUBLE ? Op_SqrtVD : 0);
  case Op_PopCountI:
    // Unimplemented for subword types since bit count changes
    // depending on size of lane (and sign bit).
    return (bt == T_INT ? Op_PopCountVI : 0);
  case Op_LShiftI:
    switch (bt) {
    case T_BOOLEAN:
    case T_BYTE:   return Op_LShiftVB;
    case T_CHAR:
    case T_SHORT:  return Op_LShiftVS;
    case T_INT:    return Op_LShiftVI;
    default:       return 0;
    }
  case Op_LShiftL:
    return (bt == T_LONG ? Op_LShiftVL : 0);
  case Op_RShiftI:
    switch (bt) {
    case T_BOOLEAN:return Op_URShiftVB; // boolean is unsigned value
    case T_CHAR:   return Op_URShiftVS; // char is unsigned value
    case T_BYTE:   return Op_RShiftVB;
    case T_SHORT:  return Op_RShiftVS;
    case T_INT:    return Op_RShiftVI;
    default:       return 0;
    }
  case Op_RShiftL:
    return (bt == T_LONG ? Op_RShiftVL : 0);
  case Op_URShiftB:
    return (bt == T_BYTE ? Op_URShiftVB : 0);
  case Op_URShiftS:
    return (bt == T_SHORT ? Op_URShiftVS : 0);
  case Op_URShiftI:
    switch (bt) {
    case T_BOOLEAN:return Op_URShiftVB;
    case T_CHAR:   return Op_URShiftVS;
    case T_BYTE:
    case T_SHORT:  return 0; // Vector logical right shift for signed short
                             // values produces incorrect Java result for
                             // negative data because java code should convert
                             // a short value into int value with sign
                             // extension before a shift.
    case T_INT:    return Op_URShiftVI;
    default:       return 0;
    }
  case Op_URShiftL:
    return (bt == T_LONG ? Op_URShiftVL : 0);
  case Op_AndI:
  case Op_AndL:
    return Op_AndV;
  case Op_OrI:
  case Op_OrL:
    return Op_OrV;
  case Op_XorI:
  case Op_XorL:
    return Op_XorV;

  case Op_LoadB:
  case Op_LoadUB:
  case Op_LoadUS:
  case Op_LoadS:
  case Op_LoadI:
  case Op_LoadL:
  case Op_LoadF:
  case Op_LoadD:
    return Op_LoadVector;

  case Op_StoreB:
  case Op_StoreC:
  case Op_StoreI:
  case Op_StoreL:
  case Op_StoreF:
  case Op_StoreD:
    return Op_StoreVector;
  case Op_MulAddS2I:
    return Op_MulAddVS2VI;

  default:
    return 0; // Unimplemented
  }
}

int VectorNode::replicate_opcode(BasicType bt) {
  switch(bt) {
    case T_BOOLEAN:
    case T_BYTE:
      return Op_ReplicateB;
    case T_SHORT:
    case T_CHAR:
      return Op_ReplicateS;
    case T_INT:
      return Op_ReplicateI;
    case T_LONG:
      return Op_ReplicateL;
    case T_FLOAT:
      return Op_ReplicateF;
    case T_DOUBLE:
      return Op_ReplicateD;
    default:
      assert(false, "wrong type: %s", type2name(bt));
      return 0;
  }
}

// Also used to check if the code generator
// supports the vector operation.
bool VectorNode::implemented(int opc, uint vlen, BasicType bt) {
  if (is_java_primitive(bt) &&
      (vlen > 1) && is_power_of_2(vlen) &&
      Matcher::vector_size_supported(bt, vlen)) {
    int vopc = VectorNode::opcode(opc, bt);
    // For rotate operation we will do a lazy de-generation into
    // OrV/LShiftV/URShiftV pattern if the target does not support
    // vector rotation instruction.
    if (vopc == Op_RotateLeftV || vopc == Op_RotateRightV) {
      return is_vector_rotate_supported(vopc, vlen, bt);
    }
    return vopc > 0 && Matcher::match_rule_supported_vector(vopc, vlen, bt);
  }
  return false;
}

bool VectorNode::is_type_transition_short_to_int(Node* n) {
  switch (n->Opcode()) {
  case Op_MulAddS2I:
    return true;
  }
  return false;
}

bool VectorNode::is_type_transition_to_int(Node* n) {
  return is_type_transition_short_to_int(n);
}

bool VectorNode::is_muladds2i(Node* n) {
  if (n->Opcode() == Op_MulAddS2I) {
    return true;
  }
  return false;
}

bool VectorNode::is_roundopD(Node* n) {
  if (n->Opcode() == Op_RoundDoubleMode) {
    return true;
  }
  return false;
}

bool VectorNode::is_scalar_rotate(Node* n) {
  if (n->Opcode() == Op_RotateLeft || n->Opcode() == Op_RotateRight) {
    return true;
  }
  return false;
}

bool VectorNode::is_vector_rotate_supported(int vopc, uint vlen, BasicType bt) {
  assert(vopc == Op_RotateLeftV || vopc == Op_RotateRightV, "wrong opcode");

  // If target defines vector rotation patterns then no
  // need for degeneration.
  if (Matcher::match_rule_supported_vector(vopc, vlen, bt)) {
    return true;
  }

  // If target does not support variable shift operations then no point
  // in creating a rotate vector node since it will not be disintegratable.
  // Adding a pessimistic check to avoid complex pattern mathing which
  // may not be full proof.
  if (!Matcher::supports_vector_variable_shifts()) {
     return false;
  }

  // Validate existence of nodes created in case of rotate degeneration.
  switch (bt) {
    case T_INT:
      return Matcher::match_rule_supported_vector(Op_OrV,       vlen, bt) &&
             Matcher::match_rule_supported_vector(Op_LShiftVI,  vlen, bt) &&
             Matcher::match_rule_supported_vector(Op_URShiftVI, vlen, bt);
    case T_LONG:
      return Matcher::match_rule_supported_vector(Op_OrV,       vlen, bt) &&
             Matcher::match_rule_supported_vector(Op_LShiftVL,  vlen, bt) &&
             Matcher::match_rule_supported_vector(Op_URShiftVL, vlen, bt);
    default:
      assert(false, "not supported: %s", type2name(bt));
      return false;
  }
}

bool VectorNode::is_shift_opcode(int opc) {
  switch (opc) {
  case Op_LShiftI:
  case Op_LShiftL:
  case Op_RShiftI:
  case Op_RShiftL:
  case Op_URShiftB:
  case Op_URShiftS:
  case Op_URShiftI:
  case Op_URShiftL:
    return true;
  default:
    return false;
  }
}

bool VectorNode::is_shift(Node* n) {
  return is_shift_opcode(n->Opcode());
}

bool VectorNode::is_vshift_cnt(Node* n) {
  switch (n->Opcode()) {
  case Op_LShiftCntV:
  case Op_RShiftCntV:
    return true;
  default:
    return false;
  }
}

// Check if input is loop invariant vector.
bool VectorNode::is_invariant_vector(Node* n) {
  // Only Replicate vector nodes are loop invariant for now.
  switch (n->Opcode()) {
  case Op_ReplicateB:
  case Op_ReplicateS:
  case Op_ReplicateI:
  case Op_ReplicateL:
  case Op_ReplicateF:
  case Op_ReplicateD:
    return true;
  default:
    return false;
  }
}

// [Start, end) half-open range defining which operands are vectors
void VectorNode::vector_operands(Node* n, uint* start, uint* end) {
  switch (n->Opcode()) {
  case Op_LoadB:   case Op_LoadUB:
  case Op_LoadS:   case Op_LoadUS:
  case Op_LoadI:   case Op_LoadL:
  case Op_LoadF:   case Op_LoadD:
  case Op_LoadP:   case Op_LoadN:
    *start = 0;
    *end   = 0; // no vector operands
    break;
  case Op_StoreB:  case Op_StoreC:
  case Op_StoreI:  case Op_StoreL:
  case Op_StoreF:  case Op_StoreD:
  case Op_StoreP:  case Op_StoreN:
    *start = MemNode::ValueIn;
    *end   = MemNode::ValueIn + 1; // 1 vector operand
    break;
  case Op_LShiftI:  case Op_LShiftL:
  case Op_RShiftI:  case Op_RShiftL:
  case Op_URShiftI: case Op_URShiftL:
    *start = 1;
    *end   = 2; // 1 vector operand
    break;
  case Op_AddI: case Op_AddL: case Op_AddF: case Op_AddD:
  case Op_SubI: case Op_SubL: case Op_SubF: case Op_SubD:
  case Op_MulI: case Op_MulL: case Op_MulF: case Op_MulD:
  case Op_DivF: case Op_DivD:
  case Op_AndI: case Op_AndL:
  case Op_OrI:  case Op_OrL:
  case Op_XorI: case Op_XorL:
  case Op_MulAddS2I:
    *start = 1;
    *end   = 3; // 2 vector operands
    break;
  case Op_CMoveI:  case Op_CMoveL:  case Op_CMoveF:  case Op_CMoveD:
    *start = 2;
    *end   = n->req();
    break;
  case Op_FmaD:
  case Op_FmaF:
    *start = 1;
    *end   = 4; // 3 vector operands
    break;
  default:
    *start = 1;
    *end   = n->req(); // default is all operands
  }
}

// Make a vector node for binary operation
VectorNode* VectorNode::make(int vopc, Node* n1, Node* n2, const TypeVect* vt) {
  // This method should not be called for unimplemented vectors.
  guarantee(vopc > 0, "vopc must be > 0");
  switch (vopc) {
  case Op_AddVB: return new AddVBNode(n1, n2, vt);
  case Op_AddVS: return new AddVSNode(n1, n2, vt);
  case Op_AddVI: return new AddVINode(n1, n2, vt);
  case Op_AddVL: return new AddVLNode(n1, n2, vt);
  case Op_AddVF: return new AddVFNode(n1, n2, vt);
  case Op_AddVD: return new AddVDNode(n1, n2, vt);

  case Op_SubVB: return new SubVBNode(n1, n2, vt);
  case Op_SubVS: return new SubVSNode(n1, n2, vt);
  case Op_SubVI: return new SubVINode(n1, n2, vt);
  case Op_SubVL: return new SubVLNode(n1, n2, vt);
  case Op_SubVF: return new SubVFNode(n1, n2, vt);
  case Op_SubVD: return new SubVDNode(n1, n2, vt);

  case Op_MulVB: return new MulVBNode(n1, n2, vt);
  case Op_MulVS: return new MulVSNode(n1, n2, vt);
  case Op_MulVI: return new MulVINode(n1, n2, vt);
  case Op_MulVL: return new MulVLNode(n1, n2, vt);
  case Op_MulVF: return new MulVFNode(n1, n2, vt);
  case Op_MulVD: return new MulVDNode(n1, n2, vt);

  case Op_DivVF: return new DivVFNode(n1, n2, vt);
  case Op_DivVD: return new DivVDNode(n1, n2, vt);

  case Op_MinV: return new MinVNode(n1, n2, vt);
  case Op_MaxV: return new MaxVNode(n1, n2, vt);

  case Op_AbsVF: return new AbsVFNode(n1, vt);
  case Op_AbsVD: return new AbsVDNode(n1, vt);
  case Op_AbsVB: return new AbsVBNode(n1, vt);
  case Op_AbsVS: return new AbsVSNode(n1, vt);
  case Op_AbsVI: return new AbsVINode(n1, vt);
  case Op_AbsVL: return new AbsVLNode(n1, vt);

  case Op_NegVI: return new NegVINode(n1, vt);
  case Op_NegVF: return new NegVFNode(n1, vt);
  case Op_NegVD: return new NegVDNode(n1, vt);

  case Op_SqrtVF: return new SqrtVFNode(n1, vt);
  case Op_SqrtVD: return new SqrtVDNode(n1, vt);

  case Op_PopCountVI: return new PopCountVINode(n1, vt);
  case Op_RotateLeftV: return new RotateLeftVNode(n1, n2, vt);
  case Op_RotateRightV: return new RotateRightVNode(n1, n2, vt);

  case Op_LShiftVB: return new LShiftVBNode(n1, n2, vt);
  case Op_LShiftVS: return new LShiftVSNode(n1, n2, vt);
  case Op_LShiftVI: return new LShiftVINode(n1, n2, vt);
  case Op_LShiftVL: return new LShiftVLNode(n1, n2, vt);

  case Op_RShiftVB: return new RShiftVBNode(n1, n2, vt);
  case Op_RShiftVS: return new RShiftVSNode(n1, n2, vt);
  case Op_RShiftVI: return new RShiftVINode(n1, n2, vt);
  case Op_RShiftVL: return new RShiftVLNode(n1, n2, vt);

  case Op_URShiftVB: return new URShiftVBNode(n1, n2, vt);
  case Op_URShiftVS: return new URShiftVSNode(n1, n2, vt);
  case Op_URShiftVI: return new URShiftVINode(n1, n2, vt);
  case Op_URShiftVL: return new URShiftVLNode(n1, n2, vt);

  case Op_AndV: return new AndVNode(n1, n2, vt);
  case Op_OrV:  return new OrVNode (n1, n2, vt);
  case Op_XorV: return new XorVNode(n1, n2, vt);

  case Op_RoundDoubleModeV: return new RoundDoubleModeVNode(n1, n2, vt);

  case Op_MulAddVS2VI: return new MulAddVS2VINode(n1, n2, vt);
  default:
    fatal("Missed vector creation for '%s'", NodeClassNames[vopc]);
    return NULL;
  }
}

// Return the vector version of a scalar binary operation node.
VectorNode* VectorNode::make(int opc, Node* n1, Node* n2, uint vlen, BasicType bt) {
  const TypeVect* vt = TypeVect::make(bt, vlen);
  int vopc = VectorNode::opcode(opc, bt);
  // This method should not be called for unimplemented vectors.
  guarantee(vopc > 0, "Vector for '%s' is not implemented", NodeClassNames[opc]);
  return make(vopc, n1, n2, vt);
}

// Make a vector node for ternary operation
VectorNode* VectorNode::make(int vopc, Node* n1, Node* n2, Node* n3, const TypeVect* vt) {
  // This method should not be called for unimplemented vectors.
  guarantee(vopc > 0, "vopc must be > 0");
  switch (vopc) {
  case Op_FmaVD: return new FmaVDNode(n1, n2, n3, vt);
  case Op_FmaVF: return new FmaVFNode(n1, n2, n3, vt);
  default:
    fatal("Missed vector creation for '%s'", NodeClassNames[vopc]);
    return NULL;
  }
}

// Return the vector version of a scalar ternary operation node.
VectorNode* VectorNode::make(int opc, Node* n1, Node* n2, Node* n3, uint vlen, BasicType bt) {
  const TypeVect* vt = TypeVect::make(bt, vlen);
  int vopc = VectorNode::opcode(opc, bt);
  // This method should not be called for unimplemented vectors.
  guarantee(vopc > 0, "Vector for '%s' is not implemented", NodeClassNames[opc]);
  return make(vopc, n1, n2, n3, vt);
}

// Scalar promotion
VectorNode* VectorNode::scalar2vector(Node* s, uint vlen, const Type* opd_t) {
  BasicType bt = opd_t->array_element_basic_type();
  const TypeVect* vt = opd_t->singleton() ? TypeVect::make(opd_t, vlen)
                                          : TypeVect::make(bt, vlen);
  switch (bt) {
  case T_BOOLEAN:
  case T_BYTE:
    return new ReplicateBNode(s, vt);
  case T_CHAR:
  case T_SHORT:
    return new ReplicateSNode(s, vt);
  case T_INT:
    return new ReplicateINode(s, vt);
  case T_LONG:
    return new ReplicateLNode(s, vt);
  case T_FLOAT:
    return new ReplicateFNode(s, vt);
  case T_DOUBLE:
    return new ReplicateDNode(s, vt);
  default:
    fatal("Type '%s' is not supported for vectors", type2name(bt));
    return NULL;
  }
}

VectorNode* VectorNode::shift_count(int opc, Node* cnt, uint vlen, BasicType bt) {
  // Match shift count type with shift vector type.
  const TypeVect* vt = TypeVect::make(bt, vlen);
  switch (opc) {
  case Op_LShiftI:
  case Op_LShiftL:
    return new LShiftCntVNode(cnt, vt);
  case Op_RShiftI:
  case Op_RShiftL:
  case Op_URShiftB:
  case Op_URShiftS:
  case Op_URShiftI:
  case Op_URShiftL:
    return new RShiftCntVNode(cnt, vt);
  default:
    fatal("Missed vector creation for '%s'", NodeClassNames[opc]);
    return NULL;
  }
}

bool VectorNode::is_vector_shift(int opc) {
  assert(opc > _last_machine_leaf && opc < _last_opcode, "invalid opcode");
  switch (opc) {
  case Op_LShiftVB:
  case Op_LShiftVS:
  case Op_LShiftVI:
  case Op_LShiftVL:
  case Op_RShiftVB:
  case Op_RShiftVS:
  case Op_RShiftVI:
  case Op_RShiftVL:
  case Op_URShiftVB:
  case Op_URShiftVS:
  case Op_URShiftVI:
  case Op_URShiftVL:
    return true;
  default:
    return false;
  }
}

bool VectorNode::is_vector_shift_count(int opc) {
  assert(opc > _last_machine_leaf && opc < _last_opcode, "invalid opcode");
  switch (opc) {
  case Op_RShiftCntV:
  case Op_LShiftCntV:
    return true;
  default:
    return false;
  }
}

static bool is_con_M1(Node* n) {
  if (n->is_Con()) {
    const Type* t = n->bottom_type();
    if (t->isa_int() && t->is_int()->get_con() == -1) {
      return true;
    }
    if (t->isa_long() && t->is_long()->get_con() == -1) {
      return true;
    }
  }
  return false;
}

bool VectorNode::is_all_ones_vector(Node* n) {
  switch (n->Opcode()) {
  case Op_ReplicateB:
  case Op_ReplicateS:
  case Op_ReplicateI:
  case Op_ReplicateL:
    return is_con_M1(n->in(1));
  default:
    return false;
  }
}

bool VectorNode::is_vector_bitwise_not_pattern(Node* n) {
  if (n->Opcode() == Op_XorV) {
    return is_all_ones_vector(n->in(1)) ||
           is_all_ones_vector(n->in(2));
  }
  return false;
}

// Return initial Pack node. Additional operands added with add_opd() calls.
PackNode* PackNode::make(Node* s, uint vlen, BasicType bt) {
  const TypeVect* vt = TypeVect::make(bt, vlen);
  switch (bt) {
  case T_BOOLEAN:
  case T_BYTE:
    return new PackBNode(s, vt);
  case T_CHAR:
  case T_SHORT:
    return new PackSNode(s, vt);
  case T_INT:
    return new PackINode(s, vt);
  case T_LONG:
    return new PackLNode(s, vt);
  case T_FLOAT:
    return new PackFNode(s, vt);
  case T_DOUBLE:
    return new PackDNode(s, vt);
  default:
    fatal("Type '%s' is not supported for vectors", type2name(bt));
    return NULL;
  }
}

// Create a binary tree form for Packs. [lo, hi) (half-open) range
PackNode* PackNode::binary_tree_pack(int lo, int hi) {
  int ct = hi - lo;
  assert(is_power_of_2(ct), "power of 2");
  if (ct == 2) {
    PackNode* pk = PackNode::make(in(lo), 2, vect_type()->element_basic_type());
    pk->add_opd(in(lo+1));
    return pk;
  } else {
    int mid = lo + ct/2;
    PackNode* n1 = binary_tree_pack(lo,  mid);
    PackNode* n2 = binary_tree_pack(mid, hi );

    BasicType bt = n1->vect_type()->element_basic_type();
    assert(bt == n2->vect_type()->element_basic_type(), "should be the same");
    switch (bt) {
    case T_BOOLEAN:
    case T_BYTE:
      return new PackSNode(n1, n2, TypeVect::make(T_SHORT, 2));
    case T_CHAR:
    case T_SHORT:
      return new PackINode(n1, n2, TypeVect::make(T_INT, 2));
    case T_INT:
      return new PackLNode(n1, n2, TypeVect::make(T_LONG, 2));
    case T_LONG:
      return new Pack2LNode(n1, n2, TypeVect::make(T_LONG, 2));
    case T_FLOAT:
      return new PackDNode(n1, n2, TypeVect::make(T_DOUBLE, 2));
    case T_DOUBLE:
      return new Pack2DNode(n1, n2, TypeVect::make(T_DOUBLE, 2));
    default:
      fatal("Type '%s' is not supported for vectors", type2name(bt));
      return NULL;
    }
  }
}

// Return the vector version of a scalar load node.
LoadVectorNode* LoadVectorNode::make(int opc, Node* ctl, Node* mem,
                                     Node* adr, const TypePtr* atyp,
                                     uint vlen, BasicType bt,
                                     ControlDependency control_dependency) {
  const TypeVect* vt = TypeVect::make(bt, vlen);
  return new LoadVectorNode(ctl, mem, adr, atyp, vt, control_dependency);
}

// Return the vector version of a scalar store node.
StoreVectorNode* StoreVectorNode::make(int opc, Node* ctl, Node* mem,
                                       Node* adr, const TypePtr* atyp, Node* val,
                                       uint vlen) {
  return new StoreVectorNode(ctl, mem, adr, atyp, val);
}

Node* LoadVectorMaskedNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  if (!in(3)->is_top() && in(3)->Opcode() == Op_VectorMaskGen) {
    Node* mask_len = in(3)->in(1);
    const TypeLong* ty = phase->type(mask_len)->isa_long();
    if (ty && ty->is_con()) {
      BasicType mask_bt = ((VectorMaskGenNode*)in(3))->get_elem_type();
      uint load_sz      = type2aelembytes(mask_bt) * ty->get_con();
      if ( load_sz == 32 || load_sz == 64) {
        assert(load_sz == 32 || MaxVectorSize > 32, "Unexpected load size");
        Node* ctr = in(MemNode::Control);
        Node* mem = in(MemNode::Memory);
        Node* adr = in(MemNode::Address);
        return phase->transform(new LoadVectorNode(ctr, mem, adr, adr_type(), vect_type()));
      }
    }
  }
  return NULL;
}

Node* StoreVectorMaskedNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  if (!in(4)->is_top() && in(4)->Opcode() == Op_VectorMaskGen) {
    Node* mask_len = in(4)->in(1);
    const TypeLong* ty = phase->type(mask_len)->isa_long();
    if (ty && ty->is_con()) {
      BasicType mask_bt = ((VectorMaskGenNode*)in(4))->get_elem_type();
      uint load_sz      = type2aelembytes(mask_bt) * ty->get_con();
      if ( load_sz == 32 || load_sz == 64) {
        assert(load_sz == 32 || MaxVectorSize > 32, "Unexpected store size");
        Node* ctr = in(MemNode::Control);
        Node* mem = in(MemNode::Memory);
        Node* adr = in(MemNode::Address);
        Node* val = in(MemNode::ValueIn);
        return phase->transform(new StoreVectorNode(ctr, mem, adr, adr_type(), val));
      }
    }
  }
  return NULL;
}

int ExtractNode::opcode(BasicType bt) {
  switch (bt) {
    case T_BOOLEAN: return Op_ExtractUB;
    case T_BYTE:    return Op_ExtractB;
    case T_CHAR:    return Op_ExtractC;
    case T_SHORT:   return Op_ExtractS;
    case T_INT:     return Op_ExtractI;
    case T_LONG:    return Op_ExtractL;
    case T_FLOAT:   return Op_ExtractF;
    case T_DOUBLE:  return Op_ExtractD;
    default:
      assert(false, "wrong type: %s", type2name(bt));
      return 0;
  }
}

// Extract a scalar element of vector.
Node* ExtractNode::make(Node* v, uint position, BasicType bt) {
  assert((int)position < Matcher::max_vector_size(bt), "pos in range");
  ConINode* pos = ConINode::make((int)position);
  switch (bt) {
  case T_BOOLEAN: return new ExtractUBNode(v, pos);
  case T_BYTE:    return new ExtractBNode(v, pos);
  case T_CHAR:    return new ExtractCNode(v, pos);
  case T_SHORT:   return new ExtractSNode(v, pos);
  case T_INT:     return new ExtractINode(v, pos);
  case T_LONG:    return new ExtractLNode(v, pos);
  case T_FLOAT:   return new ExtractFNode(v, pos);
  case T_DOUBLE:  return new ExtractDNode(v, pos);
  default:
    assert(false, "wrong type: %s", type2name(bt));
    return NULL;
  }
}

int ReductionNode::opcode(int opc, BasicType bt) {
  int vopc = opc;
  switch (opc) {
    case Op_AddI:
      switch (bt) {
        case T_BOOLEAN:
        case T_CHAR: return 0;
        case T_BYTE:
        case T_SHORT:
        case T_INT:
          vopc = Op_AddReductionVI;
          break;
        default: ShouldNotReachHere(); return 0;
      }
      break;
    case Op_AddL:
      assert(bt == T_LONG, "must be");
      vopc = Op_AddReductionVL;
      break;
    case Op_AddF:
      assert(bt == T_FLOAT, "must be");
      vopc = Op_AddReductionVF;
      break;
    case Op_AddD:
      assert(bt == T_DOUBLE, "must be");
      vopc = Op_AddReductionVD;
      break;
    case Op_MulI:
      switch (bt) {
        case T_BOOLEAN:
        case T_CHAR: return 0;
        case T_BYTE:
        case T_SHORT:
        case T_INT:
          vopc = Op_MulReductionVI;
          break;
        default: ShouldNotReachHere(); return 0;
      }
      break;
    case Op_MulL:
      assert(bt == T_LONG, "must be");
      vopc = Op_MulReductionVL;
      break;
    case Op_MulF:
      assert(bt == T_FLOAT, "must be");
      vopc = Op_MulReductionVF;
      break;
    case Op_MulD:
      assert(bt == T_DOUBLE, "must be");
      vopc = Op_MulReductionVD;
      break;
    case Op_MinI:
      switch (bt) {
        case T_BOOLEAN:
        case T_CHAR: return 0;
        case T_BYTE:
        case T_SHORT:
        case T_INT:
          vopc = Op_MinReductionV;
          break;
        default: ShouldNotReachHere(); return 0;
      }
      break;
    case Op_MinL:
      assert(bt == T_LONG, "must be");
      vopc = Op_MinReductionV;
      break;
    case Op_MinF:
      assert(bt == T_FLOAT, "must be");
      vopc = Op_MinReductionV;
      break;
    case Op_MinD:
      assert(bt == T_DOUBLE, "must be");
      vopc = Op_MinReductionV;
      break;
    case Op_MaxI:
      switch (bt) {
        case T_BOOLEAN:
        case T_CHAR: return 0;
        case T_BYTE:
        case T_SHORT:
        case T_INT:
          vopc = Op_MaxReductionV;
          break;
        default: ShouldNotReachHere(); return 0;
      }
      break;
    case Op_MaxL:
      assert(bt == T_LONG, "must be");
      vopc = Op_MaxReductionV;
      break;
    case Op_MaxF:
      assert(bt == T_FLOAT, "must be");
      vopc = Op_MaxReductionV;
      break;
    case Op_MaxD:
      assert(bt == T_DOUBLE, "must be");
      vopc = Op_MaxReductionV;
      break;
    case Op_AndI:
      switch (bt) {
      case T_BOOLEAN:
      case T_CHAR: return 0;
      case T_BYTE:
      case T_SHORT:
      case T_INT:
        vopc = Op_AndReductionV;
        break;
      default: ShouldNotReachHere(); return 0;
      }
      break;
    case Op_AndL:
      assert(bt == T_LONG, "must be");
      vopc = Op_AndReductionV;
      break;
    case Op_OrI:
      switch(bt) {
      case T_BOOLEAN:
      case T_CHAR: return 0;
      case T_BYTE:
      case T_SHORT:
      case T_INT:
        vopc = Op_OrReductionV;
        break;
      default: ShouldNotReachHere(); return 0;
      }
      break;
    case Op_OrL:
      assert(bt == T_LONG, "must be");
      vopc = Op_OrReductionV;
      break;
    case Op_XorI:
      switch(bt) {
      case T_BOOLEAN:
      case T_CHAR: return 0;
      case T_BYTE:
      case T_SHORT:
      case T_INT:
        vopc = Op_XorReductionV;
        break;
      default: ShouldNotReachHere(); return 0;
      }
      break;
    case Op_XorL:
      assert(bt == T_LONG, "must be");
      vopc = Op_XorReductionV;
      break;
    default:
      break;
  }
  return vopc;
}

// Return the appropriate reduction node.
ReductionNode* ReductionNode::make(int opc, Node *ctrl, Node* n1, Node* n2, BasicType bt) {

  int vopc = opcode(opc, bt);

  // This method should not be called for unimplemented vectors.
  guarantee(vopc != opc, "Vector for '%s' is not implemented", NodeClassNames[opc]);

  switch (vopc) {
  case Op_AddReductionVI: return new AddReductionVINode(ctrl, n1, n2);
  case Op_AddReductionVL: return new AddReductionVLNode(ctrl, n1, n2);
  case Op_AddReductionVF: return new AddReductionVFNode(ctrl, n1, n2);
  case Op_AddReductionVD: return new AddReductionVDNode(ctrl, n1, n2);
  case Op_MulReductionVI: return new MulReductionVINode(ctrl, n1, n2);
  case Op_MulReductionVL: return new MulReductionVLNode(ctrl, n1, n2);
  case Op_MulReductionVF: return new MulReductionVFNode(ctrl, n1, n2);
  case Op_MulReductionVD: return new MulReductionVDNode(ctrl, n1, n2);
  case Op_MinReductionV:  return new MinReductionVNode(ctrl, n1, n2);
  case Op_MaxReductionV:  return new MaxReductionVNode(ctrl, n1, n2);
  case Op_AndReductionV:  return new AndReductionVNode(ctrl, n1, n2);
  case Op_OrReductionV:   return new OrReductionVNode(ctrl, n1, n2);
  case Op_XorReductionV:  return new XorReductionVNode(ctrl, n1, n2);
  default:
    assert(false, "unknown node: %s", NodeClassNames[vopc]);
    return NULL;
  }
}

Node* VectorLoadMaskNode::Identity(PhaseGVN* phase) {
  BasicType out_bt = type()->is_vect()->element_basic_type();
  if (out_bt == T_BOOLEAN) {
    return in(1); // redundant conversion
  }
  return this;
}

Node* VectorStoreMaskNode::Identity(PhaseGVN* phase) {
  // Identity transformation on boolean vectors.
  //   VectorStoreMask (VectorLoadMask bv) elem_size ==> bv
  //   vector[n]{bool} => vector[n]{t} => vector[n]{bool}
  if (in(1)->Opcode() == Op_VectorLoadMask) {
    return in(1)->in(1);
  }
  return this;
}

VectorStoreMaskNode* VectorStoreMaskNode::make(PhaseGVN& gvn, Node* in, BasicType in_type, uint num_elem) {
  assert(in->bottom_type()->isa_vect(), "sanity");
  const TypeVect* vt = TypeVect::make(T_BOOLEAN, num_elem);
  int elem_size = type2aelembytes(in_type);
  return new VectorStoreMaskNode(in, gvn.intcon(elem_size), vt);
}

VectorCastNode* VectorCastNode::make(int vopc, Node* n1, BasicType bt, uint vlen) {
  const TypeVect* vt = TypeVect::make(bt, vlen);
  switch (vopc) {
    case Op_VectorCastB2X: return new VectorCastB2XNode(n1, vt);
    case Op_VectorCastS2X: return new VectorCastS2XNode(n1, vt);
    case Op_VectorCastI2X: return new VectorCastI2XNode(n1, vt);
    case Op_VectorCastL2X: return new VectorCastL2XNode(n1, vt);
    case Op_VectorCastF2X: return new VectorCastF2XNode(n1, vt);
    case Op_VectorCastD2X: return new VectorCastD2XNode(n1, vt);
    default:
      assert(false, "unknown node: %s", NodeClassNames[vopc]);
      return NULL;
  }
}

int VectorCastNode::opcode(BasicType bt) {
  switch (bt) {
    case T_BYTE:   return Op_VectorCastB2X;
    case T_SHORT:  return Op_VectorCastS2X;
    case T_INT:    return Op_VectorCastI2X;
    case T_LONG:   return Op_VectorCastL2X;
    case T_FLOAT:  return Op_VectorCastF2X;
    case T_DOUBLE: return Op_VectorCastD2X;
    default:
      assert(false, "unknown type: %s", type2name(bt));
      return 0;
  }
}

Node* VectorCastNode::Identity(PhaseGVN* phase) {
  if (!in(1)->is_top()) {
    BasicType  in_bt = in(1)->bottom_type()->is_vect()->element_basic_type();
    BasicType out_bt = vect_type()->element_basic_type();
    if (in_bt == out_bt) {
      return in(1); // redundant cast
    }
  }
  return this;
}

Node* ReductionNode::make_reduction_input(PhaseGVN& gvn, int opc, BasicType bt) {
  int vopc = opcode(opc, bt);
  guarantee(vopc != opc, "Vector reduction for '%s' is not implemented", NodeClassNames[opc]);

  switch (vopc) {
    case Op_AndReductionV:
      switch (bt) {
        case T_BYTE:
        case T_SHORT:
        case T_INT:
          return gvn.makecon(TypeInt::MINUS_1);
        case T_LONG:
          return gvn.makecon(TypeLong::MINUS_1);
        default:
          fatal("Missed vector creation for '%s' as the basic type is not correct.", NodeClassNames[vopc]);
          return NULL;
      }
      break;
    case Op_AddReductionVI: // fallthrough
    case Op_AddReductionVL: // fallthrough
    case Op_AddReductionVF: // fallthrough
    case Op_AddReductionVD:
    case Op_OrReductionV:
    case Op_XorReductionV:
      return gvn.zerocon(bt);
    case Op_MulReductionVI:
      return gvn.makecon(TypeInt::ONE);
    case Op_MulReductionVL:
      return gvn.makecon(TypeLong::ONE);
    case Op_MulReductionVF:
      return gvn.makecon(TypeF::ONE);
    case Op_MulReductionVD:
      return gvn.makecon(TypeD::ONE);
    case Op_MinReductionV:
      switch (bt) {
        case T_BYTE:
        case T_SHORT:
        case T_INT:
          return gvn.makecon(TypeInt::MAX);
        case T_LONG:
          return gvn.makecon(TypeLong::MAX);
        case T_FLOAT:
          return gvn.makecon(TypeF::POS_INF);
        case T_DOUBLE:
          return gvn.makecon(TypeD::POS_INF);
          default: Unimplemented(); return NULL;
      }
      break;
    case Op_MaxReductionV:
      switch (bt) {
        case T_BYTE:
        case T_SHORT:
        case T_INT:
          return gvn.makecon(TypeInt::MIN);
        case T_LONG:
          return gvn.makecon(TypeLong::MIN);
        case T_FLOAT:
          return gvn.makecon(TypeF::NEG_INF);
        case T_DOUBLE:
          return gvn.makecon(TypeD::NEG_INF);
          default: Unimplemented(); return NULL;
      }
      break;
    default:
      fatal("Missed vector creation for '%s'", NodeClassNames[vopc]);
      return NULL;
  }
}

bool ReductionNode::implemented(int opc, uint vlen, BasicType bt) {
  if (is_java_primitive(bt) &&
      (vlen > 1) && is_power_of_2(vlen) &&
      Matcher::vector_size_supported(bt, vlen)) {
    int vopc = ReductionNode::opcode(opc, bt);
    return vopc != opc && Matcher::match_rule_supported_vector(vopc, vlen, bt);
  }
  return false;
}

MacroLogicVNode* MacroLogicVNode::make(PhaseGVN& gvn, Node* in1, Node* in2, Node* in3,
                                       uint truth_table, const TypeVect* vt) {
  assert(truth_table <= 0xFF, "invalid");
  assert(in1->bottom_type()->is_vect()->length_in_bytes() == vt->length_in_bytes(), "mismatch");
  assert(in2->bottom_type()->is_vect()->length_in_bytes() == vt->length_in_bytes(), "mismatch");
  assert(in3->bottom_type()->is_vect()->length_in_bytes() == vt->length_in_bytes(), "mismatch");
  Node* fn = gvn.intcon(truth_table);
  return new MacroLogicVNode(in1, in2, in3, fn, vt);
}

Node* VectorNode::degenerate_vector_rotate(Node* src, Node* cnt, bool is_rotate_left,
                                           int vlen, BasicType bt, PhaseGVN* phase) {
  assert(bt == T_INT || bt == T_LONG, "sanity");
  const TypeVect* vt = TypeVect::make(bt, vlen);

  int shift_mask = (bt == T_INT) ? 0x1F : 0x3F;
  int shiftLOpc = (bt == T_INT) ? Op_LShiftI : Op_LShiftL;
  int shiftROpc = (bt == T_INT) ? Op_URShiftI: Op_URShiftL;

  // Compute shift values for right rotation and
  // later swap them in case of left rotation.
  Node* shiftRCnt = NULL;
  Node* shiftLCnt = NULL;
  const TypeInt* cnt_type = cnt->bottom_type()->isa_int();
  bool is_binary_vector_op = false;
  if (cnt_type && cnt_type->is_con()) {
    // Constant shift.
    int shift = cnt_type->get_con() & shift_mask;
    shiftRCnt = phase->intcon(shift);
    shiftLCnt = phase->intcon(shift_mask + 1 - shift);
  } else if (VectorNode::is_invariant_vector(cnt)) {
    // Scalar variable shift, handle replicates generated by auto vectorizer.
    cnt = cnt->in(1);
    if (bt == T_LONG) {
      // Shift count vector for Rotate vector has long elements too.
      if (cnt->Opcode() == Op_ConvI2L) {
         cnt = cnt->in(1);
      } else {
         assert(cnt->bottom_type()->isa_long() &&
                cnt->bottom_type()->is_long()->is_con(), "Long constant expected");
         cnt = phase->transform(new ConvL2INode(cnt));
      }
    }
    shiftRCnt = phase->transform(new AndINode(cnt, phase->intcon(shift_mask)));
    shiftLCnt = phase->transform(new SubINode(phase->intcon(shift_mask + 1), shiftRCnt));
  } else {
    // Vector variable shift.
    assert(Matcher::supports_vector_variable_shifts(), "");
    assert(bt == T_INT, "Variable vector case supported for integer type rotation");

    assert(cnt->bottom_type()->isa_vect(), "Unexpected shift");
    const Type* elem_ty = Type::get_const_basic_type(bt);

    Node* shift_mask_node = phase->intcon(shift_mask);
    Node* const_one_node = phase->intcon(1);

    int subVopc = VectorNode::opcode(Op_SubI, bt);
    int addVopc = VectorNode::opcode(Op_AddI, bt);

    Node* vector_mask = phase->transform(VectorNode::scalar2vector(shift_mask_node, vlen, elem_ty));
    Node* vector_one = phase->transform(VectorNode::scalar2vector(const_one_node, vlen, elem_ty));

    shiftRCnt = cnt;
    shiftRCnt = phase->transform(VectorNode::make(Op_AndV, shiftRCnt, vector_mask, vt));
    vector_mask = phase->transform(VectorNode::make(addVopc, vector_one, vector_mask, vt));
    shiftLCnt = phase->transform(VectorNode::make(subVopc, vector_mask, shiftRCnt, vt));
    is_binary_vector_op = true;
  }

  // Swap the computed left and right shift counts.
  if (is_rotate_left) {
    swap(shiftRCnt,shiftLCnt);
  }

  if (!is_binary_vector_op) {
    shiftLCnt = phase->transform(new LShiftCntVNode(shiftLCnt, vt));
    shiftRCnt = phase->transform(new RShiftCntVNode(shiftRCnt, vt));
  }

  return new OrVNode(phase->transform(VectorNode::make(shiftLOpc, src, shiftLCnt, vlen, bt)),
                     phase->transform(VectorNode::make(shiftROpc, src, shiftRCnt, vlen, bt)),
                     vt);
}

Node* RotateLeftVNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  int vlen = length();
  BasicType bt = vect_type()->element_basic_type();
  if ((!in(2)->is_Con() && !Matcher::supports_vector_variable_rotates()) ||
       !Matcher::match_rule_supported_vector(Op_RotateLeftV, vlen, bt)) {
    return VectorNode::degenerate_vector_rotate(in(1), in(2), true, vlen, bt, phase);
  }
  return NULL;
}

Node* RotateRightVNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  int vlen = length();
  BasicType bt = vect_type()->element_basic_type();
  if ((!in(2)->is_Con() && !Matcher::supports_vector_variable_rotates()) ||
       !Matcher::match_rule_supported_vector(Op_RotateRightV, vlen, bt)) {
    return VectorNode::degenerate_vector_rotate(in(1), in(2), false, vlen, bt, phase);
  }
  return NULL;
}

#ifndef PRODUCT
void VectorMaskCmpNode::dump_spec(outputStream *st) const {
  st->print(" %d #", _predicate); _type->dump_on(st);
}
#endif // PRODUCT

Node* VectorReinterpretNode::Identity(PhaseGVN *phase) {
  Node* n = in(1);
  if (n->Opcode() == Op_VectorReinterpret) {
    // "VectorReinterpret (VectorReinterpret node) ==> node" if:
    //   1) Types of 'node' and 'this' are identical
    //   2) Truncations are not introduced by the first VectorReinterpret
    if (Type::cmp(bottom_type(), n->in(1)->bottom_type()) == 0 &&
        length_in_bytes() <= n->bottom_type()->is_vect()->length_in_bytes()) {
      return n->in(1);
    }
  }
  return this;
}

Node* VectorInsertNode::make(Node* vec, Node* new_val, int position) {
  assert(position < (int)vec->bottom_type()->is_vect()->length(), "pos in range");
  ConINode* pos = ConINode::make(position);
  return new VectorInsertNode(vec, new_val, pos, vec->bottom_type()->is_vect());
}

Node* VectorUnboxNode::Ideal(PhaseGVN* phase, bool can_reshape) {
  Node* n = obj()->uncast();
  if (EnableVectorReboxing && n->Opcode() == Op_VectorBox) {
    if (Type::cmp(bottom_type(), n->in(VectorBoxNode::Value)->bottom_type()) == 0) {
      // Handled by VectorUnboxNode::Identity()
    } else {
      VectorBoxNode* vbox = static_cast<VectorBoxNode*>(n);
      ciKlass* vbox_klass = vbox->box_type()->klass();
      const TypeVect* in_vt = vbox->vec_type();
      const TypeVect* out_vt = type()->is_vect();

      if (in_vt->length() == out_vt->length()) {
        Node* value = vbox->in(VectorBoxNode::Value);

        bool is_vector_mask    = vbox_klass->is_subclass_of(ciEnv::current()->vector_VectorMask_klass());
        bool is_vector_shuffle = vbox_klass->is_subclass_of(ciEnv::current()->vector_VectorShuffle_klass());
        if (is_vector_mask) {
          if (in_vt->length_in_bytes() == out_vt->length_in_bytes() &&
              Matcher::match_rule_supported_vector(Op_VectorMaskCast, out_vt->length(), out_vt->element_basic_type())) {
            // Apply "VectorUnbox (VectorBox vmask) ==> VectorMaskCast (vmask)"
            // directly. This could avoid the transformation ordering issue from
            // "VectorStoreMask (VectorLoadMask vmask) => vmask".
            return new VectorMaskCastNode(value, out_vt);
          }
          // VectorUnbox (VectorBox vmask) ==> VectorLoadMask (VectorStoreMask vmask)
          value = phase->transform(VectorStoreMaskNode::make(*phase, value, in_vt->element_basic_type(), in_vt->length()));
          return new VectorLoadMaskNode(value, out_vt);
        } else if (is_vector_shuffle) {
          if (!is_shuffle_to_vector()) {
            // VectorUnbox (VectorBox vshuffle) ==> VectorLoadShuffle vshuffle
            return new VectorLoadShuffleNode(value, out_vt);
          }
        } else {
          // Vector type mismatch is only supported for masks and shuffles, but sometimes it happens in pathological cases.
        }
      } else {
        // Vector length mismatch.
        // Sometimes happen in pathological cases (e.g., when unboxing happens in effectively dead code).
      }
    }
  }
  return NULL;
}

Node* VectorUnboxNode::Identity(PhaseGVN* phase) {
  Node* n = obj()->uncast();
  if (EnableVectorReboxing && n->Opcode() == Op_VectorBox) {
    if (Type::cmp(bottom_type(), n->in(VectorBoxNode::Value)->bottom_type()) == 0) {
      return n->in(VectorBoxNode::Value); // VectorUnbox (VectorBox v) ==> v
    } else {
      // Handled by VectorUnboxNode::Ideal().
    }
  }
  return this;
}

const TypeFunc* VectorBoxNode::vec_box_type(const TypeInstPtr* box_type) {
  const Type** fields = TypeTuple::fields(0);
  const TypeTuple *domain = TypeTuple::make(TypeFunc::Parms, fields);

  fields = TypeTuple::fields(1);
  fields[TypeFunc::Parms+0] = box_type;
  const TypeTuple *range = TypeTuple::make(TypeFunc::Parms+1, fields);

  return TypeFunc::make(domain, range);
}

Node* ShiftVNode::Identity(PhaseGVN* phase) {
  Node* in2 = in(2);
  // Shift by ZERO does nothing
  if (is_vshift_cnt(in2) && phase->find_int_type(in2->in(1)) == TypeInt::ZERO) {
    return in(1);
  }
  return this;
}

Node* VectorMaskOpNode::make(Node* mask, const Type* ty, int mopc) {
  switch(mopc) {
    case Op_VectorMaskTrueCount:
      return new VectorMaskTrueCountNode(mask, ty);
    case Op_VectorMaskLastTrue:
      return new VectorMaskLastTrueNode(mask, ty);
    case Op_VectorMaskFirstTrue:
      return new VectorMaskFirstTrueNode(mask, ty);
    default:
      assert(false, "Unhandled operation");
  }
  return NULL;
}


#ifndef PRODUCT
void VectorBoxAllocateNode::dump_spec(outputStream *st) const {
  CallStaticJavaNode::dump_spec(st);
}

#endif // !PRODUCT
