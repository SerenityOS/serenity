/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/ATAPIDiscDevice.h>
#include <Kernel/Storage/IDEChannel.h>
#include <Kernel/Storage/IDEController.h>

namespace Kernel {

NonnullRefPtr<ATAPIDiscDevice> ATAPIDiscDevice::create(const ATAController& controller, ATADevice::Address ata_address, u16 capabilities, u64 max_addressable_block)
{
    auto disc_device_or_error = DeviceManagement::try_create_device<ATAPIDiscDevice>(controller, ata_address, capabilities, max_addressable_block);
    // FIXME: Find a way to propagate errors
    VERIFY(!disc_device_or_error.is_error());
    return disc_device_or_error.release_value();
}

ATAPIDiscDevice::ATAPIDiscDevice(const ATAController& controller, ATADevice::Address ata_address, u16 capabilities, u64 max_addressable_block)
    : ATADevice(controller, ata_address, capabilities, 0, max_addressable_block)
{
}

ATAPIDiscDevice::~ATAPIDiscDevice()
{
}

StringView ATAPIDiscDevice::class_name() const
{
    return "ATAPIDiscDevice";
}

}
