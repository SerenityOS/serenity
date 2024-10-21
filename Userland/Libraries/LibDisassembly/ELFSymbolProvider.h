/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibDisassembly/SymbolProvider.h>
#include <LibDisassembly/x86/Instruction.h>
#include <LibELF/Image.h>

namespace Disassembly {

class ELFSymbolProvider final : public SymbolProvider {
public:
    ELFSymbolProvider(const ELF::Image& elf, FlatPtr base_address = 0)
        : m_elf(elf)
        , m_base_address(base_address)
    {
    }

    virtual ByteString symbolicate(FlatPtr address, u32* offset = nullptr) const override
    {
        return m_elf.symbolicate(address - m_base_address, offset);
    }

private:
    const ELF::Image& m_elf;
    FlatPtr m_base_address;
};
}
