/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Userland/Libraries/LibELF/ELFABI.h>

#include <AK/Platform.h>
VALIDATE_IS_RISCV64()

namespace ELF {

enum class GenericDynamicRelocationType : unsigned {
    NONE = R_RISCV_NONE,
    ABSOLUTE = R_RISCV_64,
    COPY = R_RISCV_COPY,
    // there is no R_RISCV_GLOB_DAT
    JUMP_SLOT = R_RISCV_JUMP_SLOT,
    RELATIVE = R_RISCV_RELATIVE,
    TLS_DTPMOD = R_RISCV_TLS_DTPMOD64,
    TLS_DTPREL = R_RISCV_TLS_DTPREL64,
    TLS_TPREL = R_RISCV_TLS_TPREL64,
    TLSDESC = R_RISCV_TLSDESC,
    IRELATIVE = R_RISCV_IRELATIVE,
};

}
