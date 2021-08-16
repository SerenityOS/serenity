/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_STACKOVERFLOW_HPP
#define SHARE_RUNTIME_STACKOVERFLOW_HPP

#include "utilities/align.hpp"
#include "utilities/debug.hpp"

class JavaThread;

// StackOverflow handling is encapsulated in this class.  This class contains state variables
// for each JavaThread that are used to detect stack overflow though explicit checks or through
// checks in the signal handler when stack banging into guard pages causes a trap.
// The state variables also record whether guard pages are enabled or disabled.

class StackOverflow {
  friend class JVMCIVMStructs;
  friend class JavaThread;
 public:
  // State of the stack guard pages for the containing thread.
  enum StackGuardState {
    stack_guard_unused,         // not needed
    stack_guard_reserved_disabled,
    stack_guard_yellow_reserved_disabled,// disabled (temporarily) after stack overflow
    stack_guard_enabled         // enabled
  };

  StackOverflow() :
    _stack_guard_state(stack_guard_unused),
    _stack_overflow_limit(nullptr),
    _reserved_stack_activation(nullptr),  // stack base not known yet
    _stack_base(nullptr), _stack_end(nullptr) {}

  // Initialization after thread is started.
  void initialize(address base, address end) {
     _stack_base = base;
     _stack_end = end;
    set_stack_overflow_limit();
    set_reserved_stack_activation(base);
  }
 private:

  StackGuardState  _stack_guard_state;

  // Precompute the limit of the stack as used in stack overflow checks.
  // We load it from here to simplify the stack overflow check in assembly.
  address          _stack_overflow_limit;
  address          _reserved_stack_activation;

  // Support for stack overflow handling, copied down from thread.
  address          _stack_base;
  address          _stack_end;

  address stack_end()  const           { return _stack_end; }
  address stack_base() const           { assert(_stack_base != nullptr, "Sanity check"); return _stack_base; }

  // Stack overflow support
  //
  //  (low addresses)
  //
  //  --  <-- stack_end()                   ---
  //  |                                      |
  //  |  red zone                            |
  //  |                                      |
  //  --  <-- stack_red_zone_base()          |
  //  |                                      |
  //  |                                     guard
  //  |  yellow zone                        zone
  //  |                                      |
  //  |                                      |
  //  --  <-- stack_yellow_zone_base()       |
  //  |                                      |
  //  |                                      |
  //  |  reserved zone                       |
  //  |                                      |
  //  --  <-- stack_reserved_zone_base()    ---      ---
  //                                                 /|\  shadow     <--  stack_overflow_limit() (somewhere in here)
  //                                                  |   zone
  //                                                 \|/  size
  //  some untouched memory                          ---
  //
  //
  //  --
  //  |
  //  |  shadow zone
  //  |
  //  --
  //  x    frame n
  //  --
  //  x    frame n-1
  //  x
  //  --
  //  ...
  //
  //  --
  //  x    frame 0
  //  --  <-- stack_base()
  //
  //  (high addresses)
  //

 private:
  // These values are derived from flags StackRedPages, StackYellowPages,
  // StackReservedPages and StackShadowPages.
  static size_t _stack_red_zone_size;
  static size_t _stack_yellow_zone_size;
  static size_t _stack_reserved_zone_size;
  static size_t _stack_shadow_zone_size;

 public:
  static void initialize_stack_zone_sizes();

  static size_t stack_red_zone_size() {
    assert(_stack_red_zone_size > 0, "Don't call this before the field is initialized.");
    return _stack_red_zone_size;
  }

  // Returns base of red zone (one-beyond the highest red zone address, so
  //  itself outside red zone and the highest address of the yellow zone).
  address stack_red_zone_base() const {
    return (address)(stack_end() + stack_red_zone_size());
  }

  // Returns true if address points into the red zone.
  bool in_stack_red_zone(address a) const {
    return a < stack_red_zone_base() && a >= stack_end();
  }

  static size_t stack_yellow_zone_size() {
    assert(_stack_yellow_zone_size > 0, "Don't call this before the field is initialized.");
    return _stack_yellow_zone_size;
  }

  static size_t stack_reserved_zone_size() {
    // _stack_reserved_zone_size may be 0. This indicates the feature is off.
    return _stack_reserved_zone_size;
  }

  // Returns base of the reserved zone (one-beyond the highest reserved zone address).
  address stack_reserved_zone_base() const {
    return (address)(stack_end() +
                     (stack_red_zone_size() + stack_yellow_zone_size() + stack_reserved_zone_size()));
  }

  // Returns true if address points into the reserved zone.
  bool in_stack_reserved_zone(address a) const {
    return (a < stack_reserved_zone_base()) &&
           (a >= (address)((intptr_t)stack_reserved_zone_base() - stack_reserved_zone_size()));
  }

  static size_t stack_yellow_reserved_zone_size() {
    return _stack_yellow_zone_size + _stack_reserved_zone_size;
  }

  // Returns true if a points into either yellow or reserved zone.
  bool in_stack_yellow_reserved_zone(address a) const {
    return (a < stack_reserved_zone_base()) && (a >= stack_red_zone_base());
  }

  // Size of red + yellow + reserved zones.
  static size_t stack_guard_zone_size() {
    return stack_red_zone_size() + stack_yellow_reserved_zone_size();
  }

  static size_t stack_shadow_zone_size() {
    assert(_stack_shadow_zone_size > 0, "Don't call this before the field is initialized.");
    return _stack_shadow_zone_size;
  }

  void create_stack_guard_pages();
  void remove_stack_guard_pages();

  void enable_stack_reserved_zone(bool check_if_disabled = false);
  void disable_stack_reserved_zone();
  void enable_stack_yellow_reserved_zone();
  void disable_stack_yellow_reserved_zone();
  void enable_stack_red_zone();
  void disable_stack_red_zone();

  bool stack_guard_zone_unused() const { return _stack_guard_state == stack_guard_unused; }

  bool stack_yellow_reserved_zone_disabled() const {
    return _stack_guard_state == stack_guard_yellow_reserved_disabled;
  }

  size_t stack_available(address cur_sp) const {
    // This code assumes java stacks grow down
    address low_addr; // Limit on the address for deepest stack depth
    if (_stack_guard_state == stack_guard_unused) {
      low_addr = stack_end();
    } else {
      low_addr = stack_reserved_zone_base();
    }
    return cur_sp > low_addr ? cur_sp - low_addr : 0;
  }

  bool stack_guards_enabled() const;

  address reserved_stack_activation() const { return _reserved_stack_activation; }
  void set_reserved_stack_activation(address addr) {
    assert(_reserved_stack_activation == stack_base()
            || _reserved_stack_activation == nullptr
            || addr == stack_base(), "Must not be set twice");
    _reserved_stack_activation = addr;
  }

  // Attempt to reguard the stack after a stack overflow may have occurred.
  // Returns true if (a) guard pages are not needed on this thread, (b) the
  // pages are already guarded, or (c) the pages were successfully reguarded.
  // Returns false if there is not enough stack space to reguard the pages, in
  // which case the caller should unwind a frame and try again.  The argument
  // should be the caller's (approximate) sp.
  bool reguard_stack(address cur_sp);
  // Similar to above but see if current stackpoint is out of the guard area
  // and reguard if possible.
  bool reguard_stack(void);
  bool reguard_stack_if_needed(void);

  void set_stack_overflow_limit() {
    _stack_overflow_limit =
      stack_end() + MAX2(stack_guard_zone_size(), stack_shadow_zone_size());
  }
};

#endif // SHARE_RUNTIME_STACKOVERFLOW_HPP
