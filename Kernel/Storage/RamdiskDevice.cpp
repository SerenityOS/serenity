/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <AK/StringView.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Storage/RamdiskController.h>
#include <Kernel/Storage/RamdiskDevice.h>

namespace Kernel {

NonnullRefPtr<RamdiskDevice> RamdiskDevice::create(const RamdiskController& controller, PhysicalAddress start_address, size_t length, int major, int minor)
{
    // FIXME: Try to not hardcode a maximum of 16 partitions per drive!
    size_t drive_index = minor / 16;
    auto device_name = MUST(KString::formatted("ramdisk{}", drive_index));

    auto device_or_error = DeviceManagement::try_create_device<RamdiskDevice>(controller, start_address, length, major, minor, move(device_name));
    // FIXME: Find a way to propagate errors
    VERIFY(!device_or_error.is_error());
    return device_or_error.release_value();
}

RamdiskDevice::RamdiskDevice(const RamdiskController&, PhysicalAddress start_address, size_t length, int major, int minor, NonnullOwnPtr<KString> device_name)
    : StorageDevice(major, minor, 512, length / 512, move(device_name))
    , m_start_address(start_address)
    , m_length(length)
{
    dmesgln("Ramdisk: Device #{} @ {}, Capacity={}", minor, start_address, max_addressable_block() * 512);
}

RamdiskDevice::~RamdiskDevice()
{
}

StringView RamdiskDevice::class_name() const
{
    return "RamdiskDevice"sv;
}

void RamdiskDevice::start_request(AsyncBlockDeviceRequest& request)
{
    MutexLocker locker(m_lock);

    size_t request_offset = request.block_index() * 512;
    size_t request_length = request.block_count() * 512;

    if (Checked<size_t>::addition_would_overflow(m_start_address.get(), request_offset)) {
        request.complete(AsyncDeviceRequest::Failure);
        return;
    }

    if (Checked<size_t>::addition_would_overflow(request_offset + m_start_address.get(), request_length)) {
        request.complete(AsyncDeviceRequest::Failure);
        return;
    }

    if (m_length < (request_length + request_offset)) {
        request.complete(AsyncDeviceRequest::Failure);
        return;
    }

    auto do_io_transaction = [](AsyncBlockDeviceRequest& request, Memory::Region const& region, size_t request_offset, size_t nprocessed, size_t length_to_map) -> ErrorOr<void> {
        ErrorOr<void> result;
        auto* ptr = region.vaddr().offset((request_offset + nprocessed) % PAGE_SIZE).as_ptr();
        if (request.request_type() == AsyncBlockDeviceRequest::Read)
            result = request.buffer().write(ptr, nprocessed, length_to_map);
        else
            result = request.buffer().read(ptr, nprocessed, length_to_map);
        return result;
    };

    size_t remaining_length = request_length;
    size_t nprocessed = 0;
    ErrorOr<void> result;
    while (remaining_length > 0) {
        size_t length_to_map = min<size_t>(PAGE_SIZE, remaining_length);
        auto mapping = Memory::map_typed<u8>(m_start_address.offset(request_offset + nprocessed), length_to_map, Memory::Region::Access::ReadWrite);
        if (mapping.is_error()) {
            result = mapping.release_error();
            break;
        }
        result = do_io_transaction(request, *(mapping.value().region), request_offset, nprocessed, length_to_map);
        nprocessed += length_to_map;
        remaining_length -= length_to_map;
        if (result.is_error())
            break;
    }

    request.complete(!result.is_error() ? AsyncDeviceRequest::Success : AsyncDeviceRequest::MemoryFault);
}

}
