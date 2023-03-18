/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/Storage/ATA/ATADiskDevice.h>
#include <Kernel/Devices/Storage/StorageManagement.h>
#include <Kernel/Sections.h>

namespace Kernel {

NonnullLockRefPtr<ATADiskDevice> ATADiskDevice::create(ATAController const& controller, ATADevice::Address ata_address, u16 capabilities, u16 logical_sector_size, u64 max_addressable_block)
{
    auto disk_device_or_error = DeviceManagement::try_create_device<ATADiskDevice>(controller, ata_address, capabilities, logical_sector_size, max_addressable_block);
    // FIXME: Find a way to propagate errors
    VERIFY(!disk_device_or_error.is_error());
    return disk_device_or_error.release_value();
}

ATADiskDevice::ATADiskDevice(ATAController const& controller, ATADevice::Address ata_address, u16 capabilities, u16 logical_sector_size, u64 max_addressable_block)
    : ATADevice(controller, ata_address, capabilities, logical_sector_size, max_addressable_block)
{
}

ATADiskDevice::~ATADiskDevice() = default;

StringView ATADiskDevice::class_name() const
{
    return "ATADiskDevice"sv;
}

}
