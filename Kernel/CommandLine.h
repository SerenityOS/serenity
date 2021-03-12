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

#pragma once

#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace Kernel {

enum class BootMode {
    Text,
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

enum class AHCIResetMode {
    ControllerOnly,
    Complete,
    None
};

class CommandLine {
    AK_MAKE_ETERNAL;

public:
    static void early_initialize(const char* cmd_line);
    static void initialize();

    [[nodiscard]] const String& string() const { return m_string; }
    Optional<String> lookup(const String& key) const;
    [[nodiscard]] bool contains(const String& key) const;

    [[nodiscard]] bool is_boot_profiling_enabled() const;
    [[nodiscard]] bool is_ide_enabled() const;
    [[nodiscard]] bool is_smp_enabled() const;
    [[nodiscard]] bool is_vmmouse_enabled() const;
    [[nodiscard]] bool is_mmio_enabled() const;
    [[nodiscard]] bool is_legacy_time_enabled() const;
    [[nodiscard]] bool is_text_mode() const;
    [[nodiscard]] bool is_force_pio() const;
    [[nodiscard]] AcpiFeatureLevel acpi_feature_level() const;
    [[nodiscard]] BootMode boot_mode() const;
    [[nodiscard]] HPETMode hpet_mode() const;
    [[nodiscard]] AHCIResetMode ahci_reset_mode() const;
    [[nodiscard]] String userspace_init() const;
    [[nodiscard]] Vector<String> userspace_init_args() const;
    [[nodiscard]] String root_device() const;

private:
    CommandLine(const String&);

    String m_string;
    HashMap<String, String> m_params;
};

const CommandLine& kernel_command_line();

}
