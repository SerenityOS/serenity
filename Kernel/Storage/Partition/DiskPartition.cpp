/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Storage/Partition/DiskPartition.h>

namespace Kernel {

NonnullRefPtr<DiskPartition> DiskPartition::create(BlockDevice& device, unsigned minor_number, DiskPartitionMetadata metadata)
{
    auto partition_or_error = DeviceManagement::try_create_device<DiskPartition>(device, minor_number, metadata);
    // FIXME: Find a way to propagate errors
    VERIFY(!partition_or_error.is_error());
    return partition_or_error.release_value();
}

DiskPartition::DiskPartition(BlockDevice& device, unsigned minor_number, DiskPartitionMetadata metadata)
    : BlockDevice(100, minor_number, device.block_size())
    , m_device(device)
    , m_metadata(metadata)
{
}

DiskPartition::~DiskPartition()
{
}

const DiskPartitionMetadata& DiskPartition::metadata() const
{
    return m_metadata;
}

void DiskPartition::start_request(AsyncBlockDeviceRequest& request)
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

ErrorOr<size_t> DiskPartition::read(OpenFileDescription& fd, u64 offset, UserOrKernelBuffer& outbuf, size_t len)
{
    unsigned adjust = m_metadata.start_block() * block_size();
    dbgln_if(OFFD_DEBUG, "DiskPartition::read offset={}, adjust={}, len={}", fd.offset(), adjust, len);
    return m_device.strong_ref()->read(fd, offset + adjust, outbuf, len);
}

bool DiskPartition::can_read(const OpenFileDescription& fd, size_t offset) const
{
    unsigned adjust = m_metadata.start_block() * block_size();
    dbgln_if(OFFD_DEBUG, "DiskPartition::can_read offset={}, adjust={}", offset, adjust);
    return m_device.strong_ref()->can_read(fd, offset + adjust);
}

ErrorOr<size_t> DiskPartition::write(OpenFileDescription& fd, u64 offset, const UserOrKernelBuffer& inbuf, size_t len)
{
    unsigned adjust = m_metadata.start_block() * block_size();
    dbgln_if(OFFD_DEBUG, "DiskPartition::write offset={}, adjust={}, len={}", offset, adjust, len);
    return m_device.strong_ref()->write(fd, offset + adjust, inbuf, len);
}

bool DiskPartition::can_write(const OpenFileDescription& fd, size_t offset) const
{
    unsigned adjust = m_metadata.start_block() * block_size();
    dbgln_if(OFFD_DEBUG, "DiskPartition::can_write offset={}, adjust={}", offset, adjust);
    return m_device.strong_ref()->can_write(fd, offset + adjust);
}

StringView DiskPartition::class_name() const
{
    return "DiskPartition"sv;
}

}
