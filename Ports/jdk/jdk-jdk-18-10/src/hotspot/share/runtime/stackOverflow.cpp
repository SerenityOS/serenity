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

#include "precompiled.hpp"
#include "logging/log.hpp"
#include "runtime/os.inline.hpp"
#include "runtime/stackOverflow.hpp"
#include "runtime/thread.hpp"
#include "utilities/align.hpp"
#include "utilities/globalDefinitions.hpp"

size_t StackOverflow::_stack_red_zone_size = 0;
size_t StackOverflow::_stack_yellow_zone_size = 0;
size_t StackOverflow::_stack_reserved_zone_size = 0;
size_t StackOverflow::_stack_shadow_zone_size = 0;

void StackOverflow::initialize_stack_zone_sizes() {
  // Stack zone sizes must be page aligned.
  size_t page_size = os::vm_page_size();

  // We need to adapt the configured number of stack protection pages given
  // in 4K pages to the actual os page size. We must do this before setting
  // up minimal stack sizes etc. in os::init_2().
  size_t alignment = 4*K;

  assert(_stack_red_zone_size == 0, "This should be called only once.");
  _stack_red_zone_size = align_up(StackRedPages * alignment, page_size);

  assert(_stack_yellow_zone_size == 0, "This should be called only once.");
  _stack_yellow_zone_size = align_up(StackYellowPages * alignment, page_size);

  assert(_stack_reserved_zone_size == 0, "This should be called only once.");
  _stack_reserved_zone_size = align_up(StackReservedPages * alignment, page_size);

  // The shadow area is not allocated or protected, so
  // it needs not be page aligned.
  // But the stack bang currently assumes that it is a
  // multiple of page size. This guarantees that the bang
  // loop touches all pages in the shadow zone.
  // This can be guaranteed differently, as well.  E.g., if
  // the page size is a multiple of 4K, banging in 4K steps
  // suffices to touch all pages. (Some pages are banged
  // several times, though.)
  assert(_stack_shadow_zone_size == 0, "This should be called only once.");
  _stack_shadow_zone_size = align_up(StackShadowPages * alignment, page_size);
}

bool StackOverflow::stack_guards_enabled() const {
#ifdef ASSERT
  if (os::uses_stack_guard_pages() &&
      !(DisablePrimordialThreadGuardPages && os::is_primordial_thread())) {
    assert(_stack_guard_state != stack_guard_unused, "guard pages must be in use");
  }
#endif
  return _stack_guard_state == stack_guard_enabled;
}

void StackOverflow::create_stack_guard_pages() {
  if (!os::uses_stack_guard_pages() ||
      _stack_guard_state != stack_guard_unused ||
      (DisablePrimordialThreadGuardPages && os::is_primordial_thread())) {
      log_info(os, thread)("Stack guard page creation for thread "
                           UINTX_FORMAT " disabled", os::current_thread_id());
    return;
  }
  address low_addr = stack_end();
  size_t len = stack_guard_zone_size();

  assert(is_aligned(low_addr, os::vm_page_size()), "Stack base should be the start of a page");
  assert(is_aligned(len, os::vm_page_size()), "Stack size should be a multiple of page size");

  int must_commit = os::must_commit_stack_guard_pages();
  // warning("Guarding at " PTR_FORMAT " for len " SIZE_FORMAT "\n", low_addr, len);

  if (must_commit && !os::create_stack_guard_pages((char *) low_addr, len)) {
    log_warning(os, thread)("Attempt to allocate stack guard pages failed.");
    return;
  }

  if (os::guard_memory((char *) low_addr, len)) {
    _stack_guard_state = stack_guard_enabled;
  } else {
    log_warning(os, thread)("Attempt to protect stack guard pages failed ("
      PTR_FORMAT "-" PTR_FORMAT ").", p2i(low_addr), p2i(low_addr + len));
    vm_exit_out_of_memory(len, OOM_MPROTECT_ERROR, "memory to guard stack pages");
  }

  log_debug(os, thread)("Thread " UINTX_FORMAT " stack guard pages activated: "
    PTR_FORMAT "-" PTR_FORMAT ".",
    os::current_thread_id(), p2i(low_addr), p2i(low_addr + len));
}

void StackOverflow::remove_stack_guard_pages() {
  if (_stack_guard_state == stack_guard_unused) return;
  address low_addr = stack_end();
  size_t len = stack_guard_zone_size();

  if (os::must_commit_stack_guard_pages()) {
    if (os::remove_stack_guard_pages((char *) low_addr, len)) {
      _stack_guard_state = stack_guard_unused;
    } else {
      log_warning(os, thread)("Attempt to deallocate stack guard pages failed ("
        PTR_FORMAT "-" PTR_FORMAT ").", p2i(low_addr), p2i(low_addr + len));
      return;
    }
  } else {
    if (_stack_guard_state == stack_guard_unused) return;
    if (os::unguard_memory((char *) low_addr, len)) {
      _stack_guard_state = stack_guard_unused;
    } else {
      log_warning(os, thread)("Attempt to unprotect stack guard pages failed ("
        PTR_FORMAT "-" PTR_FORMAT ").", p2i(low_addr), p2i(low_addr + len));
      return;
    }
  }

  log_debug(os, thread)("Thread " UINTX_FORMAT " stack guard pages removed: "
    PTR_FORMAT "-" PTR_FORMAT ".",
    os::current_thread_id(), p2i(low_addr), p2i(low_addr + len));
}

void StackOverflow::enable_stack_reserved_zone(bool check_if_disabled) {
  if (check_if_disabled && _stack_guard_state == stack_guard_reserved_disabled) {
    return;
  }
  assert(_stack_guard_state == stack_guard_reserved_disabled, "inconsistent state");

  // The base notation is from the stack's point of view, growing downward.
  // We need to adjust it to work correctly with guard_memory()
  address base = stack_reserved_zone_base() - stack_reserved_zone_size();

  guarantee(base < stack_base(),"Error calculating stack reserved zone");
  guarantee(base < os::current_stack_pointer(),"Error calculating stack reserved zone");

  if (os::guard_memory((char *) base, stack_reserved_zone_size())) {
    _stack_guard_state = stack_guard_enabled;
  } else {
    warning("Attempt to guard stack reserved zone failed.");
  }
}

void StackOverflow::disable_stack_reserved_zone() {
  assert(_stack_guard_state == stack_guard_enabled, "inconsistent state");

  // Simply return if called for a thread that does not use guard pages.
  if (_stack_guard_state != stack_guard_enabled) return;

  // The base notation is from the stack's point of view, growing downward.
  // We need to adjust it to work correctly with guard_memory()
  address base = stack_reserved_zone_base() - stack_reserved_zone_size();

  if (os::unguard_memory((char *)base, stack_reserved_zone_size())) {
    _stack_guard_state = stack_guard_reserved_disabled;
  } else {
    warning("Attempt to unguard stack reserved zone failed.");
  }
}

void StackOverflow::enable_stack_yellow_reserved_zone() {
  assert(_stack_guard_state != stack_guard_unused, "must be using guard pages.");
  assert(_stack_guard_state != stack_guard_enabled, "already enabled");

  // The base notation is from the stacks point of view, growing downward.
  // We need to adjust it to work correctly with guard_memory()
  address base = stack_red_zone_base();

  guarantee(base < stack_base(), "Error calculating stack yellow zone");
  guarantee(base < os::current_stack_pointer(), "Error calculating stack yellow zone");

  if (os::guard_memory((char *) base, stack_yellow_reserved_zone_size())) {
    _stack_guard_state = stack_guard_enabled;
  } else {
    warning("Attempt to guard stack yellow zone failed.");
  }
}

void StackOverflow::disable_stack_yellow_reserved_zone() {
  assert(_stack_guard_state != stack_guard_unused, "must be using guard pages.");
  assert(_stack_guard_state != stack_guard_yellow_reserved_disabled, "already disabled");

  // Simply return if called for a thread that does not use guard pages.
  if (_stack_guard_state == stack_guard_unused) return;

  // The base notation is from the stacks point of view, growing downward.
  // We need to adjust it to work correctly with guard_memory()
  address base = stack_red_zone_base();

  if (os::unguard_memory((char *)base, stack_yellow_reserved_zone_size())) {
    _stack_guard_state = stack_guard_yellow_reserved_disabled;
  } else {
    warning("Attempt to unguard stack yellow zone failed.");
  }
}

void StackOverflow::enable_stack_red_zone() {
  // The base notation is from the stacks point of view, growing downward.
  // We need to adjust it to work correctly with guard_memory()
  assert(_stack_guard_state != stack_guard_unused, "must be using guard pages.");
  address base = stack_red_zone_base() - stack_red_zone_size();

  guarantee(base < stack_base(), "Error calculating stack red zone");
  guarantee(base < os::current_stack_pointer(), "Error calculating stack red zone");

  if (!os::guard_memory((char *) base, stack_red_zone_size())) {
    warning("Attempt to guard stack red zone failed.");
  }
}

void StackOverflow::disable_stack_red_zone() {
  // The base notation is from the stacks point of view, growing downward.
  // We need to adjust it to work correctly with guard_memory()
  assert(_stack_guard_state != stack_guard_unused, "must be using guard pages.");
  address base = stack_red_zone_base() - stack_red_zone_size();
  if (!os::unguard_memory((char *)base, stack_red_zone_size())) {
    warning("Attempt to unguard stack red zone failed.");
  }
}

bool StackOverflow::reguard_stack(address cur_sp) {
  if (_stack_guard_state != stack_guard_yellow_reserved_disabled
      && _stack_guard_state != stack_guard_reserved_disabled) {
    return true; // Stack already guarded or guard pages not needed.
  }

  // Java code never executes within the yellow zone: the latter is only
  // there to provoke an exception during stack banging.  If java code
  // is executing there, either StackShadowPages should be larger, or
  // some exception code in c1, c2 or the interpreter isn't unwinding
  // when it should.
  guarantee(cur_sp > stack_reserved_zone_base(),
            "not enough space to reguard - increase StackShadowPages");
  if (_stack_guard_state == stack_guard_yellow_reserved_disabled) {
    enable_stack_yellow_reserved_zone();
    if (reserved_stack_activation() != stack_base()) {
      set_reserved_stack_activation(stack_base());
    }
  } else if (_stack_guard_state == stack_guard_reserved_disabled) {
    set_reserved_stack_activation(stack_base());
    enable_stack_reserved_zone();
  }
  return true;
}

bool StackOverflow::reguard_stack(void) {
  return reguard_stack(os::current_stack_pointer());
}

bool StackOverflow::reguard_stack_if_needed() {
  return !stack_guards_enabled() ? reguard_stack() : true;
}
