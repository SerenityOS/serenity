/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/stackMapTableFormat.hpp"
#include "interpreter/bytecodes.hpp"
#include "memory/metadataFactory.hpp"
#include "memory/oopFactory.hpp"
#include "oops/method.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/relocator.hpp"

#define MAX_METHOD_LENGTH  65535

#define MAX_SHORT ((1 << 15) - 1)
#define MIN_SHORT (- (1 << 15))

// Encapsulates a code change request. There are 3 types.
// General instruction, jump instruction, and table/lookup switches
//
class ChangeItem : public ResourceObj {
  int _bci;
 public:
   ChangeItem(int bci) { _bci = bci; }
   virtual bool handle_code_change(Relocator *r) = 0;

   // type info
   virtual bool is_widen()      { return false; }
   virtual bool is_jump_widen() { return false; }
   virtual bool is_switch_pad() { return false; }

   // accessors
   int bci()    { return _bci; }
   void relocate(int break_bci, int delta) { if (_bci > break_bci) { _bci += delta; } }

   virtual bool adjust(int bci, int delta) { return false; }

   // debug
   virtual void print() = 0;
};

class ChangeWiden : public ChangeItem {
  int              _new_ilen;    // New length of instruction at bci
  u_char*          _inst_buffer; // New bytecodes
 public:
  ChangeWiden(int bci, int new_ilen, u_char* inst_buffer) : ChangeItem(bci) {
    _new_ilen = new_ilen;
    _inst_buffer = inst_buffer;
  }

  // Callback to do instruction
  bool handle_code_change(Relocator *r) { return r->handle_widen(bci(), _new_ilen, _inst_buffer); };

  bool is_widen()              { return true; }

  void print()                 { tty->print_cr("ChangeWiden. bci: %d   New_ilen: %d", bci(), _new_ilen); }
};

class ChangeJumpWiden : public ChangeItem {
  int _delta;  // New length of instruction at bci
 public:
  ChangeJumpWiden(int bci, int delta) : ChangeItem(bci) { _delta = delta; }

  // Callback to do instruction
  bool handle_code_change(Relocator *r) { return r->handle_jump_widen(bci(), _delta); };

  bool is_jump_widen()         { return true; }

  // If the bci matches, adjust the delta in the change jump request.
  bool adjust(int jump_bci, int delta) {
    if (bci() == jump_bci) {
      if (_delta > 0)
        _delta += delta;
      else
        _delta -= delta;
      return true;
    }
    return false;
  }

  void print()                 { tty->print_cr("ChangeJumpWiden. bci: %d   Delta: %d", bci(), _delta); }
};

class ChangeSwitchPad : public ChangeItem {
  int  _padding;
  bool _is_lookup_switch;
 public:
   ChangeSwitchPad(int bci, int padding, bool is_lookup_switch) : ChangeItem(bci) {
     _padding = padding;
     _is_lookup_switch = is_lookup_switch;
   }

   // Callback to do instruction
   bool handle_code_change(Relocator *r) { return r->handle_switch_pad(bci(), _padding, _is_lookup_switch); };

   bool is_switch_pad()        { return true; }
   int  padding()              { return _padding;  }
   bool is_lookup_switch()     { return _is_lookup_switch; }

   void print()                { tty->print_cr("ChangeSwitchPad. bci: %d   Padding: %d  IsLookupSwitch: %d", bci(), _padding, _is_lookup_switch); }
};

//-----------------------------------------------------------------------------------------------------------
// Relocator code

Relocator::Relocator(const methodHandle& m, RelocatorListener* listener) {
  set_method(m);
  set_code_length(method()->code_size());
  set_code_array(NULL);
  // Allocate code array and copy bytecodes
  if (!expand_code_array(0)) {
    // Should have at least MAX_METHOD_LENGTH available or the verifier
    // would have failed.
    ShouldNotReachHere();
  }
  set_compressed_line_number_table(NULL);
  set_compressed_line_number_table_size(0);
  _listener = listener;
}

// size is the new size of the instruction at bci. Hence, if size is less than the current
// instruction size, we will shrink the code.
methodHandle Relocator::insert_space_at(int bci, int size, u_char inst_buffer[], TRAPS) {
  _changes = new GrowableArray<ChangeItem*> (10);
  _changes->push(new ChangeWiden(bci, size, inst_buffer));

  if (TraceRelocator) {
    tty->print_cr("Space at: %d Size: %d", bci, size);
    _method->print();
    _method->print_codes();
    tty->print_cr("-------------------------------------------------");
  }

  if (!handle_code_changes()) return methodHandle();

    // Construct the new method
  methodHandle new_method = Method::clone_with_new_data(method(),
                              code_array(), code_length(),
                              compressed_line_number_table(),
                              compressed_line_number_table_size(),
                              CHECK_(methodHandle()));

  // Deallocate the old Method* from metadata
  ClassLoaderData* loader_data = method()->method_holder()->class_loader_data();
  loader_data->add_to_deallocate_list(method()());

    set_method(new_method);

  if (TraceRelocator) {
    tty->print_cr("-------------------------------------------------");
    tty->print_cr("new method");
    _method->print_codes();
  }

  return new_method;
}


bool Relocator::handle_code_changes() {
  assert(_changes != NULL, "changes vector must be initialized");

  while (!_changes->is_empty()) {
    // Inv: everything is aligned.
    ChangeItem* ci = _changes->first();

    if (TraceRelocator) {
      ci->print();
    }

    // Execute operation
    if (!ci->handle_code_change(this)) return false;

    // Shuffle items up
    for (int index = 1; index < _changes->length(); index++) {
      _changes->at_put(index-1, _changes->at(index));
    }
    _changes->pop();
  }
  return true;
}


bool Relocator::is_opcode_lookupswitch(Bytecodes::Code bc) {
  switch (bc) {
    case Bytecodes::_tableswitch:       return false;
    case Bytecodes::_lookupswitch:                   // not rewritten on ia64
    case Bytecodes::_fast_linearswitch:              // rewritten _lookupswitch
    case Bytecodes::_fast_binaryswitch: return true; // rewritten _lookupswitch
    default: ShouldNotReachHere();
  }
  return true; // dummy
}

// We need a special instruction size method, since lookupswitches and tableswitches might not be
// properly aligned during relocation
int Relocator::rc_instr_len(int bci) {
  Bytecodes::Code bc= code_at(bci);
  switch (bc) {
    // In the case of switch instructions, see if we have the original
    // padding recorded.
    case Bytecodes::_tableswitch:
    case Bytecodes::_lookupswitch:
    case Bytecodes::_fast_linearswitch:
    case Bytecodes::_fast_binaryswitch:
    {
      int pad = get_orig_switch_pad(bci, is_opcode_lookupswitch(bc));
      if (pad == -1) {
        return instruction_length_at(bci);
      }
      // Otherwise, depends on the switch type.
      switch (bc) {
        case Bytecodes::_tableswitch: {
          int lo = int_at(bci + 1 + pad + 4 * 1);
          int hi = int_at(bci + 1 + pad + 4 * 2);
          int n = hi - lo + 1;
          return 1 + pad + 4*(3 + n);
        }
        case Bytecodes::_lookupswitch:
        case Bytecodes::_fast_linearswitch:
        case Bytecodes::_fast_binaryswitch: {
          int npairs = int_at(bci + 1 + pad + 4 * 1);
          return 1 + pad + 4*(2 + 2*npairs);
        }
        default:
          ShouldNotReachHere();
      }
    }
    default:
      break;
  }
  return instruction_length_at(bci);
}

// If a change item is recorded for "pc", with type "ct", returns the
// associated padding, else -1.
int Relocator::get_orig_switch_pad(int bci, bool is_lookup_switch) {
  for (int k = 0; k < _changes->length(); k++) {
    ChangeItem* ci = _changes->at(k);
    if (ci->is_switch_pad()) {
      ChangeSwitchPad* csp = (ChangeSwitchPad*)ci;
      if (csp->is_lookup_switch() == is_lookup_switch && csp->bci() == bci) {
        return csp->padding();
      }
    }
  }
  return -1;
}


// Push a ChangeJumpWiden if it doesn't already exist on the work queue,
// otherwise adjust the item already there by delta.  The calculation for
// new_delta is wrong for this because it uses the offset stored in the
// code stream itself which wasn't fixed when item was pushed on the work queue.
void Relocator::push_jump_widen(int bci, int delta, int new_delta) {
  for (int j = 0; j < _changes->length(); j++) {
    ChangeItem* ci = _changes->at(j);
    if (ci->adjust(bci, delta)) return;
  }
  _changes->push(new ChangeJumpWiden(bci, new_delta));
}


// The current instruction of "c" is a jump; one of its offset starts
// at "offset" and is a short if "isShort" is "TRUE",
// and an integer otherwise.  If the jump crosses "breakPC", change
// the span of the jump by "delta".
void Relocator::change_jump(int bci, int offset, bool is_short, int break_bci, int delta) {
  int bci_delta = (is_short) ? short_at(offset) : int_at(offset);
  int targ = bci + bci_delta;

  if ((bci <= break_bci && targ >  break_bci) ||
      (bci >  break_bci && targ <= break_bci)) {
    int new_delta;
    if (bci_delta > 0)
      new_delta = bci_delta + delta;
    else
      new_delta = bci_delta - delta;

    if (is_short && ((new_delta > MAX_SHORT) || new_delta < MIN_SHORT)) {
      push_jump_widen(bci, delta, new_delta);
    } else if (is_short) {
      short_at_put(offset, new_delta);
    } else {
      int_at_put(offset, new_delta);
    }
  }
}


// Changes all jumps crossing "break_bci" by "delta".  May enqueue things
// on "rc->changes"
void Relocator::change_jumps(int break_bci, int delta) {
  int bci = 0;
  Bytecodes::Code bc;
  // Now, adjust any affected instructions.
  while (bci < code_length()) {
    switch (bc= code_at(bci)) {
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
      case Bytecodes::_goto:
      case Bytecodes::_jsr:
        change_jump(bci, bci+1, true, break_bci, delta);
        break;
      case Bytecodes::_goto_w:
      case Bytecodes::_jsr_w:
        change_jump(bci, bci+1, false, break_bci, delta);
        break;
      case Bytecodes::_tableswitch:
      case Bytecodes::_lookupswitch:
      case Bytecodes::_fast_linearswitch:
      case Bytecodes::_fast_binaryswitch: {
        int recPad = get_orig_switch_pad(bci, (bc != Bytecodes::_tableswitch));
        int oldPad = (recPad != -1) ? recPad : align(bci+1) - (bci+1);
        if (bci > break_bci) {
          int new_bci = bci + delta;
          int newPad = align(new_bci+1) - (new_bci+1);
          // Do we need to check the padding?
          if (newPad != oldPad) {
            if (recPad == -1) {
              _changes->push(new ChangeSwitchPad(bci, oldPad, (bc != Bytecodes::_tableswitch)));
            }
          }
        }

        // Then the rest, which depend on the kind of switch.
        switch (bc) {
          case Bytecodes::_tableswitch: {
            change_jump(bci, bci +1 + oldPad, false, break_bci, delta);
            // We cannot use the Bytecode_tableswitch abstraction, since the padding might not be correct.
            int lo = int_at(bci + 1 + oldPad + 4 * 1);
            int hi = int_at(bci + 1 + oldPad + 4 * 2);
            int n = hi - lo + 1;
            for (int k = 0; k < n; k++) {
              change_jump(bci, bci +1 + oldPad + 4*(k+3), false, break_bci, delta);
            }
            // Special next-bci calculation here...
            bci += 1 + oldPad + (n+3)*4;
            continue;
          }
          case Bytecodes::_lookupswitch:
          case Bytecodes::_fast_linearswitch:
          case Bytecodes::_fast_binaryswitch: {
            change_jump(bci, bci +1 + oldPad, false, break_bci, delta);
            // We cannot use the Bytecode_lookupswitch abstraction, since the padding might not be correct.
            int npairs = int_at(bci + 1 + oldPad + 4 * 1);
            for (int k = 0; k < npairs; k++) {
              change_jump(bci, bci + 1 + oldPad + 4*(2 + 2*k + 1), false, break_bci, delta);
            }
            /* Special next-bci calculation here... */
            bci += 1 + oldPad + (2 + (npairs*2))*4;
            continue;
          }
          default:
            ShouldNotReachHere();
        }
      }
      default:
        break;
    }
    bci += rc_instr_len(bci);
  }
}

// The width of instruction at "pc" is changing by "delta".  Adjust the
// exception table, if any, of "rc->mb".
void Relocator::adjust_exception_table(int bci, int delta) {
  ExceptionTable table(_method());
  for (int index = 0; index < table.length(); index ++) {
    if (table.start_pc(index) > bci) {
      table.set_start_pc(index, table.start_pc(index) + delta);
      table.set_end_pc(index, table.end_pc(index) + delta);
    } else if (bci < table.end_pc(index)) {
      table.set_end_pc(index, table.end_pc(index) + delta);
    }
    if (table.handler_pc(index) > bci)
      table.set_handler_pc(index, table.handler_pc(index) + delta);
  }
}


// The width of instruction at "bci" is changing by "delta".  Adjust the line number table.
void Relocator::adjust_line_no_table(int bci, int delta) {
  if (method()->has_linenumber_table()) {
    CompressedLineNumberReadStream  reader(method()->compressed_linenumber_table());
    CompressedLineNumberWriteStream writer(64);  // plenty big for most line number tables
    while (reader.read_pair()) {
      int adjustment = (reader.bci() > bci) ? delta : 0;
      writer.write_pair(reader.bci() + adjustment, reader.line());
    }
    writer.write_terminator();
    set_compressed_line_number_table(writer.buffer());
    set_compressed_line_number_table_size(writer.position());
  }
}


// The width of instruction at "bci" is changing by "delta".  Adjust the local variable table.
void Relocator::adjust_local_var_table(int bci, int delta) {
  int localvariable_table_length = method()->localvariable_table_length();
  if (localvariable_table_length > 0) {
    LocalVariableTableElement* table = method()->localvariable_table_start();
    for (int i = 0; i < localvariable_table_length; i++) {
      u2 current_bci = table[i].start_bci;
      if (current_bci > bci) {
        table[i].start_bci = current_bci + delta;
      } else {
        u2 current_length = table[i].length;
        if (current_bci + current_length > bci) {
          table[i].length = current_length + delta;
        }
      }
    }
  }
}

// Create a new array, copying the src array but adding a hole at
// the specified location
static Array<u1>* insert_hole_at(ClassLoaderData* loader_data,
    size_t where, int hole_sz, Array<u1>* src) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  Array<u1>* dst =
      MetadataFactory::new_array<u1>(loader_data, src->length() + hole_sz, 0, CHECK_NULL);

  address src_addr = (address)src->adr_at(0);
  address dst_addr = (address)dst->adr_at(0);

  memcpy(dst_addr, src_addr, where);
  memcpy(dst_addr + where + hole_sz,
         src_addr + where, src->length() - where);
  return dst;
}

// The width of instruction at "bci" is changing by "delta".  Adjust the stack
// map frames.
void Relocator::adjust_stack_map_table(int bci, int delta) {
  if (method()->has_stackmap_table()) {
    Array<u1>* data = method()->stackmap_data();
    // The data in the array is a classfile representation of the stackmap table
    stack_map_table* sm_table =
        stack_map_table::at((address)data->adr_at(0));

    int count = sm_table->number_of_entries();
    stack_map_frame* frame = sm_table->entries();
    int bci_iter = -1;
    bool offset_adjusted = false; // only need to adjust one offset

    for (int i = 0; i < count; ++i) {
      int offset_delta = frame->offset_delta();
      bci_iter += offset_delta;

      if (!offset_adjusted && bci_iter > bci) {
        int new_offset_delta = offset_delta + delta;

        if (frame->is_valid_offset(new_offset_delta)) {
          frame->set_offset_delta(new_offset_delta);
        } else {
          assert(frame->is_same_frame() ||
                 frame->is_same_locals_1_stack_item_frame(),
                 "Frame must be one of the compressed forms");
          // The new delta exceeds the capacity of the 'same_frame' or
          // 'same_frame_1_stack_item_frame' frame types.  We need to
          // convert these frames to the extended versions, but the extended
          // version is bigger and requires more room.  So we allocate a
          // new array and copy the data, being sure to leave u2-sized hole
          // right after the 'frame_type' for the new offset field.
          //
          // We can safely ignore the reverse situation as a small delta
          // can still be used in an extended version of the frame.

          size_t frame_offset = (address)frame - (address)data->adr_at(0);

          ClassLoaderData* loader_data = method()->method_holder()->class_loader_data();
          Array<u1>* new_data = insert_hole_at(loader_data, frame_offset + 1, 2, data);
          if (new_data == NULL) {
            return; // out-of-memory?
          }
          // Deallocate old data
          MetadataFactory::free_array<u1>(loader_data, data);
          data = new_data;

          address frame_addr = (address)(data->adr_at(0) + frame_offset);
          frame = stack_map_frame::at(frame_addr);


          // Now convert the frames in place
          if (frame->is_same_frame()) {
            same_frame_extended::create_at(frame_addr, new_offset_delta);
          } else {
            same_locals_1_stack_item_extended::create_at(
              frame_addr, new_offset_delta, NULL);
            // the verification_info_type should already be at the right spot
          }
        }
        offset_adjusted = true; // needs to be done only once, since subsequent
                                // values are offsets from the current
      }

      // The stack map frame may contain verification types, if so we need to
      // check and update any Uninitialized type's bci (no matter where it is).
      int number_of_types = frame->number_of_types();
      verification_type_info* types = frame->types();

      for (int i = 0; i < number_of_types; ++i) {
        if (types->is_uninitialized() && types->bci() > bci) {
          types->set_bci(types->bci() + delta);
        }
        types = types->next();
      }

      // Full frame has stack values too
      full_frame* ff = frame->as_full_frame();
      if (ff != NULL) {
        address eol = (address)types;
        number_of_types = ff->stack_slots(eol);
        types = ff->stack(eol);
        for (int i = 0; i < number_of_types; ++i) {
          if (types->is_uninitialized() && types->bci() > bci) {
            types->set_bci(types->bci() + delta);
          }
          types = types->next();
        }
      }

      frame = frame->next();
    }

    method()->set_stackmap_data(data); // in case it has changed
  }
}


bool Relocator::expand_code_array(int delta) {
  int length = MAX2(code_length() + delta, code_length() * (100+code_slop_pct()) / 100);

  if (length > MAX_METHOD_LENGTH) {
    if (delta == 0 && code_length() <= MAX_METHOD_LENGTH) {
      length = MAX_METHOD_LENGTH;
    } else {
      return false;
    }
  }

  unsigned char* new_code_array = NEW_RESOURCE_ARRAY(unsigned char, length);
  if (!new_code_array) return false;

  // Expanding current array
  if (code_array() != NULL) {
    memcpy(new_code_array, code_array(), code_length());
  } else {
    // Initial copy. Copy directly from Method*
    memcpy(new_code_array, method()->code_base(), code_length());
  }

  set_code_array(new_code_array);
  set_code_array_length(length);

  return true;
}


// The instruction at "bci", whose size is "ilen", is changing size by
// "delta".  Reallocate, move code, recalculate jumps, and enqueue
// change items as necessary.
bool Relocator::relocate_code(int bci, int ilen, int delta) {
  int next_bci = bci + ilen;
  if (delta > 0 && code_length() + delta > code_array_length())  {
    // Expand allocated code space, if necessary.
    if (!expand_code_array(delta)) {
          return false;
    }
  }

  // We require 4-byte alignment of code arrays.
  assert(((intptr_t)code_array() & 3) == 0, "check code alignment");
  // Change jumps before doing the copying; this routine requires aligned switches.
  change_jumps(bci, delta);

  // In case we have shrunken a tableswitch/lookupswitch statement, we store the last
  // bytes that get overwritten. We have to copy the bytes after the change_jumps method
  // has been called, since it is likely to update last offset in a tableswitch/lookupswitch
  assert(delta >= -3, "We cannot overwrite more than 3 bytes.");
  if (delta < 0 && delta >= -3) {
    memcpy(_overwrite, addr_at(bci + ilen + delta), -delta);
  }

  memmove(addr_at(next_bci + delta), addr_at(next_bci), code_length() - next_bci);
  set_code_length(code_length() + delta);
  // Also adjust exception tables...
  adjust_exception_table(bci, delta);
  // Line number tables...
  adjust_line_no_table(bci, delta);
  // And local variable table...
  adjust_local_var_table(bci, delta);

  // Adjust stack maps
  adjust_stack_map_table(bci, delta);

  // Relocate the pending change stack...
  for (int j = 0; j < _changes->length(); j++) {
    ChangeItem* ci = _changes->at(j);
    ci->relocate(bci, delta);
  }

  // Notify any listeners about code relocation
  notify(bci, delta, code_length());

  return true;
}

// relocate a general instruction. Called by ChangeWiden class
bool Relocator::handle_widen(int bci, int new_ilen, u_char inst_buffer[]) {
  int ilen = rc_instr_len(bci);
  if (!relocate_code(bci, ilen, new_ilen - ilen))
    return false;

  // Insert new bytecode(s)
  for(int k = 0; k < new_ilen; k++) {
    code_at_put(bci + k, (Bytecodes::Code)inst_buffer[k]);
  }

  return true;
}

// handle jump_widen instruction. Called be ChangeJumpWiden class
bool Relocator::handle_jump_widen(int bci, int delta) {
  int ilen = rc_instr_len(bci);

  Bytecodes::Code bc = code_at(bci);
  switch (bc) {
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
    case Bytecodes::_ifnonnull: {
      const int goto_length   = Bytecodes::length_for(Bytecodes::_goto);

      // If 'if' points to the next bytecode after goto, it's already handled.
      // it shouldn't be.
      assert (short_at(bci+1) != ilen+goto_length, "if relocation already handled");
      assert(ilen == 3, "check length");

      // Convert to 0 if <cond> goto 6
      //            3 _goto 11
      //            6 _goto_w <wide delta offset>
      //            11 <else code>
      const int goto_w_length = Bytecodes::length_for(Bytecodes::_goto_w);
      const int add_bci = goto_length + goto_w_length;

      if (!relocate_code(bci, 3, /*delta*/add_bci)) return false;

      // if bytecode points to goto_w instruction
      short_at_put(bci + 1, ilen + goto_length);

      int cbci = bci + ilen;
      // goto around
      code_at_put(cbci, Bytecodes::_goto);
      short_at_put(cbci + 1, add_bci);
      // goto_w <wide delta>
      cbci = cbci + goto_length;
      code_at_put(cbci, Bytecodes::_goto_w);
      if (delta > 0) {
        delta += 2;                 // goto_w is 2 bytes more than "if" code
      } else {
        delta -= ilen+goto_length;  // branch starts at goto_w offset
      }
      int_at_put(cbci + 1, delta);
      break;

      }
    case Bytecodes::_goto:
    case Bytecodes::_jsr:
      assert(ilen == 3, "check length");

      if (!relocate_code(bci, 3, 2)) return false;
      if (bc == Bytecodes::_goto)
        code_at_put(bci, Bytecodes::_goto_w);
      else
        code_at_put(bci, Bytecodes::_jsr_w);

      // If it's a forward jump, add 2 for the widening.
      if (delta > 0) delta += 2;
      int_at_put(bci + 1, delta);
      break;

    default: ShouldNotReachHere();
  }

  return true;
}

// handle lookup/table switch instructions.  Called be ChangeSwitchPad class
bool Relocator::handle_switch_pad(int bci, int old_pad, bool is_lookup_switch) {
  int ilen = rc_instr_len(bci);
  int new_pad = align(bci+1) - (bci+1);
  int pad_delta = new_pad - old_pad;
  if (pad_delta != 0) {
    int len;
    if (!is_lookup_switch) {
      int low  = int_at(bci+1+old_pad+4);
      int high = int_at(bci+1+old_pad+8);
      len = high-low+1 + 3; // 3 for default, hi, lo.
    } else {
      int npairs = int_at(bci+1+old_pad+4);
      len = npairs*2 + 2; // 2 for default, npairs.
    }
    // Because "relocateCode" does a "changeJumps" loop,
    // which parses instructions to determine their length,
    // we need to call that before messing with the current
    // instruction.  Since it may also overwrite the current
    // instruction when moving down, remember the possibly
    // overwritten part.

    // Move the code following the instruction...
    if (!relocate_code(bci, ilen, pad_delta)) return false;

    if (pad_delta < 0) {
      // Move the shrunken instruction down.
      memmove(addr_at(bci + 1 + new_pad),
              addr_at(bci + 1 + old_pad),
              len * 4 + pad_delta);
      memmove(addr_at(bci + 1 + new_pad + len*4 + pad_delta),
              _overwrite, -pad_delta);
    } else {
      assert(pad_delta > 0, "check");
      // Move the expanded instruction up.
      memmove(addr_at(bci +1 + new_pad),
              addr_at(bci +1 + old_pad),
              len * 4);
      memset(addr_at(bci + 1), 0, new_pad); // pad must be 0
    }
  }
  return true;
}
