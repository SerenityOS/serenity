/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/ATA/ATAPIDiscDevice.h>
#include <Kernel/Storage/ATA/IDEChannel.h>
#include <Kernel/Storage/ATA/IDEController.h>
#include <Kernel/Storage/StorageManagement.h>

namespace Kernel {

NonnullRefPtr<ATAPIDiscDevice> ATAPIDiscDevice::create(ATAController const& controller, ATADevice::Address ata_address, u16 capabilities, u64 max_addressable_block)
{
    auto minor_device_number = StorageManagement::generate_storage_minor_number();

    auto device_name = MUST(KString::formatted("hd{:c}", 'a' + minor_device_number.value()));

    auto disc_device_or_error = DeviceManagement::try_create_device<ATAPIDiscDevice>(controller, ata_address, minor_device_number.value(), capabilities, max_addressable_block, move(device_name));
    // FIXME: Find a way to propagate errors
    VERIFY(!disc_device_or_error.is_error());
    return disc_device_or_error.release_value();
}

ATAPIDiscDevice::ATAPIDiscDevice(ATAController const& controller, ATADevice::Address ata_address, MinorNumber minor_number, u16 capabilities, u64 max_addressable_block, NonnullOwnPtr<KString> early_storage_name)
    : ATADevice(controller, ata_address, minor_number, capabilities, 0, max_addressable_block, move(early_storage_name))
{
}

ATAPIDiscDevice::~ATAPIDiscDevice() = default;

StringView ATAPIDiscDevice::class_name() const
{
    return "ATAPIDiscDevice";
}

}
