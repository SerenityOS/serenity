/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <Kernel/Firmware/ACPI/StaticParsing.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Memory/TypedMapping.h>
#include <LibACPI/Definitions.h>

#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/Firmware/ACPI.h>
#endif

namespace Kernel::ACPI::StaticParsing {

Optional<PhysicalAddress> find_rsdp()
{
    Optional<PhysicalAddress> rsdp;
    if (!g_boot_info.acpi_rsdp_paddr.is_null())
        rsdp = g_boot_info.acpi_rsdp_paddr;

#if ARCH(X86_64)
    if (!rsdp.has_value() && g_boot_info.boot_method == BootMethod::Multiboot1)
        rsdp = StaticParsing::find_rsdp_in_ia_pc_specific_memory_locations();
#endif

    return rsdp;
}

}
