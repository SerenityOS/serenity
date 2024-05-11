/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Memory/PrivateInodeVMObject.h>

namespace Kernel::Memory {

ErrorOr<NonnullLockRefPtr<PrivateInodeVMObject>> PrivateInodeVMObject::try_create_with_inode(Inode& inode)
{
    if (inode.size() == 0)
        return EINVAL;
    return try_create_with_inode_and_range(inode, 0, inode.size());
}

ErrorOr<NonnullLockRefPtr<PrivateInodeVMObject>> PrivateInodeVMObject::try_create_with_inode_and_range(Inode& inode, u64 offset, size_t range_size)
{
    // Note: To ensure further allocation of a Region with this VMObject will not complain
    // on "smaller" VMObject than the requested Region, we simply take the max size between both values.
    auto size = max(inode.size(), (offset + range_size));
    VERIFY(size > 0);
    auto new_physical_pages = TRY(VMObject::try_create_physical_pages(size));
    auto dirty_pages = TRY(Bitmap::create(new_physical_pages.size(), false));
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) PrivateInodeVMObject(inode, move(new_physical_pages), move(dirty_pages)));
}

ErrorOr<NonnullLockRefPtr<VMObject>> PrivateInodeVMObject::try_clone()
{
    auto new_physical_pages = TRY(this->try_clone_physical_pages());
    auto dirty_pages = TRY(Bitmap::create(new_physical_pages.size(), false));
    return adopt_nonnull_lock_ref_or_enomem<VMObject>(new (nothrow) PrivateInodeVMObject(*this, move(new_physical_pages), move(dirty_pages)));
}

PrivateInodeVMObject::PrivateInodeVMObject(Inode& inode, FixedArray<RefPtr<PhysicalRAMPage>>&& new_physical_pages, Bitmap dirty_pages)
    : InodeVMObject(inode, move(new_physical_pages), move(dirty_pages))
{
}

PrivateInodeVMObject::PrivateInodeVMObject(PrivateInodeVMObject const& other, FixedArray<RefPtr<PhysicalRAMPage>>&& new_physical_pages, Bitmap dirty_pages)
    : InodeVMObject(other, move(new_physical_pages), move(dirty_pages))
{
}

PrivateInodeVMObject::~PrivateInodeVMObject() = default;

}
