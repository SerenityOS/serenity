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
    auto bios_directory_or_error = BIOSSysFSDirectory::try_create(*this);
    VERIFY(!bios_directory_or_error.is_error());
    auto acpi_directory_or_error = ACPI::ACPISysFSDirectory::try_create(*this);
    VERIFY(!acpi_directory_or_error.is_error());
    auto power_state_switch_node = PowerStateSwitchNode::must_create(*this);
    m_components.append(bios_directory_or_error.release_value());
    m_components.append(acpi_directory_or_error.release_value());
    m_components.append(power_state_switch_node);
}

UNMAP_AFTER_INIT FirmwareSysFSDirectory::FirmwareSysFSDirectory()
    : SysFSDirectory("firmware", SysFSComponentRegistry::the().root_directory())
{
}

}
