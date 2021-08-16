/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CIMETHODDATA_HPP
#define SHARE_CI_CIMETHODDATA_HPP

#include "ci/ciClassList.hpp"
#include "ci/ciKlass.hpp"
#include "ci/ciObject.hpp"
#include "ci/ciUtilities.hpp"
#include "oops/methodData.hpp"
#include "oops/oop.hpp"
#include "runtime/deoptimization.hpp"

class ciBitData;
class ciCounterData;
class ciJumpData;
class ciReceiverTypeData;
class ciRetData;
class ciBranchData;
class ciArrayData;
class ciMultiBranchData;
class ciArgInfoData;
class ciCallTypeData;
class ciVirtualCallTypeData;
class ciParametersTypeData;
class ciSpeculativeTrapData;

typedef ProfileData ciProfileData;

class ciBitData : public BitData {
public:
  ciBitData(DataLayout* layout) : BitData(layout) {};
};

class ciCounterData : public CounterData {
public:
  ciCounterData(DataLayout* layout) : CounterData(layout) {};
};

class ciJumpData : public JumpData {
public:
  ciJumpData(DataLayout* layout) : JumpData(layout) {};
};

class ciTypeEntries {
protected:
  static intptr_t translate_klass(intptr_t k) {
    Klass* v = TypeEntries::valid_klass(k);
    if (v != NULL) {
      ciKlass* klass = CURRENT_ENV->get_klass(v);
      return with_status(klass, k);
    }
    return with_status(NULL, k);
  }

public:
  static ciKlass* valid_ciklass(intptr_t k) {
    if (!TypeEntries::is_type_none(k) &&
        !TypeEntries::is_type_unknown(k)) {
      ciKlass* res = (ciKlass*)TypeEntries::klass_part(k);
      assert(res != NULL, "invalid");
      return res;
    } else {
      return NULL;
    }
  }

  static ProfilePtrKind ptr_kind(intptr_t v) {
    bool maybe_null = TypeEntries::was_null_seen(v);
    if (!maybe_null) {
      return ProfileNeverNull;
    } else if (TypeEntries::is_type_none(v)) {
      return ProfileAlwaysNull;
    } else {
      return ProfileMaybeNull;
    }
  }

  static intptr_t with_status(ciKlass* k, intptr_t in) {
    return TypeEntries::with_status((intptr_t)k, in);
  }

#ifndef PRODUCT
  static void print_ciklass(outputStream* st, intptr_t k);
#endif
};

class ciTypeStackSlotEntries : public TypeStackSlotEntries, ciTypeEntries {
public:
  void translate_type_data_from(const TypeStackSlotEntries* args);

  ciKlass* valid_type(int i) const {
    return valid_ciklass(type(i));
  }

  ProfilePtrKind ptr_kind(int i) const {
    return ciTypeEntries::ptr_kind(type(i));
  }

#ifndef PRODUCT
  void print_data_on(outputStream* st) const;
#endif
};

class ciReturnTypeEntry : public ReturnTypeEntry, ciTypeEntries {
public:
  void translate_type_data_from(const ReturnTypeEntry* ret);

  ciKlass* valid_type() const {
    return valid_ciklass(type());
  }

  ProfilePtrKind ptr_kind() const {
    return ciTypeEntries::ptr_kind(type());
  }

#ifndef PRODUCT
  void print_data_on(outputStream* st) const;
#endif
};

class ciCallTypeData : public CallTypeData {
public:
  ciCallTypeData(DataLayout* layout) : CallTypeData(layout) {}

  ciTypeStackSlotEntries* args() const { return (ciTypeStackSlotEntries*)CallTypeData::args(); }
  ciReturnTypeEntry* ret() const { return (ciReturnTypeEntry*)CallTypeData::ret(); }

  void translate_from(const ProfileData* data) {
    if (has_arguments()) {
      args()->translate_type_data_from(data->as_CallTypeData()->args());
    }
    if (has_return()) {
      ret()->translate_type_data_from(data->as_CallTypeData()->ret());
    }
  }

  intptr_t argument_type(int i) const {
    assert(has_arguments(), "no arg type profiling data");
    return args()->type(i);
  }

  ciKlass* valid_argument_type(int i) const {
    assert(has_arguments(), "no arg type profiling data");
    return args()->valid_type(i);
  }

  intptr_t return_type() const {
    assert(has_return(), "no ret type profiling data");
    return ret()->type();
  }

  ciKlass* valid_return_type() const {
    assert(has_return(), "no ret type profiling data");
    return ret()->valid_type();
  }

  ProfilePtrKind argument_ptr_kind(int i) const {
    return args()->ptr_kind(i);
  }

  ProfilePtrKind return_ptr_kind() const {
    return ret()->ptr_kind();
  }

#ifndef PRODUCT
  void print_data_on(outputStream* st, const char* extra = NULL) const;
#endif
};

class ciReceiverTypeData : public ReceiverTypeData {
public:
  ciReceiverTypeData(DataLayout* layout) : ReceiverTypeData(layout) {};

  void set_receiver(uint row, ciKlass* recv) {
    assert((uint)row < row_limit(), "oob");
    set_intptr_at(receiver0_offset + row * receiver_type_row_cell_count,
                  (intptr_t) recv);
  }

  ciKlass* receiver(uint row) const {
    assert((uint)row < row_limit(), "oob");
    ciKlass* recv = (ciKlass*)intptr_at(receiver0_offset + row * receiver_type_row_cell_count);
    assert(recv == NULL || recv->is_klass(), "wrong type");
    return recv;
  }

  // Copy & translate from oop based ReceiverTypeData
  virtual void translate_from(const ProfileData* data) {
    translate_receiver_data_from(data);
  }
  void translate_receiver_data_from(const ProfileData* data);
#ifndef PRODUCT
  void print_data_on(outputStream* st, const char* extra = NULL) const;
  void print_receiver_data_on(outputStream* st) const;
#endif
};

class ciVirtualCallData : public VirtualCallData {
  // Fake multiple inheritance...  It's a ciReceiverTypeData also.
  ciReceiverTypeData* rtd_super() const { return (ciReceiverTypeData*) this; }

public:
  ciVirtualCallData(DataLayout* layout) : VirtualCallData(layout) {};

  void set_receiver(uint row, ciKlass* recv) {
    rtd_super()->set_receiver(row, recv);
  }

  ciKlass* receiver(uint row) {
    return rtd_super()->receiver(row);
  }

  // Copy & translate from oop based VirtualCallData
  virtual void translate_from(const ProfileData* data) {
    rtd_super()->translate_receiver_data_from(data);
  }
#ifndef PRODUCT
  void print_data_on(outputStream* st, const char* extra = NULL) const;
#endif
};

class ciVirtualCallTypeData : public VirtualCallTypeData {
private:
  // Fake multiple inheritance...  It's a ciReceiverTypeData also.
  ciReceiverTypeData* rtd_super() const { return (ciReceiverTypeData*) this; }
public:
  ciVirtualCallTypeData(DataLayout* layout) : VirtualCallTypeData(layout) {}

  void set_receiver(uint row, ciKlass* recv) {
    rtd_super()->set_receiver(row, recv);
  }

  ciKlass* receiver(uint row) const {
    return rtd_super()->receiver(row);
  }

  ciTypeStackSlotEntries* args() const { return (ciTypeStackSlotEntries*)VirtualCallTypeData::args(); }
  ciReturnTypeEntry* ret() const { return (ciReturnTypeEntry*)VirtualCallTypeData::ret(); }

  // Copy & translate from oop based VirtualCallData
  virtual void translate_from(const ProfileData* data) {
    rtd_super()->translate_receiver_data_from(data);
    if (has_arguments()) {
      args()->translate_type_data_from(data->as_VirtualCallTypeData()->args());
    }
    if (has_return()) {
      ret()->translate_type_data_from(data->as_VirtualCallTypeData()->ret());
    }
  }

  ciKlass* valid_argument_type(int i) const {
    assert(has_arguments(), "no arg type profiling data");
    return args()->valid_type(i);
  }

  intptr_t return_type() const {
    assert(has_return(), "no ret type profiling data");
    return ret()->type();
  }

  ciKlass* valid_return_type() const {
    assert(has_return(), "no ret type profiling data");
    return ret()->valid_type();
  }

  ProfilePtrKind argument_ptr_kind(int i) const {
    return args()->ptr_kind(i);
  }

  ProfilePtrKind return_ptr_kind() const {
    return ret()->ptr_kind();
  }

#ifndef PRODUCT
  void print_data_on(outputStream* st, const char* extra = NULL) const;
#endif
};


class ciRetData : public RetData {
public:
  ciRetData(DataLayout* layout) : RetData(layout) {};
};

class ciBranchData : public BranchData {
public:
  ciBranchData(DataLayout* layout) : BranchData(layout) {};
};

class ciMultiBranchData : public MultiBranchData {
public:
  ciMultiBranchData(DataLayout* layout) : MultiBranchData(layout) {};
};

class ciArgInfoData : public ArgInfoData {
public:
  ciArgInfoData(DataLayout* layout) : ArgInfoData(layout) {};
};

class ciParametersTypeData : public ParametersTypeData {
public:
  ciParametersTypeData(DataLayout* layout) : ParametersTypeData(layout) {}

  virtual void translate_from(const ProfileData* data) {
    parameters()->translate_type_data_from(data->as_ParametersTypeData()->parameters());
  }

  ciTypeStackSlotEntries* parameters() const { return (ciTypeStackSlotEntries*)ParametersTypeData::parameters(); }

  ciKlass* valid_parameter_type(int i) const {
    return parameters()->valid_type(i);
  }

  ProfilePtrKind parameter_ptr_kind(int i) const {
    return parameters()->ptr_kind(i);
  }

#ifndef PRODUCT
  void print_data_on(outputStream* st, const char* extra = NULL) const;
#endif
};

class ciSpeculativeTrapData : public SpeculativeTrapData {
public:
  ciSpeculativeTrapData(DataLayout* layout) : SpeculativeTrapData(layout) {}

  virtual void translate_from(const ProfileData* data);

  ciMethod* method() const {
    return (ciMethod*)intptr_at(speculative_trap_method);
  }

  void set_method(ciMethod* m) {
    set_intptr_at(speculative_trap_method, (intptr_t)m);
  }

#ifndef PRODUCT
  void print_data_on(outputStream* st, const char* extra = NULL) const;
#endif
};

// ciMethodData
//
// This class represents a MethodData* in the HotSpot virtual
// machine.

class ciMethodData : public ciMetadata {
  CI_PACKAGE_ACCESS
  friend class ciReplay;

private:
  // Size in bytes
  int _data_size;
  int _extra_data_size;

  // Data entries
  intptr_t* _data;

  // Cached hint for data_layout_before()
  int _hint_di;

  // Is data attached?  And is it mature?
  enum { empty_state, immature_state, mature_state };
  u_char _state;

  // Set this true if empty extra_data slots are ever witnessed.
  u_char _saw_free_extra_data;

  // Support for interprocedural escape analysis
  intx _eflags;       // flags on escape information
  intx _arg_local;    // bit set of non-escaping arguments
  intx _arg_stack;    // bit set of stack-allocatable arguments
  intx _arg_returned; // bit set of returned arguments

  int _creation_mileage; // method mileage at MDO creation

  // Maturity of the oop when the snapshot is taken.
  int _current_mileage;

  // These counters hold the age of MDO in tiered. In tiered we can have the same method
  // running at different compilation levels concurrently. So, in order to precisely measure
  // its maturity we need separate counters.
  int _invocation_counter;
  int _backedge_counter;

  // Coherent snapshot of original header.
  MethodData::CompilerCounters _orig;

  // Area dedicated to parameters. NULL if no parameter profiling for this method.
  DataLayout* _parameters;
  int parameters_size() const {
    return _parameters == NULL ? 0 : parameters_type_data()->size_in_bytes();
  }

  ciMethodData(MethodData* md = NULL);

  // Accessors
  int data_size() const { return _data_size; }
  int extra_data_size() const { return _extra_data_size; }
  intptr_t * data() const { return _data; }

  MethodData* get_MethodData() const {
    return (MethodData*)_metadata;
  }

  const char* type_string()                      { return "ciMethodData"; }

  void print_impl(outputStream* st);

  DataLayout* data_layout_at(int data_index) const {
    assert(data_index % sizeof(intptr_t) == 0, "unaligned");
    return (DataLayout*) (((address)_data) + data_index);
  }

  bool out_of_bounds(int data_index) {
    return data_index >= data_size();
  }

  // hint accessors
  int      hint_di() const  { return _hint_di; }
  void set_hint_di(int di)  {
    assert(!out_of_bounds(di), "hint_di out of bounds");
    _hint_di = di;
  }

  DataLayout* data_layout_before(int bci) {
    // avoid SEGV on this edge case
    if (data_size() == 0)
      return NULL;
    DataLayout* layout = data_layout_at(hint_di());
    if (layout->bci() <= bci)
      return layout;
    return data_layout_at(first_di());
  }

  // What is the index of the first data entry?
  int first_di() { return 0; }

  ciArgInfoData *arg_info() const;

  void prepare_metadata();
  void load_remaining_extra_data();
  ciProfileData* bci_to_extra_data(int bci, ciMethod* m, bool& two_free_slots);

  void dump_replay_data_type_helper(outputStream* out, int round, int& count, ProfileData* pdata, ByteSize offset, ciKlass* k);
  template<class T> void dump_replay_data_call_type_helper(outputStream* out, int round, int& count, T* call_type_data);
  template<class T> void dump_replay_data_receiver_type_helper(outputStream* out, int round, int& count, T* call_type_data);
  void dump_replay_data_extra_data_helper(outputStream* out, int round, int& count);
  ciProfileData* data_from(DataLayout* data_layout);

public:
  bool is_method_data() const { return true; }

  bool is_empty()  { return _state == empty_state; }
  bool is_mature() { return _state == mature_state; }

  int creation_mileage() { return _creation_mileage; }
  int current_mileage()  { return _current_mileage; }

  int invocation_count() { return _invocation_counter; }
  int backedge_count()   { return _backedge_counter;   }

#if INCLUDE_RTM_OPT
  // return cached value
  int rtm_state() {
    if (is_empty()) {
      return NoRTM;
    } else {
      return get_MethodData()->rtm_state();
    }
  }
#endif

  // Transfer information about the method to MethodData*.
  // would_profile means we would like to profile this method,
  // meaning it's not trivial.
  void set_would_profile(bool p);
  // Also set the numer of loops and blocks in the method.
  // Again, this is used to determine if a method is trivial.
  void set_compilation_stats(short loops, short blocks);
  // If the compiler finds a profiled type that is known statically
  // for sure, set it in the MethodData
  void set_argument_type(int bci, int i, ciKlass* k);
  void set_parameter_type(int i, ciKlass* k);
  void set_return_type(int bci, ciKlass* k);

  bool load_data();

  // Convert a dp (data pointer) to a di (data index).
  int dp_to_di(address dp) {
    return dp - ((address)_data);
  }

  // Get the data at an arbitrary (sort of) data index.
  ciProfileData* data_at(int data_index);

  // Walk through the data in order.
  ciProfileData* first_data() { return data_at(first_di()); }
  ciProfileData* next_data(ciProfileData* current);
  DataLayout* next_data_layout(DataLayout* current);
  bool is_valid(ciProfileData* current) { return current != NULL; }
  bool is_valid(DataLayout* current)    { return current != NULL; }

  DataLayout* extra_data_base() const  { return data_layout_at(data_size()); }
  DataLayout* args_data_limit() const  { return data_layout_at(data_size() + extra_data_size() -
                                                               parameters_size()); }

  // Get the data at an arbitrary bci, or NULL if there is none. If m
  // is not NULL look for a SpeculativeTrapData if any first.
  ciProfileData* bci_to_data(int bci, ciMethod* m = NULL);

  uint overflow_trap_count() const {
    return _orig.overflow_trap_count();
  }
  uint overflow_recompile_count() const {
    return _orig.overflow_recompile_count();
  }
  uint decompile_count() const {
    return _orig.decompile_count();
  }
  uint trap_count(int reason) const {
    return _orig.trap_count(reason);
  }
  uint trap_reason_limit() const { return MethodData::trap_reason_limit(); }
  uint trap_count_limit()  const { return MethodData::trap_count_limit(); }

  // Helpful query functions that decode trap_state.
  int has_trap_at(ciProfileData* data, int reason);
  int has_trap_at(int bci, ciMethod* m, int reason) {
    assert((m != NULL) == Deoptimization::reason_is_speculate(reason), "inconsistent method/reason");
    return has_trap_at(bci_to_data(bci, m), reason);
  }
  int trap_recompiled_at(ciProfileData* data);
  int trap_recompiled_at(int bci, ciMethod* m) {
    return trap_recompiled_at(bci_to_data(bci, m));
  }

  void clear_escape_info();
  bool has_escape_info();
  void update_escape_info();

  void set_eflag(MethodData::EscapeFlag f);
  bool eflag_set(MethodData::EscapeFlag f) const;

  void set_arg_local(int i);
  void set_arg_stack(int i);
  void set_arg_returned(int i);
  void set_arg_modified(int arg, uint val);

  bool is_arg_local(int i) const;
  bool is_arg_stack(int i) const;
  bool is_arg_returned(int i) const;
  uint arg_modified(int arg) const;

  ciParametersTypeData* parameters_type_data() const {
    return _parameters != NULL ? new ciParametersTypeData(_parameters) : NULL;
  }

  // Code generation helper
  ByteSize offset_of_slot(ciProfileData* data, ByteSize slot_offset_in_data);
  int      byte_offset_of_slot(ciProfileData* data, ByteSize slot_offset_in_data) { return in_bytes(offset_of_slot(data, slot_offset_in_data)); }

#ifndef PRODUCT
  // printing support for method data
  void print();
  void print_data_on(outputStream* st);
#endif
  void dump_replay_data(outputStream* out);
};

#endif // SHARE_CI_CIMETHODDATA_HPP
