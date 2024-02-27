/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <stdint.h>
#include <sys/cdefs.h>

#if !defined(__riscv) || __riscv_xlen != 64
#    error "This file should not be included on architectures other than riscv64."
#endif

__BEGIN_DECLS

// Chapter numbers from RISC-V Unprivileged ISA V20191213
// RISC-V F extension version 2.2, Figure 11.1
typedef struct fenv_t {
    union {
        // 11.2: fcsr is always 32 bits, even for the D and Q extensions, since only the lowest byte of data is in use.
        uint32_t fcsr;
        struct {
            // Accrued exceptions (fflags).
            uint8_t inexact : 1;           // NX
            uint8_t underflow : 1;         // UF
            uint8_t overflow : 1;          // OF
            uint8_t divide_by_zero : 1;    // DZ
            uint8_t invalid_operation : 1; // NV
            uint8_t rounding_mode : 3;     // frm
            uint32_t reserved : 24;
        };
    };
} fenv_t;

__END_DECLS
