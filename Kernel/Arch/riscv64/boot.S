/*
 * Copyright (c) 2023, Sönke Holz <sholz830@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// In a specially-named text section so that the linker script can put it first in .text.
.section ".text.first"

.global start
.type start, @function
start:
  // We expect that only one hart jumps here and that we are running in supervisor mode.
  // We also expect that an implementation of the RISC-V Supervisor Binary Interface is available.

  // Don't touch a0/a1 as we expect those registers to contain the hart ID
  // and a pointer to the Flattened Fevice Tree.

  // Set sstatus to a known state (which includes disabling supervisor interrupts).
  csrw sstatus, zero

  // Also, disable all interrupts sources and mark them as non-pending.
  csrw sie, zero
  csrw sip, zero

  // TODO: maybe load the gp register here?

  // Clear the BSS.
  lla t0, start_of_bss
  lla t1, end_of_bss
  bgeu t0, t1, Lclear_bss_done
Lclear_bss_loop:
  sd zero, (t0)
  addi t0, t0, 8
  bltu t0, t1, Lclear_bss_loop
Lclear_bss_done:

  // Let the stack start before .text for now.
  lla sp, start

  // Zero all registers except sp, a0 and a1.
  li ra, 0
  // sp
  li gp, 0
  li tp, 0
  li t0, 0
  li t1, 0
  li t2, 0
  li fp, 0
  li s1, 0
  // a0
  // a1
  li a2, 0
  li a3, 0
  li a4, 0
  li a5, 0
  li a6, 0
  li a7, 0
  li s2, 0
  li s3, 0
  li s4, 0
  li s5, 0
  li s6, 0
  li s7, 0
  li s8, 0
  li s9, 0
  li s10, 0
  li s11, 0
  li t3, 0
  li t4, 0
  li t5, 0
  li t6, 0

  tail pre_init
