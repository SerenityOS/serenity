/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Firmware/BIOS.h>
#include <Kernel/Firmware/PowerStateSwitch.h>
#include <Kernel/Firmware/SysFSFirmware.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT void FirmwareSysFSDirectory::initialize()
{
    auto firmware_directory = adopt_ref_if_nonnull(new (nothrow) FirmwareSysFSDirectory()).release_nonnull();
    SysFSComponentRegistry::the().register_new_component(firmware_directory);
    firmware_directory->create_components();
}

void FirmwareSysFSDirectory::create_components()
{
    m_components.append(BIOSSysFSDirectory::must_create(*this));
    m_components.append(ACPI::ACPISysFSDirectory::must_create(*this));
    m_components.append(PowerStateSwitchNode::must_create(*this));
}

UNMAP_AFTER_INIT FirmwareSysFSDirectory::FirmwareSysFSDirectory()
    : SysFSDirectory(SysFSComponentRegistry::the().root_directory())
{
}

}
