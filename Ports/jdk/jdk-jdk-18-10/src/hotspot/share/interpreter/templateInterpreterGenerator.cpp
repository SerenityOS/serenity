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
#include "compiler/disassembler.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "interpreter/interp_masm.hpp"
#include "interpreter/templateInterpreter.hpp"
#include "interpreter/templateInterpreterGenerator.hpp"
#include "interpreter/templateTable.hpp"
#include "oops/methodData.hpp"

#define __ Disassembler::hook<InterpreterMacroAssembler>(__FILE__, __LINE__, _masm)->

TemplateInterpreterGenerator::TemplateInterpreterGenerator(StubQueue* _code): AbstractInterpreterGenerator(_code) {
  _unimplemented_bytecode    = NULL;
  _illegal_bytecode_sequence = NULL;
  generate_all();
}

static const BasicType types[Interpreter::number_of_result_handlers] = {
  T_BOOLEAN,
  T_CHAR   ,
  T_BYTE   ,
  T_SHORT  ,
  T_INT    ,
  T_LONG   ,
  T_VOID   ,
  T_FLOAT  ,
  T_DOUBLE ,
  T_OBJECT
};

void TemplateInterpreterGenerator::generate_all() {
  { CodeletMark cm(_masm, "slow signature handler");
    AbstractInterpreter::_slow_signature_handler = generate_slow_signature_handler();
  }

  { CodeletMark cm(_masm, "error exits");
    _unimplemented_bytecode    = generate_error_exit("unimplemented bytecode");
    _illegal_bytecode_sequence = generate_error_exit("illegal bytecode sequence - method not verified");
  }

#ifndef PRODUCT
  if (TraceBytecodes) {
    CodeletMark cm(_masm, "bytecode tracing support");
    Interpreter::_trace_code =
      EntryPoint(
                 generate_trace_code(atos),
                 generate_trace_code(itos),
                 generate_trace_code(ltos),
                 generate_trace_code(ftos),
                 generate_trace_code(dtos),
                 generate_trace_code(vtos)
                 );
  }
#endif // !PRODUCT

  { CodeletMark cm(_masm, "return entry points");
    Interpreter::_return_entry[0] = EntryPoint();
    for (int i = 1; i < Interpreter::number_of_return_entries; i++) {
      Interpreter::_return_entry[i] =
        EntryPoint(
                   generate_return_entry_for(atos, i, sizeof(u2)),
                   generate_return_entry_for(itos, i, sizeof(u2)),
                   generate_return_entry_for(ltos, i, sizeof(u2)),
                   generate_return_entry_for(ftos, i, sizeof(u2)),
                   generate_return_entry_for(dtos, i, sizeof(u2)),
                   generate_return_entry_for(vtos, i, sizeof(u2))
                   );
    }
  }

  { CodeletMark cm(_masm, "invoke return entry points");
    // These states are in order specified in TosState, except btos/ztos/ctos/stos which
    // are the same as itos since there is no top of stack optimization for these types
    const TosState states[] = {ilgl, ilgl, ilgl, ilgl, itos, ltos, ftos, dtos, atos, vtos, ilgl};
    const int invoke_length = Bytecodes::length_for(Bytecodes::_invokestatic);
    const int invokeinterface_length = Bytecodes::length_for(Bytecodes::_invokeinterface);
    const int invokedynamic_length = Bytecodes::length_for(Bytecodes::_invokedynamic);

    assert(invoke_length >= 0 && invoke_length < Interpreter::number_of_return_entries, "invariant");
    assert(invokeinterface_length >= 0 && invokeinterface_length < Interpreter::number_of_return_entries, "invariant");

    for (int i = itos; i < Interpreter::number_of_return_addrs; i++) {
      TosState state = states[i];
      assert(state != ilgl, "states array is wrong above");

      // Reuse generated entry points
      Interpreter::_invoke_return_entry[i]          = Interpreter::_return_entry[invoke_length].entry(state);
      Interpreter::_invokeinterface_return_entry[i] = Interpreter::_return_entry[invokeinterface_length].entry(state);

      Interpreter::_invokedynamic_return_entry[i]   = generate_return_entry_for(state, invokedynamic_length, sizeof(u4));
    }

    // set itos entry points for btos/ztos/ctos/stos
    for (int i = 0; i < itos; i++) {
      Interpreter::_invoke_return_entry[i]          = Interpreter::_invoke_return_entry[itos];
      Interpreter::_invokeinterface_return_entry[i] = Interpreter::_invokeinterface_return_entry[itos];
      Interpreter::_invokedynamic_return_entry[i]   = Interpreter::_invokedynamic_return_entry[itos];
    }
  }

  { CodeletMark cm(_masm, "earlyret entry points");
    Interpreter::_earlyret_entry =
      EntryPoint(
                 generate_earlyret_entry_for(atos),
                 generate_earlyret_entry_for(itos),
                 generate_earlyret_entry_for(ltos),
                 generate_earlyret_entry_for(ftos),
                 generate_earlyret_entry_for(dtos),
                 generate_earlyret_entry_for(vtos)
                 );
  }

  { CodeletMark cm(_masm, "result handlers for native calls");
    // The various result converter stublets.
    int is_generated[Interpreter::number_of_result_handlers];
    memset(is_generated, 0, sizeof(is_generated));

    for (int i = 0; i < Interpreter::number_of_result_handlers; i++) {
      BasicType type = types[i];
      if (!is_generated[Interpreter::BasicType_as_index(type)]++) {
        Interpreter::_native_abi_to_tosca[Interpreter::BasicType_as_index(type)] = generate_result_handler_for(type);
      }
    }
  }


  { CodeletMark cm(_masm, "safepoint entry points");
    Interpreter::_safept_entry =
      EntryPoint(
                 generate_safept_entry_for(atos, CAST_FROM_FN_PTR(address, InterpreterRuntime::at_safepoint)),
                 generate_safept_entry_for(itos, CAST_FROM_FN_PTR(address, InterpreterRuntime::at_safepoint)),
                 generate_safept_entry_for(ltos, CAST_FROM_FN_PTR(address, InterpreterRuntime::at_safepoint)),
                 generate_safept_entry_for(ftos, CAST_FROM_FN_PTR(address, InterpreterRuntime::at_safepoint)),
                 generate_safept_entry_for(dtos, CAST_FROM_FN_PTR(address, InterpreterRuntime::at_safepoint)),
                 generate_safept_entry_for(vtos, CAST_FROM_FN_PTR(address, InterpreterRuntime::at_safepoint))
                 );
  }

  { CodeletMark cm(_masm, "exception handling");
    // (Note: this is not safepoint safe because thread may return to compiled code)
    generate_throw_exception();
  }

  { CodeletMark cm(_masm, "throw exception entrypoints");
    Interpreter::_throw_ArrayIndexOutOfBoundsException_entry = generate_ArrayIndexOutOfBounds_handler();
    Interpreter::_throw_ArrayStoreException_entry            = generate_klass_exception_handler("java/lang/ArrayStoreException");
    Interpreter::_throw_ArithmeticException_entry            = generate_exception_handler("java/lang/ArithmeticException", "/ by zero");
    Interpreter::_throw_ClassCastException_entry             = generate_ClassCastException_handler();
    Interpreter::_throw_NullPointerException_entry           = generate_exception_handler("java/lang/NullPointerException", NULL);
    Interpreter::_throw_StackOverflowError_entry             = generate_StackOverflowError_handler();
  }



#define method_entry(kind)                                              \
  { CodeletMark cm(_masm, "method entry point (kind = " #kind ")"); \
    Interpreter::_entry_table[Interpreter::kind] = generate_method_entry(Interpreter::kind); \
  }

  // all non-native method kinds
  method_entry(zerolocals)
  method_entry(zerolocals_synchronized)
  method_entry(empty)
  method_entry(getter)
  method_entry(setter)
  method_entry(abstract)
  method_entry(java_lang_math_sin  )
  method_entry(java_lang_math_cos  )
  method_entry(java_lang_math_tan  )
  method_entry(java_lang_math_abs  )
  method_entry(java_lang_math_sqrt )
  method_entry(java_lang_math_log  )
  method_entry(java_lang_math_log10)
  method_entry(java_lang_math_exp  )
  method_entry(java_lang_math_pow  )
  method_entry(java_lang_math_fmaF )
  method_entry(java_lang_math_fmaD )
  method_entry(java_lang_ref_reference_get)

  AbstractInterpreter::initialize_method_handle_entries();

  // all native method kinds (must be one contiguous block)
  Interpreter::_native_entry_begin = Interpreter::code()->code_end();
  method_entry(native)
  method_entry(native_synchronized)
  Interpreter::_native_entry_end = Interpreter::code()->code_end();

  method_entry(java_util_zip_CRC32_update)
  method_entry(java_util_zip_CRC32_updateBytes)
  method_entry(java_util_zip_CRC32_updateByteBuffer)
  method_entry(java_util_zip_CRC32C_updateBytes)
  method_entry(java_util_zip_CRC32C_updateDirectByteBuffer)

  method_entry(java_lang_Float_intBitsToFloat);
  method_entry(java_lang_Float_floatToRawIntBits);
  method_entry(java_lang_Double_longBitsToDouble);
  method_entry(java_lang_Double_doubleToRawLongBits);

#undef method_entry

  // Bytecodes
  set_entry_points_for_all_bytes();

  // installation of code in other places in the runtime
  // (ExcutableCodeManager calls not needed to copy the entries)
  set_safepoints_for_all_bytes();

  { CodeletMark cm(_masm, "deoptimization entry points");
    Interpreter::_deopt_entry[0] = EntryPoint();
    Interpreter::_deopt_entry[0].set_entry(vtos, generate_deopt_entry_for(vtos, 0));
    for (int i = 1; i < Interpreter::number_of_deopt_entries; i++) {
      Interpreter::_deopt_entry[i] =
        EntryPoint(
                   generate_deopt_entry_for(atos, i),
                   generate_deopt_entry_for(itos, i),
                   generate_deopt_entry_for(ltos, i),
                   generate_deopt_entry_for(ftos, i),
                   generate_deopt_entry_for(dtos, i),
                   generate_deopt_entry_for(vtos, i)
                   );
    }
    address return_continuation = Interpreter::_normal_table.entry(Bytecodes::_return).entry(vtos);
    vmassert(return_continuation != NULL, "return entry not generated yet");
    Interpreter::_deopt_reexecute_return_entry = generate_deopt_entry_for(vtos, 0, return_continuation);
  }

}

//------------------------------------------------------------------------------------------------------------------------

address TemplateInterpreterGenerator::generate_error_exit(const char* msg) {
  address entry = __ pc();
  __ stop(msg);
  return entry;
}


//------------------------------------------------------------------------------------------------------------------------

void TemplateInterpreterGenerator::set_entry_points_for_all_bytes() {
  for (int i = 0; i < DispatchTable::length; i++) {
    Bytecodes::Code code = (Bytecodes::Code)i;
    if (Bytecodes::is_defined(code)) {
      set_entry_points(code);
    } else {
      set_unimplemented(i);
    }
  }
}


void TemplateInterpreterGenerator::set_safepoints_for_all_bytes() {
  for (int i = 0; i < DispatchTable::length; i++) {
    Bytecodes::Code code = (Bytecodes::Code)i;
    if (Bytecodes::is_defined(code)) Interpreter::_safept_table.set_entry(code, Interpreter::_safept_entry);
  }
}


void TemplateInterpreterGenerator::set_unimplemented(int i) {
  address e = _unimplemented_bytecode;
  EntryPoint entry(e, e, e, e, e, e, e, e, e, e);
  Interpreter::_normal_table.set_entry(i, entry);
  Interpreter::_wentry_point[i] = _unimplemented_bytecode;
}


void TemplateInterpreterGenerator::set_entry_points(Bytecodes::Code code) {
  CodeletMark cm(_masm, Bytecodes::name(code), code);
  // initialize entry points
  assert(_unimplemented_bytecode    != NULL, "should have been generated before");
  assert(_illegal_bytecode_sequence != NULL, "should have been generated before");
  address bep = _illegal_bytecode_sequence;
  address zep = _illegal_bytecode_sequence;
  address cep = _illegal_bytecode_sequence;
  address sep = _illegal_bytecode_sequence;
  address aep = _illegal_bytecode_sequence;
  address iep = _illegal_bytecode_sequence;
  address lep = _illegal_bytecode_sequence;
  address fep = _illegal_bytecode_sequence;
  address dep = _illegal_bytecode_sequence;
  address vep = _unimplemented_bytecode;
  address wep = _unimplemented_bytecode;
  // code for short & wide version of bytecode
  if (Bytecodes::is_defined(code)) {
    Template* t = TemplateTable::template_for(code);
    assert(t->is_valid(), "just checking");
    set_short_entry_points(t, bep, cep, sep, aep, iep, lep, fep, dep, vep);
  }
  if (Bytecodes::wide_is_defined(code)) {
    Template* t = TemplateTable::template_for_wide(code);
    assert(t->is_valid(), "just checking");
    set_wide_entry_point(t, wep);
  }
  // set entry points
  EntryPoint entry(bep, zep, cep, sep, aep, iep, lep, fep, dep, vep);
  Interpreter::_normal_table.set_entry(code, entry);
  Interpreter::_wentry_point[code] = wep;
}


void TemplateInterpreterGenerator::set_wide_entry_point(Template* t, address& wep) {
  assert(t->is_valid(), "template must exist");
  assert(t->tos_in() == vtos, "only vtos tos_in supported for wide instructions");
  wep = __ pc(); generate_and_dispatch(t);
}


void TemplateInterpreterGenerator::set_short_entry_points(Template* t, address& bep, address& cep, address& sep, address& aep, address& iep, address& lep, address& fep, address& dep, address& vep) {
  assert(t->is_valid(), "template must exist");
  switch (t->tos_in()) {
    case btos:
    case ztos:
    case ctos:
    case stos:
      ShouldNotReachHere();  // btos/ctos/stos should use itos.
      break;
    case atos: vep = __ pc(); __ pop(atos); aep = __ pc(); generate_and_dispatch(t); break;
    case itos: vep = __ pc(); __ pop(itos); iep = __ pc(); generate_and_dispatch(t); break;
    case ltos: vep = __ pc(); __ pop(ltos); lep = __ pc(); generate_and_dispatch(t); break;
    case ftos: vep = __ pc(); __ pop(ftos); fep = __ pc(); generate_and_dispatch(t); break;
    case dtos: vep = __ pc(); __ pop(dtos); dep = __ pc(); generate_and_dispatch(t); break;
    case vtos: set_vtos_entry_points(t, bep, cep, sep, aep, iep, lep, fep, dep, vep);     break;
    default  : ShouldNotReachHere();                                                 break;
  }
}


//------------------------------------------------------------------------------------------------------------------------

void TemplateInterpreterGenerator::generate_and_dispatch(Template* t, TosState tos_out) {
  if (PrintBytecodeHistogram)                                    histogram_bytecode(t);
#ifndef PRODUCT
  // debugging code
  if (CountBytecodes || TraceBytecodes || StopInterpreterAt > 0) count_bytecode();
  if (PrintBytecodePairHistogram)                                histogram_bytecode_pair(t);
  if (TraceBytecodes)                                            trace_bytecode(t);
  if (StopInterpreterAt > 0)                                     stop_interpreter_at();
  __ verify_FPU(1, t->tos_in());
#endif // !PRODUCT
  int step = 0;
  if (!t->does_dispatch()) {
    step = t->is_wide() ? Bytecodes::wide_length_for(t->bytecode()) : Bytecodes::length_for(t->bytecode());
    if (tos_out == ilgl) tos_out = t->tos_out();
    // compute bytecode size
    assert(step > 0, "just checkin'");
    // setup stuff for dispatching next bytecode
    if (ProfileInterpreter && VerifyDataPointer
        && MethodData::bytecode_has_profile(t->bytecode())) {
      __ verify_method_data_pointer();
    }
    __ dispatch_prolog(tos_out, step);
  }
  // generate template
  t->generate(_masm);
  // advance
  if (t->does_dispatch()) {
#ifdef ASSERT
    // make sure execution doesn't go beyond this point if code is broken
    __ should_not_reach_here();
#endif // ASSERT
  } else {
    // dispatch to next bytecode
    __ dispatch_epilog(tos_out, step);
  }
}

// Generate method entries
address TemplateInterpreterGenerator::generate_method_entry(
                                        AbstractInterpreter::MethodKind kind) {
  // determine code generation flags
  bool native = false;
  bool synchronized = false;
  address entry_point = NULL;

  switch (kind) {
  case Interpreter::zerolocals             :                                          break;
  case Interpreter::zerolocals_synchronized:                synchronized = true;      break;
  case Interpreter::native                 : native = true;                           break;
  case Interpreter::native_synchronized    : native = true; synchronized = true;      break;
  case Interpreter::empty                  : break;
  case Interpreter::getter                 : break;
  case Interpreter::setter                 : break;
  case Interpreter::abstract               : entry_point = generate_abstract_entry(); break;

  case Interpreter::java_lang_math_sin     : // fall thru
  case Interpreter::java_lang_math_cos     : // fall thru
  case Interpreter::java_lang_math_tan     : // fall thru
  case Interpreter::java_lang_math_abs     : // fall thru
  case Interpreter::java_lang_math_log     : // fall thru
  case Interpreter::java_lang_math_log10   : // fall thru
  case Interpreter::java_lang_math_sqrt    : // fall thru
  case Interpreter::java_lang_math_pow     : // fall thru
  case Interpreter::java_lang_math_exp     : // fall thru
  case Interpreter::java_lang_math_fmaD    : // fall thru
  case Interpreter::java_lang_math_fmaF    : entry_point = generate_math_entry(kind);      break;
  case Interpreter::java_lang_ref_reference_get
                                           : entry_point = generate_Reference_get_entry(); break;
  case Interpreter::java_util_zip_CRC32_update
                                           : native = true; entry_point = generate_CRC32_update_entry();  break;
  case Interpreter::java_util_zip_CRC32_updateBytes
                                           : // fall thru
  case Interpreter::java_util_zip_CRC32_updateByteBuffer
                                           : native = true; entry_point = generate_CRC32_updateBytes_entry(kind); break;
  case Interpreter::java_util_zip_CRC32C_updateBytes
                                           : // fall thru
  case Interpreter::java_util_zip_CRC32C_updateDirectByteBuffer
                                           : entry_point = generate_CRC32C_updateBytes_entry(kind); break;
#ifdef IA32
  // On x86_32 platforms, a special entry is generated for the following four methods.
  // On other platforms the normal entry is used to enter these methods.
  case Interpreter::java_lang_Float_intBitsToFloat
                                           : native = true; entry_point = generate_Float_intBitsToFloat_entry(); break;
  case Interpreter::java_lang_Float_floatToRawIntBits
                                           : native = true; entry_point = generate_Float_floatToRawIntBits_entry(); break;
  case Interpreter::java_lang_Double_longBitsToDouble
                                           : native = true; entry_point = generate_Double_longBitsToDouble_entry(); break;
  case Interpreter::java_lang_Double_doubleToRawLongBits
                                           : native = true; entry_point = generate_Double_doubleToRawLongBits_entry(); break;
#else
  case Interpreter::java_lang_Float_intBitsToFloat:
  case Interpreter::java_lang_Float_floatToRawIntBits:
  case Interpreter::java_lang_Double_longBitsToDouble:
  case Interpreter::java_lang_Double_doubleToRawLongBits:
    native = true;
    break;
#endif // !IA32
  default:
    fatal("unexpected method kind: %d", kind);
    break;
  }

  if (entry_point) {
    return entry_point;
  }

  // We expect the normal and native entry points to be generated first so we can reuse them.
  if (native) {
    entry_point = Interpreter::entry_for_kind(synchronized ? Interpreter::native_synchronized : Interpreter::native);
    if (entry_point == NULL) {
      entry_point = generate_native_entry(synchronized);
    }
  } else {
    entry_point = Interpreter::entry_for_kind(synchronized ? Interpreter::zerolocals_synchronized : Interpreter::zerolocals);
    if (entry_point == NULL) {
      entry_point = generate_normal_entry(synchronized);
    }
  }

  return entry_point;
}
