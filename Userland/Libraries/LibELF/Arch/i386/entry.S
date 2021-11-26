/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.align 4
.globl _invoke_entry
.hidden _invoke_entry
.type _invoke_entry,@function
_invoke_entry: # (argc, argv, envp, entry)
    addl $4, %esp # return address
    popl %edi # argc
    popl %esi # argv
    popl %edx # envp
    popl %ecx # entry

    // The System V ABI for x86 and x86_64 prescribes that the stack pointer is 16-byte aligned
    andl $~15, %esp

    // We're going to push three arguments so we need to align the stack for that
    subl $4, %esp

    // FIXME: The way we're setting up the stack and passing arguments to the entry point isn't ABI-compliant
    pushl %edx
    pushl %esi
    pushl %edi
    jmp *%ecx
