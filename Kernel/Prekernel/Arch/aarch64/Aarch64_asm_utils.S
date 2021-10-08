/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.global get_current_exception_level
.type get_current_exception_level, @function
get_current_exception_level:
  mrs x0, CurrentEL
  lsr x0, x0, #2
  and x0, x0, #0x3
  ret

.global wait_cycles
.type wait_cycles, @function
wait_cycles:
Lstart:
  // This is probably too fast when caching and branch prediction is turned on.
  // FIXME: Make timer-based.
  subs x0, x0, #1
  bne Lstart
  ret
