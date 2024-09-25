/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/USB/USBController.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/Storage/StorageManagement.h>
#include <Kernel/Devices/Storage/USB/BulkSCSIInterface.h>
#include <Kernel/Devices/Storage/USB/SCSIComands.h>

namespace Kernel::USB {

BulkSCSIInterface::BulkSCSIInterface(StorageDevice::LUNAddress logical_unit_number_address, size_t sector_size, u64 max_addressable_block, USB::Device& device, NonnullOwnPtr<BulkInPipe> in_pipe, NonnullOwnPtr<BulkOutPipe> out_pipe)
    : m_device(device)
    , m_in_pipe(move(in_pipe))
    , m_out_pipe(move(out_pipe))
{
    auto storage_device = DeviceManagement::try_create_device<BulkSCSIStorageDevice>(
        *this,
        *m_out_pipe,
        *m_in_pipe,
        logical_unit_number_address,
        device.address(), // FIXME: Figure out a better ID to put here
        sector_size,
        max_addressable_block)
                              .release_value_but_fixme_should_propagate_errors();

    m_storage_devices.append(storage_device);

    StorageManagement::the().add_device(storage_device);
}

BulkSCSIInterface::~BulkSCSIInterface()
{
    for (auto& storage_device : m_storage_devices)
        StorageManagement::the().remove_device(storage_device);
}

}
