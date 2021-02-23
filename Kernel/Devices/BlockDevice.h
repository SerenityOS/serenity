/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <Kernel/Devices/Device.h>

namespace Kernel {

class BlockDevice;

class AsyncBlockDeviceRequest : public AsyncDeviceRequest {
public:
    enum RequestType {
        Read,
        Write
    };
    AsyncBlockDeviceRequest(Device& block_device, RequestType request_type,
        u32 block_index, u32 block_count, const UserOrKernelBuffer& buffer, size_t buffer_size);

    RequestType request_type() const { return m_request_type; }
    u32 block_index() const { return m_block_index; }
    u32 block_count() const { return m_block_count; }
    UserOrKernelBuffer& buffer() { return m_buffer; }
    const UserOrKernelBuffer& buffer() const { return m_buffer; }
    size_t buffer_size() const { return m_buffer_size; }

    virtual void start() override;
    virtual const char* name() const override
    {
        switch (m_request_type) {
        case Read:
            return "BlockDeviceRequest (read)";
        case Write:
            return "BlockDeviceRequest (write)";
        default:
            VERIFY_NOT_REACHED();
        }
    }

private:
    BlockDevice& m_block_device;
    const RequestType m_request_type;
    const u32 m_block_index;
    const u32 m_block_count;
    UserOrKernelBuffer m_buffer;
    const size_t m_buffer_size;
};

class BlockDevice : public Device {
public:
    virtual ~BlockDevice() override;

    size_t block_size() const { return m_block_size; }
    virtual bool is_seekable() const override { return true; }

    bool read_block(unsigned index, UserOrKernelBuffer&);
    bool write_block(unsigned index, const UserOrKernelBuffer&);

    virtual void start_request(AsyncBlockDeviceRequest&) = 0;

protected:
    BlockDevice(unsigned major, unsigned minor, size_t block_size = PAGE_SIZE)
        : Device(major, minor)
        , m_block_size(block_size)
    {
    }

private:
    virtual bool is_block_device() const final { return true; }

    size_t m_block_size { 0 };
};

}
