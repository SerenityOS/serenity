/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019 SAP SE. All rights reserved.
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
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/gcLocker.hpp"
#include "interpreter/bytecodeUtils.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/signature.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "utilities/events.hpp"
#include "utilities/ostream.hpp"

class SimulatedOperandStack;
class ExceptionMessageBuilder;

// The entries of a SimulatedOperandStack. They carry the analysis
// information gathered for the slot.
class StackSlotAnalysisData {
 private:

  friend class SimulatedOperandStack;
  friend class ExceptionMessageBuilder;

  unsigned int _bci:17;    // The bci of the bytecode that pushed the current value on the operand stack.
                           // INVALID if ambiguous, e.g. after a control flow merge.
                           // 16 bits for bci (max bytecode size) and one for INVALID.
  unsigned int _type:15;   // The BasicType of the value on the operand stack.

  // Merges this slot data with the given one and returns the result. If
  // the bcis of the two merged objects are different, the bci of the result
  // will be undefined. If the types are different, the result type is T_CONFLICT.
  // (An exception is if one type is an array and the other is object, then
  // the result type will be T_OBJECT).
  StackSlotAnalysisData merge(StackSlotAnalysisData other);

 public:

  // Creates a new object with an invalid bci and the given type.
  StackSlotAnalysisData(BasicType type = T_CONFLICT);

  // Creates a new object with the given bci and type.
  StackSlotAnalysisData(int bci, BasicType type);

  enum {
    // An invalid bytecode index, as > 65535.
    INVALID = 0x1FFFF
  };

  // Returns the bci. If the bci is invalid, INVALID is returned.
  unsigned int get_bci();

  // Returns true, if the bci is not invalid.
  bool has_bci() { return get_bci() != INVALID; }

  // Returns the type of the slot data.
  BasicType get_type();
};

// A stack consisting of SimulatedOperandStackEntries.
// This represents the analysis information for the operand stack
// for a given bytecode at a given bci.
// It also holds an additional field that serves to collect
// information whether local slots were written.
class SimulatedOperandStack: CHeapObj<mtInternal> {

 private:

  friend class ExceptionMessageBuilder;
  friend class StackSlotAnalysisData;

  // The stack.
  GrowableArray<StackSlotAnalysisData> _stack;

  // Optimized bytecode can reuse local variable slots for several
  // local variables.
  // If there is no variable name information, we print 'parameter<i>'
  // if a parameter maps to a local slot. Once a local slot has been
  // written, we don't know any more whether it was written as the
  // corresponding parameter, or whether another local has been
  // mapped to the slot. So we don't want to print 'parameter<i>' any
  // more, but 'local<i>'. Similary for 'this'.
  // Therefore, during the analysis, we mark a bit for local slots that
  // get written and propagate this information.
  // We only run the analysis for 64 slots. If a method has more
  // parameters, we print 'local<i>' in all cases.
  uint64_t _written_local_slots;

  SimulatedOperandStack(): _written_local_slots(0) { };
  SimulatedOperandStack(const SimulatedOperandStack &copy);

  // Pushes the given slot data.
  void push_raw(StackSlotAnalysisData slotData);

  // Like push_raw, but if the slotData has type long or double, we push two.
  void push(StackSlotAnalysisData slotData);

  // Like push(slotData), but using bci/type to create an instance of
  // StackSlotAnalysisData first.
  void push(int bci, BasicType type);

  // Pops the given number of entries.
  void pop(int slots);

  // Merges this with the given stack by merging all entries. The
  // size of the stacks must be the same.
  void merge(SimulatedOperandStack const& other);

 public:

  // Returns the size of the stack.
  int get_size() const;

  // Returns the slot data at the given index. Slot 0 is top of stack.
  StackSlotAnalysisData get_slot_data(int slot);

  // Mark that local slot i was written.
  void set_local_slot_written(int i);

  // Check whether local slot i was written by this or a previous bytecode.
  bool local_slot_was_written(int i);
};

// Helper class to build internal exception messages for exceptions
// that are thrown because prerequisites to execute a bytecode
// are not met.
// E.g., if a NPE is thrown because an iload can not be executed
// by the VM because the reference to load from is null.
//
// It analyses the bytecode to assemble Java-like message text
// to give precise information where in a larger expression the
// exception occured.
//
// To assemble this message text, it is needed to know how
// operand stack slot entries were pushed on the operand stack.
// This class contains an analysis over the bytecodes to compute
// this information. The information is stored in a
// SimulatedOperandStack for each bytecode.
class ExceptionMessageBuilder : public StackObj {

  // The stacks for each bytecode.
  GrowableArray<SimulatedOperandStack*>* _stacks;

  // The method.
  Method* _method;

  // The number of entries used (the sum of all entries of all stacks).
  int _nr_of_entries;

  // If true, we have added at least one new stack.
  bool _added_one;

  // If true, we have processed all bytecodes.
  bool _all_processed;

  // The maximum number of entries we want to use. This is used to
  // limit the amount of memory we waste for insane methods (as they
  // appear in JCK tests).
  static const int _max_entries = 1000000;

  static const int _max_cause_detail = 5;

  // Merges the stack the the given bci with the given stack. If there
  // is no stack at the bci, we just put the given stack there. This
  // method doesn't takes ownership of the stack.
  void merge(int bci, SimulatedOperandStack* stack);

  // Processes the instruction at the given bci in the method. Returns
  // the size of the instruction.
  int do_instruction(int bci);

  bool print_NPE_cause0(outputStream *os, int bci, int slot, int max_detail,
                        bool inner_expr = false, const char *prefix = NULL);

 public:

  // Creates an ExceptionMessageBuilder object and runs the analysis
  // building SimulatedOperandStacks for each bytecode in the given
  // method (the method must be rewritten already). Note that you're
  // not allowed to use this object when crossing a safepoint! If the
  // bci is != -1, we only create the stacks as far as needed to get a
  // stack for the bci.
  ExceptionMessageBuilder(Method* method, int bci = -1);

  // Releases the resources.
  ~ExceptionMessageBuilder();

  // Returns the number of stacks (this is the size of the method).
  int get_size() { return _stacks->length() - 1; }

  // Assuming that a NullPointerException was thrown at the given bci,
  // we return the nr of the slot holding the null reference. If this
  // NPE is created by hand, we return -2 as the slot. If there
  // cannot be a NullPointerException at the bci, -1 is returned.
  int get_NPE_null_slot(int bci);

  // Prints a java-like expression for the bytecode that pushed
  // the value to the given slot being live at the given bci.
  // It constructs the expression by recursing backwards over the
  // bytecode using the results of the analysis done in the
  // constructor of ExceptionMessageBuilder.
  //  os:   The stream to print the message to.
  //  bci:  The index of the bytecode that caused the NPE.
  //  slot: The slot on the operand stack that contains null.
  //        The slots are numbered from TOS downwards, i.e.,
  //        TOS has the slot number 0, that below 1 and so on.
  //
  // Returns false if nothing was printed, else true.
  bool print_NPE_cause(outputStream *os, int bci, int slot);

  // Prints a string describing the failed action.
  void print_NPE_failed_action(outputStream *os, int bci);
};

// Replaces the following well-known class names:
//   java.lang.Object -> Object
//   java.lang.String -> String
static char *trim_well_known_class_names_from_signature(char *signature) {
  size_t len = strlen(signature);
  size_t skip_len = strlen("java.lang.");
  size_t min_pattern_len = strlen("java.lang.String");
  if (len < min_pattern_len) return signature;

  for (size_t isrc = 0, idst = 0; isrc <= len; isrc++, idst++) {
    // We must be careful not to trim names like test.java.lang.String.
    if ((isrc == 0 && strncmp(signature + isrc, "java.lang.Object", min_pattern_len) == 0) ||
        (isrc == 0 && strncmp(signature + isrc, "java.lang.String", min_pattern_len) == 0) ||
        (isrc > 1  && strncmp(signature + isrc-2, ", java.lang.Object", min_pattern_len+2) == 0) ||
        (isrc > 1  && strncmp(signature + isrc-2, ", java.lang.String", min_pattern_len+2) == 0)   ) {
      isrc += skip_len;
    }
    if (idst != isrc) {
      signature[idst] = signature[isrc];
    }
  }
  return signature;
}

// Replaces the following well-known class names:
//   java.lang.Object -> Object
//   java.lang.String -> String
static void print_klass_name(outputStream *os, Symbol *klass) {
  const char *name = klass->as_klass_external_name();
  if (strcmp(name, "java.lang.Object") == 0) name = "Object";
  if (strcmp(name, "java.lang.String") == 0) name = "String";
  os->print("%s", name);
}

// Prints the name of the method that is described at constant pool
// index cp_index in the constant pool of method 'method'.
static void print_method_name(outputStream *os, Method* method, int cp_index) {
  ResourceMark rm;
  ConstantPool* cp  = method->constants();
  Symbol* klass     = cp->klass_ref_at_noresolve(cp_index);
  Symbol* name      = cp->name_ref_at(cp_index);
  Symbol* signature = cp->signature_ref_at(cp_index);

  print_klass_name(os, klass);
  os->print(".%s(", name->as_C_string());
  stringStream sig;
  signature->print_as_signature_external_parameters(&sig);
  os->print("%s)", trim_well_known_class_names_from_signature(sig.as_string()));
}

// Prints the name of the field that is described at constant pool
// index cp_index in the constant pool of method 'method'.
static void print_field_and_class(outputStream *os, Method* method, int cp_index) {
  ResourceMark rm;
  ConstantPool* cp = method->constants();
  Symbol* klass    = cp->klass_ref_at_noresolve(cp_index);
  Symbol *name     = cp->name_ref_at(cp_index);
  print_klass_name(os, klass);
  os->print(".%s", name->as_C_string());
}

// Returns the name of the field that is described at constant pool
// index cp_index in the constant pool of method 'method'.
static char const* get_field_name(Method* method, int cp_index) {
  Symbol* name = method->constants()->name_ref_at(cp_index);
  return name->as_C_string();
}

static void print_local_var(outputStream *os, unsigned int bci, Method* method, int slot, bool is_parameter) {
  if (method->has_localvariable_table()) {
    for (int i = 0; i < method->localvariable_table_length(); i++) {
      LocalVariableTableElement* elem = method->localvariable_table_start() + i;
      unsigned int start = elem->start_bci;
      unsigned int end = start + elem->length;

      if ((bci >= start) && (bci < end) && (elem->slot == slot)) {
        ConstantPool* cp = method->constants();
        char *var =  cp->symbol_at(elem->name_cp_index)->as_C_string();
        os->print("%s", var);

        return;
      }
    }
  }

  // Handle at least some cases we know.
  if (!method->is_static() && (slot == 0) && is_parameter) {
    os->print("this");
  } else {
    int curr = method->is_static() ? 0 : 1;
    SignatureStream ss(method->signature());
    int param_index = 1;
    bool found = false;

    for (SignatureStream ss(method->signature()); !ss.is_done(); ss.next()) {
      if (ss.at_return_type()) {
        continue;
      }
      int size = type2size[ss.type()];
      if ((slot >= curr) && (slot < curr + size)) {
        found = true;
        break;
      }
      param_index += 1;
      curr += size;
    }

    if (found && is_parameter) {
      os->print("<parameter%d>", param_index);
    } else {
      // This is the best we can do.
      os->print("<local%d>", slot);
    }
  }
}

StackSlotAnalysisData::StackSlotAnalysisData(BasicType type) : _bci(INVALID), _type(type) {}

StackSlotAnalysisData::StackSlotAnalysisData(int bci, BasicType type) : _bci(bci), _type(type) {
  assert(bci >= 0, "BCI must be >= 0");
  assert(bci < 65536, "BCI must be < 65536");
}

unsigned int StackSlotAnalysisData::get_bci() {
  return _bci;
}

BasicType StackSlotAnalysisData::get_type() {
  return (BasicType)_type;
}

StackSlotAnalysisData StackSlotAnalysisData::merge(StackSlotAnalysisData other) {
  if (get_type() != other.get_type()) {
    if (((get_type() == T_OBJECT) || (get_type() == T_ARRAY)) &&
        ((other.get_type() == T_OBJECT) || (other.get_type() == T_ARRAY))) {
      if (get_bci() == other.get_bci()) {
        return StackSlotAnalysisData(get_bci(), T_OBJECT);
      } else {
        return StackSlotAnalysisData(T_OBJECT);
      }
    } else {
      return StackSlotAnalysisData(T_CONFLICT);
    }
  }

  if (get_bci() == other.get_bci()) {
    return *this;
  } else {
    return StackSlotAnalysisData(get_type());
  }
}

SimulatedOperandStack::SimulatedOperandStack(const SimulatedOperandStack &copy) {
  for (int i = 0; i < copy.get_size(); i++) {
    push_raw(copy._stack.at(i));
  }
  _written_local_slots = copy._written_local_slots;
}

void SimulatedOperandStack::push_raw(StackSlotAnalysisData slotData) {
  if (slotData.get_type() == T_VOID) {
    return;
  }

  _stack.push(slotData);
}

void SimulatedOperandStack::push(StackSlotAnalysisData slotData) {
  if (type2size[slotData.get_type()] == 2) {
    push_raw(slotData);
    push_raw(slotData);
  } else {
    push_raw(slotData);
  }
}

void SimulatedOperandStack::push(int bci, BasicType type) {
  push(StackSlotAnalysisData(bci, type));
}

void SimulatedOperandStack::pop(int slots) {
  for (int i = 0; i < slots; ++i) {
    _stack.pop();
  }

  assert(get_size() >= 0, "Popped too many slots");
}

void SimulatedOperandStack::merge(SimulatedOperandStack const& other) {
  assert(get_size() == other.get_size(), "Stacks not of same size");

  for (int i = get_size() - 1; i >= 0; --i) {
    _stack.at_put(i, _stack.at(i).merge(other._stack.at(i)));
  }
  _written_local_slots = _written_local_slots | other._written_local_slots;
}

int SimulatedOperandStack::get_size() const {
  return _stack.length();
}

StackSlotAnalysisData SimulatedOperandStack::get_slot_data(int slot) {
  assert(slot >= 0, "Slot=%d < 0", slot);
  assert(slot < get_size(), "Slot=%d >= size=%d", slot, get_size());

  return _stack.at(get_size() - slot - 1);
}

void SimulatedOperandStack::set_local_slot_written(int i) {
  // Local slots > 63 are very unlikely. Consider these
  // as written all the time. Saves space and complexity
  // for dynamic data size.
  if (i > 63) return;
  _written_local_slots = _written_local_slots | (1ULL << i);
}

bool SimulatedOperandStack::local_slot_was_written(int i) {
  if (i > 63) return true;
  return (_written_local_slots & (1ULL << i)) != 0;
}

ExceptionMessageBuilder::ExceptionMessageBuilder(Method* method, int bci) :
                    _method(method), _nr_of_entries(0),
                    _added_one(true), _all_processed(false) {

  ConstMethod* const_method = method->constMethod();
  const int len = const_method->code_size();

  assert(bci >= 0, "BCI too low: %d", bci);
  assert(bci < len, "BCI too large: %d size: %d", bci, len);

  _stacks = new GrowableArray<SimulatedOperandStack*> (len + 1);

  for (int i = 0; i <= len; ++i) {
    _stacks->push(NULL);
  }

  // Initialize stack a bci 0.
  _stacks->at_put(0, new SimulatedOperandStack());

  // And initialize the start of all exception handlers.
  if (const_method->has_exception_handler()) {
    ExceptionTableElement *et = const_method->exception_table_start();
    for (int i = 0; i < const_method->exception_table_length(); ++i) {
      u2 index = et[i].handler_pc;

      if (_stacks->at(index) == NULL) {
        _stacks->at_put(index, new SimulatedOperandStack());
        _stacks->at(index)->push(index, T_OBJECT);
      }
    }
  }

  // Do this until each bytecode has a stack or we haven't
  // added a new stack in one iteration.
  while (!_all_processed && _added_one) {
    _all_processed = true;
    _added_one = false;

    for (int i = 0; i < len; ) {
      // Analyse bytecode i. Step by size of the analyzed bytecode to next bytecode.
      i += do_instruction(i);

      // If we want the data only for a certain bci, we can possibly end early.
      if ((bci == i) && (_stacks->at(i) != NULL)) {
        _all_processed = true;
        break;
      }

      if (_nr_of_entries > _max_entries) {
        return;
      }
    }
  }
}

ExceptionMessageBuilder::~ExceptionMessageBuilder() {
  if (_stacks != NULL) {
    for (int i = 0; i < _stacks->length(); ++i) {
      delete _stacks->at(i);
    }
  }
}

void ExceptionMessageBuilder::merge(int bci, SimulatedOperandStack* stack) {
  assert(stack != _stacks->at(bci), "Cannot merge itself");

  if (_stacks->at(bci) != NULL) {
    stack->merge(*_stacks->at(bci));
  } else {
    // Got a new stack, so count the entries.
    _nr_of_entries += stack->get_size();
  }

  // Replace the stack at this bci with a copy of our new merged stack.
  delete _stacks->at(bci);
  _stacks->at_put(bci, new SimulatedOperandStack(*stack));
}

int ExceptionMessageBuilder::do_instruction(int bci) {
  ConstMethod* const_method = _method->constMethod();
  address code_base = _method->constMethod()->code_base();

  // We use the java code, since we don't want to cope with all the fast variants.
  int len = Bytecodes::java_length_at(_method, code_base + bci);

  // If we have no stack for this bci, we cannot process the bytecode now.
  if (_stacks->at(bci) == NULL) {
    _all_processed = false;
    return len;
  }

  // Make a local copy of the stack for this bci to work on.
  SimulatedOperandStack* stack = new SimulatedOperandStack(*_stacks->at(bci));

  // dest_bci is != -1 if we branch.
  int dest_bci = -1;

  // This is for table and lookup switch.
  static const int initial_length = 2;
  GrowableArray<int> dests(initial_length);

  bool flow_ended = false;

  // Get the bytecode.
  bool is_wide = false;
  Bytecodes::Code raw_code = Bytecodes::code_at(_method, code_base + bci);
  Bytecodes::Code code = Bytecodes::java_code_at(_method, code_base + bci);
  int pos = bci + 1;

  if (code == Bytecodes::_wide) {
    is_wide = true;
    code = Bytecodes::java_code_at(_method, code_base + bci + 1);
    pos += 1;
  }

  // Now simulate the action of each bytecode.
  switch (code) {
    case Bytecodes::_nop:
    case Bytecodes::_aconst_null:
    case Bytecodes::_iconst_m1:
    case Bytecodes::_iconst_0:
    case Bytecodes::_iconst_1:
    case Bytecodes::_iconst_2:
    case Bytecodes::_iconst_3:
    case Bytecodes::_iconst_4:
    case Bytecodes::_iconst_5:
    case Bytecodes::_lconst_0:
    case Bytecodes::_lconst_1:
    case Bytecodes::_fconst_0:
    case Bytecodes::_fconst_1:
    case Bytecodes::_fconst_2:
    case Bytecodes::_dconst_0:
    case Bytecodes::_dconst_1:
    case Bytecodes::_bipush:
    case Bytecodes::_sipush:
    case Bytecodes::_iload:
    case Bytecodes::_lload:
    case Bytecodes::_fload:
    case Bytecodes::_dload:
    case Bytecodes::_aload:
    case Bytecodes::_iload_0:
    case Bytecodes::_iload_1:
    case Bytecodes::_iload_2:
    case Bytecodes::_iload_3:
    case Bytecodes::_lload_0:
    case Bytecodes::_lload_1:
    case Bytecodes::_lload_2:
    case Bytecodes::_lload_3:
    case Bytecodes::_fload_0:
    case Bytecodes::_fload_1:
    case Bytecodes::_fload_2:
    case Bytecodes::_fload_3:
    case Bytecodes::_dload_0:
    case Bytecodes::_dload_1:
    case Bytecodes::_dload_2:
    case Bytecodes::_dload_3:
    case Bytecodes::_aload_0:
    case Bytecodes::_aload_1:
    case Bytecodes::_aload_2:
    case Bytecodes::_aload_3:
    case Bytecodes::_iinc:
    case Bytecodes::_new:
      stack->push(bci, Bytecodes::result_type(code));
      break;

    case Bytecodes::_ldc:
    case Bytecodes::_ldc_w:
    case Bytecodes::_ldc2_w: {
      int cp_index;
      ConstantPool* cp = _method->constants();

      if (code == Bytecodes::_ldc) {
        cp_index = *(uint8_t*) (code_base + pos);

        if (raw_code == Bytecodes::_fast_aldc) {
          cp_index = cp->object_to_cp_index(cp_index);
        }
      } else {
        if (raw_code == Bytecodes::_fast_aldc_w) {
          cp_index = Bytes::get_native_u2(code_base + pos);
          cp_index = cp->object_to_cp_index(cp_index);
        }
        else {
          cp_index = Bytes::get_Java_u2(code_base + pos);
        }
      }

      constantTag tag = cp->tag_at(cp_index);
      if (tag.is_klass()  || tag.is_unresolved_klass() ||
          tag.is_method() || tag.is_interface_method() ||
          tag.is_field()  || tag.is_string()) {
        stack->push(bci, T_OBJECT);
      } else if (tag.is_int()) {
        stack->push(bci, T_INT);
      } else if (tag.is_long()) {
        stack->push(bci, T_LONG);
      } else if (tag.is_float()) {
        stack->push(bci, T_FLOAT);
      } else if (tag.is_double()) {
        stack->push(bci, T_DOUBLE);
      } else {
        assert(false, "Unexpected tag");
      }
      break;
    }

    case Bytecodes::_iaload:
    case Bytecodes::_faload:
    case Bytecodes::_aaload:
    case Bytecodes::_baload:
    case Bytecodes::_caload:
    case Bytecodes::_saload:
    case Bytecodes::_laload:
    case Bytecodes::_daload:
      stack->pop(2);
      stack->push(bci, Bytecodes::result_type(code));
      break;

    case Bytecodes::_istore:
    case Bytecodes::_lstore:
    case Bytecodes::_fstore:
    case Bytecodes::_dstore:
    case Bytecodes::_astore:
      int index;
      if (is_wide) {
        index = Bytes::get_Java_u2(code_base + bci + 2);
      } else {
        index = *(uint8_t*) (code_base + bci + 1);
      }
      stack->set_local_slot_written(index);
      stack->pop(-Bytecodes::depth(code));
      break;
    case Bytecodes::_istore_0:
    case Bytecodes::_lstore_0:
    case Bytecodes::_fstore_0:
    case Bytecodes::_dstore_0:
    case Bytecodes::_astore_0:
      stack->set_local_slot_written(0);
      stack->pop(-Bytecodes::depth(code));
      break;
    case Bytecodes::_istore_1:
    case Bytecodes::_fstore_1:
    case Bytecodes::_lstore_1:
    case Bytecodes::_dstore_1:
    case Bytecodes::_astore_1:
      stack->set_local_slot_written(1);
      stack->pop(-Bytecodes::depth(code));
      break;
    case Bytecodes::_istore_2:
    case Bytecodes::_lstore_2:
    case Bytecodes::_fstore_2:
    case Bytecodes::_dstore_2:
    case Bytecodes::_astore_2:
      stack->set_local_slot_written(2);
      stack->pop(-Bytecodes::depth(code));
      break;
    case Bytecodes::_istore_3:
    case Bytecodes::_lstore_3:
    case Bytecodes::_fstore_3:
    case Bytecodes::_dstore_3:
    case Bytecodes::_astore_3:
      stack->set_local_slot_written(3);
      stack->pop(-Bytecodes::depth(code));
      break;
    case Bytecodes::_iastore:
    case Bytecodes::_lastore:
    case Bytecodes::_fastore:
    case Bytecodes::_dastore:
    case Bytecodes::_aastore:
    case Bytecodes::_bastore:
    case Bytecodes::_castore:
    case Bytecodes::_sastore:
    case Bytecodes::_pop:
    case Bytecodes::_pop2:
    case Bytecodes::_monitorenter:
    case Bytecodes::_monitorexit:
    case Bytecodes::_breakpoint:
      stack->pop(-Bytecodes::depth(code));
      break;

    case Bytecodes::_dup:
      stack->push_raw(stack->get_slot_data(0));
      break;

    case Bytecodes::_dup_x1: {
      StackSlotAnalysisData top1 = stack->get_slot_data(0);
      StackSlotAnalysisData top2 = stack->get_slot_data(1);
      stack->pop(2);
      stack->push_raw(top1);
      stack->push_raw(top2);
      stack->push_raw(top1);
      break;
    }

    case Bytecodes::_dup_x2: {
      StackSlotAnalysisData top1 = stack->get_slot_data(0);
      StackSlotAnalysisData top2 = stack->get_slot_data(1);
      StackSlotAnalysisData top3 = stack->get_slot_data(2);
      stack->pop(3);
      stack->push_raw(top1);
      stack->push_raw(top3);
      stack->push_raw(top2);
      stack->push_raw(top1);
      break;
    }

    case Bytecodes::_dup2:
      stack->push_raw(stack->get_slot_data(1));
      // The former '0' entry is now at '1'.
      stack->push_raw(stack->get_slot_data(1));
      break;

    case Bytecodes::_dup2_x1: {
      StackSlotAnalysisData top1 = stack->get_slot_data(0);
      StackSlotAnalysisData top2 = stack->get_slot_data(1);
      StackSlotAnalysisData top3 = stack->get_slot_data(2);
      stack->pop(3);
      stack->push_raw(top2);
      stack->push_raw(top1);
      stack->push_raw(top3);
      stack->push_raw(top2);
      stack->push_raw(top1);
      break;
    }

    case Bytecodes::_dup2_x2: {
      StackSlotAnalysisData top1 = stack->get_slot_data(0);
      StackSlotAnalysisData top2 = stack->get_slot_data(1);
      StackSlotAnalysisData top3 = stack->get_slot_data(2);
      StackSlotAnalysisData top4 = stack->get_slot_data(3);
      stack->pop(4);
      stack->push_raw(top2);
      stack->push_raw(top1);
      stack->push_raw(top4);
      stack->push_raw(top3);
      stack->push_raw(top2);
      stack->push_raw(top1);
      break;
    }

    case Bytecodes::_swap: {
      StackSlotAnalysisData top1 = stack->get_slot_data(0);
      StackSlotAnalysisData top2 = stack->get_slot_data(1);
      stack->pop(2);
      stack->push(top1);
      stack->push(top2);
      break;
    }

    case Bytecodes::_iadd:
    case Bytecodes::_ladd:
    case Bytecodes::_fadd:
    case Bytecodes::_dadd:
    case Bytecodes::_isub:
    case Bytecodes::_lsub:
    case Bytecodes::_fsub:
    case Bytecodes::_dsub:
    case Bytecodes::_imul:
    case Bytecodes::_lmul:
    case Bytecodes::_fmul:
    case Bytecodes::_dmul:
    case Bytecodes::_idiv:
    case Bytecodes::_ldiv:
    case Bytecodes::_fdiv:
    case Bytecodes::_ddiv:
    case Bytecodes::_irem:
    case Bytecodes::_lrem:
    case Bytecodes::_frem:
    case Bytecodes::_drem:
    case Bytecodes::_iand:
    case Bytecodes::_land:
    case Bytecodes::_ior:
    case Bytecodes::_lor:
    case Bytecodes::_ixor:
    case Bytecodes::_lxor:
      stack->pop(2 * type2size[Bytecodes::result_type(code)]);
      stack->push(bci, Bytecodes::result_type(code));
      break;

    case Bytecodes::_ineg:
    case Bytecodes::_lneg:
    case Bytecodes::_fneg:
    case Bytecodes::_dneg:
      stack->pop(type2size[Bytecodes::result_type(code)]);
      stack->push(bci, Bytecodes::result_type(code));
      break;

    case Bytecodes::_ishl:
    case Bytecodes::_lshl:
    case Bytecodes::_ishr:
    case Bytecodes::_lshr:
    case Bytecodes::_iushr:
    case Bytecodes::_lushr:
      stack->pop(1 + type2size[Bytecodes::result_type(code)]);
      stack->push(bci, Bytecodes::result_type(code));
      break;

    case Bytecodes::_i2l:
    case Bytecodes::_i2f:
    case Bytecodes::_i2d:
    case Bytecodes::_f2i:
    case Bytecodes::_f2l:
    case Bytecodes::_f2d:
    case Bytecodes::_i2b:
    case Bytecodes::_i2c:
    case Bytecodes::_i2s:
      stack->pop(1);
      stack->push(bci, Bytecodes::result_type(code));
      break;

    case Bytecodes::_l2i:
    case Bytecodes::_l2f:
    case Bytecodes::_l2d:
    case Bytecodes::_d2i:
    case Bytecodes::_d2l:
    case Bytecodes::_d2f:
      stack->pop(2);
      stack->push(bci, Bytecodes::result_type(code));
      break;

    case Bytecodes::_lcmp:
    case Bytecodes::_fcmpl:
    case Bytecodes::_fcmpg:
    case Bytecodes::_dcmpl:
    case Bytecodes::_dcmpg:
      stack->pop(1 - Bytecodes::depth(code));
      stack->push(bci, T_INT);
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
      stack->pop(-Bytecodes::depth(code));
      dest_bci = bci + (int16_t) Bytes::get_Java_u2(code_base + pos);
      break;

    case Bytecodes::_jsr:
      // NOTE: Bytecodes has wrong depth for jsr.
      stack->push(bci, T_ADDRESS);
      dest_bci = bci + (int16_t) Bytes::get_Java_u2(code_base + pos);
      flow_ended = true;
      break;

    case Bytecodes::_jsr_w: {
      // NOTE: Bytecodes has wrong depth for jsr.
      stack->push(bci, T_ADDRESS);
      dest_bci = bci + (int32_t) Bytes::get_Java_u4(code_base + pos);
      flow_ended = true;
      break;
    }

    case Bytecodes::_ret:
      // We don't track local variables, so we cannot know were we
      // return. This makes the stacks imprecise, but we have to
      // live with that.
      flow_ended = true;
      break;

    case Bytecodes::_tableswitch: {
      stack->pop(1);
      pos = (pos + 3) & ~3;
      dest_bci = bci + (int32_t) Bytes::get_Java_u4(code_base + pos);
      int low = (int32_t) Bytes::get_Java_u4(code_base + pos + 4);
      int high = (int32_t) Bytes::get_Java_u4(code_base + pos + 8);

      for (int64_t i = low; i <= high; ++i) {
        dests.push(bci + (int32_t) Bytes::get_Java_u4(code_base + pos + 12 + 4 * (i - low)));
      }

      break;
    }

    case Bytecodes::_lookupswitch: {
      stack->pop(1);
      pos = (pos + 3) & ~3;
      dest_bci = bci + (int32_t) Bytes::get_Java_u4(code_base + pos);
      int nr_of_dests = (int32_t) Bytes::get_Java_u4(code_base + pos + 4);

      for (int i = 0; i < nr_of_dests; ++i) {
        dests.push(bci + (int32_t) Bytes::get_Java_u4(code_base + pos + 12 + 8 * i));
      }

      break;
    }

    case Bytecodes::_ireturn:
    case Bytecodes::_lreturn:
    case Bytecodes::_freturn:
    case Bytecodes::_dreturn:
    case Bytecodes::_areturn:
    case Bytecodes::_return:
    case Bytecodes::_athrow:
      stack->pop(-Bytecodes::depth(code));
      flow_ended = true;
      break;

    case Bytecodes::_getstatic:
    case Bytecodes::_getfield: {
      // Find out the type of the field accessed.
      int cp_index = Bytes::get_native_u2(code_base + pos) DEBUG_ONLY(+ ConstantPool::CPCACHE_INDEX_TAG);
      ConstantPool* cp = _method->constants();
      int name_and_type_index = cp->name_and_type_ref_index_at(cp_index);
      int type_index = cp->signature_ref_index_at(name_and_type_index);
      Symbol* signature = cp->symbol_at(type_index);
      // Simulate the bytecode: pop the address, push the 'value' loaded
      // from the field.
      stack->pop(1 - Bytecodes::depth(code));
      stack->push(bci, Signature::basic_type(signature));
      break;
    }

    case Bytecodes::_putstatic:
    case Bytecodes::_putfield: {
      int cp_index = Bytes::get_native_u2(code_base + pos) DEBUG_ONLY(+ ConstantPool::CPCACHE_INDEX_TAG);
      ConstantPool* cp = _method->constants();
      int name_and_type_index = cp->name_and_type_ref_index_at(cp_index);
      int type_index = cp->signature_ref_index_at(name_and_type_index);
      Symbol* signature = cp->symbol_at(type_index);
      BasicType bt = Signature::basic_type(signature);
      stack->pop(type2size[bt] - Bytecodes::depth(code) - 1);
      break;
    }

    case Bytecodes::_invokevirtual:
    case Bytecodes::_invokespecial:
    case Bytecodes::_invokestatic:
    case Bytecodes::_invokeinterface:
    case Bytecodes::_invokedynamic: {
      ConstantPool* cp = _method->constants();
      int cp_index;

      if (code == Bytecodes::_invokedynamic) {
        cp_index = ((int) Bytes::get_native_u4(code_base + pos));
      } else {
        cp_index = Bytes::get_native_u2(code_base + pos) DEBUG_ONLY(+ ConstantPool::CPCACHE_INDEX_TAG);
      }

      int name_and_type_index = cp->name_and_type_ref_index_at(cp_index);
      int type_index = cp->signature_ref_index_at(name_and_type_index);
      Symbol* signature = cp->symbol_at(type_index);

      if ((code != Bytecodes::_invokestatic) && (code != Bytecodes::_invokedynamic)) {
        // Pop receiver.
        stack->pop(1);
      }

      stack->pop(ArgumentSizeComputer(signature).size());
      ResultTypeFinder result_type(signature);
      stack->push(bci, result_type.type());
      break;
    }

    case Bytecodes::_newarray:
    case Bytecodes::_anewarray:
    case Bytecodes::_instanceof:
      stack->pop(1);
      stack->push(bci, Bytecodes::result_type(code));
      break;

    case Bytecodes::_arraylength:
      // The return type of arraylength is wrong in the bytecodes table (T_VOID).
      stack->pop(1);
      stack->push(bci, T_INT);
      break;

    case Bytecodes::_checkcast:
      break;

    case Bytecodes::_multianewarray:
      stack->pop(*(uint8_t*) (code_base + pos + 2));
      stack->push(bci, T_OBJECT);
      break;

   case Bytecodes::_goto:
      stack->pop(-Bytecodes::depth(code));
      dest_bci = bci + (int16_t) Bytes::get_Java_u2(code_base + pos);
      flow_ended = true;
      break;


   case Bytecodes::_goto_w:
      stack->pop(-Bytecodes::depth(code));
      dest_bci = bci + (int32_t) Bytes::get_Java_u4(code_base + pos);
      flow_ended = true;
      break;

    default:
      // Allow at least the bcis which have stack info to work.
      _all_processed = false;
      _added_one = false;
      delete stack;

      return len;
  }

  // Put new stack to the next instruction, if we might reach it from
  // this bci.
  if (!flow_ended) {
    if (_stacks->at(bci + len) == NULL) {
      _added_one = true;
    }
    merge(bci + len, stack);
  }

  // Put the stack to the branch target too.
  if (dest_bci != -1) {
    if (_stacks->at(dest_bci) == NULL) {
      _added_one = true;
    }
    merge(dest_bci, stack);
  }

  // If we have more than one branch target, process these too.
  for (int64_t i = 0; i < dests.length(); ++i) {
    if (_stacks->at(dests.at(i)) == NULL) {
      _added_one = true;
    }
    merge(dests.at(i), stack);
  }

  delete stack;

  return len;
}

#define INVALID_BYTECODE_ENCOUNTERED -1
#define NPE_EXPLICIT_CONSTRUCTED -2
int ExceptionMessageBuilder::get_NPE_null_slot(int bci) {
  // Get the bytecode.
  address code_base = _method->constMethod()->code_base();
  Bytecodes::Code code = Bytecodes::java_code_at(_method, code_base + bci);
  int pos = bci + 1;  // Position of argument of the bytecode.
  if (code == Bytecodes::_wide) {
    code = Bytecodes::java_code_at(_method, code_base + bci + 1);
    pos += 1;
  }

  switch (code) {
    case Bytecodes::_getfield:
    case Bytecodes::_arraylength:
    case Bytecodes::_athrow:
    case Bytecodes::_monitorenter:
    case Bytecodes::_monitorexit:
      return 0;
    case Bytecodes::_iaload:
    case Bytecodes::_faload:
    case Bytecodes::_aaload:
    case Bytecodes::_baload:
    case Bytecodes::_caload:
    case Bytecodes::_saload:
    case Bytecodes::_laload:
    case Bytecodes::_daload:
      return 1;
    case Bytecodes::_iastore:
    case Bytecodes::_fastore:
    case Bytecodes::_aastore:
    case Bytecodes::_bastore:
    case Bytecodes::_castore:
    case Bytecodes::_sastore:
      return 2;
    case Bytecodes::_lastore:
    case Bytecodes::_dastore:
      return 3;
    case Bytecodes::_putfield: {
        int cp_index = Bytes::get_native_u2(code_base + pos) DEBUG_ONLY(+ ConstantPool::CPCACHE_INDEX_TAG);
        ConstantPool* cp = _method->constants();
        int name_and_type_index = cp->name_and_type_ref_index_at(cp_index);
        int type_index = cp->signature_ref_index_at(name_and_type_index);
        Symbol* signature = cp->symbol_at(type_index);
        BasicType bt = Signature::basic_type(signature);
        return type2size[bt];
      }
    case Bytecodes::_invokevirtual:
    case Bytecodes::_invokespecial:
    case Bytecodes::_invokeinterface: {
        int cp_index = Bytes::get_native_u2(code_base+ pos) DEBUG_ONLY(+ ConstantPool::CPCACHE_INDEX_TAG);
        ConstantPool* cp = _method->constants();
        int name_and_type_index = cp->name_and_type_ref_index_at(cp_index);
        int name_index = cp->name_ref_index_at(name_and_type_index);
        Symbol* name = cp->symbol_at(name_index);

        // Assume the the call of a constructor can never cause a NullPointerException
        // (which is true in Java). This is mainly used to avoid generating wrong
        // messages for NullPointerExceptions created explicitly by new in Java code.
        if (name != vmSymbols::object_initializer_name()) {
          int     type_index = cp->signature_ref_index_at(name_and_type_index);
          Symbol* signature  = cp->symbol_at(type_index);
          // The 'this' parameter was null. Return the slot of it.
          return ArgumentSizeComputer(signature).size();
        } else {
          return NPE_EXPLICIT_CONSTRUCTED;
        }
      }

    default:
      break;
  }

  return INVALID_BYTECODE_ENCOUNTERED;
}

bool ExceptionMessageBuilder::print_NPE_cause(outputStream* os, int bci, int slot) {
  if (print_NPE_cause0(os, bci, slot, _max_cause_detail, false, " because \"")) {
    os->print("\" is null");
    return true;
  }
  return false;
}

// Recursively print what was null.
//
// Go to the bytecode that pushed slot 'slot' on the operand stack
// at bytecode 'bci'. Compute a message for that bytecode. If
// necessary (array, field), recur further.
// At most do max_detail recursions.
// Prefix is used to print a proper beginning of the whole
// sentence.
// inner_expr is used to omit some text, like 'static' in
// inner expressions like array subscripts.
//
// Returns true if something was printed.
//
bool ExceptionMessageBuilder::print_NPE_cause0(outputStream* os, int bci, int slot,
                                               int max_detail,
                                               bool inner_expr, const char *prefix) {
  assert(bci >= 0, "BCI too low");
  assert(bci < get_size(), "BCI too large");

  if (max_detail <= 0) {
    return false;
  }

  if (_stacks->at(bci) == NULL) {
    return false;
  }

  SimulatedOperandStack* stack = _stacks->at(bci);
  assert(slot >= 0, "Slot nr. too low");
  assert(slot < stack->get_size(), "Slot nr. too large");

  StackSlotAnalysisData slotData = stack->get_slot_data(slot);

  if (!slotData.has_bci()) {
    return false;
  }

  // Get the bytecode.
  unsigned int source_bci = slotData.get_bci();
  address code_base = _method->constMethod()->code_base();
  Bytecodes::Code code = Bytecodes::java_code_at(_method, code_base + source_bci);
  bool is_wide = false;
  int pos = source_bci + 1;

  if (code == Bytecodes::_wide) {
    is_wide = true;
    code = Bytecodes::java_code_at(_method, code_base + source_bci + 1);
    pos += 1;
  }

  if (max_detail == _max_cause_detail &&
      prefix != NULL &&
      code != Bytecodes::_invokevirtual &&
      code != Bytecodes::_invokespecial &&
      code != Bytecodes::_invokestatic &&
      code != Bytecodes::_invokeinterface) {
    os->print("%s", prefix);
  }

  switch (code) {
    case Bytecodes::_iload_0:
    case Bytecodes::_aload_0:
      print_local_var(os, source_bci, _method, 0, !stack->local_slot_was_written(0));
      return true;

    case Bytecodes::_iload_1:
    case Bytecodes::_aload_1:
      print_local_var(os, source_bci, _method, 1, !stack->local_slot_was_written(1));
      return true;

    case Bytecodes::_iload_2:
    case Bytecodes::_aload_2:
      print_local_var(os, source_bci, _method, 2, !stack->local_slot_was_written(2));
      return true;

    case Bytecodes::_iload_3:
    case Bytecodes::_aload_3:
      print_local_var(os, source_bci, _method, 3, !stack->local_slot_was_written(3));
      return true;

    case Bytecodes::_iload:
    case Bytecodes::_aload: {
      int index;
      if (is_wide) {
        index = Bytes::get_Java_u2(code_base + source_bci + 2);
      } else {
        index = *(uint8_t*) (code_base + source_bci + 1);
      }
      print_local_var(os, source_bci, _method, index, !stack->local_slot_was_written(index));
      return true;
    }

    case Bytecodes::_aconst_null:
      os->print("null");
      return true;
    case Bytecodes::_iconst_m1:
      os->print("-1");
      return true;
    case Bytecodes::_iconst_0:
      os->print("0");
      return true;
    case Bytecodes::_iconst_1:
      os->print("1");
      return true;
    case Bytecodes::_iconst_2:
      os->print("2");
      return true;
    case Bytecodes::_iconst_3:
      os->print("3");
      return true;
    case Bytecodes::_iconst_4:
      os->print("4");
      return true;
    case Bytecodes::_iconst_5:
      os->print("5");
      return true;
    case Bytecodes::_bipush: {
      jbyte con = *(jbyte*) (code_base + source_bci + 1);
      os->print("%d", con);
      return true;
    }
    case Bytecodes::_sipush: {
      u2 con = Bytes::get_Java_u2(code_base + source_bci + 1);
      os->print("%d", con);
      return true;
    }
   case Bytecodes::_iaload:
   case Bytecodes::_aaload: {
      // Print the 'name' of the array. Go back to the bytecode that
      // pushed the array reference on the operand stack.
     if (!print_NPE_cause0(os, source_bci, 1, max_detail - 1, inner_expr)) {
        //  Returned false. Max recursion depth was reached. Print dummy.
        os->print("<array>");
      }
      os->print("[");
      // Print the index expression. Go back to the bytecode that
      // pushed the index on the operand stack.
      // inner_expr == true so we don't print unwanted strings
      // as "The return value of'". And don't decrement max_detail so we always
      // get a value here and only cancel out on the dereference.
      if (!print_NPE_cause0(os, source_bci, 0, max_detail, true)) {
        // Returned false. We don't print complex array index expressions. Print placeholder.
        os->print("...");
      }
      os->print("]");
      return true;
    }

    case Bytecodes::_getstatic: {
      int cp_index = Bytes::get_native_u2(code_base + pos) + ConstantPool::CPCACHE_INDEX_TAG;
      print_field_and_class(os, _method, cp_index);
      return true;
    }

    case Bytecodes::_getfield: {
      // Print the sender. Go back to the bytecode that
      // pushed the sender on the operand stack.
      if (print_NPE_cause0(os, source_bci, 0, max_detail - 1, inner_expr)) {
        os->print(".");
      }
      int cp_index = Bytes::get_native_u2(code_base + pos) + ConstantPool::CPCACHE_INDEX_TAG;
      os->print("%s", get_field_name(_method, cp_index));
      return true;
    }

    case Bytecodes::_invokevirtual:
    case Bytecodes::_invokespecial:
    case Bytecodes::_invokestatic:
    case Bytecodes::_invokeinterface: {
      int cp_index = Bytes::get_native_u2(code_base + pos) DEBUG_ONLY(+ ConstantPool::CPCACHE_INDEX_TAG);
      if (max_detail == _max_cause_detail && !inner_expr) {
        os->print(" because the return value of \"");
      }
      print_method_name(os, _method, cp_index);
      return true;
    }

    default: break;
  }
  return false;
}

void ExceptionMessageBuilder::print_NPE_failed_action(outputStream *os, int bci) {

  // Get the bytecode.
  address code_base = _method->constMethod()->code_base();
  Bytecodes::Code code = Bytecodes::java_code_at(_method, code_base + bci);
  int pos = bci + 1;
  if (code == Bytecodes::_wide) {
    code = Bytecodes::java_code_at(_method, code_base + bci + 1);
    pos += 1;
  }

  switch (code) {
    case Bytecodes::_iaload:
      os->print("Cannot load from int array"); break;
    case Bytecodes::_faload:
      os->print("Cannot load from float array"); break;
    case Bytecodes::_aaload:
      os->print("Cannot load from object array"); break;
    case Bytecodes::_baload:
      os->print("Cannot load from byte/boolean array"); break;
    case Bytecodes::_caload:
      os->print("Cannot load from char array"); break;
    case Bytecodes::_saload:
      os->print("Cannot load from short array"); break;
    case Bytecodes::_laload:
      os->print("Cannot load from long array"); break;
    case Bytecodes::_daload:
      os->print("Cannot load from double array"); break;

    case Bytecodes::_iastore:
      os->print("Cannot store to int array"); break;
    case Bytecodes::_fastore:
      os->print("Cannot store to float array"); break;
    case Bytecodes::_aastore:
      os->print("Cannot store to object array"); break;
    case Bytecodes::_bastore:
      os->print("Cannot store to byte/boolean array"); break;
    case Bytecodes::_castore:
      os->print("Cannot store to char array"); break;
    case Bytecodes::_sastore:
      os->print("Cannot store to short array"); break;
    case Bytecodes::_lastore:
      os->print("Cannot store to long array"); break;
    case Bytecodes::_dastore:
      os->print("Cannot store to double array"); break;

    case Bytecodes::_arraylength:
      os->print("Cannot read the array length"); break;
    case Bytecodes::_athrow:
      os->print("Cannot throw exception"); break;
    case Bytecodes::_monitorenter:
      os->print("Cannot enter synchronized block"); break;
    case Bytecodes::_monitorexit:
      os->print("Cannot exit synchronized block"); break;
    case Bytecodes::_getfield: {
        int cp_index = Bytes::get_native_u2(code_base + pos) DEBUG_ONLY(+ ConstantPool::CPCACHE_INDEX_TAG);
        ConstantPool* cp = _method->constants();
        int name_and_type_index = cp->name_and_type_ref_index_at(cp_index);
        int name_index = cp->name_ref_index_at(name_and_type_index);
        Symbol* name = cp->symbol_at(name_index);
        os->print("Cannot read field \"%s\"", name->as_C_string());
      } break;
    case Bytecodes::_putfield: {
        int cp_index = Bytes::get_native_u2(code_base + pos) DEBUG_ONLY(+ ConstantPool::CPCACHE_INDEX_TAG);
        os->print("Cannot assign field \"%s\"", get_field_name(_method, cp_index));
      } break;
    case Bytecodes::_invokevirtual:
    case Bytecodes::_invokespecial:
    case Bytecodes::_invokeinterface: {
        int cp_index = Bytes::get_native_u2(code_base+ pos) DEBUG_ONLY(+ ConstantPool::CPCACHE_INDEX_TAG);
        os->print("Cannot invoke \"");
        print_method_name(os, _method, cp_index);
        os->print("\"");
      } break;

    default:
      assert(0, "We should have checked this bytecode in get_NPE_null_slot().");
      break;
  }
}

// Main API
bool BytecodeUtils::get_NPE_message_at(outputStream* ss, Method* method, int bci) {

  NoSafepointVerifier _nsv;   // Cannot use this object over a safepoint.

  // If this NPE was created via reflection, we have no real NPE.
  if (method->method_holder() ==
      vmClasses::reflect_NativeConstructorAccessorImpl_klass()) {
    return false;
  }

  // Analyse the bytecodes.
  ResourceMark rm;
  ExceptionMessageBuilder emb(method, bci);

  // The slot of the operand stack that contains the null reference.
  // Also checks for NPE explicitly constructed and returns NPE_EXPLICIT_CONSTRUCTED.
  int slot = emb.get_NPE_null_slot(bci);

  // Build the message.
  if (slot == NPE_EXPLICIT_CONSTRUCTED) {
    // We don't want to print a message.
    return false;
  } else if (slot == INVALID_BYTECODE_ENCOUNTERED) {
    // We encountered a bytecode that does not dereference a reference.
    DEBUG_ONLY(ss->print("There cannot be a NullPointerException at bci %d of method %s",
                         bci, method->external_name()));
    NOT_DEBUG(return false);
  } else {
    // Print string describing which action (bytecode) could not be
    // performed because of the null reference.
    emb.print_NPE_failed_action(ss, bci);
    // Print a description of what is null.
    if (!emb.print_NPE_cause(ss, bci, slot)) {
      // Nothing was printed. End the sentence without the 'because'
      // subordinate sentence.
    }
  }
  return true;
}
