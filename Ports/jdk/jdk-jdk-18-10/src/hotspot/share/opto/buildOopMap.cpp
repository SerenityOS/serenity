/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "code/vmreg.inline.hpp"
#include "compiler/oopMap.hpp"
#include "memory/resourceArea.hpp"
#include "opto/addnode.hpp"
#include "opto/callnode.hpp"
#include "opto/compile.hpp"
#include "opto/machnode.hpp"
#include "opto/matcher.hpp"
#include "opto/output.hpp"
#include "opto/phase.hpp"
#include "opto/regalloc.hpp"
#include "opto/rootnode.hpp"
#include "utilities/align.hpp"

// The functions in this file builds OopMaps after all scheduling is done.
//
// OopMaps contain a list of all registers and stack-slots containing oops (so
// they can be updated by GC).  OopMaps also contain a list of derived-pointer
// base-pointer pairs.  When the base is moved, the derived pointer moves to
// follow it.  Finally, any registers holding callee-save values are also
// recorded.  These might contain oops, but only the caller knows.
//
// BuildOopMaps implements a simple forward reaching-defs solution.  At each
// GC point we'll have the reaching-def Nodes.  If the reaching Nodes are
// typed as pointers (no offset), then they are oops.  Pointers+offsets are
// derived pointers, and bases can be found from them.  Finally, we'll also
// track reaching callee-save values.  Note that a copy of a callee-save value
// "kills" it's source, so that only 1 copy of a callee-save value is alive at
// a time.
//
// We run a simple bitvector liveness pass to help trim out dead oops.  Due to
// irreducible loops, we can have a reaching def of an oop that only reaches
// along one path and no way to know if it's valid or not on the other path.
// The bitvectors are quite dense and the liveness pass is fast.
//
// At GC points, we consult this information to build OopMaps.  All reaching
// defs typed as oops are added to the OopMap.  Only 1 instance of a
// callee-save register can be recorded.  For derived pointers, we'll have to
// find and record the register holding the base.
//
// The reaching def's is a simple 1-pass worklist approach.  I tried a clever
// breadth-first approach but it was worse (showed O(n^2) in the
// pick-next-block code).
//
// The relevant data is kept in a struct of arrays (it could just as well be
// an array of structs, but the struct-of-arrays is generally a little more
// efficient).  The arrays are indexed by register number (including
// stack-slots as registers) and so is bounded by 200 to 300 elements in
// practice.  One array will map to a reaching def Node (or NULL for
// conflict/dead).  The other array will map to a callee-saved register or
// OptoReg::Bad for not-callee-saved.


// Structure to pass around
struct OopFlow : public ResourceObj {
  short *_callees;              // Array mapping register to callee-saved
  Node **_defs;                 // array mapping register to reaching def
                                // or NULL if dead/conflict
  // OopFlow structs, when not being actively modified, describe the _end_ of
  // this block.
  Block *_b;                    // Block for this struct
  OopFlow *_next;               // Next free OopFlow
                                // or NULL if dead/conflict
  Compile* C;

  OopFlow( short *callees, Node **defs, Compile* c ) : _callees(callees), _defs(defs),
    _b(NULL), _next(NULL), C(c) { }

  // Given reaching-defs for this block start, compute it for this block end
  void compute_reach( PhaseRegAlloc *regalloc, int max_reg, Dict *safehash );

  // Merge these two OopFlows into the 'this' pointer.
  void merge( OopFlow *flow, int max_reg );

  // Copy a 'flow' over an existing flow
  void clone( OopFlow *flow, int max_size);

  // Make a new OopFlow from scratch
  static OopFlow *make( Arena *A, int max_size, Compile* C );

  // Build an oopmap from the current flow info
  OopMap *build_oop_map( Node *n, int max_reg, PhaseRegAlloc *regalloc, int* live );
};

// Given reaching-defs for this block start, compute it for this block end
void OopFlow::compute_reach( PhaseRegAlloc *regalloc, int max_reg, Dict *safehash ) {

  for( uint i=0; i<_b->number_of_nodes(); i++ ) {
    Node *n = _b->get_node(i);

    if( n->jvms() ) {           // Build an OopMap here?
      JVMState *jvms = n->jvms();
      // no map needed for leaf calls
      if( n->is_MachSafePoint() && !n->is_MachCallLeaf() ) {
        int *live = (int*) (*safehash)[n];
        assert( live, "must find live" );
        n->as_MachSafePoint()->set_oop_map( build_oop_map(n,max_reg,regalloc, live) );
      }
    }

    // Assign new reaching def's.
    // Note that I padded the _defs and _callees arrays so it's legal
    // to index at _defs[OptoReg::Bad].
    OptoReg::Name first = regalloc->get_reg_first(n);
    OptoReg::Name second = regalloc->get_reg_second(n);
    _defs[first] = n;
    _defs[second] = n;

    // Pass callee-save info around copies
    int idx = n->is_Copy();
    if( idx ) {                 // Copies move callee-save info
      OptoReg::Name old_first = regalloc->get_reg_first(n->in(idx));
      OptoReg::Name old_second = regalloc->get_reg_second(n->in(idx));
      int tmp_first = _callees[old_first];
      int tmp_second = _callees[old_second];
      _callees[old_first] = OptoReg::Bad; // callee-save is moved, dead in old location
      _callees[old_second] = OptoReg::Bad;
      _callees[first] = tmp_first;
      _callees[second] = tmp_second;
    } else if( n->is_Phi() ) {  // Phis do not mod callee-saves
      assert( _callees[first] == _callees[regalloc->get_reg_first(n->in(1))], "" );
      assert( _callees[second] == _callees[regalloc->get_reg_second(n->in(1))], "" );
      assert( _callees[first] == _callees[regalloc->get_reg_first(n->in(n->req()-1))], "" );
      assert( _callees[second] == _callees[regalloc->get_reg_second(n->in(n->req()-1))], "" );
    } else {
      _callees[first] = OptoReg::Bad; // No longer holding a callee-save value
      _callees[second] = OptoReg::Bad;

      // Find base case for callee saves
      if( n->is_Proj() && n->in(0)->is_Start() ) {
        if( OptoReg::is_reg(first) &&
            regalloc->_matcher.is_save_on_entry(first) )
          _callees[first] = first;
        if( OptoReg::is_reg(second) &&
            regalloc->_matcher.is_save_on_entry(second) )
          _callees[second] = second;
      }
    }
  }
}

// Merge the given flow into the 'this' flow
void OopFlow::merge( OopFlow *flow, int max_reg ) {
  assert( _b == NULL, "merging into a happy flow" );
  assert( flow->_b, "this flow is still alive" );
  assert( flow != this, "no self flow" );

  // Do the merge.  If there are any differences, drop to 'bottom' which
  // is OptoReg::Bad or NULL depending.
  for( int i=0; i<max_reg; i++ ) {
    // Merge the callee-save's
    if( _callees[i] != flow->_callees[i] )
      _callees[i] = OptoReg::Bad;
    // Merge the reaching defs
    if( _defs[i] != flow->_defs[i] )
      _defs[i] = NULL;
  }

}

void OopFlow::clone( OopFlow *flow, int max_size ) {
  _b = flow->_b;
  memcpy( _callees, flow->_callees, sizeof(short)*max_size);
  memcpy( _defs   , flow->_defs   , sizeof(Node*)*max_size);
}

OopFlow *OopFlow::make( Arena *A, int max_size, Compile* C ) {
  short *callees = NEW_ARENA_ARRAY(A,short,max_size+1);
  Node **defs    = NEW_ARENA_ARRAY(A,Node*,max_size+1);
  debug_only( memset(defs,0,(max_size+1)*sizeof(Node*)) );
  OopFlow *flow = new (A) OopFlow(callees+1, defs+1, C);
  assert( &flow->_callees[OptoReg::Bad] == callees, "Ok to index at OptoReg::Bad" );
  assert( &flow->_defs   [OptoReg::Bad] == defs   , "Ok to index at OptoReg::Bad" );
  return flow;
}

static int get_live_bit( int *live, int reg ) {
  return live[reg>>LogBitsPerInt] &   (1<<(reg&(BitsPerInt-1))); }
static void set_live_bit( int *live, int reg ) {
         live[reg>>LogBitsPerInt] |=  (1<<(reg&(BitsPerInt-1))); }
static void clr_live_bit( int *live, int reg ) {
         live[reg>>LogBitsPerInt] &= ~(1<<(reg&(BitsPerInt-1))); }

// Build an oopmap from the current flow info
OopMap *OopFlow::build_oop_map( Node *n, int max_reg, PhaseRegAlloc *regalloc, int* live ) {
  int framesize = regalloc->_framesize;
  int max_inarg_slot = OptoReg::reg2stack(regalloc->_matcher._new_SP);
  debug_only( char *dup_check = NEW_RESOURCE_ARRAY(char,OptoReg::stack0());
              memset(dup_check,0,OptoReg::stack0()) );

  OopMap *omap = new OopMap( framesize,  max_inarg_slot );
  MachCallNode *mcall = n->is_MachCall() ? n->as_MachCall() : NULL;
  JVMState* jvms = n->jvms();

  // For all registers do...
  for( int reg=0; reg<max_reg; reg++ ) {
    if( get_live_bit(live,reg) == 0 )
      continue;                 // Ignore if not live

    // %%% C2 can use 2 OptoRegs when the physical register is only one 64bit
    // register in that case we'll get an non-concrete register for the second
    // half. We only need to tell the map the register once!
    //
    // However for the moment we disable this change and leave things as they
    // were.

    VMReg r = OptoReg::as_VMReg(OptoReg::Name(reg), framesize, max_inarg_slot);

    if (false && r->is_reg() && !r->is_concrete()) {
      continue;
    }

    // See if dead (no reaching def).
    Node *def = _defs[reg];     // Get reaching def
    assert( def, "since live better have reaching def" );

    // Classify the reaching def as oop, derived, callee-save, dead, or other
    const Type *t = def->bottom_type();
    if( t->isa_oop_ptr() ) {    // Oop or derived?
      assert( !OptoReg::is_valid(_callees[reg]), "oop can't be callee save" );
#ifdef _LP64
      // 64-bit pointers record oop-ishness on 2 aligned adjacent registers.
      // Make sure both are record from the same reaching def, but do not
      // put both into the oopmap.
      if( (reg&1) == 1 ) {      // High half of oop-pair?
        assert( _defs[reg-1] == _defs[reg], "both halves from same reaching def" );
        continue;               // Do not record high parts in oopmap
      }
#endif

      // Check for a legal reg name in the oopMap and bailout if it is not.
      if (!omap->legal_vm_reg_name(r)) {
        regalloc->C->record_method_not_compilable("illegal oopMap register name");
        continue;
      }
      if( t->is_ptr()->_offset == 0 ) { // Not derived?
        if( mcall ) {
          // Outgoing argument GC mask responsibility belongs to the callee,
          // not the caller.  Inspect the inputs to the call, to see if
          // this live-range is one of them.
          uint cnt = mcall->tf()->domain()->cnt();
          uint j;
          for( j = TypeFunc::Parms; j < cnt; j++)
            if( mcall->in(j) == def )
              break;            // reaching def is an argument oop
          if( j < cnt )         // arg oops dont go in GC map
            continue;           // Continue on to the next register
        }
        omap->set_oop(r);
      } else {                  // Else it's derived.
        // Find the base of the derived value.
        uint i;
        // Fast, common case, scan
        for( i = jvms->oopoff(); i < n->req(); i+=2 )
          if( n->in(i) == def ) break; // Common case
        if( i == n->req() ) {   // Missed, try a more generous scan
          // Scan again, but this time peek through copies
          for( i = jvms->oopoff(); i < n->req(); i+=2 ) {
            Node *m = n->in(i); // Get initial derived value
            while( 1 ) {
              Node *d = def;    // Get initial reaching def
              while( 1 ) {      // Follow copies of reaching def to end
                if( m == d ) goto found; // breaks 3 loops
                int idx = d->is_Copy();
                if( !idx ) break;
                d = d->in(idx);     // Link through copy
              }
              int idx = m->is_Copy();
              if( !idx ) break;
              m = m->in(idx);
            }
          }
          guarantee( 0, "must find derived/base pair" );
        }
      found: ;
        Node *base = n->in(i+1); // Base is other half of pair
        int breg = regalloc->get_reg_first(base);
        VMReg b = OptoReg::as_VMReg(OptoReg::Name(breg), framesize, max_inarg_slot);

        // I record liveness at safepoints BEFORE I make the inputs
        // live.  This is because argument oops are NOT live at a
        // safepoint (or at least they cannot appear in the oopmap).
        // Thus bases of base/derived pairs might not be in the
        // liveness data but they need to appear in the oopmap.
        if( get_live_bit(live,breg) == 0 ) {// Not live?
          // Flag it, so next derived pointer won't re-insert into oopmap
          set_live_bit(live,breg);
          // Already missed our turn?
          if( breg < reg ) {
            if (b->is_stack() || b->is_concrete() || true ) {
              omap->set_oop( b);
            }
          }
        }
        if (b->is_stack() || b->is_concrete() || true ) {
          omap->set_derived_oop( r, b);
        }
      }

    } else if( t->isa_narrowoop() ) {
      assert( !OptoReg::is_valid(_callees[reg]), "oop can't be callee save" );
      // Check for a legal reg name in the oopMap and bailout if it is not.
      if (!omap->legal_vm_reg_name(r)) {
        regalloc->C->record_method_not_compilable("illegal oopMap register name");
        continue;
      }
      if( mcall ) {
          // Outgoing argument GC mask responsibility belongs to the callee,
          // not the caller.  Inspect the inputs to the call, to see if
          // this live-range is one of them.
        uint cnt = mcall->tf()->domain()->cnt();
        uint j;
        for( j = TypeFunc::Parms; j < cnt; j++)
          if( mcall->in(j) == def )
            break;            // reaching def is an argument oop
        if( j < cnt )         // arg oops dont go in GC map
          continue;           // Continue on to the next register
      }
      omap->set_narrowoop(r);
    } else if( OptoReg::is_valid(_callees[reg])) { // callee-save?
      // It's a callee-save value
      assert( dup_check[_callees[reg]]==0, "trying to callee save same reg twice" );
      debug_only( dup_check[_callees[reg]]=1; )
      VMReg callee = OptoReg::as_VMReg(OptoReg::Name(_callees[reg]));
      if ( callee->is_concrete() || true ) {
        omap->set_callee_saved( r, callee);
      }

    } else {
      // Other - some reaching non-oop value
#ifdef ASSERT
      if( t->isa_rawptr() && C->cfg()->_raw_oops.member(def) ) {
        def->dump();
        n->dump();
        assert(false, "there should be a oop in OopMap instead of a live raw oop at safepoint");
      }
#endif
    }

  }

#ifdef ASSERT
  /* Nice, Intel-only assert
  int cnt_callee_saves=0;
  int reg2 = 0;
  while (OptoReg::is_reg(reg2)) {
    if( dup_check[reg2] != 0) cnt_callee_saves++;
    assert( cnt_callee_saves==3 || cnt_callee_saves==5, "missed some callee-save" );
    reg2++;
  }
  */
#endif

#ifdef ASSERT
  for( OopMapStream oms1(omap); !oms1.is_done(); oms1.next()) {
    OopMapValue omv1 = oms1.current();
    if (omv1.type() != OopMapValue::derived_oop_value) {
      continue;
    }
    bool found = false;
    for( OopMapStream oms2(omap); !oms2.is_done(); oms2.next()) {
      OopMapValue omv2 = oms2.current();
      if (omv2.type() != OopMapValue::oop_value) {
        continue;
      }
      if( omv1.content_reg() == omv2.reg() ) {
        found = true;
        break;
      }
    }
    assert( found, "derived with no base in oopmap" );
  }
#endif

  return omap;
}

// Compute backwards liveness on registers
static void do_liveness(PhaseRegAlloc* regalloc, PhaseCFG* cfg, Block_List* worklist, int max_reg_ints, Arena* A, Dict* safehash) {
  int* live = NEW_ARENA_ARRAY(A, int, (cfg->number_of_blocks() + 1) * max_reg_ints);
  int* tmp_live = &live[cfg->number_of_blocks() * max_reg_ints];
  Node* root = cfg->get_root_node();
  // On CISC platforms, get the node representing the stack pointer  that regalloc
  // used for spills
  Node *fp = NodeSentinel;
  if (UseCISCSpill && root->req() > 1) {
    fp = root->in(1)->in(TypeFunc::FramePtr);
  }
  memset(live, 0, cfg->number_of_blocks() * (max_reg_ints << LogBytesPerInt));
  // Push preds onto worklist
  for (uint i = 1; i < root->req(); i++) {
    Block* block = cfg->get_block_for_node(root->in(i));
    worklist->push(block);
  }

  // ZKM.jar includes tiny infinite loops which are unreached from below.
  // If we missed any blocks, we'll retry here after pushing all missed
  // blocks on the worklist.  Normally this outer loop never trips more
  // than once.
  while (1) {

    while( worklist->size() ) { // Standard worklist algorithm
      Block *b = worklist->rpop();

      // Copy first successor into my tmp_live space
      int s0num = b->_succs[0]->_pre_order;
      int *t = &live[s0num*max_reg_ints];
      for( int i=0; i<max_reg_ints; i++ )
        tmp_live[i] = t[i];

      // OR in the remaining live registers
      for( uint j=1; j<b->_num_succs; j++ ) {
        uint sjnum = b->_succs[j]->_pre_order;
        int *t = &live[sjnum*max_reg_ints];
        for( int i=0; i<max_reg_ints; i++ )
          tmp_live[i] |= t[i];
      }

      // Now walk tmp_live up the block backwards, computing live
      for( int k=b->number_of_nodes()-1; k>=0; k-- ) {
        Node *n = b->get_node(k);
        // KILL def'd bits
        int first = regalloc->get_reg_first(n);
        int second = regalloc->get_reg_second(n);
        if( OptoReg::is_valid(first) ) clr_live_bit(tmp_live,first);
        if( OptoReg::is_valid(second) ) clr_live_bit(tmp_live,second);

        MachNode *m = n->is_Mach() ? n->as_Mach() : NULL;

        // Check if m is potentially a CISC alternate instruction (i.e, possibly
        // synthesized by RegAlloc from a conventional instruction and a
        // spilled input)
        bool is_cisc_alternate = false;
        if (UseCISCSpill && m) {
          is_cisc_alternate = m->is_cisc_alternate();
        }

        // GEN use'd bits
        for( uint l=1; l<n->req(); l++ ) {
          Node *def = n->in(l);
          assert(def != 0, "input edge required");
          int first = regalloc->get_reg_first(def);
          int second = regalloc->get_reg_second(def);
          //If peephole had removed the node,do not set live bit for it.
          if (!(def->is_Mach() && def->as_Mach()->get_removed())) {
            if (OptoReg::is_valid(first)) set_live_bit(tmp_live,first);
            if (OptoReg::is_valid(second)) set_live_bit(tmp_live,second);
          }
          // If we use the stack pointer in a cisc-alternative instruction,
          // check for use as a memory operand.  Then reconstruct the RegName
          // for this stack location, and set the appropriate bit in the
          // live vector 4987749.
          if (is_cisc_alternate && def == fp) {
            const TypePtr *adr_type = NULL;
            intptr_t offset;
            const Node* base = m->get_base_and_disp(offset, adr_type);
            if (base == NodeSentinel) {
              // Machnode has multiple memory inputs. We are unable to reason
              // with these, but are presuming (with trepidation) that not any of
              // them are oops. This can be fixed by making get_base_and_disp()
              // look at a specific input instead of all inputs.
              assert(!def->bottom_type()->isa_oop_ptr(), "expecting non-oop mem input");
            } else if (base != fp || offset == Type::OffsetBot) {
              // Do nothing: the fp operand is either not from a memory use
              // (base == NULL) OR the fp is used in a non-memory context
              // (base is some other register) OR the offset is not constant,
              // so it is not a stack slot.
            } else {
              assert(offset >= 0, "unexpected negative offset");
              offset -= (offset % jintSize);  // count the whole word
              int stack_reg = regalloc->offset2reg(offset);
              if (OptoReg::is_stack(stack_reg)) {
                set_live_bit(tmp_live, stack_reg);
              } else {
                assert(false, "stack_reg not on stack?");
              }
            }
          }
        }

        if( n->jvms() ) {       // Record liveness at safepoint

          // This placement of this stanza means inputs to calls are
          // considered live at the callsite's OopMap.  Argument oops are
          // hence live, but NOT included in the oopmap.  See cutout in
          // build_oop_map.  Debug oops are live (and in OopMap).
          int *n_live = NEW_ARENA_ARRAY(A, int, max_reg_ints);
          for( int l=0; l<max_reg_ints; l++ )
            n_live[l] = tmp_live[l];
          safehash->Insert(n,n_live);
        }

      }

      // Now at block top, see if we have any changes.  If so, propagate
      // to prior blocks.
      int *old_live = &live[b->_pre_order*max_reg_ints];
      int l;
      for( l=0; l<max_reg_ints; l++ )
        if( tmp_live[l] != old_live[l] )
          break;
      if( l<max_reg_ints ) {     // Change!
        // Copy in new value
        for( l=0; l<max_reg_ints; l++ )
          old_live[l] = tmp_live[l];
        // Push preds onto worklist
        for (l = 1; l < (int)b->num_preds(); l++) {
          Block* block = cfg->get_block_for_node(b->pred(l));
          worklist->push(block);
        }
      }
    }

    // Scan for any missing safepoints.  Happens to infinite loops
    // ala ZKM.jar
    uint i;
    for (i = 1; i < cfg->number_of_blocks(); i++) {
      Block* block = cfg->get_block(i);
      uint j;
      for (j = 1; j < block->number_of_nodes(); j++) {
        if (block->get_node(j)->jvms() && (*safehash)[block->get_node(j)] == NULL) {
           break;
        }
      }
      if (j < block->number_of_nodes()) {
        break;
      }
    }
    if (i == cfg->number_of_blocks()) {
      break;                    // Got 'em all
    }

    if (PrintOpto && Verbose) {
      tty->print_cr("retripping live calc");
    }

    // Force the issue (expensively): recheck everybody
    for (i = 1; i < cfg->number_of_blocks(); i++) {
      worklist->push(cfg->get_block(i));
    }
  }
}

// Collect GC mask info - where are all the OOPs?
void PhaseOutput::BuildOopMaps() {
  Compile::TracePhase tp("bldOopMaps", &timers[_t_buildOopMaps]);
  // Can't resource-mark because I need to leave all those OopMaps around,
  // or else I need to resource-mark some arena other than the default.
  // ResourceMark rm;              // Reclaim all OopFlows when done
  int max_reg = C->regalloc()->_max_reg; // Current array extent

  Arena *A = Thread::current()->resource_area();
  Block_List worklist;          // Worklist of pending blocks

  int max_reg_ints = align_up(max_reg, BitsPerInt)>>LogBitsPerInt;
  Dict *safehash = NULL;        // Used for assert only
  // Compute a backwards liveness per register.  Needs a bitarray of
  // #blocks x (#registers, rounded up to ints)
  safehash = new Dict(cmpkey,hashkey,A);
  do_liveness( C->regalloc(), C->cfg(), &worklist, max_reg_ints, A, safehash );
  OopFlow *free_list = NULL;    // Free, unused

  // Array mapping blocks to completed oopflows
  OopFlow **flows = NEW_ARENA_ARRAY(A, OopFlow*, C->cfg()->number_of_blocks());
  memset( flows, 0, C->cfg()->number_of_blocks() * sizeof(OopFlow*) );


  // Do the first block 'by hand' to prime the worklist
  Block *entry = C->cfg()->get_block(1);
  OopFlow *rootflow = OopFlow::make(A,max_reg,C);
  // Initialize to 'bottom' (not 'top')
  memset( rootflow->_callees, OptoReg::Bad, max_reg*sizeof(short) );
  memset( rootflow->_defs   ,            0, max_reg*sizeof(Node*) );
  flows[entry->_pre_order] = rootflow;

  // Do the first block 'by hand' to prime the worklist
  rootflow->_b = entry;
  rootflow->compute_reach( C->regalloc(), max_reg, safehash );
  for( uint i=0; i<entry->_num_succs; i++ )
    worklist.push(entry->_succs[i]);

  // Now worklist contains blocks which have some, but perhaps not all,
  // predecessors visited.
  while( worklist.size() ) {
    // Scan for a block with all predecessors visited, or any randoms slob
    // otherwise.  All-preds-visited order allows me to recycle OopFlow
    // structures rapidly and cut down on the memory footprint.
    // Note: not all predecessors might be visited yet (must happen for
    // irreducible loops).  This is OK, since every live value must have the
    // SAME reaching def for the block, so any reaching def is OK.
    uint i;

    Block *b = worklist.pop();
    // Ignore root block
    if (b == C->cfg()->get_root_block()) {
      continue;
    }
    // Block is already done?  Happens if block has several predecessors,
    // he can get on the worklist more than once.
    if( flows[b->_pre_order] ) continue;

    // If this block has a visited predecessor AND that predecessor has this
    // last block as his only undone child, we can move the OopFlow from the
    // pred to this block.  Otherwise we have to grab a new OopFlow.
    OopFlow *flow = NULL;       // Flag for finding optimized flow
    Block *pred = (Block*)((intptr_t)0xdeadbeef);
    // Scan this block's preds to find a done predecessor
    for (uint j = 1; j < b->num_preds(); j++) {
      Block* p = C->cfg()->get_block_for_node(b->pred(j));
      OopFlow *p_flow = flows[p->_pre_order];
      if( p_flow ) {            // Predecessor is done
        assert( p_flow->_b == p, "cross check" );
        pred = p;               // Record some predecessor
        // If all successors of p are done except for 'b', then we can carry
        // p_flow forward to 'b' without copying, otherwise we have to draw
        // from the free_list and clone data.
        uint k;
        for( k=0; k<p->_num_succs; k++ )
          if( !flows[p->_succs[k]->_pre_order] &&
              p->_succs[k] != b )
            break;

        // Either carry-forward the now-unused OopFlow for b's use
        // or draw a new one from the free list
        if( k==p->_num_succs ) {
          flow = p_flow;
          break;                // Found an ideal pred, use him
        }
      }
    }

    if( flow ) {
      // We have an OopFlow that's the last-use of a predecessor.
      // Carry it forward.
    } else {                    // Draw a new OopFlow from the freelist
      if( !free_list )
        free_list = OopFlow::make(A,max_reg,C);
      flow = free_list;
      assert( flow->_b == NULL, "oopFlow is not free" );
      free_list = flow->_next;
      flow->_next = NULL;

      // Copy/clone over the data
      flow->clone(flows[pred->_pre_order], max_reg);
    }

    // Mark flow for block.  Blocks can only be flowed over once,
    // because after the first time they are guarded from entering
    // this code again.
    assert( flow->_b == pred, "have some prior flow" );
    flow->_b = NULL;

    // Now push flow forward
    flows[b->_pre_order] = flow;// Mark flow for this block
    flow->_b = b;
    flow->compute_reach( C->regalloc(), max_reg, safehash );

    // Now push children onto worklist
    for( i=0; i<b->_num_succs; i++ )
      worklist.push(b->_succs[i]);

  }
}
