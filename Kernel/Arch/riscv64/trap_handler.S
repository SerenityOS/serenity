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

.macro save_gpr_state_to_stack
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
.endm

.macro load_gpr_state_from_stack_except_sp
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
.endm

.p2align 2
.global asm_trap_handler
asm_trap_handler:
    // We entered here from either the kernel or userland
    // So we have to find out if we came here from userland and if so, switch to the kernel stack

    // Swap contents of sscratch and sp
    csrrw sp, sscratch, sp

    // sp now contains the value of sscratch when we entered the trap handler.
    // When this value is 0, we were already in supervisor (kernel) mode.
    // Otherwise, the value in sp is now the kernel stack and sscratch contains the user stack pointer.
    beqz sp, .Ltrap_is_from_kernel

    j .Ltrap_is_from_userland

.Ltrap_is_from_kernel:
    // Store 0 in sscratch and write old value inside sscratch to sp,
    // as sscratch and sp were swapped, so sscratch has the kernel stack pointer
    csrrw sp, sscratch, zero

.Ltrap_is_from_userland:
    // sscratch now contains the user stack pointer or 0 if the trap was from supervisor mode
    // and sp points to the kernel stack

    // Save the current register state on the kernel stack

    // Allocate stack space for RegisterState struct
    addi sp, sp, -REGISTER_STATE_SIZE

    save_gpr_state_to_stack

    // Save some special register to correctly handle the trap
    csrr t0, sepc
    sd t0, SEPC_SLOT(sp)

    // Also store these register to be able to display the state of them before at trap entry
    // We also might get an interrupt while handling page faults, so scause and stval would be changed by the interrupt.
    csrr t0, scause
    sd t0, SCAUSE_SLOT(sp)
    csrr t0, stval
    sd t0, STVAL_SLOT(sp)
    csrr t0, sstatus
    sd t0, SSTATUS_SLOT(sp)

    // Read user stack pointer from sscratch (which is 0 if trap is from kernel)
    // And set sscratch to 0, as we are currently in the kernel
    csrrw t0, sscratch, zero
    sd t0, USERSPACE_SP_SLOT(sp)

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

    csrr t0, sstatus
    srl t0, t0, 8 // SPP (previous privilege mode)
    andi t0, t0, 1
    beqz t0, .Lreturn_from_trap_to_user

    // Return to kernel
    csrw sscratch, zero

    load_gpr_state_from_stack_except_sp
    ld sp, 1*8(sp)

    // Remove RegisterState struct from kernel stack
    addi sp, sp, REGISTER_STATE_SIZE

    sret

.Lreturn_from_trap_to_user:
    // Store sp with RegisterState struct removed to sscratch (which contains the kernel stack pointer)
    addi t0, sp, REGISTER_STATE_SIZE
    csrw sscratch, t0

    load_gpr_state_from_stack_except_sp

    // Load user stack pointer from RegisterState struct on kernel stack
    ld sp, USERSPACE_SP_SLOT(sp)

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

    csrr t0, sstatus
    srl t0, t0, 8 // SPP (previous privilege mode)
    andi t0, t0, 1
    beqz t0, .Lreturn_to_user

    // Return to kernel
    csrw sscratch, zero

    load_gpr_state_from_stack_except_sp

    // Remove RegisterState struct from kernel stack
    addi sp, sp, REGISTER_STATE_SIZE

    sret

.Lreturn_to_user:
    // Store sp with RegisterState struct removed to sscratch (which contains the kernel stack pointer)
    addi t0, sp, REGISTER_STATE_SIZE
    csrw sscratch, t0

    load_gpr_state_from_stack_except_sp

    // Load user stack pointer from RegisterState struct on kernel stack
    ld sp, USERSPACE_SP_SLOT(sp)

    sret
