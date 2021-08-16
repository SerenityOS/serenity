/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/javaClasses.hpp"
#include "compiler/compileLog.hpp"
#include "opto/addnode.hpp"
#include "opto/callGenerator.hpp"
#include "opto/callnode.hpp"
#include "opto/divnode.hpp"
#include "opto/graphKit.hpp"
#include "opto/idealKit.hpp"
#include "opto/rootnode.hpp"
#include "opto/runtime.hpp"
#include "opto/stringopts.hpp"
#include "opto/subnode.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"

#define __ kit.

class StringConcat : public ResourceObj {
 private:
  PhaseStringOpts*    _stringopts;
  Node*               _string_alloc;
  AllocateNode*       _begin;          // The allocation the begins the pattern
  CallStaticJavaNode* _end;            // The final call of the pattern.  Will either be
                                       // SB.toString or or String.<init>(SB.toString)
  bool                _multiple;       // indicates this is a fusion of two or more
                                       // separate StringBuilders

  Node*               _arguments;      // The list of arguments to be concatenated
  GrowableArray<int>  _mode;           // into a String along with a mode flag
                                       // indicating how to treat the value.
  Node_List           _constructors;   // List of constructors (many in case of stacked concat)
  Node_List           _control;        // List of control nodes that will be deleted
  Node_List           _uncommon_traps; // Uncommon traps that needs to be rewritten
                                       // to restart at the initial JVMState.

 public:
  // Mode for converting arguments to Strings
  enum {
    StringMode,
    IntMode,
    CharMode,
    StringNullCheckMode
  };

  StringConcat(PhaseStringOpts* stringopts, CallStaticJavaNode* end):
    _stringopts(stringopts),
    _string_alloc(NULL),
    _begin(NULL),
    _end(end),
    _multiple(false) {
    _arguments = new Node(1);
    _arguments->del_req(0);
  }

  bool validate_mem_flow();
  bool validate_control_flow();

  void merge_add() {
#if 0
    // XXX This is place holder code for reusing an existing String
    // allocation but the logic for checking the state safety is
    // probably inadequate at the moment.
    CallProjections endprojs;
    sc->end()->extract_projections(&endprojs, false);
    if (endprojs.resproj != NULL) {
      for (SimpleDUIterator i(endprojs.resproj); i.has_next(); i.next()) {
        CallStaticJavaNode *use = i.get()->isa_CallStaticJava();
        if (use != NULL && use->method() != NULL &&
            use->method()->intrinsic_id() == vmIntrinsics::_String_String &&
            use->in(TypeFunc::Parms + 1) == endprojs.resproj) {
          // Found useless new String(sb.toString()) so reuse the newly allocated String
          // when creating the result instead of allocating a new one.
          sc->set_string_alloc(use->in(TypeFunc::Parms));
          sc->set_end(use);
        }
      }
    }
#endif
  }

  StringConcat* merge(StringConcat* other, Node* arg);

  void set_allocation(AllocateNode* alloc) {
    _begin = alloc;
  }

  void append(Node* value, int mode) {
    _arguments->add_req(value);
    _mode.append(mode);
  }
  void push(Node* value, int mode) {
    _arguments->ins_req(0, value);
    _mode.insert_before(0, mode);
  }

  void push_string(Node* value) {
    push(value, StringMode);
  }
  void push_string_null_check(Node* value) {
    push(value, StringNullCheckMode);
  }
  void push_int(Node* value) {
    push(value, IntMode);
  }
  void push_char(Node* value) {
    push(value, CharMode);
  }

  static bool is_SB_toString(Node* call) {
    if (call->is_CallStaticJava()) {
      CallStaticJavaNode* csj = call->as_CallStaticJava();
      ciMethod* m = csj->method();
      if (m != NULL &&
          (m->intrinsic_id() == vmIntrinsics::_StringBuilder_toString ||
           m->intrinsic_id() == vmIntrinsics::_StringBuffer_toString)) {
        return true;
      }
    }
    return false;
  }

  static Node* skip_string_null_check(Node* value) {
    // Look for a diamond shaped Null check of toString() result
    // (could be code from String.valueOf()):
    // (Proj == NULL) ? "null":"CastPP(Proj)#NotNULL
    if (value->is_Phi()) {
      int true_path = value->as_Phi()->is_diamond_phi();
      if (true_path != 0) {
        // phi->region->if_proj->ifnode->bool
        BoolNode* b = value->in(0)->in(1)->in(0)->in(1)->as_Bool();
        Node* cmp = b->in(1);
        Node* v1 = cmp->in(1);
        Node* v2 = cmp->in(2);
        // Null check of the return of toString which can simply be skipped.
        if (b->_test._test == BoolTest::ne &&
            v2->bottom_type() == TypePtr::NULL_PTR &&
            value->in(true_path)->Opcode() == Op_CastPP &&
            value->in(true_path)->in(1) == v1 &&
            v1->is_Proj() && is_SB_toString(v1->in(0))) {
          return v1;
        }
      }
    }
    return value;
  }

  Node* argument(int i) {
    return _arguments->in(i);
  }
  Node* argument_uncast(int i) {
    Node* arg = argument(i);
    int amode = mode(i);
    if (amode == StringConcat::StringMode ||
        amode == StringConcat::StringNullCheckMode) {
      arg = skip_string_null_check(arg);
    }
    return arg;
  }
  void set_argument(int i, Node* value) {
    _arguments->set_req(i, value);
  }
  int num_arguments() {
    return _mode.length();
  }
  int mode(int i) {
    return _mode.at(i);
  }
  void add_control(Node* ctrl) {
    assert(!_control.contains(ctrl), "only push once");
    _control.push(ctrl);
  }
  void add_constructor(Node* init) {
    assert(!_constructors.contains(init), "only push once");
    _constructors.push(init);
  }
  CallStaticJavaNode* end() { return _end; }
  AllocateNode* begin() { return _begin; }
  Node* string_alloc() { return _string_alloc; }

  void eliminate_unneeded_control();
  void eliminate_initialize(InitializeNode* init);
  void eliminate_call(CallNode* call);

  void maybe_log_transform() {
    CompileLog* log = _stringopts->C->log();
    if (log != NULL) {
      log->head("replace_string_concat arguments='%d' string_alloc='%d' multiple='%d'",
                num_arguments(),
                _string_alloc != NULL,
                _multiple);
      JVMState* p = _begin->jvms();
      while (p != NULL) {
        log->elem("jvms bci='%d' method='%d'", p->bci(), log->identify(p->method()));
        p = p->caller();
      }
      log->tail("replace_string_concat");
    }
  }

  void convert_uncommon_traps(GraphKit& kit, const JVMState* jvms) {
    for (uint u = 0; u < _uncommon_traps.size(); u++) {
      Node* uct = _uncommon_traps.at(u);

      // Build a new call using the jvms state of the allocate
      address call_addr = SharedRuntime::uncommon_trap_blob()->entry_point();
      const TypeFunc* call_type = OptoRuntime::uncommon_trap_Type();
      const TypePtr* no_memory_effects = NULL;
      Compile* C = _stringopts->C;
      CallStaticJavaNode* call = new CallStaticJavaNode(call_type, call_addr, "uncommon_trap",
                                                        no_memory_effects);
      for (int e = 0; e < TypeFunc::Parms; e++) {
        call->init_req(e, uct->in(e));
      }
      // Set the trap request to record intrinsic failure if this trap
      // is taken too many times.  Ideally we would handle then traps by
      // doing the original bookkeeping in the MDO so that if it caused
      // the code to be thrown out we could still recompile and use the
      // optimization.  Failing the uncommon traps doesn't really mean
      // that the optimization is a bad idea but there's no other way to
      // do the MDO updates currently.
      int trap_request = Deoptimization::make_trap_request(Deoptimization::Reason_intrinsic,
                                                           Deoptimization::Action_make_not_entrant);
      call->init_req(TypeFunc::Parms, __ intcon(trap_request));
      kit.add_safepoint_edges(call);

      _stringopts->gvn()->transform(call);
      C->gvn_replace_by(uct, call);
      uct->disconnect_inputs(C);
    }
  }

  void cleanup() {
    // disconnect the hook node
    _arguments->disconnect_inputs(_stringopts->C);
  }
};


void StringConcat::eliminate_unneeded_control() {
  for (uint i = 0; i < _control.size(); i++) {
    Node* n = _control.at(i);
    if (n->is_Allocate()) {
      eliminate_initialize(n->as_Allocate()->initialization());
    }
    if (n->is_Call()) {
      if (n != _end) {
        eliminate_call(n->as_Call());
      }
    } else if (n->is_IfTrue()) {
      Compile* C = _stringopts->C;
      C->gvn_replace_by(n, n->in(0)->in(0));
      // get rid of the other projection
      C->gvn_replace_by(n->in(0)->as_If()->proj_out(false), C->top());
    }
  }
}


StringConcat* StringConcat::merge(StringConcat* other, Node* arg) {
  StringConcat* result = new StringConcat(_stringopts, _end);
  for (uint x = 0; x < _control.size(); x++) {
    Node* n = _control.at(x);
    if (n->is_Call()) {
      result->_control.push(n);
    }
  }
  for (uint x = 0; x < other->_control.size(); x++) {
    Node* n = other->_control.at(x);
    if (n->is_Call()) {
      result->_control.push(n);
    }
  }
  assert(result->_control.contains(other->_end), "what?");
  assert(result->_control.contains(_begin), "what?");
  for (int x = 0; x < num_arguments(); x++) {
    Node* argx = argument_uncast(x);
    if (argx == arg) {
      // replace the toString result with the all the arguments that
      // made up the other StringConcat
      for (int y = 0; y < other->num_arguments(); y++) {
        result->append(other->argument(y), other->mode(y));
      }
    } else {
      result->append(argx, mode(x));
    }
  }
  result->set_allocation(other->_begin);
  for (uint i = 0; i < _constructors.size(); i++) {
    result->add_constructor(_constructors.at(i));
  }
  for (uint i = 0; i < other->_constructors.size(); i++) {
    result->add_constructor(other->_constructors.at(i));
  }
  result->_multiple = true;
  return result;
}


void StringConcat::eliminate_call(CallNode* call) {
  Compile* C = _stringopts->C;
  CallProjections projs;
  call->extract_projections(&projs, false);
  if (projs.fallthrough_catchproj != NULL) {
    C->gvn_replace_by(projs.fallthrough_catchproj, call->in(TypeFunc::Control));
  }
  if (projs.fallthrough_memproj != NULL) {
    C->gvn_replace_by(projs.fallthrough_memproj, call->in(TypeFunc::Memory));
  }
  if (projs.catchall_memproj != NULL) {
    C->gvn_replace_by(projs.catchall_memproj, C->top());
  }
  if (projs.fallthrough_ioproj != NULL) {
    C->gvn_replace_by(projs.fallthrough_ioproj, call->in(TypeFunc::I_O));
  }
  if (projs.catchall_ioproj != NULL) {
    C->gvn_replace_by(projs.catchall_ioproj, C->top());
  }
  if (projs.catchall_catchproj != NULL) {
    // EA can't cope with the partially collapsed graph this
    // creates so put it on the worklist to be collapsed later.
    for (SimpleDUIterator i(projs.catchall_catchproj); i.has_next(); i.next()) {
      Node *use = i.get();
      int opc = use->Opcode();
      if (opc == Op_CreateEx || opc == Op_Region) {
        _stringopts->record_dead_node(use);
      }
    }
    C->gvn_replace_by(projs.catchall_catchproj, C->top());
  }
  if (projs.resproj != NULL) {
    C->gvn_replace_by(projs.resproj, C->top());
  }
  C->gvn_replace_by(call, C->top());
}

void StringConcat::eliminate_initialize(InitializeNode* init) {
  Compile* C = _stringopts->C;

  // Eliminate Initialize node.
  assert(init->outcnt() <= 2, "only a control and memory projection expected");
  assert(init->req() <= InitializeNode::RawStores, "no pending inits");
  Node *ctrl_proj = init->proj_out_or_null(TypeFunc::Control);
  if (ctrl_proj != NULL) {
    C->gvn_replace_by(ctrl_proj, init->in(TypeFunc::Control));
  }
  Node *mem_proj = init->proj_out_or_null(TypeFunc::Memory);
  if (mem_proj != NULL) {
    Node *mem = init->in(TypeFunc::Memory);
    C->gvn_replace_by(mem_proj, mem);
  }
  C->gvn_replace_by(init, C->top());
  init->disconnect_inputs(C);
}

Node_List PhaseStringOpts::collect_toString_calls() {
  Node_List string_calls;
  Node_List worklist;

  _visited.clear();

  // Prime the worklist
  for (uint i = 1; i < C->root()->len(); i++) {
    Node* n = C->root()->in(i);
    if (n != NULL && !_visited.test_set(n->_idx)) {
      worklist.push(n);
    }
  }

  while (worklist.size() > 0) {
    Node* ctrl = worklist.pop();
    if (StringConcat::is_SB_toString(ctrl)) {
      CallStaticJavaNode* csj = ctrl->as_CallStaticJava();
      string_calls.push(csj);
    }
    if (ctrl->in(0) != NULL && !_visited.test_set(ctrl->in(0)->_idx)) {
      worklist.push(ctrl->in(0));
    }
    if (ctrl->is_Region()) {
      for (uint i = 1; i < ctrl->len(); i++) {
        if (ctrl->in(i) != NULL && !_visited.test_set(ctrl->in(i)->_idx)) {
          worklist.push(ctrl->in(i));
        }
      }
    }
  }
  return string_calls;
}


StringConcat* PhaseStringOpts::build_candidate(CallStaticJavaNode* call) {
  ciMethod* m = call->method();
  ciSymbol* string_sig;
  ciSymbol* int_sig;
  ciSymbol* char_sig;
  if (m->holder() == C->env()->StringBuilder_klass()) {
    string_sig = ciSymbols::String_StringBuilder_signature();
    int_sig = ciSymbols::int_StringBuilder_signature();
    char_sig = ciSymbols::char_StringBuilder_signature();
  } else if (m->holder() == C->env()->StringBuffer_klass()) {
    string_sig = ciSymbols::String_StringBuffer_signature();
    int_sig = ciSymbols::int_StringBuffer_signature();
    char_sig = ciSymbols::char_StringBuffer_signature();
  } else {
    return NULL;
  }
#ifndef PRODUCT
  if (PrintOptimizeStringConcat) {
    tty->print("considering toString call in ");
    call->jvms()->dump_spec(tty); tty->cr();
  }
#endif

  StringConcat* sc = new StringConcat(this, call);

  AllocateNode* alloc = NULL;
  InitializeNode* init = NULL;

  // possible opportunity for StringBuilder fusion
  CallStaticJavaNode* cnode = call;
  while (cnode) {
    Node* recv = cnode->in(TypeFunc::Parms)->uncast();
    if (recv->is_Proj()) {
      recv = recv->in(0);
    }
    cnode = recv->isa_CallStaticJava();
    if (cnode == NULL) {
      alloc = recv->isa_Allocate();
      if (alloc == NULL) {
        break;
      }
      // Find the constructor call
      Node* result = alloc->result_cast();
      if (result == NULL || !result->is_CheckCastPP() || alloc->in(TypeFunc::Memory)->is_top()) {
        // strange looking allocation
#ifndef PRODUCT
        if (PrintOptimizeStringConcat) {
          tty->print("giving up because allocation looks strange ");
          alloc->jvms()->dump_spec(tty); tty->cr();
        }
#endif
        break;
      }
      Node* constructor = NULL;
      for (SimpleDUIterator i(result); i.has_next(); i.next()) {
        CallStaticJavaNode *use = i.get()->isa_CallStaticJava();
        if (use != NULL &&
            use->method() != NULL &&
            !use->method()->is_static() &&
            use->method()->name() == ciSymbols::object_initializer_name() &&
            use->method()->holder() == m->holder()) {
          // Matched the constructor.
          ciSymbol* sig = use->method()->signature()->as_symbol();
          if (sig == ciSymbols::void_method_signature() ||
              sig == ciSymbols::int_void_signature() ||
              sig == ciSymbols::string_void_signature()) {
            if (sig == ciSymbols::string_void_signature()) {
              // StringBuilder(String) so pick this up as the first argument
              assert(use->in(TypeFunc::Parms + 1) != NULL, "what?");
              const Type* type = _gvn->type(use->in(TypeFunc::Parms + 1));
              if (type == TypePtr::NULL_PTR) {
                // StringBuilder(null) throws exception.
#ifndef PRODUCT
                if (PrintOptimizeStringConcat) {
                  tty->print("giving up because StringBuilder(null) throws exception");
                  alloc->jvms()->dump_spec(tty); tty->cr();
                }
#endif
                return NULL;
              }
              // StringBuilder(str) argument needs null check.
              sc->push_string_null_check(use->in(TypeFunc::Parms + 1));
            }
            // The int variant takes an initial size for the backing
            // array so just treat it like the void version.
            constructor = use;
          } else {
#ifndef PRODUCT
            if (PrintOptimizeStringConcat) {
              tty->print("unexpected constructor signature: %s", sig->as_utf8());
            }
#endif
          }
          break;
        }
      }
      if (constructor == NULL) {
        // couldn't find constructor
#ifndef PRODUCT
        if (PrintOptimizeStringConcat) {
          tty->print("giving up because couldn't find constructor ");
          alloc->jvms()->dump_spec(tty); tty->cr();
        }
#endif
        break;
      }

      // Walked all the way back and found the constructor call so see
      // if this call converted into a direct string concatenation.
      sc->add_control(call);
      sc->add_control(constructor);
      sc->add_control(alloc);
      sc->set_allocation(alloc);
      sc->add_constructor(constructor);
      if (sc->validate_control_flow() && sc->validate_mem_flow()) {
        return sc;
      } else {
        return NULL;
      }
    } else if (cnode->method() == NULL) {
      break;
    } else if (!cnode->method()->is_static() &&
               cnode->method()->holder() == m->holder() &&
               cnode->method()->name() == ciSymbols::append_name() &&
               (cnode->method()->signature()->as_symbol() == string_sig ||
                cnode->method()->signature()->as_symbol() == char_sig ||
                cnode->method()->signature()->as_symbol() == int_sig)) {
      sc->add_control(cnode);
      Node* arg = cnode->in(TypeFunc::Parms + 1);
      if (arg == NULL || arg->is_top()) {
#ifndef PRODUCT
        if (PrintOptimizeStringConcat) {
          tty->print("giving up because the call is effectively dead");
          cnode->jvms()->dump_spec(tty); tty->cr();
        }
#endif
        break;
      }
      if (cnode->method()->signature()->as_symbol() == int_sig) {
        sc->push_int(arg);
      } else if (cnode->method()->signature()->as_symbol() == char_sig) {
        sc->push_char(arg);
      } else {
        if (arg->is_Proj() && arg->in(0)->is_CallStaticJava()) {
          CallStaticJavaNode* csj = arg->in(0)->as_CallStaticJava();
          if (csj->method() != NULL &&
              csj->method()->intrinsic_id() == vmIntrinsics::_Integer_toString &&
              arg->outcnt() == 1) {
            // _control is the list of StringBuilder calls nodes which
            // will be replaced by new String code after this optimization.
            // Integer::toString() call is not part of StringBuilder calls
            // chain. It could be eliminated only if its result is used
            // only by this SB calls chain.
            // Another limitation: it should be used only once because
            // it is unknown that it is used only by this SB calls chain
            // until all related SB calls nodes are collected.
            assert(arg->unique_out() == cnode, "sanity");
            sc->add_control(csj);
            sc->push_int(csj->in(TypeFunc::Parms));
            continue;
          }
        }
        sc->push_string(arg);
      }
      continue;
    } else {
      // some unhandled signature
#ifndef PRODUCT
      if (PrintOptimizeStringConcat) {
        tty->print("giving up because encountered unexpected signature ");
        cnode->tf()->dump(); tty->cr();
        cnode->in(TypeFunc::Parms + 1)->dump();
      }
#endif
      break;
    }
  }
  return NULL;
}


PhaseStringOpts::PhaseStringOpts(PhaseGVN* gvn, Unique_Node_List*):
  Phase(StringOpts),
  _gvn(gvn) {

  assert(OptimizeStringConcat, "shouldn't be here");

  size_table_field = C->env()->Integer_klass()->get_field_by_name(ciSymbol::make("sizeTable"),
                                                                  ciSymbols::int_array_signature(), true);
  if (size_table_field == NULL) {
    // Something wrong so give up.
    assert(false, "why can't we find Integer.sizeTable?");
    return;
  }

  // Collect the types needed to talk about the various slices of memory
  byte_adr_idx = C->get_alias_index(TypeAryPtr::BYTES);

  // For each locally allocated StringBuffer see if the usages can be
  // collapsed into a single String construction.

  // Run through the list of allocation looking for SB.toString to see
  // if it's possible to fuse the usage of the SB into a single String
  // construction.
  GrowableArray<StringConcat*> concats;
  Node_List toStrings = collect_toString_calls();
  while (toStrings.size() > 0) {
    StringConcat* sc = build_candidate(toStrings.pop()->as_CallStaticJava());
    if (sc != NULL) {
      concats.push(sc);
    }
  }

  // try to coalesce separate concats
 restart:
  for (int c = 0; c < concats.length(); c++) {
    StringConcat* sc = concats.at(c);
    for (int i = 0; i < sc->num_arguments(); i++) {
      Node* arg = sc->argument_uncast(i);
      if (arg->is_Proj() && StringConcat::is_SB_toString(arg->in(0))) {
        CallStaticJavaNode* csj = arg->in(0)->as_CallStaticJava();
        for (int o = 0; o < concats.length(); o++) {
          if (c == o) continue;
          StringConcat* other = concats.at(o);
          if (other->end() == csj) {
#ifndef PRODUCT
            if (PrintOptimizeStringConcat) {
              tty->print_cr("considering stacked concats");
            }
#endif

            StringConcat* merged = sc->merge(other, arg);
            if (merged->validate_control_flow() && merged->validate_mem_flow()) {
#ifndef PRODUCT
              if (PrintOptimizeStringConcat) {
                tty->print_cr("stacking would succeed");
              }
#endif
              if (c < o) {
                concats.remove_at(o);
                concats.at_put(c, merged);
              } else {
                concats.remove_at(c);
                concats.at_put(o, merged);
              }
              goto restart;
            } else {
#ifndef PRODUCT
              if (PrintOptimizeStringConcat) {
                tty->print_cr("stacking would fail");
              }
#endif
            }
          }
        }
      }
    }
  }


  for (int c = 0; c < concats.length(); c++) {
    StringConcat* sc = concats.at(c);
    replace_string_concat(sc);
  }

  remove_dead_nodes();
}

void PhaseStringOpts::record_dead_node(Node* dead) {
  dead_worklist.push(dead);
}

void PhaseStringOpts::remove_dead_nodes() {
  // Delete any dead nodes to make things clean enough that escape
  // analysis doesn't get unhappy.
  while (dead_worklist.size() > 0) {
    Node* use = dead_worklist.pop();
    int opc = use->Opcode();
    switch (opc) {
      case Op_Region: {
        uint i = 1;
        for (i = 1; i < use->req(); i++) {
          if (use->in(i) != C->top()) {
            break;
          }
        }
        if (i >= use->req()) {
          for (SimpleDUIterator i(use); i.has_next(); i.next()) {
            Node* m = i.get();
            if (m->is_Phi()) {
              dead_worklist.push(m);
            }
          }
          C->gvn_replace_by(use, C->top());
        }
        break;
      }
      case Op_AddP:
      case Op_CreateEx: {
        // Recurisvely clean up references to CreateEx so EA doesn't
        // get unhappy about the partially collapsed graph.
        for (SimpleDUIterator i(use); i.has_next(); i.next()) {
          Node* m = i.get();
          if (m->is_AddP()) {
            dead_worklist.push(m);
          }
        }
        C->gvn_replace_by(use, C->top());
        break;
      }
      case Op_Phi:
        if (use->in(0) == C->top()) {
          C->gvn_replace_by(use, C->top());
        }
        break;
    }
  }
}


bool StringConcat::validate_mem_flow() {
  Compile* C = _stringopts->C;

  for (uint i = 0; i < _control.size(); i++) {
#ifndef PRODUCT
    Node_List path;
#endif
    Node* curr = _control.at(i);
    if (curr->is_Call() && curr != _begin) { // For all calls except the first allocation
      // Now here's the main invariant in our case:
      // For memory between the constructor, and appends, and toString we should only see bottom memory,
      // produced by the previous call we know about.
      if (!_constructors.contains(curr)) {
        NOT_PRODUCT(path.push(curr);)
        Node* mem = curr->in(TypeFunc::Memory);
        assert(mem != NULL, "calls should have memory edge");
        assert(!mem->is_Phi(), "should be handled by control flow validation");
        NOT_PRODUCT(path.push(mem);)
        while (mem->is_MergeMem()) {
          for (uint i = 1; i < mem->req(); i++) {
            if (i != Compile::AliasIdxBot && mem->in(i) != C->top()) {
#ifndef PRODUCT
              if (PrintOptimizeStringConcat) {
                tty->print("fusion has incorrect memory flow (side effects) for ");
                _begin->jvms()->dump_spec(tty); tty->cr();
                path.dump();
              }
#endif
              return false;
            }
          }
          // skip through a potential MergeMem chain, linked through Bot
          mem = mem->in(Compile::AliasIdxBot);
          NOT_PRODUCT(path.push(mem);)
        }
        // now let it fall through, and see if we have a projection
        if (mem->is_Proj()) {
          // Should point to a previous known call
          Node *prev = mem->in(0);
          NOT_PRODUCT(path.push(prev);)
          if (!prev->is_Call() || !_control.contains(prev)) {
#ifndef PRODUCT
            if (PrintOptimizeStringConcat) {
              tty->print("fusion has incorrect memory flow (unknown call) for ");
              _begin->jvms()->dump_spec(tty); tty->cr();
              path.dump();
            }
#endif
            return false;
          }
        } else {
          assert(mem->is_Store() || mem->is_LoadStore(), "unexpected node type: %s", mem->Name());
#ifndef PRODUCT
          if (PrintOptimizeStringConcat) {
            tty->print("fusion has incorrect memory flow (unexpected source) for ");
            _begin->jvms()->dump_spec(tty); tty->cr();
            path.dump();
          }
#endif
          return false;
        }
      } else {
        // For memory that feeds into constructors it's more complicated.
        // However the advantage is that any side effect that happens between the Allocate/Initialize and
        // the constructor will have to be control-dependent on Initialize.
        // So we actually don't have to do anything, since it's going to be caught by the control flow
        // analysis.
#ifdef ASSERT
        // Do a quick verification of the control pattern between the constructor and the initialize node
        assert(curr->is_Call(), "constructor should be a call");
        // Go up the control starting from the constructor call
        Node* ctrl = curr->in(0);
        IfNode* iff = NULL;
        RegionNode* copy = NULL;

        while (true) {
          // skip known check patterns
          if (ctrl->is_Region()) {
            if (ctrl->as_Region()->is_copy()) {
              copy = ctrl->as_Region();
              ctrl = copy->is_copy();
            } else { // a cast
              assert(ctrl->req() == 3 &&
                     ctrl->in(1) != NULL && ctrl->in(1)->is_Proj() &&
                     ctrl->in(2) != NULL && ctrl->in(2)->is_Proj() &&
                     ctrl->in(1)->in(0) == ctrl->in(2)->in(0) &&
                     ctrl->in(1)->in(0) != NULL && ctrl->in(1)->in(0)->is_If(),
                     "must be a simple diamond");
              Node* true_proj = ctrl->in(1)->is_IfTrue() ? ctrl->in(1) : ctrl->in(2);
              for (SimpleDUIterator i(true_proj); i.has_next(); i.next()) {
                Node* use = i.get();
                assert(use == ctrl || use->is_ConstraintCast(),
                       "unexpected user: %s", use->Name());
              }

              iff = ctrl->in(1)->in(0)->as_If();
              ctrl = iff->in(0);
            }
          } else if (ctrl->is_IfTrue()) { // null checks, class checks
            iff = ctrl->in(0)->as_If();
            // Verify that the other arm is an uncommon trap
            Node* otherproj = iff->proj_out(1 - ctrl->as_Proj()->_con);
            CallStaticJavaNode* call = otherproj->unique_out()->isa_CallStaticJava();
            assert(strcmp(call->_name, "uncommon_trap") == 0, "must be uncommon trap");
            ctrl = iff->in(0);
          } else {
            break;
          }
        }

        assert(ctrl->is_Proj(), "must be a projection");
        assert(ctrl->in(0)->is_Initialize(), "should be initialize");
        for (SimpleDUIterator i(ctrl); i.has_next(); i.next()) {
          Node* use = i.get();
          assert(use == copy || use == iff || use == curr || use->is_CheckCastPP() || use->is_Load(),
                 "unexpected user: %s", use->Name());
        }
#endif // ASSERT
      }
    }
  }

#ifndef PRODUCT
  if (PrintOptimizeStringConcat) {
    tty->print("fusion has correct memory flow for ");
    _begin->jvms()->dump_spec(tty); tty->cr();
    tty->cr();
  }
#endif
  return true;
}

bool StringConcat::validate_control_flow() {
  // We found all the calls and arguments now lets see if it's
  // safe to transform the graph as we would expect.

  // Check to see if this resulted in too many uncommon traps previously
  if (Compile::current()->too_many_traps(_begin->jvms()->method(), _begin->jvms()->bci(),
                        Deoptimization::Reason_intrinsic)) {
    return false;
  }

  // Walk backwards over the control flow from toString to the
  // allocation and make sure all the control flow is ok.  This
  // means it's either going to be eliminated once the calls are
  // removed or it can safely be transformed into an uncommon
  // trap.

  int null_check_count = 0;
  Unique_Node_List ctrl_path;

  assert(_control.contains(_begin), "missing");
  assert(_control.contains(_end), "missing");

  // Collect the nodes that we know about and will eliminate into ctrl_path
  for (uint i = 0; i < _control.size(); i++) {
    // Push the call and it's control projection
    Node* n = _control.at(i);
    if (n->is_Allocate()) {
      AllocateNode* an = n->as_Allocate();
      InitializeNode* init = an->initialization();
      ctrl_path.push(init);
      ctrl_path.push(init->as_Multi()->proj_out(0));
    }
    if (n->is_Call()) {
      CallNode* cn = n->as_Call();
      ctrl_path.push(cn);
      ctrl_path.push(cn->proj_out(0));
      ctrl_path.push(cn->proj_out(0)->unique_out());
      Node* catchproj = cn->proj_out(0)->unique_out()->as_Catch()->proj_out_or_null(0);
      if (catchproj != NULL) {
        ctrl_path.push(catchproj);
      }
    } else {
      ShouldNotReachHere();
    }
  }

  // Skip backwards through the control checking for unexpected control flow
  Node* ptr = _end;
  bool fail = false;
  while (ptr != _begin) {
    if (ptr->is_Call() && ctrl_path.member(ptr)) {
      ptr = ptr->in(0);
    } else if (ptr->is_CatchProj() && ctrl_path.member(ptr)) {
      ptr = ptr->in(0)->in(0)->in(0);
      assert(ctrl_path.member(ptr), "should be a known piece of control");
    } else if (ptr->is_IfTrue()) {
      IfNode* iff = ptr->in(0)->as_If();
      BoolNode* b = iff->in(1)->isa_Bool();

      if (b == NULL) {
#ifndef PRODUCT
        if (PrintOptimizeStringConcat) {
          tty->print_cr("unexpected input to IfNode");
          iff->in(1)->dump();
          tty->cr();
        }
#endif
        fail = true;
        break;
      }

      Node* cmp = b->in(1);
      Node* v1 = cmp->in(1);
      Node* v2 = cmp->in(2);
      Node* otherproj = iff->proj_out(1 - ptr->as_Proj()->_con);

      // Null check of the return of append which can simply be eliminated
      if (b->_test._test == BoolTest::ne &&
          v2->bottom_type() == TypePtr::NULL_PTR &&
          v1->is_Proj() && ctrl_path.member(v1->in(0))) {
        // NULL check of the return value of the append
        null_check_count++;
        if (otherproj->outcnt() == 1) {
          CallStaticJavaNode* call = otherproj->unique_out()->isa_CallStaticJava();
          if (call != NULL && call->_name != NULL && strcmp(call->_name, "uncommon_trap") == 0) {
            ctrl_path.push(call);
          }
        }
        _control.push(ptr);
        ptr = ptr->in(0)->in(0);
        continue;
      }

      // A test which leads to an uncommon trap which should be safe.
      // Later this trap will be converted into a trap that restarts
      // at the beginning.
      if (otherproj->outcnt() == 1) {
        CallStaticJavaNode* call = otherproj->unique_out()->isa_CallStaticJava();
        if (call != NULL && call->_name != NULL && strcmp(call->_name, "uncommon_trap") == 0) {
          // control flow leads to uct so should be ok
          _uncommon_traps.push(call);
          ctrl_path.push(call);
          ptr = ptr->in(0)->in(0);
          continue;
        }
      }

#ifndef PRODUCT
      // Some unexpected control flow we don't know how to handle.
      if (PrintOptimizeStringConcat) {
        tty->print_cr("failing with unknown test");
        b->dump();
        cmp->dump();
        v1->dump();
        v2->dump();
        tty->cr();
      }
#endif
      fail = true;
      break;
    } else if (ptr->is_Proj() && ptr->in(0)->is_Initialize()) {
      ptr = ptr->in(0)->in(0);
    } else if (ptr->is_Region()) {
      Node* copy = ptr->as_Region()->is_copy();
      if (copy != NULL) {
        ptr = copy;
        continue;
      }
      if (ptr->req() == 3 &&
          ptr->in(1) != NULL && ptr->in(1)->is_Proj() &&
          ptr->in(2) != NULL && ptr->in(2)->is_Proj() &&
          ptr->in(1)->in(0) == ptr->in(2)->in(0) &&
          ptr->in(1)->in(0) != NULL && ptr->in(1)->in(0)->is_If()) {
        // Simple diamond.
        // XXX should check for possibly merging stores.  simple data merges are ok.
        // The IGVN will make this simple diamond go away when it
        // transforms the Region. Make sure it sees it.
        Compile::current()->record_for_igvn(ptr);
        ptr = ptr->in(1)->in(0)->in(0);
        continue;
      }
#ifndef PRODUCT
      if (PrintOptimizeStringConcat) {
        tty->print_cr("fusion would fail for region");
        _begin->dump();
        ptr->dump(2);
      }
#endif
      fail = true;
      break;
    } else {
      // other unknown control
      if (!fail) {
#ifndef PRODUCT
        if (PrintOptimizeStringConcat) {
          tty->print_cr("fusion would fail for");
          _begin->dump();
        }
#endif
        fail = true;
      }
#ifndef PRODUCT
      if (PrintOptimizeStringConcat) {
        ptr->dump();
      }
#endif
      ptr = ptr->in(0);
    }
  }
#ifndef PRODUCT
  if (PrintOptimizeStringConcat && fail) {
    tty->cr();
  }
#endif
  if (fail) return !fail;

  // Validate that all these results produced are contained within
  // this cluster of objects.  First collect all the results produced
  // by calls in the region.
  _stringopts->_visited.clear();
  Node_List worklist;
  Node* final_result = _end->proj_out_or_null(TypeFunc::Parms);
  for (uint i = 0; i < _control.size(); i++) {
    CallNode* cnode = _control.at(i)->isa_Call();
    if (cnode != NULL) {
      _stringopts->_visited.test_set(cnode->_idx);
    }
    Node* result = cnode != NULL ? cnode->proj_out_or_null(TypeFunc::Parms) : NULL;
    if (result != NULL && result != final_result) {
      worklist.push(result);
    }
  }

  Node* last_result = NULL;
  while (worklist.size() > 0) {
    Node* result = worklist.pop();
    if (_stringopts->_visited.test_set(result->_idx))
      continue;
    for (SimpleDUIterator i(result); i.has_next(); i.next()) {
      Node *use = i.get();
      if (ctrl_path.member(use)) {
        // already checked this
        continue;
      }
      int opc = use->Opcode();
      if (opc == Op_CmpP || opc == Op_Node) {
        ctrl_path.push(use);
        continue;
      }
      if (opc == Op_CastPP || opc == Op_CheckCastPP) {
        for (SimpleDUIterator j(use); j.has_next(); j.next()) {
          worklist.push(j.get());
        }
        worklist.push(use->in(1));
        ctrl_path.push(use);
        continue;
      }
#ifndef PRODUCT
      if (PrintOptimizeStringConcat) {
        if (result != last_result) {
          last_result = result;
          tty->print_cr("extra uses for result:");
          last_result->dump();
        }
        use->dump();
      }
#endif
      fail = true;
      break;
    }
  }

#ifndef PRODUCT
  if (PrintOptimizeStringConcat && !fail) {
    ttyLocker ttyl;
    tty->cr();
    tty->print("fusion has correct control flow (%d %d) for ", null_check_count, _uncommon_traps.size());
    _begin->jvms()->dump_spec(tty); tty->cr();
    for (int i = 0; i < num_arguments(); i++) {
      argument(i)->dump();
    }
    _control.dump();
    tty->cr();
  }
#endif

  return !fail;
}

Node* PhaseStringOpts::fetch_static_field(GraphKit& kit, ciField* field) {
  const TypeInstPtr* mirror_type = TypeInstPtr::make(field->holder()->java_mirror());
  Node* klass_node = __ makecon(mirror_type);
  BasicType bt = field->layout_type();
  ciType* field_klass = field->type();

  const Type *type;
  if( bt == T_OBJECT ) {
    if (!field->type()->is_loaded()) {
      type = TypeInstPtr::BOTTOM;
    } else if (field->is_static_constant()) {
      // This can happen if the constant oop is non-perm.
      ciObject* con = field->constant_value().as_object();
      // Do not "join" in the previous type; it doesn't add value,
      // and may yield a vacuous result if the field is of interface type.
      type = TypeOopPtr::make_from_constant(con, true)->isa_oopptr();
      assert(type != NULL, "field singleton type must be consistent");
      return __ makecon(type);
    } else {
      type = TypeOopPtr::make_from_klass(field_klass->as_klass());
    }
  } else {
    type = Type::get_const_basic_type(bt);
  }

  return kit.make_load(NULL, kit.basic_plus_adr(klass_node, field->offset_in_bytes()),
                       type, T_OBJECT,
                       C->get_alias_index(mirror_type->add_offset(field->offset_in_bytes())),
                       MemNode::unordered);
}

Node* PhaseStringOpts::int_stringSize(GraphKit& kit, Node* arg) {
  if (arg->is_Con()) {
    // Constant integer. Compute constant length using Integer.sizeTable
    int arg_val = arg->get_int();
    int count = 1;
    if (arg_val < 0) {
      // Special case for min_jint - it can't be negated.
      if (arg_val == min_jint) {
        return __ intcon(11);
      }

      arg_val = -arg_val;
      count++;
    }

    ciArray* size_table = (ciArray*)size_table_field->constant_value().as_object();
    for (int i = 0; i < size_table->length(); i++) {
      if (arg_val <= size_table->element_value(i).as_int()) {
        count += i;
        break;
      }
    }
    return __ intcon(count);
  }

  RegionNode *final_merge = new RegionNode(3);
  kit.gvn().set_type(final_merge, Type::CONTROL);
  Node* final_size = new PhiNode(final_merge, TypeInt::INT);
  kit.gvn().set_type(final_size, TypeInt::INT);

  IfNode* iff = kit.create_and_map_if(kit.control(),
                                      __ Bool(__ CmpI(arg, __ intcon(0x80000000)), BoolTest::ne),
                                      PROB_FAIR, COUNT_UNKNOWN);
  Node* is_min = __ IfFalse(iff);
  final_merge->init_req(1, is_min);
  final_size->init_req(1, __ intcon(11));

  kit.set_control(__ IfTrue(iff));
  if (kit.stopped()) {
    final_merge->init_req(2, C->top());
    final_size->init_req(2, C->top());
  } else {

    // int size = (i < 0) ? stringSize(-i) + 1 : stringSize(i);
    RegionNode *r = new RegionNode(3);
    kit.gvn().set_type(r, Type::CONTROL);
    Node *phi = new PhiNode(r, TypeInt::INT);
    kit.gvn().set_type(phi, TypeInt::INT);
    Node *size = new PhiNode(r, TypeInt::INT);
    kit.gvn().set_type(size, TypeInt::INT);
    Node* chk = __ CmpI(arg, __ intcon(0));
    Node* p = __ Bool(chk, BoolTest::lt);
    IfNode* iff = kit.create_and_map_if(kit.control(), p, PROB_FAIR, COUNT_UNKNOWN);
    Node* lessthan = __ IfTrue(iff);
    Node* greaterequal = __ IfFalse(iff);
    r->init_req(1, lessthan);
    phi->init_req(1, __ SubI(__ intcon(0), arg));
    size->init_req(1, __ intcon(1));
    r->init_req(2, greaterequal);
    phi->init_req(2, arg);
    size->init_req(2, __ intcon(0));
    kit.set_control(r);
    C->record_for_igvn(r);
    C->record_for_igvn(phi);
    C->record_for_igvn(size);

    // for (int i=0; ; i++)
    //   if (x <= sizeTable[i])
    //     return i+1;

    // Add loop predicate first.
    kit.add_empty_predicates();
    C->set_has_loops(true);

    RegionNode *loop = new RegionNode(3);
    loop->init_req(1, kit.control());
    kit.gvn().set_type(loop, Type::CONTROL);

    Node *index = new PhiNode(loop, TypeInt::INT);
    index->init_req(1, __ intcon(0));
    kit.gvn().set_type(index, TypeInt::INT);
    kit.set_control(loop);
    Node* sizeTable = fetch_static_field(kit, size_table_field);

    Node* value = kit.load_array_element(NULL, sizeTable, index, TypeAryPtr::INTS);
    C->record_for_igvn(value);
    Node* limit = __ CmpI(phi, value);
    Node* limitb = __ Bool(limit, BoolTest::le);
    IfNode* iff2 = kit.create_and_map_if(kit.control(), limitb, PROB_MIN, COUNT_UNKNOWN);
    Node* lessEqual = __ IfTrue(iff2);
    Node* greater = __ IfFalse(iff2);

    loop->init_req(2, greater);
    index->init_req(2, __ AddI(index, __ intcon(1)));

    kit.set_control(lessEqual);
    C->record_for_igvn(loop);
    C->record_for_igvn(index);

    final_merge->init_req(2, kit.control());
    final_size->init_req(2, __ AddI(__ AddI(index, size), __ intcon(1)));
  }

  kit.set_control(final_merge);
  C->record_for_igvn(final_merge);
  C->record_for_igvn(final_size);

  return final_size;
}

// Simplified version of Integer.getChars
void PhaseStringOpts::getChars(GraphKit& kit, Node* arg, Node* dst_array, BasicType bt, Node* end, Node* final_merge, Node* final_mem, int merge_index) {
  // if (i < 0) {
  //     sign = '-';
  //     i = -i;
  // }
  IfNode* iff = kit.create_and_map_if(kit.control(), __ Bool(__ CmpI(arg, __ intcon(0)), BoolTest::lt),
                                      PROB_FAIR, COUNT_UNKNOWN);

  RegionNode* merge = new RegionNode(3);
  kit.gvn().set_type(merge, Type::CONTROL);
  Node* i = new PhiNode(merge, TypeInt::INT);
  kit.gvn().set_type(i, TypeInt::INT);
  Node* sign = new PhiNode(merge, TypeInt::INT);
  kit.gvn().set_type(sign, TypeInt::INT);

  merge->init_req(1, __ IfTrue(iff));
  i->init_req(1, __ SubI(__ intcon(0), arg));
  sign->init_req(1, __ intcon('-'));
  merge->init_req(2, __ IfFalse(iff));
  i->init_req(2, arg);
  sign->init_req(2, __ intcon(0));

  kit.set_control(merge);

  C->record_for_igvn(merge);
  C->record_for_igvn(i);
  C->record_for_igvn(sign);

  // for (;;) {
  //     q = i / 10;
  //     r = i - ((q << 3) + (q << 1));  // r = i-(q*10) ...
  //     buf [--charPos] = digits [r];
  //     i = q;
  //     if (i == 0) break;
  // }

  // Add loop predicate first.
  kit.add_empty_predicates();

  C->set_has_loops(true);
  RegionNode* head = new RegionNode(3);
  head->init_req(1, kit.control());

  kit.gvn().set_type(head, Type::CONTROL);
  Node* i_phi = new PhiNode(head, TypeInt::INT);
  i_phi->init_req(1, i);
  kit.gvn().set_type(i_phi, TypeInt::INT);
  Node* charPos = new PhiNode(head, TypeInt::INT);
  charPos->init_req(1, end);
  kit.gvn().set_type(charPos, TypeInt::INT);
  Node* mem = PhiNode::make(head, kit.memory(byte_adr_idx), Type::MEMORY, TypeAryPtr::BYTES);
  kit.gvn().set_type(mem, Type::MEMORY);

  kit.set_control(head);
  kit.set_memory(mem, byte_adr_idx);

  Node* q = __ DivI(kit.null(), i_phi, __ intcon(10));
  Node* r = __ SubI(i_phi, __ AddI(__ LShiftI(q, __ intcon(3)),
                                   __ LShiftI(q, __ intcon(1))));
  Node* index = __ SubI(charPos, __ intcon((bt == T_BYTE) ? 1 : 2));
  Node* ch = __ AddI(r, __ intcon('0'));
  Node* st = __ store_to_memory(kit.control(), kit.array_element_address(dst_array, index, T_BYTE),
                                ch, bt, byte_adr_idx, MemNode::unordered, false /* require_atomic_access */,
                                false /* unaligned */, (bt != T_BYTE) /* mismatched */);

  iff = kit.create_and_map_if(head, __ Bool(__ CmpI(q, __ intcon(0)), BoolTest::ne),
                              PROB_FAIR, COUNT_UNKNOWN);
  Node* ne = __ IfTrue(iff);
  Node* eq = __ IfFalse(iff);

  head->init_req(2, ne);
  mem->init_req(2, st);

  i_phi->init_req(2, q);
  charPos->init_req(2, index);
  charPos = index;

  kit.set_control(eq);
  kit.set_memory(st, byte_adr_idx);

  C->record_for_igvn(head);
  C->record_for_igvn(mem);
  C->record_for_igvn(i_phi);
  C->record_for_igvn(charPos);

  // if (sign != 0) {
  //     buf [--charPos] = sign;
  // }
  iff = kit.create_and_map_if(kit.control(), __ Bool(__ CmpI(sign, __ intcon(0)), BoolTest::ne),
                              PROB_FAIR, COUNT_UNKNOWN);

  final_merge->init_req(merge_index + 2, __ IfFalse(iff));
  final_mem->init_req(merge_index + 2, kit.memory(byte_adr_idx));

  kit.set_control(__ IfTrue(iff));
  if (kit.stopped()) {
    final_merge->init_req(merge_index + 1, C->top());
    final_mem->init_req(merge_index + 1, C->top());
  } else {
    Node* index = __ SubI(charPos, __ intcon((bt == T_BYTE) ? 1 : 2));
    st = __ store_to_memory(kit.control(), kit.array_element_address(dst_array, index, T_BYTE),
                            sign, bt, byte_adr_idx, MemNode::unordered, false /* require_atomic_access */,
                            false /* unaligned */, (bt != T_BYTE) /* mismatched */);

    final_merge->init_req(merge_index + 1, kit.control());
    final_mem->init_req(merge_index + 1, st);
  }
}

// Copy the characters representing arg into dst_array starting at start
Node* PhaseStringOpts::int_getChars(GraphKit& kit, Node* arg, Node* dst_array, Node* dst_coder, Node* start, Node* size) {
  bool dcon = dst_coder->is_Con();
  bool dbyte = dcon ? (dst_coder->get_int() == java_lang_String::CODER_LATIN1) : false;
  Node* end = __ AddI(start, __ LShiftI(size, dst_coder));

  // The final_merge node has 4 entries in case the encoding is known:
  // (0) Control, (1) result w/ sign, (2) result w/o sign, (3) result for Integer.min_value
  // or 6 entries in case the encoding is not known:
  // (0) Control, (1) Latin1 w/ sign, (2) Latin1 w/o sign, (3) min_value, (4) UTF16 w/ sign, (5) UTF16 w/o sign
  RegionNode* final_merge = new RegionNode(dcon ? 4 : 6);
  kit.gvn().set_type(final_merge, Type::CONTROL);

  Node* final_mem = PhiNode::make(final_merge, kit.memory(byte_adr_idx), Type::MEMORY, TypeAryPtr::BYTES);
  kit.gvn().set_type(final_mem, Type::MEMORY);

  // need to handle arg == Integer.MIN_VALUE specially because negating doesn't make it positive
  IfNode* iff = kit.create_and_map_if(kit.control(), __ Bool(__ CmpI(arg, __ intcon(0x80000000)), BoolTest::ne),
                                      PROB_FAIR, COUNT_UNKNOWN);

  Node* old_mem = kit.memory(byte_adr_idx);

  kit.set_control(__ IfFalse(iff));
  if (kit.stopped()) {
    // Statically not equal to MIN_VALUE so this path is dead
    final_merge->init_req(3, kit.control());
  } else {
    copy_string(kit, __ makecon(TypeInstPtr::make(C->env()->the_min_jint_string())),
                dst_array, dst_coder, start);
    final_merge->init_req(3, kit.control());
    final_mem->init_req(3, kit.memory(byte_adr_idx));
  }

  kit.set_control(__ IfTrue(iff));
  kit.set_memory(old_mem, byte_adr_idx);

  if (!dcon) {
    // Check encoding of destination
    iff = kit.create_and_map_if(kit.control(), __ Bool(__ CmpI(dst_coder, __ intcon(0)), BoolTest::eq),
                                PROB_FAIR, COUNT_UNKNOWN);
    old_mem = kit.memory(byte_adr_idx);
  }
  if (!dcon || dbyte) {
    // Destination is Latin1,
    if (!dcon) {
      kit.set_control(__ IfTrue(iff));
    }
    getChars(kit, arg, dst_array, T_BYTE, end, final_merge, final_mem);
  }
  if (!dcon || !dbyte) {
    // Destination is UTF16
    int merge_index = 0;
    if (!dcon) {
      kit.set_control(__ IfFalse(iff));
      kit.set_memory(old_mem, byte_adr_idx);
      merge_index = 3; // Account for Latin1 case
    }
    getChars(kit, arg, dst_array, T_CHAR, end, final_merge, final_mem, merge_index);
  }

  // Final merge point for Latin1 and UTF16 case
  kit.set_control(final_merge);
  kit.set_memory(final_mem, byte_adr_idx);

  C->record_for_igvn(final_merge);
  C->record_for_igvn(final_mem);
  return end;
}

// Copy 'count' bytes/chars from src_array to dst_array starting at index start
void PhaseStringOpts::arraycopy(GraphKit& kit, IdealKit& ideal, Node* src_array, Node* dst_array, BasicType elembt, Node* start, Node* count) {
  assert(elembt == T_BYTE || elembt == T_CHAR, "Invalid type for arraycopy");

  if (elembt == T_CHAR) {
    // Get number of chars
    count = __ RShiftI(count, __ intcon(1));
  }

  Node* extra = NULL;
#ifdef _LP64
  count = __ ConvI2L(count);
  extra = C->top();
#endif

  Node* src_ptr = __ array_element_address(src_array, __ intcon(0), T_BYTE);
  Node* dst_ptr = __ array_element_address(dst_array, start, T_BYTE);
  // Check if destination address is aligned to HeapWordSize
  const TypeInt* tdst = __ gvn().type(start)->is_int();
  bool aligned = tdst->is_con() && ((tdst->get_con() * type2aelembytes(T_BYTE)) % HeapWordSize == 0);
  // Figure out which arraycopy runtime method to call (disjoint, uninitialized).
  const char* copyfunc_name = "arraycopy";
  address     copyfunc_addr = StubRoutines::select_arraycopy_function(elembt, aligned, true, copyfunc_name, true);
  ideal.make_leaf_call_no_fp(OptoRuntime::fast_arraycopy_Type(), copyfunc_addr, copyfunc_name,
                             TypeAryPtr::BYTES, src_ptr, dst_ptr, count, extra);
}

#undef __
#define __ ideal.

// Copy contents of a Latin1 encoded string from src_array to dst_array
void PhaseStringOpts::copy_latin1_string(GraphKit& kit, IdealKit& ideal, Node* src_array, IdealVariable& count,
                                         Node* dst_array, Node* dst_coder, Node* start) {
  bool dcon = dst_coder->is_Con();
  bool dbyte = dcon ? (dst_coder->get_int() == java_lang_String::CODER_LATIN1) : false;

  if (!dcon) {
    __ if_then(dst_coder, BoolTest::eq, __ ConI(java_lang_String::CODER_LATIN1));
  }
  if (!dcon || dbyte) {
    // Destination is Latin1. Simply emit a byte arraycopy.
    arraycopy(kit, ideal, src_array, dst_array, T_BYTE, start, __ value(count));
  }
  if (!dcon) {
    __ else_();
  }
  if (!dcon || !dbyte) {
    // Destination is UTF16. Inflate src_array into dst_array.
    kit.sync_kit(ideal);
    if (Matcher::match_rule_supported(Op_StrInflatedCopy)) {
      // Use fast intrinsic
      Node* src = kit.array_element_address(src_array, kit.intcon(0), T_BYTE);
      Node* dst = kit.array_element_address(dst_array, start, T_BYTE);
      kit.inflate_string(src, dst, TypeAryPtr::BYTES, __ value(count));
    } else {
      // No intrinsic available, use slow method
      kit.inflate_string_slow(src_array, dst_array, start, __ value(count));
    }
    ideal.sync_kit(&kit);
    // Multiply count by two since we now need two bytes per char
    __ set(count, __ LShiftI(__ value(count), __ ConI(1)));
  }
  if (!dcon) {
    __ end_if();
  }
}

// Read two bytes from index and index+1 and convert them to a char
static jchar readChar(ciTypeArray* array, int index) {
  int shift_high, shift_low;
#ifdef VM_LITTLE_ENDIAN
    shift_high = 0;
    shift_low = 8;
#else
    shift_high = 8;
    shift_low = 0;
#endif

  jchar b1 = ((jchar) array->byte_at(index)) & 0xff;
  jchar b2 = ((jchar) array->byte_at(index+1)) & 0xff;
  return (b1 << shift_high) | (b2 << shift_low);
}

// Copy contents of constant src_array to dst_array by emitting individual stores
void PhaseStringOpts::copy_constant_string(GraphKit& kit, IdealKit& ideal, ciTypeArray* src_array, IdealVariable& count,
                                           bool src_is_byte, Node* dst_array, Node* dst_coder, Node* start) {
  bool dcon = dst_coder->is_Con();
  bool dbyte = dcon ? (dst_coder->get_int() == java_lang_String::CODER_LATIN1) : false;
  int length = src_array->length();

  if (!dcon) {
    __ if_then(dst_coder, BoolTest::eq, __ ConI(java_lang_String::CODER_LATIN1));
  }
  if (!dcon || dbyte) {
    // Destination is Latin1. Copy each byte of src_array into dst_array.
    Node* index = start;
    for (int i = 0; i < length; i++) {
      Node* adr = kit.array_element_address(dst_array, index, T_BYTE);
      Node* val = __ ConI(src_array->byte_at(i));
      __ store(__ ctrl(), adr, val, T_BYTE, byte_adr_idx, MemNode::unordered);
      index = __ AddI(index, __ ConI(1));
    }
  }
  if (!dcon) {
    __ else_();
  }
  if (!dcon || !dbyte) {
    // Destination is UTF16. Copy each char of src_array into dst_array.
    Node* index = start;
    for (int i = 0; i < length; i++) {
      Node* adr = kit.array_element_address(dst_array, index, T_BYTE);
      jchar val;
      if (src_is_byte) {
        val = src_array->byte_at(i) & 0xff;
      } else {
        val = readChar(src_array, i++);
      }
      __ store(__ ctrl(), adr, __ ConI(val), T_CHAR, byte_adr_idx, MemNode::unordered, false /* require_atomic_access */,
               true /* mismatched */);
      index = __ AddI(index, __ ConI(2));
    }
    if (src_is_byte) {
      // Multiply count by two since we now need two bytes per char
      __ set(count, __ ConI(2 * length));
    }
  }
  if (!dcon) {
    __ end_if();
  }
}

// Compress copy contents of the byte/char String str into dst_array starting at index start.
Node* PhaseStringOpts::copy_string(GraphKit& kit, Node* str, Node* dst_array, Node* dst_coder, Node* start) {
  Node* src_array = kit.load_String_value(str, true);

  IdealKit ideal(&kit, true, true);
  IdealVariable count(ideal); __ declarations_done();

  if (str->is_Con()) {
    // Constant source string
    ciTypeArray* src_array_type = get_constant_value(kit, str);

    // Check encoding of constant string
    bool src_is_byte = (get_constant_coder(kit, str) == java_lang_String::CODER_LATIN1);

    // For small constant strings just emit individual stores.
    // A length of 6 seems like a good space/speed tradeof.
    __ set(count, __ ConI(src_array_type->length()));
    int src_len = src_array_type->length() / (src_is_byte ? 1 : 2);
    if (src_len < unroll_string_copy_length) {
      // Small constant string
      copy_constant_string(kit, ideal, src_array_type, count, src_is_byte, dst_array, dst_coder, start);
    } else if (src_is_byte) {
      // Source is Latin1
      copy_latin1_string(kit, ideal, src_array, count, dst_array, dst_coder, start);
    } else {
      // Source is UTF16 (destination too). Simply emit a char arraycopy.
      arraycopy(kit, ideal, src_array, dst_array, T_CHAR, start, __ value(count));
    }
  } else {
    Node* size = kit.load_array_length(src_array);
    __ set(count, size);
    // Non-constant source string
    if (CompactStrings) {
      // Emit runtime check for coder
      Node* coder = kit.load_String_coder(str, true);
      __ if_then(coder, BoolTest::eq, __ ConI(java_lang_String::CODER_LATIN1)); {
        // Source is Latin1
        copy_latin1_string(kit, ideal, src_array, count, dst_array, dst_coder, start);
      } __ else_();
    }
    // Source is UTF16 (destination too). Simply emit a char arraycopy.
    arraycopy(kit, ideal, src_array, dst_array, T_CHAR, start, __ value(count));

    if (CompactStrings) {
      __ end_if();
    }
  }

  // Finally sync IdealKit and GraphKit.
  kit.sync_kit(ideal);
  return __ AddI(start, __ value(count));
}

// Compress copy the char into dst_array at index start.
Node* PhaseStringOpts::copy_char(GraphKit& kit, Node* val, Node* dst_array, Node* dst_coder, Node* start) {
  bool dcon = (dst_coder != NULL) && dst_coder->is_Con();
  bool dbyte = dcon ? (dst_coder->get_int() == java_lang_String::CODER_LATIN1) : false;

  IdealKit ideal(&kit, true, true);
  IdealVariable end(ideal); __ declarations_done();
  Node* adr = kit.array_element_address(dst_array, start, T_BYTE);
  if (!dcon){
    __ if_then(dst_coder, BoolTest::eq, __ ConI(java_lang_String::CODER_LATIN1));
  }
  if (!dcon || dbyte) {
    // Destination is Latin1. Store a byte.
    __ store(__ ctrl(), adr, val, T_BYTE, byte_adr_idx, MemNode::unordered);
    __ set(end, __ AddI(start, __ ConI(1)));
  }
  if (!dcon) {
    __ else_();
  }
  if (!dcon || !dbyte) {
    // Destination is UTF16. Store a char.
    __ store(__ ctrl(), adr, val, T_CHAR, byte_adr_idx, MemNode::unordered, false /* require_atomic_access */,
             true /* mismatched */);
    __ set(end, __ AddI(start, __ ConI(2)));
  }
  if (!dcon) {
    __ end_if();
  }
  // Finally sync IdealKit and GraphKit.
  kit.sync_kit(ideal);
  return __ value(end);
}

#undef __
#define __ kit.

// Allocate a byte array of specified length.
Node* PhaseStringOpts::allocate_byte_array(GraphKit& kit, IdealKit* ideal, Node* length) {
  if (ideal != NULL) {
    // Sync IdealKit and graphKit.
    kit.sync_kit(*ideal);
  }
  Node* byte_array = NULL;
  {
    PreserveReexecuteState preexecs(&kit);
    // The original jvms is for an allocation of either a String or
    // StringBuffer so no stack adjustment is necessary for proper
    // reexecution.  If we deoptimize in the slow path the bytecode
    // will be reexecuted and the char[] allocation will be thrown away.
    kit.jvms()->set_should_reexecute(true);
    byte_array = kit.new_array(__ makecon(TypeKlassPtr::make(ciTypeArrayKlass::make(T_BYTE))),
                               length, 1);
  }

  // Mark the allocation so that zeroing is skipped since the code
  // below will overwrite the entire array
  AllocateArrayNode* byte_alloc = AllocateArrayNode::Ideal_array_allocation(byte_array, _gvn);
  byte_alloc->maybe_set_complete(_gvn);

  if (ideal != NULL) {
    // Sync IdealKit and graphKit.
    ideal->sync_kit(&kit);
  }
  return byte_array;
}

jbyte PhaseStringOpts::get_constant_coder(GraphKit& kit, Node* str) {
  assert(str->is_Con(), "String must be constant");
  const TypeOopPtr* str_type = kit.gvn().type(str)->isa_oopptr();
  ciInstance* str_instance = str_type->const_oop()->as_instance();
  jbyte coder = str_instance->field_value_by_offset(java_lang_String::coder_offset()).as_byte();
  assert(CompactStrings || (coder == java_lang_String::CODER_UTF16), "Strings must be UTF16 encoded");
  return coder;
}

int PhaseStringOpts::get_constant_length(GraphKit& kit, Node* str) {
  assert(str->is_Con(), "String must be constant");
  return get_constant_value(kit, str)->length();
}

ciTypeArray* PhaseStringOpts::get_constant_value(GraphKit& kit, Node* str) {
  assert(str->is_Con(), "String must be constant");
  const TypeOopPtr* str_type = kit.gvn().type(str)->isa_oopptr();
  ciInstance* str_instance = str_type->const_oop()->as_instance();
  ciObject* src_array = str_instance->field_value_by_offset(java_lang_String::value_offset()).as_object();
  return src_array->as_type_array();
}

void PhaseStringOpts::replace_string_concat(StringConcat* sc) {
  // Log a little info about the transformation
  sc->maybe_log_transform();

  // pull the JVMState of the allocation into a SafePointNode to serve as
  // as a shim for the insertion of the new code.
  JVMState* jvms     = sc->begin()->jvms()->clone_shallow(C);
  uint size = sc->begin()->req();
  SafePointNode* map = new SafePointNode(size, jvms);

  // copy the control and memory state from the final call into our
  // new starting state.  This allows any preceeding tests to feed
  // into the new section of code.
  for (uint i1 = 0; i1 < TypeFunc::Parms; i1++) {
    map->init_req(i1, sc->end()->in(i1));
  }
  // blow away old allocation arguments
  for (uint i1 = TypeFunc::Parms; i1 < jvms->debug_start(); i1++) {
    map->init_req(i1, C->top());
  }
  // Copy the rest of the inputs for the JVMState
  for (uint i1 = jvms->debug_start(); i1 < sc->begin()->req(); i1++) {
    map->init_req(i1, sc->begin()->in(i1));
  }
  // Make sure the memory state is a MergeMem for parsing.
  if (!map->in(TypeFunc::Memory)->is_MergeMem()) {
    map->set_req(TypeFunc::Memory, MergeMemNode::make(map->in(TypeFunc::Memory)));
  }

  jvms->set_map(map);
  map->ensure_stack(jvms, jvms->method()->max_stack());

  // disconnect all the old StringBuilder calls from the graph
  sc->eliminate_unneeded_control();

  // At this point all the old work has been completely removed from
  // the graph and the saved JVMState exists at the point where the
  // final toString call used to be.
  GraphKit kit(jvms);

  // There may be uncommon traps which are still using the
  // intermediate states and these need to be rewritten to point at
  // the JVMState at the beginning of the transformation.
  sc->convert_uncommon_traps(kit, jvms);

  // Now insert the logic to compute the size of the string followed
  // by all the logic to construct array and resulting string.

  Node* null_string = __ makecon(TypeInstPtr::make(C->env()->the_null_string()));

  // Create a region for the overflow checks to merge into.
  int args = MAX2(sc->num_arguments(), 1);
  RegionNode* overflow = new RegionNode(args);
  kit.gvn().set_type(overflow, Type::CONTROL);

  // Create a hook node to hold onto the individual sizes since they
  // are need for the copying phase.
  Node* string_sizes = new Node(args);

  Node* coder = __ intcon(0);
  Node* length = __ intcon(0);
  // If at least one argument is UTF16 encoded, we can fix the encoding.
  bool coder_fixed = false;

  if (!CompactStrings) {
    // Fix encoding of result string to UTF16
    coder_fixed = true;
    coder = __ intcon(java_lang_String::CODER_UTF16);
  }

  for (int argi = 0; argi < sc->num_arguments(); argi++) {
    Node* arg = sc->argument(argi);
    switch (sc->mode(argi)) {
      case StringConcat::IntMode: {
        Node* string_size = int_stringSize(kit, arg);

        // accumulate total
        length = __ AddI(length, string_size);

        // Cache this value for the use by int_toString
        string_sizes->init_req(argi, string_size);
        break;
      }
      case StringConcat::StringNullCheckMode: {
        const Type* type = kit.gvn().type(arg);
        assert(type != TypePtr::NULL_PTR, "missing check");
        if (!type->higher_equal(TypeInstPtr::NOTNULL)) {
          // Null check with uncommon trap since
          // StringBuilder(null) throws exception.
          // Use special uncommon trap instead of
          // calling normal do_null_check().
          Node* p = __ Bool(__ CmpP(arg, kit.null()), BoolTest::ne);
          IfNode* iff = kit.create_and_map_if(kit.control(), p, PROB_MIN, COUNT_UNKNOWN);
          overflow->add_req(__ IfFalse(iff));
          Node* notnull = __ IfTrue(iff);
          kit.set_control(notnull); // set control for the cast_not_null
          arg = kit.cast_not_null(arg, false);
          sc->set_argument(argi, arg);
        }
        assert(kit.gvn().type(arg)->higher_equal(TypeInstPtr::NOTNULL), "sanity");
        // Fallthrough to add string length.
      }
      case StringConcat::StringMode: {
        const Type* type = kit.gvn().type(arg);
        Node* count = NULL;
        Node* arg_coder = NULL;
        if (type == TypePtr::NULL_PTR) {
          // replace the argument with the null checked version
          arg = null_string;
          sc->set_argument(argi, arg);
          count = kit.load_String_length(arg, true);
          arg_coder = kit.load_String_coder(arg, true);
        } else if (!type->higher_equal(TypeInstPtr::NOTNULL)) {
          // s = s != null ? s : "null";
          // length = length + (s.count - s.offset);
          RegionNode *r = new RegionNode(3);
          kit.gvn().set_type(r, Type::CONTROL);
          Node *phi = new PhiNode(r, type);
          kit.gvn().set_type(phi, phi->bottom_type());
          Node* p = __ Bool(__ CmpP(arg, kit.null()), BoolTest::ne);
          IfNode* iff = kit.create_and_map_if(kit.control(), p, PROB_MIN, COUNT_UNKNOWN);
          Node* notnull = __ IfTrue(iff);
          Node* isnull =  __ IfFalse(iff);
          kit.set_control(notnull); // set control for the cast_not_null
          r->init_req(1, notnull);
          phi->init_req(1, kit.cast_not_null(arg, false));
          r->init_req(2, isnull);
          phi->init_req(2, null_string);
          kit.set_control(r);
          C->record_for_igvn(r);
          C->record_for_igvn(phi);
          // replace the argument with the null checked version
          arg = phi;
          sc->set_argument(argi, arg);
          count = kit.load_String_length(arg, true);
          arg_coder = kit.load_String_coder(arg, true);
        } else {
          // A corresponding nullcheck will be connected during IGVN MemNode::Ideal_common_DU_postCCP
          // kit.control might be a different test, that can be hoisted above the actual nullcheck
          // in case, that the control input is not null, Ideal_common_DU_postCCP will not look for a nullcheck.
          count = kit.load_String_length(arg, false);
          arg_coder = kit.load_String_coder(arg, false);
        }
        if (arg->is_Con()) {
          // Constant string. Get constant coder and length.
          jbyte const_coder = get_constant_coder(kit, arg);
          int const_length = get_constant_length(kit, arg);
          if (const_coder == java_lang_String::CODER_LATIN1) {
            // Can be latin1 encoded
            arg_coder = __ intcon(const_coder);
            count = __ intcon(const_length);
          } else {
            // Found UTF16 encoded string. Fix result array encoding to UTF16.
            coder_fixed = true;
            coder = __ intcon(const_coder);
            count = __ intcon(const_length / 2);
          }
        }

        if (!coder_fixed) {
          coder = __ OrI(coder, arg_coder);
        }
        length = __ AddI(length, count);
        string_sizes->init_req(argi, NULL);
        break;
      }
      case StringConcat::CharMode: {
        // one character only
        const TypeInt* t = kit.gvn().type(arg)->is_int();
        if (!coder_fixed && t->is_con()) {
          // Constant char
          if (t->get_con() <= 255) {
            // Can be latin1 encoded
            coder = __ OrI(coder, __ intcon(java_lang_String::CODER_LATIN1));
          } else {
            // Must be UTF16 encoded. Fix result array encoding to UTF16.
            coder_fixed = true;
            coder = __ intcon(java_lang_String::CODER_UTF16);
          }
        } else if (!coder_fixed) {
          // Not constant
#undef __
#define __ ideal.
          IdealKit ideal(&kit, true, true);
          IdealVariable char_coder(ideal); __ declarations_done();
          // Check if character can be latin1 encoded
          __ if_then(arg, BoolTest::le, __ ConI(0xFF));
            __ set(char_coder, __ ConI(java_lang_String::CODER_LATIN1));
          __ else_();
            __ set(char_coder, __ ConI(java_lang_String::CODER_UTF16));
          __ end_if();
          kit.sync_kit(ideal);
          coder = __ OrI(coder, __ value(char_coder));
#undef __
#define __ kit.
        }
        length = __ AddI(length, __ intcon(1));
        break;
      }
      default:
        ShouldNotReachHere();
    }
    if (argi > 0) {
      // Check that the sum hasn't overflowed
      IfNode* iff = kit.create_and_map_if(kit.control(),
                                          __ Bool(__ CmpI(length, __ intcon(0)), BoolTest::lt),
                                          PROB_MIN, COUNT_UNKNOWN);
      kit.set_control(__ IfFalse(iff));
      overflow->set_req(argi, __ IfTrue(iff));
    }
  }

  {
    // Hook
    PreserveJVMState pjvms(&kit);
    kit.set_control(overflow);
    C->record_for_igvn(overflow);
    kit.uncommon_trap(Deoptimization::Reason_intrinsic,
                      Deoptimization::Action_make_not_entrant);
  }

  Node* result;
  if (!kit.stopped()) {
    assert(CompactStrings || (coder->is_Con() && coder->get_int() == java_lang_String::CODER_UTF16),
           "Result string must be UTF16 encoded if CompactStrings is disabled");

    Node* dst_array = NULL;
    if (sc->num_arguments() == 1 &&
        (sc->mode(0) == StringConcat::StringMode ||
         sc->mode(0) == StringConcat::StringNullCheckMode)) {
      // Handle the case when there is only a single String argument.
      // In this case, we can just pull the value from the String itself.
      dst_array = kit.load_String_value(sc->argument(0), true);
    } else {
      // Allocate destination byte array according to coder
      dst_array = allocate_byte_array(kit, NULL, __ LShiftI(length, coder));

      // Now copy the string representations into the final byte[]
      Node* start = __ intcon(0);
      for (int argi = 0; argi < sc->num_arguments(); argi++) {
        Node* arg = sc->argument(argi);
        switch (sc->mode(argi)) {
          case StringConcat::IntMode: {
            start = int_getChars(kit, arg, dst_array, coder, start, string_sizes->in(argi));
            break;
          }
          case StringConcat::StringNullCheckMode:
          case StringConcat::StringMode: {
            start = copy_string(kit, arg, dst_array, coder, start);
            break;
          }
          case StringConcat::CharMode: {
            start = copy_char(kit, arg, dst_array, coder, start);
          break;
          }
          default:
            ShouldNotReachHere();
        }
      }
    }

    // If we're not reusing an existing String allocation then allocate one here.
    result = sc->string_alloc();
    if (result == NULL) {
      PreserveReexecuteState preexecs(&kit);
      // The original jvms is for an allocation of either a String or
      // StringBuffer so no stack adjustment is necessary for proper
      // reexecution.
      kit.jvms()->set_should_reexecute(true);
      result = kit.new_instance(__ makecon(TypeKlassPtr::make(C->env()->String_klass())));
    }

    // Initialize the string
    kit.store_String_value(result, dst_array);
    kit.store_String_coder(result, coder);

    // The value field is final. Emit a barrier here to ensure that the effect
    // of the initialization is committed to memory before any code publishes
    // a reference to the newly constructed object (see Parse::do_exits()).
    assert(AllocateNode::Ideal_allocation(result, _gvn) != NULL, "should be newly allocated");
    kit.insert_mem_bar(Op_MemBarRelease, result);
  } else {
    result = C->top();
  }
  // hook up the outgoing control and result
  kit.replace_call(sc->end(), result);

  // Unhook any hook nodes
  string_sizes->disconnect_inputs(C);
  sc->cleanup();
}
