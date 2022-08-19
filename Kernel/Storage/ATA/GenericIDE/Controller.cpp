/*
 * Copyright (c) 2020-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/FileSystem/ProcFS.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Sections.h>
#include <Kernel/Storage/ATA/ATADiskDevice.h>
#include <Kernel/Storage/ATA/GenericIDE/Channel.h>
#include <Kernel/Storage/ATA/GenericIDE/Controller.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullLockRefPtr<IDEController> IDEController::initialize()
{
    return adopt_lock_ref(*new IDEController());
}

bool IDEController::reset()
{
    TODO();
}

bool IDEController::shutdown()
{
    TODO();
}

size_t IDEController::devices_count() const
{
    size_t count = 0;
    for (u32 index = 0; index < 4; index++) {
        if (!device(index).is_null())
            count++;
    }
    return count;
}

void IDEController::start_request(ATADevice const& device, AsyncBlockDeviceRequest& request)
{
    auto& address = device.ata_address();
    VERIFY(address.subport < 2);
    switch (address.port) {
    case 0: {
        auto result = m_channels[0].start_request(device, request);
        // FIXME: Propagate errors properly
        VERIFY(!result.is_error());
        return;
    }
    case 1: {
        auto result = m_channels[1].start_request(device, request);
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

LockRefPtr<StorageDevice> IDEController::device_by_channel_and_position(u32 index) const
{
    switch (index) {
    case 0:
        return m_channels[0].connected_device(0);
    case 1:
        return m_channels[0].connected_device(1);
    case 2:
        return m_channels[1].connected_device(0);
    case 3:
        return m_channels[1].connected_device(1);
    }
    VERIFY_NOT_REACHED();
}

LockRefPtr<StorageDevice> IDEController::device(u32 index) const
{
    NonnullLockRefPtrVector<StorageDevice> connected_devices;
    for (size_t index = 0; index < 4; index++) {
        auto checked_device = device_by_channel_and_position(index);
        if (checked_device.is_null())
            continue;
        connected_devices.append(checked_device.release_nonnull());
    }
    if (index >= connected_devices.size())
        return nullptr;
    return connected_devices[index];
}
}
