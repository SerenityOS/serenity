/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <Kernel/CommandLine.h>
#include <Kernel/Panic.h>
#include <Kernel/StdLib.h>

namespace Kernel {

static char s_cmd_line[1024];
static CommandLine* s_the;

UNMAP_AFTER_INIT void CommandLine::early_initialize(const char* cmd_line)
{
    if (!cmd_line)
        return;
    size_t length = strlen(cmd_line);
    if (length >= sizeof(s_cmd_line))
        length = sizeof(s_cmd_line) - 1;
    memcpy(s_cmd_line, cmd_line, length);
    s_cmd_line[length] = '\0';
}

const CommandLine& kernel_command_line()
{
    VERIFY(s_the);
    return *s_the;
}

UNMAP_AFTER_INIT void CommandLine::initialize()
{
    VERIFY(!s_the);
    s_the = new CommandLine(s_cmd_line);
}

UNMAP_AFTER_INIT CommandLine::CommandLine(const String& string)
    : m_string(string)
{
    s_the = this;

    const auto& args = m_string.split(' ');
    m_params.ensure_capacity(args.size());
    for (auto&& str : args) {
        if (str == "") {
            continue;
        }

        auto pair = str.split_limit('=', 2);

        if (pair.size() == 1) {
            m_params.set(move(pair[0]), "");
        } else {
            m_params.set(move(pair[0]), move(pair[1]));
        }
    }
}

Optional<String> CommandLine::lookup(const String& key) const
{
    return m_params.get(key);
}

bool CommandLine::contains(const String& key) const
{
    return m_params.contains(key);
}

UNMAP_AFTER_INIT bool CommandLine::is_boot_profiling_enabled() const
{
    return contains("boot_prof");
}

UNMAP_AFTER_INIT bool CommandLine::is_ide_enabled() const
{
    return !contains("disable_ide");
}

UNMAP_AFTER_INIT bool CommandLine::is_smp_enabled() const
{
    return lookup("smp").value_or("off") == "on";
}

UNMAP_AFTER_INIT bool CommandLine::is_vmmouse_enabled() const
{
    return lookup("vmmouse").value_or("on") == "on";
}

UNMAP_AFTER_INIT bool CommandLine::is_mmio_enabled() const
{
    return lookup("pci_mmio").value_or("off") == "on";
}

UNMAP_AFTER_INIT bool CommandLine::is_legacy_time_enabled() const
{
    return lookup("time").value_or("modern") == "legacy";
}

UNMAP_AFTER_INIT bool CommandLine::is_force_pio() const
{
    return contains("force_pio");
}

UNMAP_AFTER_INIT String CommandLine::root_device() const
{
    return lookup("root").value_or("/dev/hda");
}

UNMAP_AFTER_INIT AcpiFeatureLevel CommandLine::acpi_feature_level() const
{
    auto value = kernel_command_line().lookup("acpi").value_or("on");
    if (value == "limited")
        return AcpiFeatureLevel::Limited;
    if (value == "off")
        return AcpiFeatureLevel::Disabled;
    return AcpiFeatureLevel::Enabled;
}

UNMAP_AFTER_INIT HPETMode CommandLine::hpet_mode() const
{
    auto hpet_mode = lookup("hpet").value_or("periodic");
    if (hpet_mode == "periodic")
        return HPETMode::Periodic;
    if (hpet_mode == "nonperiodic")
        return HPETMode::NonPeriodic;
    PANIC("Unknown HPETMode: {}", hpet_mode);
}

UNMAP_AFTER_INIT AHCIResetMode CommandLine::ahci_reset_mode() const
{
    const auto ahci_reset_mode = lookup("ahci_reset_mode").value_or("controller");
    if (ahci_reset_mode == "controller") {
        return AHCIResetMode::ControllerOnly;
    } else if (ahci_reset_mode == "none") {
        return AHCIResetMode::None;
    } else if (ahci_reset_mode == "complete") {
        return AHCIResetMode::Complete;
    }
    PANIC("Unknown AHCIResetMode: {}", ahci_reset_mode);
}

UNMAP_AFTER_INIT BootMode CommandLine::boot_mode() const
{
    const auto boot_mode = lookup("boot_mode").value_or("graphical");
    if (boot_mode == "text") {
        return BootMode::Text;
    } else if (boot_mode == "self-test") {
        return BootMode::SelfTest;
    } else if (boot_mode == "graphical") {
        return BootMode::Graphical;
    }
    PANIC("Unknown BootMode: {}", boot_mode);
}

UNMAP_AFTER_INIT bool CommandLine::is_text_mode() const
{
    const auto mode = boot_mode();
    return mode == BootMode::Text || mode == BootMode::SelfTest;
}

String CommandLine::userspace_init() const
{
    return lookup("init").value_or("/bin/SystemServer");
}

Vector<String> CommandLine::userspace_init_args() const
{
    auto init_args = lookup("init_args").value_or("").split(',');
    if (!init_args.is_empty())
        init_args.prepend(userspace_init());

    return init_args;
}

}
