/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <bits/sighow.h>

//
// /!\ Read setjmp.h before modifying this file!
//

.global _setjmp
.global setjmp
_setjmp:
setjmp:
    mov $0, %esi            // Set val argument to 0

.global sigsetjmp
sigsetjmp:
    mov %esi, 64(%rdi)      // Store val into did_save_signal_mask
    movl $0, 68(%rdi)       // Clear saved_signal_mask
    test %esi, %esi
    jz .Lsaveregs

    push %rbp               // Prepare ABI-compliant call to sigprocmask
    mov %rsp, %rbp
    push %rdi
    lea 68(%rdi), %rdx      // Set argument oldset
    mov $0, %rsi            // Set argument set
    mov $0, %rdi            // Set argument how
    call sigprocmask@plt
    pop %rdi
    pop %rbp

.Lsaveregs:
    mov %rbx, (0 * 8)(%rdi) // Save registers
    mov %r12, (1 * 8)(%rdi)
    mov %r13, (2 * 8)(%rdi)
    mov %r14, (3 * 8)(%rdi)
    mov %r15, (4 * 8)(%rdi)
    mov %rbp, (5 * 8)(%rdi)
    mov %rsp, (6 * 8)(%rdi)
    mov (%rsp), %rax        // Grab return address
    mov %rax, (7 * 8)(%rdi)
    xor %eax, %eax
    ret

.global _longjmp
.global longjmp
_longjmp:
longjmp:
    mov %esi, %eax
    test %eax, %eax
    jnz  .Lnonzero
    mov $1, %eax

.Lnonzero:
    mov (0 * 8)(%rdi), %rbx // Restore registers
    mov (1 * 8)(%rdi), %r12
    mov (2 * 8)(%rdi), %r13
    mov (3 * 8)(%rdi), %r14
    mov (4 * 8)(%rdi), %r15
    mov (5 * 8)(%rdi), %rbp
    //
    // Until this point, the stack is still from the caller.
    //
    mov (6 * 8)(%rdi), %rsp
    mov (7 * 8)(%rdi), %rcx
    mov %rcx, (%rsp)        // Patch return address
    //
    // From this point on, the former stack has been restored.
    //
    ret
