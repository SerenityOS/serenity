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
    // The System V ABI for x86 and x86_64 prescribes that the stack pointer is 16-byte aligned
    andq $~15, %rsp

    // FIXME: The way we're setting up the stack and passing arguments to the entry point isn't ABI-compliant
    jmp *%rcx
