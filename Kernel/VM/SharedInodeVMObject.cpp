/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/VM/SharedInodeVMObject.h>

namespace Kernel {

NonnullRefPtr<SharedInodeVMObject> SharedInodeVMObject::create_with_inode(Inode& inode)
{
    size_t size = inode.size();
    if (auto shared_vmobject = inode.shared_vmobject())
        return shared_vmobject.release_nonnull();
    auto vmobject = adopt_ref(*new SharedInodeVMObject(inode, size));
    vmobject->inode().set_shared_vmobject(*vmobject);
    return vmobject;
}

RefPtr<VMObject> SharedInodeVMObject::clone()
{
    return adopt_ref(*new SharedInodeVMObject(*this));
}

SharedInodeVMObject::SharedInodeVMObject(Inode& inode, size_t size)
    : InodeVMObject(inode, size)
{
}

SharedInodeVMObject::SharedInodeVMObject(const SharedInodeVMObject& other)
    : InodeVMObject(other)
{
}

}
