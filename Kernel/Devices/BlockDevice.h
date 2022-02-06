/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntegralMath.h>
#include <AK/Weakable.h>
#include <Kernel/Devices/Device.h>

namespace Kernel {

class BlockDevice;

class AsyncBlockDeviceRequest final : public AsyncDeviceRequest {
public:
    enum RequestType {
        Read,
        Write
    };
    AsyncBlockDeviceRequest(Device& block_device, RequestType request_type,
        u64 block_index, u32 block_count, const UserOrKernelBuffer& buffer, size_t buffer_size);

    RequestType request_type() const { return m_request_type; }
    u64 block_index() const { return m_block_index; }
    u32 block_count() const { return m_block_count; }
    UserOrKernelBuffer& buffer() { return m_buffer; }
    const UserOrKernelBuffer& buffer() const { return m_buffer; }
    size_t buffer_size() const { return m_buffer_size; }

    virtual void start() override;
    virtual StringView name() const override
    {
        switch (m_request_type) {
        case Read:
            return "BlockDeviceRequest (read)"sv;
        case Write:
            return "BlockDeviceRequest (write)"sv;
        default:
            VERIFY_NOT_REACHED();
        }
    }

private:
    BlockDevice& m_block_device;
    const RequestType m_request_type;
    const u64 m_block_index;
    const u32 m_block_count;
    UserOrKernelBuffer m_buffer;
    const size_t m_buffer_size;
};

class BlockDevice : public Device {
public:
    virtual ~BlockDevice() override;

    size_t block_size() const { return m_block_size; }
    u8 block_size_log() const { return m_block_size_log; }
    virtual bool is_seekable() const override { return true; }

    bool read_block(u64 index, UserOrKernelBuffer&);
    bool write_block(u64 index, const UserOrKernelBuffer&);

    virtual void start_request(AsyncBlockDeviceRequest&) = 0;

protected:
    BlockDevice(MajorNumber major, MinorNumber minor, size_t block_size = PAGE_SIZE)
        : Device(major, minor)
        , m_block_size(block_size)
    {
        // 512 is the minimum sector size in most block devices
        VERIFY(m_block_size >= 512);
        VERIFY(is_power_of_two(m_block_size));
        m_block_size_log = AK::log2(m_block_size);
    }

private:
    virtual bool is_block_device() const final { return true; }

    size_t m_block_size { 0 };
    u8 m_block_size_log { 0 };
};

}
