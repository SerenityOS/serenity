/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.global setjmp
setjmp:
    mov 4(%esp), %eax
    mov %ebx, 0(%eax)
    mov %esi, 4(%eax)
    mov %edi, 8(%eax)
    mov %ebp, 12(%eax)
    lea 4(%esp), %ecx
    mov %ecx, 16(%eax)
    mov (%esp), %ecx
    mov %ecx, 20(%eax)
    xor %eax, %eax
    ret

.global longjmp
longjmp:
    mov 4(%esp), %edx
    mov 8(%esp), %eax
    mov 0(%edx), %ebx
    mov 4(%edx), %esi
    mov 8(%edx), %edi
    mov 12(%edx), %ebp
    mov 16(%edx), %ecx
    mov %ecx, %esp
    mov 20(%edx), %ecx
    test %eax, %eax
    jnz  .nonzero
    mov 1, %eax
.nonzero:
    jmp *%ecx

