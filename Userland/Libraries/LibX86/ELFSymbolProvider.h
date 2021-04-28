/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibELF/Image.h>
#include <LibX86/Instruction.h>

namespace X86 {

class ELFSymbolProvider final : public SymbolProvider {
public:
    ELFSymbolProvider(const ELF::Image& elf)
        : m_elf(elf)
    {
    }

    virtual String symbolicate(FlatPtr address, u32* offset = nullptr) const override
    {
        return m_elf.symbolicate(address, offset);
    }

private:
    const ELF::Image& m_elf;
};
}
