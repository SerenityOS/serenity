/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/Firmware/PCBIOS/SysFSDirectory.h>
#endif
#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Firmware/Directory.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT void SysFSFirmwareDirectory::initialize()
{
    auto firmware_directory = adopt_ref_if_nonnull(new (nothrow) SysFSFirmwareDirectory()).release_nonnull();
    SysFSComponentRegistry::the().register_new_component(firmware_directory);
    firmware_directory->create_components();
}

void SysFSFirmwareDirectory::create_components()
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
#if ARCH(X86_64)
        list.append(SysFSBIOSDirectory::must_create(*this));
#endif
        if (ACPI::is_enabled())
            list.append(ACPI::ACPISysFSDirectory::must_create(*this));
        return {};
    }));
}

UNMAP_AFTER_INIT SysFSFirmwareDirectory::SysFSFirmwareDirectory()
    : SysFSDirectory(SysFSComponentRegistry::the().root_directory())
{
}

}
