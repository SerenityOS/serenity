/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/ACPI/Bytecode/Package.h>

#define FIRST_6_BITS 0x3F
#define FIRST_4_BITS 0x0F

namespace Kernel::ACPI::Package {

/*
Package::DecodingResult parse_encoded_name_path_length(u8 first_byte, Vector<u8> other_bytes)
{
    if (first_byte == 0)
        return { 0, 1 };
    if (first_byte == 0x2E)
        return { 8, 1 };
    if (first_byte == 0x2F) {
        VERIFY(!other_bytes.is_empty());
        return { static_cast<size_t>(4 * other_bytes[0]), 2 };
    }

    size_t simple_name_string_size = 1;
    VERIFY(other_bytes.size() >= 3);
    for (size_t index = 0; index < 3; index++) {
        if (other_bytes[index] == 0)
            break;
        simple_name_string_size++;
    }
    return { simple_name_string_size, 0 };
}
*/

DecodingResult parse_encoded_package_length(u8 first_byte, Vector<u8> other_bytes)
{
    if (!(first_byte & (1 << 7) || first_byte & (1 << 6))) {
        return { static_cast<size_t>(first_byte & FIRST_6_BITS), 1 };
    }
    VERIFY(other_bytes.size() > 1);
    size_t size = first_byte & FIRST_4_BITS;
    size_t encoding_length = (first_byte >> 6) + 1;
    size_t shift_index = 1;
    size_t bytes_count = min(other_bytes.size(), first_byte >> 6);
    for (size_t index = 0; index < bytes_count; index++) {
        size += other_bytes[index] << (4 + (shift_index - 1) * 8);
        shift_index++;
    }
    return { size, encoding_length };
}
}
