/*
 * Copyright (c) 2023, Kirill Nikolaev <cyril7@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Bus/VirtIO/Transport/PCIe/TransportLink.h>
#include <Kernel/Devices/Storage/StorageManagement.h>
#include <Kernel/Devices/Storage/VirtIO/VirtIOBlockController.h>
#include <Kernel/Devices/Storage/VirtIO/VirtIOBlockDevice.h>

namespace Kernel {

VirtIOBlockController::VirtIOBlockController()
    : StorageController(StorageManagement::generate_controller_id())
{
}

bool VirtIOBlockController::is_handled(PCI::DeviceIdentifier const& device_identifier)
{
    return device_identifier.hardware_id().vendor_id == PCI::VendorID::VirtIO
        && device_identifier.hardware_id().device_id == PCI::DeviceID::VirtIOBlockDevice;
}

ErrorOr<void> VirtIOBlockController::add_device(PCI::DeviceIdentifier const& device_identifier)
{
    // NB: Thread-unsafe, but device initialization is single threaded anyway.
    auto index = m_devices.size();
    auto lun = StorageDevice::LUNAddress { controller_id(), (u32)index, 0 };
    auto cid = hardware_relative_controller_id();

    auto transport_link = TRY(VirtIO::PCIeTransportLink::create(device_identifier));

    auto device = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) VirtIOBlockDevice(move(transport_link), lun, cid)));
    TRY(device->initialize_virtio_resources());

    m_devices.append(device);
    return {};
}

LockRefPtr<StorageDevice> VirtIOBlockController::device(u32 index) const
{
    return m_devices[index];
}

void VirtIOBlockController::complete_current_request(AsyncDeviceRequest::RequestResult)
{
    VERIFY_NOT_REACHED();
}

}
