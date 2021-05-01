/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.global setjmp
setjmp:
    mov %rbx, (%rdi)
    mov %rbp, 8(%rdi)
    mov %r12, 16(%rdi)
    mov %r13, 24(%rdi)
    mov %r14, 32(%rdi)
    mov %r15, 40(%rdi)
    lea 8(%rsp), %rcx
    mov %rcx, 48(%rdi)
    mov (%rsp), %rcx
    mov %rcx, 56(%rdi)
    xor %eax, %eax
    ret

.global longjmp
longjmp:
    mov (%rdi), %rbx
    mov 8(%rdi), %rbp
    mov 16(%rdi), %r12
    mov 24(%rdi), %r13
    mov 32(%rdi), %r14
    mov 40(%rdi), %r15
    mov 48(%rdi), %rsp

    test %rsi, %rsi
    jnz .nonzero
    mov 1, %rax
.nonzero:
    jmp *56(%rdi)

