/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <bits/sighow.h>

//
// /!\ Read setjmp.h before modifying this file!
//

#define DID_SAVE_SIGNAL_MASK_SLOT (14 * 8)
#define SAVED_SIGNAL_MASK_SLOT    (14 * 8 + 4)

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/setjmp.html
// int setjmp(jmp_buf env)
// int _setjmp(jmp_buf env)
.global _setjmp
.global setjmp
_setjmp:
setjmp:
    li a1, 0                             // Set savemask argument to 0

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sigsetjmp.html
// int sigsetjmp(sigjmp_buf env, int savemask)
.global sigsetjmp
sigsetjmp:
    sw a1, DID_SAVE_SIGNAL_MASK_SLOT(a0) // Store savemask into did_save_signal_mask
    sw zero, SAVED_SIGNAL_MASK_SLOT(a0)  // Clear saved_signal_mask
    beqz a1, .Lsaveregs

    addi sp, sp, -16                     // Prepare ABI-compliant call to sigprocmask
    sd a0, 0(sp)
    sd ra, 8(sp)

    addi a2, a0, SAVED_SIGNAL_MASK_SLOT  // Set argument oldset
    li a1, 0                             // Set argument set
    li a0, 0                             // Set argument how
    call sigprocmask@plt

    ld ra, 8(sp)
    ld a0, 0(sp)
    addi sp, sp, 16

.Lsaveregs:
    sd s0, 0*8(a0)                       // Save registers
    sd s1, 1*8(a0)
    sd s2, 2*8(a0)
    sd s3, 3*8(a0)
    sd s4, 4*8(a0)
    sd s5, 5*8(a0)
    sd s6, 6*8(a0)
    sd s7, 7*8(a0)
    sd s8, 8*8(a0)
    sd s9, 9*8(a0)
    sd s10, 10*8(a0)
    sd s11, 11*8(a0)
    sd sp, 12*8(a0)
    sd ra, 13*8(a0)

    li a0, 0
    ret

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/longjmp.html
// void longjmp(jmp_buf env, int val)
// void _longjmp(jmp_buf env, int val)
.global _longjmp
.global longjmp
_longjmp:
longjmp:
    ld s0, 0*8(a0)                       // Restore registers
    ld s1, 1*8(a0)
    ld s2, 2*8(a0)
    ld s3, 3*8(a0)
    ld s4, 4*8(a0)
    ld s5, 5*8(a0)
    ld s6, 6*8(a0)
    ld s7, 7*8(a0)
    ld s8, 8*8(a0)
    ld s9, 9*8(a0)
    ld s10, 10*8(a0)
    ld s11, 11*8(a0)
    ld sp, 12*8(a0)
    ld ra, 13*8(a0)

    mv a0, a1
    bnez a0, .Lnonzero
    li a0, 1
.Lnonzero:
    ret
