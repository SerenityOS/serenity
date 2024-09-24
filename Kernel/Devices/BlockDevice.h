/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntegralMath.h>
#include <Kernel/API/MajorNumberAllocation.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Library/LockWeakable.h>

namespace Kernel {

class AsyncBlockDeviceRequest;

class BlockDevice : public Device {
public:
    virtual ~BlockDevice() override;

    size_t block_size() const { return m_block_size; }
    u8 block_size_log() const { return m_block_size_log; }
    virtual bool is_seekable() const override { return true; }

    bool read_block(u64 index, UserOrKernelBuffer&);
    bool write_block(u64 index, UserOrKernelBuffer const&);

    virtual void start_request(AsyncBlockDeviceRequest&) = 0;

protected:
    BlockDevice(MajorAllocation::BlockDeviceFamily, MinorNumber minor, size_t block_size = PAGE_SIZE);

protected:
    virtual bool is_block_device() const final { return true; }

    virtual void after_inserting_add_symlink_to_device_identifier_directory() override final;
    virtual void before_will_be_destroyed_remove_symlink_from_device_identifier_directory() override final;

private:
    // FIXME: These methods will be eventually removed after all nodes in /sys/dev/block/ are symlinks
    virtual void after_inserting_add_to_device_identifier_directory() override final;
    virtual void before_will_be_destroyed_remove_from_device_identifier_directory() override final;

    size_t m_block_size { 0 };
    u8 m_block_size_log { 0 };
};

class AsyncBlockDeviceRequest final : public AsyncDeviceRequest {
public:
    enum RequestType {
        Read,
        Write
    };
    AsyncBlockDeviceRequest(Device& block_device, RequestType request_type,
        u64 block_index, u32 block_count, UserOrKernelBuffer const& buffer, size_t buffer_size);

    RequestType request_type() const { return m_request_type; }
    u64 block_index() const { return m_block_index; }
    u32 block_count() const { return m_block_count; }
    size_t block_size() const { return m_block_device.block_size(); }
    UserOrKernelBuffer& buffer() { return m_buffer; }
    UserOrKernelBuffer const& buffer() const { return m_buffer; }
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
    RequestType const m_request_type;
    u64 const m_block_index;
    u32 const m_block_count;
    UserOrKernelBuffer m_buffer;
    size_t const m_buffer_size;
};

}
