/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Storage/Partition/DiskPartition.h>

namespace Kernel {

NonnullRefPtr<DiskPartition> DiskPartition::create(BlockDevice& device, unsigned minor_number, DiskPartitionMetadata metadata)
{
    return adopt_ref(*new DiskPartition(device, minor_number, metadata));
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
    request.add_sub_request(m_device->make_request<AsyncBlockDeviceRequest>(request.request_type(),
        request.block_index() + m_metadata.start_block(), request.block_count(), request.buffer(), request.buffer_size()));
}

KResultOr<size_t> DiskPartition::read(OpenFileDescription& fd, u64 offset, UserOrKernelBuffer& outbuf, size_t len)
{
    unsigned adjust = m_metadata.start_block() * block_size();
    dbgln_if(OFFD_DEBUG, "DiskPartition::read offset={}, adjust={}, len={}", fd.offset(), adjust, len);
    return m_device->read(fd, offset + adjust, outbuf, len);
}

bool DiskPartition::can_read(const OpenFileDescription& fd, size_t offset) const
{
    unsigned adjust = m_metadata.start_block() * block_size();
    dbgln_if(OFFD_DEBUG, "DiskPartition::can_read offset={}, adjust={}", offset, adjust);
    return m_device->can_read(fd, offset + adjust);
}

KResultOr<size_t> DiskPartition::write(OpenFileDescription& fd, u64 offset, const UserOrKernelBuffer& inbuf, size_t len)
{
    unsigned adjust = m_metadata.start_block() * block_size();
    dbgln_if(OFFD_DEBUG, "DiskPartition::write offset={}, adjust={}, len={}", offset, adjust, len);
    return m_device->write(fd, offset + adjust, inbuf, len);
}

bool DiskPartition::can_write(const OpenFileDescription& fd, size_t offset) const
{
    unsigned adjust = m_metadata.start_block() * block_size();
    dbgln_if(OFFD_DEBUG, "DiskPartition::can_write offset={}, adjust={}", offset, adjust);
    return m_device->can_write(fd, offset + adjust);
}

String DiskPartition::device_name() const
{
    // FIXME: Try to not hardcode a maximum of 16 partitions per drive!
    size_t partition_index = minor() % 16;
    return String::formatted("{}{}", m_device->device_name(), partition_index + 1);
}

StringView DiskPartition::class_name() const
{
    return "DiskPartition";
}

}
