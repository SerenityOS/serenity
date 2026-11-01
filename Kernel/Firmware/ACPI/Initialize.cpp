/*
 * Copyright (c) 2020-2021, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Boot/BootInfo.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/Firmware/ACPI.h>
#endif

namespace Kernel::ACPI {

static Optional<PhysicalAddress> find_rsdp()
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

UNMAP_AFTER_INIT ErrorOr<void> early_initialize()
{
    auto feature_level = kernel_command_line().acpi_feature_level();
    if (feature_level == AcpiFeatureLevel::Disabled)
        return {};

    auto rsdp = find_rsdp();
    if (!rsdp.has_value())
        return ENOENT;

    TRY(Parser::initialize(rsdp.value()));

    return {};
}

bool is_enabled()
{
    return Parser::the();
}

}
