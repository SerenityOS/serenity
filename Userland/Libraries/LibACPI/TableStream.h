/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Definitions.h"
#include "NameString.h"
#include <AK/Array.h>
#include <AK/Forward.h>
#include <AK/MemoryStream.h>
#include <AK/StringView.h>
#include <LibACPI/Definitions.h>

namespace ACPI::AML {

class TableStream : public FixedMemoryStream {
public:
    explicit TableStream(ReadonlyBytes bytes)
        : FixedMemoryStream(bytes)
    {
    }

    ErrorOr<Structures::SDTHeader> read_sdt_header();

    ErrorOr<Opcode> read_opcode();
    ErrorOr<u32> read_package_length();
    ErrorOr<NameString> read_name_string();

    ErrorOr<u8> peek_byte();
    ErrorOr<Opcode> peek_opcode();
};

}
