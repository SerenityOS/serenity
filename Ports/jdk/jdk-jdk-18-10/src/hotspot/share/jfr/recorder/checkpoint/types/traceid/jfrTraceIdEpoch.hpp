/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEIDEPOCH_HPP
#define SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEIDEPOCH_HPP

#include "jfr/utilities/jfrSignal.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "memory/allocation.hpp"
#include "runtime/atomic.hpp"

#define BIT                                  1
#define METHOD_BIT                           (BIT << 2)
#define EPOCH_0_SHIFT                        0
#define EPOCH_1_SHIFT                        1
#define EPOCH_0_BIT                          (BIT << EPOCH_0_SHIFT)
#define EPOCH_1_BIT                          (BIT << EPOCH_1_SHIFT)
#define EPOCH_0_METHOD_BIT                   (METHOD_BIT << EPOCH_0_SHIFT)
#define EPOCH_1_METHOD_BIT                   (METHOD_BIT << EPOCH_1_SHIFT)
#define METHOD_AND_CLASS_BITS                (METHOD_BIT | BIT)
#define EPOCH_0_METHOD_AND_CLASS_BITS        (METHOD_AND_CLASS_BITS << EPOCH_0_SHIFT)
#define EPOCH_1_METHOD_AND_CLASS_BITS        (METHOD_AND_CLASS_BITS << EPOCH_1_SHIFT)

 // Epoch alternation on each rotation allow for concurrent tagging.
 // The epoch shift happens only during a safepoint.
 //
 // _synchronizing is a transition state, the purpose of which is to
 // have JavaThreads that run _thread_in_native (i.e. Compiler threads)
 // respect the current epoch shift in-progress during the safepoint.
 //
 // _changed_tag_state == true signals an incremental modification to artifact tagging
 // (klasses, methods, CLDs, etc), purpose of which is to trigger collection of artifacts.
 //
class JfrTraceIdEpoch : AllStatic {
  friend class JfrCheckpointManager;
 private:
  static JfrSignal _tag_state;
  static bool _epoch_state;
  static bool _synchronizing;

  static void begin_epoch_shift();
  static void end_epoch_shift();

 public:
  static bool epoch() {
    return _epoch_state;
  }

  static address epoch_address() {
    return (address)&_epoch_state;
  }

  static u1 current() {
    return _epoch_state ? (u1)1 : (u1)0;
  }

  static u1 previous() {
    return _epoch_state ? (u1)0 : (u1)1;
  }

  static bool is_synchronizing() {
    return Atomic::load_acquire(&_synchronizing);
  }

  static traceid this_epoch_bit() {
    return _epoch_state ? EPOCH_1_BIT : EPOCH_0_BIT;
  }

  static traceid previous_epoch_bit() {
    return _epoch_state ? EPOCH_0_BIT : EPOCH_1_BIT;
  }

  static traceid this_epoch_method_bit() {
    return _epoch_state ? EPOCH_1_METHOD_BIT : EPOCH_0_METHOD_BIT;
  }

  static traceid previous_epoch_method_bit() {
    return _epoch_state ? EPOCH_0_METHOD_BIT : EPOCH_1_METHOD_BIT;
  }

  static traceid this_epoch_method_and_class_bits() {
    return _epoch_state ? EPOCH_1_METHOD_AND_CLASS_BITS : EPOCH_0_METHOD_AND_CLASS_BITS;
  }

  static traceid previous_epoch_method_and_class_bits() {
    return _epoch_state ? EPOCH_0_METHOD_AND_CLASS_BITS : EPOCH_1_METHOD_AND_CLASS_BITS;
  }

  static bool has_changed_tag_state() {
    return _tag_state.is_signaled_with_reset();
  }

  static bool has_changed_tag_state_no_reset() {
    return _tag_state.is_signaled();
  }

  static void set_changed_tag_state() {
    _tag_state.signal();
  }

  static address signal_address() {
    return _tag_state.signaled_address();
  }
};

#endif // SHARE_JFR_RECORDER_CHECKPOINT_TYPES_TRACEID_JFRTRACEIDEPOCH_HPP
