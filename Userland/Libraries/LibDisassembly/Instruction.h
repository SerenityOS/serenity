/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Types.h>
#include <LibDisassembly/SymbolProvider.h>

namespace Disassembly {

class Instruction {
public:
    virtual ~Instruction() = default;

    virtual ByteString to_byte_string(u32 origin, Optional<SymbolProvider const&> = {}) const = 0;
    virtual ByteString mnemonic() const = 0;
    virtual size_t length() const = 0;
};

}
