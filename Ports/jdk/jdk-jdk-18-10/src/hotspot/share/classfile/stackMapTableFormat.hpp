/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CLASSFILE_STACKMAPTABLEFORMAT_HPP
#define SHARE_CLASSFILE_STACKMAPTABLEFORMAT_HPP

#include "classfile/verificationType.hpp"

// These classes represent the stack-map substructures described in the JVMS
// (hence the non-conforming naming scheme).

// These classes work with the types in their compressed form in-place (as they
// would appear in the classfile).  No virtual methods or fields allowed.

class verification_type_info {
 private:
  // u1 tag
  // u2 cpool_index || u2 bci (for ITEM_Object & ITEM_Uninitailized only)

  address tag_addr() const { return (address)this; }
  address cpool_index_addr() const { return tag_addr() + sizeof(u1); }
  address bci_addr() const { return cpool_index_addr(); }
  NONCOPYABLE(verification_type_info);

 protected:
  // No constructors  - should be 'private', but GCC issues a warning if it is
  verification_type_info() {}

 public:

  static verification_type_info* at(address addr) {
    return (verification_type_info*)addr;
  }

  static verification_type_info* create_at(address addr, u1 tag) {
    verification_type_info* vti = (verification_type_info*)addr;
    vti->set_tag(tag);
    return vti;
  }

  static verification_type_info* create_object_at(address addr, u2 cp_idx) {
    verification_type_info* vti = (verification_type_info*)addr;
    vti->set_tag(ITEM_Object);
    vti->set_cpool_index(cp_idx);
    return vti;
  }

  static verification_type_info* create_uninit_at(address addr, u2 bci) {
    verification_type_info* vti = (verification_type_info*)addr;
    vti->set_tag(ITEM_Uninitialized);
    vti->set_bci(bci);
    return vti;
  }

  static size_t calculate_size(u1 tag) {
    if (tag == ITEM_Object || tag == ITEM_Uninitialized) {
      return sizeof(u1) + sizeof(u2);
    } else {
      return sizeof(u1);
    }
  }

  static size_t max_size() { return sizeof(u1) + sizeof(u2); }

  u1 tag() const { return *(u1*)tag_addr(); }
  void set_tag(u1 tag) { *((u1*)tag_addr()) = tag; }

  bool is_object() const { return tag() == ITEM_Object; }
  bool is_uninitialized() const { return tag() == ITEM_Uninitialized; }

  u2 cpool_index() const {
    assert(is_object(), "This type has no cp_index");
    return Bytes::get_Java_u2(cpool_index_addr());
  }
  void set_cpool_index(u2 idx) {
    assert(is_object(), "This type has no cp_index");
    Bytes::put_Java_u2(cpool_index_addr(), idx);
  }

  u2 bci() const {
    assert(is_uninitialized(), "This type has no bci");
    return Bytes::get_Java_u2(bci_addr());
  }

  void set_bci(u2 bci) {
    assert(is_uninitialized(), "This type has no bci");
    Bytes::put_Java_u2(bci_addr(), bci);
  }

  void copy_from(verification_type_info* from) {
    set_tag(from->tag());
    if (from->is_object()) {
      set_cpool_index(from->cpool_index());
    } else if (from->is_uninitialized()) {
      set_bci(from->bci());
    }
  }

  size_t size() const {
    return calculate_size(tag());
  }

  verification_type_info* next() {
    return (verification_type_info*)((address)this + size());
  }

  // This method is used when reading unverified data in order to ensure
  // that we don't read past a particular memory limit.  It returns false
  // if any part of the data structure is outside the specified memory bounds.
  bool verify(address start, address end) {
    return ((address)this >= start &&
            (address)this < end &&
            (bci_addr() + sizeof(u2) <= end ||
             (!is_object() && !is_uninitialized())));
  }

  void print_on(outputStream* st) {
    switch (tag()) {
      case ITEM_Top: st->print("Top"); break;
      case ITEM_Integer: st->print("Integer"); break;
      case ITEM_Float: st->print("Float"); break;
      case ITEM_Double: st->print("Double"); break;
      case ITEM_Long: st->print("Long"); break;
      case ITEM_Null: st->print("Null"); break;
      case ITEM_UninitializedThis:
        st->print("UninitializedThis"); break;
      case ITEM_Uninitialized:
        st->print("Uninitialized[#%d]", bci()); break;
      case ITEM_Object:
        st->print("Object[#%d]", cpool_index()); break;
      default:
        st->print("BAD:%d", tag()); break;
    }
  }
};

#define FOR_EACH_STACKMAP_FRAME_TYPE(macro, arg1, arg2) \
  macro(same_frame, arg1, arg2) \
  macro(same_frame_extended, arg1, arg2) \
  macro(same_locals_1_stack_item_frame, arg1, arg2) \
  macro(same_locals_1_stack_item_extended, arg1, arg2) \
  macro(chop_frame, arg1, arg2) \
  macro(append_frame, arg1, arg2) \
  macro(full_frame, arg1, arg2)

#define SM_FORWARD_DECL(type, arg1, arg2) class type;
FOR_EACH_STACKMAP_FRAME_TYPE(SM_FORWARD_DECL, x, x)
#undef SM_FORWARD_DECL

class stack_map_frame {
  NONCOPYABLE(stack_map_frame);

 protected:
  address frame_type_addr() const { return (address)this; }

  // No constructors  - should be 'private', but GCC issues a warning if it is
  stack_map_frame() {}

 public:

  static stack_map_frame* at(address addr) {
    return (stack_map_frame*)addr;
  }

  stack_map_frame* next() const {
    return at((address)this + size());
  }

  u1 frame_type() const { return *(u1*)frame_type_addr(); }
  void set_frame_type(u1 type) { *((u1*)frame_type_addr()) = type; }

  // pseudo-virtual methods
  inline size_t size() const;
  inline int offset_delta() const;
  inline void set_offset_delta(int offset_delta);
  inline int number_of_types() const; // number of types contained in the frame
  inline verification_type_info* types() const; // pointer to first type
  inline bool is_valid_offset(int offset_delta) const;

  // This method must be used when reading unverified data in order to ensure
  // that we don't read past a particular memory limit.  It returns false
  // if any part of the data structure is outside the specified memory bounds.
  inline bool verify(address start, address end) const;

  inline void print_on(outputStream* st, int current_offset) const;
  inline void print_truncated(outputStream* st, int current_offset) const;

  // Create as_xxx and is_xxx methods for the subtypes
#define FRAME_TYPE_DECL(stackmap_frame_type, arg1, arg2) \
  inline stackmap_frame_type* as_##stackmap_frame_type() const; \
  bool is_##stackmap_frame_type() { \
    return as_##stackmap_frame_type() != NULL; \
  }

  FOR_EACH_STACKMAP_FRAME_TYPE(FRAME_TYPE_DECL, x, x)
#undef FRAME_TYPE_DECL
};

class same_frame : public stack_map_frame {
 private:
  static int frame_type_to_offset_delta(u1 frame_type) {
      return frame_type + 1; }
  static u1 offset_delta_to_frame_type(int offset_delta) {
      return (u1)(offset_delta - 1); }

 public:

  static bool is_frame_type(u1 tag) {
    return tag < 64;
  }

  static same_frame* at(address addr) {
    assert(is_frame_type(*addr), "Wrong frame id");
    return (same_frame*)addr;
  }

  static same_frame* create_at(address addr, int offset_delta) {
    same_frame* sm = (same_frame*)addr;
    sm->set_offset_delta(offset_delta);
    return sm;
  }

  static size_t calculate_size() { return sizeof(u1); }

  size_t size() const { return calculate_size(); }
  int offset_delta() const { return frame_type_to_offset_delta(frame_type()); }

  void set_offset_delta(int offset_delta) {
    assert(offset_delta <= 64, "Offset too large for same_frame");
    set_frame_type(offset_delta_to_frame_type(offset_delta));
  }

  int number_of_types() const { return 0; }
  verification_type_info* types() const { return NULL; }

  bool is_valid_offset(int offset_delta) const {
    return is_frame_type(offset_delta_to_frame_type(offset_delta));
  }

  bool verify_subtype(address start, address end) const {
    return true;
  }

  void print_on(outputStream* st, int current_offset = -1) const {
    st->print("same_frame(@%d)", offset_delta() + current_offset);
  }

  void print_truncated(outputStream* st, int current_offset = -1) const {
    print_on(st, current_offset);
  }
};

class same_frame_extended : public stack_map_frame {
 private:
  enum { _frame_id = 251 };
  address offset_delta_addr() const { return frame_type_addr() + sizeof(u1); }

 public:
  static bool is_frame_type(u1 tag) {
    return tag == _frame_id;
  }

  static same_frame_extended* at(address addr) {
    assert(is_frame_type(*addr), "Wrong frame type");
    return (same_frame_extended*)addr;
  }

  static same_frame_extended* create_at(address addr, u2 offset_delta) {
    same_frame_extended* sm = (same_frame_extended*)addr;
    sm->set_frame_type(_frame_id);
    sm->set_offset_delta(offset_delta);
    return sm;
  }

  static size_t calculate_size() { return sizeof(u1) + sizeof(u2); }

  size_t size() const { return calculate_size(); }
  int offset_delta() const {
    return Bytes::get_Java_u2(offset_delta_addr()) + 1;
  }

  void set_offset_delta(int offset_delta) {
    Bytes::put_Java_u2(offset_delta_addr(), offset_delta - 1);
  }

  int number_of_types() const { return 0; }
  verification_type_info* types() const { return NULL; }
  bool is_valid_offset(int offset) const { return true; }

  bool verify_subtype(address start, address end) const {
    return frame_type_addr() + size() <= end;
  }

  void print_on(outputStream* st, int current_offset = -1) const {
    st->print("same_frame_extended(@%d)", offset_delta() + current_offset);
  }

  void print_truncated(outputStream* st, int current_offset = -1) const {
    print_on(st, current_offset);
  }
};

class same_locals_1_stack_item_frame : public stack_map_frame {
 private:
  address type_addr() const { return frame_type_addr() + sizeof(u1); }

  static int frame_type_to_offset_delta(u1 frame_type) {
      return frame_type - 63; }
  static u1 offset_delta_to_frame_type(int offset_delta) {
      return (u1)(offset_delta + 63); }

 public:
  static bool is_frame_type(u1 tag) {
    return tag >= 64 && tag < 128;
  }

  static same_locals_1_stack_item_frame* at(address addr) {
    assert(is_frame_type(*addr), "Wrong frame id");
    return (same_locals_1_stack_item_frame*)addr;
  }

  static same_locals_1_stack_item_frame* create_at(
      address addr, int offset_delta, verification_type_info* vti) {
    same_locals_1_stack_item_frame* sm = (same_locals_1_stack_item_frame*)addr;
    sm->set_offset_delta(offset_delta);
    if (vti != NULL) {
      sm->set_type(vti);
    }
    return sm;
  }

  static size_t calculate_size(verification_type_info* vti) {
    return sizeof(u1) + vti->size();
  }

  static size_t max_size() {
    return sizeof(u1) + verification_type_info::max_size();
  }

  size_t size() const { return calculate_size(types()); }
  int offset_delta() const { return frame_type_to_offset_delta(frame_type()); }

  void set_offset_delta(int offset_delta) {
    assert(offset_delta > 0 && offset_delta <= 64,
           "Offset too large for this frame type");
    set_frame_type(offset_delta_to_frame_type(offset_delta));
  }

  void set_type(verification_type_info* vti) {
    verification_type_info* cur = types();
    cur->copy_from(vti);
  }

  int number_of_types() const { return 1; }
  verification_type_info* types() const {
    return verification_type_info::at(type_addr());
  }

  bool is_valid_offset(int offset_delta) const {
    return is_frame_type(offset_delta_to_frame_type(offset_delta));
  }

  bool verify_subtype(address start, address end) const {
    return types()->verify(start, end);
  }

  void print_on(outputStream* st, int current_offset = -1) const {
    st->print("same_locals_1_stack_item_frame(@%d,",
        offset_delta() + current_offset);
    types()->print_on(st);
    st->print(")");
  }

  void print_truncated(outputStream* st, int current_offset = -1) const {
    st->print("same_locals_1_stack_item_frame(@%d), output truncated, Stackmap exceeds table size.",
              offset_delta() + current_offset);
  }
};

class same_locals_1_stack_item_extended : public stack_map_frame {
 private:
  address offset_delta_addr() const { return frame_type_addr() + sizeof(u1); }
  address type_addr() const { return offset_delta_addr() + sizeof(u2); }

  enum { _frame_id = 247 };

 public:
  static bool is_frame_type(u1 tag) {
    return tag == _frame_id;
  }

  static same_locals_1_stack_item_extended* at(address addr) {
    assert(is_frame_type(*addr), "Wrong frame id");
    return (same_locals_1_stack_item_extended*)addr;
  }

  static same_locals_1_stack_item_extended* create_at(
      address addr, int offset_delta, verification_type_info* vti) {
    same_locals_1_stack_item_extended* sm =
       (same_locals_1_stack_item_extended*)addr;
    sm->set_frame_type(_frame_id);
    sm->set_offset_delta(offset_delta);
    if (vti != NULL) {
      sm->set_type(vti);
    }
    return sm;
  }

  static size_t calculate_size(verification_type_info* vti) {
    return sizeof(u1) + sizeof(u2) + vti->size();
  }

  size_t size() const { return calculate_size(types()); }
  int offset_delta() const {
    return Bytes::get_Java_u2(offset_delta_addr()) + 1;
  }

  void set_offset_delta(int offset_delta) {
    Bytes::put_Java_u2(offset_delta_addr(), offset_delta - 1);
  }

  void set_type(verification_type_info* vti) {
    verification_type_info* cur = types();
    cur->copy_from(vti);
  }

  int number_of_types() const { return 1; }
  verification_type_info* types() const {
    return verification_type_info::at(type_addr());
  }
  bool is_valid_offset(int offset) { return true; }

  bool verify_subtype(address start, address end) const {
    return type_addr() < end && types()->verify(start, end);
  }

  void print_on(outputStream* st, int current_offset = -1) const {
    st->print("same_locals_1_stack_item_extended(@%d,",
        offset_delta() + current_offset);
    types()->print_on(st);
    st->print(")");
  }

  void print_truncated(outputStream* st, int current_offset = -1) const {
    st->print("same_locals_1_stack_item_extended(@%d), output truncated, Stackmap exceeds table size.",
              offset_delta() + current_offset);
  }
};

class chop_frame : public stack_map_frame {
 private:
  address offset_delta_addr() const { return frame_type_addr() + sizeof(u1); }

  static int frame_type_to_chops(u1 frame_type) {
    int chop = 251 - frame_type;
    return chop;
  }

  static u1 chops_to_frame_type(int chop) {
    return 251 - chop;
  }

 public:
  static bool is_frame_type(u1 tag) {
    return frame_type_to_chops(tag) > 0 && frame_type_to_chops(tag) < 4;
  }

  static chop_frame* at(address addr) {
    assert(is_frame_type(*addr), "Wrong frame id");
    return (chop_frame*)addr;
  }

  static chop_frame* create_at(address addr, int offset_delta, int chops) {
    chop_frame* sm = (chop_frame*)addr;
    sm->set_chops(chops);
    sm->set_offset_delta(offset_delta);
    return sm;
  }

  static size_t calculate_size() {
    return sizeof(u1) + sizeof(u2);
  }

  size_t size() const { return calculate_size(); }
  int offset_delta() const {
    return Bytes::get_Java_u2(offset_delta_addr()) + 1;
  }
  void set_offset_delta(int offset_delta) {
    Bytes::put_Java_u2(offset_delta_addr(), offset_delta - 1);
  }

  int chops() const {
    int chops = frame_type_to_chops(frame_type());
    assert(chops > 0 && chops < 4, "Invalid number of chops in frame");
    return chops;
  }
  void set_chops(int chops) {
    assert(chops > 0 && chops <= 3, "Bad number of chops");
    set_frame_type(chops_to_frame_type(chops));
  }

  int number_of_types() const { return 0; }
  verification_type_info* types() const { return NULL; }
  bool is_valid_offset(int offset) { return true; }

  bool verify_subtype(address start, address end) const {
    return frame_type_addr() + size() <= end;
  }

  void print_on(outputStream* st, int current_offset = -1) const {
    st->print("chop_frame(@%d,%d)", offset_delta() + current_offset, chops());
  }

  void print_truncated(outputStream* st, int current_offset = -1) const {
    print_on(st, current_offset);
  }
};

class append_frame : public stack_map_frame {
 private:
  address offset_delta_addr() const { return frame_type_addr() + sizeof(u1); }
  address types_addr() const { return offset_delta_addr() + sizeof(u2); }

  static int frame_type_to_appends(u1 frame_type) {
    int append = frame_type - 251;
    return append;
  }

  static u1 appends_to_frame_type(int appends) {
    assert(appends > 0 && appends < 4, "Invalid append amount");
    return 251 + appends;
  }

 public:
  static bool is_frame_type(u1 tag) {
    return frame_type_to_appends(tag) > 0 && frame_type_to_appends(tag) < 4;
  }

  static append_frame* at(address addr) {
    assert(is_frame_type(*addr), "Wrong frame id");
    return (append_frame*)addr;
  }

  static append_frame* create_at(
      address addr, int offset_delta, int appends,
      verification_type_info* types) {
    append_frame* sm = (append_frame*)addr;
    sm->set_appends(appends);
    sm->set_offset_delta(offset_delta);
    if (types != NULL) {
      verification_type_info* cur = sm->types();
      for (int i = 0; i < appends; ++i) {
        cur->copy_from(types);
        cur = cur->next();
        types = types->next();
      }
    }
    return sm;
  }

  static size_t calculate_size(int appends, verification_type_info* types) {
    size_t sz = sizeof(u1) + sizeof(u2);
    for (int i = 0; i < appends; ++i) {
      sz += types->size();
      types = types->next();
    }
    return sz;
  }

  static size_t max_size() {
    return sizeof(u1) + sizeof(u2) + 3 * verification_type_info::max_size();
  }

  size_t size() const { return calculate_size(number_of_types(), types()); }
  int offset_delta() const {
    return Bytes::get_Java_u2(offset_delta_addr()) + 1;
  }

  void set_offset_delta(int offset_delta) {
    Bytes::put_Java_u2(offset_delta_addr(), offset_delta - 1);
  }

  void set_appends(int appends) {
    assert(appends > 0 && appends < 4, "Bad number of appends");
    set_frame_type(appends_to_frame_type(appends));
  }

  int number_of_types() const {
    int appends = frame_type_to_appends(frame_type());
    assert(appends > 0 && appends < 4, "Invalid number of appends in frame");
    return appends;
  }
  verification_type_info* types() const {
    return verification_type_info::at(types_addr());
  }
  bool is_valid_offset(int offset) const { return true; }

  bool verify_subtype(address start, address end) const {
    verification_type_info* vti = types();
    if ((address)vti < end && vti->verify(start, end)) {
      int nof = number_of_types();
      vti = vti->next();
      if (nof < 2 || vti->verify(start, end)) {
        vti = vti->next();
        if (nof < 3 || vti->verify(start, end)) {
          return true;
        }
      }
    }
    return false;
  }

  void print_on(outputStream* st, int current_offset = -1) const {
    st->print("append_frame(@%d,", offset_delta() + current_offset);
    verification_type_info* vti = types();
    for (int i = 0; i < number_of_types(); ++i) {
      vti->print_on(st);
      if (i != number_of_types() - 1) {
        st->print(",");
      }
      vti = vti->next();
    }
    st->print(")");
  }

  void print_truncated(outputStream* st, int current_offset = -1) const {
    st->print("append_frame(@%d), output truncated, Stackmap exceeds table size.",
              offset_delta() + current_offset);
  }
};

class full_frame : public stack_map_frame {
 private:
  address offset_delta_addr() const { return frame_type_addr() + sizeof(u1); }
  address num_locals_addr() const { return offset_delta_addr() + sizeof(u2); }
  address locals_addr() const { return num_locals_addr() + sizeof(u2); }
  address stack_slots_addr(address end_of_locals) const {
      return end_of_locals; }
  address stack_addr(address end_of_locals) const {
      return stack_slots_addr(end_of_locals) + sizeof(u2); }

  enum { _frame_id = 255 };

 public:
  static bool is_frame_type(u1 tag) {
    return tag == _frame_id;
  }

  static full_frame* at(address addr) {
    assert(is_frame_type(*addr), "Wrong frame id");
    return (full_frame*)addr;
  }

  static full_frame* create_at(
      address addr, int offset_delta, int num_locals,
      verification_type_info* locals,
      int stack_slots, verification_type_info* stack) {
    full_frame* sm = (full_frame*)addr;
    sm->set_frame_type(_frame_id);
    sm->set_offset_delta(offset_delta);
    sm->set_num_locals(num_locals);
    if (locals != NULL) {
      verification_type_info* cur = sm->locals();
      for (int i = 0; i < num_locals; ++i) {
        cur->copy_from(locals);
        cur = cur->next();
        locals = locals->next();
      }
      address end_of_locals = (address)cur;
      sm->set_stack_slots(end_of_locals, stack_slots);
      cur = sm->stack(end_of_locals);
      for (int i = 0; i < stack_slots; ++i) {
        cur->copy_from(stack);
        cur = cur->next();
        stack = stack->next();
      }
    }
    return sm;
  }

  static size_t calculate_size(
      int num_locals, verification_type_info* locals,
      int stack_slots, verification_type_info* stack) {
    size_t sz = sizeof(u1) + sizeof(u2) + sizeof(u2) + sizeof(u2);
    verification_type_info* vti = locals;
    for (int i = 0; i < num_locals; ++i) {
      sz += vti->size();
      vti = vti->next();
    }
    vti = stack;
    for (int i = 0; i < stack_slots; ++i) {
      sz += vti->size();
      vti = vti->next();
    }
    return sz;
  }

  static size_t max_size(int locals, int stack) {
    return sizeof(u1) + 3 * sizeof(u2) +
        (locals + stack) * verification_type_info::max_size();
  }

  size_t size() const {
    address eol = end_of_locals();
    return calculate_size(num_locals(), locals(), stack_slots(eol), stack(eol));
  }

  int offset_delta() const {
    return Bytes::get_Java_u2(offset_delta_addr()) + 1;
  }
  int num_locals() const { return Bytes::get_Java_u2(num_locals_addr()); }
  verification_type_info* locals() const {
    return verification_type_info::at(locals_addr());
  }
  address end_of_locals() const {
    verification_type_info* vti = locals();
    for (int i = 0; i < num_locals(); ++i) {
      vti = vti->next();
    }
    return (address)vti;
  }
  int stack_slots(address end_of_locals) const {
    return Bytes::get_Java_u2(stack_slots_addr(end_of_locals));
  }
  verification_type_info* stack(address end_of_locals) const {
    return verification_type_info::at(stack_addr(end_of_locals));
  }

  void set_offset_delta(int offset_delta) {
    Bytes::put_Java_u2(offset_delta_addr(), offset_delta - 1);
  }
  void set_num_locals(int num_locals) {
    Bytes::put_Java_u2(num_locals_addr(), num_locals);
  }
  void set_stack_slots(address end_of_locals, int stack_slots) {
    Bytes::put_Java_u2(stack_slots_addr(end_of_locals), stack_slots);
  }

  // These return only the locals.  Extra processing is required for stack
  // types of full frames.
  int number_of_types() const { return num_locals(); }
  verification_type_info* types() const { return locals(); }
  bool is_valid_offset(int offset) { return true; }

  bool verify_subtype(address start, address end) const {
    verification_type_info* vti = types();
    if ((address)vti >= end) {
      return false;
    }
    int count = number_of_types();
    for (int i = 0; i < count; ++i) {
      if (!vti->verify(start, end)) {
        return false;
      }
      vti = vti->next();
    }
    address eol = (address)vti;
    if (eol + sizeof(u2) > end) {
      return false;
    }
    count = stack_slots(eol);
    vti = stack(eol);
    for (int i = 0; i < stack_slots(eol); ++i) {
      if (!vti->verify(start, end)) {
        return false;
      }
      vti = vti->next();
    }
    return true;
  }

  void print_on(outputStream* st, int current_offset = -1) const {
    st->print("full_frame(@%d,{", offset_delta() + current_offset);
    verification_type_info* vti = locals();
    for (int i = 0; i < num_locals(); ++i) {
      vti->print_on(st);
      if (i != num_locals() - 1) {
        st->print(",");
      }
      vti = vti->next();
    }
    st->print("},{");
    address end_of_locals = (address)vti;
    vti = stack(end_of_locals);
    int ss = stack_slots(end_of_locals);
    for (int i = 0; i < ss; ++i) {
      vti->print_on(st);
      if (i != ss - 1) {
        st->print(",");
      }
      vti = vti->next();
    }
    st->print("})");
  }

  void print_truncated(outputStream* st, int current_offset = -1) const {
    st->print("full_frame(@%d), output truncated, Stackmap exceeds table size.",
              offset_delta() + current_offset);
  }
};

#define VIRTUAL_DISPATCH(stack_frame_type, func_name, args) \
  stack_frame_type* item_##stack_frame_type = as_##stack_frame_type(); \
  if (item_##stack_frame_type != NULL) { \
    return item_##stack_frame_type->func_name args;  \
  }

#define VOID_VIRTUAL_DISPATCH(stack_frame_type, func_name, args) \
  stack_frame_type* item_##stack_frame_type = as_##stack_frame_type(); \
  if (item_##stack_frame_type != NULL) { \
    item_##stack_frame_type->func_name args;  \
    return; \
  }

size_t stack_map_frame::size() const {
  FOR_EACH_STACKMAP_FRAME_TYPE(VIRTUAL_DISPATCH, size, ());
  return 0;
}

int stack_map_frame::offset_delta() const {
  FOR_EACH_STACKMAP_FRAME_TYPE(VIRTUAL_DISPATCH, offset_delta, ());
  return 0;
}

void stack_map_frame::set_offset_delta(int offset_delta) {
  FOR_EACH_STACKMAP_FRAME_TYPE(
      VOID_VIRTUAL_DISPATCH, set_offset_delta, (offset_delta));
}

int stack_map_frame::number_of_types() const {
  FOR_EACH_STACKMAP_FRAME_TYPE(VIRTUAL_DISPATCH, number_of_types, ());
  return 0;
}

verification_type_info* stack_map_frame::types() const {
  FOR_EACH_STACKMAP_FRAME_TYPE(VIRTUAL_DISPATCH, types, ());
  return NULL;
}

bool stack_map_frame::is_valid_offset(int offset) const {
  FOR_EACH_STACKMAP_FRAME_TYPE(VIRTUAL_DISPATCH, is_valid_offset, (offset));
  return true;
}

bool stack_map_frame::verify(address start, address end) const {
  if (frame_type_addr() >= start && frame_type_addr() < end) {
    FOR_EACH_STACKMAP_FRAME_TYPE(
       VIRTUAL_DISPATCH, verify_subtype, (start, end));
  }
  return false;
}

void stack_map_frame::print_on(outputStream* st, int offs = -1) const {
  FOR_EACH_STACKMAP_FRAME_TYPE(VOID_VIRTUAL_DISPATCH, print_on, (st, offs));
}

void stack_map_frame::print_truncated(outputStream* st, int offs = -1) const {
  FOR_EACH_STACKMAP_FRAME_TYPE(VOID_VIRTUAL_DISPATCH, print_truncated, (st, offs));
}

#undef VIRTUAL_DISPATCH
#undef VOID_VIRTUAL_DISPATCH

#define AS_SUBTYPE_DEF(stack_frame_type, arg1, arg2) \
stack_frame_type* stack_map_frame::as_##stack_frame_type() const { \
  if (stack_frame_type::is_frame_type(frame_type())) { \
    return (stack_frame_type*)this; \
  } else { \
    return NULL; \
  } \
}

FOR_EACH_STACKMAP_FRAME_TYPE(AS_SUBTYPE_DEF, x, x)
#undef AS_SUBTYPE_DEF

class stack_map_table {
 private:
  address number_of_entries_addr() const {
    return (address)this;
  }
  address entries_addr() const {
    return number_of_entries_addr() + sizeof(u2);
  }
  NONCOPYABLE(stack_map_table);

 protected:
  // No constructors  - should be 'private', but GCC issues a warning if it is
  stack_map_table() {}

 public:

  static stack_map_table* at(address addr) {
    return (stack_map_table*)addr;
  }

  u2 number_of_entries() const {
    return Bytes::get_Java_u2(number_of_entries_addr());
  }
  stack_map_frame* entries() const {
    return stack_map_frame::at(entries_addr());
  }

  void set_number_of_entries(u2 num) {
    Bytes::put_Java_u2(number_of_entries_addr(), num);
  }
};

class stack_map_table_attribute {
 private:
  address name_index_addr() const {
      return (address)this; }
  address attribute_length_addr() const {
      return name_index_addr() + sizeof(u2); }
  address stack_map_table_addr() const {
      return attribute_length_addr() + sizeof(u4); }
  NONCOPYABLE(stack_map_table_attribute);

 protected:
  // No constructors  - should be 'private', but GCC issues a warning if it is
  stack_map_table_attribute() {}

 public:

  static stack_map_table_attribute* at(address addr) {
    return (stack_map_table_attribute*)addr;
  }

  u2 name_index() const {
    return Bytes::get_Java_u2(name_index_addr()); }
  u4 attribute_length() const {
    return Bytes::get_Java_u4(attribute_length_addr()); }
  stack_map_table* table() const {
    return stack_map_table::at(stack_map_table_addr());
  }

  void set_name_index(u2 idx) {
    Bytes::put_Java_u2(name_index_addr(), idx);
  }
  void set_attribute_length(u4 len) {
    Bytes::put_Java_u4(attribute_length_addr(), len);
  }
};

#undef FOR_EACH_STACKMAP_FRAME_TYPE

#endif // SHARE_CLASSFILE_STACKMAPTABLEFORMAT_HPP
