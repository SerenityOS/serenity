/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Userland/Libraries/LibELF/ELFABI.h>

#include <AK/Platform.h>
VALIDATE_IS_AARCH64()

namespace ELF {

enum class GenericDynamicRelocationType : unsigned {
    NONE = R_AARCH64_NONE,
    ABSOLUTE = R_AARCH64_ABS64,
    COPY = R_AARCH64_COPY,
    GLOB_DAT = R_AARCH64_GLOB_DAT,
    JUMP_SLOT = R_AARCH64_JUMP_SLOT,
    RELATIVE = R_AARCH64_RELATIVE,
    TLS_DTPMOD = R_AARCH64_TLS_DTPMOD,
    TLS_DTPREL = R_AARCH64_TLS_DTPREL,
    TLS_TPREL = R_AARCH64_TLS_TPREL,
    TLSDESC = R_AARCH64_TLSDESC,
    IRELATIVE = R_AARCH64_IRELATIVE,
};

}
