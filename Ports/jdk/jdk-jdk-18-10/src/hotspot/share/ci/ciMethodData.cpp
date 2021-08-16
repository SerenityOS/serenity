/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "ci/ciMetadata.hpp"
#include "ci/ciMethodData.hpp"
#include "ci/ciReplay.hpp"
#include "ci/ciUtilities.inline.hpp"
#include "compiler/compiler_globals.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/klass.inline.hpp"
#include "runtime/deoptimization.hpp"
#include "utilities/copy.hpp"

// ciMethodData

// ------------------------------------------------------------------
// ciMethodData::ciMethodData
//
ciMethodData::ciMethodData(MethodData* md)
: ciMetadata(md),
  _data_size(0), _extra_data_size(0), _data(NULL),
  // Set an initial hint. Don't use set_hint_di() because
  // first_di() may be out of bounds if data_size is 0.
  _hint_di(first_di()),
  _state(empty_state),
  _saw_free_extra_data(false),
  // Initialize the escape information (to "don't know.");
  _eflags(0), _arg_local(0), _arg_stack(0), _arg_returned(0),
  _creation_mileage(0),
  _current_mileage(0),
  _invocation_counter(0),
  _backedge_counter(0),
  _orig(),
  _parameters(NULL) {}

// Check for entries that reference an unloaded method
class PrepareExtraDataClosure : public CleanExtraDataClosure {
  MethodData*            _mdo;
  SafepointStateTracker  _safepoint_tracker;
  GrowableArray<Method*> _uncached_methods;

public:
  PrepareExtraDataClosure(MethodData* mdo)
    : _mdo(mdo),
      _safepoint_tracker(SafepointSynchronize::safepoint_state_tracker()),
      _uncached_methods()
  { }

  bool is_live(Method* m) {
    if (!m->method_holder()->is_loader_alive()) {
      return false;
    }
    if (CURRENT_ENV->cached_metadata(m) == NULL) {
      // Uncached entries need to be pre-populated.
      _uncached_methods.append(m);
    }
    return true;
  }

  bool has_safepointed() {
    return _safepoint_tracker.safepoint_state_changed();
  }

  bool finish() {
    if (_uncached_methods.length() == 0) {
      // Preparation finished iff all Methods* were already cached.
      return true;
    }
    // Holding locks through safepoints is bad practice.
    MutexUnlocker mu(_mdo->extra_data_lock());
    for (int i = 0; i < _uncached_methods.length(); ++i) {
      if (has_safepointed()) {
        // The metadata in the growable array might contain stale
        // entries after a safepoint.
        return false;
      }
      Method* method = _uncached_methods.at(i);
      // Populating ciEnv caches may cause safepoints due
      // to taking the Compile_lock with safepoint checks.
      (void)CURRENT_ENV->get_method(method);
    }
    return false;
  }
};

void ciMethodData::prepare_metadata() {
  MethodData* mdo = get_MethodData();

  for (;;) {
    ResourceMark rm;
    PrepareExtraDataClosure cl(mdo);
    mdo->clean_extra_data(&cl);
    if (cl.finish()) {
      // When encountering uncached metadata, the Compile_lock might be
      // acquired when creating ciMetadata handles, causing safepoints
      // which requires a new round of preparation to clean out potentially
      // new unloading metadata.
      return;
    }
  }
}

void ciMethodData::load_remaining_extra_data() {
  MethodData* mdo = get_MethodData();
  MutexLocker ml(mdo->extra_data_lock());
  // Deferred metadata cleaning due to concurrent class unloading.
  prepare_metadata();
  // After metadata preparation, there is no stale metadata,
  // and no safepoints can introduce more stale metadata.
  NoSafepointVerifier no_safepoint;

  assert((mdo->data_size() == _data_size) && (mdo->extra_data_size() == _extra_data_size), "sanity, unchanged");
  assert(extra_data_base() == (DataLayout*)((address) _data + _data_size), "sanity");

  // Copy the extra data once it is prepared (i.e. cache populated, no release of extra data lock anymore)
  Copy::disjoint_words_atomic((HeapWord*) mdo->extra_data_base(),
                              (HeapWord*)((address) _data + _data_size),
                              (_extra_data_size - mdo->parameters_size_in_bytes()) / HeapWordSize);

  // speculative trap entries also hold a pointer to a Method so need to be translated
  DataLayout* dp_src  = mdo->extra_data_base();
  DataLayout* end_src = mdo->args_data_limit();
  DataLayout* dp_dst  = extra_data_base();
  for (;; dp_src = MethodData::next_extra(dp_src), dp_dst = MethodData::next_extra(dp_dst)) {
    assert(dp_src < end_src, "moved past end of extra data");
    assert(((intptr_t)dp_dst) - ((intptr_t)extra_data_base()) == ((intptr_t)dp_src) - ((intptr_t)mdo->extra_data_base()), "source and destination don't match");

    int tag = dp_src->tag();
    switch(tag) {
    case DataLayout::speculative_trap_data_tag: {
      ciSpeculativeTrapData data_dst(dp_dst);
      SpeculativeTrapData   data_src(dp_src);
      data_dst.translate_from(&data_src);
      break;
    }
    case DataLayout::bit_data_tag:
      break;
    case DataLayout::no_tag:
    case DataLayout::arg_info_data_tag:
      // An empty slot or ArgInfoData entry marks the end of the trap data
      {
        return; // Need a block to avoid SS compiler bug
      }
    default:
      fatal("bad tag = %d", tag);
    }
  }
}

bool ciMethodData::load_data() {
  MethodData* mdo = get_MethodData();
  if (mdo == NULL) {
    return false;
  }

  // To do: don't copy the data if it is not "ripe" -- require a minimum #
  // of invocations.

  // Snapshot the data and extra parameter data first without the extra trap and arg info data.
  // Those are copied in a second step. Actually, an approximate snapshot of the data is taken.
  // Any concurrently executing threads may be changing the data as we copy it.
  //
  // The first snapshot step requires two copies (data entries and parameter data entries) since
  // the MDO is laid out as follows:
  //
  //  data_base:        ---------------------------
  //                    |       data entries      |
  //                    |           ...           |
  //  extra_data_base:  ---------------------------
  //                    |    trap data entries    |
  //                    |           ...           |
  //                    | one arg info data entry |
  //                    |    data for each arg    |
  //                    |           ...           |
  //  args_data_limit:  ---------------------------
  //                    |  parameter data entries |
  //                    |           ...           |
  //  extra_data_limit: ---------------------------
  //
  // _data_size = extra_data_base - data_base
  // _extra_data_size = extra_data_limit - extra_data_base
  // total_size = _data_size + _extra_data_size
  // args_data_limit = data_base + total_size - parameter_data_size

#ifndef ZERO
  // Some Zero platforms do not have expected alignment, and do not use
  // this code. static_assert would still fire and fail for them.
  static_assert(sizeof(_orig) % HeapWordSize == 0, "align");
#endif
  Copy::disjoint_words_atomic((HeapWord*) &mdo->_compiler_counters,
                              (HeapWord*) &_orig,
                              sizeof(_orig) / HeapWordSize);
  Arena* arena = CURRENT_ENV->arena();
  _data_size = mdo->data_size();
  _extra_data_size = mdo->extra_data_size();
  int total_size = _data_size + _extra_data_size;
  _data = (intptr_t *) arena->Amalloc(total_size);
  Copy::disjoint_words_atomic((HeapWord*) mdo->data_base(),
                              (HeapWord*) _data,
                              _data_size / HeapWordSize);

  int parameters_data_size = mdo->parameters_size_in_bytes();
  if (parameters_data_size > 0) {
    // Snapshot the parameter data
    Copy::disjoint_words_atomic((HeapWord*) mdo->args_data_limit(),
                                (HeapWord*) ((address)_data + total_size - parameters_data_size),
                                parameters_data_size / HeapWordSize);
  }
  // Traverse the profile data, translating any oops into their
  // ci equivalents.
  ResourceMark rm;
  ciProfileData* ci_data = first_data();
  ProfileData* data = mdo->first_data();
  while (is_valid(ci_data)) {
    ci_data->translate_from(data);
    ci_data = next_data(ci_data);
    data = mdo->next_data(data);
  }
  if (mdo->parameters_type_data() != NULL) {
    _parameters = data_layout_at(mdo->parameters_type_data_di());
    ciParametersTypeData* parameters = new ciParametersTypeData(_parameters);
    parameters->translate_from(mdo->parameters_type_data());
  }

  assert((DataLayout*) ((address)_data + total_size - parameters_data_size) == args_data_limit(),
      "sanity - parameter data starts after the argument data of the single ArgInfoData entry");
  load_remaining_extra_data();

  // Note:  Extra data are all BitData, and do not need translation.
  _creation_mileage = mdo->creation_mileage();
  _current_mileage = MethodData::mileage_of(mdo->method());
  _invocation_counter = mdo->invocation_count();
  _backedge_counter = mdo->backedge_count();
  _state = mdo->is_mature()? mature_state: immature_state;

  _eflags = mdo->eflags();
  _arg_local = mdo->arg_local();
  _arg_stack = mdo->arg_stack();
  _arg_returned  = mdo->arg_returned();
#ifndef PRODUCT
  if (ReplayCompiles) {
    ciReplay::initialize(this);
    if (is_empty()) {
      return false;
    }
  }
#endif
  return true;
}

void ciReceiverTypeData::translate_receiver_data_from(const ProfileData* data) {
  for (uint row = 0; row < row_limit(); row++) {
    Klass* k = data->as_ReceiverTypeData()->receiver(row);
    if (k != NULL) {
      if (k->is_loader_alive()) {
        ciKlass* klass = CURRENT_ENV->get_klass(k);
        set_receiver(row, klass);
      } else {
        // With concurrent class unloading, the MDO could have stale metadata; override it
        clear_row(row);
      }
    } else {
      set_receiver(row, NULL);
    }
  }
}

void ciTypeStackSlotEntries::translate_type_data_from(const TypeStackSlotEntries* entries) {
  for (int i = 0; i < number_of_entries(); i++) {
    intptr_t k = entries->type(i);
    Klass* klass = (Klass*)klass_part(k);
    if (klass != NULL && !klass->is_loader_alive()) {
      // With concurrent class unloading, the MDO could have stale metadata; override it
      TypeStackSlotEntries::set_type(i, TypeStackSlotEntries::with_status((Klass*)NULL, k));
    } else {
      TypeStackSlotEntries::set_type(i, translate_klass(k));
    }
  }
}

void ciReturnTypeEntry::translate_type_data_from(const ReturnTypeEntry* ret) {
  intptr_t k = ret->type();
  Klass* klass = (Klass*)klass_part(k);
  if (klass != NULL && !klass->is_loader_alive()) {
    // With concurrent class unloading, the MDO could have stale metadata; override it
    set_type(ReturnTypeEntry::with_status((Klass*)NULL, k));
  } else {
    set_type(translate_klass(k));
  }
}

void ciSpeculativeTrapData::translate_from(const ProfileData* data) {
  Method* m = data->as_SpeculativeTrapData()->method();
  ciMethod* ci_m = CURRENT_ENV->get_method(m);
  set_method(ci_m);
}

// Get the data at an arbitrary (sort of) data index.
ciProfileData* ciMethodData::data_at(int data_index) {
  if (out_of_bounds(data_index)) {
    return NULL;
  }
  DataLayout* data_layout = data_layout_at(data_index);
  return data_from(data_layout);
}

ciProfileData* ciMethodData::data_from(DataLayout* data_layout) {
  switch (data_layout->tag()) {
  case DataLayout::no_tag:
  default:
    ShouldNotReachHere();
    return NULL;
  case DataLayout::bit_data_tag:
    return new ciBitData(data_layout);
  case DataLayout::counter_data_tag:
    return new ciCounterData(data_layout);
  case DataLayout::jump_data_tag:
    return new ciJumpData(data_layout);
  case DataLayout::receiver_type_data_tag:
    return new ciReceiverTypeData(data_layout);
  case DataLayout::virtual_call_data_tag:
    return new ciVirtualCallData(data_layout);
  case DataLayout::ret_data_tag:
    return new ciRetData(data_layout);
  case DataLayout::branch_data_tag:
    return new ciBranchData(data_layout);
  case DataLayout::multi_branch_data_tag:
    return new ciMultiBranchData(data_layout);
  case DataLayout::arg_info_data_tag:
    return new ciArgInfoData(data_layout);
  case DataLayout::call_type_data_tag:
    return new ciCallTypeData(data_layout);
  case DataLayout::virtual_call_type_data_tag:
    return new ciVirtualCallTypeData(data_layout);
  case DataLayout::parameters_type_data_tag:
    return new ciParametersTypeData(data_layout);
  };
}

// Iteration over data.
ciProfileData* ciMethodData::next_data(ciProfileData* current) {
  int current_index = dp_to_di(current->dp());
  int next_index = current_index + current->size_in_bytes();
  ciProfileData* next = data_at(next_index);
  return next;
}

DataLayout* ciMethodData::next_data_layout(DataLayout* current) {
  int current_index = dp_to_di((address)current);
  int next_index = current_index + current->size_in_bytes();
  if (out_of_bounds(next_index)) {
    return NULL;
  }
  DataLayout* next = data_layout_at(next_index);
  return next;
}

ciProfileData* ciMethodData::bci_to_extra_data(int bci, ciMethod* m, bool& two_free_slots) {
  DataLayout* dp  = extra_data_base();
  DataLayout* end = args_data_limit();
  two_free_slots = false;
  for (;dp < end; dp = MethodData::next_extra(dp)) {
    switch(dp->tag()) {
    case DataLayout::no_tag:
      _saw_free_extra_data = true;  // observed an empty slot (common case)
      two_free_slots = (MethodData::next_extra(dp)->tag() == DataLayout::no_tag);
      return NULL;
    case DataLayout::arg_info_data_tag:
      return NULL; // ArgInfoData is after the trap data right before the parameter data.
    case DataLayout::bit_data_tag:
      if (m == NULL && dp->bci() == bci) {
        return new ciBitData(dp);
      }
      break;
    case DataLayout::speculative_trap_data_tag: {
      ciSpeculativeTrapData* data = new ciSpeculativeTrapData(dp);
      // data->method() might be null if the MDO is snapshotted
      // concurrently with a trap
      if (m != NULL && data->method() == m && dp->bci() == bci) {
        return data;
      }
      break;
    }
    default:
      fatal("bad tag = %d", dp->tag());
    }
  }
  return NULL;
}

// Translate a bci to its corresponding data, or NULL.
ciProfileData* ciMethodData::bci_to_data(int bci, ciMethod* m) {
  // If m is not NULL we look for a SpeculativeTrapData entry
  if (m == NULL) {
    DataLayout* data_layout = data_layout_before(bci);
    for ( ; is_valid(data_layout); data_layout = next_data_layout(data_layout)) {
      if (data_layout->bci() == bci) {
        set_hint_di(dp_to_di((address)data_layout));
        return data_from(data_layout);
      } else if (data_layout->bci() > bci) {
        break;
      }
    }
  }
  bool two_free_slots = false;
  ciProfileData* result = bci_to_extra_data(bci, m, two_free_slots);
  if (result != NULL) {
    return result;
  }
  if (m != NULL && !two_free_slots) {
    // We were looking for a SpeculativeTrapData entry we didn't
    // find. Room is not available for more SpeculativeTrapData
    // entries, look in the non SpeculativeTrapData entries.
    return bci_to_data(bci, NULL);
  }
  return NULL;
}

// Conservatively decode the trap_state of a ciProfileData.
int ciMethodData::has_trap_at(ciProfileData* data, int reason) {
  typedef Deoptimization::DeoptReason DR_t;
  int per_bc_reason
    = Deoptimization::reason_recorded_per_bytecode_if_any((DR_t) reason);
  if (trap_count(reason) == 0) {
    // Impossible for this trap to have occurred, regardless of trap_state.
    // Note:  This happens if the MDO is empty.
    return 0;
  } else if (per_bc_reason == Deoptimization::Reason_none) {
    // We cannot conclude anything; a trap happened somewhere, maybe here.
    return -1;
  } else if (data == NULL) {
    // No profile here, not even an extra_data record allocated on the fly.
    // If there are empty extra_data records, and there had been a trap,
    // there would have been a non-null data pointer.  If there are no
    // free extra_data records, we must return a conservative -1.
    if (_saw_free_extra_data)
      return 0;                 // Q.E.D.
    else
      return -1;                // bail with a conservative answer
  } else {
    return Deoptimization::trap_state_has_reason(data->trap_state(), per_bc_reason);
  }
}

int ciMethodData::trap_recompiled_at(ciProfileData* data) {
  if (data == NULL) {
    return (_saw_free_extra_data? 0: -1);  // (see previous method)
  } else {
    return Deoptimization::trap_state_is_recompiled(data->trap_state())? 1: 0;
  }
}

void ciMethodData::clear_escape_info() {
  VM_ENTRY_MARK;
  MethodData* mdo = get_MethodData();
  if (mdo != NULL) {
    mdo->clear_escape_info();
    ArgInfoData *aid = arg_info();
    int arg_count = (aid == NULL) ? 0 : aid->number_of_args();
    for (int i = 0; i < arg_count; i++) {
      set_arg_modified(i, 0);
    }
  }
  _eflags = _arg_local = _arg_stack = _arg_returned = 0;
}

// copy our escape info to the MethodData* if it exists
void ciMethodData::update_escape_info() {
  VM_ENTRY_MARK;
  MethodData* mdo = get_MethodData();
  if ( mdo != NULL) {
    mdo->set_eflags(_eflags);
    mdo->set_arg_local(_arg_local);
    mdo->set_arg_stack(_arg_stack);
    mdo->set_arg_returned(_arg_returned);
    int arg_count = mdo->method()->size_of_parameters();
    for (int i = 0; i < arg_count; i++) {
      mdo->set_arg_modified(i, arg_modified(i));
    }
  }
}

void ciMethodData::set_compilation_stats(short loops, short blocks) {
  VM_ENTRY_MARK;
  MethodData* mdo = get_MethodData();
  if (mdo != NULL) {
    mdo->set_num_loops(loops);
    mdo->set_num_blocks(blocks);
  }
}

void ciMethodData::set_would_profile(bool p) {
  VM_ENTRY_MARK;
  MethodData* mdo = get_MethodData();
  if (mdo != NULL) {
    mdo->set_would_profile(p);
  }
}

void ciMethodData::set_argument_type(int bci, int i, ciKlass* k) {
  VM_ENTRY_MARK;
  MethodData* mdo = get_MethodData();
  if (mdo != NULL) {
    ProfileData* data = mdo->bci_to_data(bci);
    if (data != NULL) {
      if (data->is_CallTypeData()) {
        data->as_CallTypeData()->set_argument_type(i, k->get_Klass());
      } else {
        assert(data->is_VirtualCallTypeData(), "no arguments!");
        data->as_VirtualCallTypeData()->set_argument_type(i, k->get_Klass());
      }
    }
  }
}

void ciMethodData::set_parameter_type(int i, ciKlass* k) {
  VM_ENTRY_MARK;
  MethodData* mdo = get_MethodData();
  if (mdo != NULL) {
    mdo->parameters_type_data()->set_type(i, k->get_Klass());
  }
}

void ciMethodData::set_return_type(int bci, ciKlass* k) {
  VM_ENTRY_MARK;
  MethodData* mdo = get_MethodData();
  if (mdo != NULL) {
    ProfileData* data = mdo->bci_to_data(bci);
    if (data != NULL) {
      if (data->is_CallTypeData()) {
        data->as_CallTypeData()->set_return_type(k->get_Klass());
      } else {
        assert(data->is_VirtualCallTypeData(), "no arguments!");
        data->as_VirtualCallTypeData()->set_return_type(k->get_Klass());
      }
    }
  }
}

bool ciMethodData::has_escape_info() {
  return eflag_set(MethodData::estimated);
}

void ciMethodData::set_eflag(MethodData::EscapeFlag f) {
  set_bits(_eflags, f);
}

bool ciMethodData::eflag_set(MethodData::EscapeFlag f) const {
  return mask_bits(_eflags, f) != 0;
}

void ciMethodData::set_arg_local(int i) {
  set_nth_bit(_arg_local, i);
}

void ciMethodData::set_arg_stack(int i) {
  set_nth_bit(_arg_stack, i);
}

void ciMethodData::set_arg_returned(int i) {
  set_nth_bit(_arg_returned, i);
}

void ciMethodData::set_arg_modified(int arg, uint val) {
  ArgInfoData *aid = arg_info();
  if (aid == NULL)
    return;
  assert(arg >= 0 && arg < aid->number_of_args(), "valid argument number");
  aid->set_arg_modified(arg, val);
}

bool ciMethodData::is_arg_local(int i) const {
  return is_set_nth_bit(_arg_local, i);
}

bool ciMethodData::is_arg_stack(int i) const {
  return is_set_nth_bit(_arg_stack, i);
}

bool ciMethodData::is_arg_returned(int i) const {
  return is_set_nth_bit(_arg_returned, i);
}

uint ciMethodData::arg_modified(int arg) const {
  ArgInfoData *aid = arg_info();
  if (aid == NULL)
    return 0;
  assert(arg >= 0 && arg < aid->number_of_args(), "valid argument number");
  return aid->arg_modified(arg);
}

ByteSize ciMethodData::offset_of_slot(ciProfileData* data, ByteSize slot_offset_in_data) {
  // Get offset within MethodData* of the data array
  ByteSize data_offset = MethodData::data_offset();

  // Get cell offset of the ProfileData within data array
  int cell_offset = dp_to_di(data->dp());

  // Add in counter_offset, the # of bytes into the ProfileData of counter or flag
  int offset = in_bytes(data_offset) + cell_offset + in_bytes(slot_offset_in_data);

  return in_ByteSize(offset);
}

ciArgInfoData *ciMethodData::arg_info() const {
  // Should be last, have to skip all traps.
  DataLayout* dp  = extra_data_base();
  DataLayout* end = args_data_limit();
  for (; dp < end; dp = MethodData::next_extra(dp)) {
    if (dp->tag() == DataLayout::arg_info_data_tag)
      return new ciArgInfoData(dp);
  }
  return NULL;
}


// Implementation of the print method.
void ciMethodData::print_impl(outputStream* st) {
  ciMetadata::print_impl(st);
}

void ciMethodData::dump_replay_data_type_helper(outputStream* out, int round, int& count, ProfileData* pdata, ByteSize offset, ciKlass* k) {
  if (k != NULL) {
    if (round == 0) {
      count++;
    } else {
      out->print(" %d %s", (int)(dp_to_di(pdata->dp() + in_bytes(offset)) / sizeof(intptr_t)), k->name()->as_quoted_ascii());
    }
  }
}

template<class T> void ciMethodData::dump_replay_data_receiver_type_helper(outputStream* out, int round, int& count, T* vdata) {
  for (uint i = 0; i < vdata->row_limit(); i++) {
    dump_replay_data_type_helper(out, round, count, vdata, vdata->receiver_offset(i), vdata->receiver(i));
  }
}

template<class T> void ciMethodData::dump_replay_data_call_type_helper(outputStream* out, int round, int& count, T* call_type_data) {
  if (call_type_data->has_arguments()) {
    for (int i = 0; i < call_type_data->number_of_arguments(); i++) {
      dump_replay_data_type_helper(out, round, count, call_type_data, call_type_data->argument_type_offset(i), call_type_data->valid_argument_type(i));
    }
  }
  if (call_type_data->has_return()) {
    dump_replay_data_type_helper(out, round, count, call_type_data, call_type_data->return_type_offset(), call_type_data->valid_return_type());
  }
}

void ciMethodData::dump_replay_data_extra_data_helper(outputStream* out, int round, int& count) {
  DataLayout* dp  = extra_data_base();
  DataLayout* end = args_data_limit();

  for (;dp < end; dp = MethodData::next_extra(dp)) {
    switch(dp->tag()) {
    case DataLayout::no_tag:
    case DataLayout::arg_info_data_tag:
      return;
    case DataLayout::bit_data_tag:
      break;
    case DataLayout::speculative_trap_data_tag: {
      ciSpeculativeTrapData* data = new ciSpeculativeTrapData(dp);
      ciMethod* m = data->method();
      if (m != NULL) {
        if (round == 0) {
          count++;
        } else {
          out->print(" %d ", (int)(dp_to_di(((address)dp) + in_bytes(ciSpeculativeTrapData::method_offset())) / sizeof(intptr_t)));
          m->dump_name_as_ascii(out);
        }
      }
      break;
    }
    default:
      fatal("bad tag = %d", dp->tag());
    }
  }
}

void ciMethodData::dump_replay_data(outputStream* out) {
  ResourceMark rm;
  MethodData* mdo = get_MethodData();
  Method* method = mdo->method();
  Klass* holder = method->method_holder();
  out->print("ciMethodData %s %s %s %d %d",
             holder->name()->as_quoted_ascii(),
             method->name()->as_quoted_ascii(),
             method->signature()->as_quoted_ascii(),
             _state,
             current_mileage());

  // dump the contents of the MDO header as raw data
  unsigned char* orig = (unsigned char*)&_orig;
  int length = sizeof(_orig);
  out->print(" orig %d", length);
  for (int i = 0; i < length; i++) {
    out->print(" %d", orig[i]);
  }

  // dump the MDO data as raw data
  int elements = (data_size() + extra_data_size()) / sizeof(intptr_t);
  out->print(" data %d", elements);
  for (int i = 0; i < elements; i++) {
    // We could use INTPTR_FORMAT here but that's zero justified
    // which makes comparing it with the SA version of this output
    // harder. data()'s element type is intptr_t.
    out->print(" " INTPTRNZ_FORMAT, data()[i]);
  }

  // The MDO contained oop references as ciObjects, so scan for those
  // and emit pairs of offset and klass name so that they can be
  // reconstructed at runtime.  The first round counts the number of
  // oop references and the second actually emits them.
  ciParametersTypeData* parameters = parameters_type_data();
  for (int count = 0, round = 0; round < 2; round++) {
    if (round == 1) out->print(" oops %d", count);
    ProfileData* pdata = first_data();
    for ( ; is_valid(pdata); pdata = next_data(pdata)) {
      if (pdata->is_VirtualCallData()) {
        ciVirtualCallData* vdata = (ciVirtualCallData*)pdata;
        dump_replay_data_receiver_type_helper<ciVirtualCallData>(out, round, count, vdata);
        if (pdata->is_VirtualCallTypeData()) {
          ciVirtualCallTypeData* call_type_data = (ciVirtualCallTypeData*)pdata;
          dump_replay_data_call_type_helper<ciVirtualCallTypeData>(out, round, count, call_type_data);
        }
      } else if (pdata->is_ReceiverTypeData()) {
        ciReceiverTypeData* vdata = (ciReceiverTypeData*)pdata;
        dump_replay_data_receiver_type_helper<ciReceiverTypeData>(out, round, count, vdata);
      } else if (pdata->is_CallTypeData()) {
          ciCallTypeData* call_type_data = (ciCallTypeData*)pdata;
          dump_replay_data_call_type_helper<ciCallTypeData>(out, round, count, call_type_data);
      }
    }
    if (parameters != NULL) {
      for (int i = 0; i < parameters->number_of_parameters(); i++) {
        dump_replay_data_type_helper(out, round, count, parameters, ParametersTypeData::type_offset(i), parameters->valid_parameter_type(i));
      }
    }
  }
  for (int count = 0, round = 0; round < 2; round++) {
    if (round == 1) out->print(" methods %d", count);
    dump_replay_data_extra_data_helper(out, round, count);
  }
  out->cr();
}

#ifndef PRODUCT
void ciMethodData::print() {
  print_data_on(tty);
}

void ciMethodData::print_data_on(outputStream* st) {
  ResourceMark rm;
  ciParametersTypeData* parameters = parameters_type_data();
  if (parameters != NULL) {
    parameters->print_data_on(st);
  }
  ciProfileData* data;
  for (data = first_data(); is_valid(data); data = next_data(data)) {
    st->print("%d", dp_to_di(data->dp()));
    st->fill_to(6);
    data->print_data_on(st);
  }
  st->print_cr("--- Extra data:");
  DataLayout* dp  = extra_data_base();
  DataLayout* end = args_data_limit();
  for (;; dp = MethodData::next_extra(dp)) {
    assert(dp < end, "moved past end of extra data");
    switch (dp->tag()) {
    case DataLayout::no_tag:
      continue;
    case DataLayout::bit_data_tag:
      data = new BitData(dp);
      break;
    case DataLayout::arg_info_data_tag:
      data = new ciArgInfoData(dp);
      dp = end; // ArgInfoData is after the trap data right before the parameter data.
      break;
    case DataLayout::speculative_trap_data_tag:
      data = new ciSpeculativeTrapData(dp);
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

void ciTypeEntries::print_ciklass(outputStream* st, intptr_t k) {
  if (TypeEntries::is_type_none(k)) {
    st->print("none");
  } else if (TypeEntries::is_type_unknown(k)) {
    st->print("unknown");
  } else {
    valid_ciklass(k)->print_name_on(st);
  }
  if (TypeEntries::was_null_seen(k)) {
    st->print(" (null seen)");
  }
}

void ciTypeStackSlotEntries::print_data_on(outputStream* st) const {
  for (int i = 0; i < number_of_entries(); i++) {
    _pd->tab(st);
    st->print("%d: stack (%u) ", i, stack_slot(i));
    print_ciklass(st, type(i));
    st->cr();
  }
}

void ciReturnTypeEntry::print_data_on(outputStream* st) const {
  _pd->tab(st);
  st->print("ret ");
  print_ciklass(st, type());
  st->cr();
}

void ciCallTypeData::print_data_on(outputStream* st, const char* extra) const {
  print_shared(st, "ciCallTypeData", extra);
  if (has_arguments()) {
    tab(st, true);
    st->print_cr("argument types");
    args()->print_data_on(st);
  }
  if (has_return()) {
    tab(st, true);
    st->print_cr("return type");
    ret()->print_data_on(st);
  }
}

void ciReceiverTypeData::print_receiver_data_on(outputStream* st) const {
  uint row;
  int entries = 0;
  for (row = 0; row < row_limit(); row++) {
    if (receiver(row) != NULL)  entries++;
  }
  st->print_cr("count(%u) entries(%u)", count(), entries);
  for (row = 0; row < row_limit(); row++) {
    if (receiver(row) != NULL) {
      tab(st);
      receiver(row)->print_name_on(st);
      st->print_cr("(%u)", receiver_count(row));
    }
  }
}

void ciReceiverTypeData::print_data_on(outputStream* st, const char* extra) const {
  print_shared(st, "ciReceiverTypeData", extra);
  print_receiver_data_on(st);
}

void ciVirtualCallData::print_data_on(outputStream* st, const char* extra) const {
  print_shared(st, "ciVirtualCallData", extra);
  rtd_super()->print_receiver_data_on(st);
}

void ciVirtualCallTypeData::print_data_on(outputStream* st, const char* extra) const {
  print_shared(st, "ciVirtualCallTypeData", extra);
  rtd_super()->print_receiver_data_on(st);
  if (has_arguments()) {
    tab(st, true);
    st->print("argument types");
    args()->print_data_on(st);
  }
  if (has_return()) {
    tab(st, true);
    st->print("return type");
    ret()->print_data_on(st);
  }
}

void ciParametersTypeData::print_data_on(outputStream* st, const char* extra) const {
  st->print_cr("ciParametersTypeData");
  parameters()->print_data_on(st);
}

void ciSpeculativeTrapData::print_data_on(outputStream* st, const char* extra) const {
  st->print_cr("ciSpeculativeTrapData");
  tab(st);
  method()->print_short_name(st);
  st->cr();
}
#endif
