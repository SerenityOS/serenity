/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Graphics/GenericFramebufferDevice.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class FramebufferDevice final : public GenericFramebufferDevice {
    friend class DeviceManagement;

public:
    static NonnullRefPtr<FramebufferDevice> create(const GenericGraphicsAdapter&, PhysicalAddress, size_t, size_t, size_t);

    virtual ErrorOr<Memory::Region*> mmap(Process&, OpenFileDescription&, Memory::VirtualRange const&, u64 offset, int prot, bool shared) override;

    virtual void deactivate_writes() override;
    virtual void activate_writes() override;

    virtual ErrorOr<void> try_to_initialize() override;

    virtual bool multihead_support() const override { return false; }
    virtual bool flushing_support() const override { return false; }
    virtual bool partial_flushing_support() const override { return false; }
    virtual size_t heads_count() const override { return 1; }
    virtual ErrorOr<size_t> buffer_length(size_t head) const override;
    virtual ErrorOr<size_t> pitch(size_t head) const override;
    virtual ErrorOr<size_t> height(size_t head) const override;
    virtual ErrorOr<size_t> width(size_t head) const override;
    virtual ErrorOr<size_t> vertical_offset(size_t head) const override;
    virtual ErrorOr<bool> vertical_offsetted(size_t head) const override;

    virtual ErrorOr<ByteBuffer> get_edid(size_t head) const override;

private:
    virtual ErrorOr<void> set_head_resolution(size_t head, size_t width, size_t height, size_t pitch) override;
    virtual ErrorOr<void> set_head_buffer(size_t head, bool second_buffer) override;
    virtual ErrorOr<void> flush_head_buffer(size_t head) override;
    virtual ErrorOr<void> flush_rectangle(size_t head, FBRect const&) override;

    FramebufferDevice(const GenericGraphicsAdapter&, PhysicalAddress, size_t, size_t, size_t);

    PhysicalAddress m_framebuffer_address;
    size_t m_framebuffer_pitch { 0 };
    size_t m_framebuffer_width { 0 };
    size_t m_framebuffer_height { 0 };

    Spinlock m_activation_lock;
    mutable Spinlock m_buffer_offset_lock;

    RefPtr<Memory::AnonymousVMObject> m_real_framebuffer_vmobject;
    RefPtr<Memory::AnonymousVMObject> m_swapped_framebuffer_vmobject;
    OwnPtr<Memory::Region> m_real_framebuffer_region;
    OwnPtr<Memory::Region> m_swapped_framebuffer_region;

    bool m_graphical_writes_enabled { true };

    RefPtr<Memory::AnonymousVMObject> m_userspace_real_framebuffer_vmobject;
    Memory::Region* m_userspace_framebuffer_region { nullptr };

    size_t m_y_offset { 0 };
};

}
