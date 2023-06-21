/*
 * Copyright (c) 2022, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// Optimized x86-64 memset routine based on the following post from the MSRC blog:
// https://msrc-blog.microsoft.com/2021/01/11/building-faster-amd64-memset-routines
//
// This algorithm
// - makes use of REP MOVSB on CPUs where it is fast (a notable exception is
//   qemu's TCG backend used in CI)
// - uses SSE stores otherwise
// - performs quick branchless stores for sizes < 64 bytes where REP STOSB would have
//   a large overhead

.intel_syntax noprefix

.global  memset_sse2_erms
.type    memset_sse2_erms, @function
.p2align 4

memset_sse2_erms:
    // Fill all bytes of esi and xmm0 with the given character.
    movzx  esi, sil
    imul   esi, 0x01010101
    movd   xmm0, esi
    pshufd xmm0, xmm0, 0

    // Store the original address for the return value.
    mov rax, rdi

    cmp rdx, 64
    jb  .Lunder_64

    // Limit taken from the article. Could be lower (256 or 512) if we want to
    // tune it for the latest CPUs.
    cmp rdx, 800
    jb  .Lbig

.Lerms:
    // We're going to align the pointer to 64 bytes, and then use REP STOSB.

    // Fill the first 64 bytes of the memory using SSE stores.
    movups [rdi], xmm0
    movups [rdi + 16], xmm0
    movups [rdi + 32], xmm0
    movups [rdi + 48], xmm0

    // Store the address of the last byte in r8.
    lea r8, [rdi + rdx]

    // Align the start pointer to 64 bytes.
    add rdi, 63
    and rdi, ~63

    // Calculate the number of remaining bytes to store.
    mov rcx, r8
    sub rcx, rdi

    // Use REP STOSB to fill the rest. This is implemented in microcode on
    // recent Intel and AMD CPUs, and can automatically use the widest stores
    // available in the CPU, so it's strictly faster than SSE for sizes of more
    // than a couple hundred bytes.
    xchg rax, rsi
    rep  stosb
    mov  rax, rsi

    ret

.global  memset_sse2
.type    memset_sse2, @function
.p2align 4

memset_sse2:
    // Fill all bytes of esi and xmm0 with the given character.
    movzx  esi, sil
    imul   rsi, 0x01010101
    movd   xmm0, esi
    pshufd xmm0, xmm0, 0

    // Store the original address for the return value.
    mov rax, rdi

    cmp rdx, 64
    jb  .Lunder_64

.Lbig:
    // We're going to align the pointer to 16 bytes, fill 4*16 bytes in a hot
    // loop, and then fill the last 48-64 bytes separately to take care of any
    // trailing bytes.

    // Fill the first 16 bytes, which might be unaligned.
    movups [rdi], xmm0

    // Calculate the first 16 byte aligned address for the SSE stores.
    lea rsi, [rdi + 16]
    and rsi, ~15

    // Calculate the number of remaining bytes.
    sub rdi, rsi
    add rdx, rdi

    // Calculate the last aligned address for trailing stores such that
    // 48-64 bytes are left.
    lea rcx, [rsi + rdx - 48]
    and rcx, ~15

    // Calculate the address 16 bytes from the end.
    lea r8, [rsi + rdx - 16]

    cmp rdx, 64
    jb  .Ltrailing

.Lbig_loop:
    // Fill 4*16 bytes in a loop.
    movaps [rsi], xmm0
    movaps [rsi + 16], xmm0
    movaps [rsi + 32], xmm0
    movaps [rsi + 48], xmm0

    add rsi, 64
    cmp rsi, rcx
    jb  .Lbig_loop

.Ltrailing:
    // We have 48-64 bytes left. Fill the first 48 and the last 16 bytes.
    movaps [rcx], xmm0
    movaps [rcx + 16], xmm0
    movaps [rcx + 32], xmm0
    movups [r8], xmm0

    ret

.Lunder_64:
    cmp rdx, 16
    jb  .Lunder_16

    // We're going to fill 16-63 bytes using variable sized branchess stores.
    // Although this means that we might set the same byte up to 4 times, we
    // can avoid branching which is expensive compared to straight-line code.

    // Calculate the address of the last SSE store.
    lea r8, [rdi + rdx - 16]

    // Set rdx to 32 if there are >= 32 bytes, otherwise let its value be 0.
    and rdx, 32

    // Fill the first 16 bytes.
    movups [rdi], xmm0

    // Set rdx to 16 if there are >= 32 bytes, otherwise let its value be 0.
    shr rdx, 1

    // Fill the last 16 bytes.
    movups [r8], xmm0

    // Fill bytes 16 - 32 if there are more than 32 bytes, otherwise fill the first 16 again.
    movups [rdi + rdx], xmm0

    // Fill bytes (n-32) - (n-16) if there are n >= 32 bytes, otherwise fill the last 16 again.
    neg    rdx
    movups [r8 + rdx], xmm0

    ret

.Lunder_16:
    cmp rdx, 4
    jb  .Lunder_4

    // We're going to fill 4-15 bytes using variable sized branchless stores like above.
    lea r8, [rdi + rdx - 4]
    and rdx, 8
    mov [rdi], esi
    shr rdx, 1
    mov [r8], esi
    mov [rdi + rdx], esi
    neg rdx
    mov [r8 + rdx], esi
    ret

.Lunder_4:
    cmp rdx, 1
    jb  .Lend

    // Fill the first byte.
    mov [rdi], sil

    jbe .Lend

    // The size is 2 or 3 bytes. Fill the second and the last one.
    mov [rdi + 1], sil
    mov [rdi + rdx - 1], sil

.Lend:
    ret
