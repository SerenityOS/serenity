/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <bits/sighow.h>

//
// /!\ Read setjmp.h before modifying this file!
//

.global setjmp
setjmp:
    mov $0, %esi            // Set val argument to 0

.global sigsetjmp
sigsetjmp:
    mov %esi, 60(%rdi)      // Store val into did_save_signal_mask
    movl $0, 64(%rdi)       // Clear saved_signal_mask
    test %esi, %esi
    jz .Lsaveregs

    mov %rdi, %r12          // Preserve sigjmp_buf argument
    mov $0, %rdi            // Set argument how
    mov $0, %rsi            // Set argument set
    lea 64(%rdi), %rdx      // Set argument oldset
    call sigprocmask
    mov %r12, %rdi          // Restore sigjmp_buf argument

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

.global longjmp
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
