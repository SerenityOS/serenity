/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Storage/AHCI/ATADiskDevice.h>
#include <Kernel/Devices/Storage/StorageManagement.h>
#include <Kernel/Sections.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<ATADiskDevice>> ATADiskDevice::create(AHCIController const& controller, ATA::Address ata_address, u16 capabilities, u16 logical_sector_size, u64 max_addressable_block)
{
    return TRY(Device::try_create_device<ATADiskDevice>(controller, ata_address, capabilities, logical_sector_size, max_addressable_block));
}

ATADiskDevice::ATADiskDevice(AHCIController const& controller, ATA::Address ata_address, u16 capabilities, u16 logical_sector_size, u64 max_addressable_block)
    : ATADevice(controller, ata_address, capabilities, logical_sector_size, max_addressable_block)
{
}

ATADiskDevice::~ATADiskDevice() = default;

StringView ATADiskDevice::class_name() const
{
    return "ATADiskDevice"sv;
}

}
