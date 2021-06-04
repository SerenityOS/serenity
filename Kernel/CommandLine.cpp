/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Panic.h>
#include <Kernel/StdLib.h>

namespace Kernel {

static char s_cmd_line[1024];
static constexpr StringView s_embedded_cmd_line = "";
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
    dmesgln("Kernel Commandline: {}", kernel_command_line().string());
}

UNMAP_AFTER_INIT void CommandLine::build_commandline(const String& cmdline_from_bootloader)
{
    StringBuilder builder;
    builder.append(cmdline_from_bootloader);
    if (!s_embedded_cmd_line.is_empty()) {
        builder.append(" ");
        builder.append(s_embedded_cmd_line);
    }
    m_string = builder.to_string();
}

UNMAP_AFTER_INIT void CommandLine::add_arguments(const Vector<StringView>& args)
{
    for (auto&& str : args) {
        if (str == ""sv) {
            continue;
        }

        auto pair = str.split_view('=', false);
        VERIFY(pair.size() == 2 || pair.size() == 1);

        if (pair.size() == 1) {
            m_params.set(move(pair[0]), ""sv);
        } else {
            m_params.set(move(pair[0]), move(pair[1]));
        }
    }
}

UNMAP_AFTER_INIT CommandLine::CommandLine(const String& cmdline_from_bootloader)
{
    s_the = this;
    build_commandline(cmdline_from_bootloader);
    const auto& args = m_string.split_view(' ');
    m_params.ensure_capacity(args.size());
    add_arguments(args);
}

Optional<String> CommandLine::lookup(const StringView& key) const
{
    return m_params.get(key);
}

bool CommandLine::contains(const StringView& key) const
{
    return m_params.contains(key);
}

UNMAP_AFTER_INIT bool CommandLine::is_boot_profiling_enabled() const
{
    return contains("boot_prof"sv);
}

UNMAP_AFTER_INIT bool CommandLine::is_ide_enabled() const
{
    return !contains("disable_ide"sv);
}

UNMAP_AFTER_INIT bool CommandLine::is_smp_enabled() const
{
    return lookup("smp"sv).value_or("off"sv) == "on"sv;
}

UNMAP_AFTER_INIT bool CommandLine::is_vmmouse_enabled() const
{
    return lookup("vmmouse"sv).value_or("on") == "on"sv;
}

UNMAP_AFTER_INIT PCIAccessLevel CommandLine::pci_access_level() const
{
    auto value = lookup("pci_ecam"sv).value_or("off"sv);
    if (value == "on"sv)
        return PCIAccessLevel::MappingPerBus;
    if (value == "per-device"sv)
        return PCIAccessLevel::MappingPerDevice;
    if (value == "off"sv)
        return PCIAccessLevel::IOAddressing;
    PANIC("Unknown PCI ECAM setting: {}", value);
}

UNMAP_AFTER_INIT bool CommandLine::is_legacy_time_enabled() const
{
    return lookup("time"sv).value_or("modern"sv) == "legacy"sv;
}

UNMAP_AFTER_INIT bool CommandLine::is_force_pio() const
{
    return contains("force_pio"sv);
}

UNMAP_AFTER_INIT String CommandLine::root_device() const
{
    return lookup("root"sv).value_or("/dev/hda"sv);
}

UNMAP_AFTER_INIT AcpiFeatureLevel CommandLine::acpi_feature_level() const
{
    auto value = kernel_command_line().lookup("acpi"sv).value_or("on"sv);
    if (value == "limited"sv)
        return AcpiFeatureLevel::Limited;
    if (value == "off"sv)
        return AcpiFeatureLevel::Disabled;
    return AcpiFeatureLevel::Enabled;
}

UNMAP_AFTER_INIT HPETMode CommandLine::hpet_mode() const
{
    auto hpet_mode = lookup("hpet"sv).value_or("periodic"sv);
    if (hpet_mode == "periodic"sv)
        return HPETMode::Periodic;
    if (hpet_mode == "nonperiodic"sv)
        return HPETMode::NonPeriodic;
    PANIC("Unknown HPETMode: {}", hpet_mode);
}

UNMAP_AFTER_INIT bool CommandLine::disable_ps2_controller() const
{
    return contains("disable_ps2_controller"sv);
}

UNMAP_AFTER_INIT bool CommandLine::disable_physical_storage() const
{
    return contains("disable_physical_storage"sv);
}

UNMAP_AFTER_INIT bool CommandLine::disable_uhci_controller() const
{
    return contains("disable_uhci_controller"sv);
}

UNMAP_AFTER_INIT bool CommandLine::disable_virtio() const
{
    return contains("disable_virtio"sv);
}

UNMAP_AFTER_INIT AHCIResetMode CommandLine::ahci_reset_mode() const
{
    const auto ahci_reset_mode = lookup("ahci_reset_mode"sv).value_or("controllers"sv);
    if (ahci_reset_mode == "controllers"sv) {
        return AHCIResetMode::ControllerOnly;
    } else if (ahci_reset_mode == "aggressive"sv) {
        return AHCIResetMode::Aggressive;
    }
    PANIC("Unknown AHCIResetMode: {}", ahci_reset_mode);
}

UNMAP_AFTER_INIT BootMode CommandLine::boot_mode() const
{
    const auto boot_mode = lookup("boot_mode"sv).value_or("graphical"sv);
    if (boot_mode == "no-fbdev"sv) {
        return BootMode::NoFramebufferDevices;
    } else if (boot_mode == "self-test"sv) {
        return BootMode::SelfTest;
    } else if (boot_mode == "graphical"sv) {
        return BootMode::Graphical;
    }
    PANIC("Unknown BootMode: {}", boot_mode);
}

UNMAP_AFTER_INIT bool CommandLine::is_no_framebuffer_devices_mode() const
{
    const auto mode = boot_mode();
    return mode == BootMode::NoFramebufferDevices || mode == BootMode::SelfTest;
}

String CommandLine::userspace_init() const
{
    return lookup("init"sv).value_or("/bin/SystemServer"sv);
}

Vector<String> CommandLine::userspace_init_args() const
{
    auto init_args = lookup("init_args"sv).value_or(""sv).split(',');
    if (!init_args.is_empty())
        init_args.prepend(userspace_init());

    return init_args;
}

UNMAP_AFTER_INIT size_t CommandLine::switch_to_tty() const
{
    const auto default_tty = lookup("switch_to_tty"sv).value_or("1"sv);
    auto switch_tty_number = default_tty.to_uint();
    if (switch_tty_number.has_value() && switch_tty_number.value() >= 1) {
        return switch_tty_number.value() - 1;
    }
    PANIC("Invalid default tty value: {}", default_tty);
}
}
