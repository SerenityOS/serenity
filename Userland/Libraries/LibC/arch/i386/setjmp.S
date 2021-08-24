/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <bits/sighow.h>

//
// /!\ Read setjmp.h before modifying this file!
//

.Lget_pc:
    mov (%esp), %ebx
    ret

.global setjmp
setjmp:
    xor %eax, %eax          // Grab val argument (hardcoded to zero)
    jmp .Lsigset_common

.global sigsetjmp
sigsetjmp:
    mov 8(%esp), %eax       // Grab val argument

.Lsigset_common:
    mov 4(%esp), %ecx       // Grab jmp_buf argument
    mov %eax, 24(%ecx)      // Store val into did_save_signal_mask
    movl $0, 28(%ecx)       // Clear saved_signal_mask
    test %eax, %eax
    jz .Lsaveregs

    push %ebp               // Prepare ABI-compliant call to sigprocmask
    mov %esp, %ebp
    push %ebx
    call .Lget_pc           // Grab the GOT pointer
    addl $_GLOBAL_OFFSET_TABLE_, %ebx

    lea 28(%ecx), %eax      // Set argument oldset
    push %eax
    push $0                 // Set argument set
    push $0                 // Set argument how
    call sigprocmask@plt
    add $12, %esp
    pop %ebx
    pop %ebp

.Lsaveregs:
    mov 4(%esp), %ecx       // Grab jmp_buf argument
    mov (%esp), %edx        // Grab return address
    mov %ebx, (0 * 4)(%ecx) // Save registers
    mov %esi, (1 * 4)(%ecx)
    mov %edi, (2 * 4)(%ecx)
    mov %ebp, (3 * 4)(%ecx)
    mov %esp, (4 * 4)(%ecx)
    mov %edx, (5 * 4)(%ecx)
    xor %eax, %eax
    ret

.global longjmp
longjmp:
    mov 4(%esp), %ecx       // Grab jmp_buf argument
    mov 8(%esp), %eax       // Grab val argument
    test %eax, %eax
    jnz  .Lnonzero
    mov $1, %eax

.Lnonzero:
    mov (0 * 4)(%ecx), %ebx // Restore registers
    mov (1 * 4)(%ecx), %esi
    mov (2 * 4)(%ecx), %edi
    mov (3 * 4)(%ecx), %ebp
    //
    // Until this point, the stack is still from the caller.
    //
    mov (4 * 4)(%ecx), %esp
    mov (5 * 4)(%ecx), %edx
    mov %edx, (%esp)        // Patch return address
    //
    // From this point on, the former stack has been restored.
    //
    ret
