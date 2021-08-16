/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_C1_C1_IR_HPP
#define SHARE_C1_C1_IR_HPP

#include "c1/c1_Instruction.hpp"
#include "ci/ciExceptionHandler.hpp"
#include "ci/ciMethod.hpp"
#include "ci/ciStreams.hpp"
#include "memory/allocation.hpp"

// An XHandler is a C1 internal description for an exception handler

class XHandler: public CompilationResourceObj {
 private:
  ciExceptionHandler* _desc;

  BlockBegin*         _entry_block;  // Entry block of xhandler
  LIR_List*           _entry_code;   // LIR-operations that must be executed before jumping to entry_block
  int                 _entry_pco;    // pco where entry_code (or entry_block if no entry_code) starts
  int                 _phi_operand;  // For resolving of phi functions at begin of entry_block
  int                 _scope_count;  // for filling ExceptionRangeEntry::scope_count

#ifdef ASSERT
  int                 _lir_op_id;    // op_id of the LIR-operation throwing to this handler
#endif

 public:
  // creation
  XHandler(ciExceptionHandler* desc)
    : _desc(desc)
    , _entry_block(NULL)
    , _entry_code(NULL)
    , _entry_pco(-1)
    , _phi_operand(-1)
    , _scope_count(-1)
#ifdef ASSERT
    , _lir_op_id(-1)
#endif
  { }

  XHandler(XHandler* other)
    : _desc(other->_desc)
    , _entry_block(other->_entry_block)
    , _entry_code(other->_entry_code)
    , _entry_pco(other->_entry_pco)
    , _phi_operand(other->_phi_operand)
    , _scope_count(other->_scope_count)
#ifdef ASSERT
    , _lir_op_id(other->_lir_op_id)
#endif
  { }

  // accessors for data of ciExceptionHandler
  int  beg_bci() const                           { return _desc->start(); }
  int  end_bci() const                           { return _desc->limit(); }
  int  handler_bci() const                       { return _desc->handler_bci(); }
  bool is_catch_all() const                      { return _desc->is_catch_all(); }
  int  catch_type() const                        { return _desc->catch_klass_index(); }
  ciInstanceKlass* catch_klass() const           { return _desc->catch_klass(); }
  bool covers(int bci) const                     { return beg_bci() <= bci && bci < end_bci(); }

  // accessors for additional fields
  BlockBegin* entry_block() const                { return _entry_block; }
  LIR_List*   entry_code() const                 { return _entry_code; }
  int         entry_pco() const                  { return _entry_pco; }
  int         phi_operand() const                { assert(_phi_operand != -1, "not set"); return _phi_operand; }
  int         scope_count() const                { assert(_scope_count != -1, "not set"); return _scope_count; }
  DEBUG_ONLY(int lir_op_id() const               { return _lir_op_id; });

  void set_entry_block(BlockBegin* entry_block) {
    assert(entry_block->is_set(BlockBegin::exception_entry_flag), "must be an exception handler entry");
    assert(entry_block->bci() == handler_bci(), "bci's must correspond");
    _entry_block = entry_block;
  }
  void set_entry_code(LIR_List* entry_code)      { _entry_code = entry_code; }
  void set_entry_pco(int entry_pco)              { _entry_pco = entry_pco; }
  void set_phi_operand(int phi_operand)          { _phi_operand = phi_operand; }
  void set_scope_count(int scope_count)          { _scope_count = scope_count; }
  DEBUG_ONLY(void set_lir_op_id(int lir_op_id)   { _lir_op_id = lir_op_id; });

  bool equals(XHandler* other) const;
};

typedef GrowableArray<XHandler*> _XHandlerList;

// XHandlers is the C1 internal list of exception handlers for a method
class XHandlers: public CompilationResourceObj {
 private:
  _XHandlerList    _list;

 public:
  // creation
  XHandlers() : _list()                          { }
  XHandlers(ciMethod* method);
  XHandlers(XHandlers* other);

  // accessors
  int       length() const                       { return _list.length(); }
  XHandler* handler_at(int i) const              { return _list.at(i); }
  bool      has_handlers() const                 { return _list.length() > 0; }
  void      append(XHandler* h)                  { _list.append(h); }
  XHandler* remove_last()                        { return _list.pop(); }

  bool      could_catch(ciInstanceKlass* klass, bool type_is_exact) const;
  bool      equals(XHandlers* others) const;
};


class IRScope;
typedef GrowableArray<IRScope*> IRScopeList;

class Compilation;
class IRScope: public CompilationResourceObj {
 private:
  // hierarchy
  Compilation*  _compilation;                    // the current compilation
  IRScope*      _caller;                         // the caller scope, or NULL
  int           _level;                          // the inlining level
  ciMethod*     _method;                         // the corresponding method
  IRScopeList   _callees;                        // the inlined method scopes

  // graph
  XHandlers*    _xhandlers;                      // the exception handlers
  int           _number_of_locks;                // the number of monitor lock slots needed
  bool          _monitor_pairing_ok;             // the monitor pairing info
  bool          _wrote_final;                    // has written final field
  bool          _wrote_fields;                   // has written fields
  bool          _wrote_volatile;                 // has written volatile field
  BlockBegin*   _start;                          // the start block, successsors are method entries

  ResourceBitMap _requires_phi_function;         // bit is set if phi functions at loop headers are necessary for a local variable

  // helper functions
  BlockBegin* build_graph(Compilation* compilation, int osr_bci);

 public:
  // creation
  IRScope(Compilation* compilation, IRScope* caller, int caller_bci, ciMethod* method, int osr_bci, bool create_graph = false);

  // accessors
  Compilation*  compilation() const              { return _compilation; }
  IRScope*      caller() const                   { return _caller; }
  int           level() const                    { return _level; }
  ciMethod*     method() const                   { return _method; }
  int           max_stack() const;               // NOTE: expensive
  BitMap&       requires_phi_function()          { return _requires_phi_function; }

  // hierarchy
  bool          is_top_scope() const             { return _caller == NULL; }
  void          add_callee(IRScope* callee)      { _callees.append(callee); }
  int           number_of_callees() const        { return _callees.length(); }
  IRScope*      callee_no(int i) const           { return _callees.at(i); }

  // accessors, graph
  bool          is_valid() const                 { return start() != NULL; }
  XHandlers*    xhandlers() const                { return _xhandlers; }
  int           number_of_locks() const          { return _number_of_locks; }
  void          set_min_number_of_locks(int n)   { if (n > _number_of_locks) _number_of_locks = n; }
  bool          monitor_pairing_ok() const       { return _monitor_pairing_ok; }
  BlockBegin*   start() const                    { return _start; }
  void          set_wrote_final()                { _wrote_final = true; }
  bool          wrote_final    () const          { return _wrote_final; }
  void          set_wrote_fields()               { _wrote_fields = true; }
  bool          wrote_fields    () const         { return _wrote_fields; }
  void          set_wrote_volatile()             { _wrote_volatile = true; }
  bool          wrote_volatile    () const       { return _wrote_volatile; }
};


//
// IRScopeDebugInfo records the debug information for a particular IRScope
// in a particular CodeEmitInfo.  This allows the information to be computed
// once early enough for the OopMap to be available to the LIR and also to be
// reemited for different pcs using the same CodeEmitInfo without recomputing
// everything.
//

class IRScopeDebugInfo: public CompilationResourceObj {
 private:
  IRScope*                      _scope;
  int                           _bci;
  GrowableArray<ScopeValue*>*   _locals;
  GrowableArray<ScopeValue*>*   _expressions;
  GrowableArray<MonitorValue*>* _monitors;
  IRScopeDebugInfo*             _caller;

 public:
  IRScopeDebugInfo(IRScope*                      scope,
                   int                           bci,
                   GrowableArray<ScopeValue*>*   locals,
                   GrowableArray<ScopeValue*>*   expressions,
                   GrowableArray<MonitorValue*>* monitors,
                   IRScopeDebugInfo*             caller):
      _scope(scope)
    , _bci(bci)
    , _locals(locals)
    , _expressions(expressions)
    , _monitors(monitors)
    , _caller(caller) {}


  IRScope*                      scope()       { return _scope;       }
  int                           bci()         { return _bci;         }
  GrowableArray<ScopeValue*>*   locals()      { return _locals;      }
  GrowableArray<ScopeValue*>*   expressions() { return _expressions; }
  GrowableArray<MonitorValue*>* monitors()    { return _monitors;    }
  IRScopeDebugInfo*             caller()      { return _caller;      }

  //Whether we should reexecute this bytecode for deopt
  bool should_reexecute();

  void record_debug_info(DebugInformationRecorder* recorder, int pc_offset, bool topmost, bool is_method_handle_invoke = false) {
    if (caller() != NULL) {
      // Order is significant:  Must record caller first.
      caller()->record_debug_info(recorder, pc_offset, false/*topmost*/);
    }
    DebugToken* locvals = recorder->create_scope_values(locals());
    DebugToken* expvals = recorder->create_scope_values(expressions());
    DebugToken* monvals = recorder->create_monitor_values(monitors());
    // reexecute allowed only for the topmost frame
    bool reexecute = topmost ? should_reexecute() : false;
    bool return_oop = false; // This flag will be ignored since it used only for C2 with escape analysis.
    bool rethrow_exception = false;
    bool is_opt_native = false;
    bool has_ea_local_in_scope = false;
    bool arg_escape = false;
    recorder->describe_scope(pc_offset, methodHandle(), scope()->method(), bci(),
                             reexecute, rethrow_exception, is_method_handle_invoke, is_opt_native, return_oop,
                             has_ea_local_in_scope, arg_escape, locvals, expvals, monvals);
  }
};


class CodeEmitInfo: public CompilationResourceObj {
  friend class LinearScan;
 private:
  IRScopeDebugInfo* _scope_debug_info;
  IRScope*          _scope;
  XHandlers*        _exception_handlers;
  OopMap*           _oop_map;
  ValueStack*       _stack;                      // used by deoptimization (contains also monitors
  bool              _is_method_handle_invoke;    // true if the associated call site is a MethodHandle call site.
  bool              _deoptimize_on_exception;

  FrameMap*     frame_map() const                { return scope()->compilation()->frame_map(); }
  Compilation*  compilation() const              { return scope()->compilation(); }

 public:

  // use scope from ValueStack
  CodeEmitInfo(ValueStack* stack, XHandlers* exception_handlers, bool deoptimize_on_exception = false);

  // make a copy
  CodeEmitInfo(CodeEmitInfo* info, ValueStack* stack = NULL);

  // accessors
  OopMap* oop_map()                              { return _oop_map; }
  ciMethod* method() const                       { return _scope->method(); }
  IRScope* scope() const                         { return _scope; }
  XHandlers* exception_handlers() const          { return _exception_handlers; }
  ValueStack* stack() const                      { return _stack; }
  bool deoptimize_on_exception() const           { return _deoptimize_on_exception; }

  void add_register_oop(LIR_Opr opr);
  void record_debug_info(DebugInformationRecorder* recorder, int pc_offset);

  bool     is_method_handle_invoke() const { return _is_method_handle_invoke;     }
  void set_is_method_handle_invoke(bool x) {        _is_method_handle_invoke = x; }

  int interpreter_frame_size() const;
};


class IR: public CompilationResourceObj {
 private:
  Compilation*     _compilation;                 // the current compilation
  IRScope*         _top_scope;                   // the root of the scope hierarchy
  int              _num_loops;                   // Total number of loops
  BlockList*       _code;                        // the blocks in code generation order w/ use counts

 public:
  // creation
  IR(Compilation* compilation, ciMethod* method, int osr_bci);

  // accessors
  bool             is_valid() const              { return top_scope()->is_valid(); }
  Compilation*     compilation() const           { return _compilation; }
  IRScope*         top_scope() const             { return _top_scope; }
  int              number_of_locks() const       { return top_scope()->number_of_locks(); }
  ciMethod*        method() const                { return top_scope()->method(); }
  BlockBegin*      start() const                 { return top_scope()->start(); }
  BlockBegin*      std_entry() const             { return start()->end()->as_Base()->std_entry(); }
  BlockBegin*      osr_entry() const             { return start()->end()->as_Base()->osr_entry(); }
  BlockList*       code() const                  { return _code; }
  int              num_loops() const             { return _num_loops; }
  int              max_stack() const             { return top_scope()->max_stack(); } // expensive

  // ir manipulation
  void optimize_blocks();
  void eliminate_null_checks();
  void compute_predecessors();
  void split_critical_edges();
  void compute_code();
  void compute_use_counts();

  // The linear-scan order and the code emission order are equal, but
  // this may change in future
  BlockList* linear_scan_order() {  assert(_code != NULL, "not computed"); return _code; }

  // iteration
  void iterate_preorder   (BlockClosure* closure);
  void iterate_postorder  (BlockClosure* closure);
  void iterate_linear_scan_order(BlockClosure* closure);

  // debugging
  static void print(BlockBegin* start, bool cfg_only, bool live_only = false) PRODUCT_RETURN;
  void print(bool cfg_only, bool live_only = false)                           PRODUCT_RETURN;
  void verify()                                                               PRODUCT_RETURN;
};


// Globally do instruction substitution and remove substituted
// instructions from the instruction list.
//

class SubstitutionResolver: public BlockClosure, ValueVisitor {
  virtual void visit(Value* v);

 public:
  SubstitutionResolver(IR* hir) {
    hir->iterate_preorder(this);
  }

  SubstitutionResolver(BlockBegin* block) {
    block->iterate_preorder(this);
  }

  virtual void block_do(BlockBegin* block);
};

#endif // SHARE_C1_C1_IR_HPP
