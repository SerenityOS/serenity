/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Memory/SharedInodeVMObject.h>

namespace Kernel::Memory {

RefPtr<SharedInodeVMObject> SharedInodeVMObject::try_create_with_inode(Inode& inode)
{
    size_t size = inode.size();
    if (auto shared_vmobject = inode.shared_vmobject())
        return shared_vmobject.release_nonnull();
    auto vmobject = adopt_ref_if_nonnull(new (nothrow) SharedInodeVMObject(inode, size));
    if (!vmobject)
        return nullptr;
    vmobject->inode().set_shared_vmobject(*vmobject);
    return vmobject;
}

RefPtr<VMObject> SharedInodeVMObject::try_clone()
{
    return adopt_ref_if_nonnull(new (nothrow) SharedInodeVMObject(*this));
}

SharedInodeVMObject::SharedInodeVMObject(Inode& inode, size_t size)
    : InodeVMObject(inode, size)
{
}

SharedInodeVMObject::SharedInodeVMObject(SharedInodeVMObject const& other)
    : InodeVMObject(other)
{
}

}
