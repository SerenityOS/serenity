/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/SharedFramebufferVMObject.h>

namespace Kernel::Memory {

ErrorOr<NonnullLockRefPtr<SharedFramebufferVMObject>> SharedFramebufferVMObject::try_create_for_physical_range(PhysicalAddress paddr, size_t size)
{
    auto real_framebuffer_vmobject = TRY(AnonymousVMObject::try_create_for_physical_range(paddr, size));
    auto new_physical_pages = TRY(VMObject::try_create_physical_pages(size));
    auto committed_pages = TRY(MM.commit_physical_pages(ceil_div(size, static_cast<size_t>(PAGE_SIZE))));
    auto vm_object = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) SharedFramebufferVMObject(move(new_physical_pages), move(committed_pages), real_framebuffer_vmobject)));
    TRY(vm_object->create_fake_writes_framebuffer_vm_object());
    TRY(vm_object->create_real_writes_framebuffer_vm_object());
    return vm_object;
}

ErrorOr<NonnullLockRefPtr<SharedFramebufferVMObject>> SharedFramebufferVMObject::try_create_at_arbitrary_physical_range(size_t size)
{
    auto real_framebuffer_vmobject = TRY(AnonymousVMObject::try_create_with_size(size, AllocationStrategy::AllocateNow));
    auto new_physical_pages = TRY(VMObject::try_create_physical_pages(size));
    auto committed_pages = TRY(MM.commit_physical_pages(ceil_div(size, static_cast<size_t>(PAGE_SIZE))));
    auto vm_object = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) SharedFramebufferVMObject(move(new_physical_pages), move(committed_pages), real_framebuffer_vmobject)));
    TRY(vm_object->create_fake_writes_framebuffer_vm_object());
    TRY(vm_object->create_real_writes_framebuffer_vm_object());
    return vm_object;
}

ErrorOr<NonnullLockRefPtr<SharedFramebufferVMObject::FakeWritesFramebufferVMObject>> SharedFramebufferVMObject::FakeWritesFramebufferVMObject::try_create(Badge<SharedFramebufferVMObject>, SharedFramebufferVMObject const& parent_object)
{
    auto new_physical_pages = TRY(VMObject::try_create_physical_pages(0));
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) FakeWritesFramebufferVMObject(parent_object, move(new_physical_pages)));
}

ErrorOr<NonnullLockRefPtr<SharedFramebufferVMObject::RealWritesFramebufferVMObject>> SharedFramebufferVMObject::RealWritesFramebufferVMObject::try_create(Badge<SharedFramebufferVMObject>, SharedFramebufferVMObject const& parent_object)
{
    auto new_physical_pages = TRY(VMObject::try_create_physical_pages(0));
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) RealWritesFramebufferVMObject(parent_object, move(new_physical_pages)));
}

ErrorOr<void> SharedFramebufferVMObject::create_fake_writes_framebuffer_vm_object()
{
    m_fake_writes_framebuffer_vmobject = TRY(FakeWritesFramebufferVMObject::try_create({}, *this));
    return {};
}

ErrorOr<void> SharedFramebufferVMObject::create_real_writes_framebuffer_vm_object()
{
    m_real_writes_framebuffer_vmobject = TRY(RealWritesFramebufferVMObject::try_create({}, *this));
    return {};
}

Span<RefPtr<PhysicalRAMPage>> SharedFramebufferVMObject::real_framebuffer_physical_pages()
{
    return m_real_framebuffer_vmobject->physical_pages();
}
ReadonlySpan<RefPtr<PhysicalRAMPage>> SharedFramebufferVMObject::real_framebuffer_physical_pages() const
{
    return m_real_framebuffer_vmobject->physical_pages();
}

Span<RefPtr<PhysicalRAMPage>> SharedFramebufferVMObject::fake_sink_framebuffer_physical_pages()
{
    return m_physical_pages.span();
}

ReadonlySpan<RefPtr<PhysicalRAMPage>> SharedFramebufferVMObject::fake_sink_framebuffer_physical_pages() const
{
    return m_physical_pages.span();
}

void SharedFramebufferVMObject::switch_to_fake_sink_framebuffer_writes(Badge<Kernel::DisplayConnector>)
{
    SpinlockLocker locker(m_writes_state_lock);
    m_writes_are_faked = true;
    remap_regions();
}
void SharedFramebufferVMObject::switch_to_real_framebuffer_writes(Badge<Kernel::DisplayConnector>)
{
    SpinlockLocker locker(m_writes_state_lock);
    m_writes_are_faked = false;
    remap_regions();
}

ReadonlySpan<RefPtr<PhysicalRAMPage>> SharedFramebufferVMObject::physical_pages() const
{
    SpinlockLocker locker(m_writes_state_lock);
    if (m_writes_are_faked)
        return VMObject::physical_pages();
    return m_real_framebuffer_vmobject->physical_pages();
}
Span<RefPtr<PhysicalRAMPage>> SharedFramebufferVMObject::physical_pages()
{
    SpinlockLocker locker(m_writes_state_lock);
    if (m_writes_are_faked)
        return VMObject::physical_pages();
    return m_real_framebuffer_vmobject->physical_pages();
}

SharedFramebufferVMObject::SharedFramebufferVMObject(FixedArray<RefPtr<PhysicalRAMPage>>&& new_physical_pages, CommittedPhysicalPageSet committed_pages, AnonymousVMObject& real_framebuffer_vmobject)
    : VMObject(move(new_physical_pages))
    , m_real_framebuffer_vmobject(real_framebuffer_vmobject)
    , m_committed_pages(move(committed_pages))
{
    // Allocate all pages right now. We know we can get all because we committed the amount needed
    for (size_t i = 0; i < page_count(); ++i)
        m_physical_pages[i] = m_committed_pages.take_one();
}

}
