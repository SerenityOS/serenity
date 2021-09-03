/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/Memory.h>
#include <YAK/StringView.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Storage/RamdiskController.h>
#include <Kernel/Storage/RamdiskDevice.h>

namespace Kernel {

NonnullRefPtr<RamdiskDevice> RamdiskDevice::create(const RamdiskController& controller, NonnullOwnPtr<Memory::Region>&& region, int major, int minor)
{
    return adopt_ref(*new RamdiskDevice(controller, move(region), major, minor));
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
        bool success;

        if (request.request_type() == AsyncBlockDeviceRequest::Read) {
            success = request.buffer().write(offset, length);
        } else {
            success = request.buffer().read(offset, length);
        }

        request.complete(success ? AsyncDeviceRequest::Success : AsyncDeviceRequest::MemoryFault);
    }
}

String RamdiskDevice::device_name() const
{
    // FIXME: Try to not hardcode a maximum of 16 partitions per drive!
    size_t drive_index = minor() / 16;
    return String::formatted("ramdisk{}", drive_index);
}

}
