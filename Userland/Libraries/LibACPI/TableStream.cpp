/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TableStream.h"
#include "Definitions.h"

namespace ACPI::AML {

ErrorOr<Structures::SDTHeader> TableStream::read_sdt_header()
{
    // DefBlockHeader := TableSignature TableLength
    //                   SpecCompliance CheckSum
    //                   OemID OemTableID OemRevision
    //                   CreatorID CreatorRevision
    // TableSignature   := DWordData
    // TableLength      := DWordData
    // SpecCompliance   := ByteData
    // CheckSum         := ByteData
    // OemID            := ByteData(6)::ZeroFilled
    // OemTableID       := ByteData(8)::ZeroFilled
    // OemRevision      := DWordData
    // CreatorID        := DWordData
    // CreatorRevision  := DWordData
    return *TRY(read_in_place<Structures::SDTHeader const>());
}

ErrorOr<Opcode> TableStream::read_opcode()
{
    u16 opcode = TRY(read_value<u8>());
    if (opcode == static_cast<u16>(Prefix::ExtOpPrefix))
        opcode = (opcode << 8) | TRY(read_value<u8>());

    return static_cast<Opcode>(opcode);
}

ErrorOr<u32> TableStream::read_package_length()
{
    u8 initial = TRY(read_value<u8>());
    if ((initial & 0b11000000) == 0) {
        return initial;
    }

    i64 value = (initial & 0b1111);
    u8 additional_byte_count = initial >> 6;
    int bits = 4;
    for (auto i = 0; i < additional_byte_count; i++) {
        value = value | (TRY(read_value<u8>()) << bits);
        bits += 8;
    }

    return value;
}

ErrorOr<NameString> TableStream::read_name_string()
{
    auto value = TRY(NameString::from_table_stream(*this));
    return value;
}

ErrorOr<u8> TableStream::peek_byte()
{
    u8 value = TRY(read_value<u8>());
    TRY(seek(offset() - 1));
    return value;
}

ErrorOr<Opcode> TableStream::peek_opcode()
{
    auto previous = offset();
    Opcode value = TRY(read_opcode());
    TRY(seek(previous));
    return value;
}

}
