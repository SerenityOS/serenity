/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/BlockDevicesDirectory.h>

namespace Kernel {

AsyncBlockDeviceRequest::AsyncBlockDeviceRequest(Device& block_device, RequestType request_type, u64 block_index, u32 block_count, UserOrKernelBuffer const& buffer, size_t buffer_size)
    : AsyncDeviceRequest(block_device)
    , m_block_device(static_cast<BlockDevice&>(block_device))
    , m_request_type(request_type)
    , m_block_index(block_index)
    , m_block_count(block_count)
    , m_buffer(buffer)
    , m_buffer_size(buffer_size)
{
}

void AsyncBlockDeviceRequest::start()
{
    m_block_device.start_request(*this);
}

BlockDevice::BlockDevice(MajorAllocation::BlockDeviceFamily block_device_family, MinorNumber minor, size_t block_size)
    : Device(MajorAllocation::block_device_family_to_major_number(block_device_family), minor)
    , m_block_size(block_size)
{
    // 512 is the minimum sector size in most block devices
    VERIFY(m_block_size >= 512);
    VERIFY(is_power_of_two(m_block_size));
    m_block_size_log = AK::log2(m_block_size);
}

BlockDevice::~BlockDevice() = default;

void BlockDevice::after_inserting_add_symlink_to_device_identifier_directory()
{
    VERIFY(m_symlink_sysfs_component);
    SysFSBlockDevicesDirectory::the().devices_list({}).with([&](auto& list) -> void {
        list.append(*m_symlink_sysfs_component);
    });
}

void BlockDevice::before_will_be_destroyed_remove_symlink_from_device_identifier_directory()
{
    VERIFY(m_symlink_sysfs_component);
    SysFSBlockDevicesDirectory::the().devices_list({}).with([&](auto& list) -> void {
        list.remove(*m_symlink_sysfs_component);
    });
}

// FIXME: This method will be eventually removed after all nodes in /sys/dev/block/ are symlinks
void BlockDevice::after_inserting_add_to_device_identifier_directory()
{
    VERIFY(m_sysfs_component);
    SysFSBlockDevicesDirectory::the().devices_list({}).with([&](auto& list) -> void {
        list.append(*m_sysfs_component);
    });
}

// FIXME: This method will be eventually removed after all nodes in /sys/dev/block/ are symlinks
void BlockDevice::before_will_be_destroyed_remove_from_device_identifier_directory()
{
    VERIFY(m_sysfs_component);
    SysFSBlockDevicesDirectory::the().devices_list({}).with([&](auto& list) -> void {
        list.remove(*m_sysfs_component);
    });
}

bool BlockDevice::read_block(u64 index, UserOrKernelBuffer& buffer)
{
    auto read_request_or_error = try_make_request<AsyncBlockDeviceRequest>(AsyncBlockDeviceRequest::Read, index, 1, buffer, m_block_size);
    if (read_request_or_error.is_error()) {
        dbgln("BlockDevice::read_block({}): try_make_request failed", index);
        return false;
    }
    auto read_request = read_request_or_error.release_value();
    switch (read_request->wait().request_result()) {
    case AsyncDeviceRequest::Success:
        return true;
    case AsyncDeviceRequest::Failure:
        dbgln("BlockDevice::read_block({}) IO error", index);
        break;
    case AsyncDeviceRequest::MemoryFault:
        dbgln("BlockDevice::read_block({}) EFAULT", index);
        break;
    case AsyncDeviceRequest::Cancelled:
        dbgln("BlockDevice::read_block({}) cancelled", index);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    return false;
}

bool BlockDevice::write_block(u64 index, UserOrKernelBuffer const& buffer)
{
    auto write_request_or_error = try_make_request<AsyncBlockDeviceRequest>(AsyncBlockDeviceRequest::Write, index, 1, buffer, m_block_size);
    if (write_request_or_error.is_error()) {
        dbgln("BlockDevice::write_block({}): try_make_request failed", index);
        return false;
    }
    auto write_request = write_request_or_error.release_value();
    switch (write_request->wait().request_result()) {
    case AsyncDeviceRequest::Success:
        return true;
    case AsyncDeviceRequest::Failure:
        dbgln("BlockDevice::write_block({}) IO error", index);
        break;
    case AsyncDeviceRequest::MemoryFault:
        dbgln("BlockDevice::write_block({}) EFAULT", index);
        break;
    case AsyncDeviceRequest::Cancelled:
        dbgln("BlockDevice::write_block({}) cancelled", index);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    return false;
}

}
