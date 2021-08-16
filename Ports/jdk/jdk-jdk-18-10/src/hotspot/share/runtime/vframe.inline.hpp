/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_VFRAME_INLINE_HPP
#define SHARE_RUNTIME_VFRAME_INLINE_HPP

#include "runtime/vframe.hpp"

#include "runtime/frame.inline.hpp"
#include "runtime/thread.inline.hpp"

inline vframeStreamCommon::vframeStreamCommon(JavaThread* thread, bool process_frames) : _reg_map(thread, false, process_frames) {
  _thread = thread;
}

inline intptr_t* vframeStreamCommon::frame_id() const        { return _frame.id(); }

inline int vframeStreamCommon::vframe_id() const {
  assert(_mode == compiled_mode, "unexpected mode: %d", _mode);
  return _vframe_id;
}

inline int vframeStreamCommon::decode_offset() const {
  assert(_mode == compiled_mode, "unexpected mode: %d", _mode);
  return _decode_offset;
}

inline bool vframeStreamCommon::is_interpreted_frame() const { return _frame.is_interpreted_frame(); }

inline bool vframeStreamCommon::is_entry_frame() const       { return _frame.is_entry_frame(); }

inline void vframeStreamCommon::next() {
  // handle frames with inlining
  if (_mode == compiled_mode    && fill_in_compiled_inlined_sender()) return;

  // handle general case
  do {
    _prev_frame = _frame;
    _frame = _frame.sender(&_reg_map);
  } while (!fill_from_frame());
}

inline vframeStream::vframeStream(JavaThread* thread, bool stop_at_java_call_stub, bool process_frame)
  : vframeStreamCommon(thread, process_frame /* process_frames */) {
  _stop_at_java_call_stub = stop_at_java_call_stub;

  if (!thread->has_last_Java_frame()) {
    _mode = at_end_mode;
    return;
  }

  _frame = _thread->last_frame();
  while (!fill_from_frame()) {
    _prev_frame = _frame;
    _frame = _frame.sender(&_reg_map);
  }
}

inline bool vframeStreamCommon::fill_in_compiled_inlined_sender() {
  if (_sender_decode_offset == DebugInformationRecorder::serialized_null) {
    return false;
  }
  fill_from_compiled_frame(_sender_decode_offset);
  ++_vframe_id;
  return true;
}


inline void vframeStreamCommon::fill_from_compiled_frame(int decode_offset) {
  _mode = compiled_mode;
  _decode_offset = decode_offset;

  // Range check to detect ridiculous offsets.
  if (decode_offset == DebugInformationRecorder::serialized_null ||
      decode_offset < 0 ||
      decode_offset >= nm()->scopes_data_size()) {
    // 6379830 AsyncGetCallTrace sometimes feeds us wild frames.
    // If we read nmethod::scopes_data at serialized_null (== 0)
    // or if read some at other invalid offset, invalid values will be decoded.
    // Based on these values, invalid heap locations could be referenced
    // that could lead to crashes in product mode.
    // Therefore, do not use the decode offset if invalid, but fill the frame
    // as it were a native compiled frame (no Java-level assumptions).
#ifdef ASSERT
    if (WizardMode) {
      ttyLocker ttyl;
      tty->print_cr("Error in fill_from_frame: pc_desc for "
                    INTPTR_FORMAT " not found or invalid at %d",
                    p2i(_frame.pc()), decode_offset);
      nm()->print();
      nm()->method()->print_codes();
      nm()->print_code();
      nm()->print_pcs();
    }
    found_bad_method_frame();
#endif
    // Provide a cheap fallback in product mode.  (See comment above.)
    fill_from_compiled_native_frame();
    return;
  }

  // Decode first part of scopeDesc
  DebugInfoReadStream buffer(nm(), decode_offset);
  _sender_decode_offset = buffer.read_int();
  _method               = buffer.read_method();
  _bci                  = buffer.read_bci();

  assert(_method->is_method(), "checking type of decoded method");
}

// The native frames are handled specially. We do not rely on ScopeDesc info
// since the pc might not be exact due to the _last_native_pc trick.
inline void vframeStreamCommon::fill_from_compiled_native_frame() {
  _mode = compiled_mode;
  _sender_decode_offset = DebugInformationRecorder::serialized_null;
  _decode_offset = DebugInformationRecorder::serialized_null;
  _vframe_id = 0;
  _method = nm()->method();
  _bci = 0;
}

inline bool vframeStreamCommon::fill_from_frame() {
  // Interpreted frame
  if (_frame.is_interpreted_frame()) {
    fill_from_interpreter_frame();
    return true;
  }

  // Compiled frame

  if (cb() != NULL && cb()->is_compiled()) {
    if (nm()->is_native_method()) {
      // Do not rely on scopeDesc since the pc might be unprecise due to the _last_native_pc trick.
      fill_from_compiled_native_frame();
    } else {
      PcDesc* pc_desc = nm()->pc_desc_at(_frame.pc());
      int decode_offset;
      if (pc_desc == NULL) {
        // Should not happen, but let fill_from_compiled_frame handle it.

        // If we are trying to walk the stack of a thread that is not
        // at a safepoint (like AsyncGetCallTrace would do) then this is an
        // acceptable result. [ This is assuming that safe_for_sender
        // is so bullet proof that we can trust the frames it produced. ]
        //
        // So if we see that the thread is not safepoint safe
        // then simply produce the method and a bci of zero
        // and skip the possibility of decoding any inlining that
        // may be present. That is far better than simply stopping (or
        // asserting. If however the thread is safepoint safe this
        // is the sign of a compiler bug  and we'll let
        // fill_from_compiled_frame handle it.


        JavaThreadState state = _thread->thread_state();

        // in_Java should be good enough to test safepoint safety
        // if state were say in_Java_trans then we'd expect that
        // the pc would have already been slightly adjusted to
        // one that would produce a pcDesc since the trans state
        // would be one that might in fact anticipate a safepoint

        if (state == _thread_in_Java ) {
          // This will get a method a zero bci and no inlining.
          // Might be nice to have a unique bci to signify this
          // particular case but for now zero will do.

          fill_from_compiled_native_frame();

          // There is something to be said for setting the mode to
          // at_end_mode to prevent trying to walk further up the
          // stack. There is evidence that if we walk any further
          // that we could produce a bad stack chain. However until
          // we see evidence that allowing this causes us to find
          // frames bad enough to cause segv's or assertion failures
          // we don't do it as while we may get a bad call chain the
          // probability is much higher (several magnitudes) that we
          // get good data.

          return true;
        }
        decode_offset = DebugInformationRecorder::serialized_null;
      } else {
        decode_offset = pc_desc->scope_decode_offset();
      }
      fill_from_compiled_frame(decode_offset);
      _vframe_id = 0;
    }
    return true;
  }

  // End of stack?
  if (_frame.is_first_frame() || (_stop_at_java_call_stub && _frame.is_entry_frame())) {
    _mode = at_end_mode;
    return true;
  }

  return false;
}


inline void vframeStreamCommon::fill_from_interpreter_frame() {
  Method* method = _frame.interpreter_frame_method();
  address   bcp    = _frame.interpreter_frame_bcp();
  int       bci    = method->validate_bci_from_bcp(bcp);
  // 6379830 AsyncGetCallTrace sometimes feeds us wild frames.
  // AsyncGetCallTrace interrupts the VM asynchronously. As a result
  // it is possible to access an interpreter frame for which
  // no Java-level information is yet available (e.g., becasue
  // the frame was being created when the VM interrupted it).
  // In this scenario, pretend that the interpreter is at the point
  // of entering the method.
  if (bci < 0) {
    DEBUG_ONLY(found_bad_method_frame();)
    bci = 0;
  }
  _mode   = interpreted_mode;
  _method = method;
  _bci    = bci;
}

#endif // SHARE_RUNTIME_VFRAME_INLINE_HPP
