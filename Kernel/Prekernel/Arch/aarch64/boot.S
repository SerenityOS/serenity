/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.text

.global start
.type start, @function
start:
  // Let only core 0 continue, put other cores to sleep.
  mrs x13, MPIDR_EL1
  and x13, x13, 0xff
  cbnz x13, Lhalt

  // Let stack start before .text for now.
  // 512 kiB (0x8000) of stack are probably not sufficient, especially once we give the other cores some stack too,
  // but for now it's ok.
  ldr x14, =start
  mov sp, x14

  b init

Lhalt:
  wfi
  b Lhalt
