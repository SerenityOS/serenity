/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/Try.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Graphics/FramebufferDevice.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

NonnullRefPtr<FramebufferDevice> FramebufferDevice::create(const GenericGraphicsAdapter& adapter, PhysicalAddress paddr, size_t width, size_t height, size_t pitch)
{
    auto framebuffer_device_or_error = DeviceManagement::try_create_device<FramebufferDevice>(adapter, paddr, width, height, pitch);
    // FIXME: Find a way to propagate errors
    VERIFY(!framebuffer_device_or_error.is_error());
    return framebuffer_device_or_error.release_value();
}

ErrorOr<Memory::Region*> FramebufferDevice::mmap(Process& process, OpenFileDescription&, Memory::VirtualRange const& range, u64 offset, int prot, bool shared)
{
    TRY(process.require_promise(Pledge::video));
    SpinlockLocker lock(m_activation_lock);
    if (!shared)
        return ENODEV;
    if (offset != 0)
        return ENXIO;
    auto framebuffer_length = TRY(buffer_length(0));
    framebuffer_length = TRY(Memory::page_round_up(framebuffer_length));
    if (range.size() != framebuffer_length)
        return EOVERFLOW;

    m_userspace_real_framebuffer_vmobject = TRY(Memory::AnonymousVMObject::try_create_for_physical_range(m_framebuffer_address, framebuffer_length));
    m_real_framebuffer_vmobject = TRY(Memory::AnonymousVMObject::try_create_for_physical_range(m_framebuffer_address, framebuffer_length));
    m_swapped_framebuffer_vmobject = TRY(Memory::AnonymousVMObject::try_create_with_size(framebuffer_length, AllocationStrategy::AllocateNow));
    m_real_framebuffer_region = TRY(MM.allocate_kernel_region_with_vmobject(*m_real_framebuffer_vmobject, framebuffer_length, "Framebuffer", Memory::Region::Access::ReadWrite));
    m_swapped_framebuffer_region = TRY(MM.allocate_kernel_region_with_vmobject(*m_swapped_framebuffer_vmobject, framebuffer_length, "Framebuffer Swap (Blank)", Memory::Region::Access::ReadWrite));

    RefPtr<Memory::VMObject> chosen_vmobject;
    if (m_graphical_writes_enabled) {
        chosen_vmobject = m_real_framebuffer_vmobject;
    } else {
        chosen_vmobject = m_swapped_framebuffer_vmobject;
    }
    m_userspace_framebuffer_region = TRY(process.address_space().allocate_region_with_vmobject(
        range,
        chosen_vmobject.release_nonnull(),
        0,
        "Framebuffer",
        prot,
        shared));
    if (auto result = m_userspace_framebuffer_region->set_write_combine(true); result.is_error())
        dbgln("FramebufferDevice: Failed to enable Write-Combine on Framebuffer: {}", result.error());
    return m_userspace_framebuffer_region;
}

void FramebufferDevice::deactivate_writes()
{
    SpinlockLocker lock(m_activation_lock);
    if (!m_userspace_framebuffer_region)
        return;
    auto framebuffer_length_or_error = buffer_length(0);
    VERIFY(!framebuffer_length_or_error.is_error());
    size_t rounded_framebuffer_length = Memory::page_round_up(framebuffer_length_or_error.release_value()).release_value_but_fixme_should_propagate_errors();
    memcpy(m_swapped_framebuffer_region->vaddr().as_ptr(), m_real_framebuffer_region->vaddr().as_ptr(), rounded_framebuffer_length);
    auto vmobject = m_swapped_framebuffer_vmobject;
    m_userspace_framebuffer_region->set_vmobject(vmobject.release_nonnull());
    m_userspace_framebuffer_region->remap();
    m_graphical_writes_enabled = false;
}
void FramebufferDevice::activate_writes()
{
    SpinlockLocker lock(m_activation_lock);
    if (!m_userspace_framebuffer_region || !m_real_framebuffer_vmobject)
        return;
    // restore the image we had in the void area
    // FIXME: if we happen to have multiple Framebuffers that are writing to that location
    // we will experience glitches...
    auto framebuffer_length_or_error = buffer_length(0);
    VERIFY(!framebuffer_length_or_error.is_error());

    size_t rounded_framebuffer_length = Memory::page_round_up(framebuffer_length_or_error.release_value()).release_value_but_fixme_should_propagate_errors();
    memcpy(m_real_framebuffer_region->vaddr().as_ptr(), m_swapped_framebuffer_region->vaddr().as_ptr(), rounded_framebuffer_length);
    auto vmobject = m_userspace_real_framebuffer_vmobject;
    m_userspace_framebuffer_region->set_vmobject(vmobject.release_nonnull());
    m_userspace_framebuffer_region->remap();
    m_graphical_writes_enabled = true;
}

UNMAP_AFTER_INIT ErrorOr<void> FramebufferDevice::try_to_initialize()
{
    // FIXME: Would be nice to be able to unify this with mmap above, but this
    //        function is UNMAP_AFTER_INIT for the time being.
    auto framebuffer_length = TRY(buffer_length(0));
    framebuffer_length = TRY(Memory::page_round_up(framebuffer_length));
    m_real_framebuffer_vmobject = TRY(Memory::AnonymousVMObject::try_create_for_physical_range(m_framebuffer_address, framebuffer_length));
    m_swapped_framebuffer_vmobject = TRY(Memory::AnonymousVMObject::try_create_with_size(framebuffer_length, AllocationStrategy::AllocateNow));
    m_real_framebuffer_region = TRY(MM.allocate_kernel_region_with_vmobject(*m_real_framebuffer_vmobject, framebuffer_length, "Framebuffer", Memory::Region::Access::ReadWrite));
    m_swapped_framebuffer_region = TRY(MM.allocate_kernel_region_with_vmobject(*m_swapped_framebuffer_vmobject, framebuffer_length, "Framebuffer Swap (Blank)", Memory::Region::Access::ReadWrite));
    return {};
}

UNMAP_AFTER_INIT FramebufferDevice::FramebufferDevice(const GenericGraphicsAdapter& adapter, PhysicalAddress addr, size_t width, size_t height, size_t pitch)
    : GenericFramebufferDevice(adapter)
    , m_framebuffer_address(addr)
    , m_framebuffer_pitch(pitch)
    , m_framebuffer_width(width)
    , m_framebuffer_height(height)
{
    VERIFY(!m_framebuffer_address.is_null());
    VERIFY(m_framebuffer_pitch);
    VERIFY(m_framebuffer_width);
    VERIFY(m_framebuffer_height);
    dbgln("Framebuffer {}: address={}, pitch={}, width={}, height={}", minor(), addr, pitch, width, height);
}

ErrorOr<size_t> FramebufferDevice::buffer_length(size_t head) const
{
    // Note: This FramebufferDevice class doesn't support multihead setup.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally have a value different than 0, assert.
    VERIFY(head == 0);
    SpinlockLocker locker(m_resolution_lock);
    auto adapter = m_graphics_adapter.strong_ref();
    if (!adapter)
        return Error::from_errno(EIO);
    if (adapter->double_framebuffering_capable())
        return m_framebuffer_pitch * m_framebuffer_height * 2;
    return m_framebuffer_pitch * m_framebuffer_height;
}

ErrorOr<size_t> FramebufferDevice::pitch(size_t head) const
{
    // Note: This FramebufferDevice class doesn't support multihead setup.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally have a value different than 0, assert.
    VERIFY(head == 0);
    SpinlockLocker locker(m_resolution_lock);
    return m_framebuffer_pitch;
}
ErrorOr<size_t> FramebufferDevice::height(size_t head) const
{
    // Note: This FramebufferDevice class doesn't support multihead setup.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally have a value different than 0, assert.
    VERIFY(head == 0);
    SpinlockLocker locker(m_resolution_lock);
    return m_framebuffer_height;
}
ErrorOr<size_t> FramebufferDevice::width(size_t head) const
{
    // Note: This FramebufferDevice class doesn't support multihead setup.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally have a value different than 0, assert.
    VERIFY(head == 0);
    SpinlockLocker locker(m_resolution_lock);
    return m_framebuffer_width;
}
ErrorOr<size_t> FramebufferDevice::vertical_offset(size_t head) const
{
    // Note: This FramebufferDevice class doesn't support multihead setup.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally have a value different than 0, assert.
    VERIFY(head == 0);
    SpinlockLocker locker(m_buffer_offset_lock);
    return m_y_offset;
}
ErrorOr<bool> FramebufferDevice::vertical_offsetted(size_t head) const
{
    // Note: This FramebufferDevice class doesn't support multihead setup.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally have a value different than 0, assert.
    VERIFY(head == 0);
    SpinlockLocker locker(m_buffer_offset_lock);
    return m_y_offset == 0 ? 0 : 1;
}

ErrorOr<void> FramebufferDevice::set_head_resolution(size_t head, size_t width, size_t height, size_t)
{
    // Note: This FramebufferDevice class doesn't support multihead setup.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally have a value different than 0, assert.
    VERIFY(head == 0);
    SpinlockLocker buffer_offset_locker(m_buffer_offset_lock);
    SpinlockLocker resolution_locker(m_resolution_lock);
    auto adapter = m_graphics_adapter.strong_ref();
    if (!adapter)
        return Error::from_errno(EIO);
    auto result = adapter->try_to_set_resolution(0, width, height);
    // FIXME: Find a better way to return here a ErrorOr<void>.
    if (!result)
        return Error::from_errno(ENOTSUP);
    m_framebuffer_width = width;
    m_framebuffer_height = height;
    m_framebuffer_pitch = width * sizeof(u32);
    return {};
}
ErrorOr<void> FramebufferDevice::set_head_buffer(size_t head, bool second_buffer)
{
    // Note: This FramebufferDevice class doesn't support multihead setup.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally have a value different than 0, assert.
    VERIFY(head == 0);
    SpinlockLocker locker(m_buffer_offset_lock);
    auto adapter = m_graphics_adapter.strong_ref();
    if (!adapter)
        return Error::from_errno(EIO);
    if (second_buffer) {
        if (!adapter->set_y_offset(0, m_framebuffer_height)) {
            // FIXME: Find a better ErrorOr<void> here.
            return Error::from_errno(ENOTSUP);
        }
        m_y_offset = m_framebuffer_height;
    } else {
        if (!adapter->set_y_offset(0, 0)) {
            // FIXME: Find a better ErrorOr<void> here.
            return Error::from_errno(ENOTSUP);
        }
        m_y_offset = 0;
    }
    return {};
}
ErrorOr<void> FramebufferDevice::flush_head_buffer(size_t)
{
    // Note: This FramebufferDevice class doesn't support flushing.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally reach this code, assert.
    VERIFY_NOT_REACHED();
}
ErrorOr<void> FramebufferDevice::flush_rectangle(size_t, FBRect const&)
{
    // Note: This FramebufferDevice class doesn't support partial flushing.
    // We take care to verify this at the GenericFramebufferDevice::ioctl method
    // so if we happen to accidentally reach this code, assert.
    VERIFY_NOT_REACHED();
}

ErrorOr<ByteBuffer> FramebufferDevice::get_edid(size_t head) const
{
    auto adapter = m_graphics_adapter.strong_ref();
    if (!adapter)
        return Error::from_errno(EIO);
    return adapter->get_edid(head);
}

}
