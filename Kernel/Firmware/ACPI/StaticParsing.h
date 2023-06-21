/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Optional.h>
#include <AK/Types.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel::ACPI::StaticParsing {

ErrorOr<Optional<PhysicalAddress>> find_table(PhysicalAddress rsdp, StringView signature);
ErrorOr<Optional<PhysicalAddress>> search_table_in_xsdt(PhysicalAddress xsdt, StringView signature);
ErrorOr<Optional<PhysicalAddress>> search_table_in_rsdt(PhysicalAddress rsdt, StringView signature);

// NOTE: This function is implemented for each CPU architecture in a subdirectory
// under the Kernel/Arch directory.
ErrorOr<Optional<PhysicalAddress>> find_rsdp_in_platform_specific_memory_locations();

}
