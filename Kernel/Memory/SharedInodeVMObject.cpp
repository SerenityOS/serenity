/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/SharedInodeVMObject.h>

namespace Kernel::Memory {

ErrorOr<NonnullRefPtr<SharedInodeVMObject>> SharedInodeVMObject::try_create_with_inode(Inode& inode)
{
    size_t size = inode.size();
    if (auto shared_vmobject = inode.shared_vmobject())
        return shared_vmobject.release_nonnull();
    auto new_physical_pages = TRY(VMObject::try_create_physical_pages(size));
    auto vmobject = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) SharedInodeVMObject(inode, move(new_physical_pages))));
    vmobject->inode().set_shared_vmobject(*vmobject);
    return vmobject;
}

ErrorOr<NonnullRefPtr<VMObject>> SharedInodeVMObject::try_clone()
{
    auto new_physical_pages = TRY(this->try_clone_physical_pages());
    return adopt_nonnull_ref_or_enomem<VMObject>(new (nothrow) SharedInodeVMObject(*this, move(new_physical_pages)));
}

SharedInodeVMObject::SharedInodeVMObject(Inode& inode, FixedArray<RefPtr<PhysicalPage>>&& new_physical_pages)
    : InodeVMObject(inode, move(new_physical_pages))
{
}

SharedInodeVMObject::SharedInodeVMObject(SharedInodeVMObject const& other, FixedArray<RefPtr<PhysicalPage>>&& new_physical_pages)
    : InodeVMObject(other, move(new_physical_pages))
{
}

ErrorOr<void> SharedInodeVMObject::sync(off_t offset_in_pages, size_t pages)
{
    SpinlockLocker locker(m_lock);

    size_t highest_page_to_flush = min(page_count(), offset_in_pages + pages);

    for (size_t page_index = offset_in_pages; page_index < highest_page_to_flush; ++page_index) {
        auto& physical_page = m_physical_pages[page_index];
        if (!physical_page)
            continue;

        u8 page_buffer[PAGE_SIZE];
        MM.copy_physical_page(*physical_page, page_buffer);

        TRY(m_inode->write_bytes(page_index * PAGE_SIZE, PAGE_SIZE, UserOrKernelBuffer::for_kernel_buffer(page_buffer), nullptr));
    }

    return {};
}

}
