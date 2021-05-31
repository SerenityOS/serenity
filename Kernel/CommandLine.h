/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace Kernel {

enum class BootMode {
    NoFramebufferDevices,
    SelfTest,
    Graphical
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
    MappingPerBus,
    MappingPerDevice,
};

enum class AHCIResetMode {
    ControllerOnly,
    Aggressive,
};

class CommandLine {
    AK_MAKE_ETERNAL;

public:
    static void early_initialize(const char* cmd_line);
    static void initialize();

    [[nodiscard]] const String& string() const { return m_string; }
    Optional<String> lookup(const StringView& key) const;
    [[nodiscard]] bool contains(const StringView& key) const;

    [[nodiscard]] bool is_boot_profiling_enabled() const;
    [[nodiscard]] bool is_ide_enabled() const;
    [[nodiscard]] bool is_smp_enabled() const;
    [[nodiscard]] bool is_vmmouse_enabled() const;
    [[nodiscard]] PCIAccessLevel pci_access_level() const;
    [[nodiscard]] bool is_legacy_time_enabled() const;
    [[nodiscard]] bool is_no_framebuffer_devices_mode() const;
    [[nodiscard]] bool is_force_pio() const;
    [[nodiscard]] AcpiFeatureLevel acpi_feature_level() const;
    [[nodiscard]] BootMode boot_mode() const;
    [[nodiscard]] HPETMode hpet_mode() const;
    [[nodiscard]] bool disable_physical_storage() const;
    [[nodiscard]] bool disable_ps2_controller() const;
    [[nodiscard]] bool disable_uhci_controller() const;
    [[nodiscard]] bool disable_virtio() const;
    [[nodiscard]] AHCIResetMode ahci_reset_mode() const;
    [[nodiscard]] String userspace_init() const;
    [[nodiscard]] Vector<String> userspace_init_args() const;
    [[nodiscard]] String root_device() const;
    [[nodiscard]] size_t switch_to_tty() const;

private:
    CommandLine(const String&);

    void add_arguments(const Vector<StringView>& args);
    void build_commandline(const String& cmdline_from_bootloader);

    String m_string;
    HashMap<StringView, StringView> m_params;
};

const CommandLine& kernel_command_line();

}
