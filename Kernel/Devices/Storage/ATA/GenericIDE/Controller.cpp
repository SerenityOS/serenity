/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Devices/Storage/ATA/ATADiskDevice.h>
#include <Kernel/Devices/Storage/ATA/GenericIDE/Channel.h>
#include <Kernel/Devices/Storage/ATA/GenericIDE/Controller.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Sections.h>

namespace Kernel {

void IDEController::start_request(ATADevice const& device, AsyncBlockDeviceRequest& request)
{
    auto& address = device.ata_address();
    VERIFY(address.subport < 2);
    switch (address.port) {
    case 0: {
        auto result = m_channels[0]->start_request(device, request);
        // FIXME: Propagate errors properly
        VERIFY(!result.is_error());
        return;
    }
    case 1: {
        auto result = m_channels[1]->start_request(device, request);
        // FIXME: Propagate errors properly
        VERIFY(!result.is_error());
        return;
    }
    }
    VERIFY_NOT_REACHED();
}

void IDEController::complete_current_request(AsyncDeviceRequest::RequestResult)
{
    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT IDEController::IDEController() = default;
UNMAP_AFTER_INIT IDEController::~IDEController() = default;

}
