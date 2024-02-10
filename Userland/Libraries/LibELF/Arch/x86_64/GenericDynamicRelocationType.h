/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Userland/Libraries/LibELF/ELFABI.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace ELF {

enum class GenericDynamicRelocationType : unsigned {
    NONE = R_X86_64_NONE,
    ABSOLUTE = R_X86_64_64,
    COPY = R_X86_64_COPY,
    GLOB_DAT = R_X86_64_GLOB_DAT,
    JUMP_SLOT = R_X86_64_JUMP_SLOT,
    RELATIVE = R_X86_64_RELATIVE,
    TLS_DTPMOD = R_X86_64_DTPMOD64,
    TLS_DTPREL = R_X86_64_DTPOFF64,
    TLS_TPREL = R_X86_64_TPOFF64,
    TLSDESC = R_X86_64_TLSDESC,
    IRELATIVE = R_X86_64_IRELATIVE,
};

}
