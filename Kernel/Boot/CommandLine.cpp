/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Library/StdLib.h>
#include <Kernel/Sections.h>

namespace Kernel {

static char s_cmd_line[1024];
static constexpr StringView s_embedded_cmd_line = ""sv;
static CommandLine* s_the;

UNMAP_AFTER_INIT void CommandLine::early_initialize(StringView cmd_line)
{
    (void)cmd_line.copy_characters_to_buffer(s_cmd_line, sizeof(s_cmd_line));
}

bool CommandLine::was_initialized()
{
    return s_the != nullptr;
}

CommandLine const& kernel_command_line()
{
    VERIFY(s_the);
    return *s_the;
}

UNMAP_AFTER_INIT void CommandLine::initialize()
{
    VERIFY(!s_the);
    s_the = new CommandLine({ s_cmd_line, strlen(s_cmd_line) });
    dmesgln("Kernel Commandline: {}", kernel_command_line().string());
    // Validate the modes the user passed in.
    (void)s_the->panic_mode(Validate::Yes);
}

UNMAP_AFTER_INIT NonnullOwnPtr<KString> CommandLine::build_commandline(StringView cmdline_from_bootloader)
{
    StringBuilder builder;
    builder.append(cmdline_from_bootloader);
    if constexpr (!s_embedded_cmd_line.is_empty()) {
        builder.append(' ');
        builder.append(s_embedded_cmd_line);
    }
    return KString::must_create(builder.string_view());
}

UNMAP_AFTER_INIT void CommandLine::add_arguments(Vector<StringView> const& args)
{
    for (auto&& str : args) {
        if (str == ""sv) {
            continue;
        }
        // Some boot loaders may include complex key-value pairs where the value is a composite entry,
        // we handle this by only checking for the first equals sign in each command line parameter.
        auto key = str.find_first_split_view('=');
        if (key.length() == str.length())
            m_params.set(key, ""sv);
        else
            m_params.set(key, str.substring_view(key.length() + 1));
    }
}

UNMAP_AFTER_INIT CommandLine::CommandLine(StringView cmdline_from_bootloader)
    : m_string(build_commandline(cmdline_from_bootloader))
{
    s_the = this;
    auto const& args = m_string->view().split_view(' ');
    MUST(m_params.try_ensure_capacity(args.size()));
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

UNMAP_AFTER_INIT bool CommandLine::is_early_boot_console_disabled() const
{
    auto value = lookup("early_boot_console"sv).value_or("on"sv);
    if (value == "on"sv)
        return false;
    if (value == "off"sv)
        return true;
    PANIC("Unknown early_boot_console setting: {}", value);
}

UNMAP_AFTER_INIT bool CommandLine::i8042_enable_first_port_translation() const
{
    // FIXME: Disable first port translation when the keyboard works OK.
    auto value = lookup("i8042_first_port_translation"sv).value_or("on"sv);
    if (value == "off"sv)
        return false;
    if (value == "on"sv)
        return true;
    PANIC("Unknown i8042_enable_first_port_translation setting: {}", value);
}

UNMAP_AFTER_INIT I8042PresenceMode CommandLine::i8042_presence_mode() const
{
    auto value = lookup("i8042_presence_mode"sv).value_or("auto"sv);
    if (value == "auto"sv)
        return I8042PresenceMode::Automatic;
    if (value == "none"sv)
        return I8042PresenceMode::None;
    if (value == "force"sv)
        return I8042PresenceMode::Force;
    if (value == "aggressive-test"sv)
        return I8042PresenceMode::AggressiveTest;
    PANIC("Unknown i8042_presence_mode setting: {}", value);
}

UNMAP_AFTER_INIT bool CommandLine::is_vmmouse_enabled() const
{
    return lookup("vmmouse"sv).value_or("on"sv) == "on"sv;
}

UNMAP_AFTER_INIT PCIAccessLevel CommandLine::pci_access_level() const
{
    auto value = lookup("pci"sv).value_or("ecam"sv);
    if (value == "ecam"sv)
        return PCIAccessLevel::MemoryAddressing;
#if ARCH(X86_64)
    if (value == "io"sv)
        return PCIAccessLevel::IOAddressing;
#endif
    if (value == "none"sv)
        return PCIAccessLevel::None;
    PANIC("Unknown PCI ECAM setting: {}", value);
}

UNMAP_AFTER_INIT bool CommandLine::is_pci_disabled() const
{
    return lookup("pci"sv).value_or("ecam"sv) == "none"sv;
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

UNMAP_AFTER_INIT StringView CommandLine::root_device() const
{
    return lookup("root"sv).value_or("lun0:0:0"sv);
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

UNMAP_AFTER_INIT bool CommandLine::disable_ps2_mouse() const
{
    return contains("disable_ps2_mouse"sv);
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
    auto const ahci_reset_mode = lookup("ahci_reset_mode"sv).value_or("controllers"sv);
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
    auto const panic_mode = lookup("panic"sv).value_or("halt"sv);
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

UNMAP_AFTER_INIT CommandLine::GraphicsSubsystemMode CommandLine::graphics_subsystem_mode() const
{
    auto const graphics_subsystem_mode_value = lookup("graphics_subsystem_mode"sv).value_or("on"sv);
    if (graphics_subsystem_mode_value == "on"sv)
        return GraphicsSubsystemMode::Enabled;
    if (graphics_subsystem_mode_value == "limited"sv)
        return GraphicsSubsystemMode::Limited;
    if (graphics_subsystem_mode_value == "off"sv)
        return GraphicsSubsystemMode::Disabled;
    PANIC("Invalid graphics_subsystem_mode value: {}", graphics_subsystem_mode_value);
}

StringView CommandLine::userspace_init() const
{
    return lookup("init"sv).value_or("/init"sv);
}

Vector<NonnullOwnPtr<KString>> CommandLine::userspace_init_args() const
{
    Vector<NonnullOwnPtr<KString>> args;

    auto init_args = lookup("init_args"sv).value_or(""sv).split_view(';');
    if (!init_args.is_empty())
        MUST(args.try_prepend(MUST(KString::try_create(userspace_init()))));
    for (auto& init_arg : init_args)
        args.append(MUST(KString::try_create(init_arg)));
    return args;
}

UNMAP_AFTER_INIT size_t CommandLine::switch_to_tty() const
{
    auto const default_tty = lookup("switch_to_tty"sv).value_or("1"sv);
    auto switch_tty_number = default_tty.to_number<unsigned>();
    if (switch_tty_number.has_value() && switch_tty_number.value() >= 1) {
        return switch_tty_number.value() - 1;
    }
    PANIC("Invalid default tty value: {}", default_tty);
}
}
