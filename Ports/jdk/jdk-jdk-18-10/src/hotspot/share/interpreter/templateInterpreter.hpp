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

#ifndef SHARE_INTERPRETER_TEMPLATEINTERPRETER_HPP
#define SHARE_INTERPRETER_TEMPLATEINTERPRETER_HPP

#include "interpreter/abstractInterpreter.hpp"
#include "interpreter/templateTable.hpp"

// This file contains the platform-independent parts
// of the template interpreter and the template interpreter generator.

#ifndef ZERO

class InterpreterMacroAssembler;
class InterpreterCodelet;

//------------------------------------------------------------------------------------------------------------------------
// A little wrapper class to group tosca-specific entry points into a unit.
// (tosca = Top-Of-Stack CAche)

class EntryPoint {
 private:
  address _entry[number_of_states];

 public:
  // Construction
  EntryPoint();
  EntryPoint(address bentry, address zentry, address centry, address sentry, address aentry, address ientry, address lentry, address fentry, address dentry, address ventry);
  // Will use the ientry for each of [bzcs]entry
  EntryPoint(address aentry, address ientry, address lentry, address fentry, address dentry, address ventry);
  // Attributes
  address entry(TosState state) const;                // return target address for a given tosca state
  void    set_entry(TosState state, address entry);   // set    target address for a given tosca state
  void    print();

  // Comparison
  bool operator == (const EntryPoint& y);             // for debugging only
};


//------------------------------------------------------------------------------------------------------------------------
// A little wrapper class to group tosca-specific dispatch tables into a unit.

class DispatchTable {
 public:
  enum { length = 1 << BitsPerByte };                 // an entry point for each byte value (also for undefined bytecodes)

 private:
  address _table[number_of_states][length];           // dispatch tables, indexed by tosca and bytecode

 public:
  // Attributes
  EntryPoint entry(int i) const;                      // return entry point for a given bytecode i
  void       set_entry(int i, EntryPoint& entry);     // set    entry point for a given bytecode i
  address*   table_for(TosState state)          { return _table[state]; }
  address*   table_for()                        { return table_for((TosState)0); }
  int        distance_from(address *table)      { return table - table_for(); }
  int        distance_from(TosState state)      { return distance_from(table_for(state)); }

  // Comparison
  bool operator == (DispatchTable& y);                // for debugging only
};

class TemplateInterpreter: public AbstractInterpreter {
  friend class VMStructs;
  friend class InterpreterMacroAssembler;
  friend class TemplateInterpreterGenerator;
  friend class TemplateTable;
  friend class CodeCacheExtensions;
  // friend class Interpreter;
 public:

  enum MoreConstants {
    max_invoke_length = 5,    // invokedynamic is the longest
    max_bytecode_length = 6,  // worse case is wide iinc, "reexecute" bytecodes are excluded because "skip" will be 0
    number_of_return_entries  = max_invoke_length + 1,          // number of return entry points
    number_of_deopt_entries   = max_bytecode_length + 1,        // number of deoptimization entry points
    number_of_return_addrs    = number_of_states                // number of return addresses
  };

 protected:

  static address    _throw_ArrayIndexOutOfBoundsException_entry;
  static address    _throw_ArrayStoreException_entry;
  static address    _throw_ArithmeticException_entry;
  static address    _throw_ClassCastException_entry;
  static address    _throw_NullPointerException_entry;
  static address    _throw_exception_entry;

  static address    _throw_StackOverflowError_entry;

  static address    _remove_activation_entry;                   // continuation address if an exception is not handled by current frame
  static address    _remove_activation_preserving_args_entry;   // continuation address when current frame is being popped

#ifndef PRODUCT
  static EntryPoint _trace_code;
#endif // !PRODUCT
  static EntryPoint _return_entry[number_of_return_entries];    // entry points to return to from a call
  static EntryPoint _earlyret_entry;                            // entry point to return early from a call
  static EntryPoint _deopt_entry[number_of_deopt_entries];      // entry points to return to from a deoptimization
  static address    _deopt_reexecute_return_entry;
  static EntryPoint _safept_entry;

  static address _invoke_return_entry[number_of_return_addrs];           // for invokestatic, invokespecial, invokevirtual return entries
  static address _invokeinterface_return_entry[number_of_return_addrs];  // for invokeinterface return entries
  static address _invokedynamic_return_entry[number_of_return_addrs];    // for invokedynamic return entries

  static DispatchTable _active_table;                           // the active    dispatch table (used by the interpreter for dispatch)
  static DispatchTable _normal_table;                           // the normal    dispatch table (used to set the active table in normal mode)
  static DispatchTable _safept_table;                           // the safepoint dispatch table (used to set the active table for safepoints)
  static address       _wentry_point[DispatchTable::length];    // wide instructions only (vtos tosca always)


 public:
  // Initialization/debugging
  static void       initialize_stub();
  static void       initialize_code();
  // this only returns whether a pc is within generated code for the interpreter.
  static bool       contains(address pc)                        { return _code != NULL && _code->contains(pc); }
  // Debugging/printing
  static InterpreterCodelet* codelet_containing(address pc);


 public:

  static address    remove_activation_early_entry(TosState state) { return _earlyret_entry.entry(state); }
  static address    remove_activation_preserving_args_entry()     { return _remove_activation_preserving_args_entry; }

  static address    remove_activation_entry()                   { return _remove_activation_entry; }
  static address    throw_exception_entry()                     { return _throw_exception_entry; }
  static address    throw_ArithmeticException_entry()           { return _throw_ArithmeticException_entry; }
  static address    throw_NullPointerException_entry()          { return _throw_NullPointerException_entry; }
  static address    throw_StackOverflowError_entry()            { return _throw_StackOverflowError_entry; }

  // Code generation
#ifndef PRODUCT
  static address    trace_code    (TosState state)              { return _trace_code.entry(state); }
#endif // !PRODUCT
  static address*   dispatch_table(TosState state)              { return _active_table.table_for(state); }
  static address*   dispatch_table()                            { return _active_table.table_for(); }
  static int        distance_from_dispatch_table(TosState state){ return _active_table.distance_from(state); }
  static address*   normal_table(TosState state)                { return _normal_table.table_for(state); }
  static address*   normal_table()                              { return _normal_table.table_for(); }
  static address*   safept_table(TosState state)                { return _safept_table.table_for(state); }

  // Support for invokes
  static address*   invoke_return_entry_table()                 { return _invoke_return_entry; }
  static address*   invokeinterface_return_entry_table()        { return _invokeinterface_return_entry; }
  static address*   invokedynamic_return_entry_table()          { return _invokedynamic_return_entry; }
  static int        TosState_as_index(TosState state);

  static address* invoke_return_entry_table_for(Bytecodes::Code code);

  static address deopt_entry(TosState state, int length);
  static address deopt_reexecute_return_entry()                 { return _deopt_reexecute_return_entry; }
  static address return_entry(TosState state, int length, Bytecodes::Code code);

  // Safepoint support
  static void       notice_safepoints();                        // stops the thread when reaching a safepoint
  static void       ignore_safepoints();                        // ignores safepoints

  // Deoptimization support
  // Compute the entry address for continuation after
  static address deopt_continue_after_entry(Method* method,
                                            address bcp,
                                            int callee_parameters,
                                            bool is_top_frame);
  // Deoptimization should reexecute this bytecode
  static bool    bytecode_should_reexecute(Bytecodes::Code code);
  // Compute the address for reexecution
  static address deopt_reexecute_entry(Method* method, address bcp);

  // Size of interpreter code.  Max size with JVMTI
  static int InterpreterCodeSize;
};

#endif // !ZERO

#endif // SHARE_INTERPRETER_TEMPLATEINTERPRETER_HPP
