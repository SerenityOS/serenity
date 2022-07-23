/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Logical/Partition/DeviceDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Logical/Partition/Directory.h>
#include <Kernel/Storage/DiskPartition.h>

namespace Kernel {

NonnullRefPtr<DiskPartition> DiskPartition::create(BlockDevice& device, MinorNumber minor_number, Partition::DiskPartitionMetadata metadata)
{
    auto partition_or_error = DeviceManagement::try_create_device<DiskPartition>(device, minor_number, metadata);
    // FIXME: Find a way to propagate errors
    VERIFY(!partition_or_error.is_error());
    return partition_or_error.release_value();
}

DiskPartition::DiskPartition(BlockDevice& device, MinorNumber minor_number, Partition::DiskPartitionMetadata metadata)
    : BlockDevice(100, minor_number, device.block_size())
    , m_device(device)
    , m_metadata(metadata)
{
}

void DiskPartition::after_inserting()
{
    after_inserting_add_to_device_management();
    auto parent_device = m_device.strong_ref();
    if (!parent_device) {
        // Note: If for some odd reason there's no parent device, this device will probably be erased soon
        // so let's exit this function without doing anything.
        return;
    }
    auto parent_sysfs_device_identifier_component = parent_device->sysfs_device_identifier_component();
    auto sysfs_partition_device_directory = PartitionDeviceSysFSDirectory::create(SysFSStoragePartitionDevicesDirectory::the(), *this, *parent_sysfs_device_identifier_component);
    m_sysfs_device_directory = sysfs_partition_device_directory;
    SysFSStoragePartitionDevicesDirectory::the().plug({}, *sysfs_partition_device_directory);
    VERIFY(!m_symlink_sysfs_component);
    auto sys_fs_component = MUST(SysFSSymbolicLinkDeviceComponent::try_create(SysFSBlockDevicesDirectory::the(), *this, *m_sysfs_device_directory));
    m_symlink_sysfs_component = sys_fs_component;
    after_inserting_add_symlink_to_device_identifier_directory();
}

void DiskPartition::will_be_destroyed()
{
    VERIFY(m_symlink_sysfs_component);
    before_will_be_destroyed_remove_symlink_from_device_identifier_directory();
    m_symlink_sysfs_component.clear();
    SysFSStoragePartitionDevicesDirectory::the().unplug({}, *m_sysfs_device_directory);
    before_will_be_destroyed_remove_from_device_management();
}

DiskPartition::~DiskPartition() = default;

Partition::DiskPartitionMetadata const& DiskPartition::metadata() const
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
    u64 adjust = m_metadata.start_block() * block_size();
    dbgln_if(OFFD_DEBUG, "DiskPartition::read offset={}, adjust={}, len={}", fd.offset(), adjust, len);
    return m_device.strong_ref()->read(fd, offset + adjust, outbuf, len);
}

bool DiskPartition::can_read(OpenFileDescription const& fd, u64 offset) const
{
    u64 adjust = m_metadata.start_block() * block_size();
    dbgln_if(OFFD_DEBUG, "DiskPartition::can_read offset={}, adjust={}", offset, adjust);
    return m_device.strong_ref()->can_read(fd, offset + adjust);
}

ErrorOr<size_t> DiskPartition::write(OpenFileDescription& fd, u64 offset, UserOrKernelBuffer const& inbuf, size_t len)
{
    u64 adjust = m_metadata.start_block() * block_size();
    dbgln_if(OFFD_DEBUG, "DiskPartition::write offset={}, adjust={}, len={}", offset, adjust, len);
    return m_device.strong_ref()->write(fd, offset + adjust, inbuf, len);
}

bool DiskPartition::can_write(OpenFileDescription const& fd, u64 offset) const
{
    u64 adjust = m_metadata.start_block() * block_size();
    dbgln_if(OFFD_DEBUG, "DiskPartition::can_write offset={}, adjust={}", offset, adjust);
    return m_device.strong_ref()->can_write(fd, offset + adjust);
}

StringView DiskPartition::class_name() const
{
    return "DiskPartition"sv;
}

}
