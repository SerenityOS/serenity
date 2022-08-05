/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/ATA/ATADevice.h>
#include <Kernel/Storage/StorageManagement.h>

namespace Kernel {

static StorageDevice::LUNAddress convert_ata_address_to_lun_address(ATAController const& controller, ATADevice::Address ata_address)
{
    return StorageDevice::LUNAddress { controller.controller_id(), ata_address.port, ata_address.subport };
}

ATADevice::ATADevice(ATAController const& controller, ATADevice::Address ata_address, u16 capabilities, u16 logical_sector_size, u64 max_addressable_block)
    : StorageDevice(convert_ata_address_to_lun_address(controller, ata_address), controller.hardware_relative_controller_id(), logical_sector_size, max_addressable_block)
    , m_controller(controller)
    , m_ata_address(ata_address)
    , m_capabilities(capabilities)
{
}

ATADevice::~ATADevice() = default;

void ATADevice::start_request(AsyncBlockDeviceRequest& request)
{
    auto controller = m_controller.strong_ref();
    VERIFY(controller);
    controller->start_request(*this, request);
}

}
