/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciMethodData.hpp"
#include "classfile/vmSymbols.hpp"
#include "compiler/compilationPolicy.hpp"
#include "compiler/compilerOracle.hpp"
#include "interpreter/bytecode.hpp"
#include "interpreter/bytecodeStream.hpp"
#include "interpreter/linkResolver.hpp"
#include "memory/metaspaceClosure.hpp"
#include "memory/resourceArea.hpp"
#include "oops/klass.inline.hpp"
#include "oops/methodData.inline.hpp"
#include "prims/jvmtiRedefineClasses.hpp"
#include "runtime/atomic.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/signature.hpp"
#include "utilities/align.hpp"
#include "utilities/copy.hpp"

// ==================================================================
// DataLayout
//
// Overlay for generic profiling data.

// Some types of data layouts need a length field.
bool DataLayout::needs_array_len(u1 tag) {
  return (tag == multi_branch_data_tag) || (tag == arg_info_data_tag) || (tag == parameters_type_data_tag);
}

// Perform generic initialization of the data.  More specific
// initialization occurs in overrides of ProfileData::post_initialize.
void DataLayout::initialize(u1 tag, u2 bci, int cell_count) {
  _header._bits = (intptr_t)0;
  _header._struct._tag = tag;
  _header._struct._bci = bci;
  for (int i = 0; i < cell_count; i++) {
    set_cell_at(i, (intptr_t)0);
  }
  if (needs_array_len(tag)) {
    set_cell_at(ArrayData::array_len_off_set, cell_count - 1); // -1 for header.
  }
  if (tag == call_type_data_tag) {
    CallTypeData::initialize(this, cell_count);
  } else if (tag == virtual_call_type_data_tag) {
    VirtualCallTypeData::initialize(this, cell_count);
  }
}

void DataLayout::clean_weak_klass_links(bool always_clean) {
  ResourceMark m;
  data_in()->clean_weak_klass_links(always_clean);
}


// ==================================================================
// ProfileData
//
// A ProfileData object is created to refer to a section of profiling
// data in a structured way.

// Constructor for invalid ProfileData.
ProfileData::ProfileData() {
  _data = NULL;
}

char* ProfileData::print_data_on_helper(const MethodData* md) const {
  DataLayout* dp  = md->extra_data_base();
  DataLayout* end = md->args_data_limit();
  stringStream ss;
  for (;; dp = MethodData::next_extra(dp)) {
    assert(dp < end, "moved past end of extra data");
    switch(dp->tag()) {
    case DataLayout::speculative_trap_data_tag:
      if (dp->bci() == bci()) {
        SpeculativeTrapData* data = new SpeculativeTrapData(dp);
        int trap = data->trap_state();
        char buf[100];
        ss.print("trap/");
        data->method()->print_short_name(&ss);
        ss.print("(%s) ", Deoptimization::format_trap_state(buf, sizeof(buf), trap));
      }
      break;
    case DataLayout::bit_data_tag:
      break;
    case DataLayout::no_tag:
    case DataLayout::arg_info_data_tag:
      return ss.as_string();
      break;
    default:
      fatal("unexpected tag %d", dp->tag());
    }
  }
  return NULL;
}

void ProfileData::print_data_on(outputStream* st, const MethodData* md) const {
  print_data_on(st, print_data_on_helper(md));
}

void ProfileData::print_shared(outputStream* st, const char* name, const char* extra) const {
  st->print("bci: %d", bci());
  st->fill_to(tab_width_one);
  st->print("%s", name);
  tab(st);
  int trap = trap_state();
  if (trap != 0) {
    char buf[100];
    st->print("trap(%s) ", Deoptimization::format_trap_state(buf, sizeof(buf), trap));
  }
  if (extra != NULL) {
    st->print("%s", extra);
  }
  int flags = data()->flags();
  if (flags != 0) {
    st->print("flags(%d) ", flags);
  }
}

void ProfileData::tab(outputStream* st, bool first) const {
  st->fill_to(first ? tab_width_one : tab_width_two);
}

// ==================================================================
// BitData
//
// A BitData corresponds to a one-bit flag.  This is used to indicate
// whether a checkcast bytecode has seen a null value.


void BitData::print_data_on(outputStream* st, const char* extra) const {
  print_shared(st, "BitData", extra);
  st->cr();
}

// ==================================================================
// CounterData
//
// A CounterData corresponds to a simple counter.

void CounterData::print_data_on(outputStream* st, const char* extra) const {
  print_shared(st, "CounterData", extra);
  st->print_cr("count(%u)", count());
}

// ==================================================================
// JumpData
//
// A JumpData is used to access profiling information for a direct
// branch.  It is a counter, used for counting the number of branches,
// plus a data displacement, used for realigning the data pointer to
// the corresponding target bci.

void JumpData::post_initialize(BytecodeStream* stream, MethodData* mdo) {
  assert(stream->bci() == bci(), "wrong pos");
  int target;
  Bytecodes::Code c = stream->code();
  if (c == Bytecodes::_goto_w || c == Bytecodes::_jsr_w) {
    target = stream->dest_w();
  } else {
    target = stream->dest();
  }
  int my_di = mdo->dp_to_di(dp());
  int target_di = mdo->bci_to_di(target);
  int offset = target_di - my_di;
  set_displacement(offset);
}

void JumpData::print_data_on(outputStream* st, const char* extra) const {
  print_shared(st, "JumpData", extra);
  st->print_cr("taken(%u) displacement(%d)", taken(), displacement());
}

int TypeStackSlotEntries::compute_cell_count(Symbol* signature, bool include_receiver, int max) {
  // Parameter profiling include the receiver
  int args_count = include_receiver ? 1 : 0;
  ResourceMark rm;
  ReferenceArgumentCount rac(signature);
  args_count += rac.count();
  args_count = MIN2(args_count, max);
  return args_count * per_arg_cell_count;
}

int TypeEntriesAtCall::compute_cell_count(BytecodeStream* stream) {
  assert(Bytecodes::is_invoke(stream->code()), "should be invoke");
  assert(TypeStackSlotEntries::per_arg_count() > ReturnTypeEntry::static_cell_count(), "code to test for arguments/results broken");
  const methodHandle m = stream->method();
  int bci = stream->bci();
  Bytecode_invoke inv(m, bci);
  int args_cell = 0;
  if (MethodData::profile_arguments_for_invoke(m, bci)) {
    args_cell = TypeStackSlotEntries::compute_cell_count(inv.signature(), false, TypeProfileArgsLimit);
  }
  int ret_cell = 0;
  if (MethodData::profile_return_for_invoke(m, bci) && is_reference_type(inv.result_type())) {
    ret_cell = ReturnTypeEntry::static_cell_count();
  }
  int header_cell = 0;
  if (args_cell + ret_cell > 0) {
    header_cell = header_cell_count();
  }

  return header_cell + args_cell + ret_cell;
}

class ArgumentOffsetComputer : public SignatureIterator {
private:
  int _max;
  int _offset;
  GrowableArray<int> _offsets;

  friend class SignatureIterator;  // so do_parameters_on can call do_type
  void do_type(BasicType type) {
    if (is_reference_type(type) && _offsets.length() < _max) {
      _offsets.push(_offset);
    }
    _offset += parameter_type_word_count(type);
  }

 public:
  ArgumentOffsetComputer(Symbol* signature, int max)
    : SignatureIterator(signature),
      _max(max), _offset(0),
      _offsets(max) {
    do_parameters_on(this);  // non-virtual template execution
  }

  int off_at(int i) const { return _offsets.at(i); }
};

void TypeStackSlotEntries::post_initialize(Symbol* signature, bool has_receiver, bool include_receiver) {
  ResourceMark rm;
  int start = 0;
  // Parameter profiling include the receiver
  if (include_receiver && has_receiver) {
    set_stack_slot(0, 0);
    set_type(0, type_none());
    start += 1;
  }
  ArgumentOffsetComputer aos(signature, _number_of_entries-start);
  for (int i = start; i < _number_of_entries; i++) {
    set_stack_slot(i, aos.off_at(i-start) + (has_receiver ? 1 : 0));
    set_type(i, type_none());
  }
}

void CallTypeData::post_initialize(BytecodeStream* stream, MethodData* mdo) {
  assert(Bytecodes::is_invoke(stream->code()), "should be invoke");
  Bytecode_invoke inv(stream->method(), stream->bci());

  if (has_arguments()) {
#ifdef ASSERT
    ResourceMark rm;
    ReferenceArgumentCount rac(inv.signature());
    int count = MIN2(rac.count(), (int)TypeProfileArgsLimit);
    assert(count > 0, "room for args type but none found?");
    check_number_of_arguments(count);
#endif
    _args.post_initialize(inv.signature(), inv.has_receiver(), false);
  }

  if (has_return()) {
    assert(is_reference_type(inv.result_type()), "room for a ret type but doesn't return obj?");
    _ret.post_initialize();
  }
}

void VirtualCallTypeData::post_initialize(BytecodeStream* stream, MethodData* mdo) {
  assert(Bytecodes::is_invoke(stream->code()), "should be invoke");
  Bytecode_invoke inv(stream->method(), stream->bci());

  if (has_arguments()) {
#ifdef ASSERT
    ResourceMark rm;
    ReferenceArgumentCount rac(inv.signature());
    int count = MIN2(rac.count(), (int)TypeProfileArgsLimit);
    assert(count > 0, "room for args type but none found?");
    check_number_of_arguments(count);
#endif
    _args.post_initialize(inv.signature(), inv.has_receiver(), false);
  }

  if (has_return()) {
    assert(is_reference_type(inv.result_type()), "room for a ret type but doesn't return obj?");
    _ret.post_initialize();
  }
}

void TypeStackSlotEntries::clean_weak_klass_links(bool always_clean) {
  for (int i = 0; i < _number_of_entries; i++) {
    intptr_t p = type(i);
    Klass* k = (Klass*)klass_part(p);
    if (k != NULL && (always_clean || !k->is_loader_alive())) {
      set_type(i, with_status((Klass*)NULL, p));
    }
  }
}

void ReturnTypeEntry::clean_weak_klass_links(bool always_clean) {
  intptr_t p = type();
  Klass* k = (Klass*)klass_part(p);
  if (k != NULL && (always_clean || !k->is_loader_alive())) {
    set_type(with_status((Klass*)NULL, p));
  }
}

bool TypeEntriesAtCall::return_profiling_enabled() {
  return MethodData::profile_return();
}

bool TypeEntriesAtCall::arguments_profiling_enabled() {
  return MethodData::profile_arguments();
}

void TypeEntries::print_klass(outputStream* st, intptr_t k) {
  if (is_type_none(k)) {
    st->print("none");
  } else if (is_type_unknown(k)) {
    st->print("unknown");
  } else {
    valid_klass(k)->print_value_on(st);
  }
  if (was_null_seen(k)) {
    st->print(" (null seen)");
  }
}

void TypeStackSlotEntries::print_data_on(outputStream* st) const {
  for (int i = 0; i < _number_of_entries; i++) {
    _pd->tab(st);
    st->print("%d: stack(%u) ", i, stack_slot(i));
    print_klass(st, type(i));
    st->cr();
  }
}

void ReturnTypeEntry::print_data_on(outputStream* st) const {
  _pd->tab(st);
  print_klass(st, type());
  st->cr();
}

void CallTypeData::print_data_on(outputStream* st, const char* extra) const {
  CounterData::print_data_on(st, extra);
  if (has_arguments()) {
    tab(st, true);
    st->print("argument types");
    _args.print_data_on(st);
  }
  if (has_return()) {
    tab(st, true);
    st->print("return type");
    _ret.print_data_on(st);
  }
}

void VirtualCallTypeData::print_data_on(outputStream* st, const char* extra) const {
  VirtualCallData::print_data_on(st, extra);
  if (has_arguments()) {
    tab(st, true);
    st->print("argument types");
    _args.print_data_on(st);
  }
  if (has_return()) {
    tab(st, true);
    st->print("return type");
    _ret.print_data_on(st);
  }
}

// ==================================================================
// ReceiverTypeData
//
// A ReceiverTypeData is used to access profiling information about a
// dynamic type check.  It consists of a counter which counts the total times
// that the check is reached, and a series of (Klass*, count) pairs
// which are used to store a type profile for the receiver of the check.

void ReceiverTypeData::clean_weak_klass_links(bool always_clean) {
    for (uint row = 0; row < row_limit(); row++) {
    Klass* p = receiver(row);
    if (p != NULL && (always_clean || !p->is_loader_alive())) {
      clear_row(row);
    }
  }
}

void ReceiverTypeData::print_receiver_data_on(outputStream* st) const {
  uint row;
  int entries = 0;
  for (row = 0; row < row_limit(); row++) {
    if (receiver(row) != NULL)  entries++;
  }
#if INCLUDE_JVMCI
  st->print_cr("count(%u) nonprofiled_count(%u) entries(%u)", count(), nonprofiled_count(), entries);
#else
  st->print_cr("count(%u) entries(%u)", count(), entries);
#endif
  int total = count();
  for (row = 0; row < row_limit(); row++) {
    if (receiver(row) != NULL) {
      total += receiver_count(row);
    }
  }
  for (row = 0; row < row_limit(); row++) {
    if (receiver(row) != NULL) {
      tab(st);
      receiver(row)->print_value_on(st);
      st->print_cr("(%u %4.2f)", receiver_count(row), (float) receiver_count(row) / (float) total);
    }
  }
}
void ReceiverTypeData::print_data_on(outputStream* st, const char* extra) const {
  print_shared(st, "ReceiverTypeData", extra);
  print_receiver_data_on(st);
}

void VirtualCallData::print_data_on(outputStream* st, const char* extra) const {
  print_shared(st, "VirtualCallData", extra);
  print_receiver_data_on(st);
}

// ==================================================================
// RetData
//
// A RetData is used to access profiling information for a ret bytecode.
// It is composed of a count of the number of times that the ret has
// been executed, followed by a series of triples of the form
// (bci, count, di) which count the number of times that some bci was the
// target of the ret and cache a corresponding displacement.

void RetData::post_initialize(BytecodeStream* stream, MethodData* mdo) {
  for (uint row = 0; row < row_limit(); row++) {
    set_bci_displacement(row, -1);
    set_bci(row, no_bci);
  }
  // release so other threads see a consistent state.  bci is used as
  // a valid flag for bci_displacement.
  OrderAccess::release();
}

// This routine needs to atomically update the RetData structure, so the
// caller needs to hold the RetData_lock before it gets here.  Since taking
// the lock can block (and allow GC) and since RetData is a ProfileData is a
// wrapper around a derived oop, taking the lock in _this_ method will
// basically cause the 'this' pointer's _data field to contain junk after the
// lock.  We require the caller to take the lock before making the ProfileData
// structure.  Currently the only caller is InterpreterRuntime::update_mdp_for_ret
address RetData::fixup_ret(int return_bci, MethodData* h_mdo) {
  // First find the mdp which corresponds to the return bci.
  address mdp = h_mdo->bci_to_dp(return_bci);

  // Now check to see if any of the cache slots are open.
  for (uint row = 0; row < row_limit(); row++) {
    if (bci(row) == no_bci) {
      set_bci_displacement(row, mdp - dp());
      set_bci_count(row, DataLayout::counter_increment);
      // Barrier to ensure displacement is written before the bci; allows
      // the interpreter to read displacement without fear of race condition.
      release_set_bci(row, return_bci);
      break;
    }
  }
  return mdp;
}

void RetData::print_data_on(outputStream* st, const char* extra) const {
  print_shared(st, "RetData", extra);
  uint row;
  int entries = 0;
  for (row = 0; row < row_limit(); row++) {
    if (bci(row) != no_bci)  entries++;
  }
  st->print_cr("count(%u) entries(%u)", count(), entries);
  for (row = 0; row < row_limit(); row++) {
    if (bci(row) != no_bci) {
      tab(st);
      st->print_cr("bci(%d: count(%u) displacement(%d))",
                   bci(row), bci_count(row), bci_displacement(row));
    }
  }
}

// ==================================================================
// BranchData
//
// A BranchData is used to access profiling data for a two-way branch.
// It consists of taken and not_taken counts as well as a data displacement
// for the taken case.

void BranchData::post_initialize(BytecodeStream* stream, MethodData* mdo) {
  assert(stream->bci() == bci(), "wrong pos");
  int target = stream->dest();
  int my_di = mdo->dp_to_di(dp());
  int target_di = mdo->bci_to_di(target);
  int offset = target_di - my_di;
  set_displacement(offset);
}

void BranchData::print_data_on(outputStream* st, const char* extra) const {
  print_shared(st, "BranchData", extra);
  st->print_cr("taken(%u) displacement(%d)",
               taken(), displacement());
  tab(st);
  st->print_cr("not taken(%u)", not_taken());
}

// ==================================================================
// MultiBranchData
//
// A MultiBranchData is used to access profiling information for
// a multi-way branch (*switch bytecodes).  It consists of a series
// of (count, displacement) pairs, which count the number of times each
// case was taken and specify the data displacment for each branch target.

int MultiBranchData::compute_cell_count(BytecodeStream* stream) {
  int cell_count = 0;
  if (stream->code() == Bytecodes::_tableswitch) {
    Bytecode_tableswitch sw(stream->method()(), stream->bcp());
    cell_count = 1 + per_case_cell_count * (1 + sw.length()); // 1 for default
  } else {
    Bytecode_lookupswitch sw(stream->method()(), stream->bcp());
    cell_count = 1 + per_case_cell_count * (sw.number_of_pairs() + 1); // 1 for default
  }
  return cell_count;
}

void MultiBranchData::post_initialize(BytecodeStream* stream,
                                      MethodData* mdo) {
  assert(stream->bci() == bci(), "wrong pos");
  int target;
  int my_di;
  int target_di;
  int offset;
  if (stream->code() == Bytecodes::_tableswitch) {
    Bytecode_tableswitch sw(stream->method()(), stream->bcp());
    int len = sw.length();
    assert(array_len() == per_case_cell_count * (len + 1), "wrong len");
    for (int count = 0; count < len; count++) {
      target = sw.dest_offset_at(count) + bci();
      my_di = mdo->dp_to_di(dp());
      target_di = mdo->bci_to_di(target);
      offset = target_di - my_di;
      set_displacement_at(count, offset);
    }
    target = sw.default_offset() + bci();
    my_di = mdo->dp_to_di(dp());
    target_di = mdo->bci_to_di(target);
    offset = target_di - my_di;
    set_default_displacement(offset);

  } else {
    Bytecode_lookupswitch sw(stream->method()(), stream->bcp());
    int npairs = sw.number_of_pairs();
    assert(array_len() == per_case_cell_count * (npairs + 1), "wrong len");
    for (int count = 0; count < npairs; count++) {
      LookupswitchPair pair = sw.pair_at(count);
      target = pair.offset() + bci();
      my_di = mdo->dp_to_di(dp());
      target_di = mdo->bci_to_di(target);
      offset = target_di - my_di;
      set_displacement_at(count, offset);
    }
    target = sw.default_offset() + bci();
    my_di = mdo->dp_to_di(dp());
    target_di = mdo->bci_to_di(target);
    offset = target_di - my_di;
    set_default_displacement(offset);
  }
}

void MultiBranchData::print_data_on(outputStream* st, const char* extra) const {
  print_shared(st, "MultiBranchData", extra);
  st->print_cr("default_count(%u) displacement(%d)",
               default_count(), default_displacement());
  int cases = number_of_cases();
  for (int i = 0; i < cases; i++) {
    tab(st);
    st->print_cr("count(%u) displacement(%d)",
                 count_at(i), displacement_at(i));
  }
}

void ArgInfoData::print_data_on(outputStream* st, const char* extra) const {
  print_shared(st, "ArgInfoData", extra);
  int nargs = number_of_args();
  for (int i = 0; i < nargs; i++) {
    st->print("  0x%x", arg_modified(i));
  }
  st->cr();
}

int ParametersTypeData::compute_cell_count(Method* m) {
  if (!MethodData::profile_parameters_for_method(methodHandle(Thread::current(), m))) {
    return 0;
  }
  int max = TypeProfileParmsLimit == -1 ? INT_MAX : TypeProfileParmsLimit;
  int obj_args = TypeStackSlotEntries::compute_cell_count(m->signature(), !m->is_static(), max);
  if (obj_args > 0) {
    return obj_args + 1; // 1 cell for array len
  }
  return 0;
}

void ParametersTypeData::post_initialize(BytecodeStream* stream, MethodData* mdo) {
  _parameters.post_initialize(mdo->method()->signature(), !mdo->method()->is_static(), true);
}

bool ParametersTypeData::profiling_enabled() {
  return MethodData::profile_parameters();
}

void ParametersTypeData::print_data_on(outputStream* st, const char* extra) const {
  st->print("parameter types"); // FIXME extra ignored?
  _parameters.print_data_on(st);
}

void SpeculativeTrapData::print_data_on(outputStream* st, const char* extra) const {
  print_shared(st, "SpeculativeTrapData", extra);
  tab(st);
  method()->print_short_name(st);
  st->cr();
}

// ==================================================================
// MethodData*
//
// A MethodData* holds information which has been collected about
// a method.

MethodData* MethodData::allocate(ClassLoaderData* loader_data, const methodHandle& method, TRAPS) {
  int size = MethodData::compute_allocation_size_in_words(method);

  return new (loader_data, size, MetaspaceObj::MethodDataType, THREAD)
    MethodData(method);
}

int MethodData::bytecode_cell_count(Bytecodes::Code code) {
  if (CompilerConfig::is_c1_simple_only() && !ProfileInterpreter) {
    return no_profile_data;
  }
  switch (code) {
  case Bytecodes::_checkcast:
  case Bytecodes::_instanceof:
  case Bytecodes::_aastore:
    if (TypeProfileCasts) {
      return ReceiverTypeData::static_cell_count();
    } else {
      return BitData::static_cell_count();
    }
  case Bytecodes::_invokespecial:
  case Bytecodes::_invokestatic:
    if (MethodData::profile_arguments() || MethodData::profile_return()) {
      return variable_cell_count;
    } else {
      return CounterData::static_cell_count();
    }
  case Bytecodes::_goto:
  case Bytecodes::_goto_w:
  case Bytecodes::_jsr:
  case Bytecodes::_jsr_w:
    return JumpData::static_cell_count();
  case Bytecodes::_invokevirtual:
  case Bytecodes::_invokeinterface:
    if (MethodData::profile_arguments() || MethodData::profile_return()) {
      return variable_cell_count;
    } else {
      return VirtualCallData::static_cell_count();
    }
  case Bytecodes::_invokedynamic:
    if (MethodData::profile_arguments() || MethodData::profile_return()) {
      return variable_cell_count;
    } else {
      return CounterData::static_cell_count();
    }
  case Bytecodes::_ret:
    return RetData::static_cell_count();
  case Bytecodes::_ifeq:
  case Bytecodes::_ifne:
  case Bytecodes::_iflt:
  case Bytecodes::_ifge:
  case Bytecodes::_ifgt:
  case Bytecodes::_ifle:
  case Bytecodes::_if_icmpeq:
  case Bytecodes::_if_icmpne:
  case Bytecodes::_if_icmplt:
  case Bytecodes::_if_icmpge:
  case Bytecodes::_if_icmpgt:
  case Bytecodes::_if_icmple:
  case Bytecodes::_if_acmpeq:
  case Bytecodes::_if_acmpne:
  case Bytecodes::_ifnull:
  case Bytecodes::_ifnonnull:
    return BranchData::static_cell_count();
  case Bytecodes::_lookupswitch:
  case Bytecodes::_tableswitch:
    return variable_cell_count;
  default:
    return no_profile_data;
  }
}

// Compute the size of the profiling information corresponding to
// the current bytecode.
int MethodData::compute_data_size(BytecodeStream* stream) {
  int cell_count = bytecode_cell_count(stream->code());
  if (cell_count == no_profile_data) {
    return 0;
  }
  if (cell_count == variable_cell_count) {
    switch (stream->code()) {
    case Bytecodes::_lookupswitch:
    case Bytecodes::_tableswitch:
      cell_count = MultiBranchData::compute_cell_count(stream);
      break;
    case Bytecodes::_invokespecial:
    case Bytecodes::_invokestatic:
    case Bytecodes::_invokedynamic:
      assert(MethodData::profile_arguments() || MethodData::profile_return(), "should be collecting args profile");
      if (profile_arguments_for_invoke(stream->method(), stream->bci()) ||
          profile_return_for_invoke(stream->method(), stream->bci())) {
        cell_count = CallTypeData::compute_cell_count(stream);
      } else {
        cell_count = CounterData::static_cell_count();
      }
      break;
    case Bytecodes::_invokevirtual:
    case Bytecodes::_invokeinterface: {
      assert(MethodData::profile_arguments() || MethodData::profile_return(), "should be collecting args profile");
      if (profile_arguments_for_invoke(stream->method(), stream->bci()) ||
          profile_return_for_invoke(stream->method(), stream->bci())) {
        cell_count = VirtualCallTypeData::compute_cell_count(stream);
      } else {
        cell_count = VirtualCallData::static_cell_count();
      }
      break;
    }
    default:
      fatal("unexpected bytecode for var length profile data");
    }
  }
  // Note:  cell_count might be zero, meaning that there is just
  //        a DataLayout header, with no extra cells.
  assert(cell_count >= 0, "sanity");
  return DataLayout::compute_size_in_bytes(cell_count);
}

bool MethodData::is_speculative_trap_bytecode(Bytecodes::Code code) {
  // Bytecodes for which we may use speculation
  switch (code) {
  case Bytecodes::_checkcast:
  case Bytecodes::_instanceof:
  case Bytecodes::_aastore:
  case Bytecodes::_invokevirtual:
  case Bytecodes::_invokeinterface:
  case Bytecodes::_if_acmpeq:
  case Bytecodes::_if_acmpne:
  case Bytecodes::_ifnull:
  case Bytecodes::_ifnonnull:
  case Bytecodes::_invokestatic:
#ifdef COMPILER2
    if (CompilerConfig::is_c2_enabled()) {
      return UseTypeSpeculation;
    }
#endif
  default:
    return false;
  }
  return false;
}

#if INCLUDE_JVMCI

void* FailedSpeculation::operator new(size_t size, size_t fs_size) throw() {
  return CHeapObj<mtCompiler>::operator new(fs_size, std::nothrow);
}

FailedSpeculation::FailedSpeculation(address speculation, int speculation_len) : _data_len(speculation_len), _next(NULL) {
  memcpy(data(), speculation, speculation_len);
}

// A heuristic check to detect nmethods that outlive a failed speculations list.
static void guarantee_failed_speculations_alive(nmethod* nm, FailedSpeculation** failed_speculations_address) {
  jlong head = (jlong)(address) *failed_speculations_address;
  if ((head & 0x1) == 0x1) {
    stringStream st;
    if (nm != NULL) {
      st.print("%d", nm->compile_id());
      Method* method = nm->method();
      st.print_raw("{");
      if (method != NULL) {
        method->print_name(&st);
      } else {
        const char* jvmci_name = nm->jvmci_name();
        if (jvmci_name != NULL) {
          st.print_raw(jvmci_name);
        }
      }
      st.print_raw("}");
    } else {
      st.print("<unknown>");
    }
    fatal("Adding to failed speculations list that appears to have been freed. Source: %s", st.as_string());
  }
}

bool FailedSpeculation::add_failed_speculation(nmethod* nm, FailedSpeculation** failed_speculations_address, address speculation, int speculation_len) {
  assert(failed_speculations_address != NULL, "must be");
  size_t fs_size = sizeof(FailedSpeculation) + speculation_len;
  FailedSpeculation* fs = new (fs_size) FailedSpeculation(speculation, speculation_len);
  if (fs == NULL) {
    // no memory -> ignore failed speculation
    return false;
  }

  guarantee(is_aligned(fs, sizeof(FailedSpeculation*)), "FailedSpeculation objects must be pointer aligned");
  guarantee_failed_speculations_alive(nm, failed_speculations_address);

  FailedSpeculation** cursor = failed_speculations_address;
  do {
    if (*cursor == NULL) {
      FailedSpeculation* old_fs = Atomic::cmpxchg(cursor, (FailedSpeculation*) NULL, fs);
      if (old_fs == NULL) {
        // Successfully appended fs to end of the list
        return true;
      }
      cursor = old_fs->next_adr();
    } else {
      cursor = (*cursor)->next_adr();
    }
  } while (true);
}

void FailedSpeculation::free_failed_speculations(FailedSpeculation** failed_speculations_address) {
  assert(failed_speculations_address != NULL, "must be");
  FailedSpeculation* fs = *failed_speculations_address;
  while (fs != NULL) {
    FailedSpeculation* next = fs->next();
    delete fs;
    fs = next;
  }

  // Write an unaligned value to failed_speculations_address to denote
  // that it is no longer a valid pointer. This is allows for the check
  // in add_failed_speculation against adding to a freed failed
  // speculations list.
  long* head = (long*) failed_speculations_address;
  (*head) = (*head) | 0x1;
}
#endif // INCLUDE_JVMCI

int MethodData::compute_extra_data_count(int data_size, int empty_bc_count, bool needs_speculative_traps) {
#if INCLUDE_JVMCI
  if (ProfileTraps) {
    // Assume that up to 30% of the possibly trapping BCIs with no MDP will need to allocate one.
    int extra_data_count = MIN2(empty_bc_count, MAX2(4, (empty_bc_count * 30) / 100));

    // Make sure we have a minimum number of extra data slots to
    // allocate SpeculativeTrapData entries. We would want to have one
    // entry per compilation that inlines this method and for which
    // some type speculation assumption fails. So the room we need for
    // the SpeculativeTrapData entries doesn't directly depend on the
    // size of the method. Because it's hard to estimate, we reserve
    // space for an arbitrary number of entries.
    int spec_data_count = (needs_speculative_traps ? SpecTrapLimitExtraEntries : 0) *
      (SpeculativeTrapData::static_cell_count() + DataLayout::header_size_in_cells());

    return MAX2(extra_data_count, spec_data_count);
  } else {
    return 0;
  }
#else // INCLUDE_JVMCI
  if (ProfileTraps) {
    // Assume that up to 3% of BCIs with no MDP will need to allocate one.
    int extra_data_count = (uint)(empty_bc_count * 3) / 128 + 1;
    // If the method is large, let the extra BCIs grow numerous (to ~1%).
    int one_percent_of_data
      = (uint)data_size / (DataLayout::header_size_in_bytes()*128);
    if (extra_data_count < one_percent_of_data)
      extra_data_count = one_percent_of_data;
    if (extra_data_count > empty_bc_count)
      extra_data_count = empty_bc_count;  // no need for more

    // Make sure we have a minimum number of extra data slots to
    // allocate SpeculativeTrapData entries. We would want to have one
    // entry per compilation that inlines this method and for which
    // some type speculation assumption fails. So the room we need for
    // the SpeculativeTrapData entries doesn't directly depend on the
    // size of the method. Because it's hard to estimate, we reserve
    // space for an arbitrary number of entries.
    int spec_data_count = (needs_speculative_traps ? SpecTrapLimitExtraEntries : 0) *
      (SpeculativeTrapData::static_cell_count() + DataLayout::header_size_in_cells());

    return MAX2(extra_data_count, spec_data_count);
  } else {
    return 0;
  }
#endif // INCLUDE_JVMCI
}

// Compute the size of the MethodData* necessary to store
// profiling information about a given method.  Size is in bytes.
int MethodData::compute_allocation_size_in_bytes(const methodHandle& method) {
  int data_size = 0;
  BytecodeStream stream(method);
  Bytecodes::Code c;
  int empty_bc_count = 0;  // number of bytecodes lacking data
  bool needs_speculative_traps = false;
  while ((c = stream.next()) >= 0) {
    int size_in_bytes = compute_data_size(&stream);
    data_size += size_in_bytes;
    if (size_in_bytes == 0 JVMCI_ONLY(&& Bytecodes::can_trap(c)))  empty_bc_count += 1;
    needs_speculative_traps = needs_speculative_traps || is_speculative_trap_bytecode(c);
  }
  int object_size = in_bytes(data_offset()) + data_size;

  // Add some extra DataLayout cells (at least one) to track stray traps.
  int extra_data_count = compute_extra_data_count(data_size, empty_bc_count, needs_speculative_traps);
  object_size += extra_data_count * DataLayout::compute_size_in_bytes(0);

  // Add a cell to record information about modified arguments.
  int arg_size = method->size_of_parameters();
  object_size += DataLayout::compute_size_in_bytes(arg_size+1);

  // Reserve room for an area of the MDO dedicated to profiling of
  // parameters
  int args_cell = ParametersTypeData::compute_cell_count(method());
  if (args_cell > 0) {
    object_size += DataLayout::compute_size_in_bytes(args_cell);
  }
  return object_size;
}

// Compute the size of the MethodData* necessary to store
// profiling information about a given method.  Size is in words
int MethodData::compute_allocation_size_in_words(const methodHandle& method) {
  int byte_size = compute_allocation_size_in_bytes(method);
  int word_size = align_up(byte_size, BytesPerWord) / BytesPerWord;
  return align_metadata_size(word_size);
}

// Initialize an individual data segment.  Returns the size of
// the segment in bytes.
int MethodData::initialize_data(BytecodeStream* stream,
                                       int data_index) {
  if (CompilerConfig::is_c1_simple_only() && !ProfileInterpreter) {
    return 0;
  }
  int cell_count = -1;
  int tag = DataLayout::no_tag;
  DataLayout* data_layout = data_layout_at(data_index);
  Bytecodes::Code c = stream->code();
  switch (c) {
  case Bytecodes::_checkcast:
  case Bytecodes::_instanceof:
  case Bytecodes::_aastore:
    if (TypeProfileCasts) {
      cell_count = ReceiverTypeData::static_cell_count();
      tag = DataLayout::receiver_type_data_tag;
    } else {
      cell_count = BitData::static_cell_count();
      tag = DataLayout::bit_data_tag;
    }
    break;
  case Bytecodes::_invokespecial:
  case Bytecodes::_invokestatic: {
    int counter_data_cell_count = CounterData::static_cell_count();
    if (profile_arguments_for_invoke(stream->method(), stream->bci()) ||
        profile_return_for_invoke(stream->method(), stream->bci())) {
      cell_count = CallTypeData::compute_cell_count(stream);
    } else {
      cell_count = counter_data_cell_count;
    }
    if (cell_count > counter_data_cell_count) {
      tag = DataLayout::call_type_data_tag;
    } else {
      tag = DataLayout::counter_data_tag;
    }
    break;
  }
  case Bytecodes::_goto:
  case Bytecodes::_goto_w:
  case Bytecodes::_jsr:
  case Bytecodes::_jsr_w:
    cell_count = JumpData::static_cell_count();
    tag = DataLayout::jump_data_tag;
    break;
  case Bytecodes::_invokevirtual:
  case Bytecodes::_invokeinterface: {
    int virtual_call_data_cell_count = VirtualCallData::static_cell_count();
    if (profile_arguments_for_invoke(stream->method(), stream->bci()) ||
        profile_return_for_invoke(stream->method(), stream->bci())) {
      cell_count = VirtualCallTypeData::compute_cell_count(stream);
    } else {
      cell_count = virtual_call_data_cell_count;
    }
    if (cell_count > virtual_call_data_cell_count) {
      tag = DataLayout::virtual_call_type_data_tag;
    } else {
      tag = DataLayout::virtual_call_data_tag;
    }
    break;
  }
  case Bytecodes::_invokedynamic: {
    // %%% should make a type profile for any invokedynamic that takes a ref argument
    int counter_data_cell_count = CounterData::static_cell_count();
    if (profile_arguments_for_invoke(stream->method(), stream->bci()) ||
        profile_return_for_invoke(stream->method(), stream->bci())) {
      cell_count = CallTypeData::compute_cell_count(stream);
    } else {
      cell_count = counter_data_cell_count;
    }
    if (cell_count > counter_data_cell_count) {
      tag = DataLayout::call_type_data_tag;
    } else {
      tag = DataLayout::counter_data_tag;
    }
    break;
  }
  case Bytecodes::_ret:
    cell_count = RetData::static_cell_count();
    tag = DataLayout::ret_data_tag;
    break;
  case Bytecodes::_ifeq:
  case Bytecodes::_ifne:
  case Bytecodes::_iflt:
  case Bytecodes::_ifge:
  case Bytecodes::_ifgt:
  case Bytecodes::_ifle:
  case Bytecodes::_if_icmpeq:
  case Bytecodes::_if_icmpne:
  case Bytecodes::_if_icmplt:
  case Bytecodes::_if_icmpge:
  case Bytecodes::_if_icmpgt:
  case Bytecodes::_if_icmple:
  case Bytecodes::_if_acmpeq:
  case Bytecodes::_if_acmpne:
  case Bytecodes::_ifnull:
  case Bytecodes::_ifnonnull:
    cell_count = BranchData::static_cell_count();
    tag = DataLayout::branch_data_tag;
    break;
  case Bytecodes::_lookupswitch:
  case Bytecodes::_tableswitch:
    cell_count = MultiBranchData::compute_cell_count(stream);
    tag = DataLayout::multi_branch_data_tag;
    break;
  default:
    break;
  }
  assert(tag == DataLayout::multi_branch_data_tag ||
         ((MethodData::profile_arguments() || MethodData::profile_return()) &&
          (tag == DataLayout::call_type_data_tag ||
           tag == DataLayout::counter_data_tag ||
           tag == DataLayout::virtual_call_type_data_tag ||
           tag == DataLayout::virtual_call_data_tag)) ||
         cell_count == bytecode_cell_count(c), "cell counts must agree");
  if (cell_count >= 0) {
    assert(tag != DataLayout::no_tag, "bad tag");
    assert(bytecode_has_profile(c), "agree w/ BHP");
    data_layout->initialize(tag, stream->bci(), cell_count);
    return DataLayout::compute_size_in_bytes(cell_count);
  } else {
    assert(!bytecode_has_profile(c), "agree w/ !BHP");
    return 0;
  }
}

// Get the data at an arbitrary (sort of) data index.
ProfileData* MethodData::data_at(int data_index) const {
  if (out_of_bounds(data_index)) {
    return NULL;
  }
  DataLayout* data_layout = data_layout_at(data_index);
  return data_layout->data_in();
}

int DataLayout::cell_count() {
  switch (tag()) {
  case DataLayout::no_tag:
  default:
    ShouldNotReachHere();
    return 0;
  case DataLayout::bit_data_tag:
    return BitData::static_cell_count();
  case DataLayout::counter_data_tag:
    return CounterData::static_cell_count();
  case DataLayout::jump_data_tag:
    return JumpData::static_cell_count();
  case DataLayout::receiver_type_data_tag:
    return ReceiverTypeData::static_cell_count();
  case DataLayout::virtual_call_data_tag:
    return VirtualCallData::static_cell_count();
  case DataLayout::ret_data_tag:
    return RetData::static_cell_count();
  case DataLayout::branch_data_tag:
    return BranchData::static_cell_count();
  case DataLayout::multi_branch_data_tag:
    return ((new MultiBranchData(this))->cell_count());
  case DataLayout::arg_info_data_tag:
    return ((new ArgInfoData(this))->cell_count());
  case DataLayout::call_type_data_tag:
    return ((new CallTypeData(this))->cell_count());
  case DataLayout::virtual_call_type_data_tag:
    return ((new VirtualCallTypeData(this))->cell_count());
  case DataLayout::parameters_type_data_tag:
    return ((new ParametersTypeData(this))->cell_count());
  case DataLayout::speculative_trap_data_tag:
    return SpeculativeTrapData::static_cell_count();
  }
}
ProfileData* DataLayout::data_in() {
  switch (tag()) {
  case DataLayout::no_tag:
  default:
    ShouldNotReachHere();
    return NULL;
  case DataLayout::bit_data_tag:
    return new BitData(this);
  case DataLayout::counter_data_tag:
    return new CounterData(this);
  case DataLayout::jump_data_tag:
    return new JumpData(this);
  case DataLayout::receiver_type_data_tag:
    return new ReceiverTypeData(this);
  case DataLayout::virtual_call_data_tag:
    return new VirtualCallData(this);
  case DataLayout::ret_data_tag:
    return new RetData(this);
  case DataLayout::branch_data_tag:
    return new BranchData(this);
  case DataLayout::multi_branch_data_tag:
    return new MultiBranchData(this);
  case DataLayout::arg_info_data_tag:
    return new ArgInfoData(this);
  case DataLayout::call_type_data_tag:
    return new CallTypeData(this);
  case DataLayout::virtual_call_type_data_tag:
    return new VirtualCallTypeData(this);
  case DataLayout::parameters_type_data_tag:
    return new ParametersTypeData(this);
  case DataLayout::speculative_trap_data_tag:
    return new SpeculativeTrapData(this);
  }
}

// Iteration over data.
ProfileData* MethodData::next_data(ProfileData* current) const {
  int current_index = dp_to_di(current->dp());
  int next_index = current_index + current->size_in_bytes();
  ProfileData* next = data_at(next_index);
  return next;
}

DataLayout* MethodData::next_data_layout(DataLayout* current) const {
  int current_index = dp_to_di((address)current);
  int next_index = current_index + current->size_in_bytes();
  if (out_of_bounds(next_index)) {
    return NULL;
  }
  DataLayout* next = data_layout_at(next_index);
  return next;
}

// Give each of the data entries a chance to perform specific
// data initialization.
void MethodData::post_initialize(BytecodeStream* stream) {
  ResourceMark rm;
  ProfileData* data;
  for (data = first_data(); is_valid(data); data = next_data(data)) {
    stream->set_start(data->bci());
    stream->next();
    data->post_initialize(stream, this);
  }
  if (_parameters_type_data_di != no_parameters) {
    parameters_type_data()->post_initialize(NULL, this);
  }
}

// Initialize the MethodData* corresponding to a given method.
MethodData::MethodData(const methodHandle& method)
  : _method(method()),
    _extra_data_lock(Mutex::leaf, "MDO extra data lock"),
    _compiler_counters(),
    _parameters_type_data_di(parameters_uninitialized) {
  initialize();
}

void MethodData::initialize() {
  Thread* thread = Thread::current();
  NoSafepointVerifier no_safepoint;  // init function atomic wrt GC
  ResourceMark rm(thread);

  init();
  set_creation_mileage(mileage_of(method()));

  // Go through the bytecodes and allocate and initialize the
  // corresponding data cells.
  int data_size = 0;
  int empty_bc_count = 0;  // number of bytecodes lacking data
  _data[0] = 0;  // apparently not set below.
  BytecodeStream stream(methodHandle(thread, method()));
  Bytecodes::Code c;
  bool needs_speculative_traps = false;
  while ((c = stream.next()) >= 0) {
    int size_in_bytes = initialize_data(&stream, data_size);
    data_size += size_in_bytes;
    if (size_in_bytes == 0 JVMCI_ONLY(&& Bytecodes::can_trap(c)))  empty_bc_count += 1;
    needs_speculative_traps = needs_speculative_traps || is_speculative_trap_bytecode(c);
  }
  _data_size = data_size;
  int object_size = in_bytes(data_offset()) + data_size;

  // Add some extra DataLayout cells (at least one) to track stray traps.
  int extra_data_count = compute_extra_data_count(data_size, empty_bc_count, needs_speculative_traps);
  int extra_size = extra_data_count * DataLayout::compute_size_in_bytes(0);

  // Let's zero the space for the extra data
  Copy::zero_to_bytes(((address)_data) + data_size, extra_size);

  // Add a cell to record information about modified arguments.
  // Set up _args_modified array after traps cells so that
  // the code for traps cells works.
  DataLayout *dp = data_layout_at(data_size + extra_size);

  int arg_size = method()->size_of_parameters();
  dp->initialize(DataLayout::arg_info_data_tag, 0, arg_size+1);

  int arg_data_size = DataLayout::compute_size_in_bytes(arg_size+1);
  object_size += extra_size + arg_data_size;

  int parms_cell = ParametersTypeData::compute_cell_count(method());
  // If we are profiling parameters, we reserved an area near the end
  // of the MDO after the slots for bytecodes (because there's no bci
  // for method entry so they don't fit with the framework for the
  // profiling of bytecodes). We store the offset within the MDO of
  // this area (or -1 if no parameter is profiled)
  if (parms_cell > 0) {
    object_size += DataLayout::compute_size_in_bytes(parms_cell);
    _parameters_type_data_di = data_size + extra_size + arg_data_size;
    DataLayout *dp = data_layout_at(data_size + extra_size + arg_data_size);
    dp->initialize(DataLayout::parameters_type_data_tag, 0, parms_cell);
  } else {
    _parameters_type_data_di = no_parameters;
  }

  // Set an initial hint. Don't use set_hint_di() because
  // first_di() may be out of bounds if data_size is 0.
  // In that situation, _hint_di is never used, but at
  // least well-defined.
  _hint_di = first_di();

  post_initialize(&stream);

  assert(object_size == compute_allocation_size_in_bytes(methodHandle(thread, _method)), "MethodData: computed size != initialized size");
  set_size(object_size);
}

void MethodData::init() {
  _compiler_counters = CompilerCounters(); // reset compiler counters
  _invocation_counter.init();
  _backedge_counter.init();
  _invocation_counter_start = 0;
  _backedge_counter_start = 0;

  // Set per-method invoke- and backedge mask.
  double scale = 1.0;
  methodHandle mh(Thread::current(), _method);
  CompilerOracle::has_option_value(mh, CompileCommand::CompileThresholdScaling, scale);
  _invoke_mask = right_n_bits(CompilerConfig::scaled_freq_log(Tier0InvokeNotifyFreqLog, scale)) << InvocationCounter::count_shift;
  _backedge_mask = right_n_bits(CompilerConfig::scaled_freq_log(Tier0BackedgeNotifyFreqLog, scale)) << InvocationCounter::count_shift;

  _tenure_traps = 0;
  _num_loops = 0;
  _num_blocks = 0;
  _would_profile = unknown;

#if INCLUDE_JVMCI
  _jvmci_ir_size = 0;
  _failed_speculations = NULL;
#endif

#if INCLUDE_RTM_OPT
  _rtm_state = NoRTM; // No RTM lock eliding by default
  if (UseRTMLocking &&
      !CompilerOracle::has_option(mh, CompileCommand::NoRTMLockEliding)) {
    if (CompilerOracle::has_option(mh, CompileCommand::UseRTMLockEliding) || !UseRTMDeopt) {
      // Generate RTM lock eliding code without abort ratio calculation code.
      _rtm_state = UseRTM;
    } else if (UseRTMDeopt) {
      // Generate RTM lock eliding code and include abort ratio calculation
      // code if UseRTMDeopt is on.
      _rtm_state = ProfileRTM;
    }
  }
#endif

  // Initialize escape flags.
  clear_escape_info();
}

// Get a measure of how much mileage the method has on it.
int MethodData::mileage_of(Method* method) {
  return MAX2(method->invocation_count(), method->backedge_count());
}

bool MethodData::is_mature() const {
  return CompilationPolicy::is_mature(_method);
}

// Translate a bci to its corresponding data index (di).
address MethodData::bci_to_dp(int bci) {
  ResourceMark rm;
  DataLayout* data = data_layout_before(bci);
  DataLayout* prev = NULL;
  for ( ; is_valid(data); data = next_data_layout(data)) {
    if (data->bci() >= bci) {
      if (data->bci() == bci)  set_hint_di(dp_to_di((address)data));
      else if (prev != NULL)   set_hint_di(dp_to_di((address)prev));
      return (address)data;
    }
    prev = data;
  }
  return (address)limit_data_position();
}

// Translate a bci to its corresponding data, or NULL.
ProfileData* MethodData::bci_to_data(int bci) {
  DataLayout* data = data_layout_before(bci);
  for ( ; is_valid(data); data = next_data_layout(data)) {
    if (data->bci() == bci) {
      set_hint_di(dp_to_di((address)data));
      return data->data_in();
    } else if (data->bci() > bci) {
      break;
    }
  }
  return bci_to_extra_data(bci, NULL, false);
}

DataLayout* MethodData::next_extra(DataLayout* dp) {
  int nb_cells = 0;
  switch(dp->tag()) {
  case DataLayout::bit_data_tag:
  case DataLayout::no_tag:
    nb_cells = BitData::static_cell_count();
    break;
  case DataLayout::speculative_trap_data_tag:
    nb_cells = SpeculativeTrapData::static_cell_count();
    break;
  default:
    fatal("unexpected tag %d", dp->tag());
  }
  return (DataLayout*)((address)dp + DataLayout::compute_size_in_bytes(nb_cells));
}

ProfileData* MethodData::bci_to_extra_data_helper(int bci, Method* m, DataLayout*& dp, bool concurrent) {
  DataLayout* end = args_data_limit();

  for (;; dp = next_extra(dp)) {
    assert(dp < end, "moved past end of extra data");
    // No need for "Atomic::load_acquire" ops,
    // since the data structure is monotonic.
    switch(dp->tag()) {
    case DataLayout::no_tag:
      return NULL;
    case DataLayout::arg_info_data_tag:
      dp = end;
      return NULL; // ArgInfoData is at the end of extra data section.
    case DataLayout::bit_data_tag:
      if (m == NULL && dp->bci() == bci) {
        return new BitData(dp);
      }
      break;
    case DataLayout::speculative_trap_data_tag:
      if (m != NULL) {
        SpeculativeTrapData* data = new SpeculativeTrapData(dp);
        // data->method() may be null in case of a concurrent
        // allocation. Maybe it's for the same method. Try to use that
        // entry in that case.
        if (dp->bci() == bci) {
          if (data->method() == NULL) {
            assert(concurrent, "impossible because no concurrent allocation");
            return NULL;
          } else if (data->method() == m) {
            return data;
          }
        }
      }
      break;
    default:
      fatal("unexpected tag %d", dp->tag());
    }
  }
  return NULL;
}


// Translate a bci to its corresponding extra data, or NULL.
ProfileData* MethodData::bci_to_extra_data(int bci, Method* m, bool create_if_missing) {
  // This code assumes an entry for a SpeculativeTrapData is 2 cells
  assert(2*DataLayout::compute_size_in_bytes(BitData::static_cell_count()) ==
         DataLayout::compute_size_in_bytes(SpeculativeTrapData::static_cell_count()),
         "code needs to be adjusted");

  // Do not create one of these if method has been redefined.
  if (m != NULL && m->is_old()) {
    return NULL;
  }

  DataLayout* dp  = extra_data_base();
  DataLayout* end = args_data_limit();

  // Allocation in the extra data space has to be atomic because not
  // all entries have the same size and non atomic concurrent
  // allocation would result in a corrupted extra data space.
  ProfileData* result = bci_to_extra_data_helper(bci, m, dp, true);
  if (result != NULL) {
    return result;
  }

  if (create_if_missing && dp < end) {
    MutexLocker ml(&_extra_data_lock);
    // Check again now that we have the lock. Another thread may
    // have added extra data entries.
    ProfileData* result = bci_to_extra_data_helper(bci, m, dp, false);
    if (result != NULL || dp >= end) {
      return result;
    }

    assert(dp->tag() == DataLayout::no_tag || (dp->tag() == DataLayout::speculative_trap_data_tag && m != NULL), "should be free");
    assert(next_extra(dp)->tag() == DataLayout::no_tag || next_extra(dp)->tag() == DataLayout::arg_info_data_tag, "should be free or arg info");
    u1 tag = m == NULL ? DataLayout::bit_data_tag : DataLayout::speculative_trap_data_tag;
    // SpeculativeTrapData is 2 slots. Make sure we have room.
    if (m != NULL && next_extra(dp)->tag() != DataLayout::no_tag) {
      return NULL;
    }
    DataLayout temp;
    temp.initialize(tag, bci, 0);

    dp->set_header(temp.header());
    assert(dp->tag() == tag, "sane");
    assert(dp->bci() == bci, "no concurrent allocation");
    if (tag == DataLayout::bit_data_tag) {
      return new BitData(dp);
    } else {
      SpeculativeTrapData* data = new SpeculativeTrapData(dp);
      data->set_method(m);
      return data;
    }
  }
  return NULL;
}

ArgInfoData *MethodData::arg_info() {
  DataLayout* dp    = extra_data_base();
  DataLayout* end   = args_data_limit();
  for (; dp < end; dp = next_extra(dp)) {
    if (dp->tag() == DataLayout::arg_info_data_tag)
      return new ArgInfoData(dp);
  }
  return NULL;
}

// Printing

void MethodData::print_on(outputStream* st) const {
  assert(is_methodData(), "should be method data");
  st->print("method data for ");
  method()->print_value_on(st);
  st->cr();
  print_data_on(st);
}

void MethodData::print_value_on(outputStream* st) const {
  assert(is_methodData(), "should be method data");
  st->print("method data for ");
  method()->print_value_on(st);
}

void MethodData::print_data_on(outputStream* st) const {
  ResourceMark rm;
  ProfileData* data = first_data();
  if (_parameters_type_data_di != no_parameters) {
    parameters_type_data()->print_data_on(st);
  }
  for ( ; is_valid(data); data = next_data(data)) {
    st->print("%d", dp_to_di(data->dp()));
    st->fill_to(6);
    data->print_data_on(st, this);
  }
  st->print_cr("--- Extra data:");
  DataLayout* dp    = extra_data_base();
  DataLayout* end   = args_data_limit();
  for (;; dp = next_extra(dp)) {
    assert(dp < end, "moved past end of extra data");
    // No need for "Atomic::load_acquire" ops,
    // since the data structure is monotonic.
    switch(dp->tag()) {
    case DataLayout::no_tag:
      continue;
    case DataLayout::bit_data_tag:
      data = new BitData(dp);
      break;
    case DataLayout::speculative_trap_data_tag:
      data = new SpeculativeTrapData(dp);
      break;
    case DataLayout::arg_info_data_tag:
      data = new ArgInfoData(dp);
      dp = end; // ArgInfoData is at the end of extra data section.
      break;
    default:
      fatal("unexpected tag %d", dp->tag());
    }
    st->print("%d", dp_to_di(data->dp()));
    st->fill_to(6);
    data->print_data_on(st);
    if (dp >= end) return;
  }
}

// Verification

void MethodData::verify_on(outputStream* st) {
  guarantee(is_methodData(), "object must be method data");
  // guarantee(m->is_perm(), "should be in permspace");
  this->verify_data_on(st);
}

void MethodData::verify_data_on(outputStream* st) {
  NEEDS_CLEANUP;
  // not yet implemented.
}

bool MethodData::profile_jsr292(const methodHandle& m, int bci) {
  if (m->is_compiled_lambda_form()) {
    return true;
  }

  Bytecode_invoke inv(m , bci);
  return inv.is_invokedynamic() || inv.is_invokehandle();
}

bool MethodData::profile_unsafe(const methodHandle& m, int bci) {
  Bytecode_invoke inv(m , bci);
  if (inv.is_invokevirtual()) {
    Symbol* klass = inv.klass();
    if (klass == vmSymbols::jdk_internal_misc_Unsafe() ||
        klass == vmSymbols::sun_misc_Unsafe() ||
        klass == vmSymbols::jdk_internal_misc_ScopedMemoryAccess()) {
      Symbol* name = inv.name();
      if (name->starts_with("get") || name->starts_with("put")) {
        return true;
      }
    }
  }
  return false;
}

bool MethodData::profile_memory_access(const methodHandle& m, int bci) {
  Bytecode_invoke inv(m , bci);
  if (inv.is_invokestatic()) {
    if (inv.klass() == vmSymbols::jdk_incubator_foreign_MemoryAccess()) {
      if (inv.name()->starts_with("get") || inv.name()->starts_with("set")) {
        return true;
      }
    }
  }
  return false;
}

int MethodData::profile_arguments_flag() {
  return TypeProfileLevel % 10;
}

bool MethodData::profile_arguments() {
  return profile_arguments_flag() > no_type_profile && profile_arguments_flag() <= type_profile_all;
}

bool MethodData::profile_arguments_jsr292_only() {
  return profile_arguments_flag() == type_profile_jsr292;
}

bool MethodData::profile_all_arguments() {
  return profile_arguments_flag() == type_profile_all;
}

bool MethodData::profile_arguments_for_invoke(const methodHandle& m, int bci) {
  if (!profile_arguments()) {
    return false;
  }

  if (profile_all_arguments()) {
    return true;
  }

  if (profile_unsafe(m, bci)) {
    return true;
  }

  if (profile_memory_access(m, bci)) {
    return true;
  }

  assert(profile_arguments_jsr292_only(), "inconsistent");
  return profile_jsr292(m, bci);
}

int MethodData::profile_return_flag() {
  return (TypeProfileLevel % 100) / 10;
}

bool MethodData::profile_return() {
  return profile_return_flag() > no_type_profile && profile_return_flag() <= type_profile_all;
}

bool MethodData::profile_return_jsr292_only() {
  return profile_return_flag() == type_profile_jsr292;
}

bool MethodData::profile_all_return() {
  return profile_return_flag() == type_profile_all;
}

bool MethodData::profile_return_for_invoke(const methodHandle& m, int bci) {
  if (!profile_return()) {
    return false;
  }

  if (profile_all_return()) {
    return true;
  }

  assert(profile_return_jsr292_only(), "inconsistent");
  return profile_jsr292(m, bci);
}

int MethodData::profile_parameters_flag() {
  return TypeProfileLevel / 100;
}

bool MethodData::profile_parameters() {
  return profile_parameters_flag() > no_type_profile && profile_parameters_flag() <= type_profile_all;
}

bool MethodData::profile_parameters_jsr292_only() {
  return profile_parameters_flag() == type_profile_jsr292;
}

bool MethodData::profile_all_parameters() {
  return profile_parameters_flag() == type_profile_all;
}

bool MethodData::profile_parameters_for_method(const methodHandle& m) {
  if (!profile_parameters()) {
    return false;
  }

  if (profile_all_parameters()) {
    return true;
  }

  assert(profile_parameters_jsr292_only(), "inconsistent");
  return m->is_compiled_lambda_form();
}

void MethodData::metaspace_pointers_do(MetaspaceClosure* it) {
  log_trace(cds)("Iter(MethodData): %p", this);
  it->push(&_method);
}

void MethodData::clean_extra_data_helper(DataLayout* dp, int shift, bool reset) {
  if (shift == 0) {
    return;
  }
  if (!reset) {
    // Move all cells of trap entry at dp left by "shift" cells
    intptr_t* start = (intptr_t*)dp;
    intptr_t* end = (intptr_t*)next_extra(dp);
    for (intptr_t* ptr = start; ptr < end; ptr++) {
      *(ptr-shift) = *ptr;
    }
  } else {
    // Reset "shift" cells stopping at dp
    intptr_t* start = ((intptr_t*)dp) - shift;
    intptr_t* end = (intptr_t*)dp;
    for (intptr_t* ptr = start; ptr < end; ptr++) {
      *ptr = 0;
    }
  }
}

// Check for entries that reference an unloaded method
class CleanExtraDataKlassClosure : public CleanExtraDataClosure {
  bool _always_clean;
public:
  CleanExtraDataKlassClosure(bool always_clean) : _always_clean(always_clean) {}
  bool is_live(Method* m) {
    return !(_always_clean) && m->method_holder()->is_loader_alive();
  }
};

// Check for entries that reference a redefined method
class CleanExtraDataMethodClosure : public CleanExtraDataClosure {
public:
  CleanExtraDataMethodClosure() {}
  bool is_live(Method* m) { return !m->is_old(); }
};


// Remove SpeculativeTrapData entries that reference an unloaded or
// redefined method
void MethodData::clean_extra_data(CleanExtraDataClosure* cl) {
  DataLayout* dp  = extra_data_base();
  DataLayout* end = args_data_limit();

  int shift = 0;
  for (; dp < end; dp = next_extra(dp)) {
    switch(dp->tag()) {
    case DataLayout::speculative_trap_data_tag: {
      SpeculativeTrapData* data = new SpeculativeTrapData(dp);
      Method* m = data->method();
      assert(m != NULL, "should have a method");
      if (!cl->is_live(m)) {
        // "shift" accumulates the number of cells for dead
        // SpeculativeTrapData entries that have been seen so
        // far. Following entries must be shifted left by that many
        // cells to remove the dead SpeculativeTrapData entries.
        shift += (int)((intptr_t*)next_extra(dp) - (intptr_t*)dp);
      } else {
        // Shift this entry left if it follows dead
        // SpeculativeTrapData entries
        clean_extra_data_helper(dp, shift);
      }
      break;
    }
    case DataLayout::bit_data_tag:
      // Shift this entry left if it follows dead SpeculativeTrapData
      // entries
      clean_extra_data_helper(dp, shift);
      continue;
    case DataLayout::no_tag:
    case DataLayout::arg_info_data_tag:
      // We are at end of the live trap entries. The previous "shift"
      // cells contain entries that are either dead or were shifted
      // left. They need to be reset to no_tag
      clean_extra_data_helper(dp, shift, true);
      return;
    default:
      fatal("unexpected tag %d", dp->tag());
    }
  }
}

// Verify there's no unloaded or redefined method referenced by a
// SpeculativeTrapData entry
void MethodData::verify_extra_data_clean(CleanExtraDataClosure* cl) {
#ifdef ASSERT
  DataLayout* dp  = extra_data_base();
  DataLayout* end = args_data_limit();

  for (; dp < end; dp = next_extra(dp)) {
    switch(dp->tag()) {
    case DataLayout::speculative_trap_data_tag: {
      SpeculativeTrapData* data = new SpeculativeTrapData(dp);
      Method* m = data->method();
      assert(m != NULL && cl->is_live(m), "Method should exist");
      break;
    }
    case DataLayout::bit_data_tag:
      continue;
    case DataLayout::no_tag:
    case DataLayout::arg_info_data_tag:
      return;
    default:
      fatal("unexpected tag %d", dp->tag());
    }
  }
#endif
}

void MethodData::clean_method_data(bool always_clean) {
  ResourceMark rm;
  for (ProfileData* data = first_data();
       is_valid(data);
       data = next_data(data)) {
    data->clean_weak_klass_links(always_clean);
  }
  ParametersTypeData* parameters = parameters_type_data();
  if (parameters != NULL) {
    parameters->clean_weak_klass_links(always_clean);
  }

  CleanExtraDataKlassClosure cl(always_clean);
  clean_extra_data(&cl);
  verify_extra_data_clean(&cl);
}

// This is called during redefinition to clean all "old" redefined
// methods out of MethodData for all methods.
void MethodData::clean_weak_method_links() {
  ResourceMark rm;
  CleanExtraDataMethodClosure cl;
  clean_extra_data(&cl);
  verify_extra_data_clean(&cl);
}
