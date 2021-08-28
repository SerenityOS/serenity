/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.align 4
.globl _plt_trampoline
.hidden _plt_trampoline
.type _plt_trampoline,@function
_plt_trampoline: # (object, relocation_index)
     # save flags/registers (https://stackoverflow.com/questions/18024672/what-registers-are-preserved-through-a-linux-x86-64-function-call)
    pushfq
    pushq %rax
    pushq %rcx
    pushq %rdx
    pushq %rsi
    pushq %rdi
    pushq %r8
    pushq %r9
    pushq %r10
    pushq %r11

    movq 80(%rsp), %rdi # object
    movq 88(%rsp), %rsi # relocation_index

    # offset = index * sizeof(Elf64_Rela)
    shlq $3, %rsi
    leaq (%rsi, %rsi, 2), %rsi

    pushq %rbp
    movq %rsp, %rbp
    andq $~15, %rsp
    call _fixup_plt_entry@PLT
    movq %rbp, %rsp
    popq %rbp

    movq %rax, 88(%rsp) # replace object argument with symbol address

     # restore flags/registers
    popq %r11
    popq %r10
    popq %r9
    popq %r8
    popq %rdi
    popq %rsi
    popq %rdx
    popq %rcx
    popq %rax
    popfq

    addq $8, %rsp # remove relocation_index argument

    retq
