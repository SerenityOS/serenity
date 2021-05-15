/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/ACPI/DynamicParser.h>
#include <Kernel/CommandLine.h>

namespace Kernel {
namespace ACPI {

UNMAP_AFTER_INIT void initialize()
{
    auto feature_level = kernel_command_line().acpi_feature_level();
    if (feature_level == AcpiFeatureLevel::Disabled)
        return;

    auto rsdp = StaticParsing::find_rsdp();
    if (!rsdp.has_value())
        return;

    if (feature_level == AcpiFeatureLevel::Enabled)
        Parser::initialize<DynamicParser>(rsdp.value());
    else
        Parser::initialize<Parser>(rsdp.value());
}

bool is_enabled()
{
    return Parser::the();
}

}
}
