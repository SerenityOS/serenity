/*
 * Copyright (c) 2020-2021, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Boot/BootInfo.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Firmware/ACPI/StaticParsing.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

namespace Kernel::ACPI {

UNMAP_AFTER_INIT ErrorOr<void> initialize()
{
    auto feature_level = kernel_command_line().acpi_feature_level();
    if (feature_level == AcpiFeatureLevel::Disabled)
        return {};

    auto rsdp = StaticParsing::find_rsdp();
    if (!rsdp.has_value())
        return ENOENT;

    auto facp = TRY(StaticParsing::find_table(rsdp.value(), "FACP"sv));
    if (!facp.has_value())
        return ENOENT;

    auto facp_table = TRY(Memory::map_typed<::ACPI::Structures::FADT>(facp.value()));
    InterruptNumber irq_line = facp_table->sci_int;

    TRY(Parser::initialize(rsdp.value(), facp.value(), irq_line));

    if (kernel_command_line().acpi_feature_level() == AcpiFeatureLevel::Enabled)
        Parser::the()->enable_aml_parsing();

    return {};
}

bool is_enabled()
{
    return Parser::the();
}

}
