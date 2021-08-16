/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright 2007, 2008, 2009, 2010, 2011 Red Hat, Inc.
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
#include "gc/shared/collectedHeap.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/method.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/signature.hpp"
#include "vmreg_zero.inline.hpp"

#ifdef ASSERT
void RegisterMap::check_location_valid() {
  ShouldNotCallThis();
}
#endif

bool frame::is_interpreted_frame() const {
  return zeroframe()->is_interpreter_frame();
}

bool frame::is_fake_stub_frame() const {
  return zeroframe()->is_fake_stub_frame();
}

frame frame::sender_for_entry_frame(RegisterMap *map) const {
  assert(zeroframe()->is_entry_frame(), "wrong type of frame");
  assert(map != NULL, "map must be set");
  assert(!entry_frame_is_first(), "next Java fp must be non zero");
  assert(entry_frame_call_wrapper()->anchor()->last_Java_sp() == sender_sp(),
         "sender should be next Java frame");
  map->clear();
  assert(map->include_argument_oops(), "should be set by clear");
  return frame(zeroframe()->next(), sender_sp());
}

OptimizedEntryBlob::FrameData* OptimizedEntryBlob::frame_data_for_frame(const frame& frame) const {
  ShouldNotCallThis();
  return nullptr;
}

bool frame::optimized_entry_frame_is_first() const {
  ShouldNotCallThis();
  return false;
}

frame frame::sender_for_nonentry_frame(RegisterMap *map) const {
  assert(zeroframe()->is_interpreter_frame() ||
         zeroframe()->is_fake_stub_frame(), "wrong type of frame");
  return frame(zeroframe()->next(), sender_sp());
}

frame frame::sender(RegisterMap* map) const {
  // Default is not to follow arguments; the various
  // sender_for_xxx methods update this accordingly.
  map->set_include_argument_oops(false);

  if (is_entry_frame())
    return sender_for_entry_frame(map);
  else
    return sender_for_nonentry_frame(map);
}

BasicObjectLock* frame::interpreter_frame_monitor_begin() const {
  return get_interpreterState()->monitor_base();
}

BasicObjectLock* frame::interpreter_frame_monitor_end() const {
  return (BasicObjectLock*) get_interpreterState()->stack_base();
}

void frame::patch_pc(Thread* thread, address pc) {
  if (pc != NULL) {
    assert(_cb == CodeCache::find_blob(pc), "unexpected pc");
    _pc = pc;
    _deopt_state = is_deoptimized;
  } else {
    // We borrow this call to set the thread pointer in the interpreter
    // state; the hook to set up deoptimized frames isn't supplied it.
    assert(pc == NULL, "should be");
    get_interpreterState()->set_thread(JavaThread::cast(thread));
  }
}

bool frame::safe_for_sender(JavaThread *thread) {
  ShouldNotCallThis();
  return false;
}

bool frame::is_interpreted_frame_valid(JavaThread *thread) const {
  ShouldNotCallThis();
  return false;
}

BasicType frame::interpreter_frame_result(oop* oop_result,
                                          jvalue* value_result) {
  assert(is_interpreted_frame(), "interpreted frame expected");
  Method* method = interpreter_frame_method();
  BasicType type = method->result_type();
  intptr_t* tos_addr = (intptr_t *) interpreter_frame_tos_address();
  oop obj;

  switch (type) {
  case T_VOID:
    break;
  case T_BOOLEAN:
    value_result->z = *(jboolean *) tos_addr;
    break;
  case T_BYTE:
    value_result->b = *(jbyte *) tos_addr;
    break;
  case T_CHAR:
    value_result->c = *(jchar *) tos_addr;
    break;
  case T_SHORT:
    value_result->s = *(jshort *) tos_addr;
    break;
  case T_INT:
    value_result->i = *(jint *) tos_addr;
    break;
  case T_LONG:
    value_result->j = *(jlong *) tos_addr;
    break;
  case T_FLOAT:
    value_result->f = *(jfloat *) tos_addr;
    break;
  case T_DOUBLE:
    value_result->d = *(jdouble *) tos_addr;
    break;

  case T_OBJECT:
  case T_ARRAY:
    if (method->is_native()) {
      obj = get_interpreterState()->oop_temp();
    }
    else {
      oop* obj_p = (oop *) tos_addr;
      obj = (obj_p == NULL) ? (oop) NULL : *obj_p;
    }
    assert(obj == NULL || Universe::heap()->is_in(obj), "sanity check");
    *oop_result = obj;
    break;

  default:
    ShouldNotReachHere();
  }

  return type;
}

int frame::frame_size(RegisterMap* map) const {
#ifdef PRODUCT
  ShouldNotCallThis();
#endif // PRODUCT
  return 0; // make javaVFrame::print_value work
}

intptr_t* frame::interpreter_frame_tos_at(jint offset) const {
  int index = (Interpreter::expr_offset_in_bytes(offset) / wordSize);
  return &interpreter_frame_tos_address()[index];
}

void frame::zero_print_on_error(int           frame_index,
                                outputStream* st,
                                char*         buf,
                                int           buflen) const {
  // Divide the buffer between the field and the value
  buflen >>= 1;
  char *fieldbuf = buf;
  char *valuebuf = buf + buflen;

  // Print each word of the frame
  for (intptr_t *addr = sp(); addr <= fp(); addr++) {
    int offset = fp() - addr;

    // Fill in default values, then try and improve them
    snprintf(fieldbuf, buflen, "word[%d]", offset);
    snprintf(valuebuf, buflen, PTR_FORMAT, *addr);
    zeroframe()->identify_word(frame_index, offset, fieldbuf, valuebuf, buflen);
    fieldbuf[buflen - 1] = '\0';
    valuebuf[buflen - 1] = '\0';

    // Print the result
    st->print_cr(" " PTR_FORMAT ": %-21s = %s", p2i(addr), fieldbuf, valuebuf);
  }
}

void ZeroFrame::identify_word(int   frame_index,
                              int   offset,
                              char* fieldbuf,
                              char* valuebuf,
                              int   buflen) const {
  switch (offset) {
  case next_frame_off:
    strncpy(fieldbuf, "next_frame", buflen);
    break;

  case frame_type_off:
    strncpy(fieldbuf, "frame_type", buflen);
    if (is_entry_frame())
      strncpy(valuebuf, "ENTRY_FRAME", buflen);
    else if (is_interpreter_frame())
      strncpy(valuebuf, "INTERPRETER_FRAME", buflen);
    else if (is_fake_stub_frame())
      strncpy(valuebuf, "FAKE_STUB_FRAME", buflen);
    break;

  default:
    if (is_entry_frame()) {
      as_entry_frame()->identify_word(
        frame_index, offset, fieldbuf, valuebuf, buflen);
    }
    else if (is_interpreter_frame()) {
      as_interpreter_frame()->identify_word(
        frame_index, offset, fieldbuf, valuebuf, buflen);
    }
    else if (is_fake_stub_frame()) {
      as_fake_stub_frame()->identify_word(
        frame_index, offset, fieldbuf, valuebuf, buflen);
    }
  }
}

void EntryFrame::identify_word(int   frame_index,
                               int   offset,
                               char* fieldbuf,
                               char* valuebuf,
                               int   buflen) const {
  switch (offset) {
  case call_wrapper_off:
    strncpy(fieldbuf, "call_wrapper", buflen);
    break;

  default:
    snprintf(fieldbuf, buflen, "local[%d]", offset - 3);
  }
}

void InterpreterFrame::identify_word(int   frame_index,
                                     int   offset,
                                     char* fieldbuf,
                                     char* valuebuf,
                                     int   buflen) const {
  interpreterState istate = interpreter_state();
  bool is_valid = istate->self_link() == istate;
  intptr_t *addr = addr_of_word(offset);

  // Fixed part
  if (addr >= (intptr_t *) istate) {
    const char *field = istate->name_of_field_at_address((address) addr);
    if (field) {
      if (is_valid && !strcmp(field, "_method")) {
        istate->method()->name_and_sig_as_C_string(valuebuf, buflen);
      }
      else if (is_valid && !strcmp(field, "_bcp") && istate->bcp()) {
        snprintf(valuebuf, buflen, PTR_FORMAT " (bci %d)",
                 (intptr_t) istate->bcp(),
                 istate->method()->bci_from(istate->bcp()));
      }
      snprintf(fieldbuf, buflen, "%sistate->%s",
               field[strlen(field) - 1] == ')' ? "(": "", field);
    }
    else if (addr == (intptr_t *) istate) {
      strncpy(fieldbuf, "(vtable for istate)", buflen);
    }
    return;
  }

  // Variable part
  if (!is_valid)
    return;

  // JNI stuff
  if (istate->method()->is_native() && addr < istate->stack_base()) {
    address hA = istate->method()->signature_handler();
    if (hA != NULL) {
      if (hA != (address) InterpreterRuntime::slow_signature_handler) {
        InterpreterRuntime::SignatureHandler *handler =
          InterpreterRuntime::SignatureHandler::from_handlerAddr(hA);

        intptr_t *params = istate->stack_base() - handler->argument_count();
        if (addr >= params) {
          int param = addr - params;
          const char *desc = "";
          if (param == 0)
            desc = " (JNIEnv)";
          else if (param == 1) {
            if (istate->method()->is_static())
              desc = " (mirror)";
            else
              desc = " (this)";
          }
          snprintf(fieldbuf, buflen, "parameter[%d]%s", param, desc);
          return;
        }

        for (int i = 0; i < handler->argument_count(); i++) {
          if (params[i] == (intptr_t) addr) {
            snprintf(fieldbuf, buflen, "unboxed parameter[%d]", i);
            return;
          }
        }
      }
    }
    return;
  }

  // Monitors and stack
  identify_vp_word(frame_index, addr,
                   (intptr_t *) istate->monitor_base(),
                   istate->stack_base(),
                   fieldbuf, buflen);
}

void ZeroFrame::identify_vp_word(int       frame_index,
                                 intptr_t* addr,
                                 intptr_t* monitor_base,
                                 intptr_t* stack_base,
                                 char*     fieldbuf,
                                 int       buflen) const {
  // Monitors
  if (addr >= stack_base && addr < monitor_base) {
    int monitor_size = frame::interpreter_frame_monitor_size();
    int last_index = (monitor_base - stack_base) / monitor_size - 1;
    int index = last_index - (addr - stack_base) / monitor_size;
    intptr_t monitor = (intptr_t) (
      (BasicObjectLock *) monitor_base - 1 - index);
    intptr_t offset = (intptr_t) addr - monitor;

    if (offset == BasicObjectLock::obj_offset_in_bytes())
      snprintf(fieldbuf, buflen, "monitor[%d]->_obj", index);
    else if (offset ==  BasicObjectLock::lock_offset_in_bytes())
      snprintf(fieldbuf, buflen, "monitor[%d]->_lock", index);

    return;
  }

  // Expression stack
  if (addr < stack_base) {
    snprintf(fieldbuf, buflen, "%s[%d]",
             frame_index == 0 ? "stack_word" : "local",
             (int) (stack_base - addr - 1));
    return;
  }
}

#ifndef PRODUCT

void frame::describe_pd(FrameValues& values, int frame_no) {

}

#endif

intptr_t *frame::initial_deoptimization_info() {
  // unused... but returns fp() to minimize changes introduced by 7087445
  return fp();
}

#ifndef PRODUCT
// This is a generic constructor which is only used by pns() in debug.cpp.
frame::frame(void* sp, void* fp, void* pc) {
  Unimplemented();
}

void frame::pd_ps() {}
#endif
