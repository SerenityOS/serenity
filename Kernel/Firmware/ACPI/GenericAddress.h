/*
 * Copyright (c) 2026, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibACPI/Definitions.h>

namespace Kernel::ACPI {

ErrorOr<void> write_generic_address(::ACPI::Structures::GenericAddressStructure const& structure, u64 value);

}
