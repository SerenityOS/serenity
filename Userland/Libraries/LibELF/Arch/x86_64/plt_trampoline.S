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
    subq $128, %rsp
    movdqu %xmm0, 0x0(%rsp)
    movdqu %xmm1, 0x10(%rsp)
    movdqu %xmm2, 0x20(%rsp)
    movdqu %xmm3, 0x30(%rsp)
    movdqu %xmm4, 0x40(%rsp)
    movdqu %xmm5, 0x50(%rsp)
    movdqu %xmm6, 0x60(%rsp)
    movdqu %xmm7, 0x70(%rsp)
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

    movq 208(%rsp), %rdi # object
    movq 216(%rsp), %rsi # relocation_index

    # offset = index * sizeof(Elf64_Rela)
    shlq $3, %rsi
    leaq (%rsi, %rsi, 2), %rsi

    pushq %rbp
    movq %rsp, %rbp
    andq $~15, %rsp
    call _fixup_plt_entry@PLT
    movq %rbp, %rsp
    popq %rbp

    movq %rax, 216(%rsp) # replace object argument with symbol address

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

    movdqu 0x0(%rsp), %xmm0
    movdqu 0x10(%rsp), %xmm1
    movdqu 0x20(%rsp), %xmm2
    movdqu 0x30(%rsp), %xmm3
    movdqu 0x40(%rsp), %xmm4
    movdqu 0x50(%rsp), %xmm5
    movdqu 0x60(%rsp), %xmm6
    movdqu 0x70(%rsp), %xmm7
    addq $128, %rsp
    addq $8, %rsp # remove relocation_index argument

    retq
