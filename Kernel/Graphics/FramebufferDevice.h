/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/Graphics/GraphicsDevice.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class FramebufferDevice : public BlockDevice {
    AK_MAKE_ETERNAL
public:
    static NonnullRefPtr<FramebufferDevice> create(const GraphicsDevice&, size_t, PhysicalAddress, size_t, size_t, size_t);

    virtual KResult ioctl(FileDescription&, unsigned request, Userspace<void*> arg) override;
    virtual KResultOr<Memory::Region*> mmap(Process&, FileDescription&, Memory::VirtualRange const&, u64 offset, int prot, bool shared) override;

    // ^Device
    virtual mode_t required_mode() const override { return 0660; }
    virtual String device_name() const override;

    virtual void deactivate_writes();
    virtual void activate_writes();
    size_t framebuffer_size_in_bytes() const;

    virtual ~FramebufferDevice() {};
    KResult initialize();

private:
    // ^File
    virtual StringView class_name() const override { return "FramebufferDevice"; }

    virtual bool can_read(const FileDescription&, size_t) const override final { return true; }
    virtual bool can_write(const FileDescription&, size_t) const override final { return true; }
    virtual void start_request(AsyncBlockDeviceRequest& request) override final { request.complete(AsyncDeviceRequest::Failure); }
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override { return EINVAL; }
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override { return EINVAL; }

    FramebufferDevice(const GraphicsDevice&, size_t, PhysicalAddress, size_t, size_t, size_t);

    PhysicalAddress m_framebuffer_address;
    size_t m_framebuffer_pitch { 0 };
    size_t m_framebuffer_width { 0 };
    size_t m_framebuffer_height { 0 };

    Spinlock m_activation_lock;

    RefPtr<Memory::AnonymousVMObject> m_real_framebuffer_vmobject;
    RefPtr<Memory::AnonymousVMObject> m_swapped_framebuffer_vmobject;
    OwnPtr<Memory::Region> m_real_framebuffer_region;
    OwnPtr<Memory::Region> m_swapped_framebuffer_region;

    bool m_graphical_writes_enabled { true };

    RefPtr<Memory::AnonymousVMObject> m_userspace_real_framebuffer_vmobject;
    Memory::Region* m_userspace_framebuffer_region { nullptr };

    size_t m_y_offset { 0 };
    size_t m_output_port_index;
    NonnullRefPtr<GraphicsDevice> m_graphics_adapter;
};

}
