/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/Storage/StorageDevicePartition.h>
#include <Kernel/FileSystem/OpenFileDescription.h>

namespace Kernel {

NonnullLockRefPtr<StorageDevicePartition> StorageDevicePartition::create(BlockDevice& device, MinorNumber minor_number, Partition::DiskPartitionMetadata metadata)
{
    auto partition_or_error = DeviceManagement::try_create_device<StorageDevicePartition>(device, minor_number, metadata);
    // FIXME: Find a way to propagate errors
    VERIFY(!partition_or_error.is_error());
    return partition_or_error.release_value();
}

StorageDevicePartition::StorageDevicePartition(BlockDevice& device, MinorNumber minor_number, Partition::DiskPartitionMetadata metadata)
    : BlockDevice(100, minor_number, device.block_size())
    , m_device(device)
    , m_metadata(metadata)
{
}

StorageDevicePartition::~StorageDevicePartition() = default;

Partition::DiskPartitionMetadata const& StorageDevicePartition::metadata() const
{
    return m_metadata;
}

void StorageDevicePartition::start_request(AsyncBlockDeviceRequest& request)
{
    auto device = m_device.strong_ref();
    if (!device)
        request.complete(AsyncBlockDeviceRequest::RequestResult::Failure);
    auto sub_request_or_error = device->try_make_request<AsyncBlockDeviceRequest>(request.request_type(),
        request.block_index() + m_metadata.start_block(), request.block_count(), request.buffer(), request.buffer_size());
    if (sub_request_or_error.is_error())
        TODO();
    request.add_sub_request(sub_request_or_error.release_value());
}

ErrorOr<size_t> StorageDevicePartition::read(OpenFileDescription& fd, u64 offset, UserOrKernelBuffer& outbuf, size_t len)
{
    // NOTE: The last available offset is actually just after the last addressable block.
    if (offset >= (m_metadata.end_block() - m_metadata.start_block() + 1) * block_size())
        return 0;
    size_t nread = min(static_cast<size_t>((m_metadata.end_block() - m_metadata.start_block() + 1) - offset), len);
    u64 adjust = m_metadata.start_block() * block_size();
    dbgln_if(OFFD_DEBUG, "StorageDevicePartition::read offset={}, adjust={}, len={}", fd.offset(), adjust, nread);
    return m_device.strong_ref()->read(fd, offset + adjust, outbuf, nread);
}

ErrorOr<size_t> StorageDevicePartition::write(OpenFileDescription& fd, u64 offset, UserOrKernelBuffer const& inbuf, size_t len)
{
    // NOTE: The last available offset is actually just after the last addressable block.
    if (offset >= (m_metadata.end_block() - m_metadata.start_block() + 1) * block_size())
        return Error::from_errno(ENOSPC);
    size_t nwrite = min(static_cast<size_t>((m_metadata.end_block() - m_metadata.start_block() + 1) - offset), len);
    u64 adjust = m_metadata.start_block() * block_size();
    dbgln_if(OFFD_DEBUG, "StorageDevicePartition::write offset={}, adjust={}, len={}", offset, adjust, nwrite);
    return m_device.strong_ref()->write(fd, offset + adjust, inbuf, nwrite);
}

StringView StorageDevicePartition::class_name() const
{
    return "StorageDevicePartition"sv;
}

}
