/*
 * Copyright (c) 2024, Sönke Holz <sholz830@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.section .text.asm_trap_handler

#define XLEN 8

#define REGISTER_STATE_SIZE (36 * XLEN)
#if REGISTER_STATE_SIZE % 16 != 0
#    error "REGISTER_STATE_SIZE is not a multiple of 16 bytes!"
#endif

#define SSTATUS_SLOT        (31 * XLEN)
#define SEPC_SLOT           (32 * XLEN)
#define SCAUSE_SLOT         (33 * XLEN)
#define STVAL_SLOT          (34 * XLEN)
#define USERSPACE_SP_SLOT   (35 * XLEN)

.extern trap_handler

.p2align 2
.global asm_trap_handler
asm_trap_handler:
    // FIXME: Handle traps from userspace

    // Save the current register state to the current stack
    // and enter the C++ trap handler

    // Allocate stack space for trap frame
    addi sp, sp, -REGISTER_STATE_SIZE

    sd x1, 0*XLEN(sp)
    sd x2, 1*XLEN(sp)
    sd x3, 2*XLEN(sp)
    sd x4, 3*XLEN(sp)
    sd x5, 4*XLEN(sp)
    sd x6, 5*XLEN(sp)
    sd x7, 6*XLEN(sp)
    sd x8, 7*XLEN(sp)
    sd x9, 8*XLEN(sp)
    sd x10, 9*XLEN(sp)
    sd x11, 10*XLEN(sp)
    sd x12, 11*XLEN(sp)
    sd x13, 12*XLEN(sp)
    sd x14, 13*XLEN(sp)
    sd x15, 14*XLEN(sp)
    sd x16, 15*XLEN(sp)
    sd x17, 16*XLEN(sp)
    sd x18, 17*XLEN(sp)
    sd x19, 18*XLEN(sp)
    sd x20, 19*XLEN(sp)
    sd x21, 20*XLEN(sp)
    sd x22, 21*XLEN(sp)
    sd x23, 22*XLEN(sp)
    sd x24, 23*XLEN(sp)
    sd x25, 24*XLEN(sp)
    sd x26, 25*XLEN(sp)
    sd x27, 26*XLEN(sp)
    sd x28, 27*XLEN(sp)
    sd x29, 28*XLEN(sp)
    sd x30, 29*XLEN(sp)
    sd x31, 30*XLEN(sp)

    // Let's save some special registers
    csrr t0, sstatus
    sd t0, SSTATUS_SLOT(sp)
    csrr t0, sepc
    sd t0, SEPC_SLOT(sp)

    // We also have to those registers as interrupts are enabled during the page fault handling code.
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
    sd t0, 1*XLEN(sp)
    sd zero, 0*XLEN(sp)

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

    ld x1, 0*XLEN(sp)
    // sp
    ld x3, 2*XLEN(sp)
    ld x4, 3*XLEN(sp)
    ld x5, 4*XLEN(sp)
    ld x6, 5*XLEN(sp)
    ld x7, 6*XLEN(sp)
    ld x8, 7*XLEN(sp)
    ld x9, 8*XLEN(sp)
    ld x10, 9*XLEN(sp)
    ld x11, 10*XLEN(sp)
    ld x12, 11*XLEN(sp)
    ld x13, 12*XLEN(sp)
    ld x14, 13*XLEN(sp)
    ld x15, 14*XLEN(sp)
    ld x16, 15*XLEN(sp)
    ld x17, 16*XLEN(sp)
    ld x18, 17*XLEN(sp)
    ld x19, 18*XLEN(sp)
    ld x20, 19*XLEN(sp)
    ld x21, 20*XLEN(sp)
    ld x22, 21*XLEN(sp)
    ld x23, 22*XLEN(sp)
    ld x24, 23*XLEN(sp)
    ld x25, 24*XLEN(sp)
    ld x26, 25*XLEN(sp)
    ld x27, 26*XLEN(sp)
    ld x28, 27*XLEN(sp)
    ld x29, 28*XLEN(sp)
    ld x30, 29*XLEN(sp)
    ld x31, 30*XLEN(sp)

    ld x2, 1*XLEN(sp)
    addi sp, sp, REGISTER_STATE_SIZE
    sret


.global restore_context_and_sret
restore_context_and_sret:
    mv a0, sp
    call exit_trap

    // Remove TrapFrame from the stack
    addi sp, sp, 16

    // Restore special registers first
    ld t0, SSTATUS_SLOT(sp)
    csrw sstatus, t0
    ld t0, SEPC_SLOT(sp)
    csrw sepc, t0

    ld x1, 0*XLEN(sp)
    // sp
    ld x3, 2*XLEN(sp)
    ld x4, 3*XLEN(sp)
    ld x5, 4*XLEN(sp)
    ld x6, 5*XLEN(sp)
    ld x7, 6*XLEN(sp)
    ld x8, 7*XLEN(sp)
    ld x9, 8*XLEN(sp)
    ld x10, 9*XLEN(sp)
    ld x11, 10*XLEN(sp)
    ld x12, 11*XLEN(sp)
    ld x13, 12*XLEN(sp)
    ld x14, 13*XLEN(sp)
    ld x15, 14*XLEN(sp)
    ld x16, 15*XLEN(sp)
    ld x17, 16*XLEN(sp)
    ld x18, 17*XLEN(sp)
    ld x19, 18*XLEN(sp)
    ld x20, 19*XLEN(sp)
    ld x21, 20*XLEN(sp)
    ld x22, 21*XLEN(sp)
    ld x23, 22*XLEN(sp)
    ld x24, 23*XLEN(sp)
    ld x25, 24*XLEN(sp)
    ld x26, 25*XLEN(sp)
    ld x27, 26*XLEN(sp)
    ld x28, 27*XLEN(sp)
    ld x29, 28*XLEN(sp)
    ld x30, 29*XLEN(sp)
    ld x31, 30*XLEN(sp)

    // FIXME: don't clobber t0 and t1
    ld t0, USERSPACE_SP_SLOT(sp)
    addi sp, sp, REGISTER_STATE_SIZE

    csrr t1, sstatus
    srl t1, t1, 8
    andi t1, t1, 1
    beqz t1, 1f

    // Kernel
    csrw sscratch, zero
    j 2f

1:
    // User
    csrw sscratch, sp

2:
    mv sp, t0
    sret
