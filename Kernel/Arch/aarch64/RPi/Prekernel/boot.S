/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// In a specially-named text section which isn't relocated.
.section ".entry_stub.text", "ax"

.global start
.type start, @function
start:
  // Let only core 0 continue, put other cores to sleep.
  mrs x13, MPIDR_EL1
  and x13, x13, 0xff
  cbnz x13, wait_for_copy

  // Zero registers.
  mov x0, xzr
  mov x1, xzr
  mov x2, xzr
  mov x3, xzr
  mov x4, xzr
  mov x5, xzr
  mov x6, xzr
  mov x7, xzr
  mov x8, xzr
  mov x9, xzr
  mov x10, xzr
  mov x11, xzr
  mov x12, xzr
  mov x13, xzr
  mov x14, xzr
  mov x15, xzr
  mov x16, xzr
  mov x17, xzr
  mov x18, xzr
  mov x19, xzr
  mov x20, xzr
  mov x21, xzr
  mov x22, xzr
  mov x23, xzr
  mov x24, xzr
  mov x25, xzr
  mov x26, xzr
  mov x27, xzr
  mov x28, xzr
  mov x29, xzr
  mov x30, xzr

  // Move the prekernel to addresses below 0x80000 so that we can safely load the Kernel there later.
  // x10 = scratch, x12 = target, x14 = source, x15 = loop counter
  ldr x14, =prekernel_load_base
  ldr x15, =size_of_prekernel_image_divided_by_8
  ldr x12, =start_of_prekernel_image
prekernel_image_move_loop:
  ldr x10, [x14], #8
  str x10, [x12], #8
  subs x15, x15, #1
  bne prekernel_image_move_loop

move_loop_exit:
  // Let stack start before any kernel/prekernel code.
  ldr x10, =PREKERNEL_BASE
  mov sp, x10

  // Clear (moved) BSS.
  ldr x14, =start_of_bss
  ldr x15, =size_of_bss_divided_by_8
  // Ensure that we don't get an infinite loop if BSS is empty.
  cbz x15, signal_copy_end
bss_clear_loop:
  str xzr, [x14], #8
  subs x15, x15, #1
  bne bss_clear_loop

signal_copy_end:
  // Signal cores to jump to halt.
  adr x10, copy_finished
  mov x11, #1
  str x11, [x10]
  // TODO: If the other cores jump too slowly and we already start writing a Kernel binary, they might execute wrong code.
  //       In practice, this seems improbable, however.

jump_to_entry:
  // Jump to entry point.
  isb
  bl init

wait_for_copy:
  adr x10, copy_finished
wait_for_copy_loop:
  ldr x11, [x10]
  cbz x11, wait_for_copy_loop

  isb
  b halt

.section ".entry_stub.data", "a"
copy_finished:
  .word 0
