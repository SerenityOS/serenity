/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <Kernel/ACPI/DynamicParser.h>
#include <Kernel/CommandLine.h>

namespace Kernel {
namespace ACPI {

enum class FeatureLevel {
    Enabled,
    Limited,
    Disabled,
};

static FeatureLevel determine_feature_level()
{
    auto value = kernel_command_line().lookup("acpi").value_or("on");
    if (value == "limited")
        return FeatureLevel::Limited;
    if (value == "off")
        return FeatureLevel::Disabled;
    return FeatureLevel::Enabled;
}

void initialize()
{
    auto feature_level = determine_feature_level();
    if (feature_level == FeatureLevel::Disabled)
        return;

    auto rsdp = StaticParsing::find_rsdp();
    if (!rsdp.has_value())
        return;

    if (feature_level == FeatureLevel::Enabled)
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
