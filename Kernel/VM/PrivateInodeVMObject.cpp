/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/VM/PrivateInodeVMObject.h>

namespace Kernel {

RefPtr<PrivateInodeVMObject> PrivateInodeVMObject::create_with_inode(Inode& inode)
{
    return adopt_ref_if_nonnull(new PrivateInodeVMObject(inode, inode.size()));
}

RefPtr<VMObject> PrivateInodeVMObject::clone()
{
    return adopt_ref_if_nonnull(new PrivateInodeVMObject(*this));
}

PrivateInodeVMObject::PrivateInodeVMObject(Inode& inode, size_t size)
    : InodeVMObject(inode, size)
{
}

PrivateInodeVMObject::PrivateInodeVMObject(const PrivateInodeVMObject& other)
    : InodeVMObject(other)
{
}

PrivateInodeVMObject::~PrivateInodeVMObject()
{
}

}
