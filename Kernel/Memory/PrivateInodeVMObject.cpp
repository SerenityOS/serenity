/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Memory/PrivateInodeVMObject.h>

namespace Kernel::Memory {

ErrorOr<NonnullRefPtr<PrivateInodeVMObject>> PrivateInodeVMObject::try_create_with_inode(Inode& inode)
{
    auto new_physical_pages = TRY(VMObject::try_create_physical_pages(inode.size()));
    auto dirty_pages = TRY(Bitmap::try_create(new_physical_pages.size(), false));
    return adopt_nonnull_ref_or_enomem(new (nothrow) PrivateInodeVMObject(inode, move(new_physical_pages), move(dirty_pages)));
}

ErrorOr<NonnullRefPtr<VMObject>> PrivateInodeVMObject::try_clone()
{
    auto new_physical_pages = TRY(this->try_clone_physical_pages());
    auto dirty_pages = TRY(Bitmap::try_create(new_physical_pages.size(), false));
    return adopt_nonnull_ref_or_enomem<VMObject>(new (nothrow) PrivateInodeVMObject(*this, move(new_physical_pages), move(dirty_pages)));
}

PrivateInodeVMObject::PrivateInodeVMObject(Inode& inode, FixedArray<RefPtr<PhysicalPage>>&& new_physical_pages, Bitmap dirty_pages)
    : InodeVMObject(inode, move(new_physical_pages), move(dirty_pages))
{
}

PrivateInodeVMObject::PrivateInodeVMObject(PrivateInodeVMObject const& other, FixedArray<RefPtr<PhysicalPage>>&& new_physical_pages, Bitmap dirty_pages)
    : InodeVMObject(other, move(new_physical_pages), move(dirty_pages))
{
}

PrivateInodeVMObject::~PrivateInodeVMObject()
{
}

}
