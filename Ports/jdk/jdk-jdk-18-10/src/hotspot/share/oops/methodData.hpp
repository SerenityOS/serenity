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

#ifndef SHARE_OOPS_METHODDATA_HPP
#define SHARE_OOPS_METHODDATA_HPP

#include "interpreter/bytecodes.hpp"
#include "oops/metadata.hpp"
#include "oops/method.hpp"
#include "oops/oop.hpp"
#include "runtime/atomic.hpp"
#include "runtime/mutex.hpp"
#include "utilities/align.hpp"
#include "utilities/copy.hpp"

class BytecodeStream;

// The MethodData object collects counts and other profile information
// during zeroth-tier (interpretive) and first-tier execution.
// The profile is used later by compilation heuristics.  Some heuristics
// enable use of aggressive (or "heroic") optimizations.  An aggressive
// optimization often has a down-side, a corner case that it handles
// poorly, but which is thought to be rare.  The profile provides
// evidence of this rarity for a given method or even BCI.  It allows
// the compiler to back out of the optimization at places where it
// has historically been a poor choice.  Other heuristics try to use
// specific information gathered about types observed at a given site.
//
// All data in the profile is approximate.  It is expected to be accurate
// on the whole, but the system expects occasional inaccuraces, due to
// counter overflow, multiprocessor races during data collection, space
// limitations, missing MDO blocks, etc.  Bad or missing data will degrade
// optimization quality but will not affect correctness.  Also, each MDO
// is marked with its birth-date ("creation_mileage") which can be used
// to assess the quality ("maturity") of its data.
//
// Short (<32-bit) counters are designed to overflow to a known "saturated"
// state.  Also, certain recorded per-BCI events are given one-bit counters
// which overflow to a saturated state which applied to all counters at
// that BCI.  In other words, there is a small lattice which approximates
// the ideal of an infinite-precision counter for each event at each BCI,
// and the lattice quickly "bottoms out" in a state where all counters
// are taken to be indefinitely large.
//
// The reader will find many data races in profile gathering code, starting
// with invocation counter incrementation.  None of these races harm correct
// execution of the compiled code.

// forward decl
class ProfileData;

// DataLayout
//
// Overlay for generic profiling data.
class DataLayout {
  friend class VMStructs;
  friend class JVMCIVMStructs;

private:
  // Every data layout begins with a header.  This header
  // contains a tag, which is used to indicate the size/layout
  // of the data, 8 bits of flags, which can be used in any way,
  // 32 bits of trap history (none/one reason/many reasons),
  // and a bci, which is used to tie this piece of data to a
  // specific bci in the bytecodes.
  union {
    u8 _bits;
    struct {
      u1 _tag;
      u1 _flags;
      u2 _bci;
      u4 _traps;
    } _struct;
  } _header;

  // The data layout has an arbitrary number of cells, each sized
  // to accomodate a pointer or an integer.
  intptr_t _cells[1];

  // Some types of data layouts need a length field.
  static bool needs_array_len(u1 tag);

public:
  enum {
    counter_increment = 1
  };

  enum {
    cell_size = sizeof(intptr_t)
  };

  // Tag values
  enum {
    no_tag,
    bit_data_tag,
    counter_data_tag,
    jump_data_tag,
    receiver_type_data_tag,
    virtual_call_data_tag,
    ret_data_tag,
    branch_data_tag,
    multi_branch_data_tag,
    arg_info_data_tag,
    call_type_data_tag,
    virtual_call_type_data_tag,
    parameters_type_data_tag,
    speculative_trap_data_tag
  };

  enum {
    // The trap state breaks down as [recompile:1 | reason:31].
    // This further breakdown is defined in deoptimization.cpp.
    // See Deoptimization::trap_state_reason for an assert that
    // trap_bits is big enough to hold reasons < Reason_RECORDED_LIMIT.
    //
    // The trap_state is collected only if ProfileTraps is true.
    trap_bits = 1+31,  // 31: enough to distinguish [0..Reason_RECORDED_LIMIT].
    trap_mask = -1,
    first_flag = 0
  };

  // Size computation
  static int header_size_in_bytes() {
    return header_size_in_cells() * cell_size;
  }
  static int header_size_in_cells() {
    return LP64_ONLY(1) NOT_LP64(2);
  }

  static int compute_size_in_bytes(int cell_count) {
    return header_size_in_bytes() + cell_count * cell_size;
  }

  // Initialization
  void initialize(u1 tag, u2 bci, int cell_count);

  // Accessors
  u1 tag() {
    return _header._struct._tag;
  }

  // Return 32 bits of trap state.
  // The state tells if traps with zero, one, or many reasons have occurred.
  // It also tells whether zero or many recompilations have occurred.
  // The associated trap histogram in the MDO itself tells whether
  // traps are common or not.  If a BCI shows that a trap X has
  // occurred, and the MDO shows N occurrences of X, we make the
  // simplifying assumption that all N occurrences can be blamed
  // on that BCI.
  uint trap_state() const {
    return _header._struct._traps;
  }

  void set_trap_state(uint new_state) {
    assert(ProfileTraps, "used only under +ProfileTraps");
    uint old_flags = _header._struct._traps;
    _header._struct._traps = new_state | old_flags;
  }

  u1 flags() const {
    return _header._struct._flags;
  }

  u2 bci() const {
    return _header._struct._bci;
  }

  void set_header(u8 value) {
    _header._bits = value;
  }
  u8 header() {
    return _header._bits;
  }
  void set_cell_at(int index, intptr_t value) {
    _cells[index] = value;
  }
  void release_set_cell_at(int index, intptr_t value);
  intptr_t cell_at(int index) const {
    return _cells[index];
  }

  void set_flag_at(u1 flag_number) {
    _header._struct._flags |= (0x1 << flag_number);
  }
  bool flag_at(u1 flag_number) const {
    return (_header._struct._flags & (0x1 << flag_number)) != 0;
  }

  // Low-level support for code generation.
  static ByteSize header_offset() {
    return byte_offset_of(DataLayout, _header);
  }
  static ByteSize tag_offset() {
    return byte_offset_of(DataLayout, _header._struct._tag);
  }
  static ByteSize flags_offset() {
    return byte_offset_of(DataLayout, _header._struct._flags);
  }
  static ByteSize bci_offset() {
    return byte_offset_of(DataLayout, _header._struct._bci);
  }
  static ByteSize cell_offset(int index) {
    return byte_offset_of(DataLayout, _cells) + in_ByteSize(index * cell_size);
  }
  // Return a value which, when or-ed as a byte into _flags, sets the flag.
  static u1 flag_number_to_constant(u1 flag_number) {
    DataLayout temp; temp.set_header(0);
    temp.set_flag_at(flag_number);
    return temp._header._struct._flags;
  }
  // Return a value which, when or-ed as a word into _header, sets the flag.
  static u8 flag_mask_to_header_mask(uint byte_constant) {
    DataLayout temp; temp.set_header(0);
    temp._header._struct._flags = byte_constant;
    return temp._header._bits;
  }

  ProfileData* data_in();

  int size_in_bytes() {
    int cells = cell_count();
    assert(cells >= 0, "invalid number of cells");
    return DataLayout::compute_size_in_bytes(cells);
  }
  int cell_count();

  // GC support
  void clean_weak_klass_links(bool always_clean);
};


// ProfileData class hierarchy
class ProfileData;
class   BitData;
class     CounterData;
class       ReceiverTypeData;
class         VirtualCallData;
class           VirtualCallTypeData;
class       RetData;
class       CallTypeData;
class   JumpData;
class     BranchData;
class   ArrayData;
class     MultiBranchData;
class     ArgInfoData;
class     ParametersTypeData;
class   SpeculativeTrapData;

// ProfileData
//
// A ProfileData object is created to refer to a section of profiling
// data in a structured way.
class ProfileData : public ResourceObj {
  friend class TypeEntries;
  friend class ReturnTypeEntry;
  friend class TypeStackSlotEntries;
private:
  enum {
    tab_width_one = 16,
    tab_width_two = 36
  };

  // This is a pointer to a section of profiling data.
  DataLayout* _data;

  char* print_data_on_helper(const MethodData* md) const;

protected:
  DataLayout* data() { return _data; }
  const DataLayout* data() const { return _data; }

  enum {
    cell_size = DataLayout::cell_size
  };

public:
  // How many cells are in this?
  virtual int cell_count() const {
    ShouldNotReachHere();
    return -1;
  }

  // Return the size of this data.
  int size_in_bytes() {
    return DataLayout::compute_size_in_bytes(cell_count());
  }

protected:
  // Low-level accessors for underlying data
  void set_intptr_at(int index, intptr_t value) {
    assert(0 <= index && index < cell_count(), "oob");
    data()->set_cell_at(index, value);
  }
  void release_set_intptr_at(int index, intptr_t value);
  intptr_t intptr_at(int index) const {
    assert(0 <= index && index < cell_count(), "oob");
    return data()->cell_at(index);
  }
  void set_uint_at(int index, uint value) {
    set_intptr_at(index, (intptr_t) value);
  }
  void release_set_uint_at(int index, uint value);
  uint uint_at(int index) const {
    return (uint)intptr_at(index);
  }
  void set_int_at(int index, int value) {
    set_intptr_at(index, (intptr_t) value);
  }
  void release_set_int_at(int index, int value);
  int int_at(int index) const {
    return (int)intptr_at(index);
  }
  int int_at_unchecked(int index) const {
    return (int)data()->cell_at(index);
  }
  void set_oop_at(int index, oop value) {
    set_intptr_at(index, cast_from_oop<intptr_t>(value));
  }
  oop oop_at(int index) const {
    return cast_to_oop(intptr_at(index));
  }

  void set_flag_at(int flag_number) {
    data()->set_flag_at(flag_number);
  }
  bool flag_at(int flag_number) const {
    return data()->flag_at(flag_number);
  }

  // two convenient imports for use by subclasses:
  static ByteSize cell_offset(int index) {
    return DataLayout::cell_offset(index);
  }
  static int flag_number_to_constant(int flag_number) {
    return DataLayout::flag_number_to_constant(flag_number);
  }

  ProfileData(DataLayout* data) {
    _data = data;
  }

public:
  // Constructor for invalid ProfileData.
  ProfileData();

  u2 bci() const {
    return data()->bci();
  }

  address dp() {
    return (address)_data;
  }

  int trap_state() const {
    return data()->trap_state();
  }
  void set_trap_state(int new_state) {
    data()->set_trap_state(new_state);
  }

  // Type checking
  virtual bool is_BitData()         const { return false; }
  virtual bool is_CounterData()     const { return false; }
  virtual bool is_JumpData()        const { return false; }
  virtual bool is_ReceiverTypeData()const { return false; }
  virtual bool is_VirtualCallData() const { return false; }
  virtual bool is_RetData()         const { return false; }
  virtual bool is_BranchData()      const { return false; }
  virtual bool is_ArrayData()       const { return false; }
  virtual bool is_MultiBranchData() const { return false; }
  virtual bool is_ArgInfoData()     const { return false; }
  virtual bool is_CallTypeData()    const { return false; }
  virtual bool is_VirtualCallTypeData()const { return false; }
  virtual bool is_ParametersTypeData() const { return false; }
  virtual bool is_SpeculativeTrapData()const { return false; }


  BitData* as_BitData() const {
    assert(is_BitData(), "wrong type");
    return is_BitData()         ? (BitData*)        this : NULL;
  }
  CounterData* as_CounterData() const {
    assert(is_CounterData(), "wrong type");
    return is_CounterData()     ? (CounterData*)    this : NULL;
  }
  JumpData* as_JumpData() const {
    assert(is_JumpData(), "wrong type");
    return is_JumpData()        ? (JumpData*)       this : NULL;
  }
  ReceiverTypeData* as_ReceiverTypeData() const {
    assert(is_ReceiverTypeData(), "wrong type");
    return is_ReceiverTypeData() ? (ReceiverTypeData*)this : NULL;
  }
  VirtualCallData* as_VirtualCallData() const {
    assert(is_VirtualCallData(), "wrong type");
    return is_VirtualCallData() ? (VirtualCallData*)this : NULL;
  }
  RetData* as_RetData() const {
    assert(is_RetData(), "wrong type");
    return is_RetData()         ? (RetData*)        this : NULL;
  }
  BranchData* as_BranchData() const {
    assert(is_BranchData(), "wrong type");
    return is_BranchData()      ? (BranchData*)     this : NULL;
  }
  ArrayData* as_ArrayData() const {
    assert(is_ArrayData(), "wrong type");
    return is_ArrayData()       ? (ArrayData*)      this : NULL;
  }
  MultiBranchData* as_MultiBranchData() const {
    assert(is_MultiBranchData(), "wrong type");
    return is_MultiBranchData() ? (MultiBranchData*)this : NULL;
  }
  ArgInfoData* as_ArgInfoData() const {
    assert(is_ArgInfoData(), "wrong type");
    return is_ArgInfoData() ? (ArgInfoData*)this : NULL;
  }
  CallTypeData* as_CallTypeData() const {
    assert(is_CallTypeData(), "wrong type");
    return is_CallTypeData() ? (CallTypeData*)this : NULL;
  }
  VirtualCallTypeData* as_VirtualCallTypeData() const {
    assert(is_VirtualCallTypeData(), "wrong type");
    return is_VirtualCallTypeData() ? (VirtualCallTypeData*)this : NULL;
  }
  ParametersTypeData* as_ParametersTypeData() const {
    assert(is_ParametersTypeData(), "wrong type");
    return is_ParametersTypeData() ? (ParametersTypeData*)this : NULL;
  }
  SpeculativeTrapData* as_SpeculativeTrapData() const {
    assert(is_SpeculativeTrapData(), "wrong type");
    return is_SpeculativeTrapData() ? (SpeculativeTrapData*)this : NULL;
  }


  // Subclass specific initialization
  virtual void post_initialize(BytecodeStream* stream, MethodData* mdo) {}

  // GC support
  virtual void clean_weak_klass_links(bool always_clean) {}

  // CI translation: ProfileData can represent both MethodDataOop data
  // as well as CIMethodData data. This function is provided for translating
  // an oop in a ProfileData to the ci equivalent. Generally speaking,
  // most ProfileData don't require any translation, so we provide the null
  // translation here, and the required translators are in the ci subclasses.
  virtual void translate_from(const ProfileData* data) {}

  virtual void print_data_on(outputStream* st, const char* extra = NULL) const {
    ShouldNotReachHere();
  }

  void print_data_on(outputStream* st, const MethodData* md) const;

  void print_shared(outputStream* st, const char* name, const char* extra) const;
  void tab(outputStream* st, bool first = false) const;
};

// BitData
//
// A BitData holds a flag or two in its header.
class BitData : public ProfileData {
  friend class VMStructs;
  friend class JVMCIVMStructs;
protected:
  enum {
    // null_seen:
    //  saw a null operand (cast/aastore/instanceof)
      null_seen_flag              = DataLayout::first_flag + 0
#if INCLUDE_JVMCI
    // bytecode threw any exception
    , exception_seen_flag         = null_seen_flag + 1
#endif
  };
  enum { bit_cell_count = 0 };  // no additional data fields needed.
public:
  BitData(DataLayout* layout) : ProfileData(layout) {
  }

  virtual bool is_BitData() const { return true; }

  static int static_cell_count() {
    return bit_cell_count;
  }

  virtual int cell_count() const {
    return static_cell_count();
  }

  // Accessor

  // The null_seen flag bit is specially known to the interpreter.
  // Consulting it allows the compiler to avoid setting up null_check traps.
  bool null_seen()     { return flag_at(null_seen_flag); }
  void set_null_seen()    { set_flag_at(null_seen_flag); }

#if INCLUDE_JVMCI
  // true if an exception was thrown at the specific BCI
  bool exception_seen() { return flag_at(exception_seen_flag); }
  void set_exception_seen() { set_flag_at(exception_seen_flag); }
#endif

  // Code generation support
  static int null_seen_byte_constant() {
    return flag_number_to_constant(null_seen_flag);
  }

  static ByteSize bit_data_size() {
    return cell_offset(bit_cell_count);
  }

  void print_data_on(outputStream* st, const char* extra = NULL) const;
};

// CounterData
//
// A CounterData corresponds to a simple counter.
class CounterData : public BitData {
  friend class VMStructs;
  friend class JVMCIVMStructs;
protected:
  enum {
    count_off,
    counter_cell_count
  };
public:
  CounterData(DataLayout* layout) : BitData(layout) {}

  virtual bool is_CounterData() const { return true; }

  static int static_cell_count() {
    return counter_cell_count;
  }

  virtual int cell_count() const {
    return static_cell_count();
  }

  // Direct accessor
  int count() const {
    intptr_t raw_data = intptr_at(count_off);
    if (raw_data > max_jint) {
      raw_data = max_jint;
    } else if (raw_data < min_jint) {
      raw_data = min_jint;
    }
    return int(raw_data);
  }

  // Code generation support
  static ByteSize count_offset() {
    return cell_offset(count_off);
  }
  static ByteSize counter_data_size() {
    return cell_offset(counter_cell_count);
  }

  void set_count(int count) {
    set_int_at(count_off, count);
  }

  void print_data_on(outputStream* st, const char* extra = NULL) const;
};

// JumpData
//
// A JumpData is used to access profiling information for a direct
// branch.  It is a counter, used for counting the number of branches,
// plus a data displacement, used for realigning the data pointer to
// the corresponding target bci.
class JumpData : public ProfileData {
  friend class VMStructs;
  friend class JVMCIVMStructs;
protected:
  enum {
    taken_off_set,
    displacement_off_set,
    jump_cell_count
  };

  void set_displacement(int displacement) {
    set_int_at(displacement_off_set, displacement);
  }

public:
  JumpData(DataLayout* layout) : ProfileData(layout) {
    assert(layout->tag() == DataLayout::jump_data_tag ||
      layout->tag() == DataLayout::branch_data_tag, "wrong type");
  }

  virtual bool is_JumpData() const { return true; }

  static int static_cell_count() {
    return jump_cell_count;
  }

  virtual int cell_count() const {
    return static_cell_count();
  }

  // Direct accessor
  uint taken() const {
    return uint_at(taken_off_set);
  }

  void set_taken(uint cnt) {
    set_uint_at(taken_off_set, cnt);
  }

  // Saturating counter
  uint inc_taken() {
    uint cnt = taken() + 1;
    // Did we wrap? Will compiler screw us??
    if (cnt == 0) cnt--;
    set_uint_at(taken_off_set, cnt);
    return cnt;
  }

  int displacement() const {
    return int_at(displacement_off_set);
  }

  // Code generation support
  static ByteSize taken_offset() {
    return cell_offset(taken_off_set);
  }

  static ByteSize displacement_offset() {
    return cell_offset(displacement_off_set);
  }

  // Specific initialization.
  void post_initialize(BytecodeStream* stream, MethodData* mdo);

  void print_data_on(outputStream* st, const char* extra = NULL) const;
};

// Entries in a ProfileData object to record types: it can either be
// none (no profile), unknown (conflicting profile data) or a klass if
// a single one is seen. Whether a null reference was seen is also
// recorded. No counter is associated with the type and a single type
// is tracked (unlike VirtualCallData).
class TypeEntries {

public:

  // A single cell is used to record information for a type:
  // - the cell is initialized to 0
  // - when a type is discovered it is stored in the cell
  // - bit zero of the cell is used to record whether a null reference
  // was encountered or not
  // - bit 1 is set to record a conflict in the type information

  enum {
    null_seen = 1,
    type_mask = ~null_seen,
    type_unknown = 2,
    status_bits = null_seen | type_unknown,
    type_klass_mask = ~status_bits
  };

  // what to initialize a cell to
  static intptr_t type_none() {
    return 0;
  }

  // null seen = bit 0 set?
  static bool was_null_seen(intptr_t v) {
    return (v & null_seen) != 0;
  }

  // conflicting type information = bit 1 set?
  static bool is_type_unknown(intptr_t v) {
    return (v & type_unknown) != 0;
  }

  // not type information yet = all bits cleared, ignoring bit 0?
  static bool is_type_none(intptr_t v) {
    return (v & type_mask) == 0;
  }

  // recorded type: cell without bit 0 and 1
  static intptr_t klass_part(intptr_t v) {
    intptr_t r = v & type_klass_mask;
    return r;
  }

  // type recorded
  static Klass* valid_klass(intptr_t k) {
    if (!is_type_none(k) &&
        !is_type_unknown(k)) {
      Klass* res = (Klass*)klass_part(k);
      assert(res != NULL, "invalid");
      return res;
    } else {
      return NULL;
    }
  }

  static intptr_t with_status(intptr_t k, intptr_t in) {
    return k | (in & status_bits);
  }

  static intptr_t with_status(Klass* k, intptr_t in) {
    return with_status((intptr_t)k, in);
  }

  static void print_klass(outputStream* st, intptr_t k);

protected:
  // ProfileData object these entries are part of
  ProfileData* _pd;
  // offset within the ProfileData object where the entries start
  const int _base_off;

  TypeEntries(int base_off)
    : _pd(NULL), _base_off(base_off) {}

  void set_intptr_at(int index, intptr_t value) {
    _pd->set_intptr_at(index, value);
  }

  intptr_t intptr_at(int index) const {
    return _pd->intptr_at(index);
  }

public:
  void set_profile_data(ProfileData* pd) {
    _pd = pd;
  }
};

// Type entries used for arguments passed at a call and parameters on
// method entry. 2 cells per entry: one for the type encoded as in
// TypeEntries and one initialized with the stack slot where the
// profiled object is to be found so that the interpreter can locate
// it quickly.
class TypeStackSlotEntries : public TypeEntries {

private:
  enum {
    stack_slot_entry,
    type_entry,
    per_arg_cell_count
  };

  // offset of cell for stack slot for entry i within ProfileData object
  int stack_slot_offset(int i) const {
    return _base_off + stack_slot_local_offset(i);
  }

  const int _number_of_entries;

  // offset of cell for type for entry i within ProfileData object
  int type_offset_in_cells(int i) const {
    return _base_off + type_local_offset(i);
  }

public:

  TypeStackSlotEntries(int base_off, int nb_entries)
    : TypeEntries(base_off), _number_of_entries(nb_entries) {}

  static int compute_cell_count(Symbol* signature, bool include_receiver, int max);

  void post_initialize(Symbol* signature, bool has_receiver, bool include_receiver);

  int number_of_entries() const { return _number_of_entries; }

  // offset of cell for stack slot for entry i within this block of cells for a TypeStackSlotEntries
  static int stack_slot_local_offset(int i) {
    return i * per_arg_cell_count + stack_slot_entry;
  }

  // offset of cell for type for entry i within this block of cells for a TypeStackSlotEntries
  static int type_local_offset(int i) {
    return i * per_arg_cell_count + type_entry;
  }

  // stack slot for entry i
  uint stack_slot(int i) const {
    assert(i >= 0 && i < _number_of_entries, "oob");
    return _pd->uint_at(stack_slot_offset(i));
  }

  // set stack slot for entry i
  void set_stack_slot(int i, uint num) {
    assert(i >= 0 && i < _number_of_entries, "oob");
    _pd->set_uint_at(stack_slot_offset(i), num);
  }

  // type for entry i
  intptr_t type(int i) const {
    assert(i >= 0 && i < _number_of_entries, "oob");
    return _pd->intptr_at(type_offset_in_cells(i));
  }

  // set type for entry i
  void set_type(int i, intptr_t k) {
    assert(i >= 0 && i < _number_of_entries, "oob");
    _pd->set_intptr_at(type_offset_in_cells(i), k);
  }

  static ByteSize per_arg_size() {
    return in_ByteSize(per_arg_cell_count * DataLayout::cell_size);
  }

  static int per_arg_count() {
    return per_arg_cell_count;
  }

  ByteSize type_offset(int i) const {
    return DataLayout::cell_offset(type_offset_in_cells(i));
  }

  // GC support
  void clean_weak_klass_links(bool always_clean);

  void print_data_on(outputStream* st) const;
};

// Type entry used for return from a call. A single cell to record the
// type.
class ReturnTypeEntry : public TypeEntries {

private:
  enum {
    cell_count = 1
  };

public:
  ReturnTypeEntry(int base_off)
    : TypeEntries(base_off) {}

  void post_initialize() {
    set_type(type_none());
  }

  intptr_t type() const {
    return _pd->intptr_at(_base_off);
  }

  void set_type(intptr_t k) {
    _pd->set_intptr_at(_base_off, k);
  }

  static int static_cell_count() {
    return cell_count;
  }

  static ByteSize size() {
    return in_ByteSize(cell_count * DataLayout::cell_size);
  }

  ByteSize type_offset() {
    return DataLayout::cell_offset(_base_off);
  }

  // GC support
  void clean_weak_klass_links(bool always_clean);

  void print_data_on(outputStream* st) const;
};

// Entries to collect type information at a call: contains arguments
// (TypeStackSlotEntries), a return type (ReturnTypeEntry) and a
// number of cells. Because the number of cells for the return type is
// smaller than the number of cells for the type of an arguments, the
// number of cells is used to tell how many arguments are profiled and
// whether a return value is profiled. See has_arguments() and
// has_return().
class TypeEntriesAtCall {
private:
  static int stack_slot_local_offset(int i) {
    return header_cell_count() + TypeStackSlotEntries::stack_slot_local_offset(i);
  }

  static int argument_type_local_offset(int i) {
    return header_cell_count() + TypeStackSlotEntries::type_local_offset(i);
  }

public:

  static int header_cell_count() {
    return 1;
  }

  static int cell_count_local_offset() {
    return 0;
  }

  static int compute_cell_count(BytecodeStream* stream);

  static void initialize(DataLayout* dl, int base, int cell_count) {
    int off = base + cell_count_local_offset();
    dl->set_cell_at(off, cell_count - base - header_cell_count());
  }

  static bool arguments_profiling_enabled();
  static bool return_profiling_enabled();

  // Code generation support
  static ByteSize cell_count_offset() {
    return in_ByteSize(cell_count_local_offset() * DataLayout::cell_size);
  }

  static ByteSize args_data_offset() {
    return in_ByteSize(header_cell_count() * DataLayout::cell_size);
  }

  static ByteSize stack_slot_offset(int i) {
    return in_ByteSize(stack_slot_local_offset(i) * DataLayout::cell_size);
  }

  static ByteSize argument_type_offset(int i) {
    return in_ByteSize(argument_type_local_offset(i) * DataLayout::cell_size);
  }

  static ByteSize return_only_size() {
    return ReturnTypeEntry::size() + in_ByteSize(header_cell_count() * DataLayout::cell_size);
  }

};

// CallTypeData
//
// A CallTypeData is used to access profiling information about a non
// virtual call for which we collect type information about arguments
// and return value.
class CallTypeData : public CounterData {
private:
  // entries for arguments if any
  TypeStackSlotEntries _args;
  // entry for return type if any
  ReturnTypeEntry _ret;

  int cell_count_global_offset() const {
    return CounterData::static_cell_count() + TypeEntriesAtCall::cell_count_local_offset();
  }

  // number of cells not counting the header
  int cell_count_no_header() const {
    return uint_at(cell_count_global_offset());
  }

  void check_number_of_arguments(int total) {
    assert(number_of_arguments() == total, "should be set in DataLayout::initialize");
  }

public:
  CallTypeData(DataLayout* layout) :
    CounterData(layout),
    _args(CounterData::static_cell_count()+TypeEntriesAtCall::header_cell_count(), number_of_arguments()),
    _ret(cell_count() - ReturnTypeEntry::static_cell_count())
  {
    assert(layout->tag() == DataLayout::call_type_data_tag, "wrong type");
    // Some compilers (VC++) don't want this passed in member initialization list
    _args.set_profile_data(this);
    _ret.set_profile_data(this);
  }

  const TypeStackSlotEntries* args() const {
    assert(has_arguments(), "no profiling of arguments");
    return &_args;
  }

  const ReturnTypeEntry* ret() const {
    assert(has_return(), "no profiling of return value");
    return &_ret;
  }

  virtual bool is_CallTypeData() const { return true; }

  static int static_cell_count() {
    return -1;
  }

  static int compute_cell_count(BytecodeStream* stream) {
    return CounterData::static_cell_count() + TypeEntriesAtCall::compute_cell_count(stream);
  }

  static void initialize(DataLayout* dl, int cell_count) {
    TypeEntriesAtCall::initialize(dl, CounterData::static_cell_count(), cell_count);
  }

  virtual void post_initialize(BytecodeStream* stream, MethodData* mdo);

  virtual int cell_count() const {
    return CounterData::static_cell_count() +
      TypeEntriesAtCall::header_cell_count() +
      int_at_unchecked(cell_count_global_offset());
  }

  int number_of_arguments() const {
    return cell_count_no_header() / TypeStackSlotEntries::per_arg_count();
  }

  void set_argument_type(int i, Klass* k) {
    assert(has_arguments(), "no arguments!");
    intptr_t current = _args.type(i);
    _args.set_type(i, TypeEntries::with_status(k, current));
  }

  void set_return_type(Klass* k) {
    assert(has_return(), "no return!");
    intptr_t current = _ret.type();
    _ret.set_type(TypeEntries::with_status(k, current));
  }

  // An entry for a return value takes less space than an entry for an
  // argument so if the number of cells exceeds the number of cells
  // needed for an argument, this object contains type information for
  // at least one argument.
  bool has_arguments() const {
    bool res = cell_count_no_header() >= TypeStackSlotEntries::per_arg_count();
    assert (!res || TypeEntriesAtCall::arguments_profiling_enabled(), "no profiling of arguments");
    return res;
  }

  // An entry for a return value takes less space than an entry for an
  // argument, so if the remainder of the number of cells divided by
  // the number of cells for an argument is not null, a return value
  // is profiled in this object.
  bool has_return() const {
    bool res = (cell_count_no_header() % TypeStackSlotEntries::per_arg_count()) != 0;
    assert (!res || TypeEntriesAtCall::return_profiling_enabled(), "no profiling of return values");
    return res;
  }

  // Code generation support
  static ByteSize args_data_offset() {
    return cell_offset(CounterData::static_cell_count()) + TypeEntriesAtCall::args_data_offset();
  }

  ByteSize argument_type_offset(int i) {
    return _args.type_offset(i);
  }

  ByteSize return_type_offset() {
    return _ret.type_offset();
  }

  // GC support
  virtual void clean_weak_klass_links(bool always_clean) {
    if (has_arguments()) {
      _args.clean_weak_klass_links(always_clean);
    }
    if (has_return()) {
      _ret.clean_weak_klass_links(always_clean);
    }
  }

  virtual void print_data_on(outputStream* st, const char* extra = NULL) const;
};

// ReceiverTypeData
//
// A ReceiverTypeData is used to access profiling information about a
// dynamic type check.  It consists of a counter which counts the total times
// that the check is reached, and a series of (Klass*, count) pairs
// which are used to store a type profile for the receiver of the check.
class ReceiverTypeData : public CounterData {
  friend class VMStructs;
  friend class JVMCIVMStructs;
protected:
  enum {
#if INCLUDE_JVMCI
    // Description of the different counters
    // ReceiverTypeData for instanceof/checkcast/aastore:
    //   count is decremented for failed type checks
    //   JVMCI only: nonprofiled_count is incremented on type overflow
    // VirtualCallData for invokevirtual/invokeinterface:
    //   count is incremented on type overflow
    //   JVMCI only: nonprofiled_count is incremented on method overflow

    // JVMCI is interested in knowing the percentage of type checks involving a type not explicitly in the profile
    nonprofiled_count_off_set = counter_cell_count,
    receiver0_offset,
#else
    receiver0_offset = counter_cell_count,
#endif
    count0_offset,
    receiver_type_row_cell_count = (count0_offset + 1) - receiver0_offset
  };

public:
  ReceiverTypeData(DataLayout* layout) : CounterData(layout) {
    assert(layout->tag() == DataLayout::receiver_type_data_tag ||
           layout->tag() == DataLayout::virtual_call_data_tag ||
           layout->tag() == DataLayout::virtual_call_type_data_tag, "wrong type");
  }

  virtual bool is_ReceiverTypeData() const { return true; }

  static int static_cell_count() {
    return counter_cell_count + (uint) TypeProfileWidth * receiver_type_row_cell_count JVMCI_ONLY(+ 1);
  }

  virtual int cell_count() const {
    return static_cell_count();
  }

  // Direct accessors
  static uint row_limit() {
    return TypeProfileWidth;
  }
  static int receiver_cell_index(uint row) {
    return receiver0_offset + row * receiver_type_row_cell_count;
  }
  static int receiver_count_cell_index(uint row) {
    return count0_offset + row * receiver_type_row_cell_count;
  }

  Klass* receiver(uint row) const {
    assert(row < row_limit(), "oob");

    Klass* recv = (Klass*)intptr_at(receiver_cell_index(row));
    assert(recv == NULL || recv->is_klass(), "wrong type");
    return recv;
  }

  void set_receiver(uint row, Klass* k) {
    assert((uint)row < row_limit(), "oob");
    set_intptr_at(receiver_cell_index(row), (uintptr_t)k);
  }

  uint receiver_count(uint row) const {
    assert(row < row_limit(), "oob");
    return uint_at(receiver_count_cell_index(row));
  }

  void set_receiver_count(uint row, uint count) {
    assert(row < row_limit(), "oob");
    set_uint_at(receiver_count_cell_index(row), count);
  }

  void clear_row(uint row) {
    assert(row < row_limit(), "oob");
    // Clear total count - indicator of polymorphic call site.
    // The site may look like as monomorphic after that but
    // it allow to have more accurate profiling information because
    // there was execution phase change since klasses were unloaded.
    // If the site is still polymorphic then MDO will be updated
    // to reflect it. But it could be the case that the site becomes
    // only bimorphic. Then keeping total count not 0 will be wrong.
    // Even if we use monomorphic (when it is not) for compilation
    // we will only have trap, deoptimization and recompile again
    // with updated MDO after executing method in Interpreter.
    // An additional receiver will be recorded in the cleaned row
    // during next call execution.
    //
    // Note: our profiling logic works with empty rows in any slot.
    // We do sorting a profiling info (ciCallProfile) for compilation.
    //
    set_count(0);
    set_receiver(row, NULL);
    set_receiver_count(row, 0);
#if INCLUDE_JVMCI
    if (!this->is_VirtualCallData()) {
      // if this is a ReceiverTypeData for JVMCI, the nonprofiled_count
      // must also be reset (see "Description of the different counters" above)
      set_nonprofiled_count(0);
    }
#endif
  }

  // Code generation support
  static ByteSize receiver_offset(uint row) {
    return cell_offset(receiver_cell_index(row));
  }
  static ByteSize receiver_count_offset(uint row) {
    return cell_offset(receiver_count_cell_index(row));
  }
#if INCLUDE_JVMCI
  static ByteSize nonprofiled_receiver_count_offset() {
    return cell_offset(nonprofiled_count_off_set);
  }
  uint nonprofiled_count() const {
    return uint_at(nonprofiled_count_off_set);
  }
  void set_nonprofiled_count(uint count) {
    set_uint_at(nonprofiled_count_off_set, count);
  }
#endif // INCLUDE_JVMCI
  static ByteSize receiver_type_data_size() {
    return cell_offset(static_cell_count());
  }

  // GC support
  virtual void clean_weak_klass_links(bool always_clean);

  void print_receiver_data_on(outputStream* st) const;
  void print_data_on(outputStream* st, const char* extra = NULL) const;
};

// VirtualCallData
//
// A VirtualCallData is used to access profiling information about a
// virtual call.  For now, it has nothing more than a ReceiverTypeData.
class VirtualCallData : public ReceiverTypeData {
public:
  VirtualCallData(DataLayout* layout) : ReceiverTypeData(layout) {
    assert(layout->tag() == DataLayout::virtual_call_data_tag ||
           layout->tag() == DataLayout::virtual_call_type_data_tag, "wrong type");
  }

  virtual bool is_VirtualCallData() const { return true; }

  static int static_cell_count() {
    // At this point we could add more profile state, e.g., for arguments.
    // But for now it's the same size as the base record type.
    return ReceiverTypeData::static_cell_count();
  }

  virtual int cell_count() const {
    return static_cell_count();
  }

  // Direct accessors
  static ByteSize virtual_call_data_size() {
    return cell_offset(static_cell_count());
  }

  void print_method_data_on(outputStream* st) const NOT_JVMCI_RETURN;
  void print_data_on(outputStream* st, const char* extra = NULL) const;
};

// VirtualCallTypeData
//
// A VirtualCallTypeData is used to access profiling information about
// a virtual call for which we collect type information about
// arguments and return value.
class VirtualCallTypeData : public VirtualCallData {
private:
  // entries for arguments if any
  TypeStackSlotEntries _args;
  // entry for return type if any
  ReturnTypeEntry _ret;

  int cell_count_global_offset() const {
    return VirtualCallData::static_cell_count() + TypeEntriesAtCall::cell_count_local_offset();
  }

  // number of cells not counting the header
  int cell_count_no_header() const {
    return uint_at(cell_count_global_offset());
  }

  void check_number_of_arguments(int total) {
    assert(number_of_arguments() == total, "should be set in DataLayout::initialize");
  }

public:
  VirtualCallTypeData(DataLayout* layout) :
    VirtualCallData(layout),
    _args(VirtualCallData::static_cell_count()+TypeEntriesAtCall::header_cell_count(), number_of_arguments()),
    _ret(cell_count() - ReturnTypeEntry::static_cell_count())
  {
    assert(layout->tag() == DataLayout::virtual_call_type_data_tag, "wrong type");
    // Some compilers (VC++) don't want this passed in member initialization list
    _args.set_profile_data(this);
    _ret.set_profile_data(this);
  }

  const TypeStackSlotEntries* args() const {
    assert(has_arguments(), "no profiling of arguments");
    return &_args;
  }

  const ReturnTypeEntry* ret() const {
    assert(has_return(), "no profiling of return value");
    return &_ret;
  }

  virtual bool is_VirtualCallTypeData() const { return true; }

  static int static_cell_count() {
    return -1;
  }

  static int compute_cell_count(BytecodeStream* stream) {
    return VirtualCallData::static_cell_count() + TypeEntriesAtCall::compute_cell_count(stream);
  }

  static void initialize(DataLayout* dl, int cell_count) {
    TypeEntriesAtCall::initialize(dl, VirtualCallData::static_cell_count(), cell_count);
  }

  virtual void post_initialize(BytecodeStream* stream, MethodData* mdo);

  virtual int cell_count() const {
    return VirtualCallData::static_cell_count() +
      TypeEntriesAtCall::header_cell_count() +
      int_at_unchecked(cell_count_global_offset());
  }

  int number_of_arguments() const {
    return cell_count_no_header() / TypeStackSlotEntries::per_arg_count();
  }

  void set_argument_type(int i, Klass* k) {
    assert(has_arguments(), "no arguments!");
    intptr_t current = _args.type(i);
    _args.set_type(i, TypeEntries::with_status(k, current));
  }

  void set_return_type(Klass* k) {
    assert(has_return(), "no return!");
    intptr_t current = _ret.type();
    _ret.set_type(TypeEntries::with_status(k, current));
  }

  // An entry for a return value takes less space than an entry for an
  // argument, so if the remainder of the number of cells divided by
  // the number of cells for an argument is not null, a return value
  // is profiled in this object.
  bool has_return() const {
    bool res = (cell_count_no_header() % TypeStackSlotEntries::per_arg_count()) != 0;
    assert (!res || TypeEntriesAtCall::return_profiling_enabled(), "no profiling of return values");
    return res;
  }

  // An entry for a return value takes less space than an entry for an
  // argument so if the number of cells exceeds the number of cells
  // needed for an argument, this object contains type information for
  // at least one argument.
  bool has_arguments() const {
    bool res = cell_count_no_header() >= TypeStackSlotEntries::per_arg_count();
    assert (!res || TypeEntriesAtCall::arguments_profiling_enabled(), "no profiling of arguments");
    return res;
  }

  // Code generation support
  static ByteSize args_data_offset() {
    return cell_offset(VirtualCallData::static_cell_count()) + TypeEntriesAtCall::args_data_offset();
  }

  ByteSize argument_type_offset(int i) {
    return _args.type_offset(i);
  }

  ByteSize return_type_offset() {
    return _ret.type_offset();
  }

  // GC support
  virtual void clean_weak_klass_links(bool always_clean) {
    ReceiverTypeData::clean_weak_klass_links(always_clean);
    if (has_arguments()) {
      _args.clean_weak_klass_links(always_clean);
    }
    if (has_return()) {
      _ret.clean_weak_klass_links(always_clean);
    }
  }

  virtual void print_data_on(outputStream* st, const char* extra = NULL) const;
};

// RetData
//
// A RetData is used to access profiling information for a ret bytecode.
// It is composed of a count of the number of times that the ret has
// been executed, followed by a series of triples of the form
// (bci, count, di) which count the number of times that some bci was the
// target of the ret and cache a corresponding data displacement.
class RetData : public CounterData {
protected:
  enum {
    bci0_offset = counter_cell_count,
    count0_offset,
    displacement0_offset,
    ret_row_cell_count = (displacement0_offset + 1) - bci0_offset
  };

  void set_bci(uint row, int bci) {
    assert((uint)row < row_limit(), "oob");
    set_int_at(bci0_offset + row * ret_row_cell_count, bci);
  }
  void release_set_bci(uint row, int bci);
  void set_bci_count(uint row, uint count) {
    assert((uint)row < row_limit(), "oob");
    set_uint_at(count0_offset + row * ret_row_cell_count, count);
  }
  void set_bci_displacement(uint row, int disp) {
    set_int_at(displacement0_offset + row * ret_row_cell_count, disp);
  }

public:
  RetData(DataLayout* layout) : CounterData(layout) {
    assert(layout->tag() == DataLayout::ret_data_tag, "wrong type");
  }

  virtual bool is_RetData() const { return true; }

  enum {
    no_bci = -1 // value of bci when bci1/2 are not in use.
  };

  static int static_cell_count() {
    return counter_cell_count + (uint) BciProfileWidth * ret_row_cell_count;
  }

  virtual int cell_count() const {
    return static_cell_count();
  }

  static uint row_limit() {
    return BciProfileWidth;
  }
  static int bci_cell_index(uint row) {
    return bci0_offset + row * ret_row_cell_count;
  }
  static int bci_count_cell_index(uint row) {
    return count0_offset + row * ret_row_cell_count;
  }
  static int bci_displacement_cell_index(uint row) {
    return displacement0_offset + row * ret_row_cell_count;
  }

  // Direct accessors
  int bci(uint row) const {
    return int_at(bci_cell_index(row));
  }
  uint bci_count(uint row) const {
    return uint_at(bci_count_cell_index(row));
  }
  int bci_displacement(uint row) const {
    return int_at(bci_displacement_cell_index(row));
  }

  // Interpreter Runtime support
  address fixup_ret(int return_bci, MethodData* mdo);

  // Code generation support
  static ByteSize bci_offset(uint row) {
    return cell_offset(bci_cell_index(row));
  }
  static ByteSize bci_count_offset(uint row) {
    return cell_offset(bci_count_cell_index(row));
  }
  static ByteSize bci_displacement_offset(uint row) {
    return cell_offset(bci_displacement_cell_index(row));
  }

  // Specific initialization.
  void post_initialize(BytecodeStream* stream, MethodData* mdo);

  void print_data_on(outputStream* st, const char* extra = NULL) const;
};

// BranchData
//
// A BranchData is used to access profiling data for a two-way branch.
// It consists of taken and not_taken counts as well as a data displacement
// for the taken case.
class BranchData : public JumpData {
  friend class VMStructs;
  friend class JVMCIVMStructs;
protected:
  enum {
    not_taken_off_set = jump_cell_count,
    branch_cell_count
  };

  void set_displacement(int displacement) {
    set_int_at(displacement_off_set, displacement);
  }

public:
  BranchData(DataLayout* layout) : JumpData(layout) {
    assert(layout->tag() == DataLayout::branch_data_tag, "wrong type");
  }

  virtual bool is_BranchData() const { return true; }

  static int static_cell_count() {
    return branch_cell_count;
  }

  virtual int cell_count() const {
    return static_cell_count();
  }

  // Direct accessor
  uint not_taken() const {
    return uint_at(not_taken_off_set);
  }

  void set_not_taken(uint cnt) {
    set_uint_at(not_taken_off_set, cnt);
  }

  uint inc_not_taken() {
    uint cnt = not_taken() + 1;
    // Did we wrap? Will compiler screw us??
    if (cnt == 0) cnt--;
    set_uint_at(not_taken_off_set, cnt);
    return cnt;
  }

  // Code generation support
  static ByteSize not_taken_offset() {
    return cell_offset(not_taken_off_set);
  }
  static ByteSize branch_data_size() {
    return cell_offset(branch_cell_count);
  }

  // Specific initialization.
  void post_initialize(BytecodeStream* stream, MethodData* mdo);

  void print_data_on(outputStream* st, const char* extra = NULL) const;
};

// ArrayData
//
// A ArrayData is a base class for accessing profiling data which does
// not have a statically known size.  It consists of an array length
// and an array start.
class ArrayData : public ProfileData {
  friend class VMStructs;
  friend class JVMCIVMStructs;
protected:
  friend class DataLayout;

  enum {
    array_len_off_set,
    array_start_off_set
  };

  uint array_uint_at(int index) const {
    int aindex = index + array_start_off_set;
    return uint_at(aindex);
  }
  int array_int_at(int index) const {
    int aindex = index + array_start_off_set;
    return int_at(aindex);
  }
  oop array_oop_at(int index) const {
    int aindex = index + array_start_off_set;
    return oop_at(aindex);
  }
  void array_set_int_at(int index, int value) {
    int aindex = index + array_start_off_set;
    set_int_at(aindex, value);
  }

  // Code generation support for subclasses.
  static ByteSize array_element_offset(int index) {
    return cell_offset(array_start_off_set + index);
  }

public:
  ArrayData(DataLayout* layout) : ProfileData(layout) {}

  virtual bool is_ArrayData() const { return true; }

  static int static_cell_count() {
    return -1;
  }

  int array_len() const {
    return int_at_unchecked(array_len_off_set);
  }

  virtual int cell_count() const {
    return array_len() + 1;
  }

  // Code generation support
  static ByteSize array_len_offset() {
    return cell_offset(array_len_off_set);
  }
  static ByteSize array_start_offset() {
    return cell_offset(array_start_off_set);
  }
};

// MultiBranchData
//
// A MultiBranchData is used to access profiling information for
// a multi-way branch (*switch bytecodes).  It consists of a series
// of (count, displacement) pairs, which count the number of times each
// case was taken and specify the data displacment for each branch target.
class MultiBranchData : public ArrayData {
  friend class VMStructs;
  friend class JVMCIVMStructs;
protected:
  enum {
    default_count_off_set,
    default_disaplacement_off_set,
    case_array_start
  };
  enum {
    relative_count_off_set,
    relative_displacement_off_set,
    per_case_cell_count
  };

  void set_default_displacement(int displacement) {
    array_set_int_at(default_disaplacement_off_set, displacement);
  }
  void set_displacement_at(int index, int displacement) {
    array_set_int_at(case_array_start +
                     index * per_case_cell_count +
                     relative_displacement_off_set,
                     displacement);
  }

public:
  MultiBranchData(DataLayout* layout) : ArrayData(layout) {
    assert(layout->tag() == DataLayout::multi_branch_data_tag, "wrong type");
  }

  virtual bool is_MultiBranchData() const { return true; }

  static int compute_cell_count(BytecodeStream* stream);

  int number_of_cases() const {
    int alen = array_len() - 2; // get rid of default case here.
    assert(alen % per_case_cell_count == 0, "must be even");
    return (alen / per_case_cell_count);
  }

  uint default_count() const {
    return array_uint_at(default_count_off_set);
  }
  int default_displacement() const {
    return array_int_at(default_disaplacement_off_set);
  }

  uint count_at(int index) const {
    return array_uint_at(case_array_start +
                         index * per_case_cell_count +
                         relative_count_off_set);
  }
  int displacement_at(int index) const {
    return array_int_at(case_array_start +
                        index * per_case_cell_count +
                        relative_displacement_off_set);
  }

  // Code generation support
  static ByteSize default_count_offset() {
    return array_element_offset(default_count_off_set);
  }
  static ByteSize default_displacement_offset() {
    return array_element_offset(default_disaplacement_off_set);
  }
  static ByteSize case_count_offset(int index) {
    return case_array_offset() +
           (per_case_size() * index) +
           relative_count_offset();
  }
  static ByteSize case_array_offset() {
    return array_element_offset(case_array_start);
  }
  static ByteSize per_case_size() {
    return in_ByteSize(per_case_cell_count) * cell_size;
  }
  static ByteSize relative_count_offset() {
    return in_ByteSize(relative_count_off_set) * cell_size;
  }
  static ByteSize relative_displacement_offset() {
    return in_ByteSize(relative_displacement_off_set) * cell_size;
  }

  // Specific initialization.
  void post_initialize(BytecodeStream* stream, MethodData* mdo);

  void print_data_on(outputStream* st, const char* extra = NULL) const;
};

class ArgInfoData : public ArrayData {

public:
  ArgInfoData(DataLayout* layout) : ArrayData(layout) {
    assert(layout->tag() == DataLayout::arg_info_data_tag, "wrong type");
  }

  virtual bool is_ArgInfoData() const { return true; }


  int number_of_args() const {
    return array_len();
  }

  uint arg_modified(int arg) const {
    return array_uint_at(arg);
  }

  void set_arg_modified(int arg, uint val) {
    array_set_int_at(arg, val);
  }

  void print_data_on(outputStream* st, const char* extra = NULL) const;
};

// ParametersTypeData
//
// A ParametersTypeData is used to access profiling information about
// types of parameters to a method
class ParametersTypeData : public ArrayData {

private:
  TypeStackSlotEntries _parameters;

  static int stack_slot_local_offset(int i) {
    assert_profiling_enabled();
    return array_start_off_set + TypeStackSlotEntries::stack_slot_local_offset(i);
  }

  static int type_local_offset(int i) {
    assert_profiling_enabled();
    return array_start_off_set + TypeStackSlotEntries::type_local_offset(i);
  }

  static bool profiling_enabled();
  static void assert_profiling_enabled() {
    assert(profiling_enabled(), "method parameters profiling should be on");
  }

public:
  ParametersTypeData(DataLayout* layout) : ArrayData(layout), _parameters(1, number_of_parameters()) {
    assert(layout->tag() == DataLayout::parameters_type_data_tag, "wrong type");
    // Some compilers (VC++) don't want this passed in member initialization list
    _parameters.set_profile_data(this);
  }

  static int compute_cell_count(Method* m);

  virtual bool is_ParametersTypeData() const { return true; }

  virtual void post_initialize(BytecodeStream* stream, MethodData* mdo);

  int number_of_parameters() const {
    return array_len() / TypeStackSlotEntries::per_arg_count();
  }

  const TypeStackSlotEntries* parameters() const { return &_parameters; }

  uint stack_slot(int i) const {
    return _parameters.stack_slot(i);
  }

  void set_type(int i, Klass* k) {
    intptr_t current = _parameters.type(i);
    _parameters.set_type(i, TypeEntries::with_status((intptr_t)k, current));
  }

  virtual void clean_weak_klass_links(bool always_clean) {
    _parameters.clean_weak_klass_links(always_clean);
  }

  virtual void print_data_on(outputStream* st, const char* extra = NULL) const;

  static ByteSize stack_slot_offset(int i) {
    return cell_offset(stack_slot_local_offset(i));
  }

  static ByteSize type_offset(int i) {
    return cell_offset(type_local_offset(i));
  }
};

// SpeculativeTrapData
//
// A SpeculativeTrapData is used to record traps due to type
// speculation. It records the root of the compilation: that type
// speculation is wrong in the context of one compilation (for
// method1) doesn't mean it's wrong in the context of another one (for
// method2). Type speculation could have more/different data in the
// context of the compilation of method2 and it's worthwhile to try an
// optimization that failed for compilation of method1 in the context
// of compilation of method2.
// Space for SpeculativeTrapData entries is allocated from the extra
// data space in the MDO. If we run out of space, the trap data for
// the ProfileData at that bci is updated.
class SpeculativeTrapData : public ProfileData {
protected:
  enum {
    speculative_trap_method,
#ifndef _LP64
    // The size of the area for traps is a multiple of the header
    // size, 2 cells on 32 bits. Packed at the end of this area are
    // argument info entries (with tag
    // DataLayout::arg_info_data_tag). The logic in
    // MethodData::bci_to_extra_data() that guarantees traps don't
    // overflow over argument info entries assumes the size of a
    // SpeculativeTrapData is twice the header size. On 32 bits, a
    // SpeculativeTrapData must be 4 cells.
    padding,
#endif
    speculative_trap_cell_count
  };
public:
  SpeculativeTrapData(DataLayout* layout) : ProfileData(layout) {
    assert(layout->tag() == DataLayout::speculative_trap_data_tag, "wrong type");
  }

  virtual bool is_SpeculativeTrapData() const { return true; }

  static int static_cell_count() {
    return speculative_trap_cell_count;
  }

  virtual int cell_count() const {
    return static_cell_count();
  }

  // Direct accessor
  Method* method() const {
    return (Method*)intptr_at(speculative_trap_method);
  }

  void set_method(Method* m) {
    assert(!m->is_old(), "cannot add old methods");
    set_intptr_at(speculative_trap_method, (intptr_t)m);
  }

  static ByteSize method_offset() {
    return cell_offset(speculative_trap_method);
  }

  virtual void print_data_on(outputStream* st, const char* extra = NULL) const;
};

// MethodData*
//
// A MethodData* holds information which has been collected about
// a method.  Its layout looks like this:
//
// -----------------------------
// | header                    |
// | klass                     |
// -----------------------------
// | method                    |
// | size of the MethodData* |
// -----------------------------
// | Data entries...           |
// |   (variable size)         |
// |                           |
// .                           .
// .                           .
// .                           .
// |                           |
// -----------------------------
//
// The data entry area is a heterogeneous array of DataLayouts. Each
// DataLayout in the array corresponds to a specific bytecode in the
// method.  The entries in the array are sorted by the corresponding
// bytecode.  Access to the data is via resource-allocated ProfileData,
// which point to the underlying blocks of DataLayout structures.
//
// During interpretation, if profiling in enabled, the interpreter
// maintains a method data pointer (mdp), which points at the entry
// in the array corresponding to the current bci.  In the course of
// intepretation, when a bytecode is encountered that has profile data
// associated with it, the entry pointed to by mdp is updated, then the
// mdp is adjusted to point to the next appropriate DataLayout.  If mdp
// is NULL to begin with, the interpreter assumes that the current method
// is not (yet) being profiled.
//
// In MethodData* parlance, "dp" is a "data pointer", the actual address
// of a DataLayout element.  A "di" is a "data index", the offset in bytes
// from the base of the data entry array.  A "displacement" is the byte offset
// in certain ProfileData objects that indicate the amount the mdp must be
// adjusted in the event of a change in control flow.
//

class CleanExtraDataClosure : public StackObj {
public:
  virtual bool is_live(Method* m) = 0;
};


#if INCLUDE_JVMCI
// Encapsulates an encoded speculation reason. These are linked together in
// a list that is atomically appended to during deoptimization. Entries are
// never removed from the list.
// @see jdk.vm.ci.hotspot.HotSpotSpeculationLog.HotSpotSpeculationEncoding
class FailedSpeculation: public CHeapObj<mtCompiler> {
 private:
  // The length of HotSpotSpeculationEncoding.toByteArray(). The data itself
  // is an array embedded at the end of this object.
  int   _data_len;

  // Next entry in a linked list.
  FailedSpeculation* _next;

  FailedSpeculation(address data, int data_len);

  FailedSpeculation** next_adr() { return &_next; }

  // Placement new operator for inlining the speculation data into
  // the FailedSpeculation object.
  void* operator new(size_t size, size_t fs_size) throw();

 public:
  char* data()         { return (char*)(((address) this) + sizeof(FailedSpeculation)); }
  int data_len() const { return _data_len; }
  FailedSpeculation* next() const { return _next; }

  // Atomically appends a speculation from nm to the list whose head is at (*failed_speculations_address).
  // Returns false if the FailedSpeculation object could not be allocated.
  static bool add_failed_speculation(nmethod* nm, FailedSpeculation** failed_speculations_address, address speculation, int speculation_len);

  // Frees all entries in the linked list whose head is at (*failed_speculations_address).
  static void free_failed_speculations(FailedSpeculation** failed_speculations_address);
};
#endif

class ciMethodData;

class MethodData : public Metadata {
  friend class VMStructs;
  friend class JVMCIVMStructs;
private:
  friend class ProfileData;
  friend class TypeEntriesAtCall;
  friend class ciMethodData;

  // If you add a new field that points to any metaspace object, you
  // must add this field to MethodData::metaspace_pointers_do().

  // Back pointer to the Method*
  Method* _method;

  // Size of this oop in bytes
  int _size;

  // Cached hint for bci_to_dp and bci_to_data
  int _hint_di;

  Mutex _extra_data_lock;

  MethodData(const methodHandle& method);
public:
  static MethodData* allocate(ClassLoaderData* loader_data, const methodHandle& method, TRAPS);

  virtual bool is_methodData() const { return true; }
  void initialize();

  // Whole-method sticky bits and flags
  enum {
    _trap_hist_limit    = 25 JVMCI_ONLY(+5),   // decoupled from Deoptimization::Reason_LIMIT
    _trap_hist_mask     = max_jubyte,
    _extra_data_count   = 4     // extra DataLayout headers, for trap history
  }; // Public flag values

  // Compiler-related counters.
  class CompilerCounters {
    friend class VMStructs;
    friend class JVMCIVMStructs;

    uint _nof_decompiles;             // count of all nmethod removals
    uint _nof_overflow_recompiles;    // recompile count, excluding recomp. bits
    uint _nof_overflow_traps;         // trap count, excluding _trap_hist
    union {
      intptr_t _align;
      u1 _array[JVMCI_ONLY(2 *) MethodData::_trap_hist_limit];
    } _trap_hist;

  public:
    CompilerCounters() : _nof_decompiles(0), _nof_overflow_recompiles(0), _nof_overflow_traps(0) {
#ifndef ZERO
      // Some Zero platforms do not have expected alignment, and do not use
      // this code. static_assert would still fire and fail for them.
      static_assert(sizeof(_trap_hist) % HeapWordSize == 0, "align");
#endif
      uint size_in_words = sizeof(_trap_hist) / HeapWordSize;
      Copy::zero_to_words((HeapWord*) &_trap_hist, size_in_words);
    }

    // Return (uint)-1 for overflow.
    uint trap_count(int reason) const {
      assert((uint)reason < JVMCI_ONLY(2*) _trap_hist_limit, "oob");
      return (int)((_trap_hist._array[reason]+1) & _trap_hist_mask) - 1;
    }

    uint inc_trap_count(int reason) {
      // Count another trap, anywhere in this method.
      assert(reason >= 0, "must be single trap");
      assert((uint)reason < JVMCI_ONLY(2*) _trap_hist_limit, "oob");
      uint cnt1 = 1 + _trap_hist._array[reason];
      if ((cnt1 & _trap_hist_mask) != 0) {  // if no counter overflow...
        _trap_hist._array[reason] = cnt1;
        return cnt1;
      } else {
        return _trap_hist_mask + (++_nof_overflow_traps);
      }
    }

    uint overflow_trap_count() const {
      return _nof_overflow_traps;
    }
    uint overflow_recompile_count() const {
      return _nof_overflow_recompiles;
    }
    uint inc_overflow_recompile_count() {
      return ++_nof_overflow_recompiles;
    }
    uint decompile_count() const {
      return _nof_decompiles;
    }
    uint inc_decompile_count() {
      return ++_nof_decompiles;
    }

    // Support for code generation
    static ByteSize trap_history_offset() {
      return byte_offset_of(CompilerCounters, _trap_hist._array);
    }
  };

private:
  CompilerCounters _compiler_counters;

  // Support for interprocedural escape analysis, from Thomas Kotzmann.
  intx              _eflags;          // flags on escape information
  intx              _arg_local;       // bit set of non-escaping arguments
  intx              _arg_stack;       // bit set of stack-allocatable arguments
  intx              _arg_returned;    // bit set of returned arguments

  int               _creation_mileage; // method mileage at MDO creation

  // How many invocations has this MDO seen?
  // These counters are used to determine the exact age of MDO.
  // We need those because in tiered a method can be concurrently
  // executed at different levels.
  InvocationCounter _invocation_counter;
  // Same for backedges.
  InvocationCounter _backedge_counter;
  // Counter values at the time profiling started.
  int               _invocation_counter_start;
  int               _backedge_counter_start;
  uint              _tenure_traps;
  int               _invoke_mask;      // per-method Tier0InvokeNotifyFreqLog
  int               _backedge_mask;    // per-method Tier0BackedgeNotifyFreqLog

#if INCLUDE_RTM_OPT
  // State of RTM code generation during compilation of the method
  int               _rtm_state;
#endif

  // Number of loops and blocks is computed when compiling the first
  // time with C1. It is used to determine if method is trivial.
  short             _num_loops;
  short             _num_blocks;
  // Does this method contain anything worth profiling?
  enum WouldProfile {unknown, no_profile, profile};
  WouldProfile      _would_profile;

#if INCLUDE_JVMCI
  // Support for HotSpotMethodData.setCompiledIRSize(int)
  int                _jvmci_ir_size;
  FailedSpeculation* _failed_speculations;
#endif

  // Size of _data array in bytes.  (Excludes header and extra_data fields.)
  int _data_size;

  // data index for the area dedicated to parameters. -1 if no
  // parameter profiling.
  enum { no_parameters = -2, parameters_uninitialized = -1 };
  int _parameters_type_data_di;

  // Beginning of the data entries
  intptr_t _data[1];

  // Helper for size computation
  static int compute_data_size(BytecodeStream* stream);
  static int bytecode_cell_count(Bytecodes::Code code);
  static bool is_speculative_trap_bytecode(Bytecodes::Code code);
  enum { no_profile_data = -1, variable_cell_count = -2 };

  // Helper for initialization
  DataLayout* data_layout_at(int data_index) const {
    assert(data_index % sizeof(intptr_t) == 0, "unaligned");
    return (DataLayout*) (((address)_data) + data_index);
  }

  // Initialize an individual data segment.  Returns the size of
  // the segment in bytes.
  int initialize_data(BytecodeStream* stream, int data_index);

  // Helper for data_at
  DataLayout* limit_data_position() const {
    return data_layout_at(_data_size);
  }
  bool out_of_bounds(int data_index) const {
    return data_index >= data_size();
  }

  // Give each of the data entries a chance to perform specific
  // data initialization.
  void post_initialize(BytecodeStream* stream);

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
  int first_di() const { return 0; }

  ProfileData* bci_to_extra_data_helper(int bci, Method* m, DataLayout*& dp, bool concurrent);
  // Find or create an extra ProfileData:
  ProfileData* bci_to_extra_data(int bci, Method* m, bool create_if_missing);

  // return the argument info cell
  ArgInfoData *arg_info();

  enum {
    no_type_profile = 0,
    type_profile_jsr292 = 1,
    type_profile_all = 2
  };

  static bool profile_jsr292(const methodHandle& m, int bci);
  static bool profile_unsafe(const methodHandle& m, int bci);
  static bool profile_memory_access(const methodHandle& m, int bci);
  static int profile_arguments_flag();
  static bool profile_all_arguments();
  static bool profile_arguments_for_invoke(const methodHandle& m, int bci);
  static int profile_return_flag();
  static bool profile_all_return();
  static bool profile_return_for_invoke(const methodHandle& m, int bci);
  static int profile_parameters_flag();
  static bool profile_parameters_jsr292_only();
  static bool profile_all_parameters();

  void clean_extra_data_helper(DataLayout* dp, int shift, bool reset = false);
  void verify_extra_data_clean(CleanExtraDataClosure* cl);

public:
  void clean_extra_data(CleanExtraDataClosure* cl);

  static int header_size() {
    return sizeof(MethodData)/wordSize;
  }

  // Compute the size of a MethodData* before it is created.
  static int compute_allocation_size_in_bytes(const methodHandle& method);
  static int compute_allocation_size_in_words(const methodHandle& method);
  static int compute_extra_data_count(int data_size, int empty_bc_count, bool needs_speculative_traps);

  // Determine if a given bytecode can have profile information.
  static bool bytecode_has_profile(Bytecodes::Code code) {
    return bytecode_cell_count(code) != no_profile_data;
  }

  // reset into original state
  void init();

  // My size
  int size_in_bytes() const { return _size; }
  int size() const    { return align_metadata_size(align_up(_size, BytesPerWord)/BytesPerWord); }

  int      creation_mileage() const { return _creation_mileage; }
  void set_creation_mileage(int x)  { _creation_mileage = x; }

  int invocation_count() {
    if (invocation_counter()->carry()) {
      return InvocationCounter::count_limit;
    }
    return invocation_counter()->count();
  }
  int backedge_count() {
    if (backedge_counter()->carry()) {
      return InvocationCounter::count_limit;
    }
    return backedge_counter()->count();
  }

  int invocation_count_start() {
    if (invocation_counter()->carry()) {
      return 0;
    }
    return _invocation_counter_start;
  }

  int backedge_count_start() {
    if (backedge_counter()->carry()) {
      return 0;
    }
    return _backedge_counter_start;
  }

  int invocation_count_delta() { return invocation_count() - invocation_count_start(); }
  int backedge_count_delta()   { return backedge_count()   - backedge_count_start();   }

  void reset_start_counters() {
    _invocation_counter_start = invocation_count();
    _backedge_counter_start = backedge_count();
  }

  InvocationCounter* invocation_counter()     { return &_invocation_counter; }
  InvocationCounter* backedge_counter()       { return &_backedge_counter;   }

#if INCLUDE_JVMCI
  FailedSpeculation** get_failed_speculations_address() {
    return &_failed_speculations;
  }
#endif

#if INCLUDE_RTM_OPT
  int rtm_state() const {
    return _rtm_state;
  }
  void set_rtm_state(RTMState rstate) {
    _rtm_state = (int)rstate;
  }
  void atomic_set_rtm_state(RTMState rstate) {
    Atomic::store(&_rtm_state, (int)rstate);
  }

  static int rtm_state_offset_in_bytes() {
    return offset_of(MethodData, _rtm_state);
  }
#endif

  void set_would_profile(bool p)              { _would_profile = p ? profile : no_profile; }
  bool would_profile() const                  { return _would_profile != no_profile; }

  int num_loops() const                       { return _num_loops;  }
  void set_num_loops(int n)                   { _num_loops = n;     }
  int num_blocks() const                      { return _num_blocks; }
  void set_num_blocks(int n)                  { _num_blocks = n;    }

  bool is_mature() const;  // consult mileage and ProfileMaturityPercentage
  static int mileage_of(Method* m);

  // Support for interprocedural escape analysis, from Thomas Kotzmann.
  enum EscapeFlag {
    estimated    = 1 << 0,
    return_local = 1 << 1,
    return_allocated = 1 << 2,
    allocated_escapes = 1 << 3,
    unknown_modified = 1 << 4
  };

  intx eflags()                                  { return _eflags; }
  intx arg_local()                               { return _arg_local; }
  intx arg_stack()                               { return _arg_stack; }
  intx arg_returned()                            { return _arg_returned; }
  uint arg_modified(int a)                       { ArgInfoData *aid = arg_info();
                                                   assert(aid != NULL, "arg_info must be not null");
                                                   assert(a >= 0 && a < aid->number_of_args(), "valid argument number");
                                                   return aid->arg_modified(a); }

  void set_eflags(intx v)                        { _eflags = v; }
  void set_arg_local(intx v)                     { _arg_local = v; }
  void set_arg_stack(intx v)                     { _arg_stack = v; }
  void set_arg_returned(intx v)                  { _arg_returned = v; }
  void set_arg_modified(int a, uint v)           { ArgInfoData *aid = arg_info();
                                                   assert(aid != NULL, "arg_info must be not null");
                                                   assert(a >= 0 && a < aid->number_of_args(), "valid argument number");
                                                   aid->set_arg_modified(a, v); }

  void clear_escape_info()                       { _eflags = _arg_local = _arg_stack = _arg_returned = 0; }

  // Location and size of data area
  address data_base() const {
    return (address) _data;
  }
  int data_size() const {
    return _data_size;
  }

  int parameters_size_in_bytes() const {
    ParametersTypeData* param = parameters_type_data();
    return param == NULL ? 0 : param->size_in_bytes();
  }

  // Accessors
  Method* method() const { return _method; }

  // Get the data at an arbitrary (sort of) data index.
  ProfileData* data_at(int data_index) const;

  // Walk through the data in order.
  ProfileData* first_data() const { return data_at(first_di()); }
  ProfileData* next_data(ProfileData* current) const;
  DataLayout*  next_data_layout(DataLayout* current) const;
  bool is_valid(ProfileData* current) const { return current != NULL; }
  bool is_valid(DataLayout*  current) const { return current != NULL; }

  // Convert a dp (data pointer) to a di (data index).
  int dp_to_di(address dp) const {
    return dp - ((address)_data);
  }

  // bci to di/dp conversion.
  address bci_to_dp(int bci);
  int bci_to_di(int bci) {
    return dp_to_di(bci_to_dp(bci));
  }

  // Get the data at an arbitrary bci, or NULL if there is none.
  ProfileData* bci_to_data(int bci);

  // Same, but try to create an extra_data record if one is needed:
  ProfileData* allocate_bci_to_data(int bci, Method* m) {
    ProfileData* data = NULL;
    // If m not NULL, try to allocate a SpeculativeTrapData entry
    if (m == NULL) {
      data = bci_to_data(bci);
    }
    if (data != NULL) {
      return data;
    }
    data = bci_to_extra_data(bci, m, true);
    if (data != NULL) {
      return data;
    }
    // If SpeculativeTrapData allocation fails try to allocate a
    // regular entry
    data = bci_to_data(bci);
    if (data != NULL) {
      return data;
    }
    return bci_to_extra_data(bci, NULL, true);
  }

  // Add a handful of extra data records, for trap tracking.
  DataLayout* extra_data_base() const  { return limit_data_position(); }
  DataLayout* extra_data_limit() const { return (DataLayout*)((address)this + size_in_bytes()); }
  DataLayout* args_data_limit() const  { return (DataLayout*)((address)this + size_in_bytes() -
                                                              parameters_size_in_bytes()); }
  int extra_data_size() const          { return (address)extra_data_limit() - (address)extra_data_base(); }
  static DataLayout* next_extra(DataLayout* dp);

  // Return (uint)-1 for overflow.
  uint trap_count(int reason) const {
    return _compiler_counters.trap_count(reason);
  }
  // For loops:
  static uint trap_reason_limit() { return _trap_hist_limit; }
  static uint trap_count_limit()  { return _trap_hist_mask; }
  uint inc_trap_count(int reason) {
    return _compiler_counters.inc_trap_count(reason);
  }

  uint overflow_trap_count() const {
    return _compiler_counters.overflow_trap_count();
  }
  uint overflow_recompile_count() const {
    return _compiler_counters.overflow_recompile_count();
  }
  uint inc_overflow_recompile_count() {
    return _compiler_counters.inc_overflow_recompile_count();
  }
  uint decompile_count() const {
    return _compiler_counters.decompile_count();
  }
  uint inc_decompile_count() {
    uint dec_count = _compiler_counters.inc_decompile_count();
    if (dec_count > (uint)PerMethodRecompilationCutoff) {
      method()->set_not_compilable("decompile_count > PerMethodRecompilationCutoff", CompLevel_full_optimization);
    }
    return dec_count;
  }
  uint tenure_traps() const {
    return _tenure_traps;
  }
  void inc_tenure_traps() {
    _tenure_traps += 1;
  }

  // Return pointer to area dedicated to parameters in MDO
  ParametersTypeData* parameters_type_data() const {
    assert(_parameters_type_data_di != parameters_uninitialized, "called too early");
    return _parameters_type_data_di != no_parameters ? data_layout_at(_parameters_type_data_di)->data_in()->as_ParametersTypeData() : NULL;
  }

  int parameters_type_data_di() const {
    assert(_parameters_type_data_di != parameters_uninitialized && _parameters_type_data_di != no_parameters, "no args type data");
    return _parameters_type_data_di;
  }

  // Support for code generation
  static ByteSize data_offset() {
    return byte_offset_of(MethodData, _data[0]);
  }

  static ByteSize trap_history_offset() {
    return byte_offset_of(MethodData, _compiler_counters) + CompilerCounters::trap_history_offset();
  }

  static ByteSize invocation_counter_offset() {
    return byte_offset_of(MethodData, _invocation_counter);
  }

  static ByteSize backedge_counter_offset() {
    return byte_offset_of(MethodData, _backedge_counter);
  }

  static ByteSize invoke_mask_offset() {
    return byte_offset_of(MethodData, _invoke_mask);
  }

  static ByteSize backedge_mask_offset() {
    return byte_offset_of(MethodData, _backedge_mask);
  }

  static ByteSize parameters_type_data_di_offset() {
    return byte_offset_of(MethodData, _parameters_type_data_di);
  }

  virtual void metaspace_pointers_do(MetaspaceClosure* iter);
  virtual MetaspaceObj::Type type() const { return MethodDataType; }

  // Deallocation support - no metaspace pointer fields to deallocate
  void deallocate_contents(ClassLoaderData* loader_data) {}

  // GC support
  void set_size(int object_size_in_bytes) { _size = object_size_in_bytes; }

  // Printing
  void print_on      (outputStream* st) const;
  void print_value_on(outputStream* st) const;

  // printing support for method data
  void print_data_on(outputStream* st) const;

  const char* internal_name() const { return "{method data}"; }

  // verification
  void verify_on(outputStream* st);
  void verify_data_on(outputStream* st);

  static bool profile_parameters_for_method(const methodHandle& m);
  static bool profile_arguments();
  static bool profile_arguments_jsr292_only();
  static bool profile_return();
  static bool profile_parameters();
  static bool profile_return_jsr292_only();

  void clean_method_data(bool always_clean);
  void clean_weak_method_links();
  Mutex* extra_data_lock() { return &_extra_data_lock; }
};

#endif // SHARE_OOPS_METHODDATA_HPP
