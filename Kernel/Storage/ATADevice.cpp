/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/ATADevice.h>
#include <Kernel/Storage/IDEChannel.h>
#include <Kernel/Storage/IDEController.h>

namespace Kernel {

ATADevice::ATADevice(const ATAController& controller, ATADevice::Address ata_address, u16 capabilities, u16 logical_sector_size, u64 max_addressable_block)
    : StorageDevice(logical_sector_size, max_addressable_block)
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
