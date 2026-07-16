/*
 * Copyright (c) 2026, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// Privileged Access Never (PAN) is a feature of ARMv8.1-A.
// https://developer.arm.com/documentation/ddi0595/2021-03/AArch64-Registers/PAN--Privileged-Access-Never
// The default compile architecture we use is ARMv8-A, which does not include this extension.
.arch_extension pan

.global asm_clear_pan
.type asm_clear_pan, @function
asm_clear_pan:
    msr pan, #0
    ret

.global asm_set_pan
.type asm_set_pan, @function
asm_set_pan:
    msr pan, #1
    ret

.global asm_is_pan_set
.type asm_is_pan_set, @function
asm_is_pan_set:
    mrs x0, pan
    ubfx x0, x0, #22, #1
    ret
