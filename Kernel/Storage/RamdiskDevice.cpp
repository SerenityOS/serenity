/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <AK/StringView.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Storage/RamdiskController.h>
#include <Kernel/Storage/RamdiskDevice.h>

namespace Kernel {

NonnullRefPtr<RamdiskDevice> RamdiskDevice::create(const RamdiskController& controller, NonnullOwnPtr<Memory::Region>&& region, int major, int minor)
{
    auto device_or_error = DeviceManagement::try_create_device<RamdiskDevice>(controller, move(region), major, minor);
    // FIXME: Find a way to propagate errors
    VERIFY(!device_or_error.is_error());
    return device_or_error.release_value();
}

RamdiskDevice::RamdiskDevice(const RamdiskController& controller, NonnullOwnPtr<Memory::Region>&& region, int major, int minor)
    : StorageDevice(controller, major, minor, 512, region->size() / 512)
    , m_region(move(region))
{
    dmesgln("Ramdisk: Device #{} @ {}, Capacity={}", minor, m_region->vaddr(), max_addressable_block() * 512);
}

RamdiskDevice::~RamdiskDevice()
{
}

StringView RamdiskDevice::class_name() const
{
    return "RamdiskDevice";
}

void RamdiskDevice::start_request(AsyncBlockDeviceRequest& request)
{
    MutexLocker locker(m_lock);

    u8* base = m_region->vaddr().as_ptr();
    size_t size = m_region->size();
    u8* offset = base + request.block_index() * 512;
    size_t length = request.block_count() * 512;

    if ((offset + length > base + size) || (offset + length < base)) {
        request.complete(AsyncDeviceRequest::Failure);
    } else {
        auto result = KResult(KSuccess);
        if (request.request_type() == AsyncBlockDeviceRequest::Read) {
            result = request.buffer().write(offset, length);
        } else {
            result = request.buffer().read(offset, length);
        }
        request.complete(result.is_success() ? AsyncDeviceRequest::Success : AsyncDeviceRequest::MemoryFault);
    }
}

String RamdiskDevice::storage_name() const
{
    // FIXME: Try to not hardcode a maximum of 16 partitions per drive!
    size_t drive_index = minor() / 16;
    return String::formatted("ramdisk{}", drive_index);
}

}
