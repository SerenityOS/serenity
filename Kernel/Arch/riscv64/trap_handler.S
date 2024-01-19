/*
 * Copyright (c) 2024, Sönke Holz <sholz830@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.section .text.asm_trap_handler

#define REGISTER_STATE_SIZE (36 * 8)
#if REGISTER_STATE_SIZE % 16 != 0
#    error "REGISTER_STATE_SIZE is not a multiple of 16 bytes!"
#endif

#define SSTATUS_SLOT        (31 * 8)
#define SEPC_SLOT           (32 * 8)
#define SCAUSE_SLOT         (33 * 8)
#define STVAL_SLOT          (34 * 8)
#define USERSPACE_SP_SLOT   (35 * 8)

.extern trap_handler

.p2align 2
.global asm_trap_handler
asm_trap_handler:
    // FIXME: Handle traps from userspace

    // Save the current register state to the current stack
    // and enter the C++ trap handler

    // Allocate stack space for trap frame
    addi sp, sp, -REGISTER_STATE_SIZE

    sd x1, 0*8(sp)
    sd x2, 1*8(sp)
    sd x3, 2*8(sp)
    sd x4, 3*8(sp)
    sd x5, 4*8(sp)
    sd x6, 5*8(sp)
    sd x7, 6*8(sp)
    sd x8, 7*8(sp)
    sd x9, 8*8(sp)
    sd x10, 9*8(sp)
    sd x11, 10*8(sp)
    sd x12, 11*8(sp)
    sd x13, 12*8(sp)
    sd x14, 13*8(sp)
    sd x15, 14*8(sp)
    sd x16, 15*8(sp)
    sd x17, 16*8(sp)
    sd x18, 17*8(sp)
    sd x19, 18*8(sp)
    sd x20, 19*8(sp)
    sd x21, 20*8(sp)
    sd x22, 21*8(sp)
    sd x23, 22*8(sp)
    sd x24, 23*8(sp)
    sd x25, 24*8(sp)
    sd x26, 25*8(sp)
    sd x27, 26*8(sp)
    sd x28, 27*8(sp)
    sd x29, 28*8(sp)
    sd x30, 29*8(sp)
    sd x31, 30*8(sp)

    // Let's save some special registers
    csrr t0, sstatus
    sd t0, SSTATUS_SLOT(sp)
    csrr t0, sepc
    sd t0, SEPC_SLOT(sp)

    // We also have to save those registers as interrupts are enabled during the page fault handling code.
    // A page fault exception may be reported as an interrupt in the register dump, if we wouldn't do that.
    csrr t0, scause
    sd t0, SCAUSE_SLOT(sp)
    csrr t0, stval
    sd t0, STVAL_SLOT(sp)

    // TODO
    sd zero, USERSPACE_SP_SLOT(sp)

    // Set up TrapFrame struct on the stack
    mv t0, sp
    addi sp, sp, -16
    sd t0, 1*8(sp)
    sd zero, 0*8(sp)

    // Move stack pointer into first argument register
    // and jump to the C++ trap handler
    mv a0, sp

    call trap_handler

    // Remove TrapFrame from the stack
    addi sp, sp, 16

    // Restore special registers first
    ld t0, SSTATUS_SLOT(sp)
    csrw sstatus, t0
    ld t0, SEPC_SLOT(sp)
    csrw sepc, t0

    ld x1, 0*8(sp)
    // sp
    ld x3, 2*8(sp)
    ld x4, 3*8(sp)
    ld x5, 4*8(sp)
    ld x6, 5*8(sp)
    ld x7, 6*8(sp)
    ld x8, 7*8(sp)
    ld x9, 8*8(sp)
    ld x10, 9*8(sp)
    ld x11, 10*8(sp)
    ld x12, 11*8(sp)
    ld x13, 12*8(sp)
    ld x14, 13*8(sp)
    ld x15, 14*8(sp)
    ld x16, 15*8(sp)
    ld x17, 16*8(sp)
    ld x18, 17*8(sp)
    ld x19, 18*8(sp)
    ld x20, 19*8(sp)
    ld x21, 20*8(sp)
    ld x22, 21*8(sp)
    ld x23, 22*8(sp)
    ld x24, 23*8(sp)
    ld x25, 24*8(sp)
    ld x26, 25*8(sp)
    ld x27, 26*8(sp)
    ld x28, 27*8(sp)
    ld x29, 28*8(sp)
    ld x30, 29*8(sp)
    ld x31, 30*8(sp)

    ld x2, 1*8(sp)

    addi sp, sp, REGISTER_STATE_SIZE
    sret
