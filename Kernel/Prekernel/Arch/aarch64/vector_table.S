/*
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.section .text.vector_table

// Vector Table Entry macro. Each entry is aligned at 128 bytes, meaning we have
// at most that many instructions.
.macro table_entry label
    .align 7
    b \label
.endm

.macro unimplemented_entry
    .align 7
    wfe
    b .
.endm

.global vector_table_el1
.weak vector_table_el1 // Vector table is weak in case someone wants to hook us in C++ land :^)
.type vector_table_el1, @object

// Vector table is 2KiB aligned (2^11)
.align 11
vector_table_el1:
    // Exceptions taken from Current EL, with SP_EL0
    unimplemented_entry
    unimplemented_entry
    unimplemented_entry
    unimplemented_entry

    // Exceptions taken from Current EL, with SP_ELx, x>0
    table_entry synchronous_current_elsp_elx
    table_entry irq_current_elsp_elx
    table_entry fiq_current_elsp_elx
    table_entry system_error_current_elsp_elx

    // Exceptions from Lower EL, where causing application is in AArch64 mode
    unimplemented_entry
    unimplemented_entry
    unimplemented_entry
    unimplemented_entry

    // Exceptions from Lower EL, where causing application is in AArch32 mode
    unimplemented_entry
    unimplemented_entry
    unimplemented_entry
    unimplemented_entry

synchronous_current_elsp_elx:
    b .

irq_current_elsp_elx:
    b .

fiq_current_elsp_elx:
    b .

system_error_current_elsp_elx:
    b .
