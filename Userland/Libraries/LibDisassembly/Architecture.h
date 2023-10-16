/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/Platform.h>
#include <LibELF/ELFABI.h>

namespace Disassembly {

enum class Architecture {
    Aarch64,
    RISCV64,
    Wasm32,
    X86,
};

constexpr Architecture host_architecture()
{
#if ARCH(AARCH64)
    return Architecture::Aarch64;
#elif ARCH(RISCV64)
    return Architecture::RISCV64;
#elif ARCH(WASM32)
    return Architecture::Wasm32;
#elif ARCH(X86_64)
    return Architecture::X86;
#else
#    error "Unknown architecture"
#endif
}

ALWAYS_INLINE Optional<Architecture> architecture_from_elf_machine(Elf64_Quarter e_machine)
{
    switch (e_machine) {
    case EM_AARCH64:
        return Architecture::Aarch64;
    case EM_RISCV:
        return Architecture::RISCV64;
    case EM_X86_64:
        return Architecture::X86;
    default:
        return {};
    }
}

}
