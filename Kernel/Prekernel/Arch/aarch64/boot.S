/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// In a specially-named text section so that the linker script can put it first in .text.
.section ".text.first"

.global start
.type start, @function
start:
  // Let only core 0 continue, put other cores to sleep.
  mrs x13, MPIDR_EL1
  and x13, x13, 0xff
  cbnz x13, halt

  // Let stack start before .text for now.
  // 512 kiB (0x80000) of stack are probably not sufficient, especially once we give the other cores some stack too,
  // but for now it's ok.
  ldr x14, =start
  mov sp, x14

  // Clear BSS.
  ldr x14, =start_of_bss
  ldr x15, =size_of_bss_divided_by_8
Lbss_clear_loop:
  str xzr, [x14], #8
  subs x15, x15, #1
  bne Lbss_clear_loop

  b init

.globl wait_cycles
.type wait_cycles, @function
wait_cycles:
Lstart:
  // This is probably too fast when caching and branch prediction is turned on.
  // FIXME: Make timer-based.
  subs x0, x0, #1
  bne Lstart
  ret
