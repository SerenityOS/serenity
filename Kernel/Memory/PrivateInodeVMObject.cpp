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
    return adopt_nonnull_ref_or_enomem(new (nothrow) PrivateInodeVMObject(inode, inode.size()));
}

ErrorOr<NonnullRefPtr<VMObject>> PrivateInodeVMObject::try_clone()
{
    return adopt_nonnull_ref_or_enomem<VMObject>(new (nothrow) PrivateInodeVMObject(*this));
}

PrivateInodeVMObject::PrivateInodeVMObject(Inode& inode, size_t size)
    : InodeVMObject(inode, size)
{
}

PrivateInodeVMObject::PrivateInodeVMObject(PrivateInodeVMObject const& other)
    : InodeVMObject(other)
{
}

PrivateInodeVMObject::~PrivateInodeVMObject()
{
}

}
