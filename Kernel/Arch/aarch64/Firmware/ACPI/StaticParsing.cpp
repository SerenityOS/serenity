/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Firmware/ACPI/StaticParsing.h>

namespace Kernel::ACPI::StaticParsing {

ErrorOr<Optional<PhysicalAddress>> find_rsdp_in_platform_specific_memory_locations()
{
    // FIXME: Implement finding RSDP for aarch64.
    return Optional<PhysicalAddress> {};
}

}
