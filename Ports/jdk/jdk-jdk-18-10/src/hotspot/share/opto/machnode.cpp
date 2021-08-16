/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/collectedHeap.hpp"
#include "memory/universe.hpp"
#include "oops/compressedOops.hpp"
#include "opto/machnode.hpp"
#include "opto/output.hpp"
#include "opto/regalloc.hpp"
#include "utilities/vmError.hpp"

//=============================================================================
// Return the value requested
// result register lookup, corresponding to int_format
int MachOper::reg(PhaseRegAlloc *ra_, const Node *node) const {
  return (int)ra_->get_encode(node);
}
// input register lookup, corresponding to ext_format
int MachOper::reg(PhaseRegAlloc *ra_, const Node *node, int idx) const {
  return (int)(ra_->get_encode(node->in(idx)));
}
intptr_t  MachOper::constant() const { return 0x00; }
relocInfo::relocType MachOper::constant_reloc() const { return relocInfo::none; }
jdouble MachOper::constantD() const { ShouldNotReachHere(); return 0.0; }
jfloat  MachOper::constantF() const { ShouldNotReachHere(); return 0.0; }
jlong   MachOper::constantL() const { ShouldNotReachHere(); return CONST64(0) ; }
TypeOopPtr *MachOper::oop() const { return NULL; }
int MachOper::ccode() const { return 0x00; }
// A zero, default, indicates this value is not needed.
// May need to lookup the base register, as done in int_ and ext_format
int MachOper::base (PhaseRegAlloc *ra_, const Node *node, int idx)  const { return 0x00; }
int MachOper::index(PhaseRegAlloc *ra_, const Node *node, int idx)  const { return 0x00; }
int MachOper::scale()  const { return 0x00; }
int MachOper::disp (PhaseRegAlloc *ra_, const Node *node, int idx)  const { return 0x00; }
int MachOper::constant_disp()  const { return 0; }
int MachOper::base_position()  const { return -1; }  // no base input
int MachOper::index_position() const { return -1; }  // no index input
// Check for PC-Relative displacement
relocInfo::relocType MachOper::disp_reloc() const { return relocInfo::none; }
// Return the label
Label*   MachOper::label()  const { ShouldNotReachHere(); return 0; }
intptr_t MachOper::method() const { ShouldNotReachHere(); return 0; }


//------------------------------negate-----------------------------------------
// Negate conditional branches.  Error for non-branch operands
void MachOper::negate() {
  ShouldNotCallThis();
}

//-----------------------------type--------------------------------------------
const Type *MachOper::type() const {
  return Type::BOTTOM;
}

//------------------------------in_RegMask-------------------------------------
const RegMask *MachOper::in_RegMask(int index) const {
  ShouldNotReachHere();
  return NULL;
}

//------------------------------dump_spec--------------------------------------
// Print any per-operand special info
#ifndef PRODUCT
void MachOper::dump_spec(outputStream *st) const { }
#endif

//------------------------------hash-------------------------------------------
// Print any per-operand special info
uint MachOper::hash() const {
  ShouldNotCallThis();
  return 5;
}

//------------------------------cmp--------------------------------------------
// Print any per-operand special info
bool MachOper::cmp( const MachOper &oper ) const {
  ShouldNotCallThis();
  return opcode() == oper.opcode();
}

//------------------------------hash-------------------------------------------
// Print any per-operand special info
uint labelOper::hash() const {
  return _block_num;
}

//------------------------------cmp--------------------------------------------
// Print any per-operand special info
bool labelOper::cmp( const MachOper &oper ) const {
  return (opcode() == oper.opcode()) && (_label == oper.label());
}

//------------------------------hash-------------------------------------------
// Print any per-operand special info
uint methodOper::hash() const {
  return (uint)_method;
}

//------------------------------cmp--------------------------------------------
// Print any per-operand special info
bool methodOper::cmp( const MachOper &oper ) const {
  return (opcode() == oper.opcode()) && (_method == oper.method());
}


//=============================================================================
//------------------------------MachNode---------------------------------------

//------------------------------emit-------------------------------------------
void MachNode::emit(CodeBuffer &cbuf, PhaseRegAlloc *ra_) const {
  #ifdef ASSERT
  tty->print("missing MachNode emit function: ");
  dump();
  #endif
  ShouldNotCallThis();
}

//---------------------------postalloc_expand----------------------------------
// Expand node after register allocation.
void MachNode::postalloc_expand(GrowableArray <Node *> *nodes, PhaseRegAlloc *ra_) {}

//------------------------------size-------------------------------------------
// Size of instruction in bytes
uint MachNode::size(PhaseRegAlloc *ra_) const {
  // If a virtual was not defined for this specific instruction,
  // Call the helper which finds the size by emitting the bits.
  return MachNode::emit_size(ra_);
}

//------------------------------size-------------------------------------------
// Helper function that computes size by emitting code
uint MachNode::emit_size(PhaseRegAlloc *ra_) const {
  // Emit into a trash buffer and count bytes emitted.
  assert(ra_ == ra_->C->regalloc(), "sanity");
  return ra_->C->output()->scratch_emit_size(this);
}



//------------------------------hash-------------------------------------------
uint MachNode::hash() const {
  uint no = num_opnds();
  uint sum = rule();
  for( uint i=0; i<no; i++ )
    sum += _opnds[i]->hash();
  return sum+Node::hash();
}

//-----------------------------cmp---------------------------------------------
bool MachNode::cmp( const Node &node ) const {
  MachNode& n = *((Node&)node).as_Mach();
  uint no = num_opnds();
  if( no != n.num_opnds() ) return false;
  if( rule() != n.rule() ) return false;
  for( uint i=0; i<no; i++ )    // All operands must match
    if( !_opnds[i]->cmp( *n._opnds[i] ) )
      return false;             // mis-matched operands
  return true;                  // match
}

// Return an equivalent instruction using memory for cisc_operand position
MachNode *MachNode::cisc_version(int offset) {
  ShouldNotCallThis();
  return NULL;
}

void MachNode::use_cisc_RegMask() {
  ShouldNotReachHere();
}


//-----------------------------in_RegMask--------------------------------------
const RegMask &MachNode::in_RegMask( uint idx ) const {
  uint numopnds = num_opnds();        // Virtual call for number of operands
  uint skipped   = oper_input_base(); // Sum of leaves skipped so far
  if( idx < skipped ) {
    assert( ideal_Opcode() == Op_AddP, "expected base ptr here" );
    assert( idx == 1, "expected base ptr here" );
    // debug info can be anywhere
    return *Compile::current()->matcher()->idealreg2spillmask[Op_RegP];
  }
  uint opcnt     = 1;                 // First operand
  uint num_edges = _opnds[1]->num_edges(); // leaves for first operand
  while( idx >= skipped+num_edges ) {
    skipped += num_edges;
    opcnt++;                          // Bump operand count
    assert( opcnt < numopnds, "Accessing non-existent operand" );
    num_edges = _opnds[opcnt]->num_edges(); // leaves for next operand
  }

  const RegMask *rm = cisc_RegMask();
  if( rm == NULL || (int)opcnt != cisc_operand() ) {
    rm = _opnds[opcnt]->in_RegMask(idx-skipped);
  }
  return *rm;
}

//-----------------------------memory_inputs--------------------------------
const MachOper*  MachNode::memory_inputs(Node* &base, Node* &index) const {
  const MachOper* oper = memory_operand();

  if (oper == (MachOper*)-1) {
    base = NodeSentinel;
    index = NodeSentinel;
  } else {
    base = NULL;
    index = NULL;
    if (oper != NULL) {
      // It has a unique memory operand.  Find its index.
      int oper_idx = num_opnds();
      while (--oper_idx >= 0) {
        if (_opnds[oper_idx] == oper)  break;
      }
      int oper_pos = operand_index(oper_idx);
      int base_pos = oper->base_position();
      if (base_pos >= 0) {
        base = _in[oper_pos+base_pos];
      }
      int index_pos = oper->index_position();
      if (index_pos >= 0) {
        index = _in[oper_pos+index_pos];
      }
    }
  }

  return oper;
}

//-----------------------------get_base_and_disp----------------------------
const Node* MachNode::get_base_and_disp(intptr_t &offset, const TypePtr* &adr_type) const {

  // Find the memory inputs using our helper function
  Node* base;
  Node* index;
  const MachOper* oper = memory_inputs(base, index);

  if (oper == NULL) {
    // Base has been set to NULL
    offset = 0;
  } else if (oper == (MachOper*)-1) {
    // Base has been set to NodeSentinel
    // There is not a unique memory use here.  We will fall to AliasIdxBot.
    offset = Type::OffsetBot;
  } else {
    // Base may be NULL, even if offset turns out to be != 0

    intptr_t disp = oper->constant_disp();
    int scale = oper->scale();
    // Now we have collected every part of the ADLC MEMORY_INTER.
    // See if it adds up to a base + offset.
    if (index != NULL) {
      const Type* t_index = index->bottom_type();
      if (t_index->isa_narrowoop() || t_index->isa_narrowklass()) { // EncodeN, LoadN, LoadConN, LoadNKlass,
                                                                    // EncodeNKlass, LoadConNklass.
        // Memory references through narrow oops have a
        // funny base so grab the type from the index:
        // [R12 + narrow_oop_reg<<3 + offset]
        assert(base == NULL, "Memory references through narrow oops have no base");
        offset = disp;
        adr_type = t_index->make_ptr()->add_offset(offset);
        return NULL;
      } else if (!index->is_Con()) {
        disp = Type::OffsetBot;
      } else if (disp != Type::OffsetBot) {
        const TypeX* ti = t_index->isa_intptr_t();
        if (ti == NULL) {
          disp = Type::OffsetBot;  // a random constant??
        } else {
          disp += ti->get_con() << scale;
        }
      }
    }
    offset = disp;

    // In x86_32.ad, indOffset32X uses base==RegI and disp==RegP,
    // this will prevent alias analysis without the following support:
    // Lookup the TypePtr used by indOffset32X, a compile-time constant oop,
    // Add the offset determined by the "base", or use Type::OffsetBot.
    if( adr_type == TYPE_PTR_SENTINAL ) {
      const TypePtr *t_disp = oper->disp_as_type();  // only !NULL for indOffset32X
      if (t_disp != NULL) {
        offset = Type::OffsetBot;
        const Type* t_base = base->bottom_type();
        if (t_base->isa_intptr_t()) {
          const TypeX *t_offset = t_base->is_intptr_t();
          if( t_offset->is_con() ) {
            offset = t_offset->get_con();
          }
        }
        adr_type = t_disp->add_offset(offset);
      } else if( base == NULL && offset != 0 && offset != Type::OffsetBot ) {
        // Use ideal type if it is oop ptr.
        const TypePtr *tp = oper->type()->isa_ptr();
        if( tp != NULL) {
          adr_type = tp;
        }
      }
    }

  }
  return base;
}


//---------------------------------adr_type---------------------------------
const class TypePtr *MachNode::adr_type() const {
  intptr_t offset = 0;
  const TypePtr *adr_type = TYPE_PTR_SENTINAL;  // attempt computing adr_type
  const Node *base = get_base_and_disp(offset, adr_type);
  if( adr_type != TYPE_PTR_SENTINAL ) {
    return adr_type;      // get_base_and_disp has the answer
  }

  // Direct addressing modes have no base node, simply an indirect
  // offset, which is always to raw memory.
  // %%%%% Someday we'd like to allow constant oop offsets which
  // would let Intel load from static globals in 1 instruction.
  // Currently Intel requires 2 instructions and a register temp.
  if (base == NULL) {
    // NULL base, zero offset means no memory at all (a null pointer!)
    if (offset == 0) {
      return NULL;
    }
    // NULL base, any offset means any pointer whatever
    if (offset == Type::OffsetBot) {
      return TypePtr::BOTTOM;
    }
    // %%% make offset be intptr_t
    assert(!Universe::heap()->is_in(cast_to_oop(offset)), "must be a raw ptr");
    return TypeRawPtr::BOTTOM;
  }

  // base of -1 with no particular offset means all of memory
  if (base == NodeSentinel)  return TypePtr::BOTTOM;

  const Type* t = base->bottom_type();
  if (t->isa_narrowoop() && CompressedOops::shift() == 0) {
    // 32-bit unscaled narrow oop can be the base of any address expression
    t = t->make_ptr();
  }
  if (t->isa_narrowklass() && CompressedKlassPointers::shift() == 0) {
    // 32-bit unscaled narrow oop can be the base of any address expression
    t = t->make_ptr();
  }
  if (t->isa_intptr_t() && offset != 0 && offset != Type::OffsetBot) {
    // We cannot assert that the offset does not look oop-ish here.
    // Depending on the heap layout the cardmark base could land
    // inside some oopish region.  It definitely does for Win2K.
    // The sum of cardmark-base plus shift-by-9-oop lands outside
    // the oop-ish area but we can't assert for that statically.
    return TypeRawPtr::BOTTOM;
  }

  const TypePtr *tp = t->isa_ptr();

  // be conservative if we do not recognize the type
  if (tp == NULL) {
    assert(false, "this path may produce not optimal code");
    return TypePtr::BOTTOM;
  }
  assert(tp->base() != Type::AnyPtr, "not a bare pointer");

  return tp->add_offset(offset);
}


//-----------------------------operand_index---------------------------------
int MachNode::operand_index(uint operand) const {
  if (operand < 1)  return -1;
  assert(operand < num_opnds(), "oob");
  if (_opnds[operand]->num_edges() == 0)  return -1;

  uint skipped   = oper_input_base(); // Sum of leaves skipped so far
  for (uint opcnt = 1; opcnt < operand; opcnt++) {
    uint num_edges = _opnds[opcnt]->num_edges(); // leaves for operand
    skipped += num_edges;
  }
  return skipped;
}

int MachNode::operand_index(const MachOper *oper) const {
  uint skipped = oper_input_base(); // Sum of leaves skipped so far
  uint opcnt;
  for (opcnt = 1; opcnt < num_opnds(); opcnt++) {
    if (_opnds[opcnt] == oper) break;
    uint num_edges = _opnds[opcnt]->num_edges(); // leaves for operand
    skipped += num_edges;
  }
  if (_opnds[opcnt] != oper) return -1;
  return skipped;
}

int MachNode::operand_index(Node* def) const {
  uint skipped = oper_input_base(); // Sum of leaves skipped so far
  for (uint opcnt = 1; opcnt < num_opnds(); opcnt++) {
    uint num_edges = _opnds[opcnt]->num_edges(); // leaves for operand
    for (uint i = 0; i < num_edges; i++) {
      if (in(skipped + i) == def) {
        return opcnt;
      }
    }
    skipped += num_edges;
  }
  return -1;
}

//------------------------------peephole---------------------------------------
// Apply peephole rule(s) to this instruction
MachNode *MachNode::peephole(Block *block, int block_index, PhaseRegAlloc *ra_, int &deleted) {
  return NULL;
}

//------------------------------add_case_label---------------------------------
// Adds the label for the case
void MachNode::add_case_label( int index_num, Label* blockLabel) {
  ShouldNotCallThis();
}

//------------------------------method_set-------------------------------------
// Set the absolute address of a method
void MachNode::method_set( intptr_t addr ) {
  ShouldNotCallThis();
}

//------------------------------rematerialize----------------------------------
bool MachNode::rematerialize() const {
  // Temps are always rematerializable
  if (is_MachTemp()) return true;

  uint r = rule();              // Match rule
  if (r <  Matcher::_begin_rematerialize ||
      r >= Matcher::_end_rematerialize) {
    return false;
  }

  // For 2-address instructions, the input live range is also the output
  // live range. Remateralizing does not make progress on the that live range.
  if (two_adr()) return false;

  // Check for rematerializing float constants, or not
  if (!Matcher::rematerialize_float_constants) {
    int op = ideal_Opcode();
    if (op == Op_ConF || op == Op_ConD) {
      return false;
    }
  }

  // Defining flags - can't spill these! Must remateralize.
  if (ideal_reg() == Op_RegFlags) {
    return true;
  }

  // Stretching lots of inputs - don't do it.
  if (req() > 2) {
    return false;
  }

  if (req() == 2 && in(1) && in(1)->ideal_reg() == Op_RegFlags) {
    // In(1) will be rematerialized, too.
    // Stretching lots of inputs - don't do it.
    if (in(1)->req() > 2) {
      return false;
    }
  }

  // Don't remateralize somebody with bound inputs - it stretches a
  // fixed register lifetime.
  uint idx = oper_input_base();
  if (req() > idx) {
    const RegMask &rm = in_RegMask(idx);
    if (rm.is_bound(ideal_reg())) {
      return false;
    }
  }

  return true;
}

#ifndef PRODUCT
//------------------------------dump_spec--------------------------------------
// Print any per-operand special info
void MachNode::dump_spec(outputStream *st) const {
  uint cnt = num_opnds();
  for( uint i=0; i<cnt; i++ ) {
    if (_opnds[i] != NULL) {
      _opnds[i]->dump_spec(st);
    } else {
      st->print(" _");
    }
  }
  const TypePtr *t = adr_type();
  if( t ) {
    Compile* C = Compile::current();
    if( C->alias_type(t)->is_volatile() )
      st->print(" Volatile!");
  }
}

//------------------------------dump_format------------------------------------
// access to virtual
void MachNode::dump_format(PhaseRegAlloc *ra, outputStream *st) const {
  format(ra, st); // access to virtual
}
#endif

//=============================================================================
#ifndef PRODUCT
void MachTypeNode::dump_spec(outputStream *st) const {
  if (_bottom_type != NULL) {
    _bottom_type->dump_on(st);
  } else {
    st->print(" NULL");
  }
}
#endif


//=============================================================================
int MachConstantNode::constant_offset() {
  // Bind the offset lazily.
  if (_constant.offset() == -1) {
    ConstantTable& constant_table = Compile::current()->output()->constant_table();
    int offset = constant_table.find_offset(_constant);
    // If called from Compile::scratch_emit_size return the
    // pre-calculated offset.
    // NOTE: If the AD file does some table base offset optimizations
    // later the AD file needs to take care of this fact.
    if (Compile::current()->output()->in_scratch_emit_size()) {
      return constant_table.calculate_table_base_offset() + offset;
    }
    _constant.set_offset(constant_table.table_base_offset() + offset);
  }
  return _constant.offset();
}

int MachConstantNode::constant_offset_unchecked() const {
  return _constant.offset();
}

//=============================================================================
#ifndef PRODUCT
void MachNullCheckNode::format( PhaseRegAlloc *ra_, outputStream *st ) const {
  int reg = ra_->get_reg_first(in(1)->in(_vidx));
  st->print("%s %s", Name(), Matcher::regName[reg]);
}
#endif

void MachNullCheckNode::emit(CodeBuffer &cbuf, PhaseRegAlloc *ra_) const {
  // only emits entries in the null-pointer exception handler table
}
void MachNullCheckNode::label_set(Label* label, uint block_num) {
  // Nothing to emit
}
void MachNullCheckNode::save_label( Label** label, uint* block_num ) {
  // Nothing to emit
}

const RegMask &MachNullCheckNode::in_RegMask( uint idx ) const {
  if( idx == 0 ) return RegMask::Empty;
  else return in(1)->as_Mach()->out_RegMask();
}

//=============================================================================
const Type *MachProjNode::bottom_type() const {
  if( _ideal_reg == fat_proj ) return Type::BOTTOM;
  // Try the normal mechanism first
  const Type *t = in(0)->bottom_type();
  if( t->base() == Type::Tuple ) {
    const TypeTuple *tt = t->is_tuple();
    if (_con < tt->cnt())
      return tt->field_at(_con);
  }
  // Else use generic type from ideal register set
  assert((uint)_ideal_reg < (uint)_last_machine_leaf && Type::mreg2type[_ideal_reg], "in bounds");
  return Type::mreg2type[_ideal_reg];
}

const TypePtr *MachProjNode::adr_type() const {
  if (bottom_type() == Type::MEMORY) {
    // in(0) might be a narrow MemBar; otherwise we will report TypePtr::BOTTOM
    Node* ctrl = in(0);
    if (ctrl == NULL)  return NULL; // node is dead
    const TypePtr* adr_type = ctrl->adr_type();
    #ifdef ASSERT
    if (!VMError::is_error_reported() && !Node::in_dump())
      assert(adr_type != NULL, "source must have adr_type");
    #endif
    return adr_type;
  }
  assert(bottom_type()->base() != Type::Memory, "no other memories?");
  return NULL;
}

#ifndef PRODUCT
void MachProjNode::dump_spec(outputStream *st) const {
  ProjNode::dump_spec(st);
  switch (_ideal_reg) {
  case unmatched_proj:  st->print("/unmatched");                           break;
  case fat_proj:        st->print("/fat"); if (WizardMode) _rout.dump(st); break;
  }
}
#endif

//=============================================================================
#ifndef PRODUCT
void MachIfNode::dump_spec(outputStream *st) const {
  st->print("P=%f, C=%f",_prob, _fcnt);
}
#endif

//=============================================================================
uint MachReturnNode::size_of() const { return sizeof(*this); }

//------------------------------Registers--------------------------------------
const RegMask &MachReturnNode::in_RegMask( uint idx ) const {
  return _in_rms[idx];
}

const TypePtr *MachReturnNode::adr_type() const {
  // most returns and calls are assumed to consume & modify all of memory
  // the matcher will copy non-wide adr_types from ideal originals
  return _adr_type;
}

//=============================================================================
const Type *MachSafePointNode::bottom_type() const {  return TypeTuple::MEMBAR; }

//------------------------------Registers--------------------------------------
const RegMask &MachSafePointNode::in_RegMask( uint idx ) const {
  // Values in the domain use the users calling convention, embodied in the
  // _in_rms array of RegMasks.
  if( idx < TypeFunc::Parms ) return _in_rms[idx];

  if (idx == TypeFunc::Parms &&
      ideal_Opcode() == Op_SafePoint) {
    return MachNode::in_RegMask(idx);
  }

  // Values outside the domain represent debug info
  assert(in(idx)->ideal_reg() != Op_RegFlags, "flags register is not spillable");
  return *Compile::current()->matcher()->idealreg2spillmask[in(idx)->ideal_reg()];
}


//=============================================================================

bool MachCallNode::cmp( const Node &n ) const
{ return _tf == ((MachCallNode&)n)._tf; }
const Type *MachCallNode::bottom_type() const { return tf()->range(); }
const Type* MachCallNode::Value(PhaseGVN* phase) const { return tf()->range(); }

#ifndef PRODUCT
void MachCallNode::dump_spec(outputStream *st) const {
  st->print("# ");
  if (tf() != NULL)  tf()->dump_on(st);
  if (_cnt != COUNT_UNKNOWN)  st->print(" C=%f",_cnt);
  if (jvms() != NULL)  jvms()->dump_spec(st);
}
#endif

bool MachCallNode::return_value_is_used() const {
  if (tf()->range()->cnt() == TypeFunc::Parms) {
    // void return
    return false;
  }

  // find the projection corresponding to the return value
  for (DUIterator_Fast imax, i = fast_outs(imax); i < imax; i++) {
    Node *use = fast_out(i);
    if (!use->is_Proj()) continue;
    if (use->as_Proj()->_con == TypeFunc::Parms) {
      return true;
    }
  }
  return false;
}

// Similar to cousin class CallNode::returns_pointer
// Because this is used in deoptimization, we want the type info, not the data
// flow info; the interpreter will "use" things that are dead to the optimizer.
bool MachCallNode::returns_pointer() const {
  const TypeTuple *r = tf()->range();
  return (r->cnt() > TypeFunc::Parms &&
          r->field_at(TypeFunc::Parms)->isa_ptr());
}

//------------------------------Registers--------------------------------------
const RegMask &MachCallNode::in_RegMask(uint idx) const {
  // Values in the domain use the users calling convention, embodied in the
  // _in_rms array of RegMasks.
  if (idx < tf()->domain()->cnt()) {
    return _in_rms[idx];
  }
  if (idx == mach_constant_base_node_input()) {
    return MachConstantBaseNode::static_out_RegMask();
  }
  // Values outside the domain represent debug info
  return *Compile::current()->matcher()->idealreg2debugmask[in(idx)->ideal_reg()];
}

//=============================================================================
uint MachCallJavaNode::size_of() const { return sizeof(*this); }
bool MachCallJavaNode::cmp( const Node &n ) const {
  MachCallJavaNode &call = (MachCallJavaNode&)n;
  return MachCallNode::cmp(call) && _method->equals(call._method) &&
         _override_symbolic_info == call._override_symbolic_info;
}
#ifndef PRODUCT
void MachCallJavaNode::dump_spec(outputStream *st) const {
  if (_method_handle_invoke)
    st->print("MethodHandle ");
  if (_method) {
    _method->print_short_name(st);
    st->print(" ");
  }
  MachCallNode::dump_spec(st);
}
#endif

//------------------------------Registers--------------------------------------
const RegMask &MachCallJavaNode::in_RegMask(uint idx) const {
  // Values in the domain use the users calling convention, embodied in the
  // _in_rms array of RegMasks.
  if (idx < tf()->domain()->cnt()) {
    return _in_rms[idx];
  }
  if (idx == mach_constant_base_node_input()) {
    return MachConstantBaseNode::static_out_RegMask();
  }
  // Values outside the domain represent debug info
  Matcher* m = Compile::current()->matcher();
  // If this call is a MethodHandle invoke we have to use a different
  // debugmask which does not include the register we use to save the
  // SP over MH invokes.
  RegMask** debugmask = _method_handle_invoke ? m->idealreg2mhdebugmask : m->idealreg2debugmask;
  return *debugmask[in(idx)->ideal_reg()];
}

//=============================================================================
uint MachCallStaticJavaNode::size_of() const { return sizeof(*this); }
bool MachCallStaticJavaNode::cmp( const Node &n ) const {
  MachCallStaticJavaNode &call = (MachCallStaticJavaNode&)n;
  return MachCallJavaNode::cmp(call) && _name == call._name;
}

//----------------------------uncommon_trap_request----------------------------
// If this is an uncommon trap, return the request code, else zero.
int MachCallStaticJavaNode::uncommon_trap_request() const {
  if (_name != NULL && !strcmp(_name, "uncommon_trap")) {
    return CallStaticJavaNode::extract_uncommon_trap_request(this);
  }
  return 0;
}

#ifndef PRODUCT
// Helper for summarizing uncommon_trap arguments.
void MachCallStaticJavaNode::dump_trap_args(outputStream *st) const {
  int trap_req = uncommon_trap_request();
  if (trap_req != 0) {
    char buf[100];
    st->print("(%s)",
               Deoptimization::format_trap_request(buf, sizeof(buf),
                                                   trap_req));
  }
}

void MachCallStaticJavaNode::dump_spec(outputStream *st) const {
  st->print("Static ");
  if (_name != NULL) {
    st->print("wrapper for: %s", _name );
    dump_trap_args(st);
    st->print(" ");
  }
  MachCallJavaNode::dump_spec(st);
}
#endif

//=============================================================================
#ifndef PRODUCT
void MachCallDynamicJavaNode::dump_spec(outputStream *st) const {
  st->print("Dynamic ");
  MachCallJavaNode::dump_spec(st);
}
#endif
//=============================================================================
uint MachCallRuntimeNode::size_of() const { return sizeof(*this); }
bool MachCallRuntimeNode::cmp( const Node &n ) const {
  MachCallRuntimeNode &call = (MachCallRuntimeNode&)n;
  return MachCallNode::cmp(call) && !strcmp(_name,call._name);
}
#ifndef PRODUCT
void MachCallRuntimeNode::dump_spec(outputStream *st) const {
  st->print("%s ",_name);
  MachCallNode::dump_spec(st);
}
#endif
//=============================================================================
uint MachCallNativeNode::size_of() const { return sizeof(*this); }
bool MachCallNativeNode::cmp( const Node &n ) const {
  MachCallNativeNode &call = (MachCallNativeNode&)n;
  return MachCallNode::cmp(call) && !strcmp(_name,call._name)
    && _arg_regs == call._arg_regs && _ret_regs == call._ret_regs;
}
#ifndef PRODUCT
void MachCallNativeNode::dump_spec(outputStream *st) const {
  st->print("%s ",_name);
  st->print("_arg_regs: ");
  CallNativeNode::print_regs(_arg_regs, st);
  st->print("_ret_regs: ");
  CallNativeNode::print_regs(_ret_regs, st);
  MachCallNode::dump_spec(st);
}
#endif
//=============================================================================
// A shared JVMState for all HaltNodes.  Indicates the start of debug info
// is at TypeFunc::Parms.  Only required for SOE register spill handling -
// to indicate where the stack-slot-only debug info inputs begin.
// There is no other JVM state needed here.
JVMState jvms_for_throw(0);
JVMState *MachHaltNode::jvms() const {
  return &jvms_for_throw;
}

uint MachMemBarNode::size_of() const { return sizeof(*this); }

const TypePtr *MachMemBarNode::adr_type() const {
  return _adr_type;
}


//=============================================================================
#ifndef PRODUCT
void labelOper::int_format(PhaseRegAlloc *ra, const MachNode *node, outputStream *st) const {
  st->print("B%d", _block_num);
}
#endif // PRODUCT

//=============================================================================
#ifndef PRODUCT
void methodOper::int_format(PhaseRegAlloc *ra, const MachNode *node, outputStream *st) const {
  st->print(INTPTR_FORMAT, _method);
}
#endif // PRODUCT
