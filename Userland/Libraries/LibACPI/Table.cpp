/*
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Table.h"
#include <AK/BitCast.h>

namespace ACPI {

bool validate_table(::ACPI::Structures::SDTHeader const& header)
{
    auto const* sdt = reinterpret_cast<u8 const*>(&header);

    u8 checksum = 0;
    for (size_t i = 0; i < header.length; i++)
        checksum += sdt[i];

    return checksum == 0;
}

}
