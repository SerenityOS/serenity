/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

//#define STORAGE_DEVICE_DEBUG

#include <AK/Memory.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

StorageDevice::StorageDevice(const StorageController& controller, int major, int minor, size_t sector_size, size_t max_addressable_block)
    : BlockDevice(major, minor, sector_size)
    , m_storage_controller(controller)
    , m_max_addressable_block(max_addressable_block)
{
}

const char* StorageDevice::class_name() const
{
    return "StorageDevice";
}

NonnullRefPtr<StorageController> StorageDevice::controller() const
{
    return m_storage_controller;
}

KResultOr<size_t> StorageDevice::read(FileDescription&, size_t offset, UserOrKernelBuffer& outbuf, size_t len)
{
    unsigned index = offset / block_size();
    u16 whole_blocks = len / block_size();
    ssize_t remaining = len % block_size();

    unsigned blocks_per_page = PAGE_SIZE / block_size();

    // PATAChannel will chuck a wobbly if we try to read more than PAGE_SIZE
    // at a time, because it uses a single page for its DMA buffer.
    if (whole_blocks >= blocks_per_page) {
        whole_blocks = blocks_per_page;
        remaining = 0;
    }

#ifdef STORAGE_DEVICE_DEBUG
    klog() << "StorageDevice::read() index=" << index << " whole_blocks=" << whole_blocks << " remaining=" << remaining;
#endif

    if (whole_blocks > 0) {
        auto read_request = make_request<AsyncBlockDeviceRequest>(AsyncBlockDeviceRequest::Read, index, whole_blocks, outbuf, whole_blocks * block_size());
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
        auto data = ByteBuffer::create_uninitialized(block_size());
        auto data_buffer = UserOrKernelBuffer::for_kernel_buffer(data.data());
        auto read_request = make_request<AsyncBlockDeviceRequest>(AsyncBlockDeviceRequest::Read, index + whole_blocks, 1, data_buffer, block_size());
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
            ASSERT_NOT_REACHED();
        default:
            break;
        }
        if (!outbuf.write(data.data(), pos, remaining))
            return EFAULT;
    }

    return pos + remaining;
}

bool StorageDevice::can_read(const FileDescription&, size_t offset) const
{
    return offset < (max_addressable_block() * block_size());
}

KResultOr<size_t> StorageDevice::write(FileDescription&, size_t offset, const UserOrKernelBuffer& inbuf, size_t len)
{
    unsigned index = offset / block_size();
    u16 whole_blocks = len / block_size();
    ssize_t remaining = len % block_size();

    unsigned blocks_per_page = PAGE_SIZE / block_size();

    // PATAChannel will chuck a wobbly if we try to write more than PAGE_SIZE
    // at a time, because it uses a single page for its DMA buffer.
    if (whole_blocks >= blocks_per_page) {
        whole_blocks = blocks_per_page;
        remaining = 0;
    }

#ifdef STORAGE_DEVICE_DEBUG
    klog() << "StorageDevice::write() index=" << index << " whole_blocks=" << whole_blocks << " remaining=" << remaining;
#endif

    if (whole_blocks > 0) {
        auto write_request = make_request<AsyncBlockDeviceRequest>(AsyncBlockDeviceRequest::Write, index, whole_blocks, inbuf, whole_blocks * block_size());
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
        auto data = ByteBuffer::create_zeroed(block_size());
        auto data_buffer = UserOrKernelBuffer::for_kernel_buffer(data.data());

        {
            auto read_request = make_request<AsyncBlockDeviceRequest>(AsyncBlockDeviceRequest::Read, index + whole_blocks, 1, data_buffer, block_size());
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
                ASSERT_NOT_REACHED();
            default:
                break;
            }
        }

        if (!inbuf.read(data.data(), pos, remaining))
            return EFAULT;

        {
            auto write_request = make_request<AsyncBlockDeviceRequest>(AsyncBlockDeviceRequest::Write, index + whole_blocks, 1, data_buffer, block_size());
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
                ASSERT_NOT_REACHED();
            default:
                break;
            }
        }
    }

    return pos + remaining;
}

bool StorageDevice::can_write(const FileDescription&, size_t offset) const
{
    return offset < (max_addressable_block() * block_size());
}

}
