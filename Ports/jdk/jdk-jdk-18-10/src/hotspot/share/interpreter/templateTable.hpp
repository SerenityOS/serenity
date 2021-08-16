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

#ifndef SHARE_INTERPRETER_TEMPLATETABLE_HPP
#define SHARE_INTERPRETER_TEMPLATETABLE_HPP

#include "interpreter/bytecodes.hpp"
#include "memory/allocation.hpp"
#include "runtime/frame.hpp"
#include "utilities/macros.hpp"

#ifndef ZERO
// All the necessary definitions used for (bytecode) template generation. Instead of
// spreading the implementation functionality for each bytecode in the interpreter
// and the snippet generator, a template is assigned to each bytecode which can be
// used to generate the bytecode's implementation if needed.

class InterpreterMacroAssembler;

// A Template describes the properties of a code template for a given bytecode
// and provides a generator to generate the code template.

class Template {
 private:
  enum Flags {
    uses_bcp_bit,                                // set if template needs the bcp pointing to bytecode
    does_dispatch_bit,                           // set if template dispatches on its own
    calls_vm_bit,                                // set if template calls the vm
    wide_bit                                     // set if template belongs to a wide instruction
  };

  typedef void (*generator)(int arg);

  int       _flags;                              // describes interpreter template properties (bcp unknown)
  TosState  _tos_in;                             // tos cache state before template execution
  TosState  _tos_out;                            // tos cache state after  template execution
  generator _gen;                                // template code generator
  int       _arg;                                // argument for template code generator

  void      initialize(int flags, TosState tos_in, TosState tos_out, generator gen, int arg);

  friend class TemplateTable;

 public:
  Bytecodes::Code bytecode() const;
  bool      is_valid() const                     { return _gen != NULL; }
  bool      uses_bcp() const                     { return (_flags & (1 << uses_bcp_bit     )) != 0; }
  bool      does_dispatch() const                { return (_flags & (1 << does_dispatch_bit)) != 0; }
  bool      calls_vm() const                     { return (_flags & (1 << calls_vm_bit     )) != 0; }
  bool      is_wide() const                      { return (_flags & (1 << wide_bit         )) != 0; }
  TosState  tos_in() const                       { return _tos_in; }
  TosState  tos_out() const                      { return _tos_out; }
  void      generate(InterpreterMacroAssembler* masm);
};


// The TemplateTable defines all Templates and provides accessor functions
// to get the template for a given bytecode.

class TemplateTable: AllStatic {
 public:
  enum Operation { add, sub, mul, div, rem, _and, _or, _xor, shl, shr, ushr };
  enum Condition { equal, not_equal, less, less_equal, greater, greater_equal };
  enum CacheByte { f1_byte = 1, f2_byte = 2 };  // byte_no codes
  enum RewriteControl { may_rewrite, may_not_rewrite };  // control for fast code under CDS

 private:
  static Template        _template_table     [Bytecodes::number_of_codes];
  static Template        _template_table_wide[Bytecodes::number_of_codes];

  static Template*       _desc;                  // the current template to be generated
  static Bytecodes::Code bytecode()              { return _desc->bytecode(); }
 public:
  //%note templates_1
  static InterpreterMacroAssembler* _masm;       // the assembler used when generating templates

 private:

  // special registers
  static inline Address at_bcp(int offset);

  // helpers
  static void unimplemented_bc();
  static void patch_bytecode(Bytecodes::Code bc, Register bc_reg,
                             Register temp_reg, bool load_bc_into_bc_reg = true, int byte_no = -1);

  // C calls
  static void call_VM(Register oop_result, address entry_point);
  static void call_VM(Register oop_result, address entry_point, Register arg_1);
  static void call_VM(Register oop_result, address entry_point, Register arg_1, Register arg_2);
  static void call_VM(Register oop_result, address entry_point, Register arg_1, Register arg_2, Register arg_3);

  // these overloadings are not presently used on SPARC:
  static void call_VM(Register oop_result, Register last_java_sp, address entry_point);
  static void call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1);
  static void call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, Register arg_2);
  static void call_VM(Register oop_result, Register last_java_sp, address entry_point, Register arg_1, Register arg_2, Register arg_3);

  // bytecodes
  static void nop();

  static void aconst_null();
  static void iconst(int value);
  static void lconst(int value);
  static void fconst(int value);
  static void dconst(int value);

  static void bipush();
  static void sipush();
  static void ldc(bool wide);
  static void ldc2_w();
  static void fast_aldc(bool wide);

  static void locals_index(Register reg, int offset = 1);
  static void iload();
  static void fast_iload();
  static void fast_iload2();
  static void fast_icaload();
  static void lload();
  static void fload();
  static void dload();
  static void aload();

  static void locals_index_wide(Register reg);
  static void wide_iload();
  static void wide_lload();
  static void wide_fload();
  static void wide_dload();
  static void wide_aload();

  static void iaload();
  static void laload();
  static void faload();
  static void daload();
  static void aaload();
  static void baload();
  static void caload();
  static void saload();

  static void iload(int n);
  static void lload(int n);
  static void fload(int n);
  static void dload(int n);
  static void aload(int n);
  static void aload_0();
  static void nofast_aload_0();
  static void nofast_iload();
  static void iload_internal(RewriteControl rc = may_rewrite);
  static void aload_0_internal(RewriteControl rc = may_rewrite);

  static void istore();
  static void lstore();
  static void fstore();
  static void dstore();
  static void astore();

  static void wide_istore();
  static void wide_lstore();
  static void wide_fstore();
  static void wide_dstore();
  static void wide_astore();

  static void iastore();
  static void lastore();
  static void fastore();
  static void dastore();
  static void aastore();
  static void bastore();
  static void castore();
  static void sastore();

  static void istore(int n);
  static void lstore(int n);
  static void fstore(int n);
  static void dstore(int n);
  static void astore(int n);

  static void pop();
  static void pop2();
  static void dup();
  static void dup_x1();
  static void dup_x2();
  static void dup2();
  static void dup2_x1();
  static void dup2_x2();
  static void swap();

  static void iop2(Operation op);
  static void lop2(Operation op);
  static void fop2(Operation op);
  static void dop2(Operation op);

  static void idiv();
  static void irem();

  static void lmul();
  static void ldiv();
  static void lrem();
  static void lshl();
  static void lshr();
  static void lushr();

  static void ineg();
  static void lneg();
  static void fneg();
  static void dneg();

  static void iinc();
  static void wide_iinc();
  static void convert();
  static void lcmp();

  static void float_cmp (bool is_float, int unordered_result);
  static void float_cmp (int unordered_result);
  static void double_cmp(int unordered_result);

  static void branch(bool is_jsr, bool is_wide);
  static void if_0cmp   (Condition cc);
  static void if_icmp   (Condition cc);
  static void if_nullcmp(Condition cc);
  static void if_acmp   (Condition cc);

  static void _goto();
  static void jsr();
  static void ret();
  static void wide_ret();

  static void goto_w();
  static void jsr_w();

  static void tableswitch();
  static void lookupswitch();
  static void fast_linearswitch();
  static void fast_binaryswitch();

  static void _return(TosState state);

  static void resolve_cache_and_index(int byte_no,       // one of 1,2,11
                                      Register cache,    // output for CP cache
                                      Register index,    // output for CP index
                                      size_t index_size); // one of 1,2,4
  static void load_invoke_cp_cache_entry(int byte_no,
                                         Register method,
                                         Register itable_index,
                                         Register flags,
                                         bool is_invokevirtual,
                                         bool is_virtual_final,
                                         bool is_invokedynamic);
  static void load_field_cp_cache_entry(Register obj,
                                        Register cache,
                                        Register index,
                                        Register offset,
                                        Register flags,
                                        bool is_static);
  static void invokevirtual(int byte_no);
  static void invokespecial(int byte_no);
  static void invokestatic(int byte_no);
  static void invokeinterface(int byte_no);
  static void invokedynamic(int byte_no);
  static void invokehandle(int byte_no);
  static void fast_invokevfinal(int byte_no);

  static void getfield_or_static(int byte_no, bool is_static, RewriteControl rc = may_rewrite);
  static void putfield_or_static(int byte_no, bool is_static, RewriteControl rc = may_rewrite);

  static void getfield(int byte_no);
  static void putfield(int byte_no);
  static void nofast_getfield(int byte_no);
  static void nofast_putfield(int byte_no);
  static void getstatic(int byte_no);
  static void putstatic(int byte_no);
  static void pop_and_check_object(Register obj);
  static void condy_helper(Label& Done);  // shared by ldc instances

  static void _new();
  static void newarray();
  static void anewarray();
  static void arraylength();
  static void checkcast();
  static void instanceof();

  static void athrow();

  static void monitorenter();
  static void monitorexit();

  static void wide();
  static void multianewarray();

  static void fast_xaccess(TosState state);
  static void fast_accessfield(TosState state);
  static void fast_storefield(TosState state);

  static void _breakpoint();

  static void shouldnotreachhere();

  // jvmti support
  static void jvmti_post_field_access(Register cache, Register index, bool is_static, bool has_tos);
  static void jvmti_post_field_mod(Register cache, Register index, bool is_static);
  static void jvmti_post_fast_field_mod();

  // debugging of TemplateGenerator
  static void transition(TosState tos_in, TosState tos_out);// checks if in/out states expected by template generator correspond to table entries

  // initialization helpers
  static void def(Bytecodes::Code code, int flags, TosState in, TosState out, void (*gen)(            ), char filler );
  static void def(Bytecodes::Code code, int flags, TosState in, TosState out, void (*gen)(int arg     ), int arg     );
  static void def(Bytecodes::Code code, int flags, TosState in, TosState out, void (*gen)(bool arg    ), bool arg    );
  static void def(Bytecodes::Code code, int flags, TosState in, TosState out, void (*gen)(TosState tos), TosState tos);
  static void def(Bytecodes::Code code, int flags, TosState in, TosState out, void (*gen)(Operation op), Operation op);
  static void def(Bytecodes::Code code, int flags, TosState in, TosState out, void (*gen)(Condition cc), Condition cc);

  friend class Template;

  // InterpreterMacroAssembler::is_a(), etc., need TemplateTable::call_VM().
  friend class InterpreterMacroAssembler;

 public:
  // Initialization
  static void initialize();

  // Templates
  static Template* template_for     (Bytecodes::Code code)  { Bytecodes::check     (code); return &_template_table     [code]; }
  static Template* template_for_wide(Bytecodes::Code code)  { Bytecodes::wide_check(code); return &_template_table_wide[code]; }

  // Platform specifics
#include CPU_HEADER(templateTable)

};
#endif /* !ZERO */

#endif // SHARE_INTERPRETER_TEMPLATETABLE_HPP
