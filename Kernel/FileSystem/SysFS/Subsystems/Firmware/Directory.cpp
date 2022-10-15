/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/SysFS/Registry.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Firmware/BIOS/Directory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Firmware/Directory.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT void FirmwareSysFSDirectory::initialize()
{
    auto firmware_directory = adopt_lock_ref_if_nonnull(new (nothrow) FirmwareSysFSDirectory()).release_nonnull();
    SysFSComponentRegistry::the().register_new_component(firmware_directory);
    firmware_directory->create_components();
}

void FirmwareSysFSDirectory::create_components()
{
    MUST(m_child_components.with([&](auto& list) -> ErrorOr<void> {
        list.append(BIOSSysFSDirectory::must_create(*this));
        if (ACPI::is_enabled())
            list.append(ACPI::ACPISysFSDirectory::must_create(*this));
        return {};
    }));
}

UNMAP_AFTER_INIT FirmwareSysFSDirectory::FirmwareSysFSDirectory()
    : SysFSDirectory(SysFSComponentRegistry::the().root_directory())
{
}

}
