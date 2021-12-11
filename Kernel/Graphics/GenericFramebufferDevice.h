/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Types.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Locking/Mutex.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

class GenericFramebufferDevice : public BlockDevice {
    AK_MAKE_ETERNAL
    friend class DeviceManagement;

public:
    virtual ErrorOr<void> try_to_initialize() = 0;

    virtual void deactivate_writes() = 0;
    virtual void activate_writes() = 0;

    virtual ~GenericFramebufferDevice() = default;

    // ^File
    virtual ErrorOr<Memory::Region*> mmap(Process&, OpenFileDescription&, Memory::VirtualRange const&, u64 offset, int prot, bool shared) override = 0;
    virtual ErrorOr<void> ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg) override final;
    virtual StringView class_name() const override final { return "FramebufferDevice"sv; }

private:
    // ^File
    virtual bool can_read(const OpenFileDescription&, size_t) const override final { return true; }
    virtual bool can_write(const OpenFileDescription&, size_t) const override final { return true; }
    virtual void start_request(AsyncBlockDeviceRequest& request) override final { request.complete(AsyncDeviceRequest::Failure); }
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override { return EINVAL; }
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return EINVAL; }

protected:
    virtual bool multihead_support() const = 0;
    virtual bool flushing_support() const = 0;
    virtual bool partial_flushing_support() const = 0;
    virtual size_t heads_count() const = 0;
    virtual ErrorOr<size_t> buffer_length(size_t head) const = 0;
    virtual ErrorOr<size_t> pitch(size_t head) const = 0;
    virtual ErrorOr<size_t> height(size_t head) const = 0;
    virtual ErrorOr<size_t> width(size_t head) const = 0;
    virtual ErrorOr<size_t> vertical_offset(size_t head) const = 0;
    virtual ErrorOr<bool> vertical_offsetted(size_t head) const = 0;

    virtual ErrorOr<void> set_head_resolution(size_t head, size_t width, size_t height, size_t pitch) = 0;
    virtual ErrorOr<void> set_head_buffer(size_t head, bool second_buffer) = 0;
    virtual ErrorOr<void> flush_head_buffer(size_t head) = 0;
    // FIXME: This method is too much specific to the VirtIO implementation (especially the buffer_index parameter)
    virtual ErrorOr<void> flush_rectangle(size_t buffer_index, FBRect const&) = 0;

    ErrorOr<void> verify_head_index(int head_index) const;

    GenericFramebufferDevice(const GenericGraphicsAdapter&);
    mutable WeakPtr<GenericGraphicsAdapter> m_graphics_adapter;
    mutable Mutex m_heads_lock;
    mutable Mutex m_flushing_lock;
    mutable Mutex m_resolution_lock;
};

}
