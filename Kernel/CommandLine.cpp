/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Panic.h>
#include <Kernel/Sections.h>
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

bool CommandLine::was_initialized()
{
    return s_the != nullptr;
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
    // Validate the modes the user passed in.
    (void)s_the->panic_mode(Validate::Yes);
    if (s_the->contains("boot_mode"sv)) {
        // I know, we don't do legacy, but even though I eliminated 'boot_mode' from the codebase, there
        // is a good chance that someone's still using it. Let's be nice and tell them where to look.
        // TODO: Remove this in 2022.
        PANIC("'boot_mode' is now split into panic=[halt|shutdown], fbdev=[on|off], and system_mode=[graphical|text|selftest].");
    }
}

UNMAP_AFTER_INIT NonnullOwnPtr<KString> CommandLine::build_commandline(StringView cmdline_from_bootloader)
{
    StringBuilder builder;
    builder.append(cmdline_from_bootloader);
    if constexpr (!s_embedded_cmd_line.is_empty()) {
        builder.append(" ");
        builder.append(s_embedded_cmd_line);
    }
    return KString::must_create(builder.string_view());
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
            m_params.set(pair[0], ""sv);
        } else {
            m_params.set(pair[0], pair[1]);
        }
    }
}

UNMAP_AFTER_INIT CommandLine::CommandLine(StringView cmdline_from_bootloader)
    : m_string(build_commandline(cmdline_from_bootloader))
{
    s_the = this;
    const auto& args = m_string->view().split_view(' ');
    m_params.ensure_capacity(args.size());
    add_arguments(args);
}

Optional<StringView> CommandLine::lookup(StringView key) const
{
    return m_params.get(key);
}

bool CommandLine::contains(StringView key) const
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
    // Note: We can't enable SMP mode without enabling the IOAPIC.
    if (!is_ioapic_enabled())
        return false;
    return lookup("smp"sv).value_or("off"sv) == "on"sv;
}

UNMAP_AFTER_INIT bool CommandLine::is_smp_enabled_without_ioapic_enabled() const
{
    auto smp_enabled = lookup("smp"sv).value_or("off"sv) == "on"sv;
    return smp_enabled && !is_ioapic_enabled();
}

UNMAP_AFTER_INIT bool CommandLine::is_ioapic_enabled() const
{
    auto value = lookup("enable_ioapic"sv).value_or("on"sv);
    if (value == "on"sv)
        return true;
    if (value == "off"sv)
        return false;
    PANIC("Unknown enable_ioapic setting: {}", value);
}

UNMAP_AFTER_INIT bool CommandLine::is_vmmouse_enabled() const
{
    return lookup("vmmouse"sv).value_or("on"sv) == "on"sv;
}

UNMAP_AFTER_INIT PCIAccessLevel CommandLine::pci_access_level() const
{
    auto value = lookup("pci_ecam"sv).value_or("on"sv);
    if (value == "on"sv)
        return PCIAccessLevel::MemoryAddressing;
    if (value == "off"sv)
        return PCIAccessLevel::IOAddressing;
    PANIC("Unknown PCI ECAM setting: {}", value);
}

UNMAP_AFTER_INIT bool CommandLine::is_legacy_time_enabled() const
{
    return lookup("time"sv).value_or("modern"sv) == "legacy"sv;
}

bool CommandLine::is_pc_speaker_enabled() const
{
    auto value = lookup("pcspeaker"sv).value_or("off"sv);
    if (value == "on"sv)
        return true;
    if (value == "off"sv)
        return false;
    PANIC("Unknown pcspeaker setting: {}", value);
}

UNMAP_AFTER_INIT bool CommandLine::is_force_pio() const
{
    return contains("force_pio"sv);
}

UNMAP_AFTER_INIT StringView CommandLine::root_device() const
{
    return lookup("root"sv).value_or("/dev/hda"sv);
}

bool CommandLine::is_nvme_polling_enabled() const
{
    return contains("nvme_poll"sv);
}

UNMAP_AFTER_INIT AcpiFeatureLevel CommandLine::acpi_feature_level() const
{
    auto value = kernel_command_line().lookup("acpi"sv).value_or("limited"sv);
    if (value == "limited"sv)
        return AcpiFeatureLevel::Limited;
    if (value == "off"sv)
        return AcpiFeatureLevel::Disabled;
    if (value == "on"sv)
        return AcpiFeatureLevel::Enabled;
    PANIC("Unknown ACPI feature level: {}", value);
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

UNMAP_AFTER_INIT bool CommandLine::is_physical_networking_disabled() const
{
    return contains("disable_physical_networking"sv);
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

UNMAP_AFTER_INIT bool CommandLine::disable_usb() const
{
    return contains("disable_usb"sv);
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
    }
    if (ahci_reset_mode == "aggressive"sv) {
        return AHCIResetMode::Aggressive;
    }
    PANIC("Unknown AHCIResetMode: {}", ahci_reset_mode);
}

StringView CommandLine::system_mode() const
{
    return lookup("system_mode"sv).value_or("graphical"sv);
}

PanicMode CommandLine::panic_mode(Validate should_validate) const
{
    const auto panic_mode = lookup("panic"sv).value_or("halt"sv);
    if (panic_mode == "halt"sv) {
        return PanicMode::Halt;
    }
    if (panic_mode == "shutdown"sv) {
        return PanicMode::Shutdown;
    }

    if (should_validate == Validate::Yes)
        PANIC("Unknown PanicMode: {}", panic_mode);

    return PanicMode::Halt;
}

UNMAP_AFTER_INIT auto CommandLine::are_framebuffer_devices_enabled() const -> FrameBufferDevices
{
    const auto fbdev_value = lookup("fbdev"sv).value_or("on"sv);
    if (fbdev_value == "on"sv)
        return FrameBufferDevices::Enabled;
    if (fbdev_value == "bootloader"sv)
        return FrameBufferDevices::BootloaderOnly;
    return FrameBufferDevices::ConsoleOnly;
}

StringView CommandLine::userspace_init() const
{
    return lookup("init"sv).value_or("/bin/SystemServer"sv);
}

NonnullOwnPtrVector<KString> CommandLine::userspace_init_args() const
{
    NonnullOwnPtrVector<KString> args;

    auto init_args = lookup("init_args"sv).value_or(""sv).split_view(';');
    if (!init_args.is_empty())
        MUST(args.try_prepend(KString::must_create(userspace_init())));
    for (auto& init_arg : init_args)
        args.append(KString::must_create(init_arg));
    return args;
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
