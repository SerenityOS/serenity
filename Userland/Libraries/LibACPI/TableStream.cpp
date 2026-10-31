/*
 * Copyright (c) 2025, RatcheT2497 <ratchetnumbers@proton.me>
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibACPI/OpcodeFilters.h>
#include <LibACPI/TableStream.h>

#ifdef KERNEL
#    define warnln dbgln
#endif

namespace ACPI::AML {

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#defblockheader
ErrorOr<Structures::SDTHeader> TableStream::read_sdt_header()
{
    return *TRY(read_in_place<Structures::SDTHeader const>());
}

ErrorOr<Opcode> TableStream::read_opcode()
{
    u16 opcode = TRY(read_value<u8>());
    if (opcode == EXT_OP_PREFIX)
        opcode = (opcode << 8) | TRY(read_value<u8>());

    return static_cast<Opcode>(opcode);
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#package-length-encoding
ErrorOr<u32> TableStream::read_package_length()
{
    u8 initial = TRY(read_value<u8>());
    if ((initial & 0b1100'0000) == 0) {
        return initial;
    }

    u8 additional_byte_count = initial >> 6;

    u32 value = (initial & 0b1111);
    int bits = 4;
    for (auto i = 0; i < additional_byte_count; i++) {
        value |= TRY(read_value<u8>()) << bits;
        bits += 8;
    }

    return value;
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#stringprefix
ErrorOr<ByteBuffer> TableStream::read_string()
{
    size_t start = offset();

    u8 b = TRY(read_value<u8>());
    if (b == 0)
        return ByteBuffer::create_uninitialized(0);

    do {
        if (b > 0x7f) {
            warnln("Invalid character in string: {} at offset {}", b, offset());
            return Error::from_string_view_or_print_error_and_return_errno("Invalid character in string!"sv, EINVAL);
        }

        b = TRY(read_value<u8>());
    } while (b != 0);

    size_t size = offset() - start;
    TRY(seek(start));

    auto span = TRY(read_in_place<u8 const>(size - 1));
    // Skip over null byte so it doesn't end up in the buffer.
    TRY(seek(1, SeekMode::FromCurrentPosition));
    return ByteBuffer::copy(span);
}

// https://uefi.org/specs/ACPI/6.6/20_AML_Specification.html#namestring
ErrorOr<NameString> TableStream::read_name_string()
{
    NameString::Type type = NameString::Type::Relative;
    size_t depth = 0;
    size_t count = 0;
    u8 initial = 0;

    // Figure out path type.
    initial = TRY(read_value<u8>());
    if (initial == '\\') {
        type = NameString::Type::Absolute;
    } else if (initial == '^') {
        while (initial == '^') {
            depth++;
            if (is_eof())
                return Error::from_string_view_or_print_error_and_return_errno("Unexpected EOF in NameString."sv, EINVAL);
            initial = TRY(read_value<u8>());
        }

        TRY(seek(offset() - 1));
    } else {
        TRY(seek(offset() - 1));
    }

    // Figure out how many segments are in the path.
    // NamePath := NameSeg | DualNamePath | MultiNamePath | NullName
    initial = TRY(read_value<u8>());
    switch (initial) {
    case 0:
        return NameString(type, depth);

    case MULTI_NAME_PREFIX:
        count = TRY(read_value<u8>());

        if (count == 0) {
            return Error::from_string_view_or_print_error_and_return_errno("Multiname path must have at least 1 item."sv, EINVAL);
        }
        break;

    case DUAL_NAME_PREFIX:
        count = 2;
        break;

    default:
        if (is_lead_name_char(initial)) {
            TRY(seek(offset() - 1));
            count = 1;
            break;
        }

        warnln("ACPI: Invalid character: '{:X}' at offset {:X}", (char)initial, offset());
        return Error::from_string_view_or_print_error_and_return_errno("Invalid character found."sv, EINVAL);
    }

    Vector<NameSegment, 4> segments;
    for (size_t i = 0; i < count; i++) {
        auto item = TRY(read_in_place<u8 const>(4));
        TRY(segments.try_append(TRY(NameSegment::from_bytes(item))));
    }

    return NameString(type, depth, move(segments));
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
