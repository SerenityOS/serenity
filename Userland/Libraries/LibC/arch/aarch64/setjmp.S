/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

//
// /!\ Read setjmp.h before modifying this file!
//

#define DID_SAVE_SIGNAL_MASK_SLOT (21 * 8)
#define SAVED_SIGNAL_MASK_SLOT    (21 * 8 + 4)

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/setjmp.html
// int setjmp(jmp_buf env)
// int _setjmp(jmp_buf env)
.global _setjmp
.global setjmp
.type _setjmp, @function
.type setjmp, @function
_setjmp:
setjmp:
    mov x1, #0                               // Set savemask argument to 0

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sigsetjmp.html
// int sigsetjmp(sigjmp_buf env, int savemask)
.global sigsetjmp
.type sigsetjmp, @function
sigsetjmp:
    str w1, [x0, #DID_SAVE_SIGNAL_MASK_SLOT] // Store savemask into did_save_signal_mask
    str wzr, [x0, #SAVED_SIGNAL_MASK_SLOT]   // Clear saved_signal_mask
    cbz w1, .Lsaveregs

    stp x0, lr, [sp, #-16]!                  // Prepare ABI-compliant call to sigprocmask

    add x2, x0, #SAVED_SIGNAL_MASK_SLOT      // Set argument oldset
    mov x1, #0                               // Set argument set
    mov x0, #0                               // Set argument how
    bl sigprocmask

    ldp x0, lr, [sp], #16

.Lsaveregs:
    stp x19, x20, [x0, #(0 * 8)]             // Save registers
    stp x21, x22, [x0, #(2 * 8)]
    stp x23, x24, [x0, #(4 * 8)]
    stp x25, x26, [x0, #(6 * 8)]
    stp x27, x28, [x0, #(8 * 8)]
    stp x29, x30, [x0, #(10 * 8)]

    stp d8, d9, [x0, #(12 * 8)]
    stp d10, d11, [x0, #(14 * 8)]
    stp d12, d13, [x0, #(16 * 8)]
    stp d14, d15, [x0, #(18 * 8)]

    mov x1, sp
    str x1, [x0, #(20 * 8)]

    mov x0, #0
    ret

.global _longjmp
.global longjmp
.type _longjmp, @function
.type longjmp, @function
_longjmp:
longjmp:
    ldp x19, x20, [x0, #(0 * 8)]             // Restore registers
    ldp x21, x22, [x0, #(2 * 8)]
    ldp x23, x24, [x0, #(4 * 8)]
    ldp x25, x26, [x0, #(6 * 8)]
    ldp x27, x28, [x0, #(8 * 8)]
    ldp x29, x30, [x0, #(10 * 8)]

    ldp d8, d9, [x0, #(12 * 8)]
    ldp d10, d11, [x0, #(14 * 8)]
    ldp d12, d13, [x0, #(16 * 8)]
    ldp d14, d15, [x0, #(18 * 8)]

    ldr x2, [x0, #(20 * 8)]
    mov sp, x2

    mov x0, x1
    cbnz x0, .Lnonzero
    mov x0, #1
.Lnonzero:
    ret
