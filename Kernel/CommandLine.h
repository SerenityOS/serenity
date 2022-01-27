/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/Optional.h>
#include <AK/Vector.h>
#include <Kernel/KString.h>

namespace Kernel {

enum class PanicMode {
    Halt,
    Shutdown,
};

enum class HPETMode {
    Periodic,
    NonPeriodic
};

enum class AcpiFeatureLevel {
    Enabled,
    Limited,
    Disabled,
};

enum class PCIAccessLevel {
    IOAddressing,
    MemoryAddressing,
};

enum class AHCIResetMode {
    ControllerOnly,
    Aggressive,
};

class CommandLine {

public:
    static void early_initialize(const char* cmd_line);
    static void initialize();
    static bool was_initialized();

    enum class Validate {
        Yes,
        No,
    };

    enum class FrameBufferDevices {
        Enabled,
        ConsoleOnly,
        BootloaderOnly
    };

    [[nodiscard]] StringView string() const { return m_string->view(); }
    Optional<StringView> lookup(StringView key) const;
    [[nodiscard]] bool contains(StringView key) const;

    [[nodiscard]] bool is_boot_profiling_enabled() const;
    [[nodiscard]] bool is_ide_enabled() const;
    [[nodiscard]] bool is_ioapic_enabled() const;
    [[nodiscard]] bool is_smp_enabled_without_ioapic_enabled() const;
    [[nodiscard]] bool is_smp_enabled() const;
    [[nodiscard]] bool is_physical_networking_disabled() const;
    [[nodiscard]] bool is_vmmouse_enabled() const;
    [[nodiscard]] PCIAccessLevel pci_access_level() const;
    [[nodiscard]] bool is_legacy_time_enabled() const;
    [[nodiscard]] bool is_pc_speaker_enabled() const;
    [[nodiscard]] FrameBufferDevices are_framebuffer_devices_enabled() const;
    [[nodiscard]] bool is_force_pio() const;
    [[nodiscard]] AcpiFeatureLevel acpi_feature_level() const;
    [[nodiscard]] StringView system_mode() const;
    [[nodiscard]] PanicMode panic_mode(Validate should_validate = Validate::No) const;
    [[nodiscard]] HPETMode hpet_mode() const;
    [[nodiscard]] bool disable_physical_storage() const;
    [[nodiscard]] bool disable_ps2_controller() const;
    [[nodiscard]] bool disable_uhci_controller() const;
    [[nodiscard]] bool disable_usb() const;
    [[nodiscard]] bool disable_virtio() const;
    [[nodiscard]] AHCIResetMode ahci_reset_mode() const;
    [[nodiscard]] StringView userspace_init() const;
    [[nodiscard]] NonnullOwnPtrVector<KString> userspace_init_args() const;
    [[nodiscard]] StringView root_device() const;
    [[nodiscard]] bool is_nvme_polling_enabled() const;
    [[nodiscard]] size_t switch_to_tty() const;

private:
    CommandLine(StringView);

    void add_arguments(const Vector<StringView>& args);
    static NonnullOwnPtr<KString> build_commandline(StringView cmdline_from_bootloader);

    NonnullOwnPtr<KString> m_string;
    HashMap<StringView, StringView> m_params;
};

const CommandLine& kernel_command_line();

}
