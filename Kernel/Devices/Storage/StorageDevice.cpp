/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/API/Ioctl.h>
#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/Devices/Storage/StorageManagement.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/BlockDevicesDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/DeviceIdentifiers/SymbolicLinkDeviceComponent.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/DeviceDirectory.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Devices/Storage/Directory.h>

namespace Kernel {

StorageDevice::StorageDevice(LUNAddress logical_unit_number_address, u32 hardware_relative_controller_id, size_t sector_size, u64 max_addressable_block)
    : BlockDevice(MajorAllocation::BlockDeviceFamily::Storage, StorageManagement::generate_storage_minor_number(), sector_size)
    , m_logical_unit_number_address(logical_unit_number_address)
    , m_hardware_relative_controller_id(hardware_relative_controller_id)
    , m_max_addressable_block(max_addressable_block)
    , m_blocks_per_page(PAGE_SIZE / block_size())
{
}

ErrorOr<void> StorageDevice::after_inserting()
{
    auto sysfs_storage_device_directory = StorageDeviceSysFSDirectory::create(SysFSStorageDirectory::the(), *this);
    m_sysfs_device_directory = sysfs_storage_device_directory;
    SysFSStorageDirectory::the().plug({}, *sysfs_storage_device_directory);
    VERIFY(!m_symlink_sysfs_component);
    auto sys_fs_component = TRY(SysFSSymbolicLinkDeviceComponent::try_create(SysFSBlockDevicesDirectory::the(), *this, *m_sysfs_device_directory));
    m_symlink_sysfs_component = sys_fs_component;
    after_inserting_add_symlink_to_device_identifier_directory();
    after_inserting_add_to_device_management();
    return {};
}

void StorageDevice::will_be_destroyed()
{
    // NOTE: We check if m_symlink_sysfs_component is not null, because if we failed
    // in StorageDevice::after_inserting(), then that method will not set m_symlink_sysfs_component.
    if (m_symlink_sysfs_component) {
        before_will_be_destroyed_remove_symlink_from_device_identifier_directory();
        m_symlink_sysfs_component.clear();
    }
    SysFSStorageDirectory::the().unplug({}, *m_sysfs_device_directory);
    before_will_be_destroyed_remove_from_device_management();
}

StringView StorageDevice::class_name() const
{
    return "StorageDevice"sv;
}

StringView StorageDevice::command_set_to_string_view() const
{
    switch (command_set()) {
    case CommandSet::SCSI:
        return "scsi"sv;
    case CommandSet::ATA:
        return "ata"sv;
    case CommandSet::NVMe:
        return "nvme"sv;
    case CommandSet::SD:
        return "sd"sv;
    default:
        break;
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<size_t> StorageDevice::read(OpenFileDescription&, u64 offset, UserOrKernelBuffer& outbuf, size_t len)
{
    // NOTE: The last available offset is actually just after the last addressable block.
    if (offset >= (max_mathematical_addressable_block() * block_size()))
        return 0;
    size_t nread = min(static_cast<size_t>((max_mathematical_addressable_block() * block_size()) - offset), len);
    u64 index = offset >> block_size_log();
    off_t offset_within_block = 0;
    size_t whole_blocks = nread >> block_size_log();
    size_t remaining = nread - (whole_blocks << block_size_log());

    // PATAChannel will chuck a wobbly if we try to read more than PAGE_SIZE
    // at a time, because it uses a single page for its DMA buffer.
    if (whole_blocks >= m_blocks_per_page) {
        whole_blocks = m_blocks_per_page;
        remaining = 0;
    }

    if (nread < block_size())
        offset_within_block = offset - (index << block_size_log());

    dbgln_if(STORAGE_DEVICE_DEBUG, "StorageDevice::read() index={}, whole_blocks={}, remaining={}", index, whole_blocks, remaining);

    if (whole_blocks > 0) {
        auto read_request = TRY(try_make_request<AsyncBlockDeviceRequest>(AsyncBlockDeviceRequest::Read, index, whole_blocks, outbuf, whole_blocks * block_size()));
        auto result = read_request->wait();
        if (result.wait_result().was_interrupted())
            return EINTR;
        switch (result.request_result()) {
        case AsyncDeviceRequest::Failure:
        case AsyncDeviceRequest::Cancelled:
            return EIO;
        case AsyncDeviceRequest::MemoryFault:
            return EFAULT;
        default:
            break;
        }
    }

    off_t pos = whole_blocks * block_size();

    if (remaining > 0) {
        auto data = TRY(ByteBuffer::create_uninitialized(block_size()));
        auto data_buffer = UserOrKernelBuffer::for_kernel_buffer(data.data());
        auto read_request = TRY(try_make_request<AsyncBlockDeviceRequest>(AsyncBlockDeviceRequest::Read, index + whole_blocks, 1, data_buffer, block_size()));
        auto result = read_request->wait();
        if (result.wait_result().was_interrupted())
            return EINTR;
        switch (result.request_result()) {
        case AsyncDeviceRequest::Failure:
            return pos;
        case AsyncDeviceRequest::Cancelled:
            return EIO;
        case AsyncDeviceRequest::MemoryFault:
            // This should never happen, we're writing to a kernel buffer!
            VERIFY_NOT_REACHED();
        default:
            break;
        }
        TRY(outbuf.write(data.offset_pointer(offset_within_block), pos, remaining));
    }

    return pos + remaining;
}

ErrorOr<size_t> StorageDevice::write(OpenFileDescription&, u64 offset, UserOrKernelBuffer const& inbuf, size_t len)
{
    // NOTE: The last available offset is actually just after the last addressable block.
    if (offset >= (max_mathematical_addressable_block() * block_size()))
        return Error::from_errno(ENOSPC);
    size_t nwrite = min(static_cast<size_t>((max_mathematical_addressable_block() * block_size()) - offset), len);
    u64 index = offset >> block_size_log();
    off_t offset_within_block = 0;
    size_t whole_blocks = nwrite >> block_size_log();
    size_t remaining = nwrite - (whole_blocks << block_size_log());

    // PATAChannel will chuck a wobbly if we try to write more than PAGE_SIZE
    // at a time, because it uses a single page for its DMA buffer.
    if (whole_blocks >= m_blocks_per_page) {
        whole_blocks = m_blocks_per_page;
        remaining = 0;
    }

    if (nwrite < block_size())
        offset_within_block = offset - (index << block_size_log());

    // We try to allocate the temporary block buffer for partial writes *before* we start any full block writes,
    // to try and prevent partial writes
    Optional<ByteBuffer> partial_write_block;
    if (remaining > 0)
        partial_write_block = TRY(ByteBuffer::create_zeroed(block_size()));

    dbgln_if(STORAGE_DEVICE_DEBUG, "StorageDevice::write() index={}, whole_blocks={}, remaining={}", index, whole_blocks, remaining);

    if (whole_blocks > 0) {
        auto write_request = TRY(try_make_request<AsyncBlockDeviceRequest>(AsyncBlockDeviceRequest::Write, index, whole_blocks, inbuf, whole_blocks * block_size()));
        auto result = write_request->wait();
        if (result.wait_result().was_interrupted())
            return EINTR;
        switch (result.request_result()) {
        case AsyncDeviceRequest::Failure:
        case AsyncDeviceRequest::Cancelled:
            return EIO;
        case AsyncDeviceRequest::MemoryFault:
            return EFAULT;
        default:
            break;
        }
    }

    off_t pos = whole_blocks * block_size();

    // since we can only write in block_size() increments, if we want to do a
    // partial write, we have to read the block's content first, modify it,
    // then write the whole block back to the disk.
    if (remaining > 0) {
        auto data_buffer = UserOrKernelBuffer::for_kernel_buffer(partial_write_block->data());
        {
            auto read_request = TRY(try_make_request<AsyncBlockDeviceRequest>(AsyncBlockDeviceRequest::Read, index + whole_blocks, 1, data_buffer, block_size()));
            auto result = read_request->wait();
            if (result.wait_result().was_interrupted())
                return EINTR;
            switch (result.request_result()) {
            case AsyncDeviceRequest::Failure:
                return pos;
            case AsyncDeviceRequest::Cancelled:
                return EIO;
            case AsyncDeviceRequest::MemoryFault:
                // This should never happen, we're writing to a kernel buffer!
                VERIFY_NOT_REACHED();
            default:
                break;
            }
        }

        TRY(inbuf.read(partial_write_block->offset_pointer(offset_within_block), pos, remaining));

        {
            auto write_request = TRY(try_make_request<AsyncBlockDeviceRequest>(AsyncBlockDeviceRequest::Write, index + whole_blocks, 1, data_buffer, block_size()));
            auto result = write_request->wait();
            if (result.wait_result().was_interrupted())
                return EINTR;
            switch (result.request_result()) {
            case AsyncDeviceRequest::Failure:
                return pos;
            case AsyncDeviceRequest::Cancelled:
                return EIO;
            case AsyncDeviceRequest::MemoryFault:
                // This should never happen, we're writing to a kernel buffer!
                VERIFY_NOT_REACHED();
            default:
                break;
            }
        }
    }

    return pos + remaining;
}

ErrorOr<void> StorageDevice::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    switch (request) {
    case STORAGE_DEVICE_GET_SIZE: {
        u64 disk_size = max_mathematical_addressable_block() * block_size();
        return copy_to_user(static_ptr_cast<u64*>(arg), &disk_size);
        break;
    }
    case STORAGE_DEVICE_GET_BLOCK_SIZE: {
        size_t size = block_size();
        return copy_to_user(static_ptr_cast<size_t*>(arg), &size);
        break;
    }
    default:
        return EINVAL;
    }
}

}
