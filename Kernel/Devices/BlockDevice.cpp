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

#include <Kernel/Devices/BlockDevice.h>

namespace Kernel {

AsyncBlockDeviceRequest::AsyncBlockDeviceRequest(Device& block_device, RequestType request_type, u32 block_index, u32 block_count, const UserOrKernelBuffer& buffer, size_t buffer_size)
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

BlockDevice::~BlockDevice()
{
}

bool BlockDevice::read_block(unsigned index, UserOrKernelBuffer& buffer)
{
    auto read_request = make_request<AsyncBlockDeviceRequest>(AsyncBlockDeviceRequest::Read, index, 1, buffer, 512);
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

bool BlockDevice::write_block(unsigned index, const UserOrKernelBuffer& buffer)
{
    auto write_request = make_request<AsyncBlockDeviceRequest>(AsyncBlockDeviceRequest::Write, index, 1, buffer, 512);
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
