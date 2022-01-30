/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <AK/StringView.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Storage/StorageDevice.h>
#include <Kernel/Storage/StorageManagement.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

StorageDevice::StorageDevice(MajorNumber major, MinorNumber minor, size_t sector_size, u64 max_addressable_block, NonnullOwnPtr<KString> device_name)
    : BlockDevice(major, minor, sector_size)
    , m_early_storage_device_name(move(device_name))
    , m_max_addressable_block(max_addressable_block)
    , m_blocks_per_page(PAGE_SIZE / block_size())
{
}

StringView StorageDevice::class_name() const
{
    return "StorageDevice"sv;
}

ErrorOr<size_t> StorageDevice::read(OpenFileDescription&, u64 offset, UserOrKernelBuffer& outbuf, size_t len)
{
    u64 index = offset >> block_size_log();
    size_t whole_blocks = len >> block_size_log();
    size_t remaining = len - (whole_blocks << block_size_log());

    // PATAChannel will chuck a wobbly if we try to read more than PAGE_SIZE
    // at a time, because it uses a single page for its DMA buffer.
    if (whole_blocks >= m_blocks_per_page) {
        whole_blocks = m_blocks_per_page;
        remaining = 0;
    }

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
        TRY(outbuf.write(data.data(), pos, remaining));
    }

    return pos + remaining;
}

bool StorageDevice::can_read(const OpenFileDescription&, u64 offset) const
{
    return offset < (max_addressable_block() * block_size());
}

ErrorOr<size_t> StorageDevice::write(OpenFileDescription&, u64 offset, const UserOrKernelBuffer& inbuf, size_t len)
{
    u64 index = offset >> block_size_log();
    size_t whole_blocks = len >> block_size_log();
    size_t remaining = len - (whole_blocks << block_size_log());

    // PATAChannel will chuck a wobbly if we try to write more than PAGE_SIZE
    // at a time, because it uses a single page for its DMA buffer.
    if (whole_blocks >= m_blocks_per_page) {
        whole_blocks = m_blocks_per_page;
        remaining = 0;
    }

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

        TRY(inbuf.read(partial_write_block->data(), pos, remaining));

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

StringView StorageDevice::early_storage_name() const
{
    return m_early_storage_device_name->view();
}

bool StorageDevice::can_write(const OpenFileDescription&, u64 offset) const
{
    return offset < (max_addressable_block() * block_size());
}

ErrorOr<void> StorageDevice::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    switch (request) {
    case STORAGE_DEVICE_GET_SIZE: {
        u64 disk_size = m_max_addressable_block * block_size();
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
