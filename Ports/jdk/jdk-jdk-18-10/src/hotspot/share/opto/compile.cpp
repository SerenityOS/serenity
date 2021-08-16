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
#include "jvm_io.h"
#include "asm/macroAssembler.hpp"
#include "asm/macroAssembler.inline.hpp"
#include "ci/ciReplay.hpp"
#include "classfile/javaClasses.hpp"
#include "code/exceptionHandlerTable.hpp"
#include "code/nmethod.hpp"
#include "compiler/compileBroker.hpp"
#include "compiler/compileLog.hpp"
#include "compiler/disassembler.hpp"
#include "compiler/oopMap.hpp"
#include "gc/shared/barrierSet.hpp"
#include "gc/shared/c2/barrierSetC2.hpp"
#include "jfr/jfrEvents.hpp"
#include "memory/resourceArea.hpp"
#include "opto/addnode.hpp"
#include "opto/block.hpp"
#include "opto/c2compiler.hpp"
#include "opto/callGenerator.hpp"
#include "opto/callnode.hpp"
#include "opto/castnode.hpp"
#include "opto/cfgnode.hpp"
#include "opto/chaitin.hpp"
#include "opto/compile.hpp"
#include "opto/connode.hpp"
#include "opto/convertnode.hpp"
#include "opto/divnode.hpp"
#include "opto/escape.hpp"
#include "opto/idealGraphPrinter.hpp"
#include "opto/loopnode.hpp"
#include "opto/machnode.hpp"
#include "opto/macro.hpp"
#include "opto/matcher.hpp"
#include "opto/mathexactnode.hpp"
#include "opto/memnode.hpp"
#include "opto/mulnode.hpp"
#include "opto/narrowptrnode.hpp"
#include "opto/node.hpp"
#include "opto/opcodes.hpp"
#include "opto/output.hpp"
#include "opto/parse.hpp"
#include "opto/phaseX.hpp"
#include "opto/rootnode.hpp"
#include "opto/runtime.hpp"
#include "opto/stringopts.hpp"
#include "opto/type.hpp"
#include "opto/vector.hpp"
#include "opto/vectornode.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/signature.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/timer.hpp"
#include "utilities/align.hpp"
#include "utilities/copy.hpp"
#include "utilities/macros.hpp"
#include "utilities/resourceHash.hpp"


// -------------------- Compile::mach_constant_base_node -----------------------
// Constant table base node singleton.
MachConstantBaseNode* Compile::mach_constant_base_node() {
  if (_mach_constant_base_node == NULL) {
    _mach_constant_base_node = new MachConstantBaseNode();
    _mach_constant_base_node->add_req(C->root());
  }
  return _mach_constant_base_node;
}


/// Support for intrinsics.

// Return the index at which m must be inserted (or already exists).
// The sort order is by the address of the ciMethod, with is_virtual as minor key.
class IntrinsicDescPair {
 private:
  ciMethod* _m;
  bool _is_virtual;
 public:
  IntrinsicDescPair(ciMethod* m, bool is_virtual) : _m(m), _is_virtual(is_virtual) {}
  static int compare(IntrinsicDescPair* const& key, CallGenerator* const& elt) {
    ciMethod* m= elt->method();
    ciMethod* key_m = key->_m;
    if (key_m < m)      return -1;
    else if (key_m > m) return 1;
    else {
      bool is_virtual = elt->is_virtual();
      bool key_virtual = key->_is_virtual;
      if (key_virtual < is_virtual)      return -1;
      else if (key_virtual > is_virtual) return 1;
      else                               return 0;
    }
  }
};
int Compile::intrinsic_insertion_index(ciMethod* m, bool is_virtual, bool& found) {
#ifdef ASSERT
  for (int i = 1; i < _intrinsics.length(); i++) {
    CallGenerator* cg1 = _intrinsics.at(i-1);
    CallGenerator* cg2 = _intrinsics.at(i);
    assert(cg1->method() != cg2->method()
           ? cg1->method()     < cg2->method()
           : cg1->is_virtual() < cg2->is_virtual(),
           "compiler intrinsics list must stay sorted");
  }
#endif
  IntrinsicDescPair pair(m, is_virtual);
  return _intrinsics.find_sorted<IntrinsicDescPair*, IntrinsicDescPair::compare>(&pair, found);
}

void Compile::register_intrinsic(CallGenerator* cg) {
  bool found = false;
  int index = intrinsic_insertion_index(cg->method(), cg->is_virtual(), found);
  assert(!found, "registering twice");
  _intrinsics.insert_before(index, cg);
  assert(find_intrinsic(cg->method(), cg->is_virtual()) == cg, "registration worked");
}

CallGenerator* Compile::find_intrinsic(ciMethod* m, bool is_virtual) {
  assert(m->is_loaded(), "don't try this on unloaded methods");
  if (_intrinsics.length() > 0) {
    bool found = false;
    int index = intrinsic_insertion_index(m, is_virtual, found);
     if (found) {
      return _intrinsics.at(index);
    }
  }
  // Lazily create intrinsics for intrinsic IDs well-known in the runtime.
  if (m->intrinsic_id() != vmIntrinsics::_none &&
      m->intrinsic_id() <= vmIntrinsics::LAST_COMPILER_INLINE) {
    CallGenerator* cg = make_vm_intrinsic(m, is_virtual);
    if (cg != NULL) {
      // Save it for next time:
      register_intrinsic(cg);
      return cg;
    } else {
      gather_intrinsic_statistics(m->intrinsic_id(), is_virtual, _intrinsic_disabled);
    }
  }
  return NULL;
}

// Compile::make_vm_intrinsic is defined in library_call.cpp.

#ifndef PRODUCT
// statistics gathering...

juint  Compile::_intrinsic_hist_count[vmIntrinsics::number_of_intrinsics()] = {0};
jubyte Compile::_intrinsic_hist_flags[vmIntrinsics::number_of_intrinsics()] = {0};

inline int as_int(vmIntrinsics::ID id) {
  return vmIntrinsics::as_int(id);
}

bool Compile::gather_intrinsic_statistics(vmIntrinsics::ID id, bool is_virtual, int flags) {
  assert(id > vmIntrinsics::_none && id < vmIntrinsics::ID_LIMIT, "oob");
  int oflags = _intrinsic_hist_flags[as_int(id)];
  assert(flags != 0, "what happened?");
  if (is_virtual) {
    flags |= _intrinsic_virtual;
  }
  bool changed = (flags != oflags);
  if ((flags & _intrinsic_worked) != 0) {
    juint count = (_intrinsic_hist_count[as_int(id)] += 1);
    if (count == 1) {
      changed = true;           // first time
    }
    // increment the overall count also:
    _intrinsic_hist_count[as_int(vmIntrinsics::_none)] += 1;
  }
  if (changed) {
    if (((oflags ^ flags) & _intrinsic_virtual) != 0) {
      // Something changed about the intrinsic's virtuality.
      if ((flags & _intrinsic_virtual) != 0) {
        // This is the first use of this intrinsic as a virtual call.
        if (oflags != 0) {
          // We already saw it as a non-virtual, so note both cases.
          flags |= _intrinsic_both;
        }
      } else if ((oflags & _intrinsic_both) == 0) {
        // This is the first use of this intrinsic as a non-virtual
        flags |= _intrinsic_both;
      }
    }
    _intrinsic_hist_flags[as_int(id)] = (jubyte) (oflags | flags);
  }
  // update the overall flags also:
  _intrinsic_hist_flags[as_int(vmIntrinsics::_none)] |= (jubyte) flags;
  return changed;
}

static char* format_flags(int flags, char* buf) {
  buf[0] = 0;
  if ((flags & Compile::_intrinsic_worked) != 0)    strcat(buf, ",worked");
  if ((flags & Compile::_intrinsic_failed) != 0)    strcat(buf, ",failed");
  if ((flags & Compile::_intrinsic_disabled) != 0)  strcat(buf, ",disabled");
  if ((flags & Compile::_intrinsic_virtual) != 0)   strcat(buf, ",virtual");
  if ((flags & Compile::_intrinsic_both) != 0)      strcat(buf, ",nonvirtual");
  if (buf[0] == 0)  strcat(buf, ",");
  assert(buf[0] == ',', "must be");
  return &buf[1];
}

void Compile::print_intrinsic_statistics() {
  char flagsbuf[100];
  ttyLocker ttyl;
  if (xtty != NULL)  xtty->head("statistics type='intrinsic'");
  tty->print_cr("Compiler intrinsic usage:");
  juint total = _intrinsic_hist_count[as_int(vmIntrinsics::_none)];
  if (total == 0)  total = 1;  // avoid div0 in case of no successes
  #define PRINT_STAT_LINE(name, c, f) \
    tty->print_cr("  %4d (%4.1f%%) %s (%s)", (int)(c), ((c) * 100.0) / total, name, f);
  for (auto id : EnumRange<vmIntrinsicID>{}) {
    int   flags = _intrinsic_hist_flags[as_int(id)];
    juint count = _intrinsic_hist_count[as_int(id)];
    if ((flags | count) != 0) {
      PRINT_STAT_LINE(vmIntrinsics::name_at(id), count, format_flags(flags, flagsbuf));
    }
  }
  PRINT_STAT_LINE("total", total, format_flags(_intrinsic_hist_flags[as_int(vmIntrinsics::_none)], flagsbuf));
  if (xtty != NULL)  xtty->tail("statistics");
}

void Compile::print_statistics() {
  { ttyLocker ttyl;
    if (xtty != NULL)  xtty->head("statistics type='opto'");
    Parse::print_statistics();
    PhaseCCP::print_statistics();
    PhaseRegAlloc::print_statistics();
    PhaseOutput::print_statistics();
    PhasePeephole::print_statistics();
    PhaseIdealLoop::print_statistics();
    if (xtty != NULL)  xtty->tail("statistics");
  }
  if (_intrinsic_hist_flags[as_int(vmIntrinsics::_none)] != 0) {
    // put this under its own <statistics> element.
    print_intrinsic_statistics();
  }
}
#endif //PRODUCT

void Compile::gvn_replace_by(Node* n, Node* nn) {
  for (DUIterator_Last imin, i = n->last_outs(imin); i >= imin; ) {
    Node* use = n->last_out(i);
    bool is_in_table = initial_gvn()->hash_delete(use);
    uint uses_found = 0;
    for (uint j = 0; j < use->len(); j++) {
      if (use->in(j) == n) {
        if (j < use->req())
          use->set_req(j, nn);
        else
          use->set_prec(j, nn);
        uses_found++;
      }
    }
    if (is_in_table) {
      // reinsert into table
      initial_gvn()->hash_find_insert(use);
    }
    record_for_igvn(use);
    i -= uses_found;    // we deleted 1 or more copies of this edge
  }
}


// Identify all nodes that are reachable from below, useful.
// Use breadth-first pass that records state in a Unique_Node_List,
// recursive traversal is slower.
void Compile::identify_useful_nodes(Unique_Node_List &useful) {
  int estimated_worklist_size = live_nodes();
  useful.map( estimated_worklist_size, NULL );  // preallocate space

  // Initialize worklist
  if (root() != NULL)     { useful.push(root()); }
  // If 'top' is cached, declare it useful to preserve cached node
  if( cached_top_node() ) { useful.push(cached_top_node()); }

  // Push all useful nodes onto the list, breadthfirst
  for( uint next = 0; next < useful.size(); ++next ) {
    assert( next < unique(), "Unique useful nodes < total nodes");
    Node *n  = useful.at(next);
    uint max = n->len();
    for( uint i = 0; i < max; ++i ) {
      Node *m = n->in(i);
      if (not_a_node(m))  continue;
      useful.push(m);
    }
  }
}

// Update dead_node_list with any missing dead nodes using useful
// list. Consider all non-useful nodes to be useless i.e., dead nodes.
void Compile::update_dead_node_list(Unique_Node_List &useful) {
  uint max_idx = unique();
  VectorSet& useful_node_set = useful.member_set();

  for (uint node_idx = 0; node_idx < max_idx; node_idx++) {
    // If node with index node_idx is not in useful set,
    // mark it as dead in dead node list.
    if (!useful_node_set.test(node_idx)) {
      record_dead_node(node_idx);
    }
  }
}

void Compile::remove_useless_late_inlines(GrowableArray<CallGenerator*>* inlines, Unique_Node_List &useful) {
  int shift = 0;
  for (int i = 0; i < inlines->length(); i++) {
    CallGenerator* cg = inlines->at(i);
    if (useful.member(cg->call_node())) {
      if (shift > 0) {
        inlines->at_put(i - shift, cg);
      }
    } else {
      shift++; // skip over the dead element
    }
  }
  if (shift > 0) {
    inlines->trunc_to(inlines->length() - shift); // remove last elements from compacted array
  }
}

void Compile::remove_useless_late_inlines(GrowableArray<CallGenerator*>* inlines, Node* dead) {
  assert(dead != NULL && dead->is_Call(), "sanity");
  int found = 0;
  for (int i = 0; i < inlines->length(); i++) {
    if (inlines->at(i)->call_node() == dead) {
      inlines->remove_at(i);
      found++;
      NOT_DEBUG( break; ) // elements are unique, so exit early
    }
  }
  assert(found <= 1, "not unique");
}

void Compile::remove_useless_nodes(GrowableArray<Node*>& node_list, Unique_Node_List& useful) {
  for (int i = node_list.length() - 1; i >= 0; i--) {
    Node* n = node_list.at(i);
    if (!useful.member(n)) {
      node_list.delete_at(i); // replaces i-th with last element which is known to be useful (already processed)
    }
  }
}

void Compile::remove_useless_node(Node* dead) {
  remove_modified_node(dead);

  // Constant node that has no out-edges and has only one in-edge from
  // root is usually dead. However, sometimes reshaping walk makes
  // it reachable by adding use edges. So, we will NOT count Con nodes
  // as dead to be conservative about the dead node count at any
  // given time.
  if (!dead->is_Con()) {
    record_dead_node(dead->_idx);
  }
  if (dead->is_macro()) {
    remove_macro_node(dead);
  }
  if (dead->is_expensive()) {
    remove_expensive_node(dead);
  }
  if (dead->Opcode() == Op_Opaque4) {
    remove_skeleton_predicate_opaq(dead);
  }
  if (dead->for_post_loop_opts_igvn()) {
    remove_from_post_loop_opts_igvn(dead);
  }
  if (dead->is_Call()) {
    remove_useless_late_inlines(                &_late_inlines, dead);
    remove_useless_late_inlines(         &_string_late_inlines, dead);
    remove_useless_late_inlines(         &_boxing_late_inlines, dead);
    remove_useless_late_inlines(&_vector_reboxing_late_inlines, dead);
  }
  BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
  bs->unregister_potential_barrier_node(dead);
}

// Disconnect all useless nodes by disconnecting those at the boundary.
void Compile::remove_useless_nodes(Unique_Node_List &useful) {
  uint next = 0;
  while (next < useful.size()) {
    Node *n = useful.at(next++);
    if (n->is_SafePoint()) {
      // We're done with a parsing phase. Replaced nodes are not valid
      // beyond that point.
      n->as_SafePoint()->delete_replaced_nodes();
    }
    // Use raw traversal of out edges since this code removes out edges
    int max = n->outcnt();
    for (int j = 0; j < max; ++j) {
      Node* child = n->raw_out(j);
      if (!useful.member(child)) {
        assert(!child->is_top() || child != top(),
               "If top is cached in Compile object it is in useful list");
        // Only need to remove this out-edge to the useless node
        n->raw_del_out(j);
        --j;
        --max;
      }
    }
    if (n->outcnt() == 1 && n->has_special_unique_user()) {
      record_for_igvn(n->unique_out());
    }
  }

  remove_useless_nodes(_macro_nodes,        useful); // remove useless macro nodes
  remove_useless_nodes(_predicate_opaqs,    useful); // remove useless predicate opaque nodes
  remove_useless_nodes(_skeleton_predicate_opaqs, useful);
  remove_useless_nodes(_expensive_nodes,    useful); // remove useless expensive nodes
  remove_useless_nodes(_for_post_loop_igvn, useful); // remove useless node recorded for post loop opts IGVN pass
  remove_useless_coarsened_locks(useful);            // remove useless coarsened locks nodes

  BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
  bs->eliminate_useless_gc_barriers(useful, this);
  // clean up the late inline lists
  remove_useless_late_inlines(                &_late_inlines, useful);
  remove_useless_late_inlines(         &_string_late_inlines, useful);
  remove_useless_late_inlines(         &_boxing_late_inlines, useful);
  remove_useless_late_inlines(&_vector_reboxing_late_inlines, useful);
  debug_only(verify_graph_edges(true/*check for no_dead_code*/);)
}

// ============================================================================
//------------------------------CompileWrapper---------------------------------
class CompileWrapper : public StackObj {
  Compile *const _compile;
 public:
  CompileWrapper(Compile* compile);

  ~CompileWrapper();
};

CompileWrapper::CompileWrapper(Compile* compile) : _compile(compile) {
  // the Compile* pointer is stored in the current ciEnv:
  ciEnv* env = compile->env();
  assert(env == ciEnv::current(), "must already be a ciEnv active");
  assert(env->compiler_data() == NULL, "compile already active?");
  env->set_compiler_data(compile);
  assert(compile == Compile::current(), "sanity");

  compile->set_type_dict(NULL);
  compile->set_clone_map(new Dict(cmpkey, hashkey, _compile->comp_arena()));
  compile->clone_map().set_clone_idx(0);
  compile->set_type_last_size(0);
  compile->set_last_tf(NULL, NULL);
  compile->set_indexSet_arena(NULL);
  compile->set_indexSet_free_block_list(NULL);
  compile->init_type_arena();
  Type::Initialize(compile);
  _compile->begin_method();
  _compile->clone_map().set_debug(_compile->has_method() && _compile->directive()->CloneMapDebugOption);
}
CompileWrapper::~CompileWrapper() {
  // simulate crash during compilation
  assert(CICrashAt < 0 || _compile->compile_id() != CICrashAt, "just as planned");

  _compile->end_method();
  _compile->env()->set_compiler_data(NULL);
}


//----------------------------print_compile_messages---------------------------
void Compile::print_compile_messages() {
#ifndef PRODUCT
  // Check if recompiling
  if (_subsume_loads == false && PrintOpto) {
    // Recompiling without allowing machine instructions to subsume loads
    tty->print_cr("*********************************************************");
    tty->print_cr("** Bailout: Recompile without subsuming loads          **");
    tty->print_cr("*********************************************************");
  }
  if (_do_escape_analysis != DoEscapeAnalysis && PrintOpto) {
    // Recompiling without escape analysis
    tty->print_cr("*********************************************************");
    tty->print_cr("** Bailout: Recompile without escape analysis          **");
    tty->print_cr("*********************************************************");
  }
  if (_eliminate_boxing != EliminateAutoBox && PrintOpto) {
    // Recompiling without boxing elimination
    tty->print_cr("*********************************************************");
    tty->print_cr("** Bailout: Recompile without boxing elimination       **");
    tty->print_cr("*********************************************************");
  }
  if ((_do_locks_coarsening != EliminateLocks) && PrintOpto) {
    // Recompiling without locks coarsening
    tty->print_cr("*********************************************************");
    tty->print_cr("** Bailout: Recompile without locks coarsening         **");
    tty->print_cr("*********************************************************");
  }
  if (env()->break_at_compile()) {
    // Open the debugger when compiling this method.
    tty->print("### Breaking when compiling: ");
    method()->print_short_name();
    tty->cr();
    BREAKPOINT;
  }

  if( PrintOpto ) {
    if (is_osr_compilation()) {
      tty->print("[OSR]%3d", _compile_id);
    } else {
      tty->print("%3d", _compile_id);
    }
  }
#endif
}

// ============================================================================
//------------------------------Compile standard-------------------------------
debug_only( int Compile::_debug_idx = 100000; )

// Compile a method.  entry_bci is -1 for normal compilations and indicates
// the continuation bci for on stack replacement.


Compile::Compile( ciEnv* ci_env, ciMethod* target, int osr_bci,
                  bool subsume_loads, bool do_escape_analysis, bool eliminate_boxing,
                  bool do_locks_coarsening, bool install_code, DirectiveSet* directive)
                : Phase(Compiler),
                  _compile_id(ci_env->compile_id()),
                  _subsume_loads(subsume_loads),
                  _do_escape_analysis(do_escape_analysis),
                  _install_code(install_code),
                  _eliminate_boxing(eliminate_boxing),
                  _do_locks_coarsening(do_locks_coarsening),
                  _method(target),
                  _entry_bci(osr_bci),
                  _stub_function(NULL),
                  _stub_name(NULL),
                  _stub_entry_point(NULL),
                  _max_node_limit(MaxNodeLimit),
                  _post_loop_opts_phase(false),
                  _inlining_progress(false),
                  _inlining_incrementally(false),
                  _do_cleanup(false),
                  _has_reserved_stack_access(target->has_reserved_stack_access()),
#ifndef PRODUCT
                  _igv_idx(0),
                  _trace_opto_output(directive->TraceOptoOutputOption),
                  _print_ideal(directive->PrintIdealOption),
#endif
                  _has_method_handle_invokes(false),
                  _clinit_barrier_on_entry(false),
                  _stress_seed(0),
                  _comp_arena(mtCompiler),
                  _barrier_set_state(BarrierSet::barrier_set()->barrier_set_c2()->create_barrier_state(comp_arena())),
                  _env(ci_env),
                  _directive(directive),
                  _log(ci_env->log()),
                  _failure_reason(NULL),
                  _intrinsics        (comp_arena(), 0, 0, NULL),
                  _macro_nodes       (comp_arena(), 8, 0, NULL),
                  _predicate_opaqs   (comp_arena(), 8, 0, NULL),
                  _skeleton_predicate_opaqs (comp_arena(), 8, 0, NULL),
                  _expensive_nodes   (comp_arena(), 8, 0, NULL),
                  _for_post_loop_igvn(comp_arena(), 8, 0, NULL),
                  _coarsened_locks   (comp_arena(), 8, 0, NULL),
                  _congraph(NULL),
                  NOT_PRODUCT(_printer(NULL) COMMA)
                  _dead_node_list(comp_arena()),
                  _dead_node_count(0),
                  _node_arena(mtCompiler),
                  _old_arena(mtCompiler),
                  _mach_constant_base_node(NULL),
                  _Compile_types(mtCompiler),
                  _initial_gvn(NULL),
                  _for_igvn(NULL),
                  _late_inlines(comp_arena(), 2, 0, NULL),
                  _string_late_inlines(comp_arena(), 2, 0, NULL),
                  _boxing_late_inlines(comp_arena(), 2, 0, NULL),
                  _vector_reboxing_late_inlines(comp_arena(), 2, 0, NULL),
                  _late_inlines_pos(0),
                  _number_of_mh_late_inlines(0),
                  _native_invokers(comp_arena(), 1, 0, NULL),
                  _print_inlining_stream(NULL),
                  _print_inlining_list(NULL),
                  _print_inlining_idx(0),
                  _print_inlining_output(NULL),
                  _replay_inline_data(NULL),
                  _java_calls(0),
                  _inner_loops(0),
                  _interpreter_frame_size(0)
#ifndef PRODUCT
                  , _in_dump_cnt(0)
#endif
{
  C = this;
  CompileWrapper cw(this);

  if (CITimeVerbose) {
    tty->print(" ");
    target->holder()->name()->print();
    tty->print(".");
    target->print_short_name();
    tty->print("  ");
  }
  TraceTime t1("Total compilation time", &_t_totalCompilation, CITime, CITimeVerbose);
  TraceTime t2(NULL, &_t_methodCompilation, CITime, false);

#if defined(SUPPORT_ASSEMBLY) || defined(SUPPORT_ABSTRACT_ASSEMBLY)
  bool print_opto_assembly = directive->PrintOptoAssemblyOption;
  // We can always print a disassembly, either abstract (hex dump) or
  // with the help of a suitable hsdis library. Thus, we should not
  // couple print_assembly and print_opto_assembly controls.
  // But: always print opto and regular assembly on compile command 'print'.
  bool print_assembly = directive->PrintAssemblyOption;
  set_print_assembly(print_opto_assembly || print_assembly);
#else
  set_print_assembly(false); // must initialize.
#endif

#ifndef PRODUCT
  set_parsed_irreducible_loop(false);

  if (directive->ReplayInlineOption) {
    _replay_inline_data = ciReplay::load_inline_data(method(), entry_bci(), ci_env->comp_level());
  }
#endif
  set_print_inlining(directive->PrintInliningOption || PrintOptoInlining);
  set_print_intrinsics(directive->PrintIntrinsicsOption);
  set_has_irreducible_loop(true); // conservative until build_loop_tree() reset it

  if (ProfileTraps RTM_OPT_ONLY( || UseRTMLocking )) {
    // Make sure the method being compiled gets its own MDO,
    // so we can at least track the decompile_count().
    // Need MDO to record RTM code generation state.
    method()->ensure_method_data();
  }

  Init(::AliasLevel);


  print_compile_messages();

  _ilt = InlineTree::build_inline_tree_root();

  // Even if NO memory addresses are used, MergeMem nodes must have at least 1 slice
  assert(num_alias_types() >= AliasIdxRaw, "");

#define MINIMUM_NODE_HASH  1023
  // Node list that Iterative GVN will start with
  Unique_Node_List for_igvn(comp_arena());
  set_for_igvn(&for_igvn);

  // GVN that will be run immediately on new nodes
  uint estimated_size = method()->code_size()*4+64;
  estimated_size = (estimated_size < MINIMUM_NODE_HASH ? MINIMUM_NODE_HASH : estimated_size);
  PhaseGVN gvn(node_arena(), estimated_size);
  set_initial_gvn(&gvn);

  print_inlining_init();
  { // Scope for timing the parser
    TracePhase tp("parse", &timers[_t_parser]);

    // Put top into the hash table ASAP.
    initial_gvn()->transform_no_reclaim(top());

    // Set up tf(), start(), and find a CallGenerator.
    CallGenerator* cg = NULL;
    if (is_osr_compilation()) {
      const TypeTuple *domain = StartOSRNode::osr_domain();
      const TypeTuple *range = TypeTuple::make_range(method()->signature());
      init_tf(TypeFunc::make(domain, range));
      StartNode* s = new StartOSRNode(root(), domain);
      initial_gvn()->set_type_bottom(s);
      init_start(s);
      cg = CallGenerator::for_osr(method(), entry_bci());
    } else {
      // Normal case.
      init_tf(TypeFunc::make(method()));
      StartNode* s = new StartNode(root(), tf()->domain());
      initial_gvn()->set_type_bottom(s);
      init_start(s);
      if (method()->intrinsic_id() == vmIntrinsics::_Reference_get) {
        // With java.lang.ref.reference.get() we must go through the
        // intrinsic - even when get() is the root
        // method of the compile - so that, if necessary, the value in
        // the referent field of the reference object gets recorded by
        // the pre-barrier code.
        cg = find_intrinsic(method(), false);
      }
      if (cg == NULL) {
        float past_uses = method()->interpreter_invocation_count();
        float expected_uses = past_uses;
        cg = CallGenerator::for_inline(method(), expected_uses);
      }
    }
    if (failing())  return;
    if (cg == NULL) {
      record_method_not_compilable("cannot parse method");
      return;
    }
    JVMState* jvms = build_start_state(start(), tf());
    if ((jvms = cg->generate(jvms)) == NULL) {
      if (!failure_reason_is(C2Compiler::retry_class_loading_during_parsing())) {
        record_method_not_compilable("method parse failed");
      }
      return;
    }
    GraphKit kit(jvms);

    if (!kit.stopped()) {
      // Accept return values, and transfer control we know not where.
      // This is done by a special, unique ReturnNode bound to root.
      return_values(kit.jvms());
    }

    if (kit.has_exceptions()) {
      // Any exceptions that escape from this call must be rethrown
      // to whatever caller is dynamically above us on the stack.
      // This is done by a special, unique RethrowNode bound to root.
      rethrow_exceptions(kit.transfer_exceptions_into_jvms());
    }

    assert(IncrementalInline || (_late_inlines.length() == 0 && !has_mh_late_inlines()), "incremental inlining is off");

    if (_late_inlines.length() == 0 && !has_mh_late_inlines() && !failing() && has_stringbuilder()) {
      inline_string_calls(true);
    }

    if (failing())  return;

    print_method(PHASE_BEFORE_REMOVEUSELESS, 3);

    // Remove clutter produced by parsing.
    if (!failing()) {
      ResourceMark rm;
      PhaseRemoveUseless pru(initial_gvn(), &for_igvn);
    }
  }

  // Note:  Large methods are capped off in do_one_bytecode().
  if (failing())  return;

  // After parsing, node notes are no longer automagic.
  // They must be propagated by register_new_node_with_optimizer(),
  // clone(), or the like.
  set_default_node_notes(NULL);

#ifndef PRODUCT
  if (should_print(1)) {
    _printer->print_inlining();
  }
#endif

  if (failing())  return;
  NOT_PRODUCT( verify_graph_edges(); )

  // If any phase is randomized for stress testing, seed random number
  // generation and log the seed for repeatability.
  if (StressLCM || StressGCM || StressIGVN || StressCCP) {
    _stress_seed = FLAG_IS_DEFAULT(StressSeed) ?
      static_cast<uint>(Ticks::now().nanoseconds()) : StressSeed;
    if (_log != NULL) {
      _log->elem("stress_test seed='%u'", _stress_seed);
    }
  }

  // Now optimize
  Optimize();
  if (failing())  return;
  NOT_PRODUCT( verify_graph_edges(); )

#ifndef PRODUCT
  if (print_ideal()) {
    ttyLocker ttyl;  // keep the following output all in one block
    // This output goes directly to the tty, not the compiler log.
    // To enable tools to match it up with the compilation activity,
    // be sure to tag this tty output with the compile ID.
    if (xtty != NULL) {
      xtty->head("ideal compile_id='%d'%s", compile_id(),
                 is_osr_compilation()    ? " compile_kind='osr'" :
                 "");
    }
    root()->dump(9999);
    if (xtty != NULL) {
      xtty->tail("ideal");
    }
  }
#endif

#ifdef ASSERT
  BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
  bs->verify_gc_barriers(this, BarrierSetC2::BeforeCodeGen);
#endif

  // Dump compilation data to replay it.
  if (directive->DumpReplayOption) {
    env()->dump_replay_data(_compile_id);
  }
  if (directive->DumpInlineOption && (ilt() != NULL)) {
    env()->dump_inline_data(_compile_id);
  }

  // Now that we know the size of all the monitors we can add a fixed slot
  // for the original deopt pc.
  int next_slot = fixed_slots() + (sizeof(address) / VMRegImpl::stack_slot_size);
  set_fixed_slots(next_slot);

  // Compute when to use implicit null checks. Used by matching trap based
  // nodes and NullCheck optimization.
  set_allowed_deopt_reasons();

  // Now generate code
  Code_Gen();
}

//------------------------------Compile----------------------------------------
// Compile a runtime stub
Compile::Compile( ciEnv* ci_env,
                  TypeFunc_generator generator,
                  address stub_function,
                  const char *stub_name,
                  int is_fancy_jump,
                  bool pass_tls,
                  bool return_pc,
                  DirectiveSet* directive)
  : Phase(Compiler),
    _compile_id(0),
    _subsume_loads(true),
    _do_escape_analysis(false),
    _install_code(true),
    _eliminate_boxing(false),
    _do_locks_coarsening(false),
    _method(NULL),
    _entry_bci(InvocationEntryBci),
    _stub_function(stub_function),
    _stub_name(stub_name),
    _stub_entry_point(NULL),
    _max_node_limit(MaxNodeLimit),
    _post_loop_opts_phase(false),
    _inlining_progress(false),
    _inlining_incrementally(false),
    _has_reserved_stack_access(false),
#ifndef PRODUCT
    _igv_idx(0),
    _trace_opto_output(directive->TraceOptoOutputOption),
    _print_ideal(directive->PrintIdealOption),
#endif
    _has_method_handle_invokes(false),
    _clinit_barrier_on_entry(false),
    _stress_seed(0),
    _comp_arena(mtCompiler),
    _barrier_set_state(BarrierSet::barrier_set()->barrier_set_c2()->create_barrier_state(comp_arena())),
    _env(ci_env),
    _directive(directive),
    _log(ci_env->log()),
    _failure_reason(NULL),
    _congraph(NULL),
    NOT_PRODUCT(_printer(NULL) COMMA)
    _dead_node_list(comp_arena()),
    _dead_node_count(0),
    _node_arena(mtCompiler),
    _old_arena(mtCompiler),
    _mach_constant_base_node(NULL),
    _Compile_types(mtCompiler),
    _initial_gvn(NULL),
    _for_igvn(NULL),
    _number_of_mh_late_inlines(0),
    _native_invokers(),
    _print_inlining_stream(NULL),
    _print_inlining_list(NULL),
    _print_inlining_idx(0),
    _print_inlining_output(NULL),
    _replay_inline_data(NULL),
    _java_calls(0),
    _inner_loops(0),
    _interpreter_frame_size(0),
#ifndef PRODUCT
    _in_dump_cnt(0),
#endif
    _allowed_reasons(0) {
  C = this;

  TraceTime t1(NULL, &_t_totalCompilation, CITime, false);
  TraceTime t2(NULL, &_t_stubCompilation, CITime, false);

#ifndef PRODUCT
  set_print_assembly(PrintFrameConverterAssembly);
  set_parsed_irreducible_loop(false);
#else
  set_print_assembly(false); // Must initialize.
#endif
  set_has_irreducible_loop(false); // no loops

  CompileWrapper cw(this);
  Init(/*AliasLevel=*/ 0);
  init_tf((*generator)());

  {
    // The following is a dummy for the sake of GraphKit::gen_stub
    Unique_Node_List for_igvn(comp_arena());
    set_for_igvn(&for_igvn);  // not used, but some GraphKit guys push on this
    PhaseGVN gvn(Thread::current()->resource_area(),255);
    set_initial_gvn(&gvn);    // not significant, but GraphKit guys use it pervasively
    gvn.transform_no_reclaim(top());

    GraphKit kit;
    kit.gen_stub(stub_function, stub_name, is_fancy_jump, pass_tls, return_pc);
  }

  NOT_PRODUCT( verify_graph_edges(); )

  Code_Gen();
}

//------------------------------Init-------------------------------------------
// Prepare for a single compilation
void Compile::Init(int aliaslevel) {
  _unique  = 0;
  _regalloc = NULL;

  _tf      = NULL;  // filled in later
  _top     = NULL;  // cached later
  _matcher = NULL;  // filled in later
  _cfg     = NULL;  // filled in later

  IA32_ONLY( set_24_bit_selection_and_mode(true, false); )

  _node_note_array = NULL;
  _default_node_notes = NULL;
  DEBUG_ONLY( _modified_nodes = NULL; ) // Used in Optimize()

  _immutable_memory = NULL; // filled in at first inquiry

  // Globally visible Nodes
  // First set TOP to NULL to give safe behavior during creation of RootNode
  set_cached_top_node(NULL);
  set_root(new RootNode());
  // Now that you have a Root to point to, create the real TOP
  set_cached_top_node( new ConNode(Type::TOP) );
  set_recent_alloc(NULL, NULL);

  // Create Debug Information Recorder to record scopes, oopmaps, etc.
  env()->set_oop_recorder(new OopRecorder(env()->arena()));
  env()->set_debug_info(new DebugInformationRecorder(env()->oop_recorder()));
  env()->set_dependencies(new Dependencies(env()));

  _fixed_slots = 0;
  set_has_split_ifs(false);
  set_has_loops(false); // first approximation
  set_has_stringbuilder(false);
  set_has_boxed_value(false);
  _trap_can_recompile = false;  // no traps emitted yet
  _major_progress = true; // start out assuming good things will happen
  set_has_unsafe_access(false);
  set_max_vector_size(0);
  set_clear_upper_avx(false);  //false as default for clear upper bits of ymm registers
  Copy::zero_to_bytes(_trap_hist, sizeof(_trap_hist));
  set_decompile_count(0);

  set_do_freq_based_layout(_directive->BlockLayoutByFrequencyOption);
  _loop_opts_cnt = LoopOptsCount;
  set_do_inlining(Inline);
  set_max_inline_size(MaxInlineSize);
  set_freq_inline_size(FreqInlineSize);
  set_do_scheduling(OptoScheduling);

  set_do_vector_loop(false);

  if (AllowVectorizeOnDemand) {
    if (has_method() && (_directive->VectorizeOption || _directive->VectorizeDebugOption)) {
      set_do_vector_loop(true);
      NOT_PRODUCT(if (do_vector_loop() && Verbose) {tty->print("Compile::Init: do vectorized loops (SIMD like) for method %s\n",  method()->name()->as_quoted_ascii());})
    } else if (has_method() && method()->name() != 0 &&
               method()->intrinsic_id() == vmIntrinsics::_forEachRemaining) {
      set_do_vector_loop(true);
    }
  }
  set_use_cmove(UseCMoveUnconditionally /* || do_vector_loop()*/); //TODO: consider do_vector_loop() mandate use_cmove unconditionally
  NOT_PRODUCT(if (use_cmove() && Verbose && has_method()) {tty->print("Compile::Init: use CMove without profitability tests for method %s\n",  method()->name()->as_quoted_ascii());})

  set_age_code(has_method() && method()->profile_aging());
  set_rtm_state(NoRTM); // No RTM lock eliding by default
  _max_node_limit = _directive->MaxNodeLimitOption;

#if INCLUDE_RTM_OPT
  if (UseRTMLocking && has_method() && (method()->method_data_or_null() != NULL)) {
    int rtm_state = method()->method_data()->rtm_state();
    if (method_has_option(CompileCommand::NoRTMLockEliding) || ((rtm_state & NoRTM) != 0)) {
      // Don't generate RTM lock eliding code.
      set_rtm_state(NoRTM);
    } else if (method_has_option(CompileCommand::UseRTMLockEliding) || ((rtm_state & UseRTM) != 0) || !UseRTMDeopt) {
      // Generate RTM lock eliding code without abort ratio calculation code.
      set_rtm_state(UseRTM);
    } else if (UseRTMDeopt) {
      // Generate RTM lock eliding code and include abort ratio calculation
      // code if UseRTMDeopt is on.
      set_rtm_state(ProfileRTM);
    }
  }
#endif
  if (VM_Version::supports_fast_class_init_checks() && has_method() && !is_osr_compilation() && method()->needs_clinit_barrier()) {
    set_clinit_barrier_on_entry(true);
  }
  if (debug_info()->recording_non_safepoints()) {
    set_node_note_array(new(comp_arena()) GrowableArray<Node_Notes*>
                        (comp_arena(), 8, 0, NULL));
    set_default_node_notes(Node_Notes::make(this));
  }

  // // -- Initialize types before each compile --
  // // Update cached type information
  // if( _method && _method->constants() )
  //   Type::update_loaded_types(_method, _method->constants());

  // Init alias_type map.
  if (!_do_escape_analysis && aliaslevel == 3)
    aliaslevel = 2;  // No unique types without escape analysis
  _AliasLevel = aliaslevel;
  const int grow_ats = 16;
  _max_alias_types = grow_ats;
  _alias_types   = NEW_ARENA_ARRAY(comp_arena(), AliasType*, grow_ats);
  AliasType* ats = NEW_ARENA_ARRAY(comp_arena(), AliasType,  grow_ats);
  Copy::zero_to_bytes(ats, sizeof(AliasType)*grow_ats);
  {
    for (int i = 0; i < grow_ats; i++)  _alias_types[i] = &ats[i];
  }
  // Initialize the first few types.
  _alias_types[AliasIdxTop]->Init(AliasIdxTop, NULL);
  _alias_types[AliasIdxBot]->Init(AliasIdxBot, TypePtr::BOTTOM);
  _alias_types[AliasIdxRaw]->Init(AliasIdxRaw, TypeRawPtr::BOTTOM);
  _num_alias_types = AliasIdxRaw+1;
  // Zero out the alias type cache.
  Copy::zero_to_bytes(_alias_cache, sizeof(_alias_cache));
  // A NULL adr_type hits in the cache right away.  Preload the right answer.
  probe_alias_cache(NULL)->_index = AliasIdxTop;

#ifdef ASSERT
  _type_verify_symmetry = true;
  _phase_optimize_finished = false;
  _exception_backedge = false;
#endif
}

//---------------------------init_start----------------------------------------
// Install the StartNode on this compile object.
void Compile::init_start(StartNode* s) {
  if (failing())
    return; // already failing
  assert(s == start(), "");
}

/**
 * Return the 'StartNode'. We must not have a pending failure, since the ideal graph
 * can be in an inconsistent state, i.e., we can get segmentation faults when traversing
 * the ideal graph.
 */
StartNode* Compile::start() const {
  assert (!failing(), "Must not have pending failure. Reason is: %s", failure_reason());
  for (DUIterator_Fast imax, i = root()->fast_outs(imax); i < imax; i++) {
    Node* start = root()->fast_out(i);
    if (start->is_Start()) {
      return start->as_Start();
    }
  }
  fatal("Did not find Start node!");
  return NULL;
}

//-------------------------------immutable_memory-------------------------------------
// Access immutable memory
Node* Compile::immutable_memory() {
  if (_immutable_memory != NULL) {
    return _immutable_memory;
  }
  StartNode* s = start();
  for (DUIterator_Fast imax, i = s->fast_outs(imax); true; i++) {
    Node *p = s->fast_out(i);
    if (p != s && p->as_Proj()->_con == TypeFunc::Memory) {
      _immutable_memory = p;
      return _immutable_memory;
    }
  }
  ShouldNotReachHere();
  return NULL;
}

//----------------------set_cached_top_node------------------------------------
// Install the cached top node, and make sure Node::is_top works correctly.
void Compile::set_cached_top_node(Node* tn) {
  if (tn != NULL)  verify_top(tn);
  Node* old_top = _top;
  _top = tn;
  // Calling Node::setup_is_top allows the nodes the chance to adjust
  // their _out arrays.
  if (_top != NULL)     _top->setup_is_top();
  if (old_top != NULL)  old_top->setup_is_top();
  assert(_top == NULL || top()->is_top(), "");
}

#ifdef ASSERT
uint Compile::count_live_nodes_by_graph_walk() {
  Unique_Node_List useful(comp_arena());
  // Get useful node list by walking the graph.
  identify_useful_nodes(useful);
  return useful.size();
}

void Compile::print_missing_nodes() {

  // Return if CompileLog is NULL and PrintIdealNodeCount is false.
  if ((_log == NULL) && (! PrintIdealNodeCount)) {
    return;
  }

  // This is an expensive function. It is executed only when the user
  // specifies VerifyIdealNodeCount option or otherwise knows the
  // additional work that needs to be done to identify reachable nodes
  // by walking the flow graph and find the missing ones using
  // _dead_node_list.

  Unique_Node_List useful(comp_arena());
  // Get useful node list by walking the graph.
  identify_useful_nodes(useful);

  uint l_nodes = C->live_nodes();
  uint l_nodes_by_walk = useful.size();

  if (l_nodes != l_nodes_by_walk) {
    if (_log != NULL) {
      _log->begin_head("mismatched_nodes count='%d'", abs((int) (l_nodes - l_nodes_by_walk)));
      _log->stamp();
      _log->end_head();
    }
    VectorSet& useful_member_set = useful.member_set();
    int last_idx = l_nodes_by_walk;
    for (int i = 0; i < last_idx; i++) {
      if (useful_member_set.test(i)) {
        if (_dead_node_list.test(i)) {
          if (_log != NULL) {
            _log->elem("mismatched_node_info node_idx='%d' type='both live and dead'", i);
          }
          if (PrintIdealNodeCount) {
            // Print the log message to tty
              tty->print_cr("mismatched_node idx='%d' both live and dead'", i);
              useful.at(i)->dump();
          }
        }
      }
      else if (! _dead_node_list.test(i)) {
        if (_log != NULL) {
          _log->elem("mismatched_node_info node_idx='%d' type='neither live nor dead'", i);
        }
        if (PrintIdealNodeCount) {
          // Print the log message to tty
          tty->print_cr("mismatched_node idx='%d' type='neither live nor dead'", i);
        }
      }
    }
    if (_log != NULL) {
      _log->tail("mismatched_nodes");
    }
  }
}
void Compile::record_modified_node(Node* n) {
  if (_modified_nodes != NULL && !_inlining_incrementally && !n->is_Con()) {
    _modified_nodes->push(n);
  }
}

void Compile::remove_modified_node(Node* n) {
  if (_modified_nodes != NULL) {
    _modified_nodes->remove(n);
  }
}
#endif

#ifndef PRODUCT
void Compile::verify_top(Node* tn) const {
  if (tn != NULL) {
    assert(tn->is_Con(), "top node must be a constant");
    assert(((ConNode*)tn)->type() == Type::TOP, "top node must have correct type");
    assert(tn->in(0) != NULL, "must have live top node");
  }
}
#endif


///-------------------Managing Per-Node Debug & Profile Info-------------------

void Compile::grow_node_notes(GrowableArray<Node_Notes*>* arr, int grow_by) {
  guarantee(arr != NULL, "");
  int num_blocks = arr->length();
  if (grow_by < num_blocks)  grow_by = num_blocks;
  int num_notes = grow_by * _node_notes_block_size;
  Node_Notes* notes = NEW_ARENA_ARRAY(node_arena(), Node_Notes, num_notes);
  Copy::zero_to_bytes(notes, num_notes * sizeof(Node_Notes));
  while (num_notes > 0) {
    arr->append(notes);
    notes     += _node_notes_block_size;
    num_notes -= _node_notes_block_size;
  }
  assert(num_notes == 0, "exact multiple, please");
}

bool Compile::copy_node_notes_to(Node* dest, Node* source) {
  if (source == NULL || dest == NULL)  return false;

  if (dest->is_Con())
    return false;               // Do not push debug info onto constants.

#ifdef ASSERT
  // Leave a bread crumb trail pointing to the original node:
  if (dest != NULL && dest != source && dest->debug_orig() == NULL) {
    dest->set_debug_orig(source);
  }
#endif

  if (node_note_array() == NULL)
    return false;               // Not collecting any notes now.

  // This is a copy onto a pre-existing node, which may already have notes.
  // If both nodes have notes, do not overwrite any pre-existing notes.
  Node_Notes* source_notes = node_notes_at(source->_idx);
  if (source_notes == NULL || source_notes->is_clear())  return false;
  Node_Notes* dest_notes   = node_notes_at(dest->_idx);
  if (dest_notes == NULL || dest_notes->is_clear()) {
    return set_node_notes_at(dest->_idx, source_notes);
  }

  Node_Notes merged_notes = (*source_notes);
  // The order of operations here ensures that dest notes will win...
  merged_notes.update_from(dest_notes);
  return set_node_notes_at(dest->_idx, &merged_notes);
}


//--------------------------allow_range_check_smearing-------------------------
// Gating condition for coalescing similar range checks.
// Sometimes we try 'speculatively' replacing a series of a range checks by a
// single covering check that is at least as strong as any of them.
// If the optimization succeeds, the simplified (strengthened) range check
// will always succeed.  If it fails, we will deopt, and then give up
// on the optimization.
bool Compile::allow_range_check_smearing() const {
  // If this method has already thrown a range-check,
  // assume it was because we already tried range smearing
  // and it failed.
  uint already_trapped = trap_count(Deoptimization::Reason_range_check);
  return !already_trapped;
}


//------------------------------flatten_alias_type-----------------------------
const TypePtr *Compile::flatten_alias_type( const TypePtr *tj ) const {
  int offset = tj->offset();
  TypePtr::PTR ptr = tj->ptr();

  // Known instance (scalarizable allocation) alias only with itself.
  bool is_known_inst = tj->isa_oopptr() != NULL &&
                       tj->is_oopptr()->is_known_instance();

  // Process weird unsafe references.
  if (offset == Type::OffsetBot && (tj->isa_instptr() /*|| tj->isa_klassptr()*/)) {
    assert(InlineUnsafeOps, "indeterminate pointers come only from unsafe ops");
    assert(!is_known_inst, "scalarizable allocation should not have unsafe references");
    tj = TypeOopPtr::BOTTOM;
    ptr = tj->ptr();
    offset = tj->offset();
  }

  // Array pointers need some flattening
  const TypeAryPtr *ta = tj->isa_aryptr();
  if (ta && ta->is_stable()) {
    // Erase stability property for alias analysis.
    tj = ta = ta->cast_to_stable(false);
  }
  if( ta && is_known_inst ) {
    if ( offset != Type::OffsetBot &&
         offset > arrayOopDesc::length_offset_in_bytes() ) {
      offset = Type::OffsetBot; // Flatten constant access into array body only
      tj = ta = TypeAryPtr::make(ptr, ta->ary(), ta->klass(), true, offset, ta->instance_id());
    }
  } else if( ta && _AliasLevel >= 2 ) {
    // For arrays indexed by constant indices, we flatten the alias
    // space to include all of the array body.  Only the header, klass
    // and array length can be accessed un-aliased.
    if( offset != Type::OffsetBot ) {
      if( ta->const_oop() ) { // MethodData* or Method*
        offset = Type::OffsetBot;   // Flatten constant access into array body
        tj = ta = TypeAryPtr::make(ptr,ta->const_oop(),ta->ary(),ta->klass(),false,offset);
      } else if( offset == arrayOopDesc::length_offset_in_bytes() ) {
        // range is OK as-is.
        tj = ta = TypeAryPtr::RANGE;
      } else if( offset == oopDesc::klass_offset_in_bytes() ) {
        tj = TypeInstPtr::KLASS; // all klass loads look alike
        ta = TypeAryPtr::RANGE; // generic ignored junk
        ptr = TypePtr::BotPTR;
      } else if( offset == oopDesc::mark_offset_in_bytes() ) {
        tj = TypeInstPtr::MARK;
        ta = TypeAryPtr::RANGE; // generic ignored junk
        ptr = TypePtr::BotPTR;
      } else {                  // Random constant offset into array body
        offset = Type::OffsetBot;   // Flatten constant access into array body
        tj = ta = TypeAryPtr::make(ptr,ta->ary(),ta->klass(),false,offset);
      }
    }
    // Arrays of fixed size alias with arrays of unknown size.
    if (ta->size() != TypeInt::POS) {
      const TypeAry *tary = TypeAry::make(ta->elem(), TypeInt::POS);
      tj = ta = TypeAryPtr::make(ptr,ta->const_oop(),tary,ta->klass(),false,offset);
    }
    // Arrays of known objects become arrays of unknown objects.
    if (ta->elem()->isa_narrowoop() && ta->elem() != TypeNarrowOop::BOTTOM) {
      const TypeAry *tary = TypeAry::make(TypeNarrowOop::BOTTOM, ta->size());
      tj = ta = TypeAryPtr::make(ptr,ta->const_oop(),tary,NULL,false,offset);
    }
    if (ta->elem()->isa_oopptr() && ta->elem() != TypeInstPtr::BOTTOM) {
      const TypeAry *tary = TypeAry::make(TypeInstPtr::BOTTOM, ta->size());
      tj = ta = TypeAryPtr::make(ptr,ta->const_oop(),tary,NULL,false,offset);
    }
    // Arrays of bytes and of booleans both use 'bastore' and 'baload' so
    // cannot be distinguished by bytecode alone.
    if (ta->elem() == TypeInt::BOOL) {
      const TypeAry *tary = TypeAry::make(TypeInt::BYTE, ta->size());
      ciKlass* aklass = ciTypeArrayKlass::make(T_BYTE);
      tj = ta = TypeAryPtr::make(ptr,ta->const_oop(),tary,aklass,false,offset);
    }
    // During the 2nd round of IterGVN, NotNull castings are removed.
    // Make sure the Bottom and NotNull variants alias the same.
    // Also, make sure exact and non-exact variants alias the same.
    if (ptr == TypePtr::NotNull || ta->klass_is_exact() || ta->speculative() != NULL) {
      tj = ta = TypeAryPtr::make(TypePtr::BotPTR,ta->ary(),ta->klass(),false,offset);
    }
  }

  // Oop pointers need some flattening
  const TypeInstPtr *to = tj->isa_instptr();
  if( to && _AliasLevel >= 2 && to != TypeOopPtr::BOTTOM ) {
    ciInstanceKlass *k = to->klass()->as_instance_klass();
    if( ptr == TypePtr::Constant ) {
      if (to->klass() != ciEnv::current()->Class_klass() ||
          offset < k->size_helper() * wordSize) {
        // No constant oop pointers (such as Strings); they alias with
        // unknown strings.
        assert(!is_known_inst, "not scalarizable allocation");
        tj = to = TypeInstPtr::make(TypePtr::BotPTR,to->klass(),false,0,offset);
      }
    } else if( is_known_inst ) {
      tj = to; // Keep NotNull and klass_is_exact for instance type
    } else if( ptr == TypePtr::NotNull || to->klass_is_exact() ) {
      // During the 2nd round of IterGVN, NotNull castings are removed.
      // Make sure the Bottom and NotNull variants alias the same.
      // Also, make sure exact and non-exact variants alias the same.
      tj = to = TypeInstPtr::make(TypePtr::BotPTR,to->klass(),false,0,offset);
    }
    if (to->speculative() != NULL) {
      tj = to = TypeInstPtr::make(to->ptr(),to->klass(),to->klass_is_exact(),to->const_oop(),to->offset(), to->instance_id());
    }
    // Canonicalize the holder of this field
    if (offset >= 0 && offset < instanceOopDesc::base_offset_in_bytes()) {
      // First handle header references such as a LoadKlassNode, even if the
      // object's klass is unloaded at compile time (4965979).
      if (!is_known_inst) { // Do it only for non-instance types
        tj = to = TypeInstPtr::make(TypePtr::BotPTR, env()->Object_klass(), false, NULL, offset);
      }
    } else if (offset < 0 || offset >= k->size_helper() * wordSize) {
      // Static fields are in the space above the normal instance
      // fields in the java.lang.Class instance.
      if (to->klass() != ciEnv::current()->Class_klass()) {
        to = NULL;
        tj = TypeOopPtr::BOTTOM;
        offset = tj->offset();
      }
    } else {
      ciInstanceKlass *canonical_holder = k->get_canonical_holder(offset);
      if (!k->equals(canonical_holder) || tj->offset() != offset) {
        if( is_known_inst ) {
          tj = to = TypeInstPtr::make(to->ptr(), canonical_holder, true, NULL, offset, to->instance_id());
        } else {
          tj = to = TypeInstPtr::make(to->ptr(), canonical_holder, false, NULL, offset);
        }
      }
    }
  }

  // Klass pointers to object array klasses need some flattening
  const TypeKlassPtr *tk = tj->isa_klassptr();
  if( tk ) {
    // If we are referencing a field within a Klass, we need
    // to assume the worst case of an Object.  Both exact and
    // inexact types must flatten to the same alias class so
    // use NotNull as the PTR.
    if ( offset == Type::OffsetBot || (offset >= 0 && (size_t)offset < sizeof(Klass)) ) {

      tj = tk = TypeKlassPtr::make(TypePtr::NotNull,
                                   TypeKlassPtr::OBJECT->klass(),
                                   offset);
    }

    ciKlass* klass = tk->klass();
    if( klass->is_obj_array_klass() ) {
      ciKlass* k = TypeAryPtr::OOPS->klass();
      if( !k || !k->is_loaded() )                  // Only fails for some -Xcomp runs
        k = TypeInstPtr::BOTTOM->klass();
      tj = tk = TypeKlassPtr::make( TypePtr::NotNull, k, offset );
    }

    // Check for precise loads from the primary supertype array and force them
    // to the supertype cache alias index.  Check for generic array loads from
    // the primary supertype array and also force them to the supertype cache
    // alias index.  Since the same load can reach both, we need to merge
    // these 2 disparate memories into the same alias class.  Since the
    // primary supertype array is read-only, there's no chance of confusion
    // where we bypass an array load and an array store.
    int primary_supers_offset = in_bytes(Klass::primary_supers_offset());
    if (offset == Type::OffsetBot ||
        (offset >= primary_supers_offset &&
         offset < (int)(primary_supers_offset + Klass::primary_super_limit() * wordSize)) ||
        offset == (int)in_bytes(Klass::secondary_super_cache_offset())) {
      offset = in_bytes(Klass::secondary_super_cache_offset());
      tj = tk = TypeKlassPtr::make( TypePtr::NotNull, tk->klass(), offset );
    }
  }

  // Flatten all Raw pointers together.
  if (tj->base() == Type::RawPtr)
    tj = TypeRawPtr::BOTTOM;

  if (tj->base() == Type::AnyPtr)
    tj = TypePtr::BOTTOM;      // An error, which the caller must check for.

  // Flatten all to bottom for now
  switch( _AliasLevel ) {
  case 0:
    tj = TypePtr::BOTTOM;
    break;
  case 1:                       // Flatten to: oop, static, field or array
    switch (tj->base()) {
    //case Type::AryPtr: tj = TypeAryPtr::RANGE;    break;
    case Type::RawPtr:   tj = TypeRawPtr::BOTTOM;   break;
    case Type::AryPtr:   // do not distinguish arrays at all
    case Type::InstPtr:  tj = TypeInstPtr::BOTTOM;  break;
    case Type::KlassPtr: tj = TypeKlassPtr::OBJECT; break;
    case Type::AnyPtr:   tj = TypePtr::BOTTOM;      break;  // caller checks it
    default: ShouldNotReachHere();
    }
    break;
  case 2:                       // No collapsing at level 2; keep all splits
  case 3:                       // No collapsing at level 3; keep all splits
    break;
  default:
    Unimplemented();
  }

  offset = tj->offset();
  assert( offset != Type::OffsetTop, "Offset has fallen from constant" );

  assert( (offset != Type::OffsetBot && tj->base() != Type::AryPtr) ||
          (offset == Type::OffsetBot && tj->base() == Type::AryPtr) ||
          (offset == Type::OffsetBot && tj == TypeOopPtr::BOTTOM) ||
          (offset == Type::OffsetBot && tj == TypePtr::BOTTOM) ||
          (offset == oopDesc::mark_offset_in_bytes() && tj->base() == Type::AryPtr) ||
          (offset == oopDesc::klass_offset_in_bytes() && tj->base() == Type::AryPtr) ||
          (offset == arrayOopDesc::length_offset_in_bytes() && tj->base() == Type::AryPtr),
          "For oops, klasses, raw offset must be constant; for arrays the offset is never known" );
  assert( tj->ptr() != TypePtr::TopPTR &&
          tj->ptr() != TypePtr::AnyNull &&
          tj->ptr() != TypePtr::Null, "No imprecise addresses" );
//    assert( tj->ptr() != TypePtr::Constant ||
//            tj->base() == Type::RawPtr ||
//            tj->base() == Type::KlassPtr, "No constant oop addresses" );

  return tj;
}

void Compile::AliasType::Init(int i, const TypePtr* at) {
  assert(AliasIdxTop <= i && i < Compile::current()->_max_alias_types, "Invalid alias index");
  _index = i;
  _adr_type = at;
  _field = NULL;
  _element = NULL;
  _is_rewritable = true; // default
  const TypeOopPtr *atoop = (at != NULL) ? at->isa_oopptr() : NULL;
  if (atoop != NULL && atoop->is_known_instance()) {
    const TypeOopPtr *gt = atoop->cast_to_instance_id(TypeOopPtr::InstanceBot);
    _general_index = Compile::current()->get_alias_index(gt);
  } else {
    _general_index = 0;
  }
}

BasicType Compile::AliasType::basic_type() const {
  if (element() != NULL) {
    const Type* element = adr_type()->is_aryptr()->elem();
    return element->isa_narrowoop() ? T_OBJECT : element->array_element_basic_type();
  } if (field() != NULL) {
    return field()->layout_type();
  } else {
    return T_ILLEGAL; // unknown
  }
}

//---------------------------------print_on------------------------------------
#ifndef PRODUCT
void Compile::AliasType::print_on(outputStream* st) {
  if (index() < 10)
        st->print("@ <%d> ", index());
  else  st->print("@ <%d>",  index());
  st->print(is_rewritable() ? "   " : " RO");
  int offset = adr_type()->offset();
  if (offset == Type::OffsetBot)
        st->print(" +any");
  else  st->print(" +%-3d", offset);
  st->print(" in ");
  adr_type()->dump_on(st);
  const TypeOopPtr* tjp = adr_type()->isa_oopptr();
  if (field() != NULL && tjp) {
    if (tjp->klass()  != field()->holder() ||
        tjp->offset() != field()->offset_in_bytes()) {
      st->print(" != ");
      field()->print();
      st->print(" ***");
    }
  }
}

void print_alias_types() {
  Compile* C = Compile::current();
  tty->print_cr("--- Alias types, AliasIdxBot .. %d", C->num_alias_types()-1);
  for (int idx = Compile::AliasIdxBot; idx < C->num_alias_types(); idx++) {
    C->alias_type(idx)->print_on(tty);
    tty->cr();
  }
}
#endif


//----------------------------probe_alias_cache--------------------------------
Compile::AliasCacheEntry* Compile::probe_alias_cache(const TypePtr* adr_type) {
  intptr_t key = (intptr_t) adr_type;
  key ^= key >> logAliasCacheSize;
  return &_alias_cache[key & right_n_bits(logAliasCacheSize)];
}


//-----------------------------grow_alias_types--------------------------------
void Compile::grow_alias_types() {
  const int old_ats  = _max_alias_types; // how many before?
  const int new_ats  = old_ats;          // how many more?
  const int grow_ats = old_ats+new_ats;  // how many now?
  _max_alias_types = grow_ats;
  _alias_types =  REALLOC_ARENA_ARRAY(comp_arena(), AliasType*, _alias_types, old_ats, grow_ats);
  AliasType* ats =    NEW_ARENA_ARRAY(comp_arena(), AliasType, new_ats);
  Copy::zero_to_bytes(ats, sizeof(AliasType)*new_ats);
  for (int i = 0; i < new_ats; i++)  _alias_types[old_ats+i] = &ats[i];
}


//--------------------------------find_alias_type------------------------------
Compile::AliasType* Compile::find_alias_type(const TypePtr* adr_type, bool no_create, ciField* original_field) {
  if (_AliasLevel == 0)
    return alias_type(AliasIdxBot);

  AliasCacheEntry* ace = probe_alias_cache(adr_type);
  if (ace->_adr_type == adr_type) {
    return alias_type(ace->_index);
  }

  // Handle special cases.
  if (adr_type == NULL)             return alias_type(AliasIdxTop);
  if (adr_type == TypePtr::BOTTOM)  return alias_type(AliasIdxBot);

  // Do it the slow way.
  const TypePtr* flat = flatten_alias_type(adr_type);

#ifdef ASSERT
  {
    ResourceMark rm;
    assert(flat == flatten_alias_type(flat), "not idempotent: adr_type = %s; flat = %s => %s",
           Type::str(adr_type), Type::str(flat), Type::str(flatten_alias_type(flat)));
    assert(flat != TypePtr::BOTTOM, "cannot alias-analyze an untyped ptr: adr_type = %s",
           Type::str(adr_type));
    if (flat->isa_oopptr() && !flat->isa_klassptr()) {
      const TypeOopPtr* foop = flat->is_oopptr();
      // Scalarizable allocations have exact klass always.
      bool exact = !foop->klass_is_exact() || foop->is_known_instance();
      const TypePtr* xoop = foop->cast_to_exactness(exact)->is_ptr();
      assert(foop == flatten_alias_type(xoop), "exactness must not affect alias type: foop = %s; xoop = %s",
             Type::str(foop), Type::str(xoop));
    }
  }
#endif

  int idx = AliasIdxTop;
  for (int i = 0; i < num_alias_types(); i++) {
    if (alias_type(i)->adr_type() == flat) {
      idx = i;
      break;
    }
  }

  if (idx == AliasIdxTop) {
    if (no_create)  return NULL;
    // Grow the array if necessary.
    if (_num_alias_types == _max_alias_types)  grow_alias_types();
    // Add a new alias type.
    idx = _num_alias_types++;
    _alias_types[idx]->Init(idx, flat);
    if (flat == TypeInstPtr::KLASS)  alias_type(idx)->set_rewritable(false);
    if (flat == TypeAryPtr::RANGE)   alias_type(idx)->set_rewritable(false);
    if (flat->isa_instptr()) {
      if (flat->offset() == java_lang_Class::klass_offset()
          && flat->is_instptr()->klass() == env()->Class_klass())
        alias_type(idx)->set_rewritable(false);
    }
    if (flat->isa_aryptr()) {
#ifdef ASSERT
      const int header_size_min  = arrayOopDesc::base_offset_in_bytes(T_BYTE);
      // (T_BYTE has the weakest alignment and size restrictions...)
      assert(flat->offset() < header_size_min, "array body reference must be OffsetBot");
#endif
      if (flat->offset() == TypePtr::OffsetBot) {
        alias_type(idx)->set_element(flat->is_aryptr()->elem());
      }
    }
    if (flat->isa_klassptr()) {
      if (flat->offset() == in_bytes(Klass::super_check_offset_offset()))
        alias_type(idx)->set_rewritable(false);
      if (flat->offset() == in_bytes(Klass::modifier_flags_offset()))
        alias_type(idx)->set_rewritable(false);
      if (flat->offset() == in_bytes(Klass::access_flags_offset()))
        alias_type(idx)->set_rewritable(false);
      if (flat->offset() == in_bytes(Klass::java_mirror_offset()))
        alias_type(idx)->set_rewritable(false);
      if (flat->offset() == in_bytes(Klass::secondary_super_cache_offset()))
        alias_type(idx)->set_rewritable(false);
    }
    // %%% (We would like to finalize JavaThread::threadObj_offset(),
    // but the base pointer type is not distinctive enough to identify
    // references into JavaThread.)

    // Check for final fields.
    const TypeInstPtr* tinst = flat->isa_instptr();
    if (tinst && tinst->offset() >= instanceOopDesc::base_offset_in_bytes()) {
      ciField* field;
      if (tinst->const_oop() != NULL &&
          tinst->klass() == ciEnv::current()->Class_klass() &&
          tinst->offset() >= (tinst->klass()->as_instance_klass()->size_helper() * wordSize)) {
        // static field
        ciInstanceKlass* k = tinst->const_oop()->as_instance()->java_lang_Class_klass()->as_instance_klass();
        field = k->get_field_by_offset(tinst->offset(), true);
      } else {
        ciInstanceKlass *k = tinst->klass()->as_instance_klass();
        field = k->get_field_by_offset(tinst->offset(), false);
      }
      assert(field == NULL ||
             original_field == NULL ||
             (field->holder() == original_field->holder() &&
              field->offset() == original_field->offset() &&
              field->is_static() == original_field->is_static()), "wrong field?");
      // Set field() and is_rewritable() attributes.
      if (field != NULL)  alias_type(idx)->set_field(field);
    }
  }

  // Fill the cache for next time.
  ace->_adr_type = adr_type;
  ace->_index    = idx;
  assert(alias_type(adr_type) == alias_type(idx),  "type must be installed");

  // Might as well try to fill the cache for the flattened version, too.
  AliasCacheEntry* face = probe_alias_cache(flat);
  if (face->_adr_type == NULL) {
    face->_adr_type = flat;
    face->_index    = idx;
    assert(alias_type(flat) == alias_type(idx), "flat type must work too");
  }

  return alias_type(idx);
}


Compile::AliasType* Compile::alias_type(ciField* field) {
  const TypeOopPtr* t;
  if (field->is_static())
    t = TypeInstPtr::make(field->holder()->java_mirror());
  else
    t = TypeOopPtr::make_from_klass_raw(field->holder());
  AliasType* atp = alias_type(t->add_offset(field->offset_in_bytes()), field);
  assert((field->is_final() || field->is_stable()) == !atp->is_rewritable(), "must get the rewritable bits correct");
  return atp;
}


//------------------------------have_alias_type--------------------------------
bool Compile::have_alias_type(const TypePtr* adr_type) {
  AliasCacheEntry* ace = probe_alias_cache(adr_type);
  if (ace->_adr_type == adr_type) {
    return true;
  }

  // Handle special cases.
  if (adr_type == NULL)             return true;
  if (adr_type == TypePtr::BOTTOM)  return true;

  return find_alias_type(adr_type, true, NULL) != NULL;
}

//-----------------------------must_alias--------------------------------------
// True if all values of the given address type are in the given alias category.
bool Compile::must_alias(const TypePtr* adr_type, int alias_idx) {
  if (alias_idx == AliasIdxBot)         return true;  // the universal category
  if (adr_type == NULL)                 return true;  // NULL serves as TypePtr::TOP
  if (alias_idx == AliasIdxTop)         return false; // the empty category
  if (adr_type->base() == Type::AnyPtr) return false; // TypePtr::BOTTOM or its twins

  // the only remaining possible overlap is identity
  int adr_idx = get_alias_index(adr_type);
  assert(adr_idx != AliasIdxBot && adr_idx != AliasIdxTop, "");
  assert(adr_idx == alias_idx ||
         (alias_type(alias_idx)->adr_type() != TypeOopPtr::BOTTOM
          && adr_type                       != TypeOopPtr::BOTTOM),
         "should not be testing for overlap with an unsafe pointer");
  return adr_idx == alias_idx;
}

//------------------------------can_alias--------------------------------------
// True if any values of the given address type are in the given alias category.
bool Compile::can_alias(const TypePtr* adr_type, int alias_idx) {
  if (alias_idx == AliasIdxTop)         return false; // the empty category
  if (adr_type == NULL)                 return false; // NULL serves as TypePtr::TOP
  // Known instance doesn't alias with bottom memory
  if (alias_idx == AliasIdxBot)         return !adr_type->is_known_instance();                   // the universal category
  if (adr_type->base() == Type::AnyPtr) return !C->get_adr_type(alias_idx)->is_known_instance(); // TypePtr::BOTTOM or its twins

  // the only remaining possible overlap is identity
  int adr_idx = get_alias_index(adr_type);
  assert(adr_idx != AliasIdxBot && adr_idx != AliasIdxTop, "");
  return adr_idx == alias_idx;
}

//---------------------cleanup_loop_predicates-----------------------
// Remove the opaque nodes that protect the predicates so that all unused
// checks and uncommon_traps will be eliminated from the ideal graph
void Compile::cleanup_loop_predicates(PhaseIterGVN &igvn) {
  if (predicate_count()==0) return;
  for (int i = predicate_count(); i > 0; i--) {
    Node * n = predicate_opaque1_node(i-1);
    assert(n->Opcode() == Op_Opaque1, "must be");
    igvn.replace_node(n, n->in(1));
  }
  assert(predicate_count()==0, "should be clean!");
}

void Compile::record_for_post_loop_opts_igvn(Node* n) {
  if (!n->for_post_loop_opts_igvn()) {
    assert(!_for_post_loop_igvn.contains(n), "duplicate");
    n->add_flag(Node::NodeFlags::Flag_for_post_loop_opts_igvn);
    _for_post_loop_igvn.append(n);
  }
}

void Compile::remove_from_post_loop_opts_igvn(Node* n) {
  n->remove_flag(Node::NodeFlags::Flag_for_post_loop_opts_igvn);
  _for_post_loop_igvn.remove(n);
}

void Compile::process_for_post_loop_opts_igvn(PhaseIterGVN& igvn) {
  // Verify that all previous optimizations produced a valid graph
  // at least to this point, even if no loop optimizations were done.
  PhaseIdealLoop::verify(igvn);

  C->set_post_loop_opts_phase(); // no more loop opts allowed

  assert(!C->major_progress(), "not cleared");

  if (_for_post_loop_igvn.length() > 0) {
    while (_for_post_loop_igvn.length() > 0) {
      Node* n = _for_post_loop_igvn.pop();
      n->remove_flag(Node::NodeFlags::Flag_for_post_loop_opts_igvn);
      igvn._worklist.push(n);
    }
    igvn.optimize();
    assert(_for_post_loop_igvn.length() == 0, "no more delayed nodes allowed");

    // Sometimes IGVN sets major progress (e.g., when processing loop nodes).
    if (C->major_progress()) {
      C->clear_major_progress(); // ensure that major progress is now clear
    }
  }
}

// StringOpts and late inlining of string methods
void Compile::inline_string_calls(bool parse_time) {
  {
    // remove useless nodes to make the usage analysis simpler
    ResourceMark rm;
    PhaseRemoveUseless pru(initial_gvn(), for_igvn());
  }

  {
    ResourceMark rm;
    print_method(PHASE_BEFORE_STRINGOPTS, 3);
    PhaseStringOpts pso(initial_gvn(), for_igvn());
    print_method(PHASE_AFTER_STRINGOPTS, 3);
  }

  // now inline anything that we skipped the first time around
  if (!parse_time) {
    _late_inlines_pos = _late_inlines.length();
  }

  while (_string_late_inlines.length() > 0) {
    CallGenerator* cg = _string_late_inlines.pop();
    cg->do_late_inline();
    if (failing())  return;
  }
  _string_late_inlines.trunc_to(0);
}

// Late inlining of boxing methods
void Compile::inline_boxing_calls(PhaseIterGVN& igvn) {
  if (_boxing_late_inlines.length() > 0) {
    assert(has_boxed_value(), "inconsistent");

    PhaseGVN* gvn = initial_gvn();
    set_inlining_incrementally(true);

    assert( igvn._worklist.size() == 0, "should be done with igvn" );
    for_igvn()->clear();
    gvn->replace_with(&igvn);

    _late_inlines_pos = _late_inlines.length();

    while (_boxing_late_inlines.length() > 0) {
      CallGenerator* cg = _boxing_late_inlines.pop();
      cg->do_late_inline();
      if (failing())  return;
    }
    _boxing_late_inlines.trunc_to(0);

    inline_incrementally_cleanup(igvn);

    set_inlining_incrementally(false);
  }
}

bool Compile::inline_incrementally_one() {
  assert(IncrementalInline, "incremental inlining should be on");

  TracePhase tp("incrementalInline_inline", &timers[_t_incrInline_inline]);

  set_inlining_progress(false);
  set_do_cleanup(false);

  for (int i = 0; i < _late_inlines.length(); i++) {
    _late_inlines_pos = i+1;
    CallGenerator* cg = _late_inlines.at(i);
    bool does_dispatch = cg->is_virtual_late_inline() || cg->is_mh_late_inline();
    if (inlining_incrementally() || does_dispatch) { // a call can be either inlined or strength-reduced to a direct call
      cg->do_late_inline();
      assert(_late_inlines.at(i) == cg, "no insertions before current position allowed");
      if (failing()) {
        return false;
      } else if (inlining_progress()) {
        _late_inlines_pos = i+1; // restore the position in case new elements were inserted
        print_method(PHASE_INCREMENTAL_INLINE_STEP, cg->call_node(), 3);
        break; // process one call site at a time
      }
    } else {
      // Ignore late inline direct calls when inlining is not allowed.
      // They are left in the late inline list when node budget is exhausted until the list is fully drained.
    }
  }
  // Remove processed elements.
  _late_inlines.remove_till(_late_inlines_pos);
  _late_inlines_pos = 0;

  assert(inlining_progress() || _late_inlines.length() == 0, "no progress");

  bool needs_cleanup = do_cleanup() || over_inlining_cutoff();

  set_inlining_progress(false);
  set_do_cleanup(false);

  bool force_cleanup = directive()->IncrementalInlineForceCleanupOption;
  return (_late_inlines.length() > 0) && !needs_cleanup && !force_cleanup;
}

void Compile::inline_incrementally_cleanup(PhaseIterGVN& igvn) {
  {
    TracePhase tp("incrementalInline_pru", &timers[_t_incrInline_pru]);
    ResourceMark rm;
    PhaseRemoveUseless pru(initial_gvn(), for_igvn());
  }
  {
    TracePhase tp("incrementalInline_igvn", &timers[_t_incrInline_igvn]);
    igvn = PhaseIterGVN(initial_gvn());
    igvn.optimize();
  }
  print_method(PHASE_INCREMENTAL_INLINE_CLEANUP, 3);
}

// Perform incremental inlining until bound on number of live nodes is reached
void Compile::inline_incrementally(PhaseIterGVN& igvn) {
  TracePhase tp("incrementalInline", &timers[_t_incrInline]);

  set_inlining_incrementally(true);
  uint low_live_nodes = 0;

  while (_late_inlines.length() > 0) {
    if (live_nodes() > (uint)LiveNodeCountInliningCutoff) {
      if (low_live_nodes < (uint)LiveNodeCountInliningCutoff * 8 / 10) {
        TracePhase tp("incrementalInline_ideal", &timers[_t_incrInline_ideal]);
        // PhaseIdealLoop is expensive so we only try it once we are
        // out of live nodes and we only try it again if the previous
        // helped got the number of nodes down significantly
        PhaseIdealLoop::optimize(igvn, LoopOptsNone);
        if (failing())  return;
        low_live_nodes = live_nodes();
        _major_progress = true;
      }

      if (live_nodes() > (uint)LiveNodeCountInliningCutoff) {
        bool do_print_inlining = print_inlining() || print_intrinsics();
        if (do_print_inlining || log() != NULL) {
          // Print inlining message for candidates that we couldn't inline for lack of space.
          for (int i = 0; i < _late_inlines.length(); i++) {
            CallGenerator* cg = _late_inlines.at(i);
            const char* msg = "live nodes > LiveNodeCountInliningCutoff";
            if (do_print_inlining) {
              cg->print_inlining_late(msg);
            }
            log_late_inline_failure(cg, msg);
          }
        }
        break; // finish
      }
    }

    for_igvn()->clear();
    initial_gvn()->replace_with(&igvn);

    while (inline_incrementally_one()) {
      assert(!failing(), "inconsistent");
    }
    if (failing())  return;

    inline_incrementally_cleanup(igvn);

    print_method(PHASE_INCREMENTAL_INLINE_STEP, 3);

    if (failing())  return;

    if (_late_inlines.length() == 0) {
      break; // no more progress
    }
  }
  assert( igvn._worklist.size() == 0, "should be done with igvn" );

  if (_string_late_inlines.length() > 0) {
    assert(has_stringbuilder(), "inconsistent");
    for_igvn()->clear();
    initial_gvn()->replace_with(&igvn);

    inline_string_calls(false);

    if (failing())  return;

    inline_incrementally_cleanup(igvn);
  }

  set_inlining_incrementally(false);
}

void Compile::process_late_inline_calls_no_inline(PhaseIterGVN& igvn) {
  // "inlining_incrementally() == false" is used to signal that no inlining is allowed
  // (see LateInlineVirtualCallGenerator::do_late_inline_check() for details).
  // Tracking and verification of modified nodes is disabled by setting "_modified_nodes == NULL"
  // as if "inlining_incrementally() == true" were set.
  assert(inlining_incrementally() == false, "not allowed");
  assert(_modified_nodes == NULL, "not allowed");
  assert(_late_inlines.length() > 0, "sanity");

  while (_late_inlines.length() > 0) {
    for_igvn()->clear();
    initial_gvn()->replace_with(&igvn);

    while (inline_incrementally_one()) {
      assert(!failing(), "inconsistent");
    }
    if (failing())  return;

    inline_incrementally_cleanup(igvn);
  }
}

bool Compile::optimize_loops(PhaseIterGVN& igvn, LoopOptsMode mode) {
  if (_loop_opts_cnt > 0) {
    debug_only( int cnt = 0; );
    while (major_progress() && (_loop_opts_cnt > 0)) {
      TracePhase tp("idealLoop", &timers[_t_idealLoop]);
      assert( cnt++ < 40, "infinite cycle in loop optimization" );
      PhaseIdealLoop::optimize(igvn, mode);
      _loop_opts_cnt--;
      if (failing())  return false;
      if (major_progress()) print_method(PHASE_PHASEIDEALLOOP_ITERATIONS, 2);
    }
  }
  return true;
}

// Remove edges from "root" to each SafePoint at a backward branch.
// They were inserted during parsing (see add_safepoint()) to make
// infinite loops without calls or exceptions visible to root, i.e.,
// useful.
void Compile::remove_root_to_sfpts_edges(PhaseIterGVN& igvn) {
  Node *r = root();
  if (r != NULL) {
    for (uint i = r->req(); i < r->len(); ++i) {
      Node *n = r->in(i);
      if (n != NULL && n->is_SafePoint()) {
        r->rm_prec(i);
        if (n->outcnt() == 0) {
          igvn.remove_dead_node(n);
        }
        --i;
      }
    }
    // Parsing may have added top inputs to the root node (Path
    // leading to the Halt node proven dead). Make sure we get a
    // chance to clean them up.
    igvn._worklist.push(r);
    igvn.optimize();
  }
}

//------------------------------Optimize---------------------------------------
// Given a graph, optimize it.
void Compile::Optimize() {
  TracePhase tp("optimizer", &timers[_t_optimizer]);

#ifndef PRODUCT
  if (env()->break_at_compile()) {
    BREAKPOINT;
  }

#endif

  BarrierSetC2* bs = BarrierSet::barrier_set()->barrier_set_c2();
#ifdef ASSERT
  bs->verify_gc_barriers(this, BarrierSetC2::BeforeOptimize);
#endif

  ResourceMark rm;

  print_inlining_reinit();

  NOT_PRODUCT( verify_graph_edges(); )

  print_method(PHASE_AFTER_PARSING);

 {
  // Iterative Global Value Numbering, including ideal transforms
  // Initialize IterGVN with types and values from parse-time GVN
  PhaseIterGVN igvn(initial_gvn());
#ifdef ASSERT
  _modified_nodes = new (comp_arena()) Unique_Node_List(comp_arena());
#endif
  {
    TracePhase tp("iterGVN", &timers[_t_iterGVN]);
    igvn.optimize();
  }

  if (failing())  return;

  print_method(PHASE_ITER_GVN1, 2);

  inline_incrementally(igvn);

  print_method(PHASE_INCREMENTAL_INLINE, 2);

  if (failing())  return;

  if (eliminate_boxing()) {
    // Inline valueOf() methods now.
    inline_boxing_calls(igvn);

    if (AlwaysIncrementalInline) {
      inline_incrementally(igvn);
    }

    print_method(PHASE_INCREMENTAL_BOXING_INLINE, 2);

    if (failing())  return;
  }

  // Remove the speculative part of types and clean up the graph from
  // the extra CastPP nodes whose only purpose is to carry them. Do
  // that early so that optimizations are not disrupted by the extra
  // CastPP nodes.
  remove_speculative_types(igvn);

  // No more new expensive nodes will be added to the list from here
  // so keep only the actual candidates for optimizations.
  cleanup_expensive_nodes(igvn);

  assert(EnableVectorSupport || !has_vbox_nodes(), "sanity");
  if (EnableVectorSupport && has_vbox_nodes()) {
    TracePhase tp("", &timers[_t_vector]);
    PhaseVector pv(igvn);
    pv.optimize_vector_boxes();

    print_method(PHASE_ITER_GVN_AFTER_VECTOR, 2);
  }
  assert(!has_vbox_nodes(), "sanity");

  if (!failing() && RenumberLiveNodes && live_nodes() + NodeLimitFudgeFactor < unique()) {
    Compile::TracePhase tp("", &timers[_t_renumberLive]);
    initial_gvn()->replace_with(&igvn);
    for_igvn()->clear();
    Unique_Node_List new_worklist(C->comp_arena());
    {
      ResourceMark rm;
      PhaseRenumberLive prl = PhaseRenumberLive(initial_gvn(), for_igvn(), &new_worklist);
    }
    Unique_Node_List* save_for_igvn = for_igvn();
    set_for_igvn(&new_worklist);
    igvn = PhaseIterGVN(initial_gvn());
    igvn.optimize();
    set_for_igvn(save_for_igvn);
  }

  // Now that all inlining is over and no PhaseRemoveUseless will run, cut edge from root to loop
  // safepoints
  remove_root_to_sfpts_edges(igvn);

  // Perform escape analysis
  if (_do_escape_analysis && ConnectionGraph::has_candidates(this)) {
    if (has_loops()) {
      // Cleanup graph (remove dead nodes).
      TracePhase tp("idealLoop", &timers[_t_idealLoop]);
      PhaseIdealLoop::optimize(igvn, LoopOptsMaxUnroll);
      if (major_progress()) print_method(PHASE_PHASEIDEAL_BEFORE_EA, 2);
      if (failing())  return;
    }
    ConnectionGraph::do_analysis(this, &igvn);

    if (failing())  return;

    // Optimize out fields loads from scalar replaceable allocations.
    igvn.optimize();
    print_method(PHASE_ITER_GVN_AFTER_EA, 2);

    if (failing())  return;

    if (congraph() != NULL && macro_count() > 0) {
      TracePhase tp("macroEliminate", &timers[_t_macroEliminate]);
      PhaseMacroExpand mexp(igvn);
      mexp.eliminate_macro_nodes();
      igvn.set_delay_transform(false);

      igvn.optimize();
      print_method(PHASE_ITER_GVN_AFTER_ELIMINATION, 2);

      if (failing())  return;
    }
  }

  // Loop transforms on the ideal graph.  Range Check Elimination,
  // peeling, unrolling, etc.

  // Set loop opts counter
  if((_loop_opts_cnt > 0) && (has_loops() || has_split_ifs())) {
    {
      TracePhase tp("idealLoop", &timers[_t_idealLoop]);
      PhaseIdealLoop::optimize(igvn, LoopOptsDefault);
      _loop_opts_cnt--;
      if (major_progress()) print_method(PHASE_PHASEIDEALLOOP1, 2);
      if (failing())  return;
    }
    // Loop opts pass if partial peeling occurred in previous pass
    if(PartialPeelLoop && major_progress() && (_loop_opts_cnt > 0)) {
      TracePhase tp("idealLoop", &timers[_t_idealLoop]);
      PhaseIdealLoop::optimize(igvn, LoopOptsSkipSplitIf);
      _loop_opts_cnt--;
      if (major_progress()) print_method(PHASE_PHASEIDEALLOOP2, 2);
      if (failing())  return;
    }
    // Loop opts pass for loop-unrolling before CCP
    if(major_progress() && (_loop_opts_cnt > 0)) {
      TracePhase tp("idealLoop", &timers[_t_idealLoop]);
      PhaseIdealLoop::optimize(igvn, LoopOptsSkipSplitIf);
      _loop_opts_cnt--;
      if (major_progress()) print_method(PHASE_PHASEIDEALLOOP3, 2);
    }
    if (!failing()) {
      // Verify that last round of loop opts produced a valid graph
      PhaseIdealLoop::verify(igvn);
    }
  }
  if (failing())  return;

  // Conditional Constant Propagation;
  PhaseCCP ccp( &igvn );
  assert( true, "Break here to ccp.dump_nodes_and_types(_root,999,1)");
  {
    TracePhase tp("ccp", &timers[_t_ccp]);
    ccp.do_transform();
  }
  print_method(PHASE_CCP1, 2);

  assert( true, "Break here to ccp.dump_old2new_map()");

  // Iterative Global Value Numbering, including ideal transforms
  {
    TracePhase tp("iterGVN2", &timers[_t_iterGVN2]);
    igvn = ccp;
    igvn.optimize();
  }
  print_method(PHASE_ITER_GVN2, 2);

  if (failing())  return;

  // Loop transforms on the ideal graph.  Range Check Elimination,
  // peeling, unrolling, etc.
  if (!optimize_loops(igvn, LoopOptsDefault)) {
    return;
  }

  if (failing())  return;

  C->clear_major_progress(); // ensure that major progress is now clear

  process_for_post_loop_opts_igvn(igvn);

#ifdef ASSERT
  bs->verify_gc_barriers(this, BarrierSetC2::BeforeMacroExpand);
#endif

  {
    TracePhase tp("macroExpand", &timers[_t_macroExpand]);
    PhaseMacroExpand  mex(igvn);
    if (mex.expand_macro_nodes()) {
      assert(failing(), "must bail out w/ explicit message");
      return;
    }
    print_method(PHASE_MACRO_EXPANSION, 2);
  }

  {
    TracePhase tp("barrierExpand", &timers[_t_barrierExpand]);
    if (bs->expand_barriers(this, igvn)) {
      assert(failing(), "must bail out w/ explicit message");
      return;
    }
    print_method(PHASE_BARRIER_EXPANSION, 2);
  }

  if (C->max_vector_size() > 0) {
    C->optimize_logic_cones(igvn);
    igvn.optimize();
  }

  DEBUG_ONLY( _modified_nodes = NULL; )

  assert(igvn._worklist.size() == 0, "not empty");

  assert(_late_inlines.length() == 0 || IncrementalInlineMH || IncrementalInlineVirtual, "not empty");

  if (_late_inlines.length() > 0) {
    // More opportunities to optimize virtual and MH calls.
    // Though it's maybe too late to perform inlining, strength-reducing them to direct calls is still an option.
    process_late_inline_calls_no_inline(igvn);
  }
 } // (End scope of igvn; run destructor if necessary for asserts.)

 check_no_dead_use();

 process_print_inlining();

 // A method with only infinite loops has no edges entering loops from root
 {
   TracePhase tp("graphReshape", &timers[_t_graphReshaping]);
   if (final_graph_reshaping()) {
     assert(failing(), "must bail out w/ explicit message");
     return;
   }
 }

 print_method(PHASE_OPTIMIZE_FINISHED, 2);
 DEBUG_ONLY(set_phase_optimize_finished();)
}

#ifdef ASSERT
void Compile::check_no_dead_use() const {
  ResourceMark rm;
  Unique_Node_List wq;
  wq.push(root());
  for (uint i = 0; i < wq.size(); ++i) {
    Node* n = wq.at(i);
    for (DUIterator_Fast jmax, j = n->fast_outs(jmax); j < jmax; j++) {
      Node* u = n->fast_out(j);
      if (u->outcnt() == 0 && !u->is_Con()) {
        u->dump();
        fatal("no reachable node should have no use");
      }
      wq.push(u);
    }
  }
}
#endif

void Compile::inline_vector_reboxing_calls() {
  if (C->_vector_reboxing_late_inlines.length() > 0) {
    _late_inlines_pos = C->_late_inlines.length();
    while (_vector_reboxing_late_inlines.length() > 0) {
      CallGenerator* cg = _vector_reboxing_late_inlines.pop();
      cg->do_late_inline();
      if (failing())  return;
      print_method(PHASE_INLINE_VECTOR_REBOX, cg->call_node());
    }
    _vector_reboxing_late_inlines.trunc_to(0);
  }
}

bool Compile::has_vbox_nodes() {
  if (C->_vector_reboxing_late_inlines.length() > 0) {
    return true;
  }
  for (int macro_idx = C->macro_count() - 1; macro_idx >= 0; macro_idx--) {
    Node * n = C->macro_node(macro_idx);
    assert(n->is_macro(), "only macro nodes expected here");
    if (n->Opcode() == Op_VectorUnbox || n->Opcode() == Op_VectorBox || n->Opcode() == Op_VectorBoxAllocate) {
      return true;
    }
  }
  return false;
}

//---------------------------- Bitwise operation packing optimization ---------------------------

static bool is_vector_unary_bitwise_op(Node* n) {
  return n->Opcode() == Op_XorV &&
         VectorNode::is_vector_bitwise_not_pattern(n);
}

static bool is_vector_binary_bitwise_op(Node* n) {
  switch (n->Opcode()) {
    case Op_AndV:
    case Op_OrV:
      return true;

    case Op_XorV:
      return !is_vector_unary_bitwise_op(n);

    default:
      return false;
  }
}

static bool is_vector_ternary_bitwise_op(Node* n) {
  return n->Opcode() == Op_MacroLogicV;
}

static bool is_vector_bitwise_op(Node* n) {
  return is_vector_unary_bitwise_op(n)  ||
         is_vector_binary_bitwise_op(n) ||
         is_vector_ternary_bitwise_op(n);
}

static bool is_vector_bitwise_cone_root(Node* n) {
  if (n->bottom_type()->isa_vectmask() || !is_vector_bitwise_op(n)) {
    return false;
  }
  for (DUIterator_Fast imax, i = n->fast_outs(imax); i < imax; i++) {
    if (is_vector_bitwise_op(n->fast_out(i))) {
      return false;
    }
  }
  return true;
}

static uint collect_unique_inputs(Node* n, Unique_Node_List& partition, Unique_Node_List& inputs) {
  uint cnt = 0;
  if (is_vector_bitwise_op(n)) {
    if (VectorNode::is_vector_bitwise_not_pattern(n)) {
      for (uint i = 1; i < n->req(); i++) {
        Node* in = n->in(i);
        bool skip = VectorNode::is_all_ones_vector(in);
        if (!skip && !inputs.member(in)) {
          inputs.push(in);
          cnt++;
        }
      }
      assert(cnt <= 1, "not unary");
    } else {
      uint last_req = n->req();
      if (is_vector_ternary_bitwise_op(n)) {
        last_req = n->req() - 1; // skip last input
      }
      for (uint i = 1; i < last_req; i++) {
        Node* def = n->in(i);
        if (!inputs.member(def)) {
          inputs.push(def);
          cnt++;
        }
      }
    }
    partition.push(n);
  } else { // not a bitwise operations
    if (!inputs.member(n)) {
      inputs.push(n);
      cnt++;
    }
  }
  return cnt;
}

void Compile::collect_logic_cone_roots(Unique_Node_List& list) {
  Unique_Node_List useful_nodes;
  C->identify_useful_nodes(useful_nodes);

  for (uint i = 0; i < useful_nodes.size(); i++) {
    Node* n = useful_nodes.at(i);
    if (is_vector_bitwise_cone_root(n)) {
      list.push(n);
    }
  }
}

Node* Compile::xform_to_MacroLogicV(PhaseIterGVN& igvn,
                                    const TypeVect* vt,
                                    Unique_Node_List& partition,
                                    Unique_Node_List& inputs) {
  assert(partition.size() == 2 || partition.size() == 3, "not supported");
  assert(inputs.size()    == 2 || inputs.size()    == 3, "not supported");
  assert(Matcher::match_rule_supported_vector(Op_MacroLogicV, vt->length(), vt->element_basic_type()), "not supported");

  Node* in1 = inputs.at(0);
  Node* in2 = inputs.at(1);
  Node* in3 = (inputs.size() == 3 ? inputs.at(2) : in2);

  uint func = compute_truth_table(partition, inputs);
  return igvn.transform(MacroLogicVNode::make(igvn, in3, in2, in1, func, vt));
}

static uint extract_bit(uint func, uint pos) {
  return (func & (1 << pos)) >> pos;
}

//
//  A macro logic node represents a truth table. It has 4 inputs,
//  First three inputs corresponds to 3 columns of a truth table
//  and fourth input captures the logic function.
//
//  eg.  fn = (in1 AND in2) OR in3;
//
//      MacroNode(in1,in2,in3,fn)
//
//  -----------------
//  in1 in2 in3  fn
//  -----------------
//  0    0   0    0
//  0    0   1    1
//  0    1   0    0
//  0    1   1    1
//  1    0   0    0
//  1    0   1    1
//  1    1   0    1
//  1    1   1    1
//

uint Compile::eval_macro_logic_op(uint func, uint in1 , uint in2, uint in3) {
  int res = 0;
  for (int i = 0; i < 8; i++) {
    int bit1 = extract_bit(in1, i);
    int bit2 = extract_bit(in2, i);
    int bit3 = extract_bit(in3, i);

    int func_bit_pos = (bit1 << 2 | bit2 << 1 | bit3);
    int func_bit = extract_bit(func, func_bit_pos);

    res |= func_bit << i;
  }
  return res;
}

static uint eval_operand(Node* n, ResourceHashtable<Node*,uint>& eval_map) {
  assert(n != NULL, "");
  assert(eval_map.contains(n), "absent");
  return *(eval_map.get(n));
}

static void eval_operands(Node* n,
                          uint& func1, uint& func2, uint& func3,
                          ResourceHashtable<Node*,uint>& eval_map) {
  assert(is_vector_bitwise_op(n), "");

  if (is_vector_unary_bitwise_op(n)) {
    Node* opnd = n->in(1);
    if (VectorNode::is_vector_bitwise_not_pattern(n) && VectorNode::is_all_ones_vector(opnd)) {
      opnd = n->in(2);
    }
    func1 = eval_operand(opnd, eval_map);
  } else if (is_vector_binary_bitwise_op(n)) {
    func1 = eval_operand(n->in(1), eval_map);
    func2 = eval_operand(n->in(2), eval_map);
  } else {
    assert(is_vector_ternary_bitwise_op(n), "unknown operation");
    func1 = eval_operand(n->in(1), eval_map);
    func2 = eval_operand(n->in(2), eval_map);
    func3 = eval_operand(n->in(3), eval_map);
  }
}

uint Compile::compute_truth_table(Unique_Node_List& partition, Unique_Node_List& inputs) {
  assert(inputs.size() <= 3, "sanity");
  ResourceMark rm;
  uint res = 0;
  ResourceHashtable<Node*,uint> eval_map;

  // Populate precomputed functions for inputs.
  // Each input corresponds to one column of 3 input truth-table.
  uint input_funcs[] = { 0xAA,   // (_, _, a) -> a
                         0xCC,   // (_, b, _) -> b
                         0xF0 }; // (c, _, _) -> c
  for (uint i = 0; i < inputs.size(); i++) {
    eval_map.put(inputs.at(i), input_funcs[i]);
  }

  for (uint i = 0; i < partition.size(); i++) {
    Node* n = partition.at(i);

    uint func1 = 0, func2 = 0, func3 = 0;
    eval_operands(n, func1, func2, func3, eval_map);

    switch (n->Opcode()) {
      case Op_OrV:
        assert(func3 == 0, "not binary");
        res = func1 | func2;
        break;
      case Op_AndV:
        assert(func3 == 0, "not binary");
        res = func1 & func2;
        break;
      case Op_XorV:
        if (VectorNode::is_vector_bitwise_not_pattern(n)) {
          assert(func2 == 0 && func3 == 0, "not unary");
          res = (~func1) & 0xFF;
        } else {
          assert(func3 == 0, "not binary");
          res = func1 ^ func2;
        }
        break;
      case Op_MacroLogicV:
        // Ordering of inputs may change during evaluation of sub-tree
        // containing MacroLogic node as a child node, thus a re-evaluation
        // makes sure that function is evaluated in context of current
        // inputs.
        res = eval_macro_logic_op(n->in(4)->get_int(), func1, func2, func3);
        break;

      default: assert(false, "not supported: %s", n->Name());
    }
    assert(res <= 0xFF, "invalid");
    eval_map.put(n, res);
  }
  return res;
}

bool Compile::compute_logic_cone(Node* n, Unique_Node_List& partition, Unique_Node_List& inputs) {
  assert(partition.size() == 0, "not empty");
  assert(inputs.size() == 0, "not empty");
  if (is_vector_ternary_bitwise_op(n)) {
    return false;
  }

  bool is_unary_op = is_vector_unary_bitwise_op(n);
  if (is_unary_op) {
    assert(collect_unique_inputs(n, partition, inputs) == 1, "not unary");
    return false; // too few inputs
  }

  assert(is_vector_binary_bitwise_op(n), "not binary");
  Node* in1 = n->in(1);
  Node* in2 = n->in(2);

  int in1_unique_inputs_cnt = collect_unique_inputs(in1, partition, inputs);
  int in2_unique_inputs_cnt = collect_unique_inputs(in2, partition, inputs);
  partition.push(n);

  // Too many inputs?
  if (inputs.size() > 3) {
    partition.clear();
    inputs.clear();
    { // Recompute in2 inputs
      Unique_Node_List not_used;
      in2_unique_inputs_cnt = collect_unique_inputs(in2, not_used, not_used);
    }
    // Pick the node with minimum number of inputs.
    if (in1_unique_inputs_cnt >= 3 && in2_unique_inputs_cnt >= 3) {
      return false; // still too many inputs
    }
    // Recompute partition & inputs.
    Node* child       = (in1_unique_inputs_cnt < in2_unique_inputs_cnt ? in1 : in2);
    collect_unique_inputs(child, partition, inputs);

    Node* other_input = (in1_unique_inputs_cnt < in2_unique_inputs_cnt ? in2 : in1);
    inputs.push(other_input);

    partition.push(n);
  }

  return (partition.size() == 2 || partition.size() == 3) &&
         (inputs.size()    == 2 || inputs.size()    == 3);
}


void Compile::process_logic_cone_root(PhaseIterGVN &igvn, Node *n, VectorSet &visited) {
  assert(is_vector_bitwise_op(n), "not a root");

  visited.set(n->_idx);

  // 1) Do a DFS walk over the logic cone.
  for (uint i = 1; i < n->req(); i++) {
    Node* in = n->in(i);
    if (!visited.test(in->_idx) && is_vector_bitwise_op(in)) {
      process_logic_cone_root(igvn, in, visited);
    }
  }

  // 2) Bottom up traversal: Merge node[s] with
  // the parent to form macro logic node.
  Unique_Node_List partition;
  Unique_Node_List inputs;
  if (compute_logic_cone(n, partition, inputs)) {
    const TypeVect* vt = n->bottom_type()->is_vect();
    Node* macro_logic = xform_to_MacroLogicV(igvn, vt, partition, inputs);
    igvn.replace_node(n, macro_logic);
  }
}

void Compile::optimize_logic_cones(PhaseIterGVN &igvn) {
  ResourceMark rm;
  if (Matcher::match_rule_supported(Op_MacroLogicV)) {
    Unique_Node_List list;
    collect_logic_cone_roots(list);

    while (list.size() > 0) {
      Node* n = list.pop();
      const TypeVect* vt = n->bottom_type()->is_vect();
      bool supported = Matcher::match_rule_supported_vector(Op_MacroLogicV, vt->length(), vt->element_basic_type());
      if (supported) {
        VectorSet visited(comp_arena());
        process_logic_cone_root(igvn, n, visited);
      }
    }
  }
}

//------------------------------Code_Gen---------------------------------------
// Given a graph, generate code for it
void Compile::Code_Gen() {
  if (failing()) {
    return;
  }

  // Perform instruction selection.  You might think we could reclaim Matcher
  // memory PDQ, but actually the Matcher is used in generating spill code.
  // Internals of the Matcher (including some VectorSets) must remain live
  // for awhile - thus I cannot reclaim Matcher memory lest a VectorSet usage
  // set a bit in reclaimed memory.

  // In debug mode can dump m._nodes.dump() for mapping of ideal to machine
  // nodes.  Mapping is only valid at the root of each matched subtree.
  NOT_PRODUCT( verify_graph_edges(); )

  Matcher matcher;
  _matcher = &matcher;
  {
    TracePhase tp("matcher", &timers[_t_matcher]);
    matcher.match();
    if (failing()) {
      return;
    }
  }
  // In debug mode can dump m._nodes.dump() for mapping of ideal to machine
  // nodes.  Mapping is only valid at the root of each matched subtree.
  NOT_PRODUCT( verify_graph_edges(); )

  // If you have too many nodes, or if matching has failed, bail out
  check_node_count(0, "out of nodes matching instructions");
  if (failing()) {
    return;
  }

  print_method(PHASE_MATCHING, 2);

  // Build a proper-looking CFG
  PhaseCFG cfg(node_arena(), root(), matcher);
  _cfg = &cfg;
  {
    TracePhase tp("scheduler", &timers[_t_scheduler]);
    bool success = cfg.do_global_code_motion();
    if (!success) {
      return;
    }

    print_method(PHASE_GLOBAL_CODE_MOTION, 2);
    NOT_PRODUCT( verify_graph_edges(); )
    cfg.verify();
  }

  PhaseChaitin regalloc(unique(), cfg, matcher, false);
  _regalloc = &regalloc;
  {
    TracePhase tp("regalloc", &timers[_t_registerAllocation]);
    // Perform register allocation.  After Chaitin, use-def chains are
    // no longer accurate (at spill code) and so must be ignored.
    // Node->LRG->reg mappings are still accurate.
    _regalloc->Register_Allocate();

    // Bail out if the allocator builds too many nodes
    if (failing()) {
      return;
    }
  }

  // Prior to register allocation we kept empty basic blocks in case the
  // the allocator needed a place to spill.  After register allocation we
  // are not adding any new instructions.  If any basic block is empty, we
  // can now safely remove it.
  {
    TracePhase tp("blockOrdering", &timers[_t_blockOrdering]);
    cfg.remove_empty_blocks();
    if (do_freq_based_layout()) {
      PhaseBlockLayout layout(cfg);
    } else {
      cfg.set_loop_alignment();
    }
    cfg.fixup_flow();
  }

  // Apply peephole optimizations
  if( OptoPeephole ) {
    TracePhase tp("peephole", &timers[_t_peephole]);
    PhasePeephole peep( _regalloc, cfg);
    peep.do_transform();
  }

  // Do late expand if CPU requires this.
  if (Matcher::require_postalloc_expand) {
    TracePhase tp("postalloc_expand", &timers[_t_postalloc_expand]);
    cfg.postalloc_expand(_regalloc);
  }

  // Convert Nodes to instruction bits in a buffer
  {
    TracePhase tp("output", &timers[_t_output]);
    PhaseOutput output;
    output.Output();
    if (failing())  return;
    output.install();
  }

  print_method(PHASE_FINAL_CODE);

  // He's dead, Jim.
  _cfg     = (PhaseCFG*)((intptr_t)0xdeadbeef);
  _regalloc = (PhaseChaitin*)((intptr_t)0xdeadbeef);
}

//------------------------------Final_Reshape_Counts---------------------------
// This class defines counters to help identify when a method
// may/must be executed using hardware with only 24-bit precision.
struct Final_Reshape_Counts : public StackObj {
  int  _call_count;             // count non-inlined 'common' calls
  int  _float_count;            // count float ops requiring 24-bit precision
  int  _double_count;           // count double ops requiring more precision
  int  _java_call_count;        // count non-inlined 'java' calls
  int  _inner_loop_count;       // count loops which need alignment
  VectorSet _visited;           // Visitation flags
  Node_List _tests;             // Set of IfNodes & PCTableNodes

  Final_Reshape_Counts() :
    _call_count(0), _float_count(0), _double_count(0),
    _java_call_count(0), _inner_loop_count(0) { }

  void inc_call_count  () { _call_count  ++; }
  void inc_float_count () { _float_count ++; }
  void inc_double_count() { _double_count++; }
  void inc_java_call_count() { _java_call_count++; }
  void inc_inner_loop_count() { _inner_loop_count++; }

  int  get_call_count  () const { return _call_count  ; }
  int  get_float_count () const { return _float_count ; }
  int  get_double_count() const { return _double_count; }
  int  get_java_call_count() const { return _java_call_count; }
  int  get_inner_loop_count() const { return _inner_loop_count; }
};

// Eliminate trivially redundant StoreCMs and accumulate their
// precedence edges.
void Compile::eliminate_redundant_card_marks(Node* n) {
  assert(n->Opcode() == Op_StoreCM, "expected StoreCM");
  if (n->in(MemNode::Address)->outcnt() > 1) {
    // There are multiple users of the same address so it might be
    // possible to eliminate some of the StoreCMs
    Node* mem = n->in(MemNode::Memory);
    Node* adr = n->in(MemNode::Address);
    Node* val = n->in(MemNode::ValueIn);
    Node* prev = n;
    bool done = false;
    // Walk the chain of StoreCMs eliminating ones that match.  As
    // long as it's a chain of single users then the optimization is
    // safe.  Eliminating partially redundant StoreCMs would require
    // cloning copies down the other paths.
    while (mem->Opcode() == Op_StoreCM && mem->outcnt() == 1 && !done) {
      if (adr == mem->in(MemNode::Address) &&
          val == mem->in(MemNode::ValueIn)) {
        // redundant StoreCM
        if (mem->req() > MemNode::OopStore) {
          // Hasn't been processed by this code yet.
          n->add_prec(mem->in(MemNode::OopStore));
        } else {
          // Already converted to precedence edge
          for (uint i = mem->req(); i < mem->len(); i++) {
            // Accumulate any precedence edges
            if (mem->in(i) != NULL) {
              n->add_prec(mem->in(i));
            }
          }
          // Everything above this point has been processed.
          done = true;
        }
        // Eliminate the previous StoreCM
        prev->set_req(MemNode::Memory, mem->in(MemNode::Memory));
        assert(mem->outcnt() == 0, "should be dead");
        mem->disconnect_inputs(this);
      } else {
        prev = mem;
      }
      mem = prev->in(MemNode::Memory);
    }
  }
}

//------------------------------final_graph_reshaping_impl----------------------
// Implement items 1-5 from final_graph_reshaping below.
void Compile::final_graph_reshaping_impl( Node *n, Final_Reshape_Counts &frc) {

  if ( n->outcnt() == 0 ) return; // dead node
  uint nop = n->Opcode();

  // Check for 2-input instruction with "last use" on right input.
  // Swap to left input.  Implements item (2).
  if( n->req() == 3 &&          // two-input instruction
      n->in(1)->outcnt() > 1 && // left use is NOT a last use
      (!n->in(1)->is_Phi() || n->in(1)->in(2) != n) && // it is not data loop
      n->in(2)->outcnt() == 1 &&// right use IS a last use
      !n->in(2)->is_Con() ) {   // right use is not a constant
    // Check for commutative opcode
    switch( nop ) {
    case Op_AddI:  case Op_AddF:  case Op_AddD:  case Op_AddL:
    case Op_MaxI:  case Op_MaxL:  case Op_MaxF:  case Op_MaxD:
    case Op_MinI:  case Op_MinL:  case Op_MinF:  case Op_MinD:
    case Op_MulI:  case Op_MulF:  case Op_MulD:  case Op_MulL:
    case Op_AndL:  case Op_XorL:  case Op_OrL:
    case Op_AndI:  case Op_XorI:  case Op_OrI: {
      // Move "last use" input to left by swapping inputs
      n->swap_edges(1, 2);
      break;
    }
    default:
      break;
    }
  }

#ifdef ASSERT
  if( n->is_Mem() ) {
    int alias_idx = get_alias_index(n->as_Mem()->adr_type());
    assert( n->in(0) != NULL || alias_idx != Compile::AliasIdxRaw ||
            // oop will be recorded in oop map if load crosses safepoint
            n->is_Load() && (n->as_Load()->bottom_type()->isa_oopptr() ||
                             LoadNode::is_immutable_value(n->in(MemNode::Address))),
            "raw memory operations should have control edge");
  }
  if (n->is_MemBar()) {
    MemBarNode* mb = n->as_MemBar();
    if (mb->trailing_store() || mb->trailing_load_store()) {
      assert(mb->leading_membar()->trailing_membar() == mb, "bad membar pair");
      Node* mem = BarrierSet::barrier_set()->barrier_set_c2()->step_over_gc_barrier(mb->in(MemBarNode::Precedent));
      assert((mb->trailing_store() && mem->is_Store() && mem->as_Store()->is_release()) ||
             (mb->trailing_load_store() && mem->is_LoadStore()), "missing mem op");
    } else if (mb->leading()) {
      assert(mb->trailing_membar()->leading_membar() == mb, "bad membar pair");
    }
  }
#endif
  // Count FPU ops and common calls, implements item (3)
  bool gc_handled = BarrierSet::barrier_set()->barrier_set_c2()->final_graph_reshaping(this, n, nop);
  if (!gc_handled) {
    final_graph_reshaping_main_switch(n, frc, nop);
  }

  // Collect CFG split points
  if (n->is_MultiBranch() && !n->is_RangeCheck()) {
    frc._tests.push(n);
  }
}

void Compile::final_graph_reshaping_main_switch(Node* n, Final_Reshape_Counts& frc, uint nop) {
  switch( nop ) {
  // Count all float operations that may use FPU
  case Op_AddF:
  case Op_SubF:
  case Op_MulF:
  case Op_DivF:
  case Op_NegF:
  case Op_ModF:
  case Op_ConvI2F:
  case Op_ConF:
  case Op_CmpF:
  case Op_CmpF3:
  case Op_StoreF:
  case Op_LoadF:
  // case Op_ConvL2F: // longs are split into 32-bit halves
    frc.inc_float_count();
    break;

  case Op_ConvF2D:
  case Op_ConvD2F:
    frc.inc_float_count();
    frc.inc_double_count();
    break;

  // Count all double operations that may use FPU
  case Op_AddD:
  case Op_SubD:
  case Op_MulD:
  case Op_DivD:
  case Op_NegD:
  case Op_ModD:
  case Op_ConvI2D:
  case Op_ConvD2I:
  // case Op_ConvL2D: // handled by leaf call
  // case Op_ConvD2L: // handled by leaf call
  case Op_ConD:
  case Op_CmpD:
  case Op_CmpD3:
  case Op_StoreD:
  case Op_LoadD:
  case Op_LoadD_unaligned:
    frc.inc_double_count();
    break;
  case Op_Opaque1:              // Remove Opaque Nodes before matching
  case Op_Opaque2:              // Remove Opaque Nodes before matching
  case Op_Opaque3:
    n->subsume_by(n->in(1), this);
    break;
  case Op_CallStaticJava:
  case Op_CallJava:
  case Op_CallDynamicJava:
    frc.inc_java_call_count(); // Count java call site;
  case Op_CallRuntime:
  case Op_CallLeaf:
  case Op_CallLeafVector:
  case Op_CallNative:
  case Op_CallLeafNoFP: {
    assert (n->is_Call(), "");
    CallNode *call = n->as_Call();
    // Count call sites where the FP mode bit would have to be flipped.
    // Do not count uncommon runtime calls:
    // uncommon_trap, _complete_monitor_locking, _complete_monitor_unlocking,
    // _new_Java, _new_typeArray, _new_objArray, _rethrow_Java, ...
    if (!call->is_CallStaticJava() || !call->as_CallStaticJava()->_name) {
      frc.inc_call_count();   // Count the call site
    } else {                  // See if uncommon argument is shared
      Node *n = call->in(TypeFunc::Parms);
      int nop = n->Opcode();
      // Clone shared simple arguments to uncommon calls, item (1).
      if (n->outcnt() > 1 &&
          !n->is_Proj() &&
          nop != Op_CreateEx &&
          nop != Op_CheckCastPP &&
          nop != Op_DecodeN &&
          nop != Op_DecodeNKlass &&
          !n->is_Mem() &&
          !n->is_Phi()) {
        Node *x = n->clone();
        call->set_req(TypeFunc::Parms, x);
      }
    }
    break;
  }

  case Op_StoreCM:
    {
      // Convert OopStore dependence into precedence edge
      Node* prec = n->in(MemNode::OopStore);
      n->del_req(MemNode::OopStore);
      n->add_prec(prec);
      eliminate_redundant_card_marks(n);
    }

    // fall through

  case Op_StoreB:
  case Op_StoreC:
  case Op_StorePConditional:
  case Op_StoreI:
  case Op_StoreL:
  case Op_StoreIConditional:
  case Op_StoreLConditional:
  case Op_CompareAndSwapB:
  case Op_CompareAndSwapS:
  case Op_CompareAndSwapI:
  case Op_CompareAndSwapL:
  case Op_CompareAndSwapP:
  case Op_CompareAndSwapN:
  case Op_WeakCompareAndSwapB:
  case Op_WeakCompareAndSwapS:
  case Op_WeakCompareAndSwapI:
  case Op_WeakCompareAndSwapL:
  case Op_WeakCompareAndSwapP:
  case Op_WeakCompareAndSwapN:
  case Op_CompareAndExchangeB:
  case Op_CompareAndExchangeS:
  case Op_CompareAndExchangeI:
  case Op_CompareAndExchangeL:
  case Op_CompareAndExchangeP:
  case Op_CompareAndExchangeN:
  case Op_GetAndAddS:
  case Op_GetAndAddB:
  case Op_GetAndAddI:
  case Op_GetAndAddL:
  case Op_GetAndSetS:
  case Op_GetAndSetB:
  case Op_GetAndSetI:
  case Op_GetAndSetL:
  case Op_GetAndSetP:
  case Op_GetAndSetN:
  case Op_StoreP:
  case Op_StoreN:
  case Op_StoreNKlass:
  case Op_LoadB:
  case Op_LoadUB:
  case Op_LoadUS:
  case Op_LoadI:
  case Op_LoadKlass:
  case Op_LoadNKlass:
  case Op_LoadL:
  case Op_LoadL_unaligned:
  case Op_LoadPLocked:
  case Op_LoadP:
  case Op_LoadN:
  case Op_LoadRange:
  case Op_LoadS:
    break;

  case Op_AddP: {               // Assert sane base pointers
    Node *addp = n->in(AddPNode::Address);
    assert( !addp->is_AddP() ||
            addp->in(AddPNode::Base)->is_top() || // Top OK for allocation
            addp->in(AddPNode::Base) == n->in(AddPNode::Base),
            "Base pointers must match (addp %u)", addp->_idx );
#ifdef _LP64
    if ((UseCompressedOops || UseCompressedClassPointers) &&
        addp->Opcode() == Op_ConP &&
        addp == n->in(AddPNode::Base) &&
        n->in(AddPNode::Offset)->is_Con()) {
      // If the transformation of ConP to ConN+DecodeN is beneficial depends
      // on the platform and on the compressed oops mode.
      // Use addressing with narrow klass to load with offset on x86.
      // Some platforms can use the constant pool to load ConP.
      // Do this transformation here since IGVN will convert ConN back to ConP.
      const Type* t = addp->bottom_type();
      bool is_oop   = t->isa_oopptr() != NULL;
      bool is_klass = t->isa_klassptr() != NULL;

      if ((is_oop   && Matcher::const_oop_prefer_decode()  ) ||
          (is_klass && Matcher::const_klass_prefer_decode())) {
        Node* nn = NULL;

        int op = is_oop ? Op_ConN : Op_ConNKlass;

        // Look for existing ConN node of the same exact type.
        Node* r  = root();
        uint cnt = r->outcnt();
        for (uint i = 0; i < cnt; i++) {
          Node* m = r->raw_out(i);
          if (m!= NULL && m->Opcode() == op &&
              m->bottom_type()->make_ptr() == t) {
            nn = m;
            break;
          }
        }
        if (nn != NULL) {
          // Decode a narrow oop to match address
          // [R12 + narrow_oop_reg<<3 + offset]
          if (is_oop) {
            nn = new DecodeNNode(nn, t);
          } else {
            nn = new DecodeNKlassNode(nn, t);
          }
          // Check for succeeding AddP which uses the same Base.
          // Otherwise we will run into the assertion above when visiting that guy.
          for (uint i = 0; i < n->outcnt(); ++i) {
            Node *out_i = n->raw_out(i);
            if (out_i && out_i->is_AddP() && out_i->in(AddPNode::Base) == addp) {
              out_i->set_req(AddPNode::Base, nn);
#ifdef ASSERT
              for (uint j = 0; j < out_i->outcnt(); ++j) {
                Node *out_j = out_i->raw_out(j);
                assert(out_j == NULL || !out_j->is_AddP() || out_j->in(AddPNode::Base) != addp,
                       "more than 2 AddP nodes in a chain (out_j %u)", out_j->_idx);
              }
#endif
            }
          }
          n->set_req(AddPNode::Base, nn);
          n->set_req(AddPNode::Address, nn);
          if (addp->outcnt() == 0) {
            addp->disconnect_inputs(this);
          }
        }
      }
    }
#endif
    break;
  }

  case Op_CastPP: {
    // Remove CastPP nodes to gain more freedom during scheduling but
    // keep the dependency they encode as control or precedence edges
    // (if control is set already) on memory operations. Some CastPP
    // nodes don't have a control (don't carry a dependency): skip
    // those.
    if (n->in(0) != NULL) {
      ResourceMark rm;
      Unique_Node_List wq;
      wq.push(n);
      for (uint next = 0; next < wq.size(); ++next) {
        Node *m = wq.at(next);
        for (DUIterator_Fast imax, i = m->fast_outs(imax); i < imax; i++) {
          Node* use = m->fast_out(i);
          if (use->is_Mem() || use->is_EncodeNarrowPtr()) {
            use->ensure_control_or_add_prec(n->in(0));
          } else {
            switch(use->Opcode()) {
            case Op_AddP:
            case Op_DecodeN:
            case Op_DecodeNKlass:
            case Op_CheckCastPP:
            case Op_CastPP:
              wq.push(use);
              break;
            }
          }
        }
      }
    }
    const bool is_LP64 = LP64_ONLY(true) NOT_LP64(false);
    if (is_LP64 && n->in(1)->is_DecodeN() && Matcher::gen_narrow_oop_implicit_null_checks()) {
      Node* in1 = n->in(1);
      const Type* t = n->bottom_type();
      Node* new_in1 = in1->clone();
      new_in1->as_DecodeN()->set_type(t);

      if (!Matcher::narrow_oop_use_complex_address()) {
        //
        // x86, ARM and friends can handle 2 adds in addressing mode
        // and Matcher can fold a DecodeN node into address by using
        // a narrow oop directly and do implicit NULL check in address:
        //
        // [R12 + narrow_oop_reg<<3 + offset]
        // NullCheck narrow_oop_reg
        //
        // On other platforms (Sparc) we have to keep new DecodeN node and
        // use it to do implicit NULL check in address:
        //
        // decode_not_null narrow_oop_reg, base_reg
        // [base_reg + offset]
        // NullCheck base_reg
        //
        // Pin the new DecodeN node to non-null path on these platform (Sparc)
        // to keep the information to which NULL check the new DecodeN node
        // corresponds to use it as value in implicit_null_check().
        //
        new_in1->set_req(0, n->in(0));
      }

      n->subsume_by(new_in1, this);
      if (in1->outcnt() == 0) {
        in1->disconnect_inputs(this);
      }
    } else {
      n->subsume_by(n->in(1), this);
      if (n->outcnt() == 0) {
        n->disconnect_inputs(this);
      }
    }
    break;
  }
#ifdef _LP64
  case Op_CmpP:
    // Do this transformation here to preserve CmpPNode::sub() and
    // other TypePtr related Ideal optimizations (for example, ptr nullness).
    if (n->in(1)->is_DecodeNarrowPtr() || n->in(2)->is_DecodeNarrowPtr()) {
      Node* in1 = n->in(1);
      Node* in2 = n->in(2);
      if (!in1->is_DecodeNarrowPtr()) {
        in2 = in1;
        in1 = n->in(2);
      }
      assert(in1->is_DecodeNarrowPtr(), "sanity");

      Node* new_in2 = NULL;
      if (in2->is_DecodeNarrowPtr()) {
        assert(in2->Opcode() == in1->Opcode(), "must be same node type");
        new_in2 = in2->in(1);
      } else if (in2->Opcode() == Op_ConP) {
        const Type* t = in2->bottom_type();
        if (t == TypePtr::NULL_PTR) {
          assert(in1->is_DecodeN(), "compare klass to null?");
          // Don't convert CmpP null check into CmpN if compressed
          // oops implicit null check is not generated.
          // This will allow to generate normal oop implicit null check.
          if (Matcher::gen_narrow_oop_implicit_null_checks())
            new_in2 = ConNode::make(TypeNarrowOop::NULL_PTR);
          //
          // This transformation together with CastPP transformation above
          // will generated code for implicit NULL checks for compressed oops.
          //
          // The original code after Optimize()
          //
          //    LoadN memory, narrow_oop_reg
          //    decode narrow_oop_reg, base_reg
          //    CmpP base_reg, NULL
          //    CastPP base_reg // NotNull
          //    Load [base_reg + offset], val_reg
          //
          // after these transformations will be
          //
          //    LoadN memory, narrow_oop_reg
          //    CmpN narrow_oop_reg, NULL
          //    decode_not_null narrow_oop_reg, base_reg
          //    Load [base_reg + offset], val_reg
          //
          // and the uncommon path (== NULL) will use narrow_oop_reg directly
          // since narrow oops can be used in debug info now (see the code in
          // final_graph_reshaping_walk()).
          //
          // At the end the code will be matched to
          // on x86:
          //
          //    Load_narrow_oop memory, narrow_oop_reg
          //    Load [R12 + narrow_oop_reg<<3 + offset], val_reg
          //    NullCheck narrow_oop_reg
          //
          // and on sparc:
          //
          //    Load_narrow_oop memory, narrow_oop_reg
          //    decode_not_null narrow_oop_reg, base_reg
          //    Load [base_reg + offset], val_reg
          //    NullCheck base_reg
          //
        } else if (t->isa_oopptr()) {
          new_in2 = ConNode::make(t->make_narrowoop());
        } else if (t->isa_klassptr()) {
          new_in2 = ConNode::make(t->make_narrowklass());
        }
      }
      if (new_in2 != NULL) {
        Node* cmpN = new CmpNNode(in1->in(1), new_in2);
        n->subsume_by(cmpN, this);
        if (in1->outcnt() == 0) {
          in1->disconnect_inputs(this);
        }
        if (in2->outcnt() == 0) {
          in2->disconnect_inputs(this);
        }
      }
    }
    break;

  case Op_DecodeN:
  case Op_DecodeNKlass:
    assert(!n->in(1)->is_EncodeNarrowPtr(), "should be optimized out");
    // DecodeN could be pinned when it can't be fold into
    // an address expression, see the code for Op_CastPP above.
    assert(n->in(0) == NULL || (UseCompressedOops && !Matcher::narrow_oop_use_complex_address()), "no control");
    break;

  case Op_EncodeP:
  case Op_EncodePKlass: {
    Node* in1 = n->in(1);
    if (in1->is_DecodeNarrowPtr()) {
      n->subsume_by(in1->in(1), this);
    } else if (in1->Opcode() == Op_ConP) {
      const Type* t = in1->bottom_type();
      if (t == TypePtr::NULL_PTR) {
        assert(t->isa_oopptr(), "null klass?");
        n->subsume_by(ConNode::make(TypeNarrowOop::NULL_PTR), this);
      } else if (t->isa_oopptr()) {
        n->subsume_by(ConNode::make(t->make_narrowoop()), this);
      } else if (t->isa_klassptr()) {
        n->subsume_by(ConNode::make(t->make_narrowklass()), this);
      }
    }
    if (in1->outcnt() == 0) {
      in1->disconnect_inputs(this);
    }
    break;
  }

  case Op_Proj: {
    if (OptimizeStringConcat || IncrementalInline) {
      ProjNode* proj = n->as_Proj();
      if (proj->_is_io_use) {
        assert(proj->_con == TypeFunc::I_O || proj->_con == TypeFunc::Memory, "");
        // Separate projections were used for the exception path which
        // are normally removed by a late inline.  If it wasn't inlined
        // then they will hang around and should just be replaced with
        // the original one. Merge them.
        Node* non_io_proj = proj->in(0)->as_Multi()->proj_out_or_null(proj->_con, false /*is_io_use*/);
        if (non_io_proj  != NULL) {
          proj->subsume_by(non_io_proj , this);
        }
      }
    }
    break;
  }

  case Op_Phi:
    if (n->as_Phi()->bottom_type()->isa_narrowoop() || n->as_Phi()->bottom_type()->isa_narrowklass()) {
      // The EncodeP optimization may create Phi with the same edges
      // for all paths. It is not handled well by Register Allocator.
      Node* unique_in = n->in(1);
      assert(unique_in != NULL, "");
      uint cnt = n->req();
      for (uint i = 2; i < cnt; i++) {
        Node* m = n->in(i);
        assert(m != NULL, "");
        if (unique_in != m)
          unique_in = NULL;
      }
      if (unique_in != NULL) {
        n->subsume_by(unique_in, this);
      }
    }
    break;

#endif

#ifdef ASSERT
  case Op_CastII:
    // Verify that all range check dependent CastII nodes were removed.
    if (n->isa_CastII()->has_range_check()) {
      n->dump(3);
      assert(false, "Range check dependent CastII node was not removed");
    }
    break;
#endif

  case Op_ModI:
    if (UseDivMod) {
      // Check if a%b and a/b both exist
      Node* d = n->find_similar(Op_DivI);
      if (d) {
        // Replace them with a fused divmod if supported
        if (Matcher::has_match_rule(Op_DivModI)) {
          DivModINode* divmod = DivModINode::make(n);
          d->subsume_by(divmod->div_proj(), this);
          n->subsume_by(divmod->mod_proj(), this);
        } else {
          // replace a%b with a-((a/b)*b)
          Node* mult = new MulINode(d, d->in(2));
          Node* sub  = new SubINode(d->in(1), mult);
          n->subsume_by(sub, this);
        }
      }
    }
    break;

  case Op_ModL:
    if (UseDivMod) {
      // Check if a%b and a/b both exist
      Node* d = n->find_similar(Op_DivL);
      if (d) {
        // Replace them with a fused divmod if supported
        if (Matcher::has_match_rule(Op_DivModL)) {
          DivModLNode* divmod = DivModLNode::make(n);
          d->subsume_by(divmod->div_proj(), this);
          n->subsume_by(divmod->mod_proj(), this);
        } else {
          // replace a%b with a-((a/b)*b)
          Node* mult = new MulLNode(d, d->in(2));
          Node* sub  = new SubLNode(d->in(1), mult);
          n->subsume_by(sub, this);
        }
      }
    }
    break;

  case Op_LoadVector:
  case Op_StoreVector:
  case Op_LoadVectorGather:
  case Op_StoreVectorScatter:
  case Op_VectorCmpMasked:
  case Op_VectorMaskGen:
  case Op_LoadVectorMasked:
  case Op_StoreVectorMasked:
    break;

  case Op_AddReductionVI:
  case Op_AddReductionVL:
  case Op_AddReductionVF:
  case Op_AddReductionVD:
  case Op_MulReductionVI:
  case Op_MulReductionVL:
  case Op_MulReductionVF:
  case Op_MulReductionVD:
  case Op_MinReductionV:
  case Op_MaxReductionV:
  case Op_AndReductionV:
  case Op_OrReductionV:
  case Op_XorReductionV:
    break;

  case Op_PackB:
  case Op_PackS:
  case Op_PackI:
  case Op_PackF:
  case Op_PackL:
  case Op_PackD:
    if (n->req()-1 > 2) {
      // Replace many operand PackNodes with a binary tree for matching
      PackNode* p = (PackNode*) n;
      Node* btp = p->binary_tree_pack(1, n->req());
      n->subsume_by(btp, this);
    }
    break;
  case Op_Loop:
    assert(!n->as_Loop()->is_transformed_long_inner_loop() || _loop_opts_cnt == 0, "should have been turned into a counted loop");
  case Op_CountedLoop:
  case Op_LongCountedLoop:
  case Op_OuterStripMinedLoop:
    if (n->as_Loop()->is_inner_loop()) {
      frc.inc_inner_loop_count();
    }
    n->as_Loop()->verify_strip_mined(0);
    break;
  case Op_LShiftI:
  case Op_RShiftI:
  case Op_URShiftI:
  case Op_LShiftL:
  case Op_RShiftL:
  case Op_URShiftL:
    if (Matcher::need_masked_shift_count) {
      // The cpu's shift instructions don't restrict the count to the
      // lower 5/6 bits. We need to do the masking ourselves.
      Node* in2 = n->in(2);
      juint mask = (n->bottom_type() == TypeInt::INT) ? (BitsPerInt - 1) : (BitsPerLong - 1);
      const TypeInt* t = in2->find_int_type();
      if (t != NULL && t->is_con()) {
        juint shift = t->get_con();
        if (shift > mask) { // Unsigned cmp
          n->set_req(2, ConNode::make(TypeInt::make(shift & mask)));
        }
      } else {
        if (t == NULL || t->_lo < 0 || t->_hi > (int)mask) {
          Node* shift = new AndINode(in2, ConNode::make(TypeInt::make(mask)));
          n->set_req(2, shift);
        }
      }
      if (in2->outcnt() == 0) { // Remove dead node
        in2->disconnect_inputs(this);
      }
    }
    break;
  case Op_MemBarStoreStore:
  case Op_MemBarRelease:
    // Break the link with AllocateNode: it is no longer useful and
    // confuses register allocation.
    if (n->req() > MemBarNode::Precedent) {
      n->set_req(MemBarNode::Precedent, top());
    }
    break;
  case Op_MemBarAcquire: {
    if (n->as_MemBar()->trailing_load() && n->req() > MemBarNode::Precedent) {
      // At parse time, the trailing MemBarAcquire for a volatile load
      // is created with an edge to the load. After optimizations,
      // that input may be a chain of Phis. If those phis have no
      // other use, then the MemBarAcquire keeps them alive and
      // register allocation can be confused.
      ResourceMark rm;
      Unique_Node_List wq;
      wq.push(n->in(MemBarNode::Precedent));
      n->set_req(MemBarNode::Precedent, top());
      while (wq.size() > 0) {
        Node* m = wq.pop();
        if (m->outcnt() == 0) {
          for (uint j = 0; j < m->req(); j++) {
            Node* in = m->in(j);
            if (in != NULL) {
              wq.push(in);
            }
          }
          m->disconnect_inputs(this);
        }
      }
    }
    break;
  }
  case Op_Blackhole:
    break;
  case Op_RangeCheck: {
    RangeCheckNode* rc = n->as_RangeCheck();
    Node* iff = new IfNode(rc->in(0), rc->in(1), rc->_prob, rc->_fcnt);
    n->subsume_by(iff, this);
    frc._tests.push(iff);
    break;
  }
  case Op_ConvI2L: {
    if (!Matcher::convi2l_type_required) {
      // Code generation on some platforms doesn't need accurate
      // ConvI2L types. Widening the type can help remove redundant
      // address computations.
      n->as_Type()->set_type(TypeLong::INT);
      ResourceMark rm;
      Unique_Node_List wq;
      wq.push(n);
      for (uint next = 0; next < wq.size(); next++) {
        Node *m = wq.at(next);

        for(;;) {
          // Loop over all nodes with identical inputs edges as m
          Node* k = m->find_similar(m->Opcode());
          if (k == NULL) {
            break;
          }
          // Push their uses so we get a chance to remove node made
          // redundant
          for (DUIterator_Fast imax, i = k->fast_outs(imax); i < imax; i++) {
            Node* u = k->fast_out(i);
            if (u->Opcode() == Op_LShiftL ||
                u->Opcode() == Op_AddL ||
                u->Opcode() == Op_SubL ||
                u->Opcode() == Op_AddP) {
              wq.push(u);
            }
          }
          // Replace all nodes with identical edges as m with m
          k->subsume_by(m, this);
        }
      }
    }
    break;
  }
  case Op_CmpUL: {
    if (!Matcher::has_match_rule(Op_CmpUL)) {
      // No support for unsigned long comparisons
      ConINode* sign_pos = new ConINode(TypeInt::make(BitsPerLong - 1));
      Node* sign_bit_mask = new RShiftLNode(n->in(1), sign_pos);
      Node* orl = new OrLNode(n->in(1), sign_bit_mask);
      ConLNode* remove_sign_mask = new ConLNode(TypeLong::make(max_jlong));
      Node* andl = new AndLNode(orl, remove_sign_mask);
      Node* cmp = new CmpLNode(andl, n->in(2));
      n->subsume_by(cmp, this);
    }
    break;
  }
  default:
    assert(!n->is_Call(), "");
    assert(!n->is_Mem(), "");
    assert(nop != Op_ProfileBoolean, "should be eliminated during IGVN");
    break;
  }
}

//------------------------------final_graph_reshaping_walk---------------------
// Replacing Opaque nodes with their input in final_graph_reshaping_impl(),
// requires that the walk visits a node's inputs before visiting the node.
void Compile::final_graph_reshaping_walk( Node_Stack &nstack, Node *root, Final_Reshape_Counts &frc ) {
  Unique_Node_List sfpt;

  frc._visited.set(root->_idx); // first, mark node as visited
  uint cnt = root->req();
  Node *n = root;
  uint  i = 0;
  while (true) {
    if (i < cnt) {
      // Place all non-visited non-null inputs onto stack
      Node* m = n->in(i);
      ++i;
      if (m != NULL && !frc._visited.test_set(m->_idx)) {
        if (m->is_SafePoint() && m->as_SafePoint()->jvms() != NULL) {
          // compute worst case interpreter size in case of a deoptimization
          update_interpreter_frame_size(m->as_SafePoint()->jvms()->interpreter_frame_size());

          sfpt.push(m);
        }
        cnt = m->req();
        nstack.push(n, i); // put on stack parent and next input's index
        n = m;
        i = 0;
      }
    } else {
      // Now do post-visit work
      final_graph_reshaping_impl( n, frc );
      if (nstack.is_empty())
        break;             // finished
      n = nstack.node();   // Get node from stack
      cnt = n->req();
      i = nstack.index();
      nstack.pop();        // Shift to the next node on stack
    }
  }

  // Skip next transformation if compressed oops are not used.
  if ((UseCompressedOops && !Matcher::gen_narrow_oop_implicit_null_checks()) ||
      (!UseCompressedOops && !UseCompressedClassPointers))
    return;

  // Go over safepoints nodes to skip DecodeN/DecodeNKlass nodes for debug edges.
  // It could be done for an uncommon traps or any safepoints/calls
  // if the DecodeN/DecodeNKlass node is referenced only in a debug info.
  while (sfpt.size() > 0) {
    n = sfpt.pop();
    JVMState *jvms = n->as_SafePoint()->jvms();
    assert(jvms != NULL, "sanity");
    int start = jvms->debug_start();
    int end   = n->req();
    bool is_uncommon = (n->is_CallStaticJava() &&
                        n->as_CallStaticJava()->uncommon_trap_request() != 0);
    for (int j = start; j < end; j++) {
      Node* in = n->in(j);
      if (in->is_DecodeNarrowPtr()) {
        bool safe_to_skip = true;
        if (!is_uncommon ) {
          // Is it safe to skip?
          for (uint i = 0; i < in->outcnt(); i++) {
            Node* u = in->raw_out(i);
            if (!u->is_SafePoint() ||
                (u->is_Call() && u->as_Call()->has_non_debug_use(n))) {
              safe_to_skip = false;
            }
          }
        }
        if (safe_to_skip) {
          n->set_req(j, in->in(1));
        }
        if (in->outcnt() == 0) {
          in->disconnect_inputs(this);
        }
      }
    }
  }
}

//------------------------------final_graph_reshaping--------------------------
// Final Graph Reshaping.
//
// (1) Clone simple inputs to uncommon calls, so they can be scheduled late
//     and not commoned up and forced early.  Must come after regular
//     optimizations to avoid GVN undoing the cloning.  Clone constant
//     inputs to Loop Phis; these will be split by the allocator anyways.
//     Remove Opaque nodes.
// (2) Move last-uses by commutative operations to the left input to encourage
//     Intel update-in-place two-address operations and better register usage
//     on RISCs.  Must come after regular optimizations to avoid GVN Ideal
//     calls canonicalizing them back.
// (3) Count the number of double-precision FP ops, single-precision FP ops
//     and call sites.  On Intel, we can get correct rounding either by
//     forcing singles to memory (requires extra stores and loads after each
//     FP bytecode) or we can set a rounding mode bit (requires setting and
//     clearing the mode bit around call sites).  The mode bit is only used
//     if the relative frequency of single FP ops to calls is low enough.
//     This is a key transform for SPEC mpeg_audio.
// (4) Detect infinite loops; blobs of code reachable from above but not
//     below.  Several of the Code_Gen algorithms fail on such code shapes,
//     so we simply bail out.  Happens a lot in ZKM.jar, but also happens
//     from time to time in other codes (such as -Xcomp finalizer loops, etc).
//     Detection is by looking for IfNodes where only 1 projection is
//     reachable from below or CatchNodes missing some targets.
// (5) Assert for insane oop offsets in debug mode.

bool Compile::final_graph_reshaping() {
  // an infinite loop may have been eliminated by the optimizer,
  // in which case the graph will be empty.
  if (root()->req() == 1) {
    record_method_not_compilable("trivial infinite loop");
    return true;
  }

  // Expensive nodes have their control input set to prevent the GVN
  // from freely commoning them. There's no GVN beyond this point so
  // no need to keep the control input. We want the expensive nodes to
  // be freely moved to the least frequent code path by gcm.
  assert(OptimizeExpensiveOps || expensive_count() == 0, "optimization off but list non empty?");
  for (int i = 0; i < expensive_count(); i++) {
    _expensive_nodes.at(i)->set_req(0, NULL);
  }

  Final_Reshape_Counts frc;

  // Visit everybody reachable!
  // Allocate stack of size C->live_nodes()/2 to avoid frequent realloc
  Node_Stack nstack(live_nodes() >> 1);
  final_graph_reshaping_walk(nstack, root(), frc);

  // Check for unreachable (from below) code (i.e., infinite loops).
  for( uint i = 0; i < frc._tests.size(); i++ ) {
    MultiBranchNode *n = frc._tests[i]->as_MultiBranch();
    // Get number of CFG targets.
    // Note that PCTables include exception targets after calls.
    uint required_outcnt = n->required_outcnt();
    if (n->outcnt() != required_outcnt) {
      // Check for a few special cases.  Rethrow Nodes never take the
      // 'fall-thru' path, so expected kids is 1 less.
      if (n->is_PCTable() && n->in(0) && n->in(0)->in(0)) {
        if (n->in(0)->in(0)->is_Call()) {
          CallNode *call = n->in(0)->in(0)->as_Call();
          if (call->entry_point() == OptoRuntime::rethrow_stub()) {
            required_outcnt--;      // Rethrow always has 1 less kid
          } else if (call->req() > TypeFunc::Parms &&
                     call->is_CallDynamicJava()) {
            // Check for null receiver. In such case, the optimizer has
            // detected that the virtual call will always result in a null
            // pointer exception. The fall-through projection of this CatchNode
            // will not be populated.
            Node *arg0 = call->in(TypeFunc::Parms);
            if (arg0->is_Type() &&
                arg0->as_Type()->type()->higher_equal(TypePtr::NULL_PTR)) {
              required_outcnt--;
            }
          } else if (call->entry_point() == OptoRuntime::new_array_Java() &&
                     call->req() > TypeFunc::Parms+1 &&
                     call->is_CallStaticJava()) {
            // Check for negative array length. In such case, the optimizer has
            // detected that the allocation attempt will always result in an
            // exception. There is no fall-through projection of this CatchNode .
            Node *arg1 = call->in(TypeFunc::Parms+1);
            if (arg1->is_Type() &&
                arg1->as_Type()->type()->join(TypeInt::POS)->empty()) {
              required_outcnt--;
            }
          }
        }
      }
      // Recheck with a better notion of 'required_outcnt'
      if (n->outcnt() != required_outcnt) {
        record_method_not_compilable("malformed control flow");
        return true;            // Not all targets reachable!
      }
    }
    // Check that I actually visited all kids.  Unreached kids
    // must be infinite loops.
    for (DUIterator_Fast jmax, j = n->fast_outs(jmax); j < jmax; j++)
      if (!frc._visited.test(n->fast_out(j)->_idx)) {
        record_method_not_compilable("infinite loop");
        return true;            // Found unvisited kid; must be unreach
      }

    // Here so verification code in final_graph_reshaping_walk()
    // always see an OuterStripMinedLoopEnd
    if (n->is_OuterStripMinedLoopEnd() || n->is_LongCountedLoopEnd()) {
      IfNode* init_iff = n->as_If();
      Node* iff = new IfNode(init_iff->in(0), init_iff->in(1), init_iff->_prob, init_iff->_fcnt);
      n->subsume_by(iff, this);
    }
  }

#ifdef IA32
  // If original bytecodes contained a mixture of floats and doubles
  // check if the optimizer has made it homogenous, item (3).
  if (UseSSE == 0 &&
      frc.get_float_count() > 32 &&
      frc.get_double_count() == 0 &&
      (10 * frc.get_call_count() < frc.get_float_count()) ) {
    set_24_bit_selection_and_mode(false, true);
  }
#endif // IA32

  set_java_calls(frc.get_java_call_count());
  set_inner_loops(frc.get_inner_loop_count());

  // No infinite loops, no reason to bail out.
  return false;
}

//-----------------------------too_many_traps----------------------------------
// Report if there are too many traps at the current method and bci.
// Return true if there was a trap, and/or PerMethodTrapLimit is exceeded.
bool Compile::too_many_traps(ciMethod* method,
                             int bci,
                             Deoptimization::DeoptReason reason) {
  ciMethodData* md = method->method_data();
  if (md->is_empty()) {
    // Assume the trap has not occurred, or that it occurred only
    // because of a transient condition during start-up in the interpreter.
    return false;
  }
  ciMethod* m = Deoptimization::reason_is_speculate(reason) ? this->method() : NULL;
  if (md->has_trap_at(bci, m, reason) != 0) {
    // Assume PerBytecodeTrapLimit==0, for a more conservative heuristic.
    // Also, if there are multiple reasons, or if there is no per-BCI record,
    // assume the worst.
    if (log())
      log()->elem("observe trap='%s' count='%d'",
                  Deoptimization::trap_reason_name(reason),
                  md->trap_count(reason));
    return true;
  } else {
    // Ignore method/bci and see if there have been too many globally.
    return too_many_traps(reason, md);
  }
}

// Less-accurate variant which does not require a method and bci.
bool Compile::too_many_traps(Deoptimization::DeoptReason reason,
                             ciMethodData* logmd) {
  if (trap_count(reason) >= Deoptimization::per_method_trap_limit(reason)) {
    // Too many traps globally.
    // Note that we use cumulative trap_count, not just md->trap_count.
    if (log()) {
      int mcount = (logmd == NULL)? -1: (int)logmd->trap_count(reason);
      log()->elem("observe trap='%s' count='0' mcount='%d' ccount='%d'",
                  Deoptimization::trap_reason_name(reason),
                  mcount, trap_count(reason));
    }
    return true;
  } else {
    // The coast is clear.
    return false;
  }
}

//--------------------------too_many_recompiles--------------------------------
// Report if there are too many recompiles at the current method and bci.
// Consults PerBytecodeRecompilationCutoff and PerMethodRecompilationCutoff.
// Is not eager to return true, since this will cause the compiler to use
// Action_none for a trap point, to avoid too many recompilations.
bool Compile::too_many_recompiles(ciMethod* method,
                                  int bci,
                                  Deoptimization::DeoptReason reason) {
  ciMethodData* md = method->method_data();
  if (md->is_empty()) {
    // Assume the trap has not occurred, or that it occurred only
    // because of a transient condition during start-up in the interpreter.
    return false;
  }
  // Pick a cutoff point well within PerBytecodeRecompilationCutoff.
  uint bc_cutoff = (uint) PerBytecodeRecompilationCutoff / 8;
  uint m_cutoff  = (uint) PerMethodRecompilationCutoff / 2 + 1;  // not zero
  Deoptimization::DeoptReason per_bc_reason
    = Deoptimization::reason_recorded_per_bytecode_if_any(reason);
  ciMethod* m = Deoptimization::reason_is_speculate(reason) ? this->method() : NULL;
  if ((per_bc_reason == Deoptimization::Reason_none
       || md->has_trap_at(bci, m, reason) != 0)
      // The trap frequency measure we care about is the recompile count:
      && md->trap_recompiled_at(bci, m)
      && md->overflow_recompile_count() >= bc_cutoff) {
    // Do not emit a trap here if it has already caused recompilations.
    // Also, if there are multiple reasons, or if there is no per-BCI record,
    // assume the worst.
    if (log())
      log()->elem("observe trap='%s recompiled' count='%d' recompiles2='%d'",
                  Deoptimization::trap_reason_name(reason),
                  md->trap_count(reason),
                  md->overflow_recompile_count());
    return true;
  } else if (trap_count(reason) != 0
             && decompile_count() >= m_cutoff) {
    // Too many recompiles globally, and we have seen this sort of trap.
    // Use cumulative decompile_count, not just md->decompile_count.
    if (log())
      log()->elem("observe trap='%s' count='%d' mcount='%d' decompiles='%d' mdecompiles='%d'",
                  Deoptimization::trap_reason_name(reason),
                  md->trap_count(reason), trap_count(reason),
                  md->decompile_count(), decompile_count());
    return true;
  } else {
    // The coast is clear.
    return false;
  }
}

// Compute when not to trap. Used by matching trap based nodes and
// NullCheck optimization.
void Compile::set_allowed_deopt_reasons() {
  _allowed_reasons = 0;
  if (is_method_compilation()) {
    for (int rs = (int)Deoptimization::Reason_none+1; rs < Compile::trapHistLength; rs++) {
      assert(rs < BitsPerInt, "recode bit map");
      if (!too_many_traps((Deoptimization::DeoptReason) rs)) {
        _allowed_reasons |= nth_bit(rs);
      }
    }
  }
}

bool Compile::needs_clinit_barrier(ciMethod* method, ciMethod* accessing_method) {
  return method->is_static() && needs_clinit_barrier(method->holder(), accessing_method);
}

bool Compile::needs_clinit_barrier(ciField* field, ciMethod* accessing_method) {
  return field->is_static() && needs_clinit_barrier(field->holder(), accessing_method);
}

bool Compile::needs_clinit_barrier(ciInstanceKlass* holder, ciMethod* accessing_method) {
  if (holder->is_initialized()) {
    return false;
  }
  if (holder->is_being_initialized()) {
    if (accessing_method->holder() == holder) {
      // Access inside a class. The barrier can be elided when access happens in <clinit>,
      // <init>, or a static method. In all those cases, there was an initialization
      // barrier on the holder klass passed.
      if (accessing_method->is_static_initializer() ||
          accessing_method->is_object_initializer() ||
          accessing_method->is_static()) {
        return false;
      }
    } else if (accessing_method->holder()->is_subclass_of(holder)) {
      // Access from a subclass. The barrier can be elided only when access happens in <clinit>.
      // In case of <init> or a static method, the barrier is on the subclass is not enough:
      // child class can become fully initialized while its parent class is still being initialized.
      if (accessing_method->is_static_initializer()) {
        return false;
      }
    }
    ciMethod* root = method(); // the root method of compilation
    if (root != accessing_method) {
      return needs_clinit_barrier(holder, root); // check access in the context of compilation root
    }
  }
  return true;
}

#ifndef PRODUCT
//------------------------------verify_graph_edges---------------------------
// Walk the Graph and verify that there is a one-to-one correspondence
// between Use-Def edges and Def-Use edges in the graph.
void Compile::verify_graph_edges(bool no_dead_code) {
  if (VerifyGraphEdges) {
    Unique_Node_List visited;
    // Call recursive graph walk to check edges
    _root->verify_edges(visited);
    if (no_dead_code) {
      // Now make sure that no visited node is used by an unvisited node.
      bool dead_nodes = false;
      Unique_Node_List checked;
      while (visited.size() > 0) {
        Node* n = visited.pop();
        checked.push(n);
        for (uint i = 0; i < n->outcnt(); i++) {
          Node* use = n->raw_out(i);
          if (checked.member(use))  continue;  // already checked
          if (visited.member(use))  continue;  // already in the graph
          if (use->is_Con())        continue;  // a dead ConNode is OK
          // At this point, we have found a dead node which is DU-reachable.
          if (!dead_nodes) {
            tty->print_cr("*** Dead nodes reachable via DU edges:");
            dead_nodes = true;
          }
          use->dump(2);
          tty->print_cr("---");
          checked.push(use);  // No repeats; pretend it is now checked.
        }
      }
      assert(!dead_nodes, "using nodes must be reachable from root");
    }
  }
}
#endif

// The Compile object keeps track of failure reasons separately from the ciEnv.
// This is required because there is not quite a 1-1 relation between the
// ciEnv and its compilation task and the Compile object.  Note that one
// ciEnv might use two Compile objects, if C2Compiler::compile_method decides
// to backtrack and retry without subsuming loads.  Other than this backtracking
// behavior, the Compile's failure reason is quietly copied up to the ciEnv
// by the logic in C2Compiler.
void Compile::record_failure(const char* reason) {
  if (log() != NULL) {
    log()->elem("failure reason='%s' phase='compile'", reason);
  }
  if (_failure_reason == NULL) {
    // Record the first failure reason.
    _failure_reason = reason;
  }

  if (!C->failure_reason_is(C2Compiler::retry_no_subsuming_loads())) {
    C->print_method(PHASE_FAILURE);
  }
  _root = NULL;  // flush the graph, too
}

Compile::TracePhase::TracePhase(const char* name, elapsedTimer* accumulator)
  : TraceTime(name, accumulator, CITime, CITimeVerbose),
    _phase_name(name), _dolog(CITimeVerbose)
{
  if (_dolog) {
    C = Compile::current();
    _log = C->log();
  } else {
    C = NULL;
    _log = NULL;
  }
  if (_log != NULL) {
    _log->begin_head("phase name='%s' nodes='%d' live='%d'", _phase_name, C->unique(), C->live_nodes());
    _log->stamp();
    _log->end_head();
  }
}

Compile::TracePhase::~TracePhase() {

  C = Compile::current();
  if (_dolog) {
    _log = C->log();
  } else {
    _log = NULL;
  }

#ifdef ASSERT
  if (PrintIdealNodeCount) {
    tty->print_cr("phase name='%s' nodes='%d' live='%d' live_graph_walk='%d'",
                  _phase_name, C->unique(), C->live_nodes(), C->count_live_nodes_by_graph_walk());
  }

  if (VerifyIdealNodeCount) {
    Compile::current()->print_missing_nodes();
  }
#endif

  if (_log != NULL) {
    _log->done("phase name='%s' nodes='%d' live='%d'", _phase_name, C->unique(), C->live_nodes());
  }
}

//----------------------------static_subtype_check-----------------------------
// Shortcut important common cases when superklass is exact:
// (0) superklass is java.lang.Object (can occur in reflective code)
// (1) subklass is already limited to a subtype of superklass => always ok
// (2) subklass does not overlap with superklass => always fail
// (3) superklass has NO subtypes and we can check with a simple compare.
int Compile::static_subtype_check(ciKlass* superk, ciKlass* subk) {
  if (StressReflectiveCode) {
    return SSC_full_test;       // Let caller generate the general case.
  }

  if (superk == env()->Object_klass()) {
    return SSC_always_true;     // (0) this test cannot fail
  }

  ciType* superelem = superk;
  ciType* subelem = subk;
  if (superelem->is_array_klass()) {
    superelem = superelem->as_array_klass()->base_element_type();
  }
  if (subelem->is_array_klass()) {
    subelem = subelem->as_array_klass()->base_element_type();
  }

  if (!subk->is_interface()) {  // cannot trust static interface types yet
    if (subk->is_subtype_of(superk)) {
      return SSC_always_true;   // (1) false path dead; no dynamic test needed
    }
    if (!(superelem->is_klass() && superelem->as_klass()->is_interface()) &&
        !(subelem->is_klass() && subelem->as_klass()->is_interface()) &&
        !superk->is_subtype_of(subk)) {
      return SSC_always_false;  // (2) true path dead; no dynamic test needed
    }
  }

  // If casting to an instance klass, it must have no subtypes
  if (superk->is_interface()) {
    // Cannot trust interfaces yet.
    // %%% S.B. superk->nof_implementors() == 1
  } else if (superelem->is_instance_klass()) {
    ciInstanceKlass* ik = superelem->as_instance_klass();
    if (!ik->has_subklass() && !ik->is_interface()) {
      if (!ik->is_final()) {
        // Add a dependency if there is a chance of a later subclass.
        dependencies()->assert_leaf_type(ik);
      }
      return SSC_easy_test;     // (3) caller can do a simple ptr comparison
    }
  } else {
    // A primitive array type has no subtypes.
    return SSC_easy_test;       // (3) caller can do a simple ptr comparison
  }

  return SSC_full_test;
}

Node* Compile::conv_I2X_index(PhaseGVN* phase, Node* idx, const TypeInt* sizetype, Node* ctrl) {
#ifdef _LP64
  // The scaled index operand to AddP must be a clean 64-bit value.
  // Java allows a 32-bit int to be incremented to a negative
  // value, which appears in a 64-bit register as a large
  // positive number.  Using that large positive number as an
  // operand in pointer arithmetic has bad consequences.
  // On the other hand, 32-bit overflow is rare, and the possibility
  // can often be excluded, if we annotate the ConvI2L node with
  // a type assertion that its value is known to be a small positive
  // number.  (The prior range check has ensured this.)
  // This assertion is used by ConvI2LNode::Ideal.
  int index_max = max_jint - 1;  // array size is max_jint, index is one less
  if (sizetype != NULL) index_max = sizetype->_hi - 1;
  const TypeInt* iidxtype = TypeInt::make(0, index_max, Type::WidenMax);
  idx = constrained_convI2L(phase, idx, iidxtype, ctrl);
#endif
  return idx;
}

// Convert integer value to a narrowed long type dependent on ctrl (for example, a range check)
Node* Compile::constrained_convI2L(PhaseGVN* phase, Node* value, const TypeInt* itype, Node* ctrl, bool carry_dependency) {
  if (ctrl != NULL) {
    // Express control dependency by a CastII node with a narrow type.
    value = new CastIINode(value, itype, carry_dependency ? ConstraintCastNode::StrongDependency : ConstraintCastNode::RegularDependency, true /* range check dependency */);
    // Make the CastII node dependent on the control input to prevent the narrowed ConvI2L
    // node from floating above the range check during loop optimizations. Otherwise, the
    // ConvI2L node may be eliminated independently of the range check, causing the data path
    // to become TOP while the control path is still there (although it's unreachable).
    value->set_req(0, ctrl);
    value = phase->transform(value);
  }
  const TypeLong* ltype = TypeLong::make(itype->_lo, itype->_hi, itype->_widen);
  return phase->transform(new ConvI2LNode(value, ltype));
}

void Compile::print_inlining_stream_free() {
  if (_print_inlining_stream != NULL) {
    _print_inlining_stream->~stringStream();
    _print_inlining_stream = NULL;
  }
}

// The message about the current inlining is accumulated in
// _print_inlining_stream and transfered into the _print_inlining_list
// once we know whether inlining succeeds or not. For regular
// inlining, messages are appended to the buffer pointed by
// _print_inlining_idx in the _print_inlining_list. For late inlining,
// a new buffer is added after _print_inlining_idx in the list. This
// way we can update the inlining message for late inlining call site
// when the inlining is attempted again.
void Compile::print_inlining_init() {
  if (print_inlining() || print_intrinsics()) {
    // print_inlining_init is actually called several times.
    print_inlining_stream_free();
    _print_inlining_stream = new stringStream();
    _print_inlining_list = new (comp_arena())GrowableArray<PrintInliningBuffer*>(comp_arena(), 1, 1, new PrintInliningBuffer());
  }
}

void Compile::print_inlining_reinit() {
  if (print_inlining() || print_intrinsics()) {
    print_inlining_stream_free();
    // Re allocate buffer when we change ResourceMark
    _print_inlining_stream = new stringStream();
  }
}

void Compile::print_inlining_reset() {
  _print_inlining_stream->reset();
}

void Compile::print_inlining_commit() {
  assert(print_inlining() || print_intrinsics(), "PrintInlining off?");
  // Transfer the message from _print_inlining_stream to the current
  // _print_inlining_list buffer and clear _print_inlining_stream.
  _print_inlining_list->at(_print_inlining_idx)->ss()->write(_print_inlining_stream->base(), _print_inlining_stream->size());
  print_inlining_reset();
}

void Compile::print_inlining_push() {
  // Add new buffer to the _print_inlining_list at current position
  _print_inlining_idx++;
  _print_inlining_list->insert_before(_print_inlining_idx, new PrintInliningBuffer());
}

Compile::PrintInliningBuffer* Compile::print_inlining_current() {
  return _print_inlining_list->at(_print_inlining_idx);
}

void Compile::print_inlining_update(CallGenerator* cg) {
  if (print_inlining() || print_intrinsics()) {
    if (cg->is_late_inline()) {
      if (print_inlining_current()->cg() != cg &&
          (print_inlining_current()->cg() != NULL ||
           print_inlining_current()->ss()->size() != 0)) {
        print_inlining_push();
      }
      print_inlining_commit();
      print_inlining_current()->set_cg(cg);
    } else {
      if (print_inlining_current()->cg() != NULL) {
        print_inlining_push();
      }
      print_inlining_commit();
    }
  }
}

void Compile::print_inlining_move_to(CallGenerator* cg) {
  // We resume inlining at a late inlining call site. Locate the
  // corresponding inlining buffer so that we can update it.
  if (print_inlining() || print_intrinsics()) {
    for (int i = 0; i < _print_inlining_list->length(); i++) {
      if (_print_inlining_list->at(i)->cg() == cg) {
        _print_inlining_idx = i;
        return;
      }
    }
    ShouldNotReachHere();
  }
}

void Compile::print_inlining_update_delayed(CallGenerator* cg) {
  if (print_inlining() || print_intrinsics()) {
    assert(_print_inlining_stream->size() > 0, "missing inlining msg");
    assert(print_inlining_current()->cg() == cg, "wrong entry");
    // replace message with new message
    _print_inlining_list->at_put(_print_inlining_idx, new PrintInliningBuffer());
    print_inlining_commit();
    print_inlining_current()->set_cg(cg);
  }
}

void Compile::print_inlining_assert_ready() {
  assert(!_print_inlining || _print_inlining_stream->size() == 0, "loosing data");
}

void Compile::process_print_inlining() {
  assert(_late_inlines.length() == 0, "not drained yet");
  if (print_inlining() || print_intrinsics()) {
    ResourceMark rm;
    stringStream ss;
    assert(_print_inlining_list != NULL, "process_print_inlining should be called only once.");
    for (int i = 0; i < _print_inlining_list->length(); i++) {
      PrintInliningBuffer* pib = _print_inlining_list->at(i);
      ss.print("%s", pib->ss()->as_string());
      delete pib;
      DEBUG_ONLY(_print_inlining_list->at_put(i, NULL));
    }
    // Reset _print_inlining_list, it only contains destructed objects.
    // It is on the arena, so it will be freed when the arena is reset.
    _print_inlining_list = NULL;
    // _print_inlining_stream won't be used anymore, either.
    print_inlining_stream_free();
    size_t end = ss.size();
    _print_inlining_output = NEW_ARENA_ARRAY(comp_arena(), char, end+1);
    strncpy(_print_inlining_output, ss.base(), end+1);
    _print_inlining_output[end] = 0;
  }
}

void Compile::dump_print_inlining() {
  if (_print_inlining_output != NULL) {
    tty->print_raw(_print_inlining_output);
  }
}

void Compile::log_late_inline(CallGenerator* cg) {
  if (log() != NULL) {
    log()->head("late_inline method='%d'  inline_id='" JLONG_FORMAT "'", log()->identify(cg->method()),
                cg->unique_id());
    JVMState* p = cg->call_node()->jvms();
    while (p != NULL) {
      log()->elem("jvms bci='%d' method='%d'", p->bci(), log()->identify(p->method()));
      p = p->caller();
    }
    log()->tail("late_inline");
  }
}

void Compile::log_late_inline_failure(CallGenerator* cg, const char* msg) {
  log_late_inline(cg);
  if (log() != NULL) {
    log()->inline_fail(msg);
  }
}

void Compile::log_inline_id(CallGenerator* cg) {
  if (log() != NULL) {
    // The LogCompilation tool needs a unique way to identify late
    // inline call sites. This id must be unique for this call site in
    // this compilation. Try to have it unique across compilations as
    // well because it can be convenient when grepping through the log
    // file.
    // Distinguish OSR compilations from others in case CICountOSR is
    // on.
    jlong id = ((jlong)unique()) + (((jlong)compile_id()) << 33) + (CICountOSR && is_osr_compilation() ? ((jlong)1) << 32 : 0);
    cg->set_unique_id(id);
    log()->elem("inline_id id='" JLONG_FORMAT "'", id);
  }
}

void Compile::log_inline_failure(const char* msg) {
  if (C->log() != NULL) {
    C->log()->inline_fail(msg);
  }
}


// Dump inlining replay data to the stream.
// Don't change thread state and acquire any locks.
void Compile::dump_inline_data(outputStream* out) {
  InlineTree* inl_tree = ilt();
  if (inl_tree != NULL) {
    out->print(" inline %d", inl_tree->count());
    inl_tree->dump_replay_data(out);
  }
}

int Compile::cmp_expensive_nodes(Node* n1, Node* n2) {
  if (n1->Opcode() < n2->Opcode())      return -1;
  else if (n1->Opcode() > n2->Opcode()) return 1;

  assert(n1->req() == n2->req(), "can't compare %s nodes: n1->req() = %d, n2->req() = %d", NodeClassNames[n1->Opcode()], n1->req(), n2->req());
  for (uint i = 1; i < n1->req(); i++) {
    if (n1->in(i) < n2->in(i))      return -1;
    else if (n1->in(i) > n2->in(i)) return 1;
  }

  return 0;
}

int Compile::cmp_expensive_nodes(Node** n1p, Node** n2p) {
  Node* n1 = *n1p;
  Node* n2 = *n2p;

  return cmp_expensive_nodes(n1, n2);
}

void Compile::sort_expensive_nodes() {
  if (!expensive_nodes_sorted()) {
    _expensive_nodes.sort(cmp_expensive_nodes);
  }
}

bool Compile::expensive_nodes_sorted() const {
  for (int i = 1; i < _expensive_nodes.length(); i++) {
    if (cmp_expensive_nodes(_expensive_nodes.adr_at(i), _expensive_nodes.adr_at(i-1)) < 0) {
      return false;
    }
  }
  return true;
}

bool Compile::should_optimize_expensive_nodes(PhaseIterGVN &igvn) {
  if (_expensive_nodes.length() == 0) {
    return false;
  }

  assert(OptimizeExpensiveOps, "optimization off?");

  // Take this opportunity to remove dead nodes from the list
  int j = 0;
  for (int i = 0; i < _expensive_nodes.length(); i++) {
    Node* n = _expensive_nodes.at(i);
    if (!n->is_unreachable(igvn)) {
      assert(n->is_expensive(), "should be expensive");
      _expensive_nodes.at_put(j, n);
      j++;
    }
  }
  _expensive_nodes.trunc_to(j);

  // Then sort the list so that similar nodes are next to each other
  // and check for at least two nodes of identical kind with same data
  // inputs.
  sort_expensive_nodes();

  for (int i = 0; i < _expensive_nodes.length()-1; i++) {
    if (cmp_expensive_nodes(_expensive_nodes.adr_at(i), _expensive_nodes.adr_at(i+1)) == 0) {
      return true;
    }
  }

  return false;
}

void Compile::cleanup_expensive_nodes(PhaseIterGVN &igvn) {
  if (_expensive_nodes.length() == 0) {
    return;
  }

  assert(OptimizeExpensiveOps, "optimization off?");

  // Sort to bring similar nodes next to each other and clear the
  // control input of nodes for which there's only a single copy.
  sort_expensive_nodes();

  int j = 0;
  int identical = 0;
  int i = 0;
  bool modified = false;
  for (; i < _expensive_nodes.length()-1; i++) {
    assert(j <= i, "can't write beyond current index");
    if (_expensive_nodes.at(i)->Opcode() == _expensive_nodes.at(i+1)->Opcode()) {
      identical++;
      _expensive_nodes.at_put(j++, _expensive_nodes.at(i));
      continue;
    }
    if (identical > 0) {
      _expensive_nodes.at_put(j++, _expensive_nodes.at(i));
      identical = 0;
    } else {
      Node* n = _expensive_nodes.at(i);
      igvn.replace_input_of(n, 0, NULL);
      igvn.hash_insert(n);
      modified = true;
    }
  }
  if (identical > 0) {
    _expensive_nodes.at_put(j++, _expensive_nodes.at(i));
  } else if (_expensive_nodes.length() >= 1) {
    Node* n = _expensive_nodes.at(i);
    igvn.replace_input_of(n, 0, NULL);
    igvn.hash_insert(n);
    modified = true;
  }
  _expensive_nodes.trunc_to(j);
  if (modified) {
    igvn.optimize();
  }
}

void Compile::add_expensive_node(Node * n) {
  assert(!_expensive_nodes.contains(n), "duplicate entry in expensive list");
  assert(n->is_expensive(), "expensive nodes with non-null control here only");
  assert(!n->is_CFG() && !n->is_Mem(), "no cfg or memory nodes here");
  if (OptimizeExpensiveOps) {
    _expensive_nodes.append(n);
  } else {
    // Clear control input and let IGVN optimize expensive nodes if
    // OptimizeExpensiveOps is off.
    n->set_req(0, NULL);
  }
}

/**
 * Track coarsened Lock and Unlock nodes.
 */

class Lock_List : public Node_List {
  uint _origin_cnt;
public:
  Lock_List(Arena *a, uint cnt) : Node_List(a), _origin_cnt(cnt) {}
  uint origin_cnt() const { return _origin_cnt; }
};

void Compile::add_coarsened_locks(GrowableArray<AbstractLockNode*>& locks) {
  int length = locks.length();
  if (length > 0) {
    // Have to keep this list until locks elimination during Macro nodes elimination.
    Lock_List* locks_list = new (comp_arena()) Lock_List(comp_arena(), length);
    for (int i = 0; i < length; i++) {
      AbstractLockNode* lock = locks.at(i);
      assert(lock->is_coarsened(), "expecting only coarsened AbstractLock nodes, but got '%s'[%d] node", lock->Name(), lock->_idx);
      locks_list->push(lock);
    }
    _coarsened_locks.append(locks_list);
  }
}

void Compile::remove_useless_coarsened_locks(Unique_Node_List& useful) {
  int count = coarsened_count();
  for (int i = 0; i < count; i++) {
    Node_List* locks_list = _coarsened_locks.at(i);
    for (uint j = 0; j < locks_list->size(); j++) {
      Node* lock = locks_list->at(j);
      assert(lock->is_AbstractLock(), "sanity");
      if (!useful.member(lock)) {
        locks_list->yank(lock);
      }
    }
  }
}

void Compile::remove_coarsened_lock(Node* n) {
  if (n->is_AbstractLock()) {
    int count = coarsened_count();
    for (int i = 0; i < count; i++) {
      Node_List* locks_list = _coarsened_locks.at(i);
      locks_list->yank(n);
    }
  }
}

bool Compile::coarsened_locks_consistent() {
  int count = coarsened_count();
  for (int i = 0; i < count; i++) {
    bool unbalanced = false;
    bool modified = false; // track locks kind modifications
    Lock_List* locks_list = (Lock_List*)_coarsened_locks.at(i);
    uint size = locks_list->size();
    if (size == 0) {
      unbalanced = false; // All locks were eliminated - good
    } else if (size != locks_list->origin_cnt()) {
      unbalanced = true; // Some locks were removed from list
    } else {
      for (uint j = 0; j < size; j++) {
        Node* lock = locks_list->at(j);
        // All nodes in group should have the same state (modified or not)
        if (!lock->as_AbstractLock()->is_coarsened()) {
          if (j == 0) {
            // first on list was modified, the rest should be too for consistency
            modified = true;
          } else if (!modified) {
            // this lock was modified but previous locks on the list were not
            unbalanced = true;
            break;
          }
        } else if (modified) {
          // previous locks on list were modified but not this lock
          unbalanced = true;
          break;
        }
      }
    }
    if (unbalanced) {
      // unbalanced monitor enter/exit - only some [un]lock nodes were removed or modified
#ifdef ASSERT
      if (PrintEliminateLocks) {
        tty->print_cr("=== unbalanced coarsened locks ===");
        for (uint l = 0; l < size; l++) {
          locks_list->at(l)->dump();
        }
      }
#endif
      record_failure(C2Compiler::retry_no_locks_coarsening());
      return false;
    }
  }
  return true;
}

/**
 * Remove the speculative part of types and clean up the graph
 */
void Compile::remove_speculative_types(PhaseIterGVN &igvn) {
  if (UseTypeSpeculation) {
    Unique_Node_List worklist;
    worklist.push(root());
    int modified = 0;
    // Go over all type nodes that carry a speculative type, drop the
    // speculative part of the type and enqueue the node for an igvn
    // which may optimize it out.
    for (uint next = 0; next < worklist.size(); ++next) {
      Node *n  = worklist.at(next);
      if (n->is_Type()) {
        TypeNode* tn = n->as_Type();
        const Type* t = tn->type();
        const Type* t_no_spec = t->remove_speculative();
        if (t_no_spec != t) {
          bool in_hash = igvn.hash_delete(n);
          assert(in_hash, "node should be in igvn hash table");
          tn->set_type(t_no_spec);
          igvn.hash_insert(n);
          igvn._worklist.push(n); // give it a chance to go away
          modified++;
        }
      }
      // Iterate over outs - endless loops is unreachable from below
      for (DUIterator_Fast imax, i = n->fast_outs(imax); i < imax; i++) {
        Node *m = n->fast_out(i);
        if (not_a_node(m)) {
          continue;
        }
        worklist.push(m);
      }
    }
    // Drop the speculative part of all types in the igvn's type table
    igvn.remove_speculative_types();
    if (modified > 0) {
      igvn.optimize();
    }
#ifdef ASSERT
    // Verify that after the IGVN is over no speculative type has resurfaced
    worklist.clear();
    worklist.push(root());
    for (uint next = 0; next < worklist.size(); ++next) {
      Node *n  = worklist.at(next);
      const Type* t = igvn.type_or_null(n);
      assert((t == NULL) || (t == t->remove_speculative()), "no more speculative types");
      if (n->is_Type()) {
        t = n->as_Type()->type();
        assert(t == t->remove_speculative(), "no more speculative types");
      }
      // Iterate over outs - endless loops is unreachable from below
      for (DUIterator_Fast imax, i = n->fast_outs(imax); i < imax; i++) {
        Node *m = n->fast_out(i);
        if (not_a_node(m)) {
          continue;
        }
        worklist.push(m);
      }
    }
    igvn.check_no_speculative_types();
#endif
  }
}

// Auxiliary methods to support randomized stressing/fuzzing.

int Compile::random() {
  _stress_seed = os::next_random(_stress_seed);
  return static_cast<int>(_stress_seed);
}

// This method can be called the arbitrary number of times, with current count
// as the argument. The logic allows selecting a single candidate from the
// running list of candidates as follows:
//    int count = 0;
//    Cand* selected = null;
//    while(cand = cand->next()) {
//      if (randomized_select(++count)) {
//        selected = cand;
//      }
//    }
//
// Including count equalizes the chances any candidate is "selected".
// This is useful when we don't have the complete list of candidates to choose
// from uniformly. In this case, we need to adjust the randomicity of the
// selection, or else we will end up biasing the selection towards the latter
// candidates.
//
// Quick back-envelope calculation shows that for the list of n candidates
// the equal probability for the candidate to persist as "best" can be
// achieved by replacing it with "next" k-th candidate with the probability
// of 1/k. It can be easily shown that by the end of the run, the
// probability for any candidate is converged to 1/n, thus giving the
// uniform distribution among all the candidates.
//
// We don't care about the domain size as long as (RANDOMIZED_DOMAIN / count) is large.
#define RANDOMIZED_DOMAIN_POW 29
#define RANDOMIZED_DOMAIN (1 << RANDOMIZED_DOMAIN_POW)
#define RANDOMIZED_DOMAIN_MASK ((1 << (RANDOMIZED_DOMAIN_POW + 1)) - 1)
bool Compile::randomized_select(int count) {
  assert(count > 0, "only positive");
  return (random() & RANDOMIZED_DOMAIN_MASK) < (RANDOMIZED_DOMAIN / count);
}

CloneMap&     Compile::clone_map()                 { return _clone_map; }
void          Compile::set_clone_map(Dict* d)      { _clone_map._dict = d; }

void NodeCloneInfo::dump() const {
  tty->print(" {%d:%d} ", idx(), gen());
}

void CloneMap::clone(Node* old, Node* nnn, int gen) {
  uint64_t val = value(old->_idx);
  NodeCloneInfo cio(val);
  assert(val != 0, "old node should be in the map");
  NodeCloneInfo cin(cio.idx(), gen + cio.gen());
  insert(nnn->_idx, cin.get());
#ifndef PRODUCT
  if (is_debug()) {
    tty->print_cr("CloneMap::clone inserted node %d info {%d:%d} into CloneMap", nnn->_idx, cin.idx(), cin.gen());
  }
#endif
}

void CloneMap::verify_insert_and_clone(Node* old, Node* nnn, int gen) {
  NodeCloneInfo cio(value(old->_idx));
  if (cio.get() == 0) {
    cio.set(old->_idx, 0);
    insert(old->_idx, cio.get());
#ifndef PRODUCT
    if (is_debug()) {
      tty->print_cr("CloneMap::verify_insert_and_clone inserted node %d info {%d:%d} into CloneMap", old->_idx, cio.idx(), cio.gen());
    }
#endif
  }
  clone(old, nnn, gen);
}

int CloneMap::max_gen() const {
  int g = 0;
  DictI di(_dict);
  for(; di.test(); ++di) {
    int t = gen(di._key);
    if (g < t) {
      g = t;
#ifndef PRODUCT
      if (is_debug()) {
        tty->print_cr("CloneMap::max_gen() update max=%d from %d", g, _2_node_idx_t(di._key));
      }
#endif
    }
  }
  return g;
}

void CloneMap::dump(node_idx_t key) const {
  uint64_t val = value(key);
  if (val != 0) {
    NodeCloneInfo ni(val);
    ni.dump();
  }
}

// Move Allocate nodes to the start of the list
void Compile::sort_macro_nodes() {
  int count = macro_count();
  int allocates = 0;
  for (int i = 0; i < count; i++) {
    Node* n = macro_node(i);
    if (n->is_Allocate()) {
      if (i != allocates) {
        Node* tmp = macro_node(allocates);
        _macro_nodes.at_put(allocates, n);
        _macro_nodes.at_put(i, tmp);
      }
      allocates++;
    }
  }
}

void Compile::print_method(CompilerPhaseType cpt, const char *name, int level) {
  EventCompilerPhase event;
  if (event.should_commit()) {
    CompilerEvent::PhaseEvent::post(event, C->_latest_stage_start_counter, cpt, C->_compile_id, level);
  }
#ifndef PRODUCT
  if (should_print(level)) {
    _printer->print_method(name, level);
  }
#endif
  C->_latest_stage_start_counter.stamp();
}

void Compile::print_method(CompilerPhaseType cpt, int level, int idx) {
  char output[1024];
#ifndef PRODUCT
  if (idx != 0) {
    jio_snprintf(output, sizeof(output), "%s:%d", CompilerPhaseTypeHelper::to_string(cpt), idx);
  } else {
    jio_snprintf(output, sizeof(output), "%s", CompilerPhaseTypeHelper::to_string(cpt));
  }
#endif
  print_method(cpt, output, level);
}

void Compile::print_method(CompilerPhaseType cpt, Node* n, int level) {
  ResourceMark rm;
  stringStream ss;
  ss.print_raw(CompilerPhaseTypeHelper::to_string(cpt));
  if (n != NULL) {
    ss.print(": %d %s ", n->_idx, NodeClassNames[n->Opcode()]);
  } else {
    ss.print_raw(": NULL");
  }
  C->print_method(cpt, ss.as_string(), level);
}

void Compile::end_method(int level) {
  EventCompilerPhase event;
  if (event.should_commit()) {
    CompilerEvent::PhaseEvent::post(event, C->_latest_stage_start_counter, PHASE_END, C->_compile_id, level);
  }

#ifndef PRODUCT
  if (_method != NULL && should_print(level)) {
    _printer->end_method();
  }
#endif
}


#ifndef PRODUCT
IdealGraphPrinter* Compile::_debug_file_printer = NULL;
IdealGraphPrinter* Compile::_debug_network_printer = NULL;

// Called from debugger. Prints method to the default file with the default phase name.
// This works regardless of any Ideal Graph Visualizer flags set or not.
void igv_print() {
  Compile::current()->igv_print_method_to_file();
}

// Same as igv_print() above but with a specified phase name.
void igv_print(const char* phase_name) {
  Compile::current()->igv_print_method_to_file(phase_name);
}

// Called from debugger. Prints method with the default phase name to the default network or the one specified with
// the network flags for the Ideal Graph Visualizer, or to the default file depending on the 'network' argument.
// This works regardless of any Ideal Graph Visualizer flags set or not.
void igv_print(bool network) {
  if (network) {
    Compile::current()->igv_print_method_to_network();
  } else {
    Compile::current()->igv_print_method_to_file();
  }
}

// Same as igv_print(bool network) above but with a specified phase name.
void igv_print(bool network, const char* phase_name) {
  if (network) {
    Compile::current()->igv_print_method_to_network(phase_name);
  } else {
    Compile::current()->igv_print_method_to_file(phase_name);
  }
}

// Called from debugger. Normal write to the default _printer. Only works if Ideal Graph Visualizer printing flags are set.
void igv_print_default() {
  Compile::current()->print_method(PHASE_DEBUG, 0);
}

// Called from debugger, especially when replaying a trace in which the program state cannot be altered like with rr replay.
// A method is appended to an existing default file with the default phase name. This means that igv_append() must follow
// an earlier igv_print(*) call which sets up the file. This works regardless of any Ideal Graph Visualizer flags set or not.
void igv_append() {
  Compile::current()->igv_print_method_to_file("Debug", true);
}

// Same as igv_append() above but with a specified phase name.
void igv_append(const char* phase_name) {
  Compile::current()->igv_print_method_to_file(phase_name, true);
}

void Compile::igv_print_method_to_file(const char* phase_name, bool append) {
  const char* file_name = "custom_debug.xml";
  if (_debug_file_printer == NULL) {
    _debug_file_printer = new IdealGraphPrinter(C, file_name, append);
  } else {
    _debug_file_printer->update_compiled_method(C->method());
  }
  tty->print_cr("Method %s to %s", append ? "appended" : "printed", file_name);
  _debug_file_printer->print(phase_name, (Node*)C->root());
}

void Compile::igv_print_method_to_network(const char* phase_name) {
  if (_debug_network_printer == NULL) {
    _debug_network_printer = new IdealGraphPrinter(C);
  } else {
    _debug_network_printer->update_compiled_method(C->method());
  }
  tty->print_cr("Method printed over network stream to IGV");
  _debug_network_printer->print(phase_name, (Node*)C->root());
}
#endif

void Compile::add_native_invoker(RuntimeStub* stub) {
  _native_invokers.append(stub);
}

Node* Compile::narrow_value(BasicType bt, Node* value, const Type* type, PhaseGVN* phase, bool transform_res) {
  if (type != NULL && phase->type(value)->higher_equal(type)) {
    return value;
  }
  Node* result = NULL;
  if (bt == T_BYTE) {
    result = phase->transform(new LShiftINode(value, phase->intcon(24)));
    result = new RShiftINode(result, phase->intcon(24));
  } else if (bt == T_BOOLEAN) {
    result = new AndINode(value, phase->intcon(0xFF));
  } else if (bt == T_CHAR) {
    result = new AndINode(value,phase->intcon(0xFFFF));
  } else {
    assert(bt == T_SHORT, "unexpected narrow type");
    result = phase->transform(new LShiftINode(value, phase->intcon(16)));
    result = new RShiftINode(result, phase->intcon(16));
  }
  if (transform_res) {
    result = phase->transform(result);
  }
  return result;
}

