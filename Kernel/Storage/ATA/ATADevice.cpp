/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/ATA/ATADevice.h>
#include <Kernel/Storage/ATA/IDEChannel.h>
#include <Kernel/Storage/ATA/IDEController.h>
#include <Kernel/Storage/StorageManagement.h>

namespace Kernel {

ATADevice::ATADevice(const ATAController& controller, ATADevice::Address ata_address, MinorNumber minor_number, u16 capabilities, u16 logical_sector_size, u64 max_addressable_block, NonnullOwnPtr<KString> early_storage_name)
    : StorageDevice(StorageManagement::storage_type_major_number(), minor_number, logical_sector_size, max_addressable_block, move(early_storage_name))
    , m_controller(controller)
    , m_ata_address(ata_address)
    , m_capabilities(capabilities)
{
}

ATADevice::~ATADevice()
{
}

void ATADevice::start_request(AsyncBlockDeviceRequest& request)
{
    auto controller = m_controller.strong_ref();
    VERIFY(controller);
    controller->start_request(*this, request);
}

}
