/*
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibACPI/Definitions.h>

namespace Kernel::ACPI {

::ACPI::Structures::GenericAddressStructure make_generic_address_from_io_port(u32 io_port);

ErrorOr<u64> read_generic_address(::ACPI::Structures::GenericAddressStructure const& structure);
ErrorOr<void> write_generic_address(::ACPI::Structures::GenericAddressStructure const& structure, u64 value);

}
